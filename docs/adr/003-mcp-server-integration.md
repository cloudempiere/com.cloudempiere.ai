# ADR-003: REST API for External AI Agent Access

**Status:** Proposed (Target: v0.11.0)
**Date:** 2025-12-01
**Implementation Target:** Q1 2026

---

## Context

### Problem Statement

The com.cloudempiere.ai plugin currently provides AI capabilities only through the iDempiere ZK web UI. External AI agents (Claude Code, automation tools, third-party services) cannot access these capabilities programmatically.

**Key Requirements:**
1. Enable external agents to chat with AI using iDempiere context
2. Allow secure database queries from external tools
3. Maintain existing security model (role-based access control)
4. Preserve full audit trail of external access
5. No disruption to existing ZK UI functionality

### Constraints

- Must use existing iDempiere infrastructure (no new technology stack)
- Must work with existing AIConversationService and SecureDatabaseQueryExecutor
- Cannot expose raw database access without validation
- Must support multiple external clients simultaneously
- Must log all operations for compliance

---

## Decision

Add **REST API endpoints** to the existing iDempiere AI plugin using JAX-RS/Jersey, exposing AI capabilities through HTTP.

### Architecture

```
┌─────────────────────────────────────┐
│  External AI Agents                 │
│  - Claude Code                      │
│  - Automation tools                 │
│  - Custom integrations              │
└────────────┬────────────────────────┘
             │ HTTP/REST
             ▼
┌─────────────────────────────────────┐
│  iDempiere (Port 8080)              │
│  ┌───────────────────────────────┐  │
│  │  New: REST API Endpoints      │  │
│  │  /api/ai/chat                 │  │
│  │  /api/ai/query                │  │
│  │  /api/ai/context              │  │
│  │  /api/ai/providers            │  │
│  │  /api/ai/history              │  │
│  └────────────┬──────────────────┘  │
│               │                     │
│  ┌────────────▼──────────────────┐  │
│  │  Existing: AIConversationService│ │
│  │  Existing: SecureDatabaseQueryExecutor│
│  │  Existing: AIContextProviderRegistry│
│  └─────────────────────────────────┘  │
└─────────────────────────────────────┘
```

### Why REST API (Not MCP Server)

**Rejected Alternative:** Separate Node.js MCP server
- ❌ Additional technology stack (Node.js)
- ❌ Separate deployment and maintenance
- ❌ Network overhead between MCP server and iDempiere
- ❌ More complexity

**Chosen Solution:** REST endpoints in Java
- ✅ Uses existing iDempiere infrastructure
- ✅ Same technology stack (Java/OSGi)
- ✅ Direct access to existing services
- ✅ Simpler deployment
- ✅ Lower latency

---

## Implementation

### New Package Structure

```
src/com/cloudempiere/ai/http/
├── AIRestController.java       # Main REST controller
├── dto/
│   ├── ChatRequest.java        # Request DTOs
│   ├── QueryRequest.java
│   ├── ContextRequest.java
│   ├── ChatResponse.java       # Response DTOs
│   └── ErrorResponse.java
├── validation/
│   └── RequestValidator.java   # Input validation
└── security/
    └── APIKeyAuthFilter.java   # API key authentication
```

### REST API Endpoints

#### 1. Chat Endpoint

**POST** `/api/ai/chat`

Request:
```json
{
  "message": "Show active orders",
  "ad_user_id": 100,
  "ad_role_id": 102,
  "ad_org_id": 11,
  "session_id": "uuid",
  "window_id": 143,
  "history_depth": 10
}
```

Response:
```json
{
  "session_id": "uuid",
  "response": "You have 12 active orders...",
  "tokens_used": {"input": 150, "output": 450},
  "timestamp": "2025-12-01T10:30:00Z"
}
```

#### 2. Query Endpoint

**POST** `/api/ai/query`

Request:
```json
{
  "sql": "SELECT Name FROM M_Product WHERE IsActive='Y'",
  "purpose": "List products",
  "max_rows": 50,
  "ad_user_id": 100,
  "ad_role_id": 102,
  "ad_org_id": 11
}
```

Response:
```json
{
  "rows": [{"Name": "Product A"}, {"Name": "Product B"}],
  "row_count": 2,
  "columns": ["Name"],
  "execution_time_ms": 245,
  "audit_id": 12345
}
```

