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
package com.cloudempiere.ai.util;

import org.compiere.util.CLogger;

/**
 * Monitor performance metrics for streaming rendering.
 *
 * <p>Tracks:
 * <ul>
 *   <li>DOM update frequency</li>
 *   <li>Chunk processing rate</li>
 *   <li>Batch sizes</li>
 *   <li>Render latency</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 * StreamingPerformanceMonitor monitor = new StreamingPerformanceMonitor();
 * monitor.startSession();
 *
 * // During streaming...
 * monitor.recordChunk();
 * monitor.recordDOMUpdate(renderTimeMs, batchSize);
 *
 * // After completion...
 * monitor.endSession();
 * monitor.printReport();
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 * @see com.cloudempiere.ai.component.AIChatStreamingMessage
 */
public class StreamingPerformanceMonitor {

    private static final CLogger log = CLogger.getCLogger(StreamingPerformanceMonitor.class);

    /** Session start timestamp */
    private long sessionStart;

    /** Session end timestamp */
    private long sessionEnd;

    /** Total number of chunks received */
    private int chunkCount;

    /** Total number of DOM updates performed */
    private int domUpdateCount;

    /** Total time spent rendering (milliseconds) */
    private long totalRenderTime;

    /** Maximum batch size seen */
    private int maxBatchSize;

    /** Minimum batch size seen */
    private int minBatchSize = Integer.MAX_VALUE;

    /**
     * Start a new performance monitoring session.
     */
    public void startSession() {
        sessionStart = System.currentTimeMillis();
        chunkCount = 0;
        domUpdateCount = 0;
        totalRenderTime = 0;
        maxBatchSize = 0;
        minBatchSize = Integer.MAX_VALUE;
    }

    /**
     * Record a chunk being received.
     */
    public void recordChunk() {
        chunkCount++;
    }

    /**
     * Record a DOM update with its render time and batch size.
     *
     * @param renderTimeMs time taken to render (milliseconds)
     * @param batchSize number of chunks in this batch
     */
    public void recordDOMUpdate(long renderTimeMs, int batchSize) {
        domUpdateCount++;
        totalRenderTime += renderTimeMs;
        maxBatchSize = Math.max(maxBatchSize, batchSize);
        minBatchSize = Math.min(minBatchSize, batchSize);
    }

    /**
     * End the performance monitoring session.
     */
    public void endSession() {
        sessionEnd = System.currentTimeMillis();
    }

    /**
     * Print performance report to log.
     *
     * <p>Report includes:
     * <ul>
     *   <li>Session duration</li>
     *   <li>Chunk and update rates</li>
     *   <li>Batch efficiency metrics</li>
     *   <li>Render latency statistics</li>
     *   <li>Pass/fail evaluation against targets</li>
     * </ul>
     */
    public void printReport() {
        long duration = sessionEnd - sessionStart;
        double durationSec = duration / 1000.0;

        log.warning("=== Streaming Performance Report ===");
        log.warning("Session Duration: " + String.format("%.2f", durationSec) + " seconds");
        log.warning("Total Chunks: " + chunkCount);
        log.warning("Total DOM Updates: " + domUpdateCount);
        log.warning("");

        // Frequency metrics
        double chunksPerSec = chunkCount / durationSec;
        double updatesPerSec = domUpdateCount / durationSec;
        double chunksPerUpdate = domUpdateCount > 0 ? (double) chunkCount / domUpdateCount : 0;

        log.warning("Chunk Rate: " + String.format("%.1f", chunksPerSec) + " chunks/sec");
        log.warning("DOM Update Rate: " + String.format("%.1f", updatesPerSec) + " updates/sec");
        log.warning("Batch Efficiency: " + String.format("%.1f", chunksPerUpdate) + " chunks/update");
        log.warning("");

        // Batch metrics
        log.warning("Max Batch Size: " + maxBatchSize + " chunks");
        log.warning("Min Batch Size: " + (minBatchSize == Integer.MAX_VALUE ? 0 : minBatchSize) + " chunks");
        log.warning("");

        // Render metrics
        double avgRenderTime = domUpdateCount > 0 ? (double) totalRenderTime / domUpdateCount : 0;
        log.warning("Avg Render Time: " + String.format("%.1f", avgRenderTime) + " ms");
        log.warning("Total Render Time: " + totalRenderTime + " ms");
        log.warning("");

        // Pass/fail evaluation
        boolean passFrequency = updatesPerSec < 25.0;  // Target: ~20/sec
        boolean passBatching = chunksPerUpdate > 3.0;  // Target: multiple chunks per update
        boolean passLatency = avgRenderTime < 100.0;   // Target: <100ms per render

        log.warning("=== Evaluation ===");
        log.warning("Update Frequency: " + (passFrequency ? "PASS" : "FAIL") +
                   " (target: <25/sec, actual: " + String.format("%.1f", updatesPerSec) + "/sec)");
        log.warning("Batch Efficiency: " + (passBatching ? "PASS" : "FAIL") +
                   " (target: >3 chunks/update, actual: " + String.format("%.1f", chunksPerUpdate) + ")");
        log.warning("Render Latency: " + (passLatency ? "PASS" : "FAIL") +
                   " (target: <100ms, actual: " + String.format("%.1f", avgRenderTime) + "ms)");

        boolean overallPass = passFrequency && passBatching && passLatency;
        log.warning("");
        log.warning("Overall: " + (overallPass ? "PASS" : "FAIL"));
        log.warning("=====================================");
    }

    /**
     * Get performance summary as a formatted string.
     *
     * @return summary string with key metrics
     */
    public String getSummary() {
        if (sessionEnd == 0) {
            return "Performance monitoring not complete";
        }

        long duration = sessionEnd - sessionStart;
        double durationSec = duration / 1000.0;
        double updatesPerSec = domUpdateCount / durationSec;
        double chunksPerUpdate = domUpdateCount > 0 ? (double) chunkCount / domUpdateCount : 0;
        double avgRenderTime = domUpdateCount > 0 ? (double) totalRenderTime / domUpdateCount : 0;

        return String.format(
            "Duration: %.2fs | Chunks: %d | Updates: %d (%.1f/sec) | Efficiency: %.1f chunks/update | Avg Render: %.1fms",
            durationSec, chunkCount, domUpdateCount, updatesPerSec, chunksPerUpdate, avgRenderTime
        );
    }

    /**
     * Get chunk count.
     *
     * @return total chunks received
     */
    public int getChunkCount() {
        return chunkCount;
    }

    /**
     * Get DOM update count.
     *
     * @return total DOM updates performed
     */
    public int getDOMUpdateCount() {
        return domUpdateCount;
    }

    /**
     * Get session duration in milliseconds.
     *
     * @return duration or 0 if session not complete
     */
    public long getDuration() {
        return sessionEnd > 0 ? sessionEnd - sessionStart : 0;
    }

    /**
     * Get DOM update frequency (updates per second).
     *
     * @return updates per second or 0 if session not complete
     */
    public double getUpdateFrequency() {
        long duration = getDuration();
        if (duration == 0) {
            return 0;
        }
        double durationSec = duration / 1000.0;
        return domUpdateCount / durationSec;
    }

    /**
     * Get batch efficiency (average chunks per update).
     *
     * @return chunks per update or 0 if no updates
     */
    public double getBatchEfficiency() {
        return domUpdateCount > 0 ? (double) chunkCount / domUpdateCount : 0;
    }

    /**
     * Get average render time per update.
     *
     * @return average render time in milliseconds
     */
    public double getAverageRenderTime() {
        return domUpdateCount > 0 ? (double) totalRenderTime / domUpdateCount : 0;
    }
}
