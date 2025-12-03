# ADR-034: Google Gemini Provider Integration

## Status

Proposed

## Date

2025-12-03

## Deciders

CloudEmpiere AI Team

## Context and Problem Statement

The CloudEmpiere AI plugin currently supports Anthropic Claude, AWS Bedrock, OpenAI, and Ollama providers through LangChain4j (ADR-002). Google Gemini offers competitive pricing, multimodal capabilities, and unique features like native code execution that could benefit iDempiere users. How should we integrate Google Gemini as an additional AI provider while maintaining architectural consistency?

## Decision Drivers

- **Cost Efficiency**: Gemini 2.5 Flash offers $0.15/$0.60 per million tokens (10x cheaper than Claude)
- **Feature Parity**: Gemini supports streaming, function calling, vision, and embeddings
- **Enterprise Options**: Both direct Google AI API and Vertex AI (enterprise) paths available
- **Unique Capabilities**: Native Python code execution sandbox, extended thinking (Gemini 3 Pro)
- **LangChain4j Support**: Native `langchain4j-google-ai-gemini` module available (v1.8.0+)
- **Free Tier**: Generous free tier for development and small deployments

## Considered Options

1. **Google AI Gemini (Direct API)** - Use `langchain4j-google-ai-gemini` module
2. **Google Vertex AI Gemini** - Use `langchain4j-vertex-ai-gemini` module
3. **Both Options** - Support both direct and Vertex AI paths

## Decision Outcome

**Chosen option:** "Option 1: Google AI Gemini (Direct API)" for initial implementation, with Option 3 (Both) as the roadmap target.

The direct Google AI API provides the fastest path to integration with simpler authentication (API key vs. service account), competitive pricing, and full feature support. Vertex AI can be added later for enterprise customers requiring GCP integration, VPC security, and custom SLAs.

### Confirmation

- Unit tests pass for `GeminiChatModel` instantiation and configuration
- Integration test confirms text generation, streaming, and function calling
- Cost tracking captures Gemini-specific token pricing
- Provider appears in iDempiere AIG_Provider reference list

## Pros and Cons of the Options

### Option 1: Google AI Gemini (Direct API)

Use `langchain4j-google-ai-gemini` module with API key authentication.

- Good, because simpler authentication (API key only)
- Good, because competitive pricing with generous free tier
- Good, because full feature support (streaming, tools, vision, embeddings)
- Good, because native Python code execution sandbox available
- Good, because extended thinking support (Gemini 3 Pro)
- Neutral, because requires Google Cloud API key (not AWS IAM)
- Bad, because limited enterprise features vs. Vertex AI

### Option 2: Google Vertex AI Gemini

Use `langchain4j-vertex-ai-gemini` module with GCP service account authentication.

- Good, because enterprise-grade security (VPC, IAM)
- Good, because unified GCP billing and monitoring
- Good, because custom model fine-tuning support
- Neutral, because requires GCP project setup
- Bad, because more complex authentication (service account JSON)
- Bad, because higher baseline costs vs. direct API

### Option 3: Both Options

Support both Google AI and Vertex AI paths via provider subtype.

- Good, because maximum flexibility for different customer needs
- Good, because gradual enterprise migration path
- Neutral, because additional code complexity
- Bad, because two code paths to maintain

## More Information

### Implementation Notes

#### 1. Maven Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-google-ai-gemini</artifactId>
    <version>1.0.0-beta3</version>
</dependency>
```

Note: Version should match other LangChain4j dependencies. Update to 1.8.0+ when upgrading LangChain4j core.

#### 2. Provider Type Reference List Update

Add to `AD_Ref_List` for `AIG_ProviderType`:

| Value | Name | Description |
|-------|------|-------------|
| `GEM` | Google Gemini | Google AI Gemini (Direct API) |
| `VTX` | Google Vertex AI | Google Vertex AI Gemini (Enterprise) |

#### 3. LangChain4jProviderFactory Extension

```java
// Add to LangChain4jProviderFactory.java

public static final String PROVIDER_GEMINI = "GEM";

private static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";
private static final String DEFAULT_GEMINI_EMBEDDING_MODEL = "text-embedding-004";

// In create() switch statement:
case PROVIDER_GEMINI:
    return createGeminiModel(apiKey, modelName);

// Factory method:
private static ChatLanguageModel createGeminiModel(String apiKey, String modelName) {
    return GoogleAiGeminiChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName != null ? modelName : DEFAULT_GEMINI_MODEL)
        .temperature(0.7)
        .maxOutputTokens(4096)
        .logRequestsAndResponses(true)
        .build();
}

private static StreamingChatLanguageModel createGeminiStreamingModel(String apiKey, String modelName) {
    return GoogleAiGeminiStreamingChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName != null ? modelName : DEFAULT_GEMINI_MODEL)
        .temperature(0.7)
        .maxOutputTokens(4096)
        .build();
}

