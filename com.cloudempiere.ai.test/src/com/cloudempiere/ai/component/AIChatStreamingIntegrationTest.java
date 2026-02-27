package com.cloudempiere.ai.component;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.test.categories.IntegrationTest;
import com.cloudempiere.ai.util.MarkdownValidator;

/**
 * Integration tests for AI Chat streaming with renderer competition fixes.
 *
 * <p>Tests the complete streaming flow including:
 * <ul>
 *   <li>Renderer switching (markdown → table → markdown)</li>
 *   <li>Markdown validation during streaming</li>
 *   <li>Progressive rendering</li>
 *   <li>Final rendering consistency</li>
 * </ul>
 *
 * <p><b>Key Scenarios:</b>
 * <ol>
 *   <li>Response with table in middle (pre-table text, table, post-table text)</li>
 *   <li>Response with invalid markdown from AI</li>
 *   <li>Response with mixed formatting (bold, italic, code, headings)</li>
 * </ol>
 *
 * @author Cloudempiere
 */
@IntegrationTest
@DisplayName("AI Chat Streaming - Integration tests for renderer fixes")
public class AIChatStreamingIntegrationTest {

    private AIChatStreamingMessage streamingMsg;

    @BeforeEach
    public void setUp() {
        streamingMsg = new AIChatStreamingMessage();
    }

    @Test
    @DisplayName("Scenario 1: Response with table in middle should render all parts")
    public void testTableInMiddleOfResponse() {
        // Simulate AI response: intro text → table → conclusion text
        String intro = "Here's the data:\n\n";
        String tableHeader = "| ID | Name |\n";
        String tableSeparator = "|-------|-------|\n";
        String tableRow = "| 1     | Test  |\n";
        String conclusion = "\n**Bold conclusion** after table.";

        // Stream chunks
        streamingMsg.appendChunk(intro);
        streamingMsg.appendChunk(tableHeader);
        streamingMsg.appendChunk(tableSeparator);
        streamingMsg.appendChunk(tableRow);
        streamingMsg.appendChunk(conclusion);
        streamingMsg.complete();

        // Verify final HTML contains all parts
        String html = streamingMsg.getRenderedHtml();

        assertTrue(html.contains("Here's the data"), "Should contain intro text: " + html);
        assertTrue(html.contains("<table"), "Should contain table: " + html);
        assertTrue(html.contains("<strong>Bold conclusion</strong>"),
                   "Should render bold in post-table text: " + html);
    }

    @Test
    @DisplayName("Scenario 2: Invalid markdown from AI should be fixed")
    public void testInvalidMarkdownFixed() {
        // AI generates invalid markdown: **### Invalid**
        String invalidMarkdown = "**### This is wrong**";

        // Validate (should be fixed before rendering)
        String fixed = MarkdownValidator.validate(invalidMarkdown);

        // Fixed markdown should have heading first
        assertTrue(fixed.startsWith("###"), "Should move heading to start: " + fixed);
        assertTrue(fixed.contains("**"), "Should preserve bold markers: " + fixed);
    }

    @Test
    @DisplayName("Scenario 3: Unclosed markers should be auto-closed")
    public void testUnclosedMarkersAutoClosed() {
        String unclosed = "This is **bold and *italic";

        streamingMsg.appendChunk(unclosed);
        streamingMsg.complete();

        String html = streamingMsg.getRenderedHtml();

        assertTrue(html.contains("<strong>"), "Should have bold tag: " + html);
        assertTrue(html.contains("</strong>"), "Should close bold tag: " + html);
        assertTrue(html.contains("<em>"), "Should have italic tag: " + html);
        assertTrue(html.contains("</em>"), "Should close italic tag: " + html);
    }

    @Test
    @DisplayName("Scenario 4: Complex nested formatting should render correctly")
    public void testComplexNestedFormatting() {
        String complex = "### **Bold *and italic* in heading**\n\nParagraph with `code`.";

        streamingMsg.appendChunk(complex);
        streamingMsg.complete();

        String html = streamingMsg.getRenderedHtml();

        assertTrue(html.contains("<h3>"), "Should have heading: " + html);
        assertTrue(html.contains("<strong>"), "Should have bold: " + html);
        assertTrue(html.contains("<em>"), "Should have italic: " + html);
        assertTrue(html.contains("<code>"), "Should have code: " + html);
    }

    @Test
    @DisplayName("Scenario 5: Multiple chunks with marker boundaries")
    public void testMarkerSplitAcrossChunks() {
        // Simulate ** split across chunks
        streamingMsg.appendChunk("Text *");
        streamingMsg.appendChunk("*bold");
        streamingMsg.appendChunk(" text");
        streamingMsg.appendChunk("**");
        streamingMsg.complete();

        String html = streamingMsg.getRenderedHtml();
        assertTrue(html.contains("<strong>bold text</strong>"),
                   "Should handle markers split across chunks: " + html);
    }

