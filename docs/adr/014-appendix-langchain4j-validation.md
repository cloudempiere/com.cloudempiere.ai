# ADR-014 Appendix: LangChain4j Guardrails Validation

**Date:** 2025-12-03
**Related ADR:** [ADR-014](014-guardrails-and-safety.md)
**Purpose:** Validate ADR-014 guardrails design against LangChain4j's native implementation

---

## Executive Summary

| Aspect | ADR-014 Status | LangChain4j Alignment | Action Required |
|--------|---------------|----------------------|-----------------|
| **InputGuard** | ✅ Aligned | Native `InputGuardrail` interface | Minor refactor to interface |
| **OutputGuard** | ✅ Aligned | Native `OutputGuardrail` interface | Minor refactor to interface |
| **ExecutionGuard** | ⚠️ Custom | Not in LangChain4j | Keep custom implementation |
| **Risk Classification** | ⚠️ Custom | Not in LangChain4j | Keep custom implementation |
| **Guard Chaining** | ✅ Aligned | Native annotation-based chaining | Use `@InputGuardrails` |
| **Retry/Reprompt** | 🆕 Gap | Native in OutputGuardrail | Add to ADR-014 |
| **Built-in Guards** | 🆕 Gap | `MessageModeratorInputGuardrail`, `JsonExtractorOutputGuardrail` | Leverage built-ins |

**Overall Assessment:** ADR-014 is **~80% aligned** with LangChain4j. Requires interface alignment and addition of retry/reprompt capabilities.

---

## Detailed Comparison

### 1. InputGuardrail Interface

#### LangChain4j Implementation

```java
public interface InputGuardrail {
    // Simple validation
    InputGuardrailResult validate(UserMessage userMessage);

    // Complex validation with context
    InputGuardrailResult validate(InputGuardrailRequest params);
}
```

**InputGuardrailResult outcomes:**
| Outcome | Helper Method | Behavior |
|---------|---------------|----------|
| Success | `success()` | Proceed to next guardrail |
| Success with Alternate | `successWith(String)` | Rewrite message, proceed |
| Failure | `failure(String)` | Continue chain, accumulate errors |
| Fatal | `fatal(String)` | Halt immediately, throw exception |

#### ADR-014 Current Design

```java
// ADR-014 design (conceptual)
public class PIIDetectionGuard {
    public GuardResult validate(GuardContext context) {
        // returns pass/block
    }
}
```

#### Gap Analysis

| Feature | ADR-014 | LangChain4j | Gap |
|---------|---------|-------------|-----|
| Interface name | `Guard` | `InputGuardrail` | Rename |
| Return type | `GuardResult` | `InputGuardrailResult` | Align |
| Message rewrite | ❌ Not specified | ✅ `successWith(String)` | **Add** |
| Fatal vs non-fatal | ❌ Only block | ✅ `failure()` vs `fatal()` | **Add** |
| Context access | ✅ GuardContext | ✅ InputGuardrailRequest | Aligned |

#### Recommended Changes

```java
// Updated ADR-014 implementation
package com.cloudempiere.ai.guardrails;

import dev.langchain4j.service.guardrail.InputGuardrail;
import dev.langchain4j.service.guardrail.InputGuardrailResult;
import dev.langchain4j.data.message.UserMessage;

/**
 * PII Detection InputGuardrail for iDempiere AI
 * Masks or blocks PII in user input before LLM processing
 *
 * @see ADR-014 Guardrails and Safety
 */
public class PIIDetectionGuardrail implements InputGuardrail {

    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CC_PATTERN = Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String text = userMessage.singleText();

        // Check for SSN
        if (SSN_PATTERN.matcher(text).find()) {
            String masked = SSN_PATTERN.matcher(text).replaceAll("[SSN_REDACTED]");
            return InputGuardrailResult.successWith(masked); // Rewrite, don't block
        }

        // Check for credit card
        if (CC_PATTERN.matcher(text).find()) {
            return InputGuardrailResult.fatal("Credit card numbers not allowed in prompts");
        }

        // Check for email (mask, don't block)
        if (EMAIL_PATTERN.matcher(text).find()) {
            String masked = EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL_REDACTED]");
            return InputGuardrailResult.successWith(masked);
        }

        return InputGuardrailResult.success();
    }
}
```

---

### 2. OutputGuardrail Interface

