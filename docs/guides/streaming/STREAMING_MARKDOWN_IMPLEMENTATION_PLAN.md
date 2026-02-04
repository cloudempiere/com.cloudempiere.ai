# Streaming Markdown Implementation Plan

## Overview
Implement progressive server-side markdown-to-HTML rendering during streaming to match ChatGPT's UX behavior. Markdown elements render immediately as closing markers arrive, eliminating the plain-text-only streaming experience.

## Objective
Replace plain text streaming (line 719 in `AIChatStreamingMessage.java`) with incremental HTML rendering that shows formatted markdown in real-time.

## Architecture

### State Machine Design
```
States:
- NORMAL: Plain text rendering
- IN_BOLD: Inside ** markers
- IN_ITALIC: Inside * or _ markers
- IN_INLINE_CODE: Inside ` markers
- IN_CODE_BLOCK: Inside ``` markers
- IN_HEADING: After # markers at line start
```

### Component Structure
```
StreamingMarkdownRenderer
├── State tracking (currentState, markerBuffer, contentBuffer)
├── Character-by-character parsing
├── Immediate HTML emission
└── Integration with StreamingTableRenderer for tables
```

## Implementation Phases

### Phase 1: Core Parser (Priority: High)
**File:** `com.cloudempiere.ai.util.StreamingMarkdownRenderer.java`

**Responsibilities:**
- Character-by-character markdown parsing
- State machine for tracking context (bold, italic, code, etc.)
- Immediate HTML tag emission when opening markers detected
- HTML output accumulation

**Public API:**
```java
public class StreamingMarkdownRenderer {
    public void appendChunk(String chunk);
    public String renderCurrentState();  // Returns HTML with cursor
    public String renderFinal();          // Returns complete HTML
    public void reset();
}
```

**State Tracking:**
```java
private enum State { NORMAL, IN_BOLD, IN_ITALIC, IN_INLINE_CODE, IN_CODE_BLOCK, IN_HEADING }
private State currentState = State.NORMAL;
private StringBuilder markerBuffer = new StringBuilder();  // Detect multi-char markers
private StringBuilder contentBuffer = new StringBuilder();  // Current segment content
private StringBuilder htmlOutput = new StringBuilder();     // Accumulated HTML
private String codeBlockLanguage = null;  // For syntax highlighting hint
```

**Character Processing Logic:**
```java
public void appendChunk(String chunk) {
    for (char ch : chunk.toCharArray()) {
        switch (currentState) {
            case NORMAL:
                if (ch == '*' || ch == '_' || ch == '`') {
                    markerBuffer.append(ch);
                    // Check if we have complete marker (**,*,```,`)
                    checkAndTransitionState();
                } else if (ch == '#' && isLineStart) {
                    currentState = State.IN_HEADING;
                    headingBuffer.append(ch);
                } else {
                    htmlOutput.append(escapeHtml(ch));
                }
                break;

            case IN_BOLD:
                if (ch == '*') {
                    markerBuffer.append(ch);
                    if (markerBuffer.length() == 2) {
                        // Close bold: emit </strong>
                        htmlOutput.append(escapeHtml(contentBuffer.toString()));
                        htmlOutput.append("</strong>");
                        currentState = State.NORMAL;
                        markerBuffer.setLength(0);
                        contentBuffer.setLength(0);
                    }
                } else {
                    contentBuffer.append(ch);
                }
                break;

            case IN_CODE_BLOCK:
                // Similar logic for code blocks
                break;

            // ... other states
        }
    }
}

private void checkAndTransitionState() {
    if (markerBuffer.toString().equals("**")) {
        // Open bold: emit <strong>
        htmlOutput.append("<strong>");
        currentState = State.IN_BOLD;
        markerBuffer.setLength(0);
    } else if (markerBuffer.toString().equals("```")) {
        // Open code block: emit <pre><code>
        htmlOutput.append("<pre><code>");
        currentState = State.IN_CODE_BLOCK;
        markerBuffer.setLength(0);
    }
    // ... other transitions
}
```

**Supported Markdown Elements (Phase 1):**
- `**bold**` → `<strong>bold</strong>`
- `*italic*` or `_italic_` → `<em>italic</em>`
- `` `code` `` → `<code>code</code>`
- ````language\ncode\n```` → `<pre><code class="language">code</code></pre>`
- `# Heading` → `<h1>Heading</h1>` (h1-h6)

