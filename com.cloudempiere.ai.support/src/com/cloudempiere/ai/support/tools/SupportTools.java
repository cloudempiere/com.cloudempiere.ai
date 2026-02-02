package com.cloudempiere.ai.support.tools;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.support.boundary.SupportDomainBoundary;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * Support domain tools for AI agents.
 *
 * <p>This class provides LangChain4j-annotated tools that support agents can use
 * to query and manage support tickets. All queries are validated against
 * {@link SupportDomainBoundary} and executed via {@link SecureDatabaseQueryExecutor}
 * for security.</p>
 *
 * <p><b>Architecture Layer:</b> Domain Tools Layer (Support)</p>
 * <p><b>Security:</b> All queries validated against SupportDomainBoundary</p>
 * <p><b>Execution:</b> Queries run via SecureDatabaseQueryExecutor (role-based access)</p>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
@Component(service = SupportTools.class, immediate = true)
public class SupportTools {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private volatile SecureDatabaseQueryExecutor dbExecutor;

    /**
     * Get support ticket details by ID.
     *
     * @param requestId the R_Request_ID
     * @return ticket details as JSON
     */
    @Tool("Get support ticket details including status, priority, category, description, and history")
    public String getTicket(@P("R_Request_ID") int requestId) {
        SupportDomainBoundary.validateReadTable("R_Request");

        String sql =
            "SELECT r.R_Request_ID, r.DocumentNo, r.Summary, r.RequestAmt, " +
            "       r.Priority, r.R_Status_ID, rs.Name AS StatusName, " +
            "       r.R_RequestType_ID, rt.Name AS TypeName, " +
            "       r.R_Category_ID, rc.Name AS CategoryName, " +
            "       r.C_BPartner_ID, bp.Name AS BPartnerName, " +
            "       r.AD_User_ID, u.Name AS ContactName, " +
            "       r.SalesRep_ID, sr.Name AS AssignedTo, " +
            "       r.DateNextAction, r.DueType, r.IsEscalated, " +
            "       r.Created, r.Updated " +
            "FROM R_Request r " +
            "LEFT JOIN R_Status rs ON r.R_Status_ID = rs.R_Status_ID " +
            "LEFT JOIN R_RequestType rt ON r.R_RequestType_ID = rt.R_RequestType_ID " +
            "LEFT JOIN R_Category rc ON r.R_Category_ID = rc.R_Category_ID " +
            "LEFT JOIN C_BPartner bp ON r.C_BPartner_ID = bp.C_BPartner_ID " +
            "LEFT JOIN AD_User u ON r.AD_User_ID = u.AD_User_ID " +
            "LEFT JOIN AD_User sr ON r.SalesRep_ID = sr.AD_User_ID " +
            "WHERE r.R_Request_ID = ?";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{requestId})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Search support tickets with filters.
     *
     * @param partnerId optional business partner ID filter
     * @param status optional status name filter
     * @param priority optional priority filter (High, Medium, Low, etc.)
     * @param assignedTo optional assigned user ID filter
     * @return list of tickets as JSON
     */
    @Tool("Search support tickets with optional filters for customer, status, priority, and assigned representative")
    public String searchTickets(
        @P("C_BPartner_ID (optional)") Integer partnerId,
        @P("Status name (optional)") String status,
        @P("Priority (optional): 1=High, 5=Medium, 9=Low") String priority,
        @P("SalesRep_ID (assigned user, optional)") Integer assignedTo
    ) {
        SupportDomainBoundary.validateReadTable("R_Request");

        StringBuilder sql = new StringBuilder(
            "SELECT r.R_Request_ID, r.DocumentNo, r.Summary, r.Priority, " +
            "       rs.Name AS StatusName, rt.Name AS TypeName, " +
            "       bp.Name AS BPartnerName, sr.Name AS AssignedTo, " +
            "       r.DateNextAction, r.IsEscalated, r.Created " +
            "FROM R_Request r " +
            "LEFT JOIN R_Status rs ON r.R_Status_ID = rs.R_Status_ID " +
            "LEFT JOIN R_RequestType rt ON r.R_RequestType_ID = rt.R_RequestType_ID " +
            "LEFT JOIN C_BPartner bp ON r.C_BPartner_ID = bp.C_BPartner_ID " +
            "LEFT JOIN AD_User sr ON r.SalesRep_ID = sr.AD_User_ID " +
            "WHERE 1=1"
        );

        java.util.List<Object> params = new java.util.ArrayList<>();

        if (partnerId != null) {
            sql.append(" AND r.C_BPartner_ID = ?");
            params.add(partnerId);
        }

        if (status != null && !status.isEmpty()) {
            sql.append(" AND rs.Name = ?");
            params.add(status);
        }

        if (priority != null && !priority.isEmpty()) {
            sql.append(" AND r.Priority = ?");
            params.add(priority);
        }

        if (assignedTo != null) {
            sql.append(" AND r.SalesRep_ID = ?");
            params.add(assignedTo);
        }

        sql.append(" ORDER BY r.Priority, r.DateNextAction, r.Created DESC");

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql.toString())
            .parameters(params.toArray())
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get ticket history and actions.
     *
     * @param requestId the R_Request_ID
     * @return ticket actions/history as JSON
     */
    @Tool("Get ticket history including all actions, updates, and comments")
    public String getTicketHistory(@P("R_Request_ID") int requestId) {
        SupportDomainBoundary.validateReadTable("R_RequestAction");

        String sql =
            "SELECT ra.R_RequestAction_ID, ra.Created, " +
            "       u.Name AS CreatedByName, " +
            "       ra.Summary, ra.IsSelfService, " +
            "       ra.DateNextAction, ra.IsComplete " +
            "FROM R_RequestAction ra " +
            "LEFT JOIN AD_User u ON ra.CreatedBy = u.AD_User_ID " +
            "WHERE ra.R_Request_ID = ? " +
            "ORDER BY ra.Created DESC";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{requestId})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get customer information for support context.
     *
     * @param partnerId the C_BPartner_ID
     * @return customer details as JSON
     */
    @Tool("Get customer information including contact details and support history")
    public String getCustomer(@P("C_BPartner_ID") int partnerId) {
        SupportDomainBoundary.validateReadTable("C_BPartner");

        String sql =
            "SELECT bp.C_BPartner_ID, bp.Name, bp.Value, " +
            "       bp.IsCustomer, " +
            "       (SELECT COUNT(*) FROM R_Request r WHERE r.C_BPartner_ID = bp.C_BPartner_ID) AS TotalTickets, " +
            "       (SELECT COUNT(*) FROM R_Request r WHERE r.C_BPartner_ID = bp.C_BPartner_ID AND r.IsEscalated='Y') AS EscalatedTickets " +
            "FROM C_BPartner bp " +
            "WHERE bp.C_BPartner_ID = ?";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{partnerId})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get available request types.
     *
     * @return list of request types as JSON
     */
    @Tool("Get available support ticket types and categories")
    public String getRequestTypes() {
        SupportDomainBoundary.validateReadTable("R_RequestType");

        String sql =
            "SELECT rt.R_RequestType_ID, rt.Name, rt.IsActive, rt.IsInvoiced, " +
            "       rt.DueType, rt.DueDateTolerance " +
            "FROM R_RequestType rt " +
            "WHERE rt.IsActive='Y' " +
            "ORDER BY rt.Name";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get escalated tickets summary.
     *
     * @return list of escalated tickets as JSON
     */
    @Tool("Get all escalated support tickets requiring immediate attention")
    public String getEscalatedTickets() {
        SupportDomainBoundary.validateReadTable("R_Request");

        String sql =
            "SELECT r.R_Request_ID, r.DocumentNo, r.Summary, r.Priority, " +
            "       rs.Name AS StatusName, bp.Name AS BPartnerName, " +
            "       sr.Name AS AssignedTo, r.DateNextAction, r.Created " +
            "FROM R_Request r " +
            "LEFT JOIN R_Status rs ON r.R_Status_ID = rs.R_Status_ID " +
            "LEFT JOIN C_BPartner bp ON r.C_BPartner_ID = bp.C_BPartner_ID " +
            "LEFT JOIN AD_User sr ON r.SalesRep_ID = sr.AD_User_ID " +
            "WHERE r.IsEscalated='Y' " +
            "ORDER BY r.Priority, r.DateNextAction";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }
}
