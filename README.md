# com.cloudempiere.ai

**AI Plugin for iDempiere ERP**

[![Version](https://img.shields.io/badge/version-0.9.0-blue.svg)](CHANGELOG.md)
[![iDempiere](https://img.shields.io/badge/iDempiere-v10-green.svg)](https://www.idempiere.org/)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](LICENSE)

---

## ⚠️ Important: iDempiere Dependency

**This plugin depends on the `iDempiereCLDE` branch:**

- **Repository**: `../iDempiereCLDE/`
- **Branch**: `iDempiereCLDE`
- **Version**: iDempiere v10 (10.0.0-SNAPSHOT)
- **Java**: Amazon Corretto 11
- **Location**: `/Users/norbertbede/github/iDempiereCLDE`

**Before building this plugin:**
```bash
# Set Java 11
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home

# Build iDempiere dependencies
cd ../iDempiereCLDE/org.idempiere.parent && mvn clean install -DskipTests
cd ../iDempiereCLDE/org.idempiere.p2.targetplatform && mvn clean install -DskipTests

# Return to plugin directory
cd ../com.cloudempiere.ai
```

---

## Overview

AI plugin for Cloudempiere that integrates advanced AI capabilities into iDempiere ERP. Supports multiple AI providers (Anthropic Claude, AWS Bedrock, Ollama, OpenAI) with secure, role-based database access.

### Key Features

- **Multi-Provider Architecture**: Support for Anthropic, AWS Bedrock, Ollama, OpenAI
- **LangChain4j Integration**: Agent framework for complex AI workflows
- **Secure Database Access**: AI queries respect user roles and permissions
- **AI Chat Widget**: Interactive chat component for ZK UI
- **Context-Aware**: Extracts business context from iDempiere windows/charts
- **ERP Tools**: @Tool-annotated methods for database operations
- **Audit Trail**: Complete logging of AI-initiated actions

---

## Quick Start

### Installation

1. **Clone and setup iDempiere dependency:**
   ```bash
   cd ~/github
   git clone https://github.com/cloudempiere/iDempiere.git iDempiereCLDE
   cd iDempiereCLDE
   git checkout iDempiereCLDE

   # Build parent and target platform
   cd org.idempiere.parent && mvn clean install -DskipTests
   cd ../org.idempiere.p2.targetplatform && mvn clean install -DskipTests
   ```

2. **Clone this plugin:**
   ```bash
   cd ~/github
   git clone https://github.com/cloudempiere/com.cloudempiere.ai.git
   cd com.cloudempiere.ai
   ```

3. **Build the plugin:**
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home
   mvn clean install -DskipTests
   ```

4. **Configure AI Provider:**
   - Deploy plugin to iDempiere server
   - Configure AI Provider in System Configurator
   - Add API credentials (Anthropic, AWS, etc.)

### Configuration

Configure an AI provider in iDempiere:

1. Navigate to **System Configurator → AI Providers**
2. Create new **AIG_Provider** record:
   - **Name**: "Production Claude"
   - **Type**: Anthropic (ANT)
   - **Model**: claude-3-5-sonnet-20241022
   - **API Key**: Your Anthropic API key
   - **AI User**: Select user account for AI operations
   - **Active**: Yes

### Usage

```java
// Get AI service
IDempiereAIService aiService = IDempiereAIService.getInstance();

// Generate text
String response = aiService.chat(providerConfig, "Explain sales order 12345");

// Query database with AI
String result = aiService.queryDatabase("Show me top 10 customers by revenue");

// Get business partner info
String bpInfo = aiService.getBusinessPartner("BP001");
```

---

## Architecture

### Provider Layer

- **IAIProvider**: Provider abstraction interface (deprecated, use ChatLanguageModel)
- **LangChain4jProviderFactory**: Creates ChatLanguageModel from configuration
- **Implementations**: AnthropicProvider, AWSBedrockProvider, OllamaProvider, OpenAIProvider

### Agent Framework

- **IDempiereAgent**: AiServices interface with system prompt
- **IDempiereAIService**: Main facade for AI interactions
- **ERPTools**: @Tool-annotated methods for ERP operations

### Security Layer

- **SecureDatabaseQueryExecutor**: Role-based query execution
- **AI User Model**: Queries execute as configured AI user + caller's role
- **Audit Trail**: Complete logging in AIG_QueryAudit table

---

## Documentation

- **[PROJECT.md](PROJECT.md)**: Complete project overview
- **[FEATURES.md](FEATURES.md)**: Feature matrix and capabilities
- **[CHANGELOG.md](CHANGELOG.md)**: Version history and changes
- **[CLAUDE.md](.claude/CLAUDE.md)**: Development guide for Claude Code
- **[ADRs](docs/adr/)**: Architecture Decision Records
- **[MCP Server Docs](docs/mcpserver/)**: Model Context Protocol integration

---

## Development

### Build Commands

```bash
# Clean build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run tests (requires Eclipse environment)
mvn test

# Package plugin
mvn package
```

### Project Structure

```
com.cloudempiere.ai/
├── src/
│   ├── com/cloudempiere/ai/
│   │   ├── provider/          # AI provider implementations
│   │   ├── model/             # Data models (MAIProvider)
│   │   ├── service/           # AI services
│   │   ├── component/         # UI components (chat widget)
│   │   ├── context/           # Context providers
│   │   └── database/          # Secure query executor
│   └── test/                  # Unit tests
├── lib/                       # Embedded dependencies
├── docs/                      # Documentation
├── .claude/                   # Claude Code configuration
├── pom.xml                    # Maven configuration
└── META-INF/MANIFEST.MF       # OSGi bundle manifest
```

---

## Version History

- **v0.9.0** (2025-12-01): LangChain4j native providers
- **v0.8.0** (2025-12-01): MCP server & strategic architecture
- **v0.7.0** (2025-12-01): Documentation & Claude agents
- **v0.6.0** (2025-11-28): LangChain4j agent framework
- **v0.5.0** (2025-11-26): LangChain integration
- **v0.4.0** (2025-11-26): AI chat widget
- **v0.3.0** (2025-11-20): Security layer & context providers
- **v0.2.0** (2025-11-18): AWS Bedrock integration
- **v0.1.0** (2025-11-18): Initial provider infrastructure

See [CHANGELOG.md](CHANGELOG.md) for detailed changes.

---

## Roadmap

| Phase | Version | Status | Milestone |
|-------|---------|--------|-----------|
| Phase 9 | v0.9.0 | ✅ Released | LangChain4j Native Providers |
| Phase 10 | v0.10.0 | In Progress | Domain Agents |
| Phase 11 | v0.11.0 | Planned | Production Database Schema |
| Phase 12 | v1.0.0 | Planned | Production Release |

---

## Contributing

This is a proprietary plugin for Cloudempiere. For feature requests or bug reports, please contact the Cloudempiere team.

---

## License

Proprietary - Cloudempiere

---

## Support

- **Documentation**: See [docs/](docs/) directory
- **Issues**: Internal tracking
- **Contact**: Cloudempiere Development Team
