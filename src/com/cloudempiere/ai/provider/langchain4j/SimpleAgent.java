package com.cloudempiere.ai.provider.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Simple AI Agent interface for providers without tool support.
 *
 * <p>This interface is used for Ollama/Llama providers in LangChain4j 0.35.0
 * which don't support function/tool calling. It provides a simplified system
 * prompt without tool-related instructions.
 *
 * @author Cloudempiere
 * @version 1.0
 * @since 0.17.0
 */
public interface SimpleAgent {

    /**
     * Simplified system prompt for models without tool support.
     * Same as {@link SimpleStreamingAgent#SIMPLE_SYSTEM_PROMPT}.
     */
    String SIMPLE_SYSTEM_PROMPT = SimpleStreamingAgent.SIMPLE_SYSTEM_PROMPT;

    /**
     * Main chat method for conversational interaction.
     *
     * @param sessionId Unique session identifier for conversation memory
     * @param userMessage The user's message or question
     * @return AI response
     */
    @SystemMessage(SIMPLE_SYSTEM_PROMPT)
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);

    /**
     * Execute a specific task/goal without conversation context.
     *
     * @param goal Task/goal to execute
     * @return AI response
     */
    @SystemMessage(SIMPLE_SYSTEM_PROMPT)
    String execute(@UserMessage String goal);
}
