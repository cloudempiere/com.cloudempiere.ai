# Layered Architecture Design: Facade Pattern for Future API Extensibility

## Executive Summary

**Architectural Challenge:** Current monolithic implementation mixes UI, service, and data layers

**Solution:** **Facade Pattern with Clean Layer Separation**

**Goals:**
- ✅ **A. Support current monolithic implementation** (iDempiere OSGi plugin)
- ✅ **B. Enable future external access** (REST API, GraphQL, MCP server)
- ✅ **C. Architectural isolation** (layers don't know about each other's internals)

**Result:** Clean separation allowing multiple consumption patterns from single codebase

---

## Part 1: Current Architecture Problems

### 1.1 Current Monolithic Structure (Before)

```
┌─────────────────────────────────────────────────────────┐
│                 CURRENT MONOLITHIC                       │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  AIChatWidget (ZK UI)                                   │
│         ↓  (direct dependency)                          │
│  AIService                                              │
│         ↓  (direct dependency)                          │
│  LangChain4j Agents                                     │
│         ↓  (direct dependency)                          │
│  Tools (ERPTools, RagTools)                             │
│         ↓  (direct dependency)                          │
│  SecureDatabaseQueryExecutor                            │
│         ↓  (direct dependency)                          │
│  Database (iDempiere tables)                            │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

**Problems:**

| Problem | Impact |
|---|---|
| **UI coupled to service layer** | Cannot expose via REST without ZK | **Service layer coupled to LangChain4j** | Cannot swap frameworks |
| **Tools coupled to iDempiere context** | Cannot call from stateless HTTP |
| **No clear API boundary** | Cannot version APIs independently |
| **Difficult to test** | Must mock entire stack |

---

### 1.2 Future Requirements

**Consumption Pattern A (Current):** iDempiere OSGi Plugin (Monolithic)
```
User → ZK UI → AIService → LangChain4j → Database
```

**Consumption Pattern B (Future):** REST API
```
External App → HTTP → REST Controller → AIService → LangChain4j → Database
```

**Consumption Pattern C (Future):** GraphQL API
```
External App → HTTP → GraphQL Resolver → AIService → LangChain4j → Database
```

**Consumption Pattern D (Future):** MCP Server
```
AI Client (Claude Desktop) → MCP Protocol → MCP Server → AIService → LangChain4j → Database
```

**Key Insight:** All patterns need access to **same core services**, but through **different entry points**

---

## Part 2: Proposed Layered Architecture

### 2.1 Five-Layer Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│             PROPOSED LAYERED ARCHITECTURE                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────┐      │
│  │  LAYER 1: PRESENTATION (Entry Points)                │      │
│  │  ════════════════════════════════════════════════════│      │
│  │  • ZK UI (Current)                                    │      │
│  │  • REST Controllers (Future)                          │      │
│  │  • GraphQL Resolvers (Future)                         │      │
│  │  • MCP Server (Future)                                │      │
│  └──────────────────────────────────────────────────────┘      │
│         ↓ calls                                                  │
│  ┌──────────────────────────────────────────────────────┐      │
│  │  LAYER 2: APPLICATION FACADE (Service API)           │      │
│  │  ════════════════════════════════════════════════════│      │
│  │  • IAIChatFacade (Main API)                          │      │
│  │  • AIAgentFacade (Agent orchestration)               │      │
│  │  • AIKnowledgeFacade (RAG/knowledge)                 │      │
│  │  • AIAnalyticsFacade (Metrics/usage)                 │      │
│  └──────────────────────────────────────────────────────┘      │
│         ↓ delegates to                                           │
│  ┌──────────────────────────────────────────────────────┐      │
│  │  LAYER 3: DOMAIN SERVICES (Business Logic)           │      │
│  │  ════════════════════════════════════════════════════│      │
│  │  • ChatService (Conversation management)             │      │
│  │  • AgentOrchestrator (Multi-agent coordination)      │      │
│  │  • KnowledgeService (RAG retrieval)                  │      │
│  │  • ToolExecutor (Tool invocation)                    │      │
│  └──────────────────────────────────────────────────────┘      │
│         ↓ uses                                                   │
│  ┌──────────────────────────────────────────────────────┐      │
│  │  LAYER 4: INFRASTRUCTURE (Providers & Tools)         │      │
│  │  ════════════════════════════════════════════════════│      │
│  │  • LangChain4j Integration                           │      │
│  │  • Provider Factory (Anthropic/Bedrock/etc.)         │      │
│  │  • Memory Management (ThreadAwareChatMemory)         │      │
│  │  • Tools (ERPTools, RagTools, DomainTools)           │      │
│  └──────────────────────────────────────────────────────┘      │
│         ↓ persists to                                            │
│  ┌──────────────────────────────────────────────────────┐      │
│  │  LAYER 5: DATA ACCESS (Persistence)                  │      │
│  │  ════════════════════════════════════════════════════│      │
│  │  • SecureDatabaseQueryExecutor                       │      │
│  │  • iDempiere Models (MAIProvider, MAIChatEntry, etc.)│      │
│  │  • Vector Store (pgvector)                           │      │
│  └──────────────────────────────────────────────────────┘      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

### 2.2 Layer Responsibilities

#### Layer 1: Presentation (Entry Points)

**Responsibility:** Handle protocol-specific concerns (HTTP, ZK events, MCP protocol)

**Does:**
- Parse requests (HTTP body, ZK event, MCP JSON-RPC)
- Authentication/session management
- Request validation
- Response serialization (JSON, HTML, protobuf)

**Does NOT:**
- Business logic
- Database access
- LLM calls

**Example Classes:**
```
com/cloudempiere/ai/presentation/
├── zk/
│   └── AIChatWidget.java              (Current ZK UI)
├── rest/
│   ├── AIChatController.java          (Future REST)
│   └── AIAgentController.java
├── graphql/
│   ├── AIChatResolver.java            (Future GraphQL)
│   └── AIAgentResolver.java
└── mcp/
    └── AIMCPServer.java                (Future MCP)
