# Rendering Method Refactoring Plan

**Issue:** AIChatStreamingMessage has duplicated markdown transformation code (52 lines duplicated between `renderPartialMarkdown` and `processSimpleMarkdown`)

**Created:** 2025-12-18
**Status:** Proposed

## Current Architecture (Redundant)

```
Streaming Display:
  updateContentDisplay()
    └─> renderPartialMarkdown(text)
         ├─> Remove function calls (829-835)
         ├─> Render tables (841-852)
         ├─> Escape HTML conditionally (858-862)
         └─> Transform markdown (866-898) ← DUPLICATED CODE

Final Display:
  renderFinalMarkdown()
    └─> processMarkdownPreservingHTML(text)
         └─> processSimpleMarkdown(text)
              ├─> Escape HTML (1009)
              └─> Transform markdown (1010-1023) ← DUPLICATED CODE
```

## Proposed Unified Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ MarkdownRenderer (New Utility Class)                        │
├─────────────────────────────────────────────────────────────┤
│ + renderMarkdown(text, options) : String                    │
│   ├─> applyMarkdownTransformations(text) : String           │
│   │    ├─> Headings: ### → <h4>, ## → <h3>, # → <h2>       │
│   │    ├─> Bold: **text** → <strong>                        │
│   │    ├─> Italic: *text* → <em>                            │
│   │    ├─> Code: `text` → <code>                            │
│   │    ├─> Lists: - item → <li>                             │
│   │    └─> Line breaks: \n → <br/>                          │
│   ├─> cleanupBlockElements(text, elements) : String         │
│   │    └─> Remove <br/> after specified block elements      │
│   └─> Options:                                              │
│        - removeFunctionCalls: boolean                        │
│        - renderTables: boolean                               │
│        - escapeHtml: boolean                                 │
│        - blockElementsToCleanup: String[]                    │
└─────────────────────────────────────────────────────────────┘

AIChatStreamingMessage:
  - renderPartialMarkdown() → calls MarkdownRenderer with streaming options
  - processSimpleMarkdown() → calls MarkdownRenderer with simple options
  - renderFinalMarkdown() → orchestrates rendering with MarkdownRenderer
```

## Implementation Steps

### Step 1: Create MarkdownRenderer Utility Class

**File:** `src/com/cloudempiere/ai/util/MarkdownRenderer.java`

```java
package com.cloudempiere.ai.util;

import org.compiere.util.Util;

/**
 * Unified markdown to HTML renderer for AI chat streaming.
 *
 * <p>Consolidates markdown transformation logic previously duplicated
 * across multiple methods in AIChatStreamingMessage.
 *
 * <p>Supports:
 * <ul>
 *   <li>Headings (h1-h4)</li>
 *   <li>Bold and italic text</li>
 *   <li>Inline code</li>
 *   <li>Lists</li>
 *   <li>Configurable HTML escaping</li>
 *   <li>Block element cleanup</li>
 * </ul>
 *
 * @since v0.31.0
 */
public class MarkdownRenderer {

    /**
     * Rendering options for markdown transformation.
     */
    public static class RenderOptions {
        /** Remove function call XML blocks before rendering */
        public boolean removeFunctionCalls = false;

        /** Render tables using MarkdownTableRenderer */
        public boolean renderTables = false;

        /** Escape HTML before markdown transformation */
        public boolean escapeHtml = true;

        /** Block elements to remove trailing <br/> tags from */
        public String[] blockElementsToCleanup = new String[] {"h2", "h3", "h4", "li"};

        /** Table renderer context (for table rendering) */
        public MarkdownTableRenderer tableRenderer = null;
    }

    /**
     * Render markdown text to HTML with specified options.
     *
     * @param text raw markdown text
     * @param options rendering options
     * @return HTML output
     */
    public static String render(String text, RenderOptions options) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;

        // Step 1: Remove function calls (optional)
        if (options.removeFunctionCalls) {
            result = removeFunctionCalls(result);
        }

        // Step 2: Render tables (optional, before HTML escaping)
        if (options.renderTables && options.tableRenderer != null) {
            result = options.tableRenderer.renderTables(result);
        }

        // Step 3: Escape HTML (optional)
        if (options.escapeHtml) {
            result = Util.maskHTML(result, true);
        }

        // Step 4: Apply markdown transformations
        result = applyMarkdownTransformations(result);

        // Step 5: Convert line breaks
        result = result.replace("\n", "<br/>");

        // Step 6: Cleanup block elements
        result = cleanupBlockElements(result, options.blockElementsToCleanup);

