package com.cloudempiere.ai.purchasing.tools;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.purchasing.boundary.PurchasingDomainBoundary;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;

import java.util.Properties;

/**
 * Purchasing domain tools for AI agents.
 *
 * <p>This class provides LangChain4j-annotated tools that purchasing agents can use
 * to query and analyze purchasing data. All queries are validated against
 * {@link PurchasingDomainBoundary} and executed via {@link SecureDatabaseQueryExecutor}
 * for security.</p>
 *
 * <p><b>Architecture Layer:</b> Domain Tools Layer (Purchasing)</p>
 * <p><b>Security:</b> All queries validated against PurchasingDomainBoundary</p>
 * <p><b>Execution:</b> Queries run via SecureDatabaseQueryExecutor (role-based access)</p>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
@Component(service = PurchasingTools.class, immediate = true)
public class PurchasingTools {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private volatile SecureDatabaseQueryExecutor dbExecutor;

    /**
     * Get supplier (vendor) information by ID.
     *
     * @param partnerId the C_BPartner_ID
     * @return supplier details as JSON
     */
    @Tool("Get supplier (vendor) information including name, address, payment terms, and purchasing representative")
    public String getSupplier(@P("C_BPartner_ID") int partnerId) {
        PurchasingDomainBoundary.validateReadTable("C_BPartner");

        String sql =
            "SELECT bp.C_BPartner_ID, bp.Name, bp.Value, " +
            "       bp.IsVendor, bp.PO_PaymentTerm_ID, pt.Name AS PaymentTermName, " +
            "       bp.PO_PriceList_ID, pl.Name AS PriceListName, " +
            "       bp.POReference, u.Name AS PurchasingRepName " +
            "FROM C_BPartner bp " +
            "LEFT JOIN C_PaymentTerm pt ON bp.PO_PaymentTerm_ID = pt.C_PaymentTerm_ID " +
            "LEFT JOIN M_PriceList pl ON bp.PO_PriceList_ID = pl.M_PriceList_ID " +
            "LEFT JOIN AD_User u ON bp.SalesRep_ID = u.AD_User_ID " +
            "WHERE bp.C_BPartner_ID = ? AND bp.IsVendor='Y'";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{partnerId})
            .build();

        SecureQueryResult result = dbExecutor.executeSecureQuery(request);
        return result.toJSON();
    }

    /**
     * Get purchase order details by ID.
     *
     * @param orderId the C_Order_ID
     * @return purchase order details including lines as JSON
     */
    @Tool("Get purchase order details including header, lines, products, quantities, and amounts")
    public String getPurchaseOrder(@P("C_Order_ID") int orderId) {
        PurchasingDomainBoundary.validateReadTable("C_Order");
        PurchasingDomainBoundary.validateReadTable("C_OrderLine");

        String sql =
            "SELECT o.C_Order_ID, o.DocumentNo, o.DateOrdered, o.GrandTotal, " +
            "       o.DocStatus, o.C_BPartner_ID, bp.Name AS VendorName, " +
            "       ol.Line, ol.M_Product_ID, p.Name AS ProductName, " +
            "       ol.QtyOrdered, ol.PriceEntered, ol.LineNetAmt, " +
            "       o.DatePromised, o.POReference " +
            "FROM C_Order o " +
            "INNER JOIN C_BPartner bp ON o.C_BPartner_ID = bp.C_BPartner_ID " +
            "LEFT JOIN C_OrderLine ol ON o.C_Order_ID = ol.C_Order_ID " +
            "LEFT JOIN M_Product p ON ol.M_Product_ID = p.M_Product_ID " +
            "WHERE o.C_Order_ID = ? AND o.IsSOTrx='N' " +
            "ORDER BY ol.Line";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{orderId})
            .build();

        SecureQueryResult result = dbExecutor.executeSecureQuery(request);
        return result.toJSON();
    }

    /**
     * Get vendor invoice details by ID.
     *
     * @param invoiceId the C_Invoice_ID
     * @return vendor invoice details including lines as JSON
     */
    @Tool("Get vendor invoice details including header, lines, products, and amounts")
    public String getVendorInvoice(@P("C_Invoice_ID") int invoiceId) {
        PurchasingDomainBoundary.validateReadTable("C_Invoice");
        PurchasingDomainBoundary.validateReadTable("C_InvoiceLine");

        String sql =
            "SELECT inv.C_Invoice_ID, inv.DocumentNo, inv.DateInvoiced, inv.GrandTotal, " +
            "       inv.DocStatus, inv.C_BPartner_ID, bp.Name AS VendorName, " +
            "       il.Line, il.M_Product_ID, p.Name AS ProductName, " +
            "       il.QtyInvoiced, il.PriceEntered, il.LineNetAmt, " +
            "       inv.IsPaid, inv.DateAcct " +
            "FROM C_Invoice inv " +
            "INNER JOIN C_BPartner bp ON inv.C_BPartner_ID = bp.C_BPartner_ID " +
            "LEFT JOIN C_InvoiceLine il ON inv.C_Invoice_ID = il.C_Invoice_ID " +
            "LEFT JOIN M_Product p ON il.M_Product_ID = p.M_Product_ID " +
            "WHERE inv.C_Invoice_ID = ? AND inv.IsSOTrx='N' " +
            "ORDER BY il.Line";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{invoiceId})
            .build();

        SecureQueryResult result = dbExecutor.executeSecureQuery(request);
        return result.toJSON();
    }

    /**
     * Search purchase orders with filters.
     *
     * @param partnerId optional vendor ID filter
     * @param status optional document status filter
     * @param productId optional product ID filter
     * @return list of purchase orders as JSON
     */
    @Tool("Search purchase orders with optional filters for vendor, status, and product")
    public String searchPurchaseOrders(
        @P("C_BPartner_ID (optional)") Integer partnerId,
        @P("Document status (optional): DR=Draft, CO=Completed, CL=Closed") String status,
        @P("M_Product_ID (optional)") Integer productId
    ) {
        PurchasingDomainBoundary.validateReadTable("C_Order");

        StringBuilder sql = new StringBuilder(
            "SELECT o.C_Order_ID, o.DocumentNo, o.DateOrdered, o.GrandTotal, " +
            "       o.DocStatus, o.C_BPartner_ID, bp.Name AS VendorName, " +
            "       o.DatePromised, o.POReference " +
            "FROM C_Order o " +
            "INNER JOIN C_BPartner bp ON o.C_BPartner_ID = bp.C_BPartner_ID " +
            "WHERE o.IsSOTrx='N'"
        );

        java.util.List<Object> params = new java.util.ArrayList<>();

        if (partnerId != null) {
            sql.append(" AND o.C_BPartner_ID = ?");
            params.add(partnerId);
        }

        if (status != null && !status.isEmpty()) {
            sql.append(" AND o.DocStatus = ?");
            params.add(status);
        }

        if (productId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM C_OrderLine ol " +
                      "WHERE ol.C_Order_ID = o.C_Order_ID AND ol.M_Product_ID = ?)");
            params.add(productId);
        }

        sql.append(" ORDER BY o.DateOrdered DESC");

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql.toString())
            .parameters(params.toArray())
            .build();

        SecureQueryResult result = dbExecutor.executeSecureQuery(request);
        return result.toJSON();
    }

    /**
     * Get product information for purchasing.
     *
     * @param productId the M_Product_ID
     * @return product details as JSON
     */
    @Tool("Get product information including name, category, and purchasing details")
    public String getProduct(@P("M_Product_ID") int productId) {
        PurchasingDomainBoundary.validateReadTable("M_Product");

        String sql =
            "SELECT p.M_Product_ID, p.Value, p.Name, p.Description, " +
            "       p.M_Product_Category_ID, pc.Name AS CategoryName, " +
            "       p.IsPurchased, p.IsActive " +
            "FROM M_Product p " +
            "LEFT JOIN M_Product_Category pc ON p.M_Product_Category_ID = pc.M_Product_Category_ID " +
            "WHERE p.M_Product_ID = ?";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{productId})
            .build();

        SecureQueryResult result = dbExecutor.executeSecureQuery(request);
        return result.toJSON();
    }

    /**
     * Get purchasing performance summary for a vendor.
     *
     * @param partnerId the C_BPartner_ID
     * @return purchasing summary as JSON
     */
    @Tool("Get purchasing performance summary for a vendor including total orders, spending, and outstanding invoices")
    public String getVendorPurchasingSummary(@P("C_BPartner_ID") int partnerId) {
        PurchasingDomainBoundary.validateReadTable("C_BPartner");
        PurchasingDomainBoundary.validateReadTable("C_Order");
        PurchasingDomainBoundary.validateReadTable("C_Invoice");

        String sql =
            "SELECT bp.C_BPartner_ID, bp.Name, " +
            "       bp.PO_PaymentTerm_ID, pt.Name AS PaymentTermName, " +
            "       COUNT(DISTINCT o.C_Order_ID) AS TotalOrders, " +
            "       SUM(o.GrandTotal) AS TotalOrderAmount, " +
            "       COUNT(DISTINCT inv.C_Invoice_ID) AS TotalInvoices, " +
            "       SUM(inv.GrandTotal) AS TotalInvoiceAmount, " +
            "       SUM(CASE WHEN inv.IsPaid='N' THEN inv.GrandTotal ELSE 0 END) AS OutstandingAmount " +
            "FROM C_BPartner bp " +
            "LEFT JOIN C_PaymentTerm pt ON bp.PO_PaymentTerm_ID = pt.C_PaymentTerm_ID " +
            "LEFT JOIN C_Order o ON bp.C_BPartner_ID = o.C_BPartner_ID AND o.IsSOTrx='N' " +
            "LEFT JOIN C_Invoice inv ON bp.C_BPartner_ID = inv.C_BPartner_ID AND inv.IsSOTrx='N' " +
            "WHERE bp.C_BPartner_ID = ? AND bp.IsVendor='Y' " +
            "GROUP BY bp.C_BPartner_ID, bp.Name, bp.PO_PaymentTerm_ID, pt.Name";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{partnerId})
            .build();

        SecureQueryResult result = dbExecutor.executeSecureQuery(request);
        return result.toJSON();
    }
}
