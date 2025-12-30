-- CLD-1601 - P1 Context Layer - Vector Embedding Storage
SELECT register_migration_script('202512101600_CLD-1601.sql') FROM dual;

-- Purpose: Add pgvector support for RAG-based AI context retrieval
-- Author: Cloudempiere
-- Date: 2025-12-10
-- Reference: ADR-026, ADR-012 (P1 Context Layer)
-- Depends on: pgvector extension must be installed

-- ============================================================================
-- IMPORTANT: pgvector extension must be installed on the PostgreSQL server
-- Run as superuser: CREATE EXTENSION IF NOT EXISTS vector;
-- ============================================================================

-- NOTE: This migration creates tables directly via DDL instead of using
-- Application Dictionary because:
-- 1. pgvector 'vector' data type is not supported by iDempiere's AD framework
-- 2. HNSW indexes require pgvector-specific syntax
-- 3. These tables are infrastructure (like AD_Changelog), not business tables
-- 4. No UI windows/tabs needed - accessed programmatically only

-- =====================================================================
-- 1. Create embedding table for persistent vector storage
-- =====================================================================

CREATE TABLE IF NOT EXISTS AIG_Embedding (
    -- iDempiere standard columns
    AIG_Embedding_UU UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    AD_Client_ID NUMERIC(10) NOT NULL DEFAULT 0,
    AD_Org_ID NUMERIC(10) NOT NULL DEFAULT 0,
    IsActive CHAR(1) DEFAULT 'Y' NOT NULL CHECK (IsActive IN ('Y', 'N')),
    Created TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    CreatedBy NUMERIC(10) DEFAULT 100 NOT NULL,
    Updated TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    UpdatedBy NUMERIC(10) DEFAULT 100 NOT NULL,

    -- Vector embedding data (768 dimensions for nomic-embed-text)
    -- Can also support 1536 for OpenAI text-embedding-3-small
    Embedding vector(768),

    -- Source content
    TextSegment TEXT NOT NULL,
    TextSegmentHash VARCHAR(64),  -- SHA-256 for deduplication

    -- Metadata (LangChain4j stores as JSONB)
    Metadata JSONB,

    -- Source tracking for knowledge management
    SourceType VARCHAR(50) NOT NULL,  -- 'ad_metadata', 'knowledge_entry', 'glossary', 'window_context'
    SourceID VARCHAR(100),             -- AD_Window_ID, K_Entry_ID, etc.
    SourceTable VARCHAR(100),          -- Table name for AD entities

    -- Language support
    AD_Language VARCHAR(6),

    -- Constraints
    CONSTRAINT AIG_Embedding_Client_FK FOREIGN KEY (AD_Client_ID)
        REFERENCES AD_Client(AD_Client_ID) ON DELETE CASCADE
);

-- =====================================================================
-- 2. Create HNSW index for fast approximate nearest neighbor search
-- =====================================================================

