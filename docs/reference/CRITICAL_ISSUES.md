# Critical Issues - Chat Window Stability

> **Priority:** Fix these issues before production deployment
> **Last Updated:** 2025-12-26
> **Last Review:** Verified against codebase 2025-12-26

---

## Summary

| Priority | Issue | Status | Effort |
|----------|-------|--------|--------|
| **P0** | Unbounded Message Queue | NOT FIXED | 1 hour |
| **P1** | Invalid Desktop Reference | PARTIAL | 30 min |
| **P2** | Batch Render Throughput | PARTIAL | 2 hours |

---

## P0 - BLOCKER (Fix Immediately)

### 1. Unbounded Message Queue
**File:** `AIChatStreamingMessage.java:135`
**Impact:** HIGH | **Likelihood:** MEDIUM

**Problem:** If rendering is slower than streaming, queue grows unbounded → OOM.

```java
// CURRENT (UNBOUNDED):
private final List<String> chunkQueue = new ArrayList<>();

public void appendChunk(String chunk) {
    synchronized (queueLock) {
        chunkQueue.add(cleaned);  // No limit!
    }
}
```

**Risk:** 10KB response = 100 chunks. At 1MB/sec streaming with slow render → 20,000 chunks/sec → OOM.

**Fix:**
```java
private static final int MAX_QUEUE_SIZE = 5000;
private final ArrayBlockingQueue<String> chunkQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

public void appendChunk(String chunk) {
    if (isCancelled || isComplete) return;

    if (!chunkQueue.offer(cleaned)) {
        log.warning("Chunk queue full, applying backpressure");
        chunkQueue.poll();  // Drop oldest
        chunkQueue.offer(cleaned);
    }
}
```

**Effort:** 1 hour

---

## P1 - HIGH (Fix Before Production)

### 2. Invalid Desktop Reference in Callbacks
**File:** `AIChatWidget.java:984-1149`
**Impact:** MEDIUM | **Likelihood:** MEDIUM

**Problem:** Desktop captured in lambda becomes invalid if user navigates away. Has try-catch but minimal handling.

```java
// CURRENT (PARTIAL):
final Desktop desktop = Executions.getCurrent().getDesktop();
// ... later:
try {
    Executions.schedule(desktop, e -> {
        streamingMsg.appendChunk(chunk);
    }, new Event("onChunk"));
} catch (Exception e) {
    log.warning("Failed to schedule");  // Minimal handling
}
```

**Fix:** Add desktop.isAlive() check:
```java
private void safeSchedule(Desktop desktop, Consumer<Event> handler, Event event) {
    if (desktop == null || !desktop.isAlive()) {
        log.warning("Desktop no longer available, cancelling stream");
        cancelCurrentRequest();
        return;
    }
    try {
        Executions.schedule(desktop, e -> handler.accept(e), event);
    } catch (DesktopUnavailableException e) {
        log.warning("Desktop became unavailable: " + e.getMessage());
        cancelCurrentRequest();
    }
}
```

**Effort:** 30 minutes

---

## P2 - MEDIUM (Fix in Next Sprint)

### 3. Throughput Degradation in Batch Render
**File:** `AIChatStreamingMessage.java:380-445`
**Impact:** MEDIUM | **Likelihood:** LOW

**Problem:** Fixed 50ms batching works for typical usage but no adaptive sizing under heavy load.

**Current State:**
- ✅ Has 50ms batching (20 FPS)
- ✅ Processes multiple chunks per render
- ❌ No adaptive batch sizing based on render time
- ❌ Queue still unbounded (relates to Issue #1)

**Fix:** Implement adaptive batch sizing:
```java
private static final int MIN_BATCH_INTERVAL_MS = 30;
private static final int MAX_BATCH_INTERVAL_MS = 100;
private volatile int currentBatchInterval = 50;

private void onBatchRender() {
    long startTime = System.currentTimeMillis();
    // ... existing render logic ...
    long renderTime = System.currentTimeMillis() - startTime;

    // Adaptive: slow down if render is taking too long
    if (renderTime > currentBatchInterval * 0.8) {
        currentBatchInterval = Math.min(MAX_BATCH_INTERVAL_MS, currentBatchInterval + 10);
    } else if (renderTime < currentBatchInterval * 0.3) {
        currentBatchInterval = Math.max(MIN_BATCH_INTERVAL_MS, currentBatchInterval - 5);
    }
}
```

**Effort:** 2 hours

---

## Verification Checklist

- [ ] #1: Fast streaming with slow render → queue stays bounded, no OOM
- [ ] #2: Close window during stream → clean cancellation, no exception spam
- [ ] #3: Heavy load → latency stays reasonable

---

## Resolved Issues (2025-12-26)

The following issues have been verified as FIXED in the current codebase:

| Issue | Resolution |
|-------|------------|
| Cross-Tenant Data Access | Query uses AD_Client_ID filter (ChatAccessService.java:78-93) |
| NPE in Streaming Completion | Comprehensive null checks + try-catch (AIService.java:1012-1118) |
| Race Condition Thread Mgmt | Synchronized threadLock (AIChatWidget.java:937-973) |
| Streaming Cancellation Race | AtomicBoolean with compareAndSet (AIChatWidget.java:104, 2259) |
| Memory Leak Cache | Bounded LRU caches with MAX_SIZE=100 (AIService.java:109-150) |
| O(n²) Markdown Rendering | Fixed via 50ms batching (small chunks) |
| Exception Handler Guardrails | Catch-all with user-friendly messages (AIService.java:297-330) |
| Null Language Dereference | Null-safe ternary operators (AIService.java:772-773, 799) |

---

## Related ADRs

- [ADR-047](adr/047-streaming-chat-rendering-best-practices.md) - Streaming (relates to #1, #3)
- [ADR-051](adr/051-zk-ui-defensive-programming.md) - ZK UI Defensive Programming (relates to #2)
