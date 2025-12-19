# LangChain4j Integration Flows

> Comprehensive documentation of LangChain4j integration in the com.cloudempiere.ai plugin.

**Version:** 0.20.0
**LangChain4j Version:** 0.35.0 (Java 11 compatible)
**Last Updated:** December 2025

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [Data Flows](#data-flows)
5. [Provider Factory](#provider-factory)
6. [Agent Interfaces](#agent-interfaces)
7. [ERP Tools](#erp-tools)
8. [Memory Management](#memory-management)
9. [Streaming Support](#streaming-support)
10. [Guardrails Pipeline](#guardrails-pipeline)
11. [Metrics & Observability](#metrics--observability)
12. [Code Examples](#code-examples)
13. [Configuration](#configuration)

---

## Overview

The com.cloudempiere.ai plugin integrates LangChain4j to provide AI-powered capabilities within the iDempiere ERP system. This integration enables:

- **Multi-provider LLM support**: Anthropic Claude, AWS Bedrock, OpenAI, Ollama, Llama (local)
- **Real-time streaming responses**: Token-by-token response delivery
- **Tool/Function calling**: AI can query the ERP database securely (supported providers only)
- **Thread-aware conversation memory**: Persistent chat history with thread support
- **Safety guardrails**: Input/output validation, cost control, rate limiting
- **Full observability**: Token usage, cost tracking, latency metrics
- **Model selection**: Configure model per provider via `AIG_Provider.ModelName`

### Key Design Decisions

| ADR | Description |
|-----|-------------|
| [ADR-002](adr/002-langchain4j-strategic-adoption.md) | LangChain4j Strategic Adoption |
| [ADR-031](adr/031-chat-panel-langchain4j-chatmodel-integration.md) | Chat Panel Integration |
| [ADR-033](adr/033-streaming-thinking-timeline-ux.md) | Streaming Responses UX |
| [ADR-035](adr/035-java-version-strategy.md) | Java 11 + LangChain4j 0.35.0 |

---

## Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           UI Layer (ZK Framework)                            │
│                              AIChatWidget                                    │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          AIService (Singleton)                               │
│                     STREAMING-FIRST ARCHITECTURE                             │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │ chatStreamingWithContext() ← Primary (streaming)                       │ │
│  │ chatBlocking()             ← Wrapper (CompletableFuture.join())        │ │
│  │ chatWithContext()          ← Legacy (calls streaming internally)       │ │
│  │ chat()                     ← Legacy simple method                      │ │
│  └────────────────────────────────┬───────────────────────────────────────┘ │
│                                   │                                          │
│  ┌────────────────────────────────▼───────────────────────────────────────┐ │
│  │                      Guardrails Pipeline                                │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────────┐  │ │
│  │  │ CostGuard    │  │ InputGuard   │  │ OutputGuard                  │  │ │
│  │  │ (budget)     │  │ (PII, inject)│  │ (leaks, harmful)             │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     LangChain4j Provider Factory                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ create() / createStreaming() / getOrCreate()                            ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                  │                                           │
│    ┌─────────────┬───────────────┼───────────────┬─────────────────┐        │
│    ▼             ▼               ▼               ▼                 ▼        │
│ ┌──────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────┐ ┌────────────────┐ │
│ │Anthropic │ │AWS       │ │OpenAI        │ │Ollama    │ │Metrics         │ │
│ │ChatModel │ │Bedrock   │ │ChatModel     │ │ChatModel │ │Listener        │ │
│ └──────────┘ └──────────┘ └──────────────┘ └──────────┘ └────────────────┘ │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    LangChain4j AiServices Framework                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ AiServices.builder(ERPStreamingAgent.class)                             ││
│  │   .streamingChatLanguageModel(model)                                    ││
│  │   .tools(ERPTools)        // Unified tools with optional callback       ││
│  │   .chatMemoryProvider(ThreadAwareChatMemory)                            ││
│  │   .build()                                                              ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              ▼                    ▼                    ▼
┌──────────────────────┐ ┌──────────────────┐ ┌──────────────────────────────┐
│   ERPTools           │ │ AIStreamCallback │ │ ThreadAwareChatMemory        │
│   (@Tool methods)    │ │ (optional)       │ │                              │
├──────────────────────┤ ├──────────────────┤ │ - Load from CM_ChatEntry     │
│ queryDatabase()      │ │ onChunk()        │ │ - Thread filtering           │
│ lookupRecord()       │ │ onToolStart()    │ │ - Persist messages           │
│ searchRecords()      │ │ onToolComplete() │ │ - Max message window         │
│ getTableMetadata()   │ │ onToolError()    │ └──────────────────────────────┘
│ listTables()         │ │ onError()        │
│ getBusinessPartner() │ │ onProgress()     │
│ getProduct()         │ │ onThinking()     │
│ getOrder()           │ └──────────────────┘
└──────────┬───────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                    SecureDatabaseQueryExecutor                                │
│  - Role-based access control (AD_Role permissions)                           │
│  - Query validation (SQL injection prevention)                               │
│  - Audit logging (AI user + initiating user)                                 │
└──────────────────────────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        iDempiere Database                                     │
│                      (PostgreSQL / Oracle)                                    │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
src/com/cloudempiere/ai/
├── provider/
│   └── langchain4j/
│       ├── AIService.java              # Main entry point (singleton)
│       ├── LangChain4jProviderFactory.java  # Model factory
│       ├── ERPAgent.java               # Non-streaming agent interface (with tools)
│       ├── ERPStreamingAgent.java      # Streaming agent interface (with tools)
│       ├── SimpleAgent.java            # Simple agent (fallback for streaming tools)
│       ├── SimpleStreamingAgent.java   # Simple streaming agent (Ollama/Llama streaming)
│       ├── ERPTools.java               # @Tool annotated methods
│       │   (ERPTools now supports optional callbacks)
│       └── ThreadAwareChatMemory.java  # Persistent chat memory
│   └── dto/
│       └── AIStreamCallback.java       # Streaming callback interface
├── guardrails/
│   ├── InputGuard.java                 # Input validation
│   └── OutputGuard.java                # Output filtering
├── observability/
│   ├── CostGuard.java                  # Budget/rate limiting
│   └── AIMetricsListener.java          # Token/cost tracking
└── database/
    └── SecureDatabaseQueryExecutor.java  # Secure query execution
```

---

## Core Components

### 1. AIService

**File:** `src/com/cloudempiere/ai/provider/langchain4j/AIService.java`

The main entry point for all AI capabilities. Implements a singleton pattern with three primary methods:

| Method | Description | Use Case |
|--------|-------------|----------|
| `chat()` | Simple chat with memory | Basic conversations |
| `chatWithContext()` | Chat with window context | AIChatWidget integration |
| `chatStreamingWithContext()` | Real-time streaming | Live response display |

**Key Features:**
- Applies guardrails pipeline (cost, input, output)
- Manages agent and memory caches
- Injects window/tab context into messages
- Records usage metrics

### 2. LangChain4jProviderFactory

**File:** `src/com/cloudempiere/ai/provider/langchain4j/LangChain4jProviderFactory.java`

Factory for creating LangChain4j model instances from iDempiere configuration.

**Supported Providers:**

| Provider | Chat Model | Streaming | Embedding | Tools |
|----------|-----------|-----------|-----------|-------|
| Anthropic (`ANT`) | `claude-sonnet-4-20250514` | ✅ | Via Bedrock | ✅ |
| AWS Bedrock (`BED`) | `claude-3-5-sonnet-v2` | ✅ | Titan Embed v2 | ✅ |
| OpenAI (`OAI`) | `gpt-4o` | ✅ | `text-embedding-3-small` | ✅ |
| Ollama (`OLL`) | `llama3.2` | ✅ | `nomic-embed-text` | ⚠️* |
| Llama (`LLA`) | `llama3.2` | ✅ | `nomic-embed-text` | ⚠️* |

**\*Ollama/Llama Tool Support Limitations (LangChain4j 0.35.0):**

| Mode | Tool Support | Notes |
|------|--------------|-------|
| Non-streaming | ✅ Supported | Works with `ERPAgent` |
| Streaming | ❌ Not supported | Uses `SimpleStreamingAgent` (no tools) |

Streaming tool support for Ollama/Llama requires **LangChain4j 0.37.0+** which requires **Java 17**. Since we're constrained to Java 11 (iDempiere v10), Ollama/Llama streaming uses `SimpleStreamingAgent` without tool access. For full tool support with streaming, use Anthropic, OpenAI, or AWS Bedrock providers.

**Model Selection:**

Configure the model via `AIG_Provider.ModelName` column:

| Provider | Default Model | Example Alternatives |
|----------|---------------|---------------------|
| Anthropic | `claude-sonnet-4-20250514` | `claude-3-opus`, `claude-3-haiku` |
| AWS Bedrock | `claude-3-5-sonnet-v2` | `claude-3-haiku`, `amazon.titan-text` |
| OpenAI | `gpt-4o` | `gpt-4o-mini`, `gpt-3.5-turbo` |
| Ollama | `llama3.2` | `llama3.1`, `phi3:mini`, `gemma2:2b` |
| Llama | `llama3.2` | `llama3.2:1b`, `codellama`, `llama2` |

### 3. Agent Interfaces

**Files:**
- `src/com/cloudempiere/ai/provider/langchain4j/ERPAgent.java` - With tools
- `src/com/cloudempiere/ai/provider/langchain4j/ERPStreamingAgent.java` - Streaming with tools
- `src/com/cloudempiere/ai/provider/langchain4j/SimpleAgent.java` - Fallback (non-streaming tools)
- `src/com/cloudempiere/ai/provider/langchain4j/SimpleStreamingAgent.java` - Ollama/Llama streaming (tools don't work in streaming mode)

LangChain4j service interfaces that define the agent contract:

```java
// ERPAgent - Full functionality with tools (Anthropic, OpenAI, Bedrock)
public interface ERPAgent {
    @SystemMessage(SYSTEM_PROMPT)  // Includes tool usage instructions
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}

// ERPStreamingAgent - Streaming with tools
public interface ERPStreamingAgent {
    @SystemMessage(SYSTEM_PROMPT)
    TokenStream chat(@MemoryId String sessionId, @UserMessage String userMessage);
}

// SimpleAgent - Basic chat without tools (Ollama/Llama)
public interface SimpleAgent {
    @SystemMessage(SIMPLE_SYSTEM_PROMPT)  // No tool instructions
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}

// SimpleStreamingAgent - Streaming without tools
public interface SimpleStreamingAgent {
    @SystemMessage(SIMPLE_SYSTEM_PROMPT)
    TokenStream chat(@MemoryId String sessionId, @UserMessage String userMessage);
}
```

**Agent Selection (in AIService):**

| Provider | Non-Streaming Agent | Streaming Agent |
|----------|---------------------|-----------------|
| Anthropic (`ANT`) | `ERPAgent` (tools) | `ERPStreamingAgent` (tools) |
| AWS Bedrock (`BED`) | `ERPAgent` (tools) | `ERPStreamingAgent` (tools) |
| OpenAI (`OAI`) | `ERPAgent` (tools) | `ERPStreamingAgent` (tools) |
| Ollama (`OLL`) | `ERPAgent` (tools) | `SimpleStreamingAgent` (no tools) |
| Llama (`LLA`) | `ERPAgent` (tools) | `SimpleStreamingAgent` (no tools) |

The selection is based on two methods in `AIService`:
- `supportsTools(provider)` - All major providers support tools in non-streaming mode
- `supportsStreamingTools(provider)` - Only `ANT`, `OAI`, `BED` support streaming tools in LangChain4j 0.35.0

**ERPAgent System Prompt Highlights:**
- Must use tools for all data queries (never hallucinate)
- Respects role-based security
- Available tools: `queryDatabase`, `lookupRecord`, `searchRecords`, etc.

**SimpleAgent System Prompt Highlights:**
- General ERP assistant without database access
- Explains limitations when asked about specific data
- Suggests using cloud providers for full functionality

### 4. ERPTools (Unified with Optional Callbacks)

**File:** `src/com/cloudempiere/ai/provider/langchain4j/ERPTools.java`

`@Tool` annotated methods exposed to the AI for function calling:

| Tool | Description |
|------|-------------|
| `queryDatabase(sql, maxRows, purpose)` | Execute SELECT queries |
| `lookupRecord(tableName, recordId)` | Get record by ID |
| `searchRecords(tableName, whereClause, maxRows)` | Search with WHERE |
| `getTableMetadata(tableName)` | Table structure info |
| `listTables(namePattern)` | List available tables |
| `getBusinessPartner(identifier)` | Customer/vendor lookup |
| `getProduct(identifier)` | Product lookup |
| `getOrder(identifier)` | Order lookup |

**ERPTools** supports optional streaming callbacks:
- `onToolStart(toolName, args)` - Tool execution started
- `onToolComplete(toolName, result)` - Tool completed
- `onToolError(toolName, error)` - Tool failed

**Constructor Options:**
```java
// Without callbacks (blocking use)
ERPTools tools = new ERPTools(provider, ctx);

// With callbacks (streaming use)
ERPTools tools = new ERPTools(provider, ctx, callback);
```

### 5. ThreadAwareChatMemory

**File:** `src/com/cloudempiere/ai/provider/langchain4j/ThreadAwareChatMemory.java`

Thread-isolated conversation memory that persists to `CM_ChatEntry`:

**Features:**
- Loads history from database on initialization
- Filters messages by thread (`CM_ChatEntryParent_ID`)
- Supports thread switching and creation
- Configurable max message window (default: 20)
- Distinguishes AI vs User messages by `AD_User_ID`

---

## Data Flows

### Non-Streaming Chat Flow

```
┌─────────────────┐
│  User Message   │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│                    PRE-REQUEST GUARDRAILS                    │
│  1. CostGuard.checkBudget(clientId, estimatedCost)          │
│  2. CostGuard.checkRateLimit(userId)                        │
│  3. InputGuard.validate(message)                            │
│     - Block if injection/PII detected                       │
│     - Mask sensitive data if needed                         │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    BUILD MEMORY                              │
│  ThreadAwareChatMemory.builder()                            │
│    .chat(chat)                                              │
│    .threadRootId(threadRootId)                              │
│    .maxMessages(20)                                         │
│    .aiUserId(provider.getAD_User_ID())                      │
│    .build()                                                 │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    INJECT CONTEXT                            │
│  if (contextData != null) {                                 │
│    contextPrompt = buildContextPrompt(contextData);         │
│    processedMessage = contextPrompt + "\n\n" + message;     │
│  }                                                          │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    CREATE AGENT                              │
│  ChatLanguageModel model = LangChain4jProviderFactory       │
│    .getOrCreate(provider);                                  │
│                                                             │
│  ERPTools tools = new ERPTools(provider, ctx);              │
│                                                             │
│  ERPAgent agent = AiServices.builder(ERPAgent.class)        │
│    .chatLanguageModel(model)                                │
│    .tools(tools)                                            │
│    .chatMemoryProvider(memoryId -> memory)                  │
│    .build();                                                │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    EXECUTE AGENT                             │
│  String response = agent.chat(sessionId, processedMessage); │
│                                                             │
│  [Agent may call tools like queryDatabase(), getOrder()     │
│   which go through SecureDatabaseQueryExecutor]             │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    POST-RESPONSE GUARDRAILS                  │
│  OutputGuard.validate(response)                             │
│    - Block if harmful content detected                      │
│    - Mask sensitive data leaks                              │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    RECORD METRICS                            │
│  MAIUsageMetrics.record(ctx, userId, roleId, providerId,    │
│    agentName, agentType, modelName, inputTokens,            │
│    outputTokens, costMicrodollars, latencyMs, sessionId)    │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────┐
│  ChatResult     │
│  (response,     │
│   threadRootId, │
│   warning)      │
└─────────────────┘
```

### Streaming Chat Flow

```
┌─────────────────┐
│  User Message   │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│         PRE-REQUEST GUARDRAILS (same as non-streaming)       │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    CREATE STREAMING AGENT                    │
│  StreamingChatLanguageModel streamingModel =                │
│    LangChain4jProviderFactory.createStreaming(provider);    │
│                                                             │
│  ERPTools tools = new ERPTools(provider, ctx, callback);    │
│    // Unified tools with optional callback for tool events  │
│                                                             │
│  ERPStreamingAgent agent = AiServices.builder(              │
│      ERPStreamingAgent.class)                               │
│    .streamingChatLanguageModel(streamingModel)              │
│    .tools(tools)                                            │
│    .chatMemoryProvider(memoryId -> memory)                  │
│    .build();                                                │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    START TOKEN STREAM                        │
│  TokenStream tokenStream = agent.chat(sessionId, message);  │
│                                                             │
│  tokenStream                                                │
│    .onNext(token -> {                                       │
│        responseAccumulator.append(token);                   │
│        callback.onChunk(token);  // Real-time to UI         │
│    })                                                       │
│    .onComplete(response -> {                                │
│        // Record metrics                                    │
│        // Apply output guardrails                           │
│        callback.onComplete();                               │
│    })                                                       │
│    .onError(error -> {                                      │
│        callback.onError(error);                             │
│    })                                                       │
│    .start();                                                │
└─────────────────────────────────────────────────────────────┘
                             │
          During streaming:  │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    TOOL EXECUTION CALLBACKS                  │
│  When AI calls a tool:                                      │
│    1. callback.onToolStart("queryDatabase", args)           │
│    2. [Tool executes via SecureDatabaseQueryExecutor]       │
│    3. callback.onToolComplete("queryDatabase", result)      │
│       OR callback.onToolError("queryDatabase", error)       │
└─────────────────────────────────────────────────────────────┘
```

### Tool Execution Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         AI Model Decision                                 │
│  "I need to query the database to answer this question"                  │
│  → Generates function call: queryDatabase(sql, maxRows, purpose)         │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    ERPTools.queryDatabase()                               │
│  @Tool("Execute a read-only SQL SELECT query...")                        │
│  public String queryDatabase(String sql, Integer maxRows, String purpose)│
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    SecureQueryRequest                                     │
│  request.setCtx(ctx);           // AD_Client_ID, AD_Org_ID, AD_Role_ID  │
│  request.setProviderId(providerId);  // For audit                        │
│  request.setSql(sql);                                                    │
│  request.setMaxRows(min(maxRows, 500));                                  │
│  request.setQueryPurpose(purpose);                                       │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    SecureDatabaseQueryExecutor                            │
│  1. Validate SQL (SELECT only, no injection)                             │
│  2. Apply AccessSqlParser for role-based filtering                       │
│  3. Execute query with role permissions                                  │
│  4. Create audit record (AI user + initiating user)                      │
│  5. Return SecureQueryResult (JSON rows or error)                        │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    Result Back to AI                                      │
│  → AI receives JSON array of rows                                        │
│  → AI formats response for user                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Provider Factory

### Creating Models

```java
// Get provider configuration from database
MAIProvider provider = new MAIProvider(ctx, providerId, trxName);

// Create non-streaming model (with caching)
ChatLanguageModel model = LangChain4jProviderFactory.getOrCreate(provider);

// Create streaming model
StreamingChatLanguageModel streamingModel =
    LangChain4jProviderFactory.createStreaming(provider, null, null);

// Create embedding model (for RAG)
EmbeddingModel embeddingModel =
    LangChain4jProviderFactory.createEmbeddingModel(provider);
```

### Model Configuration

| Provider | Default Model | Max Tokens | Temperature |
|----------|---------------|------------|-------------|
| Anthropic | `claude-sonnet-4-20250514` | 4096 | 0.7 |
| Bedrock | `claude-3-5-sonnet-v2` | 4096 | 0.7 |
| OpenAI | `gpt-4o` | - | 0.7 |
| Ollama | `llama3.2` | - | 0.7 |

### Caching

Models are cached by provider ID:

```java
// Cache stores
Map<Integer, ChatLanguageModel> modelCache
Map<Integer, StreamingChatLanguageModel> streamingModelCache
Map<Integer, EmbeddingModel> embeddingModelCache

// Clear all caches (e.g., after configuration change)
LangChain4jProviderFactory.clearCache();

// Evict specific provider
LangChain4jProviderFactory.evictFromCache(providerId);
```

---

## Agent Interfaces

### ERPAgent (Non-Streaming)

```java
public interface ERPAgent {

    String SYSTEM_PROMPT =
        "You are an intelligent assistant for the iDempiere ERP system.\n\n" +
        "CRITICAL RULE - ALWAYS USE TOOLS FOR DATA:\n" +
        "When the user asks about data, you MUST use the provided tools...\n\n" +
        "AVAILABLE TOOLS:\n" +
        "- queryDatabase: Execute SQL SELECT queries\n" +
        "- lookupRecord: Get a specific record by ID\n" +
        "- searchRecords: Search records with WHERE clause\n" +
        "...";

    @SystemMessage(SYSTEM_PROMPT)
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);

    @SystemMessage(SYSTEM_PROMPT)
    String execute(@UserMessage String goal);
}
```

### ERPStreamingAgent

```java
public interface ERPStreamingAgent {

    // Same system prompt as ERPAgent
    String SYSTEM_PROMPT = ERPAgent.SYSTEM_PROMPT;

    @SystemMessage(SYSTEM_PROMPT)
    TokenStream chat(@MemoryId String sessionId, @UserMessage String userMessage);

    @SystemMessage(SYSTEM_PROMPT)
    TokenStream execute(@UserMessage String goal);
}
```

### Building Agents

```java
// Non-streaming agent
ERPAgent agent = AiServices.builder(ERPAgent.class)
    .chatLanguageModel(model)
    .tools(new ERPTools(provider, ctx))
    .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
    .build();

// Streaming agent with thread-aware memory
ERPStreamingAgent streamingAgent = AiServices.builder(ERPStreamingAgent.class)
    .streamingChatLanguageModel(streamingModel)
    .tools(new ERPTools(provider, ctx, callback))
    .chatMemoryProvider(memoryId -> threadAwareChatMemory)
    .build();
```

---

## ERP Tools

### Tool Definitions

```java
@Tool("Execute a read-only SQL SELECT query against the iDempiere ERP database. " +
      "Results are filtered by user's role permissions. Returns JSON array of rows.")
public String queryDatabase(
    @P("SQL SELECT query to execute") String sql,
    @P("Maximum number of rows to return (default 50, max 500)") Integer maxRows,
    @P("Purpose of the query for audit logging") String purpose
)

@Tool("Look up a specific record by ID from an iDempiere table.")
public String lookupRecord(
    @P("Table name (e.g., C_Order, C_BPartner, M_Product)") String tableName,
    @P("Record ID to look up") int recordId
)

@Tool("Search for records in an iDempiere table using a WHERE clause.")
public String searchRecords(
    @P("Table name") String tableName,
    @P("WHERE clause without 'WHERE' keyword") String whereClause,
    @P("Maximum rows to return (default 50)") Integer maxRows
)

@Tool("Get metadata about an iDempiere database table.")
public String getTableMetadata(
    @P("Table name") String tableName
)

@Tool("List available iDempiere tables.")
public String listTables(
    @P("Filter by table name pattern (optional)") String namePattern
)

@Tool("Get details of a Business Partner by ID or search value.")
public String getBusinessPartner(
    @P("Business Partner ID or Value (search key)") String identifier
)

@Tool("Get details of a Product by ID or search value.")
public String getProduct(
    @P("Product ID or Value (search key)") String identifier
)

@Tool("Get details of an Order by DocumentNo or ID.")
public String getOrder(
    @P("Order ID or DocumentNo") String identifier
)
```

### Security Model

All tools go through `SecureDatabaseQueryExecutor`:

1. **Query Validation**: Only SELECT statements allowed
2. **Role-Based Filtering**: Queries filtered by `AD_Role_ID` permissions
3. **Audit Trail**: Records AI user ID and initiating user ID
4. **Row Limiting**: Maximum 500 rows per query

---

## Memory Management

### ThreadAwareChatMemory

```java
// Create memory for a chat thread
ThreadAwareChatMemory memory = ThreadAwareChatMemory.builder()
    .chat(chat)                    // Parent MChat
    .threadRootId(threadRootId)    // Thread root (0 = root level)
    .maxMessages(20)               // Message window size
    .persistMessages(false)        // Widget handles persistence
    .aiUserId(provider.getAD_User_ID())  // For role detection
    .build();

// Switch threads
memory.switchThread(newThreadRootId);

// Create new thread
memory.createNewThread();

// Get current thread
int threadId = memory.getCurrentThreadRootId();
```

### Message Loading

Messages are loaded from `CM_ChatEntry` table:

```sql
SELECT CM_ChatEntry_ID, CharacterData, AD_User_ID, CM_ChatEntryParent_ID
FROM CM_ChatEntry
WHERE CM_Chat_ID = ?
  AND (CM_ChatEntry_ID = ? OR CM_ChatEntryParent_ID = ?)
ORDER BY Created ASC
```

### Message Type Detection

```java
private ChatMessage convertToChatMessage(String content, int userId) {
    if (isAIUser(userId)) {
        return AiMessage.from(content);
    } else {
        return UserMessage.from(content);
    }
}

private boolean isAIUser(int userId) {
    if (aiUserId != null) {
        return aiUserId.equals(userId);
    }
    return userId == 0;  // System user fallback
}
```

---

## Streaming Support

### AIStreamCallback Interface

```java
public interface AIStreamCallback {

    // Core streaming events
    void onChunk(String chunk);
    void onComplete();
    void onError(Exception error);

    // Tool events
    default void onToolStart(String toolName, String arguments) {}
    default void onToolComplete(String toolName, String result) {}
    default void onToolError(String toolName, String error) {}

    // Extended thinking (future)
    default void onThinking(String thinkingChunk) {}
    default void onThinkingComplete() {}

    // Progress indicators
    default void onProgress(String stage, String detail) {}
}
```

### Creating Callbacks

```java
// Simple callback
AIStreamCallback callback = AIStreamCallback.simple(
    chunk -> System.out.print(chunk),
    () -> System.out.println("\nDone"),
    error -> error.printStackTrace()
);

// Full-featured callback
AIStreamCallback callback = AIStreamCallback.builder()
    .onChunk(chunk -> ui.appendText(chunk))
    .onComplete(() -> ui.markComplete())
    .onError(error -> ui.showError(error))
    .onToolStart((name, args) -> ui.showToolRunning(name))
    .onToolComplete((name, result) -> ui.showToolDone(name))
    .onProgress((stage, detail) -> ui.updateStatus(stage))
    .build();
```

### ERPTools Callback Support

Fires callback events when callback is provided:

```java
@Tool("Execute a read-only SQL SELECT query...")
public String queryDatabase(String sql, Integer maxRows, String purpose) {
    String toolName = "queryDatabase";
    String args = "{\"sql\": \"" + truncate(sql, 100) + "\", \"maxRows\": " + maxRows + "}";

    fireToolStart(toolName, args);  // callback.onToolStart() if callback != null
    long startTime = System.currentTimeMillis();

    try {
        // Execute query via SecureDatabaseQueryExecutor
        SecureQueryRequest request = new SecureQueryRequest();
        request.setCtx(ctx);
        request.setProviderId(provider.getAIG_Provider_ID());
        request.setSql(sql);
        request.setMaxRows(maxRows != null ? Math.min(maxRows, 500) : 50);

        SecureQueryResult result = executor.executeQuery(request);

        if (result.isSuccess()) {
            String resultStr = result.getRows().toString();
            long elapsed = System.currentTimeMillis() - startTime;
            fireToolComplete(toolName, truncate(resultStr, 200) + " (" + elapsed + "ms)");
            return resultStr;
        } else {
            fireToolError(toolName, result.getErrorMessage());
            return createErrorResponse(result.getErrorMessage());
        }
    } catch (Exception e) {
        fireToolError(toolName, e.getMessage());
        return createErrorResponse(e.getMessage());
    }
}
```

---

## Guardrails Pipeline

### Pre-Request Guards

```java
// 1. Cost Guard - Budget check
costGuard.checkBudget(clientId, ESTIMATED_COST_PER_REQUEST);

// 2. Cost Guard - Rate limit
costGuard.checkRateLimit(userId);

// 3. Input Guard - PII and injection detection
GuardResult inputResult = inputGuard.validate(message);
if (inputResult.isBlocked()) {
    return "I cannot process this request: " + inputResult.getBlockReason();
}
if (inputResult.wasModified()) {
    processedMessage = inputResult.getProcessedContent();
}
```

### Post-Response Guards

```java
// Output Guard - Leak and harmful content detection
GuardResult outputResult = outputGuard.validate(response);
if (outputResult.isBlocked()) {
    return "I apologize, but I cannot provide that response.";
}
if (outputResult.wasModified()) {
    response = outputResult.getProcessedContent();
}
```

### Guard Configuration

```java
// Enable/disable guardrails
aiService.setGuardrailsEnabled(true);

// Check budget status
CostGuard.BudgetStatus status = aiService.getBudgetStatus(clientId);

// Clear budget cache after configuration change
aiService.clearBudgetCache(clientId);
```

---

## Metrics & Observability

### AIMetricsListener

Added to chat models to capture metrics:

```java
var builder = AnthropicChatModel.builder()
    .apiKey(apiKey)
    .modelName(modelName)
    .listeners(List.of(new AIMetricsListener("anthropic", sessionId)));
```

### Metrics Recording

```java
MAIUsageMetrics.record(
    ctx,                    // iDempiere context
    userId,                 // AD_User_ID
    roleId,                 // AD_Role_ID
    providerId,             // AIG_Provider_ID
    "chat-streaming-tools", // agentName
    "STREAMING",            // agentType
    "claude-sonnet-4",      // modelName
    inputTokens,
    outputTokens,
    costMicrodollars,
    latencyMs,
    sessionId,
    "CHAT_STREAMING",       // requestType
    null                    // trxName
);
```

### Cost Calculation

```java
private static int calculateCostMicrodollars(String model, int inputTokens, int outputTokens) {
    // Pricing per 1M tokens
    if (model.contains("opus")) {
        inputRate = 15.0; outputRate = 75.0;
    } else if (model.contains("sonnet")) {
        inputRate = 3.0; outputRate = 15.0;
    } else if (model.contains("haiku")) {
        inputRate = 0.25; outputRate = 1.25;
    } else if (model.contains("gpt-4o")) {
        inputRate = 5.0; outputRate = 15.0;
    } else if (model.contains("ollama") || model.contains("llama")) {
        return 0;  // Local models are free
    }

    return (int) ((inputTokens / 1_000_000.0) * inputRate * 1_000_000 +
                  (outputTokens / 1_000_000.0) * outputRate * 1_000_000);
}
```

---

## Code Examples

### Basic Chat

```java
AIService aiService = AIService.getInstance();

// Get provider from database
MAIProvider provider = new MAIProvider(ctx, providerId, null);

// Simple chat
String response = aiService.chat(provider, ctx, "session-123", "Show me pending orders");
```

### Chat with Context (Widget Integration)

```java
// Build context from current window
JSONObject contextData = new JSONObject();
contextData.put("success", true);
contextData.put("window_metadata", new JSONObject()
    .put("name", "Sales Order")
    .put("description", "Manage sales orders"));
contextData.put("tab_context", new JSONObject()
    .put("tab_name", "Order")
    .put("table_name", "C_Order")
    .put("record_id", 1000000));

// Chat with context
AIService.ChatResult result = aiService.chatWithContext(
    provider,
    chat,           // MChat parent
    "What is the total?",
    contextData,
    threadRootId
);

if (result.isSuccess()) {
    String response = result.getResponse();
    int newThreadId = result.getThreadRootId();
}
```

### Blocking Chat (Recommended for Programmatic Use)

```java
// chatBlocking() wraps streaming with CompletableFuture.join()
// Use this for API integrations, batch processing, or tests
ChatResult result = aiService.chatBlocking(
    provider,
    chat,
    "Show me overdue invoices",
    contextData,
    threadRootId
);

if (result.isSuccess()) {
    String response = result.getResponse();
    // Process complete response
}
```

### Streaming Chat (Recommended for UI)

```java
AIStreamCallback callback = AIStreamCallback.builder()
    .onChunk(chunk -> {
        // Append to UI in real-time
        textArea.append(chunk);
    })
    .onToolStart((name, args) -> {
        // Show tool indicator
        statusLabel.setText("Running: " + name);
    })
    .onToolComplete((name, result) -> {
        statusLabel.setText("Completed: " + name);
    })
    .onComplete(() -> {
        // Mark as done
        sendButton.setEnabled(true);
    })
    .onError(error -> {
        // Show error
        showErrorDialog(error.getMessage());
    })
    .build();

aiService.chatStreamingWithContext(
    provider,
    chat,
    "Show me overdue invoices",
    contextData,
    threadRootId,
    callback
);
```

### Direct Agent Usage

```java
// Create model and tools
ChatLanguageModel model = LangChain4jProviderFactory.create(provider);
ERPTools tools = new ERPTools(provider, ctx);

// Build agent
ERPAgent agent = AiServices.builder(ERPAgent.class)
    .chatLanguageModel(model)
    .tools(tools)
    .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
    .build();

// Execute
String response = agent.chat("session-456", "What products are low on stock?");
```

---

## Configuration

### Provider Configuration (AIG_Provider Table)

| Column | Description |
|--------|-------------|
| `AIGProviderType` | Provider type code (ANT, BED, OAI, OLL) |
| `APIKey` | API key (encrypted) |
| `AD_User_ID` | AI user for audit trail |
| `IsActive` | Provider enabled |

### System Properties

| Property | Default | Description |
|----------|---------|-------------|
| `ai.guardrails.enabled` | `true` | Enable guardrails |
| `ai.memory.maxMessages` | `20` | Default memory window |
| `ai.metrics.enabled` | `true` | Enable metrics collection |

### Clearing Caches

```java
// Clear AIService caches
AIService.getInstance().clearAllCaches();

// Clear provider factory cache
LangChain4jProviderFactory.clearCache();

// Clear specific provider
LangChain4jProviderFactory.evictFromCache(providerId);

// Clear budget cache for client
AIService.getInstance().clearBudgetCache(clientId);
```

---

## Related Documentation

- [ADR-002: LangChain4j Strategic Adoption](adr/002-langchain4j-strategic-adoption.md)
- [ADR-031: Chat Panel Integration](adr/031-chat-panel-langchain4j-chatmodel-integration.md)
- [ADR-033: Streaming Responses UX](adr/033-streaming-thinking-timeline-ux.md)
- [ADR-014: Guardrails and Safety](adr/014-guardrails-and-safety.md)
- [ADR-013: Observability and Cost Tracking](adr/013-observability-cost-tracking.md)
- [ADR-007: Database Security Model](adr/007-database-security-model.md)
- [ADR-035: Java Version Strategy](adr/035-java-version-strategy.md)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.17.1 | Dec 2025 | Initial documentation |
| 0.19.0 | Dec 2025 | Unified streaming-first architecture, chatBlocking(), streaming tools limitation docs |
| 0.18.0 | Dec 2025 | Llama provider, ModelName column, SimpleStreamingAgent for Ollama/Llama |
| 0.17.0 | Dec 2025 | Streaming tool callbacks, improved markdown rendering |
| 0.14.0 | Nov 2025 | ThreadAwareChatMemory |
| 0.13.0 | Nov 2025 | AIService, ERPAgent |
| 0.9.0 | Oct 2025 | LangChain4j integration |
