package com.cloudempiere.ai.provider.langchain4j;

import java.util.Properties;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * Streaming-aware ERP Tools wrapper for LangChain4j agents.
 *
 * <p>This class wraps {@link ERPTools} to provide callback notifications
 * when tools are executed. Since LangChain4j 0.35.0 TokenStream does not
 * have built-in tool execution callbacks (added in later versions),
 * this wrapper fires callbacks directly when methods are invoked.
 *
 * <p>Implements ADR-033: Streaming Responses and Thinking Timeline UX
 *
 * @author Cloudempiere
 * @version 1.0
 * @since ADR-033
 */
public class StreamingERPTools {

    private static final CLogger log = CLogger.getCLogger(StreamingERPTools.class);

    private final ERPTools delegate;
    private final AIStreamCallback callback;

    /**
     * Create streaming-aware ERP tools.
     *
     * @param provider AI Provider configuration
     * @param ctx iDempiere context
     * @param callback Streaming callback for tool events (can be null)
     */
    public StreamingERPTools(MAIProvider provider, Properties ctx, AIStreamCallback callback) {
        this.delegate = new ERPTools(provider, ctx);
        this.callback = callback;
    }

    // ========================================================================
    // Database Query Tools (with callbacks)
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
            String result = delegate.queryDatabase(sql, maxRows, purpose);
            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, truncate(result, 200) + " (" + elapsed + "ms)");
            return result;
        } catch (Exception e) {
            fireToolError(toolName, e.getMessage());
            throw e;
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
            String result = delegate.lookupRecord(tableName, recordId);
            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, truncate(result, 200) + " (" + elapsed + "ms)");
            return result;
        } catch (Exception e) {
            fireToolError(toolName, e.getMessage());
            throw e;
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
            String result = delegate.searchRecords(tableName, whereClause, maxRows);
            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, truncate(result, 200) + " (" + elapsed + "ms)");
            return result;
        } catch (Exception e) {
            fireToolError(toolName, e.getMessage());
            throw e;
        }
    }

    // ========================================================================
    // Metadata Tools (with callbacks)
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
            String result = delegate.getTableMetadata(tableName);
            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, "Metadata for " + tableName + " (" + elapsed + "ms)");
            return result;
        } catch (Exception e) {
            fireToolError(toolName, e.getMessage());
            throw e;
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
            String result = delegate.listTables(namePattern);
            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, "Listed tables (" + elapsed + "ms)");
            return result;
        } catch (Exception e) {
            fireToolError(toolName, e.getMessage());
            throw e;
        }
    }

    // ========================================================================
    // Business Object Tools (with callbacks)
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
            String result = delegate.getBusinessPartner(identifier);
            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, truncate(result, 200) + " (" + elapsed + "ms)");
            return result;
        } catch (Exception e) {
            fireToolError(toolName, e.getMessage());
            throw e;
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
            String result = delegate.getProduct(identifier);
            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, truncate(result, 200) + " (" + elapsed + "ms)");
            return result;
        } catch (Exception e) {
            fireToolError(toolName, e.getMessage());
            throw e;
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
            String result = delegate.getOrder(identifier);
            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, truncate(result, 200) + " (" + elapsed + "ms)");
            return result;
        } catch (Exception e) {
            fireToolError(toolName, e.getMessage());
            throw e;
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private void fireToolStart(String toolName, String args) {
        log.info("[TOOL] Starting: " + toolName);
        if (callback != null) {
            try {
                callback.onToolStart(toolName, args);
            } catch (Exception e) {
                log.warning("Error in onToolStart callback: " + e.getMessage());
            }
        }
    }

    private void fireToolComplete(String toolName, String result) {
        log.info("[TOOL] Completed: " + toolName);
        if (callback != null) {
            try {
                callback.onToolComplete(toolName, result);
            } catch (Exception e) {
                log.warning("Error in onToolComplete callback: " + e.getMessage());
            }
        }
    }

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

    private String truncate(String s, int maxLength) {
        if (s == null) return "";
        if (s.length() <= maxLength) return s;
        return s.substring(0, maxLength) + "...";
    }
}
