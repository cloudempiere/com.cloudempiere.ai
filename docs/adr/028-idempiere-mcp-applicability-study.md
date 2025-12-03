# ADR-028: iDempiere-MCP Applicability Study

**Status:** Study/Reference
**Date:** 2025-12-03
**Deciders:** CloudEmpiere AI Team
**Related:** ADR-003 (MCP Server Integration)

---

## Context

This document analyzes [hengsin/idempiere-mcp](https://github.com/hengsin/idempiere-mcp), a proof-of-concept MCP server for iDempiere developed by Heng Sin Low (iDempiere core maintainer). The goal is to identify architectural patterns, features, and implementation approaches that could benefit CloudEmpiere's AI integration.

### CloudEmpiere Current State

We have three related projects:

| Project | Purpose | Status |
|---------|---------|--------|
| **idempiere-mcp-server** | MCP protocol layer (Java 21) | Built, basic App Dictionary tools |
| **cloudempiere-cli** | Quarkus CLI backend | v1.26.0, registry commands |
| **com.cloudempiere.ai** | OSGi plugin with LangChain4j | In development |

### idempiere-mcp Overview

**Repository:** https://github.com/hengsin/idempiere-mcp
**Type:** Proof-of-concept iDempiere OSGi plugin
**Dependencies:** Requires idempiere-rest project
**Transport:** SSE + Streamable HTTP
**Authentication:** Bearer token (iDempiere Rest Auth Tokens)

---

## Architecture Analysis

### 1. Project Structure

```
org.idempiere.mcp.server/
├── src/org/idempiere/mcp/server/
│   ├── Activator.java              # OSGi lifecycle
│   ├── api/
│   │   └── IMcpService.java        # Service interface
│   ├── client/
│   │   └── RestApiClient.java      # HTTP client wrapper
│   ├── config/
│   │   └── McpConfig.java          # Environment configuration
│   ├── core/
│   │   ├── McpServiceImpl.java     # Main MCP handler
│   │   ├── McpToolExecutor.java    # Tool implementations
│   │   └── McpResourceExecutor.java # Resource implementations
│   └── web/
│       └── McpServlet.java         # SSE + HTTP transport
├── META-INF/MANIFEST.MF            # OSGi bundle metadata
└── OSGI-INF/                       # OSGi service declarations
```

### 2. MCP Implementation Details

#### Transport Layer (McpServlet.java)

**Dual Transport Support:**
- **SSE Transport:** `GET /mcp/sse` - Long-lived connections with `POST /message?sessionId=X` for requests
- **Streamable HTTP:** `POST /mcp/streaming` - Single-request/response with session tracking

**Session Management:**
```java
ConcurrentHashMap<String, AsyncContext> sseContexts;  // SSE sessions
ConcurrentHashMap<String, McpSession> streamingSessions; // HTTP sessions
```

**Configuration (Environment Variables):**
- `MCP_PROTOCOL_VERSION`: Default "2025-06-18"
- `MCP_STREAMING_SESSION_TTL_MINUTES`: Session timeout (default 30 min)
- `MCP_CLEANUP_INTERVAL_MINUTES`: Cleanup scheduler (default 10 min)

**Status Endpoint:** `GET /mcp/status` - Active sessions, cleanup stats

#### Service Layer (McpServiceImpl.java)

**JSON-RPC 2.0 Routing:**
```java
String processRequest(String jsonRequest, String authToken) {
    switch (method) {
        case "initialize" -> serverCapabilities();
        case "tools/list" -> listAllTools();
        case "tools/call" -> executeToolFromArgs(args, authToken);
        case "resources/list" -> listResources();
        case "resources/read" -> readResourceByUri(uri);
    }
}
```

**Protocol Compliance:**
- Version: "2025-06-18"
- Capabilities: tools (listChanged: false), resources (subscribe: false, listChanged: false)

#### Tool Categories

**1. Model/Record Operations (CRUD):**
| Tool | Description |
|------|-------------|
| `search_records` | OData-style filtering with `$filter`, `$top`, `$skip` |
| `get_record` | Retrieve by ID (numeric or UUID) |
| `create_record` | POST to `/models/{model}` |
| `update_record` | PUT to `/models/{model}/{id}` |
| `delete_record` | DELETE with optional confirmation bypass |
| `get_property` | Access specific column value |

**2. Schema Discovery:**
| Tool | Description |
|------|-------------|
| `list_models` | Available tables/models |
| `get_model_openapi` | OpenAPI YAML schema per model |

**3. Process Execution:**
| Tool | Description |
|------|-------------|
| `list_processes` | Available processes |
| `get_process` | Process definition with parameters |
| `run_process` | Execute with parameter JSON |

**4. Window-Based Operations:**
| Tool | Description |
|------|-------------|
| `list_windows` | Available windows |
| `get_window` | Window with tabs structure |
| `list_tabs` | Tabs for a window |
| `get_tab` | Tab with fields |
| `window_search`, `window_get`, `window_create`, `window_update`, `window_delete` | Window-scoped CRUD |
| `window_child_*` | Child tab/line item operations |

**5. Server Jobs:**
| Tool | Description |
|------|-------------|
| `list_server_jobs`, `get_server_job` | Job inspection |
| `run_server_job`, `toggle_server_job` | Job control |
| `list_scheduler_jobs`, `run_scheduler_job`, `delete_scheduler_job` | Scheduler management |

**6. Attachments:**
| Tool | Description |
|------|-------------|
| `get_attachments`, `get_attachment_zip` | Download attachments |
| `get_attachment_by_name` | Single file retrieval |
| `add_attachment`, `delete_attachment`, `archive_attachments` | Attachment management |

**7. Printing:**
| Tool | Description |
|------|-------------|
| `print_record` | Generate print output |

#### Resource Layer (McpResourceExecutor.java)

**Available Resources:**
- `idempiere://metadata/models` - List of available data models
- `idempiere://metadata/processes` - List of active processes

### 3. REST API Client Pattern

**RestApiClient.java** wraps Java HttpClient:
```java
public class RestApiClient {
    private final HttpClient httpClient;  // HTTP/1.1, 20s timeout
    private final String baseUrl;

    public JsonObject get(String path, String token);
    public JsonObject post(String path, JsonObject data, String token);
    public JsonObject put(String path, JsonObject data, String token);
    public JsonObject delete(String path, String token);
    public String getYaml(String path, String token);      // OpenAPI schemas
    public byte[] getBinary(String path, String token, String accept);
}
```

### 4. Authentication Model

**Bearer Token Flow:**
1. User creates Rest Auth Token record in iDempiere
2. Auto-generated token used as Bearer token
3. All MCP endpoints require `Authorization: Bearer <token>`
4. Token maps to AD_User_ID for permission enforcement

**Multi-Tenant Support (via Gemini CLI):**
```json
{
  "mcpServers": {
    "tenant1": { "url": "...", "headers": { "Authorization": "Bearer TOKEN1" } },
    "tenant2": { "url": "...", "headers": { "Authorization": "Bearer TOKEN2" }, "excluded": true }
  }
}
```

---

## Comparison with CloudEmpiere Architecture

### Critical Limitation: REST-Only Data Access

**hengsin-mcp uses REST API exclusively for all data operations.** This creates significant limitations:

| Limitation | Impact |
|------------|--------|
| **No custom SQL queries** | Cannot run ad-hoc analytics, aggregations, or complex JOINs |
| **Model-bound data** | Only access data exposed through Java model classes or views |
| **No LangChain4j** | No AI agent framework, RAG, or intelligent query generation |
| **Pre-defined endpoints** | Limited to what idempiere-rest exposes |
| **No query optimization** | Cannot leverage AI to generate efficient queries |

**Example - What hengsin-mcp CAN do:**
```
GET /models/C_Order?$filter=IsSOTrx eq true&$top=10
→ Returns 10 sales orders (pre-defined model fields only)
```

**Example - What hengsin-mcp CANNOT do:**
```sql
-- Complex analytics query
SELECT bp.Name, SUM(ol.LineNetAmt) as TotalSales,
       COUNT(DISTINCT o.C_Order_ID) as OrderCount
FROM C_Order o
JOIN C_OrderLine ol ON o.C_Order_ID = ol.C_Order_ID
JOIN C_BPartner bp ON o.C_BPartner_ID = bp.C_BPartner_ID
WHERE o.DateOrdered >= '2025-01-01'
GROUP BY bp.Name
HAVING SUM(ol.LineNetAmt) > 10000
ORDER BY TotalSales DESC
```

**CloudEmpiere Advantage:**
- `SecureDatabaseQueryExecutor` executes any SQL with role-based security
- LangChain4j agents can generate and optimize queries
- RAG retrieval for context-aware responses
- Not limited to pre-defined REST endpoints

### Architecture Differences

| Aspect | idempiere-mcp | CloudEmpiere |
|--------|--------------|--------------|
| **Runtime** | Inside iDempiere (OSGi) | Separate MCP server + CLI |
| **Java Version** | Java 17 (iDempiere 12+) | Java 21 (MCP server), Java 11 (OSGi plugin) |
| **REST Backend** | idempiere-rest project | cloudempiere-cli (Quarkus) |
| **Data Access** | **REST API only (limited)** | **Direct SQL + REST (flexible)** |
| **Query Capability** | Pre-defined models/views only | Any SQL with role security |
| **Transport** | SSE + Streamable HTTP | STDIO (via MCP SDK) |
| **Session** | Server-managed | Stateless |
| **AI Integration** | None (pure MCP) | LangChain4j agents |
| **RAG/Embeddings** | None | pgvector + ContentRetriever |

### Feature Gap Analysis

| Feature | idempiere-mcp | CloudEmpiere | Gap |
|---------|--------------|--------------|-----|
| **Model CRUD** | ✅ Full OData filtering | ⚠️ Basic via CLI | Need OData support |
| **Window Operations** | ✅ Window-scoped CRUD | ❌ Not implemented | Add window tools |
| **Process Execution** | ✅ With parameters | ⚠️ Limited | Enhance process support |
| **Server Jobs** | ✅ Full control | ❌ Not implemented | Low priority |
| **Attachments** | ✅ Full CRUD + ZIP | ❌ Not implemented | Add attachment tools |
| **Schema Discovery** | ✅ OpenAPI YAML | ✅ Via registry commands | Comparable |
| **Print Output** | ✅ Print record | ❌ Not implemented | Add if needed |
| **SSE Transport** | ✅ Implemented | ❌ STDIO only | Consider for web clients |
| **AI Chat** | ❌ Not implemented | ✅ Core feature | Our differentiator |
| **Secure Queries** | ❌ Via REST only | ✅ SecureDatabaseQueryExecutor | Our strength |
| **Context Providers** | ❌ Not implemented | ✅ Window/Chart context | Our strength |
| **Agent Orchestration** | ❌ Not implemented | ✅ LangChain4j | Our strength |

---

## Key Learnings and Adoption Recommendations

### 1. Adopt: Tool Organization Pattern

**idempiere-mcp Pattern:**
```
tools/
├── Model Operations (search, get, create, update, delete)
├── Schema Discovery (list_models, get_model_openapi)
├── Process Execution (list, get, run)
├── Window Operations (window-scoped CRUD)
└── Server Management (jobs, scheduler)
```

**Recommendation:** Organize our MCP tools into similar categories for discoverability.

### 2. Adopt: OData-Style Filtering

**idempiere-mcp Approach:**
```
$filter=IsActive eq true and contains(tolower(Name),'test')
$top=50
$skip=0
$orderby=Created desc
```

**Recommendation:** Add OData filter support to our search tools for compatibility with existing patterns.

### 3. Adopt: Window-Scoped Operations

**Why Valuable:**
- Windows represent user-facing UI groupings
- Provides familiar navigation for ERP users
- Enables context-aware operations

**Recommendation:** Add `window_*` tools that leverage our WindowContextProvider.

### 4. Consider: SSE Transport

**Current State:** Our MCP server uses STDIO (subprocess)
**idempiere-mcp:** SSE + Streamable HTTP for web clients

**Recommendation:**
- Keep STDIO for CLI/desktop tools (Claude Code, Cursor)
- Add SSE endpoint for web-based integrations (future)
- Use dual-transport pattern from McpServlet.java

### 5. Adopt: Session Management Pattern

**idempiere-mcp Pattern:**
```java
ConcurrentHashMap<String, AsyncContext> sessions;
ScheduledExecutorService cleanupScheduler;
long sessionTtlMs = TimeUnit.MINUTES.toMillis(30);
```

**Recommendation:** Implement session cleanup for SSE transport when added.

### 6. Do Not Adopt: Direct REST Backend

**idempiere-mcp:** MCP server calls idempiere-rest directly
**CloudEmpiere:** MCP server calls cloudempiere-cli

**Reasoning:**
- Our CLI backend provides AI-specific features
- CLI has iDempiere core JARs for Application Dictionary access
- Separation allows independent testing

### 7. Enhance: Process Execution

**idempiere-mcp Features:**
- List processes with parameters
- Execute with JSON parameter payload
- Parameter type handling (string, integer, table)

**Recommendation:** Enhance our process tools to match this capability.

### 8. Add: Attachment Support

**idempiere-mcp Features:**
- List attachments for any record
- Download as ZIP or individual files
- Add/delete attachments
- Archive support

**Recommendation:** Add attachment tools for document-heavy workflows.

---

## Implementation Roadmap

### Phase 1: Tool Parity (2 weeks)

| Task | Priority | Effort |
|------|----------|--------|
| OData filter support in search tools | High | 3 days |
| Window-scoped CRUD tools | High | 3 days |
| Enhanced process execution | Medium | 2 days |
| Attachment management tools | Medium | 2 days |

### Phase 2: Transport Enhancement (1 week)

| Task | Priority | Effort |
|------|----------|--------|
| Add SSE transport option | Low | 3 days |
| Session management | Low | 2 days |
| Status/health endpoint | Low | 1 day |

### Phase 3: Advanced Features (future)

| Task | Priority | Effort |
|------|----------|--------|
| Server job management | Low | 2 days |
| Print record support | Low | 1 day |
| OpenAPI schema generation | Low | 2 days |

---

## Decision Outcome

### What We Will Adopt

1. **Tool Organization** - Categorize tools by domain (Model, Window, Process, etc.)
2. **OData Filtering** - Support `$filter`, `$top`, `$skip` in search operations
3. **Window-Scoped Operations** - Add `window_*` tools
4. **Process Enhancement** - Full parameter support for process execution
5. **Attachment Tools** - CRUD operations for attachments

### What We Will Not Adopt

1. **Direct OSGi Integration** - Keep separate MCP server + CLI architecture
2. **REST-Only Backend** - Maintain CLI backend with AI capabilities
3. **Immediate SSE Support** - STDIO sufficient for current use cases

### What Differentiates CloudEmpiere

1. **Flexible Data Access** - Direct SQL queries, not limited to REST models
2. **AI-Native Design** - LangChain4j agents with domain expertise
3. **Intelligent Query Generation** - AI generates optimized SQL for complex analytics
4. **Secure Query Execution** - Role-based SQL access with audit trail
5. **Context Providers** - Window and Chart context extraction
6. **RAG Integration** - Vector search for documentation and data
7. **Multi-Provider Support** - Anthropic, AWS Bedrock, Ollama

**Key Insight:** hengsin-mcp is a solid MCP tool wrapper around idempiere-rest, but it's fundamentally a CRUD interface. CloudEmpiere is an **AI-powered analytics and automation platform** that can answer complex business questions requiring custom queries, aggregations, and cross-table analysis.

---

## References

### External
- [hengsin/idempiere-mcp](https://github.com/hengsin/idempiere-mcp) - Source repository
- [idempiere-rest](https://github.com/idempiere/idempiere-rest) - REST API backend
- [Model Context Protocol](https://modelcontextprotocol.io/) - MCP specification
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) - Official Java implementation

### Internal
- [ADR-003: MCP Server Integration](003-mcp-server-integration.md) - Our MCP strategy
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md) - Agent framework

---

*ADR-028 | Version 1.0 | 2025-12-03*
*Status: Study/Reference*
