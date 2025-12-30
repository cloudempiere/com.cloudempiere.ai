# Streaming Rendering Implementation Plan

**Date:** 2025-12-18
**Target Version:** v0.31.0
**Priority:** P0 (Critical Performance Fix)

## Overview

This document provides concrete implementation steps to fix streaming table rendering issues by:

1. Implementing throttled rendering (50ms batching)
2. Processing zoom links only for complete cells
3. Validating performance improvements

## Phase 1: Throttled Rendering with Batching

### Step 1.1: Add Chunk Queue to AIChatStreamingMessage

**File:** `src/com/cloudempiere/ai/component/AIChatStreamingMessage.java`

**Add instance variables after line 131:**

```java
/** Streaming table renderer for cell-by-cell table rendering */
private StreamingTableRenderer tableRenderer;

// ========== ADD THESE FIELDS ==========
/** Queue for batching chunks before rendering (throttling) */
private final List<String> chunkQueue = new ArrayList<>();

/** Flag to track if render is scheduled */
private boolean renderScheduled = false;

/** Lock object for synchronizing chunk queue access */
private final Object queueLock = new Object();
// ======================================
```

---

### Step 1.2: Modify appendChunk() to Queue Instead of Immediate Render

**File:** `src/com/cloudempiere/ai/component/AIChatStreamingMessage.java`

**Replace method at line 313 with:**

```java
/**
 * Append a text chunk to the streaming content.
 *
 * <p>Called from onChunk callback via Executions.schedule().
 *
 * <p><b>Performance Note (ADR-047):</b> Chunks are queued and rendered
 * at fixed 50ms intervals to prevent DOM thrashing. This reduces updates
 * from 100+/sec to ~20/sec for smoother UX.
 *
 * @param chunk text chunk to append
 */
public void appendChunk(String chunk) {
    // Ignore chunks if cancelled or already complete
    if (isCancelled || isComplete) {
        return;
    }
    if (chunk == null || chunk.isEmpty()) {
        return;
    }

    // Clean chunk before processing (ADR-047 Phase 1)
    String cleaned = ChunkCleaner.clean(chunk);

    // Queue chunk for batched rendering
    synchronized (queueLock) {
        chunkQueue.add(cleaned);

        // Schedule render if not already pending
        if (!renderScheduled) {
            renderScheduled = true;
            scheduleRender();
        }
    }
}
```

---

### Step 1.3: Add scheduleRender() Method

**File:** `src/com/cloudempiere/ai/component/AIChatStreamingMessage.java`

**Add after appendChunk() method:**

```java
/**
 * Schedule a batched render update after 50ms delay.
 *
 * <p>Uses ZK's client-side timer mechanism to throttle DOM updates.
 * Multiple chunks are batched together and rendered once per interval.
 *
 * <p><b>Performance Impact:</b>
 * <ul>
 *   <li>Before: 100+ DOM updates/sec (per chunk)</li>
 *   <li>After: ~20 DOM updates/sec (every 50ms)</li>
 *   <li>Result: 80% reduction in browser reflows</li>
 * </ul>
 */
private void scheduleRender() {
    // Use JavaScript setTimeout to schedule server callback after 50ms
    // This ensures smooth 20 FPS rendering without overwhelming the browser
    String script = String.format(
        "setTimeout(function() {" +
        "  var w = zk.Widget.$('%s');" +
        "  if (w) {" +
        "    zAu.send(new zk.Event(w, 'onBatchRender'));" +
        "  }" +
        "}, 50);",  // 50ms = 20 FPS (smooth without overhead)
        getId()
    );

    org.zkoss.zk.ui.util.Clients.evalJavaScript(script);
}
```

---

### Step 1.4: Add onBatchRender() Event Handler

**File:** `src/com/cloudempiere/ai/component/AIChatStreamingMessage.java`

**Add after scheduleRender() method:**

