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
import org.compiere.util.CLogger;
import org.compiere.util.Env;

/**
 * Model class for AIG_UsageMetrics (ADR-013).
 *
 * <p>Provides business logic for recording and querying AI usage metrics
 * including token counts, costs, and latency.
 *
 * <p>Key features:
 * <ul>
 *   <li>Factory methods for creating metrics records</li>
 *   <li>Query methods for usage summaries by user, agent, session</li>
 *   <li>Cost aggregation utilities</li>
 *   <li>Performance tracking helpers</li>
 * </ul>
 *
 * <p><b>Tags:</b> #model #observability #metrics #cost-tracking #usage
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see com.cloudempiere.ai.observability.AIMetricsListener
 * @see com.cloudempiere.ai.observability.CostGuard
 */
public class MAIUsageMetrics extends X_AIG_UsageMetrics {

    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(MAIUsageMetrics.class);

    // ========================================================================
    // Constructors
    // ========================================================================

    /**
     * Standard Constructor.
     *
     * @param ctx Context
     * @param AIG_UsageMetrics_ID ID
     * @param trxName Transaction name
     */
    public MAIUsageMetrics(Properties ctx, int AIG_UsageMetrics_ID, String trxName) {
        super(ctx, AIG_UsageMetrics_ID, trxName);
    }

    /**
     * Standard Constructor with virtual columns.
     *
     * @param ctx Context
     * @param AIG_UsageMetrics_ID ID
     * @param trxName Transaction name
     * @param virtualColumns Virtual columns
     */
    public MAIUsageMetrics(Properties ctx, int AIG_UsageMetrics_ID, String trxName, String... virtualColumns) {
        super(ctx, AIG_UsageMetrics_ID, trxName, virtualColumns);
    }

