# Documentation Organization

This document describes the organization of documentation in the `docs/` folder.

## Directory Structure

```
docs/
├── adr/                        # Architecture Decision Records
├── architecture/               # Architectural documentation
├── guides/                     # Implementation and operational guides
│   ├── context-layer/         # Context layer implementation
│   ├── ide/                   # Eclipse and IDE setup
│   ├── implementation/        # Implementation plans and summaries
│   ├── langchain4j/          # LangChain4j integration guides
│   ├── migration/            # Migration and legacy guides
│   ├── misc/                 # Miscellaneous guides
│   ├── mvp/                  # MVP implementation
│   ├── osgi/                 # OSGi configuration and fixes
│   ├── performance/          # Performance optimization
│   ├── security/             # Security and sanitization
│   ├── streaming/            # Streaming and rendering
│   ├── testing/              # Testing strategies and fixes
│   └── zk-ui/                # ZK UI development
├── knowledge-base/            # Knowledge base articles
├── reference/                 # Reference documentation
├── reports/                   # Progress and status reports
│   ├── phases/               # Phase completion reports
│   └── sessions/             # Development session summaries
└── research/                  # Research and analysis documents
```

## Core Files (Root Directory)

- **README.md** - Project overview and getting started
- **CLAUDE.md** - Instructions for Claude Code

## Reference Documentation (`docs/reference/`)

Core project documentation and planning:

- **PROJECT.md** - Comprehensive project documentation
- **FEATURES.md** - Feature matrix and capabilities
- **CHANGELOG.md** - Version history and changes
- **GOVERNANCE.md** - Project governance and decision-making
- **ROADMAP.md** - Product roadmap and future plans
- **DEPRECATION_ROADMAP.md** - Deprecation schedule and migration paths
- **CRITICAL_ISSUES.md** - Critical issues tracking

## Architecture (`docs/architecture/`)

High-level architectural designs and patterns:

- **ARCHITECTURE.md** - Overall system architecture
- **ARCHITECTURE_DIAGRAMS.md** - Visual architecture diagrams
- **LAYERED_ARCHITECTURE_DESIGN.md** - Layered architecture specification
- **OSGI_MULTI_PLUGIN_ARCHITECTURE.md** - OSGi bundle architecture
- **EXTERNAL_INTEGRATION_ARCHITECTURE.md** - External integration patterns
- **ARCHITECTURAL_ISSUES_MATRIX.md** - Architectural issues tracking
- **ARCHITECTURAL_VALIDATION_ADR-055.md** - ADR-055 validation results

## Implementation Guides (`docs/guides/implementation/`)

Implementation plans and progress tracking:

- **IMPLEMENTATION_PLAN_FINAL.md** - Final implementation plan
- **IMPLEMENTATION_STATUS_SUMMARY.md** - Overall status summary
- **IMPLEMENTATION_SUMMARY_ADR-054.md** - ADR-054 implementation
- **IMPLEMENTATION_SUMMARY_ADR-055.md** - ADR-055 implementation
- **GREENFIELD_IMPLEMENTATION_PLAN.md** - Greenfield approach plan
- **COMPREHENSIVE_REQUIREMENTS_SUMMARY.md** - Requirements consolidation
- **REQUIREMENT_VALIDATION_RENDERING_AND_ZOOM.md** - Rendering requirements
- **CHAT_TENANT_ISOLATION_FIX.md** - Chat tenant isolation implementation
- **STANDALONE_AI_CHAT_WIDGET.md** - Standalone widget implementation

## Migration Guides (`docs/guides/migration/`)

Legacy code migration and deprecation:

- **MIGRATION_COMPLETE.md** - Migration completion report
- **MIGRATION_NEXT_STEPS.md** - Post-migration tasks
- **MIGRATION_VALIDATION_SUMMARY.md** - Migration validation results
- **LEGACY_DEPRECATION_STRATEGY.md** - Deprecation approach
- **LEGACY_VALIDATION_REPORT.md** - Legacy code validation
- **OBSOLETE_FUNCTIONALITY_MAPPING.md** - Obsolete code mapping
- **OBSOLETE_PACKAGES_INVESTIGATION.md** - Package obsolescence analysis

## OSGi Guides (`docs/guides/osgi/`)

OSGi configuration and troubleshooting:

- **OSGI_STARTUP_FIXES_COMPLETE.md** - Startup issue resolutions
- **OSGI_VALIDATION_REPORT.md** - OSGi validation results
- **CRITICAL_OSGI_FIXES_APPLIED.md** - Critical fixes applied
- **COMPILATION_FIXES.md** - Compilation issue resolutions

## Performance Guides (`docs/guides/performance/`)

Performance optimization and analysis:

- **PERFORMANCE_REGRESSION_ANALYSIS.md** - Regression analysis
- **PERFORMANCE_FIX_SUMMARY.md** - Performance improvements applied

## Streaming & Rendering (`docs/guides/streaming/`)

