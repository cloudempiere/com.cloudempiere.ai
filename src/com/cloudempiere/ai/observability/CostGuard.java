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
package com.cloudempiere.ai.observability;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.cloudempiere.ai.model.MAIBudget;

/**
 * Cost Guard for AI budget enforcement (ADR-013).
 *
 * <p>Enforces cost and rate limits before LLM calls:
 * <ul>
 *   <li>Daily budget limits per client</li>
 *   <li>Monthly budget limits per client</li>
 *   <li>Request rate limiting per user/session</li>
 *   <li>Token limits per request</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * CostGuard guard = new CostGuard();
 * guard.checkBudget(clientId, estimatedCost);  // throws BudgetExceededException
 * guard.checkRateLimit(userId);                // throws RateLimitExceededException
 * </pre>
 *
 * <p><b>Tags:</b> #budget #cost-control #rate-limiting #guardrails #multi-tenant
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see AIMetricsListener
 * @see com.cloudempiere.ai.guardrails.ExecutionGuard
 */
public class CostGuard {

    private static final CLogger log = CLogger.getCLogger(CostGuard.class);

    // ========================================================================
    // Default Limits (can be overridden per client via AIG_Budget table)
    // ========================================================================

    /** Default daily budget per client (USD) */
    public static final BigDecimal DEFAULT_DAILY_BUDGET = new BigDecimal("50.00");

    /** Default monthly budget per client (USD) */
    public static final BigDecimal DEFAULT_MONTHLY_BUDGET = new BigDecimal("500.00");

    /** Default max requests per minute per user */
    public static final int DEFAULT_REQUESTS_PER_MINUTE = 20;

    /** Default max tokens per request */
    public static final int DEFAULT_MAX_TOKENS_PER_REQUEST = 4000;

    /** Alert threshold (percentage of budget) */
    public static final double ALERT_THRESHOLD = 0.8; // 80%

    // ========================================================================
    // Rate Limiting State
    // ========================================================================

    /** Request counts per user (userId -> count this minute) */
    private final Map<Integer, AtomicInteger> userRequestCounts = new ConcurrentHashMap<>();

    /** Last reset time for rate limiting */
    private volatile long lastRateLimitReset = System.currentTimeMillis();

    /** Cached budget limits per client */
    private final Map<Integer, BudgetLimits> clientBudgets = new ConcurrentHashMap<>();

    /**
     * Check if budget allows the estimated cost.
     *
     * @param clientId Client ID
     * @param estimatedCost Estimated cost for this request (USD)
     * @throws BudgetExceededException if budget would be exceeded
     */
    public void checkBudget(int clientId, BigDecimal estimatedCost)
            throws BudgetExceededException {

        BudgetLimits limits = getBudgetLimits(clientId);

        // Check daily budget
        BigDecimal todayCost = AIMetricsListener.getTodayCost(clientId);
        BigDecimal projectedDaily = todayCost.add(estimatedCost);

        if (projectedDaily.compareTo(limits.dailyBudget) > 0) {
            log.warning("Daily budget exceeded for client " + clientId +
                       ": projected $" + projectedDaily + " > limit $" + limits.dailyBudget);
            throw new BudgetExceededException(
                "Daily AI budget exceeded. Used: $" + todayCost +
                ", Limit: $" + limits.dailyBudget,
                BudgetType.DAILY
            );
        }

        // Check monthly budget
        BigDecimal monthCost = getMonthCost(clientId);
        BigDecimal projectedMonthly = monthCost.add(estimatedCost);

        if (projectedMonthly.compareTo(limits.monthlyBudget) > 0) {
            log.warning("Monthly budget exceeded for client " + clientId +
                       ": projected $" + projectedMonthly + " > limit $" + limits.monthlyBudget);
            throw new BudgetExceededException(
                "Monthly AI budget exceeded. Used: $" + monthCost +
                ", Limit: $" + limits.monthlyBudget,
                BudgetType.MONTHLY
            );
        }

        // Alert if approaching limit
        double dailyPercent = projectedDaily.doubleValue() / limits.dailyBudget.doubleValue();
        if (dailyPercent >= ALERT_THRESHOLD) {
            log.warning("Daily budget alert for client " + clientId +
                       ": " + String.format("%.1f%%", dailyPercent * 100) + " used");
        }
    }

