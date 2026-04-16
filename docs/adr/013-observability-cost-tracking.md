# ADR-013: Observability and Cost Tracking

<!-- MADR 3.0 Template -->

## Status

Accepted — **Partially Wired**: `AIG_UsageMetrics` records written via direct `MAIUsageMetrics.record()` call in the **streaming path** of `AIService` only; `CostGuard` active in all paths; sync path has no metrics recording; `AIMetricsListener` listener wiring in `LangChain4jProviderFactory` is **dead code to be removed** (see Architecture Notes below)

## Date

2025-12-01

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

AI/LLM operations introduce unique operational challenges: unpredictable token-based costs, variable response latency (500ms-30s), non-deterministic output quality, and compliance audit requirements (GDPR, SOX). Currently, our implementation has zero observability - no token tracking, no cost monitoring, no quality metrics. Without this visibility, we cannot predict monthly AI costs, identify expensive queries, detect quality degradation, or meet compliance audit requirements.

## Decision Drivers

- **Cost Control**: Token-based pricing can lead to unexpected expenses without budget enforcement
- **Compliance**: GDPR/SOX require audit trails for AI-driven decisions
- **Performance**: Need to identify slow queries and optimize model selection
- **Capacity Planning**: Require usage patterns data for scaling decisions
- **Quality Assurance**: Must track response accuracy and user satisfaction

## Considered Options

1. **LangChain4j ChatModelListener + Custom Persistence**
2. **External Observability Platform (Datadog, Helicone)**
3. **Custom Event-Based Logging**

## Decision Outcome

**Chosen option:** "LangChain4j ChatModelListener + Custom Persistence", because it integrates natively with our existing LangChain4j architecture, keeps data within iDempiere for compliance, and provides full control over metrics without external dependencies or additional costs.

### Confirmation

- [x] `AIG_UsageMetrics` table receives records — **streaming path only**: `MAIUsageMetrics.record()` called directly at `AIService.java:1197` after each streaming response with full token counts, cost, and latency
- [ ] `AIG_UsageMetrics` records written for sync (non-streaming) path — **NOT DONE**: no `MAIUsageMetrics.record()` call in the sync agent path
- [x] `CostGuard` blocks requests when daily/monthly budget exceeded — **active in 6+ locations** in `AIService` (`checkBudget()`, `checkRateLimit()`)
- [x] `AIG_Budget` table queried by `CostGuard` (`MAIBudget.getEffective()`) — **active**
- [x] `AIG_ModelPricing` table used for cost calculation — **active** in `AIService.calculateCostMicrodollars()`
- [ ] `AIMetricsListener` registered as LangChain4j listener — **NOT DONE** (see Architecture Notes; factory wiring is dead code)
- [ ] Metrics recorded for future domain agents (Sales, KB, etc.) automatically — **NOT YET**: direct call approach requires each new agent path to add recording manually
- [ ] Unit tests for metrics recording accuracy *(partial)*
- [ ] Enhanced listener events *(blocked: requires LangChain4j 0.36+ and Java 17 — see ADR-035)*

### Architecture Notes (2026-03-11)

#### Current approach: direct call in AIService streaming path

`AIService` calls `MAIUsageMetrics.record()` directly in the streaming token handler where it has full iDempiere context (`ctx`, `AD_User_ID`, `AD_Role_ID`, provider ID, session ID). This works and produces correctly attributed records.

#### Dead code to remove: LangChain4jProviderFactory listener wiring

`LangChain4jProviderFactory` contains commented-out `/* builder.listeners(...) */` blocks and a commented-out `createMetricsListener()` method. These are dead code and should be deleted — they are misleading and serve no purpose.

**Why the factory is the wrong owner:** the factory has no iDempiere context (`ctx`, `AD_User_ID`, etc.) at model-creation time. Records written from there would have no user/role attribution.

#### Recommended future approach: AIService-level listener (Java 17 migration)

When upgrading to Java 17 + LangChain4j 0.36+ (ADR-035), register `AIMetricsListener` at **`AIService` level** when building each agent — not in the factory. At that point `AIService` has full context, and the listener fires automatically for every LLM call regardless of which agent or code path triggers it. This eliminates the need for manual `MAIUsageMetrics.record()` calls and ensures future domain agents (Sales, KB, Inventory) get metrics automatically without any additional code.

```java
// Future pattern (Java 17 + LangChain4j 0.36+):
AIMetricsListener listener = new AIMetricsListener(ctx, userId, roleId, providerId, sessionId);
ChatLanguageModel model = factory.createChatModel(provider);
// model.addListener(listener) or pass via AiServices builder
```

