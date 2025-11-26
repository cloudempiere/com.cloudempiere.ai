# MCP Server Best Practices for iDempiere AI Plugin

## Design Principles

### 1. Security-First Approach

#### Authentication & Authorization

**Principle**: Never elevate privileges beyond what the iDempiere user can access.

**Implementation**:

```
Request Flow:
┌──────────────────────────────┐
│  MCP Client (external service)│
│  Example: Claude Code Agent   │
└────────┬─────────────────────┘
         │
    API Key validation
         │
         ▼
┌──────────────────────────────┐
│  MCP Server                   │
│  - Validates API key          │
│  - Identifies service         │
│  - Maps to iDempiere user     │
└────────┬─────────────────────┘
         │
    iDempiere user session
         │
         ▼
┌──────────────────────────────┐
│  iDempiere API Endpoint       │
│  - Validates user credentials │
│  - Applies MRole permissions  │
│  - Row-level security via SQL │
└────────┬─────────────────────┘
         │
    Database query execution
         │
         ▼
┌──────────────────────────────┐
│  Results (with filtering)     │
│  - Sensitive field redaction  │
│  - Audit logging              │
└──────────────────────────────┘
```

**Key Rules**:

1. **No Service Account Escalation**
   - Don't use a privileged "AI User" that bypasses role checks
   - Always execute queries with: AI User + Requesting User's Role
   - This ensures AI can't access more than the user

2. **Explicit Permission Checks**
   ```java
   // WRONG: AI user has SUPER_USER role
   AIUser AIProvider.getUser();  // Super user - too powerful

   // RIGHT: Combine AI user with requester's role
   AIUser = AIG_Provider.AD_User_ID;
   RequestingUser = Request.AD_User_ID;
   ExecutionRole = MRole.get(RequestingUser.AD_Role_ID);
   ```

3. **Input Validation**
   - Validate all tool input before passing to iDempiere
   - Use Zod schemas (TypeScript) or Pydantic (Python) for strong typing
   - Never pass unsanitized SQL directly
   - Reject queries with unsafe patterns (UNION, UPDATE, etc.)

4. **API Key Management**
   ```typescript
   // Validate API key from environment
   const API_KEY = process.env.IDEMPIERE_API_KEY;
   if (!API_KEY) {
     throw new Error("IDEMPIERE_API_KEY not configured");
   }

   // In request handler
   if (request.headers["x-api-key"] !== API_KEY) {
     return errorResponse("Invalid API key", 401);
   }
   ```

#### Sensitive Field Redaction

**Configuration** (in iDempiere):

```java
// Define sensitive fields per org
AIG_SensitiveField table:
├─ AD_Org_ID
├─ Column_Name (PASSWORD, CREDITCARD, etc.)
├─ Redact_Format (*****, [REDACTED], etc.)
└─ IsActive
```

**Automatic Redaction**:

```java
private static final Set<String> SENSITIVE_PATTERNS = Set.of(
    "PASSWORD", "USERPIN", "CREDITCARD", "CVV", "CVC",
    "SSN", "TAXID", "BANKACCOUNT", "IBAN", "APIKEY",
    "TOKEN", "SECRET", "SALT", "LDAP", "PRIVATEKEY",
    "CERTIFICATE"
);

private String redactIfSensitive(String columnName, String value) {
    if (SENSITIVE_PATTERNS.stream().anyMatch(columnName::contains)) {
        return "[REDACTED]";
    }
    return value;
}
```

### 2. Reliability & Fault Tolerance

#### Error Handling Strategy

**Categorize Errors**:

```typescript
type ErrorCategory =
  | "VALIDATION_ERROR"  // Input validation failed
  | "AUTHENTICATION_ERROR"  // Auth failure
  | "AUTHORIZATION_ERROR"   // Permission denied
  | "NOT_FOUND"            // Resource not found
  | "TIMEOUT"              // Query took too long
  | "RATE_LIMITED"         // Too many requests
  | "INTERNAL_ERROR"       // Server error
  | "TEMPORARY_ERROR"      // Transient (retry safe)
  | "PERMANENT_ERROR";     // Don't retry

// Return appropriate HTTP status
function getHttpStatus(category: ErrorCategory): number {
  switch (category) {
    case "VALIDATION_ERROR": return 400;
    case "AUTHENTICATION_ERROR": return 401;
    case "AUTHORIZATION_ERROR": return 403;
    case "NOT_FOUND": return 404;
    case "RATE_LIMITED": return 429;
    case "TIMEOUT": return 504;
    default: return 500;
  }
}
```

