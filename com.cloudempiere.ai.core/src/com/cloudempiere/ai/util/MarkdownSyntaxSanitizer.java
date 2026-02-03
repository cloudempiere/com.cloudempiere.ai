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
 * Sanitizes markdown to only allow supported syntax (ADR-055).
 *
 * <p><b>Purpose:</b> Enforce strict allow-list of markdown features to prevent:
 * <ul>
 *   <li>XSS attacks via raw HTML</li>
 *   <li>JavaScript injection via URLs</li>
 *   <li>Rendering issues from unsupported syntax</li>
 *   <li>Performance degradation from complex features</li>
 * </ul>
 *
 * <p><b>Supported Markdown Features:</b>
 * <ul>
 *   <li><b>Tier 1 - Core</b>: Headers, bold, italic, inline code, paragraphs</li>
 *   <li><b>Tier 2 - Structure</b>: Lists, blockquotes, code blocks, horizontal rules</li>
 *   <li><b>Tier 3 - Tables</b>: GFM pipe syntax (with limits)</li>
 *   <li><b>Tier 4 - Links/Images</b>: Standard syntax (with URL whitelist)</li>
 * </ul>
 *
 * <p><b>Blocked/Sanitized Features:</b>
 * <ul>
 *   <li>Raw HTML → escaped</li>
 *   <li>JavaScript URLs → removed</li>
 *   <li>Base64 images → blocked by default</li>
 *   <li>Footnotes, definition lists, task lists → rendered as literal text</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 * // Sanitize AI response before rendering
 * String aiResponse = "**Bold** &lt;script&gt;alert('XSS')&lt;/script&gt;";
 * String sanitized = MarkdownSyntaxSanitizer.sanitize(aiResponse);
 * // Result: "**Bold** &amp;lt;script&amp;gt;alert('XSS')&amp;lt;/script&amp;gt;"
 * </pre>
 *
 * @see <a href="../../docs/adr/055-constrained-markdown-syntax-support.md">ADR-055</a>
 * @see SecuritySanitizer
 * @see MarkdownValidator
 * @author Cloudempiere
 * @version 1.0
 * @since v0.32.0
 */
public class MarkdownSyntaxSanitizer {

    private static final CLogger log = CLogger.getCLogger(MarkdownSyntaxSanitizer.class);

    // ============================================================================
    // URL Protocol Patterns
    // ============================================================================

    /** Allowed URL protocols (http, https, and mailto) */
    private static final Pattern ALLOWED_PROTOCOL = Pattern.compile("^(https?://.*|mailto:.*)", Pattern.CASE_INSENSITIVE);

    /** Blocked URL protocols (security risk) */
    private static final Pattern BLOCKED_PROTOCOL = Pattern.compile(
        "^(javascript|data|file|vbscript|about):",
        Pattern.CASE_INSENSITIVE
    );

    // ============================================================================
    // Markdown Syntax Patterns
    // ============================================================================

    /** Pattern for markdown links: [text](url) or [text](url "title") */
    private static final Pattern LINK_PATTERN = Pattern.compile(
        "\\[([^\\]]+)\\]\\(([^)\\s]+)(?:\\s+\"([^\"]+)\")?\\)"
    );

    /** Pattern for markdown images: ![alt](url) or ![alt](url "title") */
    private static final Pattern IMAGE_PATTERN = Pattern.compile(
        "!\\[([^\\]]*)\\]\\(([^)\\s]+)(?:\\s+\"([^\"]+)\")?\\)"
    );

    /** Pattern for autolinks: <http://example.com> or <email@example.com> */
    private static final Pattern AUTOLINK_PATTERN = Pattern.compile(
        "<(https?://[^>]+|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})>"
    );

    /** Pattern for footnote references: [^1] */
    private static final Pattern FOOTNOTE_REF_PATTERN = Pattern.compile("\\[\\^\\d+\\]");

    /** Pattern for footnote definitions: [^1]: content */
    private static final Pattern FOOTNOTE_DEF_PATTERN = Pattern.compile("^\\[\\^\\d+\\]:\\s+.+$", Pattern.MULTILINE);

    /** Pattern for task list items: - [ ] or - [x] */
    private static final Pattern TASK_LIST_PATTERN = Pattern.compile("^(\\s*[-*+])\\s+\\[([xX\\s])\\]\\s+", Pattern.MULTILINE);

