# Project Governance for AI-Driven Development

**Project**: com.cloudempiere.ai
**Purpose**: Decision authority framework for AI-assisted development
**Last Updated**: 2025-12-26

---

## Core Principle

> **"Documentation is not output; it's input."**
>
> In AI-driven development, documentation directs future decisions. Well-structured docs enable AI agents to work autonomously within defined boundaries while maintaining architectural consistency.

---

## 1. Documentation Hierarchy

### Tier 0: Project Foundation (Immutable)

**Purpose**: Core project identity and constraints.

| Document | Content | AI Role |
|----------|---------|---------|
| `PROJECT.md` | Vision, goals, success metrics | Read-only |
| `CLAUDE.md` | AI agent instructions | Read-only |
| `.claude/CLAUDE.md` | Extended AI context | Read-only |

**Authority**: Human-only modification
**AI Constraint**: Must never contradict

---

### Tier 1: Architectural Decisions (Stable)

**Purpose**: Major architectural choices that guide implementation.

| Document | Content | AI Role |
|----------|---------|---------|
| `docs/adr/*.md` | Architecture Decision Records | Can propose, cannot accept |
| `docs/ARCHITECTURE.md` | System architecture | Read, reference |

**Authority**: Human approval required for new ADRs
**Format**: RFC 2119 keywords (MUST, SHOULD, MAY)

**ADR Lifecycle**:
1. AI drafts ADR based on research
2. Human reviews, approves, or requests changes
3. Once accepted, ADR guides all related decisions
4. Status: `Proposed` → `Accepted` → `Superseded` (never deleted)

---

### Tier 2: Implementation Guides (Semi-Stable)

**Purpose**: Detailed technical guidance that evolves with learning.

| Document | Content | AI Role |
|----------|---------|---------|
| `docs/guides/*.md` | Implementation patterns | Can extend with examples |
| `docs/mcpserver/*.md` | MCP server documentation | Can extend |
| `FEATURES.md` | Feature status matrix | Can update status |

**Authority**: AI can extend, human review for major changes
**Update Policy**:
- Minor additions (examples, clarifications): AI autonomous
- New patterns/strategies: Human review
- Contradicting existing patterns: Requires ADR

---

### Tier 3: Implementation Details (Volatile)

**Purpose**: Code-level decisions, implementation notes, work-in-progress.

| Document | Content | AI Role |
|----------|---------|---------|
| `CHANGELOG.md` | Version history | Update on release |
| `docs/ROADMAP.md` | Phases and milestones | Update status |
| Implementation notes | Running notes | Full autonomy |

**Authority**: AI fully autonomous

---

## 2. Decision Authority Matrix

### AI Can Decide Autonomously

| Category | Authority | Example |
|----------|-----------|---------|
| Implementation details | Full | Variable naming, utility methods |
| Code patterns | Full | Loop vs stream (if no ADR) |
| Performance optimizations | Full | Caching within ADR bounds |
| Tier 3 documentation | Full | Implementation notes |
| Examples in guides | Yes | Adding code examples |
| Bug fixes | Yes | If no architectural implications |
| Test writing | Yes | Unit tests, integration tests |
| Refactoring | Yes | If improves code without breaking API |

### AI Must Request Human Approval

| Category | Approval Required | Example |
|----------|-------------------|---------|
| New ADR | Always | "Should we use cursor-based pagination?" |
| Architecture change | Always | "Switch from PO to custom SQL?" |
| Breaking API changes | Always | Changing interface signatures |
| New dependencies | Always | Adding libraries to pom.xml |
| Security decisions | Always | Authentication flow changes |
| Contradicting ADR | Always | Proposing alternative to accepted ADR |
| Major refactoring | Always | Restructuring package hierarchy |
| Build system changes | Always | Maven configuration changes |
| Database schema changes | Always | New tables, columns |

### AI Should Flag for Human Review

| Category | Flag | Example |
|----------|------|---------|
| Performance concerns | Flag | "Current approach may not scale" |
| Security concerns | Flag | "Potential vulnerability detected" |
| ADR ambiguity | Flag | "ADR doesn't specify this case" |
| Multi-solution scenarios | Flag | "3 valid approaches exist" |
| Technical debt | Flag | "Quick fix vs proper solution" |

