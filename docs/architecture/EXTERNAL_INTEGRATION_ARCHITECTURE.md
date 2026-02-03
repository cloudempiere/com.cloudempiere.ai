# External Integration Architecture - Cloudempiere AI

**Date:** 2026-01-30
**Status:** Architectural Design
**Purpose:** Define how external plugins (GraphQL, REST, MCP, Auth) interact with AI core

---

## Overview

The Cloudempiere AI ecosystem supports **external consumption** through separate OSGi plugins:

| Plugin | Purpose | Protocol | Authentication |
|--------|---------|----------|----------------|
| **com.cloudempiere.graphql** | GraphQL API for AI chat | HTTP/GraphQL | OAuth2 (com.cloudempiere.auth) |
| **com.cloudempiere.rest** | REST API for AI chat | HTTP/JSON | OAuth2 (com.cloudempiere.auth) |
| **com.cloudempiere.mcp** | Model Context Protocol server | MCP/JSON-RPC | OAuth2 (com.cloudempiere.auth) |
| **com.cloudempiere.auth** | OAuth2 authentication layer | OAuth2/JWT | N/A (provider) |

**Key Principle:** External plugins **consume** AI core services via OSGi service contracts, not direct code coupling.

---

## Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                  EXTERNAL INTEGRATION ARCHITECTURE                          │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ LAYER 0: AUTHENTICATION (Separate Plugin)                            │ │
│  │ Bundle: com.cloudempiere.auth                                        │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  OAuth2 Provider:                                                     │ │
│  │  • JWT token generation                                              │ │
│  │  • Token validation                                                   │ │
│  │  • Scope-based access control                                        │ │
│  │  • User authentication (AD_User_ID mapping)                          │ │
│  │                                                                       │ │
│  │  Export-Package:                                                      │ │
│  │  • com.cloudempiere.auth;version="1.0.0"                             │ │
│  │                                                                       │ │
│  │  OSGi Service:                                                        │ │
│  │  • IAuthenticationService                                            │ │
│  │      - validateToken(String token): AuthContext                      │ │
│  │      - getUserContext(AuthContext): UserContext                      │ │
│  │                                                                       │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
│                              ▲ @Reference                                 │ │
│  ┌───────────────────────────┴──────────────────────────────────────────┐ │
│  │ LAYER 1: EXTERNAL API PLUGINS (Separate Bundles)                     │ │
│  ├───────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  ┌─────────────────────────────────────────────────────────────┐    │ │
│  │  │ com.cloudempiere.graphql (GraphQL API Plugin)               │    │ │
│  │  ├─────────────────────────────────────────────────────────────┤    │ │
│  │  │                                                              │    │ │
│  │  │  GraphQL Schema (SDL):                                       │    │ │
│  │  │  ┌──────────────────────────────────────────────────────┐   │    │ │
│  │  │  │ type Query {                                          │   │    │ │
│  │  │  │   chat(                                               │   │    │ │
│  │  │  │     chatId: ID!,                                      │   │    │ │
│  │  │  │     message: String!                                  │   │    │ │
│  │  │  │   ): ChatResponse!                                    │   │    │ │
│  │  │  │                                                        │   │    │ │
│  │  │  │   chatHistory(                                        │   │    │ │
│  │  │  │     chatId: ID!,                                      │   │    │ │
│  │  │  │     limit: Int = 20                                   │   │    │ │
│  │  │  │   ): [ChatMessage!]!                                  │   │    │ │
│  │  │  │ }                                                      │   │    │ │
│  │  │  │                                                        │   │    │ │
│  │  │  │ type Subscription {                                   │   │    │ │
│  │  │  │   chatStreaming(                                      │   │    │ │
│  │  │  │     chatId: ID!,                                      │   │    │ │
│  │  │  │     message: String!                                  │   │    │ │
│  │  │  │   ): ChatStreamChunk!                                 │   │    │ │
│  │  │  │ }                                                      │   │    │ │
│  │  │  └──────────────────────────────────────────────────────┘   │    │ │
│  │  │                                                              │    │ │
│  │  │  Resolver Implementation:                                    │    │ │
│  │  │  • AIChatQueryResolver                                      │    │ │
│  │  │  • AIChatSubscriptionResolver (WebSocket)                   │    │ │
│  │  │                                                              │    │ │
│  │  │  Dependencies:                                               │    │ │
│  │  │  @Reference IAIChatFacade facade;  ◄─ From AI core          │    │ │
│  │  │  @Reference IAuthenticationService auth;  ◄─ From auth      │    │ │
│  │  │                                                              │    │ │
│  │  │  Flow:                                                       │    │ │
│  │  │  1. GraphQL request received (HTTP POST)                    │    │ │
│  │  │  2. Extract Authorization header (Bearer token)             │    │ │
│  │  │  3. Validate token via IAuthenticationService              │    │ │
│  │  │  4. Get UserContext (AD_User_ID, AD_Role_ID, AD_Client_ID) │    │ │
│  │  │  5. Call facade.chatStreaming(request, context, callback)  │    │ │
│  │  │  6. Stream chunks via GraphQL subscription (WebSocket)      │    │ │
│  │  │                                                              │    │ │
│  │  └──────────────────────────────────────────────────────────────    │ │
│  │                                                                   │ │
│  │  ┌─────────────────────────────────────────────────────────────┐    │ │
│  │  │ com.cloudempiere.rest (REST API Plugin)                     │    │ │
│  │  ├─────────────────────────────────────────────────────────────┤    │ │
│  │  │                                                              │    │ │
│  │  │  REST Endpoints (JAX-RS):                                    │    │ │
│  │  │  POST   /api/v1/chat/{chatId}/message                       │    │ │
│  │  │  GET    /api/v1/chat/{chatId}/history                       │    │ │
│  │  │  POST   /api/v1/chat/{chatId}/stream (SSE)                  │    │ │
│  │  │                                                              │    │ │
│  │  │  Resource Implementation:                                    │    │ │
│  │  │  @Path("/api/v1/chat")                                      │    │ │
│  │  │  public class AIChatResource {                              │    │ │
│  │  │                                                              │    │ │
│  │  │      @Reference                                             │    │ │
│  │  │      private volatile IAIChatFacade facade;                 │    │ │
│  │  │                                                              │    │ │
│  │  │      @Reference                                             │    │ │
│  │  │      private volatile IAuthenticationService auth;          │    │ │
│  │  │                                                              │    │ │
│  │  │      @POST                                                   │    │ │
│  │  │      @Path("/{chatId}/message")                             │    │ │
│  │  │      @Produces(MediaType.APPLICATION_JSON)                  │    │ │
│  │  │      public ChatResponse sendMessage(                       │    │ │
│  │  │          @HeaderParam("Authorization") String token,        │    │ │
│  │  │          @PathParam("chatId") String chatId,                │    │ │
│  │  │          ChatRequest request                                │    │ │
│  │  │      ) {                                                     │    │ │
│  │  │          UserContext ctx = auth.validateToken(token);       │    │ │
│  │  │          return facade.chatBlocking(request, ctx);          │    │ │
│  │  │      }                                                       │    │ │
│  │  │                                                              │    │ │
│  │  │      @POST                                                   │    │ │
│  │  │      @Path("/{chatId}/stream")                              │    │ │
│  │  │      @Produces(MediaType.SERVER_SENT_EVENTS)                │    │ │
│  │  │      public void streamMessage(                             │    │ │
│  │  │          @HeaderParam("Authorization") String token,        │    │ │
│  │  │          @PathParam("chatId") String chatId,                │    │ │
│  │  │          @Context SseEventSink eventSink,                   │    │ │
│  │  │          ChatRequest request                                │    │ │
│  │  │      ) {                                                     │    │ │
│  │  │          UserContext ctx = auth.validateToken(token);       │    │ │
│  │  │          facade.chatStreaming(request, ctx,                 │    │ │
│  │  │              chunk -> eventSink.send(                       │    │ │
│  │  │                  SseEvent.event("chunk", chunk)             │    │ │
│  │  │              )                                               │    │ │
│  │  │          );                                                  │    │ │
│  │  │      }                                                       │    │ │
│  │  │  }                                                           │    │ │
│  │  │                                                              │    │ │
│  │  └──────────────────────────────────────────────────────────────    │ │
│  │                                                                   │ │
│  │  ┌─────────────────────────────────────────────────────────────┐    │ │
│  │  │ com.cloudempiere.mcp (MCP Server Plugin)                    │    │ │
│  │  ├─────────────────────────────────────────────────────────────┤    │ │
│  │  │                                                              │    │ │
│  │  │  MCP Protocol (JSON-RPC):                                    │    │ │
│  │  │  • initialize                                               │    │ │
│  │  │  • tools/list                                                │    │ │
│  │  │  • tools/call                                                │    │ │
│  │  │  • resources/list                                            │    │ │
│  │  │  • resources/read                                            │    │ │
│  │  │  • prompts/list                                              │    │ │
│  │  │  • prompts/get                                               │    │ │
│  │  │                                                              │    │ │
│  │  │  Implementation:                                             │    │ │
│  │  │  public class MCPServer implements IMCPServerHandler {      │    │ │
│  │  │                                                              │    │ │
│  │  │      @Reference                                             │    │ │
│  │  │      private volatile IAIChatFacade facade;                 │    │ │
│  │  │                                                              │    │ │
│  │  │      @Reference                                             │    │ │
│  │  │      private volatile IAuthenticationService auth;          │    │ │
│  │  │                                                              │    │ │
│  │  │      @Override                                               │    │ │
│  │  │      public ToolsListResult listTools(                      │    │ │
│  │  │          AuthContext authCtx                                │    │ │
│  │  │      ) {                                                     │    │ │
│  │  │          // Return available AI tools                       │    │ │
│  │  │          return ToolsListResult.builder()                   │    │ │
│  │  │              .tools(Arrays.asList(                          │    │ │
│  │  │                  Tool.builder()                             │    │ │
│  │  │                      .name("chat")                          │    │ │
│  │  │                      .description("Chat with AI")           │    │ │
│  │  │                      .inputSchema(...)                      │    │ │
│  │  │                      .build()                               │    │ │
│  │  │              ))                                              │    │ │
│  │  │              .build();                                       │    │ │
│  │  │      }                                                       │    │ │
│  │  │                                                              │    │ │
│  │  │      @Override                                               │    │ │
│  │  │      public ToolCallResult callTool(                        │    │ │
│  │  │          String name,                                       │    │ │
│  │  │          Map<String, Object> arguments,                     │    │ │
│  │  │          AuthContext authCtx                                │    │ │
│  │  │      ) {                                                     │    │ │
│  │  │          if ("chat".equals(name)) {                         │    │ │
│  │  │              String chatId = (String) arguments.get("chatId");│   │ │
│  │  │              String message = (String) arguments.get("message");│  │ │
│  │  │              ChatRequest req = ChatRequest.builder()        │    │ │
│  │  │                  .chatId(chatId)                            │    │ │
│  │  │                  .message(message)                          │    │ │
│  │  │                  .build();                                   │    │ │
│  │  │              ChatResponse resp = facade.chatBlocking(       │    │ │
│  │  │                  req, authCtx.getUserContext()              │    │ │
│  │  │              );                                              │    │ │
│  │  │              return ToolCallResult.success(resp.getMessage());│   │ │
│  │  │          }                                                   │    │ │
│  │  │          throw new IllegalArgumentException("Unknown tool");│    │ │
│  │  │      }                                                       │    │ │
│  │  │  }                                                           │    │ │
│  │  │                                                              │    │ │
│  │  │  NOTE: MCP support requires LangChain4j 1.x (Java 17)       │    │ │
│  │  │  Status: Planned for Phase 2 (Q2 2026)                      │    │ │
│  │  │                                                              │    │ │
│  │  └──────────────────────────────────────────────────────────────    │ │
│  │                                                                   │ │
│  └───────────────────────────────────────────────────────────────────  │
│                              ▲ Import-Package                           │
│  ┌───────────────────────────┴──────────────────────────────────────────┐│
│  │ LAYER 2: AI CORE FACADE (Shared API Contract)                       ││
│  │ Bundle: com.cloudempiere.ai.core                                     ││
│  ├──────────────────────────────────────────────────────────────────────┤│
│  │                                                                       ││
│  │  IAIChatFacade (Exported Service Interface):                         ││
│  │  ┌──────────────────────────────────────────────────────────────┐   ││
│  │  │ package com.cloudempiere.ai.facade;                          │   ││
│  │  │                                                               │   ││
│  │  │ /**                                                           │   ││
│  │  │  * Protocol-agnostic AI chat facade for external consumption │   ││
│  │  │  * Implemented by: AIChatFacadeImpl.java                     │   ││
│  │  │  * Consumed by: GraphQL, REST, MCP plugins                   │   ││
│  │  │  */                                                           │   ││
│  │  │ public interface IAIChatFacade {                             │   ││
│  │  │                                                               │   ││
│  │  │     /**                                                       │   ││
│  │  │      * Blocking chat (returns full response)                 │   ││
│  │  │      */                                                       │   ││
│  │  │     ChatResponse chatBlocking(                               │   ││
│  │  │         ChatRequest request,                                 │   ││
│  │  │         UserContext context                                  │   ││
│  │  │     ) throws AIException;                                    │   ││
│  │  │                                                               │   ││
│  │  │     /**                                                       │   ││
│  │  │      * Streaming chat (callback per chunk)                   │   ││
│  │  │      */                                                       │   ││
│  │  │     String chatStreaming(                                    │   ││
│  │  │         ChatRequest request,                                 │   ││
│  │  │         UserContext context,                                 │   ││
│  │  │         StreamCallback callback                              │   ││
│  │  │     ) throws AIException;                                    │   ││
│  │  │                                                               │   ││
│  │  │     /**                                                       │   ││
│  │  │      * Get chat history                                      │   ││
│  │  │      */                                                       │   ││
│  │  │     List<MessageDTO> getHistory(                             │   ││
│  │  │         String chatId,                                       │   ││
│  │  │         UserContext context,                                 │   ││
│  │  │         int maxMessages                                      │   ││
│  │  │     ) throws AIException;                                    │   ││
│  │  │                                                               │   ││
│  │  │     /**                                                       │   ││
│  │  │      * Create new chat thread                                │   ││
│  │  │      */                                                       │   ││
│  │  │     String createThread(                                     │   ││
│  │  │         UserContext context,                                 │   ││
│  │  │         int providerId                                       │   ││
│  │  │     ) throws AIException;                                    │   ││
│  │  │ }                                                             │   ││
│  │  └──────────────────────────────────────────────────────────────┘   ││
│  │                                                                       ││
│  │  DTOs (Exported):                                                     ││
│  │  • ChatRequest - input DTO (chatId, message, provider)               ││
│  │  • ChatResponse - output DTO (message, tokens, cost)                 ││
│  │  • MessageDTO - history entry (role, content, timestamp)             ││
│  │  • UserContext - auth context (AD_User_ID, AD_Role_ID, AD_Client_ID)││
│  │  • StreamCallback - chunk callback interface                         ││
│  │                                                                       ││
│  │  Implementation:                                                      ││
│  │  • AIChatFacadeImpl.java (@Component service)                        ││
│  │      - Delegates to AIService.java (internal)                        ││
│  │      - Converts internal DTOs to facade DTOs                         ││
│  │      - Applies security context                                      ││
│  │                                                                       ││
│  │  Export-Package:                                                      ││
│  │  • com.cloudempiere.ai.facade;version="1.0.0"                        ││
│  │  • com.cloudempiere.ai.facade.dto;version="1.0.0"                    ││
│  │                                                                       ││
│  └───────────────────────────────────────────────────────────────────────┘│
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## Service Contract Specifications

