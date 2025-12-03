# ADR-021: Product Catalog Enhancement Use Case

**Status**: Proposed
**Date**: 2025-12-03
**Deciders**: Cloudempiere AI Team
**Phase**: 2 (Post-MVP)
**Related**: [ADR-002](002-langchain4j-strategic-adoption.md), [ADR-009](009-domain-boundaries-agent-scope.md), [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)

## Context

### Problem Statement

Product catalog management is time-consuming and inconsistent:
- Product descriptions require manual writing for 500+ products
- No standardized quality across catalog
- E-commerce catalog updates take weeks
- SEO optimization is manual and inconsistent
- Missing or poor descriptions hurt conversion rates (15-20% lower)

### Business Value

| Metric | Current | Target | Impact |
|--------|---------|--------|--------|
| Description writing time | 30-60 min/product | 5 min/product | 90% reduction |
| Catalog update cycle | 2-4 weeks | 1-2 days | 90% faster |
| Description quality | Variable | Standardized | Brand consistency |
| E-commerce conversion | Baseline | +15-20% | Revenue impact |

## Decision

Implement a **Product Catalog Enhancement** feature that uses AI to generate, improve, and standardize product descriptions with SEO optimization.

### Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                    M_Product Window                                   │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │  [AI Enhance] Button                                             ││
│  │     → Generate/improve description                                ││
│  │     → SEO optimization                                            ││
│  │     → Bulk processing option                                      ││
│  └──────────────────────────────────────────────────────────────────┘│
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│               ProductContextProvider                                  │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Extracts:                                                       │  │
│  │   - Product master data (name, SKU, category, UOM)             │  │
│  │   - Current description (if any)                                │  │
│  │   - Product attributes (size, color, material, etc.)           │  │
│  │   - Category hierarchy and siblings                             │  │
│  │   - Pricing tier (budget/mid/premium)                          │  │
│  │   - Related products (substitutes, complements)                 │  │
│  │   - Sales history summary (best sellers, seasonal)             │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                  ProductCatalogAgent                                  │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ LangChain4j AiServices:                                         │  │
│  │   - ChatLanguageModel (Claude Sonnet 4)                         │  │
│  │   - System prompt: E-commerce copywriting expert                │  │
│  │   - ERPTools: queryDatabase, getProduct, searchRecords          │  │
│  │   - Brand voice guidelines injection                            │  │
│  │   - SEO keyword integration                                     │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Enhanced Product Result                            │
│  {                                                                    │
│    "short_description": "Concise 1-2 sentence summary",              │
│    "long_description": "Full marketing description (150-300 words)", │
│    "bullet_points": ["Feature 1", "Feature 2", "Feature 3"],         │
│    "seo_title": "Optimized page title (60 chars)",                   │
│    "meta_description": "SEO meta description (160 chars)",           │
│    "keywords": ["keyword1", "keyword2", "keyword3"],                 │
│    "suggested_categories": ["Category1", "Category2"],               │
│    "quality_score": 0.92,                                            │
│    "improvements_made": ["Added benefits", "SEO optimized"]          │
│  }                                                                    │
└──────────────────────────────────────────────────────────────────────┘
```

### Processing Modes

```
┌─────────────────────────────────────────────────────────────────┐
│                    PROCESSING MODES                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. GENERATE (No existing description)                          │
│     Input: Product attributes only                               │
│     Output: Complete new description                             │
│                                                                  │
│  2. ENHANCE (Has basic description)                             │
│     Input: Existing + attributes                                 │
│     Output: Improved version                                     │
│                                                                  │
│  3. SEO_OPTIMIZE (Good description, needs SEO)                  │
│     Input: Description + target keywords                         │
│     Output: SEO-enhanced version                                 │
│                                                                  │
│  4. STANDARDIZE (Inconsistent format)                           │
│     Input: Description + brand guidelines                        │
│     Output: Brand-consistent version                             │
│                                                                  │
│  5. BULK (Multiple products)                                    │
│     Input: Product list + mode                                   │
│     Output: Batch results with progress                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### System Prompt

