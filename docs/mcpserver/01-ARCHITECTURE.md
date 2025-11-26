# iDempiere AI Plugin MCP Server Architecture

## Overview

This document outlines the architecture for wrapping the iDempiere AI Plugin as an MCP (Model Context Protocol) server, enabling external services and Claude Code agents to access AI-powered workflows securely.

## Current Plugin Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                    AI Plugin (OSGi Bundle)                  │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  AIConversationService (Main Orchestration Layer)    │   │
│  │  - Thread-scoped conversation management             │   │
│  │  - Multi-turn dialogue with function calling         │   │
│  │  - Intelligent data source routing                   │   │
│  └────────────┬─────────────────────────────────────────┘   │
│               │                                               │
│     ┌─────────┼──────────────────────────────────────┐       │
│     │         │                                      │       │
│  ┌──▼────┐ ┌─▼──────────┐ ┌──────────┐ ┌──────────┐ │       │
│  │IAIProvider│   Context  │ Database  │  Routing  │ │       │
│  │Factory   │ Providers  │ Function  │ & Cache   │ │       │
│  │          │ Registry   │ Handler   │           │ │       │
│  └──┬────┘ └────────────┘ └──────────┘ └──────────┘ │       │
│     │                                                 │       │
│  ┌──▼────────────────────────────────┐               │       │
│  │  AI Provider Implementations       │               │       │
│  │  - AnthropicProvider (Claude)      │               │       │
│  │  - AWSBedrockProvider              │               │       │
│  └────────────────────────────────────┘               │       │
│                                                       │       │
└───────────────────────────────────────────────────────┘       │
        │                                   │
        │ Uses iDempiere Core              │
        ▼                                   ▼
   ┌─────────────┐                  ┌──────────────┐
   │ iDempiere   │                  │  Database    │
   │ Models      │                  │  Security    │
   └─────────────┘                  │  & Audit     │
                                    └──────────────┘
```

## MCP Server Wrapper Architecture

### Design Pattern: Facade Pattern

The MCP server acts as a facade, exposing the plugin's capabilities through MCP tools while maintaining:
- **Security**: Role-based access control from iDempiere
- **Auditability**: Query audit logging
- **Context isolation**: Thread-scoped conversations
- **Reliability**: Error handling and rate limiting

### MCP Tool Boundaries

```
External Service (e.g., Claude Code Agent)
    │
    └─── MCP Client Connection
         │
         ▼
    ┌────────────────────────────────────────────┐
    │     iDempiere AI MCP Server                │
    ├────────────────────────────────────────────┤
    │                                             │
    │  1. chat_with_context                      │
    │     └─> AIConversationService              │
    │        - Send message                      │
    │        - Manage history                    │
    │        - Function calling                  │
    │                                             │
    │  2. query_database                         │
    │     └─> AIDatabaseFunctionHandler          │
    │        - Execute SELECT queries            │
    │        - Apply row limits                  │
    │        - Log to audit table                │
    │                                             │
    │  3. extract_context                        │
    │     └─> IAIContextProvider Registry        │
    │        - Window context                    │
    │        - Chart context                     │
    │        - User/Role context                 │
    │                                             │
    │  4. get_provider_info                      │
    │     └─> AIProviderFactory                  │
    │        - Available models                  │
    │        - Model capabilities                │
    │        - Health status                     │
    │        - Rate limits                       │
    │                                             │
    │  5. get_conversation_history               │
    │     └─> MAIChat/MAIChatEntry               │
    │        - Load history                      │
    │        - Manage depth                      │
    │                                             │
    └────────────────────────────────────────────┘
         │
         └─── iDempiere OSGi Context
              - Access existing models
              - Use security framework
              - Query database
              - Log audit trail
```

## Key Architectural Decisions

### 1. **Access to iDempiere Context**

**Challenge**: MCP server runs as separate process; needs access to iDempiere context (Properties, MRole, etc.)

**Solutions**:

**Option A: OSGi Service (Recommended)**
- MCP server runs as OSGi service within iDempiere JVM
- Direct access to Properties, MRole, database
- No network overhead
- Challenge: Complex deployment
- **Benefit**: Reuses all existing security infrastructure

**Option B: HTTP/REST API**
- iDempiere exposes REST API endpoint for AI operations
- MCP server calls HTTP API
- Simpler deployment, easier versioning
- Challenge: Network overhead, authentication complexity
- **Benefit**: Loosely coupled, easier to scale

**Option C: Message Queue (RabbitMQ/ActiveMQ)**
- Async request/response pattern
- Decoupled communication
- Better for high load
- Challenge: More complex orchestration

**Recommended Approach**: Option B (HTTP/REST) for Phase 1
- Cleaner separation of concerns
- Easier to test and deploy independently
- Can extend with options A/C later if needed

### 2. **Security Model**

```
External Service Request
    │
    ▼
