# Chat Panel LangChain4j ChatModel Integration - Implementation Plan

**ADR Reference:** [ADR-031](../../docs/adr/031-chat-panel-langchain4j-chatmodel-integration.md)
**Created:** 2025-12-03
**Status:** Planned
**Branch:** `langchain`

---

## Executive Summary

This plan details the migration of `AIChatWidget` from the custom `AIConversationService` to LangChain4j's `ChatLanguageModel` via a new `LangChain4jConversationService` facade. The migration preserves critical iDempiere-specific features (intelligent routing, query caching, thread filtering) while leveraging LangChain4j's simplicity.

---

## Priority Matrix

### Priority Levels

| Priority | Meaning | Criteria |
|----------|---------|----------|
| **P0 - Critical** | Must have for MVP | Blocks core functionality, no workaround |
| **P1 - High** | Should have for MVP | Significant value, minor workarounds exist |
| **P2 - Medium** | Nice to have | Enhances experience, can defer |
| **P3 - Low** | Future enhancement | Optional, implement when time permits |

### Prioritized Task Overview

| Task | Priority | Phase | Dependencies | Value | Effort |
|------|----------|-------|--------------|-------|--------|
| **ThreadAwareChatMemory** | P0 | 1.1 | None | Critical for thread isolation | 0.5 day |
| **LangChain4jConversationService** | P0 | 2.1 | 1.1 | Core integration facade | 1.5 days |
| **Update AIChatWidget** | P0 | 4.1 | 2.1 | Enable new path | 0.5 day |
| **Basic unit tests** | P0 | 7.1 | 1.1, 2.1 | Ensure correctness | 1 day |
| **IDempiereAIService thread support** | P1 | 3.1 | 1.1 | Thread memory in service | 0.5 day |
| **Feature flag rollout** | P1 | Rollout | 4.1 | Safe deployment | 0.5 day |
| **TokenUsageListener** | P1 | 1.3a | None | Cost tracking | 0.25 day |
| **LatencyMetricsListener** | P2 | 1.3b | None | Performance tracking | 0.25 day |
| **IDempiereAuditListener** | P2 | 1.3c | None | Audit logging | 0.25 day |
| **Provider factory listeners** | P2 | 5.1 | 1.3 | Wire up observability | 0.5 day |
| **IDempiereStreamingAgent** | P2 | 1.2 | None | Streaming interface | 0.25 day |
| **Streaming in service** | P2 | 3.2 | 1.2 | Streaming support | 0.5 day |
| **Streaming in widget** | P2 | 4.2 | 3.2 | Real-time UI | 1 day |
| **Integration tests** | P2 | 7.2 | 4.1 | End-to-end validation | 1 day |
| **Deprecate AIConversationService** | P3 | 6.1 | 4.1 | Code cleanup | 0.25 day |
| **Manual test checklist** | P3 | 7.3 | 4.1 | QA validation | 0.5 day |

### Critical Path (P0 Tasks)

```
ThreadAwareChatMemory (0.5d)
         │
         ▼
LangChain4jConversationService (1.5d)
         │
         ▼
Update AIChatWidget (0.5d)
         │
         ▼
Basic Unit Tests (1d)
         │
         ▼
─────────────────────────
MVP READY (3.5 days)
─────────────────────────
```

### Recommended Implementation Order

#### Sprint 1: MVP (P0) - 3.5 days
1. ✅ ThreadAwareChatMemory
2. ✅ LangChain4jConversationService (basic)
3. ✅ Update AIChatWidget
4. ✅ Basic unit tests

#### Sprint 2: Production Ready (P1) - 2 days
5. ✅ IDempiereAIService thread support
6. ✅ Feature flag rollout mechanism
7. ✅ TokenUsageListener (cost tracking)

#### Sprint 3: Enhanced (P2) - 3 days
8. ✅ LatencyMetricsListener
9. ✅ IDempiereAuditListener
10. ✅ Provider factory listener wiring
11. ✅ Streaming support (full stack)
12. ✅ Integration tests

