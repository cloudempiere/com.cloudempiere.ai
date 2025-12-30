# ADR-024: Import Data Normalization Use Case

**Status**: Proposed
**Date**: 2025-12-03
**Deciders**: Cloudempiere AI Team
**Phase**: 3 (Future)
**Related**: [ADR-002](002-langchain4j-strategic-adoption.md), [ADR-009](009-domain-boundaries-agent-scope.md), [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)

## Context

### Problem Statement

Data imports from external sources require extensive manual cleanup:
- Customer data from various sources has inconsistent formats
- Product imports have mismatched categories and attributes
- Address data needs standardization and validation
- Name variations cause duplicate records
- Manual cleanup takes hours per import batch

### Business Value

| Metric | Current | Target | Impact |
|--------|---------|--------|--------|
| Import cleanup time | 2-4 hours/batch | 15-30 min/batch | 85% reduction |
| Duplicate records created | 5-10% | <1% | Data quality |
| Address accuracy | Variable | 95%+ | Delivery success |
| Category mapping accuracy | Manual | 90%+ auto | Consistency |

### Common Import Scenarios

1. **Customer Migration** - Moving from legacy system or spreadsheet
2. **Product Catalog Import** - Vendor price lists, e-commerce feeds
3. **Business Partner Merge** - Consolidating multiple sources
4. **Address Standardization** - Postal code validation, formatting
5. **Name Normalization** - Company vs individual, titles, formats

## Decision

Implement an **Import Data Normalization** feature using AI to clean, standardize, and deduplicate imported data before it enters iDempiere.

### Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Import Source                                      │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │  CSV/Excel Upload  │  REST API  │  File Watch  │  Manual Entry   ││
│  └──────────────────────────────────────────────────────────────────┘│
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Import Loader                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Processing:                                                     │  │
│  │   - File parsing (CSV, Excel, JSON)                            │  │
│  │   - Column mapping (auto-detect or manual)                      │  │
│  │   - Data type inference                                         │  │
│  │   - Staging table population                                    │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    DataNormalizationAgent                             │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Normalization Pipeline:                                         │  │
│  │   1. NameNormalizer - Company/person name standardization      │  │
│  │   2. AddressNormalizer - Postal validation, formatting         │  │
│  │   3. PhoneNormalizer - International format                    │  │
│  │   4. EmailValidator - Format and deliverability                │  │
│  │   5. CategoryMapper - Map to existing categories               │  │
│  │   6. DuplicateDetector - Find existing matches                 │  │
│  │   7. DataEnricher - Fill missing fields                        │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Normalization Result                               │
│  {                                                                    │
│    "original": {...},                                                │
│    "normalized": {...},                                              │
│    "changes": ["Name standardized", "Address validated"],            │
│    "potential_duplicates": [...],                                    │
│    "confidence": 0.92,                                               │
│    "warnings": ["Phone format uncertain"],                           │
│    "ready_for_import": true                                          │
│  }                                                                    │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Review & Import                                    │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ Confidence Routing:                                             │  │
│  │   - HIGH (>0.95): Auto-import                                   │  │
│  │   - MEDIUM (0.80-0.95): Review changes                          │  │
│  │   - LOW (<0.80): Manual review required                         │  │
│  │   - DUPLICATE: Merge or skip decision                           │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────┘
```

### System Prompt

```
You are a data normalization expert for ERP system imports.

CONTEXT:
- Target Entity: {entity_type} (Business Partner / Product / etc.)
- Source System: {source_system}
- Country: {default_country}
- Language: {language}

TASK: Normalize and validate the following data record.

INPUT DATA:
{raw_data_json}

EXISTING RECORDS (for duplicate detection):
{potential_matches_json}

