# Architectural Issues Matrix - AI Chat Rendering Layer

## Architectural Layers

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer (ZK Components)                                   │
│  - AIChatWidget.java                                        │
│  - AIChatStreamingMessage.java                              │
│  - StreamingMarkdownRenderer.java                           │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Service Layer (LangChain4j Integration)                    │
│  - ThreadAwareChatMemory.java                               │
│  - AIMessageRenderer.java                                   │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Utility Layer (Sanitization & Validation)                  │
│  - MarkdownSyntaxSanitizer.java                             │
│  - MarkdownValidator.java                                   │
│  - SecuritySanitizer.java                                   │
│  - ChunkCleaner.java                                        │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Storage Layer (Database)                                   │
│  - MAIChatEntry (HTML storage)                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Summary Table

| Layer | Critical 🔴 | Medium 🟡 | Low 🟢 | Total |
|---|---|---|---|---|
| **UI Layer (ZK)** | **1** ⚠️ | 3 | 0 | 4 |
| **Service Layer (LangChain4j)** | 2 | 0 | 1 | 3 |
| **Utility Layer (Sanitizers)** | 1 | 4 | 1 | 6 |
| **Configuration** | 1 | 0 | 1 | 2 |
| **Cross-Cutting Security** | 1 | 1 | 1 | 3 |
| **Total** | **6** | **8** | **4** | **18** |

⚠️ **NEW CRITICAL ISSUE:** UI-04 - Multiple legacy renderer paths (Principle A violation)

---

## Issues by Architectural Layer

### Layer 1: UI Components (ZK Framework)

| ID | Issue | Severity | Component | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|---|
| UI-01 | `renderedHtml` field not volatile | 🟡 MEDIUM | AIChatStreamingMessage | `AIChatStreamingMessage.java:142` | Missing visibility guarantee for cross-thread access | ```java<br>private volatile String renderedHtml = null;<br>``` | 5 min |
| UI-02 | Renderer does HTML escaping (violates trust boundary) | 🟡 MEDIUM | StreamingMarkdownRenderer | `StreamingMarkdownRenderer.java:836-890` | Renderer doesn't trust sanitizer output | Remove `escapeHtml()` method<br>Add JavaDoc: "Assumes pre-sanitized input" | 2 hours |
| UI-03 | Triple HTML escaping in rendering chain | 🟡 MEDIUM | UI → Service → Util | Sanitizer → Validator → Renderer | Each layer tries to "make safe" | Choose ONE escaping point (recommend: Sanitizer only) | 2 hours |
| UI-04 | **Multiple legacy renderer paths** | 🔴 CRITICAL | AIChatWidget + AIChatStreamingMessage | Multiple renderer classes:<br>- `StreamingMarkdownRenderer` (new)<br>- Legacy markdown renderer(s)<br>- Legacy HTML renderer(s) | ADR-054 migration incomplete - old renderers not deprecated | **Phase 1:** Document all renderer paths<br>**Phase 2:** Migrate historical chats to HTML-only<br>**Phase 3:** Deprecate legacy renderers<br>See detailed migration plan below | 20 hours |

---

### Layer 2: Service Layer (LangChain4j Integration)

| ID | Issue | Severity | Component | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|---|
| SVC-01 | Non-streaming path bypasses sanitization | 🔴 CRITICAL | AIMessageRenderer | `AIMessageRenderer.java:80` | LangChain4j non-streaming responses not sanitized | ```java<br>public static String render(String markdownText, ...) {<br>    markdownText = MarkdownSyntaxSanitizer.sanitize(markdownText);<br>    // ...<br>}<br>``` | 15 min |
| SVC-02 | Memory persistence bypasses sanitization | 🔴 CRITICAL | ThreadAwareChatMemory | `ThreadAwareChatMemory.java:320` | Chat history stored without sanitization | ```java<br>if (message instanceof AiMessage) {<br>    content = getMessageContent(message);<br>    content = MarkdownSyntaxSanitizer.sanitize(content);<br>    // ...<br>}<br>``` | 15 min |
| SVC-03 | No provider-specific sanitization rules | 🟢 LOW | LangChain4j integration | N/A - feature gap | All providers (Anthropic, Ollama, etc.) use same rules | Add provider-specific config in `AIG_Provider` table | 12 hours |

---

### Layer 3: Utility Layer (Sanitization Tools)

| ID | Issue | Severity | Component | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|---|
| UTIL-01 | Three overlapping sanitization classes | 🔴 CRITICAL | Sanitizers | `SecuritySanitizer.java`<br>`MarkdownSyntaxSanitizer.java`<br>`MarkdownValidator.java` | Unclear responsibility boundaries | **Document responsibilities:**<br>- SecuritySanitizer: Primitives (escape HTML/URL/SQL)<br>- MarkdownSyntaxSanitizer: Feature control<br>- MarkdownValidator: Structure fixing<br><br>**OR** merge into pipeline | 8 hours |
| UTIL-02 | HTML removal vs escaping conflict | 🟡 MEDIUM | MarkdownSyntaxSanitizer | `MarkdownSyntaxSanitizer.java:227`<br>vs `MarkdownValidator` calling SecuritySanitizer | `escapeRawHtml()` removes, then validator tries to escape (redundant) | **Keep removal** (current approach)<br>Document decision in JavaDoc | 2 hours |
| UTIL-03 | URL validation duplicated | 🟢 LOW | SecuritySanitizer + MarkdownSyntaxSanitizer | `SecuritySanitizer.java:336-373`<br>`MarkdownSyntaxSanitizer.java:262-383` | Protocol validation split from encoding | Consolidate in `SecuritySanitizer.UrlSanitizer` | 3 hours |
| UTIL-04 | Hard-coded sanitization pipeline | 🟢 LOW | MarkdownSyntaxSanitizer | `sanitize()` method: 158-199 | Static method chain, no extensibility | Refactor to pipeline pattern (optional) | 8 hours |
| UTIL-05 | Validator mixes concerns | 🟡 MEDIUM | MarkdownValidator | Throughout `MarkdownValidator.java` | Validation + sanitization in one class | Split: Validator fixes structure, Sanitizer removes unsafe | 6 hours |
| UTIL-06 | Static config not thread-safe | 🟡 MEDIUM | MarkdownSyntaxSanitizer | `MarkdownSyntaxSanitizer.java:126-136` | Setters not synchronized | Use `AtomicBoolean` or synchronized setters | 30 min |

---

### Layer 4: Configuration & Multi-Tenancy

| ID | Issue | Severity | Component | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|---|
| CFG-01 | Static config breaks multi-tenancy | 🔴 CRITICAL | MarkdownSyntaxSanitizer | Static fields: 126-136 | Shared config across ALL tenants/providers | **Phase 1:** Instance-based config<br>```java<br>class MarkdownSyntaxSanitizer {<br>  private final MarkdownConfig config;<br>  public MarkdownSyntaxSanitizer(MarkdownConfig config) {...}<br>}<br>```<br><br>**Phase 2:** Load from `AIG_Provider` table | 4 hours |
| CFG-02 | ChunkCleaner vs Sanitizer overlap unclear | 🟢 LOW | ChunkCleaner | N/A - design question | Not clear if chunk cleaning is separate concern | Review `ChunkCleaner` logic vs sanitizer overlap | 2 hours |

---

### Cross-Cutting Security Concerns

| ID | Issue | Severity | Affects Layers | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|---|
| SEC-01 | 15% of messages bypass sanitization | 🔴 CRITICAL | Service + Storage | `AIMessageRenderer.java:80`<br>`ThreadAwareChatMemory.java:320` | Non-streaming and persistence paths missing sanitization | Add sanitization at both entry points (see SVC-01, SVC-02) | 30 min |
| SEC-02 | XSS prevention strategy unclear | 🟡 MEDIUM | All layers | UI → Service → Util | Conflicting approaches (remove vs escape) | **Decision:** Remove HTML tags (current)<br>Document in ADR-055 | 2 hours |
| SEC-03 | No audit trail for sanitized content | 🟢 LOW | All layers | N/A - feature gap | Can't track what was removed/changed | Add sanitization audit log (optional) | 8 hours |

