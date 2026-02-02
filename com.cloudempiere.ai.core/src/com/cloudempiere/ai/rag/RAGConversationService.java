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
package com.cloudempiere.ai.rag;

import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.json.JSONArray;
import org.json.JSONObject;

// TEMPORARILY DISABLED - Phase 3: Language detection service not yet migrated
// import com.cloudempiere.ai.service.LanguageDetectionService;

import com.cloudempiere.ai.model.MAIChat;
import com.cloudempiere.ai.model.MAIPromptConfig;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.ERPAgent;
import com.cloudempiere.ai.provider.langchain4j.ERPTools;
import com.cloudempiere.ai.provider.langchain4j.LangChain4jProviderFactory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;

/**
 * RAG-based AI Conversation Service (ADR-012).
 *
 * <p><strong>STATUS: NOT YET WIRED</strong> - This class is implemented but not currently
 * used in production. The actual chat flow uses {@link com.cloudempiere.ai.provider.langchain4j.AIService}
 * which is called by AIChatWidget.
 *
 * <p><strong>Current Production Flow:</strong>
 * <pre>
 * AIChatWidget → AIService → ERPStreamingAgent/SimpleStreamingAgent
 * </pre>
 *
 * <p><strong>Planned Future Flow (when RAG is wired):</strong>
 * <pre>
 * AIChatWidget → RAGConversationService → RAGContextManager → ERPAgent
 * </pre>
 *
 * <p>See CLAUDE/CLD-LANGCHAIN-CHAT/IMPLEMENTATION_PLAN.md for wiring status.
 *
 * <hr>
 *
 * <p>This service replaces the custom routing logic from ADR-005 with
 * LangChain4j's RAG (Retrieval-Augmented Generation) pattern.
 *
 * <p>Key improvements over legacy AIConversationService:
 * <ul>
 *   <li>Semantic search instead of regex pattern matching</li>
 *   <li>Automatic context injection via ContentRetriever</li>
 *   <li>Built-in conversation memory management</li>
 *   <li>86% less code than custom routing</li>
 * </ul>
 *
 * <p>Architecture:
 * <pre>
 * User Message
 *     ↓
 * RAGContextManager (semantic search)
 *     ↓
 * ContentRetriever (finds relevant context)
 *     ↓
 * ERPAgent (LangChain4j AiServices)
 *     ↓
 * ERPTools (@Tool methods for database access)
 *     ↓
 * AI Response
 * </pre>
 *
 * @author Cloudempiere AI Team
 * @version ADR-012
 * @since v0.10.0
 * @see com.cloudempiere.ai.provider.langchain4j.AIService AIService (currently active)
 */
public class RAGConversationService {

    private static final CLogger log = CLogger.getCLogger(RAGConversationService.class);

    /** Default maximum history entries */
    private static final int DEFAULT_MAX_HISTORY = 10;

    /** Default provider ID (temporary until provider selection UI) */
    private static final int DEFAULT_PROVIDER_ID = 1000001;

    /** RAG context manager for semantic search */
    private RAGContextManager ragContextManager;

    /** Language detection service (ADR-037) - TEMPORARILY DISABLED until Phase 3 */
    // private final LanguageDetectionService languageService;

    /** Metrics tracking */
    private final RAGMetrics metrics;

    /**
     * Create RAGConversationService.
     *
     * <p>The RAGContextManager will be lazily initialized when needed,
     * using the provider configuration from the database.
     */
    public RAGConversationService() {
        // TEMPORARILY DISABLED - Phase 3: Language detection not yet migrated
        // this.languageService = LanguageDetectionService.getInstance();
        this.metrics = new RAGMetrics();
        log.info("RAGConversationService created (RAG will be initialized on first use)");
    }

    /**
     * Create RAGConversationService with pre-configured RAG context manager.
     *
     * @param ragContextManager Pre-configured RAG context manager
     */
    public RAGConversationService(RAGContextManager ragContextManager) {
        this.ragContextManager = ragContextManager;
        // TEMPORARILY DISABLED - Phase 3: Language detection not yet migrated
        // this.languageService = LanguageDetectionService.getInstance();
        this.metrics = new RAGMetrics();

        log.info("RAGConversationService initialized: " +
                "ragAvailable=" + ragContextManager.isAvailable() +
                ", fallbackMode=" + ragContextManager.isFallbackMode());
    }

