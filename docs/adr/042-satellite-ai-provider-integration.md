# ADR-042: Satellite AI Provider Integration

## Status

Proposed

## Date

2025-12-10

## Deciders

- Cloudempiere AI Team

## Context and Problem Statement

The iDempiere AI plugin (`com.cloudempiere.ai`) currently supports multiple AI providers (Anthropic, AWS Bedrock, Ollama, OpenAI) through the `LangChain4jProviderFactory`. However, each provider requires direct API integration from within the Java 11 OSGi runtime, which has limitations:

1. **Java 11 constraint** blocks LangChain4j 1.x features (MCP, extended thinking)
2. **OSGi ServiceLoader issues** cause problems with AWS SDK and other libraries
3. **No centralized AI processing** for multi-tenant deployments
4. **Direct API costs** - each iDempiere instance pays separately for API calls

ADR-038 proposed a Quarkus Satellite AI Service that runs Java 17+ with full LangChain4j 1.x capabilities. This ADR defines how to integrate that satellite service as a **new provider type** in the existing factory pattern.

## Decision Drivers

- **Unblock LangChain4j 1.x features** without waiting for iDempiere v11 (Java 17)
- **Centralize AI processing** for cost optimization and management
- **Enable MCP server capabilities** for Claude Desktop and IDE integration
- **Maintain backward compatibility** with existing direct providers
- **Support graceful fallback** when satellite is unavailable
- **Minimize changes** to existing codebase

## Considered Options

1. **New SATELLITE provider type** - Add `PROVIDER_SATELLITE` to factory with REST/HTTP client
2. **Replace all providers with satellite proxy** - Route all AI through satellite
3. **gRPC integration** - Use gRPC for high-performance satellite communication
4. **MCP client in iDempiere** - Use MCP protocol directly (blocked by Java 11)

## Decision Outcome

**Chosen option:** "New SATELLITE provider type", because it:
- Integrates cleanly with existing factory pattern
- Allows gradual migration (per-tenant choice)
- Maintains backward compatibility
- Uses simple REST/HTTP (no new dependencies)
- Enables satellite features without breaking existing setups

### Confirmation

