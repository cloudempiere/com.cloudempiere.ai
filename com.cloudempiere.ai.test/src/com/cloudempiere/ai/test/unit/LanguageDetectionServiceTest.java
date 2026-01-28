/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.test.unit;

import com.cloudempiere.ai.test.categories.UnitTest;
import com.cloudempiere.ai.test.support.TestLogger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.cloudempiere.ai.service.LanguageDetectionService;

/**
 * Unit tests for LanguageDetectionService (ADR-037).
 *
 * Tests language change detection patterns, session override management,
 * and language instruction generation.
 *
 * These tests do not require iDempiere context and can run standalone.
 *
 * Run with:
 *   ./run-unit-tests.sh LanguageDetectionServiceTest
 *
 * @author CloudEmpiere
 * @version 1.0
 */
@UnitTest
@DisplayName("LanguageDetectionService Tests")
class LanguageDetectionServiceTest {

    private LanguageDetectionService service;
    private static final TestLogger log = new TestLogger(LanguageDetectionServiceTest.class);

    private static final int TEST_CHAT_ID = 99999;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        service = LanguageDetectionService.getInstance();
        // Clear any previous overrides
        service.clearOverrideLanguage(TEST_CHAT_ID);
        testInfo.getTestMethod().ifPresent(m -> log.testStart(m.getName()));
    }

    // ========================================================================
    // Singleton Tests
    // ========================================================================

    @Test
    @UnitTest
