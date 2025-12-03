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

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.model.MRole;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import com.cloudempiere.ai.agent.AgentContext;

/**
 * Boundary Enforcement Filter for AI agents (ADR-009, ADR-029).
 *
 * <p>Automatically injects AD_Client_ID and AD_Org_ID filters into SQL queries
 * to ensure agents only access data within their authorized scope.
 *
 * <p>Key features:
 * <ul>
 *   <li>Automatic client/org filtering on all queries</li>
 *   <li>Role-based organization access validation</li>
 *   <li>Detection of multi-tenant bypass attempts</li>
 *   <li>SQL injection prevention</li>
 *   <li>ADR-029: Tiered multi-tenant access (End User / Service Provider)</li>
 * </ul>
 *
 * <p>ADR-029 Access Tiers:
 * <ul>
 *   <li>Tier 1 (End User): Own client + knowledge sources (AD_*, K_*)</li>
 *   <li>Tier 2 (Service Provider): Target client + knowledge sources</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * BoundaryEnforcementFilter filter = new BoundaryEnforcementFilter();
 * String secureSql = filter.enforceOrgFilter(sql, context);
 * // Or with tiered access:
 * String secureSql = filter.enforceOrgFilter(sql, context, accessTier);
 * </pre>
 *
 * <p><b>Tags:</b> #boundaries #sql-filter #multi-tenant #security #injection-prevention
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see DataAccessValidator
 * @see AgentBoundary
 */
public class BoundaryEnforcementFilter {

    private static final CLogger log = CLogger.getCLogger(BoundaryEnforcementFilter.class);

    // ========================================================================
    // ADR-029: Access Tier Enumeration
    // ========================================================================

    /**
     * Access tier for multi-tenant AI operations (ADR-029).
     */
    public enum AIGAccessTier {
        /** Tenant only - user's own AD_Client_ID */
        TENANT,
        /** Tenant + Dictionary - own client + knowledge tables (AD_*, K_*) */
        TENANT_DICTIONARY,
        /** Service Provider - access to target client's data */
        SERVICE_PROVIDER
    }

    /** Data access validator for knowledge table detection */
    private final DataAccessValidator dataAccessValidator = new DataAccessValidator();

