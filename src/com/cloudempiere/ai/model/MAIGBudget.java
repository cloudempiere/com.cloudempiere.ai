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
package com.cloudempiere.ai.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;

/**
 * Model class for AIG_Budget (ADR-013).
 *
 * <p>Provides business logic for managing AI budget limits and tracking
 * current usage at client, user, and agent levels.
 *
 * <p>Key features:
 * <ul>
 *   <li>Budget scope management (CLIENT, USER, AGENT)</li>
 *   <li>Daily and monthly limit enforcement</li>
 *   <li>Current usage tracking</li>
 *   <li>Rate limiting configuration</li>
 *   <li>Automatic reset handling</li>
 * </ul>
 *
 * <p><b>Tags:</b> #model #budget #cost-control #rate-limiting #multi-tenant
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see com.cloudempiere.ai.observability.CostGuard
 * @see MAIGUsageMetrics
 */
public class MAIGBudget extends X_AIG_Budget {

    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(MAIGBudget.class);

    // ========================================================================
    // Budget Scope Constants
    // ========================================================================

    /** Budget scope: Client level (applies to entire tenant) */
    public static final String SCOPE_CLIENT = "CLIENT";

    /** Budget scope: User level (applies to specific user) */
    public static final String SCOPE_USER = "USER";

    /** Budget scope: Agent level (applies to specific agent) */
    public static final String SCOPE_AGENT = "AGENT";

    // ========================================================================
    // Default Values
    // ========================================================================

    /** Default daily limit in cents (5000 = $50.00) */
    public static final int DEFAULT_DAILY_LIMIT_CENTS = 5000;

    /** Default monthly limit in cents (50000 = $500.00) */
    public static final int DEFAULT_MONTHLY_LIMIT_CENTS = 50000;

    /** Default token limit per request */
    public static final int DEFAULT_TOKEN_LIMIT = 4000;

    /** Default requests per minute */
    public static final int DEFAULT_REQUESTS_PER_MINUTE = 20;

    // ========================================================================
    // Cache
    // ========================================================================

    /** Cache of budgets by ID */
    private static CCache<Integer, MAIGBudget> s_cache =
        new CCache<>(Table_Name, 20, 30); // 30 minute timeout

    /** Cache of client budgets */
    private static CCache<Integer, MAIGBudget> s_clientCache =
        new CCache<>(Table_Name + "_Client", 20, 30);

    // ========================================================================
    // Constructors
    // ========================================================================

    /**
     * Standard Constructor.
     *
     * @param ctx Context
     * @param AIG_Budget_ID ID
     * @param trxName Transaction name
     */
    public MAIGBudget(Properties ctx, int AIG_Budget_ID, String trxName) {
        super(ctx, AIG_Budget_ID, trxName);
    }

    /**
     * Standard Constructor with virtual columns.
     *
     * @param ctx Context
     * @param AIG_Budget_ID ID
     * @param trxName Transaction name
     * @param virtualColumns Virtual columns
     */
    public MAIGBudget(Properties ctx, int AIG_Budget_ID, String trxName, String... virtualColumns) {
        super(ctx, AIG_Budget_ID, trxName, virtualColumns);
    }

