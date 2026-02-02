package com.cloudempiere.ai.inventory.tools;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.inventory.boundary.InventoryDomainBoundary;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;

/**
 * Inventory domain tools for AI agents.
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
@Component(service = InventoryTools.class, immediate = true)
public class InventoryTools {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private volatile SecureDatabaseQueryExecutor dbExecutor;

    @Tool("Get product stock levels across all warehouses")
    public String getProductStock(@P("M_Product_ID") int productId) {
        InventoryDomainBoundary.validateReadTable("M_Storage");

        String sql =
            "SELECT s.M_Product_ID, p.Name AS ProductName, " +
            "       s.M_Locator_ID, l.Value AS LocatorName, " +
            "       s.M_Warehouse_ID, w.Name AS WarehouseName, " +
            "       s.QtyOnHand, s.QtyReserved, s.QtyAvailable " +
            "FROM M_Storage s " +
            "INNER JOIN M_Product p ON s.M_Product_ID = p.M_Product_ID " +
            "INNER JOIN M_Locator l ON s.M_Locator_ID = l.M_Locator_ID " +
            "INNER JOIN M_Warehouse w ON s.M_Warehouse_ID = w.M_Warehouse_ID " +
            "WHERE s.M_Product_ID = ? AND s.QtyOnHand > 0";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{productId})
            .build();

        return dbExecutor.executeSecureQuery(request).toJSON();
    }

    @Tool("Get warehouse inventory summary")
    public String getWarehouseInventory(@P("M_Warehouse_ID") int warehouseId) {
        InventoryDomainBoundary.validateReadTable("M_Storage");

        String sql =
            "SELECT w.M_Warehouse_ID, w.Name AS WarehouseName, " +
            "       COUNT(DISTINCT s.M_Product_ID) AS ProductCount, " +
            "       SUM(s.QtyOnHand) AS TotalQtyOnHand, " +
            "       SUM(s.QtyReserved) AS TotalQtyReserved, " +
            "       SUM(s.QtyAvailable) AS TotalQtyAvailable " +
            "FROM M_Warehouse w " +
            "LEFT JOIN M_Locator l ON w.M_Warehouse_ID = l.M_Warehouse_ID " +
            "LEFT JOIN M_Storage s ON l.M_Locator_ID = s.M_Locator_ID " +
            "WHERE w.M_Warehouse_ID = ? " +
            "GROUP BY w.M_Warehouse_ID, w.Name";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{warehouseId})
            .build();

        return dbExecutor.executeSecureQuery(request).toJSON();
    }

    @Tool("Search for products with low stock levels")
    public String searchLowStockProducts(@P("Minimum quantity threshold") int threshold) {
        InventoryDomainBoundary.validateReadTable("M_Storage");
        InventoryDomainBoundary.validateReadTable("M_Product");

        String sql =
            "SELECT p.M_Product_ID, p.Name, p.Value, " +
            "       SUM(s.QtyOnHand) AS TotalStock, " +
            "       SUM(s.QtyReserved) AS TotalReserved, " +
            "       SUM(s.QtyAvailable) AS TotalAvailable " +
            "FROM M_Product p " +
            "LEFT JOIN M_Storage s ON p.M_Product_ID = s.M_Product_ID " +
            "WHERE p.IsActive='Y' AND p.IsStocked='Y' " +
            "GROUP BY p.M_Product_ID, p.Name, p.Value " +
            "HAVING SUM(s.QtyAvailable) < ? " +
            "ORDER BY SUM(s.QtyAvailable)";

        SecureQueryRequest request = SecureQueryRequest.builder()
            .sql(sql)
            .parameters(new Object[]{threshold})
            .build();

        return dbExecutor.executeSecureQuery(request).toJSON();
    }
}
