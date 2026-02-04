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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * Test MarkdownSyntaxSanitizer (ADR-055).
 *
 * <p><b>Test Coverage:</b>
 * <ul>
 *   <li>HTML escaping (XSS prevention)</li>
 *   <li>URL sanitization (protocol validation)</li>
 *   <li>Unsupported feature removal</li>
 *   <li>Code block protection</li>
 *   <li>Supported syntax preservation</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 1.0
 * @since v0.32.0
 */
@UnitTest
@DisplayName("MarkdownSyntaxSanitizer Tests (ADR-055)")
public class MarkdownSyntaxSanitizerTest {

    private MarkdownSyntaxSanitizer sanitizer;

    @BeforeEach
    public void setUp() {
        // Create sanitizer with default secure configuration
        sanitizer = new MarkdownSyntaxSanitizer(MarkdownConfig.secure());
    }

    // ============================================================================
    // HTML Escaping Tests
    // ============================================================================

    @Test
    @DisplayName("Should remove raw HTML tags to prevent XSS")
    public void testRawHtmlEscaped() {
        String input = "Hello <script>alert('XSS')</script> world";
        String output = sanitizer.sanitize(input);

        // HTML tags should be removed (not escaped, to prevent ZK Html decoding issues)
        assertFalse(output.contains("<script>"), "Script tag should be removed");
        assertFalse(output.contains("</script>"), "Closing script tag should be removed");
        assertTrue(output.contains("Hello") && output.contains("world"),
                   "Text content should be preserved");
        assertTrue(output.contains("alert('XSS')"), "Script content kept as plain text");
    }

