# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Conventional Commits](https://conventionalcommits.org/).

## [Unreleased]

---

## [0.1.0] - 2025-12-01

Initial release of the AI Plugin for iDempiere.

### Added

#### Core Provider Infrastructure (CLD-1601)
- **Provider Architecture**
  - `IAIProvider` interface for provider abstraction
  - `AIProviderFactory` OSGi service for dynamic provider loading
  - Provider registry with caching and service ranking
- **Anthropic Claude Integration** (full implementation)
  - Claude 3 Opus, Sonnet, Haiku support
  - Claude 3.5 Sonnet support
  - Streaming text generation with callbacks
  - Function calling / tool use support
  - Cost estimation with pricing (Jan 2025 pricing)
  - Health monitoring and rate limit tracking
- **AWS Bedrock Integration** (skeleton)
  - Foundation models integration structure
  - Netty HTTP client dependencies

#### Database Security Layer (CLD-1601)
- **SecureDatabaseQueryExecutor**
  - AI User identity model for queries
  - Role-based access control via AccessSqlParser
  - Query validation (SQL injection prevention)
  - Audit logging with `SecureQueryAudit`
- **Data Models**
  - `AIG_Provider` table for provider configuration
  - `AIG_QueryAudit` table for audit trail
  - `I_AIG_Provider`, `X_AIG_Provider`, `MAIProvider` classes

#### Context Providers (CLD-1601)
- `IAIContextProvider` interface
- `WindowContextProvider` for iDempiere window data extraction
- `ChartContextProvider` for chart data extraction
- `AIContextProviderRegistry` for provider management

#### AI Chat Widget (CLD-1606)
- Interactive AI chat component for ZK UI
- Tab context awareness
- Conversation history (thread-scoped)
- Intelligent data source routing
- Zoom link support for enhanced UX
- SQL clause handling improvements

#### LangChain Integration (CLD-1606)
- LangChain4j agent framework integration
- ERP tool integration layer
- Enhanced AI context with syntax support

#### Processes
- `TestAIProvider` for provider connectivity testing

#### DTOs
- `AIRequest`, `AIResponse`, `AIMessage`
- `AITokenUsage`, `AIModelCapabilities`
- `AIFunction`, `AIFunctionCall`
- `AIHealthStatus`, `AIRateLimitStatus`
- `AIStreamCallback`

#### Documentation
- MCP server implementation guide
- Strategic analysis documents for LangChain4j
- Architecture Decision Records (ADR-001)
- 26 Claude Code agents for iDempiere development

### Technical Details

**Commits included:**
- `6ccc13b` Initial plugin setup
- `dfd2182` AI provider implementation
- `98c7db2` Anthropic provider refactoring
- `b98f7d4` AWS Bedrock provider
- `7609037` Netty dependencies for AWS SDK
- `8a01cbe` Context provider infrastructure
- `c428471` Secure database query executor
- `ccce013` AI user identity model
- `9666cbc` AI chat widget
- `01960c2` LangChain framework integration
- `b39939e` LangChain4j agent framework
- `dba2c00` Claude agents and documentation

---

## Version Format

- **Major** (X.0.0): Breaking changes to provider API
- **Minor** (0.X.0): New features, backward compatible
- **Patch** (0.0.X): Bug fixes, backward compatible

## Change Categories

- **Added**: New features
- **Changed**: Changes in existing functionality
- **Deprecated**: Soon-to-be removed features
- **Removed**: Removed features
- **Fixed**: Bug fixes
- **Security**: Vulnerability fixes

## iDempiere Version Compatibility

| Plugin Version | iDempiere Version | Bundle-Version |
|----------------|-------------------|----------------|
| 0.1.0 | 10.x, 11.x, 12.x | 10.0.0.qualifier |