#### Sprint 4: Cleanup (P3) - 1 day
13. ✅ Deprecate old service
14. ✅ Manual testing & documentation

---

## Phase 1: Foundation Components

### 1.1 Create ThreadAwareChatMemory [P0 - Critical]

**File:** `src/com/cloudempiere/ai/provider/langchain4j/ThreadAwareChatMemory.java`

**Priority Justification:** Core blocker - LangChain4j's default memory doesn't support thread filtering. Without this, multi-thread conversations break.

**Purpose:** Custom ChatMemory implementation that supports multi-thread conversations with thread-specific message filtering.

**Key Features:**
- Thread isolation via memory ID
- Configurable max messages per thread
- Thread switching without losing other thread history
- New thread creation

**Implementation:**

```java
package com.cloudempiere.ai.provider.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadAwareChatMemory implements ChatMemory {

    private final int maxMessages;
    private final Map<String, LinkedList<ChatMessage>> threadMessages;
    private volatile String currentThreadId;

    public ThreadAwareChatMemory(int maxMessages) {
        this.maxMessages = maxMessages;
        this.threadMessages = new ConcurrentHashMap<>();
        this.currentThreadId = "default";
    }

    @Override
    public Object id() {
        return currentThreadId;
    }

    @Override
    public void add(ChatMessage message) {
        threadMessages.computeIfAbsent(currentThreadId, k -> new LinkedList<>())
            .addLast(message);
        trimToMaxSize(currentThreadId);
    }

    @Override
    public List<ChatMessage> messages() {
        return new ArrayList<>(
            threadMessages.getOrDefault(currentThreadId, new LinkedList<>())
        );
    }

    @Override
    public void clear() {
        threadMessages.remove(currentThreadId);
    }

    // Thread management
    public void switchThread(String threadId) {
        this.currentThreadId = threadId;
    }

    public void createNewThread(String threadId) {
        this.currentThreadId = threadId;
        threadMessages.put(threadId, new LinkedList<>());
    }

    public Set<String> getThreadIds() {
        return threadMessages.keySet();
    }

    private void trimToMaxSize(String threadId) {
        LinkedList<ChatMessage> messages = threadMessages.get(threadId);
        while (messages != null && messages.size() > maxMessages) {
            messages.removeFirst();
        }
    }
}
```

**Tests:** `test/com/cloudempiere/ai/provider/langchain4j/ThreadAwareChatMemoryTest.java`

---

### 1.2 Create IDempiereStreamingAgent Interface [P2 - Medium]

**File:** `src/com/cloudempiere/ai/provider/langchain4j/IDempiereStreamingAgent.java`

**Priority Justification:** Enhancement - Streaming improves UX but sync responses work fine. Can defer to Sprint 3.

**Purpose:** Agent interface with streaming support for real-time response delivery to UI.

**Implementation:**

```java
package com.cloudempiere.ai.provider.langchain4j;

import dev.langchain4j.service.*;

@SystemMessage(IDempiereAgent.SYSTEM_PROMPT)
public interface IDempiereStreamingAgent {

    /**
     * Stream chat response token by token.
     */
    TokenStream chat(@MemoryId String sessionId, @UserMessage String message);

    /**
     * Execute one-shot task with streaming.
     */
    TokenStream execute(@UserMessage String goal);
}
```

---

### 1.3 Create Observability Listeners [P1/P2]

**Directory:** `src/com/cloudempiere/ai/provider/langchain4j/listener/`

**Priority Breakdown:**
- TokenUsageListener: **P1** - Critical for cost tracking/billing
- LatencyMetricsListener: **P2** - Nice for performance monitoring
- IDempiereAuditListener: **P2** - Nice for compliance

