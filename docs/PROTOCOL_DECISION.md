# Protocol Decision: Hybrid Approach

**Date:** 2025-12-11
**Status:** RECOMMENDED
**Decision:** Use **Hybrid Protocol Architecture**

---

## TL;DR

- **iDempiere → AI Hub:** Cloudempiere AI Protocol v1 (custom)
- **AI Hub → LLMs:** OpenAI-Compatible API (industry standard)
- **Rationale:** Use standards where they fit, extend where iDempiere needs more

---

## Research Summary

### Industry Standards Evaluated (2025)

1. **OpenAI-Compatible API** - De facto standard for LLM gateways
   - Used by: LiteLLM, Portkey, Helicone, OpenRouter
   - Strengths: Wide ecosystem, LangChain4j support
   - Weaknesses: No multi-tenancy, RBAC, or ERP context

2. **AWS Bedrock API** - AWS-specific multi-model protocol
   - Strengths: Production-grade, cost tracking
   - Weaknesses: AWS-locked, no iDempiere context

3. **Agent-to-Agent (A2A) Protocol** - New 2025 standard for multi-agent communication
   - Strengths: Agent orchestration, framework-agnostic
   - Weaknesses: Too new, agent-focused not client-focused

4. **Model Context Protocol (MCP)** - Agent-tool communication
   - Strengths: Standardizes tool calling
   - Weaknesses: For tools not HTTP APIs, needs Java 17

---

## Why Not Pure OpenAI-Compatible?

**Problem:** OpenAI API lacks iDempiere-specific features:

| Feature Needed | OpenAI API | Notes |
|----------------|------------|-------|
| Multi-tenancy (AD_Client_ID) | ❌ | No concept of clients/orgs |
| RBAC (AD_Role_ID) | ❌ | Only API key auth |
| User context (AD_User_ID) | ❌ | No user identity |
| Session management | ❌ | Stateless |
| Conversation persistence | ❌ | Client-side only |
| Window/record context | ❌ | No UI context |
| Database query execution | ❌ | No ERP operations |
| Process execution | ❌ | No business logic |
| Audit trail | ❌ | No audit concept |

**Attempting to hack these into OpenAI format:**
```json
{
  "model": "claude-sonnet-4",
  "messages": [...],

  // ❌ These break OpenAI compatibility
  "idempiere_context": {...},
  "conversation_id": "...",
  "extra": {...}
}
```

**Result:** Not really OpenAI-compatible, confuses developers, can't use standard tools.

---

## Why Not Pure Custom Protocol?

**Problem:** Custom protocol means:

1. ❌ No LangChain4j built-in support → custom HTTP client
2. ❌ No ecosystem tooling (monitoring, testing, SDKs)
3. ❌ Can't use LiteLLM/Portkey for provider routing
4. ❌ More code to write and maintain
5. ❌ Provider switching requires rewriting HTTP layer

---

## ✅ Solution: Hybrid Protocol Architecture

### **Layer 1: iDempiere → AI Hub**

**Protocol:** Cloudempiere AI Protocol v1 (custom)

**Endpoint:** `POST /ai/v1/chat`

**Request:**
```json
{
  "request_id": "req-12345",
  "version": "1.0",

  "security": {
    "client_id": 1000000,
    "org_id": 1000000,
    "user_id": 100,
    "role_id": 102,
    "session_id": "sess-abc123",
    "language": "en_US"
  },

  "provider": {
    "type": "anthropic",
    "model": "claude-sonnet-4",
    "fallback": {
      "type": "openai",
      "model": "gpt-4o"
    }
  },

  "conversation": {
    "id": "conv-789",
    "messages": [...],
    "context": {
      "window_id": 143,
      "record_id": 1000050
    }
  },

  "parameters": {
    "temperature": 0.7,
    "stream": true
  },

  "capabilities": {
    "database_query": true,
    "process_execution": false,
    "tools": [...]
  }
}
```

