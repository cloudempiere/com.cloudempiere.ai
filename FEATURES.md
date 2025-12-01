# com.cloudempiere.ai Features

This document tracks features, implementation status, and version compatibility.

## Version History

| Version | Date | Key Features |
|---------|------|--------------|
| 0.1.0 | 2025-12-01 | Initial release: Provider architecture, Anthropic, security, AI Chat, LangChain |

---

## Feature Matrix

### AI Providers

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| `IAIProvider` interface | 0.1.0 | Done | Provider abstraction layer |
| `AIProviderFactory` | 0.1.0 | Done | OSGi service for provider management |
| Anthropic Claude | 0.1.0 | Done | Full Claude 3/3.5 integration |
| AWS Bedrock | 0.1.0 | Partial | Foundation models (skeleton) |
| Ollama (Local LLM) | - | Planned | Local model support |
| OpenAI | - | Planned | GPT-4 integration |

### Provider Capabilities

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| Text generation (sync) | 0.1.0 | Done | Synchronous text generation |
| Text generation (streaming) | 0.1.0 | Done | Streaming with callbacks |
| Function calling | 0.1.0 | Done | Tool use support |
| Cost estimation | 0.1.0 | Done | Token-based pricing |
| Health monitoring | 0.1.0 | Done | Provider health checks |
| Rate limit tracking | 0.1.0 | Done | API rate limit monitoring |
| Embeddings | - | Planned | Vector embeddings |
| Vision | - | Planned | Image understanding |
| Audio | - | Planned | Audio processing |

### Security & Database

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| `SecureDatabaseQueryExecutor` | 0.1.0 | Done | Secure AI query execution |
| AI User identity model | 0.1.0 | Done | Separate AI user for queries |
| Role-based access control | 0.1.0 | Done | Uses iDempiere AccessSqlParser |
| Query validation | 0.1.0 | Done | SQL injection prevention |
| Audit logging | 0.1.0 | Done | Query audit trail |

### Context Providers

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| `IAIContextProvider` | 0.1.0 | Done | Context extraction interface |
| `WindowContextProvider` | 0.1.0 | Done | iDempiere window data |
| `ChartContextProvider` | 0.1.0 | Done | Chart data extraction |
| Form context | - | Planned | Form field context |
| Process context | - | Planned | Process parameter context |

### AI Chat Widget (CLD-1606)

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| AI Chat component | 0.1.0 | Done | Interactive ZK UI widget |
| Tab context awareness | 0.1.0 | Done | Context from active tab |
| Conversation history | 0.1.0 | Done | Thread-scoped history |
| Data source routing | 0.1.0 | Done | Intelligent query routing |
| Zoom link support | 0.1.0 | Done | Enhanced UX |
| SQL clause handling | 0.1.0 | Done | Query improvements |

### LangChain Integration (CLD-1606)

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| LangChain4j framework | 0.1.0 | Done | Agent framework integration |
| ERP tool integration | 0.1.0 | Done | Tool layer for ERP operations |
| Enhanced AI context | 0.1.0 | Done | Syntax support |

### Data Models

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| `AIG_Provider` | 0.1.0 | Done | Provider configuration table |
| `AIG_QueryAudit` | 0.1.0 | Done | Query audit table |
| `AIG_Conversation` | - | Planned | Conversation history |
| `AIG_Agent` | - | Planned | Agent configuration |

### Processes

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| `TestAIProvider` | 0.1.0 | Done | Provider connectivity test |
| AI Chat | 0.1.0 | Done | Interactive chat (widget) |
| Batch AI Processing | - | Planned | Bulk AI operations |

### Agent Framework (Planned)

| Feature | Version | Status | Description |
|---------|---------|--------|-------------|
| LangChain4j agents | 0.3.0 | Planned | Domain-specific agents |
| InventoryAgent | 0.4.0 | Planned | Inventory domain agent |
| SalesAgent | 0.4.0 | Planned | Sales domain agent |
| PurchasingAgent | 0.4.0 | Planned | Purchasing domain agent |
| Domain boundaries | 0.3.0 | Planned | Security boundaries per agent |

---

## Status Legend

| Status | Meaning |
|--------|---------|
| Done | Feature is complete and tested |
| Partial | Skeleton/incomplete implementation |
| Planned | On roadmap, not started |
| Deprecated | Will be removed in future version |
| Experimental | May change without notice |

---

## iDempiere Compatibility

| iDempiere Version | Plugin Version | Bundle-Version | Status |
|-------------------|----------------|----------------|--------|
| 12.x | 0.1.0+ | 10.0.0.qualifier | Tested |
| 11.x | 0.1.0+ | 10.0.0.qualifier | Should work |
| 10.x | 0.1.0+ | 10.0.0.qualifier | Should work |
| 13.x | - | - | Planned |

---

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Anthropic Java SDK | 2.10.0 | Claude API client |
| AWS SDK | 2.29.0 | Bedrock runtime |
| LangChain4j | latest | Agent framework |
| Jackson | 2.17.0 | JSON processing |
| OkHttp | 4.12.0 | HTTP client |
| Kotlin | 1.9.10 | SDK dependency |
| Netty | 4.1.100 | Async I/O |

---

## Claude Code Agents

26 specialized agents for iDempiere development:

| Agent | Purpose |
|-------|---------|
| `idempiere-architecture-expert` | System architecture guidance |
| `idempiere-caching-expert` | Caching strategies |
| `idempiere-code-review-expert` | Code review |
| `idempiere-context-agent` | Session context management |
| `idempiere-data-expert` | Data modeling |
| `idempiere-deployment-expert` | Deployment strategies |
| `idempiere-description-writer` | AD_Element documentation |
| `idempiere-docker-expert` | Docker/containerization |
| `idempiere-info-window-builder` | Info window creation |
| `idempiere-localization-expert` | i18n/localization |
| `idempiere-migration-generator` | Migration scripts |
| `idempiere-multitenancy-expert` | Multi-tenant setup |
| `idempiere-osgi-expert` | OSGi/plugin development |
| `idempiere-plugin-expert` | Plugin architecture |
| `idempiere-process-expert` | Process development |
| `idempiere-quality-expert` | Quality assurance |
| `idempiere-reporting-expert` | Jasper/reporting |
| `idempiere-terraform-expert` | Infrastructure as code |
| `idempiere-webservice-expert` | Web services |
| `idempiere-workflow-expert` | Workflow development |
| `code-review` | General code review |
| `migration-script` | SQL migrations |
| `model-developer` | Model class development |
| `process-developer` | Process class development |
| `test-generator` | Test generation |
| `zk-ui-developer` | ZK UI development |
