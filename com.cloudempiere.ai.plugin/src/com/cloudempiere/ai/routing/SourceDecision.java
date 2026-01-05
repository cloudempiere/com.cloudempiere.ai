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
package com.cloudempiere.ai.routing;

import java.util.List;
import java.util.Map;

/**
 * Decision about optimal data source for answering user query
 *
 * The routing decision determines whether to:
 * - Use cached context data only (fastest, no DB/AI call)
 * - Query database only (fresh data)
 * - Use hybrid approach (context for reference + DB for fresh data)
 *
 * @author Cloudempiere
 */
public class SourceDecision {

    private final DataSource source;
    private final String reasoning;
    private final List<String> contextKeys;
    private final String dbQuery;
    private final Map<String, Object> parameters;

    /**
     * Data source options
     */
    public enum DataSource {
        /** Use cached context data only - no DB query or AI call needed */
        CONTEXT_ONLY,

        /** Query database for fresh data */
        DATABASE_ONLY,

        /** Use context for reference data + DB for fresh transactional data */
        HYBRID
    }

    /**
     * Private constructor - use factory methods
     */
    private SourceDecision(DataSource source, String reasoning,
                          List<String> contextKeys, String dbQuery,
                          Map<String, Object> parameters) {
        this.source = source;
        this.reasoning = reasoning;
        this.contextKeys = contextKeys;
        this.dbQuery = dbQuery;
        this.parameters = parameters;
    }

    /**
     * Create context-only decision
     * @param match context match result
     * @return decision to use context only
     */
    public static SourceDecision contextOnly(ContextMatch match) {
        return new SourceDecision(
            DataSource.CONTEXT_ONLY,
            "All required data available in recent conversation context",
            match.getKeys(),
            null,
            null
        );
    }

    /**
     * Create database-only decision
     * @param params database query parameters
     * @return decision to query database
     */
    public static SourceDecision databaseOnly(DbQueryParams params) {
        return new SourceDecision(
            DataSource.DATABASE_ONLY,
            params.getReasoning(),
            null,
            params.getQuery(),
            params.getParameters()
        );
    }

    /**
     * Create hybrid decision
     * @param match context match for reference data
     * @param params database query parameters for fresh data
     * @return decision to use both sources
     */
    public static SourceDecision hybrid(ContextMatch match, DbQueryParams params) {
        return new SourceDecision(
            DataSource.HYBRID,
            "Using context for reference data, database for fresh transactional data",
            match.getKeys(),
            params.getQuery(),
            params.getParameters()
        );
    }

    // Getters

    /**
     * Get data source decision
     * @return data source to use
     */
    public DataSource getSource() {
        return source;
    }

    /**
     * Get reasoning for decision
     * @return human-readable explanation
     */
    public String getReasoning() {
        return reasoning;
    }

    /**
     * Get context keys to use (for CONTEXT_ONLY and HYBRID)
     * @return list of context keys or null
     */
    public List<String> getContextKeys() {
        return contextKeys;
    }

    /**
     * Get database query (for DATABASE_ONLY and HYBRID)
     * @return query string or null
     */
    public String getDbQuery() {
        return dbQuery;
    }

    /**
     * Get query parameters (for DATABASE_ONLY and HYBRID)
     * @return parameter map or null
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "SourceDecision{" +
                "source=" + source +
                ", reasoning='" + reasoning + '\'' +
                ", contextKeys=" + contextKeys +
                '}';
    }
}
