# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Conventional Commits](https://www.conventionalcommits.org/).

## [Unreleased]

---

## [0.1.0] - 2025-12-01

### Added

- Initial project setup as iDempiere OSGi plugin
- **Provider Architecture**: Multi-provider AI integration framework
  - `IAIProvider` interface for provider abstraction
  - `AIProviderFactory` OSGi service for dynamic provider loading
  - Provider registry with caching
- **Anthropic Claude Integration**: Full implementation
  - Claude 3 Opus, Sonnet, Haiku support
  - Claude 3.5 Sonnet support
  - Streaming text generation
  - Function calling support
  - Cost estimation with pricing
- **AWS Bedrock Integration**: Skeleton implementation
- **Database Security Layer**
  - `SecureDatabaseQueryExecutor` for AI-initiated queries
  - Role-based access control via AccessSqlParser
  - Query validation and audit logging
- **Context Providers**
  - `IAIContextProvider` interface
  - `WindowContextProvider` for iDempiere window data
  - `ChartContextProvider` for chart data extraction
- **Data Models**
  - `AIG_Provider` table for provider configuration
  - `AIG_QueryAudit` table for audit trail
- **DTOs**: AIRequest, AIResponse, AIMessage, AITokenUsage, AIFunction, etc.
- **Process**: `TestAIProvider` for provider connectivity testing
- **Documentation**: Strategic analysis documents for LangChain4j integration

---

## Version Format

- **Major** (X.0.0): Breaking changes
- **Minor** (0.X.0): New features, backward compatible
- **Patch** (0.0.X): Bug fixes, backward compatible

## Change Categories

- **Added**: New features
- **Changed**: Changes in existing functionality
- **Deprecated**: Soon-to-be removed features
- **Removed**: Removed features
- **Fixed**: Bug fixes
- **Security**: Vulnerability fixes
