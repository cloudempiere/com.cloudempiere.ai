package com.cloudempiere.ai.support.boundary;

import java.util.Collections;
import java.util.Set;

/**
 * Support domain boundary - restricts AI access to support-related tables only.
 *
 * <p>This class enforces security boundaries for the Support domain agent,
 * ensuring it can only read/write tables relevant to support operations.</p>
 *
 * <p><b>Architecture Layer:</b> Domain Boundary Layer (Support)</p>
 * <p><b>Security Model:</b> Whitelist-based table access control</p>
 *
 * <p><b>Related ADRs:</b></p>
 * <ul>
 *   <li><a href="../../../docs/adr/009-domain-boundaries-agent-scope.md">ADR-009: Domain Boundaries and Agent Scope</a></li>
 *   <li><a href="../../../docs/adr/011-specialized-agent-scopes.md">ADR-011: Specialized Agent Scopes</a></li>
 * </ul>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
public class SupportDomainBoundary {

    /**
     * Tables that support agents can READ.
     *
     * <p><b>Allowed:</b></p>
     * <ul>
     *   <li>R_Request - Support tickets/requests</li>
     *   <li>R_RequestType - Request types</li>
     *   <li>R_RequestAction - Request actions/updates</li>
     *   <li>R_Category - Request categories</li>
     *   <li>C_BPartner - Business partners (customers)</li>
     *   <li>AD_User - Users (for assignment and contacts)</li>
     *   <li>M_Product - Product info (for product-related tickets)</li>
     *   <li>C_Order, C_Invoice - Related documents (read-only)</li>
     * </ul>
     */
    public static final Set<String> READ_TABLES = Collections.unmodifiableSet(Set.of(
        // Support Tickets
        "R_Request",
        "R_RequestType",
        "R_RequestAction",
        "R_Category",
        "R_Status",
        "R_Resolution",

        // Business Partners
        "C_BPartner",
        "C_BPartner_Location",
        "C_Location",
        "AD_User",

        // Related Documents (read-only)
        "C_Order",
        "C_OrderLine",
        "C_Invoice",
        "C_InvoiceLine",

        // Products (for product-related tickets)
        "M_Product",
        "M_Product_Category"
    ));

    /**
     * Tables that support agents can WRITE (create/update).
     *
     * <p><b>Allowed:</b></p>
     * <ul>
     *   <li>R_Request - Can create/update support tickets</li>
     *   <li>R_RequestAction - Can add updates/comments to tickets</li>
     * </ul>
     *
     * <p><b>NOT Allowed:</b></p>
     * <ul>
     *   <li>C_Order - Cannot create or modify orders</li>
     *   <li>C_BPartner - Cannot modify partners</li>
     *   <li>M_Product - Cannot modify products</li>
     *   <li>R_RequestType, R_Category - Cannot modify configuration</li>
     * </ul>
     */
    public static final Set<String> WRITE_TABLES = Collections.unmodifiableSet(Set.of(
        "R_Request",
        "R_RequestAction"
    ));

    /**
     * Actions that support agents can perform.
     *
     * <p><b>Allowed:</b></p>
     * <ul>
     *   <li>READ - Query data</li>
     *   <li>CREATE_DRAFT - Create draft opportunities</li>
     *   <li>UPDATE_DRAFT - Update draft opportunities</li>
     * </ul>
     *
     * <p><b>NOT Allowed:</b></p>
     * <ul>
     *   <li>DELETE - Cannot delete records</li>
     *   <li>COMPLETE - Cannot complete documents (requires human approval)</li>
     *   <li>VOID - Cannot void documents</li>
     * </ul>
     */
    public static final Set<String> ALLOWED_ACTIONS = Collections.unmodifiableSet(Set.of(
        "READ",
        "CREATE_DRAFT",
        "UPDATE_DRAFT"
    ));

    /**
     * Validates that the table can be read by support agents.
     *
     * @param tableName the table to validate
     * @throws SecurityException if table is not in READ_TABLES whitelist
     */
    public static void validateReadTable(String tableName) {
        if (!READ_TABLES.contains(tableName)) {
            throw new SecurityException(
                String.format("[Support Domain] Table '%s' not accessible for reading. " +
                             "Allowed tables: %s", tableName, READ_TABLES)
            );
        }
    }

    /**
     * Validates that the table can be written by support agents.
     *
     * @param tableName the table to validate
     * @throws SecurityException if table is not in WRITE_TABLES whitelist
     */
    public static void validateWriteTable(String tableName) {
        if (!WRITE_TABLES.contains(tableName)) {
            throw new SecurityException(
                String.format("[Support Domain] Table '%s' not writable. " +
                             "Allowed tables: %s", tableName, WRITE_TABLES)
            );
        }
    }

    /**
     * Validates that the action can be performed by support agents.
     *
     * @param action the action to validate
     * @throws SecurityException if action is not in ALLOWED_ACTIONS whitelist
     */
    public static void validateAction(String action) {
        if (!ALLOWED_ACTIONS.contains(action)) {
            throw new SecurityException(
                String.format("[Support Domain] Action '%s' not allowed. " +
                             "Allowed actions: %s", action, ALLOWED_ACTIONS)
            );
        }
    }
}
