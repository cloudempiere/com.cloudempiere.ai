# OSGi Multi-Plugin Architecture for CloudEmpiere AI

**Date:** 2026-01-30
**Status:** Architectural Design
**Context:** Modular plugin architecture per domain boundary (ADR-009, ADR-011)

---

## Executive Summary

This document defines the migration from a **monolithic plugin** (`com.cloudempiere.ai.plugin`) to a **modular multi-plugin architecture** with:

- **1 Core Plugin** - Shared AI infrastructure
- **1 Shared Dependencies Plugin** - LangChain4j, AWS SDK, Anthropic SDK
- **5 Domain Plugins** - Sales, Inventory, Purchasing, Support, Knowledge Base
- **P2 Composite Repository** - Unified distribution

**Benefits:**
- 🔒 **Domain Isolation** - Each domain has independent lifecycle
- 📦 **Dependency Management** - Single source of truth for shared libs
- 🚀 **Selective Deployment** - Install only needed domains
- 🔄 **Independent Updates** - Update Sales without touching Inventory
- ✅ **Testability** - Test domains in isolation

---

## Current Monolithic Architecture (Problems)

```
com.cloudempiere.ai.plugin/
├── lib/                                    ❌ Problem: All deps in one bundle
│   ├── langchain4j-*.jar (15+ JARs)        ❌ ~10MB embedded
│   ├── anthropic-sdk-*.jar                 ❌ Duplicated if other plugins need
│   └── aws-sdk-*.jar (20+ JARs)            ❌ ~15MB embedded
├── src/
│   └── com/cloudempiere/ai/
│       ├── provider/                       ✅ OK: Core infrastructure
│       ├── database/                       ✅ OK: Core infrastructure
│       ├── context/                        ✅ OK: Core infrastructure
│       ├── agent/                          ❌ Problem: Mixed domain logic
│       │   ├── SalesAgent.java             ❌ Should be separate plugin
│       │   ├── InventoryAgent.java         ❌ Should be separate plugin
│       │   └── SupportAgent.java           ❌ Should be separate plugin
│       ├── tools/                          ❌ Problem: Mixed domain tools
│       │   ├── ERPTools.java               ❌ Contains all domain tools
│       │   └── RagTools.java               ✅ OK: Core tool
│       └── util/                           ✅ OK: Core utilities
└── META-INF/MANIFEST.MF                    ❌ Problem: Single version for all
```

**Issues:**
1. **Tight Coupling** - Can't update Sales without rebuilding entire plugin
2. **Deployment Bloat** - Install all 25MB even if only using Knowledge Base
3. **Version Lock** - All domains share same version (e.g., 1.0.0)
4. **Testing Complexity** - Changes to Inventory can break Sales tests
5. **Dependency Duplication** - Future plugins can't reuse LangChain4j libs

---

## Target Multi-Plugin Architecture

