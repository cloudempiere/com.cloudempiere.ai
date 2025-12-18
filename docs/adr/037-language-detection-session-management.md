# ADR-037: Language Detection and Session Language Management

## Status

**Enhanced** (2025-12-18)

## Date

2025-12-10 (Initial Implementation)
2025-12-11 (Enhancement: Tenant Language Fallback + Slavic Language Support)
2025-12-18 (Enhancement: Strengthened Language Instruction for AI Compliance)

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

When users interact with the AI assistant in iDempiere, the system should automatically detect and respect the user's language preference from their iDempiere login context. The user's session language (`AD_Language`) is already available through `Env.getAD_Language(ctx)`, but currently our AI responses don't consistently honor this preference.

Additionally, users should be able to override their language preference for the current AI session by explicitly requesting a language change (e.g., "respond in German" or "switch to Spanish"). This override should persist for the conversation session but not permanently change the user's iDempiere language setting.

### Current State Analysis

**Existing Implementation (RAGConversationService.java):**
```java
private String buildLanguageInstruction(Properties ctx) {
    String langCode = Env.getAD_Language(ctx);
    Language language = Language.getLanguage(langCode);

    if (language == null) {
        return "";
    }

    return "\n\n## Language\n" +
           "Respond in **" + language.getName() + "** (" + language.getLanguageCode() + "). " +
           "Keep technical terms (table names, SQL) in English.";
}
```

**Reference Implementation (idempiere-cli SupportAnswerService.java):**
```java
public ToolResult askQuestion(String question, String language) {
    String effectiveLang = (language != null && !language.isBlank()) ? language : "en_US";
    // ... uses effectiveLang for response language
}
```

**Gaps Identified:**

1. **No Session Override**: Users cannot request a language change during conversation
2. **Inconsistent Application**: Not all AI services use the language instruction
3. **No Detection of Language Change Requests**: System doesn't recognize "respond in X" patterns
4. **No Memory of Overrides**: Override language isn't persisted for the conversation session
5. **Missing Language in AgentContext**: Language preference not passed to agent context

## Decision Drivers

- **User Experience**: Users expect AI to respond in their preferred language
- **International Support**: iDempiere serves users in 40+ languages
- **Session Consistency**: Language should remain consistent within a conversation
- **Override Flexibility**: Users should be able to temporarily switch languages
- **Technical Terms**: ERP terminology should remain in English for accuracy

## Considered Options

1. **System Prompt Only (Current)**
2. **Session-Aware Language Service with Override Detection**
3. **User Preference Stored in Database**

## Decision Outcome

**Chosen option:** "Session-Aware Language Service with Override Detection", because it provides the best balance of automatic language detection from iDempiere context, explicit override capability, and session persistence without permanent database changes.

### Confirmation

The decision will be confirmed when:
- [x] `LanguageDetectionService` extracts language from iDempiere context ✓
- [x] System detects language change requests in user messages ✓
- [x] System auto-detects input language on first message ✓ (2025-12-10)
- [x] Override language persists within conversation session ✓
- [x] `AIService` integrates language detection ✓ (2025-12-10)
- [x] `RAGConversationService` integrates language detection ✓
- [ ] Technical terms remain in English regardless of response language
- [ ] Language detection works for all supported iDempiere languages (partial - major languages covered)

## Pros and Cons of the Options

### Option 1: System Prompt Only (Current)

Use static language instruction from user's iDempiere context.

- Good, because simple implementation
- Good, because uses iDempiere's existing language setting
- Bad, because no override capability
- Bad, because inconsistent across services
- Bad, because doesn't detect user's language change requests

### Option 2: Session-Aware Language Service with Override Detection

Dedicated service that:
1. Initializes from iDempiere context (`AD_Language`)
2. Detects language change requests in user messages
3. Stores override in session/conversation memory
4. Provides consistent language instruction to all AI services

- Good, because respects iDempiere language setting
- Good, because allows session overrides
- Good, because detects natural language requests ("respond in Spanish")
- Good, because consistent across all AI services
- Good, because override doesn't affect iDempiere settings
- Neutral, because requires session state management
- Bad, because more complex than option 1

### Option 3: User Preference Stored in Database

Store AI language preference in a new `AIG_UserPreference` table.

