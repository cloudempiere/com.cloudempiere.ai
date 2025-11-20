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
package com.cloudempiere.ai.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.AccessSqlParser;
import org.compiere.model.MRole;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.model.MAIQueryAudit;

/**
 * Secure database query executor for AI agents
 *
 * <p><b>SECURITY MODEL</b>: AI agents execute queries using a designated
 * AI user account (from AIG_Provider.AD_User_ID) with the logged-in user's
 * role permissions. This provides:
 * <ul>
 *   <li>Clear AI agent identity for audit trails</li>
 *   <li>Role-based permissions inherited from the logged-in user</li>
 *   <li>Separation between AI agent and human user identities</li>
 * </ul>
 *
 * <p><b>KEY PRINCIPLE</b>: If the logged-in user's role cannot access
 * a table/column/record, the AI agent cannot access it either.
 *
 * <p><b>Execution Model</b>:
 * <ul>
 *   <li>Query executes with: AI User ID (from provider) + Logged-in User's Role ID</li>
 *   <li>Permissions checked against logged-in user's role</li>
 *   <li>Audit log records both AI user and initiating user</li>
 * </ul>
 *
 * <p>Security Features:
 * <ul>
 *   <li>Uses logged-in user's MRole for all permission checks</li>
 *   <li>Automatic SQL injection via MRole.addAccessSQL()</li>
 *   <li>Read-only enforcement (SELECT only)</li>
 *   <li>Row limits and query timeout</li>
 *   <li>Comprehensive audit logging with AI user and initiating user tracking</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 3.0
 */
public class SecureDatabaseQueryExecutor {

    private static final CLogger log = CLogger.getCLogger(SecureDatabaseQueryExecutor.class);

    /** Default maximum rows to return */
    private static final int DEFAULT_MAX_ROWS = 100;

    /** Default query timeout in milliseconds */
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    /** Sensitive column patterns to redact */
    private static final String[] SENSITIVE_PATTERNS = {
        "PASSWORD", "USERPIN", "CREDITCARD", "CVV", "CVC", "SSN",
        "TAXID", "BANKACCOUNT", "IBAN", "APIKEY", "TOKEN", "SECRET",
        "SALT", "LDAP", "PRIVATEKEY", "CERTIFICATE"
    };

