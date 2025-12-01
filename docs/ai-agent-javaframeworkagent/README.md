# Java Agent Framework

**Status**: Implemented in v0.9.0
**Date**: November 26, 2025
**Decision**: [ADR-004](../adr/004-java-agent-framework.md)

---

## Decision Summary

✅ **LangChain4j** selected as the Java agent framework
✅ Implemented in v0.9.0
✅ See [ADR-004](../adr/004-java-agent-framework.md) for complete rationale

---

## For Implementation Details

- **Architecture Decision**: [ADR-004](../adr/004-java-agent-framework.md)
- **Strategic Adoption**: [ADR-002](../adr/002-langchain4j-strategic-adoption.md)
- **Source Code**: `src/com/cloudempiere/ai/provider/langchain4j/`
- **Tests**: `src/test/java/com/cloudempiere/ai/provider/langchain4j/`

---

## Quick Reference

**LangChain4j in CloudEmpiere:**

```java
// 1. Configure provider
LangChain4jProviderFactory factory = new LangChain4jProviderFactory();
ChatLanguageModel model = factory.createChatModel(providerConfig);

// 2. Create agent
IDempiereAgent agent = AiServices.create(IDempiereAgent.class, model);

// 3. Use agent
String response = agent.chat("Query top 10 customers");
```

See `src/test/java/` for complete examples.
