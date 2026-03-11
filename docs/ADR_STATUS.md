# ADR & Feature Status Analysis

> Last updated: 2026-03-11 (verified against live code — wired vs. scaffolded)
> Total ADRs: 62 | Fully Wired: ~18 | Scaffolded/Partial: ~18 | Planned/Not Started: ~18 | Blocked (Java 17): 3 | Superseded/Deprecated: 2
>
> **Legend**: ✅ = class exists AND wired into live chat path | 🚧 = partial (class exists, not fully wired) | 📋 = not started | ❌ = blocked

---

## Core Architecture (001–004)

| ADR | Title | Status |
|-----|-------|--------|
| [001](adr/001-initial-architecture.md) | Initial Architecture & Standards | ✅ Implemented |
| [002](adr/002-langchain4j-strategic-adoption.md) | LangChain4j Strategic Adoption | ✅ Implemented (70%) — providers done, RAG pending |
| [003](adr/003-mcp-server-integration.md) | REST API / MCP Server Integration | ❌ Blocked — Java 17 required |
| [004](adr/004-java-agent-framework.md) | Java Agent Framework (LangChain4j AiServices) | ✅ Implemented |

---

## Data, Security & Routing (005–008)

| ADR | Title | Status |
|-----|-------|--------|
| [005](adr/005-intelligent-data-source-routing.md) | Intelligent Data Source Routing (regex-based) | ⚠️ Superseded by ADR-012 (code still active) |
| [006](adr/006-data-model-architecture.md) | Data Model (AIG_Provider, AIG_Chat, etc.) | ✅ Implemented |
| [007](adr/007-database-security-model.md) | DB Security — Dual-Identity Model | ✅ Implemented (`SecureDatabaseQueryExecutor`) |
| [008](adr/008-llm-instruction-following.md) | LLM Instruction Following (structured prompts) | ✅ Implemented |

---

## Agent Architecture (009–016)

| ADR | Title | Status |
|-----|-------|--------|
| [009](adr/009-domain-boundaries-agent-scope.md) | Domain Boundaries (5-boundary framework) | 🚧 Partial — boundary classes + domain agents exist; `OrchestratorAgent` **not wired into chat flow** |
| [010](adr/010-agent-orchestration-architecture.md) | Agent Orchestration (layered LangChain4j) | 🚧 Partial — `ERPAgent` + `ERPTools` in use; `OrchestratorAgent` prepared but bypassed |
| [011](adr/011-specialized-agent-scopes.md) | Specialized Agents (Inventory/Sales/Purchasing) | 🚧 Partial — 5 agent classes registered as OSGi services; not reachable via current chat path |
| [012](adr/012-rag-based-context-retrieval.md) | RAG-Based Context Retrieval | 🚧 Plumbing wired in AIService — **inactive until pgvector installed** (ADR-026 blocker) |
| [013](adr/013-observability-cost-tracking.md) | Observability & Cost Tracking | 🚧 Partial — `CostGuard` active; `AIMetricsListener` **not registered** (no token/cost persistence) |
| [014](adr/014-guardrails-and-safety.md) | Guardrails & Safety (3-tier risk pipeline) | 🚧 Partial — `InputGuard` + `OutputGuard` active; `ExecutionGuard` class exists but **not wired** |
| [015](adr/015-conversational-ux-patterns.md) | Conversational UX Patterns | 🚧 Partial — `ChatRecordLinkRenderer`/`ZoomLinkProcessor` classes exist but **not called**; `ParameterChainHandler`, `ProactiveInsightEngine` not implemented |
| [016](adr/016-knowledge-base-agent.md) | Knowledge Base Agent Domain | 🚧 Partial — `KbAgent`/`KbTools` exist as OSGi services; **not reachable** via current chat path |

---

## Use Cases — Phase 1 MVP (017–020)

| ADR | Title | Status |
|-----|-------|--------|
| [017](adr/017-chart-executive-overview.md) | Chart Executive Overview | 📋 Not started |
| [018](adr/018-sales-opportunity-summary.md) | Sales Opportunity Summary | 📋 Not started |
| [019](adr/019-support-ticket-classification.md) | Support Ticket Classification | 📋 Not started |
| [020](adr/020-email-gateway-enhancement.md) | Email Gateway Enhancement | 📋 Not started |

---

## Use Cases — Phase 2 & 3 (021–025)

