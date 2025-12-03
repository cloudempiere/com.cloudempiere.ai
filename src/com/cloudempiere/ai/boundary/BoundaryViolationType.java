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
 * Types of boundary violations that can occur during AI agent execution.
 *
 * <p>Each type has a severity level and indicates whether it should trigger
 * a security alert.
 *
 * @author CloudEmpiere AI Team
 * @version ADR-009
 * @since v0.10.0
 */
public enum BoundaryViolationType {

    // ========================================================================
    // Organization/Client Boundary Violations
    // ========================================================================

    /**
     * Agent attempted to access data from a different client
     */
    CLIENT_ACCESS_DENIED("Client access denied", true, "ORG"),

    /**
     * Agent attempted to access data from an unauthorized organization
     */
    ORG_ACCESS_DENIED("Organization access denied", true, "ORG"),

    /**
     * Role not found or invalid
     */
    INVALID_ROLE("Invalid or missing role", true, "ORG"),

    // ========================================================================
    // Data Access Boundary Violations
    // ========================================================================

    /**
     * Agent attempted to access a restricted table
     */
    TABLE_ACCESS_DENIED("Table access denied", true, "DATA"),

    /**
     * Agent attempted to access a restricted column
     */
    COLUMN_ACCESS_DENIED("Column access denied", true, "DATA"),

    /**
     * Agent attempted to access a restricted record
     */
    RECORD_ACCESS_DENIED("Record access denied", false, "DATA"),

    /**
     * Agent attempted to access sensitive data (e.g., payroll, pricing)
     */
    SENSITIVE_DATA_ACCESS("Sensitive data access attempt", true, "DATA"),

    // ========================================================================
    // Action Boundary Violations
    // ========================================================================

    /**
     * Agent attempted a prohibited action (e.g., DELETE, modify finalized doc)
     */
    PROHIBITED_ACTION("Prohibited action attempted", true, "ACTION"),

    /**
     * Agent attempted to execute an unauthorized process
     */
    PROCESS_NOT_ALLOWED("Process execution not allowed", false, "ACTION"),

    /**
     * Agent attempted to modify a document that's already completed
     */
    DOCUMENT_LOCKED("Document is locked (completed/voided)", false, "ACTION"),

    // ========================================================================
    // Cost/Resource Boundary Violations
    // ========================================================================

    /**
     * Agent exceeded token limit for request
     */
    TOKEN_LIMIT_EXCEEDED("Token limit exceeded", false, "COST"),

    /**
     * Agent exceeded cost budget
     */
    COST_LIMIT_EXCEEDED("Cost budget exceeded", false, "COST"),

    /**
     * Agent exceeded tool call rate limit
     */
    RATE_LIMIT_EXCEEDED("Rate limit exceeded", false, "COST"),

    /**
     * Agent session timeout
     */
    SESSION_TIMEOUT("Session timeout", false, "COST"),

    // ========================================================================
    // Security Violations
    // ========================================================================

    /**
     * SQL injection attempt detected
     */
    INJECTION_ATTEMPT("SQL injection attempt", true, "SECURITY"),

    /**
     * Prompt injection attempt detected
     */
    PROMPT_INJECTION("Prompt injection attempt", true, "SECURITY"),

    /**
     * Attempt to bypass security controls
     */
    SECURITY_BYPASS_ATTEMPT("Security bypass attempt", true, "SECURITY");

    private final String description;
    private final boolean critical;
    private final String category;

    BoundaryViolationType(String description, boolean critical, String category) {
        this.description = description;
        this.critical = critical;
        this.category = category;
    }

    /**
     * Get human-readable description.
     *
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this violation is critical (requires immediate attention).
     *
     * @return true if critical
     */
    public boolean isCritical() {
        return critical;
    }

    /**
     * Get the category of this violation.
     *
     * @return Category code (ORG, DATA, ACTION, COST, SECURITY)
     */
    public String getCategory() {
        return category;
    }

    /**
     * Check if this is an organization boundary violation.
     *
     * @return true if org-related
     */
    public boolean isOrgViolation() {
        return "ORG".equals(category);
    }

    /**
     * Check if this is a data access violation.
     *
     * @return true if data-related
     */
    public boolean isDataViolation() {
        return "DATA".equals(category);
    }

    /**
     * Check if this is a security violation.
     *
     * @return true if security-related
     */
    public boolean isSecurityViolation() {
        return "SECURITY".equals(category);
    }
}
