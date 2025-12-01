# Architecture Decision Records (ADRs)

This directory contains architecture decisions for the CloudEmpiere AI plugin.

---

## Active ADRs

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [001](001-initial-architecture.md) | Initial Architecture and Project Standards | Accepted | 2025-12-01 |
| [002](002-langchain4j-strategic-adoption.md) | LangChain4j Strategic Adoption | Accepted | 2025-12-01 |
| [003](003-mcp-server-integration.md) | MCP Server Integration | Accepted | 2025-12-01 |
| [004](004-java-agent-framework.md) | Java Agent Framework Selection | Accepted | 2025-11-26 |

---

## Appendices

| Doc | Title | Related ADR |
|-----|-------|-------------|
| [002-appendix](002-appendix-feature-mapping.md) | Feature Mapping: Legacy to LangChain4j | ADR-002 |

---

## ADR Process

1. **Propose**: Use [000-template.md](000-template.md) to draft new ADR
2. **Discuss**: Review with team
3. **Decide**: Update status to Accepted/Rejected
4. **Implement**: Link to implementation commits
5. **Update Index**: Add to this README

---

## ADR Format

Each ADR includes:
- **Status**: Proposed, Accepted, Deprecated, Superseded
- **Context**: Problem statement
- **Decision**: What was decided
- **Consequences**: Impact analysis
- **Alternatives**: Options considered

---

## Key Decisions Summary

### ADR-001: Initial Architecture
- Plugin structure and conventions
- OSGi bundle architecture
- Maven/Tycho build system

### ADR-002: LangChain4j Strategic Adoption
- Migration from custom provider to LangChain4j
- Multi-provider abstraction layer
- Deprecation path for legacy APIs

### ADR-003: MCP Server Integration
- Model Context Protocol for external tools
- HTTP API + Node.js MCP server hybrid
- Security and authentication model

### ADR-004: Java Agent Framework Selection
- LangChain4j chosen over Spring AI and Google ADK
- Tool system with @Tool annotations
- Agent orchestration patterns
