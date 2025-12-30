# ADR-040: Embedding Ingestion Evolution Strategy

## Status
Accepted

## Date
2025-12-10

## Context

This ADR documents the evolution strategy for embedding ingestion in the Cloudempiere AI plugin. Based on analysis of:
- **idempiere-cli** - RAG architecture with timestamp-based change detection, DELETE+INSERT pattern
- **com.cloudempiere.cache** - PostgreSQL LISTEN/NOTIFY, partitioned queues, event-driven invalidation

The goal is to enable RAG (Retrieval-Augmented Generation) by embedding Application Dictionary metadata (windows, tabs, processes, tables) and making it searchable via pgvector.

## Decision

Implement embedding ingestion in **two phases**:

| Phase | Approach | Complexity | When |
|-------|----------|------------|------|
| **Phase 1** | Simple trigger + direct embedding | Low | Now (MVP) |
| **Phase 2** | Queue-based async architecture | Medium | Next iteration |

---

## Phase 1: Simple Trigger Implementation (Current)

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    iDempiere (Java 11)                      │
│                                                             │
│  User saves AD_Window                                       │
│         │                                                   │
│         ▼                                                   │
│  ┌─────────────────┐                                        │
│  │ ModelValidator  │                                        │
│  │ afterSave()     │                                        │
│  └────────┬────────┘                                        │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────┐     ┌─────────────────┐               │
│  │ EmbeddingService│────►│ Ollama/OpenAI   │               │
│  │ (sync call)     │     │ (HTTP API)      │               │
│  └────────┬────────┘     └─────────────────┘               │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────┐     ┌─────────────────┐               │
│  │ DELETE old      │────►│ aig_vector      │               │
│  │ INSERT new      │     │ (pgvector)      │               │
│  └─────────────────┘     └─────────────────┘               │
│                                                             │
│  ┌─────────────────┐                                        │
│  │ RagTools        │────► Search aig_vector                 │
│  └─────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
```

### Components

#### 1. Database Table
```sql
CREATE TABLE aig_vector (
    aig_vector_uu    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id     INTEGER NOT NULL,
    source_type      VARCHAR(30) NOT NULL,  -- ad_window, ad_tab, ad_process, ad_table
    source_id        INTEGER NOT NULL,
    source_uu        UUID,
    embedding        vector(1536),
    text_content     TEXT NOT NULL,
    metadata         JSONB,
    created          TIMESTAMP DEFAULT now(),
    updated          TIMESTAMP DEFAULT now()
);

CREATE INDEX aig_vector_source_idx ON aig_vector(ad_client_id, source_type, source_id);
CREATE INDEX aig_vector_embedding_idx ON aig_vector
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

#### 2. ModelValidator
```java
public class AIGEmbeddingValidator implements ModelValidator {

    @Override
    public String modelChange(PO po, int type) {
        if (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE) {
            if (isEmbeddableTable(po.get_TableName())) {
                embedRecord(po);
            }
        } else if (type == TYPE_AFTER_DELETE) {
            deleteEmbedding(po);
        }
        return null;
    }

    private void embedRecord(PO po) {
        String content = extractContent(po);
        float[] embedding = embeddingService.embed(content);

        // DELETE + INSERT pattern (from idempiere-cli)
        deleteEmbedding(po);
        insertEmbedding(po, content, embedding);
    }
}
```

#### 3. Initial Data Load Process
```java
public class AIGEmbeddingInitialLoad extends SvrProcess {
    // Run once to embed all existing AD records
    // Parameters: SourceType (ALL/ad_window/ad_tab/etc.)
}
```

### Pros & Cons

| Pros | Cons |
|------|------|
| Simple to implement | Blocks save transaction |
| Real-time updates | API failure can break saves |
| No additional infrastructure | No retry mechanism |
| Easy to understand | Synchronous = slow for bulk |

### When to Use
- MVP / proof of concept
- Low volume environments
- When embedding API is fast and reliable (local Ollama)

---

## Phase 2: Queue-Based Architecture via com.cloudempiere.cache (Next Iteration)

### Architecture