### IAIChatFacade Interface

```java
package com.cloudempiere.ai.facade;

import com.cloudempiere.ai.facade.dto.*;
import java.util.List;

/**
 * Protocol-agnostic AI chat facade for external consumption.
 *
 * <p><b>Architecture Layer:</b> Facade (Layer 2 from LAYERED_ARCHITECTURE_DESIGN.md)
 *
 * <p><b>Consumers:</b>
 * <ul>
 *   <li>com.cloudempiere.graphql - GraphQL API plugin</li>
 *   <li>com.cloudempiere.rest - REST API plugin</li>
 *   <li>com.cloudempiere.mcp - MCP server plugin</li>
 * </ul>
 *
 * <p><b>Security:</b> All methods require {@link UserContext} with valid authentication.
 * Authorization is enforced via AD_Role_ID and AD_Client_ID filtering.
 *
 * <p><b>Related ADRs:</b>
 * <ul>
 *   <li><a href="../docs/adr/031-chat-panel-langchain4j-chatmodel-integration.md">ADR-031</a></li>
 *   <li><a href="../LAYERED_ARCHITECTURE_DESIGN.md">Layered Architecture Design</a></li>
 * </ul>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public interface IAIChatFacade {

    /**
     * Execute blocking chat request and return complete response.
     *
     * <p><b>Flow:</b>
     * <pre>
     * 1. Validate UserContext (AD_User_ID, AD_Role_ID, AD_Client_ID)
     * 2. Load chat thread (CM_Chat)
     * 3. Get AI provider (AIG_Provider)
     * 4. Build LangChain4j agent (AiServices.builder())
     * 5. Execute chat (agent.chat(message))
     * 6. Persist response (CM_ChatEntry)
     * 7. Return ChatResponse DTO
     * </pre>
     *
     * <p><b>Use Case:</b> REST API synchronous endpoint, GraphQL query
     *
     * @param request chat request (chatId, message, optional providerId)
     * @param context user authentication context
     * @return complete AI response with tokens and cost
     * @throws AIException if chat fails (provider error, validation, auth)
     */
    ChatResponse chatBlocking(ChatRequest request, UserContext context)
        throws AIException;

    /**
     * Execute streaming chat request with chunk-by-chunk callback.
     *
     * <p><b>Flow:</b>
     * <pre>
     * 1. Validate UserContext
     * 2. Load chat thread
     * 3. Get AI provider (must support streaming)
     * 4. Build agent
     * 5. Execute streaming chat:
     *    - On each chunk: callback.onChunk(chunk)
     *    - On error: callback.onError(error)
     *    - On complete: callback.onComplete(response)
     * 6. Persist final response
     * 7. Return response ID
     * </pre>
     *
     * <p><b>Use Case:</b> GraphQL subscription, REST SSE endpoint
     *
     * @param request chat request
     * @param context user authentication context
     * @param callback chunk callback handler
     * @return response ID (for tracking, full response available in callback)
     * @throws AIException if chat setup fails
     */
    String chatStreaming(
        ChatRequest request,
        UserContext context,
        StreamCallback callback
    ) throws AIException;

    /**
     * Retrieve chat message history.
     *
     * <p><b>Flow:</b>
     * <pre>
     * 1. Validate UserContext
     * 2. Query CM_ChatEntry table
     *    - Filter: CM_Chat_ID = request.chatId
     *    - Filter: AD_Client_ID = context.clientId (multi-tenant)
     *    - Order: Created DESC
     *    - Limit: maxMessages
     * 3. Convert to MessageDTO list
     * 4. Return history
     * </pre>
     *
     * <p><b>Security:</b> Only returns messages for chats owned by user or shared
     *
     * @param chatId chat thread ID
     * @param context user authentication context
     * @param maxMessages maximum messages to return (default: 20)
     * @return list of messages (newest first)
     * @throws AIException if chat not found or access denied
     */
    List<MessageDTO> getHistory(
        String chatId,
        UserContext context,
        int maxMessages
    ) throws AIException;

    /**
     * Create new chat thread.
     *
     * <p><b>Flow:</b>
     * <pre>
     * 1. Validate UserContext
     * 2. Create CM_Chat record:
     *    - AD_User_ID = context.userId
     *    - AD_Client_ID = context.clientId
     *    - AIG_Provider_ID = providerId (or default)
     * 3. Return chat ID
     * </pre>
     *
     * @param context user authentication context
     * @param providerId AI provider ID (if null, use default for user)
     * @return new chat thread ID
     * @throws AIException if creation fails
     */
    String createThread(UserContext context, int providerId)
        throws AIException;
}
```

