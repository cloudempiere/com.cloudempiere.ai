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
import java.util.Locale;
import java.util.Properties;

import org.compiere.util.CLogger;
import org.compiere.util.Util;

/**
 * Renders markdown tables incrementally during streaming, cell by cell.
 *
 * <p>Unlike {@link MarkdownTableRenderer} which renders complete tables,
 * this renderer maintains state across streaming chunks to provide smooth
 * cell-by-cell rendering as content arrives.
 *
 * <p><b>Problem Solved:</b>
 * <ul>
 *   <li>Default streaming shows tables appearing line by line (jarring)</li>
 *   <li>Better UX: cells appear one at a time as they're streamed</li>
 *   <li>Progressive rendering shows table structure immediately</li>
 * </ul>
 *
 * <p><b>Implementation:</b>
 * <ul>
 *   <li>Tracks current row and cell position across chunks</li>
 *   <li>Buffers incomplete cells until pipe delimiter arrives</li>
 *   <li>Renders partial rows with streaming cursor in current cell</li>
 *   <li>Handles separator row (|---|---) specially</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 * StreamingTableRenderer renderer = new StreamingTableRenderer();
 * renderer.setLocale(userLocale);
 * renderer.setContext(ctx, widgetId);
 *
 * // In streaming callback:
 * renderer.appendChunk(chunk);
 * String html = renderer.renderCurrentState();
 * updateUI(html);
 *
 * // When complete:
 * String finalHtml = renderer.renderFinal();
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class StreamingTableRenderer {

    private static final CLogger log = CLogger.getCLogger(StreamingTableRenderer.class);

    /** CSS styles for rendered tables */
    private static final String TABLE_STYLE =
        "border-collapse: collapse; width: 100%; margin: 12px 0; font-size: 12px;";

    private static final String TH_STYLE =
        "border: 1px solid #ddd; padding: 8px 12px; background: #f5f5f5; " +
        "font-weight: 600; text-align: left;";

    private static final String TD_STYLE =
        "border: 1px solid #ddd; padding: 8px 12px; text-align: left;";

    // ============= State Tracking =============

    /** Markdown renderer for pre-table and post-table content */
    private StreamingMarkdownRenderer preTableRenderer = new StreamingMarkdownRenderer();

    /** Markdown renderer for post-table content (created when table ends) */
    private StreamingMarkdownRenderer postTableRenderer = null;

    /** Buffered content before table */
    private StringBuilder preTableContent = new StringBuilder();

    /** Buffered content after table */
    private StringBuilder postTableContent = new StringBuilder();

    /** Are we currently inside a table? */
    private boolean inTable = false;

    /** Parsed table rows (complete rows) */
    private List<String[]> completedRows = new ArrayList<>();

    /** Current row being built */
    private List<String> currentRow = new ArrayList<>();

    /** Current cell being built */
    private StringBuilder currentCell = new StringBuilder();

    /** Alignment specifications from separator row */
    private String[] alignments = null;

    /** Is current row the separator row? */
    private boolean currentRowIsSeparator = false;

    /** Has separator been encountered? */
    private boolean separatorFound = false;

    /** Buffer for detecting table start/end */
    private StringBuilder lineBuffer = new StringBuilder();

    /** Track if we just started a new line (for table end detection) */
    private boolean lineStart = true;

    /** Locale for number formatting */
    private Locale locale = Locale.getDefault();

    /** Context for zoom links */
    private Properties ctx;

    /** Widget ID for zoom links */
    private String widgetId;

    /**
     * Create a new streaming table renderer.
     */
    public StreamingTableRenderer() {
        // Default constructor
    }

    /**
     * Set the locale for number formatting.
     *
     * @param locale the locale to use (if null, uses system default)
     */
    public void setLocale(Locale locale) {
        this.locale = locale != null ? locale : Locale.getDefault();
    }

    /**
     * Set context for zoom link processing.
     *
     * @param ctx iDempiere context
     * @param widgetId parent widget ID for zoom events
     */
    public void setContext(Properties ctx, String widgetId) {
        this.ctx = ctx;
        this.widgetId = widgetId;
    }

    /**
     * Append a streaming chunk.
     *
     * <p>This processes the chunk character by character, detecting:
     * <ul>
     *   <li>Table start (line starting with |)</li>
     *   <li>Cell boundaries (| delimiters)</li>
     *   <li>Row boundaries (newlines)</li>
     *   <li>Table end (line not starting with |)</li>
     * </ul>
     *
     * @param chunk text chunk to append
     */
    public void appendChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }

        for (int i = 0; i < chunk.length(); i++) {
            char ch = chunk.charAt(i);

            if (ch == '\n') {
                processLineEnd();
                lineStart = true; // Mark that we're starting a new line
                continue;
            }

            if (inTable) {
                // BUG FIX (CLD-1704): Check if this is first non-whitespace char on new line
                // If it's not a pipe, we've reached the end of the table
                if (lineStart) {
                    if (ch != '|' && !Character.isWhitespace(ch)) {
                        // First non-whitespace char is not a pipe - table has ended!
                        // Flush any pending row data before exiting table mode
                        if (currentCell.length() > 0 || !currentRow.isEmpty()) {
                            String cellContent = currentCell.toString().trim();
                            if (!cellContent.isEmpty()) {
                                currentRow.add(cellContent);
                            }
                            if (!currentRow.isEmpty() && !isSeparatorRow(currentRow)) {
                                completedRows.add(currentRow.toArray(new String[0]));
                            }
                            currentRow = new ArrayList<>();
                            currentCell.setLength(0);
                        }

                        inTable = false;
                        lineBuffer.append(ch);
                        lineStart = false;
                        continue;
                    } else if (!Character.isWhitespace(ch)) {
                        // First non-whitespace is a pipe - still in table
                        lineStart = false;
                    }
                    // If whitespace, keep lineStart=true until we see non-whitespace
                }

                // Use shared parser for consistent escape handling (ADR-047 - Single Source of Truth)
                TableCellParser.ParseResult parseResult = TableCellParser.parseChar(chunk, i);

                if (parseResult.isCellSeparator()) {
                    // Unescaped pipe - cell boundary
                    processCellBoundary();
                } else if (parseResult.shouldAppend()) {
                    // Normal character or escaped sequence - append to cell
                    currentCell.append(parseResult.getCharToAppend());
                }

                // Advance by number of characters consumed (1 for normal, 2 for escapes)
                i += parseResult.getCharsToSkip() - 1; // -1 because loop increments i
            } else {
                // Not in table mode - check if this starts a table
                if (ch == '|') {
                    // Pipe character encountered - check if it starts a line
                    String bufferedContent = lineBuffer.toString().trim();
                    if (bufferedContent.isEmpty()) {
                        // First non-whitespace char on line is pipe - start table!
                        inTable = true;
                        lineStart = false;

                        // Process any leading whitespace as pre-table content if this is first table
                        if (completedRows.isEmpty() && lineBuffer.length() > 0) {
                            String leadingContent = lineBuffer.toString();
                            preTableContent.append(leadingContent);
                            preTableRenderer.appendChunk(leadingContent);
                            lineBuffer.setLength(0);
                        }

                        // Process the pipe as cell boundary
                        processCellBoundary();
                    } else {
                        // Pipe in middle of line - not a table, just append
                        lineBuffer.append(ch);
                        lineStart = false;
                    }
                } else {
                    // Regular character - append to line buffer
                    lineBuffer.append(ch);
                    if (!Character.isWhitespace(ch)) {
                        lineStart = false;
                    }
                }
            }
        }
    }

    /**
     * Process cell boundary (| character).
     */
    private void processCellBoundary() {
        String cellContent = currentCell.toString().trim();

        // Skip empty leading pipe (start of row)
        if (cellContent.isEmpty() && currentRow.isEmpty()) {
            currentCell.setLength(0);
            return;
        }

        // Add cell to current row
        if (!cellContent.isEmpty() || !currentRow.isEmpty()) {
            currentRow.add(cellContent);
        }

        currentCell.setLength(0);
    }

    /**
     * Process end of line.
     */
    private void processLineEnd() {
        // Finish current cell if any content
        if (currentCell.length() > 0 || !currentRow.isEmpty()) {
            String cellContent = currentCell.toString().trim();
            if (!cellContent.isEmpty()) {
                currentRow.add(cellContent);
            }
            currentCell.setLength(0);
        }

        if (inTable && !currentRow.isEmpty()) {
            // Check if this is a separator row
            if (isSeparatorRow(currentRow)) {
                separatorFound = true;
                alignments = parseAlignments(currentRow);
                currentRowIsSeparator = false; // Don't include separator in rendered rows
            } else {
                // Complete row
                completedRows.add(currentRow.toArray(new String[0]));
            }
            currentRow = new ArrayList<>();
            // Stay in table mode - don't reset inTable here!
            // Table mode will end when we encounter non-table content (line not starting with |)
        } else if (inTable && currentRow.isEmpty()) {
            // Empty row while in table mode
            // Check if lineBuffer has non-table content (doesn't start with | after trim)
            String lineContent = lineBuffer.toString().trim();
            if (!lineContent.isEmpty() && !lineContent.startsWith("|")) {
                // Non-table content encountered - end table mode
                inTable = false;
                postTableContent.append(lineBuffer).append("\n");
                lineBuffer.setLength(0);
            }
            // Otherwise stay in table mode (empty line or next table row coming)
        } else if (!inTable) {
            // Content before/after table
            String line = lineBuffer.toString();
            if (completedRows.isEmpty()) {
                // Pre-table content
                preTableContent.append(line).append("\n");
                preTableRenderer.appendChunk(line + "\n");
            } else {
                // Post-table content
                postTableContent.append(line).append("\n");
                // Initialize post-table renderer if needed
                if (postTableRenderer == null) {
                    postTableRenderer = new StreamingMarkdownRenderer();
                }
                postTableRenderer.appendChunk(line + "\n");
            }
            lineBuffer.setLength(0);
        }

        // Clear lineBuffer if not already cleared
        if (lineBuffer.length() > 0 && !inTable) {
            lineBuffer.setLength(0);
        }
    }

    /**
     * Check if row is a separator row (contains only dashes, colons, pipes, spaces).
     */
    private boolean isSeparatorRow(List<String> row) {
        if (row.isEmpty()) {
            return false;
        }
        for (String cell : row) {
            if (!cell.matches("^[:\\-\\s]+$")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parse alignment specifications from separator row.
     */
    private String[] parseAlignments(List<String> row) {
        String[] aligns = new String[row.size()];
        for (int i = 0; i < row.size(); i++) {
            String cell = row.get(i).trim();
            boolean leftColon = cell.startsWith(":");
            boolean rightColon = cell.endsWith(":");

            if (leftColon && rightColon) {
                aligns[i] = "center";
            } else if (rightColon) {
                aligns[i] = "right";
            } else {
                aligns[i] = "left";
            }
        }
        return aligns;
    }

    /**
     * Render the current state (for streaming display).
     *
     * <p>Returns HTML showing:
     * <ul>
     *   <li>All completed rows</li>
     *   <li>Current partial row with streaming cursor in current cell</li>
     * </ul>
     *
     * @return HTML representation of current state
     */
    public String renderCurrentState() {
        StringBuilder html = new StringBuilder();

        // Pre-table content (use markdown renderer for progressive formatting)
        if (preTableRenderer.hasContent()) {
            html.append(preTableRenderer.renderCurrentState());
        }

        // Render table if we have any content
        if (!completedRows.isEmpty() || !currentRow.isEmpty() || currentCell.length() > 0) {
            html.append("<table style='").append(TABLE_STYLE).append("'>");

            // Determine if we're in header or data rows
            boolean inHeader = !separatorFound;

            // Render completed rows
            for (int rowIdx = 0; rowIdx < completedRows.size(); rowIdx++) {
                String[] row = completedRows.get(rowIdx);
                boolean isHeaderRow = (rowIdx == 0 && separatorFound);
                html.append(renderRow(row, isHeaderRow, false, -1));
            }

            // Render current incomplete row with cursor
            if (!currentRow.isEmpty() || currentCell.length() > 0) {
                boolean isHeaderRow = completedRows.isEmpty() && !separatorFound;
                int cursorCellIndex = currentRow.size();
                html.append(renderPartialRow(currentRow, currentCell.toString(), isHeaderRow, cursorCellIndex));
            }

            html.append("</table>");
        }

        // Post-table content (use markdown renderer for progressive formatting)
        if (postTableRenderer != null && postTableRenderer.hasContent()) {
            html.append(postTableRenderer.renderCurrentState());
        }

        return html.toString();
    }

    /**
     * Render a complete row.
     *
     * @param cells cell contents
     * @param isHeaderRow true if this is a header row
     * @param withCursor true to add cursor to last cell
     * @param cursorCellIndex which cell to put cursor in (-1 for none)
     */
    private String renderRow(String[] cells, boolean isHeaderRow, boolean withCursor, int cursorCellIndex) {
        StringBuilder html = new StringBuilder();
        html.append("<tr>");

        for (int i = 0; i < cells.length; i++) {
            String cellContent = cells[i];
            String align = getAlignment(i, isHeaderRow);

            // Format numbers if not header
            if (!isHeaderRow && MarkdownTableRenderer.isNumeric(cellContent)) {
                cellContent = MarkdownTableRenderer.formatNumber(cellContent);
            }

            // Process zoom links if context available
            if (ctx != null && widgetId != null) {
                int clientId = org.compiere.util.Env.getAD_Client_ID(ctx);
                log.warning("[STREAMING-ZOOM] Processing completed cell | AD_Client_ID=" + clientId + " | content=" + cellContent.substring(0, Math.min(50, cellContent.length())));
                cellContent = com.cloudempiere.ai.util.ZoomLinkProcessor.processZoomLinks(
                    cellContent, ctx, widgetId);
            }

            // Escape HTML unless it contains zoom links
            String safeContent = containsZoomLink(cellContent) ?
                cellContent : Util.maskHTML(cellContent, true);

            // Add cursor if this is the cursor cell
            if (withCursor && i == cursorCellIndex) {
                safeContent += "<span class='streaming-cursor'>|</span>";
            }

            if (isHeaderRow) {
                html.append("<th style='").append(TH_STYLE);
                if (!"left".equals(align)) {
                    html.append(" text-align: ").append(align).append(";");
                }
                html.append("'>").append(safeContent).append("</th>");
            } else {
                html.append("<td style='").append(TD_STYLE);
                if (!"left".equals(align)) {
                    html.append(" text-align: ").append(align).append(";");
                }
                html.append("'>").append(safeContent).append("</td>");
            }
        }

        html.append("</tr>");
        return html.toString();
    }

    /**
     * Render a partial row (currently being streamed).
     *
     * <p><b>Important (ADR-047):</b> Zoom link processing happens ONLY for completed cells,
     * not for the current cell being streamed. This prevents showing raw syntax
     * when link syntax is split across chunks.
     *
     * <p>Example:
     * <pre>
     * Chunk 1: "| [[C_BPartner:1000|Acm"  ← Shows as raw text with cursor
     * Chunk 2: "e Corp]] |"                ← Now complete, shows as link
     * </pre>
     *
     * @param completedCells cells that are complete in this row (will show zoom links)
     * @param currentCellContent content of cell currently being streamed (shows raw text)
     * @param isHeaderRow true if this is a header row
     * @param cursorCellIndex which cell to show cursor in
     */
    private String renderPartialRow(List<String> completedCells, String currentCellContent,
                                     boolean isHeaderRow, int cursorCellIndex) {
        StringBuilder html = new StringBuilder();
        html.append("<tr>");

        // Render completed cells
        for (int i = 0; i < completedCells.size(); i++) {
            String cellContent = completedCells.get(i);
            String align = getAlignment(i, isHeaderRow);

            // Format numbers if not header
            if (!isHeaderRow && MarkdownTableRenderer.isNumeric(cellContent)) {
                cellContent = MarkdownTableRenderer.formatNumber(cellContent);
            }

            // Process zoom links
            if (ctx != null && widgetId != null) {
                int clientId = org.compiere.util.Env.getAD_Client_ID(ctx);
                log.warning("[STREAMING-ZOOM] Processing partial row cell | AD_Client_ID=" + clientId + " | content=" + cellContent.substring(0, Math.min(50, cellContent.length())));
                cellContent = com.cloudempiere.ai.util.ZoomLinkProcessor.processZoomLinks(
                    cellContent, ctx, widgetId);
            }

            String safeContent = containsZoomLink(cellContent) ?
                cellContent : Util.maskHTML(cellContent, true);

            if (isHeaderRow) {
                html.append("<th style='").append(TH_STYLE);
                if (!"left".equals(align)) {
                    html.append(" text-align: ").append(align).append(";");
                }
                html.append("'>").append(safeContent).append("</th>");
            } else {
                html.append("<td style='").append(TD_STYLE);
                if (!"left".equals(align)) {
                    html.append(" text-align: ").append(align).append(";");
                }
                html.append("'>").append(safeContent).append("</td>");
            }
        }

        // Render current cell with cursor (NO zoom link processing - ADR-047)
        // Incomplete cells show raw text with cursor until pipe delimiter closes them.
        // Zoom links are processed only after cell is complete (see completed cells above).
        if (currentCellContent != null && !currentCellContent.trim().isEmpty()) {
            String align = getAlignment(completedCells.size(), isHeaderRow);
            String cellContent = currentCellContent.trim();

            // Escape HTML (show raw text with cursor - no zoom link processing yet)
            String safeContent = Util.maskHTML(cellContent, true);
            safeContent += "<span class='streaming-cursor'>|</span>";

            if (isHeaderRow) {
                html.append("<th style='").append(TH_STYLE);
                if (!"left".equals(align)) {
                    html.append(" text-align: ").append(align).append(";");
                }
                html.append("'>").append(safeContent).append("</th>");
            } else {
                html.append("<td style='").append(TD_STYLE);
                if (!"left".equals(align)) {
                    html.append(" text-align: ").append(align).append(";");
                }
                html.append("'>").append(safeContent).append("</td>");
            }
        }

        html.append("</tr>");
        return html.toString();
    }

    /**
     * Get alignment for column.
     */
    private String getAlignment(int colIndex, boolean isHeaderRow) {
        if (alignments != null && colIndex < alignments.length) {
            return alignments[colIndex];
        }
        return "left";
    }

    /**
     * Check if content contains zoom link HTML.
     */
    private boolean containsZoomLink(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.contains("class=\"ai-zoom-link\"") ||
               text.contains("class=\"ai-record-link\"");
    }

    /**
     * Render final state (use standard MarkdownTableRenderer for best quality).
     *
     * @return final HTML
     */
    public String renderFinal() {
        // Flush any pending state
        if (currentCell.length() > 0 || !currentRow.isEmpty()) {
            processLineEnd();
        }

        // Use standard renderer for final output
        StringBuilder fullText = new StringBuilder();
        fullText.append(preTableContent);

        if (!completedRows.isEmpty()) {
            // Reconstruct markdown table
            fullText.append("\n");
            for (int i = 0; i < completedRows.size(); i++) {
                String[] row = completedRows.get(i);
                fullText.append("| ");
                for (String cell : row) {
                    fullText.append(cell).append(" | ");
                }
                fullText.append("\n");

                // Add separator after first row if we found one
                if (i == 0 && separatorFound) {
                    fullText.append("|");
                    for (int j = 0; j < row.length; j++) {
                        String align = alignments != null && j < alignments.length ? alignments[j] : "left";
                        if ("center".equals(align)) {
                            fullText.append(":---:|");
                        } else if ("right".equals(align)) {
                            fullText.append("---:|");
                        } else {
                            fullText.append("----|");
                        }
                    }
                    fullText.append("\n");
                }
            }
            fullText.append("\n");
        }

        fullText.append(postTableContent);

        // Use MarkdownTableRenderer for final rendering
        MarkdownTableRenderer.setLocale(locale);
        MarkdownTableRenderer.setContext(ctx);
        MarkdownTableRenderer.setWidgetId(widgetId);
        try {
            return MarkdownTableRenderer.renderTables(fullText.toString());
        } finally {
            MarkdownTableRenderer.clearLocale();
            MarkdownTableRenderer.clearZoomContext();
        }
    }

    /**
     * Check if renderer is currently processing a table.
     *
     * @return true if inside a table
     */
    public boolean isInTable() {
        return inTable;
    }

    /**
     * Check if renderer has any table content to display.
     *
     * <p><b>BUG FIX (CLD-1704):</b> This returns true if the renderer has started processing
     * a table and has any content (completed rows, current row, or cells). This should be used
     * instead of {@link #isInTable()} for display decisions, because once a table starts,
     * we should continue using the table renderer for the entire message (pre-table, table,
     * post-table content), not switch back to plain text rendering.
     *
     * @return true if renderer has table content to display
     */
    public boolean hasContent() {
        return !completedRows.isEmpty() || !currentRow.isEmpty() || currentCell.length() > 0;
    }

    /**
     * Reset the renderer state.
     */
    public void reset() {
        preTableContent.setLength(0);
        postTableContent.setLength(0);
        preTableRenderer.reset();
        postTableRenderer = null;
        inTable = false;
        completedRows.clear();
        currentRow.clear();
        currentCell.setLength(0);
        alignments = null;
        currentRowIsSeparator = false;
        separatorFound = false;
        lineBuffer.setLength(0);
        lineStart = true;
    }
}
