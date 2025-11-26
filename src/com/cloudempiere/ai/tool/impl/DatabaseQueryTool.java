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

import java.util.LinkedHashMap;
import java.util.Map;

import org.compiere.util.CLogger;
import org.json.JSONObject;

import com.cloudempiere.ai.agent.AgentContext;
import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.tool.ITool;
import com.cloudempiere.ai.tool.ToolExecutionException;
import com.cloudempiere.ai.tool.ToolParameter;
import com.cloudempiere.ai.tool.ToolPermission;

/**
 * Tool for executing database queries through the AI agent
 *
 * <p>This tool wraps the existing {@link SecureDatabaseQueryExecutor} to provide
 * secure database access for AI agents. All security features of the underlying
 * executor are preserved:
 * <ul>
 *   <li>Read-only (SELECT only) queries</li>
 *   <li>Role-based table and column access</li>
 *   <li>Automatic org filtering via MRole.addAccessSQL()</li>
 *   <li>Sensitive column redaction</li>
 *   <li>Full audit logging</li>
 * </ul>
 *
 * <p>Example usage by AI:
 * <pre>
 * {
 *   "tool": "query_database",
 *   "parameters": {
 *     "sql": "SELECT Name, Value FROM M_Product WHERE IsActive='Y'",
 *     "max_rows": 50,
 *     "purpose": "Fetching active products for inventory analysis"
 *   }
 * }
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class DatabaseQueryTool implements ITool {

    private static final CLogger log = CLogger.getCLogger(DatabaseQueryTool.class);

    /** Tool name */
    public static final String TOOL_NAME = "query_database";

    /** Default max rows */
    private static final int DEFAULT_MAX_ROWS = 100;

    /** Default timeout in milliseconds */
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    /** The underlying secure query executor */
    private final SecureDatabaseQueryExecutor executor;

    /**
     * Create a DatabaseQueryTool with a new executor instance
     */
    public DatabaseQueryTool() {
        this.executor = new SecureDatabaseQueryExecutor();
    }

    /**
     * Create a DatabaseQueryTool with an existing executor
     *
     * @param executor secure database query executor to use
     */
    public DatabaseQueryTool(SecureDatabaseQueryExecutor executor) {
        this.executor = executor != null ? executor : new SecureDatabaseQueryExecutor();
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "Execute a read-only SQL SELECT query against the iDempiere database. " +
               "The query is automatically filtered by organization and user permissions. " +
               "Only SELECT queries are allowed; INSERT, UPDATE, DELETE are blocked. " +
               "Sensitive columns (passwords, credit cards, etc.) are automatically redacted.";
    }

    @Override
    public Map<String, ToolParameter> getParameters() {
        Map<String, ToolParameter> params = new LinkedHashMap<>();

        params.put("sql", ToolParameter.requiredString(
            "The SQL SELECT query to execute. Must start with SELECT. " +
            "Tables will be automatically filtered by user's organization access. " +
            "Example: SELECT Name, Value FROM M_Product WHERE IsActive='Y'"
        ));

        params.put("max_rows", ToolParameter.optionalInteger(
            "Maximum number of rows to return. Default is 100. Maximum allowed is 1000.",
            DEFAULT_MAX_ROWS
        ));

        params.put("purpose", ToolParameter.optionalString(
            "Description of why this query is being executed (for audit logging).",
            null
        ));

        return params;
    }

    @Override
    public String execute(AgentContext context, Map<String, Object> parameters)
            throws ToolExecutionException {

        // Validate parameters
        validateParameters(parameters);

        // Extract parameters
        String sql = (String) parameters.get("sql");
        int maxRows = getIntParameter(parameters, "max_rows", DEFAULT_MAX_ROWS);
        String purpose = (String) parameters.get("purpose");

        // Enforce max rows limit
        if (maxRows > 1000) {
            maxRows = 1000;
            log.fine("Max rows capped at 1000");
        }

        log.fine("Executing database query tool: " + sql.substring(0, Math.min(100, sql.length())) + "...");

        try {
            // Build secure query request
            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(context.getCtx());
            request.setProviderId(context.getProviderId());
            request.setSql(sql);
            request.setMaxRows(maxRows);
            request.setTimeoutMs(DEFAULT_TIMEOUT_MS);
            request.setQueryPurpose(purpose != null ? purpose : "AI Agent Query");
            request.setContextType("AGENT");

            // Execute query through secure executor
            SecureQueryResult result = executor.executeQuery(request);

            // Build response
            return buildResponse(result);

        } catch (SecurityException e) {
            log.warning("Security violation in query tool: " + e.getMessage());
            throw ToolExecutionException.permissionDenied(e.getMessage());

        } catch (IllegalArgumentException e) {
            log.warning("Invalid query: " + e.getMessage());
            throw ToolExecutionException.validationError(e.getMessage());

        } catch (Exception e) {
            log.severe("Query execution failed: " + e.getMessage());
            throw new ToolExecutionException("Query execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build response JSON from query result
     *
     * @param result query result
     * @return JSON string response
     */
    private String buildResponse(SecureQueryResult result) {
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
     * Get integer parameter with default value
     */
    private int getIntParameter(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public ToolPermission getRequiredPermission() {
        return ToolPermission.READ_DATA;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public int getEstimatedOutputTokens() {
        // Database queries can return significant data
        return 1000;
    }
}
