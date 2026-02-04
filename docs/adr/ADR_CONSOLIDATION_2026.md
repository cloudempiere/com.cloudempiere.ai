# ADR Consolidation and Current Architecture State (2026-01-30)

**Status:** Master Reference Document
**Purpose:** Consolidate 55+ ADRs into actionable current state
**Supersedes:** Individual reading of all ADRs
**Target Audience:** New developers, architects, stakeholders

---

## Executive Summary

This document provides a consolidated view of architectural decisions across **55 ADRs**. It identifies:
- ✅ **Implemented ADRs** - Currently in production/staging
- 🚧 **In Progress ADRs** - Active development
- 📋 **Planned ADRs** - Validated design, pending implementation
- ⚠️ **Superseded ADRs** - Replaced by newer decisions
- ❌ **Deprecated ADRs** - No longer relevant

---

## Current Architecture State (CLD-1704 Branch)

### Phase 0: Streaming Chat Rendering (✅ COMPLETE - 2026-01-30)

**Recent Work:** CLD-1704 streaming markdown rendering with emphasis support

| ADR | Title | Status | Current State |
|-----|-------|--------|---------------|
| [ADR-047](docs/adr/047-streaming-chat-rendering-best-practices.md) | Streaming Chat Rendering Best Practices | ✅ Implemented | Phase 2.5 complete - full emphasis support |
| [ADR-054](docs/adr/054-html-only-chat-message-storage.md) | HTML-Only Chat Message Storage | 🚧 In Progress | Implementation planned Week 1-2 |
| [ADR-055](docs/adr/055-constrained-markdown-syntax-support.md) | Constrained Markdown Syntax Support | 📋 Proposed | Security sanitization layer |

**Implementation Files:**
- ✅ `StreamingMarkdownRenderer.java` - Progressive markdown → HTML with state machine
- ✅ `AIChatStreamingMessage.java` - Streaming message component with chunk queue
- ✅ `AIChatWidget.java` - ZK UI integration with server-side rendering
- ✅ `SecuritySanitizer.java` - HTML/JS/URL escaping
- ✅ `MarkdownValidator.java` - Structure validation
- ✅ `ChunkCleaner.java` - Invalid character removal
- 🚧 `MarkdownSyntaxSanitizer.java` - To be implemented (ADR-055)

**Key Decisions:**
- ✅ Server-side rendering only (no client-side markdown.js) - ADR-033, ADR-047
- ✅ Character-by-character streaming for immediate UX
- ✅ HTML storage for performance (ADR-054)
- ✅ Zoom links via JavaScript + ZK events - ADR-039
- ✅ Instance-based config (multi-tenancy safe)

---

## Phase 1: Foundation (70% Complete)

### Core Framework

| ADR | Title | Status | Implementation |
|-----|-------|--------|----------------|
| [ADR-001](docs/adr/001-initial-architecture.md) | Initial Architecture | ✅ Implemented | Provider pattern, factory, DTOs |
| [ADR-002](docs/adr/002-langchain4j-strategic-adoption.md) | LangChain4j Strategic Adoption | 🚧 70% Done | Providers done, agents partial, RAG pending |
| [ADR-004](docs/adr/004-java-agent-framework.md) | Java Agent Framework Selection | ✅ Implemented | AiServices.builder() pattern |
| [ADR-035](docs/adr/035-java-version-strategy.md) | Java Version Strategy | ✅ Accepted | Java 11 + LangChain4j 0.35.0 |

**Current Limitations (Java 11 Constraint):**
- ❌ MCP support blocked until Java 17 (ADR-003)
- ❌ Extended thinking blocked (ADR-033)
- ❌ System/tool caching blocked (cost optimization)
- ❌ Google Gemini streaming blocked (ADR-034)

**Migration Path:** Java 17 + LangChain4j 1.x with iDempiere v11 (Q2 2026)

### Data Architecture

| ADR | Title | Status | Implementation |
|-----|-------|--------|----------------|
| [ADR-006](docs/adr/006-data-model-architecture.md) | Data Model Architecture | ✅ Implemented | CM_Chat, CM_ChatEntry, AIG_Provider tables |
| [ADR-007](docs/adr/007-database-security-model.md) | Database Security Model | ✅ Implemented | SecureDatabaseQueryExecutor with role checks |
| [ADR-008](docs/adr/008-llm-instruction-following.md) | LLM Instruction Following | ✅ Implemented | System prompts with role context |

