# ADR-003: MCP Server Integration for External AI Agent Access

**Status:** Accepted
**Date:** 2025-12-01
**Context:** Enable external AI agents to interact with iDempiere through standardized protocol

## Context

The com.cloudempiere.ai plugin provides AI capabilities within the iDempiere ZK web UI. However, modern AI workflows increasingly involve external agents (Claude Code, automation tools, third-party services) that need programmatic access to ERP data and AI-powered analysis.

### Problem Statement

How can we expose iDempiere AI capabilities to external systems while:
1. Maintaining security (role-based access control)
2. Preserving auditability (query logging)
3. Supporting composable AI workflows
4. Avoiding tight coupling with specific AI vendors

### Constraints

- Must work with existing iDempiere security model (AD_Role, AccessSqlParser)
- Cannot expose raw database access without validation
- Must support multiple external clients simultaneously
- Should be deployable independently of iDempiere core
- Must log all AI-initiated operations for compliance

## Decision

Implement a **Model Context Protocol (MCP) server** that wraps the existing AI plugin functionality, exposing it through standardized MCP tools.

### Architecture

```
External AI Agent (Claude Code, etc.)
    │
    └─── MCP Client Connection (stdio/SSE)
         │
         ▼
    ┌────────────────────────────────────┐
    │   Node.js MCP Server               │
    │   - 5 MCP tools                    │
    │   - API client for iDempiere       │
    └────────────────────────────────────┘
         │
         └─── HTTP/REST
              │
              ▼
    ┌────────────────────────────────────┐
    │   iDempiere HTTP API Layer (Java)  │
    │   - AIRestController               │
    │   - Authentication/Authorization   │
    └────────────────────────────────────┘
         │
         └─── OSGi Service Calls
              │
              ▼
    ┌────────────────────────────────────┐
    │   Existing AI Plugin               │
    │   - AIConversationService          │
    │   - SecureDatabaseQueryExecutor    │
    │   - Context Providers              │
    └────────────────────────────────────┘
```

### MCP Tools Exposed

| Tool | Purpose | Maps To |
|------|---------|---------|
| `chat_with_context` | AI conversation with ERP context | AIConversationService |
| `query_database` | Secure SQL queries | SecureDatabaseQueryExecutor |
| `extract_context` | Window/chart data extraction | IAIContextProvider |
| `get_provider_info` | AI provider capabilities | AIProviderFactory |
| `get_conversation_history` | Conversation management | MAIChat/MAIChatEntry |

### Why MCP Protocol

1. **Open Standard**: Not locked to any AI vendor
2. **Composability**: Works with other MCP servers (Gmail, Slack, etc.)
3. **Tool Discovery**: Clients auto-discover available capabilities
4. **Type Safety**: Schema validation (Zod) catches errors early
5. **Future-Proof**: New AI models work automatically

## Alternatives Considered

### Option A: Direct REST API Only

**Approach**: Expose REST endpoints without MCP layer.

**Pros:**
- Simpler implementation
- Works with any HTTP client
- Standard technology

**Cons:**
- No tool discovery mechanism
- No composability with other AI tools
- Requires custom client integration

**Rejected because:** Lacks AI-native features that make integration seamless.

### Option B: GraphQL API

**Approach**: Implement GraphQL endpoint for flexible queries.

**Pros:**
- Flexible query language
- Single endpoint
- Strong typing

**Cons:**
- Complex to implement securely
- Overkill for tool-based access
- No AI-specific features

**Rejected because:** MCP is specifically designed for AI agent integration while GraphQL solves a different problem.

### Option C: gRPC Interface

**Approach**: Binary protocol for high-performance access.

**Pros:**
- High performance
- Strong typing
- Bi-directional streaming

**Cons:**
- No browser support
- Complex setup
- Not AI-focused

**Rejected because:** MCP provides adequate performance with better AI agent compatibility.

### Option D: Embedded OSGi Only

**Approach**: MCP server runs inside iDempiere JVM.

**Pros:**
- Direct access to all services
- No network overhead
- Single deployment

**Cons:**
- Complex deployment
- Tight coupling
- Scaling limitations

**Rejected because:** HTTP separation provides better operational flexibility.

## Implementation

### Phase 1: HTTP API Layer (Java)

1. Create `AIRestController` with 5 REST endpoints
2. Implement authentication (API key + iDempiere session)
3. Add input validation and error handling
4. Register as OSGi HTTP service
5. Test with curl/Postman

### Phase 2: MCP Server (Node.js)

1. Initialize TypeScript project with @modelcontextprotocol/sdk
2. Implement 5 MCP tools with Zod schemas
3. Create API client for iDempiere REST endpoints
4. Add retry logic and error handling
5. Implement structured logging

### Phase 3: Security & Optimization

1. Security audit (OWASP top 10)
2. Performance optimization (caching)
3. Rate limiting
4. Comprehensive testing

### Phase 4: Deployment

1. Docker containerization
2. Production configuration
3. Monitoring setup
4. Documentation

## Consequences

### Positive

- **AI Agent Access**: Claude Code and other agents can interact with iDempiere
- **Composable Workflows**: Chain iDempiere tools with other MCP servers
- **Headless ERP**: Enables API-first integrations
- **Security Preserved**: All access goes through existing security layer
- **Audit Trail**: Every operation logged
- **Vendor Independence**: Not locked to specific AI provider

### Negative

- **Additional Complexity**: Two-tier architecture (MCP + HTTP API)
- **Deployment Overhead**: Separate Node.js service to maintain
- **Latency**: HTTP call between MCP server and iDempiere
- **New Technology**: Team needs MCP/Node.js expertise

### Neutral

- Documentation must cover both Java API and MCP server
- Testing requires both unit tests and integration tests
- Monitoring needs to cover both tiers

## Success Metrics

| Metric | Target |
|--------|--------|
| Query response time | < 2 seconds |
| AI response time | < 30 seconds |
| Uptime | 99.9% |
| Security incidents | 0 |
| Audit coverage | 100% of operations |

## References

- [Model Context Protocol Specification](https://modelcontextprotocol.io)
- [MCP Server Documentation](../mcpserver/INDEX.md)
- [01-ARCHITECTURE.md](../mcpserver/01-ARCHITECTURE.md)
- [02-IMPLEMENTATION_GUIDE.md](../mcpserver/02-IMPLEMENTATION_GUIDE.md)
- [03-BEST_PRACTICES.md](../mcpserver/03-BEST_PRACTICES.md)
- [04-DEPLOYMENT.md](../mcpserver/04-DEPLOYMENT.md)
- [ADR-001: Initial Architecture](001-initial-architecture.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)

---

*ADR-003 | Version 1.0 | 2025-12-01*
