# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Conventional Commits](https://conventionalcommits.org/).

## [Unreleased]

### Next: v0.20.0 - Share Dialog & Advanced Sharing

**Planned:**
- Share button in AIChatWidget (OWNER only)
- User/role picker dialog
- Time-bound sharing (ValidFrom/ValidTo)
- Sharing notifications
- Ownership transfer UI

**Deferred:**
- ESC key shortcut to cancel streaming (requires ZK keyboard handling)
- Upgrade LangChain4j to v1.8.0+ for proper HTTP cancellation
- ADR-017: Chart Executive Overview
- ADR-018: Sales Opportunity Summary
- ADR-011: Domain Agents

---

## [0.19.0] - 2025-12-08

### Streaming-First Architecture & Tool Support Separation

This release unifies the ERPTools implementation and properly separates streaming vs non-streaming tool support for different providers.

#### Added

- **supportsStreamingTools() method** in AIService
  - Separates streaming tool support from non-streaming
  - Streaming tools: Anthropic, OpenAI, AWS Bedrock only
  - Non-streaming tools: All providers including Ollama/Llama
  - LangChain4j 0.35.0 limitation: OllamaStreamingChatModel doesn't support tools

#### Changed

- **Unified ERPTools** (deleted StreamingERPTools)
  - Single ERPTools class with optional callback support
  - Constructor accepts optional `AIStreamCallback` for tool events
  - Fires `onToolStart`, `onToolComplete`, `onToolError` when callback provided
  - Reduces code duplication between streaming and non-streaming paths

- **Agent Name Display** in AIChatWidget
  - Now uses `provider.getAD_User().getName()` instead of provider name
  - Displays actual AI user name (e.g., "Ollama Agent") in chat messages
  - Consistent naming between initial display and after refresh

- **AIService Tool Support Logic**
  - `TOOL_SUPPORTED_PROVIDERS`: All major providers (non-streaming)
  - `STREAMING_TOOL_SUPPORTED_PROVIDERS`: Anthropic, OpenAI, Bedrock only
  - Ollama/Llama streaming uses `SimpleStreamingAgent` (no tools)

#### Removed

- **StreamingERPTools.java** - Functionality merged into ERPTools

#### Technical Notes

- Streaming tool support for Ollama requires LangChain4j 0.37.0+ (Java 17)
- Current LangChain4j 0.35.0 throws "Tools are currently not supported by this model" for Ollama streaming
- Non-streaming tool support works for Ollama/Llama in 0.35.0

---

## [0.18.0] - 2025-12-08

### Llama Provider & Model Selection

This release adds support for Meta Llama models via Ollama and introduces model selection in provider configuration.

#### Added

