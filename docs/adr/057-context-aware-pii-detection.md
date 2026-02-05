# ADR-057: Context-Aware PII Detection and Risk Classification

## Status

Accepted

## Date

2026-02-05

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

The current `InputGuard` PII detection uses simple regex patterns that match digit sequences without considering context. This creates **false positives** where legitimate business identifiers (order IDs, invoice numbers, reference codes) are incorrectly flagged as sensitive data like bank accounts.

**Example Problem:**
```
User input: "Find order AQV/PO/260213995"
Current behavior: "260213995" matches BANK_ACCOUNT_PATTERN → Masked to "AQV/PO/************"
Expected behavior: Recognize as order ID → Allow unchanged
```

### Current Implementation Issues

**InputGuard.java lines 86-89:**
```java
/** Bank account number (8-17 digits) */
private static final Pattern BANK_ACCOUNT_PATTERN = Pattern.compile(
    "\\b\\d{8,17}\\b"
);
```

**Problems:**
1. **Too Broad** - Matches ANY 8-17 digit sequence (order IDs, invoices, product codes)
2. **No Context** - Doesn't check for words like "account", "bank", "routing"
3. **No Structure Awareness** - Ignores business ID formats (PREFIX/TYPE/NUMBER)
4. **Binary Classification** - All matches treated equally (no risk levels)

### Regulatory Context

According to official standards for AI/LLM sensitive data handling:

| Standard | Requirement | Current Gap |
|----------|-------------|-------------|
| **OWASP LLM Top 10 (2025)** | LLM06: Context-aware PII detection | ❌ No context analysis |
| **NIST AI RMF** | Risk-based classification | ❌ Binary (PII/not PII) |
| **GDPR Article 4** | Data minimization - only flag actual PII | ❌ Over-flagging non-PII |
| **ISO/IEC 42001:2023** | Configurable sensitivity levels | ❌ Hardcoded patterns |

**Key Finding:** No standard defines exact patterns for bank accounts. They all emphasize:
- **Context matters** - "260213995" alone is not sensitive
- **Risk-based approach** - Different data has different sensitivity levels
- **Configurability** - Organizations define their own PII policies

---

## Decision Drivers

- **Accuracy** - Reduce false positives (business IDs flagged as PII)
- **Compliance** - Align with OWASP LLM Top 10, NIST AI RMF
- **Usability** - Don't break legitimate business operations
- **Configurability** - Different tenants have different PII policies
- **Performance** - Maintain <10ms detection overhead

---

## Decision

Implement **context-aware PII detection with risk classification** using a hybrid approach:

### 1. Context-Aware Detection

Replace simple regex matching with **keyword proximity analysis**:

```java
public class ContextAwarePIIDetector {

    // Patterns that REQUIRE context keywords
    private static final Map<Pattern, List<String>> CONTEXTUAL_PATTERNS = Map.of(
        // Bank accounts ONLY with context
        Pattern.compile("\\b\\d{8,17}\\b"),
        List.of("account", "bank", "routing", "swift", "iban", "aba", "acct")
    );

    /**
     * Check if number appears within N words of context keywords.
     *
     * @param text Full input text
     * @param match Matched number (e.g., "260213995")
     * @param keywords Required context keywords
     * @param windowSize Words before/after to check (default: 5)
     */
    public boolean hasSensitiveContext(String text, String match,
                                       List<String> keywords, int windowSize) {
        int matchIndex = text.indexOf(match);
        if (matchIndex == -1) return false;

        // Extract ~50 character window around match
        int start = Math.max(0, matchIndex - (windowSize * 10));
        int end = Math.min(text.length(), matchIndex + match.length() + (windowSize * 10));
        String window = text.substring(start, end).toLowerCase();

        // Check if any keyword appears in window
        return keywords.stream().anyMatch(window::contains);
    }
}
```

**Example Results:**
```java
// ✅ PASS - No context
"Find order AQV/PO/260213995" → No "bank"/"account" nearby → ALLOW

// ❌ BLOCK - Has context
"My bank account is 260213995" → "bank account" nearby → MASK/BLOCK

// ✅ PASS - Business format
"Invoice INV-123456789" → Recognized structure → ALLOW
```

### 2. Business Format Exclusion

Add **structure-based recognition** for known business ID patterns:

```java
public class BusinessFormatRecognizer {

    // Known safe business ID formats (configurable per tenant)
    private static final List<Pattern> SAFE_PATTERNS = List.of(
        // Order IDs: PREFIX/TYPE/NUMBER
        Pattern.compile("[A-Z]{2,5}/[A-Z]{2}/\\d{6,12}"),

        // Invoice IDs: INV-NUMBER
        Pattern.compile("INV-?\\d{6,12}"),

        // Reference: REF# NUMBER
        Pattern.compile("REF[#\\s]?\\d{6,12}"),

        // Product codes
        Pattern.compile("PROD[A-Z0-9]{6,12}")
    );

    /**
     * Check if number is part of a business ID format.
     */
    public boolean isBusinessIdentifier(String text, String number) {
        // Check if number appears in a recognized pattern
        return SAFE_PATTERNS.stream()
            .anyMatch(p -> p.matcher(text).find() && text.contains(number));
    }
}
```

### 3. Risk Classification

Replace binary (PII/not PII) with **graduated risk levels**:

```java
/**
 * Risk classification based on NIST AI RMF and GDPR sensitivity levels.
 */
public enum DataSensitivityLevel {
    /**
     * PUBLIC - No risk if exposed
     * Examples: Order IDs, product codes, public reference numbers
     * Action: ALLOW (log only)
     */
    PUBLIC(0, PIIAction.ALLOW),

    /**
     * INTERNAL - Low risk, business context
     * Examples: Customer names, company addresses
     * Action: ALLOW (log with metadata)
     */
    INTERNAL(1, PIIAction.ALLOW),

    /**
     * CONFIDENTIAL - Medium risk
     * Examples: Email addresses, phone numbers
     * Action: MASK (keep last 4 digits)
     */
    CONFIDENTIAL(2, PIIAction.MASK),

    /**
     * RESTRICTED - High risk, regulatory requirements
     * Examples: Bank accounts (with context), tax IDs
     * Action: BLOCK (reject request)
     */
    RESTRICTED(3, PIIAction.BLOCK),

    /**
     * CRITICAL - Extreme risk
     * Examples: SSN, credit cards, passwords
     * Action: BLOCK + ALERT security team
     */
    CRITICAL(4, PIIAction.BLOCK_AND_ALERT);
}
```

**Updated Pattern Definitions:**
```java
private static final List<PIIPattern> PATTERNS = List.of(
    // CRITICAL - Always block
    new PIIPattern(
        "SSN",
        Pattern.compile("\\b\\d{3}[-\\s]?\\d{2}[-\\s]?\\d{4}\\b"),
        List.of("ssn", "social security"),
        DataSensitivityLevel.CRITICAL
    ),

    // RESTRICTED - Block only with context
    new PIIPattern(
        "Bank Account",
        Pattern.compile("\\b\\d{8,17}\\b"),
        List.of("account", "bank", "routing", "iban"), // ← Context required
        DataSensitivityLevel.RESTRICTED
    ),

    // CONFIDENTIAL - Mask
    new PIIPattern(
        "Email",
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
        List.of(), // No context needed
        DataSensitivityLevel.CONFIDENTIAL
    ),

    // PUBLIC - Allow (for reference)
    new PIIPattern(
        "Order ID",
        Pattern.compile("[A-Z]{2,5}/[A-Z]{2}/\\d{6,12}"),
        List.of(),
        DataSensitivityLevel.PUBLIC
    )
);
```

### 4. Tenant Configuration

Allow tenants to **override default risk levels**:

```java
/**
 * Tenant-specific PII configuration.
 * Stored in AIG_PIIConfig table.
 */
public class TenantPIIConfiguration {

    /**
     * Example: E-commerce company may treat phone/email as INTERNAL,
     * while financial institution treats them as CONFIDENTIAL.
     */
    public DataSensitivityLevel getRiskLevel(String piiType, int clientId) {
        // Check tenant overrides first
        DataSensitivityLevel override = loadOverride(piiType, clientId);
        if (override != null) {
            return override;
        }
        // Fall back to default
        return getDefaultRiskLevel(piiType);
    }
}
```

### 5. Hybrid Detection Flow

