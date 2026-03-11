# ADR-060: Application Dictionary Schema Cache

## Status

Proposed

## Date

2026-03-11

## Deciders

- Development Team

## Context and Problem Statement

The AI plugin's `ERPTools` class queries the iDempiere database on every tool call without any cached knowledge of the Application Dictionary schema. When the LLM needs to write SQL, it either guesses table/column names (causing hallucinations) or must first call `getTableMetadata()` and `listTables()` as separate round-trips, adding latency and token cost.

The **idempiere-hub** microservices project solved this problem comprehensively with `SchemaCache` (ADR-024) and `ADContextService` (ADR-008/055). The AI plugin should adopt the same pattern, adapted for the OSGi/iDempiere runtime environment.

### Current Problems

1. **No pre-validation** - LLM can reference non-existent tables; errors only discovered at query execution
2. **Extra round-trips** - LLM must call `getTableMetadata()` before writing SQL (wasting tokens and time)
3. **No fuzzy matching** - Misspelled table names produce cryptic SQL errors instead of "did you mean?" suggestions
4. **No relationship awareness** - LLM doesn't know FK relationships, must guess JOINs
5. **ADMetadataIngestor exists but is disconnected** - RAG embeddings of AD metadata are generated but never queried during chat

### Reference Implementation

The idempiere-hub project implements this via:
- **`SchemaCache.java`** - In-memory cache loaded at Quarkus startup, O(1) lookup, TTL-based refresh
- **`ADContextService.java`** - Rich context enrichment (relationships, windows, access levels, usage hints)
- **`TableMetadata.java`** / **`ColumnMetadata.java`** - Lightweight DTOs
- **ADR-024**: AD Metadata Caching architecture (L1 memory -> L2 file -> L3 database)
- **ADR-008**: Application Dictionary Registry (structured knowledge base)
- **ADR-055**: Tool Ecosystem Review (split RegistryToolLogic into domain-focused classes)

### Java Version Constraint

**Important:** The AI plugin runs on **Java 11** (iDempiere v10 requirement), while the idempiere-hub reference implementation uses **Java 17+** (Quarkus 3.x). All code must avoid Java 17+ features like records, sealed classes, pattern matching, and text blocks. See [ADR-035](035-java-version-strategy.md).

## Decision Drivers

- **Eliminate hallucinations** - LLM must know actual table/column names before writing SQL
- **Reduce round-trips** - Avoid extra tool calls for schema discovery
- **Performance** - Sub-millisecond lookups for table existence and metadata
- **iDempiere native** - Use `DB.getConnectionRW()`, `CCache`, and existing iDempiere patterns
- **Multi-tenant** - Respect AD_Client_ID filtering on cached metadata
- **Memory efficiency** - Don't cache entire AD if not needed; lazy-load on demand

## Considered Options

1. **iDempiere CCache-based Schema Cache** - Use iDempiere's built-in `CCache` for AD metadata
2. **Port SchemaCache directly** - Copy idempiere-hub's Quarkus `SchemaCache` into OSGi
3. **RAG-only approach** - Rely solely on existing `ADMetadataIngestor` embeddings
4. **System prompt injection** - Hardcode frequently-used schemas in the system prompt

## Decision Outcome

**Chosen option:** "iDempiere CCache-based Schema Cache", because it leverages iDempiere's native caching infrastructure (which already handles cache invalidation via `CacheMgt`), requires no external dependencies, and integrates naturally with the OSGi lifecycle.

### Confirmation

- `ADSchemaCache.tableExists("C_Order")` returns `true` in < 1ms
- `ADSchemaCache.getTableMetadata("C_Order")` returns columns, types, descriptions
- `ADSchemaCache.suggestTable("C_Ordr")` returns `["C_Order"]` (fuzzy match)
- `ERPTools.queryDatabase()` validates table names before execution
- Cache refreshes automatically when AD changes are detected

## Pros and Cons of the Options

### Option 1: iDempiere CCache-based Schema Cache (chosen)

Use `CCache<String, ADTableMeta>` populated at first access, refreshed via `CacheMgt` reset events.

- Good, because uses iDempiere's native cache invalidation (reset on AD changes)
- Good, because zero external dependencies
- Good, because thread-safe (CCache is synchronized)
- Good, because lazy initialization (loads on first AI chat, not at plugin startup)
- Good, because multi-tenant aware via `Env.getAD_Client_ID()`
- Neutral, because no file persistence across restarts (reloads from DB)
- Bad, because CCache doesn't support TTL natively (relies on reset events)

### Option 2: Port SchemaCache directly

Copy idempiere-hub's Quarkus-based `SchemaCache` with `@Observes StartupEvent`.

- Good, because proven implementation
- Bad, because Quarkus CDI annotations don't work in OSGi
- Bad, because requires adaptation of connection management
- Bad, because duplicates code across projects

