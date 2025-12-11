# com.cloudempiere.ai Features

This document tracks features, implementation status, and version compatibility aligned with the three core project goals:

**A. System Administrator / Helpdesk Support** - Intelligent issue diagnosis
**B. End User / Role-Scoped WebUI Assistant** - Natural language ERP interface
**C. Model Context Protocol (MCP) Server** - External AI client integration

---

## Current Version: v0.22.0

**Status:** ✅ Clickable Record Links in Chat with Table Support (ADR-039)
**Next:** v0.23.0 - Share Dialog & Advanced Sharing

**Strategic Focus:** The Chat Widget is our primary AI implementation. All features will be integrated and tested through the chat widget before other use cases (charts, reports, etc.).

---

## Version History

| Version | Date | Key Features | Focus Area |
|---------|------|--------------|------------|
| **0.22.0** | 2025-12-11 | **Clickable Record Links in Chat with Table Support (ADR-039)** | **UX & Navigation** |
| 0.21.0 | 2025-12-10 | Real-Time Streaming Improvements (Tables & Emojis) | UX |
| 0.20.0 | 2025-12-10 | Language Detection & User-Friendly Error Handling | UX & i18n |
| 0.19.0 | 2025-12-08 | Streaming-First Architecture & Tool Support | Architecture |
| 0.18.0 | 2025-12-08 | Llama Provider & Model Selection | Multi-Provider |
| 0.17.2 | 2025-12-08 | Unit Test Infrastructure | Testing & Docs |
| 0.17.1 | 2025-12-08 | Window Context Fix | Bug Fix |
| 0.17.0 | 2025-12-07 | Streaming Tool Callbacks & Markdown | User Experience |
| 0.16.0 | 2025-12-04 | Chat Ownership & Sharing | Multi-User Collaboration |
| 0.15.0 | 2025-12-04 | Stop Button & Error Handling | User Experience |
| 0.14.0 | 2025-12-04 | Real-Time Chat Streaming | User Experience |
| 0.13.0 | 2025-12-03 | Naming Standards, Integration Wiring | Code Quality |
| 0.12.0 | 2025-12-03 | Observability, Guardrails, Multi-Tenant | P1 Infrastructure |
| 0.11.0 | 2025-12-03 | RAG Infrastructure & Domain Boundaries | Context & Security |
| 0.10.0 | 2025-12-01 | Security Fixes & Migration Scripts | Security |
| 0.9.0 | 2025-12-01 | LangChain4j Native Providers | Foundation |
| 0.8.0 | 2025-12-01 | MCP Server & Strategic Architecture | Documentation |
| 0.7.0 | 2025-12-01 | Documentation & Claude Agents | Developer Experience |
| 0.6.0 | 2025-11-28 | LangChain4j Agent Framework | Agent Infrastructure |
| 0.5.0 | 2025-11-26 | LangChain Integration | Tool Layer |
| 0.4.0 | 2025-11-26 | AI Chat Widget (CLD-1606) | User Interface |
| 0.3.0 | 2025-11-20 | Security Layer & Context Providers | Security |
| 0.2.0 | 2025-11-18 | AWS Bedrock Integration | Multi-Provider |
| 0.1.0 | 2025-11-18 | Initial Provider Infrastructure | Foundation |

---

## Feature Matrix by Goal

### Goal A: System Administrator / Helpdesk Support

**Target Users:** System admins, support engineers, technical consultants

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| **Error Analysis Tool** | - | 🔜 Planned | Analyze posting failures, trace errors through logs |
| **System Health Checks** | 0.1.0 | ✅ Done | Provider health monitoring via AIHealthStatus |
| **Configuration Assistant** | - | 🔜 Planned | Guide admins through setup (GL categories, accounts) |
| **Audit Query Tool** | 0.3.0 | ✅ Done | SecureDatabaseQueryExecutor with full audit trail |
| **HelpDeskAgent** | - | 🔜 v0.11.0 | Specialized agent for troubleshooting |
| **AD_Error Integration** | - | 🔜 Planned | Query error logs via ERPTools |
| **System Metadata Search** | 0.9.0 | ✅ Done | getTableMetadata, listTables tools |

**Priority:** 🟡 Medium - After end-user features are stable

---

### Goal B: End User / Role-Scoped WebUI Assistant