### Plugin Structure Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       CLOUDEMPIERE AI ECOSYSTEM                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │ com.cloudempiere.ai.deps (Shared Dependencies)                 │    │
│  │ Version: 0.35.0.qualifier                                      │    │
│  │                                                                 │    │
│  │ Exports:                                                        │    │
│  │ • dev.langchain4j.*                                            │    │
│  │ • com.anthropic.sdk.*                                          │    │
│  │ • software.amazon.awssdk.services.bedrock.*                    │    │
│  │ • io.netty.* (for Bedrock async)                               │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                              ▲ Import-Package                           │
│  ┌───────────────────────────┴─────────────────────────────────────┐   │
│  │ com.cloudempiere.ai.core (Core Infrastructure)                  │   │
│  │ Version: 1.0.0.qualifier                                        │   │
│  │                                                                  │   │
│  │ Provides:                                                        │   │
│  │ • IAIProvider, IAIProviderFactory                               │   │
│  │ • AIRequest, AIResponse (DTOs)                                  │   │
│  │ • SecureDatabaseQueryExecutor                                   │   │
│  │ • AIContextProviderRegistry                                     │   │
│  │ • StreamingMarkdownRenderer                                     │   │
│  │ • SecuritySanitizer, MarkdownValidator                          │   │
│  │                                                                  │   │
│  │ Exports (API Packages):                                         │   │
│  │ • com.cloudempiere.ai.provider;version="1.0.0"                  │   │
│  │ • com.cloudempiere.ai.provider.dto;version="1.0.0"              │   │
│  │ • com.cloudempiere.ai.database;version="1.0.0"                  │   │
│  │ • com.cloudempiere.ai.context;version="1.0.0"                   │   │
│  │ • com.cloudempiere.ai.util;version="1.0.0"                      │   │
│  └──────────────────────────────────────────────────────────────────   │
│                              ▲ Import-Package                           │
│  ┌───────────────────────────┴─────────────────────────────────────┐   │
│  │                      DOMAIN PLUGINS                              │   │
│  ├──────────────────────────────────────────────────────────────────   │
│  │                                                                  │   │
│  │ ┌─────────────────────────────────────────────────────────┐    │   │
│  │ │ com.cloudempiere.ai.sales (Sales Domain)                │    │   │
│  │ │ Version: 1.0.0.qualifier                                │    │   │
│  │ │                                                          │    │   │
│  │ │ • SalesAgent (AiServices interface)                     │    │   │
│  │ │ • SalesTools (@Tool methods)                            │    │   │
│  │ │ • OpportunitySummaryAgent                               │    │   │
│  │ │                                                          │    │   │
│  │ │ Boundary: C_Order, C_Opportunity, C_BPartner           │    │   │
│  │ └─────────────────────────────────────────────────────────┘    │   │
│  │                                                                  │   │
│  │ ┌─────────────────────────────────────────────────────────┐    │   │
│  │ │ com.cloudempiere.ai.inventory (Inventory Domain)        │    │   │
│  │ │ Version: 1.0.0.qualifier                                │    │   │
│  │ │                                                          │    │   │
│  │ │ • InventoryAgent                                        │    │   │
│  │ │ • InventoryTools                                        │    │   │
│  │ │                                                          │    │   │
│  │ │ Boundary: M_Product, M_Warehouse, M_Storage            │    │   │
│  │ └─────────────────────────────────────────────────────────┘    │   │
│  │                                                                  │   │
│  │ ┌─────────────────────────────────────────────────────────┐    │   │
│  │ │ com.cloudempiere.ai.purchasing (Purchasing Domain)      │    │   │
│  │ │ Version: 1.0.0.qualifier                                │    │   │
│  │ │                                                          │    │   │
│  │ │ • PurchasingAgent                                       │    │   │
│  │ │ • PurchasingTools                                       │    │   │
│  │ │                                                          │    │   │
│  │ │ Boundary: C_Invoice, M_InOut                           │    │   │
│  │ └─────────────────────────────────────────────────────────┘    │   │
│  │                                                                  │   │
│  │ ┌─────────────────────────────────────────────────────────┐    │   │
│  │ │ com.cloudempiere.ai.support (Support Domain)            │    │   │
│  │ │ Version: 1.0.0.qualifier                                │    │   │
│  │ │                                                          │    │   │
│  │ │ • SupportAgent                                          │    │   │
│  │ │ • TicketClassificationAgent                             │    │   │
│  │ │ • SupportTools                                          │    │   │
│  │ │                                                          │    │   │
│  │ │ Boundary: R_Request                                     │    │   │
│  │ └─────────────────────────────────────────────────────────┘    │   │
│  │                                                                  │   │
│  │ ┌─────────────────────────────────────────────────────────┐    │   │
│  │ │ com.cloudempiere.ai.knowledge (Knowledge Base Domain)   │    │   │
│  │ │ Version: 1.0.0.qualifier                                │    │   │
│  │ │                                                          │    │   │
│  │ │ • KnowledgeBaseAgent                                    │    │   │
│  │ │ • ArticleSearchAgent                                    │    │   │
│  │ │ • KnowledgeTools                                        │    │   │
│  │ │                                                          │    │   │
│  │ │ Boundary: K_Entry, K_Category                          │    │   │
│  │ └─────────────────────────────────────────────────────────┘    │   │
│  └──────────────────────────────────────────────────────────────────   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Plugin Definitions

### 1. com.cloudempiere.ai.deps (Shared Dependencies)

**Purpose:** Single source of truth for LangChain4j, Anthropic SDK, AWS Bedrock dependencies

**Directory Structure:**
```
com.cloudempiere.ai.deps/
├── lib/
│   ├── langchain4j-core-0.35.0.jar
│   ├── langchain4j-anthropic-0.35.0.jar
│   ├── langchain4j-bedrock-0.35.0.jar
│   ├── langchain4j-ollama-0.35.0.jar
│   ├── langchain4j-open-ai-0.35.0.jar
│   ├── anthropic-sdk-2.10.0.jar
│   ├── aws-sdk-bedrock-2.20.162.jar
│   ├── aws-sdk-bedrockruntime-2.20.162.jar
│   ├── aws-sdk-core-2.20.162.jar
│   ├── netty-nio-client-2.20.162.jar
│   └── ... (all AWS SDK jars)
├── META-INF/MANIFEST.MF
├── build.properties
└── pom.xml
```

