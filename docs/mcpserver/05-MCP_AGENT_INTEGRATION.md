# MCP Server & AI Agent Framework Integration

**Status**: Documentation updated after code review (2025-11-26)
**Commit**: 01960c2 feat(AIPlugin): Add langchain framework and ERP tool integration

## Overview

The recent addition of a **LangChain4j-based AI Agent Framework** complements the MCP server design perfectly. This document explains how both systems work together and when to use each.

## Quick Comparison

| Aspect | MCP Server | AI Agent Framework |
|--------|-----------|-------------------|
| **Purpose** | External tool invocation | Autonomous reasoning loop |
| **Client** | Claude Code agents, external services | Internal ERP processes |
| **Transport** | HTTP, stdio, SSE | In-process, synchronous |
| **Reasoning** | Single-turn (user specifies tool) | Multi-turn (agent decides tools) |
| **Best For** | Interactive queries, explicit requests | Background tasks, autonomous workflows |
| **Examples** | "Query orders", "Extract context" | "Process orders autonomously", "Reconcile accounts" |

## Architecture Alignment

### Tools Are Unified

Both systems expose the **same underlying tools** through different interfaces:

```
Agent Tools (ITool Interface)
├─ DatabaseQueryTool
├─ FieldMetadataTool
├─ TableMetadataTool
└─ [Custom Tools]
    │
    ├─ Exposed via Agent Loop (BaseAgent)
    │   └─ Multi-turn reasoning with LLM
    │
    └─ Exposed via MCP Server
        └─ Single-tool invocation per request
```

### Tool Definition Flows

#### Path 1: Direct Agent Usage
```
User/Process → AgentFactory.createAgent()
    ↓
BaseAgent (agentic loop)
    ├─ ToolRegistry.getAvailableTools()
    ├─ IAIProvider.generateTextWithFunctions()
    ├─ Parse function calls
    ├─ BoundaryValidator.validateBeforeExecution()
    ├─ ITool.execute()
    └─ Feed results back to LLM
    ↓
AgentResponse (structured result)
```

#### Path 2: MCP Server Usage
```
External Client (Claude Code) → MCP Server
    ├─ Discover tools (ToolRegistry)
    ├─ Select tool
    └─ Call tool with parameters
        ├─ BoundaryValidator.validateBeforeExecution()
        ├─ ITool.execute()
        └─ Return result (JSON/Markdown)
```

**Key Insight**: Both paths use **identical BoundaryValidator** and **ITool** implementations, ensuring consistent security and behavior.

## Integration Points

### 1. Shared ToolRegistry

The `ToolRegistry` is the **single source of truth** for all available tools:

```java
// Register tools once
ToolRegistry registry = ToolRegistry.getInstance();
registry.register(new DatabaseQueryTool());
registry.register(new FieldMetadataTool());
registry.register(new TableMetadataTool());

// Used by Agents
BaseAgent agent = AgentFactory.create(registry, context);

// Used by MCP Server
MCPServer server = new MCPServer(registry);
```

### 2. BoundaryValidator Security

All tool execution (agent or MCP) enforces:

```java
BoundaryValidator.validateBeforeExecution(
    tool,                    // Which tool
    context,                 // User context
    org_id,                  // Multi-tenancy
    role,                    // User role
    parameters               // Parameters to check
);

// Enforces:
// - Role-based permissions (ToolPermission)
// - Org filtering
// - Parameter validation
// - SQL injection prevention
// - Rate limiting
```

### 3. Shared AIFunction DTOs

Tools expose themselves as AIFunction for LLM function calling:

```
ITool Interface
    ↓
ToolRegistry.toAIFunctions()  // Convert to AIFunction
    ↓
AIRequest.setFunctions()  // Send to LLM
    ↓
IAIProvider (uses function calling)
```

This enables the MCP server to:
1. Discover available functions from ToolRegistry
2. Pass them to the LLM via IAIProvider
3. Invoke them when LLM requests

