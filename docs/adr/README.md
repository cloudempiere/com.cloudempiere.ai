# Architecture Decision Records (ADRs)

This directory contains architecture decisions for the Cloudempiere AI plugin.

---

## Active ADRs

### Core Architecture

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [001](001-initial-architecture.md) | Initial Architecture and Project Standards | Accepted | 2025-12-01 |
| [002](002-langchain4j-strategic-adoption.md) | LangChain4j Strategic Adoption | Accepted | 2025-12-01 |
| [003](003-mcp-server-integration.md) | MCP Server Integration | Accepted | 2025-12-01 |
| [004](004-java-agent-framework.md) | Java Agent Framework Selection | Accepted | 2025-11-26 |
| [034](034-google-gemini-provider-integration.md) | Google Gemini Provider Integration | Proposed | 2025-12-03 |
| [035](035-java-version-strategy.md) | Java Version Strategy and Migration Path | Accepted | 2025-12-04 |
| [038](038-quarkus-satellite-ai-service.md) | Quarkus Satellite AI Service Architecture | Research | 2025-12-10 |

### Data & Intelligence

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [005](005-intelligent-data-source-routing.md) | Intelligent Data Source Routing | Superseded | 2025-12-01 |
| [006](006-data-model-architecture.md) | Data Model Architecture | Accepted | 2025-12-01 |
| [007](007-database-security-model.md) | Database Security Model | Accepted | 2025-12-01 |
| [008](008-llm-instruction-following.md) | LLM Instruction Following Strategy | Accepted | 2025-12-01 |
| [012](012-rag-based-context-retrieval.md) | RAG-Based Context Retrieval | Accepted | 2025-12-01 |
| [026](026-vector-database-strategy.md) | Vector Database Strategy | Proposed | 2025-12-03 |
| [029](029-multi-tenant-ai-access.md) | Multi-Tenant AI Access for Service Providers | Proposed | 2025-12-03 |

### Agent Architecture

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [009](009-domain-boundaries-agent-scope.md) | Domain Boundaries and Agent Scope | Accepted | 2025-12-01 |
| [010](010-agent-orchestration-architecture.md) | Agent Orchestration Architecture | Accepted | 2025-12-01 |
| [011](011-specialized-agent-scopes.md) | Specialized Agent Scopes by Business Domain | Accepted | 2025-12-01 |
| [016](016-knowledge-base-agent.md) | Knowledge Base Agent Domain | Accepted | 2025-12-02 |

### Operations & UX

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [013](013-observability-cost-tracking.md) | Observability and Cost Tracking | Accepted | 2025-12-01 |
| [014](014-guardrails-and-safety.md) | Guardrails and Safety | Accepted | 2025-12-01 |
| [015](015-conversational-ux-patterns.md) | Conversational UX Patterns | Accepted | 2025-12-01 |
| [027](027-chain-maintainability-ui-configuration.md) | Chain Maintainability and UI Configuration | Proposed | 2025-12-03 |
| [031](031-chat-panel-langchain4j-chatmodel-integration.md) | Chat Panel LangChain4j ChatModel Integration | Proposed | 2025-12-03 |
| [032](032-testing-strategy.md) | Testing Strategy | Proposed | 2025-12-03 |
| [033](033-streaming-thinking-timeline-ux.md) | Streaming Responses and Thinking Timeline UX | Proposed | 2025-12-03 |
| [037](037-language-detection-session-management.md) | Language Detection and Session Language Management | Proposed | 2025-12-10 |
| [038](038-user-friendly-error-handling.md) | User-Friendly Error Handling and Issue Tracking | Accepted | 2025-12-10 |
| [039](039-chat-panel-record-zoom-drill.md) | Chat Panel Record Zoom and Drill Integration | Implemented v0.22.0 | 2025-12-11 |

### Use Cases - Phase 1 (MVP)

