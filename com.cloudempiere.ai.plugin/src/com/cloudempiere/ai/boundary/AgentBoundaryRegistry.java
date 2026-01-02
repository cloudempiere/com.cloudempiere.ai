/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.boundary;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.boundary.AgentBoundary.ActionType;

/**
 * Registry of Agent Boundaries (ADR-009, ADR-011).
 *
 * <p>Central registry for all agent type boundaries. Provides predefined
 * boundaries for specialized agents and allows custom boundary registration.
 *
 * <p>Predefined agent types:
 * <ul>
 *   <li><b>inventory-agent</b>: Read-only access to inventory tables</li>
 *   <li><b>sales-agent</b>: Read + limited write to sales tables</li>
 *   <li><b>purchasing-agent</b>: Read + limited write to purchasing tables</li>
 *   <li><b>knowledge-base-agent</b>: Read-only access to documentation</li>
 *   <li><b>general-agent</b>: Default boundary for untyped agents</li>
 * </ul>
 *
 * @author Cloudempiere AI Team
 * @version ADR-009
 * @since v0.10.0
 */
public class AgentBoundaryRegistry {

    private static final CLogger log = CLogger.getCLogger(AgentBoundaryRegistry.class);

    /** Singleton instance */
    private static final AgentBoundaryRegistry INSTANCE = new AgentBoundaryRegistry();

    /** Registered boundaries by agent type */
    private final Map<String, AgentBoundary> boundaries = new ConcurrentHashMap<>();

    /** Default boundary for unknown agent types */
    private final AgentBoundary defaultBoundary;

    // ========================================================================
    // Agent Type Constants
    // ========================================================================

    public static final String AGENT_INVENTORY = "inventory-agent";
    public static final String AGENT_SALES = "sales-agent";
    public static final String AGENT_PURCHASING = "purchasing-agent";
    public static final String AGENT_KNOWLEDGE_BASE = "knowledge-base-agent";
    public static final String AGENT_GENERAL = "general-agent";

    /**
     * Private constructor - use getInstance()
     */
    private AgentBoundaryRegistry() {
        // Create default boundary
        this.defaultBoundary = createDefaultBoundary();

        // Register predefined boundaries
        registerPredefinedBoundaries();

        log.info("AgentBoundaryRegistry initialized with " + boundaries.size() + " boundaries");
    }

    /**
     * Get singleton instance.
     *
     * @return Registry instance
     */
    public static AgentBoundaryRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Get boundary for an agent type.
     *
     * @param agentType Agent type identifier
     * @return Boundary configuration (never null)
     */
    public AgentBoundary getBoundary(String agentType) {
        if (agentType == null || agentType.isEmpty()) {
            return defaultBoundary;
        }

        return boundaries.getOrDefault(agentType, defaultBoundary);
    }

    /**
     * Register a custom boundary.
     *
     * @param boundary Boundary to register
     */
    public void register(AgentBoundary boundary) {
        boundaries.put(boundary.getAgentType(), boundary);
        log.info("Registered boundary: " + boundary.getAgentType());
    }

    /**
     * Check if a boundary is registered.
     *
     * @param agentType Agent type
     * @return true if registered
     */
    public boolean hasBoundary(String agentType) {
        return boundaries.containsKey(agentType);
    }

    /**
     * Get all registered agent types.
     *
     * @return Unmodifiable set of agent types
     */
    public java.util.Set<String> getAgentTypes() {
        return Collections.unmodifiableSet(boundaries.keySet());
    }

    /**
     * Get default boundary.
     *
     * @return Default boundary
     */
    public AgentBoundary getDefaultBoundary() {
        return defaultBoundary;
    }

    // ========================================================================
    // Predefined Boundaries
    // ========================================================================

    private void registerPredefinedBoundaries() {
        register(createInventoryAgentBoundary());
        register(createSalesAgentBoundary());
        register(createPurchasingAgentBoundary());
        register(createKnowledgeBaseAgentBoundary());
        register(createGeneralAgentBoundary());
    }

    /**
     * Create default (restrictive) boundary.
     */
    private AgentBoundary createDefaultBoundary() {
        return AgentBoundary.builder("default")
            .description("Default restrictive boundary for unknown agents")
            .allowActions(ActionType.READ, ActionType.QUERY)
            .maxCostPerRequest(new BigDecimal("0.10"))
            .maxCostPerDay(new BigDecimal("5.00"))
            .maxToolCallsPerRequest(20)
            .maxToolCallsPerMinute(50)
            .maxTokensPerRequest(50000)
            .build();
    }

    /**
     * Create Inventory Agent boundary (ADR-011).
     *
     * <p>Read-only access to inventory-related tables.
     */
    private AgentBoundary createInventoryAgentBoundary() {
        return AgentBoundary.builder(AGENT_INVENTORY)
            .description("Read-only access to inventory data")
            .allowTables(
                // Core inventory tables
                "M_Product", "M_Product_Category", "M_Product_BOM",
                "M_Warehouse", "M_Locator",
                "M_Storage", "M_StorageOnHand", "M_StorageReservation",
                // Inventory transactions
                "M_InOut", "M_InOutLine", "M_InOutConfirm",
                "M_Inventory", "M_InventoryLine",
                "M_Movement", "M_MovementLine",
                // Reference data
                "C_UOM", "C_UOM_Conversion",
                "M_AttributeSet", "M_Attribute", "M_AttributeValue"
            )
            .blockTables(
                // Block cost data
                "M_Cost", "M_CostDetail", "M_CostQueue"
            )
            .allowActions(ActionType.READ, ActionType.QUERY, ActionType.REPORT)
            .allowSensitiveData(false)
            .maxCostPerRequest(new BigDecimal("0.25"))
            .maxCostPerDay(new BigDecimal("10.00"))
            .maxToolCallsPerRequest(30)
            .maxToolCallsPerMinute(100)
            .maxTokensPerRequest(100000)
            .build();
    }