### Option 3: RAG-only approach

Wire existing `ADMetadataIngestor` into `AIService` for semantic table discovery.

- Good, because already implemented (just needs wiring)
- Good, because handles semantic queries ("tables for sales")
- Bad, because too slow for exact lookups (embedding + similarity search vs O(1))
- Bad, because overkill for "does C_Order exist?" validation
- Bad, because requires embedding model running

### Option 4: System prompt injection

Hardcode top-50 table schemas in `ERPAgent.SYSTEM_PROMPT`.

- Good, because zero latency (already in context)
- Bad, because wastes tokens on every request (even non-SQL queries)
- Bad, because static, doesn't reflect customer customizations
- Bad, because doesn't scale beyond a handful of tables

## Architecture

### Cache Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                      ADSchemaCache                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                    │
│  CCache<String, ADTableMeta> tableCache                          │
│  ├── "C_ORDER"      → ADTableMeta(id=259, columns=[...])        │
│  ├── "C_BPARTNER"   → ADTableMeta(id=291, columns=[...])        │
│  ├── "M_PRODUCT"    → ADTableMeta(id=208, columns=[...])        │
│  └── ...                                                         │
│                                                                    │
│  Map<String, String> tableNameIndex  (case-insensitive lookup)   │
│                                                                    │
│  Methods:                                                         │
│  ├── tableExists(tableName) → boolean          [O(1)]            │
│  ├── getTableMetadata(tableName) → ADTableMeta [O(1)]            │
│  ├── getColumnMetadata(table, col) → ADColMeta [O(1)]            │
│  ├── suggestTable(misspelled) → List<String>   [Levenshtein]     │
│  ├── getRelatedTables(tableName) → List<FK>    [cached]          │
│  └── ensureLoaded() → void                     [lazy init]       │
│                                                                    │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
ERPTools.queryDatabase(sql)
    │
    ▼
ADSchemaCache.ensureLoaded()          ← lazy init on first use
    │
    ▼
Extract table names from SQL          ← AccessSqlParser (existing)
    │
    ▼
For each table:
    ├── tableExists(name)?
    │   ├── YES → continue
    │   └── NO  → suggestTable(name) → return error with suggestions
    │
    ▼
SecureDatabaseQueryExecutor.execute()  ← existing security layer
    │
    ▼
Enrich result with AD context          ← NEW: table descriptions,
    │                                      column hints, relationships
    ▼
Return to LLM
```

### DTOs

```java
/**
 * Cached table metadata - mirrors idempiere-hub's TableMetadata.
 */
public class ADTableMeta {
    private int adTableId;
    private String tableName;
    private String name;           // Display name
    private String description;
    private boolean isView;
    private String accessLevel;    // "3" = Client+Org
    private String entityType;
    private List<ADColumnMeta> columns;
    private List<ADRelation> relations;  // FK relationships
}

/**
 * Cached column metadata - mirrors idempiere-hub's ColumnMetadata.
 */
public class ADColumnMeta {
    private int adColumnId;
    private String columnName;
    private String name;           // Display name
    private String description;
    private int adReferenceId;     // DisplayType
    private String referenceType;  // "TableDirect", "List", etc.
    private String foreignTable;   // For FK columns
    private boolean mandatory;
    private boolean key;
    private int fieldLength;
}

/**
 * Foreign key relationship.
 */
public class ADRelation {
    private String foreignTable;
    private String foreignColumn;
    private String localColumn;
    private String type;           // "parent" or "child"
}
```

### Loading Strategy

```java
@Component
public class ADSchemaCache implements ICacheReset {

    private static final CCache<String, ADTableMeta> s_cache =
        new CCache<>("ADSchemaCache", "AD_Table", 500, 60, false);

    private static volatile boolean s_loaded = false;

    /**
     * Lazy-load all active tables with columns on first access.
     * Mirrors idempiere-hub SchemaCache.loadSchema().
     */
    public synchronized void ensureLoaded() {
        if (s_loaded && s_cache.size() > 0)
            return;

        String sqlTables =
            "SELECT t.AD_Table_ID, t.TableName, " +
            "COALESCE(trl.Name, t.Name) as Name, " +
            "COALESCE(trl.Description, t.Description) as Description, " +
            "t.IsView, t.AccessLevel, t.EntityType " +
            "FROM AD_Table t " +
            "LEFT JOIN AD_Table_Trl trl ON t.AD_Table_ID = trl.AD_Table_ID " +
            "  AND trl.AD_Language = ? " +
            "WHERE t.IsActive = 'Y'";

        String sqlColumns =
            "SELECT c.AD_Column_ID, c.ColumnName, " +
            "COALESCE(etrl.Name, e.Name) as Name, " +
            "COALESCE(etrl.Description, e.Description) as Description, " +
            "c.AD_Reference_ID, r.Name as ReferenceType, " +
            "c.IsMandatory, c.IsKey, c.FieldLength " +
            "FROM AD_Column c " +
            "JOIN AD_Element e ON c.AD_Element_ID = e.AD_Element_ID " +
            "LEFT JOIN AD_Element_Trl etrl ON e.AD_Element_ID = etrl.AD_Element_ID " +
            "  AND etrl.AD_Language = ? " +
            "LEFT JOIN AD_Reference r ON c.AD_Reference_ID = r.AD_Reference_ID " +
            "WHERE c.AD_Table_ID = ? AND c.IsActive = 'Y' " +
            "ORDER BY c.SeqNo";

        // Load tables, then columns per table
        // Detect FK relationships from _ID columns with AD_Reference_ID = 19
    }

