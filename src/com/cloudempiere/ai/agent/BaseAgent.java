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
package com.cloudempiere.ai.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.agent.AgentResponse.ToolCallRecord;
import com.cloudempiere.ai.provider.AIProviderException;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.dto.AIFunction;
import com.cloudempiere.ai.provider.dto.AIFunctionCall;
import com.cloudempiere.ai.provider.dto.AIMessage;
import com.cloudempiere.ai.provider.dto.AIRequest;
import com.cloudempiere.ai.provider.dto.AIResponse;
import com.cloudempiere.ai.tool.BoundaryValidator;
import com.cloudempiere.ai.tool.ITool;
import com.cloudempiere.ai.tool.ToolExecutionException;
import com.cloudempiere.ai.tool.ToolRegistry;

/**
 * Base AI Agent implementation using the existing IAIProvider infrastructure
 *
 * <p>This agent implementation:
 * <ul>
 *   <li>Uses existing IAIProvider for LLM API calls</li>
 *   <li>Implements the agent loop (decide -> execute tool -> iterate)</li>
 *   <li>Integrates with ToolRegistry for tool management</li>
 *   <li>Enforces boundaries via BoundaryValidator</li>
 *   <li>Tracks costs and tool calls</li>
 * </ul>
 *
 * <p>The agent loop:
 * <pre>
 * 1. Send user goal + tools to LLM
 * 2. If LLM returns tool calls:
 *    a. Execute each tool
 *    b. Add tool results to conversation
 *    c. Go to step 1
 * 3. If LLM returns text response:
 *    a. Return response to user
 * 4. If limits exceeded:
 *    a. Return partial response with warning
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class BaseAgent implements IAIAgent {

    private static final CLogger log = CLogger.getCLogger(BaseAgent.class);

    /** Default maximum iterations to prevent infinite loops */
    private static final int DEFAULT_MAX_ITERATIONS = 10;

    /** Agent name */
    private final String name;

    /** AI Provider for making LLM calls */
    private final IAIProvider provider;

    /** Tool registry */
    private final ToolRegistry toolRegistry;

    /** Boundary validator */
    private final BoundaryValidator boundaryValidator;

    /** System prompt */
    private String systemPrompt;

    /** Maximum iterations */
    private int maxIterations = DEFAULT_MAX_ITERATIONS;

    /** Model to use */
    private String model;

    /**
     * Create a base agent
     *
     * @param name agent name
     * @param provider AI provider for LLM calls
     */
    public BaseAgent(String name, IAIProvider provider) {
        this.name = name;
        this.provider = provider;
        this.toolRegistry = new ToolRegistry();
        this.boundaryValidator = new BoundaryValidator();
        this.systemPrompt = buildDefaultSystemPrompt();
    }

    /**
     * Create a base agent with custom tool registry
     *
     * @param name agent name
     * @param provider AI provider
     * @param toolRegistry tool registry to use
     */
    public BaseAgent(String name, IAIProvider provider, ToolRegistry toolRegistry) {
        this.name = name;
        this.provider = provider;
        this.toolRegistry = toolRegistry != null ? toolRegistry : new ToolRegistry();
        this.boundaryValidator = new BoundaryValidator();
        this.systemPrompt = buildDefaultSystemPrompt();
    }

    /**
     * Build the default system prompt
     */
    private String buildDefaultSystemPrompt() {
        return "You are an AI assistant for iDempiere ERP system. " +
               "You have access to tools that allow you to query the database and retrieve information. " +
               "Always use the available tools to get accurate, up-to-date information. " +
               "When querying the database, write efficient SQL queries and respect the user's data access permissions. " +
               "Be concise and helpful in your responses.";
    }

    @Override
    public AgentResponse execute(String goal, AgentContext context) throws AgentException {
        return execute(goal, new ArrayList<>(), context);
    }

    @Override
    public AgentResponse execute(String goal, List<AgentMessage> conversationHistory,
                                 AgentContext context) throws AgentException {

        long startTime = System.currentTimeMillis();
        AgentResponse response = new AgentResponse();
        response.setSessionId(context.getSessionId());

        try {
            // Validate context
            boundaryValidator.validateContext(context);

            // Build conversation messages
            List<AIMessage> messages = new ArrayList<>();

            // Add conversation history
            for (AgentMessage msg : conversationHistory) {
                messages.add(convertToAIMessage(msg));
            }

            // Add current user goal
            messages.add(new AIMessage("user", goal));

            // Get tools as AIFunctions
            List<AIFunction> functions = toolRegistry.toAIFunctions();

            // Agent loop
            int iteration = 0;
            boolean done = false;

            while (!done && iteration < maxIterations) {
                iteration++;

                log.fine("Agent iteration " + iteration + " for session: " + context.getSessionId());

                // Check limits before making API call
                try {
                    boundaryValidator.validateCostLimits(context);
                    boundaryValidator.validateRateLimits(context);
                } catch (ToolExecutionException e) {
                    // Limits exceeded, return partial response
                    response.setContent("Agent stopped: " + e.getMessage());
                    response.setSuccess(false);
                    response.setErrorMessage(e.getMessage());
                    break;
                }

                // Build request
                AIRequest request = new AIRequest();
                request.setSystemPrompt(systemPrompt);
                request.setMessages(messages);
                request.setMaxTokens(context.getMaxTokensPerRequest());

                if (model != null) {
                    request.setModel(model);
                }

                // Call LLM with tools
                AIResponse aiResponse;
                try {
                    if (functions.isEmpty()) {
                        aiResponse = provider.generateText(request);
                    } else {
                        aiResponse = provider.generateTextWithFunctions(request, functions);
                    }
                } catch (AIProviderException e) {
                    throw AgentException.providerError("LLM call failed: " + e.getMessage(), e);
                }

                // Track cost
                if (aiResponse.getCostUSD() > 0) {
                    context.addCost(aiResponse.getCostUSD());
                }

                // Update response metrics
                response.setInputTokens(response.getInputTokens() +
                    (aiResponse.getTokenUsage() != null ? aiResponse.getTokenUsage().getPromptTokens() : 0));
                response.setOutputTokens(response.getOutputTokens() +
                    (aiResponse.getTokenUsage() != null ? aiResponse.getTokenUsage().getCompletionTokens() : 0));

                // Check for function calls
                List<AIFunctionCall> functionCalls = aiResponse.getFunctionCalls();

                if (functionCalls != null && !functionCalls.isEmpty()) {
                    // Process tool calls
                    for (AIFunctionCall call : functionCalls) {
                        ToolCallRecord record = executeToolCall(call, context);
                        response.addToolCall(record);

                        // Add assistant message with tool call
                        messages.add(new AIMessage("assistant", "Calling tool: " + call.getName()));

                        // Add tool result to conversation
                        AIMessage toolResult = new AIMessage("user",
                            "Tool " + call.getName() + " result:\n" + record.getResult());
                        toolResult.setName(call.getName());
                        messages.add(toolResult);
                    }
                } else {
                    // No tool calls, we have a final response
                    response.setContent(aiResponse.getContent());
                    response.setSuccess(true);
                    done = true;
                }
            }

            // Check if we hit iteration limit
            if (!done) {
                response.setContent("Agent reached maximum iterations (" + maxIterations + "). " +
                                   "Partial response may be incomplete.");
                response.setSuccess(false);
                response.setErrorMessage("Maximum iterations exceeded");
            }

            response.setIterations(iteration);

        } catch (ToolExecutionException e) {
            log.log(Level.WARNING, "Tool execution failed", e);
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
            response.setContent("An error occurred: " + e.getMessage());

        } catch (Exception e) {
            log.log(Level.SEVERE, "Agent execution failed", e);
            throw AgentException.executionError("Agent execution failed: " + e.getMessage(), e);
        }

        // Finalize response
        response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        response.setTotalTokens(response.getInputTokens() + response.getOutputTokens());
        response.setCostUSD(context.getCurrentCostUSD());

        log.info("Agent completed: " + response);
        return response;
    }

    /**
     * Execute a single tool call
     */
    private ToolCallRecord executeToolCall(AIFunctionCall call, AgentContext context)
            throws ToolExecutionException {

        long startTime = System.currentTimeMillis();
        ToolCallRecord record = new ToolCallRecord(call.getName(), call.getArguments());

        try {
            // Get tool
            ITool tool = toolRegistry.get(call.getName());
            if (tool == null) {
                throw new ToolExecutionException("Unknown tool: " + call.getName());
            }

            // Validate boundaries
            boundaryValidator.validateBeforeExecution(context, tool);

            // Parse arguments
            Map<String, Object> args = parseArguments(call.getArguments());

            // Execute tool
            String result = tool.execute(context, args);

            // Track execution
            context.incrementToolCalls();
            boundaryValidator.trackExecution(context, 0.001); // Estimate minimal cost

            record.setResult(result);
            record.setSuccess(true);

        } catch (ToolExecutionException e) {
            record.setSuccess(false);
            record.setErrorMessage(e.getMessage());
            record.setResult("Error: " + e.getMessage());
            throw e;

        } catch (Exception e) {
            record.setSuccess(false);
            record.setErrorMessage(e.getMessage());
            record.setResult("Error: " + e.getMessage());
            throw new ToolExecutionException("Tool execution failed: " + e.getMessage(), e);

        } finally {
            record.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        }

        return record;
    }

    /**
     * Parse JSON arguments string to map
     */
    private Map<String, Object> parseArguments(String argumentsJson) {
        Map<String, Object> args = new HashMap<>();

        if (argumentsJson == null || argumentsJson.trim().isEmpty()) {
            return args;
        }

        try {
            JSONObject json = new JSONObject(argumentsJson);
            for (String key : json.keySet()) {
                Object value = json.get(key);
                if (value instanceof JSONArray) {
                    args.put(key, jsonArrayToList((JSONArray) value));
                } else if (value instanceof JSONObject) {
                    args.put(key, jsonObjectToMap((JSONObject) value));
                } else {
                    args.put(key, value);
                }
            }
        } catch (Exception e) {
            log.warning("Failed to parse tool arguments: " + e.getMessage());
        }

        return args;
    }

    private List<Object> jsonArrayToList(JSONArray array) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.get(i));
        }
        return list;
    }

    private Map<String, Object> jsonObjectToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        for (String key : json.keySet()) {
            map.put(key, json.get(key));
        }
        return map;
    }

    /**
     * Convert AgentMessage to AIMessage
     */
    private AIMessage convertToAIMessage(AgentMessage msg) {
        String role;
        switch (msg.getRole()) {
            case SYSTEM:
                role = "system";
                break;
            case USER:
                role = "user";
                break;
            case ASSISTANT:
                role = "assistant";
                break;
            case TOOL:
                role = "user"; // Tool results go as user messages
                break;
            default:
                role = "user";
        }
        return new AIMessage(role, msg.getContent());
    }

    // IAIAgent interface methods

    @Override
    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSystemPrompt() {
        return systemPrompt;
    }

    @Override
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    @Override
    public boolean isReady() {
        return provider != null && provider.isReady();
    }

    @Override
    public String getModel() {
        return model;
    }

    /**
     * Set the model to use
     *
     * @param model model identifier
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Set maximum iterations
     *
     * @param maxIterations maximum iterations
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override
    public void shutdown() {
        log.info("Shutting down agent: " + name);
    }
}