| ADR | Title | Status | Priority | Date |
|-----|-------|--------|----------|------|
| [017](017-chart-executive-overview.md) | Chart Executive Overview | Accepted | P0 | 2025-12-03 |
| [018](018-sales-opportunity-summary.md) | Sales Opportunity Summary | Accepted | P0 | 2025-12-03 |
| [019](019-support-ticket-classification.md) | Support Ticket Classification | Accepted | P1 | 2025-12-03 |
| [020](020-email-gateway-enhancement.md) | Email Gateway Enhancement | Accepted | P1 | 2025-12-03 |

### Use Cases - Phase 2

| ADR | Title | Status | Priority | Date |
|-----|-------|--------|----------|------|
| [021](021-product-catalog-enhancement.md) | Product Catalog Enhancement | Accepted | P2 | 2025-12-03 |
| [022](022-translation-wizard.md) | Translation Wizard | Accepted | P2 | 2025-12-03 |
| [030](030-ecommerce-operations-automation.md) | E-Commerce Operations Automation | Proposed | P2 | 2025-12-03 |

### Use Cases - Phase 3

| ADR | Title | Status | Priority | Date |
|-----|-------|--------|----------|------|
| [023](023-ocr-invoice-processing.md) | OCR Invoice Processing | Accepted | P3 | 2025-12-03 |
| [024](024-import-data-normalization.md) | Import Data Normalization | Accepted | P3 | 2025-12-03 |
| [025](025-idempiere-development-assistant.md) | iDempiere Development Assistant | Accepted | P3 | 2025-12-03 |

---

## Superseded ADRs

| ADR | Title | Superseded By |
|-----|-------|---------------|
| [005](005-intelligent-data-source-routing.md) | Intelligent Data Source Routing | [ADR-012](012-rag-based-context-retrieval.md) |

---

## Studies & References

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [028](028-idempiere-mcp-applicability-study.md) | iDempiere-MCP (hengsin) Applicability Study | Reference | 2025-12-03 |

---

## Appendices

| Doc | Title | Related ADR |
|-----|-------|-------------|
| [002-appendix](002-appendix-feature-mapping.md) | Feature Mapping: Legacy to LangChain4j | ADR-002 |
| [014-appendix](014-appendix-langchain4j-validation.md) | LangChain4j Guardrails Validation | ADR-014 |

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

**ADR-034: Google Gemini Provider Integration**
- Google AI Gemini via `langchain4j-google-ai-gemini`
- Cost-effective alternative (95% savings vs. Claude)
- Native embeddings, code execution, and extended thinking support
- Vertex AI enterprise path planned

**ADR-035: Java Version Strategy and Migration Path**
- Critical version incompatibility identified: LangChain4j 1.0+ requires Java 17
- Decision: Downgrade to LangChain4j 0.35.0 for Java 11 MVP
- Phase 1: Java 11 / LangChain4j 0.35.0 (MVP use cases ADR-017 to ADR-020)
- Phase 2: Java 17 / LangChain4j 1.x (with iDempiere Release-11 migration)
- MCP and extended thinking blocked until Java 17 migration

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

**ADR-026: Vector Database Strategy**
- AWS RDS PostgreSQL with pgvector (Phase 1)
- Aurora pgvector, S3 Vectors alternatives analyzed
- Cost comparison: RDS pgvector vs OpenSearch Serverless vs S3 Vectors

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

**ADR-016: Knowledge Base Agent Domain**
- RAG-based documentation search
- iDempiere wiki and Application Dictionary integration
- Context-aware help system

**ADR-027: Chain Maintainability and UI Configuration**
- Hybrid approach: code structure + database configuration
- OSGi hot-deploy for chain updates without server restart
- UI-configurable prompts, parameters, and cost budgets
- A/B testing support with version management
- Three-tier update strategy (hot/OSGi/full deployment)

**ADR-031: Chat Panel LangChain4j ChatModel Integration**
- Migration from custom AIConversationService to LangChain4j
- ThreadAwareChatMemory for multi-thread conversations
- IDempiereAIService/IDempiereAgent for AI calls
- Retention of intelligent routing and caching