#### LangChain4j Implementation

```java
public interface OutputGuardrail {
    // Simple validation
    OutputGuardrailResult validate(AiMessage responseFromLLM);

    // Complex validation with context
    OutputGuardrailResult validate(OutputGuardrailRequest params);
}
```

**OutputGuardrailResult outcomes:**
| Outcome | Helper Method | Behavior |
|---------|---------------|----------|
| Success | `success()` | Return response to caller |
| Success with Rewrite | `successWith(String)` | Rewrite output, re-run chain |
| Failure | `failure(String)` | Continue chain, accumulate errors |
| Fatal | `fatal(String)` | Halt, throw exception |
| **Retry** | `retry(String)` | Call LLM again with same prompt |
| **Reprompt** | `reprompt(String, String)` | Call LLM with new prompt appended |

#### ADR-014 Gap: Missing Retry/Reprompt

ADR-014's `OutputGuard` does not include retry or reprompt capabilities. This is a significant gap for:
- **Hallucination recovery** - Retry if fact-check fails
- **Format correction** - Reprompt if JSON invalid
- **Professionalism** - Reprompt if tone unprofessional

#### Recommended Addition

```java
/**
 * Hallucination Detection OutputGuardrail
 * Verifies LLM claims against database facts
 *
 * @see ADR-014 Guardrails and Safety
 */
public class HallucinationDetectionGuardrail implements OutputGuardrail {

    private final SecureDatabaseQueryExecutor queryExecutor;
    private static final int MAX_RETRIES = 3;

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String response = responseFromLLM.text();

        // Extract factual claims from response
        List<FactualClaim> claims = extractClaims(response);

        for (FactualClaim claim : claims) {
            boolean verified = verifyAgainstDatabase(claim);
            if (!verified) {
                // Reprompt with correction instruction
                return OutputGuardrailResult.reprompt(
                    "Unverified claim: " + claim.getText(),
                    "Please verify the following claim against actual data: " + claim.getText()
                );
            }
        }

        return OutputGuardrailResult.success();
    }

    private boolean verifyAgainstDatabase(FactualClaim claim) {
        // Query database to verify claim
        // Uses SecureDatabaseQueryExecutor (ADR-007)
        return queryExecutor.verifyFact(claim);
    }
}
```

---

### 3. Guard Chaining

#### LangChain4j Approach

```java
// Method 1: Builder
var assistant = AiServices.builder(IDempiereAgent.class)
    .chatModel(model)
    .inputGuardrailClasses(
        PIIDetectionGuardrail.class,
        PromptInjectionGuardrail.class,
        RateLimitGuardrail.class
    )
    .outputGuardrailClasses(
        HallucinationDetectionGuardrail.class,
        DataLeakageGuardrail.class
    )
    .build();

// Method 2: Annotations
@InputGuardrails({PIIDetectionGuardrail.class, PromptInjectionGuardrail.class})
@OutputGuardrails(value = HallucinationDetectionGuardrail.class, maxRetries = 5)
public interface IDempiereAgent {
    String chat(String message);

    @InputGuardrails(PromptInjectionGuardrail.class) // Override for specific method
    @OutputGuardrails(JsonOutputGuardrail.class)
    OrderSummary getOrderSummary(String orderId);
}
```

#### ADR-014 Current Design

```java
// ADR-014 manual chaining
public class GuardChain {
    private final List<Guard> guards = List.of(
        new PIIDetectionGuard(),
        new InjectionDetectionGuard(),
        new RateLimitGuard(),
        new ExecutionGuard(),
        new OutputValidationGuard(),
        new DataLeakageGuard()
    );
}
```

#### Recommendation

**Keep ADR-014's `GuardChain` concept** but implement using LangChain4j interfaces. The annotation-based approach is cleaner and provides:
- Method-level override capability
- Built-in retry configuration
- Automatic exception handling

---

### 4. ExecutionGuard (ADR-014 Custom)

**Not present in LangChain4j.** This is iDempiere-specific and should remain custom.

#### Keep As-Is

