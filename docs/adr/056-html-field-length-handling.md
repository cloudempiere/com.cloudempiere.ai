# ADR-056: HTML Field Length Handling and Truncation Prevention

**Status:** Accepted
**Date:** 2026-02-04
**Decision Makers:** Development Team
**Related:** ADR-054 (HTML-Only Chat Message Storage), ADR-047 (Streaming Chat Rendering Best Practices)

## Context

Chat messages are stored as rendered HTML in `CM_ChatEntry.CharacterData` (per ADR-054). When HTML length exceeds the database field limit, silent truncation occurs, causing:

1. **Broken HTML Structure:** Incomplete closing tags (`<div>` without `</div>`)
2. **UI Corruption:** Browser renders broken HTML, causing layout collapse
3. **Content Leakage:** Other page elements (dashboard) leak into chat widget
4. **System Instability:** Entire widget becomes unusable

### Real-World Incident

**Scenario:**
- AI generated response with >6000 chars of HTML
- `CM_ChatEntry.CharacterData` field was VARCHAR(4000)
- HTML silently truncated at 4000 chars during save
- On login/reload, broken HTML destroyed entire AI chat widget
- Dashboard content from main panel rendered inside AI chat
- User had to manually increase field length to recover

**Root Cause:** No validation or graceful handling of field length limits.

## Problem Statement

**How should we handle HTML content that exceeds database field limits to prevent UI corruption while maintaining good UX?**

Key requirements:
1. **Never break the UI** - Corruption must not make widget unusable
2. **Clear feedback** - User knows when content is too large
3. **Graceful degradation** - Prefer partial content over total failure
4. **Retrievable limit** - Use actual field length from AD_Column, not hardcoded

## Decision

**Implement layered defense with immediate validation and documented fallback strategies.**

### Phase 1: Defensive Validation (Implemented)

**Validate HTML length before save against actual field limit from AD_Column.**

**Implementation:**
```java
private void captureRenderedHtml() {
    renderedHtml = streamingContent.getContent();
    int htmlLength = renderedHtml.length();

    // Get field length from AD_Column for CM_ChatEntry.CharacterData
    int maxFieldLength = getCharacterDataFieldLength();

    if (htmlLength > maxFieldLength) {
        throw new AdempiereException(
            "AI response HTML too long to save: " + htmlLength +
            " chars exceeds field limit of " + maxFieldLength + " chars");
    }
}

private int getCharacterDataFieldLength() {
    String sql = "SELECT FieldLength FROM AD_Column " +
                "WHERE AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'CM_ChatEntry') " +
                "AND ColumnName = 'CharacterData'";
    return DB.getSQLValueEx(null, sql); // Returns actual field length
}
```

**Benefits:**
- ✅ Prevents corruption at source
- ✅ Uses actual field limit (not hardcoded)
- ✅ Clear error message to user
- ✅ Simple to implement

**Drawbacks:**
- ⚠️ User loses content if exceeds limit
- ⚠️ Poor UX for very long responses

### Phase 2: Smart Truncation (Future)

**If HTML exceeds limit, truncate at safe tag boundary with warning.**

**Implementation Strategy:**
```java
if (htmlLength > maxFieldLength) {
    // Find last complete tag before limit
    String truncated = truncateAtSafePoint(html, maxFieldLength - 200);

    // Add warning notice
    truncated += "<div style='margin-top:12px; padding:8px; background:#fff3cd; border-left:4px solid #ffc107;'>" +
                 "<strong>⚠️ Content Truncated</strong><br>" +
                 "Response was too long to save completely (" + htmlLength + " chars). " +
                 "Showing first " + truncated.length() + " chars. " +
                 "Consider increasing field length or enabling compression." +
                 "</div>";

    renderedHtml = truncated;
}

private String truncateAtSafePoint(String html, int maxLength) {
    // Parse HTML and truncate at last complete tag before maxLength
    // Ensure all opening tags are closed
    // Use JSoup or similar library for robust HTML parsing
}
```

**Benefits:**
- ✅ Saves most content
- ✅ UI stays functional
- ✅ Clear warning to user

**Drawbacks:**
- ⚠️ Still loses data
- ⚠️ Complex implementation (requires HTML parser)

### Phase 3: Graceful Degradation on Load (Future)

**Detect broken HTML on reload and fall back to plain text.**

**Implementation Strategy:**
```java
// In AIChatWidget reload logic
try {
    String html = entry.getCharacterData();
    validateHtmlStructure(html); // Check balanced tags
    messageComponent.setContent(html);
} catch (HtmlCorruptionException e) {
    log.warn("Broken HTML detected, falling back to plain text", e);

    // Strip HTML tags and show as plain text
    String plainText = stripHtmlTags(html);

    String fallbackHtml =
        "<div style='padding:12px; background:#f8f9fa; border:1px solid #dee2e6;'>" +
        escapeHtml(plainText) +
        "</div>" +
        "<div style='margin-top:8px; color:#dc3545; font-size:11px;'>" +
        "⚠️ Content was truncated during storage. Showing plain text fallback." +
        "</div>";

    messageComponent.setContent(fallbackHtml);
}

private void validateHtmlStructure(String html) throws HtmlCorruptionException {
    // Check for balanced opening/closing tags
    // Use stack-based parser or JSoup validation
    if (!isWellFormed(html)) {
        throw new HtmlCorruptionException("Unbalanced HTML tags detected");
    }
}
```

