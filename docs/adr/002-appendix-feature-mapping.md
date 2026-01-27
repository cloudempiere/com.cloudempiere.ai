# ADR-002 Appendix: Feature Mapping - No Functionality Lost

This appendix ensures every feature in our custom implementation has a LangChain4j equivalent or is preserved in wrapper code.

---

## IAIProvider Interface Feature Mapping

### 1. Provider Lifecycle & Metadata

| Custom Feature | LangChain4j Equivalent | Migration Strategy |
|---------------|------------------------|-------------------|
| `initialize(MAIProvider)` | Builder pattern with config | **Wrap**: Create `IDempiereModelFactory.create(MAIProvider)` |
| `getProviderType()` | N/A (known at construction) | **Preserve**: Store in wrapper metadata |
| `getProviderName()` | N/A | **Preserve**: Store in wrapper metadata |
| `getAPIVersion()` | N/A | **Preserve**: Store in wrapper metadata |
| `shutdown()` | `Closeable` in some models | **Preserve**: Wrapper handles cleanup |
| `isReady()` | N/A | **Preserve**: Wrapper state tracking |
| `getLastError()` | Exception handling | **Preserve**: Wrapper error tracking |

**Wrapper Class:**
```java
public class IDempiereModelWrapper implements Closeable {
    private final ChatLanguageModel model;
    private final MAIProvider config;
    private boolean ready = false;
    private String lastError;

    public static IDempiereModelWrapper create(MAIProvider config) {
        ChatLanguageModel model = switch(config.getProviderType()) {
            case "ANTHROPIC" -> AnthropicChatModel.builder()
                .apiKey(config.getAPIKey())
                .modelName(config.getModelName())
                .build();
            // ... other providers
        };
        return new IDempiereModelWrapper(model, config);
    }

    public String getProviderType() { return config.getProviderType(); }
    public String getProviderName() { return config.getName(); }
    public boolean isReady() { return ready; }
    public String getLastError() { return lastError; }
}
```

---

### 2. Capability Checks

| Custom Feature | LangChain4j Equivalent | Migration Strategy |
|---------------|------------------------|-------------------|
| `supportsTextGeneration()` | Always true for ChatLanguageModel | **Implicit**: All chat models support this |
| `supportsStreaming()` | `StreamingChatLanguageModel` | **Check**: `instanceof StreamingChatLanguageModel` |
| `supportsFunctionCalling()` | `ChatLanguageModel.supportedCapabilities()` | **LangChain4j**: Native support check |
| `supportsVision()` | Model-specific | **Preserve**: Config-based flag |
| `supportsEmbeddings()` | `EmbeddingModel` exists | **Check**: Separate model creation |
| `supportsAudio()` | N/A (use Whisper separately) | **Preserve**: Config-based flag |
| `getSupportedModels()` | Provider documentation | **Preserve**: Static list per provider |
| `getModelCapabilities()` | Partial support | **Preserve**: Enhanced wrapper |

**Capability Wrapper:**
```java
public class ModelCapabilities {
    public static boolean supportsStreaming(ChatLanguageModel model) {
        return model instanceof StreamingChatLanguageModel;
    }

    public static boolean supportsFunctions(ChatLanguageModel model) {
        try {
            return model.supportedCapabilities()
                .contains(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
        } catch (Exception e) {
            return false;
        }
    }

    public static AIModelCapabilities getCapabilities(MAIProvider config) {
        AIModelCapabilities caps = new AIModelCapabilities();
        caps.setSupportsStreaming(true);
        caps.setSupportsFunctions(true);

        // Model-specific context lengths
        switch(config.getModelName()) {
            case "claude-3-opus-20240229" -> caps.setMaxContextLength(200000);
            case "claude-3-5-sonnet-20241022" -> caps.setMaxContextLength(200000);
            case "gpt-4-turbo" -> caps.setMaxContextLength(128000);
            // ... etc
        }
        return caps;
    }
}
```

---

### 3. Text Generation

| Custom Feature | LangChain4j Equivalent | Migration Strategy |
|---------------|------------------------|-------------------|
| `generateText(AIRequest)` | `model.generate(messages)` | **Direct**: LangChain4j native |
| `generateTextStream(request, callback)` | `StreamingChatLanguageModel.generate()` | **Direct**: LangChain4j native |
| `generateTextWithFunctions(request, functions)` | `model.generate(messages, toolSpecs)` | **Direct**: LangChain4j native |