private static EmbeddingModel createGeminiEmbeddingModel(String apiKey, String modelName) {
    return GoogleAiGeminiEmbeddingModel.builder()
        .apiKey(apiKey)
        .modelName(modelName != null ? modelName : DEFAULT_GEMINI_EMBEDDING_MODEL)
        .build();
}
```

#### 4. Supported Gemini Models

| Model | Use Case | Input ($/1M) | Output ($/1M) | Context |
|-------|----------|--------------|---------------|---------|
| `gemini-3-pro-preview` | Complex reasoning | $2.00 | $12.00 | 1M tokens |
| `gemini-2.5-pro` | Balanced | $1.25 | $10.00 | 2M tokens |
| `gemini-2.5-flash` | **Recommended** | $0.15 | $0.60 | 1M tokens |
| `gemini-2.5-flash-lite` | Cost-optimized | $0.10 | $0.40 | 1M tokens |
| `gemini-2.0-flash` | Legacy | $0.10 | $0.40 | 1M tokens |

#### 5. Feature Comparison

| Feature | Anthropic | Gemini | Notes |
|---------|-----------|--------|-------|
| Streaming | Yes | Yes | `GoogleAiGeminiStreamingChatModel` |
| Function Calling | Yes | Yes | Including parallel tool calls |
| Vision | Yes | Yes | Images, video, audio, PDF |
| Embeddings | No (use Bedrock) | Yes | `text-embedding-004` |
| Code Execution | No | Yes | `allowCodeExecution(true)` |
| Extended Thinking | Yes (Claude 3.5) | Yes | Gemini 3 Pro with thinking budget |
| Batch API | No | Yes | 50% cost reduction |

#### 6. Cost Tracking Integration

Update `AIMetricsListener` to include Gemini pricing:

```java
// Model pricing map addition
private static final Map<String, ModelPricing> MODEL_PRICING = Map.of(
    // ... existing entries
    "gemini-3-pro-preview", new ModelPricing(2.0, 12.0),
    "gemini-2.5-pro", new ModelPricing(1.25, 10.0),
    "gemini-2.5-flash", new ModelPricing(0.15, 0.60),
    "gemini-2.5-flash-lite", new ModelPricing(0.10, 0.40),
    "gemini-2.0-flash", new ModelPricing(0.10, 0.40)
);
```

#### 7. Optional: Code Execution Feature

Gemini's unique code execution sandbox can be enabled for analytical use cases:

```java
GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
    .apiKey(apiKey)
    .modelName("gemini-2.5-flash")
    .allowCodeExecution(true)
    .includeCodeExecutionOutput(true)
    .build();
```

This enables the model to generate and execute Python code in a sandboxed environment, useful for:
- Data analysis and calculations
- Chart generation (with output capture)
- Statistical computations

Security consideration: Code execution happens in Google's sandbox, not on iDempiere server.

### Migration from Anthropic

For cost-sensitive deployments, migrate from Claude to Gemini:

| Scenario | Claude 3.5 Sonnet | Gemini 2.5 Flash | Savings |
|----------|-------------------|------------------|---------|
| 1M input + 100K output | $4.50 | $0.21 | **95%** |
| Chart Analysis (avg) | $0.003 | $0.0002 | **93%** |
| Daily 1000 requests | $3.00 | $0.15 | **95%** |

### Implementation Phases

| Phase | Scope | Timeline |
|-------|-------|----------|
| Phase 1 | Basic chat model integration | v0.10.0 |
| Phase 2 | Streaming + embeddings | v0.10.0 |
| Phase 3 | Code execution feature | v0.11.0 |
| Phase 4 | Vertex AI enterprise option | v0.12.0 |

### Related ADRs

- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j Strategic Adoption (parent decision)
- [ADR-012](012-rag-based-context-retrieval.md) - RAG (uses Gemini embeddings)
- [ADR-013](013-observability-cost-tracking.md) - Cost tracking integration
- [ADR-026](026-vector-database-strategy.md) - Vector storage for Gemini embeddings

### References

- [LangChain4j Google AI Gemini Documentation](https://docs.langchain4j.dev/integrations/language-models/google-ai-gemini)
- [Google Gemini API Pricing](https://ai.google.dev/gemini-api/docs/pricing)
- [Maven: langchain4j-google-ai-gemini](https://central.sonatype.com/artifact/dev.langchain4j/langchain4j-google-ai-gemini)
- [Gemini Java Developers Codelab](https://codelabs.developers.google.com/codelabs/gemini-java-developers)
- [LangChain4j GitHub Repository](https://github.com/langchain4j/langchain4j)

---

*ADR-034 | Version 1.0 | 2025-12-03*