### DTO Definitions

```java
package com.cloudempiere.ai.facade.dto;

/**
 * Chat request DTO for external API consumption.
 *
 * <p><b>JSON Example (REST API):</b>
 * <pre>{@code
 * {
 *   "chatId": "12345",
 *   "message": "What are my top sales opportunities?",
 *   "providerId": 1,  // optional, defaults to user's provider
 *   "options": {
 *     "temperature": 0.7,
 *     "maxTokens": 1000
 *   }
 * }
 * }</pre>
 *
 * <p><b>GraphQL Example:</b>
 * <pre>{@code
 * mutation {
 *   chat(
 *     chatId: "12345",
 *     message: "What are my top sales opportunities?"
 *   ) {
 *     message
 *     tokens { input output total }
 *     cost
 *   }
 * }
 * }</pre>
 */
public class ChatRequest {
    private String chatId;
    private String message;
    private Integer providerId;  // optional, null = use default
    private Map<String, Object> options;  // optional provider-specific params

    // Builder pattern
    public static Builder builder() { return new Builder(); }
}

/**
 * Chat response DTO.
 *
 * <p><b>JSON Response Example:</b>
 * <pre>{@code
 * {
 *   "responseId": "67890",
 *   "message": "Your top 3 opportunities are: ...",
 *   "tokens": {
 *     "input": 50,
 *     "output": 200,
 *     "total": 250
 *   },
 *   "cost": 0.0025,
 *   "provider": "anthropic-claude-3.5-sonnet",
 *   "timestamp": "2026-01-30T10:30:00Z"
 * }
 * }</pre>
 */
public class ChatResponse {
    private String responseId;
    private String message;
    private TokenUsageDTO tokens;
    private double cost;
    private String provider;
    private Instant timestamp;

    // Getters
}

/**
 * Message history entry DTO.
 */
public class MessageDTO {
    private String messageId;
    private String role;  // "user" or "assistant"
    private String content;  // HTML for AI messages, plain text for user
    private Instant timestamp;
    private TokenUsageDTO tokens;  // null for user messages

    // Getters
}

/**
 * User authentication context DTO.
 *
 * <p><b>Populated by:</b> com.cloudempiere.auth OAuth2 validation
 */
public class UserContext {
    private int userId;  // AD_User_ID
    private int roleId;  // AD_Role_ID
    private int clientId;  // AD_Client_ID
    private int orgId;  // AD_Org_ID
    private String[] scopes;  // OAuth2 scopes (e.g., ["chat:read", "chat:write"])

    // Getters
}

/**
 * Stream callback for chunk-by-chunk processing.
 */
public interface StreamCallback {
    /**
     * Called for each chunk from AI provider.
     *
     * @param chunk text chunk (may be partial word/sentence)
     */
    void onChunk(String chunk);

    /**
     * Called when streaming completes successfully.
     *
     * @param response final complete response with tokens/cost
     */
    void onComplete(ChatResponse response);

    /**
     * Called if streaming encounters error.
     *
     * @param error exception details
     */
    void onError(AIException error);
}
```

