# ADR-042: AI Hub Provider Integration

## Status

Accepted

## Date

2025-12-10 (Proposed)
2025-12-11 (Accepted - Database schema implemented)

## Deciders

- Cloudempiere AI Team

## Context and Problem Statement

The iDempiere AI plugin (`com.cloudempiere.ai`) currently supports multiple AI providers (Anthropic, AWS Bedrock, Ollama, OpenAI) through the `LangChain4jProviderFactory`. However, each provider requires direct API integration from within the Java 11 OSGi runtime, which has limitations:

1. **Java 11 constraint** blocks LangChain4j 1.x features (MCP, extended thinking)
2. **OSGi ServiceLoader issues** cause problems with AWS SDK and other libraries
3. **No centralized AI processing** for multi-tenant deployments
4. **Direct API costs** - each iDempiere instance pays separately for API calls

ADR-038 proposed a iDempiere AI Hub AI Service that runs Java 17+ with full LangChain4j 1.x capabilities. This ADR defines how to integrate that AI Hub service as a **new provider type** in the existing factory pattern.

## Decision Drivers

- **Unblock LangChain4j 1.x features** without waiting for iDempiere v11 (Java 17)
- **Centralize AI processing** for cost optimization and management
- **Enable MCP server capabilities** for Claude Desktop and IDE integration
- **Maintain backward compatibility** with existing direct providers
- **Support graceful fallback** when AI Hub is unavailable
- **Minimize changes** to existing codebase

## Considered Options

1. **New AIHUB provider type** - Add `PROVIDER_AIHUB` to factory with REST/HTTP client
2. **Replace all providers with AI Hub proxy** - Route all AI through AI Hub
3. **gRPC integration** - Use gRPC for high-performance AI Hub communication
4. **MCP client in iDempiere** - Use MCP protocol directly (blocked by Java 11)

## Decision Outcome

**Chosen option:** "New AIHUB provider type", because it:
- Integrates cleanly with existing factory pattern
- Allows gradual migration (per-tenant choice)
- Maintains backward compatibility
- Uses simple REST/HTTP (no new dependencies)
- Enables AI Hub features without breaking existing setups

### Confirmation

- Unit tests verify `PROVIDER_AIHUB` creates correct model
- Integration tests confirm iDempiere → AI Hub → LLM flow
- Health check endpoint validates AI Hub availability
- Metrics show requests routed through AI Hub

---

## Architecture

### High-Level Integration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         iDempiere (Java 11)                                  │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐ │
│  │                    AIG_Provider Table                                  │ │
│  │                                                                        │ │
│  │  ID │ Name          │ Type │ URL                         │ APIKey     │ │
│  │  ───┼───────────────┼──────┼─────────────────────────────┼──────────  │ │
│  │  1  │ Claude Direct │ ANT  │ (not used)                  │ sk-ant-... │ │
│  │  2  │ Bedrock       │ BED  │ (not used)                  │ key:sec:rg │ │
│  │  3  │ AI Hub AI  │ SAT  │ http://AI Hub:8080       │ bearer-tok │ │
│  │  4  │ AI Hub Dev │ SAT  │ http://localhost:8080       │ dev-token  │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                    │                                        │
│  ┌─────────────────────────────────▼─────────────────────────────────────┐ │
│  │              LangChain4jProviderFactory                                │ │
│  │                                                                        │ │
│  │  public static ChatLanguageModel create(MAIProvider config) {         │ │
│  │      switch (config.getAIGProviderType()) {                           │ │
│  │          case PROVIDER_ANTHROPIC:  return createAnthropicModel();     │ │
│  │          case PROVIDER_BEDROCK:    return createBedrockModel();       │ │
│  │          case PROVIDER_OLLAMA:     return createOllamaModel();        │ │
│  │          case PROVIDER_OPENAI:     return createOpenAiModel();        │ │
│  │          case PROVIDER_AIHUB:  return createAI HubModel(); ◄── │ │
│  │      }                                                                 │ │
│  │  }                                                                     │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                    │                                        │
└────────────────────────────────────┼────────────────────────────────────────┘
                                     │
                    HTTP/REST        │  iDempiereContext DTO
                    (OpenAI-compat)  │  + Bearer Token Auth
                                     │
                    ┌────────────────▼────────────────┐
                    │   iDempiere AI Hub Service      │
                    │   (Java 17+, LangChain4j 1.x)    │
                    │                                  │
                    │  ┌────────────────────────────┐ │
                    │  │  REST API Layer            │ │
                    │  │                            │ │
                    │  │  POST /v1/chat/completions │ │
                    │  │  POST /v1/embeddings       │ │
                    │  │  GET  /health              │ │
                    │  │  GET  /metrics             │ │
                    │  └─────────────┬──────────────┘ │
                    │                │                 │
                    │  ┌─────────────▼──────────────┐ │
                    │  │  Security & Context Layer  │ │
                    │  │                            │ │
                    │  │  - Bearer token validation │ │
                    │  │  - iDempiere context parse │ │
                    │  │  - Role-based access check │ │
                    │  │  - Audit logging           │ │
                    │  └─────────────┬──────────────┘ │
                    │                │                 │
                    │  ┌─────────────▼──────────────┐ │
                    │  │  LangChain4j 1.x Engine    │ │
                    │  │                            │ │
                    │  │  - Provider routing        │ │
                    │  │  - MCP server/client       │ │
                    │  │  - Extended thinking       │ │
                    │  │  - Tool execution          │ │
                    │  │  - RAG/embeddings          │ │
                    │  └─────────────┬──────────────┘ │
                    │                │                 │
                    │  ┌─────────────▼──────────────┐ │
                    │  │  Upstream Providers        │ │
                    │  │                            │ │
                    │  │  ┌─────┐ ┌─────┐ ┌─────┐  │ │
                    │  │  │Anth.│ │Bedr.│ │Ollama│  │ │
                    │  │  └─────┘ └─────┘ └─────┘  │ │
                    │  └────────────────────────────┘ │
                    └─────────────────────────────────┘
