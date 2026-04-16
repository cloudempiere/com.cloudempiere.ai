# ADR-061: SQL Query Context Enrichment

## Status

Proposed

## Date

2026-03-11

## Deciders

- Development Team

## Context and Problem Statement

When the LLM executes SQL via `ERPTools.queryDatabase()`, results are returned as raw JSON with no contextual information about the tables, columns, or relationships involved. The LLM must interpret column names like `M_Product_Category_ID` without knowing what they mean, leading to poor natural-language responses.

The **idempiere-hub** microservices project solved this with `ADContextService` which enriches query results with table descriptions, column hints, window associations, and relationship information (see idempiere-hub ADR-008, ADR-055).

### Current Problem

```
User: "Show me top 10 products by revenue"
LLM → queryDatabase("SELECT p.M_Product_ID, p.Value, p.Name,
                      SUM(ol.LineNetAmt) as Revenue
                      FROM M_Product p JOIN C_OrderLine ol ...")

Returns: [{"M_Product_ID": 123, "Value": "SKU001", "Name": "Widget", "Revenue": 50000}]

LLM has NO context about:
- What M_Product is (Product master data)
- That C_OrderLine is child of C_Order
- That LineNetAmt is a currency amount
- That M_Product_Category_ID in M_Product links to M_Product_Category
```

### Java Version Constraint

**Important:** The AI plugin runs on **Java 11** (iDempiere v10 requirement), while the idempiere-hub reference implementation uses **Java 17+** (Quarkus 3.x). All code must avoid Java 17+ features like records, sealed classes, pattern matching, and text blocks. See [ADR-035](035-java-version-strategy.md).

### Reference Implementation

The idempiere-hub implements this via:
- **`ADContextService.java`** - Runtime enrichment service
- **`ADContextService.extractTableNames(sql)`** - Regex-based table extraction from SQL
- **`ADContextService.getTableContext(tableName, language)`** - Rich context with descriptions, windows, related tables, access levels, hints
- **`QueryToolLogic.java`** - Unified query execution with automatic context enrichment

## Decision Drivers

- **Better LLM responses** - LLM understands data it retrieves, produces better natural language
- **Reduce follow-up questions** - LLM doesn't need to call `getTableMetadata()` after every query
- **Relationship awareness** - LLM can suggest JOINs and related queries
- **Language support** - Context should use translated names matching user's language
- **Low overhead** - Enrichment must not significantly slow down query responses
- **Java 11 compatible** - No Java 17+ features (records, sealed classes, text blocks)

## Considered Options

1. **Inline context enrichment** - Attach AD context to every query result automatically
2. **Separate context tool** - Keep current `getTableMetadata()` as separate tool call
3. **System prompt injection** - Pre-load all context into system prompt

## Decision Outcome

**Chosen option:** "Inline context enrichment", because it eliminates extra round-trips and provides context exactly when the LLM needs it - alongside query results.

### Confirmation

- Query results include `tableContext` section with descriptions and relationships
- LLM generates better natural-language summaries of query results
- No additional tool calls needed for schema understanding
- Response latency increase < 5ms (cached metadata from ADR-060)

## Architecture

### Enrichment Flow

```
ERPTools.queryDatabase(sql)
    │
    ├─ 1. Validate tables (ADR-060 cache)
    │
    ├─ 2. Execute query (SecureDatabaseQueryExecutor)
    │
    ├─ 3. Extract table names from SQL          ← NEW
    │     Pattern: FROM|JOIN\s+(\w+)
    │
    ├─ 4. Fetch context per table (ADR-060 cache)  ← NEW
    │     ├── Table description, access level
    │     ├── Column descriptions for returned columns
    │     ├── FK relationships (parent/child tables)
    │     └── Window association (which UI window shows this data)
    │
    └─ 5. Return enriched result                ← NEW
          {
            "data": [...rows...],
            "rowCount": 10,
            "tableContext": {
              "M_Product": {
                "description": "Product, Service, Item",
                "accessLevel": "Client+Organization",
                "columns": {
                  "M_Product_Category_ID": {
                    "name": "Product Category",
                    "foreignTable": "M_Product_Category"
                  }
                },
                "relatedTables": [
                  {"table": "M_Product_Category", "type": "parent"},
                  {"table": "C_OrderLine", "type": "child"}
                ]
              }
            }
          }
```

### Context Service (Java 11 compatible)

