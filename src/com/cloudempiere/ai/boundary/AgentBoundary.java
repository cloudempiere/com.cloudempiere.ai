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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Agent Boundary Configuration (ADR-009).
 *
 * <p>Defines the security boundaries for a specific AI agent type.
 * Each agent type (Inventory, Sales, Purchasing, etc.) has its own
 * boundary configuration that limits what data and actions it can access.
 *
 * <p>Boundary types:
 * <ul>
 *   <li><b>Data boundaries</b>: Which tables/columns the agent can access</li>
 *   <li><b>Action boundaries</b>: Which operations the agent can perform</li>
 *   <li><b>Cost boundaries</b>: Token and dollar limits</li>
 *   <li><b>Rate boundaries</b>: Tool call frequency limits</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * AgentBoundary boundary = AgentBoundary.builder("inventory-agent")
 *     .allowTables("M_Product", "M_Warehouse", "M_Storage")
 *     .allowActions(ActionType.READ, ActionType.QUERY)
 *     .maxCostPerRequest(new BigDecimal("0.10"))
 *     .maxToolCallsPerRequest(20)
 *     .build();
 * </pre>
 *
 * @author Cloudempiere AI Team
 * @version ADR-009
 * @since v0.10.0
 */
public class AgentBoundary {

    /** Agent type identifier */
    private final String agentType;

    /** Allowed tables (whitelist) */
    private final Set<String> allowedTables;

    /** Blocked tables (blacklist - takes precedence over whitelist) */
    private final Set<String> blockedTables;

    /** Allowed columns per table */
    private final Map<String, Set<String>> allowedColumns;

    /** Blocked columns per table */
    private final Map<String, Set<String>> blockedColumns;

    /** Allowed action types */
    private final Set<ActionType> allowedActions;

    /** Whether sensitive data access is allowed */
    private final boolean sensitiveDataAllowed;

    /** Maximum cost per request (USD) */
    private final BigDecimal maxCostPerRequest;

    /** Maximum cost per day (USD) */
    private final BigDecimal maxCostPerDay;

    /** Maximum tool calls per request */
    private final int maxToolCallsPerRequest;

    /** Maximum tool calls per minute (rate limit) */
    private final int maxToolCallsPerMinute;

    /** Maximum tokens per request */
    private final int maxTokensPerRequest;

    /** Description of this boundary */
    private final String description;

    private AgentBoundary(Builder builder) {
        this.agentType = builder.agentType;
        this.allowedTables = Collections.unmodifiableSet(new HashSet<>(builder.allowedTables));
        this.blockedTables = Collections.unmodifiableSet(new HashSet<>(builder.blockedTables));
        this.allowedColumns = Collections.unmodifiableMap(new HashMap<>(builder.allowedColumns));
        this.blockedColumns = Collections.unmodifiableMap(new HashMap<>(builder.blockedColumns));
        this.allowedActions = Collections.unmodifiableSet(new HashSet<>(builder.allowedActions));
        this.sensitiveDataAllowed = builder.sensitiveDataAllowed;
        this.maxCostPerRequest = builder.maxCostPerRequest;
        this.maxCostPerDay = builder.maxCostPerDay;
        this.maxToolCallsPerRequest = builder.maxToolCallsPerRequest;
        this.maxToolCallsPerMinute = builder.maxToolCallsPerMinute;
        this.maxTokensPerRequest = builder.maxTokensPerRequest;
        this.description = builder.description;
    }

    /**
     * Check if a table is allowed.
     *
     * @param tableName Table name
     * @return true if allowed
     */
    public boolean isTableAllowed(String tableName) {
        // Blacklist takes precedence
        if (blockedTables.contains(tableName)) {
            return false;
        }

        // If whitelist is empty, allow all (except blacklisted)
        if (allowedTables.isEmpty()) {
            return true;
        }

        // Check whitelist
        return allowedTables.contains(tableName);
    }

    /**
     * Check if a column is allowed.
     *
     * @param tableName Table name
     * @param columnName Column name
     * @return true if allowed
     */
    public boolean isColumnAllowed(String tableName, String columnName) {
        // Check blocked columns for this table
        Set<String> blocked = blockedColumns.get(tableName);
        if (blocked != null && blocked.contains(columnName)) {
            return false;
        }

        // Check allowed columns for this table
        Set<String> allowed = allowedColumns.get(tableName);
        if (allowed == null || allowed.isEmpty()) {
            return true; // No restrictions
        }

        return allowed.contains(columnName);
    }

    /**
     * Check if an action type is allowed.
     *
     * @param action Action type
     * @return true if allowed
     */
    public boolean isActionAllowed(ActionType action) {
        if (allowedActions.isEmpty()) {
            return true; // No restrictions
        }
        return allowedActions.contains(action);
    }

