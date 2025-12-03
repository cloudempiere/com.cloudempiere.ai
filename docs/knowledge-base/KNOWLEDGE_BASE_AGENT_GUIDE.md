# Knowledge Base Agent Integration Guide

## Overview

The Knowledge Base Agent is a specialized AI-powered component that helps maintain and organize qualified knowledge bases in iDempiere. It analyzes new content and recommends optimal placement in the knowledge base hierarchy based on existing structure and best practices.

## Components

### 1. **EditorJsParser** (`kb/parser/EditorJsParser.java`)
Converts editor.js JSON format to markdown for AI analysis.

**Features:**
- Parse editor.js blocks (heading, paragraph, list, code, quote, image, table, etc.)
- Convert to structured markdown
- Preserve formatting and links
- Convert markdown to plain text for similarity matching

**Usage:**
```java
String editorJsJson = "{ \"blocks\": [...] }";
String markdown = EditorJsParser.parseToMarkdown(editorJsJson);

String plainText = EditorJsParser.markdownToPlainText(markdown);
```

### 2. **KnowledgeBaseContextProvider** (`context/impl/KnowledgeBaseContextProvider.java`)
Extends `IAIContextProvider` to read k_entry navigation tree from iDempiere.

**Context Type:** `"KNOWLEDGE_BASE"`

**Features:**
- Loads complete KB hierarchy from k_entry table
- Builds navigation tree structure
- Parses editor.js content to markdown
- Returns structured JSON for AI consumption

**Usage:**
```java
AIContextProviderRegistry registry = AIContextProviderRegistry.getInstance();
IAIContextProvider provider = registry.getProvider("KNOWLEDGE_BASE");

ContextParameters params = ContextParameters.forKnowledgeBase("my_kb_type");
JSONObject context = provider.extractContext(ctx, 0, params);

// Context includes:
// {
//   "k_type": "my_kb_type",
//   "total_entries": 42,
//   "tree_structure": { ... },
//   "user_context": { ... }
// }
```

### 3. **KnowledgeBaseEntry & KnowledgeBaseHierarchy** (`kb/dto/`)

**KnowledgeBaseEntry:**
- Represents a single k_entry record
- Fields: k_entry_id, name, title, description, content, parent_id, sequence_no, k_type, language
- Includes both original content and markdown representation

**KnowledgeBaseHierarchy:**
- In-memory tree structure of all KB entries
- Methods:
  - `addEntry(entry)` - Add entry to hierarchy
  - `getChildren(parentId)` - Get child entries
  - `getEntry(id)` - Get entry by ID
  - `getBreadcrumbPath(entryId)` - Get full hierarchy path
  - `getDepth(entryId)` - Get nesting depth
  - `getTreeStructure()` - Get tree as string for debugging

### 4. **KnowledgeBaseSimilarityAnalyzer** (`kb/analysis/KnowledgeBaseSimilarityAnalyzer.java`)
Finds similar/duplicate entries using keyword-based matching.

**Features:**
- Jaccard similarity coefficient for text matching
- Keyword extraction with stop-word filtering
- Scores matches by title, description, and content
- Identifies duplicates (score > 0.85) and similar entries (score > 0.60)

**Usage:**
```java
SimilarityResult result = KnowledgeBaseSimilarityAnalyzer.analyzeSimilarity(
    userContent,
    hierarchy
);

if (result.isHasDuplicate()) {
    // Warning: This looks like an existing entry
    for (SimilarityResult.SimilarEntry similar : result.getTopSimilar(3)) {
        System.out.println(similar.getEntry().getTitle() +
                          ": " + similar.getSimilarityScore());
    }
}
```

### 5. **PlacementRecommendation** (`kb/dto/PlacementRecommendation.java`)
Final recommendation from the Knowledge Base Agent.

**Actions:**
- `CREATE_NEW` - Create new KB entry
- `EXTEND_EXISTING` - Add content to existing entry
- `UPDATE_EXISTING` - Replace existing entry content
- `REPLACE_EXISTING` - Replace with new version
- `DUPLICATE_WARNING` - Duplicate detected, user review needed

**Fields:**
- `action` - Recommended action
- `targetEntryId` - ID of target entry (if extending/updating)
- `suggestedParentId` - Parent ID for new entry
- `confidence` - 0.0 to 1.0 confidence score
- `title` - Suggested title for new entry
- `reasoning` - Why this recommendation
- `bestPractices` - Applied KB best practices
- `warningMessage` - Any warnings or conflicts