#### 3. Context Extraction Endpoint

**POST** `/api/ai/context`

Request:
```json
{
  "context_type": "WINDOW",
  "window_id": 143,
  "ad_user_id": 100,
  "ad_org_id": 11
}
```

Response:
```json
{
  "window_metadata": {...},
  "tab_context": {...},
  "record_data": {...},
  "extracted_at": "2025-12-01T10:30:00Z"
}
```

#### 4. Provider Info Endpoint

**GET** `/api/ai/providers`

Response:
```json
{
  "providers": [
    {
      "id": 1000001,
      "name": "Anthropic Claude",
      "type": "ANTHROPIC",
      "available_models": ["claude-3-5-sonnet-20241022"],
      "health": "HEALTHY"
    }
  ]
}
```

#### 5. Conversation History Endpoint

**GET** `/api/ai/history?session_id=uuid&limit=10`

Response:
```json
{
  "session_id": "uuid",
  "messages": [
    {"role": "user", "content": "Hello", "timestamp": "..."},
    {"role": "assistant", "content": "Hi!", "timestamp": "..."}
  ],
  "message_count": 2
}
```

### Implementation Code

**File:** `src/com/cloudempiere/ai/http/AIRestController.java`

```java
package com.cloudempiere.ai.http;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.json.JSONObject;
import java.util.Properties;

import com.cloudempiere.ai.service.AIConversationService;
import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.context.AIContextProviderRegistry;
import org.compiere.util.Env;

/**
 * REST API for AI capabilities
 * Accessible at: /api/ai/*
 */
@Path("/api/ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AIRestController {

    private AIConversationService conversationService;
    private SecureDatabaseQueryExecutor queryExecutor;

    public AIRestController() {
        this.conversationService = new AIConversationService();
        this.queryExecutor = new SecureDatabaseQueryExecutor();
    }

    /**
     * POST /api/ai/chat
     */
    @POST
    @Path("/chat")
    public Response chat(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);

            // Validate API key
            String apiKey = getApiKey(request);
            if (!validateApiKey(apiKey)) {
                return errorResponse(401, "Invalid API key");
            }

            // Initialize context
            Properties ctx = initializeContext(
                request.getInt("ad_user_id"),
                request.getInt("ad_role_id"),
                request.getInt("ad_org_id")
            );

            // Extract context if window_id provided
            Map<String, Object> contextData = new HashMap<>();
            if (request.has("window_id")) {
                contextData = AIContextProviderRegistry.extractWindowContext(
                    ctx,
                    request.getInt("window_id")
                );
            }

            // Get or create chat session
            String sessionId = request.optString("session_id",
                UUID.randomUUID().toString());
            MAIChat chat = getOrCreateChat(sessionId, ctx);

            // Call AI service
            AIResponse aiResponse = conversationService.sendMessage(
                ctx,
                chat,
                request.getString("message"),
                contextData,
                request.optInt("history_depth", 10)
            );

            // Build response
            JSONObject response = new JSONObject();
            response.put("session_id", sessionId);
            response.put("response", aiResponse.getContent());
            response.put("tokens_used", new JSONObject()
                .put("input", aiResponse.getTokenUsage().getInputTokens())
                .put("output", aiResponse.getTokenUsage().getOutputTokens())
            );
            response.put("timestamp", new Date().toInstant().toString());

            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            return errorResponse(500, e.getMessage());
        }
    }

    /**
     * POST /api/ai/query
     */
    @POST
    @Path("/query")
    public Response query(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);

            // Validate API key
            String apiKey = getApiKey(request);
            if (!validateApiKey(apiKey)) {
                return errorResponse(401, "Invalid API key");
            }

            // Initialize context
            Properties ctx = initializeContext(
                request.getInt("ad_user_id"),
                request.getInt("ad_role_id"),
                request.getInt("ad_org_id")
            );

            // Build secure query request
            SecureQueryRequest queryReq = SecureQueryRequest.builder()
                .sql(request.getString("sql"))
                .purpose(request.getString("purpose"))
                .maxRows(request.optInt("max_rows", 100))
                .build();

            // Execute query
            SecureQueryResult result = queryExecutor.execute(ctx, queryReq);

            // Build response
            JSONObject response = new JSONObject();
            response.put("rows", result.getRowsAsJson());
            response.put("row_count", result.getRowCount());
            response.put("columns", result.getColumnNames());
            response.put("execution_time_ms", result.getExecutionTimeMs());
            response.put("audit_id", result.getAuditId());

            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            return errorResponse(500, e.getMessage());
        }
    }

    /**
     * POST /api/ai/context
     */
    @POST
    @Path("/context")
    public Response extractContext(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);

            // Validate API key
            String apiKey = getApiKey(request);
            if (!validateApiKey(apiKey)) {
                return errorResponse(401, "Invalid API key");
            }

            // Initialize context
            Properties ctx = initializeContext(
                request.getInt("ad_user_id"),
                request.getInt("ad_org_id")
            );

            // Extract context based on type
            String contextType = request.getString("context_type");
            Map<String, Object> context = null;

            if ("WINDOW".equals(contextType)) {
                context = AIContextProviderRegistry.extractWindowContext(
                    ctx,
                    request.getInt("window_id")
                );
            } else if ("CHART".equals(contextType)) {
                context = AIContextProviderRegistry.extractChartContext(
                    ctx,
                    request.getInt("chart_id")
                );
            }

            JSONObject response = new JSONObject(context);
            response.put("extracted_at", new Date().toInstant().toString());

            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            return errorResponse(500, e.getMessage());
        }
    }

    /**
     * GET /api/ai/providers
     */
    @GET
    @Path("/providers")
    public Response getProviders() {
        try {
            AIProviderFactory factory = new AIProviderFactory();
            List<MAIProvider> providers = factory.getAvailableProviders();

            JSONArray providersArray = new JSONArray();
            for (MAIProvider provider : providers) {
                JSONObject p = new JSONObject();
                p.put("id", provider.get_ID());
                p.put("name", provider.getName());
                p.put("type", provider.getProviderType());
                p.put("available_models", provider.getAvailableModels());
                p.put("health", provider.getHealthStatus());
                providersArray.put(p);
            }

            JSONObject response = new JSONObject();
            response.put("providers", providersArray);

            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            return errorResponse(500, e.getMessage());
        }
    }

    /**
     * GET /api/ai/history
     */
    @GET
    @Path("/history")
    public Response getHistory(
        @QueryParam("session_id") String sessionId,
        @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        try {
            // Load chat and entries
            MAIChat chat = loadChat(sessionId);
            if (chat == null) {
                return errorResponse(404, "Session not found");
            }

            List<MAIChatEntry> entries = loadChatEntries(chat.get_ID(), limit);

            JSONArray messages = new JSONArray();
            for (MAIChatEntry entry : entries) {
                JSONObject msg = new JSONObject();
                msg.put("role", entry.getRole());
                msg.put("content", entry.getMessage());
                msg.put("timestamp", entry.getCreated().toInstant().toString());
                messages.put(msg);
            }

            JSONObject response = new JSONObject();
            response.put("session_id", sessionId);
            response.put("messages", messages);
            response.put("message_count", entries.size());

            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            return errorResponse(500, e.getMessage());
        }
    }

    // Helper methods

    private Properties initializeContext(int userId, int roleId, int orgId) {
        Properties ctx = new Properties();
        ctx.setProperty("#AD_Client_ID", "11"); // Get from config
        ctx.setProperty("#AD_User_ID", String.valueOf(userId));
        ctx.setProperty("#AD_Role_ID", String.valueOf(roleId));
        ctx.setProperty("#AD_Org_ID", String.valueOf(orgId));
        return ctx;
    }

    private String getApiKey(JSONObject request) {
        // Get from request header or body
        return request.optString("api_key", null);
    }

    private boolean validateApiKey(String apiKey) {
        // Validate against database or configuration
        String validKey = System.getenv("IDEMPIERE_AI_API_KEY");
        return validKey != null && validKey.equals(apiKey);
    }

    private Response errorResponse(int status, String message) {
        JSONObject error = new JSONObject();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", new Date().toInstant().toString());
        return Response.status(status).entity(error.toString()).build();
    }

    private MAIChat getOrCreateChat(String sessionId, Properties ctx) {
        // Implementation to get or create chat session
        return null; // TODO
    }

    private MAIChat loadChat(String sessionId) {
        // Implementation to load chat by session ID
        return null; // TODO
    }

    private List<MAIChatEntry> loadChatEntries(int chatId, int limit) {
        // Implementation to load chat entries
        return new ArrayList<>(); // TODO
    }
}
```

