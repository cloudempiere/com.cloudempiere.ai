package com.cloudempiere.ai.provider.langchain4j;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;
import org.compiere.util.Env;

import com.cloudempiere.ai.guardrails.InputGuard;
import com.cloudempiere.ai.guardrails.OutputGuard;
import com.cloudempiere.ai.guardrails.dto.GuardResult;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.observability.CostGuard;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;

/**
 * Main entry point for ERP AI capabilities using LangChain4j.
 *
 * This service provides:
 * - Agent creation with automatic tool binding
 * - Session-scoped conversation memory
 * - Simple API for chat interactions
 *
 * Replaces the complex AIConversationService with LangChain4j simplicity.
 *
 * Usage:
 * <pre>
 * AIService aiService = AIService.getInstance();
 * String response = aiService.chat(provider, ctx, sessionId, "Show me pending orders");
 * </pre>
 *
 * @author Cloudempiere
 * @version 0.13.0
 * @since ADR-002 LangChain4j Strategic Adoption
 */
public class AIService {

    private static final CLogger log = CLogger.getCLogger(AIService.class);

    /** Singleton instance */
    private static AIService instance;

    /** Agent cache by provider ID */
    private final Map<Integer, ERPAgent> agentCache = new ConcurrentHashMap<>();

    /** Memory cache by session ID */
    private final Map<String, MessageWindowChatMemory> memoryCache = new ConcurrentHashMap<>();

    /** Default conversation memory size */
    private static final int DEFAULT_MEMORY_SIZE = 20;

    // ========================================================================
    // Guardrails (ADR-014) and Cost Control (ADR-013)
    // ========================================================================

    /** Input guard for PII/injection detection */
    private final InputGuard inputGuard = new InputGuard();

    /** Output guard for leak/harmful content detection */
    private final OutputGuard outputGuard = new OutputGuard();

    /** Cost guard for budget enforcement */
    private final CostGuard costGuard = new CostGuard();

    /** Flag to enable/disable guardrails (default: enabled) */
    private boolean guardrailsEnabled = true;

    /** Estimated cost per request in USD (conservative estimate) */
    private static final BigDecimal ESTIMATED_COST_PER_REQUEST = new BigDecimal("0.05");

    private AIService() {
        // Private constructor for singleton
        log.info("AIService initialized with guardrails " +
                (guardrailsEnabled ? "enabled" : "disabled"));
    }

    /**
     * Get singleton instance.
     */
    public static synchronized AIService getInstance() {
        if (instance == null) {
            instance = new AIService();
        }
        return instance;
    }