---

## Authentication Flow (OAuth2)

### Token Validation Sequence

```
External Client                com.cloudempiere.auth          External API Plugin
     │                                 │                              │
     │ 1. Request with Bearer token    │                              │
     ├─────────────────────────────────┼─────────────────────────────>│
     │    Authorization: Bearer eyJ... │                              │
     │                                 │                              │
     │                                 │  2. Extract token            │
     │                                 │<─────────────────────────────┤
     │                                 │                              │
     │                                 │  3. Validate JWT             │
     │                                 │  • Verify signature          │
     │                                 │  • Check expiration          │
     │                                 │  • Extract claims            │
     │                                 │                              │
     │                                 │  4. Query AD_User            │
     │                                 │  SELECT AD_User_ID,          │
     │                                 │         AD_Role_ID,          │
     │                                 │         AD_Client_ID         │
     │                                 │  FROM AD_User                │
     │                                 │  WHERE UserName = ?          │
     │                                 │                              │
     │                                 │  5. Return UserContext       │
     │                                 ├─────────────────────────────>│
     │                                 │  {                           │
     │                                 │    userId: 100,              │
     │                                 │    roleId: 50,               │
     │                                 │    clientId: 11,             │
     │                                 │    orgId: 0,                 │
     │                                 │    scopes: ["chat:read",     │
     │                                 │             "chat:write"]    │
     │                                 │  }                           │
     │                                 │                              │
     │                                 │  6. Call facade with context │
     │                                 │  facade.chatBlocking(        │
     │                                 │      request, userContext    │
     │                                 │  )                           │
     │                                 │                              │
     │                                 │                      ┌───────┴───────┐
     │                                 │                      │ AI Core       │
     │                                 │                      │ Applies:      │
     │                                 │                      │ • AD_Client_ID│
     │                                 │                      │ • AD_Role_ID  │
     │                                 │                      │ • AD_Org_ID   │
     │                                 │                      │ filtering     │
     │                                 │                      └───────┬───────┘
     │                                 │                              │
     │                                 │  7. Return response          │
     │                                 │<─────────────────────────────┤
     │                                 │                              │
     │ 8. Return JSON/GraphQL response │                              │
     │<────────────────────────────────┼──────────────────────────────┤
     │                                 │                              │
     ▼                                 ▼                              ▼
```

