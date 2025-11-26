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

import java.util.HashMap;
import java.util.Map;

/**
 * Database query parameters for routing decision
 *
 * Contains metadata about why a database query is needed
 * and any parameters extracted from the user prompt
 *
 * @author Cloudempiere
 */
public class DbQueryParams {

    /** Query string (can be null if using AI function calling) */
    private final String query;

    /** Query parameters extracted from prompt */
    private final Map<String, Object> parameters;

    /** Reasoning for database query decision */
    private final String reasoning;

    /**
     * Constructor
     * @param query query string (can be null)
     * @param parameters query parameters
     * @param reasoning explanation for database query
     */
    public DbQueryParams(String query,
                        Map<String, Object> parameters,
                        String reasoning) {
        this.query = query;
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.reasoning = reasoning != null ? reasoning : "Database query required";
    }

    /**
     * Get query string
     * @return SQL query or null
     */
    public String getQuery() {
        return query;
    }

    /**
     * Get query parameters
     * @return parameter map
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Get reasoning for database query
     * @return explanation string
     */
    public String getReasoning() {
        return reasoning;
    }

    @Override
    public String toString() {
        return "DbQueryParams{" +
                "reasoning='" + reasoning + '\'' +
                ", hasQuery=" + (query != null) +
                ", paramCount=" + parameters.size() +
                '}';
    }
}