```

### Request/Response Flow

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐     ┌─────────────┐
│  iDempiere   │     │   AI Hub      │     │   AI Hub      │     │   Claude    │
│  AIChatWidget│     │   REST API       │     │   LangChain4j    │     │   API       │
└──────┬───────┘     └────────┬─────────┘     └────────┬─────────┘     └──────┬──────┘
       │                      │                        │                      │
       │  POST /v1/chat/completions                    │                      │
       │  {                   │                        │                      │
       │    "model": "claude-sonnet-4",                │                      │
       │    "messages": [...],│                        │                      │
       │    "context": {      │                        │                      │
       │      "ad_client_id": 1000000,                 │                      │
       │      "ad_user_id": 100,                       │                      │
       │      "ad_role_id": 102,                       │                      │
       │      "ad_window_id": 143                      │                      │
       │    }                  │                        │                      │
       │  }                   │                        │                      │
       │ ─────────────────────>                        │                      │
       │                      │                        │                      │
       │                      │  Validate token        │                      │
       │                      │  Parse context         │                      │
       │                      │  Check permissions     │                      │
       │                      │                        │                      │
       │                      │  Route to provider ────>                      │
       │                      │                        │                      │
       │                      │                        │  Anthropic API call  │
       │                      │                        │ ─────────────────────>
       │                      │                        │                      │
       │                      │                        │  <─────────────────────
       │                      │                        │  Response + usage    │
       │                      │                        │                      │
       │                      │  <──────────────────────                      │
       │                      │  Add metrics, audit    │                      │
       │                      │                        │                      │
       │  <─────────────────────                       │                      │
       │  {                   │                        │                      │
       │    "choices": [...], │                        │                      │
       │    "usage": {        │                        │                      │
       │      "prompt_tokens": 150,                    │                      │
       │      "completion_tokens": 89                  │                      │
       │    }                  │                        │                      │
       │  }                   │                        │                      │
       │                      │                        │                      │
```

---

## Implementation

### 1. Provider Type Constant

Add to `LangChain4jProviderFactory.java`:

```java
/**
 * AI Hub provider type - routes requests to iDempiere AI Hub Service.
 *
 * Configuration in AIG_Provider:
 * - URL: AI Hub service URL (e.g., http://AI Hub:8080)
 * - APIKey: Bearer token for authentication
 * - ModelName: Target model (routed by AI Hub to upstream provider)
 */
public static final String PROVIDER_AIHUB = "SAT";
```

### 2. AD_Ref_List Entry

Add reference list value for AI Hub provider:

| Column | Value |
|--------|-------|
| AD_Reference_ID | (AIG_ProviderType reference) |
| Value | SAT |
| Name | iDempiere AI Hub |
| Description | AI requests routed through iDempiere AI Hub Service |
| IsActive | Y |