#### TokenUsageListener.java [P1 - High]
```java
package com.cloudempiere.ai.provider.langchain4j.listener;

import dev.langchain4j.model.chat.listener.*;
import org.compiere.util.CLogger;

public class TokenUsageListener implements ChatModelListener {

    private static final CLogger log = CLogger.getCLogger(TokenUsageListener.class);

    @Override
    public void onRequest(ChatModelRequestContext context) {
        // Log request start
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        var usage = context.response().tokenUsage();
        if (usage != null) {
            log.info("Tokens: input=" + usage.inputTokenCount() +
                     ", output=" + usage.outputTokenCount() +
                     ", total=" + usage.totalTokenCount());
        }
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        log.severe("AI Error: " + context.error().getMessage());
    }
}
```

#### LatencyMetricsListener.java [P2 - Medium]
```java
package com.cloudempiere.ai.provider.langchain4j.listener;

import dev.langchain4j.model.chat.listener.*;
import org.compiere.util.CLogger;

public class LatencyMetricsListener implements ChatModelListener {

    private static final CLogger log = CLogger.getCLogger(LatencyMetricsListener.class);
    private long startTime;

    @Override
    public void onRequest(ChatModelRequestContext context) {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("AI Response latency: " + duration + "ms");
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        long duration = System.currentTimeMillis() - startTime;
        log.warning("AI Error after " + duration + "ms: " + context.error().getMessage());
    }
}
```

#### IDempiereAuditListener.java [P2 - Medium]
```java
package com.cloudempiere.ai.provider.langchain4j.listener;

import dev.langchain4j.model.chat.listener.*;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import java.util.Properties;

public class IDempiereAuditListener implements ChatModelListener {

    private static final CLogger log = CLogger.getCLogger(IDempiereAuditListener.class);
    private final Properties ctx;

    public IDempiereAuditListener(Properties ctx) {
        this.ctx = ctx;
    }

    @Override
    public void onRequest(ChatModelRequestContext context) {
        int userId = Env.getAD_User_ID(ctx);
        int clientId = Env.getAD_Client_ID(ctx);
        log.fine("AI Request: user=" + userId + ", client=" + clientId);
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        // Could persist to audit table if needed
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        int userId = Env.getAD_User_ID(ctx);
        log.severe("AI Error for user " + userId + ": " + context.error().getMessage());
    }
}
```

---

## Phase 2: LangChain4jConversationService [P0 - Critical]

### 2.1 Create Service Facade [P0 - Critical]

**File:** `src/com/cloudempiere/ai/service/LangChain4jConversationService.java`

**Priority Justification:** Core component - This is the main integration point. Everything depends on this.

**Purpose:** Bridge between AIChatWidget and LangChain4j, preserving intelligent routing and caching.

**Implementation Outline:**