### Provider Architecture

| ADR | Title | Status | Providers |
|-----|-------|--------|-----------|
| ADR-002 | Multi-Provider Support | ✅ Implemented | Anthropic ✅, Bedrock ✅, Ollama ✅, OpenAI ✅ |
| [ADR-034](docs/adr/034-google-gemini-provider-integration.md) | Google Gemini Integration | 📋 Planned | Sync only until Java 17 |
| [ADR-042](docs/adr/042-ai-hub-provider-integration.md) | AI Hub Integration | 📋 Planned | Future provider |

**Implementation Status:**
- ✅ `AnthropicProvider.java` - Full streaming + tools
- ✅ `BedrockStreamingChatModelWrapper.java` - Custom OSGi wrapper (workaround for ServiceLoader)
- ✅ `OllamaProvider.java` - Via LangChain4j (streaming disabled in 0.35.0)
- ✅ `OpenAIProvider.java` - Via LangChain4j
- ✅ `AIProviderFactory.java` - OSGi service with dynamic loading

---

## Phase 2: Context Layer (🔴 CRITICAL - Not Started)

**Priority Shift (2026-01-10 - ADR-027 v1.1):** Context is foundational, moved to P1

| ADR | Title | Status | Blocker |
|-----|-------|--------|---------|
| [ADR-026](docs/adr/026-vector-database-strategy.md) | Vector Database Strategy | 📋 Not Started | P1 - Required for RAG |
| [ADR-012](docs/adr/012-rag-based-context-retrieval.md) | RAG-Based Context Retrieval | 📋 Not Started | P1 - Depends on ADR-026 |
| [ADR-040](docs/adr/040-embedding-ingestion-evolution.md) | Embedding Ingestion Evolution | 📋 Not Started | P1 - Depends on ADR-012 |
| [ADR-016](docs/adr/016-knowledge-base-agent.md) | Knowledge Base Agent | 📋 Partial | P1 - Needs vector storage |

**Why Critical:**
Without context layer, AI responses are generic. Example:
- ❌ "Tell me about business partner BP-001" → Generic response
- ✅ With RAG → "BP-001 is Acme Corp, $500K YTD revenue, last order 2025-12-15"

**Implementation Needed:**
```sql
-- PostgreSQL with pgvector
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE AIG_Embedding (
    AIG_Embedding_ID SERIAL PRIMARY KEY,
    AD_Client_ID INT NOT NULL,
    EmbeddingType VARCHAR(50) NOT NULL,  -- 'KNOWLEDGE', 'ENTITY', 'CONVERSATION'
    ContentHash VARCHAR(64),
    Embedding vector(1536),
    Metadata JSONB,
    Created TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_embedding_hnsw
ON AIG_Embedding USING hnsw (Embedding vector_cosine_ops);
```

**Reference Architecture:** idempiere-cli (Quarkus + LangChain4j + pgvector) - See ADR-027 §"Reference Implementation"

---

## Phase 3: Safety Layer (P2 - Deferred)

| ADR | Title | Status | Notes |
|-----|-------|--------|-------|
| [ADR-013](docs/adr/013-observability-cost-tracking.md) | Observability & Cost Tracking | 📋 Not Started | Custom metrics until Java 17 |
| [ADR-014](docs/adr/014-guardrails-and-safety.md) | Guardrails and Safety | 📋 Not Started | InputGuard, OutputGuard, ExecutionGuard |
| [ADR-048](docs/adr/048-comprehensive-security-strategy.md) | Comprehensive Security Strategy | 📋 Not Started | OWASP LLM Top 10 alignment |

**Note:** Deferred to P2 because:
- Security basics covered by existing `SecuritySanitizer`
- Observability can be added later without breaking changes
- Context layer (P1) is more critical for functionality

---

## Phase 4: Domain Agents (P3 - Not Started)

