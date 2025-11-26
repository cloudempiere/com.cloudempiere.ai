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

import java.util.List;

import com.cloudempiere.ai.tool.ITool;
import com.cloudempiere.ai.tool.ToolRegistry;

/**
 * Interface for AI Agents
 *
 * <p>An AI Agent is an autonomous system that can:
 * <ul>
 *   <li>Accept a goal or task from the user</li>
 *   <li>Decide which tools to use to accomplish the goal</li>
 *   <li>Execute tools and process their results</li>
 *   <li>Iterate until the goal is achieved or limits are reached</li>
 *   <li>Return a final response to the user</li>
 * </ul>
 *
 * <p>The agent loop typically follows this pattern:
 * <pre>
 * 1. Receive user goal
 * 2. Analyze goal and decide on action
 * 3. If tool call needed:
 *    a. Select appropriate tool
 *    b. Execute tool with parameters
 *    c. Process tool result
 *    d. Go to step 2
 * 4. If goal achieved or limits reached:
 *    a. Generate final response
 *    b. Return to user
 * </pre>
 *
 * <p>Implementations should respect:
 * <ul>
 *   <li>Security boundaries (via AgentContext)</li>
 *   <li>Cost limits (via BoundaryValidator)</li>
 *   <li>Rate limits (via BoundaryValidator)</li>
 *   <li>Tool permissions</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public interface IAIAgent {

    /**
     * Execute the agent with a user goal
     *
     * <p>This is the main entry point for agent execution. The agent will:
     * <ol>
     *   <li>Parse the user's goal</li>
     *   <li>Decide which tools to use</li>
     *   <li>Execute the agent loop until done</li>
     *   <li>Return the final response</li>
     * </ol>
     *
     * @param goal user's goal or task description
     * @param context agent context with user, role, org, and limits
     * @return agent response with result and metadata
     * @throws AgentException if execution fails
     */
    AgentResponse execute(String goal, AgentContext context) throws AgentException;

    /**
     * Execute the agent with conversation history
     *
     * <p>Allows continuing a conversation with prior context.
     *
     * @param goal user's current message/goal
     * @param conversationHistory previous messages in the conversation
     * @param context agent context
     * @return agent response
     * @throws AgentException if execution fails
     */
    AgentResponse execute(String goal, List<AgentMessage> conversationHistory,
                         AgentContext context) throws AgentException;

    /**
     * Get the tool registry used by this agent
     *
     * @return tool registry
     */
    ToolRegistry getToolRegistry();

    /**
     * Register a tool with this agent
     *
     * <p>Convenience method to add a tool to the agent's registry.
     *
     * @param tool tool to register
     */
    default void registerTool(ITool tool) {
        getToolRegistry().register(tool);
    }

    /**
     * Get the agent's name/identifier
     *
     * @return agent name
     */
    String getName();

    /**
     * Get the system prompt used by this agent
     *
     * <p>The system prompt defines the agent's behavior, capabilities,
     * and constraints.
     *
     * @return system prompt
     */
    String getSystemPrompt();

    /**
     * Set a custom system prompt
     *
     * @param systemPrompt custom system prompt
     */
    void setSystemPrompt(String systemPrompt);

    /**
     * Check if agent is ready to execute
     *
     * @return true if agent is properly configured and ready
     */
    boolean isReady();

    /**
     * Get the model being used by this agent
     *
     * @return model identifier (e.g., "claude-3-5-sonnet-20240620")
     */
    String getModel();

    /**
     * Shutdown the agent and release resources
     */
    void shutdown();
}
