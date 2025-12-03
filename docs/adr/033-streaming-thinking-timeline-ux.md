# ADR-033: Streaming Responses and Thinking Timeline UX

<!-- MADR 3.0 Template -->

## Status

Proposed

## Date

2025-12-03

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

The current `AIChatWidget` implementation waits for complete AI responses before displaying them to users. This creates a poor user experience where users see only a static "Thinking..." indicator for several seconds with no visibility into what the AI is doing. Modern AI interfaces like Claude Desktop/Web show streaming tokens as they arrive, display tool usage progress ("Querying database...", "Reading file..."), and optionally show reasoning/thinking processes. Without these features, our chat panel feels unresponsive and opaque compared to industry-standard AI experiences.

**Current UX Problems:**
1. **Black box waiting** - Users see "Thinking..." with no indication of progress
2. **No streaming** - Response appears all at once after multi-second wait
3. **Hidden tool use** - Users don't know when AI queries database or uses tools
4. **No reasoning visibility** - Extended thinking models' reasoning is not displayed

## Decision Drivers

- **User Experience**: Modern AI interfaces stream responses; static waiting feels outdated
- **Transparency**: Users should understand what the AI is doing (tool calls, reasoning)
- **Perceived Performance**: Streaming makes responses feel faster even if total time is same
- **Debugging/Trust**: Visible tool calls help users understand and trust AI decisions
- **LangChain4j Alignment**: ADR-031 plans LangChain4j migration; streaming is native
- **Extended Thinking**: Claude 3.5 Sonnet and future models support extended thinking

## Considered Options

1. **Full Streaming UX with Tool Timeline and Thinking Display**
2. **Streaming Text Only (no tool/thinking visibility)**
3. **Progressive Disclosure (expandable details after completion)**
4. **Keep Current Batch Response Model**

## Decision Outcome

**Chosen option:** "Option 1: Full Streaming UX with Tool Timeline and Thinking Display", because it provides the best user experience, aligns with industry standards (Claude Desktop/Web, ChatGPT), and leverages LangChain4j's native streaming capabilities planned in ADR-031.

### Confirmation

The decision will be confirmed when:
- [ ] `AIStreamCallback` interface extended with tool/thinking events
- [ ] `AIChatStreamingMessage` component renders streaming tokens
- [ ] Tool timeline shows start/complete status for each tool call
- [ ] Thinking section (collapsible) displays extended thinking content
- [ ] CSS animations provide smooth visual feedback
- [ ] ZK thread safety verified with `Executions.schedule()`
- [ ] User testing confirms improved perceived responsiveness

## Pros and Cons of the Options

### Option 1: Full Streaming UX with Tool Timeline and Thinking Display

Complete streaming implementation with visibility into all AI operations.

- Good, because matches modern AI interface expectations (Claude Desktop/Web)
- Good, because streaming improves perceived performance significantly
- Good, because tool timeline increases transparency and trust
- Good, because thinking display aids debugging and understanding
- Good, because aligns with LangChain4j `TokenStream` API (ADR-031)
- Neutral, because requires more UI components and state management
- Bad, because ZK threading requires careful `Executions.schedule()` usage
- Bad, because increases client-side JavaScript complexity

### Option 2: Streaming Text Only

Stream response tokens but hide tool usage and thinking.

- Good, because simpler implementation than full option
- Good, because still improves perceived performance
- Neutral, because partial improvement over current state
- Bad, because users still don't see what AI is doing during pauses
- Bad, because tool calls cause unexplained delays in token stream
- Bad, because misses opportunity for transparency

### Option 3: Progressive Disclosure

Show complete response first, then allow expanding details.

- Good, because simpler streaming implementation
- Good, because details available for power users
- Bad, because doesn't address perceived performance issue
- Bad, because users still wait for complete response
- Bad, because tool/thinking info only visible after the fact

### Option 4: Keep Current Batch Response Model

No changes to current implementation.

- Good, because no development effort required
- Bad, because poor user experience compared to competitors
- Bad, because wastes LangChain4j streaming capabilities
- Bad, because misses transparency opportunity
- Bad, because increasingly outdated as AI interfaces evolve

## More Information

### Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Streaming UX Architecture                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  User Input                                                                 │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  AIChatWidget                                                        │   │
│  │  - Creates AIChatStreamingMessage component                         │   │
│  │  - Passes AIStreamCallback to service                               │   │
│  │  - Uses Executions.schedule() for UI updates                        │   │
│  └────────────────────────────────────┬────────────────────────────────┘   │
│                                       │                                     │
│                                       ▼                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  LangChain4jConversationService (ADR-031)                           │   │
│  │  - Uses StreamingChatLanguageModel                                  │   │
│  │  - Emits events via AIStreamCallback                                │   │
│  │  - Handles tool execution with progress events                      │   │
│  └────────────────────────────────────┬────────────────────────────────┘   │
│                                       │                                     │
│                                       ▼                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  TokenStream (LangChain4j)                                          │   │
│  │  - onPartialResponse(token)  → callback.onChunk(token)              │   │
│  │  - onToolExecuted(exec)      → callback.onToolStart/Complete()      │   │
│  │  - onComplete(response)      → callback.onComplete()                │   │
│  │  - onError(error)            → callback.onError()                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Enhanced AIStreamCallback Interface

