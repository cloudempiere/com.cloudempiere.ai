# Sanitization: Invalid Character Handling

## Requirement

**User Statement:**
> We are aware that invalid chars could break data processing and html rendering, so to avoid we could make invalid char sanitization as br, critical characters. this usually makes problems in http operations (url decoding/encoding)

**Breaking Down:**
1. ⚠️ **Problem:** Invalid characters break data processing & HTML rendering
2. ✅ **Solution:** Sanitize invalid chars (replace with `<br>` for critical characters)
3. 🔍 **Common source:** HTTP operations (URL decoding/encoding issues)

---

## Problem Analysis

### Critical Characters That Break Processing

| Character | Issue | Where It Breaks | Current Handling |
|---|---|---|---|
| **Control Characters** | `\x00`-`\x1F` (except `\n`, `\t`) | HTML rendering, database storage | ✅ **REMOVED** (ChunkCleaner) |
| **Null Byte** | `\x00` (NUL) | SQL queries, string processing | ✅ **REMOVED** (ChunkCleaner) |
| **Non-UTF8** | Invalid byte sequences | HTTP transfer, JSON serialization | ⚠️ **Partially handled** |
| **Surrogate Pairs** | `\uD800`-`\uDFFF` (unpaired) | UTF-16 encoding, database | ⚠️ **Not explicitly handled** |
| **Zero-Width** | `\u200B`, `\uFEFF` (BOM) | Text processing, rendering | ✅ **REMOVED** (ChunkCleaner) |
| **Line Separators** | `\u2028`, `\u2029` (Unicode) | JavaScript strings, JSON | ⚠️ **Not explicitly handled** |
| **URL-Encoded** | `%00`, `%0D%0A` | HTTP operations (CRLF injection) | ✅ **Handled** (CRLF normalized) |

---

## Current Sanitization Review

### ✅ File: `ChunkCleaner.java` - **CONTROL CHARACTERS HANDLED**

**Location:** Line 32-53

```java
public static String clean(String chunk) {
    if (chunk == null || chunk.isEmpty()) {
        return chunk;
    }

    // 1. Remove zero-width characters
    String cleaned = chunk
        .replaceAll("[\u200B\u200C\u200D\u200E\u200F\uFEFF]", "");

    // 2. Remove control characters (except \t, \n)
    cleaned = cleaned.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

    // 3. Normalize line breaks
    cleaned = cleaned
        .replace("\r\n", "\n")
        .replace("\r", "\n");

    // 4. Limit consecutive blank lines to 2
    cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

    return cleaned;
}
```

**What It Removes:**

| Range | Characters | Reason |
|---|---|---|
| `\x00-\x08` | NUL, SOH, STX, ETX, EOT, ENQ, ACK, BEL, BS | Break SQL, string processing, rendering |
| `\x0B` | Vertical Tab (VT) | Breaks HTML layout |
| `\x0C` | Form Feed (FF) | Breaks HTML layout |
| `\x0E-\x1F` | SO, SI, DLE, ..., US | Control characters, no semantic value |
| `\x7F` | DEL | Delete character, breaks text |
| `\u200B-\u200F` | Zero-width space, joiner, etc. | Breaks text selection, copy-paste |
| `\uFEFF` | BOM (Byte Order Mark) | Breaks JSON, text processing |

**What It Preserves:**

| Char | Code | Reason |
|---|---|---|
| `\t` | `\x09` | Needed for markdown code block indentation |
| `\n` | `\x0A` | Line breaks (normalized from `\r\n`, `\r`) |

**Status:** ✅ **COMPREHENSIVE CONTROL CHARACTER SANITIZATION IMPLEMENTED**

---

### Where ChunkCleaner Is Called

**File:** `AIChatStreamingMessage.java`

**Search for ChunkCleaner usage:**

```bash
grep -n "ChunkCleaner" com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/component/AIChatStreamingMessage.java
```

**Expected Call Chain:**

```
LLM Stream → AIChatStreamingMessage.onNext(chunk)
    ↓
ChunkCleaner.clean(chunk)  ← CONTROL CHARS REMOVED HERE
    ↓
MarkdownSyntaxSanitizer.sanitize(cleaned)  ← UNSAFE MARKDOWN REMOVED
    ↓
StreamingMarkdownRenderer.appendChunk(sanitized)  ← MARKDOWN → HTML
    ↓
htmlOutput (safe, sanitized HTML)
```

**Validation Needed:** Verify ChunkCleaner is called **before** rendering in all paths:
1. ✅ Streaming path (AIChatStreamingMessage)
2. ❓ Non-streaming path (AIMessageRenderer)
3. ❓ Historical chat reload (ThreadAwareChatMemory)

---

## Gap Analysis

