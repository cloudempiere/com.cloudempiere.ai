package com.cloudempiere.ai.provider.langchain4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.compiere.model.MChat;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.cloudempiere.ai.guardrails.InputGuard;
import com.cloudempiere.ai.guardrails.OutputGuard;
import com.cloudempiere.ai.guardrails.dto.GuardResult;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.model.MAIUsageMetrics;
import com.cloudempiere.ai.observability.CostGuard;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;
import com.cloudempiere.ai.provider.langchain4j.tools.RagTools;
import com.cloudempiere.ai.rag.IRagService;
import com.cloudempiere.ai.service.LanguageDetectionService;

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
 * <p><strong>STATUS: ACTIVE - This is the production chat service.</strong>
 *
 * <p><strong>Production Flow:</strong>
 * <pre>
 * AIChatWidget → AIService → ERPStreamingAgent/SimpleStreamingAgent
 * </pre>
 *
 * <p><strong>Note:</strong> {@link com.cloudempiere.ai.rag.RAGConversationService} exists
 * but is NOT yet wired. This class (AIService) handles all chat functionality.
 *
 * <hr>
 *
 * <p>This service provides a unified streaming-first API:
 * <ul>
 *   <li>{@link #chatStreamingWithContext} - Core streaming method (primary)</li>
 *   <li>{@link #chatBlocking} - Blocking wrapper using CompletableFuture</li>
 *   <li>{@link #chat} - Legacy simple chat method</li>
 *   <li>{@link #chatWithContext} - Legacy context-aware chat method</li>
 * </ul>
 *
 * <p>Streaming-First Architecture:
 * All chat functionality is built on streaming as the foundation. Non-streaming
 * calls use {@link CompletableFuture#join()} to block on the streaming response.
 * This eliminates duplicate code paths and ensures consistent behavior.
 *
 * <p>Language Detection (ADR-037):
 * Uses {@link LanguageDetectionService} to respect user's iDempiere login language
 * and handle session-level language change requests (e.g., "respond in German").
 *
 * <p>Usage:
 * <pre>
 * AIService aiService = AIService.getInstance();
 *
 * // Streaming (recommended for UI)
 * aiService.chatStreamingWithContext(provider, chat, message, context, threadId,
 *     AIStreamCallback.builder()
 *         .onChunk(chunk -> ui.append(chunk))
 *         .onComplete(() -> ui.done())
 *         .build());
 *
 * // Blocking (for programmatic use)
 * ChatResult result = aiService.chatBlocking(provider, chat, message, context, threadId);
 * </pre>
 *
 * @author Cloudempiere
 * @version 0.19.0
 * @since ADR-002 LangChain4j Strategic Adoption
 * @see com.cloudempiere.ai.rag.RAGConversationService RAGConversationService (not yet wired)
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

    /** Language detection service (ADR-037) */
    private final LanguageDetectionService languageService = LanguageDetectionService.getInstance();

    /** Cached RAG service (P1 Context Layer) */
    private volatile IRagService ragService;

    /** Lock for RAG service initialization */
    private final Object ragServiceLock = new Object();

    /** Default conversation memory size */
    private static final int DEFAULT_MEMORY_SIZE = 20;

    /** Provider types that support tool/function calling (non-streaming) in LangChain4j 0.35.0 */
    private static final Set<String> TOOL_SUPPORTED_PROVIDERS = Set.of(
        LangChain4jProviderFactory.PROVIDER_ANTHROPIC,
        LangChain4jProviderFactory.PROVIDER_OPENAI,
        LangChain4jProviderFactory.PROVIDER_BEDROCK,
        LangChain4jProviderFactory.PROVIDER_AI_HUB  // OpenAI-compatible, supports tools
        // NOTE: Ollama/Llama tools depend on model capability, not just provider type.
        // Models like qwen2:0.5b don't support tools. Only certain models like
        // llama3.1, mistral, qwen2.5:7b support tool calling.
        // For simplicity, disable tools for all Ollama models in this version.
        // LangChain4jProviderFactory.PROVIDER_OLLAMA,
        // LangChain4jProviderFactory.PROVIDER_LLAMA
    );

    /** Provider types that support tool/function calling in STREAMING mode (LangChain4j 0.35.0) */
    private static final Set<String> STREAMING_TOOL_SUPPORTED_PROVIDERS = Set.of(
        LangChain4jProviderFactory.PROVIDER_ANTHROPIC,
        LangChain4jProviderFactory.PROVIDER_OPENAI,
        LangChain4jProviderFactory.PROVIDER_BEDROCK,
        LangChain4jProviderFactory.PROVIDER_AI_HUB  // OpenAI-compatible, supports streaming tools
        // Ollama/Llama streaming tools NOT supported in 0.35.0
        // Throws: "Tools are currently not supported by this model"
        // Requires LangChain4j 0.37.0+ (Java 17)
    );

    /**
     * Check if provider supports tool calling (non-streaming).
     * LangChain4j 0.35.0 supports tools for all major providers including Ollama/Llama.
     */
    private static boolean supportsTools(MAIProvider provider) {
        if (provider == null || provider.getAIGProviderType() == null) {
            return false;
        }
        return TOOL_SUPPORTED_PROVIDERS.contains(provider.getAIGProviderType());
    }

    /**
     * Check if provider supports tool calling in streaming mode.
     * LangChain4j 0.35.0 does NOT support streaming tools for Ollama/Llama.
     * Streaming tool support for Ollama was added in LangChain4j 0.37.0.
     */
    private static boolean supportsStreamingTools(MAIProvider provider) {
        if (provider == null || provider.getAIGProviderType() == null) {
            return false;
        }
        return STREAMING_TOOL_SUPPORTED_PROVIDERS.contains(provider.getAIGProviderType());
    }

    /**
     * Check if provider supports streaming at all.
     * LangChain4j 0.35.0 has a bug in OllamaClient streaming that causes NPE.
     * Disable streaming for Ollama/Llama until LangChain4j upgrade (requires Java 17).
     */
    private static boolean supportsStreaming(MAIProvider provider) {
        if (provider == null || provider.getAIGProviderType() == null) {
            return false;
        }
        String providerType = provider.getAIGProviderType();
        // Ollama/Llama streaming is broken in LangChain4j 0.35.0 - NPE in OllamaClient
        if (LangChain4jProviderFactory.PROVIDER_OLLAMA.equals(providerType) ||
            LangChain4jProviderFactory.PROVIDER_LLAMA.equals(providerType)) {
            return false;
        }
        return true;
    }

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

            // Build session ID for the agent
            String sessionId = chat.getCM_Chat_ID() + "-" + memory.getCurrentThreadRootId();
            String response;

            // Check if provider supports tools (non-streaming mode)
            // All major providers including Ollama/Llama support tools in LangChain4j 0.35.0
            if (supportsTools(provider)) {
                // Use full agent with tools
                ERPTools erpTools = new ERPTools(provider, ctx);

                // Build tools list - include RAG tools if available (P1 Context Layer)
                Object[] toolsList;
                IRagService rag = getRagService();
                if (rag != null && rag.isAvailable()) {
                    RagTools ragTools = new RagTools(rag, ctx);
                    toolsList = new Object[] { erpTools, ragTools };
                    log.fine("Agent created with ERPTools + RagTools");
                } else {
                    toolsList = new Object[] { erpTools };
                    log.fine("Agent created with ERPTools only (RAG not available)");
                }

                // Use chatMemoryProvider for @MemoryId support in ERPAgent
                // The provider returns our thread-aware memory for any session ID
                final ThreadAwareChatMemory memoryForProvider = memory;
                ERPAgent agent = AiServices.builder(ERPAgent.class)
                    .chatLanguageModel(model)
                    .tools(toolsList)
                    .chatMemoryProvider(memoryId -> memoryForProvider)
                    .build();

                response = agent.chat(sessionId, processedMessage);
            } else {
                // Simple chat without tools (fallback for unknown providers)
                log.info("Provider " + provider.getAIGProviderType() +
                        " doesn't support tools, using simple chat mode");

                // Use SimpleAgent which has a system prompt without tool instructions
                final ThreadAwareChatMemory memoryForProvider = memory;
                SimpleAgent agent = AiServices.builder(SimpleAgent.class)
                    .chatLanguageModel(model)
                    .chatMemoryProvider(memoryId -> memoryForProvider)
                    .build();

                response = agent.chat(sessionId, processedMessage);
            }

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
            log.log(Level.SEVERE, "ChatWithContext failed: " + errorMsg, e);
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

        // Check if provider supports streaming (Ollama/Llama has bug in LangChain4j 0.35.0)
        if (!supportsStreaming(provider)) {
            log.warning("[STREAM] Provider " + provider.getAIGProviderType() +
                    " doesn't support streaming in LangChain4j 0.35.0, falling back to batch mode");
            // Fall back to non-streaming batch mode
            chatBatchWithStreamingCallback(provider, chat, message, contextData, threadRootId, callback);
            return;
        }

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
            // LANGUAGE DETECTION (ADR-037)
            // Detect input language on first message or when no session override exists.
            // Sets session language based on user's input to maintain consistency.
            // ================================================================

            int chatId = chat.getCM_Chat_ID();
            // clientId and userId already declared above for guardrails
            int roleId = Env.getAD_Role_ID(ctx);
            int orgId = Env.getAD_Org_ID(ctx);

            // Get user and role names for logging
            String userName = Env.getContext(ctx, "#AD_User_Name");
            String roleName = Env.getContext(ctx, "#AD_Role_Name");
            String clientName = Env.getContext(ctx, "#AD_Client_Name");
            String orgName = Env.getContext(ctx, "#AD_Org_Name");

            log.warning("[LANGUAGE] ========================================");
            log.warning("[LANGUAGE] LANGUAGE DETECTION for chat " + chatId);
            log.warning("[LANGUAGE] Tenant: " + clientName + " (ID=" + clientId + ")");
            log.warning("[LANGUAGE] User: " + userName + " (ID=" + userId + ")");
            log.warning("[LANGUAGE] Role: " + roleName + " (ID=" + roleId + ")");
            log.warning("[LANGUAGE] Org: " + orgName + " (ID=" + orgId + ")");
            log.warning("[LANGUAGE] ========================================");

            // Get login/system language from iDempiere context
            String loginLanguage = Env.getAD_Language(ctx);
            org.compiere.util.Language loginLangObj = org.compiere.util.Language.getLanguage(loginLanguage);
            String loginLangName = loginLangObj != null ? loginLangObj.getName() : loginLanguage;
            log.warning("[LANGUAGE] Login/System language: " + loginLangName + " (" + loginLanguage + ")");

            // Get current session language state (before any changes)
            String currentLang = languageService.getSessionLanguage(ctx, chatId);
            org.compiere.util.Language currentLangObj = org.compiere.util.Language.getLanguage(currentLang);
            String currentLangName = currentLangObj != null ? currentLangObj.getName() : currentLang;
            boolean hasOverride = languageService.hasOverrideLanguage(chatId);
            log.warning("[LANGUAGE] Current session language: " + currentLangName + " (" + currentLang + ")");
            log.warning("[LANGUAGE] Has override: " + hasOverride);

            // Check if current differs from login
            if (!currentLang.equals(loginLanguage)) {
                log.warning("[LANGUAGE] ⚠ LANGUAGE MISMATCH: Session (" + currentLang + ") != Login (" + loginLanguage + ")");
                if (hasOverride) {
                    log.warning("[LANGUAGE] ⚠ Reason: Session override is active");
                } else {
                    log.warning("[LANGUAGE] ⚠ Reason: Unknown - this should not happen!");
                }
            }

            if (!hasOverride) {
                // No session language set yet - detect from input
                log.warning("[LANGUAGE] No override set - attempting auto-detection from message");
                log.warning("[LANGUAGE] Message preview: " + processedMessage.substring(0, Math.min(200, processedMessage.length())));

                java.util.Optional<String> detectedLang = languageService.detectInputLanguage(processedMessage);
                if (detectedLang.isPresent()) {
                    String detected = detectedLang.get();
                    org.compiere.util.Language detectedLangObj = org.compiere.util.Language.getLanguage(detected);
                    String detectedLangName = detectedLangObj != null ? detectedLangObj.getName() : detected;

                    log.warning("[LANGUAGE] ✓ Auto-detected: " + detectedLangName + " (" + detected + ")");

                    // Check if detected differs from login
                    if (!detected.equals(loginLanguage)) {
                        log.warning("[LANGUAGE] ⚠ CHANGE: Auto-detected (" + detected + ") != Login (" + loginLanguage + ")");
                        log.warning("[LANGUAGE] ⚠ This will override the user's login language!");
                    } else {
                        log.warning("[LANGUAGE] ✓ Auto-detected matches login language");
                    }

                    // setOverrideLanguage will log the language switch if there was a previous one
                    languageService.setOverrideLanguage(chatId, detected);
                    callback.onProgress("language", "Language detected: " + detectedLangName);
                } else {
                    log.warning("[LANGUAGE] ✗ Could not auto-detect language from input");
                    log.warning("[LANGUAGE] ✓ Will use login language: " + loginLangName);
                }
            } else {
                log.warning("[LANGUAGE] Override already exists - checking for explicit language change requests");
            }

            // Always check for explicit language change requests
            java.util.Optional<String> requestedLang = languageService.detectLanguageChangeRequest(processedMessage);
            if (requestedLang.isPresent()) {
                String requested = requestedLang.get();
                org.compiere.util.Language requestedLangObj = org.compiere.util.Language.getLanguage(requested);
                String requestedLangName = requestedLangObj != null ? requestedLangObj.getName() : requested;

                log.warning("[LANGUAGE] ✓ User explicitly requested: " + requestedLangName + " (" + requested + ")");

                // Check if requested differs from current session language
                String previousSessionLang = languageService.getSessionLanguage(ctx, chatId);
                if (!requested.equals(previousSessionLang)) {
                    log.warning("[LANGUAGE] ⚠ USER INITIATED SWITCH: " + previousSessionLang + " → " + requested);
                } else {
                    log.warning("[LANGUAGE] ✓ User requested matches current session language (no change)");
                }

                // Check if requested differs from login
                if (!requested.equals(loginLanguage)) {
                    log.warning("[LANGUAGE] ⚠ Requested language (" + requested + ") != Login (" + loginLanguage + ")");
                } else {
                    log.warning("[LANGUAGE] ✓ User requested matches login language");
                }

                // setOverrideLanguage will log the language switch detection
                languageService.setOverrideLanguage(chatId, requested);
                callback.onProgress("language", "Switching to: " + requestedLangName);
            }

            // Final language state - show complete trace
            String finalLang = languageService.getSessionLanguage(ctx, chatId);
            org.compiere.util.Language finalLangObj = org.compiere.util.Language.getLanguage(finalLang);
            String finalLangName = finalLangObj != null ? finalLangObj.getName() : finalLang;

            log.warning("[LANGUAGE] ========================================");
            log.warning("[LANGUAGE] LANGUAGE TRACE SUMMARY:");
            log.warning("[LANGUAGE]   Login/System: " + loginLangName + " (" + loginLanguage + ")");
            log.warning("[LANGUAGE]   Final/Active: " + finalLangName + " (" + finalLang + ")");

            if (!finalLang.equals(loginLanguage)) {
                log.warning("[LANGUAGE]   ⚠ CHANGED FROM LOGIN LANGUAGE!");
                if (requestedLang.isPresent()) {
                    log.warning("[LANGUAGE]   Reason: User explicitly requested");
                } else if (languageService.hasOverrideLanguage(chatId)) {
                    log.warning("[LANGUAGE]   Reason: Auto-detected from input");
                } else {
                    log.warning("[LANGUAGE]   Reason: UNKNOWN - INVESTIGATE!");
                }
            } else {
                log.warning("[LANGUAGE]   ✓ Using login language");
            }

            // Validate final language
            if (finalLangObj == null) {
                log.warning("[LANGUAGE]   ⚠⚠⚠ ERROR: Unknown language code: " + finalLang);
                log.warning("[LANGUAGE]   ⚠⚠⚠ This may cause response issues!");
            }

            log.warning("[LANGUAGE] ========================================");

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
            // STREAMING AI CALL (ADR-033)
            // ================================================================

            StreamingChatLanguageModel streamingModel = getOrCreateStreamingModel(provider);

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

            // Build session ID for the agent
            String sessionId = chat.getCM_Chat_ID() + "-" + memory.getCurrentThreadRootId();
            final ThreadAwareChatMemory memoryForProvider = memory;

            // Check if provider supports streaming tools
            // Note: Ollama/Llama support tools in non-streaming mode only (LangChain4j 0.35.0)
            // Streaming tools for Ollama requires LangChain4j 0.37.0+
            dev.langchain4j.service.TokenStream tokenStream;

            if (supportsStreamingTools(provider)) {
                // ================================================================
                // BUILD STREAMING AGENT WITH TOOLS (for Anthropic, OpenAI, Bedrock)
                // ================================================================
                log.warning("[STREAM] Building ERPStreamingAgent with tools...");

                // Create tools with optional callback support (unified ERPTools)
                ERPTools erpTools = new ERPTools(provider, ctx, callback);
                log.warning("[STREAM] ERPTools class: " + erpTools.getClass().getName());
                log.warning("[STREAM] StreamingModel class: " + streamingModel.getClass().getName());

                // Build tools list - include RAG tools if available (P1 Context Layer)
                Object[] toolsList;
                IRagService rag = getRagService();
                if (rag != null && rag.isAvailable()) {
                    RagTools ragTools = new RagTools(rag, ctx);
                    toolsList = new Object[] { erpTools, ragTools };
                    log.warning("[STREAM] Agent created with ERPTools + RagTools");
                } else {
                    toolsList = new Object[] { erpTools };
                    log.warning("[STREAM] Agent created with ERPTools only (RAG not available)");
                }

                // Build system prompt with language instruction (ADR-037)
                final String systemPrompt = buildSystemPromptWithLanguage(ctx, chat.getCM_Chat_ID(), true);
                log.warning("[STREAM] System prompt built, length=" + systemPrompt.length());

                ERPStreamingAgent agent = AiServices.builder(ERPStreamingAgent.class)
                    .streamingChatLanguageModel(streamingModel)
                    .tools(toolsList)
                    .chatMemoryProvider(memoryId -> memoryForProvider)
                    .systemMessageProvider(memoryId -> systemPrompt)
                    .build();

                log.warning("[STREAM] Agent built successfully: " + agent.getClass().getName());
                log.warning("[STREAM] Starting TokenStream with sessionId: " + sessionId);
                log.warning("[STREAM] Memory has " + memoryForProvider.getMessageCount() + " messages before agent.chat()");

                // Get TokenStream from agent
                tokenStream = agent.chat(sessionId, processedMessage);
            } else {
                // ================================================================
                // SIMPLE STREAMING WITHOUT TOOLS (for Ollama/Llama)
                // ================================================================
                log.warning("[STREAM] Provider " + provider.getAIGProviderType() +
                        " doesn't support tools, using simple streaming chat mode");
                log.warning("[STREAM] StreamingModel class: " + streamingModel.getClass().getName());

                // Build system prompt with language instruction (ADR-037) - no tools
                final String systemPrompt = buildSystemPromptWithLanguage(ctx, chat.getCM_Chat_ID(), false);
                log.warning("[STREAM] System prompt built (no tools), length=" + systemPrompt.length());

                // Build SimpleStreamingAgent (no tools, simplified system prompt)
                SimpleStreamingAgent agent = AiServices.builder(SimpleStreamingAgent.class)
                    .streamingChatLanguageModel(streamingModel)
                    .chatMemoryProvider(memoryId -> memoryForProvider)
                    .systemMessageProvider(memoryId -> systemPrompt)
                    .build();

                log.warning("[STREAM] SimpleStreamingAgent built successfully: " + agent.getClass().getName());
                log.warning("[STREAM] Starting TokenStream with sessionId: " + sessionId);
                log.warning("[STREAM] Memory has " + memoryForProvider.getMessageCount() + " messages before agent.chat()");

                // Get TokenStream from agent
                tokenStream = agent.chat(sessionId, processedMessage);
            }

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
            log.log(Level.SEVERE, "ChatStreamingWithContext failed: " + errorMsg, e);
            callback.onError(e);
        }
    }

    /**
     * Fallback batch chat method that emulates streaming callbacks.
     *
     * <p>Used when the provider doesn't support streaming (e.g., Ollama in LangChain4j 0.35.0
     * has a bug that causes NPE during streaming). This method uses the batch API but
     * calls the streaming callbacks to maintain UI compatibility.
     *
     * @param provider AI Provider configuration
     * @param chat Parent MChat for message persistence
     * @param message User message
     * @param contextData Optional context from window/tab (JSON)
     * @param threadRootId Thread root ID (0 for new thread)
     * @param callback Streaming callback for progress updates
     */
    private void chatBatchWithStreamingCallback(MAIProvider provider, MChat chat,
                                                 String message, JSONObject contextData,
                                                 int threadRootId, AIStreamCallback callback) {
        log.warning("[BATCH-STREAM] Using batch mode with streaming callback (provider: " +
                provider.getAIGProviderType() + ")");

        try {
            // Signal progress
            callback.onProgress("analyzing", "Processing your request...");

            // Call the batch API
            ChatResult result = chatWithContext(provider, chat, message, contextData, threadRootId);

            if (result.isBlocked()) {
                // Blocked by guardrails
                callback.onError(new RuntimeException("Blocked: " + result.getViolationType()));
                return;
            }

            if (!result.isSuccess()) {
                callback.onError(new RuntimeException(result.getResponse()));
                return;
            }

            // Send the complete response as a single chunk
            String response = result.getResponse();
            if (response != null && !response.isEmpty()) {
                callback.onChunk(response);
            }

            // Signal completion
            callback.onComplete();

        } catch (Exception e) {
            log.severe("[BATCH-STREAM] Error: " + e.getMessage());
            callback.onError(e);
        }
    }

    // ========================================================================
    // Blocking Chat (Streaming-First Wrapper)
    // ========================================================================

    /**
     * Blocking chat method that wraps streaming using CompletableFuture.
     *
     * <p>This method provides a synchronous API built on top of the streaming
     * implementation. It blocks until the streaming response is complete.
     *
     * <p>Use this method for:
     * <ul>
     *   <li>Programmatic/batch processing</li>
     *   <li>API integrations that need the full response</li>
     *   <li>Tests that don't need streaming</li>
     * </ul>
     *
     * <p>For interactive UI, prefer {@link #chatStreamingWithContext} instead.
     *
     * @param provider AI Provider configuration
     * @param chat Parent MChat for message persistence
     * @param message User message
     * @param contextData Optional context from window/tab (JSON)
     * @param threadRootId Thread root ID (0 for new thread)
     * @return ChatResult with response and thread info
     */
    public ChatResult chatBlocking(MAIProvider provider, MChat chat,
                                    String message, JSONObject contextData,
                                    int threadRootId) {
        log.info("AIService.chatBlocking: chat=" + (chat != null ? chat.getCM_Chat_ID() : "null") +
                ", thread=" + threadRootId);

        // Use CompletableFuture to block on streaming response
        CompletableFuture<ChatResult> future = new CompletableFuture<>();
        StringBuilder responseAccumulator = new StringBuilder();
        AtomicReference<String> warningRef = new AtomicReference<>();

        AIStreamCallback blockingCallback = AIStreamCallback.builder()
            .onChunk(responseAccumulator::append)
            .onComplete(() -> {
                String response = responseAccumulator.toString();
                // Apply output guardrails on complete response
                String warningMessage = null;
                if (guardrailsEnabled && response != null && !response.isEmpty()) {
                    GuardResult outputResult = outputGuard.validate(response);
                    if (outputResult.isBlocked()) {
                        future.complete(ChatResult.blocked(
                            "I apologize, but I cannot provide that response.",
                            threadRootId, outputResult.getViolationType()));
                        return;
                    }
                    if (outputResult.wasModified()) {
                        response = outputResult.getProcessedContent();
                        warningMessage = "Some content was filtered for safety.";
                    }
                }
                future.complete(ChatResult.success(response, threadRootId, warningMessage));
            })
            .onError(error -> {
                future.completeExceptionally(error);
            })
            .build();

        // Delegate to streaming method
        chatStreamingWithContext(provider, chat, message, contextData, threadRootId, blockingCallback);

        // Block until complete
        try {
            return future.join();
        } catch (Exception e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            log.severe("chatBlocking failed: " + errorMsg);
            return ChatResult.error("AI chat failed: " + errorMsg, threadRootId);
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
     * Build system prompt with language instruction prepended.
     *
     * <p>This method combines the language instruction (from LanguageDetectionService)
     * with the appropriate base system prompt (ERPStreamingAgent or SimpleStreamingAgent).
     *
     * <p>The language instruction is placed FIRST in the system prompt to ensure
     * the AI prioritizes language compliance. This implements ADR-037.
     *
     * @param ctx iDempiere context (contains AD_Language from user login)
     * @param chatId Chat ID for session override lookup
     * @param withTools true for ERPStreamingAgent (tool support), false for SimpleStreamingAgent
     * @return Complete system prompt with language instruction
     */
    private String buildSystemPromptWithLanguage(Properties ctx, int chatId, boolean withTools) {
        // Get language instruction based on session language (override > context > fallback)
        String languageInstruction = languageService.getLanguageInstruction(ctx, chatId);

        // Detect and log database type
        String dbType = "Unknown";
        if (org.compiere.util.DB.isPostgreSQL()) {
            dbType = "PostgreSQL";
        } else if (org.compiere.util.DB.isOracle()) {
            dbType = "Oracle";
        }
        log.warning("[DATABASE] Detected database type: " + dbType);

        // Get base system prompt with database-specific SQL syntax guidance
        String basePrompt = withTools
            ? DatabaseSyntaxHelper.appendDatabaseGuidance(ERPAgent.SYSTEM_PROMPT)
            : SimpleStreamingAgent.SIMPLE_SYSTEM_PROMPT;

        // If no language instruction, just return the base prompt
        if (languageInstruction == null || languageInstruction.isEmpty()) {
            log.warning("[LANGUAGE] No language instruction - using base prompt only");
            log.warning("[DATABASE] System prompt includes " + dbType + "-specific SQL syntax guidance");
            return basePrompt;
        }

        // Log the language instruction being used
        String sessionLang = languageService.getSessionLanguage(ctx, chatId);
        org.compiere.util.Language langObj = org.compiere.util.Language.getLanguage(sessionLang);
        String langName = langObj != null ? langObj.getName() : sessionLang;
        log.warning("[LANGUAGE] System prompt language: " + langName + " (" + sessionLang + ")");
        log.warning("[LANGUAGE] Language instruction length: " + languageInstruction.length() + " chars");
        log.warning("[DATABASE] System prompt includes " + dbType + "-specific SQL syntax guidance");

        // Combine: language instruction FIRST, then base prompt
        // This ensures language compliance is prioritized
        return languageInstruction + "\n\n" + basePrompt;
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
            String response;

            // Check if provider supports tools (non-streaming mode)
            if (supportsTools(provider)) {
                ERPTools erpTools = new ERPTools(provider, ctx);

                // Build tools list - include RAG tools if available (P1 Context Layer)
                Object[] toolsList;
                IRagService rag = getRagService();
                if (rag != null && rag.isAvailable()) {
                    RagTools ragTools = new RagTools(rag, ctx);
                    toolsList = new Object[] { erpTools, ragTools };
                    log.fine("Execute: Agent created with ERPTools + RagTools");
                } else {
                    toolsList = new Object[] { erpTools };
                    log.fine("Execute: Agent created with ERPTools only (RAG not available)");
                }

                ERPAgent agent = AiServices.builder(ERPAgent.class)
                    .chatLanguageModel(model)
                    .tools(toolsList)
                    .build();

                response = agent.execute(processedGoal);
            } else {
                // Simple execution without tools (fallback for unknown providers)
                log.info("Provider " + provider.getAIGProviderType() +
                        " doesn't support tools, using SimpleAgent");

                SimpleAgent agent = AiServices.builder(SimpleAgent.class)
                    .chatLanguageModel(model)
                    .build();

                response = agent.execute(processedGoal);
            }

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

        // Check if provider supports tools (non-streaming mode)
        if (supportsTools(provider)) {
            ERPTools erpTools = new ERPTools(provider, ctx);

            // Build tools list - include RAG tools if available (P1 Context Layer)
            Object[] toolsList;
            IRagService rag = getRagService();
            if (rag != null && rag.isAvailable()) {
                RagTools ragTools = new RagTools(rag, ctx);
                toolsList = new Object[] { erpTools, ragTools };
                log.fine("getOrCreateAgent: Agent created with ERPTools + RagTools");
            } else {
                toolsList = new Object[] { erpTools };
                log.fine("getOrCreateAgent: Agent created with ERPTools only (RAG not available)");
            }

            return AiServices.builder(ERPAgent.class)
                .chatLanguageModel(model)
                .tools(toolsList)
                .chatMemory(memory)
                .build();
        } else {
            // Use SimpleAgent (fallback for unknown providers)
            log.info("Provider " + provider.getAIGProviderType() +
                    " doesn't support tools, creating SimpleAgent");
            // Return a wrapper that adapts SimpleAgent to ERPAgent interface
            SimpleAgent simpleAgent = AiServices.builder(SimpleAgent.class)
                .chatLanguageModel(model)
                .chatMemory(memory)
                .build();
            // Wrap SimpleAgent in ERPAgent adapter
            return new ERPAgent() {
                @Override
                public String chat(String sessionId, String userMessage) {
                    return simpleAgent.chat(sessionId, userMessage);
                }
                @Override
                public String execute(String goal) {
                    return simpleAgent.execute(goal);
                }
            };
        }
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

    // ========================================================================
    // RAG Service Access (P1 Context Layer)
    // ========================================================================

    /**
     * Get the RAG service for knowledge retrieval.
     *
     * <p>Uses lazy initialization with OSGi service lookup.
     * Returns null if RAG service is not available.
     *
     * @return IRagService instance or null
     */
    private IRagService getRagService() {
        if (ragService != null) {
            return ragService;
        }

        synchronized (ragServiceLock) {
            if (ragService == null) {
                try {
                    Bundle bundle = FrameworkUtil.getBundle(AIService.class);
                    if (bundle != null) {
                        BundleContext context = bundle.getBundleContext();
                        if (context != null) {
                            ServiceReference<IRagService> ref = context.getServiceReference(IRagService.class);
                            if (ref != null) {
                                ragService = context.getService(ref);
                                if (ragService != null) {
                                    log.info("RAG service initialized: available=" + ragService.isAvailable());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warning("Failed to get RAG service: " + e.getMessage());
                }
            }
        }

        return ragService;
    }

    /**
     * Check if RAG service is available.
     *
     * @return true if RAG service is available and configured
     */
    public boolean isRagAvailable() {
        IRagService rag = getRagService();
        return rag != null && rag.isAvailable();
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