### 3. Factory Method

```java
/**
 * Create a ChatLanguageModel that routes through iDempiere AI Hub Service.
 *
 * <p>The AI Hub service exposes an OpenAI-compatible REST API, so we use
 * OpenAiChatModel with custom baseUrl pointing to the AI Hub.
 *
 * <p>Benefits of AI Hub routing:
 * <ul>
 *   <li>Access to LangChain4j 1.x features (MCP, extended thinking)</li>
 *   <li>Centralized AI processing and cost management</li>
 *   <li>Advanced observability and caching</li>
 *   <li>Multi-tenant support with context isolation</li>
 * </ul>
 *
 * @param config MAIProvider with AI Hub endpoint and auth token
 * @return ChatLanguageModel proxying through AI Hub
 */
private static ChatLanguageModel createAI HubModel(MAIProvider config) {
    String url = config.getURL();
    if (url == null || url.isEmpty()) {
        throw new IllegalArgumentException(
            "AI Hub URL not configured. Set URL in AIG_Provider.");
    }

    String apiKey = config.getAPIKey();
    if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalArgumentException(
            "AI Hub API key not configured. Set APIKey (bearer token) in AIG_Provider.");
    }

    // Normalize endpoint URL
    if (!endpoint.endsWith("/")) {
        endpoint = endpoint + "/";
    }
    String baseUrl = endpoint + "v1";

    String modelName = config.getModelName();
    if (modelName == null || modelName.isEmpty()) {
        modelName = "claude-sonnet-4";  // Default model
    }

    log.info("Creating AI Hub proxy model: endpoint=" + endpoint + ", model=" + modelName);

    // Use OpenAI-compatible client (AI Hub exposes /v1/chat/completions)
    var builder = OpenAiChatModel.builder()
        .baseUrl(baseUrl)
        .apiKey(apiKey)  // Bearer token for AI Hub auth
        .modelName(modelName)
        .temperature(0.7)
        .timeout(Duration.ofSeconds(120))  // Longer timeout for AI Hub hop
        .logRequests(true)
        .logResponses(true);

    // Add observability listener
    if (metricsEnabled) {
        builder.listeners(List.of(createMetricsListener("AI Hub")));
    }

    return builder.build();
}

/**
 * Create a StreamingChatLanguageModel through AI Hub.
 */
private static StreamingChatLanguageModel createAI HubStreamingModel(MAIProvider config) {
    String url = config.getURL();
    if (url == null || url.isEmpty()) {
        throw new IllegalArgumentException(
            "AI Hub URL not configured. Set URL in AIG_Provider.");
    }

    String apiKey = config.getAPIKey();
    if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalArgumentException(
            "AI Hub API key not configured. Set APIKey (bearer token) in AIG_Provider.");
    }

    if (!endpoint.endsWith("/")) {
        endpoint = endpoint + "/";
    }
    String baseUrl = endpoint + "v1";

    String modelName = config.getModelName();
    if (modelName == null || modelName.isEmpty()) {
        modelName = "claude-sonnet-4";
    }

    return OpenAiStreamingChatModel.builder()
        .baseUrl(baseUrl)
        .apiKey(apiKey)
        .modelName(modelName)
        .temperature(0.7)
        .timeout(Duration.ofSeconds(120))
        .build();
}

/**
 * Create an EmbeddingModel through AI Hub.
 */
private static EmbeddingModel createAI HubEmbeddingModel(MAIProvider config) {
    String url = config.getURL();
    if (url == null || url.isEmpty()) {
        throw new IllegalArgumentException(
            "AI Hub URL not configured. Set URL in AIG_Provider.");
    }

    String apiKey = config.getAPIKey();
    if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalArgumentException(
            "AI Hub API key not configured. Set APIKey (bearer token) in AIG_Provider.");
    }

    if (!endpoint.endsWith("/")) {
        endpoint = endpoint + "/";
    }
    String baseUrl = endpoint + "v1";

    // Use default embedding model or from config
    String modelName = "text-embedding-3-small";  // AI Hub routes appropriately

    return OpenAiEmbeddingModel.builder()
        .baseUrl(baseUrl)
        .apiKey(apiKey)
        .modelName(modelName)
        .build();
}
```

### 4. Switch Statement Updates

Update `create()`, `createStreaming()`, and `createEmbeddingModel()`:

```java
case PROVIDER_AIHUB:
    return createAI HubModel(config);

// In createStreaming():
case PROVIDER_AIHUB:
    return createAI HubStreamingModel(config);

// In createEmbeddingModel():
case PROVIDER_AIHUB:
    return createAI HubEmbeddingModel(config);
```

### 5. Health Check Support

```java
/**
 * Check if AI Hub service is available.
 *
 * @param config MAIProvider with AI Hub endpoint
 * @return true if AI Hub responds to health check
 */
public static boolean isAI HubHealthy(MAIProvider config) {
    if (!PROVIDER_AIHUB.equals(config.getAIGProviderType())) {
        return true;  // Not a AI Hub provider
    }

    String url = config.getURL();
    if (url == null || url.isEmpty()) {
        return false;
    }

    try {
        URL healthUrl = new URL(endpoint + "/health");
        HttpURLConnection conn = (HttpURLConnection) healthUrl.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int status = conn.getResponseCode();
        return status == 200;
    } catch (Exception e) {
        log.warning("AI Hub health check failed: " + e.getMessage());
        return false;
    }
}
```

---

## Context Passing

### iDempiereContext DTO

The AI Hub needs iDempiere context for security and audit:

```java
/**
 * Context passed to AI Hub service with each request.
 * Included in request headers or body extension.
 */
public class iDempiereContext {
    // Security Context
    private int adClientId;
    private int adOrgId;
    private int adUserId;
    private int adRoleId;
    private String sessionId;

    // Window/UI Context (optional)
    private Integer adWindowId;
    private Integer adTabId;
    private Integer recordId;
    private String tableName;

    // Language/Locale
    private String adLanguage;

    // Conversation tracking
    private String conversationId;

    // Getters, setters, builder...
}
```

### Header-Based Context

Pass context via custom headers:

```java
// In request to AI Hub
X-iDempiere-Client-ID: 1000000
X-iDempiere-Org-ID: 1000000
X-iDempiere-User-ID: 100
X-iDempiere-Role-ID: 102
X-iDempiere-Session-ID: abc123
X-iDempiere-Window-ID: 143
X-iDempiere-Language: en_US
```

---

## AI Hub REST API Specification

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/v1/chat/completions` | Chat completion (OpenAI-compatible) |
| POST | `/v1/chat/completions/stream` | Streaming chat (SSE) |
| POST | `/v1/embeddings` | Generate embeddings |
| GET | `/health` | Health check |
| GET | `/health/live` | Liveness probe |
| GET | `/health/ready` | Readiness probe |
| GET | `/metrics` | Prometheus metrics |

### Chat Request Extension

```json
{
  "model": "claude-sonnet-4",
  "messages": [
    {"role": "user", "content": "Hello"}
  ],
  "temperature": 0.7,
  "max_tokens": 4096,
  "stream": false,
  "idempiere_context": {
    "ad_client_id": 1000000,
    "ad_org_id": 1000000,
    "ad_user_id": 100,
    "ad_role_id": 102,
    "ad_window_id": 143,
    "conversation_id": "conv-123"
  }
}
```

### Model Routing

The AI Hub routes requests based on model name:

| Model Name | Upstream Provider |
|------------|-------------------|
| `claude-*` | Anthropic |
| `anthropic.*` | AWS Bedrock |
| `gpt-*` | OpenAI |
| `llama*` | Ollama |
| `mistral*` | Ollama |

---

## Pros and Cons of the Options

### Option 1: New AIHUB Provider Type (Chosen)

- Good, because it integrates cleanly with existing factory pattern
- Good, because it allows per-tenant choice of AI Hub vs direct
- Good, because it maintains backward compatibility
- Good, because it uses standard REST/HTTP (no new dependencies)
- Good, because it enables gradual migration
- Neutral, because it adds network latency (one hop)
- Bad, because it requires AI Hub service deployment

### Option 2: Replace All Providers with AI Hub Proxy

- Good, because it simplifies architecture (single provider)
- Good, because it centralizes all AI management
- Bad, because it's all-or-nothing (no gradual migration)
- Bad, because it creates single point of failure
- Bad, because it breaks existing direct provider setups

### Option 3: gRPC Integration

- Good, because it has better performance than REST
- Good, because it has native streaming support
- Bad, because it requires gRPC dependencies in OSGi
- Bad, because it's more complex to implement
- Bad, because it's harder to debug

### Option 4: MCP Client in iDempiere

- Good, because it's the native LangChain4j 1.x approach
- Good, because it enables full MCP features
- Bad, because MCP requires LangChain4j 0.36+ (Java 17)
- Bad, because it's blocked until iDempiere v11

---

## Migration Path

### Phase 1: Add Provider Type (This ADR)

1. Add `PROVIDER_AIHUB` constant
2. Add AD_Ref_List entry
3. Implement factory methods
4. Add health check support

### Phase 2: Deploy AI Hub Service

1. Create Quarkus AI Hub project (separate repo)
2. Implement OpenAI-compatible REST endpoints
3. Add authentication and context handling
4. Deploy to test environment

### Phase 3: Tenant Migration

1. Create AI Hub AIG_Provider record for test tenant
2. Validate end-to-end flow
3. Migrate production tenants gradually
4. Monitor metrics and costs

### Phase 4: Advanced Features

1. Enable MCP server in AI Hub
2. Add extended thinking support
3. Implement response caching
4. Add multi-tenant isolation

---

## Development: Mock AI Hub Server

A **MockAI HubServer** is provided for development and testing while the real iDempiere AI Hub service is being built.

### Quick Start

```bash
# Start mock server on default port 8090
./run-mock-AI Hub.sh

