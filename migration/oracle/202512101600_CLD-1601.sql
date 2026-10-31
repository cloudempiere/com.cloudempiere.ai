-- CLD-1601 - P1 Context Layer - Vector Embedding Storage
SELECT register_migration_script('202512101600_CLD-1601.sql') FROM dual;

SET SQLBLANKLINES ON
SET DEFINE OFF

-- Purpose: Add vector embedding support for RAG-based AI context retrieval
-- Author: Cloudempiere
-- Date: 2025-12-10
-- Reference: ADR-026, ADR-012 (P1 Context Layer)

-- ============================================================================
-- IMPORTANT: This feature requires vector database support
-- ============================================================================
--
-- ORACLE SUPPORT STATUS:
-- Oracle 23c introduced AI Vector Search with VECTOR data type
-- Oracle 19c and earlier: Use Oracle Text or external vector database
--
-- OPTIONS FOR ORACLE DEPLOYMENTS:
-- 1. Oracle 23c+: Use native VECTOR type (not yet implemented in this plugin)
-- 2. Oracle 19c: Store embeddings as BLOB and use external search
-- 3. Alternative: Use PostgreSQL specifically for vector storage
-- 4. Alternative: Use cloud vector databases (Pinecone, Weaviate, etc.)
--
-- CURRENT IMPLEMENTATION:
-- This migration creates basic tables without vector operations.
-- Vector search will fall back to in-memory EmbeddingStore.
-- ============================================================================

-- SUPERSEDED 2026-08-24: replaced by an Application Dictionary-managed
-- AIG_Embedding table (iDempiereCLDE 202608241634_CLD-1601.sql), storing
-- Embedding as a plain large string column instead of BLOB, for
-- consistency with the PostgreSQL replacement (which had to drop pgvector
-- entirely - the extension isn't available on this deployment's Postgres
-- server). Left commented out rather than deleted so this BLOB-based
-- version is easy to restore if Oracle deployments need it back; a fresh
-- DB build must not run both CREATE TABLE AIG_Embedding statements.
--
-- -- =====================================================================
-- -- 1. Create embedding table (basic structure without vector type)
-- -- =====================================================================
--
-- CREATE TABLE AIG_Embedding (
--     -- iDempiere standard columns
--     AIG_Embedding_UU VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
--     AD_Client_ID NUMBER(10) DEFAULT 0 NOT NULL,
--     AD_Org_ID NUMBER(10) DEFAULT 0 NOT NULL,
--     IsActive CHAR(1) DEFAULT 'Y' NOT NULL CHECK (IsActive IN ('Y', 'N')),
--     Created DATE DEFAULT SYSDATE NOT NULL,
--     CreatedBy NUMBER(10) DEFAULT 100 NOT NULL,
--     Updated DATE DEFAULT SYSDATE NOT NULL,
--     UpdatedBy NUMBER(10) DEFAULT 100 NOT NULL,
--
--     -- Vector embedding data (stored as BLOB on Oracle)
--     -- On PostgreSQL this would be vector(768)
--     -- Oracle 23c+ could use VECTOR type
--     Embedding BLOB,
--     EmbeddingDimension NUMBER(10) DEFAULT 768,
--
--     -- Source content
--     TextSegment CLOB NOT NULL,
--     TextSegmentHash VARCHAR2(64),  -- SHA-256 for deduplication
--
--     -- Metadata (JSON stored as CLOB on Oracle 12c, JSON type on 21c+)
--     Metadata CLOB,
--
--     -- Source tracking for knowledge management
--     SourceType VARCHAR2(50) NOT NULL,
--     SourceID VARCHAR2(100),
--     SourceTable VARCHAR2(100),
--
--     -- Language support
--     AD_Language VARCHAR2(6),
--
--     -- Constraints
--     CONSTRAINT AIG_Embedding_Client_FK FOREIGN KEY (AD_Client_ID)
--         REFERENCES AD_Client(AD_Client_ID) ON DELETE CASCADE
-- );
--
-- -- =====================================================================
-- -- 2. Create supporting indexes (no vector index on Oracle <23c)
-- -- =====================================================================
--
-- CREATE INDEX AIG_Embedding_Client_Idx
--     ON AIG_Embedding (AD_Client_ID);
--
-- CREATE INDEX AIG_Embedding_SourceType_Idx
--     ON AIG_Embedding (SourceType);
--
-- CREATE INDEX AIG_Embedding_SourceID_Idx
--     ON AIG_Embedding (SourceType, SourceID);
--
-- CREATE INDEX AIG_Embedding_Hash_Idx
--     ON AIG_Embedding (TextSegmentHash);
--
-- CREATE INDEX AIG_Embedding_Language_Idx
--     ON AIG_Embedding (AD_Language);

