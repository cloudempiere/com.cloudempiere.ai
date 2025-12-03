-- ============================================================================
-- Knowledge Base Schema - PostgreSQL Optimization
-- ============================================================================
-- Full-text search, indexes, and materialized views for k_entry management
-- ============================================================================

-- 1. FULL-TEXT SEARCH INDEX
-- ============================================================================

-- Add tsvector column for full-text search (if not exists)
ALTER TABLE k_entry ADD COLUMN IF NOT EXISTS content_tsv tsvector;

-- Create function to automatically update tsvector
CREATE OR REPLACE FUNCTION k_entry_content_tsv() RETURNS trigger AS $$
BEGIN
    NEW.content_tsv := to_tsvector('english',
        COALESCE(NEW.title, '') || ' ' ||
        COALESCE(NEW.description, '') || ' ' ||
        COALESCE(NEW.name, '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop trigger if exists (to avoid conflicts)
DROP TRIGGER IF EXISTS k_entry_content_tsv_trigger ON k_entry;

-- Create trigger to update tsvector on insert/update
CREATE TRIGGER k_entry_content_tsv_trigger
BEFORE INSERT OR UPDATE ON k_entry
FOR EACH ROW
EXECUTE FUNCTION k_entry_content_tsv();

-- Update existing records with tsvector
UPDATE k_entry SET content_tsv = to_tsvector('english',
    COALESCE(title, '') || ' ' ||
    COALESCE(description, '') || ' ' ||
    COALESCE(name, '')
) WHERE content_tsv IS NULL;

-- Create GIN index for full-text search
CREATE INDEX IF NOT EXISTS idx_k_entry_content_tsv
    ON k_entry USING gin(content_tsv);

-- 2. HIERARCHY INDEXES
-- ============================================================================

-- Index for parent-child relationships
CREATE INDEX IF NOT EXISTS idx_k_entry_parent_id
    ON k_entry(parent_id) WHERE parent_id > 0;

-- Index for sequence ordering within parent
CREATE INDEX IF NOT EXISTS idx_k_entry_parent_seq
    ON k_entry(parent_id, seqno);

-- Index for KB type
CREATE INDEX IF NOT EXISTS idx_k_entry_k_type
    ON k_entry(k_type, isactive);

-- Composite index for hierarchy traversal
CREATE INDEX IF NOT EXISTS idx_k_entry_hierarchy
    ON k_entry(k_type, parent_id, seqno)
    WHERE isactive = 'Y';

-- 3. MATERIALIZED VIEW: KB Hierarchy with Levels
-- ============================================================================

DROP MATERIALIZED VIEW IF EXISTS v_k_entry_hierarchy CASCADE;

CREATE MATERIALIZED VIEW v_k_entry_hierarchy AS
WITH RECURSIVE hierarchy AS (
    -- Anchor: root entries
    SELECT
        k_entry_id,
        k_type,
        name,
        title,
        description,
        parent_id,
        seqno,
        isactive,
        content_tsv,
        1 as depth,
        ARRAY[k_entry_id] as path,
        title as breadcrumb,
        0 as total_children
    FROM k_entry
    WHERE parent_id = 0 AND isactive = 'Y'

    UNION ALL

    -- Recursive: child entries
    SELECT
        ke.k_entry_id,
        ke.k_type,
        ke.name,
        ke.title,
        ke.description,
        ke.parent_id,
        ke.seqno,
        ke.isactive,
        ke.content_tsv,
        h.depth + 1,
        h.path || ke.k_entry_id,
        h.breadcrumb || ' > ' || ke.title,
        0
    FROM k_entry ke
    INNER JOIN hierarchy h ON ke.parent_id = h.k_entry_id
    WHERE ke.isactive = 'Y' AND h.depth < 10 -- Prevent infinite recursion
)
SELECT
    k_entry_id,
    k_type,
    name,
    title,
    description,
    parent_id,
    seqno,
    depth,
    path,
    breadcrumb,
    array_length(path, 1) as path_length
FROM hierarchy
ORDER BY k_type, path;

-- Create indexes on materialized view
CREATE INDEX idx_v_k_entry_hierarchy_k_type
    ON v_k_entry_hierarchy(k_type);
CREATE INDEX idx_v_k_entry_hierarchy_depth
    ON v_k_entry_hierarchy(depth);
CREATE INDEX idx_v_k_entry_hierarchy_parent
    ON v_k_entry_hierarchy(parent_id);

-- 4. MATERIALIZED VIEW: Similarity Ranking
-- ============================================================================
-- Pre-computed similarity scores for common searches

DROP MATERIALIZED VIEW IF EXISTS v_k_entry_similarity CASCADE;

CREATE MATERIALIZED VIEW v_k_entry_similarity AS
SELECT
    a.k_entry_id as entry_id_1,
    b.k_entry_id as entry_id_2,
    a.k_type,
    -- Similarity score based on keyword overlap
    CASE
        WHEN a.k_entry_id = b.k_entry_id THEN 1.0
        ELSE (
            (
                CASE WHEN a.title % b.title THEN 0.4 ELSE 0 END +
                CASE WHEN a.description % b.description THEN 0.3 ELSE 0 END +
                (ts_rank(a.content_tsv, to_tsquery('english',
                    to_tsvector('english', COALESCE(b.title, '')))) * 0.3)
            ) / 1.0
        )
    END as similarity_score
FROM k_entry a
CROSS JOIN k_entry b
WHERE a.k_type = b.k_type
    AND a.isactive = 'Y'
    AND b.isactive = 'Y'
    AND a.k_entry_id <= b.k_entry_id;

CREATE INDEX idx_v_k_entry_similarity_entries
    ON v_k_entry_similarity(entry_id_1, entry_id_2);
CREATE INDEX idx_v_k_entry_similarity_score
    ON v_v_k_entry_similarity(similarity_score DESC);

-- 5. STORED PROCEDURES FOR COMMON OPERATIONS
-- ============================================================================

-- Procedure: Find similar entries
CREATE OR REPLACE FUNCTION find_similar_entries(
    p_k_type VARCHAR,
    p_search_text TEXT,
    p_min_similarity FLOAT DEFAULT 0.3,
    p_limit INT DEFAULT 10
)
RETURNS TABLE (
    k_entry_id INT,
    title VARCHAR,
    description TEXT,
    similarity_score FLOAT,
    breadcrumb TEXT,
    depth INT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        ke.k_entry_id,
        ke.title,
        ke.description,
        ts_rank(ke.content_tsv, to_tsquery('english', p_search_text)) as similarity_score,
        vh.breadcrumb,
        vh.depth
    FROM k_entry ke
    LEFT JOIN v_k_entry_hierarchy vh ON ke.k_entry_id = vh.k_entry_id
    WHERE ke.k_type = p_k_type
        AND ke.isactive = 'Y'
        AND ke.content_tsv @@ to_tsquery('english', p_search_text)
    ORDER BY similarity_score DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Procedure: Get hierarchy path
CREATE OR REPLACE FUNCTION get_hierarchy_path(
    p_k_entry_id INT
)
RETURNS TABLE (
    level INT,
    k_entry_id INT,
    title VARCHAR,
    breadcrumb TEXT
) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE path_up AS (
        -- Start from target entry
        SELECT
            1 as level,
            ke.k_entry_id,
            ke.title,
            ke.title as breadcrumb
        FROM k_entry ke
        WHERE ke.k_entry_id = p_k_entry_id

        UNION ALL

        -- Walk up hierarchy
        SELECT
            pu.level + 1,
            ke.k_entry_id,
            ke.title,
            ke.title || ' > ' || pu.breadcrumb
        FROM k_entry ke
        INNER JOIN path_up pu ON ke.k_entry_id = pu.parent_id
        WHERE pu.parent_id > 0
    )
    SELECT
        level,
        k_entry_id,
        title,
        breadcrumb
    FROM path_up
    ORDER BY level;
END;
$$ LANGUAGE plpgsql;

-- Procedure: Get KB structure with stats
CREATE OR REPLACE FUNCTION get_kb_structure(
    p_k_type VARCHAR
)
RETURNS TABLE (
    k_entry_id INT,
    title VARCHAR,
    depth INT,
    breadcrumb TEXT,
    child_count INT,
    path INT[]
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        vh.k_entry_id,
        vh.title,
        vh.depth,
        vh.breadcrumb,
        COUNT(DISTINCT ke.k_entry_id) as child_count,
        vh.path
    FROM v_k_entry_hierarchy vh
    LEFT JOIN k_entry ke ON ke.parent_id = vh.k_entry_id AND ke.isactive = 'Y'
    WHERE vh.k_type = p_k_type
    GROUP BY vh.k_entry_id, vh.title, vh.depth, vh.breadcrumb, vh.path
    ORDER BY vh.path;
END;
$$ LANGUAGE plpgsql;

-- Procedure: Detect duplicate entries
CREATE OR REPLACE FUNCTION find_duplicates(
    p_k_type VARCHAR,
    p_min_similarity FLOAT DEFAULT 0.85
)
RETURNS TABLE (
    entry_id_1 INT,
    entry_id_2 INT,
    title_1 VARCHAR,
    title_2 VARCHAR,
    similarity_score FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        vs.entry_id_1,
        vs.entry_id_2,
        ke1.title,
        ke2.title,
        vs.similarity_score
    FROM v_k_entry_similarity vs
    INNER JOIN k_entry ke1 ON vs.entry_id_1 = ke1.k_entry_id
    INNER JOIN k_entry ke2 ON vs.entry_id_2 = ke2.k_entry_id
    WHERE vs.k_type = p_k_type
        AND vs.similarity_score >= p_min_similarity
        AND vs.entry_id_1 <> vs.entry_id_2
    ORDER BY vs.similarity_score DESC;
END;
$$ LANGUAGE plpgsql;

-- 6. REFRESH MATERIALIZED VIEWS
-- ============================================================================

-- Create procedure to refresh all views
CREATE OR REPLACE FUNCTION refresh_kb_views()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY v_k_entry_hierarchy;
    REFRESH MATERIALIZED VIEW CONCURRENTLY v_k_entry_similarity;
END;
$$ LANGUAGE plpgsql;

-- Schedule refresh (PostgreSQL 10+)
-- Note: This requires pg_cron extension
-- CREATE EXTENSION IF NOT EXISTS pg_cron;
-- SELECT cron.schedule('refresh_kb_views', '0 2 * * *', 'SELECT refresh_kb_views()');

-- 7. GRANT PERMISSIONS
-- ============================================================================

-- Grant select on views to application user
-- GRANT SELECT ON v_k_entry_hierarchy TO idempiere_user;
-- GRANT SELECT ON v_k_entry_similarity TO idempiere_user;
-- GRANT EXECUTE ON FUNCTION find_similar_entries TO idempiere_user;
-- GRANT EXECUTE ON FUNCTION get_hierarchy_path TO idempiere_user;
-- GRANT EXECUTE ON FUNCTION get_kb_structure TO idempiere_user;
-- GRANT EXECUTE ON FUNCTION find_duplicates TO idempiere_user;
-- GRANT EXECUTE ON FUNCTION refresh_kb_views TO idempiere_user;

-- ============================================================================
-- USAGE EXAMPLES
-- ============================================================================

/*

-- Find similar entries for deployment guide
SELECT * FROM find_similar_entries('DEPLOYMENT', 'docker kubernetes', 0.3, 10);

-- Get path to specific entry
SELECT * FROM get_hierarchy_path(1234);

-- Get full KB structure with stats
SELECT * FROM get_kb_structure('DEPLOYMENT');

-- Find duplicate entries
SELECT * FROM find_duplicates('DEPLOYMENT', 0.85);

-- Refresh views
SELECT refresh_kb_views();

*/

-- ============================================================================
-- PERFORMANCE TIPS
-- ============================================================================

/*

1. Full-Text Search:
   - tsvector index is GIN (good for updates)
   - For large DBs, consider GIST index
   - Can customize stop words per language

2. Hierarchy Queries:
   - Materialized view prevents recursive query overhead
   - Refresh periodically (nightly recommended)
   - CONCURRENTLY keyword prevents locks

3. Similarity Matching:
   - Uses ts_rank for relevance scoring
   - Pre-computed view for performance
   - Consider partial indexes for active records only

4. Maintenance:
   - ANALYZE k_entry regularly
   - VACUUM ANALYZE after bulk operations
   - Monitor view refresh times

*/
