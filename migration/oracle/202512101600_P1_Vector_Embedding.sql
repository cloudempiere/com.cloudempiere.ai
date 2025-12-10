-- Migration: P1 Context Layer - Vector Embedding Storage
-- Description: Add vector support for RAG-based AI context retrieval
-- Author: Claude Code
-- Date: 2025-12-10
-- Ticket: ADR-026, ADR-012 (P1 Context Layer)
-- Note: Oracle 23ai has native VECTOR type, older versions need workaround

-- ============================================================================
-- IMPORTANT: This migration requires Oracle 23ai for native VECTOR type
-- For older Oracle versions, embeddings would need to be stored differently
-- ============================================================================

-- 1. Create embedding table for persistent vector storage
-- Oracle 23ai VECTOR type stores high-dimensional vectors natively
CREATE TABLE aig_embedding (
    -- iDempiere standard columns
    aig_embedding_uu RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    ad_client_id NUMBER(10) DEFAULT 0 NOT NULL,
    ad_org_id NUMBER(10) DEFAULT 0 NOT NULL,
    isactive CHAR(1) DEFAULT 'Y' NOT NULL CHECK (isactive IN ('Y', 'N')),
    created TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    createdby NUMBER(10) DEFAULT 100 NOT NULL,
    updated TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updatedby NUMBER(10) DEFAULT 100 NOT NULL,

    -- Vector embedding data (768 dimensions for nomic-embed-text)
    -- Oracle 23ai: VECTOR(768, FLOAT32)
    -- For older versions: BLOB or CLOB with serialized vector
    embedding VECTOR(768, FLOAT32),

    -- Source content
    text_segment CLOB NOT NULL,
    text_segment_hash VARCHAR2(64),  -- SHA-256 for deduplication

    -- Metadata (stored as JSON in CLOB)
    metadata CLOB CHECK (metadata IS JSON),

    -- Source tracking for knowledge management
    source_type VARCHAR2(50) NOT NULL,  -- 'ad_metadata', 'knowledge_entry', 'glossary', 'window_context'
    source_id VARCHAR2(100),             -- AD_Window_ID, K_Entry_ID, etc.
    source_table VARCHAR2(100),          -- Table name for AD entities

    -- Language support
    ad_language VARCHAR2(6),

    -- Constraints
    CONSTRAINT aig_embedding_client_fk FOREIGN KEY (ad_client_id)
        REFERENCES ad_client(ad_client_id) ON DELETE CASCADE
);

-- 2. Create vector index for similarity search (Oracle 23ai)
CREATE VECTOR INDEX idx_aig_embedding_vec
    ON aig_embedding (embedding)
    ORGANIZATION NEIGHBOR PARTITIONS
    WITH DISTANCE COSINE;

-- 3. Create supporting indexes
CREATE INDEX idx_aig_embedding_client ON aig_embedding (ad_client_id);
CREATE INDEX idx_aig_embedding_source_type ON aig_embedding (source_type);
CREATE INDEX idx_aig_embedding_source_id ON aig_embedding (source_type, source_id);
CREATE INDEX idx_aig_embedding_hash ON aig_embedding (text_segment_hash);

-- JSON search index for metadata queries
CREATE SEARCH INDEX idx_aig_embedding_metadata ON aig_embedding (metadata) FOR JSON;

-- 4. Create ingestion tracking table
CREATE TABLE aig_ingestion_metadata (
    aig_ingestion_metadata_uu RAW(16) DEFAULT SYS_GUID() PRIMARY KEY,
    ad_client_id NUMBER(10) DEFAULT 0 NOT NULL,
    ad_org_id NUMBER(10) DEFAULT 0 NOT NULL,
    isactive CHAR(1) DEFAULT 'Y' NOT NULL CHECK (isactive IN ('Y', 'N')),
    created TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    createdby NUMBER(10) DEFAULT 100 NOT NULL,
    updated TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updatedby NUMBER(10) DEFAULT 100 NOT NULL,

    -- Ingestor identification
    source_type VARCHAR2(50) NOT NULL,

    -- Ingestion state
    last_ingestion TIMESTAMP,
    last_successful_ingestion TIMESTAMP,
    record_count NUMBER(10) DEFAULT 0,
    status VARCHAR2(20) DEFAULT 'pending' CHECK (status IN ('pending', 'running', 'completed', 'failed')),
    error_message CLOB,
    duration_ms NUMBER(19),

    -- Unique constraint per client and source type
    CONSTRAINT aig_ingestion_metadata_unique UNIQUE (ad_client_id, source_type),
    CONSTRAINT aig_ingestion_metadata_client_fk FOREIGN KEY (ad_client_id)
        REFERENCES ad_client(ad_client_id) ON DELETE CASCADE
);

-- 5. Create trigger for auto-updating timestamp
CREATE OR REPLACE TRIGGER trg_aig_embedding_updated
    BEFORE UPDATE ON aig_embedding
    FOR EACH ROW
BEGIN
    :NEW.updated := SYSTIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_aig_ingestion_updated
    BEFORE UPDATE ON aig_ingestion_metadata
    FOR EACH ROW
BEGIN
    :NEW.updated := SYSTIMESTAMP;
END;
/

-- 6. Add comments for documentation
COMMENT ON TABLE aig_embedding IS 'AI embedding vectors for RAG-based context retrieval (ADR-012, ADR-026)';
COMMENT ON COLUMN aig_embedding.embedding IS 'Vector embedding (768 dimensions for nomic-embed-text)';
COMMENT ON COLUMN aig_embedding.source_type IS 'Knowledge source: ad_metadata, knowledge_entry, glossary, window_context';
COMMENT ON COLUMN aig_embedding.metadata IS 'LangChain4j metadata stored as JSON';

COMMENT ON TABLE aig_ingestion_metadata IS 'Tracks knowledge ingestion state for incremental updates';
COMMENT ON COLUMN aig_ingestion_metadata.source_type IS 'Ingestor identifier matching IKnowledgeIngestor.getSourceType()';