**MANIFEST.MF:**
```manifest
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: CloudEmpiere AI - Shared Dependencies
Bundle-SymbolicName: com.cloudempiere.ai.deps
Bundle-Version: 0.35.0.qualifier
Bundle-Vendor: CloudEmpiere
Bundle-RequiredExecutionEnvironment: JavaSE-11
Bundle-ClassPath: .,
 lib/langchain4j-core-0.35.0.jar,
 lib/langchain4j-anthropic-0.35.0.jar,
 lib/langchain4j-bedrock-0.35.0.jar,
 lib/langchain4j-ollama-0.35.0.jar,
 lib/langchain4j-open-ai-0.35.0.jar,
 lib/anthropic-sdk-2.10.0.jar,
 lib/aws-sdk-bedrock-2.20.162.jar,
 lib/aws-sdk-bedrockruntime-2.20.162.jar,
 lib/aws-sdk-core-2.20.162.jar,
 lib/netty-nio-client-2.20.162.jar
Export-Package: dev.langchain4j;version="0.35.0",
 dev.langchain4j.agent;version="0.35.0",
 dev.langchain4j.data.message;version="0.35.0",
 dev.langchain4j.memory;version="0.35.0",
 dev.langchain4j.model.chat;version="0.35.0",
 dev.langchain4j.model.embedding;version="0.35.0",
 dev.langchain4j.model.input;version="0.35.0",
 dev.langchain4j.model.output;version="0.35.0",
 dev.langchain4j.service;version="0.35.0",
 dev.langchain4j.store.embedding;version="0.35.0",
 com.anthropic.sdk;version="2.10.0",
 com.anthropic.sdk.models;version="2.10.0",
 software.amazon.awssdk.services.bedrock;version="2.20.162",
 software.amazon.awssdk.services.bedrockruntime;version="2.20.162"
```

**Version Strategy:**
- Bundle version matches LangChain4j version (0.35.0)
- When upgrading to Java 17 + LangChain4j 1.x, create new version (1.0.0)
- All domain plugins specify version range: `[0.35.0,1.0.0)`

---

### 2. com.cloudempiere.ai.core (Core Infrastructure)

**Purpose:** Shared AI infrastructure (providers, rendering, security, database access)

**Directory Structure:**
```
com.cloudempiere.ai.core/
├── src/
│   └── com/cloudempiere/ai/
│       ├── provider/
│       │   ├── IAIProvider.java                      (exported API)
│       │   ├── IAIProviderFactory.java               (exported API)
│       │   ├── AIProviderException.java              (exported API)
│       │   ├── dto/                                   (exported DTOs)
│       │   │   ├── AIRequest.java
│       │   │   ├── AIResponse.java
│       │   │   ├── AIMessage.java
│       │   │   ├── AITokenUsage.java
│       │   │   └── ...
│       │   ├── factory/
│       │   │   └── AIProviderFactory.java            (@Component service)
│       │   └── impl/
│       │       ├── AnthropicProvider.java            (internal)
│       │       └── BedrockStreamingChatModelWrapper.java (internal)
│       ├── database/
│       │   ├── SecureDatabaseQueryExecutor.java      (exported API)
│       │   └── dto/
│       │       ├── SecureQueryRequest.java
│       │       └── SecureQueryResult.java
│       ├── context/
│       │   ├── IAIContextProvider.java               (exported API)
│       │   ├── AIContextProviderRegistry.java        (@Component service)
│       │   └── impl/
│       │       └── WindowContextProvider.java        (internal)
│       ├── component/
│       │   ├── AIChatWidget.java                     (ZK UI component)
│       │   ├── AIChatStreamingMessage.java
│       │   └── AIChatBubble.java
│       ├── util/
│       │   ├── StreamingMarkdownRenderer.java        (exported API)
│       │   ├── SecuritySanitizer.java                (exported API)
│       │   ├── MarkdownValidator.java                (exported API)
│       │   ├── MarkdownSyntaxSanitizer.java          (exported API)
│       │   └── ZoomLinkProcessor.java                (internal)
│       └── model/
│           ├── MAIProvider.java                      (exported model)
│           └── MAIChatEntry.java                     (exported model)
├── OSGI-INF/                                          (auto-generated from @Component)
│   ├── AIProviderFactory.xml
│   └── AIContextProviderRegistry.xml
├── META-INF/MANIFEST.MF
├── build.properties
└── pom.xml
```

**MANIFEST.MF:**
```manifest
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: CloudEmpiere AI - Core Infrastructure
Bundle-SymbolicName: com.cloudempiere.ai.core;singleton:=true
Bundle-Version: 1.0.0.qualifier
Bundle-Vendor: CloudEmpiere
Bundle-RequiredExecutionEnvironment: JavaSE-11
Bundle-Activator: com.cloudempiere.ai.Activator
Service-Component: OSGI-INF/*.xml
Export-Package: com.cloudempiere.ai.provider;version="1.0.0",
 com.cloudempiere.ai.provider.dto;version="1.0.0",
 com.cloudempiere.ai.database;version="1.0.0",
 com.cloudempiere.ai.database.dto;version="1.0.0",
 com.cloudempiere.ai.context;version="1.0.0",
 com.cloudempiere.ai.util;version="1.0.0",
 com.cloudempiere.ai.model;version="1.0.0"
Import-Package: dev.langchain4j;version="[0.35.0,1.0.0)",
 dev.langchain4j.agent;version="[0.35.0,1.0.0)",
 dev.langchain4j.data.message;version="[0.35.0,1.0.0)",
 dev.langchain4j.memory;version="[0.35.0,1.0.0)",
 dev.langchain4j.model.chat;version="[0.35.0,1.0.0)",
 dev.langchain4j.service;version="[0.35.0,1.0.0)",
 com.anthropic.sdk;version="[2.10.0,3.0.0)",
 software.amazon.awssdk.services.bedrock;version="[2.20.0,3.0.0)",
 org.compiere.*,
 org.adempiere.*
```