**LangChain4j equivalents:**
```java
// Synchronous
ChatResponse response = model.chat(ChatRequest.builder()
    .messages(messages)
    .build());

// Streaming
StreamingChatLanguageModel streamingModel = ...;
streamingModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
    @Override public void onNext(String token) { callback.onChunk(token); }
    @Override public void onComplete(Response<AiMessage> response) { callback.onComplete(); }
    @Override public void onError(Throwable error) { callback.onError(error); }
});

// With tools/functions
ChatResponse response = model.chat(ChatRequest.builder()
    .messages(messages)
    .toolSpecifications(toolSpecs)
    .build());
```

---

### 4. Embeddings

| Custom Feature | LangChain4j Equivalent | Migration Strategy |
|---------------|------------------------|-------------------|
| `generateEmbedding(text, model)` | `EmbeddingModel.embed(text)` | **Direct**: Separate LangChain4j model |
| `generateEmbeddingsBatch(texts, model)` | `EmbeddingModel.embedAll(texts)` | **Direct**: Native batch support |

**LangChain4j implementation:**
```java
// Single embedding
EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("nomic-embed-text")
    .build();

Response<Embedding> response = embeddingModel.embed("text to embed");
float[] vector = response.content().vector();

// Batch embeddings
Response<List<Embedding>> batchResponse = embeddingModel.embedAll(
    List.of("text 1", "text 2", "text 3")
);
```

---

### 5. Vision (Image Analysis)

| Custom Feature | LangChain4j Equivalent | Migration Strategy |
|---------------|------------------------|-------------------|
| `analyzeImage(bytes, prompt, model)` | `ImageContent` in messages | **Direct**: LangChain4j native |
| `analyzeImageURL(url, prompt, model)` | `ImageContent.from(url)` | **Direct**: LangChain4j native |

**LangChain4j implementation:**
```java
// From bytes
ImageContent imageContent = ImageContent.from(
    Base64.getEncoder().encodeToString(imageData),
    "image/jpeg"
);

// From URL
ImageContent imageContent = ImageContent.from(imageUrl);

// In message
UserMessage message = UserMessage.from(
    TextContent.from(prompt),
    imageContent
);

ChatResponse response = model.chat(message);
```

---

### 6. Audio Transcription

| Custom Feature | LangChain4j Equivalent | Migration Strategy |
|---------------|------------------------|-------------------|
| `transcribeAudio(bytes, format, lang)` | Not in core LangChain4j | **Preserve**: Use OpenAI Whisper directly or `langchain4j-openai` |

**Implementation:**
```java
// Option 1: Direct OpenAI Whisper
OpenAiAudioTranscriptionModel whisperModel = OpenAiAudioTranscriptionModel.builder()
    .apiKey(apiKey)
    .modelName("whisper-1")
    .build();

String transcription = whisperModel.transcribe(audioData);

// Option 2: Keep custom integration
public class AudioTranscriptionService {
    public String transcribe(byte[] audioData, String format, String language) {
        // Existing Whisper API integration
    }
}
```

---

### 7. Health & Monitoring

| Custom Feature | LangChain4j Equivalent | Migration Strategy |
|---------------|------------------------|-------------------|
| `checkHealth()` | N/A | **Preserve**: Custom health check wrapper |
| `testConnection()` | N/A | **Preserve**: Simple ping test |
| `getRateLimitStatus()` | Partial (in listeners) | **Preserve**: Track via listeners |

**Preserved Implementation:**
```java
public class HealthCheckService {

    public AIHealthStatus checkHealth(ChatLanguageModel model, MAIProvider config) {
        AIHealthStatus status = new AIHealthStatus();
        long startTime = System.currentTimeMillis();

        try {
            // Minimal test request
            model.generate("test");

            status.setHealthy(true);
            status.setStatus("Healthy");
            status.setResponseTimeMs(System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            status.setHealthy(false);
            status.setStatus("Unhealthy");
            status.setMessage(e.getMessage());
        }

        return status;
    }

    public boolean testConnection(ChatLanguageModel model) {
        try {
            model.generate("ping");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

### 8. Cost Estimation

| Custom Feature | LangChain4j Equivalent | Migration Strategy |
|---------------|------------------------|-------------------|
| `estimateCost(AIRequest)` | `TokenUsage` in response | **Preserve**: Cost calculator using token counts |
| `estimateTokenCount(text, model)` | `Tokenizer` interface | **LangChain4j**: Native tokenizer support |

**Implementation:**
```java
public class CostEstimator {