- Unit tests verify `PROVIDER_SATELLITE` creates correct model
- Integration tests confirm iDempiere → Satellite → LLM flow
- Health check endpoint validates satellite availability
- Metrics show requests routed through satellite

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
│  │  ID │ Name          │ Type │ Endpoint                    │ APIKey     │ │
│  │  ───┼───────────────┼──────┼─────────────────────────────┼──────────  │ │
│  │  1  │ Claude Direct │ ANT  │ (not used)                  │ sk-ant-... │ │
│  │  2  │ Bedrock       │ BED  │ (not used)                  │ key:sec:rg │ │
│  │  3  │ Satellite AI  │ SAT  │ http://satellite:8080       │ bearer-tok │ │
│  │  4  │ Satellite Dev │ SAT  │ http://localhost:8080       │ dev-token  │ │
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
│  │          case PROVIDER_SATELLITE:  return createSatelliteModel(); ◄── │ │
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
                    │   Quarkus Satellite Service      │
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
│  iDempiere   │     │   Satellite      │     │   Satellite      │     │   Claude    │
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
 * Satellite provider type - routes requests to Quarkus Satellite Service.
 *
 * Configuration in AIG_Provider:
 * - Endpoint: Satellite service URL (e.g., http://satellite:8080)
 * - APIKey: Bearer token for authentication
 * - ModelName: Target model (routed by satellite to upstream provider)
 */
public static final String PROVIDER_SATELLITE = "SAT";
```

### 2. AD_Ref_List Entry

Add reference list value for satellite provider:

| Column | Value |
|--------|-------|
| AD_Reference_ID | (AIG_ProviderType reference) |
| Value | SAT |
| Name | Quarkus Satellite |
| Description | AI requests routed through Quarkus Satellite Service |
| IsActive | Y |

### 3. Factory Method

```java
/**
 * Create a ChatLanguageModel that routes through Quarkus Satellite Service.
 *
 * <p>The satellite service exposes an OpenAI-compatible REST API, so we use
 * OpenAiChatModel with custom baseUrl pointing to the satellite.
 *
 * <p>Benefits of satellite routing:
 * <ul>
 *   <li>Access to LangChain4j 1.x features (MCP, extended thinking)</li>
 *   <li>Centralized AI processing and cost management</li>
 *   <li>Advanced observability and caching</li>
 *   <li>Multi-tenant support with context isolation</li>
 * </ul>
 *
 * @param config MAIProvider with satellite endpoint and auth token
 * @return ChatLanguageModel proxying through satellite
 */
private static ChatLanguageModel createSatelliteModel(MAIProvider config) {
    String endpoint = config.getEndpoint();
    if (endpoint == null || endpoint.isEmpty()) {
        throw new IllegalArgumentException(
            "Satellite endpoint not configured. Set Endpoint in AIG_Provider.");
    }

    String apiKey = config.getAPIKey();
    if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalArgumentException(
            "Satellite API key not configured. Set APIKey (bearer token) in AIG_Provider.");
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

    log.info("Creating Satellite proxy model: endpoint=" + endpoint + ", model=" + modelName);

    // Use OpenAI-compatible client (satellite exposes /v1/chat/completions)
    var builder = OpenAiChatModel.builder()
        .baseUrl(baseUrl)
        .apiKey(apiKey)  // Bearer token for satellite auth
        .modelName(modelName)
        .temperature(0.7)
        .timeout(Duration.ofSeconds(120))  // Longer timeout for satellite hop
        .logRequests(true)
        .logResponses(true);

    // Add observability listener
    if (metricsEnabled) {
        builder.listeners(List.of(createMetricsListener("satellite")));
    }

    return builder.build();
}

/**
 * Create a StreamingChatLanguageModel through satellite.
 */
private static StreamingChatLanguageModel createSatelliteStreamingModel(MAIProvider config) {
    String endpoint = config.getEndpoint();
    if (endpoint == null || endpoint.isEmpty()) {
        throw new IllegalArgumentException(
            "Satellite endpoint not configured. Set Endpoint in AIG_Provider.");
    }

    String apiKey = config.getAPIKey();
    if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalArgumentException(
            "Satellite API key not configured. Set APIKey (bearer token) in AIG_Provider.");
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
 * Create an EmbeddingModel through satellite.
 */
private static EmbeddingModel createSatelliteEmbeddingModel(MAIProvider config) {
    String endpoint = config.getEndpoint();
    if (endpoint == null || endpoint.isEmpty()) {
        throw new IllegalArgumentException(
            "Satellite endpoint not configured. Set Endpoint in AIG_Provider.");
    }

    String apiKey = config.getAPIKey();
    if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalArgumentException(
            "Satellite API key not configured. Set APIKey (bearer token) in AIG_Provider.");
    }

    if (!endpoint.endsWith("/")) {
        endpoint = endpoint + "/";
    }
    String baseUrl = endpoint + "v1";

    // Use default embedding model or from config
    String modelName = "text-embedding-3-small";  // Satellite routes appropriately

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
case PROVIDER_SATELLITE:
    return createSatelliteModel(config);

// In createStreaming():
case PROVIDER_SATELLITE:
    return createSatelliteStreamingModel(config);

// In createEmbeddingModel():
case PROVIDER_SATELLITE:
    return createSatelliteEmbeddingModel(config);
```

### 5. Health Check Support

```java
/**
 * Check if satellite service is available.
 *
 * @param config MAIProvider with satellite endpoint
 * @return true if satellite responds to health check
 */
public static boolean isSatelliteHealthy(MAIProvider config) {
    if (!PROVIDER_SATELLITE.equals(config.getAIGProviderType())) {
        return true;  // Not a satellite provider
    }

    String endpoint = config.getEndpoint();
    if (endpoint == null || endpoint.isEmpty()) {
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
        log.warning("Satellite health check failed: " + e.getMessage());
        return false;
    }
}
```

---

## Context Passing

### iDempiereContext DTO

The satellite needs iDempiere context for security and audit:

```java
/**
 * Context passed to satellite service with each request.
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
// In request to satellite
X-iDempiere-Client-ID: 1000000
X-iDempiere-Org-ID: 1000000
X-iDempiere-User-ID: 100
X-iDempiere-Role-ID: 102
X-iDempiere-Session-ID: abc123
X-iDempiere-Window-ID: 143
X-iDempiere-Language: en_US
```

---

## Satellite REST API Specification

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

The satellite routes requests based on model name:

| Model Name | Upstream Provider |
|------------|-------------------|
| `claude-*` | Anthropic |
| `anthropic.*` | AWS Bedrock |
| `gpt-*` | OpenAI |
| `llama*` | Ollama |
| `mistral*` | Ollama |

---

## Pros and Cons of the Options

### Option 1: New SATELLITE Provider Type (Chosen)

- Good, because it integrates cleanly with existing factory pattern
- Good, because it allows per-tenant choice of satellite vs direct
- Good, because it maintains backward compatibility
- Good, because it uses standard REST/HTTP (no new dependencies)
- Good, because it enables gradual migration
- Neutral, because it adds network latency (one hop)
- Bad, because it requires satellite service deployment

### Option 2: Replace All Providers with Satellite Proxy

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

1. Add `PROVIDER_SATELLITE` constant
2. Add AD_Ref_List entry
3. Implement factory methods
4. Add health check support

### Phase 2: Deploy Satellite Service

1. Create Quarkus satellite project (separate repo)
2. Implement OpenAI-compatible REST endpoints
3. Add authentication and context handling
4. Deploy to test environment

### Phase 3: Tenant Migration

1. Create satellite AIG_Provider record for test tenant
2. Validate end-to-end flow
3. Migrate production tenants gradually
4. Monitor metrics and costs

### Phase 4: Advanced Features

1. Enable MCP server in satellite
2. Add extended thinking support
3. Implement response caching
4. Add multi-tenant isolation

---

## Development: Mock Satellite Server

A **MockSatelliteServer** is provided for development and testing while the real Quarkus Satellite service is being built.

### Quick Start

```bash
# Start mock server on default port 8090
./run-mock-satellite.sh

# Start on custom port with ECHO mode
./run-mock-satellite.sh 8091 ECHO
```

### Configure iDempiere

Create an `AIG_Provider` record:

| Field | Value |
|-------|-------|
| Name | Mock Satellite |
| Type | SAT (Quarkus Satellite) |
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
| `src/.../satellite/MockSatelliteServer.java` | Mock server implementation |
| `src/.../satellite/SatelliteProviderConstants.java` | Constants and headers |
| `run-mock-satellite.sh` | Startup script |
| `org.idempiere.test/.../MockSatelliteServerTest.java` | Unit tests |

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

If satellite is unavailable, the factory can fall back to direct providers:

```java
public static ChatLanguageModel createWithFallback(MAIProvider primary, MAIProvider fallback) {
    if (PROVIDER_SATELLITE.equals(primary.getAIGProviderType())) {
        if (!isSatelliteHealthy(primary)) {
            log.warning("Satellite unavailable, using fallback provider");
            return create(fallback);
        }
    }
    return create(primary);
}
```

### Related ADRs

- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j Strategic Adoption
- [ADR-035](035-java-version-strategy.md) - Java Version Strategy (explains Java 11 constraint)
- [ADR-038](038-quarkus-satellite-ai-service.md) - Quarkus Satellite AI Service Architecture (research)
- [ADR-040](040-embedding-ingestion-evolution.md) - Embedding Ingestion Evolution (alternative for RAG)

### References

- [Quarkus LangChain4j Extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [OpenAI API Specification](https://platform.openai.com/docs/api-reference)
- [LangChain4j Documentation](https://docs.langchain4j.dev/)

---

*ADR-042 | Version 1.0 | 2025-12-10*
*Status: Proposed*
*Next Step: Implement PROVIDER_SATELLITE in LangChain4jProviderFactory*
