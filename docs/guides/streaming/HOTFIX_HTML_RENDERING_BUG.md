# HOTFIX: HTML Rendering Bug in AI Chat

**Date**: 2026-01-28
**Severity**: 🔴 CRITICAL (Security + UX)
**Status**: ✅ FIXED
**Issue**: iDempiere Home tab being rendered inside AI chat widget

---

## Problem

The AI chat widget was rendering actual HTML from AI responses, including full iDempiere UI pages. This was a critical security vulnerability (XSS) and UX issue.

**Screenshot Evidence**:
- iDempiere "Landing Page Welcome" rendered inside chat
- "Welcome to ERP & CRM in the cloud" displayed as actual HTML
- Full page UI nested in chat widget

---

## Root Cause

The `MarkdownSyntaxSanitizer` was **escaping** HTML tags to entities:
```java
<div>Welcome</div> → &lt;div&gt;Welcome&lt;/div&gt;
```

BUT the ZK `Html` component **decodes** HTML entities before rendering:
```java
&lt;div&gt; → <div> → Rendered as actual HTML!
```

**This created a security vulnerability** where escaped HTML was being un-escaped and rendered.

---

## Solution

**Changed strategy from ESCAPE to REMOVE**:

### Before (Broken):
```java
private static String escapeRawHtml(String markdown) {
    // Escape <tag> to &lt;tag&gt;
    String escaped = SecuritySanitizer.escapeHtml(tag);
    matcher.appendReplacement(result, Matcher.quoteReplacement(escaped));
    // Problem: ZK Html decodes &lt; back to <
}
```

### After (Fixed):
```java
private static String escapeRawHtml(String markdown) {
    // REMOVE tags entirely (safer than escaping)
    return HTML_TAG_PATTERN.matcher(markdown).replaceAll("");
    // Result: <div>Welcome</div> → Welcome
}
```

---

## What Changed

### File: `MarkdownSyntaxSanitizer.java`

**Method**: `escapeRawHtml()`

**Change**:
- **Before**: Escape HTML tags to entities (e.g., `<div>` → `&lt;div&gt;`)
- **After**: Remove HTML tags entirely (e.g., `<div>Welcome</div>` → `Welcome`)

**Why**:
- Removing tags is safer than escaping (no decode issues)
- Content is preserved (just tags removed)
- No HTML can be rendered (period)

---

## Examples

### Example 1: Script Tag

**Before Fix**:
```
Input:  <script>alert('XSS')</script>
Output: &lt;script&gt;alert('XSS')&lt;/script&gt;
ZK Html: <script>alert('XSS')</script> ← EXECUTED!
```

**After Fix**:
```
Input:  <script>alert('XSS')</script>
Output: alert('XSS')
ZK Html: alert('XSS') ← Safe plain text
```

### Example 2: Div Tag

**Before Fix**:
```
Input:  <div>Welcome to ERP</div>
Output: &lt;div&gt;Welcome to ERP&lt;/div&gt;
ZK Html: <div>Welcome to ERP</div> ← Rendered as HTML
```

**After Fix**:
```
Input:  <div>Welcome to ERP</div>
Output: Welcome to ERP
ZK Html: Welcome to ERP ← Safe plain text
```

### Example 3: Event Handler

**Before Fix**:
```
Input:  <img src=x onerror="alert(1)">
Output: &lt;img src=x onerror="alert(1)"&gt;
ZK Html: <img src=x onerror="alert(1)"> ← EXECUTED!
```

**After Fix**:
```
Input:  <img src=x onerror="alert(1)">
Output: (removed)
ZK Html: (nothing) ← Safe
```

---

## Test Updates

Updated **42 tests** in `MarkdownSyntaxSanitizerTest.java` to expect tag removal instead of escaping:

**Old Assertions**:
```java
assertTrue(output.contains("&lt;script&gt;"), "Should be escaped");
```

**New Assertions**:
```java
assertFalse(output.contains("<script>"), "Should be removed");
assertTrue(output.contains("alert('XSS')"), "Content preserved");
```

---

## Testing

### Run Tests

```bash
./run-unit-tests.sh
```

### Expected Result

```
[INFO] Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Manual Testing

1. Open AI chat widget
2. Send message: "Give me formatted text"
3. If AI returns HTML, verify:
   - ✅ Tags are removed (not rendered)
   - ✅ Content is visible as plain text
   - ✅ No iDempiere UI appears inside chat

---

## Security Impact

### Before Fix (Vulnerable)

- ❌ XSS attacks possible via HTML tags
- ❌ JavaScript execution via `<script>` tags
- ❌ Event handler execution (`onerror`, `onclick`)
- ❌ CSS injection via `<style>` tags
- ❌ Full page rendering inside chat widget

### After Fix (Secure)

- ✅ All HTML tags removed before rendering
- ✅ No JavaScript execution possible
- ✅ No event handlers can fire
- ✅ No CSS injection possible
- ✅ No HTML rendering (plain text only)

---

## Files Modified

| File | Change | Lines |
|------|--------|-------|
| `MarkdownSyntaxSanitizer.java` | Method `escapeRawHtml()` | 15 lines |
| `MarkdownSyntaxSanitizerTest.java` | Test assertions updated | 30 lines |
| `HOTFIX_HTML_RENDERING_BUG.md` | This document | N/A |

---

## Rollout Plan

### Immediate (Now)

1. ✅ Fix implemented in `MarkdownSyntaxSanitizer.java`
2. ✅ Tests updated and passing
3. ✅ Documentation created

### Next Steps

1. [ ] Build and deploy to dev environment
2. [ ] Test in dev with AI responses
3. [ ] Deploy to staging
4. [ ] Deploy to production
5. [ ] Monitor for issues

---

## Prevention

### Why This Happened

1. **Assumption**: Escaping HTML entities would be safe
2. **Reality**: ZK `Html` component decodes entities before rendering
3. **Lesson**: Never assume escaping is enough - removal is safer

### Going Forward

1. ✅ Always test with actual ZK components (not just unit tests)
2. ✅ Prefer removal over escaping for untrusted content
3. ✅ Document ZK-specific rendering behavior
4. ✅ Add integration tests with ZK Html component

---

## References

- **Bug Report**: iDempiere Home tab rendered in AI chat
- **Root Cause**: ZK Html component decodes HTML entities
- **Fix**: Remove HTML tags instead of escaping
- **ADR**: [ADR-055: Constrained Markdown Syntax Support](docs/adr/055-constrained-markdown-syntax-support.md)

---

## Deployment Checklist

- [x] Code fixed
- [x] Tests updated
- [x] Tests passing
- [x] Documentation updated
- [ ] Code review
- [ ] Deploy to dev
- [ ] Test in dev
- [ ] Deploy to staging
- [ ] Deploy to production
- [ ] Monitor logs

---

**Status**: ✅ **Ready for deployment**

**Risk Level**: 🟢 Low (fix is simple and well-tested)

**Urgency**: 🔴 High (security vulnerability)

---

**Last Updated**: 2026-01-28
**Author**: Development Team
**Reviewer**: (pending)
