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
package com.cloudempiere.ai.database.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cached table metadata from Application Dictionary.
 * Mirrors idempiere-hub's TableMetadata for fast table/column validation
 * and fuzzy matching on misspelled names.
 *
 * @author Cloudempiere
 * @version ADR-060
 */
public class ADTableMeta {

    private final int adTableId;
    private final String tableName;
    private final String name;
    private final String description;
    private final String accessLevel;
    private final boolean view;

    /** Column map for case-insensitive O(1) lookup */
    private final Map<String, ADColumnMeta> columnMap;

    /** Ordered column list (by SeqNo) */
    private final List<ADColumnMeta> columns;

    public ADTableMeta(int adTableId, String tableName, String name,
                       String description, String accessLevel, boolean isView,
                       List<ADColumnMeta> columns) {
        this.adTableId = adTableId;
        this.tableName = tableName;
        this.name = name;
        this.description = description;
        this.accessLevel = accessLevel;
        this.view = isView;
        this.columns = columns != null ? Collections.unmodifiableList(new ArrayList<>(columns))
                                        : Collections.emptyList();

        this.columnMap = new HashMap<>();
        for (ADColumnMeta col : this.columns) {
            this.columnMap.put(col.getColumnName().toLowerCase(), col);
        }
    }

    // --- Getters ---

    public int getAdTableId() { return adTableId; }
    public String getTableName() { return tableName; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAccessLevel() { return accessLevel; }
    public boolean isView() { return view; }
    public List<ADColumnMeta> getColumns() { return columns; }

    // --- Column lookup ---

    /**
     * Check if column exists (case-insensitive).
     */
    public boolean hasColumn(String columnName) {
        if (columnName == null) return false;
        return columnMap.containsKey(columnName.toLowerCase());
    }

    /**
     * Get column metadata by name (case-insensitive).
     */
    public ADColumnMeta getColumn(String columnName) {
        if (columnName == null) return null;
        return columnMap.get(columnName.toLowerCase());
    }

    /**
     * Get FK columns that reference other tables.
     */
    public List<ADColumnMeta> getForeignKeyColumns() {
        List<ADColumnMeta> fks = new ArrayList<>();
        for (ADColumnMeta col : columns) {
            if (col.getForeignTable() != null) {
                fks.add(col);
            }
        }
        return fks;
    }

    // --- Fuzzy matching ---

    /**
     * Find similar column names using Levenshtein distance.
     * Returns up to 3 suggestions sorted by similarity.
     * Mirrors idempiere-hub's TableMetadata.findSimilarColumns().
     */
    public List<String> findSimilarColumns(String columnName) {
        if (columnName == null || columnMap.isEmpty()) {
            return Collections.emptyList();
        }

        String searchLower = columnName.toLowerCase();
        List<NameDistance> candidates = new ArrayList<>();

        for (ADColumnMeta col : columns) {
            int dist = levenshteinDistance(searchLower, col.getColumnName().toLowerCase());
            if (dist <= 5) {
                candidates.add(new NameDistance(col.getColumnName(), dist));
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

    // --- Access level description ---

    /**
     * Get human-readable access level description.
     */
    public String getAccessLevelDescription() {
        if (accessLevel == null) return "Unknown";
        switch (accessLevel) {
            case "1": return "Organization only";
            case "2": return "Client+Organization";
            case "3": return "Client only";
            case "4": return "System only";
            case "6": return "System+Client";
            case "7": return "All";
            default: return "Unknown (" + accessLevel + ")";
        }
    }

    /**
     * Get usage hint based on table prefix naming convention.
     */
    public String getUsageHint() {
        if (tableName.startsWith("C_")) return "Core business (Customer/Commerce)";
        if (tableName.startsWith("M_")) return "Material Management";
        if (tableName.startsWith("AD_")) return "Application Dictionary (system)";
        if (tableName.startsWith("GL_")) return "General Ledger (accounting)";
        if (tableName.startsWith("S_")) return "Service/Resource";
        if (tableName.startsWith("HR_")) return "Human Resources";
        if (tableName.startsWith("PP_")) return "Manufacturing/Production";
        if (tableName.startsWith("A_")) return "Asset Management";
        if (tableName.startsWith("R_")) return "Request/Support";
        if (tableName.startsWith("W_")) return "Web/eCommerce";
        if (tableName.startsWith("K_")) return "Knowledge Base";
        if (tableName.startsWith("CLD_") || tableName.startsWith("AIG_")) return "CloudEmpiere extension";
        return "Custom/extension";
    }

    // --- Levenshtein distance ---

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

    private static class NameDistance {
        final String name;
        final int distance;
        NameDistance(String name, int distance) {
            this.name = name;
            this.distance = distance;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ADTableMeta that = (ADTableMeta) o;
        return adTableId == that.adTableId && Objects.equals(tableName, that.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adTableId, tableName);
    }

    @Override
    public String toString() {
        return "ADTableMeta{" + tableName + " (" + name + "), " +
               columns.size() + " columns}";
    }
}