OUTPUT FORMAT (JSON only):
{
  "normalized": {
    "name": "Standardized name",
    "name2": "Additional name if applicable",
    "is_company": true/false,
    "tax_id": "Formatted tax ID",
    "address1": "Street address",
    "address2": "Additional address",
    "city": "City name",
    "postal": "Postal/ZIP code",
    "region": "State/Province",
    "country_code": "ISO country code",
    "phone": "+XX XXX XXX XXXX",
    "phone2": "Secondary phone",
    "fax": "Fax if present",
    "email": "validated@email.com",
    "url": "https://website.com"
  },
  "changes_made": [
    {"field": "name", "from": "ABC corp.", "to": "ABC Corp.", "reason": "Capitalization"},
    {"field": "postal", "from": "1234", "to": "01234", "reason": "Leading zero added"}
  ],
  "potential_duplicates": [
    {
      "C_BPartner_ID": 12345,
      "name": "ABC Corporation",
      "similarity": 0.92,
      "matching_fields": ["tax_id", "city"],
      "recommendation": "MERGE|SKIP|NEW"
    }
  ],
  "validation": {
    "address_valid": true,
    "email_valid": true,
    "phone_valid": true,
    "tax_id_valid": true
  },
  "confidence": 0.0-1.0,
  "warnings": ["Warning message if applicable"],
  "errors": ["Error if data cannot be normalized"],
  "ready_for_import": true/false
}

NORMALIZATION RULES:

1. Company Names:
   - Remove trailing punctuation (Inc., Ltd., GmbH stay)
   - Capitalize properly (ABC Corp. not ABC CORP or abc corp)
   - Expand common abbreviations consistently
   - Keep legal suffixes (GmbH, Ltd, Inc, S.A., etc.)

2. Person Names:
   - Format: "First Last" or "Last, First" based on locale
   - Handle titles (Dr., Prof., etc.) separately
   - Remove unnecessary spaces/characters

3. Addresses:
   - Street number before/after based on country
   - Standardize abbreviations (St., Ave., Str., etc.)
   - Validate postal codes for country format
   - Expand abbreviated cities if needed

4. Phone Numbers:
   - Convert to E.164 format (+XX XXX XXXX XXXX)
   - Add country code if missing (use default country)
   - Remove non-numeric characters except + and spaces

5. Emails:
   - Lowercase
   - Basic format validation
   - Check for common typos (gmial→gmail, etc.)

6. Tax IDs:
   - Format based on country rules
   - Validate checksum where applicable (VAT, etc.)
   - Flag if format doesn't match country

7. Duplicate Detection:
   - Match on: Tax ID (exact), Name (fuzzy 85%+), Address
   - Consider email and phone as secondary matches
   - MERGE if high confidence same entity
   - NEW if clearly different
   - REVIEW if uncertain
```

### Agent Definition

```java
package com.cloudempiere.ai.agent.import;

@Agent(
    name = "DataNormalizationAgent",
    domain = "IMPORT",
    riskLevel = RiskLevel.LOW
)
public interface DataNormalizationAgent {

    @SystemMessage(fromResource = "prompts/data-normalization.txt")
    NormalizationResult normalize(
        @UserMessage String dataContext
    );

    @SystemMessage(fromResource = "prompts/data-normalization.txt")
    List<NormalizationResult> normalizeBatch(
        @UserMessage String batchContext
    );

    @SystemMessage(fromResource = "prompts/duplicate-detection.txt")
    DuplicateAnalysis analyzeDuplicates(
        @UserMessage String recordsToCompare
    );
}

public record NormalizationResult(
    @Description("Original input data") Map<String, Object> original,
    @Description("Normalized data") NormalizedRecord normalized,
    @Description("Changes made") List<FieldChange> changes_made,
    @Description("Potential duplicates") List<DuplicateMatch> potential_duplicates,
    @Description("Validation results") ValidationResult validation,
    @Description("Overall confidence") double confidence,
    @Description("Warning messages") List<String> warnings,
    @Description("Error messages") List<String> errors,
    @Description("Ready for import") boolean ready_for_import
) {}

public record NormalizedRecord(
    String name,
    String name2,
    boolean is_company,
    String tax_id,
    String address1,
    String address2,
    String city,
    String postal,
    String region,
    String country_code,
    String phone,
    String phone2,
    String fax,
    String email,
    String url
) {}

