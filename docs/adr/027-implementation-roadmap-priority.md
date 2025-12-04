# ADR-027: Implementation Roadmap and Priority Matrix

**Status:** Accepted
**Date:** 2025-12-03
**Deciders:** Cloudempiere AI Team
**Context:** LangChain4j Foundation → Business Case Validation

---

## Context

### Problem Statement

We have 26 ADRs covering technical infrastructure and business use cases. Without a clear implementation priority:
- Technical foundation may be incomplete before business cases are attempted
- Business cases may fail due to missing infrastructure
- No validation path to prove technical ADRs work in practice

### Solution: Foundation-First, Business-Validated Approach

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           IMPLEMENTATION PHILOSOPHY                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   Phase 1: FOUNDATION (Technical ADRs)                                          │
│   ────────────────────────────────────                                          │
│   Build solid LangChain4j base that ALL business cases depend on                │
│                                                                                 │
│                              ↓ validates ↓                                      │
│                                                                                 │
│   Phase 2: MVP BUSINESS CASES (Business ADRs)                                   │
│   ────────────────────────────────────────────                                  │
│   Implement first business cases that PROVE technical foundation works          │
│   If business case fails → technical ADR needs revision                         │
│                                                                                 │
│                              ↓ enables ↓                                        │
│                                                                                 │
│   Phase 3: ADVANCED FEATURES (Technical + Business)                             │
│   ───────────────────────────────────────────────                               │
│   Build on proven foundation with more complex capabilities                     │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Decision

### Implementation Priority Matrix

| Priority | Type | ADR | Status | Validates/Enables |
|----------|------|-----|--------|-------------------|
| **P0** | Foundation | ADR-002 (LangChain4j) | 70% Done | ALL business cases |
| **P0** | Foundation | ADR-004 (Agent Framework) | Done | ALL agents |
| **P1** | Foundation | ADR-013 (Observability) | Not Started | Cost tracking, production readiness |
| **P1** | Foundation | ADR-014 (Guardrails) | Not Started | Production safety |
| **P2** | Foundation | ADR-012 (RAG) | Not Started | ADR-016, ADR-017, ADR-018 |
| **P2** | Foundation | ADR-026 (Vector DB) | Not Started | ADR-012 persistence |
| **P2** | Foundation | ADR-036 (Chat Ownership) | Accepted | Team collaboration, shared chats |
| **P3** | Foundation | ADR-009 (Boundaries) | 20% Done | ALL domain agents |
| **P3** | Foundation | ADR-011 (Specialized Agents) | Not Started | ADR-018, ADR-019 |
| **V1** | Business MVP | ADR-017 (Chart Overview) | Not Started | Validates P0, P1 |
| **V1** | Business MVP | ADR-018 (Sales Summary) | Not Started | Validates P0, P1, P2 |
| **V1** | Business MVP | ADR-019 (Ticket Classification) | Not Started | Validates P0, P1, P3 |
| **V2** | Business | ADR-016 (Knowledge Base) | Partial | Validates P2 (RAG) |
| **V2** | Business | ADR-020 (Email Gateway) | Not Started | Validates P3 |

**Legend:**
- **P0-P3**: Priority levels for technical foundation
- **V1-V2**: Validation phases using business cases

---

## Phase 1: LangChain4j Foundation Complete

### Goal
Complete the LangChain4j technical foundation so that ANY business case can be implemented.

### Remaining Work for ADR-002 (30% remaining)

| Component | ADR Section | Current | Target | Validates With |
|-----------|-------------|---------|--------|----------------|
| Structured Outputs | §3.1 | Missing | Records with @Description | ADR-017, ADR-018, ADR-019 |
| ChatModelListener | §4.1 | Missing | TokenUsageListener, LatencyListener | ADR-013 (Observability) |
| langchain4j-embeddings | §3.2 | Missing | RAG infrastructure | ADR-012 (RAG) |

### ADR-013: Observability (Critical - P1)

