package com.cloudempiere.ai.util;

import java.util.ArrayList;
import java.util.List;

import java.util.Collections;

import org.commonmark.Extension;
// import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension; // TODO: Enable after mvn compile
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.compiere.util.CLogger;

/**
 * CommonMark-based markdown renderer with HTML preservation.
 *
 * <p>Provides full markdown support using the CommonMark Java library while
 * preserving pre-rendered HTML elements (tables and zoom links).
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Full CommonMark specification support</li>
 *   <li>GitHub Flavored Markdown (GFM) extensions</li>
 *   <li>Numbered and unordered lists with proper nesting</li>
 *   <li>Code blocks with syntax highlighting hooks</li>
 *   <li>Blockquotes, horizontal rules, and links</li>
 *   <li>Preserves pre-rendered HTML (tables, zoom links)</li>
 * </ul>
 *
 * <p><b>Architecture:</b>
 * <ol>
 *   <li>Extract pre-rendered HTML blocks (tables, links)</li>
 *   <li>Replace with placeholder tokens</li>
 *   <li>Parse markdown using CommonMark</li>
 *   <li>Render to HTML</li>
 *   <li>Restore pre-rendered HTML blocks</li>
 * </ol>
 *
 * <p><b>Usage:</b>
 * <pre>
 * // Text with pre-rendered HTML
 * String text = "# Results\n\n&lt;table&gt;...&lt;/table&gt;\n\n1. Item one\n2. Item two";
 *
 * // Render markdown while preserving HTML
 * String html = CommonMarkRenderer.render(text);
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 * @since v0.31.0 (ADR-047)
 * @see MarkdownRenderer
 * @see MarkdownTableRenderer
 */
public class CommonMarkRenderer {

    private static final CLogger log = CLogger.getCLogger(CommonMarkRenderer.class);

    /** Placeholder prefix for extracted HTML blocks */
    // BUG FIX (CLD-1704): Use {{}} instead of ___ to avoid markdown emphasis parsing
    // Triple underscores (___text___) are interpreted as bold+italic markdown!
    private static final String HTML_PLACEHOLDER_PREFIX = "{{HTML_BLOCK_";
    private static final String HTML_PLACEHOLDER_SUFFIX = "}}";

    /**
     * Represents an extracted HTML block with its placeholder.
     */
    private static class HtmlBlock {
        final String placeholder;
        final String html;

        HtmlBlock(int index, String html) {
            this.placeholder = HTML_PLACEHOLDER_PREFIX + index + HTML_PLACEHOLDER_SUFFIX;
            this.html = html;
        }
    }

    /**
     * Render markdown to HTML using CommonMark with HTML preservation.
     *
     * <p>Pre-rendered HTML (tables, zoom links) is preserved by extracting
     * it before CommonMark parsing and restoring it after rendering.
     *
     * @param markdown markdown text with optional pre-rendered HTML
     * @return fully rendered HTML
     */
    public static String render(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        try {
            // Step 1: Extract pre-rendered HTML blocks
            List<HtmlBlock> htmlBlocks = new ArrayList<>();
            String processed = extractHtmlBlocks(markdown, htmlBlocks);

            // Step 2: Parse markdown using CommonMark with GFM extensions
            Parser parser = createParser();
            Node document = parser.parse(processed);

            // Step 3: Render to HTML
            HtmlRenderer renderer = createRenderer();
            String html = renderer.render(document);

            // Step 4: Restore pre-rendered HTML blocks
            html = restoreHtmlBlocks(html, htmlBlocks);

            return html;

        } catch (Exception e) {
            log.warning("[COMMONMARK] Rendering error: " + e.getMessage());
            // Fallback to original text with basic HTML escaping
            return org.compiere.util.Util.maskHTML(markdown, true).replace("\n", "<br/>");
        }
    }

    /**
     * Create CommonMark parser with GitHub Flavored Markdown extensions.
     *
     * <p>Extensions included:
     * <ul>
     *   <li>Tables (pipe syntax)</li>
     *   <li>Strikethrough (~~text~~) - TODO: Enable after mvn compile</li>
     * </ul>
     *
     * @return configured parser
     */
    private static Parser createParser() {
        // TODO: Add StrikethroughExtension.create() after mvn compile downloads JARs
        List<Extension> extensions = Collections.singletonList(
            TablesExtension.create()
            // StrikethroughExtension.create()
        );
        return Parser.builder()
            .extensions(extensions)
            .build();
    }

