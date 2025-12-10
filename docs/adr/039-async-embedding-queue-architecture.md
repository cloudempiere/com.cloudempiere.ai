# ADR-039: Async Embedding Queue Architecture

## Status
Proposed (Phase 2 - Future Implementation)

## Date
2025-12-10

## Supersedes
This ADR documents the **queue-based architecture** for Phase 2.
For the current implementation (Phase 1 - Simple Trigger), see **ADR-040**.

## Context

Phase 1 uses a simple synchronous trigger approach for embedding generation. While simple, it has limitations:

1. **Blocking**: Embedding API call blocks the save transaction
2. **No Retry**: Failed embeddings are lost
3. **Bulk Imports**: Direct SQL imports bypass ModelValidator
4. **Performance**: Synchronous calls slow down user saves

This ADR describes the queue-based approach for Phase 2 to address these limitations.

## Decision

Adopt an **async queue architecture** using the **Transactional Outbox Pattern**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           iDempiere (Java 11)                               │
│                                                                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐         │
│  │  ModelValidator │    │   aig_outbox    │    │   Scheduler     │         │
│  │  (non-blocking) │───►│   (events)      │───►│   ProcessQueue  │         │
│  │                 │    │                 │    │                 │         │
│  │  INSERT outbox  │    │  - source_type  │    │  - poll pending │         │
│  │  return fast    │    │  - source_id    │    │  - call embed   │         │
│  └─────────────────┘    │  - payload      │    │  - store vector │         │
│                         │  - status       │    │  - mark done    │         │
│  ┌─────────────────┐    └─────────────────┘    └────────┬────────┘         │
│  │  Scheduler      │                                    │                   │
│  │  BatchProcess   │─── Pull mode backup ───────────────┤                   │
│  │  (every 5 min)  │                                    │                   │
│  └─────────────────┘                                    ▼                   │
│                         ┌─────────────────┐    ┌─────────────────┐         │
│                         │   aig_vector    │◄───│  Embedding API  │         │
│  ┌─────────────────┐    │   (pgvector)    │    │  (Ollama/OpenAI)│         │
│  │   RagTools      │───►│                 │    └─────────────────┘         │
│  │   (search)      │    │  - embedding    │                                 │
│  └─────────────────┘    │  - text_content │                                 │
│                         │  - metadata     │                                 │
│                         └─────────────────┘                                 │
│                                                                             │
│                         ┌─────────────────┐                                 │
│                         │aig_ingestion    │                                 │
│                         │  _metadata      │◄── tracks last_ingestion        │
│                         └─────────────────┘                                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Patterns

#### From idempiere-cli:
- **Timestamp-based change detection**: `WHERE Updated >= last_ingestion`
- **DELETE + INSERT**: No UPSERT (handles variable segment counts from splitting)
- **Incremental vs Force modes**: Force clears all, incremental uses timestamp
- **JSONB metadata**: For filtering by source_type

#### From com.cloudempiere.cache:
- **FOR UPDATE SKIP LOCKED**: Safe concurrent queue processing
- **Retry with dead letter**: Failed items don't block processing

### Database Tables

#### aig_outbox (Queue)
```sql
CREATE TABLE aig_outbox (
    aig_outbox_uu    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id     INTEGER NOT NULL,
    source_type      VARCHAR(30) NOT NULL,  -- ad_window, ad_tab, ad_process
    source_id        INTEGER NOT NULL,
    action           VARCHAR(10) DEFAULT 'UPSERT',  -- UPSERT or DELETE
    payload          JSONB NOT NULL,         -- name, description, help
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
```

#### aig_vector (Vector Storage)
```sql
CREATE TABLE aig_vector (
    aig_vector_uu    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id     INTEGER NOT NULL,
    source_type      VARCHAR(30) NOT NULL,
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

#### aig_ingestion_metadata (Tracking)
```sql
CREATE TABLE aig_ingestion_metadata (
    aig_ingestion_metadata_uu UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id     INTEGER NOT NULL,
    source_type      VARCHAR(30) NOT NULL,
    last_ingestion   TIMESTAMP NOT NULL,
    record_count     INTEGER DEFAULT 0,
    CONSTRAINT aig_ingestion_uq UNIQUE (ad_client_id, source_type)
);
```

### Event Emission: Push vs Pull

#### Push Mode (Real-time via ModelValidator)
```java
// Non-blocking - just insert to queue
public String modelChange(PO po, int type) {
    if (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE) {
        insertOutbox(po, "UPSERT");  // Fast INSERT, no embedding call
    } else if (type == TYPE_AFTER_DELETE) {
        insertOutbox(po, "DELETE");
    }
    return null;  // Never block save
}
```

#### Pull Mode (Batch via Scheduler)
```sql
-- Catches bulk imports and missed events
INSERT INTO aig_outbox (ad_client_id, source_type, source_id, payload, status)
SELECT ad_client_id, 'ad_window', ad_window_id,
       jsonb_build_object('name', name, 'description', description, 'help', help),
       'pending'