```java
/**
 * Handle batched render event (triggered every 50ms by scheduleRender).
 *
 * <p>This method:
 * <ol>
 *   <li>Collects all queued chunks</li>
 *   <li>Processes them through content buffer and table renderer</li>
 *   <li>Updates DOM once for entire batch</li>
 *   <li>Reschedules if more chunks arrived during processing</li>
 * </ol>
 *
 * <p>Called via ZK event system from client-side JavaScript timer.
 */
public void onBatchRender() {
    // Collect batch of chunks
    List<String> batch;
    synchronized (queueLock) {
        if (chunkQueue.isEmpty()) {
            renderScheduled = false;
            return;
        }

        // Copy and clear queue
        batch = new ArrayList<>(chunkQueue);
        chunkQueue.clear();
    }

    // Process all chunks in batch
    for (String chunk : batch) {
        content.append(chunk);

        // Feed chunk to streaming table renderer for cell-by-cell rendering
        tableRenderer.appendChunk(chunk);
    }

    // Update DOM once for entire batch
    updateContentDisplay();

    // Reschedule if more chunks arrived during processing
    synchronized (queueLock) {
        if (!chunkQueue.isEmpty()) {
            scheduleRender();
        } else {
            renderScheduled = false;
        }
    }
}
```

---

### Step 1.5: Update complete() Method to Flush Remaining Chunks

**File:** `src/com/cloudempiere/ai/component/AIChatStreamingMessage.java`

**Replace method at line 413 with:**

```java
/**
 * Complete the message when streaming finishes.
 *
 * <p>Removes streaming cursor, enables copy button, renders final markdown using marked.js.
 *
 * <p><b>Note:</b> This method was renamed from finalize() to avoid conflict
 * with Java's Object.finalize() which is called by the garbage collector.
 */
public void complete() {
    if (isComplete) {
        return; // Already completed
    }

    // Flush any remaining chunks in queue
    synchronized (queueLock) {
        if (!chunkQueue.isEmpty()) {
            for (String chunk : chunkQueue) {
                content.append(chunk);
                tableRenderer.appendChunk(chunk);
            }
            chunkQueue.clear();
        }
        renderScheduled = false;
    }

    isComplete = true;
    // Use marked.js for full markdown rendering (tables, code blocks, etc.)
    renderFinalMarkdown();
    enableCopyButton();
}
```

---

## Phase 2: Process Zoom Links Only for Complete Cells

### Step 2.1: Remove Zoom Link Processing from Incomplete Cells

**File:** `src/com/cloudempiere/ai/util/StreamingTableRenderer.java`

**Modify renderPartialRow() method at line 432:**

**REMOVE lines 476-480:**

```java
// Render current cell with cursor
if (currentCellContent != null && !currentCellContent.trim().isEmpty()) {
    String align = getAlignment(completedCells.size(), isHeaderRow);
    String cellContent = currentCellContent.trim();

    // ❌ REMOVE THESE LINES (476-480)
    // Process zoom links for current cell during streaming (ADR-047 Phase 1)
    // if (ctx != null && widgetId != null) {
    //     cellContent = ZoomLinkProcessor.processZoomLinks(
    //         cellContent, ctx, widgetId);
    // }

    // Escape HTML unless it contains zoom links
    String safeContent = containsZoomLink(cellContent) ?
        cellContent : Util.maskHTML(cellContent, true);
    safeContent += "<span class='streaming-cursor'>|</span>";
```

**REPLACE with:**

```java
// Render current cell with cursor (NO zoom link processing)
if (currentCellContent != null && !currentCellContent.trim().isEmpty()) {
    String align = getAlignment(completedCells.size(), isHeaderRow);
    String cellContent = currentCellContent.trim();

    // ✅ NEW: Don't process zoom links for incomplete cells
    // Incomplete cells show raw text with cursor until pipe delimiter closes them
    // Zoom links are processed only after cell is complete (in renderRow or completed cells above)
    String safeContent = Util.maskHTML(cellContent, true);
    safeContent += "<span class='streaming-cursor'>|</span>";
```

---

### Step 2.2: Update Comments to Explain Incomplete Cell Handling

