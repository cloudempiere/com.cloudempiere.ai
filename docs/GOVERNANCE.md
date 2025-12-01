# Project Governance

This document describes the governance structure, workflows, and standards for the `com.cloudempiere.ai` project.

---

## Documentation Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  PROJECT.md  │────▶│ CHANGELOG.md │────▶│  FEATURES.md │
│              │     │              │     │              │
│ Quick Start  │     │ What Changed │     │ What Works   │
│ Overview     │     │ Version Hist │     │ Status Track │
│              │     │ Releases     │     │ Roadmap      │
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │                    │
       ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────────┐
│                    docs/adr/*.md                         │
│         Architecture Decision Records (Why we chose)    │
└─────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────┐
│  CLAUDE.md   │
│              │
│ AI Context   │
│ Build Cmds   │
│ Architecture │
└──────────────┘
```

---

## Commit Convention

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

**Format:** `<type>(<scope>): <description>`

**Example:** `feat(provider): add Ollama local LLM support`

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

**Command:** `/release ship`

### Files Updated During Release

| File | What to Update |
|------|----------------|
| `pom.xml` | Version (if not inherited) |
| `MANIFEST.MF` | Bundle-Version |
| `CHANGELOG.md` | Version sections |
| `FEATURES.md` | Version history table |

---

## Version Phases (Released)

```
v0.1.0 ──▶ v0.2.0 ──▶ v0.3.0 ──▶ v0.4.0 ──▶ v0.5.0 ──▶ v0.6.0 ──▶ v0.7.0
  │          │          │          │          │          │          │
  ▼          ▼          ▼          ▼          ▼          ▼          ▼
┌────┐    ┌────┐    ┌────┐    ┌────┐    ┌────┐    ┌────┐    ┌────┐
│ P1 │    │ P2 │    │ P3 │    │ P4 │    │ P5 │    │ P6 │    │ P7 │
│ ✅ │    │ ✅ │    │ ✅ │    │ ✅ │    │ ✅ │    │ ✅ │    │ ✅ │
└────┘    └────┘    └────┘    └────┘    └────┘    └────┘    └────┘
Provider  Bedrock   Security  AI Chat  LangChain Lang4j   Docs &
Anthropic Provider  Context   Widget   Integr    Agents   Claude
                    Provider                              Agents

Nov 18    Nov 18    Nov 20    Nov 26   Nov 26    Nov 28   Dec 1
```

### Completed Phases

| Phase | Version | Date | Status | Key Features |
|-------|---------|------|--------|--------------|
| Phase 1 | v0.1.0 | 2025-11-18 | ✅ Done | Initial provider infrastructure, Anthropic Claude |
| Phase 2 | v0.2.0 | 2025-11-18 | ✅ Done | AWS Bedrock provider (skeleton) |
| Phase 3 | v0.3.0 | 2025-11-20 | ✅ Done | Security layer, context providers |
| Phase 4 | v0.4.0 | 2025-11-26 | ✅ Done | AI Chat widget (CLD-1606) |
| Phase 5 | v0.5.0 | 2025-11-26 | ✅ Done | LangChain integration |
| Phase 6 | v0.6.0 | 2025-11-28 | ✅ Done | LangChain4j agent framework |
| Phase 7 | v0.7.0 | 2025-12-01 | ✅ Done | Documentation & Claude agents |

### Upcoming Phases

| Phase | Version | Target | Status | Key Features |
|-------|---------|--------|--------|--------------|
| Phase 8 | v0.8.0 | Q1 2026 | Planned | AWS Bedrock completion, Ollama integration |
| Phase 9 | v0.9.0 | Q1 2026 | Planned | Domain agents (Inventory, Sales, Purchasing) |
| Phase 10 | v0.10.0 | Q2 2026 | Planned | Production database schema, migrations |
| Phase 11 | v1.0.0 | Q2 2026 | Planned | Production release |

---

## File Structure

```
com.cloudempiere.ai/
│
├── .claude/
│   ├── CLAUDE.md ◀──────────── AI Context & Instructions
│   ├── commands/
│   │   └── release.md ◀─────── /release ship|status|bump
│   └── agents/ ◀────────────── Claude Code Agent Specs (26 agents)
│
├── docs/
│   ├── adr/
│   │   ├── 000-template.md ◀── ADR Template
│   │   └── 001-*.md ◀───────── Architecture Decisions
│   ├── analysis-to-langchain/  Strategic Analysis
│   └── GOVERNANCE.md ◀──────── This document
│
├── src/com/cloudempiere/ai/
│   ├── provider/ ◀──────────── AI Provider Layer
│   │   ├── impl/ ◀──────────── Anthropic, Bedrock implementations
│   │   ├── factory/ ◀──────── OSGi service factory
│   │   └── dto/ ◀───────────── Request/Response DTOs
│   ├── database/ ◀──────────── Security Layer
│   ├── context/ ◀───────────── Context Providers
│   └── model/ ◀─────────────── Data Models (I_*, X_*, M*)
│
├── PROJECT.md ◀─────────────── Quick Start & Overview
├── CHANGELOG.md ◀───────────── Version History
├── FEATURES.md ◀────────────── Feature Matrix
├── pom.xml ◀────────────────── Maven Build
└── MANIFEST.MF ◀────────────── OSGi Bundle
```

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
| `main` | Production-ready code |
| `develop` | Integration branch |
| `nbe` | Development branch |
| `CLD-*` | Feature branches (Linear tickets) |

### Feature Branch Naming

```
CLD-1234-short-description
feature/short-description
fix/short-description
```

---

## Decision Making

### Architecture Decision Records (ADRs)

For significant decisions, create an ADR in `docs/adr/`:

1. Copy `000-template.md` to `NNN-title.md`
2. Fill in Context, Decision, Consequences
3. Set Status to "Proposed"
4. Review with team
5. Update Status to "Accepted"

### When to Create an ADR

- New provider integration
- Security model changes
- Database schema changes
- Breaking API changes
- Technology stack decisions

---

## Claude Code Commands

| Command | Description |
|---------|-------------|
| `/release ship` | Full release workflow |
| `/release status` | Show version and changes |
| `/release bump minor` | Bump to next minor version |
| `/release tag v0.x.x` | Create specific version tag |

---

## Quick Reference

### Commit Message Template

```
feat(provider): add Ollama local LLM support

- Add OllamaProvider implementation
- Support model selection
- Add health check endpoint

Closes CLD-1234

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

### Document Updates Checklist

- [ ] CHANGELOG.md updated
- [ ] FEATURES.md updated
- [ ] PROJECT.md updated (if user-facing)
- [ ] ADR created (if architectural)
- [ ] Version files updated

---

## References

- [Conventional Commits](https://conventionalcommits.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
- [PROJECT.md](../PROJECT.md)
- [FEATURES.md](../FEATURES.md)
- [CHANGELOG.md](../CHANGELOG.md)
- [ADR-001: Initial Architecture](adr/001-initial-architecture.md)

---

*Template Version: 1.0 | Based on cloudempiere-cli project standards*
