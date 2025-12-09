# ADR-031: Chat Panel Adaptation to LangChain4j ChatModel

## Status

Accepted (Revised 2025-12-08) - Implementation In Progress

## Date

2025-12-03

## Deciders

Cloudempiere AI Team

## Revision Note (2025-12-08)

**Original Plan (2025-12-03):** Create `LangChain4jConversationService` that retained custom routing.

**Current Implementation (v0.19.0):** `AIService` singleton with streaming-first architecture:
- `chatStreamingWithContext()` - Primary streaming method (all flows)
- `chatBlocking()` - CompletableFuture.join() wrapper for synchronous API
- `chatWithContext()` - Legacy method for non-streaming use cases
- `ThreadAwareChatMemory` - Thread-isolated conversation memory

**Streaming-First Pattern:**
```
chatBlocking() ──┐
                 ├──► chatStreamingWithContext() ──► AI Provider
chatWithContext()┘     (Core implementation)
```

**Impact:**
- No separate `LangChain4jConversationService` created - using `AIService` directly
- Unified `ERPTools` with optional callbacks (merged from StreamingERPTools)
- Custom routing deferred to RAG integration (ADR-012)
- `ThreadAwareChatMemory` implemented for thread isolation

## Context and Problem Statement

The `AIChatWidget` currently uses `AIConversationService` which relies on the custom `IAIProvider` interface with custom DTOs (`AIRequest`, `AIResponse`, `AIMessage`). Per ADR-002, we are strategically adopting LangChain4j as the primary AI framework. The chat panel must be adapted to use LangChain4j's `ChatLanguageModel` and `AiServices` directly while preserving critical iDempiere-specific features.

**Current Architecture:**
```
AIChatWidget
    └── AIConversationService (1226 lines)
            ├── Uses: AIProviderFactory (custom)
            │           └── IAIProvider (AnthropicProvider, AWSBedrockProvider)
            ├── Custom: Intelligent routing (PromptAnalyzer, SourceDecision)
            ├── Custom: Query caching (ConversationContextManager)
            ├── Custom: Function calling (AIDatabaseFunctionHandler)
            └── Custom: System prompt building
```

**Current Architecture (v0.19.0 - Streaming-First):**
```
AIChatWidget
    └── AIService (Singleton, Streaming-First)
            ├── chatStreamingWithContext() ─────────────────────┐
            │                                                    │
            ├── chatBlocking() ──┐                               │
            │                    │ CompletableFuture.join()      │
            │                    ▼                               │
            │            ───────────────────────────────────────►│
            │                                                    │
            ├── Uses: LangChain4jProviderFactory                 │
            │           └── StreamingChatLanguageModel           │
            │                                                    │
            ├── Agents: ERPStreamingAgent (with tools)           │
            │           SimpleStreamingAgent (Ollama/Llama)      │
            │                                                    │
            ├── Tools: ERPTools (unified, optional callback)     │
            │                                                    │
            ├── Memory: ThreadAwareChatMemory                    │
            │                                                    │
            └── Guardrails: CostGuard, InputGuard, OutputGuard   │
```

## Decision Drivers

- **Consistency with ADR-002**: Complete LangChain4j adoption requires chat panel integration
- **Code Simplification**: Replace ~1200 lines of custom orchestration with LangChain4j AiServices
- **Feature Parity**: Must retain intelligent routing, caching, thread filtering, and context awareness
- **Streaming Support**: Enable real-time streaming responses via StreamingChatLanguageModel
- **Observability**: Leverage LangChain4j listeners for cost/latency tracking
- **Thread-Safe Memory**: Support multi-thread conversations with session-isolated memory
- **Backward Compatibility**: Existing MChat/MChatEntry persistence must continue working

## Considered Options

1. **Full Replacement**: Replace AIConversationService entirely with IDempiereAIService
2. **Adapter Pattern**: Create thin adapter from AIConversationService to IDempiereAIService
3. **Parallel Systems**: Keep both systems, gradually migrate features
4. **New Facade**: Create LangChain4jConversationService that wraps IDempiereAIService with routing/caching

## Decision Outcome