| ADR | Title | Status |
|-----|-------|--------|
| [021](adr/021-product-catalog-enhancement.md) | Product Catalog Enhancement | 📋 Phase 2 |
| [022](adr/022-translation-wizard.md) | Translation Wizard | 📋 Phase 2 |
| [023](adr/023-ocr-invoice-processing.md) | OCR Invoice Processing | 📋 Phase 3 |
| [024](adr/024-import-data-normalization.md) | Import Data Normalization | 📋 Phase 3 |
| [025](adr/025-idempiere-development-assistant.md) | iDempiere Development Assistant | 📋 Phase 3 |

---

## Infrastructure & Multi-Tenancy (026–030)

| ADR | Title | Status |
|-----|-------|--------|
| [026](adr/026-vector-database-strategy.md) | Vector DB Strategy (pgvector) | 🚧 Schema created (`AIG_Embedding`); pgvector persistence pending (Java 17 blocker) |
| [027](adr/027-implementation-roadmap-priority.md) | Implementation Roadmap & Priority | ✅ Accepted (context-first approach) |
| [028](adr/028-mcp-applicability-study.md) | MCP Applicability Study | ⚠️ Deprecated — superseded by ADR-003 |
| [029](adr/029-multi-tenant-ai-access.md) | Multi-Tenant AI Access | 🚧 Partial — `AIG_Provider_Access` table + model exist; **access not enforced** in AIService; dual-identity DB security active |
| [030](adr/030-ecommerce-operations-automation.md) | E-Commerce Operations Automation | 📋 Planned |

---

## Chat UX, Rendering & Integration (031–041)

| ADR | Title | Status |
|-----|-------|--------|
| [031](adr/031-chat-panel-langchain4j-chatmodel-integration.md) | Chat Panel LangChain4j Integration (`AIService.java`) | ✅ Implemented |
| [032](adr/032-testing-strategy.md) | Testing Strategy | 🚧 Framework implemented (25+ tests, test bundle); CI/CD pending |
| [033](adr/033-streaming-thinking-timeline-ux.md) | Streaming + Thinking Timeline UX | 🚧 Partial — `ERPStreamingAgent` active; `StreamingMarkdownRenderer`/`StreamingTextBuffer` classes exist but **not in pipeline**; thinking blocked (Java 17) |
| [034](adr/034-google-gemini-provider-integration.md) | Google Gemini Provider | 📋 Deferred — blocked by Java 11 + LangChain4j 0.35.0 |
| [035](adr/035-java-version-strategy.md) | Java Version Strategy (11 → 17 migration) | ✅ Accepted, Phase 2 target |
| [036](adr/036-chat-ownership-and-sharing-model.md) | Chat Ownership & Sharing Model | ✅ Implemented |
| [037](adr/037-language-detection-session-management.md) | Language Detection & Session Language | ✅ Implemented (CLD-1703 fixed) |
| [038](adr/038-user-friendly-error-handling.md) | User-Friendly Error Handling | 🚧 Partial — `AIErrorHandler` class + migration scripts exist; **not called** from AIService catch blocks; no AD_Issue records created |
| [039](adr/039-chat-panel-record-zoom-drill.md) | Record Zoom & Drill Integration | ✅ Implemented — Phase 1 & 2 complete |
| [040](adr/040-embedding-ingestion-evolution.md) | Embedding Ingestion Evolution | 🚧 Ingestors implemented; pgvector persistence pending (ADR-026) |
| [041](adr/041-chain-maintainability-ui-configuration.md) | Chain Maintainability & UI Config | 📋 Planned |

---

## Provider Integrations (042–046)

| ADR | Title | Status |
|-----|-------|--------|
| [042](adr/042-ai-hub-provider-integration.md) | AI Hub Provider Integration | ✅ Implemented |
| [049](adr/049-mcp-client-external-tools-integration.md) | MCP Client for External Tools | ❌ Blocked (Java 17) |

---

## UI Components & Security (047–059)