```java
/**
 * Execution Guard - Risk Classification for iDempiere Actions
 * This is NOT part of LangChain4j standard - custom implementation
 *
 * @see ADR-014 Guardrails and Safety
 */
public class ExecutionGuard {

    public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

    public RiskLevel classifyAction(ERPAction action) {
        // Read queries → LOW
        if (action.isReadOnly()) return RiskLevel.LOW;

        // Create draft → MEDIUM
        if (action.isDraftDocument()) return RiskLevel.MEDIUM;

        // Amount thresholds → HIGH
        if (action.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            return RiskLevel.HIGH;
        }

        // Bulk operations → CRITICAL
        if (action.getRecordCount() > 10) return RiskLevel.CRITICAL;

        return RiskLevel.MEDIUM;
    }

    public ExecutionResult route(ERPAction action) {
        RiskLevel risk = classifyAction(action);

        switch (risk) {
            case LOW:
                return ExecutionResult.autoExecute();
            case MEDIUM:
                return ExecutionResult.executeAndNotify();
            case HIGH:
                return ExecutionResult.requireApproval();
            case CRITICAL:
                return ExecutionResult.requireDualApproval();
            default:
                return ExecutionResult.requireApproval();
        }
    }
}
```

This integrates with `@Tool` methods but operates **after** LangChain4j guardrails, before actual ERP execution.

---

### 5. Built-in Guardrails to Leverage

LangChain4j provides built-in guardrails we should use:

#### MessageModeratorInputGuardrail

```java
// Uses ModerationModel for content moderation
// Available in: dev.langchain4j.service.guardrail.internal

// Configuration
AiServices.builder(IDempiereAgent.class)
    .chatModel(chatModel)
    .moderationModel(OpenAiModerationModel.builder()
        .apiKey(apiKey)
        .build())
    .inputGuardrailClasses(MessageModeratorInputGuardrail.class)
    .build();
```

#### JsonExtractorOutputGuardrail

```java
// Validates JSON output and reprompts if invalid
// Useful for structured outputs like OrderSummary, InvoiceData

@OutputGuardrails(JsonExtractorOutputGuardrail.class)
OrderSummary getOrderSummary(String orderId);
```

---

## Updated Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    GUARDRAILS PIPELINE (Updated for LangChain4j)                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  User Input                                                                     │
│       │                                                                         │
│       ▼                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  INPUT GUARDRAILS (implements InputGuardrail)                           │   │
│  │  Declared via @InputGuardrails or AiServices.builder()                  │   │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │   │
│  │  │ PIIDetection     │  │ PromptInjection  │  │ RateLimit        │      │   │
│  │  │ Guardrail        │  │ Guardrail        │  │ Guardrail        │      │   │
│  │  │                  │  │                  │  │                  │      │   │
│  │  │ Results:         │  │ Results:         │  │ Results:         │      │   │
│  │  │ - success()      │  │ - success()      │  │ - success()      │      │   │
│  │  │ - successWith()  │→ │ - failure()      │→ │ - fatal()        │      │   │
│  │  │ - failure()      │  │ - fatal()        │  │                  │      │   │
│  │  │ - fatal()        │  │                  │  │                  │      │   │
│  │  └──────────────────┘  └──────────────────┘  └──────────────────┘      │   │
│  └───────────────────────────────┬─────────────────────────────────────────┘   │
│                                  │                                             │
│                                  ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  LLM INVOCATION (LangChain4j AiServices)                                │   │
│  │  - Chat completion                                                      │   │
│  │  - Tool/function calls (ERPTools)                                       │   │
│  └───────────────────────────────┬─────────────────────────────────────────┘   │
│                                  │                                             │
│                                  ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  EXECUTION GUARD (iDempiere Custom - NOT LangChain4j)                   │   │
│  │  Invoked by @Tool methods before ERP operations                         │   │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │   │
│  │  │ ActionRiskClassifier                                             │  │   │
│  │  │ LOW      → Auto-execute via SecureDatabaseQueryExecutor          │  │   │
│  │  │ MEDIUM   → Execute + Notify via AIG_UsageMetrics                 │  │   │
│  │  │ HIGH     → Route to AIG_ApprovalRequest (single approval)        │  │   │
│  │  │ CRITICAL → Route to AIG_ApprovalRequest (dual approval)          │  │   │
│  │  └──────────────────────────────────────────────────────────────────┘  │   │
│  └───────────────────────────────┬─────────────────────────────────────────┘   │
│                                  │                                             │
│                                  ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  OUTPUT GUARDRAILS (implements OutputGuardrail)                         │   │
│  │  Declared via @OutputGuardrails(maxRetries = N)                         │   │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │   │
│  │  │ Hallucination    │  │ DataLeakage      │  │ JsonExtractor    │      │   │
│  │  │ Guardrail        │  │ Guardrail        │  │ Guardrail        │      │   │
│  │  │                  │  │                  │  │ (built-in)       │      │   │
│  │  │ Results:         │  │ Results:         │  │                  │      │   │
│  │  │ - success()      │  │ - success()      │  │ Results:         │      │   │
│  │  │ - successWith()  │→ │ - failure()      │→ │ - retry()        │      │   │
│  │  │ - retry()        │  │ - fatal()        │  │ - reprompt()     │      │   │
│  │  │ - reprompt()     │  │                  │  │                  │      │   │
│  │  └──────────────────┘  └──────────────────┘  └──────────────────┘      │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Implementation Checklist