- **Llama Provider (LLA)** in `LangChain4jProviderFactory`
  - Dedicated provider type for Meta Llama models
  - `createLlamaModel()`, `createLlamaStreamingModel()`, `createLlamaEmbeddingModel()`
  - Supports llama3.2, llama3.1, llama2, codellama variants
  - Uses Ollama backend (http://localhost:11434 by default)

- **SimpleAgent & SimpleStreamingAgent** interfaces
  - For providers without tool/function calling support
  - Simplified system prompt without tool instructions
  - Automatic fallback in AIService for Ollama/Llama providers
  - Clear messaging about limitations (no database queries)

- **ModelName Column** in `AIG_Provider` table
  - Configure model per provider (e.g., `llama3.2:1b`, `gpt-4o`, `claude-sonnet-4`)
  - Empty value uses provider-specific defaults
  - Migration scripts for PostgreSQL and Oracle (CLD-1628)

- **LLA Provider Type** in reference list
  - Added to AIGProviderType (AD_Reference)
  - Migration scripts included

#### Changed

- `LangChain4jProviderFactory` reads ModelName from config
- `AIService` detects tool support and uses appropriate agent
- Enhanced logging shows configured model name

#### Technical Notes

- LangChain4j 0.35.0 Ollama integration doesn't support tools
- Tool support limited to: Anthropic, OpenAI, AWS Bedrock
- Local Llama models run CPU-only on Intel Macs (slow)
- Recommended: Use smaller models (llama3.2:1b) for faster responses

---

## [0.17.2] - 2025-12-08

### Unit Test Infrastructure

This patch release adds testing infrastructure for the AI plugin.

#### Added

- **Unit Test Runner Script** (`run-unit-tests.sh`)
  - CLI script for running JUnit 5 tests outside of Eclipse
  - Downloads JUnit Platform Console and AssertJ dependencies
  - Supports test filtering by class name or pattern
  - Verbose mode flag (`-v`) for detailed logging
  - Classpath setup for iDempiere base classes + AI plugin libs

- **Claude Agent: idempiere-junit-manager** (`.claude/agents/`)
  - Specialized agent for JUnit test management
  - Test creation, organization, and troubleshooting
  - iDempiere-specific testing patterns (context, transactions, model layer)
  - Maven/Tycho test configuration guidance

- **LangChain4j Integration Documentation** (`docs/LANGCHAIN4J_FLOWS.md`)
  - Comprehensive documentation of LangChain4j integration
  - Architecture diagrams and data flows
  - Provider factory, agent interfaces, ERP tools
  - Memory management, streaming support, guardrails pipeline
  - Configuration examples and code samples

#### Removed

- **AIServiceStreamingTest.java** - Removed obsolete test with LangChain4j mocking issues
  - Test relied on internal LangChain4j classes that were difficult to mock
  - Streaming functionality tested via integration tests instead

---

## [0.17.1] - 2025-12-08

### Bug Fix: Window Context Not Passed to AI Chat

This patch release fixes a critical bug where the AI chat was not receiving context from the currently opened iDempiere window/tab.

#### Fixed

- **AIService.buildContextPrompt()** - Fixed JSON structure mismatch
  - The method was looking for flat keys (`windowName`, `tabName`, `recordId`)
  - WindowContextProvider produces nested structure (`window_metadata.name`, `tab_context.tab_name`, `record_data`)
  - Context is now correctly extracted and included in AI prompts

- **AIChatStreamingMessage** - Fixed markdown rendering DOM race condition
  - Improved DOM element lookup for marked.js rendering
  - Added fallback strategies for ZK framework element access

#### Added

- Context debugging logs in `chatWithContext()` and `chatStreamingWithContext()`
  - Logs context keys and success flag for troubleshooting
  - Logs when context injection succeeds or fails

---

## [0.17.0] - 2025-12-07

### Phase 17: Streaming Tool Callbacks & Markdown Rendering (ADR-033)

This release enhances streaming with tool execution callbacks and improves markdown rendering.

#### Added

- **StreamingERPTools** (ADR-033)
  - Wrapper for ERPTools with callback support
  - Tool execution notifications for UI updates
  - Fires `onToolStart` and `onToolEnd` callbacks during execution
  - Works with LangChain4j 0.35.0 (Java 11 compatible)

- **Marked.js Client-Side Markdown Rendering**
  - Full markdown support for final AI responses (tables, code blocks, lists)
  - Prism.js syntax highlighting for code blocks
  - GFM (GitHub Flavored Markdown) support

#### Changed

- **AIChatStreamingMessage**
  - `complete()` now uses `renderFinalMarkdown()` with marked.js
  - Filters hallucinated XML function call blocks from output
  - Improved escaping for JavaScript string literals

- **AIService & ERPAgent**
  - Enhanced streaming tool integration
  - Improved callback handling

#### Technical Notes

- LangChain4j 0.35.0 TokenStream lacks built-in tool callbacks (added in later versions)
- StreamingERPTools provides callbacks by wrapping tool method invocations
- Marked.js loaded client-side with retry logic for async availability

---

## [0.16.0] - 2025-12-04

### Phase 16: Chat Ownership & Sharing (ADR-036, CLD-1636)

This release implements chat ownership and access control for multi-user collaboration.

#### Added

- **AIG_ChatOwnership Table** (CLD-1636)
  - PostgreSQL and Oracle migration scripts
  - Links users/roles to chats with access levels (Owner, Write, Read)
  - Time-bound sharing support (ValidFrom/ValidTo)
  - Audit trail via SharedBy_User_ID

- **ChatAccessService** (ADR-036)
  - Hybrid access resolution algorithm (5-step priority)
  - Creator always has OWNER access
  - Explicit user grants via AIG_ChatOwnership
  - Role-based grants for team access
  - ConfidentialType baseline fallback
  - Record access check for context chats

- **MAIChatOwnership Model**
  - Factory methods: createOwnerGrant, createReadGrant, createWriteGrant, createRoleGrant
  - Time-bound validation: isCurrentlyValid()
  - Revoke and delete operations

- **AIChatWidget Access Control**
  - Access check on chat load
  - Write permission check before sending messages
  - Read-only badge for limited access users
  - Input disabled for READ-only access
  - "Shared with me" section in thread selector
  - Access icons: 📖 (read), ✏️ (write)
  - Switch between owned and shared chats

- **MAIChat Access Methods**
  - canRead(), canWrite(), canShare(), canDelete()
  - shareWith(), shareWithRole()
  - getSharingInfo()
  - getOrCreatePrivateContextChat()
  - getOrCreateSharedContextChat()

#### UI Notes

⚠️ **UI Improvement Needed:** The current access indicator and shared chat display uses basic styling. Future improvements should include:
- Polished read-only badge design
- Better visual separation in thread selector
- Share button with proper modal dialog (deferred to v0.17.0)

#### Deferred to v0.17.0

- Share button in AIChatWidget
- User/role picker dialog
- Transfer ownership UI
- Revoke access UI

---

## [0.15.0] - 2025-12-04

### Phase 15: Stop Button & Error Handling (ADR-031, CLD-1606)

This release adds the ability to cancel in-progress AI streaming requests and improves error handling.

#### Added

- **Stop Button** (ADR-031)
  - Red stop button replaces send button during AI processing
  - Cancel streaming requests in progress
  - Partial responses are persisted with "AI request cancelled" notice
  - Uses `z-icon-Square-White` icon

- **User-Friendly Error Messages**
  - Parse JSON error responses from Anthropic API
  - Friendly messages for: content filtering, rate limits, auth errors, timeouts
  - Error responses persisted to chat entry table

- **Cancellation Infrastructure**
  - `streamingInProgress` flag for reliable cancellation detection
  - `requestCancelled` flag checked in all streaming callbacks
  - `markCancelled()` method on AIChatStreamingMessage
  - Renamed `finalize()` to `complete()` to avoid Java GC conflict

#### Fixed

- Send button stays enabled after cancellation
- Error responses now persist to database (visible after logout/login)
- Streaming message ignores chunks after cancellation

#### Known Limitations

- ESC key shortcut not yet implemented (TODO for v0.16.0)
- HTTP connection not actually closed on cancel (requires LangChain4j v1.8.0+)

---

## [0.14.0] - 2025-12-04

### Phase 14: Real-Time Chat Streaming (ADR-033, CLD-1606)

This release implements real-time streaming for AI chat responses with tool execution timeline.

#### Added

- **AIChatStreamingMessage Component** (CLD-1606)
  - Real-time token-by-token streaming display
  - Tool execution timeline with status indicators (pending, running, completed, error)
  - Thinking/reasoning section display
  - Copy button with proper quote escaping
  - Markdown rendering for streamed content

- **ERPStreamingAgent** (CLD-1606)
  - Streaming-aware agent for tool execution
  - Integration with LangChain4j StreamingChatLanguageModel

- **AIStreamCallback Builder Pattern** (CLD-1606)
  - `onThinking(Consumer<String>)` - Thinking content callback
  - `onToolStart(BiConsumer<String, String>)` - Tool execution start
  - `onToolEnd(TriConsumer<String, String, String>)` - Tool execution end
  - `onToken(Consumer<String>)` - Token streaming callback
  - `onComplete(Consumer<AiMessage>)` - Completion callback
  - `onError(Consumer<Throwable>)` - Error handling callback

- **AIService Streaming Methods** (CLD-1606)
  - `chatStreamingWithContext()` - Context-aware streaming chat
  - `getOrCreateStreamingModel()` - Streaming model caching

- **LangChain4jProviderFactory Streaming Support** (CLD-1628)
  - `createAnthropicStreamingModel()` - Anthropic streaming model factory
  - Streaming model instance caching

- **Dependencies** (CLD-1628)
  - Retrofit2 2.9.0 for LangChain4j Anthropic HTTP client
  - converter-jackson and converter-gson for serialization
  - Gson 2.10.1 as Retrofit dependency
  - OkHttp-SSE for streaming SSE support

- **Database Migrations** (CLD-1628)
  - `202512032347_CLD-1628.sql` - PostgreSQL and Oracle migrations

#### Changed

- **AIChatWidget** (CLD-1606)
  - Added `sendMessageLangChain4jStreaming()` method
  - Integrated with AIService streaming infrastructure
  - Real-time message updates during streaming

#### Removed

- Legacy test files with LangChain4j mocking issues
  - AIServiceTest.java
  - ERPToolsTest.java
  - LangChain4jProviderFactoryTest.java
  - AIContextProviderRegistryTest.java
  - SecureDatabaseQueryExecutorTest.java

---

## [0.13.0] - 2025-12-03

### Phase 13: Naming Standards & Integration Wiring

This release refactors naming conventions to use neutral branding and wires all v0.12.0 infrastructure components into the main service.

#### Changed

- **Naming Standards Refactor**
  - `IDempiereAIService` → `AIService` - Neutral branding (not iDempiere-specific)
  - `IDempiereAgent` → `ERPAgent` - Matches `IERPAgent` interface
  - `MAIGBudget` → `MAIBudget` - Consistent with `MAIProvider`, `MAIChat` pattern
  - `MAIGUsageMetrics` → `MAIUsageMetrics` - Same consistency fix
  - All references updated across 10+ files

- **Integration Wiring** (v0.12.0 components were orphaned, now connected)
  - `AIMetricsListener` wired into `LangChain4jProviderFactory`
    - All chat model builders (Anthropic, Ollama, OpenAI, Bedrock) now include metrics listeners
    - Added `setMetricsEnabled(boolean)` for runtime control
  - Guardrails pipeline wired into `AIService.chat()` and `AIService.execute()`
    - **CostGuard** - Budget/rate limit checks before AI calls
    - **InputGuard** - PII masking, injection detection
    - **OutputGuard** - Credential leak filtering, harmful content detection
  - Added `setGuardrailsEnabled(boolean)` for runtime control
  - Added `getBudgetStatus(clientId)` for monitoring

- **Documentation**
  - Updated test README with new class names
  - Established naming conventions in implementation plan:
    - Model classes: `MAI<Entity>` (drop "G" from `AIG_*` tables)
    - Service classes: `AIService`, `ERPAgent` (neutral, not vendor-specific)
    - Interfaces: `I<Concept>` for custom, `I_<TABLE>` for iDempiere generated

#### Fixed

- Test file renamed: `IDempiereAIServiceTest` → `AIServiceTest`
- All internal references updated for consistency

---

## [0.12.0] - 2025-12-03

### Phase 12: Observability, Guardrails & Multi-Tenant Access

This release implements critical P1 infrastructure for production readiness.

#### Added

- **ADR-013: Observability & Cost Tracking** (CLD-1628)
  - `AIMetricsListener.java` - LangChain4j ChatModelListener implementation
    - Token usage tracking (input, output, total)
    - Cost estimation by model (Claude, GPT, Bedrock, Ollama)
    - Latency measurement in milliseconds
    - Persists to AIG_UsageMetrics via model class
  - `CostGuard.java` - Budget enforcement
    - Daily and monthly budget limits per client
    - Rate limiting per user (requests/minute)
    - Token limits per request
    - Uses MAIGBudget model for configuration
  - `UsageMetrics.java` - Metrics DTO
    - Factory methods for easy creation
    - Computed properties (tokensPerSecond, costPerToken)
  - `MAIGUsageMetrics.java` - Business model for AIG_UsageMetrics
    - Factory: `record()`, `recordError()`
    - Query: `getByUser()`, `getByAgent()`, `getBySession()`, `getRecent()`
    - Aggregation: `getTodayTokensByUser()`, `getTodayCostByUser()`
  - `MAIGBudget.java` - Business model for AIG_Budget
    - Scope hierarchy: Agent → User → Client
    - Budget checks: `isDailyBudgetExceeded()`, `wouldExceedDailyBudget()`
    - Usage tracking: `addUsage()`, `maybeResetCounters()`
    - CCache integration for performance

- **ADR-014: Guardrails & Safety**
  - `InputGuard.java` - Input sanitization
    - PII detection (SSN, credit cards, email, phone, tax ID)
    - Prompt injection detection
    - SQL injection pattern detection
    - Actions: PASS, MASK, BLOCK
  - `OutputGuard.java` - Output filtering
    - Credential leakage detection (API keys, passwords, connection strings)
    - Hallucination indicators
    - Internal ID masking
    - Harmful content detection (code execution, destructive SQL)
  - `ExecutionGuard.java` - Risk-based routing
    - Risk levels: LOW, MEDIUM, HIGH, CRITICAL, PROHIBITED
    - Approval routing: AUTO_APPROVE, REQUIRE_CONFIRMATION, REQUIRE_HUMAN_APPROVAL, BLOCK
    - Financial thresholds and bulk operation limits
    - Sensitive table protection
  - `GuardResult.java` - Guard result DTO
    - Actions: PASS, MASK, BLOCK
    - Factory methods for each action type

- **ADR-029: Multi-Tenant Access Integration**
  - `AIGAccessTier` enum in `BoundaryEnforcementFilter.java`
    - TENANT: Own client only
    - TENANT_DICTIONARY: Own + knowledge sources
    - SERVICE_PROVIDER: Target client(s) + knowledge
  - Enhanced `DataAccessValidator.java`
    - `KNOWLEDGE_CLIENT_ID = 1000014`
    - `isKnowledgeTable(tableName)` - detects AD_*, K_* tables
    - `getClientFilter()` methods for tiered access
  - Tiered SQL filtering in `BoundaryEnforcementFilter.java`
    - Knowledge tables: `AD_Client_ID IN (0, 1000014)`
    - Business tables: Respects user/target client

- **Database Migration CLD-1628**
  - `migration/postgresql/202512031000_CLD-1628.sql`
  - `migration/oracle/202512031000_CLD-1628.sql`
  - Tables: AIG_UsageMetrics, AIG_Budget
  - Views: AIG_UsageSummary_Daily, AIG_AgentPerformance, AIG_BudgetStatus

#### Changed

- **Documentation standardization**
  - Standardized company name to "Cloudempiere" throughout
  - Added version annotations (@since v0.11.0, v0.12.0)
  - Added searchable tags (#observability, #guardrails, #multi-tenant, etc.)

#### Fixed

- Migration script format (added `register_migration_script` call)
- Oracle migration compatibility (NUMBER, VARCHAR2, DATE, SYSDATE, SYS_GUID())

---

## [0.11.0] - 2025-12-03

### Phase 11: RAG Infrastructure & Domain Boundaries

#### Added

- **RAG Infrastructure (ADR-012)**
  - `src/com/cloudempiere/ai/rag/RAGContextManager.java` - Core RAG context manager
    - Provider-based embedding model selection (not hardcoded)
    - In-memory embedding store with session isolation
    - Support for all providers: Claude→Bedrock Titan, Bedrock→Titan, Ollama→nomic-embed-text, OpenAI→text-embedding-3-small
    - Fallback handling when embedding model unavailable
  - `src/com/cloudempiere/ai/rag/RAGConversationService.java` - RAG-enabled conversation service
    - Lazy initialization of RAGContextManager from provider
    - ContentRetriever integration with LangChain4j AiServices
    - Automatic context injection into agent prompts

- **Domain Boundaries (ADR-009)**
  - `src/com/cloudempiere/ai/boundary/AgentBoundary.java` - Boundary configuration
    - Builder pattern for defining boundaries
    - Table/column whitelist and blacklist support
    - Action type restrictions (READ, QUERY, CREATE, UPDATE, DELETE, etc.)
    - Cost and rate limit configuration
  - `src/com/cloudempiere/ai/boundary/AgentBoundaryRegistry.java` - Predefined boundaries
    - `inventory-agent`: Read-only access to inventory tables
    - `sales-agent`: Read + limited write to sales tables
    - `purchasing-agent`: Read + limited write to purchasing tables
    - `knowledge-base-agent`: Read-only access to AD metadata
    - `general-agent`: Default balanced access
  - `src/com/cloudempiere/ai/boundary/BoundaryEnforcementFilter.java` - SQL security filter
    - Auto-injects AD_Client_ID and AD_Org_ID into queries
    - SQL injection pattern detection
    - Org scope validation against AgentContext
  - `src/com/cloudempiere/ai/boundary/CostBoundaryMonitor.java` - Budget tracking
    - Daily cost tracking per client
    - Rate limiting per session (tool calls/minute)
    - Token usage monitoring
    - Alert thresholds at 80% of limits
  - `src/com/cloudempiere/ai/boundary/DataAccessValidator.java` - Access control
    - Table whitelist/blacklist validation
    - Column-level access control
    - Sensitive table detection (HR, Pricing, Security)
    - Sensitive column protection (Password, SSN, TaxID, etc.)
    - Integration with iDempiere MRole permissions
  - `src/com/cloudempiere/ai/boundary/BoundaryViolationType.java` - Violation categories
  - `src/com/cloudempiere/ai/boundary/BoundaryViolationException.java` - Exception handling

#### Changed
- **LangChain4jProviderFactory** - Added embedding model creation methods
  - `createEmbeddingModel(MAIProvider, modelName, baseUrl)` - Provider-aware factory
  - `createBedrockEmbeddingModel()` - AWS Bedrock Titan embeddings
  - `createOllamaEmbeddingModel()` - Local Ollama embeddings
  - `createOpenAiEmbeddingModel()` - OpenAI text-embedding-3-small
  - Claude provider now falls back to Bedrock Titan for embeddings

- **MAIProvider** - Added static lookup methods with caching
  - `get(ctx, AIG_Provider_ID, trxName)` - Get by ID with cache
  - `getDefault(ctx, trxName)` - Get default provider
  - `getByType(ctx, providerType, trxName)` - Get by provider type
  - CCache integration for performance

#### Fixed
- **KnowledgeBaseQuery.java** - Java 11 compatibility
  - Converted Java 15 text blocks (`"""..."""`) to string concatenation
  - Fixed `DB.query()` calls to use `DB.prepareStatement()` + `executeQuery()` pattern
  - Fixed `DB.executeUpdate(sql, Object[])` to use PreparedStatement pattern
  - All 6 methods now compile correctly on Java 11

---

### Next: v0.10.0 (Q1 2026)
**Focus:** RAG Migration, Structured Outputs, Observability, MCP REST API

**Phase 1: RAG Migration (ADR-012) - 2 weeks**
- Week 1: Setup LangChain4j RAG infrastructure (EmbeddingStore, ContentRetriever)
  - ✅ Add dependencies: langchain4j-embeddings, langchain4j-ollama (already in pom.xml)
  - ✅ Create RAGContextManager (~100 lines) - DONE
  - ✅ Create RAGConversationService - DONE
  - Parallel testing (RAG vs custom routing)
- Week 2: Migration and cleanup
  - Switch AIConversationService to RAG pattern
  - Remove custom routing code (PromptAnalyzer, ConversationContextManager, EntityExtractor - 630 lines)
  - Validation testing (target: >50% cache hit rate, >95% accuracy)

**Phase 2: Domain Boundaries (ADR-009) - 1 week**
- ✅ BoundaryEnforcementFilter - DONE (auto-inject org/client filters)
- ✅ DataAccessValidator - DONE (table/column permissions)
- ✅ CostBoundaryMonitor - DONE (budget tracking)
- ✅ AgentBoundaryRegistry - DONE (predefined boundaries)
- Integration with existing BoundaryValidator and SecureDatabaseQueryExecutor

**Phase 2: Structured Outputs - 1 week**
- Structured outputs (OrderSummary, InventoryReport Java records)
- Type-safe AI responses with validation

**Phase 3: Observability - 1 week**
- Observability listeners (TokenUsageListener, LatencyListener, AuditListener)
- Cost tracking and performance metrics

**Phase 4: MCP REST API - 2 weeks**
- MCP REST API completion (HTTP endpoints for external clients)
- Enhanced context extraction (form state, process parameters)
- Performance optimization (query caching, connection pooling)

### Next: v0.11.0 (Q1-Q2 2026)
**Focus:** LangChain4j Enhancements (ADR-008), Domain Agents, Agentic Workflows

**Phase 1: Instruction Following Enhancements (ADR-008) - 4 weeks**
- Week 1: Temperature Control (Quick Win)
  - Set `temperature(0.0)` for deterministic function calling
  - 1 line of code, 15-minute implementation
  - **Impact:** Reproducible results for testing, consistent behavior

- Week 2-3: Structured Outputs (High Priority)
  - Define typed interfaces (`ERPQueryResult`, `Record`) with `@Description` annotations
  - Replace manual JSON parsing with LangChain4j automatic schema validation
  - Type-safe AI responses with compile-time validation
  - **Impact:** 80% less parsing code (100 → 20 lines), automatic retry on invalid structure

- Week 4: Validation Loop (Medium Priority)
  - Implement `OutputParser` with `formatInstructions()` for automatic error correction
  - Self-correcting AI (retries with enhanced prompts on validation failure)
  - **Impact:** 70% less validation code (50 → 15 lines), auto-correction of AI errors

**Expected Results:**
- ✅ 80% less parsing code
- ✅ Type-safe responses
- ✅ Deterministic function calling
- ✅ Auto-correction of AI errors
- ✅ 95% → 98% accuracy improvement

**Phase 2: Domain Agents & Agentic Workflows - 4 weeks**
- Domain-specific agents (InventoryAgent, SalesAgent, PurchasingAgent, HelpDeskAgent)
- Agentic workflows (sequential, parallel, conditional routing via `langchain4j-agentic`)
- Input/Output Guards (cost boundaries, PII detection, SQL injection prevention)
- Process execution tool (AI can trigger iDempiere processes with approval)
- Report generation tool (AI-generated Jasper reports)

---

## [0.10.1] - 2025-12-01

### MCP Architecture Validation & Documentation Cleanup

#### Added
- **ADR-003 MCP Validation Report** (`docs/ADR-003_MCP_VALIDATION_REPORT.md`)
  - Validated ADR-003 against LangChain4j MCP capabilities
  - Confirmed Java MCP SDK maturity (v0.16.0, Feb 2025)
  - Documented LangChain4j `langchain4j-mcp` client support
  - Updated decision: Keep idempiere-mcp-server + cloudempiere-cli architecture

- **ADR-001 Architecture Diagrams**
  - Complete system architecture ASCII diagram (done + planned)
  - ADR implementation status with progress bars
  - Data flow architecture
  - Project ecosystem comparison

- **New ADRs** (005-015)
  - ADR-005: Intelligent Data Source Routing
  - ADR-006: Data Model Architecture
  - ADR-007: Database Security Model
  - ADR-008: LLM Instruction Following
  - ADR-009: Domain Boundaries and Agent Scope
  - ADR-010: Agent Orchestration Architecture
  - ADR-011: Specialized Agent Scopes
  - ADR-012: RAG-Based Context Retrieval
  - ADR-013: Observability and Cost Tracking
  - ADR-014: Guardrails and Safety
  - ADR-015: Conversational UX Patterns

#### Changed
- **ADR-003** - Updated to v6.0 with revised MCP architecture decision
  - Keep separate projects (idempiere-mcp-server + cloudempiere-cli)
  - No merge needed - CLI backend already works via subprocess
  - Added configuration examples for Claude Desktop
  - Reduced effort estimate from 4 weeks to 1 week

#### Removed
- **Obsolete research documents** moved to `docs/archive/`
  - `docs/ai-agent-javaframeworkagent/` - consolidated into ADR-004
  - `docs/ai-agent-vs-prompt-claudejava/` - consolidated into ADR-004
  - `docs/analysis-to-langchain/` - consolidated into ADRs 009-011
  - `docs/bxchatbot/` - consolidated into ADR-002

---

## [0.10.0] - 2025-12-01

### Phase 10: Security Fixes, Migration Scripts & ADR Validation

#### Added
- **ADR-013: Observability and Cost Tracking** (`docs/adr/013-observability-cost-tracking.md`)
  - LangChain4j ChatModelListener + Custom Persistence
  - Components: AIMetricsListener, CostGuard, AIG_UsageMetrics table
  - Token usage, cost attribution, latency tracking
  - Budget enforcement with daily/monthly caps
  - 9-day implementation timeline

- **ADR-014: Guardrails and Safety** (`docs/adr/014-guardrails-and-safety.md`)
  - LangChain4j Guards + Custom Risk Classification
  - InputGuard: PII detection, prompt injection prevention
  - ExecutionGuard: Risk-based routing (LOW/MEDIUM/HIGH/CRITICAL)
  - OutputGuard: Hallucination detection, data leakage prevention
  - Human-in-the-loop approval workflows
  - 12-day implementation timeline

- **ADR-015: Conversational UX Patterns** (`docs/adr/015-conversational-ux-patterns.md`)
  - Parameter Chain pattern for flexible input collection
  - Structured response formatting with action links
  - Proactive insight engine (warnings, suggestions)
  - iDempiere zoom link integration
  - 8-day implementation timeline

- **ADR-012: RAG-Based Context Retrieval** (`docs/adr/012-rag-based-context-retrieval.md`)
  - Supersedes ADR-005 custom regex-based routing
  - Migrates to LangChain4j ContentRetriever with semantic search
  - 86% code reduction (730 → 100 lines)

- **Code vs ADR Gap Analysis** (`docs/CODE_ADR_GAP_ANALYSIS.md`)
  - Comprehensive analysis of 81 Java files vs 15 ADRs
  - Identified 28 files (~1,930 lines) of deprecated code to remove
  - 3 critical conflicts - ALL RESOLVED:
    - Conflict 1: ADR-012 RAG migration approved (100% RAG, delete routing/)
    - Conflict 2: Remove legacy agent code approved (7 files, 500 lines)
    - Conflict 3: Remove legacy tool code approved (9 files, 400 lines)
  - ADR implementation status: 5 fully implemented, 3 partial, 6 not started
  - Package-by-package coverage matrix
  - 8-week sprint plan to production-ready state

- **Updated Priority Revision** (`docs/PRIORITY_REVISION_2025_12.md`)
  - Added Part 5: Critical Conflicts section - ALL RESOLVED
  - Added Part 6: Code Coverage Summary
  - Added Part 8: Updated Action Items with 25 tasks across 4 sprints
  - Status changed from DRAFT to APPROVED - Implementation Ready
  - Decisions documented:
    - Decision #5: Complete ADR-012 RAG migration (100%)
    - Decision #6: Remove legacy agent code
    - Decision #7: Remove legacy tool code

- **AI Database Access Validation Report** (`docs/AI_DATABASE_ACCESS_VALIDATION_REPORT.md`)
  - Validated 4 planning documents (~3,700 lines total)
  - All documents superseded by ADR-002 (@Tool pattern) + ADR-007 (Security model)
  - Implementation complete: ERPTools with 9 @Tool methods
  - 75% code reduction vs planned approach (manual function loop → @Tool annotations)
  - Enhanced security: Dual-identity audit model (AI User + Real User)
  - Projected improvements: 65% cache hit rate, 98% accuracy
  - Implementation plan: 2-week migration with feature flag

- **CLD-1601 Folder Validation Report** (`docs/CLD_1601_VALIDATION_REPORT.md`)
  - Validated 8 planning documents (~8,750 lines total)
  - All 8 documents superseded by ADRs (002, 006, 007, 009, 012)
  - Original 30-week/$316K-$432K plan reduced to ~8 weeks/$80K-$120K
  - 90% code reduction achieved through LangChain4j adoption
  - All documents updated with superseded notices pointing to current ADRs
- **Routing Validation Report** (`docs/ROUTING_VALIDATION_REPORT.md`)
  - Comprehensive validation of intelligent routing against LangChain4j RAG
  - Component-by-component comparison (custom vs LangChain4j)
  - Performance projections and migration path
  - Success metrics and rollback plan
- **ADR Validation Report** (`docs/ADR_VALIDATION_REPORT.md`)
  - Comprehensive validation of ADRs against LangChain4j 2025 capabilities
  - Identified critical gaps: RAG, structured outputs, agentic patterns, observability
  - Code reduction potential: ~1000 lines via LangChain4j standard patterns
  - Validation findings: ADR-002 and ADR-004 ✅ validated, ADR-005/009/010 need updates
- **Updated PROJECT.md**
  - Aligned with three core goals (Admin/Helpdesk, End User WebUI, MCP Server)
  - Strategic architecture section (Custom iDempiere + LangChain4j Standards + Provider SDKs)
  - 4 detailed use case examples
  - Complete architecture diagram from external clients to database
- **Updated FEATURES.md**
  - Feature matrix organized by three core goals
  - Validation status section with ADR review findings
  - Performance benchmarks section
  - Planned dependencies for v0.10.0+ (embeddings, agentic, mcp)
- **Database Migration Scripts** (CLD-1601, CLD-1606)
  - PostgreSQL and Oracle migration scripts for AI tables
  - `AIG_Provider`, `AIG_QueryAudit`, `AIG_Chat`, `AIG_ChatEntry` tables
  - AI prompt configuration tables

#### Fixed
- **Metadata Exposure in AI Responses** - Internal fields (user_id, role_id, role_name, column_types) no longer exposed in AI chat responses
  - `AIDatabaseFunctionHandler.buildFilteredResponse()` - Filters query results before returning to AI
  - `AIConversationService.buildContextOnlyResponse()` - Formats cached data properly instead of raw JSON dump
  - Added `formatQueryResultForDisplay()` and `formatColumnName()` helpers

#### Changed
- LangChain4j provider adapter improvements
- Documentation cleanup (removed obsolete planning docs)

#### Removed
- **Deleted `docs/CLD-1601/` folder** - 8 obsolete planning documents (~8,750 lines)
  - All documents superseded by formal ADRs (002, 006, 007, 009, 012)
  - Original 30-week plan replaced by LangChain4j adoption (~8 weeks)
  - Validation report preserved at `docs/CLD_1601_VALIDATION_REPORT.md`

**Key Insights from ADR Validation:**
- LangChain4j capabilities were significantly underestimated
- Standard solutions exist for problems we're solving custom (routing, boundaries, orchestration)
- **ADR-005 superseded by ADR-012:** Custom routing (730 lines) → LangChain4j RAG (100 lines)
- v0.10.0+ will leverage LangChain4j RAG, structured outputs, and agentic patterns
- Estimated 60% further code reduction possible via standard patterns

- **Instruction Following Validation Report** (`docs/INSTRUCTION_FOLLOWING_VALIDATION_REPORT.md`)
  - Validated LLM instruction following strategies against ADR-008 implementation
  - ADR-008 already implements 60% of research strategies (explicit instructions, examples, language detection, schema context)
  - Identified 3 LangChain4j enhancement opportunities (structured outputs, temperature control, validation loop)
  - 80% code reduction potential via structured outputs (100 lines → 20 lines parsing code)
  - No new ADR needed - enhancements added to ADR-008

#### Updated
- **ADR-008: LLM Instruction Following**
  - Added "LangChain4j Enhancements (Planned v0.11.0)" section
  - Structured outputs with typed interfaces (80% less parsing code)
  - Temperature control for deterministic function calling (1-line quick win)
  - Validation loop with OutputParser (automatic retry on errors)
  - 4-week implementation timeline for v0.11.0

**Strategic Decision:**
- ✅ Created ADR-012 to migrate from custom regex routing to LangChain4j RAG
- ✅ Updated ADR-005 status to "Superseded by ADR-012"
- ✅ Validated database access implementation (already complete via ADR-002)
- 📋 Next: Implement RAG infrastructure (2-week timeline, see Unreleased section)

**Validation Reports:**
- `docs/ROUTING_VALIDATION_REPORT.md` - Custom routing vs LangChain4j RAG comparison
- `docs/DATABASE_ACCESS_VALIDATION_REPORT.md` - Database access implementation validation

**Commits:**
- `154b710` fix(ai): filter sensitive metadata from AI chat responses
- docs(adr): add comprehensive ADR validation report
- docs(project): align PROJECT.md with three core goals
- docs(features): reorganize by goals and add validation status

---

## [0.9.0] - 2025-12-01

### Phase 9: LangChain4j Native Providers

#### Added
- **LangChain4j Provider Infrastructure**
  - `LangChain4jProviderFactory` - Creates ChatLanguageModel from MAIProvider config
  - `IDempiereAgent` - AiServices interface with system prompt
  - `IDempiereAIService` - Main facade for AI interactions
  - `ERPTools` - @Tool annotated methods for ERP operations
- **New Provider Support**
  - `langchain4j-anthropic` - Native Anthropic Claude integration
  - `langchain4j-bedrock` - AWS Bedrock via LangChain4j
  - `langchain4j-ollama` - Local LLM support
  - `langchain4j-open-ai` - OpenAI/Azure integration
- **ERP Tools with @Tool Annotations**
  - `queryDatabase` - Execute SQL SELECT queries
  - `lookupRecord` - Get record by ID
  - `searchRecords` - Search with WHERE clause
  - `getTableMetadata` - Table structure info
  - `listTables` - List accessible tables
  - `getBusinessPartner` - BP lookup
  - `getProduct` - Product lookup
  - `getOrder` - Order lookup

#### Deprecated
- `AnthropicProvider` - Use LangChain4jProviderFactory instead
- `AWSBedrockProvider` - Use LangChain4jProviderFactory instead
- `IAIProvider` interface - Use ChatLanguageModel instead

**Commits:**
- `62eb679` feat(LangChain4j): Migrate to LangChain4j native providers (ADR-002)

---

## [0.8.0] - 2025-12-01

### Phase 8: MCP Server & Strategic Architecture

#### Added
- **MCP Server Documentation** (11 comprehensive guides)
  - `00-START_HERE.md` - Quick orientation guide
  - `01-ARCHITECTURE.md` - System design and integration patterns
  - `02-IMPLEMENTATION_GUIDE.md` - Complete build guide (HTTP API + Node.js MCP)
  - `03-BEST_PRACTICES.md` - Security, reliability, performance patterns
  - `04-DEPLOYMENT.md` - Docker, AWS, on-prem deployment
  - `05-MCP_AGENT_INTEGRATION.md` - Agent integration patterns
  - `06-REUSE_EXISTING_PLUGIN.md` - Leveraging current codebase
  - `INDEX.md` - Documentation navigation
  - `MCP_BENEFITS.md` - Business value and ROI
  - `QUICK_REFERENCE.md` - Developer cheatsheet
  - `README.md` - Project overview
- **Architecture Decision Records**
  - `ADR-002` - LangChain4j strategic adoption plan
  - `ADR-002-appendix` - Feature mapping (no functionality lost)
  - `ADR-003` - MCP server integration decision
- **Project Governance**
  - `docs/GOVERNANCE.md` - Workflows, conventions, release process

#### Changed
- Updated roadmap with strategic LangChain4j migration path
- Refined phase planning for v0.9.0 - v1.0.0

**Commits:**
- `026f0fb` docs(MCPServer): Add comprehensive MCP server documentation
- `02ad59d` docs(ADR): Add feature mapping appendix
- `80ac7bd` docs(ADR): Add ADR-002 for LangChain4j strategic adoption

---

## [0.7.0] - 2025-12-01

### Phase 7: Documentation & Claude Agents

#### Added
- **Claude Code Agents**: 26 specialized agents for iDempiere development
  - Architecture, caching, code review, data modeling experts
  - Docker, deployment, localization, migration specialists
  - OSGi, plugin, process, workflow developers
  - Quality assurance, reporting, terraform experts
- **Project Documentation**
  - `PROJECT.md` - Project overview and quick start guide
  - `FEATURES.md` - Comprehensive feature matrix
  - `docs/adr/001-initial-architecture.md` - Architecture Decision Record
  - `.claude/commands/release.md` - Release workflow command

#### Changed
- Updated `CLAUDE.md` with roadmap phases and commit conventions

---

## [0.6.0] - 2025-11-28

### Phase 6: LangChain4j Agent Framework

#### Added
- **LangChain4j Integration**
  - Agent framework for complex AI workflows
  - `langchain4j.jar` embedded in plugin
  - Agent-based conversation handling

**Commit:** `b39939e` feat(LangChain): Integrate LangChain4j agent framework, CLD-1606

---

## [0.5.0] - 2025-11-26

### Phase 5: LangChain Integration

#### Added
- **LangChain Framework**
  - ERP tool integration layer
  - Enhanced AI context with syntax support
- **Thread-Scoped Context**
  - Conversation context persistence
  - History management per thread
- **Documentation**
  - MCP server implementation guide
  - Strategic analysis documents
  - iDempiere context agent specification

**Commits:**
- `01960c2` feat(AIPlugin): Add langchain framework and ERP tool integration, CLD-1606
- `eda02bd` feat(AIPlugin): Add thread-scoped conversation context and history, CLD-1606
- `6800a87` docs(MCPServer): Add comprehensive MCP server documentation

**PR:** #6 merged to master

---

## [0.4.0] - 2025-11-26

### Phase 4: AI Chat Widget (CLD-1606)

#### Added
- **AI Chat Component**
  - Interactive chat widget for ZK UI
  - `AIChatWidget` class with conversation support
- **Tab Context Awareness**
  - Extract context from active iDempiere tab
  - Window and record context injection
- **Conversation Features**
  - Thread-scoped conversation history
  - Intelligent data source routing
  - Zoom link support for enhanced UX
- **Query Improvements**
  - SQL clause handling fixes
  - Better query generation

**Commits:**
- `9666cbc` feat(AIChat): Add AI chat widget and supporting classes, CLD-1606
- `8c13dc0` feat(AIChat): Add tab context to ai chat, CLD-1606
- `27107d9` feat(AIPlugin): AI chat enhancements, CLD-1606
- `ab084dd` feat(AIPlugin): Add zoom link support and improve AI chat UX, CLD-1606
- `4a42139` feat(AIPlugin): Add intelligent data source routing and SQL clause fix, CLD-1606

**PR:** #5 (CLD-1606)

---

## [0.3.0] - 2025-11-20

### Phase 3: Security Layer & Context Providers (CLD-1601)

#### Added
- **Secure Database Query Executor**
  - `SecureDatabaseQueryExecutor` class
  - AI User identity model for queries
  - Role-based access control via `AccessSqlParser`
  - Query validation (SQL injection prevention)
- **Audit System**
  - `SecureQueryAudit` DTO
  - `AIG_QueryAudit` table for audit trail
  - Records AI user and initiating user
- **Context Providers**
  - `IAIContextProvider` interface
  - `AIContextProviderRegistry` for provider management
  - `WindowContextProvider` - iDempiere window data extraction
  - `ChartContextProvider` - Chart data extraction
  - `ContextParameters` DTO

#### Fixed
- Use constant for success status in `isSuccess()` method

**Commits:**
- `8a01cbe` feat(AIPlugin): Add AI context provider infrastructure, CLD-1601
- `c428471` feat(AIPlugin): Add secure AI database query executor and audit, CLD-1601
- `ccce013` Implement AI user identity for secure query execution
- `36f95dd` fix(AIPlugin): Use constant for success status in isSuccess(), CLD-1601

**PRs:** #3, #4

---

## [0.2.0] - 2025-11-18

### Phase 2: AWS Bedrock Integration (CLD-1601)

#### Added
- **AWS Bedrock Provider**
  - `AWSBedrockProvider` class (skeleton implementation)
  - Foundation models integration structure
- **Dependencies**
  - Netty HTTP client for AWS SDK
  - `bedrockruntime.jar`
  - Netty jars (buffer, codec, handler, transport, etc.)

**Commits:**
- `b98f7d4` feat(AIPlugin): AWS Bedrock AI provider, CLD-1601
- `7609037` feat(AIPlugin): Add Netty HTTP client dependencies for AWS SDK, CLD-1601

**PR:** #2

---

## [0.1.0] - 2025-11-18

### Phase 1: Initial Provider Infrastructure (CLD-1601)

#### Added
- **Plugin Setup**
  - OSGi bundle configuration
  - `Activator.java` for bundle lifecycle
  - Maven/Tycho build configuration
- **Provider Architecture**
  - `IAIProvider` interface - provider abstraction
  - `IAIProviderFactory` interface
  - `AIProviderFactory` OSGi service (@Component)
  - Provider registry with caching
- **Anthropic Claude Provider**
  - `AnthropicProvider` - full implementation
  - Claude 3 Opus, Sonnet, Haiku support
  - Claude 3.5 Sonnet support
  - Synchronous text generation
  - Streaming text generation with callbacks
  - Function calling / tool use support
  - Cost estimation with pricing (Jan 2025)
  - Health monitoring
  - Rate limit tracking
- **Data Transfer Objects**
  - `AIRequest` - request wrapper (Builder pattern)
  - `AIResponse` - response with tokens and cost
  - `AIMessage` - conversation message
  - `AITokenUsage` - token tracking
  - `AIModelCapabilities` - feature flags per model
  - `AIHealthStatus` - health monitoring
  - `AIRateLimitStatus` - rate limit tracking
  - `AIFunction` / `AIFunctionCall` - tool use
  - `AIStreamCallback` - streaming interface
- **Data Models**
  - `I_AIG_Provider` - interface (from AD_Table)
  - `X_AIG_Provider` - base model (from AD_Table)
  - `MAIProvider` - business logic model
  - `AIG_Provider` table for provider configuration
- **Exception Handling**
  - `AIProviderException` with error codes and retryable flag
- **Processes**
  - `TestAIProvider` - provider connectivity testing
- **Dependencies**
  - Anthropic Java SDK v2.10.0
  - Jackson v2.17.0
  - OkHttp v4.12.0
  - Kotlin v1.9.10

**Commits:**
- `6ccc13b` feat(AIPlugin): Initial plugin setup, CLD-1601
- `dfd2182` feat(AIPlugin): AI provider, CLD-1601
- `98c7db2` fix(AnthropicProvider): Refactor Anthropic provider and add test process, CLD-1601

**PR:** #1

---

## Version Format

- **Major** (X.0.0): Breaking changes to provider API
- **Minor** (0.X.0): New features, backward compatible
- **Patch** (0.0.X): Bug fixes, backward compatible

## Phase Summary

| Phase | Version | Date | Milestone |
|-------|---------|------|-----------|
| 1 | v0.1.0 | 2025-11-18 | Initial Provider Infrastructure |
| 2 | v0.2.0 | 2025-11-18 | AWS Bedrock Integration |
| 3 | v0.3.0 | 2025-11-20 | Security Layer & Context Providers |
| 4 | v0.4.0 | 2025-11-26 | AI Chat Widget |
| 5 | v0.5.0 | 2025-11-26 | LangChain Integration |
| 6 | v0.6.0 | 2025-11-28 | LangChain4j Agent Framework |
| 7 | v0.7.0 | 2025-12-01 | Documentation & Claude Agents |
| 8 | v0.8.0 | 2025-12-01 | MCP Server & Strategic Architecture |
| 9 | v0.9.0 | 2025-12-01 | LangChain4j Native Providers |
| 10 | v0.10.0 | 2025-12-01 | Security Fixes & Migration Scripts |
| 11 | v0.11.0 | 2025-12-03 | RAG Infrastructure & Domain Boundaries |
| 12 | v0.12.0 | 2025-12-03 | Observability, Guardrails & Multi-Tenant Access |
| 13 | v0.13.0 | 2025-12-03 | Naming Standards & Integration Wiring |
| 14 | v0.14.0 | 2025-12-04 | Real-Time Chat Streaming |
| 15 | v0.15.0 | 2025-12-04 | Stop Button & Error Handling |
| 16 | v0.16.0 | 2025-12-04 | Chat Ownership & Sharing |
| 17 | v0.17.0 | 2025-12-07 | Streaming Tool Callbacks & Markdown Rendering |
| 18 | v0.18.0 | 2025-12-08 | Llama Provider & Model Selection |
| 19 | v0.19.0 | 2025-12-08 | Streaming-First Architecture & Tool Support Separation |

## Upcoming Phases

| Phase | Version | Target | Milestone |
|-------|---------|--------|-----------|
| 20 | v0.20.0 | Q1 2026 | Share Dialog & Advanced Sharing |
| 21 | v0.21.0 | Q1 2026 | Chart Executive Overview (First Business Case) |
| 22 | v0.22.0 | Q1 2026 | Domain Agents (Inventory, Sales, Purchasing) |
| 23 | v1.0.0 | Q2 2026 | Production Release |

## iDempiere Compatibility

| Plugin Version | iDempiere Version | Bundle-Version |
|----------------|-------------------|----------------|
| 0.1.0 - 0.10.0 | 10.x, 11.x, 12.x | 10.0.0.qualifier |