---

## 🚨 CRITICAL ARCHITECTURAL PRINCIPLE VIOLATION

### Principle A: Multiple Legacy Renderer Paths (UI-04)

**Status:** 🔴 **CRITICAL WEAKNESS** - Incomplete migration from ADR-054

#### Problem Statement

The system currently has **at least 3 different rendering paths**:

```
┌─────────────────────────────────────────────────────────────┐
│  CURRENT STATE (Multiple Renderers Coexist)                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Path 1: NEW - Streaming with HTML storage                  │
│  ├─ StreamingMarkdownRenderer (progressive rendering)       │
│  ├─ AIChatStreamingMessage.captureRenderedHtml()            │
│  └─ Store: contentHTML (already rendered)                   │
│                                                              │
│  Path 2: LEGACY - Markdown storage (historical chats)       │
│  ├─ Database: contentMarkdown field populated              │
│  ├─ On reload: Re-render markdown → HTML                    │
│  └─ Uses: Legacy markdown renderer (?)                      │
│                                                              │
│  Path 3: LEGACY - Static HTML (historical chats)            │
│  ├─ Database: contentHTML field populated (old format)      │
│  ├─ On reload: Return HTML directly (no rendering)          │
│  └─ Issues: May contain unsanitized/unvalidated HTML        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### Impact on Architecture

| Layer | Impact | Severity |
|---|---|---|
| **UI Layer** | Must handle 3 different rendering scenarios | 🔴 HIGH |
| **Service Layer** | Must detect which path to use (HTML vs Markdown) | 🔴 HIGH |
| **Storage Layer** | Database has both `contentHTML` and `contentMarkdown` | 🟡 MEDIUM |
| **Testing** | Must test all 3 paths + migration scenarios | 🔴 HIGH |
| **Maintenance** | Cannot deprecate old renderers (breaks historical chats) | 🔴 HIGH |

#### Code Duplication & Complexity

**Estimated LOC:**
- `StreamingMarkdownRenderer.java`: ~1000 LOC (new)
- Legacy markdown renderer(s): ~800 LOC (old)
- Legacy HTML handler: ~200 LOC (old)
- Migration detection logic: ~100 LOC
- **Total:** ~2100 LOC for rendering (should be ~1000 LOC)

**Complexity Metrics:**
- **Cyclomatic Complexity:** High (if/else for renderer selection)
- **Test Coverage Gap:** Legacy paths may have fewer tests
- **Bug Surface:** 3x rendering paths = 3x potential bugs

#### Current Detection Logic (Assumed)

```java
// Pseudocode - likely in AIChatWidget or similar
public void loadChatMessage(MAIChatEntry entry) {
    if (entry.getContentHTML() != null && !entry.getContentHTML().isEmpty()) {
        // Path 3: Static HTML (legacy or new)
        if (isNewFormat(entry)) {
            // New format: HTML is sanitized and valid
            displayHtml(entry.getContentHTML());
        } else {
            // Old format: HTML may be unsanitized!
            // What do we do here? Re-sanitize? Re-render?
            // ⚠️ UNDEFINED BEHAVIOR
        }
    } else if (entry.getContentMarkdown() != null) {
        // Path 2: Markdown storage (legacy)
        String markdown = entry.getContentMarkdown();
        // Which renderer? StreamingMarkdownRenderer or legacy?
        // ⚠️ UNDEFINED BEHAVIOR
        String html = renderMarkdown(markdown);
        displayHtml(html);
    } else {
        // Error: No content
    }
}
```

#### Security Implications

**Old HTML (Path 3) is NOT sanitized:**
- Created before ADR-055 (constrained markdown)
- May contain `<script>`, `<iframe>`, `javascript:` URLs
- May contain unlimited table rows/columns
- May contain base64 images when not allowed

**Old Markdown (Path 2) may use different renderer:**
- If using legacy renderer: Bypasses new sanitization
- If using new renderer: May break formatting (different markdown support)

#### Migration State Unknown

**Questions to answer:**

1. **How many historical chats exist?**
   ```sql
   SELECT
       COUNT(*) AS total_chats,
       COUNT(CASE WHEN ContentHTML IS NOT NULL THEN 1 END) AS html_chats,
       COUNT(CASE WHEN ContentMarkdown IS NOT NULL THEN 1 END) AS markdown_chats
   FROM AIG_ChatEntry;
   ```

2. **Which renderer is used for historical markdown?**
   - Same as streaming (`StreamingMarkdownRenderer`)?
   - Different legacy renderer?
   - **Location unknown**

3. **Is there a migration flag?**
   - How do we detect "new format HTML" vs "old format HTML"?
   - Date-based? Flag column? Version number?

4. **What happens on reload?**
   - New chats: Load HTML directly ✅
   - Old HTML chats: Re-sanitize? Display as-is? ❌
   - Old markdown chats: Re-render? Use legacy renderer? ❌

#### Recommended Solution (3-Phase Migration)

**Phase 1: IMMEDIATE - Document Current State (Week 1 - 4 hours)**

1. **Audit all renderer classes:**
   ```bash
   # Find all renderer classes
   find . -name "*Renderer*.java" -o -name "*Markdown*.java"

   # Output expected:
   # - StreamingMarkdownRenderer.java (new)
   # - ??? (legacy markdown renderer)
   # - ??? (legacy HTML handler)
   ```

2. **Document renderer selection logic:**
   - Where is it? (`AIChatWidget.java`? `AIChatStreamingMessage.java`?)
   - Create flow diagram showing all paths

3. **Measure migration state:**
   ```sql
   -- Run this query to understand scope
   SELECT
       Created,
       COUNT(*) AS chats_per_month,
       COUNT(CASE WHEN ContentHTML IS NOT NULL THEN 1 END) AS html_count,
       COUNT(CASE WHEN ContentMarkdown IS NOT NULL THEN 1 END) AS md_count
   FROM AIG_ChatEntry
   GROUP BY DATE_TRUNC('month', Created)
   ORDER BY Created DESC;
   ```

4. **Add to ARCHITECTURAL_ISSUES_MATRIX.md:**
   - Current renderer inventory
   - Selection logic location
   - Migration statistics

**Phase 2: SHORT-TERM - Safe Coexistence (Week 2-3 - 16 hours)**

**Goal:** Make all paths safe, even if not optimal

```java
// NEW: Unified rendering entry point
public class ChatMessageRenderer {

    /**
     * Renders chat message HTML for display.
     * Handles both new (HTML-only) and legacy (markdown/old HTML) formats.
     *
     * @param entry Chat entry from database
     * @return Safe, sanitized HTML for display
     */
    public static String renderForDisplay(MAIChatEntry entry, MAIProvider provider) {

        // Path 1: New format (HTML-only, ADR-054)
        if (isNewFormat(entry)) {
            // HTML is already sanitized and rendered
            return entry.getContentHTML();
        }

        // Path 2: Legacy markdown
        if (entry.getContentMarkdown() != null) {
            String markdown = entry.getContentMarkdown();

            // RE-SANITIZE (may not have been sanitized originally)
            MarkdownConfig config = MarkdownConfig.fromProvider(provider);
            MarkdownSyntaxSanitizer sanitizer = new MarkdownSyntaxSanitizer(config);
            markdown = sanitizer.sanitize(markdown);

            // Render using NEW renderer (consistency)
            return AIMessageRenderer.render(markdown, provider, entry.getCtx());
        }

        // Path 3: Legacy HTML (DANGEROUS - may be unsanitized)
        if (entry.getContentHTML() != null) {
            // Option A: RE-SANITIZE HTML (safest, may break formatting)
            return HtmlSanitizer.sanitize(entry.getContentHTML());

            // Option B: Display with warning (transparent, less safe)
            // return wrapWithLegacyWarning(entry.getContentHTML());
        }

        return "<p>Error: No content available</p>";
    }

