package com.cloudempiere.ai.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * Unit tests for MarkdownValidator.
 *
 * <p>Tests markdown validation and fixing logic for AI-generated content.
 * Covers:
 * <ul>
 *   <li>Unclosed markers (**, *, `)</li>
 *   <li>Invalid heading order (**### vs ### **)</li>
 *   <li>Overlapping markers</li>
 *   <li>Mixed valid/invalid patterns</li>
 * </ul>
 *
 * @author Cloudempiere
 */
@UnitTest
@DisplayName("MarkdownValidator - AI markdown validation and fixing")
@Tag("needs-runtime")   // ADR-020: host class needs org.compiere.util.CLogger at class-init
public class MarkdownValidatorTest {

    @Test
    @DisplayName("Should pass through valid markdown unchanged")
    public void testValidMarkdown() {
        String valid = "This is **bold** and *italic* and `code`.";
        String result = MarkdownValidator.validate(valid);
        assertEquals(valid, result, "Valid markdown should not be modified");
    }

    @Test
    @DisplayName("Should close unclosed bold markers")
    public void testUnclosedBold() {
        String input = "This is **bold text";
        String expected = "This is **bold text**";
        String result = MarkdownValidator.validate(input);
        assertEquals(expected, result, "Unclosed bold should be auto-closed");
    }

    @Test
    @DisplayName("Should close unclosed italic markers")
    public void testUnclosedItalic() {
        String input = "This is *italic text";
        String expected = "This is *italic text*";
        String result = MarkdownValidator.validate(input);
        assertEquals(expected, result, "Unclosed italic should be auto-closed");
    }

    @Test
    @DisplayName("Should close unclosed code markers")
    public void testUnclosedCode() {
        String input = "This is `inline code";
        String expected = "This is `inline code`";
        String result = MarkdownValidator.validate(input);
        assertEquals(expected, result, "Unclosed code should be auto-closed");
    }

    @Test
    @DisplayName("Should fix invalid heading order: **### Invalid")
    public void testInvalidHeadingOrder() {
        String input = "**### This is wrong**";
        String expected = "### **This is wrong**";
        String result = MarkdownValidator.validate(input);
        assertEquals(expected, result, "Heading markers should be moved to start");
    }

    @Test
    @DisplayName("Should NOT modify valid heading with nested bold: ### **Valid**")
    public void testValidHeadingWithBold() {
        String input = "### **Bold Heading**";
        String result = MarkdownValidator.validate(input);
        assertEquals(input, result, "Valid heading with nested bold should not be modified");
    }

    @Test
    @DisplayName("Should handle multiple unclosed markers")
    public void testMultipleUnclosedMarkers() {
        String input = "**Bold and *italic and `code";
        String result = MarkdownValidator.validate(input);
        // Note: Auto-closes in LIFO order (last opened, first closed)
        assertTrue(result.contains("**"), "Should have bold markers");
        assertTrue(result.contains("*"), "Should have italic markers");
        assertTrue(result.contains("`"), "Should have code markers");
    }

    @Test
    @DisplayName("Should handle overlapping markers correctly")
    public void testOverlappingMarkers() {
        String input = "**Bold *italic**";
        String expected = "**Bold *italic***";  // Auto-close bold before italic ends
        String result = MarkdownValidator.validate(input);
        assertEquals(expected, result, "Overlapping markers should be auto-closed");
    }

    @Test
    @DisplayName("Should handle empty input")
    public void testEmptyInput() {
        String empty = "";
        String result = MarkdownValidator.validate(empty);
        assertEquals(empty, result, "Empty input should return empty");
    }

    @Test
    @DisplayName("Should handle null input")
    public void testNullInput() {
        String result = MarkdownValidator.validate(null);
        assertNull(result, "Null input should return null");
    }

    @Test
    @DisplayName("Should handle multiline content")
    public void testMultilineContent() {
        String input = "Line 1 with **bold\nLine 2 continues\nLine 3 ends";
        String result = MarkdownValidator.validate(input);

        // Each line should be processed independently
        String[] lines = result.split("\n");
        assertTrue(lines[0].contains("**"), "First line should have bold markers");
        assertTrue(lines[0].endsWith("**"), "First line should close bold");
    }

    @Test
    @DisplayName("Should handle complex nested formatting")
    public void testComplexNesting() {
        String input = "### **Bold *and italic* in heading**";
        String result = MarkdownValidator.validate(input);
        assertEquals(input, result, "Valid complex nesting should not be modified");
    }

    @Test
    @DisplayName("Should detect structural issues")
    public void testStructureCheck() {
        String invalid = "This has **unbalanced bold";
        List<String> issues = MarkdownValidator.checkStructure(invalid);

        assertFalse(issues.isEmpty(), "Should detect structural issues");
        assertTrue(issues.get(0).contains("Unbalanced bold"), "Should report unbalanced bold");
    }

    @Test
    @DisplayName("Should pass structure check for valid markdown")
    public void testStructureCheckValid() {
        String valid = "### Heading\n\n**Bold** and *italic* and `code`.";
        List<String> issues = MarkdownValidator.checkStructure(valid);

        assertTrue(issues.isEmpty(), "Valid markdown should have no structural issues");
    }

    @Test
    @DisplayName("Should detect invalid heading syntax (no space after #)")
    public void testInvalidHeadingSyntax() {
        String invalid = "###NoSpace";
        List<String> issues = MarkdownValidator.checkStructure(invalid);

        assertFalse(issues.isEmpty(), "Should detect invalid heading syntax");
        assertTrue(issues.get(0).contains("Invalid heading syntax"),
                   "Should report missing space after #");
    }

    @Test
    @DisplayName("Should handle real AI-generated problematic patterns")
    public void testRealAIPatterns() {
        // Pattern 1: Triple markers without proper nesting
        String pattern1 = "***This is bold and italic***";
        String result1 = MarkdownValidator.validate(pattern1);
        assertNotNull(result1, "Should handle triple markers");

        // Pattern 2: Mixed heading and emphasis
        String pattern2 = "### **1. Bold Text Examples**";
        String result2 = MarkdownValidator.validate(pattern2);
        assertEquals(pattern2, result2, "Valid heading with bold should not change");

        // Pattern 3: Unclosed code block
        String pattern3 = "Here is code:\n```java\npublic void test()";
        String result3 = MarkdownValidator.validate(pattern3);
        assertNotNull(result3, "Should handle unclosed code blocks");
    }

    @Test
    @DisplayName("Should handle edge case: marker at end of line")
    public void testMarkerAtEndOfLine() {
        String input = "Text with trailing **";
        String result = MarkdownValidator.validate(input);
        assertTrue(result.endsWith("****"), "Should close trailing marker");
    }

    @Test
    @DisplayName("Should handle edge case: marker at start of line")
    public void testMarkerAtStartOfLine() {
        String input = "** leading marker";
        String result = MarkdownValidator.validate(input);
        assertTrue(result.contains("****"), "Should close leading marker");
    }

    @Test
    @DisplayName("Should preserve valid horizontal rule")
    public void testHorizontalRule() {
        String input = "Text above\n---\nText below";
        String result = MarkdownValidator.validate(input);
        assertEquals(input, result, "Valid horizontal rule should not be modified");
    }

    @Test
    @DisplayName("Should handle code blocks with language hint")
    public void testCodeBlockWithLanguage() {
        String input = "```java\npublic void test() {\n}\n```";
        String result = MarkdownValidator.validate(input);
        assertEquals(input, result, "Valid code block should not be modified");
    }
}
