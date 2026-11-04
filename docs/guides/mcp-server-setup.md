# Connecting an MCP client to the iDempiere AI tools

> **Status:** written from the code (CLD-2542, merged). The end-to-end flow has **not yet been run on a dev server**.
> Check the steps marked ⚠ the first time and fix this guide.

The `com.cloudempiere.ai.mcp` bundle serves an MCP server at `POST https://<server>/ai-mcp`. MCP clients such as
Claude Code, Claude Desktop, Cursor, n8n or your own agents can call the AI assistant's tools: SQL queries and
lookups (`ERPTools`), plus knowledge search (`RagTools`) when RAG is set up. Design: [ADR-003](../adr/003-mcp-server-integration.md).

## Access model

The MCP server adds no tables of its own. Who may call it, and what they see, comes from records that already exist:

```
 MCP client (Claude Code, n8n, an agent)
        │  Authorization: Bearer <token>
        ▼
┌─────────────────────────────┐
│ AD_Authorization_AppClient  │  1. who is calling: the registered app client
└─────────────┬───────────────┘
              │ AD_Role_AppClientAccess
              ▼
┌─────────────────────────────┐
│ AD_Role + Mapped_AD_User_ID │  2. runs as this service user and role
└─────────────┬───────────────┘
              │ the role's table, column and record access
              ▼
┌─────────────────────────────┐
│ tables                      │  3. SecureDatabaseQueryExecutor enforces it on every query
└─────────────┬───────────────┘
              ▼
┌─────────────────────────────┐
│ tools                       │  4. today: all tools (all read-only); per-tool permissions are planned
└─────────────────────────────┘
```

Every request goes through these checks, in this order:

| Check | Fails with |
|---|---|
| `Origin` header, when sent, is listed in `AI_MCP_ALLOWED_ORIGINS` | 403 |
| Bearer token resolves to an iDempiere context (`RequestContextService`, the same as GraphQL) | 401 |
| The server is switched on (POC: `McpServlet.SERVER_ENABLED`, hardcoded `true`) | 404 |
| The role has AI access: `AD_Role.AIAccessLevel` and `AIG_Provider_Access` (ADR-058) | 403 |
| Body ≤ `AI_MCP_MAX_REQUEST_BYTES` (default 1 MB) | 413 |

## Setup

### 1. Install and enable

1. Install the `com.cloudempiere.ai.feature` feature, which includes `com.cloudempiere.ai.mcp`. ⚠ Check in the OSGi
   console that the bundle is `ACTIVE`, and in the log that the line
   `AI MCP server endpoint ready at /ai-mcp` appears.
2. Nothing to switch on: for the POC the endpoint is enabled by a constant (`McpServlet.SERVER_ENABLED = true`).
   It becomes the per-tenant AD_SysConfig setting `AI_MCP_SERVER_ENABLED` later. Access still needs a valid token
   and a role with AI access.

### 2. A role for the MCP caller

Create or pick the role that MCP calls run as:

- **Table access:** give it only what the agent should read. Every query runs under this role's access, so this is
  the real data boundary.
- **AI access:** set `AIAccessLevel` to `A` (All), or to `R` (User/Role access) with an `AIG_Provider_Access` row for
  the role. With `N` every call gets 403.

### 3. The caller's identity

Pick one of these.

**a) A machine client (n8n, a backend agent): Cognito client_credentials (recommended)**

Set it up with the identity playbook
[M2M Onboarding via Cognito Federation](https://github.com/cloudempiere/com.cloudempiere.identity/blob/main/docs/guides/m2m-cognito-federation.md)
(ADR-0023, ADR-0028). It creates the `REST_OIDCService` row, the service user and the `AD_Role_AppClientAccess`
row linking the app client to the role from step 2 (`Mapped_AD_User_ID`).

At runtime the client gets a token from Cognito and sends it as is:

```bash
TOKEN=$(curl -s -X POST "$COGNITO_TOKEN_URL/oauth2/token" \
  -d grant_type=client_credentials -d client_id=$CLIENT_ID -d client_secret=$CLIENT_SECRET \
  -d scope="$SCOPES" | jq -r .access_token)
```

⚠ The playbook describes the REST path (`CLDERequestFilter`). `/ai-mcp` resolves the same token through
`RequestContextService` → `DefaultTokenAuthenticator`, as GraphQL does. Check the first run against Part 6
*Verification* of the playbook.

Use resource-server scopes that match the access level (`api.read`, or `api.read api.write`), as ADR-0027 requires.
They aren't enforced yet, but per-tool permissions will build on them.

**b) A person using Claude Code or Cursor: their own login token**

