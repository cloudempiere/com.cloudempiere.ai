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
package com.cloudempiere.ai.agent.langchain4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.agent.AgentContext;
import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.tool.BoundaryValidator;
import com.cloudempiere.ai.tool.ToolExecutionException;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * iDempiere AI Tools for LangChain4j Agent
 *
 * <p>This class provides tool methods annotated with @Tool that LangChain4j
 * automatically discovers and makes available to the AI model. Each tool:
 * <ul>
 *   <li>Has a clear description for the AI to understand when to use it</li>
 *   <li>Validates boundaries (cost, rate limits) before execution</li>
 *   <li>Respects iDempiere security (org filtering, role access)</li>
 *   <li>Tracks execution for audit and cost management</li>
 * </ul>
 *
 * <p>The AgentContext must be set before tools are called via {@link #setContext(AgentContext)}.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class IAITools {

    private static final CLogger log = CLogger.getCLogger(IAITools.class);

    /** Default max rows for queries */
    private static final int DEFAULT_MAX_ROWS = 100;

    /** Default timeout in milliseconds */
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    /** Agent context - set per request */
    private AgentContext context;

    /** Boundary validator for enforcing limits */
    private final BoundaryValidator boundaryValidator;

    /** Secure query executor */
    private final SecureDatabaseQueryExecutor queryExecutor;

    /**
     * Create IAITools with default components
     */
    public IAITools() {
        this.boundaryValidator = new BoundaryValidator();
        this.queryExecutor = new SecureDatabaseQueryExecutor();
    }

    /**
     * Create IAITools with custom components
     *
     * @param boundaryValidator boundary validator to use
     * @param queryExecutor query executor to use
     */
    public IAITools(BoundaryValidator boundaryValidator, SecureDatabaseQueryExecutor queryExecutor) {
        this.boundaryValidator = boundaryValidator != null ? boundaryValidator : new BoundaryValidator();
        this.queryExecutor = queryExecutor != null ? queryExecutor : new SecureDatabaseQueryExecutor();
    }

    /**
     * Set the agent context for tool execution
     *
     * <p>MUST be called before any tool is invoked. The context provides:
     * <ul>
     *   <li>iDempiere Properties context</li>
     *   <li>User, role, client, org information</li>
     *   <li>Cost and rate limit tracking</li>
     * </ul>
     *
     * @param context agent context
     */
    public void setContext(AgentContext context) {
        this.context = context;
    }

    /**
     * Get the current agent context
     *
     * @return agent context or null if not set
     */
    public AgentContext getContext() {
        return context;
    }

    // ========================================================================
    // DATABASE QUERY TOOL
    // ========================================================================

    /**
     * Execute a read-only SQL query against the iDempiere database
     *
     * <p>This is the primary tool for data retrieval. The query is automatically
     * filtered by the user's organization access permissions.
     *
     * @param sql SQL SELECT query to execute
     * @param maxRows maximum rows to return (default 100, max 1000)
     * @param purpose description of why this query is being executed
     * @return JSON string with query results
     */
    @Tool("Execute a read-only SQL SELECT query against the iDempiere database. " +
          "The query is automatically filtered by organization and user permissions. " +
          "Only SELECT queries are allowed; INSERT, UPDATE, DELETE are blocked. " +
          "Sensitive columns (passwords, credit cards) are automatically redacted.")
    public String queryDatabase(
            @P("The SQL SELECT query to execute. Must start with SELECT. " +
               "Tables will be automatically filtered by user's organization access. " +
               "Example: SELECT Name, Value FROM M_Product WHERE IsActive='Y'")
            String sql,

            @P("Maximum number of rows to return. Default is 100. Maximum allowed is 1000.")
            Integer maxRows,

            @P("Description of why this query is being executed (for audit logging)")
            String purpose) {

        validateContext();
        validateBoundaries();

        try {
            int effectiveMaxRows = maxRows != null ? Math.min(maxRows, 1000) : DEFAULT_MAX_ROWS;

            log.fine("Executing database query: " + sql.substring(0, Math.min(100, sql.length())) + "...");

            // Build secure query request
            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(context.getCtx());
            request.setProviderId(context.getProviderId());
            request.setSql(sql);
            request.setMaxRows(effectiveMaxRows);
            request.setTimeoutMs(DEFAULT_TIMEOUT_MS);
            request.setQueryPurpose(purpose != null ? purpose : "AI Agent Query");
            request.setContextType("AGENT");

            // Execute query through secure executor
            SecureQueryResult result = queryExecutor.executeQuery(request);

            // Track execution
            trackExecution();

            // Build response
            return buildQueryResponse(result);

        } catch (SecurityException e) {
            log.warning("Security violation in query tool: " + e.getMessage());
            return errorResponse("Permission denied: " + e.getMessage());

        } catch (IllegalArgumentException e) {
            log.warning("Invalid query: " + e.getMessage());
            return errorResponse("Invalid query: " + e.getMessage());

        } catch (Exception e) {
            log.log(Level.SEVERE, "Query execution failed", e);
            return errorResponse("Query failed: " + e.getMessage());
        }
    }

    // ========================================================================
    // TABLE METADATA TOOL
    // ========================================================================

    /**
     * Get metadata about an iDempiere database table
     *
     * @param tableName exact table name (e.g., 'M_Product', 'C_Order')
     * @param searchKeyword search for tables containing this keyword
     * @param includeColumns whether to include column list
     * @return JSON string with table metadata
     */
    @Tool("Get metadata about an iDempiere database table. " +
          "Returns table description, key columns, column list, and related tables. " +
          "Use this to understand the data model and table relationships before querying.")
    public String getTableMetadata(
            @P("The exact name of the table (e.g., 'M_Product', 'C_Order'). " +
               "If not provided, use searchKeyword to find tables.")
            String tableName,

            @P("Search for tables containing this keyword in name or description. " +
               "For example: 'product', 'order', 'invoice'.")
            String searchKeyword,

            @P("Whether to include column list in the result. Default is true.")
            Boolean includeColumns) {

        validateContext();
        validateBoundaries();

        if ((tableName == null || tableName.trim().isEmpty()) &&
            (searchKeyword == null || searchKeyword.trim().isEmpty())) {
            return errorResponse("Either tableName or searchKeyword is required");
        }

        boolean incCols = includeColumns == null || includeColumns;

        try {
            JSONObject result = new JSONObject();

            if (tableName != null && !tableName.trim().isEmpty()) {
                JSONObject tableInfo = getTableInfo(tableName, incCols);
                if (tableInfo == null) {
                    return errorResponse("Table not found: " + tableName);
                }
                result.put("table", tableInfo);
            } else {
                JSONArray tables = searchTables(searchKeyword);
                result.put("search_keyword", searchKeyword);
                result.put("tables", tables);
                result.put("table_count", tables.length());
            }

            trackExecution();
            return result.toString();

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error getting table metadata", e);
            return errorResponse("Failed to get table metadata: " + e.getMessage());
        }
    }

    // ========================================================================
    // FIELD METADATA TOOL
    // ========================================================================

    /**
     * Get detailed metadata about a specific field/column
     *
     * @param tableName table name
     * @param columnName column name
     * @return JSON string with field metadata
     */
    @Tool("Get detailed metadata about a specific field/column in iDempiere. " +
          "Returns data type, display type, reference values, validation rules, and more. " +
          "Use this to understand field constraints before using values in queries.")
    public String getFieldMetadata(
            @P("The table name (e.g., 'M_Product')")
            String tableName,

            @P("The column name (e.g., 'M_Product_Category_ID')")
            String columnName) {

        validateContext();
        validateBoundaries();

        if (tableName == null || tableName.trim().isEmpty()) {
            return errorResponse("tableName is required");
        }
        if (columnName == null || columnName.trim().isEmpty()) {
            return errorResponse("columnName is required");
        }

        try {
            JSONObject result = getColumnInfo(tableName, columnName);
            if (result == null) {
                return errorResponse("Column not found: " + tableName + "." + columnName);
            }

            trackExecution();
            return result.toString();

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error getting field metadata", e);
            return errorResponse("Failed to get field metadata: " + e.getMessage());
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Validate that context is set and valid
     */
    private void validateContext() {
        if (context == null) {
            throw new IllegalStateException("AgentContext not set. Call setContext() before using tools.");
        }
        context.validate();
    }

    /**
     * Validate boundaries (cost and rate limits)
     */
    private void validateBoundaries() {
        try {
            boundaryValidator.validateCostLimits(context);
            boundaryValidator.validateRateLimits(context);
        } catch (ToolExecutionException e) {
            throw new RuntimeException("Boundary exceeded: " + e.getMessage(), e);
        }
    }

    /**
     * Track tool execution
     */
    private void trackExecution() {
        context.incrementToolCalls();
        boundaryValidator.trackExecution(context, 0.001); // Minimal cost estimate
    }

    /**
     * Build query response JSON
     */
    private String buildQueryResponse(SecureQueryResult result) {
        JSONObject response = new JSONObject();

        response.put("status", result.getStatus());
        response.put("row_count", result.getRowCount());
        response.put("execution_time_ms", result.getQueryExecutionTimeMs());

        if (result.getColumns() != null) {
            response.put("columns", result.getColumns());
        }

        if (result.getRows() != null) {
            response.put("data", result.getRows());
        }

        if (result.getErrorMessage() != null) {
            response.put("error", result.getErrorMessage());
        }

        if (result.getTablesAccessed() != null && !result.getTablesAccessed().isEmpty()) {
            response.put("tables_accessed", result.getTablesAccessed());
        }

        return response.toString();
    }

    /**
     * Build error response JSON
     */
    private String errorResponse(String message) {
        JSONObject response = new JSONObject();
        response.put("status", "ERROR");
        response.put("error", message);
        return response.toString();
    }

    /**
     * Get table info from AD_Table
     */
    private JSONObject getTableInfo(String tableName, boolean includeColumns) {
        JSONObject info = null;

        String sql = "SELECT " +
            "t.AD_Table_ID, t.TableName, t.Name, t.Description, t.Help, " +
            "t.IsView, t.IsSecurityEnabled, t.AccessLevel " +
            "FROM AD_Table t " +
            "WHERE UPPER(t.TableName) = ? AND t.IsActive = 'Y'";

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
                info.put("is_view", "Y".equals(rs.getString("IsView")));
                info.put("is_security_enabled", "Y".equals(rs.getString("IsSecurityEnabled")));
                info.put("access_level", getAccessLevelDescription(rs.getString("AccessLevel")));

                info.put("key_columns", getKeyColumns(tableId));
                info.put("related_tables", getRelatedTables(tableId));

                if (includeColumns) {
                    info.put("columns", getColumnList(tableId));
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error getting table info", e);
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

        String sql = "SELECT t.TableName, t.Name, t.Description, t.IsView " +
            "FROM AD_Table t " +
            "WHERE t.IsActive = 'Y' " +
            "AND (UPPER(t.TableName) LIKE ? OR UPPER(t.Name) LIKE ? OR UPPER(t.Description) LIKE ?) " +
            "ORDER BY t.TableName FETCH FIRST 20 ROWS ONLY";

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
            log.log(Level.WARNING, "Error searching tables", e);
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

        String sql = "SELECT ColumnName, Name FROM AD_Column " +
                    "WHERE AD_Table_ID = ? AND IsKey = 'Y' AND IsActive = 'Y' ORDER BY SeqNo";

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
            log.log(Level.WARNING, "Error getting key columns", e);
        } finally {
            DB.close(rs, pstmt);
        }

        return keys;
    }

    /**
     * Get related tables (foreign keys)
     */
    private JSONArray getRelatedTables(int tableId) {
        JSONArray related = new JSONArray();

        String sql = "SELECT DISTINCT c.ColumnName, t.TableName AS RelatedTable, t.Name AS RelatedTableName " +
            "FROM AD_Column c " +
            "JOIN AD_Table t ON c.AD_Reference_Value_ID = t.AD_Table_ID " +
            "WHERE c.AD_Table_ID = ? AND c.AD_Reference_ID IN (18, 19, 30) " +
            "AND c.IsActive = 'Y' AND t.IsActive = 'Y' ORDER BY c.ColumnName";

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
            log.log(Level.WARNING, "Error getting related tables", e);
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

        String sql = "SELECT c.ColumnName, c.Name, r.Name AS DataType, c.IsMandatory, c.IsKey " +
            "FROM AD_Column c " +
            "LEFT JOIN AD_Reference r ON c.AD_Reference_ID = r.AD_Reference_ID " +
            "WHERE c.AD_Table_ID = ? AND c.IsActive = 'Y' ORDER BY c.SeqNo, c.ColumnName";

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
            log.log(Level.WARNING, "Error getting column list", e);
        } finally {
            DB.close(rs, pstmt);
        }

        return columns;
    }

    /**
     * Get column info for a specific column
     */
    private JSONObject getColumnInfo(String tableName, String columnName) {
        JSONObject info = null;

        String sql = "SELECT c.AD_Column_ID, c.ColumnName, c.Name, c.Description, " +
            "c.ColumnSQL, c.FieldLength, c.IsMandatory, c.IsKey, c.IsIdentifier, " +
            "c.DefaultValue, r.Name AS DataType, c.AD_Reference_ID " +
            "FROM AD_Column c " +
            "JOIN AD_Table t ON c.AD_Table_ID = t.AD_Table_ID " +
            "LEFT JOIN AD_Reference r ON c.AD_Reference_ID = r.AD_Reference_ID " +
            "WHERE UPPER(t.TableName) = ? AND UPPER(c.ColumnName) = ? AND c.IsActive = 'Y'";

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setString(1, tableName.toUpperCase());
            pstmt.setString(2, columnName.toUpperCase());
            rs = pstmt.executeQuery();

            if (rs.next()) {
                info = new JSONObject();
                info.put("column_id", rs.getInt("AD_Column_ID"));
                info.put("column_name", rs.getString("ColumnName"));
                info.put("display_name", rs.getString("Name"));
                info.put("description", rs.getString("Description"));
                info.put("data_type", rs.getString("DataType"));
                info.put("reference_id", rs.getInt("AD_Reference_ID"));
                info.put("field_length", rs.getInt("FieldLength"));
                info.put("is_mandatory", "Y".equals(rs.getString("IsMandatory")));
                info.put("is_key", "Y".equals(rs.getString("IsKey")));
                info.put("is_identifier", "Y".equals(rs.getString("IsIdentifier")));
                info.put("default_value", rs.getString("DefaultValue"));
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error getting column info", e);
        } finally {
            DB.close(rs, pstmt);
        }

        return info;
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
}
