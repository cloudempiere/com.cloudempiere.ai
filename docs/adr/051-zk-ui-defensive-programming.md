# ADR-051: ZK UI Defensive Programming and Error Resilience

## Status

Proposed

## Date

2025-12-25

## Deciders

- Cloudempiere AI Team
- iDempiere UI Team

## Context and Problem Statement

iDempiere has a tendency to propagate raw exceptions directly to the ZK UI layer, resulting in:
1. **Stack traces shown to users** - Technical errors displayed in dialogs
2. **UI crashes** - Entire window/panel becomes unusable
3. **Cascading failures** - One component error breaks the whole page
4. **Poor user experience** - Confusing, scary error messages
5. **Support burden** - Users report "error" without actionable information

The AI plugin introduces additional failure modes:
- Missing prerequisite tables (AIG_* tables not deployed)
- External service unavailability (Ollama down, API rate limited)
- Missing configuration (no AIG_Provider configured)
- OSGi service not started (EmbeddingStoreProvider not ready)

We need a **defensive programming strategy** that prevents any error from reaching the UI in raw form.

## Decision Drivers

- **Never show stack traces** - Users must never see Java exceptions
- **Graceful degradation** - Show "feature not available" instead of crashing
- **Fail fast, fail safe** - Detect issues early, handle them gracefully
- **Consistent patterns** - All AI UI components follow same error handling
- **Integration with health checks** - Use ADR-050 health service
- **Integration with error handler** - Use ADR-038 for user messages

## Considered Options

1. **Try-catch everywhere** - Wrap every method in try-catch
2. **Error boundary components** - Create wrapper components that catch errors
3. **Defensive service layer** - All UI calls go through defensive service
4. **ZK EventQueue exception handler** - Global ZK-level error handling

## Decision Outcome

**Chosen option:** Combination of "Defensive service layer" + "Error boundary components", because:
- Service layer catches all backend failures before reaching UI
- Error boundary components provide fallback UI for any uncaught errors
- Provides multiple layers of defense
- Follows established patterns from React/Angular error boundaries

### Confirmation

- Verify no stack traces appear in browser when services unavailable
- Verify "Feature not available" message appears when prerequisites missing
- Verify chat panel degrades gracefully when AI provider not configured
- Verify user can still navigate away from failed components

## More Information

### Error Resilience Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                    USER INTERACTION                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 1: Error Boundary Component (Last Resort)                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Catches any uncaught exception from child components   │   │
│  │  Shows: "Something went wrong. Please refresh."         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                           ▼                                     │
│  Layer 2: UI Component (AIChatWidget, AIChatPanel, etc.)        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Checks health before rendering features                │   │
│  │  Shows: "AI features not available" if unhealthy        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                           ▼                                     │
│  Layer 3: Defensive Service Layer                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  AIUIService.safeExecute(operation)                     │   │
│  │  Catches all exceptions, returns Result<T>              │   │
│  │  Never throws to UI layer                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                           ▼                                     │
│  Layer 4: Health Check (ADR-050)                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  AIPluginHealthService.isHealthy()                      │   │
│  │  Fast check before any operation                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                     │
│                           ▼                                     │
│  Layer 5: Backend Services                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  LangChain4jProviderFactory, RAGService, etc.           │   │
│  │  May throw exceptions - NEVER reaches UI                │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Pattern 1: Defensive Service Layer

All UI-to-backend calls go through a defensive wrapper that **never throws**.