### OAuth2 Scopes

| Scope | Permission | Use Case |
|-------|-----------|----------|
| `chat:read` | Read chat history | GET /api/v1/chat/{id}/history |
| `chat:write` | Send messages | POST /api/v1/chat/{id}/message |
| `chat:admin` | Manage all chats | Admin dashboard |
| `provider:read` | List providers | Provider selection UI |
| `provider:write` | Configure providers | Admin settings |

---

## OSGi Service Dependencies

### com.cloudempiere.graphql Dependencies

**MANIFEST.MF:**
```manifest
Bundle-SymbolicName: com.cloudempiere.graphql
Bundle-Version: 1.0.0.qualifier
Import-Package: com.cloudempiere.ai.facade;version="[1.0.0,2.0.0)",
 com.cloudempiere.ai.facade.dto;version="[1.0.0,2.0.0)",
 com.cloudempiere.auth;version="[1.0.0,2.0.0)",
 graphql.schema,
 graphql.execution
```

**Service References:**
```java
@Component
public class AIChatQueryResolver {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private volatile IAIChatFacade facade;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private volatile IAuthenticationService auth;

    // GraphQL resolver methods
}
```

### com.cloudempiere.rest Dependencies

**MANIFEST.MF:**
```manifest
Bundle-SymbolicName: com.cloudempiere.rest
Bundle-Version: 1.0.0.qualifier
Import-Package: com.cloudempiere.ai.facade;version="[1.0.0,2.0.0)",
 com.cloudempiere.ai.facade.dto;version="[1.0.0,2.0.0)",
 com.cloudempiere.auth;version="[1.0.0,2.0.0)",
 javax.ws.rs,
 javax.ws.rs.sse
```