## When to Use Each

### Use MCP Server When:

✅ **External services need access** - Integrate iDempiere with external systems
✅ **Interactive queries** - User asks questions via Claude Code
✅ **One-off operations** - Single tool invocation per request
✅ **Web/Mobile apps** - Need HTTP API to iDempiere
✅ **Third-party integrations** - Standard protocol (MCP)

**Example**: "Show me active orders for customer ABC"
- User specifies exact intent
- MCP server executes query_database tool once
- Returns results to client

### Use AI Agents When:

✅ **Multi-step autonomous workflows** - Process orders automatically
✅ **Reasoning and decision-making** - Analyze and decide on actions
✅ **Background jobs** - Run without user interaction
✅ **Complex business logic** - "If X then do Y" type processes
✅ **ERP-native operations** - Execute within iDempiere context

**Example**: "Process end-of-month reconciliation"
- Agent analyzes transactions
- Identifies discrepancies
- Queries databases for details
- Makes decisions
- Creates journal entries
- Generates reports

## Complementary Capabilities

### What MCP Server Adds to Agents:

1. **External Interface** - Agents run internally; MCP lets external systems use same tools
2. **HTTP/Web Access** - Traditional REST integration for non-Claude clients
3. **Loose Coupling** - MCP clients don't need internal iDempiere deployment
4. **Standardized Protocol** - Agents are proprietary; MCP is industry standard

### What Agent Framework Adds to MCP:

1. **Reasoning** - Agents can reason across multiple tools
2. **Autonomous Execution** - No human in the loop needed
3. **Cost Optimization** - AgentContext tracks costs and can optimize
4. **Audit Trail** - AgentResponse records full reasoning path
5. **Conversation Memory** - Multi-turn dialogue support built-in

## Implementation Strategy

### Phase 1: Unify Tool Definitions (Done ✓)

The recent commit establishes:
- ✓ `ITool` interface for all tools
- ✓ `ToolRegistry` as single source of truth
- ✓ `BoundaryValidator` enforcing security uniformly
- ✓ Initial tools (DatabaseQueryTool, metadata tools)

### Phase 2: Build MCP Server with Tool Bridge (Recommended)

**Create `MCPToolBridge.java`**:

```java
/**
 * Bridge between ToolRegistry (agents) and MCP server
 * Enables MCP clients to invoke agent-registered tools
 */
public class MCPToolBridge {
    private ToolRegistry toolRegistry;
    private IAIProvider provider;

    /**
     * Create MCP server using agent tool registry
     */
    public MCPServer createMCPServer(ToolRegistry registry) {
        this.toolRegistry = registry;
        return new MCPServer() {
            @Override
            public List<MCPResource> discoverResources() {
                return toolRegistry.getTools()
                    .stream()
                    .filter(tool -> isReadOnlyOrSafe(tool))
                    .map(tool -> adaptToolToResource(tool))
                    .collect(toList());
            }

            @Override
            public String executeResource(String toolName, Map params) {
                ITool tool = toolRegistry.get(toolName);
                AgentContext context = createMCPContext(currentUser);
                return tool.execute(context, params);
            }
        };
    }

    private MCPResource adaptToolToResource(ITool tool) {
        // Convert ITool → MCPResource format
        // - Name: tool.getName()
        // - Description: tool.getDescription()
        // - Parameters: tool.getParameters() → JSON Schema
        // - Permissions: tool.getPermissions()
    }
}
```

**Benefits**:
- Single tool registration serves both agents and MCP
- No code duplication
- Consistent security model
- Easy to add new tools (automatic MCP exposure)

### Phase 3: Extend Tool Library (In Progress)

Current tools:
- `DatabaseQueryTool` - Query data
- `FieldMetadataTool` - Get field definitions
- `TableMetadataTool` - Get table schema

Recommended additions:
- `ProcessExecutionTool` - Run iDempiere processes
- `DocumentCreationTool` - Create documents (orders, invoices, etc.)
- `WorkflowTool` - Trigger workflows
- `ReportGenerationTool` - Generate reports

