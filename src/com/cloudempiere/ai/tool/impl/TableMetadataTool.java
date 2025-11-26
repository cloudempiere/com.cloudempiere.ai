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
package com.cloudempiere.ai.tool.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.agent.AgentContext;
import com.cloudempiere.ai.tool.ITool;
import com.cloudempiere.ai.tool.ToolExecutionException;
import com.cloudempiere.ai.tool.ToolParameter;
import com.cloudempiere.ai.tool.ToolPermission;

/**
 * Tool for retrieving table metadata from Application Dictionary
 *
 * <p>Provides information about tables in iDempiere, including:
 * <ul>
 *   <li>Table name and description</li>
 *   <li>Key columns</li>
 *   <li>Related tables (foreign keys)</li>
 *   <li>Column list with types</li>
 * </ul>
 *
 * <p>This helps the AI understand the data model and relationships.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class TableMetadataTool implements ITool {

    private static final CLogger log = CLogger.getCLogger(TableMetadataTool.class);

    public static final String TOOL_NAME = "get_table_metadata";

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "Get metadata about an iDempiere database table. " +
               "Returns table description, key columns, column list, and related tables. " +
               "Use this to understand the data model and table relationships. " +
               "You can also search for tables by keyword.";
    }

    @Override
    public Map<String, ToolParameter> getParameters() {
        Map<String, ToolParameter> params = new LinkedHashMap<>();

        params.put("table_name", ToolParameter.optionalString(
            "The exact name of the table (e.g., 'M_Product', 'C_Order'). " +
            "If not provided, use search_keyword to find tables.",
            null
        ));

        params.put("search_keyword", ToolParameter.optionalString(
            "Search for tables containing this keyword in name or description. " +
            "For example: 'product', 'order', 'invoice'.",
            null
        ));

        params.put("include_columns", ToolParameter.optionalBoolean(
            "Whether to include column list in the result. Default is true.",
            true
        ));

        return params;
    }

    @Override
    public String execute(AgentContext context, Map<String, Object> parameters)
            throws ToolExecutionException {

        String tableName = (String) parameters.get("table_name");
        String searchKeyword = (String) parameters.get("search_keyword");
        boolean includeColumns = getBooleanParam(parameters, "include_columns", true);

        if (tableName == null && searchKeyword == null) {
            throw ToolExecutionException.validationError(
                "Either table_name or search_keyword is required"
            );
        }

        try {
            JSONObject result = new JSONObject();

            if (tableName != null && !tableName.trim().isEmpty()) {
                // Get specific table metadata
                JSONObject tableInfo = getTableInfo(tableName, includeColumns);
                if (tableInfo == null) {
                    throw ToolExecutionException.notFound("Table not found: " + tableName);
                }
                result.put("table", tableInfo);

            } else if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                // Search for tables
                JSONArray tables = searchTables(searchKeyword);
                result.put("search_keyword", searchKeyword);
                result.put("tables", tables);
                result.put("table_count", tables.length());
            }

            return result.toString();

        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.severe("Error getting table metadata: " + e.getMessage());
            throw new ToolExecutionException("Failed to get table metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Get detailed information about a specific table
     */
    private JSONObject getTableInfo(String tableName, boolean includeColumns) {
        JSONObject info = null;

        String sql = "SELECT " +
            "t.AD_Table_ID, " +
            "t.TableName, " +
            "t.Name, " +
            "t.Description, " +
            "t.Help, " +
            "t.IsView, " +
            "t.IsSecurityEnabled, " +
            "t.IsDeleteable, " +
            "t.IsHighVolume, " +
            "t.IsChangeLog, " +
            "t.AccessLevel " +
            "FROM AD_Table t " +
            "WHERE UPPER(t.TableName) = ? " +
            "AND t.IsActive = 'Y'";

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setString(1, tableName.toUpperCase());
            rs = pstmt.executeQuery();

            if (rs.next()) {
                info = new JSONObject();
                int tableId = rs.getInt("AD_Table_ID");

                info.put("table_id", tableId);
                info.put("table_name", rs.getString("TableName"));
                info.put("display_name", rs.getString("Name"));
                info.put("description", rs.getString("Description"));
                info.put("help", rs.getString("Help"));
                info.put("is_view", "Y".equals(rs.getString("IsView")));
                info.put("is_security_enabled", "Y".equals(rs.getString("IsSecurityEnabled")));
                info.put("is_deleteable", "Y".equals(rs.getString("IsDeleteable")));
                info.put("is_high_volume", "Y".equals(rs.getString("IsHighVolume")));
                info.put("is_change_log", "Y".equals(rs.getString("IsChangeLog")));
                info.put("access_level", getAccessLevelDescription(rs.getString("AccessLevel")));

                // Get key columns
                info.put("key_columns", getKeyColumns(tableId));

                // Get related tables (foreign keys)
                info.put("related_tables", getRelatedTables(tableId));

                // Get columns if requested
                if (includeColumns) {
                    info.put("columns", getColumnList(tableId));
                }
            }

        } catch (Exception e) {
            log.severe("Error getting table info: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        return info;
    }

    /**
     * Search for tables by keyword
     */
    private JSONArray searchTables(String keyword) {
        JSONArray tables = new JSONArray();

        String sql = "SELECT " +
            "t.AD_Table_ID, " +
            "t.TableName, " +
            "t.Name, " +
            "t.Description, " +
            "t.IsView " +
            "FROM AD_Table t " +
            "WHERE t.IsActive = 'Y' " +
            "AND (UPPER(t.TableName) LIKE ? " +
            "     OR UPPER(t.Name) LIKE ? " +
            "     OR UPPER(t.Description) LIKE ?) " +
            "ORDER BY t.TableName " +
            "FETCH FIRST 20 ROWS ONLY";

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            String searchPattern = "%" + keyword.toUpperCase() + "%";
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                JSONObject table = new JSONObject();
                table.put("table_name", rs.getString("TableName"));
                table.put("display_name", rs.getString("Name"));
                table.put("description", rs.getString("Description"));
                table.put("is_view", "Y".equals(rs.getString("IsView")));
                tables.put(table);
            }

        } catch (Exception e) {
            log.severe("Error searching tables: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        return tables;
    }

    /**
     * Get key columns for a table
     */
    private JSONArray getKeyColumns(int tableId) {
        JSONArray keys = new JSONArray();

        String sql = "SELECT ColumnName, Name " +
                    "FROM AD_Column " +
                    "WHERE AD_Table_ID = ? " +
                    "AND IsKey = 'Y' " +
                    "AND IsActive = 'Y' " +
                    "ORDER BY SeqNo";

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, tableId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                JSONObject key = new JSONObject();
                key.put("column_name", rs.getString("ColumnName"));
                key.put("display_name", rs.getString("Name"));
                keys.put(key);
            }

        } catch (Exception e) {
            log.warning("Error getting key columns: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        return keys;
    }

    /**
     * Get related tables (foreign key relationships)
     */
    private JSONArray getRelatedTables(int tableId) {
        JSONArray related = new JSONArray();

        String sql = "SELECT DISTINCT " +
            "c.ColumnName, " +
            "t.TableName AS RelatedTable, " +
            "t.Name AS RelatedTableName " +
            "FROM AD_Column c " +
            "JOIN AD_Table t ON c.AD_Reference_Value_ID = t.AD_Table_ID " +
            "WHERE c.AD_Table_ID = ? " +
            "AND c.AD_Reference_ID IN (18, 19, 30) " + // Table, TableDir, Search
            "AND c.IsActive = 'Y' " +
            "AND t.IsActive = 'Y' " +
            "ORDER BY c.ColumnName";

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, tableId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                JSONObject rel = new JSONObject();
                rel.put("column_name", rs.getString("ColumnName"));
                rel.put("related_table", rs.getString("RelatedTable"));
                rel.put("related_table_name", rs.getString("RelatedTableName"));
                related.put(rel);
            }

        } catch (Exception e) {
            log.warning("Error getting related tables: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        return related;
    }

    /**
     * Get simplified column list
     */
    private JSONArray getColumnList(int tableId) {
        JSONArray columns = new JSONArray();

        String sql = "SELECT " +
            "c.ColumnName, " +
            "c.Name, " +
            "r.Name AS DataType, " +
            "c.IsMandatory, " +
            "c.IsKey " +
            "FROM AD_Column c " +
            "LEFT JOIN AD_Reference r ON c.AD_Reference_ID = r.AD_Reference_ID " +
            "WHERE c.AD_Table_ID = ? " +
            "AND c.IsActive = 'Y' " +
            "ORDER BY c.SeqNo, c.ColumnName";

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, tableId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                JSONObject col = new JSONObject();
                col.put("column_name", rs.getString("ColumnName"));
                col.put("display_name", rs.getString("Name"));
                col.put("data_type", rs.getString("DataType"));
                col.put("is_mandatory", "Y".equals(rs.getString("IsMandatory")));
                col.put("is_key", "Y".equals(rs.getString("IsKey")));
                columns.put(col);
            }

        } catch (Exception e) {
            log.warning("Error getting column list: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        return columns;
    }

    /**
     * Get human-readable access level description
     */
    private String getAccessLevelDescription(String accessLevel) {
        if (accessLevel == null) return "Unknown";

        switch (accessLevel) {
            case "1": return "Organization";
            case "2": return "Client+Organization";
            case "3": return "Client only";
            case "4": return "System only";
            case "6": return "System+Client";
            case "7": return "All";
            default: return "Access Level " + accessLevel;
        }
    }

    /**
     * Get boolean parameter with default value
     */
    private boolean getBooleanParam(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    @Override
    public ToolPermission getRequiredPermission() {
        return ToolPermission.READ_METADATA;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public int getEstimatedOutputTokens() {
        return 800;
    }
}
