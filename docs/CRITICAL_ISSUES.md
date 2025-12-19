# Critical Issues - Chat Window Stability

> **Priority:** Fix these issues before production deployment
> **Last Updated:** 2025-12-19

---

## Priority Matrix

| Priority | Impact | Likelihood | Count |
|----------|--------|------------|-------|
| **P0 - BLOCKER** | CRITICAL | HIGH/MEDIUM | 4 |
| **P1 - HIGH** | HIGH | HIGH/MEDIUM | 4 |
| **P2 - MEDIUM** | MEDIUM | MEDIUM/LOW | 3 |

---

## P0 - BLOCKERS (Fix Immediately)

### 1. Cross-Tenant Data Access Vulnerability
**File:** `ChatAccessService.java:78-103`
**Impact:** CRITICAL | **Likelihood:** MEDIUM

**Problem:** Chat is loaded from database BEFORE tenant validation. Attackers can query any chat by ID.

```java
// CURRENT (VULNERABLE):
MChat chat = new MChat(ctx, CM_Chat_ID, trxName);  // Loads ANY chat
if (chat.get_ID() == 0) return ChatAccess.NONE;
return getAccess(ctx, chat);  // Tenant check happens AFTER load
```

**Fix:**
```java
// SECURE:
String sql = "SELECT * FROM CM_Chat WHERE CM_Chat_ID=? AND AD_Client_ID=?";
MChat chat = new Query(ctx, MChat.Table_Name, "CM_Chat_ID=? AND AD_Client_ID=?", trxName)
    .setParameters(CM_Chat_ID, Env.getAD_Client_ID(ctx))
    .first();
if (chat == null) return ChatAccess.NONE;
```

**Effort:** 30 minutes

---

### 2. NPE in Streaming Completion Handler
**File:** `AIService.java:1050-1065`
**Impact:** CRITICAL | **Likelihood:** MEDIUM

**Problem:** `onComplete()` callback doesn't consistently null-check response, causing chat to hang.

```java
// CURRENT (VULNERABLE):
.onComplete(response -> {
    String aiResponseText = responseAccumulator.get().toString();
    if (response != null && response.content() != null) {
        aiResponseText = response.content().text();  // Can still NPE on text()
    }
    TokenUsage tokenUsage = response != null ? response.tokenUsage() : null;
    // What if response.tokenUsage() returns null?
})
```

**Fix:**
```java
.onComplete(response -> {
    try {
        String aiResponseText = responseAccumulator.get().toString();
        if (response != null && response.content() != null
            && response.content().text() != null) {
            aiResponseText = response.content().text();
        }
        TokenUsage tokenUsage = (response != null) ? response.tokenUsage() : null;
        int inputTokens = (tokenUsage != null) ? tokenUsage.inputTokenCount() : 0;
        int outputTokens = (tokenUsage != null) ? tokenUsage.outputTokenCount() : 0;
        // ... rest of handler
    } catch (Exception e) {
        log.severe("Error in completion handler: " + e.getMessage());
        callback.onError(e);
    }
})
```

**Effort:** 1 hour

---

### 3. Race Condition in Thread Management
**File:** `AIChatWidget.java:909-937`
**Impact:** HIGH | **Likelihood:** HIGH

**Problem:** User message save, thread ID update, and streaming start are not atomic. Rapid messages cause thread corruption.

```java
// CURRENT (RACE CONDITION):
MAIChatEntry userEntry = new MAIChatEntry(chat, message);
userEntry.saveEx();  // COMMITTED

if (currentThreadRootId == 0) {
    currentThreadRootId = userEntry.getCM_ChatEntry_ID();
    loadThreadList();  // LONG-RUNNING - meanwhile streaming starts
}

// Streaming starts in parallel, may use wrong thread ID
currentRequest = CompletableFuture.runAsync(() -> { ... });
```