**Why Critical:** Without observability, we cannot:
- Track API costs (risk of $500+ bills)
- Monitor response latency
- Detect quality degradation
- Meet compliance audit requirements

**Components Needed:**

```java
// 1. AIMetricsListener - Capture all LLM calls
public class AIMetricsListener implements ChatModelListener {
    @Override
    public void onRequest(ChatRequest request) {
        // Log request start, track timing
    }

    @Override
    public void onResponse(ChatResponse response) {
        // Record: tokens, cost, latency, model
        // Persist to AIG_UsageMetrics table
    }
}

// 2. CostGuard - Enforce budgets
public class CostGuard {
    public void checkBudget(AgentContext ctx) throws BudgetExceededException {
        // Check daily/monthly limits before LLM call
    }
}

// 3. Database table
CREATE TABLE AIG_UsageMetrics (
    AIG_UsageMetrics_ID SERIAL PRIMARY KEY,
    AD_Client_ID INT NOT NULL,
    AD_User_ID INT NOT NULL,
    AgentName VARCHAR(100),
    ModelName VARCHAR(100),
    InputTokens INT,
    OutputTokens INT,
    CostUSD NUMERIC(10,6),
    LatencyMs INT,
    Created TIMESTAMP DEFAULT NOW()
);
```

**Validated By:** Every business case - all should log metrics.

### ADR-014: Guardrails (Critical - P1)

**Why Critical:** Without guardrails, we cannot:
- Prevent prompt injection attacks
- Detect PII in inputs/outputs
- Classify action risk levels
- Require approval for high-risk actions

**Components Needed:**

```java
// 1. InputGuard - Pre-processing
public class InputGuard {
    public GuardResult validate(String input) {
        // PII detection (SSN, CC#, email patterns)
        // Injection detection (ignore, disregard, bypass patterns)
        // Return: PASS, MASK, BLOCK
    }
}

// 2. ExecutionGuard - Action control
public class ExecutionGuard {
    public RiskLevel classifyRisk(String action, Object params) {
        // LOW: Read queries
        // MEDIUM: Create drafts
        // HIGH: Complete documents, >$1000
        // CRITICAL: Bulk operations, system config
    }
}

// 3. OutputGuard - Post-processing
public class OutputGuard {
    public GuardResult validate(String output) {
        // PII leak detection
        // Hallucination flags (low confidence)
        // Data leakage (credentials, internal data)
    }
}
```

**Validated By:** ADR-019 (Ticket Classification) - processes external email input.

---

## Phase 2: RAG Infrastructure

### ADR-012: RAG-Based Context Retrieval (P2)

**Why P2:** Required for semantic search in business cases.

**Dependencies:**
- ADR-026 (Vector DB) for production persistence
- ADR-002 §3.2 for embedding model

**Components Needed:**

```java
// 1. RAGContextManager
public class RAGContextManager {
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel embeddingModel;

    public ContentRetriever getRetriever() {
        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(embeddingModel)
            .maxResults(5)
            .minScore(0.7)
            .build();
    }
}

// 2. Agent integration
IDempiereAgent agent = AiServices.builder(IDempiereAgent.class)
    .chatLanguageModel(model)
    .contentRetriever(ragManager.getRetriever())  // <-- Automatic context injection
    .build();
```

**Validated By:**
- ADR-016 (Knowledge Base) - semantic article search
- ADR-017 (Chart Overview) - context from previous explanations
- ADR-018 (Sales Summary) - similar opportunity patterns

### ADR-026: Vector Database (P2)

**Why P2:** Production persistence for embeddings.

**Components Needed:**

```sql
-- PostgreSQL with pgvector
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE AIG_Embedding (
    AIG_Embedding_ID SERIAL PRIMARY KEY,
    AD_Client_ID INT NOT NULL,
    EmbeddingType VARCHAR(50) NOT NULL,
    ContentHash VARCHAR(64),
    Embedding vector(1536),
    Metadata JSONB,
    Created TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_embedding_hnsw
ON AIG_Embedding USING hnsw (Embedding vector_cosine_ops);
```