    /**
     * Detects new format HTML (post ADR-054).
     *
     * Heuristics:
     * - Created after ADR-054 implementation date
     * - Has ContentHTML but NOT ContentMarkdown (exclusive)
     * - HTML contains marker comment: <!-- ADR-054 -->
     */
    private static boolean isNewFormat(MAIChatEntry entry) {
        // Option 1: Date-based
        Timestamp adr054Date = Timestamp.valueOf("2025-01-15 00:00:00");
        if (entry.getCreated().after(adr054Date)) {
            return entry.getContentHTML() != null && entry.getContentMarkdown() == null;
        }

        // Option 2: Marker-based
        if (entry.getContentHTML() != null) {
            return entry.getContentHTML().contains("<!-- ADR-054 -->");
        }

        return false;
    }
}

// NEW: HTML sanitizer for legacy content
public class HtmlSanitizer {

    /**
     * Sanitizes legacy HTML that may contain unsafe content.
     *
     * WARNING: This is a best-effort sanitizer for historical data.
     * May alter formatting.
     */
    public static String sanitize(String html) {
        // Use OWASP Java HTML Sanitizer or similar
        // Remove: <script>, <iframe>, javascript: URLs, data: URLs
        // Keep: <p>, <strong>, <em>, <ul>, <ol>, <li>, <a>, <code>, <pre>

        PolicyFactory policy = new HtmlPolicyBuilder()
            .allowElements("p", "br", "strong", "em", "ul", "ol", "li",
                          "a", "code", "pre", "blockquote", "h1", "h2", "h3")
            .allowUrlProtocols("http", "https")
            .allowAttributes("href").onElements("a")
            .requireRelNofollowOnLinks()
            .toFactory();

        return policy.sanitize(html);
    }
}
```

**Add HTML marker to new messages:**
```java
// In AIChatStreamingMessage.captureRenderedHtml()
private String captureRenderedHtml() {
    String html = renderer.getRenderedHtml();

    // Add marker for future detection
    html = "<!-- ADR-054 -->\n" + html;

    return html;
}
```

**Phase 3: LONG-TERM - Complete Migration (Sprint 2 - 40 hours)**

**Goal:** Migrate all historical chats to new format, deprecate legacy renderers

**Step 1: Batch Migration Process**

```java
/**
 * Migrates legacy chat entries to new HTML-only format (ADR-054).
 *
 * Run as background process during low-traffic hours.
 */
public class ChatMigrationProcess extends SvrProcess {

    @Override
    protected String doIt() throws Exception {

        // Find all legacy entries
        String sql = "SELECT AIG_ChatEntry_ID FROM AIG_ChatEntry " +
                    "WHERE ContentMarkdown IS NOT NULL " +
                    "OR (ContentHTML IS NOT NULL AND ContentHTML NOT LIKE '%ADR-054%')";

        List<MAIChatEntry> legacyEntries = new Query(getCtx(), MAIChatEntry.Table_Name, sql, get_TrxName())
            .list();

        int migrated = 0;
        int errors = 0;

        for (MAIChatEntry entry : legacyEntries) {
            try {
                migrateEntry(entry);
                migrated++;

                if (migrated % 100 == 0) {
                    addLog("Migrated " + migrated + " entries...");
                    commitEx();  // Commit in batches
                }

            } catch (Exception e) {
                log.warning("Failed to migrate entry " + entry.get_ID() + ": " + e.getMessage());
                errors++;
            }
        }

        return "Migrated " + migrated + " entries (" + errors + " errors)";
    }

    private void migrateEntry(MAIChatEntry entry) throws Exception {

        MAIProvider provider = MAIProvider.get(getCtx(), entry.getAIG_Provider_ID());

        // Render to new format
        String html = ChatMessageRenderer.renderForDisplay(entry, provider);

        // Update entry
        entry.setContentHTML(html);
        entry.setContentMarkdown(null);  // Clear legacy markdown
        entry.saveEx();
    }
}
```

**Step 2: Deprecate Legacy Renderers**

After migration completes:
1. Remove legacy renderer classes
2. Remove markdown rendering path from `ChatMessageRenderer`
3. Simplify to single path:
   ```java
   public static String renderForDisplay(MAIChatEntry entry, MAIProvider provider) {
       return entry.getContentHTML();  // Simple!
   }
   ```

**Step 3: Database Cleanup**

```sql
-- After confirming migration success, can drop ContentMarkdown column
-- (Keep for 3-6 months as backup)

-- Phase 1: Mark as deprecated
COMMENT ON COLUMN AIG_ChatEntry.ContentMarkdown IS
    'DEPRECATED: Migrated to ContentHTML per ADR-054. Will be dropped in v1.1.0';

-- Phase 2: Drop column (future release)
-- ALTER TABLE AIG_ChatEntry DROP COLUMN ContentMarkdown;
```

#### Testing Strategy for Multi-Renderer Paths

```java
@Test
public void testNewFormatHtml() {
    // New format: HTML-only, sanitized
    MAIChatEntry entry = createEntry();
    entry.setContentHTML("<!-- ADR-054 -->\n<p>Hello <strong>world</strong></p>");
    entry.setContentMarkdown(null);
    entry.setCreated(new Timestamp(System.currentTimeMillis()));

    String html = ChatMessageRenderer.renderForDisplay(entry, provider);

    assertEquals("<!-- ADR-054 -->\n<p>Hello <strong>world</strong></p>", html);
    // Should return HTML directly, no re-rendering
}

@Test
public void testLegacyMarkdown() {
    // Legacy format: Markdown storage
    MAIChatEntry entry = createEntry();
    entry.setContentMarkdown("Hello **world**");
    entry.setContentHTML(null);
    entry.setCreated(Timestamp.valueOf("2024-12-01 00:00:00"));  // Before ADR-054

    String html = ChatMessageRenderer.renderForDisplay(entry, provider);

    assertTrue(html.contains("<strong>world</strong>"));
    // Should re-render using new renderer
}

@Test
public void testLegacyHtmlWithXss() {
    // Legacy format: Old HTML (potentially unsafe)
    MAIChatEntry entry = createEntry();
    entry.setContentHTML("<p>Hello <script>alert('XSS')</script> world</p>");
    entry.setContentMarkdown(null);
    entry.setCreated(Timestamp.valueOf("2024-12-01 00:00:00"));  // Before ADR-054

    String html = ChatMessageRenderer.renderForDisplay(entry, provider);

    assertFalse(html.contains("<script>"));  // Should be sanitized
    assertTrue(html.contains("Hello"));      // Safe content preserved
}

