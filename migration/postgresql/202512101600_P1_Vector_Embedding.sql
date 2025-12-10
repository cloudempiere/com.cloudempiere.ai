-- Migration: P1 Context Layer - Vector Embedding Storage
-- Description: Add pgvector support for RAG-based AI context retrieval
-- Author: Claude Code
-- Date: 2025-12-10
-- Ticket: ADR-026, ADR-012 (P1 Context Layer)
-- Depends on: pgvector extension must be installed

-- ============================================================================
-- IMPORTANT: pgvector extension must be installed on the PostgreSQL server
-- Run as superuser: CREATE EXTENSION IF NOT EXISTS vector;
-- ============================================================================

-- 1. Create embedding table for persistent vector storage
-- Replaces InMemoryEmbeddingStore with persistent pgvector storage
CREATE TABLE IF NOT EXISTS aig_embedding (
    -- iDempiere standard columns
    aig_embedding_uu UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id INTEGER NOT NULL DEFAULT 0,
    ad_org_id INTEGER NOT NULL DEFAULT 0,
    isactive CHAR(1) DEFAULT 'Y' NOT NULL CHECK (isactive IN ('Y', 'N')),
    created TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    createdby INTEGER DEFAULT 100 NOT NULL,
    updated TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    updatedby INTEGER DEFAULT 100 NOT NULL,

    -- Vector embedding data (768 dimensions for nomic-embed-text)
    -- Can also support 1536 for OpenAI text-embedding-3-small
    embedding vector(768),

    -- Source content
    text_segment TEXT NOT NULL,
    text_segment_hash VARCHAR(64),  -- SHA-256 for deduplication

    -- Metadata (LangChain4j stores as JSONB)
    metadata JSONB,

    -- Source tracking for knowledge management
    source_type VARCHAR(50) NOT NULL,  -- 'ad_metadata', 'knowledge_entry', 'glossary', 'window_context'
    source_id VARCHAR(100),             -- AD_Window_ID, K_Entry_ID, etc.
    source_table VARCHAR(100),          -- Table name for AD entities

    -- Language support
    ad_language VARCHAR(6),

    -- Constraints
    CONSTRAINT aig_embedding_client_fk FOREIGN KEY (ad_client_id)
        REFERENCES ad_client(ad_client_id) ON DELETE CASCADE
);

-- 2. Create HNSW index for fast approximate nearest neighbor search
-- HNSW (Hierarchical Navigable Small World) is faster than IVFFlat for most use cases
CREATE INDEX IF NOT EXISTS idx_aig_embedding_hnsw
    ON aig_embedding USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 3. Create supporting indexes for filtering
CREATE INDEX IF NOT EXISTS idx_aig_embedding_client
    ON aig_embedding (ad_client_id);

CREATE INDEX IF NOT EXISTS idx_aig_embedding_source_type
    ON aig_embedding (source_type);

CREATE INDEX IF NOT EXISTS idx_aig_embedding_source_id
    ON aig_embedding (source_type, source_id);

CREATE INDEX IF NOT EXISTS idx_aig_embedding_hash
    ON aig_embedding (text_segment_hash);

CREATE INDEX IF NOT EXISTS idx_aig_embedding_language
    ON aig_embedding (ad_language) WHERE ad_language IS NOT NULL;

-- GIN index for JSONB metadata queries
CREATE INDEX IF NOT EXISTS idx_aig_embedding_metadata
    ON aig_embedding USING gin (metadata);