**File:** `src/com/cloudempiere/ai/util/StreamingTableRenderer.java`

**Add JavaDoc comment before renderPartialRow() at line 424:**

```java
/**
 * Render a partial row (currently being streamed).
 *
 * <p><b>Important:</b> Zoom link processing happens ONLY for completed cells,
 * not for the current cell being streamed. This prevents showing raw syntax
 * when link syntax is split across chunks.
 *
 * <p>Example:
 * <pre>
 * Chunk 1: "| [[C_BPartner:1000|Acm"  ← Shows as raw text with cursor
 * Chunk 2: "e Corp]] |"                ← Now complete, shows as link
 * </pre>
 *
 * @param completedCells cells that are complete in this row (will show zoom links)
 * @param currentCellContent content of cell currently being streamed (shows raw text)
 * @param isHeaderRow true if this is a header row
 * @param cursorCellIndex which cell to show cursor in
 */
private String renderPartialRow(List<String> completedCells, String currentCellContent,
                                 boolean isHeaderRow, int cursorCellIndex) {
```

---

## Phase 3: Testing and Validation

### Test 1: Verify DOM Update Frequency

**Create test script:** `test-streaming-performance.sh`

```bash
#!/bin/bash
# Test streaming performance and DOM update frequency

echo "=== Streaming Performance Test ==="
echo ""

# Start iDempiere server (if not running)
echo "1. Ensure iDempiere is running on http://localhost:8080"
echo ""

# Open browser with performance monitoring
echo "2. Open Chrome DevTools:"
echo "   - Performance tab"
echo "   - Record"
echo ""

# Test scenario
echo "3. Open AI Chat window and send:"
echo '   "Show me a table of all business partners with their orders"'
echo ""

echo "4. While streaming, watch for:"
echo "   - DOM update frequency (should be ~20/sec, not 100+/sec)"
echo "   - No visible flickering"
echo "   - Smooth cell-by-cell rendering"
echo ""

echo "5. After completion:"
echo "   - Stop recording"
echo "   - Check 'Main Thread' timeline"
echo "   - Count 'Recalculate Style' events"
echo "   - Expected: ~20 events/sec (one per 50ms interval)"
echo ""

echo "=== Performance Metrics ==="
echo "Before fix:"
echo "  - DOM updates: 100+ per second"
echo "  - Recalculate Style: Every chunk"
echo "  - Flickering: Multiple per table"
echo ""
echo "After fix (Target):"
echo "  - DOM updates: ~20 per second"
echo "  - Recalculate Style: Every 50ms"
echo "  - Flickering: Zero visible"
```

---

### Test 2: Verify Zoom Link Rendering

**Create test script:** `test-zoom-link-streaming.sh`

```bash
#!/bin/bash
# Test zoom link rendering during streaming

echo "=== Zoom Link Streaming Test ==="
echo ""

echo "1. Open AI Chat window"
echo ""

echo "2. Send prompt:"
echo '   "Show me a table with business partner [[C_BPartner:1000000|Test Corp]]"'
echo ""

echo "3. Watch during streaming:"
echo "   - Incomplete cells show raw syntax with cursor"
echo "   - Example: '[[C_BPartner:1000000|Test C|' (cursor after C)"
echo ""

echo "4. After cell completes (pipe arrives):"
echo "   - Cell should immediately show as clickable link"
echo "   - Text: 'Test Corp' (blue, underlined)"
echo "   - NO raw syntax visible"
echo ""

echo "5. Click link:"
echo "   - Should open Business Partner window"
echo "   - Should navigate to record ID 1000000"
echo ""

echo "=== Pass Criteria ==="
echo "✓ Incomplete cells show raw text (not processed links)"
echo "✓ Complete cells show clickable links immediately"
echo "✓ No flickering from raw → styled"
echo "✓ Links are clickable and functional"
```

---

### Test 3: Performance Metrics Collection

**Create monitoring class:** `StreamingPerformanceMonitor.java`