Leverage the existing **com.cloudempiere.cache** infrastructure for queue processing:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           iDempiere                                         │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    com.cloudempiere.cache                            │   │
│  │                                                                      │   │
│  │  ┌────────────────┐    ┌────────────────┐    ┌────────────────┐     │   │
│  │  │ PostgreSQL     │    │ Partitioned    │    │ CacheRefresh   │     │   │
│  │  │ LISTEN/NOTIFY  │───►│ QueueManager   │───►│ Strategy       │     │   │
│  │  │ (<500ms)       │    │                │    │ (embedding)    │     │   │
│  │  └────────────────┘    └────────────────┘    └───────┬────────┘     │   │
│  │                                                      │               │   │
│  └──────────────────────────────────────────────────────┼───────────────┘   │
│                                                         │                   │
│  ┌─────────────────────────────────────────────────────┐│                   │
│  │                 com.cloudempiere.ai                  ││                   │
│  │                                                      ▼│                   │
│  │  ┌────────────────┐    ┌────────────────┐    ┌────────────────┐         │
│  │  │ ModelValidator │    │ EmbeddingRefresh│    │ aig_vector     │         │
│  │  │ (triggers      │───►│ Strategy        │───►│ (pgvector)     │         │
│  │  │  pg_notify)    │    │ (implements     │    │                │         │
│  │  └────────────────┘    │  ICacheRefresh) │    └────────────────┘         │
│  │                        └────────────────┘                                │
│  │  ┌────────────────┐                                                      │
│  │  │ RagTools       │───► Search aig_vector                                │
│  │  └────────────────┘                                                      │
│  └──────────────────────────────────────────────────────────────────────────┘
└─────────────────────────────────────────────────────────────────────────────┘
```

### Why Use com.cloudempiere.cache?

| Feature | Build from scratch | Use cache plugin |
|---------|-------------------|------------------|
| LISTEN/NOTIFY | Must implement | Already done |
| Partitioned queues | Must implement | Already done |
| FOR UPDATE SKIP LOCKED | Must implement | Already done |
| Dead letter queue | Must implement | Already done |
| Retry mechanism | Must implement | Already done |
| Multi-tenant support | Must implement | Already done |
| Monitoring/diagnostics | Must implement | Already done |

### Integration Approach

#### 1. Register Embedding as Cache Distribution Behavior

Configure in AD_CacheDistributionBehavior:
- **Path Pattern**: `aig_embedding`
- **Source Tables**: `AD_Window`, `AD_Tab`, `AD_Process`, `AD_Table`
- **Strategy**: `EmbeddingRefreshStrategy`

#### 2. Implement ICacheRefreshStrategy

```java
public class EmbeddingRefreshStrategy implements ICacheRefreshStrategy {

    @Override
    public CacheRefreshResult refresh(CacheRefreshEvent event) {
        String tableName = event.getTableName();
        int recordId = event.getRecordId();
        String action = event.getTriggerAction();  // INSERT, UPDATE, DELETE

        if ("DELETE".equals(action)) {
            deleteEmbedding(tableName, recordId);
        } else {
            // DELETE + INSERT pattern
            deleteEmbedding(tableName, recordId);

            String content = extractContent(tableName, recordId);
            float[] embedding = embeddingService.embed(content);
            insertEmbedding(tableName, recordId, content, embedding);
        }

        return CacheRefreshResult.success();
    }

    @Override
    public String getStrategyName() {
        return "EmbeddingRefreshStrategy";
    }
}
```

#### 3. Database Trigger (handled by cache plugin)

The cache plugin already creates triggers that call `pg_notify`:

```sql
-- Trigger created by cache plugin for registered tables
CREATE TRIGGER aig_ad_window_notify
AFTER INSERT OR UPDATE OR DELETE ON ad_window
FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();
```

### Key Patterns (From Repository Analysis)

#### From idempiere-cli:
1. **Timestamp-based change detection** - `WHERE Updated >= last_ingestion`
2. **DELETE + INSERT** - No UPSERT (handles variable segment counts)
3. **Incremental vs Force modes** - Force clears all, incremental uses timestamp
4. **JSONB metadata** - For filtering by source_type

#### From com.cloudempiere.cache:
1. **PostgreSQL LISTEN/NOTIFY** - Near real-time (<500ms)
2. **Partitioned queues** - Prevent large workloads blocking small ones
3. **FOR UPDATE SKIP LOCKED** - Safe concurrent processing
4. **Dead letter queue** - Failed items don't block processing
5. **Multi-tenant context** - AD_Client_ID in every event
6. **Configuration-driven** - Zero Java code for new sources

### Database Tables

```sql
-- Vector storage (same as Phase 1)
-- aig_vector table already exists

