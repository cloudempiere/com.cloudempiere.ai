# MCP Server Implementation Guide for iDempiere AI Plugin

## Quick Start

This guide provides step-by-step instructions for implementing an MCP server that wraps the iDempiere AI Plugin.

## Phase 1: HTTP API Layer (iDempiere)

### 1.1 Create REST API Controller

Create a new package: `com.cloudempiere.ai.http`

**File**: `src/com/cloudempiere/ai/http/AIRestController.java`

```java
package com.cloudempiere.ai.http;

import com.cloudempiere.ai.provider.dto.*;
import com.cloudempiere.ai.service.AIConversationService;
import com.cloudempiere.ai.context.AIContextProviderRegistry;
import org.adempiere.base.Core;
import org.compiere.model.MChat;
import org.compiere.model.MUser;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API for AI Plugin operations
 * Accessible by: /api/ai/*
 *
 * Provides HTTP endpoints for:
 * - Chat with context
 * - Database queries
 * - Context extraction
 * - Provider information
 * - Conversation history
 */
public class AIRestController {

    private static final String API_VERSION = "1.0";
    private static final int DEFAULT_HISTORY_DEPTH = 10;
    private static final int MAX_HISTORY_DEPTH = 50;

    // Thread-local Properties context
    private static final ThreadLocal<Properties> contextHolder = new ThreadLocal<>();

    // Session management (simple in-memory; use DB for production)
    private static final Map<String, AISessionContext> sessions =
        new ConcurrentHashMap<>();

    /**
     * POST /api/ai/chat
     * Send message with automatic context extraction
     *
     * Request:
     * {
     *   "session_id": "uuid",          // Optional: existing conversation
     *   "message": "Show active orders",
     *   "ad_user_id": 100,             // Required: iDempiere user
     *   "ad_org_id": 11,               // Required: iDempiere org
     *   "provider": "ANTHROPIC",       // Optional: default from config
     *   "window_id": 143,              // Optional: for window context
     *   "include_history": true,       // Optional: default true
     *   "history_depth": 10,           // Optional: max messages to include
     *   "context_data": {}             // Optional: explicit context override
     * }
     *
     * Response:
     * {
     *   "session_id": "uuid",
     *   "message_id": "uuid",
     *   "response": "...",
     *   "function_calls": [...],
     *   "context_used": {
     *     "source": "HYBRID",
     *     "keys": ["current_window", "cached_result"]
     *   },
     *   "tokens_used": {
     *     "input": 150,
     *     "output": 450
     *   },
     *   "timestamp": "2025-11-26T10:30:00Z"
     * }
     */
    public Response chatWithContext(Request req) {
        try {
            // 1. Validate and initialize context
            Properties ctx = initializeContext(req.getAdUserId(), req.getAdOrgId());
            contextHolder.set(ctx);

            // 2. Get or create session
            AISessionContext session = getOrCreateSession(
                req.getSessionId(),
                req.getAdUserId(),
                req.getAdOrgId()
            );

            // 3. Extract context if needed
            Map<String, Object> contextData = req.getContextData();
            if (req.isIncludeHistory() && req.getWindowId() != null) {
                Map<String, Object> windowContext =
                    AIContextProviderRegistry.extractWindowContext(
                        ctx,
                        req.getWindowId()
                    );
                contextData.putAll(windowContext);
            }

            // 4. Load conversation history
            AIConversationService service =
                getAIConversationService();

            List<AIMessage> history = loadMessageHistory(
                session.getChatId(),
                Math.min(req.getHistoryDepth(), MAX_HISTORY_DEPTH)
            );

            // 5. Build request
            AIRequest aiRequest = AIRequest.builder()
                .messages(history)
                .addUserMessage(req.getMessage())
                .provider(req.getProvider())
                .contextData(contextData)
                .sessionId(session.getSessionId())
                .sessionThreadId(Thread.currentThread().getId())
                .build();

            // 6. Call AI service
            AIResponse response = service.sendMessageWithContext(
                ctx,
                session.getChat(),
                req.getMessage(),
                contextData,
                Math.min(req.getHistoryDepth(), MAX_HISTORY_DEPTH),
                Thread.currentThread().getId(),
                null  // trxName
            );

            // 7. Save to conversation history
            saveMessageToHistory(
                session.getChatId(),
                req.getMessage(),
                response,
                req.getAdUserId()
            );

            // 8. Return formatted response
            return new Response.Builder()
                .sessionId(session.getSessionId())
                .response(response.getContent())
                .functionCalls(response.getFunctionCalls())
                .tokensUsed(response.getTokenUsage())
                .timestamp(new Date())
                .build();

        } catch (Exception e) {
            return errorResponse(e);
        } finally {
            contextHolder.remove();
        }
    }

    /**
     * POST /api/ai/query
     * Execute database query with security enforcement
     *
     * Request:
     * {
     *   "sql": "SELECT Name FROM M_Product WHERE IsActive='Y'",
     *   "purpose": "List products for order",
     *   "max_rows": 50,
     *   "timeout_seconds": 5,
     *   "ad_user_id": 100,
     *   "ad_org_id": 11
     * }
     *
     * Response:
     * {
     *   "rows": [
     *     {"Name": "Product A"},
     *     {"Name": "Product B"}
     *   ],
     *   "row_count": 2,
     *   "columns": ["Name"],
     *   "audit_id": "AUG_QueryAudit_ID",
     *   "execution_time_ms": 245
     * }
     */
    public Response queryDatabase(QueryRequest req) {
        try {
            Properties ctx = initializeContext(req.getAdUserId(), req.getAdOrgId());
            contextHolder.set(ctx);

            SecureQueryRequest secureReq = SecureQueryRequest.builder()
                .sql(req.getSql())
                .purpose(req.getPurpose())
                .maxRows(req.getMaxRows())
                .timeoutSeconds(req.getTimeoutSeconds())
                .aiUserId(getAIUserId(ctx))
                .executingUserId(req.getAdUserId())
                .build();

            SecureQueryResult result = SecureDatabaseQueryExecutor.execute(
                ctx,
                secureReq
            );

            return new Response.Builder()
                .queryResult(result)
                .auditId(result.getAuditId())
                .executionTimeMs(result.getExecutionTimeMs())
                .timestamp(new Date())
                .build();

        } catch (Exception e) {
            return errorResponse(e);
        } finally {
            contextHolder.remove();
        }
    }

    /**
     * POST /api/ai/context
     * Extract context from specific window/entity
     *
     * Request:
     * {
     *   "context_type": "WINDOW",  // WINDOW, CHART, PROCESS
     *   "window_id": 143,
     *   "ad_user_id": 100,
     *   "ad_org_id": 11
     * }
     *
     * Response:
     * {
     *   "user_context": { ... },
     *   "window_metadata": { ... },
     *   "tab_context": { ... },
     *   "record_data": { ... },
     *   "extracted_at": "2025-11-26T10:30:00Z"
     * }
     */
    public Response extractContext(ContextRequest req) {
        try {
            Properties ctx = initializeContext(req.getAdUserId(), req.getAdOrgId());
            contextHolder.set(ctx);

            Map<String, Object> context = null;

            if ("WINDOW".equals(req.getContextType())) {
                context = AIContextProviderRegistry.extractWindowContext(
                    ctx,
                    req.getWindowId()
                );
            } else if ("CHART".equals(req.getContextType())) {
                context = AIContextProviderRegistry.extractChartContext(
                    ctx,
                    req.getChartId()
                );
            }

            return new Response.Builder()
                .context(context)
                .timestamp(new Date())
                .build();

        } catch (Exception e) {
            return errorResponse(e);
        } finally {
            contextHolder.remove();
        }
    }

    /**
     * GET /api/ai/provider/info
     * Get available providers and their capabilities
     *
     * Response:
     * {
     *   "providers": [
     *     {
     *       "name": "ANTHROPIC",
     *       "available_models": ["claude-3-5-sonnet", ...],
     *       "capabilities": {
     *         "text_generation": true,
     *         "function_calling": true,
     *         "vision": true,
     *         "embeddings": false
     *       },
     *       "health": "HEALTHY",
     *       "rate_limit": {
     *         "remaining": 4950,
     *         "reset_at": "2025-11-26T11:00:00Z"
     *       }
     *     }
     *   ]
     * }
     */
    public Response getProviderInfo() {
        try {
            List<Map<String, Object>> providers = new ArrayList<>();

            for (IAIProvider provider : AIProviderFactory.getAllProviders()) {
                providers.add(Map.ofEntries(
                    Map.entry("name", provider.getProviderName()),
                    Map.entry("available_models", provider.getAvailableModels()),
                    Map.entry("capabilities", provider.getCapabilities()),
                    Map.entry("health", provider.checkHealth().getStatus()),
                    Map.entry("rate_limit", provider.getRateLimitStatus())
                ));
            }

            return new Response.Builder()
                .providers(providers)
                .timestamp(new Date())
                .build();

        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    /**
     * GET /api/ai/conversation/{sessionId}/history
     * Get conversation history
     *
     * Query params:
     * - limit: max entries to return (default: 10, max: 50)
     * - offset: pagination offset (default: 0)
     *
     * Response:
     * {
     *   "session_id": "uuid",
     *   "messages": [
     *     {
     *       "message_id": "uuid",
     *       "role": "user",
     *       "content": "Show orders",
     *       "created_at": "2025-11-26T10:00:00Z"
     *     },
     *     {
     *       "message_id": "uuid",
     *       "role": "assistant",
     *       "content": "Here are your orders...",
     *       "function_calls": [...],
     *       "created_at": "2025-11-26T10:01:00Z"
     *     }
     *   ],
     *   "total_count": 25,
     *   "has_more": true
     * }
     */
    public Response getConversationHistory(String sessionId, int limit, int offset) {
        try {
            AISessionContext session = sessions.get(sessionId);
            if (session == null) {
                return errorResponse(new IllegalArgumentException("Session not found"));
            }

            List<AIMessage> history = loadMessageHistory(
                session.getChatId(),
                Math.min(limit, MAX_HISTORY_DEPTH)
            );

            return new Response.Builder()
                .sessionId(sessionId)
                .messages(history)
                .offset(offset)
                .limit(limit)
                .timestamp(new Date())
                .build();

        } catch (Exception e) {
            return errorResponse(e);
        }
    }

    // ============= PRIVATE HELPER METHODS =============

    private Properties initializeContext(int adUserId, int adOrgId) {
        Properties ctx = new Properties();
        Env.setContext(ctx, "#AD_Client_ID", 0);
        Env.setContext(ctx, "#AD_Org_ID", adOrgId);
        Env.setContext(ctx, "#AD_User_ID", adUserId);
        Env.setContext(ctx, "#AD_Language", getLanguageCode(adUserId));
        return ctx;
    }

    private AISessionContext getOrCreateSession(
        String sessionId,
        int adUserId,
        int adOrgId)
    {
        if (sessionId != null) {
            return sessions.get(sessionId);
        }

        String newSessionId = UUID.randomUUID().toString();
        MChat chat = new MChat(new Properties(), 0);
        chat.setAD_Org_ID(adOrgId);
        chat.setAD_User_ID(adUserId);
        chat.setSubject("MCP Session: " + newSessionId);
        chat.setType("AI");
        chat.save();

        AISessionContext context = new AISessionContext(
            newSessionId,
            chat.getCM_Chat_ID(),
            adUserId,
            adOrgId
        );
        sessions.put(newSessionId, context);

        return context;
    }

    private List<AIMessage> loadMessageHistory(int chatId, int depth) {
        List<AIMessage> messages = new ArrayList<>();
        // Query CM_ChatEntry ordered by descending created, limit to depth
        // Convert to AIMessage objects
        return messages;
    }

    private void saveMessageToHistory(
        int chatId,
        String userMessage,
        AIResponse response,
        int adUserId)
    {
        // Save user message to CM_ChatEntry
        // Save AI response to CM_ChatEntry
    }

    private AIConversationService getAIConversationService() {
        return (AIConversationService)
            Core.getApplicationContext()
                .getBean("aiConversationService");
    }

    private int getAIUserId(Properties ctx) {
        // Get configured AI User ID from AIG_Provider
        return 0;
    }

    private String getLanguageCode(int adUserId) {
        MUser user = new MUser(new Properties(), adUserId);
        return user.getAD_Language();
    }

    private Response errorResponse(Exception e) {
        return new Response.Builder()
            .error(true)
            .errorMessage(e.getMessage())
            .errorCode(e.getClass().getSimpleName())
            .timestamp(new Date())
            .build();
    }

    // ============= REQUEST/RESPONSE CLASSES =============

    public static class Request {
        private String sessionId;
        private String message;
        private int adUserId;
        private int adOrgId;
        private String provider;
        private Integer windowId;
        private boolean includeHistory = true;
        private int historyDepth = 10;
        private Map<String, Object> contextData = new HashMap<>();

        // Getters and setters...
    }

    public static class QueryRequest {
        private String sql;
        private String purpose;
        private int maxRows = 50;
        private int timeoutSeconds = 5;
        private int adUserId;
        private int adOrgId;

        // Getters and setters...
    }

    public static class ContextRequest {
        private String contextType;  // WINDOW, CHART, PROCESS
        private Integer windowId;
        private Integer chartId;
        private int adUserId;
        private int adOrgId;

        // Getters and setters...
    }

    public static class Response {
        // Builder pattern...
    }

    private static class AISessionContext {
        private final String sessionId;
        private final int chatId;
        private final int adUserId;
        private final int adOrgId;
        private final MChat chat;
        private final long createdAt;

        public AISessionContext(String sessionId, int chatId, int adUserId, int adOrgId) {
            this.sessionId = sessionId;
            this.chatId = chatId;
            this.adUserId = adUserId;
            this.adOrgId = adOrgId;
            this.createdAt = System.currentTimeMillis();
            this.chat = new MChat(new Properties(), chatId);
        }

        // Getters...
    }
}
```