public record FieldChange(
    String field,
    String from,
    String to,
    String reason
) {}

public record DuplicateMatch(
    int record_id,
    String name,
    double similarity,
    List<String> matching_fields,
    DuplicateRecommendation recommendation
) {}

public enum DuplicateRecommendation {
    MERGE,  // High confidence same entity
    SKIP,   // Already exists, don't import
    NEW,    // Different entity, create new
    REVIEW  // Uncertain, needs human review
}
```

### Boundaries (per ADR-009)

```yaml
Agent: DataNormalizationAgent
Domain: IMPORT
Risk Level: LOW

Data Access:
  Read Tables:
    - C_BPartner (duplicate detection)
    - C_BPartner_Location (address matching)
    - AD_User (contact matching)
    - M_Product (product matching)
    - M_Product_Category (category mapping)
    - C_Country (country validation)
    - C_Region (region validation)

  Write Tables:
    - I_BPartner (import staging)
    - I_Product (import staging)
    - AIG_ImportBatch (batch tracking)
    - AIG_ImportLog (processing log)

  Forbidden Tables:
    - Financial tables
    - AD_User credentials
    - System configuration

Organizational:
  - Respects AD_Role access
  - Import within AD_Client_ID
  - Cannot cross-tenant

Token Limits:
  Max Tokens/Request: 10,000
  Max Records/Batch: 100

Cost:
  Daily Budget: $100
  Cost per Record: ~$0.02 (estimated)

Performance:
  Response Time: <3 seconds (single)
  Batch Throughput: 100 records/minute
```

### Specialized Normalizers

```java
public class NameNormalizer {

    /**
     * Normalize company or person name
     */
    public NameResult normalize(String rawName, boolean isCompany) {
        String normalized = rawName.trim();

        // Remove extra whitespace
        normalized = normalized.replaceAll("\\s+", " ");

        if (isCompany) {
            normalized = normalizeCompanyName(normalized);
        } else {
            normalized = normalizePersonName(normalized);
        }

        return new NameResult(normalized, detectNameType(normalized));
    }

    private String normalizeCompanyName(String name) {
        // Keep legal suffixes as-is
        // Proper case the rest
        // Handle common abbreviations

        String[] suffixes = {"GmbH", "AG", "Ltd", "Inc", "Corp", "LLC", "S.A.", "B.V."};
        String suffix = extractSuffix(name, suffixes);
        String base = removeSuffix(name, suffix);

        // Title case the base
        base = toTitleCase(base);

        return suffix != null ? base + " " + suffix : base;
    }

    private String normalizePersonName(String name) {
        // Detect format (First Last vs Last, First)
        // Extract titles (Dr., Prof., etc.)
        // Proper case

        if (name.contains(",")) {
            // Last, First format
            String[] parts = name.split(",", 2);
            return toTitleCase(parts[1].trim()) + " " + toTitleCase(parts[0].trim());
        }

        return toTitleCase(name);
    }
}

public class AddressNormalizer {

    /**
     * Normalize and validate address
     */
    public AddressResult normalize(String address1, String address2,
                                   String city, String postal,
                                   String region, String countryCode) {
        AddressResult result = new AddressResult();

        // Normalize country
        MCountry country = MCountry.get(ctx, countryCode);
        if (country == null) {
            country = detectCountry(postal, city);
        }
        result.setCountryCode(country.getCountryCode());

        // Validate postal code format
        String normalizedPostal = normalizePostalCode(postal, country);
        result.setPostal(normalizedPostal);
        result.setPostalValid(validatePostalCode(normalizedPostal, country));

        // Normalize city
        result.setCity(toTitleCase(city.trim()));

        // Normalize region
        if (region != null) {
            MRegion reg = findRegion(region, country);
            if (reg != null) {
                result.setRegion(reg.getName());
            }
        }

        // Normalize street address
        result.setAddress1(normalizeStreetAddress(address1, country));
        result.setAddress2(address2 != null ? address2.trim() : null);

        return result;
    }

