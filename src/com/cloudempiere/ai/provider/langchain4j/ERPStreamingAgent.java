package com.cloudempiere.ai.provider.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * Streaming ERP AI Agent interface for LangChain4j AiServices.
 *
 * <p>This interface defines the contract for streaming AI agents that interact with
 * the ERP system. Unlike {@link ERPAgent}, this returns a {@link TokenStream}
 * that allows processing response tokens as they arrive.
 *
 * <p>Implements ADR-033: Streaming Responses and Thinking Timeline UX
 *
 * <p>Usage:
 * <pre>
 * StreamingChatLanguageModel model = LangChain4jProviderFactory.createStreaming(config, null, null);
 * ERPTools tools = new ERPTools(provider, ctx);
 *
 * ERPStreamingAgent agent = AiServices.builder(ERPStreamingAgent.class)
 *     .streamingChatLanguageModel(model)
 *     .tools(tools)
 *     .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
 *     .build();
 *
 * TokenStream stream = agent.chat(sessionId, "Show me overdue orders");
 * stream.onPartialResponse(token -&gt; System.out.print(token))
 *       .onComplete(response -&gt; System.out.println("\nDone"))
 *       .onError(error -&gt; error.printStackTrace())
 *       .start();
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 * @since ADR-033
 */
public interface ERPStreamingAgent {

    /**
     * System message that defines the agent's behavior and constraints.
     * Same as {@link ERPAgent#SYSTEM_PROMPT}.
     */
    String SYSTEM_PROMPT = ERPAgent.SYSTEM_PROMPT;

    /**
     * Main streaming chat method for conversational interaction.
     *
     * <p>Returns a {@link TokenStream} that can be used to process
     * response tokens as they arrive from the model.
     *
     * <p>Note: System message is provided dynamically via systemMessageProvider()
     * in AiServices builder to support language detection (ADR-037).
     * Do NOT add @SystemMessage annotation here - it would override the dynamic prompt.
     *
     * @param sessionId Unique session identifier for conversation memory
     * @param userMessage The user's message or question
     * @return TokenStream for streaming response handling
     */
    TokenStream chat(@MemoryId String sessionId, @UserMessage String userMessage);

    /**
     * Execute a specific task/goal without conversation context (streaming).
     *
     * <p>Note: System message is provided dynamically via systemMessageProvider().
     *
     * @param goal Task/goal to execute
     * @return TokenStream for streaming response handling
     */
    TokenStream execute(@UserMessage String goal);
}