[MCP Server Authentication]
    │ Validates MCP client (API key, OAuth, etc.)
    │
    ▼
[iDempiere HTTP Endpoint]
    │ Validates iDempiere user/role (session/token)
    │
    ▼
[AIConversationService]
    │ Enforces permissions via MRole
    │ AI queries execute with: AI User ID + Logged User Role
    │
    ▼
[Database Layer]
    │ Row-level security via AccessSqlParser
    │ Sensitive field redaction
    │ Audit logging (AIG_QueryAudit)
    │
    ▼
Response (sanitized, audited)
```

**Key Principles**:
- No elevation of privilege (AI can't access more than the user)
- All queries logged with both AI user and initiating user
- Sensitive fields automatically redacted
- Configurable max row limits per query

### 3. **Context & State Management**

```
Conversation Session
    │
    ├─ Thread ID (uniqueness)
    │
    ├─ ConversationContextManager
    │  ├─ TTL cache (per data type)
    │  ├─ LRU eviction (50 entries default)
    │  └─ Thread-scoped isolation
    │
    ├─ Message History
    │  ├─ Stored in DB (MAIChat/MAIChatEntry)
    │  ├─ Customizable depth (default: 10)
    │  └─ Includes function calls & results
    │
    └─ Routing Decision
       ├─ PromptAnalyzer evaluates source
       ├─ OPTIONS: DATABASE_ONLY, CONTEXT_ONLY, HYBRID
       └─ Intelligent caching of results
```

### 4. **Function Calling Flow**

```
User Message
    │
    ▼
[AIConversationService.sendMessageWithContext()]
    │
    ├─ Load history
    ├─ Build system prompt (with routing hints)
    ├─ Send to AI provider with available functions
    │
    ▼
[AI Provider Response]
    │ Contains: content + function_calls
    │
    ▼
[Process Function Calls] (up to depth 5)
    │
    ├─ query_database
    │  └─> [AIDatabaseFunctionHandler]
    │      ├─ Parse SQL
    │      ├─ Check role permissions
    │      ├─ Execute with timeout (5s default)
    │      ├─ Log to AIG_QueryAudit
    │      └─ Cache result
    │
    └─ [Send results back to AI]
        │
        ▼
    [AI Refines response]
        │
        ▼
[Final Response] → Save to DB → Return to MCP Client
```

## MCP Server Implementation Strategy

### Phase 1: Java-Based MCP Server (Recommended)

**Why Java?**
- Direct access to iDempiere OSGi context (if deployed as bundle)
- Leverage existing AI plugin code
- Reuse DTO classes (AIRequest, AIResponse, etc.)
- Type-safe integration

**Architecture**:

```java
// MCP Server (Java)
com/cloudempiere/ai/mcp/
├── MCPServer.java              // StdIO transport handler
├── tool/
│   ├── ChatWithContextTool.java
│   ├── QueryDatabaseTool.java
│   ├── ExtractContextTool.java
│   ├── GetProviderInfoTool.java
│   └── GetConversationHistoryTool.java
├── dto/
│   ├── MCPRequest.java
│   └── MCPResponse.java
├── transport/
│   ├── StdIOTransport.java
│   └── TransportHandler.java
└── Activator.java              // OSGi bundle registration
```

**Benefits**:
- Zero serialization overhead (use Java objects directly)
- Access to iDempiere context via Properties
- Inherit plugin's error handling and logging
- Share DTO classes with plugin

### Phase 2: Lightweight HTTP API

```java
com/cloudempiere/ai/http/
├── AIChatController.java
├── AIDatabaseQueryController.java
├── AIContextController.java
└── AIProviderController.java
```

This allows:
- REST API for non-OSGi deployments
- MCP server calls HTTP endpoints
- Standard Spring Web or iDempiere's native servlet support

## Data Model Integration

### Existing Tables to Leverage

```
Database Schema