    /** Pattern to detect WHERE clause */
    private static final Pattern WHERE_PATTERN =
        Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);

    /** Pattern to detect FROM clause for table extraction */
    private static final Pattern FROM_PATTERN =
        Pattern.compile("\\bFROM\\s+([a-zA-Z_][a-zA-Z0-9_]*)",
            Pattern.CASE_INSENSITIVE);

    /** Pattern to detect potential SQL injection */
    private static final Pattern INJECTION_PATTERN =
        Pattern.compile(
            "(--)|(/\\*)|(\\.\\.)|(;\\s*DROP)|(\\.\\s*DELETE)|" +
            "(UNION\\s+ALL\\s+SELECT)|(UNION\\s+SELECT)",
            Pattern.CASE_INSENSITIVE);

    /** Tables that don't have AD_Client_ID (system tables) */
    private static final String[] SYSTEM_TABLES = {
        "AD_System", "AD_Language", "AD_Reference", "AD_Ref_List",
        "AD_Element", "AD_Message", "AD_Sequence"
    };

    /**
     * Enforce organization boundary on SQL query.
     *
     * <p>Adds AD_Client_ID and AD_Org_ID filters if not already present.
     *
     * @param sql Original SQL query
     * @param context Agent context with client/org info
     * @return Modified SQL with boundary filters
     * @throws BoundaryViolationException if SQL contains injection attempt
     */
    public String enforceOrgFilter(String sql, AgentContext context)
            throws BoundaryViolationException {

        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        // Check for injection attempts
        validateNoInjection(sql);

        // Extract main table name
        String tableName = extractMainTable(sql);
        if (tableName == null) {
            log.warning("Could not extract table name from SQL: " + sql);
            return sql;
        }

        // Skip system tables
        if (isSystemTable(tableName)) {
            log.fine("Skipping boundary filter for system table: " + tableName);
            return sql;
        }

        // Build filter clause
        StringBuilder filter = new StringBuilder();
        filter.append("AD_Client_ID = ").append(context.getClientId());

        // Add org filter if not accessing all orgs
        if (context.getOrgId() > 0) {
            filter.append(" AND AD_Org_ID IN (0, ").append(context.getOrgId()).append(")");
        } else {
            // Validate user can access all orgs
            MRole role = MRole.get(context.getCtx(), context.getRoleId());
            if (role != null && !role.isAccessAllOrgs()) {
                // Restrict to authorized orgs
                String orgClause = buildAuthorizedOrgClause(role);
                filter.append(" AND ").append(orgClause);
            }
        }

        // Inject filter into SQL
        String filteredSql = injectFilter(sql, filter.toString());

        log.fine("Boundary filter applied: " + tableName +
                " [Client=" + context.getClientId() +
                ", Org=" + context.getOrgId() + "]");

        return filteredSql;
    }

    /**
     * Enforce organization boundary on SQL query with access tier (ADR-029).
     *
     * <p>Applies appropriate client filter based on access tier:
     * <ul>
     *   <li>TENANT: AD_Client_ID = userClientId</li>
     *   <li>TENANT_DICTIONARY: AD_Client_ID = userClientId OR knowledge tables</li>
     *   <li>SERVICE_PROVIDER: AD_Client_ID = targetClientId</li>
     * </ul>
     *
     * @param sql Original SQL query
     * @param context Agent context with client/org info
     * @param accessTier Access tier for this operation
     * @param targetClientId Target client ID (for SERVICE_PROVIDER tier)
     * @return Modified SQL with boundary filters
     * @throws BoundaryViolationException if SQL contains injection attempt
     */
    public String enforceOrgFilter(String sql, AgentContext context,
                                    AIGAccessTier accessTier, int targetClientId)
            throws BoundaryViolationException {

        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        // Check for injection attempts
        validateNoInjection(sql);

        // Extract main table name
        String tableName = extractMainTable(sql);
        if (tableName == null) {
            log.warning("Could not extract table name from SQL: " + sql);
            return sql;
        }

        // Skip system tables
        if (isSystemTable(tableName)) {
            log.fine("Skipping boundary filter for system table: " + tableName);
            return sql;
        }

        // Build filter clause based on access tier
        StringBuilder filter = new StringBuilder();
        boolean isKnowledgeTable = dataAccessValidator.isKnowledgeTable(tableName);

        switch (accessTier) {
            case TENANT:
                // User's own client only
                filter.append("AD_Client_ID = ").append(context.getClientId());
                break;

            case TENANT_DICTIONARY:
                // User's client + knowledge tables
                if (isKnowledgeTable) {
                    filter.append("AD_Client_ID IN (0, ")
                          .append(DataAccessValidator.KNOWLEDGE_CLIENT_ID).append(")");
                } else {
                    filter.append("AD_Client_ID = ").append(context.getClientId());
                }
                break;

            case SERVICE_PROVIDER:
                // Target client (for service providers)
                if (isKnowledgeTable) {
                    filter.append("AD_Client_ID IN (0, ")
                          .append(DataAccessValidator.KNOWLEDGE_CLIENT_ID).append(")");
                } else if (targetClientId > 0) {
                    filter.append("AD_Client_ID = ").append(targetClientId);
                } else {
                    // Fallback to user's own client
                    filter.append("AD_Client_ID = ").append(context.getClientId());
                }
                break;

            default:
                filter.append("AD_Client_ID = ").append(context.getClientId());
        }

        // Add org filter if not accessing all orgs
        if (context.getOrgId() > 0 && !isKnowledgeTable) {
            filter.append(" AND AD_Org_ID IN (0, ").append(context.getOrgId()).append(")");
        } else if (!isKnowledgeTable) {
            // Validate user can access all orgs
            MRole role = MRole.get(context.getCtx(), context.getRoleId());
            if (role != null && !role.isAccessAllOrgs()) {
                // Restrict to authorized orgs
                String orgClause = buildAuthorizedOrgClause(role);
                filter.append(" AND ").append(orgClause);
            }
        }

        // Inject filter into SQL
        String filteredSql = injectFilter(sql, filter.toString());

        log.fine("Tiered boundary filter applied: " + tableName +
                " [Tier=" + accessTier +
                ", Client=" + (accessTier == AIGAccessTier.SERVICE_PROVIDER ? targetClientId : context.getClientId()) +
                ", Org=" + context.getOrgId() +
                ", KnowledgeTable=" + isKnowledgeTable + "]");

        return filteredSql;
    }

    /**
     * Check if a table is a knowledge table (ADR-029 delegation).
     *
     * @param tableName Table name
     * @return true if knowledge table (AD_*, K_*)
     */
    public boolean isKnowledgeTable(String tableName) {
        return dataAccessValidator.isKnowledgeTable(tableName);
    }

    /**
     * Get the knowledge client ID (ADR-029 delegation).
     *
     * @return Knowledge client ID (1000014)
     */
    public int getKnowledgeClientId() {
        return DataAccessValidator.KNOWLEDGE_CLIENT_ID;
    }

    /**
     * Validate SQL doesn't contain injection patterns.
     *
     * @param sql SQL to validate
     * @throws BoundaryViolationException if injection detected
     */
    private void validateNoInjection(String sql) throws BoundaryViolationException {
        Matcher matcher = INJECTION_PATTERN.matcher(sql);
        if (matcher.find()) {
            log.severe("SQL injection attempt detected: " + matcher.group());
            throw new BoundaryViolationException(
                "SQL injection attempt detected",
                BoundaryViolationType.INJECTION_ATTEMPT
            );
        }
    }

    /**
     * Extract main table name from SQL.
     *
     * @param sql SQL query
     * @return Table name or null
     */
    private String extractMainTable(String sql) {
        Matcher matcher = FROM_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Check if table is a system table (no client filtering).
     *
     * @param tableName Table name
     * @return true if system table
     */
    private boolean isSystemTable(String tableName) {
        for (String sysTable : SYSTEM_TABLES) {
            if (sysTable.equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build clause for authorized organizations.
     *
     * @param role User's role
     * @return SQL clause for org access
     */
    private String buildAuthorizedOrgClause(MRole role) {
        // Get org access from role
        // This uses iDempiere's role-based org access
        return "AD_Org_ID IN (SELECT AD_Org_ID FROM AD_Role_OrgAccess " +
               "WHERE AD_Role_ID = " + role.getAD_Role_ID() +
               " AND IsActive = 'Y')";
    }

    /**
     * Inject filter into SQL query.
     *
     * @param sql Original SQL
     * @param filter Filter clause
     * @return Modified SQL
     */
    private String injectFilter(String sql, String filter) {
        Matcher whereMatcher = WHERE_PATTERN.matcher(sql);

        if (whereMatcher.find()) {
            // Has WHERE clause - add filter with AND
            int whereEnd = whereMatcher.end();
            return sql.substring(0, whereEnd) + " " + filter + " AND " +
                   sql.substring(whereEnd).trim();
        } else {
            // No WHERE clause - add one before ORDER BY, GROUP BY, or at end
            String upperSql = sql.toUpperCase();
            int insertPos = sql.length();

            // Find first of ORDER BY, GROUP BY, LIMIT
            int orderBy = upperSql.indexOf("ORDER BY");
            int groupBy = upperSql.indexOf("GROUP BY");
            int limit = upperSql.indexOf("LIMIT");

            if (orderBy > 0) insertPos = Math.min(insertPos, orderBy);
            if (groupBy > 0) insertPos = Math.min(insertPos, groupBy);
            if (limit > 0) insertPos = Math.min(insertPos, limit);

            return sql.substring(0, insertPos).trim() +
                   " WHERE " + filter +
                   " " + sql.substring(insertPos);
        }
    }

    /**
     * Validate that a specific org ID is accessible.
     *
     * @param context Agent context
     * @param orgId Organization ID to check
     * @throws BoundaryViolationException if org not accessible
     */
    public void validateOrgAccess(AgentContext context, int orgId)
            throws BoundaryViolationException {

        MRole role = MRole.get(context.getCtx(), context.getRoleId());
        if (role == null) {
            throw new BoundaryViolationException(
                "Role not found: " + context.getRoleId(),
                BoundaryViolationType.INVALID_ROLE
            );
        }

        // Org 0 is always accessible
        if (orgId == 0) {
            return;
        }

        // Check if role has access to org
        if (!role.isOrgAccess(orgId, true)) { // true = read-only
            log.warning("Org access denied: Role=" + role.getName() +
                       ", Org=" + orgId);
            throw new BoundaryViolationException(
                "Access denied to organization: " + orgId,
                BoundaryViolationType.ORG_ACCESS_DENIED
            );
        }
    }

    /**
     * Validate that client ID matches context.
     *
     * @param context Agent context
     * @param clientId Client ID to check
     * @throws BoundaryViolationException if client doesn't match
     */
    public void validateClientAccess(AgentContext context, int clientId)
            throws BoundaryViolationException {

        if (clientId != context.getClientId()) {
            log.severe("Client boundary violation: expected=" + context.getClientId() +
                      ", actual=" + clientId);
            throw new BoundaryViolationException(
                "Access denied to client: " + clientId,
                BoundaryViolationType.CLIENT_ACCESS_DENIED
            );
        }
    }
}