### 1.2 Register REST Controller as OSGi Service

**File**: `OSGI-INF/ai-rest-api.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://osgi.org/xmlns/scr/v1.1.0" name="AIRestAPIComponent">
    <service>
        <provide interface="org.osgi.service.servlet.Servlet"/>
    </service>
    <property name="osgi.http.whiteboard.servlet.pattern" value="/api/ai/*"/>
    <property name="service.description" value="iDempiere AI REST API"/>
</scr:component>
```

### 1.3 Add pom.xml Dependencies

```xml
<!-- JAX-RS for REST support -->
<dependency>
    <groupId>javax.ws.rs</groupId>
    <artifactId>javax.ws.rs-api</artifactId>
    <version>2.1.1</version>
</dependency>

<!-- JSON processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.0</version>
</dependency>
```

## Phase 2: MCP Server Implementation

### 2.1 Choose Implementation Language

#### Option A: Java-Based (Recommended)

**Pros**:
- Direct access to iDempiere models
- Share DTO classes with plugin
- Single codebase

**Cons**:
- MCP SDK support less mature
- Requires custom transport

**Recommendation**: Implement custom StdIO transport

#### Option B: Node.js/TypeScript (Alternative)

**Pros**:
- Official MCP SDK with full support
- JavaScript ecosystem
- Lightweight

