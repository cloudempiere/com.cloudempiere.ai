# P1: Context Layer Implementation Plan

**Status:** Planning
**Priority:** P1 (Context First)
**Date:** 2025-12-10
**Reference:** ADR-027 (Revised), idempiere-cli RAG architecture

---

## Overview

This document details the implementation plan for the Context Layer (P1), which includes:
- **ADR-026**: Vector Database (pgvector)
- **ADR-012**: RAG-Based Context Retrieval
- **ADR-016**: Knowledge Base

The Context Layer is foundational - without relevant context, AI responses are generic and unhelpful.

---

## Current State

### What We Have

| Component | File | Status |
|-----------|------|--------|
| RAGContextManager | `rag/RAGContextManager.java` | ✅ Exists (InMemory only) |
| RAGConversationService | `rag/RAGConversationService.java` | ✅ Exists (not wired) |
| EmbeddingModel integration | `LangChain4jProviderFactory` | ✅ Works |
| ContentRetriever | `EmbeddingStoreContentRetriever` | ✅ Works |
| InMemoryEmbeddingStore | Per-session | ✅ Works (but volatile) |

### What's Missing

| Component | Description | Priority |
|-----------|-------------|----------|
| **PgVectorEmbeddingStore** | Persistent vector storage | P1.1 |
| **EmbeddingStoreProvider** | OSGi service for store management | P1.1 |
| **Knowledge Ingestors** | Populate embeddings from sources | P1.2 |
| **RagTools** | @Tool methods for agent search | P1.3 |
| **Hybrid Search** | Semantic + keyword (RRF fusion) | P2 |

---

## LangChain4j 0.35.0 Compatibility

### Verified Available

| Component | Maven Artifact | Version | Status |
|-----------|---------------|---------|--------|
| langchain4j-pgvector | `dev.langchain4j:langchain4j-pgvector` | 0.35.0 | ✅ Exists |
| PgVectorEmbeddingStore | `dev.langchain4j.store.embedding.pgvector` | 0.35.0 | ✅ Available |
| EmbeddingStoreContentRetriever | `dev.langchain4j.rag.content.retriever` | 0.35.0 | ✅ Already used |
| @Tool annotation | `dev.langchain4j.agent.tool` | 0.35.0 | ✅ Already used |

### Maven Dependency to Add

```xml
<artifactItem>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>0.35.0</version>
</artifactItem>
```

---

## Implementation Phases

### Phase P1.1: Persistent Vector Storage

**Goal:** Replace InMemoryEmbeddingStore with PgVectorEmbeddingStore

#### 1.1.1 Database Setup

```sql
-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Embedding table (LangChain4j creates this automatically)
-- But we may want custom metadata columns
CREATE TABLE IF NOT EXISTS aig_embedding (
    embedding_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id INTEGER NOT NULL DEFAULT 0,
    ad_org_id INTEGER NOT NULL DEFAULT 0,
    embedding vector(768),  -- nomic-embed-text dimension
    text_segment TEXT,
    metadata JSONB,
    source_type VARCHAR(50),  -- 'ad_metadata', 'knowledge_entry', 'glossary'
    source_id VARCHAR(100),   -- Table_ID, K_Entry_ID, etc.
    created TIMESTAMP DEFAULT NOW(),
    updated TIMESTAMP DEFAULT NOW()
);

-- HNSW index for fast similarity search
CREATE INDEX IF NOT EXISTS idx_aig_embedding_hnsw
ON aig_embedding USING hnsw (embedding vector_cosine_ops);

-- Client filter index
CREATE INDEX IF NOT EXISTS idx_aig_embedding_client
ON aig_embedding (ad_client_id);

-- Source type filter index
CREATE INDEX IF NOT EXISTS idx_aig_embedding_source
ON aig_embedding (source_type);
```

#### 1.1.2 EmbeddingStoreProvider (OSGi Service)

