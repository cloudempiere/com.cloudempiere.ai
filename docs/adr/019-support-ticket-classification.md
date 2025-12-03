# ADR-019: Support Ticket Auto-Classification Use Case

**Status**: Proposed
**Date**: 2025-12-03
**Deciders**: CloudEmpiere AI Team
**Related**: [ADR-002](002-langchain4j-strategic-adoption.md), [ADR-009](009-domain-boundaries-agent-scope.md), [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)

## Context

### Problem Statement

Support ticket handling is inefficient:
- Incoming customer emails are unstructured
- Support agents spend 15-20 min reading history before responding
- Manual classification of tickets causes delays (40% exceed 24h first response)
- No automatic routing to appropriate team/agent
- Inconsistent priority assignment

### Business Value

| Metric | Current | Target | Impact |
|--------|---------|--------|--------|
| First response time | 8-24 hours | <4 hours | 50%+ improvement |
| Manual classification time | 5-10 min | 0 (auto) | Full automation |
| Classification accuracy | Variable | 85%+ | Consistency |
| Ticket routing accuracy | Manual | 90%+ | Faster resolution |

### Reference Implementation

The [de.bxservice.chatbotpoc](https://github.com/d-ruiz/de.bxservice.chatbotpoc) demonstrates:
- LangGraph StateGraph for multi-step workflows
- Query validation step before execution
- Confidence scoring for decisions

Our implementation extends this with:
- Email parsing and cleaning
- Multi-field classification (type, priority, category, sentiment)
- Auto-assignment with confidence thresholds
- Bulk processing for backlog

## Decision

Implement a **Support Ticket Auto-Classification** feature that automatically classifies incoming emails into tickets with type, priority, category, sentiment, and suggested assignee.

### Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                      Email Gateway                                    │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │  Incoming Email                                                  ││
│  │    - Subject, Body, Sender, Attachments                          ││
│  └──────────────────────────────────────────────────────────────────┘│
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      EmailParser                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Processing:                                                     │  │
│  │   - Signature detection and removal                             │  │
│  │   - Small image filtering (<5KB)                                │  │
│  │   - Entity extraction (order IDs, SKUs, dates, amounts)         │  │
│  │   - Text cleaning (whitespace, encoding)                        │  │
│  │   - Customer lookup (match sender to C_BPartner)                │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                  TicketClassificationAgent                            │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ LangChain4j AiServices:                                         │  │
│  │   - ChatLanguageModel (Claude Sonnet 4)                         │  │
│  │   - System prompt: Support classification rules                 │  │
│  │   - ERPTools: queryDatabase (historical tickets, customer)      │  │
│  │   - Classification logic with confidence scoring                │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Classification Result                              │
│  {                                                                    │
│    "summary": "Customer reports invoice discrepancy...",             │
│    "type": "SUPPORT",                                                │
│    "priority": "HIGH",                                               │
│    "category": "FINANCE",                                            │
│    "sentiment": "FRUSTRATED",                                        │
│    "confidence": 0.92,                                               │
│    "suggested_assignee_skill": "Finance Expert",                     │
│    "required_actions": ["Review invoice", "Contact customer"],       │
│    "extracted_entities": {"invoice_no": "INV-1234", "amount": "$500"}│
│  }                                                                    │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Ticket Creation Logic                              │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ if confidence > 0.85:                                           │  │
│  │     Auto-assign to suggested assignee                           │  │
│  │ elif confidence > 0.70:                                         │  │
│  │     Create ticket, mark for supervisor review                   │  │
│  │ else:                                                           │  │
│  │     Flag for manual classification                              │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      R_Request (Ticket)                               │
│  - Summary (AI-generated, max 200 chars)                             │
│  - Original email preserved as first comment                          │
│  - Type, Priority, Category auto-filled                               │
│  - AI confidence score stored                                         │
│  - Assigned based on confidence threshold                             │
└──────────────────────────────────────────────────────────────────────┘
```

### Classification Workflow (LangGraph-inspired)

```
┌────────────────┐
│  Parse Email   │
│  (EmailParser) │
└───────┬────────┘
        │
        ▼
┌────────────────┐     ┌─────────────────┐
│ Lookup Customer│────▶│ Get Historical  │
│ (C_BPartner)   │     │ Tickets         │
└───────┬────────┘     └────────┬────────┘
        │                       │
        └───────────┬───────────┘
                    ▼
        ┌────────────────────┐
        │ Classify Request   │
        │ (AI Agent)         │
        └───────┬────────────┘
                │
                ▼
        ┌────────────────────┐
        │ Calculate          │
        │ Confidence         │
        └───────┬────────────┘
                │
        ┌───────┴───────┐
        ▼               ▼
   confidence      confidence
   >= 0.85         < 0.85
        │               │
        ▼               ▼
┌──────────────┐ ┌──────────────┐
│ Auto-Assign  │ │ Flag Review  │
│ Create Ticket│ │ Create Ticket│
└──────────────┘ └──────────────┘
```

### System Prompt

```
You are a support ticket classification AI for an ERP system.

TASK: Analyze incoming email and classify as a support ticket.

INPUT:
- Email Subject: {subject}
- Email Body (cleaned): {body}
- Sender: {sender_email} ({sender_name})
- Customer Record: {customer_info} (if found)
- Recent Tickets from Customer: {recent_tickets}
- Extracted Entities: {entities}

OUTPUT FORMAT (JSON only):
{
  "summary": "One-line ticket description (max 200 chars)",
  "type": "BUG | FEATURE_REQUEST | SUPPORT | QUESTION | COMPLAINT",
  "priority": "HIGH | MEDIUM | LOW",
  "category": "PRODUCT | INVENTORY | SALES | PURCHASING | FINANCE | GENERAL",
  "sentiment": "FRUSTRATED | NEUTRAL | SATISFIED",
  "confidence": 0.0-1.0,
  "suggested_assignee_skill": "Role or expertise needed",
  "root_cause": "What is the underlying issue?",
  "required_actions": ["Action 1", "Action 2"],
  "escalation_required": true/false,
  "escalation_reason": "Reason if escalation needed"
}

CLASSIFICATION RULES:

Type Classification:
- BUG: Reports system failure, data error, unexpected behavior, crashes
- FEATURE_REQUEST: Asks for new functionality, enhancement, improvement
- SUPPORT: Requests help using existing features, how-to questions
- QUESTION: Asks how something works, general inquiry
- COMPLAINT: Expresses dissatisfaction, service issue, negative feedback

Priority Heuristics:
- HIGH: System down, data loss, VIP customer, urgent deadline, revenue impact
        frustrated sentiment + existing open ticket, legal/compliance mention
- MEDIUM: Standard request, non-critical issue, normal customer
- LOW: General inquiry, documentation request, nice-to-have

Category Mapping:
- PRODUCT: Product catalog, descriptions, SKUs, pricing display
- INVENTORY: Stock levels, warehouse, shipping, receiving
- SALES: Orders, quotes, customers, invoicing
- PURCHASING: POs, vendors, procurement, receiving
- FINANCE: Payments, accounting, reports, taxes
- GENERAL: Other, unclear, multiple categories

Sentiment Detection:
- FRUSTRATED: Words like urgent, unacceptable, disappointed, still waiting, broken
              ALL CAPS, excessive punctuation (!!!, ???)
              Multiple follow-ups, escalation language
- SATISFIED: Thank you, appreciate, works great, resolved
- NEUTRAL: Standard business communication

Confidence Scoring:
- 0.90-1.00: Clear, unambiguous classification with strong evidence
- 0.80-0.89: High confidence but minor ambiguity
- 0.70-0.79: Moderate confidence, some interpretation needed
- 0.50-0.69: Low confidence, multiple valid interpretations
- 0.00-0.49: Very uncertain, needs human review

BUSINESS RULES:
- Escalate if: VIP customer + frustrated sentiment
- Escalate if: Legal/compliance keywords (lawsuit, attorney, GDPR, audit)
- Escalate if: Data breach or security concern mentioned
- HIGH priority if: Customer mentions competitor or threat to leave
- Preserve original email exactly (for audit)

BE CONSERVATIVE:
- When in doubt, use lower confidence (err on side of human review)
- Don't assume context not explicitly stated
- Flag ambiguous cases for review
```

### Agent Definition

```java
package com.cloudempiere.ai.agent.mvp;

@Agent(
    name = "TicketClassificationAgent",
    domain = "SUPPORT",
    riskLevel = RiskLevel.LOW
)
public interface TicketClassificationAgent {

    @SystemMessage(fromResource = "prompts/ticket-classification.txt")
    TicketClassification classify(
        @UserMessage String emailContext
    );

    @SystemMessage(fromResource = "prompts/ticket-classification.txt")
    List<TicketClassification> classifyBatch(
        @UserMessage String batchEmailsContext
    );
}

public record TicketClassification(
    @Description("Brief ticket summary") String summary,
    @Description("Request type") TicketType type,
    @Description("Priority level") Priority priority,
    @Description("Business category") Category category,
    @Description("Customer sentiment") Sentiment sentiment,
    @Description("Classification confidence 0-1") double confidence,
    @Description("Skill needed for assignment") String suggested_assignee_skill,
    @Description("Root cause analysis") String root_cause,
    @Description("Required actions") List<String> required_actions,
    @Description("Needs escalation?") boolean escalation_required,
    @Description("Escalation reason") String escalation_reason
) {}

public enum TicketType {
    BUG, FEATURE_REQUEST, SUPPORT, QUESTION, COMPLAINT
}

public enum Priority {
    HIGH, MEDIUM, LOW
}

public enum Category {
    PRODUCT, INVENTORY, SALES, PURCHASING, FINANCE, GENERAL
}

public enum Sentiment {
    FRUSTRATED, NEUTRAL, SATISFIED
}
```

### Boundaries (per ADR-009)

```yaml
Agent: TicketClassificationAgent
Domain: SUPPORT
Risk Level: LOW

Data Access:
  Read Tables:
    - R_Request (existing tickets for patterns)
    - R_RequestType (ticket types)
    - R_Status (status definitions)
    - C_BPartner (customer lookup)
    - AD_User (contacts for matching)
    - R_Category (categories)

  Write Tables:
    - R_Request (create tickets)
    - R_RequestUpdate (add comments)

  Forbidden Tables:
    - C_Payment (financial)
    - AD_User credentials
    - HR_Employee (payroll)
    - Pricing/discount data

Organizational:
  - Respects AD_Role access
  - Tickets assigned to accessible groups only
  - Customer data filtered by org

Token Limits:
  Max Tokens/Request: 10,000
  Max Email Length: 10,000 chars (truncate body if longer)

Cost:
  Daily Budget: $150
  Cost per Classification: ~$0.05 (estimated)

Performance:
  Response Time: <2 seconds (p95)
  Throughput: 100 emails/minute (batch mode)
```

### Email Parser Utility

```java
public class EmailParser {

    /**
     * Parse raw email into structured format
     */
    public ParsedEmail parse(String rawEmail) {
        ParsedEmail parsed = new ParsedEmail();

        // 1. Extract headers
        parsed.setSubject(extractSubject(rawEmail));
        parsed.setSender(extractSender(rawEmail));
        parsed.setDate(extractDate(rawEmail));

        // 2. Extract body
        String body = extractBody(rawEmail);

        // 3. Remove signature
        body = removeSignature(body);

        // 4. Remove small images (inline logos)
        body = removeSmallImages(body);

        // 5. Clean whitespace and encoding
        body = cleanText(body);

        parsed.setCleanedBody(body);
        parsed.setOriginalBody(extractBody(rawEmail)); // Preserve original

        // 6. Extract entities
        parsed.setExtractedEntities(extractEntities(body));

        return parsed;
    }

    /**
     * Detect and remove email signature
     * Patterns: "Best regards", "Sincerely", "--", "Sent from", etc.
     */
    private String removeSignature(String body) {
        // Common signature patterns
        String[] patterns = {
            "(?m)^--\\s*$.*",                    // -- followed by signature
            "(?m)^Best regards,.*",
            "(?m)^Sincerely,.*",
            "(?m)^Thanks,.*",
            "(?m)^Kind regards,.*",
            "(?m)^Regards,.*",
            "(?m)^Sent from my .*",
            "(?m)^Get Outlook for .*",
            "(?m)^_+\\s*$.*",                    // _____ divider
            "(?m)^This email and any attachments.*" // Legal disclaimer
        };

        for (String pattern : patterns) {
            body = body.replaceAll(pattern, "");
        }

        return body.trim();
    }

    /**
     * Extract entities like order numbers, SKUs, dates, amounts
     */
    public ExtractedEntities extractEntities(String text) {
        ExtractedEntities entities = new ExtractedEntities();

        // Order numbers: SO-1234, PO-5678, INV-9999
        Pattern orderPattern = Pattern.compile("(SO|PO|INV|RMA)-?\\d{4,}");
        entities.setOrderNumbers(findAll(orderPattern, text));

        // SKUs: SKU-ABC123, #ABC-123
        Pattern skuPattern = Pattern.compile("(SKU|#)?[A-Z]{2,4}-?\\d{3,}");
        entities.setSkus(findAll(skuPattern, text));

        // Dates: 2025-01-15, Jan 15, 2025, 15/01/2025
        Pattern datePattern = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}|" +
            "\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},?\\s+\\d{4}|" +
            "\\d{1,2}/\\d{1,2}/\\d{4}"
        );
        entities.setDates(findAll(datePattern, text));

        // Amounts: $1,500.00, USD 1500, 1500 EUR
        Pattern amountPattern = Pattern.compile(
            "\\$[\\d,]+\\.?\\d{0,2}|" +
            "(?:USD|EUR|GBP)\\s*[\\d,]+\\.?\\d{0,2}|" +
            "[\\d,]+\\.?\\d{0,2}\\s*(?:USD|EUR|GBP)"
        );
        entities.setAmounts(findAll(amountPattern, text));

        return entities;
    }
}