**Error Response Format**:

```json
{
  "error": true,
  "error_code": "VALIDATION_ERROR",
  "error_message": "Invalid AD_User_ID: must be positive integer",
  "details": {
    "field": "ad_user_id",
    "expected": "positive integer",
    "received": "-5"
  },
  "timestamp": "2025-11-26T10:30:00Z",
  "request_id": "req_abc123"  // For debugging
}
```

**Actionable Error Messages**:

```typescript
// BAD: Not actionable
throw new Error("Query failed");

// GOOD: Guides next steps
throw new Error(
  "Query execution timeout (5s exceeded). " +
  "Try: (1) adding WHERE clause to reduce rows, " +
  "(2) increasing timeout_seconds parameter, or " +
  "(3) breaking query into smaller chunks. " +
  "Query returned ~500k rows before timeout."
);
```

#### Retry Strategy

```typescript
async function executeWithRetry<T>(
  fn: () => Promise<T>,
  options: {
    maxRetries: number;
    baseDelayMs: number;
    maxDelayMs: number;
    isRetryable: (error: Error) => boolean;
  }
): Promise<T> {
  let lastError: Error | null = null;

  for (let attempt = 0; attempt <= options.maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;

      if (!options.isRetryable(lastError) || attempt === options.maxRetries) {
        throw lastError;
      }

      // Exponential backoff with jitter
      const delay = Math.min(
        options.baseDelayMs * Math.pow(2, attempt) + Math.random() * 1000,
        options.maxDelayMs
      );

      logger.info(`Retry attempt ${attempt + 1} after ${delay}ms`, {
        error: lastError.message,
      });

      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }

  throw lastError;
}

// Usage
const result = await executeWithRetry(
  () => apiClient.post("/chat", request),
  {
    maxRetries: 3,
    baseDelayMs: 1000,
    maxDelayMs: 10000,
    isRetryable: (error) => {
      // Retry on network errors, timeouts, and 5xx
      return error.message.includes("TIMEOUT") ||
             error.message.includes("502") ||
             error.message.includes("503");
    },
  }
);
```

#### Timeout Configuration

```typescript
// Tool-specific timeouts
const TIMEOUTS = {
  // Quick responses
  "get_provider_info": 5000,      // 5 seconds
  "extract_context": 10000,       // 10 seconds
  "get_conversation_history": 10000,

  // Longer for AI operations
  "chat_with_context": 60000,     // 60 seconds (AI might be slow)
  "query_database": 30000,        // 30 seconds (query execution)
};

// Apply timeout
async function callToolWithTimeout(
  toolName: string,
  handler: () => Promise<any>
): Promise<any> {
  const timeout = TIMEOUTS[toolName] || 30000;

  return Promise.race([
    handler(),
    new Promise((_, reject) =>
      setTimeout(
        () => reject(new Error(`Tool timeout after ${timeout}ms`)),
        timeout
      )
    ),
  ]);
}
```

### 3. Performance Optimization

#### Response Format Control

**Principle**: Return concise by default, detailed on request.

```typescript
interface ChatRequest {
  // ... other fields
  response_format: "CONCISE" | "DETAILED";  // Default: CONCISE
  response_detail_level?: 1 | 2 | 3;         // Level 1-3
}

// Level 1 (default): Summary only
"The 5 most active orders in the last 30 days are..."

// Level 2: Summary + key data
"The 5 most active orders:
| Order | Status | Value |
|-------|--------|-------|
| SO-001 | Delivered | $5000 |
..."

// Level 3: Full details
"The 5 most active orders (complete details):
| Order | Status | Value | Customer | Created | Updated | Notes |
..."
```

**Implementation**:

```typescript
interface FormatOptions {
  concise: boolean;
  maxTokens?: number;
  maxChars?: number;
  includeMetadata: boolean;
}

function formatResponse(
  data: any,
  options: FormatOptions
): string {
  let output = "";

  if (options.concise) {
    // Summary version
    output = generateSummary(data);
  } else {
    // Full version
    output = generateDetailed(data);
  }

  // Truncate if needed
  if (options.maxChars && output.length > options.maxChars) {
    output = output.substring(0, options.maxChars) + "\n...[truncated]";
  }

  if (options.includeMetadata) {
    output += `\n\nCharacters: ${output.length} | Generated: ${new Date().toISOString()}`;
  }

  return output;
}
```

#### Caching Strategy

**Cache Layers**:

```
Request
  │
  ├─ L1: In-Memory Cache (per tool, 5 min TTL)
  │  └─ provider_info, available_models
  │
  ├─ L2: iDempiere ConversationContextManager
  │  └─ Query results, extracted context (configurable TTL)
  │
  └─ L3: Database (CM_ChatEntry, persistent)
     └─ Full conversation history
```

**Implementation**:

```typescript
class CacheManager {
  private cache = new Map<string, CacheEntry>();

  async get<T>(
    key: string,
    loader: () => Promise<T>,
    ttlSeconds: number = 300
  ): Promise<T> {
    const cached = this.cache.get(key);

    // Valid cache hit
    if (cached && !this.isExpired(cached)) {
      logger.debug(`Cache hit: ${key}`);
      return cached.value;
    }

    // Cache miss or expired
    logger.debug(`Cache miss: ${key}`);
    const value = await loader();

    this.cache.set(key, {
      value,
      expiresAt: Date.now() + ttlSeconds * 1000,
    });

    return value;
  }

  invalidate(pattern: RegExp) {
    for (const key of this.cache.keys()) {
      if (pattern.test(key)) {
        this.cache.delete(key);
        logger.debug(`Cache invalidated: ${key}`);
      }
    }
  }

  private isExpired(entry: CacheEntry): boolean {
    return entry.expiresAt < Date.now();
  }
}

// Usage
const cache = new CacheManager();

// Cache provider info for 30 minutes
const providerInfo = await cache.get(
  "provider:info",
  () => apiClient.get("/provider/info"),
  1800  // 30 minutes
);

// Invalidate when provider config changes
cache.invalidate(/^provider:/);
```

#### Rate Limiting

```typescript
interface RateLimitConfig {
  requestsPerMinute: number;
  requestsPerHour: number;
  burstSize: number;
  keyFunction: (request: any) => string;  // e.g., API key, user ID
}

class RateLimiter {
  private buckets = new Map<string, TokenBucket>();

  async checkLimit(request: any, config: RateLimitConfig): Promise<boolean> {
    const key = config.keyFunction(request);
    let bucket = this.buckets.get(key);

    if (!bucket) {
      bucket = new TokenBucket(config);
      this.buckets.set(key, bucket);
    }

    return bucket.consume();
  }

  getRemainingTokens(request: any, config: RateLimitConfig): number {
    const key = config.keyFunction(request);
    return this.buckets.get(key)?.getTokens() ?? config.burstSize;
  }
}

// Usage
const limiter = new RateLimiter();

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const allowed = await limiter.checkLimit(request, {
    requestsPerMinute: 60,
    requestsPerHour: 1000,
    burstSize: 10,
    keyFunction: (req) => req.params.userId,
  });

  if (!allowed) {
    const remaining = limiter.getRemainingTokens(request, config);
    return {
      type: "text",
      text: `Rate limit exceeded. Retry in 1 minute. (${remaining} tokens remaining)`,
      isError: true,
    };
  }

  // Process request
});
```

### 4. Observability & Monitoring

#### Structured Logging

```typescript
import pino from "pino";

const logger = pino({
  level: process.env.LOG_LEVEL || "info",
  formatters: {
    level: (label) => ({ level: label.toUpperCase() }),
    bindings: (bindings) => ({
      pid: bindings.pid,
      hostname: bindings.hostname,
    }),
  },
  timestamp: pino.stdTimeFunctions.isoTime,
});

// Log tool execution
logger.info(
  {
    tool: "chat_with_context",
    sessionId: "sess_123",
    userId: 100,
    messageLength: 150,
    executionTimeMs: 2500,
  },
  "Tool executed successfully"
);

// Log errors with context
logger.error(
  {
    tool: "query_database",
    userId: 100,
    error: error.message,
    errorCode: "QUERY_TIMEOUT",
    queryLength: 500,
    timeoutMs: 5000,
  },
  "Tool execution failed"
);
```

#### Metrics & Health Checks