```
You are an expert e-commerce copywriter specializing in product descriptions.

CONTEXT:
- Product Name: {product_name}
- SKU: {sku}
- Category: {category_path}
- Attributes: {attributes_json}
- Current Description: {current_description}
- Price Tier: {price_tier}
- Target Audience: {target_audience}
- Brand Voice: {brand_guidelines}
- SEO Keywords: {target_keywords}

TASK: {mode} - Generate/Enhance/Optimize product description

OUTPUT FORMAT (JSON only):
{
  "short_description": "Concise 1-2 sentence hook (max 200 chars)",
  "long_description": "Full marketing description (150-300 words)",
  "bullet_points": [
    "Key feature or benefit 1",
    "Key feature or benefit 2",
    "Key feature or benefit 3",
    "Key feature or benefit 4",
    "Key feature or benefit 5"
  ],
  "seo_title": "Page title with primary keyword (max 60 chars)",
  "meta_description": "SEO meta description (max 160 chars)",
  "keywords": ["primary", "secondary", "tertiary"],
  "suggested_tags": ["tag1", "tag2", "tag3"],
  "quality_score": 0.0-1.0,
  "improvements_made": ["Improvement 1", "Improvement 2"]
}

WRITING GUIDELINES:

Short Description:
- Lead with primary benefit
- Include main keyword naturally
- Create urgency or curiosity
- Max 200 characters

Long Description:
- Opening hook (problem/solution or benefit)
- Features translated to benefits
- Use sensory language where appropriate
- Include social proof elements if available
- Call to action at end
- 150-300 words optimal

Bullet Points:
- Start each with action verb or benefit
- Be specific (numbers, measurements)
- Address customer pain points
- Highlight unique selling points
- 5 bullets maximum

SEO Guidelines:
- Primary keyword in title, first paragraph
- Secondary keywords distributed naturally
- Avoid keyword stuffing
- Use semantic variations
- Meta description includes CTA

Brand Voice ({brand_voice}):
- Tone: {tone}
- Vocabulary level: {vocabulary}
- Personality: {personality}

AVOID:
- Generic superlatives ("best", "amazing")
- Unsubstantiated claims
- Competitor mentions
- Technical jargon (unless B2B)
- Passive voice
- Duplicate content from similar products
```

### Agent Definition

```java
package com.cloudempiere.ai.agent.catalog;

@Agent(
    name = "ProductCatalogAgent",
    domain = "PRODUCT",
    riskLevel = RiskLevel.LOW
)
public interface ProductCatalogAgent {

    @SystemMessage(fromResource = "prompts/product-catalog.txt")
    ProductDescription generate(
        @MemoryId String sessionId,
        @UserMessage String productContext
    );

    @SystemMessage(fromResource = "prompts/product-catalog.txt")
    ProductDescription enhance(
        @MemoryId String sessionId,
        @UserMessage String productWithExisting
    );

    @SystemMessage(fromResource = "prompts/product-catalog.txt")
    List<ProductDescription> bulkGenerate(
        @UserMessage String batchProductContext
    );
}

public record ProductDescription(
    @Description("Short marketing hook") String short_description,
    @Description("Full marketing description") String long_description,
    @Description("Key feature bullets") List<String> bullet_points,
    @Description("SEO page title") String seo_title,
    @Description("SEO meta description") String meta_description,
    @Description("Target keywords") List<String> keywords,
    @Description("Suggested product tags") List<String> suggested_tags,
    @Description("Quality score 0-1") double quality_score,
    @Description("Improvements made") List<String> improvements_made
) {}

public enum CatalogMode {
    GENERATE,      // Create from scratch
    ENHANCE,       // Improve existing
    SEO_OPTIMIZE,  // Add SEO elements
    STANDARDIZE,   // Apply brand voice
    BULK           // Process multiple
}
```

### Boundaries (per ADR-009)

