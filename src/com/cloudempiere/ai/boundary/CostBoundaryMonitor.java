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
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.agent.AgentContext;

/**
 * Cost Boundary Monitor for AI agents (ADR-009, ADR-013).
 *
 * <p>Tracks and enforces cost and rate limits for AI agent operations.
 * Prevents runaway costs and ensures fair resource usage.
 *
 * <p>Monitors:
 * <ul>
 *   <li>Cost per request (USD)</li>
 *   <li>Cost per day (USD)</li>
 *   <li>Tool calls per request</li>
 *   <li>Tool calls per minute (rate limiting)</li>
 *   <li>Token usage</li>
 * </ul>
 *
 * @author Cloudempiere AI Team
 * @version ADR-009
 * @since v0.10.0
 */
public class CostBoundaryMonitor {

    private static final CLogger log = CLogger.getCLogger(CostBoundaryMonitor.class);

    /** Daily cost tracking by client ID */
    private static final Map<Integer, DailyCostTracker> dailyCosts = new ConcurrentHashMap<>();

    /** Rate limiting by session ID */
    private static final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    /** Alert threshold (percentage of limit) */
    private static final double ALERT_THRESHOLD = 0.8; // 80%

    /**
     * Check if cost limit would be exceeded by additional cost.
     *
     * @param context Agent context
     * @param boundary Agent boundary
     * @param additionalCost Cost to add (USD)
     * @throws BoundaryViolationException if limit exceeded
     */
    public void checkCostLimit(AgentContext context, AgentBoundary boundary,
                               BigDecimal additionalCost)
            throws BoundaryViolationException {

        // Check request-level cost
        BigDecimal currentCost = new BigDecimal(context.getCurrentCostUSD());
        BigDecimal totalCost = currentCost.add(additionalCost);

        if (totalCost.compareTo(boundary.getMaxCostPerRequest()) > 0) {
            log.warning("Request cost limit exceeded: " + totalCost +
                       " > " + boundary.getMaxCostPerRequest());
            throw new BoundaryViolationException(
                "Request cost limit exceeded: $" + totalCost +
                " (max: $" + boundary.getMaxCostPerRequest() + ")",
                BoundaryViolationType.COST_LIMIT_EXCEEDED
            );
        }

        // Check daily cost
        DailyCostTracker tracker = getDailyCostTracker(context.getClientId());
        BigDecimal dailyCost = tracker.getTodayCost().add(additionalCost);

        if (dailyCost.compareTo(boundary.getMaxCostPerDay()) > 0) {
            log.warning("Daily cost limit exceeded for client " + context.getClientId() +
                       ": " + dailyCost + " > " + boundary.getMaxCostPerDay());
            throw new BoundaryViolationException(
                "Daily cost limit exceeded: $" + dailyCost +
                " (max: $" + boundary.getMaxCostPerDay() + ")",
                BoundaryViolationType.COST_LIMIT_EXCEEDED
            );
        }

        // Alert if approaching limit
        double dailyPercentage = dailyCost.doubleValue() /
                                 boundary.getMaxCostPerDay().doubleValue();
        if (dailyPercentage >= ALERT_THRESHOLD) {
            log.warning("Daily cost alert: " + String.format("%.1f%%", dailyPercentage * 100) +
                       " of daily limit used (client " + context.getClientId() + ")");
        }
    }

    /**
     * Check if tool call limit would be exceeded.
     *
     * @param context Agent context
     * @param boundary Agent boundary
     * @throws BoundaryViolationException if limit exceeded
     */
    public void checkToolCallLimit(AgentContext context, AgentBoundary boundary)
            throws BoundaryViolationException {

        // Check request-level limit
        int currentCalls = context.getCurrentToolCalls();
        if (currentCalls >= boundary.getMaxToolCallsPerRequest()) {
            log.warning("Tool call limit exceeded: " + currentCalls +
                       " >= " + boundary.getMaxToolCallsPerRequest());
            throw new BoundaryViolationException(
                "Tool call limit exceeded: " + currentCalls +
                " (max: " + boundary.getMaxToolCallsPerRequest() + ")",
                BoundaryViolationType.RATE_LIMIT_EXCEEDED
            );
        }

        // Check rate limit (per minute)
        RateLimiter limiter = getRateLimiter(context.getSessionId());
        if (!limiter.tryAcquire(boundary.getMaxToolCallsPerMinute())) {
            log.warning("Rate limit exceeded for session: " + context.getSessionId());
            throw new BoundaryViolationException(
                "Rate limit exceeded: " + boundary.getMaxToolCallsPerMinute() +
                " calls/minute",
                BoundaryViolationType.RATE_LIMIT_EXCEEDED
            );
        }
    }

