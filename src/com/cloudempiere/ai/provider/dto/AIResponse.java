package com.cloudempiere.ai.provider.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response object from AI provider
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIResponse {

    // Generated text content
    private String content;

    // Alternative completions (if requested)
    private List<String> choices;

    // Model used
    private String model;

    // Finish reason ("stop", "length", "function_call")
    private String finishReason;

    // Token usage
    private AITokenUsage tokenUsage;

    // Processing time in milliseconds
    private long processingTimeMs;

    // Cost in USD
    private double costUSD;

    // Function calls (if any)
    private List<AIFunctionCall> functionCalls;

    // Provider-specific metadata
    private Map<String, Object> metadata;

    // Error information (if failed)
    private String errorMessage;
    private String errorCode;

    public AIResponse() {
        this.choices = new ArrayList<>();
        this.functionCalls = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    // Getters and setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getChoices() { return choices; }
    public void setChoices(List<String> choices) { this.choices = choices; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }

    public AITokenUsage getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(AITokenUsage tokenUsage) { this.tokenUsage = tokenUsage; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public double getCostUSD() { return costUSD; }
    public void setCostUSD(double costUSD) { this.costUSD = costUSD; }

    public List<AIFunctionCall> getFunctionCalls() { return functionCalls; }
    public void setFunctionCalls(List<AIFunctionCall> functionCalls) {
        this.functionCalls = functionCalls;
    }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public boolean isSuccess() {
        return errorMessage == null;
    }
}