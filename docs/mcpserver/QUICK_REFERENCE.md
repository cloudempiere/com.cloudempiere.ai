# MCP Server Quick Reference Guide

One-page reference for common MCP server tasks.

## 📋 Documentation Map

| Document | Purpose | When to Read |
|----------|---------|--------------|
| **README.md** | Overview & quick start | First - start here |
| **01-ARCHITECTURE.md** | Design & architecture decisions | Planning phase |
| **02-IMPLEMENTATION_GUIDE.md** | Step-by-step implementation | Development phase |
| **03-BEST_PRACTICES.md** | Production patterns & security | During & after development |
| **04-DEPLOYMENT.md** | Deployment & operations | Pre-production & production |

## 🚀 Quick Start Commands

### Local Development
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f mcp-server

# Stop services
docker-compose down
```

### Development Server
```bash
# Install dependencies
npm install

# Build TypeScript
npm run build

# Start server (blocks terminal)
npm start

# Test with timeout
timeout 5s npm start || true
```

### Testing
```bash
# Run unit tests
npm test

# Run specific test
npm test -- chatTool.test.ts

# Test with coverage
npm test -- --coverage
```

## 🛠️ Development Checklist

### Before Implementation
- [ ] Read 01-ARCHITECTURE.md completely
- [ ] Choose deployment architecture (Recommended: Option 1)
- [ ] Review existing AI plugin code
- [ ] Set up development environment (Docker or local)

### HTTP API Implementation (Phase 1)
- [ ] Create AIRestController.java
- [ ] Add 5 REST endpoints (/chat, /query, /context, /provider/info, /history)
- [ ] Implement request validation
- [ ] Add error handling
- [ ] Register as OSGi service
- [ ] Write unit tests
- [ ] Document with Swagger/OpenAPI

### MCP Server Implementation (Phase 2)
- [ ] Initialize Node.js project
- [ ] Install @modelcontextprotocol/sdk
- [ ] Create 5 MCP tools (chat, query, context, provider, history)
- [ ] Implement Zod schemas for input validation
- [ ] Create API client for iDempiere
- [ ] Implement error handling & retries
- [ ] Add comprehensive logging
- [ ] Write unit & integration tests
- [ ] Build Docker image

### Testing & Validation
- [ ] Unit tests for all tools (100% coverage)
- [ ] Integration tests with real iDempiere
- [ ] MCP inspector validation
- [ ] Load testing (stress test with multiple concurrent requests)
- [ ] Security testing (SQL injection, authorization checks)

### Deployment Preparation
- [ ] Security audit (API keys, HTTPS, auth)
- [ ] Documentation review
- [ ] Backup strategy implementation
- [ ] Monitoring setup
- [ ] Runbook creation

## 📊 MCP Server Architecture

```
External MCP Clients
(Claude Code, etc.)
        │
        ▼
┌─────────────────────────────┐
│  MCP Server (Node.js)       │
├─────────────────────────────┤
│  5 Tools:                   │
│  ├─ chat_with_context       │
│  ├─ query_database          │
│  ├─ extract_context         │
│  ├─ get_provider_info       │
│  └─ get_conversation_history│
└────────────┬────────────────┘
             │ HTTP (API calls)
             ▼
