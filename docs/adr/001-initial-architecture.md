# ADR-001: Initial Architecture and Project Standards

**Status:** Accepted
**Date:** 2025-12-01
**Context:** Establishing project standards for AI plugin development

## Context

The `com.cloudempiere.ai` project is an iDempiere ERP plugin integrating AI capabilities. As the project grows with multiple providers, agents, and security layers, we need consistent standards for development, documentation, and release management.

### Problem Statement

Without established standards:
- Documentation becomes inconsistent
- Version tracking is difficult
- Release processes are error-prone
- Architectural decisions are lost

### Constraints

- Must integrate with iDempiere OSGi plugin architecture
- Must follow Maven/Tycho build conventions
- Must support multi-developer collaboration
- Must maintain audit trail for enterprise use

## Decision

We adopt the following standards from the cloudempiere-cli project:

### 1. Conventional Commits

All commits follow [Conventional Commits](https://conventionalcommits.org/):
- `feat:` new feature
- `fix:` bug fix
- `docs:` documentation
- `refactor:` code refactor
- `chore:` maintenance
- `test:` adding tests
- `perf:` performance improvement

### 2. Documentation Files

| File | Purpose |
|------|---------|
| `CLAUDE.md` | AI assistant instructions |
| `CHANGELOG.md` | Version history (Keep a Changelog) |
| `FEATURES.md` | Feature matrix and status |
| `docs/adr/*.md` | Architecture Decision Records |

### 3. Version Numbering

Semantic Versioning (SemVer):
- **Major**: Breaking changes
- **Minor**: New features, backward compatible
- **Patch**: Bug fixes

### 4. Release Workflow

1. Update version in `pom.xml`, `MANIFEST.MF`
2. Update `CHANGELOG.md` with release date
3. Commit and create tag: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`
4. Bump to next development version

### 5. Architecture Decision Records

ADRs document significant decisions:
- Numbered sequentially (001, 002, ...)
- Stored in `docs/adr/`
- Include context, decision, consequences

## Implementation

### Phase 1: Initial Setup (v0.1.0)

1. Create `CHANGELOG.md` with initial version
2. Create `FEATURES.md` with feature matrix
3. Create `docs/adr/` directory with template
4. Update `CLAUDE.md` with conventions
5. Create `.claude/commands/release.md`

### Phase 2: Ongoing Maintenance

- Update documentation with each feature
- Create ADR for significant decisions
- Follow commit conventions strictly

## Consequences

### Positive

- Consistent documentation across the project
- Clear version history and feature tracking
- Automated release workflow reduces errors
- Architectural decisions preserved for future reference
- Better AI assistant context via CLAUDE.md

### Negative

- Additional documentation overhead per feature
- Learning curve for team members new to conventions

### Neutral

- Aligns with cloudempiere-cli standards for cross-project consistency

## References

- [Conventional Commits](https://conventionalcommits.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
- [cloudempiere-cli Repository Rules](../../../cloudempiere-cli/docs/templates/REPOSITORY_RULES.md)
