# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**com.cloudempiere.ai** is an iDempiere ERP plugin that integrates AI capabilities into the CloudEmpiere enterprise platform. It implements a multi-provider architecture supporting both external AI APIs (Anthropic Claude, AWS Bedrock) and local LLMs (Ollama) with secure database query execution.

**Project Type**: Eclipse Plugin / Maven OSGi Bundle
**Language**: Java (33 source files)
**Build System**: Maven with PDE (Tycho) integration

## Dependencies

### iDempiere Core Dependency

**IMPORTANT:** This plugin depends on the **iDempiereCLDE branch** of the iDempiere project:

- **Repository**: `../iDempiereCLDE/` (relative path from plugin root)
- **Branch**: `iDempiereCLDE`
- **Version**: iDempiere v10 (10.0.0-SNAPSHOT)
- **Java Version**: Amazon Corretto 11
- **Location**: `/Users/norbertbede/github/iDempiereCLDE`

Before building or testing this plugin, ensure:
1. iDempiereCLDE repository is cloned at `../iDempiereCLDE/`
2. The iDempiereCLDE branch is checked out
3. Java 11 (Corretto) is being used: `JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home`
4. iDempiere parent and target platform are built:
   ```bash
   cd ../iDempiereCLDE/org.idempiere.parent && mvn clean install -DskipTests
   cd ../iDempiereCLDE/org.idempiere.p2.targetplatform && mvn clean install -DskipTests
   ```

The plugin references the iDempiere parent POM at `../iDempiereCLDE/org.idempiere.parent/pom.xml` (see pom.xml line 10).

## Build and Development Commands

### Maven Build Commands

```bash
# Clean build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Skip dependency copy (faster for iterative dev)
mvn clean compile -DskipTests

# Run tests only
mvn test

# Run a specific test class
mvn test -Dtest=AnthropicProviderTest

# Package as OSGi plugin
mvn package
```

### Key Maven Phases

- **validate**: Copies dependencies (Anthropic SDK, AWS Bedrock, Jackson, Kotlin, Netty, OkHttp)
- **compile**: Compiles source to `target/classes/`
- **package**: Creates OSGi plugin JAR with embedded libraries

### IDE Setup

- **Eclipse**: Import as "Existing Maven Projects" - pom.xml has Maven and PDE integration
- **Build Spec**: Includes Eclipse JDT, PDE ManifestBuilder, SchemaBuilder, and Maven2Builder
- **Output Directory**: `target/classes/`

## Project Architecture

### High-Level Design

The plugin implements a **Provider Pattern** with **Factory Pattern** for extensible AI provider integration:

```
AIRequest/AIResponse (DTOs)
    ↓
IAIProvider (Interface)
    ↓
    ├── AnthropicProvider (Claude API)
    ├── AWSBedrockProvider (AWS Foundation Models)
    └── [Other Providers]
    ↓
AIProviderFactory (OSGi Service, @Component)
    ↓
Uses: MAIProvider (Database Model)
```

### Core Components

#### 1. **Provider Layer** (`src/com/cloudempiere/ai/provider/`)
- **IAIProvider**: Main interface defining provider contract
  - Text generation (sync/streaming)
  - Embeddings
  - Vision and audio support
  - Health monitoring and cost estimation
  - Lifecycle management

- **Implementations** (`impl/`):
  - **AnthropicProvider**: Full Claude integration (3-Opus, 3-Sonnet, 3-Haiku, 3.5-Sonnet)
    - Uses official Anthropic Java SDK v2.10.0
    - Streaming support with callbacks
    - Function calling support
    - Cost estimation (pricing updated Jan 2025)
  - **AWSBedrockProvider**: AWS Foundation Models integration

- **Factory** (`factory/`):
  - **AIProviderFactory**: OSGi service that dynamically loads providers
    - Registry: Maps AIGProviderType → Implementation class
    - Caching: Instances cached by provider ID
    - Service Ranking: 100 (higher priority)
    - Immediate initialization on bundle start

- **DTOs** (`dto/`):
  - **AIRequest**: Request wrapper (Builder pattern)
    - Message history, model, temperature, maxTokens, topP
    - Provider-specific parameters map
    - iDempiere context map
  - **AIResponse**: Response with tokens, cost, function calls
  - **AIMessage**: Conversation message (role, content, function calls)
  - **AITokenUsage**: Token tracking (input, output, total)
  - **AIModelCapabilities**: Feature flags per model
  - **AIHealthStatus**: Health monitoring
  - **AIRateLimitStatus**: Rate limit tracking
  - **AIFunction** / **AIFunctionCall**: Tool use
  - **AIStreamCallback**: Streaming interface

