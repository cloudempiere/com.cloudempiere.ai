# MockOpenAI Provider Guide

## Overview

The **MockOpenAI** provider type enables iDempiere AI plugin to connect to OpenAI-compatible API endpoints without requiring actual external AI service credentials. This is particularly useful for:

- **Testing & Development**: Test AI features without incurring API costs
- **Integration Testing**: Validate AI workflows in CI/CD pipelines
- **Local Development**: Use local LLMs via OpenAI-compatible wrappers
- **iDempiere-CLI Integration**: Connect to the iDempiere-CLI Chat API (ADR-048)

## Provider Details

| Property | Value |
|----------|-------|
| **Provider Type Code** | `MOA` |
| **Provider Name** | Mock OpenAI |
| **Default Base URL** | `http://localhost:8081/v1` |
| **Default Model** | `llama3.2` |
| **Default Embedding Model** | `nomic-embed-text` |
| **API Key** | Optional (uses "test" as dummy value if not provided) |

## iDempiere-CLI Chat API (ADR-048)

The iDempiere-CLI provides an OpenAI-compatible Chat API server that runs locally. This is the primary use case for the MockOpenAI provider.

### Starting the iDempiere-CLI Chat API

```bash
# From the iDempiere-CLI project directory
./idempiere-cli

# The server will start on http://0.0.0.0:8081
# Profile: chat-api
```

### Available Endpoints

- **Chat Completions**: `POST /v1/chat/completions`
- **List Models**: `GET /v1/chat/models`
- **Model Details**: `GET /v1/chat/models/{model}`
- **Health Checks**: `GET /health`, `/health/ready`, `/health/live`, `/health/started`
- **OpenAPI/Swagger**: `GET /q/openapi`, `/q/swagger-ui`

### Configuration