public record ParsedEmail(
    String subject,
    String sender,
    String senderName,
    LocalDateTime date,
    String cleanedBody,
    String originalBody,
    ExtractedEntities extractedEntities
) {}

public record ExtractedEntities(
    List<String> orderNumbers,
    List<String> skus,
    List<String> dates,
    List<String> amounts
) {}
```

### Ticket Creator

```java
public class TicketCreator {

    private final Properties ctx;
    private final String trxName;

    /**
     * Create ticket from classification result
     */
    public MRequest createTicket(ParsedEmail email, TicketClassification classification) {
        MRequest request = new MRequest(ctx, 0, trxName);

        // Set basic fields
        request.setSummary(classification.summary());
        request.setRequestType(mapType(classification.type()));
        request.setPriority(mapPriority(classification.priority()));
        request.setCategory(mapCategory(classification.category()));

        // Link to customer if found
        Integer bPartnerId = lookupCustomer(email.sender());
        if (bPartnerId != null) {
            request.setC_BPartner_ID(bPartnerId);
        }

        // Set AI metadata
        request.set_ValueOfColumn("AIConfidence", classification.confidence());
        request.set_ValueOfColumn("AISentiment", classification.sentiment().name());
        request.set_ValueOfColumn("AIClassifiedBy", "TicketClassificationAgent");
        request.set_ValueOfColumn("AIClassifiedAt", Timestamp.valueOf(LocalDateTime.now()));

        // Determine assignment based on confidence
        if (classification.confidence() >= 0.85) {
            // Auto-assign
            Integer assigneeId = findAssignee(classification.suggested_assignee_skill());
            if (assigneeId != null) {
                request.setSalesRep_ID(assigneeId);
                request.setIsEscalated(false);
            }
        } else {
            // Flag for review
            request.set_ValueOfColumn("NeedsReview", true);
            request.setIsEscalated(classification.confidence() < 0.70);
        }

        // Handle escalation
        if (classification.escalation_required()) {
            request.setIsEscalated(true);
            request.set_ValueOfColumn("EscalationReason", classification.escalation_reason());
        }

        request.saveEx();

        // Add original email as first comment
        addEmailComment(request, email);

        // Add extracted entities as references
        addEntityReferences(request, email.extractedEntities());

        return request;
    }

