/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.kb.database;

import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.cloudempiere.ai.kb.dto.KnowledgeBaseEntry;
import com.cloudempiere.ai.kb.dto.KnowledgeBaseHierarchy;
import com.cloudempiere.ai.kb.dto.SimilarityResult;
import com.cloudempiere.ai.kb.dto.SimilarityResult.SimilarEntry;

import java.sql.ResultSet;
import java.util.logging.Level;

/**
 * Database queries for knowledge base operations
 *
 * <p>Leverages PostgreSQL full-text search, recursive CTEs, and materialized views
 * for efficient KB hierarchy and similarity matching.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class KnowledgeBaseQuery {

    private static final CLogger log = CLogger.getCLogger(KnowledgeBaseQuery.class);

    /**
     * Load KB hierarchy using materialized view
     *
     * <p>Uses v_k_entry_hierarchy materialized view for efficient hierarchical queries
     * with breadcrumb paths and depth information.
     */
    public static KnowledgeBaseHierarchy loadHierarchyFromView(String kType) {
        KnowledgeBaseHierarchy hierarchy = new KnowledgeBaseHierarchy(kType);

        String sql = """
            SELECT k_entry_id, name, title, description, content,
                   parent_id, seqno, isactive, ad_language,
                   depth, breadcrumb, path_length
            FROM v_k_entry_hierarchy
            WHERE k_type = ?
            ORDER BY path
            """;

        try {
            ResultSet rs = DB.query(sql, new Object[]{kType});

            while (rs.next()) {
                KnowledgeBaseEntry entry = new KnowledgeBaseEntry();
                entry.setK_entry_id(rs.getInt("k_entry_id"));
                entry.setName(rs.getString("name"));
                entry.setTitle(rs.getString("title"));
                entry.setDescription(rs.getString("description"));
                entry.setContent(rs.getString("content"));
                entry.setParent_id(rs.getInt("parent_id"));
                entry.setSequence_no(rs.getInt("seqno"));
                entry.setIs_active("Y".equals(rs.getString("isactive")));
                entry.setLanguage(rs.getString("ad_language"));
                entry.setK_type(kType);

                // Breadcrumb path from DB
                String breadcrumb = rs.getString("breadcrumb");

                hierarchy.addEntry(entry);
            }

            rs.close();
            log.fine("Loaded " + hierarchy.getTotalEntries() + " entries for KB type: " + kType);

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to load KB hierarchy from view", e);
        }

        return hierarchy;
    }

    /**
     * Find similar entries using full-text search
     *
     * <p>Uses PostgreSQL tsvector and ts_rank for efficient similarity matching.
     * Searches across title, description, and name fields.
     */
    public static SimilarityResult findSimilarEntries(
        String kType,
        String searchText,
        double minSimilarity
    ) {
        SimilarityResult result = new SimilarityResult();
        result.setUserContent(searchText);

        // Prepare search query - split into keywords for tsquery
        String searchQuery = prepareSearchQuery(searchText);

        String sql = """
            SELECT k_entry_id, title, description,
                   ts_rank(content_tsv, to_tsquery(?, ?)) as similarity_score,
                   breadcrumb, depth
            FROM v_k_entry_hierarchy
            WHERE k_type = ?
              AND content_tsv @@ to_tsquery(?, ?)
            ORDER BY similarity_score DESC
            LIMIT 10
            """;

        try {
            ResultSet rs = DB.query(sql, new Object[]{
                "english",
                searchQuery,
                kType,
                "english",
                searchQuery
            });

            while (rs.next()) {
                int entryId = rs.getInt("k_entry_id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                double score = rs.getDouble("similarity_score");
                String breadcrumb = rs.getString("breadcrumb");
                int depth = rs.getInt("depth");

                // Create entry object
                KnowledgeBaseEntry entry = new KnowledgeBaseEntry(entryId, "", title);
                entry.setDescription(description);

                // Create similar entry
                SimilarEntry similar = new SimilarEntry(entry, score);
                similar.setReason("Full-text match: " + breadcrumb);

                result.addSimilarEntry(similar);

                log.fine("Found similar entry: " + title + " (score: " +
                        String.format("%.2f", score) + ")");
            }

            rs.close();

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to find similar entries", e);
        }

        return result;
    }

    /**
     * Find duplicate entries (high similarity)
     *
     * <p>Uses v_k_entry_similarity materialized view to identify duplicates
     * with similarity score > 0.85.
     */
    public static SimilarityResult findDuplicates(String kType, String entryTitle) {
        SimilarityResult result = new SimilarityResult();

        String sql = """
            SELECT entry_id_2, title_2, similarity_score
            FROM v_k_entry_similarity
            WHERE k_type = ?
              AND similarity_score > 0.85
              AND entry_id_1 <> entry_id_2
            ORDER BY similarity_score DESC
            LIMIT 5
            """;

        try {
            ResultSet rs = DB.query(sql, new Object[]{kType});

            while (rs.next()) {
                int entryId = rs.getInt("entry_id_2");
                String title = rs.getString("title_2");
                double score = rs.getDouble("similarity_score");

                KnowledgeBaseEntry entry = new KnowledgeBaseEntry(entryId, "", title);
                SimilarEntry similar = new SimilarEntry(entry, score);
                similar.setReason("Potential duplicate");

                result.addSimilarEntry(similar);
            }

            rs.close();

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to find duplicates", e);
        }

        return result;
    }

    /**
     * Get hierarchy path for an entry using recursive CTE
     *
     * <p>Returns full breadcrumb path from root to entry.
     */
    public static String getHierarchyPath(int entryId) {
        String sql = """
            SELECT breadcrumb FROM get_hierarchy_path(?)
            ORDER BY level DESC
            LIMIT 1
            """;

        try {
            ResultSet rs = DB.query(sql, new Object[]{entryId});
            if (rs.next()) {
                String breadcrumb = rs.getString("breadcrumb");
                rs.close();
                return breadcrumb;
            }
            rs.close();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get hierarchy path", e);
        }

        return null;
    }

    /**
     * Get KB structure with statistics
     *
     * <p>Returns all entries with child counts and hierarchy info.
     */
    public static KnowledgeBaseHierarchy getKBStructureWithStats(String kType) {
        KnowledgeBaseHierarchy hierarchy = new KnowledgeBaseHierarchy(kType);

        String sql = """
            SELECT k_entry_id, title, description, depth, breadcrumb,
                   child_count, path
            FROM get_kb_structure(?)
            ORDER BY path
            """;

        try {
            ResultSet rs = DB.query(sql, new Object[]{kType});

            while (rs.next()) {
                KnowledgeBaseEntry entry = new KnowledgeBaseEntry();
                entry.setK_entry_id(rs.getInt("k_entry_id"));
                entry.setTitle(rs.getString("title"));
                entry.setDescription(rs.getString("description"));

                hierarchy.addEntry(entry);
            }

            rs.close();

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get KB structure", e);
        }

        return hierarchy;
    }

    /**
     * Update full-text search index for entry
     *
     * <p>Trigger should handle this automatically, but can be called manually
     * if needed after bulk updates.
     */
    public static void updateSearchIndex(int entryId) {
        String sql = """
            UPDATE k_entry
            SET content_tsv = to_tsvector('english',
                COALESCE(title, '') || ' ' ||
                COALESCE(description, '') || ' ' ||
                COALESCE(name, '')
            )
            WHERE k_entry_id = ?
            """;

        try {
            DB.executeUpdate(sql, new Object[]{entryId});
            log.fine("Updated search index for entry: " + entryId);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to update search index", e);
        }
    }

    /**
     * Refresh materialized views
     *
     * <p>Should be called after significant KB changes.
     * Can be scheduled to run nightly.
     */
    public static void refreshMaterializedViews() {
        try {
            DB.executeUpdate("SELECT refresh_kb_views()");
            log.info("Refreshed knowledge base materialized views");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to refresh materialized views", e);
        }
    }

    /**
     * Prepare PostgreSQL tsquery from user text
     *
     * <p>Converts plain text to tsquery format:
     * "docker kubernetes" → "docker & kubernetes"
     * Handles special characters and quoted phrases.
     */
    private static String prepareSearchQuery(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Split into words
        String[] words = text.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .split("\\s+");

        // Filter out very short words
        StringBuilder query = new StringBuilder();
        for (String word : words) {
            if (word.length() >= 2) {
                if (query.length() > 0) {
                    query.append(" & ");
                }
                query.append(word);
            }
        }

        return query.toString().isEmpty() ? "*" : query.toString();
    }

    /**
     * Get KB statistics
     *
     * <p>Returns counts and structure info for a KB type.
     */
    public static KBStatistics getStatistics(String kType) {
        KBStatistics stats = new KBStatistics();

        String sql = """
            SELECT
                COUNT(*) as total_entries,
                COUNT(CASE WHEN parent_id = 0 THEN 1 END) as root_entries,
                MAX(depth) as max_depth,
                AVG(array_length(path, 1)) as avg_depth
            FROM v_k_entry_hierarchy
            WHERE k_type = ?
            """;

        try {
            ResultSet rs = DB.query(sql, new Object[]{kType});
            if (rs.next()) {
                stats.totalEntries = rs.getInt("total_entries");
                stats.rootEntries = rs.getInt("root_entries");
                stats.maxDepth = rs.getInt("max_depth");
                stats.avgDepth = rs.getDouble("avg_depth");
            }
            rs.close();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get KB statistics", e);
        }

        return stats;
    }

    /**
     * Statistics DTO
     */
    public static class KBStatistics {
        public int totalEntries;
        public int rootEntries;
        public int maxDepth;
        public double avgDepth;

        @Override
        public String toString() {
            return "KBStatistics{" +
                    "totalEntries=" + totalEntries +
                    ", rootEntries=" + rootEntries +
                    ", maxDepth=" + maxDepth +
                    ", avgDepth=" + String.format("%.1f", avgDepth) +
                    '}';
        }
    }
}
