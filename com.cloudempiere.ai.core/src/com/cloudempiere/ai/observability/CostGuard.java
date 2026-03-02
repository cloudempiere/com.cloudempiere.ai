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

    /**
     * Check if budget allows the estimated cost.
     *
     * <p>Resolves the effective AIG_Budget record for the user (agent → user → client scope)
     * and checks the stored CurrentDailyAmt / CurrentMonthlyAmt counters directly.
     *
     * @param clientId Client ID
     * @param userId User ID
     * @param estimatedCost Estimated cost for this request (USD)
     * @throws BudgetExceededException if budget would be exceeded
     */
    public void checkBudget(int clientId, int userId, BigDecimal estimatedCost)
            throws BudgetExceededException {

        Properties ctx = Env.getCtx();
        MAIBudget budget = MAIBudget.getEffective(ctx, userId, null, null);

        if (budget == null) {
            return; // No budget configured for this user/client
        }

        // Run lazy reset before checking — counters are normally reset inside addUsage(),
        // but if the guard blocks the request, addUsage() is never called and the stale
        // counter would permanently block the user after a day/month boundary.
        budget.maybeResetCounters();
        if (budget.is_Changed()) {
            budget.save();
        }

        // Check daily budget against current usage (not projected)
        // Actual cost is tracked post-request via addUsage(); pre-flight only blocks when limit is reached
        if (budget.isDailyBudgetExceeded()) {
            log.warning("Daily budget exceeded for user " + userId +
                       ": used=" + budget.getCurrentDailyAmt() + "¢, limit=" + budget.getDailyLimit() + "¢");
            throw new BudgetExceededException(
                "Daily AI budget exceeded. Used: " + budget.getCurrentDailyAsBigDecimal() +
                " USD, Limit: " + budget.getDailyLimitAsBigDecimal() + " USD",
                BudgetType.DAILY
            );
        }

        // Check monthly budget against current usage (not projected)
        if (budget.isMonthlyBudgetExceeded()) {
            log.warning("Monthly budget exceeded for user " + userId +
                       ": used=" + budget.getCurrentMonthlyAmt() + "¢, limit=" + budget.getMonthlyLimit() + "¢");
            throw new BudgetExceededException(
                "Monthly AI budget exceeded. Used: " + budget.getCurrentMonthlyAsBigDecimal() +
                " USD, Limit: " + budget.getMonthlyLimitAsBigDecimal() + " USD",
                BudgetType.MONTHLY
            );
        }

        // Alert if approaching daily limit
        if (budget.getDailyLimit() > 0) {
            double dailyPercent = budget.getDailyUsagePercent() / 100.0;
            if (dailyPercent >= ALERT_THRESHOLD) {
                log.warning("Daily budget alert for user " + userId +
                           ": " + String.format("%.1f%%", dailyPercent * 100) + " used");
            }
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

        // Resolve the effective rate limit from AIG_Budget (falls back to default)
        Properties ctx = Env.getCtx();
        MAIBudget budget = MAIBudget.getEffective(ctx, userId, null, null);
        int maxRequestsPerMinute = budget != null && budget.getRequestsPerMinute() > 0
            ? budget.getRequestsPerMinute()
            : DEFAULT_REQUESTS_PER_MINUTE;

        AtomicInteger count = userRequestCounts.computeIfAbsent(
            userId, k -> new AtomicInteger(0)
        );

        int currentCount = count.incrementAndGet();

        if (currentCount > maxRequestsPerMinute) {
            log.warning("Rate limit exceeded for user " + userId +
                       ": " + currentCount + " requests/minute (limit=" + maxRequestsPerMinute + ")");
            throw new RateLimitExceededException(
                "AI request rate limit exceeded. Please wait before making more requests.",
                maxRequestsPerMinute
            );
        }
    }

    /**
     * Check token limit for a request.
     *
     * @param clientId Client ID
     * @param userId User ID
     * @param estimatedTokens Estimated tokens for this request
     * @throws TokenLimitExceededException if token limit exceeded
     */
    public void checkTokenLimit(int clientId, int userId, int estimatedTokens)
            throws TokenLimitExceededException {

        Properties ctx = Env.getCtx();
        MAIBudget budget = MAIBudget.getEffective(ctx, userId, null, null);

        int maxTokens = budget != null && budget.getTokenLimitPerRequest() > 0
            ? budget.getTokenLimitPerRequest()
            : DEFAULT_MAX_TOKENS_PER_REQUEST;

        if (estimatedTokens > maxTokens) {
            log.warning("Token limit exceeded: " + estimatedTokens + " > " + maxTokens);
            throw new TokenLimitExceededException(
                "Request too large. Estimated tokens: " + estimatedTokens +
                ", Limit: " + maxTokens,
                maxTokens
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

        checkBudget(clientId, userId, estimatedCost);
        checkRateLimit(userId);
        checkTokenLimit(clientId, userId, estimatedTokens);
    }

    /**
     * Get budget status for a client.
     *
     * @param clientId Client ID
     * @return Budget status information
     */
    public BudgetStatus getBudgetStatus(int clientId) {
        Properties ctx = Env.getCtx();
        MAIBudget budget = MAIBudget.getForClient(ctx, clientId, null);

        if (budget == null) {
            return new BudgetStatus(BigDecimal.ZERO, DEFAULT_DAILY_BUDGET,
                BigDecimal.ZERO, DEFAULT_MONTHLY_BUDGET, 0);
        }

        int todayRequests = AIMetricsListener.getTodayRequestCount(clientId);

        return new BudgetStatus(
            budget.getCurrentDailyAsBigDecimal(),
            budget.getDailyLimitAsBigDecimal(),
            budget.getCurrentMonthlyAsBigDecimal(),
            budget.getMonthlyLimitAsBigDecimal(),
            todayRequests
        );
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
    public static void clearBudgetCache() {
        MAIBudget.clearCache();
        log.info("All budget caches cleared");
    }

    /**
     * Clear cached budget for a specific client.
     *
     * @param clientId Client ID
     */
    public static void clearBudgetCache(int clientId) {
        MAIBudget.clearCache();
        log.fine("Budget cache cleared for client " + clientId);
    }

    // ========================================================================
    // Inner Classes
    // ========================================================================

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
        private static final long serialVersionUID = 1L;
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
        private static final long serialVersionUID = 1L;
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
        private static final long serialVersionUID = 1L;
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
