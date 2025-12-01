# Priority Revision: ADRs and Implementation Gaps

**Date:** 2025-12-01
**Based on:** AI_MCP_ERP_BEST_PRACTICES.md analysis + Code Coverage Gap Analysis
**Status:** APPROVED - Implementation Ready

---

## Executive Summary

After analyzing our current ADRs and implementation against best practices and actual code coverage, I've identified:

- **3 Missing ADRs** - COMPLETED (ADR-013, 014, 015 created)
- **4 ADRs Needing Updates** (Incomplete coverage)
- **7 Code Implementation Gaps** (High priority)
- **~1,930 lines deprecated code** to remove (24% of codebase)
- **2 Critical Conflicts** requiring immediate decisions
- **Revised Priority Order** (Based on industry best practices)

---

## Part 1: ADR Gap Analysis

### Current ADRs (12 total)

| ADR | Title | Status | Best Practice Coverage |
|-----|-------|--------|----------------------|
| 001 | Initial Architecture | ✅ Accepted | ✅ Complete |
| 002 | LangChain4j Strategic Adoption | ✅ Accepted | ✅ Complete |
| 003 | MCP Server Integration | ✅ Accepted | ⚠️ Needs Update (security details) |
| 004 | Java Agent Framework | ✅ Accepted | ✅ Complete |
| 005 | Intelligent Routing | ⚠️ Superseded | → ADR-012 |
| 006 | Data Model Architecture | ✅ Accepted | ⚠️ Needs Update (audit tables) |
| 007 | Database Security Model | ✅ Accepted | ✅ Complete |
| 008 | LLM Instruction Following | ✅ Accepted | ✅ Complete |
| 009 | Domain Boundaries | ✅ Accepted | ⚠️ Needs Update (guardrails) |
| 010 | Agent Orchestration | ✅ Accepted | ⚠️ Needs Update (agentic patterns) |
| 011 | Specialized Agent Scopes | ✅ Accepted | ✅ Complete |
| 012 | RAG-Based Context Retrieval | ✅ Accepted | ✅ Complete |

### Missing ADRs (Critical Gaps)

#### ADR-013: Observability and Cost Tracking (MISSING)

**Best Practice Source:** Section 8 of AI_MCP_ERP_BEST_PRACTICES.md

**Gap:** No ADR covers LLM observability, cost tracking, or budget controls.

**Should Cover:**
- Token usage monitoring
- Cost attribution (per-user, per-team, per-feature)
- Budget caps and rate limiting
- Latency tracking
- Quality metrics (hallucination detection, accuracy)
- Audit trail for compliance

**Implementation Impact:**
- New: `AIMetricsListener` class
- New: `AIG_UsageMetrics` table
- New: `CostGuard` budget enforcement
- Integration: LangChain4j `ChatModelListener`

**Priority:** 🔴 HIGH (Required before production)

---

#### ADR-014: Guardrails and Safety (MISSING)

**Best Practice Source:** Section 6 of AI_MCP_ERP_BEST_PRACTICES.md

**Gap:** ADR-009 mentions boundaries but doesn't specify guardrails architecture.

**Should Cover:**
- Input guards (PII detection, prompt injection prevention)
- Execution guards (monetary thresholds, rate limits)
- Output guards (hallucination detection, sensitive data leakage)
- Human-in-the-loop patterns (approval workflows)
- Critic agents (validation before action)

**Implementation Impact:**
- New: `InputGuard`, `OutputGuard`, `ExecutionGuard` classes
- New: `ApprovalWorkflow` for high-risk actions
- Update: ERPTools with risk classification
- Integration: LangChain4j Guards pattern

**Priority:** 🔴 HIGH (Security critical)

---

#### ADR-015: Conversational UX Patterns (MISSING)

**Best Practice Source:** Section 9 of AI_MCP_ERP_BEST_PRACTICES.md

**Gap:** No ADR covers conversation design patterns for ERP context.

**Should Cover:**
- Parameter chain pattern (collect info in any order)
- Acknowledgment pattern (confirm understanding)
- Message length guidelines (3 lines max)
- Zoom link integration
- Proactive insights (e.g., "This order requires approval")
- Multi-turn conversation management

