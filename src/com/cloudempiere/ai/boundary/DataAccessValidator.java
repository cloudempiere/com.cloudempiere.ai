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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.cloudempiere.ai.agent.AgentContext;

/**
 * Data Access Validator for AI agents (ADR-009, ADR-029).
 *
 * <p>Validates that agents only access tables and columns they are authorized
 * to see based on their configured boundaries and user role.
 *
 * <p>Key features:
 * <ul>
 *   <li>Table whitelist/blacklist per agent type</li>
 *   <li>Column-level access control</li>
 *   <li>Sensitive data protection (payroll, pricing, etc.)</li>
 *   <li>Integration with iDempiere role-based access</li>
 *   <li>Multi-tenant knowledge table access (ADR-029)</li>
 * </ul>
 *
 * <p>ADR-029 Knowledge Access:
 * <ul>
 *   <li>Knowledge tables (AD_*, K_*) accessible via AD_Client_ID IN (0, KNOWLEDGE_CLIENT_ID)</li>
 *   <li>System client (0) provides base definitions</li>
 *   <li>Knowledge client (1000014) provides AI knowledge base content</li>
 * </ul>
 *
 * <p><b>Tags:</b> #boundaries #data-access #multi-tenant #security #role-based-access
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see BoundaryEnforcementFilter
 * @see AgentBoundary
 */
public class DataAccessValidator {

    private static final CLogger log = CLogger.getCLogger(DataAccessValidator.class);

    // ========================================================================
    // ADR-029: Multi-Tenant Knowledge Access Constants
    // ========================================================================

    /** Knowledge client ID for shared AI knowledge base content */
    public static final int KNOWLEDGE_CLIENT_ID = 1000014;