```yaml
Agent: ProductCatalogAgent
Domain: PRODUCT
Risk Level: LOW

Data Access:
  Read Tables:
    - M_Product (product master)
    - M_Product_Category (categories)
    - M_AttributeSet (attributes)
    - M_AttributeSetInstance (attribute values)
    - M_Product_Acct (for pricing tier inference)
    - M_Substitute (related products)
    - C_OrderLine (sales history - aggregated only)

  Write Tables:
    - M_Product (description fields only)
      - Description
      - DocumentNote
      - DescriptionURL
      - Help (extended description)

  Forbidden Tables:
    - M_ProductPrice (pricing)
    - M_Cost (cost data)
    - C_BPartner (customer/vendor)
    - Competitor information

Organizational:
  - Respects AD_Role access
  - Products filtered by AD_Org_ID
  - Cannot see competitor pricing

Token Limits:
  Max Tokens/Request: 8,000
  Max Products/Batch: 20

Cost:
  Daily Budget: $100
  Cost per Description: ~$0.08 (estimated)

Performance:
  Response Time: <5 seconds (single)
  Batch Throughput: 20 products/minute
```

### Bulk Processing

```java
public class ProductCatalogBatchProcessor {

    private final ProductCatalogAgent agent;
    private final ProductContextProvider contextProvider;

    /**
     * Process multiple products in batch
     */
    public BatchResult processBatch(List<Integer> productIds, CatalogMode mode) {
        BatchResult result = new BatchResult();

        // Process in chunks of 20
        List<List<Integer>> chunks = partition(productIds, 20);

        for (List<Integer> chunk : chunks) {
            // Build batch context
            String batchContext = buildBatchContext(chunk, mode);

            // Call agent
            List<ProductDescription> descriptions = agent.bulkGenerate(batchContext);

            // Save results
            for (int i = 0; i < chunk.size(); i++) {
                try {
                    saveProductDescription(chunk.get(i), descriptions.get(i));
                    result.addSuccess(chunk.get(i));
                } catch (Exception e) {
                    result.addFailure(chunk.get(i), e.getMessage());
                }
            }

            // Progress callback
            result.updateProgress(chunk.size());
        }

        return result;
    }

    /**
     * Find products needing enhancement
     */
    public List<MProduct> findProductsNeedingEnhancement() {
        return new Query(ctx, MProduct.Table_Name,
            "IsActive='Y' AND IsSold='Y' AND " +
            "(Description IS NULL OR LENGTH(Description) < 50)", null)
            .setOrderBy("M_Product_Category_ID, Name")
            .list();
    }
}
```

### Brand Voice Configuration

```java
public class BrandVoiceConfig {

    @Column(name = "BrandName")
    private String brandName;

    @Column(name = "Tone")
    private String tone;  // Professional, Friendly, Luxurious, Technical, Casual

    @Column(name = "VocabularyLevel")
    private String vocabularyLevel;  // Simple, Moderate, Sophisticated

    @Column(name = "Personality")
    private String personality;  // Authoritative, Helpful, Innovative, Traditional

    @Column(name = "AvoidWords")
    private String avoidWords;  // Comma-separated words to avoid

    @Column(name = "PreferredPhrases")
    private String preferredPhrases;  // Brand-specific phrases

    @Column(name = "TargetAudience")
    private String targetAudience;  // B2B, B2C Consumer, B2C Premium

    public String toPromptSection() {
        return String.format("""
            Brand: %s
            Tone: %s
            Vocabulary: %s
            Personality: %s
            Avoid: %s
            Preferred phrases: %s
            Target: %s
            """,
            brandName, tone, vocabularyLevel, personality,
            avoidWords, preferredPhrases, targetAudience);
    }
}
```

### Quality Scoring

```java
public class DescriptionQualityScorer {

    /**
     * Score description quality (0-1)
     */
    public double score(ProductDescription desc) {
        double score = 0.0;

        // Length checks
        if (desc.short_description().length() >= 50 &&
            desc.short_description().length() <= 200) {
            score += 0.15;
        }

        if (desc.long_description().length() >= 150 &&
            desc.long_description().length() <= 300) {
            score += 0.15;
        }

        // Bullet points
        if (desc.bullet_points().size() >= 3 &&
            desc.bullet_points().size() <= 5) {
            score += 0.15;
        }

        // SEO elements present
        if (desc.seo_title() != null && desc.seo_title().length() <= 60) {
            score += 0.1;
        }

        if (desc.meta_description() != null &&
            desc.meta_description().length() <= 160) {
            score += 0.1;
        }

        // Keywords present
        if (desc.keywords() != null && desc.keywords().size() >= 3) {
            score += 0.1;
        }

        // Keyword usage in description
        if (containsKeywords(desc.long_description(), desc.keywords())) {
            score += 0.15;
        }

        // No banned words
        if (!containsBannedWords(desc.long_description())) {
            score += 0.1;
        }

        return Math.min(score, 1.0);
    }

    private static final String[] BANNED_WORDS = {
        "best in class", "world-class", "revolutionary",
        "game-changing", "synergy", "leverage"
    };
}
```