**Exported Services (OSGi DS):**
- `IAIProviderFactory` - Factory for creating AI providers
- `AIContextProviderRegistry` - Registry for context providers

**Notes:**
- `singleton:=true` because provides UI components (ZK widgets)
- Internal packages (`.impl`, `.factory`) NOT exported
- Domain plugins depend on exported API packages only

---

### 3. com.cloudempiere.ai.sales (Sales Domain)

**Purpose:** Sales-specific AI agents and tools

**Directory Structure:**
```
com.cloudempiere.ai.sales/
├── src/
│   └── com/cloudempiere/ai/sales/
│       ├── agent/
│       │   ├── SalesAgent.java                   (@Component, AiServices interface)
│       │   └── OpportunitySummaryAgent.java
│       ├── tools/
│       │   └── SalesTools.java                   (@Tool methods)
│       ├── boundary/
│       │   └── SalesDomainBoundary.java          (table/action whitelist)
│       └── dto/
│           └── OpportunitySummary.java           (structured output)
├── OSGI-INF/                                      (auto-generated)
│   └── SalesAgent.xml
├── META-INF/MANIFEST.MF
├── build.properties
└── pom.xml
```

**MANIFEST.MF:**
```manifest
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: CloudEmpiere AI - Sales Domain
Bundle-SymbolicName: com.cloudempiere.ai.sales
Bundle-Version: 1.0.0.qualifier
Bundle-Vendor: CloudEmpiere
Bundle-RequiredExecutionEnvironment: JavaSE-11
Service-Component: OSGI-INF/*.xml
Import-Package: com.cloudempiere.ai.provider;version="[1.0.0,2.0.0)",
 com.cloudempiere.ai.provider.dto;version="[1.0.0,2.0.0)",
 com.cloudempiere.ai.database;version="[1.0.0,2.0.0)",
 com.cloudempiere.ai.util;version="[1.0.0,2.0.0)",
 dev.langchain4j.service;version="[0.35.0,1.0.0)",
 org.compiere.*,
 org.adempiere.*
```

**Domain Boundary (from ADR-009):**
```java
/**
 * Sales domain boundary - restricts AI access to sales tables only
 */
public class SalesDomainBoundary {

    public static final Set<String> READ_TABLES = Set.of(
        "C_Order", "C_OrderLine",
        "C_Opportunity", "C_OpportunityLine",
        "C_BPartner", "C_BPartner_Location",
        "M_Product"  // read-only for product info
    );

    public static final Set<String> WRITE_TABLES = Set.of(
        "C_Opportunity",      // Can create opportunities
        "C_OpportunityLine"   // Can add lines
        // C_Order NOT writable - only read existing orders
    );

    public static final Set<String> ALLOWED_ACTIONS = Set.of(
        "READ", "CREATE_DRAFT", "UPDATE_DRAFT"
        // NO DELETE, NO COMPLETE
    );
}
```

**Agent Implementation:**
```java
package com.cloudempiere.ai.sales.agent;

import org.osgi.service.component.annotations.*;
import dev.langchain4j.service.AiServices;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.sales.tools.SalesTools;

@Component(
    service = SalesAgent.class,
    immediate = true
)
public class SalesAgent {

    @Reference
    private volatile IAIProviderFactory providerFactory;

    private SalesAgentInterface agent;

    @Activate
    protected void activate() {
        IAIProvider provider = providerFactory.getProvider(/* config */);

        agent = AiServices.builder(SalesAgentInterface.class)
            .chatLanguageModel(provider.getChatModel())
            .tools(new SalesTools())
            .chatMemory(/* ... */)
            .build();
    }

    public String analyzeSalesOpportunity(int opportunityId) {
        return agent.analyze(opportunityId);
    }
}

interface SalesAgentInterface {
    @SystemMessage("You are a sales analyst AI...")
    String analyze(int opportunityId);
}
```

---

### 4-6. Other Domain Plugins

Similar structure for:
- **com.cloudempiere.ai.inventory** - Inventory domain (M_Product, M_Warehouse, M_Storage)
- **com.cloudempiere.ai.purchasing** - Purchasing domain (C_Invoice, M_InOut)
- **com.cloudempiere.ai.support** - Support domain (R_Request)
- **com.cloudempiere.ai.knowledge** - Knowledge Base domain (K_Entry, K_Category)

Each follows same pattern:
- Import core API packages
- Define domain boundary
- Implement domain-specific agents
- Register as OSGi service

---

## OSGi Service Contracts

### Service Discovery Pattern

**Core Plugin Provides:**
```java
// IAIProviderFactory service (exported)
@Component(service = IAIProviderFactory.class)
public class AIProviderFactory implements IAIProviderFactory {

    @Override
    public IAIProvider getProvider(int providerId) {
        // Factory logic
    }
}
```

