# com.cloudempiere.ai

**AI Plugin for iDempiere ERP**

[![Version](https://img.shields.io/badge/version-0.9.0-blue.svg)](CHANGELOG.md)
[![iDempiere](https://img.shields.io/badge/iDempiere-10.x%20|%2011.x%20|%2012.x-green.svg)](https://www.idempiere.org/)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](LICENSE)

---

## Vision

**Transform iDempiere into an AI-augmented ERP system** where users interact naturally with their business data through intelligent chat assistants that understand context, respect security boundaries, and provide actionable insights across all business domains.

---

## Project Goal

**Implement a comprehensive AI plugin for iDempiere** that delivers three core capabilities:

### A. System Administrator / Helpdesk Support
**Intelligent issue diagnosis and system assistance**
- Contextual understanding of reported issues
- Automated troubleshooting guidance
- System health monitoring and alerts
- Configuration assistance and best practices
- **Use Case:** Admin reports "Order posting failed for SO-12345" → AI analyzes error logs, database state, and configuration to identify root cause

### B. End User / Role-Scoped WebUI Assistant
**Natural language interface for business users**
- Contextual help within ZK UI components
- Natural language data queries and summaries
- Business process guidance
- Role-based knowledge assistance
- **Use Case:** Sales user asks "Show me my pending orders over $10k" → AI queries C_Order with user's org/role filters and formats results

### C. Model Context Protocol (MCP) Server
**Expose iDempiere capabilities as AI services**
- REST API for external AI clients (Claude Desktop, chatbots, custom frontends)
- Standardized tool definitions for iDempiere operations
- Secure multi-tenant access
- Integration with Claude Code, VS Code extensions, and web clients
- **Use Case:** Developer uses Claude Desktop to query "What's the data model for Purchase Orders?" → MCP server provides AD_Table metadata

---

## Strategic Architecture

We believe **iDempiere's strong plugin architecture** enables rapid AI integration by combining:

1. **Custom iDempiere-Specific Code** (from ADRs)
   - Multi-tenant security enforcement
   - Role-based query filtering
   - Context extraction from ZK windows
   - Audit trail integration
   - Database schema awareness (Application Dictionary)

2. **Standardized AI Frameworks** (from LangChain4j ecosystem)
   - Agent orchestration (AiServices, agentic patterns)
   - Tool discovery and execution (@Tool annotations)
   - Conversation memory management
   - RAG (Retrieval Augmented Generation)
   - Observability and cost tracking

3. **Provider SDKs** (from AI vendors)
   - Anthropic Java SDK for Claude
   - AWS SDK for Bedrock
   - LangChain4j modules for OpenAI, Ollama

**Core Principle:** *Leverage standards where they exist, customize only where iDempiere's unique requirements demand it.*

---

## Key Features

### Implemented (v0.9.0)

| Feature | Status | Description |
|---------|--------|-------------|
| **Multi-Provider Support** | ✅ | Anthropic Claude, AWS Bedrock, Ollama (local), OpenAI, iDempiere AI Hub 🚀 |
| **LangChain4j Integration** | ✅ | Native agent framework with @Tool annotations |
| **Secure Database Access** | ✅ | AI queries respect AD_Client_ID, AD_Org_ID, AD_Role_ID filters |
| **9 ERP Tools** | ✅ | Database query, record lookup, metadata, business objects |
| **Conversation Memory** | ✅ | Session-based chat history (20 messages default) |
| **Streaming Responses** | ✅ | Real-time token streaming for better UX |
| **Cost Tracking** | ✅ | Token usage and API cost estimation |
| **AI Chat Widget (ZK)** | ✅ | Interactive chat component for iDempiere WebUI |
| **Context Providers** | ✅ | Extract context from windows, tabs, charts |
| **MCP Server (REST)** | 🚧 | HTTP API layer for external AI clients (in progress) |

### Planned (Roadmap)

| Feature | Priority | Target | Description |
|---------|----------|--------|-------------|
| **RAG (Knowledge Base)** | 🔴 Critical | v0.10.0 | Semantic search over iDempiere documentation, help texts |
| **Structured Outputs** | 🔴 Critical | v0.10.0 | Reliable data extraction (OrderSummary, InventoryReport records) |
| **Agentic Workflows** | 🔴 Critical | v0.11.0 | Sequential/parallel agent orchestration (InventoryAgent, SalesAgent) |
| **Input/Output Guards** | 🟡 Medium | v0.11.0 | Boundary validation (cost limits, PII detection, SQL injection prevention) |
| **Observability Listeners** | 🟡 Medium | v0.10.0 | Real-time monitoring (latency, errors, costs) |
| **Process Execution Tool** | 🟡 Medium | v0.11.0 | AI can trigger iDempiere processes (with approval) |
| **Report Generation Tool** | 🟢 Low | v1.0.0 | AI-generated Jasper reports |
| **Multi-Agent Collaboration** | 🟢 Low | v1.0.0 | Agent-to-Agent communication (A2A pattern) |

---

## Architecture Overview

### High-Level Design

```
┌─────────────────────────────────────────────────────────────┐
│                    External AI Clients                       │
│  Claude Desktop | Web Chatbots | VS Code | Custom Frontends │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│              MCP Server (Model Context Protocol)             │
│                    REST API Gateway                          │
│         - Authentication (iDempiere users/roles)             │
│         - Tool discovery & execution                         │
│         - Multi-tenant isolation                             │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                  iDempiere ZK WebUI Layer                    │
│              AIChatWidget (ZK Component)                     │
│         - Contextual chat in windows/tabs                    │
│         - Role-scoped assistance                             │
│         - Session-based conversations                        │
└────────────────────────────┬────────────────────────────────┘
                             │
              ┌──────────────┴──────────────┐
              ▼                             ▼
┌──────────────────────────┐   ┌──────────────────────────────┐
│  IDempiereAIService      │   │  Domain-Specific Agents      │
│  (Facade)                │   │  - InventoryAgent            │
│  - Session management    │   │  - SalesAgent                │
│  - Provider routing      │   │  - PurchasingAgent           │
│  - Security context      │   │  - HelpDeskAgent             │
└──────────┬───────────────┘   └──────────────┬───────────────┘
           │                                   │
           └───────────────┬───────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              LangChain4j Agent Framework                     │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ AiServices (Agent Orchestration)                       │ │
│  │  - Automatic tool discovery & execution                │ │
│  │  - Conversation memory (per session)                   │ │
│  │  - ContentRetriever (RAG)                              │ │
│  │  - Input/Output Guards (boundary validation)           │ │
│  │  - Listeners (observability)                            │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ ChatLanguageModel (Provider Abstraction)               │ │
│  │  - AnthropicChatModel                                  │ │
│  │  - BedrockChatModel (Claude, Nova, Mistral, Llama)    │ │
│  │  - OllamaChatModel (local LLMs)                        │ │
│  │  - OpenAiChatModel                                     │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ ERPTools (@Tool Annotated)                             │ │
│  │  - queryDatabase (SELECT with role filters)            │ │
│  │  - lookupRecord (by ID)                                │ │
│  │  - searchRecords (WHERE clause)                        │ │
│  │  - getTableMetadata (AD_Table, AD_Column)              │ │
│  │  - listTables                                          │ │
│  │  - getBusinessPartner, getProduct, getOrder            │ │
│  │  - [Future] executeProcess, generateReport             │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────┬───────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│         iDempiere Security & Data Access Layer               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ SecureDatabaseQueryExecutor                            │ │
│  │  - Automatic org/client filtering (AD_Client_ID,       │ │
│  │    AD_Org_ID)                                          │ │
│  │  - Role-based access control (AD_Role_ID)              │ │
│  │  - SQL injection prevention                            │ │
│  │  - Audit logging (AIG_AgentAudit)                      │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Context Providers                                      │ │
│  │  - WindowContextProvider (current window/tab/record)   │ │
│  │  - ChartContextProvider (dashboard data)               │ │
│  │  - ApplicationDictionary (metadata extraction)         │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────┬───────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              iDempiere Database (PostgreSQL)                 │
│  - Business data (C_Order, M_Product, C_BPartner, ...)      │
│  - Application Dictionary (AD_Table, AD_Column, AD_Field)   │
│  - AI configuration (AIG_Provider, AIG_Chat, AIG_Message)   │
└─────────────────────────────────────────────────────────────┘
```

### Design Principles

1. **Context-Aware AI**: Agents understand the user's current window, selected records, and business context
2. **Security-First**: All AI operations execute within user's role permissions (no privilege escalation)
3. **Pluggable Providers**: Switch between Claude, Bedrock, Ollama, OpenAI without code changes
4. **Standard Patterns**: Use LangChain4j patterns where possible, customize only for iDempiere-specific needs
5. **Observable & Auditable**: Full logging of AI actions, costs, and decisions
6. **Scalable Architecture**: From single-user chat to multi-agent workflows

---

## Use Case Examples

### Use Case 1: End User - Sales Analysis
**Actor:** Sales Manager (WebUI)
**Context:** Viewing Sales Order window

```
User: "Show me my overdue orders with payment delays"

AI Agent (via ERPTools):
1. detectContext() → Current window: C_Order, Current org: HQ Sales
2. queryDatabase(
   "SELECT DocumentNo, C_BPartner_ID, GrandTotal, DateOrdered
    FROM C_Order
    WHERE DocStatus='CO'
      AND IsPaid='N'
      AND DateOrdered < CURRENT_DATE - 30
      AND AD_Client_ID = 11
      AND AD_Org_ID IN (50, 51, 52)"  -- User's accessible orgs
   )
3. getBusinessPartner(id) for each order
4. Format response with clickable links

Response: "You have 12 overdue orders totaling $127,450:
- SO-1234: Acme Corp, $45,000 (62 days overdue)
- SO-1289: GlobalTech, $32,100 (45 days overdue)
[See full list...]

Would you like me to generate a dunning letter for the top 5?"
```

### Use Case 2: System Administrator - Troubleshooting
**Actor:** System Administrator (HelpDesk)
**Context:** Error reported by user

```
User: "User reports 'Posting failed for invoice INV-5678'"

AI Agent (HelpDeskAgent):
1. searchRecords("C_Invoice", "DocumentNo='INV-5678'")
2. queryDatabase("SELECT * FROM Fact_Acct WHERE AD_Table_ID=318 AND Record_ID=?")
3. getTableMetadata("Fact_Acct")
4. searchRecords("AD_Error", "WHERE AD_PInstance_ID=? ORDER BY Created DESC")
5. analyzeErrorPattern()

Response: "Invoice INV-5678 posting failed due to:
- Missing GL category assignment on Product ID 1024
- Default expense account is inactive (Account 5400)

To fix:
1. Assign GL category to Product 1024
2. Re-activate Account 5400 or assign new default
3. Re-post the invoice

Root cause: Product was imported without mandatory accounting setup.

Prevention: Add validation rule to Product window requiring GL category."
```

### Use Case 3: External Client - MCP Integration
**Actor:** Developer using Claude Desktop
**Context:** Building custom integration

```
Developer (via Claude Desktop + MCP):
"What's the database schema for Purchase Orders?"

MCP Server:
1. Authenticates developer's iDempiere credentials
2. Calls ERPTools.getTableMetadata("C_Order")
   with IsSOTrx='N' filter
3. Returns structured schema

Claude Desktop receives:
{
  "tableName": "C_Order",
  "description": "Order header for purchasing",
  "columns": [
    {"name": "C_Order_ID", "type": "ID", "mandatory": true},
    {"name": "DocumentNo", "type": "String", "description": "Purchase Order Number"},
    {"name": "C_BPartner_ID", "type": "Table", "reference": "C_BPartner"},
    ...
  ],
  "keyColumns": ["C_Order_ID"],
  "parentTable": "C_DocType"
}

Developer can now write code against accurate schema without manual lookups.
```

### Use Case 4: Multi-Agent Workflow
**Actor:** Inventory Manager
**Context:** Stock replenishment planning

```
User: "Analyze inventory and create purchase orders for items below reorder point"

Orchestrator (Sequential Workflow):

Step 1: InventoryAgent
  - Query M_Storage for products with QtyOnHand < MinimumStock
  - Identify 47 products needing reorder
  - Calculate suggested quantities based on lead time + safety stock

Step 2: PurchasingAgent
  - Group products by preferred vendor (C_BPartner)
  - Calculate optimal order quantities (MOQ, price breaks)
  - Create draft purchase orders (C_Order with IsSOTrx='N')

Step 3: ApprovalAgent (if configured)
  - Check budget availability (GL_Budget)
  - Route POs over $10k to approval workflow
  - Auto-approve POs under threshold

Response: "Created 8 draft purchase orders totaling $47,300:
- PO-2401: Vendor A ($18,500) - 12 products - Pending approval
- PO-2402: Vendor B ($9,200) - 8 products - Auto-approved
- PO-2403: Vendor C ($7,100) - 5 products - Auto-approved
...

Next steps:
1. Review PO-2401 in Purchase Order window (requires manager approval)
2. Auto-approved POs will be processed in tonight's batch
3. Expected delivery: 14 days average lead time"
```

---

## Implementation Strategy

### Phase 1: Foundation (v0.9.0 - v0.10.0) ✅ IN PROGRESS

**Goal:** Solid technical foundation with LangChain4j

- ✅ Multi-provider support (Anthropic, Bedrock, Ollama, OpenAI)
- ✅ LangChain4j agent framework with @Tool annotations
- ✅ Secure database query execution with role filtering
- ✅ 9 ERP tools (database, metadata, business objects)
- ✅ Session-based conversation memory
- ✅ AI Chat Widget (ZK UI component)
- 🚧 MCP Server REST API (in progress)
- 🔜 RAG for documentation search
- 🔜 Structured outputs for data extraction
- 🔜 Observability listeners (cost, latency, errors)

### Phase 2: User-Facing Features (v0.11.0 - v0.12.0)

**Goal:** Deliver value to end users and admins

- Domain-specific agents (InventoryAgent, SalesAgent, PurchasingAgent, HelpDeskAgent)
- Agentic workflows (sequential, parallel, conditional routing)
- Process execution tool (trigger iDempiere processes)
- Report generation tool (Jasper reports)
- Advanced context extraction (form state, process parameters)
- Input/Output Guards (cost limits, PII detection, SQL injection)

### Phase 3: Production Readiness (v1.0.0)

**Goal:** Enterprise deployment

- Performance optimization (query caching, connection pooling)
- Comprehensive testing (unit, integration, security)
- Production database schema & migrations
- Documentation (user guides, admin guides, API docs)
- Monitoring & alerting (Prometheus, Grafana integration)
- Multi-agent collaboration (A2A pattern)

---

## Quick Start

### Prerequisites

**iDempiere Core Dependency:**
- **Repository**: `../iDempiereCLDE/`
- **Branch**: `iDempiereCLDE`
- **Version**: iDempiere v10 (10.0.0-SNAPSHOT)
- **Java**: Amazon Corretto 11
- **Location**: `/Users/norbertbede/github/iDempiereCLDE`

**Setup iDempiere Dependencies:**
```bash
cd ../iDempiereCLDE/org.idempiere.parent && mvn clean install -DskipTests
cd ../iDempiereCLDE/org.idempiere.p2.targetplatform && mvn clean install -DskipTests
```

**Other Requirements:**
- Maven 3.8+
- API key for AI provider (Anthropic recommended)

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/cloudempiere/com.cloudempiere.ai.git
   cd com.cloudempiere.ai
   ```

2. **Build the plugin:**
   ```bash
   mvn clean install
   ```

3. **Deploy to iDempiere:**
   - Copy `target/com.cloudempiere.ai-*.jar` to iDempiere `plugins/` directory
   - Restart iDempiere server

4. **Configure AI Provider:**
   - Navigate to **AI > AI Provider** window in iDempiere
   - Create new provider record:
     - Name: "Claude Sonnet 4"
     - Provider Type: "Anthropic Claude"
     - API Key: (your Anthropic API key)
     - Model: "claude-sonnet-4-20250514"
     - Default Max Tokens: 4096

5. **Test the Integration:**
   ```java
   // Via IDempiereAIService
   IDempiereAIService aiService = IDempiereAIService.getInstance();
   String response = aiService.chat(
       provider,
       ctx,
       "session-123",
       "Show me my pending sales orders"
   );
   ```

---

## Project Structure

```
com.cloudempiere.ai/
├── src/com/cloudempiere/ai/
│   ├── agent/
│   │   ├── langchain4j/
│   │   │   ├── LangChain4jAgent.java        # Agent implementation
│   │   │   └── LangChain4jAgentFactory.java # Agent factory
│   │   ├── BaseAgent.java                    # Legacy agent (deprecated)
│   │   └── IAIAgent.java                     # Agent interface
│   ├── context/
│   │   ├── IAIContextProvider.java           # Context provider interface
│   │   ├── AIContextProviderRegistry.java    # Context registry
│   │   └── impl/
│   │       ├── WindowContextProvider.java    # Window context extraction
│   │       └── ChartContextProvider.java     # Chart context extraction
│   ├── database/
│   │   ├── SecureDatabaseQueryExecutor.java  # Secure query execution
│   │   └── dto/
│   │       ├── SecureQueryRequest.java       # Query request DTO
│   │       └── SecureQueryResult.java        # Query result DTO
│   ├── model/
│   │   ├── I_AIG_Provider.java               # Provider interface (generated)
│   │   ├── X_AIG_Provider.java               # Provider base model (generated)
│   │   └── MAIProvider.java                  # Provider business logic
│   ├── process/
│   │   └── TestAIProvider.java               # Provider test process
│   ├── provider/
│   │   ├── IAIProvider.java                  # Provider interface (legacy)
│   │   ├── AIProviderException.java          # Provider exception
│   │   ├── dto/                              # Legacy DTOs (deprecated)
│   │   ├── factory/
│   │   │   └── AIProviderFactory.java        # Legacy factory (deprecated)
│   │   ├── impl/
│   │   │   ├── AnthropicProvider.java        # Legacy Anthropic (deprecated)
│   │   │   └── AWSBedrockProvider.java       # Legacy Bedrock (deprecated)
│   │   └── langchain4j/
│   │       ├── LangChain4jProviderFactory.java  # LangChain4j provider factory
│   │       ├── IDempiereAgent.java              # Agent interface
│   │       ├── IDempiereAIService.java          # Main AI service facade
│   │       └── ERPTools.java                    # @Tool annotated methods
│   ├── ui/
│   │   └── zkwebui/
│   │       └── AIChatWidget.java             # ZK chat component
│   └── Activator.java                        # OSGi activator
├── docs/
│   ├── adr/                                  # Architecture Decision Records
│   │   ├── 001-initial-architecture.md
│   │   ├── 002-langchain4j-strategic-adoption.md
│   │   ├── 003-mcp-server-integration.md
│   │   ├── 004-java-agent-framework.md
│   │   ├── 005-intelligent-data-source-routing.md
│   │   ├── 006-data-model-architecture.md
│   │   ├── 007-database-security-model.md
│   │   ├── 008-llm-instruction-following.md
│   │   ├── 009-domain-boundaries-agent-scope.md
│   │   ├── 010-agent-orchestration-architecture.md
│   │   └── 011-specialized-agent-scopes.md
│   ├── mcpserver/                            # MCP server documentation
│   ├── GOVERNANCE.md                         # Decision authority framework
│   ├── ROADMAP.md                            # Version phases and releases
│   └── ADR_VALIDATION_REPORT.md              # ADR validation findings
├── .claude/
│   ├── agents/                               # Claude Code agents (26)
│   ├── commands/                             # Claude Code commands
│   └── CLAUDE.md                             # Development guidelines
├── META-INF/MANIFEST.MF                      # OSGi bundle manifest
├── pom.xml                                   # Maven build configuration
├── PROJECT.md                                # This file
├── CHANGELOG.md                              # Version history
└── FEATURES.md                               # Feature matrix
```

---

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| **LangChain4j Core** | 1.0.0-beta3 | Agent framework, @Tool annotations |
| **LangChain4j Anthropic** | 1.0.0-beta3 | Claude provider integration |
| **LangChain4j Bedrock** | 1.0.0-beta3 | AWS Bedrock provider |
| **LangChain4j Ollama** | 1.0.0-beta3 | Local LLM support |
| **LangChain4j OpenAI** | 1.0.0-beta3 | OpenAI/Azure integration |
| Anthropic Java SDK | 2.10.0 | Direct Claude API client (fallback) |
| AWS SDK | 2.29.0 | Bedrock runtime |
| Jackson | 2.17.0 | JSON processing |
| OkHttp | 4.12.0 | HTTP client |
| Kotlin | 1.9.10 | SDK dependency |
| Netty | 4.1.100 | Async I/O |

**Future Dependencies (Planned):**
- `langchain4j-embeddings` - RAG support
- `langchain4j-agentic` - Advanced orchestration patterns
- `langchain4j-mcp` - MCP server support

---

## Documentation

| Document | Description |
|----------|-------------|
| [PROJECT.md](PROJECT.md) | Project overview and goals (this file) |
| [CHANGELOG.md](CHANGELOG.md) | Version history and release notes |
| [FEATURES.md](FEATURES.md) | Feature matrix and implementation status |
| [CLAUDE.md](.claude/CLAUDE.md) | Development guidelines for Claude Code |
| [GOVERNANCE.md](docs/GOVERNANCE.md) | Decision authority framework |
| [ROADMAP.md](docs/ROADMAP.md) | Version phases and releases |
| [ADR Validation Report](docs/ADR_VALIDATION_REPORT.md) | Architecture validation findings |

**Architecture Decision Records (ADRs):**

*Core Architecture:*
- [ADR-001: Initial Architecture](docs/adr/001-initial-architecture.md) ✅
- [ADR-002: LangChain4j Strategic Adoption](docs/adr/002-langchain4j-strategic-adoption.md) ✅
- [ADR-003: MCP Server Integration](docs/adr/003-mcp-server-integration.md)
- [ADR-004: Java Agent Framework Selection](docs/adr/004-java-agent-framework.md) ✅

*Data & Intelligence:*
- [ADR-005: Intelligent Data Source Routing](docs/adr/005-intelligent-data-source-routing.md) ⚠️ Superseded by ADR-012
- [ADR-006: Data Model Architecture](docs/adr/006-data-model-architecture.md) ✅
- [ADR-007: Database Security Model](docs/adr/007-database-security-model.md) ✅
- [ADR-008: LLM Instruction Following](docs/adr/008-llm-instruction-following.md) ✅
- [ADR-012: RAG-Based Context Retrieval](docs/adr/012-rag-based-context-retrieval.md) 🚧 Not implemented

*Agent Architecture:*
- [ADR-009: Domain Boundaries and Agent Scope](docs/adr/009-domain-boundaries-agent-scope.md) ⚠️ Partial (30%)
- [ADR-010: Agent Orchestration Architecture](docs/adr/010-agent-orchestration-architecture.md) ⚠️ Partial (40%)
- [ADR-011: Specialized Agent Scopes](docs/adr/011-specialized-agent-scopes.md) 🚧 Not implemented

*Operations & UX:*
- [ADR-013: Observability and Cost Tracking](docs/adr/013-observability-cost-tracking.md) 🚧 Not implemented
- [ADR-014: Guardrails and Safety](docs/adr/014-guardrails-and-safety.md) 🚧 Not implemented
- [ADR-015: Conversational UX Patterns](docs/adr/015-conversational-ux-patterns.md) 🚧 Not implemented

**Planning Documents:**
- [Priority Revision](docs/PRIORITY_REVISION_2025_12.md) - 8-week sprint plan with 25 action items
- [Code vs ADR Gap Analysis](docs/CODE_ADR_GAP_ANALYSIS.md) - 81 files vs 15 ADRs coverage matrix
- [Best Practices Reference](docs/AI_MCP_ERP_BEST_PRACTICES.md) - Industry best practices from Anthropic, BCG, MIT

**Validation Reports:**
- [Routing Validation Report](docs/ROUTING_VALIDATION_REPORT.md) - ADR-005 vs ADR-012 comparison
- [Database Access Validation Report](docs/AI_DATABASE_ACCESS_VALIDATION_REPORT.md) - 4 planning docs superseded by ADR-002 + ADR-007
- [Data Source Validation Report](docs/DATA_SOURCE_VALIDATION_REPORT.md) - 3-domain architecture validation
- [LangChain4j Integration Validation Report](docs/LANGCHAIN4J_INTEGRATION_VALIDATION_REPORT.md) - Integration plan superseded by ADR-002
- [Instruction Following Validation Report](docs/INSTRUCTION_FOLLOWING_VALIDATION_REPORT.md) - ADR-008 enhancements
- [CLD-1601 Folder Validation Report](docs/CLD_1601_VALIDATION_REPORT.md) - 8 planning docs (~8,750 lines) all superseded by ADRs

**MCP Server Documentation:**
- [MCP Index](docs/mcpserver/INDEX.md)

---

## Roadmap

### Completed (v0.1.0 - v0.9.0)

| Version | Date | Milestone |
|---------|------|-----------|
| v0.1.0 | 2025-11-18 | Initial provider infrastructure, Anthropic |
| v0.2.0 | 2025-11-18 | AWS Bedrock provider (skeleton) |
| v0.3.0 | 2025-11-20 | Security layer, context providers |
| v0.4.0 | 2025-11-26 | AI Chat widget (CLD-1606) |
| v0.5.0 | 2025-11-26 | LangChain integration |
| v0.6.0 | 2025-11-28 | LangChain4j agent framework |
| v0.7.0 | 2025-12-01 | Documentation & Claude agents |
| v0.8.0 | 2025-12-01 | MCP Server & Strategic Architecture |
| v0.9.0 | 2025-12-01 | LangChain4j native providers, ADR validation |

### In Progress (v0.10.0)

**Target:** Q1 2026 (6 weeks)

**Focus:** RAG Migration, Structured Outputs, Observability

**Phase 1: RAG Migration (ADR-012) - 2 weeks**
- [ ] Week 1: RAG Infrastructure Setup
  - [ ] Add dependencies: `langchain4j-embeddings`, `langchain4j-ollama`
  - [ ] Create `RAGContextManager` (~50 lines)
  - [ ] Setup `EmbeddingStore` and `ContentRetriever`
  - [ ] Parallel testing (RAG vs custom routing)
- [ ] Week 2: Migration & Cleanup
  - [ ] Update `AIConversationService` to use RAG
  - [ ] Remove custom routing code (630 lines)
  - [ ] Feature flag for rollback
  - [ ] Validation: >50% cache hit rate, >95% accuracy, <1.5s response

**Phase 2-4: Additional Features - 4 weeks**
- [ ] Week 3: Structured outputs (OrderSummary, InventoryReport Java records)
- [ ] Week 4: Observability listeners (TokenUsageListener, LatencyListener, AuditListener)
- [ ] Weeks 5-6: MCP REST API completion, enhanced context, query optimization

**Success Metrics:**
- Cache hit rate: 38% → 65%
- Decision accuracy: 92% → 98%
- Code reduction: 630 lines
- Response time: <1.5s

### Planned (v0.11.0 - v1.0.0)

**v0.11.0** (Q1-Q2 2026) - LangChain4j Enhancements & Domain Agents

**Phase 1: Instruction Following Enhancements (ADR-008) - 4 weeks**
- [ ] Week 1: Temperature Control (Quick Win)
  - [ ] Set `temperature(0.0)` in ChatLanguageModel builder
  - [ ] 1 line of code, 15-minute implementation
  - [ ] **Impact:** Deterministic function calling, reproducible results
- [ ] Week 2-3: Structured Outputs (High Priority)
  - [ ] Define typed interfaces (`ERPQueryResult`, `Record`) with `@Description` annotations
  - [ ] Replace manual JSON parsing with LangChain4j automatic validation
  - [ ] Type-safe AI responses with compile-time validation
  - [ ] **Impact:** 80% less parsing code (100 → 20 lines), automatic retry on invalid structure
- [ ] Week 4: Validation Loop (Medium Priority)
  - [ ] Implement `OutputParser` with `formatInstructions()`
  - [ ] Self-correcting AI (retries with enhanced prompts on validation failure)
  - [ ] **Impact:** 70% less validation code (50 → 15 lines), auto-correction of AI errors

**Expected Results (Phase 1):**
- ✅ 80% less parsing code
- ✅ Type-safe responses
- ✅ Deterministic function calling
- ✅ Auto-correction of AI errors
- ✅ 95% → 98% accuracy improvement

**Phase 2: Domain Agents & Workflows - 4 weeks**
- Domain-specific agents (InventoryAgent, SalesAgent, PurchasingAgent, HelpDeskAgent)
- Agentic workflows (sequential, parallel, conditional routing via `langchain4j-agentic`)
- Input/Output Guards (cost boundaries, PII detection, SQL injection prevention)
- Process execution tool (AI can trigger iDempiere processes with approval)
- Report generation tool (AI-generated Jasper reports)

**v0.12.0** (Q2 2026) - Production Preparation
- Production database schema & migrations
- Comprehensive testing suite
- Multi-agent collaboration (A2A pattern)
- Advanced RAG (query transformation, re-ranking)

**v1.0.0** (Q2 2026) - Production Release
- Production-ready deployment
- Complete documentation (user, admin, API)
- Monitoring & alerting integration
- Performance benchmarks
- Enterprise support

---

## Contributing

1. Follow [Conventional Commits](https://conventionalcommits.org/)
2. Update CHANGELOG.md for all changes
3. Create ADR for significant architectural decisions
4. Run tests before committing
5. Review [GOVERNANCE.md](docs/GOVERNANCE.md) for decision authority

**Commit Message Format:**
```
feat(agent): add InventoryAgent with sequential workflow
fix(security): prevent SQL injection in queryDatabase tool
docs(adr): add ADR-012 for RAG implementation
chore(deps): upgrade langchain4j to 1.0.0-beta4
```

---

## License

Proprietary - Cloudempiere Ltd.

---

## Support

- **Issues**: [GitHub Issues](https://github.com/cloudempiere/com.cloudempiere.ai/issues)
- **Documentation**: See `docs/` directory
- **Architecture Discussions**: See `docs/adr/` for decision records

---

**Last Updated:** 2025-12-01
**Current Version:** v0.9.0
**Status:** Active Development