```java
/**
 * Defensive service layer for AI UI components.
 * Guarantees no exceptions reach the UI layer.
 */
public class AIUIService {

    private static final CLogger log = CLogger.getCLogger(AIUIService.class);

    /**
     * Result wrapper that carries either success value or error.
     */
    public static class Result<T> {
        private final T value;
        private final String errorMessage;
        private final String errorReference;
        private final boolean available;

        private Result(T value, String errorMessage, String errorReference, boolean available) {
            this.value = value;
            this.errorMessage = errorMessage;
            this.errorReference = errorReference;
            this.available = available;
        }

        public static <T> Result<T> success(T value) {
            return new Result<>(value, null, null, true);
        }

        public static <T> Result<T> notAvailable(String message) {
            return new Result<>(null, message, null, false);
        }

        public static <T> Result<T> error(String message, String reference) {
            return new Result<>(null, message, reference, true);
        }

        public boolean isSuccess() { return value != null && errorMessage == null; }
        public boolean isNotAvailable() { return !available; }
        public boolean isError() { return errorMessage != null && available; }
        public T getValue() { return value; }
        public String getErrorMessage() { return errorMessage; }
        public String getErrorReference() { return errorReference; }
    }

    /**
     * Execute an AI operation with full error protection.
     *
     * @param operation The operation to execute
     * @param featureName Name of the feature (for error messages)
     * @return Result containing success value or error information
     */
    public static <T> Result<T> safeExecute(
            Supplier<T> operation,
            String featureName) {

        // Layer 4: Health check first
        AIPluginHealthService health = AIPluginHealthService.getInstance();
        if (health == null || !health.isHealthy()) {
            String msg = Msg.getMsg(Env.getCtx(), "AIG_FeatureNotAvailable",
                    new Object[] { featureName });
            if (msg.startsWith("AIG_")) {
                msg = featureName + " is not available. The AI plugin is not properly configured.";
            }
            log.warning("AI feature '" + featureName + "' not available: " +
                    (health != null ? health.getStatus().getSummary() : "service not initialized"));
            return Result.notAvailable(msg);
        }

        // Layer 3: Execute with exception catching
        try {
            T result = operation.get();
            return Result.success(result);

        } catch (Exception e) {
            // Use ADR-038 error handler for user-friendly message
            AIErrorHandler.AIErrorResult error = AIErrorHandler.handleError(
                    e, featureName, null);

            log.log(Level.WARNING, "Error in AI feature '" + featureName + "'", e);

            return Result.error(error.getUserMessage(), error.getErrorReference());
        }
    }

    /**
     * Execute a void operation with full error protection.
     */
    public static Result<Void> safeExecuteVoid(
            Runnable operation,
            String featureName) {
        return safeExecute(() -> {
            operation.run();
            return null;
        }, featureName);
    }

    /**
     * Quick check if AI features are available.
     * Use this before showing AI-related UI elements.
     */
    public static boolean isAIAvailable() {
        AIPluginHealthService health = AIPluginHealthService.getInstance();
        return health != null && health.isHealthy();
    }

    /**
     * Get the reason why AI is not available.
     * Returns empty string if AI is available.
     */
    public static String getUnavailableReason() {
        AIPluginHealthService health = AIPluginHealthService.getInstance();
        if (health == null) {
            return Msg.getMsg(Env.getCtx(), "AIG_ServiceNotInitialized");
        }
        if (!health.isHealthy()) {
            return health.getStatus().getSummary();
        }
        return "";
    }
}
```

### Pattern 2: UI Component Health Check

Components check health before rendering features.

```java
/**
 * Example: AI Chat Widget with defensive health checking.
 */
public class AIChatWidget extends DashboardPanel {

    private Div errorPanel;
    private Div chatPanel;

    @Override
    protected void onCreate() {
        // Always safe - just creates empty containers
        errorPanel = new Div();
        errorPanel.setVisible(false);
        appendChild(errorPanel);

        chatPanel = new Div();
        appendChild(chatPanel);

        // Defensive initialization
        initializeSafely();
    }

    private void initializeSafely() {
        // Use defensive service to check availability
        if (!AIUIService.isAIAvailable()) {
            showNotAvailable(AIUIService.getUnavailableReason());
            return;
        }

        // Safe execution of initialization
        AIUIService.Result<Void> result = AIUIService.safeExecuteVoid(
            this::initializeChatComponents,
            "AI Chat");

        if (result.isNotAvailable()) {
            showNotAvailable(result.getErrorMessage());
        } else if (result.isError()) {
            showError(result.getErrorMessage(), result.getErrorReference());
        }
        // Success - chat components initialized
    }

    private void showNotAvailable(String reason) {
        chatPanel.setVisible(false);

        errorPanel.getChildren().clear();
        errorPanel.setVisible(true);
        errorPanel.setSclass("ai-not-available");

        // Friendly message, not stack trace
        Label icon = new Label();
        icon.setSclass("z-icon-info-circle");
        errorPanel.appendChild(icon);

        Label message = new Label(reason);
        message.setSclass("ai-not-available-message");
        errorPanel.appendChild(message);

        // Admin hint if they have permission
        if (MRole.getDefault().isAdministrator()) {
            Label adminHint = new Label(
                Msg.getMsg(Env.getCtx(), "AIG_AdminHint_CheckSetup"));
            adminHint.setSclass("ai-admin-hint");
            errorPanel.appendChild(adminHint);
        }
    }

    private void showError(String message, String reference) {
        chatPanel.setVisible(false);

        errorPanel.getChildren().clear();
        errorPanel.setVisible(true);
        errorPanel.setSclass("ai-error");

        Label icon = new Label();
        icon.setSclass("z-icon-exclamation-triangle");
        errorPanel.appendChild(icon);

        Label messageLabel = new Label(message);
        errorPanel.appendChild(messageLabel);

        if (reference != null) {
            Label refLabel = new Label("Ref: " + reference);
            refLabel.setSclass("ai-error-reference");
            errorPanel.appendChild(refLabel);
        }
    }
}
```

### Pattern 3: Error Boundary Component

Wrapper component that catches any uncaught errors from children.