**Domain Plugin Consumes:**
```java
// SalesAgent (imports IAIProviderFactory)
@Component(service = SalesAgent.class)
public class SalesAgent {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private volatile IAIProviderFactory providerFactory;

    @Activate
    protected void activate() {
        IAIProvider provider = providerFactory.getProvider(1);
        // Use provider
    }
}
```

### Service Contract Interfaces

**Core exports (API):**
```java
package com.cloudempiere.ai.provider;

public interface IAIProviderFactory {
    IAIProvider getProvider(int providerId);
    List<IAIProvider> getAllProviders();
}

public interface IAIProvider {
    AIResponse generateText(AIRequest request) throws AIProviderException;
    String generateTextStreaming(AIRequest request, AIStreamCallback callback);
    // ...
}
```

**Domain plugins import:**
```java
import com.cloudempiere.ai.provider.IAIProviderFactory;
import com.cloudempiere.ai.provider.IAIProvider;
```

---

## P2 Composite Repository Structure

```
cloudempiere-ai-p2/
├── v10/
│   ├── latest/                                    (composite - development)
│   │   ├── compositeContent.xml
│   │   ├── compositeArtifacts.xml
│   │   └── p2.index
│   │
│   ├── stable/                                    (composite - production)
│   │   ├── compositeContent.xml
│   │   ├── compositeArtifacts.xml
│   │   └── p2.index
│   │
│   ├── deps/
│   │   └── 0.35.0/                                (LangChain4j 0.35.0 bundle)
│   │       └── repository/
│   │           ├── content.xml
│   │           ├── artifacts.xml
│   │           └── plugins/
│   │               └── com.cloudempiere.ai.deps_0.35.0.202601301200.jar
│   │
│   ├── core/
│   │   └── 1.0.1/                                 (Core infrastructure)
│   │       └── repository/
│   │           └── plugins/
│   │               └── com.cloudempiere.ai.core_1.0.1.202601301200.jar
│   │
│   └── domains/
│       ├── sales/1.0.0/repository/
│       ├── inventory/1.0.0/repository/
│       ├── purchasing/1.0.0/repository/
│       ├── support/1.0.0/repository/
│       └── knowledge/1.0.0/repository/
│
└── v11/                                           (future Java 17 + LangChain4j 1.x)
    └── latest/
        └── deps/
            └── 1.0.0/                             (LangChain4j 1.x bundle)
```

### compositeContent.xml (v10/latest)

```xml
<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='CloudEmpiere AI Latest (v10)'
    type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository'
    version='1.0.0'>
  <properties size='1'>
    <property name='p2.timestamp' value='1738233600000'/>
  </properties>
  <children size='6'>
    <child location='../deps/0.35.0'/>
    <child location='../core/1.0.1'/>
    <child location='../domains/sales/1.0.0'/>
    <child location='../domains/inventory/1.0.0'/>
    <child location='../domains/purchasing/1.0.0'/>
    <child location='../domains/support/1.0.0'/>
    <child location='../domains/knowledge/1.0.0'/>
  </children>
</repository>
```

### Selective Installation

**Install Core + Sales only:**
```bash
# Add repository
p2 install http://p2.cloudempiere.com/ai/v10/latest

# Install features
p2 install com.cloudempiere.ai.deps.feature
p2 install com.cloudempiere.ai.core.feature
p2 install com.cloudempiere.ai.sales.feature
```

**Install All Domains:**
```bash
p2 install com.cloudempiere.ai.all.feature  # meta-feature
```

---

## Migration Plan from Monolithic to Modular

### Phase 1: Extract Shared Dependencies (Week 1)

**Goal:** Create `com.cloudempiere.ai.deps` plugin

1. **Create new plugin:**
   ```bash
   mkdir -p com.cloudempiere.ai.deps/lib
   mkdir -p com.cloudempiere.ai.deps/META-INF
   ```

2. **Copy dependencies:**
   ```bash
   cp com.cloudempiere.ai.plugin/lib/langchain4j-*.jar com.cloudempiere.ai.deps/lib/
   cp com.cloudempiere.ai.plugin/lib/anthropic-*.jar com.cloudempiere.ai.deps/lib/
   cp com.cloudempiere.ai.plugin/lib/aws-sdk-*.jar com.cloudempiere.ai.deps/lib/
   ```

3. **Create MANIFEST.MF** (see template above)

4. **Create pom.xml:**
   ```xml
   <project>
     <modelVersion>4.0.0</modelVersion>
     <parent>
       <groupId>com.cloudempiere</groupId>
       <artifactId>ai-parent</artifactId>
       <version>1.0.0-SNAPSHOT</version>
     </parent>
     <artifactId>com.cloudempiere.ai.deps</artifactId>
     <version>0.35.0-SNAPSHOT</version>
     <packaging>eclipse-plugin</packaging>
   </project>
   ```

5. **Verify build:**
   ```bash
   cd com.cloudempiere.ai.deps
   mvn clean install
   ```