**Cons**:
- Need HTTP adapter to call iDempiere API
- Extra process to manage

**Recommendation**: Use if team prefers JavaScript

### 2.2 Node.js/TypeScript Implementation (Recommended)

**Project structure**:

```
mcp-server/
├── src/
│   ├── index.ts              # Main server entry point
│   ├── tools/
│   │   ├── chatTool.ts       # Chat with context
│   │   ├── queryTool.ts      # Database queries
│   │   ├── contextTool.ts    # Context extraction
│   │   ├── providerTool.ts   # Provider info
│   │   └── historyTool.ts    # Conversation history
│   ├── api/
│   │   ├── client.ts         # HTTP client for iDempiere API
│   │   └── types.ts          # TypeScript interfaces
│   ├── utils/
│   │   ├── validation.ts     # Input validation (Zod)
│   │   ├── formatter.ts      # Response formatting
│   │   └── logger.ts         # Logging
│   └── schemas/
│       └── tools.ts          # Zod schemas for tools
├── package.json
├── tsconfig.json
└── README.md
```

#### 2.2.1 Create package.json

```json
{
  "name": "idempiere-ai-mcp-server",
  "version": "1.0.0",
  "description": "MCP server for iDempiere AI Plugin",
  "main": "dist/index.js",
  "scripts": {
    "build": "tsc",
    "start": "node dist/index.js",
    "dev": "ts-node src/index.ts",
    "test": "jest",
    "lint": "eslint src/**/*.ts"
  },
  "dependencies": {
    "@modelcontextprotocol/sdk": "^1.0.0",
    "axios": "^1.6.0",
    "zod": "^3.22.0",
    "pino": "^8.17.0"
  },
  "devDependencies": {
    "@types/node": "^20.0.0",
    "typescript": "^5.3.0",
    "ts-node": "^10.9.0",
    "@typescript-eslint/eslint-plugin": "^6.0.0",
    "@typescript-eslint/parser": "^6.0.0",
    "jest": "^29.0.0",
    "ts-jest": "^29.0.0"
  },
  "engines": {
    "node": ">=18.0.0"
  }
}
```

