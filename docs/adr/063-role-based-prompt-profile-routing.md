# ADR-063: Role-Based Prompt Profile Routing

## Status

Phase 1 — Implemented (v0.20.0+) | Phase 2 — Planned

## Date

2026-03-12

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

Today all users share one global `SYSTEM_ADDENDUM` prompt configured in `AIG_Prompt_Config` (ADR-059). A warehouse clerk and a CFO get identical AI behavior — the same persona, domain focus, and behavioral constraints. The CE_LLM_Search ERD research (`docs/research/ce_llm_search_erd.md`) proposed 7 new tables for role-scoped, vertical-aware RAG retrieval, but deploying that entire schema before validating the core routing premise is premature. We need a minimal mechanism to deliver different AI behavior to different roles without over-engineering the schema.

## Decision Drivers

- **KISS**: Validate role→prompt routing with real users before investing in verticals, knowledge bases, and search sessions
- **Zero new tables for Phase 1**: Reuse `AIG_Provider_Access` (ADR-058) and `AIG_Prompt_Config` (ADR-059) — both already exist and are deployed
- **Backward compatibility**: Existing single-addendum installations must continue working unchanged
- **Semantic context understanding for Phase 2**: Static role-based routing cannot adapt within a session when a user crosses domains (CEO asking CRM, then sales, then inventory in one chat). The LLM is better positioned to select the right behavioral profile per message than keyword scanning or a preflight classifier
- **Extensibility**: The design must accommodate future phases without breaking the Phase 1 schema

## Considered Options

1. **New `CE_LLM_*` table family** — 7 tables from the CE_LLM_Search ERD (Provider, Vertical, SearchConfig, ContextProfile, KnowledgeBase, ContextProfile_KB junction, SearchSession)
2. **FK on `AIG_Provider_Access` + IsDefault fallback** — 3 columns on existing tables, zero new tables
3. **VARCHAR key routing** — Store a text key like `"WAREHOUSE_PROFILE"` on `AIG_Provider_Access` and match it to `AIGPromptKey` on `AIG_Prompt_Config`

## Decision Outcome

**Chosen option:** Option 2 — FK bridge + IsDefault, because it delivers role-based prompt routing with 3 column additions, zero new tables, and a deterministic resolution chain that can be implemented in a single `PromptProfileResolver` class. The CE_LLM ERD tables are preserved as the Phase 2 and Phase 3 roadmap, renamed to `AIG_*` naming to stay within the AI plugin's namespace.

### Confirmation

The decision is confirmed when:
- [ ] `AIG_Provider_Access.AIG_Prompt_Config_ID` FK column exists and is visible in the Provider Access child tab
- [ ] `AIG_Prompt_Config.IsDefault` column exists; exactly one record per client has `IsDefault = 'Y'`
- [ ] `AIG_Prompt_Config.AIGStatus` column exists with values D/A/X; only `'A'` records are loaded
- [ ] `PromptProfileResolver.resolve(ctx, providerID, roleID, userID)` returns the correct `AIG_Prompt_Config_ID` following the 3-step resolution chain
- [ ] `AIService.appendOperatorAddendum()` uses `PromptProfileResolver` instead of hardcoded `"SYSTEM_ADDENDUM"` key lookup
- [ ] Null/empty profile resolution produces identical output to current behavior (backward-compatible)
- [ ] Unit test: role with explicit FK → loads that profile's `AIGPromptText`
- [ ] Unit test: role without FK → falls back to `IsDefault = 'Y'` record
- [ ] Unit test: no default, no FK → no addendum appended (base prompt only)

## Pros and Cons of the Options

### Option 1: New CE_LLM_* table family (7 tables)

Full ERD from `docs/research/ce_llm_search_erd.md`: CE_LLM_Provider, CE_LLM_Vertical, CE_LLM_SearchConfig, CE_LLM_ContextProfile, CE_LLM_KnowledgeBase, CE_LLM_ContextProfile_KB, CE_LLM_SearchSession.

- Good, because comprehensive — covers verticals, knowledge bases, search sessions, and role × vertical matrix
- Good, because CE_LLM_ContextProfile supports `SystemPromptTemplate`, `QueryRewritePrefix`, `RoleTierMin`, `CrossVerticalAllowed`
- Bad, because 7 tables + 7 AD windows + 7 model classes + migrations before a single user tests role routing
- Bad, because CE_LLM_ prefix creates a separate namespace from the existing AIG_ tables
- Bad, because YAGNI — knowledge bases and search sessions have no consumer until RAG is implemented