```xml
<!-- pom.xml dependency -->
<artifactItem>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>1.0.0-beta3</version>
</artifactItem>
```

**Validated By:** ADR-016 (Knowledge Base) - persistent article embeddings.

---

## Phase 3: Domain Agents

### ADR-009: Domain Boundaries (P3)

**Why P3:** Required for specialized agents with proper security.

**Components Needed:**

```java
// 1. BoundaryEnforcementFilter
public class BoundaryEnforcementFilter {
    public String enforceOrgFilter(String sql, AgentContext ctx) {
        return sql + " AND AD_Client_ID = " + ctx.getClientId()
                   + " AND AD_Org_ID IN (" + ctx.getOrgIds() + ")";
    }
}

// 2. DataAccessValidator
public class DataAccessValidator {
    public boolean canRead(String tableName, AgentContext ctx) {
        return ctx.getBoundary().getReadTables().contains(tableName);
    }

    public boolean canWrite(String tableName, AgentContext ctx) {
        return ctx.getBoundary().getWriteTables().contains(tableName)
            && isDocumentDraft(tableName);
    }
}

// 3. ActionBoundaryValidator
public class ActionBoundaryValidator {
    public boolean isAllowed(String action, AgentContext ctx) {
        if ("DELETE".equals(action)) return false; // NEVER
        return ctx.getBoundary().getAllowedActions().contains(action);
    }
}
```

**Validated By:** All domain-specific business cases (ADR-018 Sales, ADR-019 Support).

### ADR-011: Specialized Agent Scopes (P3)

**Why P3:** Domain agents can only be built on complete boundaries.

**Validated By:**
- ADR-018 → SalesAgent domain
- ADR-019 → Support domain (TicketClassificationAgent)
- ADR-016 → Knowledge Base domain

---

## Validation Phase 1: MVP Business Cases

### ADR-017: Chart Executive Overview (V1)

**Technical Dependencies:**

| Technical ADR | Component Needed | Criticality |
|---------------|------------------|-------------|
| ADR-002 | LangChain4jProviderFactory | Critical |
| ADR-002 | Structured Outputs (ChartExplanation record) | Critical |
| ADR-013 | AIMetricsListener | Required |
| ADR-014 | InputGuard (for follow-up questions) | Required |
| ADR-007 | SecureDatabaseQueryExecutor | Already Done |

**Validation Criteria:**

```yaml
ADR-002 Validated If:
  - [ ] ChartExplainerAgent uses AiServices.builder()
  - [ ] ChartExplanation record parsed correctly
  - [ ] Response time < 3 seconds

ADR-013 Validated If:
  - [ ] Every chart explanation logged to AIG_UsageMetrics
  - [ ] Token count and cost recorded
  - [ ] Query visible in observability dashboard

ADR-014 Validated If:
  - [ ] Follow-up questions sanitized
  - [ ] No PII leaked in explanations
  - [ ] Forbidden tables not accessed
```

### ADR-018: Sales Opportunity Summary (V1)

**Technical Dependencies:**

| Technical ADR | Component Needed | Criticality |
|---------------|------------------|-------------|
| ADR-002 | LangChain4jProviderFactory | Critical |
| ADR-002 | Structured Outputs (OpportunitySummary) | Critical |
| ADR-012 | RAG for similar opportunities | Recommended |
| ADR-013 | AIMetricsListener | Required |
| ADR-014 | OutputGuard (competitor data protection) | Required |
| ADR-011 | SalesAgent domain boundaries | Required |

**Validation Criteria:**

```yaml
ADR-002 Validated If:
  - [ ] OpportunitySummaryAgent uses AiServices
  - [ ] Complex nested records (Stakeholder, Timeline) work
  - [ ] Response time < 5 seconds

ADR-012 Validated If:
  - [ ] Similar past opportunities retrieved
  - [ ] Cache hit rate > 50%
  - [ ] Semantic matching works ("won" = "closed-won")

ADR-011 Validated If:
  - [ ] Agent only accesses Sales domain tables
  - [ ] Competitor data not exposed
  - [ ] SalesRep_ID filtering works
```

