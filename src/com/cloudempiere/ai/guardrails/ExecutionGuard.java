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
package com.cloudempiere.ai.guardrails;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.compiere.util.CLogger;

/**
 * Execution Guard for AI safety (ADR-014).
 *
 * <p>Classifies and controls AI-requested actions:
 * <ul>
 *   <li>Risk classification: LOW, MEDIUM, HIGH, CRITICAL</li>
 *   <li>Approval routing: Auto-approve, human approval, blocked</li>
 *   <li>Action limits: Financial thresholds, record counts</li>
 *   <li>Prohibited actions: Never allow certain operations</li>
 * </ul>
 *
 * <p>Risk Levels:
 * <ul>
 *   <li>LOW - Read-only operations, safe to auto-approve</li>
 *   <li>MEDIUM - Single record modifications, auto-approve with logging</li>
 *   <li>HIGH - Bulk operations, financial actions, require confirmation</li>
 *   <li>CRITICAL - Sensitive data, large amounts, require human approval</li>
 * </ul>
 *
 * <p><b>Tags:</b> #guardrails #risk-classification #approval-routing #action-control #security
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see InputGuard
 * @see OutputGuard
 * @see com.cloudempiere.ai.observability.CostGuard
 */
public class ExecutionGuard {

    private static final CLogger log = CLogger.getCLogger(ExecutionGuard.class);

    // ========================================================================
    // Risk Levels
    // ========================================================================

    /**
     * Risk level classification.
     */
    public enum RiskLevel {
        /** Read-only, safe to auto-approve */
        LOW,
        /** Single record changes, auto-approve with logging */
        MEDIUM,
        /** Bulk operations, require user confirmation */
        HIGH,
        /** Sensitive operations, require human approval */
        CRITICAL,
        /** Never allowed, always blocked */
        PROHIBITED
    }

    /**
     * Approval decision.
     */
    public enum ApprovalDecision {
        /** Proceed without asking */
        AUTO_APPROVE,
        /** Ask user to confirm */
        REQUIRE_CONFIRMATION,
        /** Require supervisor/admin approval */
        REQUIRE_HUMAN_APPROVAL,
        /** Block the action entirely */
        BLOCK
    }

    // ========================================================================
    // Action Categories
    // ========================================================================

    /** Read-only operations */
    private static final Set<String> READ_ONLY_ACTIONS = new HashSet<>(Arrays.asList(
        "SELECT", "READ", "VIEW", "LIST", "GET", "SEARCH", "QUERY", "REPORT"
    ));

    /** Single-record modifications */
    private static final Set<String> SINGLE_RECORD_ACTIONS = new HashSet<>(Arrays.asList(
        "UPDATE", "MODIFY", "EDIT", "SAVE"
    ));

    /** Record creation */
    private static final Set<String> CREATE_ACTIONS = new HashSet<>(Arrays.asList(
        "INSERT", "CREATE", "ADD", "NEW"
    ));

    /** Record deletion */
    private static final Set<String> DELETE_ACTIONS = new HashSet<>(Arrays.asList(
        "DELETE", "REMOVE", "DROP", "TRUNCATE"
    ));

    /** Financial operations */
    private static final Set<String> FINANCIAL_ACTIONS = new HashSet<>(Arrays.asList(
        "PAYMENT", "INVOICE", "CREDIT", "DEBIT", "TRANSFER", "REFUND",
        "COMPLETE_ORDER", "VOID", "REVERSE", "POST"
    ));

    /** Administrative operations */
    private static final Set<String> ADMIN_ACTIONS = new HashSet<>(Arrays.asList(
        "GRANT", "REVOKE", "ALTER", "CHANGE_PASSWORD", "CHANGE_ROLE",
        "CREATE_USER", "DELETE_USER", "MODIFY_PERMISSION"
    ));

    /** Prohibited operations - never allowed via AI */
    private static final Set<String> PROHIBITED_ACTIONS = new HashSet<>(Arrays.asList(
        "DROP_TABLE", "DROP_DATABASE", "TRUNCATE_TABLE",
        "BULK_DELETE", "MASS_UPDATE", "DELETE_ALL",
        "EXPORT_CREDENTIALS", "EXPORT_PASSWORDS",
        "CHANGE_SYSTEM_CONFIG", "MODIFY_SECURITY_SETTINGS"
    ));

