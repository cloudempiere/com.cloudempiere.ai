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

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.compiere.util.CLogger;

/**
 * Manages conversation-level context with TTL-based caching
 * Stores recent query results and entity data to reduce database calls
 *
 * Features:
 * - TTL-based expiration for cache freshness
 * - LRU eviction when capacity is reached
 * - Thread-safe concurrent access
 * - Pronoun resolution tracking (it, that, this)
 * - Entity history for conversation continuity
 *
 * @author Cloudempiere
 */
public class ConversationContextManager {

    private static final CLogger log = CLogger.getCLogger(ConversationContextManager.class);

    /** Context storage with concurrent access support */
    private final Map<String, ContextEntry> data = new ConcurrentHashMap<>();

    /** Default TTL in milliseconds */
    private final long defaultTtlMs;

    /** Maximum entries before LRU eviction kicks in */
    private final int maxEntries;

    /** Track last accessed entity for pronoun resolution */
    private String lastEntityKey;

    /** Entity history stack for "previous" references */
    private final Stack<String> entityHistory = new Stack<>();

    /**
     * Constructor
     * @param defaultTtl default time-to-live duration
     * @param maxEntries maximum entries before LRU eviction
     */
    public ConversationContextManager(Duration defaultTtl, int maxEntries) {
        this.defaultTtlMs = defaultTtl.toMillis();
        this.maxEntries = maxEntries;
        log.fine("ConversationContextManager initialized (TTL: " + defaultTtl.toMinutes() +
                "min, max entries: " + maxEntries + ")");
    }

    /**
     * Store data with custom TTL
     * @param key unique key for the data
     * @param value data to store
     * @param ttlMs time-to-live in milliseconds
     */
    public void put(String key, Object value, long ttlMs) {
        cleanupExpired();
        enforceMaxEntries();

        data.put(key, new ContextEntry(
            value,
            System.currentTimeMillis() + ttlMs,
            determineDataType(key, value)
        ));

        lastEntityKey = key;
        log.fine("Cached: " + key + " (TTL: " + (ttlMs / 1000) + "s)");
    }

    /**
     * Store data with default TTL
     * @param key unique key for the data
     * @param value data to store
     */
    public void put(String key, Object value) {
        put(key, value, defaultTtlMs);
    }