⚠ Log in through the REST login (`/api/v1/auth/tokens`), which issues a local token listed in `AD_AuthorizationToken`. The calls then run
as that person with the role they chose at login. These tokens expire (`ValidTo`), so the header must be refreshed
when they do.

**c) POC: a person logs in with their own PIN (fastest to set up)**

Identity's token endpoint already issues tokens for a PIN (`grant_type=pin`, `TokenPinGrantTypeHandler`). Nothing
new has to be built:

1. An app client (`AD_Authorization_AppClient`) whose `AD_Role_AppClientAccess` gives the role from step 2.
2. The user has a PIN (identity's `PinSetup` process).
3. Get a token:

```bash
curl -s -X POST https://<host>/auth/token \
  -d grant_type=pin -d client_id=<app client> -d username=<e-mail> -d pin=<PIN>
# → { "access_token": "...", "expires_in": 1800, "refresh_token": "..." }
```

`client_secret` is needed only for a confidential app client. Without `username` the PIN is looked up within the
tenant (PIN-only mode). Access tokens live **30 minutes** and refresh tokens 24 hours (constants in `OauthUtils`), so a
pasted token stops working after half an hour. Let Claude Code fetch it instead with
[`scripts/idempiere-mcp-token.sh`](../../scripts/idempiere-mcp-token.sh):

```bash
export IDEMPIERE_URL=https://<host> IDEMPIERE_CLIENT_ID=<app client> IDEMPIERE_USER=<e-mail> IDEMPIERE_PIN=<PIN>
```

```json
{ "mcpServers": { "idempiere": {
    "type": "http",
    "url": "https://<host>/ai-mcp",
    "headersHelper": "/path/to/com.cloudempiere.ai/scripts/idempiere-mcp-token.sh"
} } }
```

⚠ `headersHelper` (a command that prints the headers, run when Claude Code connects) is not yet verified against the
current Claude Code. The fallback is a pasted token, renewed every 30 minutes:
`claude mcp add --transport http idempiere https://<host>/ai-mcp --header "Authorization: Bearer <token>"`.

POC only: the PIN sits in an environment variable. The production path is the browser login (authorization code +
PKCE) once the identity login portal exists (identity ADR-0010).

### 4. Connect the client

**Claude Code**

```bash
claude mcp add --transport http idempiere https://<server>/ai-mcp \
  --header "Authorization: Bearer $TOKEN"
```

**Any client, with curl**

```bash
curl -s https://<server>/ai-mcp -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}'

curl -s https://<server>/ai-mcp -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'

curl -s https://<server>/ai-mcp -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"listTables","arguments":{}}}'
```

**MCP Inspector:** `npx @modelcontextprotocol/inspector`, transport *Streamable HTTP*, URL `https://<server>/ai-mcp`,
header `Authorization: Bearer <token>`.

## Verification checklist

- [ ] `initialize` returns `serverInfo.name = cloudempiere-ai`
- [ ] `tools/list` lists `listTables`, `getTableMetadata`, `queryDatabase`, …
- [ ] `tools/call listTables` returns only tables the role may read
- [ ] the server log has a WARNING line `MCP tools/call listTables OK …ms user=… role=… client=…`
- [ ] a role with `AIAccessLevel=N` → 403
- [ ] no or expired token → 401 with `WWW-Authenticate: Bearer`

## Troubleshooting

| Symptom | Cause |
|---|---|
| 404 `MCP server is not enabled` | the build has `McpServlet.SERVER_ENABLED = false` |
| 401 | token missing, expired or revoked; for Cognito see Part 5 of the identity playbook (`REST_OIDCService` row, `AD_Authorization_AppClient_ID` FK) |
| 403 `You do not have access to the AI assistant` | the role's `AIAccessLevel` is `N`, or `R` without an `AIG_Provider_Access` row |
| 403 `Origin not allowed` | a browser-based client sends `Origin`; add it to `AI_MCP_ALLOWED_ORIGINS` (comma-separated) |
| tool result `isError: true` | the tool failed, for example the SQL was rejected for the role; the text says why |
