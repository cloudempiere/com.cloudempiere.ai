# ADR-017: Chart Executive Overview Use Case

**Status**: Proposed
**Date**: 2025-12-03
**Deciders**: CloudEmpiere AI Team
**Related**: [ADR-002](002-langchain4j-strategic-adoption.md), [ADR-009](009-domain-boundaries-agent-scope.md), [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)

## Context

### Problem Statement

Dashboard charts in iDempiere require domain expertise to interpret:
- Executives must consult analysts to understand chart meaning
- Data interpretation overhead costs ~8 hours/week per user
- No instant insights available when viewing charts
- Users leave workflow to seek explanations

### Business Value

| Metric | Current | Target | Impact |
|--------|---------|--------|--------|
| Chart interpretation time | 5-10 min | <1 min | 80% reduction |
| Analyst consultation | Frequent | Rare | Cost savings |
| User satisfaction | N/A | 4.5/5 | High adoption |

### Reference Implementation

The [de.bxservice.chatbotpoc](https://github.com/d-ruiz/de.bxservice.chatbotpoc) demonstrates a minimalistic pattern:
- ZK Form with chat UI (`ChatBotForm.java`)
- Python subprocess for AI (`ChatbotUtils.java`)
- LangChain SQL agent for database queries

Our implementation improves on this with:
- Native Java/LangChain4j (no subprocess)
- Role-based security integration
- Context-aware chart analysis
- Caching for performance

## Decision

Implement a **Chart Executive Overview** feature that provides instant plain-language explanations of any iDempiere dashboard chart.

### Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                        ZK Dashboard                                   │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │  MChart (Bar/Line/Pie/Combo)                                     ││
│  │     [Click] → Opens AI Panel                                     ││
│  └──────────────────────────────────────────────────────────────────┘│
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    ChartContextProvider                               │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Extracts:                                                       │  │
│  │   - Chart metadata (AD_Chart_ID, name, type, period)           │  │
│  │   - SQL query from MChart                                       │  │
│  │   - Query results (up to 100 rows)                              │  │
│  │   - Applied filters (date range, org, etc.)                     │  │
│  │   - JSON chart model (series, axes, colors)                     │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                     ChartExplainerAgent                               │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ LangChain4j AiServices:                                         │  │
│  │   - ChatLanguageModel (Claude Sonnet 4)                         │  │
│  │   - System prompt: Executive explanation style                  │  │
│  │   - ERPTools: queryDatabase (for follow-up questions)           │  │
│  │   - MessageWindowChatMemory (10 messages)                       │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      Response (JSON)                                  │
│  {                                                                    │
│    "summary": "Revenue is down 12% vs last quarter due to...",       │
│    "key_findings": ["Finding 1", "Finding 2", "Finding 3"],          │
│    "suggested_questions": ["Why is Q4 lower?", "Top products?"],     │
│    "methodology": "Data aggregated by month from C_Invoice..."       │
│  }                                                                    │
└──────────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
User clicks chart
    │
    ▼
1. ZK Event Handler
   - Extract AD_Chart_ID
   - Get user context (AD_Role_ID, AD_Org_ID)
    │
    ▼
2. Check Cache (15-minute TTL)
   - Key: chart:{id}:{role}:{dataHash}
   - Cache hit? → Return cached response
    │
    ▼
3. ChartContextProvider.extractContext()
   - Load MChart record
   - Execute SQL query (with role access)
   - Build context JSON
    │
    ▼
4. ChartExplainerAgent.explain(context)
   - Inject system prompt
   - Call LangChain4j AiServices
   - Parse structured output
    │
    ▼
5. Cache & Return
   - Store in cache
   - Display in AI panel
   - Enable follow-up chat
```

### System Prompt

```
You are an AI assistant explaining business charts to executives.

CONTEXT:
- Chart Name: {chart_name}
- Chart Type: {chart_type}
- Period: {period}
- Data Source: {sql_query}
- Results: {data_rows}

TASK: Analyze the chart data and provide a clear, non-technical explanation.

OUTPUT FORMAT (JSON only):
{
  "summary": "3-5 sentence executive summary explaining what the chart shows and the key takeaway",
  "key_findings": [
    "Most important insight from the data",
    "Second most important insight",
    "Third insight or notable trend"
  ],
  "suggested_questions": [
    "Natural follow-up question 1",
    "Natural follow-up question 2",
    "Natural follow-up question 3"
  ],
  "methodology": "Brief note on how data was calculated (1 sentence)"
}

RULES:
- Use plain language, no jargon or technical terms
- Focus on business implications, not raw numbers
- Highlight trends, anomalies, and actionable insights
- Compare to prior periods if data available
- Be concise and direct
```

### Agent Definition

```java
package com.cloudempiere.ai.agent.mvp;

@Agent(
    name = "ChartExplainerAgent",
    domain = "EXECUTIVE_REPORTING",
    riskLevel = RiskLevel.LOW
)
public interface ChartExplainerAgent {

    @SystemMessage(fromResource = "prompts/chart-explainer.txt")
    ChartExplanation explain(
        @MemoryId String sessionId,
        @UserMessage String chartContext
    );
}

public record ChartExplanation(
    @Description("Executive summary of the chart") String summary,
    @Description("Top 3 key findings") List<String> key_findings,
    @Description("Suggested follow-up questions") List<String> suggested_questions,
    @Description("Brief methodology note") String methodology
) {}
```

### Boundaries (per ADR-009)

```yaml
Agent: ChartExplainerAgent
Domain: EXECUTIVE_REPORTING
Risk Level: LOW

Data Access:
  Read Tables:
    - AD_Chart (chart definitions)
    - AD_ChartData (chart data source)
    - Any table referenced by chart SQL (via role access)
  Write Tables: None (read-only)
  Forbidden Tables:
    - HR_Employee (payroll)
    - AD_User (credentials)
    - C_Payment (bank details)

Organizational:
  - Respects AD_Role access
  - Filters by AD_Org_ID automatically
  - User sees only accessible data

Token Limits:
  Max Tokens/Request: 8,000
  Max Data Rows: 100 (truncate if more)

Cost:
  Daily Budget: $50
  Cost per Query: ~$0.05 (estimated)

Cache:
  TTL: 15 minutes
  Key: chart:{AD_Chart_ID}:{AD_Role_ID}:{dataHash}
  Invalidation: On chart data change

Performance:
  Response Time: <3 seconds (p95)
  Cache Hit Rate Target: >70%
```

### Tools Available

```java
public class ChartExplainerTools {

    @Tool("Execute additional SQL query for follow-up questions")
    public String queryDatabase(
        @P("SQL SELECT query") String sql,
        @P("Maximum rows") Integer maxRows,
        @P("Query purpose") String purpose
    ) {
        // Delegates to SecureDatabaseQueryExecutor
        // Enforces role-based access
        // Logs to AIG_QueryAudit
    }

    @Tool("Get chart metadata from Application Dictionary")
    public String getChartMetadata(
        @P("Chart ID") Integer chartId
    ) {
        // Returns chart name, type, description, SQL
    }
}
```

### UI Integration

```java
// In ZK chart component onClick handler
public void onChartClick(Event event) {
    int chartId = getChartId();

    // 1. Extract context
    ChartContextProvider provider = new ChartContextProvider();
    JSONObject context = provider.extractContext(ctx, windowNo,
        new ContextParameters().setChartId(chartId));

    // 2. Check cache
    String cacheKey = buildCacheKey(chartId, roleId, context.hashCode());
    ChartExplanation cached = cache.get(cacheKey);
    if (cached != null) {
        displayExplanation(cached);
        return;
    }

    // 3. Call agent
    ChartExplainerAgent agent = AiServices.builder(ChartExplainerAgent.class)
        .chatLanguageModel(LangChain4jProviderFactory.create(provider))
        .tools(new ChartExplainerTools(executor))
        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
        .build();

    ChartExplanation explanation = agent.explain(sessionId, context.toString());

    // 4. Cache and display
    cache.put(cacheKey, explanation, Duration.ofMinutes(15));
    displayExplanation(explanation);
}
```

## Implementation Plan

### Phase 1: Foundation (Days 1-2)

| Task | Effort | Owner |
|------|--------|-------|
| Create `ChartExplainerAgent` interface | 2h | Dev |
| Create `ChartExplanation` record | 1h | Dev |
| Create system prompt file | 2h | Dev |
| Update `ChartContextProvider` for full extraction | 4h | Dev |

### Phase 2: Integration (Days 3-4)

| Task | Effort | Owner |
|------|--------|-------|
| Create `ChartExplainerTools` | 4h | Dev |
| Integrate with `IDempiereAIService` | 4h | Dev |
| Add caching layer | 4h | Dev |
| Update `AIChatWidget` for chart context | 4h | Dev |

### Phase 3: Testing (Day 5)

| Task | Effort | Owner |
|------|--------|-------|
| Unit tests for agent | 4h | Dev |
| Integration tests (5 chart types) | 4h | QA |
| Performance testing (<3s response) | 2h | QA |
| User acceptance testing | 2h | PM |

### Deliverables

- `ChartExplainerAgent.java` (~80 lines)
- `ChartExplanation.java` (~20 lines)
- `ChartExplainerTools.java` (~100 lines)
- `chart-explainer.txt` (system prompt)
- Updated `ChartContextProvider.java`
- Updated `AIChatWidget.java`
- Unit and integration tests

**Total Effort**: 5 days
**Risk Level**: Low (all infrastructure exists)

## Acceptance Criteria

```yaml
Functional:
  - [ ] Click any chart → AI panel opens in <2 seconds
  - [ ] Explanation includes: summary, key findings (3), methodology
  - [ ] Chat interface allows follow-up questions
  - [ ] Works for all chart types (bar, line, pie, combo)
  - [ ] Respects role-based data access

Non-Functional:
  - [ ] Response time: <3 seconds (p95)
  - [ ] Cache hit rate: >70%
  - [ ] Cost per query: <$0.10
  - [ ] Explanation accuracy: 90%+ (human validation)

Security:
  - [ ] Cannot access forbidden tables
  - [ ] Respects AD_Org_ID filtering
  - [ ] All queries logged to AIG_QueryAudit
```

## Consequences

### Positive

1. **Instant Value**: Executives understand charts immediately
2. **Low Risk**: Read-only, no data modification
3. **High Visibility**: First AI feature users will see
4. **Reusable Pattern**: Template for other use cases
5. **Cache Efficiency**: Reduces API costs

### Negative

1. **Prompt Engineering**: May require iteration for quality
2. **Chart Variety**: Different chart types need different prompts
3. **Data Volume**: Large datasets may need summarization

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Incorrect interpretation | MEDIUM | Human validation, confidence scores |
| Slow response | LOW | Aggressive caching |
| High API cost | LOW | Cache 70%+, token limits |
| Data leakage | LOW | Role-based access enforced |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Response time | <3 seconds | CloudWatch |
| Cache hit rate | >70% | Metrics |
| User satisfaction | 4.5/5 | Survey |
| Explanation accuracy | 90%+ | Human review |
| Daily cost | <$50 | API billing |

## References

- [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [ADR-009: Domain Boundaries](009-domain-boundaries-agent-scope.md)
- [de.bxservice.chatbotpoc](https://github.com/d-ruiz/de.bxservice.chatbotpoc) - Reference implementation

---

*ADR-017 | Version 1.0 | 2025-12-03*