**Deferred to Existing Components:**
- Tables: Use `StreamingTableRenderer` (already implemented)
- Links: Defer to final rendering (complex)
- Lists: Phase 2 (requires line-level state)

### Phase 2: Integration (Priority: High)
**File:** `com.cloudempiere.ai.component.AIChatStreamingMessage.java`

**Changes Required:**

**1. Add StreamingMarkdownRenderer instance:**
```java
/** Streaming markdown renderer for progressive formatting */
private StreamingMarkdownRenderer markdownRenderer;
```

**2. Initialize in constructor (line ~195):**
```java
this.markdownRenderer = new StreamingMarkdownRenderer();
```

**3. Modify `onBatchRender()` method (line 428):**
```java
public void onBatchRender() {
    // Collect batch of chunks
    List<String> batch;
    synchronized (queueLock) {
        if (chunkQueue.isEmpty()) {
            renderScheduled = false;
            return;
        }
        batch = new ArrayList<>(chunkQueue);
        chunkQueue.clear();
    }

    // Process all chunks in batch
    for (String chunk : batch) {
        content.append(chunk);

        // Feed to table renderer (existing)
        tableRenderer.appendChunk(chunk);

        // NEW: Feed to markdown renderer
        markdownRenderer.appendChunk(chunk);
    }

    // Update DOM once for entire batch
    updateContentDisplay();

    // Restart timer if more chunks arrived
    synchronized (queueLock) {
        if (!chunkQueue.isEmpty() && !renderTimer.isRunning()) {
            renderTimer.start();
        } else {
            renderScheduled = false;
        }
    }
}
```

**4. Replace `updateContentDisplay()` method (line 701):**
```java
private void updateContentDisplay() {
    String html;

    // Priority 1: Table streaming (if table detected)
    if (tableRenderer != null && tableRenderer.hasContent()) {
        html = tableRenderer.renderCurrentState();
    }
    // Priority 2: Markdown streaming (if no table, use progressive markdown)
    else {
        html = markdownRenderer.renderCurrentState();

        // Add streaming cursor if not complete
        if (!isComplete) {
            html += "<span class='streaming-cursor'>|</span>";
        }
    }

    streamingContent.setContent("<div class='ai-markdown-content'>" + html + "</div>");
}
```

**5. Update `complete()` method (line 542):**
```java
public void complete() {
    if (isComplete) {
        return;
    }

    // Flush any remaining chunks
    synchronized (queueLock) {
        if (!chunkQueue.isEmpty()) {
            for (String chunk : chunkQueue) {
                content.append(chunk);
                try {
                    tableRenderer.appendChunk(chunk);
                    markdownRenderer.appendChunk(chunk);  // NEW
                } catch (Exception e) {
                    log.warn("Error appending chunk during completion: " + e.getMessage());
                }
            }
            chunkQueue.clear();
        }
        renderScheduled = false;
    }

    isComplete = true;
    renderFinalMarkdown();  // Existing final render
    enableCopyButton();
}
```

### Phase 3: Advanced Markdown (Priority: Medium)
**Extend `StreamingMarkdownRenderer` to support:**

**Lists (Ordered & Unordered):**
```markdown
- Item 1
- Item 2
```
Requires line-level state tracking:
```java
private boolean inList = false;
private String listType = null;  // "ul" or "ol"
private StringBuilder listBuffer = new StringBuilder();
```

**Blockquotes:**
```markdown
> Quote text
```

**Horizontal Rules:**
```markdown
---
```

**Nested Formatting:**
```markdown
**bold with *italic* inside**
```
Requires state stack instead of single state variable:
```java
private Stack<State> stateStack = new Stack<>();
```