    /**
     * Check if token limit would be exceeded.
     *
     * @param context Agent context
     * @param boundary Agent boundary
     * @param tokenCount Tokens to add
     * @throws BoundaryViolationException if limit exceeded
     */
    public void checkTokenLimit(AgentContext context, AgentBoundary boundary,
                                int tokenCount)
            throws BoundaryViolationException {

        // Token tracking would typically be in context
        // For now, just check against boundary
        if (tokenCount > boundary.getMaxTokensPerRequest()) {
            log.warning("Token limit exceeded: " + tokenCount +
                       " > " + boundary.getMaxTokensPerRequest());
            throw new BoundaryViolationException(
                "Token limit exceeded: " + tokenCount +
                " (max: " + boundary.getMaxTokensPerRequest() + ")",
                BoundaryViolationType.TOKEN_LIMIT_EXCEEDED
            );
        }
    }

    /**
     * Record cost for tracking.
     *
     * @param context Agent context
     * @param cost Cost to record (USD)
     */
    public void recordCost(AgentContext context, BigDecimal cost) {
        // Update context
        context.addCost(cost.doubleValue());

        // Update daily tracker
        DailyCostTracker tracker = getDailyCostTracker(context.getClientId());
        tracker.addCost(cost);

        log.fine("Cost recorded: $" + cost + " (session=" + context.getSessionId() +
                ", daily total=$" + tracker.getTodayCost() + ")");
    }

    /**
     * Record tool call for rate limiting.
     *
     * @param context Agent context
     */
    public void recordToolCall(AgentContext context) {
        context.incrementToolCalls();
    }

    /**
     * Get daily cost tracker for a client.
     *
     * @param clientId Client ID
     * @return Daily cost tracker
     */
    private DailyCostTracker getDailyCostTracker(int clientId) {
        return dailyCosts.computeIfAbsent(clientId, k -> new DailyCostTracker());
    }

    /**
     * Get rate limiter for a session.
     *
     * @param sessionId Session ID
     * @return Rate limiter
     */
    private RateLimiter getRateLimiter(String sessionId) {
        return rateLimiters.computeIfAbsent(sessionId, k -> new RateLimiter());
    }

    /**
     * Get current daily cost for a client.
     *
     * @param clientId Client ID
     * @return Today's cost (USD)
     */
    public BigDecimal getDailyCost(int clientId) {
        DailyCostTracker tracker = dailyCosts.get(clientId);
        return tracker != null ? tracker.getTodayCost() : BigDecimal.ZERO;
    }

    /**
     * Reset daily costs (call at midnight).
     */
    public void resetDailyCosts() {
        dailyCosts.values().forEach(DailyCostTracker::reset);
        log.info("Daily cost trackers reset");
    }

    /**
     * Clean up expired rate limiters.
     */
    public void cleanupRateLimiters() {
        // Remove rate limiters that haven't been used recently
        // In production, this would be called periodically
        rateLimiters.entrySet().removeIf(e -> e.getValue().isExpired());
        log.fine("Cleaned up expired rate limiters");
    }

    /**
     * Get statistics for monitoring.
     *
     * @return Map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("trackedClients", dailyCosts.size());
        stats.put("activeRateLimiters", rateLimiters.size());

        BigDecimal totalDailyCost = dailyCosts.values().stream()
            .map(DailyCostTracker::getTodayCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalDailyCost", totalDailyCost);

        return stats;
    }

    /**
     * Daily cost tracker per client.
     */
    private static class DailyCostTracker {
        private volatile LocalDate date = LocalDate.now();
        private volatile BigDecimal cost = BigDecimal.ZERO;

        synchronized void addCost(BigDecimal amount) {
            checkDate();
            cost = cost.add(amount);
        }

        synchronized BigDecimal getTodayCost() {
            checkDate();
            return cost;
        }

        synchronized void reset() {
            date = LocalDate.now();
            cost = BigDecimal.ZERO;
        }

        private void checkDate() {
            if (!LocalDate.now().equals(date)) {
                reset();
            }
        }
    }

    /**
     * Simple rate limiter for tool calls.
     */
    private static class RateLimiter {
        private final AtomicInteger callsThisMinute = new AtomicInteger(0);
        private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
        private final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());

        /** Expiration time for cleanup (5 minutes) */
        private static final long EXPIRATION_MS = 5 * 60 * 1000;

        boolean tryAcquire(int maxPerMinute) {
            lastAccessTime.set(System.currentTimeMillis());
            maybeReset();

            int current = callsThisMinute.incrementAndGet();
            return current <= maxPerMinute;
        }

        private void maybeReset() {
            long now = System.currentTimeMillis();
            long lastReset = lastResetTime.get();
            if (now - lastReset >= 60_000) { // 1 minute
                if (lastResetTime.compareAndSet(lastReset, now)) {
                    callsThisMinute.set(0);
                }
            }
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime.get() > EXPIRATION_MS;
        }
    }
}