The iDempiere-CLI Chat API uses:
- **LLM Provider**: Ollama (http://localhost:11434)
- **Default Model**: llama3.2
- **CORS**: Open (*) - configure for production

## Setting Up MockOpenAI Provider in iDempiere

### Step 1: Run the Migration Script

The migration script adds the MockOpenAI provider type to the AD_Ref_List:

```sql
-- PostgreSQL/Oracle
migration/postgresql/202512142110_MockOpenAI_Provider.sql
migration/oracle/202512142110_MockOpenAI_Provider.sql
```

**To apply the migration:**

1. Option A: Through iDempiere UI (System Admin → Migration Scripts → Run)
2. Option B: Direct database execution:
   ```bash
   psql -U adempiere -d idempiere -f migration/postgresql/202512142110_MockOpenAI_Provider.sql
   ```

### Step 2: Create AI Provider Configuration

1. Navigate to **System Admin → AI → AI Provider**
2. Click **New Record**
3. Fill in the following:

| Field | Value | Notes |
|-------|-------|-------|
| **Name** | Local Mock OpenAI | Descriptive name |
| **Provider Type** | Mock OpenAI | Select from dropdown |
| **Model Name** | llama3.2 | Or any model available in your local server |
| **API Key** | test | Optional, any value works for local servers |
| **Base URL** | http://localhost:8081/v1 | Custom URL if different |
| **Is Default** | Yes | If you want this as the default provider |
| **AI User** | Select a system user | User identity for AI operations |

4. **Save** the record

### Step 3: Test the Provider

Use the **Test AI Provider** process to verify connectivity:

1. Navigate to **System Admin → AI → AI Provider**
2. Select your MockOpenAI provider record
3. Click the **Test AI Provider** process button
4. The process will:
   - Check health status
   - Verify connectivity to the endpoint
   - List available models
   - Show success/failure messages

## Configuration Options

### Custom Base URL

If your OpenAI-compatible server runs on a different host/port:

```java
// In MAIProvider record, store in a custom field or use APIKey field
// Format: <baseUrl>|<modelName>|<apiKey>
// Example: http://myserver:8080/v1|gpt-3.5-turbo|sk-test123

// The factory will parse this in future versions
```

### Custom Model Selection

Override the default model by setting the **Model Name** field in the AI Provider record:

- `llama3.2` (default)
- `gpt-3.5-turbo`
- `gpt-4`
- Any model name supported by your mock server

### Embedding Models

For RAG (Retrieval-Augmented Generation) features, the MockOpenAI provider uses:

- **Default**: `nomic-embed-text`
- **Alternatives**: `mxbai-embed-large`, `all-minilm`, `text-embedding-3-small`

## Architecture

### Provider Flow

```
iDempiere AI Plugin
    ↓
LangChain4jProviderFactory
    ↓
createMockOpenAiModel()
    ↓
OpenAiChatModel.builder()
    .baseUrl(http://localhost:8081/v1)
    .apiKey("test")
    .modelName("llama3.2")
    ↓
iDempiere-CLI Chat API
    ↓
Ollama (http://localhost:11434)
    ↓
Local LLM (llama3.2)
```

### Code Structure

#### 1. Provider Type Constant

```java
// X_AIG_Provider.java
public static final String AIGPROVIDERTYPE_MockOpenAI = "MOA";
```

#### 2. Factory Method

```java
// LangChain4jProviderFactory.java
private static ChatLanguageModel createMockOpenAiModel(
    String baseUrl,
    String modelName,
    String apiKey
) {
    var builder = OpenAiChatModel.builder()
        .baseUrl(baseUrl != null ? baseUrl : DEFAULT_MOCK_OPENAI_URL)
        .apiKey(apiKey != null && !apiKey.isEmpty() ? apiKey : "test")
        .modelName(modelName != null ? modelName : DEFAULT_MOCK_OPENAI_MODEL)
        .temperature(0.7)
        .logRequests(true)
        .logResponses(true);

    if (metricsEnabled) {
        builder.listeners(List.of(createMetricsListener("mock-openai")));
    }

    return builder.build();
}
```

#### 3. Streaming Support

```java
// LangChain4jProviderFactory.java
private static StreamingChatLanguageModel createMockOpenAiStreamingModel(
    String baseUrl,
    String modelName,
    String apiKey
) {
    return OpenAiStreamingChatModel.builder()
        .baseUrl(baseUrl != null ? baseUrl : DEFAULT_MOCK_OPENAI_URL)
        .apiKey(apiKey != null && !apiKey.isEmpty() ? apiKey : "test")
        .modelName(modelName != null ? modelName : DEFAULT_MOCK_OPENAI_MODEL)
        .temperature(0.7)
        .build();
}
```

#### 4. Embedding Support

```java
// LangChain4jProviderFactory.java
private static EmbeddingModel createMockOpenAiEmbeddingModel(
    String baseUrl,
    String modelName,
    String apiKey
) {
    return OpenAiEmbeddingModel.builder()
        .baseUrl(baseUrl != null ? baseUrl : DEFAULT_MOCK_OPENAI_URL)
        .apiKey(apiKey != null && !apiKey.isEmpty() ? apiKey : "test")
        .modelName(modelName != null ? modelName : DEFAULT_MOCK_OPENAI_EMBEDDING_MODEL)
        .build();
}
```

## Use Cases

### 1. Unit Testing

```java
@Test
public void testMockOpenAIProvider() {
    // Setup
    MAIProvider provider = new MAIProvider(ctx, 0, null);
    provider.setName("Test Mock OpenAI");
    provider.setAIGProviderType(X_AIG_Provider.AIGPROVIDERTYPE_MockOpenAI);
    provider.setModelName("llama3.2");
    provider.setAPIKey("test");
    provider.saveEx();

    // Create chat model
    ChatLanguageModel model = LangChain4jProviderFactory.create(provider);

    // Test
    String response = model.generate("Hello, how are you?");
    assertNotNull(response);
    assertTrue(response.length() > 0);
}
```

### 2. Integration Testing

```java
@Test
public void testChatWithMockProvider() {
    // Assuming iDempiere-CLI Chat API is running on localhost:8081
    MAIProvider provider = MAIProvider.getMockOpenAIProvider(ctx);

    ChatLanguageModel model = LangChain4jProviderFactory.create(provider);

    List<ChatMessage> messages = List.of(
        SystemMessage.from("You are a helpful ERP assistant"),
        UserMessage.from("List the top 3 features of iDempiere")
    );

    AiMessage response = model.generate(messages).content();

    System.out.println("Response: " + response.text());
    assertTrue(response.text().contains("iDempiere"));
}
```

### 3. Development Workflow

```java
// Switch between real and mock providers based on environment
MAIProvider provider;

if (System.getenv("USE_MOCK_AI") != null) {
    // Development: Use local mock
    provider = MAIProvider.getMockOpenAIProvider(ctx);
} else {
    // Production: Use real Anthropic/Bedrock
    provider = MAIProvider.getDefaultProvider(ctx);
}

ChatLanguageModel model = LangChain4jProviderFactory.create(provider);
```

## Troubleshooting

### Connection Refused

**Problem**: `Connection refused: localhost:8081`

**Solution**:
1. Verify iDempiere-CLI Chat API is running:
   ```bash
   curl http://localhost:8081/health
   ```
2. Check the logs for startup errors
3. Ensure port 8081 is not blocked by firewall

### Model Not Found

**Problem**: `Model 'llama3.2' not found`

**Solution**:
1. Check available models:
   ```bash
   curl http://localhost:8081/v1/chat/models
   ```
2. Pull the model in Ollama:
   ```bash
   ollama pull llama3.2
   ```
3. Update the Model Name field in the AI Provider record

### Invalid API Response

**Problem**: Response doesn't match OpenAI format

**Solution**:
1. Verify the endpoint is OpenAI-compatible:
   ```bash
   curl -X POST http://localhost:8081/v1/chat/completions \
     -H "Content-Type: application/json" \
     -d '{"model":"llama3.2","messages":[{"role":"user","content":"test"}]}'
   ```
2. Check the response format matches OpenAI spec
3. Update iDempiere-CLI to the latest version

## Performance Considerations

### Local vs Remote

| Aspect | Local (MockOpenAI) | Remote (Anthropic/Bedrock) |
|--------|-------------------|----------------------------|
| **Latency** | < 100ms | 500-2000ms |
| **Cost** | Free | $$ per 1M tokens |
| **Rate Limits** | None | Yes (varies by provider) |
| **Model Quality** | Varies (local models) | High (GPT-4, Claude) |
| **Privacy** | Data stays local | Data sent to provider |

### Recommended Use Cases

**Use MockOpenAI for:**
- Unit tests
- Integration tests
- Local development
- Privacy-sensitive development
- Cost optimization in dev/test environments

**Use Real Providers for:**
- Production workloads
- Customer-facing features
- Tasks requiring high-quality responses
- Features requiring latest models (Claude Opus, GPT-4 Turbo)

## Security Notes

### API Key Handling

For MockOpenAI, the API key is optional and can be any value (defaults to "test"). However:

- **DO NOT** store real API keys in the MockOpenAI provider
- **DO NOT** use MockOpenAI in production for sensitive data
- **DO** use separate provider configurations for dev/test/prod

### Network Security

- The default configuration uses `http://localhost:8081` (no encryption)
- For remote mock servers, use HTTPS
- Configure firewalls to restrict access to localhost only

## Migration Path

### From Ollama Direct to MockOpenAI

If you were using Ollama provider directly:

```sql
-- Update existing Ollama provider to MockOpenAI
UPDATE AIG_Provider
SET AIGProviderType = 'MOA',
    -- Store the Ollama URL in a custom field if needed
    Description = 'Migrated from Ollama to MockOpenAI (iDempiere-CLI)'
WHERE AIGProviderType = 'OLL';
```

### Benefits of Migration

1. **Standardization**: Uses OpenAI-compatible interface
2. **Flexibility**: Can switch between different OpenAI-compatible backends
3. **Testing**: Easier to mock/stub in tests
4. **Future-proof**: OpenAI API is the de-facto standard

## Related Documentation

- [ADR-002: LangChain4j Strategic Adoption](../adr/002-langchain4j-strategic-adoption.md)
- [ADR-048: iDempiere-CLI Chat API](../adr/048-idempiere-cli-chat-api.md) *(to be created)*
- [LangChain4j OpenAI Integration](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/integrations/language-models/openai.md)
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.10.0 | 2025-12-14 | Initial implementation of MockOpenAI provider type |

## Support

For issues or questions:

1. Check the [Troubleshooting](#troubleshooting) section
2. Review iDempiere-CLI logs
3. Check Ollama status: `ollama list`
4. File an issue on GitHub: [com.cloudempiere.ai/issues](https://github.com/cloudempiere/com.cloudempiere.ai/issues)