### Option 2: FK on AIG_Provider_Access + IsDefault fallback (KISS) — Chosen

3 columns on 2 existing tables. `AIG_Provider_Access` gains an FK to `AIG_Prompt_Config`; `AIG_Prompt_Config` gains `IsDefault` and `AIGStatus`.

- Good, because zero new tables — schema changes are 3 `ALTER TABLE ADD COLUMN` statements
- Good, because FK routing is deterministic and type-safe (integer FK, not string matching)
- Good, because `IsDefault` is a standard iDempiere pattern (used in `C_BPartner`, `C_BankAccount`, etc.)
- Good, because backward-compatible — existing `SYSTEM_ADDENDUM` record becomes the `IsDefault = 'Y'` fallback
- Good, because `AIGStatus` supports governance workflow (Draft → Active → Archived) per ADR-059
- Neutral, because limited to role-level routing (no window/table context routing yet)
- Bad, because does not support verticals or knowledge base scoping — deferred to Phase 2/3

### Option 3: VARCHAR key routing

Store a string key (e.g., `"WAREHOUSE_PROFILE"`) on `AIG_Provider_Access.AIGPromptKey` and match it against `AIG_Prompt_Config.AIGPromptKey`.

- Good, because simple string matching, no FK constraint
- Bad, because non-deterministic — typos in keys silently fail to resolve
- Bad, because no referential integrity — orphaned keys when profiles are renamed or deleted
- Bad, because requires `LIKE`/`=` query instead of indexed FK join

## More Information

### Roadmap

```
Phase 1 (Implemented)                Phase 2 (Planned — primary)           Phase 3 (Deferred)
─────────────────────                ───────────────────────────            ──────────────────
Static role-based routing            LLM-driven per-message routing         Window/RAG context routing
0 new tables                         0 new tables                           +2 new tables

AIG_Provider_Access                  AIG_Provider_Access                    AIG_Prompt_Context
  + AIG_Prompt_Config_ID (FK)          multiple rows per role/user            ├ AIG_Prompt_Config_ID (FK)
  one profile per role                 each row = one available profile        ├ AD_Window_ID (FK, nullable)
                                                                               └ AD_Table_ID (FK, nullable)
AIG_Prompt_Config                    PromptConfigSchemaCache
  + IsDefault CHAR(1)                  session-level cache of               AIG_KnowledgeBase
  + AIGStatus CHAR(1)                  {id, name, description, key}           ├ StorageType, ConnectionRef
                                       for this role/user                      └ EmbeddingModel
PromptProfileResolver (Java)
  └ 3-step resolution chain          selectPromptProfile tool (Java)        PromptContextResolver (Java)
                                       LLM picks best profile                 └ window/table + RAG chain
                                       per message from allowed list
```

### Runtime Flow — Phase 1 (Implemented)

One profile per role. Resolved once at prompt assembly, static for the session.

```
User message arrives
         │
         ▼
┌──────────────────────┐
│ AD_Role.AIAccessLevel │  ADR-058 gate
│   A → allow all      │
│   N → block          │
│   R → check access   │
└─────────┬────────────┘
          │ (allowed)
          ▼
┌─────────────────────────────────────────────┐
│ PromptProfileResolver.resolve()              │
│                                              │
│ Step 1: AIG_Provider_Access                  │
│         AIG_Prompt_Config_ID for             │
│         (providerID, roleID, userID)         │
│         found + AIGStatus='A'? → use it      │
│         Draft/Inactive? → fall through       │
│                                              │
│ Step 2: AIG_Prompt_Config                    │
│         IsDefault='Y' AND AIGStatus='A'      │
│         found? → use it                      │
│                                              │
│ Step 3: null → no addendum                   │
└─────────┬───────────────────────────────────┘
          │ configID (or 0)
          ▼
┌─────────────────────────────────────────────┐
│ buildSystemPromptWithLanguage()              │
│                                              │
│ [LANGUAGE INSTRUCTION]                       │
│ [LOCKED BASE PROMPT]                         │
│ [DATABASE SYNTAX GUIDANCE]                   │
│ [OPERATOR_INSTRUCTIONS]                      │
│   ← resolved profile's AIGPromptText         │
│ [/OPERATOR_INSTRUCTIONS]                     │
└─────────────────────────────────────────────┘
          │
          ▼ sent to LLM — prompt frozen
```

### Runtime Flow — Phase 2 (Planned)

