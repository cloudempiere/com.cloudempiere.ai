package com.cloudempiere.ai.util;

/**
 * Shared utility for parsing markdown table cell content.
 *
 * <p>Provides consistent escape handling for both streaming and final table rendering.
 * This is the single source of truth for markdown table cell parsing logic (ADR-047).
 *
 * <p><b>Escape Handling:</b>
 * <ul>
 *   <li><code>\|</code> → <code>|</code> (literal pipe, not cell separator)</li>
 *   <li><code>\\</code> → <code>\</code> (escaped backslash)</li>
 *   <li>Other backslash sequences preserved as-is</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * String content = "Name: [[C_BPartner:123\\|Acme Corp]]";
 * int i = 0;
 * StringBuilder cell = new StringBuilder();
 *
 * while (i < content.length()) {
 *     char ch = content.charAt(i);
 *     ParseResult result = TableCellParser.parseChar(content, i);
 *
 *     if (result.shouldAppend()) {
 *         cell.append(result.getCharToAppend());
 *     }
 *
 *     i += result.getCharsToSkip();
 * }
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 * @see StreamingTableRenderer
 * @see MarkdownTableRenderer
 */
public class TableCellParser {

    /**
     * Result of parsing a single character position in markdown content.
     *
     * <p>Indicates what character (if any) should be appended to the cell,
     * and how many positions to advance in the input stream.
     */
    public static class ParseResult {
        private final boolean shouldAppend;
        private final char charToAppend;
        private final int charsToSkip;
        private final boolean isCellSeparator;

        private ParseResult(boolean shouldAppend, char charToAppend, int charsToSkip, boolean isCellSeparator) {
            this.shouldAppend = shouldAppend;
            this.charToAppend = charToAppend;
            this.charsToSkip = charsToSkip;
            this.isCellSeparator = isCellSeparator;
        }

        /**
         * Create result for appending a character
         * @param ch character to append
         * @param skip number of positions to advance (1 = normal, 2 = consumed escape sequence)
         */
        public static ParseResult append(char ch, int skip) {
            return new ParseResult(true, ch, skip, false);
        }

        /**
         * Create result for skipping character(s) without appending
         * @param skip number of positions to advance
         */
        public static ParseResult skip(int skip) {
            return new ParseResult(false, '\0', skip, false);
        }

        /**
         * Create result indicating a cell separator pipe was found
         */
        public static ParseResult cellSeparator() {
            return new ParseResult(false, '\0', 1, true);
        }

        public boolean shouldAppend() { return shouldAppend; }
        public char getCharToAppend() { return charToAppend; }
        public int getCharsToSkip() { return charsToSkip; }
        public boolean isCellSeparator() { return isCellSeparator; }
    }

    /**
     * Parse character at given position, handling markdown escapes.
     *
     * <p><b>Escape Rules:</b>
     * <ul>
     *   <li><code>\|</code> → Append <code>|</code>, skip 2 chars (backslash and pipe)</li>
     *   <li><code>\\</code> → Append <code>\</code>, skip 2 chars (both backslashes)</li>
     *   <li><code>|</code> (unescaped) → Cell separator, don't append, skip 1 char</li>
     *   <li>Other chars → Append as-is, skip 1 char</li>
     * </ul>
     *
     * @param content full content string being parsed
     * @param position current position (index of character to parse)
     * @return ParseResult indicating what to append and how many positions to advance
     */
    public static ParseResult parseChar(String content, int position) {
        if (position >= content.length()) {
            return ParseResult.skip(1);
        }

        char ch = content.charAt(position);

        // Check for backslash escape sequences
        if (ch == '\\' && position + 1 < content.length()) {
            char nextChar = content.charAt(position + 1);

            if (nextChar == '|') {
                // Backslash-escaped pipe: \| → |
                // This is a literal pipe character, not a cell separator
                return ParseResult.append('|', 2);
            } else if (nextChar == '\\') {
                // Backslash-escaped backslash: \\ → \
                return ParseResult.append('\\', 2);
            } else {
                // Other backslash sequences - keep backslash as-is
                return ParseResult.append(ch, 1);
            }
        }

        // Check for unescaped pipe (cell separator)
        if (ch == '|') {
            return ParseResult.cellSeparator();
        }

        // Normal character - append as-is
        return ParseResult.append(ch, 1);
    }

    /**
     * Check if character at position is an escaped pipe (\|).
     *
     * <p>Quick check without creating ParseResult object.
     *
     * @param content full content string
     * @param position current position
     * @return true if this is \| escape sequence
     */
    public static boolean isEscapedPipe(String content, int position) {
        return position + 1 < content.length()
            && content.charAt(position) == '\\'
            && content.charAt(position + 1) == '|';
    }

    /**
     * Check if character at position is an unescaped pipe (cell separator).
     *
     * @param content full content string
     * @param position current position
     * @return true if this is an unescaped pipe
     */
    public static boolean isCellSeparator(String content, int position) {
        if (position >= content.length()) {
            return false;
        }

        char ch = content.charAt(position);

        // Not a pipe at all
        if (ch != '|') {
            return false;
        }

        // Check if it's escaped
        if (position > 0 && content.charAt(position - 1) == '\\') {
            return false; // Escaped pipe
        }

        return true; // Unescaped pipe = cell separator
    }
}
