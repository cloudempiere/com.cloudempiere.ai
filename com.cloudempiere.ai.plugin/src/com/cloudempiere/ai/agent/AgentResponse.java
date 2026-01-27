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
import java.util.List;

/**
 * Response from AI Agent execution
 *
 * <p>Contains the agent's response along with metadata about the execution,
 * including tool calls made, costs incurred, and timing information.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AgentResponse {

    /** The final text response from the agent */
    private String content;

    /** Whether the agent successfully completed its goal */
    private boolean success;

    /** Error message if execution failed */
    private String errorMessage;

    /** List of tool calls made during execution */
    private List<ToolCallRecord> toolCalls = new ArrayList<>();

    /** Total tokens used (input + output) */
    private int totalTokens;

    /** Input tokens used */
    private int inputTokens;

    /** Output tokens used */
    private int outputTokens;

    /** Estimated cost in USD */
    private double costUSD;

    /** Total execution time in milliseconds */
    private long executionTimeMs;

    /** Number of agent loop iterations */
    private int iterations;

    /** Session ID for tracking */
    private String sessionId;

    /**
     * Create an empty response
     */
    public AgentResponse() {
    }

    /**
     * Create a successful response with content
     *
     * @param content response content
     */
    public AgentResponse(String content) {
        this.content = content;
        this.success = true;
    }

    /**
     * Create an error response
     *
     * @param errorMessage error message
     * @return error response
     */
    public static AgentResponse error(String errorMessage) {
        AgentResponse response = new AgentResponse();
        response.success = false;
        response.errorMessage = errorMessage;
        return response;
    }

    /**
     * Create a success response
     *
     * @param content response content
     * @return success response
     */
    public static AgentResponse success(String content) {
        return new AgentResponse(content);
    }

    // Getters and setters

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<ToolCallRecord> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCallRecord> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public void addToolCall(ToolCallRecord toolCall) {
        this.toolCalls.add(toolCall);
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(int inputTokens) {
        this.inputTokens = inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(int outputTokens) {
        this.outputTokens = outputTokens;
    }

    public double getCostUSD() {
        return costUSD;
    }

    public void setCostUSD(double costUSD) {
        this.costUSD = costUSD;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Get count of tool calls made
     *
     * @return tool call count
     */
    public int getToolCallCount() {
        return toolCalls.size();
    }

    @Override
    public String toString() {
        return "AgentResponse{" +
               "success=" + success +
               ", iterations=" + iterations +
               ", toolCalls=" + toolCalls.size() +
               ", tokens=" + totalTokens +
               ", cost=$" + String.format("%.4f", costUSD) +
               ", time=" + executionTimeMs + "ms" +
               '}';
    }

    /**
     * Record of a tool call made during agent execution
     */
    public static class ToolCallRecord {
        private String toolName;
        private String parameters;
        private String result;
        private long executionTimeMs;
        private boolean success;
        private String errorMessage;

        public ToolCallRecord() {
        }

        public ToolCallRecord(String toolName, String parameters) {
            this.toolName = toolName;
            this.parameters = parameters;
        }

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public String getParameters() {
            return parameters;
        }

        public void setParameters(String parameters) {
            this.parameters = parameters;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public void setExecutionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return "ToolCall{" +
                   "tool='" + toolName + '\'' +
                   ", success=" + success +
                   ", time=" + executionTimeMs + "ms" +
                   '}';
        }
    }
}