    /**
     * Get or create the RAG context manager using provider configuration.
     *
     * @param ctx iDempiere context
     * @param trxName Transaction name
     * @return RAGContextManager instance
     */
    private RAGContextManager getOrCreateRAGContextManager(Properties ctx, String trxName) {
        if (ragContextManager == null) {
            synchronized (this) {
                if (ragContextManager == null) {
                    MAIProvider provider = MAIProvider.get(ctx, DEFAULT_PROVIDER_ID, trxName);
                    if (provider != null) {
                        ragContextManager = new RAGContextManager(provider);
                        log.info("RAGContextManager initialized from provider: " + provider.getName());
                    } else {
                        // Create with null provider - will run in fallback mode
                        ragContextManager = new RAGContextManager((MAIProvider) null);
                        log.warning("No AI provider found - RAG running in fallback mode");
                    }
                }
            }
        }
        return ragContextManager;
    }

    /**
     * Send message and get AI response using RAG-based context retrieval
     *
     * @param ctx iDempiere context
     * @param chat Chat instance
     * @param userMessage User's message
     * @param trxName Transaction name
     * @return AI response as string
     */
    public String sendMessage(Properties ctx, MAIChat chat, String userMessage, String trxName) {
        return sendMessageWithContext(ctx, chat, userMessage, null, DEFAULT_MAX_HISTORY, trxName);
    }

    /**
     * Send message with context using RAG-based retrieval
     *
     * @param ctx iDempiere context
     * @param chat Chat instance
     * @param userMessage User's message
     * @param windowContext Optional window context (JSON)
     * @param maxHistoryEntries Maximum history entries to include
     * @param trxName Transaction name
     * @return AI response as string
     */
    public String sendMessageWithContext(
            Properties ctx,
            MAIChat chat,
            String userMessage,
            JSONObject windowContext,
            int maxHistoryEntries,
            String trxName) {

        long startTime = System.currentTimeMillis();
        String sessionId = getSessionId(chat);
        int chatId = chat != null ? chat.getCM_Chat_ID() : 0;

        try {
            // Ensure RAG context manager is initialized
            RAGContextManager ragManager = getOrCreateRAGContextManager(ctx, trxName);

            // 0. Check for language change request (ADR-037) - TEMPORARILY DISABLED until Phase 3
            // String languageAcknowledgment = processLanguageChangeRequest(userMessage, chatId);

            // 1. Store window context in RAG (if provided)
            if (windowContext != null) {
                storeWindowContext(sessionId, windowContext, ragManager);
            }

            // 2. Create RAG-enabled agent with current session language
            ERPAgent agent = createRAGAgent(ctx, chatId, sessionId, trxName, ragManager);

            // 3. Build conversation with history
            String enhancedMessage = buildEnhancedMessage(ctx, userMessage, windowContext);

            // 4. Execute through LangChain4j agent
            //    The agent automatically:
            //    - Retrieves relevant context via ContentRetriever
            //    - Calls @Tool methods when needed
            //    - Manages conversation memory
            String response = agent.chat(sessionId, enhancedMessage);

            // 5. Prepend language acknowledgment if language was changed - TEMPORARILY DISABLED
            // if (languageAcknowledgment != null) {
            //     response = languageAcknowledgment + "\n\n" + response;
            // }

            // 6. Store the interaction in RAG for future reference
            storeInteraction(sessionId, userMessage, response, ragManager);

            // 7. Track metrics
            long responseTime = System.currentTimeMillis() - startTime;
            metrics.recordSuccess(responseTime);

            // TEMPORARILY DISABLED - Phase 3: Language detection not yet migrated
            log.fine("RAG response generated: session=" + sessionId +
                    ", time=" + responseTime + "ms");

            return response;

        } catch (Exception e) {
            log.log(Level.SEVERE, "RAG conversation failed", e);
            metrics.recordError();

            return "I apologize, but I encountered an error processing your request. " +
                   "Please try again or contact support. Error: " + e.getMessage();
        }
    }