    /**
     * Load Constructor.
     *
     * @param ctx Context
     * @param rs ResultSet
     * @param trxName Transaction name
     */
    public MAIGBudget(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Get budget by ID (cached).
     *
     * @param ctx Context
     * @param AIG_Budget_ID Budget ID
     * @param trxName Transaction name
     * @return Budget or null
     */
    public static MAIGBudget get(Properties ctx, int AIG_Budget_ID, String trxName) {
        if (AIG_Budget_ID <= 0) {
            return null;
        }

        MAIGBudget budget = s_cache.get(AIG_Budget_ID);
        if (budget != null) {
            return budget;
        }

        budget = new MAIGBudget(ctx, AIG_Budget_ID, trxName);
        if (budget.getAIG_Budget_ID() == AIG_Budget_ID) {
            s_cache.put(AIG_Budget_ID, budget);
            return budget;
        }

        return null;
    }

    /**
     * Get client-level budget (cached).
     *
     * @param ctx Context
     * @param clientId Client ID
     * @param trxName Transaction name
     * @return Client budget or null
     */
    public static MAIGBudget getForClient(Properties ctx, int clientId, String trxName) {
        MAIGBudget budget = s_clientCache.get(clientId);
        if (budget != null) {
            return budget;
        }

        budget = new Query(ctx, Table_Name,
                COLUMNNAME_BudgetScope + "=? AND AD_Client_ID=? AND IsActive='Y'", trxName)
            .setParameters(SCOPE_CLIENT, clientId)
            .first();

        if (budget != null) {
            s_clientCache.put(clientId, budget);
        }

        return budget;
    }

    /**
     * Get user-level budget.
     *
     * @param ctx Context
     * @param userId User ID
     * @param trxName Transaction name
     * @return User budget or null
     */
    public static MAIGBudget getForUser(Properties ctx, int userId, String trxName) {
        return new Query(ctx, Table_Name,
                COLUMNNAME_BudgetScope + "=? AND " + COLUMNNAME_AD_User_ID + "=? AND IsActive='Y'", trxName)
            .setClient_ID()
            .setParameters(SCOPE_USER, userId)
            .first();
    }

    /**
     * Get agent-level budget.
     *
     * @param ctx Context
     * @param agentName Agent name
     * @param trxName Transaction name
     * @return Agent budget or null
     */
    public static MAIGBudget getForAgent(Properties ctx, String agentName, String trxName) {
        if (agentName == null || agentName.isEmpty()) {
            return null;
        }

        return new Query(ctx, Table_Name,
                COLUMNNAME_BudgetScope + "=? AND " + COLUMNNAME_AgentName + "=? AND IsActive='Y'", trxName)
            .setClient_ID()
            .setParameters(SCOPE_AGENT, agentName)
            .first();
    }

    /**
     * Get effective budget for a request.
     *
     * <p>Checks in order: Agent → User → Client, returns first found.
     *
     * @param ctx Context
     * @param userId User ID
     * @param agentName Agent name (may be null)
     * @param trxName Transaction name
     * @return Most specific budget or null
     */
    public static MAIGBudget getEffective(Properties ctx, int userId, String agentName, String trxName) {
        // Try agent budget first
        if (agentName != null && !agentName.isEmpty()) {
            MAIGBudget agentBudget = getForAgent(ctx, agentName, trxName);
            if (agentBudget != null) {
                return agentBudget;
            }
        }

        // Try user budget
        MAIGBudget userBudget = getForUser(ctx, userId, trxName);
        if (userBudget != null) {
            return userBudget;
        }

        // Fall back to client budget
        int clientId = org.compiere.util.Env.getAD_Client_ID(ctx);
        return getForClient(ctx, clientId, trxName);
    }

    /**
     * Get all budgets for a client.
     *
     * @param ctx Context
     * @param trxName Transaction name
     * @return List of budgets
     */
    public static List<MAIGBudget> getAll(Properties ctx, String trxName) {
        return new Query(ctx, Table_Name, "IsActive='Y'", trxName)
            .setClient_ID()
            .setOrderBy(COLUMNNAME_BudgetScope + ", " + COLUMNNAME_AgentName)
            .list();
    }

    /**
     * Create a new client-level budget.
     *
     * @param ctx Context
     * @param dailyLimitCents Daily limit in cents
     * @param monthlyLimitCents Monthly limit in cents
     * @param trxName Transaction name
     * @return New budget (saved)
     */
    public static MAIGBudget createClientBudget(Properties ctx, int dailyLimitCents,
            int monthlyLimitCents, String trxName) {

        MAIGBudget budget = new MAIGBudget(ctx, 0, trxName);
        budget.setBudgetScope(SCOPE_CLIENT);
        budget.setDailyLimitUSD(dailyLimitCents);
        budget.setMonthlyLimitUSD(monthlyLimitCents);
        budget.setTokenLimitPerRequest(DEFAULT_TOKEN_LIMIT);
        budget.setRequestsPerMinute(DEFAULT_REQUESTS_PER_MINUTE);
        budget.setCurrentDailyUSD(0);
        budget.setCurrentMonthlyUSD(0);

        if (!budget.save()) {
            log.warning("Failed to create client budget");
            return null;
        }

        // Update cache
        s_clientCache.put(budget.getAD_Client_ID(), budget);
        return budget;
    }

    /**
     * Create a new user-level budget.
     *
     * @param ctx Context
     * @param userId User ID
     * @param dailyLimitCents Daily limit in cents
     * @param monthlyLimitCents Monthly limit in cents
     * @param trxName Transaction name
     * @return New budget (saved)
     */
    public static MAIGBudget createUserBudget(Properties ctx, int userId,
            int dailyLimitCents, int monthlyLimitCents, String trxName) {

        MAIGBudget budget = new MAIGBudget(ctx, 0, trxName);
        budget.setBudgetScope(SCOPE_USER);
        budget.setAD_User_ID(userId);
        budget.setDailyLimitUSD(dailyLimitCents);
        budget.setMonthlyLimitUSD(monthlyLimitCents);
        budget.setTokenLimitPerRequest(DEFAULT_TOKEN_LIMIT);
        budget.setRequestsPerMinute(DEFAULT_REQUESTS_PER_MINUTE);
        budget.setCurrentDailyUSD(0);
        budget.setCurrentMonthlyUSD(0);

        if (!budget.save()) {
            log.warning("Failed to create user budget for user=" + userId);
            return null;
        }

        return budget;
    }

    /**
     * Create a new agent-level budget.
     *
     * @param ctx Context
     * @param agentName Agent name
     * @param dailyLimitCents Daily limit in cents
     * @param monthlyLimitCents Monthly limit in cents
     * @param trxName Transaction name
     * @return New budget (saved)
     */
    public static MAIGBudget createAgentBudget(Properties ctx, String agentName,
            int dailyLimitCents, int monthlyLimitCents, String trxName) {

        MAIGBudget budget = new MAIGBudget(ctx, 0, trxName);
        budget.setBudgetScope(SCOPE_AGENT);
        budget.setAgentName(agentName);
        budget.setDailyLimitUSD(dailyLimitCents);
        budget.setMonthlyLimitUSD(monthlyLimitCents);
        budget.setTokenLimitPerRequest(DEFAULT_TOKEN_LIMIT);
        budget.setRequestsPerMinute(DEFAULT_REQUESTS_PER_MINUTE);
        budget.setCurrentDailyUSD(0);
        budget.setCurrentMonthlyUSD(0);

        if (!budget.save()) {
            log.warning("Failed to create agent budget for agent=" + agentName);
            return null;
        }

        return budget;
    }

    // ========================================================================
    // Budget Check Methods
    // ========================================================================

    /**
     * Check if daily budget is exceeded.
     *
     * @return true if daily budget exceeded
     */
    public boolean isDailyBudgetExceeded() {
        int limit = getDailyLimitUSD();
        if (limit <= 0) {
            return false; // No limit set
        }
        return getCurrentDailyUSD() >= limit;
    }

    /**
     * Check if monthly budget is exceeded.
     *
     * @return true if monthly budget exceeded
     */
    public boolean isMonthlyBudgetExceeded() {
        int limit = getMonthlyLimitUSD();
        if (limit <= 0) {
            return false; // No limit set
        }
        return getCurrentMonthlyUSD() >= limit;
    }

    /**
     * Check if adding cost would exceed daily budget.
     *
     * @param additionalCostCents Additional cost in cents
     * @return true if would exceed
     */
    public boolean wouldExceedDailyBudget(int additionalCostCents) {
        int limit = getDailyLimitUSD();
        if (limit <= 0) {
            return false; // No limit set
        }
        return (getCurrentDailyUSD() + additionalCostCents) > limit;
    }

    /**
     * Check if adding cost would exceed monthly budget.
     *
     * @param additionalCostCents Additional cost in cents
     * @return true if would exceed
     */
    public boolean wouldExceedMonthlyBudget(int additionalCostCents) {
        int limit = getMonthlyLimitUSD();
        if (limit <= 0) {
            return false; // No limit set
        }
        return (getCurrentMonthlyUSD() + additionalCostCents) > limit;
    }

    /**
     * Get daily usage percentage.
     *
     * @return Percentage (0-100+)
     */
    public double getDailyUsagePercent() {
        int limit = getDailyLimitUSD();
        if (limit <= 0) {
            return 0;
        }
        return (getCurrentDailyUSD() * 100.0) / limit;
    }

    /**
     * Get monthly usage percentage.
     *
     * @return Percentage (0-100+)
     */
    public double getMonthlyUsagePercent() {
        int limit = getMonthlyLimitUSD();
        if (limit <= 0) {
            return 0;
        }
        return (getCurrentMonthlyUSD() * 100.0) / limit;
    }

    /**
     * Get remaining daily budget in cents.
     *
     * @return Remaining cents (may be negative if exceeded)
     */
    public int getRemainingDailyCents() {
        int limit = getDailyLimitUSD();
        if (limit <= 0) {
            return Integer.MAX_VALUE; // Unlimited
        }
        return limit - getCurrentDailyUSD();
    }

    /**
     * Get remaining monthly budget in cents.
     *
     * @return Remaining cents (may be negative if exceeded)
     */
    public int getRemainingMonthlyCents() {
        int limit = getMonthlyLimitUSD();
        if (limit <= 0) {
            return Integer.MAX_VALUE; // Unlimited
        }
        return limit - getCurrentMonthlyUSD();
    }

    // ========================================================================
    // Usage Update Methods
    // ========================================================================

    /**
     * Add usage to current totals.
     *
     * @param costCents Cost in cents to add
     * @return true if saved successfully
     */
    public boolean addUsage(int costCents) {
        maybeResetCounters();

        setCurrentDailyUSD(getCurrentDailyUSD() + costCents);
        setCurrentMonthlyUSD(getCurrentMonthlyUSD() + costCents);

        return save();
    }

    /**
     * Reset daily counter if needed.
     */
    public void maybeResetCounters() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Check if daily reset needed
        Timestamp lastDaily = getLastResetDaily();
        if (lastDaily == null || !isSameDay(lastDaily, now)) {
            setCurrentDailyUSD(0);
            setLastResetDaily(now);
        }

        // Check if monthly reset needed
        Timestamp lastMonthly = getLastResetMonthly();
        if (lastMonthly == null || !isSameMonth(lastMonthly, now)) {
            setCurrentMonthlyUSD(0);
            setLastResetMonthly(now);
        }
    }