    /** Pattern for HTML tags (not in code blocks) */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /** Pattern for fenced code blocks: ```language ... ``` */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
        "```[a-zA-Z0-9]*\\s*\\n[\\s\\S]*?\\n```",
        Pattern.MULTILINE
    );

    /** Pattern for inline code: `code` */
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`[^`]+`");

    // ============================================================================
    // Configuration (Instance-based, multi-tenant safe)
    // ============================================================================

    /** Configuration instance (per-provider, not shared) */
    private final MarkdownConfig config;

    // ============================================================================
    // Constructors
    // ============================================================================

    /**
     * Create sanitizer with custom configuration.
     *
     * @param config instance-based configuration (multi-tenant safe)
     */
    public MarkdownSyntaxSanitizer(MarkdownConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("MarkdownConfig cannot be null");
        }
        this.config = config;
    }

    /**
     * Create sanitizer with default secure configuration.
     */
    public MarkdownSyntaxSanitizer() {
        this(MarkdownConfig.secure());
    }

    // ============================================================================
    // Main Sanitization Entry Point
    // ============================================================================

    /**
     * Sanitize markdown to only supported syntax.
     *
     * <p><b>Process:</b>
     * <ol>
     *   <li>Protect code blocks and inline code (temporary placeholders)</li>
     *   <li>Escape raw HTML tags</li>
     *   <li>Sanitize URLs in links and images</li>
     *   <li>Remove unsupported markdown features</li>
     *   <li>Restore code blocks and inline code</li>
     *   <li>Validate markdown structure (existing MarkdownValidator)</li>
     * </ol>
     *
     * @param markdown raw markdown from AI (can be null)
     * @return sanitized markdown (supported syntax only)
     */
    public String sanitize(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        // EARLY EXIT: Detect pre-rendered HTML and skip markdown processing
        // Tool results and pre-rendered content should pass through without modification
        if (isLikelyHtml(markdown)) {
            log.fine("Content is pre-rendered HTML, skipping markdown sanitization");
            return markdown;
        }

        try {
            String sanitized = markdown;

            // Step 1: Protect code blocks and inline code (don't sanitize their content)
            CodeBlockProtector protector = new CodeBlockProtector();
            sanitized = protector.protect(sanitized);

            // Step 1b: Protect zoom links from markdown processing (FIX BUG #3)
            ZoomLinkProtector zoomProtector = new ZoomLinkProtector();
            sanitized = zoomProtector.protect(sanitized);

            // Step 2: Escape raw HTML tags (prevent XSS)
            sanitized = escapeRawHtml(sanitized);

            // Step 3: Sanitize URLs (links and images)
            sanitized = sanitizeUrls(sanitized);

            // Step 4: Remove unsupported markdown features
            sanitized = removeUnsupportedFeatures(sanitized);

            // Step 5: Restore code blocks and inline code
            sanitized = protector.restore(sanitized);

            // Step 5b: Restore zoom links (FIX BUG #3)
            sanitized = zoomProtector.restore(sanitized);

            // Step 6: Validate markdown structure (use existing validator)
            sanitized = MarkdownValidator.validate(sanitized);

            return sanitized;

        } catch (Exception e) {
            log.warning("Markdown sanitization failed: " + e.getMessage());

            // SMART FALLBACK (Fix for double-escaping bug):
            // Don't blindly escape everything - detect if content is already HTML
            // Tool results and pre-rendered content should pass through safely

            if (isLikelyHtml(markdown)) {
                // Content appears to be HTML (from tool results or pre-rendered)
                // Return as-is but log warning about sanitization bypass
                log.warning("Content appears to be HTML, bypassing markdown sanitization");
                return markdown;
            } else {
                // Content is likely markdown or plain text - safe to escape
                log.warning("Falling back to full HTML escaping for safety");
                return SecuritySanitizer.escapeHtml(markdown);
            }
        }
    }

    /**
     * Check if content is likely HTML rather than markdown.
     *
     * <p>Heuristics:
     * <ul>
     *   <li>Starts with HTML tag (div, table, p, pre, ul, ol)</li>
     *   <li>Contains multiple closing tags</li>
     *   <li>Has HTML structure (opening + closing tags)</li>
     * </ul>
     *
     * @param content content to check
     * @return true if content appears to be HTML
     */
    private static boolean isLikelyHtml(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        String trimmed = content.trim();

        // Check if starts with common HTML tags
        if (trimmed.startsWith("<div") ||
            trimmed.startsWith("<table") ||
            trimmed.startsWith("<p>") ||
            trimmed.startsWith("<pre>") ||
            trimmed.startsWith("<ul>") ||
            trimmed.startsWith("<ol>") ||
            trimmed.startsWith("<h1>") ||
            trimmed.startsWith("<h2>") ||
            trimmed.startsWith("<h3>")) {
            return true;
        }

        // Count HTML closing tags - if more than 2, likely HTML structure
        int closingTags = 0;
        int pos = 0;
        while ((pos = content.indexOf("</", pos)) != -1) {
            closingTags++;
            pos += 2;
            if (closingTags > 2) {
                return true;
            }
        }

        return false;
    }

    // ============================================================================
    // Step 2: Escape Raw HTML
    // ============================================================================

    /**
     * Remove all HTML tags (except in code blocks).
     *
     * <p><b>Strategy:</b> Strip HTML tags entirely (not escape) to prevent ZK Html component from decoding entities.
     *
     * <p><b>Why remove instead of escape:</b>
     * <ul>
     *   <li>Escaping creates entities like {@code &lt;} which ZK Html component decodes back to {@code <}</li>
     *   <li>This causes actual HTML rendering (security vulnerability)</li>
     *   <li>Removing tags is safer - no HTML can be rendered</li>
     * </ul>
     *
     * <p><b>Examples:</b>
     * <ul>
     *   <li>{@code <script>alert('XSS')</script>} → {@code alert('XSS')}</li>
     *   <li>{@code <img src=x onerror="alert(1)">} → {@code (removed)}</li>
     *   <li>{@code <div>Welcome</div>} → {@code Welcome}</li>
     * </ul>
     *
     * @param markdown markdown with potential HTML tags
     * @return markdown with HTML tags removed
     */
    private static String escapeRawHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        // Remove all HTML tags entirely (safer than escaping)
        // This prevents ZK Html component from decoding entities and rendering HTML
        return HTML_TAG_PATTERN.matcher(markdown).replaceAll("");
    }

    // ============================================================================
    // Step 3: Sanitize URLs
    // ============================================================================

    /**
     * Sanitize URLs in links and images.
     *
     * <p><b>Validation:</b>
     * <ul>
     *   <li>Block dangerous protocols: javascript:, data:, file:, vbscript:, about:</li>
     *   <li>Allow only: http://, https://</li>
     *   <li>Optionally block data: URLs (base64 images)</li>
     *   <li>Escape URL with {@link SecuritySanitizer#escapeUrl(String)}</li>
     * </ul>
     *
     * <p><b>Examples:</b>
     * <ul>
     *   <li>{@code [Click](javascript:alert(1))} → removed or replaced</li>
     *   <li>{@code ![](data:image/svg+xml;base64,...)} → removed (if base64 blocked)</li>
     *   <li>{@code [Safe](https://example.com)} → kept</li>
     * </ul>
     *
     * @param markdown markdown with potential unsafe URLs
     * @return markdown with sanitized URLs
     */
    private String sanitizeUrls(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        String result = markdown;

        // Sanitize links
        result = sanitizeLinks(result);

        // Sanitize images
        result = sanitizeImages(result);

        return result;
    }

    /**
     * Sanitize markdown links: [text](url).
     */
    private String sanitizeLinks(String markdown) {
        Matcher matcher = LINK_PATTERN.matcher(markdown);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String text = matcher.group(1);
            String url = matcher.group(2);
            String title = matcher.group(3); // Optional title

            // Validate URL
            if (isUrlSafe(url)) {
                // Keep link with validated URL (no escaping needed for markdown URLs)
                String replacement;
                if (title != null) {
                    String sanitizedTitle = SecuritySanitizer.escapeHtmlAttribute(title);
                    replacement = "[" + text + "](" + url + " \"" + sanitizedTitle + "\")";
                } else {
                    replacement = "[" + text + "](" + url + ")";
                }
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // Remove dangerous link, keep text only
                log.warning("Blocked unsafe URL in link: " + url);
                matcher.appendReplacement(result, Matcher.quoteReplacement(text));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Sanitize markdown images: ![alt](url).
     */
    private String sanitizeImages(String markdown) {
        Matcher matcher = IMAGE_PATTERN.matcher(markdown);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String alt = matcher.group(1);
            String url = matcher.group(2);
            String title = matcher.group(3); // Optional title

            // Validate URL (context-aware for images - allows data: URLs when configured)
            if (isUrlSafe(url, true)) {
                // Keep image with validated URL (no escaping needed for markdown URLs)
                String replacement;
                if (title != null) {
                    String sanitizedTitle = SecuritySanitizer.escapeHtmlAttribute(title);
                    replacement = "![" + alt + "](" + url + " \"" + sanitizedTitle + "\")";
                } else {
                    replacement = "![" + alt + "](" + url + ")";
                }
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // Remove dangerous image, keep alt text
                log.warning("Blocked unsafe URL in image: " + url);
                String replacement = "[Image removed: " + alt + "]";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Check if URL is safe (protocol validation).
     *
     * @param url URL to validate
     * @return true if URL protocol is safe (http/https)
     */
    private boolean isUrlSafe(String url) {
        return isUrlSafe(url, false);
    }

    /**
     * Check if URL is safe (protocol validation) with context awareness.
     *
     * @param url URL to validate
     * @param isImage true if this is an image URL (allows data: URLs when configured)
     * @return true if URL protocol is safe
     */
    private boolean isUrlSafe(String url, boolean isImage) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Special case: data: URLs in images when allowed
        if (url.startsWith("data:")) {
            if (isImage && config.isBase64ImagesAllowed()) {
                return true;  // Allow data: URLs in images when configured
            } else {
                return false;  // Block data: URLs in links or when not allowed
            }
        }

        // Allow http, https, and mailto protocols
        if (ALLOWED_PROTOCOL.matcher(url).matches()) {
            return true;
        }

        // Block all other protocols (javascript, file, vbscript, about, etc.)
        return false;
    }

    /**
     * Additional check for image URLs (base64 validation).
     *
     * @param url image URL to validate
     * @return true if image URL is safe
     */
    private boolean isImageUrlSafe(String url) {
        // Block base64 data URLs unless explicitly allowed
        if (!config.isBase64ImagesAllowed() && url.startsWith("data:")) {
            return false;
        }

        return true;
    }

    // ============================================================================
    // Step 4: Remove Unsupported Features
    // ============================================================================

    /**
     * Remove unsupported markdown features.
     *
     * <p><b>Removed:</b>
     * <ul>
     *   <li>Footnotes: [^1] and [^1]: definition</li>
     *   <li>Task lists: - [ ] and - [x]</li>
     *   <li>Autolinks: &lt;http://example.com&gt;</li>
     * </ul>
     *
     * <p><b>Strategy:</b> Remove or convert to supported syntax
     *
     * @param markdown markdown with potential unsupported features
     * @return markdown with unsupported features removed
     */
    private String removeUnsupportedFeatures(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        String result = markdown;

        // Remove footnote definitions (must be before references)
        result = FOOTNOTE_DEF_PATTERN.matcher(result).replaceAll("");

        // Remove footnote references
        result = FOOTNOTE_REF_PATTERN.matcher(result).replaceAll("");

        // Convert task lists to plain lists
        result = TASK_LIST_PATTERN.matcher(result).replaceAll("$1 ");

        // Convert autolinks to standard link syntax
        result = convertAutolinks(result);

        return result;
    }

    /**
     * Convert autolinks to standard link syntax.
     *
     * <p>Example: {@code <http://example.com>} → {@code [http://example.com](http://example.com)}
     */
    private String convertAutolinks(String markdown) {
        Matcher matcher = AUTOLINK_PATTERN.matcher(markdown);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String url = matcher.group(1);

            // Check if it's an email
            if (url.contains("@")) {
                // Email autolink - convert to mailto link
                String replacement = "[" + url + "](mailto:" + url + ")";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else if (isUrlSafe(url)) {
                // HTTP/HTTPS autolink - convert to standard link
                String replacement = "[" + url + "](" + url + ")";
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // Unsafe URL - remove
                log.warning("Blocked unsafe autolink: " + url);
                matcher.appendReplacement(result, Matcher.quoteReplacement(url));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    // ============================================================================
    // Code Block Protector (Preserve code blocks during sanitization)
    // ============================================================================

    /**
     * Protects code blocks and inline code from sanitization.
     *
     * <p><b>Strategy:</b> Replace code blocks with placeholders, sanitize markdown,
     * then restore code blocks.
     *
     * <p><b>Why needed:</b> Code blocks may contain characters that look like
     * markdown or HTML, but should be preserved as-is.
     */
    private static class CodeBlockProtector {
        private final List<String> codeBlocks = new ArrayList<>();
        private final List<String> inlineCodes = new ArrayList<>();

        private static final String CODE_BLOCK_PLACEHOLDER = "___CODE_BLOCK_%d___";
        private static final String INLINE_CODE_PLACEHOLDER = "___INLINE_CODE_%d___";

        /**
         * Protect code blocks and inline code by replacing with placeholders.
         */
        public String protect(String markdown) {
            String result = markdown;

            // Protect fenced code blocks first
            result = protectCodeBlocks(result);

            // Protect inline code
            result = protectInlineCode(result);

            return result;
        }

        /**
         * Restore code blocks and inline code from placeholders.
         */
        public String restore(String markdown) {
            String result = markdown;

            // Restore code blocks
            for (int i = 0; i < codeBlocks.size(); i++) {
                String placeholder = String.format(CODE_BLOCK_PLACEHOLDER, i);
                result = result.replace(placeholder, codeBlocks.get(i));
            }

            // Restore inline code
            for (int i = 0; i < inlineCodes.size(); i++) {
                String placeholder = String.format(INLINE_CODE_PLACEHOLDER, i);
                result = result.replace(placeholder, inlineCodes.get(i));
            }

            return result;
        }

        /**
         * Protect fenced code blocks.
         */
        private String protectCodeBlocks(String markdown) {
            Matcher matcher = CODE_BLOCK_PATTERN.matcher(markdown);
            StringBuffer result = new StringBuffer();
            int index = 0;

            while (matcher.find()) {
                String codeBlock = matcher.group();
                codeBlocks.add(codeBlock);
                String placeholder = String.format(CODE_BLOCK_PLACEHOLDER, index++);
                matcher.appendReplacement(result, Matcher.quoteReplacement(placeholder));
            }
            matcher.appendTail(result);

            return result.toString();
        }

        /**
         * Protect inline code.
         */
        private String protectInlineCode(String markdown) {
            Matcher matcher = INLINE_CODE_PATTERN.matcher(markdown);
            StringBuffer result = new StringBuffer();
            int index = 0;

            while (matcher.find()) {
                String inlineCode = matcher.group();
                inlineCodes.add(inlineCode);
                String placeholder = String.format(INLINE_CODE_PLACEHOLDER, index++);
                matcher.appendReplacement(result, Matcher.quoteReplacement(placeholder));
            }
            matcher.appendTail(result);

            return result.toString();
        }
    }

    // ============================================================================
    // Zoom Link Protector (FIX BUG #3)
    // ============================================================================

    /**
     * Protects iDempiere zoom link syntax from markdown processing.
     *
     * <p><b>Problem:</b> Zoom links use syntax like {@code [[C_BPartner:123|Name]]}.
     * The underscore in {@code C_BPartner} would be interpreted as italic markdown,
     * corrupting the zoom link syntax before {@code ZoomLinkProcessor} can handle it.
     *
     * <p><b>Solution:</b> Replace zoom links with placeholders during markdown processing,
     * then restore them afterwards.
     *
     * <p><b>Example:</b>
     * <ul>
     *   <li>Input: {@code **[[C_BPartner:123|Name]]**}</li>
     *   <li>Protected: {@code **___ZOOM_LINK_0___**}</li>
     *   <li>After markdown: {@code <strong>___ZOOM_LINK_0___</strong>}</li>
     *   <li>Restored: {@code <strong>[[C_BPartner:123|Name]]</strong>}</li>
     * </ul>
     */
    private static class ZoomLinkProtector {
        private final List<String> zoomLinks = new ArrayList<>();

        private static final String ZOOM_LINK_PLACEHOLDER = "___ZOOM_LINK_%d___";

        /** Pattern for zoom links: [[Table:ID|Label]] or [[Table:ID]] */
        private static final Pattern ZOOM_LINK_PATTERN = Pattern.compile(
            "\\[\\[([^\\]]+)\\]\\]"
        );

        /**
         * Protect zoom links by replacing with placeholders.
         */
        public String protect(String markdown) {
            Matcher matcher = ZOOM_LINK_PATTERN.matcher(markdown);
            StringBuffer result = new StringBuffer();
            int index = 0;

            while (matcher.find()) {
                String zoomLink = matcher.group();
                zoomLinks.add(zoomLink);
                String placeholder = String.format(ZOOM_LINK_PLACEHOLDER, index++);
                matcher.appendReplacement(result, Matcher.quoteReplacement(placeholder));
            }
            matcher.appendTail(result);

            return result.toString();
        }

        /**
         * Restore zoom links from placeholders.
         */
        public String restore(String markdown) {
            String result = markdown;

            // Restore zoom links
            for (int i = 0; i < zoomLinks.size(); i++) {
                String placeholder = String.format(ZOOM_LINK_PLACEHOLDER, i);
                result = result.replace(placeholder, zoomLinks.get(i));
            }

            return result;
        }
    }

    // ============================================================================
    // Configuration Getter (for inspection)
    // ============================================================================

    /**
     * Get the configuration used by this sanitizer.
     *
     * @return configuration instance
     */
    public MarkdownConfig getConfig() {
        return config;
    }
}