**Implementation Impact:**
- Update: `AIChatWidget` conversation flow
- New: `ConversationFlowManager` class
- New: `ParameterChainHandler` for multi-step inputs
- Update: Response formatting guidelines

**Priority:** 🟡 MEDIUM (UX improvement)

---

### ADRs Needing Updates

#### ADR-003: MCP Server Integration

**Current State:** Architecture design only
**Missing (per best practices):**
- Authentication integration details
- Rate limiting implementation
- Error message format (actionable)
- Tool namespacing convention
- Versioning strategy

**Update Scope:** Add implementation details section

---

#### ADR-006: Data Model Architecture

**Current State:** Basic table definitions
**Missing (per best practices):**
- `AIG_UsageMetrics` table (cost tracking)
- `AIG_AuditLog` table (compliance)
- `AIG_Embedding` table (RAG vectors)
- `AIG_ApprovalRequest` table (human-in-the-loop)

**Update Scope:** Add observability and approval tables

---

#### ADR-009: Domain Boundaries and Agent Scope

**Current State:** Domain separation only
**Missing (per best practices):**
- Guardrails per domain
- Risk classification per action
- Escalation paths
- Mission owner pattern

**Update Scope:** Add guardrails section, integrate ADR-014

---

#### ADR-010: Agent Orchestration Architecture

**Current State:** Sequential/parallel patterns
**Missing (per best practices):**
- `langchain4j-agentic-patterns` module integration
- Critic agent pattern
- State persistence (LangGraph style)
- Multi-agent collaboration (A2A)

**Update Scope:** Add agentic patterns section

---

## Part 2: Code Implementation Gaps

### Current Implementation (82 Java files)

| Package | Files | Status |
|---------|-------|--------|
| `agent/` | 8 | ⚠️ Legacy (BaseAgent, custom AgentFactory) |
| `agent/langchain4j/` | 6 | ✅ Current (LangChain4j integration) |
| `context/` | 5 | ⚠️ To be replaced by RAG |
| `database/` | 3 | ✅ Good (SecureDatabaseQueryExecutor) |
| `function/` | 1 | ⚠️ Legacy (AIDatabaseFunctionHandler) |
| `model/` | 12 | ✅ Good |
| `provider/` | 15 | ⚠️ Partly legacy (custom DTOs) |
| `provider/langchain4j/` | 5 | ✅ Current |
| `routing/` | 11 | ❌ To be removed (ADR-012) |
| `service/` | 1 | ⚠️ Uses legacy routing |
| `tool/` | 8 | ⚠️ Legacy (custom ITool interface) |

### Critical Implementation Gaps

#### Gap 1: No Observability (Priority: 🔴 HIGH)

**Best Practice:** Section 8 - 5 pillars of LLM observability

**Current State:** Zero monitoring

**Missing:**
```java
// No equivalent exists
public class AIMetricsListener implements ChatModelListener {
    void onRequest(ChatModelRequest request);
    void onResponse(ChatModelResponse response);
    void onError(ChatModelErrorContext context);
}
```

**Implementation Required:**
- `AIMetricsListener.java` - Token/cost/latency tracking
- `CostGuard.java` - Budget enforcement
- `AIG_UsageMetrics` table - Persistence
- Dashboard integration

**Effort:** 2-3 days

---

#### Gap 2: No Guardrails (Priority: 🔴 HIGH)

**Best Practice:** Section 6 - Input/Output/Execution guards

**Current State:** Only SQL injection prevention in `SecureDatabaseQueryExecutor`

**Missing:**
```java
// No equivalent exists
public interface Guard {
    GuardResult validate(GuardContext context);
}

public class InputGuard implements Guard {
    // PII detection, prompt injection
}

public class OutputGuard implements Guard {
    // Sensitive data leakage, hallucination detection
}

public class ExecutionGuard implements Guard {
    // Monetary thresholds, rate limits
}
```

**Implementation Required:**
- `InputGuard.java` - PII masking, injection detection
- `OutputGuard.java` - Response validation
- `ExecutionGuard.java` - Action risk classification
- `ApprovalWorkflow.java` - Human-in-the-loop

**Effort:** 5-7 days

---

#### Gap 3: Tool Naming Non-Compliant (Priority: 🟡 MEDIUM)

**Best Practice:** Section 4.2 - Namespacing with prefixes

