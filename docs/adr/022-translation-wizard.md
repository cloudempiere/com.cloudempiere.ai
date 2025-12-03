# ADR-022: Translation Wizard Use Case

**Status**: Proposed
**Date**: 2025-12-03
**Deciders**: CloudEmpiere AI Team
**Phase**: 2 (Post-MVP)
**Related**: [ADR-021](021-product-catalog-enhancement.md), [ADR-002](002-langchain4j-strategic-adoption.md), [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)

## Context

### Problem Statement

Multi-language support in iDempiere requires extensive manual translation:
- Application Dictionary elements (windows, tabs, fields) need translation
- Product descriptions need localization for each market
- Reports and documents need multi-language support
- Professional translation services are expensive and slow
- Context is often lost in translation (ERP-specific terminology)

### Business Value

| Metric | Current | Target | Impact |
|--------|---------|--------|--------|
| Translation time per element | 5-10 min | 30 sec | 95% reduction |
| Translation cost | €0.10/word | €0.01/word | 90% cost reduction |
| Context accuracy | Variable | 95%+ | Fewer corrections |
| Deployment to new market | 4-6 weeks | 1 week | 80% faster |

### iDempiere Translation Architecture

iDempiere stores translations in `_Trl` tables:
- `AD_Element_Trl` - Column/field labels
- `AD_Window_Trl` - Window names
- `AD_Tab_Trl` - Tab names
- `AD_Field_Trl` - Field labels
- `AD_Menu_Trl` - Menu items
- `AD_Message_Trl` - System messages
- `M_Product_Trl` - Product descriptions
- `C_DocType_Trl` - Document type names

## Decision

Implement a **Translation Wizard** that uses AI to translate Application Dictionary elements and business content with ERP context awareness.

### Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Translation Wizard UI                              │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │  Source Language: [EN ▼]    Target Language: [DE ▼]              ││
│  │  Content Type: [AD Elements ▼] / [Products ▼] / [Documents ▼]    ││
│  │  [Select All Untranslated] [Preview] [Translate]                 ││
│  └──────────────────────────────────────────────────────────────────┘│
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│               TranslationContextProvider                              │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Extracts:                                                       │  │
│  │   - Source text and context                                     │  │
│  │   - Element type (field, window, message, etc.)                │  │
│  │   - Related elements (parent window, sibling fields)           │  │
│  │   - Existing translations in other languages                    │  │
│  │   - ERP terminology glossary                                    │  │
│  │   - Character limits (for UI elements)                          │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    TranslationAgent                                   │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ LangChain4j AiServices:                                         │  │
│  │   - ChatLanguageModel (Claude Sonnet 4)                         │  │
│  │   - System prompt: ERP translation specialist                   │  │
│  │   - Glossary injection for terminology consistency              │  │
│  │   - Context from related translations                           │  │
│  │   - Length constraints for UI elements                          │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Translation Result                                 │
│  {                                                                    │
│    "source_text": "Sales Order",                                     │
│    "target_text": "Kundenauftrag",                                   │
│    "source_lang": "en_US",                                           │
│    "target_lang": "de_DE",                                           │
│    "confidence": 0.95,                                               │
│    "alternatives": ["Verkaufsauftrag", "Auftrag"],                   │
│    "context_used": "Window name in Sales module",                    │
│    "length_ok": true,                                                │
│    "glossary_terms_used": ["Order → Auftrag"]                        │
│  }                                                                    │
└──────────────────────────────────────────────────────────────────────┘
```

### Translation Workflow

```
┌─────────────────┐
│  Select Content │
│  to Translate   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Load Glossary   │ ──▶ ERP-specific terms
│ & Context       │     Industry terms
└────────┬────────┘     Previous translations
         │
         ▼
┌─────────────────┐
│ Batch Translate │ ──▶ Respects length limits
│ (AI Agent)      │     Uses context
└────────┬────────┘     Maintains consistency
         │
         ▼
┌─────────────────┐
│ Quality Check   │ ──▶ Confidence scoring
│                 │     Back-translation check
└────────┬────────┘     Length validation
         │
         ▼
┌─────────────────┐
│ Human Review    │ ──▶ Side-by-side preview
│ (Optional)      │     Accept/Edit/Reject
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Save to _Trl    │ ──▶ Update AD_*_Trl tables
│ Tables          │     Audit logging
└─────────────────┘
```

### System Prompt

```
You are an expert ERP software translator specializing in iDempiere/Compiere terminology.

CONTEXT:
- Source Language: {source_lang}
- Target Language: {target_lang}
- Element Type: {element_type} (Window/Tab/Field/Menu/Message/Product)
- Parent Context: {parent_context}
- Character Limit: {max_length} (for UI elements)
- Related Translations: {related_translations}