### Phase 4: Edge Case Handling (Priority: Medium)

**Split Markers Across Chunks:**
```
Chunk 1: "text **bo"
Chunk 2: "ld** more"
```
**Solution:** Marker buffer holds partial markers until complete or invalidated.

**Escaped Characters:**
```markdown
\**not bold\**
```
**Solution:** Check for backslash before markers, skip state transition.

**Incomplete Code Blocks:**
```
Chunk 1: "```python\ndef"
Chunk 2: " func():\n```"
```
**Solution:** Stay in `IN_CODE_BLOCK` state until closing ``` detected.

**Mixed Content:**
```markdown
Text before table

| Col1 | Col2 |
|------|------|
| A    | B    |

Text after table **bold**
```
**Solution:** Coordinate between `StreamingTableRenderer` and `StreamingMarkdownRenderer`:
- Table renderer detects table start (`|` at line start)
- Switch to table mode, disable markdown parser
- When table ends, re-enable markdown parser for post-table content

### Phase 5: Testing (Priority: High)

**Unit Tests:** `com.cloudempiere.ai.test/src/com/cloudempiere/ai/util/StreamingMarkdownRendererTest.java`

**Test Cases:**
```java
@Test
public void testBoldRendering() {
    StreamingMarkdownRenderer renderer = new StreamingMarkdownRenderer();
    renderer.appendChunk("This is **bo");
    String html1 = renderer.renderCurrentState();
    // Should show: "This is **bo|" (plain text, marker incomplete)

    renderer.appendChunk("ld** text");
    String html2 = renderer.renderCurrentState();
    // Should show: "This is <strong>bold</strong> text|"
}

@Test
public void testSplitMarkers() {
    StreamingMarkdownRenderer renderer = new StreamingMarkdownRenderer();
    renderer.appendChunk("Text *");
    renderer.appendChunk("*bold*");
    renderer.appendChunk("* more");
    String html = renderer.renderCurrentState();
    // Should show: "Text <strong>bold</strong> more|"
}

@Test
public void testCodeBlock() {
    StreamingMarkdownRenderer renderer = new StreamingMarkdownRenderer();
    renderer.appendChunk("```python\n");
    String html1 = renderer.renderCurrentState();
    // Should show: "<pre><code class='language-python'>|"

    renderer.appendChunk("def func():\n");
    renderer.appendChunk("    pass\n```");
    String html2 = renderer.renderCurrentState();
    // Should show: "<pre><code class='language-python'>def func():\n    pass</code></pre>|"
}

@Test
public void testNestedFormatting() {
    StreamingMarkdownRenderer renderer = new StreamingMarkdownRenderer();
    renderer.appendChunk("**bold with *italic* inside**");
    String html = renderer.renderCurrentState();
    // Should show: "<strong>bold with <em>italic</em> inside</strong>|"
}

