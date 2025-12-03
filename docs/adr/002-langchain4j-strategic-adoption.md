# ADR-002: Strategic LangChain4j Adoption and Architecture Simplification

**Status:** Accepted (Phase 1 Implemented in v0.9.0)
**Date:** 2025-12-01
**Updated:** 2025-12-01
**Deciders:** Cloudempiere AI Team
**Context:** Plugin architecture evolution for v0.8.0+

---

## Context

### Current State Analysis

The `com.cloudempiere.ai` plugin has evolved organically with two parallel implementation paths:

| Layer | Custom Implementation | LangChain4j Alternative |
|-------|----------------------|------------------------|
| Provider Interface | `IAIProvider` (30+ methods) | `ChatLanguageModel` (3 methods) |
| Provider Factory | `AIProviderFactory` (reflection-based) | Native LangChain4j modules |
| Agent Loop | `BaseAgent` (manual iteration) | `AiServices.builder()` (automatic) |
| Tool Discovery | `ToolRegistry` + `ITool` interface | `@Tool` annotations |
| Memory | Custom conversation tracking | `MessageWindowChatMemory` |
| Message Format | `AIMessage`, `AIRequest`, `AIResponse` | `ChatMessage`, `ChatRequest`, `ChatResponse` |

### Problem Statement

1. **Code Duplication**: ~70% of agent logic is custom-built, duplicating LangChain4j capabilities
2. **Maintenance Burden**: Two parallel systems (BaseAgent vs LangChain4jAgent) require double maintenance
3. **Feature Lag**: Custom providers miss LangChain4j ecosystem features (RAG, structured outputs, observability)
4. **Adapter Overhead**: `IAIProviderChatModelAdapter` adds conversion complexity
5. **Limited Provider Options**: Only Anthropic and Bedrock; LangChain4j supports 15+ providers out-of-box

### Current Architecture (Hybrid)

```
┌─────────────────────────────────────────────────────────────┐
│                    AIChatWidget (ZK UI)                     │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────────┐
│  AIConversationService  │     │     LangChain4jAgent        │
│  (Custom routing)       │     │  (AiServices + @Tool)       │
└─────────────────────────┘     └─────────────────────────────┘
              │                               │
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────────┐
│    AIProviderFactory    │     │ IAIProviderChatModelAdapter │
│    (Custom registry)    │     │     (Bridge pattern)        │
└─────────────────────────┘     └─────────────────────────────┘
              │                               │
              ▼                               ▼
┌─────────────────────────────────────────────────────────────┐
│              IAIProvider (Custom interface)                  │
│         AnthropicProvider  |  AWSBedrockProvider            │
└─────────────────────────────────────────────────────────────┘
```

---

## Decision

**Adopt LangChain4j as the primary AI framework**, replacing custom implementations where LangChain4j provides equivalent or superior functionality, while preserving iDempiere-specific security and context layers.

### Target Architecture (LangChain4j-First)

```
┌─────────────────────────────────────────────────────────────┐
│                    AIChatWidget (ZK UI)                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              IDempiereAIService (Facade)                    │
│         - Context injection                                  │
│         - Security enforcement                               │
│         - Audit logging                                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              LangChain4j AiServices                         │
│         - Agent orchestration                                │
│         - Tool discovery (@Tool)                             │
│         - Memory management                                  │
│         - Structured outputs                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│           LangChain4j Native Providers                      │
│  AnthropicChatModel | BedrockChatModel | OllamaChatModel   │
│  OpenAiChatModel    | AzureOpenAiChatModel | ...           │
└─────────────────────────────────────────────────────────────┘
```

---

## Recommendations

### Phase 1: Provider Migration (v0.9.0) ✅ IMPLEMENTED

#### 1.1 Replace Custom Providers with LangChain4j Native Modules

| Current | Replace With | Maven Artifact | Status |
|---------|--------------|----------------|--------|
| `AnthropicProvider` | `AnthropicChatModel` | `langchain4j-anthropic` | ✅ Done |
| `AWSBedrockProvider` | `BedrockChatModel` | `langchain4j-bedrock` | ✅ Done |
| (new) | `OllamaChatModel` | `langchain4j-ollama` | ✅ Done |
| (new) | `OpenAiChatModel` | `langchain4j-open-ai` | ✅ Done |