```

---

#### Layer 2: Application Facade (Service API)

**Responsibility:** Unified API for all presentation layers (protocol-agnostic)

**Does:**
- Define service contracts (interfaces)
- Coordinate domain services
- Handle cross-cutting concerns (logging, metrics, caching)
- Provide DTOs (Data Transfer Objects)

**Does NOT:**
- Protocol-specific logic (HTTP headers, ZK components)
- Direct LLM calls
- Direct database access

**Example Interfaces:**

```java
package com.cloudempiere.ai.facade;

/**
 * Main facade for AI chat functionality.
 * Protocol-agnostic - can be called from ZK, REST, GraphQL, or MCP.
 */
public interface IAIChatFacade {

    /**
     * Start a streaming chat conversation.
     *
     * @param request Chat request (protocol-agnostic)
     * @param callback Streaming callback
     * @return Conversation ID
     */
    String chatStreaming(ChatRequest request, StreamCallback callback);

    /**
     * Blocking chat (returns complete response).
     *
     * @param request Chat request
     * @return Chat response
     */
    ChatResponse chatBlocking(ChatRequest request);

    /**
     * Get conversation history.
     *
     * @param conversationId Conversation ID
     * @param maxMessages Max messages to return
     * @return List of messages
     */
    List<MessageDTO> getHistory(String conversationId, int maxMessages);

    /**
     * Create new conversation thread.
     *
     * @param userId User ID
     * @param providerId AI provider ID
     * @return Thread ID
     */
    String createThread(int userId, int providerId);
}
```

**DTOs (Protocol-Agnostic):**

```java
package com.cloudempiere.ai.facade.dto;

public class ChatRequest {
    private int userId;
    private int providerId;
    private String message;
    private String conversationId;
    private Map<String, Object> context;  // Optional context (current window, etc.)

    // Builders, getters, setters
}