```java
/**
 * Enhanced callback interface for streaming responses with tool and thinking support.
 * Extends the basic streaming model to provide full visibility into AI operations.
 */
public interface AIStreamCallback {

    // ============= Core Streaming Events =============

    /**
     * Called when a new text chunk is received from the model.
     * @param chunk text chunk (typically 1-5 tokens)
     */
    void onChunk(String chunk);

    /**
     * Called when streaming is complete.
     */
    void onComplete();

    /**
     * Called when an error occurs during streaming.
     * @param error exception that occurred
     */
    void onError(Exception error);

    // ============= Tool/Function Events =============

    /**
     * Called when the AI starts executing a tool/function.
     * @param toolName name of the tool being executed
     * @param arguments JSON string of tool arguments
     */
    default void onToolStart(String toolName, String arguments) {}

    /**
     * Called when a tool/function execution completes.
     * @param toolName name of the tool that completed
     * @param result result of the tool execution (may be truncated for display)
     */
    default void onToolComplete(String toolName, String result) {}

    /**
     * Called when a tool/function execution fails.
     * @param toolName name of the tool that failed
     * @param error error message
     */
    default void onToolError(String toolName, String error) {}

    // ============= Thinking/Reasoning Events =============

    /**
     * Called when thinking/reasoning content is received (extended thinking models).
     * @param thinkingChunk chunk of thinking/reasoning text
     */
    default void onThinking(String thinkingChunk) {}

    /**
     * Called when thinking phase completes and response generation begins.
     */
    default void onThinkingComplete() {}

    // ============= Progress Events =============

    /**
     * Called to indicate processing stage changes.
     * @param stage current stage (e.g., "analyzing", "querying", "formatting")
     * @param detail optional detail message
     */
    default void onProgress(String stage, String detail) {}
}
```

### AIChatStreamingMessage Component

```java
/**
 * ZK component for rendering streaming AI responses with tool timeline and thinking.
 */
public class AIChatStreamingMessage extends Div {

    // UI Sections
    private Div thinkingSection;      // Collapsible thinking display
    private Div toolsSection;         // Tool usage timeline
    private Div contentSection;       // Main streaming text content
    private Html streamingContent;    // The actual streaming text element

    // State
    private StringBuilder content = new StringBuilder();
    private StringBuilder thinking = new StringBuilder();
    private List<ToolEvent> toolEvents = new ArrayList<>();
    private boolean isThinkingExpanded = false;
    private boolean isComplete = false;

    /**
     * Append a text chunk to the streaming content.
     * Called from onChunk callback via Executions.schedule().
     */
    public void appendChunk(String chunk) {
        content.append(chunk);
        updateContentDisplay();
    }

    /**
     * Show tool execution start in the timeline.
     */
    public void showToolStart(String toolName, String arguments) {
        toolsSection.setVisible(true);
        toolEvents.add(new ToolEvent(toolName, ToolStatus.RUNNING, arguments, null));
        updateToolsDisplay();
    }

    /**
     * Update tool to complete status in the timeline.
     */
    public void showToolComplete(String toolName, String result) {
        for (ToolEvent event : toolEvents) {
            if (event.getName().equals(toolName) && event.getStatus() == ToolStatus.RUNNING) {
                event.setStatus(ToolStatus.COMPLETE);
                event.setResult(truncateResult(result, 100));
                break;
            }
        }
        updateToolsDisplay();
    }

    /**
     * Append thinking content (collapsible section).
     */
    public void appendThinking(String thinkingChunk) {
        thinkingSection.setVisible(true);
        thinking.append(thinkingChunk);
        updateThinkingDisplay();
    }

    /**
     * Finalize the message when streaming completes.
     * Removes streaming cursor, enables copy button, etc.
     */
    public void finalize() {
        isComplete = true;
        removeCursorAnimation();
        enableCopyButton();
        renderFinalMarkdown();
    }

    /**
     * Get the complete content for persistence.
     */
    public String getContent() {
        return content.toString();
    }

    // ============= Private UI Update Methods =============

    private void updateContentDisplay() {
        // Render partial markdown with streaming cursor
        String html = renderPartialMarkdown(content.toString());
        html += "<span class='streaming-cursor'>|</span>";
        streamingContent.setContent(html);
    }

    private void updateToolsDisplay() {
        StringBuilder html = new StringBuilder();
        html.append("<div class='ai-tools-timeline'>");
        for (ToolEvent event : toolEvents) {
            html.append(renderToolEvent(event));
        }
        html.append("</div>");
        ((Html) toolsSection.getFirstChild()).setContent(html.toString());
    }

    private String renderToolEvent(ToolEvent event) {
        String icon = switch (event.getStatus()) {
            case RUNNING -> "<span class='tool-spinner'>⟳</span>";
            case COMPLETE -> "<span class='tool-complete'>✓</span>";
            case ERROR -> "<span class='tool-error'>✗</span>";
        };
        String displayName = getToolDisplayName(event.getName());
        return String.format(
            "<div class='tool-event tool-%s'>" +
            "  %s <span class='tool-name'>%s</span>" +
            "</div>",
            event.getStatus().name().toLowerCase(), icon, displayName
        );
    }

    private String getToolDisplayName(String toolName) {
        // Map internal tool names to user-friendly display names
        return switch (toolName) {
            case "queryDatabase" -> "Querying database...";
            case "executeQuery" -> "Running query...";
            case "getWindowContext" -> "Reading window data...";
            case "calculateMetrics" -> "Calculating metrics...";
            default -> toolName + "...";
        };
    }

    /** Tool event tracking */
    private static class ToolEvent {
        private String name;
        private ToolStatus status;
        private String arguments;
        private String result;
        // Constructor, getters, setters...
    }

    private enum ToolStatus { RUNNING, COMPLETE, ERROR }
}
```

