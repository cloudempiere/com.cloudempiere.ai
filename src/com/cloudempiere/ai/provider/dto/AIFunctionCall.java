package com.cloudempiere.ai.provider.dto;

/**
 * Function call information
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIFunctionCall {
    private String name;
    private String arguments; // JSON string

    public AIFunctionCall() {
    }

    public AIFunctionCall(String name, String arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getArguments() { return arguments; }
    public void setArguments(String arguments) { this.arguments = arguments; }
}