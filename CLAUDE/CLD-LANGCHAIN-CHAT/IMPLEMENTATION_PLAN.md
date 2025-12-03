# Chat Panel LangChain4j ChatModel Integration - Implementation Plan

**ADR Reference:** [ADR-031](../../docs/adr/031-chat-panel-langchain4j-chatmodel-integration.md)
**Created:** 2025-12-03
**Updated:** 2025-12-03
**Status:** In Progress
**Branch:** `langchain`

---

## Executive Summary

This plan details the migration of `AIChatWidget` from the custom `AIConversationService` to LangChain4j's `ChatLanguageModel` via a new `LangChain4jConversationService` facade. The migration preserves critical iDempiere-specific features (intelligent routing, query caching, thread filtering) while leveraging LangChain4j's simplicity.

---

## Current State Assessment (2025-12-03)

### What's Already Implemented

| Component | ADR | Status | Location |
|-----------|-----|--------|----------|
| **RAGContextManager** | ADR-012 | ✅ Done | `src/.../rag/RAGContextManager.java` |
| **RAGConversationService** | ADR-012 | ✅ Done | `src/.../rag/RAGConversationService.java` |
| **AgentBoundary** | ADR-009 | ✅ Done | `src/.../boundary/AgentBoundary.java` |
| **AgentBoundaryRegistry** | ADR-009 | ✅ Done | `src/.../boundary/AgentBoundaryRegistry.java` |
| **BoundaryEnforcementFilter** | ADR-009 | ✅ Done | `src/.../boundary/BoundaryEnforcementFilter.java` |
| **CostBoundaryMonitor** | ADR-009 | ✅ Done | `src/.../boundary/CostBoundaryMonitor.java` |
| **DataAccessValidator** | ADR-009 | ✅ Done | `src/.../boundary/DataAccessValidator.java` |
| **LangChain4jProviderFactory** | ADR-002 | ✅ Done | `src/.../langchain4j/LangChain4jProviderFactory.java` |
| **Context/Database Tests** | ADR-032 | ✅ Done | `src/test/.../` |

### What's NOT Yet Implemented (Gaps)

| Component | ADR | Status | Blocking |
|-----------|-----|--------|----------|
| **ThreadAwareChatMemory** | ADR-031 | ❌ Not Started | MVP |
| **LangChain4jConversationService** | ADR-031 | ❌ Not Started | MVP |
| **Update AIChatWidget** | ADR-031 | ❌ Not Started | MVP |
| **Wire RAGConversationService to Widget** | ADR-012 | ❌ Not Started | Integration |
| **RAGContextManager Tests** | ADR-012 | ❌ Not Started | Quality |
| **Boundary Component Tests** | ADR-009 | ❌ Not Started | Quality |
| **Remove Old Routing Code** | ADR-012 | ❌ Not Started | Cleanup |
| **Time-Based Boundaries** | ADR-009 | ⚠️ Partial | Security |

### Key Decision: RAG vs Custom Routing

**ADR-031 Plan vs ADR-012 Implementation Conflict:**

The original ADR-031 plan references `PromptAnalyzer` and `ConversationContextManager` (custom routing).
However, ADR-012 supersedes this with RAG-based context retrieval.

**Resolution:** Use `RAGConversationService` instead of creating `LangChain4jConversationService` with custom routing.

```
OLD PATH (ADR-031 original):
AIChatWidget → LangChain4jConversationService → PromptAnalyzer → IDempiereAIService

NEW PATH (ADR-012 aligned):
AIChatWidget → RAGConversationService → RAGContextManager → IDempiereAIService
```

---

## Revised Priority Matrix

### Priority Levels

| Priority | Meaning | Criteria |
|----------|---------|----------|
| **P0 - Critical** | Must have for MVP | Blocks core functionality |
| **P1 - High** | Should have for MVP | Significant value |
| **P2 - Medium** | Nice to have | Enhances experience |
| **P3 - Low** | Future enhancement | Optional |

### Prioritized Task Overview (Revised)

