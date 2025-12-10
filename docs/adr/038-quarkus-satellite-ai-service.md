# ADR-038: Quarkus Satellite AI Service Architecture

**Status:** Research/Proposed
**Date:** 2025-12-10
**Deciders:** Cloudempiere AI Team
**Type:** Strategic Architecture Decision

---

## Executive Summary

This ADR explores the architectural option of deploying a **Quarkus-based satellite AI service** that provides CLI, REST, MCP, and other interfaces while bridging to the iDempiere ERP system. The satellite service would:

- Run on **Java 17/21** with **LangChain4j 1.x** (latest features)
- Provide **MCP server, REST API, CLI** interfaces
- Access iDempiere database with **role-based security**
- **Offload and centralize** AI processing outside the OSGi runtime
- Enable **horizontal scaling** of AI workloads

---

## Context and Problem Statement

### Current Constraints

The `com.cloudempiere.ai` OSGi plugin faces fundamental technology constraints:

| Constraint | Current State | Impact |
|------------|---------------|--------|
| **Java Version** | Java 11 (iDempiere v10) | LangChain4j limited to 0.35.0 |
| **LangChain4j** | 0.35.0 | No MCP, limited observability |
| **OSGi Runtime** | Complex classloader | ServiceLoader issues (AWS SDK) |
| **Scalability** | Single JVM | Cannot scale AI independently |
| **Interfaces** | ZK UI only | No CLI, limited REST |

### Opportunity

A **satellite service** architecture could bypass these constraints by running AI workloads in a separate, modern Java runtime while maintaining secure integration with iDempiere.

### Decision Drivers

1. **Unblock LangChain4j 1.x features** (MCP, extended thinking, enhanced observability)
2. **Enable horizontal scaling** of AI workloads
3. **Provide multiple interfaces** (CLI, REST, MCP, gRPC)
4. **Centralize AI for multi-tenant deployments**
5. **Preserve security model** (role-based access, audit trail)
6. **Share flows/chains/tools** across deployments

---

## Research Findings

### Quarkus LangChain4j Extension

The [Quarkus LangChain4j extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html) provides production-ready AI integration:

**Version:** 1.4.x (bundling LangChain4j 1.5.0+)
**Java Requirement:** Java 17+

**Key Features:**
| Feature | Description |
|---------|-------------|
| **Declarative AI Services** | `@RegisterAiService` annotation for clean interfaces |
| **MCP Support** | Client and server modes, STDIO/HTTP/SSE/WebSocket |
| **Tool/Function Calling** | `@Tool` annotations for business logic |
| **RAG Integration** | EmbeddingStore, ContentRetriever |
| **Streaming** | Full streaming response support |
| **Observability** | Metrics, tracing, logging built-in |
| **Dev Services** | Auto-start Ollama, pgvector for development |
| **Native Image** | GraalVM compilation for fast startup |

**Supported LLM Providers:**
- Anthropic Claude (3, 3.5, 3.6)
- AWS Bedrock
- OpenAI / Azure OpenAI
- Google Gemini
- Ollama (local)
- Mistral AI
- IBM watsonx.ai

### MCP Integration Capabilities

The [Quarkus MCP extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/mcp.html) enables:

**As MCP Server:**
```java
@Tool(description = "Query iDempiere orders")
public List<Order> queryOrders(@ToolArg("status") String status) {
    // Business logic
}
```

**Transport Options:**
- STDIO (for CLI integration)
- HTTP/SSE (for remote access)
- Streamable HTTP (for modern clients)
- WebSocket (for bidirectional)

**Security:**
- Bearer token authentication
- OIDC token propagation
- mTLS support
- Quarkus Security integration

### Performance Characteristics