@Test
public void testMigrationProcess() {
    // Create legacy entries
    MAIChatEntry legacyMd = createLegacyMarkdownEntry();
    MAIChatEntry legacyHtml = createLegacyHtmlEntry();

    // Run migration
    ChatMigrationProcess process = new ChatMigrationProcess();
    process.doIt();

    // Verify migration
    legacyMd.load(legacyMd.get_TrxName());
    assertTrue(legacyMd.getContentHTML().contains("<!-- ADR-054 -->"));
    assertNull(legacyMd.getContentMarkdown());

    legacyHtml.load(legacyHtml.get_TrxName());
    assertTrue(legacyHtml.getContentHTML().contains("<!-- ADR-054 -->"));
}
```

#### Complexity Metrics: Before vs After

| Metric | Current (3 paths) | After Phase 2 (safe coexistence) | After Phase 3 (migrated) |
|---|---|---|---|
| Renderer classes | 3 | 3 (but centralized) | 1 |
| Lines of code | ~2100 LOC | ~2300 LOC (+200 for safety) | ~1000 LOC (-1100 cleanup) |
| Test cases needed | ~15 | ~25 (+10 for migration) | ~8 (-17 after cleanup) |
| Cyclomatic complexity | High (if/else) | Medium (centralized) | Low (single path) |
| Security risk | 🔴 HIGH | 🟡 MEDIUM | 🟢 LOW |
| Maintenance burden | 🔴 HIGH | 🟡 MEDIUM | 🟢 LOW |

---

| ID | Issue | Severity | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|
| SEC-01 | Non-streaming paths bypass sanitization (15% gap) | 🔴 CRITICAL | `AIMessageRenderer.java:80`<br>`ThreadAwareChatMemory.java:320` | Missing sanitization calls in non-streaming rendering paths | Add `MarkdownSyntaxSanitizer.sanitize()` before rendering:<br>`markdownText = MarkdownSyntaxSanitizer.sanitize(markdownText);` | 15 min |
| SEC-02 | HTML removal makes escaping redundant | 🟡 MEDIUM | `MarkdownSyntaxSanitizer.java:227`<br>`MarkdownValidator.java` (calls SecuritySanitizer) | Conflicting strategies: removal happens before escaping | Choose ONE strategy:<br>**Option A:** Remove HTML (current)<br>**Option B:** Escape HTML (safer for audit trail)<br>Document decision in code | 2 hours |
| SEC-03 | URL validation duplicated in two places | 🟡 MEDIUM | `SecuritySanitizer.java:336-373`<br>`MarkdownSyntaxSanitizer.java:262-383` | Protocol validation split between two classes | Move ALL URL security to `SecuritySanitizer`:<br>- `isUrlSafe(String url)` (protocol validation)<br>- `sanitizeUrl(String url)` (validation + encoding)<br><br>MarkdownSyntaxSanitizer only extracts URLs | 3 hours |

---

### 2. Architecture & Design Issues

| ID | Issue | Severity | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|
| ARCH-01 | Three overlapping sanitization layers | 🔴 CRITICAL | `SecuritySanitizer.java`<br>`MarkdownSyntaxSanitizer.java`<br>`MarkdownValidator.java` | Unclear responsibility boundaries | **Option 1 (Quick):** Document clear responsibilities:<br>- SecuritySanitizer: HTML/URL/SQL escaping ONLY<br>- MarkdownSyntaxSanitizer: Feature removal ONLY<br>- MarkdownValidator: Structure fixing ONLY<br><br>**Option 2 (Best):** Merge into pipeline pattern | 8 hours |
| ARCH-02 | Renderer does HTML escaping (separation of concerns violation) | 🟡 MEDIUM | `StreamingMarkdownRenderer.java:836-890` | Renderer doesn't trust sanitizer output | Remove `escapeHtml()` from renderer<br>Add JavaDoc: "Assumes input is pre-sanitized"<br>Update tests to sanitize before rendering | 2 hours |
| ARCH-03 | Hard-coded sanitization pipeline | 🟢 LOW | `MarkdownSyntaxSanitizer.sanitize()`:158-199 | Static method chain, no extensibility | Refactor to pipeline pattern:<br>```java<br>MarkdownPipeline.create()<br>  .addStep(new BlockProtector())<br>  .addStep(new HtmlRemover())<br>  .addStep(new UrlValidator())<br>  .process(markdown)<br>``` | 8 hours |
| ARCH-04 | Unclear class responsibilities | 🟡 MEDIUM | `MarkdownValidator.java` | Validator mixes validation with sanitization | Split responsibilities:<br>- Validator: ONLY fix markdown structure<br>- Sanitizer: ONLY remove unsafe content<br>Remove cross-calls between them | 6 hours |

---

### 3. Thread Safety Issues

| ID | Issue | Severity | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|
| TS-01 | `renderedHtml` field not volatile | 🟡 MEDIUM | `AIChatStreamingMessage.java:142` | Missing visibility guarantee across threads | ```java<br>private volatile String renderedHtml = null;<br>``` | 5 min |
| TS-02 | Static config fields not thread-safe | 🟡 MEDIUM | `MarkdownSyntaxSanitizer.java:126-136` | Setters not synchronized | **Option 1:** Use `AtomicBoolean`<br>```java<br>private static final AtomicBoolean allowBase64Images = new AtomicBoolean(false);<br>```<br><br>**Option 2:** Synchronize setters<br>```java<br>public static synchronized void setAllowBase64Images(boolean allow)<br>``` | 30 min |

---

### 4. Configuration & Multi-Tenancy Issues

| ID | Issue | Severity | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|
| CFG-01 | Static configuration breaks multi-tenancy | 🔴 CRITICAL | `MarkdownSyntaxSanitizer.java:126-136` | Static fields shared across ALL tenants/providers | **Phase 1 (Quick):** Instance-based config<br>```java<br>public class MarkdownSyntaxSanitizer {<br>  private final MarkdownConfig config;<br>  <br>  public MarkdownSyntaxSanitizer(MarkdownConfig config) {<br>    this.config = config;<br>  }<br>}<br>```<br><br>**Phase 2 (Best):** Load from `AIG_Provider` table<br>```java<br>MarkdownConfig.fromProvider(MAIProvider provider)<br>``` | 4 hours |
| CFG-02 | No provider-specific sanitization rules | 🟢 LOW | N/A - feature gap | All providers use same sanitization rules | Add `AIG_ProviderConfig` table:<br>- `AllowBase64Images`<br>- `MaxTableRows`<br>- `MaxTableColumns`<br>- `MaxCodeBlockLines`<br>- `SupportedMarkdownFeatures` (JSON) | 12 hours |

---

### 5. Code Quality & Duplication Issues

| ID | Issue | Severity | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|
| DUP-01 | Triple HTML escaping in rendering chain | 🟡 MEDIUM | `MarkdownSyntaxSanitizer.java:227`<br>`MarkdownValidator.java`<br>`StreamingMarkdownRenderer.java:836` | Each layer tries to "make safe" | **Single escaping point:**<br>1. Remove from `StreamingMarkdownRenderer`<br>2. Choose ONE: `MarkdownSyntaxSanitizer` OR `SecuritySanitizer`<br>3. Document in JavaDoc which layer is responsible | 2 hours |
| DUP-02 | URL validation logic duplicated | 🟢 LOW | `SecuritySanitizer.escapeUrl()`<br>`MarkdownSyntaxSanitizer.sanitizeUrls()` | Protocol validation split from encoding | Consolidate in `SecuritySanitizer`:<br>```java<br>public static class UrlValidator {<br>  public boolean isProtocolSafe(String url);<br>  public String sanitize(String url);<br>}<br>``` | 3 hours |
| DUP-03 | Chunk cleaning duplicated | 🟢 LOW | `ChunkCleaner.clean()`<br>Logic potentially overlaps with sanitizer | Not clear if chunk cleaning is separate concern | Review `ChunkCleaner` logic:<br>- If overlaps: merge into sanitizer<br>- If distinct: document difference in JavaDoc | 2 hours |

---

### 6. Performance & Optimization Issues

| ID | Issue | Severity | Location | Root Cause | Solution | Effort |
|---|---|---|---|---|---|---|
| PERF-01 | Multiple regex passes over same text | 🟢 LOW | `MarkdownSyntaxSanitizer.sanitize()` calls 6 methods, each with regex | Pipeline design requires multiple passes | **Option 1 (Keep):** Accept multiple passes (simpler code)<br><br>**Option 2 (Optimize):** Single-pass state machine<br>⚠️ Warning: Much more complex, only if profiling shows bottleneck | 40 hours |
| PERF-02 | CodeBlockProtector uses ArrayList | 🟢 LOW | `CodeBlockProtector.java:471-550` | ArrayList grows on each code block | Current: Safe (new instance per call)<br>No action needed unless reusing instances | 0 hours |

---

---

## Summary by Priority & Architectural Layer

### 🔴 CRITICAL (Do Immediately - 9 hours total)

#### UI Layer (ZK Components)
1. **UI-04:** Multiple legacy renderer paths - **Document current state** (4 hours) → **Safe coexistence** (16 hours)

#### Service Layer (LangChain4j Integration)
2. **SVC-01:** Non-streaming path sanitization gap (`AIMessageRenderer.java`) - **15 min**
3. **SVC-02:** Memory persistence sanitization gap (`ThreadAwareChatMemory.java`) - **15 min**

#### Configuration Layer
4. **CFG-01:** Static config breaks multi-tenancy (`MarkdownSyntaxSanitizer`) - **4 hours**

#### Utility Layer
5. **UTIL-01:** Three overlapping sanitization classes - **Document responsibilities** (0 hours) OR **pipeline refactor** (8 hours)

### 🟡 MEDIUM (Do This Week - 15 hours total)

#### UI Layer (ZK Components)
5. **UI-01:** Make `renderedHtml` volatile (`AIChatStreamingMessage.java`) - **5 min**
6. **UI-02:** Remove escaping from renderer (`StreamingMarkdownRenderer.java`) - **2 hours**
7. **UI-03:** Eliminate triple HTML escaping (UI → Service → Util) - **2 hours**

#### Utility Layer (Sanitizers)
8. **UTIL-02:** Resolve HTML removal vs escaping conflict - **2 hours**
9. **UTIL-03:** Consolidate URL validation (`SecuritySanitizer`) - **3 hours**
10. **UTIL-05:** Split validator responsibilities (`MarkdownValidator.java`) - **6 hours**
11. **UTIL-06:** Fix static config thread safety (use `AtomicBoolean`) - **30 min**

### 🟢 LOW (Do Next Sprint - 33 hours total)

#### Service Layer (LangChain4j)
12. **SVC-03:** Add provider-specific sanitization rules (`AIG_Provider` table) - **12 hours**

#### Utility Layer (Sanitizers)
13. **UTIL-04:** Refactor to pipeline pattern (extensibility) - **8 hours**

#### Configuration Layer
14. **CFG-02:** Review ChunkCleaner vs Sanitizer overlap - **2 hours**

#### Cross-Cutting
15. **SEC-03:** Add sanitization audit trail (optional) - **8 hours**

#### Performance (Profile First!)
16. **PERF-01:** Optimize multiple regex passes - **40 hours** ⚠️ Only if profiling shows bottleneck

---

## Implementation Guide by Layer

### Layer 1: UI Components (ZK Framework)

**Files to modify:**
- `AIChatStreamingMessage.java` - Make `renderedHtml` volatile
- `StreamingMarkdownRenderer.java` - Remove `escapeHtml()` method

**Key Principles:**
- UI components should **trust** sanitized input from service layer
- ZK threading: Synchronize access to shared state (already done with `queueLock`)
- Volatile fields for cross-thread visibility

**Example Fix (UI-01):**
```java
// File: AIChatStreamingMessage.java:142
- private String renderedHtml = null;
+ private volatile String renderedHtml = null;  // Visible across threads
```

**Example Fix (UI-02):**
```java
// File: StreamingMarkdownRenderer.java
/**
 * Renders markdown to HTML for streaming chat.
 *
 * IMPORTANT: Assumes input is pre-sanitized by MarkdownSyntaxSanitizer.
 * Does NOT perform HTML escaping - security handled upstream.
 *
 * @see MarkdownSyntaxSanitizer#sanitize(String)
 */