| ADR | Title | Status | Dependencies |
|-----|-------|--------|--------------|
| [ADR-009](docs/adr/009-domain-boundaries-agent-scope.md) | Domain Boundaries | 📋 20% Done | Needs safety layer (ADR-014) |
| [ADR-010](docs/adr/010-agent-orchestration-architecture.md) | Agent Orchestration | 📋 Not Started | Depends on ADR-009 |
| [ADR-011](docs/adr/011-specialized-agent-scopes.md) | Specialized Agent Scopes | 📋 Not Started | Depends on ADR-009 |

**Planned Domain Agents:**
- 📋 SalesAgent - ADR-018 validation
- 📋 InventoryAgent - ADR-009 scope definition
- 📋 PurchasingAgent - ADR-009 scope definition
- 📋 SupportAgent - ADR-019 validation
- 📋 KnowledgeBaseAgent - ADR-016

---

## Use Case Validation (V1-V3)

### Phase 1 MVP (V1 - Not Started)

| ADR | Title | Status | Technical Deps |
|-----|-------|--------|----------------|
| [ADR-017](docs/adr/017-chart-executive-overview.md) | Chart Executive Overview | 📋 Not Started | ADR-002, ADR-013, ADR-014 |
| [ADR-018](docs/adr/018-sales-opportunity-summary.md) | Sales Opportunity Summary | 📋 Not Started | ADR-002, ADR-012, ADR-011 |
| [ADR-019](docs/adr/019-support-ticket-classification.md) | Support Ticket Classification | 📋 Not Started | ADR-002, ADR-014, ADR-011 |
| [ADR-020](docs/adr/020-email-gateway-enhancement.md) | Email Gateway Enhancement | 📋 Not Started | ADR-011, ADR-014 |

### Phase 2 (V2 - Planned)

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-021](docs/adr/021-product-catalog-enhancement.md) | Product Catalog Enhancement | 📋 Planned |
| [ADR-022](docs/adr/022-translation-wizard.md) | Translation Wizard | 📋 Planned |

### Phase 3 (V3 - Planned)

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-023](docs/adr/023-ocr-invoice-processing.md) | OCR Invoice Processing | 📋 Planned |
| [ADR-024](docs/adr/024-import-data-normalization.md) | Import Data Normalization | 📋 Planned |
| [ADR-025](docs/adr/025-idempiere-development-assistant.md) | iDempiere Development Assistant | 📋 Planned |

---

## Operations & UX

### Chat UX

| ADR | Title | Status | Implementation |
|-----|-------|--------|----------------|
| [ADR-031](docs/adr/031-chat-panel-langchain4j-chatmodel-integration.md) | Chat Panel LangChain4j Integration | ✅ Implemented | `AIService.java` |
| [ADR-033](docs/adr/033-streaming-thinking-timeline-ux.md) | Streaming Responses | ✅ Partial | Basic streaming done, thinking timeline blocked |
| [ADR-036](docs/adr/036-chat-ownership-and-sharing-model.md) | Chat Ownership | ✅ Accepted | CM_Chat.AD_User_ID, IsShared |
| [ADR-037](docs/adr/037-language-detection-session-management.md) | Language Detection | ✅ Implemented | Bug fix completed (CLD-1703) |
| [ADR-038](docs/adr/038-user-friendly-error-handling.md) | User-Friendly Error Handling | 📋 Planned | Linear issue tracking integration |
| [ADR-039](docs/adr/039-chat-panel-record-zoom-drill.md) | Record Zoom Integration | ✅ Implemented | ZoomLinkProcessor.java |

### UI Architecture

| ADR | Title | Status | Notes |
|-----|-------|--------|-------|
| [ADR-051](docs/adr/051-zk-ui-defensive-programming.md) | ZK UI Defensive Programming | ✅ Implemented | Thread safety, null checks |
| [ADR-052](docs/adr/052-ai-chat-widget-core-decoupling.md) | AI Chat Widget Core Decoupling | ✅ Implemented | Plugin-based, no core changes |
| [ADR-053](docs/adr/053-floating-chat-bubble-zero-core-changes.md) | Floating Chat Bubble | ✅ Implemented | ZK fragment injection |

### Testing & Quality

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-032](docs/adr/032-testing-strategy.md) | Testing Strategy | 📋 Planned |
| [ADR-041](docs/adr/041-chain-maintainability-ui-configuration.md) | Chain Maintainability | 📋 Planned |