**Benefits:**
- ✅ UI never breaks
- ✅ Graceful degradation
- ✅ User sees readable content

**Drawbacks:**
- ⚠️ Lost formatting
- ⚠️ Requires HTML validation library

### Phase 4: HTML Compression (Future)

**Compress HTML before storage to fit 3-5x more content in same field.**

**Implementation Strategy:**
```java
private void captureRenderedHtml() {
    String html = streamingContent.getContent();

    // Compress HTML using gzip
    byte[] compressed = gzipCompress(html);
    String base64Encoded = Base64.getEncoder().encodeToString(compressed);

    if (base64Encoded.length() > maxFieldLength) {
        throw new AdempiereException("Even compressed HTML exceeds field limit");
    }

    renderedHtml = base64Encoded;
}

// On load:
private String decompressHtml(String compressedBase64) {
    byte[] compressed = Base64.getDecoder().decode(compressedBase64);
    return gzipDecompress(compressed);
}
```

**Benefits:**
- ✅ 3-5x size reduction
- ✅ Fits more in same field
- ✅ Transparent to users

**Drawbacks:**
- ⚠️ Cannot query/search compressed content
- ⚠️ Debugging harder (must decompress to read)
- ⚠️ Small performance overhead

### Phase 5: CLOB or Overflow Storage (Future)

**Store large content in CLOB field or separate table.**

**Option A: Migrate to CLOB**
```sql
ALTER TABLE CM_ChatEntry MODIFY CharacterData CLOB;
```

**Option B: Overflow Table**
```sql
CREATE TABLE CM_ChatEntry_Overflow (
    CM_ChatEntry_ID NUMBER(10),
    Content CLOB,
    CONSTRAINT FK_ChatEntry FOREIGN KEY (CM_ChatEntry_ID)
        REFERENCES CM_ChatEntry(CM_ChatEntry_ID)
);

-- Store in main table if fits, overflow table if too large
```

**Benefits:**
- ✅ No size limits
- ✅ Simple to implement (Option A)
- ✅ Maintains VARCHAR for small content (Option B)

**Drawbacks:**
- ⚠️ Database migration required
- ⚠️ CLOB performance characteristics different
- ⚠️ Overflow table adds complexity (Option B)

## Alternatives Considered

### Alternative 1: Store Markdown Instead of HTML

**Rejected:** Conflicts with ADR-054 (HTML-Only Chat Message Storage). Rendering inconsistencies between streaming and reload were major pain point. Markdown syntax also requires storage space.

### Alternative 2: Client-Side Rendering Only

**Rejected:** Browser must re-render on every page load, impacting performance. Also conflicts with ADR-054 which explicitly chose server-side rendering for consistency.

### Alternative 3: Hardcode Field Length

**Rejected:** Fragile. If field length changes in database, hardcoded value becomes incorrect. Using AD_Column metadata is more robust.

## Implementation Roadmap

| Phase | Priority | Effort | Status | Deliverable |
|-------|----------|--------|--------|-------------|
| Phase 1 | **High** | Low | ✅ Implemented | Defensive validation with AD_Column lookup |
| Phase 2 | Medium | Medium | Planned | Smart truncation at tag boundaries |
| Phase 3 | Medium | Medium | Planned | Graceful degradation on load |
| Phase 4 | Low | Medium | Future | HTML compression |
| Phase 5 | Low | High | Future | CLOB migration or overflow table |

**Recommendation:** Implement Phase 2 (Smart Truncation) or Phase 3 (Graceful Degradation) in next release to improve UX when limits are hit.

## Consequences

### Positive

1. **UI Stability:** Validation prevents broken HTML from corrupting widget
2. **Clear Feedback:** Users know when content exceeds limits
3. **Maintainable:** Uses AD_Column metadata, not hardcoded values
4. **Extensible:** Layered approach allows incremental improvements

### Negative

1. **User Friction:** Current implementation blocks save if too long
2. **Content Loss:** Long responses cannot be saved (until Phase 2+)
3. **Incomplete Solution:** Phase 1 only prevents corruption, doesn't solve underlying size limits

### Neutral

1. **Migration Path:** Provides clear roadmap for improved handling
2. **Decision Deferred:** Choose between compression, CLOB, or truncation based on production data

## Metrics

Track in production to inform future phases:

1. **Truncation Rate:** How often responses exceed field limits
2. **Average HTML Size:** Typical response sizes to determine if CLOB needed
3. **95th Percentile Size:** Worst-case sizes to set appropriate limits
4. **Compression Ratio:** If implementing compression, measure space savings

## References

- **ADR-054:** HTML-Only Chat Message Storage
- **ADR-047:** Streaming Chat Rendering Best Practices
- **Database Field Metadata:** AD_Column table for dynamic limit lookup
- **JSoup Library:** For robust HTML parsing/validation (if implementing Phase 2/3)

## Notes

- Field length retrieval uses AD_Column to support multi-version iDempiere deployments
- Conservative default (4000 chars) used if AD_Column lookup fails
- Error message includes actual lengths to help administrators adjust field size
- Future phases can be implemented independently based on production needs

## Decision

**Accepted** - Implement Phase 1 (Defensive Validation) immediately. Document Phases 2-5 as future enhancements to be prioritized based on production metrics and user feedback.
