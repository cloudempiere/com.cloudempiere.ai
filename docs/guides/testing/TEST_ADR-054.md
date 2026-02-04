# ADR-054 Test Plan

## Quick Verification Steps

### Test 1: New Message Storage (HTML Format)
**Expected:** New AI messages should be stored as HTML in the database

1. Open iDempiere and navigate to AI Chat
2. Send a test message: "Show me a table with 3 sample products"
3. Wait for AI response to complete
4. In database, run:
   ```sql
   SELECT CM_ChatEntry_ID, CharacterData
   FROM CM_ChatEntry
   WHERE AD_User_ID = [AI_USER_ID]  -- Replace with your AI user ID
   ORDER BY Created DESC
   LIMIT 1;
   ```
5. **Verify:** CharacterData starts with `<div` and contains HTML tags like `<p>`, `<table>`, `<pre>`
6. **Expected log:** `[UI-STREAM] Saving rendered HTML, length=XXX`
7. **Expected log:** `[HTML-CAPTURE] Captured rendered HTML for persistence, length=XXX`

---

### Test 2: Fast Reload (No Re-Rendering)
**Expected:** Reloading chat should display HTML directly without re-rendering

1. Continue from Test 1
2. Refresh the browser or navigate away and back to the chat
3. Check browser console/logs for:
   - ✅ **Should see:** `[RELOAD-DISPLAY] Using pre-rendered HTML | length: XXX`
   - ❌ **Should NOT see:** `[RELOAD-RENDER] Using unified AIMessageRenderer`
4. **Verify:** Message displays correctly with formatting, tables, etc.
5. **Performance:** Reload should feel instant (<10ms per message)

---

### Test 3: Legacy Markdown Fallback
**Expected:** Old markdown messages should still render correctly

**Option A: If you have old messages**
1. Find a chat with old markdown messages (created before this change)
2. Open the chat
3. Check logs:
   - ✅ **Should see:** `[RELOAD-RENDER] Rendering legacy markdown | length: XXX`
4. **Verify:** Old messages display correctly

**Option B: Simulate old markdown**
1. Manually insert markdown into database:
   ```sql
   UPDATE CM_ChatEntry
   SET CharacterData = '**Bold text** and *italic* with a table:\n\n| Col1 | Col2 |\n|------|------|\n| A | B |'
   WHERE CM_ChatEntry_ID = [SOME_OLD_ENTRY_ID];
   ```
2. Reload chat
3. **Verify:** Message renders correctly as HTML
4. Check logs for `[RELOAD-RENDER] Rendering legacy markdown`

---

### Test 4: Cancelled Message
**Expected:** Cancelled messages should save partial HTML

1. Open AI Chat
2. Send a message: "Write a long story about..."
3. **Immediately click Cancel** while streaming
4. Check database:
   ```sql
   SELECT CharacterData
   FROM CM_ChatEntry
   WHERE AD_User_ID = [AI_USER_ID]
   ORDER BY Created DESC
   LIMIT 1;
   ```
5. **Verify:** Contains `<p><em>AI request cancelled</em></p>`
6. **Verify:** Any partial content before cancellation is in HTML format
7. **Expected log:** `[CANCEL] Partial HTML response persisted, length=XXX`

---

### Test 5: Error Message
**Expected:** Error messages should save HTML with error formatting

1. Temporarily misconfigure AI provider (invalid API key)
2. Send a message
3. Wait for error
4. Check database - should contain HTML error message
5. **Verify:** Contains `<div class='ai-error-message'>` or similar HTML
6. **Expected log:** `[ERROR] Error HTML response persisted, length=XXX`
7. Reload chat - error should display correctly without re-rendering

---

### Test 6: Non-Streaming Mode (if applicable)
**Expected:** Non-streaming messages should also be stored as HTML

1. If your setup supports disabling streaming, disable it
2. Send a message
3. Check database - should contain HTML
4. **Expected log:** Message about rendering to HTML before saving (in non-streaming path)
5. Reload - should use pre-rendered HTML

---

### Test 7: Performance Benchmark
**Expected:** 10x improvement in reload time

**Setup:**
1. Create a chat with 50+ messages (mix of text, tables, code blocks)

**Measurement:**
1. Open browser DevTools → Console
2. Reload the chat
3. Count how many times you see:
   - `[RELOAD-DISPLAY] Using pre-rendered HTML` (should be ~50+)
   - `[RELOAD-RENDER] Rendering legacy markdown` (should be 0 if all new messages)
4. **Performance:** Reload should complete in <500ms total (~10ms per message)

**Before ADR-054:** Would see `[RELOAD-RENDER]` for every message (~100ms total)

---

### Test 8: Formatting Verification
**Expected:** All markdown formatting preserved in HTML storage

Send a message that triggers various formatting:
```
"Show me examples of:
1. **Bold text**
2. *Italic text*
3. `inline code`
4. A table with 3 columns
5. A code block in Python
```