public class StreamingMarkdownRenderer {
    // REMOVE escapeHtml() method entirely (lines 836-890)
    // REMOVE all calls to escapeHtml()
}
```

---

### Layer 2: Service Layer (LangChain4j Integration)

**Files to modify:**
- `AIMessageRenderer.java` - Add sanitization for non-streaming
- `ThreadAwareChatMemory.java` - Add sanitization before persistence

**Key Principles:**
- **ALL** paths (streaming + non-streaming) MUST sanitize
- Sanitize **before** rendering HTML
- Sanitize **before** storing in database

**Example Fix (SVC-01):**
```java
// File: AIMessageRenderer.java:80
public static String render(String markdownText, MAIProvider provider, Properties ctx) {
    // Step 0: Sanitize AI-generated markdown (ADR-055)
    MarkdownConfig config = MarkdownConfig.fromProvider(provider);
    MarkdownSyntaxSanitizer sanitizer = new MarkdownSyntaxSanitizer(config);
    markdownText = sanitizer.sanitize(markdownText);

    // Step 1-N: Existing rendering logic
    // ...
}
```

**Example Fix (SVC-02):**
```java
// File: ThreadAwareChatMemory.java:320
if (message instanceof AiMessage) {
    content = getMessageContent(message);

    // Sanitize BEFORE rendering and storage (ADR-055)
    MarkdownConfig config = MarkdownConfig.fromProvider(provider);
    MarkdownSyntaxSanitizer sanitizer = new MarkdownSyntaxSanitizer(config);
    content = sanitizer.sanitize(content);

    String contentHtml = AIMessageRenderer.render(content, provider, ctx);
    entry = MAIChatEntry.createAIResponse(chat, contentHtml);
}
```

**LangChain4j Integration Points:**
```
LangChain4j ChatModel.generate()
    ↓
AiMessage (raw markdown)
    ↓
ThreadAwareChatMemory.add()  ← SANITIZE HERE (SVC-02)
    ↓
AIMessageRenderer.render()  ← SANITIZE HERE (SVC-01)
    ↓
AIChatStreamingMessage (already sanitized in streaming path)
```

---

### Layer 3: Utility Layer (Sanitization Tools)

**Files to modify:**
- `MarkdownSyntaxSanitizer.java` - Instance-based config, clear responsibilities
- `MarkdownValidator.java` - Structure fixing ONLY (no sanitization)
- `SecuritySanitizer.java` - Consolidate URL validation
- **NEW:** `MarkdownConfig.java` - Configuration object

**Key Principles:**
- **Single Responsibility:** Each class does ONE thing
- **Clear Trust Boundary:** Sanitizer → Validator → Renderer (no backward calls)
- **No Redundancy:** HTML escaping happens ONCE (in Sanitizer)

**Example: Current Confusing Flow**
```
Input: "Hello <script>alert('XSS')</script> **bold**"

MarkdownSyntaxSanitizer.sanitize():
  ├─ escapeRawHtml()          → "Hello  **bold**" (removed <script>)
  └─ MarkdownValidator.validate()
       └─ SecuritySanitizer.escapeHtml() → No-op (nothing to escape)

StreamingMarkdownRenderer.appendChunk():
  └─ escapeHtml()              → No-op (nothing to escape)

Result: "Hello  **bold**"
```

**Recommended Clear Flow:**
```
Input: "Hello <script>alert('XSS')</script> **bold**"

MarkdownSyntaxSanitizer.sanitize():
  ├─ Remove HTML tags          → "Hello  **bold**"
  └─ Fix markdown structure    → "Hello  **bold**"

StreamingMarkdownRenderer.render():
  └─ Render markdown to HTML   → "Hello  <strong>bold</strong>"
  └─ NO escaping (trust sanitizer!)

