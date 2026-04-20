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
package com.cloudempiere.ai.database.dto;

import java.util.Objects;

/**
 * Cached column metadata from Application Dictionary.
 * Mirrors idempiere-hub's ColumnMetadata for fast column validation.
 *
 * @author Cloudempiere
 * @version ADR-060
 */
public class ADColumnMeta {

    private final String columnName;
    private final String name;
    private final String description;
    private final int adReferenceId;
    private final String referenceType;
    private final boolean mandatory;
    private final boolean key;
    private final int fieldLength;

    public ADColumnMeta(String columnName, String name, String description,
                        int adReferenceId, String referenceType,
                        boolean mandatory, boolean key, int fieldLength) {
        this.columnName = columnName;
        this.name = name;
        this.description = description;
        this.adReferenceId = adReferenceId;
        this.referenceType = referenceType;
        this.mandatory = mandatory;
        this.key = key;
        this.fieldLength = fieldLength;
    }

    public String getColumnName() { return columnName; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getAdReferenceId() { return adReferenceId; }
    public String getReferenceType() { return referenceType; }
    public boolean isMandatory() { return mandatory; }
    public boolean isKey() { return key; }
    public int getFieldLength() { return fieldLength; }

    /**
     * Get the foreign table name for TableDirect (_ID) columns.
     * Convention: column ending in _ID references a table with that prefix.
     * e.g., C_BPartner_ID references C_BPartner.
     *
     * @return foreign table name or null if not a FK column
     */
    public String getForeignTable() {
        // AD_Reference_ID 19 = TableDirect
        if (adReferenceId == 19 && columnName.endsWith("_ID") && !key) {
            return columnName.substring(0, columnName.length() - 3);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ADColumnMeta that = (ADColumnMeta) o;
        return Objects.equals(columnName, that.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName);
    }

    @Override
    public String toString() {
        return "ADColumnMeta{" + columnName + " (" + referenceType + ")" +
               (mandatory ? " NOT NULL" : "") + (key ? " PK" : "") + "}";
    }
}
