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
package com.cloudempiere.ai.guardrails.dto;

import java.util.Collections;
import java.util.List;

/**
 * Guard Result DTO for AI safety (ADR-014).
 *
 * <p>Represents the outcome of a guard check with three possible actions:
 * <ul>
 *   <li>PASS - Input/output is safe, proceed unchanged</li>
 *   <li>MASK - Sensitive data detected and masked, proceed with modified content</li>
 *   <li>BLOCK - Dangerous content detected, reject the request</li>
 * </ul>
 *
 * <p><b>Tags:</b> #dto #guardrails #guard-result #security
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see com.cloudempiere.ai.guardrails.InputGuard
 * @see com.cloudempiere.ai.guardrails.OutputGuard
 * @see com.cloudempiere.ai.guardrails.ExecutionGuard
 */
public class GuardResult {

    // ========================================================================
    // Actions
    // ========================================================================

    /**
     * Guard action type.
     */
    public enum Action {
        /** Input/output is safe, proceed */
        PASS,
        /** Sensitive data masked, proceed with modified content */
        MASK,
        /** Dangerous content, reject request */
        BLOCK
    }

    // ========================================================================
    // Fields
    // ========================================================================

    private final Action action;
    private final String processedContent;
    private final String blockReason;
    private final List<String> violations;
    private final String violationType;

    // ========================================================================
    // Constructors
    // ========================================================================

    private GuardResult(Action action, String processedContent, String blockReason,
                       List<String> violations, String violationType) {
        this.action = action;
        this.processedContent = processedContent;
        this.blockReason = blockReason;
        this.violations = violations != null ? violations : Collections.emptyList();
        this.violationType = violationType;
    }

    // ========================================================================
    // Factory Methods
    // ========================================================================

    /**
     * Create a PASS result - input is safe.
     *
     * @param content Original content (unchanged)
     * @return Pass result
     */
    public static GuardResult pass(String content) {
        return new GuardResult(Action.PASS, content, null, null, null);
    }

    /**
     * Create a MASK result - sensitive data was masked.
     *
     * @param maskedContent Content with sensitive data masked
     * @param maskedTypes Types of data that were masked (e.g., "SSN", "Credit Card")
     * @return Mask result
     */
    public static GuardResult mask(String maskedContent, List<String> maskedTypes) {
        return new GuardResult(Action.MASK, maskedContent, null, maskedTypes, "PII");
    }

    /**
     * Create a BLOCK result - dangerous content detected.
     *
     * @param reason User-friendly reason for blocking
     * @param violations List of specific violations detected
     * @param violationType Category of violation (e.g., "INJECTION", "SQL_INJECTION", "PII")
     * @return Block result
     */
    public static GuardResult block(String reason, List<String> violations, String violationType) {
        return new GuardResult(Action.BLOCK, null, reason, violations, violationType);
    }

    // ========================================================================
    // Getters
    // ========================================================================

    /**
     * Get the action to take.
     */
    public Action getAction() {
        return action;
    }

    /**
     * Get the processed content (original for PASS, masked for MASK, null for BLOCK).
     */
    public String getProcessedContent() {
        return processedContent;
    }

    /**
     * Get the reason for blocking (only for BLOCK action).
     */
    public String getBlockReason() {
        return blockReason;
    }

    /**
     * Get the list of violations detected.
     */
    public List<String> getViolations() {
        return violations;
    }

    /**
     * Get the violation type category.
     */
    public String getViolationType() {
        return violationType;
    }

    // ========================================================================
    // Convenience Methods
    // ========================================================================

    /**
     * Check if the request should proceed (PASS or MASK).
     */
    public boolean shouldProceed() {
        return action != Action.BLOCK;
    }

    /**
     * Check if the request was blocked.
     */
    public boolean isBlocked() {
        return action == Action.BLOCK;
    }

    /**
     * Check if content was modified (masked).
     */
    public boolean wasModified() {
        return action == Action.MASK;
    }

    /**
     * Check if the request passed without modification.
     */
    public boolean passedClean() {
        return action == Action.PASS;
    }

    @Override
    public String toString() {
        switch (action) {
            case PASS:
                return "GuardResult[PASS]";
            case MASK:
                return "GuardResult[MASK, masked=" + violations + "]";
            case BLOCK:
                return "GuardResult[BLOCK, reason=" + blockReason + ", type=" + violationType + "]";
            default:
                return "GuardResult[UNKNOWN]";
        }
    }
}
