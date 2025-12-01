# ADR-003: REST API for External AI Agent Access

**Status:** Proposed (Target: v0.11.0)
**Date:** 2025-12-01
**Deciders:** CloudEmpiere AI Team
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
External AI Agents (Claude Code, automation tools)
    │
    └─── HTTP/REST API
         │
         ▼
┌─────────────────────────────────────┐
│  iDempiere (Port 8080)              │
│  ┌───────────────────────────────┐  │
│  │  New: REST API Layer          │  │
│  │  /api/ai/chat                 │  │
│  │  /api/ai/query                │  │
│  │  /api/ai/context              │  │
│  │  /api/ai/providers            │  │
│  │  /api/ai/history              │  │
│  └────────────┬──────────────────┘  │
│               │                     │
│  ┌────────────▼──────────────────┐  │
│  │  Existing Services:           │  │
│  │  - AIConversationService      │  │
│  │  - SecureDatabaseQueryExecutor│  │
│  │  - AIContextProviderRegistry  │  │
│  └─────────────────────────────────┘  │
└─────────────────────────────────────┘
```

### MCP Integration Options

There are **two ways** to enable MCP (Model Context Protocol) access:

#### Option A: REST API + External MCP Client (Chosen)

**How it works:**
- iDempiere exposes REST endpoints
- External tools call REST API directly via HTTP
- No MCP server needed - standard REST
- Tools can wrap REST calls in their own MCP tools if needed

**Pros:**
- ✅ Simplest implementation (4 weeks)
- ✅ Standard technology (JAX-RS)
- ✅ Works with any HTTP client
- ✅ Lower latency (direct access)

**Cons:**
- ❌ No native MCP protocol support
- ❌ External tools must implement HTTP calls

#### Option B: Built-in Java MCP Server (Deferred)

**How it works:**
- Implement MCP protocol server in Java
- Native tool discovery and schema validation
- Claude Code connects via MCP protocol

**Pros:**
- ✅ Native MCP protocol
- ✅ Better MCP ecosystem integration
- ✅ Single tech stack (Java only)

**Cons:**
- ❌ Complex implementation (8-12 weeks)
- ❌ No mature Java MCP SDK
- ❌ Additional testing surface

**Decision:** Start with **Option A** (REST API), evaluate Option B later based on demand.

### REST API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/ai/chat` | POST | Send message to AI with context |
| `/api/ai/query` | POST | Execute secure database query |
| `/api/ai/context` | POST | Extract window/chart context |
| `/api/ai/providers` | GET | List available AI providers |
| `/api/ai/history` | GET | Retrieve conversation history |

### Implementation Approach

**New Package:** `com.cloudempiere.ai.http`

**Components:**
1. **AIRestController** - JAX-RS controller with 5 endpoints
2. **APIKeyAuthFilter** - API key authentication
3. **RequestValidator** - Input validation
4. **Request/Response DTOs** - Data transfer objects

**Technology Stack:**
- JAX-RS/Jersey (REST framework)
- JSON (request/response format)
- OSGi (service registration)

**Example Request/Response:**

```json
// POST /api/ai/chat
{
  "message": "Show active orders",
  "ad_user_id": 100,
  "ad_role_id": 102,
  "ad_org_id": 11,
  "window_id": 143
}

// Response
{
  "session_id": "uuid",
  "response": "You have 12 active orders...",
  "tokens_used": {"input": 150, "output": 450}
}
```

### Security Model

**Authentication:**
- API key in `X-API-Key` header
- API keys stored in new table: `AIG_APIKey`
- Each key linked to AD_User_ID + AD_Role_ID

**Authorization:**
- API key maps to specific iDempiere user and role
- All queries executed with user's role permissions
- No privilege escalation possible
- Row-level security via AccessSqlParser

**Audit Trail:**
- All operations logged with API key, user, role
- Stored in existing `AIG_QueryAudit` table
- Includes timestamp, operation, result

**Input Validation:**
- Only SELECT queries allowed (no INSERT/UPDATE/DELETE)
- SQL injection prevention
- Rate limiting per API key
- Maximum row limits enforced

---

## Implementation Timeline