Streaming implementation and markdown rendering:

- **STREAMING_ARCHITECTURE_FIX.md** - Architecture corrections
- **STREAMING_RENDERING_SPECIFICATION.md** - Rendering specification
- **STREAMING_SEQUENCE_DIAGRAMS.md** - Sequence diagrams
- **STREAMING_MARKDOWN_IMPLEMENTATION_PLAN.md** - Markdown streaming implementation plan
- **STREAMING_MARKDOWN_IMPLEMENTATION_COMPLETE.md** - Implementation completion report
- **MARKDOWN_STREAMING_FIX.md** - Markdown streaming fixes
- **QUICK_REFERENCE_STREAMING_FIX.md** - Quick reference guide
- **RENDERING_FIXES_SUMMARY.md** - Rendering fixes summary
- **HOTFIX_HTML_RENDERING_BUG.md** - HTML rendering bug fix

## Security Guides (`docs/guides/security/`)

Security implementation and sanitization:

- **SECURITY_SANITIZATION_PROPOSAL.md** - Sanitization strategy
- **SANITIZATION_INVALID_CHARS_ANALYSIS.md** - Character sanitization analysis
- **DEBUG_SANITIZER.md** - Sanitizer debugging guide

## LangChain4j Integration (`docs/guides/langchain4j/`)

LangChain4j framework integration:

- **LANGCHAIN4J_STRATEGIC_ANALYSIS.md** - Strategic analysis
- **LANGCHAIN4J_ANOMALIES.md** - Known anomalies and workarounds

## Testing Guides (`docs/guides/testing/`)

Testing strategies and results:

- **TEST_ADR-054.md** - ADR-054 test documentation
- **TEST_ADR-055.md** - ADR-055 test documentation
- **TEST_FIX_SUMMARY.md** - Test fixes applied
- **TEST_STATUS_AND_RECOMMENDATION.md** - Testing recommendations

## IDE & Eclipse (`docs/guides/ide/`)

Eclipse and IDE configuration:

- **ECLIPSE_REFRESH.md** - Eclipse refresh procedures
- **ECLIPSE_REFRESH_INSTRUCTIONS.md** - Detailed refresh instructions
- **ECLIPSE_SYNC_INSTRUCTIONS.md** - Sync instructions
- **SLF4J_CONFLICT_RESOLVED.md** - SLF4J dependency resolution
- **STARTUP_FIXES_APPLIED.md** - Startup fixes

## ZK UI Development (`docs/guides/zk-ui/`)

ZK framework UI development guides:

- **ai-widget-css-refactoring-guide.md** - CSS refactoring guide for AI widget
- **theme-fragment-dual-path-solution.md** - Theme fragment solution

## Miscellaneous Guides (`docs/guides/misc/`)

Other guides and decisions:

- **SERVICE_PACKAGE_DECISION.md** - Service package organization
- **LANGUAGE_FIX_SUMMARY.md** - Language handling fixes

## ADR Documentation (`docs/adr/`)

Architecture Decision Records:

- **ADR_CONSOLIDATION_2026.md** - 2026 ADR consolidation
- *[Other ADR files already present]*

## Research (`docs/research/`)

Research and validation documents:

- **INDUSTRY_PATTERNS_VALIDATION.md** - Industry patterns analysis

## Phase Reports (`docs/reports/phases/`)

Development phase completion reports:

- **PHASE_1_COMPLETION_REPORT.md** - Phase 1 completion
- **PHASE_2_KICKOFF.md** - Phase 2 kickoff
- **PHASE_2_WEEK_3_COMPLETE.md** - Week 3 completion
- **PHASE_2_WEEK_4_COMPLETION.md** - Week 4 completion
- **PHASE_2_WEEK_4_PROGRESS.md** - Week 4 progress
- **PHASE_2_WEEK_5-6_COMPLETION.md** - Week 5-6 completion

## Session Reports (`docs/reports/sessions/`)

Development session summaries:

- **SESSION_SUMMARY_2026-01-30.md** - Session summary

## Finding Documentation

Use the following quick reference to find documentation by topic:

| Topic | Location |
|-------|----------|
| Getting Started | `README.md` (root) |
| Project Overview | `docs/reference/PROJECT.md` |
| Architecture | `docs/architecture/` |
| Implementation Plans | `docs/guides/implementation/` |
| Phase Reports | `docs/reports/phases/` |
| Migration & Legacy | `docs/guides/migration/` |
| OSGi Configuration | `docs/guides/osgi/` |
| Performance | `docs/guides/performance/` |
| Streaming & Rendering | `docs/guides/streaming/` |
| Security | `docs/guides/security/` |
| LangChain4j | `docs/guides/langchain4j/` |
| Testing | `docs/guides/testing/` |
| Eclipse/IDE Setup | `docs/guides/ide/` |
| ADRs | `docs/adr/` |
| Research | `docs/research/` |