FROM ad_window
WHERE updated >= (SELECT last_ingestion FROM aig_ingestion_metadata WHERE source_type = 'ad_window')
  AND NOT EXISTS (SELECT 1 FROM aig_outbox WHERE source_type = 'ad_window'
                  AND source_id = ad_window.ad_window_id AND status IN ('pending','processing'));
```

#### Recommended: Use Both
1. **Push** for real-time during normal operation
2. **Pull** as scheduled backup (every 5 min) to catch missed events

### Queue Processing

```java
public class AIGEmbeddingProcessQueue extends SvrProcess {
    @Override
    protected String doIt() {
        // 1. Claim batch with row-level locking
        List<MOutbox> batch = claimBatch(p_BatchSize);

        for (MOutbox item : batch) {
            try {
                if ("DELETE".equals(item.getAction())) {
                    deleteVectors(item.getSourceType(), item.getSourceId());
                } else {
                    // DELETE old + INSERT new (handles segment count changes)
                    deleteVectors(item.getSourceType(), item.getSourceId());

                    String content = extractContent(item.getPayload());
                    float[] embedding = embeddingService.embed(content);
                    insertVector(item, content, embedding);
                }
                item.setStatus("completed");
            } catch (Exception e) {
                handleError(item, e);
            }
            item.saveEx();
        }
        return "@Processed@ " + batch.size();
    }

    private List<MOutbox> claimBatch(int size) {
        // FOR UPDATE SKIP LOCKED prevents concurrent processing
        return DB.executeQuery("""
            UPDATE aig_outbox SET status = 'processing', updated = now()
            WHERE aig_outbox_uu IN (
                SELECT aig_outbox_uu FROM aig_outbox
                WHERE status = 'pending'
                ORDER BY created LIMIT ?
                FOR UPDATE SKIP LOCKED
            ) RETURNING *
            """, size);
    }

    private void handleError(MOutbox item, Exception e) {
        item.setRetryCount(item.getRetryCount() + 1);
        item.setErrorMessage(e.getMessage());
        if (item.getRetryCount() >= 3) {
            item.setStatus("dead_letter");
        } else {
            item.setStatus("pending");  // Will retry
        }
    }
}
```

### Re-ingestion on Content Change

When source content changes, we must re-embed:

| Scenario | Outbox Status | Action |
|----------|---------------|--------|
| New record | No entry | INSERT new event |
| Changed record | completed/failed | INSERT new event |
| Already queued | pending/processing | SKIP (already in queue) |

```sql
-- Only skip if already in queue
AND NOT EXISTS (
    SELECT 1 FROM aig_outbox o
    WHERE o.source_type = 'ad_window'
      AND o.source_id = w.AD_Window_ID
      AND o.status IN ('pending', 'processing')  -- Only these!
)
```

### Benefits over Phase 1

| Aspect | Phase 1 (Trigger) | Phase 2 (Queue) |
|--------|-------------------|-----------------|
| Save blocking | Yes | No |
| Retry on failure | No | Yes (3 attempts) |
| Bulk imports | Missed | Caught by pull mode |
| Concurrent processing | No | Yes (SKIP LOCKED) |
| Dead letter handling | No | Yes |
| Observable | Limited | Full audit in outbox |

## Consequences

### Positive
- Non-blocking saves improve user experience
- Retry mechanism handles transient API failures
- Pull mode catches bulk imports and missed events
- Full audit trail in outbox table
- Scalable concurrent processing

### Negative
- More complex than simple trigger
- Eventual consistency (seconds delay)
- Additional tables to manage

### Neutral
- Requires scheduler jobs configuration
- Outbox needs periodic cleanup (completed records)

## Related ADRs
- ADR-012: RAG-Based Context Retrieval
- ADR-026: Vector Database Strategy
- ADR-040: Embedding Ingestion Evolution Strategy (Phase 1)

## References
- idempiere-cli RAG: `/Users/developer/GitHub/idempiere-cli/src/main/java/org/idempiere/cli/rag/`
- com.cloudempiere.cache: `/Users/developer/GitHub/com.cloudempiere.cache/`
- PostgreSQL pgvector: https://github.com/pgvector/pgvector
- Transactional Outbox Pattern: https://microservices.io/patterns/data/transactional-outbox.html