Result: "Hello  <strong>bold</strong>"
```

**Responsibility Matrix:**

| Class | Responsibility | Does NOT Do |
|---|---|---|
| `SecuritySanitizer` | HTML/URL/SQL primitive escaping | ❌ Markdown feature removal<br>❌ Structure validation |
| `MarkdownSyntaxSanitizer` | Remove unsafe HTML<br>Remove unsupported features<br>Validate URLs | ❌ HTML escaping (delegates to SecuritySanitizer)<br>❌ Structure fixing (delegates to Validator) |
| `MarkdownValidator` | Fix unclosed markers<br>Fix wrong nesting | ❌ HTML escaping<br>❌ Feature removal |
| `StreamingMarkdownRenderer` | Convert markdown → HTML | ❌ HTML escaping<br>❌ Validation<br>❌ Sanitization |

**Example Fix (UTIL-01): Document Responsibilities**
```java
/**
 * Sanitizes AI-generated markdown to prevent security issues and
 * rendering errors.
 *
 * Responsibilities:
 * - Remove HTML tags (ADR-055: AI should only generate markdown)
 * - Validate and sanitize URLs (delegate to SecuritySanitizer)
 * - Remove unsupported markdown features
 * - Protect code blocks and iDempiere zoom links during sanitization
 *
 * Does NOT:
 * - Escape HTML (removed entirely, not escaped)
 * - Fix markdown structure (delegated to MarkdownValidator)
 * - Render markdown to HTML (delegated to StreamingMarkdownRenderer)
 *
 * @see SecuritySanitizer for primitive escaping operations
 * @see MarkdownValidator for structure fixing
 * @see ADR-055 for constrained markdown syntax rationale
 */
public class MarkdownSyntaxSanitizer {
    // ...
}
```

**Example Fix (CFG-01): Instance-Based Config**
```java
// NEW FILE: MarkdownConfig.java
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
        return new MarkdownConfig(false, 100, 20, 50);
    }

    /**
     * Load configuration from provider settings.
     *
     * Future: Read from AIG_Provider table columns or JSON config field.
     */
    public static MarkdownConfig fromProvider(MAIProvider provider) {
        // TODO Phase 2: Read from database
        // For now, use defaults
        return createDefault();
    }

    // Getters
    public boolean isAllowBase64Images() { return allowBase64Images; }
    public int getMaxTableRows() { return maxTableRows; }
    public int getMaxTableColumns() { return maxTableColumns; }
    public int getMaxCodeBlockLines() { return maxCodeBlockLines; }
}

// MODIFIED: MarkdownSyntaxSanitizer.java
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
        // ...
    }

    // REMOVE static fields:
    // - private static boolean allowBase64Images = false;
    // - private static int maxTableRows = 100;
    // etc.
}
```

---

### Layer 4: Configuration & Storage

**Database Schema (Future Enhancement - SVC-03):**

Add provider-specific sanitization config to `AIG_Provider` table:

```sql
-- Option 1: Separate columns (structured)
ALTER TABLE AIG_Provider ADD COLUMN AllowBase64Images CHAR(1) DEFAULT 'N';
ALTER TABLE AIG_Provider ADD COLUMN MaxTableRows INTEGER DEFAULT 100;
ALTER TABLE AIG_Provider ADD COLUMN MaxCodeBlockLines INTEGER DEFAULT 50;

-- Option 2: JSON config field (flexible)
ALTER TABLE AIG_Provider ADD COLUMN SanitizationConfig VARCHAR(1000);
-- Example value: {"allowBase64Images": false, "maxTableRows": 100}
```

**Load config in LangChain4j integration:**
```java
// When creating sanitizer:
MAIProvider provider = MAIProvider.get(ctx, providerId);
MarkdownConfig config = MarkdownConfig.fromProvider(provider);
MarkdownSyntaxSanitizer sanitizer = new MarkdownSyntaxSanitizer(config);
```

---

## Testing Strategy by Layer

### Layer 1: UI Component Tests

```java
@Test
public void testRenderedHtmlVolatileVisibility() throws InterruptedException {
    AIChatStreamingMessage msg = new AIChatStreamingMessage(...);

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> readerResult = new AtomicReference<>();

    Thread writer = new Thread(() -> {
        msg.appendChunk("Test content");
        msg.complete();
        latch.countDown();
    });

    Thread reader = new Thread(() -> {
        try {
            latch.await();  // Wait for completion
            readerResult.set(msg.getRenderedHtml());  // Should see updated value
        } catch (InterruptedException e) { }
    });

    writer.start();
    reader.start();
    writer.join();
    reader.join();

    assertNotNull("Volatile field should be visible", readerResult.get());
}

@Test
public void testRendererDoesNotEscape() {
    String safeMarkdown = "Hello **world**";  // Already sanitized
    StreamingMarkdownRenderer renderer = new StreamingMarkdownRenderer(...);

    renderer.appendChunk(safeMarkdown);
    String html = renderer.getHtml();

    assertEquals("Hello <strong>world</strong>", html);
    // Renderer should NOT double-escape
}
```

### Layer 2: Service Layer Tests (LangChain4j)

```java
@Test
public void testNonStreamingPathSanitization() {
    String unsafeMarkdown = "Click <script>alert('XSS')</script> here";

    // Non-streaming rendering
    String html = AIMessageRenderer.render(unsafeMarkdown, provider, ctx);

    assertFalse("Script tags should be removed", html.contains("<script>"));
    assertTrue("Safe content should remain", html.contains("Click"));
}

@Test
public void testMemoryPersistenceSanitization() {
    ThreadAwareChatMemory memory = new ThreadAwareChatMemory(...);

    AiMessage unsafeMessage = new AiMessage("Hello <img src=x onerror=alert(1)>");
    memory.add(unsafeMessage);

    // Verify persisted HTML is sanitized
    MAIChatEntry entry = getLastChatEntry();
    assertFalse("HTML should be removed", entry.getContentHTML().contains("<img"));
}

@Test
public void testProviderSpecificConfig() {
    // Provider 1: Allows base64 images
    MAIProvider provider1 = createProvider(1);
    setProviderConfig(provider1, "allowBase64Images", true);

    // Provider 2: Blocks base64 images
    MAIProvider provider2 = createProvider(2);
    setProviderConfig(provider2, "allowBase64Images", false);

    String markdown = "![test](data:image/png;base64,iVBORw0KGg...)";

    String html1 = AIMessageRenderer.render(markdown, provider1, ctx);
    String html2 = AIMessageRenderer.render(markdown, provider2, ctx);

    assertTrue("Provider 1 allows base64", html1.contains("data:image"));
    assertFalse("Provider 2 blocks base64", html2.contains("data:image"));
}
```

### Layer 3: Utility Layer Tests

```java
@Test
public void testSanitizationDoesNotEscapeTwice() {
    String input = "Hello <script>alert('XSS')</script> world";

    MarkdownConfig config = MarkdownConfig.createDefault();
    MarkdownSyntaxSanitizer sanitizer = new MarkdownSyntaxSanitizer(config);

    String sanitized = sanitizer.sanitize(input);

    // Should remove HTML, not escape it
    assertEquals("Hello  world", sanitized);
    assertFalse(sanitized.contains("&lt;script&gt;"));  // Not escaped
    assertFalse(sanitized.contains("<script>"));         // Removed
}

@Test
public void testUrlValidationDelegation() {
    String markdown = "[Click](javascript:alert('XSS'))";

    MarkdownConfig config = MarkdownConfig.createDefault();
    MarkdownSyntaxSanitizer sanitizer = new MarkdownSyntaxSanitizer(config);

    String sanitized = sanitizer.sanitize(markdown);

    // URL validation should delegate to SecuritySanitizer
    assertFalse("javascript: protocol should be blocked",
                sanitized.contains("javascript:"));
}