    // ========================================================================
    // Sensitive Tables
    // ========================================================================

    /** Tables that always require high scrutiny */
    private static final Set<String> SENSITIVE_TABLES = new HashSet<>(Arrays.asList(
        "AD_User", "AD_Role", "AD_Role_Access", "AD_User_Roles",
        "AD_Private_Access", "AD_Password_History",
        "C_Payment", "C_BankStatement", "C_BankTransfer",
        "AD_PInstance", "AD_ChangeLog"
    ));

    // ========================================================================
    // Thresholds
    // ========================================================================

    /** Financial amount threshold for HIGH risk (USD) */
    private BigDecimal financialThresholdHigh = new BigDecimal("1000.00");

    /** Financial amount threshold for CRITICAL risk (USD) */
    private BigDecimal financialThresholdCritical = new BigDecimal("10000.00");

    /** Record count threshold for bulk operations */
    private int bulkOperationThreshold = 10;

    /** Maximum records that can be affected in a single operation */
    private int maxAffectedRecords = 100;

    /**
     * Create execution guard with default configuration.
     */
    public ExecutionGuard() {
    }

    /**
     * Classify the risk level of an action.
     *
     * @param action Action type (e.g., "UPDATE", "DELETE", "PAYMENT")
     * @param tableName Target table name
     * @param affectedRecords Number of records affected (0 if unknown)
     * @param amount Financial amount involved (null if not applicable)
     * @return Risk assessment result
     */
    public RiskAssessment assessRisk(String action, String tableName,
                                      int affectedRecords, BigDecimal amount) {

        String normalizedAction = action.toUpperCase();
        List<String> reasons = new ArrayList<>();

        // Check if prohibited
        if (isProhibited(normalizedAction)) {
            reasons.add("Prohibited action: " + action);
            return new RiskAssessment(RiskLevel.PROHIBITED, ApprovalDecision.BLOCK, reasons);
        }

        // Start with LOW risk
        RiskLevel risk = RiskLevel.LOW;

        // Read-only is always LOW
        if (READ_ONLY_ACTIONS.contains(normalizedAction)) {
            return new RiskAssessment(RiskLevel.LOW, ApprovalDecision.AUTO_APPROVE, reasons);
        }

        // Check table sensitivity
        if (SENSITIVE_TABLES.contains(tableName)) {
            risk = escalateRisk(risk, RiskLevel.HIGH);
            reasons.add("Sensitive table: " + tableName);
        }

        // Check action category
        if (DELETE_ACTIONS.contains(normalizedAction)) {
            risk = escalateRisk(risk, RiskLevel.HIGH);
            reasons.add("Delete operation");
        } else if (ADMIN_ACTIONS.contains(normalizedAction)) {
            risk = escalateRisk(risk, RiskLevel.CRITICAL);
            reasons.add("Administrative operation");
        } else if (FINANCIAL_ACTIONS.contains(normalizedAction)) {
            risk = escalateRisk(risk, RiskLevel.MEDIUM);
            reasons.add("Financial operation");
        } else if (CREATE_ACTIONS.contains(normalizedAction)) {
            risk = escalateRisk(risk, RiskLevel.MEDIUM);
            reasons.add("Create operation");
        } else if (SINGLE_RECORD_ACTIONS.contains(normalizedAction)) {
            risk = escalateRisk(risk, RiskLevel.MEDIUM);
            reasons.add("Modification operation");
        }

        // Check affected record count
        if (affectedRecords > maxAffectedRecords) {
            risk = RiskLevel.PROHIBITED;
            reasons.add("Exceeds max affected records: " + affectedRecords + " > " + maxAffectedRecords);
            return new RiskAssessment(risk, ApprovalDecision.BLOCK, reasons);
        } else if (affectedRecords > bulkOperationThreshold) {
            risk = escalateRisk(risk, RiskLevel.HIGH);
            reasons.add("Bulk operation: " + affectedRecords + " records");
        }

        // Check financial amount
        if (amount != null) {
            if (amount.compareTo(financialThresholdCritical) >= 0) {
                risk = escalateRisk(risk, RiskLevel.CRITICAL);
                reasons.add("High financial amount: $" + amount);
            } else if (amount.compareTo(financialThresholdHigh) >= 0) {
                risk = escalateRisk(risk, RiskLevel.HIGH);
                reasons.add("Significant financial amount: $" + amount);
            }
        }

        // Determine approval decision
        ApprovalDecision decision = determineApproval(risk);

        log.fine("Risk assessment: action=" + action + ", table=" + tableName +
                ", records=" + affectedRecords + ", amount=" + amount +
                " -> " + risk + " (" + decision + ")");

        return new RiskAssessment(risk, decision, reasons);
    }

