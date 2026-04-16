# ADR-010: Agent Orchestration Architecture

**Status**: Partially Implemented — `ERPAgent` + `ERPTools` layer is fully active; `OrchestratorAgent` + domain agent layer is prepared but **not yet wired into the chat flow**
**Date**: 2025-12-01
**Updated**: 2026-03-11
**Deciders**: Architecture Team
**Related**: [ADR-004](004-java-agent-framework.md), [ADR-009](009-domain-boundaries-agent-scope.md)

## Implementation Status

The primary `ERPAgent` path is fully implemented and active. `OrchestratorAgent` exists as an OSGi `@Component` and binds domain agents dynamically, but `AIService` does not inject or call it — it builds `ERPAgent` directly via `AiServices.builder()`. The multi-domain routing layer is prepared but bypassed.

| Component | Status | Notes |
|-----------|--------|-------|
| `IERPAgent` / `ERPAgent` | ✅ Active in production | Main agent used by `AIService` |
| `ERPStreamingAgent` | ✅ Active in production | Streaming variant |
| `SimpleAgent` / `SimpleStreamingAgent` | ✅ Active in production | Fallback for providers without tool support |
| `ERPTools` (@Tool methods) | ✅ Active in production | `queryDatabase()`, `getTableMetadata()`, etc. |
| `AIService` (main facade) | ✅ Active in production | Builds ERPAgent; does not call OrchestratorAgent |
| `ThreadAwareChatMemory` | ✅ Active in production | Per-session chat memory |
| `LangChain4jProviderFactory` | ✅ Active in production | Creates native ChatLanguageModel instances |
| `OrchestratorAgent` | ✅ Implemented as OSGi `@Component` | **Not called** — no callers in codebase |
| `IDomainAgent` routing | ❌ Not connected | `OrchestratorAgent.processQuery()` has no callers |

**Note:** The custom `CloudempiereLanguageModel` adapter from the original plan was not needed — `LangChain4jProviderFactory` creates native `ChatLanguageModel` instances directly from `MAIProvider` config.

## Context

The current `com.cloudempiere.ai` implementation provides excellent provider abstraction (IAIProvider, AnthropicProvider, AWSBedrockProvider) but lacks the orchestration layer needed to build autonomous agents that can:
- Execute multi-step workflows
- Use multiple tools to accomplish goals
- Maintain conversation context
- Make decisions about which tools to invoke
- Handle errors and retries

**Current Architecture** (Provider-focused):
```
User Request → IAIProvider → LLM API → Response
```

**Needed Architecture** (Agent-focused):
```
User Goal → Agent (with tools) → Provider → LLM API
                ↓
            Tool Execution
                ↓
            Business Logic
                ↓
            Database/iDempiere
```

### The Gap

We have:
- ✅ Provider abstraction (IAIProvider)
- ✅ Security layer (SecureDatabaseQueryExecutor)
- ✅ Context providers (WindowContextProvider, ChartContextProvider)
- ✅ Audit trail infrastructure

We're missing:
- ❌ Agent interface/implementation
- ❌ Agent loop (ReAct pattern)
- ❌ Tool registry and discovery
- ❌ Conversation memory management
- ❌ ERP-specific tools beyond database queries
- ❌ Workflow orchestration

## Decision

We will implement a **layered agent architecture** using LangChain4j for orchestration while preserving our existing provider abstraction:

### Architecture Layers

