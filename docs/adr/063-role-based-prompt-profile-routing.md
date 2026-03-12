# ADR-063: Role-Based Prompt Profile Routing

## Status

Proposed

## Date

2026-03-12

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

Today all users share one global `SYSTEM_ADDENDUM` prompt configured in `AIG_Prompt_Config` (ADR-059). A warehouse clerk and a CFO get identical AI behavior — the same persona, domain focus, and behavioral constraints. The CE_LLM_Search ERD research (`docs/research/ce_llm_search_erd.md`) proposed 7 new tables for role-scoped, vertical-aware RAG retrieval, but deploying that entire schema before validating the core routing premise is premature. We need a minimal mechanism to deliver different AI behavior to different roles without over-engineering the schema.

## Decision Drivers

- **KISS**: Validate role→prompt routing with real users before investing in verticals, knowledge bases, and search sessions
- **Zero new tables for Phase 1**: Reuse `AIG_Provider_Access` (ADR-058) and `AIG_Prompt_Config` (ADR-059) — both already exist and are deployed
- **Deterministic routing**: The prompt profile must be resolved in Java at prompt-assembly time, not by the LLM (the LLM cannot choose its own system prompt)
- **Backward compatibility**: Existing single-addendum installations must continue working unchanged
- **Extensibility**: The design must accommodate future phases (verticals, window-context routing, knowledge bases) without breaking the QuickWin schema

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

### Three-Phase Roadmap

The CE_LLM_Search ERD is implemented incrementally across three phases, with each phase building on the previous one. Tables are renamed from `CE_LLM_*` to `AIG_*` to stay within the AI plugin namespace.

```
Phase 1: QuickWin (this ADR)              Phase 2: Mid                          Phase 3: Complete
──────────────────────────                 ──────────────────                     ──────────────────────
3 columns, 0 new tables                   +2 new tables                         +3 new tables, +1 junction

AIG_Provider_Access                        AIG_Vertical                          AIG_SearchConfig
  + AIG_Prompt_Config_ID (FK)               ├ Value, Name, Description            ├ AIG_Provider_ID (FK)
                                            └ IsActive                            ├ TopK, SimilarityThreshold
AIG_Prompt_Config                                                                 ├ RetrievalStrategy
  + IsDefault CHAR(1)                      AIG_Prompt_Context                     └ IsDefault
  + AIGStatus CHAR(1)                       ├ AIG_Prompt_Config_ID (FK)
                                            ├ AD_Window_ID (FK, nullable)       AIG_KnowledgeBase
PromptProfileResolver (Java)                ├ AD_Table_ID (FK, nullable)          ├ AIG_Vertical_ID (FK)
  └ 3-step resolution chain                 └ SeqNo                              ├ StorageType, ConnectionRef
                                                                                  └ EmbeddingModel
                                           PromptContextResolver (Java)
                                            └ 6-step resolution chain           AIG_ContextProfile_KB (junction)
                                              (extends QuickWin resolver)         ├ AIG_Prompt_Config_ID (FK)
                                                                                  ├ AIG_KnowledgeBase_ID (FK)
                                                                                  └ SeqNo, MetadataFilterJSON

                                                                                 AIG_SearchSession (audit)
                                                                                  ├ AD_User_ID, AIG_Prompt_Config_ID
                                                                                  ├ RawQuery, RewrittenQuery
                                                                                  └ ResultCount, AvgScore
```

**Mapping from CE_LLM ERD to AIG phases:**

| CE_LLM ERD Table | AIG Name | Phase |
|---|---|---|
| CE_LLM_Provider | *(already exists as AIG_Provider)* | — |
| CE_LLM_Vertical | AIG_Vertical | Mid |
| CE_LLM_SearchConfig | AIG_SearchConfig | Complete |
| CE_LLM_ContextProfile | *(absorbed into AIG_Prompt_Config + AIG_Provider_Access FK)* | QuickWin |
| CE_LLM_KnowledgeBase | AIG_KnowledgeBase | Complete |
| CE_LLM_ContextProfile_KB | AIG_ContextProfile_KB | Complete |
| CE_LLM_SearchSession | AIG_SearchSession | Complete |

### Runtime Flow Diagram