6. **Deploy to P2:**
   ```bash
   mvn deploy -Pdeploy-p2
   ```

---

### Phase 2: Refactor Core Plugin (Week 2)

**Goal:** Create `com.cloudempiere.ai.core` from monolithic plugin

1. **Create new plugin structure:**
   ```bash
   mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai/{provider,database,context,component,util,model}
   ```

2. **Move core packages:**
   ```bash
   # Move provider infrastructure (keep, remove agent impls)
   mv plugin/src/com/cloudempiere/ai/provider core/src/com/cloudempiere/ai/
   # Remove domain-specific agents (will go to domain plugins)
   rm -rf core/src/com/cloudempiere/ai/agent/Sales*.java
   rm -rf core/src/com/cloudempiere/ai/agent/Inventory*.java

   # Move database, context, util
   mv plugin/src/com/cloudempiere/ai/database core/src/com/cloudempiere/ai/
   mv plugin/src/com/cloudempiere/ai/context core/src/com/cloudempiere/ai/
   mv plugin/src/com/cloudempiere/ai/util core/src/com/cloudempiere/ai/
   mv plugin/src/com/cloudempiere/ai/component core/src/com/cloudempiere/ai/
   mv plugin/src/com/cloudempiere/ai/model core/src/com/cloudempiere/ai/
   ```

3. **Update MANIFEST.MF:**
   - Remove embedded lib/ (now Import-Package from deps)
   - Add Export-Package for API packages
   - Add Import-Package for LangChain4j

4. **Update dependencies:**
   ```bash
   # Remove lib/ JARs
   rm -rf com.cloudempiere.ai.core/lib

   # Update MANIFEST.MF to import from deps plugin
   Import-Package: dev.langchain4j;version="[0.35.0,1.0.0)"
   ```

5. **Verify build:**
   ```bash
   cd com.cloudempiere.ai.core
   mvn clean verify
   ```

---

### Phase 3: Create Domain Plugins (Week 3-4)

**For each domain (Sales, Inventory, Purchasing, Support, Knowledge):**

1. **Create plugin structure:**
   ```bash
   mkdir -p com.cloudempiere.ai.sales/src/com/cloudempiere/ai/sales/{agent,tools,boundary,dto}
   ```

2. **Extract domain code from monolithic plugin:**
   ```bash
   # Sales domain
   grep -r "class.*SalesAgent" plugin/src/ | awk '{print $1}' | xargs -I {} cp {} sales/src/...

   # Inventory domain
   grep -r "class.*InventoryAgent" plugin/src/ | awk '{print $1}' | xargs -I {} cp {} inventory/src/...
   ```

3. **Define domain boundary:**
   ```java
   // com.cloudempiere.ai.sales/src/.../boundary/SalesDomainBoundary.java
   public class SalesDomainBoundary {
       public static final Set<String> READ_TABLES = Set.of("C_Order", "C_Opportunity");
       public static final Set<String> WRITE_TABLES = Set.of("C_Opportunity");
       public static final Set<String> ALLOWED_ACTIONS = Set.of("READ", "CREATE_DRAFT");
   }
   ```

4. **Create MANIFEST.MF:**
   ```manifest
   Bundle-SymbolicName: com.cloudempiere.ai.sales
   Import-Package: com.cloudempiere.ai.provider;version="[1.0.0,2.0.0)",
    dev.langchain4j.service;version="[0.35.0,1.0.0)"
   ```

5. **Implement @Component service:**
   ```java
   @Component(service = SalesAgent.class, immediate = true)
   public class SalesAgent {
       @Reference
       private volatile IAIProviderFactory providerFactory;
   }
   ```

6. **Build and test:**
   ```bash
   mvn clean verify
   osgi> ss | grep sales
   ```

---

### Phase 4: P2 Repository Setup (Week 4)

1. **Create composite repository:**
   ```bash
   mkdir -p p2-repo/v10/{latest,stable,deps,core,domains}
   ```

2. **Generate compositeContent.xml** (see template above)

3. **Build individual repositories:**
   ```bash
   # Deps
   cd com.cloudempiere.ai.deps
   mvn clean install -Pp2-repository

   # Core
   cd ../com.cloudempiere.ai.core
   mvn clean install -Pp2-repository

   # Each domain
   cd ../com.cloudempiere.ai.sales
   mvn clean install -Pp2-repository
   ```

4. **Copy to composite:**
   ```bash
   cp -r com.cloudempiere.ai.deps/target/repository p2-repo/v10/deps/0.35.0/
   cp -r com.cloudempiere.ai.core/target/repository p2-repo/v10/core/1.0.1/
   cp -r com.cloudempiere.ai.sales/target/repository p2-repo/v10/domains/sales/1.0.0/
   ```

5. **Update composite metadata:**
   ```bash
   p2-admin update-composite v10/latest
   ```

6. **Test installation:**
   ```bash
   p2 install http://localhost:8080/p2-repo/v10/latest
   ```

---

## Testing Strategy