-- 4. Create ingestion tracking table
-- Tracks when each source was last ingested for incremental updates
CREATE TABLE IF NOT EXISTS aig_ingestion_metadata (
    aig_ingestion_metadata_uu UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_client_id INTEGER NOT NULL DEFAULT 0,
    ad_org_id INTEGER NOT NULL DEFAULT 0,
    isactive CHAR(1) DEFAULT 'Y' NOT NULL CHECK (isactive IN ('Y', 'N')),
    created TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    createdby INTEGER DEFAULT 100 NOT NULL,
    updated TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    updatedby INTEGER DEFAULT 100 NOT NULL,

    -- Ingestor identification
    source_type VARCHAR(50) NOT NULL,

    -- Ingestion state
    last_ingestion TIMESTAMP WITHOUT TIME ZONE,
    last_successful_ingestion TIMESTAMP WITHOUT TIME ZONE,
    record_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'running', 'completed', 'failed')),
    error_message TEXT,
    duration_ms BIGINT,

    -- Unique constraint per client and source type
    CONSTRAINT aig_ingestion_metadata_unique UNIQUE (ad_client_id, source_type),
    CONSTRAINT aig_ingestion_metadata_client_fk FOREIGN KEY (ad_client_id)
        REFERENCES ad_client(ad_client_id) ON DELETE CASCADE
);

-- 5. Create function to update 'updated' timestamp
CREATE OR REPLACE FUNCTION aig_embedding_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 6. Create trigger for auto-updating timestamp
DROP TRIGGER IF EXISTS trg_aig_embedding_updated ON aig_embedding;
CREATE TRIGGER trg_aig_embedding_updated
    BEFORE UPDATE ON aig_embedding
    FOR EACH ROW
    EXECUTE FUNCTION aig_embedding_update_timestamp();

DROP TRIGGER IF EXISTS trg_aig_ingestion_metadata_updated ON aig_ingestion_metadata;
CREATE TRIGGER trg_aig_ingestion_metadata_updated
    BEFORE UPDATE ON aig_ingestion_metadata
    FOR EACH ROW
    EXECUTE FUNCTION aig_embedding_update_timestamp();

-- 7. Create helper function for similarity search with client filtering
-- This can be called directly or used by LangChain4j's PgVectorEmbeddingStore
CREATE OR REPLACE FUNCTION aig_embedding_search(
    query_embedding vector(768),
    p_ad_client_id INTEGER,
    p_source_type VARCHAR(50) DEFAULT NULL,
    p_max_results INTEGER DEFAULT 5,
    p_min_score DOUBLE PRECISION DEFAULT 0.7
)
RETURNS TABLE (
    embedding_uu UUID,
    text_segment TEXT,
    source_type VARCHAR(50),
    source_id VARCHAR(100),
    metadata JSONB,
    similarity DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        e.aig_embedding_uu,
        e.text_segment,
        e.source_type,
        e.source_id,
        e.metadata,
        1 - (e.embedding <=> query_embedding) AS similarity
    FROM aig_embedding e
    WHERE e.ad_client_id = p_ad_client_id
      AND e.isactive = 'Y'
      AND (p_source_type IS NULL OR e.source_type = p_source_type)
      AND (1 - (e.embedding <=> query_embedding)) >= p_min_score
    ORDER BY e.embedding <=> query_embedding
    LIMIT p_max_results;
END;
$$ LANGUAGE plpgsql;

-- 8. Grant permissions (adjust role names as needed for your installation)
-- These may need to be run separately by DBA
-- GRANT SELECT, INSERT, UPDATE, DELETE ON aig_embedding TO adempiere;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON aig_ingestion_metadata TO adempiere;
-- GRANT EXECUTE ON FUNCTION aig_embedding_search TO adempiere;

-- 9. Add comments for documentation
COMMENT ON TABLE aig_embedding IS 'AI embedding vectors for RAG-based context retrieval (ADR-012, ADR-026)';
COMMENT ON COLUMN aig_embedding.embedding IS 'Vector embedding (768 dimensions for nomic-embed-text)';
COMMENT ON COLUMN aig_embedding.source_type IS 'Knowledge source: ad_metadata, knowledge_entry, glossary, window_context';
COMMENT ON COLUMN aig_embedding.metadata IS 'LangChain4j metadata stored as JSONB';

COMMENT ON TABLE aig_ingestion_metadata IS 'Tracks knowledge ingestion state for incremental updates';
COMMENT ON COLUMN aig_ingestion_metadata.source_type IS 'Ingestor identifier matching IKnowledgeIngestor.getSourceType()';

COMMENT ON FUNCTION aig_embedding_search IS 'Semantic search with client filtering and minimum similarity threshold';