public class ChatResponse {
    private String conversationId;
    private String message;
    private List<ToolCall> toolCalls;
    private TokenUsage tokenUsage;
    private long latencyMs;

    // Builders, getters, setters
}

public class MessageDTO {
    private String role;  // "user", "assistant", "system"
    private String content;
    private Timestamp timestamp;
    private List<ToolCall> toolCalls;

    // Builders, getters, setters
}

public class StreamCallback {
    void onStart();
    void onNext(String chunk);
    void onToolCall(ToolCall toolCall);
    void onComplete(ChatResponse response);
    void onError(Throwable error);
}
```

---

#### Layer 3: Domain Services (Business Logic)

**Responsibility:** Core business logic (framework-agnostic)

**Does:**
- Conversation management
- Agent orchestration
- Knowledge retrieval
- Tool execution
- Business rules enforcement

**Does NOT:**
- HTTP handling
- ZK component manipulation
- LangChain4j-specific code (delegates to Layer 4)

**Example Classes:**

```java
package com.cloudempiere.ai.service;

/**
 * Chat service (business logic).
 * Framework-agnostic - can work with any LLM framework.
 */
public class ChatService {

    @Reference
    private IAgentOrchestrator orchestrator;

    @Reference
    private IChatMemoryService memoryService;

    @Reference
    private IToolExecutor toolExecutor;

    public String chatStreaming(ChatRequest request, StreamCallback callback) {
        // 1. Load conversation context
        ChatContext context = memoryService.loadContext(request.getConversationId());

        // 2. Determine appropriate agent (orchestration)
        IAgent agent = orchestrator.selectAgent(request.getMessage(), context);

        // 3. Execute chat with agent
        agent.chatStreaming(request.getMessage(), new AgentCallback() {
            @Override
            public void onNext(String chunk) {
                callback.onNext(chunk);
            }

            @Override
            public void onToolCall(ToolCall toolCall) {
                // Execute tool
                ToolResult result = toolExecutor.execute(toolCall, context);
                callback.onToolCall(toolCall);
            }

            @Override
            public void onComplete(String response) {
                // Persist message
                memoryService.addMessage(context.getConversationId(), "assistant", response);
                callback.onComplete(buildResponse(response, context));
            }
        });

        return context.getConversationId();
    }
}
```

---

#### Layer 4: Infrastructure (Providers & Tools)

**Responsibility:** LLM provider integration, tool implementations

**Does:**
- LangChain4j integration
- Provider instantiation (Anthropic, Bedrock, etc.)
- Tool implementations (@Tool methods)
- Memory management

**Does NOT:**
- Business logic
- Direct database access (uses Layer 5)

**Example Classes:**

```java
package com.cloudempiere.ai.infrastructure.langchain4j;

/**
 * LangChain4j agent wrapper (infrastructure layer).
 * Abstracts LangChain4j specifics from domain services.
 */
public class LangChain4jAgent implements IAgent {

    private final ChatLanguageModel chatModel;
    private final List<Object> tools;
    private final ChatMemory memory;