```java
package com.cloudempiere.ai.service;

import java.time.Duration;
import java.util.Properties;
import org.compiere.util.CLogger;
import org.json.JSONObject;

import com.cloudempiere.ai.model.MAIChat;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.*;
import com.cloudempiere.ai.routing.*;

public class LangChain4jConversationService {

    private static final CLogger log = CLogger.getCLogger(LangChain4jConversationService.class);

    private static final int DEFAULT_PROVIDER_ID = 1000001;
    private static final int DEFAULT_MEMORY_SIZE = 20;

    private final IDempiereAIService aiService;
    private final PromptAnalyzer promptAnalyzer;
    private final ConversationContextManager contextManager;
    private final RoutingMetrics routingMetrics;

    public LangChain4jConversationService() {
        this.aiService = IDempiereAIService.getInstance();
        this.contextManager = new ConversationContextManager(
            Duration.ofMinutes(30), 50
        );
        this.promptAnalyzer = new PromptAnalyzer(new EntityExtractor());
        this.routingMetrics = new RoutingMetrics();
    }

    /**
     * Send message with full context support.
     */
    public String sendMessage(
        Properties ctx,
        MAIChat chat,
        String userMessage,
        JSONObject contextData,
        int threadRootId,
        String trxName
    ) {
        long startTime = System.currentTimeMillis();

        try {
            // Set thread context for cache isolation
            contextManager.setCurrentThreadRootId(threadRootId);

            // Analyze prompt for routing decision
            SourceDecision decision = promptAnalyzer.analyzePrompt(
                userMessage, contextManager
            );

            log.fine("Routing: " + decision.getSource() + " - " + decision.getReasoning());

            // Handle CONTEXT_ONLY (fastest path - no AI call)
            if (decision.getSource() == SourceDecision.DataSource.CONTEXT_ONLY) {
                routingMetrics.recordContextOnlyResponse();
                return buildContextOnlyResponse(decision);
            }

            // Track routing metrics
            if (decision.getSource() == SourceDecision.DataSource.HYBRID) {
                routingMetrics.recordHybridQuery();
            } else {
                routingMetrics.recordDatabaseQuery();
            }

            // Build session ID from chat + thread
            String sessionId = buildSessionId(chat, threadRootId);

            // Get provider
            MAIProvider provider = getProvider(ctx, DEFAULT_PROVIDER_ID, trxName);
            if (provider == null) {
                throw new RuntimeException("AI Provider not found: " + DEFAULT_PROVIDER_ID);
            }

            // Call LangChain4j via IDempiereAIService
            return aiService.chat(provider, ctx, sessionId, userMessage);

        } catch (Exception e) {
            log.severe("sendMessage failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            log.fine("Total processing time: " + elapsed + "ms");
        }
    }

    /**
     * Send message with streaming callback.
     */
    public void sendMessageStreaming(
        Properties ctx,
        MAIChat chat,
        String userMessage,
        JSONObject contextData,
        int threadRootId,
        StreamingCallback callback,
        String trxName
    ) {
        // Similar logic but uses IDempiereStreamingAgent
        // Calls callback.onToken(), callback.onComplete(), callback.onError()
    }

    /**
     * Create new conversation thread.
     */
    public void createNewThread(MAIChat chat, int threadRootId) {
        String sessionId = buildSessionId(chat, threadRootId);
        aiService.clearMemory(sessionId);
    }

    /**
     * Clear conversation memory for a thread.
     */
    public void clearThread(MAIChat chat, int threadRootId) {
        String sessionId = buildSessionId(chat, threadRootId);
        aiService.clearMemory(sessionId);
    }

    // Helper methods
    private String buildSessionId(MAIChat chat, int threadRootId) {
        return "chat-" + chat.getCM_Chat_ID() + "-thread-" + threadRootId;
    }

    private String buildContextOnlyResponse(SourceDecision decision) {
        return decision.getCachedResult() != null
            ? decision.getCachedResult()
            : "I can answer this from context: " + decision.getReasoning();
    }

    private MAIProvider getProvider(Properties ctx, int providerId, String trxName) {
        return new MAIProvider(ctx, providerId, trxName);
    }

    public RoutingMetrics getRoutingMetrics() {
        return routingMetrics;
    }

    /**
     * Callback interface for streaming responses.
     */
    public interface StreamingCallback {
        void onToken(String token);
        void onComplete(String fullResponse);
        void onError(Throwable error);
    }
}
```

---

## Phase 3: Update IDempiereAIService [P1 - High]

### 3.1 Add Thread-Aware Memory Support [P1 - High]

**File:** `src/com/cloudempiere/ai/provider/langchain4j/IDempiereAIService.java`

**Priority Justification:** Important for proper thread isolation in the service layer. MVP can work with basic implementation, but production needs this.

**Changes:**
- Replace `MessageWindowChatMemory` with `ThreadAwareChatMemory`
- Add streaming support via `IDempiereStreamingAgent`
- Add listener configuration

