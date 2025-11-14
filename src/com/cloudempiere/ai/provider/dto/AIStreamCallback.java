package com.cloudempiere.ai.provider.dto;

/**
 * Callback interface for streaming responses
 *
 * @author Cloudempiere
 * @version 1.0
 */
public interface AIStreamCallback {

    /**
     * Called when a new text chunk is received
     *
     * @param chunk text chunk
     */
    void onChunk(String chunk);

    /**
     * Called when streaming is complete
     */
    void onComplete();

    /**
     * Called when an error occurs during streaming
     *
     * @param error exception that occurred
     */
    void onError(Exception error);
}