**Version 0.11.0 (Q1 2026) - 4 weeks:**

| Week | Tasks |
|------|-------|
| 1-2 | Core implementation (REST endpoints, DTOs, validation) |
| 3 | Security & testing (API keys, integration tests) |
| 4 | Deployment (OSGi registration, documentation) |

**Dependencies:**
- Existing: AIConversationService, SecureDatabaseQueryExecutor
- New: JAX-RS/Jersey, API key management table

---

## Alternatives Considered

### Alternative 1: Java MCP Server (Deferred)

**Approach:** Native MCP protocol in Java

**Why deferred:**
- More complex (8-12 weeks vs 4 weeks)
- Java MCP SDK not mature
- REST API works for 90% of use cases
- Can build on REST API later if needed

### Alternative 2: Node.js MCP Server (Rejected)

**Approach:** External Node.js service

**Why rejected:**
- Additional technology stack
- Separate deployment/maintenance
- Network overhead (2 hops)
- Java MCP server is better if MCP protocol needed

### Alternative 3: GraphQL (Rejected)

**Why rejected:** Overkill for simple operations, complex to secure

### Alternative 4: WebSocket/SSE (Rejected)

**Why rejected:** Not needed for request/response pattern

---

## Consequences

### Positive

- ✅ External AI agents can access iDempiere programmatically
- ✅ No new technology stack (Java only)
- ✅ Direct access to existing services (low latency)
- ✅ Security model preserved (role-based access)
- ✅ Full audit trail maintained
- ✅ Simple to deploy and maintain
- ✅ No disruption to existing ZK UI
- ✅ Fast implementation (4 weeks)

### Negative

- ❌ Need to maintain REST API alongside ZK UI
- ❌ API versioning required for breaking changes
- ❌ Additional testing surface
- ❌ Need API key management infrastructure
- ❌ No native MCP protocol support (deferred)

### Neutral

- Documentation must cover both UI and API usage
- Monitoring needs to track API usage separately
- May need API gateway in future for advanced features

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| API response time | < 2 seconds | P95 latency |
| Error rate | < 1% | Failed/total requests |
| Security incidents | 0 | Audit review |
| Uptime | 99.9% | Monthly average |
| Adoption | 5+ integrations | Within 6 months |

---

## Testing Strategy

**Unit Tests:**
- Endpoint validation (valid/invalid requests)
- API key authentication
- SQL injection prevention

**Integration Tests:**
- End-to-end flow with real AI providers
- Role-based permission enforcement
- Audit logging verification

**Security Tests:**
- Penetration testing
- API key rotation
- Rate limiting

---

## Deployment Checklist

- [ ] API key generated (32+ chars, secure random)
- [ ] HTTPS enabled (TLS 1.2+)
- [ ] Rate limiting configured (60 req/min)
- [ ] Audit logging verified
- [ ] Security headers added (CORS, CSP)
- [ ] Monitoring/alerting set up
- [ ] API documentation published (Swagger/OpenAPI)
- [ ] Backup/restore tested

---

## References

- [JAX-RS Specification](https://jakarta.ee/specifications/restful-ws/)
- [Jersey Framework](https://eclipse-ee4j.github.io/jersey/)
- [iDempiere REST API](https://wiki.idempiere.org/en/REST_Web_Services)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [ADR-001: Initial Architecture](001-initial-architecture.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [CHANGELOG.md](../../CHANGELOG.md) - Current version: v0.10.0

---

## Implementation Notes

**For detailed implementation guidance, see:**
- Implementation code examples in Git history (commit b95d1e8)
- JAX-RS/Jersey documentation
- iDempiere OSGi service registration patterns

**Key Implementation Points:**
1. Wrap existing services (don't reimplement)
2. Use iDempiere's Properties context for user/role
3. Leverage existing SecureDatabaseQueryExecutor for security
4. Follow iDempiere conventions (M* models, CLogger)
5. Test with existing ZK UI to ensure no regression

---

*ADR-003 | Version 3.1 | 2025-12-01*
*Status: **Proposed** (Target: v0.11.0, Q1 2026)*
*Decision: REST API first, Java MCP Server later if needed*
