# CloudEmpiere AI Protocol Specification v1.0

**Status:** DRAFT
**Date:** 2025-12-11
**Authors:** CloudEmpiere AI Team

---

## Overview

The **CloudEmpiere AI Protocol** is a RESTful API specification designed for AI-powered ERP systems. Unlike generic LLM APIs (OpenAI, Anthropic), this protocol is purpose-built for:

1. **Multi-tenant ERP environments** with role-based access control
2. **Dynamic provider routing** across multiple LLM providers
3. **Enterprise audit requirements** with full traceability
4. **Structured business operations** (database queries, process execution)

---

## Design Principles

### 1. Security First
- Every request carries complete security context
- Role-based access enforced at protocol level
- Audit trail is mandatory, not optional
- No anonymous requests

### 2. Provider Agnostic
- Clear separation between provider selection and model selection
- Support for multiple providers in single deployment
- Fallback and routing strategies built-in

### 3. ERP Native
- First-class support for database operations
- Process execution and workflow integration
- Window/record context awareness
- Multi-language support

### 4. Backward Compatible
- API versioning in URL path
- Extensions via optional fields
- Deprecation warnings before removal

---

## Base URL

```
https://satellite.cloudempiere.com/ai/v1
```

**Local Development:**
```
http://localhost:8090/ai/v1
```

---

## Authentication

### Bearer Token (Session-Based)

**Header:**
```http
Authorization: Bearer <idempiere_session_token>
```

The session token encodes:
- AD_Client_ID
- AD_User_ID
- AD_Role_ID
- AD_Session_ID
- Expiration timestamp

**Token Format:** JWT signed with satellite service secret

---

## Core Endpoints

### 1. Chat Completion

**Endpoint:** `POST /ai/v1/chat`

#### Request

```json
{
  "request_id": "req-550e8400-e29b-41d4-a716-446655440000",
  "version": "1.0",

  "security": {
    "client_id": 1000000,
    "org_id": 1000000,
    "user_id": 100,
    "role_id": 102,
    "session_id": "sess-abc123",
    "language": "en_US"
  },

  "provider": {
    "type": "anthropic",
    "model": "claude-sonnet-4",
    "fallback": {
      "type": "openai",
      "model": "gpt-4o"
    }
  },

  "conversation": {
    "id": "conv-789",
    "messages": [
      {
        "role": "system",
        "content": "You are an ERP assistant."
      },
      {
        "role": "user",
        "content": "Show me open sales orders"
      }
    ],
    "context": {
      "window_id": 143,
      "tab_id": 187,
      "record_id": 1000050,
      "table_name": "C_Order"
    }
  },

  "parameters": {
    "temperature": 0.7,
    "max_tokens": 4096,
    "top_p": 0.95,
    "stream": true
  },

  "capabilities": {
    "database_query": true,
    "process_execution": false,
    "record_modification": false,
    "tools": [
      {
        "type": "database_query",
        "name": "query_sales_orders",
        "description": "Query sales orders with role-based access",
        "parameters": {
          "type": "object",
          "properties": {
            "sql": {"type": "string"},
            "max_rows": {"type": "integer"}
          }
        }
      }
    ]
  }
}
```

#### Response (Non-Streaming)

```json
{
  "request_id": "req-550e8400-e29b-41d4-a716-446655440000",
  "response_id": "resp-660e8400-e29b-41d4-a716-446655440001",
  "version": "1.0",
  "timestamp": "2025-12-11T11:30:00Z",

  "result": {
    "content": "I found 15 open sales orders:\n\n[List of orders...]",
    "finish_reason": "complete",
    "role": "assistant"
  },

  "provider": {
    "type": "anthropic",
    "model": "claude-sonnet-4",
    "used_fallback": false
  },

  "operations": [
    {
      "type": "database_query",
      "tool": "query_sales_orders",
      "input": {
        "sql": "SELECT DocumentNo, GrandTotal FROM C_Order WHERE DocStatus='DR'"
      },
      "output": {
        "rows_returned": 15,
        "execution_ms": 45
      },
      "timestamp": "2025-12-11T11:30:01Z"
    }
  ],

  "usage": {
    "prompt_tokens": 150,
    "completion_tokens": 89,
    "total_tokens": 239,
    "cost_usd": 0.0012,
    "cache_read_tokens": 0,
    "cache_write_tokens": 0
  },

  "audit": {
    "executed_by": "satellite-service",
    "on_behalf_of": {
      "user_id": 100,
      "user_name": "admin",
      "role_id": 102,
      "role_name": "System Administrator"
    },
    "client_id": 1000000,
    "org_id": 1000000,
    "conversation_id": "conv-789",
    "duration_ms": 1250
  }
}
```

#### Response (Streaming)

**Content-Type:** `text/event-stream`

