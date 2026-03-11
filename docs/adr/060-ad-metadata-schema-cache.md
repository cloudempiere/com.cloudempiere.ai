# ADR-060: Application Dictionary Schema Cache

## Status

Implemented

## Date

2026-03-11

## Deciders

- Development Team

## Context and Problem Statement

The AI plugin's `ERPTools` class queries the iDempiere database on every tool call without any cached knowledge of the Application Dictionary schema. When the LLM needs to write SQL, it either guesses table/column names (causing hallucinations) or must first call `getTableMetadata()` as a separate round-trip, adding latency and token cost.

The **idempiere-hub** microservices project solved this with `SchemaCache` (ADR-024). This ADR adapts the same pattern for the OSGi/iDempiere Java 11 runtime.

### Problems Solved

1. **Hallucinated table names** — LLM references non-existent tables; errors only discovered at query execution
2. **Hallucinated column names** — LLM guesses column names that don't exist in the target table
3. **Invalid JOIN syntax** — LLM generates comma-joins with ON clauses, or ON without parentheses, which `AccessSqlParser` cannot parse
4. **Tableless queries** — LLM generates `SELECT CURRENT_DATE` which crashes `SecureDatabaseQueryExecutor`
5. **Extra round-trips** — LLM calls `getTableMetadata()` before every SQL query (slow, token-expensive)
6. **Cryptic errors** — Bad SQL produces confusing DB errors instead of actionable guidance

### Java Version Constraint

The AI plugin runs on **Java 11** (iDempiere v10 requirement). All code avoids Java 17+ features (records, sealed classes, text blocks). See [ADR-035](035-java-version-strategy.md).

## Decision Outcome

**In-memory `ConcurrentHashMap` singleton with 30-minute TTL**, loaded lazily from `AD_Table` + `AD_Column` on first use. Chosen over `CCache` + `ICacheReset` because `CCache` requires OSGi registration and iDempiere context at startup, which is unavailable before the first user session.

## Architecture

### Components

```
ADSchemaCache                     singleton, ConcurrentHashMap, 30-min TTL
  └── ADTableMeta                 one per AD_Table (name, accessLevel, isView)
        └── ADColumnMeta          one per AD_Column (name, type, FK, isKey)
```

### Cache Loading

- **Trigger**: Lazy — loaded on first call to `ensureLoaded()` (inside `tableExists()`, `getTableMetadata()`, `getAllTables()`, `suggestTable()`)
- **Source**: `AD_Table` + `AD_Column` where `IsActive = 'Y'`, ordered by `SeqNo`
- **TTL**: 30 minutes. Checked on each `ensureLoaded()` call; reloads if expired
- **Thread safety**: `synchronized` on `ensureLoaded()` + `loadSchema()`; reads use `ConcurrentHashMap`
- **Size**: ~1934 tables, ~52141 columns, loads in ~200–300ms

```java
// Lazy-load trigger
public synchronized void ensureLoaded() {
    long now = System.currentTimeMillis();
    boolean needsLoad = cache.isEmpty()
        || (lastLoadedTime == 0)
        || ((now - lastLoadedTime) > CACHE_TTL_MINUTES * 60 * 1000L);
    if (needsLoad) loadSchema();
}
```

### Lookup API

| Method | Complexity | Purpose |
|---|---|---|
| `tableExists(name)` | O(1) | Pre-execution table check |
| `getTableMetadata(name)` | O(1) | Full table + column info |
| `getAllTables()` | O(1) | Table listing (no DB round-trip) |
| `suggestTable(misspelled)` | O(n) | Levenshtein fuzzy match |
| `ADTableMeta.hasColumn(name)` | O(1) | Column existence check |
| `ADTableMeta.findSimilarColumns(name)` | O(n) | Levenshtein fuzzy match |
| `getCacheSize()` | O(1) | Diagnostics |
| `getLastLoadedTime()` | O(1) | Diagnostics |

### Fuzzy Matching Threshold

Levenshtein threshold is **proportional** to avoid nonsensical suggestions:

```java
// Tables:  max(2, min(4, name.length() / 3))
// Columns: max(2, min(3, name.length() / 3))
```

Example: `C_UOM_ID` (8 chars) → threshold = 2. `AD_Org_ID` has distance 5 — not suggested. A genuine typo like `C_UOM_OD` has distance 1 — suggested.

## Validation Pipeline

Every `ERPTools.queryDatabase()` call runs through `validateTablesInSql()` before reaching `SecureDatabaseQueryExecutor`:

```
SQL from LLM
    │
    ├─ 1. No FROM clause?
    │     → "Query has no FROM clause. Use getTableMetadata() first."
    │
    ├─ 2. Comma-join + ON clause?  e.g. "FROM t1, t2 ON t1.id = t2.id"
    │     → "Invalid JOIN syntax. Use explicit JOIN...ON (condition)."
    │
    ├─ 3. ON without parentheses?  e.g. "JOIN t ON a.col = b.col"
    │     → "ON conditions must be wrapped in parentheses."
    │       (AccessSqlParser requirement — strips ON clauses by finding ')')
    │
    ├─ 4. Table not in cache?
    │     → "Table 'X' does not exist. Did you mean: [suggestions]"
    │
    └─ 5. alias.column not in table?
          → "Column 'X' does not exist in 'Table'.
             Available columns: [key+FK cols], other: [rest]"
```

Column validation resolves aliases to tables by parsing FROM/JOIN clauses first, then checks every `alias.column` reference.

## Error Enrichment

All errors from `queryDatabase` include `schemaHints` so the LLM can self-correct without a separate `getTableMetadata` call:

```json
{
  "error": true,
  "message": "ON conditions must be wrapped in parentheses...",
  "schemaHints": {
    "tables": [
      {
        "tableName": "M_InOut",
        "keyAndFkColumns": ["M_InOut_ID", "C_BPartner_ID", "M_Warehouse_ID"],
        "otherColumns": ["DocumentNo", "MovementDate", "PickDate", "DocStatus"]
      }
    ],
    "hint": "Use exact column names above for follow-up queries."
  }
}
```

Successful responses also include `schemaHints` alongside `rows` for follow-up query context.

## Retry Suppression

`ERPTools` tracks consecutive `queryDatabase` failures per conversation instance:

- **Failures 1 to (MAX-1)**: error + schema hints returned to LLM only — no UI event fired (`fireToolError` suppressed). LLM self-corrects silently.
- **Failure at MAX** (default 6): `fireToolError` fires once with a user-facing message; `RuntimeException` thrown to terminate the agent.

```java
private int queryFailureCount = 0;
private static final int MAX_QUERY_FAILURES = 6;
```

The user sees one clean terminal message instead of a stream of failed attempts.

## Tool Description Guidance

`@Tool` on `queryDatabase` explicitly instructs the LLM:

> "IMPORTANT: Call getTableMetadata() for each table BEFORE writing SQL to discover exact column names. ON conditions MUST be wrapped in parentheses: JOIN t ON (a.col = b.col)."

## Logging

All validation events logged at `WARNING` level (visible in Eclipse console per project convention):

```
[SCHEMA-VALIDATE] SQL: SELECT ...
[SCHEMA-VALIDATE] Tables found in SQL: [M_InOut, C_BPartner]
[SCHEMA-VALIDATE] All tables OK: [M_InOut, C_BPartner]
[SCHEMA-VALIDATE] REJECTED ON-without-parens: ...
[QUERY-FAIL 2/6] Column 'X' does not exist...
ADSchemaCache loaded: 1934 tables, 52141 columns in 217ms
```

## Deviations from Original Design

The original ADR proposed:
- `CCache` + `ICacheReset` + `@Component` — **not implemented** (requires OSGi context at startup)
- `ADRelation` class for FK relationships — **not implemented** (FK stored as `foreignTable` string on `ADColumnMeta`)
- Translation support (`AD_Table_Trl` join) — **not implemented**
- `entityType` field — **not implemented**
- Phase 3 RAG wiring — **not implemented**

## Known Gaps

- No `ICacheReset` integration — cache does not invalidate on AD changes, only on TTL
- No translation support — column/table names always in base language
- Column validation does not check subquery aliases
- LLM may skip `getTableMetadata` on first attempt despite `@Tool` guidance (relies on model compliance)
- Intermediate LLM reasoning text ("Let me fix...") still leaks to UI between retries

## Related ADRs

- [ADR-007](007-database-security-model.md) — SecureDatabaseQueryExecutor (query execution layer)
- [ADR-035](035-java-version-strategy.md) — Java 11 constraint
- [ADR-061](061-sql-context-enrichment.md) — SQL context enrichment
- [ADR-062](062-schema-aware-system-prompt.md) — Schema-aware system prompt

### idempiere-hub Reference
- **ADR-024**: AD Metadata Caching — L1 memory / L2 file / L3 database
- **SchemaCache.java**, **QueryToolLogic.java** — reference implementations
