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

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j Agent Interface for iDempiere ERP
 *
 * <p>This interface defines the contract for AI agent interactions.
 * LangChain4j's AiServices.builder() creates a dynamic proxy implementation
 * that automatically:
 * <ul>
 *   <li>Sends messages to the configured ChatLanguageModel</li>
 *   <li>Discovers and makes @Tool methods available to the model</li>
 *   <li>Executes tool calls when the model requests them</li>
 *   <li>Manages conversation memory per session</li>
 *   <li>Loops until the model provides a final answer</li>
 * </ul>
 *
 * <p>The @SystemMessage annotation provides the default system prompt.
 * The @MemoryId annotation enables per-session conversation history.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public interface IERPAgent {

    /**
     * Execute the agent with a user goal
     *
     * <p>This is the main entry point for agent execution. The LangChain4j
     * framework automatically handles:
     * <ol>
     *   <li>Sending the goal to the AI model with available tools</li>
     *   <li>Processing tool calls if the model requests them</li>
     *   <li>Iterating until the model provides a final response</li>
     * </ol>
     *
     * @param sessionId unique session identifier for conversation memory
     * @param goal user's goal or question
     * @return AI response text
     */
    @SystemMessage({
        "You are an AI assistant integrated with iDempiere ERP system.",
        "",
        "## Your Capabilities",
        "- Query the database to retrieve business data",
        "- Understand the iDempiere data model (tables, columns, relationships)",
        "- Provide insights based on ERP data",
        "",
        "## Important Guidelines",
        "1. Always use the queryDatabase tool to get real data - never make up numbers",
        "2. Use getTableMetadata first if you're unsure about table structure",
        "3. Queries are automatically filtered by user's organization access",
        "4. Be concise and actionable in your responses",
        "5. If a query fails, explain the issue and suggest alternatives",
        "",
        "## Security Notes",
        "- Only SELECT queries are allowed (read-only access)",
        "- Sensitive data (passwords, credit cards) is automatically redacted",
        "- All queries are logged for audit purposes"
    })
    String execute(
            @MemoryId String sessionId,
            @UserMessage String goal);

    /**
     * Execute agent without session memory
     *
     * <p>Use this for one-off queries that don't need conversation history.
     *
     * @param goal user's goal or question
     * @return AI response text
     */
    @SystemMessage({
        "You are an AI assistant integrated with iDempiere ERP system.",
        "",
        "## Your Capabilities",
        "- Query the database to retrieve business data",
        "- Understand the iDempiere data model (tables, columns, relationships)",
        "- Provide insights based on ERP data",
        "",
        "## Important Guidelines",
        "1. Always use the queryDatabase tool to get real data - never make up numbers",
        "2. Use getTableMetadata first if you're unsure about table structure",
        "3. Queries are automatically filtered by user's organization access",
        "4. Be concise and actionable in your responses",
        "5. If a query fails, explain the issue and suggest alternatives",
        "",
        "## Security Notes",
        "- Only SELECT queries are allowed (read-only access)",
        "- Sensitive data (passwords, credit cards) is automatically redacted",
        "- All queries are logged for audit purposes"
    })
    String chat(@UserMessage String goal);
}
