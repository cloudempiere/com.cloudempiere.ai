# Deprecation Roadmap: Custom Code → LangChain4j 0.35.0

> **Context:** Project accepted Java 11 constraint with LangChain4j 0.35.0 (last Java 11 compatible version).
> Many custom implementations were written before full LangChain4j adoption.
> This document tracks what can be deprecated/replaced.

**Last Updated:** 2025-12-19
**Estimated Total Reduction:** ~5,000 lines (60% of custom layer)

---

## Summary by Priority

| Priority | Category | Lines | Reducible | Effort |
|----------|----------|-------|-----------|--------|
| 🔴 HIGH | DTO Layer | 1,413 | 1,413 (100%) | 2 weeks |
| 🔴 HIGH | Custom Memory | 465 | 465 (100%) | 1 week |
| 🟡 MEDIUM | Streaming | 259 | 200 (75%) | 1 week |
| 🟡 MEDIUM | Tool Registry | 450 | 350 (75%) | 2 weeks |
| 🟡 MEDIUM | Context Management | 1,884 | 1,200 (65%) | 3 weeks |
| 🟢 LOW | Routing Logic | 1,884 | 1,200 (65%) | Superseded by RAG |
| 🟢 LOW | Wrappers | 1,035 | 150 (15%) | 1 week |

---

## Phase 1: DTO Layer (HIGH PRIORITY)

**Target:** v0.32.0
**Effort:** 2-3 weeks
**Reduction:** 1,413 lines → 0 lines (100%)

### Files to Deprecate

| File | Lines | LangChain4j Replacement |
|------|-------|-------------------------|
| `provider/dto/AIRequest.java` | 180 | `ChatMemoryProvider`, `ChatLanguageModel.generate()` |
| `provider/dto/AIResponse.java` | 112 | `dev.langchain4j.model.output.Generation` + `TokenUsage` |
| `provider/dto/AIMessage.java` | 81 | `dev.langchain4j.data.message.ChatMessage` subtypes |
| `provider/dto/AIFunctionCall.java` | 29 | `ToolExecutionResultMessage` |
| `provider/dto/AIFunction.java` | 25 | `@Tool` annotation + reflection |
| `provider/dto/AITokenUsage.java` | 24 | `dev.langchain4j.model.output.TokenUsage` |
| `provider/dto/AIHealthStatus.java` | 26 | Keep (iDempiere-specific) |
| `provider/dto/AIModelCapabilities.java` | 34 | Keep (iDempiere-specific) |
| `provider/dto/AIRateLimitStatus.java` | 210 | Partial - use ChatModelListener |

### Migration Steps

1. [ ] Create adapter layer: Custom DTO → LangChain4j types
2. [ ] Update all callers to use LangChain4j types directly
3. [ ] Mark custom DTOs as `@Deprecated`
4. [ ] Remove in v0.33.0

### LangChain4j Types to Use

```java
// Instead of AIMessage
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

// Instead of AITokenUsage
import dev.langchain4j.model.output.TokenUsage;

// Instead of AIRequest (use memory + model)
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
ChatLanguageModel model = ...;
```

---

## Phase 2: Custom Memory (HIGH PRIORITY)

**Target:** v0.32.0
**Effort:** 1 week
**Reduction:** 465 lines → ~100 lines (78%)

### File to Refactor

| File | Lines | Issue |
|------|-------|-------|
| `langchain4j/ThreadAwareChatMemory.java` | 465 | Custom persistence logic duplicates ChatModelListener |

### Current Pattern (Complex)

```java
class ThreadAwareChatMemory implements ChatMemory {
    // 465 lines of:
    // - Thread-aware message storage
    // - iDempiere DB persistence
    // - Manual cache management
    // - TTL handling
}
```

### Target Pattern (Simple)

```java
// Use standard ChatMemory
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);

// Persist via ChatModelListener (already have AIMetricsListener pattern)
class ChatPersistenceListener implements ChatModelListener {
    @Override
    public void onResponse(ChatModelResponseContext ctx) {
        // Persist to CM_ChatEntry here
    }
}
```

### Migration Steps