**Current State:** Generic names
```java
// Current (non-compliant)
@Tool("Execute a read-only SQL SELECT query...")
public String queryDatabase(...)

@Tool("Look up a specific record by ID...")
public String lookupRecord(...)
```

**Should Be:**
```java
// Best practice (namespaced)
@Tool("erp_database_query: Execute a read-only SQL SELECT...")
public String erp_database_query(...)

@Tool("erp_record_lookup: Look up a specific record...")
public String erp_record_lookup(...)
```

**Implementation Required:**
- Rename all @Tool methods with `erp_` prefix
- Update tool descriptions for clarity
- Add response format parameter

**Effort:** 1 day

---

#### Gap 4: No Human-in-the-Loop (Priority: 🔴 HIGH)

**Best Practice:** Section 6.2 - Approval workflows for high-risk actions

**Current State:** All actions auto-execute

**Missing:**
```java
// No equivalent exists
public enum ActionRiskLevel {
    LOW,      // Auto-execute
    MEDIUM,   // Execute + notify
    HIGH,     // Require approval
    CRITICAL  // Require dual approval
}

public class ApprovalWorkflow {
    public ApprovalResult requestApproval(ActionRequest request);
    public void processApproval(int requestId, boolean approved, int approverId);
}
```

**Implementation Required:**
- `ActionRiskClassifier.java` - Risk assessment
- `ApprovalWorkflow.java` - Approval management
- `AIG_ApprovalRequest` table - Persistence
- UI integration for approval notifications

**Effort:** 5-7 days

---

#### Gap 5: Legacy Code Not Removed (Priority: 🟡 MEDIUM)

**Best Practice:** ADR-002 specifies deprecation path

**Current State:** Dual systems running

**To Remove (per ADR-002 and ADR-012):**
```
routing/ (11 files, 630 lines) - Replaced by RAG
├── PromptAnalyzer.java
├── EntityExtractor.java
├── ConversationContextManager.java
├── SourceDecision.java
├── ContextEntry.java
├── ContextMatch.java
├── DataType.java
├── DbQueryParams.java
├── RoutingMetrics.java
├── TTLConfig.java
└── (various)

tool/ (8 files) - Replaced by ERPTools
├── ITool.java
├── ToolRegistry.java
├── impl/DatabaseQueryTool.java
├── impl/TableMetadataTool.java
├── impl/FieldMetadataTool.java
└── (various)

function/ (1 file) - Replaced by ERPTools
└── AIDatabaseFunctionHandler.java
```

**Implementation Required:**
- Complete RAG migration first
- Remove routing package
- Remove legacy tool package
- Update AIConversationService to use LangChain4j only

**Effort:** 3-5 days (after RAG migration)

---

## Part 3: Revised Priority Order

### Current Roadmap (from PROJECT.md)

```
v0.10.0: RAG Migration, Structured Outputs, Observability
v0.11.0: Instruction Following, Domain Agents
v0.12.0: Production Preparation
v1.0.0: Production Release
```

### Revised Roadmap (Based on Best Practices)

**Rationale:** Security and observability should come before feature expansion.

```
v0.10.0: Foundation Hardening (6 weeks)
├── Week 1-2: Observability (ADR-013)
│   ├── AIMetricsListener (token/cost/latency)
│   ├── CostGuard (budget controls)
│   └── AIG_UsageMetrics table
├── Week 3-4: Guardrails (ADR-014)
│   ├── InputGuard (PII, injection)
│   ├── OutputGuard (leakage, hallucination)
│   └── ExecutionGuard (risk classification)
├── Week 5-6: Tool Compliance
│   ├── Rename tools with erp_ prefix
│   ├── Add response format parameters
│   └── Update error messages (actionable)
└── Deliverables:
    ├── ADR-013: Observability
    ├── ADR-014: Guardrails
    └── 4 new classes, 1 new table

v0.11.0: RAG & Cleanup (4 weeks)
├── Week 1-2: RAG Migration (ADR-012)
│   ├── EmbeddingStore setup
│   ├── ContentRetriever integration
│   └── Feature flag testing
├── Week 3-4: Legacy Removal
│   ├── Remove routing/ package (630 lines)
│   ├── Remove tool/ package (8 files)
│   └── Update AIConversationService
└── Deliverables:
    ├── -630 lines of code
    └── Clean LangChain4j architecture

v0.12.0: Human-in-the-Loop (4 weeks)
├── Week 1-2: Approval Workflow
│   ├── ActionRiskClassifier
│   ├── ApprovalWorkflow
│   └── AIG_ApprovalRequest table
├── Week 3-4: UX Patterns (ADR-015)
│   ├── ParameterChainHandler
│   ├── ConversationFlowManager
│   └── Response formatting
└── Deliverables:
    ├── ADR-015: Conversational UX
    └── Human-in-the-loop for high-risk actions

v0.13.0: Domain Agents (4 weeks)
├── Structured outputs
├── InventoryAgent, SalesAgent, PurchasingAgent
├── Domain-specific guardrails
└── Mission owner pattern

v1.0.0: Production Release (4 weeks)
├── ADR updates (003, 006, 009, 010)
├── Complete documentation
├── Performance benchmarks
└── Security audit
```