@Test
public void testMixedContent() {
    StreamingMarkdownRenderer renderer = new StreamingMarkdownRenderer();
    renderer.appendChunk("Before table\n\n| A | B |\n|---|---|\n| 1 | 2 |\n\n**After**");
    String html = renderer.renderCurrentState();
    // Should coordinate with table renderer
}
```

**Integration Tests:** `com.cloudempiere.ai.test/src/com/cloudempiere/ai/component/AIChatStreamingMessageTest.java`

**Manual Testing Scenarios:**
1. Ask AI for formatted response with bold, italic, code
2. Observe progressive rendering during streaming
3. Verify cursor appears at correct position
4. Verify final rendering matches streamed rendering
5. Test cancellation mid-stream (partial formatting should be valid HTML)

## Technical Specifications

### Performance Requirements
- **Latency:** < 5ms per chunk processing (same as current plain text)
- **Throughput:** Handle 100+ chunks/sec without blocking
- **Memory:** < 50KB per streaming message (state + buffers)

### HTML Safety
- All user content must be HTML-escaped before emission
- Only emit known-safe HTML tags (`<strong>`, `<em>`, `<code>`, `<pre>`, `<h1-6>`)
- Never emit user-provided HTML (XSS prevention)

### Browser Compatibility
- Progressive HTML rendering relies on browser's native partial HTML parsing
- All modern browsers (Chrome, Firefox, Safari, Edge) support this
- Graceful degradation: If streaming fails, final render still works

## Dependencies

### New Files to Create
1. `com.cloudempiere.ai.util.StreamingMarkdownRenderer.java` - Core parser
2. `com.cloudempiere.ai.test/src/com/cloudempiere/ai/util/StreamingMarkdownRendererTest.java` - Unit tests

### Files to Modify
1. `com.cloudempiere.ai.component.AIChatStreamingMessage.java`
   - Add `markdownRenderer` field
   - Modify `onBatchRender()`
   - Modify `updateContentDisplay()`
   - Modify `complete()`

### No External Dependencies Required
- Pure Java implementation using existing utilities
- Reuse `Util.maskHTML()` for HTML escaping
- Reuse existing streaming infrastructure

## Migration Plan

### Phase 1 Deployment (Week 1-2)
- Implement `StreamingMarkdownRenderer` core (bold, italic, inline code, code blocks, headings)
- Unit tests
- Integration with `AIChatStreamingMessage`
- Internal testing

### Phase 2 Deployment (Week 3)
- Add list support (ordered, unordered)
- Add blockquote support
- Edge case handling (split markers, escaping)
- Comprehensive integration tests

### Phase 3 Deployment (Week 4)
- Performance tuning
- Manual QA
- Production deployment
- Monitor for issues

### Rollback Plan
If streaming markdown causes issues:
1. Add feature flag: `ai.chat.streaming.markdown=true/false`
2. Default to `false` to revert to plain text streaming
3. Fix issues and re-enable

## Success Criteria

✅ **User Experience:**
- Bold text appears immediately when closing `**` arrives during streaming
- Code blocks render with proper formatting during streaming
- No visual glitches or HTML escaping errors
- Matches ChatGPT's progressive rendering feel

✅ **Technical:**
- All unit tests pass (>95% coverage for `StreamingMarkdownRenderer`)
- Performance: < 5ms per chunk processing
- No memory leaks (streaming messages properly garbage collected)
- Browser console shows no errors

✅ **Quality:**
- No XSS vulnerabilities (all content properly escaped)
- Works across browsers (Chrome, Firefox, Safari, Edge)
- Graceful handling of split markers and incomplete syntax

## Timeline

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| Phase 1: Core Parser | 3 days | `StreamingMarkdownRenderer` with bold, italic, code |
| Phase 2: Integration | 2 days | Updated `AIChatStreamingMessage` |
| Phase 3: Advanced Markdown | 3 days | Lists, blockquotes, nested formatting |
| Phase 4: Edge Cases | 2 days | Split marker handling, escaping |
| Phase 5: Testing | 3 days | Comprehensive test suite |
| **Total** | **13 days** | **Production-ready streaming markdown** |

## Future Enhancements (Post-MVP)

1. **Syntax Highlighting During Streaming:**
   - Apply Prism.js highlighting as code content streams
   - Requires streaming-aware highlighting that works on partial code

2. **Link Rendering:**
   - `[text](url)` → `<a href="url">text</a>`
   - Requires buffering link syntax until complete

3. **Image Rendering:**
   - `![alt](url)` → `<img src="url" alt="alt">`
   - Could show placeholder during streaming

4. **Advanced Tables:**
   - Coordinate better with `StreamingTableRenderer`
   - Support nested markdown in table cells during streaming

5. **Math Rendering (LaTeX):**
   - `$equation$` and `$$block$$`
   - Integration with KaTeX or MathJax for progressive rendering

## References

- **ChatGPT Analysis:** Server-side HTML streaming with `data-start`/`data-end` tracking
- **ADR-047:** Streaming Chat Rendering Best Practices
- **CLD-1653:** Fix for plain text streaming (basis for this enhancement)
- **Current Implementation:** `StreamingTableRenderer.java` (reference architecture)