#### 2.2.2 Create tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "moduleResolution": "node"
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

#### 2.2.3 Create Main Server (src/index.ts)

```typescript
import { Server } from "@modelcontextprotocol/sdk/server/stdio";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ToolSchema,
} from "@modelcontextprotocol/sdk/types";
import { logger } from "./utils/logger";
import { createChatTool, createQueryTool, createContextTool,
         createProviderTool, createHistoryTool } from "./tools";

// Initialize MCP Server
const transport = new StdioServerTransport();
const server = new Server({
  name: "idempiere-ai",
  version: "1.0.0",
}, {
  capabilities: {
    tools: {},
  },
});

// Register tools
const tools: ToolSchema[] = [
  createChatTool(),
  createQueryTool(),
  createContextTool(),
  createProviderTool(),
  createHistoryTool(),
];

// Implement ListTools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools,
}));

// Implement CallTool
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  logger.info(`Calling tool: ${request.params.name}`);

  switch (request.params.name) {
    case "chat_with_context":
      return await createChatTool().handler(request.params.arguments);
    case "query_database":
      return await createQueryTool().handler(request.params.arguments);
    case "extract_context":
      return await createContextTool().handler(request.params.arguments);
    case "get_provider_info":
      return await createProviderTool().handler(request.params.arguments);
    case "get_conversation_history":
      return await createHistoryTool().handler(request.params.arguments);
    default:
      throw new Error(`Unknown tool: ${request.params.name}`);
  }
});

// Connect and run
async function main() {
  logger.info("Starting iDempiere AI MCP Server");

  try {
    await server.connect(transport);
    logger.info("Server connected and ready for requests");
  } catch (error) {
    logger.error("Failed to start server:", error);
    process.exit(1);
  }
}

main();
```

