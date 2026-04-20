# ADR-038: User-Friendly Error Handling and Issue Tracking

## Status

Accepted — **Partially Wired**: `AIErrorHandler` class and migration scripts exist; **not called** from `AIService` catch blocks — errors are returned as plain strings, not persisted to `AD_Issue`

## Date

2025-12-10

## Deciders

- Cloudempiere AI Team

## Context and Problem Statement

When AI operations fail (API errors, timeouts, rate limits, etc.), users were seeing raw technical error messages like stack traces and exception details. This created a poor user experience because:

1. Technical errors are confusing and unhelpful to business users
2. Users don't know what action to take to resolve the issue
3. Support teams lack context when users report problems
4. No systematic tracking of AI-related errors for debugging

We need a user-friendly error handling system that shows meaningful messages in the user's language while preserving technical details for support and debugging.

## Decision Drivers

- **User Experience**: Users must see friendly, actionable messages, never raw technical errors
- **Internationalization**: Error messages must be translatable via AD_Message
- **Supportability**: Technical details must be logged for debugging with trackable reference codes
- **Actionability**: Users must know what to do next (retry, rephrase, contact admin, etc.)
- **iDempiere Integration**: Use existing AD_Issue and AD_Message infrastructure

## Considered Options

1. **Simple error message mapping** - Map error types to static strings in code
2. **AD_Message with AD_Issue integration** - Use iDempiere's message system with issue tracking
3. **External error tracking service** - Use third-party error tracking (Sentry, etc.)

## Decision Outcome

**Chosen option:** "AD_Message with AD_Issue integration", because it:
- Leverages existing iDempiere infrastructure for translations
- Provides built-in issue tracking without external dependencies
- Allows administrators to customize messages per tenant
- Creates audit trail for support cases

### Confirmation

- [x] `AIErrorHandler.java` implemented (`com.cloudempiere.ai.error.AIErrorHandler`) — **class exists**
- [x] 7 error categories defined (RATE_LIMIT, TIMEOUT, CONTENT_FILTER, CONFIGURATION, CONTEXT_LENGTH, SERVICE_UNAVAILABLE, GENERIC)
- [x] AD_Message entries created (IDs 800100–800106) via migration scripts
- [x] Migration scripts: `migration/postgresql/202512101430_CLD-ERROR-MESSAGES.sql`
- [ ] `AIErrorHandler` called from `AIService` catch blocks — **NOT DONE**: catch blocks return plain error strings (e.g. `"I cannot process this request: " + e.getMessage()`) without calling `AIErrorHandler`
- [ ] AD_Issue creation on error — **NOT TRIGGERED**: `AIErrorHandler.handleError()` is never called, so no `AD_Issue` records are created
- [ ] AIChatWidget displaying friendly messages with reference codes — **NOT WIRED** end-to-end; the widget would need to receive structured error results from `AIErrorHandler`
- [ ] AD_Message_Trl translations (pending per-language localization)
- [ ] "Submit to Support" button (future enhancement)

## Pros and Cons of the Options

### Option 1: Simple Error Message Mapping

Map error types to hardcoded strings in Java code.

- Good, because simple to implement
- Good, because no database dependency
- Bad, because not translatable without code changes
- Bad, because no tracking or audit trail
- Bad, because support has no context for debugging

### Option 2: AD_Message with AD_Issue Integration (Chosen)

Use AD_Message for user-facing text and AD_Issue for technical tracking.

- Good, because messages are translatable via standard iDempiere tools
- Good, because AD_Issue provides full context for support
- Good, because error reference codes link user reports to technical details
- Good, because integrates with existing iDempiere workflows
- Neutral, because requires database records (migration scripts)
- Bad, because slightly more complex than hardcoded strings

### Option 3: External Error Tracking Service

Use third-party services like Sentry, Rollbar, or Bugsnag.

- Good, because advanced error analytics and alerting
- Good, because cross-platform error tracking
- Bad, because external dependency and cost
- Bad, because data leaves the system (privacy concerns)
- Bad, because doesn't integrate with iDempiere workflows

## More Information

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        User Request                              │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AI Service Layer                              │
│                                                                  │
│  ┌──────────────┐    Error    ┌──────────────────────────────┐ │
│  │ LangChain4j  │ ──────────► │     AIErrorHandler           │ │
│  │   Bedrock    │             │                              │ │
│  │   Anthropic  │             │  1. Categorize error         │ │
│  └──────────────┘             │  2. Generate reference code  │ │
│                               │  3. Create AD_Issue          │ │
│                               │  4. Get AD_Message text      │ │
│                               │  5. Return user message      │ │
│                               └──────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    ▼                       ▼
        ┌───────────────────┐   ┌───────────────────────────┐
        │    AD_Issue       │   │      Chat UI              │
        │                   │   │                           │
        │ - Stack trace     │   │ "I'm having trouble..."   │
        │ - Error context   │   │                           │
        │ - User message    │   │ What you can do:          │
        │ - Chat ID         │   │ • Contact administrator   │
        │ - Provider info   │   │                           │
        │ - Reference code  │   │ ⚠️ (hover: AIG-xxx-xxxx)  │
        └───────────────────┘   └───────────────────────────┘
