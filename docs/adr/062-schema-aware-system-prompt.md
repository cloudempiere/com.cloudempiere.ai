# ADR-062: Schema-Aware System Prompt and RAG Integration

## Status

Proposed

## Date

2026-03-11

## Deciders

- Development Team

## Context and Problem Statement

The AI plugin has two disconnected capabilities that should work together:

1. **System prompt** (`ERPAgent.SYSTEM_PROMPT`) - Static, hardcoded, contains no schema knowledge. The LLM starts every conversation blind to the database structure.
2. **ADMetadataIngestor** - Generates vector embeddings of AD_Window, AD_Tab, AD_Process, AD_Table into an EmbeddingStore. But this RAG data is **never queried during chat** - it's generated but unused.

The **idempiere-hub** project solves this with a multi-tier approach (ADR-008, ADR-024, ADR-053):
- **Tier 1**: Static schema cache for fast exact lookups (implemented in our ADR-060)
- **Tier 2**: Dynamic context enrichment alongside query results (implemented in our ADR-061)
- **Tier 3**: Schema hints injected into system prompt + RAG for semantic discovery

This ADR addresses Tier 3 - making the system prompt schema-aware and wiring RAG into the chat flow.

### Java Version Constraint

**Important:** This plugin runs on **Java 11** (iDempiere v10), while idempiere-hub uses **Java 17+** (Quarkus 3.x). All implementations must use Java 11 compatible code. See [ADR-035](035-java-version-strategy.md).

### Reference Implementation

The idempiere-hub implements this via:
- **ADR-053**: Build-Time Model Discovery - embed `model-metadata.json` in JAR to prevent hallucinations
- **ADR-008**: Application Dictionary Registry - structured JSON schemas for AI consumption
- **`CliRouterAgent.SYSTEM_PROMPT`** - Rich system prompt with iDempiere naming conventions and domain knowledge
- **`RagService`** - Semantic search across AD metadata, wiki content, and knowledge base entries
- **`ADMetadataIngestor`** - Comprehensive ingestion of AD_Table, AD_Window, AD_Process, AD_Tab, AD_Field, AD_Reference

## Decision Drivers

- **Reduce tool call round-trips** - LLM should know common tables without calling `listTables()` first
- **Enable semantic discovery** - "What tables store customer data?" should work without exact name knowledge
- **Wire existing code** - `ADMetadataIngestor` and `RAGContextManager` already exist, just need connection
- **Token efficiency** - Don't inject entire schema (800+ tables) into every prompt
- **Dynamic, not static** - Schema hints should reflect actual customer's AD, not hardcoded defaults
- **Java 11 compatible** - No records, sealed classes, text blocks, or other Java 17+ features

## Considered Options

1. **Dynamic schema hints in system prompt + wired RAG** - Inject top-N frequently used tables into prompt, wire RAG for semantic search
2. **Full schema in system prompt** - Inject all table/column metadata into system prompt
3. **RAG-only** - Wire RAG but don't modify system prompt
4. **Build-time embedding** - Port idempiere-hub's ADR-053 (Maven plugin generates model-metadata.json)

## Decision Outcome

**Chosen option:** "Dynamic schema hints in system prompt + wired RAG", because it balances token efficiency with schema awareness, uses existing code (`ADMetadataIngestor`, `RAGContextManager`), and adapts to each customer's AD.

### Confirmation

- System prompt includes top-20 most-used table names with descriptions
- `RAGContextManager` is queried before SQL generation for semantic table discovery
- LLM can answer "What tables store customer data?" without tool calls
- Token usage for schema hints < 500 tokens per conversation
- `ADMetadataIngestor` output is queried during chat (not just generated)

## Architecture

### Three-Tier Schema Intelligence

```
┌─────────────────────────────────────────────────────────────────┐
│                   Schema Intelligence Tiers                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                    │
│  TIER 1: System Prompt (always available)                        │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ "Common iDempiere tables:                                  │  │
│  │  - C_BPartner: Business Partners (customers, vendors)      │  │
│  │  - C_Order / C_OrderLine: Sales/Purchase Orders            │  │
│  │  - C_Invoice / C_InvoiceLine: Invoices                     │  │
│  │  - M_Product: Products and Services                        │  │
│  │  - M_InOut / M_InOutLine: Shipments/Receipts               │  │
│  │  - C_Payment: Payments                                     │  │
│  │  ... (top 20 by usage)"                                    │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              │                                    │
│  TIER 2: RAG Semantic Search (on demand)                        │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ User: "tables for warehouse management"                    │  │
│  │ → RAG finds: M_Warehouse, M_Locator, M_Movement,          │  │
│  │              M_Inventory, M_InOut                           │  │
│  │ → Injected as augmented context before SQL generation      │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              │                                    │
│  TIER 3: Schema Cache (exact lookup, validation)                │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ ADSchemaCache.getTableMetadata("M_Warehouse")              │  │
│  │ → Full columns, types, relationships (ADR-060)             │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                    │
└─────────────────────────────────────────────────────────────────┘
```

