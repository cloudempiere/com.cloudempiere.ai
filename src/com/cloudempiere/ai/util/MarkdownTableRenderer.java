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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Renders markdown tables to HTML for streaming display.
 *
 * <p>During streaming, markdown tables arrive incrementally. This renderer
 * provides a "skeleton" view of tables while they're being streamed, then
 * renders complete tables properly.
 *
 * <p><b>Problem Solved:</b>
 * <ul>
 *   <li>Markdown tables in streaming display as raw pipe-separated text</li>
 *   <li>Users see ugly unformatted tables until streaming completes</li>
 *   <li>marked.js only runs after streaming finishes</li>
 * </ul>
 *
 * <p><b>Solution:</b>
 * Parse and render tables in real-time during streaming, showing properly
 * formatted HTML tables even for partial/incomplete table content.
 *
 * <p><b>Supported Features:</b>
 * <ul>
 *   <li>GFM (GitHub Flavored Markdown) pipe tables</li>
 *   <li>Header rows with separator line (|---|---|)</li>
 *   <li>Left/center/right alignment via :---|:---:|---:</li>
 *   <li>Partial tables (incomplete rows during streaming)</li>
 *   <li>Mixed content (tables embedded in other text)</li>
 *   <li><b>Auto-detection of numeric columns</b> - right-aligned by default</li>
 *   <li><b>Locale-aware number formatting</b> - formats numbers per user locale</li>
 * </ul>
 *
 * <p><b>Numeric Detection:</b>
 * Numbers are automatically detected and right-aligned (best practice for tabular data).
 * Supported formats include:
 * <ul>
 *   <li>Integers: 1234, -567</li>
 *   <li>Decimals: 123.45, -0.99</li>
 *   <li>Currency: $1,234.56, €1.234,56</li>
 *   <li>Percentages: 45%, 12.5%</li>
 *   <li>Scientific: 1.23e10</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 1.1
 * @since CLD-1601
 */
public class MarkdownTableRenderer {

    /** Pattern to match a potential table row (pipe-separated) */
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile(
        "^\\|(.+)\\|\\s*$", Pattern.MULTILINE
    );

    /** Pattern to match the separator row (|---|---|) */
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile(
        "^\\|[:\\-\\|\\s]+\\|\\s*$", Pattern.MULTILINE
    );

    /** Pattern to detect if content contains any table-like structure */
    private static final Pattern HAS_TABLE_PATTERN = Pattern.compile(
        "\\|[^|]+\\|"
    );