```

### Error Categories

| Category | AD_Message Key | Trigger Conditions | User Action |
|----------|---------------|-------------------|-------------|
| RATE_LIMIT | `AIG_Error_RateLimit` | 429, rate_limit, throttle | Wait 30 seconds, retry |
| TIMEOUT | `AIG_Error_Timeout` | timeout, deadline exceeded | Simplify question |
| CONTENT_FILTER | `AIG_Error_ContentFilter` | blocked, safety, policy | Rephrase question |
| CONFIGURATION | `AIG_Error_Configuration` | 401, 403, api_key, auth | Contact administrator |
| CONTEXT_LENGTH | `AIG_Error_ContextLength` | max_tokens, context_length | Start new chat |
| SERVICE_UNAVAILABLE | `AIG_Error_ServiceUnavailable` | 500, 503, overloaded | Wait and retry |
| GENERIC | `AIG_Error_Generic` | Unknown errors | Retry, rephrase, or contact support |

### Error Reference Code Format

```
AIG-{unix_timestamp}-{random_4_digits}

Example: AIG-1733826380-4521
```

This code:
- Appears in the chat UI as a tooltip on the ⚠️ emoji
- Is stored in AD_Issue.Name for lookup
- Can be provided to support for quick issue identification

### AD_Message Entries

| AD_Message_ID | Value | MsgType | EntityType |
|---------------|-------|---------|------------|
| 800100 | AIG_Error_RateLimit | E | CLDE |
| 800101 | AIG_Error_Timeout | E | CLDE |
| 800102 | AIG_Error_ContentFilter | E | CLDE |
| 800103 | AIG_Error_Configuration | E | CLDE |
| 800104 | AIG_Error_ContextLength | E | CLDE |
| 800105 | AIG_Error_ServiceUnavailable | E | CLDE |
| 800106 | AIG_Error_Generic | E | CLDE |

### Implementation Components

1. **AIErrorHandler** (`com.cloudempiere.ai.error.AIErrorHandler`)
   - Categorizes errors based on message content and exception type
   - Generates unique error reference codes
   - Creates AD_Issue records with full context
   - Retrieves translated messages from AD_Message
   - Falls back to hardcoded English messages if AD_Message not found

2. **AIErrorResult** (inner class)
   - Contains user message, error reference, debug tooltip, and AD_Issue_ID
   - Provides formatted output for chat display

3. **Migration Scripts** (iDempiere v10 / iD110)
   - `migration/iD110/postgresql/202512101430_CLD-1601.sql`
   - `migration/iD110/oracle/202512101430_CLD-1601.sql`

### UI Display

Error messages are displayed in the chat as:

```
I'm having trouble connecting to the AI service.

What you can do: Please contact your system administrator
to check the AI provider configuration. ⚠️
                                        └── Tooltip: "Ref: AIG-1733826380-4521 |
                                                      Category: CONFIGURATION |
                                                      Provider: AWS Bedrock"
```

### Future Enhancements (Roadmap)

1. **"Submit to Support" Button**: Optional dialog allowing users to report errors with consent
2. **AI-Powered Translation Fallback**: Use AI to translate error messages for languages without AD_Message_Trl
3. **Error Analytics Dashboard**: Track error patterns by category, provider, and time
4. **Auto-Recovery Suggestions**: Intelligent suggestions based on error history

### Guide: Adding New Error Codes

When adding new error categories, follow this process:

#### Step 1: Identify the Error Pattern

Document the error scenario:
- **Trigger conditions**: What causes this error? (exception types, message patterns, HTTP codes)
- **Frequency**: How often does it occur?
- **User impact**: What does the user experience?
- **Resolution path**: What should the user/admin do?

#### Step 2: Create the Error Category

1. Add constant in `AIErrorHandler.java`:
   ```java
   public static final String CATEGORY_NEW_ERROR = "NEW_ERROR";
   private static final String MSG_NEW_ERROR = "AIG_Error_NewError";
   private static final String FALLBACK_NEW_ERROR = "User-friendly message. What you can do: Action hint.";
   ```

2. Add detection logic in `categorizeError()`:
   ```java
   if (message.contains("pattern1") || message.contains("pattern2")) {
       return CATEGORY_NEW_ERROR;
   }
   ```

3. Add to `getUserFriendlyMessage()` switch statement

#### Step 3: Create Migration Script

Create paired migration scripts in iD110 folder:
- `migration/iD110/postgresql/YYYYMMDDHHMM_CLD-XXXX.sql`
- `migration/iD110/oracle/YYYYMMDDHHMM_CLD-XXXX.sql`

Use next available AD_Message_ID (current range: 800100-800106, next: 800107+)

```sql
INSERT INTO AD_Message (MsgType, MsgText, AD_Client_ID, AD_Org_ID, IsActive,
    Created, CreatedBy, Updated, UpdatedBy, AD_Message_ID, Value, EntityType, AD_Message_UU)