**Chosen option:** "Option 4: New Facade", because it provides the best balance between leveraging LangChain4j simplicity while retaining critical iDempiere-specific features that are not available in LangChain4j.

### Confirmation

The decision will be confirmed when:
- [x] AIChatWidget uses AIService (streaming-first) ✅ v0.19.0
- [ ] All existing tests pass
- [ ] New integration tests for LangChain4j path added
- [x] Streaming responses work in UI ✅ v0.17.0
- [x] Multi-thread conversations work correctly ✅ ThreadAwareChatMemory v0.14.0
- [x] Cost/token tracking functions via listeners ✅ MAIUsageMetrics v0.17.0
- [ ] AIConversationService marked @Deprecated
- [ ] Stop button implemented for request cancellation
- [ ] Streaming cancellation works correctly

## Pros and Cons of the Options

### Option 1: Full Replacement

Replace AIConversationService with IDempiereAIService directly.

- Good, because maximum code reduction (~1200 lines removed)
- Good, because simplest architecture
- Bad, because loses intelligent routing (CONTEXT_ONLY path)
- Bad, because loses query result caching
- Bad, because LangChain4j MessageWindowChatMemory doesn't support thread filtering
- Bad, because loses dynamic system prompt building with context

### Option 2: Adapter Pattern

Wrap AIConversationService to call IDempiereAIService internally.

- Good, because minimal changes to AIChatWidget
- Good, because gradual migration possible
- Bad, because adds complexity (adapter on top of facade)
- Bad, because doesn't reduce code, just adds indirection
- Neutral, because still uses custom DTOs (AIRequest, AIResponse)

### Option 3: Parallel Systems

Run both AIConversationService and IDempiereAIService side by side.

- Good, because no migration risk
- Good, because can A/B test
- Bad, because double maintenance burden
- Bad, because confusing for developers
- Bad, because no code reduction

### Option 4: New Facade (Chosen)

Create LangChain4jConversationService that:
1. Uses IDempiereAIService/IDempiereAgent for AI calls
2. Retains intelligent routing layer
3. Retains query caching layer
4. Implements thread-aware ChatMemory
5. Returns LangChain4j native types to caller

- Good, because leverages LangChain4j simplicity
- Good, because retains iDempiere-specific optimizations
- Good, because cleaner separation of concerns
- Good, because enables future enhancements (RAG, structured outputs)
- Neutral, because requires new code (but simpler than original)
- Bad, because requires AIChatWidget refactoring

## More Information

### Implementation Notes

#### 1. Thread-Aware ChatMemory

LangChain4j's `MessageWindowChatMemory` doesn't support filtering by thread. We need a custom implementation:

```java
public class ThreadAwareChatMemory implements ChatMemory {

    private final int maxMessages;
    private final Map<String, List<ChatMessage>> threadMessages = new ConcurrentHashMap<>();

    @Override
    public void add(ChatMessage message) {
        String memoryId = getCurrentThreadId();
        threadMessages.computeIfAbsent(memoryId, k -> new ArrayList<>())
            .add(message);
        trimToMaxSize(memoryId);
    }

    @Override
    public List<ChatMessage> messages() {
        String memoryId = getCurrentThreadId();
        return threadMessages.getOrDefault(memoryId, Collections.emptyList());
    }

    public void switchThread(String threadId) {
        // Thread context switching
    }

    public void createNewThread() {
        // Creates new empty thread
    }
}
```

#### 2. LangChain4jConversationService Structure

```java
public class LangChain4jConversationService {

    private final IDempiereAIService aiService;
    private final PromptAnalyzer promptAnalyzer;
    private final ConversationContextManager contextManager;
    private final RoutingMetrics routingMetrics;

    public String sendMessage(
        Properties ctx,
        MAIChat chat,
        String userMessage,
        JSONObject contextData,
        int threadRootId,
        String trxName
    ) {
        // 1. Analyze prompt for routing
        SourceDecision decision = promptAnalyzer.analyzePrompt(userMessage, contextManager);

        // 2. Handle CONTEXT_ONLY (no AI call needed)
        if (decision.getSource() == DataSource.CONTEXT_ONLY) {
            routingMetrics.recordContextOnlyResponse();
            return buildContextOnlyResponse(decision);
        }

        // 3. Build session ID from chat + thread
        String sessionId = buildSessionId(chat, threadRootId);

        // 4. Inject context into system prompt if needed
        if (contextData != null) {
            injectContextIntoMemory(sessionId, contextData);
        }

        // 5. Call LangChain4j via IDempiereAIService
        MAIProvider provider = getProvider(ctx, DEFAULT_PROVIDER_ID, trxName);
        return aiService.chat(provider, ctx, sessionId, userMessage);
    }

    public void createNewThread(String sessionId) {
        aiService.clearMemory(sessionId);
    }
}
```