    /**
     * Execute a secure database query using AI user with logged-in user's permissions
     *
     * <p><b>CRITICAL</b>: This method executes the query using:
     * <ul>
     *   <li>AI User ID from AIG_Provider.AD_User_ID (who executes the query)</li>
     *   <li>Logged-in User's Role ID from context (what permissions apply)</li>
     * </ul>
     *
     * <p>Security Flow:
     * <ol>
     *   <li>Load AI Provider and get designated AI User ID</li>
     *   <li>Validate AI user exists and is active</li>
     *   <li>Extract logged-in user's role from provided context (ctx)</li>
     *   <li>Validate SQL structure (read-only only)</li>
     *   <li>Get logged-in user's MRole (includes all inherited permissions)</li>
     *   <li>Validate table access using logged-in user's role</li>
     *   <li>Apply MRole.addAccessSQL() with AI user + logged-in role</li>
     *   <li>Apply row limits</li>
     *   <li>Execute query with timeout</li>
     *   <li>Redact sensitive columns</li>
     *   <li>Audit log: record AI user ID, logged-in user ID, and provider ID</li>
     * </ol>
     *
     * @param request Query request with user's context and AI provider info
     * @return Query result with data and metadata
     * @throws SecurityException if access is denied or AI user not configured
     */
    public SecureQueryResult executeQuery(SecureQueryRequest request) {

        long startTime = System.currentTimeMillis();
        SecureQueryResult result = new SecureQueryResult();

        // Variables for audit logging (need to be accessible in catch blocks)
        Properties ctx = request.getCtx();
        int loggedInUserId = Env.getAD_User_ID(ctx);
        int aiUserId = -1;

        try {
            // 1. Load AI Provider and get designated AI User
            MAIProvider provider = new MAIProvider(ctx, request.getProviderId(), null);
            if (provider.get_ID() == 0) {
                throw new SecurityException("AI Provider not found: " + request.getProviderId());
            }

            aiUserId = provider.getAD_User_ID();
            if (aiUserId <= 0) {
                throw new SecurityException("AI Provider does not have a designated AI User (AD_User_ID). " +
                    "Please configure an AI user account for this provider.");
            }

            // 2. Get logged-in user's information from context
            int loggedInRoleId = Env.getAD_Role_ID(ctx);

            if (loggedInUserId < 0 || loggedInRoleId < 0) {
                throw new SecurityException("Invalid user context - user not authenticated");
            }

            // 3. Get logged-in user's role (permissions to apply)
            // Use AI User ID but logged-in user's Role ID for permission checks
            MRole role = MRole.get(ctx, loggedInRoleId, aiUserId, false);
            result.setUserId(aiUserId);
            result.setRoleId(loggedInRoleId);
            result.setRoleName(role.getName());

            log.fine("AI Query - AI User: " + aiUserId + ", Logged-in User: " + loggedInUserId +
                    ", Role: " + loggedInRoleId + " (" + role.getName() + "), Provider: " + request.getProviderId());

            // 4. Validate SQL is read-only (no DML/DDL)
            validateReadOnlySQL(request.getSql());

            // 5. Extract table names from query
            List<String> tables = extractTableNames(request.getSql());
            result.setTablesAccessed(tables);

            // 6. Validate logged-in user's role has access to all tables in query
            for (String tableName : tables) {
                int tableId = getTableId(tableName);
                if (tableId <= 0) {
                    String error = "Table not found: " + tableName;
                    auditQuery(request, result, MAIQueryAudit.AIGQUERYSTATUS_Error, error,
                              System.currentTimeMillis() - startTime, loggedInUserId);
                    throw new IllegalArgumentException(error);
                }

                // Check if logged-in user's role allows table access (read-only)
                if (!role.isTableAccess(tableId, true)) { // true = read-only
                    String error = "Logged-in user's role does not have access to table: " + tableName;
                    auditQuery(request, result, MAIQueryAudit.AIGQUERYSTATUS_PermissionDenied, error,
                              System.currentTimeMillis() - startTime, loggedInUserId);
                    throw new SecurityException(error);
                }
            }

            // 7. Apply logged-in user's role-based security SQL injection
            // This adds WHERE clauses for client, org, table access, etc.
            // Uses AI user + logged-in user's role for MRole.addAccessSQL()
            String securedSQL = role.addAccessSQL(
                request.getSql(),
                tables.get(0), // Primary table
                true,          // Fully qualified
                true           // Read-write mode (more restrictive)
            );
            result.setSecuredSQL(securedSQL);

            // 8. Apply row limit
            int maxRows = request.getMaxRows() > 0 ?
                         request.getMaxRows() : DEFAULT_MAX_ROWS;
            securedSQL = applyRowLimit(securedSQL, maxRows);

            // 9. Execute query with timeout
            executeWithTimeout(ctx, securedSQL,
                             request.getTimeoutMs() > 0 ?
                             request.getTimeoutMs() : DEFAULT_TIMEOUT_MS,
                             result);

            // 10. Redact sensitive columns based on logged-in user's role column access
            redactSensitiveColumns(result, role);

            // 11. Audit log success
            result.setStatus(MAIQueryAudit.AIGQUERYSTATUS_Success);
            result.setTotalExecutionTimeMs(System.currentTimeMillis() - startTime);
            auditQuery(request, result, MAIQueryAudit.AIGQUERYSTATUS_Success, null,
                      result.getTotalExecutionTimeMs(), loggedInUserId);

        } catch (SecurityException e) {
            result.setStatus(MAIQueryAudit.AIGQUERYSTATUS_PermissionDenied);
            result.setErrorMessage(e.getMessage());
            result.setTotalExecutionTimeMs(System.currentTimeMillis() - startTime);
            log.log(Level.WARNING, "Security violation in AI query", e);
            auditQuery(request, result, MAIQueryAudit.AIGQUERYSTATUS_PermissionDenied, e.getMessage(),
                    result.getTotalExecutionTimeMs(), loggedInUserId);
            throw e;

        } catch (Exception e) {
            result.setStatus(MAIQueryAudit.AIGQUERYSTATUS_Error);
            result.setErrorMessage(e.getMessage());
            result.setTotalExecutionTimeMs(System.currentTimeMillis() - startTime);
            log.log(Level.SEVERE, "Error executing AI query", e);
            auditQuery(request, result, MAIQueryAudit.AIGQUERYSTATUS_Error, e.getMessage(),
                      result.getTotalExecutionTimeMs(), loggedInUserId);
            throw new RuntimeException("Query execution failed", e);
        }

        return result;
    }