```java
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
 * <p>Usage:
 * <pre>
 * StreamingPerformanceMonitor monitor = new StreamingPerformanceMonitor();
 * monitor.startSession();
 *
 * // During streaming...
 * monitor.recordChunk();
 * monitor.recordDOMUpdate();
 *
 * // After completion...
 * monitor.endSession();
 * monitor.printReport();
 * </pre>
 */
public class StreamingPerformanceMonitor {

    private static final CLogger log = CLogger.getCLogger(StreamingPerformanceMonitor.class);

    private long sessionStart;
    private long sessionEnd;
    private int chunkCount;
    private int domUpdateCount;
    private long totalRenderTime;
    private int maxBatchSize;
    private int minBatchSize = Integer.MAX_VALUE;

    public void startSession() {
        sessionStart = System.currentTimeMillis();
        chunkCount = 0;
        domUpdateCount = 0;
        totalRenderTime = 0;
        maxBatchSize = 0;
        minBatchSize = Integer.MAX_VALUE;
    }

    public void recordChunk() {
        chunkCount++;
    }

    public void recordDOMUpdate(long renderTimeMs, int batchSize) {
        domUpdateCount++;
        totalRenderTime += renderTimeMs;
        maxBatchSize = Math.max(maxBatchSize, batchSize);
        minBatchSize = Math.min(minBatchSize, batchSize);
    }

    public void endSession() {
        sessionEnd = System.currentTimeMillis();
    }

    public void printReport() {
        long duration = sessionEnd - sessionStart;
        double durationSec = duration / 1000.0;

        log.warning("=== Streaming Performance Report ===");
        log.warning("Session Duration: " + durationSec + " seconds");
        log.warning("Total Chunks: " + chunkCount);
        log.warning("Total DOM Updates: " + domUpdateCount);
        log.warning("");

        // Frequency metrics
        double chunksPerSec = chunkCount / durationSec;
        double updatesPerSec = domUpdateCount / durationSec;
        double chunksPerUpdate = (double) chunkCount / domUpdateCount;

        log.warning("Chunk Rate: " + String.format("%.1f", chunksPerSec) + " chunks/sec");
        log.warning("DOM Update Rate: " + String.format("%.1f", updatesPerSec) + " updates/sec");
        log.warning("Batch Efficiency: " + String.format("%.1f", chunksPerUpdate) + " chunks/update");
        log.warning("");

        // Batch metrics
        log.warning("Max Batch Size: " + maxBatchSize + " chunks");
        log.warning("Min Batch Size: " + (minBatchSize == Integer.MAX_VALUE ? 0 : minBatchSize) + " chunks");
        log.warning("");

        // Render metrics
        double avgRenderTime = (double) totalRenderTime / domUpdateCount;
        log.warning("Avg Render Time: " + String.format("%.1f", avgRenderTime) + " ms");
        log.warning("Total Render Time: " + totalRenderTime + " ms");
        log.warning("");

        // Pass/fail evaluation
        boolean passFrequency = updatesPerSec < 25.0;  // Target: ~20/sec
        boolean passBatching = chunksPerUpdate > 3.0;  // Target: multiple chunks per update
        boolean passLatency = avgRenderTime < 100.0;   // Target: <100ms per render

        log.warning("=== Evaluation ===");
        log.warning("Update Frequency: " + (passFrequency ? "PASS" : "FAIL") + " (target: <25/sec)");
        log.warning("Batch Efficiency: " + (passBatching ? "PASS" : "FAIL") + " (target: >3 chunks/update)");
        log.warning("Render Latency: " + (passLatency ? "PASS" : "FAIL") + " (target: <100ms)");

        boolean overallPass = passFrequency && passBatching && passLatency;
        log.warning("");
        log.warning("Overall: " + (overallPass ? "✓ PASS" : "✗ FAIL"));
        log.warning("=====================================");
    }
}
```

**Integration:**

Add to `AIChatStreamingMessage.java`:

