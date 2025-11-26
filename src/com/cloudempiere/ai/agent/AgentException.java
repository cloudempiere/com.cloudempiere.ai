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
package com.cloudempiere.ai.agent;

/**
 * Exception thrown by AI Agent operations
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AgentException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Error code for categorization */
    private final String errorCode;

    /**
     * Create an agent exception
     *
     * @param message error message
     */
    public AgentException(String message) {
        super(message);
        this.errorCode = "AGENT_ERROR";
    }

    /**
     * Create an agent exception with cause
     *
     * @param message error message
     * @param cause underlying cause
     */
    public AgentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "AGENT_ERROR";
    }

    /**
     * Create an agent exception with error code
     *
     * @param message error message
     * @param errorCode error code
     */
    public AgentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Create an agent exception with all options
     *
     * @param message error message
     * @param errorCode error code
     * @param cause underlying cause
     */
    public AgentException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Get error code
     *
     * @return error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    // Factory methods

    /**
     * Create a configuration error
     *
     * @param message error message
     * @return new exception
     */
    public static AgentException configurationError(String message) {
        return new AgentException(message, "CONFIG_ERROR");
    }

    /**
     * Create an initialization error
     *
     * @param message error message
     * @return new exception
     */
    public static AgentException initializationError(String message) {
        return new AgentException(message, "INIT_ERROR");
    }

    /**
     * Create an execution error
     *
     * @param message error message
     * @param cause underlying cause
     * @return new exception
     */
    public static AgentException executionError(String message, Throwable cause) {
        return new AgentException(message, "EXECUTION_ERROR", cause);
    }

    /**
     * Create a timeout error
     *
     * @param message error message
     * @return new exception
     */
    public static AgentException timeout(String message) {
        return new AgentException(message, "TIMEOUT");
    }

    /**
     * Create a limit exceeded error
     *
     * @param message error message
     * @return new exception
     */
    public static AgentException limitExceeded(String message) {
        return new AgentException(message, "LIMIT_EXCEEDED");
    }

    /**
     * Create a provider error
     *
     * @param message error message
     * @param cause underlying cause
     * @return new exception
     */
    public static AgentException providerError(String message, Throwable cause) {
        return new AgentException(message, "PROVIDER_ERROR", cause);
    }
}
