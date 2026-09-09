# com.cloudempiere.ai

<!-- local-build-link -->
> **Local build setup:** see [iDempiereCLDE/LOCAL_BUILD.md](../iDempiereCLDE/LOCAL_BUILD.md) for the one-time `~/.m2/settings.xml` configuration
> that lets `mvn verify` resolve P2 artifacts from sibling local builds instead of S3.


**Modular AI Plugin Suite for iDempiere ERP**

[![Version](https://img.shields.io/badge/version-0.32.0-blue.svg)](CHANGELOG.md)
[![iDempiere](https://img.shields.io/badge/iDempiere-v10-green.svg)](https://www.idempiere.org/)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)](LICENSE)

---

## ⚠️ Important: iDempiere Dependency

**This plugin depends on the `iDempiereCLDE` project:**

- **Repository**: `cloudempiere/iDempiereCLDE`
- **Version**: iDempiere v10 (10.0.0-SNAPSHOT)
- **Java**: Amazon Corretto 11
- **Location**: `../iDempiereCLDE/`

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

Modular AI plugin suite for Cloudempiere that integrates advanced AI capabilities into iDempiere ERP. The project has been refactored from a monolithic plugin into domain-specific modules for better maintainability and flexibility.

### Module Architecture

The plugin suite consists of:

- **com.cloudempiere.ai.deps** (v0.35.0) - Shared dependencies (LangChain4j, Jackson, AWS SDK)
- **com.cloudempiere.ai.core** (v0.32.0) - Core infrastructure and provider framework
- **com.cloudempiere.ai.sales** (v0.32.0) - Sales domain AI capabilities
- **com.cloudempiere.ai.inventory** (v0.32.0) - Inventory domain AI capabilities
- **com.cloudempiere.ai.purchasing** (v0.32.0) - Purchasing domain AI capabilities
- **com.cloudempiere.ai.support** (v0.32.0) - Support ticket domain AI capabilities
- **com.cloudempiere.ai.kb** (v0.32.0) - Knowledge base domain AI capabilities
- **com.cloudempiere.ai.theme** (v10.0.2) - ZK UI theme customizations
- **com.cloudempiere.ai.feature** (v10.0.2) - Eclipse feature definition
- **com.cloudempiere.ai.p2** (v10.0.2) - P2 update site repository

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

### Architecture & Design
- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)**: Component diagrams, data flow, security layers
- **[ADRs](docs/adr/)**: Architecture Decision Records (48 decisions)
- **[DEPRECATION_ROADMAP.md](docs/DEPRECATION_ROADMAP.md)**: Migration plan for legacy code

### Project Management
- **[PROJECT.md](PROJECT.md)**: Complete project overview
- **[FEATURES.md](FEATURES.md)**: Feature matrix and capabilities
- **[CHANGELOG.md](CHANGELOG.md)**: Version history and changes
- **[ROADMAP.md](docs/ROADMAP.md)**: Version phases and milestones
- **[GOVERNANCE.md](docs/GOVERNANCE.md)**: Decision authority and conventions

### Development
- **[CLAUDE.md](.claude/CLAUDE.md)**: Development guide for Claude Code
- **[Implementation Guides](docs/guides/)**: Streaming, LangChain4j, ZK UI guides
- **[MCP Server Docs](docs/mcpserver/)**: Model Context Protocol integration

---

## Development

### Build Commands

```bash
# Clean build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Package plugin
mvn package
```

`mvn test` is **not** how you run the tests here — see [Testing](#testing).

### Testing

Tests are split across **two modules by runtime tier**, per
[ADR-020](../iDempiereCLDE/docs/adr/ADR-020-test-categorization-runtime-tiers.md). The tier is how
much has to exist before the first assertion runs, and it is a property of the *module*, not of an
annotation:

| module | tier | packaging | what launches | CI |
|---|---|---|---|---|
| `com.cloudempiere.ai.test.unit` | **U** | `jar` + maven-surefire | JVM only | runs on every build |
| `com.cloudempiere.ai.test` | **D** | `eclipse-test-plugin`, `testRuntime=p2Installed` | Equinox + p2 director + seeded DB | compiles always, runs on request |

```bash
# Everything in the tier-U module (the common case)
./run-unit-tests.sh

# One class, or a surefire pattern
./run-unit-tests.sh AIRequestTest
./run-unit-tests.sh 'Streaming*'

# Rebuild and install the host jar first — needed after any change under
# com.cloudempiere.ai.core/src, because the tier-U module resolves the host by GAV from the
# local repository, not from a reactor. This also builds ai.deps, which `-am` would miss:
# ai.core Require-Bundles it, and no pom dependency records that.
./run-unit-tests.sh --host

# What each module actually holds
./run-unit-tests.sh --list

# tier D — the fragment. Needs a target platform, a p2 runtime and a seeded database.
./run-unit-tests.sh --runtime
```

Two things to know before adding a test:

- **Tier-U tests carry no `@Tag`.** In a tier-U module the module *is* the tag. Do not add
  `@Tag("unit")` or `@UnitTest` there.
- **`skipTests` defaults to `true` in the fragment.** A green `mvn verify` on
  `com.cloudempiere.ai.test` proves it compiled, not that anything ran. Use the script.

All 14 unit-scope tests left in the fragment also carry `@Tag("needs-runtime")`. That is evidence,
not an opinion: 20 candidates were shortlisted by import scan, 6 survived a real run in the jar
module, and each of the other 14 carries its reason inline so nobody repeats the analysis. The
binding constraint is almost never the test's own imports; it is the *host* class, where one static
`CLogger` field or an `AdempiereException` in a catch clause is enough to fail class initialization
without `org.adempiere.base`.

### Project Structure

```
com.cloudempiere.ai/
├── com.cloudempiere.ai.parent/       # Maven parent POM
├── com.cloudempiere.ai.deps/         # Shared dependencies (LangChain4j 0.35.0)
│   └── lib/                          # 49 embedded JARs
├── com.cloudempiere.ai.core/         # Core infrastructure
│   ├── src/com/cloudempiere/ai/
│   │   ├── provider/                 # AI provider implementations
│   │   ├── boundary/                 # API boundary interfaces
│   │   ├── model/                    # Data models (MAIProvider)
│   │   ├── component/                # UI components (chat widget)
│   │   ├── context/                  # Context providers
│   │   ├── database/                 # Secure query executor
│   │   ├── rag/                      # RAG implementation
│   │   ├── guardrails/               # Security guardrails
│   │   └── observability/            # Monitoring & logging
│   └── lib/                          # AWS Bedrock & Netty (14 JARs)
├── com.cloudempiere.ai.sales/        # Sales domain plugin
├── com.cloudempiere.ai.inventory/    # Inventory domain plugin
├── com.cloudempiere.ai.purchasing/   # Purchasing domain plugin
├── com.cloudempiere.ai.support/      # Support domain plugin
├── com.cloudempiere.ai.kb/           # Knowledge base domain plugin
├── com.cloudempiere.ai.theme/        # ZK UI theme fragment
├── com.cloudempiere.ai.feature/      # Eclipse feature
├── com.cloudempiere.ai.p2/           # P2 update site
├── com.cloudempiere.ai.test/         # Test bundle (JUnit 5)
├── docs/                             # Documentation
├── .claude/                          # Claude Code configuration
└── pom.xml                           # Root aggregator POM
```

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