    /**
     * Load Constructor.
     *
     * @param ctx Context
     * @param rs ResultSet
     * @param trxName Transaction name
     */
    public MAIUsageMetrics(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Create a new usage metrics record.
     *
     * @param ctx Context
     * @param userId User ID
     * @param agentName Agent name
     * @param modelName Model name
     * @param inputTokens Input tokens
     * @param outputTokens Output tokens
     * @param costUSD Cost in USD (microdollars as int for now)
     * @param latencyMs Latency in milliseconds
     * @param trxName Transaction name
     * @return New metrics record (not saved)
     */
    public static MAIUsageMetrics create(Properties ctx, int userId, String agentName,
            String modelName, int inputTokens, int outputTokens, int costUSD,
            int latencyMs, String trxName) {

        // Validate userId - AD_User_ID is NOT NULL in the database
        // Use fallback to System user (100) if no user context is available
        int effectiveUserId = userId;
        if (userId <= 0) {
            effectiveUserId = 100; // System user as fallback
            log.fine("No user context for metrics, using System user (100) for agent=" + agentName);
        }

        MAIUsageMetrics metrics = new MAIUsageMetrics(ctx, 0, trxName);
        metrics.setAD_User_ID(effectiveUserId);
        metrics.setAgentName(agentName);
        metrics.setModelName(modelName);
        metrics.setInputTokens(inputTokens);
        metrics.setOutputTokens(outputTokens);
        metrics.setTotalTokens(inputTokens + outputTokens);
        metrics.setCostUSD(costUSD);
        metrics.setLatencyMs(latencyMs);
        metrics.setRequestTimestamp(new Timestamp(System.currentTimeMillis()));
        metrics.setSuccessFlag(true);

        return metrics;
    }

    /**
     * Create a metrics record for a successful request.
     *
     * @param ctx Context
     * @param userId User ID
     * @param roleId Role ID
     * @param providerId AI Provider ID
     * @param agentName Agent name
     * @param agentType Agent type
     * @param modelName Model name
     * @param inputTokens Input tokens
     * @param outputTokens Output tokens
     * @param costUSD Cost in USD
     * @param latencyMs Latency in milliseconds
     * @param sessionId Session ID
     * @param requestType Request type
     * @param trxName Transaction name
     * @return Saved metrics record
     */
    public static MAIUsageMetrics record(Properties ctx, int userId, int roleId,
            int providerId, String agentName, String agentType, String modelName,
            int inputTokens, int outputTokens, int costUSD, int latencyMs,
            String sessionId, String requestType, String trxName) {

        // Validate userId - AD_User_ID is NOT NULL in the database
        // Use fallback to System user (100) if no user context is available
        int effectiveUserId = userId;
        if (userId <= 0) {
            effectiveUserId = 100; // System user as fallback
            log.fine("No user context for metrics, using System user (100) for agent=" +
                agentName + ", session=" + sessionId);
        }

        MAIUsageMetrics metrics = new MAIUsageMetrics(ctx, 0, trxName);
        metrics.setAD_User_ID(effectiveUserId);
        if (roleId > 0) {
            metrics.setAD_Role_ID(roleId);
        }
        if (providerId > 0) {
            metrics.setAIG_Provider_ID(providerId);
        }
        metrics.setAgentName(agentName);
        metrics.setAgentType(agentType);
        metrics.setModelName(modelName);
        metrics.setInputTokens(inputTokens);
        metrics.setOutputTokens(outputTokens);
        metrics.setTotalTokens(inputTokens + outputTokens);
        metrics.setCostUSD(costUSD);
        metrics.setLatencyMs(latencyMs);
        metrics.setSessionID(sessionId);
        metrics.setRequestType(requestType);
        metrics.setRequestTimestamp(new Timestamp(System.currentTimeMillis()));
        metrics.setSuccessFlag(true);

        if (!metrics.save()) {
            log.warning("Failed to save usage metrics for user=" + userId + ", agent=" + agentName);
            return null;
        }

        return metrics;
    }

    /**
     * Record an error in metrics.
     *
     * @param ctx Context
     * @param userId User ID
     * @param agentName Agent name
     * @param modelName Model name
     * @param errorMessage Error message
     * @param latencyMs Latency in milliseconds
     * @param trxName Transaction name
     * @return Saved metrics record
     */
    public static MAIUsageMetrics recordError(Properties ctx, int userId, String agentName,
            String modelName, String errorMessage, int latencyMs, String trxName) {

        // Validate userId - AD_User_ID is NOT NULL in the database
        // Use fallback to System user (100) if no user context is available
        int effectiveUserId = userId;
        if (userId <= 0) {
            effectiveUserId = 100; // System user as fallback
            log.fine("No user context for error metrics, using System user (100) for agent=" +
                agentName);
        }

        MAIUsageMetrics metrics = new MAIUsageMetrics(ctx, 0, trxName);
        metrics.setAD_User_ID(effectiveUserId);
        metrics.setAgentName(agentName);
        metrics.setModelName(modelName);
        metrics.setLatencyMs(latencyMs);
        metrics.setRequestTimestamp(new Timestamp(System.currentTimeMillis()));
        metrics.setSuccessFlag(false);
        metrics.setErrorMessage(truncate(errorMessage, 2000));

        if (!metrics.save()) {
            log.warning("Failed to save error metrics for user=" + userId + ", agent=" + agentName);
            return null;
        }

        return metrics;
    }

    // ========================================================================
    // Query Methods
    // ========================================================================

    /**
     * Get usage metrics by ID.
     *
     * @param ctx Context
     * @param AIG_UsageMetrics_ID ID
     * @param trxName Transaction name
     * @return Metrics record or null
     */
    public static MAIUsageMetrics get(Properties ctx, int AIG_UsageMetrics_ID, String trxName) {
        if (AIG_UsageMetrics_ID <= 0) {
            return null;
        }
        return new MAIUsageMetrics(ctx, AIG_UsageMetrics_ID, trxName);
    }

    /**
     * Get all metrics for a user.
     *
     * @param ctx Context
     * @param userId User ID
     * @param trxName Transaction name
     * @return List of metrics records
     */
    public static List<MAIUsageMetrics> getByUser(Properties ctx, int userId, String trxName) {
        return new Query(ctx, Table_Name, COLUMNNAME_AD_User_ID + "=?", trxName)
            .setClient_ID()
            .setParameters(userId)
            .setOrderBy(COLUMNNAME_Created + " DESC")
            .list();
    }

    /**
     * Get metrics for a user within a date range.
     *
     * @param ctx Context
     * @param userId User ID
     * @param from Start date
     * @param to End date
     * @param trxName Transaction name
     * @return List of metrics records
     */
    public static List<MAIUsageMetrics> getByUserAndDateRange(Properties ctx, int userId,
            Timestamp from, Timestamp to, String trxName) {
        return new Query(ctx, Table_Name,
                COLUMNNAME_AD_User_ID + "=? AND " +
                COLUMNNAME_RequestTimestamp + ">=? AND " +
                COLUMNNAME_RequestTimestamp + "<=?", trxName)
            .setClient_ID()
            .setParameters(userId, from, to)
            .setOrderBy(COLUMNNAME_RequestTimestamp + " DESC")
            .list();
    }

    /**
     * Get all metrics for an agent.
     *
     * @param ctx Context
     * @param agentName Agent name
     * @param trxName Transaction name
     * @return List of metrics records
     */
    public static List<MAIUsageMetrics> getByAgent(Properties ctx, String agentName, String trxName) {
        return new Query(ctx, Table_Name, COLUMNNAME_AgentName + "=?", trxName)
            .setClient_ID()
            .setParameters(agentName)
            .setOrderBy(COLUMNNAME_Created + " DESC")
            .list();
    }

    /**
     * Get all metrics for a session.
     *
     * @param ctx Context
     * @param sessionId Session ID
     * @param trxName Transaction name
     * @return List of metrics records
     */
    public static List<MAIUsageMetrics> getBySession(Properties ctx, String sessionId, String trxName) {
        return new Query(ctx, Table_Name, COLUMNNAME_SessionID + "=?", trxName)
            .setClient_ID()
            .setParameters(sessionId)
            .setOrderBy(COLUMNNAME_Created + " ASC")
            .list();
    }

    /**
     * Get recent metrics (last N records).
     *
     * @param ctx Context
     * @param limit Maximum number of records
     * @param trxName Transaction name
     * @return List of metrics records
     */
    public static List<MAIUsageMetrics> getRecent(Properties ctx, int limit, String trxName) {
        return new Query(ctx, Table_Name, null, trxName)
            .setClient_ID()
            .setOrderBy(COLUMNNAME_Created + " DESC")
            .setPageSize(limit)
            .list();
    }

    // ========================================================================
    // Aggregation Methods
    // ========================================================================

    /**
     * Get total tokens used by a user today.
     *
     * @param ctx Context
     * @param userId User ID
     * @param trxName Transaction name
     * @return Total tokens
     */
    public static int getTodayTokensByUser(Properties ctx, int userId, String trxName) {
        Timestamp today = Env.getContextAsDate(ctx, "#Date");
        if (today == null) {
            today = new Timestamp(System.currentTimeMillis());
        }

        List<MAIUsageMetrics> metrics = new Query(ctx, Table_Name,
                COLUMNNAME_AD_User_ID + "=? AND TRUNC(" + COLUMNNAME_RequestTimestamp + ")=TRUNC(?)", trxName)
            .setClient_ID()
            .setParameters(userId, today)
            .list();

        return metrics.stream()
            .mapToInt(MAIUsageMetrics::getTotalTokens)
            .sum();
    }

    /**
     * Get total cost for a user today.
     *
     * @param ctx Context
     * @param userId User ID
     * @param trxName Transaction name
     * @return Total cost in microdollars
     */
    public static int getTodayCostByUser(Properties ctx, int userId, String trxName) {
        Timestamp today = Env.getContextAsDate(ctx, "#Date");
        if (today == null) {
            today = new Timestamp(System.currentTimeMillis());
        }

        List<MAIUsageMetrics> metrics = new Query(ctx, Table_Name,
                COLUMNNAME_AD_User_ID + "=? AND TRUNC(" + COLUMNNAME_RequestTimestamp + ")=TRUNC(?)", trxName)
            .setClient_ID()
            .setParameters(userId, today)
            .list();

        return metrics.stream()
            .mapToInt(MAIUsageMetrics::getCostUSD)
            .sum();
    }

    /**
     * Count requests by user in the last N minutes.
     *
     * @param ctx Context
     * @param userId User ID
     * @param minutes Number of minutes
     * @param trxName Transaction name
     * @return Request count
     */
    public static int countRecentRequestsByUser(Properties ctx, int userId, int minutes, String trxName) {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - (minutes * 60 * 1000L));

        return new Query(ctx, Table_Name,
                COLUMNNAME_AD_User_ID + "=? AND " + COLUMNNAME_RequestTimestamp + ">=?", trxName)
            .setClient_ID()
            .setParameters(userId, cutoff)
            .count();
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Get cost as BigDecimal.
     *
     * @return Cost in USD
     */
    public BigDecimal getCostAsBigDecimal() {
        // CostUSD is stored as microdollars (integer)
        return BigDecimal.valueOf(getCostUSD()).divide(BigDecimal.valueOf(1000000), 6, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Calculate tokens per second.
     *
     * @return Tokens per second
     */
    public double getTokensPerSecond() {
        int latency = getLatencyMs();
        if (latency <= 0) {
            return 0;
        }
        return (getTotalTokens() * 1000.0) / latency;
    }

    /**
     * Truncate string to max length.
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    @Override
    public String toString() {
        return "MAIUsageMetrics[" + getAIG_UsageMetrics_ID() +
            ", agent=" + getAgentName() +
            ", tokens=" + getTotalTokens() +
            ", cost=" + getCostUSD() +
            ", latency=" + getLatencyMs() + "ms]";
    }
}