```
User opens AI chat panel
         │
         ▼
┌─────────────────────┐
│ AD_Role.AIAccessLevel│  ADR-058 gate
│   A → allow all     │
│   N → block         │
│   R → check access  │
└────────┬────────────┘
         │ (allowed)
         ▼
┌─────────────────────┐
│ AIG_Provider_Access  │  Which provider for this role/user?
│ (providerID, roleID, │
│  userID)             │
└────────┬────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ PromptProfileResolver.resolve()              │
│                                              │
│ Step 1: AIG_Provider_Access.AIG_Prompt_      │
│         Config_ID for this (provider,role)?  │
│         → found? return that profile         │
│                                              │
│ Step 2: AIG_Prompt_Config where IsDefault='Y'│
│         AND AIGStatus='A' for this client?   │
│         → found? return that profile         │
│                                              │
│ Step 3: No profile → return null             │
│         (no addendum appended)               │
└────────┬────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ AIService.buildSystemPromptWithLanguage()    │
│                                              │
│ [LANGUAGE INSTRUCTION]                       │
│ [LOCKED BASE PROMPT]                         │
│ [DATABASE SYNTAX GUIDANCE]                   │
│ [OPERATOR_INSTRUCTIONS]                      │
│   ← resolved profile's AIGPromptText         │
│ [/OPERATOR_INSTRUCTIONS]                     │
└─────────────────────────────────────────────┘
```

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

```java
package com.cloudempiere.ai.provider.langchain4j;

/**
 * Resolves the active AIG_Prompt_Config profile for a given provider+role+user
 * combination. Implements the 3-step resolution chain (ADR-063 QuickWin).
 */
public class PromptProfileResolver {

    /**
     * Resolve the prompt profile for the current session.
     *
     * @param ctx        iDempiere context (AD_Client_ID, AD_Org_ID)
     * @param providerID AIG_Provider_ID of the active provider
     * @param roleID     AD_Role_ID of the current user's role
     * @param userID     AD_User_ID of the current user
     * @return resolved AIG_Prompt_Config_ID, or 0 if no profile found
     */
    public static int resolve(Properties ctx, int providerID,
                              int roleID, int userID) {
        // Step 1: Explicit FK on AIG_Provider_Access for this role+provider
        //   SELECT AIG_Prompt_Config_ID
        //   FROM AIG_Provider_Access
        //   WHERE AIG_Provider_ID = ? AND AD_Role_ID = ?
        //     AND IsActive = 'Y' AND AIG_Prompt_Config_ID IS NOT NULL
        //   ORDER BY AD_User_ID DESC  -- user-level takes precedence
        //   LIMIT 1
        int configID = lookupFromProviderAccess(ctx, providerID, roleID, userID);
        if (configID > 0) return configID;

        // Step 2: Client default (IsDefault='Y', AIGStatus='A')
        //   SELECT AIG_Prompt_Config_ID
        //   FROM AIG_Prompt_Config
        //   WHERE AD_Client_ID = ? AND IsDefault = 'Y'
        //     AND AIGStatus = 'A' AND IsActive = 'Y'
        configID = lookupClientDefault(ctx);
        if (configID > 0) return configID;

        // Step 3: No profile — no addendum
        return 0;
    }
}
```

#### AIService Integration Point

The change is localized to `AIService.appendOperatorAddendum()` (line 1580):

```java
// BEFORE (ADR-059):
public static String appendOperatorAddendum(Properties ctx, String basePrompt) {
    String addendum = MAIPromptConfig.getPromptText(ctx, "SYSTEM_ADDENDUM", null);
    ...
}

// AFTER (ADR-063):
public static String appendOperatorAddendum(Properties ctx, String basePrompt,
                                            int providerID, int roleID, int userID) {
    int configID = PromptProfileResolver.resolve(ctx, providerID, roleID, userID);
    String addendum;
    if (configID > 0) {
        addendum = MAIPromptConfig.getPromptText(ctx, configID);
    } else {
        addendum = null; // no profile → no addendum
    }
    if (addendum == null || addendum.trim().isEmpty()) {
        return basePrompt;
    }
    return basePrompt + OPERATOR_SECTION_OPEN + addendum.trim() + OPERATOR_SECTION_CLOSE;
}
```

The caller `buildSystemPromptWithLanguage()` already has access to `ctx` (which contains `AD_Role_ID` via `Env.getAD_Role_ID(ctx)`) and `providerID`. The method signature change is backward-compatible via overload.

#### Resolution Chain (3 steps, no SeqNo)

| Step | Source | Lookup | Precedence |
|------|--------|--------|------------|
| 1 | `AIG_Provider_Access` | FK `AIG_Prompt_Config_ID` for matching `(providerID, roleID)` | Highest — explicit role→profile binding |
| 2 | `AIG_Prompt_Config` | `IsDefault = 'Y'` AND `AIGStatus = 'A'` for client | Fallback — current behavior preserved |
| 3 | *(none)* | No profile found | Lowest — base prompt only, no addendum |

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

*ADR-063 | Version 1.0 | 2026-03-12*
