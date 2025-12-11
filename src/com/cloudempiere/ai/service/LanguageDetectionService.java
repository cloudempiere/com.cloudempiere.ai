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

import org.compiere.model.MClient;
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
        Pattern.compile("(?i)\\bin\\s+(\\w+)\\s+(?:please|bitte|por favor|s'il vous pla[iî]t|prosím|kérem)$"),
        Pattern.compile("(?i)\\b(\\w+)\\s+(?:please|bitte|por favor|s'il vous pla[iî]t|prosím|kérem)$"),
        // German patterns
        Pattern.compile("(?i)\\bauf\\s+(deutsch|englisch|spanisch|franz[oö]sisch|italienisch)\\b"),
        Pattern.compile("(?i)\\bantworte?\\s+(?:mir\\s+)?(?:auf|in)\\s+(\\w+)"),
        // Spanish patterns
        Pattern.compile("(?i)\\ben\\s+(espa[nñ]ol|ingl[eé]s|franc[eé]s|alem[aá]n|italiano)\\b"),
        Pattern.compile("(?i)\\bresponde?\\s+en\\s+(\\w+)"),
        // French patterns
        Pattern.compile("(?i)\\ben\\s+(fran[cç]ais|anglais|espagnol|allemand|italien)\\b"),
        Pattern.compile("(?i)\\br[eé]ponds?\\s+en\\s+(\\w+)"),
        // Slovak patterns
        Pattern.compile("(?i)\\b(?:prepni|prepnime|prepnite|prejdi|prejdime)\\s+(?:do|na)\\s+(\\w+)"),
        Pattern.compile("(?i)\\b(?:odpovedaj|odpovedajte)\\s+(?:v|po)\\s+(\\w+)"),
        Pattern.compile("(?i)\\b(?:po|v)\\s+(sloven[sč]ky|[cč]esky|anglicky|nemecky|maďarsky|poľsky)\\b"),
        // Czech patterns
        Pattern.compile("(?i)\\b(?:přepni|přepněte|přejdi|přejděte)\\s+(?:do|na)\\s+(\\w+)"),
        Pattern.compile("(?i)\\b(?:odpovídej|odpovídejte)\\s+(?:v|česky|anglicky|německy|maďarsky|polsky)"),
        Pattern.compile("(?i)\\b(?:v|po)\\s+(česky|anglicky|slovensky|německy|maďarsky|polsky)\\b"),
        // Hungarian patterns
        Pattern.compile("(?i)\\b(?:válaszolj|válaszoljunk|válts|váltsunk)\\s+(?:magyarul|angolul|németül|franciául|spanyolul|szlovákul|csehül)\\b"),
        Pattern.compile("(?i)\\bmagyarul\\s+kérem\\b"),
        // Polish patterns
        Pattern.compile("(?i)\\b(?:przełącz|przejdź|odpowiadaj)\\s+(?:na|po)\\s+(\\w+)"),
        Pattern.compile("(?i)\\b(?:po|w)\\s+(polsku|angielsku|niemiecku|francusku|hiszpańsku)\\b")
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
        LANGUAGE_MAP.put("polsku", "pl_PL");
        LANGUAGE_MAP.put("русский", "ru_RU");
        LANGUAGE_MAP.put("中文", "zh_CN");
        LANGUAGE_MAP.put("日本語", "ja_JP");
        LANGUAGE_MAP.put("한국어", "ko_KR");
        LANGUAGE_MAP.put("العربية", "ar_SA");
        LANGUAGE_MAP.put("עברית", "he_IL");
        LANGUAGE_MAP.put("türkçe", "tr_TR");
        LANGUAGE_MAP.put("čeština", "cs_CZ");
        LANGUAGE_MAP.put("česky", "cs_CZ");
        LANGUAGE_MAP.put("cesky", "cs_CZ");
        LANGUAGE_MAP.put("slovenčina", "sk_SK");
        LANGUAGE_MAP.put("slovenský", "sk_SK");
        LANGUAGE_MAP.put("slovensky", "sk_SK");
        LANGUAGE_MAP.put("slovenciny", "sk_SK"); // genitive form
        LANGUAGE_MAP.put("magyar", "hu_HU");
        LANGUAGE_MAP.put("magyarul", "hu_HU");
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

        // Slovak language names (used in patterns)
        LANGUAGE_MAP.put("anglicky", "en_US");
        LANGUAGE_MAP.put("nemecky", "de_DE");
        LANGUAGE_MAP.put("maďarsky", "hu_HU");
        LANGUAGE_MAP.put("poľsky", "pl_PL");

        // Czech language names (used in patterns)
        LANGUAGE_MAP.put("německy", "de_DE");
        LANGUAGE_MAP.put("maďarsky", "hu_HU");
        LANGUAGE_MAP.put("polsky", "pl_PL");

        // Hungarian language names (used in patterns)
        LANGUAGE_MAP.put("angolul", "en_US");
        LANGUAGE_MAP.put("németül", "de_DE");
        LANGUAGE_MAP.put("franciául", "fr_FR");
        LANGUAGE_MAP.put("spanyolul", "es_ES");
        LANGUAGE_MAP.put("szlovákul", "sk_SK");
        LANGUAGE_MAP.put("csehül", "cs_CZ");

        // Polish language names (used in patterns)
        LANGUAGE_MAP.put("angielsku", "en_US");
        LANGUAGE_MAP.put("niemiecku", "de_DE");
        LANGUAGE_MAP.put("francusku", "fr_FR");
        LANGUAGE_MAP.put("hiszpańsku", "es_ES");

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
     *   <li>Tenant/Client language (AD_Client → AD_Language)</li>
     *   <li>Fallback: en_US</li>
     * </ol>
     *
     * @param ctx iDempiere context
     * @param chatId Chat ID (for session override lookup)
     * @return Effective language code (e.g., "de_DE")
     */
    public String getSessionLanguage(Properties ctx, int chatId) {
        log.warning("[LANGUAGE] getSessionLanguage called for chat " + chatId);

        // 1. Check session override first
        String override = sessionOverrides.get(chatId);
        if (override != null && !override.isBlank()) {
            Language lang = Language.getLanguage(override);
            String langName = lang != null ? lang.getName() : override;
            log.warning("[LANGUAGE] ✓ Priority 1: Session override → " + langName + " (" + override + ")");
            return override;
        } else {
            log.warning("[LANGUAGE] ✗ Priority 1: No session override found");
        }

        // 2. Use iDempiere context language (user login language)
        if (ctx != null) {
            String adLanguage = Env.getAD_Language(ctx);
            if (adLanguage != null && !adLanguage.isBlank()) {
                Language lang = Language.getLanguage(adLanguage);
                String langName = lang != null ? lang.getName() : adLanguage;
                log.warning("[LANGUAGE] ✓ Priority 2: User login language → " + langName + " (" + adLanguage + ")");
                return adLanguage;
            } else {
                log.warning("[LANGUAGE] ✗ Priority 2: User login language is null/blank");
            }

            // 3. Use tenant/client language
            int clientId = Env.getAD_Client_ID(ctx);
            if (clientId > 0) {
                try {
                    MClient client = MClient.get(ctx, clientId);
                    if (client != null) {
                        String clientLang = client.getAD_Language();
                        if (clientLang != null && !clientLang.isBlank()) {
                            Language lang = Language.getLanguage(clientLang);
                            String langName = lang != null ? lang.getName() : clientLang;
                            log.warning("[LANGUAGE] ✓ Priority 3: Tenant/Client language → " + langName + " (" + clientLang + ")");
                            return clientLang;
                        } else {
                            log.warning("[LANGUAGE] ✗ Priority 3: Client language is null/blank for client " + clientId);
                        }
                    } else {
                        log.warning("[LANGUAGE] ✗ Priority 3: Client not found for ID " + clientId);
                    }
                } catch (Exception e) {
                    log.warning("[LANGUAGE] ✗ Priority 3: Error loading client " + clientId + ": " + e.getMessage());
                }
            } else {
                log.warning("[LANGUAGE] ✗ Priority 3: Client ID is 0 or negative");
            }
        } else {
            log.warning("[LANGUAGE] ✗ Priority 2-3: Context is null");
        }

        // 4. Final fallback to English
        log.warning("[LANGUAGE] ✓ Priority 4: Final fallback → English (" + DEFAULT_LANGUAGE + ")");
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
                    log.warning("[LANGUAGE] Detected language change request: '" + languageName + "' -> " + langCode);
                    return Optional.of(langCode);
                } else {
                    log.warning("[LANGUAGE] Unrecognized language name in request: " + languageName);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Detect the language of the input text based on character patterns and common words.
     * This is used to automatically set the session language based on the first user message.
     *
     * <p>Detection priority:</p>
     * <ol>
     *   <li>Script-based detection (Cyrillic, CJK, Arabic, Hebrew, Thai, etc.)</li>
     *   <li>Diacritic patterns for European languages</li>
     *   <li>Common word patterns</li>
     * </ol>
     *
     * @param text The input text to analyze
     * @return Optional containing the detected language code, or empty if cannot determine
     */
    public Optional<String> detectInputLanguage(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        log.warning("[LANGUAGE] Detecting input language from text: " + text.substring(0, Math.min(100, text.length())));
        String normalized = text.toLowerCase().trim();

        // 1. Script-based detection (non-Latin scripts are distinctive)
        if (containsCyrillic(normalized)) {
            // Could be Russian, Ukrainian, Bulgarian, Serbian...
            // Default to Russian as most common
            return Optional.of("ru_RU");
        }
        if (containsCJK(normalized)) {
            // Simplified Chinese most common in business context
            return Optional.of("zh_CN");
        }
        if (containsArabic(normalized)) {
            return Optional.of("ar_SA");
        }
        if (containsHebrew(normalized)) {
            return Optional.of("he_IL");
        }
        if (containsThai(normalized)) {
            return Optional.of("th_TH");
        }
        if (containsJapaneseKana(normalized)) {
            return Optional.of("ja_JP");
        }
        if (containsKorean(normalized)) {
            return Optional.of("ko_KR");
        }
        if (containsGreek(normalized)) {
            return Optional.of("el_GR");
        }

        // 2. Latin-script languages - check diacritics and common words

        // Slovak-specific patterns (ľ, ĺ, ŕ, ô, ä with háčky and dĺžne)
        if (normalized.matches(".*[ľĺŕťďňô].*") ||
            normalized.matches(".*\\b(nie|áno|prosím|ďakujem|dobrý|deň|ako|čo|kde|kedy|prečo|som|je|sú|má|máme|chcem|potrebujem)\\b.*")) {
            return Optional.of("sk_SK");
        }

        // Czech-specific patterns (ř, ů, ě)
        if (normalized.matches(".*[řůě].*") ||
            normalized.matches(".*\\b(ano|ne|prosím|děkuji|dobrý|jak|co|kde|kdy|proč|jsem|je|jsou|má|máme|chci|potřebuji)\\b.*")) {
            return Optional.of("cs_CZ");
        }

        // Polish-specific patterns (ł, ń, ś, ź, ż, ó, ą, ę)
        if (normalized.matches(".*[łńśźżąę].*") ||
            normalized.matches(".*\\b(tak|nie|proszę|dziękuję|dzień|dobry|jak|co|gdzie|kiedy|dlaczego|jestem|jest|są|chcę|potrzebuję)\\b.*")) {
            return Optional.of("pl_PL");
        }

        // Hungarian-specific patterns (ő, ű)
        if (normalized.matches(".*[őű].*") ||
            normalized.matches(".*\\b(igen|nem|kérem|köszönöm|szép|napot|hogy|mi|hol|mikor|miért|vagyok|van|szeretnék|kell)\\b.*")) {
            return Optional.of("hu_HU");
        }

        // Romanian-specific patterns (ș, ț, ă, â, î)
        if (normalized.matches(".*[șțăâî].*") ||
            normalized.matches(".*\\b(da|nu|vă rog|mulțumesc|bună|ziua|cum|ce|unde|când|de ce|sunt|este|vreau|trebuie)\\b.*")) {
            return Optional.of("ro_RO");
        }

        // German-specific patterns (ß, ä, ö, ü with German words)
        if (normalized.matches(".*ß.*") ||
            normalized.matches(".*\\b(ich|du|sie|wir|ihr|ist|sind|haben|werden|können|möchten|bitte|danke|guten|tag|hallo|wie|was|wo|wann|warum)\\b.*")) {
            return Optional.of("de_DE");
        }

        // French-specific patterns (ç, œ, and common words)
        if (normalized.matches(".*[çœ].*") ||
            normalized.matches(".*\\b(je|tu|il|elle|nous|vous|ils|elles|suis|est|sont|avons|avez|ont|merci|bonjour|comment|quoi|où|quand|pourquoi)\\b.*")) {
            return Optional.of("fr_FR");
        }

        // Spanish-specific patterns (ñ, ¿, ¡)
        if (normalized.matches(".*[ñ¿¡].*") ||
            normalized.matches(".*\\b(yo|tú|él|ella|nosotros|ustedes|ellos|soy|es|son|tengo|tiene|tienen|gracias|hola|cómo|qué|dónde|cuándo|por qué)\\b.*")) {
            return Optional.of("es_ES");
        }

        // Italian-specific patterns
        if (normalized.matches(".*\\b(io|tu|lui|lei|noi|voi|loro|sono|è|siamo|sono|ho|ha|hanno|grazie|ciao|buongiorno|come|cosa|dove|quando|perché)\\b.*")) {
            return Optional.of("it_IT");
        }

        // Portuguese-specific patterns (ã, õ)
        if (normalized.matches(".*[ãõ].*") ||
            normalized.matches(".*\\b(eu|tu|ele|ela|nós|vocês|eles|sou|é|somos|são|tenho|tem|têm|obrigado|olá|como|que|onde|quando|por que)\\b.*")) {
            return Optional.of("pt_BR");
        }

        // Dutch-specific patterns
        if (normalized.matches(".*\\b(ik|jij|hij|zij|wij|jullie|zij|ben|is|zijn|heb|heeft|hebben|dank|hallo|goedendag|hoe|wat|waar|wanneer|waarom)\\b.*")) {
            return Optional.of("nl_NL");
        }

        // Swedish-specific patterns (å, ä, ö with Swedish words)
        if (normalized.matches(".*[åäö].*") && normalized.matches(".*\\b(jag|du|han|hon|vi|ni|de|är|har|tack|hej|hur|vad|var|när|varför)\\b.*")) {
            return Optional.of("sv_SE");
        }

        // Norwegian-specific patterns
        if (normalized.matches(".*\\b(jeg|du|han|hun|vi|dere|de|er|har|takk|hei|hvordan|hva|hvor|når|hvorfor)\\b.*")) {
            return Optional.of("no_NO");
        }

        // Danish-specific patterns
        if (normalized.matches(".*\\b(jeg|du|han|hun|vi|i|de|er|har|tak|hej|hvordan|hvad|hvor|hvornår|hvorfor)\\b.*")) {
            return Optional.of("da_DK");
        }

        // Finnish-specific patterns (lots of double vowels, ä, ö, and distinctive endings)
        if (normalized.matches(".*\\b(minä|sinä|hän|me|te|he|olen|on|olemme|kiitos|hei|miten|mitä|missä|milloin|miksi|" +
                "yli|ylivoimaisesti|miljoonan|euron|myynnillä|tilauksella|lähes|asiakas|suurin)\\b.*")) {
            log.warning("[LANGUAGE] Detected Finnish from word patterns");
            return Optional.of("fi_FI");
        }

        // Turkish-specific patterns (ı, ğ, ş)
        if (normalized.matches(".*[ığş].*") ||
            normalized.matches(".*\\b(ben|sen|o|biz|siz|onlar|var|yok|teşekkür|merhaba|nasıl|ne|nerede|ne zaman|neden)\\b.*")) {
            return Optional.of("tr_TR");
        }

        // Croatian/Serbian Latin-specific patterns (č, ć, đ, š, ž)
        if (normalized.matches(".*[čćđšž].*") && !normalized.matches(".*[ľĺŕťň].*")) {
            // Croatian more common in business
            return Optional.of("hr_HR");
        }

        // Slovenian-specific patterns
        if (normalized.matches(".*\\b(jaz|ti|on|ona|mi|vi|oni|sem|je|smo|so|hvala|zdravo|kako|kaj|kje|kdaj|zakaj)\\b.*")) {
            return Optional.of("sl_SI");
        }

        // English - check common words as fallback
        if (normalized.matches(".*\\b(the|is|are|was|were|have|has|had|do|does|did|will|would|could|should|can|may|must|i|you|he|she|it|we|they|what|where|when|why|how|hello|hi|please|thank|thanks)\\b.*")) {
            log.warning("[LANGUAGE] Detected English from text patterns");
            return Optional.of("en_US");
        }

        // Cannot determine - log error and return empty
        log.severe("[LANGUAGE] ⚠ ERROR: Cannot auto-detect language from text: " + normalized.substring(0, Math.min(100, normalized.length())));
        log.severe("[LANGUAGE] ⚠ ERROR: No distinctive patterns found (scripts, diacritics, or common words)");
        log.severe("[LANGUAGE] ⚠ ERROR: Will fall back to user login or tenant language");
        return Optional.empty();
    }

    // Helper methods for script detection

    private boolean containsCyrillic(String text) {
        boolean result = text.matches(".*[\\u0400-\\u04FF].*");
        if (result) log.warning("[LANGUAGE] Detected Cyrillic script -> ru_RU");
        return result;
    }

    private boolean containsCJK(String text) {
        boolean result = text.matches(".*[\\u4E00-\\u9FFF\\u3400-\\u4DBF].*");
        if (result) log.warning("[LANGUAGE] Detected CJK script -> zh_CN");
        return result;
    }

    private boolean containsArabic(String text) {
        return text.matches(".*[\\u0600-\\u06FF].*");
    }

    private boolean containsHebrew(String text) {
        return text.matches(".*[\\u0590-\\u05FF].*");
    }

    private boolean containsThai(String text) {
        return text.matches(".*[\\u0E00-\\u0E7F].*");
    }

    private boolean containsJapaneseKana(String text) {
        return text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF].*");
    }

    private boolean containsKorean(String text) {
        return text.matches(".*[\\uAC00-\\uD7AF\\u1100-\\u11FF].*");
    }

    private boolean containsGreek(String text) {
        return text.matches(".*[\\u0370-\\u03FF].*");
    }

    /**
     * Set override language for a chat session.
     *
     * @param chatId Chat ID
     * @param languageCode Language code (e.g., "de_DE")
     */
    public void setOverrideLanguage(int chatId, String languageCode) {
        if (languageCode != null && !languageCode.isBlank()) {
            // Check if there was a previous override
            String previousOverride = sessionOverrides.get(chatId);

            sessionOverrides.put(chatId, languageCode);
            Language lang = Language.getLanguage(languageCode);
            String langName = lang != null ? lang.getName() : languageCode;

            if (previousOverride != null && !previousOverride.equals(languageCode)) {
                // Language switch detected!
                Language prevLang = Language.getLanguage(previousOverride);
                String prevLangName = prevLang != null ? prevLang.getName() : previousOverride;

                log.warning("[LANGUAGE] ========================================");
                log.warning("[LANGUAGE] ⚠ LANGUAGE SWITCH DETECTED");
                log.warning("[LANGUAGE] Chat ID: " + chatId);
                log.warning("[LANGUAGE] Previous: " + prevLangName + " (" + previousOverride + ")");
                log.warning("[LANGUAGE] Current:  " + langName + " (" + languageCode + ")");
                log.warning("[LANGUAGE] ========================================");
            } else if (previousOverride == null) {
                // First time setting override
                log.warning("[LANGUAGE] ✓ Override set for chat " + chatId + ": " + languageCode + " (" + langName + ")");
            } else {
                // Same language, no change
                log.warning("[LANGUAGE] ✓ Override confirmed for chat " + chatId + ": " + languageCode + " (" + langName + ")");
            }
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

        return "## LANGUAGE REQUIREMENT\n" +
               "Respond ENTIRELY in **" + language.getName() + "** (" + language.getLanguageCode() + ").\n" +
               "This applies to ALL parts of your response - explanations, summaries, questions, and suggestions.\n" +
               "Exception: Keep technical terms (table names, column names, SQL keywords, " +
               "process names, window names) in English for accuracy.\n\n" +
               "IMPORTANT: If the user requests a language change (e.g., \"respond in German\", \"switch to Spanish\"), " +
               "HONOR that request immediately. The system will update your language setting automatically.";
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

    /**
     * Log all active session overrides for debugging.
     * Useful for troubleshooting language issues.
     */
    public void logActiveOverrides() {
        if (sessionOverrides.isEmpty()) {
            log.warning("[LANGUAGE] No active session overrides");
            return;
        }

        log.warning("[LANGUAGE] ========================================");
        log.warning("[LANGUAGE] ACTIVE SESSION OVERRIDES: " + sessionOverrides.size());
        log.warning("[LANGUAGE] ========================================");

        for (Map.Entry<Integer, String> entry : sessionOverrides.entrySet()) {
            int chatId = entry.getKey();
            String langCode = entry.getValue();
            Language lang = Language.getLanguage(langCode);
            String langName = lang != null ? lang.getName() : langCode;

            log.warning("[LANGUAGE]   Chat " + chatId + ": " + langName + " (" + langCode + ")");
        }

        log.warning("[LANGUAGE] ========================================");
    }
}