**Target Users:** Business users (sales, purchasing, inventory, accounting)

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| **AI Chat Widget (ZK)** | 0.4.0 | ✅ Done | Interactive chat component in iDempiere WebUI |
| **Contextual Chat** | 0.4.0 | ✅ Done | Window/tab context awareness |
| **Role-Based Security** | 0.3.0 | ✅ Done | Queries filtered by AD_Client_ID, AD_Org_ID, AD_Role_ID |
| **Chat Ownership & Sharing** | 0.16.0 | ✅ Done | Multi-user chat access control (ADR-036) |
| **Natural Language Queries** | 0.9.0 | ✅ Done | "Show me pending orders over $10k" |
| **9 ERP Tools** | 0.9.0 | ✅ Done | Database query, lookup, search, metadata |
| **Business Object Shortcuts** | 0.9.0 | ✅ Done | getBusinessPartner, getProduct, getOrder |
| **Conversation Memory** | 0.6.0 | ✅ Done | Session-based chat history (20 messages) |
| **Streaming Responses** | 0.9.0 | ✅ Done | Real-time token streaming for better UX |
| **Zoom Link Support** | 0.22.0 | ✅ Done | Clickable record links in chat (ADR-039 Phase 1) |
| **Table Cell Zoom Links** | 0.22.0 | ✅ Done | Zoom links work in markdown table cells |
| **RAG (Context Retrieval)** | 0.10.0 | 🚧 In Progress | ADR-012: LangChain4j ContentRetriever with semantic search |
| **Share Dialog** | - | 🔜 v0.17.0 | Share button with user/role picker (ADR-036 Phase 3) |
| **Structured Outputs** | - | 🔴 v0.10.0 | OrderSummary, InventoryReport records |
| **Domain Agents** | - | 🔴 v0.11.0 | InventoryAgent, SalesAgent, PurchasingAgent |
| **Process Execution** | - | 🟡 v0.11.0 | AI can trigger iDempiere processes (with approval) |
| **Report Generation** | - | 🟢 v1.0.0 | AI-generated Jasper reports |

**Priority:** 🔴 Critical - Primary user-facing value

---

### Goal C: Model Context Protocol (MCP) Server

**Target Users:** Developers, external AI clients, integrators

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| **MCP Documentation** | 0.8.0 | ✅ Done | 11 comprehensive guides (architecture, deployment, best practices) |
| **Architecture Design** | 0.8.0 | ✅ Done | HTTP API + Node.js MCP dual-layer approach |
| **HTTP API Layer** | - | 🚧 In Progress | Java REST endpoints for tool exposure |
| **Node.js MCP Server** | - | 🔜 v0.10.0 | MCP protocol implementation |
| **Authentication** | - | 🔜 v0.10.0 | iDempiere user/role authentication |
| **Tool Discovery** | 0.9.0 | ✅ Done | ERPTools exposed as MCP tools |
| **Multi-Tenant Isolation** | 0.3.0 | ✅ Done | Automatic client/org filtering |
| **Claude Desktop Integration** | - | 🔜 v0.10.0 | Test with Claude Desktop client |
| **VS Code Extension Support** | - | 🟡 v0.11.0 | MCP for VS Code |
| **Webhook Support** | - | 🟢 v1.0.0 | Async event notifications |

**Priority:** 🟡 Medium - After core features are solid

---

## Core Infrastructure Features

### AI Provider Support

| Provider | Version | Status | Technology | Description |
|----------|---------|--------|------------|-------------|
| **Anthropic Claude** | 0.9.0 | ✅ Done | LangChain4j | Native integration (Sonnet 4, 3.5, 3, Opus, Haiku) |
| **AWS Bedrock** | 0.9.0 | ✅ Done | LangChain4j | Claude, Nova, Mistral, Llama via BedrockChatModel |
| **Ollama (Local)** | 0.9.0 | ✅ Done | LangChain4j | Local LLM support (llama3.2, mistral, etc.) ⚠️* |
| **Llama (via Ollama)** | 0.18.0 | ✅ Done | LangChain4j | Llama models via Ollama backend ⚠️* |
| **OpenAI** | 0.9.0 | ✅ Done | LangChain4j | GPT-4o, GPT-4-turbo via OpenAiChatModel |
| **Azure OpenAI** | - | 🟡 v0.11.0 | LangChain4j | Enterprise OpenAI deployment |
| **Custom Providers** | 0.9.0 | ✅ Done | LangChain4j | Extensible via ChatLanguageModel interface |

