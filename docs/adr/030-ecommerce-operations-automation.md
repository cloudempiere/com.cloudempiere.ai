# ADR-030: E-Commerce Operations Automation

**Status**: Proposed
**Date**: 2025-12-03
**Deciders**: Cloudempiere AI Team
**Phase**: 2-3 (Post-MVP)
**Related**: [ADR-021](021-product-catalog-enhancement.md) (Product Descriptions)

---

## Context

### Problem Statement

E-commerce operations are labor-intensive and error-prone:

| Task | Current Pain | Time Impact |
|------|--------------|-------------|
| **Product Import** | Manual CSV mapping, data cleanup | 2-4 hours/import |
| **Image Processing** | Manual background removal, resizing | 5-10 min/image |
| **Attribute Assignment** | Manual categorization, tagging | 3-5 min/product |
| **Description Writing** | Manual copywriting | 30-60 min/product |
| **Quality Control** | Manual review of each item | 2-3 min/product |

**Scale Problem**: Adding 500 new products takes 2-4 weeks with current manual processes.

### Industry Best Practices (2025)

Research from leading e-commerce AI providers shows:

| Practice | Industry Adoption | Impact |
|----------|-------------------|--------|
| AI-powered catalog management | 60%+ by end 2025 | 70% reduction in manual time |
| Agentic AI for PIM | 25% enterprises 2025 → 50% by 2027 | Autonomous updates |
| AI image enhancement | Standard practice | 15-20% conversion lift |
| Auto-categorization | Growing rapidly | 90%+ accuracy achievable |
| Zero-shot classification | Emerging (LLMs) | No training data needed |

