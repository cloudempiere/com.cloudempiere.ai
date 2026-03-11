package com.cloudempiere.ai.provider.langchain4j;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.util.CLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.database.ADSchemaCache;
import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.ADColumnMeta;
import com.cloudempiere.ai.database.dto.ADTableMeta;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * ERP Tools for LangChain4j agents using @Tool annotations.
 *
 * <p>These tools are automatically discovered by LangChain4j AiServices
 * and exposed to the AI model for function calling.
 *
 * <p>All database operations go through SecureDatabaseQueryExecutor
 * which enforces role-based access control.
 *
 * <p>Supports optional streaming callbacks for tool execution events.
 * When a callback is provided, tool start/complete/error events are fired.
 *
 * @author Cloudempiere
 * @version 0.19.0
 * @since ADR-002 LangChain4j Strategic Adoption
 */
public class ERPTools {

    private static final CLogger log = CLogger.getCLogger(ERPTools.class);

    private final SecureDatabaseQueryExecutor executor;
    private final MAIProvider provider;
    private final Properties ctx;

    /** Optional callback for streaming tool events */
    private final AIStreamCallback callback;

    /**
     * Create ERPTools with security context (no streaming callbacks).
     *
     * @param provider AI Provider configuration (for audit logging)
     * @param ctx iDempiere context (contains AD_Client_ID, AD_Org_ID, AD_Role_ID)
     */
    public ERPTools(MAIProvider provider, Properties ctx) {
        this(provider, ctx, null);
    }

    /**
     * Create ERPTools with security context and optional streaming callback.
     *
     * @param provider AI Provider configuration (for audit logging)
     * @param ctx iDempiere context (contains AD_Client_ID, AD_Org_ID, AD_Role_ID)
     * @param callback Optional callback for tool execution events (can be null)
     */
    public ERPTools(MAIProvider provider, Properties ctx, AIStreamCallback callback) {
        this.provider = provider;
        this.ctx = ctx;
        this.executor = new SecureDatabaseQueryExecutor();
        this.callback = callback;
    }

    // ========================================================================
    // Database Query Tools
    // ========================================================================