AIG_Provider
├─ AD_Org_ID (multi-tenant)
├─ AD_User_ID (AI user)
├─ Name (provider name)
├─ Type (ANTHROPIC, BEDROCK, etc.)
├─ IsActive
└─ ... (API key, endpoint, config)

CM_Chat (extends iDempiere standard)
├─ CM_Chat_ID
├─ AD_User_ID (conversation owner)
├─ AD_Org_ID
├─ Created/Updated timestamps
├─ Subject
└─ Type = 'AI'

CM_ChatEntry (extends standard)
├─ CM_Chat_ID (FK)
├─ CM_ChatEntry_ID
├─ Line
├─ ChatMessage
├─ Created_by
├─ IsAIResponse (new field)
└─ Response_Data (JSON)

AIG_QueryAudit (new table)
├─ AIG_QueryAudit_ID
├─ AD_Org_ID
├─ AI_User_ID (who made the request)
├─ AD_User_ID (actual user)
├─ ExecutedSQL
├─ ExecutionTime
├─ RowCount
├─ Purpose
└─ Created/Updated
```

### New Tables for MCP

```
AIG_MCP_Session (to track MCP conversations)
├─ AIG_MCP_Session_ID
├─ AD_Org_ID
├─ AD_User_ID (external service user)
├─ External_Service_ID (identifier of MCP client)
├─ Session_Key (UUID)
├─ CM_Chat_ID (FK to conversation)
├─ Created/Updated
├─ IsActive

AIG_MCP_AuditLog (detailed MCP operation logging)
├─ AIG_MCP_AuditLog_ID
├─ AIG_MCP_Session_ID (FK)
├─ Operation (chat, query, context_extract, etc.)
├─ Input_JSON
├─ Output_JSON
├─ Status (success, error)
├─ Error_Message
├─ Execution_Time_Ms
└─ Created
```

## Integration Points with Existing Plugin

### Reusable Components

1. **AIConversationService** - Core orchestration
   - Minimal changes needed
   - Add MCP-specific context parameter
   - Output in MCP-compatible format

2. **AIDatabaseFunctionHandler** - Query execution
   - Already security-hardened
   - Already does audit logging
   - Output DAO to JSON conversion needed

3. **IAIContextProvider & Registry** - Context extraction
   - Already abstracts context sources
   - MCP tools can call directly
   - Add new provider for external context?

4. **AIProviderFactory** - Provider management
   - Already handles provider selection
   - Health checks and capabilities available
   - Expose as MCP tool info

5. **ConversationContextManager** - Caching
   - Thread-scoped by design
   - MCP session = thread context
   - TTL cache already optimized

### Changes Required

| Component | Change | Scope |
|-----------|--------|-------|
| AIRequest | Add `mcp_session_id` optional field | Minor |
| AIResponse | Include `conversation_id`, timestamps | Minor |
| ConversationContextManager | Add session isolation option | Minor |
| AIDatabaseFunctionHandler | Add `mcp_mode` flag for output format | Minor |
| New: MCPTransport | Implement StdIO/SSE handler | New module |
| New: MCPToolRegistry | Wrap plugin capabilities as MCP tools | New module |

## Security Considerations

### Authentication & Authorization

```
MCP Request Flow
    │
    ├─ [MCP Client Auth]
    │  │ API Key / OAuth / JWT
    │  └─ Identifies external service
    │
    └─ [iDempiere User Auth]
       │ Session / Token / Service User
       │
       └─ Identifies which iDempiere user's permissions to use
          (could be "MCP_SERVICE_USER" or mapped from external identity)
```

### Permission Escalation Prevention

```
Principle: AI cannot do more than the user