        return result;
    }

    /**
     * Apply markdown transformations (headings, bold, italic, code, lists).
     */
    private static String applyMarkdownTransformations(String text) {
        String result = text;

        // Headings: # text, ## text, ### text (must be at start of line)
        result = result.replaceAll("(?m)^### (.+)$",
            "<h4 style='font-size: 14px; font-weight: 600; margin: 12px 0 8px 0;'>$1</h4>");
        result = result.replaceAll("(?m)^## (.+)$",
            "<h3 style='font-size: 15px; font-weight: 600; margin: 14px 0 8px 0;'>$1</h3>");
        result = result.replaceAll("(?m)^# (.+)$",
            "<h2 style='font-size: 16px; font-weight: 600; margin: 16px 0 10px 0;'>$1</h2>");

        // Bold: **text** or __text__
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        result = result.replaceAll("__(.+?)__", "<strong>$1</strong>");

        // Italic: *text* or _text_
        result = result.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "<em>$1</em>");
        result = result.replaceAll("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)", "<em>$1</em>");

        // Code: `text`
        result = result.replaceAll("`([^`]+)`",
            "<code style='background: #f5f5f5; padding: 2px 6px; border-radius: 3px; " +
            "font-family: monospace; font-size: 0.9em;'>$1</code>");

        // Lists: - item or * item (basic support)
        result = result.replaceAll("(?m)^- (.+)$",
            "<li style='margin-left: 16px; list-style-type: disc;'>$1</li>");
        result = result.replaceAll("(?m)^\\* (.+)$",
            "<li style='margin-left: 16px; list-style-type: disc;'>$1</li>");

        return result;
    }

    /**
     * Remove function call XML blocks.
     */
    private static String removeFunctionCalls(String text) {
        String result = text;
        result = result.replaceAll("(?s)<function_calls>.*?</function_calls>", "");
        result = result.replaceAll("(?s)<function_result>.*?</function_result>", "");
        result = result.replaceAll("(?s)<function_calls>.*$", "");
        result = result.replaceAll("(?s)<function_result>.*$", "");
        result = result.replaceAll("(?s)<invoke[^>]*>.*?</invoke>", "");
        result = result.replaceAll("(?s)<parameter[^>]*>.*?</parameter>", "");
        return result;
    }

    /**
     * Clean up extra <br/> after block elements.
     */
    private static String cleanupBlockElements(String text, String[] elements) {
        String result = text;
        for (String element : elements) {
            result = result.replaceAll("</" + element + "><br/>", "</" + element + ">");
        }
        // Also clean up table-related elements if present
        result = result.replaceAll("</table><br/>", "</table>");
        result = result.replaceAll("</tr><br/>", "</tr>");
        result = result.replaceAll("</th><br/>", "</th>");
        result = result.replaceAll("</td><br/>", "</td>");
        return result;
    }
}
```

### Step 2: Refactor AIChatStreamingMessage

Replace duplicated code with calls to `MarkdownRenderer`:

```java
// OLD: renderPartialMarkdown() - 80 lines
private String renderPartialMarkdown(String text) {
    // ... 80 lines of code ...
}

// NEW: renderPartialMarkdown() - 15 lines
private String renderPartialMarkdown(String text) {
    if (text == null || text.isEmpty()) {
        return "";
    }

    MarkdownRenderer.RenderOptions options = new MarkdownRenderer.RenderOptions();
    options.removeFunctionCalls = true;
    options.renderTables = true;
    options.escapeHtml = false; // Will be handled by escapeNonTableContent
    options.tableRenderer = this.tableRenderer;
    options.blockElementsToCleanup = new String[] {"h2", "h3", "h4", "li", "table", "tr", "th", "td"};

    String result = MarkdownRenderer.render(text, options);

    // Conditionally escape HTML based on table presence
    if (result.contains("<table")) {
        result = escapeNonTableContent(result);
    } else {
        result = Util.maskHTML(result, true);
    }

    return result;
}

// OLD: processSimpleMarkdown() - 18 lines
private String processSimpleMarkdown(String text) {
    // ... 18 lines of code ...
}

// NEW: processSimpleMarkdown() - 8 lines
private String processSimpleMarkdown(String text) {
    if (text == null || text.isEmpty()) return "";

    MarkdownRenderer.RenderOptions options = new MarkdownRenderer.RenderOptions();
    options.escapeHtml = true;
    options.blockElementsToCleanup = new String[] {"h2", "h3", "h4", "li"};

    return MarkdownRenderer.render(text, options);
}
```

### Step 3: Testing

1. **Unit Tests:**
   - Create `MarkdownRendererTest.java` with tests for all transformations
   - Test different option combinations
   - Verify backward compatibility

2. **Integration Tests:**
   - Test streaming display still works
   - Test final rendering still works
   - Compare output before/after refactoring (should be identical)

3. **Manual Testing:**
   - Stream a response with headings, tables, bold, italic, code, lists
   - Verify display matches previous behavior

## Benefits

1. **Code Reduction:** ~52 lines of duplicated code eliminated
2. **Maintainability:** Single source of truth for markdown transformations
3. **Testability:** Isolated utility can be thoroughly unit tested
4. **Flexibility:** Easy to add new options or transformations
5. **Reusability:** Can be used by other components if needed

## Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Regression in rendering | High | Comprehensive testing, side-by-side comparison |
| Performance impact | Low | MarkdownRenderer is stateless, no overhead |
| Breaking table rendering | Medium | Preserve existing table renderer integration |
| Option complexity | Low | Provide sensible defaults, document clearly |

## Rollback Plan

If issues arise:
1. Revert `MarkdownRenderer` class
2. Restore original `renderPartialMarkdown()` and `processSimpleMarkdown()`
3. All code is backward compatible

## Timeline

- **Step 1:** Create MarkdownRenderer utility (2 hours)
- **Step 2:** Refactor AIChatStreamingMessage (2 hours)
- **Step 3:** Testing (2 hours)
- **Total:** 6 hours

## Decision

**Recommended:** Implement this refactoring as part of v0.31.0 or v0.32.0

The code duplication is significant and this refactoring will improve long-term maintainability without affecting functionality.
