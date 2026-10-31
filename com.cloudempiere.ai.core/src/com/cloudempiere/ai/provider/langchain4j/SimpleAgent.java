package com.cloudempiere.ai.provider.langchain4j;

import com.cloudempiere.ai.guardrails.InputGuard;
import com.cloudempiere.ai.guardrails.OutputGuard;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import dev.langchain4j.service.guardrail.OutputGuardrails;

/**
 * Simple AI Agent interface for providers without tool support.
 *
 * <p>This interface is used for providers registered without tool support in
 * {@link LangChain4jProviderFactory} (Ollama/Llama: the exclusion dates from LangChain4j
 * 0.35.0 and has not been re-tested on 1.20.0). It provides a simplified system
 * prompt without tool-related instructions.
 *
 * @author Cloudempiere
 * @version 1.0
 * @since 0.17.0
 */
@InputGuardrails(InputGuard.class)
@OutputGuardrails(OutputGuard.class)
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
