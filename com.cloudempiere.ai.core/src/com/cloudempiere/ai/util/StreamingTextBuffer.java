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

/**
 * Buffer for handling streaming text with proper UTF-8/UTF-16 surrogate pair handling.
 *
 * <p>When AI providers stream responses, text arrives in chunks that may split
 * multi-byte characters (emojis, CJK characters, etc.) across chunk boundaries.
 * This buffer accumulates text and only releases complete characters for display.
 *
 * <p><b>Problem Solved:</b>
 * <ul>
 *   <li>Emojis like U+1F4D6 (Open Book) are represented as surrogate pairs in UTF-16</li>
 *   <li>Streaming may deliver the high surrogate (\uD83D) in one chunk and
 *       low surrogate (\uDCE5) in the next</li>
 *   <li>Displaying incomplete surrogates shows replacement characters (�)</li>
 * </ul>
 *
 * <p><b>Solution:</b>
 * Hold back trailing incomplete surrogate pairs until the next chunk completes them.
 *
 * <p><b>Usage:</b>
 * <pre>
 * StreamingTextBuffer buffer = new StreamingTextBuffer();
 *
 * // In streaming callback:
 * buffer.append(chunk);
 * String displayText = buffer.getDisplayableText();
 * updateUI(displayText);
 *
 * // When complete:
 * String finalText = buffer.flush();
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 * @since CLD-1601
 */
public class StreamingTextBuffer {

    /** The complete accumulated text */
    private final StringBuilder buffer = new StringBuilder();

    /** Pending incomplete surrogate (high surrogate waiting for low) */
    private char pendingHighSurrogate = 0;

    /** Track if we have a pending high surrogate */
    private boolean hasPendingSurrogate = false;

    /**
     * Create a new streaming text buffer.
     */
    public StreamingTextBuffer() {
        // Default constructor
    }

    /**
     * Append a chunk of text to the buffer.
     *
     * <p>The chunk may contain partial surrogate pairs. This method handles
     * incomplete surrogates by holding them until the next chunk arrives.
     *
     * @param chunk the text chunk to append (may be null or empty)
     */
    public void append(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }

        // If we have a pending high surrogate from the previous chunk,
        // check if this chunk starts with a low surrogate
        if (hasPendingSurrogate && chunk.length() > 0) {
            char firstChar = chunk.charAt(0);
            if (Character.isLowSurrogate(firstChar)) {
                // Complete the surrogate pair
                buffer.append(pendingHighSurrogate);
                buffer.append(firstChar);
                hasPendingSurrogate = false;
                pendingHighSurrogate = 0;
                // Process the rest of the chunk
                chunk = chunk.substring(1);
            } else {
                // The previous high surrogate was orphaned - append replacement char
                buffer.append('\uFFFD'); // Unicode replacement character
                hasPendingSurrogate = false;
                pendingHighSurrogate = 0;
            }
        }

        if (chunk.isEmpty()) {
            return;
        }

        // Check if the chunk ends with an incomplete surrogate pair
        char lastChar = chunk.charAt(chunk.length() - 1);
        if (Character.isHighSurrogate(lastChar)) {
            // Hold the high surrogate for the next chunk
            pendingHighSurrogate = lastChar;
            hasPendingSurrogate = true;
            // Append everything except the last character
            buffer.append(chunk, 0, chunk.length() - 1);
        } else {
            // Complete chunk - append all
            buffer.append(chunk);
        }
    }

    /**
     * Get the text that is safe to display.
     *
     * <p>Returns all accumulated text. Any pending incomplete surrogates
     * are held back and not included in the display text.
     *
     * @return displayable text (never null)
     */
    public String getDisplayableText() {
        return buffer.toString();
    }

    /**
     * Flush and return all accumulated text, including any pending surrogates.
     *
     * <p>Use this when streaming is complete. Any orphaned high surrogate
     * will be converted to a replacement character.
     *
     * @return complete text (never null)
     */
    public String flush() {
        if (hasPendingSurrogate) {
            // Orphaned high surrogate - convert to replacement character
            buffer.append('\uFFFD');
            hasPendingSurrogate = false;
            pendingHighSurrogate = 0;
        }
        return buffer.toString();
    }

    /**
     * Get the current length of displayable text.
     *
     * @return length in characters
     */
    public int length() {
        return buffer.length();
    }

    /**
     * Check if the buffer is empty.
     *
     * @return true if no displayable text has been accumulated
     */
    public boolean isEmpty() {
        return buffer.length() == 0 && !hasPendingSurrogate;
    }

    /**
     * Check if there's a pending incomplete surrogate.
     *
     * @return true if waiting for a low surrogate
     */
    public boolean hasPendingSurrogate() {
        return hasPendingSurrogate;
    }

    /**
     * Clear the buffer and reset state.
     */
    public void clear() {
        buffer.setLength(0);
        pendingHighSurrogate = 0;
        hasPendingSurrogate = false;
    }

    @Override
    public String toString() {
        return getDisplayableText();
    }
}
