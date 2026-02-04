# Greenfield Implementation Plan: AI Chat Rendering Layer

## Key Assumption

**No backward compatibility required** - This is a new solution, never deployed.

**Impact:**
- ❌ NO legacy renderer code paths needed
- ❌ NO migration scripts needed
- ❌ NO HTML-only entries to handle
- ✅ Clean, single-path architecture
- ✅ Dual storage (markdown + HTML) from day 1
- ✅ Strict sanitization from the start

---

## Simplified Architecture

### Single Rendering Path (No Legacy!)

```
AI Response (Markdown)
    ↓
ChunkCleaner.clean(chunk)                    ← Remove control chars
    ↓
MarkdownSyntaxSanitizer.sanitize(cleaned)    ← Remove unsafe markdown
    ↓
    ├─ STORE: entry.setContentMarkdown(sanitized)  ← For resume/export
    └─ RENDER: StreamingMarkdownRenderer.renderFinal()
          ↓
      STORE: entry.setContentHTML(html)       ← For fast display
```

**One renderer, one path, no legacy complexity!**

---

## Database Schema (Final)

### Clean Implementation (No Deprecated Fields)

```sql
CREATE TABLE AIG_ChatEntry (
    AIG_ChatEntry_ID NUMBER(10) NOT NULL,

    -- DUAL STORAGE: Both formats from day 1
    ContentMarkdown VARCHAR2(4000),  -- Source (for resume/export/processing)
    ContentHTML VARCHAR2(4000),      -- Rendered (for fast display)

    -- Both fields are REQUIRED (NOT NULL after initial insert)
    -- No legacy nulls to handle!

    -- Other fields...
    Created TIMESTAMP,
    CreatedBy NUMBER(10),
    -- ...
);
```

**Constraint:** Both `ContentMarkdown` AND `ContentHTML` must be populated (no nulls).

---

## Implementation Checklist

### Phase 1: Core Sanitization (2 hours)

**File:** `ChunkCleaner.java`

**Enhancement:** Add missing Unicode control chars

```java
public static String clean(String chunk) {
    if (chunk == null || chunk.isEmpty()) {
        return chunk;
    }

    // 1. Remove zero-width characters + Unicode line separators + replacement char
    String cleaned = chunk
        .replaceAll("[\u200B\u200C\u200D\u200E\u200F\uFEFF\u2028\u2029\uFFFD]", "");
        //                                                    ^^^^ ^^^^ ^^^^^ ADD THESE

    // 2. Remove control characters (except \t, \n)
    cleaned = cleaned.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

    // 3. Normalize line breaks
    cleaned = cleaned
        .replace("\r\n", "\n")
        .replace("\r", "\n");

    // 4. Limit consecutive blank lines to 2
    cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

    // 5. Remove unpaired surrogates (NEW)
    cleaned = removeSurrogatePairs(cleaned);

    return cleaned;
}

/**
 * Remove unpaired UTF-16 surrogate characters.
 * Prevents database encoding errors.
 */
private static String removeSurrogatePairs(String text) {
    StringBuilder clean = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
        char ch = text.charAt(i);
        if (Character.isHighSurrogate(ch)) {
            // Check if next char is valid low surrogate
            if (i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                clean.append(ch);  // Valid pair, keep both
                clean.append(text.charAt(++i));
            }
            // else: unpaired high surrogate, skip
        } else if (Character.isLowSurrogate(ch)) {
            // Unpaired low surrogate, skip
            continue;
        } else {
            clean.append(ch);
        }
    }
    return clean.toString();
}
```

**Test:**
```java
@Test
public void testControlCharSanitization() {
    // Control chars
    assertEquals("HelloWorld", ChunkCleaner.clean("Hello\x00World"));
    assertEquals("HelloWorld", ChunkCleaner.clean("Hello\x1BWorld"));

    // Unicode line separators
    assertEquals("HelloWorld", ChunkCleaner.clean("Hello\u2028World"));

    // Unpaired surrogates
    assertEquals("HelloWorld", ChunkCleaner.clean("Hello\uD800World"));
}
```

---

### Phase 2: Dual Storage (3 hours)

**File:** `AIChatStreamingMessage.java` or `ThreadAwareChatMemory.java`

**Implementation:**

