package com.cloudempiere.ai.provider.langchain4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;

/**
 * In-plugin mock AI Hub provider - no external server required.
 *
 * <p>This mock implementation simulates the AI Hub service behavior
 * without requiring an HTTP server. It's ideal for:
 * <ul>
 *   <li>Development and testing without external dependencies</li>
 *   <li>Embedded fallback when real Hub is unavailable</li>
 *   <li>Fast UI/streaming tests</li>
 *   <li>Offline development</li>
 * </ul>
 *
 * <p><b>Benefits vs HTTP Mock Server:</b>
 * <ul>
 *   <li>[+] No external process needed</li>
 *   <li>[+] No port conflicts</li>
 *   <li>[+] Automatic - works out of the box</li>
 *   <li>[+] Faster (in-memory)</li>
 *   <li>[+] Can be used as production fallback</li>
 * </ul>
 *
 * <p><b>Drawbacks:</b>
 * <ul>
 *   <li>[-] Doesn't test HTTP/network layer</li>
 *   <li>[-] Less realistic than external service</li>
 *   <li>[!] Not suitable for integration testing with real HTTP</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 0.27.0
 * @since In-Plugin Mock Provider Implementation
 */
public class MockAIHubChatModel implements StreamingChatLanguageModel {

    private static final Logger log = Logger.getLogger(MockAIHubChatModel.class.getName());

    private final String modelName;
    private final boolean verbose;

    /**
     * Create a mock AI Hub chat model.
     *
     * @param modelName Model name for display purposes
     * @param verbose Enable verbose logging
     */
    public MockAIHubChatModel(String modelName, boolean verbose) {
        this.modelName = modelName != null ? modelName : "mock-ai-hub";
        this.verbose = verbose;

        if (verbose) {
            log.info("[MOCK-HUB] Initialized in-plugin mock (no HTTP server required)");
            log.info("[MOCK-HUB] Model: " + this.modelName);
        }
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        if (verbose) {
            log.info("[MOCK-HUB] Generating response for " + messages.size() + " messages");
        }

        // Simulate async processing
        CompletableFuture.runAsync(() -> {
            try {
                // Extract last user message
                String lastMessage = extractLastUserMessage(messages);

                // Generate contextual mock response
                String response = generateMockResponse(lastMessage);

                // Stream response word by word
                String[] words = response.split(" ");
                for (String word : words) {
                    handler.onNext(word + " ");

                    // Simulate streaming delay
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        handler.onError(e);
                        return;
                    }
                }

                // Complete
                Response<AiMessage> aiResponse = Response.from(
                    AiMessage.from(response),
                    null,  // token usage
                    null   // finish reason
                );
                handler.onComplete(aiResponse);

                if (verbose) {
                    log.info("[MOCK-HUB] Response completed: " + response.length() + " chars");
                }

            } catch (Exception e) {
                log.severe("[MOCK-HUB] Error generating response: " + e.getMessage());
                handler.onError(e);
            }
        });
    }

    /**
     * Extract last user message from conversation.
     */
    private String extractLastUserMessage(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        // Find last user message
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg instanceof UserMessage) {
                return ((UserMessage) msg).singleText();
            }
        }

        ChatMessage last = messages.get(messages.size() - 1);
        if (last instanceof UserMessage) {
            return ((UserMessage) last).singleText();
        } else if (last instanceof AiMessage) {
            return ((AiMessage) last).text();
        } else if (last instanceof SystemMessage) {
            return ((SystemMessage) last).text();
        }
        return "";
    }

    /**
     * Generate contextual mock response based on user message.
     */
    private String generateMockResponse(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return "Hello! I'm the Mock AI Hub (in-plugin, no server required). How can I help you today?";
        }

        String lower = userMessage.toLowerCase();

        // Context-aware responses
        if (lower.contains("hello") || lower.contains("hi")) {
            return "Hello! I'm the in-plugin Mock AI Hub. No external server needed! How can I assist you?";
        }

        if (lower.contains("order") || lower.contains("sales")) {
            return "I can help you with sales orders. In mock mode, I simulate responses without external services. " +
                   "This is perfect for development and testing! What would you like to know about orders?";
        }

        if (lower.contains("invoice") || lower.contains("payment")) {
            return "Mock invoice data:\n" +
                   "• Outstanding invoices: 5 items ($12,450.00)\n" +
                   "• Overdue: 2 items ($3,200.00)\n" +
                   "• Payment terms: Net 30\n\n" +
                   "This is simulated data from the in-plugin mock provider.";
        }

        if (lower.contains("product") || lower.contains("inventory")) {
            return "Mock inventory status:\n" +
                   "• Total SKUs: 1,234\n" +
                   "• Low stock: 15 items\n" +
                   "• Out of stock: 3 items\n\n" +
                   "Generated by in-plugin Mock AI Hub.";
        }

        if (lower.contains("help") || lower.contains("what can you")) {
            return "I'm the in-plugin Mock AI Hub provider. Benefits:\n" +
                   "[OK] No external server required\n" +
                   "[OK] No port conflicts\n" +
                   "[OK] Works offline\n" +
                   "[OK] Fast in-memory processing\n" +
                   "[OK] Perfect for development\n\n" +
                   "Try asking about orders, invoices, or products!";
        }

        // Default response
        return "I understand you're asking about: \"" + truncate(userMessage, 100) + "\"\n\n" +
               "This response is generated by the in-plugin Mock AI Hub provider. " +
               "No external HTTP server is required - this runs directly in the OSGi plugin. " +
               "Perfect for development, testing, and as a fallback when the real AI Hub is unavailable!";
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