### CSS Styling

```css
/* Streaming message styles */

.ai-streaming-message {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 12px 18px;
}

/* Thinking section (collapsible) */
.ai-thinking-section {
    border: 1px solid #E8E8E8;
    border-radius: 8px;
    background: #FAFAFA;
    overflow: hidden;
}

.ai-thinking-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    cursor: pointer;
    font-size: 11px;
    color: #666;
}

.ai-thinking-header:hover {
    background: #F0F0F0;
}

.ai-thinking-toggle {
    transition: transform 0.2s;
}

.ai-thinking-toggle.expanded {
    transform: rotate(90deg);
}

.ai-thinking-content {
    padding: 12px;
    font-size: 11px;
    color: #555;
    line-height: 1.5;
    max-height: 200px;
    overflow-y: auto;
    border-top: 1px solid #E8E8E8;
    background: #FDFDFD;
}

/* Tools timeline */
.ai-tools-timeline {
    padding: 8px 12px;
    background: #F8F9FA;
    border-radius: 8px;
    border-left: 3px solid #1976D2;
}

.tool-event {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 0;
    font-size: 11px;
    color: #555;
}

.tool-spinner {
    color: #1976D2;
    animation: tool-spin 1s linear infinite;
}

@keyframes tool-spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.tool-complete {
    color: #4CAF50;
}

.tool-error {
    color: #F44336;
}

.tool-name {
    color: #333;
}

/* Streaming cursor */
.streaming-cursor {
    color: #1976D2;
    animation: cursor-blink 0.8s ease-in-out infinite;
    font-weight: bold;
}

@keyframes cursor-blink {
    0%, 50% { opacity: 1; }
    51%, 100% { opacity: 0; }
}

/* Thinking indicator (animated dots) */
.ai-thinking-dots {
    display: inline-flex;
    gap: 4px;
    margin-left: 8px;
}

.ai-thinking-dots span {
    width: 5px;
    height: 5px;
    background: #717680;
    border-radius: 50%;
    animation: thinking-pulse 1.4s ease-in-out infinite;
}

.ai-thinking-dots span:nth-child(1) { animation-delay: 0s; }
.ai-thinking-dots span:nth-child(2) { animation-delay: 0.2s; }
.ai-thinking-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes thinking-pulse {
    0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
    40% { opacity: 1; transform: scale(1); }
}
```

### ZK Threading Considerations

ZK UI updates must happen on the correct thread. All callback methods are invoked from background threads and must use `Executions.schedule()`:

```java
// In AIChatWidget.sendMessage()
AIStreamCallback callback = new AIStreamCallback() {
    @Override
    public void onChunk(String chunk) {
        // CRITICAL: Must schedule UI update on ZK event thread
        Executions.schedule(desktop, event -> {
            streamingMsg.appendChunk(chunk);
            scrollToBottom();
        }, new Event("onChunk"));
    }

    @Override
    public void onToolStart(String toolName, String args) {
        Executions.schedule(desktop, event -> {
            streamingMsg.showToolStart(toolName, args);
        }, new Event("onToolStart"));
    }

    @Override
    public void onComplete() {
        Executions.schedule(desktop, event -> {
            streamingMsg.finalize();
            saveAIResponse(streamingMsg.getContent(), threadRootId);
            enableInput();
        }, new Event("onComplete"));
    }

    // ... other callbacks follow same pattern
};
```

