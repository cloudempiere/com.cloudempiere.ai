package com.cloudempiere.ai.provider.dto;

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

    // Optional: function name (if role=function)
    private String name;

    // Optional: function call details
    private AIFunctionCall functionCall;

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
}