### Unit Tests (Per Plugin)

**Core Plugin:**
```bash
cd com.cloudempiere.ai.core
mvn test

# Tests:
# - AIProviderFactoryTest
# - SecureDatabaseQueryExecutorTest
# - StreamingMarkdownRendererTest
```

**Domain Plugin:**
```bash
cd com.cloudempiere.ai.sales
mvn test

# Tests:
# - SalesAgentTest (mock IAIProviderFactory)
# - SalesToolsTest
# - SalesDomainBoundaryTest
```

### Integration Tests (OSGi Container)

```java
@RunWith(PaxExam.class)
public class SalesAgentIntegrationTest {

    @Inject
    @Filter("(component.name=com.cloudempiere.ai.sales.agent.SalesAgent)")
    private SalesAgent salesAgent;

    @Test
    public void testOpportunitySummary() {
        String summary = salesAgent.analyzeSalesOpportunity(1000);
        assertNotNull(summary);
    }
}
```

### Dependency Isolation Test

Verify domain plugins can't access each other:

```java
@Test(expected = ClassNotFoundException.class)
public void testSalesCannotAccessInventory() {
    // From sales plugin bundle
    Class.forName("com.cloudempiere.ai.inventory.agent.InventoryAgent");
    // Should throw CNFE - not in Import-Package
}
```

---

## Deployment Scenarios

### Scenario 1: Full Installation (All Domains)

```bash
# Add P2 repo
p2 repo add http://p2.cloudempiere.com/ai/v10/latest

# Install meta-feature
p2 install com.cloudempiere.ai.all.feature

# Result:
# ✅ com.cloudempiere.ai.deps_0.35.0
# ✅ com.cloudempiere.ai.core_1.0.1
# ✅ com.cloudempiere.ai.sales_1.0.0
# ✅ com.cloudempiere.ai.inventory_1.0.0
# ✅ com.cloudempiere.ai.purchasing_1.0.0
# ✅ com.cloudempiere.ai.support_1.0.0
# ✅ com.cloudempiere.ai.knowledge_1.0.0
```

### Scenario 2: Minimal Installation (Core + Knowledge Base Only)

```bash
# Install only core + knowledge
p2 install com.cloudempiere.ai.deps.feature
p2 install com.cloudempiere.ai.core.feature
p2 install com.cloudempiere.ai.knowledge.feature

# Result:
# ✅ com.cloudempiere.ai.deps_0.35.0
# ✅ com.cloudempiere.ai.core_1.0.1
# ✅ com.cloudempiere.ai.knowledge_1.0.0
# ❌ Sales, Inventory, Purchasing, Support NOT installed
```

### Scenario 3: Staged Rollout (Core → Sales → Inventory)

```bash
# Week 1: Core only
p2 install com.cloudempiere.ai.core.feature

# Week 2: Add Sales
p2 install com.cloudempiere.ai.sales.feature

# Week 3: Add Inventory
p2 install com.cloudempiere.ai.inventory.feature

# No restarts needed - OSGi hot-deploys
```

---

## Version Management Strategy

### Semantic Versioning Per Plugin

| Plugin | Version | Rationale |
|--------|---------|-----------|
| com.cloudempiere.ai.deps | 0.35.0 | Matches LangChain4j version |
| com.cloudempiere.ai.core | 1.0.0 | Stable API, incremental updates |
| com.cloudempiere.ai.sales | 1.0.0 | Domain-specific, independent lifecycle |
| com.cloudempiere.ai.inventory | 1.0.0 | Can update without affecting sales |

### Version Ranges in Import-Package

**Core plugin imports deps:**
```manifest
Import-Package: dev.langchain4j;version="[0.35.0,1.0.0)"
```
- Minimum: 0.35.0 (required features)
- Maximum: <1.0.0 (breaking changes in 1.x)

**Domain plugin imports core:**
```manifest
Import-Package: com.cloudempiere.ai.provider;version="[1.0.0,2.0.0)"
```
- Minimum: 1.0.0 (API stability)
- Maximum: <2.0.0 (next major version)

### Java 17 Migration Path

**Current (Java 11):**
```
v10/deps/0.35.0     → LangChain4j 0.35.0
v10/core/1.0.x      → Uses 0.35.0 API
v10/domains/*/1.0.x → Uses core 1.0.x API
```

**After Java 17 Migration:**
```
v11/deps/1.0.0      → LangChain4j 1.9.x
v11/core/2.0.x      → Uses 1.9.x API (breaking changes)
v11/domains/*/2.0.x → Uses core 2.0.x API
```

**Migration Steps:**
1. Create `com.cloudempiere.ai.deps/1.0.0` with LangChain4j 1.9.x
2. Update `com.cloudempiere.ai.core` to 2.0.0 (API changes)
3. Update domain plugins to 2.0.0 (use new core API)
4. Create `v11/` P2 repository
5. Deprecate `v10/` (still available for Java 11 users)

---

## Documentation Requirements

### JavaDoc Standards (Per User Request)

