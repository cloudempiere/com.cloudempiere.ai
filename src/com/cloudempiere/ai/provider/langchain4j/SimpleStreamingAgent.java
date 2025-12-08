package com.cloudempiere.ai.provider.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * Simple streaming AI Agent interface for providers without tool support.
 *
 * <p>This interface is used for Ollama/Llama providers in LangChain4j 0.35.0
 * which don't support function/tool calling. It provides a simplified system
 * prompt without tool-related instructions.
 *
 * @author Cloudempiere
 * @version 1.0
 * @since 0.17.0
 */
public interface SimpleStreamingAgent {

    /**
     * Simplified system prompt for models without tool support.
     */
    String SIMPLE_SYSTEM_PROMPT =
        "You are an intelligent assistant for the iDempiere ERP system.\n\n" +
        "CAPABILITIES:\n" +
        "- Answer questions about ERP concepts, business processes, and best practices\n" +
        "- Help users understand iDempiere functionality\n" +
        "- Provide guidance on ERP workflows\n" +
        "- Explain business terminology and concepts\n\n" +
        "LIMITATIONS:\n" +
        "- You cannot directly query the database or access live ERP data\n" +
        "- You cannot look up specific records, customers, or orders\n" +
        "- If the user asks about specific data, explain that you need a provider with tool support\n\n" +
        "BEHAVIOR:\n" +
        "- Be helpful and informative\n" +
        "- Be concise and accurate\n" +
        "- If you're unsure about something, say so\n" +
        "- Suggest using a cloud AI provider (Claude, GPT) for database queries";

    /**
     * Main streaming chat method for conversational interaction.
     *
     * @param sessionId Unique session identifier for conversation memory
     * @param userMessage The user's message or question
     * @return TokenStream for streaming response handling
     */
    @SystemMessage(SIMPLE_SYSTEM_PROMPT)
    TokenStream chat(@MemoryId String sessionId, @UserMessage String userMessage);

    /**
     * Execute a specific task/goal without conversation context (streaming).
     *
     * @param goal Task/goal to execute
     * @return TokenStream for streaming response handling
     */
    @SystemMessage(SIMPLE_SYSTEM_PROMPT)
    TokenStream execute(@UserMessage String goal);
}