    private static final Map<String, ModelPricing> PRICING = Map.of(
        "claude-3-5-sonnet-20241022", new ModelPricing(3.0, 15.0),
        "claude-3-opus-20240229", new ModelPricing(15.0, 75.0),
        "gpt-4-turbo", new ModelPricing(10.0, 30.0),
        "gpt-4o", new ModelPricing(5.0, 15.0)
        // ... more models
    );

    public double estimateCost(ChatResponse response, String modelName) {
        TokenUsage usage = response.tokenUsage();
        ModelPricing pricing = PRICING.get(modelName);

        if (pricing == null || usage == null) return 0.0;

        return (usage.inputTokenCount() / 1_000_000.0) * pricing.inputPer1M +
               (usage.outputTokenCount() / 1_000_000.0) * pricing.outputPer1M;
    }

    public int estimateTokenCount(String text, String modelName) {
        // LangChain4j tokenizer
        Tokenizer tokenizer = new OpenAiTokenizer(); // or model-specific
        return tokenizer.estimateTokenCountInText(text);
    }
}
```

---

## Summary: Feature Coverage

| Category | Custom Features | LangChain4j Native | Preserved in Wrapper | Total |
|----------|-----------------|-------------------|---------------------|-------|
| Lifecycle | 7 | 0 | 7 | 7/7 ✅ |
| Capabilities | 9 | 4 | 5 | 9/9 ✅ |
| Text Gen | 3 | 3 | 0 | 3/3 ✅ |
| Embeddings | 2 | 2 | 0 | 2/2 ✅ |
| Vision | 2 | 2 | 0 | 2/2 ✅ |
| Audio | 1 | 0 | 1 | 1/1 ✅ |
| Health | 3 | 0 | 3 | 3/3 ✅ |
| Cost | 2 | 1 | 1 | 2/2 ✅ |
| **TOTAL** | **29** | **12** | **17** | **29/29** ✅ |

---

## iDempiere-Specific Features (Always Preserved)

These features have NO LangChain4j equivalent and MUST remain custom:

| Feature | Class | Reason |
|---------|-------|--------|
| `SecureDatabaseQueryExecutor` | database/ | iDempiere role-based security |
| `AccessSqlParser` integration | database/ | iDempiere permission model |
| `MAIProvider` database model | model/ | iDempiere AD configuration |
| `AIChatWidget` | component/ | ZK UI integration |
| `WindowContextProvider` | context/ | iDempiere window data |
| `ChartContextProvider` | context/ | iDempiere chart data |
| Audit logging | database/ | Compliance requirements |
| `BoundaryValidator` | agent/ | Cost/rate limit enforcement |

---

## Migration Validation Checklist

Before removing any custom code, verify:

- [ ] Text generation works with same parameters
- [ ] Streaming delivers chunks to UI correctly
- [ ] Function/tool calling returns same format
- [ ] Embeddings produce compatible vectors
- [ ] Vision analysis accepts same image formats
- [ ] Health checks return same status structure
- [ ] Cost estimation matches current calculations
- [ ] Token counting is accurate
- [ ] Database config (MAIProvider) still loads
- [ ] Security executor still enforces permissions
- [ ] Audit trail still records all queries
- [ ] UI widgets still function correctly

---

## Recommended Wrapper Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 IDempiereAIService (Facade)                 │
│                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │ ModelFactory    │  │ HealthService   │  │ CostService │ │
│  │ (from MAIProvider)│ │ (health checks) │  │ (estimates) │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
│                                                             │
│  ┌─────────────────┐  ┌─────────────────┐                  │
│  │ CapabilityCheck │  │ TokenEstimator  │                  │
│  │ (feature flags) │  │ (tokenization)  │                  │
│  └─────────────────┘  └─────────────────┘                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              LangChain4j Native Models                      │
│                                                             │
│  AnthropicChatModel | OpenAiChatModel | OllamaChatModel    │
│  BedrockChatModel   | AzureOpenAiChatModel | ...           │
└─────────────────────────────────────────────────────────────┘
```

---

## Conclusion

**No functionality will be lost.** Every feature in `IAIProvider` either:

1. Maps directly to LangChain4j equivalent (12 features)
2. Is preserved in thin wrapper classes (17 features)
3. Is iDempiere-specific and untouched (8 features)

The migration reduces code while maintaining 100% feature parity.

---

*ADR-002 Appendix | Version 1.0 | 2025-12-01*
