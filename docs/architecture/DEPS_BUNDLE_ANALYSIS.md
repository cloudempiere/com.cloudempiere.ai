# Deps Bundle Architecture Analysis

**Date**: 2026-02-02
**Context**: CLD-1704 Migration from monolithic to modular architecture
**Decision**: AWS SDK and Netty dependencies placed in `ai.core` instead of `ai.deps`

---

## Executive Summary

The initial architectural vision was to have a **separate deps bundle** to avoid embedding any third-party JARs in the core business logic bundle. However, during implementation, we discovered that AWS SDK dependencies **must** be in `ai.core` due to OSGi classloading and circular dependency constraints.

This document explains:
1. Why AWS SDK is in `ai.core` instead of `ai.deps`
2. Whether this challenges our architectural direction
3. Pros/cons of the deps pattern in general OSGi/iDempiere context

---

## Why AWS SDK is in ai.core (Not ai.deps)

### The Problem: OSGi Classloading with Transitive Dependencies

```
BedrockRuntimeAsyncClient (AWS SDK class)
    └─ extends/implements AwsClient (from aws-core JAR)
```

**Scenario 1**: If `ai.deps` embeds bedrock JARs and exports packages:
```
ai.deps exports: software.amazon.awssdk.services.bedrockruntime
ai.core imports: bedrockruntime packages from ai.deps

Runtime execution:
1. ai.core instantiates BedrockRuntimeAsyncClient
2. OSGi classloader loads class from ai.deps (the exporter)
3. Class requires AwsClient from aws-core
4. ai.deps doesn't have aws-core → ClassNotFoundException ❌
```

**Scenario 2**: If `ai.deps` requires `aws.provider` to access aws-core:
```
ai.deps → aws.provider → org.adempiere.base
ai.core → ai.deps → ...

Problem: Creates circular dependency in workspace
(Eclipse PDE detects cycles during workspace resolution)
```

**Solution**: AWS SDK in `ai.core`
```
ai.core embeds: bedrockruntime, bedrock, netty JARs
ai.core requires: aws.provider (for aws-core)
ai.core exports: bedrock and netty packages

Runtime execution:
1. ai.core instantiates BedrockRuntimeAsyncClient
2. OSGi classloader loads class from ai.core (owner bundle)
3. Class requires AwsClient from aws-core
4. ai.core requires aws.provider → AwsClient available ✅
```

### Why Netty is Also in ai.core

Netty transport JARs are **dependencies of AWS SDK netty-nio-client**, not LangChain4j:
- `netty-nio-client` (AWS SDK async HTTP client) depends on:
  - `io.netty.channel.socket.nio.NioSocketChannel`
  - `io.netty.buffer.*`
  - `io.netty.handler.*`

Since the AWS SDK's netty-nio-client is in ai.core, core Netty JARs must also be in ai.core to satisfy dependencies.

### Why Reactive Streams is Also in ai.core

Reactive Streams (`reactive-streams-1.0.4.jar`) is a **dependency of AWS SDK async classes**:
- `software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher` implements `org.reactivestreams.Publisher`
- AWS SDK uses reactive streams for async HTTP response handling
- If reactive-streams is in deps, OSGi sees incompatible interface implementations (IncompatibleClassChangeError)

Since AWS SDK classes in ai.core implement reactive-streams interfaces, the reactive-streams JAR must be in ai.core to share the same classloader.

**Pattern Summary:**
```
ai.core embeds:
  - AWS Bedrock JARs (bedrockruntime, bedrock)
  - AWS Netty HTTP client (netty-nio-client)
  - Core Netty JARs (10 JARs for async I/O)
  - Reactive Streams (for AWS SDK async interfaces)

All AWS-specific dependencies stay together in ai.core.
```

---

## Does This Challenge Our Architectural Direction?

### Initial Vision
> **Separate deps bundle to avoid embedding any third-party JARs in core**

**Answer**: No, it does NOT fundamentally challenge the vision. Here's why:

### What We Achieved
1. **Framework vs Integration Separation** ✅
   - `ai.deps` = LangChain4j framework (provider-agnostic)
   - `ai.core` = AWS-specific integration layer
   - Clear separation of concerns maintained

2. **Reusability** ✅
   - `ai.deps` can be reused by any plugin needing LangChain4j
   - AWS-specific code isolated to ai.core
   - If we add Google Vertex AI, we'd create `ai.google` (not pollute deps)

3. **Dependency Management** ✅
   - Core iDempiere framework deps (org.adempiere.base) → separate
   - LangChain4j framework → ai.deps
   - AWS SDK → ai.core (provider-specific)
   - Clean layering preserved

### What Changed from Vision
- **Expected**: All third-party JARs in deps
- **Reality**: Provider-specific JARs in integration layer (core)
- **Why it's OK**: Integration layer is the natural home for provider-specific SDKs

---

## Pros and Cons: Deps Pattern in OSGi/iDempiere

### OSGi Best Practices Context

According to OSGi and iDempiere best practices (see `idempiere-osgi-p2` skill):

**Pattern 1**: Embed in plugin lib/ (1-5 JARs, private use)
**Pattern 2**: Shared deps plugin (5+ JARs, multiple consumers)
**Pattern 3**: Import from workspace provider (aws.provider, org.adempiere.base)

Our architecture uses **all three patterns** appropriately:
- Pattern 1: AWS SDK in ai.core (3 bedrock JARs + 11 netty JARs + 1 reactive-streams = 15 JARs)
- Pattern 2: LangChain4j in ai.deps (framework shared by sales/inventory/kb agents)
- Pattern 3: Import aws-core from aws.provider