### LangChain4j Integration (per ADR-031)

```java
/**
 * Streaming service using LangChain4j TokenStream.
 */
public class LangChain4jConversationService {

    public void sendMessageStreaming(
        Properties ctx,
        MAIChat chat,
        String userMessage,
        JSONObject contextData,
        int threadRootId,
        AIStreamCallback callback,
        String trxName
    ) {
        String sessionId = buildSessionId(chat, threadRootId);

        // Build streaming agent with tool support
        StreamingChatLanguageModel model = providerFactory.getStreamingModel(ctx, trxName);

        IDempiereStreamingAgent agent = AiServices.builder(IDempiereStreamingAgent.class)
            .streamingChatLanguageModel(model)
            .chatMemory(getMemory(sessionId))
            .tools(erpTools)
            .build();

        TokenStream stream = agent.chat(sessionId, userMessage);

        stream
            .onPartialResponse(token -> callback.onChunk(token))
            .onToolExecuted(exec -> {
                callback.onToolStart(exec.request().name(), exec.request().arguments());
                // Tool execution happens...
                callback.onToolComplete(exec.request().name(), exec.result());
            })
            .onComplete(response -> {
                callback.onComplete();
            })
            .onError(error -> {
                callback.onError(new Exception(error.getMessage()));
            })
            .start();
    }
}
```

### Implementation Phases

| Phase | Description | Files | Effort |
|-------|-------------|-------|--------|
| 1 | Enhanced `AIStreamCallback` interface | `dto/AIStreamCallback.java` | 0.5 day |
| 2 | `AIChatStreamingMessage` component | `component/AIChatStreamingMessage.java` | 2 days |
| 3 | CSS animations and styling | Theme CSS files | 0.5 day |
| 4 | Update `AIChatWidget.sendMessage()` | `component/AIChatWidget.java` | 1 day |
| 5 | Streaming service layer | `service/LangChain4jConversationService.java` | 1.5 days |
| 6 | Extended thinking support (if model supports) | Callback handling | 0.5 day |
| 7 | Testing and polish | Test classes | 1 day |

**Total Effort:** 7 days

### User Experience Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  User sends: "What are my top 5 overdue invoices?"                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  [Thinking...]  ● ● ●                                                       │
│                                                                             │
│  ▸ Thinking (click to expand)                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ▸ Thinking (click to expand)                                              │
│                                                                             │
│  ┌─ Tools ────────────────────────────────────────────────────────────┐    │
│  │  ⟳ Querying database...                                            │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  |                                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ▸ Thinking (click to expand)                                              │
│                                                                             │
│  ┌─ Tools ────────────────────────────────────────────────────────────┐    │
│  │  ✓ Querying database                                               │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  Here are your top 5 overdue invoices:|                                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  ▸ Thinking (click to expand)                                              │
│                                                                             │
│  ┌─ Tools ────────────────────────────────────────────────────────────┐    │
│  │  ✓ Querying database                                               │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
│  Here are your top 5 overdue invoices:                                     │
│                                                                             │
│  | Invoice    | Customer   | Amount    | Days Overdue |                    │
│  |------------|------------|-----------|--------------|                    │
│  | INV-1234   | Acme Corp  | $12,500   | 45           |                    │
│  | INV-1198   | TechCo     | $8,900    | 32           |                    │
│  | INV-1201   | GlobalInc  | $6,200    | 28           |                    │
│  | INV-1189   | StartupXYZ | $4,100    | 21           |                    │
│  | INV-1205   | MegaCorp   | $3,800    | 14           |                    │
│                                                                             │
│  Total overdue: **$35,500**                                                │
│                                                                             │
│  [Copy]                                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Related ADRs

- [ADR-015](015-conversational-ux-patterns.md) - Conversational UX Patterns (response formatting)
- [ADR-031](031-chat-panel-langchain4j-chatmodel-integration.md) - Chat Panel LangChain4j Integration (streaming backend)
- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j Strategic Adoption (TokenStream API)
- [ADR-013](013-observability-cost-tracking.md) - Observability and Cost Tracking (streaming metrics)

### References

- [Claude Desktop/Web App](https://claude.ai) - Reference UX implementation
- [LangChain4j Response Streaming](https://docs.langchain4j.dev/tutorials/response-streaming)
- [ZK Executions.schedule()](https://www.zkoss.org/wiki/ZK_Developer's_Reference/UI_Patterns/Long_Operations)
- [Anthropic Extended Thinking](https://docs.anthropic.com/claude/docs/extended-thinking)

---

*ADR-033 | Version 1.0 | 2025-12-03*