---

## Part 4: Action Items

### Immediate (This Week)

| # | Action | Owner | Effort | Status |
|---|--------|-------|--------|--------|
| 1 | Create ADR-013: Observability and Cost Tracking | Dev | 2h | DONE |
| 2 | Create ADR-014: Guardrails and Safety | Dev | 2h | DONE |
| 3 | Create ADR-015: Conversational UX Patterns | Dev | 2h | DONE |
| 4 | Update ADR-003: Add MCP security details | Dev | 1h | Pending |
| 5 | Implement AIMetricsListener | Dev | 1d | Pending |
| 6 | Implement CostGuard | Dev | 1d | Pending |

### Short-term (2 weeks)

| # | Action | Owner | Effort |
|---|--------|-------|--------|
| 6 | Implement InputGuard, OutputGuard | Dev | 3d |
| 7 | Rename tools with erp_ prefix | Dev | 0.5d |
| 8 | Create AIG_UsageMetrics table | Dev | 0.5d |
| 9 | Update tool error messages | Dev | 0.5d |
| 10 | Add response format parameters | Dev | 0.5d |

### Medium-term (1 month)

| # | Action | Owner | Effort |
|---|--------|-------|--------|
| 11 | Complete RAG migration | Dev | 1w |
| 12 | Remove routing/ package | Dev | 1d |
| 13 | Remove tool/ package | Dev | 1d |
| 14 | Implement ApprovalWorkflow | Dev | 1w |

---

## Part 5: Critical Conflicts (NEW - from Code Gap Analysis)

### Conflict 1: ADR-005 vs ADR-012 (CRITICAL) - RESOLVED

**Issue:** ADR-005 marked "Superseded by ADR-012" but routing code still actively used

**Evidence:**
- `routing/` package (630 lines) exists and is imported in `AIConversationService.java`
- No `RAGContextManager.java` exists (ADR-012 specifies this)
- `AIConversationService` imports: `routing.ConversationContextManager`, `routing.PromptAnalyzer`

**Decision:**
- [x] **Option A:** Complete ADR-012 migration, delete all ADR-005 code ✅ **APPROVED**
- [ ] ~~Option B: Revert ADR-005 status to "Accepted", defer ADR-012~~

**Action Plan:**
1. Implement `RAGContextManager.java` per ADR-012 specification
2. Update `AIConversationService` to use LangChain4j ContentRetriever
3. Delete `routing/` package (10 files, 630 lines)
4. Target: Sprint 1, Week 1-2

---

### Conflict 2: Dual Agent Systems (HIGH) - RESOLVED

**Issue:** Two competing agent architectures exist in codebase

| System | Files | Lines | Status |
|--------|-------|-------|--------|
| Legacy (BaseAgent) | 7 files | ~500 | Deprecated per ADR-002 |
| Current (LangChain4j) | 6 files | ~600 | Active |

**Evidence:**
```
AIConversationService.java mixes both:
- import com.cloudempiere.ai.routing.ConversationContextManager  // Legacy
- import com.cloudempiere.ai.agent.BaseAgent                    // Legacy
- import com.cloudempiere.ai.provider.langchain4j.IDempiereAgent // Current
```

**Decision:**
- [x] Remove BaseAgent and all legacy agent code ✅ **APPROVED**
- [x] Update AIConversationService to use LangChain4j only ✅ **APPROVED**