---

## Pros of Deps Bundle Pattern

### ✅ Advantages

1. **Single Source of Truth**
   - LangChain4j version managed in one place
   - All domain agents (sales, inventory, purchasing, kb) use same version
   - Prevents version conflicts across plugins

2. **Reduced Bundle Size**
   - Domain agents (sales, inventory, etc.) are lightweight
   - Only contain business logic, not framework JARs
   - P2 repository size reduced (JARs not duplicated 5x)

3. **Framework Isolation**
   - Can upgrade LangChain4j without touching domain agent code
   - Can swap underlying framework (e.g., LangGraph) by changing deps
   - Domain agents depend on abstractions, not implementations

4. **Clear Dependency Graph**
   ```
   sales → deps → base
   inventory → deps → base
   purchasing → deps → base
   kb → deps → base
   ```
   Linear, predictable, easy to reason about

5. **Reusability Across Projects**
   - Other Cloudempiere plugins can reuse ai.deps
   - Can be extracted to separate repository
   - Becomes a "Cloudempiere LangChain4j integration bundle"

---

## Cons of Deps Bundle Pattern

### ❌ Disadvantages

1. **Architectural Complexity**
   - Extra bundle to maintain (pom.xml, MANIFEST.MF, build.properties)
   - More complex than monolithic "embed everything in core"
   - Requires understanding OSGi classloading nuances

2. **Provider-Specific SDKs Still in Core**
   - AWS SDK cannot go in deps (circular dependency)
   - Netty cannot go in deps (transitive dependency of AWS)
   - Pattern breaks down for provider-specific libraries

3. **Potential Circular Dependency Traps**
   - If deps needs workspace provider (aws.provider), creates cycle
   - Must carefully analyze dependency graph
   - Eclipse PDE workspace resolution can be fragile

4. **Version Coupling**
   - All consumers (sales, inventory, etc.) locked to same LangChain4j version
   - Cannot have sales using LangChain4j 0.35.0 and inventory using 1.0.0
   - Requires coordination for upgrades

5. **OSGi Classloader Overhead**
   - Extra bundle = extra classloader
   - Slight memory overhead (~few KB)
   - Minor performance impact (negligible in practice)

---

## Alternative: No Deps Bundle (Embed Everything in Core)

### Hypothetical Architecture
```
ai.core:
  - LangChain4j JARs
  - AWS SDK JARs
  - Netty JARs
  - Anthropic SDK JARs
  - All supporting libs

ai.sales → ai.core
ai.inventory → ai.core
ai.kb → ai.core
```

### Pros of No-Deps Alternative
- Simpler structure (one less bundle)
- No circular dependency risks
- All dependencies in one place
- Easier for newcomers to understand

### Cons of No-Deps Alternative
- ai.sales/inventory/kb have heavier transitive dependency graph
- Bundle size grows (if sales doesn't use AWS, still imports it transitively)
- Harder to swap LangChain4j version per domain
- Less modular architecture

---

## General OSGi/iDempiere Best Practice Recommendation

### When to Use Deps Bundle Pattern

✅ **USE** a separate deps bundle when:
1. **Shared by 5+ plugins** in your project
2. **Framework-level dependencies** (LangChain4j, Apache Camel, Quarkus)
3. **Provider-agnostic** (not tied to AWS, Anthropic, etc.)
4. **Stable API** (domain agents depend on interfaces)
5. **No workspace provider dependencies** (no aws.provider, no circular deps)

❌ **DON'T USE** a separate deps bundle when:
1. **Provider-specific SDKs** (AWS SDK, Anthropic SDK) → embed in integration layer
2. **Few consumers** (1-2 plugins) → embed in plugin lib/
3. **Already in org.adempiere.base** → use Import-Package
4. **Already in workspace provider** (aws.provider) → use Require-Bundle
5. **Creates circular dependencies** → refactor or embed

### iDempiere-Specific Guidance

For **Cloudempiere AI plugin** specifically:
- ✅ Deps bundle for LangChain4j (shared by 5+ agents)
- ✅ Core embeds AWS SDK (provider-specific, requires aws.provider)
- ✅ Agents import from deps (lightweight domain bundles)

For **general iDempiere plugins**:
- If < 5 JARs → embed in plugin lib/ (Pattern 1)
- If already in workspace → require provider (Pattern 3)
- If 5+ JARs shared by multiple plugins → create deps bundle (Pattern 2)

---

## Conclusion

### Our Architecture is Correct

The final architecture **correctly applies OSGi patterns**:
- **LangChain4j** in `ai.deps` (shared framework)
- **AWS SDK** in `ai.core` (provider-specific, requires aws.provider)
- **Domain agents** lightweight (require deps + core)

### Deps Pattern is Valuable

The deps bundle pattern provides:
- Framework isolation and reusability
- Single source of truth for LangChain4j
- Lightweight domain agent bundles

### Provider-Specific SDKs Belong in Integration Layer

It's **architecturally sound** to have:
- Framework wrappers in deps
- Provider SDKs in core
- This is separation by **concern** (framework vs provider), not just "all JARs in one place"

### Recommendation

**Keep the current architecture.** It represents the correct balance between:
- Modularity (separate deps bundle)
- Pragmatism (AWS SDK in core to avoid circular deps)
- OSGi best practices (pattern selection based on use case)

---

## References

- CLD-1704: Monolithic to modular migration
- OSGi best practices: See `idempiere-osgi-p2` skill documentation
- iDempiere plugin development: https://wiki.idempiere.org/
- Commit: `7a9831e` - refactor(deps): move AWS SDK and Netty from deps to core bundle
