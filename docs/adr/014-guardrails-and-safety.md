# ADR-014: Guardrails and Safety

<!-- MADR 3.0 Template -->

## Status

Accepted

## Date

2025-12-01

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

AI agents that can execute actions in ERP systems (create orders, process payments, modify data) pose significant operational risks without proper safeguards. Currently, our implementation has only SQL injection prevention in `SecureDatabaseQueryExecutor`. We lack input validation for prompt injection, output validation for hallucinations, execution controls for high-risk actions, and human-in-the-loop workflows for critical operations. Without comprehensive guardrails, the system cannot safely operate in production.

## Decision Drivers

- **Data Integrity**: Prevent AI from making incorrect or unauthorized modifications
- **Financial Risk**: Control monetary transactions and high-value operations
- **Compliance**: GDPR requires data protection, SOX requires audit controls
- **User Trust**: Users must trust AI actions before adoption
- **Production Readiness**: Enterprise systems require fail-safes

## Considered Options

1. **LangChain4j Guards + Custom Risk Classification**
2. **External Guardrails Platform (Guardrails AI, NeMo)**
3. **Approval-Only Mode (All actions require human approval)**

## Decision Outcome

**Chosen option:** "LangChain4j Guards + Custom Risk Classification", because it provides fine-grained control over risk levels, integrates with our existing LangChain4j architecture, keeps approval workflows within iDempiere's existing document workflow system, and allows progressive automation as trust builds.

### Confirmation

The decision will be confirmed when:
- [ ] `InputGuard` blocks prompt injection attempts in test suite
- [ ] `OutputGuard` detects and flags potential hallucinations
- [ ] `ExecutionGuard` enforces risk-based routing (LOW/MEDIUM/HIGH/CRITICAL)
- [ ] `AIG_ApprovalRequest` table receives records for HIGH-risk actions
- [ ] Unit tests verify guard chains execute in order
- [ ] Integration test: Order >$10,000 triggers approval workflow

## Pros and Cons of the Options

### Option 1: LangChain4j Guards + Custom Risk Classification

Native guard implementation with iDempiere workflow integration.

- Good, because integrates with existing LangChain4j agent architecture
- Good, because risk levels can be tuned per-client/per-role
- Good, because approval workflows use iDempiere's existing AD_WF_Process
- Good, because progressive automation (start strict, relax with trust)
- Neutral, because requires custom implementation of guard logic
- Bad, because no pre-built guardrail templates

### Option 2: External Guardrails Platform (Guardrails AI, NeMo)

Use SaaS platform with pre-built validation rules.

- Good, because pre-built validators for common patterns
- Good, because community-maintained rule sets
- Good, because reduces development effort
- Bad, because external API calls add latency (50-200ms per check)
- Bad, because data leaves iDempiere (compliance concern)
- Bad, because subscription cost ($200-1000/month)
- Bad, because limited customization for ERP-specific rules

### Option 3: Approval-Only Mode

All AI actions require human approval before execution.

- Good, because maximum safety (no autonomous actions)
- Good, because simplest implementation
- Good, because clear audit trail
- Bad, because defeats purpose of AI automation
- Bad, because poor user experience (approval fatigue)
- Bad, because not scalable for high-volume operations

