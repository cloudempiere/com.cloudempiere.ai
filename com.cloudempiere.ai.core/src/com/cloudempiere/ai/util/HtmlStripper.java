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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.util.CLogger;

/**
 * Strips HTML tags from AI responses to ensure only Markdown is used.
 *
 * <p><b>Purpose:</b> Safety layer to prevent HTML injection when LLM ignores
 * system prompt constraints. Since we only have a Markdown renderer, any HTML
 * in responses will break rendering or create security vulnerabilities.
 *
 * <p><b>Strategy:</b>
 * <ul>
 *   <li>Extract and protect code blocks (inline `code` and fenced ```blocks```)</li>
 *   <li>Strip HTML tags from non-code content only</li>
 *   <li>Preserve HTML entities (&amp;lt;, &amp;quot;, etc.) - these are valid in Markdown</li>
 *   <li>Restore protected code blocks after stripping</li>
 *   <li>Log warnings when HTML is detected (system prompt violation)</li>
 * </ul>
 *
 * <p><b>Important:</b> This runs BEFORE markdown rendering (in AIMessageRenderer
 * pipeline step 2.5), so it operates on raw markdown text where code blocks
 * are still delimited by backticks, not HTML tags.
 *
 * <p><b>Usage:</b>
 * <pre>
 * String aiResponse = llm.generate(prompt);
 * String cleanResponse = HtmlStripper.stripHtml(aiResponse);
 * String html = markdownRenderer.render(cleanResponse);
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 * @since v0.34.0
 */
public class HtmlStripper {

    private static final CLogger log = CLogger.getCLogger(HtmlStripper.class);

    /**
     * Pattern to match HTML tags (opening, closing, self-closing).
     * Preserves HTML entities (&amp;lt;, &amp;quot;, etc.).
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
        "</?[a-zA-Z][a-zA-Z0-9]*(?:\\s+[^>]*)?/?>",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern to match fenced code blocks (```...```).
     * Supports language hints: ```java, ```python, etc.
     * Non-greedy to match individual blocks, not everything between first and last.
     */
    private static final Pattern FENCED_CODE_BLOCK_PATTERN = Pattern.compile(
        "```[a-zA-Z0-9]*\\s*\\n.*?\\n```",
        Pattern.DOTALL
    );

    /**
     * Pattern to match inline code (`code`).
     * Non-greedy to avoid matching across multiple inline code spans.
     */
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile(
        "`[^`]+?`"
    );

    /**
     * Placeholder prefix for code block extraction.
     */
    private static final String CODE_BLOCK_PLACEHOLDER = "___CODE_BLOCK_";
    private static final String CODE_BLOCK_PLACEHOLDER_SUFFIX = "___";

    /**
     * Strip all HTML tags from text, preserving code blocks and HTML entities.
     *
     * <p><b>Algorithm:</b>
     * <ol>
     *   <li>Extract all fenced code blocks (```...```) with placeholders</li>
     *   <li>Extract all inline code spans (`...`) with placeholders</li>
     *   <li>Strip HTML tags from remaining text</li>
     *   <li>Restore code blocks and inline code</li>
     * </ol>
     *
     * <p><b>Examples:</b>
     * <ul>
     *   <li>{@code "<div>hello</div>"} → {@code "hello"}</li>
     *   <li>{@code "<p>text <b>bold</b></p>"} → {@code "text bold"}</li>
     *   <li>{@code "<br/>"} → {@code ""}</li>
     *   <li>{@code "1 &lt; 2"} → {@code "1 &lt; 2"} (entity preserved)</li>
     *   <li>{@code "&quot;quote&quot;"} → {@code "&quot;quote&quot;"} (entity preserved)</li>
     *   <li>{@code "See `<div>code</div>` example"} → {@code "See `<div>code</div>` example"} (code preserved)</li>
     *   <li>{@code "```html\n<div>example</div>\n```"} → preserved (fenced code block)</li>
     * </ul>
     *
     * <p><b>Note:</b> Logs a warning when HTML tags are detected outside code blocks,
     * as this indicates the LLM violated the Markdown-only constraint in the system prompt.
     *
     * @param text text that may contain HTML tags
     * @return text with HTML tags removed (except in code blocks)
     */
    public static String stripHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Step 1: Extract and protect fenced code blocks
        List<String> fencedCodeBlocks = new ArrayList<>();
        Matcher fencedMatcher = FENCED_CODE_BLOCK_PATTERN.matcher(text);
        StringBuffer afterFenced = new StringBuffer();
        int fencedIndex = 0;
        while (fencedMatcher.find()) {
            String codeBlock = fencedMatcher.group();
            fencedCodeBlocks.add(codeBlock);
            String placeholder = CODE_BLOCK_PLACEHOLDER + fencedIndex + CODE_BLOCK_PLACEHOLDER_SUFFIX;
            fencedMatcher.appendReplacement(afterFenced, Matcher.quoteReplacement(placeholder));
            fencedIndex++;
        }
        fencedMatcher.appendTail(afterFenced);