Multiple profiles available per role. LLM selects the best one **per message** via tool call. Zero new tables — multiple `AIG_Provider_Access` rows per role, each pointing to a different `AIG_Prompt_Config`.

```
Session start
         │
         ▼
┌──────────────────────────────────────────────┐
│ PromptConfigSchemaCache.load()                │
│                                               │
│ SELECT id, name, description, key             │
│ FROM AIG_Prompt_Config                        │
│ WHERE AIG_Prompt_Config_ID IN (               │
│   SELECT AIG_Prompt_Config_ID                 │
│   FROM AIG_Provider_Access                    │
│   WHERE (AD_Role_ID=? OR AD_User_ID=?)        │
│     AND AIG_Prompt_Config_ID IS NOT NULL      │
│     AND AIGStatus='A' AND IsActive='Y'        │
│ )                                             │
│                                               │
│ → [{id, name, description, key}, ...]         │
│   keyed by (clientID, roleID, userID,         │
│             providerID)                       │
└──────────────────────────────────────────────┘

         Per message:
         │
         ▼
┌──────────────────────────────────────────────┐
│ buildSystemPromptWithLanguage()               │
│                                               │
│ [LANGUAGE INSTRUCTION]                        │
│ [LOCKED BASE PROMPT]                          │
│   ...includes operator-level authority        │
│   declaration for selectPromptProfile tool    │
│ [DATABASE SYNTAX GUIDANCE]                    │
│ (no static addendum — LLM selects per message)│
└──────────────┬───────────────────────────────┘
               │
               ▼ LLM receives message + system prompt
┌──────────────────────────────────────────────┐
│ LLM reasons: what is this message about?      │
│ Calls selectPromptProfile tool                │
│ (first, before any ERP data query)            │
└──────────────┬───────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────┐
│ selectPromptProfile tool                      │
│                                               │
│ 1. Read PromptConfigSchemaCache (no DB hit)   │
│    → present list to LLM: name + description  │
│                                               │
│ 2. LLM selected "SALES" →                    │
│    load AIGPromptText by PK (single DB fetch) │
│    return as tool result                      │
│                                               │
│ 3. Cache empty / no match →                  │
│    fall back to Phase 1 resolver              │
│    → IsDefault='Y' → no addendum             │
└──────────────┬───────────────────────────────┘
               │ profile instructions as tool result
               ▼
┌──────────────────────────────────────────────┐
│ LLM follows profile instructions              │
│ for this response, then continues             │
│ with ERP tool calls as needed                 │
│                                               │
│ Selected AIG_Prompt_Config_ID logged          │
│ on CM_ChatEntry for audit                     │
└──────────────────────────────────────────────┘
               │
               ▼ next message → same cycle
```

#### Phase 2 — Configuration Example (CEO Role)

Admin creates multiple `AIG_Provider_Access` rows for the CEO role — one per available profile. `AIG_Provider_ID` is not required.

| Role | User | AIG_Provider_ID | AIG_Prompt_Config_ID |
|------|------|-----------------|---------------------|
| CEO | *(null)* | *(null)* | CRM Profile |
| CEO | *(null)* | *(null)* | Sales Profile |
| CEO | *(null)* | *(null)* | Finance Profile |
| CEO | *(null)* | *(null)* | Inventory Profile |

The cache loads all four as the allowed list. Per message, the LLM picks the most appropriate one based on the user's message and context. A follow-up on the same topic re-selects the same profile — the cache makes this free.

#### Phase 2 — Key Design Constraints

| Constraint | Detail |
|---|---|
| **Per-message selection** | LLM re-evaluates on every message — adapts as topics change within a session |
| **Tool fires first** | `selectPromptProfile` executes before any ERP data query tool |
| **Single selection** | `maxCount=1` on the tool — LLM picks exactly one profile per message |
| **Cache content** | `{id, name, description, key}` only — lightweight. Full `AIGPromptText` fetched only for the selected profile |
| **Trust level** | Profile arrives as tool result (user context), not system prompt. Mitigated by base prompt declaration granting operator-level authority to the tool result |
| **Fallback** | Empty cache / no match / tool not called → Phase 1 resolver → `IsDefault='Y'` → no addendum |
| **Audit** | Selected `AIG_Prompt_Config_ID` stored on `CM_ChatEntry` |

### AIG_Provider_Access Bridge with New FK