- **Exception**: **AIProviderException** with error codes and retryable flag

#### 2. **Database & Security** (`src/com/cloudempiere/ai/database/`)
- **SecureDatabaseQueryExecutor**: Executes AI-requested queries with security
  - **Security Model**: Query executes as AI User (from AIG_Provider.AD_User_ID) + Logged-in User's Role
  - **Principle**: If user's role cannot access a table/column/record, AI cannot either
  - **Audit Trail**: Records both AI user and initiating user
  - Uses iDempiere's **AccessSqlParser** for role-based permission checks
  - Query validation prevents injection attacks

- **DTOs**:
  - **SecureQueryRequest**: Query + context info
  - **SecureQueryResult**: Result set as JSON + metadata
  - **SecureQueryAudit**: Audit log records

#### 3. **Context Providers** (`src/com/cloudempiere/ai/context/`)
- **IAIContextProvider**: Interface for context extraction
  - **WindowContextProvider**: Extracts context from iDempiere windows
  - **ChartContextProvider**: Extracts chart data and metadata
- **AIContextProviderRegistry**: Registry for context providers
- **ContextParameters**: Parameters for context extraction

#### 4. **Data Models** (`src/com/cloudempiere/ai/model/`)
- **I_AIG_Provider**: Interface (generated from AD_Table)
- **X_AIG_Provider**: Base model (generated from AD_Table)
- **MAIProvider**: Business logic model
  - Wrapper around AIG_Provider table
  - Manages provider credentials and configuration
  - Supports multiple providers in single instance

#### 5. **Processes** (`src/com/cloudempiere/ai/process/`)
- **TestAIProvider**: iDempiere process for testing provider connectivity
  - Checks health status
  - Verifies credentials
  - Lists available models

#### 6. **OSGi Integration**
- **Activator.java**: Bundle lifecycle management
- **OSGI-INF/com.cloudempiere.ai.provider.factory.AIProviderFactory.xml**: Service declaration

### Package Structure

```
src/com/cloudempiere/ai/
├── context/
│   ├── IAIContextProvider.java
│   ├── AIContextProviderRegistry.java
│   ├── ContextParameters.java
│   └── impl/
│       ├── WindowContextProvider.java
│       └── ChartContextProvider.java
├── database/
│   ├── SecureDatabaseQueryExecutor.java
│   └── dto/
│       ├── SecureQueryRequest.java
│       ├── SecureQueryResult.java
│       └── SecureQueryAudit.java
├── model/
│   ├── I_AIG_Provider.java
│   ├── X_AIG_Provider.java
│   └── MAIProvider.java
├── process/
│   └── TestAIProvider.java
├── provider/
│   ├── IAIProvider.java
│   ├── AIProviderException.java
│   ├── dto/
│   │   ├── AIFunction.java
│   │   ├── AIFunctionCall.java
│   │   ├── AIHealthStatus.java
│   │   ├── AIMessage.java
│   │   ├── AIModelCapabilities.java
│   │   ├── AIRateLimitStatus.java
│   │   ├── AIRequest.java
│   │   ├── AIResponse.java
│   │   ├── AIStreamCallback.java
│   │   └── AITokenUsage.java
│   ├── factory/
│   │   ├── IAIProviderFactory.java
│   │   └── AIProviderFactory.java
│   ├── impl/
│   │   ├── AnthropicProvider.java
│   │   └── AWSBedrockProvider.java
│   └── test/
│       └── AnthropicProviderTest.java
├── Activator.java
└── ...
```

## Key Dependencies

### Direct Dependencies (Maven)
- **Anthropic Java SDK v2.10.0**: Official Claude API client
- **AWS SDK v2.29.0**: Bedrock runtime for AWS Foundation Models
- **Jackson v2.17.0**: JSON serialization/deserialization
- **Kotlin v1.9.10**: Dependency of Anthropic SDK
- **OkHttp v4.12.0**: HTTP client for Anthropic SDK
- **Netty v4.1.100**: Async I/O for AWS SDK
- **iDempiere Core**: Compiere framework (OSGi, database access, logging)

### Embedded in Plugin (via build.properties)
All Maven dependencies are copied to `lib/` directory and included in the OSGi plugin JAR.