**Action Plan:**
1. Update `AIConversationService` to remove legacy imports
2. Delete legacy agent files:
   - `agent/BaseAgent.java`
   - `agent/IAIAgent.java`
   - `agent/AgentContext.java`
   - `agent/AgentException.java`
   - `agent/AgentFactory.java`
   - `agent/AgentMessage.java`
   - `agent/AgentResponse.java`
3. Target: Sprint 2, after RAG migration

---

### Conflict 3: Tool System Undocumented (MEDIUM) - RESOLVED

**Issue:** 400 lines of tool code with no ADR coverage

**Files:**
- `tool/ITool.java`, `tool/ToolRegistry.java`
- `tool/BoundaryValidator.java`, `tool/ToolParameter.java`
- `tool/impl/DatabaseQueryTool.java`, etc.

**Decision:**
- [ ] ~~Option A: Create ADR-016 for Tool System Architecture~~
- [x] **Option B:** Remove legacy tool package (replaced by ERPTools @Tool annotations) ✅ **APPROVED**

**Action Plan:**
1. Verify ERPTools covers all use cases from legacy tools
2. Delete legacy tool files:
   - `tool/ITool.java`
   - `tool/ToolRegistry.java`
   - `tool/BoundaryValidator.java`
   - `tool/ToolParameter.java`
   - `tool/ToolPermission.java`
   - `tool/ToolExecutionException.java`
   - `tool/impl/DatabaseQueryTool.java`
   - `tool/impl/FieldMetadataTool.java`
   - `tool/impl/TableMetadataTool.java`
3. Target: Sprint 2, after RAG migration

---

## Part 6: Code Coverage Summary

### ADR Implementation Status

| Status | Count | ADRs |
|--------|-------|------|
| **Fully Implemented** | 5 | 001, 004, 006, 007, 008 |
| **Partially Implemented** | 3 | 002 (60%), 009 (30%), 010 (40%) |
| **Not Implemented** | 6 | 003, 011, 012, 013, 014, 015 |

### Deprecated Code to Remove

| Package | Files | Lines | Blocked By |
|---------|-------|-------|------------|
| `routing/` | 10 | 630 | ADR-012 completion |
| `agent/` (legacy) | 7 | 500 | Decision on Conflict 2 |
| `tool/` | 9 | 400 | Decision on Conflict 3 |
| `provider/impl/` | 2 | 400 | ADR-002 cleanup |
| **Total** | **28** | **1,930** | |

### Files Coverage by Package

| Package | Files | Documented | Deprecated | Gap |
|---------|-------|------------|------------|-----|
| provider/ | 20 | 16 (80%) | 2 | 2 undocumented |
| agent/ | 13 | 0 | 6 | 7 undocumented |
| database/ | 3 | 3 (100%) | 0 | None |
| model/ | 12 | 12 (100%) | 0 | None |
| context/ | 5 | 5 (100%) | 0 | None |
| routing/ | 10 | 0 | 10 | All deprecated |
| tool/ | 9 | 0 | 9 | All deprecated |
| service/ | 2 | 0 | 0 | 2 undocumented |

**See:** [CODE_ADR_GAP_ANALYSIS.md](CODE_ADR_GAP_ANALYSIS.md) for full file-by-file matrix

---

## Part 7: Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **No observability in prod** | High | Critical | ADR-013 + implementation first |
| **Cost overruns** | Medium | High | CostGuard before production |
| **Data leakage** | Low | Critical | OutputGuard + redaction |
| **User confusion** | Medium | Medium | ADR-015 UX patterns |
| **Technical debt** | High | Medium | Remove legacy code in v0.11.0 |
| **Architectural confusion** | HIGH | HIGH | Resolve ADR-005/012 conflict immediately |
| **Dual agent systems** | MEDIUM | HIGH | Remove BaseAgent after RAG migration |

---

## Part 8: Updated Action Items

### Immediate (This Week) - CRITICAL DECISIONS

| # | Action | Owner | Effort | Status |
|---|--------|-------|--------|--------|
| 1 | Create ADR-013: Observability | Dev | 2h | DONE |
| 2 | Create ADR-014: Guardrails | Dev | 2h | DONE |
| 3 | Create ADR-015: Conversational UX | Dev | 2h | DONE |
| 4 | Create CODE_ADR_GAP_ANALYSIS.md | Dev | 2h | DONE |
| 5 | **DECISION:** ADR-005 vs ADR-012 | Team | 1h | **DECIDED: Complete ADR-012 migration** |
| 6 | **DECISION:** Remove legacy agent code | Team | 1h | **DECIDED: Remove** |
| 7 | **DECISION:** Remove legacy tool code | Team | 1h | **DECIDED: Remove** |