    /**
     * Get data if not expired
     * @param key unique key
     * @param <T> expected return type
     * @return cached value or null if expired/not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        // Handle pronoun resolution
        if ("LAST_ENTITY".equals(key) && lastEntityKey != null) {
            key = lastEntityKey;
            log.fine("Resolved LAST_ENTITY to: " + key);
        }

        ContextEntry entry = data.get(key);
        if (entry == null) {
            log.fine("Cache miss: " + key);
            return null;
        }

        if (entry.isExpired()) {
            data.remove(key);
            log.fine("Expired entry removed: " + key + " (age: " +
                    (entry.getAge() / 1000) + "s)");
            return null;
        }

        entry.markAccessed();
        log.fine("Cache hit: " + key);
        return (T) entry.getValue();
    }

    /**
     * Get multiple keys at once
     * @param keys list of keys to retrieve
     * @return map of found values (excludes expired/missing entries)
     */
    public Map<String, Object> getMultiple(List<String> keys) {
        Map<String, Object> result = new HashMap<>();
        for (String key : keys) {
            Object value = get(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Check if key exists and is not expired
     * @param key unique key
     * @return true if exists and valid
     */
    public boolean has(String key) {
        return get(key) != null;
    }

    /**
     * Check if context has any data
     * @return true if context is not empty
     */
    public boolean hasData() {
        cleanupExpired();
        return !data.isEmpty();
    }

    /**
     * Track entity mention for pronoun resolution
     * Enables "What's its status?" to work after mentioning an entity
     *
     * @param entityType entity type (ORDER, CUSTOMER, PRODUCT, etc.)
     * @param entityId entity identifier
     */
    public void trackEntity(String entityType, String entityId) {
        String key = entityType + "_" + entityId;
        lastEntityKey = key;
        entityHistory.push(key);

        // Limit history size to prevent memory growth
        if (entityHistory.size() > 10) {
            entityHistory.remove(0);
        }

        log.fine("Tracked entity: " + key);
    }

    /**
     * Resolve pronoun reference to entity key
     * @param pronoun pronoun to resolve (it, that, this, previous)
     * @return entity key or null if can't resolve
     */
    public String resolveReference(String pronoun) {
        String resolved = null;

        if ("it".equalsIgnoreCase(pronoun) ||
            "that".equalsIgnoreCase(pronoun) ||
            "this".equalsIgnoreCase(pronoun)) {
            resolved = lastEntityKey;
        } else if ("previous".equalsIgnoreCase(pronoun) && entityHistory.size() > 1) {
            resolved = entityHistory.get(entityHistory.size() - 2);
        }

        if (resolved != null) {
            log.fine("Resolved pronoun '" + pronoun + "' to: " + resolved);
        }

        return resolved;
    }

    /**
     * Get description of context contents for LLM system prompt
     * @return formatted description of cached entities
     */
    public String describeContents() {
        cleanupExpired();

        if (data.isEmpty()) {
            return "No recent conversation context available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Recently discussed entities:\n");

        Map<DataType, List<String>> byType = new EnumMap<>(DataType.class);

        data.forEach((key, entry) -> {
            if (!entry.isExpired()) {
                byType.computeIfAbsent(entry.getDataType(), k -> new ArrayList<>())
                      .add(key);
            }
        });

        byType.forEach((type, keys) -> {
            sb.append("- ").append(type).append(": ")
              .append(String.join(", ", keys)).append("\n");
        });

        return sb.toString();
    }

    /**
     * Clear all context data
     */
    public void clear() {
        data.clear();
        lastEntityKey = null;
        entityHistory.clear();
        log.fine("Context cleared");
    }

    /**
     * Get current cache size
     * @return number of entries
     */
    public int size() {
        cleanupExpired();
        return data.size();
    }

    /**
     * Remove expired entries from cache
     */
    private void cleanupExpired() {
        int removedCount = 0;
        Iterator<Map.Entry<String, ContextEntry>> iterator = data.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, ContextEntry> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.fine("Cleaned up " + removedCount + " expired entries");
        }
    }

    /**
     * Enforce maximum entries using LRU eviction
     */
    private void enforceMaxEntries() {
        if (data.size() >= maxEntries) {
            // Find and remove least recently accessed entry
            String lruKey = data.entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().getLastAccessed()))
                .map(Map.Entry::getKey)
                .orElse(null);

            if (lruKey != null) {
                data.remove(lruKey);
                log.fine("Evicted LRU entry: " + lruKey + " (cache full at " + maxEntries + " entries)");
            }
        }
    }

    /**
     * Determine data type from key and value
     * @param key cache key
     * @param value cached value
     * @return data type category
     */
    private DataType determineDataType(String key, Object value) {
        // Check key first (most reliable)
        String lowerKey = key.toLowerCase();

        if (lowerKey.contains("customer") || lowerKey.contains("bpartner")) {
            return DataType.CUSTOMER;
        }
        if (lowerKey.contains("order")) {
            return DataType.ORDER;
        }
        if (lowerKey.contains("product")) {
            return DataType.PRODUCT;
        }
        if (lowerKey.contains("invoice")) {
            return DataType.INVOICE;
        }
        if (lowerKey.contains("shipment") || lowerKey.contains("inout")) {
            return DataType.SHIPMENT;
        }
        if (lowerKey.contains("payment")) {
            return DataType.PAYMENT;
        }
        if (lowerKey.contains("request")) {
            return DataType.REQUEST;
        }

        // Fallback: inspect value class name
        String className = value.getClass().getSimpleName().toLowerCase();

        if (className.contains("customer") || className.contains("bpartner")) {
            return DataType.CUSTOMER;
        }
        if (className.contains("order")) {
            return DataType.ORDER;
        }
        if (className.contains("product")) {
            return DataType.PRODUCT;
        }
        if (className.contains("invoice")) {
            return DataType.INVOICE;
        }
        if (className.contains("shipment")) {
            return DataType.SHIPMENT;
        }
        if (className.contains("payment")) {
            return DataType.PAYMENT;
        }
        if (className.contains("request")) {
            return DataType.REQUEST;
        }

        return DataType.OTHER;
    }
}