    @Tool("Execute a read-only SQL SELECT query against the iDempiere ERP database. " +
          "Results are filtered by user's role permissions. Returns JSON array of rows.")
    public String queryDatabase(
        @P("SQL SELECT query to execute") String sql,
        @P("Maximum number of rows to return (default 50, max 500)") Integer maxRows,
        @P("Purpose of the query for audit logging") String purpose
    ) {
        String toolName = "queryDatabase";
        String args = "{\"sql\": \"" + truncate(sql, 100) + "\", \"maxRows\": " + maxRows + "}";

        fireToolStart(toolName, args);
        long startTime = System.currentTimeMillis();

        try {
            // ADR-060: Validate table names against AD schema cache
            String tableValidationError = validateTablesInSql(sql);
            if (tableValidationError != null) {
                fireToolError(toolName, tableValidationError);
                return createErrorResponse(tableValidationError);
            }

            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql);
            request.setMaxRows(maxRows != null ? Math.min(maxRows, 500) : 50);
            request.setQueryPurpose(purpose != null ? purpose : "AI Agent Query");

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                String resultStr = result.getRows().toString();
                long elapsed = System.currentTimeMillis() - startTime;
                fireToolComplete(toolName, truncate(resultStr, 200) + " (" + elapsed + "ms)");
                return resultStr;
            } else {
                fireToolError(toolName, result.getErrorMessage());
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Query execution failed: " + e.getMessage());
            fireToolError(toolName, e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    @Tool("Look up a specific record by ID from an iDempiere table. " +
          "Returns all accessible columns for the record as JSON.")
    public String lookupRecord(
        @P("Table name (e.g., C_Order, C_BPartner, M_Product)") String tableName,
        @P("Record ID to look up") int recordId
    ) {
        String toolName = "lookupRecord";
        String args = "{\"tableName\": \"" + tableName + "\", \"recordId\": " + recordId + "}";

        fireToolStart(toolName, args);
        long startTime = System.currentTimeMillis();

        try {
            // ADR-060: Validate table name against AD schema cache
            String tableValidationError = validateTableName(tableName);
            if (tableValidationError != null) {
                fireToolError(toolName, tableValidationError);
                return createErrorResponse(tableValidationError);
            }

            // Build secure SELECT query
            String sql = String.format(
                "SELECT * FROM %s WHERE %s_ID = %d",
                tableName, tableName, recordId
            );

            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql);
            request.setMaxRows(1);
            request.setQueryPurpose("Record lookup: " + tableName + "#" + recordId);

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                String resultStr = result.getRows().toString();
                long elapsed = System.currentTimeMillis() - startTime;
                fireToolComplete(toolName, truncate(resultStr, 200) + " (" + elapsed + "ms)");
                return resultStr;
            } else {
                fireToolError(toolName, result.getErrorMessage());
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Record lookup failed: " + e.getMessage());
            fireToolError(toolName, e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    @Tool("Search for records in an iDempiere table using a WHERE clause. " +
          "Returns matching records as JSON array.")
    public String searchRecords(
        @P("Table name (e.g., C_Order, C_BPartner)") String tableName,
        @P("WHERE clause without 'WHERE' keyword (e.g., \"IsActive='Y' AND Name LIKE '%test%'\")") String whereClause,
        @P("Maximum rows to return (default 50)") Integer maxRows
    ) {
        String toolName = "searchRecords";
        String args = "{\"tableName\": \"" + tableName + "\", \"whereClause\": \"" + truncate(whereClause, 50) + "\"}";

        fireToolStart(toolName, args);
        long startTime = System.currentTimeMillis();

        try {
            // ADR-060: Validate table name against AD schema cache
            String tableValidationError = validateTableName(tableName);
            if (tableValidationError != null) {
                fireToolError(toolName, tableValidationError);
                return createErrorResponse(tableValidationError);
            }

            String sql = String.format("SELECT * FROM %s WHERE %s", tableName, whereClause);

            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql);
            request.setMaxRows(maxRows != null ? Math.min(maxRows, 500) : 50);
            request.setQueryPurpose("Search: " + tableName);

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                String resultStr = result.getRows().toString();
                long elapsed = System.currentTimeMillis() - startTime;
                fireToolComplete(toolName, truncate(resultStr, 200) + " (" + elapsed + "ms)");
                return resultStr;
            } else {
                fireToolError(toolName, result.getErrorMessage());
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Search failed: " + e.getMessage());
            fireToolError(toolName, e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    // ========================================================================
    // Metadata Tools
    // ========================================================================

    @Tool("Get metadata about an iDempiere database table including column names, types, and descriptions.")
    public String getTableMetadata(
        @P("Table name (e.g., C_Order, C_BPartner, M_Product)") String tableName
    ) {
        String toolName = "getTableMetadata";
        String args = "{\"tableName\": \"" + tableName + "\"}";

        fireToolStart(toolName, args);
        long startTime = System.currentTimeMillis();

        try {
            // ADR-060: Use schema cache for fast metadata lookup
            ADSchemaCache schemaCache = ADSchemaCache.get();
            ADTableMeta tableMeta = schemaCache.getTableMetadata(tableName);

            if (tableMeta == null) {
                // Table not in cache — provide suggestions
                List<String> suggestions = schemaCache.suggestTable(tableName);
                String msg = "Table not found: " + tableName;
                if (!suggestions.isEmpty()) {
                    msg += ". Did you mean: " + suggestions;
                }
                fireToolError(toolName, msg);
                return createErrorResponse(msg);
            }

            JSONObject metadata = new JSONObject();
            metadata.put("tableName", tableMeta.getTableName());
            metadata.put("name", tableMeta.getName());
            metadata.put("description", tableMeta.getDescription());
            metadata.put("isView", tableMeta.isView());
            metadata.put("accessLevel", tableMeta.getAccessLevelDescription());
            metadata.put("usageHint", tableMeta.getUsageHint());

            // Build columns array from cache
            JSONArray columnsArray = new JSONArray();
            for (ADColumnMeta col : tableMeta.getColumns()) {
                JSONObject colJson = new JSONObject();
                colJson.put("columnName", col.getColumnName());
                colJson.put("name", col.getName());
                colJson.put("description", col.getDescription());
                colJson.put("referenceType", col.getReferenceType());
                colJson.put("isMandatory", col.isMandatory());
                colJson.put("isKey", col.isKey());
                colJson.put("fieldLength", col.getFieldLength());
                String fkTable = col.getForeignTable();
                if (fkTable != null) {
                    colJson.put("foreignTable", fkTable);
                }
                columnsArray.put(colJson);
            }
            metadata.put("columns", columnsArray);

            // Add FK relationships summary
            List<ADColumnMeta> fkColumns = tableMeta.getForeignKeyColumns();
            if (!fkColumns.isEmpty()) {
                JSONArray fkArray = new JSONArray();
                for (ADColumnMeta fk : fkColumns) {
                    fkArray.put(fk.getColumnName() + " -> " + fk.getForeignTable());
                }
                metadata.put("foreignKeys", fkArray);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, "Metadata for " + tableName + " (" + elapsed + "ms)");
            return metadata.toString(2);
        } catch (Exception e) {
            log.severe("Metadata lookup failed: " + e.getMessage());
            fireToolError(toolName, e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    @Tool("List available iDempiere tables that the current user has access to.")
    public String listTables(
        @P("Filter by table name pattern (optional, e.g., 'C_%' for client tables)") String namePattern
    ) {
        String toolName = "listTables";
        String args = "{\"namePattern\": \"" + (namePattern != null ? namePattern : "") + "\"}";

        fireToolStart(toolName, args);
        long startTime = System.currentTimeMillis();

        try {
            // ADR-060: Use schema cache for fast table listing
            ADSchemaCache schemaCache = ADSchemaCache.get();
            JSONArray tablesArray = new JSONArray();

            for (ADTableMeta table : schemaCache.getAllTables()) {
                // Apply name pattern filter if provided
                if (namePattern != null && !namePattern.isEmpty()) {
                    // Convert SQL LIKE pattern to regex: % = .*, leave _ as literal
                    String regex = "(?i)" + namePattern.replace("%", ".*");
                    if (!table.getTableName().matches(regex)) {
                        continue;
                    }
                }

                JSONObject tableJson = new JSONObject();
                tableJson.put("tableName", table.getTableName());
                tableJson.put("name", table.getName());
                tableJson.put("description", table.getDescription());
                tableJson.put("isView", table.isView());
                tableJson.put("usageHint", table.getUsageHint());
                tableJson.put("columnCount", table.getColumns().size());
                tablesArray.put(tableJson);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, "Listed " + tablesArray.length() + " tables (" + elapsed + "ms)");
            return tablesArray.toString();
        } catch (Exception e) {
            log.severe("List tables failed: " + e.getMessage());
            fireToolError(toolName, e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    // ========================================================================
    // Business Object Tools
    // ========================================================================

    @Tool("Get details of a Business Partner (customer/vendor) by ID or search value.")
    public String getBusinessPartner(
        @P("Business Partner ID or Value (search key)") String identifier
    ) {
        String toolName = "getBusinessPartner";
        String args = "{\"identifier\": \"" + identifier + "\"}";

        fireToolStart(toolName, args);
        long startTime = System.currentTimeMillis();

        try {
            String whereClause;
            try {
                int id = Integer.parseInt(identifier);
                whereClause = "C_BPartner_ID = " + id;
            } catch (NumberFormatException e) {
                whereClause = "Value = '" + identifier.replace("'", "''") + "'";
            }

            String sql = "SELECT C_BPartner_ID, Value, Name, Name2, IsCustomer, IsVendor, " +
                        "IsEmployee, TotalOpenBalance, SO_CreditLimit, SO_CreditUsed " +
                        "FROM C_BPartner WHERE " + whereClause;

            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql);
            request.setMaxRows(1);
            request.setQueryPurpose("Business Partner lookup");

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                String resultStr = result.getRows().toString();
                long elapsed = System.currentTimeMillis() - startTime;
                fireToolComplete(toolName, truncate(resultStr, 200) + " (" + elapsed + "ms)");
                return resultStr;
            } else {
                fireToolError(toolName, result.getErrorMessage());
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Business Partner lookup failed: " + e.getMessage());
            fireToolError(toolName, e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    @Tool("Get details of a Product by ID or search value.")
    public String getProduct(
        @P("Product ID or Value (search key)") String identifier
    ) {
        String toolName = "getProduct";
        String args = "{\"identifier\": \"" + identifier + "\"}";

        fireToolStart(toolName, args);
        long startTime = System.currentTimeMillis();

        try {
            String whereClause;
            try {
                int id = Integer.parseInt(identifier);
                whereClause = "M_Product_ID = " + id;
            } catch (NumberFormatException e) {
                whereClause = "Value = '" + identifier.replace("'", "''") + "'";
            }

            String sql = "SELECT M_Product_ID, Value, Name, Description, ProductType, " +
                        "M_Product_Category_ID, C_UOM_ID, IsSold, IsPurchased, IsStocked " +
                        "FROM M_Product WHERE " + whereClause;

            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql);
            request.setMaxRows(1);
            request.setQueryPurpose("Product lookup");

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                String resultStr = result.getRows().toString();
                long elapsed = System.currentTimeMillis() - startTime;
                fireToolComplete(toolName, truncate(resultStr, 200) + " (" + elapsed + "ms)");
                return resultStr;
            } else {
                fireToolError(toolName, result.getErrorMessage());
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Product lookup failed: " + e.getMessage());
            fireToolError(toolName, e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    @Tool("Get details of an Order (Sales or Purchase) by DocumentNo or ID.")
    public String getOrder(
        @P("Order ID or DocumentNo") String identifier
    ) {
        String toolName = "getOrder";
        String args = "{\"identifier\": \"" + identifier + "\"}";

        fireToolStart(toolName, args);
        long startTime = System.currentTimeMillis();

        try {
            String whereClause;
            try {
                int id = Integer.parseInt(identifier);
                whereClause = "C_Order_ID = " + id;
            } catch (NumberFormatException e) {
                whereClause = "DocumentNo = '" + identifier.replace("'", "''") + "'";
            }

            String sql = "SELECT C_Order_ID, DocumentNo, C_BPartner_ID, DateOrdered, " +
                        "GrandTotal, DocStatus, IsSOTrx, C_DocType_ID " +
                        "FROM C_Order WHERE " + whereClause;

            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql);
            request.setMaxRows(1);
            request.setQueryPurpose("Order lookup");

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                String resultStr = result.getRows().toString();
                long elapsed = System.currentTimeMillis() - startTime;
                fireToolComplete(toolName, truncate(resultStr, 200) + " (" + elapsed + "ms)");
                return resultStr;
            } else {
                fireToolError(toolName, result.getErrorMessage());
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Order lookup failed: " + e.getMessage());
            fireToolError(toolName, e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    // ========================================================================
    // ADR-060: Schema Cache Validation Helpers
    // ========================================================================

    /** Pattern to extract table names from FROM and JOIN clauses */
    private static final Pattern TABLE_NAME_PATTERN =
        Pattern.compile("(?:FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_]*)", Pattern.CASE_INSENSITIVE);

    /**
     * Validate all table names referenced in a SQL query against the AD schema cache.
     * Extracts table names from FROM and JOIN clauses and checks each one.
     *
     * @param sql SQL query to validate
     * @return error message if any table is invalid, null if all tables are valid
     */
    private String validateTablesInSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "SQL query is empty";
        }

        ADSchemaCache schemaCache = ADSchemaCache.get();
        Set<String> tableNames = new LinkedHashSet<String>();

        Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        while (matcher.find()) {
            tableNames.add(matcher.group(1));
        }

        if (tableNames.isEmpty()) {
            return null; // No tables to validate (could be a function call etc.)
        }

        for (String tableName : tableNames) {
            if (!schemaCache.tableExists(tableName)) {
                List<String> suggestions = schemaCache.suggestTable(tableName);
                StringBuilder msg = new StringBuilder();
                msg.append("Table '").append(tableName).append("' does not exist in the Application Dictionary");
                if (!suggestions.isEmpty()) {
                    msg.append(". Did you mean: ").append(suggestions);
                }
                return msg.toString();
            }
        }

        return null; // All tables valid
    }

    /**
     * Validate a single table name against the AD schema cache.
     *
     * @param tableName table name to validate
     * @return error message if invalid, null if valid
     */
    private String validateTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return "Table name is required";
        }

        ADSchemaCache schemaCache = ADSchemaCache.get();
        if (!schemaCache.tableExists(tableName)) {
            List<String> suggestions = schemaCache.suggestTable(tableName);
            StringBuilder msg = new StringBuilder();
            msg.append("Table '").append(tableName).append("' does not exist in the Application Dictionary");
            if (!suggestions.isEmpty()) {
                msg.append(". Did you mean: ").append(suggestions);
            }
            return msg.toString();
        }

        return null; // Valid
    }

    private String createErrorResponse(String message) {
        JSONObject error = new JSONObject();
        error.put("error", true);
        error.put("message", message);
        return error.toString();
    }

    /**
     * Fire tool start callback if callback is configured.
     */
    private void fireToolStart(String toolName, String args) {
        log.log(Level.FINE, "[TOOL] Starting: " + toolName);
        if (callback != null) {
            try {
                callback.onToolStart(toolName, args);
            } catch (Exception e) {
                log.warning("Error in onToolStart callback: " + e.getMessage());
            }
        }
    }

    /**
     * Fire tool complete callback if callback is configured.
     */
    private void fireToolComplete(String toolName, String result) {
        log.log(Level.FINE, "[TOOL] Completed: " + toolName);
        if (callback != null) {
            try {
                callback.onToolComplete(toolName, result);
            } catch (Exception e) {
                log.warning("Error in onToolComplete callback: " + e.getMessage());
            }
        }
    }

    /**
     * Fire tool error callback if callback is configured.
     */
    private void fireToolError(String toolName, String error) {
        log.warning("[TOOL] Error in " + toolName + ": " + error);
        if (callback != null) {
            try {
                callback.onToolError(toolName, error);
            } catch (Exception e) {
                log.warning("Error in onToolError callback: " + e.getMessage());
            }
        }
    }

    /**
     * Truncate string for logging.
     */
    private String truncate(String s, int maxLength) {
        if (s == null) return "";
        if (s.length() <= maxLength) return s;
        return s.substring(0, maxLength) + "...";
    }
}
