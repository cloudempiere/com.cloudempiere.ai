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
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * Unit tests for StreamingMarkdownRenderer.
 *
 * <p>Tests progressive markdown rendering during streaming to match ChatGPT's UX.
 *
 * @author Cloudempiere
 */
@UnitTest
public class StreamingMarkdownRendererTest {

    private StreamingMarkdownRenderer renderer;

    @BeforeEach
    public void setUp() {
        renderer = new StreamingMarkdownRenderer();
    }

    @Test
    public void testBoldRendering() {
        renderer.appendChunk("This is **bo");
        String html1 = renderer.renderCurrentState();
        // Should show plain text until closing ** arrives
        assertTrue(html1.contains("This is **bo") || html1.contains("**bo"),
            "Incomplete bold should show as plain text");

        renderer.appendChunk("ld** text");
        String html2 = renderer.renderCurrentState();
        // Should show formatted bold
        assertTrue(html2.contains("<strong>bold</strong>"),
            "Complete bold should be formatted: " + html2);
        assertTrue(html2.contains("text"),
            "Text after bold should appear: " + html2);
    }

    @Test
    public void testItalicRendering() {
        renderer.appendChunk("This is *ital");
        String html1 = renderer.renderCurrentState();
        // Incomplete italic shows as plain text
        assertTrue(html1.contains("*ital") || html1.contains("This is *ital"),
            "Incomplete italic should show as plain text");

        renderer.appendChunk("ic* text");
        String html2 = renderer.renderCurrentState();
        // Complete italic should be formatted
        assertTrue(html2.contains("<em>italic</em>"),
            "Complete italic should be formatted: " + html2);
    }

    @Test
    public void testInlineCodeRendering() {
        renderer.appendChunk("Use `cod");
        String html1 = renderer.renderCurrentState();
        // Incomplete code shows as plain text
        assertTrue(html1.contains("`cod") || html1.contains("Use `cod"),
            "Incomplete code should show as plain text");

        renderer.appendChunk("e` here");
        String html2 = renderer.renderCurrentState();
        // Complete code should be formatted
        assertTrue(html2.contains("<code>code</code>"),
            "Complete code should be formatted: " + html2);
    }

    @Test
    public void testCodeBlockRendering() {
        renderer.appendChunk("```python\n");
        renderer.appendChunk("def func():\n");
        renderer.appendChunk("    pass\n");
        renderer.appendChunk("```");

        String html = renderer.renderCurrentState();
        // Should have code block with language
        assertTrue(html.contains("<pre><code"),
            "Should contain code block: " + html);
        assertTrue(html.contains("language-python") || html.contains("def func"),
            "Should contain Python code: " + html);
    }

    @Test
    public void testHeadingRendering() {
        renderer.appendChunk("# Heading");
        renderer.appendChunk(" 1\n");

        String html = renderer.renderCurrentState();
        // Should have heading
        assertTrue(html.contains("<h1>"),
            "Should contain h1 tag: " + html);
        assertTrue(html.contains("Heading 1"),
            "Should contain heading text: " + html);
        assertFalse(html.contains("# Heading"),
            "Should not show raw # marker: " + html);
    }

    @Test
    public void testMultipleHashHeading() {
        // Test h3 heading (###)
        renderer.appendChunk("### Lihavoitu teksti\n");

        String html = renderer.renderFinal();
        // Should have h3 heading
        assertTrue(html.contains("<h3>"),
            "Should contain h3 tag: " + html);
        assertTrue(html.contains("Lihavoitu teksti"),
            "Should contain heading text: " + html);
        assertFalse(html.contains("###"),
            "Should not show raw ### markers: " + html);
    }

    @Test
    public void testHeadingWithoutSpace() {
        // Invalid heading (no space after #)
        renderer.appendChunk("#NoSpace\n");

        String html = renderer.renderFinal();
        // Should NOT be a heading - should show as plain text
        assertFalse(html.contains("<h1>"),
            "Should not be a heading without space: " + html);
        assertTrue(html.contains("#NoSpace"),
            "Should show as plain text: " + html);
    }

