#!/bin/sh
# Prints the Authorization header JSON for the iDempiere AI MCP server (/ai-mcp), for Claude Code's
# "headersHelper". Gets a fresh access token from identity with the user's PIN (grant_type=pin).
# Access tokens live 30 minutes, and Claude Code runs this again when it connects.
#
# POC only: the PIN sits in an environment variable. See docs/guides/mcp-server-setup.md.
#
# Required:  IDEMPIERE_URL        e.g. https://erp.example.com (no trailing slash)
#            IDEMPIERE_CLIENT_ID  OAuth client id of an AD_Authorization_AppClient
#            IDEMPIERE_PIN        the user's PIN
# Optional:  IDEMPIERE_USER          e-mail; without it the PIN is looked up within the app client's tenant
#            IDEMPIERE_CLIENT_SECRET only for a confidential app client
# Needs curl and jq.
set -eu

: "${IDEMPIERE_URL:?set IDEMPIERE_URL}"
: "${IDEMPIERE_CLIENT_ID:?set IDEMPIERE_CLIENT_ID}"
: "${IDEMPIERE_PIN:?set IDEMPIERE_PIN}"

set -- -d grant_type=pin --data-urlencode "client_id=$IDEMPIERE_CLIENT_ID" --data-urlencode "pin=$IDEMPIERE_PIN"
if [ -n "${IDEMPIERE_USER:-}" ]; then
  set -- "$@" --data-urlencode "username=$IDEMPIERE_USER"
fi
if [ -n "${IDEMPIERE_CLIENT_SECRET:-}" ]; then
  set -- "$@" --data-urlencode "client_secret=$IDEMPIERE_CLIENT_SECRET"
fi

RESPONSE=$(curl -sS -X POST "$IDEMPIERE_URL/auth/token" "$@")
TOKEN=$(printf '%s' "$RESPONSE" | jq -r '.access_token // empty')
if [ -z "$TOKEN" ]; then
  # The response holds no secret when login failed; show its error fields only
  echo "idempiere-mcp-token: no access token: $(printf '%s' "$RESPONSE" | jq -c '{error, error_description}' 2>/dev/null || echo "$RESPONSE")" >&2
  exit 1
fi

printf '{"Authorization":"Bearer %s"}\n' "$TOKEN"
