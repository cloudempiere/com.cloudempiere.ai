# ADR-012: RAG-Based Context Retrieval

**Status:** Accepted
**Date:** 2025-12-01
**Deciders:** CloudEmpiere AI Team
**Supersedes:** ADR-005 (Intelligent Data Source Routing)
**Planned Implementation:** v0.10.0

---

## Context

### Domain Architecture Context

This ADR addresses the **CONTEXT domain** from the 3-domain data source architecture described in `docs/DATA_SOURCE_ARCHITECTURE.md`.

**3 Data Domains:**

| Domain | Scope | Implementation |
|--------|-------|----------------|
| **CONTEXT** | Conversation history + UI state | ✅ This ADR (RAG pattern) |
| **DATABASE** | iDempiere ERP data | ✅ ERPTools @Tool methods (ADR-002) |
| **MCP TOOLS** | External services | 🔜 Planned (ADR-003) |

**Original Design (DATA_SOURCE_ARCHITECTURE.md):**
- Proposed custom `PromptAnalyzer` with regex-based routing
- Proposed custom `ConversationContextManager` with manual TTL

**This ADR Decision:**
- Replace custom routing with LangChain4j `ContentRetriever` (semantic search)
- Replace manual cache with `EmbeddingStore` (built-in TTL)
- Achieve same goals with 86% less code (730 → 100 lines)

**Domain Boundary:** CONTEXT domain handles all conversation-related data that doesn't require fresh database queries or external API calls.

See: `docs/DATA_SOURCE_ARCHITECTURE.md` for complete domain model and boundary definitions.

---

### Problem Statement

ADR-005 implemented intelligent data source routing with **custom regex-based pattern matching** (730 lines of code) to decide when to use:
- **Cached conversation context** (fast, possibly stale)
- **Fresh database queries** (accurate, slower)

While this achieved excellent results (69% faster response, 38% cache hit rate), validation against **LangChain4j 1.0.0-beta3** revealed that we **reinvented a standard pattern**.

### Current Implementation (ADR-005)

**Architecture:**
```
User Prompt: "What's the current status of order SO-1234?"
    │
    ▼
PromptAnalyzer (120 lines)
  ├── Regex: REAL_TIME_PATTERN
  ├── Regex: BULK_PATTERN
  ├── Regex: PRONOUN_PATTERN
  └── Decision: CONTEXT/DATABASE/HYBRID
    │
    ▼
ConversationContextManager (230 lines)
  ├── TTL-based cache
  ├── LRU eviction
  └── Entity tracking
```

**Achieved Metrics:**
- ✅ Response time: 4.2s → 1.3s (69% faster)
- ✅ Database queries: -62%
- ✅ API costs: -63%
- ✅ Cache hit rate: 38%
- ✅ Decision accuracy: 92%