```
event: start
data: {"request_id":"req-550e8400","response_id":"resp-660e8400","provider":{"type":"anthropic","model":"claude-sonnet-4"}}

event: content
data: {"delta":"I found"}

event: content
data: {"delta":" 15"}

event: content
data: {"delta":" open"}

event: tool_call
data: {"tool":"query_sales_orders","status":"executing","input":{"sql":"SELECT..."}}

event: tool_result
data: {"tool":"query_sales_orders","status":"completed","output":{"rows_returned":15,"execution_ms":45}}

event: content
data: {"delta":" sales orders"}

event: done
data: {"finish_reason":"complete","usage":{"total_tokens":239,"cost_usd":0.0012},"audit":{"duration_ms":1250}}
```

---

### 2. Provider Management

#### List Available Providers

**Endpoint:** `GET /ai/v1/providers`

**Response:**
```json
{
  "providers": [
    {
      "type": "anthropic",
      "name": "Anthropic Claude",
      "status": "healthy",
      "models": [
        {
          "id": "claude-sonnet-4",
          "name": "Claude 3.5 Sonnet",
          "capabilities": ["chat", "tools", "vision"],
          "max_tokens": 8192,
          "cost_per_1k_input": 0.003,
          "cost_per_1k_output": 0.015
        }
      ]
    },
    {
      "type": "openai",
      "name": "OpenAI",
      "status": "healthy",
      "models": [
        {
          "id": "gpt-4o",
          "name": "GPT-4 Optimized",
          "capabilities": ["chat", "tools", "vision"],
          "max_tokens": 4096,
          "cost_per_1k_input": 0.005,
          "cost_per_1k_output": 0.015
        }
      ]
    }
  ]
}
```

#### Get Provider Health

**Endpoint:** `GET /ai/v1/providers/{type}/health`

**Response:**
```json
{
  "provider": "anthropic",
  "status": "healthy",
  "latency_ms": 150,
  "last_check": "2025-12-11T11:30:00Z",
  "rate_limit": {
    "requests_remaining": 9850,
    "tokens_remaining": 985000,
    "reset_at": "2025-12-11T12:00:00Z"
  }
}
```

---

### 3. Conversation Management

#### Get Conversation History

**Endpoint:** `GET /ai/v1/conversations/{conversation_id}`

**Response:**
```json
{
  "conversation_id": "conv-789",
  "created_at": "2025-12-11T10:00:00Z",
  "updated_at": "2025-12-11T11:30:00Z",
  "client_id": 1000000,
  "user_id": 100,
  "messages": [
    {
      "role": "user",
      "content": "Show me open sales orders",
      "timestamp": "2025-12-11T11:30:00Z"
    },
    {
      "role": "assistant",
      "content": "I found 15 open sales orders...",
      "timestamp": "2025-12-11T11:30:01Z",
      "provider": "anthropic",
      "model": "claude-sonnet-4"
    }
  ],
  "total_tokens": 1250,
  "total_cost_usd": 0.045
}
```

---

### 4. Embeddings

**Endpoint:** `POST /ai/v1/embeddings`

**Request:**
```json
{
  "request_id": "req-770e8400",
  "security": {
    "client_id": 1000000,
    "user_id": 100,
    "role_id": 102
  },
  "provider": {
    "type": "openai",
    "model": "text-embedding-3-small"
  },
  "input": [
    "Product: Laptop Dell XPS 15",
    "Product: iPhone 15 Pro Max"
  ]
}
```

**Response:**
```json
{
  "request_id": "req-770e8400",
  "embeddings": [
    {
      "index": 0,
      "embedding": [0.123, -0.456, ...],
      "dimensions": 1536
    },
    {
      "index": 1,
      "embedding": [0.789, -0.012, ...],
      "dimensions": 1536
    }
  ],
  "provider": {
    "type": "openai",
    "model": "text-embedding-3-small"
  },
  "usage": {
    "total_tokens": 20,
    "cost_usd": 0.00002
  }
}
```

---

## Security Model

### Request Authentication Flow

```
1. iDempiere authenticates user → creates AD_Session
2. iDempiere requests session token from Satellite
3. Satellite validates user/role → issues JWT token
4. iDempiere includes token in Authorization header
5. Satellite validates token → extracts security context
6. Satellite enforces role-based access on operations
7. Satellite logs audit trail
```

### Role-Based Access Control

Every operation checks:
- Does role have access to the table/window?
- Does role have access to the specific record?
- Does role have permission for the operation (read/write)?

### Audit Trail

Every request generates audit entry:
```sql
INSERT INTO AIG_Audit (
  AIG_Audit_ID,
  AD_Client_ID,
  AD_Org_ID,
  AD_User_ID,
  AD_Role_ID,
  RequestID,
  Provider,
  Model,
  PromptTokens,
  CompletionTokens,
  CostUSD,
  DurationMS,
  Created
) VALUES (...);
```

