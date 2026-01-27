/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.boundary;

/**
 * Exception thrown when an AI agent violates a security boundary.
 *
 * <p>This exception is thrown by boundary validators when an agent attempts
 * to access data or perform actions outside its authorized scope.
 *
 * @author Cloudempiere AI Team
 * @version ADR-009
 * @since v0.10.0
 */
public class BoundaryViolationException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Type of boundary violation */
    private final BoundaryViolationType violationType;

    /**
     * Create exception with message and violation type.
     *
     * @param message Error message
     * @param violationType Type of violation
     */
    public BoundaryViolationException(String message, BoundaryViolationType violationType) {
        super(message);
        this.violationType = violationType;
    }

    /**
     * Create exception with message, violation type, and cause.
     *
     * @param message Error message
     * @param violationType Type of violation
     * @param cause Underlying cause
     */
    public BoundaryViolationException(String message, BoundaryViolationType violationType,
                                      Throwable cause) {
        super(message, cause);
        this.violationType = violationType;
    }

    /**
     * Get the type of boundary violation.
     *
     * @return Violation type
     */
    public BoundaryViolationType getViolationType() {
        return violationType;
    }

    /**
     * Check if this violation should trigger an alert.
     *
     * @return true if security alert should be raised
     */
    public boolean isAlertRequired() {
        return violationType.isCritical();
    }

    @Override
    public String toString() {
        return "BoundaryViolationException[" +
               "type=" + violationType +
               ", message=" + getMessage() + "]";
    }
}
