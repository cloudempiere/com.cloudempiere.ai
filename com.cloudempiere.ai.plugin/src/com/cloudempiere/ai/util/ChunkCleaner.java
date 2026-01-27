package com.cloudempiere.ai.util;

/**
 * Cleans streamed text chunks by removing problematic characters
 * while preserving markdown formatting requirements.
 *
 * <p>Removes:
 * <ul>
 *   <li>Zero-width characters (U+200B-U+200F, U+FEFF)</li>
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

        // 1. Remove zero-width characters
        String cleaned = chunk
            .replaceAll("[\u200B\u200C\u200D\u200E\u200F\uFEFF]", "");

        // 2. Remove control characters (except \t, \n)
        cleaned = cleaned.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // 3. Normalize line breaks
        cleaned = cleaned
            .replace("\r\n", "\n")
            .replace("\r", "\n");

        // 4. Limit consecutive blank lines to 2
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
}