### Phase 4: Add Streaming Support (Optional)

```java
// Current: Synchronous
BaseAgent agent = AgentFactory.create(registry, context);
AgentResponse response = agent.execute("your goal");

// Future: Streaming (if needed)
agent.executeStreaming("your goal", (event) -> {
    if (event instanceof ToolInvocationEvent) {
        // Stream tool selection
    } else if (event instanceof ToolResultEvent) {
        // Stream tool results
    } else if (event instanceof FinalAnswerEvent) {
        // Stream final answer
    }
});
```

## Security Model

### Unified Permission Enforcement

Both MCP server and agents enforce permissions identically:

```
Tool Invocation Request
    ↓
BoundaryValidator.validate()
    ├─ Check: Is user allowed to use this tool? (ToolPermission)
    ├─ Check: Is user's role allowed? (MRole.get())
    ├─ Check: Is org_id within user's access? (Multi-tenancy)
    ├─ Check: Are parameters valid? (ITool.validateParameters())
    ├─ Check: Is parameter data within user's access? (Row-level security)
    └─ Check: Rate limit reached? (AgentContext cost limits)
    ↓
Result: Permission granted OR ToolExecutionException
    ↓
ITool.execute() OR Error response
```

### Data Isolation

- **Org Filtering**: Automatic in DatabaseQueryTool
- **Row-Level Security**: Via SecureDatabaseQueryExecutor
- **Field Redaction**: Sensitive fields masked by DatabaseQueryTool
- **Audit Logging**: All invocations logged with user context

## Error Handling

Both systems use consistent error codes:

```
Tool Execution Errors:
├─ PERMISSION_DENIED - User can't access this tool
├─ VALIDATION_ERROR - Parameters invalid
├─ EXECUTION_ERROR - Tool runtime error
├─ RESOURCE_NOT_FOUND - Tool/table/field doesn't exist
├─ TIMEOUT - Query/operation took too long
├─ RATE_LIMIT_EXCEEDED - Cost or request limit hit
└─ INTERNAL_ERROR - Unexpected failure

Agent Loop Errors:
├─ INVALID_GOAL - Goal can't be parsed
├─ TOOL_NOT_FOUND - Requested tool missing
├─ COST_LIMIT_EXCEEDED - Agent session cost exceeded
├─ TOKEN_LIMIT_EXCEEDED - Token budget exhausted
└─ ITERATION_LIMIT_EXCEEDED - Too many reasoning steps
```

## Recommended MCP Documentation Updates

Based on this analysis, update the MCP documentation:

### Update: `02-IMPLEMENTATION_GUIDE.md`

**Add new section: "Using Existing Agent Tools"**

```markdown
## Integration with Existing Agent Framework

The iDempiere AI Plugin now includes a LangChain4j-based agent framework
(added in commit 01960c2). The MCP server can leverage these tools directly.

### Approach 1: Expose Agent Tools via MCP (Recommended)

Instead of reimplementing the 5 MCP tools from scratch, use the existing
ToolRegistry to power the MCP server.

\`\`\`java
// In MCPServer.ts (translated to Node.js):
const toolRegistry = new ToolRegistry();
const mcpServer = createMCPServerFromRegistry(toolRegistry);

// The MCP server now exposes:
// - All registered ITool implementations
// - With same security (BoundaryValidator)
// - With same error handling
\`\`\`

### Approach 2: Implement Custom MCP Tools

Create MCP tools that aren't in the agent framework yet:

\`\`\`typescript
// Tool not yet in agent framework
createWorkflowExecutionTool(): ToolSchema {
  return {
    name: "execute_workflow",
    description: "Execute an iDempiere workflow",
    // Implementation here
  };
}
\`\`\`

### Tool Development Guide

See `docs/AGENT_FRAMEWORK_GUIDE.md` for:
- How to implement ITool interface
- Security via BoundaryValidator
- Exposing tools to both agents and MCP
```

