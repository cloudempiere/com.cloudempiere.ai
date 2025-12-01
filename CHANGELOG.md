# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Conventional Commits](https://conventionalcommits.org/).

## [Unreleased]

### Next: v0.9.0
- AWS Bedrock completion
- Ollama local LLM integration
- Domain agents (Inventory, Sales, Purchasing)

---

## [0.8.0] - 2025-12-01

### Phase 8: MCP Server & Strategic Architecture

#### Added
- **MCP Server Documentation** (11 comprehensive guides)
  - `00-START_HERE.md` - Quick orientation guide
  - `01-ARCHITECTURE.md` - System design and integration patterns
  - `02-IMPLEMENTATION_GUIDE.md` - Complete build guide (HTTP API + Node.js MCP)
  - `03-BEST_PRACTICES.md` - Security, reliability, performance patterns
  - `04-DEPLOYMENT.md` - Docker, AWS, on-prem deployment
  - `05-MCP_AGENT_INTEGRATION.md` - Agent integration patterns
  - `06-REUSE_EXISTING_PLUGIN.md` - Leveraging current codebase
  - `INDEX.md` - Documentation navigation
  - `MCP_BENEFITS.md` - Business value and ROI
  - `QUICK_REFERENCE.md` - Developer cheatsheet
  - `README.md` - Project overview
- **Architecture Decision Records**
  - `ADR-002` - LangChain4j strategic adoption plan
  - `ADR-002-appendix` - Feature mapping (no functionality lost)
  - `ADR-003` - MCP server integration decision
- **Project Governance**
  - `docs/GOVERNANCE.md` - Workflows, conventions, release process

#### Changed
- Updated roadmap with strategic LangChain4j migration path
- Refined phase planning for v0.9.0 - v1.0.0

**Commits:**
- `026f0fb` docs(MCPServer): Add comprehensive MCP server documentation
- `02ad59d` docs(ADR): Add feature mapping appendix
- `80ac7bd` docs(ADR): Add ADR-002 for LangChain4j strategic adoption

---

## [0.7.0] - 2025-12-01

### Phase 7: Documentation & Claude Agents

#### Added
- **Claude Code Agents**: 26 specialized agents for iDempiere development
  - Architecture, caching, code review, data modeling experts
  - Docker, deployment, localization, migration specialists
  - OSGi, plugin, process, workflow developers
  - Quality assurance, reporting, terraform experts
- **Project Documentation**
  - `PROJECT.md` - Project overview and quick start guide
  - `FEATURES.md` - Comprehensive feature matrix
  - `docs/adr/001-initial-architecture.md` - Architecture Decision Record
  - `.claude/commands/release.md` - Release workflow command

#### Changed
- Updated `CLAUDE.md` with roadmap phases and commit conventions

---

## [0.6.0] - 2025-11-28

### Phase 6: LangChain4j Agent Framework

#### Added
- **LangChain4j Integration**
  - Agent framework for complex AI workflows
  - `langchain4j.jar` embedded in plugin
  - Agent-based conversation handling

**Commit:** `b39939e` feat(LangChain): Integrate LangChain4j agent framework, CLD-1606

---

## [0.5.0] - 2025-11-26

### Phase 5: LangChain Integration

#### Added
- **LangChain Framework**
  - ERP tool integration layer
  - Enhanced AI context with syntax support
- **Thread-Scoped Context**
  - Conversation context persistence
  - History management per thread
- **Documentation**
  - MCP server implementation guide
  - Strategic analysis documents
  - iDempiere context agent specification

**Commits:**
- `01960c2` feat(AIPlugin): Add langchain framework and ERP tool integration, CLD-1606
- `eda02bd` feat(AIPlugin): Add thread-scoped conversation context and history, CLD-1606
- `6800a87` docs(MCPServer): Add comprehensive MCP server documentation

**PR:** #6 merged to master

---

## [0.4.0] - 2025-11-26

### Phase 4: AI Chat Widget (CLD-1606)

#### Added
- **AI Chat Component**
  - Interactive chat widget for ZK UI
  - `AIChatWidget` class with conversation support
- **Tab Context Awareness**
  - Extract context from active iDempiere tab
  - Window and record context injection
- **Conversation Features**
  - Thread-scoped conversation history
  - Intelligent data source routing
  - Zoom link support for enhanced UX
- **Query Improvements**
  - SQL clause handling fixes
  - Better query generation

**Commits:**
- `9666cbc` feat(AIChat): Add AI chat widget and supporting classes, CLD-1606
- `8c13dc0` feat(AIChat): Add tab context to ai chat, CLD-1606
- `27107d9` feat(AIPlugin): AI chat enhancements, CLD-1606
- `ab084dd` feat(AIPlugin): Add zoom link support and improve AI chat UX, CLD-1606
- `4a42139` feat(AIPlugin): Add intelligent data source routing and SQL clause fix, CLD-1606

**PR:** #5 (CLD-1606)

---

## [0.3.0] - 2025-11-20

### Phase 3: Security Layer & Context Providers (CLD-1601)

#### Added
- **Secure Database Query Executor**
  - `SecureDatabaseQueryExecutor` class
  - AI User identity model for queries
  - Role-based access control via `AccessSqlParser`
  - Query validation (SQL injection prevention)
- **Audit System**
  - `SecureQueryAudit` DTO
  - `AIG_QueryAudit` table for audit trail
  - Records AI user and initiating user
