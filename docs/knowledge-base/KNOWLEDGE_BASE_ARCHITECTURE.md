# Knowledge Base Agent Architecture

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    User Interface / Chat Panel                   │
│                                                                   │
│  User provides new KB content and optional title                 │
│  System shows recommendations and similar entries                │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│              KnowledgeBaseAgent (Orchestrator)                   │
│                                                                   │
│  1. Load KB Hierarchy → Context Provider                        │
│  2. Find Similar Entries → Similarity Analyzer                  │
│  3. Ask AI for Recommendation → Claude via AI Provider          │
│  4. Parse & Return Recommendation                              │
└──────────────┬──────────────────────────────┬───────────────────┘
               │                              │
        ┌──────▼──────────┐        ┌─────────▼─────────┐
        │   KB Context    │        │  Similarity       │
        │   Provider      │        │  Analyzer         │
        │                 │        │                   │
        │ Reads from:     │        │ Compares:         │
        │ • k_entry       │        │ • User content    │
        │ • k_type        │        │ • vs KB entries   │
        │                 │        │                   │
        │ Parses:         │        │ Uses:             │
        │ • editor.js JSON│        │ • Keyword extract │
        │ • to markdown   │        │ • Jaccard similar │
        └─────────────────┘        │ • Score filtering │
                                    └───────────────────┘
               │
        ┌──────▼────────────────────────────────┐
        │   KnowledgeBaseHierarchy               │
        │                                        │
        │  In-Memory Tree Structure:             │
        │  • Root entries (parent_id = 0)       │
        │  • Child entries (parent_id > 0)      │
        │  • Depth/breadcrumb calculation       │
        │  • Content preview (markdown)         │
        └─────────────────────────────────────────┘
               │
        ┌──────▼────────────────────────────────┐
        │   Editor.js Parser                     │
        │                                        │
        │  Converts:                             │
        │  • editor.js JSON → markdown           │
        │  • Headings, paragraphs, lists        │
        │  • Code blocks, quotes, tables        │
        │  • Back to plain text for matching    │
        └─────────────────────────────────────────┘
               │
        ┌──────▼────────────────────────────────┐
        │   Database (iDempiere)                 │
        │                                        │
        │  Tables:                               │
        │  • k_entry (articles/docs)            │
        │  • k_type (KB categories)             │
        └─────────────────────────────────────────┘


        ┌──────────────────────────────────────────┐
        │   Claude AI Provider                     │
        │                                          │
        │  Receives:                               │
        │  • KB structure (tree)                  │
        │  • User content                        │
        │  • Similar entries (if any)            │
        │  • Best practices examples             │
        │                                         │
        │  Returns:                               │
        │  • Recommended action (CREATE/EXTEND)  │
        │  • Target entry or parent ID           │
        │  • Confidence score                    │
        │  • Reasoning and best practices        │
        └──────────────────────────────────────────┘
               │
        ┌──────▼────────────────────────────────┐
        │   PlacementRecommendation              │
        │                                        │
        │  Fields:                               │
        │  • action (CREATE_NEW, EXTEND, etc)   │
        │  • targetEntryId                      │
        │  • suggestedParentId                  │
        │  • confidence (0.0-1.0)               │
        │  • title, reasoning, warnings         │
        └─────────────────────────────────────────┘
               │
               ▼
        Return to User Interface
```

## Data Flow Diagram

```
User Input
  ├─ Content: "How to deploy Cloudempiere to Docker"
  ├─ Title: "Docker Deployment"
  └─ KB Type: "DEPLOYMENT"
         │
         ▼
  ┌─────────────────────────────┐
  │ KnowledgeBaseAgent           │
  │ .analyzePlacement()          │
  └──────────┬──────────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
┌─────────────┐   ┌──────────────────┐
│KB Context   │   │Similarity        │
│Provider     │   │Analyzer          │
│             │   │                  │
│Loads:       │   │Finds:            │
│• 42 entries │   │• Docker guide    │
│• 3 levels   │   │  (95% similar)   │
│• Tree       │   │• Kubernetes      │
│• Markdown   │   │  (60% similar)   │
└─────────────┘   │• DevOps section  │
    │             │  (50% match)     │
    │             └──────────────────┘
    │                    │
    │                    │
    └────────┬───────────┘
             │
             ▼
    ┌──────────────────────────────┐
    │ Build AI Prompt              │
    │                              │
    │ "You are a KB Assistant.     │
    │  Here's the structure...     │
    │  Here's new content...       │
    │  Here are similar entries... │
    │  Recommend placement..."     │
    └──────────────┬───────────────┘
                   │
                   ▼
    ┌──────────────────────────────┐
    │ Claude API Call              │
    │                              │
    │ model: claude-3.5-sonnet     │
    │ temperature: 0.3             │
    └──────────────┬───────────────┘
                   │
                   ▼
    ┌──────────────────────────────┐
    │ AI Response (JSON)           │
    │                              │
    │ {                            │
    │   "action": "EXTEND_EXISTING"│
    │   "target_entry_id": 1234,   │
    │   "confidence": 92,          │
    │   "reasoning": "Docker guide │
    │    exists; content adds      │
    │    Kubernetes integration"   │
    │ }                            │
    └──────────────┬───────────────┘
                   │
                   ▼
    ┌──────────────────────────────┐
    │ Parse & Build Recommendation │
    │                              │
    │ PlacementRecommendation {    │
    │   action: EXTEND_EXISTING    │
    │   targetEntryId: 1234        │
    │   confidence: 0.92           │
    │   reasoning: ...             │
    │ }                            │
    └──────────────┬───────────────┘
                   │
                   ▼
    Return Recommendation to User
