# ADR-004: Java Agent Framework Selection

**Status**: Accepted
**Date**: 2025-11-26
**Deciders**: CloudEmpiere Development Team
**Related**: [ADR-002](002-langchain4j-strategic-adoption.md)

---

## Context

The AI plugin requires an agent framework to orchestrate multi-step AI workflows that can:
- Execute tools (database queries, ERP operations)
- Maintain conversation context
- Handle complex business logic
- Integrate with multiple AI providers

Three options were evaluated: **LangChain4j**, Spring AI, and Google Agent Development Kit.

---

## Decision

**We will use LangChain4j as the agent framework.**

Already implemented in v0.9.0:
- `IDempiereAgent` interface with `AiServices`
- `ERPTools` with @Tool annotations
- Multi-provider support (Anthropic, Bedrock, Ollama, OpenAI)

---

## Rationale

### Why LangChain4j

| Criterion | LangChain4j | Spring AI | Google ADK |
|-----------|-------------|-----------|------------|
| **Maturity** | Stable (1.0) | Beta | Alpha |
| **Multi-provider** | ✅ 15+ | ✅ 10+ | ❌ Vertex only |
| **OSGi Compatible** | ✅ Pure Java | ⚠️ Spring deps | ✅ Pure Java |
| **iDempiere Fit** | ✅ Excellent | ⚠️ Framework conflict | ❌ Limited |
| **Documentation** | ✅ Extensive | ⚠️ Evolving | ⚠️ Minimal |
| **Effort** | 4-6 hours | 8-12 hours | 12-16 hours |

### Key Advantages

1. **OSGi Native**: No Spring Framework conflicts with iDempiere
2. **Provider Abstraction**: Swap AI providers without code changes
3. **Tool System**: `@Tool` annotations for ERP operations
4. **Proven**: Used in production by multiple enterprises
5. **Minimal Dependencies**: Lightweight, no bloat

### Implementation Proven

Already working in v0.9.0:
```java
@Tool("Query the database")
public String queryDatabase(@P("SQL query") String query) {
    return SecureDatabaseQueryExecutor.execute(query);
}
```

---

## Consequences

### Positive

- **Rapid Development**: Framework handles agent loop, context, retries
- **Maintainable**: Standard patterns, clear separation of concerns
- **Extensible**: Easy to add new tools and providers
- **Type-Safe**: Compile-time checking for tool signatures

### Negative

- **Library Dependency**: 6 additional JARs (~2MB)
- **Learning Curve**: Team must learn LangChain4j patterns
- **Version Lock**: Must track LangChain4j releases

### Neutral

- **Migration Path**: Can extract to custom if needed (low risk)
- **Cost**: No licensing fees, Apache 2.0

---

## Alternatives Considered

### Spring AI
- **Rejected**: Requires Spring Framework, conflicts with iDempiere's OSGi architecture
- **Effort**: Would require 8-12 hours for Spring Boot wrapper

### Google Agent Development Kit
- **Rejected**: Alpha maturity, Vertex AI only, limited multi-provider support
- **Risk**: Unstable API, poor documentation

### Custom Framework
- **Rejected**: 40-60 hours development time, maintenance burden
- **Risk**: Reinventing wheel, bug-prone

---

## Implementation

**Status**: ✅ Completed in v0.9.0

See [ADR-002](002-langchain4j-strategic-adoption.md) for full migration details.

### Core Components

```
src/com/cloudempiere/ai/provider/langchain4j/
├── LangChain4jProviderFactory.java   # Creates ChatLanguageModel
├── IDempiereAgent.java               # AiServices interface
├── IDempiereAIService.java           # Main facade
└── ERPTools.java                     # @Tool methods
```

### Dependencies Added

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>1.0.0-beta3</version>
</dependency>
```

---

## References

- **LangChain4j Documentation**: https://docs.langchain4j.dev/
- **Provider Comparison**: `docs/ai-agent-javaframeworkagent/SPECIALIZED-LIBRARIES-COMPARISON.md`
- **Integration Guide**: `docs/ai-agent-javaframeworkagent/CloudEmpiere-Integration-Guide.md`

---

## Notes

- Exploratory research archived in `docs/ai-agent-javaframeworkagent/`
- Decision aligns with ADR-002 LangChain4j strategic adoption
- No breaking changes to existing provider API