| Task | Priority | Status | Dependencies | Notes |
|------|----------|--------|--------------|-------|
| **ThreadAwareChatMemory** | P0 | ❌ TODO | None | Thread isolation |
| **Wire RAGConversationService to AIChatWidget** | P0 | ❌ TODO | ThreadAwareChatMemory | Replaces LangChain4jConversationService plan |
| **Basic unit tests (RAG + Boundary)** | P0 | ❌ TODO | Above | Quality gate |
| **Feature flag rollout** | P1 | ❌ TODO | Widget wiring | Safe deployment |
| **TokenUsageListener** | P1 | ❌ TODO | None | Cost tracking |
| **Remove old routing code** | P1 | ❌ TODO | Widget wiring | Per ADR-012 |
| **Time-based boundaries** | P2 | ⚠️ Partial | None | Period restrictions |
| **LatencyMetricsListener** | P2 | ❌ TODO | None | Performance |
| **IDempiereAuditListener** | P2 | ❌ TODO | None | Compliance |
| **Streaming support** | P2 | ❌ TODO | Widget wiring | UX enhancement |
| **Integration tests** | P2 | ❌ TODO | All above | E2E validation |
| **Deprecate AIConversationService** | P3 | ❌ TODO | All above | Cleanup |

---

## Revised Implementation Plan

### Sprint 1: MVP Core (P0) - 3 days

#### 1.1 Create ThreadAwareChatMemory [P0]

**File:** `src/com/cloudempiere/ai/provider/langchain4j/ThreadAwareChatMemory.java`

**Purpose:** Thread-isolated chat memory for multi-thread conversations.

```java
package com.cloudempiere.ai.provider.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-aware ChatMemory implementation (ADR-031).
 *
 * <p>Supports multi-thread conversations where each thread has isolated
 * message history while sharing the same session context.
 */
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

    /** Switch to existing thread */
    public void switchThread(String threadId) {
        this.currentThreadId = threadId;
    }

    /** Create new thread with empty history */
    public void createNewThread(String threadId) {
        this.currentThreadId = threadId;
        threadMessages.put(threadId, new LinkedList<>());
    }

    /** Get all thread IDs */
    public Set<String> getThreadIds() {
        return Collections.unmodifiableSet(threadMessages.keySet());
    }

    /** Clear all threads */
    public void clearAll() {
        threadMessages.clear();
        currentThreadId = "default";
    }

    private void trimToMaxSize(String threadId) {
        LinkedList<ChatMessage> messages = threadMessages.get(threadId);
        while (messages != null && messages.size() > maxMessages) {
            messages.removeFirst();
        }
    }
}
```

**Acceptance Criteria:**
- [ ] Thread isolation verified
- [ ] Max message trimming works
- [ ] Concurrent access safe
- [ ] Unit test passes

---

#### 1.2 Wire RAGConversationService to AIChatWidget [P0]

**File:** `src/com/cloudempiere/ai/component/AIChatWidget.java`

**Changes:** Replace `AIConversationService` with `RAGConversationService`.

**Before:**
```java
private AIConversationService aiService;
// ...
aiService = new AIConversationService();
// ...
AIResponse aiResponse = aiService.sendMessageWithContext(...);
String content = aiResponse.getContent();
```

**After:**
```java
private RAGConversationService aiService;
// ...
aiService = new RAGConversationService();
// ...
String content = aiService.sendMessageWithContext(
    sessionCtx, (MAIChat)chat, userMessage, currentContext,
    DEFAULT_MAX_HISTORY, null
);
```

**Feature Flag (for safe rollout):**
```java
// In AIChatWidget.init()
boolean useRAG = MSysConfig.getBooleanValue(
    "AI_USE_RAG_SERVICE", true, Env.getAD_Client_ID(ctx)
);

if (useRAG) {
    aiService = new RAGConversationService();
} else {
    aiService = new AIConversationService(); // Legacy fallback
}
```

**Acceptance Criteria:**
- [ ] Widget compiles with new service
- [ ] Chat responses work
- [ ] Thread switching works
- [ ] Context injection works

---

#### 1.3 Add Thread Support to RAGConversationService [P0]

**File:** `src/com/cloudempiere/ai/rag/RAGConversationService.java`

**Changes:** Integrate `ThreadAwareChatMemory` for thread isolation.