```java
// Add instance variable
private StreamingPerformanceMonitor perfMonitor;

// In constructor
if (log.isLoggable(Level.FINE)) {
    perfMonitor = new StreamingPerformanceMonitor();
    perfMonitor.startSession();
}

// In appendChunk()
if (perfMonitor != null) {
    perfMonitor.recordChunk();
}

// In onBatchRender()
long renderStart = System.currentTimeMillis();
// ... do rendering ...
long renderTime = System.currentTimeMillis() - renderStart;

if (perfMonitor != null) {
    perfMonitor.recordDOMUpdate(renderTime, batch.size());
}

// In complete()
if (perfMonitor != null) {
    perfMonitor.endSession();
    perfMonitor.printReport();
}
```

---

## Validation Checklist

After implementation, verify:

### Functional Tests
- [ ] Cell-by-cell rendering still works smoothly
- [ ] Tables with 5, 20, 100 rows render correctly
- [ ] Zoom links appear in completed cells
- [ ] Incomplete cells show raw text with cursor
- [ ] No flickering during streaming
- [ ] Final rendering shows all links correctly
- [ ] Copy button works after completion

### Performance Tests
- [ ] DOM update frequency: <25 updates/sec (target: ~20/sec)
- [ ] CPU usage during streaming: <15% average
- [ ] No visible UI freezes or jank
- [ ] Memory usage stable (no leaks)
- [ ] Batch efficiency: >3 chunks per DOM update

### Edge Cases
- [ ] Empty tables
- [ ] Single-row tables (header only)
- [ ] Tables with no zoom links
- [ ] Tables with multiple zoom links per cell
- [ ] Zoom link syntax split across chunks
- [ ] Very fast streaming (100+ chunks/sec)
- [ ] Very slow streaming (<1 chunk/sec)

---

## Rollback Plan

If issues occur after deployment:

### Symptom: Streaming slower or laggy
**Cause:** 50ms delay too aggressive for system
**Fix:** Increase interval to 100ms (10 FPS still smooth)
```javascript
}, 100);  // Change from 50ms to 100ms
```

### Symptom: Zoom links not appearing
**Cause:** Processing removed from wrong place
**Fix:** Restore zoom link processing in `renderRow()` for completed cells
**Verify:** Check lines 386-394 in `StreamingTableRenderer.java` still intact

### Symptom: Race conditions or null pointers
**Cause:** Synchronization issues in queue
**Fix:** Restore immediate rendering (remove batching)
```java
// Temporary rollback - process immediately
content.append(cleaned);
tableRenderer.appendChunk(cleaned);
updateContentDisplay();
```

---

## Success Criteria

### Quantitative Metrics
- DOM update frequency reduced by 80% (100+/sec → ~20/sec)
- Zero visible flickering during streaming
- <15% CPU usage during streaming
- <100ms average render latency

### Qualitative Metrics
- Users report smoother rendering
- No complaints about raw syntax showing
- Zoom links work reliably
- Tables appear progressively without jank

---

## Timeline

| Task | Effort | Status |
|------|--------|--------|
| 1.1: Add chunk queue | 15 min | Pending |
| 1.2: Modify appendChunk() | 30 min | Pending |
| 1.3: Add scheduleRender() | 20 min | Pending |
| 1.4: Add onBatchRender() | 30 min | Pending |
| 1.5: Update complete() | 15 min | Pending |
| 2.1: Remove incomplete cell processing | 10 min | Pending |
| 2.2: Update comments | 10 min | Pending |
| 3: Testing and validation | 2 hours | Pending |
| **Total** | **4 hours** | |

---

## Next Steps

1. Review this implementation plan with team
2. Create feature branch: `fix/streaming-rendering-throttling`
3. Implement Phase 1 (throttling)
4. Implement Phase 2 (zoom link fix)
5. Run performance tests
6. Code review and merge
7. Deploy to test environment
8. Validate metrics
9. Deploy to production

---

## References

- Analysis Document: `docs/STREAMING_RENDERING_ANALYSIS.md`
- ADR-047: Streaming Chat Rendering Best Practices
- Progressive Table Builder Research: `docs/progressivetablebuilder/`