### Security Implementation

**File:** `src/com/cloudempiere/ai/http/security/APIKeyAuthFilter.java`

```java
package com.cloudempiere.ai.http.security;

import javax.ws.rs.container.*;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * API Key authentication filter for REST endpoints
 */
@PreMatching
public class APIKeyAuthFilter implements ContainerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String apiKey = requestContext.getHeaderString(API_KEY_HEADER);

        if (!isValidApiKey(apiKey)) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Invalid or missing API key\"}")
                    .build()
            );
        }
    }

    private boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }

        // Validate against environment variable or database
        String validKey = System.getenv("IDEMPIERE_AI_API_KEY");
        return validKey != null && validKey.equals(apiKey);
    }
}
```

### OSGi Service Registration

**File:** `OSGI-INF/com.cloudempiere.ai.http.AIRestController.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.3.0"
               name="com.cloudempiere.ai.http.AIRestController">
   <implementation class="com.cloudempiere.ai.http.AIRestController"/>
   <service>
      <provide interface="javax.ws.rs.core.Application"/>
   </service>
   <property name="service.ranking" type="Integer" value="100"/>
   <property name="jersey.context.path" value="/api/ai"/>
</scr:component>
```

---

## Security Model

### Authentication

**API Key-based:**
- External clients provide API key in `X-API-Key` header
- API keys stored in iDempiere (new table: `AIG_APIKey`)
- Each API key linked to AD_User_ID and AD_Role_ID
- Keys can be revoked/rotated