---

## Advanced Features (Blocked by Java 17)

| ADR | Title | Status | Blocker |
|-----|-------|--------|---------|
| [ADR-003](docs/adr/003-mcp-server-integration.md) | MCP Server Integration | ❌ Blocked | LangChain4j 1.x requires Java 17 |
| [ADR-049](docs/adr/049-mcp-client-external-tools-integration.md) | MCP Client Integration | ❌ Blocked | Same as ADR-003 |
| ADR-033 §Extended Thinking | Claude Thinking Timeline | ❌ Blocked | LangChain4j 1.x feature |

**Unlock Date:** Q2 2026 with iDempiere v11 migration

---

## Deprecated and Superseded ADRs

### Superseded by Later Decisions

| ADR | Title | Superseded By | Reason |
|-----|-------|---------------|--------|
| [ADR-005](docs/adr/005-intelligent-data-source-routing.md) | Intelligent Data Source Routing | ADR-012 | RAG-based approach replaced routing logic |
| ADR-054 (pre-implementation) | Dual Storage (Markdown + HTML) | ADR-054 (final) | Changed to HTML-only storage |

### Deprecated (No Longer Applicable)

| ADR | Title | Status | Reason |
|-----|-------|--------|--------|
| [ADR-028](docs/adr/028-idempiere-mcp-applicability-study.md) | iDempiere MCP Applicability | ⚠️ Deprecated | Blocked by Java 11, superseded by ADR-003 |

### Appendix ADRs (Reference Only)

| ADR | Title | Purpose |
|-----|-------|---------|
| [ADR-002 Appendix](docs/adr/002-appendix-feature-mapping.md) | LangChain4j Feature Mapping | Reference for LangChain4j capabilities |
| [ADR-014 Appendix](docs/adr/014-appendix-langchain4j-validation.md) | LangChain4j Validation | Technical validation appendix |

---

## Special Topics

### Multi-Tenancy

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-029](docs/adr/029-multi-tenant-ai-access.md) | Multi-Tenant AI Access | ✅ Implemented |

**Implementation:** AD_Client_ID filtering in all queries, provider-level tenant isolation

### E-Commerce Integration

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-030](docs/adr/030-ecommerce-operations-automation.md) | E-Commerce Operations Automation | 📋 Planned |

### Plugin Health

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-050](docs/adr/050-plugin-health-prerequisite-verification.md) | Plugin Health & Prerequisite Verification | ✅ Implemented |

---

## Implementation Roadmap Summary

Based on [ADR-027](docs/adr/027-implementation-roadmap-priority.md) v1.1 (Context-First approach):

### Immediate Priority (Next 4 Weeks)

```
Week 1-2: Context Layer Foundation (P1)
─────────────────────────────────────────
✅ DONE: Streaming rendering (CLD-1704)
🚧 IN PROGRESS: HTML storage (ADR-054)
📋 NEXT: Vector DB setup (ADR-026)
📋 NEXT: RAG implementation (ADR-012)
📋 NEXT: Knowledge Base ingestion (ADR-016)

Week 3-4: First Business Case (V1)
────────────────────────────────────
📋 ADR-017: Chart Executive Overview
    └── Validates: ADR-002 (LangChain4j), ADR-012 (RAG)
```

### Phase 2 (Weeks 5-8)

```
Week 5-6: Safety Layer (P2)
────────────────────────────
📋 ADR-013: Observability (metrics, cost tracking)
📋 ADR-014: Guardrails (InputGuard, OutputGuard)
📋 ADR-055: Markdown sanitization

Week 7-8: More Business Cases (V1)
────────────────────────────────────
📋 ADR-018: Sales Opportunity Summary
📋 ADR-019: Support Ticket Classification
```

### Phase 3 (Weeks 9-16)

```
Week 9-12: Domain Agents (P3)
──────────────────────────────
📋 ADR-009: Domain boundaries complete
📋 ADR-011: Specialized agent definitions
📋 Domain agents: Sales, Inventory, Purchasing

Week 13-16: Advanced Use Cases (V2-V3)
────────────────────────────────────────
📋 ADR-020-025: Phase 2-3 business cases
```

