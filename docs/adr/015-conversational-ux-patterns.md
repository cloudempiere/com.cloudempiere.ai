# ADR-015: Conversational UX Patterns

<!-- MADR 3.0 Template -->

## Status

Accepted

## Date

2025-12-01

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

ERP systems contain complex, interconnected data that users traditionally access through forms and reports. AI-powered conversational interfaces can simplify access but require careful design to avoid confusion, incomplete information gathering, and poor user experiences. Currently, our implementation lacks standardized conversation flows, parameter collection patterns, response formatting guidelines, and proactive insight delivery. Without defined UX patterns, users will experience inconsistent AI interactions.

## Decision Drivers

- **User Adoption**: Poor UX leads to low AI feature adoption
- **Task Completion**: Users must complete tasks efficiently through conversation
- **Context Preservation**: Multi-turn conversations must maintain context
- **Error Recovery**: Users must easily recover from misunderstandings
- **Discoverability**: Users should discover AI capabilities naturally

## Considered Options

1. **Parameter Chain Pattern + Structured Responses**
2. **Form-Based Wizard Pattern**
3. **Free-Form Natural Language Only**

## Decision Outcome

**Chosen option:** "Parameter Chain Pattern + Structured Responses", because it allows flexible parameter collection in any order, provides consistent response formatting that integrates with iDempiere UI, and balances natural conversation with structured data display.

### Confirmation

The decision will be confirmed when:
- [ ] `ParameterChainHandler` collects required parameters in any order
- [ ] `ResponseFormatter` produces consistent markdown with action links
- [ ] `ProactiveInsightEngine` surfaces warnings (e.g., credit limits, approval required)
- [ ] User testing shows >80% task completion rate in 3 or fewer messages
- [ ] Response length stays under 3 screen-heights for mobile

## Pros and Cons of the Options

### Option 1: Parameter Chain Pattern + Structured Responses

Flexible parameter collection with consistent formatted output.

- Good, because users can provide information in any order
- Good, because partial inputs are accepted and missing items requested
- Good, because structured responses integrate with iDempiere UI
- Good, because proactive insights improve user awareness
- Neutral, because requires conversation state management
- Bad, because more complex than rigid flows

### Option 2: Form-Based Wizard Pattern

Sequential step-by-step data collection.

- Good, because predictable flow
- Good, because easy to implement
- Good, because clear progress indication
- Bad, because feels robotic and frustrating
- Bad, because cannot handle out-of-order information
- Bad, because poor conversational feel

### Option 3: Free-Form Natural Language Only

Rely entirely on LLM to interpret and respond.

- Good, because most natural feel
- Good, because lowest implementation effort
- Bad, because inconsistent responses
- Bad, because cannot ensure required data collected
- Bad, because hard to integrate with ERP workflows
- Bad, because unpredictable token costs

## More Information

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  Conversational UX Pipeline                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  User Message                                               │
│       │                                                     │
│       ▼                                                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Intent Recognition                                  │   │
│  │  - Query (read-only lookup)                         │   │
│  │  - Action (create/update/process)                   │   │
│  │  - Clarification (follow-up question)               │   │
│  └───────────────────────────┬─────────────────────────┘   │
│                              │                              │
│                              ▼                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Parameter Chain Handler                             │   │
│  │  - Extract provided parameters                      │   │
│  │  - Check required vs optional                       │   │
│  │  - Apply defaults for missing optional              │   │
│  │  - Request missing required                         │   │
│  └───────────────────────────┬─────────────────────────┘   │
│                              │                              │
│                              ▼                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Action Execution (if parameters complete)          │   │
│  │  - Tool invocation via LangChain4j                  │   │
│  │  - Result retrieval                                 │   │
│  └───────────────────────────┬─────────────────────────┘   │
│                              │                              │
│                              ▼                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Response Formatter                                  │   │
│  │  - Acknowledgment (what was understood)             │   │
│  │  - Structured data (tables, lists)                  │   │
│  │  - Action links (zoom to record)                    │   │
│  │  - Proactive insights (warnings, suggestions)       │   │
│  │  - Next steps (guide forward)                       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| `ParameterChainHandler` | Flexible parameter collection | `conversation/` |
| `ResponseFormatter` | Consistent output formatting | `conversation/` |
| `ProactiveInsightEngine` | Surface warnings and suggestions | `conversation/` |
| `ConversationContextManager` | Multi-turn state management | `conversation/` |
| `ActionLinkGenerator` | Generate iDempiere zoom links | `conversation/` |

### Response Format Guidelines

```markdown
# Standard Response Structure

**Acknowledgment** (1 line)
I found 12 pending orders totaling $45,230.

**Data Display** (compact table or list)
| Order | Customer | Amount | Status |
|-------|----------|--------|--------|
| SO-1234 | Acme Corp | $12,500 | Pending |
| SO-1235 | TechCo | $8,900 | Pending |
(+10 more)

**Action Links** (when applicable)
[View SO-1234 in iDempiere](zoom://C_Order/1234)

**Proactive Insights** (warnings, flags)
> Note: 3 orders exceed credit limit and require approval.

**Next Steps** (guide forward)
Would you like to:
- See all 12 orders
- Filter by customer
- Process an order
```

