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
package com.cloudempiere.ai.test.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.test.categories.UnitTest;
import com.cloudempiere.ai.util.HtmlStripper;

/**
 * Test suite for HtmlStripper utility.
 *
 * <p>Verifies that HTML tags are properly stripped from AI responses while
 * preserving code blocks, HTML entities, and markdown syntax.
 *
 * @author Cloudempiere
 * @version 1.0
 */
@UnitTest
@Tag("needs-runtime")   // ADR-020: host class needs org.compiere.util.CLogger at class-init
public class HtmlStripperTest {

    // ============================================================================
    // Basic HTML Stripping Tests
    // ============================================================================

    @Test
    public void testStripSimpleHtmlTags() {
        String input = "<div>Hello World</div>";
        String expected = "Hello World";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should strip simple div tags");
    }

    @Test
    public void testStripNestedHtmlTags() {
        String input = "<p>This is <b>bold</b> and <i>italic</i> text</p>";
        String expected = "This is bold and italic text";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should strip nested HTML tags");
    }

    @Test
    public void testStripSelfClosingTags() {
        String input = "Line 1<br/>Line 2<hr/>Line 3";
        String expected = "Line 1Line 2Line 3";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should strip self-closing tags");
    }

    @Test
    public void testStripTagsWithAttributes() {
        String input = "<div class='test' id='container'>Content</div>";
        String expected = "Content";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should strip tags with attributes");
    }

    // ============================================================================
    // HTML Entity Preservation Tests
    // ============================================================================

    @Test
    public void testPreserveHtmlEntities() {
        String input = "1 &lt; 2 &amp;&amp; 3 &gt; 2";
        String expected = "1 &lt; 2 &amp;&amp; 3 &gt; 2";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should preserve HTML entities");
    }

    @Test
    public void testPreserveQuotEntities() {
        String input = "&quot;Hello&quot; &amp; &apos;World&apos;";
        String expected = "&quot;Hello&quot; &amp; &apos;World&apos;";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should preserve quote entities");
    }

    // ============================================================================
    // Code Block Protection Tests
    // ============================================================================

    @Test
    public void testPreserveInlineCodeWithHtml() {
        String input = "Use `<div>` tag for containers";
        String expected = "Use `<div>` tag for containers";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should preserve HTML in inline code");
    }

    @Test
    public void testPreserveFencedCodeBlockWithHtml() {
        String input = "Example:\n```html\n<div class='container'>\n  <p>Hello</p>\n</div>\n```\nThat's it!";
        String expected = "Example:\n```html\n<div class='container'>\n  <p>Hello</p>\n</div>\n```\nThat's it!";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should preserve HTML in fenced code blocks");
    }

    @Test
    public void testStripHtmlOutsideCodeBlocks() {
        String input = "<p>Here is code: `<div>test</div>` example</p>";
        String expected = "Here is code: `<div>test</div>` example";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should strip HTML outside code blocks only");
    }

    @Test
    public void testPreserveMultipleCodeBlocksWithHtml() {
        String input = "First: `<span>inline</span>` then:\n```html\n<div>block</div>\n```\nand `<a>link</a>`";
        String expected = "First: `<span>inline</span>` then:\n```html\n<div>block</div>\n```\nand `<a>link</a>`";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should preserve HTML in all code blocks");
    }

    @Test
    public void testPreserveJavaScriptCodeBlock() {
        String input = "```javascript\nconst html = '<div>test</div>';\ndocument.body.innerHTML = html;\n```";
        String expected = "```javascript\nconst html = '<div>test</div>';\ndocument.body.innerHTML = html;\n```";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should preserve HTML in JavaScript code blocks");
    }

    // ============================================================================
    // Mixed Content Tests
    // ============================================================================

    @Test
    public void testMixedMarkdownAndHtml() {
        String input = "This is **bold** text with <div>HTML</div> and `<code>` inline.";
        String expected = "This is **bold** text with HTML and `<code>` inline.";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should strip HTML but preserve markdown and code");
    }