    /**
     * Process language change request from user message (ADR-037).
     * TEMPORARILY DISABLED - Phase 3: Language detection service not yet migrated
     *
     * <p>Detects patterns like "respond in German" and sets the session language override.</p>
     *
     * @param userMessage User's message
     * @param chatId Chat ID for session storage
     * @return Acknowledgment message if language changed, null otherwise
     */
    /*
    private String processLanguageChangeRequest(String userMessage, int chatId) {
        if (chatId <= 0) {
            return null;
        }

        Optional<String> requestedLang = languageService.detectLanguageChangeRequest(userMessage);
        if (requestedLang.isPresent()) {
            String langCode = requestedLang.get();
            languageService.setOverrideLanguage(chatId, langCode);
            return languageService.getLanguageChangeAcknowledgment(langCode);
        }

        return null;
    }
    */

    /**
     * Create a RAG-enabled agent using LangChain4j AiServices
     *
     * @param ctx iDempiere context
     * @param chatId Chat ID for language detection
     * @param sessionId Session identifier
     * @param trxName Transaction name
     * @param ragManager RAG context manager
     * @return Configured ERPAgent
     */
    private ERPAgent createRAGAgent(Properties ctx, int chatId, String sessionId, String trxName,
                                          RAGContextManager ragManager) {
        // Get provider for chat model and tools
        MAIProvider provider = MAIProvider.get(ctx, DEFAULT_PROVIDER_ID, trxName);
        if (provider == null) {
            throw new IllegalStateException("AI Provider not found: " + DEFAULT_PROVIDER_ID);
        }

        // Get chat model from provider factory
        ChatLanguageModel chatModel = LangChain4jProviderFactory.getOrCreate(provider);
        if (chatModel == null) {
            throw new IllegalStateException("Failed to create chat language model");
        }

        // Create ERP tools
        ERPTools erpTools = new ERPTools(provider, ctx);

        // Get content retriever for RAG
        ContentRetriever retriever = ragManager.getRetriever(sessionId);

        // Build system prompt - TEMPORARILY DISABLED: session language (ADR-037) until Phase 3
        String systemPrompt = buildSystemPrompt(ctx, 0);

        // Create agent with RAG integration
        return AiServices.builder(ERPAgent.class)
            .chatLanguageModel(chatModel)
            .tools(erpTools)
            .contentRetriever(retriever)  // <-- RAG integration (replaces PromptAnalyzer)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(DEFAULT_MAX_HISTORY))
            .systemMessageProvider(memoryId -> systemPrompt)
            .build();
    }

    /**
     * Store window context in RAG for semantic retrieval
     *
     * @param sessionId Session identifier
     * @param windowContext Window context JSON
     * @param ragManager RAG context manager
     */
    private void storeWindowContext(String sessionId, JSONObject windowContext, RAGContextManager ragManager) {
        try {
            // Extract and store record data
            if (windowContext.has("record_data")) {
                JSONObject recordData = windowContext.getJSONObject("record_data");
                String key = "window_record_" + System.currentTimeMillis();
                ragManager.addContext(sessionId, key, formatRecordData(recordData));
            }

            // Extract and store tab context
            if (windowContext.has("tab_context")) {
                JSONObject tabContext = windowContext.getJSONObject("tab_context");
                String key = "window_tab_" + System.currentTimeMillis();
                ragManager.addContext(sessionId, key, formatTabContext(tabContext));
            }

            // Store child tabs data
            if (windowContext.has("child_tabs")) {
                JSONArray childTabs = windowContext.getJSONArray("child_tabs");
                for (int i = 0; i < childTabs.length(); i++) {
                    JSONObject childTab = childTabs.getJSONObject(i);
                    String key = "window_child_" + i + "_" + System.currentTimeMillis();
                    ragManager.addContext(sessionId, key, formatChildTab(childTab));
                }
            }

            log.fine("Stored window context in RAG: session=" + sessionId);

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to store window context in RAG", e);
        }
    }