**\*Ollama/Llama Tool Support Limitation:** Streaming mode uses `SimpleStreamingAgent` without tools due to LangChain4j 0.35.0 limitations. Full streaming+tools requires LangChain4j 0.37.0+ (Java 17). Use Anthropic/OpenAI/Bedrock for full tool support.

**Legacy Providers (Deprecated in v0.9.0):**
- ❌ Custom `AnthropicProvider` → Replaced by `AnthropicChatModel`
- ❌ Custom `AWSBedrockProvider` → Replaced by `BedrockChatModel`
- ❌ `IAIProvider` interface → Replaced by LangChain4j `ChatLanguageModel`

---

### LangChain4j Agent Framework

| Feature | Version | Status | Technology | Description |
|---------|---------|--------|------------|-------------|
| **AiServices** | 0.6.0 | ✅ Done | LangChain4j | Automatic agent orchestration |
| **@Tool Annotations** | 0.9.0 | ✅ Done | LangChain4j | Declarative tool discovery |
| **ChatMemory** | 0.6.0 | ✅ Done | LangChain4j | MessageWindowChatMemory (per session) |
| **ContentRetriever (RAG)** | - | 🔴 v0.10.0 | LangChain4j | Semantic search & context injection |
| **Structured Outputs** | - | 🔴 v0.10.0 | LangChain4j | Java records as response schemas |
| **Input/Output Guards** | 0.12.0 | ✅ Done | LangChain4j | InputGuard, OutputGuard, ExecutionGuard |
| **Listeners (Observability)** | 0.12.0 | ✅ Done | LangChain4j | AIMetricsListener, CostGuard |
| **Agentic Patterns** | - | 🔴 v0.11.0 | LangChain4j | Sequential, parallel, conditional workflows |
| **Agents as Tools** | - | 🟡 v0.11.0 | LangChain4j | Agent-to-Agent delegation |

---

### ERP Tools (@Tool Annotated)

| Tool | Version | Status | Description | Use Case |
|------|---------|--------|-------------|----------|
| **queryDatabase** | 0.9.0 | ✅ Done | Execute SQL SELECT queries | "Show me overdue orders" |
| **lookupRecord** | 0.9.0 | ✅ Done | Get record by ID | "Details of order SO-1234" |
| **searchRecords** | 0.9.0 | ✅ Done | Search with WHERE clause | "Find customers in California" |
| **getTableMetadata** | 0.9.0 | ✅ Done | Table structure info | "What fields are in C_Order?" |
| **listTables** | 0.9.0 | ✅ Done | List accessible tables | "Show me inventory tables" |
| **getBusinessPartner** | 0.9.0 | ✅ Done | BP lookup by ID or Value | "Find customer Acme Corp" |
| **getProduct** | 0.9.0 | ✅ Done | Product lookup | "Find product SKU-1024" |
| **getOrder** | 0.9.0 | ✅ Done | Order lookup | "Show order SO-1234" |
| **executeProcess** | - | 🟡 v0.11.0 | Trigger iDempiere process | "Run MRP for Product 1024" |
| **generateReport** | - | 🟢 v1.0.0 | Generate Jasper report | "Generate sales report for Q4" |
| **searchDocumentation** | - | 🔴 v0.10.0 | RAG over help texts | "How do I post an invoice?" |

**Tool Execution Security:**
- ✅ All tools execute with user's role permissions
- ✅ Automatic AD_Client_ID, AD_Org_ID filtering
- ✅ SQL injection prevention
- ✅ Audit logging (AIG_QueryAudit table)

---

### Security & Access Control

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| **SecureDatabaseQueryExecutor** | 0.3.0 | ✅ Done | Role-based query execution |
| **AI User Identity Model** | 0.3.0 | ✅ Done | Separate AI user for audit trail |
| **AccessSqlParser Integration** | 0.3.0 | ✅ Done | iDempiere RBAC enforcement |
| **SQL Injection Prevention** | 0.3.0 | ✅ Done | Query validation & parameterization |
| **Audit Logging** | 0.3.0 | ✅ Done | AIG_QueryAudit table (who, what, when) |
| **Org/Client Filtering** | 0.3.0 | ✅ Done | Automatic multi-tenant isolation |
| **Sensitive Metadata Filtering** | 0.10.0 | ✅ Done | Filter user_id, role_id from AI responses |
| **Cost Boundaries** | 0.12.0 | ✅ Done | CostGuard with budget enforcement, rate limiting |
| **PII Detection** | 0.12.0 | ✅ Done | InputGuard detects SSN, credit cards, emails |
| **Prompt Injection Prevention** | 0.12.0 | ✅ Done | InputGuard blocks injection attempts |
| **Credential Leak Prevention** | 0.12.0 | ✅ Done | OutputGuard blocks API keys, passwords |
| **Multi-Tenant Access Tiers** | 0.12.0 | ✅ Done | AIGAccessTier (TENANT, TENANT_DICTIONARY, SERVICE_PROVIDER) |