### Sprint 1: Foundation (2 weeks)

| # | Action | Owner | Effort | Depends On |
|---|--------|-------|--------|------------|
| 8 | Implement ADR-012 RAG migration | Dev | 5d | Decision #5 |
| 9 | Implement AIMetricsListener (ADR-013) | Dev | 2d | None |
| 10 | Implement CostGuard (ADR-013) | Dev | 2d | #9 |
| 11 | Create AIG_UsageMetrics table | Dev | 1d | #9 |

### Sprint 2: Cleanup + Guards (2 weeks)

| # | Action | Owner | Effort | Depends On |
|---|--------|-------|--------|------------|
| 12 | Remove routing/ package | Dev | 1d | #8 complete |
| 13 | Remove legacy agent/ code | Dev | 1d | Decision #6 |
| 14 | Remove legacy tool/ code | Dev | 1d | Decision #7 |
| 15 | Implement InputGuard (ADR-014) | Dev | 2d | None |
| 16 | Implement ExecutionGuard (ADR-014) | Dev | 2d | None |
| 17 | Implement OutputGuard (ADR-014) | Dev | 2d | None |

### Sprint 3: Human-in-the-Loop (2 weeks)

| # | Action | Owner | Effort | Depends On |
|---|--------|-------|--------|------------|
| 18 | Implement ApprovalWorkflow (ADR-014) | Dev | 3d | #15-17 |
| 19 | Create AIG_ApprovalRequest table | Dev | 1d | #18 |
| 20 | Implement ParameterChainHandler (ADR-015) | Dev | 2d | None |
| 21 | Implement ResponseFormatter (ADR-015) | Dev | 2d | None |
| 22 | Rename tools with erp_ prefix | Dev | 1d | None |

### Sprint 4: Domain Agents (2 weeks)

| # | Action | Owner | Effort | Depends On |
|---|--------|-------|--------|------------|
| 23 | Implement InventoryAgent (ADR-011) | Dev | 3d | #8, #15-17 |
| 24 | Implement SalesAgent (ADR-011) | Dev | 3d | #8, #15-17 |
| 25 | Implement PurchasingAgent (ADR-011) | Dev | 3d | #8, #15-17 |

---

## Conclusion

The best practices analysis combined with code coverage gap analysis reveals:

**Documentation Completed:**
1. **ADR-013 (Observability)** - DONE
2. **ADR-014 (Guardrails)** - DONE
3. **ADR-015 (Conversational UX)** - DONE
4. **CODE_ADR_GAP_ANALYSIS.md** - DONE

**Critical Decisions Required:**
1. **ADR-005 vs ADR-012** - Routing strategy conflict
2. **Legacy agent code** - Remove BaseAgent system
3. **Legacy tool code** - Remove ITool system

**Technical Debt Identified:**
- **28 files** (~1,930 lines) marked for removal
- **24%** of codebase is deprecated code
- **6 ADRs** not yet implemented

**Implementation Roadmap:**
- **Sprint 1:** RAG Migration + Observability (2 weeks)
- **Sprint 2:** Cleanup + Guardrails (2 weeks)
- **Sprint 3:** Human-in-the-Loop + UX (2 weeks)
- **Sprint 4:** Domain Agents (2 weeks)

**Total Timeline:** 8 weeks to production-ready state

---

**Document Version:** 1.2
**Created:** 2025-12-01
**Updated:** 2025-12-01 - Added Code Gap Analysis findings
**Author:** CloudEmpiere AI Team
**Status:** APPROVED - Implementation Ready

---

## Related Documents

- [CODE_ADR_GAP_ANALYSIS.md](CODE_ADR_GAP_ANALYSIS.md) - Full file-by-file coverage matrix
- [AI_MCP_ERP_BEST_PRACTICES.md](AI_MCP_ERP_BEST_PRACTICES.md) - Industry best practices reference
- [ADR Index](adr/README.md) - All Architecture Decision Records
