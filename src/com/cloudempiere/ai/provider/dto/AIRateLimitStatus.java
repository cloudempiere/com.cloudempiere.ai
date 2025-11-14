package com.cloudempiere.ai.provider.dto;

/**
 * Rate limit status
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIRateLimitStatus {
    private int remainingRequests;
    private int limitRequests;
    private long resetTimeMs;
    private int remainingTokens;
    private int limitTokens;

    // Getters and setters
    public int getRemainingRequests() { return remainingRequests; }
    public void setRemainingRequests(int remaining) { this.remainingRequests = remaining; }

    public int getLimitRequests() { return limitRequests; }
    public void setLimitRequests(int limit) { this.limitRequests = limit; }

    public long getResetTimeMs() { return resetTimeMs; }
    public void setResetTimeMs(long time) { this.resetTimeMs = time; }

    public int getRemainingTokens() { return remainingTokens; }
    public void setRemainingTokens(int remaining) { this.remainingTokens = remaining; }

    public int getLimitTokens() { return limitTokens; }
    public void setLimitTokens(int limit) { this.limitTokens = limit; }
}