**Fix:**
```java
// ATOMIC OPERATION:
synchronized (threadLock) {
    MAIChatEntry userEntry = new MAIChatEntry(chat, message);
    userEntry.saveEx();

    if (currentThreadRootId == 0) {
        currentThreadRootId = userEntry.getCM_ChatEntry_ID();
    }

    final int threadId = currentThreadRootId;  // Capture before async
    currentRequest = CompletableFuture.runAsync(() -> {
        langchainService.chatStreamingWithContext(..., threadId, ...);
    });
}
loadThreadList();  // Move OUTSIDE synchronized block
```

**Effort:** 2 hours

---

### 4. Invalid Desktop Reference in Callbacks
**File:** `AIChatWidget.java:948-1249`
**Impact:** CRITICAL | **Likelihood:** MEDIUM

**Problem:** Desktop captured in lambda becomes invalid if user navigates away. Silent failure with no error.

```java
// CURRENT (SILENT FAILURE):
final Desktop desktop = Executions.getCurrent().getDesktop();
// ... later:
Executions.schedule(desktop, e -> {
    streamingMsg.appendChunk(chunk);  // Throws DesktopUnavailableException
}, new Event("onChunk"));
```

**Fix:**
```java
private void safeSchedule(Desktop desktop, Consumer<Event> handler, Event event) {
    if (desktop == null || !desktop.isAlive()) {
        log.warning("Desktop no longer available, cannot schedule UI update");
        return;
    }
    try {
        Executions.schedule(desktop, e -> handler.accept(e), event);
    } catch (DesktopUnavailableException e) {
        log.warning("Desktop became unavailable: " + e.getMessage());
    }
}

// Usage:
safeSchedule(desktop, e -> streamingMsg.appendChunk(chunk), new Event("onChunk"));
```

**Effort:** 1 hour

---

## P1 - HIGH (Fix Before Production)

### 5. Race Condition in Streaming Cancellation
**File:** `AIChatWidget.java:2155-2182`
**Impact:** HIGH | **Likelihood:** HIGH

**Problem:** Cancellation flag check and streaming stop are not atomic. Chunks appear after cancel.

```java
// CURRENT:
private volatile boolean requestCancelled = false;

// In cancel:
requestCancelled = true;  // Flag set
// But streaming thread may have already passed the check

// In streaming callback:
if (requestCancelled) return;  // Check happens BEFORE flag is set!
```

**Fix:**
```java
private final AtomicBoolean requestCancelled = new AtomicBoolean(false);

// In cancel:
if (requestCancelled.compareAndSet(false, true)) {
    // Only execute cancel logic once
    streamingInProgress = false;
    if (currentStreamingMessage != null) {
        currentStreamingMessage.markCancelled();
    }
}

// In streaming callback:
if (requestCancelled.get()) return;
```

**Effort:** 1 hour

---

### 6. Memory Leak in Agent/Model Cache
**File:** `AIService.java:106-1209`
**Impact:** MEDIUM | **Likelihood:** HIGH

**Problem:** Caches grow unbounded. Long-running servers eventually OOM.

```java
// CURRENT (UNBOUNDED):
private final Map<Integer, ERPAgent> agentCache = new ConcurrentHashMap<>();
private final Map<Integer, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();
```

**Fix:**
```java
// BOUNDED WITH LRU EVICTION:
private static final int MAX_CACHE_SIZE = 100;

private final Map<Integer, ERPAgent> agentCache = Collections.synchronizedMap(
    new LinkedHashMap<Integer, ERPAgent>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, ERPAgent> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    }
);
```

**Effort:** 2 hours

---

### 7. O(n²) Markdown Rendering Performance
**File:** `AIChatStreamingMessage.java:1031-1075`
**Impact:** HIGH | **Likelihood:** MEDIUM

**Problem:** Nested `indexOf()` calls in loop create O(n²) complexity. 10KB response = noticeable lag.

```java
// CURRENT (O(n²)):
while (pos < text.length()) {
    int tagStart = text.indexOf('<', pos);     // O(n)
    int tagEnd = text.indexOf('>', tagStart);  // O(n)
    int closingPos = text.indexOf(closingTag, tagEnd + 1);  // O(n)
    // Total: O(n²) for each iteration
}
```

