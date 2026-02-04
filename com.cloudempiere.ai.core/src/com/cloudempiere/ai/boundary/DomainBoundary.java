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

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for domain boundary enforcement (ADR-009).
 *
 * <p>Each domain plugin extends this class to define its allowed tables.
 * Prevents cross-domain data access via tool methods.
 *
 * @author Cloudempiere
 * @version 1.0
 * @since Phase 2 - Multi-Plugin Architecture
 */
public abstract class DomainBoundary {

    private final String domainName;
    private final Set<String> allowedTables;

    protected DomainBoundary(String domainName) {
        this.domainName = domainName;
        this.allowedTables = new HashSet<>();
        registerAllowedTables();
    }

    /**
     * Subclasses override this to register allowed tables.
     */
    protected abstract void registerAllowedTables();

    /**
     * Register a table as allowed for this domain.
     *
     * @param tableName iDempiere table name (e.g., "C_BPartner")
     */
    protected void allow(String tableName) {
        allowedTables.add(tableName);
    }

    /**
     * Validate if table access is allowed for this domain.
     *
     * @param tableName table to access
     * @throws SecurityException if table not allowed
     */
    public void validateReadTable(String tableName) {
        if (!allowedTables.contains(tableName)) {
            throw new SecurityException(
                String.format("[%s Domain] Table %s not allowed. Allowed tables: %s",
                    domainName, tableName, allowedTables)
            );
        }
    }

    /**
     * Check if table is allowed (non-throwing).
     */
    public boolean isTableAllowed(String tableName) {
        return allowedTables.contains(tableName);
    }

    public String getDomainName() {
        return domainName;
    }

    public Set<String> getAllowedTables() {
        return new HashSet<>(allowedTables);  // Defensive copy
    }
}