### Authorization

**Role-based permissions:**
- API key maps to AD_User_ID + AD_Role_ID
- All queries executed with user's role permissions
- No privilege escalation possible
- Row-level security enforced via AccessSqlParser

### Audit Trail

**All operations logged:**
- API key used (which external client)
- User ID and Role ID
- Operation performed (chat/query/context)
- Timestamp and result
- Stored in existing AIG_QueryAudit table

### Input Validation

**Request validation:**
- All parameters validated before processing
- SQL injection prevention (only SELECT allowed)
- Rate limiting per API key
- Maximum row limits enforced

---

## Implementation Timeline

### Version 0.11.0 (Target: Q1 2026)

**Week 1-2: Core Implementation**
- ✅ Create `com.cloudempiere.ai.http` package
- ✅ Implement AIRestController with 5 endpoints
- ✅ Add input validation
- ✅ API key authentication

**Week 3: Security & Testing**
- ✅ Security audit
- ✅ Integration tests
- ✅ Load testing
- ✅ Documentation

**Week 4: Deployment**
- ✅ OSGi service registration
- ✅ Configuration documentation
- ✅ API documentation (Swagger/OpenAPI)
- ✅ Production deployment

### Dependencies

**Existing (No changes needed):**
- AIConversationService
- SecureDatabaseQueryExecutor
- AIContextProviderRegistry
- AIProviderFactory

**New (To be added):**
- JAX-RS/Jersey (for REST)
- API key management table
- Request/Response DTOs

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testChatEndpoint_ValidRequest_ReturnsResponse() {
    // Arrange
    String request = "{\"message\":\"Hello\",\"ad_user_id\":100,\"ad_role_id\":102,\"ad_org_id\":11}";

    // Act
    Response response = controller.chat(request);

    // Assert
    assertEquals(200, response.getStatus());
    JSONObject json = new JSONObject(response.getEntity());
    assertTrue(json.has("response"));
    assertTrue(json.has("session_id"));
}

@Test
public void testQueryEndpoint_InvalidApiKey_Returns401() {
    // Arrange
    String request = "{\"sql\":\"SELECT * FROM M_Product\",\"api_key\":\"invalid\"}";

    // Act
    Response response = controller.query(request);

    // Assert
    assertEquals(401, response.getStatus());
}