        // Step 2: Extract and protect inline code spans
        List<String> inlineCodeSpans = new ArrayList<>();
        Matcher inlineMatcher = INLINE_CODE_PATTERN.matcher(afterFenced.toString());
        StringBuffer afterInline = new StringBuffer();
        int inlineIndex = 0;
        while (inlineMatcher.find()) {
            String codeSpan = inlineMatcher.group();
            inlineCodeSpans.add(codeSpan);
            String placeholder = CODE_BLOCK_PLACEHOLDER + "INLINE_" + inlineIndex + CODE_BLOCK_PLACEHOLDER_SUFFIX;
            inlineMatcher.appendReplacement(afterInline, Matcher.quoteReplacement(placeholder));
            inlineIndex++;
        }
        inlineMatcher.appendTail(afterInline);

        String textWithoutCode = afterInline.toString();

        // Step 3: Check if remaining text (outside code blocks) contains HTML tags
        if (HTML_TAG_PATTERN.matcher(textWithoutCode).find()) {
            log.warning("HTML tags detected in AI response outside code blocks (system prompt violation). Stripping tags.");
            if (log.isLoggable(java.util.logging.Level.FINE)) {
                // Log snippet for debugging (first 200 chars)
                String snippet = textWithoutCode.length() > 200
                    ? textWithoutCode.substring(0, 200) + "..."
                    : textWithoutCode;
                log.fine("Response snippet (code blocks removed): " + snippet);
            }
        }

        // Step 4: Remove all HTML tags from non-code content
        String stripped = HTML_TAG_PATTERN.matcher(textWithoutCode).replaceAll("");

        // Step 5: Restore inline code spans
        for (int i = 0; i < inlineCodeSpans.size(); i++) {
            String placeholder = CODE_BLOCK_PLACEHOLDER + "INLINE_" + i + CODE_BLOCK_PLACEHOLDER_SUFFIX;
            stripped = stripped.replace(placeholder, inlineCodeSpans.get(i));
        }

        // Step 6: Restore fenced code blocks
        for (int i = 0; i < fencedCodeBlocks.size(); i++) {
            String placeholder = CODE_BLOCK_PLACEHOLDER + i + CODE_BLOCK_PLACEHOLDER_SUFFIX;
            stripped = stripped.replace(placeholder, fencedCodeBlocks.get(i));
        }

        return stripped;
    }

    /**
     * Check if text contains HTML tags outside code blocks (without stripping).
     *
     * <p>Use this to detect system prompt violations without modifying the text.
     *
     * <p><b>Note:</b> HTML tags inside code blocks are allowed and not counted.
     *
     * @param text text to check
     * @return true if text contains HTML tags outside code blocks
     */
    public static boolean containsHtml(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Remove code blocks temporarily to check for HTML in non-code content
        String textWithoutCode = text;

        // Remove fenced code blocks
        textWithoutCode = FENCED_CODE_BLOCK_PATTERN.matcher(textWithoutCode).replaceAll("");

        // Remove inline code spans
        textWithoutCode = INLINE_CODE_PATTERN.matcher(textWithoutCode).replaceAll("");

        // Check if remaining text has HTML tags
        return HTML_TAG_PATTERN.matcher(textWithoutCode).find();
    }

    /**
     * Strip HTML tags and log detailed info if found outside code blocks.
     *
     * <p>Use this during development/testing to identify which prompts
     * are causing the LLM to generate HTML.
     *
     * @param text text that may contain HTML tags
     * @param context context info for logging (e.g., "user prompt: X")
     * @return text with HTML tags removed (except in code blocks)
     */
    public static String stripHtmlWithLogging(String text, String context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        if (containsHtml(text)) {
            log.warning("HTML tags detected in AI response outside code blocks. Context: " + context);
            log.warning("Full response: " + text);
        }

        return stripHtml(text);
    }
}
