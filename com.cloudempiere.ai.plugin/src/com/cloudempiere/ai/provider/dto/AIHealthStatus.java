package com.cloudempiere.ai.provider.dto;

/**
 * Health status
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIHealthStatus {
    private boolean healthy;
    private String status; // "UP", "DOWN", "DEGRADED"
    private String message;
    private long responseTimeMs;

    // Getters and setters
    public boolean isHealthy() { return healthy; }
    public void setHealthy(boolean healthy) { this.healthy = healthy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long time) { this.responseTimeMs = time; }
}