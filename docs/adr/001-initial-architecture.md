# ADR-001: Initial Architecture and Project Standards

**Status:** Accepted
**Date:** 2025-12-01
**Context:** Establishing project standards for AI plugin development

## Context

The `com.cloudempiere.ai` project is an iDempiere ERP plugin integrating AI capabilities. As the project grows with multiple providers, agents, and security layers, we need consistent standards for development, documentation, and release management.

### Problem Statement

Without established standards:
- Documentation becomes inconsistent
- Version tracking is difficult
- Release processes are error-prone
- Architectural decisions are lost

### Constraints

- Must integrate with iDempiere OSGi plugin architecture
- Must follow Maven/Tycho build conventions
- Must support multi-developer collaboration
- Must maintain audit trail for enterprise use

## Decision

We adopt the following standards from the cloudempiere-cli project:

### 1. Conventional Commits

All commits follow [Conventional Commits](https://conventionalcommits.org/):
- `feat:` new feature
- `fix:` bug fix
- `docs:` documentation
- `refactor:` code refactor
- `chore:` maintenance
- `test:` adding tests
- `perf:` performance improvement

### 2. Documentation Files

| File | Purpose |
|------|---------|
| `PROJECT.md` | Project overview and quick start |
| `CLAUDE.md` | AI assistant instructions |
| `CHANGELOG.md` | Version history (Keep a Changelog) |
| `FEATURES.md` | Feature matrix and status |
| `docs/adr/*.md` | Architecture Decision Records |

### 3. Version Numbering

Semantic Versioning (SemVer):
- **Major**: Breaking changes
- **Minor**: New features, backward compatible
- **Patch**: Bug fixes

### 4. Release Workflow

1. Update version in `pom.xml`, `MANIFEST.MF`
2. Update `CHANGELOG.md` with release date
3. Commit and create tag: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`
4. Bump to next development version

### 5. Architecture Decision Records

ADRs document significant decisions:
- Numbered sequentially (001, 002, ...)
- Stored in `docs/adr/`
- Include context, decision, consequences

## Implementation

### Phase 1: Initial Setup (v0.1.0)

1. Create `CHANGELOG.md` with initial version
2. Create `FEATURES.md` with feature matrix
3. Create `docs/adr/` directory with template
4. Update `CLAUDE.md` with conventions
5. Create `.claude/commands/release.md`

### Phase 2: Ongoing Maintenance

- Update documentation with each feature
- Create ADR for significant decisions
- Follow commit conventions strictly

## Consequences

### Positive

- Consistent documentation across the project
- Clear version history and feature tracking
- Automated release workflow reduces errors
- Architectural decisions preserved for future reference
- Better AI assistant context via CLAUDE.md

### Negative

- Additional documentation overhead per feature
- Learning curve for team members new to conventions

### Neutral

- Aligns with cloudempiere-cli standards for cross-project consistency

## Architecture Overview

### Complete System Architecture (Done + Planned)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           EXTERNAL AI CLIENTS                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Claude Code    │    Cursor     │    n8n      │   Custom Apps   │   REST Clients│
│     IDE         │     IDE       │  Workflow   │   LangChain4j   │    HTTP/JSON  │
└────────┬────────┴───────┬───────┴──────┬──────┴────────┬────────┴───────┬───────┘
         │                │              │               │                │
         │ MCP Protocol   │ MCP Protocol │ MCP Protocol  │ langchain4j-mcp│ REST API
         │ (stdio)        │ (stdio)      │ (SSE)         │                │
         ▼                ▼              ▼               ▼                │
┌─────────────────────────────────────────────────────────────────┐       │
│  idempiere-mcp-server (Java 21, MCP SDK 0.16.0) ✅ BUILT        │       │
│  Repository: ~/github/idempiere-mcp-server                      │       │
│  JAR: target/idempiere-mcp-server-1.0.0-SNAPSHOT.jar (8.9MB)    │       │
│  ┌───────────────────────────────────────────────────────────┐  │       │
│  │ ✅ DONE - 6 source files, 5 MCP tools:                    │  │       │
│  │    IdempiereMcpServer.java (main)                         │  │       │
│  │    TableTools.java (listTables, describeTable,            │  │       │
│  │                     addTable, addColumn, syncTable)       │  │       │
│  │    CliBackend.java (calls cloudempiere-cli)               │  │       │
│  │    RestApiBackend.java (calls iDempiere REST)             │  │       │
│  │    BackendAdapter.java, McpConfig.java                    │  │       │
│  ├───────────────────────────────────────────────────────────┤  │       │
│  │ 🔜 PLANNED (Phase 2-4):                ADR-003            │  │       │
│  │    AI tools: chat, query, analyze                         │  │       │
│  │    LangChain4j vector storage, semantic cache             │  │       │
│  └───────────────────────────────────────────────────────────┘  │       │
└────────────────────────────┬────────────────────────────────────┘       │
                             │                                            │
              ┌──────────────┼──────────────┐                             │
              ▼              ▼              ▼                             │
       ┌───────────┐  ┌───────────┐  ┌───────────────┐                    │
       │REST Backend│  │CLI Backend│  │LangChain4j    │                    │
       │(iDempiere) │  │(Quarkus)  │  │Backend (NEW)  │                    │
       └─────┬─────┘  └─────┬─────┘  └───────────────┘                    │
             │              │                                              │
             │              ▼                                              │
             │   ┌─────────────────────────────────────────────────┐      │
             │   │  cloudempiere-cli (Quarkus 3.16, Java 21)       │      │
             │   │  Repository: ~/github/cloudempiere-cli          │      │
             │   │  ┌───────────────────────────────────────────┐  │      │
             │   │  │ ✅ DONE:                                  │  │      │
             │   │  │    registry (tables, columns, windows)    │  │      │
             │   │  │    2pack, generate model/window           │  │      │
             │   │  │    AIService + ClaudeProvider             │  │      │
             │   │  ├───────────────────────────────────────────┤  │      │
             │   │  │ 🔜 PLANNED:                               │  │      │
             │   │  │    Quarkus MCP Extension (Option B)       │  │      │
             │   │  └───────────────────────────────────────────┘  │      │
             │   │  Uses: iDempiere Core JARs (base, pipo)         │      │
             │   └──────────────────────┬──────────────────────────┘      │
             │                          │                                  │
             ▼                          ▼                                  ▼
┌────────────────────────────────────────────────────────────────────────────────┐
│                        iDempiere ERP (Port 8080)                               │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │  com.cloudempiere.ai (OSGi Plugin, Java 11)                              │  │
│  │  Repository: ~/github/com.cloudempiere.ai                                │  │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │  │
│  │  │ ✅ DONE (ADR-001, 002, 004, 006, 007):                             │  │  │
│  │  │    Provider Layer: IAIProvider, AnthropicProvider, AWSBedrock      │  │  │
│  │  │    Data Model: AIG_Provider, AIG_Conversation, AIG_Message         │  │  │
│  │  │    Security: SecureDatabaseQueryExecutor (Role-based)              │  │  │
│  │  │    Context: WindowContextProvider, ChartContextProvider            │  │  │
│  │  ├────────────────────────────────────────────────────────────────────┤  │  │
│  │  │ 🔜 PLANNED:                                                        │  │  │
│  │  │    ADR-002: LangChain4j agents, ERPTools (@Tool)                   │  │  │
│  │  │    ADR-008: System prompts, structured output                      │  │  │
│  │  │    ADR-009: Domain boundaries (Sales, Inventory, Purchasing)       │  │  │
│  │  │    ADR-010: Agent orchestration, supervisor pattern                │  │  │
│  │  │    ADR-011: InventoryAgent, SalesAgent, PurchasingAgent            │  │  │
│  │  │    ADR-012: RAG ContentRetriever, pgvector embeddings              │  │  │
│  │  │    ADR-013: AIMetricsListener, CostGuard, usage tracking           │  │  │
│  │  │    ADR-014: InputGuard, ExecutionGuard, OutputGuard                │  │  │
│  │  │    ADR-015: ParameterChain, ResponseFormatter, ZoomLinks           │  │  │
│  │  └────────────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
│                                                                                │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │  iDempiere Core Services                                                 │  │
│  │    MRole.addAccessSQL() │ AccessSqlParser │ Env context │ CLogger        │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌────────────────────────────────────────────────────────────────────────────────┐
│                        PostgreSQL Database                                      │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │ ✅ DONE:                              │ 🔜 PLANNED:                      │  │
│  │    AD_* (Application Dictionary)      │    AIG_Embedding (pgvector)      │  │
│  │    AIG_Provider                       │    AIG_UsageMetrics              │  │
│  │    AIG_Conversation                   │    AIG_ApprovalRequest           │  │
│  │    AIG_Message                        │    AIG_APIKey                    │  │
│  │    AIG_QueryAudit                     │    AIG_CostBudget                │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────┘
```

### ADR Implementation Status

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         ADR IMPLEMENTATION STATUS                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  CORE ARCHITECTURE                                                              │
│  ═══════════════════════════════════════════════════════════════════════════    │
│  ✅ ADR-001 Initial Architecture ████████████████████████████████████ 100%     │
│  ⚠️  ADR-002 LangChain4j Adoption ████████████████░░░░░░░░░░░░░░░░░░░  60%     │
│  ⚠️  ADR-003 MCP Server           ██████████████████████░░░░░░░░░░░░░  60%     │
│     (idempiere-mcp-server BUILT, AI tools pending)                              │
│  ✅ ADR-004 Java Agent Framework ██████████████████████████████████░░  95%     │
│                                                                                 │
│  DATA & INTELLIGENCE                                                            │
│  ═══════════════════════════════════════════════════════════════════════════    │
│  ❌ ADR-005 Data Source Routing  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│     (SUPERSEDED by ADR-012)                                                     │
│  ✅ ADR-006 Data Model           ████████████████████████████████████ 100%     │
│  ✅ ADR-007 Database Security    ████████████████████████████████████ 100%     │
│  ✅ ADR-008 LLM Instructions     ██████████████████████████████████░░  95%     │
│  🔜 ADR-012 RAG Context          ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│                                                                                 │
│  AGENT ARCHITECTURE                                                             │
│  ═══════════════════════════════════════════════════════════════════════════    │
│  ⚠️  ADR-009 Domain Boundaries   ██████████░░░░░░░░░░░░░░░░░░░░░░░░░░  30%     │
│  ⚠️  ADR-010 Agent Orchestration ████████████████░░░░░░░░░░░░░░░░░░░░  40%     │
│  🔜 ADR-011 Specialized Agents   ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│                                                                                 │
│  OPERATIONS & UX                                                                │
│  ═══════════════════════════════════════════════════════════════════════════    │
│  🔜 ADR-013 Observability        ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│  🔜 ADR-014 Guardrails           ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│  🔜 ADR-015 UX Patterns          ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│  🔜 ADR-016 Knowledge Base Agent ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│                                                                                 │
│  USE CASES - PHASE 1 (MVP)                                                      │
│  ═══════════════════════════════════════════════════════════════════════════    │
│  🔜 ADR-017 Chart Executive      ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│  🔜 ADR-018 Sales Opportunity    ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│  🔜 ADR-019 Ticket Classification░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│  🔜 ADR-020 Email Gateway        ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│                                                                                 │
│  USE CASES - PHASE 2                                                            │
│  ═══════════════════════════════════════════════════════════════════════════    │
│  🔜 ADR-021 Product Catalog      ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│  🔜 ADR-022 Translation Wizard   ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│                                                                                 │
│  USE CASES - PHASE 3                                                            │
│  ═══════════════════════════════════════════════════════════════════════════    │
│  🔜 ADR-023 OCR Invoice          ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│  🔜 ADR-024 Data Normalization   ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│  🔜 ADR-025 Dev Assistant        ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   0%     │
│                                                                                 │
│  LEGEND: ✅ Done  ⚠️ Partial  🔜 Planned  ❌ Superseded                         │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              DATA FLOW                                           │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  User Request                                                                   │
│       │                                                                         │
│       ▼                                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                         │
│  │ InputGuard  │───▶│ Cost Check  │───▶│ Risk Level  │                         │
│  │ (ADR-014)   │    │ (ADR-013)   │    │ (ADR-014)   │                         │
│  │ PII, Inject │    │ Budget OK?  │    │ L/M/H/CRIT  │                         │
│  └─────────────┘    └─────────────┘    └──────┬──────┘                         │
│                                               │                                 │
│                     ┌─────────────────────────┼─────────────────────────┐       │
│                     ▼                         ▼                         ▼       │
│              ┌───────────┐            ┌───────────┐            ┌───────────┐   │
│              │ LOW RISK  │            │ MED/HIGH  │            │ CRITICAL  │   │
│              │ Auto-exec │            │ Log+Exec  │            │ Approval  │   │
│              └─────┬─────┘            └─────┬─────┘            └─────┬─────┘   │
│                    │                        │                        │         │
│                    └────────────────────────┼────────────────────────┘         │
│                                             ▼                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                     AGENT ORCHESTRATION (ADR-010)                       │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │   │
│  │  │ Supervisor  │  │ Inventory   │  │   Sales     │  │ Purchasing  │    │   │
│  │  │   Agent     │──│   Agent     │  │   Agent     │  │   Agent     │    │   │
│  │  │ (ADR-010)   │  │ (ADR-011)   │  │ (ADR-011)   │  │ (ADR-011)   │    │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                             │                                   │
│                    ┌────────────────────────┼────────────────────────┐         │
│                    ▼                        ▼                        ▼         │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐    │
│  │  CONTEXT Domain     │  │  DATABASE Domain    │  │  MCP TOOLS Domain   │    │
│  │  (ADR-012 RAG)      │  │  (ADR-007 Secure)   │  │  (ADR-003)          │    │
│  │  ┌───────────────┐  │  │  ┌───────────────┐  │  │  ┌───────────────┐  │    │
│  │  │ Conversation  │  │  │  │ ERPTools      │  │  │  │ External MCP  │  │    │
│  │  │ History       │  │  │  │ @Tool methods │  │  │  │ Servers       │  │    │
│  │  │ RAG Retrieval │  │  │  │ SQL Executor  │  │  │  │ (GitHub, etc) │  │    │
│  │  │ pgvector      │  │  │  │ Role Check    │  │  │  │               │  │    │
│  │  └───────────────┘  │  │  └───────────────┘  │  │  └───────────────┘  │    │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘    │
│                                             │                                   │
│                                             ▼                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                        OUTPUT PROCESSING                                │   │
│  │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                 │   │
│  │  │ OutputGuard │───▶│ Formatter   │───▶│  Metrics    │                 │   │
│  │  │ (ADR-014)   │    │ (ADR-015)   │    │ (ADR-013)   │                 │   │
│  │  │ Hallucinate │    │ ZoomLinks   │    │ Tokens/Cost │                 │   │
│  │  └─────────────┘    └─────────────┘    └─────────────┘                 │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                             │                                   │
│                                             ▼                                   │
│                                      User Response                              │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Project Ecosystem

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        CLOUDEMPIERE AI ECOSYSTEM                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │ idempiere-mcp-server          │ cloudempiere-cli       │ com.cloudempiere.ai│
│  │ ~/github/idempiere-mcp-server │ ~/github/cloudempiere-cli │ ~/github/com...   │
│  ├─────────────────────────────────────────────────────────────────────────┤   │
│  │ Plain Java 21 (no framework)  │ Quarkus 3.16 + Java 21 │ OSGi + Java 11     │
│  │ MCP SDK 0.16.0 (Anthropic)    │ Picocli CLI            │ LangChain4j        │
│  │ Maven shade uber-jar          │ Quarkus runner JAR     │ iDempiere Plugin   │
│  │ NOT a git repo (generated)    │ Git repo (develop)     │ Git repo (langchain)│
│  ├─────────────────────────────────────────────────────────────────────────┤   │
│  │ PURPOSE:                      │ PURPOSE:               │ PURPOSE:           │
│  │ External AI agent access      │ Developer CLI +        │ ERP AI runtime     │
│  │ (Claude Code, Cursor)         │ App Dictionary ops     │ inside iDempiere   │
│  ├─────────────────────────────────────────────────────────────────────────┤   │
│  │ MCP TOOLS (5):                │ COMMANDS:              │ COMPONENTS:        │
│  │ ✅ listTables                 │ ✅ registry            │ ✅ IAIProvider     │
│  │ ✅ describeTable              │ ✅ 2pack               │ ✅ AnthropicProvider│
│  │ ✅ addTable                   │ ✅ generate model      │ ✅ SecureQuery     │
│  │ ✅ addColumn                  │ ✅ ai generate         │ 🔜 LangChain4j     │
│  │ ✅ syncTable                  │ ✅ AIService           │ 🔜 Domain Agents   │
│  │ 🔜 chat, query, analyze       │ 🔜 Quarkus MCP         │ 🔜 RAG/Guardrails  │
│  │ BACKENDS:                     │                        │                    │
│  │ ✅ CliBackend                 │                        │                    │
│  │ ✅ RestApiBackend             │                        │                    │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  INTEGRATION:                                                                   │
│  ─────────────                                                                  │
│  idempiere-mcp-server ──[CLI backend]──▶ cloudempiere-cli ──[DB]──▶ PostgreSQL │
│  idempiere-mcp-server ──[REST backend]──▶ iDempiere REST API                   │
│  cloudempiere-cli ──[core JARs]──▶ org.adempiere.base, pipo                    │
│  com.cloudempiere.ai ──[OSGi]──▶ iDempiere Runtime                             │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## References

- [Conventional Commits](https://conventionalcommits.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
- [PROJECT.md](../../PROJECT.md)
- [FEATURES.md](../../FEATURES.md)
- [CHANGELOG.md](../../CHANGELOG.md)

---

*ADR-001 | Version 2.0 | 2025-12-01*
*Updated: Added comprehensive architecture diagrams showing done/planned status*
