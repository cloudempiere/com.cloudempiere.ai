# Architecture Decision Records (ADRs)

This directory contains architecture decisions for the CloudEmpiere AI plugin.

---

## Active ADRs

### Core Architecture

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [001](001-initial-architecture.md) | Initial Architecture and Project Standards | Accepted | 2025-12-01 |
| [002](002-langchain4j-strategic-adoption.md) | LangChain4j Strategic Adoption | Accepted | 2025-12-01 |
| [003](003-mcp-server-integration.md) | MCP Server Integration | Accepted | 2025-12-01 |
| [004](004-java-agent-framework.md) | Java Agent Framework Selection | Accepted | 2025-11-26 |

### Data & Intelligence

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [005](005-intelligent-data-source-routing.md) | Intelligent Data Source Routing | Superseded | 2025-12-01 |
| [006](006-data-model-architecture.md) | Data Model Architecture | Accepted | 2025-12-01 |
| [007](007-database-security-model.md) | Database Security Model | Accepted | 2025-12-01 |
| [008](008-llm-instruction-following.md) | LLM Instruction Following Strategy | Accepted | 2025-12-01 |
| [012](012-rag-based-context-retrieval.md) | RAG-Based Context Retrieval | Accepted | 2025-12-01 |

### Agent Architecture

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [009](009-domain-boundaries-agent-scope.md) | Domain Boundaries and Agent Scope | Accepted | 2025-12-01 |
| [010](010-agent-orchestration-architecture.md) | Agent Orchestration Architecture | Accepted | 2025-12-01 |
| [011](011-specialized-agent-scopes.md) | Specialized Agent Scopes by Business Domain | Accepted | 2025-12-01 |

### Operations & UX

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [013](013-observability-cost-tracking.md) | Observability and Cost Tracking | Accepted | 2025-12-01 |
| [014](014-guardrails-and-safety.md) | Guardrails and Safety | Accepted | 2025-12-01 |
| [015](015-conversational-ux-patterns.md) | Conversational UX Patterns | Accepted | 2025-12-01 |

---

## Superseded ADRs

| ADR | Title | Superseded By |
|-----|-------|---------------|
| [005](005-intelligent-data-source-routing.md) | Intelligent Data Source Routing | [ADR-012](012-rag-based-context-retrieval.md) |

---

## Appendices

| Doc | Title | Related ADR |
|-----|-------|-------------|
| [002-appendix](002-appendix-feature-mapping.md) | Feature Mapping: Legacy to LangChain4j | ADR-002 |

---

## ADR Process

We follow the [MADR 3.0](https://adr.github.io/madr/) (Markdown Any Decision Records) format.

1. **Propose**: Use [000-template.md](000-template.md) to draft new ADR
2. **Discuss**: Review with team
3. **Decide**: Update status to Accepted/Rejected
4. **Implement**: Link to implementation commits
5. **Update Index**: Add to this README

---

## ADR Format (MADR 3.0)

Each ADR includes:
- **Status**: Proposed, Accepted, Deprecated, Superseded
- **Date**: Decision date (YYYY-MM-DD)
- **Deciders**: People involved in the decision
- **Context and Problem Statement**: Problem description (required)
- **Decision Drivers**: Key factors influencing the decision
- **Considered Options**: Options evaluated (required)
- **Decision Outcome**: What was decided and why (required)
- **Confirmation**: How to verify implementation
- **Pros and Cons**: Analysis of each option
- **More Information**: Implementation details, diagrams, references

---

## Key Decisions Summary

### Core Architecture

**ADR-001: Initial Architecture**
- Plugin structure and conventions
- OSGi bundle architecture
- Maven/Tycho build system

**ADR-002: LangChain4j Strategic Adoption**
- Migration from custom provider to LangChain4j
- Multi-provider abstraction layer
- Deprecation path for legacy APIs

**ADR-003: MCP Server Integration**
- Model Context Protocol for external tools
- HTTP API + Node.js MCP server hybrid
- Security and authentication model

**ADR-004: Java Agent Framework Selection**
- LangChain4j chosen over Spring AI and Google ADK
- Tool system with @Tool annotations
- Agent orchestration patterns

### Data & Intelligence

**ADR-006: Data Model Architecture**
- AIG_* table naming convention
- Tenant isolation via AD_Client_ID
- Embedding storage for RAG

**ADR-007: Database Security Model**
- Dual-identity model (AI User + Requesting User's Role)
- MRole.addAccessSQL() enforcement
- Audit trail for all AI queries

**ADR-008: LLM Instruction Following**
- System prompt templates
- Structured output schemas
- Error handling guidelines

**ADR-012: RAG-Based Context Retrieval**
- LangChain4j ContentRetriever
- pgvector for embedding storage
- Hybrid search (vector + keyword)

### Agent Architecture

**ADR-009: Domain Boundaries and Agent Scope**
- Domain isolation (Sales, Purchasing, Inventory, Finance)
- Cross-domain orchestration patterns
- Data access boundaries per domain

**ADR-010: Agent Orchestration Architecture**
- Sequential and parallel agent patterns
- Supervisor agent for complex tasks
- State management with LangGraph-style persistence

**ADR-011: Specialized Agent Scopes**
- InventoryAgent, SalesAgent, PurchasingAgent, FinanceAgent
- Domain-specific tools and knowledge
- Permission boundaries per agent type

### Operations & UX

**ADR-013: Observability and Cost Tracking**
- AIMetricsListener for token/cost/latency tracking
- CostGuard for budget enforcement
- AIG_UsageMetrics table for persistence

**ADR-014: Guardrails and Safety**
- InputGuard (PII, prompt injection)
- ExecutionGuard (risk classification)
- OutputGuard (hallucination, data leakage)
- Human-in-the-loop approval workflows

**ADR-015: Conversational UX Patterns**
- Parameter Chain pattern for flexible input
- Structured response formatting
- Proactive insights (warnings, suggestions)
- iDempiere zoom link integration
