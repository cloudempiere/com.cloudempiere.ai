package com.cloudempiere.ai.sales.tools;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.sales.boundary.SalesDomainBoundary;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;

import java.util.Properties;

/**
 * Sales domain tools for AI agents.
 *
 * <p>This class provides LangChain4j-annotated tools that sales agents can use
 * to query and manipulate sales data. All queries are validated against
 * {@link SalesDomainBoundary} and executed via {@link SecureDatabaseQueryExecutor}
 * for security.</p>
 *
 * <p><b>Architecture Layer:</b> Domain Tools Layer (Sales)</p>
 * <p><b>Security:</b> All queries validated against SalesDomainBoundary</p>
 * <p><b>Execution:</b> Queries run via SecureDatabaseQueryExecutor (role-based access)</p>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
@Component(service = SalesTools.class, immediate = true)
public class SalesTools {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private volatile SecureDatabaseQueryExecutor dbExecutor;

    /**
     * Get business partner information by ID.
     *
     * @param partnerId the C_BPartner_ID
     * @return business partner details as JSON
     */
    @Tool("Get business partner information including name, address, credit limit, and sales representative")
    public String getBusinessPartner(@P("C_BPartner_ID") int partnerId) {
        SalesDomainBoundary.validateReadTable("C_BPartner");

        String sql =
            "SELECT bp.C_BPartner_ID, bp.Name, bp.Value, " +
            "       bp.SO_CreditLimit, bp.TotalOpenBalance, " +
            "       bp.SalesRep_ID, u.Name AS SalesRepName " +
            "FROM C_BPartner bp " +
            "LEFT JOIN AD_User u ON bp.SalesRep_ID = u.AD_User_ID " +
            "WHERE bp.C_BPartner_ID = ?";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{partnerId})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get sales order details by ID.
     *
     * @param orderId the C_Order_ID
     * @return order details including lines as JSON
     */
    @Tool("Get sales order details including header, lines, products, quantities, and amounts")
    public String getSalesOrder(@P("C_Order_ID") int orderId) {
        SalesDomainBoundary.validateReadTable("C_Order");
        SalesDomainBoundary.validateReadTable("C_OrderLine");

        String sql =
            "SELECT o.C_Order_ID, o.DocumentNo, o.DateOrdered, o.GrandTotal, " +
            "       o.DocStatus, o.C_BPartner_ID, bp.Name AS BPartnerName, " +
            "       ol.Line, ol.M_Product_ID, p.Name AS ProductName, " +
            "       ol.QtyOrdered, ol.PriceEntered, ol.LineNetAmt " +
            "FROM C_Order o " +
            "INNER JOIN C_BPartner bp ON o.C_BPartner_ID = bp.C_BPartner_ID " +
            "LEFT JOIN C_OrderLine ol ON o.C_Order_ID = ol.C_Order_ID " +
            "LEFT JOIN M_Product p ON ol.M_Product_ID = p.M_Product_ID " +
            "WHERE o.C_Order_ID = ? " +
            "ORDER BY ol.Line";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{orderId})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Search sales opportunities.
     *
     * @param partnerId optional business partner ID filter
     * @param status optional opportunity status filter
     * @param salesRepId optional sales rep ID filter
     * @return list of opportunities as JSON
     */
    @Tool("Search sales opportunities with optional filters for business partner, status, and sales representative")
    public String searchOpportunities(
        @P("C_BPartner_ID (optional)") Integer partnerId,
        @P("Opportunity status (optional): Prospecting, Qualification, Needs Analysis, Value Proposition, Proposal, Negotiation, Closed Won, Closed Lost") String status,
        @P("SalesRep_ID (optional)") Integer salesRepId
    ) {
        SalesDomainBoundary.validateReadTable("C_Opportunity");

        StringBuilder sql = new StringBuilder(
            "SELECT opp.C_Opportunity_ID, opp.DocumentNo, opp.Description, " +
            "       opp.OpportunityAmt, opp.Probability, opp.OpportunityStatus, " +
            "       opp.C_BPartner_ID, bp.Name AS BPartnerName, " +
            "       opp.SalesRep_ID, u.Name AS SalesRepName, " +
            "       opp.DateNextAction, opp.ExpectedCloseDate " +
            "FROM C_Opportunity opp " +
            "INNER JOIN C_BPartner bp ON opp.C_BPartner_ID = bp.C_BPartner_ID " +
            "LEFT JOIN AD_User u ON opp.SalesRep_ID = u.AD_User_ID " +
            "WHERE 1=1"
        );

        java.util.List<Object> params = new java.util.ArrayList<>();

        if (partnerId != null) {
            sql.append(" AND opp.C_BPartner_ID = ?");
            params.add(partnerId);
        }

        if (status != null && !status.isEmpty()) {
            sql.append(" AND opp.OpportunityStatus = ?");
            params.add(status);
        }

        if (salesRepId != null) {
            sql.append(" AND opp.SalesRep_ID = ?");
            params.add(salesRepId);
        }

        sql.append(" ORDER BY opp.ExpectedCloseDate, opp.OpportunityAmt DESC");

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql.toString())
            .parameters(params.toArray())
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get sales opportunity details including lines.
     *
     * @param opportunityId the C_Opportunity_ID
     * @return opportunity details with lines as JSON
     */
    @Tool("Get sales opportunity details including opportunity lines with products and amounts")
    public String getOpportunity(@P("C_Opportunity_ID") int opportunityId) {
        SalesDomainBoundary.validateReadTable("C_Opportunity");
        SalesDomainBoundary.validateReadTable("C_OpportunityLine");

        String sql =
            "SELECT opp.C_Opportunity_ID, opp.DocumentNo, opp.Description, " +
            "       opp.OpportunityAmt, opp.Probability, opp.OpportunityStatus, " +
            "       opp.C_BPartner_ID, bp.Name AS BPartnerName, " +
            "       opp.SalesRep_ID, u.Name AS SalesRepName, " +
            "       oppline.Line, oppline.M_Product_ID, p.Name AS ProductName, " +
            "       oppline.Qty, oppline.UnitPrice, oppline.Amount " +
            "FROM C_Opportunity opp " +
            "INNER JOIN C_BPartner bp ON opp.C_BPartner_ID = bp.C_BPartner_ID " +
            "LEFT JOIN AD_User u ON opp.SalesRep_ID = u.AD_User_ID " +
            "LEFT JOIN C_OpportunityLine oppline ON opp.C_Opportunity_ID = oppline.C_Opportunity_ID " +
            "LEFT JOIN M_Product p ON oppline.M_Product_ID = p.M_Product_ID " +
            "WHERE opp.C_Opportunity_ID = ? " +
            "ORDER BY oppline.Line";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql.toString())
            .parameters(new Object[]{opportunityId})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get product information.
     *
     * @param productId the M_Product_ID
     * @return product details as JSON
     */
    @Tool("Get product information including name, category, price, and availability")
    public String getProduct(@P("M_Product_ID") int productId) {
        SalesDomainBoundary.validateReadTable("M_Product");

        String sql =
            "SELECT p.M_Product_ID, p.Value, p.Name, p.Description, " +
            "       p.M_Product_Category_ID, pc.Name AS CategoryName, " +
            "       p.IsSold, p.IsActive " +
            "FROM M_Product p " +
            "LEFT JOIN M_Product_Category pc ON p.M_Product_Category_ID = pc.M_Product_Category_ID " +
            "WHERE p.M_Product_ID = ?";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{productId})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }

    /**
     * Get sales performance summary for a business partner.
     *
     * @param partnerId the C_BPartner_ID
     * @return sales summary as JSON
     */
    @Tool("Get sales performance summary for a business partner including total orders, revenue, and outstanding balance")
    public String getPartnerSalesSummary(@P("C_BPartner_ID") int partnerId) {
        SalesDomainBoundary.validateReadTable("C_BPartner");
        SalesDomainBoundary.validateReadTable("C_Order");
        SalesDomainBoundary.validateReadTable("C_Invoice");

        String sql =
            "SELECT bp.C_BPartner_ID, bp.Name, " +
            "       bp.SO_CreditLimit, bp.TotalOpenBalance, " +
            "       COUNT(DISTINCT o.C_Order_ID) AS TotalOrders, " +
            "       SUM(o.GrandTotal) AS TotalOrderAmount, " +
            "       COUNT(DISTINCT inv.C_Invoice_ID) AS TotalInvoices, " +
            "       SUM(inv.GrandTotal) AS TotalInvoiceAmount " +
            "FROM C_BPartner bp " +
            "LEFT JOIN C_Order o ON bp.C_BPartner_ID = o.C_BPartner_ID AND o.IsSOTrx='Y' " +
            "LEFT JOIN C_Invoice inv ON bp.C_BPartner_ID = inv.C_BPartner_ID AND inv.IsSOTrx='Y' " +
            "WHERE bp.C_BPartner_ID = ? " +
            "GROUP BY bp.C_BPartner_ID, bp.Name, bp.SO_CreditLimit, bp.TotalOpenBalance";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{partnerId})
            .build();

        SecureQueryResult result = dbExecutor.executeQuery(request);
        return result.toJSON().toString();
    }
}
