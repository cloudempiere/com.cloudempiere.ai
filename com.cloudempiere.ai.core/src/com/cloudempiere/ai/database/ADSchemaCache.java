/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.cloudempiere.ai.database.dto.ADColumnMeta;
import com.cloudempiere.ai.database.dto.ADTableMeta;

/**
 * In-memory cache of iDempiere Application Dictionary schema metadata.
 *
 * <p>Provides O(1) table/column existence checks, fuzzy name suggestions,
 * and FK relationship discovery without hitting the database on every
 * AI tool call.
 *
 * <p>Adapted from idempiere-hub's SchemaCache (Java 17/Quarkus) for
 * iDempiere's Java 11 / OSGi runtime. Uses a simple ConcurrentHashMap
 * instead of Quarkus CDI. Lazy-loaded on first access.
 *
 * <h3>Reference</h3>
 * <ul>
 *   <li>ADR-060: AD Metadata Schema Cache</li>
 *   <li>idempiere-hub ADR-008: Application Dictionary Registry</li>
 *   <li>idempiere-hub ADR-024: AD Metadata Caching</li>
 *   <li>idempiere-hub SchemaCache.java (Quarkus @ApplicationScoped)</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version ADR-060
 */
public class ADSchemaCache {

    private static final CLogger log = CLogger.getCLogger(ADSchemaCache.class);

    /** Singleton instance */
    private static final ADSchemaCache INSTANCE = new ADSchemaCache();

    /** Cache: tableName (UPPER_CASE) -> ADTableMeta */
    private final Map<String, ADTableMeta> cache = new ConcurrentHashMap<>();

    /** When cache was last loaded (millis since epoch) */
    private volatile long lastLoadedTime = 0;

    /** Cache TTL in minutes */
    private static final int CACHE_TTL_MINUTES = 30;

    private ADSchemaCache() {
    }

    /**
     * Get the singleton instance.
     */
    public static ADSchemaCache get() {
        return INSTANCE;
    }

    /** Number of tables currently in cache (0 if not yet loaded). */
    public int getCacheSize() {
        return cache.size();
    }

    /** Epoch millis when cache was last loaded (0 if never). */
    public long getLastLoadedTime() {
        return lastLoadedTime;
    }

    // ========================================================================
    // Lookup methods (O(1))
    // ========================================================================

    /**
     * Check if table exists in cache (case-insensitive).
     */
    public boolean tableExists(String tableName) {
        if (tableName == null) return false;
        ensureLoaded();
        return cache.containsKey(tableName.toUpperCase());
    }

    /**
     * Get table metadata from cache (case-insensitive).
     *
     * @return metadata or null if not found
     */
    public ADTableMeta getTableMetadata(String tableName) {
        if (tableName == null) return null;
        ensureLoaded();
        return cache.get(tableName.toUpperCase());
    }

    /**
     * Get all cached table metadata.
     */
    public Collection<ADTableMeta> getAllTables() {
        ensureLoaded();
        return Collections.unmodifiableCollection(cache.values());
    }

    /**
     * Get number of cached tables.
     */
    public int size() {
        return cache.size();
    }

    // ========================================================================
    // Fuzzy matching
    // ========================================================================