Example:
- User: Sales Manager (can see own org's orders)
- MCP Request: "Show all orders in system"
- Result: Only returns orders visible to Sales Manager's org

Implementation:
1. Parse request in context of iDempiere user
2. Apply MRole.get(AD_User_ID) to all queries
3. AccessSqlParser enforces where clauses
4. Audit log shows both AI user and initiating user
```

### Data Sensitivity

```
Automatic Redaction

Sensitive Fields (auto-hidden):
PASSWORD, USERPIN, CREDITCARD*, CVV, SSN, TAXID, BANKACCOUNT*, IBAN, APIKEY, TOKEN, SECRET, SALT, LDAP*, PRIVATEKEY, CERTIFICATE

Configuration:
- Org-level sensitivity settings
- Per-user role sensitivity overrides
- Audit log of what was accessed
```

## Performance Considerations

### Caching Strategy

```
ConversationContextManager (already implemented)

Cache Layers:
1. Thread-scoped (per conversation)
2. TTL-based eviction (per data type)
3. LRU capacity (50 entries default)
4. Query result caching (1 hour default)

MCP Impact:
- Each MCP session = separate thread context
- Minimal cross-conversation interference
- TTL prevents stale data
- Configurable via AIG_Prompt_Config
```

### Timeout & Rate Limiting

```
Current Implementations:

Query Timeout: 5 seconds (database queries)
Request Timeout: 30 seconds (AI API calls)
Rate Limiting: Per AIG_Provider configuration

MCP Enhancement:
- Add per-session rate limiting
- Add per-external-service quota
- Configurable via AIG_MCP_Config table
```

## Deployment Options

### Option 1: Embedded MCP Server (OSGi Bundle)

```
iDempiere Instance
│
├─ [AI Plugin Bundle] (existing)
│
└─ [MCP Server Bundle] (new)
   │
   ├─ Direct OSGi service access
   ├─ Shares same JVM and Properties
   └─ HTTP endpoint exposed (localhost:8080/ai/mcp)

External Service
│
└─ HTTP connection to: localhost:8080/ai/mcp
   (runs MCP protocol over HTTP/SSE)
```

### Option 2: Standalone MCP Server

```
iDempiere Instance
│
└─ [HTTP API Endpoint] (e.g., /api/ai/*)
   (standard REST endpoints)

Standalone MCP Server Process
│
├─ Reads HTTP API endpoint from config
├─ Translates MCP tools to HTTP calls
└─ Exposes: stdio/SSE/HTTP transport

External Service
│
└─ Connects to: Standalone Server (stdio or HTTP)
```

### Option 3: Containerized (Recommended for Production)

```
Docker Compose:

- iDempiere Container (port 8080)
- MCP Server Container (port 9000)
- PostgreSQL Container (port 5432)

MCP Server:
- Environment variables for iDempiere endpoint
- Health checks for availability
- Restart policy: always
```

## Testing Strategy

### Unit Tests (Java)

```java
// Test Tool Input Validation
@Test
void testChatWithContextTool_InvalidInput() { ... }

// Test Database Query Isolation
@Test
void testQueryDatabaseTool_RolePermissions() { ... }

// Test Context Extraction
@Test
void testExtractContextTool_WindowContext() { ... }

// Test Error Handling
@Test
void testChatWithContextTool_NetworkError() { ... }
```

### Integration Tests

```java
// Test with actual iDempiere database
@Test
void testE2E_ChatFlow_WithDatabaseQuery() {
    // 1. Send message requiring DB query
    // 2. Verify function call generated
    // 3. Execute query via AIDatabaseFunctionHandler
    // 4. Verify audit log created
    // 5. Verify response formatted correctly
}
```

### MCP Compliance Tests

- Use MCP inspector to validate tool schemas
- Test tool discovery and capabilities
- Verify error message format
- Validate response structure

## Benefits of MCP Wrapper

### For External Services

| Benefit | Details |
|---------|---------|
| **AI-Powered Operations** | Access AI-driven features from any system |
| **Standardized Interface** | MCP protocol = consistent with Claude Code agents |
| **Conversation Management** | Multi-turn dialogue with history |
| **Secure Queries** | Row-level security, audit logging |
| **Context Awareness** | Window/chart context from iDempiere |

### For iDempiere

| Benefit | Details |
|---------|---------|
| **Extended Reach** | iDempiere AI accessible to external tools |
| **No Code Changes** | Wrapper around existing plugin |
| **Reuses Infrastructure** | Security, audit, caching, routing |
| **Extensible** | New providers easily integrated |
| **Monitored** | All operations logged and auditable |

### For Developers

| Benefit | Details |
|---------|---------|
| **Composable** | MCP tools can be combined with others |
| **Discoverable** | Tool metadata available to agents |
| **Type-Safe** | Schema validation on inputs |
| **Well-Tested** | Comprehensive test suite included |
| **Well-Documented** | Clear examples and best practices |

## Next Steps

1. **Phase 1**: Implement HTTP API layer in iDempiere
2. **Phase 2**: Implement MCP server (Java or Node.js)
3. **Phase 3**: Create evaluation suite
4. **Phase 4**: Deploy and monitor

See `02-IMPLEMENTATION_GUIDE.md` for detailed implementation steps.