    /**
     * Create Sales Agent boundary (ADR-011).
     *
     * <p>Read + limited write access to sales-related tables.
     */
    private AgentBoundary createSalesAgentBoundary() {
        return AgentBoundary.builder(AGENT_SALES)
            .description("Sales order and customer data access")
            .allowTables(
                // Customer/partner tables
                "C_BPartner", "C_BPartner_Location", "C_BP_Group",
                "C_Location", "C_Country", "C_Region", "C_City",
                // Sales order tables
                "C_Order", "C_OrderLine", "C_OrderTax",
                "C_DocType",
                // Pricing
                "M_PriceList", "M_PriceList_Version",
                // Products (for quoting)
                "M_Product", "M_Product_Category",
                // Invoicing (read-only)
                "C_Invoice", "C_InvoiceLine",
                // Payments (read-only)
                "C_Payment"
            )
            .blockTables(
                // Block sensitive financial data
                "C_BP_BankAccount", "C_BP_Vendor_Acct"
            )
            .blockColumns("C_BPartner", "TaxID", "DUNS")
            .allowActions(ActionType.READ, ActionType.QUERY, ActionType.CREATE,
                         ActionType.UPDATE, ActionType.REPORT)
            .allowSensitiveData(false)
            .maxCostPerRequest(new BigDecimal("0.50"))
            .maxCostPerDay(new BigDecimal("25.00"))
            .maxToolCallsPerRequest(50)
            .maxToolCallsPerMinute(150)
            .maxTokensPerRequest(150000)
            .build();
    }

    /**
     * Create Purchasing Agent boundary (ADR-011).
     *
     * <p>Read + limited write access to purchasing-related tables.
     */
    private AgentBoundary createPurchasingAgentBoundary() {
        return AgentBoundary.builder(AGENT_PURCHASING)
            .description("Purchase order and vendor data access")
            .allowTables(
                // Vendor/partner tables
                "C_BPartner", "C_BPartner_Location", "C_BP_Group",
                "C_Location", "C_Country", "C_Region",
                // Purchase order tables
                "C_Order", "C_OrderLine",
                "C_DocType",
                // Products
                "M_Product", "M_Product_Category", "M_Product_PO",
                // Receiving
                "M_InOut", "M_InOutLine",
                // Vendor invoices
                "C_Invoice", "C_InvoiceLine",
                // Inventory (for planning)
                "M_Storage", "M_Warehouse"
            )
            .blockTables(
                "C_BP_BankAccount", "M_Cost", "M_CostDetail"
            )
            .allowActions(ActionType.READ, ActionType.QUERY, ActionType.CREATE,
                         ActionType.UPDATE, ActionType.REPORT)
            .allowSensitiveData(false)
            .maxCostPerRequest(new BigDecimal("0.50"))
            .maxCostPerDay(new BigDecimal("25.00"))
            .maxToolCallsPerRequest(50)
            .maxToolCallsPerMinute(150)
            .maxTokensPerRequest(150000)
            .build();
    }

    /**
     * Create Knowledge Base Agent boundary (ADR-016).
     *
     * <p>Read-only access to documentation and AD metadata.
     */
    private AgentBoundary createKnowledgeBaseAgentBoundary() {
        return AgentBoundary.builder(AGENT_KNOWLEDGE_BASE)
            .description("Read-only access to documentation and metadata")
            .allowTables(
                // Application Dictionary (metadata)
                "AD_Table", "AD_Column", "AD_Element",
                "AD_Window", "AD_Tab", "AD_Field",
                "AD_Process", "AD_Process_Para",
                "AD_Form", "AD_InfoWindow",
                "AD_Reference", "AD_Ref_List", "AD_Ref_Table",
                // Help/documentation
                "AD_Message", "AD_Note",
                // User/role info (limited)
                "AD_Role"
            )
            .blockColumns("AD_Role", "UserLevel") // Don't expose role permissions
            .allowActions(ActionType.READ, ActionType.QUERY)
            .allowSensitiveData(false)
            .maxCostPerRequest(new BigDecimal("0.15"))
            .maxCostPerDay(new BigDecimal("5.00"))
            .maxToolCallsPerRequest(25)
            .maxToolCallsPerMinute(75)
            .maxTokensPerRequest(75000)
            .build();
    }

    /**
     * Create General Agent boundary.
     *
     * <p>Balanced access for general-purpose queries.
     */
    private AgentBoundary createGeneralAgentBoundary() {
        return AgentBoundary.builder(AGENT_GENERAL)
            .description("General-purpose agent with balanced access")
            // Empty whitelist = allow all (except blacklisted)
            .blockTables(
                // Block sensitive tables
                "AD_User", "AD_Role", "AD_Session", "AD_PInstance_Log",
                "HR_Employee", "HR_Payroll", "HR_Salary",
                "C_BP_BankAccount", "M_Cost", "M_CostDetail"
            )
            .allowActions(ActionType.READ, ActionType.QUERY, ActionType.REPORT)
            .allowSensitiveData(false)
            .maxCostPerRequest(new BigDecimal("0.50"))
            .maxCostPerDay(new BigDecimal("20.00"))
            .maxToolCallsPerRequest(40)
            .maxToolCallsPerMinute(120)
            .maxTokensPerRequest(100000)
            .build();
    }
}
