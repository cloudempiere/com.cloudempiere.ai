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
package com.cloudempiere.ai.observability.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

/**
 * Usage Metrics DTO for AI observability (ADR-013).
 *
 * <p>Represents a single AI request/response cycle with:
 * <ul>
 *   <li>Token counts (input, output, total)</li>
 *   <li>Cost in USD</li>
 *   <li>Latency in milliseconds</li>
 *   <li>Model and agent information</li>
 *   <li>Success/failure status</li>
 * </ul>
 *
 * <p><b>Tags:</b> #dto #metrics #observability #tokens #latency
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see com.cloudempiere.ai.observability.AIMetricsListener
 */
public class UsageMetrics {

    // ========================================================================
    // Identity
    // ========================================================================

    private int aigUsageMetricsId;
    private int adClientId;
    private int adOrgId;
    private int adUserId;

    // ========================================================================
    // Request Info
    // ========================================================================

    private String agentName;
    private String sessionId;
    private String modelName;

    // ========================================================================
    // Token Usage
    // ========================================================================

    private int inputTokens;
    private int outputTokens;
    private int totalTokens;

    // ========================================================================
    // Cost and Performance
    // ========================================================================

    private BigDecimal costUsd;
    private long latencyMs;

    // ========================================================================
    // Status
    // ========================================================================

    private boolean success;
    private String errorMessage;

    // ========================================================================
    // Timestamps
    // ========================================================================

    private Timestamp created;

    // ========================================================================
    // Constructors
    // ========================================================================

    public UsageMetrics() {
        this.costUsd = BigDecimal.ZERO;
        this.success = true;
    }

    /**
     * Create metrics for a successful request.
     */
    public static UsageMetrics success(
        int clientId, int orgId, int userId,
        String agentName, String sessionId, String modelName,
        int inputTokens, int outputTokens,
        BigDecimal costUsd, long latencyMs
    ) {
        UsageMetrics m = new UsageMetrics();
        m.adClientId = clientId;
        m.adOrgId = orgId;
        m.adUserId = userId;
        m.agentName = agentName;
        m.sessionId = sessionId;
        m.modelName = modelName;
        m.inputTokens = inputTokens;
        m.outputTokens = outputTokens;
        m.totalTokens = inputTokens + outputTokens;
        m.costUsd = costUsd;
        m.latencyMs = latencyMs;
        m.success = true;
        m.created = new Timestamp(System.currentTimeMillis());
        return m;
    }

    /**
     * Create metrics for a failed request.
     */
    public static UsageMetrics failure(
        int clientId, int orgId, int userId,
        String agentName, String sessionId, String modelName,
        long latencyMs, String errorMessage
    ) {
        UsageMetrics m = new UsageMetrics();
        m.adClientId = clientId;
        m.adOrgId = orgId;
        m.adUserId = userId;
        m.agentName = agentName;
        m.sessionId = sessionId;
        m.modelName = modelName;
        m.inputTokens = 0;
        m.outputTokens = 0;
        m.totalTokens = 0;
        m.costUsd = BigDecimal.ZERO;
        m.latencyMs = latencyMs;
        m.success = false;
        m.errorMessage = errorMessage;
        m.created = new Timestamp(System.currentTimeMillis());
        return m;
    }

    // ========================================================================
    // Getters and Setters
    // ========================================================================

    public int getAigUsageMetricsId() {
        return aigUsageMetricsId;
    }

    public void setAigUsageMetricsId(int aigUsageMetricsId) {
        this.aigUsageMetricsId = aigUsageMetricsId;
    }

    public int getAdClientId() {
        return adClientId;
    }

    public void setAdClientId(int adClientId) {
        this.adClientId = adClientId;
    }

    public int getAdOrgId() {
        return adOrgId;
    }

    public void setAdOrgId(int adOrgId) {
        this.adOrgId = adOrgId;
    }

    public int getAdUserId() {
        return adUserId;
    }

    public void setAdUserId(int adUserId) {
        this.adUserId = adUserId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(int inputTokens) {
        this.inputTokens = inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(int outputTokens) {
        this.outputTokens = outputTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(BigDecimal costUsd) {
        this.costUsd = costUsd;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    // ========================================================================
    // Computed Properties
    // ========================================================================

    /**
     * Get tokens per second (throughput metric).
     */
    public double getTokensPerSecond() {
        if (latencyMs <= 0) return 0;
        return totalTokens / (latencyMs / 1000.0);
    }

    /**
     * Get cost per token (efficiency metric).
     */
    public BigDecimal getCostPerToken() {
        if (totalTokens <= 0) return BigDecimal.ZERO;
        return costUsd.divide(new BigDecimal(totalTokens), 8, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return String.format(
            "UsageMetrics[agent=%s, model=%s, tokens=%d, cost=$%.4f, latency=%dms, success=%s]",
            agentName, modelName, totalTokens, costUsd, latencyMs, success
        );
    }
}