#### 3. AIChatWidget Changes

**Before:**
```java
private AIConversationService aiService;
private AIResponse aiResponse;

aiResponse = aiService.sendMessageWithContext(
    sessionCtx, chat, userMessage, contextData, 10, threadRootId, null);
String content = aiResponse.getContent();
```

**After:**
```java
private LangChain4jConversationService aiService;

String content = aiService.sendMessage(
    sessionCtx, chat, userMessage, contextData, threadRootId, null);
```

#### 4. Stop Button for Request Cancellation

The chat panel currently lacks a stop button to cancel in-progress AI requests. This is a critical UX requirement for long-running requests.

**Current State:**
- Send button is disabled during processing
- Loading indicator shows "AI is thinking..."
- No way to cancel/abort the request
- User must wait for completion or error timeout

**Required Implementation:**

```java
// New fields in AIChatWidget
private Button stopButton;
private volatile CompletableFuture<?> currentRequest;
private volatile boolean requestCancelled = false;

// Stop button initialization (in init() method)
stopButton = new Button();
stopButton.addEventListener(Events.ON_CLICK, this);
stopButton.setSclass("ai-stop-btn");
if (ThemeManager.isUseFontIconForImage())
    stopButton.setIconSclass("z-icon-Stop");
else
    stopButton.setImage(ThemeManager.getThemeResource("images/Stop.png"));
stopButton.setStyle("width: 42px; height: 42px; background: #D32F2F; border-radius: 100px; " +
    "display: none; align-items: center; justify-content: center; border: none; cursor: pointer;");
stopButton.setTooltiptext(Msg.getMsg(Env.getCtx(), "Stop"));

// Add to input area (after send button)
inputArea.appendChild(stopButton);

// Toggle button visibility
private void showStopButton() {
    sendButton.setStyle(sendButton.getStyle().replace("display: flex", "display: none"));
    stopButton.setStyle(stopButton.getStyle().replace("display: none", "display: flex"));
}

private void showSendButton() {
    stopButton.setStyle(stopButton.getStyle().replace("display: flex", "display: none"));
    sendButton.setStyle(sendButton.getStyle().replace("display: none", "display: flex"));
}

// Stop button handler (in onEvent method)
else if (event.getTarget() == stopButton) {
    cancelCurrentRequest();
}

// Cancel implementation
private void cancelCurrentRequest() {
    requestCancelled = true;
    if (currentRequest != null && !currentRequest.isDone()) {
        currentRequest.cancel(true);
    }
    hideLoading();
    showSendButton();
    inputBox.setDisabled(false);

    // Show cancellation message
    Clients.showNotification(
        Msg.getMsg(Env.getCtx(), "AIRequestCancelled"),
        "info", this, null, 2000);
}

// Modify sendMessage to track the future
public void sendMessage() {
    // ... existing code ...
    requestCancelled = false;
    showStopButton();

    currentRequest = CompletableFuture.runAsync(() -> {
        // Check cancellation at key points
        if (requestCancelled) return;

        // ... AI call ...

        if (requestCancelled) return;

        // ... process response ...
    });
}
```

**Streaming Cancellation (for LangChain4j):**