## Pros and Cons of the Options

### Option 1: LangChain4j ChatModelListener + Custom Persistence

Native listener implementation with iDempiere table storage.

- Good, because integrates seamlessly with existing LangChain4j architecture
- Good, because data stays within iDempiere (compliance friendly)
- Good, because no external dependencies or additional costs
- Good, because full control over metric schema and retention
- Neutral, because requires custom dashboard development
- Bad, because requires maintenance of metrics code

### Option 2: External Observability Platform (Datadog, Helicone)

Use SaaS observability platform with pre-built dashboards.

- Good, because rich pre-built dashboards and alerting
- Good, because less development effort for UI
- Good, because industry-standard integrations
- Bad, because data leaves iDempiere (compliance concern)
- Bad, because ongoing subscription cost ($500-5000/month)
- Bad, because external dependency for critical functionality

### Option 3: Custom Event-Based Logging

Log to files/message queue, process async.

- Good, because minimal impact on request latency
- Good, because flexible processing options
- Bad, because requires additional infrastructure (message queue)
- Bad, because metrics not immediately available
- Bad, because complex failure handling

## More Information

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Observability Flow                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Request → CostGuard (pre-check) → LLM + Listener → Persist │
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌───────────────┐ │
│  │  CostGuard   │───►│ ChatModel +  │───►│ AIG_Usage     │ │
│  │  - Budget    │    │ AIMetrics    │    │ Metrics       │ │
│  │  - Rate Limit│    │ Listener     │    │ (persist)     │ │
│  └──────────────┘    └──────────────┘    └───────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| `AIMetricsListener` | Capture token/cost/latency | `observability/` |
| `CostGuard` | Budget/rate enforcement | `observability/` |
| `AIG_UsageMetrics` | Metrics storage | Database |
| `AIG_UsageSummary` | Aggregated view | Database |
| `AIG_ModelPricing` | Per-model token pricing | Database |

### Key Metrics

| Metric | Purpose | Alert Threshold |
|--------|---------|-----------------|
| Daily Cost | Budget monitoring | >80% of daily budget |
| Tokens/Request | Usage patterns | >4000 tokens avg |
| Latency P95 | Performance | >5000ms |
| Error Rate | Reliability | >5% |

### Model Pricing Table (`AIG_ModelPricing`)

Token-to-cost conversion rates are stored in a dedicated table rather than hardcoded in `AIMetricsListener`. This allows pricing updates (providers change rates frequently) without code changes or redeployment.

| Column | Type | Notes |
|--------|------|-------|
| `AIG_ModelPricing_ID` | PK | |
| `AIG_Provider_ID` | FK → AIG_Provider | Optional — null means applies to any provider |
| `ModelName` | String(100) | Substring match against model name, e.g. `claude-3-5-sonnet`, `gpt-4o` |
| `InputCostPerMToken` | Decimal(20,6) | Provider cost per million **input** tokens |
| `OutputCostPerMToken` | Decimal(20,6) | Provider cost per million **output** tokens |
| `C_Currency_ID` | FK → C_Currency | Provider's billing currency (USD, EUR…) |
| `ValidFrom` | Date | Pricing effective date (latest active row wins) |
| `IsActive` | YesNo | Standard iDempiere flag |

**Lookup logic in `AIMetricsListener.calculateCost()`:**
1. Query `AIG_ModelPricing` where `ModelName` is contained in the response model name, ordered by `ValidFrom DESC`, take first active row
2. Calculate: `cost = (inputTokens / 1,000,000 × InputCostPerMToken) + (outputTokens / 1,000,000 × OutputCostPerMToken)`
3. Convert from `C_Currency_ID` to the comparison currency via iDempiere `MConversionRate` if needed
4. Cache results for 30 minutes to avoid repeated DB lookups

**Fallback:** If no matching row is found, fall back to hardcoded defaults (existing behaviour) and log a warning.

### Provider Pricing API Availability

Researched 2026-02-27. Summary of whether each provider exposes pricing data programmatically:

| Provider | Programmatic Pricing API | Notes |
|----------|--------------------------|-------|
| **Anthropic** | ❌ None | Rates published on docs/pricing page only. Must be inserted into `AIG_ModelPricing` manually. |
| **AWS Bedrock** | ✅ AWS Price List API | `aws pricing get-products --service-code "AmazonBedrock"` returns per-model `pricePerUnit.USD`. Requires `pricing:GetProducts` IAM permission. Region must be `us-east-1` (pricing API is global but only available there). |
| **OpenAI** | ❌ None | Long-standing community request, never implemented. Manual maintenance required. |
| **Ollama (local)** | N/A — always $0 | Local inference has no cost. If using Ollama Cloud (launched Sep 2025), pricing is flat subscription tiers, not per-token. |

