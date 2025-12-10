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
package com.cloudempiere.ai.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Language;

/**
 * Service for managing session language detection and overrides (ADR-037).
 *
 * <p>This service provides language detection from iDempiere context and
 * allows users to override the language for their conversation session
 * via natural language requests like "respond in German".</p>
 *
 * <p>Language priority order:</p>
 * <ol>
 *   <li>Session override (user explicitly requested via "respond in X")</li>
 *   <li>iDempiere user language (AD_Language from login context)</li>
 *   <li>Fallback: en_US</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>
 * LanguageDetectionService langService = LanguageDetectionService.getInstance();
 *
 * // Check for language change request in user message
 * Optional&lt;String&gt; requestedLang = langService.detectLanguageChangeRequest(userMessage);
 * if (requestedLang.isPresent()) {
 *     langService.setOverrideLanguage(chatId, requestedLang.get());
 * }
 *
 * // Get effective language for system prompt
 * String langInstruction = langService.getLanguageInstruction(ctx, chatId);
 * </pre>
 *
 * @author Cloudempiere AI Team
 * @version ADR-037
 * @since v0.11.0
 * @see <a href="docs/adr/037-language-detection-session-management.md">ADR-037</a>
 */
public class LanguageDetectionService {

    private static final CLogger log = CLogger.getCLogger(LanguageDetectionService.class);

    /** Singleton instance */
    private static volatile LanguageDetectionService instance;

    /** Default fallback language */
    private static final String DEFAULT_LANGUAGE = "en_US";

    /** Session-scoped language overrides: chatId -> languageCode */
    private final Map<Integer, String> sessionOverrides = new ConcurrentHashMap<>();

    /** Patterns for detecting language change requests */
    private static final List<Pattern> LANGUAGE_CHANGE_PATTERNS = Arrays.asList(
        // English patterns
        Pattern.compile("(?i)\\b(?:respond|answer|reply|speak|write)\\s+(?:to me\\s+)?in\\s+(\\w+)"),
        Pattern.compile("(?i)\\b(?:switch|change)\\s+(?:to|the\\s+language\\s+to)\\s+(\\w+)"),
        Pattern.compile("(?i)\\buse\\s+(\\w+)\\s*(?:language)?\\b"),
        Pattern.compile("(?i)\\bin\\s+(\\w+)\\s+(?:please|bitte|por favor|s'il vous pla[iî]t)$"),
        Pattern.compile("(?i)\\b(\\w+)\\s+(?:please|bitte|por favor|s'il vous pla[iî]t)$"),
        // German patterns
        Pattern.compile("(?i)\\bauf\\s+(deutsch|englisch|spanisch|franz[oö]sisch|italienisch)\\b"),
        Pattern.compile("(?i)\\bantworte?\\s+(?:mir\\s+)?(?:auf|in)\\s+(\\w+)"),
        // Spanish patterns
        Pattern.compile("(?i)\\ben\\s+(espa[nñ]ol|ingl[eé]s|franc[eé]s|alem[aá]n|italiano)\\b"),
        Pattern.compile("(?i)\\bresponde?\\s+en\\s+(\\w+)"),
        // French patterns
        Pattern.compile("(?i)\\ben\\s+(fran[cç]ais|anglais|espagnol|allemand|italien)\\b"),
        Pattern.compile("(?i)\\br[eé]ponds?\\s+en\\s+(\\w+)")
    );

    /** Language name to code mapping (lowercase name -> AD_Language code) */
    private static final Map<String, String> LANGUAGE_MAP = new HashMap<>();