    /**
     * Suggest similar table names when the given name is not found.
     * Uses Levenshtein distance, returns up to 3 suggestions.
     */
    public List<String> suggestTable(String tableName) {
        if (tableName == null || cache.isEmpty()) {
            return Collections.emptyList();
        }

        ensureLoaded();
        String searchUpper = tableName.toUpperCase();

        // Proportional threshold: at most 2 edits for short names, 4 for longer ones
        int threshold = Math.max(2, Math.min(4, searchUpper.length() / 3));
        List<NameDistance> candidates = new ArrayList<>();
        for (ADTableMeta meta : cache.values()) {
            int dist = levenshteinDistance(searchUpper, meta.getTableName().toUpperCase());
            if (dist <= threshold) {
                candidates.add(new NameDistance(meta.getTableName(), dist));
            }
        }

        Collections.sort(candidates, new Comparator<NameDistance>() {
            @Override
            public int compare(NameDistance a, NameDistance b) {
                return Integer.compare(a.distance, b.distance);
            }
        });

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(3, candidates.size()); i++) {
            result.add(candidates.get(i).name);
        }
        return result;
    }

    // ========================================================================
    // Cache loading
    // ========================================================================

    /**
     * Ensure cache is loaded and not expired.
     * Thread-safe: synchronized so only one thread loads.
     */
    public synchronized void ensureLoaded() {
        long now = System.currentTimeMillis();
        long ttlMillis = CACHE_TTL_MINUTES * 60 * 1000L;

        boolean needsLoad = cache.isEmpty()
            || (lastLoadedTime == 0)
            || ((now - lastLoadedTime) > ttlMillis);

        if (needsLoad) {
            loadSchema();
        }
    }

    /**
     * Force refresh the cache from database.
     */
    public synchronized void refresh() {
        cache.clear();
        lastLoadedTime = 0;
        loadSchema();
        log.warning("ADSchemaCache refreshed: " + cache.size() + " tables");
    }

    /**
     * Clear cache (e.g., on CacheMgt reset).
     */
    public synchronized void reset() {
        cache.clear();
        lastLoadedTime = 0;
    }

    /**
     * Load all active tables with their columns from AD_Table and AD_Column.
     */
    private void loadSchema() {
        long start = System.currentTimeMillis();

        // Step 1: Load tables
        Map<Integer, TableBuilder> builders = loadTables();
        if (builders.isEmpty()) {
            log.warning("No tables loaded from AD_Table");
            return;
        }

        // Step 2: Load columns for all tables
        loadColumns(builders);

        // Step 3: Build cache
        cache.clear();
        for (TableBuilder builder : builders.values()) {
            ADTableMeta meta = builder.build();
            cache.put(meta.getTableName().toUpperCase(), meta);
        }

        lastLoadedTime = System.currentTimeMillis();
        long elapsed = lastLoadedTime - start;

        int totalColumns = 0;
        for (ADTableMeta m : cache.values()) {
            totalColumns += m.getColumns().size();
        }

        log.warning("ADSchemaCache loaded: " + cache.size() + " tables, "
                + totalColumns + " columns in " + elapsed + "ms");
    }

    private Map<Integer, TableBuilder> loadTables() {
        String sql = "SELECT t.AD_Table_ID, t.TableName, t.Name, t.Description, " +
                     "t.AccessLevel, t.IsView " +
                     "FROM AD_Table t " +
                     "WHERE t.IsActive = 'Y' " +
                     "ORDER BY t.TableName";

        Map<Integer, TableBuilder> builders = new HashMap<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                int tableId = rs.getInt("AD_Table_ID");
                builders.put(tableId, new TableBuilder(
                    tableId,
                    rs.getString("TableName"),
                    rs.getString("Name"),
                    rs.getString("Description"),
                    rs.getString("AccessLevel"),
                    "Y".equals(rs.getString("IsView"))
                ));
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to load tables for schema cache", e);
        } finally {
            DB.close(rs, pstmt);
        }
        return builders;
    }

    private void loadColumns(Map<Integer, TableBuilder> builders) {
        String sql = "SELECT c.AD_Table_ID, c.ColumnName, " +
                     "COALESCE(e.Name, c.Name) as Name, " +
                     "COALESCE(e.Description, c.Description) as Description, " +
                     "c.AD_Reference_ID, r.Name as ReferenceType, " +
                     "c.IsMandatory, c.IsKey, c.FieldLength " +
                     "FROM AD_Column c " +
                     "LEFT JOIN AD_Element e ON c.AD_Element_ID = e.AD_Element_ID " +
                     "LEFT JOIN AD_Reference r ON c.AD_Reference_ID = r.AD_Reference_ID " +
                     "WHERE c.IsActive = 'Y' " +
                     "ORDER BY c.AD_Table_ID, c.SeqNo";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int count = 0;
        try {
            pstmt = DB.prepareStatement(sql, null);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                int tableId = rs.getInt("AD_Table_ID");
                TableBuilder builder = builders.get(tableId);
                if (builder != null) {
                    builder.addColumn(new ADColumnMeta(
                        rs.getString("ColumnName"),
                        rs.getString("Name"),
                        rs.getString("Description"),
                        rs.getInt("AD_Reference_ID"),
                        rs.getString("ReferenceType"),
                        "Y".equals(rs.getString("IsMandatory")),
                        "Y".equals(rs.getString("IsKey")),
                        rs.getInt("FieldLength")
                    ));
                    count++;
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to load columns for schema cache", e);
        } finally {
            DB.close(rs, pstmt);
        }

        if (log.isLoggable(Level.FINE)) {
            log.warning("Loaded " + count + " columns for " + builders.size() + " tables");
        }
    }

    // ========================================================================
    // Helper classes
    // ========================================================================

    /**
     * Builder for constructing ADTableMeta during loading.
     */
    private static class TableBuilder {
        final int tableId;
        final String tableName;
        final String name;
        final String description;
        final String accessLevel;
        final boolean isView;
        final List<ADColumnMeta> columns = new ArrayList<>();

        TableBuilder(int tableId, String tableName, String name,
                     String description, String accessLevel, boolean isView) {
            this.tableId = tableId;
            this.tableName = tableName;
            this.name = name;
            this.description = description;
            this.accessLevel = accessLevel;
            this.isView = isView;
        }

        void addColumn(ADColumnMeta column) {
            columns.add(column);
        }

        ADTableMeta build() {
            return new ADTableMeta(tableId, tableName, name, description,
                                   accessLevel, isView, columns);
        }
    }

    private static class NameDistance {
        final String name;
        final int distance;
        NameDistance(String name, int distance) {
            this.name = name;
            this.distance = distance;
        }
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[s1.length()][s2.length()];
    }
}