**Why:**
- ✅ Full iDempiere context (all AD_* IDs)
- ✅ Security first-class (not hacked in)
- ✅ Provider routing native
- ✅ Conversation management built-in
- ✅ ERP operations specified
- ✅ Clear, unambiguous contract

---

### **Layer 2: AI Hub → LLM Providers**

**Protocol:** OpenAI-Compatible API (industry standard)

**Implementation:**
```java
// In AI Hub Service
public AIResponse chat(CloudempiereRequest request) {
    // 1. Validate security context
    validateContext(request.security());

    // 2. Enforce RBAC
    enforcePermissions(request.security(), request.capabilities());

    // 3. Convert to OpenAI format
    OpenAIRequest openaiReq = toOpenAIFormat(request);

    // 4. Use LangChain4j (OpenAI-compatible)
    var model = switch(request.provider().type()) {
        case "anthropic" -> OpenAiChatModel.builder()
            .baseUrl("https://api.anthropic.com/v1")
            .apiKey(getProviderKey("anthropic"))
            .modelName(request.provider().model())
            .build();
        case "openai" -> OpenAiChatModel.builder()
            .baseUrl("https://api.openai.com/v1")
            .apiKey(getProviderKey("openai"))
            .modelName(request.provider().model())
            .build();
        // ... other providers
    };

    // 5. Execute with OpenAI-compatible client
    Response response = model.generate(openaiReq.messages());

    // 6. Convert back to Cloudempiere format
    return toCloudempiereFormat(response, request);
}
```

**Why:**
- ✅ LangChain4j handles all provider HTTP details
- ✅ Works with 100+ providers via LangChain4j
- ✅ Can add LiteLLM/Portkey in front if needed
- ✅ Standard ecosystem tooling works
- ✅ Easy provider switching (just change baseUrl)

---

## Protocol Adapter Pattern

```
┌──────────────────────────────────────┐
│   Cloudempiere Protocol v1           │
│                                      │
│   {                                  │
│     "security": {...},               │
│     "provider": {...},               │
│     "conversation": {...},           │
│     "capabilities": {...}            │
│   }                                  │
└────────────────┬─────────────────────┘
                 │
                 ↓
┌────────────────────────────────────────┐
│   Protocol Adapter                     │
│                                        │
│   1. Extract security context          │
│   2. Validate & enforce RBAC           │
│   3. Route to provider                 │
│   4. Convert to OpenAI format          │
│   5. Strip iDempiere fields            │
└────────────────┬───────────────────────┘
                 │
                 ↓
┌──────────────────────────────────────┐
│   OpenAI-Compatible Format           │
│                                      │
│   {                                  │
│     "model": "claude-sonnet-4",      │
│     "messages": [...],               │
│     "stream": true,                  │
│     "temperature": 0.7               │
│   }                                  │
└────────────────┬─────────────────────┘
                 │
                 ↓
         LangChain4j Client
                 │
        ┌────────┼────────┐
        ↓        ↓        ↓
    Anthropic  OpenAI  Ollama
```

---

## Benefits of Hybrid Approach

### 1. Best of Both Worlds

| Layer | Protocol | Benefit |
|-------|----------|---------|
| iDempiere → AI Hub | Custom (Cloudempiere) | Full iDempiere features |
| AI Hub → LLMs | Standard (OpenAI) | Ecosystem & LangChain4j |

### 2. Clean Separation

- **Business Logic** (security, RBAC, audit) → AI Hub (custom protocol)
- **Technical Integration** (HTTP, providers) → LangChain4j (OpenAI protocol)

### 3. Future-Proof

- If OpenAI protocol evolves → change adapter only
- If iDempiere needs more features → extend custom protocol only
- Layers are independent

### 4. Developer Experience

**iDempiere Plugin Developer:**
```java
// Works with iDempiere concepts
CloudempiereRequest request = CloudempiereRequest.builder()
    .security(SecurityContext.fromEnv(ctx))  // Env.getCtx()
    .provider("anthropic", "claude-sonnet-4")
    .userMessage("Show me open orders")
    .build();

CloudempiereResponse response = AI HubClient.chat(request);
```

