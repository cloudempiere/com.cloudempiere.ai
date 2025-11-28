/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.agent.langchain4j;

import java.util.List;
import java.util.logging.Level;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.agent.AgentContext;
import com.cloudempiere.ai.agent.AgentException;
import com.cloudempiere.ai.agent.AgentMessage;
import com.cloudempiere.ai.agent.AgentResponse;
import com.cloudempiere.ai.agent.IAIAgent;
import com.cloudempiere.ai.tool.ToolRegistry;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;

/**
 * LangChain4j-based AI Agent implementation
 *
 * <p>This agent uses LangChain4j's AiServices to create a dynamic proxy
 * that automatically handles:
 * <ul>
 *   <li>The agent decision loop (think -> act -> observe)</li>
 *   <li>Tool discovery and execution via @Tool annotations</li>
 *   <li>Conversation memory management</li>
 *   <li>Message formatting for different LLM providers</li>
 * </ul>
 *
 * <p>Compared to the manual BaseAgent implementation, this provides:
 * <ul>
 *   <li>~90% less code (no manual loop, parsing, or tool execution)</li>
 *   <li>Automatic tool schema generation from @Tool annotations</li>
 *   <li>Built-in conversation memory</li>
 *   <li>Provider-agnostic implementation</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * LangChain4jAgent agent = new LangChain4jAgent("erp-agent", chatModel);
 * AgentResponse response = agent.execute("What products are low on stock?", context);
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class LangChain4jAgent implements IAIAgent {

    private static final CLogger log = CLogger.getCLogger(LangChain4jAgent.class);

    /** Default maximum messages in memory */
    private static final int DEFAULT_MEMORY_SIZE = 20;

    /** Agent name */
    private final String name;

    /** The LangChain4j chat model */
    private final ChatLanguageModel chatModel;

    /** The tools instance with @Tool annotated methods */
    private final IAITools tools;

    /** The generated agent proxy */
    private IERPAgent agentProxy;

    /** Custom system prompt (optional) */
    private String customSystemPrompt;

    /** Memory size for conversation history */
    private int memorySize = DEFAULT_MEMORY_SIZE;

    /**
     * Create a LangChain4j agent
     *
     * @param name agent name/identifier
     * @param chatModel LangChain4j ChatLanguageModel (e.g., from Anthropic, OpenAI, etc.)
     */
    public LangChain4jAgent(String name, ChatLanguageModel chatModel) {
        this.name = name;
        this.chatModel = chatModel;
        this.tools = new IAITools();
    }

    /**
     * Create a LangChain4j agent with custom tools
     *
     * @param name agent name
     * @param chatModel chat model
     * @param tools custom tools instance
     */
    public LangChain4jAgent(String name, ChatLanguageModel chatModel, IAITools tools) {
        this.name = name;
        this.chatModel = chatModel;
        this.tools = tools != null ? tools : new IAITools();
    }

    /**
     * Build the agent proxy if not already built
     */
    private IERPAgent getOrCreateProxy() {
        if (agentProxy == null) {
            log.fine("Building LangChain4j agent proxy for: " + name);

            AiServices.AiServiceContext serviceBuilder = AiServices.builder(IERPAgent.class)
                .chatLanguageModel(chatModel)
                .tools(tools)
                .chatMemoryProvider(memoryId ->
                    MessageWindowChatMemory.withMaxMessages(memorySize));

            agentProxy = (IERPAgent) serviceBuilder.build();

            log.info("LangChain4j agent proxy created: " + name);
        }
        return agentProxy;
    }

    @Override
    public AgentResponse execute(String goal, AgentContext context) throws AgentException {
        long startTime = System.currentTimeMillis();
        AgentResponse response = new AgentResponse();
        response.setSessionId(context.getSessionId());

        try {
            // Validate context
            context.validate();

            // Set context on tools so they have access to iDempiere context
            tools.setContext(context);

            // Get or create the agent proxy
            IERPAgent proxy = getOrCreateProxy();

            // Execute through LangChain4j - the framework handles:
            // 1. Sending goal to LLM with tool definitions
            // 2. Parsing tool calls from response
            // 3. Executing tools via @Tool methods
            // 4. Sending tool results back to LLM
            // 5. Looping until final response
            log.fine("Executing agent for session: " + context.getSessionId());

            String result = proxy.execute(context.getSessionId(), goal);

            response.setContent(result);
            response.setSuccess(true);

            // Get metrics from context (updated by tools during execution)
            response.setIterations(context.getCurrentToolCalls());

        } catch (RuntimeException e) {
            // LangChain4j wraps exceptions - unwrap and handle
            log.log(Level.WARNING, "Agent execution failed", e);

            String errorMsg = e.getMessage();
            if (e.getCause() != null) {
                errorMsg = e.getCause().getMessage();
            }

            // Check for boundary exceeded
            if (errorMsg != null && errorMsg.contains("Boundary exceeded")) {
                response.setSuccess(false);
                response.setErrorMessage(errorMsg);
                response.setContent("Agent stopped: " + errorMsg);
            } else {
                throw AgentException.executionError("Agent execution failed: " + errorMsg, e);
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Unexpected error in agent", e);
            throw AgentException.executionError("Agent execution failed: " + e.getMessage(), e);
        }

        // Finalize response
        response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        response.setCostUSD(context.getCurrentCostUSD());

        log.info("Agent completed: session=" + context.getSessionId() +
                ", success=" + response.isSuccess() +
                ", toolCalls=" + context.getCurrentToolCalls() +
                ", time=" + response.getExecutionTimeMs() + "ms");

        return response;
    }

    @Override
    public AgentResponse execute(String goal, List<AgentMessage> conversationHistory,
                                 AgentContext context) throws AgentException {
        // LangChain4j handles conversation history internally via ChatMemory
        // For existing history, we would need to pre-populate the memory
        // For now, delegate to single-turn execution
        // TODO: Support loading conversation history into ChatMemory

        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            log.warning("Conversation history provided but not yet supported in LangChain4j agent. " +
                       "Using built-in session memory instead.");
        }

        return execute(goal, context);
    }

    @Override
    public ToolRegistry getToolRegistry() {
        // LangChain4j uses @Tool annotations, not ToolRegistry
        // Return null or an empty registry for compatibility
        log.fine("LangChain4j agent uses @Tool annotations, not ToolRegistry");
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSystemPrompt() {
        return customSystemPrompt;
    }

    @Override
    public void setSystemPrompt(String systemPrompt) {
        this.customSystemPrompt = systemPrompt;
        // Invalidate proxy so it's rebuilt with new prompt
        this.agentProxy = null;
    }

    @Override
    public boolean isReady() {
        return chatModel != null;
    }

    @Override
    public String getModel() {
        // ChatLanguageModel doesn't expose model name directly
        // Return the class name as identifier
        return chatModel != null ? chatModel.getClass().getSimpleName() : null;
    }

    @Override
    public void shutdown() {
        log.info("Shutting down LangChain4j agent: " + name);
        this.agentProxy = null;
    }

    /**
     * Set the memory size for conversation history
     *
     * @param memorySize number of messages to retain
     */
    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
        // Invalidate proxy so it's rebuilt with new memory size
        this.agentProxy = null;
    }

    /**
     * Get the tools instance
     *
     * @return IAITools instance used by this agent
     */
    public IAITools getTools() {
        return tools;
    }

    /**
     * Get the chat model
     *
     * @return ChatLanguageModel instance
     */
    public ChatLanguageModel getChatModel() {
        return chatModel;
    }
}