    /**
     * Chat with the AI agent.
     *
     * <p>This method applies the full guardrails pipeline:
     * <ol>
     *   <li>CostGuard: Check budget before making request</li>
     *   <li>InputGuard: Sanitize input (PII, injection)</li>
     *   <li>AI Call: Execute the agent</li>
     *   <li>OutputGuard: Filter response (leaks, harmful content)</li>
     * </ol>
     *
     * @param provider AI Provider configuration
     * @param ctx iDempiere context
     * @param sessionId Session ID for conversation memory
     * @param message User message
     * @return AI response (sanitized)
     * @throws RuntimeException if guardrails block the request or AI call fails
     */
    public String chat(MAIProvider provider, Properties ctx, String sessionId, String message) {
        log.info("AIService.chat: session=" + sessionId +
                ", message=" + message.substring(0, Math.min(50, message.length())));

        try {
            String processedMessage = message;
            int clientId = Env.getAD_Client_ID(ctx);
            int userId = Env.getAD_User_ID(ctx);

            // ================================================================
            // PRE-REQUEST GUARDRAILS
            // ================================================================

            if (guardrailsEnabled) {
                // 1. Cost Guard: Check budget before making request
                try {
                    costGuard.checkBudget(clientId, ESTIMATED_COST_PER_REQUEST);
                    costGuard.checkRateLimit(userId);
                } catch (CostGuard.BudgetExceededException e) {
                    log.warning("Budget exceeded for client " + clientId + ": " + e.getMessage());
                    return "I cannot process this request: " + e.getMessage();
                } catch (CostGuard.RateLimitExceededException e) {
                    log.warning("Rate limit exceeded for user " + userId + ": " + e.getMessage());
                    return "Please wait a moment before sending another message. " + e.getMessage();
                }

                // 2. Input Guard: Sanitize input
                GuardResult inputResult = inputGuard.validate(message);
                if (inputResult.isBlocked()) {
                    log.warning("Input blocked: " + inputResult.getBlockReason());
                    return "I cannot process this request: " + inputResult.getBlockReason();
                }
                if (inputResult.wasModified()) {
                    processedMessage = inputResult.getProcessedContent();
                    log.info("Input masked: " + inputResult.getViolationType());
                }
            }

            // ================================================================
            // AI CALL
            // ================================================================

            ERPAgent agent = getOrCreateAgent(provider, ctx, sessionId);
            String response = agent.chat(sessionId, processedMessage);

            // ================================================================
            // POST-RESPONSE GUARDRAILS
            // ================================================================

            if (guardrailsEnabled) {
                // 3. Output Guard: Filter response
                GuardResult outputResult = outputGuard.validate(response);
                if (outputResult.isBlocked()) {
                    log.warning("Output blocked: " + outputResult.getBlockReason());
                    return "I apologize, but I cannot provide that response. " +
                           "Please try rephrasing your question.";
                }
                if (outputResult.wasModified()) {
                    response = outputResult.getProcessedContent();
                    log.info("Output masked: " + outputResult.getViolationType());
                }
            }

            return response;

        } catch (Exception e) {
            log.severe("Chat failed: " + e.getMessage());
            throw new RuntimeException("AI chat failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a one-shot task without conversation memory.
     *
     * <p>This method applies the same guardrails pipeline as {@link #chat}.
     *
     * @param provider AI Provider configuration
     * @param ctx iDempiere context
     * @param goal Task/goal to execute
     * @return AI response (sanitized)
     * @throws RuntimeException if guardrails block the request or AI call fails
     */
    public String execute(MAIProvider provider, Properties ctx, String goal) {
        log.info("AIService.execute: " + goal.substring(0, Math.min(50, goal.length())));

        try {
            String processedGoal = goal;
            int clientId = Env.getAD_Client_ID(ctx);
            int userId = Env.getAD_User_ID(ctx);

            // ================================================================
            // PRE-REQUEST GUARDRAILS
            // ================================================================

            if (guardrailsEnabled) {
                // 1. Cost Guard: Check budget
                try {
                    costGuard.checkBudget(clientId, ESTIMATED_COST_PER_REQUEST);
                    costGuard.checkRateLimit(userId);
                } catch (CostGuard.BudgetExceededException e) {
                    log.warning("Budget exceeded for client " + clientId + ": " + e.getMessage());
                    return "I cannot process this request: " + e.getMessage();
                } catch (CostGuard.RateLimitExceededException e) {
                    log.warning("Rate limit exceeded for user " + userId + ": " + e.getMessage());
                    return "Please wait a moment. " + e.getMessage();
                }

                // 2. Input Guard: Sanitize input
                GuardResult inputResult = inputGuard.validate(goal);
                if (inputResult.isBlocked()) {
                    log.warning("Input blocked: " + inputResult.getBlockReason());
                    return "I cannot process this request: " + inputResult.getBlockReason();
                }
                if (inputResult.wasModified()) {
                    processedGoal = inputResult.getProcessedContent();
                    log.info("Input masked: " + inputResult.getViolationType());
                }
            }

            // ================================================================
            // AI CALL
            // ================================================================

            ChatLanguageModel model = LangChain4jProviderFactory.create(provider);
            ERPTools tools = new ERPTools(provider, ctx);

            ERPAgent agent = AiServices.builder(ERPAgent.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();

            String response = agent.execute(processedGoal);

            // ================================================================
            // POST-RESPONSE GUARDRAILS
            // ================================================================

            if (guardrailsEnabled) {
                GuardResult outputResult = outputGuard.validate(response);
                if (outputResult.isBlocked()) {
                    log.warning("Output blocked: " + outputResult.getBlockReason());
                    return "I apologize, but I cannot provide that response.";
                }
                if (outputResult.wasModified()) {
                    response = outputResult.getProcessedContent();
                    log.info("Output masked: " + outputResult.getViolationType());
                }
            }

            return response;

        } catch (Exception e) {
            log.severe("Execute failed: " + e.getMessage());
            throw new RuntimeException("AI execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get or create an agent for the given provider and session.
     */
    private ERPAgent getOrCreateAgent(MAIProvider provider, Properties ctx, String sessionId) {
        int providerId = provider.getAIG_Provider_ID();

        // Get or create memory for this session
        MessageWindowChatMemory memory = memoryCache.computeIfAbsent(sessionId,
            id -> MessageWindowChatMemory.withMaxMessages(DEFAULT_MEMORY_SIZE));

        // Create agent (agents are stateless, memory is per-session)
        ChatLanguageModel model = LangChain4jProviderFactory.getOrCreate(provider);
        ERPTools tools = new ERPTools(provider, ctx);

        return AiServices.builder(ERPAgent.class)
            .chatLanguageModel(model)
            .tools(tools)
            .chatMemory(memory)
            .build();
    }

    /**
     * Clear conversation memory for a session.
     */
    public void clearMemory(String sessionId) {
        memoryCache.remove(sessionId);
        log.info("Memory cleared for session: " + sessionId);
    }

    /**
     * Clear all caches (e.g., on configuration change).
     */
    public void clearAllCaches() {
        agentCache.clear();
        memoryCache.clear();
        LangChain4jProviderFactory.clearCache();
        log.info("All caches cleared");
    }

    /**
     * Get conversation memory size for a session.
     */
    public int getMemorySize(String sessionId) {
        MessageWindowChatMemory memory = memoryCache.get(sessionId);
        return memory != null ? memory.messages().size() : 0;
    }

    // ========================================================================
    // Guardrails Control (ADR-014)
    // ========================================================================

    /**
     * Enable or disable guardrails.
     *
     * <p>When disabled:
     * <ul>
     *   <li>No budget/rate limit checks</li>
     *   <li>No input sanitization (PII, injection)</li>
     *   <li>No output filtering (leaks, harmful content)</li>
     * </ul>
     *
     * <p>Use with caution - disabling guardrails removes safety protections.
     *
     * @param enabled true to enable guardrails, false to disable
     */
    public void setGuardrailsEnabled(boolean enabled) {
        this.guardrailsEnabled = enabled;
        log.info("Guardrails " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Check if guardrails are enabled.
     *
     * @return true if guardrails are enabled
     */
    public boolean isGuardrailsEnabled() {
        return guardrailsEnabled;
    }

    /**
     * Get budget status for a client.
     *
     * @param clientId AD_Client_ID
     * @return Budget status information
     */
    public CostGuard.BudgetStatus getBudgetStatus(int clientId) {
        return costGuard.getBudgetStatus(clientId);
    }

    /**
     * Clear budget cache for a client (e.g., after configuration change).
     *
     * @param clientId AD_Client_ID
     */
    public void clearBudgetCache(int clientId) {
        costGuard.clearBudgetCache(clientId);
    }
}
