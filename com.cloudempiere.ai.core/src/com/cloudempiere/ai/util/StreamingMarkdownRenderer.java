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

import java.util.Properties;
import java.util.Stack;

import org.compiere.util.CLogger;
import org.compiere.util.Util;

/**
 * Renders markdown incrementally during streaming, element by element.
 *
 * <p>Unlike final markdown rendering which processes complete markdown,
 * this renderer maintains state across streaming chunks to provide smooth
 * progressive rendering as content arrives - matching ChatGPT's UX.
 *
 * <p><b>Problem Solved:</b>
 * <ul>
 *   <li>Default streaming shows plain text only (jarring)</li>
 *   <li>Better UX: bold/italic/code appear as soon as closing markers arrive</li>
 *   <li>Progressive rendering shows formatted content immediately</li>
 * </ul>
 *
 * <p><b>Implementation:</b>
 * <ul>
 *   <li>State machine tracking current markdown context (bold, italic, code, etc.)</li>
 *   <li>Character-by-character parsing across chunk boundaries</li>
 *   <li>Immediate HTML emission when opening markers detected</li>
 *   <li>Buffers incomplete markers until closing marker arrives</li>
 * </ul>
 *
 * <p><b>Supported Elements:</b>
 * <ul>
 *   <li>**bold** → &lt;strong&gt;bold&lt;/strong&gt;</li>
 *   <li>*italic* or _italic_ → &lt;em&gt;italic&lt;/em&gt;</li>
 *   <li>`code` → &lt;code&gt;code&lt;/code&gt;</li>
 *   <li>```language\ncode\n``` → &lt;pre&gt;&lt;code&gt;code&lt;/code&gt;&lt;/pre&gt;</li>
 *   <li># Heading → &lt;h1&gt;Heading&lt;/h1&gt; (h1-h6)</li>
 * </ul>
 *
 * <p><b>Deferred to Other Components:</b>
 * <ul>
 *   <li>Tables: Use {@link StreamingTableRenderer}</li>
 *   <li>Zoom links: Post-processed in {@link ZoomLinkProcessor}</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 * StreamingMarkdownRenderer renderer = new StreamingMarkdownRenderer();
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
public class StreamingMarkdownRenderer {

    private static final CLogger log = CLogger.getCLogger(StreamingMarkdownRenderer.class);

    /**
     * Current parsing state.
     */
    private enum State {
        NORMAL,          // Plain text
        IN_BOLD,         // Inside ** markers
        IN_ITALIC,       // Inside * or _ markers
        IN_INLINE_CODE,  // Inside ` markers
        IN_CODE_BLOCK,   // Inside ``` markers
        IN_HEADING,      // After # at line start
        IN_TABLE,        // Inside a markdown table
        IN_TABLE_CELL    // Inside a table cell (between | delimiters)
    }

    // ============= State Tracking =============

    /** State stack for handling nested formatting (e.g., bold inside heading) */
    private Stack<State> stateStack = new Stack<>();

    /** Current parsing state (top of stack) */
    private State currentState = State.NORMAL;

    /** Buffer for detecting multi-character markers (**, ```, etc.) */
    private StringBuilder markerBuffer = new StringBuilder();

    /** Buffer for content within current markdown element */
    private StringBuilder contentBuffer = new StringBuilder();

    /** Accumulated HTML output */
    private StringBuilder htmlOutput = new StringBuilder();

    /** Track if we're at the start of a line (for heading detection) */
    private boolean isLineStart = true;

    /** Track last character for newline detection */
    private char lastChar = '\n';

    /** Heading level for current heading (1-6) */
    private int headingLevel = 0;

    /** Code block language hint (e.g., "java", "python") */
    private String codeBlockLanguage = null;

    /** Buffer for holding incomplete code blocks across chunks */
    private StringBuilder codeBlockBuffer = new StringBuilder();

    /** Flag indicating we're accumulating an incomplete code block */
    private boolean bufferingCodeBlock = false;

    /** Context for zoom link processing (passed through, not used during streaming) */
    private Properties ctx;

    /** Widget ID for zoom events (passed through, not used during streaming) */
    private String widgetId;

    // ============= Table State Tracking =============

    /** CSS styles for rendered tables */
    private static final String TABLE_STYLE =
        "border-collapse: collapse; width: 100%; margin: 12px 0; font-size: 12px;";

    private static final String TH_STYLE =
        "border: 1px solid #ddd; padding: 8px 12px; background: #f5f5f5; " +
        "font-weight: 600; text-align: left;";

    private static final String TD_STYLE =
        "border: 1px solid #ddd; padding: 8px 12px; text-align: left;";

    /** Parsed table rows (complete rows) */
    private java.util.List<String[]> tableCompletedRows = new java.util.ArrayList<>();

    /** Current table row being built */
    private java.util.List<String> tableCurrentRow = new java.util.ArrayList<>();

    /** Current table cell being built */
    private StringBuilder tableCurrentCell = new StringBuilder();

    /** Alignment specifications from separator row (:---, :---:, ---:) */
    private String[] tableAlignments = null;

    /** Has separator row been encountered? */
    private boolean tableSeparatorFound = false;

    /** Flag indicating table has opened <table> tag */
    private boolean tableOpened = false;

    /** Locale for number formatting in tables */
    private java.util.Locale locale = java.util.Locale.getDefault();

    /**
     * Create a new streaming markdown renderer.
     */
    public StreamingMarkdownRenderer() {
        // Initialize with NORMAL state
        stateStack.push(State.NORMAL);
    }

    /**
     * Push a new state onto the stack (entering nested formatting).
     */
    private void pushState(State newState) {
        stateStack.push(newState);
        currentState = newState;
    }

    /**
     * Pop current state from stack (exiting nested formatting).
     *
     * @return the state we're returning to
     */
    private State popState() {
        if (stateStack.size() > 1) {
            stateStack.pop();
            currentState = stateStack.peek();
        }
        return currentState;
    }

    /**
     * Get current state (top of stack).
     */
    private State getCurrentState() {
        return currentState;
    }

    /**
     * Set context for zoom link processing (used in final render only).
     *
     * @param ctx iDempiere context
     * @param widgetId parent widget ID for zoom events
     */
    public void setContext(Properties ctx, String widgetId) {
        this.ctx = ctx;
        this.widgetId = widgetId;
    }

    /**
     * Set locale for number formatting in tables.
     *
     * @param locale the locale to use (if null, uses system default)
     */
    public void setLocale(java.util.Locale locale) {
        this.locale = locale != null ? locale : java.util.Locale.getDefault();
    }

    /**
     * Append a streaming chunk.
     *
     * <p>Processes character-by-character, detecting markdown markers
     * and emitting HTML immediately when opening markers detected.
     *
     * <p><b>Code Block Protection:</b> If a chunk contains an incomplete
     * code block (odd number of ```), it will be buffered until the closing
     * marker arrives in a subsequent chunk. This prevents rendering errors
     * when code blocks span multiple streaming chunks.
     *
     * @param chunk text chunk to append
     */
    public void appendChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }

        // If we're buffering an incomplete code block, append to buffer
        if (bufferingCodeBlock) {
            codeBlockBuffer.append(chunk);

            // Check if buffer now has complete code block
            if (!hasIncompleteCodeBlock(codeBlockBuffer.toString())) {
                // Complete! Process buffered content
                String buffered = codeBlockBuffer.toString();
                codeBlockBuffer.setLength(0);
                bufferingCodeBlock = false;

                // Process complete content
                processChunkInternal(buffered);
            }
            // Otherwise keep buffering
            return;
        }

        // Check if this chunk starts a code block that's incomplete
        if (hasIncompleteCodeBlock(chunk)) {
            // Start buffering
            bufferingCodeBlock = true;
            codeBlockBuffer.append(chunk);
            return;
        }

        // Normal processing
        processChunkInternal(chunk);
    }

    /**
     * Internal chunk processing (character-by-character parsing).
     *
     * @param chunk text chunk to process
     */
    private void processChunkInternal(String chunk) {
        for (int i = 0; i < chunk.length(); i++) {
            char ch = chunk.charAt(i);
            processCharacter(ch);
            lastChar = ch;
        }
    }

    /**
     * Check if content has incomplete code block.
     *
     * <p>Counts occurrences of backtick runs (3+ consecutive backticks).
     * An odd count indicates an incomplete code block.
     *
     * <p><b>Why this matters:</b> If a streaming chunk ends with an incomplete
     * code block, we must buffer it until the closing marker arrives. Otherwise,
     * content after the opening ``` will be incorrectly treated as code, and
     * markdown in subsequent chunks will be rendered as literal text.
     *
     * <p><b>CommonMark Support:</b> Code blocks can use any number of backticks
     * (3 or more) as delimiters. For example:
     * <ul>
     *   <li>``` standard code block ```</li>
     *   <li>```` code with ``` inside ````</li>
     *   <li>`````` code with ````` inside ``````</li>
     * </ul>
     *
     * <p><b>Example:</b>
     * <pre>
     * Chunk 1: "Code: ```java\npublic class"  → Incomplete (1 run)
     * Chunk 2: " Example {}\n```\nDone!"       → Complete (2 runs)
     * Chunk 3: "Six: ``````\ncode\n``````"    → Complete (2 runs of 6)
     * </pre>
     *
     * @param content markdown content to check
     * @return true if code block is incomplete (odd number of backtick runs)
     */
    private boolean hasIncompleteCodeBlock(String content) {
        if (content == null || content.length() < 3) {
            return false;
        }

        int backtickRuns = 0;
        int i = 0;

        while (i < content.length()) {
            if (content.charAt(i) == '`') {
                // Count consecutive backticks
                int runLength = 0;
                while (i < content.length() && content.charAt(i) == '`') {
                    runLength++;
                    i++;
                }
                // Only count runs of 3+ as code block delimiters
                if (runLength >= 3) {
                    backtickRuns++;
                }
            } else {
                i++;
            }
        }

        // Odd count = incomplete (one opening without closing)
        return backtickRuns % 2 != 0;
    }

    /**
     * Process a single character based on current state.
     */
    private void processCharacter(char ch) {
        // Track line starts for heading detection
        if (lastChar == '\n') {
            isLineStart = true;
        }

        processCharacterInState(ch);

        // Update line start flag
        if (ch != ' ' && ch != '\t' && ch != '\r') {
            isLineStart = false;
        }
    }

    /**
     * Process character in current state (without updating line tracking flags).
     * Used for reprocessing characters after state transitions.
     */
    private void processCharacterInState(char ch) {
        switch (getCurrentState()) {
            case NORMAL:
                processNormalState(ch);
                break;

            case IN_BOLD:
                processInBold(ch);
                break;

            case IN_ITALIC:
                processInItalic(ch);
                break;

            case IN_INLINE_CODE:
                processInInlineCode(ch);
                break;

            case IN_CODE_BLOCK:
                processInCodeBlock(ch);
                break;

            case IN_HEADING:
                processInHeading(ch);
                break;

            case IN_TABLE:
                processInTable(ch);
                break;

            case IN_TABLE_CELL:
                processInTableCell(ch);
                break;
        }
    }

    /**
     * Process character in NORMAL state.
     */
    private void processNormalState(char ch) {
        if (ch == '*' || ch == '_') {
            markerBuffer.append(ch);
            // Don't emit yet - need to see if it's ** or just *
        } else if (ch == '-' && (isLineStart || isHorizontalRuleMarker())) {
            // Allow accumulating - markers for horizontal rule
            markerBuffer.append(ch);
            return; // Keep building --- marker
        } else if (ch == '`') {
            markerBuffer.append(ch);
            // Don't emit yet - need to see if it's ``` or just `
        } else if (ch == '#' && (isLineStart || isHeadingMarker())) {
            // Allow accumulating # markers for headings
            // Either at line start, or continuing a heading marker sequence
            markerBuffer.append(ch);
            // Keep isLineStart=true to allow multiple # characters
            return; // Skip the isLineStart=false at end of processCharacter
        } else if (ch == '|' && isLineStart) {
            // Pipe at line start - entering table mode
            // Open table tag if not already opened
            if (!tableOpened) {
                htmlOutput.append("<table style='").append(TABLE_STYLE).append("'>");
                tableOpened = true;
            }
            // Enter IN_TABLE state
            pushState(State.IN_TABLE);
            // Don't process the pipe as cell content - it's row start delimiter
            return;
        } else if (ch == '\n') {
            // Check if we have a horizontal rule marker (---)
            if (markerBuffer.toString().equals("---")) {
                htmlOutput.append("<hr/>");
                markerBuffer.setLength(0);
            } else {
                // Flush any pending marker as literal text
                flushMarkerAsText();
            }

            // Close heading if we're inside one
            if (getCurrentState() == State.IN_HEADING) {
                htmlOutput.append("</h").append(headingLevel).append(">");
                popState(); // Return to NORMAL
                headingLevel = 0;
            }

            htmlOutput.append("<br/>");
        } else {
            // Regular character - check if we should transition state first
            if (markerBuffer.length() > 0) {
                checkAndTransitionState(ch);
            }

            // If still in NORMAL (no transition), emit escaped char
            if (currentState == State.NORMAL && markerBuffer.length() == 0) {
                htmlOutput.append(escapeHtml(ch));
            }
        }
    }

    /**
     * Check if marker buffer contains only - characters (building a horizontal rule marker).
     */
    private boolean isHorizontalRuleMarker() {
        if (markerBuffer.length() == 0) {
            return false;
        }
        for (int i = 0; i < markerBuffer.length(); i++) {
            if (markerBuffer.charAt(i) != '-') {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if marker buffer contains only # characters (building a heading marker).
     */
    private boolean isHeadingMarker() {
        if (markerBuffer.length() == 0) {
            return false;
        }
        for (int i = 0; i < markerBuffer.length(); i++) {
            if (markerBuffer.charAt(i) != '#') {
                return false;
            }
        }
        return true;
    }

    /**
     * Check marker buffer and transition to appropriate state.
     * Called when a non-marker character is encountered.
     */
    private void checkAndTransitionState(char nextChar) {
        String marker = markerBuffer.toString();

        // Normalize excessive markers to valid patterns
        // Fixes: *****text***** (5 opening + 7 closing) → treat as ***text***
        if (marker.length() >= 5 && (marker.charAt(0) == '*' || marker.charAt(0) == '_')) {
            // Check if all characters are the same (all * or all _)
            char markerChar = marker.charAt(0);
            boolean allSame = true;
            for (int i = 1; i < marker.length(); i++) {
                if (marker.charAt(i) != markerChar) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) {
                // 5+ of same marker: treat as *** (bold+italic)
                marker = String.valueOf(markerChar) + markerChar + markerChar;
            }
        } else if (marker.length() == 4 && (marker.charAt(0) == '*' || marker.charAt(0) == '_')) {
            // Check if all characters are the same
            char markerChar = marker.charAt(0);
            if (marker.equals(String.valueOf(markerChar).repeat(4))) {
                // 4 of same marker: treat as ** (bold)
                marker = String.valueOf(markerChar) + markerChar;
            }
        }

        // Check for triple markers (bold+italic)
        // Process as: open bold, then open italic nested
        if (marker.equals("***") || marker.equals("___")) {
            // Open bold
            htmlOutput.append("<strong>");
            pushState(State.IN_BOLD);
            // Open italic (nested in bold)
            htmlOutput.append("<em>");
            pushState(State.IN_ITALIC);
            markerBuffer.setLength(0);
            // Emit the next char in new state
            htmlOutput.append(escapeHtml(nextChar));
        } else if (marker.equals("**") || marker.equals("__")) {
            // Open bold (push state)
            htmlOutput.append("<strong>");
            pushState(State.IN_BOLD);
            markerBuffer.setLength(0);
            // Emit the next char in new state
            htmlOutput.append(escapeHtml(nextChar));
        } else if (marker.equals("*") || marker.equals("_")) {
            // Open italic (push state)
            htmlOutput.append("<em>");
            pushState(State.IN_ITALIC);
            markerBuffer.setLength(0);
            // Emit the next char in new state
            htmlOutput.append(escapeHtml(nextChar));
        } else if (marker.equals("```")) {
            // Open code block (push state)
            // Check if next chars are language hint
            markerBuffer.setLength(0);
            pushState(State.IN_CODE_BLOCK);
            codeBlockLanguage = null;
            // Don't emit <pre><code> yet - collect language hint first
            contentBuffer.setLength(0);
            // FIX BUG #1: Preserve first character after ``` (start of language hint)
            // Without this, "java" becomes "ava" because 'j' is lost
            contentBuffer.append(nextChar);
        } else if (marker.equals("`")) {
            // Open inline code (push state)
            htmlOutput.append("<code>");
            pushState(State.IN_INLINE_CODE);
            markerBuffer.setLength(0);
            // Emit the next char in new state
            htmlOutput.append(escapeHtml(nextChar));
        } else if (marker.startsWith("#")) {
            // Heading - count level
            // FIXED: Validate heading requires space, tab, or newline after #
            // This prevents false positives like ###text (no space)
            if (nextChar == ' ' || nextChar == '\t' || nextChar == '\n') {
                headingLevel = marker.length();
                if (headingLevel > 6) headingLevel = 6;
                htmlOutput.append("<h").append(headingLevel).append(">");
                // Push IN_HEADING state to allow nested formatting
                pushState(State.IN_HEADING);
                markerBuffer.setLength(0);

                // Handle the next character appropriately
                if (nextChar == '\n') {
                    // Empty heading - close immediately
                    htmlOutput.append("</h").append(headingLevel).append("><br/>");
                    popState();
                    headingLevel = 0;
                }
                // Skip space/tab - don't add it to content
                // Next character will be processed in IN_HEADING state
            } else {
                // Not a valid heading (no space/tab/newline after #) - emit as literal text
                flushMarkerAsText();
                htmlOutput.append(escapeHtml(nextChar));
            }
        } else {
            // Not a valid marker - emit as literal text
            flushMarkerAsText();
            htmlOutput.append(escapeHtml(nextChar));
        }
    }

    /**
     * Process character in IN_BOLD state.
     *
     * <p>Bold can contain nested formatting (italic, code).
     */
    private void processInBold(char ch) {
        if (ch == '*' || ch == '_') {
            markerBuffer.append(ch);
            if (markerBuffer.length() == 2) {
                // Closing ** or __ found - close tag
                htmlOutput.append("</strong>");
                popState(); // Return to parent state (NORMAL or IN_HEADING)
                markerBuffer.setLength(0);
                contentBuffer.setLength(0);
            } else if (markerBuffer.length() == 1) {
                // Single * or _ - might be italic, wait for next char
            }
        } else if (ch == '`') {
            // Process nested code
            // First flush any pending single * or _ as literal or nested
            if (markerBuffer.length() == 1) {
                // Single marker before ` - could be nested italic or literal
                markerBuffer.append(ch);
                // Will be processed on next char
                return;
            }
            markerBuffer.append(ch);
            // Will be processed on next char
        } else {
            // Regular character - check for nested formatting
            if (markerBuffer.length() > 0) {
                checkNestedFormattingInBold(ch);
            } else {
                // Emit escaped char directly
                htmlOutput.append(escapeHtml(ch));
            }
        }
    }

    /**
     * Check for nested formatting inside bold (italic, code).
     */
    private void checkNestedFormattingInBold(char nextChar) {
        String marker = markerBuffer.toString();

        if (marker.equals("*") || marker.equals("_")) {
            // Open italic (push state)
            htmlOutput.append("<em>");
            pushState(State.IN_ITALIC);
            markerBuffer.setLength(0);
            // Emit the next char in new state (will be processed by processInItalic next)
            htmlOutput.append(escapeHtml(nextChar));
        } else if (marker.equals("`")) {
            // Open inline code (push state)
            htmlOutput.append("<code>");
            pushState(State.IN_INLINE_CODE);
            markerBuffer.setLength(0);
            // Emit the next char in new state (will be processed by processInInlineCode next)
            htmlOutput.append(escapeHtml(nextChar));
        } else {
            // Not a valid marker - emit as literal text
            flushMarkerAsText();
            htmlOutput.append(escapeHtml(nextChar));
        }
    }

    /**
     * Process character in IN_ITALIC state.
     *
     * <p>Italic can contain nested formatting (code).
     */
    private void processInItalic(char ch) {
        if (ch == '*' || ch == '_') {
            // Close italic on first matching marker
            htmlOutput.append("</em>");
            popState(); // Return to parent state (NORMAL, IN_HEADING, or IN_BOLD)
            contentBuffer.setLength(0);
            // DON'T clear markerBuffer - let parent state see this marker
            // If parent is IN_BOLD, it will accumulate for **
            // Reprocess this character in the new state
            processCharacterInState(ch);
        } else if (ch == '`') {
            // Process nested code
            if (markerBuffer.length() > 0) {
                // Flush any pending marker as literal
                flushMarkerAsText();
            }
            markerBuffer.append(ch);
            // Will be processed on next char
        } else {
            // Regular character - check for nested formatting
            if (markerBuffer.length() > 0) {
                // We had partial marker - check if it's code
                if (markerBuffer.toString().equals("`")) {
                    checkNestedFormattingInItalic(ch);
                } else {
                    // Not a valid marker - emit as literal text
                    flushMarkerAsText();
                    htmlOutput.append(escapeHtml(ch));
                }
            } else {
                // Emit escaped char directly
                htmlOutput.append(escapeHtml(ch));
            }
        }
    }

    /**
     * Check for nested formatting inside italic (code).
     */
    private void checkNestedFormattingInItalic(char nextChar) {
        String marker = markerBuffer.toString();

        if (marker.equals("`")) {
            // Open inline code (push state)
            htmlOutput.append("<code>");
            pushState(State.IN_INLINE_CODE);
            markerBuffer.setLength(0);
            // Emit the next char in new state
            htmlOutput.append(escapeHtml(nextChar));
        } else {
            // Not a valid marker - emit as literal text
            flushMarkerAsText();
            htmlOutput.append(escapeHtml(nextChar));
        }
    }

    /**
     * Process character in IN_INLINE_CODE state.
     *
     * <p>Code is literal - no nested formatting inside code.
     */
    private void processInInlineCode(char ch) {
        if (ch == '`') {
            // Closing ` found - close tag
            htmlOutput.append("</code>");
            popState(); // Return to parent state (NORMAL, IN_HEADING, IN_BOLD, or IN_ITALIC)
            contentBuffer.setLength(0);
        } else {
            // Emit escaped char directly (code is literal)
            htmlOutput.append(escapeHtml(ch));
        }
    }

    /**
     * Process character in IN_CODE_BLOCK state.
     */
    private void processInCodeBlock(char ch) {
        if (ch == '`') {
            markerBuffer.append(ch);
            if (markerBuffer.length() == 3) {
                // Closing ``` found

                // Emit opening tag if not yet emitted
                if (codeBlockLanguage == null && contentBuffer.length() == 0) {
                    // Just entered code block, no content yet
                    htmlOutput.append("<pre><code>");
                } else if (codeBlockLanguage == null) {
                    // Check if first line is language hint
                    String firstLine = contentBuffer.toString();
                    int newlineIdx = firstLine.indexOf('\n');
                    if (newlineIdx > 0 && newlineIdx < 20) {
                        codeBlockLanguage = firstLine.substring(0, newlineIdx).trim();
                        htmlOutput.append("<pre><code class='language-")
                                  .append(escapeHtml(codeBlockLanguage))
                                  .append("'>");
                        // Emit content after language line
                        htmlOutput.append(escapeHtml(firstLine.substring(newlineIdx + 1)));
                    } else {
                        htmlOutput.append("<pre><code>");
                        htmlOutput.append(escapeHtml(contentBuffer.toString()));
                    }
                } else {
                    // Already emitted opening tag, emit remaining content
                    htmlOutput.append(escapeHtml(contentBuffer.toString()));
                }

                // Close code block
                htmlOutput.append("</code></pre>");
                popState(); // Return to NORMAL
                markerBuffer.setLength(0);
                contentBuffer.setLength(0);
                codeBlockLanguage = null;
            }
        } else {
            // Flush any partial ``` as content
            if (markerBuffer.length() > 0) {
                for (int i = 0; i < markerBuffer.length(); i++) {
                    contentBuffer.append('`');
                }
                markerBuffer.setLength(0);
            }

            // Check if we should emit opening tag (on first newline after ```)
            if (codeBlockLanguage == null && ch == '\n' && contentBuffer.length() > 0) {
                // First line is language hint
                codeBlockLanguage = contentBuffer.toString().trim();
                if (codeBlockLanguage.isEmpty()) {
                    htmlOutput.append("<pre><code>");
                } else {
                    htmlOutput.append("<pre><code class='language-")
                              .append(escapeHtml(codeBlockLanguage))
                              .append("'>");
                }
                contentBuffer.setLength(0);
            } else {
                contentBuffer.append(ch);
            }
        }
    }

    /**
     * Process character in IN_HEADING state.
     *
     * <p>Headings can contain nested formatting (bold, italic, code).
     * Process markers normally and close heading on newline.
     */
    private void processInHeading(char ch) {
        if (ch == '\n') {
            // Flush any pending markers
            flushMarkerAsText();

            // End of heading - close tag and pop state
            htmlOutput.append("</h").append(headingLevel).append(">");
            htmlOutput.append("<br/>");
            popState(); // Return to NORMAL
            headingLevel = 0;
        } else if (ch == '*' || ch == '_') {
            markerBuffer.append(ch);
            // Will be processed on next non-marker char
        } else if (ch == '`') {
            markerBuffer.append(ch);
            // Will be processed on next non-marker char
        } else {
            // Regular character - check if we should transition state first
            if (markerBuffer.length() > 0) {
                checkAndTransitionStateInHeading(ch);
            } else {
                // Emit escaped char
                htmlOutput.append(escapeHtml(ch));
            }
        }
    }

    /**
     * Check for nested formatting transitions inside headings.
     * Called when inside IN_HEADING state.
     */
    private void checkAndTransitionStateInHeading(char nextChar) {
        String marker = markerBuffer.toString();

        // Normalize excessive markers to valid patterns (same as checkAndTransitionState)
        if (marker.length() >= 5 && (marker.charAt(0) == '*' || marker.charAt(0) == '_')) {
            char markerChar = marker.charAt(0);
            boolean allSame = true;
            for (int i = 1; i < marker.length(); i++) {
                if (marker.charAt(i) != markerChar) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) {
                marker = String.valueOf(markerChar) + markerChar + markerChar;
            }
        } else if (marker.length() == 4 && (marker.charAt(0) == '*' || marker.charAt(0) == '_')) {
            char markerChar = marker.charAt(0);
            if (marker.equals(String.valueOf(markerChar).repeat(4))) {
                marker = String.valueOf(markerChar) + markerChar;
            }
        }

        // Check for triple markers (bold+italic)
        if (marker.equals("***") || marker.equals("___")) {
            // Open bold, then italic (nested)
            htmlOutput.append("<strong><em>");
            pushState(State.IN_BOLD);
            pushState(State.IN_ITALIC);
            markerBuffer.setLength(0);
            // Emit the next char in new state
            htmlOutput.append(escapeHtml(nextChar));
        } else if (marker.equals("**") || marker.equals("__")) {
            // Open bold (push state)
            htmlOutput.append("<strong>");
            pushState(State.IN_BOLD);
            markerBuffer.setLength(0);
            // Emit the next char in new state
            htmlOutput.append(escapeHtml(nextChar));
        } else if (marker.equals("*") || marker.equals("_")) {
            // Open italic (push state)
            htmlOutput.append("<em>");
            pushState(State.IN_ITALIC);
            markerBuffer.setLength(0);
            // Emit the next char in new state
            htmlOutput.append(escapeHtml(nextChar));
        } else if (marker.equals("`")) {
            // Open inline code (push state)
            htmlOutput.append("<code>");
            pushState(State.IN_INLINE_CODE);
            markerBuffer.setLength(0);
            // Emit the next char in new state
            htmlOutput.append(escapeHtml(nextChar));
        } else {
            // Not a valid marker - emit as literal text
            flushMarkerAsText();
            htmlOutput.append(escapeHtml(nextChar));
        }
    }

    /**
     * Process character in IN_TABLE state.
     *
     * <p>Handles table row and cell boundaries while building table structure.
     */
    private void processInTable(char ch) {
        // Check for table end: non-pipe character at line start
        if (isLineStart && ch != '|' && !Character.isWhitespace(ch)) {
            // Table has ended - close table tag and return to NORMAL
            closeTable();

            // Re-process this character in NORMAL state
            popState(); // Return to NORMAL
            processCharacterInState(ch);
            return;
        }

        if (ch == '|') {
            // Cell boundary - complete current cell and start new one
            completeTableCell();
            // Continue in IN_TABLE state
        } else if (ch == '\n') {
            // End of row - complete row and check for table end
            completeTableRow();
        } else if (ch == '\\') {
            // Possible escape sequence - peek next char
            // For now, add to cell content (will be handled in processInTableCell)
            tableCurrentCell.append(ch);
        } else {
            // Regular cell content
            tableCurrentCell.append(ch);
        }
    }

    /**
     * Close table tag and reset table state.
     */
    private void closeTable() {
        if (tableOpened) {
            htmlOutput.append("</table>");
            tableOpened = false;
        }

        // Reset table state
        tableCompletedRows.clear();
        tableCurrentRow.clear();
        tableCurrentCell.setLength(0);
        tableAlignments = null;
        tableSeparatorFound = false;
    }

    /**
     * Process character in IN_TABLE_CELL state.
     * (Currently unused - we handle cells directly in IN_TABLE)
     */
    private void processInTableCell(char ch) {
        // For now, delegate to processInTable
        processInTable(ch);
    }

    /**
     * Complete current table cell and emit HTML.
     */
    private void completeTableCell() {
        String cellContent = tableCurrentCell.toString().trim();

        // Skip empty leading cell (table row start pipe)
        if (cellContent.isEmpty() && tableCurrentRow.isEmpty()) {
            tableCurrentCell.setLength(0);
            return;
        }

        // Add cell to current row
        tableCurrentRow.add(cellContent);
        tableCurrentCell.setLength(0);
    }

    /**
     * Complete current table row and emit HTML.
     */
    private void completeTableRow() {
        // Complete any pending cell
        if (tableCurrentCell.length() > 0 || !tableCurrentRow.isEmpty()) {
            String cellContent = tableCurrentCell.toString().trim();
            if (!cellContent.isEmpty()) {
                tableCurrentRow.add(cellContent);
            }
            tableCurrentCell.setLength(0);
        }

        // Check if this is a separator row (|---|---|)
        if (isSeparatorRow(tableCurrentRow)) {
            tableSeparatorFound = true;
            tableAlignments = parseAlignments(tableCurrentRow);
            // Don't emit separator row as HTML
            tableCurrentRow = new java.util.ArrayList<>();
            return;
        }

        // Emit row HTML if we have content
        if (!tableCurrentRow.isEmpty()) {
            boolean isHeaderRow = (!tableSeparatorFound && tableCompletedRows.isEmpty());
            emitTableRow(tableCurrentRow, isHeaderRow);

            // Store completed row
            tableCompletedRows.add(tableCurrentRow.toArray(new String[0]));
            tableCurrentRow = new java.util.ArrayList<>();
        }

        // No <br/> needed - table rows are self-contained
        // Stay in IN_TABLE state - will exit when non-table content detected
    }

    /**
     * Check if row is a separator row (contains only dashes, colons, pipes, spaces).
     */
    private boolean isSeparatorRow(java.util.List<String> row) {
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
    private String[] parseAlignments(java.util.List<String> row) {
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
     * Emit HTML for a table row.
     */
    private void emitTableRow(java.util.List<String> row, boolean isHeaderRow) {
        // Open row
        htmlOutput.append("<tr>");

        for (int i = 0; i < row.size(); i++) {
            String cellContent = row.get(i);
            String align = getTableAlignment(i);

            // Escape HTML
            String safeContent = escapeHtml(cellContent);

            if (isHeaderRow) {
                htmlOutput.append("<th style='").append(TH_STYLE);
                if (!"left".equals(align)) {
                    htmlOutput.append(" text-align: ").append(align).append(";");
                }
                htmlOutput.append("'>").append(safeContent).append("</th>");
            } else {
                htmlOutput.append("<td style='").append(TD_STYLE);
                if (!"left".equals(align)) {
                    htmlOutput.append(" text-align: ").append(align).append(";");
                }
                htmlOutput.append("'>").append(safeContent).append("</td>");
            }
        }
        // Close row
        htmlOutput.append("</tr>");
    }

    /**
     * Get alignment for column.
     */
    private String getTableAlignment(int colIndex) {
        if (tableAlignments != null && colIndex < tableAlignments.length) {
            return tableAlignments[colIndex];
        }
        return "left";
    }

    /**
     * Flush marker buffer as literal text (when it's not a valid marker).
     */
    private void flushMarkerAsText() {
        if (markerBuffer.length() > 0) {
            for (int i = 0; i < markerBuffer.length(); i++) {
                htmlOutput.append(escapeHtml(markerBuffer.charAt(i)));
            }
            markerBuffer.setLength(0);
        }
    }

    /**
     * Flush any pending content (for completion).
     * Closes all open tags from the state stack.
     */
    private void flushPendingContent() {
        // If we have buffered code block content, process it now
        // This handles the edge case where streaming ends mid-code-block
        if (bufferingCodeBlock && codeBlockBuffer.length() > 0) {
            // Force process incomplete code block as literal text
            // (since we never got the closing ```)
            String buffered = codeBlockBuffer.toString();
            codeBlockBuffer.setLength(0);
            bufferingCodeBlock = false;

            // Process as-is (will be treated as literal since incomplete)
            processChunkInternal(buffered);
        }

        // Flush marker buffer as literal text
        flushMarkerAsText();

        // Close all open states (from innermost to outermost)
        while (stateStack.size() > 1) {
            State state = getCurrentState();

            if (state == State.IN_BOLD) {
                // Content already emitted to htmlOutput
                htmlOutput.append("</strong>");
                contentBuffer.setLength(0);
            } else if (state == State.IN_ITALIC) {
                // Content already emitted to htmlOutput
                htmlOutput.append("</em>");
                contentBuffer.setLength(0);
            } else if (state == State.IN_INLINE_CODE) {
                // Content already emitted to htmlOutput
                htmlOutput.append("</code>");
                contentBuffer.setLength(0);
            } else if (state == State.IN_CODE_BLOCK) {
                // Emit opening tag if not yet emitted
                if (codeBlockLanguage == null && contentBuffer.length() > 0) {
                    // Check if first line is language hint
                    String content = contentBuffer.toString();
                    int newlineIdx = content.indexOf('\n');
                    if (newlineIdx > 0 && newlineIdx < 20) {
                        codeBlockLanguage = content.substring(0, newlineIdx).trim();
                        htmlOutput.append("<pre><code class='language-")
                                  .append(escapeHtml(codeBlockLanguage))
                                  .append("'>");
                        htmlOutput.append(escapeHtml(content.substring(newlineIdx + 1)));
                    } else {
                        htmlOutput.append("<pre><code>");
                        htmlOutput.append(escapeHtml(content));
                    }
                } else if (contentBuffer.length() > 0) {
                    htmlOutput.append(escapeHtml(contentBuffer.toString()));
                }
                htmlOutput.append("</code></pre>");
                contentBuffer.setLength(0);
            } else if (state == State.IN_HEADING) {
                htmlOutput.append("</h").append(headingLevel).append(">");
                headingLevel = 0;
            } else if (state == State.IN_TABLE || state == State.IN_TABLE_CELL) {
                // Complete any pending table content
                if (tableCurrentCell.length() > 0 || !tableCurrentRow.isEmpty()) {
                    completeTableRow();
                }
                // Close table
                closeTable();
            }

            popState();
        }

        contentBuffer.setLength(0);
    }

    /**
     * Render the current state (for streaming display).
     *
     * <p>Returns HTML showing accumulated content with cursor position.
     *
     * @return HTML representation of current state
     */
    public String renderCurrentState() {
        StringBuilder result = new StringBuilder(htmlOutput);

        // Append any buffered content (incomplete markdown)
        if (markerBuffer.length() > 0) {
            // Show marker as literal text during streaming
            result.append(escapeHtml(markerBuffer.toString()));
        }

        // Append content buffer if we're inside a markdown element
        if (contentBuffer.length() > 0) {
            result.append(escapeHtml(contentBuffer.toString()));
        }

        return result.toString();
    }

    /**
     * Render final state (for completion).
     *
     * <p><b>Important:</b> Does NOT re-parse markdown. Returns accumulated HTML
     * from streaming, with any pending content flushed.
     *
     * @return final HTML (no re-parsing, just what was built during streaming)
     */
    public String renderFinal() {
        // Flush any pending state
        flushPendingContent();

        // Return accumulated HTML as-is (no re-parsing)
        return htmlOutput.toString();
    }

    /**
     * Reset the renderer state.
     */
    public void reset() {
        stateStack.clear();
        stateStack.push(State.NORMAL);
        currentState = State.NORMAL;
        markerBuffer.setLength(0);
        contentBuffer.setLength(0);
        htmlOutput.setLength(0);
        isLineStart = true;
        lastChar = '\n';
        headingLevel = 0;
        codeBlockLanguage = null;

        // Reset table state
        tableCompletedRows.clear();
        tableCurrentRow.clear();
        tableCurrentCell.setLength(0);
        tableAlignments = null;
        tableSeparatorFound = false;
        tableOpened = false;
    }

    /**
     * Check if renderer has any content.
     *
     * @return true if any content has been processed
     */
    public boolean hasContent() {
        return htmlOutput.length() > 0 ||
               markerBuffer.length() > 0 ||
               contentBuffer.length() > 0 ||
               codeBlockBuffer.length() > 0;
    }

    /**
     * Escape HTML special characters.
     *
     * @param ch character to escape
     * @return escaped HTML string
     */
    private String escapeHtml(char ch) {
        // Fast path for common characters
        switch (ch) {
            case '<': return "&lt;";
            case '>': return "&gt;";
            case '&': return "&amp;";
            case '"': return "&quot;";
            case '\'': return "&#39;";
            default: return String.valueOf(ch);
        }
    }

    /**
     * Escape HTML special characters while preserving UTF-8 (emojis).
     *
     * <p>Unlike {@link Util#maskHTML(String, boolean)}, this preserves
     * UTF-8 characters like emojis instead of mangling them.
     *
     * @param text text to escape
     * @return escaped HTML string
     */
    private String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Manual HTML escaping to preserve UTF-8 (emojis)
        StringBuilder escaped = new StringBuilder(text.length() + 20);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '&':
                    escaped.append("&amp;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#39;");
                    break;
                default:
                    // Preserve all other characters including UTF-8 emojis
                    escaped.append(ch);
                    break;
            }
        }
        return escaped.toString();
    }
}