### ADR-019: Support Ticket Classification (V1)

**Technical Dependencies:**

| Technical ADR | Component Needed | Criticality |
|---------------|------------------|-------------|
| ADR-002 | LangChain4jProviderFactory | Critical |
| ADR-002 | Structured Outputs (TicketClassification) | Critical |
| ADR-013 | AIMetricsListener (high volume) | Critical |
| ADR-014 | InputGuard (external email input) | Critical |
| ADR-014 | ExecutionGuard (ticket creation) | Required |
| ADR-009 | Org boundaries (ticket assignment) | Required |

**Validation Criteria:**

```yaml
ADR-002 Validated If:
  - [ ] TicketClassificationAgent uses AiServices
  - [ ] Enum fields (TicketType, Priority) parsed correctly
  - [ ] Batch classification works

ADR-013 Validated If:
  - [ ] 100+ tickets/day tracked
  - [ ] Cost per ticket < $0.10
  - [ ] Daily budget enforcement works

ADR-014 Validated If:
  - [ ] Email injection attempts blocked
  - [ ] PII masked before AI processing
  - [ ] Confidence < 0.70 triggers manual review

ADR-009 Validated If:
  - [ ] Tickets created in correct AD_Org
  - [ ] Assignee from accessible pool only
```

---

## Implementation Roadmap

```
Week 1-2: Complete P0+P1 (Foundation)
──────────────────────────────────────
├── ADR-002: Structured Outputs, Listeners
├── ADR-013: AIMetricsListener, AIG_UsageMetrics table
└── ADR-014: InputGuard, OutputGuard, ExecutionGuard

Week 3: V1 Validation (First Business Case)
─────────────────────────────────────────────
└── ADR-017: Chart Executive Overview
    ├── Validates ADR-002 structured outputs
    ├── Validates ADR-013 observability
    └── Validates ADR-014 input sanitization

Week 4-5: P2 (RAG Infrastructure)
───────────────────────────────────
├── ADR-012: RAGContextManager
├── ADR-026: pgvector setup
└── langchain4j-embeddings, langchain4j-pgvector deps

Week 6: V1 Validation (More Business Cases)
─────────────────────────────────────────────
├── ADR-018: Sales Opportunity Summary
│   └── Validates ADR-012 RAG retrieval
└── ADR-019: Support Ticket Classification
    └── Validates ADR-014 external input handling

Week 7-8: P3 (Domain Boundaries)
─────────────────────────────────
├── ADR-009: Complete boundary enforcement
└── ADR-011: Domain agent definitions

Week 9+: V2 Validation (Advanced Cases)
────────────────────────────────────────
├── ADR-016: Knowledge Base Agent
├── ADR-020: Email Gateway Enhancement
└── ADR-021-025: Phase 2-3 use cases
```

---

## Dependency Graph

```
                         ┌──────────────────┐
                         │   ADR-002        │
                         │   LangChain4j    │
                         │   Foundation     │
                         └────────┬─────────┘
                                  │
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
         ▼                        ▼                        ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   ADR-013        │    │   ADR-014        │    │   ADR-012        │
│   Observability  │    │   Guardrails     │    │   RAG            │
│   (Metrics)      │    │   (Safety)       │    │   (Context)      │
└────────┬─────────┘    └────────┬─────────┘    └────────┬─────────┘
         │                        │                        │
         │                        │                        ▼
         │                        │              ┌──────────────────┐
         │                        │              │   ADR-026        │
         │                        │              │   Vector DB      │
         │                        │              │   (Persistence)  │
         │                        │              └────────┬─────────┘
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │   ADR-009        │
                         │   Boundaries     │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │   ADR-011        │
                         │   Domain Agents  │
                         └────────┬─────────┘
                                  │
    ┌─────────────────────────────┼─────────────────────────────┐
    │                             │                             │
    ▼                             ▼                             ▼
┌──────────┐              ┌──────────────┐              ┌──────────────┐
│ ADR-017  │              │   ADR-018    │              │   ADR-019    │
│ Chart    │              │   Sales      │              │   Ticket     │
│ Overview │              │   Summary    │              │   Classify   │
└──────────┘              └──────────────┘              └──────────────┘
    │                             │                             │
    └─────────────────────────────┼─────────────────────────────┘
                                  │
                         VALIDATES FOUNDATION
```