```java
// For streaming responses, use TokenStream's cancel capability
private volatile TokenStream currentStream;

TokenStream stream = agent.chat(sessionId, message);
currentStream = stream;

stream.onPartialResponse(token -> {
    if (requestCancelled) {
        currentStream.cancel();  // Stop the stream
        return;
    }
    Executions.schedule(desktop, e -> appendToken(token), new Event("onToken"));
})
.onComplete(response -> {
    if (!requestCancelled) {
        Executions.schedule(desktop, e -> finalizeMessage(), new Event("onComplete"));
    }
    showSendButton();
})
.onError(error -> {
    if (!requestCancelled) {
        Executions.schedule(desktop, e -> showError(error), new Event("onError"));
    }
    showSendButton();
})
.start();
```

**Migration Phase Addition:**

| Phase | Action | Files Affected |
|-------|--------|----------------|
| 4a | Add stop button UI | MODIFY: component/AIChatWidget.java |
| 4b | Implement request cancellation | MODIFY: component/AIChatWidget.java |
| 4c | Add streaming cancellation | MODIFY: component/AIChatWidget.java |

**New Messages Required:**
- `AIRequestCancelled` = "AI request cancelled"
- `Stop` = "Stop" (tooltip)

#### 5. Streaming Integration

For streaming responses, use `StreamingChatLanguageModel`:

```java
public interface IDempiereStreamingAgent {
    @SystemMessage(SYSTEM_PROMPT)
    TokenStream chat(@MemoryId String sessionId, @UserMessage String message);
}

// In AIChatWidget
TokenStream stream = agent.chat(sessionId, message);
stream.onPartialResponse(token -> {
    Executions.schedule(desktop, e -> appendToken(token), new Event("onToken"));
})
.onComplete(response -> {
    Executions.schedule(desktop, e -> finalizeMessage(), new Event("onComplete"));
})
.onError(error -> {
    Executions.schedule(desktop, e -> showError(error), new Event("onError"));
})
.start();
```

#### 5. Observability via Listeners

```java
ChatLanguageModel model = AnthropicChatModel.builder()
    .apiKey(apiKey)
    .modelName("claude-sonnet-4-20250514")
    .listeners(List.of(
        new TokenUsageListener(),           // Cost tracking
        new LatencyMetricsListener(),       // Performance
        new IDempiereAuditListener(ctx)     // Audit logging
    ))
    .build();
```

#### 6. Migration Strategy

