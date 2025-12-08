package com.cloudempiere.ai.provider.dto;

import java.time.Instant;

/**
 * Rate limit status from AI provider response headers.
 *
 * Anthropic returns these headers on every response:
 * - anthropic-ratelimit-requests-limit/remaining/reset
 * - anthropic-ratelimit-tokens-limit/remaining/reset
 * - anthropic-ratelimit-input-tokens-limit/remaining/reset
 * - anthropic-ratelimit-output-tokens-limit/remaining/reset
 *
 * Note: Credit balance is NOT available via API - only in Claude Console.
 * See: https://github.com/anthropics/anthropic-sdk-python/issues/505
 *
 * @author Cloudempiere
 * @version 1.1
 */
public class AIRateLimitStatus {

    // Request limits
    private int remainingRequests;
    private int limitRequests;
    private Instant requestsResetTime;

    // Combined token limits (most restrictive)
    private int remainingTokens;
    private int limitTokens;
    private Instant tokensResetTime;

    // Input token limits (Anthropic-specific)
    private int remainingInputTokens;
    private int limitInputTokens;
    private Instant inputTokensResetTime;

    // Output token limits (Anthropic-specific)
    private int remainingOutputTokens;
    private int limitOutputTokens;
    private Instant outputTokensResetTime;

    // Legacy field for backward compatibility
    private long resetTimeMs;

    // Usage percentages (calculated)
    private double requestsUsagePercent;
    private double inputTokensUsagePercent;
    private double outputTokensUsagePercent;

    // Credit/billing info (when available)
    private String creditStatus;  // "ok", "low", "depleted"
    private String billingTier;   // "tier1", "tier2", etc.

    // ========================================================================
    // Request Limits
    // ========================================================================

    public int getRemainingRequests() { return remainingRequests; }
    public void setRemainingRequests(int remaining) { this.remainingRequests = remaining; }

    public int getLimitRequests() { return limitRequests; }
    public void setLimitRequests(int limit) { this.limitRequests = limit; }

    public Instant getRequestsResetTime() { return requestsResetTime; }
    public void setRequestsResetTime(Instant time) { this.requestsResetTime = time; }

    // ========================================================================
    // Combined Token Limits
    // ========================================================================

    public int getRemainingTokens() { return remainingTokens; }
    public void setRemainingTokens(int remaining) { this.remainingTokens = remaining; }

    public int getLimitTokens() { return limitTokens; }
    public void setLimitTokens(int limit) { this.limitTokens = limit; }

    public Instant getTokensResetTime() { return tokensResetTime; }
    public void setTokensResetTime(Instant time) { this.tokensResetTime = time; }

    // ========================================================================
    // Input Token Limits
    // ========================================================================

    public int getRemainingInputTokens() { return remainingInputTokens; }
    public void setRemainingInputTokens(int remaining) { this.remainingInputTokens = remaining; }

    public int getLimitInputTokens() { return limitInputTokens; }
    public void setLimitInputTokens(int limit) { this.limitInputTokens = limit; }

    public Instant getInputTokensResetTime() { return inputTokensResetTime; }
    public void setInputTokensResetTime(Instant time) { this.inputTokensResetTime = time; }

    // ========================================================================
    // Output Token Limits
    // ========================================================================

    public int getRemainingOutputTokens() { return remainingOutputTokens; }
    public void setRemainingOutputTokens(int remaining) { this.remainingOutputTokens = remaining; }

    public int getLimitOutputTokens() { return limitOutputTokens; }
    public void setLimitOutputTokens(int limit) { this.limitOutputTokens = limit; }

    public Instant getOutputTokensResetTime() { return outputTokensResetTime; }
    public void setOutputTokensResetTime(Instant time) { this.outputTokensResetTime = time; }

    // ========================================================================
    // Legacy/Backward Compatibility
    // ========================================================================

    /** @deprecated Use getTokensResetTime() instead */
    @Deprecated
    public long getResetTimeMs() { return resetTimeMs; }

    /** @deprecated Use setTokensResetTime() instead */
    @Deprecated
    public void setResetTimeMs(long time) { this.resetTimeMs = time; }

    // ========================================================================
    // Usage Percentages
    // ========================================================================

    public double getRequestsUsagePercent() {
        if (limitRequests > 0) {
            return 100.0 * (limitRequests - remainingRequests) / limitRequests;
        }
        return requestsUsagePercent;
    }
    public void setRequestsUsagePercent(double percent) { this.requestsUsagePercent = percent; }

    public double getInputTokensUsagePercent() {
        if (limitInputTokens > 0) {
            return 100.0 * (limitInputTokens - remainingInputTokens) / limitInputTokens;
        }
        return inputTokensUsagePercent;
    }
    public void setInputTokensUsagePercent(double percent) { this.inputTokensUsagePercent = percent; }

    public double getOutputTokensUsagePercent() {
        if (limitOutputTokens > 0) {
            return 100.0 * (limitOutputTokens - remainingOutputTokens) / limitOutputTokens;
        }
        return outputTokensUsagePercent;
    }
    public void setOutputTokensUsagePercent(double percent) { this.outputTokensUsagePercent = percent; }

    // ========================================================================
    // Credit/Billing Status
    // ========================================================================

    public String getCreditStatus() { return creditStatus; }
    public void setCreditStatus(String status) { this.creditStatus = status; }

    public String getBillingTier() { return billingTier; }
    public void setBillingTier(String tier) { this.billingTier = tier; }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Check if any rate limit is approaching (>80% used)
     */
    public boolean isApproachingLimit() {
        return getRequestsUsagePercent() > 80 ||
               getInputTokensUsagePercent() > 80 ||
               getOutputTokensUsagePercent() > 80;
    }

    /**
     * Check if any rate limit is critical (>95% used)
     */
    public boolean isCritical() {
        return getRequestsUsagePercent() > 95 ||
               getInputTokensUsagePercent() > 95 ||
               getOutputTokensUsagePercent() > 95;
    }

    /**
     * Get a summary string for logging/display
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rate Limits: ");

        if (limitRequests > 0) {
            sb.append(String.format("Requests: %d/%d (%.1f%%), ",
                remainingRequests, limitRequests, getRequestsUsagePercent()));
        }

        if (limitInputTokens > 0) {
            sb.append(String.format("Input: %dK/%dK (%.1f%%), ",
                remainingInputTokens / 1000, limitInputTokens / 1000, getInputTokensUsagePercent()));
        }

        if (limitOutputTokens > 0) {
            sb.append(String.format("Output: %dK/%dK (%.1f%%)",
                remainingOutputTokens / 1000, limitOutputTokens / 1000, getOutputTokensUsagePercent()));
        }

        if (creditStatus != null) {
            sb.append(" | Credits: ").append(creditStatus);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getSummary();
    }
}