@Test
public void testValidatorDoesNotSanitize() {
    String input = "**unclosed bold";

    String fixed = MarkdownValidator.validate(input);

    // Validator should ONLY fix structure (add closing marker)
    assertEquals("**unclosed bold**", fixed);

    // Validator should NOT remove or escape content
    String inputWithHtml = "<div>**bold**";
    String fixedWithHtml = MarkdownValidator.validate(inputWithHtml);
    assertTrue("Validator should not remove HTML", fixedWithHtml.contains("<div>"));
}
```

---

## Migration Path

### Week 1: Critical Fixes (9 hours)

**Day 1: Document Renderer Paths (UI-04 Phase 1) - 4 hours**
- [ ] Find all renderer classes: `find . -name "*Renderer*.java" -name "*Markdown*.java"`
- [ ] Document renderer selection logic (where? `AIChatWidget.java`?)
- [ ] Run migration state query (see SQL above)
- [ ] Create flow diagram showing all rendering paths
- [ ] Update UI-04 section with findings

**Day 2: Service Layer (SVC-01, SVC-02)**
- [ ] Add sanitization to `AIMessageRenderer.render()`
- [ ] Add sanitization to `ThreadAwareChatMemory.persistMessage()`
- [ ] Add integration test: `testNonStreamingPathSanitization()`
- [ ] Add integration test: `testMemoryPersistenceSanitization()`

**Day 2-3: Configuration Layer (CFG-01)**
- [ ] Create `MarkdownConfig.java` class
- [ ] Refactor `MarkdownSyntaxSanitizer` to instance-based
- [ ] Update all sanitizer call sites (AIMessageRenderer, ThreadAwareChatMemory, AIChatStreamingMessage)
- [ ] Update `MarkdownSyntaxSanitizerTest` (static → instance)

### Week 2-3: Architecture Cleanup (31 hours)

**Week 2 Focus: Safe Coexistence (UI-04 Phase 2) - 16 hours**
- [ ] Create `ChatMessageRenderer.renderForDisplay()` unified entry point
- [ ] Implement `isNewFormat()` detection logic
- [ ] Add `HtmlSanitizer` for legacy HTML content
- [ ] Add HTML marker `<!-- ADR-054 -->` to new messages
- [ ] Test all 3 rendering paths (new HTML, legacy markdown, legacy HTML)
- [ ] Integration test: `testLegacyMarkdown()`, `testLegacyHtmlWithXss()`

**Week 3 Focus: Architecture Cleanup - 15 hours**

**Day 1: UI Layer (UI-01, UI-02, UI-03)**
- [ ] Make `renderedHtml` volatile
- [ ] Remove `escapeHtml()` from `StreamingMarkdownRenderer`
- [ ] Add JavaDoc documenting trust boundary
- [ ] Add test: `testRenderedHtmlVolatileVisibility()`
- [ ] Add test: `testRendererDoesNotEscape()`

**Day 2-4: Utility Layer (UTIL-02, UTIL-03, UTIL-05, UTIL-06)**
- [ ] Document HTML removal decision in JavaDoc (UTIL-02)
- [ ] Create `SecuritySanitizer.UrlSanitizer` inner class (UTIL-03)
- [ ] Move URL protocol validation to `SecuritySanitizer` (UTIL-03)
- [ ] Split `MarkdownValidator` responsibilities (UTIL-05)
- [ ] Add `AtomicBoolean` for remaining static config (UTIL-06)
- [ ] Add test: `testUrlValidationDelegation()`
- [ ] Add test: `testValidatorDoesNotSanitize()`

### Sprint 2: Advanced Features & Migration (73 hours)

**UI-04 Phase 3: Complete Migration (40 hours)**
- [ ] Implement `ChatMigrationProcess` (iDempiere SvrProcess)
- [ ] Test migration on dev environment
- [ ] Run migration on production (low-traffic hours)
- [ ] Verify migration success (all entries have ADR-054 marker)
- [ ] Deprecate legacy renderer classes
- [ ] Simplify `ChatMessageRenderer` to single path
- [ ] Database cleanup: Mark `ContentMarkdown` as deprecated

**Optional Enhancements (33 hours)**

**Provider-Specific Config (SVC-03)**
- [ ] Add columns to `AIG_Provider` table OR JSON config field
- [ ] Implement `MarkdownConfig.fromProvider(MAIProvider)`
- [ ] Migration script to set defaults for existing providers
- [ ] Add test: `testProviderSpecificConfig()`

**Pipeline Pattern (UTIL-04)**
- [ ] Create `MarkdownProcessingPipeline` class
- [ ] Create `MarkdownProcessor` interface
- [ ] Implement individual processor classes
- [ ] Migrate existing code to use pipeline
- [ ] Add tests for each processor

---

## Decision Log

### Decision 1: HTML Removal vs Escaping (UTIL-02)

**Question:** Should we remove HTML tags or escape them?

**Current Implementation:** Remove (via `HTML_TAG_PATTERN.matcher().replaceAll("")`)

**Alternatives:**
- Option A: Remove (current)
- Option B: Escape (`<script>` → `&lt;script&gt;`)

**Decision:** **Keep removal** (Option A)

**Rationale:**
- AI should only generate markdown, not HTML
- Removal prevents any rendering issues (escaped HTML could still break layout)
- Simpler than escaping (no encoding concerns)
- Users can see what was typed, just without HTML tags

**Documented in:** ADR-055, `MarkdownSyntaxSanitizer` JavaDoc

### Decision 2: Instance vs Static Config (CFG-01)

**Question:** Should sanitizer config be static or instance-based?

**Current Implementation:** Static fields (shared across all calls)

**Decision:** **Instance-based config** (Phase 1)

**Rationale:**
- iDempiere is multi-tenant (AD_Client_ID)
- Different providers may need different rules (Anthropic vs Ollama)
- Future: Load from `AIG_Provider` table per provider

**Migration:** Static → Instance (Week 1), Database (Sprint 2)

### Decision 3: Pipeline vs Single Class (UTIL-04)

**Question:** Should we refactor to pipeline pattern or merge into one class?

**Decision:** **Document current responsibilities** (Week 2), **Pipeline optional** (Sprint 2)

**Rationale:**
- Current code works, but responsibilities overlap
- Documentation fixes clarity without refactoring
- Pipeline refactor is nice-to-have (extensibility), not critical
- Focus on critical bugs first (security, multi-tenancy)

**Timeline:** Document (Week 2), Refactor (Sprint 2 if time permits)

---

## Recommended Approach

### Week 1: Critical Fixes (Security + Multi-Tenancy)

**Goal:** Close security gap and fix multi-tenancy model

```java
// 1. Fix SEC-01 (15 min)
// File: AIMessageRenderer.java:80
public static String render(String markdownText, ...) {
    markdownText = MarkdownSyntaxSanitizer.sanitize(markdownText); // ← ADD
    // ... existing logic
}

// File: ThreadAwareChatMemory.java:320
if (message instanceof AiMessage) {
    content = getMessageContent(message);
    content = MarkdownSyntaxSanitizer.sanitize(content);  // ← ADD
    // ... existing logic
}

// 2. Fix CFG-01 (4 hours)
// Create MarkdownConfig class
public class MarkdownConfig {
    private final boolean allowBase64Images;
    private final int maxTableRows;
    private final int maxTableColumns;
    private final int maxCodeBlockLines;

    public static MarkdownConfig createDefault() { ... }
    public static MarkdownConfig fromProvider(MAIProvider provider) { ... }
}

// Refactor MarkdownSyntaxSanitizer to use instance config
public class MarkdownSyntaxSanitizer {
    private final MarkdownConfig config;

    public MarkdownSyntaxSanitizer(MarkdownConfig config) {
        this.config = config;
    }

    public String sanitize(String markdown) { ... }
}
```

**Tests to Update:**
- `MarkdownSyntaxSanitizerTest.java` (change static calls to instance calls)
- `AIChatStreamingIntegrationTest.java` (add non-streaming test case)

---

### Week 2: Architecture Cleanup (Separation of Concerns)

**Goal:** Remove duplication and clarify responsibilities

```java
// 3. Fix ARCH-02 (2 hours)
// File: StreamingMarkdownRenderer.java
// REMOVE escapeHtml() method entirely
// ADD JavaDoc to class:
/**
 * Renders markdown to HTML for streaming chat.
 *
 * IMPORTANT: Assumes input is pre-sanitized by MarkdownSyntaxSanitizer.
 * Does NOT perform HTML escaping - security is handled upstream.
 */