## Security Considerations

### Critical: AI User Identity Model
- Each AIG_Provider has an associated AD_User_ID (AI user account)
- Database queries execute as this AI user + caller's role
- This provides:
  - Clear audit trail (who initiated vs. who executed)
  - Role-based access control enforcement
  - Prevention of privilege escalation

### Query Execution Security
- All database queries go through **SecureDatabaseQueryExecutor**
- Uses iDempiere's **AccessSqlParser** to enforce role-based permissions
- Query validation prevents SQL injection
- Audit logging of all AI-initiated queries

### Credential Management
- Provider credentials stored encrypted in database (AIG_Provider table)
- Never log or expose API keys
- Use environment variables for testing (not committed to repo)

## Testing

### Existing Tests
- **AnthropicProviderTest**: Tests Anthropic provider integration
  - Health checks
  - Text generation (sync and streaming)
  - Function calling
  - Cost estimation
- **TestAIProvider**: iDempiere process test

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=AnthropicProviderTest

# Skip tests during build
mvn install -DskipTests
```

### Important: Environment Setup for Tests
- Set `ANTHROPIC_API_KEY` environment variable for real API testing
- For CI/CD, use mock providers or test API keys with rate limits

## Documentation

### In-Repository Documentation
- **CLAUDE/CLD-1601/**: Implementation planning and tracking
  - **IMPLEMENTATION_STATUS.md**: Phase completion status
  - **COMPREHENSIVE_IMPLEMENTATION_PLAN.md**: Full architectural plan with 11 use cases
  - **AI_PROVIDER_IMPLEMENTATION_PLAN.md**: Provider-specific implementation details
  - **AI_DATA_MODEL_ARCHITECTURE.md**: Database schema with ERD and materialized views

### External Documentation
- **Anthropic API**: https://docs.anthropic.com/claude/reference
- **iDempiere Plugin Development**: https://wiki.idempiere.org/

## Common Development Tasks

### Adding a New AI Provider

1. **Create Provider Class** in `src/com/cloudempiere/ai/provider/impl/`:
   ```java
   public class MyProvider implements IAIProvider {
       @Override public AIResponse generateText(AIRequest request) throws AIProviderException { }
       // Implement other interface methods
   }
   ```

2. **Register in AIProviderFactory**:
   - Add entry to provider registry map in factory constructor
   - Factory will dynamically instantiate based on AIGProviderType

3. **Add Model Pricing** if applicable:
   - Similar to AnthropicProvider's MODEL_PRICING map
   - Used for cost estimation in AIResponse

4. **Testing**:
   - Create test class similar to AnthropicProviderTest
   - Test health checks, text generation, streaming
   - Test error handling for API rate limits and timeouts

### Extending Context Providers

1. **Implement IAIContextProvider** in `context/impl/`:
   - Extract relevant business data for AI consumption
   - Return structured context with metadata

2. **Register in AIContextProviderRegistry**:
   - Map context type to provider implementation

### Database Query Integration

- Use **SecureDatabaseQueryExecutor** for all AI-initiated queries
- Call `SecureDatabaseQueryExecutor.executeSecureQuery(request)`
- Results returned as JSON (SecureQueryResult)
- Audit trail automatically recorded

## Recent Development

### Latest Implementation (CLD-1601)
- ✅ AI user identity model for secure database access
- ✅ Complete provider infrastructure (14 classes)
- ✅ Anthropic Claude integration (full implementation)
- ✅ AWS Bedrock integration (skeleton)
- ✅ Database security layer with audit logging
- ✅ Context provider system for window/chart data

## Upcoming Phases

| Phase | Version | Target | Status | Key Features |
|-------|---------|--------|--------|--------------|
| Phase 1 | v0.1.0 | 2025-12-01 | ✅ Done | Provider architecture, Anthropic integration, security layer |
| Phase 2 | v0.2.0 | Q1 2026 | Planned | AWS Bedrock completion, Ollama integration |
| Phase 3 | v0.3.0 | Q1 2026 | Planned | LangChain4j agent framework, domain boundaries |
| Phase 4 | v0.4.0 | Q1 2026 | Planned | InventoryAgent, SalesAgent, PurchasingAgent |
| Phase 5 | v0.5.0 | Q2 2026 | Planned | Production database schema, migrations |
| Phase 6 | v0.6.0 | Q2 2026 | Planned | Comprehensive testing, documentation |
| Phase 7 | v1.0.0 | Q2 2026 | Planned | Production release |

## Commit Convention

Follow [Conventional Commits](https://conventionalcommits.org/):
- `feat:` new feature
- `fix:` bug fix
- `docs:` documentation
- `refactor:` code refactor
- `chore:` maintenance
- `test:` adding tests
- `perf:` performance improvement

**Always update:**
1. `CHANGELOG.md` - Document changes
2. `FEATURES.md` - Update feature matrix

**Release workflow:**
1. Update version in `pom.xml`, `MANIFEST.MF`
2. Update `CHANGELOG.md` with release date
3. Commit and create tag: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`
4. Bump to next SNAPSHOT version

