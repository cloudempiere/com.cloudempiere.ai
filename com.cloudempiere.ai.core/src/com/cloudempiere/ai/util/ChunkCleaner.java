package com.cloudempiere.ai.util;

/**
 * Cleans streamed text chunks by removing problematic characters
 * while preserving markdown formatting requirements.
 *
 * <p>Removes:
 * <ul>
 *   <li>Zero-width characters (U+200B-U+200F, U+FEFF)</li>
 *   <li>Unicode line/paragraph separators (U+2028, U+2029)</li>
 *   <li>Replacement character (U+FFFD)</li>
 *   <li>Unpaired surrogate pairs (U+D800-U+DFFF)</li>
 *   <li>Control characters (except tab, newline)</li>
 *   <li>Excessive blank lines (>2)</li>
 * </ul>
 *
 * <p>Preserves:
 * <ul>
 *   <li>Markdown line breaks (2 trailing spaces)</li>
 *   <li>Code block indentation (tabs/spaces)</li>
 *   <li>Table formatting</li>
 * </ul>
 *
 * <p><b>International Support:</b> Handles Unicode edge cases that can cause
 * database corruption (Oracle/PostgreSQL) or formatting issues in international
 * deployments.
 *
 * @see <a href="../../docs/adr/047-streaming-chat-rendering-best-practices.md">ADR-047</a>
 * @since v0.31.0
 */
public class ChunkCleaner {

    /**
     * Clean a streaming chunk.
     *
     * @param chunk raw chunk from LLM
     * @return cleaned chunk safe for display
     */
    public static String clean(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return chunk;
        }

        // 1. Remove zero-width characters + Unicode line/paragraph separators + replacement char
        String cleaned = chunk
            .replaceAll("[\u200B\u200C\u200D\u200E\u200F\uFEFF\u2028\u2029\uFFFD]", "");

        // 2. Remove unpaired surrogate pairs (prevents database corruption)
        cleaned = removeSurrogatePairs(cleaned);

        // 3. Remove control characters (except \t, \n)
        cleaned = cleaned.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // 4. Normalize line breaks
        cleaned = cleaned
            .replace("\r\n", "\n")
            .replace("\r", "\n");

        // 5. Limit consecutive blank lines to 2
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

        return cleaned;
    }

    /**
     * Clean and preserve markdown line breaks (2 trailing spaces).
     *
     * @param chunk raw chunk
     * @return cleaned chunk with markdown line breaks preserved
     */
    public static String cleanPreservingMarkdown(String chunk) {
        String cleaned = clean(chunk);

        // Handle null or empty after cleaning
        if (cleaned == null || cleaned.isEmpty()) {
            return cleaned;
        }

        // Process line by line to preserve 2-space line breaks
        String[] lines = cleaned.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Preserve markdown line breaks (2 trailing spaces)
            if (line.endsWith("  ")) {
                result.append(line.stripLeading());
            } else {
                result.append(line.trim());
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Remove unpaired surrogate pairs from text.
     *
     * <p><b>Purpose:</b> Prevent database corruption from invalid UTF-16 sequences.
     * Some LLMs may generate broken Unicode that contains unpaired surrogates:
     * <ul>
     *   <li>High surrogates: U+D800 to U+DBFF (must be followed by low surrogate)</li>
     *   <li>Low surrogates: U+DC00 to U+DFFF (must be preceded by high surrogate)</li>
     * </ul>
     *
     * <p><b>Database Impact:</b> Oracle and PostgreSQL reject unpaired surrogates,
     * causing SQL exceptions when storing AI responses. This method removes them
     * while preserving valid surrogate pairs (emoji, extended Unicode).
     *
     * <p><b>Example:</b>
     * <pre>
     * // Valid surrogate pair (emoji U+1F600, encoded as surrogate pair U+D83D U+DE00)
     * "Hello \uD83D\uDE00" -> "Hello \uD83D\uDE00" (preserved)
     *
     * // Unpaired high surrogate
     * "Hello \uD83D world" → "Hello  world" (removed)
     *
     * // Unpaired low surrogate
     * "Hello \uDE00 world" → "Hello  world" (removed)
     * </pre>
     *
     * @param text text that may contain unpaired surrogates
     * @return text with unpaired surrogates removed
     */
    private static String removeSurrogatePairs(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            // Check if this is a high surrogate (U+D800 to U+DBFF)
            if (Character.isHighSurrogate(ch)) {
                // Check if followed by low surrogate (valid pair)
                if (i + 1 < text.length() && Character.isLowSurrogate(text.charAt(i + 1))) {
                    // Valid surrogate pair - keep both chars
                    result.append(ch);
                    result.append(text.charAt(i + 1));
                    i++; // Skip low surrogate (already processed)
                } else {
                    // Unpaired high surrogate - remove it
                    // (silently drop - no placeholder needed)
                }
            }
            // Check if this is a low surrogate (U+DC00 to U+DFFF)
            else if (Character.isLowSurrogate(ch)) {
                // Low surrogate without preceding high surrogate - remove it
                // (should never happen if we process sequentially, but defensive check)
            }
            // Regular character (not a surrogate)
            else {
                result.append(ch);
            }
        }

        return result.toString();
    }
}
