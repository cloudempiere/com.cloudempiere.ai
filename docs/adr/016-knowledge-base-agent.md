# ADR-016: Knowledge Base Agent Domain

## Status

Proposed

## Date

2025-12-01

## Deciders

CloudEmpiere AI Team

## Context and Problem Statement

The Knowledge Base (KB) feature provides AI-assisted content management for iDempiere documentation, help articles, and structured content using Editor.js format. An initial implementation exists (commit `fe6d605`) but uses a custom agent architecture instead of the LangChain4j pattern established in ADR-002 and ADR-004.

This ADR defines Knowledge Base as a recognized domain and specifies how to align the existing implementation with the LangChain4j architecture.

## Decision Drivers

- **ADR Compliance** - Must align with LangChain4j adoption (ADR-002, ADR-004)
- **Orchestration** - Must integrate with OrchestratorAgent (ADR-010)
- **Security** - Must use SecureDatabaseQueryExecutor (ADR-007)
- **Reusability** - Must follow @Tool pattern for MCP exposure (ADR-003)
- **Existing Code** - Leverage existing implementation where possible

## Considered Options

1. **Refactor to LangChain4j @Tool methods** - Convert KnowledgeBaseAgent to @Tool annotations
2. **Keep custom implementation** - Bypass LangChain4j architecture
3. **Hybrid approach** - Keep utilities, refactor agent layer

## Decision Outcome

**Chosen option:** "Option 3: Hybrid approach", because it preserves valuable utility code (parsers, DTOs) while aligning the agent layer with LangChain4j architecture.

### Confirmation

- [ ] `KnowledgeBaseTools.java` created with `@Tool` annotations
- [ ] Integration with `OrchestratorAgent` verified
- [ ] All DB queries use `SecureDatabaseQueryExecutor`
- [ ] MCP tools exposed via idempiere-mcp-server
- [ ] Unit tests pass for all KB tools

## Pros and Cons of the Options

### Option 1: Full Refactor to LangChain4j

Convert everything to LangChain4j patterns.

- Good, because full ADR compliance
- Good, because consistent with other agents
- Bad, because significant rework (~1,000 lines)
- Bad, because delays KB feature availability

### Option 2: Keep Custom Implementation

Maintain current standalone KnowledgeBaseAgent.

- Good, because no rework needed
- Good, because feature already works
- Bad, because violates ADR-002, ADR-004, ADR-010
- Bad, because can't integrate with orchestrator
- Bad, because can't expose via MCP

### Option 3: Hybrid Approach (CHOSEN)

Keep utilities, refactor agent layer only.

- Good, because preserves working parser/DTO code
- Good, because aligns agent with LangChain4j
- Good, because moderate effort (~200 lines to change)
- Good, because enables MCP exposure
- Neutral, because some code restructuring needed

## More Information

### Knowledge Base Domain Definition

**Scope (from ADR-009 pattern):**

| Aspect | Definition |
|--------|------------|
| **Domain** | Knowledge Base Content Management |
| **Data Access** | KB_Article, KB_Category, KB_Tag tables |
| **Operations** | CRUD, search, similarity, hierarchy |
| **AI Capabilities** | Content analysis, recommendations, syntax validation |

**Tools to Implement:**

| Tool | Description | Risk Level |
|------|-------------|------------|
| `searchArticles` | Full-text search across KB | LOW |
| `getArticle` | Retrieve article by ID | LOW |
| `listCategories` | List KB categories | LOW |
| `analyzeContent` | Editor.js content metrics | LOW |
| `findSimilar` | Similarity-based recommendations | LOW |
| `validateSyntax` | Check Editor.js block support | LOW |
| `createArticle` | Create new KB article | MEDIUM |
| `updateArticle` | Update existing article | MEDIUM |
| `suggestPlacement` | AI placement recommendations | LOW |

### Current Implementation (fe6d605)

**Files to KEEP (utilities):**
```
src/com/cloudempiere/ai/kb/dto/
  EditorJsSyntaxInfo.java      ✅ Keep - Singleton DTO
  KnowledgeBaseEntry.java      ✅ Keep - Data transfer
  KnowledgeBaseHierarchy.java  ✅ Keep - Tree structure
  PlacementRecommendation.java ✅ Keep - AI output DTO
  SimilarityResult.java        ✅ Keep - Search result

src/com/cloudempiere/ai/kb/parser/
  EditorJsParser.java          ✅ Keep - Basic parser
  EditorJsParserEnhanced.java  ✅ Keep - Enhanced parser

src/com/cloudempiere/ai/context/impl/
  KnowledgeBaseContextProvider.java  ✅ Keep - Context extraction
```

**Files to REFACTOR:**
```
src/com/cloudempiere/ai/kb/
  KnowledgeBaseAgent.java      → Convert to KnowledgeBaseTools.java with @Tool

src/com/cloudempiere/ai/kb/database/
  KnowledgeBaseQuery.java      → Wrap with SecureDatabaseQueryExecutor

src/com/cloudempiere/ai/kb/analysis/
  KnowledgeBaseSimilarityAnalyzer.java    → Move to tool method
  KnowledgeBaseSimilarityAnalyzerDB.java  → Integrate with secure query
```

### Refactoring Plan