// 4. Fix SEC-02 + DUP-01 (2 hours)
// DECISION: Keep HTML removal (current approach)
// Document in MarkdownSyntaxSanitizer:
/**
 * Step 2: Remove HTML tags entirely.
 *
 * Note: We remove rather than escape HTML to prevent any potential
 * rendering issues. AI should only generate markdown, not HTML.
 *
 * @see SecuritySanitizer.escapeHtml() - NOT used here (would be redundant)
 */

// 5. Fix SEC-03 + DUP-02 (3 hours)
// File: SecuritySanitizer.java
public static class UrlSanitizer {
    private static final Set<String> SAFE_PROTOCOLS = Set.of("http", "https", "mailto");

    public static boolean isUrlSafe(String url) {
        // Protocol validation (moved from MarkdownSyntaxSanitizer)
    }

    public static String sanitize(String url) {
        if (!isUrlSafe(url)) {
            return "#unsafe-url-removed";
        }
        return escapeUrl(url);
    }
}

// File: MarkdownSyntaxSanitizer.java
private String sanitizeUrls(String markdown) {
    // Extract URLs from markdown
    // Delegate to SecuritySanitizer.UrlSanitizer.sanitize()
}

// 6. Fix TS-01 + TS-02 (35 min)
// File: AIChatStreamingMessage.java:142
private volatile String renderedHtml = null;  // ← ADD volatile

// File: MarkdownSyntaxSanitizer.java (if keeping static config temporarily)
private static final AtomicBoolean allowBase64Images = new AtomicBoolean(false);
private static final AtomicInteger maxTableRows = new AtomicInteger(100);
// ... etc
```

---

### Sprint 2: Advanced Refactoring (Optional)

**Goal:** Pipeline pattern for extensibility

```java
// 7. Fix ARCH-03 (8 hours)
public class MarkdownProcessingPipeline {
    private final List<MarkdownProcessor> processors;

    public static MarkdownProcessingPipeline createDefault(MarkdownConfig config) {
        return new MarkdownProcessingPipeline()
            .addProcessor(new CodeBlockProtector())
            .addProcessor(new ZoomLinkProtector())
            .addProcessor(new HtmlRemover())
            .addProcessor(new UrlSanitizer(config))
            .addProcessor(new UnsupportedFeatureRemover(config))
            .addProcessor(new StructureFixer())
            .addProcessor(new BlockRestorer());
    }

    public String process(String markdown) {
        for (MarkdownProcessor processor : processors) {
            markdown = processor.process(markdown);
        }
        return markdown;
    }
}

interface MarkdownProcessor {
    String process(String markdown);
}

// Each processor is small, focused, testable
class HtmlRemover implements MarkdownProcessor {
    public String process(String markdown) {
        return HTML_TAG_PATTERN.matcher(markdown).replaceAll("");
    }
}
```

---

## Alternative: Simplified Merge (Fastest Path)

**If time is limited**, merge all into ONE class:

```java
/**
 * Single-purpose markdown security processor for AI chat.
 * Combines sanitization, validation, and structure fixing.
 *
 * Responsibilities:
 * - Remove unsafe HTML tags
 * - Validate and sanitize URLs
 * - Remove unsupported markdown features
 * - Fix markdown structure errors
 */
public class AIChatMarkdownProcessor {

    private final MarkdownConfig config;

    public AIChatMarkdownProcessor(MarkdownConfig config) {
        this.config = config;
    }

    public String process(String markdown) {
        // All logic from MarkdownSyntaxSanitizer + MarkdownValidator
        // in one place, one pass
    }
}
```

**Deprecate:**
- `MarkdownSyntaxSanitizer` (merge into new class)
- `MarkdownValidator` (merge into new class)

**Keep:**
- `SecuritySanitizer` (for HTML/URL primitives)
- `StreamingMarkdownRenderer` (remove escaping)

**Effort:** 6 hours (vs 8 hours for pipeline pattern)

---

## Testing Strategy

### Critical Path Tests (Add Immediately)

```java
// Test SEC-01 fix
@Test
public void testNonStreamingPathSanitization() {
    String unsafeMarkdown = "Hello <script>alert('XSS')</script>";
    String rendered = AIMessageRenderer.render(unsafeMarkdown, ...);
    assertFalse(rendered.contains("<script>"));
}

// Test CFG-01 fix
@Test
public void testProviderSpecificConfig() {
    MarkdownConfig config1 = new MarkdownConfig(true, 100, 20, 50);  // Allow base64
    MarkdownConfig config2 = new MarkdownConfig(false, 50, 10, 25);  // Block base64

    MarkdownSyntaxSanitizer sanitizer1 = new MarkdownSyntaxSanitizer(config1);
    MarkdownSyntaxSanitizer sanitizer2 = new MarkdownSyntaxSanitizer(config2);

    String markdown = "![image](data:image/png;base64,iVBORw0...)";

    assertTrue(sanitizer1.sanitize(markdown).contains("data:image"));   // Allowed
    assertFalse(sanitizer2.sanitize(markdown).contains("data:image"));  // Blocked
}

// Test thread safety (TS-01)
@Test
public void testConcurrentRenderedHtmlAccess() throws InterruptedException {
    AIChatStreamingMessage msg = new AIChatStreamingMessage(...);

    Thread writer = new Thread(() -> msg.complete());
    Thread reader = new Thread(() -> {
        String html = msg.getRenderedHtml();  // Should not return stale null
        assertNotNull(html);
    });

    writer.start();
    Thread.sleep(10);  // Small delay to trigger race
    reader.start();

    writer.join();
    reader.join();
}
```

---

## Decision Matrix: Which Path to Take?

| Approach | Effort | Complexity Reduction | Risk | Recommended? |
|---|---|---|---|---|
| **Quick Fixes Only** (SEC-01, TS-01, TS-02) | 1 hour | 0% | Low | ✅ Do immediately |
| **+ Architecture Cleanup** (Remove duplication) | 12 hours | 30% | Low | ✅ Do this week |
| **+ Pipeline Refactor** (ARCH-03) | 20 hours | 60% | Medium | 🟡 Optional |
| **Full Merge** (One class) | 6 hours | 50% | Medium | 🟡 Alternative to pipeline |

**Recommendation:** Do **Quick Fixes + Architecture Cleanup** (13 hours total) this week. Defer pipeline refactor to next sprint when you have more time.

---

## Files to Modify

### Immediate Changes (Week 1)
- ✏️ `AIMessageRenderer.java` (add sanitization)
- ✏️ `ThreadAwareChatMemory.java` (add sanitization)
- ✏️ `AIChatStreamingMessage.java` (add volatile)
- ✏️ `MarkdownSyntaxSanitizer.java` (instance-based config)
- ➕ `MarkdownConfig.java` (new class)
- ✏️ `MarkdownSyntaxSanitizerTest.java` (update tests)

### Architecture Cleanup (Week 2)
- ✏️ `StreamingMarkdownRenderer.java` (remove escapeHtml)
- ✏️ `SecuritySanitizer.java` (add UrlSanitizer inner class)
- ✏️ `MarkdownSyntaxSanitizer.java` (delegate URL validation)
- ✏️ All test files (update for new responsibilities)

### Optional Refactor (Sprint 2)
- ➕ `MarkdownProcessingPipeline.java` (new)
- ➕ `MarkdownProcessor.java` (interface)
- ➕ Individual processor classes (6-8 new files)
- ❌ Deprecate `MarkdownSyntaxSanitizer` and `MarkdownValidator`