```java
/**
 * Error boundary component for ZK.
 * Catches any exception from child components and shows fallback UI.
 *
 * Usage:
 * <errorBoundary fallbackMessage="Something went wrong">
 *     <aiChatWidget/>
 * </errorBoundary>
 */
public class ErrorBoundary extends Div {

    private String fallbackMessage = "Something went wrong. Please refresh the page.";
    private boolean hasError = false;
    private Div fallbackPanel;

    public void setFallbackMessage(String message) {
        this.fallbackMessage = message;
    }

    @Override
    public void onChildAdded(Component child) {
        super.onChildAdded(child);

        // Wrap child event listeners with error handling
        wrapChildListeners(child);
    }

    private void wrapChildListeners(Component component) {
        // This is a simplified approach - in production, you'd
        // intercept at the EventQueue level
    }

    /**
     * Called by ZK when an error occurs in child processing.
     * This requires a custom ZK error handler configuration.
     */
    public void onError(Exception e) {
        if (hasError) return; // Already showing fallback

        hasError = true;
        log.log(Level.SEVERE, "Error in child component", e);

        // Hide all children
        for (Component child : getChildren()) {
            if (child != fallbackPanel) {
                child.setVisible(false);
            }
        }

        // Show fallback
        showFallback(e);
    }

    private void showFallback(Exception e) {
        if (fallbackPanel == null) {
            fallbackPanel = new Div();
            fallbackPanel.setSclass("error-boundary-fallback");
            appendChild(fallbackPanel);
        }

        fallbackPanel.getChildren().clear();
        fallbackPanel.setVisible(true);

        // Icon
        Label icon = new Label();
        icon.setSclass("z-icon-times-circle error-boundary-icon");
        fallbackPanel.appendChild(icon);

        // Message
        Label message = new Label(fallbackMessage);
        message.setSclass("error-boundary-message");
        fallbackPanel.appendChild(message);

        // Retry button
        Button retry = new Button(Msg.getMsg(Env.getCtx(), "Retry"));
        retry.addEventListener(Events.ON_CLICK, event -> {
            hasError = false;
            fallbackPanel.setVisible(false);
            // Show children again and re-initialize
            for (Component child : getChildren()) {
                if (child != fallbackPanel) {
                    child.setVisible(true);
                }
            }
        });
        fallbackPanel.appendChild(retry);

        // Create AD_Issue for tracking
        try {
            AIErrorHandler.handleError(e, "ErrorBoundary", null);
        } catch (Exception ignored) {
            // Don't let error handler cause more errors
        }
    }
}
```

### Pattern 4: ZK Desktop-Level Error Handler

Global error handler for the ZK desktop.

```java
/**
 * Register in ZK desktop initialization to catch unhandled errors.
 */
public class AIErrorDesktopListener implements DesktopCleanup, ExecutionCleanup {

    private static final CLogger log = CLogger.getCLogger(AIErrorDesktopListener.class);

    public static void register(Desktop desktop) {
        // Set custom error handler
        desktop.setAttribute("ai.errorHandler", new ErrorHandler() {
            @Override
            public void handleError(Throwable error) {
                log.log(Level.SEVERE, "Unhandled error in desktop", error);

                // Create AD_Issue
                try {
                    AIErrorHandler.handleError(
                        error instanceof Exception ? (Exception) error : new Exception(error),
                        "ZKDesktop",
                        null);
                } catch (Exception ignored) {}

                // Show user-friendly dialog instead of raw error
                try {
                    Messagebox.show(
                        Msg.getMsg(Env.getCtx(), "AIG_UnexpectedError"),
                        Msg.getMsg(Env.getCtx(), "Error"),
                        Messagebox.OK,
                        Messagebox.ERROR);
                } catch (Exception e) {
                    // Last resort - can't even show dialog
                    log.severe("Cannot show error dialog: " + e.getMessage());
                }
            }
        });
    }
}
```

### AD_Message Entries for UI Resilience

| AD_Message Value | English Text | Purpose |
|-----------------|--------------|---------|
| `AIG_FeatureNotAvailable` | "{0} is not available. The AI plugin is not properly configured." | When health check fails |
| `AIG_ServiceNotInitialized` | "The AI service is starting up. Please try again in a moment." | When OSGi service not ready |
| `AIG_UnexpectedError` | "An unexpected error occurred. Please refresh the page and try again." | Global fallback |
| `AIG_AdminHint_CheckSetup` | "Administrator: Check System Admin → AI Provider configuration." | Admin-only hint |

### CSS Styles for Error States