---

## 3. AI Behavioral Guidelines

### Communication Style

**When proposing ADR**:
```
Bad:  "We should use LangChain4j."
Good: "I've researched agent frameworks and drafted ADR-004.
       Based on Java 11 constraint, I recommend LangChain4j 0.35.0.
       Would you like to review the alternatives analysis?"
```

**When flagging ambiguity**:
```
Bad:  "I don't know what to do."
Good: "ADR-002 specifies LangChain4j but doesn't cover this case:
       [describe situation]. Based on ADR principles, I interpret
       this to mean [X]. Please confirm."
```

**When documenting findings**:
```
Bad:  "It's slow."
Good: "Performance test: Provider initialization: 420ms.
       Target: < 100ms. Recommend lazy loading per ADR-013."
```

### Proactive Behavior

**AI Should**:
- Suggest ADRs when detecting architectural questions
- Flag performance issues with metrics
- Document gotchas in implementation notes
- Propose documentation improvements
- Check for ADR contradictions before implementing

**AI Should NOT**:
- Assume undocumented decisions
- Contradict ADRs without proposing replacement
- Make breaking changes without explicit approval
- Skip documentation because "it's obvious"
- Commit without approval

---

## 4. Quality Gates

### Gate 1: Code Completion

Before marking feature complete:

- [ ] Code follows patterns from relevant ADRs
- [ ] Implementation guide patterns applied correctly
- [ ] Unit tests written (use `@UnitTest` annotation)
- [ ] Performance meets targets (if specified)
- [ ] Security checklist passed (authorization, validation)
- [ ] Multi-tenancy respected (AD_Client_ID, AD_Org_ID)
- [ ] Error handling follows established patterns
- [ ] Documentation updated

**If any fail**: Flag for human review

---

### Gate 2: ADR Quality

Before presenting ADR to human:

- [ ] Context section has quantitative data
- [ ] Minimum 3 alternatives analyzed
- [ ] Each alternative has pros, cons, verdict
- [ ] Consequences quantified (performance, effort, risk)
- [ ] Implementation plan has concrete steps
- [ ] Success criteria are measurable
- [ ] References include all sources
- [ ] Status is "Proposed" (not "Accepted")

---

### Gate 3: Documentation Consistency

Periodic check:

- [ ] All ADRs link to related ADRs
- [ ] Implementation guides reference relevant ADRs
- [ ] No contradictions between documents
- [ ] Code examples compile and run
- [ ] CHANGELOG.md reflects actual changes
- [ ] FEATURES.md status is accurate

---

## 5. Workflows

### Workflow 1: Implementing New Feature

```
STEP 1: Understand Context
├── Read CLAUDE.md (goals, constraints)
├── Search for related ADRs
├── Review implementation guides
└── Check for existing patterns

STEP 2: Check Decision Authority
├── Requires architectural decision? → Propose ADR, wait
└── Within existing guidance? → Continue

STEP 3: Design Solution
├── Apply patterns from ADRs
├── Follow decision trees in guides
└── Document design rationale

STEP 4: Implement
├── Write code following patterns
├── Add tests (./run-unit-tests.sh)
├── Update implementation notes
└── Flag concerns for review

STEP 5: Document
├── Add examples to guides if helpful
├── Document performance characteristics
└── Update troubleshooting docs
```

---

### Workflow 2: Proposing Architectural Change

```
STEP 1: Identify Need for ADR
├── Multiple valid approaches exist
├── Decision impacts multiple components
├── Performance/security concern
└── Contradicts existing guidance

STEP 2: Research Alternatives
├── Research industry patterns
├── Analyze iDempiere capabilities
├── Review similar decisions
└── Identify at least 3 alternatives

STEP 3: Draft ADR
├── Use ADR template (docs/adr/000-template.md)
├── Document context with metrics
├── Analyze all alternatives
├── Quantify consequences
└── Status: "Proposed"

STEP 4: Present to Human
├── Executive summary (2-3 sentences)
├── Recommendation with confidence
├── Key trade-offs
└── Questions for consideration

STEP 5: Incorporate Feedback
├── Update ADR based on feedback
├── Add discussion notes
└── Wait for human to accept
```