```java
package com.cloudempiere.ai.rag.embedding;

/**
 * OSGi service providing embedding store access.
 * Replaces Quarkus CDI @Inject pattern from idempiere-cli.
 */
@Component(
    service = IEmbeddingStoreProvider.class,
    immediate = true
)
public class EmbeddingStoreProvider implements IEmbeddingStoreProvider {

    private PgVectorEmbeddingStore embeddingStore;
    private final Object lock = new Object();

    @Override
    public EmbeddingStore<TextSegment> getStore() {
        synchronized (lock) {
            if (embeddingStore == null) {
                embeddingStore = createStore();
            }
            return embeddingStore;
        }
    }

    private PgVectorEmbeddingStore createStore() {
        // Get connection from iDempiere's DB pool
        DataSource ds = DB.getDataSource();

        return PgVectorEmbeddingStore.builder()
            .dataSource(ds)
            .table("aig_embedding")
            .dimension(768)  // nomic-embed-text
            .createTable(false)  // We create manually for iDempiere conventions
            .build();
    }

    @Override
    public boolean isAvailable() {
        try {
            // Check if pgvector extension is installed
            return checkPgVectorExtension();
        } catch (Exception e) {
            return false;
        }
    }
}
```

#### 1.1.3 Update RAGContextManager

```java
// Replace InMemoryEmbeddingStore with provider-managed store
public class RAGContextManager {

    @Reference
    private IEmbeddingStoreProvider storeProvider;

    private EmbeddingStore<TextSegment> getStore() {
        if (storeProvider != null && storeProvider.isAvailable()) {
            return storeProvider.getStore();
        }
        // Fallback to in-memory for development
        return inMemoryStore;
    }
}
```

### Phase P1.2: Knowledge Ingestors

**Goal:** Populate embedding store from iDempiere data sources

#### 1.2.1 Ingestor Interface

```java
package com.cloudempiere.ai.rag.ingest;

/**
 * Interface for knowledge ingestion into embedding store.
 */
public interface IKnowledgeIngestor {

    /** Unique identifier for this ingestor */
    String getSourceType();

    /** Ingest knowledge from source */
    IngestResult ingest(Properties ctx, boolean forceRefresh);

    /** Check if ingestion is needed */
    boolean needsRefresh(Properties ctx);

    /** Get last ingestion timestamp */
    Timestamp getLastIngestion(Properties ctx);
}

public class IngestResult {
    private int documentsAdded;
    private int documentsUpdated;
    private int documentsDeleted;
    private long durationMs;
    private List<String> errors;
}
```

#### 1.2.2 AD Metadata Ingestor

```java
/**
 * Ingests Application Dictionary metadata for semantic search.
 *
 * Sources:
 * - AD_Window (name, description, help)
 * - AD_Tab (name, description, help)
 * - AD_Field (name, description, help)
 * - AD_Process (name, description, help)
 * - AD_Column (name, description)
 * - AD_Table (name, description)
 */
@Component(service = IKnowledgeIngestor.class)
public class ADMetadataIngestor implements IKnowledgeIngestor {

    @Override
    public String getSourceType() {
        return "ad_metadata";
    }

    @Override
    public IngestResult ingest(Properties ctx, boolean forceRefresh) {
        IngestResult result = new IngestResult();

        // Ingest windows
        ingestWindows(ctx, result);

        // Ingest processes
        ingestProcesses(ctx, result);

        // Ingest tables with their columns
        ingestTables(ctx, result);

        return result;
    }

    private void ingestWindows(Properties ctx, IngestResult result) {
        String sql = """
            SELECT w.AD_Window_ID, w.Name, w.Description, w.Help,
                   t.Name as TabName, t.Description as TabDesc,
                   tbl.TableName
            FROM AD_Window w
            JOIN AD_Tab t ON w.AD_Window_ID = t.AD_Window_ID AND t.SeqNo = 10
            JOIN AD_Table tbl ON t.AD_Table_ID = tbl.AD_Table_ID
            WHERE w.IsActive = 'Y' AND t.IsActive = 'Y'
            ORDER BY w.Name
            """;

        // Process and embed each window
        // Store with metadata: source_type='ad_window', source_id=AD_Window_ID
    }
}
```

#### 1.2.3 Knowledge Entry Ingestor

```java
/**
 * Ingests K_Entry records (Knowledge Base articles).
 */
@Component(service = IKnowledgeIngestor.class)
public class KnowledgeEntryIngestor implements IKnowledgeIngestor {

    @Override
    public String getSourceType() {
        return "knowledge_entry";
    }

    @Override
    public IngestResult ingest(Properties ctx, boolean forceRefresh) {
        String sql = """
            SELECT e.K_Entry_ID, e.Name, e.TextMsg, e.Keywords,
                   t.Name as TopicName, c.Name as CategoryName
            FROM K_Entry e
            LEFT JOIN K_Topic t ON e.K_Topic_ID = t.K_Topic_ID
            LEFT JOIN K_Category c ON t.K_Category_ID = c.K_Category_ID
            WHERE e.IsActive = 'Y' AND e.IsPublic = 'Y'
            ORDER BY e.Updated DESC
            """;

        // Process and embed each entry
    }
}
```