    static {
        // English names
        LANGUAGE_MAP.put("english", "en_US");
        LANGUAGE_MAP.put("german", "de_DE");
        LANGUAGE_MAP.put("spanish", "es_ES");
        LANGUAGE_MAP.put("french", "fr_FR");
        LANGUAGE_MAP.put("italian", "it_IT");
        LANGUAGE_MAP.put("portuguese", "pt_BR");
        LANGUAGE_MAP.put("dutch", "nl_NL");
        LANGUAGE_MAP.put("polish", "pl_PL");
        LANGUAGE_MAP.put("russian", "ru_RU");
        LANGUAGE_MAP.put("chinese", "zh_CN");
        LANGUAGE_MAP.put("japanese", "ja_JP");
        LANGUAGE_MAP.put("korean", "ko_KR");
        LANGUAGE_MAP.put("arabic", "ar_SA");
        LANGUAGE_MAP.put("hebrew", "he_IL");
        LANGUAGE_MAP.put("turkish", "tr_TR");
        LANGUAGE_MAP.put("czech", "cs_CZ");
        LANGUAGE_MAP.put("slovak", "sk_SK");
        LANGUAGE_MAP.put("hungarian", "hu_HU");
        LANGUAGE_MAP.put("romanian", "ro_RO");
        LANGUAGE_MAP.put("bulgarian", "bg_BG");
        LANGUAGE_MAP.put("croatian", "hr_HR");
        LANGUAGE_MAP.put("slovenian", "sl_SI");
        LANGUAGE_MAP.put("serbian", "sr_RS");
        LANGUAGE_MAP.put("ukrainian", "uk_UA");
        LANGUAGE_MAP.put("thai", "th_TH");
        LANGUAGE_MAP.put("vietnamese", "vi_VN");
        LANGUAGE_MAP.put("indonesian", "id_ID");
        LANGUAGE_MAP.put("malay", "ms_MY");
        LANGUAGE_MAP.put("greek", "el_GR");
        LANGUAGE_MAP.put("finnish", "fi_FI");
        LANGUAGE_MAP.put("swedish", "sv_SE");
        LANGUAGE_MAP.put("norwegian", "no_NO");
        LANGUAGE_MAP.put("danish", "da_DK");

        // Native language names
        LANGUAGE_MAP.put("deutsch", "de_DE");
        LANGUAGE_MAP.put("español", "es_ES");
        LANGUAGE_MAP.put("espanol", "es_ES");
        LANGUAGE_MAP.put("français", "fr_FR");
        LANGUAGE_MAP.put("francais", "fr_FR");
        LANGUAGE_MAP.put("italiano", "it_IT");
        LANGUAGE_MAP.put("português", "pt_BR");
        LANGUAGE_MAP.put("portugues", "pt_BR");
        LANGUAGE_MAP.put("polski", "pl_PL");
        LANGUAGE_MAP.put("русский", "ru_RU");
        LANGUAGE_MAP.put("中文", "zh_CN");
        LANGUAGE_MAP.put("日本語", "ja_JP");
        LANGUAGE_MAP.put("한국어", "ko_KR");
        LANGUAGE_MAP.put("العربية", "ar_SA");
        LANGUAGE_MAP.put("עברית", "he_IL");
        LANGUAGE_MAP.put("türkçe", "tr_TR");
        LANGUAGE_MAP.put("čeština", "cs_CZ");
        LANGUAGE_MAP.put("slovenčina", "sk_SK");
        LANGUAGE_MAP.put("slovenský", "sk_SK");
        LANGUAGE_MAP.put("magyar", "hu_HU");
        LANGUAGE_MAP.put("română", "ro_RO");
        LANGUAGE_MAP.put("български", "bg_BG");
        LANGUAGE_MAP.put("hrvatski", "hr_HR");
        LANGUAGE_MAP.put("slovenščina", "sl_SI");
        LANGUAGE_MAP.put("srpski", "sr_RS");
        LANGUAGE_MAP.put("українська", "uk_UA");
        LANGUAGE_MAP.put("ภาษาไทย", "th_TH");
        LANGUAGE_MAP.put("tiếng việt", "vi_VN");

        // German language names
        LANGUAGE_MAP.put("englisch", "en_US");
        LANGUAGE_MAP.put("spanisch", "es_ES");
        LANGUAGE_MAP.put("französisch", "fr_FR");
        LANGUAGE_MAP.put("franzosisch", "fr_FR");
        LANGUAGE_MAP.put("italienisch", "it_IT");

        // Spanish language names
        LANGUAGE_MAP.put("inglés", "en_US");
        LANGUAGE_MAP.put("ingles", "en_US");
        LANGUAGE_MAP.put("alemán", "de_DE");
        LANGUAGE_MAP.put("aleman", "de_DE");
        LANGUAGE_MAP.put("francés", "fr_FR");
        LANGUAGE_MAP.put("frances", "fr_FR");

        // French language names
        LANGUAGE_MAP.put("anglais", "en_US");
        LANGUAGE_MAP.put("allemand", "de_DE");
        LANGUAGE_MAP.put("espagnol", "es_ES");
        LANGUAGE_MAP.put("italien", "it_IT");
    }