```java
// Add to IDempiereAIService.java

/** Thread-aware memory cache by session ID */
private final Map<String, ThreadAwareChatMemory> threadMemoryCache = new ConcurrentHashMap<>();

/**
 * Chat with thread support.
 */
public String chat(MAIProvider provider, Properties ctx, String sessionId, String threadId, String message) {
    ThreadAwareChatMemory memory = threadMemoryCache.computeIfAbsent(sessionId,
        id -> new ThreadAwareChatMemory(DEFAULT_MEMORY_SIZE));

    memory.switchThread(threadId);

    ChatLanguageModel model = createModelWithListeners(provider, ctx);
    ERPTools tools = new ERPTools(provider, ctx);

    IDempiereAgent agent = AiServices.builder(IDempiereAgent.class)
        .chatLanguageModel(model)
        .tools(tools)
        .chatMemory(memory)
        .build();

    return agent.chat(sessionId, message);
}

/**
 * Streaming chat.
 */
public void chatStreaming(
    MAIProvider provider,
    Properties ctx,
    String sessionId,
    String message,
    Consumer<String> onToken,
    Consumer<String> onComplete,
    Consumer<Throwable> onError
) {
    StreamingChatLanguageModel model = LangChain4jProviderFactory.createStreaming(provider, null, null);
    ThreadAwareChatMemory memory = threadMemoryCache.computeIfAbsent(sessionId,
        id -> new ThreadAwareChatMemory(DEFAULT_MEMORY_SIZE));

    ERPTools tools = new ERPTools(provider, ctx);

    IDempiereStreamingAgent agent = AiServices.builder(IDempiereStreamingAgent.class)
        .streamingChatLanguageModel(model)
        .tools(tools)
        .chatMemory(memory)
        .build();

    TokenStream stream = agent.chat(sessionId, message);
    StringBuilder fullResponse = new StringBuilder();

    stream
        .onPartialResponse(token -> {
            fullResponse.append(token);
            onToken.accept(token);
        })
        .onComplete(response -> onComplete.accept(fullResponse.toString()))
        .onError(onError)
        .start();
}

private ChatLanguageModel createModelWithListeners(MAIProvider provider, Properties ctx) {
    // Get base model
    ChatLanguageModel baseModel = LangChain4jProviderFactory.create(provider);

    // Note: Listeners are configured in LangChain4jProviderFactory
    // This method could add context-specific configuration if needed
    return baseModel;
}
```

---

## Phase 4: Update AIChatWidget [P0 - Critical]

### 4.1 Switch to LangChain4jConversationService [P0 - Critical]

**File:** `src/com/cloudempiere/ai/component/AIChatWidget.java`

**Priority Justification:** This enables the new LangChain4j path in the UI. Without this, all other work is unused.

**Changes:**

```java
// Replace
private AIConversationService aiService;

// With
private LangChain4jConversationService aiService;

// In init()
// Replace
aiService = new AIConversationService();

// With
aiService = new LangChain4jConversationService();

// In sendMessage async block
// Replace
AIResponse aiResponse = aiService.sendMessageWithContext(
    sessionCtx, chat, userMessage, currentContext,
    DEFAULT_MAX_HISTORY, currentThreadRootId, null
);
String content = aiResponse.getContent();

// With
String content = aiService.sendMessage(
    sessionCtx, (MAIChat)chat, userMessage, currentContext,
    currentThreadRootId, null
);

// Add streaming support (optional enhancement)
private void sendMessageStreaming(String userMessage) {
    aiService.sendMessageStreaming(
        sessionCtx,
        (MAIChat)chat,
        userMessage,
        currentContext,
        currentThreadRootId,
        new LangChain4jConversationService.StreamingCallback() {
            @Override
            public void onToken(String token) {
                Executions.schedule(desktop, e -> appendTokenToUI(token), new Event("onToken"));
            }

            @Override
            public void onComplete(String fullResponse) {
                Executions.schedule(desktop, e -> finalizeMessage(fullResponse), new Event("onComplete"));
            }

            @Override
            public void onError(Throwable error) {
                Executions.schedule(desktop, e -> showError(error), new Event("onError"));
            }
        },
        null
    );
}
```

---

## Phase 5: Update LangChain4jProviderFactory [P2 - Medium]

### 5.1 Add Listener Support [P2 - Medium]

**File:** `src/com/cloudempiere/ai/provider/langchain4j/LangChain4jProviderFactory.java`

