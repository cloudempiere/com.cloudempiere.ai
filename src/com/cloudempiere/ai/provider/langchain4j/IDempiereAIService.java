package com.cloudempiere.ai.provider.langchain4j;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.model.MAIProvider;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;

/**
 * Main entry point for iDempiere AI capabilities using LangChain4j.
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
 * IDempiereAIService aiService = IDempiereAIService.getInstance();
 * String response = aiService.chat(provider, ctx, sessionId, "Show me pending orders");
 * </pre>
 *
 * @author Cloudempiere
 * @version 0.9.0
 * @since ADR-002 LangChain4j Strategic Adoption
 */
public class IDempiereAIService {

    private static final CLogger log = CLogger.getCLogger(IDempiereAIService.class);

    /** Singleton instance */
    private static IDempiereAIService instance;

    /** Agent cache by provider ID */
    private final Map<Integer, IDempiereAgent> agentCache = new ConcurrentHashMap<>();

    /** Memory cache by session ID */
    private final Map<String, MessageWindowChatMemory> memoryCache = new ConcurrentHashMap<>();

    /** Default conversation memory size */
    private static final int DEFAULT_MEMORY_SIZE = 20;

    private IDempiereAIService() {
        // Private constructor for singleton
    }

    /**
     * Get singleton instance.
     */
    public static synchronized IDempiereAIService getInstance() {
        if (instance == null) {
            instance = new IDempiereAIService();
        }
        return instance;
    }

    /**
     * Chat with the AI agent.
     *
     * @param provider AI Provider configuration
     * @param ctx iDempiere context
     * @param sessionId Session ID for conversation memory
     * @param message User message
     * @return AI response
     */
    public String chat(MAIProvider provider, Properties ctx, String sessionId, String message) {
        log.info("IDempiereAIService.chat: session=" + sessionId + ", message=" + message.substring(0, Math.min(50, message.length())));

        try {
            IDempiereAgent agent = getOrCreateAgent(provider, ctx, sessionId);
            return agent.chat(sessionId, message);
        } catch (Exception e) {
            log.severe("Chat failed: " + e.getMessage());
            throw new RuntimeException("AI chat failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a one-shot task without conversation memory.
     *
     * @param provider AI Provider configuration
     * @param ctx iDempiere context
     * @param goal Task/goal to execute
     * @return AI response
     */
    public String execute(MAIProvider provider, Properties ctx, String goal) {
        log.info("IDempiereAIService.execute: " + goal.substring(0, Math.min(50, goal.length())));

        try {
            // Create agent without memory for one-shot execution
            ChatLanguageModel model = LangChain4jProviderFactory.create(provider);
            ERPTools tools = new ERPTools(provider, ctx);

            IDempiereAgent agent = AiServices.builder(IDempiereAgent.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();

            return agent.execute(goal);
        } catch (Exception e) {
            log.severe("Execute failed: " + e.getMessage());
            throw new RuntimeException("AI execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get or create an agent for the given provider and session.
     */
    private IDempiereAgent getOrCreateAgent(MAIProvider provider, Properties ctx, String sessionId) {
        int providerId = provider.getAIG_Provider_ID();

        // Get or create memory for this session
        MessageWindowChatMemory memory = memoryCache.computeIfAbsent(sessionId,
            id -> MessageWindowChatMemory.withMaxMessages(DEFAULT_MEMORY_SIZE));

        // Create agent (agents are stateless, memory is per-session)
        ChatLanguageModel model = LangChain4jProviderFactory.getOrCreate(provider);
        ERPTools tools = new ERPTools(provider, ctx);

        return AiServices.builder(IDempiereAgent.class)
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
}