    /**
     * Validate SQL is SELECT only (no INSERT, UPDATE, DELETE, DROP, etc.)
     */
    private void validateReadOnlySQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query is required");
        }

        String upperSQL = sql.trim().toUpperCase();

        // Must start with SELECT
        if (!upperSQL.startsWith("SELECT")) {
            throw new SecurityException("Only SELECT queries are allowed");
        }

        // Forbidden keywords
        String[] forbidden = {
            "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
            "TRUNCATE", "MERGE", "GRANT", "REVOKE", "EXECUTE",
            "CALL", "EXEC"
        };

        for (String keyword : forbidden) {
            if (upperSQL.contains(" " + keyword + " ") ||
                upperSQL.contains(" " + keyword + "(") ||
                upperSQL.endsWith(" " + keyword)) {
                throw new SecurityException("Forbidden SQL keyword: " + keyword);
            }
        }

        // No semicolons (prevent multiple statements)
        if (sql.contains(";")) {
            throw new SecurityException("Multiple SQL statements not allowed");
        }

        // No comments that could hide malicious code
        if (sql.contains("--") || sql.contains("/*")) {
            throw new SecurityException("SQL comments not allowed");
        }
    }

    /**
     * Extract table names from SQL query using iDempiere's parser
     */
    private List<String> extractTableNames(String sql) {
        List<String> tables = new ArrayList<>();

        try {
            // Use iDempiere's AccessSqlParser
            AccessSqlParser parser = new AccessSqlParser(sql);

            // Get the main SQL statement index
            int mainIndex = parser.getMainSqlIndex();
            if (mainIndex >= 0) {
                // Get table info for the main SQL statement
                AccessSqlParser.TableInfo[] tableInfo = parser.getTableInfo(mainIndex);
                if (tableInfo != null) {
                    for (AccessSqlParser.TableInfo info : tableInfo) {
                        String tableName = info.getTableName();
                        if (tableName != null && tableName.trim().length() > 0) {
                            // Remove any parentheses or function wrappers
                            tableName = tableName.trim();
                            if (tableName.startsWith("(")) {
                                tableName = tableName.substring(1);
                            }
                            if (tableName.endsWith(")")) {
                                tableName = tableName.substring(0, tableName.length() - 1);
                            }
                            tables.add(tableName.trim());
                        }
                    }
                }
            }

            if (tables.isEmpty()) {
                throw new IllegalArgumentException("No tables found in SQL query");
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Error parsing SQL for table names", e);
            throw new IllegalArgumentException("Invalid SQL structure: " + e.getMessage());
        }

        return tables;
    }

    /**
     * Get table ID from table name
     */
    private int getTableId(String tableName) {
        return DB.getSQLValue(null,
            "SELECT AD_Table_ID FROM AD_Table WHERE UPPER(TableName)=?",
            tableName.toUpperCase());
    }

    /**
     * Apply LIMIT/FETCH FIRST clause to SQL
     */
    private String applyRowLimit(String sql, int maxRows) {
        String upperSQL = sql.toUpperCase();

        // Check if LIMIT already exists
        if (upperSQL.contains("LIMIT") || upperSQL.contains("FETCH FIRST")) {
            return sql; // Don't modify existing limits
        }

        // Add database-specific limit clause
        if (DB.isPostgreSQL()) {
            return sql + " LIMIT " + maxRows;
        } else if (DB.isOracle()) {
            return sql + " FETCH FIRST " + maxRows + " ROWS ONLY";
        }

        return sql;
    }

    /**
     * Execute query with timeout
     */
    private void executeWithTimeout(
        Properties ctx,
        String sql,
        int timeoutMs,
        SecureQueryResult result
    ) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setQueryTimeout(timeoutMs / 1000); // Convert to seconds

            long queryStartTime = System.currentTimeMillis();
            rs = pstmt.executeQuery();
            long queryEndTime = System.currentTimeMillis();

            result.setQueryExecutionTimeMs(queryEndTime - queryStartTime);

            // Get column metadata
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            JSONArray columns = new JSONArray();
            JSONArray columnTypes = new JSONArray();

            for (int i = 1; i <= columnCount; i++) {
                columns.put(rsmd.getColumnName(i));
                columnTypes.put(rsmd.getColumnTypeName(i));
            }

            result.setColumns(columns);
            result.setColumnTypes(columnTypes);

            // Get rows
            JSONArray rows = new JSONArray();
            int rowCount = 0;

            while (rs.next()) {
                JSONObject row = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rsmd.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value != null ? value.toString() : null);
                }
                rows.put(row);
                rowCount++;
            }

            result.setRows(rows);
            result.setRowCount(rowCount);

        } catch (java.sql.SQLTimeoutException e) {
            throw new RuntimeException("Query timeout exceeded", e);
        } catch (Exception e) {
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        } finally {
            DB.close(rs, pstmt);
        }
    }

    /**
     * Redact sensitive columns based on role's column access and patterns
     */
    private void redactSensitiveColumns(SecureQueryResult result, MRole role) {
        JSONArray columns = result.getColumns();
        JSONArray rows = result.getRows();

        if (columns == null || rows == null) {
            return;
        }

        // Find columns to redact
        List<Integer> redactIndices = new ArrayList<>();
        for (int i = 0; i < columns.length(); i++) {
            String colName = columns.getString(i).toUpperCase();
            for (String pattern : SENSITIVE_PATTERNS) {
                if (colName.contains(pattern)) {
                    redactIndices.add(i);
                    log.fine("Redacting sensitive column: " + colName);
                    break;
                }
            }
        }

        // Redact sensitive values in all rows
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            for (int redactIdx : redactIndices) {
                String colName = columns.getString(redactIdx);
                if (row.has(colName)) {
                    row.put(colName, "***REDACTED***");
                }
            }
        }
    }

    /**
     * Audit log the query execution
     *
     * <p>Records:
     * <ul>
     *   <li>The AI user who executed the query (AD_User_ID from provider)</li>
     *   <li>The logged-in user who initiated the request (CreatedBy)</li>
     *   <li>The logged-in user's role used for permissions (AD_Role_ID)</li>
     *   <li>The AI provider (AIG_Provider_ID)</li>
     * </ul>
     */
    private void auditQuery(
        SecureQueryRequest request,
        SecureQueryResult result,
        String status,
        String errorMessage,
        long executionTimeMs,
        int loggedInUserId
    ) {
        try {
            Properties ctx = request.getCtx();
            int aiUserId = result.getUserId() > 0 ? result.getUserId() : loggedInUserId; // AI user from result - fallback to logged in user
            int roleId = result.getRoleId(); // Logged-in user's role from result

            // Create audit record using MAIQueryAudit model
            // CreatedBy will be the logged-in user, AD_User_ID will be the AI user
            MAIQueryAudit audit = new MAIQueryAudit(ctx, 0, null);

            // Set required fields
            audit.setAIG_Provider_ID(request.getProviderId());
            audit.setAD_User_ID(aiUserId); // AI user who executed the query
            audit.setAD_Role_ID(roleId); // Logged-in user's role
            audit.setAIGQuerySQL(request.getSql());
            audit.setAIGQueryStatus(status);

            // Set optional fields
            if (result.getSecuredSQL() != null) {
                audit.setAIGSecuredSQL(result.getSecuredSQL());
            }
            if (request.getQueryPurpose() != null) {
                audit.setAIGQueryPurpose(request.getQueryPurpose());
            }
            if (request.getContextType() != null) {
                audit.setAIGContextType(request.getContextType());
            }
            if (errorMessage != null) {
                audit.setAIGErrorMessage(errorMessage);
                audit.setAIGPermissionDeniedReason(errorMessage);
            }
            audit.setAIGRowCount(result.getRowCount());
            audit.setAIGExecutionTimeMs((int) executionTimeMs);
            if (result.getTablesAccessed() != null) {
                audit.setAIGTablesAccessed(String.join(", ", result.getTablesAccessed()));
            }

            // Save the audit record (CreatedBy will be set automatically to logged-in user from ctx)
            audit.saveEx();

            log.fine("AI Query Audit: " + status +
                    " | AI User: " + aiUserId +
                    " | Logged-in User: " + loggedInUserId +
                    " | Role: " + roleId +
                    " | Provider: " + request.getProviderId() +
                    " | Tables: " + result.getTablesAccessed() +
                    " | Rows: " + result.getRowCount() +
                    " | Time: " + executionTimeMs + "ms");

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to audit AI query", e);
            // Don't fail the main operation due to audit logging failure
        }
    }
}
