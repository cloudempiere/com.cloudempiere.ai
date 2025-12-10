# ADR-037: Language Detection and Session Language Management

## Status

**Proposed**

## Date

2025-12-10

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
- [ ] `LanguageDetectionService` extracts language from iDempiere context
- [ ] System detects language change requests in user messages
- [ ] Override language persists within conversation session
- [ ] All AI services consistently use the language instruction
- [ ] Technical terms remain in English regardless of response language
- [ ] Language detection works for all supported iDempiere languages

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

The system should detect explicit language change requests:

| Pattern | Language | Example |
|---------|----------|---------|
| `respond in {language}` | Any | "respond in German" |
| `switch to {language}` | Any | "switch to Spanish" |
| `answer in {language}` | Any | "answer in French" |
| `use {language}` | Any | "use Italian" |
| `{language} please` | Any | "German please" |
| `auf Deutsch` | German | "Erkläre das auf Deutsch" |
| `en español` | Spanish | "responde en español" |
| `en français` | French | "réponds en français" |

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
| User with `de_DE` login starts chat | Responses in German |
| User says "respond in Spanish" | Responses switch to Spanish |
| User continues conversation | Spanish persists |
| User says "switch to English" | Responses return to English |
| User starts new chat | Reverts to login language (de_DE) |
| Unknown language request | Logs warning, no change |
| User with `en_US` says "auf Deutsch" | Switches to German |

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

### References

- [iDempiere Localization](https://wiki.idempiere.org/en/Localization)
- [idempiere-cli SupportAnswerService](https://github.com/cloudempiere/idempiere-cli) - Reference implementation
- [LangChain4j System Message](https://docs.langchain4j.dev/) - System prompt handling
