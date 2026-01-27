# ADR-003: REST API for External AI Agent Access

**Status:** Proposed (Target: v0.11.0)
**Date:** 2025-12-01
**Deciders:** Cloudempiere AI Team
**Implementation Target:** Q1 2026

---

## Context

### Domain Architecture Context

This ADR addresses the **MCP TOOLS domain** from the 3-domain data source architecture described in `docs/DATA_SOURCE_ARCHITECTURE.md`.

**3 Data Domains:**

| Domain | Scope | Implementation |
|--------|-------|----------------|
| **CONTEXT** | Conversation history + UI state | ✅ ADR-012 (RAG pattern) |
| **DATABASE** | iDempiere ERP data | ✅ ERPTools @Tool methods (ADR-002) |
| **MCP TOOLS** | External services | 🔜 This ADR (REST API + @Tool) |

**MCP TOOLS Domain Scope (from DATA_SOURCE_ARCHITECTURE.md):**
- **Information Retrieval:** Web search, documentation lookup
- **External Data:** Weather, stock prices, exchange rates
- **File System:** Read, write, list operations
- **Integration:** Email, calendar, CRM connections
- **Action Tools:** Workflows, notifications, automations

**Implementation Approach:**
1. Expose iDempiere AI capabilities via **REST API** (JAX-RS)
2. External tools can wrap REST calls in **@Tool methods** (ADR-002 pattern)
3. Standard HTTP interface - no custom MCP protocol required
4. Same security model as DATABASE domain (role-based access)

**Domain Boundary:** MCP TOOLS domain handles all external service integrations that require data or actions outside iDempiere.

See: `docs/DATA_SOURCE_ARCHITECTURE.md` for complete domain model and boundary definitions.

---

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

**DECISION (2025-12-01):** Keep **idempiere-mcp-server** as standalone MCP server with **cloudempiere-cli** as backend. Both projects already exist and work - just configuration needed.

REST API available as fallback for non-MCP clients.

### Architecture (CURRENT - WORKING)

```
┌─────────────────────────────────────────────────────────────────┐
│  External AI Agents (Claude Code, Cursor, n8n, etc.)            │
└────────────────────────┬────────────────────────────────────────┘
                         │ MCP Protocol (stdio)
┌────────────────────────▼────────────────────────────────────────┐
│  idempiere-mcp-server (Java 21, MCP SDK 0.16.0) ✅ BUILT        │
│  Location: ~/github/idempiere-mcp-server                        │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ MCP TOOLS (5 existing):                                    │ │
│  │  - listTables, describeTable                               │ │
│  │  - addTable, addColumn, syncTable                          │ │
│  │                                                            │ │
│  │ AI TOOLS (to add):                                         │ │
│  │  - chat, query, analyze                                    │ │
│  └────────────────────────────────────────────────────────────┘ │
│  Configuration:                                                 │
│    IDEMPIERE_BACKEND=cli                                        │
│    IDEMPIERE_CLI_PATH=../cloudempiere-cli/target/...-runner.jar │
└────────────────────────┬────────────────────────────────────────┘
                         │ Subprocess call (java -jar)
┌────────────────────────▼────────────────────────────────────────┐
│  cloudempiere-cli (Quarkus 3.16) ✅ v1.26.0                     │
│  Repository: ~/github/cloudempiere-cli (develop branch)         │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ registry commands                                          │ │
│  │  - tables, columns, windows, export                        │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ AI Service                                                 │ │
│  │  - ClaudeProvider (Anthropic SDK)                          │ │
│  │  - AIService facade                                        │ │
│  ├────────────────────────────────────────────────────────────┤ │
│  │ iDempiere Core JARs                                        │ │
│  │  - org.adempiere.base (AD_* access)                        │ │
│  │  - org.adempiere.pipo (2pack import/export)                │ │
│  └────────────────────────────────────────────────────────────┘ │
└────────────────────────┬────────────────────────────────────────┘
                         │ Direct JDBC
         ┌───────────────▼───────────────┐
         │  iDempiere Database (PostgreSQL)│
         │  - Application Dictionary       │
         │  - Business Data                │
         └───────────────────────────────┘

         ┌─────────────────────────────────┐
         │  com.cloudempiere.ai (OSGi)     │
         │  Repository: ~/github/com.cloudempiere.ai
         │  ┌────────────────────────────┐ │
         │  │ LangChain4j Agents         │ │
         │  │  - ERPTools (@Tool)        │ │
         │  │  - Domain Agents           │ │
         │  ├────────────────────────────┤ │
         │  │ Secure Query Executor      │ │
         │  │  - Role-based access       │ │
         │  │  - Audit logging           │ │
         │  └────────────────────────────┘ │
         │  (Inside iDempiere Runtime)     │
         └─────────────────────────────────┘
```

### Why Separate Projects (Not Merged)

| Concern | Resolution |
|---------|------------|
| Code duplication? | No - MCP server delegates to CLI |
| Config complexity? | Just 2 env vars in MCP server |
| Multiple deployments? | Both already built, just run |
| Performance overhead? | Subprocess call is fast (single JVM option later) |

**Key insight:** idempiere-mcp-server is just a thin MCP wrapper - all business logic is in cloudempiere-cli. No need to merge.

### Project Ecosystem

| Project | Tech Stack | Purpose | Status |
|---------|------------|---------|--------|
| **idempiere-mcp-server** | Java 21, MCP SDK 0.16.0 | MCP protocol layer | ✅ BUILT |
| **cloudempiere-cli** | Quarkus 3.16, Java 21 | Business logic + AI | ✅ v1.26.0 |
| **com.cloudempiere.ai** | Java 11, LangChain4j, OSGi | ERP AI agents | 🔜 This project |

