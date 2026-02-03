# LangChain4j Integration: Anomalies, Anti-Patterns & Boundary Issues

**Version:** 1.0
**Date:** 2026-02-03
**LangChain4j Version:** 0.35.0
**Java Version:** 11 (Amazon Corretto)

---

## Executive Summary

This document identifies 10 anomalies, anti-patterns, and boundary issues in the LangChain4j integration that cause rendering bugs, potential memory leaks, and architectural violations.

**Critical Issues:** 3
**Medium Issues:** 5
**Low Issues:** 2

---

## Critical Issues

### 1. Tool Response Boundary Violation 🔴

**Severity:** Critical
**Location:** `AIService.java:1127-1135`

**Problem:** LangChain4j streams ALL content through `TokenStream.onNext()` without distinguishing between:
- Pure AI-generated text (markdown)
- Tool execution results (structured JSON)
- Tool call requests (should be hidden from user)

**Code:**
```java
tokenStream
    .onNext(token -> {
        responseAccumulator.get().append(token);
        callback.onChunk(token); // ❌ Sends EVERYTHING to UI
    })
```

**Impact:**
- Tool responses (JSON) go through markdown renderer
- Double HTML escaping when tool results contain formatted content
- Mixed markdown/HTML in output
- No separation of concerns between data and presentation

**Root Cause:** LangChain4j 0.35.0 doesn't expose tool execution metadata in streaming mode

**Workaround:** `MarkdownSyntaxSanitizer` now detects and preserves HTML to minimize damage

**Fix Required:** Upgrade to LangChain4j 1.x (blocked by Java 17 requirement, ADR-035)

---

### 2. No Tool Result Formatting Control 🔴

**Severity:** Critical
**Location:** `ERPTools.java:145-148`

**Problem:** ERPTools returns raw JSON strings from database queries, but has no control over how LangChain4j presents these to the AI or streams them to the user.

**Code:**
```java
if (result.isSuccess()) {
    String resultStr = result.getRows().toString(); // ← Raw JSON
    return resultStr; // ❌ LangChain4j embeds this in response stream
}
```

**Impact:**
- Agent receives JSON, may format it as markdown/HTML before returning
- User sees both the tool result AND the agent's reformatted version
- Double rendering: JSON → Agent formatting → Markdown rendering → HTML escaping
- Potential XSS if tool returns user-generated content

**Missing Boundary:** No mechanism to mark tool results as "pre-formatted" or "raw data"

**LangChain4j Limitation:** Version 0.35.0 doesn't support:
- Custom tool result renderers
- Tool result visibility control (hide from stream)
- Structured data types (everything is String)

**Workaround:** `MarkdownSyntaxSanitizer.isLikelyHtml()` detects and preserves tool results

---

### 3. Exception Fallback Causes Double Escaping 🔴

**Severity:** Critical
**Location:** `MarkdownSyntaxSanitizer.java:211` (FIXED)

**Problem:** When markdown sanitization failed, it fell back to escaping EVERYTHING as HTML entities, breaking any pre-rendered content from tools.

**Scenario:**
1. LangChain4j streams tool result with HTML: `<table><tr><td>Data</td></tr></table>`
2. Sanitizer throws exception (malformed markdown)
3. Fallback escapes: `&lt;table&gt;&lt;tr&gt;&lt;td&gt;Data&lt;/td&gt;&lt;/tr&gt;&lt;/table&gt;`
4. Browser displays literal HTML tags instead of rendering

**Root Cause:** Sanitizer assumed all content is markdown, but tool results may already be HTML

**Fix:** Added `isLikelyHtml()` detection in exception handler. HTML passes through, only markdown gets escaped.

**Status:** ✅ FIXED

---

## Medium Issues

### 4. Tool Execution Transparency Leak 🟡

**Severity:** Medium
**Location:** `AIService.java:1085-1090`

**Problem:** Tool executions are visible in the response stream by default. Users see tool call syntax in markdown format.

**Expected:** Tool calls should be hidden from main response stream and only shown in timeline panel.

**LangChain4j Gap:** No `toolVisibility()` or `toolResultFormatter()` builder options in 0.35.0

---

### 5. Memory Management Without Eviction Strategy 🟡

**Severity:** Medium
**Location:** `AIService.java:179-191`

**Problem:** Memory caches use simple LRU eviction but don't clean up LangChain4j's internal message lists.

**Code:**
```java
protected boolean removeEldestEntry(...) {
    boolean shouldRemove = size() > MAX_CACHE_SIZE;
    if (shouldRemove) {
        log.fine("Evicting eldest memory from cache");
        // ❌ No cleanup of LangChain4j internal state
    }
    return shouldRemove;
}
```

**Memory Leak Risk:** Long-running servers with many users could accumulate orphaned message stores.

**Fix Required:**
```java
if (shouldRemove) {
    MessageWindowChatMemory evicted = eldest.getValue();
    evicted.clear(); // Clear LangChain4j internal state
}
```

---

### 6. Agent Caching Without Lifecycle 🟡

**Severity:** Medium
**Location:** `AIService.java:164-176`

**Problem:** `ERPAgent` instances are cached but never explicitly closed or cleaned up.

**LangChain4j Design Issue:** `AiServices.builder()` creates dynamic proxies but provides no lifecycle management methods.

**Potential Resource Leak:** If agents hold HTTP connections or thread pools (via underlying chat models), these won't be released.

---

### 7. Thread Safety - Shared Agent Instances 🟡

