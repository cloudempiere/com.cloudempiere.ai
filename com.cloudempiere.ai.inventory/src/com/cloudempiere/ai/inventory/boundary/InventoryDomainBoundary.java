package com.cloudempiere.ai.inventory.boundary;

import java.util.Collections;
import java.util.Set;

/**
 * Inventory domain boundary - restricts AI access to inventory tables only.
 *
 * <p><b>Architecture Layer:</b> Domain Boundary Layer (Inventory)</p>
 * <p><b>Related ADRs:</b> ADR-009, ADR-011</p>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
public class InventoryDomainBoundary {

    /**
     * Tables that inventory agents can READ.
     */
    public static final Set<String> READ_TABLES = Collections.unmodifiableSet(Set.of(
        // Products
        "M_Product",
        "M_Product_Category",
        "M_AttributeSet",
        "M_AttributeSetInstance",

        // Warehouses and Storage
        "M_Warehouse",
        "M_Locator",
        "M_Storage",
        "M_StorageOnHand",
        "M_StorageReservation",

        // Inventory transactions
        "M_Transaction",
        "M_Movement",
        "M_MovementLine",
        "M_Inventory",
        "M_InventoryLine",

        // Material receipts
        "M_InOut",
        "M_InOutLine"
    ));

    /**
     * Tables that inventory agents can WRITE.
     */
    public static final Set<String> WRITE_TABLES = Collections.unmodifiableSet(Set.of(
        "M_Movement",      // Can create inventory movements
        "M_MovementLine",  // Can add movement lines
        "M_Inventory",     // Can create physical inventory
        "M_InventoryLine"  // Can add inventory lines
    ));

    /**
     * Actions that inventory agents can perform.
     */
    public static final Set<String> ALLOWED_ACTIONS = Collections.unmodifiableSet(Set.of(
        "READ",
        "CREATE_DRAFT",
        "UPDATE_DRAFT"
    ));

    public static void validateReadTable(String tableName) {
        if (!READ_TABLES.contains(tableName)) {
            throw new SecurityException(
                String.format("[Inventory Domain] Table '%s' not accessible. " +
                             "Allowed: %s", tableName, READ_TABLES)
            );
        }
    }

    public static void validateWriteTable(String tableName) {
        if (!WRITE_TABLES.contains(tableName)) {
            throw new SecurityException(
                String.format("[Inventory Domain] Table '%s' not writable. " +
                             "Allowed: %s", tableName, WRITE_TABLES)
            );
        }
    }

    public static void validateAction(String action) {
        if (!ALLOWED_ACTIONS.contains(action)) {
            throw new SecurityException(
                String.format("[Inventory Domain] Action '%s' not allowed. " +
                             "Allowed: %s", action, ALLOWED_ACTIONS)
            );
        }
    }
}