---

### Context Providers

| Provider | Version | Status | Description |
|----------|---------|--------|-------------|
| **WindowContextProvider** | 0.3.0 | ✅ Done | Current window/tab/record data |
| **ChartContextProvider** | 0.3.0 | ✅ Done | Dashboard chart data extraction |
| **ApplicationDictionary** | 0.9.0 | ✅ Done | Metadata from AD_Table, AD_Column |
| **FormContextProvider** | - | 🟡 v0.11.0 | Form field state extraction |
| **ProcessContextProvider** | - | 🟡 v0.11.0 | Process parameter context |
| **UserPreferencesProvider** | - | 🟢 v1.0.0 | User settings & preferences |

---

### Data Models (iDempiere Tables)

| Table | Version | Status | Description |
|-------|---------|--------|-------------|
| **AIG_Provider** | 0.1.0 | ✅ Done | AI provider configuration |
| **AIG_QueryAudit** | 0.3.0 | ✅ Done | Query audit trail |
| **AIG_Chat** | 0.10.0 | ✅ Done | Chat session metadata |
| **AIG_ChatEntry** | 0.10.0 | ✅ Done | Chat message history |
| **AIG_UsageMetrics** | 0.12.0 | ✅ Done | Token usage, cost, latency tracking |
| **AIG_Budget** | 0.12.0 | ✅ Done | Budget limits (daily, monthly, per-agent) |
| **AIG_Agent** | - | 🔜 v0.13.0 | Agent configuration |
| **AIG_AgentBoundary** | - | 🔜 v0.13.0 | Agent permission boundaries |
| **AIG_Conversation** | - | 🔜 v0.14.0 | Persistent conversation store |
| **AIG_Document** | - | 🟡 v0.14.0 | RAG document store |

---

## Advanced Features (Roadmap)

### v0.14.0 - Chat Widget LangChain4j Migration (ADR-031)

**Focus:** Wire AIChatWidget to use LangChain4j infrastructure with guardrails and metrics.

| Feature | Priority | Technology | Description |
|---------|----------|------------|-------------|
| **AIChatWidget → AIService** | 🔴 Critical | LangChain4j | Replace legacy AIConversationService |
| **Guardrails in Chat Flow** | 🔴 Critical | CostGuard, InputGuard, OutputGuard | Pre/post processing |
| **Metrics in Chat Flow** | 🔴 Critical | AIMetricsListener | Token/cost tracking per message |
| **ThreadAwareChatMemory** | 🔴 Critical | LangChain4j ChatMemory | Thread-isolated conversation memory |
| **Feature Flag** | 🟡 Medium | System Property | `ai.chat.service=LANGCHAIN4J|LEGACY` |

### v0.15.0 - RAG & Streaming in Chat

**Focus:** Semantic context retrieval and real-time response streaming.

| Feature | Priority | Technology | Description |
|---------|----------|------------|-------------|
| **RAG in Chat Flow** | 🔴 Critical | RAGContextManager | Semantic context retrieval |
| **Streaming Responses** | 🔴 Critical | StreamingChatModel | Token-by-token rendering |
| **Legacy Code Removal** | 🟡 Medium | Refactoring | Remove AIConversationService (~1,500 lines) |

### v0.16.0+ - Business Use Cases & Domain Agents

**Focus:** Extend proven chat infrastructure to other UI contexts.

| Feature | Priority | Technology | Description |
|---------|----------|------------|-------------|
| **ADR-017: Chart Executive Overview** | 🟡 Medium | LangChain4j AiServices | Chart data analysis |
| **ADR-018: Sales Opportunity Summary** | 🟡 Medium | Structured Outputs | Typed response objects |
| **ADR-011: Domain Agents** | 🟡 Medium | Agentic Patterns | Inventory, Sales, Purchasing |
| **ADR-026: pgvector** | 🟡 Medium | PostgreSQL Extension | Production embedding persistence |
| **MCP REST API** | 🟡 Medium | JAX-RS | HTTP API for external clients |

