# Chat Panel LangChain4j ChatModel Integration - Implementation Plan

**ADR Reference:** [ADR-031](adr/031-chat-panel-langchain4j-chatmodel-integration.md)
**Created:** 2025-12-03
**Updated:** 2025-12-26
**Status:** In Progress
**Branch:** `langchain`

---

## Executive Summary

This plan details the migration of `AIChatWidget` to use LangChain4j's `ChatLanguageModel` via `AIService`. The migration preserves critical iDempiere-specific features (intelligent routing, query caching, thread filtering) while leveraging LangChain4j's simplicity.

---

## Current State Assessment (2025-12-26)

### What's Already Implemented

| Component | ADR | Status | Location |
|-----------|-----|--------|----------|
| **RAGContextManager** | ADR-012 | ✅ Done | `src/.../rag/RAGContextManager.java` |
| **RAGConversationService** | ADR-012 | ✅ Done | `src/.../rag/RAGConversationService.java` |
| **RagService** | ADR-012 | ✅ Done | `src/.../rag/RagService.java` |
| **AgentBoundary** | ADR-009 | ✅ Done | `src/.../boundary/AgentBoundary.java` |
| **AgentBoundaryRegistry** | ADR-009 | ✅ Done | `src/.../boundary/AgentBoundaryRegistry.java` |
| **BoundaryEnforcementFilter** | ADR-009 | ✅ Done | `src/.../boundary/BoundaryEnforcementFilter.java` |
| **BoundaryViolationException** | ADR-009 | ✅ Done | `src/.../boundary/BoundaryViolationException.java` |
| **CostBoundaryMonitor** | ADR-009 | ✅ Done | `src/.../boundary/CostBoundaryMonitor.java` |
| **DataAccessValidator** | ADR-009 | ✅ Done | `src/.../boundary/DataAccessValidator.java` |
| **LangChain4jProviderFactory** | ADR-002 | ✅ Done | `src/.../langchain4j/LangChain4jProviderFactory.java` |
| **ThreadAwareChatMemory** | ADR-031 | ✅ Done | `src/.../langchain4j/ThreadAwareChatMemory.java` (465 lines) |
| **AIService** | ADR-031 | ✅ Done | `src/.../langchain4j/AIService.java` (1941 lines) |
| **ERPAgent** | ADR-031 | ✅ Done | `src/.../langchain4j/ERPAgent.java` |
| **ERPStreamingAgent** | ADR-031 | ✅ Done | `src/.../langchain4j/ERPStreamingAgent.java` |
| **ERPTools** | ADR-031 | ✅ Done | `src/.../langchain4j/ERPTools.java` |
| **AIChatWidget Integration** | ADR-031 | ✅ Done | Uses `AIService` |

### What's NOT Yet Implemented (Gaps)

| Component | ADR | Status | Blocking |
|-----------|-----|--------|----------|
| **ThreadAwareChatMemoryTest** | ADR-032 | ❌ Not Started | Quality |
| **RAGContextManagerTest** | ADR-032 | ❌ Not Started | Quality |
| **AgentBoundaryTest** | ADR-032 | ❌ Not Started | Quality |
| **TokenUsageListener** | ADR-013 | ❌ Not Started | Cost Tracking |
| **LatencyMetricsListener** | ADR-013 | ❌ Not Started | Observability |
| **TimeBoundaryValidator** | ADR-009 | ❌ Not Started | Period Restrictions |
| **Remove Old Routing Code** | ADR-012 | ❌ Not Started | Cleanup (1884 lines) |
| **Integration Tests** | ADR-032 | ❌ Not Started | E2E Validation |

### Architecture Resolution

**ADR-031 Plan vs ADR-012 Implementation:**

The original ADR-031 plan referenced custom routing components. ADR-012 superseded this with RAG-based context retrieval.

