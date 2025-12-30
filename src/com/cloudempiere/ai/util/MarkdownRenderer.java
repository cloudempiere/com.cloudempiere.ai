package com.cloudempiere.ai.util;

import java.util.Locale;
import java.util.Properties;

import org.compiere.util.Util;

/**
 * Unified markdown to HTML renderer for AI chat streaming.
 *
 * <p>Consolidates markdown transformation logic previously duplicated
 * across multiple methods in AIChatStreamingMessage.
 *
 * <p>Supports:
 * <ul>
 *   <li>Headings (h1-h4)</li>
 *   <li>Bold and italic text</li>
 *   <li>Inline code</li>
 *   <li>Lists</li>
 *   <li>Configurable HTML escaping</li>
 *   <li>Block element cleanup</li>
 *   <li>Optional table rendering</li>
 *   <li>Optional function call removal</li>
 * </ul>
 *
 * @since v0.31.0
 * @see com.cloudempiere.ai.component.AIChatStreamingMessage
 */
public class MarkdownRenderer {

    /**
     * Rendering options for markdown transformation.
     *
     * <p>Provides fine-grained control over rendering behavior for different contexts
     * (streaming vs final, with/without tables, etc.).
     */
    public static class RenderOptions {
        /** Remove function call XML blocks before rendering */
        public boolean removeFunctionCalls = false;

        /** Render tables using MarkdownTableRenderer */
        public boolean renderTables = false;

        /** Escape HTML before markdown transformation */
        public boolean escapeHtml = true;

        /** Block elements to remove trailing <br/> tags from */
        public String[] blockElementsToCleanup = new String[] {"h2", "h3", "h4", "li"};

        /** Locale for table number formatting (if renderTables is true) */
        public Locale locale = null;

        /** iDempiere context for zoom links in tables (if renderTables is true) */
        public Properties context = null;

        /** Parent widget ID for zoom event targeting (if renderTables is true) */
        public String parentWidgetId = null;

        /**
         * Create default render options (escape HTML, no tables, no function calls).
         */
        public RenderOptions() {
        }

        /**
         * Create render options with specified HTML escaping behavior.
         *
         * @param escapeHtml whether to escape HTML
         */
        public RenderOptions(boolean escapeHtml) {
            this.escapeHtml = escapeHtml;
        }
    }

    /**
     * Render markdown text to HTML with specified options.
     *
     * <p>This is the main entry point for markdown rendering.
     * It orchestrates the rendering pipeline based on provided options.
     *
     * @param text raw markdown text
     * @param options rendering options
     * @return HTML output
     */
    public static String render(String text, RenderOptions options) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;

        // Step 1: Remove function calls (optional)
        if (options.removeFunctionCalls) {
            result = removeFunctionCalls(result);
        }

        // Step 2: Render tables (optional, before HTML escaping)
        if (options.renderTables && MarkdownTableRenderer.containsTable(result)) {
            // Set table renderer context
            if (options.locale != null) {
                MarkdownTableRenderer.setLocale(options.locale);
            }
            if (options.context != null) {
                MarkdownTableRenderer.setContext(options.context);
            }
            if (options.parentWidgetId != null) {
                MarkdownTableRenderer.setWidgetId(options.parentWidgetId);
            }

            try {
                result = MarkdownTableRenderer.renderTables(result);
            } finally {
                // Always clear context
                if (options.locale != null) {
                    MarkdownTableRenderer.clearLocale();
                }
                if (options.context != null || options.parentWidgetId != null) {
                    MarkdownTableRenderer.clearZoomContext();
                }
            }
        }

        // Step 3: Escape HTML (optional)
        if (options.escapeHtml) {
            result = Util.maskHTML(result, true);
        }

        // Step 4: Apply markdown transformations
        result = applyMarkdownTransformations(result);

        // Step 5: Convert line breaks
        result = result.replace("\n", "<br/>");

