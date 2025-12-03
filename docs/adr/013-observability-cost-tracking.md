# ADR-013: Observability and Cost Tracking

<!-- MADR 3.0 Template -->

## Status

Accepted

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

The decision will be confirmed when:
- [ ] `AIG_UsageMetrics` table exists and receives records for every LLM call
- [ ] `AIMetricsListener` logs all token usage, cost, and latency metrics
- [ ] `CostGuard` blocks requests when daily/monthly budget exceeded
- [ ] Unit tests verify metrics recording accuracy
- [ ] Query: `SELECT COUNT(*) FROM AIG_UsageMetrics WHERE Created > CURRENT_DATE - 1` returns expected count

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

### Key Metrics

| Metric | Purpose | Alert Threshold |
|--------|---------|-----------------|
| Daily Cost | Budget monitoring | >80% of daily budget |
| Tokens/Request | Usage patterns | >4000 tokens avg |
| Latency P95 | Performance | >5000ms |
| Error Rate | Reliability | >5% |

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
