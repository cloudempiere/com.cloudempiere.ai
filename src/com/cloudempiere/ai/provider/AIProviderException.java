package com.cloudempiere.ai.provider;

/**
 * Base exception for AI provider errors
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIProviderException extends Exception {

    private static final long serialVersionUID = 1L;

    private String errorCode;
    private boolean retryable;

    public AIProviderException(String message) {
        super(message);
        this.retryable = false;
    }

    public AIProviderException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = false;
    }

    public AIProviderException(String message, String errorCode, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public AIProviderException(String message, String errorCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}