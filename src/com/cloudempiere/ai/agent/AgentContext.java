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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.util.Env;

/**
 * Context for AI Agent execution
 *
 * <p>Holds all context information needed for agent execution including:
 * <ul>
 *   <li>iDempiere context (Properties)</li>
 *   <li>User, role, client, org information</li>
 *   <li>AI provider configuration</li>
 *   <li>Boundary limits (cost, rate, data)</li>
 *   <li>Session metadata</li>
 * </ul>
 *
 * <p>This context is passed to all tools during execution to ensure
 * proper security, audit logging, and boundary enforcement.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AgentContext {

    /** iDempiere context */
    private final Properties ctx;

    /** AI Provider ID from AIG_Provider table */
    private final int providerId;

    /** User ID (logged-in user) */
    private final int userId;

    /** Role ID (logged-in user's role) */
    private final int roleId;

    /** Client ID */
    private final int clientId;

    /** Organization ID */
    private final int orgId;

    /** Maximum cost allowed for this agent session (in USD) */
    private double maxCostUSD = 10.0;

    /** Maximum number of tool calls allowed */
    private int maxToolCalls = 20;

    /** Maximum tokens per request */
    private int maxTokensPerRequest = 4096;

    /** Session ID for tracking */
    private String sessionId;

    /** Current cost accumulated in this session */
    private double currentCostUSD = 0.0;

    /** Current tool call count */
    private int currentToolCalls = 0;

    /** Additional metadata */
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Create agent context from iDempiere context
     *
     * @param ctx iDempiere context (Properties)
     * @param providerId AI Provider ID from AIG_Provider table
     */
    public AgentContext(Properties ctx, int providerId) {
        this.ctx = ctx;
        this.providerId = providerId;
        this.userId = Env.getAD_User_ID(ctx);
        this.roleId = Env.getAD_Role_ID(ctx);
        this.clientId = Env.getAD_Client_ID(ctx);
        this.orgId = Env.getAD_Org_ID(ctx);
        this.sessionId = generateSessionId();
    }

    /**
     * Create agent context with custom user/role (for testing or special cases)
     *
     * @param ctx iDempiere context
     * @param providerId AI Provider ID
     * @param userId User ID
     * @param roleId Role ID
     * @param clientId Client ID
     * @param orgId Organization ID
     */
    public AgentContext(Properties ctx, int providerId, int userId, int roleId,
                       int clientId, int orgId) {
        this.ctx = ctx;
        this.providerId = providerId;
        this.userId = userId;
        this.roleId = roleId;
        this.clientId = clientId;
        this.orgId = orgId;
        this.sessionId = generateSessionId();
    }

    /**
     * Generate a unique session ID
     */
    private String generateSessionId() {
        return "agent-" + System.currentTimeMillis() + "-" +
               Thread.currentThread().getId();
    }

    // ========================================================================
    // GETTERS
    // ========================================================================

    public Properties getCtx() {
        return ctx;
    }

    public int getProviderId() {
        return providerId;
    }

    public int getUserId() {
        return userId;
    }

    public int getRoleId() {
        return roleId;
    }

    public int getClientId() {
        return clientId;
    }

    public int getOrgId() {
        return orgId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public double getMaxCostUSD() {
        return maxCostUSD;
    }

    public int getMaxToolCalls() {
        return maxToolCalls;
    }

    public int getMaxTokensPerRequest() {
        return maxTokensPerRequest;
    }

    public double getCurrentCostUSD() {
        return currentCostUSD;
    }

    public int getCurrentToolCalls() {
        return currentToolCalls;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    // ========================================================================
    // SETTERS FOR LIMITS
    // ========================================================================

    public AgentContext setMaxCostUSD(double maxCostUSD) {
        this.maxCostUSD = maxCostUSD;
        return this;
    }

    public AgentContext setMaxToolCalls(int maxToolCalls) {
        this.maxToolCalls = maxToolCalls;
        return this;
    }

    public AgentContext setMaxTokensPerRequest(int maxTokensPerRequest) {
        this.maxTokensPerRequest = maxTokensPerRequest;
        return this;
    }

    public AgentContext setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    // ========================================================================
    // TRACKING METHODS
    // ========================================================================

    /**
     * Add cost to current session
     *
     * @param costUSD cost in USD
     */
    public void addCost(double costUSD) {
        this.currentCostUSD += costUSD;
    }

    /**
     * Increment tool call counter
     */
    public void incrementToolCalls() {
        this.currentToolCalls++;
    }

    /**
     * Check if cost limit has been exceeded
     *
     * @return true if current cost exceeds max cost
     */
    public boolean isCostLimitExceeded() {
        return currentCostUSD >= maxCostUSD;
    }

    /**
     * Check if tool call limit has been exceeded
     *
     * @return true if current tool calls exceed max tool calls
     */
    public boolean isToolCallLimitExceeded() {
        return currentToolCalls >= maxToolCalls;
    }

    /**
     * Get remaining cost budget
     *
     * @return remaining cost in USD
     */
    public double getRemainingCostUSD() {
        return Math.max(0, maxCostUSD - currentCostUSD);
    }

    /**
     * Get remaining tool calls
     *
     * @return remaining tool calls
     */
    public int getRemainingToolCalls() {
        return Math.max(0, maxToolCalls - currentToolCalls);
    }

    // ========================================================================
    // METADATA METHODS
    // ========================================================================

    /**
     * Add metadata
     *
     * @param key metadata key
     * @param value metadata value
     * @return this context for chaining
     */
    public AgentContext addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Get metadata value
     *
     * @param key metadata key
     * @return metadata value or null
     */
    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }

    /**
     * Get metadata value with default
     *
     * @param key metadata key
     * @param defaultValue default value if not found
     * @return metadata value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadataValue(String key, T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Validate context is properly configured
     *
     * @throws IllegalStateException if context is invalid
     */
    public void validate() {
        if (ctx == null) {
            throw new IllegalStateException("iDempiere context is null");
        }
        if (providerId <= 0) {
            throw new IllegalStateException("Invalid provider ID: " + providerId);
        }
        if (userId < 0) {
            throw new IllegalStateException("Invalid user ID: " + userId);
        }
        if (roleId < 0) {
            throw new IllegalStateException("Invalid role ID: " + roleId);
        }
        if (clientId < 0) {
            throw new IllegalStateException("Invalid client ID: " + clientId);
        }
    }

    @Override
    public String toString() {
        return "AgentContext{" +
               "sessionId='" + sessionId + '\'' +
               ", providerId=" + providerId +
               ", userId=" + userId +
               ", roleId=" + roleId +
               ", clientId=" + clientId +
               ", orgId=" + orgId +
               ", currentCost=" + currentCostUSD + "/" + maxCostUSD +
               ", toolCalls=" + currentToolCalls + "/" + maxToolCalls +
               '}';
    }
}