**Service References:**
```java
@Path("/api/v1/chat")
@Component
public class AIChatResource {

    @Reference
    private volatile IAIChatFacade facade;

    @Reference
    private volatile IAuthenticationService auth;

    // JAX-RS endpoint methods
}
```

### com.cloudempiere.mcp Dependencies

**MANIFEST.MF:**
```manifest
Bundle-SymbolicName: com.cloudempiere.mcp
Bundle-Version: 1.0.0.qualifier
Import-Package: com.cloudempiere.ai.facade;version="[1.0.0,2.0.0)",
 com.cloudempiere.ai.facade.dto;version="[1.0.0,2.0.0)",
 com.cloudempiere.auth;version="[1.0.0,2.0.0)",
 dev.langchain4j.mcp;version="[1.0.0,2.0.0)"
```

**NOTE:** MCP requires LangChain4j 1.x (Java 17), blocked until Phase 2.

---

## P2 Repository Structure

```
cloudempiere-p2/
├── v10/
│   ├── latest/
│   │   ├── compositeContent.xml
│   │   └── compositeArtifacts.xml
│   │
│   ├── ai/                                    (AI core plugins)
│   │   ├── deps/0.35.0/
│   │   ├── core/1.0.1/
│   │   └── domains/
│   │       ├── sales/1.0.0/
│   │       ├── inventory/1.0.0/
│   │       └── ...
│   │
│   ├── api/                                   (External API plugins)
│   │   ├── graphql/1.0.0/repository/
│   │   │   └── plugins/
│   │   │       └── com.cloudempiere.graphql_1.0.0.jar
│   │   │
│   │   └── rest/1.0.0/repository/
│   │       └── plugins/
│   │           └── com.cloudempiere.rest_1.0.0.jar
│   │
│   └── auth/
│       └── oauth2/1.0.0/repository/
│           └── plugins/
│               └── com.cloudempiere.auth_1.0.0.jar
│
└── v11/                                       (Java 17 + LangChain4j 1.x)
    └── api/
        └── mcp/1.0.0/repository/
            └── plugins/
                └── com.cloudempiere.mcp_1.0.0.jar
```