# Start on custom port with ECHO mode
./run-mock-AI Hub.sh 8091 ECHO
```

### Configure iDempiere

Create an `AIG_Provider` record:

| Field | Value |
|-------|-------|
| Name | Mock AI Hub |
| Type | SAT (iDempiere AI Hub) |
| Endpoint | http://localhost:8090 |
| APIKey | mock-token |
| ModelName | claude-sonnet-4 |

### Response Modes

| Mode | Description |
|------|-------------|
| **MOCK** (default) | Returns contextual mock responses based on user message |
| **ECHO** | Echoes back the user's message with [ECHO] prefix |
| **PROXY** | Forwards to real provider (not yet implemented) |

### Mock Server Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/chat/completions` | POST | Chat completion (OpenAI-compatible) |
| `/v1/embeddings` | POST | Embeddings (1536 dimensions) |
| `/health` | GET | Health check |
| `/info` | GET | Server info and stats |

### Source Files

| File | Description |
|------|-------------|
| `src/.../AI Hub/MockAI HubServer.java` | Mock server implementation |
| `src/.../AI Hub/AI HubProviderConstants.java` | Constants and headers |
| `run-mock-AI Hub.sh` | Startup script |
| `org.idempiere.test/.../MockAI HubServerTest.java` | Unit tests |

### Test the Mock Server

```bash
# Health check
curl http://localhost:8090/health

# Chat completion
curl -X POST http://localhost:8090/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer mock-token" \
  -d '{"model":"claude-sonnet-4","messages":[{"role":"user","content":"Hello"}]}'

# With iDempiere context
curl -X POST http://localhost:8090/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer mock-token" \
  -H "X-iDempiere-Client-ID: 1000000" \
  -H "X-iDempiere-User-ID: 100" \
  -d '{"model":"claude-sonnet-4","messages":[{"role":"user","content":"Show orders"}]}'
```

---

## More Information

### Fallback Strategy

If AI Hub is unavailable, the factory can fall back to direct providers:

```java
public static ChatLanguageModel createWithFallback(MAIProvider primary, MAIProvider fallback) {
    if (PROVIDER_AIHUB.equals(primary.getAIGProviderType())) {
        if (!isAI HubHealthy(primary)) {
            log.warning("AI Hub unavailable, using fallback provider");
            return create(fallback);
        }
    }
    return create(primary);
}
```

### Related ADRs

- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j Strategic Adoption
- [ADR-035](035-java-version-strategy.md) - Java Version Strategy (explains Java 11 constraint)
- [ADR-038](038-quarkus-AI Hub-ai-service.md) - iDempiere AI Hub AI Service Architecture (research)
- [ADR-040](040-embedding-ingestion-evolution.md) - Embedding Ingestion Evolution (alternative for RAG)

### References

- [Quarkus LangChain4j Extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [OpenAI API Specification](https://platform.openai.com/docs/api-reference)
- [LangChain4j Documentation](https://docs.langchain4j.dev/)

---

*ADR-042 | Version 1.0 | 2025-12-10*
*Status: Proposed*
*Next Step: Implement PROVIDER_AIHUB in LangChain4jProviderFactory*