@Test
public void testQueryEndpoint_SQLInjection_Blocked() {
    // Arrange
    String request = "{\"sql\":\"SELECT * FROM M_Product; DROP TABLE M_Product;\"}";

    // Act
    Response response = controller.query(request);

    // Assert
    assertEquals(400, response.getStatus());
}
```

### Integration Tests

```bash
# Test chat endpoint
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-key-here" \
  -d '{
    "message": "Show active orders",
    "ad_user_id": 100,
    "ad_role_id": 102,
    "ad_org_id": 11
  }'

# Test query endpoint
curl -X POST http://localhost:8080/api/ai/query \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-key-here" \
  -d '{
    "sql": "SELECT Name FROM M_Product WHERE IsActive='\''Y'\'' LIMIT 10",
    "purpose": "List products",
    "ad_user_id": 100,
    "ad_role_id": 102,
    "ad_org_id": 11
  }'
```

---

## Deployment

### Configuration

**Environment Variables:**
```bash
# API key for external access
export IDEMPIERE_AI_API_KEY="your-secure-key-here"

# Rate limiting (requests per minute)
export IDEMPIERE_AI_RATE_LIMIT=60

# Enable/disable external API
export IDEMPIERE_AI_API_ENABLED=true
```

**iDempiere System Configurator:**
- Add "AI_API_ENABLED" parameter
- Add "AI_API_KEY" parameter (encrypted)
- Add "AI_API_RATE_LIMIT" parameter

### Production Checklist

- [ ] API key generated (32+ chars, cryptographically random)
- [ ] HTTPS enabled (TLS 1.2+)
- [ ] Rate limiting configured
- [ ] Audit logging verified
- [ ] Security headers added (CORS, CSP)
- [ ] Monitoring/alerting set up
- [ ] Documentation published
- [ ] Backup/restore tested

---

## Alternatives Considered

### Alternative 1: Separate Node.js MCP Server

**Approach:** External Node.js service calling iDempiere HTTP API

**Pros:**
- MCP protocol native support
- Easier for JavaScript developers
- Can be deployed independently

**Cons:**
- ❌ Additional technology stack
- ❌ Separate deployment/maintenance
- ❌ Network overhead (2 hops: MCP → Node → iDempiere)
- ❌ More complexity

**Rejected because:** Adds unnecessary complexity when REST API in Java achieves the same goal.

### Alternative 2: GraphQL API

**Approach:** GraphQL endpoint instead of REST

**Pros:**
- Flexible queries
- Single endpoint
- Strong typing

**Cons:**
- ❌ Overkill for simple operations
- ❌ Complex to secure properly
- ❌ Not AI-specific

**Rejected because:** REST is simpler and sufficient for AI agent access.

### Alternative 3: WebSocket/SSE

**Approach:** Real-time streaming connection

**Pros:**
- Real-time updates
- Efficient for streaming responses

**Cons:**
- ❌ More complex than REST
- ❌ Not needed for request/response pattern
- ❌ Harder to load balance

**Rejected because:** REST is adequate for current use cases.

---

## Consequences

### Positive

- ✅ External AI agents can access iDempiere programmatically
- ✅ No new technology stack (stays in Java)
- ✅ Direct access to existing services (low latency)
- ✅ Security model preserved
- ✅ Full audit trail maintained
- ✅ Simple to deploy and maintain
- ✅ No disruption to existing ZK UI

### Negative

- ❌ Need to maintain REST API alongside ZK UI
- ❌ API versioning required for breaking changes
- ❌ Additional testing surface
- ❌ Need API key management

### Neutral

- Documentation must cover both UI and API usage
- Monitoring needs to track API usage separately
- May need API gateway in future for advanced features

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| API response time | < 2 seconds | P95 latency |
| Error rate | < 1% | Failed requests / total |
| Security incidents | 0 | Audit review |
| Uptime | 99.9% | Monthly average |
| Adoption | 5+ external integrations | Within 6 months |

---

## References

- [JAX-RS Specification](https://jakarta.ee/specifications/restful-ws/)
- [Jersey Framework](https://eclipse-ee4j.github.io/jersey/)
- [iDempiere REST API](https://wiki.idempiere.org/en/REST_Web_Services)
- [ADR-001: Initial Architecture](001-initial-architecture.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [CHANGELOG.md](../../CHANGELOG.md)

---

*ADR-003 | Version 3.0 | 2025-12-01*
*Status: **Proposed** (Target: v0.11.0, Q1 2026)*
*Focus: Java REST API for external access*