### Create: `docs/mcpserver/AGENT_FRAMEWORK_GUIDE.md` (New)

Document the agent framework for developers who need to:
- Understand how agents work
- Implement custom ITool classes
- Integrate agent tools with MCP

### Create: `docs/mcpserver/HYBRID_ARCHITECTURE.md` (New)

Document when and how to use:
- MCP server for external access
- Agents for internal autonomous tasks
- Both together in hybrid workflows

## Performance Implications

### Agent Framework Performance
- **Pros**: No serialization (in-process), single JVM, fast
- **Cons**: Synchronous, blocks waiting for LLM, tight coupling to iDempiere

### MCP Server Performance
- **Pros**: Decoupled, can scale independently, supports async
- **Cons**: Serialization overhead, HTTP latency, network hops

### Recommendation
- Use agents for time-sensitive internal operations
- Use MCP for external integrations where latency is acceptable
- Consider hybrid: MCP server calls agent tools via bridge

## Code Quality & Maintenance

### Unified Testing

Test both paths with same test suite:

```java
@Test
void testDatabaseQueryTool() {
    ITool tool = new DatabaseQueryTool();

    // Scenario 1: Direct agent invocation
    AgentContext agentCtx = createAgentContext(user);
    String result1 = tool.execute(agentCtx, params);

    // Scenario 2: MCP server invocation
    MCPContext mcpCtx = createMCPContext(user);
    String result2 = tool.execute(mcpCtx, params);

    // Results should be identical
    assertEquals(result1, result2);
}
```

### Unified Security Testing

```java
@Test
void testBoundaryValidatorEnforcement() {
    BoundaryValidator validator = new BoundaryValidator();

    // Test 1: Agent context
    assertThrows(PermissionDeniedException.class, () ->
        validator.validateBeforeExecution(tool, agentContext, params)
    );

    // Test 2: MCP context
    assertThrows(PermissionDeniedException.class, () ->
        validator.validateBeforeExecution(tool, mcpContext, params)
    );
}
```

## Implementation Roadmap

### Immediate (Week 1)
- ✓ Review and understand new agent framework
- [ ] Update MCP documentation with integration guidance
- [ ] Create MCPToolBridge adapter class

### High Priority (Week 2-3)
- [ ] Implement 3 additional agent tools
  - ProcessExecutionTool
  - DocumentCreationTool
  - WorkflowExecutionTool
- [ ] Write AGENT_FRAMEWORK_GUIDE.md
- [ ] Test both agent and MCP paths equally

### Medium Priority (Week 4-5)
- [ ] Add streaming support if needed
- [ ] Create HYBRID_ARCHITECTURE.md guide
- [ ] Build comprehensive test suite
- [ ] Document for operations team

### Low Priority (Future)
- [ ] Async support for agents
- [ ] Cost optimization algorithms
- [ ] Advanced reasoning patterns

## Conclusion

The new AI Agent Framework and the MCP Server design are **complementary, not competing**:

- **Agents**: Internal reasoning engine for autonomous ERP workflows
- **MCP**: External interface for tool access from any client

By unifying tools through `ToolRegistry` and security through `BoundaryValidator`, we get:
- ✓ **Single source of truth** for tool definitions
- ✓ **Consistent security** across all invocation paths
- ✓ **Code reuse** between agent and MCP implementations
- ✓ **Flexibility** to use each where appropriate
- ✓ **Extensibility** to add new tools once, use everywhere

The architecture elegantly handles both autonomous reasoning (agents) and interactive tool use (MCP), making iDempiere a truly AI-native ERP platform.

---

**For detailed implementation**, see:
- Agent Framework: Files in `src/com/cloudempiere/ai/agent/*`
- Tool Definition: Files in `src/com/cloudempiere/ai/tool/*`
- MCP Server: `02-IMPLEMENTATION_GUIDE.md`
- Security: `03-BEST_PRACTICES.md → Security Section`