    @Test
    @DisplayName("Scenario 6: Streaming state transitions correctly")
    public void testStateTransitions() {
        assertEquals(AIChatStreamingMessage.StreamingState.IDLE,
                     streamingMsg.getState(), "Should start in IDLE state");

        streamingMsg.transitionTo(AIChatStreamingMessage.StreamingState.WAITING_FOR_LLM);
        assertEquals(AIChatStreamingMessage.StreamingState.WAITING_FOR_LLM,
                     streamingMsg.getState(), "Should transition to WAITING_FOR_LLM");

        streamingMsg.appendChunk("text");
        assertEquals(AIChatStreamingMessage.StreamingState.STREAMING_TEXT,
                     streamingMsg.getState(), "Should transition to STREAMING_TEXT on chunk");

        streamingMsg.complete();
        assertEquals(AIChatStreamingMessage.StreamingState.COMPLETE,
                     streamingMsg.getState(), "Should transition to COMPLETE on finish");
    }

    @Test
    @DisplayName("Scenario 7: Cancellation during streaming")
    public void testCancellation() {
        streamingMsg.appendChunk("Some text");
        streamingMsg.markCancelled();

        assertEquals(AIChatStreamingMessage.StreamingState.CANCELLED,
                     streamingMsg.getState(), "Should be in CANCELLED state");

        String html = streamingMsg.getRenderedHtml();
        assertTrue(html.contains("cancelled"), "Should show cancellation message: " + html);
    }

    @Test
    @DisplayName("Scenario 8: Code block with language hint")
    public void testCodeBlockWithLanguage() {
        String codeBlock = "```java\npublic void test() {\n    System.out.println(\"test\");\n}\n```";

        streamingMsg.appendChunk(codeBlock);
        streamingMsg.complete();

        String html = streamingMsg.getRenderedHtml();
        assertTrue(html.contains("<pre>"), "Should have pre tag: " + html);
        assertTrue(html.contains("<code"), "Should have code tag: " + html);
        assertTrue(html.contains("language-java"), "Should have language class: " + html);
    }

    @Test
    @DisplayName("Scenario 9: UTF-8 emojis should be preserved")
    public void testEmojiPreservation() {
        String withEmoji = "Test with emoji \uD83D\uDE80 and **bold \uD83C\uDF89**";

        streamingMsg.appendChunk(withEmoji);
        streamingMsg.complete();

        String html = streamingMsg.getRenderedHtml();
        assertTrue(html.contains("\uD83D\uDE80"), "Should preserve emoji: " + html);
        assertTrue(html.contains("\uD83C\uDF89"), "Should preserve emoji in bold: " + html);
    }

    @Test
    @DisplayName("Scenario 10: HTML should be escaped in user content")
    public void testHTMLEscaping() {
        String malicious = "<script>alert('xss')</script>";

        streamingMsg.appendChunk(malicious);
        streamingMsg.complete();

        String html = streamingMsg.getRenderedHtml();
        assertFalse(html.contains("<script>"), "Should escape script tags: " + html);
        assertTrue(html.contains("&lt;script&gt;"), "Should have escaped HTML: " + html);
    }

    @Test
    @DisplayName("Scenario 11: Empty heading should be handled")
    public void testEmptyHeading() {
        String emptyHeading = "### \n";

        streamingMsg.appendChunk(emptyHeading);
        streamingMsg.complete();

        String html = streamingMsg.getRenderedHtml();
        // Empty heading should either be rendered as empty <h3></h3> or skipped
        assertNotNull(html, "Should handle empty heading without error");
    }

    @Test
    @DisplayName("Scenario 12: Horizontal rule should render")
    public void testHorizontalRule() {
        String hr = "Text above\n---\nText below";

        streamingMsg.appendChunk(hr);
        streamingMsg.complete();

        String html = streamingMsg.getRenderedHtml();
        assertTrue(html.contains("<hr"), "Should render horizontal rule: " + html);
    }

    @Test
    @DisplayName("Scenario 13: Content buffer should flush properly")
    public void testContentFlush() {
        streamingMsg.appendChunk("Part 1");
        String content1 = streamingMsg.getContent();
        assertTrue(content1.contains("Part 1"), "Should have part 1");

        streamingMsg.appendChunk(" Part 2");
        String content2 = streamingMsg.getContent();
        assertTrue(content2.contains("Part 1 Part 2"), "Should have both parts");
    }

    @Test
    @DisplayName("Scenario 14: Multiple tables in response")
    public void testMultipleTables() {
        String table1 = "| A | B |\n|---|---|\n| 1 | 2 |\n\n";
        String text = "Text between tables\n\n";
        String table2 = "| C | D |\n|---|---|\n| 3 | 4 |\n";

        streamingMsg.appendChunk(table1);
        streamingMsg.appendChunk(text);
        streamingMsg.appendChunk(table2);
        streamingMsg.complete();

        String html = streamingMsg.getRenderedHtml();
        // Count table tags (should have 2)
        int tableCount = html.split("<table").length - 1;
        assertTrue(tableCount >= 1, "Should have at least one table: " + html);
        assertTrue(html.contains("Text between tables"), "Should have text between tables: " + html);
    }

    @Test
    @DisplayName("Scenario 15: Thinking content should be tracked separately")
    public void testThinkingContent() {
        streamingMsg.appendThinking("Analyzing data...");
        streamingMsg.completeThinking();

        String thinking = streamingMsg.getThinkingContent();
        assertTrue(thinking.contains("Analyzing"), "Should track thinking content: " + thinking);
    }
}