    /** Tables containing knowledge base content (K_ prefix) */
    private static final Set<String> KNOWLEDGE_TABLE_PREFIXES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "K_",      // Knowledge base tables
            "AD_"      // Application dictionary tables
        ))
    );

    /** Specific knowledge tables (not matching prefix) */
    private static final Set<String> KNOWLEDGE_TABLES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "K_Category", "K_CategoryValue", "K_Comment",
            "K_Entry", "K_Index", "K_IndexLog",
            "K_Source", "K_Synonym", "K_Topic", "K_Type"
        ))
    );

    // ========================================================================
    // Sensitive Tables (always require extra validation)
    // ========================================================================

    /** Tables containing sensitive HR/payroll data */
    private static final Set<String> SENSITIVE_HR_TABLES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "HR_Employee", "HR_Payroll", "HR_PayrollLine",
            "HR_Salary", "C_BP_BankAccount"
        ))
    );

    /** Tables containing sensitive pricing data */
    private static final Set<String> SENSITIVE_PRICING_TABLES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "M_ProductPrice", "M_PriceList_Version", "C_BP_Vendor_Acct",
            "M_Cost", "M_CostDetail"
        ))
    );

    /** Tables containing security-sensitive data */
    private static final Set<String> SECURITY_TABLES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "AD_User", "AD_Role", "AD_PInstance_Log", "AD_Changelog",
            "AD_Session", "AD_AccessLog"
        ))
    );

    // ========================================================================
    // Sensitive Columns (redacted from query results)
    // ========================================================================

    /** Columns that should never be exposed to AI */
    private static final Set<String> SENSITIVE_COLUMNS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "Password", "Salt", "APIKey", "SecretKey", "Token",
            "SSN", "SocialSecurityNo", "TaxID", "CreditCardNumber",
            "CreditCardVV", "BankAccountNo", "RoutingNo"
        ))
    );

    /**
     * Validate table access for an agent.
     *
     * @param context Agent context
     * @param tableName Table to access
     * @param boundary Agent's boundary configuration
     * @throws BoundaryViolationException if access denied
     */
    public void validateTableAccess(AgentContext context, String tableName,
                                     AgentBoundary boundary)
            throws BoundaryViolationException {

        if (tableName == null || tableName.isEmpty()) {
            throw new BoundaryViolationException(
                "Table name is required",
                BoundaryViolationType.TABLE_ACCESS_DENIED
            );
        }

        // 1. Check boundary whitelist/blacklist
        if (!boundary.isTableAllowed(tableName)) {
            log.warning("Table not in agent whitelist: " + tableName);
            throw new BoundaryViolationException(
                "Access denied to table: " + tableName,
                BoundaryViolationType.TABLE_ACCESS_DENIED
            );
        }

        // 2. Check if table exists
        int tableId = getTableId(tableName);
        if (tableId <= 0) {
            throw new BoundaryViolationException(
                "Table not found: " + tableName,
                BoundaryViolationType.TABLE_ACCESS_DENIED
            );
        }

        // 3. Check role-based table access
        MRole role = MRole.get(context.getCtx(), context.getRoleId());
        if (role != null && !role.isTableAccess(tableId, true)) { // true = read-only
            log.warning("Role doesn't have table access: " +
                       role.getName() + " → " + tableName);
            throw new BoundaryViolationException(
                "Role access denied to table: " + tableName,
                BoundaryViolationType.TABLE_ACCESS_DENIED
            );
        }

        // 4. Check sensitive table access
        if (isSensitiveTable(tableName)) {
            validateSensitiveTableAccess(context, tableName, boundary);
        }

        log.fine("Table access validated: " + tableName);
    }

    /**
     * Validate column access for an agent.
     *
     * @param context Agent context
     * @param tableName Table name
     * @param columnName Column to access
     * @param boundary Agent's boundary configuration
     * @throws BoundaryViolationException if access denied
     */
    public void validateColumnAccess(AgentContext context, String tableName,
                                      String columnName, AgentBoundary boundary)
            throws BoundaryViolationException {

        if (columnName == null || columnName.isEmpty()) {
            return; // No specific column
        }

        // 1. Check if column is globally sensitive
        if (isSensitiveColumn(columnName)) {
            log.warning("Attempt to access sensitive column: " + columnName);
            throw new BoundaryViolationException(
                "Access denied to sensitive column: " + columnName,
                BoundaryViolationType.SENSITIVE_DATA_ACCESS
            );
        }

        // 2. Check boundary column restrictions
        if (!boundary.isColumnAllowed(tableName, columnName)) {
            throw new BoundaryViolationException(
                "Access denied to column: " + tableName + "." + columnName,
                BoundaryViolationType.COLUMN_ACCESS_DENIED
            );
        }

        // 3. Check role-based column access (if implemented)
        MRole role = MRole.get(context.getCtx(), context.getRoleId());
        if (role != null) {
            int tableId = getTableId(tableName);
            int columnId = getColumnId(tableId, columnName);
            if (columnId > 0 && !role.isColumnAccess(tableId, columnId, true)) {
                log.warning("Role doesn't have column access: " +
                           role.getName() + " → " + tableName + "." + columnName);
                throw new BoundaryViolationException(
                    "Role access denied to column: " + columnName,
                    BoundaryViolationType.COLUMN_ACCESS_DENIED
                );
            }
        }
    }

    /**
     * Validate access to sensitive tables.
     *
     * @param context Agent context
     * @param tableName Sensitive table name
     * @param boundary Agent boundary
     * @throws BoundaryViolationException if access denied
     */
    private void validateSensitiveTableAccess(AgentContext context, String tableName,
                                               AgentBoundary boundary)
            throws BoundaryViolationException {

        // Check if boundary explicitly allows sensitive data
        if (!boundary.isSensitiveDataAllowed()) {
            String category = getSensitiveCategory(tableName);
            log.warning("Sensitive data access denied: " + tableName +
                       " [category=" + category + "]");
            throw new BoundaryViolationException(
                "Access denied to sensitive data: " + category,
                BoundaryViolationType.SENSITIVE_DATA_ACCESS
            );
        }

        // Log access to sensitive tables for audit
        log.info("Sensitive table access: " + tableName +
                " [user=" + context.getUserId() +
                ", role=" + context.getRoleId() + "]");
    }

    /**
     * Check if table is sensitive.
     *
     * @param tableName Table name
     * @return true if sensitive
     */
    public boolean isSensitiveTable(String tableName) {
        return SENSITIVE_HR_TABLES.contains(tableName) ||
               SENSITIVE_PRICING_TABLES.contains(tableName) ||
               SECURITY_TABLES.contains(tableName);
    }

    /**
     * Check if column is sensitive.
     *
     * @param columnName Column name
     * @return true if sensitive
     */
    public boolean isSensitiveColumn(String columnName) {
        // Case-insensitive check
        for (String sensitive : SENSITIVE_COLUMNS) {
            if (sensitive.equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the sensitive category for a table.
     *
     * @param tableName Table name
     * @return Category (HR, PRICING, SECURITY) or null
     */
    private String getSensitiveCategory(String tableName) {
        if (SENSITIVE_HR_TABLES.contains(tableName)) return "HR/Payroll";
        if (SENSITIVE_PRICING_TABLES.contains(tableName)) return "Pricing/Cost";
        if (SECURITY_TABLES.contains(tableName)) return "Security";
        return null;
    }

    /**
     * Get table ID from name.
     *
     * @param tableName Table name
     * @return Table ID or -1
     */
    private int getTableId(String tableName) {
        return DB.getSQLValue(null,
            "SELECT AD_Table_ID FROM AD_Table WHERE UPPER(TableName)=?",
            tableName.toUpperCase());
    }

    /**
     * Get column ID from table and column name.
     *
     * @param tableId Table ID
     * @param columnName Column name
     * @return Column ID or -1
     */
    private int getColumnId(int tableId, String columnName) {
        return DB.getSQLValue(null,
            "SELECT AD_Column_ID FROM AD_Column WHERE AD_Table_ID=? AND UPPER(ColumnName)=?",
            tableId, columnName.toUpperCase());
    }

    /**
     * Filter sensitive columns from a list.
     *
     * @param columns Column names
     * @return Filtered set (sensitive columns removed)
     */
    public Set<String> filterSensitiveColumns(Set<String> columns) {
        Set<String> filtered = new HashSet<>(columns);
        filtered.removeIf(this::isSensitiveColumn);
        return filtered;
    }

    /**
     * Get all sensitive tables.
     *
     * @return Unmodifiable set of sensitive table names
     */
    public Set<String> getSensitiveTables() {
        Set<String> all = new HashSet<>();
        all.addAll(SENSITIVE_HR_TABLES);
        all.addAll(SENSITIVE_PRICING_TABLES);
        all.addAll(SECURITY_TABLES);
        return Collections.unmodifiableSet(all);
    }

    // ========================================================================
    // ADR-029: Multi-Tenant Knowledge Access
    // ========================================================================

    /**
     * Check if table is a knowledge table (ADR-029).
     *
     * <p>Knowledge tables are shared across tenants and accessible via:
     * <pre>AD_Client_ID IN (0, KNOWLEDGE_CLIENT_ID)</pre>
     *
     * <p>Knowledge tables include:
     * <ul>
     *   <li>AD_* tables - Application Dictionary definitions</li>
     *   <li>K_* tables - Knowledge base content</li>
     * </ul>
     *
     * @param tableName Table name to check
     * @return true if table is a knowledge table
     */
    public boolean isKnowledgeTable(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return false;
        }

        // Check explicit knowledge tables
        if (KNOWLEDGE_TABLES.contains(tableName)) {
            return true;
        }

        // Check prefixes (AD_, K_)
        String upperName = tableName.toUpperCase();
        for (String prefix : KNOWLEDGE_TABLE_PREFIXES) {
            if (upperName.startsWith(prefix.toUpperCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the client filter for a table (ADR-029).
     *
     * <p>Returns appropriate AD_Client_ID filter based on table type:
     * <ul>
     *   <li>Knowledge tables: AD_Client_ID IN (0, 1000014)</li>
     *   <li>Business tables: AD_Client_ID = {userClientId}</li>
     * </ul>
     *
     * @param tableName Table name
     * @param userClientId User's client ID
     * @return SQL filter clause (without "AND" prefix)
     */
    public String getClientFilter(String tableName, int userClientId) {
        if (isKnowledgeTable(tableName)) {
            return "AD_Client_ID IN (0, " + KNOWLEDGE_CLIENT_ID + ")";
        } else {
            return "AD_Client_ID = " + userClientId;
        }
    }

    /**
     * Get the client filter for a table with service provider support (ADR-029).
     *
     * <p>Tiered access model:
     * <ul>
     *   <li>Tier 1 (End User): Own client + knowledge sources</li>
     *   <li>Tier 2 (Service Provider): Target client + knowledge sources</li>
     * </ul>
     *
     * @param tableName Table name
     * @param userClientId User's client ID
     * @param isServiceProvider Whether user is a service provider accessing another tenant
     * @param targetClientId Target client ID (for service providers)
     * @return SQL filter clause
     */
    public String getClientFilter(String tableName, int userClientId,
                                   boolean isServiceProvider, int targetClientId) {
        if (isKnowledgeTable(tableName)) {
            // Knowledge tables always accessible via system + knowledge client
            return "AD_Client_ID IN (0, " + KNOWLEDGE_CLIENT_ID + ")";
        } else if (isServiceProvider && targetClientId > 0) {
            // Service provider accessing target client's data
            return "AD_Client_ID = " + targetClientId;
        } else {
            // Normal user accessing own client's data
            return "AD_Client_ID = " + userClientId;
        }
    }

    /**
     * Get the knowledge client ID (ADR-029).
     *
     * @return Knowledge client ID (1000014)
     */
    public int getKnowledgeClientId() {
        return KNOWLEDGE_CLIENT_ID;
    }
}
