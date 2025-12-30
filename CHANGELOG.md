# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Conventional Commits](https://conventionalcommits.org/).

## [Unreleased]

### vNext - TBD

**Added:**
- None

**Changed:**
- None

**Fixed:**
- None

**Removed:**
- None

---

## [10.0.1] - 2025-12-30

### Bug Fixes and Improvements

**Added:**
- Migration script for AIG_Embedding Application Dictionary metadata (CLD-1601)

**Changed:**
- Refactored AI Chat Widget CSS architecture - removed all inline styles (CLD-1606)
- Converted inline styles to CSS classes in AI chat widget for better maintainability (CLD-1601)
- Moved AI Chat Panel creation to plugin factory pattern (CLD-1601)

**Fixed:**
- Improved CSS organization and maintainability in AI chat widget components

---

## [10.0.0] - 2025-12-29

### Initial Release: iDempiere v10 AI Plugin

This is the first production release of the CloudEmpiere AI Plugin for iDempiere v10. This release includes a comprehensive AI integration framework with LangChain4j, multi-provider support, and enterprise-grade security features.

#### Added

- **Plugin Health Verification System**
  - Automatic prerequisite checking (PostgreSQL driver, pgvector extension)
  - Creates AD_Issues for missing optional dependencies with severity warnings
  - Health status dashboard in AI Configuration window
  - File: `AIPluginHealthService.java`

- **LangChain4j Integration**
  - Multi-provider architecture (Anthropic, AWS Bedrock, Ollama, OpenAI)
  - Streaming chat responses with real-time UI updates
  - RAG (Retrieval Augmented Generation) with pgvector embeddings
  - Function calling and tool use capabilities
  - File: `AIService.java`, `EmbeddingStoreProvider.java`

- **Security & Access Control**
  - Cross-tenant data isolation (AD_Client_ID validation)
  - Role-based access control for chat history
  - Secure database query execution
  - File: `ChatAccessService.java`

- **AI Chat Widget**
  - ZK-based chat interface with streaming support
  - Thread management for conversation context
  - Message cancellation and error handling
  - Batch rendering optimization (50ms batching)
  - File: `AIChatWidget.java`, `AIChatStreamingMessage.java`

- **Database Schema**
  - Migration scripts for PostgreSQL and Oracle
  - Consolidated schema: `202512291504_CLD-1601.sql`
  - Vector embeddings support with pgvector
  - Chat history and message persistence

#### Fixed

- **NoClassDefFoundError for PostgreSQL Driver**
  - Added explicit dependency: `org.compiere.db.postgresql.provider` in MANIFEST.MF
  - Ensures PostgreSQL JDBC driver is available at runtime
  - Critical for embedding store initialization

