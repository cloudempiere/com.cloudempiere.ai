# iDempiere AI Plugin MCP Server Documentation

Complete guide for building, deploying, and using an MCP (Model Context Protocol) server wrapper around the iDempiere AI Plugin.

## 📋 Contents

1. **[01-ARCHITECTURE.md](./01-ARCHITECTURE.md)** - System design and architectural decisions
   - Current plugin architecture overview
   - MCP server wrapper design (3 implementation options)
   - Key architectural decisions (access patterns, security, state management)
   - Integration points with existing plugin
   - Benefits and use cases

2. **[02-IMPLEMENTATION_GUIDE.md](./02-IMPLEMENTATION_GUIDE.md)** - Step-by-step implementation instructions
   - Phase 1: HTTP API layer (Java/iDempiere)
   - Phase 2: MCP server implementation (Node.js/TypeScript)
   - Complete code examples for all tools
   - Docker configuration for containerized deployment
   - Testing approaches

3. **[03-BEST_PRACTICES.md](./03-BEST_PRACTICES.md)** - Production-ready patterns and practices
   - Security-first approach (authentication, authorization, sensitive data)
   - Reliability patterns (error handling, retries, timeouts)
   - Performance optimization (caching, response formatting, rate limiting)
   - Observability (logging, metrics, health checks)
   - Code organization and testing strategies

4. **[04-DEPLOYMENT.md](./04-DEPLOYMENT.md)** - Deployment architectures and operational guides
   - 2 deployment architectures (recommended + advanced)
   - Local development setup (Docker Compose + manual)
   - Production deployment (AWS, on-premises)
   - Security hardening (TLS, API keys, network isolation)
   - Monitoring, logging, and troubleshooting
   - High availability setup

## 🚀 Quick Start

### Prerequisites
- Java 11+ (for iDempiere)
- Node.js 18+ (for MCP server)
- Docker & Docker Compose (recommended)

### Local Development (5 minutes)

1. **Clone and setup**
```bash
cd /path/to/com.cloudempiere.ai
git checkout MCP  # Already on this branch
```

2. **Start with Docker Compose**
```bash
cd mcp-server
docker-compose up -d
```

3. **Verify services are running**
```bash
docker-compose ps
# Should show: postgres, idempiere, mcp-server all healthy
```

4. **Test connectivity**
```bash
# Test iDempiere API
curl http://localhost:8080/api/ai/provider/info \
  -H "X-API-Key: dev-key-change-in-prod"

# Test MCP server (should block, Ctrl+C to exit)
timeout 5s npm start || true
```

### Using the MCP Server

#### With Claude Code

```bash
# In Claude Code, configure MCP transport to stdio
# Command: npx node /path/to/mcp-server/dist/index.js
# Environment variables:
#   IDEMPIERE_API_URL=http://localhost:8080/api/ai
#   IDEMPIERE_API_KEY=dev-key-change-in-prod
```

#### With MCP Inspector

```bash
# Install inspector
npm install -g @modelcontextprotocol/inspector

# Run inspector
mcp-inspector node /path/to/mcp-server/dist/index.js

# Open browser to http://localhost:3000
```

## 📚 Core Concepts

### What is an MCP Server?

MCP (Model Context Protocol) enables LLMs to interact with external services through well-defined tools. An MCP server:
- Exposes a set of tools (functions)
- Validates input using schemas
- Handles tool execution
- Returns formatted results

### iDempiere MCP Server Tools

The server exposes 5 main tools:

1. **chat_with_context** - Send message to AI with context awareness
   - Uses conversation history
   - Extracts window context from iDempiere
   - Supports AI function calling for database queries
   - Maintains thread-scoped conversation state

