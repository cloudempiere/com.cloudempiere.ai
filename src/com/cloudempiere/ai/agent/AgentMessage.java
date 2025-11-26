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

/**
 * Message in agent conversation history
 *
 * <p>Represents a single message in the conversation between the user
 * and the agent. Used for maintaining context across multiple turns.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AgentMessage {

    /**
     * Message role types
     */
    public enum Role {
        /** System prompt / instructions */
        SYSTEM,
        /** User message */
        USER,
        /** Assistant (AI) message */
        ASSISTANT,
        /** Tool result */
        TOOL
    }

    private final Role role;
    private final String content;
    private final String toolName;
    private final String toolCallId;

    /**
     * Create a user message
     *
     * @param content message content
     * @return user message
     */
    public static AgentMessage user(String content) {
        return new AgentMessage(Role.USER, content, null, null);
    }

    /**
     * Create an assistant message
     *
     * @param content message content
     * @return assistant message
     */
    public static AgentMessage assistant(String content) {
        return new AgentMessage(Role.ASSISTANT, content, null, null);
    }

    /**
     * Create a system message
     *
     * @param content message content
     * @return system message
     */
    public static AgentMessage system(String content) {
        return new AgentMessage(Role.SYSTEM, content, null, null);
    }

    /**
     * Create a tool result message
     *
     * @param toolName name of the tool that was called
     * @param toolCallId ID of the tool call
     * @param result tool execution result
     * @return tool message
     */
    public static AgentMessage tool(String toolName, String toolCallId, String result) {
        return new AgentMessage(Role.TOOL, result, toolName, toolCallId);
    }

    /**
     * Create a message
     *
     * @param role message role
     * @param content message content
     * @param toolName tool name (for tool messages)
     * @param toolCallId tool call ID (for tool messages)
     */
    public AgentMessage(Role role, String content, String toolName, String toolCallId) {
        this.role = role;
        this.content = content;
        this.toolName = toolName;
        this.toolCallId = toolCallId;
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * Check if this is a user message
     *
     * @return true if user message
     */
    public boolean isUser() {
        return role == Role.USER;
    }

    /**
     * Check if this is an assistant message
     *
     * @return true if assistant message
     */
    public boolean isAssistant() {
        return role == Role.ASSISTANT;
    }

    /**
     * Check if this is a system message
     *
     * @return true if system message
     */
    public boolean isSystem() {
        return role == Role.SYSTEM;
    }

    /**
     * Check if this is a tool message
     *
     * @return true if tool message
     */
    public boolean isTool() {
        return role == Role.TOOL;
    }

    @Override
    public String toString() {
        return "AgentMessage{" +
               "role=" + role +
               ", content='" + (content != null && content.length() > 50 ?
                               content.substring(0, 50) + "..." : content) + '\'' +
               (toolName != null ? ", toolName='" + toolName + '\'' : "") +
               '}';
    }
}
