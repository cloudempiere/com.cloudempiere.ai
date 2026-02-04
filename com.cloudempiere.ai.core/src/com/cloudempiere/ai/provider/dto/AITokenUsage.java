package com.cloudempiere.ai.provider.dto;

/**
 * Token usage information
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AITokenUsage {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    // Getters and setters
    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }

    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
}