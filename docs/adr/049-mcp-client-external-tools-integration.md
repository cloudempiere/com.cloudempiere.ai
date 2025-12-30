# ADR-049: MCP Client Integration for External Tool Access

**Status:** Proposed (Ready for iDempiere v11)
**Date:** 2025-12-21
**Deciders:** Cloudempiere AI Team
**Depends On:** ADR-002, ADR-003, ADR-035
**Target:** iDempiere Release-11 (Java 17)

---

## TL;DR - iDempiere v11 Enables MCP

| iDempiere Version | Java | LangChain4j | MCP Support |
|-------------------|------|-------------|-------------|
| **v10 (Current)** | 11 | 0.35.0 | No |
| **v11 (Target)** | **17** | **1.9.1** | **Yes** |

**When you migrate to iDempiere v11, you get:**
- LangChain4j 1.x with full MCP client support
- Access to 1000+ external MCP servers
- Extended thinking, caching, and advanced features

---

## Context

### Background Research

This ADR is based on research conducted on Block's Goose AI agent framework and the Model Context Protocol (MCP) ecosystem. See: `docs/research/goose-vs-langchain4j-research.md`

**Key Findings:**

| Component | What It Is | Java Requirement |
|-----------|-----------|------------------|
| [Block's Goose](https://github.com/block/goose) | CLI/Desktop AI agent | N/A (Rust-based) |
| [MCP Protocol](https://modelcontextprotocol.io/) | Standard for AI-tool communication | N/A (Protocol) |
| [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) | Official Java implementation | **Java 17+** |
| [LangChain4j MCP](https://docs.langchain4j.dev/tutorials/mcp/) | MCP client for LangChain4j | **LangChain4j 0.36.0+ (Java 17)** |

### Problem Statement

The iDempiere AI plugin currently operates in isolation - it can only access:
1. **iDempiere database** (via SecureDatabaseQueryExecutor)
2. **Conversation context** (via RAG)
3. **Built-in LangChain4j tools** (via @Tool methods)

**Missing Capability:** Access to the 1000+ external MCP servers providing tools for:
- GitHub integration (issues, PRs, code search)
- Slack/Discord messaging
- Google Drive / filesystem access
- Web search and browsing
- Database connectors (PostgreSQL, MySQL, MongoDB)
- Cloud services (AWS, GCP, Azure)
- Development tools (Docker, Kubernetes)
- And many more...

### Current State (ADR-003)

ADR-003 defines iDempiere as an **MCP Server** (exposing tools TO external agents):

```
External AI Agents → MCP Protocol → idempiere-mcp-server → iDempiere
```

This ADR defines iDempiere as an **MCP Client** (consuming tools FROM external servers):

```
iDempiere AI Agents → MCP Client → External MCP Servers → (GitHub, Slack, etc.)
```

### Constraint: Java 17 Requirement

Per ADR-035, we are currently constrained to:

| Component | Current | Constraint |
|-----------|---------|------------|
| Java Runtime | Amazon Corretto 11 | iDempiere v10 requirement |
| LangChain4j | 0.35.0 | Last Java 11 compatible version |
| MCP Support | Not available | Requires LangChain4j 0.36.0+ / Java 17 |

**This ADR is BLOCKED until Java 17 migration (iDempiere Release-11).**

---

## Decision

**Adopt MCP Client capability via LangChain4j** to enable iDempiere AI agents to consume external MCP tools, implemented **after Java 17 migration**.

### Target Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         iDempiere AI Plugin (v2.0+)                         │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    IDempiereAIService (Facade)                        │  │
│  │         - Context injection, Security, Audit logging                  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                      │                                       │
│                                      ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                      LangChain4j AiServices                           │  │
│  │                                                                       │  │
│  │   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐   │  │
│  │   │  ERPTools       │  │  MCP Tool       │  │  Built-in Tools     │   │  │
│  │   │  (@Tool)        │  │  Provider       │  │  (Web Search, etc)  │   │  │
│  │   │                 │  │  (MCP Client)   │  │                     │   │  │
│  │   └────────┬────────┘  └────────┬────────┘  └─────────────────────┘   │  │
│  │            │                    │                                      │  │
│  └────────────┼────────────────────┼──────────────────────────────────────┘  │
│               │                    │                                         │
│               ▼                    ▼                                         │
│  ┌─────────────────────┐  ┌───────────────────────────────────────────────┐ │
│  │  SecureDatabase     │  │           MCP Transport Layer                 │ │
│  │  QueryExecutor      │  │  ┌─────────┐ ┌─────────┐ ┌─────────────────┐  │ │
│  │                     │  │  │  STDIO  │ │   SSE   │ │ Streamable HTTP │  │ │
│  │                     │  │  └────┬────┘ └────┬────┘ └────────┬────────┘  │ │
│  └──────────┬──────────┘  └───────┼───────────┼───────────────┼───────────┘ │
│             │                     │           │               │             │
└─────────────┼─────────────────────┼───────────┼───────────────┼─────────────┘
              │                     │           │               │
              ▼                     ▼           ▼               ▼
     ┌────────────────┐   ┌─────────────────────────────────────────────────┐
     │   iDempiere    │   │              External MCP Servers               │
     │   Database     │   │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌───────┐  │
     │   (PostgreSQL) │   │  │ GitHub  │ │  Slack  │ │ Google  │ │ More  │  │
     │                │   │  │ MCP     │ │  MCP    │ │ Drive   │ │ 1000+ │  │
     └────────────────┘   │  └─────────┘ └─────────┘ └─────────┘ └───────┘  │
                          └─────────────────────────────────────────────────┘
```

### Design Principles

1. **Security First**: All external tool access governed by iDempiere role-based permissions
2. **Audit Trail**: Every external tool invocation logged with user, role, tool, parameters
3. **Configurable**: MCP servers managed via Application Dictionary (new tables)
4. **Graceful Degradation**: External tool failures don't break core ERP functionality
5. **Tenant Isolation**: Each AD_Client can have different MCP server configurations

---

## Implementation Plan

### Phase 1: Infrastructure (Post-Java 17)

**Prerequisites:**
- iDempiere upgraded to Release-11 (Java 17)
- LangChain4j upgraded to 1.x (MCP support)

**Components:**

#### 1.1 New Application Dictionary Tables

```sql
-- MCP Server Configuration
CREATE TABLE AIG_MCPServer (
    AIG_MCPServer_ID     NUMERIC(10) NOT NULL,
    AD_Client_ID         NUMERIC(10) NOT NULL,
    AD_Org_ID            NUMERIC(10) NOT NULL,
    Name                 VARCHAR(60) NOT NULL,
    Description          VARCHAR(255),
    ServerType           VARCHAR(20) NOT NULL,  -- 'STDIO', 'SSE', 'HTTP'
    ConnectionConfig     TEXT NOT NULL,         -- JSON config
    IsActive             CHAR(1) DEFAULT 'Y',
    IsDefault            CHAR(1) DEFAULT 'N',
    SecurityLevel        VARCHAR(20),           -- 'PUBLIC', 'INTERNAL', 'RESTRICTED'
    Created              TIMESTAMP NOT NULL,
    CreatedBy            NUMERIC(10) NOT NULL,
    Updated              TIMESTAMP NOT NULL,
    UpdatedBy            NUMERIC(10) NOT NULL,
    PRIMARY KEY (AIG_MCPServer_ID)
);

-- MCP Server Role Access
CREATE TABLE AIG_MCPServer_Role (
    AIG_MCPServer_Role_ID  NUMERIC(10) NOT NULL,
    AIG_MCPServer_ID       NUMERIC(10) NOT NULL,
    AD_Role_ID             NUMERIC(10) NOT NULL,
    IsReadOnly             CHAR(1) DEFAULT 'N',
    IsActive               CHAR(1) DEFAULT 'Y',
    PRIMARY KEY (AIG_MCPServer_Role_ID),
    FOREIGN KEY (AIG_MCPServer_ID) REFERENCES AIG_MCPServer(AIG_MCPServer_ID),
    FOREIGN KEY (AD_Role_ID) REFERENCES AD_Role(AD_Role_ID)
);

-- MCP Tool Audit Log
CREATE TABLE AIG_MCPToolAudit (
    AIG_MCPToolAudit_ID   NUMERIC(10) NOT NULL,
    AD_Client_ID          NUMERIC(10) NOT NULL,
    AD_Org_ID             NUMERIC(10) NOT NULL,
    AD_User_ID            NUMERIC(10) NOT NULL,
    AD_Role_ID            NUMERIC(10) NOT NULL,
    AIG_MCPServer_ID      NUMERIC(10) NOT NULL,
    ToolName              VARCHAR(120) NOT NULL,
    ToolParameters        TEXT,                  -- JSON parameters
    ToolResult            TEXT,                  -- JSON result (truncated)
    ExecutionTimeMS       NUMERIC(10),
    IsSuccess             CHAR(1) DEFAULT 'Y',
    ErrorMessage          VARCHAR(2000),
    Created               TIMESTAMP NOT NULL,
    PRIMARY KEY (AIG_MCPToolAudit_ID)
);
```

#### 1.2 Connection Configuration Examples

**STDIO Transport (Local subprocess):**
```json
{
  "transport": "stdio",
  "command": ["npx", "-y", "@modelcontextprotocol/server-github"],
  "env": {
    "GITHUB_PERSONAL_ACCESS_TOKEN": "${env:GITHUB_TOKEN}"
  }
}
```

**SSE Transport (Remote HTTP):**
```json
{
  "transport": "sse",
  "url": "https://mcp.example.com/github/sse",
  "headers": {
    "Authorization": "Bearer ${secret:github_token}"
  }
}
```

**Streamable HTTP Transport:**
```json
{
  "transport": "http",
  "url": "https://mcp.example.com/slack",
  "timeout": 30000
}
```

### Phase 2: LangChain4j MCP Client Integration

#### 2.1 MCP Tool Provider Factory

```java
package com.cloudempiere.ai.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.*;

/**
 * Factory for creating MCP tool providers from AIG_MCPServer configuration.
 * Respects iDempiere role-based access control.
 */
public class MCPToolProviderFactory {

    /**
     * Create MCP tool providers for the current user's accessible servers.
     */
    public List<ToolProvider> createToolProviders(Properties ctx) {
        int adClientId = Env.getAD_Client_ID(ctx);
        int adRoleId = Env.getAD_Role_ID(ctx);

        List<ToolProvider> providers = new ArrayList<>();

        // Query accessible MCP servers for this role
        List<MAIGMCPServer> servers = MAIGMCPServer
            .getAccessibleServers(adClientId, adRoleId);

        for (MAIGMCPServer server : servers) {
            try {
                McpTransport transport = createTransport(server);
                McpClient client = McpClient.builder()
                    .transport(transport)
                    .build();

                McpToolProvider toolProvider = McpToolProvider.builder()
                    .mcpClient(client)
                    .build();

                // Wrap with audit logging
                providers.add(new AuditingToolProvider(
                    toolProvider,
                    server,
                    ctx
                ));

            } catch (Exception e) {
                log.warning("Failed to connect to MCP server: "
                    + server.getName() + " - " + e.getMessage());
            }
        }

        return providers;
    }

    private McpTransport createTransport(MAIGMCPServer server) {
        JSONObject config = new JSONObject(server.getConnectionConfig());
        String transportType = config.getString("transport");

        switch (transportType) {
            case "stdio":
                return createStdioTransport(config);
            case "sse":
                return createSseTransport(config);
            case "http":
                return createHttpTransport(config);
            default:
                throw new IllegalArgumentException(
                    "Unknown transport type: " + transportType);
        }
    }

    private McpTransport createStdioTransport(JSONObject config) {
        List<String> command = config.getJSONArray("command")
            .toList().stream()
            .map(Object::toString)
            .collect(Collectors.toList());

        Map<String, String> env = resolveEnvironmentVariables(
            config.optJSONObject("env"));

        return new StdioMcpTransport.Builder()
            .command(command)
            .environment(env)
            .build();
    }

    private McpTransport createSseTransport(JSONObject config) {
        String url = config.getString("url");
        Map<String, String> headers = resolveSecrets(
            config.optJSONObject("headers"));

        return new HttpMcpTransport.Builder()
            .sseUrl(url)
            .customHeaders(headers)
            .build();
    }
}
```

#### 2.2 Auditing Tool Provider Wrapper

```java
package com.cloudempiere.ai.mcp;

import dev.langchain4j.agent.tool.*;

/**
 * Wrapper that adds audit logging to all MCP tool invocations.
 */
public class AuditingToolProvider implements ToolProvider {

    private final ToolProvider delegate;
    private final MAIGMCPServer server;
    private final Properties ctx;

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        // Delegate to actual MCP provider
        ToolProviderResult result = delegate.provideTools(request);

        // Wrap each tool with auditing
        List<ToolSpecification> auditedTools = result.tools().stream()
            .map(tool -> wrapWithAudit(tool))
            .collect(Collectors.toList());

        return ToolProviderResult.builder()
            .tools(auditedTools)
            .build();
    }

    private void auditToolExecution(
            String toolName,
            String parameters,
            String result,
            long executionTimeMs,
            boolean success,
            String errorMessage) {

        MAIGMCPToolAudit audit = new MAIGMCPToolAudit(ctx);
        audit.setAD_Client_ID(Env.getAD_Client_ID(ctx));
        audit.setAD_Org_ID(Env.getAD_Org_ID(ctx));
        audit.setAD_User_ID(Env.getAD_User_ID(ctx));
        audit.setAD_Role_ID(Env.getAD_Role_ID(ctx));
        audit.setAIG_MCPServer_ID(server.getAIG_MCPServer_ID());
        audit.setToolName(toolName);
        audit.setToolParameters(truncate(parameters, 4000));
        audit.setToolResult(truncate(result, 4000));
        audit.setExecutionTimeMS(executionTimeMs);
        audit.setIsSuccess(success);
        audit.setErrorMessage(errorMessage);
        audit.saveEx();
    }
}
```

#### 2.3 Integration with AiServices

```java
package com.cloudempiere.ai.service;

import dev.langchain4j.service.AiServices;

/**
 * Enhanced IDempiereAIService with MCP tool support.
 */
public class IDempiereAIService {

    private final MCPToolProviderFactory mcpFactory;

    public <T> T createAgent(Class<T> agentInterface, Properties ctx) {
        ChatLanguageModel model = getModel(ctx);
        ChatMemory memory = getMemory(ctx);

        AiServices.AiServicesBuilder<T> builder = AiServices.builder(agentInterface)
            .chatLanguageModel(model)
            .chatMemory(memory);

        // Add iDempiere ERP tools
        builder.tools(new ERPTools(ctx));

        // Add MCP external tools (role-filtered)
        List<ToolProvider> mcpProviders = mcpFactory.createToolProviders(ctx);
        for (ToolProvider provider : mcpProviders) {
            builder.toolProvider(provider);
        }

        return builder.build();
    }
}
```

### Phase 3: Use Cases

#### 3.1 GitHub Integration

**MCP Server:** `@modelcontextprotocol/server-github`

**Use Cases:**
- Create issues from iDempiere support tickets
- Link commits to business partner records
- Search code for customization references

**Example Interaction:**
```
User: "Create a GitHub issue for BP #1001's enhancement request"

Agent: I'll create a GitHub issue based on the enhancement request.
       [Uses MCP github_create_issue tool]
       Created issue #456 in cloudempiere/idempiere-customizations:
       "Enhancement: Add batch invoice generation for BP Acme Corp"
```

#### 3.2 Slack Integration

**MCP Server:** `@modelcontextprotocol/server-slack`

**Use Cases:**
- Send order confirmations to customer channels
- Alert sales team about high-value opportunities
- Notify warehouse about urgent shipments

**Example Interaction:**
```
User: "Notify #sales about the $50k order from Acme Corp"

Agent: I'll send a notification to the sales channel.
       [Uses MCP slack_send_message tool]
       Message sent to #sales:
       "New $50,000 order from Acme Corp (SO-2024-1234)"
```

#### 3.3 Google Drive Integration

**MCP Server:** `@modelcontextprotocol/server-gdrive`

**Use Cases:**
- Attach Google Drive documents to business partners
- Export reports to shared drives
- Import product catalogs from spreadsheets

#### 3.4 Web Search Integration

**MCP Server:** `@anthropic/server-brave-search` or `@anthropic/server-exa`

**Use Cases:**
- Research competitor pricing
- Find supplier contact information
- Look up product specifications

---

## Security Considerations

### Role-Based Access Control

```
┌────────────────────────────────────────────────────────────────────┐
│                    MCP Access Control Flow                         │
│                                                                    │
│  1. User initiates AI chat                                         │
│  2. IDempiereAIService checks AD_Role_ID                          │
│  3. Query AIG_MCPServer_Role for accessible servers               │
│  4. Create tool providers ONLY for permitted servers              │
│  5. Each tool invocation logged to AIG_MCPToolAudit               │
│  6. IsReadOnly flag limits destructive operations                 │
└────────────────────────────────────────────────────────────────────┘
```

### Secret Management

| Pattern | Example | Resolution |
|---------|---------|------------|
| `${env:VAR}` | `${env:GITHUB_TOKEN}` | System environment variable |
| `${secret:key}` | `${secret:slack_webhook}` | iDempiere Secure Value |
| `${ctx:field}` | `${ctx:AD_User_ID}` | Current context property |

**Secrets stored in existing `AD_SecureValue` table** - never in connection config plaintext.

### Audit Requirements

Every external tool invocation MUST log:
1. **Who**: AD_User_ID, AD_Role_ID
2. **What**: Tool name, parameters
3. **When**: Timestamp
4. **Where**: MCP server, AD_Client_ID
5. **Result**: Success/failure, execution time

### Network Security

- STDIO transports run in sandboxed subprocesses
- HTTP transports require HTTPS (enforced)
- Timeouts prevent runaway external calls
- Rate limiting per server per user

---

## Dependencies and Prerequisites

### Hard Dependencies (Unblocked by iDempiere v11)

| Dependency | v10 (Current) | v11 (Target) | Status |
|------------|---------------|--------------|--------|
| Java Version | 11 | **17** | Ready with v11 |
| LangChain4j | 0.35.0 | **1.9.1** | Ready with v11 |
| iDempiere | v10 | **v11** | Migration required |
| MCP Java SDK | N/A | **0.12.1** | Available |

**Bottom Line:** Upgrade to iDempiere v11 → Everything works.

### iDempiere v11 Migration Checklist

```bash
# Step 1: Update iDempiere to Release-11
cd ../iDempiereCLDE
git fetch origin
git checkout release-11
mvn clean install -DskipTests

# Step 2: Update Java environment
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home

# Step 3: Update pom.xml dependencies
# Change LangChain4j from 0.35.0 to 1.9.1
# Add MCP module
```

**pom.xml changes for v11:**
```xml
<properties>
    <langchain4j.version>1.9.1</langchain4j.version>
</properties>

<dependencies>
    <!-- LangChain4j MCP Client (NEW) -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-mcp</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
</dependencies>
```

### Soft Dependencies (Non-Blocking)

| Component | Purpose | Status |
|-----------|---------|--------|
| MCP Java SDK 0.12+ | Alternative to LangChain4j MCP | Optional |
| External MCP servers | GitHub, Slack, etc. | Available |
| Network access | For HTTP/SSE transports | Infrastructure |

---

## Alternatives Considered

### Alternative 1: Direct REST Integration (Rejected)

**Approach:** Build custom REST clients for each external service.

**Pros:**
- No MCP dependency
- Works with Java 11

**Cons:**
- Massive development effort per integration
- No standardization
- No tool discovery
- Duplicates work MCP already solves

### Alternative 2: Block's Goose as Orchestrator (Rejected)

**Approach:** Deploy Goose as external agent, call via REST.

**Pros:**
- Immediate access to 1000+ MCP servers
- No Java version constraint

**Cons:**
- Separate deployment (Rust-based)
- Additional infrastructure
- Network hop latency
- Not embedded in iDempiere

### Alternative 3: Spring AI MCP (Considered for Future)

**Approach:** Use Spring AI's MCP implementation instead of LangChain4j.

**Pros:**
- Mature Spring ecosystem integration
- Strong enterprise support

**Cons:**
- Would require major architecture change
- LangChain4j already adopted (ADR-002)
- May revisit if LangChain4j MCP proves insufficient

### Alternative 4: Custom MCP Client (Rejected)

**Approach:** Implement MCP protocol from scratch.

**Pros:**
- Full control
- Could target Java 11

**Cons:**
- Massive development effort
- Protocol maintenance burden
- Why reinvent the wheel?

---

## Migration Path

### Current State → Target State

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Phase 0: Current (v10, Java 11)                                             │
│ ─────────────────────────────────                                           │
│ • LangChain4j 0.35.0 (last Java 11 compatible)                              │
│ • Internal tools only (ERPTools, SecureQueryExecutor)                       │
│ • No MCP support                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ Phase 1: iDempiere v11 Migration (2-3 weeks)                                │
│ ─────────────────────────────────────────────                               │
│ • Upgrade iDempiere to Release-11                                           │
│ • Switch to Java 17 (Corretto 17)                                           │
│ • Upgrade LangChain4j to 1.9.1                                              │
│ • Add langchain4j-mcp dependency                                            │
│ • Run existing tests, fix any breaks                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ Phase 2: MCP Infrastructure (1-2 weeks)                                     │
│ ─────────────────────────────────────────                                   │
│ • Create AIG_MCPServer tables (migration scripts)                           │
│ • Implement MCPToolProviderFactory                                          │
│ • Implement AuditingToolProvider wrapper                                    │
│ • Add role-based access control                                             │
│ • Create iDempiere windows for MCP Server management                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ Phase 3: First MCP Integrations (1 week)                                    │
│ ─────────────────────────────────────────                                   │
│ • GitHub MCP Server (issues, PRs, code search)                              │
│ • Web Search MCP Server (Brave, Exa)                                        │
│ • Test end-to-end with real workloads                                       │
│ • Document setup and configuration                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ Phase 4: Expansion (Ongoing)                                                │
│ ─────────────────────────────────                                           │
│ • Slack, Google Drive, filesystem MCP servers                               │
│ • Custom MCP servers for internal tools                                     │
│ • Community MCP server catalog                                              │
│ • Performance optimization                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Estimated Timeline

| Phase | Duration | Prerequisites |
|-------|----------|---------------|
| Phase 1 | 2-3 weeks | Decision to migrate |
| Phase 2 | 1-2 weeks | Phase 1 complete |
| Phase 3 | 1 week | Phase 2 complete |
| Phase 4 | Ongoing | Phase 3 complete |

**Total to first MCP integration: ~4-6 weeks after v11 decision**

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| External tools available | 10+ | Unique MCP servers configured |
| Tool invocations/month | 1000+ | AIG_MCPToolAudit count |
| Success rate | >95% | Successful invocations / total |
| Latency | <5s P95 | Tool execution time |
| Adoption | 50% users | Users invoking external tools |

---

## Consequences

### Positive

- Access to 1000+ MCP tools without custom development
- Standardized tool interface (no per-service integration)
- Enterprise-grade audit trail for all external access
- Role-based access control extends to external tools
- Future-proof architecture (MCP is industry standard)

### Negative

- Blocked until Java 17 migration
- Additional infrastructure for STDIO transports
- Network dependency for HTTP transports
- Learning curve for MCP configuration

### Neutral

- New tables in Application Dictionary
- Additional monitoring requirements
- Documentation needed for each MCP server

---

## Related ADRs

| ADR | Relationship |
|-----|--------------|
| [ADR-002](002-langchain4j-strategic-adoption.md) | LangChain4j as core framework |
| [ADR-003](003-mcp-server-integration.md) | iDempiere as MCP SERVER (opposite direction) |
| [ADR-035](035-java-version-strategy.md) | Java version constraints (BLOCKER) |
| [ADR-007](007-database-security-model.md) | Security model extension |
| [ADR-013](013-observability-cost-tracking.md) | Audit logging requirements |

---

## References

### MCP Protocol & SDKs
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) - Requires Java 17+
- [LangChain4j MCP Tutorial](https://docs.langchain4j.dev/tutorials/mcp/)
- [Spring AI MCP Reference](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)

### Block's Goose (Research Reference)
- [Goose GitHub](https://github.com/block/goose)
- [Goose Documentation](https://block.github.io/goose/)
- [Agentic AI and MCP Ecosystem](https://block.github.io/goose/blog/2025/02/17/agentic-ai-mcp/)

### Available MCP Servers
- [MCP Server Registry](https://github.com/modelcontextprotocol/servers)
- [GitHub MCP Server](https://github.com/modelcontextprotocol/servers/tree/main/src/github)
- [Slack MCP Server](https://github.com/modelcontextprotocol/servers/tree/main/src/slack)
- [Google Drive MCP Server](https://github.com/modelcontextprotocol/servers/tree/main/src/gdrive)

### Implementation Examples
- [Quarkus LangChain4j MCP](https://docs.quarkiverse.io/quarkus-langchain4j/dev/mcp.html)
- [Baeldung MCP Tutorial](https://www.baeldung.com/langchain4j-quarkus-mcp)
- [MCP Client Example](https://glaforge.dev/posts/2025/04/04/mcp-client-and-server-with-java-mcp-sdk-and-langchain4j/)

---

*ADR-049 | Version 1.0 | 2025-12-21*
*Status: **Proposed** - Ready for implementation with iDempiere v11 (Java 17)*
*Research: [docs/research/goose-vs-langchain4j-research.md](../research/goose-vs-langchain4j-research.md)*