```typescript
interface ServerMetrics {
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  averageResponseTimeMs: number;
  lastErrorTime?: Date;
  lastError?: string;
  uptime: number;
}

class MetricsCollector {
  private metrics: ServerMetrics = {
    totalRequests: 0,
    successfulRequests: 0,
    failedRequests: 0,
    averageResponseTimeMs: 0,
    uptime: Date.now(),
  };

  recordRequest(success: boolean, responseTimeMs: number) {
    this.metrics.totalRequests++;
    if (success) {
      this.metrics.successfulRequests++;
    } else {
      this.metrics.failedRequests++;
    }

    // Update rolling average
    this.metrics.averageResponseTimeMs =
      (this.metrics.averageResponseTimeMs * (this.metrics.totalRequests - 1) +
        responseTimeMs) /
      this.metrics.totalRequests;
  }

  recordError(error: Error) {
    this.metrics.lastErrorTime = new Date();
    this.metrics.lastError = error.message;
  }

  getMetrics(): ServerMetrics {
    return {
      ...this.metrics,
      uptime: Date.now() - this.metrics.uptime,
    };
  }

  getHealthStatus(): "HEALTHY" | "DEGRADED" | "UNHEALTHY" {
    const errorRate = this.metrics.failedRequests / this.metrics.totalRequests;

    if (errorRate > 0.5) return "UNHEALTHY";
    if (errorRate > 0.1) return "DEGRADED";
    return "HEALTHY";
  }
}

// Health check endpoint
app.get("/health", (req, res) => {
  const status = metrics.getHealthStatus();
  const code = status === "HEALTHY" ? 200 : status === "DEGRADED" ? 503 : 500;

  res.status(code).json({
    status,
    metrics: metrics.getMetrics(),
    timestamp: new Date().toISOString(),
  });
});
```

### 5. Documentation Standards

#### Tool Documentation

Each tool should have:

```typescript
{
  name: "chat_with_context",

  // 1. One-line summary
  description: "Send a message to the AI with automatic context extraction.",

  // 2. Detailed explanation (in docstring)
  // """
  // This tool sends a message to the iDempiere AI and can optionally:
  // - Load conversation history for context
  // - Extract window/chart context from iDempiere
  // - Execute AI function calls to query the database
  // - Support multi-turn dialogue with coherent conversation
  //
  // The AI response considers:
  // - User's role-based permissions (only data they can access)
  // - Conversation history (remembers previous exchanges)
  // - Window context (what they're currently viewing)
  // - Available database functions
  //
  // Use cases:
  // - "Show me active orders for this customer"
  // - "What's the inventory status of this product?"
  // - "Create a summary of this order"
  // """

  // 3. Input schema with examples
  inputSchema: {
    type: "object",
    properties: {
      message: {
        type: "string",
        description: "Your question or request",
        examples: [
          "Show me active orders",
          "What's the inventory level?",
          "Create a summary of this order"
        ]
      },
      // ... more properties with clear descriptions
    }
  }
}
```

#### Configuration Documentation

```markdown
# Configuration Guide

## Environment Variables

### Required
- `IDEMPIERE_API_URL`: Base URL of iDempiere API endpoint (e.g., http://localhost:8080/api/ai)
- `IDEMPIERE_API_KEY`: API key for authentication

### Optional
- `LOG_LEVEL`: Logging level (debug, info, warn, error). Default: info
- `IDEMPIERE_REQUEST_TIMEOUT`: Request timeout in milliseconds. Default: 30000
- `CACHE_TTL_SECONDS`: Cache TTL for provider info. Default: 1800
- `MAX_QUERY_ROWS`: Maximum rows per query. Default: 100
- `QUERY_TIMEOUT_SECONDS`: Query execution timeout. Default: 5
- `MAX_HISTORY_DEPTH`: Maximum conversation history entries. Default: 10

## Example Configuration

```bash
# .env
IDEMPIERE_API_URL=http://idempiere:8080/api/ai
IDEMPIERE_API_KEY=sk-prod-abc123def456
LOG_LEVEL=info
```
```

## Code Organization Best Practices

### Directory Structure

```
mcp-server/
├── src/
│   ├── index.ts               # Main entry point
│   ├── server.ts              # Server initialization
│   │
│   ├── tools/                 # Tool implementations
│   │   ├── index.ts           # Tool exports
│   │   ├── chatTool.ts
│   │   ├── queryTool.ts
│   │   ├── contextTool.ts
│   │   ├── providerTool.ts
│   │   └── historyTool.ts
│   │
│   ├── api/                   # API client & communication
│   │   ├── client.ts          # HTTP client
│   │   ├── endpoints.ts       # Endpoint definitions
│   │   └── types.ts           # Response types
│   │
│   ├── schemas/               # Zod validation schemas
│   │   ├── tools.ts           # Tool input schemas
│   │   └── responses.ts       # Response schemas
│   │
│   ├── utils/                 # Shared utilities
│   │   ├── logger.ts          # Logging setup
│   │   ├── formatter.ts       # Response formatting
│   │   ├── validation.ts      # Common validation
│   │   ├── cache.ts           # Caching utilities
│   │   └── errors.ts          # Error handling
│   │
│   ├── config/                # Configuration
│   │   └── constants.ts       # App constants
│   │
│   └── types/                 # TypeScript types
│       └── index.ts           # Common types
│
├── tests/
│   ├── tools/
│   ├── api/
│   └── utils/
│
├── .env.example               # Example configuration
├── package.json
├── tsconfig.json
└── README.md
```

