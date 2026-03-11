package com.cloudempiere.ai.provider.langchain4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    /** Tracks consecutive queryDatabase failures to suppress intermediate errors from the UI */
    private int queryFailureCount = 0;

    /** After this many consecutive failures, terminate the agent with a user-facing message */
    private static final int MAX_QUERY_FAILURES = 6;

    /**
     * ADR-060 Option C: Tables registered via prepareQuery() for this conversation instance.
     * Soft-enforces that the LLM consults schema before writing SQL.
     */
    private final Set<String> preparedTables = new HashSet<>();

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
          "Results are filtered by user's role permissions. Returns JSON array of rows with schemaHints. " +
          "IMPORTANT: Call prepareQuery() for ALL tables BEFORE writing SQL to declare intent and receive exact column names. " +
          "ON conditions MUST be wrapped in parentheses: JOIN t ON (a.col = b.col). " +
          "schemaHints in the result show available columns for follow-up queries.")
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
            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql);
            request.setMaxRows(maxRows != null ? Math.min(maxRows, 500) : 50);
            request.setQueryPurpose(purpose != null ? purpose : "AI Agent Query");

            // ADR-060 Option C: Soft-enforce prepareQuery() was called for all tables in this SQL
            String unpreparedError = checkPreparedTables(sql);
            if (unpreparedError != null) {
                return handleQueryFailure(toolName, unpreparedError, sql);
            }

            // ADR-060: Validate table names against AD schema cache
            String tableValidationError = validateTablesInSql(sql);
            if (tableValidationError != null) {
                executor.auditRejectedQuery(request, tableValidationError,
                        System.currentTimeMillis() - startTime);
                return handleQueryFailure(toolName, tableValidationError, sql);
            }

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                queryFailureCount = 0; // reset on success
                long elapsed = System.currentTimeMillis() - startTime;
                JSONObject response = new JSONObject();
                response.put("rows", result.getRows());
                JSONObject hints = generateSchemaHints(sql);
                if (hints != null) {
                    response.put("schemaHints", hints);
                }
                String resultStr = response.toString();
                fireToolComplete(toolName, truncate(resultStr, 200) + " (" + elapsed + "ms)");
                return resultStr;
            } else {
                return handleQueryFailure(toolName, result.getErrorMessage(), sql);
            }
        } catch (RuntimeException e) {
            throw e; // terminal failure — let it propagate to stop the agent
        } catch (Exception e) {
            log.warning("Query execution failed: " + e.getMessage());
            return handleQueryFailure(toolName, e.getMessage(), sql);
        }
    }

    /**
     * Handles a queryDatabase failure. Below the threshold: suppresses the error from the UI
     * (logged only) and returns schema hints inline so the LLM can self-correct silently.
     * At the threshold: fires a single user-visible error and throws to terminate the agent.
     */
    private String handleQueryFailure(String toolName, String errorMsg, String sql) {
        queryFailureCount++;
        log.warning("[QUERY-FAIL " + queryFailureCount + "/" + MAX_QUERY_FAILURES + "] " + errorMsg);

        if (queryFailureCount >= MAX_QUERY_FAILURES) {
            String terminalMsg = "Unable to retrieve data after " + MAX_QUERY_FAILURES +
                " attempts. Please rephrase your question or contact support.";
            fireToolError(toolName, terminalMsg); // single visible error to the user
            throw new RuntimeException(terminalMsg); // stops the agent
        }

        // Transient failure — return error + schema hints to the LLM, but don't surface to UI
        return createErrorResponseWithSchema(errorMsg, sql);
    }

    /**
     * ADR-060 Option C: Declare tables the LLM intends to query.
     * Registers them in preparedTables and returns their exact schema so the LLM
     * can construct correct SQL without guessing column names.
     */
    @Tool("Declare the tables you intend to query. Call this BEFORE queryDatabase() for every table " +
          "you plan to use in your SQL. Returns exact column names, types, and FK relationships " +
          "so you can write correct SQL without guessing. " +
          "Example: prepareQuery([\"C_Order\", \"C_BPartner\"]) before querying orders with business partner data.")
    public String prepareQuery(
        @P("Names of tables you plan to use in the next SQL query") String[] tableNames
    ) {
        String toolName = "prepareQuery";
        String args = "{\"tableNames\": " + Arrays.toString(tableNames) + "}";
        fireToolStart(toolName, args);

        if (tableNames == null || tableNames.length == 0) {
            fireToolError(toolName, "No table names provided");
            return createErrorResponse("No table names provided. Pass the table names you intend to query.");
        }

        ADSchemaCache schemaCache = ADSchemaCache.get();
        JSONObject result = new JSONObject();
        JSONArray preparedArray = new JSONArray();
        JSONArray notFoundArray = new JSONArray();
        JSONObject schemas = new JSONObject();

        for (String tableName : tableNames) {
            if (tableName == null || tableName.isEmpty()) continue;

            ADTableMeta meta = schemaCache.getTableMetadata(tableName);
            if (meta == null) {
                List<String> suggestions = schemaCache.suggestTable(tableName);
                JSONObject notFound = new JSONObject();
                notFound.put("table", tableName);
                if (!suggestions.isEmpty()) {
                    notFound.put("didYouMean", suggestions.toString());
                }
                notFoundArray.put(notFound);
                continue;
            }

            // Register as prepared for this conversation
            preparedTables.add(tableName.toUpperCase());
            preparedArray.put(meta.getTableName());

            // Build compact schema: key + FK columns (most useful for JOINs), then others
            JSONObject schema = new JSONObject();
            JSONArray keyAndFk = new JSONArray();
            JSONArray other = new JSONArray();
            for (ADColumnMeta col : meta.getColumns()) {
                if (col.isKey() || col.getForeignTable() != null) {
                    String entry = col.isKey()
                        ? col.getColumnName() + " (PK)"
                        : col.getColumnName() + " -> " + col.getForeignTable();
                    keyAndFk.put(entry);
                } else {
                    other.put(col.getColumnName());
                }
            }
            schema.put("keyAndFkColumns", keyAndFk);
            schema.put("otherColumns", other);
            schemas.put(meta.getTableName(), schema);
        }

        result.put("prepared", preparedArray);
        if (notFoundArray.length() > 0) {
            result.put("notFound", notFoundArray);
        }
        result.put("schemas", schemas);
        result.put("hint", "Use exact column names above when writing SQL. " +
            "ON conditions must be wrapped in parentheses: JOIN table ON (a.col = b.col).");

        log.warning("[PREPARE-QUERY] Registered tables: " + preparedTables);
        fireToolComplete(toolName, "Prepared " + preparedArray.length() + " tables: " + preparedArray);
        return result.toString(2);
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
            // Build query for audit trail (before validation so it's recorded on failure too)
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

            // ADR-060: Validate table name against AD schema cache
            String tableValidationError = validateTableName(tableName);
            if (tableValidationError != null) {
                executor.auditRejectedQuery(request, tableValidationError,
                        System.currentTimeMillis() - startTime);
                fireToolError(toolName, tableValidationError);
                return createErrorResponse(tableValidationError);
            }

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
            // Build query for audit trail (before validation so it's recorded on failure too)
            String sql = String.format("SELECT * FROM %s WHERE %s", tableName, whereClause);

            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql);
            request.setMaxRows(maxRows != null ? Math.min(maxRows, 500) : 50);
            request.setQueryPurpose("Search: " + tableName);

            // ADR-060: Validate table name against AD schema cache
            String tableValidationError = validateTableName(tableName);
            if (tableValidationError != null) {
                executor.auditRejectedQuery(request, tableValidationError,
                        System.currentTimeMillis() - startTime);
                fireToolError(toolName, tableValidationError);
                return createErrorResponse(tableValidationError);
            }

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
     * Detects comma-separated table joins combined with ON clauses, e.g.
     * "FROM M_InOut s, C_BPartner bp ON s.C_BPartner_ID = bp.C_BPartner_ID"
     * which is invalid SQL that AccessSqlParser cannot parse.
     * Note: alias may be 1 char (s, t, p) so use [A-Za-z0-9_]* (zero or more after first char).
     */
    private static final Pattern COMMA_JOIN_WITH_ON =
        Pattern.compile(",\\s*[A-Za-z_][A-Za-z0-9_]*(\\s+[A-Za-z_][A-Za-z0-9_]*)?\\s+ON\\b",
                        Pattern.CASE_INSENSITIVE);

    /**
     * Detects ON conditions not wrapped in parentheses, e.g. "JOIN t ON a = b".
     * AccessSqlParser requires "JOIN t ON (a = b)" — it strips ON clauses by finding
     * the closing ')' after ON, so missing parentheses cause "Could not remove ON" errors.
     */
    private static final Pattern ON_WITHOUT_PARENS =
        Pattern.compile("\\bON\\s+(?!\\()", Pattern.CASE_INSENSITIVE);

    /**
     * Extracts table name and optional alias from FROM/JOIN clauses.
     * Handles: FROM Table, FROM Table alias, FROM Table AS alias, JOIN Table alias
     */
    private static final Pattern TABLE_ALIAS_PATTERN =
        Pattern.compile("(?:FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_]*)(?:\\s+(?:AS\\s+)?([A-Za-z_][A-Za-z0-9_]*))?",
                        Pattern.CASE_INSENSITIVE);

    /** Matches qualified column references like alias.column or table.column */
    private static final Pattern COLUMN_REF_PATTERN =
        Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)",
                        Pattern.CASE_INSENSITIVE);

    /**
     * Extract table names from FROM/JOIN clauses in a SQL query.
     */
    private Set<String> extractTableNames(String sql) {
        Set<String> tableNames = new LinkedHashSet<String>();
        if (sql == null) return tableNames;
        Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        while (matcher.find()) {
            tableNames.add(matcher.group(1));
        }
        return tableNames;
    }

    /**
     * ADR-060 Option C: Soft-enforce that prepareQuery() was called for all tables in the SQL.
     * If any table is unprepared, returns an error with schema hints nudging the LLM
     * to call prepareQuery() first. Degrades gracefully if LLM bypasses this contract —
     * validateTablesInSql() still runs as a fallback.
     *
     * @param sql SQL query to check
     * @return error message if any table was not prepared, null if all prepared (or no tables found)
     */
    private String checkPreparedTables(String sql) {
        Set<String> sqlTables = extractTableNames(sql);
        if (sqlTables.isEmpty()) return null; // no FROM clause — caught by validateTablesInSql

        List<String> unprepared = new ArrayList<String>();
        for (String table : sqlTables) {
            if (!preparedTables.contains(table.toUpperCase())) {
                unprepared.add(table);
            }
        }
        if (unprepared.isEmpty()) return null;

        return "Tables not prepared: " + unprepared + ". " +
            "Call prepareQuery(" + unprepared + ") first to receive exact column names " +
            "before writing SQL.";
    }

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

        log.warning("[SCHEMA-VALIDATE] SQL: " + truncate(sql, 300));

        // Detect comma-join + ON pattern: invalid SQL that AccessSqlParser cannot handle
        if (COMMA_JOIN_WITH_ON.matcher(sql).find()) {
            String msg = "Invalid JOIN syntax: do not use comma-separated tables with ON clauses. " +
                   "Use explicit JOIN...ON instead, e.g. " +
                   "FROM M_InOut s JOIN C_BPartner bp ON (s.C_BPartner_ID = bp.C_BPartner_ID)";
            log.warning("[SCHEMA-VALIDATE] REJECTED comma-join+ON: " + msg);
            return msg;
        }

        // AccessSqlParser requires ON conditions wrapped in parentheses: JOIN t ON (a = b)
        // It removes ON clauses by finding the closing ')' — without parens it fails with
        // "Could not remove ON" and security filtering is skipped.
        if (ON_WITHOUT_PARENS.matcher(sql).find()) {
            String msg = "ON conditions must be wrapped in parentheses for iDempiere security parsing. " +
                   "Use: JOIN table alias ON (alias.col = other.col), not JOIN table alias ON alias.col = other.col";
            log.warning("[SCHEMA-VALIDATE] REJECTED ON-without-parens: " + msg);
            return msg;
        }

        ADSchemaCache schemaCache = ADSchemaCache.get();
        Set<String> tableNames = new LinkedHashSet<String>();

        Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        while (matcher.find()) {
            tableNames.add(matcher.group(1));
        }

        log.warning("[SCHEMA-VALIDATE] Tables found in SQL: " + tableNames);

        if (tableNames.isEmpty()) {
            // No FROM clause — queries like SELECT CURRENT_DATE will crash SecureDatabaseQueryExecutor
            // which requires at least one table for role-based security filtering.
            return "Query has no FROM clause. Use getTableMetadata() to find the right table, " +
                   "then write a SELECT ... FROM <table> query.";
        }

        for (String tableName : tableNames) {
            if (!schemaCache.tableExists(tableName)) {
                List<String> suggestions = schemaCache.suggestTable(tableName);
                StringBuilder msg = new StringBuilder();
                msg.append("Table '").append(tableName).append("' does not exist in the Application Dictionary");
                if (!suggestions.isEmpty()) {
                    msg.append(". Did you mean: ").append(suggestions);
                }
                log.warning("[SCHEMA-VALIDATE] REJECTED unknown table: " + msg);
                return msg.toString();
            }
        }
        log.warning("[SCHEMA-VALIDATE] All tables OK: " + tableNames);

        // Build alias -> table map for column validation
        Map<String, String> aliasToTable = new java.util.HashMap<String, String>();
        Matcher aliasMatcher = TABLE_ALIAS_PATTERN.matcher(sql);
        while (aliasMatcher.find()) {
            String tblName = aliasMatcher.group(1);
            String alias = aliasMatcher.group(2);
            // Map both the table name itself and any alias to the table
            aliasToTable.put(tblName.toUpperCase(), tblName);
            if (alias != null && !alias.isEmpty()) {
                aliasToTable.put(alias.toUpperCase(), tblName);
            }
        }

        // Validate qualified column references (alias.column or table.column)
        Matcher colMatcher = COLUMN_REF_PATTERN.matcher(sql);
        while (colMatcher.find()) {
            String prefix = colMatcher.group(1);
            String column = colMatcher.group(2);
            String resolvedTable = aliasToTable.get(prefix.toUpperCase());
            if (resolvedTable == null) continue; // prefix is not a known table/alias — skip

            ADTableMeta tableMeta = schemaCache.getTableMetadata(resolvedTable);
            if (tableMeta == null) continue; // already caught above

            if (!tableMeta.hasColumn(column)) {
                StringBuilder msg = new StringBuilder();
                msg.append("Column '").append(column).append("' does not exist in table '")
                   .append(resolvedTable).append("'");
                List<String> colSuggestions = tableMeta.findSimilarColumns(column);
                if (!colSuggestions.isEmpty()) {
                    msg.append(". Did you mean: ").append(colSuggestions);
                }
                // Include available columns so the LLM can self-correct without an extra getTableMetadata call
                msg.append(". Available columns in ").append(resolvedTable).append(": ")
                   .append(getColumnSummary(tableMeta));
                log.warning("[SCHEMA-VALIDATE] REJECTED unknown column: " + msg);
                return msg.toString();
            }
        }

        return null; // All tables and columns valid
    }

    /**
     * Builds schema hints for tables referenced in a SQL query.
     * Injected into successful query results so the LLM has column context
     * for follow-up queries without an extra getTableMetadata call.
     * Mirrors QueryToolLogic.generateSchemaHints() from idempiere-hub.
     */
    private JSONObject generateSchemaHints(String sql) {
        ADSchemaCache schemaCache = ADSchemaCache.get();
        Set<String> tableNames = new LinkedHashSet<String>();
        Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        while (matcher.find()) {
            tableNames.add(matcher.group(1));
        }
        if (tableNames.isEmpty()) return null;

        JSONArray tablesArray = new JSONArray();
        for (String tableName : tableNames) {
            ADTableMeta meta = schemaCache.getTableMetadata(tableName);
            if (meta == null) continue;

            JSONObject tableInfo = new JSONObject();
            tableInfo.put("tableName", meta.getTableName());

            // Key + FK columns first (most useful for JOIN construction)
            JSONArray keyAndFk = new JSONArray();
            JSONArray other = new JSONArray();
            for (ADColumnMeta col : meta.getColumns()) {
                if (col.isKey() || col.getForeignTable() != null) {
                    keyAndFk.put(col.getColumnName());
                } else {
                    other.put(col.getColumnName());
                }
            }
            tableInfo.put("keyAndFkColumns", keyAndFk);
            tableInfo.put("otherColumns", other);
            tablesArray.put(tableInfo);
        }

        if (tablesArray.length() == 0) return null;

        JSONObject hints = new JSONObject();
        hints.put("tables", tablesArray);
        hints.put("hint", "Use exact column names above for follow-up queries.");
        return hints;
    }

    /**
     * Returns a compact column list for the table: column names grouped as
     * key, FKs, and other — enough for the LLM to rewrite a query without
     * a separate getTableMetadata call.
     */
    private String getColumnSummary(ADTableMeta tableMeta) {
        List<String> keyAndFk = new ArrayList<String>();
        List<String> other = new ArrayList<String>();
        for (ADColumnMeta col : tableMeta.getColumns()) {
            if (col.isKey() || col.getForeignTable() != null) {
                keyAndFk.add(col.getColumnName());
            } else {
                other.add(col.getColumnName());
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(keyAndFk); // key + FK columns first (most useful for JOINs)
        if (!other.isEmpty()) {
            sb.append(", other: ").append(other);
        }
        return sb.toString();
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
     * Error response that includes schema hints for tables mentioned in the failed SQL.
     * Lets the LLM self-correct in the next attempt without a separate getTableMetadata call.
     */
    private String createErrorResponseWithSchema(String message, String sql) {
        JSONObject error = new JSONObject();
        error.put("error", true);
        error.put("message", message);
        JSONObject hints = generateSchemaHints(sql);
        if (hints != null) {
            error.put("schemaHints", hints);
        }
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