#### 2.2.4 Create Tool Implementations

**File**: `src/tools/chatTool.ts`

```typescript
import { z } from "zod";
import { ToolSchema, TextContent, ToolResultBlockSchema } from "@modelcontextprotocol/sdk/types";
import { apiClient } from "../api/client";
import { logger } from "../utils/logger";

const ChatInputSchema = z.object({
  session_id: z.string().optional().describe("Existing session ID"),
  message: z.string().min(1).describe("User message to send"),
  ad_user_id: z.number().int().positive().describe("iDempiere user ID"),
  ad_org_id: z.number().int().positive().describe("iDempiere org ID"),
  provider: z.string().optional().describe("AI provider (ANTHROPIC, BEDROCK)"),
  window_id: z.number().int().optional().describe("Window ID for context"),
  include_history: z.boolean().default(true).describe("Include conversation history"),
  history_depth: z.number().int().min(1).max(50).default(10)
    .describe("Number of history messages to include"),
  context_data: z.record(z.any()).optional().describe("Explicit context override"),
}).strict();

export function createChatTool(): ToolSchema {
  return {
    name: "chat_with_context",
    description: "Send a message to the AI with automatic context extraction from iDempiere. "
      + "The AI can access database, window context, and conversation history. "
      + "Supports follow-up questions with context awareness.",
    inputSchema: {
      type: "object",
      properties: {
        session_id: {
          type: "string",
          description: "Existing session ID for continuing conversation",
        },
        message: {
          type: "string",
          description: "Your message or question",
        },
        ad_user_id: {
          type: "number",
          description: "iDempiere user ID performing the action",
        },
        ad_org_id: {
          type: "number",
          description: "iDempiere organization ID",
        },
        provider: {
          type: "string",
          enum: ["ANTHROPIC", "BEDROCK"],
          description: "AI provider to use",
        },
        window_id: {
          type: "number",
          description: "iDempiere window ID for context (e.g., 143 for Orders)",
        },
        include_history: {
          type: "boolean",
          description: "Include conversation history in context",
        },
        history_depth: {
          type: "number",
          description: "How many previous messages to include",
        },
      },
      required: ["message", "ad_user_id", "ad_org_id"],
    },
    handler: async (args: unknown): Promise<ToolResultBlockSchema[]> => {
      const input = ChatInputSchema.parse(args);

      try {
        logger.info("Chat request", {
          sessionId: input.session_id,
          userId: input.ad_user_id,
          message: input.message.substring(0, 50),
        });

        const response = await apiClient.post("/chat", {
          session_id: input.session_id,
          message: input.message,
          ad_user_id: input.ad_user_id,
          ad_org_id: input.ad_org_id,
          provider: input.provider,
          window_id: input.window_id,
          include_history: input.include_history,
          history_depth: input.history_depth,
          context_data: input.context_data,
        });

        const result = response.data;

        // Format response with session info and token usage
        let content = `**Response** (Session: ${result.session_id})\n\n`;
        content += result.response;

        if (result.function_calls && result.function_calls.length > 0) {
          content += "\n\n**Function Calls Made**:\n";
          result.function_calls.forEach((call: any, idx: number) => {
            content += `${idx + 1}. ${call.name}\n`;
          });
        }

        if (result.tokens_used) {
          content += `\n**Tokens Used**: ${result.tokens_used.input} input, ${result.tokens_used.output} output`;
        }

        return [{
          type: "text",
          text: content,
        }];

      } catch (error) {
        logger.error("Chat tool error", error);

        const errorMsg = error instanceof Error ? error.message : "Unknown error";
        return [{
          type: "text",
          text: `Error: ${errorMsg}`,
          isError: true,
        }];
      }
    },
  };
}
```