    /**
     * Pattern to detect numeric values (integers, decimals, currency, percentages).
     * Matches:
     * - Plain numbers: 123, -456, 0.5, -0.99
     * - Thousands separators: 1,234 or 1.234 (European)
     * - Currency: $100, €50, £30, ¥1000
     * - Percentages: 45%, 12.5%
     * - Scientific notation: 1.23e10, 2E-5
     */
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
        "^\\s*" +                                    // Leading whitespace
        "([\\$€£¥₹]\\s*)?" +                        // Optional currency symbol
        "[-+]?" +                                    // Optional sign
        "(?:" +
            "\\d{1,3}(?:[,.]\\d{3})*(?:[,.]\\d+)?" + // Number with thousands separator
            "|" +
            "\\d+(?:[,.]\\d+)?" +                    // Simple number with optional decimal
        ")" +
        "(?:[eE][-+]?\\d+)?" +                      // Optional scientific notation
        "\\s*%?" +                                   // Optional percentage
        "\\s*$"                                      // Trailing whitespace
    );

    /** Thread-local locale for number formatting (can be set per-request) */
    private static final ThreadLocal<Locale> currentLocale = ThreadLocal.withInitial(() -> Locale.getDefault());

    /** CSS styles for rendered tables */
    private static final String TABLE_STYLE =
        "border-collapse: collapse; width: 100%; margin: 12px 0; font-size: 12px;";

    private static final String TH_STYLE =
        "border: 1px solid #ddd; padding: 8px 12px; background: #f5f5f5; " +
        "font-weight: 600; text-align: left;";

    private static final String TD_STYLE =
        "border: 1px solid #ddd; padding: 8px 12px; text-align: left;";

    /**
     * Set the locale to use for number formatting in the current thread.
     *
     * <p>This should be called at the start of request processing to ensure
     * numbers are formatted according to the user's locale preferences.
     *
     * @param locale the locale to use (if null, uses system default)
     */
    public static void setLocale(Locale locale) {
        currentLocale.set(locale != null ? locale : Locale.getDefault());
    }

    /**
     * Get the current locale for number formatting.
     *
     * @return the current locale
     */
    public static Locale getLocale() {
        return currentLocale.get();
    }

    /**
     * Clear the thread-local locale (call at end of request).
     */
    public static void clearLocale() {
        currentLocale.remove();
    }

    /**
     * Check if text contains any table-like content.
     *
     * @param text the text to check
     * @return true if the text appears to contain a markdown table
     */
    public static boolean containsTable(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return HAS_TABLE_PATTERN.matcher(text).find();
    }

    /**
     * Check if a cell value is numeric.
     *
     * @param value the cell value to check
     * @return true if the value appears to be a number
     */
    public static boolean isNumeric(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return NUMERIC_PATTERN.matcher(value).matches();
    }

    /**
     * Format a numeric value according to the current locale.
     *
     * <p>If the value is not a valid number, returns the original value.
     *
     * @param value the numeric string to format
     * @return formatted number string, or original if not parseable
     */
    public static String formatNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        String trimmed = value.trim();
        String prefix = "";
        String suffix = "";

        // Extract currency prefix
        if (trimmed.length() > 0) {
            char first = trimmed.charAt(0);
            if (first == '$' || first == '€' || first == '£' || first == '¥' || first == '₹') {
                prefix = String.valueOf(first) + " ";
                trimmed = trimmed.substring(1).trim();
            }
        }

        // Extract percentage suffix
        if (trimmed.endsWith("%")) {
            suffix = "%";
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }

        try {
            // Try to parse as a number (handle both comma and dot as decimal/thousands separators)
            Number number = parseFlexibleNumber(trimmed);
            if (number != null) {
                Locale locale = currentLocale.get();
                NumberFormat formatter;

                // Use appropriate formatter based on the original precision
                if (trimmed.contains(".") || trimmed.contains(",")) {
                    // Has decimal - preserve reasonable precision
                    formatter = NumberFormat.getNumberInstance(locale);
                    formatter.setMinimumFractionDigits(0);
                    formatter.setMaximumFractionDigits(4);
                } else {
                    // Integer
                    formatter = NumberFormat.getIntegerInstance(locale);
                }

                return prefix + formatter.format(number) + suffix;
            }
        } catch (Exception e) {
            // If parsing fails, return original
        }

        return value;
    }

    /**
     * Parse a number flexibly, handling various decimal/thousands separator conventions.
     */
    private static Number parseFlexibleNumber(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Remove any remaining whitespace and sign for analysis
        String cleaned = value.trim();
        boolean negative = cleaned.startsWith("-");
        if (negative || cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }

        // Count dots and commas
        int dotCount = cleaned.length() - cleaned.replace(".", "").length();
        int commaCount = cleaned.length() - cleaned.replace(",", "").length();

        try {
            // Determine which is the decimal separator
            if (dotCount == 1 && commaCount == 0) {
                // 123.45 - dot is decimal (US/UK style)
                double d = Double.parseDouble(value.replace(",", ""));
                return d;
            } else if (commaCount == 1 && dotCount == 0) {
                // 123,45 - comma is decimal (European style)
                double d = Double.parseDouble(value.replace(",", "."));
                return d;
            } else if (dotCount > 0 && commaCount > 0) {
                // Mixed: determine based on position
                int lastDot = cleaned.lastIndexOf('.');
                int lastComma = cleaned.lastIndexOf(',');

                if (lastDot > lastComma) {
                    // 1,234.56 - US style: comma is thousands, dot is decimal
                    double d = Double.parseDouble(value.replace(",", ""));
                    return d;
                } else {
                    // 1.234,56 - European style: dot is thousands, comma is decimal
                    double d = Double.parseDouble(value.replace(".", "").replace(",", "."));
                    return d;
                }
            } else if (commaCount > 1) {
                // 1,234,567 - commas are thousands separators
                double d = Double.parseDouble(value.replace(",", ""));
                return d;
            } else if (dotCount > 1) {
                // 1.234.567 - dots are thousands separators (European)
                double d = Double.parseDouble(value.replace(".", ""));
                return d;
            } else {
                // Plain integer
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Render markdown tables in the text to HTML.
     *
     * <p>Non-table content is left unchanged (except HTML escaping which
     * should be done by the caller).
     *
     * @param text the text containing potential markdown tables (already HTML-escaped)
     * @return text with tables converted to HTML
     */
    public static String renderTables(String text) {
        if (text == null || text.isEmpty() || !containsTable(text)) {
            return text;
        }

        // Split text into lines for processing
        String[] lines = text.split("\n", -1);
        StringBuilder result = new StringBuilder();

        int i = 0;
        while (i < lines.length) {
            String line = lines[i];

            // Check if this line starts a table
            if (isTableRow(line)) {
                // Try to parse a complete table starting here
                TableParseResult tableResult = parseTable(lines, i);
                if (tableResult.table != null && !tableResult.table.isEmpty()) {
                    result.append(renderTable(tableResult.table, tableResult.alignments));
                    i = tableResult.endIndex + 1;
                    continue;
                }
            }

            // Not a table line or couldn't parse table - keep as is
            result.append(line);
            if (i < lines.length - 1) {
                result.append("\n");
            }
            i++;
        }

        return result.toString();
    }

    /**
     * Check if a line looks like a table row.
     */
    private static boolean isTableRow(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length() > 2;
    }

    /**
     * Check if a line is a separator row.
     */
    private static boolean isSeparatorRow(String line) {
        if (line == null) {
            return false;
        }
        return SEPARATOR_PATTERN.matcher(line.trim()).matches();
    }

    /**
     * Parse a table starting at the given line index.
     *
     * @param lines all lines
     * @param startIndex where to start parsing
     * @return parse result with table data and end index
     */
    private static TableParseResult parseTable(String[] lines, int startIndex) {
        List<String[]> rows = new ArrayList<>();
        String[] alignments = null;
        int endIndex = startIndex;
        boolean foundSeparator = false;

        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();

            if (!isTableRow(line)) {
                // End of table
                endIndex = i - 1;
                break;
            }

            if (isSeparatorRow(line)) {
                // This is the separator row - extract alignments
                foundSeparator = true;
                alignments = parseAlignments(line);
                endIndex = i;
                continue;
            }

            // Regular data row
            String[] cells = parseCells(line);
            if (cells.length > 0) {
                rows.add(cells);
            }
            endIndex = i;
        }

        // For a valid table, we need at least a header row and separator
        // But for streaming, we'll render even partial tables
        if (rows.isEmpty()) {
            return new TableParseResult(null, null, startIndex);
        }

        return new TableParseResult(rows, alignments, endIndex);
    }

    /**
     * Parse cell contents from a table row.
     */
    private static String[] parseCells(String line) {
        // Remove leading/trailing pipes and split
        String content = line.trim();
        if (content.startsWith("|")) {
            content = content.substring(1);
        }
        if (content.endsWith("|")) {
            content = content.substring(0, content.length() - 1);
        }

        String[] cells = content.split("\\|", -1);
        for (int i = 0; i < cells.length; i++) {
            cells[i] = cells[i].trim();
        }
        return cells;
    }

    /**
     * Parse alignment indicators from separator row.
     */
    private static String[] parseAlignments(String line) {
        String[] cells = parseCells(line);
        String[] alignments = new String[cells.length];

        for (int i = 0; i < cells.length; i++) {
            String cell = cells[i].trim();
            boolean leftColon = cell.startsWith(":");
            boolean rightColon = cell.endsWith(":");

            if (leftColon && rightColon) {
                alignments[i] = "center";
            } else if (rightColon) {
                alignments[i] = "right";
            } else {
                alignments[i] = "left";
            }
        }
        return alignments;
    }

    /**
     * Render parsed table to HTML.
     *
     * <p>Automatically detects numeric columns and applies:
     * <ul>
     *   <li>Right alignment for numeric data (best practice)</li>
     *   <li>Locale-aware number formatting</li>
     * </ul>
     */
    private static String renderTable(List<String[]> rows, String[] alignments) {
        StringBuilder html = new StringBuilder();
        html.append("<table style='").append(TABLE_STYLE).append("'>");

        // Determine the maximum number of columns
        int maxCols = 0;
        for (String[] row : rows) {
            maxCols = Math.max(maxCols, row.length);
        }

        // Detect which columns are numeric (check data rows only, skip header)
        boolean[] numericColumns = new boolean[maxCols];
        if (rows.size() > 1) {
            // Initialize to true, then set to false if any non-numeric found
            for (int i = 0; i < maxCols; i++) {
                numericColumns[i] = true;
            }
            // Check data rows (skip first row which is header)
            for (int rowIdx = 1; rowIdx < rows.size(); rowIdx++) {
                String[] row = rows.get(rowIdx);
                for (int colIdx = 0; colIdx < row.length; colIdx++) {
                    String cell = row[colIdx];
                    // Empty cells don't disqualify a column from being numeric
                    if (cell != null && !cell.trim().isEmpty() && !isNumeric(cell)) {
                        numericColumns[colIdx] = false;
                    }
                }
            }
        }

        boolean isFirstRow = true;
        for (String[] row : rows) {
            html.append("<tr>");
            for (int i = 0; i < row.length; i++) {
                String cellContent = row[i];

                // Determine alignment:
                // 1. Explicit alignment from markdown separator takes precedence
                // 2. Auto-detect: numeric columns get right alignment
                // 3. Default: left alignment
                String align;
                if (alignments != null && i < alignments.length && !"left".equals(alignments[i])) {
                    // Explicit alignment specified in markdown
                    align = alignments[i];
                } else if (!isFirstRow && i < numericColumns.length && numericColumns[i]) {
                    // Auto-detect: numeric column gets right alignment
                    align = "right";
                } else {
                    align = "left";
                }

                // Format numeric values according to locale (data rows only)
                String displayContent = cellContent;
                if (!isFirstRow && isNumeric(cellContent)) {
                    displayContent = formatNumber(cellContent);
                }

                // HTML-escape cell content to prevent XSS
                String safeContent = escapeHtml(displayContent);

                if (isFirstRow) {
                    // Header row - also right-align headers for numeric columns
                    html.append("<th style='").append(TH_STYLE);
                    if (i < numericColumns.length && numericColumns[i]) {
                        html.append(" text-align: right;");
                    } else if (!"left".equals(align)) {
                        html.append(" text-align: ").append(align).append(";");
                    }
                    html.append("'>");
                    html.append(safeContent);
                    html.append("</th>");
                } else {
                    // Data row
                    html.append("<td style='").append(TD_STYLE);
                    if (!"left".equals(align)) {
                        html.append(" text-align: ").append(align).append(";");
                    }
                    html.append("'>");
                    html.append(safeContent);
                    html.append("</td>");
                }
            }
            html.append("</tr>");
            isFirstRow = false;
        }

        html.append("</table>");
        return html.toString();
    }

    /**
     * Escape HTML special characters to prevent XSS.
     *
     * @param text the text to escape
     * @return HTML-safe text
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * Result of parsing a table from lines.
     */
    private static class TableParseResult {
        final List<String[]> table;
        final String[] alignments;
        final int endIndex;

        TableParseResult(List<String[]> table, String[] alignments, int endIndex) {
            this.table = table;
            this.alignments = alignments;
            this.endIndex = endIndex;
        }
    }
}