**Severity:** Medium
**Location:** `AIService.java:164`

**Problem:** Cached `ERPAgent` instances may be used concurrently by multiple requests.

**Question:** Is `StreamingChatLanguageModel` thread-safe? LangChain4j documentation doesn't specify.

**Risk:** If model instances maintain mutable state (connection pools, request counters), concurrent access could cause race conditions.

**Mitigation:** Agent caching is by provider ID, memory is by session ID. As long as `chatMemoryProvider` creates separate instances per session, this should be safe.

**Status:** ⚠️ NEEDS VERIFICATION

---

### 8. No Tool Timeout or Cancellation 🟡

**Severity:** Medium
**Location:** `ERPTools.java:98`

**Problem:** Database queries have no timeout mechanism. Long-running queries block the streaming response.

**Impact:**
- User sees "thinking" indicator forever
- No way to cancel a rogue query
- Thread blocked until query completes or times out (if database enforces)

**LangChain4j Gap:** No `@Tool` timeout annotation or cancellation API in 0.35.0

**Workaround Needed:** Wrap query execution in `CompletableFuture.orTimeout()` or use `ExecutorService` with timeout

---

## Low Issues

### 9. System Prompt Language Instruction Duplication 🟢

**Severity:** Low
**Location:** `AIService.java:1082, 1107` + `ERPAgent.java:39-88`

**Problem:** System prompt is built dynamically with language instruction, but base prompt is duplicated in two places.

**Better Design:** Store system prompt template in database (AIG_Provider.SystemPrompt) with language placeholders.

---

### 10. RAG Tools Conditional Registration 🟢

**Severity:** Low
**Location:** `AIService.java:1069-1079`

**Problem:** RAG tools are conditionally added to agent based on service availability, but system prompt doesn't reflect this.

**Impact:** Agent may hallucinate RAG tool calls if system prompt mentions them but they're not registered.

**Fix:** System prompt should dynamically list available tools.

---

## Summary Table

| # | Issue | Severity | Location | Impact | Status |
|---|-------|----------|----------|--------|--------|
| 1 | Tool response boundary violation | 🔴 Critical | AIService.java:1127 | Double rendering, mixed content | Workaround applied |
| 2 | No tool result formatting control | 🔴 Critical | ERPTools.java:148 | JSON in markdown, XSS risk | Workaround applied |
| 3 | Exception fallback escapes all HTML | 🔴 Critical | MarkdownSyntaxSanitizer.java:211 | Broken tool results | ✅ FIXED |
| 4 | Tool execution transparency leak | 🟡 Medium | AIService.java:1085 | UX clutter | Open |
| 5 | Memory management without eviction | 🟡 Medium | AIService.java:179 | Memory leak potential | Open |
| 6 | Agent caching without lifecycle | 🟡 Medium | AIService.java:164 | Resource leak potential | Open |
| 7 | Thread safety - shared agents | 🟡 Medium | AIService.java:164 | Race conditions | Needs verification |
| 8 | No tool timeout or cancellation | 🟡 Medium | ERPTools.java:98 | Hung requests | Open |
| 9 | System prompt duplication | 🟢 Low | AIService.java:1082 | Maintenance overhead | Open |
| 10 | RAG tools conditional registration | 🟢 Low | AIService.java:1069 | Hallucinated tool calls | Open |

---

## Recommended Actions

### Immediate (Completed) ✅
1. ✅ Fix Issue #3: Smart fallback in MarkdownSyntaxSanitizer
2. ✅ Document Issue #1: Add code comments explaining limitation
3. ✅ Document Issue #2: Add workaround notes

### Short-term (Phase 2)
4. ⬜ Fix Issue #5: Add cleanup hook in memoryCache eviction
5. ⬜ Fix Issue #8: Add query timeout wrapper in ERPTools
6. ⬜ Fix Issue #10: Make system prompt tool list dynamic
7. ⬜ Verify Issue #7: Test thread safety of StreamingChatLanguageModel

### Long-term (Phase 3 - requires LangChain4j upgrade)
8. ⬜ Upgrade to LangChain4j 1.x (requires Java 17 migration per ADR-035)
9. ⬜ Implement Issue #1 fix: Use extended streaming API with tool execution hooks
10. ⬜ Implement Issue #2 fix: Use structured tool result types (not String)
11. ⬜ Implement Issue #4 fix: Hide tool calls from main stream, show in timeline

---

## Version Compatibility

| LangChain4j Version | Java Requirement | Status |
|---------------------|------------------|--------|
| 0.35.0 (current) | Java 8+ | ✅ Compatible |
| 0.36.0+ | Java 17+ | ❌ Blocked by ADR-035 |
| 1.0.0+ (stable) | Java 17+ | ❌ Blocked by ADR-035 |

**Blocked Features until Java 17 migration:**
- MCP (Model Context Protocol) support (ADR-003)
- Extended thinking/reasoning timeline (ADR-033)
- System/tool message caching (cost optimization)
- Enhanced observability listeners
- Google Gemini streaming (ADR-034)

---

## Related Documentation

- **ADR-002:** LangChain4j Strategic Adoption
- **ADR-035:** Java Version Strategy and Migration Path
- **ADR-033:** Streaming Responses and Thinking Timeline UX
- **RENDERING_FIXES_SUMMARY.md:** Fixes applied to mitigate issues
- **ARCHITECTURAL_ISSUES_MATRIX.md:** Complete system architecture

---

## Author

Claude Code (Sonnet 4.5)
Analysis Date: 2026-02-03
