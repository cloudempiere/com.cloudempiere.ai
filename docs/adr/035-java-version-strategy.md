# ADR-035: Java Version Strategy and LangChain4j Migration Path

## Status

**Accepted**

## Date

2025-12-04

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

The Cloudempiere AI plugin has a **critical version incompatibility** between its runtime environment and declared dependencies:

| Component | Current State | Requirement | Gap |
|-----------|--------------|-------------|-----|
| iDempiere | v10 | Java 11 | - |
| Runtime (Production) | Amazon Corretto 11 | - | - |
| LangChain4j | 1.0.0-beta3 | **Java 17+** | **INCOMPATIBLE** |
| LangChain4j (Latest) | 1.9.1 | Java 17+ | - |

**The project currently declares LangChain4j 1.0.0-beta3 in `pom.xml`, which requires Java 17, but the runtime environment is Java 11.**

This ADR documents:
1. The version incompatibility discovered during audit
2. Options for resolution
3. Migration strategy to align Java and LangChain4j versions
4. Feature availability matrix at each version level

## Decision Drivers

- **Production Stability**: Must not break existing iDempiere v10 production deployments
- **Feature Completeness**: Need sufficient LangChain4j features for MVP use cases (ADR-017 to ADR-020)
- **Future-Proofing**: Position for Java 17 migration with iDempiere Release-11
- **Security**: Ensure access to security patches and updates
- **Cost**: Minimize rework during migration

## Considered Options

1. **Downgrade LangChain4j to 0.35.0** (Java 11 compatible) for MVP, then upgrade with Java 17
2. **Immediate Java 17 migration** (upgrade iDempiere to Release-11 first)
3. **Fork LangChain4j 0.35.0** and maintain custom patches
4. **Replace LangChain4j** with custom implementation (as originally built)

## Decision Outcome

**Chosen option:** "Option 1 - Downgrade LangChain4j to 0.35.0 for MVP, upgrade with Java 17 migration"

This approach:
- Unblocks immediate development on Java 11
- Provides all features needed for Phase 1 MVP use cases
- Creates clear upgrade path aligned with iDempiere Release-11 timeline
- Minimizes rework (LangChain4j API is stable between 0.35 and 1.x)

### Confirmation

- [x] `pom.xml` updated to LangChain4j 0.35.0 *(Verified 2025-12-04)*
- [ ] All existing tests pass on Java 11
- [ ] ADR-017 (Chart Overview) works end-to-end
- [x] Migration checklist created for Java 17 upgrade *(See Phase 2 section below)*
- [x] CLAUDE.md updated with version constraints *(Added to CLAUDE.md header table)*

---

## LangChain4j Version History

| Version | Release Date | Java Requirement | Key Changes |
|---------|--------------|------------------|-------------|
| 0.33.0 | Aug 2024 | Java 8+ | Anthropic tools, streaming |
| 0.34.0 | Sep 2024 | Java 8+ | Amazon Bedrock tools |
| **0.35.0** | Sep 25, 2024 | **Java 8+** | **Last Java 11 compatible** |
| 0.36.0 | Oct 2024 | Java 17+ | Java 17 baseline, removed Lombok |
| 1.0.0-beta1 | Feb 2025 | Java 17+ | API stabilization |
| 1.0.0-beta3 | Apr 12, 2025 | Java 17+ | Pre-release, MCP support |
| 1.0.0 | May 2025 | Java 17+ | Stable release |
| **1.9.1** | Nov 28, 2025 | Java 17+ | **Current latest** |

### Source References