```
AIG_Provider_Access (Table_ID=800221)
┌──────────────────────────────┐
│ AIG_Provider_Access_ID  (PK) │
│ AIG_Provider_ID         (FK) │──→ AIG_Provider
│ AD_Role_ID              (FK) │──→ AD_Role
│ AD_User_ID              (FK) │──→ AD_User
│ AIG_Prompt_Config_ID    (FK) │──→ AIG_Prompt_Config  ← NEW
│ IsActive                     │
│ + audit columns              │
└──────────────────────────────┘

AIG_Prompt_Config (Table_ID=800204)
┌──────────────────────────────┐
│ AIG_Prompt_Config_ID    (PK) │
│ Name                         │
│ AIGPromptKey                 │
│ AIGPromptText                │  ← The actual prompt addendum text
│ Description                  │
│ IsDefault              CHAR1 │  ← NEW: 'Y'/'N', one 'Y' per client
│ AIGStatus              CHAR1 │  ← NEW: 'D'raft/'A'ctive/'X'archived
│ IsActive                     │
│ + audit columns              │
└──────────────────────────────┘
```

### QuickWin Implementation Detail

#### Schema Changes (3 columns)

```sql
-- 1. FK on AIG_Provider_Access → AIG_Prompt_Config
ALTER TABLE AIG_Provider_Access
  ADD COLUMN AIG_Prompt_Config_ID NUMERIC(10) NULL;

ALTER TABLE AIG_Provider_Access
  ADD CONSTRAINT AIG_ProvAccess_PromptConfig
  FOREIGN KEY (AIG_Prompt_Config_ID)
  REFERENCES AIG_Prompt_Config(AIG_Prompt_Config_ID);

-- 2. IsDefault on AIG_Prompt_Config
ALTER TABLE AIG_Prompt_Config
  ADD COLUMN IsDefault CHAR(1) DEFAULT 'N' NOT NULL
  CHECK (IsDefault IN ('Y','N'));

-- 3. AIGStatus on AIG_Prompt_Config
ALTER TABLE AIG_Prompt_Config
  ADD COLUMN AIGStatus CHAR(1) DEFAULT 'A' NOT NULL
  CHECK (AIGStatus IN ('D','A','X'));

-- Migrate existing SYSTEM_ADDENDUM record to be the default
UPDATE AIG_Prompt_Config
  SET IsDefault = 'Y', AIGStatus = 'A'
  WHERE AIGPromptKey = 'SYSTEM_ADDENDUM'
    AND IsActive = 'Y';
```

#### PromptProfileResolver Class Design

`com.cloudempiere.ai.provider.langchain4j.PromptProfileResolver` — stateless utility, no OSGi registration needed.

```java
public static int resolve(Properties ctx, int providerID, int roleID, int userID)
```

**Step 1 query** (`lookupFromProviderAccess`):
- Table: `AIG_Provider_Access`
- Filter: `IsActive='Y'`, `AD_Client_ID IN (0,?)`, `AIG_Prompt_Config_ID IS NOT NULL`
- Role/user: `AD_User_ID=? OR AD_Role_ID=?` (whichever are > 0)
- Provider: `AIG_Provider_ID=? OR AIG_Provider_ID IS NULL` (provider-specific preferred, agnostic also accepted)
- Order: user-level first (`AD_User_ID IS NOT NULL`), then provider-specific (`AIG_Provider_ID IS NOT NULL`)
- Returns the `AIG_Prompt_Config_ID` of the first matching row

**Step 2 query** (`lookupClientDefault`):
- Table: `AIG_Prompt_Config`
- Filter: `IsDefault='Y'`, `AIGStatus='A'`, `IsActive='Y'`, `AD_Client_ID IN (0,?)`
- Order: tenant-specific before system (`AD_Client_ID DESC`)

**Step 3**: returns 0 — no addendum appended.

#### Draft/Inactive Config Validation

`MAIPromptConfig.getPromptText(ctx, configID, trxName)` validates the loaded record before returning text:
- `AIGStatus` must equal `'A'` (Active) — Draft (`'D'`) and Archived (`'X'`) return null
- `IsActive` must be `'Y'`

This means a Draft config assigned via `AIG_Provider_Access.AIG_Prompt_Config_ID` is silently skipped: Step 1 returns the config ID, but `getPromptText` returns null, so the resolver falls through to Step 2 (client default) or Step 3 (no addendum). No error is thrown — this is intentional, allowing drafts to be prepared and previewed without affecting live users.

#### AIService Integration

Two methods updated in `AIService`:

```java
// buildSystemPromptWithLanguage — providerID added as 4th param
private String buildSystemPromptWithLanguage(Properties ctx, int chatId,
                                              boolean withTools, int providerID)

// appendOperatorAddendum — providerID replaces hardcoded key lookup
public static String appendOperatorAddendum(Properties ctx, String basePrompt, int providerID) {
    int roleID = ctx != null ? Env.getAD_Role_ID(ctx) : 0;
    int userID = ctx != null ? Env.getAD_User_ID(ctx) : 0;
    int configID = PromptProfileResolver.resolve(ctx, providerID, roleID, userID);
    String addendum = MAIPromptConfig.getPromptText(ctx, configID, null);
    if (addendum == null || addendum.trim().isEmpty())
        return basePrompt;
    return basePrompt + OPERATOR_SECTION_OPEN + addendum.trim() + OPERATOR_SECTION_CLOSE;
}
```

`roleID` and `userID` are extracted from `ctx` inside `appendOperatorAddendum` — callers only pass `providerID`. Both call sites pass `provider.getAIG_Provider_ID()`.

#### Resolution Chain (3 steps, no SeqNo)

| Step | Source | Lookup | Active check | Precedence |
|------|--------|--------|--------------|------------|
| 1 | `AIG_Provider_Access` | FK `AIG_Prompt_Config_ID` for matching `(providerID, roleID, userID)` | `getPromptText` validates `AIGStatus='A'` + `IsActive='Y'`; Draft falls through | Highest — explicit binding |
| 2 | `AIG_Prompt_Config` | `IsDefault='Y'` AND `AIGStatus='A'` for client | Filtered in query | Fallback — current behavior preserved |
| 3 | *(none)* | No profile found | — | Lowest — base prompt only, no addendum |

### Example Configurations

#### Example: 3 Profiles (Warehouse, Sales, Executive)

**AIG_Prompt_Config records:**

| ID | Name | AIGPromptKey | IsDefault | AIGStatus |
|----|------|-------------|-----------|-----------|
| 1001 | Warehouse Profile | WAREHOUSE | N | A |
| 1002 | Sales Profile | SALES | N | A |
| 1003 | Executive Profile | EXECUTIVE | Y | A |

**AIG_Provider_Access records:**

| Provider | Role | User | AIG_Prompt_Config_ID |
|----------|------|------|---------------------|
| Anthropic | Warehouse Clerk | *(null)* | 1001 |
| Anthropic | Sales Rep | *(null)* | 1002 |
| Anthropic | CFO | *(null)* | *(null)* |

**Resolution results:**

| User's Role | Step 1 (FK) | Step 2 (Default) | Resolved Profile |
|-------------|------------|-----------------|------------------|
| Warehouse Clerk | 1001 (hit) | — | **Warehouse Profile** |
| Sales Rep | 1002 (hit) | — | **Sales Profile** |
| CFO | null (miss) | 1003 (hit, IsDefault=Y) | **Executive Profile** |
| External Partner *(no access row)* | miss | 1003 (hit) | **Executive Profile** |

#### Warehouse Profile — AIGPromptText

```
You are the AI assistant for the warehouse operations team.

DOMAIN FOCUS:
- Inventory management, stock levels, and warehouse locations
- Material receipts, shipments, and physical inventory counts
- Product movement history and lot/serial tracking
- Warehouse KPIs: fill rate, inventory turns, stock accuracy

BEHAVIORAL CONSTRAINTS:
- Prioritize operational queries over financial analysis
- When asked about costs, show unit costs only — do not aggregate financial totals
- Always include warehouse location (Locator) in inventory queries
- Use simple, action-oriented language suitable for floor operations

RESPONSE STYLE:
- Keep responses concise — warehouse staff work on mobile devices
- Use tables for inventory listings, not paragraphs
- When showing quantities, always include UOM (Unit of Measure)
```

#### Sales Profile — AIGPromptText

```
You are the AI assistant for the sales team at our company.

DOMAIN FOCUS:
- Sales orders, quotations, and pricing
- Business partner management and credit status
- Sales pipeline, revenue forecasts, and commission tracking
- Product catalog, availability, and lead times

BEHAVIORAL CONSTRAINTS:
- Always check credit status before confirming order feasibility
- When discussing pricing, respect the price list hierarchy
- Do not reveal internal cost margins or purchase prices
- Escalate requests for custom discounts above 15% to management

RESPONSE STYLE:
- Professional and relationship-oriented tone
- Include customer context (payment terms, credit limit) in order queries
- Summarize opportunities with next-action recommendations
```

#### Executive Profile — AIGPromptText (IsDefault = Y)