### Module Organization

**Each tool is self-contained**:

```typescript
// tools/chatTool.ts
import { z } from "zod";
import { ToolSchema } from "@modelcontextprotocol/sdk/types";
import { apiClient } from "../api/client";
import { logger } from "../utils/logger";

// Define schema (specific to this tool)
const inputSchema = z.object({...});

// Implement tool
export function createChatTool(): ToolSchema {
  return {
    name: "chat_with_context",
    description: "...",
    inputSchema: {...},
    handler: async (args) => {
      // Implementation
    },
  };
}
```

### Avoid Common Mistakes

**❌ Don't**: Mix tools into one large file
```typescript
// tools.ts (2000+ lines)
// WRONG: All tools in one file
function createChatTool() { ... }
function createQueryTool() { ... }
function createContextTool() { ... }
// ...
```

**✅ Do**: Separate tools into individual files
```typescript
// tools/chatTool.ts
export function createChatTool() { ... }

// tools/queryTool.ts
export function createQueryTool() { ... }

// tools/contextTool.ts
export function createContextTool() { ... }

// tools/index.ts
export * from "./chatTool";
export * from "./queryTool";
export * from "./contextTool";
```

**❌ Don't**: Hardcode configuration
```typescript
const API_KEY = "sk-abc123";  // WRONG
const API_BASE = "http://localhost:8080";
```

**✅ Do**: Use environment variables
```typescript
const API_KEY = process.env.IDEMPIERE_API_KEY;
const API_BASE = process.env.IDEMPIERE_API_URL;

if (!API_KEY) {
  throw new Error("IDEMPIERE_API_KEY not configured");
}
```

**❌ Don't**: Log sensitive data
```typescript
logger.info("API response:", response.data);  // Could contain passwords!
logger.debug("User password:", password);
```

**✅ Do**: Sanitize logs
```typescript
logger.info("API call", {
  endpoint: "/chat",
  statusCode: response.status,
  responseSize: JSON.stringify(response.data).length,
  // Don't log: passwords, API keys, sensitive data
});
```

## Testing Best Practices

### Unit Tests

```typescript
import { describe, it, expect, jest } from "@jest/globals";
import { createChatTool } from "../tools/chatTool";

describe("Chat Tool", () => {
  it("should validate required input", async () => {
    const tool = createChatTool();

    expect(() => {
      tool.handler({
        // Missing required fields
        ad_user_id: 100,
        // message is missing
      });
    }).toThrow("message is required");
  });

  it("should format response correctly", async () => {
    const tool = createChatTool();
    // Mock API response
    jest.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        response: "Here are your orders...",
        tokens_used: { input: 150, output: 450 },
      },
    });

    const result = await tool.handler({
      message: "Show orders",
      ad_user_id: 100,
      ad_org_id: 11,
    });

    expect(result[0].text).toContain("Here are your orders");
    expect(result[0].text).toContain("Tokens Used");
  });

  it("should handle errors gracefully", async () => {
    jest.spyOn(apiClient, "post").mockRejectedValue(
      new Error("API timeout")
    );

    const result = await tool.handler({...});

    expect(result[0].isError).toBe(true);
    expect(result[0].text).toContain("Error:");
  });
});
```

### Integration Tests

```typescript
describe("E2E Chat Flow", () => {
  it("should execute complete chat workflow", async () => {
    // 1. Create session
    const sessionResponse = await apiClient.post("/chat", {
      message: "Show active orders",
      ad_user_id: 100,
      ad_org_id: 11,
    });

    const sessionId = sessionResponse.data.session_id;

    // 2. Continue conversation
    const continueResponse = await apiClient.post("/chat", {
      session_id: sessionId,
      message: "Filter by customer ABC",
      ad_user_id: 100,
      ad_org_id: 11,
    });

    // 3. Get history
    const historyResponse = await apiClient.get(
      `/conversation/${sessionId}/history`
    );

    expect(historyResponse.data.messages).toHaveLength(2);
    expect(historyResponse.data.messages[0].role).toBe("user");
    expect(historyResponse.data.messages[1].role).toBe("assistant");
  });
});
```

See `04-DEPLOYMENT.md` for production deployment guidelines.