---

## Critical Path Dependencies

```
CURRENT STATE (2026-01-30)
└── ✅ Streaming rendering complete (CLD-1704)
    └── 🚧 HTML storage (ADR-054) - Week 1
        └── 📋 Vector DB (ADR-026) - Week 1-2 ◄── BLOCKER for context
            └── 📋 RAG (ADR-012) - Week 2-3
                └── 📋 Knowledge Base (ADR-016) - Week 3
                    └── 📋 First business case (ADR-017) - Week 4
                        └── 📋 Observability (ADR-013) - Week 5
                            └── 📋 Guardrails (ADR-014) - Week 6
                                └── 📋 Domain boundaries (ADR-009) - Week 9
                                    └── 📋 Domain agents (ADR-011) - Week 12
```

**Critical Blocker:** Vector DB (ADR-026) must be implemented before meaningful business cases can work.

---

## Java 11 → Java 17 Migration Tracking

**Current Constraint:** Java 11 + LangChain4j 0.35.0

**Blocked Features:** (See [ADR-035](docs/adr/035-java-version-strategy.md) for details)

| Feature | Requires | External Reference |
|---------|----------|-------------------|
| MCP Support | LangChain4j 1.0+ | [LangChain4j MCP Docs](https://docs.langchain4j.dev/integrations/mcp/) |
| Extended Thinking | LangChain4j 1.0+ | [Anthropic Extended Thinking](https://docs.anthropic.com/en/docs/build-with-claude/extended-thinking) |
| System Message Caching | LangChain4j 0.36+ | [Anthropic Prompt Caching](https://docs.anthropic.com/en/docs/build-with-claude/prompt-caching) |
| Tool Result Caching | LangChain4j 0.36+ | Same as above |
| Google Gemini Streaming | LangChain4j 0.36+ | [LangChain4j Gemini Integration](https://docs.langchain4j.dev/integrations/language-models/google-gemini) |
| Enhanced Observability | LangChain4j 0.36+ | [LangChain4j Observability](https://docs.langchain4j.dev/tutorials/observability) |
| Latest Security Patches | LangChain4j 1.x | [LangChain4j Releases](https://github.com/langchain4j/langchain4j/releases) |

**Migration Checklist:** See ADR-035 §"Phase 2: Java 17 Migration"

---

## Document Maintenance

### When to Update This Document

1. **ADR Status Changes:**
   - Move ADR from "📋 Planned" → "🚧 In Progress" → "✅ Implemented"
   - Mark ADR as "⚠️ Superseded" or "❌ Deprecated"

2. **Phase Completion:**
   - Update roadmap when phase milestones reached
   - Update critical path when blockers resolved

3. **Major Architectural Shifts:**
   - Context-first priority change (already incorporated)
   - OSGi plugin architecture decisions (pending)
   - Java version migrations

### Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-30 | Initial consolidation of 55 ADRs |

---

## Quick Reference: ADR Status Legend

- ✅ **Implemented** - In production/staging code
- 🚧 **In Progress** - Active development
- 📋 **Planned** - Design approved, implementation pending
- ⚠️ **Superseded** - Replaced by newer ADR
- ❌ **Deprecated** - No longer applicable
- 🔴 **Blocked** - Waiting on dependency (Java 17, etc.)

---

## Related Documents

- [ADR-027: Implementation Roadmap](docs/adr/027-implementation-roadmap-priority.md) - Detailed implementation plan
- [ADR-035: Java Version Strategy](docs/adr/035-java-version-strategy.md) - Java 11 constraints and migration path
- [LAYERED_ARCHITECTURE_DESIGN.md](LAYERED_ARCHITECTURE_DESIGN.md) - Five-layer facade architecture
- [LANGCHAIN4J_STRATEGIC_ANALYSIS.md](LANGCHAIN4J_STRATEGIC_ANALYSIS.md) - LangChain4j gap analysis
- [GREENFIELD_IMPLEMENTATION_PLAN.md](GREENFIELD_IMPLEMENTATION_PLAN.md) - Streaming rendering simplification

---

**Document Owner:** Development Team
**Last Updated:** 2026-01-30
**Next Review:** After each phase completion or major ADR change