```

## Component Interaction Sequence

```
User                    KB Agent                Context Provider        AI Provider
  │                         │                         │                      │
  ├──NewContent────────────►│                         │                      │
  │                         │                         │                      │
  │                         ├──extractContext────────►│                      │
  │                         │                         ├─Query k_entry        │
  │                         │                         │ Parse editor.js      │
  │                         │◄──KnowledgeBase Hierarchy──────────────────────│
  │                         │                         │                      │
  │                         ├──analyzeSimilarity──────┐                      │
  │                         │                         │ (internal)           │
  │                         │◄──SimilarityResult──────┘                      │
  │                         │                         │                      │
  │                         ├──buildPrompt─────────────────────────────────┐ │
  │                         │                         │                    │ │
  │                         ├──generateText──────────────────────────────►│ │
  │                         │                         │                  └─►├─Claude API
  │                         │                         │                     │
  │                         │◄──AIResponse (JSON)────────────────────────────┤
  │                         │                         │                      │
  │                         ├──parseResponse─────────┐                      │
  │                         │                         │ (internal)           │
  │                         │◄──Recommendation───────┘                      │
  │                         │                         │                      │
  │◄──PlacementRecommendation─────────────────────────────────────────────  │
  │                         │                         │                      │
  └─ User approves & saves to k_entry ──────────────────────────────────────┘
```

## Class Hierarchy

```
IAIContextProvider (Interface)
    │
    ├── WindowContextProvider
    ├── ChartContextProvider
    └── KnowledgeBaseContextProvider ← NEW
        │
        └─ Loads KnowledgeBaseHierarchy
            │
            └─ Contains KnowledgeBaseEntry[]
                │
                └─ Uses EditorJsParser for content

IAIProvider (Interface)
    │
    ├── AnthropicProvider
    ├── AWSBedrockProvider
    └─ Used by KnowledgeBaseAgent

KnowledgeBaseAgent
    │
    ├── Uses IAIContextProvider
    ├── Uses KnowledgeBaseSimilarityAnalyzer
    ├── Uses EditorJsParser
    └── Returns PlacementRecommendation

PlacementRecommendation
    │
    └── PlacementRecommendation.Action (Enum)
        ├── CREATE_NEW
        ├── EXTEND_EXISTING
        ├── UPDATE_EXISTING
        ├── REPLACE_EXISTING
        └── DUPLICATE_WARNING

SimilarityResult
    │
    └── SimilarityResult.SimilarEntry[]
```

## Technology Stack

| Layer | Technology | Component |
|-------|-----------|-----------|
| **UI** | Chat Panel / Form | User interface |
| **Orchestration** | Java | KnowledgeBaseAgent |
| **Context** | iDempiere ORM | KnowledgeBaseContextProvider |
| **Analysis** | Keyword Matching | KnowledgeBaseSimilarityAnalyzer |
| **Parsing** | JSON Processing | EditorJsParser |
| **AI** | Claude 3.5 Sonnet | AnthropicProvider |
| **Format** | editor.js / Markdown | Content representation |
| **Database** | PostgreSQL/MySQL | k_entry, k_type tables |
| **Logging** | iDempiere CLogger | Activity tracking |

## Integration Points

### 1. Context Provider Registry
```
AIContextProviderRegistry
    ├── WINDOW → WindowContextProvider
    ├── CHART → ChartContextProvider
    └── KNOWLEDGE_BASE → KnowledgeBaseContextProvider ← NEW
```

### 2. AI Provider Factory
```
No changes needed to existing factory.
KnowledgeBaseAgent uses IAIProvider interface
which is already injectable/available.
```

### 3. Context Parameters
```
ContextParameters.forKnowledgeBase(kType)  ← NEW BUILDER METHOD
```

### 4. Database Access
```
Uses existing iDempiere DB class:
    DB.query(sql, params)
```

## Scalability Considerations

### Memory
- Hierarchy cached in memory per KB type
- For large KBs (>5000 entries), consider lazy-loading
- Content preview limited to 500 chars

### Performance
- Similarity analysis: O(n) where n = KB entries
- Keyword extraction: Optimized with stop-words
- AI calls: Cached where possible, rate-limited

### Concurrency
- Thread-safe registry (ConcurrentHashMap)
- AI calls may block briefly (typical 2-5 seconds)
- Multiple KBs can be analyzed independently

## Security

```
Input Validation
    ├── SQL injection protection (prepared statements)
    ├── Content size limits (for AI token limits)
    └── User role validation

Output Security
    ├── No sensitive fields in prompts
    ├── No API keys exposed
    └── Audit logging of all recommendations

Access Control
    ├── Respects iDempiere role permissions
    ├── KB access per org/client
    └── Recommendation logged with user context
```

## Future Architecture Enhancements

```
Phase 2: Vector Embeddings
├── Add Anthropic Embeddings API
├── Replace keyword matching with semantic similarity
└── Improve accuracy for fuzzy matches

Phase 3: Collaborative Features
├── User feedback loop
├── Learning from approved recommendations
└── KB quality metrics

Phase 4: Advanced Analytics
├── Track KB evolution
├── Identify gaps
├── Suggest new topics
└── Auto-tag content

Phase 5: Multi-Modal Support
├── Images/diagrams
├── Videos
└── Interactive demos
```

---

**Version:** 1.0
**Last Updated:** November 2024