```java
// Add to RAGConversationService

/** Thread-aware memory cache by session */
private final Map<String, ThreadAwareChatMemory> sessionMemoryCache = new ConcurrentHashMap<>();

/** Default memory size per thread */
private static final int DEFAULT_MEMORY_SIZE = 20;

/**
 * Send message with thread support
 */
public String sendMessageWithContext(
        Properties ctx,
        MAIChat chat,
        String userMessage,
        JSONObject windowContext,
        int maxHistoryEntries,
        int threadRootId,  // NEW parameter
        String trxName) {

    String sessionId = getSessionId(chat);

    // Get or create thread-aware memory
    ThreadAwareChatMemory memory = sessionMemoryCache.computeIfAbsent(
        sessionId, k -> new ThreadAwareChatMemory(DEFAULT_MEMORY_SIZE));

    // Switch to correct thread
    String threadId = "thread_" + threadRootId;
    memory.switchThread(threadId);

    // ... rest of implementation using memory
}

/**
 * Create new conversation thread
 */
public void createNewThread(MAIChat chat, int threadRootId) {
    String sessionId = getSessionId(chat);
    ThreadAwareChatMemory memory = sessionMemoryCache.get(sessionId);
    if (memory != null) {
        memory.createNewThread("thread_" + threadRootId);
    }
}

/**
 * Clear thread memory
 */
public void clearThread(MAIChat chat, int threadRootId) {
    String sessionId = getSessionId(chat);
    ThreadAwareChatMemory memory = sessionMemoryCache.get(sessionId);
    if (memory != null) {
        memory.switchThread("thread_" + threadRootId);
        memory.clear();
    }
}
```

---

#### 1.4 Unit Tests [P0]

**New Test Files:**

| File | Purpose |
|------|---------|
| `ThreadAwareChatMemoryTest.java` | Thread isolation, trimming, concurrency |
| `RAGContextManagerTest.java` | Embedding store, retrieval, fallback |
| `AgentBoundaryTest.java` | Table/column allow/block |
| `BoundaryEnforcementFilterTest.java` | SQL injection, org filtering |

**ThreadAwareChatMemoryTest.java:**
```java
package com.cloudempiere.ai.provider.langchain4j;

import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThreadAwareChatMemoryTest {

    private ThreadAwareChatMemory memory;

    @BeforeEach
    void setUp() {
        memory = new ThreadAwareChatMemory(5);
    }

    @Test
    void testThreadIsolation() {
        // Add messages to thread 1
        memory.switchThread("thread1");
        memory.add(UserMessage.from("Hello from thread 1"));

        // Add messages to thread 2
        memory.switchThread("thread2");
        memory.add(UserMessage.from("Hello from thread 2"));

        // Verify isolation
        memory.switchThread("thread1");
        assertEquals(1, memory.messages().size());
        assertTrue(memory.messages().get(0).toString().contains("thread 1"));

        memory.switchThread("thread2");
        assertEquals(1, memory.messages().size());
        assertTrue(memory.messages().get(0).toString().contains("thread 2"));
    }

    @Test
    void testMaxMessageTrimming() {
        memory.switchThread("test");
        for (int i = 0; i < 10; i++) {
            memory.add(UserMessage.from("Message " + i));
        }
        assertEquals(5, memory.messages().size());
        assertTrue(memory.messages().get(0).toString().contains("Message 5"));
    }

    @Test
    void testClearThread() {
        memory.switchThread("thread1");
        memory.add(UserMessage.from("Test"));
        memory.clear();
        assertEquals(0, memory.messages().size());
    }
}
```

---

### Sprint 2: Production Ready (P1) - 2 days

#### 2.1 Feature Flag Configuration [P1]

**Database Setup:**
```sql
-- Add to AD_SysConfig
INSERT INTO AD_SysConfig (AD_SysConfig_ID, AD_Client_ID, AD_Org_ID, Name, Value, Description)
VALUES (nextval('ad_sysconfig_seq'), 0, 0, 'AI_USE_RAG_SERVICE', 'Y',
        'Use RAG-based conversation service (Y/N). Set N to use legacy AIConversationService.');
```

**Usage in code:**
```java
boolean useRAG = MSysConfig.getBooleanValue("AI_USE_RAG_SERVICE", true, clientId);
```

---

#### 2.2 TokenUsageListener [P1]

**File:** `src/com/cloudempiere/ai/provider/langchain4j/listener/TokenUsageListener.java`

```java
package com.cloudempiere.ai.provider.langchain4j.listener;

import dev.langchain4j.model.chat.listener.*;
import org.compiere.util.CLogger;
import java.math.BigDecimal;

/**
 * Tracks token usage and cost for AI requests (ADR-013).
 */
public class TokenUsageListener implements ChatModelListener {

    private static final CLogger log = CLogger.getCLogger(TokenUsageListener.class);

    // Pricing per 1M tokens (Claude claude-sonnet-4-20250514 as of 2025)
    private static final BigDecimal INPUT_COST_PER_M = new BigDecimal("3.00");
    private static final BigDecimal OUTPUT_COST_PER_M = new BigDecimal("15.00");

    @Override
    public void onRequest(ChatModelRequestContext context) {
        log.fine("AI Request started");
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        var usage = context.response().tokenUsage();
        if (usage != null) {
            int inputTokens = usage.inputTokenCount();
            int outputTokens = usage.outputTokenCount();

            BigDecimal inputCost = INPUT_COST_PER_M
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, BigDecimal.ROUND_HALF_UP);
            BigDecimal outputCost = OUTPUT_COST_PER_M
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, BigDecimal.ROUND_HALF_UP);
            BigDecimal totalCost = inputCost.add(outputCost);

            log.info(String.format(
                "AI Usage: input=%d, output=%d, total=%d tokens | Cost: $%.6f",
                inputTokens, outputTokens, usage.totalTokenCount(), totalCost
            ));

            // TODO: Persist to AIG_UsageMetrics table (ADR-013)
        }
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        log.severe("AI Error: " + context.error().getMessage());
    }
}
```

