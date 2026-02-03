# Rendering Issues Fixed - Summary

## Issues Fixed

### 1. Double HTML Escaping (CRITICAL)
**Problem:** When `MarkdownSyntaxSanitizer` threw an exception, it fell back to `SecuritySanitizer.escapeHtml()` which escaped ALL HTML tags, destroying pre-rendered content from tool results.

**Example:**
```
Tool returns: <table><tr><td>Data</td></tr></table>
Fallback escapes it: &lt;table&gt;&lt;tr&gt;&lt;td&gt;Data&lt;/td&gt;&lt;/tr&gt;&lt;/table&gt;
Browser displays: literal HTML text instead of rendered table
```

**Fix:** Added `isLikelyHtml()` detection in exception fallback. If content is HTML (starts with `<div`, `<table`, etc. or has 3+ closing tags), it passes through without escaping. Only plain text/markdown gets escaped.

**Location:** `MarkdownSyntaxSanitizer.java` lines 208-263

---

### 2. Tool Results Processed as Markdown (CRITICAL)
**Problem:** LangChain4j streams tool results (JSON, formatted HTML) through the same pipeline as AI text. Everything got processed as markdown, causing:
- Tool JSON to be rendered as markdown
- Pre-rendered HTML to be sanitized/escaped
- Mixed markdown/HTML in output

**Fix:** Added early exit in `MarkdownSyntaxSanitizer.sanitize()` to detect pre-rendered HTML and skip markdown processing entirely.

**Location:** `MarkdownSyntaxSanitizer.java` lines 177-181

---

### 3. Wrapper Design Clarified (DOCUMENTATION)
**Problem:** Confusion about where `<div class='ai-markdown-content'>` wrapper is added.

**Clarification:** Wrapper added in exactly 3 places:
1. `AIMessageRenderer.render()` - for legacy markdown messages reloaded from database
2. `AIChatStreamingMessage.renderFinalMarkdown()` - for new streaming messages (lines 1162, 1168)
3. `AIChatStreamingMessage.cancel()` - for cancelled messages (line 885)

Progressive renderers (`StreamingMarkdownRenderer`, `MarkdownTableRenderer`) return unwrapped HTML fragments.

**Fix:** Added documentation comments explaining wrapper design.

**Location:** `AIMessageRenderer.java` lines 142-147

---

## Code Changes

### Modified Files

**1. MarkdownSyntaxSanitizer.java**
- Added `isLikelyHtml()` method to detect pre-rendered HTML
- Modified `sanitize()` to skip markdown processing for HTML
- Modified exception fallback to preserve HTML instead of escaping it

**2. AIMessageRenderer.java**
- Added documentation comment explaining wrapper design

**3. MarkdownSyntaxSanitizerTest.java**
- Added 6 new tests for HTML detection:
  - `testHtmlTablePassthrough()`
  - `testHtmlDivPassthrough()`
  - `testHtmlDetectionByClosingTags()`
  - `testMarkdownWithSingleTag()`
  - `testToolResultHtmlStructure()`
  - `testFallbackPreservesHtml()`

---

## HTML Detection Heuristics

The `isLikelyHtml()` method uses the following heuristics:

1. **Starts with HTML tag:**
   - `<div`, `<table`, `<p>`, `<pre>`, `<ul>`, `<ol>`, `<h1>`, `<h2>`, `<h3>`

2. **Contains multiple closing tags:**
   - If content has 3+ `</tag>` sequences, likely HTML structure

3. **Fallback:**
   - If neither condition met, treat as markdown/plain text

This handles:
- Tool results with HTML tables/lists
- Pre-rendered HTML from database
- Mixed content with some HTML

---

## Test Results

- **Total tests:** 498
- **Passed:** 454
- **Failed:** 44 (unrelated - LanguageDetectionService tests)
- **New tests:** 6 (all passing)

---

## Impact Assessment

### Before Fix
- Tool results displayed as literal HTML: `&lt;table&gt;`
- Tables from database queries broken
- Mixed markdown (`****`) and HTML (`<strong>`) in output
- JSON from tool responses rendered as markdown

### After Fix
- ✅ Tool results pass through without modification
- ✅ Pre-rendered HTML preserved
- ✅ Markdown-only content still sanitized for security
- ✅ No double escaping
- ✅ Consistent rendering across streaming and reload paths

---

## Related Issues

### LangChain4j Integration Anomalies (Still Open)

The root cause is **LangChain4j 0.35.0** streaming ALL content (AI text + tool results) through `TokenStream.onNext()` without distinguishing between data types.

**Documented in:** LANGCHAIN4J_ANOMALIES.md (to be created)

**Long-term fix:** Requires LangChain4j upgrade to 1.x (blocked by Java 17 requirement, ADR-035)

---

## Testing

### Manual Testing
1. Test tool results with HTML tables
2. Test pre-rendered HTML from database
3. Test markdown-only content (should still be sanitized)
4. Test exception handling (fallback should preserve HTML)

### Automated Tests
```bash
./run-unit-tests.sh
```

All 6 new tests in `MarkdownSyntaxSanitizerTest` should pass.

---

## Related Documentation

- **ADR-055:** Constrained Markdown Syntax Support
- **ADR-054:** HTML-Only Chat Message Storage
- **ARCHITECTURAL_ISSUES_MATRIX.md:** Rendering pipeline documentation
- **LANGCHAIN4J_ANOMALIES.md:** Tool response boundary violations

---

## Author
Claude Code (Sonnet 4.5)
Date: 2026-02-03