**Usage:**
```java
PlacementRecommendation rec = agent.analyzePlacement(ctx, "my_kb_type",
                                                      userContent,
                                                      userTitle);

System.out.println("Action: " + rec.getAction());
System.out.println("Confidence: " + rec.getConfidencePercent() + "%");
System.out.println("Reasoning: " + rec.getReasoning());

if (rec.getWarningMessage() != null) {
    System.out.println("Warning: " + rec.getWarningMessage());
}
```

### 6. **KnowledgeBaseAgent** (`kb/KnowledgeBaseAgent.java`)
Main orchestrator that coordinates the entire workflow.

**Constructor:**
```java
KnowledgeBaseAgent(IAIProvider aiProvider)
```

**Main Method:**
```java
PlacementRecommendation analyzePlacement(
    Properties ctx,              // iDempiere context
    String kType,                // Knowledge base type
    String userContent,          // New content to add
    String userTitle             // Suggested title (optional)
) throws Exception
```

**Workflow:**
1. Load KB hierarchy via KnowledgeBaseContextProvider
2. Analyze similarity using KnowledgeBaseSimilarityAnalyzer
3. Call Claude AI to analyze structure and recommend placement
4. Return structured PlacementRecommendation

## Integration with AI System

### 1. **Registration**

KnowledgeBaseContextProvider is auto-registered in `AIContextProviderRegistry`:

```java
AIContextProviderRegistry registry = AIContextProviderRegistry.getInstance();
// KNOWLEDGE_BASE provider is available
```

### 2. **Using with AI Providers**

The agent uses Claude for semantic analysis:

```java
AIRequest request = AIRequest.builder()
    .withModel("claude-3-5-sonnet-20241022")
    .withTemperature(0.3) // Deterministic for structured output
    .addMessage(new AIMessage("user", analysisPrompt))
    .build();

AIResponse response = aiProvider.generateText(request);
```

### 3. **AI Prompt Design**

The agent builds a structured prompt that includes:
- Current KB structure and hierarchy
- New content being analyzed
- Similar entries (if any)
- Requested recommendations (action, parent, confidence, best practices)
- JSON format for response parsing

## Usage Example

### Complete Workflow

```java
// 1. Initialize
Properties ctx = getIdempierContext();
IAIProvider aiProvider = getAnthropicProvider();
KnowledgeBaseAgent agent = new KnowledgeBaseAgent(aiProvider);

// 2. User provides new content
String newContent = "# How to Deploy Cloudempiere to Docker\n" +
                    "## Prerequisites\n" +
                    "- Docker installed\n" +
                    "...\n";
String title = "Docker Deployment Guide";

// 3. Get recommendation
PlacementRecommendation rec = agent.analyzePlacement(
    ctx,
    "DEPLOYMENT",  // KB type
    newContent,
    title
);

// 4. Display recommendation to user
System.out.println("Recommendation: " + rec.getAction());
System.out.println("Confidence: " + rec.getConfidencePercent() + "%");
System.out.println("\nReasoning:\n" + rec.getReasoning());

if (rec.getAction() == PlacementRecommendation.Action.CREATE_NEW) {
    System.out.println("\nSuggested location:");
    System.out.println("Parent: " + rec.getSuggestedParentId());
    System.out.println("Title: " + rec.getTitle());
} else if (rec.getAction() == PlacementRecommendation.Action.EXTEND_EXISTING) {
    System.out.println("\nAdd to existing entry: " + rec.getTargetEntryId());
}

// 5. User approves/modifies and saves to k_entry table
if (userApproves(rec)) {
    saveToKnowledgeBase(rec, newContent);
}
```

## Database Requirements

### k_entry Table
```sql
CREATE TABLE k_entry (
    k_entry_id      INT PRIMARY KEY,
    name            VARCHAR(100),
    title           VARCHAR(200),
    description     TEXT,
    content         LONGTEXT,  -- editor.js JSON or markdown
    parent_id       INT,
    seqno           INT,
    k_type          VARCHAR(50),
    isactive        CHAR(1),
    ad_language     VARCHAR(5),
    ...
);
```

### k_type Table
```sql
CREATE TABLE k_type (
    k_type          VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(100),
    description     TEXT,
    ...
);
```

## Best Practices

### 1. Content Format
- Prefer **editor.js JSON** for rich formatting
- Fallback to **markdown** for plain text
- Avoid mixing formats in same KB type