**Priority Justification:** Depends on listeners from Phase 1.3. Enhances observability but not required for MVP.

**Changes:**

```java
// Add to factory methods

private static ChatLanguageModel createAnthropicModel(String apiKey, String modelName) {
    return AnthropicChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName != null ? modelName : DEFAULT_ANTHROPIC_MODEL)
        .maxTokens(4096)
        .temperature(0.7)
        .logRequests(true)
        .logResponses(true)
        .listeners(List.of(
            new TokenUsageListener(),
            new LatencyMetricsListener()
        ))
        .build();
}

// Similar changes for other provider methods
```

---

## Phase 6: Deprecate Old Components [P3 - Low]

### 6.1 Mark AIConversationService as Deprecated [P3 - Low]

**File:** `src/com/cloudempiere/ai/service/AIConversationService.java`

**Priority Justification:** Cleanup task. Old code still works as fallback. Can defer to post-MVP.

```java
/**
 * @deprecated Use {@link LangChain4jConversationService} instead.
 * Will be removed in v1.0.0.
 */
@Deprecated
public class AIConversationService {
    // ... existing code ...
}
```

---

## Phase 7: Testing [P0/P2/P3]

### 7.1 Unit Tests [P0 - Critical]

**Priority Justification:** Essential for MVP - validates core components work correctly.

| Test Class | Purpose |
|------------|---------|
| ThreadAwareChatMemoryTest | Thread isolation, max messages, switching |
| LangChain4jConversationServiceTest | Routing decisions, caching, session management |
| IDempiereAIServiceThreadTest | Thread-aware memory integration |

### 7.2 Integration Tests [P2 - Medium]

**Priority Justification:** Nice to have for confidence, but unit tests + manual testing sufficient for MVP.

| Test | Purpose |
|------|---------|
| AIChatWidgetIntegrationTest | End-to-end chat flow |
| StreamingResponseTest | Streaming delivery to UI |
| MultiThreadConversationTest | Thread switching, isolation |

### 7.3 Manual Testing Checklist [P3 - Low]

**Priority Justification:** QA validation, can be done post-MVP or by QA team.

- [ ] Send message and receive response
- [ ] Create new thread
- [ ] Switch between threads
- [ ] Verify thread messages isolated
- [ ] Test context indicator updates
- [ ] Test markdown rendering
- [ ] Test zoom links
- [ ] Test streaming (if enabled)
- [ ] Test error handling
- [ ] Verify cost/token logging

---

## File Summary

### New Files

| File | Lines (est.) | Purpose |
|------|--------------|---------|
| `ThreadAwareChatMemory.java` | ~80 | Thread-aware memory |
| `IDempiereStreamingAgent.java` | ~20 | Streaming interface |
| `LangChain4jConversationService.java` | ~200 | Service facade |
| `TokenUsageListener.java` | ~40 | Cost tracking |
| `LatencyMetricsListener.java` | ~40 | Performance tracking |
| `IDempiereAuditListener.java` | ~50 | Audit logging |
| `ThreadAwareChatMemoryTest.java` | ~100 | Unit tests |
| `LangChain4jConversationServiceTest.java` | ~150 | Integration tests |

**Total New Code:** ~680 lines

### Modified Files

| File | Changes |
|------|---------|
| `AIChatWidget.java` | Switch to new service (~20 lines changed) |
| `IDempiereAIService.java` | Add thread/streaming support (~80 lines added) |
| `LangChain4jProviderFactory.java` | Add listener config (~30 lines changed) |
| `AIConversationService.java` | Add @Deprecated |

### Deprecated Files (v1.0.0 removal)

- `AIConversationService.java` (1226 lines)
- Custom DTOs if no longer needed

---

## Rollout Strategy [P1 - High]

**Priority Justification:** Feature flags are critical for safe production deployment. Enables gradual rollout and instant rollback.

### Step 1: Feature Flag [P1 - High]

Add configuration to enable/disable LangChain4j path:

