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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;

/**
 * Manages conversation-level context with TTL-based caching
 * Stores recent query results and entity data to reduce database calls
 *
 * Features:
 * - TTL-based expiration for cache freshness
 * - LRU eviction when capacity is reached
 * - Thread-safe concurrent access
 * - Thread-scoped caching (each conversation thread has isolated cache)
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

    /** Current thread root ID for thread-scoped caching (0 = global/no thread) */
    private int currentThreadRootId = 0;

    /** Track last accessed entity for pronoun resolution (per-thread) */
    private final Map<Integer, String> lastEntityKeyByThread = new ConcurrentHashMap<>();

    /** Entity history stack for "previous" references (per-thread) */
    private final Map<Integer, Stack<String>> entityHistoryByThread = new ConcurrentHashMap<>();

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
     * Set the current thread root ID for thread-scoped caching
     * All subsequent put/get operations will be scoped to this thread
     *
     * @param threadRootId thread root message ID (0 = global/no thread scope)
     */
    public void setCurrentThreadRootId(int threadRootId) {
        this.currentThreadRootId = threadRootId;
        log.fine("Thread context set to: " + (threadRootId > 0 ? threadRootId : "global"));
    }

    /**
     * Get the current thread root ID
     * @return current thread root ID (0 = global/no thread scope)
     */
    public int getCurrentThreadRootId() {
        return currentThreadRootId;
    }

    /**
     * Build thread-scoped key from base key
     * @param key base key
     * @return thread-scoped key
     */
    private String buildThreadKey(String key) {
        if (currentThreadRootId <= 0) {
            return key; // Global scope
        }
        return "t" + currentThreadRootId + ":" + key;
    }

    /**
     * Get entity history stack for current thread
     * @return entity history stack
     */
    private Stack<String> getEntityHistory() {
        return entityHistoryByThread.computeIfAbsent(currentThreadRootId, k -> new Stack<>());
    }

    /**
     * Get last entity key for current thread
     * @return last entity key or null
     */
    private String getLastEntityKey() {
        return lastEntityKeyByThread.get(currentThreadRootId);
    }

    /**
     * Set last entity key for current thread
     * @param key entity key
     */
    private void setLastEntityKey(String key) {
        lastEntityKeyByThread.put(currentThreadRootId, key);
    }

    /**
     * Store data with custom TTL
     * Data is stored with thread-scoped key if currentThreadRootId is set
     *
     * @param key unique key for the data
     * @param value data to store
     * @param ttlMs time-to-live in milliseconds
     */
    public void put(String key, Object value, long ttlMs) {
        cleanupExpired();
        enforceMaxEntries();

        String threadKey = buildThreadKey(key);
        data.put(threadKey, new ContextEntry(
            value,
            System.currentTimeMillis() + ttlMs,
            determineDataType(key, value)
        ));

        setLastEntityKey(threadKey);
        log.fine("Cached: " + threadKey + " (TTL: " + (ttlMs / 1000) + "s)" +
                (currentThreadRootId > 0 ? " [thread: " + currentThreadRootId + "]" : ""));
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
     * Data is retrieved with thread-scoped key if currentThreadRootId is set
     *
     * @param key unique key
     * @param <T> expected return type
     * @return cached value or null if expired/not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        String lastEntity = getLastEntityKey();

        // Handle pronoun resolution
        if ("LAST_ENTITY".equals(key) && lastEntity != null) {
            key = lastEntity; // Already thread-scoped
            log.fine("Resolved LAST_ENTITY to: " + key);
        } else {
            key = buildThreadKey(key);
        }

        ContextEntry entry = data.get(key);
        if (entry == null) {
            log.fine("Cache miss: " + key +
                    (currentThreadRootId > 0 ? " [thread: " + currentThreadRootId + "]" : ""));
            return null;
        }

        if (entry.isExpired()) {
            data.remove(key);
            log.fine("Expired entry removed: " + key + " (age: " +
                    (entry.getAge() / 1000) + "s)");
            return null;
        }

        entry.markAccessed();
        log.fine("Cache hit: " + key +
                (currentThreadRootId > 0 ? " [thread: " + currentThreadRootId + "]" : ""));
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
     * Check if context has any data for the current thread
     * @return true if context is not empty for current thread
     */
    public boolean hasData() {
        cleanupExpired();

        if (data.isEmpty()) {
            return false;
        }

        // Check if there's data for the current thread
        String threadPrefix = currentThreadRootId > 0 ? "t" + currentThreadRootId + ":" : "";

        for (String key : data.keySet()) {
            boolean matchesThread = (currentThreadRootId <= 0 && !key.contains(":"))
                    || (currentThreadRootId > 0 && key.startsWith(threadPrefix));
            if (matchesThread) {
                return true;
            }
        }

        return false;
    }

    /**
     * Track entity mention for pronoun resolution
     * Enables "What's its status?" to work after mentioning an entity
     * Entity is tracked per-thread to maintain conversation isolation
     *
     * @param entityType entity type (ORDER, CUSTOMER, PRODUCT, etc.)
     * @param entityId entity identifier
     */
    public void trackEntity(String entityType, String entityId) {
        String key = buildThreadKey(entityType + "_" + entityId);
        setLastEntityKey(key);

        Stack<String> history = getEntityHistory();
        history.push(key);

        // Limit history size to prevent memory growth
        if (history.size() > 10) {
            history.remove(0);
        }

        log.fine("Tracked entity: " + key +
                (currentThreadRootId > 0 ? " [thread: " + currentThreadRootId + "]" : ""));
    }

    /**
     * Resolve pronoun reference to entity key
     * Resolves within current thread's entity history
     *
     * @param pronoun pronoun to resolve (it, that, this, previous)
     * @return entity key or null if can't resolve
     */
    public String resolveReference(String pronoun) {
        String resolved = null;
        String lastEntity = getLastEntityKey();
        Stack<String> history = getEntityHistory();

        if ("it".equalsIgnoreCase(pronoun) ||
            "that".equalsIgnoreCase(pronoun) ||
            "this".equalsIgnoreCase(pronoun)) {
            resolved = lastEntity;
        } else if ("previous".equalsIgnoreCase(pronoun) && history.size() > 1) {
            resolved = history.get(history.size() - 2);
        }

        if (resolved != null) {
            log.fine("Resolved pronoun '" + pronoun + "' to: " + resolved +
                    (currentThreadRootId > 0 ? " [thread: " + currentThreadRootId + "]" : ""));
        }

        return resolved;
    }

    /**
     * Get description of context contents for LLM system prompt
     * Only includes entries for the current thread
     *
     * @return formatted description of cached entities
     */
    public String describeContents() {
        cleanupExpired();

        String threadPrefix = currentThreadRootId > 0 ? "t" + currentThreadRootId + ":" : "";

        // Filter entries by current thread
        Map<DataType, List<String>> byType = new EnumMap<>(DataType.class);
        int threadEntryCount = 0;

        for (Map.Entry<String, ContextEntry> mapEntry : data.entrySet()) {
            String key = mapEntry.getKey();
            ContextEntry entry = mapEntry.getValue();

            // Filter by thread: include if key starts with thread prefix, or no thread and no prefix
            boolean matchesThread = (currentThreadRootId <= 0 && !key.contains(":"))
                    || (currentThreadRootId > 0 && key.startsWith(threadPrefix));

            if (matchesThread && !entry.isExpired()) {
                // Strip thread prefix for display
                String displayKey = key.startsWith(threadPrefix) ? key.substring(threadPrefix.length()) : key;
                byType.computeIfAbsent(entry.getDataType(), k -> new ArrayList<>())
                      .add(displayKey);
                threadEntryCount++;
            }
        }

        if (threadEntryCount == 0) {
            return "No recent conversation context available" +
                    (currentThreadRootId > 0 ? " for this thread." : ".");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Recently discussed entities:\n");

        byType.forEach((type, keys) -> {
            sb.append("- ").append(type).append(": ")
              .append(String.join(", ", keys)).append("\n");
        });

        return sb.toString();
    }

    /**
     * Clear all context data for the current thread
     * If no thread is set, clears all global (non-thread-scoped) data
     */
    public void clear() {
        if (currentThreadRootId <= 0) {
            // Clear global entries only (keys without thread prefix)
            data.entrySet().removeIf(e -> !e.getKey().contains(":"));
            lastEntityKeyByThread.remove(0);
            entityHistoryByThread.remove(0);
            log.fine("Global context cleared");
        } else {
            // Clear entries for current thread only
            String threadPrefix = "t" + currentThreadRootId + ":";
            data.entrySet().removeIf(e -> e.getKey().startsWith(threadPrefix));
            lastEntityKeyByThread.remove(currentThreadRootId);
            entityHistoryByThread.remove(currentThreadRootId);
            log.fine("Thread " + currentThreadRootId + " context cleared");
        }
    }

    /**
     * Clear all context data for all threads
     */
    public void clearAll() {
        data.clear();
        lastEntityKeyByThread.clear();
        entityHistoryByThread.clear();
        log.fine("All context cleared");
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
