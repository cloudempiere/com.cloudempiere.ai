# ADR-040: Embedding Ingestion Evolution Strategy

## Status
Accepted

## Date
2025-12-10

## Context

This ADR documents the evolution strategy for embedding ingestion in the CloudEmpiere AI plugin. Based on analysis of:
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

## Phase 2: Queue-Based Architecture (Next Iteration)

### Architecture

```
┌──────────────────────┐      ┌──────────────────────┐
│     iDempiere        │      │      PostgreSQL      │
│                      │      │                      │
│  ┌────────────────┐  │      │  ┌────────────────┐  │
│  │ ModelValidator │  │      │  │  aig_outbox    │  │
│  │ (non-blocking) │──┼─────►│  │  status=pending│  │
│  └────────────────┘  │      │  └───────┬────────┘  │
│                      │      │          │           │
│  ┌────────────────┐  │      │          ▼           │
│  │ Scheduler Job  │  │ poll │  ┌────────────────┐  │
│  │ ProcessQueue   │◄─┼──────┼──│  FOR UPDATE    │  │
│  │ (batch)        │  │      │  │  SKIP LOCKED   │  │
│  └───────┬────────┘  │      │  └────────────────┘  │
│          │           │      │                      │
│          ▼           │      │  ┌────────────────┐  │
│  ┌────────────────┐  │      │  │  aig_vector    │  │
│  │ Embed + Store  │──┼─────►│  │  (pgvector)    │  │
│  └────────────────┘  │      │  └────────────────┘  │
│                      │      │                      │
│  ┌────────────────┐  │      │  ┌────────────────┐  │
│  │ Scheduler Job  │  │      │  │aig_ingestion   │  │
│  │ BatchProcess   │──┼─────►│  │  _metadata     │  │
│  │ (pull mode)    │  │      │  └────────────────┘  │
│  └────────────────┘  │      │                      │
└──────────────────────┘      └──────────────────────┘
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

### Database Tables

```sql
-- Outbox: queue of pending work
CREATE TABLE aig_outbox (
    aig_outbox_uu    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id     INTEGER NOT NULL,
    source_type      VARCHAR(30) NOT NULL,
    source_id        INTEGER NOT NULL,
    action           VARCHAR(10) DEFAULT 'UPSERT',
    payload          JSONB NOT NULL,
    status           VARCHAR(20) DEFAULT 'pending',
    error_message    TEXT,
    retry_count      INTEGER DEFAULT 0,
    created          TIMESTAMP DEFAULT now(),
    updated          TIMESTAMP DEFAULT now(),
    CONSTRAINT aig_outbox_status_chk
        CHECK (status IN ('pending','processing','completed','failed','dead_letter'))
);

CREATE INDEX aig_outbox_pending_idx ON aig_outbox(status, created)
    WHERE status = 'pending';

-- Ingestion tracking (for pull mode)
CREATE TABLE aig_ingestion_metadata (
    aig_ingestion_metadata_uu UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id     INTEGER NOT NULL,
    source_type      VARCHAR(30) NOT NULL,
    last_ingestion   TIMESTAMP NOT NULL,
    record_count     INTEGER DEFAULT 0,
    CONSTRAINT aig_ingestion_uq UNIQUE (ad_client_id, source_type)
);
```

### Processing Strategies

#### Push Mode (Real-time via ModelValidator)
```
Save → ModelValidator → INSERT aig_outbox → return immediately
```
- Non-blocking
- Real-time event capture
- Decoupled from embedding API

#### Pull Mode (Batch via Scheduler)
```
Scheduler → Scan AD tables WHERE Updated >= last_ingestion → INSERT aig_outbox
```
- Catches bulk imports
- Recovery mechanism
- Handles missed events

#### Recommended: Use Both
1. **Push** for real-time during normal operation
2. **Pull** as scheduled backup (every 5 min) to catch missed events

### Queue Processing

```java
public class AIGEmbeddingProcessQueue extends SvrProcess {
    @Override
    protected String doIt() {
        // 1. Claim batch with row-level locking
        String sql = """
            UPDATE aig_outbox SET status = 'processing', updated = now()
            WHERE aig_outbox_uu IN (
                SELECT aig_outbox_uu FROM aig_outbox
                WHERE status = 'pending'
                ORDER BY created
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            RETURNING *
            """;

        // 2. Process each item
        for (OutboxItem item : batch) {
            try {
                // DELETE old embeddings
                deleteBySource(item.sourceType, item.sourceId);

                // Generate and INSERT new
                float[] embedding = embeddingService.embed(item.payload);
                insertVector(item, embedding);

                // Mark completed
                item.setStatus("completed");
            } catch (Exception e) {
                item.setRetryCount(item.getRetryCount() + 1);
                if (item.getRetryCount() >= 3) {
                    item.setStatus("dead_letter");
                } else {
                    item.setStatus("pending"); // Will retry
                }
                item.setErrorMessage(e.getMessage());
            }
            item.saveEx();
        }

        return "@Processed@ " + batch.size();
    }
}
```

### Future Enhancements (Phase 3+)

| Enhancement | Description | When |
|-------------|-------------|------|
| **LISTEN/NOTIFY** | Near real-time processing via PostgreSQL | When polling latency matters |
| **Partitioned Queues** | Separate queues per source type | Performance isolation |
| **Content Hash** | Skip unchanged content | Reduce API costs |

---

## Consequences

### Phase 1 (Simple Trigger)
- **Positive**: Fast to implement, easy to debug
- **Negative**: Blocks saves, no retry, risky for production
- **Mitigation**: Use for MVP, move to Phase 2 for production

### Phase 2 (Queue-Based)
- **Positive**: Non-blocking, retry support, scalable
- **Negative**: More complex, eventual consistency
- **Mitigation**: Short polling interval (30s) for near real-time

## Migration Path

```
Phase 1 → Phase 2:
1. Create aig_outbox, aig_ingestion_metadata tables
2. Modify ModelValidator to INSERT outbox instead of direct embed
3. Create ProcessQueue scheduler job
4. Create BatchProcess scheduler job (pull mode backup)
5. Run BatchProcess with Force=Y to re-queue all existing records
```

## Related ADRs

- ADR-012: RAG-Based Context Retrieval
- ADR-026: Vector Database Strategy
- ADR-039: Async Embedding Queue Architecture (Phase 2 design)

## References

- idempiere-cli RAG implementation: `/Users/developer/GitHub/idempiere-cli/src/main/java/org/idempiere/cli/rag/`
- com.cloudempiere.cache patterns: `/Users/developer/GitHub/com.cloudempiere.cache/`
- PostgreSQL pgvector: https://github.com/pgvector/pgvector
- LangChain4j: https://docs.langchain4j.dev/