### Composite Repository (v10/latest)

**compositeContent.xml:**
```xml
<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='Cloudempiere v10 Latest'
    type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository'
    version='1.0.0'>
  <children size='9'>
    <!-- AI Core -->
    <child location='../ai/deps/0.35.0'/>
    <child location='../ai/core/1.0.1'/>
    <child location='../ai/domains/sales/1.0.0'/>
    <child location='../ai/domains/inventory/1.0.0'/>
    <child location='../ai/domains/knowledge/1.0.0'/>

    <!-- External APIs -->
    <child location='../api/graphql/1.0.0'/>
    <child location='../api/rest/1.0.0'/>

    <!-- Authentication -->
    <child location='../auth/oauth2/1.0.0'/>
  </children>
</repository>
```

---

## Deployment Scenarios

### Scenario 1: Internal ZK UI Only

**Installed Plugins:**
- com.cloudempiere.ai.deps
- com.cloudempiere.ai.core
- com.cloudempiere.ai.sales
- com.cloudempiere.ai.knowledge

**NOT Installed:**
- com.cloudempiere.graphql
- com.cloudempiere.rest
- com.cloudempiere.auth

**Result:** AI chat available in ZK UI, no external API access

---

### Scenario 2: Internal + GraphQL API

**Installed Plugins:**
- com.cloudempiere.ai.deps
- com.cloudempiere.ai.core
- com.cloudempiere.ai.sales
- com.cloudempiere.ai.knowledge
- **com.cloudempiere.auth** ← OAuth2 provider
- **com.cloudempiere.graphql** ← GraphQL API