#### 1.2.4 Glossary Ingestor (CloudEmpiere-specific)

```java
/**
 * Ingests glossary terms, naming conventions, and formulation patterns.
 *
 * Source options:
 * - Custom AIG_Glossary table
 * - AD_Element with IsGlossary flag
 * - External markdown/JSON files
 */
@Component(service = IKnowledgeIngestor.class)
public class GlossaryIngestor implements IKnowledgeIngestor {

    @Override
    public String getSourceType() {
        return "glossary";
    }
}
```

### Phase P1.3: RAG Tools for Agent

**Goal:** Create @Tool methods so agent can search knowledge

#### 1.3.1 RagTools Class

```java
package com.cloudempiere.ai.provider.langchain4j.tools;

/**
 * RAG tools for AI agent knowledge search.
 *
 * Equivalent to idempiere-cli's RagTools but adapted for OSGi.
 */
public class RagTools {

    private final RagService ragService;
    private final Properties ctx;

    public RagTools(RagService ragService, Properties ctx) {
        this.ragService = ragService;
        this.ctx = ctx;
    }

    /**
     * Search the knowledge base for relevant information.
     *
     * @param query Natural language search query
     * @param sourceFilter Optional filter: 'ad_metadata', 'knowledge_entry', 'glossary', or null for all
     * @return Relevant knowledge snippets with source references
     */
    @Tool("Search the knowledge base for information about iDempiere windows, processes, tables, and business concepts")
    public String searchKnowledge(
            @P("The search query in natural language") String query,
            @P("Optional source filter: 'ad_metadata', 'knowledge_entry', 'glossary', or leave empty for all") String sourceFilter) {

        List<SearchResult> results = ragService.search(ctx, query, sourceFilter, 5);

        if (results.isEmpty()) {
            return "No relevant knowledge found for: " + query;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.size()).append(" relevant items:\n\n");

        for (SearchResult result : results) {
            sb.append("**").append(result.getTitle()).append("**\n");
            sb.append(result.getContent()).append("\n");
            sb.append("_Source: ").append(result.getSourceType())
              .append(" (").append(result.getSourceId()).append(")_\n\n");
        }

        return sb.toString();
    }

    /**
     * Find Application Dictionary entity by name.
     *
     * @param entityName Name of window, process, table, or field
     * @param entityType Type: 'window', 'process', 'table', 'field'
     * @return Entity details with related information
     */
    @Tool("Find iDempiere Application Dictionary entity like windows, processes, tables, or fields")
    public String findADEntity(
            @P("Name of the entity to find") String entityName,
            @P("Type of entity: 'window', 'process', 'table', 'field'") String entityType) {

        return ragService.findADEntity(ctx, entityName, entityType);
    }

    /**
     * Look up glossary term or naming convention.
     *
     * @param term Term to look up
     * @return Definition, usage examples, and related terms
     */
    @Tool("Look up a glossary term, naming convention, or business concept definition")
    public String lookupGlossary(
            @P("Term or concept to look up") String term) {

        return ragService.lookupGlossary(ctx, term);
    }

    /**
     * Get knowledge base statistics.
     *
     * @return Summary of indexed knowledge by source type
     */
    @Tool("Get statistics about the knowledge base content")
    public String getKnowledgeStats() {
        return ragService.getStats(ctx);
    }
}
```

#### 1.3.2 RagService (Core Service)

