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
import java.sql.Timestamp;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.cloudempiere.ai.model.MAIUsageMetrics;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;

/**
 * AI Metrics Listener for observability (ADR-013).
 *
 * <p>Implements LangChain4j ChatModelListener to capture:
 * <ul>
 *   <li>Token usage (input, output, total)</li>
 *   <li>Cost estimation (USD)</li>
 *   <li>Latency (milliseconds)</li>
 *   <li>Model and agent information</li>
 * </ul>
 *
 * <p>Persists metrics to AIG_UsageMetrics table for analysis and budgeting.
 *
 * <p><b>Tags:</b> #observability #metrics #tokens #cost-tracking #langchain4j #audit
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see CostGuard
 * @see com.cloudempiere.ai.observability.dto.UsageMetrics
 */
public class AIMetricsListener implements ChatModelListener {

    private static final CLogger log = CLogger.getCLogger(AIMetricsListener.class);

    /** Request start times by request ID */
    private final Map<Object, Long> requestStartTimes = new ConcurrentHashMap<>();

    /** Context for the current session */
    private final int adClientId;
    private final int adOrgId;
    private final int adUserId;
    private final String agentName;
    private final String sessionId;

    /** Cost per 1K tokens by model (approximate USD) */
    private static final Map<String, BigDecimal[]> MODEL_PRICING = new ConcurrentHashMap<>();

