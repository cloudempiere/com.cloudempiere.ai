package com.cloudempiere.ai.provider.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request object for AI provider calls
 * Contains all parameters needed for text generation
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIRequest {

    // Message history for multi-turn conversations
    private List<AIMessage> messages;

    // Model to use (e.g., "gpt-4", "claude-3-sonnet")
    private String model;

    // System prompt (instructions for AI)
    private String systemPrompt;

    // Maximum tokens to generate
    private Integer maxTokens;

    // Temperature (0.0-2.0, controls randomness)
    private Double temperature;

    // Top-p nucleus sampling
    private Double topP;

    // Frequency penalty
    private Double frequencyPenalty;

    // Presence penalty
    private Double presencePenalty;

    // Stop sequences
    private List<String> stopSequences;

    // Timeout in milliseconds
    private Integer timeoutMs;

    // Custom provider-specific parameters
    private Map<String, Object> providerParams;

    // Context from iDempiere (optional)
    private Map<String, Object> context;

    // Constructor
    public AIRequest() {
        this.messages = new ArrayList<>();
        this.providerParams = new HashMap<>();
        this.context = new HashMap<>();
    }

    // Convenience method to add a message
    public AIRequest addMessage(String role, String content) {
        messages.add(new AIMessage(role, content));
        return this;
    }

    public AIRequest addSystemMessage(String content) {
        return addMessage("system", content);
    }

    public AIRequest addUserMessage(String content) {
        return addMessage("user", content);
    }

    public AIRequest addAssistantMessage(String content) {
        return addMessage("assistant", content);
    }

    // Getters and setters
    public List<AIMessage> getMessages() { return messages; }
    public void setMessages(List<AIMessage> messages) { this.messages = messages; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }

    public Double getFrequencyPenalty() { return frequencyPenalty; }
    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Double getPresencePenalty() { return presencePenalty; }
    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public List<String> getStopSequences() { return stopSequences; }
    public void setStopSequences(List<String> stopSequences) {
        this.stopSequences = stopSequences;
    }

    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }

    public Map<String, Object> getProviderParams() { return providerParams; }
    public void setProviderParams(Map<String, Object> providerParams) {
        this.providerParams = providerParams;
    }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    // Builder pattern for fluent API
    public static class Builder {
        private AIRequest request = new AIRequest();

        public Builder model(String model) {
            request.setModel(model);
            return this;
        }

        public Builder systemPrompt(String prompt) {
            request.setSystemPrompt(prompt);
            return this;
        }

        public Builder userMessage(String content) {
            request.addUserMessage(content);
            return this;
        }

        public Builder assistantMessage(String content) {
            request.addAssistantMessage(content);
            return this;
        }

        public Builder temperature(double temperature) {
            request.setTemperature(temperature);
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            request.setMaxTokens(maxTokens);
            return this;
        }

        public Builder topP(double topP) {
            request.setTopP(topP);
            return this;
        }

        public Builder frequencyPenalty(double penalty) {
            request.setFrequencyPenalty(penalty);
            return this;
        }

        public Builder presencePenalty(double penalty) {
            request.setPresencePenalty(penalty);
            return this;
        }

        public Builder timeoutMs(int timeout) {
            request.setTimeoutMs(timeout);
            return this;
        }

        public AIRequest build() {
            return request;
        }
    }
}