**Note:** `BedrockChatModel` is a unified class supporting all Bedrock foundation models (Claude, Amazon Nova, Mistral, Llama, etc.) via the modelId parameter.

**Implementation Details (v0.9.0):**
- `LangChain4jProviderFactory.java` - Factory creates ChatLanguageModel from MAIProvider config
- `ERPTools.java` - @Tool annotated methods for database operations
- `IDempiereAgent.java` - AiServices interface with system prompt
- `IDempiereAIService.java` - Main facade for AI interactions
- Old providers (`AnthropicProvider`, `AWSBedrockProvider`) deprecated, not deleted

**Benefits:**
- Native streaming support
- Built-in retry/backoff
- Automatic token counting
- Vision/multimodal support
- Function calling handled natively

**Code Reduction:** ~2,000 lines (AnthropicProvider + AWSBedrockProvider)

#### 1.2 Create Provider Configuration Adapter

```java
public class LangChain4jProviderFactory {

    public ChatLanguageModel createFromConfig(MAIProvider config) {
        return switch (config.getProviderType()) {
            case "ANTHROPIC" -> AnthropicChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .maxTokens(config.getMaxTokens())
                .build();
            case "BEDROCK" -> BedrockChatModel.builder()
                .region(Region.of(config.getRegion()))
                .modelId(config.getModelName())  // Supports Claude, Nova, Mistral, Llama, etc.
                .build();
            case "OLLAMA" -> OllamaChatModel.builder()
                .baseUrl(config.getEndpoint())
                .modelName(config.getModelName())
                .build();
            case "OPENAI" -> OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .build();
            default -> throw new IllegalArgumentException("Unknown provider: " + config.getProviderType());
        };
    }
}
```

### Phase 2: Deprecate Custom Agent Infrastructure (v0.8.0)

#### 2.1 Remove BaseAgent in Favor of AiServices

**Current (Manual Loop):**
```java
public class BaseAgent {
    public AgentResponse execute(String goal, AgentContext ctx) {
        while (iterations < maxIterations) {
            AIResponse response = provider.generateTextWithFunctions(request, tools);
            if (response.hasFunctionCalls()) {
                for (AIFunctionCall call : response.getFunctionCalls()) {
                    String result = toolRegistry.execute(call);
                    // ... manual result handling
                }
            }
            // ... manual loop control
        }
    }
}
```

**Recommended (LangChain4j):**
```java
public interface IDempiereAgent {
    @SystemMessage("""
        You are an iDempiere ERP assistant with access to database queries.
        Always respect user permissions. Never expose sensitive data.
        """)
    String execute(@MemoryId String sessionId, @UserMessage String goal);
}

// Usage
IDempiereAgent agent = AiServices.builder(IDempiereAgent.class)
    .chatLanguageModel(model)
    .tools(new ERPTools(secureExecutor))
    .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
    .build();
```

**Code Reduction:** ~500 lines (BaseAgent + AgentLoop logic)

#### 2.2 Migrate ToolRegistry to @Tool Annotations

**Current (Manual Registration):**
```java
public class ToolRegistry {
    private Map<String, ITool> tools = new HashMap<>();

    public void register(ITool tool) {
        tools.put(tool.getName(), tool);
    }

    public String execute(AIFunctionCall call) {
        ITool tool = tools.get(call.getName());
        return tool.execute(call.getArguments());
    }
}
```

**Recommended (@Tool):**
```java
public class ERPTools {
    private final SecureDatabaseQueryExecutor executor;

    @Tool("Execute a read-only SQL query against the ERP database")
    public String queryDatabase(
        @P("The SQL SELECT query") String sql,
        @P("Maximum rows to return") int maxRows
    ) {
        return executor.execute(sql, maxRows).toJSON();
    }

    @Tool("Get metadata about a database table")
    public String getTableMetadata(
        @P("Table name (e.g., C_Order)") String tableName
    ) {
        return metadataService.getTableInfo(tableName);
    }

    @Tool("Look up a record by ID")
    public String lookupRecord(
        @P("Table name") String tableName,
        @P("Record ID") int recordId
    ) {
        return recordService.lookup(tableName, recordId).toJSON();
    }
}
```