### Configuration

**idempiere-mcp-server/.env:**
```bash
IDEMPIERE_BACKEND=cli
IDEMPIERE_CLI_PATH=../cloudempiere-cli/target/idempiere-cli-1.26.0-SNAPSHOT-runner.jar
```

**Claude Desktop config (~/.config/claude/claude_desktop_config.json):**
```json
{
  "mcpServers": {
    "idempiere": {
      "command": "java",
      "args": ["-jar", "/path/to/idempiere-mcp-server-1.0.0-SNAPSHOT.jar"]
    }
  }
}
```

### Implementation Options

#### Option A: Enhance Existing Setup (PRIMARY - CHOSEN)

**How it works:**
- Keep idempiere-mcp-server as MCP layer
- Add AI tools by extending TableTools.java or creating AITools.java
- AI tools call cloudempiere-cli's AI commands

**Pros:**
- ✅ **Already working** - just add more tools
- ✅ **Zero migration** - extend existing code
- ✅ **Separation of concerns** - MCP vs business logic
- ✅ **Easy testing** - test CLI separately from MCP

**Effort:** 1 week (add AI tools)

#### Option B: Quarkus MCP Extension (FUTURE)

**How it works:**
- Add `quarkus-langchain4j-mcp` extension to cloudempiere-cli
- Merge MCP server into CLI (single process)

**Pros:**
- ✅ Single process (no subprocess overhead)
- ✅ Direct method calls

**Cons:**
- ❌ More complex build
- ❌ Migration effort

**Effort:** 2-3 weeks

**Decision:** Use **Option A** - enhance existing setup. Consider Option B later if performance is an issue.

### LangChain4j MCP Integration

Our LangChain4j architecture (ADR-002) fully supports MCP:

**As MCP Server (via idempiere-mcp-server):**
- Extend existing MCP server with AI-specific tools
- Support STDIO and SSE transports
- Enable automatic tool discovery for external agents

**As MCP Client (via LangChain4j):**
- LangChain4j `langchain4j-mcp` module connects to external MCP servers
- Our agents can consume external tools (GitHub, filesystem, etc.)
- Supports STDIO and SSE transports

**Reuse Opportunities:**

| Component | From Project | Reuse In | Status |
|-----------|-------------|----------|--------|
| MCP Server infrastructure | idempiere-mcp-server | - | ✅ Ready |
| App Dictionary tools | idempiere-mcp-server | - | ✅ Ready |
| AIService, ClaudeProvider | cloudempiere-cli | MCP AI tools | 🔜 Extract |
| registry commands | cloudempiere-cli | MCP server backend | ✅ Ready |
| LangChain4j agents | com.cloudempiere.ai | Future MCP backend | 🔜 Phase 4 |

**Phase 4 Roadmap (from idempiere-mcp-server):**
- [ ] LangChain4j vector storage
- [ ] Semantic caching
- [ ] n8n integration

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

### Alternative 1: Build New MCP Server from Scratch (Rejected)

**Approach:** Create new MCP server in com.cloudempiere.ai

**Why rejected:**
- idempiere-mcp-server already exists and works
- Would duplicate effort
- MCP SDK already integrated in existing project

### Alternative 2: Node.js MCP Server (Rejected)

**Approach:** External Node.js service

**Why rejected:**
- Additional technology stack (not Java)
- Separate deployment/maintenance
- Network overhead (2 hops)
- Java MCP server already exists

### Alternative 3: Embed MCP in iDempiere OSGi (Rejected)

**Approach:** Add MCP server directly to com.cloudempiere.ai OSGi plugin

**Why rejected:**
- OSGi classloader complexity with MCP SDK
- iDempiere uses Java 11, MCP SDK prefers Java 21
- Separate MCP server is more flexible

### Alternative 4: GraphQL (Rejected)

**Why rejected:** Overkill for simple operations, MCP provides better tool discovery

### Alternative 5: REST API Only (Deferred to Fallback)

**Approach:** Only REST endpoints, no MCP

**Why deferred:**
- MCP provides better tool discovery
- MCP is industry standard for AI agents
- REST remains as fallback option

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

### Related Projects (Cloudempiere Ecosystem)
- **idempiere-mcp-server** - `~/github/idempiere-mcp-server` - MCP server for Claude Code
- **cloudempiere-cli** - `~/github/cloudempiere-cli` - Quarkus CLI with App Dictionary access
- **com.cloudempiere.ai** - This project - LangChain4j ERP agents

### REST API
- [JAX-RS Specification](https://jakarta.ee/specifications/restful-ws/)
- [Jersey Framework](https://eclipse-ee4j.github.io/jersey/)
- [iDempiere REST API](https://wiki.idempiere.org/en/REST_Web_Services)

### MCP (Model Context Protocol)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Official Java MCP SDK](https://github.com/modelcontextprotocol/java-sdk)
- [LangChain4j MCP Tutorial](https://docs.langchain4j.dev/tutorials/mcp/)
- [Quarkus MCP Extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/mcp.html)
- [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)

### Related ADRs
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

*ADR-003 | Version 5.0 | 2025-12-01*
*Status: **Proposed** (Target: v0.11.0, Q1 2026)*
*Decision: **REVISED** - Enhance idempiere-mcp-server (PRIMARY) + Quarkus MCP (FUTURE) + REST API (FALLBACK)*
*Validated: 2025-12-01 - See [ADR-003_MCP_VALIDATION_REPORT.md](../ADR-003_MCP_VALIDATION_REPORT.md)*