**Problems:**
- ❌ 730 lines of custom code to maintain
- ❌ Regex patterns require tuning ("current" matches, but "up-to-date" doesn't)
- ❌ False positives (8%): "current customer" → DATABASE when customer is in context
- ❌ No semantic understanding (keyword matching only)

### LangChain4j RAG Pattern (2025)

LangChain4j provides **RAG (Retrieval-Augmented Generation)** as a standard pattern:

```java
// Semantic search instead of regex patterns
EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(embeddingModel)
    .maxResults(5)      // Top 5 relevant segments
    .minScore(0.7)      // Only if > 70% relevance
    .build();

IDempiereAgent agent = AiServices.builder(IDempiereAgent.class)
    .chatLanguageModel(model)
    .contentRetriever(retriever)  // Automatic context injection
    .build();
```

**Advantages:**
- ✅ 86% less code (730 → 100 lines)
- ✅ Semantic search (understands "up-to-date" = "current")
- ✅ Better accuracy (projected 92% → 98%)
- ✅ Higher cache hits (projected 38% → 65%)
- ✅ Industry-standard pattern
- ✅ No pattern tuning needed

---

## Decision

**Migrate from custom regex-based routing (ADR-005) to LangChain4j RAG pattern.**

### Architecture

```
User Prompt: "What's the current status of order SO-1234?"
    │
    ▼
┌─────────────────────────────────────────────┐
│  ContentRetriever (LangChain4j)             │
│  - Semantic search via embeddings           │
│  - Automatic relevance scoring              │
│  - Built-in caching & optimization          │
└────────────┬────────────────────────────────┘
             │
    ┌────────┴────────┐
    ▼                 ▼
EmbeddingStore    EmbeddingModel
(Context cache)   (Semantic search)
    │                 │
    ▼                 ▼
[Recent entities]  Ollama nomic-embed-text
[Query results]    (Vector embeddings)
[Window context]
```

### Component Mapping

| ADR-005 Custom | ADR-012 LangChain4j | Code Reduction |
|----------------|---------------------|----------------|
| **ConversationContextManager** (230 lines)<br>- TTL management<br>- LRU eviction<br>- Entity tracking | **EmbeddingStore** (1 line)<br>`InMemoryEmbeddingStore<>()` | **-229 lines** |
| **PromptAnalyzer** (120 lines)<br>- 8 regex patterns<br>- Decision logic | **ContentRetriever** (6 lines)<br>`EmbeddingStoreContentRetriever.builder()` | **-114 lines** |
| **EntityExtractor** (140 lines)<br>- ID pattern matching<br>- Table recognition | **EmbeddingModel** (3 lines)<br>`OllamaEmbeddingModel.builder()` | **-137 lines** |
| **ContextEntry** (40 lines) | **TextSegment** (built-in) | **-40 lines** |
| **TTLConfig** (80 lines) | Built-in expiration | **-80 lines** |
| **RoutingMetrics** (100 lines) | AiServices listeners | **-100 lines** |
| **DataType** enum (20 lines) | Not needed | **-20 lines** |
| **Total:** 730 lines | **Total:** ~100 lines | **-630 lines (86%)** |

---

## Implementation

### Phase 1: Setup (Days 1-2)

**Add Dependencies:**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-embeddings</artifactId>
    <version>1.0.0-beta3</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>1.0.0-beta3</version>
</dependency>
```

**Install Ollama Embedding Model:**
```bash
ollama pull nomic-embed-text
```

### Phase 2: RAG Infrastructure (Days 3-5)

**Create RAGContextManager:**

**File:** `src/com/cloudempiere/ai/rag/RAGContextManager.java`

```java
package com.cloudempiere.ai.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.time.Duration;
import java.util.logging.Logger;

/**
 * RAG-based context manager for iDempiere AI conversations
 * Replaces custom PromptAnalyzer and ConversationContextManager
 *
 * Uses semantic search via embeddings instead of regex pattern matching
 *
 * @author CloudEmpiere AI Team
 * @version ADR-012
 */
public class RAGContextManager {

    private static final Logger log = Logger.getLogger(RAGContextManager.class.getName());

    /** Embedding store for conversation context */
    private final EmbeddingStore<TextSegment> store;

    /** Embedding model for semantic search */
    private final EmbeddingModel embeddingModel;

    /** Base URL for Ollama service */
    private final String ollamaBaseUrl;

    /**
     * Constructor with default Ollama configuration
     */
    public RAGContextManager() {
        this("http://localhost:11434");
    }

    /**
     * Constructor with custom Ollama URL
     * @param ollamaBaseUrl Ollama service URL
     */
    public RAGContextManager(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.store = new InMemoryEmbeddingStore<>();
        this.embeddingModel = createEmbeddingModel();

        log.info("RAGContextManager initialized with Ollama at " + ollamaBaseUrl);
    }

    /**
     * Add context to embedding store
     * @param key unique identifier (e.g., "order_SO-1234")
     * @param content text content to store
     */
    public void addContext(String key, String content) {
        try {
            TextSegment segment = TextSegment.from(content);
            Embedding embedding = embeddingModel.embed(segment).content();
            store.add(embedding, segment);

            log.fine("Added context: " + key);
        } catch (Exception e) {
            log.warning("Failed to add context " + key + ": " + e.getMessage());
        }
    }

    /**
     * Create ContentRetriever for agent integration
     * @return configured content retriever
     */
    public ContentRetriever getRetriever() {
        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(embeddingModel)
            .maxResults(5)      // Top 5 relevant segments
            .minScore(0.7)      // Only if > 70% relevance
            .build();
    }

    /**
     * Create embedding model with error handling
     * @return embedding model instance
     */
    private EmbeddingModel createEmbeddingModel() {
        try {
            return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName("nomic-embed-text")
                .timeout(Duration.ofSeconds(10))
                .build();
        } catch (Exception e) {
            log.severe("Failed to create embedding model: " + e.getMessage());
            throw new RuntimeException("Embedding model initialization failed", e);
        }
    }

    /**
     * Check if embedding service is available
     * @return true if Ollama is reachable
     */
    public boolean isAvailable() {
        try {
            embeddingModel.embed("test");
            return true;
        } catch (Exception e) {
            log.warning("Embedding service not available: " + e.getMessage());
            return false;
        }
    }
}
```

### Phase 3: Service Integration (Days 6-7)

**Modify AIConversationService:**

```java
package com.cloudempiere.ai.service;

import com.cloudempiere.ai.agent.IDempiereAgent;
import com.cloudempiere.ai.provider.factory.LangChain4jProviderFactory;
import com.cloudempiere.ai.rag.RAGContextManager;
import com.cloudempiere.ai.tools.ERPTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.json.JSONObject;

/**
 * AI conversation service using RAG-based context retrieval (ADR-012)
 */
public class AIConversationService {

    private final LangChain4jProviderFactory providerFactory;
    private final ERPTools erpTools;
    private final RAGContextManager ragContextManager;

    public AIConversationService() {
        this.providerFactory = new LangChain4jProviderFactory();
        this.erpTools = new ERPTools();
        this.ragContextManager = new RAGContextManager();
    }

    /**
     * Send message with automatic RAG-based context retrieval
     * @param userMessage user's question
     * @param windowContext current window context (if any)
     * @return AI response
     */
    public String sendMessageWithContext(String userMessage, JSONObject windowContext) {
        // 1. Store window context in embedding store
        if (windowContext != null) {
            String key = "window_" + System.currentTimeMillis();
            ragContextManager.addContext(key, windowContext.toString());
        }

        // 2. Create agent with RAG-based context retrieval
        IDempiereAgent agent = createRAGAgent();

        // 3. Agent automatically retrieves relevant context via ContentRetriever
        //    No manual routing decision needed - semantic search handles it
        String response = agent.chat(userMessage);

        return response;
    }

    /**
     * Create agent with RAG-based context retrieval
     * @return configured agent
     */
    private IDempiereAgent createRAGAgent() {
        // Get chat model from provider factory
        ChatLanguageModel model = providerFactory.createChatModel("anthropic");

        // Get content retriever from RAG context manager
        ContentRetriever retriever = ragContextManager.getRetriever();

        // Build agent with automatic context injection
        return AiServices.builder(IDempiereAgent.class)
            .chatLanguageModel(model)
            .tools(erpTools)
            .contentRetriever(retriever)  // ← Replaces PromptAnalyzer routing
            .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
            .build();
    }
}
```

### Phase 4: Remove Old Code (Days 8-9)

**Delete Custom Routing Classes:**
```bash
# Remove ADR-005 implementation
rm -rf src/com/cloudempiere/ai/routing/

# Specifically:
# - PromptAnalyzer.java (120 lines)
# - EntityExtractor.java (140 lines)
# - SourceDecision.java (20 lines)
# - DataType.java (20 lines)
# - TTLConfig.java (80 lines)
# - RoutingMetrics.java (100 lines)

rm src/com/cloudempiere/ai/context/ConversationContextManager.java  # 230 lines
rm src/com/cloudempiere/ai/context/ContextEntry.java                # 40 lines
```

**Total Code Removed:** ~750 lines

### Phase 5: Testing & Validation (Day 10)

**Test Suite:**

```java
package com.cloudempiere.ai.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test RAG-based context retrieval (ADR-012)
 */
public class RAGContextManagerTest {

    private RAGContextManager ragContextManager;
    private IDempiereAgent agent;

    @BeforeEach
    public void setUp() {
        ragContextManager = new RAGContextManager();
        agent = createTestAgent();
    }

    @Test
    public void testSemanticUnderstanding() {
        // Store context
        ragContextManager.addContext(
            "order_1",
            "Order SO-1234: Status=Completed, Customer=ABC Corp, Amount=$1,250"
        );

        // Test synonyms (semantic matching, not regex)
        String response1 = agent.chat("What was the total?");
        assertTrue(response1.contains("1250"), "Should understand 'total' = 'amount'");

        String response2 = agent.chat("How much was it?");
        assertTrue(response2.contains("1250"), "Should understand 'how much' = 'amount'");

        String response3 = agent.chat("Is it done?");
        assertTrue(response3.contains("Completed"), "Should understand 'done' = 'completed'");

        // Verify context was used (no DB query)
        verify(databaseTools, never()).queryDatabase(anyString());
    }

    @Test
    public void testTemporalKeywordHandling() {
        // Store old context
        ragContextManager.addContext(
            "order_old",
            "Order SO-1234: Status=Draft (from yesterday)"
        );

        // Ask for current status → should query DB (low relevance score)
        String response = agent.chat("What's the up-to-date status of SO-1234?");

        // Verify DB was queried (old context not relevant enough)
        verify(databaseTools).queryDatabase(contains("SO-1234"));
    }

    @Test
    public void testCacheHitRate() {
        int totalQueries = 50;
        int contextHits = 0;

        for (int i = 0; i < totalQueries; i++) {
            // Initial query → DATABASE
            ragContextManager.addContext("order_" + i, "Order SO-" + i + ": Amount=$100");

            // Follow-up → should use CONTEXT
            reset(databaseTools);
            agent.chat("What was the amount?");

            if (verify(databaseTools, never()).queryDatabase(anyString())) {
                contextHits++;
            }
        }

        double hitRate = (double) contextHits / totalQueries;
        assertTrue(hitRate > 0.50, "Cache hit rate should be > 50% (actual: " + hitRate + ")");
    }

    @Test
    public void testFallbackToDatabase() {
        // No context stored

        // Query should fall back to database
        String response = agent.chat("Show me order SO-1234");

        // Verify DB was queried
        verify(databaseTools).queryDatabase(contains("SO-1234"));
    }
}
```

---

## Performance Targets

### Success Metrics

| Metric | ADR-005 (Baseline) | ADR-012 (Target) | Validation |
|--------|-------------------|------------------|------------|
| **Cache Hit Rate** | 38% | **> 50%** | Metrics logging |
| **Decision Accuracy** | 92% | **> 95%** | Manual review of 100 prompts |
| **Response Time** | 1.3s | **< 1.5s** | Performance tracking |
| **Code Complexity** | 730 lines | **< 200 lines** | LOC count |
| **False Positives** | 8% | **< 3%** | Manual review |

### Projected Improvements

Based on LangChain4j RAG benchmarks and semantic search studies:

| Metric | ADR-005 | ADR-012 (Projected) | Improvement |
|--------|---------|---------------------|-------------|
| Cache Hit Rate | 38% | **65%** | **+71%** |
| Decision Accuracy | 92% | **98%** | **+6.5%** |
| Response Time | 1.3s | **1.1s** | **+15%** |
| False Positives | 8% | **2%** | **-75%** |
| Maintenance Hours | ~8h/month | **<2h/month** | **-75%** |

**Rationale:**
- **Higher cache hits:** Semantic search finds "up-to-date" = "current" (regex misses)
- **Better accuracy:** Understands "current customer" context (regex false positive)
- **Faster response:** Vector search faster than string regex scanning

---

## Consequences

### Positive

- ✅ **86% code reduction** (730 → 100 lines)
- ✅ **Semantic understanding** (synonyms, context-aware)
- ✅ **Industry-standard pattern** (RAG is proven approach)
- ✅ **Less maintenance** (no regex tuning needed)
- ✅ **Better accuracy** (projected 92% → 98%)
- ✅ **Higher cache hits** (projected 38% → 65%)
- ✅ **Standard observability** (AiServices listeners)

### Negative

- ❌ **New dependency:** Ollama embedding service required
- ❌ **Learning curve:** Team needs to understand RAG concepts
- ❌ **Migration effort:** 2 weeks of development time
- ❌ **Embedding latency:** First query slower (embedding computation)

### Neutral

- 🔄 **Security unchanged:** `SecureDatabaseQueryExecutor` still used
- 🔄 **Audit trail unchanged:** All queries still logged
- 🔄 **User experience unchanged:** Same conversation flow

### Risks & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Ollama service unavailable** | Low (10%) | High | Fallback to ADR-005 via feature flag |
| **Lower cache hit rate** | Medium (30%) | Medium | Tune `minScore`, increase `maxResults` |
| **Higher latency** | Low (15%) | Medium | Use lightweight embedding model |
| **Migration breaks features** | Low (5%) | Critical | Parallel testing, gradual rollout |

---

## Migration Strategy

### Backward Compatibility

**Feature Flag Approach:**
```java
// application.properties
ai.routing.strategy=RAG  # or LEGACY

// AIConversationService.java
public String sendMessageWithContext(String userMessage, JSONObject windowContext) {
    String strategy = System.getProperty("ai.routing.strategy", "RAG");

    if ("LEGACY".equals(strategy)) {
        // ADR-005: Custom routing
        return sendMessageWithCustomRouting(userMessage, windowContext);
    } else {
        // ADR-012: RAG pattern
        return sendMessageWithRAG(userMessage, windowContext);
    }
}
```

### Rollback Plan

**If cache hit rate < 30% OR accuracy < 90% OR response time > 2s:**

1. Set feature flag: `ai.routing.strategy=LEGACY`
2. Keep ADR-005 code in separate branch
3. Investigate RAG configuration:
   - Tune `minScore` (0.7 → 0.5)
   - Increase `maxResults` (5 → 10)
   - Try different embedding model
4. Re-test and adjust

### Gradual Rollout

**Week 1:** 10% of requests use RAG
**Week 2:** 50% of requests use RAG
**Week 3:** 100% if metrics meet targets

---

## Alternatives Considered

### Alternative 1: Keep ADR-005 Custom Routing

**Approach:** Continue with regex-based routing

**Rejected because:**
- 730 lines of custom code to maintain
- Regex patterns require ongoing tuning
- False positives (8%)
- Not industry-standard pattern
- Already achieved targets, but can improve further

### Alternative 2: Hybrid (RAG + Custom)

**Approach:** Use RAG for semantic search, fall back to regex for edge cases

**Rejected because:**
- Doubles complexity (maintain both systems)
- Unclear decision boundary (when to use which)
- Defeats purpose of standardization
- No significant benefit over pure RAG

### Alternative 3: External Vector Database (Pinecone, Weaviate)

**Approach:** Use cloud-based vector store instead of InMemoryEmbeddingStore

**Deferred to future:**
- Adds external dependency and cost
- InMemoryEmbeddingStore sufficient for MVP
- Can migrate later if needed (ADR-012 remains compatible)

---

## Implementation Checklist

### Development

- [ ] Add LangChain4j dependencies (`langchain4j-embeddings`, `langchain4j-ollama`)
- [ ] Install Ollama and `nomic-embed-text` model
- [ ] Create `RAGContextManager.java` (~50 lines)
- [ ] Modify `AIConversationService.java` to use RAG (~30 lines)
- [ ] Add feature flag for backward compatibility
- [ ] Write tests (`RAGContextManagerTest.java`)

### Cleanup

- [ ] Remove `PromptAnalyzer.java` (120 lines)
- [ ] Remove `ConversationContextManager.java` (230 lines)
- [ ] Remove `EntityExtractor.java` (140 lines)
- [ ] Remove `ContextEntry.java`, `DataType.java`, `TTLConfig.java`, `RoutingMetrics.java` (240 lines)
- [ ] Update imports in affected classes

### Documentation

- [ ] Update ADR-005 status to "Superseded by ADR-012"
- [ ] Add ADR-012 to ADR index
- [ ] Update FEATURES.md with RAG implementation
- [ ] Update CHANGELOG.md with v0.10.0 changes

### Testing

- [ ] Unit tests for `RAGContextManager`
- [ ] Integration tests for `AIConversationService`
- [ ] Performance benchmarks (cache hit rate, accuracy, latency)
- [ ] Manual testing with 100 real prompts

---

## References

### Internal Documents

- **ADR-005:** Intelligent Data Source Routing (superseded)
- **ADR-002:** LangChain4j Strategic Adoption
- **Validation Report:** `docs/ROUTING_VALIDATION_REPORT.md`
- **Implementation Plan:** `docs/INTELLIGENT_DATA_SOURCE_ROUTING_IMPLEMENTATION.md` (research)

### External References

- **LangChain4j RAG Documentation:** https://docs.langchain4j.dev/tutorials/rag/
- **LangChain4j ContentRetriever:** https://docs.langchain4j.dev/tutorials/rag/#content-retriever
- **Ollama Embedding Models:** https://ollama.ai/library/nomic-embed-text
- **RAG Pattern Best Practices:** https://docs.langchain4j.dev/tutorials/rag/#best-practices

### Related ADRs

- **ADR-002:** LangChain4j Strategic Adoption (foundation)
- **ADR-004:** Java Agent Framework Selection (agent implementation)
- **ADR-007:** Database Security Model (security layer, unchanged)

---

## Success Criteria

**This ADR is considered successful if:**

1. ✅ Cache hit rate > 50% (target: 65%)
2. ✅ Decision accuracy > 95% (target: 98%)
3. ✅ Response time < 1.5s (target: 1.1s)
4. ✅ Code reduction > 500 lines (target: 630 lines)
5. ✅ False positives < 3% (target: 2%)
6. ✅ Zero production incidents during migration

**If criteria not met within 2 weeks:** Rollback to ADR-005 and reassess.

---

**ADR-012 | Version 1.0 | 2025-12-01**
**Status: Accepted (Supersedes ADR-005)**
**Implementation: v0.10.0 (2-week timeline)**