**File**: `src/tools/queryTool.ts`

```typescript
import { z } from "zod";
import { ToolSchema, ToolResultBlockSchema } from "@modelcontextprotocol/sdk/types";
import { apiClient } from "../api/client";
import { logger } from "../utils/logger";
import { formatAsMarkdownTable } from "../utils/formatter";

const QueryInputSchema = z.object({
  sql: z.string()
    .describe("SELECT query to execute (must be SELECT only)"),
  purpose: z.string()
    .describe("Why this query is needed (for audit purposes)"),
  ad_user_id: z.number().int().positive()
    .describe("iDempiere user ID"),
  ad_org_id: z.number().int().positive()
    .describe("iDempiere org ID"),
  max_rows: z.number().int().min(1).max(100).default(50)
    .describe("Maximum rows to return"),
  timeout_seconds: z.number().int().min(1).max(30).default(5)
    .describe("Query timeout in seconds"),
}).strict();

export function createQueryTool(): ToolSchema {
  return {
    name: "query_database",
    description: "Execute a SELECT query against iDempiere database with row-level security. "
      + "Query executes with the user's permissions - cannot access data they don't have access to. "
      + "All queries are logged for audit purposes.",
    inputSchema: {
      type: "object",
      properties: {
        sql: {
          type: "string",
          description: "SELECT query (no INSERT/UPDATE/DELETE)",
        },
        purpose: {
          type: "string",
          description: "Reason for query (e.g., 'Find active orders for Acme Corp')",
        },
        ad_user_id: {
          type: "number",
          description: "iDempiere user ID",
        },
        ad_org_id: {
          type: "number",
          description: "iDempiere organization ID",
        },
        max_rows: {
          type: "number",
          description: "Maximum rows to return (default: 50)",
        },
        timeout_seconds: {
          type: "number",
          description: "Query timeout (default: 5)",
        },
      },
      required: ["sql", "purpose", "ad_user_id", "ad_org_id"],
    },
    handler: async (args: unknown): Promise<ToolResultBlockSchema[]> => {
      const input = QueryInputSchema.parse(args);

      try {
        logger.info("Query request", {
          purpose: input.purpose,
          userId: input.ad_user_id,
          sqlLength: input.sql.length,
        });

        const response = await apiClient.post("/query", {
          sql: input.sql,
          purpose: input.purpose,
          ad_user_id: input.ad_user_id,
          ad_org_id: input.ad_org_id,
          max_rows: input.max_rows,
          timeout_seconds: input.timeout_seconds,
        });

        const result = response.data;

        if (!result.rows || result.rows.length === 0) {
          return [{
            type: "text",
            text: "No rows found matching the query.",
          }];
        }

        // Format as markdown table
        const table = formatAsMarkdownTable(
          result.columns,
          result.rows,
          100  // char limit per cell
        );

        let content = `**Query Results** (${result.row_count} rows)\n\n${table}`;

        if (result.audit_id) {
          content += `\n\n*Audit ID: ${result.audit_id} | Execution: ${result.execution_time_ms}ms*`;
        }

        return [{
          type: "text",
          text: content,
        }];

      } catch (error) {
        logger.error("Query tool error", error);

        const errorMsg = error instanceof Error ? error.message : "Unknown error";
        return [{
          type: "text",
          text: `Error executing query: ${errorMsg}`,
          isError: true,
        }];
      }
    },
  };
}
```