**ADR-032: Testing Strategy**
- JUnit 5 testing framework
- Mock provider testing patterns
- Integration test guidelines

**ADR-033: Streaming Responses and Thinking Timeline UX**
- Token-by-token streaming via LangChain4j TokenStream
- Enhanced AIStreamCallback with tool/thinking events
- AIChatStreamingMessage component for real-time rendering
- Tool timeline display ("Querying database...", "✓ Complete")
- Collapsible thinking section for extended thinking models
- ZK thread safety with Executions.schedule()

**ADR-037: Language Detection and Session Language Management**
- Automatic language detection from iDempiere user context (`AD_Language`)
- Session-level language override via natural language requests ("respond in German")
- Pattern detection for language change requests across multiple languages
- Priority order: session override > iDempiere language > fallback (en_US)
- Technical terms (table names, SQL) remain in English regardless of response language

**ADR-038: User-Friendly Error Handling and Issue Tracking**
- Never show raw technical errors to users
- Error categorization: RATE_LIMIT, TIMEOUT, CONTENT_FILTER, CONFIGURATION, CONTEXT_LENGTH, SERVICE_UNAVAILABLE, GENERIC
- AD_Message integration for translatable error messages (AIG_Error_* keys)
- AD_Issue creation with full context for support debugging
- Error reference codes (AIG-{timestamp}-{random}) link user reports to technical details
- Actionable hints guide users on what to do next

**ADR-039: Chat Panel Record Zoom and Drill Integration**
- Clickable record references in AI chat responses (SO-1234 → opens Sales Order window)
- Native iDempiere zoom integration via `AEnv.zoom()` and `MQuery`
- `RecordReference` DTO captures table/record/display metadata
- `ChatRecordLinkRenderer` renders clickable ZK components
- Zoom-across support via `WZoomAcross` for multi-target records
- LLM-provided structured references (preferred) with pattern-matching fallback
- Role-based access validation before navigation
- Drill-down support for chart segments (ADR-017 integration)

### Use Cases - Phase 1 (MVP)

**ADR-017: Chart Executive Overview**
- AI-powered chart explanations using ChartContextProvider
- Structured output with findings and methodology
- 15-minute cache TTL, <3 second response target

**ADR-018: Sales Opportunity Summary**
- Opportunity health scoring (ON_TRACK/NEEDS_ATTENTION/AT_RISK)
- Activity timeline analysis with sentiment detection
- Stakeholder mapping and engagement tracking

**ADR-019: Support Ticket Classification**
- Email parsing with signature detection
- Confidence-based routing (auto-assign >0.85, review 0.70-0.85)
- R_Request integration for ticket creation

**ADR-020: Email Gateway Enhancement**
- Signature detection with confidence scoring
- Image filtering (<5KB removal)
- Entity extraction (order numbers, SKUs, dates)

### Use Cases - Phase 2

**ADR-021: Product Catalog Enhancement**
- Multi-mode agent (GENERATE, ENHANCE, SEO_OPTIMIZE, BULK)
- Brand voice configuration
- Description quality scoring

**ADR-022: Translation Wizard**
- AD_*_Trl table integration
- Translation glossary for ERP terminology
- Back-translation quality verification

### Use Cases - Phase 3

**ADR-023: OCR Invoice Processing**
- Vision LLM (Claude 3.5) for document extraction
- 3-way match validation (PO, receipt, invoice)
- Draft C_Invoice generation

**ADR-024: Import Data Normalization**
- Pipeline architecture for name, address, phone, email
- Fuzzy duplicate detection
- Country-specific formatting (DE, AT, CH, US, GB)

**ADR-025: iDempiere Development Assistant**
- MCP server integration for external IDE access
- RAG-based wiki and documentation search
- Code templates for Model, Process, Callout generation