---

#### 2.3 Remove Old Routing Code [P1]

**Per ADR-012, remove these files (630+ lines):**

```
src/com/cloudempiere/ai/routing/
├── PromptAnalyzer.java          (120 lines) - DELETE
├── EntityExtractor.java         (140 lines) - DELETE
├── SourceDecision.java          (20 lines)  - DELETE
├── DataType.java                (20 lines)  - DELETE
├── TTLConfig.java               (80 lines)  - DELETE
└── RoutingMetrics.java          (100 lines) - DELETE

src/com/cloudempiere/ai/context/
├── ConversationContextManager.java (230 lines) - DELETE (keep others)
└── ContextEntry.java               (40 lines)  - DELETE
```

**Keep:** `AIContextProviderRegistry.java`, `IAIContextProvider.java`, `WindowContextProvider.java`, `ChartContextProvider.java`

**Verification:**
- [ ] No compile errors after deletion
- [ ] Widget still works via RAGConversationService
- [ ] Tests pass

---

### Sprint 3: Enhanced (P2) - 3 days

#### 3.1 Time-Based Boundaries [P2]

**File:** `src/com/cloudempiere/ai/boundary/TimeBoundaryValidator.java`

```java
package com.cloudempiere.ai.boundary;

import org.compiere.model.MPeriod;
import org.compiere.util.Env;
import java.sql.Timestamp;
import java.util.Properties;

/**
 * Time-based boundary validation (ADR-009).
 *
 * Enforces period restrictions:
 * - Current period: Can create/modify
 * - Closed periods: Read-only
 * - Future periods: Restricted
 */
public class TimeBoundaryValidator {

    /**
     * Check if a period allows modifications
     */
    public boolean canModifyPeriod(Properties ctx, Timestamp dateAcct) {
        MPeriod period = MPeriod.get(ctx, dateAcct,
            Env.getAD_Org_ID(ctx), null);

        if (period == null) {
            return false; // No period found
        }

        return period.isOpen(); // Only open periods
    }

    /**
     * Check document status allows modification
     */
    public boolean canModifyDocStatus(String docStatus) {
        // Only Draft and In Progress allowed
        return "DR".equals(docStatus) || "IP".equals(docStatus);
    }
}
```

---

#### 3.2 Observability Listeners [P2]

**LatencyMetricsListener.java:**
```java
package com.cloudempiere.ai.provider.langchain4j.listener;

import dev.langchain4j.model.chat.listener.*;
import org.compiere.util.CLogger;

public class LatencyMetricsListener implements ChatModelListener {

    private static final CLogger log = CLogger.getCLogger(LatencyMetricsListener.class);
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public void onRequest(ChatModelRequestContext context) {
        startTime.set(System.currentTimeMillis());
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        long duration = System.currentTimeMillis() - startTime.get();
        log.info("AI Response latency: " + duration + "ms");
        startTime.remove();
    }

    @Override
    public void onError(ChatModelErrorContext context) {
        long duration = System.currentTimeMillis() - startTime.get();
        log.warning("AI Error after " + duration + "ms: " + context.error().getMessage());
        startTime.remove();
    }
}
```

---

#### 3.3 Integration Tests [P2]

**RAGConversationServiceIntegrationTest.java:**
```java
package com.cloudempiere.ai.rag;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RAG-based conversation flow.
 */
class RAGConversationServiceIntegrationTest {

    @Test
    void testEndToEndConversation() {
        // Setup test context
        // Send message
        // Verify response
        // Verify context stored in RAG
    }

    @Test
    void testThreadIsolation() {
        // Create session with multiple threads
        // Send messages to different threads
        // Verify isolation
    }

    @Test
    void testContextRetrieval() {
        // Store context
        // Ask related question
        // Verify RAG retrieves correct context
    }
}
```