    @Override
    public int reset() {
        s_loaded = false;
        s_cache.reset();
        return 1;
    }
}
```

## Integration Points

### 1. ERPTools Enhancement

```java
@Tool("Execute a SQL query against the iDempiere database")
public String queryDatabase(String sql) {
    // NEW: Validate tables exist before execution
    Set<String> tables = extractTableNames(sql);
    for (String table : tables) {
        if (!schemaCache.tableExists(table)) {
            List<String> suggestions = schemaCache.suggestTable(table);
            return "Table '" + table + "' not found. " +
                   (suggestions.isEmpty() ? "" : "Did you mean: " + suggestions);
        }
    }

    // Existing: execute via SecureDatabaseQueryExecutor
    SecureQueryResult result = executor.executeSecureQuery(request);

    // NEW: Enrich with context
    return enrichWithContext(result, tables);
}
```

### 2. Context Enrichment (mirrors ADContextService)

```java
private String enrichWithContext(SecureQueryResult result, Set<String> tables) {
    JSONObject enriched = new JSONObject();
    enriched.put("data", result.toJSON());

    JSONObject context = new JSONObject();
    for (String table : tables) {
        ADTableMeta meta = schemaCache.getTableMetadata(table);
        if (meta != null) {
            JSONObject tableCtx = new JSONObject();
            tableCtx.put("description", meta.getDescription());
            tableCtx.put("accessLevel", meta.getAccessLevel());
            tableCtx.put("relations", meta.getRelationsJSON());
            context.put(table, tableCtx);
        }
    }
    enriched.put("tableContext", context);
    return enriched.toString();
}
```

### 3. Wire RAG (existing ADMetadataIngestor)

Connect the already-implemented `ADMetadataIngestor` to `AIService` so semantic searches like "tables related to sales" work via RAG, while exact lookups use the cache.

## Implementation Plan

### Phase 1: Core Cache (1 week)
1. Create `ADSchemaCache` with `CCache` backend
2. Create `ADTableMeta`, `ADColumnMeta`, `ADRelation` DTOs
3. Implement `ensureLoaded()` with table + column loading
4. Implement `tableExists()`, `getTableMetadata()`, `suggestTable()`
5. Register as `ICacheReset` for automatic invalidation

### Phase 2: ERPTools Integration (3 days)
1. Add pre-validation in `queryDatabase()` - validate table names before SQL execution
2. Add context enrichment to query results
3. Update `getTableMetadata()` to use cache instead of live DB query
4. Update `listTables()` to use cache

### Phase 3: RAG Wiring (2 days)
1. Connect `ADMetadataIngestor` output to `AIService`
2. Add semantic search fallback when exact match fails
3. Use cache for exact lookups, RAG for semantic queries (same pattern as idempiere-hub ADR-024)

## Related ADRs

### This Plugin
- [ADR-007](007-database-security-model.md) - Database Security Model (SecureDatabaseQueryExecutor)
- [ADR-012](012-rag-based-context-retrieval.md) - RAG-Based Context Retrieval
- [ADR-040](040-embedding-ingestion-evolution.md) - Embedding Ingestion Evolution (ADMetadataIngestor)

### idempiere-hub (Reference Implementation)
- **ADR-008**: Application Dictionary Registry - structured AD knowledge base
- **ADR-024**: AD Metadata Caching - L1 memory / L2 file / L3 database architecture
- **ADR-053**: Build-Time Model Discovery - embed metadata to eliminate hallucinations
- **ADR-055**: Tool Ecosystem Architectural Review - domain-focused registry classes

### Key Source Files (idempiere-hub)
- `src/main/java/org/idempiere/cli/ai/shared/SchemaCache.java`
- `src/main/java/org/idempiere/cli/ai/shared/ADContextService.java`
- `src/main/java/org/idempiere/cli/ai/shared/TableMetadata.java`
- `src/main/java/org/idempiere/cli/ai/shared/ColumnMetadata.java`
- `src/main/java/org/idempiere/cli/ai/shared/registry/TableRegistryLogic.java`
