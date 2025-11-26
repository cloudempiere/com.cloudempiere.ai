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

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks routing performance metrics
 *
 * Metrics tracked:
 * - Cache hit rate (how often context is used)
 * - Database query count
 * - Context-only responses (no AI/DB call)
 * - Hybrid queries
 *
 * Use these metrics to tune TTL values and validate routing effectiveness
 *
 * @author Cloudempiere
 */
public class RoutingMetrics {

    /** Successful context hits */
    private final AtomicLong contextHits = new AtomicLong(0);

    /** Context misses (data not in cache) */
    private final AtomicLong contextMisses = new AtomicLong(0);

    /** Database queries executed */
    private final AtomicLong databaseQueries = new AtomicLong(0);

    /** Responses served entirely from context */
    private final AtomicLong contextOnlyResponses = new AtomicLong(0);

    /** Hybrid queries (context + database) */
    private final AtomicLong hybridQueries = new AtomicLong(0);

    /**
     * Record successful context hit
     */
    public void recordContextHit() {
        contextHits.incrementAndGet();
    }

    /**
     * Record context miss (data not found)
     */
    public void recordContextMiss() {
        contextMisses.incrementAndGet();
    }

    /**
     * Record database query executed
     */
    public void recordDatabaseQuery() {
        databaseQueries.incrementAndGet();
    }

    /**
     * Record context-only response (no AI/DB call)
     */
    public void recordContextOnlyResponse() {
        contextOnlyResponses.incrementAndGet();
    }

    /**
     * Record hybrid query (context + database)
     */
    public void recordHybridQuery() {
        hybridQueries.incrementAndGet();
    }

    /**
     * Calculate cache hit rate
     * @return hit rate as percentage (0.0 to 1.0)
     */
    public double getCacheHitRate() {
        long total = contextHits.get() + contextMisses.get();
        return total > 0 ? (double) contextHits.get() / total : 0.0;
    }

    /**
     * Get all metrics as map
     * @return metrics map
     */
    public Map<String, Object> getMetrics() {
        return Map.of(
            "context_hits", contextHits.get(),
            "context_misses", contextMisses.get(),
            "database_queries", databaseQueries.get(),
            "context_only_responses", contextOnlyResponses.get(),
            "hybrid_queries", hybridQueries.get(),
            "cache_hit_rate", getCacheHitRate(),
            "cache_hit_rate_percent", String.format("%.1f%%", getCacheHitRate() * 100)
        );
    }

    /**
     * Reset all metrics to zero
     */
    public void reset() {
        contextHits.set(0);
        contextMisses.set(0);
        databaseQueries.set(0);
        contextOnlyResponses.set(0);
        hybridQueries.set(0);
    }

    /**
     * Get context hits
     * @return count
     */
    public long getContextHits() {
        return contextHits.get();
    }

    /**
     * Get context misses
     * @return count
     */
    public long getContextMisses() {
        return contextMisses.get();
    }

    /**
     * Get database queries
     * @return count
     */
    public long getDatabaseQueries() {
        return databaseQueries.get();
    }

    /**
     * Get context-only responses
     * @return count
     */
    public long getContextOnlyResponses() {
        return contextOnlyResponses.get();
    }

    /**
     * Get hybrid queries
     * @return count
     */
    public long getHybridQueries() {
        return hybridQueries.get();
    }

    @Override
    public String toString() {
        return String.format("RoutingMetrics{hits=%d, misses=%d, db=%d, contextOnly=%d, hybrid=%d, hitRate=%.1f%%}",
                contextHits.get(),
                contextMisses.get(),
                databaseQueries.get(),
                contextOnlyResponses.get(),
                hybridQueries.get(),
                getCacheHitRate() * 100);
    }
}