**Exposed Endpoints:**
- GraphQL: http://server:8080/graphql
- GraphQL Playground: http://server:8080/graphql-ui

**Authentication:** OAuth2 Bearer tokens

---

### Scenario 3: Full External API Suite

**Installed Plugins:**
- All AI core plugins
- com.cloudempiere.auth
- com.cloudempiere.graphql
- com.cloudempiere.rest

**Exposed Endpoints:**
- GraphQL: http://server:8080/graphql
- REST: http://server:8080/api/v1/chat
- REST SSE: http://server:8080/api/v1/chat/{id}/stream

**MCP:** Not available until Java 17 (v11 branch)

---

## Migration Roadmap

### Phase 1: Core + Auth (Q1 2026)

**Week 1-2:** Extract IAIChatFacade from AIService
- Create facade package in com.cloudempiere.ai.core
- Define DTOs (ChatRequest, ChatResponse, UserContext)
- Implement AIChatFacadeImpl as @Component service
- Export facade API packages

**Week 3:** Create com.cloudempiere.auth plugin
- Implement OAuth2 JWT validation
- Integrate with iDempiere AD_User table
- Export IAuthenticationService

**Week 4:** Testing
- Unit tests for facade
- OAuth2 token validation tests
- Integration tests

---

### Phase 2: GraphQL API (Q1 2026)

**Week 5-6:** Create com.cloudempiere.graphql plugin
- Define GraphQL schema (SDL)
- Implement Query/Mutation resolvers
- Implement Subscription resolver (WebSocket streaming)
- @Reference inject IAIChatFacade

**Week 7:** Testing
- GraphQL query/mutation tests
- WebSocket subscription tests
- Load testing (100 concurrent users)

---

### Phase 3: REST API (Q2 2026)

**Week 8-9:** Create com.cloudempiere.rest plugin
- JAX-RS resource endpoints
- SSE streaming endpoint
- Swagger/OpenAPI documentation
- @Reference inject IAIChatFacade

**Week 10:** Testing
- REST endpoint tests
- SSE streaming tests
- API documentation validation

---

### Phase 4: MCP Server (Q2 2026 - After Java 17)

**Prerequisite:** Java 17 migration + iDempiere v11

**Week 11-12:** Create com.cloudempiere.mcp plugin
- Implement MCP protocol (JSON-RPC)
- Tool/Resource/Prompt registration
- @Reference inject IAIChatFacade

**Week 13:** Testing
- MCP client integration tests (Claude Desktop, Zed)
- Tool invocation tests

---

## Related Documents

- [LAYERED_ARCHITECTURE_DESIGN.md](LAYERED_ARCHITECTURE_DESIGN.md) - Five-layer facade architecture
- [OSGI_MULTI_PLUGIN_ARCHITECTURE.md](OSGI_MULTI_PLUGIN_ARCHITECTURE.md) - Multi-plugin domain separation
- [ADR-031: Chat Panel LangChain4j Integration](docs/adr/031-chat-panel-langchain4j-chatmodel-integration.md)
- [ADR-049: MCP Client External Tools Integration](docs/adr/049-mcp-client-external-tools-integration.md)

---

**Document Status:** ✅ Complete
**Implementation Status:** 📋 Planned
**Dependencies:** com.cloudempiere.auth (external), Phase 2 for MCP (Java 17)