```java
public GuardResult detect(String text, int clientId) {
    // Step 1: Fast path - Check business formats
    if (businessFormatRecognizer.isAllBusinessFormats(text)) {
        return GuardResult.pass(text);
    }

    // Step 2: Context-aware pattern matching
    PIIDetectionResult result = contextDetector.detectWithContext(text);

    // Step 3: Risk classification
    RiskClassifiedResult riskResult = riskClassifier.classify(result);

    // Step 4: Apply tenant overrides
    riskResult = tenantConfig.applyOverrides(riskResult, clientId);

    // Step 5: Execute action based on highest risk
    DataSensitivityLevel highestRisk = riskResult.getHighestRiskLevel();

    switch (highestRisk.getDefaultAction()) {
        case ALLOW:
            return GuardResult.pass(text);
        case MASK:
            return GuardResult.mask(maskSensitiveData(text, riskResult));
        case BLOCK:
            return GuardResult.block("Input contains restricted information");
        case BLOCK_AND_ALERT:
            alertSecurityTeam(riskResult);
            return GuardResult.block("Input contains critical sensitive information");
    }
}
```

---

## Database Schema

### New Table: AIG_PIIConfig

```sql
CREATE TABLE AIG_PIIConfig (
    AIG_PIIConfig_ID NUMERIC(10) NOT NULL,
    AD_Client_ID NUMERIC(10) NOT NULL,
    AD_Org_ID NUMERIC(10) NOT NULL,
    PIIType VARCHAR(100) NOT NULL,        -- "Bank Account", "Email", etc.
    SensitivityLevel VARCHAR(20) NOT NULL, -- "PUBLIC", "INTERNAL", etc.
    RequireContext CHAR(1) DEFAULT 'Y',    -- Require keyword context
    ContextKeywords TEXT,                  -- JSON array ["account", "bank"]
    IsActive CHAR(1) DEFAULT 'Y',
    Created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CreatedBy NUMERIC(10) NOT NULL,
    Updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedBy NUMERIC(10) NOT NULL,
    CONSTRAINT AIG_PIIConfig_Key PRIMARY KEY (AIG_PIIConfig_ID),
    CONSTRAINT AIG_PIIConfig_Unique UNIQUE (AD_Client_ID, PIIType)
);

CREATE INDEX idx_aig_piiconfig_client ON AIG_PIIConfig(AD_Client_ID);
```

### New Table: AIG_BusinessFormat

```sql
CREATE TABLE AIG_BusinessFormat (
    AIG_BusinessFormat_ID NUMERIC(10) NOT NULL,
    AD_Client_ID NUMERIC(10) NOT NULL,
    AD_Org_ID NUMERIC(10) NOT NULL,
    Name VARCHAR(100) NOT NULL,            -- "Order ID", "Invoice ID"
    Pattern VARCHAR(500) NOT NULL,         -- Regex pattern
    Description VARCHAR(1000),
    IsActive CHAR(1) DEFAULT 'Y',
    Created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CreatedBy NUMERIC(10) NOT NULL,
    Updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedBy NUMERIC(10) NOT NULL,
    CONSTRAINT AIG_BusinessFormat_Key PRIMARY KEY (AIG_BusinessFormat_ID)
);

CREATE INDEX idx_aig_bizformat_client ON AIG_BusinessFormat(AD_Client_ID);
```

---

## Implementation Phases

### Phase 1: Context-Aware Detection (v0.33.0) - **2 days**
- [x] Add `ContextAwarePIIDetector` class
- [ ] Add keyword proximity check to `InputGuard`
- [ ] Update `BANK_ACCOUNT_PATTERN` to require context
- [ ] Unit tests for context detection

### Phase 2: Business Format Recognition (v0.33.0) - **1 day**
- [ ] Add `BusinessFormatRecognizer` class
- [ ] Add default safe patterns (order IDs, invoices)
- [ ] Integration with `InputGuard`
- [ ] Unit tests for format recognition

### Phase 3: Risk Classification (v0.34.0) - **2 days**
- [ ] Add `DataSensitivityLevel` enum
- [ ] Add `PIIAction` enum
- [ ] Update all PII patterns with risk levels
- [ ] Update `GuardResult` to support graduated actions

### Phase 4: Tenant Configuration (v0.34.0) - **2 days**
- [ ] Create `AIG_PIIConfig` table
- [ ] Create `AIG_BusinessFormat` table
- [ ] Add `TenantPIIConfiguration` class
- [ ] Migration scripts with sensible defaults

