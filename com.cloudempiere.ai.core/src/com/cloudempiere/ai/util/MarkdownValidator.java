/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.util.CLogger;

/**
 * Validates and fixes common markdown errors in AI-generated content.
 *
 * <p><b>Problem:</b> LLMs sometimes generate invalid markdown combinations:
 * <ul>
 *   <li>Unclosed markers: **bold without closing **</li>
 *   <li>Wrong nesting order: **bold ### heading** (heading inside bold)</li>
 *   <li>Overlapping markers: **bold *italic** (bold not closed before italic ends)</li>
 *   <li>Mixed syntax: ### **Heading** (valid but needs proper handling)</li>
 * </ul>
 *
 * <p><b>Solution:</b> Pre-process markdown chunks to fix common errors before rendering:
 * <ol>
 *   <li>Detect unclosed inline markers (**, *, `, etc.)</li>
 *   <li>Reorder invalid nesting (heading must be outermost)</li>
 *   <li>Close markers in proper LIFO order</li>
 *   <li>Preserve valid nested structures</li>
 * </ol>
 *
 * <p><b>Usage:</b>
 * <pre>
 * String cleaned = MarkdownValidator.validate(aiGeneratedMarkdown);
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class MarkdownValidator {

    private static final CLogger log = CLogger.getCLogger(MarkdownValidator.class);

    /**
     * Markdown element types for tracking nesting.
     */
    private enum ElementType {
        BOLD,        // ** or __
        ITALIC,      // * or _
        CODE,        // `
        CODE_BLOCK,  // ```
        HEADING      // # ## ### etc.
    }

    /**
     * Tracked markdown element with position and marker.
     */
    private static class MarkerInfo {
        ElementType type;
        String marker;
        int position;

        MarkerInfo(ElementType type, String marker, int position) {
            this.type = type;
            this.marker = marker;
            this.position = position;
        }
    }

    /**
     * Validate and fix markdown content.
     *
     * <p>Fixes common AI markdown errors:
     * <ul>
     *   <li>Unclosed markers</li>
     *   <li>Invalid nesting order</li>
     *   <li>Overlapping markers</li>
     * </ul>
     *
     * @param markdown raw markdown text from AI
     * @return validated markdown with fixes applied
     */
    public static String validate(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return markdown;
        }

        try {
            // Process line by line to preserve structure
            String[] lines = markdown.split("\n", -1);
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                // Fix line-level issues
                line = fixHeadingMarkers(line);
                line = fixInlineMarkers(line);

                result.append(line);
                if (i < lines.length - 1) {
                    result.append('\n');
                }
            }

            return result.toString();
        } catch (Exception e) {
            // On any error, return original markdown (fail-safe)
            log.warning("MarkdownValidator error: " + e.getMessage() + " - returning original");
            return markdown;
        }
    }

    /**
     * Fix heading markers (### **Bold** → valid, **### Invalid** → fixed).
     *
     * <p><b>Valid heading patterns:</b>
     * <ul>
     *   <li>### Heading</li>
     *   <li>### **Bold heading**</li>
     *   <li>### *Italic heading*</li>
     * </ul>
     *
     * <p><b>Invalid patterns (fixed):</b>
     * <ul>
     *   <li>**### Invalid** → ### **Invalid**</li>
     *   <li>*### Invalid* → ### *Invalid*</li>
     * </ul>
     *
     * @param line line to fix
     * @return fixed line
     */
    private static String fixHeadingMarkers(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }

        String trimmed = line.trim();

        // Pattern: **### or *### or __### or _### (invalid - heading inside emphasis)
        // Fix: Move heading markers to start
        Pattern invalidHeading = Pattern.compile("^([*_]+)(#{1,6})\\s");
        Matcher m = invalidHeading.matcher(trimmed);

        if (m.find()) {
            String emphasisMarkers = m.group(1);
            String headingMarkers = m.group(2);
            String rest = trimmed.substring(m.end());

            // Reorder: heading first, then emphasis around content
            String fixed = headingMarkers + " " + emphasisMarkers + rest + emphasisMarkers;
            log.fine("Fixed invalid heading order: " + trimmed + " → " + fixed);

            // Preserve leading whitespace
            int leadingSpaces = line.indexOf(trimmed.charAt(0));
            if (leadingSpaces > 0) {
                return line.substring(0, leadingSpaces) + fixed;
            }
            return fixed;
        }

        return line;
    }

    /**
     * Fix inline markers (bold, italic, code) - close unclosed markers.
     *
     * <p>Handles:
     * <ul>
     *   <li>Unclosed bold: **text → **text**</li>
     *   <li>Unclosed italic: *text → *text*</li>
     *   <li>Unclosed code: `text → `text`</li>
     *   <li>Overlapping markers: **bold *italic** → **bold** *italic*</li>
     * </ul>
     *
     * @param line line to fix
     * @return fixed line
     */
    private static String fixInlineMarkers(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }

        // Skip heading lines - they're handled separately
        if (line.trim().startsWith("#")) {
            return line;
        }

        // Track open markers (LIFO stack)
        Stack<MarkerInfo> openMarkers = new Stack<>();
        StringBuilder result = new StringBuilder();

        int i = 0;
        while (i < line.length()) {
            // Check for markers at current position
            MarkerInfo marker = detectMarker(line, i);

            if (marker != null) {
                // Check if this closes an open marker
                boolean closes = false;

                if (!openMarkers.isEmpty()) {
                    MarkerInfo top = openMarkers.peek();

                    // Same type and marker text = closing marker
                    if (top.type == marker.type && top.marker.equals(marker.marker)) {
                        openMarkers.pop();
                        closes = true;
                    }
                    // Special case: * can close ** if no other * in between (auto-close bold)
                    else if (marker.type == ElementType.ITALIC &&
                             top.type == ElementType.BOLD &&
                             top.marker.equals("**")) {
                        // Auto-close unclosed bold before italic closes
                        result.append("**");
                        openMarkers.pop();
                        log.fine("Auto-closed unclosed bold before italic at position " + i);
                    }
                }

                if (!closes) {
                    // Opening marker
                    openMarkers.push(marker);
                }

                result.append(marker.marker);
                i += marker.marker.length();
            } else {
                // Regular character
                result.append(line.charAt(i));
                i++;
            }
        }

        // Close any remaining open markers at end of line
        // Close in reverse order (LIFO)
        while (!openMarkers.isEmpty()) {
            MarkerInfo unclosed = openMarkers.pop();
            result.append(unclosed.marker);
            log.fine("Auto-closed unclosed marker: " + unclosed.marker + " at end of line");
        }

        return result.toString();
    }

    /**
     * Detect markdown marker at position.
     *
     * @param text text to check
     * @param pos position to check
     * @return marker info if detected, null otherwise
     */
    private static MarkerInfo detectMarker(String text, int pos) {
        if (pos >= text.length()) {
            return null;
        }

        char ch = text.charAt(pos);

        // Check for code block marker (```)
        if (ch == '`' && pos + 2 < text.length() &&
            text.charAt(pos + 1) == '`' && text.charAt(pos + 2) == '`') {
            return new MarkerInfo(ElementType.CODE_BLOCK, "```", pos);
        }

        // Check for inline code marker (`)
        if (ch == '`') {
            return new MarkerInfo(ElementType.CODE, "`", pos);
        }

        // Check for bold marker (** or __)
        if ((ch == '*' || ch == '_') && pos + 1 < text.length() && text.charAt(pos + 1) == ch) {
            String marker = String.valueOf(ch) + ch;
            return new MarkerInfo(ElementType.BOLD, marker, pos);
        }

        // Check for italic marker (* or _)
        if (ch == '*' || ch == '_') {
            return new MarkerInfo(ElementType.ITALIC, String.valueOf(ch), pos);
        }

        return null;
    }

    /**
     * Validate markdown structure (for testing/debugging).
     *
     * <p>Checks for:
     * <ul>
     *   <li>Balanced markers</li>
     *   <li>Valid nesting order</li>
     *   <li>Proper heading syntax</li>
     * </ul>
     *
     * @param markdown markdown to validate
     * @return list of validation issues (empty if valid)
     */
    public static List<String> checkStructure(String markdown) {
        List<String> issues = new ArrayList<>();

        if (markdown == null || markdown.isEmpty()) {
            return issues;
        }

        String[] lines = markdown.split("\n", -1);

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];

            // Check heading syntax
            if (line.trim().startsWith("#")) {
                if (!line.matches("^\\s*#{1,6}\\s+.*")) {
                    issues.add("Line " + (lineNum + 1) + ": Invalid heading syntax (missing space after #)");
                }
            }

            // Check balanced markers (simple check)
            int boldCount = countOccurrences(line, "**");
            int italicCount = countOccurrences(line, "*") - (boldCount * 2);
            int codeCount = countOccurrences(line, "`");

            if (boldCount % 2 != 0) {
                issues.add("Line " + (lineNum + 1) + ": Unbalanced bold markers (**)");
            }
            if (italicCount % 2 != 0) {
                issues.add("Line " + (lineNum + 1) + ": Unbalanced italic markers (*)");
            }
            if (codeCount % 2 != 0) {
                issues.add("Line " + (lineNum + 1) + ": Unbalanced code markers (`)");
            }
        }

        return issues;
    }

    /**
     * Count occurrences of substring in string.
     */
    private static int countOccurrences(String text, String substring) {
        int count = 0;
        int pos = 0;
        while ((pos = text.indexOf(substring, pos)) != -1) {
            count++;
            pos += substring.length();
        }
        return count;
    }
}