```java
/**
 * Persist AI message with dual storage (markdown + HTML).
 *
 * @param message AI response
 */
public void persistMessage(AiMessage message) {
    String markdown = message.text();

    // STEP 1: Clean control characters
    markdown = ChunkCleaner.clean(markdown);

    // STEP 2: Sanitize unsafe markdown
    MarkdownConfig config = MarkdownConfig.fromProvider(provider);
    MarkdownSyntaxSanitizer sanitizer = new MarkdownSyntaxSanitizer(config);
    markdown = sanitizer.sanitize(markdown);

    // STEP 3: Render to HTML
    String html = AIMessageRenderer.render(markdown, provider, ctx);

    // STEP 4: Store BOTH formats
    MAIChatEntry entry = new MAIChatEntry(ctx, 0, trxName);
    entry.setContentMarkdown(markdown);  // Source (for resume/export)
    entry.setContentHTML(html);          // Rendered (for display)
    entry.saveEx();
}
```

**Validation:**

```java
@Test
public void testDualStorage() {
    String markdown = "Hello **world**";

    MAIChatEntry entry = persistMessage(markdown);

    // Both fields populated
    assertNotNull("Markdown must be stored", entry.getContentMarkdown());
    assertNotNull("HTML must be stored", entry.getContentHTML());

    // Markdown is sanitized
    assertEquals("Hello **world**", entry.getContentMarkdown());

    // HTML is rendered
    assertTrue(entry.getContentHTML().contains("<strong>world</strong>"));
}
```

---

### Phase 3: Verify Sanitization in All Paths (1 hour)

**Paths to Verify:**

1. **Streaming:** `AIChatStreamingMessage.java`
   ```java
   public void onNext(String chunk) {
       chunk = ChunkCleaner.clean(chunk);  // ← VERIFY THIS LINE EXISTS
       chunk = sanitizer.sanitize(chunk);
       renderer.appendChunk(chunk);
   }
   ```

2. **Non-Streaming:** `AIMessageRenderer.java`
   ```java
   public static String render(String markdown, ...) {
       markdown = ChunkCleaner.clean(markdown);  // ← VERIFY THIS LINE EXISTS
       markdown = sanitizer.sanitize(markdown);
       return CommonMarkRenderer.render(markdown);
   }
   ```

3. **Persistence:** `ThreadAwareChatMemory.java`
   ```java
   public void add(AiMessage message) {
       String content = message.text();
       content = ChunkCleaner.clean(content);  // ← VERIFY THIS LINE EXISTS
       // ... rest of persistence
   }
   ```

**Verification Script:**

```bash
# Check ChunkCleaner is called in all critical files
echo "=== Streaming Path ==="
grep -n "ChunkCleaner" com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/component/AIChatStreamingMessage.java

echo "=== Non-Streaming Path ==="
grep -n "ChunkCleaner" com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/util/AIMessageRenderer.java

echo "=== Persistence Path ==="
grep -n "ChunkCleaner" com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/provider/langchain4j/ThreadAwareChatMemory.java
```

**Expected Output:** Each file should show at least one `ChunkCleaner.clean()` call.

---

### Phase 4: Remove Legacy Code (2 hours)

**Since no backward compatibility needed, DELETE:**

1. **Old Renderers:**
   ```bash
   # Search for legacy renderers
   find . -name "*Renderer*.java" | grep -v StreamingMarkdownRenderer

   # Example files to potentially delete:
   # - MarkdownRenderer.java (if legacy)
   # - CommonMarkRenderer.java (if duplicate)
   # - Any other markdown renderers
   ```

2. **Migration Detection Logic:**
   ```java
   // DELETE THIS (no legacy entries to detect):
   private static boolean isNewFormat(MAIChatEntry entry) {
       // No longer needed - all entries are new format!
   }
   ```

3. **Deprecated Fields:**
   ```java
   // DELETE THIS (no deprecated markdown field):
   entry.setContentMarkdown(null);  // ← Remove this line
   ```

**Simplification Impact:**

| Metric | Before (with legacy) | After (greenfield) |
|---|---|---|
| Renderer classes | 3 | 1 |
| Code paths | 3 (new/legacy-md/legacy-html) | 1 (single path) |
| Lines of code | ~2100 LOC | ~1000 LOC |
| Test scenarios | 15 (all paths) | 5 (single path) |
| Complexity | HIGH | LOW |

---

### Phase 5: Configuration (Instance-Based) (4 hours)

**Remove Static Config (Breaks Multi-Tenancy)**

**File:** `MarkdownSyntaxSanitizer.java`

**Current (WRONG for multi-tenant):**
```java
// STATIC = shared across ALL tenants!
private static boolean allowBase64Images = false;
private static int maxTableRows = 100;
```