```java
package com.cloudempiere.ai.rag;

/**
 * Core RAG service for knowledge retrieval.
 *
 * Combines:
 * - EmbeddingStoreProvider (persistence)
 * - EmbeddingModel (semantic search)
 * - Metadata filtering (source types)
 */
@Component(service = IRagService.class, immediate = true)
public class RagService implements IRagService {

    @Reference
    private IEmbeddingStoreProvider storeProvider;

    @Reference
    private IEmbeddingModelProvider modelProvider;

    @Override
    public List<SearchResult> search(Properties ctx, String query,
                                     String sourceFilter, int maxResults) {

        EmbeddingStore<TextSegment> store = storeProvider.getStore();
        EmbeddingModel model = modelProvider.getModel();

        // Create embedding for query
        Embedding queryEmbedding = model.embed(query).content();

        // Build filter if source type specified
        Filter filter = null;
        if (sourceFilter != null && !sourceFilter.isEmpty()) {
            filter = metadataKey("source_type").isEqualTo(sourceFilter);
        }

        // Add client filter for multi-tenancy
        int clientId = Env.getAD_Client_ID(ctx);
        Filter clientFilter = metadataKey("ad_client_id").isEqualTo(clientId);
        filter = filter != null ? filter.and(clientFilter) : clientFilter;

        // Search
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(maxResults)
            .minScore(0.7)
            .filter(filter)
            .build();

        EmbeddingSearchResult<TextSegment> result = store.search(request);

        return result.matches().stream()
            .map(this::toSearchResult)
            .collect(Collectors.toList());
    }

    @Override
    public ContentRetriever getContentRetriever(Properties ctx) {
        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(storeProvider.getStore())
            .embeddingModel(modelProvider.getModel())
            .maxResults(5)
            .minScore(0.7)
            .build();
    }

    @Override
    public void ingestAll(Properties ctx, boolean forceRefresh) {
        // Get all registered ingestors
        for (IKnowledgeIngestor ingestor : ingestors) {
            if (forceRefresh || ingestor.needsRefresh(ctx)) {
                IngestResult result = ingestor.ingest(ctx, forceRefresh);
                log.info("Ingested " + ingestor.getSourceType() + ": " + result);
            }
        }
    }
}
```

---

## Integration with ERPAgent

### Update ERPAgent to Include RagTools

```java
// In AIService or where agent is created
public ERPAgent createAgent(Properties ctx, MAIProvider provider) {
    ChatLanguageModel chatModel = LangChain4jProviderFactory.getOrCreate(provider);

    // ERP tools (existing)
    ERPTools erpTools = new ERPTools(provider, ctx);

    // RAG tools (new)
    RagTools ragTools = new RagTools(ragService, ctx);

    // RAG content retriever for automatic context injection
    ContentRetriever retriever = ragService.getContentRetriever(ctx);

    return AiServices.builder(ERPAgent.class)
        .chatLanguageModel(chatModel)
        .tools(erpTools, ragTools)  // <-- Both tool classes
        .contentRetriever(retriever) // <-- Automatic RAG
        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
        .build();
}
```

---

## File Structure

```
src/com/cloudempiere/ai/
├── rag/
│   ├── RAGContextManager.java       (existing, update)
│   ├── RAGConversationService.java  (existing)
│   ├── RagService.java              (NEW - core service)
│   ├── IRagService.java             (NEW - interface)
│   ├── embedding/
│   │   ├── IEmbeddingStoreProvider.java   (NEW)
│   │   ├── EmbeddingStoreProvider.java    (NEW - pgvector)
│   │   ├── IEmbeddingModelProvider.java   (NEW)
│   │   └── EmbeddingModelProvider.java    (NEW)
│   ├── ingest/
│   │   ├── IKnowledgeIngestor.java        (NEW)
│   │   ├── IngestResult.java              (NEW)
│   │   ├── ADMetadataIngestor.java        (NEW)
│   │   ├── KnowledgeEntryIngestor.java    (NEW)
│   │   └── GlossaryIngestor.java          (NEW)
│   └── dto/
│       └── SearchResult.java              (NEW)
├── provider/
│   └── langchain4j/
│       └── tools/
│           └── RagTools.java              (NEW)
└── ...
```

---

## Dependencies to Add

### pom.xml

```xml
<!-- LangChain4j PgVector -->
<artifactItem>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>0.35.0</version>
</artifactItem>

<!-- PostgreSQL JDBC (for DataSource) - may already be in iDempiere -->
<!-- Check if needed -->
```

### MANIFEST.MF

```
Bundle-ClassPath: .,
 lib/langchain4j-pgvector-0.35.0.jar,
 ...
```

---

## Database Migration

### Migration Script: 202512XX_AIG_Embedding.sql