**Verify:**
1. Message displays correctly during streaming
2. Database contains HTML with proper tags:
   - `<strong>` for bold
   - `<em>` for italic
   - `<code>` for inline code
   - `<table>` for tables
   - `<pre><code>` for code blocks
3. Reload shows identical formatting (no differences)

---

### Test 9: Zoom Links (if configured)
**Expected:** Zoom links work in stored HTML

1. Send message that includes iDempiere record references
2. During streaming, zoom link should be clickable
3. After completion, check database - HTML should contain zoom link
4. Reload - zoom link should still be clickable
5. **Verify:** No re-rendering needed for zoom links to work

---

### Test 10: Copy Button
**Expected:** Copy button works with HTML-stored messages

1. Complete a message (HTML stored)
2. Reload the chat
3. Click copy button on the message
4. Paste into text editor
5. **Verify:** Copies plain text (not HTML tags)
6. Should copy the readable content, not `<div>` tags

---

## SQL Verification Queries

### Check HTML Storage Format
```sql
-- Find recent AI messages and check format
SELECT
    CM_ChatEntry_ID,
    CASE
        WHEN CharacterData LIKE '<div%' THEN 'HTML'
        WHEN CharacterData LIKE '<%' THEN 'HTML (other)'
        ELSE 'MARKDOWN'
    END as Format,
    LENGTH(CharacterData) as Length,
    LEFT(CharacterData, 100) as Preview,
    Created
FROM CM_ChatEntry
WHERE AD_User_ID = [AI_USER_ID]  -- Replace with AI user ID
ORDER BY Created DESC
LIMIT 20;
```

### Compare Storage Sizes
```sql
-- Compare old markdown vs new HTML storage
SELECT
    AVG(CASE WHEN CharacterData LIKE '<div%' THEN LENGTH(CharacterData) END) as Avg_HTML_Size,
    AVG(CASE WHEN CharacterData NOT LIKE '<%' THEN LENGTH(CharacterData) END) as Avg_Markdown_Size,
    COUNT(CASE WHEN CharacterData LIKE '<div%' THEN 1 END) as HTML_Count,
    COUNT(CASE WHEN CharacterData NOT LIKE '<%' THEN 1 END) as Markdown_Count
FROM CM_ChatEntry
WHERE AD_User_ID = [AI_USER_ID];
```

---

## Rollback Testing

If you need to rollback:

1. Revert the 3 changed files
2. Restart server
3. **Verify:** Old HTML messages still display (they'll be re-rendered as if they were markdown)
4. **Expected:** May see garbled output for HTML messages, but system won't crash

**Note:** Rollback is non-destructive - HTML stored in DB is safe, just won't be used.

---

## Success Checklist

- [ ] New messages stored as HTML (starts with `<div`)
- [ ] Reload uses pre-rendered HTML (logs confirm)
- [ ] Old markdown messages render correctly via fallback
- [ ] Cancelled messages save partial HTML
- [ ] Error messages save HTML
- [ ] Performance improved (50+ messages reload in <500ms)
- [ ] All formatting preserved (bold, italic, tables, code blocks)
- [ ] Zoom links work in stored HTML (if applicable)
- [ ] Copy button works correctly
- [ ] No console errors

---

## Common Issues & Solutions

### Issue: "renderScheduled is false" warning
**Solution:** This is expected during testing - just means no rendering scheduled

### Issue: HTML stored but reload still renders
**Solution:** Check HTML detection logic - ensure messageText contains `<div`, `<p>`, `<table>`, or `<pre>`

### Issue: Old messages show raw HTML
**Solution:** HTML detection is failing - check if old markdown contains HTML-like strings

### Issue: Performance not improved
**Solution:** Ensure logs show `[RELOAD-DISPLAY] Using pre-rendered HTML` - if not, HTML detection failed

---

## Log Monitoring

### Key Log Messages to Watch

**During Streaming:**
```
[HTML-CAPTURE] Captured rendered HTML for persistence, length=XXX
[UI-STREAM] Saving rendered HTML, length=XXX
```

**During Reload:**
```
[RELOAD-DISPLAY] Using pre-rendered HTML | length: XXX  // NEW messages
[RELOAD-RENDER] Rendering legacy markdown | length: XXX  // OLD messages
```

**During Cancel:**
```
[CANCEL] Partial HTML response persisted, length=XXX
```

**During Error:**
```
[ERROR] Error HTML response persisted, length=XXX
```

---

## Automated Testing (Future)

To add automated tests:

1. **Unit Test:** Test HTML detection logic
   ```java
   assertTrue(isHtml("<div>content</div>"));
   assertTrue(isHtml("text <p>para</p>"));
   assertFalse(isHtml("**markdown**"));
   ```

2. **Integration Test:** Test save/load cycle
   - Create streaming message
   - Complete it
   - Verify HTML stored in DB
   - Load and verify no re-rendering

3. **Performance Test:** Benchmark reload time
   - Create 100 messages
   - Measure reload time
   - Assert < 1 second total