    private String normalizePostalCode(String postal, MCountry country) {
        if (postal == null) return null;

        postal = postal.trim().toUpperCase();

        // Country-specific formatting
        switch (country.getCountryCode()) {
            case "DE":
                // German: 5 digits, add leading zeros
                return String.format("%05d", Integer.parseInt(postal.replaceAll("\\D", "")));
            case "US":
                // US: 5 or 9 digits (ZIP+4)
                postal = postal.replaceAll("[^0-9]", "");
                if (postal.length() == 9) {
                    return postal.substring(0, 5) + "-" + postal.substring(5);
                }
                return postal;
            case "GB":
                // UK: Format AA9A 9AA
                return formatUKPostcode(postal);
            default:
                return postal;
        }
    }
}

public class PhoneNormalizer {

    /**
     * Normalize phone to E.164 format
     */
    public PhoneResult normalize(String phone, String defaultCountryCode) {
        if (phone == null || phone.trim().isEmpty()) {
            return new PhoneResult(null, false, "Empty phone");
        }

        // Remove all non-numeric except +
        String cleaned = phone.replaceAll("[^0-9+]", "");

        // Already has country code?
        if (cleaned.startsWith("+")) {
            return formatE164(cleaned);
        }

        // Add default country code
        String countryPrefix = getCountryPrefix(defaultCountryCode);
        if (countryPrefix != null) {
            // Remove leading 0 if present
            if (cleaned.startsWith("0")) {
                cleaned = cleaned.substring(1);
            }
            cleaned = "+" + countryPrefix + cleaned;
            return formatE164(cleaned);
        }

        return new PhoneResult(cleaned, false, "Could not determine country code");
    }

    private PhoneResult formatE164(String phone) {
        // Validate length (E.164 max 15 digits)
        String digits = phone.replace("+", "");
        if (digits.length() > 15) {
            return new PhoneResult(phone, false, "Too many digits");
        }

        // Format with spaces for readability
        // +XX XXX XXX XXXX
        StringBuilder formatted = new StringBuilder("+");
        formatted.append(digits.substring(0, 2)).append(" ");  // Country
        formatted.append(digits.substring(2, Math.min(5, digits.length())));
        if (digits.length() > 5) {
            formatted.append(" ").append(digits.substring(5, Math.min(8, digits.length())));
        }
        if (digits.length() > 8) {
            formatted.append(" ").append(digits.substring(8));
        }

        return new PhoneResult(formatted.toString(), true, null);
    }
}

public class DuplicateDetector {

    /**
     * Find potential duplicates in existing data
     */
    public List<DuplicateMatch> findDuplicates(NormalizedRecord record) {
        List<DuplicateMatch> matches = new ArrayList<>();

        // 1. Exact Tax ID match (highest confidence)
        if (record.tax_id() != null) {
            List<MBPartner> taxMatches = new Query(ctx, MBPartner.Table_Name,
                "TaxID=? AND IsActive='Y'", null)
                .setParameters(record.tax_id())
                .list();

            for (MBPartner bp : taxMatches) {
                matches.add(new DuplicateMatch(
                    bp.getC_BPartner_ID(),
                    bp.getName(),
                    0.99,
                    List.of("tax_id"),
                    DuplicateRecommendation.MERGE
                ));
            }
        }

        // 2. Name similarity (fuzzy matching)
        List<MBPartner> nameMatches = findByNameSimilarity(record.name(), 0.85);
        for (MBPartner bp : nameMatches) {
            if (!alreadyMatched(matches, bp.getC_BPartner_ID())) {
                double similarity = calculateSimilarity(record.name(), bp.getName());
                List<String> matchingFields = new ArrayList<>();
                matchingFields.add("name");

                // Check additional fields
                if (cityMatches(record, bp)) {
                    similarity += 0.05;
                    matchingFields.add("city");
                }
                if (emailMatches(record, bp)) {
                    similarity += 0.1;
                    matchingFields.add("email");
                }

                matches.add(new DuplicateMatch(
                    bp.getC_BPartner_ID(),
                    bp.getName(),
                    Math.min(similarity, 0.99),
                    matchingFields,
                    similarity > 0.95 ? DuplicateRecommendation.MERGE :
                    similarity > 0.85 ? DuplicateRecommendation.REVIEW :
                    DuplicateRecommendation.NEW
                ));
            }
        }

        return matches;
    }