- Good, because persists across sessions
- Good, because allows granular preferences per conversation type
- Bad, because over-engineered for the use case
- Bad, because conflicts with iDempiere's own language system
- Bad, because adds database maintenance complexity

## More Information

### Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Language Detection Flow                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  User Login                                                         │
│       │                                                             │
│       ▼                                                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  iDempiere Context (Properties ctx)                          │   │
│  │  - AD_Language = "de_DE" (from user's login language)       │   │
│  └───────────────────────────┬─────────────────────────────────┘   │
│                              │                                      │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  LanguageDetectionService                                    │   │
│  │  - getSessionLanguage(ctx, chatId) → effective language     │   │
│  │  - detectLanguageChangeRequest(userMessage) → Optional<Lang>│   │
│  │  - setOverrideLanguage(chatId, language)                    │   │
│  └───────────────────────────┬─────────────────────────────────┘   │
│                              │                                      │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Session Language (Priority Order)                           │   │
│  │  1. Override language (if set for this chat session)        │   │
│  │  2. iDempiere AD_Language (from user context)               │   │
│  │  3. Fallback: "en_US"                                       │   │
│  └───────────────────────────┬─────────────────────────────────┘   │
│                              │                                      │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Language Instruction in System Prompt                       │   │
│  │  "Respond in German (de_DE). Keep technical terms in English"│   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| `LanguageDetectionService` | Core language detection and override management | `service/` |
| `LanguageChangeDetector` | NLP patterns for detecting language requests | `service/` |
| `SessionLanguageStore` | Session-scoped language override storage | `service/` |
| `LanguageInstruction` | Builder for system prompt language instructions | `provider/` |

### Language Change Detection Patterns

The system detects explicit language change requests in multiple languages:

**English Patterns:**
| Pattern | Example |
|---------|---------|
| `respond in {language}` | "respond in German" |
| `switch to {language}` | "switch to Spanish" |
| `answer in {language}` | "answer in French" |
| `use {language}` | "use Italian" |
| `{language} please` | "German please" |

**German Patterns:**
| Pattern | Example |
|---------|---------|
| `auf {language}` | "auf Deutsch" |
| `antworte auf {language}` | "antworte auf Englisch" |

**Spanish Patterns:**
| Pattern | Example |
|---------|---------|
| `en {language}` | "en español" |
| `responde en {language}` | "responde en francés" |

**French Patterns:**
| Pattern | Example |
|---------|---------|
| `en {language}` | "en français" |
| `réponds en {language}` | "réponds en anglais" |

**Slovak Patterns (NEW: 2025-12-11):**
| Pattern | Example |
|---------|---------|
| `prepnime do {language}` | "prepnime do slovenciny" |
| `po {language}` | "po slovensky" |
| `odpovedaj v {language}` | "odpovedaj v anglicky" |

**Czech Patterns (NEW: 2025-12-11):**
| Pattern | Example |
|---------|---------|
| `přepni do {language}` | "přepni do češtiny" |
| `v {language}` | "v česky" |
| `odpovídej {language}` | "odpovídej česky" |

**Hungarian Patterns (NEW: 2025-12-11):**
| Pattern | Example |
|---------|---------|
| `válaszolj {language}` | "válaszolj magyarul" |
| `{language} kérem` | "magyarul kérem" |

**Polish Patterns (NEW: 2025-12-11):**
| Pattern | Example |
|---------|---------|
| `przełącz na {language}` | "przełącz na polski" |
| `po {language}` | "po polsku" |
| `odpowiadaj {language}` | "odpowiadaj po angielsku" |

### Implementation

```java
/**
 * Service for managing session language detection and overrides.
 *
 * <p>Language priority order:</p>
 * <ol>
 *   <li>Session override (user explicitly requested via "respond in X")</li>
 *   <li>iDempiere user language (AD_Language from login context)</li>
 *   <li>Fallback: en_US</li>
 * </ol>
 */
public class LanguageDetectionService {

    private static final CLogger log = CLogger.getCLogger(LanguageDetectionService.class);

    /** Session-scoped language overrides: chatId -> languageCode */
    private final Map<Integer, String> sessionOverrides = new ConcurrentHashMap<>();

    /** Patterns for detecting language change requests */
    private static final List<Pattern> LANGUAGE_CHANGE_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)\\b(?:respond|answer|reply|speak)\\s+in\\s+(\\w+)"),
        Pattern.compile("(?i)\\b(?:switch|change)\\s+(?:to|language to)\\s+(\\w+)"),
        Pattern.compile("(?i)\\buse\\s+(\\w+)\\s*(?:language)?\\b"),
        Pattern.compile("(?i)\\b(\\w+)\\s+(?:please|bitte|por favor|s'il vous plaît)$"),
        Pattern.compile("(?i)\\bauf\\s+(deutsch)\\b"),
        Pattern.compile("(?i)\\ben\\s+(español|français|italiano|português)\\b")
    );

    /** Language name to code mapping */
    private static final Map<String, String> LANGUAGE_MAP = Map.ofEntries(
        Map.entry("english", "en_US"),
        Map.entry("german", "de_DE"),
        Map.entry("deutsch", "de_DE"),
        Map.entry("spanish", "es_ES"),
        Map.entry("español", "es_ES"),
        Map.entry("french", "fr_FR"),
        Map.entry("français", "fr_FR"),
        Map.entry("italian", "it_IT"),
        Map.entry("italiano", "it_IT"),
        Map.entry("portuguese", "pt_BR"),
        Map.entry("português", "pt_BR"),
        Map.entry("dutch", "nl_NL"),
        Map.entry("polish", "pl_PL"),
        Map.entry("russian", "ru_RU"),
        Map.entry("chinese", "zh_CN"),
        Map.entry("japanese", "ja_JP"),
        Map.entry("korean", "ko_KR"),
        Map.entry("arabic", "ar_SA"),
        Map.entry("hebrew", "he_IL"),
        Map.entry("turkish", "tr_TR"),
        Map.entry("czech", "cs_CZ"),
        Map.entry("slovak", "sk_SK"),
        Map.entry("hungarian", "hu_HU"),
        Map.entry("romanian", "ro_RO"),
        Map.entry("bulgarian", "bg_BG"),
        Map.entry("croatian", "hr_HR"),
        Map.entry("slovenian", "sl_SI"),
        Map.entry("serbian", "sr_RS"),
        Map.entry("ukrainian", "uk_UA"),
        Map.entry("thai", "th_TH"),
        Map.entry("vietnamese", "vi_VN"),
        Map.entry("indonesian", "id_ID"),
        Map.entry("malay", "ms_MY")
    );

    /**
     * Get the effective language for the current session.
     *
     * @param ctx iDempiere context
     * @param chatId Chat ID (for session override lookup)
     * @return Effective language code (e.g., "de_DE")
     */
    public String getSessionLanguage(Properties ctx, int chatId) {
        // 1. Check session override first
        String override = sessionOverrides.get(chatId);
        if (override != null && !override.isBlank()) {
            return override;
        }

        // 2. Use iDempiere context language
        String adLanguage = Env.getAD_Language(ctx);
        if (adLanguage != null && !adLanguage.isBlank()) {
            return adLanguage;
        }

        // 3. Fallback to English
        return "en_US";
    }

    /**
     * Detect if user message contains a language change request.
     *
     * @param userMessage The user's message
     * @return Optional containing the detected language code, or empty
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
                    return Optional.of(langCode);
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
        sessionOverrides.remove(chatId);
    }

    /**
     * Build language instruction for system prompt.
     *
     * @param languageCode The language code
     * @return Language instruction text for system prompt
     */
    public String buildLanguageInstruction(String languageCode) {
        Language language = Language.getLanguage(languageCode);
        if (language == null) {
            return "";
        }

        return "\n\n## Language\n" +
               "Respond in **" + language.getName() + "** (" + language.getLanguageCode() + "). " +
               "Keep technical terms (table names, column names, SQL keywords, " +
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
}
```

### Integration Points

#### 1. AIService Integration

```java
// In AIService.chatStreamingWithContext() or similar
public void processMessage(Properties ctx, MAIChat chat, String userMessage) {
    // Check for language change request
    Optional<String> requestedLang = languageService.detectLanguageChangeRequest(userMessage);
    if (requestedLang.isPresent()) {
        languageService.setOverrideLanguage(chat.getCM_Chat_ID(), requestedLang.get());
        // Optionally acknowledge the language change
    }

    // Get effective language for system prompt
    String langInstruction = languageService.getLanguageInstruction(ctx, chat.getCM_Chat_ID());

    // Include in system prompt
    String systemPrompt = baseSystemPrompt + langInstruction;
    // ... continue with AI call
}
```

#### 2. AgentContext Integration

```java
// In AgentContext.java - add language to metadata
public class AgentContext {
    // ... existing fields

    public void setSessionLanguage(String languageCode) {
        this.metadata.put("AD_Language", languageCode);
    }

    public String getSessionLanguage() {
        return (String) this.metadata.getOrDefault("AD_Language", "en_US");
    }
}
```

#### 3. RAGConversationService Integration

```java
// Update buildSystemPrompt to use LanguageDetectionService
private String buildSystemPrompt(Properties ctx, int chatId) {
    StringBuilder prompt = new StringBuilder();
    prompt.append(loadBaseSystemPrompt());

    // Use LanguageDetectionService instead of local method
    String langInstruction = languageService.getLanguageInstruction(ctx, chatId);
    prompt.append(langInstruction);

    return prompt.toString();
}
```

### Session State Management

Language overrides are stored in a `ConcurrentHashMap` within the service, keyed by `CM_Chat_ID`. This provides:

1. **Thread Safety**: Concurrent access support
2. **Session Scope**: Overrides are per-conversation, not global
3. **Memory Efficiency**: Cleared when chat session ends
4. **No Database Impact**: Does not modify iDempiere language settings

For production, consider:
- TTL-based expiration (e.g., 24 hours)
- Maximum capacity with LRU eviction
- Distributed cache for clustered deployments

### Testing Strategy

| Test Case | Expected Result |
|-----------|-----------------|
| User with `en_US` login starts chat in Slovak | Session language set to Slovak, responses in Slovak |
| User continues conversation in Slovak | Slovak persists throughout session |
| User switches to German mid-conversation | Session language updates to German |
| User says "respond in Spanish" (explicit) | Responses switch to Spanish |
| User starts new chat | Session language resets (detected from first message) |
| User with `de_DE` login starts chat in German | German (matches both login and input) |
| Unknown language request | Logs warning, no change |

### Key Design Decision: Input Language Takes Priority

**Decision Date:** 2025-12-10

**Problem:** Users may be logged into iDempiere with one language (e.g., Kinyarwanda) but
write messages in a different language (e.g., English). The original design strictly followed
the session language (`AD_Language`), causing confusing responses in unexpected languages.

**Decision:** The system now detects the language of the user's **first message** and uses
that to set the session language. This is the standard expected UX behavior.

**Language Priority Order (Updated 2025-12-11):**
1. **Session override** - Set by explicit request ("respond in German") or auto-detected from first message
2. **iDempiere context** - `AD_Language` from user login (only if no override)
3. **Tenant/Client language** - `AD_Client` → `AD_Language` (NEW: 2025-12-11)
4. **Fallback** - `en_US`

**Rationale:**
- Users expect AI to respond in the language they write in
- Multi-lingual environments (like Africa with 40+ languages) often have users
  with a system language different from their preferred communication language
- Auto-detection on first message sets the tone for the entire conversation
- Explicit requests ("respond in X") can always override

**Implementation:**
- `LanguageDetectionService.detectInputLanguage(text)` - Detects language using character
  scripts (Cyrillic, CJK, Arabic, etc.) and common word patterns
- Called in `AIService.chatStreamingWithContext()` before building the system prompt
- Only sets override if no existing override (subsequent messages don't change it)
- Explicit requests ("respond in German") always update the override

### Implementation Phases

**Phase 1 (1 day): Core Service**
- Implement `LanguageDetectionService`
- Language change pattern detection
- Session override storage

**Phase 2 (1 day): Integration**
- Integrate with `AIService`
- Update `RAGConversationService`
- Add language to `AgentContext`

**Phase 3 (0.5 days): Testing**
- Unit tests for pattern detection
- Integration tests for session persistence
- Manual testing with various languages

**Total Effort:** 2.5 days

### Related ADRs

- [ADR-015](015-conversational-ux-patterns.md) - Response formatting guidelines apply regardless of language
- [ADR-031](031-chat-panel-langchain4j-chatmodel-integration.md) - Chat integration architecture
- [ADR-036](036-chat-ownership-and-sharing-model.md) - Chat session management

### Recent Enhancements (2025-12-11)

#### 1. Tenant/Client Language Fallback

**Problem:** Users without configured login language fell back to hardcoded English, ignoring tenant configuration.

**Solution:** Added Priority 3 fallback to tenant/client language:
```java
// 3. Use tenant/client language
int clientId = Env.getAD_Client_ID(ctx);
if (clientId > 0) {
    MClient client = MClient.get(ctx, clientId);
    if (client != null) {
        String clientLang = client.getAD_Language();
        if (clientLang != null && !clientLang.isBlank()) {
            return clientLang;
        }
    }
}
```

**Impact:** Multi-tenant deployments now respect each tenant's default language configuration.

#### 2. Enhanced Slavic Language Support

**Problem:** Users writing in Slovak, Czech, Hungarian, or Polish had their language change requests ignored.

**Solution:** Added comprehensive pattern detection for Slavic languages:
- **Slovak:** `prepnime do slovenciny`, `po slovensky`, `odpovedaj v anglicky`
- **Czech:** `přepni do češtiny`, `v česky`, `odpovídej česky`
- **Hungarian:** `válaszolj magyarul`, `magyarul kérem`
- **Polish:** `przełącz na polski`, `po polsku`

**Language Name Mappings Added:**
- Slovak forms: `slovenciny` (genitive), `slovensky`, `anglicky`, `nemecky`, `maďarsky`
- Czech forms: `česky`, `cesky`, `německy`, `maďarsky`
- Hungarian forms: `magyarul`, `angolul`, `németül`, `szlovákul`
- Polish forms: `polsku`, `angielsku`, `niemiecku`

#### 3. Improved System Prompt for Language Switching

**Problem:** AI refused to switch languages when requested, citing strict "DO NOT switch" instruction.

**Before:**
```
"DO NOT switch to another language mid-response. Maintain {language} throughout."
```

**After:**
```
"IMPORTANT: If the user requests a language change (e.g., 'respond in German', 'switch to Spanish'),
HONOR that request immediately. The system will update your language setting automatically."
```

**Impact:** AI now immediately honors language change requests instead of refusing them.

#### 4. Enhanced Error Logging

**Problem:** Auto-detection failures were logged as warnings, making debugging difficult.

**Solution:** Changed to `log.severe()` with detailed error messages:
```java
log.severe("[LANGUAGE] ⚠ ERROR: Cannot auto-detect language from text: " + text);
log.severe("[LANGUAGE] ⚠ ERROR: No distinctive patterns found (scripts, diacritics, or common words)");
log.severe("[LANGUAGE] ⚠ ERROR: Will fall back to user login or tenant language");
```

**Impact:** Detection failures are now clearly visible in production logs for troubleshooting.

#### 5. Strengthened Language Instruction for AI Compliance (2025-12-18)

**Problem:** Despite language detection working correctly (detecting Slovak, Czech, etc.), the AI
continued responding in English. The language instruction was too weak and easily ignored by the model.

**Root Cause Analysis:**
- Original instruction was polite and suggestive: "Respond in **Slovak**"
- AI models prioritize English unless explicitly and strongly instructed otherwise
- Weak language instruction allowed the model to default to English for "clarity"
- Users writing in Slovak received English responses, breaking conversational flow

**Solution:** Dramatically strengthened the language instruction with multiple reinforcement techniques:

**Before (Weak Instruction):**
```
## LANGUAGE REQUIREMENT
Respond ENTIRELY in **Slovak** (sk_SK).
This applies to ALL parts of your response - explanations, summaries, questions, and suggestions.
Exception: Keep technical terms (table names, column names, SQL keywords, process names, window names) in English for accuracy.
```

**After (Strong Instruction):**
```
## CRITICAL LANGUAGE REQUIREMENT - HIGHEST PRIORITY

🔴 **MANDATORY:** You MUST respond EXCLUSIVELY and COMPLETELY in **Slovak** (sk_SK).

This is NON-NEGOTIABLE and applies to:
- ✅ ALL explanations and descriptions
- ✅ ALL questions you ask the user
- ✅ ALL suggestions and recommendations
- ✅ ALL data summaries and analysis
- ✅ ALL error messages and warnings
- ✅ ALL introductory and concluding statements

ONLY EXCEPTION: Technical identifiers (table names like 'C_Order', column names like 'DocumentNo',
SQL keywords, ERP process names, window names) remain in English for technical accuracy.

❌ DO NOT mix languages - user speaks Slovak, you respond in Slovak.
❌ DO NOT default to English - this is explicitly forbidden.
❌ DO NOT explain in English - everything in Slovak.

If user requests language change, acknowledge and switch immediately.
```

**Key Enhancements:**

1. **Priority Markers:** "CRITICAL", "HIGHEST PRIORITY", "MANDATORY", "NON-NEGOTIABLE"
   - Signals importance to the model's attention mechanism
   - Prevents deprioritization in favor of default English behavior

2. **Visual Emphasis:** Red circle (🔴), checkmarks (✅), cross marks (❌)
   - Draws attention in the system prompt
   - Creates visual hierarchy for scanning

3. **Explicit Prohibition List:**
   - "DO NOT mix languages" - Prevents bilingual responses
   - "DO NOT default to English" - Blocks fallback behavior
   - "DO NOT explain in English" - Reinforces full compliance

4. **Comprehensive Scope List:**
   - Enumerates all response categories (7 bullet points)
   - Leaves no ambiguity about what "ENTIRELY" means
   - Explicitly includes error messages and warnings

5. **Repetition of Language Name:**
   - Language name appears 3+ times in the instruction
   - Reinforces the target language through repetition
   - Creates stronger semantic association

**Implementation:**
```java
public String buildLanguageInstruction(String languageCode) {
    if (languageCode == null || languageCode.isBlank()) {
        return "";
    }

    Language language = Language.getLanguage(languageCode);
    if (language == null) {
        log.fine("Unknown language code: " + languageCode);
        return "";
    }

    return "## CRITICAL LANGUAGE REQUIREMENT - HIGHEST PRIORITY\n\n" +
           "🔴 **MANDATORY:** You MUST respond EXCLUSIVELY and COMPLETELY in **" + language.getName() + "** (" + language.getLanguageCode() + ").\n\n" +
           "This is NON-NEGOTIABLE and applies to:\n" +
           "- ✅ ALL explanations and descriptions\n" +
           "- ✅ ALL questions you ask the user\n" +
           "- ✅ ALL suggestions and recommendations\n" +
           "- ✅ ALL data summaries and analysis\n" +
           "- ✅ ALL error messages and warnings\n" +
           "- ✅ ALL introductory and concluding statements\n\n" +
           "ONLY EXCEPTION: Technical identifiers (table names like 'C_Order', column names like 'DocumentNo', " +
           "SQL keywords, ERP process names, window names) remain in English for technical accuracy.\n\n" +
           "❌ DO NOT mix languages - user speaks " + language.getName() + ", you respond in " + language.getName() + ".\n" +
           "❌ DO NOT default to English - this is explicitly forbidden.\n" +
           "❌ DO NOT explain in English - everything in " + language.getName() + ".\n\n" +
           "If user requests language change, acknowledge and switch immediately.";
}
```

**Impact:**
- ✅ AI now consistently responds in the detected/requested language
- ✅ Slovak, Czech, Hungarian, Polish users receive responses in their language
- ✅ Language compliance maintained even with complex technical queries
- ✅ Technical terms (C_Order, DocumentNo, SQL) correctly preserved in English

**Testing Results:**
- ✅ Slovak prompt → Slovak response (previously English)
- ✅ Czech prompt → Czech response (previously English)
- ✅ Hungarian prompt → Hungarian response (previously English)
- ✅ English prompt → English response (unchanged)

**Prompt Engineering Principle:**

This enhancement demonstrates a key principle in AI system design:
> **Explicit is better than implicit.** AI models require strong, unambiguous instructions
> with explicit prohibitions to override their default behaviors. Polite suggestions
> are easily ignored; emphatic commands with visual markers are not.

**File Changed:**
- `src/com/cloudempiere/ai/service/LanguageDetectionService.java` - `buildLanguageInstruction()` method

**Commit:** `7dc886b` (2025-12-18)

### References

- [iDempiere Localization](https://wiki.idempiere.org/en/Localization)
- [idempiere-cli SupportAnswerService](https://github.com/cloudempiere/idempiere-cli) - Reference implementation
- [LangChain4j System Message](https://docs.langchain4j.dev/) - System prompt handling
- [Prompt Engineering Guide](https://www.promptingguide.ai/) - Instruction strength and compliance
