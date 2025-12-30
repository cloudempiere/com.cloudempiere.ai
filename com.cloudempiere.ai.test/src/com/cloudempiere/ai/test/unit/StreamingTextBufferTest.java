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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.util.StreamingTextBuffer;

/**
 * Unit tests for StreamingTextBuffer - handling UTF-16 surrogate pairs during streaming.
 *
 * <p>Tests verify that emojis and other multi-byte characters display correctly
 * when split across streaming chunks.
 *
 * @author Cloudempiere
 * @since CLD-1601
 */
@UnitTest
@DisplayName("StreamingTextBuffer Tests")
class StreamingTextBufferTest {

    private StreamingTextBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new StreamingTextBuffer();
    }

    @Nested
    @UnitTest
@DisplayName("Basic Operations")
    class BasicOperations {

        @Test
        @UnitTest
@DisplayName("Should start empty")
        void shouldStartEmpty() {
            assertThat(buffer.isEmpty()).isTrue();
            assertThat(buffer.length()).isZero();
            assertThat(buffer.getDisplayableText()).isEmpty();
        }

        @Test
        @UnitTest
@DisplayName("Should append simple ASCII text")
        void shouldAppendSimpleText() {
            buffer.append("Hello");
            buffer.append(" ");
            buffer.append("World");

            assertThat(buffer.getDisplayableText()).isEqualTo("Hello World");
            assertThat(buffer.length()).isEqualTo(11);
        }

        @Test
        @UnitTest
@DisplayName("Should handle null and empty appends")
        void shouldHandleNullAndEmpty() {
            buffer.append("Test");
            buffer.append(null);
            buffer.append("");
            buffer.append("End");

            assertThat(buffer.getDisplayableText()).isEqualTo("TestEnd");
        }

        @Test
        @UnitTest
@DisplayName("Should clear buffer")
        void shouldClearBuffer() {
            buffer.append("Some content");
            buffer.clear();

            assertThat(buffer.isEmpty()).isTrue();
            assertThat(buffer.getDisplayableText()).isEmpty();
        }
    }

    @Nested
    @UnitTest
@DisplayName("Emoji Handling - Complete Characters")
    class EmojiHandlingComplete {

        @Test
        @UnitTest
@DisplayName("Should handle emoji in single chunk")
        void shouldHandleEmojiInSingleChunk() {
            // Book emoji: U+1F4D6
            buffer.append("Hello \uD83D\uDCD6 World");

            assertThat(buffer.getDisplayableText()).isEqualTo("Hello \uD83D\uDCD6 World");
        }

        @Test
        @UnitTest
@DisplayName("Should handle multiple emojis")
        void shouldHandleMultipleEmojis() {
            // Various emojis
            buffer.append("\uD83D\uDE00"); // Grinning face
            buffer.append("\uD83D\uDC4B"); // Waving hand
            buffer.append("\uD83C\uDF89"); // Party popper

            assertThat(buffer.getDisplayableText()).isEqualTo("\uD83D\uDE00\uD83D\uDC4B\uD83C\uDF89");
        }

        @Test
        @UnitTest
@DisplayName("Should handle warning emoji (used in error messages)")
        void shouldHandleWarningEmoji() {
            // Warning sign + variation selector (as used in AIErrorHandler)
            buffer.append("\u26A0\uFE0F Something went wrong");

            assertThat(buffer.getDisplayableText()).isEqualTo("\u26A0\uFE0F Something went wrong");
        }
    }

    @Nested
    @UnitTest