-- =====================================================================
-- 3. Create ingestion tracking table
-- =====================================================================

CREATE TABLE AIG_IngestionMetadata (
    AIG_IngestionMetadata_UU VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    AD_Client_ID NUMBER(10) DEFAULT 0 NOT NULL,
    AD_Org_ID NUMBER(10) DEFAULT 0 NOT NULL,
    IsActive CHAR(1) DEFAULT 'Y' NOT NULL CHECK (IsActive IN ('Y', 'N')),
    Created DATE DEFAULT SYSDATE NOT NULL,
    CreatedBy NUMBER(10) DEFAULT 100 NOT NULL,
    Updated DATE DEFAULT SYSDATE NOT NULL,
    UpdatedBy NUMBER(10) DEFAULT 100 NOT NULL,

    -- Ingestor identification
    SourceType VARCHAR2(50) NOT NULL,

    -- Ingestion state
    LastIngestion DATE,
    LastSuccessfulIngestion DATE,
    RecordCount NUMBER(10) DEFAULT 0,
    Status VARCHAR2(20) DEFAULT 'pending' CHECK (Status IN ('pending', 'running', 'completed', 'failed')),
    ErrorMessage CLOB,
    DurationMs NUMBER,

    -- Unique constraint per client and source type
    CONSTRAINT AIG_IngestionMetadata_Unique UNIQUE (AD_Client_ID, SourceType),
    CONSTRAINT AIG_IngestionMetadata_Client_FK FOREIGN KEY (AD_Client_ID)
        REFERENCES AD_Client(AD_Client_ID) ON DELETE CASCADE
);

-- =====================================================================
-- 4. Create trigger for auto-updating timestamp
-- =====================================================================

-- SUPERSEDED 2026-08-24: AIG_Embedding above is commented out.
-- CREATE OR REPLACE TRIGGER AIG_Embedding_Updated_Trg
--     BEFORE UPDATE ON AIG_Embedding
--     FOR EACH ROW
-- BEGIN
--     :NEW.Updated := SYSDATE;
-- END;
-- /

CREATE OR REPLACE TRIGGER AIG_IngestionMetadata_Updated_Trg
    BEFORE UPDATE ON AIG_IngestionMetadata
    FOR EACH ROW
BEGIN
    :NEW.Updated := SYSDATE;
END;
/

-- =====================================================================
-- 5. Add comments for documentation
-- =====================================================================

-- SUPERSEDED 2026-08-24: AIG_Embedding above is commented out.
-- COMMENT ON TABLE AIG_Embedding IS 'AI embedding vectors for RAG-based context retrieval (ADR-012, ADR-026). Vector search requires Oracle 23c or PostgreSQL with pgvector.';
-- COMMENT ON COLUMN AIG_Embedding.Embedding IS 'Vector embedding stored as BLOB (768 dimensions for nomic-embed-text). Use PostgreSQL for native vector operations.';
-- COMMENT ON COLUMN AIG_Embedding.SourceType IS 'Knowledge source: ad_metadata, knowledge_entry, glossary, window_context';
-- COMMENT ON COLUMN AIG_Embedding.Metadata IS 'LangChain4j metadata stored as CLOB';

COMMENT ON TABLE AIG_IngestionMetadata IS 'Tracks knowledge ingestion state for incremental updates';
COMMENT ON COLUMN AIG_IngestionMetadata.SourceType IS 'Ingestor identifier matching IKnowledgeIngestor.getSourceType()';

-- =====================================================================
-- Note: Vector similarity search function not available on Oracle <23c
-- The Java code will automatically fall back to in-memory search
-- =====================================================================