        // Step 6: Cleanup block elements
        result = cleanupBlockElements(result, options.blockElementsToCleanup);

        return result;
    }

    /**
     * Apply markdown transformations (headings, bold, italic, code, lists).
     *
     * <p>This method applies regex-based transformations to convert
     * markdown syntax to HTML tags with appropriate styling.
     *
     * <p><b>Note:</b> This method should be called AFTER HTML escaping to prevent
     * unescaped HTML from interfering with markdown patterns.
     *
     * @param text text to transform (should already be HTML-escaped if needed)
     * @return transformed text with markdown converted to HTML
     */
    public static String applyMarkdownTransformations(String text) {
        String result = text;

        // Headings: # text, ## text, ### text (must be at start of line)
        // Process headings before line breaks to preserve newline matching
        result = result.replaceAll("(?m)^### (.+)$",
            "<h4 style='font-size: 14px; font-weight: 600; margin: 12px 0 8px 0;'>$1</h4>");
        result = result.replaceAll("(?m)^## (.+)$",
            "<h3 style='font-size: 15px; font-weight: 600; margin: 14px 0 8px 0;'>$1</h3>");
        result = result.replaceAll("(?m)^# (.+)$",
            "<h2 style='font-size: 16px; font-weight: 600; margin: 16px 0 10px 0;'>$1</h2>");

        // Bold: **text** or __text__
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        result = result.replaceAll("__(.+?)__", "<strong>$1</strong>");

        // Italic: *text* or _text_
        result = result.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "<em>$1</em>");
        result = result.replaceAll("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)", "<em>$1</em>");

        // Code: `text`
        result = result.replaceAll("`([^`]+)`",
            "<code style='background: #f5f5f5; padding: 2px 6px; border-radius: 3px; " +
            "font-family: monospace; font-size: 0.9em;'>$1</code>");

        // Lists: - item or * item (basic support)
        result = result.replaceAll("(?m)^- (.+)$",
            "<li style='margin-left: 16px; list-style-type: disc;'>$1</li>");
        result = result.replaceAll("(?m)^\\* (.+)$",
            "<li style='margin-left: 16px; list-style-type: disc;'>$1</li>");

        return result;
    }

    /**
     * Remove function call XML blocks.
     *
     * <p>Removes hallucinated function call blocks that may appear when
     * models without tool support generate tool-like syntax.
     *
     * @param text text to clean
     * @return cleaned text
     */
    public static String removeFunctionCalls(String text) {
        String result = text;
        // Remove complete function call blocks
        result = result.replaceAll("(?s)<function_calls>.*?</function_calls>", "");
        result = result.replaceAll("(?s)<function_result>.*?</function_result>", "");
        // Remove incomplete/partial tags that may appear during streaming
        result = result.replaceAll("(?s)<function_calls>.*$", "");
        result = result.replaceAll("(?s)<function_result>.*$", "");
        result = result.replaceAll("(?s)<invoke[^>]*>.*?</invoke>", "");
        result = result.replaceAll("(?s)<parameter[^>]*>.*?</parameter>", "");
        return result;
    }

    /**
     * Clean up extra <br/> after block elements.
     *
     * <p>Removes trailing <br/> tags that would create unwanted spacing
     * after block-level HTML elements like headings and lists.
     *
     * @param text text to clean
     * @param elements block element names (without angle brackets)
     * @return cleaned text
     */
    public static String cleanupBlockElements(String text, String[] elements) {
        String result = text;

        // Clean up specified elements
        for (String element : elements) {
            result = result.replaceAll("</" + element + "><br/>", "</" + element + ">");
        }

        // Always clean up table-related elements if present (regardless of options)
        result = result.replaceAll("</table><br/>", "</table>");
        result = result.replaceAll("</tr><br/>", "</tr>");
        result = result.replaceAll("</th><br/>", "</th>");
        result = result.replaceAll("</td><br/>", "</td>");

        return result;
    }
}