**Fix:** Use StringBuilder indices or regex with bounded matching:
```java
// BETTER: Use pre-compiled regex with bounded lookahead
private static final Pattern HTML_TAG = Pattern.compile("<([a-z]+)[^>]*>([\\s\\S]*?)</\\1>",
    Pattern.CASE_INSENSITIVE);

private String processMarkdownPreservingHTML(String text) {
    Matcher m = HTML_TAG.matcher(text);
    StringBuffer result = new StringBuffer();
    while (m.find()) {
        m.appendReplacement(result, m.group(0));  // Preserve HTML as-is
    }
    m.appendTail(result);
    return processSimpleMarkdown(result.toString());
}
```

**Effort:** 3 hours

---

### 8. Unbounded Message Queue
**File:** `AIChatStreamingMessage.java:136-354`
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

**Fix:**
```java
private static final int MAX_QUEUE_SIZE = 1000;
private final ArrayBlockingQueue<String> chunkQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

public void appendChunk(String chunk) {
    if (isCancelled || isComplete) return;

    // Non-blocking offer with overflow handling
    if (!chunkQueue.offer(cleaned)) {
        // Queue full - merge last chunks or drop
        log.warning("Chunk queue full, applying backpressure");
        // Option 1: Drop oldest
        chunkQueue.poll();
        chunkQueue.offer(cleaned);
        // Option 2: Signal streaming to slow down
    }
}
```

**Effort:** 2 hours

---

## P2 - MEDIUM (Fix in Next Sprint)

### 9. Missing Exception Handler in Guardrails
**File:** `AIService.java:259-280`
**Impact:** MEDIUM | **Likelihood:** LOW

**Problem:** Only specific exceptions caught. Other exceptions crash chat.

**Fix:** Add catch-all with user-friendly message.

**Effort:** 30 minutes

---

### 10. Null Language Dereference
**File:** `AIService.java:720-745`
**Impact:** MEDIUM | **Likelihood:** LOW

**Problem:** Unknown language code causes NPE.

**Fix:** Add null check before using `requestedLangObj`.

**Effort:** 30 minutes

---

### 11. Throughput Degradation in Batch Render
**File:** `AIChatStreamingMessage.java:398-431`
**Impact:** MEDIUM | **Likelihood:** MEDIUM

**Problem:** Chunks accumulate faster than they render during heavy load.

**Fix:** Implement adaptive batch sizing based on render time.

**Effort:** 2 hours

---

## Implementation Order

```
Week 1: P0 Blockers
├── Day 1: #1 Cross-tenant (30 min) + #2 NPE (1 hr)
├── Day 2: #3 Thread race (2 hr)
└── Day 3: #4 Desktop reference (1 hr)

Week 2: P1 High
├── Day 1: #5 Cancellation race (1 hr)
├── Day 2: #6 Memory leak (2 hr)
├── Day 3: #7 O(n²) rendering (3 hr)
└── Day 4: #8 Unbounded queue (2 hr)

Week 3: P2 Medium
├── #9 Exception handler (30 min)
├── #10 Language null (30 min)
└── #11 Throughput (2 hr)
```

---

## Verification Checklist

### P0 Blockers
- [ ] #1: Query chats with wrong client ID → returns empty
- [ ] #2: Simulate null response → shows error, doesn't hang
- [ ] #3: Rapid message send → all in correct thread
- [ ] #4: Close window during stream → no exception in logs

### P1 High
- [ ] #5: Click cancel during stream → no stale content
- [ ] #6: 1000 chat sessions → heap stable
- [ ] #7: 100KB response → <1 sec render time
- [ ] #8: Fast streaming → queue stays bounded

### P2 Medium
- [ ] #9: Guardrail exception → user-friendly error
- [ ] #10: "respond in Klingon" → no crash
- [ ] #11: Heavy load → latency stable

---

## Related ADRs

- [ADR-007](adr/007-database-security-model.md) - Database Security (relates to #1)
- [ADR-014](adr/014-guardrails-and-safety.md) - Guardrails (relates to #9)
- [ADR-047](adr/047-streaming-chat-rendering-best-practices.md) - Streaming (relates to #5-8)
- [ADR-048](adr/048-comprehensive-security-strategy.md) - Security Strategy (relates to #1)