```
┌────────────────────────────────────────┐
│ Layer 4: Integration Layer             │
│ - REST API endpoints                   │
│ - iDempiere UI components (ZK)         │
│ - Message queues (async operations)    │
└────────────────────────────────────────┘
              ↓
┌────────────────────────────────────────┐
│ Layer 3: Agent Orchestration (NEW!)    │
│ - Agent interface & implementations    │
│ - LangChain4j AiServices integration   │
│ - Conversation memory                  │
│ - Tool registry & discovery            │
│ - Task execution engine                │
│ - Workflow coordination                │
└────────────────────────────────────────┘
              ↓
┌────────────────────────────────────────┐
│ Layer 2: Business Tools (NEW!)         │
│ - MetadataQueryTool (AD_Field, etc.)   │
│ - DatabaseQueryTool (secure queries)   │
│ - ProcessExecutionTool (AD_Process)    │
│ - ReportGenerationTool (Jasper, etc.)  │
│ - DocumentationTool (knowledge base)   │
│ - ValidationTool (business rules)      │
└────────────────────────────────────────┘
              ↓
┌────────────────────────────────────────┐
│ Layer 1: Provider Abstraction (EXISTS) │
│ - IAIProvider interface                │
│ - AnthropicProvider, AWSBedrockProvider│
│ - Cost tracking, health monitoring     │
│ - Error handling & retries             │
└────────────────────────────────────────┘
              ↓
┌────────────────────────────────────────┐
│ Layer 0: iDempiere/Database (EXISTS)   │
│ - SecureDatabaseQueryExecutor          │
│ - Business processes (AD_Process)      │
│ - Data models & services               │
└────────────────────────────────────────┘
```

### Key Architectural Decisions

#### 1. Use LangChain4j for Agent Orchestration

**Rationale**:
- Proven library with active development
- Native support for Claude via Anthropic SDK
- Built-in ReAct agent loop implementation
- Tool/function calling abstraction
- Conversation memory management
- MCP (Model Context Protocol) support

**Integration with Existing Code**:
```java
// LangChain4j will use our IAIProvider implementations
public class CloudempiereLanguageModel implements ChatLanguageModel {
    private final IAIProvider provider;

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        AIRequest request = AIRequest.builder()
            .messages(convertMessages(messages))
            .build();

        AIResponse response = provider.generateText(request);
        return convertResponse(response);
    }
}
```

#### 2. Agent Interface Design

```java
public interface IAgent {
    /**
     * Process a user request through the agent
     * @param request User input + context
     * @return Agent response with results
     */
    AgentResponse execute(AgentRequest request);

    /**
     * Get available tools for this agent
     */
    List<Tool> getTools();

    /**
     * Get agent's domain boundaries
     */
    AgentBoundary getBoundary();

    /**
     * Get conversation memory
     */
    ChatMemory getMemory();

    /**
     * Health check
     */
    boolean isHealthy();
}
```

**Concrete Implementation**:
```java
@Component
public class InventoryAgent implements IAgent {
    private final AiServices aiService;
    private final ToolRegistry toolRegistry;
    private final AgentBoundary boundary;
    private final ChatMemoryProvider memoryProvider;

    public InventoryAgent(
        AIProviderFactory providerFactory,
        ToolRegistry toolRegistry,
        BoundaryConfigRepository boundaryRepo
    ) {
        // Load boundary configuration
        this.boundary = boundaryRepo.findByAgentName("InventoryAgent");

        // Register tools based on boundary
        this.toolRegistry = toolRegistry;
        registerTools();

        // Setup LangChain4j AiServices with our provider
        IAIProvider provider = providerFactory.getProvider(
            boundary.getProviderType()
        );

        this.aiService = AiServices.builder(InventoryAssistant.class)
            .chatLanguageModel(new CloudempiereLanguageModel(provider))
            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .tools(toolRegistry.getTools(boundary))
            .build();
    }

    private void registerTools() {
        toolRegistry.register(
            new DatabaseQueryTool(boundary),
            new MetadataQueryTool(boundary),
            new InventoryAdjustmentTool(boundary)
        );
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        // Enforce boundaries
        validateBoundaries(request);

        // Execute through LangChain4j
        String response = aiService.chat(request.getUserInput());

        // Track cost
        trackCost(request, response);

        return AgentResponse.builder()
            .content(response)
            .toolsUsed(getToolsUsed())
            .cost(calculateCost())
            .build();
    }
}
```

