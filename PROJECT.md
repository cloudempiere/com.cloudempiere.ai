# com.cloudempiere.ai

**AI Plugin for iDempiere ERP**

[![Version](https://img.shields.io/badge/version-0.7.0-blue.svg)](CHANGELOG.md)
[![iDempiere](https://img.shields.io/badge/iDempiere-10.x%20|%2011.x%20|%2012.x-green.svg)](https://www.idempiere.org/)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](LICENSE)

## Overview

This iDempiere plugin integrates AI capabilities into the CloudEmpiere enterprise platform. It provides a multi-provider architecture supporting external AI APIs (Anthropic Claude, AWS Bedrock) and local LLMs (Ollama) with secure database query execution.

## Features

- **Multi-Provider Architecture**: Extensible AI provider framework
- **Anthropic Claude**: Full integration with Claude 3/3.5 models
- **AWS Bedrock**: Foundation models integration (in progress)
- **Secure Database Access**: AI-initiated queries with role-based access control
- **AI Chat Widget**: Interactive chat component for ZK UI
- **LangChain4j Integration**: Agent framework for complex AI workflows
- **Context Providers**: Extract business context from iDempiere windows/charts

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

## Documentation

| Document | Description |
|----------|-------------|
| [CHANGELOG.md](CHANGELOG.md) | Version history and release notes |
| [FEATURES.md](FEATURES.md) | Feature matrix and implementation status |
| [CLAUDE.md](.claude/CLAUDE.md) | Development guidelines for Claude Code |
| [ADR-001](docs/adr/001-initial-architecture.md) | Initial architecture decisions |

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
│   └── mcpserver/         # MCP server documentation
├── .claude/
│   ├── agents/            # Claude Code agents (26)
│   └── commands/          # Claude Code commands
├── META-INF/MANIFEST.MF   # OSGi bundle manifest
└── pom.xml                # Maven build configuration
```

## Roadmap

| Phase | Version | Target | Status | Key Features |
|-------|---------|--------|--------|--------------|
| 1 | v0.1.0 | 2025-12-01 | Done | Provider architecture, Anthropic, security |
| 2 | v0.2.0 | Q1 2026 | Planned | AWS Bedrock completion, Ollama |
| 3 | v0.3.0 | Q1 2026 | Planned | LangChain4j agents, domain boundaries |
| 4 | v0.4.0 | Q1 2026 | Planned | Inventory, Sales, Purchasing agents |
| 5 | v0.5.0 | Q2 2026 | Planned | Production database schema |
| 6 | v0.6.0 | Q2 2026 | Planned | Testing, documentation |
| 7 | v1.0.0 | Q2 2026 | Planned | Production release |

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

## Contributing

1. Follow [Conventional Commits](https://conventionalcommits.org/)
2. Update CHANGELOG.md for all changes
3. Create ADR for significant decisions
4. Run tests before committing

## Support

- **Issues**: [GitHub Issues](https://github.com/cloudempiere/com.cloudempiere.ai/issues)
- **Documentation**: See `docs/` directory

## License

Proprietary - CloudEmpiere Ltd.