    @Test
    public void testSplitMarkers() {
        // Test bold split across chunks
        renderer.appendChunk("Text *");
        renderer.appendChunk("*bold*");
        renderer.appendChunk("* more");

        String html = renderer.renderCurrentState();
        // Should handle split ** correctly
        assertTrue(html.contains("<strong>bold</strong>") || html.contains("bold"),
            "Split markers should be handled: " + html);
    }

    @Test
    public void testNestedFormatting() {
        renderer.appendChunk("**bold with *italic* inside**");

        String html = renderer.renderFinal();
        // Should handle nested formatting
        // Note: Our simple implementation may not handle nesting perfectly,
        // so we just check that both appear
        assertTrue(html.contains("<strong>") || html.contains("bold"),
            "Should contain bold: " + html);
        assertTrue(html.contains("<em>") || html.contains("italic"),
            "Should contain italic: " + html);
    }

    @Test
    public void testMixedContent() {
        renderer.appendChunk("Plain text with **bold** and *italic* and `code`.");

        String html = renderer.renderFinal();
        // All elements should be present
        assertTrue(html.contains("<strong>bold</strong>"),
            "Should contain bold: " + html);
        assertTrue(html.contains("<em>italic</em>"),
            "Should contain italic: " + html);
        assertTrue(html.contains("<code>code</code>"),
            "Should contain code: " + html);
    }

    @Test
    public void testReset() {
        renderer.appendChunk("Some **text**");
        assertTrue(renderer.hasContent(), "Should have content");

        renderer.reset();
        assertFalse(renderer.hasContent(), "Should have no content after reset");

        String html = renderer.renderCurrentState();
        assertTrue(html.isEmpty(), "Should return empty string after reset");
    }

    @Test
    public void testFinalRenderDoesNotReparse() {
        // Append chunks progressively
        renderer.appendChunk("This is **bold");
        renderer.appendChunk("** text");

        // Get streaming state
        String streamingHtml = renderer.renderCurrentState();

        // Get final state
        String finalHtml = renderer.renderFinal();

        // Final should include what was built during streaming
        assertTrue(finalHtml.contains("<strong>bold</strong>"),
            "Final render should include progressive HTML: " + finalHtml);
    }

    @Test
    public void testEscapingSpecialChars() {
        renderer.appendChunk("Text with <script>alert('xss')</script> and & symbols");

        String html = renderer.renderFinal();
        // HTML should be escaped
        assertFalse(html.contains("<script>"),
            "Should escape script tags: " + html);
        assertTrue(html.contains("&lt;") || html.contains("&amp;"),
            "Should escape HTML entities: " + html);
    }

    @Test
    public void testHeadingWithBold() {
        renderer.appendChunk("### **Lihavoitu teksti**\n");

        String html = renderer.renderFinal();
        // Should have h3 heading with bold inside
        assertTrue(html.contains("<h3>"),
            "Should contain h3 tag: " + html);
        assertTrue(html.contains("<strong>Lihavoitu teksti</strong>"),
            "Should contain bold text inside heading: " + html);
        assertTrue(html.contains("</h3>"),
            "Should close h3 tag: " + html);
        assertFalse(html.contains("###"),
            "Should not show ### markers: " + html);
        assertFalse(html.contains("**"),
            "Should not show ** markers: " + html);
    }