### System Prompt Enhancement

```java
/**
 * Builds dynamic system prompt with schema hints.
 * Called once per chat session initialization.
 *
 * NOTE: Java 11 - no text blocks, use string concatenation.
 */
public class SchemaAwarePromptBuilder {

    private final ADSchemaCache schemaCache;

    /**
     * Generate schema hints section for system prompt.
     * Includes top-N most important iDempiere tables with descriptions.
     */
    public String buildSchemaHints() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n[IDEMPIERE SCHEMA KNOWLEDGE]\n");
        sb.append("Key iDempiere tables available for queries:\n\n");

        // Core business tables (always included)
        String[] coreTables = {
            "C_BPartner", "C_Order", "C_OrderLine",
            "C_Invoice", "C_InvoiceLine",
            "M_Product", "M_Product_Category",
            "M_InOut", "M_InOutLine",
            "C_Payment", "C_BankStatement",
            "M_Warehouse", "M_Locator",
            "AD_User", "C_Currency",
            "C_DocType", "M_PriceList"
        };

        for (String tableName : coreTables) {
            ADTableMeta meta = schemaCache.getTableMetadata(tableName);
            if (meta != null) {
                sb.append("- ").append(tableName)
                   .append(": ").append(meta.getDescription());

                // Add key FK columns
                List<String> fkHints = new ArrayList<>();
                for (ADColumnMeta col : meta.getColumns()) {
                    if (col.getForeignTable() != null && !col.isKey()) {
                        fkHints.add(col.getColumnName()
                            + " -> " + col.getForeignTable());
                    }
                }
                if (!fkHints.isEmpty() && fkHints.size() <= 5) {
                    sb.append(" [FK: ")
                       .append(String.join(", ", fkHints))
                       .append("]");
                }
                sb.append("\n");
            }
        }

        sb.append("\nNaming conventions:\n");
        sb.append("- C_* = Customer/Commerce, M_* = Material, ");
        sb.append("AD_* = Dictionary, GL_* = Accounting\n");
        sb.append("- *_ID columns are foreign keys (e.g., C_BPartner_ID -> C_BPartner)\n");
        sb.append("- Use listTables or getTableMetadata tools for tables not listed above\n");

        return sb.toString();
    }
}
```

### RAG Wiring

```java
/**
 * Wire ADMetadataIngestor output into AIService chat flow.
 *
 * Currently ADMetadataIngestor generates embeddings but they are
 * never queried. This connects them to the chat pipeline.
 */
public class SchemaRAGService {

    private final RAGContextManager ragManager;  // Existing class
    private final ADSchemaCache schemaCache;       // From ADR-060

    /**
     * Semantic search for relevant tables/windows/processes
     * before the LLM generates SQL.
     *
     * Called when user message contains data-related keywords
     * but doesn't specify exact table names.
     */
    public String findRelevantContext(String userMessage) {
        // 1. Check if message is data-related
        if (!isDataRelated(userMessage)) {
            return null;
        }

        // 2. Search RAG for relevant AD elements
        List<EmbeddingMatch<TextSegment>> matches =
            ragManager.search(userMessage, 5);

        if (matches.isEmpty()) {
            return null;
        }

        // 3. Build context from matches
        StringBuilder context = new StringBuilder();
        context.append("\n[RELEVANT SCHEMA CONTEXT]\n");
        context.append("Based on your question, these AD elements may be relevant:\n\n");

        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            context.append("- ").append(segment.text()).append("\n");

            // If it's a table, add column summary from cache
            String tableName = extractTableName(segment);
            if (tableName != null) {
                ADTableMeta meta = schemaCache.getTableMetadata(tableName);
                if (meta != null) {
                    context.append("  Key columns: ");
                    List<String> keyCols = meta.getColumns().stream()
                        .filter(c -> c.isKey() || c.isMandatory()
                                    || c.getForeignTable() != null)
                        .limit(8)
                        .map(c -> c.getColumnName())
                        .collect(Collectors.toList());
                    context.append(String.join(", ", keyCols));
                    context.append("\n");
                }
            }
        }

        return context.toString();
    }

    private boolean isDataRelated(String message) {
        String lower = message.toLowerCase();
        return lower.contains("show") || lower.contains("query")
            || lower.contains("find") || lower.contains("list")
            || lower.contains("how many") || lower.contains("report")
            || lower.contains("table") || lower.contains("data")
            || lower.contains("customer") || lower.contains("order")
            || lower.contains("product") || lower.contains("invoice");
    }
}
```

### AIService Integration