---

### Sprint 4: Cleanup (P3) - 1 day

#### 4.1 Deprecate AIConversationService [P3]

```java
/**
 * @deprecated Use {@link RAGConversationService} instead (ADR-012).
 * This class will be removed in v1.0.0.
 *
 * <p>Migration guide:
 * <pre>
 * // Before
 * AIConversationService service = new AIConversationService();
 * AIResponse response = service.sendMessageWithContext(...);
 * String content = response.getContent();
 *
 * // After
 * RAGConversationService service = new RAGConversationService();
 * String content = service.sendMessageWithContext(...);
 * </pre>
 */
@Deprecated
public class AIConversationService {
    // ... existing code ...
}
```

---

## File Summary

### New Files to Create

| File | Lines | Priority | Sprint |
|------|-------|----------|--------|
| `ThreadAwareChatMemory.java` | ~80 | P0 | 1 |
| `ThreadAwareChatMemoryTest.java` | ~100 | P0 | 1 |
| `RAGContextManagerTest.java` | ~150 | P0 | 1 |
| `AgentBoundaryTest.java` | ~100 | P0 | 1 |
| `TokenUsageListener.java` | ~50 | P1 | 2 |
| `TimeBoundaryValidator.java` | ~60 | P2 | 3 |
| `LatencyMetricsListener.java` | ~40 | P2 | 3 |
| `RAGConversationServiceIntegrationTest.java` | ~150 | P2 | 3 |

### Files to Modify

| File | Changes | Priority |
|------|---------|----------|
| `AIChatWidget.java` | Use RAGConversationService | P0 |
| `RAGConversationService.java` | Add thread support | P0 |
| `LangChain4jProviderFactory.java` | Add listeners | P2 |
| `AIConversationService.java` | Add @Deprecated | P3 |

### Files to Delete (Post-Migration)

| File | Lines | Priority |
|------|-------|----------|
| `routing/PromptAnalyzer.java` | 120 | P1 |
| `routing/EntityExtractor.java` | 140 | P1 |
| `routing/SourceDecision.java` | 20 | P1 |
| `routing/DataType.java` | 20 | P1 |
| `routing/TTLConfig.java` | 80 | P1 |
| `routing/RoutingMetrics.java` | 100 | P1 |
| `context/ConversationContextManager.java` | 230 | P1 |
| `context/ContextEntry.java` | 40 | P1 |
| **Total Removed** | **750** | |

---

## Timeline Summary

```
Sprint 1 (MVP Core):      3 days
├── ThreadAwareChatMemory          0.5 day
├── Wire RAGConversationService    1.0 day
├── Thread support in RAG          0.5 day
└── Unit tests                     1.0 day

Sprint 2 (Production Ready): 2 days
├── Feature flag                   0.5 day
├── TokenUsageListener             0.5 day
└── Remove old routing code        1.0 day

Sprint 3 (Enhanced):        3 days
├── Time boundaries                0.5 day
├── Latency listener               0.25 day
├── Streaming support              1.25 day
└── Integration tests              1.0 day

Sprint 4 (Cleanup):         1 day
├── Deprecate old service          0.25 day
├── Documentation                  0.5 day
└── Final testing                  0.25 day

TOTAL:                      9 days
```

---

## Success Criteria

| Metric | Target | Measurement |
|--------|--------|-------------|
| Widget works with RAG | ✅ | Manual test |
| Thread isolation | ✅ | Unit test |
| Code reduction | 750+ lines removed | Line count |
| Test coverage | ≥80% on new code | JaCoCo |
| Response latency | ≤ current | LatencyListener |
| Zero regressions | ✅ | All tests pass |

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| RAG service unavailable | Feature flag to fall back to legacy |
| Thread memory leaks | Implement session cleanup on logout |
| Performance regression | Benchmark before/after |
| Breaking changes | Feature flag for gradual rollout |

---

## Quick Reference: What to Implement Next

**Immediate (P0 - blocks MVP):**
1. `ThreadAwareChatMemory.java` - Thread isolation for chat
2. Wire `RAGConversationService` to `AIChatWidget`
3. Add thread support to `RAGConversationService`
4. Unit tests for above

**After MVP (P1):**
5. Feature flag configuration
6. TokenUsageListener
7. Delete old routing code (750 lines)

**Enhancement (P2):**
8. Time-based boundaries
9. Latency monitoring
10. Streaming support
11. Integration tests

---

*Implementation Plan v2.0 | 2025-12-03 | Aligned with ADR-012 RAG approach*