1. [ ] Extract persistence logic to `ChatPersistenceListener`
2. [ ] Replace `ThreadAwareChatMemory` with standard `MessageWindowChatMemory`
3. [ ] Add thread ID as metadata in persisted messages
4. [ ] Test thread switching works correctly

---

## Phase 3: Streaming Callback (MEDIUM PRIORITY)

**Target:** v0.33.0
**Effort:** 1 week
**Reduction:** 259 lines → ~50 lines (80%)

### File to Deprecate

| File | Lines | LangChain4j Replacement |
|------|-------|-------------------------|
| `provider/dto/AIStreamCallback.java` | 259 | `StreamingResponseHandler<T>` |

### Current Pattern

```java
AIStreamCallback callback = AIStreamCallback.builder()
    .onChunk(chunk -> ...)
    .onToolStart((name, args) -> ...)
    .onThinking(text -> ...)
    .onComplete(response -> ...)
    .build();
```

### Target Pattern

```java
StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<>() {
    @Override
    public void onNext(String token) { /* same as onChunk */ }
    @Override
    public void onComplete(Response<AiMessage> response) { ... }
    @Override
    public void onError(Throwable error) { ... }
};

// For tool events, use ChatModelListener
```

### What to Keep

- `onProgress()` callback (UX signal, not in LangChain4j)
- Create lightweight `ProgressListener` interface (~30 lines)

---

## Phase 4: Tool Registry (MEDIUM PRIORITY)

**Target:** v0.33.0
**Effort:** 2 weeks
**Reduction:** 450 lines → ~100 lines (78%)

### Files to Deprecate

| File | Lines | Issue |
|------|-------|-------|
| `tool/ToolRegistry.java` | 300 | Manual tool discovery |
| `tool/ITool.java` | 30 | Custom interface |
| `tool/ToolParameter.java` | 40 | Manual schema |
| `function/AIDatabaseFunctionHandler.java` | 220 | Manual JSON parsing |

### Current Pattern (Manual)

```java
public interface ITool {
    String getName();
    String getDescription();
    Map<String, ToolParameter> getParameters();
    Object execute(Map<String, Object> args);
}

// Registration
registry.register(new QueryDatabaseTool());
List<AIFunction> functions = registry.toAIFunctions();
```

### Target Pattern (Declarative)

```java
public class ERPTools {
    @Tool("Execute a read-only SQL query")
    public String queryDatabase(
        @P("SQL SELECT query") String sql,
        @P("Max rows to return") Integer maxRows
    ) {
        return executor.execute(sql, maxRows);
    }
}

// Auto-discovered
AiServices.builder(Agent.class)
    .tools(new ERPTools())
    .build();
```

### Migration Steps

1. [ ] Ensure all tools are in `ERPTools.java` with `@Tool` annotations
2. [ ] Verify `AiServices` discovers all tools correctly
3. [ ] Mark `ToolRegistry`, `ITool` as `@Deprecated`
4. [ ] Remove manual JSON schema generation

---

## Phase 5: Routing Logic (LOW - Superseded by RAG)

**Target:** v0.34.0 (with ADR-012 RAG implementation)
**Effort:** 3 weeks
**Reduction:** 1,884 lines → ~200 lines (89%)

### Files to Deprecate (routing/ package)

| File | Lines | Superseded By |
|------|-------|---------------|
| `PromptAnalyzer.java` | 270 | Semantic search via `ContentRetriever` |
| `EntityExtractor.java` | 214 | LLM extraction from search results |
| `SourceDecision.java` | 160 | Automatic via retriever scoring |
| `RoutingMetrics.java` | 170 | `ChatModelListener` for metrics |
| `TTLConfig.java` | 222 | Embedding store metadata |
| `ContextMatch.java` | 110 | `TextSegment` with metadata |
| `ContextEntry.java` | 113 | `TextSegment` |
| `DbQueryParams.java` | 84 | Tool parameters |
| `DataType.java` | 49 | Enum, keep if needed |
| `ConversationContextManager.java` | 492 | `EmbeddingStoreContentRetriever` |

### Current Pattern (Keyword-Based)

```
prompt → EntityExtractor → PromptAnalyzer → SourceDecision → [DB|Cache|KB]
```