GLOSSARY (must use these translations):
{glossary_entries}

TASK: Translate the following ERP content while:
1. Maintaining ERP terminology consistency (use glossary)
2. Respecting character limits for UI elements
3. Preserving placeholders like {0}, {1}, %s
4. Keeping technical identifiers unchanged (Table_Name, Column_Name)
5. Using formal language appropriate for business software

INPUT TO TRANSLATE:
{source_text}

OUTPUT FORMAT (JSON only):
{
  "source_text": "Original text",
  "target_text": "Translated text",
  "confidence": 0.0-1.0,
  "alternatives": ["Alt 1", "Alt 2"],
  "context_used": "Explanation of context considered",
  "length_ok": true/false,
  "actual_length": 15,
  "max_length": 20,
  "glossary_terms_used": ["Term1 → Translation1"],
  "placeholders_preserved": true,
  "notes": "Any translation notes"
}

TRANSLATION RULES:

1. ERP Terminology:
   - "Business Partner" → Use glossary term (e.g., "Geschäftspartner" in German)
   - "Sales Order" → Use consistent term across all elements
   - "Invoice" → Distinguish between customer/vendor invoice if context available

2. UI Elements:
   - Keep translations concise for buttons and menu items
   - Field labels should be short but descriptive
   - Window titles can be more descriptive

3. Messages:
   - Preserve all placeholders {0}, {1}, %s, etc.
   - Keep error codes unchanged
   - Maintain formal tone

4. Length Constraints:
   - If translation exceeds limit, provide shortened version
   - Never truncate in middle of word
   - Prefer abbreviations over truncation

5. Special Cases:
   - Don't translate: Table names, Column names, Technical IDs
   - Keep: HTML tags, formatting codes
   - Preserve: Newlines, tabs in messages

LANGUAGE-SPECIFIC RULES:

German (de_DE):
- Use formal "Sie" not informal "du"
- Compound words are acceptable
- Use German quotation marks „..."

French (fr_FR):
- Use formal "vous" not informal "tu"
- Include proper accents
- Space before : ; ? !

Spanish (es_ES):
- Use formal "usted" for business
- Include ¿ and ¡ for questions/exclamations
- Distinguish Castilian vs Latin American if specified
```

### Agent Definition

```java
package com.cloudempiere.ai.agent.translation;

@Agent(
    name = "TranslationAgent",
    domain = "LOCALIZATION",
    riskLevel = RiskLevel.LOW
)
public interface TranslationAgent {

    @SystemMessage(fromResource = "prompts/translation.txt")
    TranslationResult translate(
        @UserMessage String translationContext
    );

    @SystemMessage(fromResource = "prompts/translation.txt")
    List<TranslationResult> translateBatch(
        @UserMessage String batchContext
    );

    @SystemMessage(fromResource = "prompts/translation-review.txt")
    TranslationReview reviewTranslation(
        @UserMessage String translationToReview
    );
}

public record TranslationResult(
    @Description("Original text") String source_text,
    @Description("Translated text") String target_text,
    @Description("Translation confidence") double confidence,
    @Description("Alternative translations") List<String> alternatives,
    @Description("Context used for translation") String context_used,
    @Description("Within length limit") boolean length_ok,
    @Description("Actual character count") int actual_length,
    @Description("Maximum allowed length") int max_length,
    @Description("Glossary terms applied") List<String> glossary_terms_used,
    @Description("Placeholders preserved") boolean placeholders_preserved,
    @Description("Translator notes") String notes
) {}

public record TranslationReview(
    @Description("Is translation acceptable") boolean acceptable,
    @Description("Quality score") double quality_score,
    @Description("Issues found") List<String> issues,
    @Description("Suggested improvements") List<String> suggestions
) {}
```

### Boundaries (per ADR-009)

```yaml
Agent: TranslationAgent
Domain: LOCALIZATION
Risk Level: LOW

Data Access:
  Read Tables:
    - AD_Element, AD_Element_Trl
    - AD_Window, AD_Window_Trl
    - AD_Tab, AD_Tab_Trl
    - AD_Field, AD_Field_Trl
    - AD_Menu, AD_Menu_Trl
    - AD_Message, AD_Message_Trl
    - M_Product, M_Product_Trl
    - C_DocType, C_DocType_Trl
    - AD_Language (available languages)

  Write Tables:
    - All _Trl tables (translation content only)
    - AIG_TranslationGlossary (glossary management)
    - AIG_TranslationAudit (audit log)

  Forbidden Tables:
    - AD_User (credentials)
    - Financial tables
    - Business data

