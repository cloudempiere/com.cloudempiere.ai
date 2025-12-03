# ADR-018: Sales Opportunity Summary Use Case

**Status**: Proposed
**Date**: 2025-12-03
**Deciders**: Cloudempiere AI Team
**Related**: [ADR-002](002-langchain4j-strategic-adoption.md), [ADR-009](009-domain-boundaries-agent-scope.md), [ADR-011](011-specialized-agent-scopes.md), [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)

## Context

### Problem Statement

Sales representatives struggle with complex opportunities:
- Opportunities with 20+ activities are overwhelming to review
- 15-20 minutes spent reading history before each call
- Risk of missing critical information buried in notes
- No automated health assessment or next-action recommendations
- Stakeholder sentiment not tracked systematically

### Business Value

| Metric | Current | Target | Impact |
|--------|---------|--------|--------|
| Opportunity review time | 15-20 min | 3-5 min | 75% reduction |
| Missed follow-ups | Frequent | Rare | Higher close rate |
| Deal health visibility | Manual | Automated | Proactive intervention |
| Sales rep productivity | Baseline | +20% | Revenue impact |

### Reference Implementation

The [de.bxservice.chatbotpoc](https://github.com/d-ruiz/de.bxservice.chatbotpoc) uses views for data abstraction:
- `v_order_info`, `v_orderline_info`, `v_bpartner_info`
- LangChain SQL agent queries these views

Our implementation extends this with:
- Opportunity-specific views/queries
- Activity aggregation and sentiment analysis
- Health score calculation
- Timeline visualization data

## Decision

Implement a **Sales Opportunity Summary** feature that provides instant AI-generated summaries of opportunities with health assessment, stakeholder analysis, and next actions.

### Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                    C_Opportunity Window                               │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │  [AI Summary] Button                                             ││
│  │     → Opens AI Panel with opportunity analysis                   ││
│  └──────────────────────────────────────────────────────────────────┘│
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│               OpportunitySummaryContextProvider                       │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Extracts:                                                       │  │
│  │   - Opportunity header (name, amount, stage, expected close)   │  │
│  │   - All activities (C_ContactActivity) - emails, calls, notes  │  │
│  │   - Documents and attachments (titles, summaries)              │  │
│  │   - Status change history                                       │  │
│  │   - Business partner data (C_BPartner, contacts)               │  │
│  │   - Related quotes/orders if any                                │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                  OpportunitySummaryAgent                              │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ LangChain4j AiServices:                                         │  │
│  │   - ChatLanguageModel (Claude Sonnet 4)                         │  │
│  │   - System prompt: Sales intelligence analysis                  │  │
│  │   - ERPTools: queryDatabase, lookupRecord, searchRecords        │  │
│  │   - ActivityAnalyzer: sentiment, timeline extraction            │  │
│  │   - MessageWindowChatMemory (20 messages)                       │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      Response (JSON)                                  │
│  {                                                                    │
│    "health_score": "ON_TRACK",                                       │
│    "health_emoji": "🟢",                                              │
│    "reasoning": "Active engagement, decision maker positive...",     │
│    "key_stakeholders": [...],                                        │
│    "critical_actions": [...],                                        │
│    "risk_factors": [...],                                            │
│    "timeline": [...],                                                │
│    "next_milestone": "Demo scheduled for 2025-01-15"                 │
│  }                                                                    │
└──────────────────────────────────────────────────────────────────────┘
```

### Data Model

```sql
-- Views for AI consumption (simplified, secure access)

CREATE VIEW v_opportunity_summary AS
SELECT
    o.C_Opportunity_ID,
    o.Name,
    o.Description,
    o.OpportunityAmt,
    o.ExpectedCloseDate,
    o.Probability,
    ss.Name AS SalesStage,
    bp.Name AS CustomerName,
    bp.C_BPartner_ID,
    o.SalesRep_ID,
    u.Name AS SalesRepName,
    o.Created,
    o.Updated,
    (SELECT COUNT(*) FROM C_ContactActivity ca
     WHERE ca.C_Opportunity_ID = o.C_Opportunity_ID) AS ActivityCount,
    (SELECT MAX(ca.StartDate) FROM C_ContactActivity ca
     WHERE ca.C_Opportunity_ID = o.C_Opportunity_ID) AS LastActivityDate
FROM C_Opportunity o
JOIN C_BPartner bp ON o.C_BPartner_ID = bp.C_BPartner_ID
LEFT JOIN C_SalesStage ss ON o.C_SalesStage_ID = ss.C_SalesStage_ID
LEFT JOIN AD_User u ON o.SalesRep_ID = u.AD_User_ID;

CREATE VIEW v_opportunity_activities AS
SELECT
    ca.C_ContactActivity_ID,
    ca.C_Opportunity_ID,
    ca.StartDate,
    ca.EndDate,
    ca.Description,
    ca.Comments,
    cat.Name AS ActivityType,
    u.Name AS ContactName,
    ca.IsComplete
FROM C_ContactActivity ca
LEFT JOIN R_ContactActivityType cat ON ca.R_ContactActivityType_ID = cat.R_ContactActivityType_ID
LEFT JOIN AD_User u ON ca.AD_User_ID = u.AD_User_ID
ORDER BY ca.StartDate DESC;
```

### System Prompt

```
You are a sales intelligence assistant analyzing opportunities in an ERP system.

CONTEXT:
- Opportunity: {opportunity_name}
- Amount: {amount}
- Stage: {sales_stage}
- Expected Close: {expected_close}
- Customer: {customer_name}
- Activities: {activity_count} total
- Last Activity: {last_activity_date}

ACTIVITIES (chronological):
{activities_json}

TASK: Analyze all activity data and provide a comprehensive health assessment.

OUTPUT FORMAT (JSON only):
{
  "health_score": "ON_TRACK | NEEDS_ATTENTION | AT_RISK",
  "health_emoji": "🟢 | 🟡 | 🔴",
  "reasoning": "1-2 sentence explanation of health score",
  "key_stakeholders": [
    {
      "name": "Contact name",
      "role": "Decision Maker | Influencer | User | Champion | Blocker",
      "last_interaction": "YYYY-MM-DD",
      "sentiment": "positive | neutral | negative",
      "notes": "Key insight about this person"
    }
  ],
  "critical_actions": [
    "Most urgent action with deadline",
    "Second priority action",
    "Third priority action"
  ],
  "risk_factors": [
    "Risk 1 with specific evidence",
    "Risk 2 with specific evidence"
  ],
  "timeline": [
    {"date": "YYYY-MM-DD", "event": "Key milestone or event"},
    {"date": "YYYY-MM-DD", "event": "Another milestone"}
  ],
  "next_milestone": "Description of next expected milestone with date",
  "win_probability_assessment": "Your assessment vs stated probability with reasoning"
}

HEALTH SCORE CRITERIA:
- ON_TRACK (🟢): Recent activity (<7 days), positive stakeholder sentiment, clear next steps, no blockers
- NEEDS_ATTENTION (🟡): Activity gap (7-14 days), mixed sentiment, some unclear items, minor concerns
- AT_RISK (🔴): No activity (>14 days), negative sentiment, blockers identified, deadline pressure

RULES:
- Cite specific dates and activities as evidence
- Identify decision makers vs influencers
- Flag any stakeholder with negative sentiment
- Prioritize actions by urgency
- Compare stated probability to your assessment
- Be direct about risks - don't sugarcoat
```

### Agent Definition

```java
package com.cloudempiere.ai.agent.mvp;

@Agent(
    name = "OpportunitySummaryAgent",
    domain = "SALES",
    riskLevel = RiskLevel.LOW
)
public interface OpportunitySummaryAgent {

    @SystemMessage(fromResource = "prompts/opportunity-summary.txt")
    OpportunitySummary summarize(
        @MemoryId String sessionId,
        @UserMessage String opportunityContext
    );

    @SystemMessage(fromResource = "prompts/opportunity-summary.txt")
    String chat(
        @MemoryId String sessionId,
        @UserMessage String followUpQuestion
    );
}

public record OpportunitySummary(
    @Description("Health assessment") HealthScore health_score,
    @Description("Visual indicator") String health_emoji,
    @Description("Reasoning for health score") String reasoning,
    @Description("Key stakeholders with sentiment") List<Stakeholder> key_stakeholders,
    @Description("Prioritized next actions") List<String> critical_actions,
    @Description("Identified risks") List<String> risk_factors,
    @Description("Key timeline events") List<TimelineEvent> timeline,
    @Description("Next expected milestone") String next_milestone,
    @Description("AI probability assessment") String win_probability_assessment
) {}

public enum HealthScore {
    ON_TRACK, NEEDS_ATTENTION, AT_RISK
}

public record Stakeholder(
    String name,
    String role,
    String last_interaction,
    String sentiment,
    String notes
) {}

public record TimelineEvent(
    String date,
    String event
) {}
```

### Boundaries (per ADR-009, ADR-011)

```yaml
Agent: OpportunitySummaryAgent
Domain: SALES
Risk Level: LOW

Data Access:
  Read Tables:
    - C_Opportunity (opportunities)
    - C_ContactActivity (activities)
    - C_BPartner (customers - assigned only)
    - C_BPartner_Location (addresses)
    - AD_User (contacts - names only)
    - C_SalesStage (stage definitions)
    - C_Order (related quotes)
    - R_ContactActivityType (activity types)

  Write Tables: None (read-only analysis)

  Forbidden Tables:
    - C_Payment (financial)
    - C_Invoice (pricing)
    - HR_Employee (payroll)
    - AD_User credentials
    - Competitor information
    - Internal pricing strategies

Organizational:
  - Respects AD_Role access
  - Filters by SalesRep_ID or accessible customers
  - User sees only assigned opportunities

Token Limits:
  Max Tokens/Request: 15,000 (complex analysis)
  Max Activities: 100 (chunk if more)

Cost:
  Daily Budget: $100
  Cost per Query: ~$0.15 (estimated)

Cache:
  TTL: 1 hour
  Invalidation: On new activity added
  Key: opportunity:{C_Opportunity_ID}:{lastActivityDate}

Performance:
  Response Time: <5 seconds (p95)
  Cache Hit Rate Target: >60%
```

### Tools Available

```java
public class OpportunitySummaryTools {

    @Tool("Query opportunity activities for analysis")
    public String queryActivities(
        @P("Opportunity ID") Integer opportunityId,
        @P("Maximum activities to return") Integer limit
    ) {
        // Returns activities sorted by date DESC
        // Filtered by role access
    }

    @Tool("Get stakeholder details from contacts")
    public String getStakeholders(
        @P("Business Partner ID") Integer bPartnerId
    ) {
        // Returns AD_User contacts for the customer
        // Includes titles, roles, contact info
    }

    @Tool("Query related documents and attachments")
    public String getDocuments(
        @P("Opportunity ID") Integer opportunityId
    ) {
        // Returns document titles and summaries
    }

    @Tool("Get opportunity stage history")
    public String getStageHistory(
        @P("Opportunity ID") Integer opportunityId
    ) {
        // Returns stage transitions with dates
    }

    @Tool("Search for related orders or quotes")
    public String getRelatedOrders(
        @P("Opportunity ID") Integer opportunityId
    ) {
        // Returns C_Order records linked to opportunity
    }
}
```

### Activity Analyzer Utility

```java
public class ActivityAnalyzer {

    /**
     * Calculate health score based on activity patterns
     */
    public HealthScore calculateHealthScore(List<Activity> activities, Opportunity opp) {
        // Factors:
        // 1. Days since last activity
        // 2. Activity frequency trend
        // 3. Activity types (calls vs emails)
        // 4. Stage vs days in stage
        // 5. Expected close proximity

        int daysSinceLastActivity = getDaysSinceLastActivity(activities);
        double frequencyTrend = calculateFrequencyTrend(activities);
        boolean hasRecentCall = hasRecentCall(activities, 14);

        if (daysSinceLastActivity > 14 || frequencyTrend < 0.5) {
            return HealthScore.AT_RISK;
        } else if (daysSinceLastActivity > 7 || !hasRecentCall) {
            return HealthScore.NEEDS_ATTENTION;
        }
        return HealthScore.ON_TRACK;
    }

    /**
     * Extract sentiment from activity text
     */
    public String analyzeSentiment(String activityText) {
        // Simple keyword-based sentiment
        // Future: Use LLM for nuanced analysis

        String lower = activityText.toLowerCase();
        if (containsAny(lower, POSITIVE_KEYWORDS)) return "positive";
        if (containsAny(lower, NEGATIVE_KEYWORDS)) return "negative";
        return "neutral";
    }

    /**
     * Generate timeline from activities
     */
    public List<TimelineEvent> generateTimeline(List<Activity> activities) {
        return activities.stream()
            .filter(a -> isSignificantEvent(a))
            .map(a -> new TimelineEvent(
                formatDate(a.getStartDate()),
                summarizeActivity(a)
            ))
            .limit(10)
            .collect(toList());
    }

    private static final String[] POSITIVE_KEYWORDS = {
        "excited", "interested", "moving forward", "approved", "agreed",
        "great meeting", "positive", "enthusiastic", "committed"
    };

    private static final String[] NEGATIVE_KEYWORDS = {
        "concerned", "delayed", "budget cut", "competitor", "not interested",
        "postponed", "cancelled", "blocked", "issue", "problem"
    };
}
```

### UI Integration

```java
// On C_Opportunity window - AI Summary button handler
public void onAISummaryClick(Event event) {
    int opportunityId = getOpportunityId();

    // 1. Check cache
    String cacheKey = buildCacheKey(opportunityId, getLastActivityDate());
    OpportunitySummary cached = cache.get(cacheKey);
    if (cached != null) {
        displaySummary(cached);
        return;
    }

    // 2. Extract context
    OpportunitySummaryContextProvider provider = new OpportunitySummaryContextProvider();
    JSONObject context = provider.extractContext(ctx, windowNo,
        new ContextParameters().setOpportunityId(opportunityId));

    // 3. Pre-calculate health score (hybrid approach)
    ActivityAnalyzer analyzer = new ActivityAnalyzer();
    HealthScore preCalcHealth = analyzer.calculateHealthScore(
        getActivities(opportunityId),
        getOpportunity(opportunityId)
    );

    // 4. Call agent
    OpportunitySummaryAgent agent = AiServices.builder(OpportunitySummaryAgent.class)
        .chatLanguageModel(LangChain4jProviderFactory.create(provider))
        .tools(new OpportunitySummaryTools(executor))
        .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
        .build();

    OpportunitySummary summary = agent.summarize(sessionId, context.toString());

    // 5. Cache and display
    cache.put(cacheKey, summary, Duration.ofHours(1));
    displaySummary(summary);
}

private void displaySummary(OpportunitySummary summary) {
    // Display health badge
    healthBadge.setValue(summary.health_emoji() + " " + summary.health_score());
    healthBadge.setClass(getHealthClass(summary.health_score()));

    // Display reasoning
    reasoningLabel.setValue(summary.reasoning());

    // Display stakeholders as cards
    stakeholderGrid.setModel(new ListModelList<>(summary.key_stakeholders()));

    // Display actions as checklist
    actionsList.setModel(new ListModelList<>(summary.critical_actions()));

    // Display risks
    risksList.setModel(new ListModelList<>(summary.risk_factors()));

    // Display timeline
    timelineComponent.setModel(summary.timeline());
}
```

## Implementation Plan

### Phase 1: Context & Data Layer (Days 1-2)

| Task | Effort | Owner |
|------|--------|-------|
| Create SQL views (`v_opportunity_summary`, `v_opportunity_activities`) | 4h | DBA |
| Create `OpportunitySummaryContextProvider` | 6h | Dev |
| Create `ActivityAnalyzer` utility | 4h | Dev |
| Unit tests for context extraction | 2h | Dev |

### Phase 2: Agent Implementation (Days 3-4)

| Task | Effort | Owner |
|------|--------|-------|
| Create `OpportunitySummaryAgent` interface | 2h | Dev |
| Create DTOs (`OpportunitySummary`, `Stakeholder`, etc.) | 2h | Dev |
| Create system prompt | 4h | Dev |
| Create `OpportunitySummaryTools` | 4h | Dev |
| Integration with `IDempiereAIService` | 4h | Dev |

### Phase 3: UI & Testing (Days 5-7)

| Task | Effort | Owner |
|------|--------|-------|
| Add "AI Summary" button to C_Opportunity window | 4h | Dev |
| Create summary display panel | 6h | Dev |
| Caching implementation | 2h | Dev |
| Integration tests (20 real opportunities) | 6h | QA |
| Sales team feedback session | 4h | PM |
| Prompt refinement based on feedback | 4h | Dev |

### Deliverables

- `OpportunitySummaryAgent.java` (~100 lines)
- `OpportunitySummary.java` + related DTOs (~80 lines)
- `OpportunitySummaryContextProvider.java` (~200 lines)
- `OpportunitySummaryTools.java` (~150 lines)
- `ActivityAnalyzer.java` (~200 lines)
- `opportunity-summary.txt` (system prompt)
- SQL views for AI consumption
- ZK UI components for display
- Unit and integration tests

**Total Effort**: 7 days
**Risk Level**: Medium (activity analysis complexity)

## Acceptance Criteria

```yaml
Functional:
  - [ ] "AI Summary" button on opportunity window
  - [ ] Summary includes: health score, stakeholders, actions, risks
  - [ ] Timeline visualization of key events
  - [ ] Sentiment analysis per stakeholder
  - [ ] Chat interface for follow-up questions
  - [ ] Works for opportunities with 1-100+ activities

Non-Functional:
  - [ ] Response time: <5 seconds (p95)
  - [ ] Cache hit rate: >60%
  - [ ] Cost per query: <$0.20
  - [ ] Summary accuracy: 85%+ (sales team validation)

Security:
  - [ ] Only shows accessible opportunities
  - [ ] Respects SalesRep_ID assignment
  - [ ] No competitor data exposed
  - [ ] All queries logged
```

## Consequences

### Positive

1. **Time Savings**: 75% reduction in opportunity review time
2. **Proactive Alerts**: At-risk deals identified early
3. **Stakeholder Visibility**: Know who matters and their sentiment
4. **Consistent Analysis**: Same criteria applied to all deals
5. **Follow-up Tracking**: Never miss critical next steps

### Negative

1. **Complexity**: Activity analysis requires careful tuning
2. **Sentiment Accuracy**: Keyword-based may miss nuance
3. **Large Opportunities**: May need chunking for 100+ activities
4. **Trust Factor**: Sales reps must trust AI assessment

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Incorrect health score | HIGH | Combine AI + rule-based analysis |
| Missing critical info | MEDIUM | Chunk activities, don't truncate |
| Sales rep pushback | MEDIUM | Involve in testing, iterate on prompts |
| Slow response | LOW | Cache aggressively, pre-compute |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Response time | <5 seconds | CloudWatch |
| Summary accuracy | 85%+ | Sales team review |
| Time savings | 75% reduction | User survey |
| User satisfaction | 4.0/5 | NPS |
| Daily cost | <$100 | API billing |
| Adoption rate | 80% of sales team | Usage analytics |

## References

- [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [ADR-009: Domain Boundaries](009-domain-boundaries-agent-scope.md)
- [ADR-011: Specialized Agent Scopes](011-specialized-agent-scopes.md) - SalesAgent
- [de.bxservice.chatbotpoc](https://github.com/d-ruiz/de.bxservice.chatbotpoc) - Reference implementation

---

*ADR-018 | Version 1.0 | 2025-12-03*