## Implementation Plan

### Phase 1: Core Generation (Days 1-3)

| Task | Effort | Owner |
|------|--------|-------|
| Create `ProductContextProvider` | 6h | Dev |
| Create `ProductCatalogAgent` interface | 4h | Dev |
| Create system prompt with brand voice | 6h | Dev |
| Create `ProductDescription` DTOs | 2h | Dev |
| Unit tests for context extraction | 4h | Dev |

### Phase 2: Enhancement Features (Days 4-6)

| Task | Effort | Owner |
|------|--------|-------|
| Create `BrandVoiceConfig` model | 4h | Dev |
| Create `DescriptionQualityScorer` | 4h | Dev |
| Implement enhancement modes | 6h | Dev |
| Create bulk processor | 6h | Dev |

### Phase 3: UI & Integration (Days 7-9)

| Task | Effort | Owner |
|------|--------|-------|
| Add "AI Enhance" button to M_Product window | 4h | Dev |
| Create description preview/edit dialog | 6h | Dev |
| Create bulk processing UI | 6h | Dev |
| Admin configuration for brand voice | 4h | Dev |

### Phase 4: Testing (Days 10-12)

| Task | Effort | Owner |
|------|--------|-------|
| Test with 100 real products | 8h | QA |
| Quality assessment vs manual | 6h | QA |
| E-commerce team review | 4h | PM |
| Prompt refinement | 6h | Dev |

### Deliverables

- `ProductCatalogAgent.java` (~120 lines)
- `ProductDescription.java` + DTOs (~100 lines)
- `ProductContextProvider.java` (~250 lines)
- `ProductCatalogBatchProcessor.java` (~200 lines)
- `BrandVoiceConfig.java` (~100 lines)
- `DescriptionQualityScorer.java` (~150 lines)
- `product-catalog.txt` (system prompt)
- ZK UI components
- Admin configuration window
- Unit and integration tests

**Total Effort**: 12 days
**Risk Level**: Medium (quality subjective, brand consistency)

## Acceptance Criteria

```yaml
Functional:
  - [ ] Generate descriptions for products without any
  - [ ] Enhance existing descriptions
  - [ ] SEO optimization with target keywords
  - [ ] Bulk processing for 500+ products
  - [ ] Brand voice consistency
  - [ ] Preview before save

Non-Functional:
  - [ ] Response time: <5 seconds (single product)
  - [ ] Batch throughput: 20 products/minute
  - [ ] Quality score: 0.8+ average
  - [ ] Cost per description: <$0.10

Quality:
  - [ ] Human review: 90%+ acceptable without edit
  - [ ] SEO improvement: measurable keyword ranking
  - [ ] No duplicate content across products
```

## Consequences

### Positive

1. **Scale**: Process 500+ products quickly
2. **Consistency**: Standardized brand voice
3. **SEO**: Built-in optimization
4. **Time Savings**: 90% reduction in writing time
5. **Quality Floor**: Minimum quality guaranteed

### Negative

1. **Generic Risk**: AI descriptions may lack uniqueness
2. **Brand Fit**: May need significant prompt tuning
3. **Review Needed**: Still requires human approval
4. **Cost at Scale**: Bulk processing adds up

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Generic descriptions | MEDIUM | Category-specific prompts |
| Duplicate content | MEDIUM | Similarity detection |
| SEO over-optimization | LOW | Natural language guidelines |
| Brand voice mismatch | MEDIUM | Extensive prompt tuning |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Description quality | 0.8+ score | Quality scorer |
| Human acceptance | 90%+ | Review tracking |
| Time savings | 90% reduction | Before/after |
| E-commerce conversion | +10% | Analytics |
| SEO ranking | Improved | Search console |

## References

- [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [ADR-009: Domain Boundaries](009-domain-boundaries-agent-scope.md)

---

*ADR-021 | Version 1.0 | 2025-12-03*