    /**
     * Check if sensitive data access is allowed.
     *
     * @return true if allowed
     */
    public boolean isSensitiveDataAllowed() {
        return sensitiveDataAllowed;
    }

    // ========================================================================
    // Getters
    // ========================================================================

    public String getAgentType() {
        return agentType;
    }

    public Set<String> getAllowedTables() {
        return allowedTables;
    }

    public Set<String> getBlockedTables() {
        return blockedTables;
    }

    public Set<ActionType> getAllowedActions() {
        return allowedActions;
    }

    public BigDecimal getMaxCostPerRequest() {
        return maxCostPerRequest;
    }

    public BigDecimal getMaxCostPerDay() {
        return maxCostPerDay;
    }

    public int getMaxToolCallsPerRequest() {
        return maxToolCallsPerRequest;
    }

    public int getMaxToolCallsPerMinute() {
        return maxToolCallsPerMinute;
    }

    public int getMaxTokensPerRequest() {
        return maxTokensPerRequest;
    }

    public String getDescription() {
        return description;
    }

    // ========================================================================
    // Builder
    // ========================================================================

    /**
     * Create a new boundary builder.
     *
     * @param agentType Agent type identifier
     * @return Builder instance
     */
    public static Builder builder(String agentType) {
        return new Builder(agentType);
    }

    /**
     * Builder for AgentBoundary.
     */
    public static class Builder {
        private final String agentType;
        private final Set<String> allowedTables = new HashSet<>();
        private final Set<String> blockedTables = new HashSet<>();
        private final Map<String, Set<String>> allowedColumns = new HashMap<>();
        private final Map<String, Set<String>> blockedColumns = new HashMap<>();
        private final Set<ActionType> allowedActions = new HashSet<>();
        private boolean sensitiveDataAllowed = false;
        private BigDecimal maxCostPerRequest = new BigDecimal("0.50");
        private BigDecimal maxCostPerDay = new BigDecimal("10.00");
        private int maxToolCallsPerRequest = 50;
        private int maxToolCallsPerMinute = 100;
        private int maxTokensPerRequest = 100000;
        private String description = "";

        private Builder(String agentType) {
            this.agentType = agentType;
        }

        public Builder allowTables(String... tables) {
            Collections.addAll(this.allowedTables, tables);
            return this;
        }

        public Builder blockTables(String... tables) {
            Collections.addAll(this.blockedTables, tables);
            return this;
        }

        public Builder allowColumns(String tableName, String... columns) {
            this.allowedColumns.computeIfAbsent(tableName, k -> new HashSet<>());
            Collections.addAll(this.allowedColumns.get(tableName), columns);
            return this;
        }

        public Builder blockColumns(String tableName, String... columns) {
            this.blockedColumns.computeIfAbsent(tableName, k -> new HashSet<>());
            Collections.addAll(this.blockedColumns.get(tableName), columns);
            return this;
        }

        public Builder allowActions(ActionType... actions) {
            Collections.addAll(this.allowedActions, actions);
            return this;
        }

        public Builder allowSensitiveData(boolean allowed) {
            this.sensitiveDataAllowed = allowed;
            return this;
        }

        public Builder maxCostPerRequest(BigDecimal cost) {
            this.maxCostPerRequest = cost;
            return this;
        }

        public Builder maxCostPerDay(BigDecimal cost) {
            this.maxCostPerDay = cost;
            return this;
        }

        public Builder maxToolCallsPerRequest(int max) {
            this.maxToolCallsPerRequest = max;
            return this;
        }

        public Builder maxToolCallsPerMinute(int max) {
            this.maxToolCallsPerMinute = max;
            return this;
        }

        public Builder maxTokensPerRequest(int max) {
            this.maxTokensPerRequest = max;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public AgentBoundary build() {
            return new AgentBoundary(this);
        }
    }

    /**
     * Types of actions an agent can perform.
     */
    public enum ActionType {
        /** Read/query data */
        READ,
        /** Execute database queries */
        QUERY,
        /** Create new records */
        CREATE,
        /** Update existing records */
        UPDATE,
        /** Delete records */
        DELETE,
        /** Execute processes */
        PROCESS,
        /** Generate reports */
        REPORT,
        /** Modify document status */
        DOCACTION,
        /** Access configuration */
        CONFIG
    }

    @Override
    public String toString() {
        return "AgentBoundary[" +
               "type=" + agentType +
               ", tables=" + allowedTables.size() +
               ", actions=" + allowedActions +
               ", maxCost=" + maxCostPerRequest +
               "]";
    }
}