| ADR | Title | Status |
|-----|-------|--------|
| [047](adr/047-streaming-chat-rendering-best-practices.md) | Streaming Chat Rendering (`StreamingMarkdownRenderer`) | 🚧 Partial — `StreamingMarkdownRenderer` class exists; **not in streaming pipeline** (see ADR-033) |
| [048](adr/048-comprehensive-security-strategy.md) | Comprehensive Security (OWASP LLM Top 10) | 📋 Phase 2 |
| [049](adr/049-mcp-client-external-tools-integration.md) | MCP Client for External Tools | ❌ Blocked (Java 17) — ready for iDempiere v11 |
| [050](adr/050-plugin-health-prerequisite-verification.md) | Plugin Health & Prerequisite Checks | ✅ Implemented |
| [051](adr/051-zk-ui-defensive-programming.md) | ZK UI Defensive Programming | ✅ Implemented |
| [052](adr/052-ai-chat-widget-core-decoupling.md) | AI Chat Widget Core Decoupling | ✅ Implemented (superseded by ADR-053) |
| [053](adr/053-floating-chat-bubble-zero-core-changes.md) | Floating Chat Bubble (ZK fragment injection) | 📋 Not implemented — `AIChatBubble.java`, `AIChatBubbleInjector.java`, `WEB-INF/zk.xml` do not exist |
| [054](adr/054-html-only-chat-message-storage.md) | HTML-Only Message Storage | 🚧 Accepted — infrastructure done, integration pending |
| [055](adr/055-constrained-markdown-syntax-support.md) | Constrained Markdown Sanitization | 📋 Proposed |
| [056](adr/056-html-field-length-handling.md) | HTML Field Length Handling | ✅ Implemented |
| [057](adr/057-context-aware-pii-detection.md) | Context-Aware PII Detection | ✅ Implemented |
| [058](adr/058-ai-provider-user-role-access-control.md) | Provider-Level Role Access Control | 🚧 Partial — see ADR-029; table + model exist, enforcement not wired |
| [059](adr/059-configurable-system-prompt-architecture.md) | Configurable System Prompt (DB-driven addendum, `MAIPromptConfig`) | ✅ Implemented (v0.19.0+) |

---

## Schema & SQL Intelligence (060–062)

| ADR | Title | Status |
|-----|-------|--------|
| [060](adr/060-ad-metadata-schema-cache.md) | AD Metadata Schema Cache | 🚧 **Active dev** (`ADSchemaCache.java`, `ADTableMeta.java`) |
| [061](adr/061-sql-context-enrichment.md) | SQL Context Enrichment | 📋 Planned (depends on ADR-060) |
| [062](adr/062-schema-aware-system-prompt.md) | Schema-Aware System Prompt | 📋 Planned (depends on ADR-060) |

---

## Summary

| Status | Count | ADRs |
|--------|-------|------|
| ✅ Fully Wired & Active | ~18 | 001, 002, 004, 006, 007, 008, 027, 031, 035, 036, 037, 039, 042, 050, 051, 056, 057, 059 |
| 🚧 Scaffolded / Partially Wired | ~18 | 009 (boundary classes only), 010 (ERPAgent only), 011 (agents not reachable), 012 (pgvector blocker), 013 (CostGuard only), 014 (no ExecutionGuard), 015 (link classes unwired), 016 (KbAgent not reachable), 026 (schema only), 029 (access not enforced), 032 (tests only), 033 (streaming agent only), 038 (AIErrorHandler unwired), 040 (ingestors only), 047 (renderer class unwired), 052 (superseded), 054 (infra only), 058 (access not enforced) |
| 📋 Not Started / Planned | ~16 | 017, 018, 019, 020, 021, 022, 023, 024, 025, 030, 041, 048, 053, 055, 061, 062 |
| ❌ Blocked (Java 17) | 3 | 003, 034, 049 |
| ⚠️ Superseded/Deprecated | 2 | 005 (→ ADR-012), 028 (→ ADR-003) |

---

## Critical Blockers

| Blocker | Blocks | Resolution |
|---------|--------|------------|
| **ADR-026** — pgvector not implemented | RAG (ADR-012), Embeddings (ADR-040), Knowledge Base (ADR-016) | Implement PostgreSQL pgvector extension |
| **Java 17 migration** | MCP (ADR-003, 049), Gemini streaming (ADR-034), extended thinking, system/tool caching | Upgrade to iDempiere v11 + Java 17 + LangChain4j 1.x |
| **ADR-060** — schema cache in progress | SQL enrichment (ADR-061), schema-aware prompts (ADR-062) | Complete `ADSchemaCache` on `feat/adr-060-schema-cache` branch |