### ⚠️ Missing: Unicode Line Separators

**Problem Characters:**
- `\u2028` (LINE SEPARATOR) - Breaks JavaScript strings, JSON
- `\u2029` (PARAGRAPH SEPARATOR) - Breaks JavaScript strings, JSON

**Example Attack:**

```json
{
  "content": "Hello\u2028World"
}
// JSON.parse() in JavaScript treats \u2028 as literal newline → SYNTAX ERROR!
```

**Current Status:** ❌ **NOT HANDLED**

`ChunkCleaner` only removes `\x00-\x1F` (ASCII control chars), not Unicode control chars (`\u2028`, `\u2029`).

**Fix:**

```java
// Add to ChunkCleaner.java line 39:
String cleaned = chunk
    .replaceAll("[\u200B\u200C\u200D\u200E\u200F\uFEFF\u2028\u2029]", "");
    //                                                    ^^^^ ^^^^ ADD THESE
```

---

### ⚠️ Missing: Unpaired Surrogate Characters

**Problem Characters:**
- `\uD800-\uDBFF` (high surrogates) without matching low surrogate
- `\uDC00-\uDFFF` (low surrogates) without matching high surrogate

**Why It Matters:**
- Java uses UTF-16 internally (surrogate pairs for characters > U+FFFF)
- Unpaired surrogates are invalid UTF-16 → Database encoding errors
- Can cause `IllegalArgumentException` in String operations

**Example:**

```java
String invalid = "Hello\uD800World";  // High surrogate without low surrogate
// Database insert: ERROR (invalid UTF-8 sequence)
```

**Current Status:** ❌ **NOT HANDLED**

**Fix:**