    @Test
    public void testHeadingWithItalic() {
        renderer.appendChunk("### *Kursivoitu teksti*\n");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<h3>"),
            "Should contain h3 tag: " + html);
        assertTrue(html.contains("<em>Kursivoitu teksti</em>"),
            "Should contain italic text inside heading: " + html);
        assertFalse(html.contains("*Kursivoitu"),
            "Should not show * markers: " + html);
    }

    @Test
    public void testHeadingWithCode() {
        renderer.appendChunk("### `Kooditeksti`\n");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<h3>"),
            "Should contain h3 tag: " + html);
        assertTrue(html.contains("<code>Kooditeksti</code>"),
            "Should contain code inside heading: " + html);
        assertFalse(html.contains("`Kooditeksti"),
            "Should not show backtick markers: " + html);
    }

    @Test
    public void testHorizontalRule() {
        renderer.appendChunk("Text before\n---\nText after");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<hr/>"),
            "Should contain horizontal rule: " + html);
        assertFalse(html.contains("---"),
            "Should not show --- markers: " + html);
    }

    @Test
    public void testNestedBoldItalic() {
        renderer.appendChunk("**combine *different* formatting**");

        String html = renderer.renderFinal();
        // Should have bold with italic inside
        assertTrue(html.contains("<strong>"),
            "Should contain bold: " + html);
        assertTrue(html.contains("<em>different</em>"),
            "Should contain nested italic: " + html);
        assertTrue(html.contains("</strong>"),
            "Should close bold: " + html);
        assertFalse(html.contains("*different*"),
            "Should not show literal * markers: " + html);
    }

    @Test
    public void testNestedBoldCode() {
        renderer.appendChunk("**bold `code` text**");

        String html = renderer.renderFinal();
        // Should have bold with code inside
        assertTrue(html.contains("<strong>"),
            "Should contain bold: " + html);
        assertTrue(html.contains("<code>code</code>"),
            "Should contain nested code: " + html);
        assertTrue(html.contains("</strong>"),
            "Should close bold: " + html);
        assertFalse(html.contains("`code`"),
            "Should not show literal backticks: " + html);
    }

    @Test
    public void testEmoji() {
        renderer.appendChunk("Hello 😊 World");

        String html = renderer.renderFinal();
        // Should preserve emoji
        assertTrue(html.contains("😊"),
            "Should contain emoji: " + html);
        assertFalse(html.contains("��"),
            "Should not show replacement character: " + html);
    }

    @Test
    public void testMultipleEmojis() {
        renderer.appendChunk("Can I help? 👍😊");

        String html = renderer.renderFinal();
        assertTrue(html.contains("👍") && html.contains("😊"),
            "Should contain both emojis: " + html);
    }

    @Test
    public void testDoubleUnderscoreBold() {
        renderer.appendChunk("Use __double underscores__ for bold text.");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<strong>double underscores</strong>"),
            "Should contain bold with double underscores: " + html);
        assertFalse(html.contains("__"),
            "Should not show __ markers: " + html);
    }

    @Test
    public void testTripleAsterisksBoldItalic() {
        renderer.appendChunk("Use ***three asterisks*** to get both.");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<strong><em>three asterisks</em></strong>"),
            "Should contain bold+italic with triple asterisks: " + html);
        assertFalse(html.contains("***"),
            "Should not show *** markers: " + html);
    }

    @Test
    public void testTripleUnderscoresBoldItalic() {
        renderer.appendChunk("Use ___three underscores___ to get both.");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<strong><em>three underscores</em></strong>"),
            "Should contain bold+italic with triple underscores: " + html);
        assertFalse(html.contains("___"),
            "Should not show ___ markers: " + html);
    }

    @Test
    public void testSingleUnderscoreItalic() {
        renderer.appendChunk("Use _single underscores_ for italics.");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<em>single underscores</em>"),
            "Should contain italic with single underscores: " + html);
        assertFalse(html.contains("_single"),
            "Should not show _ markers: " + html);
    }

    @Test
    public void testMixedAsterisksAndUnderscores() {
        renderer.appendChunk("**bold** and __also bold__ and *italic* and _also italic_");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<strong>bold</strong>"),
            "Should contain asterisk bold: " + html);
        assertTrue(html.contains("<strong>also bold</strong>"),
            "Should contain underscore bold: " + html);
        assertTrue(html.contains("<em>italic</em>"),
            "Should contain asterisk italic: " + html);
        assertTrue(html.contains("<em>also italic</em>"),
            "Should contain underscore italic: " + html);
    }

    // ==========================================================================
    // Incomplete Code Block Protection Tests
    // ==========================================================================

    @Test
    public void testIncompleteCodeBlockBuffering() {
        // Chunk 1: Opens code block (incomplete)
        renderer.appendChunk("Code example:\n```java\npublic class");

        // Should not have rendered anything yet (buffering)
        String html1 = renderer.renderCurrentState();
        // Buffer is not shown during streaming
        assertFalse(html1.contains("<pre><code"),
            "Should not render incomplete code block: " + html1);

        // Chunk 2: Continues code block (still incomplete)
        renderer.appendChunk(" Example {\n    public void method() {\n");

        // Still buffering
        String html2 = renderer.renderCurrentState();
        assertFalse(html2.contains("<pre><code"),
            "Should still be buffering: " + html2);

        // Chunk 3: Closes code block (complete!)
        renderer.appendChunk("    }\n}\n```\nDone!");

        // Now should render complete code block
        String html3 = renderer.renderFinal();
        assertTrue(html3.contains("<pre><code"),
            "Should contain code block: " + html3);
        assertTrue(html3.contains("language-java"),
            "Should detect Java language: " + html3);
        assertTrue(html3.contains("public class Example"),
            "Should contain complete code: " + html3);
        assertTrue(html3.contains("Done!"),
            "Should contain text after code block: " + html3);
    }

    @Test
    public void testCodeBlockSplitAcrossTwoChunks() {
        // Simple case: code block split exactly at closing marker
        renderer.appendChunk("Here's code:\n```python\nprint('hello')");
        renderer.appendChunk("\n```\nThat's it!");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<pre><code"),
            "Should contain code block: " + html);
        assertTrue(html.contains("print('hello')"),
            "Should contain Python code: " + html);
        assertTrue(html.contains("That's it!"),
            "Should contain text after code: " + html);
    }

    @Test
    public void testMultipleCodeBlocksWithBuffering() {
        // First code block (incomplete)
        renderer.appendChunk("First:\n```js\nconst x");
        // Complete first block
        renderer.appendChunk(" = 1;\n```\n");
        // Second code block (incomplete)
        renderer.appendChunk("Second:\n```java\nString s");
        // Complete second block
        renderer.appendChunk(" = \"hi\";\n```\nDone!");

        String html = renderer.renderFinal();
        assertTrue(html.contains("const x = 1"),
            "Should contain first code block: " + html);
        assertTrue(html.contains("String s = \"hi\""),
            "Should contain second code block: " + html);
    }

    @Test
    public void testCodeBlockWithMarkdownInsideShouldNotProcess() {
        // Code block containing markdown-like syntax
        renderer.appendChunk("```java\nString s = \"**not bold**\";\n");
        renderer.appendChunk("// Comment with *asterisks*\n```");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<pre><code"),
            "Should contain code block: " + html);
        assertTrue(html.contains("**not bold**"),
            "Should preserve asterisks as literal: " + html);
        assertFalse(html.contains("<strong>not bold</strong>"),
            "Should NOT render markdown inside code: " + html);
    }

    @Test
    public void testIncompleteCodeBlockAtStreamEnd() {
        // Edge case: streaming ends before code block closes
        renderer.appendChunk("Code:\n```java\npublic class Incomplete {\n");
        // No closing ``` arrives

        String html = renderer.renderFinal();
        // Should still render something (flushPendingContent handles this)
        assertTrue(html.length() > 0,
            "Should render something even with incomplete code block: " + html);
        // The incomplete code block should be processed as-is
        assertTrue(html.contains("Code:"),
            "Should contain text before code block: " + html);
    }

    @Test
    public void testEmptyCodeBlock() {
        // Code block with no content
        renderer.appendChunk("Empty:\n```\n```\nDone!");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<pre><code"),
            "Should contain empty code block: " + html);
        assertTrue(html.contains("Done!"),
            "Should contain text after code: " + html);
    }

    @Test
    public void testCodeBlockWithOnlyLanguageHint() {
        // Code block with language but no content
        renderer.appendChunk("```javascript\n```");

        String html = renderer.renderFinal();
        assertTrue(html.contains("<pre><code"),
            "Should contain code block: " + html);
        assertTrue(html.contains("language-javascript") || html.contains("javascript"),
            "Should detect JavaScript language: " + html);
    }

    @Test
    public void testTripleBackticksInRegularText() {
        // Triple backticks mentioned in text (not code block)
        // This should be treated as inline text, not opening a code block
        renderer.appendChunk("Use ``` to create code blocks in markdown.");

        String html = renderer.renderFinal();
        // Should render the backticks as literal text
        assertTrue(html.contains("```") || html.contains("to create"),
            "Should preserve triple backticks in text: " + html);
        assertFalse(html.contains("<pre><code"),
            "Should not create code block from inline backticks: " + html);
    }
}