    @Override
    public void chatStreaming(String message, AgentCallback callback) {
        // Use LangChain4j streaming
        StreamingChatLanguageModel streamingModel = (StreamingChatLanguageModel) chatModel;

        streamingModel.generate(message, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                callback.onNext(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                callback.onComplete(response.content().text());
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }
}
```

---

#### Layer 5: Data Access (Persistence)

**Responsibility:** Database operations, security enforcement

**Does:**
- CRUD operations
- Query execution with role-based security
- Audit logging

**Does NOT:**
- Business logic
- LLM calls

**Example Classes:**

```java
package com.cloudempiere.ai.data;

/**
 * Data access layer for chat entries.
 * Framework-agnostic, pure data operations.
 */
public class ChatEntryRepository {

    public MAIChatEntry save(ChatEntryEntity entity) {
        MAIChatEntry entry = new MAIChatEntry(entity.getCtx(), 0, entity.getTrxName());

        // Dual storage
        entry.setContentMarkdown(entity.getMarkdown());
        entry.setContentHTML(entity.getHtml());
        entry.setAD_User_ID(entity.getUserId());
        // ... other fields

        entry.saveEx();
        return entry;
    }

    public List<MAIChatEntry> findByConversationId(String conversationId, int maxMessages) {
        return new Query(ctx, MAIChatEntry.Table_Name,
            "ConversationID = ?", trxName)
            .setParameters(conversationId)
            .setOrderBy("Created DESC")
            .setLimit(maxMessages)
            .list();
    }
}
```

---

## Part 3: Facade Implementation Details

### 3.1 Main Facade Interface

```java
package com.cloudempiere.ai.facade;

/**
 * Main AI Chat Facade.
 * Single entry point for all presentation layers.
 *
 * OSGi Service - can be consumed by:
 * - ZK UI (current)
 * - REST controllers (future)
 * - GraphQL resolvers (future)
 * - MCP servers (future)
 */
@Component(
    service = IAIChatFacade.class,
    immediate = true,
    serviceRanking = 100
)
public class AIChatFacade implements IAIChatFacade {

    @Reference
    private ChatService chatService;

    @Reference
    private IAgentOrchestrator orchestrator;

    @Reference
    private IKnowledgeService knowledgeService;

    @Reference
    private IAnalyticsService analyticsService;

    @Override
    public String chatStreaming(ChatRequest request, StreamCallback callback) {
        // Validate request
        validateRequest(request);

        // Delegate to domain service
        return chatService.chatStreaming(request, callback);
    }

    @Override
    public ChatResponse chatBlocking(ChatRequest request) {
        // Validate request
        validateRequest(request);

        // Delegate to domain service
        CompletableFuture<ChatResponse> future = new CompletableFuture<>();

        chatService.chatStreaming(request, new StreamCallback() {
            @Override
            public void onComplete(ChatResponse response) {
                future.complete(response);
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }

            // ... other callbacks
        });

        return future.join();  // Block until complete
    }

    @Override
    public List<MessageDTO> getHistory(String conversationId, int maxMessages) {
        return chatService.getHistory(conversationId, maxMessages);
    }

    @Override
    public String createThread(int userId, int providerId) {
        return chatService.createThread(userId, providerId);
    }

    private void validateRequest(ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        if (request.getProviderId() <= 0) {
            throw new IllegalArgumentException("Invalid provider ID");
        }
    }
}
```

---

### 3.2 Consumption Pattern A: ZK UI (Current)

```java
package com.cloudempiere.ai.presentation.zk;

/**
 * ZK UI component (Presentation Layer).
 * Delegates to facade, no business logic.
 */
public class AIChatWidget extends Div {

    @WireVariable
    private IAIChatFacade chatFacade;  // OSGi injection

    private Html messageContent;
    private Textbox inputBox;

    public void onSendMessage() {
        String message = inputBox.getValue();

        // Build request (protocol-specific → protocol-agnostic DTO)
        ChatRequest request = ChatRequest.builder()
            .userId(Env.getAD_User_ID(getCtx()))
            .providerId(getSelectedProviderId())
            .message(message)
            .conversationId(getCurrentConversationId())
            .context(buildContext())  // Current window, etc.
            .build();

        // Call facade with streaming callback
        chatFacade.chatStreaming(request, new StreamCallback() {
            @Override
            public void onNext(String chunk) {
                // Update ZK UI (protocol-specific)
                Executions.schedule(messageContent.getDesktop(), event -> {
                    messageContent.appendChild(new Text(chunk));
                }, new Event("onUpdate"));
            }

            @Override
            public void onComplete(ChatResponse response) {
                // Update UI with completion status
                showCompletionStatus(response);
            }

            @Override
            public void onError(Throwable error) {
                // Show error in UI
                Messagebox.show(error.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
            }

            // ... other callbacks
        });
    }

    private Map<String, Object> buildContext() {
        // ZK-specific context extraction
        Map<String, Object> context = new HashMap<>();
        context.put("AD_Window_ID", getCurrentWindowId());
        context.put("AD_Tab_ID", getCurrentTabId());
        context.put("Record_ID", getCurrentRecordId());
        return context;
    }
}
```

---

### 3.3 Consumption Pattern B: REST API (Future)

```java
package com.cloudempiere.ai.presentation.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

/**
 * REST API endpoint (Presentation Layer).
 * Exposes facade via HTTP.
 */
@Path("/api/v1/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AIChatController {

    @Reference
    private IAIChatFacade chatFacade;  // OSGi injection

    /**
     * POST /api/v1/chat/streaming
     * Server-Sent Events (SSE) streaming endpoint.
     */
    @POST
    @Path("/streaming")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void chatStreaming(
        @Context SseEventSink sseEventSink,
        @Context Sse sse,
        ChatRequestDTO requestDTO
    ) {
        // Convert REST DTO → Facade DTO (protocol-agnostic)
        ChatRequest request = ChatRequest.builder()
            .userId(requestDTO.getUserId())
            .providerId(requestDTO.getProviderId())
            .message(requestDTO.getMessage())
            .conversationId(requestDTO.getConversationId())
            .context(requestDTO.getContext())
            .build();

        // Call facade with streaming callback
        chatFacade.chatStreaming(request, new StreamCallback() {
            @Override
            public void onNext(String chunk) {
                // Send SSE event (protocol-specific)
                OutboundSseEvent event = sse.newEventBuilder()
                    .name("message")
                    .data(chunk)
                    .build();
                sseEventSink.send(event);
            }

            @Override
            public void onComplete(ChatResponse response) {
                // Send completion event
                OutboundSseEvent event = sse.newEventBuilder()
                    .name("complete")
                    .data(response)
                    .build();
                sseEventSink.send(event);
                sseEventSink.close();
            }

            @Override
            public void onError(Throwable error) {
                // Send error event
                OutboundSseEvent event = sse.newEventBuilder()
                    .name("error")
                    .data(error.getMessage())
                    .build();
                sseEventSink.send(event);
                sseEventSink.close();
            }

            // ... other callbacks
        });
    }

    /**
     * POST /api/v1/chat/blocking
     * Standard blocking endpoint (returns complete response).
     */
    @POST
    @Path("/blocking")
    public Response chatBlocking(ChatRequestDTO requestDTO) {
        // Convert REST DTO → Facade DTO
        ChatRequest request = convertToFacadeRequest(requestDTO);

        // Call facade (blocking)
        ChatResponse response = chatFacade.chatBlocking(request);

        // Convert Facade DTO → REST DTO
        ChatResponseDTO responseDTO = convertToRestResponse(response);

        return Response.ok(responseDTO).build();
    }

    /**
     * GET /api/v1/chat/{conversationId}/history
     * Get conversation history.
     */
    @GET
    @Path("/{conversationId}/history")
    public Response getHistory(
        @PathParam("conversationId") String conversationId,
        @QueryParam("maxMessages") @DefaultValue("20") int maxMessages
    ) {
        List<MessageDTO> messages = chatFacade.getHistory(conversationId, maxMessages);

        return Response.ok(messages).build();
    }
}
```

**REST DTO Example:**

```java
package com.cloudempiere.ai.presentation.rest.dto;

public class ChatRequestDTO {
    private int userId;
    private int providerId;
    private String message;
    private String conversationId;
    private Map<String, Object> context;

    // Jackson annotations, getters, setters
}
```

---

### 3.4 Consumption Pattern C: GraphQL API (Future)

```java
package com.cloudempiere.ai.presentation.graphql;

import org.eclipse.microprofile.graphql.*;

/**
 * GraphQL API endpoint (Presentation Layer).
 * Exposes facade via GraphQL.
 */
@GraphQLApi
public class AIChatResolver {

    @Inject
    private IAIChatFacade chatFacade;

    /**
     * Mutation: Start streaming chat.
     * Returns subscription ID.
     */
    @Mutation
    public String chatStreaming(@Name("request") ChatRequestInput request) {
        // Convert GraphQL input → Facade DTO
        ChatRequest facadeRequest = ChatRequest.builder()
            .userId(request.userId)
            .providerId(request.providerId)
            .message(request.message)
            .conversationId(request.conversationId)
            .context(request.context)
            .build();

        // Call facade (returns conversation ID)
        String conversationId = chatFacade.chatStreaming(facadeRequest, new StreamCallback() {
            @Override
            public void onNext(String chunk) {
                // Publish to GraphQL subscription
                subscriptionPublisher.publish(conversationId, chunk);
            }

            // ... other callbacks
        });

        return conversationId;
    }

    /**
     * Subscription: Subscribe to streaming messages.
     */
    @Subscription
    public Multi<String> chatMessages(@Name("conversationId") String conversationId) {
        return subscriptionPublisher.subscribe(conversationId);
    }

    /**
     * Query: Get conversation history.
     */
    @Query
    public List<MessageDTO> getHistory(
        @Name("conversationId") String conversationId,
        @Name("maxMessages") @DefaultValue("20") int maxMessages
    ) {
        return chatFacade.getHistory(conversationId, maxMessages);
    }
}
```

---

### 3.5 Consumption Pattern D: MCP Server (Future)

```java
package com.cloudempiere.ai.presentation.mcp;

/**
 * MCP Server (Model Context Protocol).
 * Exposes AI tools to external AI clients (Claude Desktop, etc.).
 */
public class AIMCPServer implements MCPServer {

    @Reference
    private IAIChatFacade chatFacade;

    @Reference
    private IAIAgentFacade agentFacade;

    @Override
    public List<MCPTool> getTools() {
        return List.of(
            MCPTool.builder()
                .name("query_database")
                .description("Execute SQL query on ERP database")
                .parameters(Map.of(
                    "sql", "string",
                    "maxRows", "integer"
                ))
                .build(),

            MCPTool.builder()
                .name("search_knowledge")
                .description("Search ERP knowledge base")
                .parameters(Map.of(
                    "query", "string",
                    "maxResults", "integer"
                ))
                .build()
            // ... other tools
        );
    }

    @Override
    public MCPToolResult executeTool(MCPToolRequest request) {
        // Route to appropriate facade method
        switch (request.getName()) {
            case "query_database":
                return executeQueryDatabase(request);

            case "search_knowledge":
                return executeSearchKnowledge(request);

            default:
                throw new IllegalArgumentException("Unknown tool: " + request.getName());
        }
    }

    private MCPToolResult executeQueryDatabase(MCPToolRequest request) {
        // Extract parameters
        String sql = request.getParameter("sql");
        int maxRows = request.getParameter("maxRows", 100);

        // Call facade (via agent facade)
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
            .toolName("queryDatabase")
            .parameters(Map.of("sql", sql, "maxRows", maxRows))
            .context(extractContextFromMCP(request))
            .build();

        ToolExecutionResult result = agentFacade.executeTool(toolRequest);

        // Convert to MCP result
        return MCPToolResult.builder()
            .content(result.getOutput())
            .isError(result.isError())
            .build();
    }
}
```

---

## Part 4: Package Structure (Reorganized)

```
com/cloudempiere/ai/
├── presentation/              (Layer 1: Entry Points)
│   ├── zk/
│   │   ├── AIChatWidget.java             (Current ZK UI)
│   │   └── AIChatMessageComponent.java
│   ├── rest/
│   │   ├── AIChatController.java         (Future REST)
│   │   ├── AIAgentController.java
│   │   └── dto/
│   │       ├── ChatRequestDTO.java
│   │       └── ChatResponseDTO.java
│   ├── graphql/
│   │   ├── AIChatResolver.java           (Future GraphQL)
│   │   ├── AIAgentResolver.java
│   │   └── input/
│   │       └── ChatRequestInput.java
│   └── mcp/
│       └── AIMCPServer.java              (Future MCP)
│
├── facade/                    (Layer 2: Application Facade)
│   ├── IAIChatFacade.java               (Main facade interface)
│   ├── AIChatFacade.java                (Implementation)
│   ├── IAIAgentFacade.java
│   ├── AIAgentFacade.java
│   ├── IAIKnowledgeFacade.java
│   ├── AIKnowledgeFacade.java
│   └── dto/
│       ├── ChatRequest.java             (Protocol-agnostic DTOs)
│       ├── ChatResponse.java
│       ├── MessageDTO.java
│       ├── ToolCall.java
│       └── TokenUsage.java
│
├── service/                   (Layer 3: Domain Services)
│   ├── ChatService.java                 (Conversation management)
│   ├── AgentOrchestrator.java           (Multi-agent coordination)
│   ├── KnowledgeService.java            (RAG retrieval)
│   ├── ToolExecutor.java                (Tool invocation)
│   ├── AnalyticsService.java            (Metrics)
│   └── ChatMemoryService.java           (Memory management)
│
├── infrastructure/            (Layer 4: Providers & Tools)
│   ├── langchain4j/
│   │   ├── LangChain4jProviderFactory.java
│   │   ├── LangChain4jAgent.java
│   │   ├── ThreadAwareChatMemory.java
│   │   └── tools/
│   │       ├── ERPTools.java
│   │       ├── RagTools.java
│   │       ├── SalesTools.java          (Domain-specific)
│   │       └── InventoryTools.java
│   ├── providers/
│   │   ├── BedrockChatModelWrapper.java
│   │   └── BedrockStreamingChatModelWrapper.java
│   └── rendering/
│       ├── StreamingMarkdownRenderer.java
│       ├── ChunkCleaner.java
│       └── MarkdownSyntaxSanitizer.java
│
└── data/                      (Layer 5: Data Access)
    ├── ChatEntryRepository.java
    ├── ProviderRepository.java
    ├── UsageMetricsRepository.java
    ├── SecureDatabaseQueryExecutor.java
    └── entities/
        ├── ChatEntryEntity.java         (Domain entities)
        └── ProviderEntity.java
```

---

## Part 5: Migration Strategy

### 5.1 Phase 1: Extract Facade (Week 1-2)

**Goal:** Create facade layer without changing existing code

**Steps:**

1. **Create facade interface**
   ```java
   // NEW: com/cloudempiere/ai/facade/IAIChatFacade.java
   public interface IAIChatFacade {
       String chatStreaming(ChatRequest request, StreamCallback callback);
       ChatResponse chatBlocking(ChatRequest request);
       List<MessageDTO> getHistory(String conversationId, int maxMessages);
   }
   ```

2. **Create DTOs**
   ```java
   // NEW: com/cloudempiere/ai/facade/dto/ChatRequest.java
   public class ChatRequest { ... }

   // NEW: com/cloudempiere/ai/facade/dto/ChatResponse.java
   public class ChatResponse { ... }
   ```

3. **Create facade implementation (delegates to existing AIService)**
   ```java
   // NEW: com/cloudempiere/ai/facade/AIChatFacade.java
   public class AIChatFacade implements IAIChatFacade {
       @Reference
       private AIService aiService;  // Existing service

       @Override
       public String chatStreaming(ChatRequest request, StreamCallback callback) {
           // Delegate to existing service
           return aiService.chatStreamingWithContext(...);
       }
   }
   ```

4. **Update ZK UI to use facade**
   ```java
   // MODIFIED: com/cloudempiere/ai/presentation/zk/AIChatWidget.java
   @WireVariable
   private IAIChatFacade chatFacade;  // Use facade instead of AIService

   public void onSendMessage() {
       ChatRequest request = buildRequest();
       chatFacade.chatStreaming(request, buildCallback());
   }
   ```

**Result:** Facade layer exists, ZK UI decoupled from implementation

---

### 5.2 Phase 2: Add REST API (Week 3-4)

**Goal:** Expose facade via REST (proof of concept)

**Steps:**

1. **Add JAX-RS dependency** (pom.xml)
   ```xml
   <dependency>
       <groupId>org.glassfish.jersey.core</groupId>
       <artifactId>jersey-server</artifactId>
       <version>3.1.0</version>
   </dependency>
   ```

2. **Create REST controller**
   ```java
   // NEW: com/cloudempiere/ai/presentation/rest/AIChatController.java
   @Path("/api/v1/chat")
   public class AIChatController {
       @Reference
       private IAIChatFacade chatFacade;

       @POST
       @Path("/streaming")
       public void chatStreaming(...) { ... }
   }
   ```

3. **Register JAX-RS application**
   ```java
   // NEW: com/cloudempiere/ai/presentation/rest/RestApplication.java
   @ApplicationPath("/ai")
   public class RestApplication extends Application { }
   ```

4. **Test REST endpoint**
   ```bash
   curl -X POST http://localhost:8080/ai/api/v1/chat/blocking \
       -H "Content-Type: application/json" \
       -d '{"userId": 100, "providerId": 1, "message": "Hello"}'
   ```

**Result:** REST API working alongside ZK UI (same facade)

---

### 5.3 Phase 3: Refactor Domain Services (Week 5-8)

**Goal:** Extract business logic from AIService into domain services

**Steps:**

1. **Create ChatService** (pure business logic)
   ```java
   // NEW: com/cloudempiere/ai/service/ChatService.java
   public class ChatService {
       // Move business logic from AIService here
   }
   ```

2. **Update facade to use ChatService**
   ```java
   // MODIFIED: com/cloudempiere/ai/facade/AIChatFacade.java
   public class AIChatFacade implements IAIChatFacade {
       @Reference
       private ChatService chatService;  // Use domain service

       @Override
       public String chatStreaming(ChatRequest request, StreamCallback callback) {
           return chatService.chatStreaming(request, callback);
       }
   }
   ```

3. **Deprecate AIService** (mark as deprecated, keep for backward compatibility)
   ```java
   // MODIFIED: com/cloudempiere/ai/provider/langchain4j/AIService.java
   @Deprecated
   public class AIService implements IAIService {
       // Keep for backward compatibility, delegate to facade
   }
   ```

**Result:** Clean separation of concerns (facade → domain service → infrastructure)

---

### 5.4 Phase 4: Add GraphQL/MCP (Week 9+)

**Goal:** Add additional consumption patterns

**Effort:** Low (facade already exists, just add presentation layer)

---

## Part 6: Benefits & Trade-offs

### 6.1 Benefits ✅

| Benefit | Description |
|---|---|
| **Multi-protocol support** | Same code, multiple entry points (ZK, REST, GraphQL, MCP) |
| **Testability** | Each layer can be unit tested independently |
| **Maintainability** | Clear separation of concerns |
| **Flexibility** | Easy to swap implementations (e.g., LangChain4j → Spring AI) |
| **API versioning** | Can version facade independently from UI |
| **Security** | Centralized validation in facade layer |

---

### 6.2 Trade-offs ⚠️

| Trade-off | Mitigation |
|---|---|
| **More classes** | Organized into clear packages |
| **DTO mapping overhead** | Use MapStruct for automatic mapping |
| **Initial effort** | Phased migration (3 months) |

---

## Part 7: Summary

**Current State:** Monolithic (UI → Service → LangChain4j → DB)

**Target State:** Layered with Facade (Presentation → Facade → Domain → Infrastructure → Data)

**Migration:** 4-phase approach (8 weeks)

**Result:** Same codebase supports multiple consumption patterns:
- ✅ ZK UI (current)
- ✅ REST API (future)
- ✅ GraphQL API (future)
- ✅ MCP Server (future)

**Key Principle:** **Facade Pattern = Single entry point, multiple consumers**