```
You are the AI assistant for senior management.

DOMAIN FOCUS:
- Cross-functional business overview: sales, purchasing, inventory, finance
- KPI dashboards, trend analysis, and variance explanations
- Strategic queries: revenue growth, margin analysis, cash flow
- Organizational performance across departments

BEHAVIORAL CONSTRAINTS:
- Provide executive summaries with drill-down capability
- Compare current vs. prior period when showing metrics
- Flag anomalies and significant changes proactively
- Respect data access boundaries — the role's permissions still apply

RESPONSE STYLE:
- Lead with the key insight, then supporting data
- Use charts and structured tables for multi-metric views
- Include trend indicators (up/down/stable) with percentages
- When data is incomplete, clearly state what is missing
```

### Prerequisites and Behavioral Clarifications

#### AIG_Provider_Access Column Changes

Before Phase 1 can be implemented, `AIG_Provider_Access` requires two prerequisite changes:

1. **`AIG_Provider_ID` must become non-mandatory** — A record may now carry only a prompt profile override with no provider override (or vice versa). The AD column mandatory flag must be removed.

2. **`beforeSave` validation** — To prevent empty records, `MAIProviderAccess.beforeSave()` enforces that at least one of `AIG_Provider_ID` or `AIG_Prompt_Config_ID` is set. Both being null is rejected with a save error.

#### Static Configuration Model

Prompt profile routing follows the same static-configuration model as AI provider selection:

| Concept | AI Provider | Prompt Profile |
|---|---|---|
| Config record | `AIG_Provider_Access.AIG_Provider_ID` | `AIG_Provider_Access.AIG_Prompt_Config_ID` |
| Scope | role+provider pair | role+provider pair |
| Runtime behavior | one provider, no switching | one profile, no switching |
| Changes when | admin updates `AIG_Provider_Access` | admin updates `AIG_Provider_Access` |
| Fallback | — | `IsDefault='Y'` profile |

`PromptProfileResolver` is a deterministic lookup at prompt-assembly time — no session state, no dynamic switching, no LLM involvement in the decision.

#### AIAccessLevel='All' with No AIG_Provider_Access Records

When `AD_Role.AIAccessLevel = 'A'` (All) and there are no `AIG_Provider_Access` records for the role:

- **Step 1 always misses** — no access records carry the `AIG_Prompt_Config_ID` FK
- **Step 2 applies** — everyone gets the `IsDefault='Y'` client default profile
- **Consequence**: Role-specific prompt profiles require explicit `AIG_Provider_Access` rows even when access control is handled by `AIAccessLevel='A'`. A minimal row (no `AIG_Provider_ID`, just `AIG_Prompt_Config_ID`) is sufficient.

#### User-Level Override Under AIAccessLevel='All'

When `AIAccessLevel='A'` but a user has an explicit `AIG_Provider_Access` record pointing to a different provider, that record **acts as an override** for that user:

```
AIAccessLevel='All' resolution order:
  1. Explicit AIG_Provider_Access for this AD_User_ID → use it (override)
  2. No user-level record → use global default (getDefault)
```

This is implemented in `MAIProvider.getForUser()`: before calling `getDefault()`, it checks for a user-level access record via `getAccessibleProvider(ctx, 0, userId, trxName)`. The override takes effect immediately upon creating the access record — no need to change `AIAccessLevel` from All to UserRoleAccess.

### Related ADRs

- [ADR-058](058-ai-provider-user-role-access-control.md) — AI Provider User/Role Access Control (defines `AIG_Provider_Access` table and `AD_Role.AIAccessLevel` gate)
- [ADR-059](059-configurable-system-prompt-architecture.md) — Configurable System Prompt Architecture (defines operator addendum pattern, spotlighting, `AIG_Prompt_Config`, and the `AIG_Prompt_Context` future improvement)
- [ADR-006](006-data-model-architecture.md) — Data Model Architecture (overall AI data model)
- [ADR-012](012-rag-based-context-retrieval.md) — RAG-Based Context Retrieval (knowledge base strategy that Phase 3 implements)

### References

- [CE_LLM_Search ERD Research](../research/ce_llm_search_erd.md) — Research input: 7-table ERD for role-scoped, vertical-aware RAG retrieval
- [Conventional Commits](https://www.conventionalcommits.org/) — Commit message standard
- [MADR 3.0](https://adr.github.io/madr/) — Architecture Decision Record template

---

*ADR-063 | Version 2.0 | 2026-03-12 — Phase 2: LLM-driven per-message profile selection added as primary approach*
