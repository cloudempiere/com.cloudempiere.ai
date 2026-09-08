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

import com.cloudempiere.ai.test.categories.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.util.MarkdownTableRenderer;

/**
 * Unit tests for MarkdownTableRenderer - GFM table rendering during streaming.
 *
 * @author Cloudempiere
 * @since CLD-1601
 */
@UnitTest
@DisplayName("MarkdownTableRenderer Tests")
@Tag("needs-runtime")   // ADR-020: host class needs org.compiere.util.CLogger at class-init
class MarkdownTableRendererTest {

    @AfterEach
    void cleanup() {
        // Clear thread-local locale after each test
        MarkdownTableRenderer.clearLocale();
    }

    @Nested
    @UnitTest
@DisplayName("Table Detection")
    class TableDetection {

        @Test
        @UnitTest
@DisplayName("Should detect simple table")
        void shouldDetectSimpleTable() {
            String text = "| Col1 | Col2 |";
            assertThat(MarkdownTableRenderer.containsTable(text)).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should detect pipe-like patterns conservatively")
        void shouldDetectPipeLikePatterns() {
            // Single pipe without ending pipe is not a table
            String text = "This is a | single pipe";
            assertThat(MarkdownTableRenderer.containsTable(text)).isFalse();

            // Two pipes is a potential table pattern (we're inclusive for streaming)
            String text2 = "| Col |";
            assertThat(MarkdownTableRenderer.containsTable(text2)).isTrue();

            String text3 = "No pipes here";
            assertThat(MarkdownTableRenderer.containsTable(text3)).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("Should handle null and empty")
        void shouldHandleNullAndEmpty() {
            assertThat(MarkdownTableRenderer.containsTable(null)).isFalse();
            assertThat(MarkdownTableRenderer.containsTable("")).isFalse();
        }
    }

    @Nested
    @UnitTest
@DisplayName("Simple Table Rendering")
    class SimpleTableRendering {

        @Test
        @UnitTest
@DisplayName("Should render simple 2-column table")
        void shouldRenderSimpleTable() {
            String markdown =
                "| Name | Value |\n" +
                "|------|-------|\n" +
                "| A    | 1     |\n" +
                "| B    | 2     |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            assertThat(result).contains("<table");
            assertThat(result).contains("<th");
            assertThat(result).contains("Name");
            assertThat(result).contains("Value");
            assertThat(result).contains("<td");
            assertThat(result).contains("A");
            assertThat(result).contains("1");
            assertThat(result).contains("B");
            assertThat(result).contains("2");
            assertThat(result).contains("</table>");
        }

        @Test
        @UnitTest
@DisplayName("Should render 3-column table")
        void shouldRenderThreeColumnTable() {
            String markdown =
                "| ID | Name | Status |\n" +
                "|----|------|--------|\n" +
                "| 1  | Test | Active |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            assertThat(result).contains("ID");
            assertThat(result).contains("Name");
            assertThat(result).contains("Status");
            assertThat(result).contains("Test");
            assertThat(result).contains("Active");
        }
    }

    @Nested
    @UnitTest
@DisplayName("Table Alignment")
    class TableAlignment {

        @Test
        @UnitTest
@DisplayName("Should handle left alignment")
        void shouldHandleLeftAlignment() {
            String markdown =
                "| Left |\n" +
                "|:-----|\n" +
                "| Data |";

            String result = MarkdownTableRenderer.renderTables(markdown);
            assertThat(result).contains("text-align: left");
        }

        @Test
        @UnitTest
@DisplayName("Should handle center alignment")
        void shouldHandleCenterAlignment() {
            String markdown =
                "| Center |\n" +
                "|:------:|\n" +
                "| Data   |";

            String result = MarkdownTableRenderer.renderTables(markdown);
            assertThat(result).contains("text-align: center");
        }

        @Test
        @UnitTest
@DisplayName("Should handle right alignment")
        void shouldHandleRightAlignment() {
            String markdown =
                "| Right |\n" +
                "|------:|\n" +
                "| Data  |";

            String result = MarkdownTableRenderer.renderTables(markdown);
            assertThat(result).contains("text-align: right");
        }

        @Test
        @UnitTest
@DisplayName("Should handle mixed alignments")
        void shouldHandleMixedAlignments() {
            String markdown =
                "| Left | Center | Right |\n" +
                "|:-----|:------:|------:|\n" +
                "| A    | B      | C     |";

            String result = MarkdownTableRenderer.renderTables(markdown);
            assertThat(result).contains("text-align: left");
            assertThat(result).contains("text-align: center");
            assertThat(result).contains("text-align: right");
        }
    }

    @Nested
    @UnitTest
@DisplayName("Partial Table Rendering")
    class PartialTableRendering {

        @Test
        @UnitTest
@DisplayName("Should render header-only table (during streaming)")
        void shouldRenderHeaderOnly() {
            String markdown = "| Col1 | Col2 |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            // Should render even without separator
            assertThat(result).contains("<table");
            assertThat(result).contains("Col1");
            assertThat(result).contains("Col2");
        }

        @Test
        @UnitTest
@DisplayName("Should render header with separator (during streaming)")
        void shouldRenderHeaderWithSeparator() {
            String markdown =
                "| Col1 | Col2 |\n" +
                "|------|------|";

            String result = MarkdownTableRenderer.renderTables(markdown);

            assertThat(result).contains("<table");
            assertThat(result).contains("<th");
        }
    }

    @Nested
    @UnitTest
@DisplayName("Mixed Content")
    class MixedContent {

        @Test
        @UnitTest
@DisplayName("Should preserve text before table")
        void shouldPreserveTextBeforeTable() {
            String markdown =
                "Here is some text.\n" +
                "| Col |\n" +
                "|-----|\n" +
                "| A   |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            assertThat(result).contains("Here is some text.");
            assertThat(result).contains("<table");
        }

        @Test
        @UnitTest
@DisplayName("Should preserve text after table")
        void shouldPreserveTextAfterTable() {
            String markdown =
                "| Col |\n" +
                "|-----|\n" +
                "| A   |\n" +
                "More text here.";

            String result = MarkdownTableRenderer.renderTables(markdown);

            assertThat(result).contains("<table");
            assertThat(result).contains("More text here.");
        }

        @Test
        @UnitTest
@DisplayName("Should handle table embedded in text")
        void shouldHandleEmbeddedTable() {
            String markdown =
                "Introduction:\n" +
                "| Item | Price |\n" +
                "|------|-------|\n" +
                "| A    | $10   |\n" +
                "Conclusion.";

            String result = MarkdownTableRenderer.renderTables(markdown);

            assertThat(result).contains("Introduction:");
            assertThat(result).contains("<table");
            // Number formatting adds space after currency symbol: "$ 10"
            assertThat(result).containsPattern("\\$\\s*10");
            assertThat(result).contains("Conclusion.");
        }
    }

    @Nested
    @UnitTest
@DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @UnitTest
@DisplayName("Should handle empty cells")
        void shouldHandleEmptyCells() {
            String markdown =
                "| A |   | C |\n" +
                "|---|---|---|\n" +
                "|   | B |   |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            assertThat(result).contains("<table");
            assertThat(result).contains("A");
            assertThat(result).contains("B");
            assertThat(result).contains("C");
        }

        @Test
        @UnitTest
@DisplayName("Should handle single column table")
        void shouldHandleSingleColumn() {
            String markdown =
                "| Single |\n" +
                "|--------|\n" +
                "| Row    |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            assertThat(result).contains("<table");
            assertThat(result).contains("Single");
            assertThat(result).contains("Row");
        }

        @Test
        @UnitTest
@DisplayName("Should return non-table text unchanged")
        void shouldReturnNonTableUnchanged() {
            String text = "This is just regular text.";

            String result = MarkdownTableRenderer.renderTables(text);

            assertThat(result).isEqualTo(text);
        }

        @Test
        @UnitTest
@DisplayName("Should handle null input")
        void shouldHandleNullInput() {
            String result = MarkdownTableRenderer.renderTables(null);
            assertThat(result).isNull();
        }

        @Test
        @UnitTest
@DisplayName("Should handle empty input")
        void shouldHandleEmptyInput() {
            String result = MarkdownTableRenderer.renderTables("");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @UnitTest
@DisplayName("Styling")
    class Styling {

        @Test
        @UnitTest
@DisplayName("Should include table styling")
        void shouldIncludeTableStyling() {
            String markdown =
                "| A |\n" +
                "|---|\n" +
                "| B |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            assertThat(result).contains("border-collapse");
            assertThat(result).contains("border:");
            assertThat(result).contains("padding:");
        }

        @Test
        @UnitTest
@DisplayName("Should style header differently from data")
        void shouldStyleHeaderDifferently() {
            String markdown =
                "| Header |\n" +
                "|--------|\n" +
                "| Data   |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            assertThat(result).contains("<th");
            assertThat(result).contains("<td");
            assertThat(result).contains("background:"); // Header has background
        }
    }

    @Nested
    @UnitTest
@DisplayName("Numeric Detection")
    class NumericDetection {

        @Test
        @UnitTest
@DisplayName("Should detect plain integers")
        void shouldDetectPlainIntegers() {
            assertThat(MarkdownTableRenderer.isNumeric("123")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("-456")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("+789")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("0")).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should detect decimal numbers")
        void shouldDetectDecimalNumbers() {
            assertThat(MarkdownTableRenderer.isNumeric("123.45")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("-0.99")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("0.5")).isTrue();
            // European style (comma as decimal)
            assertThat(MarkdownTableRenderer.isNumeric("123,45")).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should detect numbers with thousands separators")
        void shouldDetectThousandsSeparators() {
            // US style
            assertThat(MarkdownTableRenderer.isNumeric("1,234")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("1,234,567")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("1,234.56")).isTrue();
            // European style
            assertThat(MarkdownTableRenderer.isNumeric("1.234")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("1.234.567")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("1.234,56")).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should detect currency values")
        void shouldDetectCurrencyValues() {
            assertThat(MarkdownTableRenderer.isNumeric("$100")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("$1,234.56")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("€50")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("£30.99")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("¥1000")).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should detect percentages")
        void shouldDetectPercentages() {
            assertThat(MarkdownTableRenderer.isNumeric("45%")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("12.5%")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("-5%")).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should detect scientific notation")
        void shouldDetectScientificNotation() {
            assertThat(MarkdownTableRenderer.isNumeric("1.23e10")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("2E-5")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric("1e6")).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should NOT detect non-numeric values")
        void shouldNotDetectNonNumeric() {
            assertThat(MarkdownTableRenderer.isNumeric("abc")).isFalse();
            assertThat(MarkdownTableRenderer.isNumeric("Item 1")).isFalse();
            assertThat(MarkdownTableRenderer.isNumeric("Order #123")).isFalse();
            assertThat(MarkdownTableRenderer.isNumeric("2024-01-15")).isFalse();
            assertThat(MarkdownTableRenderer.isNumeric("")).isFalse();
            assertThat(MarkdownTableRenderer.isNumeric(null)).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("Should handle whitespace")
        void shouldHandleWhitespace() {
            assertThat(MarkdownTableRenderer.isNumeric("  123  ")).isTrue();
            assertThat(MarkdownTableRenderer.isNumeric(" $50 ")).isTrue();
        }
    }

    @Nested
    @UnitTest
@DisplayName("Locale-Aware Number Formatting")
    class LocaleFormatting {

        @Test
        @UnitTest
@DisplayName("Should format numbers in US locale")
        void shouldFormatUSLocale() {
            MarkdownTableRenderer.setLocale(Locale.US);

            assertThat(MarkdownTableRenderer.formatNumber("1234567")).isEqualTo("1,234,567");
            assertThat(MarkdownTableRenderer.formatNumber("1234.56")).isEqualTo("1,234.56");
        }

        @Test
        @UnitTest
@DisplayName("Should format numbers in German locale")
        void shouldFormatGermanLocale() {
            MarkdownTableRenderer.setLocale(Locale.GERMANY);

            assertThat(MarkdownTableRenderer.formatNumber("1234567")).isEqualTo("1.234.567");
            // Note: Input parsed, output formatted per locale
            assertThat(MarkdownTableRenderer.formatNumber("1234.56")).isEqualTo("1.234,56");
        }

        @Test
        @UnitTest
@DisplayName("Should preserve currency symbol")
        void shouldPreserveCurrencySymbol() {
            MarkdownTableRenderer.setLocale(Locale.US);

            String result = MarkdownTableRenderer.formatNumber("$1234.56");
            assertThat(result).startsWith("$");
            assertThat(result).contains("1,234.56");
        }

        @Test
        @UnitTest
@DisplayName("Should preserve percentage suffix")
        void shouldPreservePercentageSuffix() {
            MarkdownTableRenderer.setLocale(Locale.US);

            String result = MarkdownTableRenderer.formatNumber("45.5%");
            assertThat(result).endsWith("%");
            assertThat(result).contains("45.5");
        }

        @Test
        @UnitTest
@DisplayName("Should return original if not parseable")
        void shouldReturnOriginalIfNotParseable() {
            String result = MarkdownTableRenderer.formatNumber("not a number");
            assertThat(result).isEqualTo("not a number");
        }
    }

    @Nested
    @UnitTest
@DisplayName("Auto Numeric Column Alignment")
    class AutoNumericAlignment {

        @Test
        @UnitTest
@DisplayName("Should right-align numeric columns automatically")
        void shouldRightAlignNumericColumns() {
            String markdown =
                "| Product | Price | Quantity |\n" +
                "|---------|-------|----------|\n" +
                "| Widget  | 10.00 | 5        |\n" +
                "| Gadget  | 25.50 | 3        |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            // Price and Quantity columns should be right-aligned
            // Product column should remain left-aligned
            assertThat(result).contains("text-align: right");

            // Count occurrences of right alignment (should be multiple - header + data cells)
            int rightAlignCount = countOccurrences(result, "text-align: right");
            assertThat(rightAlignCount).isGreaterThanOrEqualTo(4); // 2 headers + 4 data cells for numeric cols
        }

        @Test
        @UnitTest
@DisplayName("Should NOT right-align text columns")
        void shouldNotRightAlignTextColumns() {
            String markdown =
                "| Name   | Status |\n" +
                "|--------|--------|\n" +
                "| Alice  | Active |\n" +
                "| Bob    | Pending|";

            String result = MarkdownTableRenderer.renderTables(markdown);

            // Text columns should be left-aligned (default)
            // Count should be 0 or minimal for right alignment
            int rightAlignCount = countOccurrences(result, "text-align: right");
            assertThat(rightAlignCount).isZero();
        }

        @Test
        @UnitTest
@DisplayName("Should handle mixed columns correctly")
        void shouldHandleMixedColumns() {
            String markdown =
                "| Item    | Price  | Notes    |\n" +
                "|---------|--------|----------|\n" +
                "| Apple   | $1.50  | Fresh    |\n" +
                "| Orange  | $2.00  | Organic  |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            // Price column should be right-aligned (numeric with currency)
            // Item and Notes columns should be left-aligned
            assertThat(result).contains("text-align: right");
        }

        @Test
        @UnitTest
@DisplayName("Should format numbers according to locale in table")
        void shouldFormatNumbersInTable() {
            MarkdownTableRenderer.setLocale(Locale.US);

            String markdown =
                "| Item   | Amount   |\n" +
                "|--------|----------|\n" +
                "| Sales  | 1234567  |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            // Number should be formatted with US thousands separator
            assertThat(result).contains("1,234,567");
        }

        @Test
        @UnitTest
@DisplayName("Explicit markdown alignment should override auto-detection")
        void explicitAlignmentShouldOverride() {
            String markdown =
                "| Item   | Amount   |\n" +
                "|:-------|:--------:|\n" +  // Center alignment for Amount
                "| Sales  | 1234     |";

            String result = MarkdownTableRenderer.renderTables(markdown);

            // Amount column should be center-aligned per markdown, not right
            assertThat(result).contains("text-align: center");
        }
    }

    /**
     * Count occurrences of a substring in a string.
     */
    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
