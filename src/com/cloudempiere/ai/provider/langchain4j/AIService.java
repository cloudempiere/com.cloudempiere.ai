package com.cloudempiere.ai.provider.langchain4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.compiere.model.MChat;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.json.JSONObject;

import com.cloudempiere.ai.guardrails.InputGuard;
import com.cloudempiere.ai.guardrails.OutputGuard;
import com.cloudempiere.ai.guardrails.dto.GuardResult;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.model.MAIUsageMetrics;
import com.cloudempiere.ai.observability.CostGuard;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.TokenUsage;
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

    /** Streaming model cache by provider ID */
    private final Map<Integer, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();

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
     * Chat with context from the chat widget.
     *
     * <p>This method is the main integration point for AIChatWidget:
     * <ul>
     *   <li>Uses ThreadAwareChatMemory for thread-isolated conversations</li>
     *   <li>Injects window/tab context into the conversation</li>
     *   <li>Applies full guardrails pipeline</li>
     *   <li>Persists messages to CM_ChatEntry</li>
     * </ul>
     *
     * @param provider AI Provider configuration
     * @param chat Parent MChat for message persistence
     * @param message User message
     * @param contextData Optional context from window/tab (JSON)
     * @param threadRootId Thread root ID (0 for new thread)
     * @return AI response (sanitized) and updated thread root ID
     * @throws RuntimeException if guardrails block or AI call fails
     */
    public ChatResult chatWithContext(MAIProvider provider, MChat chat,
                                       String message, JSONObject contextData,
                                       int threadRootId) {
        // Validate required parameters
        if (provider == null) {
            return ChatResult.error("AI Provider is not configured", threadRootId);
        }
        if (chat == null) {
            return ChatResult.error("Chat context is required", threadRootId);
        }
        if (message == null || message.trim().isEmpty()) {
            return ChatResult.error("Message cannot be empty", threadRootId);
        }

        Properties ctx = chat.getCtx();
        log.info("AIService.chatWithContext: chat=" + chat.getCM_Chat_ID() +
                ", thread=" + threadRootId +
                ", message=" + message.substring(0, Math.min(50, message.length())));

        try {
            String processedMessage = message;
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
                    return ChatResult.error("I cannot process this request: " + e.getMessage(), threadRootId);
                } catch (CostGuard.RateLimitExceededException e) {
                    log.warning("Rate limit exceeded for user " + userId + ": " + e.getMessage());
                    return ChatResult.error("Please wait a moment before sending another message.", threadRootId);
                }

                // 2. Input Guard: Sanitize input
                GuardResult inputResult = inputGuard.validate(message);
                if (inputResult.isBlocked()) {
                    log.warning("Input blocked: " + inputResult.getBlockReason());
                    return ChatResult.blocked("I cannot process this request: " + inputResult.getBlockReason(),
                                              threadRootId, inputResult.getViolationType());
                }
                if (inputResult.wasModified()) {
                    processedMessage = inputResult.getProcessedContent();
                    log.info("Input masked: " + inputResult.getViolationType());
                }
            }

            // ================================================================
            // BUILD MEMORY WITH THREAD AWARENESS
            // ================================================================

            // Get AI user ID from provider
            Integer aiUserId = provider.getAD_User_ID() > 0 ? provider.getAD_User_ID() : null;

            // Create thread-aware memory (persistence handled by widget)
            ThreadAwareChatMemory memory = ThreadAwareChatMemory.builder()
                .chat(chat)
                .threadRootId(threadRootId)
                .maxMessages(DEFAULT_MEMORY_SIZE)
                .persistMessages(false)  // Widget handles persistence
                .aiUserId(aiUserId)
                .build();

            // ================================================================
            // PREVENT CONSECUTIVE USER MESSAGES
            // Remove ALL trailing UserMessages from memory to prevent the
            // MessageSanitizer from removing the current message.
            // ================================================================
            List<ChatMessage> currentMessages = new ArrayList<>(memory.messages());
            if (!currentMessages.isEmpty()) {
                int trimIndex = currentMessages.size();
                while (trimIndex > 0 && currentMessages.get(trimIndex - 1) instanceof UserMessage) {
                    trimIndex--;
                }
                if (trimIndex < currentMessages.size()) {
                    memory.clear();
                    for (int i = 0; i < trimIndex; i++) {
                        memory.add(currentMessages.get(i));
                    }
                }
            }

            // ================================================================
            // INJECT CONTEXT (if provided)
            // ================================================================

            if (contextData != null && contextData.length() > 0) {
                log.info("[CONTEXT] Context data received with keys: " + contextData.keySet());
                log.info("[CONTEXT] Context success flag: " + contextData.optBoolean("success", false));

                String contextPrompt = buildContextPrompt(contextData);
                if (contextPrompt != null && !contextPrompt.isEmpty()) {
                    // Add context as a system-like user message prefix
                    processedMessage = contextPrompt + "\n\nUser question: " + processedMessage;
                    log.info("[CONTEXT] Context injected into message, total length: " + processedMessage.length());
                } else {
                    log.warning("[CONTEXT] Context prompt was empty or null despite having context data");
                }
            } else {
                log.fine("[CONTEXT] No context data provided to chatWithContext");
            }

            // ================================================================
            // AI CALL
            // ================================================================

            ChatLanguageModel model = LangChain4jProviderFactory.getOrCreate(provider);
            ERPTools tools = new ERPTools(provider, ctx);

            // Use chatMemoryProvider for @MemoryId support in ERPAgent
            // The provider returns our thread-aware memory for any session ID
            final ThreadAwareChatMemory memoryForProvider = memory;
            ERPAgent agent = AiServices.builder(ERPAgent.class)
                .chatLanguageModel(model)
                .tools(tools)
                .chatMemoryProvider(memoryId -> memoryForProvider)
                .build();

            // Build session ID for the agent
            String sessionId = chat.getCM_Chat_ID() + "-" + memory.getCurrentThreadRootId();
            String response = agent.chat(sessionId, processedMessage);

            // ================================================================
            // POST-RESPONSE GUARDRAILS
            // ================================================================

            String warningMessage = null;
            if (guardrailsEnabled) {
                GuardResult outputResult = outputGuard.validate(response);
                if (outputResult.isBlocked()) {
                    log.warning("Output blocked: " + outputResult.getBlockReason());
                    return ChatResult.blocked(
                        "I apologize, but I cannot provide that response. Please try rephrasing your question.",
                        memory.getCurrentThreadRootId(), outputResult.getViolationType());
                }
                if (outputResult.wasModified()) {
                    response = outputResult.getProcessedContent();
                    warningMessage = "Some content was filtered for safety.";
                    log.info("Output masked: " + outputResult.getViolationType());
                }
            }

            return ChatResult.success(response, memory.getCurrentThreadRootId(), warningMessage);

        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            // Log full stack trace to identify NPE location
            log.log(java.util.logging.Level.SEVERE, "ChatWithContext failed: " + errorMsg, e);
            return ChatResult.error("AI chat failed: " + errorMsg, threadRootId);
        }
    }

    // ========================================================================
    // Streaming Chat (ADR-033)
    // ========================================================================

    /**
     * Chat with streaming response and full callback support.
     *
     * <p>This method implements ADR-033: Streaming Responses and Thinking Timeline UX.
     *
     * <p>Features:
     * <ul>
     *   <li>Real-time token streaming via {@link AIStreamCallback#onChunk(String)}</li>
     *   <li>Tool execution events via {@link AIStreamCallback#onToolStart} and {@link AIStreamCallback#onToolComplete}</li>
     *   <li>Guardrails applied to input (output filtering done on accumulated response)</li>
     * </ul>
     *
     * @param provider AI Provider configuration
     * @param chat Parent MChat for message persistence
     * @param message User message
     * @param contextData Optional context from window/tab (JSON)
     * @param threadRootId Thread root ID (0 for new thread)
     * @param callback Streaming callback for real-time response handling
     */
    public void chatStreamingWithContext(MAIProvider provider, MChat chat,
                                          String message, JSONObject contextData,
                                          int threadRootId, AIStreamCallback callback) {
        log.warning("[STREAM] >>> chatStreamingWithContext METHOD ENTRY <<<");

        // Validate required parameters
        if (provider == null) {
            log.severe("[STREAM] Provider is NULL!");
            callback.onError(new IllegalArgumentException("AI Provider is not configured"));
            return;
        }
        log.warning("[STREAM] Provider: " + provider.getName() + " (ID=" + provider.getAIG_Provider_ID() + ")");

        if (chat == null) {
            log.severe("[STREAM] Chat is NULL!");
            callback.onError(new IllegalArgumentException("Chat context is required"));
            return;
        }
        log.warning("[STREAM] Chat ID: " + chat.getCM_Chat_ID());

        if (message == null || message.trim().isEmpty()) {
            log.severe("[STREAM] Message is empty!");
            callback.onError(new IllegalArgumentException("Message cannot be empty"));
            return;
        }
        log.warning("[STREAM] Message length: " + message.length());

        if (callback == null) {
            log.severe("[STREAM] Callback is NULL!");
            throw new IllegalArgumentException("Callback is required for streaming");
        }
        log.warning("[STREAM] Callback: OK");

        Properties ctx = chat.getCtx();
        log.warning("[STREAM] ========================================");
        log.warning("[STREAM] AIService.chatStreamingWithContext STARTED");
        log.warning("[STREAM] chat=" + chat.getCM_Chat_ID() +
                ", thread=" + threadRootId +
                ", provider=" + (provider != null ? provider.getName() : "null"));
        log.warning("[STREAM] message: " + message.substring(0, Math.min(50, message.length())));
        log.warning("[STREAM] ========================================");

        try {
            String processedMessage = message;
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
                    callback.onError(new RuntimeException("Budget exceeded: " + e.getMessage()));
                    return;
                } catch (CostGuard.RateLimitExceededException e) {
                    log.warning("Rate limit exceeded for user " + userId + ": " + e.getMessage());
                    callback.onError(new RuntimeException("Rate limit exceeded: " + e.getMessage()));
                    return;
                }

                // 2. Input Guard: Sanitize input
                GuardResult inputResult = inputGuard.validate(message);
                if (inputResult.isBlocked()) {
                    log.warning("Input blocked: " + inputResult.getBlockReason());
                    callback.onError(new RuntimeException("Input blocked: " + inputResult.getBlockReason()));
                    return;
                }
                if (inputResult.wasModified()) {
                    processedMessage = inputResult.getProcessedContent();
                    log.info("Input masked: " + inputResult.getViolationType());
                }
            }

            // ================================================================
            // BUILD MEMORY WITH THREAD AWARENESS
            // ================================================================

            Integer aiUserId = provider.getAD_User_ID() > 0 ? provider.getAD_User_ID() : null;

            ThreadAwareChatMemory memory = ThreadAwareChatMemory.builder()
                .chat(chat)
                .threadRootId(threadRootId)
                .maxMessages(DEFAULT_MEMORY_SIZE)
                .persistMessages(false)  // Widget handles persistence
                .aiUserId(aiUserId)
                .build();

            // ================================================================
            // PREVENT CONSECUTIVE USER MESSAGES
            // Remove ALL trailing UserMessages from memory to prevent the
            // MessageSanitizer from removing the current message.
            // This handles cases where multiple failed retries left consecutive
            // UserMessages in the database.
            // ================================================================
            List<ChatMessage> currentMessages = new ArrayList<>(memory.messages());
            log.warning("[STREAM] Memory loaded " + currentMessages.size() + " messages from DB");

            if (!currentMessages.isEmpty()) {
                // Find where to trim - remove all trailing UserMessages
                int trimIndex = currentMessages.size();
                while (trimIndex > 0 && currentMessages.get(trimIndex - 1) instanceof UserMessage) {
                    trimIndex--;
                }

                if (trimIndex < currentMessages.size()) {
                    int removedCount = currentMessages.size() - trimIndex;
                    log.warning("[STREAM] Removing " + removedCount + " trailing UserMessage(s) from memory to prevent consecutive message sanitization");

                    // Rebuild memory without trailing UserMessages
                    memory.clear();
                    for (int i = 0; i < trimIndex; i++) {
                        memory.add(currentMessages.get(i));
                    }
                    log.warning("[STREAM] Memory now has " + memory.getMessageCount() + " messages after cleanup");
                }
            }

            // Log current memory state for debugging
            log.warning("[STREAM] Memory state before agent.chat(): " + memory.getMessageCount() + " messages");
            if (memory.getMessageCount() > 0) {
                ChatMessage last = memory.messages().get(memory.getMessageCount() - 1);
                log.warning("[STREAM] Last message type: " + last.type());
            }

            // ================================================================
            // INJECT CONTEXT (if provided)
            // ================================================================

            if (contextData != null && contextData.length() > 0) {
                log.info("[CONTEXT-STREAM] Context data received with keys: " + contextData.keySet());
                log.info("[CONTEXT-STREAM] Context success flag: " + contextData.optBoolean("success", false));

                String contextPrompt = buildContextPrompt(contextData);
                if (contextPrompt != null && !contextPrompt.isEmpty()) {
                    processedMessage = contextPrompt + "\n\nUser question: " + processedMessage;
                    log.info("[CONTEXT-STREAM] Context injected into message, total length: " + processedMessage.length());
                } else {
                    log.warning("[CONTEXT-STREAM] Context prompt was empty or null despite having context data");
                }
            } else {
                log.fine("[CONTEXT-STREAM] No context data provided to chatStreamingWithContext");
            }

            // ================================================================
            // STREAMING AI CALL WITH TOOLS (ADR-033)
            // ================================================================

            StreamingChatLanguageModel streamingModel = getOrCreateStreamingModel(provider);

            // Create streaming-aware tools with callback support
            // StreamingERPTools wraps ERPTools and fires onToolStart/onToolComplete/onToolError
            StreamingERPTools tools = new StreamingERPTools(provider, ctx, callback);

            log.warning("[STREAM] Created StreamingERPTools with callback support");
            log.warning("[STREAM] Message preview: " + processedMessage.substring(0, Math.min(50, processedMessage.length())));

            // Track accumulated response for output guardrails and metrics
            final AtomicReference<StringBuilder> responseAccumulator = new AtomicReference<>(new StringBuilder());

            // ================================================================
            // METRICS CONTEXT (ADR-013)
            // Capture context for recording in onComplete handler
            // ================================================================
            final long streamingStartTime = System.currentTimeMillis();
            final int metricsUserId = Env.getAD_User_ID(ctx);
            final int metricsRoleId = Env.getAD_Role_ID(ctx);
            final int metricsProviderId = provider.getAIG_Provider_ID();
            final String metricsModelName = getModelNameForProvider(provider);
            final String metricsSessionId = chat.getCM_Chat_ID() + "-" + threadRootId;
            final Properties metricsCtx = ctx;
            final String metricsInputMessage = processedMessage; // Capture for token estimation

            // Start streaming
            callback.onProgress("analyzing", "Processing your request...");

            // ================================================================
            // BUILD STREAMING AGENT WITH TOOLS
            // Use AiServices to create ERPStreamingAgent with tool support
            // ================================================================
            log.warning("[STREAM] Building ERPStreamingAgent with tools...");
            log.warning("[STREAM] Tools class: " + tools.getClass().getName());
            log.warning("[STREAM] StreamingModel class: " + streamingModel.getClass().getName());

            final ThreadAwareChatMemory memoryForProvider = memory;
            ERPStreamingAgent agent = AiServices.builder(ERPStreamingAgent.class)
                .streamingChatLanguageModel(streamingModel)
                .tools(tools)
                .chatMemoryProvider(memoryId -> memoryForProvider)
                .build();

            log.warning("[STREAM] Agent built successfully: " + agent.getClass().getName());

            // Build session ID for the agent
            String sessionId = chat.getCM_Chat_ID() + "-" + memory.getCurrentThreadRootId();
            log.warning("[STREAM] Starting TokenStream with sessionId: " + sessionId);
            log.warning("[STREAM] Memory has " + memoryForProvider.getMessageCount() + " messages before agent.chat()");

            // Get TokenStream from agent
            dev.langchain4j.service.TokenStream tokenStream = agent.chat(sessionId, processedMessage);

            // Wire up TokenStream callbacks (LangChain4j 0.35.0 API)
            tokenStream
                .onNext(token -> {
                    log.fine("[STREAM] onNext: " + (token != null ? token.length() : 0) + " chars");
                    responseAccumulator.get().append(token);
                    try {
                        callback.onChunk(token);
                    } catch (Exception e) {
                        log.severe("[STREAM] Error in onChunk callback: " + e.getMessage());
                    }
                })
                .onComplete(response -> {
                    log.warning("[STREAM] onComplete: streaming finished");
                    long streamingEndTime = System.currentTimeMillis();

                    // Get AI response text (from response or accumulator)
                    String aiResponseText = responseAccumulator.get().toString();
                    if (response != null && response.content() != null && response.content().text() != null) {
                        aiResponseText = response.content().text();
                    }

                    // Add AI response to memory for conversation continuity
                    if (aiResponseText != null && !aiResponseText.isEmpty()) {
                        memory.add(AiMessage.from(aiResponseText));
                    }

                    // ================================================================
                    // RECORD METRICS (ADR-013)
                    // ================================================================
                    try {
                        TokenUsage tokenUsage = response != null ? response.tokenUsage() : null;
                        int inputTokens = 0;
                        int outputTokens = 0;

                        if (tokenUsage != null) {
                            inputTokens = tokenUsage.inputTokenCount() != null ?
                                tokenUsage.inputTokenCount() : 0;
                            outputTokens = tokenUsage.outputTokenCount() != null ?
                                tokenUsage.outputTokenCount() : 0;
                        } else {
                            // Estimate tokens if not provided (rough: 4 chars = 1 token)
                            inputTokens = metricsInputMessage.length() / 4;
                            outputTokens = aiResponseText != null ? aiResponseText.length() / 4 : 0;
                            log.fine("[METRICS] Token usage not provided, estimated: in=" +
                                inputTokens + ", out=" + outputTokens);
                        }

                        int latencyMs = (int) (streamingEndTime - streamingStartTime);

                        // Calculate cost in microdollars (1 USD = 1,000,000 microdollars)
                        int costMicrodollars = calculateCostMicrodollars(
                            metricsModelName, inputTokens, outputTokens);

                        // Persist metrics
                        MAIUsageMetrics.record(
                            metricsCtx,
                            metricsUserId,
                            metricsRoleId,
                            metricsProviderId,
                            "chat-streaming-tools",  // agentName (updated to reflect tools support)
                            "STREAMING",             // agentType
                            metricsModelName,
                            inputTokens,
                            outputTokens,
                            costMicrodollars,
                            latencyMs,
                            metricsSessionId,
                            "CHAT_STREAMING",        // requestType
                            null                     // trxName (auto-commit)
                        );

                        log.info("[METRICS] Recorded: model=" + metricsModelName +
                            ", tokens=" + (inputTokens + outputTokens) +
                            ", cost=$" + String.format("%.6f", costMicrodollars / 1000000.0) +
                            ", latency=" + latencyMs + "ms");

                    } catch (Exception e) {
                        log.warning("[METRICS] Failed to record metrics: " + e.getMessage());
                    }

                    // Apply output guardrails on complete response
                    final String finalAiResponseText = aiResponseText;
                    if (guardrailsEnabled && finalAiResponseText != null) {
                        GuardResult outputResult = outputGuard.validate(finalAiResponseText);
                        if (outputResult.isBlocked()) {
                            log.warning("Output blocked: " + outputResult.getBlockReason());
                        }
                        if (outputResult.wasModified()) {
                            log.warning("Output would have been masked: " + outputResult.getViolationType());
                        }
                    }
                    try {
                        callback.onComplete();
                    } catch (Exception e) {
                        log.severe("[STREAM] Error in onComplete callback: " + e.getMessage());
                    }
                })
                .onError(error -> {
                    log.severe("[STREAM] onError: " + error.getClass().getName() + ": " + error.getMessage());
                    try {
                        callback.onError(new Exception(error.getMessage(), error));
                    } catch (Exception e) {
                        log.severe("[STREAM] Error in onError callback: " + e.getMessage());
                    }
                })
                .start();

            log.warning("[STREAM] TokenStream started with tools support");

        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.log(java.util.logging.Level.SEVERE, "ChatStreamingWithContext failed: " + errorMsg, e);
            callback.onError(e);
        }
    }

    /**
     * Get or create a cached StreamingChatLanguageModel instance.
     *
     * @param provider MAIProvider configuration
     * @return StreamingChatLanguageModel instance
     */
    private StreamingChatLanguageModel getOrCreateStreamingModel(MAIProvider provider) {
        return streamingModelCache.computeIfAbsent(provider.getAIG_Provider_ID(),
            id -> LangChain4jProviderFactory.createStreaming(provider, null, null));
    }

    /**
     * Build context prompt from window/tab data.
     *
     * <p>Handles the nested JSON structure produced by WindowContextProvider:
     * <ul>
     *   <li>user_context - user information (user_id, user_name, role_name, etc.)</li>
     *   <li>window_metadata - window info (name, description, help)</li>
     *   <li>tab_context - current tab info (tab_name, table_name, record_id)</li>
     *   <li>record_data - business field values from current record</li>
     *   <li>technical_data - technical/audit field values</li>
     * </ul>
     *
     * @param contextData JSON context data from WindowContextProvider
     * @return Context prompt string or null if context is empty/invalid
     */
    private String buildContextPrompt(JSONObject contextData) {
        if (contextData == null || contextData.length() == 0) {
            log.fine("[CONTEXT] No context data provided");
            return null;
        }

        // Check for successful context extraction
        if (!contextData.optBoolean("success", false)) {
            log.fine("[CONTEXT] Context extraction was not successful");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Context from current iDempiere window:\n\n");

        // Window metadata (nested structure from WindowContextProvider)
        if (contextData.has("window_metadata")) {
            JSONObject windowMeta = contextData.getJSONObject("window_metadata");
            sb.append("## Window Information\n");
            sb.append("- Window: ").append(windowMeta.optString("name", "Unknown")).append("\n");

            String desc = windowMeta.optString("description", "");
            if (!desc.isEmpty()) {
                sb.append("- Description: ").append(desc).append("\n");
            }

            String help = windowMeta.optString("help", "");
            if (!help.isEmpty() && help.length() <= 200) {
                sb.append("- Help: ").append(help).append("\n");
            }
            sb.append("\n");
        }

        // Tab context (nested structure from WindowContextProvider)
        if (contextData.has("tab_context")) {
            JSONObject tabCtx = contextData.getJSONObject("tab_context");
            sb.append("## Current Tab\n");
            sb.append("- Tab: ").append(tabCtx.optString("tab_name", "Unknown")).append("\n");
            sb.append("- Table: ").append(tabCtx.optString("table_name", "Unknown")).append("\n");

            int recordId = tabCtx.optInt("record_id", 0);
            if (recordId > 0) {
                sb.append("- Record ID: ").append(recordId).append("\n");
            }

            int selectedRecordId = tabCtx.optInt("selected_record_id", 0);
            if (selectedRecordId > 0 && selectedRecordId != recordId) {
                sb.append("- Selected Record ID: ").append(selectedRecordId).append("\n");
            }
            sb.append("\n");
        }

        // Record data (business fields from WindowContextProvider)
        if (contextData.has("record_data")) {
            JSONObject recordData = contextData.getJSONObject("record_data");
            if (recordData.length() > 0) {
                sb.append("## Current Record Data\n");
                for (String key : recordData.keySet()) {
                    Object value = recordData.get(key);
                    if (value != null && !value.toString().isEmpty()) {
                        sb.append("- ").append(key).append(": ").append(value).append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        // User context (for authorization awareness)
        if (contextData.has("user_context")) {
            JSONObject userCtx = contextData.getJSONObject("user_context");
            sb.append("## User Context\n");
            sb.append("- User: ").append(userCtx.optString("user_name", "Unknown")).append("\n");
            sb.append("- Role: ").append(userCtx.optString("role_name", "Unknown")).append("\n");
            sb.append("- Client: ").append(userCtx.optString("client_name", "Unknown")).append("\n");
            sb.append("- Organization: ").append(userCtx.optString("org_name", "Unknown")).append("\n");
        }

        String result = sb.toString();
        log.info("[CONTEXT] Built context prompt (" + result.length() + " chars)");
        return result;
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
        streamingModelCache.clear();
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

    // ========================================================================
    // Metrics Helpers (ADR-013)
    // ========================================================================

    /**
     * Get model name based on provider type.
     *
     * @param provider AI Provider
     * @return Model name for metrics tracking
     */
    private static String getModelNameForProvider(MAIProvider provider) {
        if (provider == null) {
            return "unknown";
        }
        String providerType = provider.getAIGProviderType();
        if (providerType == null) {
            return "unknown";
        }

        // Map provider types to model names
        if (providerType.contains("Anthropic") || providerType.contains("Claude")) {
            return "claude-sonnet-4";
        } else if (providerType.contains("Bedrock")) {
            return "claude-3-5-sonnet-bedrock";
        } else if (providerType.contains("Ollama")) {
            return "llama3.2";
        } else if (providerType.contains("OpenAI")) {
            return "gpt-4o";
        } else {
            return providerType.toLowerCase();
        }
    }

    /**
     * Calculate cost in microdollars based on model pricing.
     *
     * <p>Pricing (per 1M tokens):
     * <ul>
     *   <li>Claude Sonnet 4: $3 input, $15 output</li>
     *   <li>Claude Haiku: $0.25 input, $1.25 output</li>
     *   <li>Claude Opus: $15 input, $75 output</li>
     *   <li>GPT-4o: $5 input, $15 output</li>
     *   <li>Ollama (local): Free</li>
     * </ul>
     *
     * @param modelName Model identifier
     * @param inputTokens Input token count
     * @param outputTokens Output token count
     * @return Cost in microdollars (1 USD = 1,000,000 microdollars)
     */
    private static int calculateCostMicrodollars(String modelName, int inputTokens, int outputTokens) {
        if (modelName == null) {
            modelName = "";
        }
        String model = modelName.toLowerCase();

        // Rates in dollars per 1M tokens
        double inputRatePerMillion;
        double outputRatePerMillion;

        if (model.contains("opus")) {
            inputRatePerMillion = 15.0;
            outputRatePerMillion = 75.0;
        } else if (model.contains("haiku")) {
            inputRatePerMillion = 0.25;
            outputRatePerMillion = 1.25;
        } else if (model.contains("sonnet") || model.contains("claude")) {
            // Claude Sonnet 4 / 3.5 Sonnet
            inputRatePerMillion = 3.0;
            outputRatePerMillion = 15.0;
        } else if (model.contains("gpt-4o-mini")) {
            inputRatePerMillion = 0.15;
            outputRatePerMillion = 0.6;
        } else if (model.contains("gpt-4o") || model.contains("gpt-4")) {
            inputRatePerMillion = 5.0;
            outputRatePerMillion = 15.0;
        } else if (model.contains("llama") || model.contains("mistral") || model.contains("ollama")) {
            // Local models are free
            return 0;
        } else {
            // Default to Sonnet pricing
            inputRatePerMillion = 3.0;
            outputRatePerMillion = 15.0;
        }

        // Calculate cost: (tokens / 1M) * rate * 1M (for microdollars)
        double inputCost = (inputTokens / 1_000_000.0) * inputRatePerMillion * 1_000_000;
        double outputCost = (outputTokens / 1_000_000.0) * outputRatePerMillion * 1_000_000;

        return (int) (inputCost + outputCost);
    }

    // ========================================================================
    // ChatResult - Result object for chatWithContext
    // ========================================================================

    /**
     * Result object for chat operations.
     *
     * <p>Contains:
     * <ul>
     *   <li>Response text (or error message)</li>
     *   <li>Updated thread root ID</li>
     *   <li>Success/error/blocked status</li>
     *   <li>Optional warning message (e.g., content filtered)</li>
     *   <li>Violation type (if blocked)</li>
     * </ul>
     */
    public static class ChatResult {

        /** Result status */
        public enum Status { SUCCESS, ERROR, BLOCKED }

        private final Status status;
        private final String response;
        private final int threadRootId;
        private final String warningMessage;
        private final String violationType;

        private ChatResult(Status status, String response, int threadRootId,
                          String warningMessage, String violationType) {
            this.status = status;
            this.response = response;
            this.threadRootId = threadRootId;
            this.warningMessage = warningMessage;
            this.violationType = violationType;
        }

        /**
         * Create a successful result.
         */
        public static ChatResult success(String response, int threadRootId, String warningMessage) {
            return new ChatResult(Status.SUCCESS, response, threadRootId, warningMessage, null);
        }

        /**
         * Create an error result.
         */
        public static ChatResult error(String errorMessage, int threadRootId) {
            return new ChatResult(Status.ERROR, errorMessage, threadRootId, null, null);
        }

        /**
         * Create a blocked result (guardrails).
         */
        public static ChatResult blocked(String message, int threadRootId, String violationType) {
            return new ChatResult(Status.BLOCKED, message, threadRootId, null, violationType);
        }

        // Getters

        public Status getStatus() { return status; }
        public String getResponse() { return response; }
        public int getThreadRootId() { return threadRootId; }
        public String getWarningMessage() { return warningMessage; }
        public String getViolationType() { return violationType; }

        public boolean isSuccess() { return status == Status.SUCCESS; }
        public boolean isError() { return status == Status.ERROR; }
        public boolean isBlocked() { return status == Status.BLOCKED; }
        public boolean hasWarning() { return warningMessage != null; }
    }
}
