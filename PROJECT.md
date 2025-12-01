# com.cloudempiere.ai

**AI Plugin for iDempiere ERP**

[![Version](https://img.shields.io/badge/version-0.8.0-blue.svg)](CHANGELOG.md)
[![iDempiere](https://img.shields.io/badge/iDempiere-10.x%20|%2011.x%20|%2012.x-green.svg)](https://www.idempiere.org/)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](LICENSE)

---

## Vision

**Empower iDempiere users with intelligent AI assistants** that understand ERP context, respect security boundaries, and augment human decision-making across all business domains.

---

## Goals

1. **Seamless Integration**: AI capabilities that feel native to iDempiere, not bolted on
2. **Security First**: AI operates within user's role permissions - no privilege escalation
3. **Multi-Provider**: Freedom to choose AI providers (cloud or local) based on requirements
4. **Domain Intelligence**: Specialized agents for Inventory, Sales, Purchasing, Finance
5. **Actionable Insights**: AI that doesn't just answer questions but suggests actions
6. **Audit Trail**: Full transparency of AI-initiated queries and recommendations

---

## Scope

### In Scope

| Area | Description |
|------|-------------|
| **AI Providers** | Anthropic Claude, AWS Bedrock, Ollama (local), OpenAI |
| **Chat Interface** | Interactive AI chat widget in ZK UI |
| **Context Awareness** | Window, tab, chart, process context extraction |
| **Secure Queries** | AI-initiated database queries with role-based access |
| **Agent Framework** | LangChain4j-based domain-specific agents |
| **Tool Integration** | AI tools for ERP operations (lookup, create, update) |

### Out of Scope

| Area | Reason |
|------|--------|
| Autonomous transactions | AI recommends, human approves |
| Training/fine-tuning | Use pre-trained models via API |
| Real-time streaming data | Batch/on-demand processing only |
| External system integration | Focus on iDempiere core |

---

## Overview

This iDempiere plugin integrates AI capabilities into the CloudEmpiere enterprise platform. It provides a multi-provider architecture supporting external AI APIs (Anthropic Claude, AWS Bedrock) and local LLMs (Ollama) with secure database query execution.

## Key Features

- **Multi-Provider Architecture**: Extensible AI provider framework
- **Anthropic Claude**: Full integration with Claude 3/3.5 models
- **AWS Bedrock**: Foundation models integration (skeleton)
- **Secure Database Access**: AI-initiated queries with role-based access control
- **AI Chat Widget**: Interactive chat component for ZK UI
- **LangChain4j Integration**: Agent framework for complex AI workflows
- **Context Providers**: Extract business context from iDempiere windows/charts
- **MCP Server**: Model Context Protocol for external AI tool integration
- **26 Claude Code Agents**: Specialized development assistants

---

## Quick Start

### Prerequisites

- iDempiere 10.x, 11.x, or 12.x
- Java 11+
- Maven 3.8+
- API key for AI provider (Anthropic, AWS, etc.)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/cloudempiere/com.cloudempiere.ai.git
   ```

2. Build the plugin:
   ```bash
   mvn clean install
   ```

3. Deploy to iDempiere:
   - Copy `target/com.cloudempiere.ai-*.jar` to `plugins/` directory
   - Restart iDempiere server

4. Configure provider:
   - Navigate to **AI > AI Provider** window
   - Create new provider record with API credentials

### Usage

```java
// Get provider factory
IAIProviderFactory factory = ... // OSGi service lookup

// Create AI request
AIRequest request = AIRequest.builder()
    .message("Analyze this sales order")
    .model("claude-3-5-sonnet")
    .maxTokens(1024)
    .build();

// Get provider and generate response
IAIProvider provider = factory.getProvider(providerId);
AIResponse response = provider.generateText(request);
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [CHANGELOG.md](CHANGELOG.md) | Version history and release notes |
| [FEATURES.md](FEATURES.md) | Feature matrix and implementation status |
| [GOVERNANCE.md](docs/GOVERNANCE.md) | Project governance and workflows |
| [CLAUDE.md](.claude/CLAUDE.md) | Development guidelines for Claude Code |
| [ADR-001](docs/adr/001-initial-architecture.md) | Initial architecture decisions |
| [ADR-002](docs/adr/002-langchain4j-strategic-adoption.md) | LangChain4j strategic adoption |
| [ADR-003](docs/adr/003-mcp-server-integration.md) | MCP server integration |
| [MCP Server Docs](docs/mcpserver/INDEX.md) | MCP implementation guides |

---

## Roadmap

### Completed Phases (v0.1.0 - v0.8.0)

| Phase | Version | Date | Milestone |
|-------|---------|------|-----------|
| 1 | v0.1.0 | 2025-11-18 | Initial provider infrastructure, Anthropic |
| 2 | v0.2.0 | 2025-11-18 | AWS Bedrock provider (skeleton) |
| 3 | v0.3.0 | 2025-11-20 | Security layer, context providers |
| 4 | v0.4.0 | 2025-11-26 | AI Chat widget (CLD-1606) |
| 5 | v0.5.0 | 2025-11-26 | LangChain integration |
| 6 | v0.6.0 | 2025-11-28 | LangChain4j agent framework |
| 7 | v0.7.0 | 2025-12-01 | Documentation & Claude agents |
| 8 | v0.8.0 | 2025-12-01 | MCP Server & Strategic Architecture |

### Upcoming Phases

| Phase | Version | Target | Key Features |
|-------|---------|--------|--------------|
| 9 | v0.9.0 | Q1 2026 | LangChain4j Native Providers, HTTP API layer |
| 10 | v0.10.0 | Q1 2026 | Domain agents (Inventory, Sales, Purchasing) |
| 11 | v0.11.0 | Q2 2026 | Production database schema, migrations |
| 12 | v1.0.0 | Q2 2026 | Production release |

---

## Project Structure

```
com.cloudempiere.ai/
├── src/com/cloudempiere/ai/
│   ├── context/           # Context providers
│   ├── database/          # Secure query execution
│   ├── model/             # Data models (AIG_Provider)
│   ├── process/           # iDempiere processes
│   ├── provider/          # AI provider framework
│   │   ├── dto/           # Data transfer objects
│   │   ├── factory/       # Provider factory
│   │   └── impl/          # Provider implementations
│   └── Activator.java     # OSGi activator
├── docs/
│   ├── adr/               # Architecture Decision Records
│   ├── mcpserver/         # MCP server documentation
│   └── GOVERNANCE.md      # Project governance
├── .claude/
│   ├── agents/            # Claude Code agents (26)
│   └── commands/          # Claude Code commands
├── META-INF/MANIFEST.MF   # OSGi bundle manifest
└── pom.xml                # Maven build configuration
```

---

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Anthropic Java SDK | 2.10.0 | Claude API client |
| AWS SDK | 2.29.0 | Bedrock runtime |
| LangChain4j | 1.0.0-beta3 | Agent framework |
| Jackson | 2.17.0 | JSON processing |
| OkHttp | 4.12.0 | HTTP client |
| Kotlin | 1.9.10 | SDK dependency |
| Netty | 4.1.100 | Async I/O |

---

## Contributing

1. Follow [Conventional Commits](https://conventionalcommits.org/)
2. Update CHANGELOG.md for all changes
3. Create ADR for significant decisions
4. Run tests before committing

See [GOVERNANCE.md](docs/GOVERNANCE.md) for detailed workflows.

---

## Support

- **Issues**: [GitHub Issues](https://github.com/cloudempiere/com.cloudempiere.ai/issues)
- **Documentation**: See `docs/` directory

---

## License

Proprietary - CloudEmpiere Ltd.