**Benefits:**
- Declarative tool definition
- Automatic schema generation for function calling
- No manual registration needed
- Better documentation via annotations

### Phase 3: Advanced LangChain4j Features (v0.9.0)

#### 3.1 Structured Outputs

```java
public record OrderAnalysis(
    @Description("Overall assessment") String summary,
    @Description("List of potential issues") List<String> issues,
    @Description("Recommended actions") List<String> recommendations,
    @Description("Risk level 1-10") int riskScore
) {}

// Usage
OrderAnalysis analysis = AiServices.builder(OrderAnalyzer.class)
    .chatLanguageModel(model)
    .build()
    .analyze(orderData);
```

#### 3.2 RAG (Retrieval Augmented Generation)

```java
// For help documentation, product catalogs, etc.
EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("nomic-embed-text")
    .build();

EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(embeddingModel)
    .maxResults(5)
    .build();

IDempiereAgent agent = AiServices.builder(IDempiereAgent.class)
    .chatLanguageModel(model)
    .contentRetriever(retriever)  // Automatic context injection
    .build();
```

#### 3.3 Guardrails and Moderation

```java
// Input validation
ChatLanguageModel moderatedModel = OpenAiModerationModel.builder()
    .apiKey(apiKey)
    .build();

// Output structured validation
public interface SafeAgent {
    @Moderate  // LangChain4j annotation for content moderation
    String respond(@UserMessage String input);
}
```

### Phase 4: Observability and Monitoring (v0.9.0)

#### 4.1 LangChain4j Observability

```java
ChatLanguageModel model = AnthropicChatModel.builder()
    .apiKey(apiKey)
    .modelName("claude-3-5-sonnet-20241022")
    .listeners(List.of(
        new TokenUsageListener(),      // Track costs
        new LatencyListener(),         // Track response times
        new IDempiereAuditListener()   // Custom audit logging
    ))
    .build();
```

---

## Migration Path

### v0.8.0 - Foundation

| Task | Effort | Impact |
|------|--------|--------|
| Add LangChain4j provider modules (anthropic, bedrock, ollama) | 2 days | High |
| Create `LangChain4jProviderFactory` | 1 day | High |
| Deprecate `IAIProvider` implementations | 1 day | Medium |
| Migrate tools to @Tool annotations | 2 days | High |
| Deprecate `BaseAgent`, `ToolRegistry` | 1 day | Medium |
| Update `AIChatWidget` to use new factory | 1 day | Medium |

### v0.9.0 - Advanced Features

| Task | Effort | Impact |
|------|--------|--------|
| Add structured outputs for common operations | 3 days | High |
| Implement RAG for documentation | 5 days | Medium |
| Add observability/listeners | 2 days | Medium |
| Domain-specific agents (Inventory, Sales) | 5 days | High |

### v1.0.0 - Production Ready

| Task | Effort | Impact |
|------|--------|--------|
| Remove deprecated code | 2 days | Medium |
| Performance optimization | 3 days | Medium |
| Comprehensive testing | 5 days | High |
| Documentation | 3 days | Medium |

---

## Dependencies to Add

```xml
<!-- LangChain4j Core (already present) -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>1.0.0-beta3</version>
</dependency>

<!-- Native Providers -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-anthropic</artifactId>
    <version>1.0.0-beta3</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-bedrock</artifactId>
    <version>1.0.0-beta3</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>1.0.0-beta3</version>
</dependency>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>1.0.0-beta3</version>
</dependency>

<!-- Embeddings for RAG -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-embeddings</artifactId>
    <version>1.0.0-beta3</version>
</dependency>
```

---

## What to Keep (iDempiere-Specific)

| Component | Reason |
|-----------|--------|
| `SecureDatabaseQueryExecutor` | iDempiere security model integration |
| `AIContextProviderRegistry` | Window/chart context extraction |
| `AIChatWidget` | ZK UI integration |
| `MAIProvider`, `MAIChat` | Database persistence |
| `BoundaryValidator` | Cost/rate limit enforcement |
| Audit logging | Compliance requirements |

