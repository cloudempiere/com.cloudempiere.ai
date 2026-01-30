package com.cloudempiere.ai.provider.langchain4j;

import java.util.Properties;

import org.compiere.model.MChat;
import org.json.JSONObject;

import com.cloudempiere.ai.model.MAIProvider;
// TEMPORARILY DISABLED - Observability will be implemented in future phase (ADR-013)
// import com.cloudempiere.ai.observability.CostGuard;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;

/**
 * OSGi service interface for ERP AI capabilities.
 *
 * <p>This interface provides the main entry point for AI chat functionality,
 * replacing the static singleton pattern with proper OSGi service injection.
 *
 * <p>Usage with OSGi Declarative Services:
 * <pre>
 * &#64;Reference
 * private IAIService aiService;
 *
 * public void chat(MAIProvider provider, MChat chat, String message) {
 *     aiService.chatStreamingWithContext(provider, chat, message, null, 0,
 *         AIStreamCallback.builder()
 *             .onChunk(chunk -> display(chunk))
 *             .onComplete(() -> done())
 *             .build());
 * }
 * </pre>
 *
 * @author Cloudempiere
 * @version 10.0.0
 * @since ADR-052 OSGi Modernization
 * @see AIService
 */
public interface IAIService {

    /**
     * Chat with the AI agent (simple mode).
     *
     * <p>This method will apply the full guardrails pipeline once implemented (ADR-014):
     * <ol>
     *   <li>CostGuard: Check budget before making request (future phase)</li>
     *   <li>InputGuard: Sanitize input (PII, injection) (future phase)</li>
     *   <li>AI Call: Execute the agent</li>
     *   <li>OutputGuard: Filter response (leaks, harmful content) (future phase)</li>
     * </ol>
     *
     * @param provider AI Provider configuration
     * @param ctx iDempiere context
     * @param sessionId Session ID for conversation memory
     * @param message User message
     * @return AI response (sanitized)
     * @throws RuntimeException if guardrails block the request or AI call fails
     */
    String chat(MAIProvider provider, Properties ctx, String sessionId, String message);

    // TEMPORARILY DISABLED - AIService will be implemented in future phase
    /*
    /**
     * Chat with context from the chat widget (blocking mode).
     *
     * <p>This method is the main integration point for programmatic use:
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
     * @return ChatResult with response and thread info
     */
    /*
    AIService.ChatResult chatWithContext(MAIProvider provider, MChat chat,
                                          String message, JSONObject contextData,
                                          int threadRootId);
    */

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
    void chatStreamingWithContext(MAIProvider provider, MChat chat,
                                   String message, JSONObject contextData,
                                   int threadRootId, AIStreamCallback callback);

    // TEMPORARILY DISABLED - AIService will be implemented in future phase
    /*
    /**
     * Blocking chat method that wraps streaming using CompletableFuture.
     *
     * <p>This method provides a synchronous API built on top of the streaming
     * implementation. It blocks until the streaming response is complete.
     *
     * @param provider AI Provider configuration
     * @param chat Parent MChat for message persistence
     * @param message User message
     * @param contextData Optional context from window/tab (JSON)
     * @param threadRootId Thread root ID (0 for new thread)
     * @return ChatResult with response and thread info
     */
    /*
    AIService.ChatResult chatBlocking(MAIProvider provider, MChat chat,
                                       String message, JSONObject contextData,
                                       int threadRootId);
    */

    /**
     * Execute a one-shot task without conversation memory.
     *
     * @param provider AI Provider configuration
     * @param ctx iDempiere context
     * @param goal Task/goal to execute
     * @return AI response (sanitized)
     * @throws RuntimeException if guardrails block the request or AI call fails
     */
    String execute(MAIProvider provider, Properties ctx, String goal);

    /**
     * Check if RAG service is available.
     *
     * @return true if RAG service is available and configured
     */
    boolean isRagAvailable();

    /**
     * Clear conversation memory for a session.
     *
     * @param sessionId Session ID to clear
     */
    void clearMemory(String sessionId);

    /**
     * Clear all caches (e.g., on configuration change).
     */
    void clearAllCaches();

    /**
     * Get conversation memory size for a session.
     *
     * @param sessionId Session ID
     * @return Number of messages in memory
     */
    int getMemorySize(String sessionId);

    /**
     * Enable or disable guardrails.
     *
     * @param enabled true to enable guardrails, false to disable
     */
    void setGuardrailsEnabled(boolean enabled);

    /**
     * Check if guardrails are enabled.
     *
     * @return true if guardrails are enabled
     */
    boolean isGuardrailsEnabled();

    // TEMPORARILY DISABLED - Cost guard will be implemented in future phase (ADR-013)
    /*
    /**
     * Get budget status for a client.
     *
     * @param clientId AD_Client_ID
     * @return Budget status information
     */
    /*
    CostGuard.BudgetStatus getBudgetStatus(int clientId);
    */

    /**
     * Clear budget cache for a client.
     *
     * @param clientId AD_Client_ID
     */
    void clearBudgetCache(int clientId);
}
