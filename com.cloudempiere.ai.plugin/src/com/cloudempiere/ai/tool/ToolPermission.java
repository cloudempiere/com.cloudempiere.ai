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
package com.cloudempiere.ai.tool;

/**
 * Permission types for tools
 *
 * <p>Defines the types of permissions that can be required by tools.
 * These are checked before tool execution to ensure the user has
 * appropriate access.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public enum ToolPermission {

    /**
     * Read data from database tables
     */
    READ_DATA("Read Data", "Permission to read data from database tables"),

    /**
     * Write data to database tables
     */
    WRITE_DATA("Write Data", "Permission to insert/update/delete data"),

    /**
     * Execute business processes
     */
    EXECUTE_PROCESS("Execute Process", "Permission to run iDempiere processes"),

    /**
     * Access metadata (AD tables)
     */
    READ_METADATA("Read Metadata", "Permission to read Application Dictionary metadata"),

    /**
     * Generate reports
     */
    GENERATE_REPORT("Generate Report", "Permission to generate reports"),

    /**
     * Access system configuration
     */
    READ_CONFIG("Read Configuration", "Permission to read system configuration"),

    /**
     * Modify system configuration
     */
    WRITE_CONFIG("Write Configuration", "Permission to modify system configuration");

    private final String displayName;
    private final String description;

    ToolPermission(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