---

## What to Remove/Deprecate

| Component | Replacement | Timeline |
|-----------|-------------|----------|
| `IAIProvider` interface | `ChatLanguageModel` | v0.9.0 deprecated ✅, v1.0.0 remove |
| `AnthropicProvider` | `AnthropicChatModel` | v0.9.0 ✅ |
| `AWSBedrockProvider` | `BedrockChatModel` | v0.9.0 ✅ |
| `AIProviderFactory` | `LangChain4jProviderFactory` | v0.9.0 ✅ |
| `IAIProviderChatModelAdapter` | Direct native models | v0.9.0 ✅ |
| `BaseAgent` | `AiServices` | v0.9.0 ✅ |
| `ToolRegistry`, `ITool` | `@Tool` annotations | v0.9.0 ✅ |
| Custom DTOs (AIRequest, etc.) | LangChain4j messages | v0.9.0 |

---

## Consequences

### Positive

1. **~60% Code Reduction**: Remove custom provider and agent implementations
2. **Faster Feature Delivery**: Native LangChain4j features available immediately
3. **Better Maintenance**: Single framework to maintain vs hybrid
4. **Provider Flexibility**: 15+ providers supported out-of-box
5. **Community Support**: Active LangChain4j community and documentation
6. **Modern Features**: RAG, structured outputs, guardrails built-in
7. **Observability**: Native listener support for monitoring

### Negative

1. **Migration Effort**: ~2-3 weeks of refactoring
2. **Breaking Changes**: Existing integrations may need updates
3. **Dependency Size**: Additional JARs to include
4. **Learning Curve**: Team needs LangChain4j expertise
5. **Version Lock**: Tied to LangChain4j release cycle

### Neutral

1. **Security Layer Unchanged**: `SecureDatabaseQueryExecutor` remains custom
2. **UI Unchanged**: `AIChatWidget` continues to work
3. **Database Model Unchanged**: `AIG_Provider` table compatible

---

## Alternatives Considered

### Alternative 1: Keep Hybrid Architecture

**Pros:** No migration effort
**Cons:** Continued maintenance burden, missing features
**Decision:** Rejected - technical debt accumulates

### Alternative 2: Full Custom Implementation

**Pros:** Complete control
**Cons:** Massive effort to replicate LangChain4j features
**Decision:** Rejected - not cost-effective

### Alternative 3: Spring AI Instead of LangChain4j

**Pros:** Spring ecosystem integration
**Cons:** Less mature, fewer providers, not OSGi-friendly
**Decision:** Rejected - LangChain4j more suitable for OSGi

---

## References

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j)
- [LangChain4j Examples](https://github.com/langchain4j/langchain4j-examples)
- [ADR-001: Initial Architecture](001-initial-architecture.md)
- [ADR-002 Appendix: Feature Mapping](002-appendix-feature-mapping.md) - **No functionality lost** verification
- [CHANGELOG.md](../../CHANGELOG.md)

---

## Appendix: Code Comparison

### Provider Instantiation

**Before (Custom):**
```java
IAIProviderFactory factory = ...;
MAIProvider config = new Query(ctx, MAIProvider.Table_Name, "AIG_Provider_ID=?", null)
    .setParameters(providerId)
    .first();
IAIProvider provider = factory.get(config);
AIResponse response = provider.generateText(request);
```

**After (LangChain4j):**
```java
MAIProvider config = ...;
ChatLanguageModel model = LangChain4jProviderFactory.create(config);
String response = model.generate(userMessage);
```

### Agent with Tools

**Before (Custom):**
```java
BaseAgent agent = new BaseAgent(provider);
agent.registerTool(new DatabaseQueryTool(executor));
agent.registerTool(new MetadataTool(metadataService));
AgentResponse response = agent.execute(goal, context);
```

**After (LangChain4j):**
```java
IDempiereAgent agent = AiServices.builder(IDempiereAgent.class)
    .chatLanguageModel(model)
    .tools(new ERPTools(executor, metadataService))
    .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
    .build();

String response = agent.execute(sessionId, goal);
```

---

*ADR-002 | Version 1.0 | 2025-12-01*