### Phase 1: Interface Alignment (2 days)

- [ ] Rename `Guard` → implement `InputGuardrail` / `OutputGuardrail`
- [ ] Update `GuardResult` → `InputGuardrailResult` / `OutputGuardrailResult`
- [ ] Add `successWith(String)` message rewriting capability
- [ ] Add `failure()` vs `fatal()` distinction

### Phase 2: Add Retry/Reprompt (2 days)

- [ ] Implement `retry(String)` in OutputGuardrails
- [ ] Implement `reprompt(String, String)` for correction prompts
- [ ] Configure `maxRetries` per guardrail (default: 3)
- [ ] Add retry logging to `AIG_UsageMetrics`

### Phase 3: Annotation Integration (1 day)

- [ ] Add `@InputGuardrails` to `IDempiereAgent` interface
- [ ] Add `@OutputGuardrails` with `maxRetries` configuration
- [ ] Support method-level override for specific operations

### Phase 4: Built-in Guardrails (1 day)

- [ ] Integrate `MessageModeratorInputGuardrail` (if using OpenAI moderation)
- [ ] Use `JsonExtractorOutputGuardrail` for structured outputs
- [ ] Document which built-ins vs custom guardrails to use

### Phase 5: Keep ExecutionGuard Custom (0 days)

- [ ] ExecutionGuard remains iDempiere-specific
- [ ] Invoke from `@Tool` methods, not via LangChain4j annotation
- [ ] Integrate with `AIG_ApprovalRequest` workflow

---

## Updated ADR-014 Components

| ADR-014 Component | LangChain4j Equivalent | Implementation |
|-------------------|----------------------|----------------|
| `PIIDetectionGuard` | `implements InputGuardrail` | Use `successWith()` to mask |
| `InjectionDetectionGuard` | `implements InputGuardrail` | Use `fatal()` to block |
| `RateLimitGuard` | `implements InputGuardrail` | Use `failure()` to throttle |
| `ExecutionGuard` | **None (custom)** | Keep as-is, invoke from `@Tool` |
| `ActionRiskClassifier` | **None (custom)** | Keep as-is |
| `ApprovalWorkflow` | **None (custom)** | Keep as-is, uses `AIG_ApprovalRequest` |
| `OutputValidationGuard` | `implements OutputGuardrail` | Use `retry()` / `reprompt()` |
| `HallucinationDetector` | `implements OutputGuardrail` | Use `reprompt()` for fact-check |
| `DataLeakageGuard` | `implements OutputGuardrail` | Use `fatal()` for leaks |

---

## References

### LangChain4j Documentation
- [LangChain4j Guardrails Tutorial](https://docs.langchain4j.dev/tutorials/guardrails/)
- [Quarkus LangChain4j Guardrails](https://docs.quarkiverse.io/quarkus-langchain4j/dev/guardrails.html)

### Implementation Examples
- [Spring Boot Guardrails Demo](https://bazlur.ca/2025/06/21/building-robust-ai-applications-with-langchain4j-guardrails-and-spring-boot/)
- [Guardrails Demo GitHub](https://github.com/rokon12/guardrails-demo)

### Feature Requests
- [LangChain4j Common Guardrails Library Issue #3248](https://github.com/langchain4j/langchain4j/issues/3248)

---

**Validation Date:** 2025-12-03
**LangChain4j Version:** 1.0.0-beta3 / 1.1.0
**Status:** ADR-014 requires minor updates for full alignment