**AWS Bedrock example query:**
```bash
aws pricing get-products \
  --service-code "AmazonBedrock" \
  --filters Type=TERM_MATCH,Field=provider,Value="Anthropic" \
            Type=TERM_MATCH,Field=feature,Value="On-demand Inference" \
  --region us-east-1
```

**Implication:** A scheduled iDempiere process could auto-sync `AIG_ModelPricing` rows for Bedrock models via the AWS Price List API. Anthropic and OpenAI rows must be maintained manually when providers publish rate changes.

**Sources (retrieved 2026-02-27):**
- [AWS Pricing API for Bedrock models (GitHub Gist)](https://gist.github.com/mikamboo/afd98fb78c3ada58b56acfd38dd342f3)
- [API for Bedrock pricing — AWS re:Post](https://repost.aws/questions/QU1IfSsHSsTbKfUtMlhvOHxw/api-for-the-pricing-of-bedrock-foundational-models)
- [OpenAI pricing API request thread](https://community.openai.com/t/is-there-an-endpoint-to-programmatically-fetch-openai-model-pricing/1229924)

### Currency Unit Conventions

**IMPORTANT:** Different tables use different currency units for precision and compatibility:

| Table | Column | Unit | Conversion | Example |
|-------|--------|------|------------|---------|
| `AIG_UsageMetrics` | `CostUSD` | **Microdollars** | ÷ 1,000,000 = USD | 121,758 = $0.121758 |
| `AIG_Budget` | `DailyLimitUSD` | **Cents** | ÷ 100 = USD | 10,000,000 = $100,000.00 |
| `AIG_Budget` | `MonthlyLimitUSD` | **Cents** | ÷ 100 = USD | 50,000,000 = $500,000.00 |
| `AIG_Budget` | `CurrentDailyUSD` | **Cents** | ÷ 100 = USD | 5,000 = $50.00 |
| `AIG_Budget` | `CurrentMonthlyUSD` | **Cents** | ÷ 100 = USD | 25,000 = $250.00 |

**Why different units?**

- **Microdollars (1M = $1)** in `AIG_UsageMetrics`: AI token costs are extremely small (e.g., $0.000003 per input token for Claude Haiku). Microdollars provide 6 decimal places of precision without floating-point issues.

- **Cents (100 = $1)** in `AIG_Budget`: Budget limits are typically whole dollar amounts ($50, $500, $10,000). Cents provide sufficient precision while being more intuitive for configuration.

**Conversion in Code:**

```java
// AIMetricsListener.getTodayCost() - converts microdollars to dollars
BigDecimal microdollars = DB.getSQLValueBD(null, sql, clientId);
return microdollars.divide(new BigDecimal("1000000"), 6, ROUND_HALF_UP);

// MAIBudget.getDailyLimitAsBigDecimal() - converts cents to dollars
return BigDecimal.valueOf(getDailyLimitUSD()).divide(BigDecimal.valueOf(100), 2, ROUND_HALF_UP);
```

**Budget Comparison Flow:**
```
AIG_UsageMetrics.CostUSD (microdollars)
        ↓ ÷ 1,000,000
     Dollars (usage)
        ↓ compare
     Dollars (limit)
        ↑ ÷ 100
AIG_Budget.DailyLimitUSD (cents)
```

### Implementation Notes

**Phase 1 (2 days):** AIG_UsageMetrics table + AIMetricsListener
**Phase 2 (2 days):** CostGuard + budget enforcement
**Phase 3 (3 days):** Dashboard views + alerts
**Phase 4 (2 days):** User-configurable budgets

**Total Effort:** 9 days

### Related ADRs

- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j provides ChatModelListener interface
- [ADR-007](007-database-security-model.md) - Security audit logging complements observability

### References

- [Datadog LLM Observability](https://www.datadoghq.com/product/llm-observability/)
- [TrueFoundry LLM Cost Tracking](https://www.truefoundry.com/blog/llm-cost-tracking-solution)
- [LangChain4j Listeners](https://docs.langchain4j.dev/tutorials/observability)
- [AI_MCP_ERP_BEST_PRACTICES.md](../AI_MCP_ERP_BEST_PRACTICES.md) Section 8