**Sources:**
- [Mirakl: AI-powered catalog management](https://www.mirakl.com/blog/how-ai-powered-catalog-management-accelerates-growth)
- [AI Journal: PIM Trends 2025](https://aijourn.com/5-product-information-management-trends-driving-ecommerce-in-2025/)
- [Salsify: AI in PIM](https://www.salsify.com/blog/ai-in-pim)
- [Claid.ai: AI Product Photography](https://claid.ai/blog/article/ai-product-photo-tools/)

---

## Decision

Implement **E-Commerce Operations Agent** with five automation modules:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    E-COMMERCE OPERATIONS AGENT                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   IMPORT    │  │   IMAGE     │  │  ATTRIBUTE  │  │ DESCRIPTION │        │
│  │   MODULE    │  │   MODULE    │  │   MODULE    │  │   MODULE    │        │
│  │             │  │             │  │             │  │             │        │
│  │ CSV/Excel   │  │ Background  │  │ Auto-tag    │  │ AI Generate │        │
│  │ AI Mapping  │  │ Removal     │  │ Categorize  │  │ SEO Optimize│        │
│  │ Enrichment  │  │ Enhancement │  │ Classify    │  │ Brand Voice │        │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘        │
│         │                │                │                │                │
│         └────────────────┴────────────────┴────────────────┘                │
│                                    │                                         │
│                                    ▼                                         │
│                        ┌─────────────────────┐                              │
│                        │   QUALITY MODULE    │                              │
│                        │                     │                              │
│                        │  Validation         │                              │
│                        │  Scoring            │                              │
│                        │  Human Review Queue │                              │
│                        └─────────────────────┘                              │
│                                    │                                         │
│                                    ▼                                         │
│                             M_Product                                        │
│                             + Images                                         │
│                             + Attributes                                     │
│                             + Categories                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Module 1: Import Automation

### Problem

Supplier CSV/Excel files are messy:
- Different column names ("Product Name" vs "Item" vs "Description")
- Missing data (no categories, incomplete specs)
- Inconsistent formats (dates, prices, units)
- No attribute mapping

### Solution: AI-Powered Import Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│  Supplier File (CSV/Excel)                                       │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Item,Desc,Cost,Cat,Img                                      ││
│  │ "Widget A","Blue widget",12.50,"Tools",widget.jpg           ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  1. AI COLUMN MAPPING                                            │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ LLM analyzes headers + sample data                          ││
│  │                                                             ││
│  │ "Item" → M_Product.Name (confidence: 0.95)                  ││
│  │ "Desc" → M_Product.Description (confidence: 0.90)           ││
│  │ "Cost" → M_ProductPO.PriceList (confidence: 0.85)           ││
│  │ "Cat"  → M_Product_Category.Name (confidence: 0.80)         ││
│  │ "Img"  → AD_Attachment (confidence: 0.92)                   ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. DATA ENRICHMENT                                              │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ For each row, AI enriches missing data:                     ││
│  │                                                             ││
│  │ - Infer category from name/description                      ││
│  │ - Extract attributes from description text                  ││
│  │ - Standardize units (kg, lb → UOM)                         ││
│  │ - Validate/format SKU patterns                              ││
│  │ - Flag duplicates (fuzzy match existing products)           ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. VALIDATION & PREVIEW                                         │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Show mapping preview with confidence scores                 ││
│  │ Highlight issues (missing required fields, duplicates)      ││
│  │ Allow manual override before import                         ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                        Import to M_Product
```

### Import Agent Interface

```java
@Agent(name = "ImportAgent", domain = "PRODUCT")
public interface ProductImportAgent {

    /**
     * Analyze file and suggest column mappings
     */
    @SystemMessage(fromResource = "prompts/import-mapping.txt")
    ImportMapping analyzeFile(@UserMessage String filePreview);

    /**
     * Enrich a single row with missing data
     */
    @SystemMessage(fromResource = "prompts/import-enrich.txt")
    EnrichedProduct enrichRow(@UserMessage String rowData);

    /**
     * Detect duplicates against existing catalog
     */
    @SystemMessage(fromResource = "prompts/import-dedupe.txt")
    DuplicateCheck checkDuplicate(@UserMessage String productContext);
}

public record ImportMapping(
    Map<String, ColumnMapping> mappings,
    List<String> unmappedColumns,
    List<String> missingRequiredFields,
    double overallConfidence
) {}

public record ColumnMapping(
    String sourceColumn,
    String targetField,
    String targetTable,
    double confidence,
    String transformation  // e.g., "UPPERCASE", "DATE_PARSE", "UOM_CONVERT"
) {}

public record EnrichedProduct(
    String inferredCategory,
    List<String> extractedAttributes,
    String standardizedUOM,
    String cleanedSKU,
    List<String> suggestedTags,
    double enrichmentConfidence
) {}
```

---

## Module 2: Image Processing

### Problem

Product images need processing:
- Background removal for clean product shots
- Consistent sizing and format
- Enhancement (lighting, color correction)
- Multiple variants (thumbnail, zoom, lifestyle)

### Solution: AI Image Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│  Raw Product Image                                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  1. BACKGROUND REMOVAL (Vision AI)                               │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ - Remove background → transparent PNG                       ││
│  │ - Or replace with white/lifestyle background                ││
│  │ - Preserve shadows optionally                               ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. IMAGE ENHANCEMENT                                            │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ - Auto color correction                                     ││
│  │ - Lighting adjustment                                       ││
│  │ - Upscaling (AI super-resolution)                          ││
│  │ - Noise reduction                                           ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. VARIANT GENERATION                                           │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Generate multiple sizes:                                    ││
│  │ - Thumbnail (150x150)                                       ││
│  │ - Product listing (400x400)                                 ││
│  │ - Product detail (800x800)                                  ││
│  │ - Zoom (1600x1600)                                          ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. ATTRIBUTE EXTRACTION (Vision LLM)                            │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Analyze image to extract:                                   ││
│  │ - Color (primary, secondary)                                ││
│  │ - Material (metal, wood, fabric)                            ││
│  │ - Style (modern, classic, industrial)                       ││
│  │ - Condition (new, vintage)                                  ││
│  │ - Generate ALT text for SEO                                 ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    AD_Attachment + M_Product attributes
```

### Image Processing Tools

```java
public interface ImageProcessingTools {

    @Tool("Remove background from product image")
    ProcessedImage removeBackground(
        @P("Image URL or base64") String image,
        @P("Background type: TRANSPARENT, WHITE, GRADIENT") String backgroundType
    );

    @Tool("Enhance product image quality")
    ProcessedImage enhanceImage(
        @P("Image URL or base64") String image,
        @P("Enhancement options") EnhancementOptions options
    );

    @Tool("Extract product attributes from image")
    ImageAttributes analyzeImage(
        @P("Image URL or base64") String image
    );

    @Tool("Generate image size variants")
    List<ImageVariant> generateVariants(
        @P("Source image") String image,
        @P("Variant sizes") List<ImageSize> sizes
    );
}

public record ImageAttributes(
    String primaryColor,
    String secondaryColor,
    String material,
    String style,
    String condition,
    String altText,
    List<String> detectedObjects,
    double confidence
) {}

public record EnhancementOptions(
    boolean autoColor,
    boolean autoLighting,
    boolean upscale,
    int targetWidth,
    int targetHeight,
    String format  // JPEG, PNG, WEBP
) {}
```

### External Service Integration

```java
public class ImageProcessingService {

    // Option 1: Claid.ai API
    private final ClaidClient claidClient;

    // Option 2: Photoroom API
    private final PhotoroomClient photoroomClient;

    // Option 3: Self-hosted (rembg + Real-ESRGAN)
    private final LocalImageProcessor localProcessor;

    // Option 4: Claude Vision for attribute extraction
    private final ChatLanguageModel visionModel;

    public ProcessedImage processProductImage(byte[] image, ProcessingOptions options) {
        // 1. Background removal
        byte[] noBackground = claidClient.removeBackground(image);

        // 2. Enhancement
        byte[] enhanced = claidClient.enhance(noBackground, options);

        // 3. Attribute extraction via Claude Vision
        ImageAttributes attrs = extractAttributes(enhanced);

        // 4. Generate variants
        List<ImageVariant> variants = generateSizeVariants(enhanced, options.sizes());

        return new ProcessedImage(enhanced, variants, attrs);
    }

    private ImageAttributes extractAttributes(byte[] image) {
        String base64 = Base64.getEncoder().encodeToString(image);

        String response = visionModel.generate(
            UserMessage.from(
                ImageContent.from(base64, "image/png"),
                TextContent.from("""
                    Analyze this product image and extract:
                    - Primary color
                    - Secondary color (if any)
                    - Material
                    - Style
                    - Suggested ALT text for SEO

                    Return as JSON.
                    """)
            )
        ).content().text();

        return parseImageAttributes(response);
    }
}
```

---

## Module 3: Attribute & Category Assignment

### Problem

New products need:
- Category assignment (from hierarchy)
- Attribute population (size, color, material, etc.)
- Tag assignment (for search/filter)
- Related product linking

### Solution: AI Classification Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│  New Product (Name + Description + Image)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  1. CATEGORY CLASSIFICATION                                      │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Input: Product name, description, image                     ││
│  │ Context: Existing category hierarchy (from M_Product_Category)
│  │                                                             ││
│  │ LLM classifies into:                                        ││
│  │ - Primary category (highest confidence)                     ││
│  │ - Secondary categories (for multi-category)                 ││
│  │ - Confidence scores                                         ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. ATTRIBUTE EXTRACTION                                         │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ From product info, extract structured attributes:           ││
│  │                                                             ││
│  │ - Dimensions (L x W x H)                                    ││
│  │ - Weight                                                    ││
│  │ - Color                                                     ││
│  │ - Material                                                  ││
│  │ - Size (S/M/L or numeric)                                   ││
│  │ - Brand                                                     ││
│  │ - Model/Part Number                                         ││
│  │                                                             ││
│  │ Map to M_AttributeSetInstance                               ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. TAG GENERATION                                               │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Generate searchable tags:                                   ││
│  │                                                             ││
│  │ - Product type tags                                         ││
│  │ - Use case tags ("outdoor", "kitchen", "office")            ││
│  │ - Audience tags ("professional", "beginner", "kids")        ││
│  │ - Season tags ("summer", "winter", "all-season")            ││
│  │ - Feature tags ("waterproof", "wireless", "organic")        ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. RELATED PRODUCT LINKING                                      │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ Find and link:                                              ││
│  │                                                             ││
│  │ - Substitutes (similar products)                            ││
│  │ - Complements (bought together)                             ││
│  │ - Variants (same product, different size/color)             ││
│  │                                                             ││
│  │ Uses embedding similarity + LLM reasoning                   ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### Classification Agent

```java
@Agent(name = "ClassificationAgent", domain = "PRODUCT")
public interface ProductClassificationAgent {

    @SystemMessage(fromResource = "prompts/classify-category.txt")
    CategoryClassification classifyCategory(
        @UserMessage String productContext,
        @V("categories") String categoryHierarchy
    );

    @SystemMessage(fromResource = "prompts/extract-attributes.txt")
    ExtractedAttributes extractAttributes(
        @UserMessage String productContext,
        @V("attributeSet") String attributeSetDefinition
    );

    @SystemMessage(fromResource = "prompts/generate-tags.txt")
    GeneratedTags generateTags(
        @UserMessage String productContext
    );

    @SystemMessage(fromResource = "prompts/find-related.txt")
    RelatedProducts findRelatedProducts(
        @UserMessage String productContext,
        @V("candidates") String candidateProducts
    );
}

public record CategoryClassification(
    String primaryCategory,
    double primaryConfidence,
    List<String> secondaryCategories,
    String reasoning
) {}

public record ExtractedAttributes(
    Map<String, String> attributes,  // attributeName -> value
    Map<String, Double> confidence,  // attributeName -> confidence
    List<String> unextractable       // attributes we couldn't find
) {}

public record GeneratedTags(
    List<String> productTypeTags,
    List<String> useCaseTags,
    List<String> audienceTags,
    List<String> featureTags,
    List<String> seasonTags
) {}
```

### Category Classification Prompt

```
You are an expert e-commerce product categorizer.

CATEGORY HIERARCHY:
{categories}

PRODUCT TO CLASSIFY:
Name: {product_name}
Description: {product_description}
Attributes: {attributes}

TASK: Classify this product into the most appropriate category.

RULES:
1. Choose the most specific category that fits
2. A product can have one primary and multiple secondary categories
3. If unsure between categories, prefer the more general one
4. Confidence must reflect actual certainty

OUTPUT (JSON):
{
  "primary_category": "Electronics > Mobile Phones > Smartphones",
  "primary_confidence": 0.95,
  "secondary_categories": ["Electronics > Accessories"],
  "reasoning": "Product is clearly a smartphone based on description mentioning..."
}
```

---

## Module 4: Description Generation

**See [ADR-021: Product Catalog Enhancement](021-product-catalog-enhancement.md)** for detailed description generation architecture.

Summary:
- AI generates short/long descriptions
- SEO optimization (titles, meta, keywords)
- Brand voice consistency
- Bulk processing support
- Quality scoring

---

## Module 5: Quality Control

### Automated Validation

```java
public class ProductQualityValidator {

    public QualityReport validate(MProduct product) {
        QualityReport report = new QualityReport();

        // Required fields
        report.addCheck("Name", product.getName() != null, "Required");
        report.addCheck("SKU", product.getValue() != null, "Required");
        report.addCheck("Category", product.getM_Product_Category_ID() > 0, "Required");

        // Description quality
        if (product.getDescription() != null) {
            int descLen = product.getDescription().length();
            report.addCheck("Description Length",
                descLen >= 100 && descLen <= 500,
                "Should be 100-500 chars, got " + descLen);
        } else {
            report.addCheck("Description", false, "Missing");
        }

        // Image check
        boolean hasImage = hasAttachment(product, "image");
        report.addCheck("Product Image", hasImage, "Required for e-commerce");

        // Price check
        boolean hasPrice = hasPriceList(product);
        report.addCheck("Price", hasPrice, "Required for sales");

        // Attribute completeness
        int attrFilled = countFilledAttributes(product);
        int attrTotal = getTotalAttributes(product);
        report.addCheck("Attributes",
            attrFilled >= attrTotal * 0.7,
            String.format("%d/%d filled", attrFilled, attrTotal));

        // SEO check
        report.addCheck("SEO Title",
            product.getDocumentNote() != null &&
            product.getDocumentNote().length() <= 60,
            "Max 60 chars for SEO title");

        return report;
    }
}

public record QualityReport(
    List<QualityCheck> checks,
    double overallScore,
    List<String> criticalIssues,
    List<String> warnings,
    List<String> suggestions
) {
    public boolean isReadyForPublish() {
        return criticalIssues.isEmpty() && overallScore >= 0.8;
    }
}
```

### Human Review Queue

```
┌─────────────────────────────────────────────────────────────────┐
│  REVIEW QUEUE                                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Filter: [All] [Pending] [Low Confidence] [Critical Issues]     │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ SKU: WIDGET-001                                           │  │
│  │ Name: Blue Widget Pro                                      │  │
│  │ Status: PENDING REVIEW                                     │  │
│  │                                                           │  │
│  │ AI Confidence: 0.72 (below 0.85 threshold)                │  │
│  │                                                           │  │
│  │ Issues:                                                   │  │
│  │ ⚠️ Category confidence low (0.65)                         │  │
│  │ ⚠️ 2 attributes could not be extracted                    │  │
│  │                                                           │  │
│  │ [Approve] [Edit] [Reject] [Re-process]                    │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ SKU: GADGET-002                                           │  │
│  │ Name: Smart Home Controller                                │  │
│  │ Status: PENDING REVIEW                                     │  │
│  │                                                           │  │
│  │ AI Confidence: 0.91 ✓                                     │  │
│  │                                                           │  │
│  │ Ready for publish                                         │  │
│  │                                                           │  │
│  │ [Approve] [Edit] [Reject]                                 │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## End-to-End Workflow

### New Product Batch Import

```
┌─────────────────────────────────────────────────────────────────┐
│  1. UPLOAD                                                       │
│     Supplier sends CSV with 500 products                         │
│     + ZIP of product images                                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. AI MAPPING (5 minutes)                                       │
│     - Analyze CSV columns → iDempiere fields                     │
│     - Match images to products                                   │
│     - Human reviews mapping, adjusts if needed                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. PARALLEL PROCESSING (30-60 minutes for 500 products)         │
│     ┌─────────────┐ ┌─────────────┐ ┌─────────────┐             │
│     │   Import    │ │   Image     │ │  Classify   │             │
│     │   Enrich    │ │   Process   │ │  Attribute  │             │
│     └──────┬──────┘ └──────┬──────┘ └──────┬──────┘             │
│            └───────────────┴───────────────┘                     │
│                            │                                     │
│                            ▼                                     │
│                    Description Generation                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. QUALITY CHECK (Automated)                                    │
│     - Score each product                                         │
│     - Route low-confidence to human review                       │
│     - Auto-approve high-confidence (>0.85)                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. HUMAN REVIEW (10-20% of products)                            │
│     - Review flagged items                                       │
│     - Approve/edit/reject                                        │
│     - Feedback improves AI                                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  6. PUBLISH                                                      │
│     - Activate products (IsActive='Y', IsSold='Y')               │
│     - Sync to e-commerce platform                                │
│     - Index for search                                           │
└─────────────────────────────────────────────────────────────────┘

TIME: 500 products in 2-4 hours (vs 2-4 weeks manual)
```

---

## Data Model Extensions

### New Tables

```sql
-- Import job tracking
CREATE TABLE AIG_ImportJob (
    AIG_ImportJob_ID NUMERIC(10) PRIMARY KEY,
    AD_Client_ID NUMERIC(10) NOT NULL,
    AD_Org_ID NUMERIC(10) NOT NULL,
    Name VARCHAR(60) NOT NULL,
    SourceFile VARCHAR(255),
    Status VARCHAR(20),  -- PENDING, MAPPING, PROCESSING, REVIEW, COMPLETE
    TotalRows NUMERIC(10),
    ProcessedRows NUMERIC(10),
    SuccessRows NUMERIC(10),
    ErrorRows NUMERIC(10),
    MappingJSON TEXT,  -- Column mapping configuration
    Created TIMESTAMP DEFAULT NOW(),
    CreatedBy NUMERIC(10),
    Updated TIMESTAMP DEFAULT NOW(),
    UpdatedBy NUMERIC(10),
    IsActive CHAR(1) DEFAULT 'Y'
);

-- Product processing queue
CREATE TABLE AIG_ProductQueue (
    AIG_ProductQueue_ID NUMERIC(10) PRIMARY KEY,
    AD_Client_ID NUMERIC(10) NOT NULL,
    AD_Org_ID NUMERIC(10) NOT NULL,
    M_Product_ID NUMERIC(10),  -- NULL until created
    AIG_ImportJob_ID NUMERIC(10),
    Status VARCHAR(20),  -- PENDING, PROCESSING, REVIEW, APPROVED, REJECTED
    RawData TEXT,  -- Original import data
    ProcessedData TEXT,  -- AI-enriched data
    AIConfidence NUMERIC(5,2),
    QualityScore NUMERIC(5,2),
    Issues TEXT,  -- JSON array of issues
    ReviewedBy NUMERIC(10),
    ReviewNotes VARCHAR(2000),
    Created TIMESTAMP DEFAULT NOW(),
    Updated TIMESTAMP DEFAULT NOW()
);

-- Image processing tracking
CREATE TABLE AIG_ImageProcess (
    AIG_ImageProcess_ID NUMERIC(10) PRIMARY KEY,
    AD_Client_ID NUMERIC(10) NOT NULL,
    M_Product_ID NUMERIC(10) NOT NULL,
    SourceImage VARCHAR(255),
    ProcessedImage VARCHAR(255),
    BackgroundRemoved CHAR(1) DEFAULT 'N',
    Enhanced CHAR(1) DEFAULT 'N',
    ExtractedAttributes TEXT,  -- JSON
    ALTText VARCHAR(500),
    VariantsGenerated CHAR(1) DEFAULT 'N',
    Created TIMESTAMP DEFAULT NOW()
);
```

---

## Cost Estimation

| Operation | Cost per Item | 500 Products |
|-----------|---------------|--------------|
| Import mapping | ~$0.02 | $10 |
| Data enrichment | ~$0.05 | $25 |
| Image processing | ~$0.10 | $50 |
| Attribute extraction | ~$0.03 | $15 |
| Description generation | ~$0.08 | $40 |
| Quality validation | ~$0.01 | $5 |
| **Total** | **~$0.29** | **~$145** |

**ROI**: Manual processing cost ~$15-20/product = $7,500-10,000 for 500 products

---

## Implementation Phases

### Phase 1: Import Automation (2 weeks)

- AI column mapping
- Data enrichment
- Validation preview
- Basic import to M_Product

### Phase 2: Image Processing (2 weeks)

- Background removal integration
- Image enhancement
- Variant generation
- Attribute extraction from images

### Phase 3: Classification (2 weeks)

- Category classification
- Attribute extraction
- Tag generation
- Related product linking

### Phase 4: Quality & Review (1 week)

- Quality scoring
- Human review queue
- Approval workflow
- Feedback loop

### Phase 5: Integration (1 week)

- End-to-end workflow
- Batch processing UI
- Progress tracking
- E-commerce sync

**Total: 8 weeks**

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Import time reduction | 90% | Before/after |
| Accuracy (category) | 90%+ | Human review sample |
| Accuracy (attributes) | 85%+ | Human review sample |
| Human review rate | <20% | Auto-approved vs total |
| Description quality | 0.8+ score | Quality scorer |
| Image processing time | <30s/image | Average processing |
| Cost per product | <$0.30 | Total AI costs |

---

## References

### Industry Research
- [Mirakl: AI-powered catalog management](https://www.mirakl.com/blog/how-ai-powered-catalog-management-accelerates-growth)
- [AI Journal: PIM Trends 2025](https://aijourn.com/5-product-information-management-trends-driving-ecommerce-in-2025/)
- [Salsify: AI in PIM](https://www.salsify.com/blog/ai-in-pim)
- [Netguru: AI in PIM Systems](https://www.netguru.com/blog/ai-in-pim-systems)
- [Claid.ai: AI Product Photography](https://claid.ai/blog/article/ai-product-photo-tools/)
- [OneSchema: AI Data Import](https://www.oneschema.co/)
- [Hypotenuse: Product Taxonomy](https://www.hypotenuse.ai/blog/ecommerce-product-taxonomy)

### Related ADRs
- [ADR-021: Product Catalog Enhancement](021-product-catalog-enhancement.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [ADR-009: Domain Boundaries](009-domain-boundaries-agent-scope.md)

---

*ADR-030 | Version 1.0 | 2025-12-03*
*Status: Proposed*