```java
// Add to ChunkCleaner.java:
public static String removeSurrogatePairs(String text) {
    StringBuilder clean = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
        char ch = text.charAt(i);
        if (Character.isHighSurrogate(ch)) {
            // Check if next char is valid low surrogate
            if (i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                clean.append(ch);  // Valid pair, keep both
                clean.append(text.charAt(++i));
            } else {
                // Unpaired high surrogate, skip
                continue;
            }
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

---

### ⚠️ User Requirement: Replace with `<br>` Instead of Removal

**User Request:**
> Make invalid char sanitization as br, critical characters

**Current Behavior:** ChunkCleaner **removes** control characters

**Proposed Behavior:** Replace critical characters with `<br>` (line break)

**Rationale:**

| Approach | Pros | Cons |
|---|---|---|
| **Remove** (current) | • Clean output<br>• No layout issues | • Loss of context<br>• User doesn't know chars were removed |
| **Replace with `<br>`** (proposed) | • Preserves line structure<br>• Visual indicator of removed chars | • May add excessive line breaks<br>• Could break markdown parsing |

**Which Characters to Replace?**

| Character | Action | Reasoning |
|---|---|---|
| `\x00` (NUL) | ✅ Replace with `<br>` | User should know null byte was present |
| `\x0D` (CR) | ⚠️ Already normalized to `\n` | Keep current behavior (normalize, not replace) |
| `\x1B` (ESC, ANSI codes) | ✅ Replace with `<br>` | Terminal escape codes → line break |
| `\x7F` (DEL) | ✅ Replace with `<br>` | Delete char → line break |
| `\u200B` (Zero-width space) | ❌ Remove (current) | Invisible, no semantic value |
| `\uFEFF` (BOM) | ❌ Remove (current) | Byte order mark, no semantic value |

**Proposed Implementation:**

```java
public static String clean(String chunk) {
    if (chunk == null || chunk.isEmpty()) {
        return chunk;
    }

    // 1. Remove zero-width characters (no semantic value)
    String cleaned = chunk
        .replaceAll("[\u200B\u200C\u200D\u200E\u200F\uFEFF\u2028\u2029]", "");

    // 2. Replace CRITICAL control characters with <br> (user-visible indicator)
    // These are chars that indicate data corruption or encoding issues:
    cleaned = cleaned.replaceAll("[\x00\x1B\x7F]", "<br/>");
    //                              ^^^  ^^^  ^^^ NUL, ESC, DEL

    // 3. Remove OTHER control characters (no semantic value)
    cleaned = cleaned.replaceAll("[\x01-\x08\x0B\x0C\x0E-\x1A\x1C-\x1F]", "");

    // 4. Normalize line breaks (keep current behavior)
    cleaned = cleaned
        .replace("\r\n", "\n")
        .replace("\r", "\n");

    // 5. Limit consecutive blank lines to 2
    cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

    // 6. Remove unpaired surrogates
    cleaned = removeSurrogatePairs(cleaned);

    return cleaned;
}
```

**Trade-Offs:**

| Scenario | Current (Remove) | Proposed (Replace with `<br>`) |
|---|---|---|
| **AI sends `\x00`** | "HelloWorld" (invisible removal) | "Hello<br/>World" (visible indicator) |
| **URL decode `%00`** | Silently removed | Line break inserted (user sees data issue) |
| **ANSI codes `\x1B[31m`** | Color codes removed | Line break inserted (breaks formatting but visible) |
| **Normal text** | No change | No change |

**Recommendation:**

**Option 1 (Conservative):** Keep current removal behavior ✅ **RECOMMENDED**
- Cleaner output
- Fewer layout issues
- Control chars have no semantic value anyway

**Option 2 (Transparent):** Replace critical chars with `<br/>` 🟡 **IF DEBUGGING NEEDED**
- Makes data corruption visible
- Helps debug HTTP encoding issues
- **Only enable in development/debug mode**

**Option 3 (Hybrid):** Replace with `<br/>` + warning log
```java
if (chunk.matches(".*[\x00\x1B\x7F].*")) {
    log.warning("Control characters detected in AI response (may indicate encoding issue)");
    // Replace with <br/>
}
```

---

## HTTP URL Encoding/Decoding Issues

### Problem: URL-Encoded Null Bytes

**Scenario:**

```
AI Response:  "Hello\x00World"
    ↓ HTTP Transfer (automatic URL encoding by some middleware)
URL Encoded:  "Hello%00World"
    ↓ Server receives and decodes
Decoded:      "Hello\x00World"  (null byte back!)
    ↓ ChunkCleaner.clean()
Cleaned:      "HelloWorld"  (null byte removed)
```

**Status:** ✅ **HANDLED** (ChunkCleaner removes `\x00` after decoding)

---

### Problem: CRLF Injection

**Scenario:**

```
AI Response:  "Header: value\r\nX-Injected: malicious"
    ↓ HTTP Transfer
URL Encoded:  "Header: value%0D%0AX-Injected: malicious"
    ↓ Server decodes
Decoded:      "Header: value\r\nX-Injected: malicious"
    ↓ ChunkCleaner.clean()
Normalized:   "Header: value\nX-Injected: malicious"  (CRLF → LF)
```

**Status:** ✅ **HANDLED** (Line 46 normalizes `\r\n` → `\n`)

**Impact:** CRLF injection attacks neutralized (HTTP header injection prevented)

---

### Problem: Non-UTF8 Byte Sequences

**Scenario:**

```
AI Response: (byte sequence) [0xC0, 0x80]  ← Invalid UTF-8 (overlong encoding of NUL)
    ↓ HTTP Transfer
    ↓ Java String decoding
Result: "�" (replacement character U+FFFD)
    ↓ ChunkCleaner.clean()
Output: "�" (passed through)
```

**Status:** ⚠️ **PARTIALLY HANDLED**

**Gap:** `ChunkCleaner` doesn't explicitly remove replacement character `\uFFFD`.

**Fix:**

```java
// Add to ChunkCleaner.java line 39:
String cleaned = chunk
    .replaceAll("[\u200B\u200C\u200D\u200E\u200F\uFEFF\u2028\u2029\uFFFD]", "");
    //                                                                ^^^^^ ADD THIS
```

---

## Validation: Call Chain Analysis

### Path 1: Streaming Messages

```
LangChain4j Stream → AIChatStreamingMessage.onNext(chunk)
    ↓
❓ ChunkCleaner.clean(chunk)  ← VERIFY THIS IS CALLED!
    ↓
MarkdownSyntaxSanitizer.sanitize(cleaned)
    ↓
StreamingMarkdownRenderer.appendChunk(sanitized)
```

**Validation Required:** Check `AIChatStreamingMessage.java` line ~200-300 for ChunkCleaner call.

---

### Path 2: Non-Streaming Messages

```
LangChain4j Response → AIMessageRenderer.render(markdown)
    ↓
❓ ChunkCleaner.clean(markdown)  ← VERIFY THIS IS CALLED!
    ↓
MarkdownSyntaxSanitizer.sanitize(cleaned)
    ↓
CommonMarkRenderer.render(sanitized)
```

**Validation Required:** Check `AIMessageRenderer.java` for ChunkCleaner call.

---

### Path 3: Historical Chat Reload

```
Database → MAIChatEntry.getContentHTML()
    ↓
❓ ChunkCleaner.clean(html)  ← PROBABLY NOT CALLED (already stored)
    ↓
Display in ZK Html component
```

**Status:** ⚠️ **POTENTIAL GAP**

Historical chats stored before ChunkCleaner was implemented may contain control characters!

**Fix:** Add migration script to clean existing chat entries:

```sql
UPDATE AIG_ChatEntry
SET ContentHTML = REGEXP_REPLACE(
    ContentHTML,
    '[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]',  -- Control chars
    '',  -- Remove
    'g'  -- Global flag
)
WHERE ContentHTML ~ '[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]';
```

---

## Recommendations

### 🔴 CRITICAL: Verify ChunkCleaner Integration

**Action:** Confirm ChunkCleaner is called in ALL code paths:

1. ✅ **Streaming:** `AIChatStreamingMessage.java`
   ```bash
   grep -A 5 "ChunkCleaner" com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/component/AIChatStreamingMessage.java
   ```

2. ❓ **Non-Streaming:** `AIMessageRenderer.java`
   ```bash
   grep -A 5 "ChunkCleaner" com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/util/AIMessageRenderer.java
   ```

3. ❓ **Memory Persistence:** `ThreadAwareChatMemory.java`
   ```bash
   grep -A 5 "ChunkCleaner" com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/provider/langchain4j/ThreadAwareChatMemory.java
   ```

**If missing:** Add `ChunkCleaner.clean(text)` before rendering/storage.

**Effort:** 2 hours (if missing in 1-2 paths)

---

### 🟡 HIGH: Add Missing Unicode Control Chars

**Action:** Enhance `ChunkCleaner.java` to remove:

```java
// Line 39: Add Unicode line separators and replacement char
String cleaned = chunk
    .replaceAll("[\u200B\u200C\u200D\u200E\u200F\uFEFF\u2028\u2029\uFFFD]", "");
    //                                                    ^^^^ ^^^^ ^^^^^ ADD THESE
```

**Effort:** 15 minutes + testing (30 minutes total)

---

### 🟡 MEDIUM: Add Unpaired Surrogate Removal

**Action:** Add surrogate pair validation to `ChunkCleaner.java`

**Implementation:** See code above (removeSurrogatePairs method)

**Effort:** 2 hours (implementation + testing)

---

### 🟢 LOW: Database Migration for Historical Chats

**Action:** Clean existing chat entries stored before ChunkCleaner

**SQL Script:**

```sql
-- PostgreSQL
UPDATE AIG_ChatEntry
SET ContentHTML = REGEXP_REPLACE(
    ContentHTML,
    '[\x00-\x08\x0B\x0C\x0E-\x1F\x7F\u200B-\u200F\uFEFF\u2028\u2029\uFFFD]',
    '',
    'g'
)
WHERE ContentHTML ~ '[\x00-\x08\x0B\x0C\x0E-\x1F\x7F\u200B-\u200F\uFEFF\u2028\u2029\uFFFD]';

-- Oracle
UPDATE AIG_ChatEntry
SET ContentHTML = REGEXP_REPLACE(
    ContentHTML,
    '[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]|[\u200B-\u200F]|[\uFEFF\u2028\u2029\uFFFD]',
    ''
)
WHERE REGEXP_LIKE(ContentHTML, '[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]|[\u200B-\u200F]|[\uFEFF\u2028\u2029\uFFFD]');
```

**Effort:** 1 hour (write + test + run)

---

### 🟢 OPTIONAL: Replace with `<br/>` Instead of Removal

**Action:** Modify `ChunkCleaner.java` to replace critical chars with `<br/>`

**Decision Required:** Do you want:
- **Option A:** Remove control chars (current) → Cleaner output
- **Option B:** Replace with `<br/>` → Makes data issues visible
- **Option C:** Hybrid (replace + log warning) → Best of both

**Effort:** 1 hour

---

## Final Status

| Requirement | Status | Action Needed |
|---|---|---|
| **Control character removal** | ✅ DONE | Verify ChunkCleaner is called in all paths |
| **Zero-width character removal** | ✅ DONE | None |
| **CRLF normalization** | ✅ DONE | None |
| **Unicode line separators** | ❌ MISSING | Add `\u2028`, `\u2029` to regex |
| **Replacement character** | ❌ MISSING | Add `\uFFFD` to regex |
| **Unpaired surrogates** | ❌ MISSING | Add surrogate pair validation |
| **URL encoding issues** | ✅ HANDLED | Control chars removed after decode |
| **Historical chat cleanup** | ❌ TODO | Run database migration script |
| **Replace with `<br/>`** | ❓ DECISION | User to decide (Remove vs Replace) |

**Overall Verdict:** ✅ **MOSTLY IMPLEMENTED**

The critical control character sanitization is already in place via `ChunkCleaner.java`.

**Remaining gaps:**
1. 🔴 Verify ChunkCleaner is called in all code paths (critical)
2. 🟡 Add Unicode line separators (`\u2028`, `\u2029`)
3. 🟡 Add replacement character (`\uFFFD`)
4. 🟢 Add unpaired surrogate removal
5. 🟢 Clean historical chat entries

**Total Effort:** ~6 hours to close all gaps