```java
// In AIChatWidget or system config
boolean useLangChain4j = MSysConfig.getBooleanValue(
    "AI_USE_LANGCHAIN4J", true, Env.getAD_Client_ID(ctx)
);

if (useLangChain4j) {
    aiService = new LangChain4jConversationService();
} else {
    aiService = new AIConversationService(); // Legacy
}
```

### Step 2: Parallel Testing

Run both services for comparison logging:

```java
String legacyResult = legacyService.sendMessage(...);
String langchainResult = langchainService.sendMessage(...);
log.info("Results match: " + legacyResult.equals(langchainResult));
```

### Step 3: Gradual Rollout

1. Internal testing (dev environment)
2. Staging environment
3. Production with feature flag (10% → 50% → 100%)
4. Remove legacy code in v1.0.0

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Response latency | ≤ current | LatencyMetricsListener |
| Code reduction | ≥1000 lines | Line count before/after |
| Test coverage | ≥80% | JaCoCo |
| Error rate | ≤ current | Error logging |
| Cost tracking accuracy | 100% | TokenUsageListener |

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| LangChain4j API changes | Pin version, monitor releases |
| Thread memory leaks | Implement cleanup on session end |
| Streaming complexity | Feature flag, fallback to sync |
| Performance regression | Benchmark before/after |
| Missing features | Retain routing/caching layers |

---

## Timeline Estimate (By Priority)

### MVP Timeline (P0 Only) - 3.5 days

| Task | Priority | Duration |
|------|----------|----------|
| ThreadAwareChatMemory | P0 | 0.5 day |
| LangChain4jConversationService | P0 | 1.5 days |
| Update AIChatWidget | P0 | 0.5 day |
| Basic Unit Tests | P0 | 1 day |
| **MVP Total** | | **3.5 days** |

### Production Ready (P0 + P1) - 5.5 days

| Task | Priority | Duration |
|------|----------|----------|
| MVP (above) | P0 | 3.5 days |
| IDempiereAIService thread support | P1 | 0.5 day |
| Feature flag rollout | P1 | 0.5 day |
| TokenUsageListener | P1 | 0.25 day |
| Buffer | | 0.75 day |
| **Production Total** | | **5.5 days** |

### Full Implementation (P0 + P1 + P2) - 8.5 days

| Task | Priority | Duration |
|------|----------|----------|
| Production Ready (above) | P0+P1 | 5.5 days |
| LatencyMetricsListener | P2 | 0.25 day |
| IDempiereAuditListener | P2 | 0.25 day |
| Provider factory listeners | P2 | 0.5 day |
| Streaming (full stack) | P2 | 1 day |
| Integration tests | P2 | 1 day |
| **Full Total** | | **8.5 days** |

### Complete with Cleanup (All) - 9.5 days

| Task | Priority | Duration |
|------|----------|----------|
| Full Implementation (above) | P0-P2 | 8.5 days |
| Deprecate old service | P3 | 0.25 day |
| Manual test checklist | P3 | 0.5 day |
| Documentation | P3 | 0.25 day |
| **Complete Total** | | **9.5 days** |

---

## Quick Start: MVP in 3.5 Days

For rapid delivery, focus only on P0 tasks:

```
Day 1 (Morning):  ThreadAwareChatMemory
Day 1 (Afternoon): LangChain4jConversationService (start)
Day 2 (Full):     LangChain4jConversationService (complete)
Day 3 (Morning):  Update AIChatWidget
Day 3 (Afternoon): Basic Unit Tests
Day 4 (Morning):  Testing & Bug Fixes
```

**What you get with MVP:**
- ✅ Chat panel using LangChain4j ChatModel
- ✅ Multi-thread conversation support
- ✅ Intelligent routing preserved
- ✅ Query caching preserved
- ✅ Basic unit test coverage

**What's deferred:**
- ❌ Streaming responses (uses sync)
- ❌ Cost/latency observability
- ❌ Audit logging
- ❌ Old code deprecation

---

*Implementation Plan v1.1 | 2025-12-03 | Updated with Priority Matrix*