    /**
     * Create CommonMark HTML renderer with custom options.
     *
     * <p>Configuration:
     * <ul>
     *   <li>Escape HTML: disabled (we control HTML via extraction)</li>
     *   <li>Soft breaks: converted to &lt;br&gt; tags</li>
     *   <li>Generate IDs: disabled (no need for heading anchors)</li>
     * </ul>
     *
     * @return configured renderer
     */
    private static HtmlRenderer createRenderer() {
        // TODO: Add StrikethroughExtension.create() after mvn compile downloads JARs
        List<Extension> extensions = Collections.singletonList(
            TablesExtension.create()
            // StrikethroughExtension.create()
        );
        return HtmlRenderer.builder()
            .extensions(extensions)
            .escapeHtml(false)  // We control HTML via extraction/restoration
            .softbreak("<br/>") // Convert soft breaks to line breaks
            .build();
    }

    /**
     * Extract HTML blocks and replace with placeholders.
     *
     * <p>Extracted elements:
     * <ul>
     *   <li>&lt;table&gt;...&lt;/table&gt; (pre-rendered tables with zoom links)</li>
     *   <li>&lt;a...&gt;...&lt;/a&gt; (zoom links outside tables)</li>
     * </ul>
     *
     * @param text text with embedded HTML
     * @param htmlBlocks list to store extracted blocks
     * @return text with placeholders
     */
    private static String extractHtmlBlocks(String text, List<HtmlBlock> htmlBlocks) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        int blockIndex = 0;

        while (pos < text.length()) {
            // Look for HTML tag start
            int tagStart = text.indexOf('<', pos);

            if (tagStart == -1) {
                // No more HTML - append remaining text
                result.append(text.substring(pos));
                break;
            }

            // Append text before HTML tag
            if (tagStart > pos) {
                result.append(text.substring(pos, tagStart));
            }

            // Determine tag type
            int tagEnd = text.indexOf('>', tagStart);
            if (tagEnd == -1) {
                // Incomplete tag - keep as-is
                result.append(text.substring(tagStart));
                break;
            }

            String tag = text.substring(tagStart, tagEnd + 1);
            String tagName = extractTagName(tag);

            // Only extract specific elements we want to preserve
            if (shouldPreserveTag(tagName)) {
                String closingTag = "</" + tagName + ">";
                int closingPos = findClosingTag(text, tagName, tagEnd + 1);

                if (closingPos != -1) {
                    // Extract entire element (tag + content + closing tag)
                    String htmlBlock = text.substring(tagStart, closingPos + closingTag.length());
                    HtmlBlock block = new HtmlBlock(blockIndex++, htmlBlock);
                    htmlBlocks.add(block);

                    // BUG FIX (CLD-1704): Add newlines around placeholder for block-level elements (tables)
                    // This prevents CommonMark from wrapping placeholder in <p> tags
                    // For inline elements (links), don't add newlines
                    if (isBlockLevelTag(tagName)) {
                        // Ensure placeholder is on its own line
                        // Check if previous char is newline
                        if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
                            result.append("\n\n");
                        }
                        result.append(block.placeholder);
                        result.append("\n\n");
                    } else {
                        // Inline element - no newlines
                        result.append(block.placeholder);
                    }

                    pos = closingPos + closingTag.length();
                    continue;
                }
            }