**File**: `src/tools/contextTool.ts`

```typescript
import { z } from "zod";
import { ToolSchema, ToolResultBlockSchema } from "@modelcontextprotocol/sdk/types";
import { apiClient } from "../api/client";
import { logger } from "../utils/logger";

const ContextInputSchema = z.object({
  context_type: z.enum(["WINDOW", "CHART", "PROCESS"])
    .describe("Type of context to extract"),
  window_id: z.number().int().optional()
    .describe("Window ID (for WINDOW context)"),
  chart_id: z.number().int().optional()
    .describe("Chart ID (for CHART context)"),
  ad_user_id: z.number().int().positive()
    .describe("iDempiere user ID"),
  ad_org_id: z.number().int().positive()
    .describe("iDempiere org ID"),
}).strict();

export function createContextTool(): ToolSchema {
  return {
    name: "extract_context",
    description: "Extract context information from iDempiere windows or charts. "
      + "Returns window metadata, current record data, and related information. "
      + "Useful for understanding what data the user is currently viewing.",
    inputSchema: {
      type: "object",
      properties: {
        context_type: {
          type: "string",
          enum: ["WINDOW", "CHART", "PROCESS"],
          description: "What context to extract",
        },
        window_id: {
          type: "number",
          description: "iDempiere window ID",
        },
        chart_id: {
          type: "number",
          description: "iDempiere chart ID",
        },
        ad_user_id: {
          type: "number",
          description: "iDempiere user ID",
        },
        ad_org_id: {
          type: "number",
          description: "iDempiere organization ID",
        },
      },
      required: ["context_type", "ad_user_id", "ad_org_id"],
    },
    handler: async (args: unknown): Promise<ToolResultBlockSchema[]> => {
      const input = ContextInputSchema.parse(args);

      try {
        logger.info("Context extraction", {
          type: input.context_type,
          windowId: input.window_id,
        });

        const response = await apiClient.post("/context", {
          context_type: input.context_type,
          window_id: input.window_id,
          chart_id: input.chart_id,
          ad_user_id: input.ad_user_id,
          ad_org_id: input.ad_org_id,
        });

        const context = response.data.context;

        let content = `**${input.context_type} Context**\n\n`;
        content += JSON.stringify(context, null, 2);

        return [{
          type: "text",
          text: content,
        }];

      } catch (error) {
        logger.error("Context extraction error", error);

        const errorMsg = error instanceof Error ? error.message : "Unknown error";
        return [{
          type: "text",
          text: `Error extracting context: ${errorMsg}`,
          isError: true,
        }];
      }
    },
  };
}
```

(Continue with `providerTool.ts` and `historyTool.ts` - similar pattern)

#### 2.2.5 Create API Client

**File**: `src/api/client.ts`

```typescript
import axios, { AxiosInstance } from "axios";
import { logger } from "../utils/logger";

const API_BASE_URL = process.env.IDEMPIERE_API_URL || "http://localhost:8080/api/ai";
const API_KEY = process.env.IDEMPIERE_API_KEY || "";
const REQUEST_TIMEOUT = 30000;  // 30 seconds

class APIClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: REQUEST_TIMEOUT,
      headers: {
        "Content-Type": "application/json",
        "X-API-Key": API_KEY,
      },
    });

    // Add response interceptor for logging
    this.client.interceptors.response.use(
      (response) => {
        logger.debug(`API response: ${response.status}`);
        return response;
      },
      (error) => {
        logger.error(`API error: ${error.message}`, error);
        throw error;
      }
    );
  }

  async post(endpoint: string, data: any) {
    try {
      const response = await this.client.post(endpoint, data);
      return response;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(
          `API request failed: ${error.response?.status} - ${error.response?.data?.message || error.message}`
        );
      }
      throw error;
    }
  }

  async get(endpoint: string, params?: any) {
    try {
      const response = await this.client.get(endpoint, { params });
      return response;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(
          `API request failed: ${error.response?.status} - ${error.response?.data?.message || error.message}`
        );
      }
      throw error;
    }
  }
}

export const apiClient = new APIClient();
```

#### 2.2.6 Create Utilities

**File**: `src/utils/logger.ts`