---

## Success Criteria

### Phase 1 Complete When:

- [ ] ADR-002: Structured outputs work with ChartExplanation record
- [ ] ADR-013: Every LLM call logged to AIG_UsageMetrics
- [ ] ADR-014: PII detection blocks sensitive input
- [ ] ADR-017: Chart Executive Overview working end-to-end

### Phase 2 Complete When:

- [ ] ADR-012: RAGContextManager returns relevant context
- [ ] ADR-026: Embeddings persist across restarts
- [ ] ADR-018: Sales Opportunity Summary with similar deals

### Phase 3 Complete When:

- [ ] ADR-009: All boundary validators implemented
- [ ] ADR-011: InventoryAgent, SalesAgent, PurchasingAgent defined
- [ ] ADR-019: Ticket Classification with domain boundaries

### Full Validation When:

```yaml
All V1 Business Cases:
  - ADR-017: Response time < 3s, accuracy > 90%
  - ADR-018: Response time < 5s, cache hit > 60%
  - ADR-019: Accuracy > 85%, cost < $0.10/ticket

Foundation Proven:
  - Total daily cost trackable and < budget
  - Zero security incidents (PII leak, injection)
  - All actions auditable
```

---

## Consequences

### Positive

1. **Clear Priority**: Team knows what to build first
2. **Validation Path**: Business cases prove technical ADRs work
3. **Risk Reduction**: Foundation complete before complex cases
4. **Measurable Progress**: Each phase has clear completion criteria

### Negative

1. **Upfront Investment**: ~4 weeks before first business value
2. **Dependency Chains**: Blocked if foundation incomplete
3. **Complexity**: Tracking multiple ADR statuses

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Foundation scope creep | HIGH | Strict P0/P1 focus, defer P2/P3 |
| Business case fails validation | MEDIUM | Iterate on technical ADR |
| Team bandwidth | MEDIUM | Parallelize where possible |

---

## References

### Technical ADRs (Foundation)
- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j Strategic Adoption
- [ADR-004](004-java-agent-framework.md) - Java Agent Framework Selection
- [ADR-009](009-domain-boundaries-agent-scope.md) - Domain Boundaries
- [ADR-011](011-specialized-agent-scopes.md) - Specialized Agent Scopes
- [ADR-012](012-rag-based-context-retrieval.md) - RAG-Based Context Retrieval
- [ADR-013](013-observability-cost-tracking.md) - Observability and Cost Tracking
- [ADR-014](014-guardrails-and-safety.md) - Guardrails and Safety
- [ADR-026](026-vector-database-strategy.md) - Vector Database Strategy
- [ADR-036](036-chat-ownership-and-sharing-model.md) - Chat Ownership and Sharing Model

### Business Case ADRs (Validation)
- [ADR-016](016-knowledge-base-agent.md) - Knowledge Base Agent
- [ADR-017](017-chart-executive-overview.md) - Chart Executive Overview
- [ADR-018](018-sales-opportunity-summary.md) - Sales Opportunity Summary
- [ADR-019](019-support-ticket-classification.md) - Support Ticket Classification
- [ADR-020](020-email-gateway-enhancement.md) - Email Gateway Enhancement

---

**ADR-027 | Version 1.0 | 2025-12-03**
**Status: Accepted**
**Philosophy: Foundation First, Business Validated**