2. **query_database** - Execute SELECT queries with security
   - Row-level security (user's permissions enforced)
   - Query audit logging (all queries tracked)
   - Configurable timeouts and row limits
   - Sensitive field redaction

3. **extract_context** - Get context from windows/charts
   - Window context (current record, field values)
   - Chart context (dashboard data)
   - User/role context
   - Useful for understanding what user is viewing

4. **get_provider_info** - List available AI providers
   - Available models (Claude 3.5 Sonnet, etc.)
   - Capabilities (vision, function calling, embeddings)
   - Health status and rate limits
   - Cost estimation

5. **get_conversation_history** - Retrieve conversation history
   - Load previous messages in session
   - Pagination support
   - Full message details (roles, timestamps, function calls)

### Security Model

```
User Request
    │
    ├─ API Key Authentication (validates external service)
    │
    ├─ iDempiere User Context (identifies user and org)
    │
    ├─ Role-Based Access Control (MRole enforcement)
    │  └─ Can only see data their role can access
    │
    ├─ SQL Security (AccessSqlParser)
    │  └─ WHERE clauses auto-added based on role
    │
    ├─ Sensitive Field Redaction
    │  └─ PASSWORD, CREDITCARD, etc. hidden automatically
    │
    └─ Audit Logging (AIG_QueryAudit)
       └─ All queries logged with AI user + requesting user
```

**Key Principle**: AI cannot access more than the authenticated user.

## 🏗️ Architecture Options

### Option 1: Recommended (HTTP API + Standalone MCP)

```
iDempiere (Port 8080)
  └─ HTTP API Endpoints (/api/ai/*)
       │
       └─ MCP Server (Port 9000, separate process)
            └─ iDempiere API Client
```

**Best for**: Most deployments, clean separation, easy scaling

See [01-ARCHITECTURE.md](./01-ARCHITECTURE.md#architecture-1-embedded-http-api--standalone-mcp-server-recommended)

### Option 2: Embedded OSGi Bundle

```
iDempiere (Port 8080)
  ├─ AI Plugin
  └─ MCP Server (OSGi Bundle)
      └─ Direct service access
```

**Best for**: High-performance, no serialization overhead

See [01-ARCHITECTURE.md](./01-ARCHITECTURE.md#architecture-2-embedded-osgi-based-mcp-server-advanced)

## 📝 Implementation Phases

### Phase 1: HTTP API Layer (Java)
Add REST endpoints to iDempiere that expose AI Plugin functionality.

**Effort**: 2-3 days
**Files**: `AIRestController.java`, OSGi registration
**See**: [02-IMPLEMENTATION_GUIDE.md#phase-1-http-api-layer-idempiere](./02-IMPLEMENTATION_GUIDE.md#phase-1-http-api-layer-idempiere)

### Phase 2: MCP Server (Node.js)
Implement standalone MCP server that calls HTTP API and exposes tools.

**Effort**: 3-5 days
**Language**: TypeScript
**See**: [02-IMPLEMENTATION_GUIDE.md#phase-2-mcp-server-implementation](./02-IMPLEMENTATION_GUIDE.md#phase-2-mcp-server-implementation)

### Phase 3: Testing & Evaluation
Create test suite and evaluation scenarios.

**Effort**: 2-3 days
**See**: [03-BEST_PRACTICES.md#testing-best-practices](./03-BEST_PRACTICES.md#testing-best-practices)

### Phase 4: Production Deployment
Deploy to staging and production environments.

**Effort**: 1-2 days
**See**: [04-DEPLOYMENT.md](./04-DEPLOYMENT.md)

## 🔒 Security Checklist

- [ ] API key stored in environment, not in code
- [ ] All API calls use HTTPS in production
- [ ] Database credentials never logged
- [ ] Input validation on all tools (Zod schemas)
- [ ] Role-based access control enforced
- [ ] Sensitive fields redacted from responses
- [ ] All queries logged to audit table
- [ ] Rate limiting configured
- [ ] Error messages don't leak schema info
- [ ] Network isolation configured (firewall rules)

See [03-BEST_PRACTICES.md#1-security-first-approach](./03-BEST_PRACTICES.md#1-security-first-approach)

## 🚢 Deployment Checklist

- [ ] HTTP API deployed to iDempiere
- [ ] iDempiere API key generated and secured
- [ ] MCP server built and tested locally
- [ ] Docker image built and pushed to registry
- [ ] docker-compose.yml configured for your environment
- [ ] HTTPS/TLS certificates acquired
- [ ] Firewall rules configured
- [ ] Backup strategy implemented
- [ ] Monitoring and alerting configured
- [ ] Runbook documentation created

See [04-DEPLOYMENT.md](./04-DEPLOYMENT.md)

## 🆘 Common Issues

### "IDEMPIERE_API_KEY not configured"
**Solution**: Set `IDEMPIERE_API_KEY` environment variable
```bash
export IDEMPIERE_API_KEY=your-key-here
npm start
```

### "Cannot connect to iDempiere API"
**Solution**: Verify API URL and connectivity
```bash
# Check environment variable
echo $IDEMPIERE_API_URL

# Test connectivity
curl http://localhost:8080/api/ai/provider/info \
  -H "X-API-Key: $IDEMPIERE_API_KEY"
```

### "Query timeout"
**Solution**: Increase timeout or optimize query
```bash
# Increase timeout
export IDEMPIERE_REQUEST_TIMEOUT=60000

# Or optimize query (add WHERE clause, reduce rows)
```

### MCP tools not discovered
**Solution**: Verify server is running and tools are registered
```bash
# Check if server is responding
timeout 2s curl http://localhost:9000/health || echo "Server not responding"

# Inspect tools
npx @modelcontextprotocol/inspector node dist/index.js
```

See [04-DEPLOYMENT.md#troubleshooting-guide](./04-DEPLOYMENT.md#troubleshooting-guide) for more issues.

## 📖 Key Files to Understand

### iDempiere AI Plugin
- `src/com/cloudempiere/ai/service/AIConversationService.java` - Main orchestration
- `src/com/cloudempiere/ai/provider/IAIProvider.java` - Provider interface
- `src/com/cloudempiere/ai/routing/ConversationContextManager.java` - Caching & context
- `src/com/cloudempiere/ai/database/SecureDatabaseQueryExecutor.java` - Query security

### MCP Server Implementation
- `mcp-server/src/index.ts` - Server entry point
- `mcp-server/src/tools/*.ts` - Tool implementations
- `mcp-server/src/api/client.ts` - iDempiere API client
- `mcp-server/Dockerfile` - Container image

### Configuration
- `.env` - Environment variables (create from .env.example)
- `docker-compose.yml` - Local development stack
- `src/com/cloudempiere/ai/http/AIRestController.java` - HTTP API

## 🔗 Useful Links

- **MCP Specification**: https://modelcontextprotocol.io
- **iDempiere Documentation**: https://wiki.idempiere.org
- **AI Plugin Code**: `/Users/norbertbede/github/com.cloudempiere.ai/src`
- **Docker Docs**: https://docs.docker.com/compose

## 🤝 Contributing

When adding new tools or features:

1. **Update architecture docs** (01-ARCHITECTURE.md)
2. **Add implementation** (02-IMPLEMENTATION_GUIDE.md)
3. **Follow best practices** (03-BEST_PRACTICES.md)
4. **Update deployment docs** (04-DEPLOYMENT.md)
5. **Test thoroughly** (unit + integration tests)
6. **Get approval** before merging to master

## 📝 License

This documentation and implementation are part of the iDempiere AI Plugin project.

---

**Last Updated**: 2025-11-26
**Documentation Version**: 1.0
**Status**: Ready for implementation

**Next Steps**:
1. Review [01-ARCHITECTURE.md](./01-ARCHITECTURE.md) to understand design
2. Follow [02-IMPLEMENTATION_GUIDE.md](./02-IMPLEMENTATION_GUIDE.md) to implement
3. Apply [03-BEST_PRACTICES.md](./03-BEST_PRACTICES.md) during development
4. Use [04-DEPLOYMENT.md](./04-DEPLOYMENT.md) for production