    static {
        // [input_cost_per_1k, output_cost_per_1k]
        // Claude models
        MODEL_PRICING.put("claude-3-opus", new BigDecimal[]{new BigDecimal("0.015"), new BigDecimal("0.075")});
        MODEL_PRICING.put("claude-3-sonnet", new BigDecimal[]{new BigDecimal("0.003"), new BigDecimal("0.015")});
        MODEL_PRICING.put("claude-3-haiku", new BigDecimal[]{new BigDecimal("0.00025"), new BigDecimal("0.00125")});
        MODEL_PRICING.put("claude-3-5-sonnet", new BigDecimal[]{new BigDecimal("0.003"), new BigDecimal("0.015")});
        MODEL_PRICING.put("claude-sonnet-4", new BigDecimal[]{new BigDecimal("0.003"), new BigDecimal("0.015")});

        // OpenAI models
        MODEL_PRICING.put("gpt-4o", new BigDecimal[]{new BigDecimal("0.005"), new BigDecimal("0.015")});
        MODEL_PRICING.put("gpt-4o-mini", new BigDecimal[]{new BigDecimal("0.00015"), new BigDecimal("0.0006")});
        MODEL_PRICING.put("gpt-4-turbo", new BigDecimal[]{new BigDecimal("0.01"), new BigDecimal("0.03")});

        // AWS Bedrock (approximate)
        MODEL_PRICING.put("anthropic.claude", new BigDecimal[]{new BigDecimal("0.003"), new BigDecimal("0.015")});
        MODEL_PRICING.put("amazon.titan", new BigDecimal[]{new BigDecimal("0.0008"), new BigDecimal("0.0016")});

        // Ollama (free, local)
        MODEL_PRICING.put("llama", new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        MODEL_PRICING.put("mistral", new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
    }

    /**
     * Create metrics listener with context.
     *
     * @param adClientId Client ID
     * @param adOrgId Organization ID
     * @param adUserId User ID
     * @param agentName Agent name for tracking
     * @param sessionId Session identifier
     */
    public AIMetricsListener(int adClientId, int adOrgId, int adUserId,
                             String agentName, String sessionId) {
        this.adClientId = adClientId;
        this.adOrgId = adOrgId;
        this.adUserId = adUserId;
        this.agentName = agentName != null ? agentName : "default";
        this.sessionId = sessionId;
    }

    /**
     * Create metrics listener from Env context.
     *
     * @param agentName Agent name
     * @param sessionId Session ID
     */
    public AIMetricsListener(String agentName, String sessionId) {
        this(
            Env.getAD_Client_ID(Env.getCtx()),
            Env.getAD_Org_ID(Env.getCtx()),
            Env.getAD_User_ID(Env.getCtx()),
            agentName,
            sessionId
        );
    }

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // Record request start time
        Object requestId = requestContext.attributes().computeIfAbsent(
            "requestId", k -> System.nanoTime()
        );
        requestStartTimes.put(requestId, System.currentTimeMillis());

        // Store context for response handler
        requestContext.attributes().put("adClientId", adClientId);
        requestContext.attributes().put("adOrgId", adOrgId);
        requestContext.attributes().put("adUserId", adUserId);
        requestContext.attributes().put("agentName", agentName);
        requestContext.attributes().put("sessionId", sessionId);

        log.fine("AI request started: agent=" + agentName + ", session=" + sessionId);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        try {
            // Calculate latency
            Object requestId = responseContext.attributes().get("requestId");
            Long startTime = requestStartTimes.remove(requestId);
            long latencyMs = startTime != null ?
                System.currentTimeMillis() - startTime : 0;

            // Extract token usage from Response (0.35.0 API)
            TokenUsage tokenUsage = responseContext.response().tokenUsage();

            int inputTokens = tokenUsage != null ? tokenUsage.inputTokenCount() : 0;
            int outputTokens = tokenUsage != null ? tokenUsage.outputTokenCount() : 0;
            int totalTokens = inputTokens + outputTokens;

            // Get model name from attributes (stored during request phase)
            String modelName = (String) responseContext.attributes().getOrDefault("modelName", "unknown");

            // Calculate cost
            BigDecimal costUsd = calculateCost(modelName, inputTokens, outputTokens);

            // Persist metrics
            persistMetrics(
                (Integer) responseContext.attributes().get("adClientId"),
                (Integer) responseContext.attributes().get("adOrgId"),
                (Integer) responseContext.attributes().get("adUserId"),
                (String) responseContext.attributes().get("agentName"),
                (String) responseContext.attributes().get("sessionId"),
                modelName,
                inputTokens,
                outputTokens,
                totalTokens,
                costUsd,
                latencyMs,
                true, // success
                null  // no error
            );

            log.fine("AI response recorded: model=" + modelName +
                    ", tokens=" + totalTokens +
                    ", cost=$" + costUsd +
                    ", latency=" + latencyMs + "ms");

        } catch (Exception e) {
            log.warning("Failed to record AI metrics: " + e.getMessage());
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        try {
            // Calculate latency
            Object requestId = errorContext.attributes().get("requestId");
            Long startTime = requestStartTimes.remove(requestId);
            long latencyMs = startTime != null ?
                System.currentTimeMillis() - startTime : 0;

            // Get model name from attributes (stored during request phase)
            String modelName = (String) errorContext.attributes().getOrDefault("modelName", "unknown");

            // Persist error metrics
            persistMetrics(
                (Integer) errorContext.attributes().get("adClientId"),
                (Integer) errorContext.attributes().get("adOrgId"),
                (Integer) errorContext.attributes().get("adUserId"),
                (String) errorContext.attributes().get("agentName"),
                (String) errorContext.attributes().get("sessionId"),
                modelName,
                0, 0, 0,
                BigDecimal.ZERO,
                latencyMs,
                false, // failure
                errorContext.error().getMessage()
            );

            log.warning("AI error recorded: model=" + modelName +
                       ", error=" + errorContext.error().getMessage());

        } catch (Exception e) {
            log.warning("Failed to record AI error metrics: " + e.getMessage());
        }
    }

    /**
     * Calculate cost based on model and token usage.
     *
     * @param modelName Model name
     * @param inputTokens Input token count
     * @param outputTokens Output token count
     * @return Estimated cost in USD
     */
    private BigDecimal calculateCost(String modelName, int inputTokens, int outputTokens) {
        // Find matching pricing
        BigDecimal[] pricing = null;
        String modelLower = modelName.toLowerCase();

        for (Map.Entry<String, BigDecimal[]> entry : MODEL_PRICING.entrySet()) {
            if (modelLower.contains(entry.getKey().toLowerCase())) {
                pricing = entry.getValue();
                break;
            }
        }

        if (pricing == null) {
            // Default to medium pricing
            pricing = new BigDecimal[]{new BigDecimal("0.003"), new BigDecimal("0.015")};
        }

        // Cost = (input_tokens / 1000 * input_rate) + (output_tokens / 1000 * output_rate)
        BigDecimal inputCost = pricing[0]
            .multiply(new BigDecimal(inputTokens))
            .divide(new BigDecimal(1000), 6, BigDecimal.ROUND_HALF_UP);

        BigDecimal outputCost = pricing[1]
            .multiply(new BigDecimal(outputTokens))
            .divide(new BigDecimal(1000), 6, BigDecimal.ROUND_HALF_UP);

        return inputCost.add(outputCost);
    }

    /**
     * Persist metrics to database using MAIUsageMetrics model.
     */
    private void persistMetrics(
        Integer clientId, Integer orgId, Integer userId,
        String agent, String session,
        String modelName,
        int inputTokens, int outputTokens, int totalTokens,
        BigDecimal costUsd, long latencyMs,
        boolean success, String errorMessage
    ) {
        // Use defaults if context not available
        int cId = clientId != null ? clientId : adClientId;
        int oId = orgId != null ? orgId : adOrgId;
        int uId = userId != null ? userId : adUserId;
        String agnt = agent != null ? agent : agentName;
        String sess = session != null ? session : sessionId;

        try {
            // Create context with client/org
            Properties ctx = Env.getCtx();
            Env.setContext(ctx, "#AD_Client_ID", cId);
            Env.setContext(ctx, "#AD_Org_ID", oId);

            // Convert cost to microdollars for storage
            int costMicrodollars = costUsd.multiply(new BigDecimal(1000000))
                .intValue();

            if (success) {
                MAIUsageMetrics.record(
                    ctx, uId, 0, // roleId
                    0, // providerId - not tracked in listener context
                    agnt, null, // agentType
                    modelName,
                    inputTokens, outputTokens,
                    costMicrodollars, (int) latencyMs,
                    sess, null, // requestType
                    null // trxName
                );
            } else {
                MAIUsageMetrics.recordError(
                    ctx, uId, agnt, modelName,
                    errorMessage, (int) latencyMs,
                    null // trxName
                );
            }

        } catch (Exception e) {
            // Table might not exist yet - log but don't fail
            log.fine("Could not persist metrics (table may not exist): " + e.getMessage());
        }
    }

    /**
     * Get total cost for a client today.
     *
     * @param clientId Client ID
     * @return Today's total cost in USD
     */
    public static BigDecimal getTodayCost(int clientId) {
        String sql = "SELECT COALESCE(SUM(CostUSD), 0) FROM AIG_UsageMetrics " +
                    "WHERE AD_Client_ID = ? AND Created >= CURRENT_DATE";

        try {
            return DB.getSQLValueBD(null, sql, clientId);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get total tokens for a client today.
     *
     * @param clientId Client ID
     * @return Today's total tokens
     */
    public static int getTodayTokens(int clientId) {
        String sql = "SELECT COALESCE(SUM(TotalTokens), 0) FROM AIG_UsageMetrics " +
                    "WHERE AD_Client_ID = ? AND Created >= CURRENT_DATE";

        try {
            return DB.getSQLValue(null, sql, clientId);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get request count for a client today.
     *
     * @param clientId Client ID
     * @return Today's request count
     */
    public static int getTodayRequestCount(int clientId) {
        String sql = "SELECT COUNT(*) FROM AIG_UsageMetrics " +
                    "WHERE AD_Client_ID = ? AND Created >= CURRENT_DATE";

        try {
            return DB.getSQLValue(null, sql, clientId);
        } catch (Exception e) {
            return 0;
        }
    }
}