**Fixed (instance-based):**
```java
public class MarkdownSyntaxSanitizer {
    private final MarkdownConfig config;

    public MarkdownSyntaxSanitizer(MarkdownConfig config) {
        this.config = config;
    }

    public String sanitize(String markdown) {
        // Use this.config instead of static fields
        if (!config.isAllowBase64Images()) {
            markdown = removeBase64Images(markdown);
        }
        if (countTableRows(markdown) > config.getMaxTableRows()) {
            markdown = truncateTable(markdown);
        }
        return markdown;
    }
}
```

**New Class:** `MarkdownConfig.java`

```java
package com.cloudempiere.ai.util;

import com.cloudempiere.ai.model.MAIProvider;

/**
 * Configuration for markdown sanitization.
 * Supports provider-specific and tenant-specific settings.
 */
public class MarkdownConfig {
    private final boolean allowBase64Images;
    private final int maxTableRows;
    private final int maxTableColumns;
    private final int maxCodeBlockLines;

    public MarkdownConfig(boolean allowBase64Images, int maxTableRows,
                         int maxTableColumns, int maxCodeBlockLines) {
        this.allowBase64Images = allowBase64Images;
        this.maxTableRows = maxTableRows;
        this.maxTableColumns = maxTableColumns;
        this.maxCodeBlockLines = maxCodeBlockLines;
    }

    /**
     * Default configuration (conservative limits).
     */
    public static MarkdownConfig createDefault() {
        return new MarkdownConfig(
            false,  // No base64 images by default
            100,    // Max 100 table rows
            20,     // Max 20 table columns
            50      // Max 50 code block lines
        );
    }

    /**
     * Load configuration from provider settings.
     * Future: Read from AIG_Provider table or JSON config field.
     */
    public static MarkdownConfig fromProvider(MAIProvider provider) {
        // TODO: Read from provider config when implemented
        // For now, use defaults
        return createDefault();
    }

    // Getters
    public boolean isAllowBase64Images() { return allowBase64Images; }
    public int getMaxTableRows() { return maxTableRows; }
    public int getMaxTableColumns() { return maxTableColumns; }
    public int getMaxCodeBlockLines() { return maxCodeBlockLines; }
}
```

**Usage:**

```java
// Create sanitizer with provider-specific config
MAIProvider provider = MAIProvider.get(ctx, providerId);
MarkdownConfig config = MarkdownConfig.fromProvider(provider);
MarkdownSyntaxSanitizer sanitizer = new MarkdownSyntaxSanitizer(config);

String sanitized = sanitizer.sanitize(markdown);
```

---

### Phase 6: Thread Safety (30 minutes)

**File:** `AIChatStreamingMessage.java`

**Fix:** Make `renderedHtml` volatile

```java
// Before:
private String renderedHtml = null;

// After:
private volatile String renderedHtml = null;  // ← ADD volatile
```

**Reason:** `renderedHtml` is written by streaming thread, read by UI thread. Without `volatile`, visibility not guaranteed.

---

### Phase 7: Remove Redundant Escaping (2 hours)

**File:** `StreamingMarkdownRenderer.java`

**Current:** Renderer does HTML escaping (defensive but redundant)

```java
private String escapeHtml(char ch) {
    switch (ch) {
        case '<': return "&lt;";
        case '>': return "&gt;";
        // ...
    }
}
```

**Decision:** Keep or remove?

| Option | Pros | Cons |
|---|---|---|
| **Keep escaping** (defensive) | Extra safety layer | Redundant (ChunkCleaner already ran) |
| **Remove escaping** (trust upstream) | Simpler, faster | Relies on ChunkCleaner |

**Recommendation:** **KEEP IT** (defense in depth)

Even though ChunkCleaner should remove control chars, having escaping in the renderer provides extra safety if sanitization is bypassed somehow.

**Update JavaDoc instead:**

```java
/**
 * Escape HTML special characters.
 *
 * NOTE: This is a DEFENSIVE layer. Input should already be sanitized
 * by ChunkCleaner and MarkdownSyntaxSanitizer. This provides extra
 * protection in case sanitization is bypassed.
 *
 * @param ch character to escape
 * @return escaped HTML string
 */
private String escapeHtml(char ch) {
    // ... existing implementation
}
```

---

## Final Architecture (Clean)

### Single Rendering Flow