```sql
-- Migration: Add pgvector support for RAG
-- Ticket: CLD-XXXX

-- 1. Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Create embedding table
CREATE TABLE IF NOT EXISTS aig_embedding (
    aig_embedding_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id INTEGER NOT NULL DEFAULT 0,
    ad_org_id INTEGER NOT NULL DEFAULT 0,
    isactive CHAR(1) DEFAULT 'Y' NOT NULL,
    created TIMESTAMP DEFAULT NOW() NOT NULL,
    createdby INTEGER DEFAULT 100 NOT NULL,
    updated TIMESTAMP DEFAULT NOW() NOT NULL,
    updatedby INTEGER DEFAULT 100 NOT NULL,

    -- Vector data
    embedding vector(768),
    text_segment TEXT,

    -- Metadata
    metadata JSONB,
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(100),
    content_hash VARCHAR(64),

    -- Constraints
    CONSTRAINT aig_embedding_client FOREIGN KEY (ad_client_id)
        REFERENCES ad_client(ad_client_id)
);

-- 3. Create indexes
CREATE INDEX IF NOT EXISTS idx_aig_embedding_hnsw
    ON aig_embedding USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_aig_embedding_client
    ON aig_embedding (ad_client_id);
CREATE INDEX IF NOT EXISTS idx_aig_embedding_source
    ON aig_embedding (source_type);
CREATE INDEX IF NOT EXISTS idx_aig_embedding_hash
    ON aig_embedding (content_hash);

-- 4. Create ingestion tracking table
CREATE TABLE IF NOT EXISTS aig_ingestion_metadata (
    aig_ingestion_metadata_id SERIAL PRIMARY KEY,
    ad_client_id INTEGER NOT NULL DEFAULT 0,
    source_type VARCHAR(50) NOT NULL UNIQUE,
    last_ingestion TIMESTAMP,
    record_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'pending',
    error_message TEXT,
    created TIMESTAMP DEFAULT NOW(),
    updated TIMESTAMP DEFAULT NOW()
);
```

---

## Testing Strategy

### Unit Tests

```java
@Test
void testEmbeddingStoreProvider_isAvailable() {
    // Mock DataSource with pgvector
    // Verify store creation
}

@Test
void testRagService_search() {
    // Pre-populate embeddings
    // Search and verify results
}

@Test
void testADMetadataIngestor_ingest() {
    // Mock AD data
    // Verify embeddings created
}
```

### Integration Tests

```java
@Test
void testEndToEnd_agentUsesRagTools() {
    // Create agent with RagTools
    // Ask question requiring knowledge lookup
    // Verify agent calls searchKnowledge tool
}
```

---

## Implementation Order

| Step | Task | Est. Effort | Dependencies |
|------|------|-------------|--------------|
| 1 | Add langchain4j-pgvector to pom.xml | 0.5h | None |
| 2 | Create database migration | 1h | Step 1 |
| 3 | Implement IEmbeddingStoreProvider | 2h | Steps 1-2 |
| 4 | Implement EmbeddingStoreProvider | 3h | Step 3 |
| 5 | Implement IEmbeddingModelProvider | 1h | None |
| 6 | Implement RagService interface | 1h | None |
| 7 | Implement RagService | 4h | Steps 4-6 |
| 8 | Implement ADMetadataIngestor | 4h | Step 7 |
| 9 | Implement KnowledgeEntryIngestor | 2h | Step 7 |
| 10 | Implement RagTools | 2h | Step 7 |
| 11 | Integrate RagTools with ERPAgent | 2h | Step 10 |
| 12 | Update RAGContextManager | 2h | Step 4 |
| 13 | Write tests | 4h | All |

**Total Estimated: ~28 hours**

---

## Success Criteria

### P1.1 (Vector Storage)
- [ ] pgvector extension enabled
- [ ] aig_embedding table created
- [ ] EmbeddingStoreProvider returns working store
- [ ] Embeddings persist across server restarts

### P1.2 (Ingestors)
- [ ] AD metadata ingested (windows, processes, tables)
- [ ] K_Entry articles ingested
- [ ] Incremental refresh works (only new/changed)

### P1.3 (RagTools)
- [ ] Agent can call searchKnowledge tool
- [ ] Agent can call findADEntity tool
- [ ] Multi-tenant filtering works (AD_Client_ID)

---

## References

- [ADR-027: Implementation Roadmap](docs/adr/027-implementation-roadmap-priority.md)
- [ADR-026: Vector Database Strategy](docs/adr/026-vector-database-strategy.md)
- [ADR-012: RAG-Based Context Retrieval](docs/adr/012-rag-based-context-retrieval.md)
- [idempiere-cli RAG implementation](../idempiere-cli/src/main/java/org/idempiere/cli/rag/)
- [LangChain4j pgvector docs](https://docs.langchain4j.dev/integrations/embedding-stores/pgvector)

---

**P1-CONTEXT-LAYER-IMPLEMENTATION.md | Version 1.0 | 2025-12-10**