Organizational:
  - Respects AD_Role access
  - System-level translations require System role
  - Client-specific content respects AD_Client_ID

Token Limits:
  Max Tokens/Request: 8,000
  Max Elements/Batch: 50

Cost:
  Daily Budget: $75
  Cost per Translation: ~$0.02 (estimated)

Performance:
  Response Time: <2 seconds (single)
  Batch Throughput: 50 elements/minute
```

### Glossary Management

```java
public class TranslationGlossary {

    /**
     * Core ERP glossary entries - must be translated consistently
     */
    public static final Map<String, Map<String, String>> CORE_GLOSSARY = Map.of(
        "en_US", Map.of(
            "Business Partner", "Business Partner",
            "Sales Order", "Sales Order",
            "Purchase Order", "Purchase Order",
            "Invoice", "Invoice",
            "Payment", "Payment",
            "Product", "Product",
            "Warehouse", "Warehouse",
            "Organization", "Organization"
        )
    );

    /**
     * Load glossary for language pair
     */
    public Map<String, String> loadGlossary(String sourceLang, String targetLang) {
        // Load from AIG_TranslationGlossary table
        return new Query(ctx, "AIG_TranslationGlossary",
            "SourceLang=? AND TargetLang=? AND IsActive='Y'", null)
            .setParameters(sourceLang, targetLang)
            .stream()
            .collect(toMap(
                g -> g.getSourceTerm(),
                g -> g.getTargetTerm()
            ));
    }

    /**
     * Add new glossary entry (from approved translations)
     */
    public void addToGlossary(String sourceLang, String targetLang,
                              String sourceTerm, String targetTerm) {
        // Check if already exists
        // Add to AIG_TranslationGlossary
    }

    /**
     * Format glossary for prompt injection
     */
    public String formatForPrompt(Map<String, String> glossary) {
        return glossary.entrySet().stream()
            .map(e -> "- \"" + e.getKey() + "\" → \"" + e.getValue() + "\"")
            .collect(joining("\n"));
    }
}
```

### Translation Service

```java
public class TranslationService {

    private final TranslationAgent agent;
    private final TranslationGlossary glossary;
    private final TranslationContextProvider contextProvider;

    /**
     * Translate AD Element
     */
    public TranslationResult translateElement(int elementId,
                                              String sourceLang,
                                              String targetLang) {
        MElement element = new MElement(ctx, elementId, null);

        // Build context
        TranslationContext context = contextProvider.buildContext(
            element,
            sourceLang,
            targetLang,
            glossary.loadGlossary(sourceLang, targetLang)
        );

        // Translate
        return agent.translate(context.toJson());
    }

    /**
     * Translate all untranslated elements for a language
     */
    public BatchResult translateAllUntranslated(String targetLang) {
        // Find untranslated elements
        List<MElement> untranslated = findUntranslatedElements(targetLang);

        // Process in batches
        return processBatch(untranslated, "en_US", targetLang);
    }

    /**
     * Translate product descriptions
     */
    public TranslationResult translateProduct(int productId,
                                              String sourceLang,
                                              String targetLang) {
        MProduct product = new MProduct(ctx, productId, null);

        TranslationContext context = new TranslationContext()
            .setElementType("Product")
            .setSourceText(product.getDescription())
            .setSourceLang(sourceLang)
            .setTargetLang(targetLang)
            .setMaxLength(2000)  // Product descriptions can be longer
            .setGlossary(glossary.loadGlossary(sourceLang, targetLang))
            .setRelatedTranslations(getRelatedProductTranslations(product, targetLang));

        return agent.translate(context.toJson());
    }

    /**
     * Find elements without translation
     */
    private List<MElement> findUntranslatedElements(String targetLang) {
        return DB.executeQuery("""
            SELECT e.AD_Element_ID
            FROM AD_Element e
            LEFT JOIN AD_Element_Trl t ON e.AD_Element_ID = t.AD_Element_ID
                AND t.AD_Language = ?
            WHERE e.IsActive = 'Y'
              AND (t.AD_Element_ID IS NULL OR t.IsTranslated = 'N')
            """,
            targetLang);
    }
}
```

### Back-Translation Quality Check

```java
public class TranslationQualityChecker {

    private final TranslationAgent agent;