- [LangChain4j GitHub Releases](https://github.com/langchain4j/langchain4j/releases)
- [Issue #1652: Raise baseline to Java 17](https://github.com/langchain4j/langchain4j/issues/1652)
- [PR #1913: Upgrade to JDK 17](https://github.com/langchain4j/langchain4j/pull/1913)
- [LangChain4j Documentation](https://docs.langchain4j.dev/get-started/)

---

## Feature Availability Matrix

### LangChain4j 0.35.0 (Java 11 Compatible)

| Feature | Available | Notes |
|---------|-----------|-------|
| **Core Framework** | | |
| AiServices interface | Yes | Full support |
| @Tool annotations | Yes | Full support |
| @SystemMessage, @UserMessage | Yes | Full support |
| ChatLanguageModel abstraction | Yes | Full support |
| StreamingChatLanguageModel | Yes | Full support |
| MessageWindowChatMemory | Yes | Full support |
| **Providers** | | |
| Anthropic Claude (3, 3.5) | Yes | Tools, streaming |
| AWS Bedrock (Claude, Nova, Titan) | Yes | Tools, streaming |
| OpenAI (GPT-4) | Yes | Full support |
| Ollama (local LLMs) | Yes | Full support |
| Google Gemini | Yes | Basic (no streaming) |
| **RAG & Embeddings** | | |
| EmbeddingModel interface | Yes | Full support |
| EmbeddingStore interface | Yes | Full support |
| ContentRetriever | Yes | Full support |
| InMemoryEmbeddingStore | Yes | For development |
| pgvector | Yes | Production storage |
| **Advanced Features** | | |
| Structured outputs (records) | Yes | Full support |
| Function calling | Yes | Full support |
| Tool calling in streaming | Yes | Added in 0.35.0 |
| Observability (listeners) | Partial | Basic events only |

### Features Unavailable Until Java 17 Migration

| Feature | Requires | Impact | ADR Affected |
|---------|----------|--------|--------------|
| **MCP Support** | 1.0+ | Cannot integrate MCP servers | ADR-003 |
| **Extended Thinking** | 1.0+ | No Claude thinking/reasoning timeline | ADR-033 |
| **System Message Caching** | 0.36+ | Higher costs (no cache hits) | ADR-013 |
| **Tool Result Caching** | 0.36+ | Higher costs (repeated calls) | ADR-013 |
| **Google Gemini Streaming** | 0.36+ | No streaming for Gemini | ADR-034 |
| **Enhanced Observability** | 0.36+ | Limited listener events | ADR-013 |
| **ChatModelListener extensions** | 0.36+ | Basic metrics only | ADR-013 |
| **Coherence EmbeddingStore** | 0.36+ | Use pgvector instead | ADR-026 |
| **Mistral Moderation** | 0.36+ | Use Anthropic or custom | ADR-014 |
| **Latest Security Patches** | 1.x | Risk of unpatched vulnerabilities | All |

---

## ADR Impact Analysis

### ADRs Fully Implementable on Java 11 / LangChain4j 0.35.0

| ADR | Title | Status |
|-----|-------|--------|
| ADR-002 | LangChain4j Strategic Adoption | Partial (0.35.0 scope) |
| ADR-004 | Java Agent Framework | Full |
| ADR-006 | Data Model Architecture | Full |
| ADR-007 | Database Security Model | Full |
| ADR-008 | LLM Instruction Following | Full |
| ADR-009 | Domain Boundaries | Full |
| ADR-010 | Agent Orchestration | Full |
| ADR-011 | Specialized Agent Scopes | Full |
| ADR-012 | RAG-Based Context Retrieval | Full |
| ADR-014 | Guardrails and Safety | Partial (no Mistral moderation) |
| ADR-015 | Conversational UX Patterns | Full |
| ADR-016 | Knowledge Base Agent | Full |
| **ADR-017** | Chart Executive Overview | **Full** |
| **ADR-018** | Sales Opportunity Summary | **Full** |
| **ADR-019** | Support Ticket Classification | **Full** |
| **ADR-020** | Email Gateway Enhancement | **Full** |
| ADR-021 | Product Catalog Enhancement | Full |
| ADR-022 | Translation Wizard | Full |
| ADR-023 | OCR Invoice Processing | Full |
| ADR-024 | Import Data Normalization | Full |
| ADR-026 | Vector Database Strategy | Full |

### ADRs Blocked or Limited Until Java 17

| ADR | Title | Blocker | Workaround |
|-----|-------|---------|------------|
| **ADR-003** | MCP Server Integration | MCP requires LangChain4j 1.0+ | HTTP API bridge (custom) |
| **ADR-013** | Observability & Cost Tracking | Limited listener events | Custom metrics tracking |
| **ADR-033** | Streaming Thinking Timeline | Extended thinking requires 1.0+ | Basic streaming only |
| ADR-034 | Google Gemini Provider | No streaming in 0.35.0 | Use sync calls or defer |

---

## Migration Phases

### Phase 1: MVP on Java 11 (Current - Q1 2026)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PHASE 1: MVP on Java 11                              │
│                    LangChain4j 0.35.0                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Runtime: Amazon Corretto 11                                            │
│  iDempiere: v10 (10.0.0-SNAPSHOT)                                       │
│  LangChain4j: 0.35.0 (last Java 11 compatible)                          │
│                                                                         │
│  Scope:                                                                 │
│  ✅ Complete ADR-017, ADR-018, ADR-019, ADR-020 (MVP use cases)         │
│  ✅ Full provider support (Anthropic, Bedrock, Ollama, OpenAI)          │
│  ✅ RAG with pgvector (ADR-012, ADR-026)                                │
│  ✅ Guardrails (ADR-014) with custom PII/injection detection            │
│  ⚠️  Basic observability (custom metrics, no enhanced listeners)        │
│  ❌ MCP integration deferred (ADR-003)                                  │
│  ❌ Extended thinking deferred (ADR-033)                                │
│                                                                         │
│  Goal: Production-ready MVP with Phase 1 use cases                      │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Immediate Actions:**

1. Update `pom.xml` to LangChain4j 0.35.0:
   ```xml
   <artifactItem>
       <groupId>dev.langchain4j</groupId>
       <artifactId>langchain4j</artifactId>
       <version>0.35.0</version>  <!-- Downgrade from 1.0.0-beta3 -->
   </artifactItem>
   ```

2. Update all LangChain4j artifacts to 0.35.0:
   - langchain4j-core
   - langchain4j-anthropic
   - langchain4j-bedrock
   - langchain4j-ollama
   - langchain4j-open-ai

3. Verify tests pass on Java 11

4. Document version constraint in CLAUDE.md

### Phase 2: Java 17 Migration (Q2 2026)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PHASE 2: Java 17 Migration                           │
│                    LangChain4j 1.x (Latest Stable)                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Prerequisites:                                                         │
│  □ iDempiere upgraded to Release-11                                     │
│  □ Java runtime upgraded to 17+ (Corretto 17 or 21)                     │
│  □ Hazelcast configuration migrated (IDEMPIERE-5816)                    │
│  □ System user ID migration verified (0 → 10)                           │
│                                                                         │
│  Migration Steps:                                                       │
│  1. Update JAVA_HOME to Corretto 17                                     │
│  2. Update pom.xml to LangChain4j 1.9.x (latest stable)                 │
│  3. Run full test suite                                                 │
│  4. Verify API compatibility (minimal changes expected)                 │
│  5. Enable enhanced features                                            │
│                                                                         │
│  Unlocks:                                                               │
│  ✅ MCP Server Integration (ADR-003)                                    │
│  ✅ Extended Thinking Timeline (ADR-033)                                │
│  ✅ Enhanced Observability Listeners (ADR-013)                          │
│  ✅ System/Tool Caching (cost reduction)                                │
│  ✅ Google Gemini Streaming (ADR-034)                                   │
│  ✅ Latest security patches                                             │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**iDempiere Release-11 Migration Checklist:**

Per [iDempiere Migration Notes](https://wiki.idempiere.org/en/Migration_Notes):

- [ ] Hazelcast configuration migration (IDEMPIERE-5816)
  - Run setup again to use new hazelcast.xml template
  - Apply customizations to v5.3 template format
- [ ] System user ID change verification (0 → 10)
  - Audit custom code referencing AD_User_ID = 0
  - Update references to AD_User_ID = 10
- [ ] HTTPS requirement for WebUI
  - Verify SSL certificates configured
  - Update any http:// references to https://
- [ ] Database compatibility check
  - PostgreSQL version requirements
  - Schema migration scripts

### Phase 3: Advanced Features (Q3 2026+)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PHASE 3: Advanced Features                           │
│                    LangChain4j 1.x + Latest Capabilities                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Advanced Implementations:                                              │
│  ✅ MCP Server for external tool integration (ADR-003)                  │
│  ✅ Claude extended thinking with timeline UX (ADR-033)                 │
│  ✅ Advanced RAG patterns (multi-index, hybrid search)                  │
│  ✅ Multi-agent orchestration with LangGraph-style state                │
│  ✅ A/B testing with version management (ADR-027)                       │
│  ✅ Production cost optimization via caching                            │
│                                                                         │
│  Future Considerations:                                                 │
│  □ Java 21 migration (virtual threads, pattern matching)                │
│  □ GraalVM native compilation for faster startup                        │
│  □ LangChain4j 2.x when available                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Pros and Cons of Options

### Option 1: Downgrade to LangChain4j 0.35.0 (Chosen)

**Phased migration: 0.35.0 now, upgrade with Java 17**

- Good, because it unblocks immediate development
- Good, because all MVP use cases (ADR-017 to ADR-020) are fully supported
- Good, because API is stable between 0.35 and 1.x (minimal migration effort later)
- Good, because aligns upgrade with iDempiere Release-11 timeline
- Neutral, because some advanced features deferred
- Bad, because no access to latest security patches until migration
- Bad, because MCP integration blocked until Phase 2

### Option 2: Immediate Java 17 Migration

**Upgrade iDempiere to Release-11 immediately**

- Good, because access to all LangChain4j features immediately
- Good, because better long-term security posture
- Bad, because requires iDempiere Release-11 migration first (significant effort)
- Bad, because production stability risk during dual migration
- Bad, because longer time to MVP delivery
- Bad, because Hazelcast, System user, HTTPS migrations required first

### Option 3: Fork LangChain4j 0.35.0

**Maintain custom fork with security patches**

- Good, because stays on Java 11 indefinitely
- Good, because can backport specific features
- Bad, because massive maintenance burden
- Bad, because diverges from community
- Bad, because security patches require manual review and integration
- Bad, because no community support for custom fork

### Option 4: Replace LangChain4j with Custom Implementation

**Return to original custom provider architecture**

- Good, because full control over Java version requirements
- Good, because no external dependencies
- Bad, because massive development effort (~40+ hours)
- Bad, because loses all LangChain4j ecosystem benefits
- Bad, because contradicts ADR-002 decision
- Bad, because maintenance burden for provider updates

---

## pom.xml Changes Required

### Current (Incompatible)

```xml
<!-- Current: LangChain4j 1.0.0-beta3 REQUIRES Java 17 -->
<artifactItem>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>1.0.0-beta3</version>
</artifactItem>
```

### Target (Java 11 Compatible)

```xml
<!-- Target: LangChain4j 0.35.0 - Last Java 11 compatible version -->
<artifactItem>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.35.0</version>
</artifactItem>
<artifactItem>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-core</artifactId>
    <version>0.35.0</version>
</artifactItem>
<artifactItem>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-anthropic</artifactId>
    <version>0.35.0</version>
</artifactItem>
<artifactItem>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-bedrock</artifactId>
    <version>0.35.0</version>
</artifactItem>
<artifactItem>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>0.35.0</version>
</artifactItem>
<artifactItem>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.35.0</version>
</artifactItem>
```

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Security vulnerability in 0.35.0 | Medium | High | Monitor CVE databases, plan expedited Java 17 migration if critical |
| API breaking changes in 1.x | Low | Medium | LangChain4j maintains backward compatibility; test thoroughly |
| iDempiere Release-11 delay | Medium | Medium | 0.35.0 provides full MVP capability; can wait |
| Performance issues on Java 11 | Low | Low | 0.35.0 optimized for Java 11; no degradation expected |
| Missing feature for use case | Low | Medium | 0.35.0 covers all Phase 1 ADRs; advanced features can wait |

---

## Success Metrics

### Phase 1 (Java 11 / LangChain4j 0.35.0)

- [ ] All MVP use cases (ADR-017 to ADR-020) functional
- [ ] Test suite passes on Java 11
- [ ] Production deployment successful
- [ ] Cost tracking operational (custom metrics)
- [ ] No security incidents

### Phase 2 (Java 17 / LangChain4j 1.x)

- [ ] iDempiere Release-11 migration complete
- [ ] LangChain4j upgraded to latest stable
- [ ] MCP integration functional (ADR-003)
- [ ] Extended thinking UX implemented (ADR-033)
- [ ] Enhanced observability active
- [ ] All tests pass on Java 17

---

## Related ADRs

- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j Strategic Adoption (updated scope)
- [ADR-003](003-mcp-server-integration.md) - MCP Server Integration (blocked until Phase 2)
- [ADR-004](004-java-agent-framework.md) - Java Agent Framework Selection
- [ADR-013](013-observability-cost-tracking.md) - Observability (limited until Phase 2)
- [ADR-027](027-implementation-roadmap-priority.md) - Implementation Roadmap (version constraints added)
- [ADR-033](033-streaming-thinking-timeline-ux.md) - Streaming Thinking (blocked until Phase 2)
- [ADR-034](034-google-gemini-provider-integration.md) - Google Gemini (streaming blocked until Phase 2)

---

## References

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [LangChain4j GitHub Releases](https://github.com/langchain4j/langchain4j/releases)
- [LangChain4j Java 17 Migration Issue](https://github.com/langchain4j/langchain4j/issues/1652)
- [iDempiere Migration Notes](https://wiki.idempiere.org/en/Migration_Notes)
- [iDempiere Release-11 Requirements](https://wiki.idempiere.org/en/Migration_Notes#release-11)
- [Java 11 to 17 Migration Guide](https://bell-sw.com/blog/migration-from-java-11-to-java-17/)

---

**ADR-035 | Version 1.0 | 2025-12-04**
**Status: Accepted**
**Strategy: Phased Migration - Java 11/0.35.0 MVP, then Java 17/1.x**