## Architecture Decision Records

### Core Architecture
- [ADR-001](docs/adr/001-initial-architecture.md) - Initial architecture and project standards
- [ADR-002](docs/adr/002-langchain4j-strategic-adoption.md) - LangChain4j Strategic Adoption
- [ADR-003](docs/adr/003-mcp-server-integration.md) - MCP Server Integration
- [ADR-004](docs/adr/004-java-agent-framework.md) - Java Agent Framework Selection (LangChain4j)
- [ADR-027](docs/adr/027-implementation-roadmap-priority.md) - Implementation Roadmap and Priority Matrix

### Data & Intelligence
- [ADR-005](docs/adr/005-intelligent-data-source-routing.md) - Intelligent Data Source Routing (Superseded by ADR-012)
- [ADR-006](docs/adr/006-data-model-architecture.md) - Data Model Architecture
- [ADR-007](docs/adr/007-database-security-model.md) - Database Security Model
- [ADR-008](docs/adr/008-llm-instruction-following.md) - LLM Instruction Following Strategy
- [ADR-012](docs/adr/012-rag-based-context-retrieval.md) - RAG-Based Context Retrieval
- [ADR-026](docs/adr/026-vector-database-strategy.md) - Vector Database Strategy (AWS pgvector vs alternatives)

### Agent Architecture
- [ADR-009](docs/adr/009-domain-boundaries-agent-scope.md) - Domain Boundaries and Agent Scope Architecture
- [ADR-010](docs/adr/010-agent-orchestration-architecture.md) - Agent Orchestration Architecture
- [ADR-011](docs/adr/011-specialized-agent-scopes.md) - Specialized Agent Scopes by Business Domain
- [ADR-016](docs/adr/016-knowledge-base-agent.md) - Knowledge Base Agent Domain

### Operations & UX
- [ADR-013](docs/adr/013-observability-cost-tracking.md) - Observability and Cost Tracking
- [ADR-014](docs/adr/014-guardrails-and-safety.md) - Guardrails and Safety
- [ADR-015](docs/adr/015-conversational-ux-patterns.md) - Conversational UX Patterns
- [ADR-031](docs/adr/031-chat-panel-langchain4j-chatmodel-integration.md) - Chat Panel LangChain4j ChatModel Integration
- [ADR-032](docs/adr/032-testing-strategy.md) - Testing Strategy
- [ADR-033](docs/adr/033-streaming-thinking-timeline-ux.md) - Streaming Responses and Thinking Timeline UX

### Use Cases - Phase 1 (MVP)
- [ADR-017](docs/adr/017-chart-executive-overview.md) - Chart Executive Overview
- [ADR-018](docs/adr/018-sales-opportunity-summary.md) - Sales Opportunity Summary
- [ADR-019](docs/adr/019-support-ticket-classification.md) - Support Ticket Classification
- [ADR-020](docs/adr/020-email-gateway-enhancement.md) - Email Gateway Enhancement

### Use Cases - Phase 2
- [ADR-021](docs/adr/021-product-catalog-enhancement.md) - Product Catalog Enhancement
- [ADR-022](docs/adr/022-translation-wizard.md) - Translation Wizard

### Use Cases - Phase 3
- [ADR-023](docs/adr/023-ocr-invoice-processing.md) - OCR Invoice Processing
- [ADR-024](docs/adr/024-import-data-normalization.md) - Import Data Normalization
- [ADR-025](docs/adr/025-idempiere-development-assistant.md) - iDempiere Development Assistant

## Notes

- The plugin follows iDempiere conventions for model classes (I_*, X_*, M*)
- OSGi services use @Component annotations for lifecycle management
- Logging uses iDempiere's CLogger (based on java.util.logging)
- Database queries use iDempiere's DB class and PreparedStatements
- All external API calls include proper error handling and retryability