    /**
     * Add original email as first request update (comment)
     */
    private void addEmailComment(MRequest request, ParsedEmail email) {
        MRequestUpdate update = new MRequestUpdate(ctx, 0, trxName);
        update.setR_Request_ID(request.getR_Request_ID());
        update.setResult(formatOriginalEmail(email));
        update.set_ValueOfColumn("IsOriginalEmail", true);
        update.saveEx();
    }

    private String formatOriginalEmail(ParsedEmail email) {
        return String.format(
            "=== Original Email ===\n" +
            "From: %s <%s>\n" +
            "Date: %s\n" +
            "Subject: %s\n\n" +
            "%s",
            email.senderName(),
            email.sender(),
            email.date(),
            email.subject(),
            email.originalBody()
        );
    }
}
```

### Batch Processing

```java
public class TicketBatchProcessor {

    private final TicketClassificationAgent agent;
    private final EmailParser parser;
    private final TicketCreator creator;

    /**
     * Process backlog of unclassified emails
     */
    public BatchResult processBatch(List<RawEmail> emails) {
        BatchResult result = new BatchResult();

        // Parse all emails
        List<ParsedEmail> parsed = emails.stream()
            .map(parser::parse)
            .collect(toList());

        // Batch classify (more efficient than one-by-one)
        String batchContext = buildBatchContext(parsed);
        List<TicketClassification> classifications = agent.classifyBatch(batchContext);

        // Create tickets
        for (int i = 0; i < parsed.size(); i++) {
            try {
                MRequest ticket = creator.createTicket(parsed.get(i), classifications.get(i));
                result.addSuccess(ticket.getR_Request_ID());
            } catch (Exception e) {
                result.addFailure(parsed.get(i).subject(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Process emails as they arrive (real-time)
     */
    public MRequest processRealTime(RawEmail email) {
        ParsedEmail parsed = parser.parse(email.content());
        String context = buildContext(parsed);
        TicketClassification classification = agent.classify(context);
        return creator.createTicket(parsed, classification);
    }
}
```

## Implementation Plan

### Phase 1: Email Processing (Days 1-2)

| Task | Effort | Owner |
|------|--------|-------|
| Create `EmailParser` with signature detection | 6h | Dev |
| Create `ExtractedEntities` and entity extraction | 4h | Dev |
| Create customer lookup logic | 2h | Dev |
| Unit tests for email parsing | 4h | Dev |

### Phase 2: Classification Agent (Days 3-4)

| Task | Effort | Owner |
|------|--------|-------|
| Create `TicketClassificationAgent` interface | 2h | Dev |
| Create classification DTOs | 2h | Dev |
| Create system prompt | 4h | Dev |
| Create tools for historical ticket lookup | 4h | Dev |
| Integration with `IDempiereAIService` | 4h | Dev |

### Phase 3: Ticket Creation (Days 5-6)

| Task | Effort | Owner |
|------|--------|-------|
| Create `TicketCreator` | 6h | Dev |
| Create assignee suggestion logic | 4h | Dev |
| Create batch processor | 4h | Dev |
| Admin configuration UI | 4h | Dev |

### Phase 4: Testing (Days 7-8)

| Task | Effort | Owner |
|------|--------|-------|
| Test with 100 real emails | 6h | QA |
| Accuracy measurement (85%+ target) | 4h | QA |
| Performance testing (<2s response) | 2h | QA |
| Support team feedback | 4h | PM |
| Prompt refinement | 4h | Dev |

### Deliverables

- `TicketClassificationAgent.java` (~100 lines)
- `TicketClassification.java` + DTOs (~120 lines)
- `EmailParser.java` (~250 lines)
- `TicketCreator.java` (~200 lines)
- `TicketBatchProcessor.java` (~100 lines)
- `ticket-classification.txt` (system prompt)
- Admin configuration window
- Unit and integration tests

**Total Effort**: 8 days
**Risk Level**: Medium (email parsing complexity, accuracy requirements)

## Acceptance Criteria

```yaml
Functional:
  - [ ] Email arrives → ticket created automatically
  - [ ] Original email preserved as first comment (unmodified)
  - [ ] Ticket populated: summary, type, priority, category, assignee
  - [ ] Low-confidence classifications flagged for review
  - [ ] Bulk processing for email backlog
  - [ ] Entity extraction (order IDs, SKUs, dates, amounts)

Non-Functional:
  - [ ] Response time: <2 seconds (p95)
  - [ ] Classification accuracy: 85%+ (precision)
  - [ ] Throughput: 100 emails/minute (batch mode)
  - [ ] Cost per classification: <$0.10

Security:
  - [ ] Original emails never modified
  - [ ] Customer data access respects role
  - [ ] All classifications logged for audit
```

## Consequences

### Positive

1. **Automation**: No manual classification needed
2. **Speed**: Immediate ticket creation vs 5-10 min delay
3. **Consistency**: Same criteria applied to all emails
4. **Routing**: Right ticket to right agent faster
5. **Audit Trail**: AI decisions logged with confidence

### Negative

1. **Accuracy Gap**: 85% means 15% need correction
2. **Edge Cases**: Unusual emails may misclassify
3. **Trust Building**: Support team needs to trust AI
4. **Email Variety**: Different formats need handling

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Misclassification | HIGH | Confidence threshold, human review |
| Missed urgency | HIGH | Escalation rules, VIP detection |
| Email parsing fails | MEDIUM | Fallback to manual, log errors |
| Customer not found | LOW | Create as unknown, flag for lookup |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Classification accuracy | 85%+ | Weekly review |
| First response time | <4 hours | Support metrics |
| Auto-assignment rate | 70%+ | Confidence > 0.85 |
| Manual corrections | <15% | Review tracking |
| Daily throughput | 200+ tickets | Usage logs |
| Cost per ticket | <$0.10 | API billing |

## References

- [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [ADR-009: Domain Boundaries](009-domain-boundaries-agent-scope.md)
- [de.bxservice.chatbotpoc](https://github.com/d-ruiz/de.bxservice.chatbotpoc) - StateGraph pattern reference

---

*ADR-019 | Version 1.0 | 2025-12-03*
