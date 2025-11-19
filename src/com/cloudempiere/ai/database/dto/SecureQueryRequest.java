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
package com.cloudempiere.ai.database.dto;

import java.util.Properties;

/**
 * Request for secure database query execution
 *
 * <p><b>IMPORTANT</b>: The ctx (context) must contain the
 * logged-in user's AD_User_ID and AD_Role_ID. The query
 * will execute with that user's permissions.
 *
 * <p>The AI provider ID is used for audit trail purposes only.
 * The actual permissions come from the user's role.
 *
 * @author Cloudempiere
 * @version 2.0
 */
public class SecureQueryRequest {

    /** User's context (contains AD_User_ID, AD_Role_ID) */
    private Properties ctx;

    /** SQL query to execute */
    private String sql;

    /** AI Provider ID (for audit trail) */
    private int providerId;

    /** Context type (CHART, WINDOW, NLSQL, etc.) */
    private String contextType;

    /** Human-readable purpose */
    private String queryPurpose;

    /** Max rows to return (0 = use default) */
    private int maxRows;

    /** Query timeout in ms (0 = use default) */
    private int timeoutMs;

    /** Transaction name */
    private String trxName;

    /**
     * Get user's context
     * @return Properties containing AD_User_ID, AD_Role_ID, etc.
     */
    public Properties getCtx() {
        return ctx;
    }

    public void setCtx(Properties ctx) {
        this.ctx = ctx;
    }

    /**
     * Get SQL query
     * @return SQL query string
     */
    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    /**
     * Get AI provider ID
     * @return AIG_Provider_ID
     */
    public int getProviderId() {
        return providerId;
    }

    public void setProviderId(int providerId) {
        this.providerId = providerId;
    }

    /**
     * Get context type
     * @return Context type (CHART, WINDOW, etc.)
     */
    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    /**
     * Get query purpose
     * @return Human-readable query purpose
     */
    public String getQueryPurpose() {
        return queryPurpose;
    }

    public void setQueryPurpose(String queryPurpose) {
        this.queryPurpose = queryPurpose;
    }

    /**
     * Get maximum rows to return
     * @return Max rows (0 = use default)
     */
    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    /**
     * Get query timeout
     * @return Timeout in milliseconds (0 = use default)
     */
    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Get transaction name
     * @return Transaction name
     */
    public String getTrxName() {
        return trxName;
    }

    public void setTrxName(String trxName) {
        this.trxName = trxName;
    }

    /**
     * Builder for SecureQueryRequest
     */
    public static class Builder {
        private final SecureQueryRequest request = new SecureQueryRequest();

        /**
         * Set user's context
         * @param ctx Properties containing user's AD_User_ID and AD_Role_ID
         * @return Builder instance
         */
        public Builder ctx(Properties ctx) {
            request.ctx = ctx;
            return this;
        }

        /**
         * Set SQL query
         * @param sql SQL query to execute
         * @return Builder instance
         */
        public Builder sql(String sql) {
            request.sql = sql;
            return this;
        }

        /**
         * Set AI provider ID
         * @param providerId AIG_Provider_ID
         * @return Builder instance
         */
        public Builder providerId(int providerId) {
            request.providerId = providerId;
            return this;
        }

        /**
         * Set context type
         * @param contextType Context type (CHART, WINDOW, NLSQL, etc.)
         * @return Builder instance
         */
        public Builder contextType(String contextType) {
            request.contextType = contextType;
            return this;
        }

        /**
         * Set query purpose
         * @param purpose Human-readable query purpose
         * @return Builder instance
         */
        public Builder queryPurpose(String purpose) {
            request.queryPurpose = purpose;
            return this;
        }

        /**
         * Set maximum rows to return
         * @param maxRows Maximum rows (0 = use default)
         * @return Builder instance
         */
        public Builder maxRows(int maxRows) {
            request.maxRows = maxRows;
            return this;
        }

        /**
         * Set query timeout
         * @param timeoutMs Timeout in milliseconds (0 = use default)
         * @return Builder instance
         */
        public Builder timeoutMs(int timeoutMs) {
            request.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * Set transaction name
         * @param trxName Transaction name
         * @return Builder instance
         */
        public Builder trxName(String trxName) {
            request.trxName = trxName;
            return this;
        }

        /**
         * Build the request
         * @return SecureQueryRequest instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public SecureQueryRequest build() {
            if (request.ctx == null) {
                throw new IllegalArgumentException("ctx (user context) is required");
            }
            if (request.sql == null || request.sql.trim().isEmpty()) {
                throw new IllegalArgumentException("sql is required");
            }
            if (request.providerId <= 0) {
                throw new IllegalArgumentException("providerId is required");
            }
            return request;
        }
    }
}