**AI Hub Developer:**
```java
// Works with standard LangChain4j
var model = OpenAiChatModel.builder()
    .baseUrl(getProviderUrl(provider))
    .apiKey(getProviderKey(provider))
    .build();
```

---

## Implementation Phases

### Phase 1: Define Cloudempiere Protocol v1 (Done ✅)

- [x] Request/response format
- [x] Security model
- [x] Provider routing
- [x] Conversation management
- [x] Error handling
- [x] Versioning

**Artifact:** `/docs/CLOUDEMPIERE_AI_PROTOCOL_V1.md`

---

### Phase 2: Implement AI Hub Service (2 weeks)

**Week 1: Core Protocol Handler**
1. ✅ Accept Cloudempiere Protocol v1 requests
2. ✅ Validate security context against iDempiere DB
3. ✅ Enforce RBAC via AD_Role lookups
4. ✅ Implement protocol adapter

**Week 2: Provider Integration**
1. ✅ Convert Cloudempiere → OpenAI format
2. ✅ Use LangChain4j OpenAiChatModel
3. ✅ Convert OpenAI → Cloudempiere format
4. ✅ Add observability (audit, cost, metrics)

---

### Phase 3: Update iDempiere Plugin (1 week)

1. ✅ Create CloudempiereClient (HTTP client for v1 protocol)
2. ✅ Replace OpenAI-compatible requests with v1 protocol
3. ✅ Update AIService to use new client
4. ✅ Add security context from Env.getCtx()

---

### Phase 4: Testing & Migration (1 week)

1. ✅ Integration tests (iDempiere → AI Hub → LLM)
2. ✅ RBAC enforcement tests
3. ✅ Provider routing tests
4. ✅ Performance testing
5. ✅ Migration guide for existing deployments

---

## Comparison: Before vs After

### Before (OpenAI-Compatible with Extensions)