```css
/* Error boundary fallback */
.error-boundary-fallback {
    padding: 20px;
    text-align: center;
    background-color: #fff3cd;
    border: 1px solid #ffc107;
    border-radius: 4px;
    margin: 10px;
}

.error-boundary-icon {
    font-size: 48px;
    color: #856404;
    display: block;
    margin-bottom: 10px;
}

.error-boundary-message {
    font-size: 16px;
    color: #856404;
    display: block;
    margin-bottom: 15px;
}

/* AI not available state */
.ai-not-available {
    padding: 15px;
    text-align: center;
    background-color: #e2e3e5;
    border: 1px solid #d6d8db;
    border-radius: 4px;
}

.ai-not-available-message {
    color: #383d41;
    margin-left: 10px;
}

.ai-admin-hint {
    display: block;
    margin-top: 10px;
    font-size: 12px;
    color: #6c757d;
    font-style: italic;
}

/* Error state */
.ai-error {
    padding: 15px;
    background-color: #f8d7da;
    border: 1px solid #f5c6cb;
    border-radius: 4px;
}

.ai-error-reference {
    display: block;
    margin-top: 5px;
    font-size: 11px;
    color: #721c24;
    font-family: monospace;
}
```

### Integration Points

#### With ADR-050 (Health Check)

```java
// Before any AI UI operation
if (!AIUIService.isAIAvailable()) {
    // Show "not available" message
    return;
}
```

#### With ADR-038 (Error Handling)

```java
// When an error occurs
AIErrorHandler.AIErrorResult error = AIErrorHandler.handleError(e, context, chatId);
showError(error.getUserMessage(), error.getErrorReference());
```

### Checklist for UI Components

Every AI UI component must:

- [ ] **Check health before initialization**: `AIUIService.isAIAvailable()`
- [ ] **Use defensive service layer**: `AIUIService.safeExecute()`
- [ ] **Never throw from event handlers**: Wrap in try-catch
- [ ] **Show "not available" state**: When health check fails
- [ ] **Show user-friendly errors**: Never raw exceptions
- [ ] **Create AD_Issue for errors**: For support tracking
- [ ] **Provide retry option**: Where appropriate
- [ ] **Show admin hints**: When user has permission
- [ ] **Log all errors**: At WARNING or SEVERE level

### Testing Scenarios

| Scenario | Expected Behavior |
|----------|-------------------|
| AIG_Provider table missing | "AI Chat is not available. The AI plugin is not properly configured." |
| No AIG_Provider configured | "AI Chat is not available. No AI provider is configured." |
| Ollama server down | User-friendly error with retry option |
| API rate limited | "I'm experiencing high demand. Please wait 30 seconds." |
| Unexpected exception | "An unexpected error occurred." + AD_Issue created |
| OSGi service not ready | "The AI service is starting up. Please try again." |

### Related ADRs

- [ADR-050](050-plugin-health-prerequisite-verification.md) - Health checks this builds on
- [ADR-038](038-user-friendly-error-handling.md) - Error message patterns
- [ADR-015](015-conversational-ux-patterns.md) - UX patterns for chat

### References

- [React Error Boundaries](https://reactjs.org/docs/error-boundaries.html) - Inspiration for pattern
- [ZK Error Handling](https://www.zkoss.org/wiki/ZK_Developer%27s_Reference/UI_Patterns/Error_Handling)
- [Defensive Programming](https://en.wikipedia.org/wiki/Defensive_programming)

---

## Appendix: Migration from Existing Components

### Step 1: Audit existing components for error handling

```bash
# Find all catch blocks that might re-throw
grep -r "throw" src/com/cloudempiere/ai/component/*.java
grep -r "catch.*Exception" src/com/cloudempiere/ai/component/*.java
```

### Step 2: Wrap initialization with defensive service

Before:
```java
@Override
protected void onCreate() {
    MAIProvider provider = MAIProvider.getDefault(ctx);  // May throw!
    // ... more code that may throw
}
```

After:
```java
@Override
protected void onCreate() {
    initializeSafely();
}

private void initializeSafely() {
    if (!AIUIService.isAIAvailable()) {
        showNotAvailable(AIUIService.getUnavailableReason());
        return;
    }

    AIUIService.Result<Void> result = AIUIService.safeExecuteVoid(
        () -> {
            MAIProvider provider = MAIProvider.getDefault(ctx);
            // ... initialization code
        },
        "AI Chat");

    if (!result.isSuccess()) {
        showError(result.getErrorMessage(), result.getErrorReference());
    }
}
```

### Step 3: Wrap event handlers

Before:
```java
button.addEventListener(Events.ON_CLICK, event -> {
    processChat(userInput);  // May throw!
});
```

After:
```java
button.addEventListener(Events.ON_CLICK, event -> {
    AIUIService.Result<AIResponse> result = AIUIService.safeExecute(
        () -> processChat(userInput),
        "Send Message");

    if (result.isSuccess()) {
        displayResponse(result.getValue());
    } else if (result.isNotAvailable()) {
        showNotAvailable(result.getErrorMessage());
    } else {
        showError(result.getErrorMessage(), result.getErrorReference());
    }
});
```