    /**
     * Store an interaction for future context retrieval
     *
     * @param sessionId Session identifier
     * @param userMessage User's message
     * @param aiResponse AI's response
     * @param ragManager RAG context manager
     */
    private void storeInteraction(String sessionId, String userMessage, String aiResponse,
                                  RAGContextManager ragManager) {
        try {
            // Store as Q&A pair for better retrieval
            String interaction = "Question: " + userMessage + "\nAnswer: " + aiResponse;
            String key = "interaction_" + System.currentTimeMillis();
            ragManager.addContext(sessionId, key, interaction);

        } catch (Exception e) {
            log.log(Level.FINE, "Failed to store interaction", e);
        }
    }

    /**
     * Build enhanced message with language context
     *
     * @param ctx iDempiere context
     * @param userMessage Original user message
     * @param windowContext Optional window context
     * @return Enhanced message
     */
    private String buildEnhancedMessage(Properties ctx, String userMessage, JSONObject windowContext) {
        // For now, return the message as-is
        // The RAG retriever will automatically find and inject relevant context
        return userMessage;
    }

    /**
     * Build system prompt for the agent
     * TEMPORARILY DISABLED - Phase 3: Language detection not yet migrated, chatId parameter not used
     *
     * @param ctx iDempiere context
     * @param chatId Chat ID for language detection (currently unused)
     * @return System prompt string
     */
    private String buildSystemPrompt(Properties ctx, int chatId) {
        // Try to load from database first
        String dbPrompt = loadSystemPromptFromDatabase();

        // TEMPORARILY DISABLED - Phase 3: Language instruction not yet migrated
        // Get language instruction using the language service (ADR-037)
        // Place at BEGINNING for stronger compliance
        // String languageInstruction = languageService.getLanguageInstruction(ctx, chatId);

        StringBuilder sb = new StringBuilder();

        // TEMPORARILY DISABLED - Phase 3: Language instruction
        // Language instruction FIRST for maximum compliance (ADR-037)
        // if (!languageInstruction.isEmpty()) {
        //     sb.append(languageInstruction.trim());
        //     sb.append("\n\n");
        // }

        if (dbPrompt != null && !dbPrompt.trim().isEmpty()) {
            sb.append(dbPrompt);
            return sb.toString();
        }

        // Use ERPAgent.SYSTEM_PROMPT as base (includes tool instructions, security rules, etc.)
        sb.append(ERPAgent.SYSTEM_PROMPT);

        return sb.toString();
    }

    /**
     * Load system prompt from database
     *
     * @return Prompt text or null
     */
    private String loadSystemPromptFromDatabase() {
        try {
            return MAIPromptConfig.getPromptText(Env.getCtx(), "SYSTEM", null);
        } catch (Exception e) {
            log.log(Level.FINE, "Could not load system prompt from database", e);
            return null;
        }
    }

    /**
     * Get session ID from chat
     *
     * @param chat Chat instance
     * @return Session identifier
     */
    private String getSessionId(MAIChat chat) {
        if (chat != null) {
            return "chat_" + chat.getCM_Chat_ID();
        }
        return "session_" + System.currentTimeMillis();
    }