#### 3. Tool Registry Design

**Tool Interface**:
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tool {
    String name();
    String description();
    String[] requiredPermissions() default {};
}

@Tool(
    name = "query_database",
    description = "Execute a secure database query with org filtering",
    requiredPermissions = {"READ_DATABASE"}
)
public class DatabaseQueryTool implements ERP Tool {
    private final SecureDatabaseQueryExecutor executor;
    private final AgentBoundary boundary;

    @ToolExecutor
    public String execute(
        @P("SQL query to execute") String query,
        @P("Maximum rows to return") int maxRows
    ) {
        // Validate against boundary
        if (!boundary.canExecuteQuery(query)) {
            throw new ToolExecutionException(
                "Query violates agent boundaries"
            );
        }

        // Add automatic org filtering
        String filteredQuery = boundary.applyOrgFilter(query);

        // Execute securely
        SecureQueryRequest request = SecureQueryRequest.builder()
            .query(filteredQuery)
            .maxRows(Math.min(maxRows, boundary.getMaxRows()))
            .userId(boundary.getAgentUserId())
            .build();

        SecureQueryResult result = executor.executeSecureQuery(request);
        return result.toJson();
    }
}
```

**Tool Registry**:
```java
@Component
public class ToolRegistry {
    private final Map<String, ERPTool> tools = new ConcurrentHashMap<>();

    public void register(ERPTool tool) {
        Tool annotation = tool.getClass().getAnnotation(Tool.class);
        tools.put(annotation.name(), tool);
    }

    public List<ERPTool> getTools(AgentBoundary boundary) {
        return tools.values().stream()
            .filter(tool -> boundary.hasPermissions(
                tool.getRequiredPermissions()
            ))
            .collect(Collectors.toList());
    }
}
```

#### 4. Essential ERP Tools

We will implement these core tools for Phase 1:

1. **MetadataQueryTool** - Query iDempiere Application Dictionary
   ```java
   @Tool(name = "get_field_metadata")
   public String getFieldMetadata(String tableName, String fieldName)
   ```

2. **DatabaseQueryTool** - Execute secure SQL queries
   ```java
   @Tool(name = "query_database")
   public String executeQuery(String query, int maxRows)
   ```

3. **ProcessExecutionTool** - Run iDempiere processes
   ```java
   @Tool(name = "execute_process")
   public String executeProcess(String processName, Map<String, String> params)
   ```

4. **ReportGenerationTool** - Generate reports
   ```java
   @Tool(name = "generate_report")
   public String generateReport(String reportName, String format)
   ```

5. **DocumentationTool** - Query knowledge base
   ```java
   @Tool(name = "search_documentation")
   public String searchDocs(String query)
   ```

#### 5. Conversation Memory Strategy

**Per-Request Context** (stateless):
```java
AgentContext {
    - userId
    - clientId
    - orgIds (accessible)
    - roleId
    - language
    - currentWindow
    - currentRecord
}
```

**Conversation Memory** (stateful):
```java
// Use LangChain4j MessageWindowChatMemory
ChatMemory memory = MessageWindowChatMemory.builder()
    .maxMessages(10) // Last 10 exchanges
    .id(sessionId)    // Per-session isolation
    .build();