            // Not a preserved tag - keep as-is
            result.append(tag);
            pos = tagEnd + 1;
        }

        return result.toString();
    }

    /**
     * Check if HTML tag should be preserved (not parsed as markdown).
     *
     * @param tagName tag name (lowercase)
     * @return true if tag should be preserved
     */
    private static boolean shouldPreserveTag(String tagName) {
        if (tagName == null) return false;

        return tagName.equals("table") ||  // Pre-rendered tables
               tagName.equals("a");         // Zoom links
    }

    /**
     * Check if HTML tag is block-level (needs newlines around placeholder).
     *
     * @param tagName tag name (lowercase)
     * @return true if tag is block-level
     */
    private static boolean isBlockLevelTag(String tagName) {
        if (tagName == null) return false;

        return tagName.equals("table") ||   // Tables are block-level
               tagName.equals("div") ||
               tagName.equals("pre") ||
               tagName.equals("blockquote");
    }

    /**
     * Find closing tag for an HTML element.
     *
     * <p>Handles nested tags of the same type.
     *
     * @param text text to search
     * @param tagName tag name to find closing for
     * @param startPos position to start searching from
     * @return position of closing tag, or -1 if not found
     */
    private static int findClosingTag(String text, String tagName, int startPos) {
        String openingTag = "<" + tagName;
        String closingTag = "</" + tagName + ">";
        int depth = 1;
        int pos = startPos;

        while (pos < text.length() && depth > 0) {
            int nextOpening = text.indexOf(openingTag, pos);
            int nextClosing = text.indexOf(closingTag, pos);

            if (nextClosing == -1) {
                // No closing tag found
                return -1;
            }

            if (nextOpening != -1 && nextOpening < nextClosing) {
                // Nested opening tag
                depth++;
                pos = nextOpening + openingTag.length();
            } else {
                // Closing tag
                depth--;
                if (depth == 0) {
                    return nextClosing;
                }
                pos = nextClosing + closingTag.length();
            }
        }

        return -1;
    }

    /**
     * Extract tag name from HTML tag.
     *
     * @param tag full HTML tag (e.g., "&lt;div class='foo'&gt;")
     * @return tag name (e.g., "div"), or null if invalid
     */
    private static String extractTagName(String tag) {
        if (tag == null || tag.length() < 3) return null;

        String content = tag.substring(1, tag.length() - 1).trim();
        if (content.startsWith("/")) content = content.substring(1).trim();
        if (content.endsWith("/")) content = content.substring(0, content.length() - 1).trim();

        int spacePos = content.indexOf(' ');
        if (spacePos > 0) content = content.substring(0, spacePos);

        return content.toLowerCase();
    }

    /**
     * Restore pre-rendered HTML blocks from placeholders.
     *
     * <p><b>BUG FIX (CLD-1704):</b> CommonMark parser may wrap placeholders in paragraph tags
     * (e.g., `&lt;p&gt;___HTML_BLOCK_0___&lt;/p&gt;`), so we need to handle both bare placeholders
     * and wrapped ones. We also need to remove the wrapping paragraph tags to prevent
     * invalid HTML (can't have `&lt;table&gt;` inside `&lt;p&gt;`).
     *
     * @param html rendered HTML with placeholders
     * @param htmlBlocks extracted HTML blocks
     * @return HTML with restored blocks
     */
    private static String restoreHtmlBlocks(String html, List<HtmlBlock> htmlBlocks) {
        String result = html;

        for (HtmlBlock block : htmlBlocks) {
            // Try exact match first (placeholder not wrapped)
            if (result.contains(block.placeholder)) {
                result = result.replace(block.placeholder, block.html);
            } else {
                // Try to find placeholder wrapped in paragraph tags: <p>___HTML_BLOCK_N___</p>
                // We need to replace the entire <p>...</p> with just the HTML block
                String wrappedPattern = "<p>" + block.placeholder + "</p>";
                if (result.contains(wrappedPattern)) {
                    result = result.replace(wrappedPattern, block.html);
                } else {
                    // Try with newlines (CommonMark might add them)
                    String wrappedWithNewlines = "<p>" + block.placeholder + "\n</p>";
                    if (result.contains(wrappedWithNewlines)) {
                        result = result.replace(wrappedWithNewlines, block.html);
                    } else {
                        // Last resort: use regex to find placeholder anywhere (even with whitespace)
                        String regex = "<p>\\s*" + java.util.regex.Pattern.quote(block.placeholder) + "\\s*</p>";
                        result = result.replaceAll(regex, java.util.regex.Matcher.quoteReplacement(block.html));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Check if text contains markdown lists.
     *
     * @param text text to check
     * @return true if text contains list markers
     */
    public static boolean containsList(String text) {
        if (text == null || text.isEmpty()) return false;

        // Check for numbered list: "1. " at start of line
        if (text.matches("(?m)^\\d+\\.\\s+.*")) return true;

        // Check for unordered list: "- " or "* " at start of line
        if (text.matches("(?m)^[-*]\\s+.*")) return true;

        return false;
    }

    /**
     * Check if text contains code blocks.
     *
     * @param text text to check
     * @return true if text contains code fences
     */
    public static boolean containsCodeBlock(String text) {
        if (text == null || text.isEmpty()) return false;
        return text.contains("```");
    }
}