### Phase 5: Testing & Documentation (v0.34.0) - **1 day**
- [ ] Integration tests for all scenarios
- [ ] Performance benchmarks (target: <10ms)
- [ ] Update documentation
- [ ] Update ADR-014 and ADR-048 references

**Total Effort:** 8 days

---

## Quick Fix (Immediate)

For immediate deployment before the comprehensive solution:

**Option 1: Disable Generic Bank Account Pattern**
```java
// In InputGuard.java, comment out lines 86-89 and 274-277
// This removes the overly broad pattern entirely
```

**Option 2: Add Context Requirement (Minimal Change)**
```java
// In InputGuard.detectPII(), add context check before masking:
if (BANK_ACCOUNT_PATTERN.matcher(result).find()) {
    // Only flag if "account" or "bank" appears nearby
    if (result.toLowerCase().matches(".*\\b(account|bank|routing|iban)\\b.*\\d{8,17}.*") ||
        result.toLowerCase().matches(".*\\d{8,17}.*\\b(account|bank|routing|iban)\\b.*")) {
        types.add("Bank Account");
        result = BANK_ACCOUNT_PATTERN.matcher(result).replaceAll(maskChar.repeat(12));
    }
}
```

**Option 3: Add Business Format Exclusion (Best Quick Fix)**
```java
// Add to InputGuard.java before PII detection:
private static final Pattern BUSINESS_ID_PATTERN = Pattern.compile(
    "[A-Z]{2,5}/[A-Z]{2}/\\d{6,12}|INV-?\\d{6,12}|REF[#\\s]?\\d{6,12}"
);

// In detectPII(), skip text matching business formats:
if (BUSINESS_ID_PATTERN.matcher(input).find()) {
    return new PIIDetectionResult(false, List.of(), input); // Not PII
}
```

See implementation details in "Quick Fix Implementation" section below.

---

## Consequences

### Positive
- ✅ Eliminates false positives (business IDs no longer masked)
- ✅ Aligns with OWASP LLM Top 10 and NIST AI RMF
- ✅ Configurable per tenant
- ✅ Graduated risk levels enable nuanced handling
- ✅ Better user experience (legitimate queries work)

### Negative
- ⚠️ Additional complexity (~8 days implementation)
- ⚠️ Slight performance overhead (~5-10ms per request)
- ⚠️ Requires tenant configuration for optimal results

### Risks
- ⚠️ Sophisticated attackers may craft input to bypass context checks
- ⚠️ Need comprehensive test suite to avoid regression
- ⚠️ Tenant misconfiguration could expose sensitive data

### Mitigation
- Defense in depth: Context detection is one layer, not sole protection
- Extensive unit tests covering edge cases
- Safe defaults + audit logging of configuration changes

---

## Performance Targets

| Operation | Target | Implementation |
|-----------|--------|----------------|
| Business format check | <1ms | Regex compilation cached |
| Context keyword search | <5ms | Simple substring search |
| Risk classification | <2ms | Enum lookup + map access |
| Full detection pipeline | <10ms | Sequential checks with fast paths |

---

## Related ADRs

- **ADR-014**: Guardrails and Safety (InputGuard foundation)
- **ADR-048**: Comprehensive Security Strategy (overall approach)
- **ADR-007**: Database Security Model (complementary security layer)

---

## References