## More Information

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Guardrails Pipeline                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  User Input                                                 │
│       │                                                     │
│       ▼                                                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  INPUT GUARDS (pre-processing)                      │   │
│  │  ┌──────────────┐  ┌──────────────┐                │   │
│  │  │ PIIDetector  │  │ Injection    │                │   │
│  │  │ - SSN, CC#   │  │ Detector     │                │   │
│  │  │ - Email, ID  │  │ - SQL inject │                │   │
│  │  │ → Mask/Block │  │ - Prompt inj │                │   │
│  │  └──────────────┘  └──────────────┘                │   │
│  └───────────────────────────┬─────────────────────────┘   │
│                              │                              │
│                              ▼                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  EXECUTION GUARDS (action control)                  │   │
│  │  ┌──────────────────────────────────────────────┐  │   │
│  │  │ ActionRiskClassifier                         │  │   │
│  │  │ LOW    → Auto-execute                        │  │   │
│  │  │ MEDIUM → Execute + Notify                    │  │   │
│  │  │ HIGH   → Require Single Approval             │  │   │
│  │  │ CRITICAL → Require Dual Approval             │  │   │
│  │  └──────────────────────────────────────────────┘  │   │
│  └───────────────────────────┬─────────────────────────┘   │
│                              │                              │
│                              ▼                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  OUTPUT GUARDS (post-processing)                    │   │
│  │  ┌──────────────┐  ┌──────────────┐                │   │
│  │  │ Hallucination│  │ DataLeakage  │                │   │
│  │  │ Detector     │  │ Detector     │                │   │
│  │  │ - Fact check │  │ - PII in out │                │   │
│  │  │ - Confidence │  │ - Credentials│                │   │
│  │  └──────────────┘  └──────────────┘                │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| `InputGuard` | PII masking, injection prevention | `guardrails/` |
| `OutputGuard` | Hallucination detection, data leakage | `guardrails/` |
| `ExecutionGuard` | Risk classification, approval routing | `guardrails/` |
| `ActionRiskClassifier` | Determine risk level per action | `guardrails/` |
| `ApprovalWorkflow` | Human-in-the-loop integration | `guardrails/` |
| `AIG_ApprovalRequest` | Pending approvals storage | Database |

### Risk Classification Rules

| Action Type | Default Risk | Escalation Triggers |
|-------------|--------------|---------------------|
| Read queries | LOW | None |
| Create draft documents | MEDIUM | New customer, unusual quantity |
| Update existing records | MEDIUM | Price change >20%, bulk update |
| Complete/process documents | HIGH | Amount >$1,000, credit limit |
| Delete records | HIGH | Always |
| Financial transactions | HIGH | Amount >$100 |
| Bulk operations (>10 records) | CRITICAL | Always |
| System configuration | CRITICAL | Always |

### Guard Execution Order

```java
public class GuardChain {
    private final List<Guard> guards = List.of(
        new PIIDetectionGuard(),      // 1. Mask sensitive input
        new InjectionDetectionGuard(), // 2. Block malicious input
        new RateLimitGuard(),          // 3. Enforce rate limits
        new ExecutionGuard(),          // 4. Route by risk level
        new OutputValidationGuard(),   // 5. Validate response
        new DataLeakageGuard()         // 6. Prevent data exposure
    );

    public GuardResult process(GuardContext context) {
        for (Guard guard : guards) {
            GuardResult result = guard.validate(context);
            if (result.isBlocked()) {
                return result; // Stop chain on block
            }
            context = result.getModifiedContext();
        }
        return GuardResult.pass(context);
    }
}
```

### Implementation Notes

**Phase 1 (3 days):** InputGuard + PIIDetector
- Regex patterns for SSN, credit cards, emails
- Prompt injection detection (ignore/disregard patterns)
- Mask or reject based on configuration

**Phase 2 (3 days):** ExecutionGuard + ActionRiskClassifier
- Risk level enum and assessment logic
- Per-action-type default risk levels
- Configurable thresholds per AD_Client_ID

**Phase 3 (3 days):** ApprovalWorkflow + AIG_ApprovalRequest
- Integration with iDempiere AD_WF_Process
- Approval notification (email, dashboard)
- Timeout handling (auto-reject after 24h)

**Phase 4 (3 days):** OutputGuard + HallucinationDetector
- Confidence scoring for generated responses
- Fact-checking against database queries
- Data leakage scanning (PII in output)

**Total Effort:** 12 days

### Related ADRs

- [ADR-007](007-database-security-model.md) - SQL security complements execution guards
- [ADR-009](009-domain-boundaries-agent-scope.md) - Domain boundaries define guard scope
- [ADR-013](013-observability-cost-tracking.md) - Guard violations should be logged as metrics

### References

- [BCG - Agentic AI](https://www.bcg.com/publications/2025/how-agentic-ai-is-transforming-enterprise-platforms)
- [HBR - Designing Agentic AI](https://hbr.org/2025/10/designing-a-successful-agentic-ai-system)
- [AI_MCP_ERP_BEST_PRACTICES.md](../AI_MCP_ERP_BEST_PRACTICES.md) Section 6
- [OWASP LLM Top 10](https://owasp.org/www-project-top-10-for-large-language-model-applications/)
- [Guardrails AI](https://www.guardrailsai.com/)