    /**
     * Private constructor for singleton.
     */
    private LanguageDetectionService() {
        log.info("LanguageDetectionService initialized");
    }

    /**
     * Get singleton instance.
     *
     * @return LanguageDetectionService instance
     */
    public static LanguageDetectionService getInstance() {
        if (instance == null) {
            synchronized (LanguageDetectionService.class) {
                if (instance == null) {
                    instance = new LanguageDetectionService();
                }
            }
        }
        return instance;
    }

    /**
     * Get the effective language for the current session.
     *
     * <p>Priority order:</p>
     * <ol>
     *   <li>Session override (user explicitly requested)</li>
     *   <li>iDempiere user language (AD_Language from context)</li>
     *   <li>Fallback: en_US</li>
     * </ol>
     *
     * @param ctx iDempiere context
     * @param chatId Chat ID (for session override lookup)
     * @return Effective language code (e.g., "de_DE")
     */
    public String getSessionLanguage(Properties ctx, int chatId) {
        // 1. Check session override first
        String override = sessionOverrides.get(chatId);
        if (override != null && !override.isBlank()) {
            log.fine("Using session override language for chat " + chatId + ": " + override);
            return override;
        }

        // 2. Use iDempiere context language
        if (ctx != null) {
            String adLanguage = Env.getAD_Language(ctx);
            if (adLanguage != null && !adLanguage.isBlank()) {
                log.fine("Using iDempiere context language: " + adLanguage);
                return adLanguage;
            }
        }

        // 3. Fallback to English
        log.fine("Using default language: " + DEFAULT_LANGUAGE);
        return DEFAULT_LANGUAGE;
    }

    /**
     * Detect if user message contains a language change request.
     *
     * <p>Supported patterns include:</p>
     * <ul>
     *   <li>"respond in German"</li>
     *   <li>"switch to Spanish"</li>
     *   <li>"auf Deutsch"</li>
     *   <li>"en español"</li>
     *   <li>"en français"</li>
     * </ul>
     *
     * @param userMessage The user's message
     * @return Optional containing the detected language code, or empty if no change requested
     */
    public Optional<String> detectLanguageChangeRequest(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }

        for (Pattern pattern : LANGUAGE_CHANGE_PATTERNS) {
            Matcher matcher = pattern.matcher(userMessage);
            if (matcher.find()) {
                String languageName = matcher.group(1).toLowerCase();
                String langCode = LANGUAGE_MAP.get(languageName);
                if (langCode != null) {
                    log.info("Detected language change request: '" + languageName + "' -> " + langCode);
                    return Optional.of(langCode);
                } else {
                    log.fine("Unrecognized language name in request: " + languageName);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Set override language for a chat session.
     *
     * @param chatId Chat ID
     * @param languageCode Language code (e.g., "de_DE")
     */
    public void setOverrideLanguage(int chatId, String languageCode) {
        if (languageCode != null && !languageCode.isBlank()) {
            sessionOverrides.put(chatId, languageCode);
            log.info("Language override set for chat " + chatId + ": " + languageCode);
        }
    }

    /**
     * Clear override language for a chat session.
     *
     * @param chatId Chat ID
     */
    public void clearOverrideLanguage(int chatId) {
        String removed = sessionOverrides.remove(chatId);
        if (removed != null) {
            log.fine("Language override cleared for chat " + chatId);
        }
    }

    /**
     * Check if a chat has an override language set.
     *
     * @param chatId Chat ID
     * @return true if override is set
     */
    public boolean hasOverrideLanguage(int chatId) {
        return sessionOverrides.containsKey(chatId);
    }

    /**
     * Get the override language for a chat, if set.
     *
     * @param chatId Chat ID
     * @return Optional containing the override language code, or empty
     */
    public Optional<String> getOverrideLanguage(int chatId) {
        return Optional.ofNullable(sessionOverrides.get(chatId));
    }

    /**
     * Build language instruction for system prompt.
     *
     * @param languageCode The language code
     * @return Language instruction text for system prompt
     */
    public String buildLanguageInstruction(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return "";
        }

        Language language = Language.getLanguage(languageCode);
        if (language == null) {
            log.fine("Unknown language code: " + languageCode);
            return "";
        }

        return "## LANGUAGE REQUIREMENT (CRITICAL)\n" +
               "**PRIMARY RULE: MATCH THE USER'S INPUT LANGUAGE.**\n" +
               "- If the user writes in English, respond in English.\n" +
               "- If the user writes in German, respond in German.\n" +
               "- If the user writes in any other language, respond in THAT language.\n" +
               "- The user's session default is " + language.getName() + " (" + language.getLanguageCode() + "), " +
               "but ALWAYS match the language of each message.\n\n" +
               "This applies to ALL parts of your response - explanations, summaries, questions, and suggestions.\n" +
               "Exception: Keep technical terms (table names, column names, SQL keywords, " +
               "process names, window names) in English for accuracy.";
    }

    /**
     * Get language instruction for a session.
     * Convenience method combining getSessionLanguage and buildLanguageInstruction.
     *
     * @param ctx iDempiere context
     * @param chatId Chat ID
     * @return Language instruction text
     */
    public String getLanguageInstruction(Properties ctx, int chatId) {
        String langCode = getSessionLanguage(ctx, chatId);
        return buildLanguageInstruction(langCode);
    }

    /**
     * Get the Language object for a session.
     *
     * @param ctx iDempiere context
     * @param chatId Chat ID
     * @return Language object or null
     */
    public Language getLanguage(Properties ctx, int chatId) {
        String langCode = getSessionLanguage(ctx, chatId);
        return Language.getLanguage(langCode);
    }

    /**
     * Get acknowledgment message for language change.
     *
     * @param languageCode The new language code
     * @return Acknowledgment message in the new language
     */
    public String getLanguageChangeAcknowledgment(String languageCode) {
        Language language = Language.getLanguage(languageCode);
        if (language == null) {
            return "Language changed.";
        }

        // Provide acknowledgment in the target language
        String langName = language.getName();
        switch (languageCode) {
            case "de_DE":
            case "de_CH":
            case "de_AT":
                return "Verstanden! Ich werde jetzt auf Deutsch antworten.";
            case "es_ES":
            case "es_MX":
            case "es_AR":
                return "¡Entendido! Ahora responderé en español.";
            case "fr_FR":
            case "fr_CA":
            case "fr_BE":
                return "Compris ! Je répondrai maintenant en français.";
            case "it_IT":
                return "Capito! Ora risponderò in italiano.";
            case "pt_BR":
            case "pt_PT":
                return "Entendido! Agora vou responder em português.";
            case "nl_NL":
            case "nl_BE":
                return "Begrepen! Ik zal nu in het Nederlands antwoorden.";
            case "pl_PL":
                return "Rozumiem! Teraz będę odpowiadać po polsku.";
            case "ru_RU":
                return "Понял! Теперь я буду отвечать на русском языке.";
            case "sk_SK":
                return "Rozumiem! Teraz budem odpovedať po slovensky.";
            case "cs_CZ":
                return "Rozumím! Nyní budu odpovídat česky.";
            case "hu_HU":
                return "Értem! Most magyarul fogok válaszolni.";
            default:
                return "Understood! I will now respond in " + langName + ".";
        }
    }

    /**
     * Clear all session overrides (for cleanup or testing).
     */
    public void clearAllOverrides() {
        int count = sessionOverrides.size();
        sessionOverrides.clear();
        log.info("Cleared " + count + " language overrides");
    }

    /**
     * Get the number of active session overrides.
     *
     * @return Number of active overrides
     */
    public int getActiveOverrideCount() {
        return sessionOverrides.size();
    }
}