### 2. KB Structure
- Organize with 2-3 levels of hierarchy
- Keep root entries broad (e.g., "Deployment", "Administration")
- Use consistent naming conventions
- Maintain alphabetical order at each level

### 3. Similarity Threshold
- **> 0.85**: Likely duplicate - user review required
- **0.60-0.85**: Similar content - consider extending or creating new subsection
- **< 0.60**: Not similar - can safely create new entry

### 4. AI Confidence
- **> 0.80**: High confidence recommendation
- **0.60-0.80**: Follow recommendation with caution
- **< 0.60**: Requires user review

## Error Handling

```java
try {
    PlacementRecommendation rec = agent.analyzePlacement(ctx, kType, content, title);

    // Check warnings
    if (rec.getAction() == PlacementRecommendation.Action.DUPLICATE_WARNING) {
        log.warning("Potential duplicate: " + rec.getWarningMessage());
        return getUserApproval(rec);  // Let user decide
    }

    // Check confidence
    if (!rec.isHighConfidence()) {
        log.info("Low confidence recommendation: " + rec.getConfidencePercent() + "%");
        return getUserApproval(rec);
    }

    return rec;

} catch (Exception e) {
    log.log(Level.SEVERE, "Failed to analyze KB placement", e);
    return getDefaultRecommendation();  // Create new at root level
}
```

## Performance Considerations

### Database Queries
- KnowledgeBaseContextProvider caches hierarchy in memory
- For large KBs (> 1000 entries), consider pagination
- Lazy-load content only when needed

### AI API Calls
- Temperature set to 0.3 for deterministic output
- Consider caching frequently accessed KBs
- Rate limit to 1-2 concurrent analyses

### Similarity Analysis
- O(n) complexity where n = number of KB entries
- Keyword extraction is optimized with stop-word filtering
- For > 5000 entries, consider vector embeddings instead

## Future Enhancements

1. **Vector Embeddings**: Use Claude embeddings for more accurate similarity
2. **Multi-Language Support**: Better handling of non-English content
3. **Collaborative Features**: Track user edits and learning
4. **Analytics**: Monitor KB quality metrics
5. **Auto-Tagging**: Suggest tags based on content
6. **Full-Text Search**: Index KB for quick lookups

## Troubleshooting

### Issue: "KnowledgeBaseContextProvider not registered"
**Solution:** Ensure AIContextProviderRegistry is properly initialized
```java
AIContextProviderRegistry.getInstance().getProvider("KNOWLEDGE_BASE");
```

### Issue: "Failed to parse editor.js JSON"
**Solution:** Check content format - may be markdown, not editor.js
```java
if (!content.startsWith("{")) {
    // Treat as markdown, not editor.js
    entry.setContentMarkdown(content);
}
```

### Issue: Low similarity scores despite obvious duplicates
**Solution:** Increase keyword overlap threshold or use semantic embeddings
```java
// Current: keyword-based Jaccard similarity
// Future: vector embeddings for semantic similarity
```

### Issue: AI returns invalid JSON in response
**Solution:** Gracefully fallback to default recommendation
```java
try {
    JSONObject responseJson = new JSONObject(aiResponse);
} catch (JSONException e) {
    rec.setAction(PlacementRecommendation.Action.CREATE_NEW);
    rec.setConfidence(0.5);
    rec.setReasoning("Manual review recommended");
}
```

## API Reference

### EditorJsParser
```java
public static String parseToMarkdown(String editorJsJson)
public static String parseEditorJson(JSONObject editorObj)
public static String markdownToPlainText(String markdown)
```

### KnowledgeBaseContextProvider
```java
public String getContextType()  // Returns "KNOWLEDGE_BASE"
public JSONObject extractContext(Properties ctx, int windowNo, ContextParameters parameters)
public boolean validateContext(JSONObject context)
public String[] getSensitiveFields()
```

### KnowledgeBaseSimilarityAnalyzer
```java
public static SimilarityResult analyzeSimilarity(String userContent, KnowledgeBaseHierarchy hierarchy)
```

### KnowledgeBaseAgent
```java
public PlacementRecommendation analyzePlacement(Properties ctx, String kType, String userContent, String userTitle) throws Exception
```

## Security

- **Input Validation**: Content is validated for SQL injection
- **Sensitive Data**: No API keys or credentials in KB content
- **Access Control**: Respects iDempiere role-based permissions
- **Audit Trail**: All AI recommendations logged with user info

---

**Version:** 1.0
**Author:** Cloudempiere
**Last Updated:** November 2024
