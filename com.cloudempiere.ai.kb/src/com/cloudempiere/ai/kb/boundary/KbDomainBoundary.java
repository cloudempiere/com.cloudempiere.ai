package com.cloudempiere.ai.kb.boundary;

import java.util.Collections;
import java.util.Set;

/**
 * Kb domain boundary - restricts AI access to kb-related tables only.
 *
 * <p>This class enforces security boundaries for the Kb domain agent,
 * ensuring it can only read/write tables relevant to kb operations.</p>
 *
 * <p><b>Architecture Layer:</b> Domain Boundary Layer (Kb)</p>
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
public class KbDomainBoundary {

    /**
     * Tables that kb agents can READ.
     *
     * <p><b>Allowed:</b></p>
     * <ul>
     *   <li>C_Order, C_OrderLine - Kb orders</li>
     *   <li>C_Opportunity, C_OpportunityLine - Opportunities</li>
     *   <li>C_BPartner, C_BPartner_Location - Business partners</li>
     *   <li>M_Product - Product info (read-only reference)</li>
     *   <li>C_Invoice, C_InvoiceLine - Invoices (read-only)</li>
     *   <li>AD_User - Users (for opportunity owners)</li>
     * </ul>
     */
    public static final Set<String> READ_TABLES = Collections.unmodifiableSet(Set.of(
        // Kb Orders
        "C_Order",
        "C_OrderLine",

        // Kb Opportunities
        "C_Opportunity",
        "C_OpportunityLine",

        // Business Partners
        "C_BPartner",
        "C_BPartner_Location",
        "C_Location",

        // Products (read-only reference)
        "M_Product",
        "M_Product_Category",

        // Invoices (read-only)
        "C_Invoice",
        "C_InvoiceLine",

        // Users (for opportunity assignment)
        "AD_User",

        // Kb regions
        "C_KbRegion"
    ));

    /**
     * Tables that kb agents can WRITE (create/update).
     *
     * <p><b>Allowed:</b></p>
     * <ul>
     *   <li>C_Opportunity, C_OpportunityLine - Can create/update opportunities</li>
     * </ul>
     *
     * <p><b>NOT Allowed:</b></p>
     * <ul>
     *   <li>C_Order - Cannot create orders (only read existing)</li>
     *   <li>C_BPartner - Cannot modify partners</li>
     *   <li>M_Product - Cannot modify products</li>
     * </ul>
     */
    public static final Set<String> WRITE_TABLES = Collections.unmodifiableSet(Set.of(
        "C_Opportunity",
        "C_OpportunityLine"
    ));

    /**
     * Actions that kb agents can perform.
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
     * Validates that the table can be read by kb agents.
     *
     * @param tableName the table to validate
     * @throws SecurityException if table is not in READ_TABLES whitelist
     */
    public static void validateReadTable(String tableName) {
        if (!READ_TABLES.contains(tableName)) {
            throw new SecurityException(
                String.format("[Kb Domain] Table '%s' not accessible for reading. " +
                             "Allowed tables: %s", tableName, READ_TABLES)
            );
        }
    }

    /**
     * Validates that the table can be written by kb agents.
     *
     * @param tableName the table to validate
     * @throws SecurityException if table is not in WRITE_TABLES whitelist
     */
    public static void validateWriteTable(String tableName) {
        if (!WRITE_TABLES.contains(tableName)) {
            throw new SecurityException(
                String.format("[Kb Domain] Table '%s' not writable. " +
                             "Allowed tables: %s", tableName, WRITE_TABLES)
            );
        }
    }

    /**
     * Validates that the action can be performed by kb agents.
     *
     * @param action the action to validate
     * @throws SecurityException if action is not in ALLOWED_ACTIONS whitelist
     */
    public static void validateAction(String action) {
        if (!ALLOWED_ACTIONS.contains(action)) {
            throw new SecurityException(
                String.format("[Kb Domain] Action '%s' not allowed. " +
                             "Allowed actions: %s", action, ALLOWED_ACTIONS)
            );
        }
    }
}