┌─────────────────────────────┐
│  iDempiere (Port 8080)      │
├─────────────────────────────┤
│  HTTP API Endpoints:        │
│  └─ /api/ai/*               │
│                             │
│  AI Plugin (OSGi):          │
│  ├─ AIConversationService   │
│  ├─ AI Providers            │
│  ├─ Database Security       │
│  └─ Context Extraction      │
└────────────┬────────────────┘
             │ SQL queries
             ▼
┌─────────────────────────────┐
│  PostgreSQL Database        │
└─────────────────────────────┘
```

## 🔐 Security Must-Haves

```javascript
// 1. Validate API key on every request
const API_KEY = process.env.IDEMPIERE_API_KEY;
if (!API_KEY) throw new Error("IDEMPIERE_API_KEY required");

if (request.headers["x-api-key"] !== API_KEY) {
  return { error: "Invalid API key", status: 401 };
}

// 2. Validate all input with Zod
const schema = z.object({
  message: z.string().min(1).max(5000),
  ad_user_id: z.number().int().positive(),
  ad_org_id: z.number().int().positive(),
}).strict();

const input = schema.parse(request.body);

// 3. Never log sensitive data
logger.info({
  tool: "chat",
  userId: input.ad_user_id,  // OK
  // Don't log: password, api_key, credit_card
});

// 4. Redact sensitive fields from responses
const sensitiveFields = ["PASSWORD", "APIKEY", "CREDITCARD"];
if (sensitiveFields.some(f => columnName.includes(f))) {
  return "[REDACTED]";
}

// 5. Use HTTPS in production
if (process.env.NODE_ENV === "production") {
  require("https").createServer(sslConfig, app).listen(443);
}
```

## 🔍 Tool Implementation Template

```typescript
import { z } from "zod";
import { ToolSchema } from "@modelcontextprotocol/sdk/types";
import { apiClient } from "../api/client";
import { logger } from "../utils/logger";

// 1. Define input schema
const InputSchema = z.object({
  required_param: z.string().describe("What this param does"),
  optional_param: z.number().optional(),
}).strict();

// 2. Create tool
export function createMyTool(): ToolSchema {
  return {
    name: "tool_name",
    description: "One-line summary. Detailed explanation of what it does and when to use it.",
    inputSchema: {
      type: "object",
      properties: {
        required_param: {
          type: "string",
          description: "What this param does",
        },
        optional_param: {
          type: "number",
          description: "Optional param",
        },
      },
      required: ["required_param"],
    },

    // 3. Implement handler
    handler: async (args: unknown) => {
      try {
        // Validate input
        const input = InputSchema.parse(args);
        logger.info("Tool called", { tool: "tool_name", ...input });

        // Call API
        const response = await apiClient.post("/endpoint", input);

        // Format response
        let content = "**Result**\n\n";
        content += response.data.result;

        return [{
          type: "text",
          text: content,
        }];

      } catch (error) {
        logger.error("Tool error", { tool: "tool_name", error });

        return [{
          type: "text",
          text: `Error: ${error.message}`,
          isError: true,
        }];
      }
    },
  };
}
```

## 📈 Performance Tuning

| Issue | Solution | Configuration |
|-------|----------|---------------|
| Slow responses | Increase timeout | `IDEMPIERE_REQUEST_TIMEOUT=60000` |
| High latency | Use caching | `CACHE_TTL_SECONDS=1800` |
| Too many rows | Limit results | `MAX_QUERY_ROWS=50` |
| Memory issues | Reduce history | `MAX_HISTORY_DEPTH=5` |
| Rate limiting | Add backoff | Implement exponential backoff |

## 🧪 Testing Template

```typescript
describe("My Tool", () => {
  // 1. Test validation
  it("should validate required input", async () => {
    expect(() => {
      inputSchema.parse({ /* missing required fields */ });
    }).toThrow();
  });

  // 2. Test success case
  it("should execute successfully", async () => {
    jest.spyOn(apiClient, "post").mockResolvedValue({
      data: { result: "success" },
    });

    const result = await tool.handler({ required_param: "test" });

    expect(result[0].text).toContain("success");
  });

  // 3. Test error case
  it("should handle errors gracefully", async () => {
    jest.spyOn(apiClient, "post").mockRejectedValue(
      new Error("API error")
    );

    const result = await tool.handler({ required_param: "test" });

    expect(result[0].isError).toBe(true);
  });
});
```

## 📊 Environment Variables Reference

```bash
# Required
IDEMPIERE_API_URL=http://localhost:8080/api/ai
IDEMPIERE_API_KEY=your-secret-key

