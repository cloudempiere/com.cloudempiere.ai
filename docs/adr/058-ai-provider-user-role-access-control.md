# ADR-058: AI Provider User/Role Access Control

## Status

Accepted

## Date

2026-02-27

## Deciders

Cloudempiere

## Context and Problem Statement

The AI chat panel is currently enabled or disabled globally per client via the active state of the default `MAIProvider`. There is no mechanism to restrict or grant access at the user or role level. Some roles (e.g. read-only users, external partners) should not have access to AI features, while others should be explicitly allowed.

## Decision Drivers

- Fine-grained access control without touching the global provider switch
- Consistency with the existing iDempiereCLDE dashboard access pattern (`AD_Role.DashboardAccessLevel` + `PA_Dashboard_Access`)
- Zero impact on existing installations (default = current open behaviour)
- Extensible to multiple providers and future permissions (e.g. read-only mode)

## Considered Options

1. Simple checkboxes on `AD_User.IsAIChatEnabled` and `AD_Role.IsAIChatEnabled`
2. `AD_Role.AIAccessLevel` + `AIG_Provider_Access` table (mirrors dashboard pattern)

## Decision Outcome

**Chosen option:** Option 2 — `AD_Role.AIAccessLevel` + `AIG_Provider_Access` table, because it follows the established iDempiereCLDE pattern, scales to multiple providers, and supports future per-provider permissions without schema changes.

### Confirmation

- `AD_Role.AIAccessLevel = 'A'` (default): existing behaviour unchanged, all users see AI chat
- `AD_Role.AIAccessLevel = 'N'`: AI chat panel not rendered for any user of that role
- `AD_Role.AIAccessLevel = 'R'`: only users/roles with an `AIG_Provider_Access` row see AI chat
- `AIChatGadgetFactory.isAvailable()` returns the correct boolean for each combination

## Pros and Cons of the Options

### Option 1 — Simple checkboxes on AD_User / AD_Role

Two boolean columns, checked directly in `AIChatGadgetFactory.isAvailable()`.

- Good, because minimal schema changes
- Bad, because not per-provider — cannot control access per provider when multiple providers exist
- Bad, because diverges from the established dashboard access pattern
- Bad, because no clean way to add future permissions (e.g. read-only mode)

### Option 2 — AD_Role.AIAccessLevel + AIG_Provider_Access table

Mirrors `AD_Role.DashboardAccessLevel` + `PA_Dashboard_Access`.

- Good, because consistent with existing iDempiereCLDE patterns
- Good, because per-provider — one row per (provider, role/user) pair
- Good, because default `'A'` preserves existing behaviour with zero migration impact
- Good, because extensible (add columns to `AIG_Provider_Access` for future permissions)
- Neutral, because slightly more implementation work than Option 1

## More Information

### AD_Role.AIAccessLevel

CHAR(1), NOT NULL, default `'A'`. Added to `AD_Role` via migration and registered in the AD (field in Role window).

| Value | Constant | Meaning |
|---|---|---|
| `'A'` | `AIACCESSLEVEL_All` | All users of this role can use AI chat (default) |
| `'R'` | `AIACCESSLEVEL_UserRoleAccess` | Only users/roles with an explicit `AIG_Provider_Access` row |
| `'N'` | `AIACCESSLEVEL_None` | No access — hard block, overrides any `AIG_Provider_Access` rows |

### AIG_Provider_Access Table

| Column | Type | Notes |
|---|---|---|
| `AIG_Provider_Access_ID` | numeric PK | |
| `AIG_Provider_Access_UU` | varchar 36 | |
| `AIG_Provider_ID` | numeric FK, mandatory | which provider |
| `AD_Role_ID` | numeric FK, nullable | role-level grant |
| `AD_User_ID` | numeric FK, nullable | user-level grant (takes precedence over role) |
| `IsActive` | CHAR 1 | standard flag |
| + standard audit columns | | |

One row = "this role/user may use this provider". Absence of a row under mode `'R'` = no access.

### MAIProviderAccess Model

Mirrors `MDashboardAccess`:

```java
// User-level takes precedence (ORDER BY AD_User_ID DESC)
public static MAIProviderAccess get(Properties ctx, int AIG_Provider_ID,
    int AD_Role_ID, int AD_User_ID, String trxName)
```

### Updated AIChatGadgetFactory.isAvailable() Logic

```
1. Active default MAIProvider must exist (existing check) → else false
2. Read current role's AIAccessLevel
3. If 'N' → return false immediately (hard block, no override)
4. If 'A' → return true (current behaviour preserved as default)
5. If 'R':
     query AIG_Provider_Access for (providerID, current role OR current user)
     if no active row found → return false
6. return true
```

### Interaction with AIG_ChatOwnership (ADR-036)

`AIG_ChatOwnership` (ADR-036) controls access to **individual chat instances** (read/write/owner per `CM_Chat_ID`). `AIG_Provider_Access` controls access to **the AI panel itself**.

These are orthogonal and do not conflict. The access check is layered:

1. **Provider level** (this ADR) — can the user open the AI panel at all?
2. **Chat level** (ADR-036) — can the user read/write/own a specific chat?

**Known edge case**: if a user's role has `AIAccessLevel = 'N'` and another user has shared a chat with them via `AIG_ChatOwnership`, the shared chat is inaccessible — the panel never renders. This is by design: `'N'` is an administrative hard block that takes precedence over chat sharing. Administrators should be aware that setting `'N'` on a role also prevents shared chats from being visible to users of that role.

### Related ADRs

- [ADR-029](029-multi-tenant-ai-access.md) - Multi-tenant AI access
- [ADR-036](036-chat-ownership-and-sharing-model.md) - Chat ownership and sharing model (AIG_ChatOwnership)
- [ADR-048](048-comprehensive-security-strategy.md) - Comprehensive security strategy
- [ADR-052](052-ai-chat-widget-core-decoupling.md) - AI Chat Widget core decoupling (introduces AIChatGadgetFactory)
