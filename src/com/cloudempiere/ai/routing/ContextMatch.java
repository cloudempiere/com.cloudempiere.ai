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

import java.util.*;

/**
 * Result of context lookup for entities mentioned in prompt
 *
 * Indicates whether requested entities were found in conversation context:
 * - Complete: All entities found
 * - Partial: Some entities found
 * - None: No entities found
 *
 * @author Cloudempiere
 */
public class ContextMatch {

    /** Found data from context */
    private final Map<String, Object> foundData;

    /** True if all requested entities were found */
    private final boolean complete;

    /** True if some (but not all) entities were found */
    private final boolean partial;

    /**
     * Constructor
     * @param foundData map of found context data
     * @param complete true if all entities found
     * @param partial true if some entities found
     */
    public ContextMatch(Map<String, Object> foundData,
                       boolean complete,
                       boolean partial) {
        this.foundData = foundData != null ? foundData : new HashMap<>();
        this.complete = complete;
        this.partial = partial;
    }

    /**
     * Check if any data was found
     * @return true if at least one entity found
     */
    public boolean exists() {
        return !foundData.isEmpty();
    }

    /**
     * Check if all requested entities were found
     * @return true if complete match
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Check if partial match (some but not all entities found)
     * @return true if partial match
     */
    public boolean isPartial() {
        return partial;
    }

    /**
     * Get list of context keys that were found
     * @return list of keys
     */
    public List<String> getKeys() {
        return new ArrayList<>(foundData.keySet());
    }

    /**
     * Get found data map
     * @return map of context data
     */
    public Map<String, Object> getData() {
        return foundData;
    }

    /**
     * Get number of entities found
     * @return count
     */
    public int getMatchCount() {
        return foundData.size();
    }

    @Override
    public String toString() {
        return "ContextMatch{" +
                "matchCount=" + foundData.size() +
                ", complete=" + complete +
                ", partial=" + partial +
                ", keys=" + foundData.keySet() +
                '}';
    }
}