# Optional (with defaults)
LOG_LEVEL=info                          # debug|info|warn|error
IDEMPIERE_REQUEST_TIMEOUT=30000         # milliseconds
CACHE_TTL_SECONDS=1800                  # 30 minutes
MAX_QUERY_ROWS=100
MAX_HISTORY_DEPTH=10
NODE_ENV=development                    # development|staging|production
```

## 🚨 Troubleshooting Quick Map

| Symptom | Likely Cause | Solution |
|---------|-------------|----------|
| "Cannot connect" | API unreachable | Check `IDEMPIERE_API_URL` and connectivity |
| "Invalid API key" | Wrong key | Verify `IDEMPIERE_API_KEY` |
| "Query timeout" | Long-running query | Increase `IDEMPIERE_REQUEST_TIMEOUT` or optimize query |
| "Tools not found" | Server not running | Check `docker-compose ps` or `npm start` |
| "Out of memory" | Too much caching | Reduce `MAX_HISTORY_DEPTH` or `CACHE_TTL_SECONDS` |
| "High latency" | Network/resource issue | Check CPU/memory/network latency |
| "Sensitive data leaked" | Missing redaction | Verify sensitive field list in code |

## 📚 Key Documentation Sections

### From 01-ARCHITECTURE.md
- **Current Plugin Architecture** (p.2) - Understand existing system
- **MCP Tool Boundaries** (p.3) - 5 tools exposed to MCP clients
- **Security Model** (p.9) - Role-based access control
- **Performance Considerations** (p.10) - Caching, timeouts, rate limiting

### From 02-IMPLEMENTATION_GUIDE.md
- **HTTP API Endpoints** (p.2) - REST API design
- **Complete Tool Code** (p.10+) - Copy-paste implementations
- **Docker Setup** (p.20) - Container configuration

### From 03-BEST_PRACTICES.md
- **Security Checklist** (p.1) - 10-point security guide
- **Error Handling** (p.3) - Categorization and responses
- **Caching Strategy** (p.5) - Multi-layer caching
- **Rate Limiting** (p.6) - Per-user/service limits

### From 04-DEPLOYMENT.md
- **Architecture Options** (p.1) - Choose deployment style
- **Docker Compose Setup** (p.4) - Local development
- **Production Checklist** (p.9) - Pre-deploy verification
- **Troubleshooting** (p.12) - Common issues and fixes

## 🔄 Development Workflow

```
1. Plan (read 01-ARCHITECTURE.md)
         ↓
2. Setup (docker-compose up -d)
         ↓
3. Implement HTTP API (follow 02-IMPLEMENTATION_GUIDE.md Phase 1)
         ↓
4. Test HTTP API (curl, integration tests)
         ↓
5. Implement MCP Server (follow 02-IMPLEMENTATION_GUIDE.md Phase 2)
         ↓
6. Test MCP Server (mcp-inspector, unit tests)
         ↓
7. Apply Best Practices (review 03-BEST_PRACTICES.md)
         ↓
8. Staging Deployment (follow 04-DEPLOYMENT.md Phase 1)
         ↓
9. Production Deployment (follow 04-DEPLOYMENT.md Phase 2)
         ↓
10. Operations (monitor, logs, backups)
```

## 🎯 Success Criteria

- [ ] All 5 MCP tools implemented and tested
- [ ] Zero security vulnerabilities in code
- [ ] All environment variables documented
- [ ] 100% unit test coverage for tools
- [ ] Integration tests passing with real iDempiere
- [ ] Docker image builds and runs successfully
- [ ] API response times < 5 seconds (95th percentile)
- [ ] Error rate < 0.1% (99.9% uptime)
- [ ] All queries logged to audit table
- [ ] Sensitive fields redacted in responses
- [ ] HTTPS/TLS enabled in production
- [ ] Backup/restore tested successfully
- [ ] Monitoring alerts configured
- [ ] Runbook documentation complete
- [ ] Deployment completed and verified

---

**For complete details, see the full documentation in 01-ARCHITECTURE.md through 04-DEPLOYMENT.md**