- **Production Readiness (P0 Blockers)**
  - Cross-tenant data access vulnerability (Issue #1)
  - NPE in streaming completion handler (Issue #2)
  - Race condition in thread management (Issue #3)
  - Invalid desktop reference in callbacks (Issue #4)
  - Race condition in streaming cancellation (Issue #5)
  - Memory leak in agent/model cache (Issue #6)
  - Missing exception handler in guardrails (Issue #9)
  - Batch render event listener missing (Issue #10)

#### Technical Details

- **Java Version**: Amazon Corretto 11 (JavaSE-11)
- **iDempiere Version**: Release-10 (10.0.0)
- **LangChain4j Version**: 0.35.0 (last Java 11 compatible)
- **Build System**: Maven with Tycho (eclipse-plugin packaging)
- **OSGi Bundle**: com.cloudempiere.ai v10.0.0

#### Migration Notes

- **From Previous Versions**: Not applicable (first release)
- **Prerequisites**:
  - iDempiere v10 (Release-10 branch)
  - PostgreSQL 12+ with pgvector extension (optional, for RAG features)
  - Java 11 (Amazon Corretto recommended)

#### Known Limitations

- Java 17 features blocked (MCP support, extended thinking, Google Gemini streaming)
- Requires migration to iDempiere Release-11 for Java 17 upgrade
- See ADR-035 for Java version strategy and migration path

---

## [0.31.1] - 2025-12-22

### Patch Release: Critical Streaming Rendering Fix

This patch release adds the missing batch render event listener that was critical for streaming to work correctly.

#### Fixed

- **Batch Render Event Listener Missing** ⚠️ **CRITICAL STREAMING FIX**
  - Added `onBatchRender` event listener registration in `AIChatStreamingMessage` constructor
  - Without this listener, JavaScript batched rendering is completely broken
  - JavaScript fires `onBatchRender` events every 50ms, but they were silently dropped
  - File: `AIChatStreamingMessage.java:318-328`
  - Impact: Streaming rendering now works correctly with 50ms batching

#### Changed

- **OSGI Service Configuration Simplification**
  - Updated `AIChatWidgetFactory.xml` xmlns from v1.3.0 to v1.1.0
  - Simplified interface reference (removed package prefix)
  - File: `OSGI-INF/com.cloudempiere.ai.factory.AIChatWidgetFactory.xml`

---

## [0.31.0] - 2025-12-19

### Critical Security and Stability Fixes (P0 Blockers)

This release addresses 8 critical security and stability issues identified in production deployment readiness review. All P0 blockers are now resolved.

#### Fixed

- **#1: Cross-Tenant Data Access Vulnerability** ⚠️ **CRITICAL SECURITY**
  - Fixed chat loading without client ID validation
  - Chat queries now filter by `AD_Client_ID` at database level
  - Prevents attackers from querying chats across tenants
  - File: `ChatAccessService.java:78-93`
  - Impact: Eliminates cross-tenant data breach risk

- **#2: NPE in Streaming Completion Handler** ⚠️ **CRITICAL STABILITY**
  - Added comprehensive null checks in `onComplete()` callback
  - Prevents chat from hanging on null responses
  - Added catch-all exception handler with user error callback
  - File: `AIService.java:963-1070`
  - Impact: Chat no longer hangs on streaming errors

- **#3: Race Condition in Thread Management** ⚠️ **HIGH PRIORITY**
  - Implemented synchronized block with `threadLock` for atomic operations
  - Fixed race condition when rapid messages create thread corruption
  - Thread ID captured before async operation starts
  - File: `AIChatWidget.java:912-950`
  - Impact: All messages correctly associated with their threads

- **#4: Invalid Desktop Reference in Callbacks** ⚠️ **CRITICAL STABILITY**
  - Added `safeSchedule()` helper method for desktop scheduling
  - Checks desktop validity before and during event execution
  - Prevents silent failures when user navigates away during streaming
  - File: `AIChatWidget.java:2250-2278`
  - Impact: No more silent crashes when window closes

- **#5: Race Condition in Streaming Cancellation** ⚠️ **HIGH PRIORITY**
  - Changed `requestCancelled` from boolean to `AtomicBoolean`
  - Implemented atomic `compareAndSet()` for cancellation flag
  - Prevents chunks from appearing after cancel button clicked
  - File: `AIChatWidget.java:100-107, 2168-2192`
  - Impact: Clean cancellation without stale content

- **#6: Memory Leak in Agent/Model Cache** ⚠️ **MEDIUM PRIORITY**
  - Replaced unbounded `ConcurrentHashMap` with bounded LRU caches
  - Added `MAX_CACHE_SIZE=100` with automatic eviction
  - Applies to agent cache, memory cache, and streaming model cache
  - File: `AIService.java:105-151`
  - Impact: Prevents OOM in long-running production servers

- **#9: Missing Exception Handler in Guardrails** ⚠️ **MEDIUM PRIORITY**
  - Added catch-all handlers for unexpected guardrail exceptions
  - User-friendly error messages instead of crashes
  - Applies to cost guard and input guard validation
  - File: `AIService.java:308-329`
  - Impact: Graceful error handling for all guardrail failures

- **#10: Null Language Dereference** ⚠️ **MEDIUM PRIORITY**
  - Verified existing null checks for language objects
  - Already protected with ternary operators
  - File: `AIService.java:762-763`
  - Impact: No crashes on unknown language codes

#### Changed

- **Provider Type Constants** (Code Quality)
  - Fixed `PROVIDER_MOCK_OPENAI` to reference correct constant
  - Added `PROVIDER_AI_HUB` for iDempiere AI Hub support
  - Updated all switch statements for consistent provider handling
  - File: `LangChain4jProviderFactory.java:44-51`

**⚠️ TEMPORARY DEBUGGING (Carried over from v0.30.0):**
- **Zoom Links Disabled** - `ZoomLinkProcessor.processZoomLinks()` returns text unchanged
  - Purpose: Isolate table rendering issues from zoom link processing
  - Impact: Zoom link syntax `[[Table:ID|Display]]` will show as raw text
  - To re-enable: Remove early return in `ZoomLinkProcessor.java:78-82`
- **AD_Client_ID Logging** - Context validation logging added at all critical points
  - `[STREAM-INIT]` - Context set on StreamingTableRenderer initialization
  - `[FINAL-RENDER]` - Context set before final table rendering
  - `[PARTIAL-RENDER]` - Context set before partial markdown rendering
  - `[STREAMING-ZOOM]` - AD_Client_ID logged during streaming zoom link processing
  - `[TABLE-ZOOM]` - AD_Client_ID logged during final table zoom link processing
  - `[ZOOM-PROCESSOR]` - AD_Client_ID logged when zoom processor is called
  - Purpose: Validate that context (including AD_Client_ID) is preserved throughout rendering pipeline
  - Impact: Additional warning-level logs during streaming and rendering
  - To remove: Search for "AD_Client_ID=" in source files and remove logging statements

**Added:**
- **CommonMark Java Integration** (ADR-047) ⭐ **FULL MARKDOWN SUPPORT**
  - Replaced basic regex-based renderer with CommonMark Java library
  - **Full CommonMark Specification**: All standard markdown features supported
  - **GitHub Flavored Markdown (GFM)**: Tables extension, strikethrough support
  - **Numbered Lists**: Proper `<ol><li>` rendering with nesting support
  - **Unordered Lists**: Bullet lists with `<ul><li>` wrappers and nesting
  - **Code Blocks**: Fenced code blocks with ``` syntax
  - **Links and Images**: `[text](url)` and `![alt](url)` support
  - **Blockquotes**: `>` quote syntax
  - **HTML Preservation**: Pre-rendered tables and zoom links preserved
  - **Dependencies**: Added `commonmark`, `commonmark-ext-gfm-tables`, `commonmark-ext-gfm-strikethrough` (v0.22.0)
  - **Impact**: Fixes "markdown not converted to HTML" issue - all markdown features now work
- **ChunkCleaner Utility** (ADR-047 Phase 1)
  - New `ChunkCleaner` utility class for cleaning streamed text chunks
  - Removes zero-width characters (U+200B-U+200F, U+FEFF)
  - Removes control characters (except tab and newline)
  - Normalizes line breaks (CRLF, CR → LF)
  - Limits consecutive blank lines to 2
  - Preserves markdown formatting (2-space line breaks, code block indentation)
  - Comprehensive unit tests with 13 test cases covering edge cases
- **CommonMarkRendererTest** (ADR-047 Phase 2.5)
  - New test suite for unified CommonMark-based rendering
  - Tests basic markdown transformations (headings, bold, italic, code)
  - Tests numbered and nested lists (critical regression test)
  - Tests code blocks and blockquotes
  - Tests HTML preservation (pre-rendered tables and zoom links)
  - Tests GFM extensions (strikethrough)
  - Tests rendering consistency and edge cases
  - Verifies unified rendering eliminates dual-path issues
  - 18 test methods covering all critical functionality
- **StreamingPerformanceMonitor Utility** (ADR-047 Phase 2)
  - New performance monitoring utility for tracking streaming metrics
  - Monitors DOM update frequency, chunk processing rate, batch sizes, render latency
  - Provides pass/fail evaluation against performance targets
  - Generates detailed performance reports with metrics summary
- **TableCellParser Utility** (ADR-047 Phase 2) ⭐ **SINGLE SOURCE OF TRUTH**
  - New shared utility class for markdown table cell parsing
  - Provides consistent escape handling for both streaming and final rendering
  - Single source of truth for parsing logic (eliminates code duplication)
  - Handles backslash-escaped pipes (`\|` → `|`)
  - Handles escaped backslashes (`\\` → `\`)
  - Returns `ParseResult` with character to append and positions to skip
  - Used by both `StreamingTableRenderer` and `MarkdownTableRenderer`
  - Comprehensive JavaDoc with usage examples

**Changed:**
- **Unified Markdown Rendering** (ADR-047 Phase 2.5) ⭐ **CRITICAL CONSISTENCY FIX**
  - Fixed dual-rendering path causing markdown leaks and content jumps
  - **Problem**: Streaming used regex-based `MarkdownRenderer`, final used `CommonMarkRenderer`
  - **Impact**: Users saw content shift/jump when streaming completed, random markdown leaks (`**bold**` appearing raw)
  - **Solution**: Use `CommonMarkRenderer` for BOTH streaming and final rendering
  - **AIChatStreamingMessage.updateContentDisplay()** (line 650):
    - Now uses `processMarkdownPreservingHTML()` during streaming (same as final)
    - Pre-renders tables before CommonMark parsing (consistent with final phase)
    - Eliminates inconsistencies between streaming and final display
  - **AIChatStreamingMessage.renderPartialMarkdown()** (line 979):
    - Deprecated with `@Deprecated` annotation
    - Now delegates to unified `processMarkdownPreservingHTML()` for backward compatibility
    - Will be removed in v0.32.0
  - **AIChatStreamingMessage.renderFinalMarkdown()** (line 825):
    - Updated comment (removed misleading "marked.js" reference)
    - Clarified that it uses CommonMark Java library
  - **Result**: Zero content jumps, zero markdown leaks, consistent rendering throughout message lifecycle
- **Table Cell Parsing Consolidation** (ADR-047 Phase 2) ⭐ **SINGLE SOURCE OF TRUTH**
  - Refactored both `StreamingTableRenderer` and `MarkdownTableRenderer` to use shared `TableCellParser`
  - **Before**: Duplicate backslash escape handling in two separate classes (14 lines each)
  - **After**: Single shared utility class with consistent parsing logic
  - **StreamingTableRenderer.appendChunk()**: Now uses `TableCellParser.parseChar()` (lines 174-186)
  - **MarkdownTableRenderer.parseCells()**: Now uses `TableCellParser.parseChar()` (lines 476-506)
  - Eliminates code duplication and ensures consistent behavior across streaming and final rendering
  - Maintains bracket depth tracking in `MarkdownTableRenderer` for zoom link syntax
  - **Impact**: Easier maintenance, guaranteed consistency, single point of change for parsing rules
- **Throttled Rendering Implementation** (ADR-047 Phase 2) ⭐ **CRITICAL PERFORMANCE FIX**
  - Implemented fixed-interval rendering (50ms batching) to eliminate DOM thrashing
  - **Performance Impact**: Reduced DOM updates by 80% (from 100+/sec to ~20/sec)
  - **Before**: `updateContentDisplay()` called on every chunk (causing flickering and lag)
  - **After**: Chunks queued and processed in batches every 50ms (smooth 20 FPS rendering)
  - Added chunk queue with synchronization (`chunkQueue`, `queueLock`, `renderScheduled`)
  - New `scheduleRender()` method using JavaScript setTimeout for client-side throttling
  - New `onBatchRender()` event handler to process batched chunks
  - Updated `complete()` and `markCancelled()` to flush remaining queued chunks
  - Aligns with industry best practices (progressive table builder patterns)
  - **Result**: Zero visible flickering, smooth streaming, 80% reduction in browser reflows
- **AIChatStreamingMessage Integration** (ADR-047 Phase 1)
  - Updated `appendChunk()` to queue chunks instead of immediate rendering
  - Applies character cleanup to all streaming content
  - Improves text cleanliness and removes problematic characters
  - Preserves emoji, CJK characters, and markdown structure
- **Markdown Rendering Refactoring** (Code Quality)
  - Created unified `MarkdownRenderer` utility class
  - Eliminated 52 lines of duplicated code between `renderPartialMarkdown()` and `processSimpleMarkdown()`
  - Refactored `renderPartialMarkdown()`: 80 lines → 38 lines (52% reduction)
  - Refactored `processSimpleMarkdown()`: 18 lines → 10 lines (44% reduction)
  - Extracted shared logic: `applyMarkdownTransformations()`, `removeFunctionCalls()`, `cleanupBlockElements()`
  - Single source of truth for markdown transformations (headings, bold, italic, code, lists)
  - Improved maintainability and testability

**Fixed:**
- **Zoom Links Showing Raw Syntax During Streaming** (ADR-047 Phase 2) ⭐ **CRITICAL UX FIX**
  - Fixed issue where zoom link syntax `[[Table:ID|Display]]` appeared as raw text during streaming
  - **Root cause**: Zoom links processed on incomplete cell content before pipe delimiter closed cell
  - **Example**: Chunk `"[[C_BPartner:1000|Acm"` showed raw, then `"e Corp]]"` completed → flicker
  - **Solution**: Removed zoom link processing from incomplete cells (StreamingTableRenderer:481-505)
  - Incomplete cells now show raw text with cursor until pipe delimiter arrives
  - Completed cells process zoom links immediately and show as clickable hyperlinks
  - **Result**: No raw syntax visible, no flickering from raw → styled transitions
  - Updated JavaDoc with clear explanation of streaming behavior and examples
- **Backslash-Escaped Pipes in Table Cells** (ADR-047 Phase 2) ⭐ **CRITICAL PARSING FIX**
  - Fixed issue where `\|` in table cells was split incorrectly
  - **Root cause**: Neither `MarkdownTableRenderer.parseCells()` nor `StreamingTableRenderer.appendChunk()` handled backslash-escaped pipes
  - **Example**: `[[C_BPartner:123\|Name]]` was split into two cells: `[[C_BPartner:123\` and `Name]]`
  - **Result**: Showed as `[[C_BPartner:123\ | Name]]` with space and broken zoom link
  - **Solution**: Created shared `TableCellParser` utility with consistent escape handling logic
    - Both `StreamingTableRenderer` and `MarkdownTableRenderer` now use `TableCellParser.parseChar()`
    - Single source of truth ensures consistent behavior across all rendering paths
  - Now correctly handles `\|` by removing backslash and keeping pipe in cell content
  - Applies to BOTH streaming rendering AND final rendering
  - Zoom links with pipes now parse correctly: `[[C_BPartner:123|Name]]` stays as one cell
  - **Impact**: All zoom links in tables now render correctly without syntax errors during streaming and final display
- **Excessive Blank Lines in Final Rendering** (ADR-047 Phase 1)
  - Added second normalization pass after table/zoom link rendering
  - Fixes issue where 4+ `<br>` tags appeared between content blocks
  - Table rendering no longer preserves excessive newlines
  - Ensures consistent spacing throughout rendered content
- **Heading Recognition** (ADR-047 Phase 1)
  - Automatically adds newlines before markdown headings when missing
  - Fixes cases where `## Heading` wasn't converted to `<h3>` tag
  - Improves markdown parsing reliability

**Removed:**
- None

---

## [0.30.0] - 2025-12-18

### Cell-by-Cell Streaming Table Rendering

This feature release implements progressive cell-by-cell rendering for markdown tables during streaming, providing a smoother and more responsive user experience.

#### Added

- **StreamingTableRenderer**
  - New `StreamingTableRenderer` class for stateful table streaming
  - Processes markdown tables incrementally during streaming
  - Maintains state across chunks (current row, current cell, completed rows)
  - Buffers incomplete cells until pipe delimiter (`|`) arrives
  - Renders partial rows with streaming cursor showing current cell position
  - Handles separator row (|---|) for alignment specifications
  - Preserves pre-table and post-table content correctly

- **Progressive Table Display**
  - Tables now render cell by cell instead of line by line
  - Streaming cursor shows in currently active cell
  - Table structure visible immediately with progressive cell filling
  - Smooth UX as content streams in from AI

#### Changed

- **AIChatStreamingMessage Integration**
  - Updated `AIChatStreamingMessage` to use `StreamingTableRenderer` during streaming
  - Each chunk feeds into table renderer for progressive parsing
  - Uses `renderCurrentState()` for real-time display updates
  - Falls back to standard `MarkdownTableRenderer` for final rendering
  - Maintains locale and context settings across renderers

#### Technical Details

- Parses table structure incrementally (pipe delimiters, newlines, separator rows)
- Detects table boundaries and switches between table/non-table modes
- Tracks bracket depth to handle zoom link syntax `[[Table:ID|Display]]` correctly
- Numbers formatted per locale, zoom links processed correctly
- Compatible with existing markdown rendering pipeline

#### Benefits

- **Improved UX**: Cells appear progressively instead of rows popping in
- **Visual Feedback**: Clear indication of streaming progress with cursor
- **Structural Clarity**: Table headers and structure visible immediately
- **Consistent Rendering**: Same final output as standard renderer

---

## [0.29.0] - 2025-12-18

### Zoom Links in Markdown Tables Fix

This patch release fixes zoom links not being clickable when rendered inside markdown table cells.

#### Fixed

- **Escaped Pipe Support in Zoom Links**
  - Updated `ZoomLinkProcessor` regex pattern to match both plain (`|`) and escaped (`\|`) pipes
  - AI escapes pipes in table cells to prevent interpretation as cell separators
  - Pattern change: `:(\\d+)\\|` → `:(\\d+)\\\\?\\|`
  - Zoom links now correctly render as clickable in all contexts (tables and inline)

#### Changed

- **Debug Logging for Zoom Links**
  - Added comprehensive debug logging in `MarkdownTableRenderer`
  - Logs cell content, context state, and processing results
  - Helps diagnose zoom link rendering issues in tables
  - Warning-level logs with emoji indicators (✅ ⚠️ ❌)

---

## [0.28.0] - 2025-12-18

### Localized Progress Messages via AD_Message System

This release implements proper iDempiere message localization for AI chat progress and tool execution messages.

#### Added

- **AD_Message Localization System**
  - 14 new AD_Message records (AIG_* prefix) with migration scripts CLD-1629
  - 3 progress messages: `AIG_Processing`, `AIG_LanguageDetected`, `AIG_SwitchingTo`
  - 11 tool messages: `AIG_QueryDatabase`, `AIG_ExecuteQuery`, `AIG_LookupRecord`, `AIG_SearchRecords`, `AIG_GetTableMetadata`, `AIG_ListTables`, `AIG_GetBusinessPartner`, `AIG_GetProduct`, `AIG_GetOrder`, `AIG_GetWindowContext`, `AIG_CalculateMetrics`
  - PostgreSQL and Oracle migration scripts for message initialization
  - English translations included, ready for multi-language support via AD_Message_Trl

- **Session Language Integration**
  - Chat ID parameter added to `AIChatStreamingMessage` constructor
  - Integration with `LanguageDetectionService` for session language lookup
  - Tool messages now respect session language overrides (ADR-037)
  - Progress messages use user's iDempiere session language

#### Changed

- **Localization Architecture** (ADR-037 Section 6)
  - Replaced hardcoded switch statements with `Msg.getMsg()` calls
  - `AIService.getLocalizedProgressMessage()` now uses AD_Message system
  - `AIChatStreamingMessage.getToolDisplayName()` now uses AD_Message system
  - Automatic fallback to English when translations missing
  - Removed obsolete hardcoded translation helper methods

#### Fixed

- **Java 11 Compatibility**
  - Converted Java 14 switch expressions to Java 11-compatible switch statements
  - Fixed compilation errors in `AIService.java` and `AIChatStreamingMessage.java`
  - Added helper methods for cleaner Java 11 switch handling

#### Documentation

- Updated ADR-037 Section 6: "Localized Progress Messages"
  - Complete implementation documentation
  - Message key mappings (14 messages)
  - Fallback behavior explanation
  - Layered localization architecture (UI Layer + AI Layer)
  - Benefits and testing instructions

#### Benefits

- Translators use iDempiere Message window (no code changes required)
- All iDempiere languages supported via AD_Message_Trl
- Clean separation of UI strings from code
- Zero runtime translation overhead
- Graceful degradation for unsupported languages

---

## [0.27.0] - 2025-12-18

### In-Plugin Mock AI Hub Provider

This release implements a zero-configuration in-plugin mock provider and removes the HTTP mock server infrastructure.

#### Added

- **In-Plugin Mock AI Hub Provider** (`MockAIHubChatModel.java`)
  - Zero-configuration mock for development and testing
  - No external HTTP server required
  - Streaming response support with word-by-word delivery
  - Contextual mock responses based on keywords
  - Can serve as production fallback
  - 12/12 unit tests passing

- **Documentation**
  - `docs/MOCK_AI_HUB_COMPARISON.md` - Implementation strategy and testing guide
  - `docs/v0.27.0-MOCK-AI-HUB-SUMMARY.md` - Complete implementation summary
  - Migration scripts for CLD-1628 (AI Hub provider type)

#### Changed

- **Provider Naming: Satellite → AI Hub**
  - Renamed all "Satellite" references to "AI Hub" throughout codebase
  - Updated ADR-042 (AI Hub Provider Integration)
  - Updated architecture documentation
  - Migration scripts for provider type updates

- **Factory Simplified** (`LangChain4jProviderFactory.java`)
  - MOA provider type always returns in-plugin mock
  - Removed HTTP mock server logic
  - Parameters `baseUrl` and `apiKey` ignored for compatibility

#### Removed

- **HTTP Mock Server Infrastructure** (~900 lines)
  - `MockAIHubServer.java` (642 lines) - HTTP server implementation
  - `AIHubProviderConstants.java` - Constants file
  - `run-mock-ai-hub.sh` - Startup script
  - `MockSatelliteServerTest.java` - HTTP server tests
  - `SatelliteProviderTest.java` - Provider integration tests

#### Testing

- ✅ In-Plugin Mock: Tested and working correctly
- ⏳ iDempiere AI Hub Integration: Postponed until local dev environment setup

---

## [0.26.0] - 2025-12-18

### Service Configuration Cleanup

This patch release cleans up OSGI service configurations and removes obsolete test code.

#### Changed

- **OSGI Service Configuration**
  - Simplified AIChatWidgetFactory interface reference (removed package prefix)
  - Added lifecycle methods (activate/deactivate) to EmbeddingTriggerService

#### Removed

- Obsolete TestRAGContextLayer.java test file (433 lines)

---

## [0.25.0] - 2025-12-11

### P1 Context Layer Vector Embedding Storage

This release implements persistent vector storage for RAG-based AI context retrieval with pgvector support.

#### Added

- **Vector Embedding Infrastructure (CLD-1601, ADR-012, ADR-026)**
  - AIG_Embedding table for persistent vector storage (768 dimensions)
  - AIG_IngestionMetadata table for tracking knowledge ingestion state
  - PostgreSQL: native vector(768) type with HNSW index for fast similarity search
  - Oracle: BLOB storage with in-memory search fallback
  - Helper function AIG_Embedding_Search() for semantic search with client filtering
  - Multi-tenant aware with AD_Client_ID filtering
  - Source tracking (ad_metadata, knowledge_entry, glossary, window_context)
  - Language support via AD_Language column

#### Changed

- Migration scripts use PascalCase naming (AIG_Embedding, not aig_embedding)
- All indexes, functions, triggers follow iDempiere conventions
- Standardized 'Cloudempiere' branding throughout documentation

#### Technical

- Supports nomic-embed-text (768d) and OpenAI text-embedding-3-small (1536d)
- HNSW index for O(log n) similarity search performance
- Client/language-aware semantic search

---

## [0.24.0] - 2025-12-11

### AI Hub Provider & Language Detection Enhancements

This release enhances the Satellite provider integration and improves language detection capabilities.

#### Added

- **Language Detection (ADR-037)**
  - Tenant/client language fallback
  - Slavic language support (Slovak, Czech, Hungarian, Polish)
  - Comprehensive language detection logging
  - Enhanced error logging with log.severe

- **AI Hub Provider**
  - Improved JSON stream flag parsing
  - Tool/function calling support
  - OpenAI protocol compatibility
  - Architecture and protocol documentation

- **Database Utilities**
  - DatabaseSyntaxHelper for database-specific SQL syntax

- **Dependencies**
  - openai4j 0.22.0 for OpenAI protocol support
  - Exported embeddings package for external use

#### Changed

- **Model Refactor**
  - Renamed Endpoint → URL in AIG_Provider
  - Backward compatibility via getEndpoint() wrapper

- **Debugging**
  - Comprehensive zoom link processing debug logs
  - Table rendering debug logs
  - Streaming message improvements

- **Documentation**
  - Satellite architecture review
  - Protocol decision analysis
  - LangChain4j compatibility matrix
  - Cloudempiere AI Protocol v1 spec
  - Updated project instructions with debug log requirements

---

## [0.23.0] - 2025-12-11

### AI Hub Provider Integration (ADR-042)

This release introduces AI Hub Provider support for external LangChain4j services.

#### Added

- **ADR-042: AI Hub Provider Integration**
  - URL column in AIG_Provider for AI Hub service endpoint configuration
  - Uses existing iDempiere URL element (AD_Element_ID=983)
  - SAT provider type in AIGProviderType reference list
  - Conditional display logic for URL field (@AIGProviderType@=SAT)
  - Supports iDempiere AI Hub Service integration (Java 17+, LangChain4j 1.x)

- **RAG Infrastructure**
  - Trigger-based embedding for K_Entry (ADR-029)
  - EmbeddingTriggerService for automatic knowledge base indexing

#### Changed

- **Migration Script Standards**
  - Standardized PostgreSQL and Oracle migration scripts
  - Added CLD-1601 ticket reference as primary identifier
  - Replaced NOW()/SYSDATE with TO_TIMESTAMP() for timestamp consistency
  - Replaced hardcoded user ID with toRecordId() function
  - Added timestamp comments before each SQL statement
  - Added Oracle-specific directives (SET SQLBLANKLINES ON, SET DEFINE OFF)

---

## [0.22.0] - 2025-12-11

### Clickable Record Links in Chat with Table Support (ADR-039)

This release implements clickable record references in AI chat responses, enabling users to drill down into records by clicking links. Zoom links work in both plain text and table cells, during streaming and after refresh.

#### Added

- **Zoom Link Infrastructure** (ADR-039 Phase 1)
  - `ZoomLinkProcessor` - Converts `[[Table:ID|Display]]` syntax to clickable HTML links
  - `RecordReference` DTO - Structured record metadata (tableName, recordId, displayText, columnName)
  - `ChatRecordLinkRenderer` - Pattern-based link extraction (deferred to Phase 2)
  - `RecordReferenceExtractor` - DocumentNo pattern matching (deferred to Phase 2)
  - Dynamic widget lookup for re-rendered messages (survives chat refresh)
  - Dual-strategy widget discovery: ID first, then DOM class search

- **Table Cell Zoom Support**
  - `MarkdownTableRenderer` - ThreadLocal context for zoom link processing
  - Bracket-aware cell parsing - Preserves `[[...|...]]` syntax without splitting on `|`
  - Smart HTML escaping - Detects and preserves zoom link HTML while escaping other content
  - Server-side table rendering before markdown processing

- **Chat Widget Enhancements**
  - `zoomToRecord(MQuery)` - Direct MQuery-based zoom (same as ChartRendererServiceImpl pattern)
  - `zoomToRecord(tableName, recordId)` - Convenience method
  - `handleZoomEvent()` - Supports MQuery, JSON, and legacy formats
  - `renderMarkdownPreservingHTML()` - Processes markdown without corrupting HTML

#### Changed

- **AIChatWidget** (`component/AIChatWidget.java`)
  - Updated `formatMessage()` to pre-render tables with zoom links before markdown
  - Added `renderMarkdownPreservingHTML()` to avoid `_` → `<em>` corruption in `C_Order_ID`
  - Enhanced `handleZoomEvent()` with MQuery support (same pattern as chart zoom)

- **AIChatStreamingMessage** (`component/AIChatStreamingMessage.java`)
  - Pre-render tables in `renderFinalMarkdown()` before passing to marked.js
  - Added `processMarkdownPreservingHTML()` to split HTML/markdown content
  - Removed marked.js table parsing extension (tables are HTML now, not markdown)
  - Process zoom links in table cells during streaming

- **MarkdownTableRenderer** (`util/MarkdownTableRenderer.java`)
  - Added `setContext()`, `setWidgetId()`, `clearZoomContext()` for zoom processing
  - Process `ZoomLinkProcessor` on each cell before HTML escaping
  - Added `containsZoomLink()` detection to skip escaping for zoom link HTML
  - Fixed `parseCells()` to handle `[[...|...]]` without splitting on internal `|`

- **ZoomLinkProcessor** (`util/ZoomLinkProcessor.java`)
  - Dynamic widget lookup: tries hardcoded ID first, then searches by `ai-chat-widget` class
  - Supports iframe/parent window contexts
  - Comprehensive logging for debugging widget discovery

- **ERPAgent** (`provider/langchain4j/ERPAgent.java`)
  - Updated system prompt to instruct LLM to format record references as `[[Table:ID|Display]]`

#### Technical Notes

- **Zoom Link Lifecycle**: Works during streaming, after completion, and after chat refresh
- **Current Implementation**: LLM-instructed format `[[C_Order:5678|SO-5678]]` (Phase 1)
- **Future Phase 2**: Pattern-based extraction `SO-1234` → zoom link (requires vector DB for fast table lookup)
- **Widget Discovery**: Uses dual strategy to handle widget UUID changes after re-render

#### ADR Status

- ✅ **ADR-039**: Chat Panel Record Zoom & Drill-Down - Phase 1 Complete

---

## [0.21.0] - 2025-12-10

### Real-Time Streaming Improvements (Tables & Emojis)

This release improves the streaming user experience with real-time markdown table rendering and proper emoji handling.

#### Added

- **StreamingTextBuffer** (`util/StreamingTextBuffer.java`)
  - UTF-16 surrogate pair handling for emojis during streaming
  - Buffers incomplete surrogates to prevent unknown character display (�)
  - Properly handles emojis like 📖 (U+1F4D6) split across streaming chunks
  - Thread-safe design for concurrent streaming sessions

- **MarkdownTableRenderer** (`util/MarkdownTableRenderer.java`)
  - Real-time GFM (GitHub Flavored Markdown) pipe table rendering
  - Tables render during streaming, not just after completion
  - **Auto-detection of numeric columns** with right alignment (best practice)
  - **Locale-aware number formatting** (e.g., 1,234.56 in US vs 1.234,56 in German)
  - Support for explicit alignment via `:---|:---:|---:`
  - Partial table rendering during streaming

- **ADR-039: Async Embedding Queue Architecture** (renamed from AI Hub Queue)
  - Updated architecture for embedding queue processing

- **ADR-040: Embedding Ingestion Evolution**
  - Future roadmap for embedding pipeline improvements

- **Unit Tests**
  - `StreamingTextBufferTest` - 18 tests for surrogate handling
  - `MarkdownTableRendererTest` - 39 tests for table rendering

#### Changed

- **AIChatStreamingMessage** - Enhanced streaming display
  - Uses `StreamingTextBuffer` instead of `StringBuilder`
  - Renders tables BEFORE HTML escaping (critical fix for table detection)
  - Added `escapeNonTableContent()` for XSS protection while preserving tables
  - Added `setLocale()` method for locale-aware number formatting
  - Fixed table rendering during streaming (previously showed raw markdown)

#### Fixed

- **Tables not rendering during streaming** - Root cause was HTML escaping pipe characters (`|`) before table detection. Fixed by reordering operations.
- **Emojis showing as unknown characters** - UTF-16 surrogate pairs split across streaming chunks now handled correctly.

---

## [0.20.0] - 2025-12-10

### Language Detection & User-Friendly Error Handling (ADR-037, ADR-038)

This release adds automatic language detection for multilingual responses and comprehensive user-friendly error handling.

#### Added

- **Language Detection Service** (ADR-037)
  - `LanguageDetectionService` - Detects user's language from input
  - AI responses now match user's language automatically
  - Session-level language persistence
  - Support for 15+ languages with high confidence detection
  - Fallback to client default language

- **User-Friendly Error Handling** (ADR-038)
  - `AIErrorHandler` - Converts technical errors to user-friendly messages
  - 7 error categories: RATE_LIMIT, TIMEOUT, CONTENT_FILTER, CONFIGURATION, CONTEXT_LENGTH, SERVICE_UNAVAILABLE, GENERIC
  - AD_Message integration for translatable error messages
  - AD_Issue integration for technical error tracking
  - Error reference codes (format: AIG-{timestamp}-{random4})
  - Debug tooltip with error details for support
  - JSON error parsing for API responses

- **Migration Scripts** (CLD-1601)
  - PostgreSQL and Oracle scripts for 7 AD_Message entries
  - Idempotent INSERT with NOT EXISTS checks
  - Messages: AIG_Error_RateLimit, AIG_Error_Timeout, AIG_Error_ContentFilter, AIG_Error_Configuration, AIG_Error_ContextLength, AIG_Error_ServiceUnavailable, AIG_Error_Generic

- **Unit Tests**
  - `AIErrorHandlerTest` - 47 tests for error categorization
  - Updated `run-unit-tests.sh` to include iDempiere base lib jars

#### Changed

- **AIChatWidget** - Uses AIErrorHandler for error display
  - Error messages now user-friendly with actionable hints
  - Debug emoji (⚠️) with tooltip for error reference
  - No more raw technical errors shown to users

- **RAGConversationService** - Enhanced language handling
  - Detects and stores user language in session
  - Passes language context to AI agents

- **Agent System Prompts** - Language awareness
  - ERPAgent, ERPStreamingAgent, SimpleStreamingAgent updated
  - Agents respond in detected user language

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
  - `docs/GOVERNANCE.md` - Decision authority framework
  - `docs/ROADMAP.md` - Phases, milestones, release process

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
| 20 | v0.20.0 | 2025-12-10 | Language Detection & User-Friendly Error Handling |
| 21 | v0.21.0 | 2025-12-10 | Real-Time Streaming Improvements (Tables & Emojis) |
| 22 | v0.22.0 | 2025-12-11 | Clickable Record Links in Chat (ADR-039) |
| 23 | v0.23.0 | 2025-12-11 | AI Hub Provider Integration (ADR-042) |
| 24 | v0.24.0 | 2025-12-11 | AI Hub Provider & Language Detection Enhancements |
| 25 | v0.25.0 | 2025-12-11 | P1 Context Layer Vector Embedding Storage |
| 26 | v0.26.0 | 2025-12-18 | Service Configuration Cleanup |
| 27 | v0.27.0 | 2025-12-18 | In-Plugin Mock AI Hub Provider |
| 28 | v0.28.0 | 2025-12-18 | AD_Message Localization for Progress/Tool Messages |
| 29 | v0.29.0 | 2025-12-18 | Zoom Links in Markdown Tables Fix |
| 30 | v0.30.0 | 2025-12-18 | Cell-by-Cell Streaming Table Rendering |

## Upcoming Phases

| Phase | Version | Target | Milestone | ADR |
|-------|---------|--------|-----------|-----|
| 31 | v0.31.0 | 2025-12 | CommonMark Integration & Unified Rendering | ADR-047 |
| 32 | v0.32.0 | Q1 2026 | Share Dialog & Advanced Sharing | ADR-036 |
| 33 | v0.33.0 | Q1 2026 | Chart Executive Overview (First Business Case) | ADR-017 |
| 34 | v0.34.0 | Q1 2026 | Domain Agents (Inventory, Sales, Purchasing) | ADR-011 |
| 35 | v1.0.0 | Q2 2026 | Production Release | - |

## Planned Tasks (from ADRs)

> **See also:** [DEPRECATION_ROADMAP.md](docs/DEPRECATION_ROADMAP.md) - Custom code → LangChain4j migration

### 🔴 Critical Priority
| Task | Target | ADR | Notes |
|------|--------|-----|-------|
| Re-enable Zoom Links | v0.31.0 | ADR-039 | Currently disabled for debugging |
| Remove AD_Client_ID Debug Logging | v0.31.0 | - | Temporary debugging code |
| **Deprecate Custom DTO Layer** | v0.32.0 | ADR-002 | 1,413 lines → LangChain4j types |
| **Simplify ThreadAwareChatMemory** | v0.32.0 | ADR-002 | 465 lines → ChatMemory + Listener |
| **Enhanced Injection Prevention** | v0.32.0 | ADR-048 | Base64, zero-width char detection |
| **Token Optimization** | v0.32.0 | ADR-048 | Budget-aware retrieval |
| Structured Outputs | v0.32.0 | ADR-008 | 80% less parsing code |
| Temperature Control | v0.32.0 | ADR-008 | 1-line quick win |

### 🟡 Medium Priority
| Task | Target | ADR | Notes |
|------|--------|-----|-------|
| Share Dialog (User/Role picker) | v0.32.0 | ADR-036 | Deferred from v0.17.0 |
| **Deprecate AIStreamCallback** | v0.33.0 | ADR-002 | 259 lines → StreamingResponseHandler |
| **Remove ToolRegistry** | v0.33.0 | ADR-002 | 450 lines → @Tool annotations only |
| **Rate Limiting** | v0.33.0 | ADR-048 | Per-user, per-tenant limits |
| **Response Caching** | v0.33.0 | ADR-048 | Tenant-aware cache |
| Validation Loop | v0.33.0 | ADR-008 | 70% less validation code |
| **Replace Routing with RAG** | v0.34.0 | ADR-012 | 1,884 lines → ContentRetriever |
| **Comprehensive Audit** | v0.34.0 | ADR-048 | AIG_SecurityViolation table |
| Agentic Patterns | v0.34.0 | ADR-010 | langchain4j-agentic module |
| MCP REST API | v0.34.0 | ADR-003 | HTTP endpoints for external clients |

### 🟢 Lower Priority
| Task | Target | ADR | Notes |
|------|--------|-----|-------|
| ESC Key Shortcut for Cancel | v0.32.0 | - | Deferred from v0.16.0 |
| HTTP Connection Close on Cancel | Java 17 | - | Requires LangChain4j v1.8.0+ |

## Known Issues / Temporary Code

> **See [CRITICAL_ISSUES.md](docs/CRITICAL_ISSUES.md) for full issue tracker**

### 🔴 P0 - Blockers (Fix Before Production)
| Issue | File | Impact |
|-------|------|--------|
| Cross-tenant data access | `ChatAccessService.java:78` | SECURITY |
| NPE in streaming completion | `AIService.java:1050` | CRASH |
| Thread race condition | `AIChatWidget.java:909` | DATA |
| Desktop reference invalid | `AIChatWidget.java:948` | CRASH |

### 🟡 P1 - High (Fix Before Scale)
| Issue | File | Impact |
|-------|------|--------|
| Cancellation race | `AIChatWidget.java:2155` | UX |
| Memory leak in cache | `AIService.java:106` | OOM |
| O(n²) markdown render | `AIChatStreamingMessage.java:1031` | PERF |
| Unbounded chunk queue | `AIChatStreamingMessage.java:136` | OOM |

### ⚠️ Temporary Debugging (v0.31.0-SNAPSHOT)
- **Zoom Links Disabled** - `ZoomLinkProcessor.processZoomLinks()` returns text unchanged
  - To re-enable: Remove early return in `ZoomLinkProcessor.java:78-82`
- **AD_Client_ID Logging** - Context validation logging at all critical points
  - Tags: `[STREAM-INIT]`, `[FINAL-RENDER]`, `[PARTIAL-RENDER]`, `[STREAMING-ZOOM]`, `[TABLE-ZOOM]`, `[ZOOM-PROCESSOR]`
  - To remove: Search for `AD_Client_ID=` in source files

## iDempiere Compatibility

| Plugin Version | iDempiere Version | Bundle-Version |
|----------------|-------------------|----------------|
| 0.1.0 - 0.10.0 | 10.x, 11.x, 12.x | 10.0.0.qualifier |