```typescript
import pino from "pino";

export const logger = pino({
  level: process.env.LOG_LEVEL || "info",
  transport: {
    target: "pino-pretty",
    options: {
      colorize: true,
    },
  },
});
```

**File**: `src/utils/formatter.ts`

```typescript
export function formatAsMarkdownTable(
  columns: string[],
  rows: any[],
  cellCharLimit: number = 100
): string {
  // Truncate cell values
  const truncated = rows.map(row =>
    columns.reduce((acc, col) => {
      let val = String(row[col] ?? "");
      if (val.length > cellCharLimit) {
        val = val.substring(0, cellCharLimit - 3) + "...";
      }
      acc[col] = val;
      return acc;
    }, {} as Record<string, string>)
  );

  // Calculate column widths
  const widths = columns.map(col =>
    Math.max(
      col.length,
      Math.max(...truncated.map(row => String(row[col]).length))
    )
  );

  // Build table
  let table = "";

  // Header
  table += "| " + columns.map((col, i) => col.padEnd(widths[i])).join(" | ") + " |\n";
  table += "|" + widths.map(w => "-".repeat(w + 2)).join("|") + "|\n";

  // Rows
  for (const row of truncated) {
    table += "| " + columns.map((col, i) => String(row[col]).padEnd(widths[i])).join(" | ") + " |\n";
  }

  return table;
}
```

### 2.3 Build and Test

```bash
# Install dependencies
npm install

# Build TypeScript
npm run build

# Test server startup (will hang, use Ctrl+C to stop)
# Better to test via MCP client
timeout 5s npm start || true
```

### 2.4 Docker Configuration

**File**: `Dockerfile`

```dockerfile
FROM node:20-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./
RUN npm ci --only=production

# Copy built server
COPY dist ./dist

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD node -e "console.log('ok')" || exit 1

# Run server
CMD ["node", "dist/index.js"]
```

**File**: `docker-compose.yml`

```yaml
version: '3.8'

services:
  idempiere:
    image: idempiere:latest
    ports:
      - "8080:8080"
      - "7654:7654"
    environment:
      IDEMPIERE_DB_HOST: postgres
      IDEMPIERE_DB_NAME: idempiere
      IDEMPIERE_DB_USER: idempiere
      IDEMPIERE_DB_PASSWORD: idempiere
    depends_on:
      - postgres
    networks:
      - idempiere-net

  mcp-server:
    build: .
    ports:
      - "9000:9000"
    environment:
      IDEMPIERE_API_URL: "http://idempiere:8080/api/ai"
      IDEMPIERE_API_KEY: "${API_KEY}"
      LOG_LEVEL: "info"
    depends_on:
      - idempiere
    networks:
      - idempiere-net
    restart: always

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: idempiere
      POSTGRES_USER: idempiere
      POSTGRES_PASSWORD: idempiere
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - idempiere-net

volumes:
  postgres_data:

networks:
  idempiere-net:
```

## Testing the MCP Server

### Using MCP Inspector

```bash
# Install MCP inspector
npm install -g @modelcontextprotocol/inspector

# Run inspector against stdio server
mcp-inspector node /path/to/mcp-server/dist/index.js

# Now visit http://localhost:3000 in browser
```

### Manual Testing with cURL

```bash
# Assuming MCP server exposes HTTP endpoint
curl -X POST http://localhost:9000/tools \
  -H "Content-Type: application/json" \
  -d '{
    "name": "chat_with_context",
    "arguments": {
      "message": "Show me active orders",
      "ad_user_id": 100,
      "ad_org_id": 11
    }
  }'
```

## Best Practices for iDempiere Integration

### 1. **Security**
- Always validate API keys
- Use HTTPS in production
- Implement rate limiting per user
- Log all access attempts
- Sanitize error messages (don't leak schema info)

### 2. **Performance**
- Cache provider info (it doesn't change often)
- Use database connection pooling
- Set reasonable timeouts
- Limit max rows per query
- Implement pagination

### 3. **Reliability**
- Implement exponential backoff for retries
- Handle network timeouts gracefully
- Validate all external input
- Monitor error rates
- Health check endpoint

### 4. **Maintainability**
- Keep HTTP layer separate from MCP server
- Use dependency injection
- Comprehensive logging
- Well-documented error codes
- Configuration via environment variables

See `03-BEST_PRACTICES.md` and `04-DEPLOYMENT.md` for detailed guides.
