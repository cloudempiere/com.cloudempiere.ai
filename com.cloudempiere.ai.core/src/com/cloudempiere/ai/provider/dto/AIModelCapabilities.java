package com.cloudempiere.ai.provider.dto;

/**
 * Model capabilities
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIModelCapabilities {
    private int maxContextLength;
    private int maxOutputTokens;
    private boolean supportsVision;
    private boolean supportsFunctions;
    private boolean supportsStreaming;
    private boolean supportsJSON;

    // Getters and setters
    public int getMaxContextLength() { return maxContextLength; }
    public void setMaxContextLength(int max) { this.maxContextLength = max; }

    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int max) { this.maxOutputTokens = max; }

    public boolean isSupportsVision() { return supportsVision; }
    public void setSupportsVision(boolean supports) { this.supportsVision = supports; }

    public boolean isSupportsFunctions() { return supportsFunctions; }
    public void setSupportsFunctions(boolean supports) { this.supportsFunctions = supports; }

    public boolean isSupportsStreaming() { return supportsStreaming; }
    public void setSupportsStreaming(boolean supports) { this.supportsStreaming = supports; }

    public boolean isSupportsJSON() { return supportsJSON; }
    public void setSupportsJSON(boolean supports) { this.supportsJSON = supports; }
}