**Current Implementation Path:**
```
AIChatWidget → AIService → LangChain4j ChatModel
                    ↓
              ERPAgent/ERPStreamingAgent
                    ↓
              ERPTools (database queries)
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

### Prioritized Task Overview

| Task | Priority | Status | Dependencies | Notes |
|------|----------|--------|--------------|-------|
| **Core LangChain4j Integration** | P0 | ✅ Done | - | AIService, ThreadAwareChatMemory |
| **AIChatWidget uses AIService** | P0 | ✅ Done | - | Complete |
| **Basic unit tests** | P1 | ❌ TODO | - | ThreadAwareChatMemory, RAG, Boundary |
| **TokenUsageListener** | P1 | ❌ TODO | None | Cost tracking |
| **Remove old routing code** | P1 | ❌ TODO | Tests passing | 1884 lines to remove |
| **LatencyMetricsListener** | P2 | ❌ TODO | None | Performance |
| **TimeBoundaryValidator** | P2 | ❌ TODO | None | Period restrictions |
| **Integration tests** | P2 | ❌ TODO | Unit tests | E2E validation |

---

## Remaining Implementation Tasks

### Sprint 1: Testing (P1) - Estimated 2-3 days

#### 1.1 Unit Tests for Core Components

**Files to create in `com.cloudempiere.ai.test`:**

| Test File | Target | Priority |
|-----------|--------|----------|
| `ThreadAwareChatMemoryTest.java` | Thread isolation, trimming, concurrency | P1 |
| `RAGContextManagerTest.java` | Embedding store, retrieval, fallback | P1 |
| `AgentBoundaryTest.java` | Table/column allow/block | P1 |
| `BoundaryEnforcementFilterTest.java` | SQL injection, org filtering | P1 |
| `AIServiceTest.java` | Message handling, streaming | P1 |

**Acceptance Criteria:**
- [ ] Thread isolation verified
- [ ] Max message trimming works
- [ ] Concurrent access safe
- [ ] All unit tests pass with `./run-unit-tests.sh`

---

### Sprint 2: Observability (P1-P2) - Estimated 1-2 days

#### 2.1 TokenUsageListener [P1]

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

    @Override
    public void onRequest(ChatModelRequestContext context) {
        log.fine("AI Request started");
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        var usage = context.response().tokenUsage();
        if (usage != null) {
            log.info(String.format(
                "AI Usage: input=%d, output=%d, total=%d tokens",
                usage.inputTokenCount(), usage.outputTokenCount(), usage.totalTokenCount()
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

#### 2.2 LatencyMetricsListener [P2]

**File:** `src/com/cloudempiere/ai/provider/langchain4j/listener/LatencyMetricsListener.java`

---

### Sprint 3: Cleanup (P1) - Estimated 1 day

#### 3.1 Remove Old Routing Code [P1]

**Files to delete (1884 lines total):**

| File | Lines | Notes |
|------|-------|-------|
| `routing/PromptAnalyzer.java` | 270 | Superseded by RAG |
| `routing/EntityExtractor.java` | 214 | Superseded by RAG |
| `routing/ConversationContextManager.java` | 492 | Superseded by ThreadAwareChatMemory |
| `routing/ContextEntry.java` | 113 | Superseded by LangChain4j messages |
| `routing/ContextMatch.java` | 110 | Superseded by RAG |
| `routing/SourceDecision.java` | 160 | Superseded by RAG |
| `routing/DataType.java` | 49 | Superseded by RAG |
| `routing/TTLConfig.java` | 222 | Superseded by RAG |
| `routing/RoutingMetrics.java` | 170 | Superseded by listeners |
| `routing/DbQueryParams.java` | 84 | Superseded by ERPTools |
| **Total** | **1884** | |

**Pre-deletion Verification:**
- [ ] All tests pass
- [ ] No compile errors after deletion
- [ ] AIChatWidget still works via AIService
- [ ] Grep for imports shows no remaining usage

---

### Sprint 4: Enhanced (P2) - Estimated 2 days

#### 4.1 TimeBoundaryValidator [P2]

**File:** `src/com/cloudempiere/ai/boundary/TimeBoundaryValidator.java`

Enforces period restrictions:
- Current period: Can create/modify
- Closed periods: Read-only
- Future periods: Restricted

#### 4.2 Integration Tests [P2]

**File:** `com.cloudempiere.ai.test/.../integration/AIServiceIntegrationTest.java`

- End-to-end conversation flow
- Thread isolation across sessions
- Context retrieval with RAG

---

## File Summary

### Files Already Created (✅ Done)

| File | Lines | Status |
|------|-------|--------|
| `ThreadAwareChatMemory.java` | 465 | ✅ Done |
| `AIService.java` | 1941 | ✅ Done |
| `ERPAgent.java` | ~200 | ✅ Done |
| `ERPStreamingAgent.java` | ~150 | ✅ Done |
| `ERPTools.java` | ~300 | ✅ Done |
| `RAGContextManager.java` | ~200 | ✅ Done |
| `RAGConversationService.java` | ~150 | ✅ Done |
| `AgentBoundary.java` | ~100 | ✅ Done |
| `BoundaryEnforcementFilter.java` | ~150 | ✅ Done |

### Files to Create

| File | Est. Lines | Priority |
|------|------------|----------|
| `ThreadAwareChatMemoryTest.java` | ~150 | P1 |
| `RAGContextManagerTest.java` | ~150 | P1 |
| `AgentBoundaryTest.java` | ~100 | P1 |
| `AIServiceTest.java` | ~200 | P1 |
| `TokenUsageListener.java` | ~50 | P1 |
| `LatencyMetricsListener.java` | ~40 | P2 |
| `TimeBoundaryValidator.java` | ~60 | P2 |
| `AIServiceIntegrationTest.java` | ~150 | P2 |

### Files to Delete (Post-Testing)

| Directory | Files | Lines |
|-----------|-------|-------|
| `routing/` | 10 files | 1884 |

---

## Success Criteria

| Metric | Target | Status |
|--------|--------|--------|
| Core integration works | ✅ | Done |
| Thread isolation | ✅ | Done (needs tests) |
| Unit test coverage | ≥80% on new code | TODO |
| Code reduction | 1884 lines removed | TODO |
| Response latency | ≤ current | TODO (needs listener) |
| Zero regressions | ✅ | TODO (needs tests) |

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Routing code still in use | Grep for imports before deletion |
| Thread memory leaks | Implement session cleanup on logout |
| Performance regression | Benchmark before/after with LatencyListener |
| Breaking changes | Comprehensive test coverage first |

---

## Quick Reference: What to Implement Next

**Immediate (P1):**
1. Unit tests for ThreadAwareChatMemory, RAGContextManager, AgentBoundary
2. TokenUsageListener for cost tracking
3. Delete old routing code (1884 lines) after tests pass

**Enhancement (P2):**
4. LatencyMetricsListener
5. TimeBoundaryValidator
6. Integration tests

---

*Implementation Plan v3.0 | 2025-12-26 | Updated to reflect current state*