- **Context Providers**
  - `IAIContextProvider` interface
  - `AIContextProviderRegistry` for provider management
  - `WindowContextProvider` - iDempiere window data extraction
  - `ChartContextProvider` - Chart data extraction
  - `ContextParameters` DTO

#### Fixed
- Use constant for success status in `isSuccess()` method

**Commits:**
- `8a01cbe` feat(AIPlugin): Add AI context provider infrastructure, CLD-1601
- `c428471` feat(AIPlugin): Add secure AI database query executor and audit, CLD-1601
- `ccce013` Implement AI user identity for secure query execution
- `36f95dd` fix(AIPlugin): Use constant for success status in isSuccess(), CLD-1601

**PRs:** #3, #4

---

## [0.2.0] - 2025-11-18

### Phase 2: AWS Bedrock Integration (CLD-1601)

#### Added
- **AWS Bedrock Provider**
  - `AWSBedrockProvider` class (skeleton implementation)
  - Foundation models integration structure
- **Dependencies**
  - Netty HTTP client for AWS SDK
  - `bedrockruntime.jar`
  - Netty jars (buffer, codec, handler, transport, etc.)

**Commits:**
- `b98f7d4` feat(AIPlugin): AWS Bedrock AI provider, CLD-1601
- `7609037` feat(AIPlugin): Add Netty HTTP client dependencies for AWS SDK, CLD-1601

**PR:** #2

---

## [0.1.0] - 2025-11-18

### Phase 1: Initial Provider Infrastructure (CLD-1601)

#### Added
- **Plugin Setup**
  - OSGi bundle configuration
  - `Activator.java` for bundle lifecycle
  - Maven/Tycho build configuration
- **Provider Architecture**
  - `IAIProvider` interface - provider abstraction
  - `IAIProviderFactory` interface
  - `AIProviderFactory` OSGi service (@Component)
  - Provider registry with caching
- **Anthropic Claude Provider**
  - `AnthropicProvider` - full implementation
  - Claude 3 Opus, Sonnet, Haiku support
  - Claude 3.5 Sonnet support
  - Synchronous text generation
  - Streaming text generation with callbacks
  - Function calling / tool use support
  - Cost estimation with pricing (Jan 2025)
  - Health monitoring
  - Rate limit tracking
- **Data Transfer Objects**
  - `AIRequest` - request wrapper (Builder pattern)
  - `AIResponse` - response with tokens and cost
  - `AIMessage` - conversation message
  - `AITokenUsage` - token tracking
  - `AIModelCapabilities` - feature flags per model
  - `AIHealthStatus` - health monitoring
  - `AIRateLimitStatus` - rate limit tracking
  - `AIFunction` / `AIFunctionCall` - tool use
  - `AIStreamCallback` - streaming interface
- **Data Models**
  - `I_AIG_Provider` - interface (from AD_Table)
  - `X_AIG_Provider` - base model (from AD_Table)
  - `MAIProvider` - business logic model
  - `AIG_Provider` table for provider configuration
- **Exception Handling**
  - `AIProviderException` with error codes and retryable flag
- **Processes**
  - `TestAIProvider` - provider connectivity testing
- **Dependencies**
  - Anthropic Java SDK v2.10.0
  - Jackson v2.17.0
  - OkHttp v4.12.0
  - Kotlin v1.9.10

**Commits:**
- `6ccc13b` feat(AIPlugin): Initial plugin setup, CLD-1601
- `dfd2182` feat(AIPlugin): AI provider, CLD-1601
- `98c7db2` fix(AnthropicProvider): Refactor Anthropic provider and add test process, CLD-1601

**PR:** #1

---

## Version Format

- **Major** (X.0.0): Breaking changes to provider API
- **Minor** (0.X.0): New features, backward compatible
- **Patch** (0.0.X): Bug fixes, backward compatible

## Phase Summary

| Phase | Version | Date | Milestone |
|-------|---------|------|-----------|
| 1 | v0.1.0 | 2025-11-18 | Initial Provider Infrastructure |
| 2 | v0.2.0 | 2025-11-18 | AWS Bedrock Integration |
| 3 | v0.3.0 | 2025-11-20 | Security Layer & Context Providers |
| 4 | v0.4.0 | 2025-11-26 | AI Chat Widget |
| 5 | v0.5.0 | 2025-11-26 | LangChain Integration |
| 6 | v0.6.0 | 2025-11-28 | LangChain4j Agent Framework |
| 7 | v0.7.0 | 2025-12-01 | Documentation & Claude Agents |
| 8 | v0.8.0 | 2025-12-01 | MCP Server & Strategic Architecture |

## Upcoming Phases

| Phase | Version | Target | Milestone |
|-------|---------|--------|-----------|
| 9 | v0.9.0 | Q1 2026 | LangChain4j Native Providers |
| 10 | v0.10.0 | Q1 2026 | Domain Agents (Inventory, Sales, Purchasing) |
| 11 | v0.11.0 | Q2 2026 | Production Database Schema |
| 12 | v1.0.0 | Q2 2026 | Production Release |

## iDempiere Compatibility

| Plugin Version | iDempiere Version | Bundle-Version |
|----------------|-------------------|----------------|
| 0.1.0 - 0.8.0 | 10.x, 11.x, 12.x | 10.0.0.qualifier |