**Step 1: Create KnowledgeBaseTools.java (Week 1)**
```java
public class KnowledgeBaseTools {

    @Tool("Search knowledge base articles by keyword")
    public List<KnowledgeBaseEntry> searchArticles(
        @P("Search query") String query,
        @P("Maximum results") int limit
    ) {
        // Use existing KnowledgeBaseQuery logic
        // Wrap with SecureDatabaseQueryExecutor
    }

    @Tool("Analyze Editor.js content structure and metrics")
    public ContentMetrics analyzeContent(
        @P("Editor.js JSON content") String content
    ) {
        // Use existing EditorJsParserEnhanced
        return parser.analyze(content);
    }

    @Tool("Find similar articles based on content")
    public List<SimilarityResult> findSimilar(
        @P("Article ID to find similar") int articleId,
        @P("Similarity threshold 0.0-1.0") double threshold
    ) {
        // Use existing KnowledgeBaseSimilarityAnalyzerDB
    }

    // ... more tools
}
```

**Step 2: Register with OrchestratorAgent (Week 1)**
```java
// In OrchestratorAgent configuration
.tools(new KnowledgeBaseTools(queryExecutor, parser))
```

**Step 3: Add MCP Tools (Week 2)**
```java
// In idempiere-mcp-server TableTools.java or new KBTools.java
@Tool("kb_search")
public String searchKnowledgeBase(String query) {
    // Call cloudempiere-cli kb search command
}
```

**Step 4: Security Integration (Week 2)**
```java
// All queries through SecureDatabaseQueryExecutor
SecureQueryRequest request = new SecureQueryRequest.Builder()
    .query("SELECT * FROM KB_Article WHERE Name ILIKE ?")
    .parameters(List.of("%" + query + "%"))
    .userId(context.getAD_User_ID())
    .roleId(context.getAD_Role_ID())
    .build();

SecureQueryResult result = queryExecutor.executeSecureQuery(request);
```

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    OrchestratorAgent (ADR-010)                   │
│  ┌─────────────┬─────────────┬─────────────┬─────────────┐      │
│  │InventoryTools│ SalesTools │PurchaseTools│   KBTools   │      │
│  │  (ADR-011)  │  (ADR-011) │  (ADR-011)  │  (ADR-016)  │      │
│  └─────────────┴─────────────┴─────────────┴──────┬──────┘      │
└───────────────────────────────────────────────────┼──────────────┘
                                                    │
                    ┌───────────────────────────────┼───────────────┐
                    │         KnowledgeBaseTools    │               │
                    │  ┌─────────────────────────────────────────┐  │
                    │  │ @Tool searchArticles()                  │  │
                    │  │ @Tool getArticle()                      │  │
                    │  │ @Tool analyzeContent()                  │  │
                    │  │ @Tool findSimilar()                     │  │
                    │  │ @Tool validateSyntax()                  │  │
                    │  │ @Tool suggestPlacement()                │  │
                    │  └──────────────┬──────────────────────────┘  │
                    │                 │                             │
                    │  ┌──────────────▼──────────────┐              │
                    │  │ Utility Classes (KEEP)      │              │
                    │  │  - EditorJsParser           │              │
                    │  │  - EditorJsSyntaxInfo       │              │
                    │  │  - KnowledgeBaseEntry       │              │
                    │  │  - SimilarityAnalyzer       │              │
                    │  └──────────────┬──────────────┘              │
                    │                 │                             │
                    │  ┌──────────────▼──────────────┐              │
                    │  │ SecureDatabaseQueryExecutor │              │
                    │  │        (ADR-007)            │              │
                    │  └──────────────┬──────────────┘              │
                    └─────────────────┼─────────────────────────────┘
                                      │
                    ┌─────────────────▼─────────────────┐
                    │  iDempiere Database               │
                    │  - KB_Article                     │
                    │  - KB_Category                    │
                    │  - KB_Tag                         │
                    └───────────────────────────────────┘
```

### Implementation Timeline

| Week | Task | Effort |
|------|------|--------|
| 1 | Create `KnowledgeBaseTools.java` with @Tool annotations | 2 days |
| 1 | Integrate with SecureDatabaseQueryExecutor | 1 day |
| 1 | Register with OrchestratorAgent | 0.5 days |
| 2 | Add MCP tools to idempiere-mcp-server | 1 day |
| 2 | Unit tests for all KB tools | 1.5 days |
| 2 | Documentation update | 0.5 days |

**Total: 2 weeks**

### Related ADRs

- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j adoption (must follow @Tool pattern)
- [ADR-004](004-java-agent-framework.md) - Framework selection (LangChain4j chosen)
- [ADR-007](007-database-security-model.md) - Security model (must use SecureDatabaseQueryExecutor)
- [ADR-009](009-domain-boundaries-agent-scope.md) - Domain boundaries (KB is new domain)
- [ADR-010](010-agent-orchestration-architecture.md) - Orchestration (must integrate)
- [ADR-011](011-specialized-agent-scopes.md) - Specialized agents (KB added as 5th domain)

### References

- [Editor.js Documentation](https://editorjs.io/)
- [LangChain4j Tools Guide](https://docs.langchain4j.dev/tutorials/tools/)
- [Commit fe6d605](../../../commit/fe6d605) - Original KB implementation

---

*ADR-016 | Version 1.0 | 2025-12-01*
*Status: Proposed*
*Decision: Hybrid approach - keep utilities, refactor agent to LangChain4j @Tool pattern*