Based on [Quarkus performance benchmarks](https://quarkus.io/performance/):

| Metric | JVM Mode | Native Mode | Notes |
|--------|----------|-------------|-------|
| **Startup Time** | ~2.3s | ~0.08s | 29x faster native |
| **Memory (startup)** | ~113 MB | ~16 MB | 86% reduction |
| **Build Time** | ~15s | ~210s | Native builds slower |
| **Runtime Throughput** | Higher | Lower | JIT optimizations |
| **Image Size** | ~200 MB | ~30 MB | Native binary smaller |

**Recommendation:** Use JVM mode for development, native for serverless/edge deployments.

---

## Proposed Architecture

### High-Level Design

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT LAYER                                       │
├───────────────┬───────────────┬───────────────┬───────────────┬─────────────┤
│   Claude      │   Cursor      │   n8n/        │   Custom      │  iDempiere  │
│   Desktop     │   IDE         │   Zapier      │   Apps        │  ZK UI      │
│   (MCP)       │   (MCP)       │   (REST)      │   (REST/gRPC) │  (Bridge)   │
└───────┬───────┴───────┬───────┴───────┬───────┴───────┬───────┴──────┬──────┘
        │               │               │               │              │
        │  MCP/STDIO    │  MCP/SSE      │   REST/JSON   │   gRPC       │  Bridge
        ▼               ▼               ▼               ▼              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│              QUARKUS SATELLITE AI SERVICE (Java 17/21)                       │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         INTERFACE LAYER                                 │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │ │
│  │  │ MCP      │ │ REST API │ │ CLI      │ │ gRPC     │ │ Bridge       │  │ │
│  │  │ Server   │ │ (JAX-RS) │ │ (Picocli)│ │ (future) │ │ (iDempiere)  │  │ │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘  │ │
│  └───────┴────────────┴────────────┴────────────┴──────────────┴──────────┘ │
│                                    │                                         │
│  ┌─────────────────────────────────▼────────────────────────────────────┐   │
│  │                         AI SERVICE LAYER                              │   │
│  │                                                                       │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐   │   │
│  │  │ ConversationSvc │  │ Agent Registry  │  │ Flow/Chain Registry │   │   │
│  │  │ (Chat Memory)   │  │ (Domain Agents) │  │ (Shareable Flows)   │   │   │
│  │  └────────┬────────┘  └────────┬────────┘  └──────────┬──────────┘   │   │
│  │           │                    │                      │              │   │
│  │  ┌────────▼────────────────────▼──────────────────────▼────────┐     │   │
│  │  │                    LangChain4j 1.x                          │     │   │
│  │  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────┐  │     │   │
│  │  │  │ AiServices │ │ RAG Engine │ │ Tool Exec  │ │ Streaming│  │     │   │
│  │  │  └────────────┘ └────────────┘ └────────────┘ └──────────┘  │     │   │
│  │  └─────────────────────────────────────────────────────────────┘     │   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                    │                                         │
│  ┌─────────────────────────────────▼────────────────────────────────────┐   │
│  │                      SECURITY & CONTEXT LAYER                         │   │
│  │                                                                       │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐   │   │
│  │  │ Context Manager │  │ Role-Based      │  │ Audit Logger        │   │   │
│  │  │ (User/Role/Org) │  │ Access Control  │  │ (Compliance)        │   │   │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────┘   │   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                         DATA ACCESS LAYER                              │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐    │  │
│  │  │ Secure Query    │  │ Vector Store    │  │ External MCP        │    │  │
│  │  │ Executor        │  │ (pgvector)      │  │ Clients             │    │  │
│  │  └────────┬────────┘  └────────┬────────┘  └──────────┬──────────┘    │  │
│  └───────────┴──────────────────┬─┴──────────────────────┴───────────────┘  │
│                                 │                                            │
└─────────────────────────────────┼────────────────────────────────────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
        ▼                         ▼                         ▼
┌───────────────┐       ┌─────────────────┐       ┌─────────────────┐
│  iDempiere    │       │    pgvector     │       │  External MCP   │
│  PostgreSQL   │       │   (embeddings)  │       │  Servers        │
│  (Business    │       │                 │       │  (GitHub, etc.) │
│   Data)       │       │                 │       │                 │
└───────────────┘       └─────────────────┘       └─────────────────┘
```

### Integration Pattern with iDempiere

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         iDempiere Server (Java 11)                        │
│                                                                           │
│  ┌──────────────────────────────────────────────────────────────────────┐│
│  │                    com.cloudempiere.ai (OSGi Plugin)                 ││
│  │                                                                      ││
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────┐ ││
│  │  │  AIChatWidget  │  │  Bridge Client │  │  Fallback Local        │ ││
│  │  │  (ZK UI)       │──▶│  (HTTP/gRPC)   │  │  LangChain4j 0.35.0    │ ││
│  │  └────────────────┘  └───────┬────────┘  └────────────────────────┘ ││
│  │                              │                                       ││
│  └──────────────────────────────┼───────────────────────────────────────┘│
│                                 │                                        │
└─────────────────────────────────┼────────────────────────────────────────┘
                                  │
                                  │  REST/gRPC (context, role, query)
                                  ▼
                    ┌─────────────────────────────┐
                    │   Quarkus Satellite Service  │
                    │   (Full AI Processing)       │
                    └─────────────────────────────┘
```

### Context Passing Model

```java
// Context DTO sent to satellite service
public class iDempiereContext {
    // Security Context
    private int adClientId;
    private int adOrgId;
    private int adUserId;
    private int adRoleId;
    private String sessionId;

    // Window/UI Context
    private int adWindowId;
    private int adTabId;
    private int recordId;
    private Map<String, Object> windowFields;

    // Language/Locale
    private String language;
    private String locale;

    // Conversation Context
    private String conversationId;
    private List<Message> history;

    // Permissions (cached from iDempiere)
    private Set<String> accessibleTables;
    private Set<String> readOnlyTables;
    private boolean canExecuteReports;
}
```

---

## Feature Matrix: OSGi Plugin vs Satellite Service

| Feature | OSGi Plugin (Current) | Satellite Service | Notes |
|---------|----------------------|-------------------|-------|
| **Java Version** | 11 | 17/21 | Satellite unblocked |
| **LangChain4j** | 0.35.0 | 1.x (latest) | Full features |
| **MCP Server** | No | Yes | Native support |
| **MCP Client** | No | Yes | Tool integration |
| **Extended Thinking** | No | Yes | Claude 3.5+ |
| **CLI Interface** | No | Yes | Picocli |
| **REST API** | Limited | Full | JAX-RS/RESTEasy |
| **Streaming** | Partial | Full | All providers |
| **Native Image** | No | Yes | Fast startup |
| **Horizontal Scaling** | No | Yes | K8s, ECS |
| **Multi-tenant** | Limited | Full | Isolated contexts |
| **Tool Caching** | No | Yes | Cost optimization |
| **Observability** | Basic | Full | Metrics, traces |
| **OSGi Complexity** | High | None | Clean classloader |
| **iDempiere Integration** | Direct | Via Bridge | Network hop |
| **ZK UI Access** | Direct | Via Bridge | Context required |

---

## Flow/Chain/Tool Sharing Architecture

### Shareable Components Registry

```java
@ApplicationScoped
public class AIComponentRegistry {

    // Shareable Prompt Templates
    Map<String, PromptTemplate> promptTemplates;

    // Shareable Tools (versioned)
    Map<String, List<ToolSpecification>> tools;

    // Shareable Chains/Flows
    Map<String, ChainDefinition> chains;

    // Shareable Agents
    Map<String, AgentDefinition> agents;

    // Import/Export for sharing across deployments
    public byte[] exportComponent(String type, String name, String version);
    public void importComponent(byte[] componentData);
}
```

### Flow/Chain Definition Model

```java
// Shareable Chain Definition (JSON/YAML serializable)
public class ChainDefinition {
    private String id;
    private String name;
    private String version;
    private String description;

    // Chain steps
    private List<ChainStep> steps;

    // Required tools (by name, resolved at runtime)
    private Set<String> requiredTools;

    // Required prompts
    private Set<String> requiredPrompts;

    // Configuration
    private Map<String, Object> config;

    // Metadata
    private String author;
    private String license;
    private List<String> tags;
}

public class ChainStep {
    private String type; // "llm", "tool", "condition", "parallel"
    private String name;
    private Map<String, Object> parameters;
    private String outputVariable;
    private ChainCondition condition;
}
```

### Distribution Model

```
┌──────────────────────────────────────────────────────────────────┐
│                    SHARED COMPONENT REGISTRY                      │
│                                                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────┐  │
│  │  Prompts    │  │   Tools     │  │   Chains    │  │  Agents │  │
│  │  Library    │  │   Library   │  │   Library   │  │  Library│  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └────┬────┘  │
│         │                │                │              │       │
│         └────────────────┴────────────────┴──────────────┘       │
│                                  │                                │
│                     ┌────────────▼────────────┐                   │
│                     │   Version Control       │                   │
│                     │   (Git-like history)    │                   │
│                     └────────────┬────────────┘                   │
│                                  │                                │
└──────────────────────────────────┼────────────────────────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
        ▼                          ▼                          ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│  Deployment A │         │  Deployment B │         │  Deployment C │
│  (Customer 1) │         │  (Customer 2) │         │  (Dev/Test)   │
│               │         │               │         │               │
│  - Base Chain │         │  - Base Chain │         │  - All Chains │
│  - Custom     │         │  - Forked     │         │  - Testing    │
│    Extension  │         │    Variant    │         │               │
└───────────────┘         └───────────────┘         └───────────────┘
```

---

## Pros and Cons Analysis

### Option 1: Quarkus Satellite Service (Proposed)

**Pros:**
| Benefit | Impact | Details |
|---------|--------|---------|
| **Unblocks Java 17+** | High | Access to LangChain4j 1.x, full MCP |
| **Multiple Interfaces** | High | CLI, REST, MCP, gRPC in one service |
| **Horizontal Scaling** | High | Scale AI independently of ERP |
| **Native Image** | Medium | Fast startup for serverless |
| **Clean Architecture** | High | No OSGi classloader issues |
| **Centralized AI** | High | Single point for all AI operations |
| **Shareable Components** | High | Flows, chains, tools across deployments |
| **Modern Tooling** | Medium | Hot reload, Dev UI, better DX |
| **Multi-tenant Ready** | High | Isolated context per tenant |
| **Cost Optimization** | Medium | Caching, batching at scale |

**Cons:**
| Drawback | Impact | Mitigation |
|----------|--------|------------|
| **Network Latency** | Medium | Local deployment, gRPC, connection pooling |
| **Additional Component** | Medium | Container orchestration (K8s/ECS) |
| **Context Passing** | Medium | Well-defined DTO, caching |
| **Security Surface** | Medium | mTLS, API keys, network isolation |
| **Operational Complexity** | Medium | Infrastructure as Code (Terraform) |
| **Two Codebases** | Low | Clear separation of concerns |
| **Bridge Development** | Medium | One-time effort, reusable |

### Option 2: Stay with OSGi Plugin Only (Current)

**Pros:**
- Single deployment unit
- Direct iDempiere integration
- Simpler operational model
- No network latency

**Cons:**
- **Blocked on Java 11** indefinitely (until iDempiere v11)
- No MCP support
- Limited scalability
- OSGi complexity (ServiceLoader issues)
- No CLI interface

### Option 3: Wait for iDempiere v11 Migration

**Pros:**
- Single technology stack
- Direct integration maintained
- Java 17+ eventually available

**Cons:**
- **Timeline uncertain** (Q2 2026+)
- All features blocked until migration
- Large migration effort for ERP system
- Customers may not migrate quickly

---

## Limitations and Issues

### Known Limitations

| Limitation | Impact | Workaround |
|------------|--------|------------|
| **Network Dependency** | AI calls fail if service unreachable | Local fallback, retry logic |
| **Context Sync** | Window context needs explicit passing | Bridge layer with caching |
| **Session Affinity** | Chat memory requires sticky sessions | Redis/distributed memory |
| **Permission Sync** | Role changes need propagation | Cache invalidation, TTL |
| **Transaction Boundary** | Cannot participate in iDempiere TX | Compensating transactions |

### Security Considerations

| Concern | Risk | Mitigation |
|---------|------|------------|
| **Network Exposure** | Medium | Private network, mTLS |
| **Credential Management** | High | Vault/Secrets Manager |
| **API Key Rotation** | Medium | Automated rotation |
| **Audit Trail** | Low | Correlation IDs, centralized logging |
| **Data Leakage** | Medium | Response filtering, PII detection |

### Operational Requirements

| Requirement | Description |
|-------------|-------------|
| **Deployment** | Container (Docker), K8s/ECS recommended |
| **Monitoring** | Prometheus metrics, Grafana dashboards |
| **Logging** | Centralized (ELK/CloudWatch) |
| **Health Checks** | Readiness/Liveness probes |
| **Scaling** | HPA based on request queue/latency |
| **Backup** | Chat history, embeddings backup |

---

## Integration with Existing ADRs

| ADR | Relation | Impact |
|-----|----------|--------|
| **ADR-002** | LangChain4j Strategy | Satellite enables 1.x features |
| **ADR-003** | MCP Integration | Satellite provides native MCP |
| **ADR-007** | Database Security | Satellite respects same model |
| **ADR-012** | RAG Context | Satellite handles RAG processing |
| **ADR-013** | Observability | Satellite provides full observability |
| **ADR-033** | Streaming/Thinking | Satellite enables extended thinking |
| **ADR-035** | Java Version | Satellite bypasses Java 11 constraint |

---

## Migration Path

### Phase 1: Parallel Deployment (Recommended Start)

```
Week 1-2: Basic Service Setup
- Quarkus project scaffold
- LangChain4j 1.x integration
- Basic REST endpoints
- Database connectivity

Week 3-4: Security & Context
- Context passing DTOs
- Role-based access implementation
- Audit logging
- Bridge client in OSGi plugin

Week 5-6: Interface Layer
- MCP server implementation
- CLI commands
- API documentation

Week 7-8: Testing & Deployment
- Integration tests
- Performance testing
- Container deployment
- Documentation
```

### Phase 2: Feature Parity

- Port existing agents to satellite
- Implement flow/chain sharing
- Add native image support
- Multi-tenant isolation

### Phase 3: Advanced Features

- gRPC interface
- A/B testing infrastructure
- Cost optimization (caching)
- Cross-deployment sharing

---

## Decision Outcome

**Recommendation:** **Proceed with research spike** (2 weeks) to:

1. Prototype basic Quarkus satellite service
2. Validate LangChain4j 1.x + MCP integration
3. Test context passing from iDempiere
4. Measure latency overhead
5. Assess operational complexity

**Decision will be made based on spike results.**

---

## Cost-Benefit Summary

| Factor | Score | Notes |
|--------|-------|-------|
| **Unblocks Features** | +++ | MCP, thinking, full observability |
| **Scalability** | +++ | Independent scaling |
| **Development Velocity** | ++ | Modern tooling, hot reload |
| **Operational Complexity** | -- | Additional component |
| **Network Latency** | - | Acceptable with proper design |
| **Long-term Value** | +++ | Future-proof architecture |

**Net Assessment:** **Strongly favorable** for medium-to-large deployments, **neutral** for single-instance deployments.

---

## References

### Quarkus Resources
- [Quarkus LangChain4j Documentation](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [Quarkus MCP Integration](https://docs.quarkiverse.io/quarkus-langchain4j/dev/mcp.html)
- [Quarkus Performance Guide](https://quarkus.io/performance/)
- [Building MCP Servers with Quarkus](https://quarkus.io/blog/mcp-server/)

### LangChain4j Resources
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j)
- [LangChain4j MCP Tutorial](https://docs.langchain4j.dev/tutorials/mcp/)

### Related Blog Posts
- [MCP with Quarkus LangChain4j - Piotr's TechBlog](https://piotrminkowski.com/2025/11/24/mcp-with-quarkus-langchain4j/)
- [LangChain4j Production-ready features with Quarkus - JavaPro](https://javapro.io/2025/11/20/langchain4j-production-ready-features-with-quarkus/)
- [Quarkus Native vs JVM Performance](https://dev.to/issam1991/quarkus-native-vs-jvm-real-world-performance-comparison-40a4)
- [Multi-tenant Quarkus Applications](https://www.the-main-thread.com/p/quarkus-multi-tenant-todo-java-hibernate)

### Related ADRs
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [ADR-003: MCP Server Integration](003-mcp-server-integration.md)
- [ADR-035: Java Version Strategy](035-java-version-strategy.md)

---

*ADR-038 | Version 1.0 | 2025-12-10*
*Status: **Research/Proposed***
*Next Step: 2-week research spike to validate architecture*