```java
// In AIService.chatStreamingWithContext():
public void chatStreamingWithContext(String sessionId, String message,
                                      AIStreamCallback callback) {
    // Existing: language detection
    String langInstruction = languageService.getLanguageInstruction();

    // Existing: base system prompt
    String basePrompt = ERPAgent.SYSTEM_PROMPT;

    // NEW: Schema hints (built once per session, cached)
    String schemaHints = promptBuilder.buildSchemaHints();

    // NEW: RAG context for this specific message
    String ragContext = schemaRAG.findRelevantContext(message);

    // Assemble full prompt
    String systemPrompt = langInstruction
        + basePrompt
        + schemaHints
        + (ragContext != null ? ragContext : "");

    // Continue with existing agent creation...
}
```

## Implementation Plan

### Phase 1: Schema Hints in System Prompt (2 days)
1. Create `SchemaAwarePromptBuilder` class
2. Generate schema hints section from `ADSchemaCache` (ADR-060)
3. Inject into `AIService` system prompt assembly
4. Cache hints per session (don't rebuild per message)

### Phase 2: Wire RAG (3 days)
1. Create `SchemaRAGService` connecting `RAGContextManager` to chat flow
2. Add data-related message detection
3. Inject RAG context as augmented prompt section
4. Ensure `ADMetadataIngestor` runs at plugin activation (or first chat)

### Phase 3: Operator-configurable schema hints (future, ties to ADR-059)
1. Allow sysadmin to configure which tables appear in schema hints
2. Store configuration in `AIG_Prompt_Config` table
3. Support per-organization table lists

## Token Budget

| Component | Est. Tokens | When |
|---|---|---|
| Schema hints (20 tables) | ~300-400 | Every message |
| RAG context (5 matches) | ~200-300 | Data-related messages only |
| Total overhead | ~400-700 | Worst case |

Compared to the LLM calling `listTables()` + `getTableMetadata()` x N (which costs ~2000+ tokens in tool calls + responses), this saves significant tokens.

## Related ADRs

### This Plugin
- [ADR-060](060-ad-metadata-schema-cache.md) - AD Schema Cache (prerequisite for schema hints)
- [ADR-061](061-sql-context-enrichment.md) - SQL Query Context Enrichment (runtime enrichment)
- [ADR-012](012-rag-based-context-retrieval.md) - RAG-Based Context Retrieval (existing RAG architecture)
- [ADR-040](040-embedding-ingestion-evolution.md) - Embedding Ingestion Evolution (ADMetadataIngestor)
- [ADR-059](059-configurable-system-prompt-architecture.md) - Configurable System Prompt (operator addendum layer)
- [ADR-035](035-java-version-strategy.md) - Java Version Strategy (Java 11 constraint)

### idempiere-hub (Reference Implementation)
- **ADR-008**: Application Dictionary Registry - structured AD knowledge for AI consumption
- **ADR-024**: AD Metadata Caching - multi-tier caching (memory / file / database)
- **ADR-053**: Build-Time Model Discovery - embed model-metadata.json to eliminate hallucinations
- **ADR-021**: RAG Architecture - multi-source RAG with AD metadata, wiki, knowledge base
- **Source**: `src/main/java/org/idempiere/cli/ai/shared/ADContextService.java`
- **Source**: `src/main/java/org/idempiere/cli/rag/ingest/ADMetadataIngestor.java`
- **Source**: `src/main/java/org/idempiere/cli/ai/langchain/CliRouterAgent.java` (system prompt)

## Notes

### Java 11 vs Java 17+ Differences

| Feature | idempiere-hub (Java 17+) | This plugin (Java 11) |
|---|---|---|
| Records | `record TableMetadata(...)` | Regular class with getters |
| Text blocks | `"""..."""` | String concatenation / StringBuilder |
| Pattern matching | `if (x instanceof String s)` | Cast after instanceof |
| Sealed classes | `sealed interface` | Regular interface |
| Stream.toList() | `.toList()` | `.collect(Collectors.toList())` |
| Switch expressions | `switch(x) { case "a" -> ...}` | Traditional switch |

### Embedding Model Constraint

The idempiere-hub uses Ollama's `mxbai-embed-large` (1024 dimensions) or AWS Bedrock embedding models. The AI plugin's embedding infrastructure should use whatever is configured in `AIG_Provider` for embeddings. If no embedding model is available, the RAG tier (Phase 2) is skipped and only the cache-based schema hints (Phase 1) are used.

### Build-Time vs Runtime

The idempiere-hub's ADR-053 proposes **build-time** metadata embedding via a Maven plugin. For the iDempiere AI plugin, we chose **runtime** loading because:
1. The plugin runs inside iDempiere with direct DB access - no need for build-time extraction
2. Runtime loading automatically reflects the customer's actual AD (custom tables, modifications)
3. Avoids build-time dependency on a running iDempiere server
4. Simpler deployment (no Maven plugin configuration needed)