### Message Length Rules

| Context | Max Lines | Max Characters |
|---------|-----------|----------------|
| Initial response | 5 | 500 |
| Data listing | 10 | 1000 |
| Detailed view | 15 | 1500 |
| Error message | 3 | 300 |

### Parameter Chain Pattern

```java
/**
 * Collect parameters in any order, request only what's missing.
 *
 * Examples:
 * - "Create order for Acme" → Ask for products
 * - "10 widgets for Acme" → Complete, create order
 * - "Order with 10 widgets" → Ask for customer
 */
public class ParameterChainHandler {

    private final Map<String, ParameterSpec> requiredParams;
    private final Map<String, ParameterSpec> optionalParams;
    private final Map<String, Object> collectedParams;

    public ChainResult process(String userMessage) {
        // 1. Extract any recognizable parameters
        extractParameters(userMessage);

        // 2. Apply defaults for optional params
        applyDefaults();

        // 3. Check if all required params present
        List<String> missing = getMissingRequired();

        if (missing.isEmpty()) {
            return ChainResult.complete(collectedParams);
        } else {
            String question = generateQuestion(missing.get(0));
            return ChainResult.needsMore(question, missing);
        }
    }

    private String generateQuestion(String paramName) {
        ParameterSpec spec = requiredParams.get(paramName);
        return switch (paramName) {
            case "customerId" -> "Which customer is this order for?";
            case "productLines" -> "What products would you like to add?";
            case "warehouse" -> "Which warehouse should fulfill this order?";
            default -> "Please provide: " + spec.getDisplayName();
        };
    }
}
```

### Proactive Insights

| Insight Type | Trigger | Example Message |
|--------------|---------|-----------------|
| Credit Warning | Order total > credit available | "Customer's credit limit ($10,000) would be exceeded by $2,500" |
| Approval Required | Amount > threshold | "This order requires manager approval (amount > $10,000)" |
| Stock Alert | Quantity > available | "Only 5 units available, 10 requested" |
| Price Change | Price differs from last order | "Product price has changed: was $99, now $109 (+10%)" |
| Duplicate Warning | Similar recent order exists | "Similar order SO-1230 created 2 hours ago for same customer" |

### iDempiere Integration

```java
/**
 * Generate zoom links to iDempiere windows.
 */
public class ActionLinkGenerator {

    public String generateZoomLink(String tableName, int recordId) {
        // Format: idempiere://zoom/TableName/RecordID
        return String.format("[View in iDempiere](idempiere://zoom/%s/%d)",
            tableName, recordId);
    }

    public String generateWindowLink(int adWindowId, int recordId) {
        // Format: idempiere://window/AD_Window_ID?Record_ID=value
        return String.format("[Open Window](idempiere://window/%d?Record_ID=%d)",
            adWindowId, recordId);
    }

    public String generateProcessLink(int adProcessId, Map<String, Object> params) {
        // Format: idempiere://process/AD_Process_ID?param1=value1&param2=value2
        StringBuilder url = new StringBuilder();
        url.append("idempiere://process/").append(adProcessId);
        if (!params.isEmpty()) {
            url.append("?");
            url.append(params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&")));
        }
        return String.format("[Run Process](%s)", url);
    }
}
```

### Implementation Notes

**Phase 1 (2 days):** ParameterChainHandler
- Parameter extraction from natural language
- Missing parameter detection
- Natural question generation

**Phase 2 (2 days):** ResponseFormatter
- Markdown response templates
- Table/list formatting helpers
- Action link generation

**Phase 3 (2 days):** ProactiveInsightEngine
- Business rule integration
- Warning/suggestion generation
- Contextual insight triggering

**Phase 4 (2 days):** ConversationContextManager
- Multi-turn state persistence
- Context window management
- Session cleanup

**Total Effort:** 8 days

### Related ADRs

- [ADR-008](008-llm-instruction-following.md) - System prompts define response style
- [ADR-011](011-specialized-agent-scopes.md) - Domain agents have domain-specific UX
- [ADR-014](014-guardrails-and-safety.md) - Guardrails may interrupt conversation flow

### References

- [Botpress - Conversation Design](https://botpress.com/blog/conversation-design)
- [IBM - Chatbot Design](https://www.ibm.com/think/topics/chatbot-design)
- [Master of Code - Parameter Chain Pattern](https://masterofcode.com/blog/conversational-design-series-3-the-parameter-chain-design-pattern)
- [AI_MCP_ERP_BEST_PRACTICES.md](../AI_MCP_ERP_BEST_PRACTICES.md) Section 9