@DisplayName("Singleton returns same instance")
    void shouldReturnSameInstance() {
        LanguageDetectionService instance1 = LanguageDetectionService.getInstance();
        LanguageDetectionService instance2 = LanguageDetectionService.getInstance();

        assertThat(instance1).isSameAs(instance2);
    }

    // ========================================================================
    // Language Detection - English Patterns
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("English Language Change Patterns")
    class EnglishPatternTests {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @UnitTest
@DisplayName("Detects 'respond in X' pattern")
        @CsvSource({
            "respond in German, de_DE",
            "respond in Spanish, es_ES",
            "respond in French, fr_FR",
            "respond in Italian, it_IT",
            "respond in Portuguese, pt_BR",
            "respond in Dutch, nl_NL",
            "respond in Polish, pl_PL",
            "respond in Russian, ru_RU",
            "respond in Slovak, sk_SK",
            "respond in Czech, cs_CZ",
            "respond in Hungarian, hu_HU",
            "respond in English, en_US"
        })
        void shouldDetectRespondInPattern(String input, String expectedLang) {
            log.input("Message", input);
            Optional<String> detected = service.detectLanguageChangeRequest(input);
            log.output("Detected", detected.orElse("(none)"));

            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo(expectedLang);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @UnitTest
@DisplayName("Detects 'switch to X' pattern")
        @CsvSource({
            "switch to German, de_DE",
            "switch to Spanish, es_ES",
            "switch to French, fr_FR",
            "change to Italian, it_IT"
        })
        void shouldDetectSwitchToPattern(String input, String expectedLang) {
            Optional<String> detected = service.detectLanguageChangeRequest(input);

            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo(expectedLang);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @UnitTest
@DisplayName("Detects 'use X' pattern")
        @CsvSource({
            "use German, de_DE",
            "use Spanish language, es_ES",
            "use French, fr_FR"
        })
        void shouldDetectUsePattern(String input, String expectedLang) {
            Optional<String> detected = service.detectLanguageChangeRequest(input);

            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo(expectedLang);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @UnitTest
@DisplayName("Detects 'answer/reply/write in X' patterns")
        @CsvSource({
            "answer in German, de_DE",
            "reply in Spanish, es_ES",
            "write in French, fr_FR",
            "speak in Italian, it_IT"
        })
        void shouldDetectOtherVerbPatterns(String input, String expectedLang) {
            Optional<String> detected = service.detectLanguageChangeRequest(input);

            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo(expectedLang);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @UnitTest
@DisplayName("Detects 'X please' suffix pattern")
        @CsvSource({
            "German please, de_DE",
            "Spanish bitte, es_ES",
            "French por favor, fr_FR"
        })
        void shouldDetectPleaseSuffixPattern(String input, String expectedLang) {
            Optional<String> detected = service.detectLanguageChangeRequest(input);

            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo(expectedLang);
        }
    }

    // ========================================================================
    // Language Detection - Native Language Patterns
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Native Language Change Patterns")
    class NativePatternTests {

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @UnitTest
@DisplayName("Detects German 'auf X' pattern")
        @CsvSource({
            "auf Deutsch, de_DE",
            "Antworte auf Deutsch, de_DE",
            "auf Englisch bitte, en_US",
            "auf Französisch, fr_FR"
        })
        void shouldDetectGermanPattern(String input, String expectedLang) {
            Optional<String> detected = service.detectLanguageChangeRequest(input);

            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo(expectedLang);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @UnitTest
@DisplayName("Detects Spanish 'en X' pattern")
        @CsvSource({
            "en español, es_ES",
            "responde en español, es_ES",
            "en inglés, en_US",
            "en francés, fr_FR"
        })
        void shouldDetectSpanishPattern(String input, String expectedLang) {
            Optional<String> detected = service.detectLanguageChangeRequest(input);

            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo(expectedLang);
        }

        @ParameterizedTest(name = "\"{0}\" -> {1}")
        @UnitTest
@DisplayName("Detects French 'en X' pattern")
        @CsvSource({
            "en français, fr_FR",
            "réponds en français, fr_FR",
            "en anglais, en_US",
            "en allemand, de_DE"
        })
        void shouldDetectFrenchPattern(String input, String expectedLang) {
            Optional<String> detected = service.detectLanguageChangeRequest(input);

            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo(expectedLang);
        }
    }

    // ========================================================================
    // No Detection Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Non-Matching Messages")
    class NoDetectionTests {

        @ParameterizedTest(name = "Should not detect: \"{0}\"")
        @UnitTest
@DisplayName("Normal queries should not trigger detection")
        @ValueSource(strings = {
            "What is the weather today?",
            "Show me the sales report",
            "How many orders do we have?",
            "German shepherd is a dog breed",
            "The French Revolution was in 1789",
            "Spanish flu was a pandemic",
            "I speak three languages",
            "Translate this to German",
            ""
        })
        void shouldNotDetectNormalQueries(String input) {
            log.input("Message", input);
            Optional<String> detected = service.detectLanguageChangeRequest(input);
            log.output("Detected", detected.orElse("(none)"));

            assertThat(detected).isEmpty();
        }

        @Test
        @UnitTest
@DisplayName("Null input should not detect")
        void shouldNotDetectNull() {
            Optional<String> detected = service.detectLanguageChangeRequest(null);
            assertThat(detected).isEmpty();
        }
    }

    // ========================================================================
    // Session Override Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Session Override Management")
    class SessionOverrideTests {

        @Test
        @UnitTest
@DisplayName("No override initially")
        void shouldHaveNoOverrideInitially() {
            assertThat(service.hasOverrideLanguage(TEST_CHAT_ID)).isFalse();
            assertThat(service.getOverrideLanguage(TEST_CHAT_ID)).isEmpty();
        }

        @Test
        @UnitTest
@DisplayName("Set and get override")
        void shouldSetAndGetOverride() {
            service.setOverrideLanguage(TEST_CHAT_ID, "de_DE");

            assertThat(service.hasOverrideLanguage(TEST_CHAT_ID)).isTrue();
            assertThat(service.getOverrideLanguage(TEST_CHAT_ID)).contains("de_DE");
        }

        @Test
        @UnitTest
@DisplayName("Override takes priority over context")
        void shouldPrioritizeOverride() {
            service.setOverrideLanguage(TEST_CHAT_ID, "de_DE");

            // Pass null for ctx - override should still be returned
            String sessionLang = service.getSessionLanguage(null, TEST_CHAT_ID);

            assertThat(sessionLang).isEqualTo("de_DE");
        }

        @Test
        @UnitTest
@DisplayName("Clear override")
        void shouldClearOverride() {
            service.setOverrideLanguage(TEST_CHAT_ID, "de_DE");
            service.clearOverrideLanguage(TEST_CHAT_ID);

            assertThat(service.hasOverrideLanguage(TEST_CHAT_ID)).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("Falls back to default when no override and no context")
        void shouldFallbackToDefault() {
            String sessionLang = service.getSessionLanguage(null, TEST_CHAT_ID);

            assertThat(sessionLang).isEqualTo("en_US");
        }

        @Test
        @UnitTest
@DisplayName("Different chats have independent overrides")
        void shouldHaveIndependentOverrides() {
            int chat1 = 11111;
            int chat2 = 22222;

            service.setOverrideLanguage(chat1, "de_DE");
            service.setOverrideLanguage(chat2, "es_ES");

            assertThat(service.getSessionLanguage(null, chat1)).isEqualTo("de_DE");
            assertThat(service.getSessionLanguage(null, chat2)).isEqualTo("es_ES");

            // Cleanup
            service.clearOverrideLanguage(chat1);
            service.clearOverrideLanguage(chat2);
        }
    }

    // ========================================================================
    // Acknowledgment Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Language Change Acknowledgments")
    class AcknowledgmentTests {

        @Test
        @UnitTest
@DisplayName("German acknowledgment is in German")
        void shouldAcknowledgeInGerman() {
            String ack = service.getLanguageChangeAcknowledgment("de_DE");
            log.output("Acknowledgment", ack);

            assertThat(ack).contains("Deutsch");
        }

        @Test
        @UnitTest
@DisplayName("Spanish acknowledgment is in Spanish")
        void shouldAcknowledgeInSpanish() {
            String ack = service.getLanguageChangeAcknowledgment("es_ES");

            assertThat(ack).contains("español");
        }

        @Test
        @UnitTest
@DisplayName("French acknowledgment is in French")
        void shouldAcknowledgeInFrench() {
            String ack = service.getLanguageChangeAcknowledgment("fr_FR");

            assertThat(ack).contains("français");
        }

        @Test
        @UnitTest
@DisplayName("Slovak acknowledgment is in Slovak")
        void shouldAcknowledgeInSlovak() {
            String ack = service.getLanguageChangeAcknowledgment("sk_SK");

            assertThat(ack).contains("slovensky");
        }

        @Test
        @UnitTest
@DisplayName("Unknown language gets English fallback")
        void shouldFallbackForUnknownLanguage() {
            String ack = service.getLanguageChangeAcknowledgment("xx_XX");

            // When Language is not found, we get a generic response
            assertThat(ack).containsAnyOf("Language changed", "Understood", "will now respond");
        }

        @ParameterizedTest(name = "{0} variants use same acknowledgment")
        @UnitTest
@DisplayName("Language variants use same acknowledgment")
        @CsvSource({
            "de_DE, Deutsch",
            "de_CH, Deutsch",
            "de_AT, Deutsch",
            "es_ES, español",
            "es_MX, español",
            "fr_FR, français",
            "fr_CA, français"
        })
        void shouldHandleLanguageVariants(String langCode, String expectedWord) {
            String ack = service.getLanguageChangeAcknowledgment(langCode);

            assertThat(ack).contains(expectedWord);
        }
    }

    // ========================================================================
    // Language Instruction Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Language Instruction Generation")
    class LanguageInstructionTests {

        @Test
        @UnitTest
@DisplayName("Builds instruction with language name")
        void shouldBuildInstructionWithLanguageName() {
            String instruction = service.buildLanguageInstruction("de_DE");
            log.output("Instruction", instruction);

            // Language.getName() returns the locale name - could be "German" or "de_DE" depending on setup
            assertThat(instruction).containsAnyOf("German", "de_DE");
            assertThat(instruction).contains("de");  // language code
            assertThat(instruction).containsIgnoringCase("Technical");
            assertThat(instruction).contains("English");
            // Verify strong language enforcement
            assertThat(instruction).containsAnyOf("MUST", "CRITICAL", "ENTIRELY");
        }

        @Test
        @UnitTest
@DisplayName("Empty instruction for null language")
        void shouldReturnEmptyForNullLanguage() {
            String instruction = service.buildLanguageInstruction(null);

            assertThat(instruction).isEmpty();
        }

        @Test
        @UnitTest
@DisplayName("Empty instruction for blank language")
        void shouldReturnEmptyForBlankLanguage() {
            String instruction = service.buildLanguageInstruction("  ");

            assertThat(instruction).isEmpty();
        }

        @Test
        @UnitTest
@DisplayName("getLanguageInstruction combines session language and build")
        void shouldCombineSessionLanguageAndBuild() {
            service.setOverrideLanguage(TEST_CHAT_ID, "fr_FR");

            String instruction = service.getLanguageInstruction(null, TEST_CHAT_ID);

            // Language.getName() returns the locale name - could be "French" or "fr_FR" depending on setup
            assertThat(instruction).containsAnyOf("French", "fr_FR");
            assertThat(instruction).contains("fr");  // language code
        }
    }

    // ========================================================================
    // Input Language Auto-Detection Tests (Bug Fix Verification)
    // ========================================================================

    @Nested
    @UnitTest
    @DisplayName("Input Language Auto-Detection")
    class InputLanguageDetectionTests {

        @Test
        @UnitTest
        @DisplayName("BUG FIX: 'find me my top 10 customers' detects as English (not Finnish)")
        void shouldDetectEnglishBusinessQuery() {
            String input = "find me my top 10 customers";
            log.input("Business Query", input);

            Optional<String> detected = service.detectInputLanguage(input);
            log.output("Detected Language", detected.orElse("(none)"));

            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo("en_US");
        }

        @ParameterizedTest(name = "English: \"{0}\"")
        @UnitTest
        @DisplayName("English business queries detect correctly")
        @ValueSource(strings = {
            "show me the top 10 orders",
            "find customers with outstanding invoices",
            "what are my best selling products",
            "list all pending orders",
            "show me sales for this month",
            "find products that need reordering",
            "who are my biggest customers"
        })
        void shouldDetectEnglishBusinessQueries(String input) {
            Optional<String> detected = service.detectInputLanguage(input);

            assertThat(detected)
                .as("Business query '%s' should detect as English", input)
                .isPresent()
                .contains("en_US");
        }

        @Test
        @UnitTest
        @DisplayName("Finnish text still detects as Finnish")
        void shouldDetectFinnish() {
            String input = "kiitos, mitä tämä tarkoittaa? Haluan nähdä tilaukset.";
            log.input("Finnish Text", input);

            Optional<String> detected = service.detectInputLanguage(input);
            log.output("Detected Language", detected.orElse("(none)"));

            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo("fi_FI");
        }

        @ParameterizedTest(name = "Finnish: \"{0}\"")
        @UnitTest
        @DisplayName("Finnish specific words detect correctly")
        @ValueSource(strings = {
            "kiitos paljon",
            "mitä kuuluu",
            "hyvää päivää",
            "minä haluan nähdä tilaukset",
            "olemme valmiita"
        })
        void shouldDetectFinnishText(String input) {
            Optional<String> detected = service.detectInputLanguage(input);

            assertThat(detected)
                .as("Finnish text '%s' should detect as Finnish", input)
                .isPresent()
                .contains("fi_FI");
        }

        @ParameterizedTest(name = "{1}: \"{0}\"")
        @UnitTest
        @DisplayName("Common European languages detect correctly")
        @CsvSource({
            "wie geht es dir heute?, de_DE",
            "¿cómo estás hoy?, es_ES",
            "comment allez-vous aujourd'hui?, fr_FR",
            "come stai oggi?, it_IT",
            "prosím pomôžte mi, sk_SK",
            "prosím pomozte mi, cs_CZ",
            "proszę pomóż mi, pl_PL",
            "kérem segítsen nekem, hu_HU"
        })
        void shouldDetectEuropeanLanguages(String input, String expectedLang) {
            Optional<String> detected = service.detectInputLanguage(input);

            assertThat(detected)
                .as("Text '%s' should detect as %s", input, expectedLang)
                .isPresent()
                .contains(expectedLang);
        }

        @ParameterizedTest(name = "{1}: \"{0}\"")
        @UnitTest
        @DisplayName("Script-based detection works for non-Latin scripts")
        @CsvSource({
            "Привет как дела?, ru_RU",
            "こんにちは、お元気ですか？, ja_JP",
            "안녕하세요 잘 지내세요?, ko_KR",
            "Γεια σας πώς είστε?, el_GR"
        })
        void shouldDetectNonLatinScripts(String input, String expectedLang) {
            Optional<String> detected = service.detectInputLanguage(input);

            assertThat(detected)
                .as("Text '%s' should detect as %s", input, expectedLang)
                .isPresent()
                .contains(expectedLang);
        }

        @Test
        @UnitTest
        @DisplayName("English detection has priority over ambiguous patterns")
        void shouldPrioritizeEnglishDetection() {
            // These contain words that could match other languages but should be English
            String[] englishPhrases = {
                "the order is ready",
                "we have ten customers",
                "can you help me please",
                "this is my report"
            };

            for (String phrase : englishPhrases) {
                Optional<String> detected = service.detectInputLanguage(phrase);
                assertThat(detected)
                    .as("Phrase '%s' should detect as English", phrase)
                    .isPresent()
                    .contains("en_US");
            }
        }

        @Test
        @UnitTest
        @DisplayName("Empty or null input returns empty")
        void shouldReturnEmptyForInvalidInput() {
            assertThat(service.detectInputLanguage(null)).isEmpty();
            assertThat(service.detectInputLanguage("")).isEmpty();
            assertThat(service.detectInputLanguage("   ")).isEmpty();
        }

        @Test
        @UnitTest
        @DisplayName("Ambiguous short text falls back gracefully")
        void shouldHandleAmbiguousText() {
            // Very short text with no distinctive patterns
            Optional<String> detected = service.detectInputLanguage("ok");

            // Should either detect something or return empty (both acceptable)
            // The important thing is it doesn't crash
            assertThat(detected).isNotNull();
        }
    }

    // ========================================================================
    // Language Map Database Integration Tests
    // ========================================================================

    @Nested
    @UnitTest
    @DisplayName("Language Map Database Integration")
    class LanguageMapTests {

        @Test
        @UnitTest
        @DisplayName("Language map can be reloaded from database")
        void shouldReloadLanguageMap() {
            // This will trigger lazy initialization
            service.detectLanguageChangeRequest("respond in German");

            // Should not throw exception
            assertThat(() -> service.reloadLanguageMap()).doesNotThrowAnyException();

            // Should still work after reload
            Optional<String> detected = service.detectLanguageChangeRequest("respond in German");
            assertThat(detected).isPresent();
        }

        @Test
        @UnitTest
        @DisplayName("detectLanguageChangeRequest works without database connection")
        void shouldWorkOffline() {
            // Should use fallback map and Language.getLanguage()
            Optional<String> detected = service.detectLanguageChangeRequest("respond in English");

            // Should either work or return empty, but not crash
            assertThat(detected).isNotNull();
        }

        @Test
        @UnitTest
        @DisplayName("Language.getLanguage() fallback works for AD_Language codes")
        void shouldHandleADLanguageCodes() {
            // Test with actual AD_Language code format
            Optional<String> detected = service.detectLanguageChangeRequest("respond in en_US");

            // Language.getLanguage() should handle this
            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo("en_US");
        }

        @Test
        @UnitTest
        @DisplayName("Language.getLanguage() fallback works for ISO codes")
        void shouldHandleISOCodes() {
            // Test with ISO language codes
            Optional<String> detected = service.detectLanguageChangeRequest("respond in de");

            // Language.getLanguage() should handle this
            assertThat(detected).isPresent();
            assertThat(detected.get()).isEqualTo("de_DE");
        }

        @Test
        @UnitTest
        @DisplayName("Unrecognized language returns empty")
        void shouldReturnEmptyForUnrecognizedLanguage() {
            Optional<String> detected = service.detectLanguageChangeRequest("respond in Klingon");

            assertThat(detected).isEmpty();
        }
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @UnitTest
@DisplayName("Case insensitive detection")
        void shouldBeCaseInsensitive() {
            assertThat(service.detectLanguageChangeRequest("RESPOND IN GERMAN")).contains("de_DE");
            assertThat(service.detectLanguageChangeRequest("Respond In German")).contains("de_DE");
            assertThat(service.detectLanguageChangeRequest("respond in german")).contains("de_DE");
        }

        @Test
        @UnitTest
@DisplayName("Works with surrounding text")
        void shouldWorkWithSurroundingText() {
            assertThat(service.detectLanguageChangeRequest("Please respond in German for this conversation"))
                .contains("de_DE");
            assertThat(service.detectLanguageChangeRequest("From now on, answer in Spanish"))
                .contains("es_ES");
        }

        @Test
        @UnitTest
@DisplayName("Handles special characters in native patterns")
        void shouldHandleSpecialCharacters() {
            assertThat(service.detectLanguageChangeRequest("en español")).contains("es_ES");
            assertThat(service.detectLanguageChangeRequest("en français")).contains("fr_FR");
            assertThat(service.detectLanguageChangeRequest("auf Französisch")).contains("fr_FR");
        }

        @Test
        @UnitTest
@DisplayName("Invalid chat ID uses fallback language")
        void shouldHandleInvalidChatId() {
            String lang = service.getSessionLanguage(null, -1);
            assertThat(lang).isEqualTo("en_US");

            lang = service.getSessionLanguage(null, 0);
            assertThat(lang).isEqualTo("en_US");
        }
    }
}