```java
/**
 * Enriches query results with Application Dictionary context.
 * Mirrors idempiere-hub's ADContextService pattern.
 *
 * NOTE: Java 11 - no records, no text blocks, no sealed classes.
 */
public class ADContextEnricher {

    private final ADSchemaCache schemaCache;  // From ADR-060

    /**
     * Extract table names from SQL statement.
     * Mirrors idempiere-hub ADContextService.extractTableNames().
     */
    public Set<String> extractTableNames(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        // Pattern: FROM/JOIN followed by table name (with optional alias)
        Pattern p = Pattern.compile(
            "(?:FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_]*)",
            Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        while (m.find()) {
            String name = m.group(1).toUpperCase();
            if (!SQL_RESERVED_WORDS.contains(name)) {
                tables.add(name);
            }
        }
        return tables;
    }

    /**
     * Build context JSON for a set of tables.
     * Mirrors idempiere-hub ADContextService.getContextForTables().
     */
    public JSONObject buildContext(Set<String> tableNames, String language) {
        JSONObject context = new JSONObject();
        for (String tableName : tableNames) {
            ADTableMeta meta = schemaCache.getTableMetadata(tableName);
            if (meta == null) continue;

            JSONObject tableCtx = new JSONObject();
            tableCtx.put("name", meta.getName());
            tableCtx.put("description", meta.getDescription());
            tableCtx.put("accessLevel", describeAccessLevel(meta.getAccessLevel()));

            // Column hints (only FK and notable columns)
            JSONObject colHints = new JSONObject();
            for (ADColumnMeta col : meta.getColumns()) {
                if (col.getForeignTable() != null || col.isKey()) {
                    JSONObject colCtx = new JSONObject();
                    colCtx.put("name", col.getName());
                    if (col.getForeignTable() != null) {
                        colCtx.put("foreignTable", col.getForeignTable());
                    }
                    colHints.put(col.getColumnName(), colCtx);
                }
            }
            tableCtx.put("columns", colHints);

            // Relationships
            JSONArray relations = new JSONArray();
            for (ADRelation rel : meta.getRelations()) {
                JSONObject relJson = new JSONObject();
                relJson.put("table", rel.getForeignTable());
                relJson.put("type", rel.getType());
                relations.put(relJson);
            }
            tableCtx.put("relatedTables", relations);

            // Usage hint based on table prefix
            tableCtx.put("hint", getUsageHint(tableName));

            context.put(tableName, tableCtx);
        }
        return context;
    }

    /**
     * Usage hints based on iDempiere table naming conventions.
     * Mirrors idempiere-hub ADContextService pattern.
     */
    private String getUsageHint(String tableName) {
        if (tableName.startsWith("C_")) return "Core business table (Customer/Commerce domain)";
        if (tableName.startsWith("M_")) return "Material Management table";
        if (tableName.startsWith("AD_")) return "Application Dictionary (system/config)";
        if (tableName.startsWith("GL_")) return "General Ledger (accounting)";
        if (tableName.startsWith("S_")) return "Service/Resource table";
        if (tableName.startsWith("HR_")) return "Human Resources table";
        if (tableName.startsWith("PP_")) return "Manufacturing/Production table";
        return "Custom/extension table";
    }

    private String describeAccessLevel(String level) {
        switch (level) {
            case "1": return "Organization only";
            case "2": return "Client+Organization";
            case "3": return "Client only";
            case "4": return "System only";
            case "6": return "System+Client";
            case "7": return "All";
            default: return "Unknown (" + level + ")";
        }
    }
}
```

## Integration with ERPTools

```java
// In ERPTools.queryDatabase():
@Tool("Execute a SQL query against the iDempiere database")
public String queryDatabase(String sql) {
    // 1. Pre-validate tables (ADR-060)
    Set<String> tables = contextEnricher.extractTableNames(sql);
    for (String table : tables) {
        if (!schemaCache.tableExists(table)) {
            List<String> suggestions = schemaCache.suggestTable(table);
            return errorWithSuggestions(table, suggestions);
        }
    }

    // 2. Execute (existing)
    SecureQueryResult result = executor.executeSecureQuery(request);

    // 3. Enrich with context (NEW)
    JSONObject enriched = new JSONObject();
    enriched.put("data", result.toJSON());
    enriched.put("rowCount", result.getRowCount());
    enriched.put("tableContext", contextEnricher.buildContext(tables,
        Env.getAD_Language(Env.getCtx())));

    return enriched.toString();
}
```

## Implementation Plan

### Phase 1: ADContextEnricher class (3 days)
1. Create `ADContextEnricher` in `com.cloudempiere.ai.database`
2. Implement `extractTableNames()` with SQL reserved word filtering
3. Implement `buildContext()` using `ADSchemaCache` from ADR-060
4. Add usage hints and access level descriptions

### Phase 2: ERPTools integration (2 days)
1. Inject `ADContextEnricher` into `ERPTools`
2. Add context enrichment to `queryDatabase()` results
3. Add context to `searchRecords()` and `lookupRecord()` results
4. Update tool descriptions to mention context availability

### Phase 3: Smart context (future)
1. Only include context for columns that appear in results (not all table columns)
2. Add window association (which iDempiere window displays this table)
3. Add "suggested next queries" based on relationships

## Related ADRs

### This Plugin
- [ADR-060](060-ad-metadata-schema-cache.md) - AD Schema Cache (prerequisite)
- [ADR-007](007-database-security-model.md) - Database Security Model
- [ADR-035](035-java-version-strategy.md) - Java Version Strategy (Java 11 constraint)

### idempiere-hub (Reference Implementation)
- **ADR-008**: Application Dictionary Registry
- **ADR-055**: Tool Ecosystem Review (QueryToolLogic + ADContextService integration)
- **Source**: `src/main/java/org/idempiere/cli/ai/shared/ADContextService.java`
- **Source**: `src/main/java/org/idempiere/cli/ai/shared/QueryToolLogic.java`

## Notes

- The idempiere-hub uses **Java 17+** features (records, text blocks, pattern matching). All code in this plugin must use **Java 11** equivalents (regular classes, string concatenation, traditional switch).
- The idempiere-hub's `ADContextService` uses Quarkus CDI (`@ApplicationScoped`, `@Inject`). This plugin uses OSGi Declarative Services (`@Component`, `@Reference`) or direct instantiation.
- Connection management: idempiere-hub uses `DatabaseConnectionProvider` with pooling. This plugin uses `DB.getConnectionRW()` / `DB.close()` (iDempiere standard).