@DisplayName("Emoji Handling - Split Surrogates")
    class EmojiHandlingSplit {

        @Test
        @UnitTest
@DisplayName("Should handle emoji split across chunks - high then low surrogate")
        void shouldHandleSplitEmoji() {
            // Book emoji: U+1F4D6 = \uD83D\uDCD6
            buffer.append("Hello ");
            buffer.append("\uD83D"); // High surrogate only

            // High surrogate should be held back
            assertThat(buffer.getDisplayableText()).isEqualTo("Hello ");
            assertThat(buffer.hasPendingSurrogate()).isTrue();

            buffer.append("\uDCD6"); // Low surrogate completes the pair

            assertThat(buffer.getDisplayableText()).isEqualTo("Hello \uD83D\uDCD6");
            assertThat(buffer.hasPendingSurrogate()).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("Should handle emoji at chunk boundary with following text")
        void shouldHandleSplitEmojiWithFollowingText() {
            buffer.append("Test\uD83D"); // Text + high surrogate

            assertThat(buffer.getDisplayableText()).isEqualTo("Test");
            assertThat(buffer.hasPendingSurrogate()).isTrue();

            buffer.append("\uDCD6 more text"); // Low surrogate + more text

            assertThat(buffer.getDisplayableText()).isEqualTo("Test\uD83D\uDCD6 more text");
        }

        @Test
        @UnitTest
@DisplayName("Should handle multiple split emojis in sequence")
        void shouldHandleMultipleSplitEmojis() {
            // First emoji split
            buffer.append("\uD83D");
            assertThat(buffer.hasPendingSurrogate()).isTrue();

            buffer.append("\uDE00"); // Completes grinning face
            assertThat(buffer.hasPendingSurrogate()).isFalse();

            // Second emoji split
            buffer.append("\uD83D");
            assertThat(buffer.hasPendingSurrogate()).isTrue();

            buffer.append("\uDC4B"); // Completes waving hand
            assertThat(buffer.hasPendingSurrogate()).isFalse();

            assertThat(buffer.getDisplayableText()).isEqualTo("\uD83D\uDE00\uD83D\uDC4B");
        }

        @Test
        @UnitTest
@DisplayName("Should handle orphaned high surrogate on flush")
        void shouldHandleOrphanedSurrogateOnFlush() {
            buffer.append("Text\uD83D"); // Text + orphaned high surrogate

            assertThat(buffer.hasPendingSurrogate()).isTrue();

            // Flush should convert orphaned surrogate to replacement char
            String result = buffer.flush();
            assertThat(result).isEqualTo("Text\uFFFD");
            assertThat(buffer.hasPendingSurrogate()).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("Should handle high surrogate followed by non-low-surrogate")
        void shouldHandleInvalidSurrogatePair() {
            buffer.append("\uD83D"); // High surrogate
            assertThat(buffer.hasPendingSurrogate()).isTrue();

            buffer.append("A"); // Not a low surrogate

            // Should have replacement character + 'A'
            assertThat(buffer.getDisplayableText()).isEqualTo("\uFFFDA");
            assertThat(buffer.hasPendingSurrogate()).isFalse();
        }
    }

    @Nested
    @UnitTest
@DisplayName("CJK Character Handling")
    class CJKHandling {

        @Test
        @UnitTest
@DisplayName("Should handle Chinese characters (BMP)")
        void shouldHandleChineseCharacters() {
            // Basic Chinese characters are in BMP (no surrogates needed)
            buffer.append("\u4F60\u597D"); // 你好

            assertThat(buffer.getDisplayableText()).isEqualTo("\u4F60\u597D");
        }

        @Test
        @UnitTest
@DisplayName("Should handle Japanese hiragana and katakana")
        void shouldHandleJapanese() {
            buffer.append("\u3053\u3093\u306B\u3061\u306F"); // こんにちは

            assertThat(buffer.getDisplayableText()).isEqualTo("\u3053\u3093\u306B\u3061\u306F");
        }

        @Test
        @UnitTest
@DisplayName("Should handle Korean characters")
        void shouldHandleKorean() {
            buffer.append("\uC548\uB155\uD558\uC138\uC694"); // 안녕하세요

            assertThat(buffer.getDisplayableText()).isEqualTo("\uC548\uB155\uD558\uC138\uC694");
        }
    }

    @Nested
    @UnitTest
@DisplayName("Mixed Content")
    class MixedContent {

        @Test
        @UnitTest
@DisplayName("Should handle realistic streaming scenario")
        void shouldHandleRealisticStreaming() {
            // Simulate realistic streaming chunks
            buffer.append("Here's a ");
            buffer.append("table:\n");
            buffer.append("| Name | Status |\n");
            buffer.append("|-----|--------|\n");
            buffer.append("| Test | \uD83D"); // Split emoji
            buffer.append("\uDC4D |\n"); // Complete emoji

            String result = buffer.getDisplayableText();
            assertThat(result).contains("| Name | Status |");
            assertThat(result).contains("\uD83D\uDC4D"); // Thumbs up
        }

        @Test
        @UnitTest
@DisplayName("Should handle markdown with emojis")
        void shouldHandleMarkdownWithEmojis() {
            buffer.append("# Title \uD83D\uDCDA\n"); // Books emoji
            buffer.append("**Bold** text with ");
            buffer.append("\uD83C\uDF1F"); // Star emoji

            String result = buffer.getDisplayableText();
            assertThat(result).contains("# Title");
            assertThat(result).contains("\uD83D\uDCDA");
            assertThat(result).contains("\uD83C\uDF1F");
        }
    }

    @Nested
    @UnitTest
@DisplayName("toString() Method")
    class ToStringMethod {

        @Test
        @UnitTest
@DisplayName("toString should return displayable text")
        void toStringShouldReturnDisplayableText() {
            buffer.append("Hello World");

            assertThat(buffer.toString()).isEqualTo("Hello World");
            assertThat(buffer.toString()).isEqualTo(buffer.getDisplayableText());
        }
    }
}