-- HNSW (Hierarchical Navigable Small World) is faster than IVFFlat for most use cases
CREATE INDEX IF NOT EXISTS AIG_Embedding_HNSW_Idx
    ON AIG_Embedding USING hnsw (Embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- =====================================================================
-- 3. Create supporting indexes for filtering
-- =====================================================================

CREATE INDEX IF NOT EXISTS AIG_Embedding_Client_Idx
    ON AIG_Embedding (AD_Client_ID);

CREATE INDEX IF NOT EXISTS AIG_Embedding_SourceType_Idx
    ON AIG_Embedding (SourceType);

CREATE INDEX IF NOT EXISTS AIG_Embedding_SourceID_Idx
    ON AIG_Embedding (SourceType, SourceID);

CREATE INDEX IF NOT EXISTS AIG_Embedding_Hash_Idx
    ON AIG_Embedding (TextSegmentHash);

CREATE INDEX IF NOT EXISTS AIG_Embedding_Language_Idx
    ON AIG_Embedding (AD_Language) WHERE AD_Language IS NOT NULL;

-- GIN index for JSONB metadata queries
CREATE INDEX IF NOT EXISTS AIG_Embedding_Metadata_Idx
    ON AIG_Embedding USING gin (Metadata);

-- =====================================================================
-- 4. Create ingestion tracking table
-- =====================================================================

-- Tracks when each source was last ingested for incremental updates
CREATE TABLE IF NOT EXISTS AIG_IngestionMetadata (
    AIG_IngestionMetadata_UU UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    AD_Client_ID NUMERIC(10) NOT NULL DEFAULT 0,
    AD_Org_ID NUMERIC(10) NOT NULL DEFAULT 0,
    IsActive CHAR(1) DEFAULT 'Y' NOT NULL CHECK (IsActive IN ('Y', 'N')),
    Created TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    CreatedBy NUMERIC(10) DEFAULT 100 NOT NULL,
    Updated TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    UpdatedBy NUMERIC(10) DEFAULT 100 NOT NULL,

    -- Ingestor identification
    SourceType VARCHAR(50) NOT NULL,

    -- Ingestion state
    LastIngestion TIMESTAMP WITHOUT TIME ZONE,
    LastSuccessfulIngestion TIMESTAMP WITHOUT TIME ZONE,
    RecordCount NUMERIC(10) DEFAULT 0,
    Status VARCHAR(20) DEFAULT 'pending' CHECK (Status IN ('pending', 'running', 'completed', 'failed')),
    ErrorMessage TEXT,
    DurationMs BIGINT,

    -- Unique constraint per client and source type
    CONSTRAINT AIG_IngestionMetadata_Unique UNIQUE (AD_Client_ID, SourceType),
    CONSTRAINT AIG_IngestionMetadata_Client_FK FOREIGN KEY (AD_Client_ID)
        REFERENCES AD_Client(AD_Client_ID) ON DELETE CASCADE
);

-- =====================================================================
-- 5. Create function to update 'updated' timestamp
-- =====================================================================

CREATE OR REPLACE FUNCTION AIG_Embedding_Update_Timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.Updated = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- 6. Create triggers for auto-updating timestamp
-- =====================================================================

DROP TRIGGER IF EXISTS AIG_Embedding_Updated_Trg ON AIG_Embedding;
CREATE TRIGGER AIG_Embedding_Updated_Trg
    BEFORE UPDATE ON AIG_Embedding
    FOR EACH ROW
    EXECUTE FUNCTION AIG_Embedding_Update_Timestamp();

DROP TRIGGER IF EXISTS AIG_IngestionMetadata_Updated_Trg ON AIG_IngestionMetadata;
CREATE TRIGGER AIG_IngestionMetadata_Updated_Trg
    BEFORE UPDATE ON AIG_IngestionMetadata
    FOR EACH ROW
    EXECUTE FUNCTION AIG_Embedding_Update_Timestamp();

-- =====================================================================
-- 7. Create helper function for similarity search with client filtering
-- =====================================================================

-- This can be called directly or used by LangChain4j's PgVectorEmbeddingStore
CREATE OR REPLACE FUNCTION AIG_Embedding_Search(
    p_query_embedding vector(768),
    p_AD_Client_ID NUMERIC(10),
    p_SourceType VARCHAR(50) DEFAULT NULL,
    p_MaxResults NUMERIC(10) DEFAULT 5,
    p_MinScore DOUBLE PRECISION DEFAULT 0.7
)
RETURNS TABLE (
    Embedding_UU UUID,
    TextSegment TEXT,
    SourceType VARCHAR(50),
    SourceID VARCHAR(100),
    Metadata JSONB,
    Similarity DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        e.AIG_Embedding_UU,
        e.TextSegment,
        e.SourceType,
        e.SourceID,
        e.Metadata,
        1 - (e.Embedding <=> p_query_embedding) AS Similarity
    FROM AIG_Embedding e
    WHERE e.AD_Client_ID = p_AD_Client_ID
      AND e.IsActive = 'Y'
      AND (p_SourceType IS NULL OR e.SourceType = p_SourceType)
      AND (1 - (e.Embedding <=> p_query_embedding)) >= p_MinScore
    ORDER BY e.Embedding <=> p_query_embedding
    LIMIT p_MaxResults;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- 8. Grant permissions (adjust role names as needed)
-- =====================================================================

-- These may need to be run separately by DBA
-- GRANT SELECT, INSERT, UPDATE, DELETE ON AIG_Embedding TO adempiere;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON AIG_IngestionMetadata TO adempiere;
-- GRANT EXECUTE ON FUNCTION AIG_Embedding_Search TO adempiere;

-- =====================================================================
-- 9. Add comments for documentation
-- =====================================================================

COMMENT ON TABLE AIG_Embedding IS 'AI embedding vectors for RAG-based context retrieval (ADR-012, ADR-026)';
COMMENT ON COLUMN AIG_Embedding.Embedding IS 'Vector embedding (768 dimensions for nomic-embed-text)';
COMMENT ON COLUMN AIG_Embedding.SourceType IS 'Knowledge source: ad_metadata, knowledge_entry, glossary, window_context';
COMMENT ON COLUMN AIG_Embedding.Metadata IS 'LangChain4j metadata stored as JSONB';

COMMENT ON TABLE AIG_IngestionMetadata IS 'Tracks knowledge ingestion state for incremental updates';
COMMENT ON COLUMN AIG_IngestionMetadata.SourceType IS 'Ingestor identifier matching IKnowledgeIngestor.getSourceType()';

COMMENT ON FUNCTION AIG_Embedding_Search IS 'Semantic search with client filtering and minimum similarity threshold';
