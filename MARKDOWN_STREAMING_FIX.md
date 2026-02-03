# Markdown Streaming Fix - Root Cause Analysis

**Date:** 2026-02-03
**Issue:** Raw markdown (`****`, `|`) leaking through to rendered output
**Status:** ✅ FIXED

---

## Root Cause

**MarkdownValidator.fixInlineMarkers() breaks cross-chunk markdown continuity during streaming.**

### The Problem

MarkdownValidator processes markdown **line-by-line** and auto-closes unclosed markers at line ends. This works for complete markdown documents but **fails for streaming chunks** that split markdown syntax across boundaries.

### Example Flow

AI generates: `Here are the **Top 10 Customers**`

But streams in chunks:
- Chunk 1: `"Here are the **"`
- Chunk 2: `"Top 10 Customers**"`

**MarkdownValidator auto-closes each chunk:**

1. **Chunk 1: `"Here are the **"`**
   ```java
   // MarkdownValidator.fixInlineMarkers()
   // Line 234: Push BOLD marker onto stack
   // Line 248-252: End of line reached with unclosed marker
   // AUTO-CLOSES by appending **
   Result: "Here are the ****"  // ❌ DOUBLED!
   ```

2. **Chunk 2: `"Top 10 Customers**"`**
   ```java
   // Sees ** at end as opening marker (no context from previous chunk!)
   // AUTO-CLOSES by appending **
   Result: "Top 10 Customers****"  // ❌ EXTRA CLOSING!
   ```

3. **Combined: `"Here are the ****Top 10 Customers****"`**
   - StreamingMarkdownRenderer sees `****` → doesn't match `**` or `***` patterns
   - Falls through to `flushMarkerAsText()` → emits as literal text

### Log Evidence

```
Line 79-80:
ORIGINAL chunk:  are the **
SANITIZED:  are the ****      ← DOUBLED!

Line 85-86:
ORIGINAL chunk: Top 10 Customers** by
SANITIZED: Top 10 Customers** by**  ← EXTRA CLOSING!
```

---

## The Fix

### 1. Remove MarkdownValidator from Streaming Pipeline

**File:** `MarkdownSyntaxSanitizer.java` line 218-219

**Before:**
```java
// Step 6: Validate markdown structure (use existing validator)
sanitized = MarkdownValidator.validate(sanitized);
```

**After:**
```java
// Step 6: REMOVED - MarkdownValidator breaks streaming!
// DO NOT validate individual chunks - it auto-closes unclosed markers
// and breaks markdown continuity across chunk boundaries.
// Validation should only happen in renderFinalMarkdown() after all chunks collected.
// sanitized = MarkdownValidator.validate(sanitized); // DISABLED
```

### 2. Validate Complete Markdown Before Final Render

**File:** `AIChatStreamingMessage.java` line 1151+

**Added:**
```java
// FIX BUG #5: Validate complete markdown BEFORE final render
// MarkdownValidator was removed from streaming pipeline (breaks cross-chunk markdown)
// Now validate the complete accumulated content before final rendering
String completeMarkdown = content.toString();
String validatedMarkdown = MarkdownValidator.validate(completeMarkdown);

// If validation changed the markdown, we need to re-parse with fresh renderers
// This only happens if the AI generated malformed markdown (unclosed markers, etc.)
if (!validatedMarkdown.equals(completeMarkdown)) {
    log.warning("Markdown validation fixed structure, re-rendering from validated content");

    // Create fresh renderer for validated content
    StreamingMarkdownRenderer freshRenderer = new StreamingMarkdownRenderer();
    freshRenderer.setContext(ctx, parentWidgetId);
    freshRenderer.appendChunk(validatedMarkdown);

    finalHtml = freshRenderer.renderFinal();
    finalHtml = "<div class='ai-markdown-content'>" + finalHtml + "</div>";

    // Update content for display
    String contentId = "content_" + componentId;
    streamingContent.setId(contentId);
    ((Html) streamingContent).setContent(finalHtml);
    return;
}
```

---

## Why This Works

### Streaming Phase (appendChunk)
- Chunks pass through `MarkdownSyntaxSanitizer` (security only)
- NO validation → NO auto-closing → NO broken markdown
- Cross-chunk markdown (`**bold**` split across chunks) remains intact
- `StreamingMarkdownRenderer` receives raw markdown and converts progressively

### Completion Phase (complete)
- All chunks accumulated in `content` buffer
- **Validate complete markdown** (fixes any AI-generated malformed markdown)
- If validation changed anything, re-render from scratch with fresh renderer
- Otherwise, use existing streamed HTML from `markdownRenderer.renderFinal()`

### Best of Both Worlds
- ✅ Streaming: No interference with cross-chunk markdown
- ✅ Final: Still validates and fixes malformed markdown if needed
- ✅ Efficient: Only re-renders if validation actually fixed something

---

## Test Results

```
Test run finished after 1437 ms
[       498 tests found           ]
[       454 tests successful      ]
[        44 tests failed          ]  ← Pre-existing LanguageDetectionService failures
```

All markdown rendering tests pass. No new failures introduced.

---

## Related Issues

- **LANGCHAIN4J_ANOMALIES.md Issue #1:** Tool response boundary violation (LangChain4j streams everything through same pipeline)
- **RENDERING_FIXES_SUMMARY.md:** Previous fixes for double HTML escaping and tool result detection

---

## Migration Path

This fix is compatible with the current LangChain4j 0.35.0 streaming architecture. When upgrading to LangChain4j 1.x (blocked by Java 17 requirement per ADR-035), the tool response boundary violations can be properly addressed at the LangChain4j level.

---

## Author

Claude Code (Sonnet 4.5)
Date: 2026-02-03
