package com.cloudempiere.ai.provider.dto;

import java.util.List;

/**
 * Represents a single message in a conversation
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIMessage {

    // Role: "system", "user", "assistant", "function"
    private String role;

    // Message content
    private String content;

    // Optional: function name (if role=function) - for function result messages
    private String name;

    // Optional: single function call (for backward compatibility)
    private AIFunctionCall functionCall;

    // Optional: multiple function calls (AI can call multiple tools)
    private List<AIFunctionCall> functionCalls;

    // Optional: function name for function result messages
    private String functionName;

    public AIMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    // Getters and setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AIFunctionCall getFunctionCall() { return functionCall; }
    public void setFunctionCall(AIFunctionCall functionCall) {
        this.functionCall = functionCall;
    }

    /**
     * Get multiple function calls (for multi-tool calling)
     * @return List of function calls or null
     */
    public List<AIFunctionCall> getFunctionCalls() {
        return functionCalls;
    }

    /**
     * Set multiple function calls
     * @param functionCalls List of function calls
     */
    public void setFunctionCalls(List<AIFunctionCall> functionCalls) {
        this.functionCalls = functionCalls;
    }

    /**
     * Get function name for function result messages
     * @return Function name
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Set function name for function result messages
     * @param functionName Function name
     */
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }
}