    private double calculateSimilarity(String s1, String s2) {
        // Levenshtein distance normalized to 0-1
        int distance = levenshteinDistance(s1.toLowerCase(), s2.toLowerCase());
        int maxLength = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) distance / maxLength);
    }
}
```

### Import Service

```java
public class AIImportService {

    private final DataNormalizationAgent agent;
    private final DuplicateDetector duplicateDetector;

    /**
     * Process import batch with normalization
     */
    public ImportBatchResult processImportBatch(List<Map<String, Object>> records,
                                                 ImportConfig config) {
        ImportBatchResult result = new ImportBatchResult();

        for (Map<String, Object> record : records) {
            try {
                // 1. Normalize
                String context = buildNormalizationContext(record, config);
                NormalizationResult normalized = agent.normalize(context);

                // 2. Find duplicates
                List<DuplicateMatch> duplicates = duplicateDetector
                    .findDuplicates(normalized.normalized());
                normalized = normalized.withDuplicates(duplicates);

                // 3. Route by confidence
                if (normalized.confidence() >= 0.95 &&
                    normalized.potential_duplicates().isEmpty()) {
                    // Auto-import
                    int recordId = importRecord(normalized.normalized(), config);
                    result.addSuccess(recordId);
                } else if (normalized.confidence() >= 0.80) {
                    // Queue for review
                    result.addForReview(normalized);
                } else {
                    // Manual handling required
                    result.addManual(normalized);
                }

            } catch (Exception e) {
                result.addError(record, e.getMessage());
            }
        }

        return result;
    }

    /**
     * Import normalized record to iDempiere
     */
    private int importRecord(NormalizedRecord normalized, ImportConfig config) {
        if (config.getEntityType() == EntityType.BUSINESS_PARTNER) {
            return importBusinessPartner(normalized);
        } else if (config.getEntityType() == EntityType.PRODUCT) {
            return importProduct(normalized);
        }
        throw new IllegalArgumentException("Unknown entity type");
    }

