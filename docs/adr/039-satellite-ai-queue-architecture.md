# ADR-039: Satellite AI Queue Architecture

## Status
Proposed

## Date
2025-12-10

## Context

The initial P1 Context Layer implementation (ADR-012, ADR-026) attempted to perform embedding generation directly within the iDempiere plugin. This approach has fundamental limitations:

1. **Java Version Constraint**: iDempiere v10 requires Java 11, but LangChain4j versions 0.36.0+ require Java 17
2. **Schema Mismatch**: Custom migration creates tables that LangChain4j's PgVectorEmbeddingStore doesn't use correctly
3. **Synchronous Processing**: Embedding generation blocks the iDempiere thread
4. **Limited Features**: LangChain4j 0.35.0 (Java 11) lacks advanced embedding capabilities

## Decision

Adopt a **queue-based satellite architecture** where:

1. **iDempiere Plugin (Java 11)**: Detects changes and queues work, performs search queries only
2. **Quarkus Satellite Service (Java 17+)**: Processes queue, generates embeddings, manages vector storage

### Architecture

```
┌──────────────────────┐      ┌──────────────────────┐      ┌──────────────────────┐
│     iDempiere        │      │      PostgreSQL      │      │   Quarkus Satellite  │
│   (Java 11 Plugin)   │      │                      │      │   (Java 17+)         │
│                      │      │                      │      │                      │
│  ┌────────────────┐  │      │  ┌────────────────┐  │      │  ┌────────────────┐  │
│  │ Model Event    │  │      │  │aig_embedding   │  │ poll │  │ Queue Processor│  │
│  │ Handlers       │──┼─────►│  │    _queue      │◄─┼──────┼──│                │  │
│  │                │  │      │  │                │  │      │  │ - fetch pending│  │
│  │ - AD_Window    │  │      │  │ - ad_client_id │  │      │  │ - generate emb │  │
│  │ - AD_Process   │  │      │  │ - source_type  │  │      │  │ - store vector │  │
│  │ - K_Entry      │  │      │  │ - source_id    │  │      │  │ - mark done    │  │
│  └────────────────┘  │      │  │ - status       │  │      │  └────────────────┘  │
│                      │      │  │ - payload      │  │      │         │            │
│                      │      │  └────────────────┘  │      │         ▼            │
│                      │      │                      │      │  ┌────────────────┐  │
│  ┌────────────────┐  │      │  ┌────────────────┐  │      │  │ LangChain4j    │  │
│  │ RagTools       │  │ query│  │ aig_vector     │  │ store│  │ 1.x (Java 17)  │  │
│  │ (search only)  │◄─┼──────┼──│ (pgvector)     │◄─┼──────┼──│                │  │
│  └────────────────┘  │      │  │                │  │      │  │ EmbeddingModel │  │
│                      │      │  │ - embedding    │  │      │  │ + pgvector     │  │
│                      │      │  │ - text         │  │      │  └────────────────┘  │
│                      │      │  │ - metadata     │  │      │                      │
│                      │      │  └────────────────┘  │      │                      │
│                      │      │                      │      │                      │
│                      │      │  ┌────────────────┐  │      │                      │
│                      │      │  │aig_ingestion   │  │      │                      │
│                      │      │  │  _metadata     │◄─┼──────┼── track progress     │
│                      │      │  └────────────────┘  │      │                      │
└──────────────────────┘      └──────────────────────┘      └──────────────────────┘
```

### Database Tables

#### aig_embedding_queue (iDempiere managed)
Queue table for pending embedding work:
- `aig_embedding_queue_uu` - UUID primary key
- `ad_client_id`, `ad_org_id` - Multi-tenant columns
- `source_type` - Type: 'ad_window', 'ad_process', 'ad_table', 'k_entry', 'glossary'
- `source_id` - Record ID (e.g., AD_Window_ID)
- `action` - 'INSERT', 'UPDATE', 'DELETE'
- `status` - 'pending', 'processing', 'completed', 'failed'
- `payload` - JSONB with text content to embed
- `error_message` - Error details if failed
- `retry_count` - Number of processing attempts
- `created`, `updated` - Timestamps

#### aig_vector (LangChain4j managed)
Vector storage table created by LangChain4j PgVectorEmbeddingStore:
- `embedding_id` - UUID
- `embedding` - vector(768) or vector(1536)
- `text` - Original text segment
- `metadata` - JSONB with source_type, source_id, ad_client_id, etc.

#### aig_ingestion_metadata (Shared)
Tracks ingestion progress per source type and client.

### Processing Flow

1. **Change Detection** (iDempiere):
   - ModelValidator or EventHandler detects AD_Window/AD_Process/K_Entry change
   - Inserts record into `aig_embedding_queue` with status='pending'
   - Includes payload with text content (name, description, help)

2. **Queue Processing** (Quarkus Satellite):
   - Polls `aig_embedding_queue` for status='pending' records
   - Sets status='processing'
   - Generates embedding via LangChain4j EmbeddingModel
   - Stores in `aig_vector` via PgVectorEmbeddingStore
   - Sets status='completed' or status='failed' with error

3. **Search** (iDempiere):
   - RagTools queries `aig_vector` directly using SQL
   - No LangChain4j required for search - simple pgvector query
   - Returns results to AI agent

### Benefits

1. **Decoupled Processing**: iDempiere doesn't block on embedding generation
2. **Java 17 Features**: Satellite uses LangChain4j 1.x with full feature set
3. **Scalability**: Satellite can be scaled independently
4. **Resilience**: Failed embeddings can be retried
5. **Clean Separation**: iDempiere handles business events, satellite handles AI

### iDempiere Plugin Changes

Remove from plugin:
- `EmbeddingStoreProvider` - No longer needed
- `ADMetadataIngestor` - Replaced by event handlers + queue
- `LangChain4jProviderFactory.createEmbeddingModel()` - Satellite handles this

Keep in plugin:
- `RagTools` - Modified to query `aig_vector` directly via SQL
- `IRagService/RagService` - Modified for search-only operations
- Event handlers for queue insertion

Add to plugin:
- `EmbeddingQueueService` - Inserts into `aig_embedding_queue`
- Model validators for AD_Window, AD_Process, AD_Table, K_Entry

### Quarkus Satellite Service

New Quarkus project with:
- LangChain4j 1.x (Java 17+)
- Scheduled queue processor
- PgVectorEmbeddingStore for storage
- Multiple EmbeddingModel support (Ollama, OpenAI, etc.)
- REST API for manual operations

## Consequences

### Positive
- Proper separation of concerns
- Full LangChain4j feature set in satellite
- Async processing doesn't block iDempiere
- Easier testing and deployment

### Negative
- Additional service to deploy and maintain
- Network latency for queue operations
- More complex infrastructure

### Neutral
- Requires pgvector extension on PostgreSQL
- Queue table adds storage overhead

## Related ADRs
- ADR-012: RAG-Based Context Retrieval
- ADR-026: Vector Database Strategy
- ADR-035: Java Version Strategy
- ADR-038: Quarkus Satellite AI Service

## References
- LangChain4j PgVector: https://docs.langchain4j.dev/integrations/embedding-stores/pgvector
- Quarkus LangChain4j: https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html