- [OWASP LLM Top 10 (2025)](https://owasp.org/www-project-top-10-for-large-language-model-applications/)
- [NIST AI Risk Management Framework](https://www.nist.gov/itl/ai-risk-management-framework)
- [ISO/IEC 42001:2023 - AI Management Systems](https://www.iso.org/standard/81230.html)
- [GDPR Article 4 - Definitions](https://gdpr-info.eu/art-4-gdpr/)
- [NIST SP 800-122 - PII Protection Guide](https://csrc.nist.gov/publications/detail/sp/800-122/final)

---

## Quick Fix Implementation

**Recommended: Option 3 (Business Format Exclusion)**

This is the safest quick fix that solves the immediate problem without full refactoring.

### Changes to InputGuard.java

```java
// Add near top of class (after existing patterns, line ~90)

/** Known business identifier formats (not PII) */
private static final Pattern BUSINESS_ID_PATTERN = Pattern.compile(
    // Order IDs: PREFIX/TYPE/NUMBER (e.g., AQV/PO/260213995)
    "[A-Z]{2,5}/[A-Z]{2}/\\d{6,12}" +
    "|" +
    // Invoice IDs: INV-NUMBER or INVNUMBER
    "INV-?\\d{6,12}" +
    "|" +
    // Reference: REF# NUMBER or REF NUMBER
    "REF[#\\s]?\\d{6,12}" +
    "|" +
    // Product codes: PROD + alphanumeric
    "PROD[A-Z0-9]{6,12}",
    Pattern.CASE_INSENSITIVE
);
```

```java
// Modify detectPII() method (line ~254), add at the beginning:

private PIIDetectionResult detectPII(String input) {
    List<String> types = new ArrayList<>();
    String result = input;

    // QUICK FIX: Exclude known business ID formats from PII detection
    if (BUSINESS_ID_PATTERN.matcher(input).find()) {
        // Input contains business identifiers, skip generic number checks
        // Still check for explicit PII like SSN, credit cards
        return detectExplicitPIIOnly(input);
    }

    // ... rest of existing code
}

/**
 * Detect only explicit PII patterns (SSN, CC, emails) - not generic numbers.
 * Used when input contains business identifiers.
 */
private PIIDetectionResult detectExplicitPIIOnly(String input) {
    List<String> types = new ArrayList<>();
    String result = input;

    // Check only patterns that don't rely on digit sequences
    if (SSN_PATTERN.matcher(result).find()) {
        types.add("SSN");
        result = SSN_PATTERN.matcher(result).replaceAll(maskChar.repeat(11));
    }

    if (CREDIT_CARD_PATTERN.matcher(result).find()) {
        types.add("Credit Card");
        result = maskCreditCard(result);
    }

    if (EMAIL_PATTERN.matcher(result).find()) {
        types.add("Email");
        result = maskEmail(result);
    }

    if (PHONE_PATTERN.matcher(result).find()) {
        types.add("Phone");
        result = maskPhone(result);
    }

    // Skip BANK_ACCOUNT_PATTERN and TAX_ID_PATTERN (too generic)

    return new PIIDetectionResult(!types.isEmpty(), types, result);
}
```

### Expected Results After Quick Fix

```
Input: "Find order AQV/PO/260213995"
Before: → "Find order AQV/PO/************" ❌
After:  → "Find order AQV/PO/260213995" ✅

Input: "Invoice INV-123456789"
Before: → "Invoice INV-************" ❌
After:  → "Invoice INV-123456789" ✅

Input: "My bank account is 260213995"
Before: → "My bank account is ************" ✅
After:  → "My bank account is 260213995" ⚠️ (Will pass - need context detection)
```

**Note:** This quick fix solves the false positive problem but doesn't add context detection. It's a pragmatic short-term solution. For full protection, implement the comprehensive solution (Phase 1-5).

### Testing the Quick Fix

```java
// Add to SecuritySanitizerTest.java or InputGuardTest.java

@Test
@DisplayName("Should allow business IDs through")
public void testBusinessIDsNotMasked() {
    InputGuard guard = new InputGuard();

    // Order IDs
    GuardResult result1 = guard.validate("Find order AQV/PO/260213995");
    assertEquals(GuardAction.PASS, result1.getAction());
    assertFalse(result1.getSanitizedInput().contains("*"));

    // Invoice IDs
    GuardResult result2 = guard.validate("Process invoice INV-123456789");
    assertEquals(GuardAction.PASS, result2.getAction());

    // Reference numbers
    GuardResult result3 = guard.validate("Check reference REF# 987654321");
    assertEquals(GuardAction.PASS, result3.getAction());
}

@Test
@DisplayName("Should still detect real PII even with business IDs")
public void testRealPIIStillDetected() {
    InputGuard guard = new InputGuard();

    // SSN should still be caught
    GuardResult result = guard.validate("Order AQV/PO/260213995 for SSN 123-45-6789");
    assertEquals(GuardAction.MASK, result.getAction());
    assertTrue(result.getViolations().contains("SSN"));
}
```

### Deployment

1. Apply changes to `InputGuard.java`
2. Run existing test suite: `./run-unit-tests.sh`
3. Add new tests above
4. Deploy to test environment
5. Verify order lookup works
6. Plan comprehensive solution implementation

**Effort:** 30 minutes
**Risk:** Low (only adds exclusion, doesn't change existing patterns)
