# Project Roadmap

Version phases, milestones, and release workflow for `com.cloudempiere.ai`.

**Related:**
- [GOVERNANCE.md](GOVERNANCE.md) - Decision authority, commit conventions, quality gates
- [FEATURES.md](../FEATURES.md) - Feature status matrix
- [CHANGELOG.md](../CHANGELOG.md) - Version history

---

## Version Phases

### Completed Phases

```
v0.1.0 ──▶ v0.2.0 ──▶ v0.3.0 ──▶ v0.4.0 ──▶ v0.5.0 ──▶ v0.6.0 ──▶ v0.7.0 ──▶ v0.8.0
  │          │          │          │          │          │          │          │
  ▼          ▼          ▼          ▼          ▼          ▼          ▼          ▼
Provider  Bedrock   Security  AI Chat  LangChain Lang4j   Docs &    MCP &
Anthropic Provider  Context   Widget   Integr    Agents   Claude    Strategic
                    Provider                              Agents    ADRs

Nov 18    Nov 18    Nov 20    Nov 26   Nov 26    Nov 28   Dec 1     Dec 1
```

| Phase | Version | Date | Key Features |
|-------|---------|------|--------------|
| Phase 1 | v0.1.0 | 2025-11-18 | Initial provider infrastructure, Anthropic Claude |
| Phase 2 | v0.2.0 | 2025-11-18 | AWS Bedrock provider (skeleton) |
| Phase 3 | v0.3.0 | 2025-11-20 | Security layer, context providers |
| Phase 4 | v0.4.0 | 2025-11-26 | AI Chat widget (CLD-1606) |
| Phase 5 | v0.5.0 | 2025-11-26 | LangChain integration |
| Phase 6 | v0.6.0 | 2025-11-28 | LangChain4j agent framework |
| Phase 7 | v0.7.0 | 2025-12-01 | Documentation & Claude agents |
| Phase 8 | v0.8.0 | 2025-12-01 | MCP Server & Strategic Architecture |

### Upcoming Phases

| Phase | Version | Target | Key Features |
|-------|---------|--------|--------------|
| Phase 9 | v0.9.0 | Q1 2026 | LangChain4j Native Providers, HTTP API layer |
| Phase 10 | v0.10.0 | Q1 2026 | Domain agents (Inventory, Sales, Purchasing) |
| Phase 11 | v0.11.0 | Q2 2026 | Production database schema, migrations |
| Phase 12 | v1.0.0 | Q2 2026 | Production release |

---

## Release Workflow

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Develop │───▶│ Update  │───▶│ Commit  │───▶│  Tag    │───▶│  Bump   │
│ Feature │    │  Docs   │    │ Release │    │ vX.Y.Z  │    │ Version │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
     │              │              │              │              │
     ▼              ▼              ▼              ▼              ▼
Code changes   CHANGELOG.md   chore(release)  git tag -a    X.Y.Z+1-SNAPSHOT
               FEATURES.md    release vX.Y.Z  vX.Y.Z
               pom.xml
               MANIFEST.MF
```

### Files Updated During Release

| File | What to Update |
|------|----------------|
| `pom.xml` | Version (if not inherited) |
| `META-INF/MANIFEST.MF` | Bundle-Version |
| `CHANGELOG.md` | Version sections |
| `FEATURES.md` | Version history table |

### Claude Code Commands

| Command | Description |
|---------|-------------|
| `/release ship` | Full release workflow |
| `/release status` | Show version and changes |
| `/release bump minor` | Bump to next minor version |
| `/release tag v0.x.x` | Create specific version tag |

---

## Status Legend

| Status | Meaning |
|--------|---------|
| ✅ Done | Feature is complete and tested |
| 🚧 Partial | Skeleton/incomplete implementation |
| ❌ Planned | On roadmap, not started |
| ⚠️ Deprecated | Will be removed in future version |
| 🔬 Experimental | May change without notice |

---

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `master` | Production-ready code |
| `develop` | Integration branch |
| `langchain` | LangChain4j integration development |
| `CLD-*` | Feature branches (Linear tickets) |

### Feature Branch Naming

```
CLD-1234-short-description
feature/short-description
fix/short-description
```

---

## Current Sprint Focus

See [chat-panel-implementation-plan.md](guides/chat-panel-implementation-plan.md) for detailed implementation tasks.

**Immediate Priorities (P1):**
1. Unit tests for ThreadAwareChatMemory, RAGContextManager, AgentBoundary
2. TokenUsageListener for cost tracking
3. Delete old routing code (1884 lines) after tests pass

**Enhancement (P2):**
4. LatencyMetricsListener
5. TimeBoundaryValidator
6. Integration tests

---

## References

- [GOVERNANCE.md](GOVERNANCE.md) - Decision authority, commit conventions
- [FEATURES.md](../FEATURES.md) - Feature matrix
- [CHANGELOG.md](../CHANGELOG.md) - Version history
- [Semantic Versioning](https://semver.org/)
- [Keep a Changelog](https://keepachangelog.com/)

---

*Version: 3.0 | Updated: 2025-12-26*