    @Test
    public void testComplexMixedContent() {
        String input =
            "# Heading\n\n" +
            "<p>Paragraph with <b>bold</b></p>\n\n" +
            "Code example: `<div class='test'>content</div>`\n\n" +
            "```html\n" +
            "<html>\n" +
            "  <body>Example</body>\n" +
            "</html>\n" +
            "```\n\n" +
            "<span>More HTML</span>";

        String expected =
            "# Heading\n\n" +
            "Paragraph with bold\n\n" +
            "Code example: `<div class='test'>content</div>`\n\n" +
            "```html\n" +
            "<html>\n" +
            "  <body>Example</body>\n" +
            "</html>\n" +
            "```\n\n" +
            "More HTML";

        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should handle complex mixed content correctly");
    }

    // ============================================================================
    // Split Tag Simulation (Streaming Concern)
    // ============================================================================

    @Test
    public void testSplitTagsAcrossChunks() {
        // Simulate streaming where HTML tags are split across chunks
        // In practice, StreamingMarkdownRenderer escapes character-by-character,
        // but if HTML somehow gets through to AIMessageRenderer, we should strip it

        // Chunk 1: "<di"
        // Chunk 2: "v>Test</"
        // Chunk 3: "div>"
        // Combined: "<div>Test</div>"
        String combined = "<div>Test</div>";
        String expected = "Test";
        String actual = HtmlStripper.stripHtml(combined);
        assertEquals(expected, actual, "Should strip HTML even if originally split across chunks");
    }

    @Test
    public void testPartialTagAtBoundary() {
        // Test edge case where tag is incomplete
        String input = "Text with <incomplete";
        // Pattern won't match incomplete tag (no closing >), so it stays as-is
        String expected = "Text with <incomplete";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should leave incomplete tags (missing >) as-is");
    }

    // ============================================================================
    // containsHtml() Tests
    // ============================================================================

    @Test
    public void testContainsHtmlDetection() {
        assertTrue(HtmlStripper.containsHtml("<div>test</div>"), "Should detect HTML tags");
        assertFalse(HtmlStripper.containsHtml("**bold** text"), "Should not detect markdown as HTML");
        assertFalse(HtmlStripper.containsHtml("`<div>code</div>`"), "Should ignore HTML in inline code");
        assertFalse(HtmlStripper.containsHtml("```html\n<div>test</div>\n```"), "Should ignore HTML in fenced code");
        assertTrue(HtmlStripper.containsHtml("<p>text</p> and `<code>`"), "Should detect HTML outside code");
    }

    // ============================================================================
    // Edge Cases
    // ============================================================================

    @Test
    public void testNullInput() {
        assertNull(HtmlStripper.stripHtml(null), "Should return null for null input");
    }

    @Test
    public void testEmptyInput() {
        assertEquals("", HtmlStripper.stripHtml(""), "Should return empty string for empty input");
    }

    @Test
    public void testNoHtmlTags() {
        String input = "This is plain text with no HTML";
        assertEquals(input, HtmlStripper.stripHtml(input), "Should return unchanged if no HTML");
    }

    @Test
    public void testOnlyHtmlTags() {
        String input = "<div></div><span></span>";
        String expected = "";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should strip all tags leaving nothing");
    }

    @Test
    public void testUnicodeAndEmojis() {
        String input = "<div>Hello \u4E16\u754C \uD83C\uDF0D</div>";
        String expected = "Hello \u4E16\u754C \uD83C\uDF0D";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should preserve Unicode and emojis");
    }

    // ============================================================================
    // Security Tests
    // ============================================================================

    @Test
    public void testScriptTagRemoval() {
        String input = "<script>alert('xss')</script>Normal text";
        String expected = "alert('xss')Normal text";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should strip script tags (XSS prevention)");
    }

    @Test
    public void testEventHandlerRemoval() {
        String input = "<div onclick='malicious()'>Click me</div>";
        String expected = "Click me";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should strip tags with event handlers");
    }

    @Test
    public void testIframeRemoval() {
        String input = "<iframe src='evil.com'></iframe>Safe content";
        String expected = "Safe content";
        String actual = HtmlStripper.stripHtml(input);
        assertEquals(expected, actual, "Should strip iframe tags");
    }
}