    /**
     * Force reset all counters.
     *
     * @return true if saved successfully
     */
    public boolean resetCounters() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        setCurrentDailyUSD(0);
        setCurrentMonthlyUSD(0);
        setLastResetDaily(now);
        setLastResetMonthly(now);
        return save();
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Get daily limit as BigDecimal (dollars).
     *
     * @return Daily limit in dollars
     */
    public BigDecimal getDailyLimitAsBigDecimal() {
        return BigDecimal.valueOf(getDailyLimitUSD()).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Get monthly limit as BigDecimal (dollars).
     *
     * @return Monthly limit in dollars
     */
    public BigDecimal getMonthlyLimitAsBigDecimal() {
        return BigDecimal.valueOf(getMonthlyLimitUSD()).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Get current daily usage as BigDecimal (dollars).
     *
     * @return Current daily usage in dollars
     */
    public BigDecimal getCurrentDailyAsBigDecimal() {
        return BigDecimal.valueOf(getCurrentDailyUSD()).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Get current monthly usage as BigDecimal (dollars).
     *
     * @return Current monthly usage in dollars
     */
    public BigDecimal getCurrentMonthlyAsBigDecimal() {
        return BigDecimal.valueOf(getCurrentMonthlyUSD()).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Check if two timestamps are on the same day.
     */
    @SuppressWarnings("deprecation")
    private boolean isSameDay(Timestamp t1, Timestamp t2) {
        return t1.getYear() == t2.getYear() &&
               t1.getMonth() == t2.getMonth() &&
               t1.getDate() == t2.getDate();
    }

    /**
     * Check if two timestamps are in the same month.
     */
    @SuppressWarnings("deprecation")
    private boolean isSameMonth(Timestamp t1, Timestamp t2) {
        return t1.getYear() == t2.getYear() &&
               t1.getMonth() == t2.getMonth();
    }

    /**
     * Clear all budget caches.
     */
    public static void clearCache() {
        s_cache.reset();
        s_clientCache.reset();
    }

    @Override
    public String toString() {
        return "MAIGBudget[" + getAIG_Budget_ID() +
            ", scope=" + getBudgetScope() +
            ", daily=" + getDailyLimitUSD() + "¢ (used " + getCurrentDailyUSD() + "¢)" +
            ", monthly=" + getMonthlyLimitUSD() + "¢ (used " + getCurrentMonthlyUSD() + "¢)]";
    }
}