    /**
     * Quick check if an action is allowed at all.
     *
     * @param action Action to check
     * @return true if action is not prohibited
     */
    public boolean isActionAllowed(String action) {
        return !isProhibited(action.toUpperCase());
    }

    /**
     * Check if a specific operation should proceed.
     *
     * @param action Action type
     * @param tableName Target table
     * @param affectedRecords Number of records
     * @param amount Financial amount
     * @param userConfirmed Whether user has already confirmed
     * @return true if operation should proceed
     */
    public boolean shouldProceed(String action, String tableName,
                                  int affectedRecords, BigDecimal amount,
                                  boolean userConfirmed) {

        RiskAssessment assessment = assessRisk(action, tableName, affectedRecords, amount);

        switch (assessment.decision) {
            case AUTO_APPROVE:
                return true;
            case REQUIRE_CONFIRMATION:
                return userConfirmed;
            case REQUIRE_HUMAN_APPROVAL:
                // Would need external approval system
                return false;
            case BLOCK:
                return false;
            default:
                return false;
        }
    }

    /**
     * Check if action is prohibited.
     */
    private boolean isProhibited(String action) {
        return PROHIBITED_ACTIONS.contains(action) ||
               action.contains("DROP") ||
               action.contains("TRUNCATE") ||
               action.contains("BULK_DELETE");
    }

    /**
     * Escalate risk level (never decrease).
     */
    private RiskLevel escalateRisk(RiskLevel current, RiskLevel proposed) {
        if (proposed.ordinal() > current.ordinal()) {
            return proposed;
        }
        return current;
    }

    /**
     * Determine approval decision based on risk level.
     */
    private ApprovalDecision determineApproval(RiskLevel risk) {
        switch (risk) {
            case LOW:
                return ApprovalDecision.AUTO_APPROVE;
            case MEDIUM:
                return ApprovalDecision.AUTO_APPROVE; // With logging
            case HIGH:
                return ApprovalDecision.REQUIRE_CONFIRMATION;
            case CRITICAL:
                return ApprovalDecision.REQUIRE_HUMAN_APPROVAL;
            case PROHIBITED:
                return ApprovalDecision.BLOCK;
            default:
                return ApprovalDecision.BLOCK;
        }
    }

    // ========================================================================
    // Configuration setters
    // ========================================================================

    public void setFinancialThresholdHigh(BigDecimal threshold) {
        this.financialThresholdHigh = threshold;
    }

    public void setFinancialThresholdCritical(BigDecimal threshold) {
        this.financialThresholdCritical = threshold;
    }

    public void setBulkOperationThreshold(int threshold) {
        this.bulkOperationThreshold = threshold;
    }

    public void setMaxAffectedRecords(int max) {
        this.maxAffectedRecords = max;
    }

    // ========================================================================
    // Result class
    // ========================================================================

    /**
     * Risk assessment result.
     */
    public static class RiskAssessment {
        private final RiskLevel level;
        private final ApprovalDecision decision;
        private final List<String> reasons;

        public RiskAssessment(RiskLevel level, ApprovalDecision decision, List<String> reasons) {
            this.level = level;
            this.decision = decision;
            this.reasons = reasons;
        }

        public RiskLevel getLevel() {
            return level;
        }

        public ApprovalDecision getDecision() {
            return decision;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public boolean isBlocked() {
            return decision == ApprovalDecision.BLOCK;
        }

        public boolean requiresConfirmation() {
            return decision == ApprovalDecision.REQUIRE_CONFIRMATION ||
                   decision == ApprovalDecision.REQUIRE_HUMAN_APPROVAL;
        }

        public boolean canAutoApprove() {
            return decision == ApprovalDecision.AUTO_APPROVE;
        }

        @Override
        public String toString() {
            return String.format("RiskAssessment[level=%s, decision=%s, reasons=%s]",
                level, decision, reasons);
        }
    }
}