    /**
     * Check rate limit for a user.
     *
     * @param userId User ID
     * @throws RateLimitExceededException if rate limit exceeded
     */
    public void checkRateLimit(int userId) throws RateLimitExceededException {
        maybeResetRateLimits();

        AtomicInteger count = userRequestCounts.computeIfAbsent(
            userId, k -> new AtomicInteger(0)
        );

        int currentCount = count.incrementAndGet();

        if (currentCount > DEFAULT_REQUESTS_PER_MINUTE) {
            log.warning("Rate limit exceeded for user " + userId +
                       ": " + currentCount + " requests/minute");
            throw new RateLimitExceededException(
                "AI request rate limit exceeded. Please wait before making more requests.",
                DEFAULT_REQUESTS_PER_MINUTE
            );
        }
    }

    /**
     * Check token limit for a request.
     *
     * @param clientId Client ID
     * @param estimatedTokens Estimated tokens for this request
     * @throws TokenLimitExceededException if token limit exceeded
     */
    public void checkTokenLimit(int clientId, int estimatedTokens)
            throws TokenLimitExceededException {

        BudgetLimits limits = getBudgetLimits(clientId);

        if (estimatedTokens > limits.maxTokensPerRequest) {
            log.warning("Token limit exceeded: " + estimatedTokens +
                       " > " + limits.maxTokensPerRequest);
            throw new TokenLimitExceededException(
                "Request too large. Estimated tokens: " + estimatedTokens +
                ", Limit: " + limits.maxTokensPerRequest,
                limits.maxTokensPerRequest
            );
        }
    }

    /**
     * Perform all pre-request checks.
     *
     * @param clientId Client ID
     * @param userId User ID
     * @param estimatedCost Estimated cost (USD)
     * @param estimatedTokens Estimated tokens
     * @throws BudgetExceededException if budget exceeded
     * @throws RateLimitExceededException if rate limit exceeded
     * @throws TokenLimitExceededException if token limit exceeded
     */
    public void checkAll(int clientId, int userId,
                         BigDecimal estimatedCost, int estimatedTokens)
            throws BudgetExceededException, RateLimitExceededException,
                   TokenLimitExceededException {

        checkBudget(clientId, estimatedCost);
        checkRateLimit(userId);
        checkTokenLimit(clientId, estimatedTokens);
    }

    /**
     * Get budget status for a client.
     *
     * @param clientId Client ID
     * @return Budget status information
     */
    public BudgetStatus getBudgetStatus(int clientId) {
        BudgetLimits limits = getBudgetLimits(clientId);
        BigDecimal todayCost = AIMetricsListener.getTodayCost(clientId);
        BigDecimal monthCost = getMonthCost(clientId);
        int todayRequests = AIMetricsListener.getTodayRequestCount(clientId);

        return new BudgetStatus(
            todayCost,
            limits.dailyBudget,
            monthCost,
            limits.monthlyBudget,
            todayRequests
        );
    }

    /**
     * Get budget limits for a client (from database or defaults).
     *
     * @param clientId Client ID
     * @return Budget limits
     */
    private BudgetLimits getBudgetLimits(int clientId) {
        return clientBudgets.computeIfAbsent(clientId, this::loadBudgetLimits);
    }

    /**
     * Load budget limits from database using MAIBudget model.
     *
     * @param clientId Client ID
     * @return Budget limits (defaults if not configured)
     */
    private BudgetLimits loadBudgetLimits(int clientId) {
        // Try to load from AIG_Budget table using model
        try {
            Properties ctx = Env.getCtx();
            Env.setContext(ctx, "#AD_Client_ID", clientId);

            MAIBudget budget = MAIBudget.getForClient(ctx, clientId, null);

            if (budget != null) {
                // Convert cents to dollars for BigDecimal limits
                BigDecimal daily = budget.getDailyLimitAsBigDecimal();
                BigDecimal monthly = budget.getMonthlyLimitAsBigDecimal();
                int maxTokens = budget.getTokenLimitPerRequest();

                return new BudgetLimits(
                    daily,
                    monthly,
                    maxTokens > 0 ? maxTokens : DEFAULT_MAX_TOKENS_PER_REQUEST
                );
            }
        } catch (Exception e) {
            // Table might not exist yet
            log.fine("AIG_Budget table not available, using defaults: " + e.getMessage());
        }

        // Return defaults
        return new BudgetLimits(
            DEFAULT_DAILY_BUDGET,
            DEFAULT_MONTHLY_BUDGET,
            DEFAULT_MAX_TOKENS_PER_REQUEST
        );
    }