    private int importBusinessPartner(NormalizedRecord normalized) {
        MBPartner bp = new MBPartner(ctx, 0, trxName);

        bp.setName(normalized.name());
        bp.setName2(normalized.name2());
        bp.setIsCompany(normalized.is_company());
        bp.setTaxID(normalized.tax_id());

        // Set type based on import config
        bp.setIsCustomer(true);  // or from config

        bp.saveEx();

        // Create location
        MBPartnerLocation bpLoc = new MBPartnerLocation(bp);
        MLocation loc = new MLocation(ctx, 0, trxName);

        loc.setAddress1(normalized.address1());
        loc.setAddress2(normalized.address2());
        loc.setCity(normalized.city());
        loc.setPostal(normalized.postal());
        loc.setC_Country_ID(getCountryId(normalized.country_code()));

        loc.saveEx();
        bpLoc.setC_Location_ID(loc.getC_Location_ID());
        bpLoc.setPhone(normalized.phone());
        bpLoc.setPhone2(normalized.phone2());
        bpLoc.setFax(normalized.fax());
        bpLoc.saveEx();

        // Create contact if email present
        if (normalized.email() != null) {
            MUser contact = new MUser(bp);
            contact.setEMail(normalized.email());
            contact.saveEx();
        }

        return bp.getC_BPartner_ID();
    }
}
```

## Implementation Plan

### Phase 1: Core Normalization (Days 1-5)

| Task | Effort | Owner |
|------|--------|-------|
| `DataNormalizationAgent` interface | 4h | Dev |
| System prompt for normalization | 6h | Dev |
| `NameNormalizer` | 6h | Dev |
| `AddressNormalizer` | 8h | Dev |
| `PhoneNormalizer` | 4h | Dev |
| Email validation | 2h | Dev |

### Phase 2: Duplicate Detection (Days 6-9)

| Task | Effort | Owner |
|------|--------|-------|
| `DuplicateDetector` | 8h | Dev |
| Fuzzy name matching | 6h | Dev |
| Multi-field matching | 4h | Dev |
| Merge recommendation logic | 6h | Dev |

### Phase 3: Import Service (Days 10-14)

| Task | Effort | Owner |
|------|--------|-------|
| `AIImportService` | 8h | Dev |
| Business Partner import | 6h | Dev |
| Product import | 6h | Dev |
| Batch processing | 6h | Dev |
| Confidence routing | 4h | Dev |

### Phase 4: UI & Testing (Days 15-20)

| Task | Effort | Owner |
|------|--------|-------|
| Import wizard UI | 10h | Dev |
| Column mapping interface | 6h | Dev |
| Review queue UI | 8h | Dev |
| Testing with real data | 12h | QA |
| Accuracy measurement | 6h | QA |

### Deliverables

- `DataNormalizationAgent.java` (~150 lines)
- `NormalizationResult.java` + DTOs (~200 lines)
- `NameNormalizer.java` (~200 lines)
- `AddressNormalizer.java` (~300 lines)
- `PhoneNormalizer.java` (~150 lines)
- `DuplicateDetector.java` (~250 lines)
- `AIImportService.java` (~300 lines)
- `data-normalization.txt` (system prompt)
- Import wizard ZK window
- Review queue interface

**Total Effort**: 20 days
**Risk Level**: Medium (data quality varies, duplicate logic complex)

## Acceptance Criteria

```yaml
Functional:
  - [ ] Normalize names (company and person)
  - [ ] Validate and format addresses
  - [ ] Format phone numbers (E.164)
  - [ ] Validate emails
  - [ ] Detect duplicates (90%+ accuracy)
  - [ ] Batch processing (100+ records)
  - [ ] Review queue for uncertain records

Non-Functional:
  - [ ] Processing time: <3 seconds per record
  - [ ] Duplicate detection accuracy: 90%+
  - [ ] False positive rate: <5%
  - [ ] Batch throughput: 100 records/minute

Countries Supported:
  - [ ] Germany (DE)
  - [ ] Austria (AT)
  - [ ] Switzerland (CH)
  - [ ] USA (US)
  - [ ] UK (GB)
  - [ ] (Extensible to others)
```

## Consequences

### Positive

1. **Data Quality**: Consistent formatting across records
2. **Duplicate Prevention**: 90%+ duplicate detection
3. **Time Savings**: 85% reduction in cleanup time
4. **Accuracy**: Validated addresses and phone numbers
5. **Scalability**: Process large batches efficiently

### Negative

1. **Country Specificity**: Rules vary by country
2. **Edge Cases**: Unusual formats may fail
3. **Duplicate Ambiguity**: Some cases need human judgment
4. **Maintenance**: Rules need updates for new formats

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Wrong normalization | MEDIUM | Confidence routing, review queue |
| Missed duplicates | HIGH | Multiple matching strategies |
| False duplicate positives | MEDIUM | Conservative thresholds |
| Country format errors | MEDIUM | Extensible rule system |

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Normalization accuracy | 95%+ | Sample review |
| Duplicate detection | 90%+ | Post-import audit |
| False positive rate | <5% | Review tracking |
| Auto-import rate | 70%+ | Confidence > 0.95 |
| Processing time | <3 sec/record | Performance logs |
| Time savings | 85% reduction | Before/after |

## References

- [MVP Documentation](../iDempiere-AI-Assistant-MVP-Documentation.md)
- [ADR-002: LangChain4j Strategic Adoption](002-langchain4j-strategic-adoption.md)
- [ADR-009: Domain Boundaries](009-domain-boundaries-agent-scope.md)
- [E.164 Phone Number Standard](https://en.wikipedia.org/wiki/E.164)

---

*ADR-024 | Version 1.0 | 2025-12-03*