    /**
     * Format record data for RAG storage
     *
     * @param recordData Record data JSON
     * @return Formatted string
     */
    private String formatRecordData(JSONObject recordData) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Record:\n");
        for (String key : recordData.keySet()) {
            Object value = recordData.get(key);
            if (value != null && !value.toString().isEmpty()) {
                sb.append("- ").append(key).append(": ").append(value).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Format tab context for RAG storage
     *
     * @param tabContext Tab context JSON
     * @return Formatted string
     */
    private String formatTabContext(JSONObject tabContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Tab: ").append(tabContext.optString("tab_name", "Unknown")).append("\n");
        sb.append("Table: ").append(tabContext.optString("table_name", "Unknown")).append("\n");

        int recordId = tabContext.optInt("selected_record_id", -1);
        if (recordId > 0) {
            sb.append("Selected Record ID: ").append(recordId).append("\n");
        }

        return sb.toString();
    }

    /**
     * Format child tab for RAG storage
     *
     * @param childTab Child tab JSON
     * @return Formatted string
     */
    private String formatChildTab(JSONObject childTab) {
        StringBuilder sb = new StringBuilder();
        sb.append("Related Tab: ").append(childTab.optString("tab_name", "Unknown")).append("\n");
        sb.append("Table: ").append(childTab.optString("table_name", "Unknown")).append("\n");
        sb.append("Row Count: ").append(childTab.optInt("row_count", 0)).append("\n");
        return sb.toString();
    }

    /**
     * Get RAG context manager (may be null if not yet initialized)
     *
     * @return RAGContextManager instance or null
     */
    public RAGContextManager getRAGContextManager() {
        return ragContextManager;
    }

    /**
     * Get metrics
     *
     * @return RAGMetrics instance
     */
    public RAGMetrics getMetrics() {
        return metrics;
    }

    /**
     * Check if RAG service is available
     *
     * @return true if embeddings are available
     */
    public boolean isAvailable() {
        return ragContextManager != null && ragContextManager.isAvailable();
    }

    /**
     * Check if operating in fallback mode
     *
     * @return true if using fallback (no embeddings) or not yet initialized
     */
    public boolean isFallbackMode() {
        return ragContextManager == null || ragContextManager.isFallbackMode();
    }

    /**
     * Clear session context
     *
     * @param sessionId Session identifier
     */
    public void clearSession(String sessionId) {
        if (ragContextManager != null) {
            ragContextManager.clearSession(sessionId);
            log.fine("Cleared RAG session: " + sessionId);
        }
    }

    /**
     * Clear session context and language override for a chat.
     * TEMPORARILY DISABLED - Phase 3: Language override not yet implemented
     *
     * @param chat Chat instance
     */
    public void clearSession(MAIChat chat) {
        if (chat != null) {
            String sessionId = getSessionId(chat);
            clearSession(sessionId);

            // TEMPORARILY DISABLED - Phase 3: Language override not yet implemented
            // Also clear language override (ADR-037)
            // int chatId = chat.getCM_Chat_ID();
            // if (chatId > 0) {
            //     languageService.clearOverrideLanguage(chatId);
            //     log.fine("Cleared language override for chat: " + chatId);
            // }
        }
    }

    /**
     * Get language detection service (ADR-037).
     * TEMPORARILY DISABLED - Phase 3: Language detection service not yet migrated
     *
     * @return LanguageDetectionService instance
     */
    /*
    public LanguageDetectionService getLanguageService() {
        return languageService;
    }

    /**
     * Get the current session language for a chat.
     *
     * @param ctx iDempiere context
     * @param chatId Chat ID
     * @return Current language code (e.g., "de_DE")
     */
    /*
    public String getSessionLanguage(Properties ctx, int chatId) {
        return languageService.getSessionLanguage(ctx, chatId);
    }
    */

    /**
     * Metrics tracking for RAG conversation service
     */
    public static class RAGMetrics {
        private long totalRequests = 0;
        private long successfulRequests = 0;
        private long failedRequests = 0;
        private long totalResponseTimeMs = 0;

        public void recordSuccess(long responseTimeMs) {
            totalRequests++;
            successfulRequests++;
            totalResponseTimeMs += responseTimeMs;
        }

        public void recordError() {
            totalRequests++;
            failedRequests++;
        }

        public long getTotalRequests() {
            return totalRequests;
        }

        public long getSuccessfulRequests() {
            return successfulRequests;
        }

        public long getFailedRequests() {
            return failedRequests;
        }

        public double getAverageResponseTimeMs() {
            return successfulRequests > 0 ? (double) totalResponseTimeMs / successfulRequests : 0;
        }

        public double getSuccessRate() {
            return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0;
        }

        public void reset() {
            totalRequests = 0;
            successfulRequests = 0;
            failedRequests = 0;
            totalResponseTimeMs = 0;
        }

        @Override
        public String toString() {
            return String.format(
                "RAGMetrics[total=%d, success=%d (%.1f%%), avgTime=%.0fms]",
                totalRequests, successfulRequests, getSuccessRate() * 100, getAverageResponseTimeMs()
            );
        }
    }
}