```
┌─────────────────────────────────────────────────────────────┐
│  1. AI Response (Markdown)                                   │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  2. ChunkCleaner.clean()                                     │
│     - Remove control chars (\x00-\x1F, \x7F)               │
│     - Remove zero-width (\u200B-\u200F, \uFEFF)            │
│     - Remove Unicode line sep (\u2028-\u2029)              │
│     - Remove replacement char (\uFFFD)                      │
│     - Remove unpaired surrogates                            │
│     - Normalize CRLF → LF                                   │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  3. MarkdownSyntaxSanitizer.sanitize()                      │
│     - Remove HTML tags                                       │
│     - Validate URLs (block javascript:, data:)              │
│     - Remove unsupported features                            │
│     - Protect code blocks & zoom links                       │
└─────────────────────────────────────────────────────────────┘
                          ↓
                    ┌─────────┴─────────┐
                    ↓                   ↓
┌──────────────────────────┐  ┌──────────────────────────┐
│  4a. STORE Markdown      │  │  4b. Render to HTML      │
│      (for processing)    │  │      StreamingMarkdown   │
│                          │  │      Renderer            │
│  entry.setContent        │  │                          │
│    Markdown(sanitized)   │  │  renderFinal()           │
└──────────────────────────┘  └──────────────────────────┘
                                          ↓
                          ┌───────────────────────────────┐
                          │  5. STORE HTML                 │
                          │     (for fast display)         │
                          │                                │
                          │  entry.setContentHTML(html)    │
                          └───────────────────────────────┘
                                          ↓
                          ┌───────────────────────────────┐
                          │  6. Display in ZK              │
                          │     Html component             │
                          └───────────────────────────────┘
```

**Key Simplifications (vs. legacy architecture):**
- ✅ Single path (no if/else for legacy formats)
- ✅ Single renderer (no multiple renderer classes)
- ✅ Dual storage from day 1 (no migration needed)
- ✅ Instance-based config (no static fields)
- ✅ Thread-safe (volatile fields)

---

## Implementation Timeline

### Day 1 (4 hours)
- ✅ Enhance ChunkCleaner (Unicode chars, surrogates)
- ✅ Create MarkdownConfig class
- ✅ Refactor MarkdownSyntaxSanitizer to use instance config

### Day 2 (4 hours)
- ✅ Implement dual storage in persistence layer
- ✅ Verify sanitization in all 3 paths (streaming, non-streaming, persistence)
- ✅ Make `renderedHtml` volatile

### Day 3 (3 hours)
- ✅ Delete legacy renderer code (if any)
- ✅ Write comprehensive tests
- ✅ Update documentation

**Total:** 11 hours for complete greenfield implementation

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testEndToEndSanitization() {
    // Input: Markdown with control chars, unsafe HTML, bad URLs
    String unsafeMarkdown =
        "Hello\x00World\n" +
        "<script>alert('XSS')</script>\n" +
        "[Click](javascript:alert('XSS'))\n" +
        "**bold**";

    // Process through full pipeline
    String cleaned = ChunkCleaner.clean(unsafeMarkdown);
    MarkdownConfig config = MarkdownConfig.createDefault();
    MarkdownSyntaxSanitizer sanitizer = new MarkdownSyntaxSanitizer(config);
    String sanitized = sanitizer.sanitize(cleaned);

    // Verify sanitization
    assertFalse("Null byte removed", sanitized.contains("\x00"));
    assertFalse("Script tag removed", sanitized.contains("<script>"));
    assertFalse("JavaScript URL removed", sanitized.contains("javascript:"));
    assertTrue("Valid markdown preserved", sanitized.contains("**bold**"));
}

@Test
public void testDualStorageComplete() {
    String markdown = "# Heading\n\nHello **world**";

    MAIChatEntry entry = persistMessage(markdown);

    // Dual storage populated
    String storedMarkdown = entry.getContentMarkdown();
    String storedHtml = entry.getContentHTML();

    assertNotNull(storedMarkdown);
    assertNotNull(storedHtml);

    // Markdown is source
    assertEquals("# Heading\n\nHello **world**", storedMarkdown);

    // HTML is rendered
    assertTrue(storedHtml.contains("<h1>Heading</h1>"));
    assertTrue(storedHtml.contains("<strong>world</strong>"));
}