---

## Provider Routing

### Routing Strategies

1. **Explicit Provider** - Request specifies provider type
2. **Model-Based** - Route by model name pattern
3. **Cost-Based** - Choose cheapest provider for capability
4. **Load-Based** - Balance across providers
5. **Tenant-Based** - Per-client provider configuration

### Fallback Strategy

If primary provider fails:
1. Check if fallback specified in request
2. Check tenant-level fallback configuration
3. Check global fallback configuration
4. Return error if no fallback available

---

## Error Handling

### Error Response Format

```json
{
  "error": {
    "code": "PROVIDER_UNAVAILABLE",
    "message": "Anthropic API is temporarily unavailable",
    "type": "provider_error",
    "retry_after_ms": 5000,
    "fallback_available": true,
    "fallback_provider": "openai"
  },
  "request_id": "req-550e8400",
  "timestamp": "2025-12-11T11:30:00Z"
}
```

### Error Codes

| Code | HTTP Status | Description | Retryable |
|------|-------------|-------------|-----------|
| `INVALID_TOKEN` | 401 | Session token invalid or expired | No |
| `ACCESS_DENIED` | 403 | Role lacks permission for operation | No |
| `PROVIDER_UNAVAILABLE` | 503 | Provider temporarily unavailable | Yes |
| `RATE_LIMIT_EXCEEDED` | 429 | Rate limit exceeded | Yes |
| `MODEL_NOT_FOUND` | 404 | Requested model not available | No |
| `INVALID_REQUEST` | 400 | Malformed request | No |
| `CONTEXT_LENGTH_EXCEEDED` | 400 | Message too long for model | No |

---

## Versioning

### URL Versioning

- Version in path: `/ai/v1/chat`
- Major version only (v1, v2, etc.)
- Breaking changes require new major version
- Non-breaking changes within same major version

### Response Version Field

Every response includes `"version": "1.0"` to indicate exact protocol version.

### Deprecation Policy

1. New version announced 6 months before release
2. Old version supported for 12 months after new version
3. Deprecation warnings in response headers:
   ```http
   X-API-Deprecation: version=1.0; sunset=2026-12-11; link="https://docs.cloudempiere.com/ai/v2"
   ```

---

## Implementation Notes

### For iDempiere Plugin

The Java plugin should:
1. Create session token via `/ai/v1/auth/token`
2. Cache token until expiration
3. Include token in Authorization header
4. Build security context from Env.getCtx()
5. Handle streaming via callback interface

### For Satellite Service

The Quarkus service should:
1. Validate JWT token on every request
2. Extract security context and validate against iDempiere database
3. Route to appropriate provider based on strategy
4. Transform protocol to provider-specific format (OpenAI, Anthropic, etc.)
5. Log audit trail to database
6. Handle streaming with proper SSE format

---

## Comparison: CloudEmpiere vs OpenAI API

| Feature | CloudEmpiere AI | OpenAI API |
|---------|-----------------|------------|
| **Multi-tenancy** | First-class (client_id, org_id) | Not supported |
| **Role-based access** | Built-in | Not supported |
| **Audit trail** | Mandatory | Not supported |
| **Provider routing** | Dynamic, multi-provider | Single provider only |
| **ERP operations** | Native (database, process) | Not supported |
| **Security context** | Full iDempiere context | API key only |
| **Conversation tracking** | Built-in with database | Requires external storage |
| **Cost tracking** | Per client/org/user | Per API key |

---

## Migration from OpenAI-Compatible

For services currently using OpenAI format:

### Adapter Layer

```java
public class OpenAIToCloudEmpiereAdapter {
    public CloudEmpiereRequest adapt(OpenAIRequest openAI, Ctx ctx) {
        return CloudEmpiereRequest.builder()
            .security(extractSecurity(ctx))
            .provider(mapProvider(openAI.getModel()))
            .conversation(mapMessages(openAI.getMessages()))
            .parameters(mapParameters(openAI))
            .build();
    }
}
```

### Gradual Migration

1. Phase 1: Satellite accepts both protocols (CloudEmpiere + OpenAI)
2. Phase 2: iDempiere plugin migrates to CloudEmpiere protocol
3. Phase 3: OpenAI compatibility deprecated after 12 months
4. Phase 4: Remove OpenAI compatibility

---

## Reference Implementation

- **iDempiere Plugin:** `com.cloudempiere.ai` (Java 11)
- **Satellite Service:** `idempiere-cli/satelite-noro` (Quarkus + Java 17)
- **Protocol Version:** 1.0
- **Status:** DRAFT - Accepting feedback

---

*CloudEmpiere AI Protocol v1.0 | 2025-12-11*