VALUES ('E', 'User message. What you can do: Action guidance.', 0, 0, 'Y',
    NOW(), 100, NOW(), 100, 800107, 'AIG_Error_NewError', 'CLDE', gen_random_uuid());
```

#### Step 4: Add Translations (Optional)

Create `AD_Message_Trl` entries for supported languages:
```sql
INSERT INTO AD_Message_Trl (AD_Message_ID, AD_Language, AD_Client_ID, AD_Org_ID,
    IsActive, Created, CreatedBy, Updated, UpdatedBy, MsgText, IsTranslated, AD_Message_Trl_UU)
VALUES (800107, 'de_DE', 0, 0, 'Y', NOW(), 100, NOW(), 100,
    'German message. Was Sie tun können: Handlungshinweis.', 'Y', gen_random_uuid());
```

#### Step 5: Test the Error Flow

1. Simulate the error condition
2. Verify correct category detection
3. Verify AD_Issue creation with proper context
4. Verify user sees friendly message + debug tooltip
5. Verify error reference links to AD_Issue

#### Step 6: Update Documentation

1. Add to error table in this ADR
2. Update CLAUDE.md if needed
3. Document in release notes

### Error Code Registry

| ID | Category | AD_Message Key | Status | Added |
|----|----------|---------------|--------|-------|
| 800100 | RATE_LIMIT | AIG_Error_RateLimit | Active | 2025-12-10 |
| 800101 | TIMEOUT | AIG_Error_Timeout | Active | 2025-12-10 |
| 800102 | CONTENT_FILTER | AIG_Error_ContentFilter | Active | 2025-12-10 |
| 800103 | CONFIGURATION | AIG_Error_Configuration | Active | 2025-12-10 |
| 800104 | CONTEXT_LENGTH | AIG_Error_ContextLength | Active | 2025-12-10 |
| 800105 | SERVICE_UNAVAILABLE | AIG_Error_ServiceUnavailable | Active | 2025-12-10 |
| 800106 | GENERIC | AIG_Error_Generic | Active | 2025-12-10 |

*Next available ID: 800107*

### Candidate Error Codes (Future)

| Category | Trigger | User Message | Priority |
|----------|---------|--------------|----------|
| BUDGET_EXCEEDED | Cost/token limits | "Your usage limit has been reached..." | P1 |
| MODEL_NOT_AVAILABLE | Model deprecation/unavailable | "The AI model is currently unavailable..." | P2 |
| INVALID_INPUT | Malformed request | "I couldn't understand your request..." | P2 |
| QUOTA_EXCEEDED | API quota limits | "The AI service quota has been reached..." | P2 |
| NETWORK_ERROR | Connection failures | "I'm having trouble connecting..." | P3 |
| TOOL_EXECUTION_FAILED | Tool/function call error | "I encountered an issue while processing..." | P3 |

### Related ADRs

- [ADR-015](015-conversational-ux-patterns.md) - Conversational UX Patterns
- [ADR-014](014-guardrails-and-safety.md) - Guardrails and Safety
- [ADR-033](033-streaming-thinking-timeline-ux.md) - Streaming Responses and Thinking Timeline UX
- [ADR-037](037-language-detection-session-management.md) - Language Detection and Session Management

### References

- [iDempiere AD_Message Documentation](https://wiki.idempiere.org/)
- [iDempiere AD_Issue for Error Tracking](https://wiki.idempiere.org/)

---

<!--
Implementation completed: 2025-12-10
Files created/modified:
- src/com/cloudempiere/ai/error/AIErrorHandler.java (new)
- src/com/cloudempiere/ai/component/AIChatWidget.java (updated)
- migration/postgresql/202512101430_CLD-ERROR-MESSAGES.sql (new)
- migration/oracle/202512101430_CLD-ERROR-MESSAGES.sql (new)
-->