**Every Java file must have:**
```java
/**
 * [Class name] - [Brief description]
 *
 * <p><b>Responsibility:</b> [What this class does]
 *
 * <p><b>Architecture Layer:</b> [Layer name from diagram below]
 * <pre>
 * Layer Interaction Flow:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ [ASCII diagram showing layer interaction]                   │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p><b>Dependencies:</b>
 * <ul>
 *   <li>IAIProviderFactory - for provider access</li>
 *   <li>SecureDatabaseQueryExecutor - for safe DB queries</li>
 * </ul>
 *
 * <p><b>Related ADRs:</b>
 * <ul>
 *   <li><a href="../../../docs/adr/009-domain-boundaries-agent-scope.md">ADR-009</a> - Domain Boundaries</li>
 *   <li><a href="../../../docs/adr/011-specialized-agent-scopes.md">ADR-011</a> - Specialized Agents</li>
 * </ul>
 *
 * @see IAIProviderFactory
 * @see SecureDatabaseQueryExecutor
 * @version 1.0.0
 * @since 1.0.0
 */
public class SalesAgent { ... }
```

**Every method must have:**
```java
/**
 * Analyzes a sales opportunity using AI and returns summary.
 *
 * <p><b>Flow:</b>
 * <pre>
 * 1. Validate opportunity ID
 * 2. Check domain boundary (SalesDomainBoundary.canRead())
 * 3. Query opportunity data (SecureDatabaseQueryExecutor)
 * 4. Build AI request (AIRequest.builder())
 * 5. Execute AI call (IAIProvider.generateText())
 * 6. Parse structured output (OpportunitySummary record)
 * 7. Return summary string
 * </pre>
 *
 * <p><b>Security:</b> Respects AD_Client_ID, AD_Org_ID, AD_Role_ID filters
 *
 * @param opportunityId C_Opportunity_ID to analyze
 * @return AI-generated opportunity summary
 * @throws AIProviderException if AI call fails
 * @throws SecurityException if user lacks access to opportunity
 */
public String analyzeSalesOpportunity(int opportunityId) { ... }
```

### Architecture Diagrams (ASCII)

Create **ARCHITECTURE_DIAGRAMS.md** with layer interaction flows (see next user message for this requirement).

---

## Rollback Plan

### Scenario: Domain Plugin Fails

If `com.cloudempiere.ai.sales/1.0.1` is broken:

```bash
# Uninstall broken version
osgi> uninstall com.cloudempiere.ai.sales

# Reinstall previous version
p2 install com.cloudempiere.ai.sales.feature/1.0.0

# Or keep using other domains (isolation benefit)
# Inventory, Purchasing, Support, Knowledge still functional
```

### Scenario: Core Plugin Breaking Change

If `com.cloudempiere.ai.core/1.1.0` breaks domains:

```bash
# Rollback core
p2 install com.cloudempiere.ai.core.feature/1.0.1

# Domain plugins automatically resolve to compatible core version
# Thanks to version ranges: [1.0.0,2.0.0)
```

### Scenario: Deps Plugin Upgrade

If `com.cloudempiere.ai.deps/0.36.0` (accidental Java 17 dep) installed:

```bash
# Won't work - version range blocks it
Import-Package: dev.langchain4j;version="[0.35.0,1.0.0)"
# 0.36.0 is outside range → resolution fails

# OSGi prevents installation automatically
```

---

## Summary of Benefits

| Benefit | Monolithic | Modular | Improvement |
|---------|-----------|---------|-------------|
| **Bundle Size** | 25MB | Core: 5MB, Domain: 1MB each | 80% smaller per domain |
| **Update Granularity** | All-or-nothing | Per domain | Selective updates |
| **Dependency Isolation** | Embedded in each | Shared deps plugin | No duplication |
| **Version Independence** | Single version | Independent versions | Parallel development |
| **Deployment Flexibility** | Install all | Install subset | Custom deployments |
| **Testing Isolation** | Coupled tests | Isolated tests | Faster CI/CD |
| **Rollback Granularity** | Entire system | Per domain | Reduced risk |

---

## Next Steps

1. **Week 1:** Extract deps plugin, deploy to P2
2. **Week 2:** Refactor core plugin, remove domain code
3. **Week 3-4:** Create 5 domain plugins
4. **Week 5:** Set up P2 composite repository
5. **Week 6:** Migration testing, documentation
6. **Week 7:** Staged production deployment

---

## Related Documents

- [ADR-009: Domain Boundaries](docs/adr/009-domain-boundaries-agent-scope.md)
- [ADR-011: Specialized Agent Scopes](docs/adr/011-specialized-agent-scopes.md)
- [ADR-027: Implementation Roadmap](docs/adr/027-implementation-roadmap-priority.md)
- [ADR-035: Java Version Strategy](docs/adr/035-java-version-strategy.md)

---

**Document Status:** ✅ Complete
**Implementation Status:** 📋 Planned
**Risk Level:** Medium (requires careful migration, but benefits outweigh risks)