### v1.0.0 - Production Release (Q2 2026)

| Feature | Priority | Description |
|---------|----------|-------------|
| **Production Deployment** | 🔴 Critical | Docker, Kubernetes, cloud-ready |
| **Complete Documentation** | 🔴 Critical | User guides, admin guides, API docs |
| **Monitoring & Alerting** | 🟡 Medium | Prometheus, Grafana integration |
| **Performance Benchmarks** | 🟡 Medium | Load testing, optimization |
| **Enterprise Support** | 🟢 Low | SLA, support channels |

---

## Validation Status (ADR Review 2025-12-01)

Based on validation reports:
- [ADR Validation Report](docs/ADR_VALIDATION_REPORT.md)
- [Instruction Following Validation Report](docs/INSTRUCTION_FOLLOWING_VALIDATION_REPORT.md)
- [Routing Validation Report](docs/ROUTING_VALIDATION_REPORT.md)
- [Database Access Validation Report](docs/AI_DATABASE_ACCESS_VALIDATION_REPORT.md) - 4 planning docs (~3,700 lines)
- [Data Source Validation Report](docs/DATA_SOURCE_VALIDATION_REPORT.md)
- [LangChain4j Integration Validation Report](docs/LANGCHAIN4J_INTEGRATION_VALIDATION_REPORT.md)
- [CLD-1601 Folder Validation Report](docs/CLD_1601_VALIDATION_REPORT.md) - 8 planning docs (~8,750 lines) all superseded

### ✅ Validated Architecture
- **ADR-002** (LangChain4j Adoption) - ✅ Excellent decision, implemented in v0.9.0
- **ADR-004** (Java Agent Framework) - ✅ Correct choice, OSGi-compatible
- **ADR-007** (Database Security) - ✅ Fully implemented, excellent security model
- **ADR-008** (Instruction Following) - ✅ 60% of strategies implemented, enhancements planned v0.11.0

### ✅ Updated / Superseded
- **ADR-005** (Intelligent Routing) - ✅ Superseded by ADR-012 (RAG-Based Context Retrieval)
- **ADR-012** (RAG-Based Context Retrieval) - ✅ Created 2025-12-01, planned implementation in v0.10.0

### ✅ Completed (v0.12.0)
- **ADR-009** (Boundaries) - ✅ Done: 7 boundary classes + ADR-029 integration
- **ADR-013** (Observability) - ✅ Done: AIMetricsListener, CostGuard, MAIUsageMetrics, MAIBudget
- **ADR-014** (Guardrails) - ✅ Done: InputGuard, OutputGuard, ExecutionGuard, GuardResult
- **ADR-029** (Multi-Tenant) - ✅ Done: AIGAccessTier, tiered access filters

### ⚠️ Needs Update
- **ADR-010** (Orchestration) - ⚠️ Should reference `langchain4j-agentic-patterns` module

### 🔴 Remaining Gaps
- ✅ **RAG implementation** - Addressed by ADR-012 (86% code reduction via ContentRetriever)
- ✅ **Database access** - Already implemented via ADR-002 ERPTools (36% code reduction vs planned)
- ✅ **Observability listeners** - Done v0.12.0 via AIMetricsListener
- ✅ **Guards for boundary enforcement** - Done v0.12.0 via InputGuard, OutputGuard, ExecutionGuard
- ⚠️ **Structured outputs** - Planned v0.13.0 (80% less parsing code via LangChain4j)
- ⚠️ **Temperature control** - Planned v0.13.0 (1-line quick win for deterministic function calling)
- ⚠️ **Validation loop** - Planned v0.13.0 (70% less validation code via OutputParser)
- ⏳ **Agentic patterns module** - Planned v0.15.0 (langchain4j-agentic)

**Code Reduction Achieved:**
- 630 lines (ADR-012 RAG migration)
- 260 lines (ADR-002 ERPTools vs planned custom approach)
- 150 lines (AI database access - @Tool vs manual function loop, 75% reduction)
- 3,330 lines (CLD-1601 planning docs - 90% reduction via LangChain4j adoption)
- **Total: 4,370 lines**