### Target Pattern (Semantic RAG)

```
prompt → EmbeddingStoreContentRetriever → [relevant documents]
       → Tool execution if needed
```

### Why RAG is Superior

- Semantic search finds relevant context without explicit rules
- No keyword matching maintenance
- Handles synonyms, paraphrases automatically
- Metadata filters replace source routing logic

---

## Phase 6: Model Wrappers (LOW PRIORITY)

**Target:** v0.34.0
**Effort:** 1 week
**Reduction:** 150 lines (15%)

### Files to Simplify

| File | Lines | Action |
|------|-------|--------|
| `BedrockChatModelWrapper.java` | 300+ | Move context to tools layer |
| `BedrockStreamingChatModelWrapper.java` | 535 | Use ChatModelListener for metrics |
| `BedrockEmbeddingModelWrapper.java` | 200+ | Simplify to pure delegation |
| `MockAIHubChatModel.java` | 150+ | Keep for testing |

### What to Move

- iDempiere context injection → Tool layer (`ERPTools`)
- Metrics/logging → `ChatModelListener` (already have `AIMetricsListener`)
- Error handling → Keep in wrapper (provider-specific)

---

## LangChain4j 0.35.0 Underutilized Features

### Currently Not Using (Could Adopt)

| Feature | Package | Benefit |
|---------|---------|---------|
| **Structured Outputs** | `dev.langchain4j.service.output` | Type-safe responses, 80% less parsing |
| **OutputParser** | `dev.langchain4j.service` | Auto-retry on invalid structure |
| **ContentRetriever** | `dev.langchain4j.rag.content.retriever` | RAG retrieval abstraction |
| **QueryRouter** | `dev.langchain4j.rag.query.router` | Multi-source routing |
| **ChatMemoryProvider** | `dev.langchain4j.memory.chat` | Per-user memory factory |

### Already Using Well

| Feature | Status |
|---------|--------|
| `@Tool` annotations | ✅ ERPTools.java |
| `AiServices` builder | ✅ LangChain4jAgent.java |
| `ChatModelListener` | ✅ AIMetricsListener.java |
| `EmbeddingStore` | ✅ RagService.java |
| `StreamingChatLanguageModel` | ✅ Multiple providers |

---

## Implementation Timeline

```
v0.32.0 (Q1 2026)
├── Phase 1: Deprecate DTO Layer (1,413 lines)
├── Phase 2: Simplify Memory (465 lines)
└── Phase 3: Simplify Streaming (259 lines)

v0.33.0 (Q1 2026)
├── Phase 4: Remove Tool Registry (450 lines)
└── Remove deprecated DTOs

v0.34.0 (Q1 2026)
├── Phase 5: Replace Routing with RAG (1,884 lines)
└── Phase 6: Simplify Wrappers (150 lines)
```

---

## Tracking

### Deprecation Status

| Component | Status | Version Deprecated | Version Removed |
|-----------|--------|-------------------|-----------------|
| AIRequest | 🔜 Pending | - | - |
| AIMessage | 🔜 Pending | - | - |
| AIResponse | 🔜 Pending | - | - |
| AIFunctionCall | 🔜 Pending | - | - |
| AIFunction | 🔜 Pending | - | - |
| AIStreamCallback | 🔜 Pending | - | - |
| ToolRegistry | 🔜 Pending | - | - |
| ITool | 🔜 Pending | - | - |
| PromptAnalyzer | 🔜 Pending | - | - |
| EntityExtractor | 🔜 Pending | - | - |
| SourceDecision | 🔜 Pending | - | - |
| ConversationContextManager | 🔜 Pending | - | - |

---

## References

- [ADR-002: LangChain4j Strategic Adoption](adr/002-langchain4j-strategic-adoption.md)
- [ADR-005: Intelligent Data Source Routing](adr/005-intelligent-data-source-routing.md) (Superseded)
- [ADR-012: RAG-Based Context Retrieval](adr/012-rag-based-context-retrieval.md)
- [ADR-035: Java Version Strategy](adr/035-java-version-strategy.md)
- [LangChain4j 0.35.0 Documentation](https://docs.langchain4j.dev/)