---

## 6. Conflict Resolution

### ADR Contradicts Project Constraints

**Example**: ADR recommends feature requiring Java 17, but CLAUDE.md says Java 11

**Resolution**:
1. AI flags conflict immediately
2. AI proposes: "Defer to future phase per Java strategy"
3. Human approves or provides alternative
4. Update ADR with constraints section

---

### Two ADRs Conflict

**Resolution**:
1. AI detects conflict when implementing
2. AI proposes: "ADR supersession needed"
3. Human decides which ADR to update
4. Losing ADR marked "Superseded by ADR-XXX"

---

### Implementation Contradicts ADR

**Resolution**:
1. Flag in code review
2. Check if ADR allows exception (SHOULD vs MUST)
3. If no exception: Refactor to comply
4. If exception justified: Document and propose ADR clarification

---

## 7. Version Constraints

### Critical Constraints (from CLAUDE.md)

| Component | Version | Constraint |
|-----------|---------|------------|
| Java | 11 | iDempiere v10 requirement |
| LangChain4j | 0.35.0 | Last Java 11 compatible |
| iDempiere | v10 | Release-10 branch |

**AI MUST check version compatibility before suggesting dependencies.**

---

## 8. Testing Requirements

### Test Categories

| Annotation | Purpose | Command |
|------------|---------|---------|
| `@UnitTest` | Fast, isolated tests | `./run-unit-tests.sh` |
| `@IntegrationTest` | Database/service tests | `./run-unit-tests.sh --integration` |
| `@E2ETest` | End-to-end tests | Manual |

### Test Location

All tests go in `com.cloudempiere.ai.test` bundle.

### Logging

Debug log level must be at least WARNING for Eclipse console visibility.

---

## 9. Commit Convention

Follow [Conventional Commits](https://conventionalcommits.org/):

| Type | Description | CHANGELOG |
|------|-------------|-----------|
| `feat(scope):` | New feature | Added |
| `fix(scope):` | Bug fix | Fixed |
| `docs:` | Documentation | (no entry) |
| `refactor:` | Code refactor | Changed |
| `chore:` | Maintenance | (no entry) |
| `test:` | Tests | (no entry) |
| `perf:` | Performance | Changed |

**AI MUST NOT commit without human approval.**

---

## 10. Documentation Standards

### File Naming

- Use `lowercase-kebab-case.md` for guides
- Use `UPPERCASE.md` for root-level docs (README, CHANGELOG, etc.)
- Use `NNN-title.md` for ADRs (sequential numbering)

### Required Elements

All documentation MUST include:
- Title (H1 header)
- Purpose/Overview (first paragraph)
- Last Updated date

### Cross-References

Use relative paths:
```markdown
[ADR-002](adr/002-langchain4j-strategic-adoption.md)
[Feature Matrix](../FEATURES.md)
```

---

## 11. Quick Reference

### Before Starting Work

1. Read relevant ADRs
2. Check FEATURES.md for current status
3. Review CLAUDE.md constraints
4. Create todo list for complex tasks

### During Implementation

1. Follow ADR patterns
2. Update implementation notes
3. Flag ambiguities immediately
4. Run tests frequently

### Before Completing Work

1. Run full test suite
2. Update CHANGELOG.md
3. Update FEATURES.md status
4. Request human review

---

## References

- [CLAUDE.md](../.claude/CLAUDE.md) - AI Context
- [PROJECT.md](../PROJECT.md) - Project Overview
- [FEATURES.md](../FEATURES.md) - Feature Matrix
- [ROADMAP.md](ROADMAP.md) - Version Phases
- [ADR Index](adr/README.md) - Architecture Decisions
- [Conventional Commits](https://conventionalcommits.org/)

---

*Version: 1.0 | Based on cloudempiere-workspace governance model*