    /**
     * Verify translation quality via back-translation
     */
    public QualityCheckResult checkQuality(TranslationResult translation) {
        // Back-translate to original language
        TranslationResult backTranslation = agent.translate(
            new TranslationContext()
                .setSourceText(translation.target_text())
                .setSourceLang(translation.target_lang())
                .setTargetLang(translation.source_lang())
                .toJson()
        );

        // Compare with original
        double similarity = calculateSimilarity(
            translation.source_text(),
            backTranslation.target_text()
        );

        // Score quality
        boolean acceptable = similarity > 0.85 &&
                            translation.confidence() > 0.80 &&
                            translation.length_ok() &&
                            translation.placeholders_preserved();

        return new QualityCheckResult(
            acceptable,
            similarity,
            translation.confidence(),
            identifyIssues(translation, backTranslation)
        );
    }

    private double calculateSimilarity(String original, String backTranslated) {
        // Levenshtein distance or semantic similarity
        // Return 0-1 score
    }
}
```

## Implementation Plan

### Phase 1: Core Translation (Days 1-4)

| Task | Effort | Owner |
|------|--------|-------|
| Create `TranslationAgent` interface | 4h | Dev |
| Create system prompt with glossary support | 6h | Dev |
| Create `TranslationContextProvider` | 6h | Dev |
| Create `TranslationGlossary` | 4h | Dev |
| Core glossary entries (EN→DE, FR, ES) | 8h | Dev |

### Phase 2: AD Translation (Days 5-8)

| Task | Effort | Owner |
|------|--------|-------|
| AD_Element translation | 4h | Dev |
| AD_Window/Tab/Field translation | 6h | Dev |
| AD_Menu translation | 2h | Dev |
| AD_Message translation | 4h | Dev |
| Batch processing | 6h | Dev |

### Phase 3: Content Translation (Days 9-11)

| Task | Effort | Owner |
|------|--------|-------|
| M_Product_Trl translation | 4h | Dev |
| C_DocType_Trl translation | 2h | Dev |
| Quality checker (back-translation) | 6h | Dev |
| Glossary management UI | 6h | Dev |

### Phase 4: UI & Testing (Days 12-15)

| Task | Effort | Owner |
|------|--------|-------|
| Translation Wizard UI | 8h | Dev |
| Preview and review workflow | 6h | Dev |
| Testing with real AD elements | 8h | QA |
| Native speaker review (DE, FR, ES) | 8h | External |

### Deliverables

- `TranslationAgent.java` (~100 lines)
- `TranslationResult.java` + DTOs (~80 lines)
- `TranslationService.java` (~300 lines)
- `TranslationContextProvider.java` (~200 lines)
- `TranslationGlossary.java` (~150 lines)
- `TranslationQualityChecker.java` (~100 lines)
- `translation.txt` (system prompt)
- Translation Wizard ZK window
- Glossary management window
- Core glossary data (EN→DE, FR, ES)

**Total Effort**: 15 days
**Risk Level**: Medium (language quality subjective, native review needed)

## Acceptance Criteria

```yaml
Functional:
  - [ ] Translate AD elements (Element, Window, Tab, Field, Menu, Message)
  - [ ] Translate product descriptions
  - [ ] Glossary-driven consistency
  - [ ] Batch translation with progress
  - [ ] Preview before save
  - [ ] Back-translation quality check

Non-Functional:
  - [ ] Response time: <2 seconds per element
  - [ ] Batch throughput: 50 elements/minute
  - [ ] Translation accuracy: 95%+ (native review)
  - [ ] Glossary consistency: 100%

Languages (Initial):
  - [ ] German (de_DE)
  - [ ] French (fr_FR)
  - [ ] Spanish (es_ES)
  - [ ] (More languages configurable)
```

## Consequences

### Positive

1. **Speed**: 95% faster than manual translation
2. **Cost**: 90% cheaper than professional services
3. **Consistency**: Glossary enforces terminology
4. **Context**: ERP-aware translations
5. **Scale**: Translate entire AD quickly

### Negative

1. **Quality Variance**: Some languages better than others
2. **Review Needed**: Native speaker verification recommended
3. **Glossary Maintenance**: Needs ongoing curation
4. **Cultural Nuance**: May miss regional variations

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Poor translation quality | HIGH | Back-translation check, native review |
| Inconsistent terminology | MEDIUM | Strict glossary enforcement |
| Length overflow | LOW | Constraint checking, alternatives |
| Placeholder corruption | MEDIUM | Validation before save |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Translation accuracy | 95%+ | Native speaker review |
| Glossary consistency | 100% | Automated check |
| Time to translate AD | <4 hours | Process timing |
| Cost per element | <€0.02 | API billing |
| Back-translation similarity | >85% | Quality checker |

## References

- [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [iDempiere Language Pack Guide](https://wiki.idempiere.org/)
- [ADR-021: Product Catalog Enhancement](021-product-catalog-enhancement.md)

---

*ADR-022 | Version 1.0 | 2025-12-03*