**Problems:**
- ❌ Not really OpenAI-compatible (extensions break it)
- ❌ iDempiere context hacked into non-standard fields
- ❌ Confusing to developers (looks like OpenAI but isn't)
- ❌ Can't use standard OpenAI tools
- ❌ RBAC unclear where enforced

**Example:**
```json
// Looks like OpenAI but has weird extensions
{
  "model": "claude-sonnet-4",
  "messages": [...],
  "idempiere_context": {...},  // ❌ Not OpenAI
  "conversation_id": "..."      // ❌ Not OpenAI
}
```

---

### After (Hybrid Protocol)

**Advantages:**
- ✅ Clear separation: Custom for iDempiere, Standard for LLMs
- ✅ iDempiere features first-class (not hacked in)
- ✅ LangChain4j integration clean
- ✅ Can use ecosystem tools at LLM layer
- ✅ RBAC clearly enforced at AI Hub

**Example:**
```json
// Cloudempiere Protocol v1 (iDempiere → AI Hub)
{
  "version": "1.0",
  "security": {
    "client_id": 1000000,
    "role_id": 102
  },
  "provider": {
    "type": "anthropic",
    "model": "claude-sonnet-4"
  },
  "conversation": {...}
}

// ↓ Converted to ↓

// OpenAI-Compatible (AI Hub → LLM)
{
  "model": "claude-sonnet-4",
  "messages": [...],
  "stream": true
}
```

---

## Alternative Considered: Pure OpenAI + Extensions

**Approach:** Keep OpenAI-compatible, add iDempiere fields as extensions

**Rejected Because:**
1. Breaks OpenAI compatibility (can't use standard tools)
2. Other OpenAI-compatible gateways would ignore extensions
3. Confusing to developers expecting real OpenAI API
4. Security model feels bolted-on

**Example of what we DON'T want:**
```json
{
  "model": "claude-sonnet-4",  // OpenAI
  "messages": [...],            // OpenAI
  "stream": true,               // OpenAI

  // ❌ These make it NOT OpenAI-compatible
  "x-idempiere-client-id": 1000000,
  "x-idempiere-context": {...},
  "x-conversation-id": "..."
}
```

---

## Decision Rationale

### Why Hybrid Wins

| Requirement | Pure OpenAI | Pure Custom | Hybrid | Winner |
|-------------|-------------|-------------|--------|---------|
| iDempiere features | ❌ Hacked | ✅ Native | ✅ Native | Tie |
| LangChain4j support | ✅ Built-in | ❌ Custom | ✅ Built-in | Tie |
| Ecosystem tools | ⚠️ Partial | ❌ None | ✅ At LLM layer | Hybrid |
| Clear contracts | ❌ Confusing | ✅ Clear | ✅ Clear | Tie |
| Maintainability | ⚠️ Messy | ⚠️ More code | ✅ Clean layers | Hybrid |
| Developer UX | ⚠️ Confusing | ✅ Clear | ✅ Clear | Tie |

**Winner:** Hybrid (best trade-offs)

---

## Implementation Guide

### For AI Hub Service Developers

```java
@Path("/ai/v1")
@Produces(MediaType.APPLICATION_JSON)
public class CloudempiereAIResource {

    @POST
    @Path("/chat")
    public Response chat(CloudempiereRequest request) {
        // 1. Validate Cloudempiere protocol
        validate(request);

        // 2. Security & RBAC
        SecurityContext sec = request.security();
        validateUser(sec.userId(), sec.clientId());
        enforceRole(sec.roleId(), request.capabilities());

        // 3. Provider routing
        Provider provider = routeProvider(request.provider());

        // 4. Convert to OpenAI format
        OpenAIRequest openaiReq = adaptToOpenAI(request);

        // 5. Call LLM via LangChain4j
        var model = createLangChain4jModel(provider);
        var response = model.generate(openaiReq.messages());

        // 6. Convert back to Cloudempiere format
        CloudempiereResponse ceResponse = adaptFromOpenAI(response, request);

        // 7. Audit
        audit(request, ceResponse);

        return Response.ok(ceResponse).build();
    }
}
```

### For iDempiere Plugin Developers

```java
// In your iDempiere process/form/window
Properties ctx = Env.getCtx();

CloudempiereClient client = CloudempiereClient.builder()
    .baseUrl("http://AI Hub:8090")
    .sessionToken(getSessionToken(ctx))
    .build();

CloudempiereRequest request = CloudempiereRequest.builder()
    .security(SecurityContext.fromCtx(ctx))  // Auto-extracts AD_Client_ID, etc.
    .provider("anthropic", "claude-sonnet-4")
    .userMessage("Show me open sales orders")
    .withDatabaseAccess()
    .build();

CloudempiereResponse response = client.chat(request);
```

---

## Conclusion

**Decision: Use Hybrid Protocol Architecture**

- **iDempiere → AI Hub:** Cloudempiere AI Protocol v1
- **AI Hub → LLMs:** OpenAI-Compatible API

**Rationale:**
- Use industry standards where they fit (LLM communication)
- Extend where iDempiere needs more (security, RBAC, multi-tenancy)
- Clean separation of concerns (business logic vs technical integration)
- Best developer experience on both sides

**Next Steps:**
1. Implement protocol adapter in AI Hub service
2. Update iDempiere plugin to use v1 protocol
3. Integration testing
4. Production deployment

---

## References

- [AWS Bedrock API Reference](https://docs.aws.amazon.com/bedrock/latest/APIReference/welcome.html)
- [LiteLLM Documentation](https://docs.litellm.ai/)
- [Portkey AI Gateway](https://portkey.ai/features/ai-gateway)
- [Top LLM Gateways 2025](https://www.helicone.ai/blog/top-llm-gateways-comparison-2025)
- [Agent-to-Agent Protocol](https://www.infoq.com/news/2025/11/a2a-amazon-bedrock-agentcore/)

---

*Protocol Decision Document | Version 1.0 | 2025-12-11*