**Code Reduction Potential (v0.11.0+):**
- 80 lines (structured outputs - parsing code)
- 35 lines (validation loop)
- ~150 lines (observability via listeners vs custom metrics)
- **Total: ~265 additional lines**

---

## Status Legend

| Status | Meaning |
|--------|---------|
| ✅ Done | Feature is complete and tested |
| 🚧 In Progress | Currently being implemented |
| 🔜 Planned | On roadmap, starting soon |
| 🔴 Critical | High priority, near-term |
| 🟡 Medium | Medium priority |
| 🟢 Low | Low priority, future |
| ❌ Deprecated | Replaced by newer approach |

---

## iDempiere Compatibility

| iDempiere Version | Plugin Version | Bundle-Version | Status |
|-------------------|----------------|----------------|--------|
| 12.x | 0.1.0+ | 10.0.0.qualifier | ✅ Tested |
| 11.x | 0.1.0+ | 10.0.0.qualifier | ✅ Compatible |
| 10.x | 0.1.0+ | 10.0.0.qualifier | ✅ Compatible |
| 13.x | - | - | 🔜 Planned |

---

## Dependencies

### Core Dependencies (v0.9.0)

| Dependency | Version | Purpose | Size |
|------------|---------|---------|------|
| **LangChain4j Core** | 1.0.0-beta3 | Agent framework, @Tool annotations | ~1.5 MB |
| **LangChain4j Anthropic** | 1.0.0-beta3 | Claude provider integration | ~0.5 MB |
| **LangChain4j Bedrock** | 1.0.0-beta3 | AWS Bedrock provider | ~0.3 MB |
| **LangChain4j Ollama** | 1.0.0-beta3 | Local LLM support | ~0.2 MB |
| **LangChain4j OpenAI** | 1.0.0-beta3 | OpenAI/Azure integration | ~0.3 MB |
| Anthropic Java SDK | 2.10.0 | Direct Claude API (fallback) | ~0.8 MB |
| AWS SDK | 2.29.0 | Bedrock runtime | ~5.0 MB |
| Jackson | 2.17.0 | JSON processing | ~1.5 MB |
| OkHttp | 4.12.0 | HTTP client | ~0.8 MB |
| Kotlin | 1.9.10 | SDK dependency | ~2.0 MB |
| Netty | 4.1.100 | Async I/O | ~3.0 MB |

### Planned Dependencies (v0.10.0+)

| Dependency | Version | Purpose | Target Version |
|------------|---------|---------|----------------|
| **langchain4j-embeddings** | 1.0.0-beta3 | RAG support | v0.10.0 |
| **langchain4j-agentic** | 1.0.0-beta3 | Orchestration patterns | v0.11.0 |
| **langchain4j-mcp** | 1.0.0-beta3 | MCP server support | v0.10.0 |

---

## Claude Code Agents

26 specialized agents for iDempiere development:

| Category | Agents | Purpose |
|----------|--------|---------|
| **Architecture** | architecture-expert, osgi-expert, plugin-expert | System design, OSGi, plugins |
| **Data & DB** | data-expert, caching-expert, migration-generator | Data modeling, caching, migrations |
| **Development** | process-developer, model-developer, zk-ui-developer | Process/model/UI development |
| **Deployment** | docker-expert, deployment-expert, terraform-expert | Containerization, IaC |
| **Quality** | quality-expert, code-review-expert, test-generator | QA, code review, testing |
| **Integration** | webservice-expert, workflow-expert, context-agent | Web services, workflows, context |
| **Domain** | multitenancy-expert, localization-expert, reporting-expert | Multi-tenancy, i18n, reporting |
| **Utilities** | description-writer, info-window-builder | AD_Element docs, info windows |

---

## Performance Benchmarks (Target v1.0.0)

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Chat response time | < 2s | ~1.3s (with cache) | ✅ |
| Database query time | < 500ms | ~200ms | ✅ |
| API cost per session | < $0.05 | ~$0.03 | ✅ |
| Memory per session | < 50 MB | ~30 MB | ✅ |
| Concurrent users | 100+ | TBD | 🔜 |
| Uptime | 99.9% | TBD | 🔜 |

---

**Last Updated:** 2025-12-10
**Current Version:** v0.21.0 (Real-Time Streaming Improvements)
**Next Release:** v0.22.0 - Share Dialog & Advanced Sharing