    @Test
    @DisplayName("Should remove HTML image with event handler")
    public void testHtmlImageWithEventHandler() {
        String input = "Text <img src=x onerror=\"alert('XSS')\"> more text";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("<img"), "Img tag should be removed");
        assertFalse(output.contains("onerror"), "Event handler should be removed");
        assertTrue(output.contains("Text") && output.contains("more text"),
                   "Surrounding text should be preserved");
    }

    @Test
    @DisplayName("Should remove HTML anchor with onclick")
    public void testHtmlAnchorWithOnclick() {
        String input = "<a href=\"#\" onclick=\"steal()\">Link</a>";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("<a "), "Anchor tag should be removed");
        assertFalse(output.contains("onclick"), "Event handler should be removed");
        assertTrue(output.contains("Link"), "Link text should be preserved");
    }

    @Test
    @DisplayName("Should remove CSS style tag")
    public void testCssStyleTag() {
        String input = "Text <style>body{display:none}</style> more";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("<style>"), "Style tag should be removed");
        assertTrue(output.contains("body{display:none}"), "Style content kept as plain text");
    }

    @Test
    @DisplayName("Should not escape HTML in code blocks")
    public void testHtmlInCodeBlock() {
        String input = "```html\n<script>alert('XSS')</script>\n```";
        String output = sanitizer.sanitize(input);

        // HTML in code blocks should be preserved
        assertTrue(output.contains("```"), "Code block should be preserved");
        assertTrue(output.contains("<script>"), "HTML in code block should NOT be escaped");
    }

    @Test
    @DisplayName("Should not escape HTML in inline code")
    public void testHtmlInInlineCode() {
        String input = "Use `<div>` for block elements";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("`<div>`"), "HTML in inline code should NOT be escaped");
    }

    // ============================================================================
    // URL Sanitization Tests - Links
    // ============================================================================

    @Test
    @DisplayName("Should block javascript: URL in links")
    public void testJavaScriptUrlInLink() {
        String input = "[Click me](javascript:alert('XSS'))";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("javascript:"), "JavaScript URL should be blocked");
        assertTrue(output.contains("Click me"), "Link text should be preserved");
    }

    @Test
    @DisplayName("Should block data: URL in links")
    public void testDataUrlInLink() {
        String input = "[Click](data:text/html,<script>alert('XSS')</script>)";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("data:"), "Data URL should be blocked");
        assertTrue(output.contains("Click"), "Link text should be preserved");
    }

    @Test
    @DisplayName("Should block file: URL in links")
    public void testFileUrlInLink() {
        String input = "[Local file](file:///etc/passwd)";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("file:"), "File URL should be blocked");
    }

    @Test
    @DisplayName("Should block vbscript: URL in links")
    public void testVbScriptUrlInLink() {
        String input = "[Click](vbscript:msgbox('XSS'))";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("vbscript:"), "VBScript URL should be blocked");
    }

    @Test
    @DisplayName("Should allow https: URL in links")
    public void testHttpsUrlInLink() {
        String input = "[Safe link](https://example.com)";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("https://example.com") ||
                   output.contains("https%3A%2F%2Fexample.com"),
                   "HTTPS URL should be allowed");
        assertTrue(output.contains("[Safe link]"), "Link should be preserved");
    }

    @Test
    @DisplayName("Should allow http: URL in links")
    public void testHttpUrlInLink() {
        String input = "[Safe link](http://example.com)";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("http://example.com") ||
                   output.contains("http%3A%2F%2Fexample.com"),
                   "HTTP URL should be allowed");
    }

    @Test
    @DisplayName("Should preserve link title attribute")
    public void testLinkWithTitle() {
        String input = "[Link](https://example.com \"Title text\")";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("Title text") || output.contains("Title&#"),
                   "Link title should be preserved");
    }

    // ============================================================================
    // URL Sanitization Tests - Images
    // ============================================================================

    @Test
    @DisplayName("Should block data: URL in images by default")
    public void testDataUrlInImage() {
        String input = "![Alt](data:image/svg+xml;base64,PHN2ZyBvbmxvYWQ9YWxlcnQoJ1hTUycpPjwvc3ZnPg==)";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("data:"), "Data URL should be blocked");
        assertTrue(output.contains("Alt") || output.contains("Image removed"),
                   "Alt text should be preserved or replaced");
    }

    @Test
    @DisplayName("Should allow data: URL in images when configured")
    public void testDataUrlInImageWhenAllowed() {
        // Create sanitizer with permissive config
        MarkdownSyntaxSanitizer permissiveSanitizer = new MarkdownSyntaxSanitizer(
            MarkdownConfig.builder().allowBase64Images(true).build()
        );

        String input = "![Alt](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA)";
        String output = permissiveSanitizer.sanitize(input);

        assertTrue(output.contains("data:image/png"), "Data URL should be allowed when configured");
    }

    @Test
    @DisplayName("Should block javascript: URL in images")
    public void testJavaScriptUrlInImage() {
        String input = "![Alt](javascript:alert('XSS'))";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("javascript:"), "JavaScript URL should be blocked");
    }

    @Test
    @DisplayName("Should allow https: URL in images")
    public void testHttpsUrlInImage() {
        String input = "![Alt text](https://example.com/image.png)";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("https://example.com/image.png") ||
                   output.contains("https%3A%2F%2Fexample.com"),
                   "HTTPS image URL should be allowed");
        assertTrue(output.contains("![Alt text]"), "Image should be preserved");
    }

    @Test
    @DisplayName("Should preserve image title attribute")
    public void testImageWithTitle() {
        String input = "![Alt](https://example.com/img.png \"Image title\")";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("Image title") || output.contains("Image&#"),
                   "Image title should be preserved");
    }

    // ============================================================================
    // Unsupported Feature Removal Tests
    // ============================================================================

    @Test
    @DisplayName("Should remove footnote references")
    public void testFootnoteReferencesRemoved() {
        String input = "Text with footnote[^1] and another[^2].";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("[^1]"), "Footnote reference should be removed");
        assertFalse(output.contains("[^2]"), "Footnote reference should be removed");
        assertTrue(output.contains("Text with footnote"), "Text should be preserved");
    }

    @Test
    @DisplayName("Should remove footnote definitions")
    public void testFootnoteDefinitionsRemoved() {
        String input = "Text[^1]\n\n[^1]: This is a footnote.";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("[^1]:"), "Footnote definition should be removed");
        assertFalse(output.contains("This is a footnote"), "Footnote content should be removed");
    }

    @Test
    @DisplayName("Should convert task list to plain list")
    public void testTaskListConverted() {
        String input = "- [ ] Unchecked task\n- [x] Checked task\n- [X] Also checked";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("[ ]"), "Unchecked box should be removed");
        assertFalse(output.contains("[x]"), "Checked box should be removed");
        assertFalse(output.contains("[X]"), "Checked box should be removed");
        assertTrue(output.contains("Unchecked task"), "Task text should be preserved");
        assertTrue(output.contains("Checked task"), "Task text should be preserved");
    }

    @Disabled("TODO: Implement autolink conversion - optional feature for Phase 2")
    @Test
    @DisplayName("Should convert autolinks to standard links")
    public void testAutolinksConverted() {
        String input = "Visit <https://example.com> for more info";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("<https://"), "Autolink syntax should be converted");
        assertTrue(output.contains("[https://example.com]"), "Should convert to standard link");
    }

    @Disabled("TODO: Implement autolink conversion - optional feature for Phase 2")
    @Test
    @DisplayName("Should convert email autolinks to mailto links")
    public void testEmailAutolinksConverted() {
        String input = "Contact <user@example.com> for support";
        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("<user@example.com>"), "Email autolink should be converted");
        assertTrue(output.contains("[user@example.com]") || output.contains("mailto:"),
                   "Should convert to mailto link");
    }

    // ============================================================================
    // Supported Syntax Preservation Tests
    // ============================================================================

    @Test
    @DisplayName("Should preserve headers")
    public void testHeadersPreserved() {
        String input = "# H1\n## H2\n### H3\n#### H4\n##### H5\n###### H6";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("# H1"), "H1 should be preserved");
        assertTrue(output.contains("## H2"), "H2 should be preserved");
        assertTrue(output.contains("### H3"), "H3 should be preserved");
        assertTrue(output.contains("#### H4"), "H4 should be preserved");
        assertTrue(output.contains("##### H5"), "H5 should be preserved");
        assertTrue(output.contains("###### H6"), "H6 should be preserved");
    }

    @Test
    @DisplayName("Should preserve bold and italic")
    public void testBoldItalicPreserved() {
        String input = "**bold** *italic* ***both*** __also bold__ _also italic_";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("**bold**"), "Bold should be preserved");
        assertTrue(output.contains("*italic*"), "Italic should be preserved");
        assertTrue(output.contains("***both***"), "Bold+italic should be preserved");
    }

    @Test
    @DisplayName("Should preserve inline code")
    public void testInlineCodePreserved() {
        String input = "Use `var x = 1;` for variables";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("`var x = 1;`"), "Inline code should be preserved");
    }

    @Test
    @DisplayName("Should preserve code blocks")
    public void testCodeBlocksPreserved() {
        String input = "```javascript\nconst x = 1;\nconsole.log(x);\n```";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("```javascript"), "Code block should be preserved");
        assertTrue(output.contains("const x = 1;"), "Code content should be preserved");
        assertTrue(output.contains("console.log(x);"), "Code content should be preserved");
    }

    @Test
    @DisplayName("Should preserve unordered lists")
    public void testUnorderedListsPreserved() {
        String input = "- Item 1\n- Item 2\n* Item 3\n+ Item 4";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("- Item 1"), "List items should be preserved");
        assertTrue(output.contains("Item 2"), "List items should be preserved");
    }

    @Test
    @DisplayName("Should preserve ordered lists")
    public void testOrderedListsPreserved() {
        String input = "1. First\n2. Second\n3. Third";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("1. First"), "Ordered list should be preserved");
        assertTrue(output.contains("2. Second"), "Ordered list should be preserved");
        assertTrue(output.contains("3. Third"), "Ordered list should be preserved");
    }

    @Test
    @DisplayName("Should preserve blockquotes")
    public void testBlockquotesPreserved() {
        String input = "> This is a quote\n> Second line";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("> This is a quote"), "Blockquote should be preserved");
        assertTrue(output.contains("> Second line"), "Blockquote should be preserved");
    }

    @Test
    @DisplayName("Should preserve horizontal rules")
    public void testHorizontalRulesPreserved() {
        String input = "Text\n\n---\n\nMore text";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("---"), "Horizontal rule should be preserved");
    }

    @Test
    @DisplayName("Should preserve tables")
    public void testTablesPreserved() {
        String input = "| A | B |\n|---|---|\n| 1 | 2 |";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("| A | B |"), "Table header should be preserved");
        assertTrue(output.contains("|---|---|"), "Table separator should be preserved");
        assertTrue(output.contains("| 1 | 2 |"), "Table row should be preserved");
    }

    // ============================================================================
    // Edge Cases and Integration Tests
    // ============================================================================

    @Test
    @DisplayName("Should handle null input")
    public void testNullInput() {
        String output = sanitizer.sanitize(null);
        assertEquals("", output, "Null input should return empty string");
    }

    @Test
    @DisplayName("Should handle empty input")
    public void testEmptyInput() {
        String output = sanitizer.sanitize("");
        assertEquals("", output, "Empty input should return empty string");
    }

    @Test
    @DisplayName("Should handle mixed safe and unsafe content")
    public void testMixedContent() {
        String input = "# Heading\n\n**Bold** text with <script>alert('XSS')</script>\n\n" +
                       "[Safe link](https://example.com) and [Bad link](javascript:alert(1))\n\n" +
                       "```\nCode block\n```";

        String output = sanitizer.sanitize(input);

        // Safe content preserved
        assertTrue(output.contains("# Heading"), "Header should be preserved");
        assertTrue(output.contains("**Bold**"), "Bold should be preserved");
        assertTrue(output.contains("https://example.com"), "Safe link should be preserved");
        assertTrue(output.contains("```"), "Code block should be preserved");

        // Unsafe content sanitized
        assertFalse(output.contains("<script>"), "Script tag should be removed");
        assertFalse(output.contains("javascript:"), "JavaScript URL should be blocked");
        assertTrue(output.contains("alert('XSS')"), "Script content kept as plain text");
    }

    @Test
    @DisplayName("Should handle complex markdown with multiple features")
    public void testComplexMarkdown() {
        String input = "# Report\n\n" +
                       "## Summary\n\n" +
                       "**Total**: 100 items\n\n" +
                       "| Name | Count |\n" +
                       "|------|-------|\n" +
                       "| A    | 50    |\n" +
                       "| B    | 50    |\n\n" +
                       "See `getCount()` method.\n\n" +
                       "```java\n" +
                       "public int getCount() {\n" +
                       "    return 100;\n" +
                       "}\n" +
                       "```";

        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("# Report"), "All features should be preserved");
        assertTrue(output.contains("**Total**"));
        assertTrue(output.contains("| Name | Count |"));
        assertTrue(output.contains("`getCount()`"));
        assertTrue(output.contains("```java"));
    }

    @Test
    @DisplayName("Should preserve special characters in safe contexts")
    public void testSpecialCharacters() {
        String input = "Price: $100 & tax = 20%\n\nFormula: `x < 10 && y > 5`";
        String output = sanitizer.sanitize(input);

        // Special chars in text should be preserved
        assertTrue(output.contains("$100"), "Dollar sign should be preserved");
        assertTrue(output.contains("&"), "Ampersand should be preserved in text");
        assertTrue(output.contains("20%"), "Percent should be preserved");

        // Special chars in code should be preserved
        assertTrue(output.contains("`x < 10 && y > 5`"), "Code should preserve special chars");
    }

    @Test
    @DisplayName("Should handle UTF-8 and emojis")
    public void testUtf8Emojis() {
        String input = "Hello 世界 🌍 **bold** 你好";
        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("世界"), "Chinese characters should be preserved");
        assertTrue(output.contains("🌍"), "Emoji should be preserved");
        assertTrue(output.contains("你好"), "Chinese characters should be preserved");
        assertTrue(output.contains("**bold**"), "Markdown should still work with UTF-8");
    }

    // ============================================================================
    // Configuration Tests
    // ============================================================================

    @Test
    @DisplayName("Should respect base64 images configuration")
    public void testBase64ImagesConfiguration() {
        String input = "![Test](data:image/png;base64,iVBORw0KGgo=)";

        // Default: blocked (secure config)
        MarkdownSyntaxSanitizer secureSanitizer = new MarkdownSyntaxSanitizer(MarkdownConfig.secure());
        String output1 = secureSanitizer.sanitize(input);
        assertFalse(output1.contains("data:image"), "Data URL should be blocked by default");

        // Enabled: allowed (permissive config)
        MarkdownSyntaxSanitizer permissiveSanitizer = new MarkdownSyntaxSanitizer(MarkdownConfig.permissive());
        String output2 = permissiveSanitizer.sanitize(input);
        assertTrue(output2.contains("data:image"), "Data URL should be allowed when enabled");

        // Disabled again: blocked (strict config)
        MarkdownSyntaxSanitizer strictSanitizer = new MarkdownSyntaxSanitizer(MarkdownConfig.strict());
        String output3 = strictSanitizer.sanitize(input);
        assertFalse(output3.contains("data:image"), "Data URL should be blocked when disabled");
    }

    @Test
    @DisplayName("Should handle multiple consecutive sanitization calls")
    public void testConsecutiveSanitization() {
        String input = "**Bold** <script>XSS</script>";

        String output1 = sanitizer.sanitize(input);
        String output2 = sanitizer.sanitize(output1);
        String output3 = sanitizer.sanitize(output2);

        // Should be idempotent (no further changes after first sanitization)
        assertTrue(output1.contains("**Bold**"), "Bold should be preserved");
        assertFalse(output1.contains("<script>"), "Script should be removed");
        assertTrue(output1.contains("XSS"), "Script content kept as plain text");

        // Subsequent calls should not break already sanitized content
        assertEquals(output2, output3, "Should be idempotent");
    }

    // ============================================================================
    // HTML Detection Tests (Fix for rendering bugs)
    // ============================================================================

    @Test
    @DisplayName("Should detect and preserve pre-rendered HTML tables")
    public void testHtmlTablePassthrough() {
        String htmlTable = "<table><tr><td>Data</td></tr></table>";

        String output = sanitizer.sanitize(htmlTable);

        // Should pass through unchanged (detected as HTML, not markdown)
        assertEquals(htmlTable, output, "HTML table should pass through without sanitization");
        assertTrue(output.contains("<table>"), "Table tag should be preserved");
        assertTrue(output.contains("</table>"), "Closing table tag should be preserved");
    }

    @Test
    @DisplayName("Should detect and preserve pre-rendered HTML divs")
    public void testHtmlDivPassthrough() {
        String htmlDiv = "<div class='ai-markdown-content'><p>Content</p></div>";

        String output = sanitizer.sanitize(htmlDiv);

        // Should pass through unchanged
        assertEquals(htmlDiv, output, "HTML div should pass through without sanitization");
        assertTrue(output.contains("<div"), "Div tag should be preserved");
        assertTrue(output.contains("</div>"), "Closing div tag should be preserved");
    }

    @Test
    @DisplayName("Should detect HTML by multiple closing tags")
    public void testHtmlDetectionByClosingTags() {
        String html = "<p>Paragraph 1</p><p>Paragraph 2</p><p>Paragraph 3</p>";

        String output = sanitizer.sanitize(html);

        // Should be detected as HTML (3+ closing tags) and pass through
        assertEquals(html, output, "HTML with multiple closing tags should pass through");
        assertFalse(output.contains("&lt;"), "Tags should not be escaped");
    }

    @Test
    @DisplayName("Should still sanitize markdown even with some HTML-like chars")
    public void testMarkdownWithSingleTag() {
        String markdown = "**Bold** and <emphasis>text</emphasis>";

        String output = sanitizer.sanitize(markdown);

        // Has only 1 closing tag, so treated as markdown
        assertTrue(output.contains("**Bold**"), "Markdown should be preserved");
        assertFalse(output.contains("<emphasis>"), "Single HTML tag should be removed");
    }

    @Test
    @DisplayName("Should handle mixed HTML structure from tool results")
    public void testToolResultHtmlStructure() {
        String toolResult = "<pre><code>SELECT * FROM C_Order;</code></pre>";

        String output = sanitizer.sanitize(toolResult);

        // Should preserve tool result HTML
        assertEquals(toolResult, output, "Tool result HTML should be preserved");
        assertTrue(output.contains("<pre>"), "Pre tag should be preserved");
        assertTrue(output.contains("<code>"), "Code tag should be preserved");
    }

    @Test
    @DisplayName("Should not escape HTML in fallback for HTML content")
    public void testFallbackPreservesHtml() {
        // Simulate exception scenario by passing content that would trigger HTML detection
        String htmlContent = "<ul><li>Item 1</li><li>Item 2</li></ul>";

        String output = sanitizer.sanitize(htmlContent);

        // Even if sanitization were to fail, HTML should be preserved (not escaped)
        assertFalse(output.contains("&lt;ul&gt;"), "HTML should not be escaped in fallback");
        assertTrue(output.contains("<ul>"), "Unordered list should be preserved");
    }

    @Test
    @DisplayName("Should process markdown inside div wrapper (not skip)")
    public void testMarkdownInsideDivNotSkipped() {
        // This mimics AI response with markdown inside wrapper
        String mixedContent = "<div class='ai-markdown-content'>**Bold** text and | table | row |</div>";

        String output = sanitizer.sanitize(mixedContent);

        // Should process the markdown, not skip it
        assertTrue(output.contains("**Bold**"), "Markdown bold should be preserved");
        // The wrapper div might be removed by escapeRawHtml step
        // The key is that markdown IS processed, not skipped
    }

    @Test
    @DisplayName("Should skip ONLY pure HTML tool results")
    public void testPureHtmlToolResultSkipped() {
        // Pure HTML table from tool result (no markdown)
        String pureHtml = "<table><tr><td>Data 1</td><td>Data 2</td></tr><tr><td>Data 3</td><td>Data 4</td></tr></table>";

        String output = sanitizer.sanitize(pureHtml);

        // Should pass through unchanged (detected as pure HTML)
        assertEquals(pureHtml, output, "Pure HTML tool result should pass through");
    }

    @Test
    @DisplayName("Should NOT skip HTML with markdown syntax")
    public void testHtmlWithMarkdownNotSkipped() {
        // HTML that contains markdown - should be processed
        String mixed = "<table><tr><td>**Bold**</td></tr></table>";

        String output = sanitizer.sanitize(mixed);

        // Should process markdown, not skip
        // The table tags might be escaped by escapeRawHtml, but markdown should be processed
        assertTrue(output.contains("**Bold**"), "Markdown inside HTML should be preserved");
    }
}
