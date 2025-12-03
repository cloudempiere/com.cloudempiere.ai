package com.cloudempiere.ai.provider.langchain4j;

import java.util.Properties;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.model.MAIProvider;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * ERP Tools for LangChain4j agents using @Tool annotations.
 *
 * These tools are automatically discovered by LangChain4j AiServices
 * and exposed to the AI model for function calling.
 *
 * All database operations go through SecureDatabaseQueryExecutor
 * which enforces role-based access control.
 *
 * @author Cloudempiere
 * @version 0.9.0
 * @since ADR-002 LangChain4j Strategic Adoption
 */
public class ERPTools {

    private static final CLogger log = CLogger.getCLogger(ERPTools.class);

    private final SecureDatabaseQueryExecutor executor;
    private final MAIProvider provider;
    private final Properties ctx;

    /**
     * Create ERPTools with security context.
     *
     * @param provider AI Provider configuration (for audit logging)
     * @param ctx iDempiere context (contains AD_Client_ID, AD_Org_ID, AD_Role_ID)
     */
    public ERPTools(MAIProvider provider, Properties ctx) {
        this.provider = provider;
        this.ctx = ctx;
        this.executor = new SecureDatabaseQueryExecutor();
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
        log.info("ERPTools.queryDatabase: " + sql);

        try {
            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql);
            request.setMaxRows(maxRows != null ? Math.min(maxRows, 500) : 50);
            request.setQueryPurpose(purpose != null ? purpose : "AI Agent Query");

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                return result.getRows().toString();
            } else {
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Query execution failed: " + e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    @Tool("Look up a specific record by ID from an iDempiere table. " +
          "Returns all accessible columns for the record as JSON.")
    public String lookupRecord(
        @P("Table name (e.g., C_Order, C_BPartner, M_Product)") String tableName,
        @P("Record ID to look up") int recordId
    ) {
        log.info("ERPTools.lookupRecord: " + tableName + " ID=" + recordId);

        try {
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
                return result.getRows().toString();
            } else {
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Record lookup failed: " + e.getMessage());
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
        log.info("ERPTools.searchRecords: " + tableName + " WHERE " + whereClause);

        try {
            String sql = String.format("SELECT * FROM %s WHERE %s", tableName, whereClause);

            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql);
            request.setMaxRows(maxRows != null ? Math.min(maxRows, 500) : 50);
            request.setQueryPurpose("Search: " + tableName);

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                return result.getRows().toString();
            } else {
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Search failed: " + e.getMessage());
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
        log.info("ERPTools.getTableMetadata: " + tableName);

        try {
            MTable table = MTable.get(ctx, tableName);
            if (table == null || table.getAD_Table_ID() == 0) {
                return createErrorResponse("Table not found: " + tableName);
            }

            JSONObject metadata = new JSONObject();
            metadata.put("tableName", table.getTableName());
            metadata.put("name", table.getName());
            metadata.put("description", table.getDescription());
            metadata.put("isView", table.isView());

            // Get columns
            String colSql = "SELECT ColumnName, Name, Description, AD_Reference_ID, IsMandatory, IsKey " +
                           "FROM AD_Column WHERE AD_Table_ID = ? AND IsActive = 'Y' ORDER BY ColumnName";

            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(colSql.replace("?", String.valueOf(table.getAD_Table_ID())));
            request.setMaxRows(200);
            request.setQueryPurpose("Table metadata: " + tableName);

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                metadata.put("columns", result.getRows());
            }

            return metadata.toString(2);
        } catch (Exception e) {
            log.severe("Metadata lookup failed: " + e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    @Tool("List available iDempiere tables that the current user has access to.")
    public String listTables(
        @P("Filter by table name pattern (optional, e.g., 'C_%' for client tables)") String namePattern
    ) {
        log.info("ERPTools.listTables: pattern=" + namePattern);

        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT TableName, Name, Description FROM AD_Table ");
            sql.append("WHERE IsActive = 'Y' AND IsView = 'N' ");

            if (namePattern != null && !namePattern.isEmpty()) {
                sql.append("AND TableName LIKE '").append(namePattern.replace("'", "''")).append("' ");
            }

            sql.append("ORDER BY TableName");

            SecureQueryRequest request = new SecureQueryRequest();
            request.setCtx(ctx);
            request.setProviderId(provider.getAIG_Provider_ID());
            request.setSql(sql.toString());
            request.setMaxRows(100);
            request.setQueryPurpose("List tables");

            SecureQueryResult result = executor.executeQuery(request);

            if (result.isSuccess()) {
                return result.getRows().toString();
            } else {
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("List tables failed: " + e.getMessage());
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
        log.info("ERPTools.getBusinessPartner: " + identifier);

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
                return result.getRows().toString();
            } else {
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Business Partner lookup failed: " + e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    @Tool("Get details of a Product by ID or search value.")
    public String getProduct(
        @P("Product ID or Value (search key)") String identifier
    ) {
        log.info("ERPTools.getProduct: " + identifier);

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
                return result.getRows().toString();
            } else {
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Product lookup failed: " + e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    @Tool("Get details of an Order (Sales or Purchase) by DocumentNo or ID.")
    public String getOrder(
        @P("Order ID or DocumentNo") String identifier
    ) {
        log.info("ERPTools.getOrder: " + identifier);

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
                return result.getRows().toString();
            } else {
                return createErrorResponse(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.severe("Order lookup failed: " + e.getMessage());
            return createErrorResponse(e.getMessage());
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String createErrorResponse(String message) {
        JSONObject error = new JSONObject();
        error.put("error", true);
        error.put("message", message);
        return error.toString();
    }
}