-- Ingestion tracking (for pull mode / initial load)
CREATE TABLE aig_ingestion_metadata (
    aig_ingestion_metadata_uu UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id     INTEGER NOT NULL,
    source_type      VARCHAR(30) NOT NULL,
    last_ingestion   TIMESTAMP NOT NULL,
    record_count     INTEGER DEFAULT 0,
    CONSTRAINT aig_ingestion_uq UNIQUE (ad_client_id, source_type)
);
```

**Note:** No `aig_outbox` table needed - the cache plugin handles queuing internally.

### Processing Strategies

#### Push Mode (Real-time via Cache Plugin)
```
Save AD_Window → DB Trigger → pg_notify → Cache Plugin → EmbeddingRefreshStrategy
```
- Near real-time (<500ms)
- Non-blocking for user
- Automatic retry on failure

#### Pull Mode (Initial Load / Recovery)
```java
public class AIGEmbeddingInitialLoad extends SvrProcess {
    // Run once to embed all existing AD records
    // Or run with Force=Y to re-embed everything
}
```
- For initial data population
- For recovery after failures
- For bulk re-processing

### Configuration

Register embedding sources in Application Dictionary:

| Table | AD_CacheDistributionBehavior |
|-------|------------------------------|
| AD_Window | path_pattern='aig_embedding', source_table='AD_Window' |
| AD_Tab | path_pattern='aig_embedding', source_table='AD_Tab' |
| AD_Process | path_pattern='aig_embedding', source_table='AD_Process' |
| AD_Table | path_pattern='aig_embedding', source_table='AD_Table' |

### Benefits of Cache Plugin Integration

1. **Proven infrastructure** - Already running in production
2. **No duplicate code** - Reuse queue, retry, monitoring
3. **Consistent patterns** - Same approach as other cache invalidations
4. **Easy to add sources** - Just register new table in AD
5. **Built-in diagnostics** - Queue depth, processing stats

---

## Consequences

### Phase 1 (Simple Trigger)
- **Positive**: Fast to implement, easy to debug
- **Negative**: Blocks saves, no retry, risky for production
- **Mitigation**: Use for MVP, move to Phase 2 for production

### Phase 2 (Cache Plugin Integration)
- **Positive**: Non-blocking, retry support, proven infrastructure, no duplicate code
- **Negative**: Dependency on cache plugin, eventual consistency
- **Mitigation**: Cache plugin already deployed; <500ms latency is acceptable

## Migration Path

```
Phase 1 → Phase 2:
1. Add com.cloudempiere.cache as plugin dependency
2. Create aig_ingestion_metadata table
3. Implement EmbeddingRefreshStrategy (ICacheRefreshStrategy)
4. Register AD_Window, AD_Tab, AD_Process, AD_Table in AD_CacheDistributionBehavior
5. Remove ModelValidator direct embedding logic
6. Run AIGEmbeddingInitialLoad with Force=Y to re-embed all existing records
```

## Related ADRs

- ADR-012: RAG-Based Context Retrieval
- ADR-026: Vector Database Strategy

## References

- idempiere-cli RAG implementation: `/Users/developer/GitHub/idempiere-cli/src/main/java/org/idempiere/cli/rag/`
- com.cloudempiere.cache patterns: `/Users/developer/GitHub/com.cloudempiere.cache/`
- PostgreSQL pgvector: https://github.com/pgvector/pgvector
- LangChain4j: https://docs.langchain4j.dev/