```

**Long-term Memory** (optional, Phase 2):
```java
// Store in iDempiere tables
AIG_AgentMemory {
    - agent_name
    - user_id
    - session_id
    - learned_patterns (JSON)
    - successful_workflows (JSON)
    - created_timestamp
}
```

## Consequences

### Positive

1. **Separation of Concerns**: Provider abstraction remains unchanged
2. **Industrial Standard**: LangChain4j is proven and well-maintained
3. **Extensibility**: Easy to add new tools and agents
4. **Testability**: Each layer can be tested independently
5. **Maintainability**: Clear boundaries between layers
6. **Performance**: Efficient tool discovery and execution
7. **Integration**: Works with existing context providers

### Negative

1. **Learning Curve**: Team must learn LangChain4j API
2. **Dependency**: Reliance on external library (mitigated by adapter pattern)
3. **Complexity**: More layers than simple provider-only approach
4. **Memory**: Chat memory requires storage management

### Risks and Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| LangChain4j breaking changes | MEDIUM | Adapter pattern isolates dependency |
| Tool discovery overhead | LOW | Cache tool registry at startup |
| Memory growth | MEDIUM | Implement TTL and cleanup jobs |
| Tool permission bypass | HIGH | Enforce boundaries at tool level |

## Implementation Roadmap

### Phase 1: Core Framework (Week 1-2)

**Week 1**:
- [x] Implement `IDomainAgent` interface
- [x] Create `LangChain4jProviderFactory` (native models, no adapter needed)
- [x] Implement `ERPTools` with @Tool annotations (replaces ToolRegistry)
- [x] Create `DatabaseQueryTool` (via `ERPTools.queryDatabase()`)
- [x] Setup LangChain4j AiServices integration

**Week 2**:
- [x] Implement metadata query (via `ERPTools` + `ADSchemaCache`)
- [x] Create first concrete agent (`InventoryAgent`)
- [x] Implement conversation memory (`ThreadAwareChatMemory`)
- [x] Add cost tracking integration (`AIMetricsListener`)
- [ ] Comprehensive integration tests

### Phase 2: Additional Tools (Week 2-3)

- [x] Domain-specific tool classes (`SalesTools`, `InventoryTools`, `PurchasingTools`, etc.)
- [x] `KbTools` (Knowledge Base tools)
- [ ] `ProcessExecutionTool` (run AD_Process from AI)
- [ ] `ReportGenerationTool`

### Phase 3: Additional Agents (Week 3-4)

- [x] Implement `SalesAgent`
- [x] Implement `PurchasingAgent`
- [x] Implement `SupportAgent`
- [x] Implement `KbAgent`
- [x] `OrchestratorAgent` routes to domain agents
- [ ] Production hardening + inter-agent call depth enforcement

## Example: Complete Agent Workflow

```java
// User request
"Show me products that are low in stock and suggest reorder quantities"

// Agent execution flow:
InventoryAgent.execute(request)
  ↓
LangChain4j AiServices (ReAct loop)
  ↓
Step 1: Use MetadataQueryTool
  → "What tables contain inventory levels?"
  → Returns: M_Storage, M_Product, M_Warehouse
  ↓
Step 2: Use DatabaseQueryTool
  → "SELECT m.M_Product_ID, p.Name, m.QtyOnHand, p.MinimumStock
      FROM M_Storage m
      JOIN M_Product p ON m.M_Product_ID = p.M_Product_ID
      WHERE m.QtyOnHand < p.MinimumStock
      AND m.AD_Client_ID = ?
      AND m.AD_Org_ID IN (?)"
  → Returns: 15 products below minimum
  ↓
Step 3: Use CalculationTool (internal logic)
  → Calculate suggested reorder quantities
  → Based on: lead time, average consumption, safety stock
  ↓
Step 4: Generate response
  → Formatted table with products + reorder suggestions
  → Cost: $0.50 (within budget)
  → Audit: Logged to AIG_AgentAudit
```

## Success Metrics

1. **Developer Productivity**: Time to create new agent < 4 hours
2. **Tool Reusability**: Each tool used by 3+ agents
3. **Cost Efficiency**: Average query cost < $1
4. **Response Quality**: 90%+ accurate tool selections
5. **System Stability**: 99.9% uptime for agent services

## References

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [ADR-004: Java Agent Framework Selection](004-java-agent-framework.md)
- [ADR-009: Domain Boundaries and Agent Scope](009-domain-boundaries-agent-scope.md)
- [ReAct: Synergizing Reasoning and Acting in Language Models](https://arxiv.org/abs/2210.03629)