| Phase | Action | Files Affected |
|-------|--------|----------------|
| 1 | Create ThreadAwareChatMemory | NEW: langchain4j/ThreadAwareChatMemory.java |
| 2 | Create LangChain4jConversationService | NEW: service/LangChain4jConversationService.java |
| 3 | Add streaming to IDempiereAgent | MODIFY: IDempiereAgent.java |
| 4 | Update AIChatWidget | MODIFY: component/AIChatWidget.java |
| 5 | Add observability listeners | NEW: langchain4j/listeners/*.java |
| 6 | Deprecate AIConversationService | MODIFY: service/AIConversationService.java |
| 7 | Update tests | MODIFY/NEW: test/*.java |

### Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                             AIChatWidget (ZK UI)                           │
│  ┌─────────────┐  ┌───────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │Thread Select│  │ Messages      │  │ Input Box    │  │Send/Stop Btn │   │
│  └─────────────┘  └───────────────┘  └──────────────┘  └──────────────┘   │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│               LangChain4jConversationService (NEW Facade)                  │
│  ┌──────────────────────────┐  ┌───────────────────────────────────────┐  │
│  │   Intelligent Routing    │  │    MChat/MChatEntry Persistence       │  │
│  │  ┌──────────────────┐    │  │                                       │  │
│  │  │ PromptAnalyzer   │    │  │  ┌─────────────┐  ┌──────────────┐    │  │
│  │  │ SourceDecision   │    │  │  │ MAIChat     │  │ MAIChatEntry │    │  │
│  │  │ ContextManager   │    │  │  └─────────────┘  └──────────────┘    │  │
│  │  └──────────────────┘    │  │                                       │  │
│  └──────────────────────────┘  └───────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                      IDempiereAIService (Existing)                         │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │                    IDempiereAgent (AiServices)                        │ │
│  │  ┌────────────────┐  ┌─────────────────────┐  ┌──────────────────┐   │ │
│  │  │ @SystemMessage │  │ ThreadAwareChatMemory│  │ ERPTools (@Tool) │   │ │
│  │  └────────────────┘  └─────────────────────┘  └──────────────────┘   │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                 LangChain4jProviderFactory (Existing)                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐│
│  │AnthropicChatModel│  │ BedrockChatModel│  │ OllamaChatModel/OpenAiChat ││
│  └─────────────────┘  └─────────────────┘  └─────────────────────────────┘│
└────────────────────────────────────────────────────────────────────────────┘
```

### Features Retained vs Migrated

| Feature | Current Location | New Location | Status |
|---------|------------------|--------------|--------|
| Intelligent Routing | AIConversationService | LangChain4jConversationService | Retained |
| Query Caching (TTL) | ConversationContextManager | LangChain4jConversationService | Retained |
| Thread Filtering | buildConversationHistory() | ThreadAwareChatMemory | Migrated |
| System Prompt Building | buildSystemPrompt() | Hybrid (see below) | Partial |
| Function Calling | AIDatabaseFunctionHandler | ERPTools (@Tool) | Migrated (ADR-002) |
| Cost Estimation | AIResponse.getCostUSD() | TokenUsageListener | Migrated |
| Streaming | AIStreamCallback | StreamingChatLanguageModel | Migrated |
| Error Handling | AIResponse.getErrorMessage() | Exception + Listener | Migrated |
| Message Persistence | MAIChatEntry | MAIChatEntry (unchanged) | Retained |
| Context Injection | contextData parameter | Memory injection | Migrated |
| Stop/Cancel Request | Not implemented | stopButton + CompletableFuture.cancel() | New |
| Streaming Cancellation | Not implemented | TokenStream.cancel() | New |

#### System Prompt Hybrid Architecture (v0.19.0)

The system prompt implementation uses a **hybrid approach**:

| Component | Prompt Source | Configurable? |
|-----------|--------------|---------------|
| `RAGConversationService` | Database (`AIG_Prompt_Config`) | ✅ Yes - per client |
| `ERPAgent` / `ERPStreamingAgent` | Java `@SystemMessage(SYSTEM_PROMPT)` | ❌ No - compile-time |
| `SimpleAgent` / `SimpleStreamingAgent` | Java `@SystemMessage(SIMPLE_SYSTEM_PROMPT)` | ❌ No - compile-time |
| `IERPAgent` | Java `@SystemMessage({...})` | ❌ No - compile-time |

**Why Hybrid?**
- LangChain4j `@SystemMessage` annotation requires compile-time String constants
- Database prompts (`AIG_Prompt_Config`) still used by `RAGConversationService` for per-client customization
- LangChain4j agents use hardcoded prompts in `ERPAgent.SYSTEM_PROMPT` and `SimpleAgent.SIMPLE_SYSTEM_PROMPT`

**Files:**
- Database lookup: `RAGConversationService.java:411-418`
- Java prompts: `ERPAgent.java:39-70`, `SimpleAgent.java:26-31`, `IERPAgent.java:57-76`
- Model: `MAIPromptConfig.java`

### Related ADRs

- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j Strategic Adoption (parent decision)
- [ADR-005](005-intelligent-data-source-routing.md) - Intelligent Data Source Routing (retained)
- [ADR-007](007-database-security-model.md) - Database Security Model (retained via ERPTools)
- [ADR-013](013-observability-cost-tracking.md) - Observability and Cost Tracking (enhanced via listeners)
- [ADR-015](015-conversational-ux-patterns.md) - Conversational UX Patterns (UI unchanged)
- [ADR-036](036-chat-ownership-and-sharing-model.md) - Chat Ownership and Sharing Model (chat access control)

### References

- [LangChain4j AiServices](https://docs.langchain4j.dev/tutorials/ai-services)
- [LangChain4j Chat Memory](https://docs.langchain4j.dev/tutorials/chat-memory)
- [LangChain4j Streaming](https://docs.langchain4j.dev/tutorials/response-streaming)
- [LangChain4j Tools](https://docs.langchain4j.dev/tutorials/tools)

---

*ADR-031 | Version 1.2 | 2025-12-08*
*Updated: Documented actual streaming-first implementation (AIService, ERPTools unified)*