    /**
     * Get month-to-date cost for a client.
     *
     * @param clientId Client ID
     * @return Month-to-date cost in USD
     */
    private BigDecimal getMonthCost(int clientId) {
        String sql = "SELECT COALESCE(SUM(CostUSD), 0) FROM AIG_UsageMetrics " +
                    "WHERE AD_Client_ID = ? " +
                    "AND Created >= DATE_TRUNC('month', CURRENT_DATE)";

        try {
            return DB.getSQLValueBD(null, sql, clientId);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Reset rate limits if a minute has passed.
     */
    private void maybeResetRateLimits() {
        long now = System.currentTimeMillis();
        if (now - lastRateLimitReset >= 60_000) { // 1 minute
            synchronized (this) {
                if (now - lastRateLimitReset >= 60_000) {
                    userRequestCounts.clear();
                    lastRateLimitReset = now;
                }
            }
        }
    }

    /**
     * Clear cached budget limits (call when configuration changes).
     */
    public void clearBudgetCache() {
        clientBudgets.clear();
    }

    /**
     * Clear cached budget for a specific client.
     *
     * @param clientId Client ID
     */
    public void clearBudgetCache(int clientId) {
        clientBudgets.remove(clientId);
    }

    // ========================================================================
    // Inner Classes
    // ========================================================================

    /**
     * Budget limits configuration.
     */
    public static class BudgetLimits {
        public final BigDecimal dailyBudget;
        public final BigDecimal monthlyBudget;
        public final int maxTokensPerRequest;

        public BudgetLimits(BigDecimal dailyBudget, BigDecimal monthlyBudget,
                          int maxTokensPerRequest) {
            this.dailyBudget = dailyBudget;
            this.monthlyBudget = monthlyBudget;
            this.maxTokensPerRequest = maxTokensPerRequest;
        }
    }

    /**
     * Current budget status.
     */
    public static class BudgetStatus {
        public final BigDecimal dailyUsed;
        public final BigDecimal dailyLimit;
        public final BigDecimal monthlyUsed;
        public final BigDecimal monthlyLimit;
        public final int todayRequests;
        public final double dailyPercentUsed;
        public final double monthlyPercentUsed;

        public BudgetStatus(BigDecimal dailyUsed, BigDecimal dailyLimit,
                           BigDecimal monthlyUsed, BigDecimal monthlyLimit,
                           int todayRequests) {
            this.dailyUsed = dailyUsed;
            this.dailyLimit = dailyLimit;
            this.monthlyUsed = monthlyUsed;
            this.monthlyLimit = monthlyLimit;
            this.todayRequests = todayRequests;
            this.dailyPercentUsed = dailyUsed.doubleValue() / dailyLimit.doubleValue() * 100;
            this.monthlyPercentUsed = monthlyUsed.doubleValue() / monthlyLimit.doubleValue() * 100;
        }

        @Override
        public String toString() {
            return String.format(
                "BudgetStatus[daily=$%.2f/$%.2f (%.1f%%), monthly=$%.2f/$%.2f (%.1f%%), requests=%d]",
                dailyUsed, dailyLimit, dailyPercentUsed,
                monthlyUsed, monthlyLimit, monthlyPercentUsed,
                todayRequests
            );
        }
    }

    /**
     * Budget type for exceptions.
     */
    public enum BudgetType {
        DAILY, MONTHLY
    }

    // ========================================================================
    // Exceptions
    // ========================================================================

    /**
     * Exception thrown when budget is exceeded.
     */
    public static class BudgetExceededException extends Exception {
        private final BudgetType budgetType;

        public BudgetExceededException(String message, BudgetType budgetType) {
            super(message);
            this.budgetType = budgetType;
        }

        public BudgetType getBudgetType() {
            return budgetType;
        }
    }

    /**
     * Exception thrown when rate limit is exceeded.
     */
    public static class RateLimitExceededException extends Exception {
        private final int limitPerMinute;

        public RateLimitExceededException(String message, int limitPerMinute) {
            super(message);
            this.limitPerMinute = limitPerMinute;
        }

        public int getLimitPerMinute() {
            return limitPerMinute;
        }
    }

    /**
     * Exception thrown when token limit is exceeded.
     */
    public static class TokenLimitExceededException extends Exception {
        private final int maxTokens;

        public TokenLimitExceededException(String message, int maxTokens) {
            super(message);
            this.maxTokens = maxTokens;
        }

        public int getMaxTokens() {
            return maxTokens;
        }
    }
}
