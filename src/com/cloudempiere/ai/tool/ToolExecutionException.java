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
package com.cloudempiere.ai.tool;

/**
 * Exception thrown when tool execution fails
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class ToolExecutionException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Error code for categorization */
    private final String errorCode;

    /** Whether the error is recoverable */
    private final boolean recoverable;

    /**
     * Create a tool execution exception
     *
     * @param message error message
     */
    public ToolExecutionException(String message) {
        super(message);
        this.errorCode = "TOOL_ERROR";
        this.recoverable = false;
    }

    /**
     * Create a tool execution exception with cause
     *
     * @param message error message
     * @param cause underlying cause
     */
    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TOOL_ERROR";
        this.recoverable = false;
    }

    /**
     * Create a tool execution exception with error code
     *
     * @param message error message
     * @param errorCode error code for categorization
     */
    public ToolExecutionException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.recoverable = false;
    }

    /**
     * Create a tool execution exception with all options
     *
     * @param message error message
     * @param errorCode error code
     * @param recoverable whether error is recoverable
     * @param cause underlying cause
     */
    public ToolExecutionException(String message, String errorCode,
                                  boolean recoverable, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.recoverable = recoverable;
    }

    /**
     * Get error code
     *
     * @return error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Check if error is recoverable
     *
     * <p>Recoverable errors might be retried or handled differently
     * by the agent.
     *
     * @return true if recoverable
     */
    public boolean isRecoverable() {
        return recoverable;
    }

    // Factory methods for common error types

    /**
     * Create a permission denied exception
     *
     * @param message error message
     * @return new exception
     */
    public static ToolExecutionException permissionDenied(String message) {
        return new ToolExecutionException(message, "PERMISSION_DENIED", false, null);
    }

    /**
     * Create a validation error exception
     *
     * @param message error message
     * @return new exception
     */
    public static ToolExecutionException validationError(String message) {
        return new ToolExecutionException(message, "VALIDATION_ERROR", true, null);
    }

    /**
     * Create a not found exception
     *
     * @param message error message
     * @return new exception
     */
    public static ToolExecutionException notFound(String message) {
        return new ToolExecutionException(message, "NOT_FOUND", false, null);
    }

    /**
     * Create a timeout exception
     *
     * @param message error message
     * @return new exception
     */
    public static ToolExecutionException timeout(String message) {
        return new ToolExecutionException(message, "TIMEOUT", true, null);
    }

    /**
     * Create a rate limit exception
     *
     * @param message error message
     * @return new exception
     */
    public static ToolExecutionException rateLimitExceeded(String message) {
        return new ToolExecutionException(message, "RATE_LIMIT", true, null);
    }

    /**
     * Create a cost limit exception
     *
     * @param message error message
     * @return new exception
     */
    public static ToolExecutionException costLimitExceeded(String message) {
        return new ToolExecutionException(message, "COST_LIMIT", false, null);
    }
}
