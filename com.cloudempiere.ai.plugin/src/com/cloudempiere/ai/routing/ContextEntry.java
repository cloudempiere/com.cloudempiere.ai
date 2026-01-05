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

/**
 * Context entry with metadata and expiration tracking
 * Used for conversation-level caching with TTL
 *
 * @author Cloudempiere
 */
public class ContextEntry {

    /** Cached value */
    private final Object value;

    /** Expiration timestamp (milliseconds) */
    private final long expiresAt;

    /** Creation timestamp (milliseconds) */
    private final long createdAt = System.currentTimeMillis();

    /** Last access timestamp (milliseconds) - for LRU eviction */
    private long lastAccessed = System.currentTimeMillis();

    /** Data type for TTL determination */
    private final DataType dataType;

    /**
     * Constructor
     * @param value cached value
     * @param expiresAt expiration timestamp in milliseconds
     * @param dataType data type category
     */
    public ContextEntry(Object value, long expiresAt, DataType dataType) {
        this.value = value;
        this.expiresAt = expiresAt;
        this.dataType = dataType;
    }

    /**
     * Check if entry is expired
     * @return true if current time exceeds expiration
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * Get age of entry in milliseconds
     * @return age since creation
     */
    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }

    /**
     * Mark entry as accessed (updates LRU timestamp)
     */
    public void markAccessed() {
        this.lastAccessed = System.currentTimeMillis();
    }

    /**
     * Get last access timestamp
     * @return timestamp of last access
     */
    public long getLastAccessed() {
        return lastAccessed;
    }

    /**
     * Get cached value
     * @return cached object
     */
    public Object getValue() {
        return value;
    }

    /**
     * Get data type
     * @return data type category
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Get creation timestamp
     * @return creation time
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Get expiration timestamp
     * @return expiration time
     */
    public long getExpiresAt() {
        return expiresAt;
    }
}