@Test
public void testProviderSpecificConfig() {
    MAIProvider provider1 = createProvider(1);
    MAIProvider provider2 = createProvider(2);

    // Different configs per provider (future enhancement)
    MarkdownConfig config1 = MarkdownConfig.fromProvider(provider1);
    MarkdownConfig config2 = MarkdownConfig.fromProvider(provider2);

    // For now, both use defaults (same config)
    assertEquals(config1.getMaxTableRows(), config2.getMaxTableRows());

    // Future: Load from provider settings
    // assertTrue(config1.isAllowBase64Images());
    // assertFalse(config2.isAllowBase64Images());
}
```

---

## Decision Points

### 1. Invalid Char Replacement

**Question:** Replace with `<br/>` or remove?

| Option | Example | Use Case |
|---|---|---|
| **Remove** (recommended) | `"HelloWorld"` | Cleaner output, production |
| **Replace with `<br/>`** | `"Hello<br/>World"` | Makes issues visible, debugging |

**Recommendation:** **REMOVE** (current ChunkCleaner behavior)
- Cleaner UX
- Control chars have no semantic value
- Production-ready

**Optional:** Add debug mode flag
```java
public static String clean(String chunk, boolean debugMode) {
    if (debugMode) {
        // Replace critical chars with <br/> for visibility
        chunk = chunk.replaceAll("[\x00\x1B\x7F]", "<br/>");
    } else {
        // Remove all control chars (production)
        chunk = chunk.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }
    // ... rest of cleaning
}
```

---

### 2. Zoom Link Approach

**Question:** JavaScript (Approach A) or Permalink (Approach B)?

| Approach | Status | Recommendation |
|---|---|---|
| **A: JavaScript** | ✅ Implemented | ✅ USE THIS (supports filtered results) |
| **B: Permalink** | ❌ Not implemented | 🟢 OPTIONAL (14 hours if needed) |

**Decision:** **KEEP APPROACH A** (JavaScript zoom)
- Already working
- Supports complex queries
- No URL length limits
- Permalink can be added later if external sharing needed

---

### 3. Renderer Escaping

**Question:** Keep or remove HTML escaping in `StreamingMarkdownRenderer`?

**Recommendation:** **KEEP IT** (defense in depth)
- Extra safety layer
- Minimal performance cost
- Protects against sanitization bypass

---

## Files to Modify

### ✅ Existing Files (Enhancements)

| File | Changes | Lines Changed |
|---|---|---|
| `ChunkCleaner.java` | Add Unicode chars, surrogates | +30 lines |
| `MarkdownSyntaxSanitizer.java` | Instance-based config | ~50 lines |
| `AIChatStreamingMessage.java` | Add `volatile`, verify ChunkCleaner | 2 lines |
| `AIMessageRenderer.java` | Verify ChunkCleaner called | 1 line |
| `ThreadAwareChatMemory.java` | Dual storage, verify ChunkCleaner | +5 lines |
| `StreamingMarkdownRenderer.java` | Update JavaDoc | 10 lines |

### ➕ New Files

| File | Purpose | Lines |
|---|---|---|
| `MarkdownConfig.java` | Instance-based config | ~60 lines |

### ❌ Files to Delete (If Legacy)

- Any old renderer classes (check with `find . -name "*Renderer*.java"`)
- Migration detection logic (no longer needed)

---

## Success Criteria

### ✅ Done When:

1. **Single Rendering Path**
   - [ ] Only one renderer class in production code
   - [ ] No if/else for legacy format detection
   - [ ] ~1000 LOC total (down from ~2100)

2. **Dual Storage Working**
   - [ ] All new entries have both markdown + HTML
   - [ ] No null checks needed (both fields always populated)
   - [ ] Tests verify both formats stored

3. **Complete Sanitization**
   - [ ] ChunkCleaner called in all 3 paths (streaming, non-streaming, persistence)
   - [ ] All control chars removed (including Unicode)
   - [ ] Unpaired surrogates handled
   - [ ] Tests cover all edge cases

4. **Multi-Tenant Safe**
   - [ ] No static config fields
   - [ ] Instance-based MarkdownSyntaxSanitizer
   - [ ] Per-provider configuration ready

5. **Thread Safe**
   - [ ] `renderedHtml` is volatile
   - [ ] No race conditions in tests

---

## Summary

**No backward compatibility = Massive simplification!**

| Metric | With Legacy | Greenfield |
|---|---|---|
| **Renderer classes** | 3 | 1 |
| **Code paths** | 3 | 1 |
| **LOC** | ~2100 | ~1000 |
| **Migration scripts** | 2-3 scripts | 0 |
| **Test scenarios** | 15 | 5 |
| **Implementation time** | 24 hours | 11 hours |
| **Complexity** | HIGH | LOW |

**Total Effort:** 11 hours (3 days)

**Deliverables:**
- ✅ Single, clean rendering path
- ✅ Dual storage (markdown + HTML)
- ✅ Complete sanitization (control chars, Unicode, surrogates)
- ✅ Multi-tenant safe (instance-based config)
- ✅ Thread-safe (volatile fields)
- ✅ Production-ready tests
