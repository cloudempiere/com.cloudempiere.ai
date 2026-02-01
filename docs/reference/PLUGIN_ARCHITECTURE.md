# Plugin Architecture Reference

**Last Updated:** 2026-01-30
**Version:** Phase 2 Week 4
**Status:** Core Infrastructure Complete

## Overview

The CloudEmpiere AI plugin uses a **multi-plugin OSGi architecture** to enforce domain boundaries, improve maintainability, and enable independent deployment of AI capabilities across different business domains.

## Why Multi-Plugin Architecture?

### 1. **Domain-Driven Design (ADR-009)**

Each business domain (Sales, Inventory, Purchasing, Support) has different:
- **Data access requirements** - Sales agents shouldn't query warehouse tables
- **Security boundaries** - Support agents can't modify financial records
- **Lifecycle cadence** - Inventory features may update weekly, sales monthly
- **Team ownership** - Different developers own different domains

A single monolithic plugin would mix all these concerns, creating security risks and maintenance nightmares.

### 2. **Dependency Management**

**Problem with monolithic approach:**
```
com.cloudempiere.ai.plugin (1 plugin)
├── Embeds 55 JARs (LangChain4j, AWS SDK, Jackson, etc.)
├── 15 MB plugin size
└── All domains share same library versions
```

**Multi-plugin solution:**
```
com.cloudempiere.ai.deps (1 shared plugin)
├── Embeds 55 JARs once
├── Exports packages to other plugins
└── Single source of truth for library versions

com.cloudempiere.ai.sales (depends on deps)
├── No embedded JARs
└── Imports LangChain4j from deps plugin

com.cloudempiere.ai.inventory (depends on deps)
├── No embedded JARs
└── Imports AWS SDK from deps plugin
```

**Benefits:**
- **No duplication** - 55 JARs stored once, not 5 times
- **Consistent versions** - All plugins use same LangChain4j 0.35.0
- **Smaller plugins** - Domain plugins only contain business logic
- **Faster updates** - Update shared library once, all plugins benefit

### 3. **Security Isolation (ADR-014)**

Each domain plugin extends `DomainBoundary` base class:

```java
public class SalesDomainBoundary extends DomainBoundary {
    protected void registerAllowedTables() {
        allow("C_Order");
        allow("C_OrderLine");
        allow("C_BPartner");
        allow("M_Product");  // Read-only for product lookup
    }
}
```

**Security enforcement:**
```java
salesAgent.query("SELECT * FROM C_Order WHERE ...");  // ✅ Allowed
salesAgent.query("SELECT * FROM M_Warehouse WHERE ...");  // ❌ SecurityException
```

**Why this matters:**
- AI agents can generate arbitrary SQL queries
- Without boundaries, a sales chatbot could leak warehouse locations
- With boundaries, each agent operates in its allowed "sandbox"

### 4. **Independent Deployment**

**Traditional monolithic plugin:**
- Bug fix in sales feature requires redeploying entire plugin
- Risk of breaking inventory features
- All domains tested together
- Longer QA cycles

**Multi-plugin architecture:**
- Bug fix in sales plugin only redeploys `com.cloudempiere.ai.sales`
- Inventory plugin unaffected
- Each domain tested independently
- Faster hotfix deployment

## Plugin Breakdown

### 1. **com.cloudempiere.ai.deps** - Shared Dependencies

**Purpose:** Central repository for all third-party libraries.

**Contents:**
- **55 embedded JARs** (Bundle-ClassPath)
- LangChain4j 0.35.0 (last Java 11 compatible version)
- AWS SDK 2.20.162 (Bedrock runtime, regions, HTTP client)
- Jackson 2.17.0 (JSON serialization)
- Kotlin 1.9.10 (LangChain4j dependency)
- OkHttp 4.12.0 (HTTP client)
- Netty 4.1.100 (async I/O)

**Exported Packages:**
```
dev.langchain4j.*;version="0.35.0"
software.amazon.awssdk.*;version="2.20.162"
com.fasterxml.jackson.*;version="2.17.0"
kotlin.*;version="1.9.10"
okhttp3.*;version="4.12.0"
io.netty.*;version="4.1.100"
```

**Why separate?**
- **Version consistency** - All plugins import from single source
- **Bundle resolution** - OSGi resolver finds packages from deps plugin
- **No duplication** - 55 JARs × 1 plugin = 15 MB, not 55 JARs × 5 plugins = 75 MB
- **Upgrade path** - Update LangChain4j version once, all plugins see it

**Analogy:** Like a shared `/usr/lib` directory in Linux - libraries installed once, used by many programs.

---

### 2. **com.cloudempiere.ai.core** - Core Infrastructure

**Purpose:** Shared business logic, provider interfaces, and utilities.

**Contents:**
- **Provider Framework** (`com.cloudempiere.ai.provider`)
  - `IAIProvider` interface
  - `LangChain4jProviderFactory` (Anthropic, Bedrock, Ollama)
  - Provider DTOs (AIRequest, AIResponse, AIMessage, AITokenUsage)

- **Database Security** (`com.cloudempiere.ai.database`)
  - `SecureDatabaseQueryExecutor` (role-based query execution)
  - Query audit logging
  - SQL injection prevention

- **Context Providers** (`com.cloudempiere.ai.context`)
  - `WindowContextProvider` (extracts iDempiere window data)
  - `ChartContextProvider` (extracts chart metadata)
  - `AIContextProviderRegistry`

- **Domain Boundary** (`com.cloudempiere.ai.boundary`)
  - `DomainBoundary` abstract base class
  - Table validation logic

- **Data Models** (`com.cloudempiere.ai.model`)
  - `MAIProvider` (AI provider configuration)
  - `MAIChat` (chat sessions)
  - `MAIUsageMetrics` (token tracking)

- **Utilities** (`com.cloudempiere.ai.util`)
  - `StreamingMarkdownRenderer` (ADR-054, ADR-055)
  - `MarkdownSyntaxSanitizer`
  - `ZoomLinkProcessor`

**Exported Packages:**
```
com.cloudempiere.ai;version="0.32.0"
com.cloudempiere.ai.boundary;version="0.32.0"
com.cloudempiere.ai.provider;version="0.32.0"
com.cloudempiere.ai.database;version="0.32.0"
com.cloudempiere.ai.context;version="0.32.0"
com.cloudempiere.ai.model;version="0.32.0"
com.cloudempiere.ai.util;version="0.32.0"
```

**Why separate from domain plugins?**
- **Shared logic** - All domains use same provider factory
- **Security core** - Database executor enforced centrally
- **API stability** - Core interfaces change less than domain logic
- **Testing** - Core can be tested independently of domains

**Analogy:** Like Spring Framework - provides infrastructure (dependency injection, transaction management) that applications build on top of.

---

### 3. **com.cloudempiere.ai.plugin** - Legacy Monolithic Plugin

**Status:** ⚠️ **Being phased out** - code migrating to core and domain plugins

**Current Contents:**
- UI components not yet migrated
- Process classes not yet migrated
- Form handlers not yet migrated

**Migration Strategy:**
1. Week 4 (✅ Complete): Extract core infrastructure to `com.cloudempiere.ai.core`
2. Week 5 (Next): Extract domain logic to domain plugins
3. Week 6: Deprecate monolithic plugin
4. Week 7: Remove monolithic plugin

**Why keep it temporarily?**
- **Gradual migration** - Less risk than big-bang rewrite
- **Backward compatibility** - Existing deployments still work
- **Incremental testing** - Test each extracted piece

---

### 4. **com.cloudempiere.ai.sales** - Sales Domain Plugin (Future - Week 5)

**Purpose:** AI capabilities for sales operations.

**Domain Scope:**
- **Tables:** `C_Order`, `C_OrderLine`, `C_BPartner`, `M_Product` (read-only)
- **Use Cases:**
  - Sales opportunity summaries (ADR-018)
  - Order analysis and recommendations
  - Customer interaction insights

**Boundary:**
```java
public class SalesDomainBoundary extends DomainBoundary {
    public SalesDomainBoundary() {
        super("Sales");
    }

    protected void registerAllowedTables() {
        allow("C_Order");
        allow("C_OrderLine");
        allow("C_BPartner");
        allow("M_Product");  // Read-only for product lookup
        // NOT allowed: M_Warehouse, GL_Journal, C_Payment
    }
}
```

**Security Model:**
- Sales agent can query orders and customers
- Sales agent **cannot** query warehouse locations (M_Warehouse)
- Sales agent **cannot** modify accounting (GL_Journal)
- Sales agent **cannot** process payments (C_Payment)

**Why separate plugin?**
- Sales team owns sales features
- Independent release cycle
- Security boundary enforcement
- Clear responsibility

---

### 5. **com.cloudempiere.ai.inventory** - Inventory Domain Plugin (Future - Week 5)

**Purpose:** AI capabilities for warehouse and inventory management.

**Domain Scope:**
- **Tables:** `M_Product`, `M_Warehouse`, `M_Storage`, `M_Movement`, `M_Inventory`
- **Use Cases:**
  - Stock level optimization
  - Warehouse space analysis
  - Inventory forecasting

**Boundary:**
```java
public class InventoryDomainBoundary extends DomainBoundary {
    public InventoryDomainBoundary() {
        super("Inventory");
    }

    protected void registerAllowedTables() {
        allow("M_Product");
        allow("M_Warehouse");
        allow("M_Storage");
        allow("M_Movement");
        allow("M_Inventory");
        // NOT allowed: C_Order, GL_Journal, C_Payment
    }
}
```

**Security Model:**
- Inventory agent can query stock levels and movements
- Inventory agent **cannot** query sales orders (C_Order)
- Inventory agent **cannot** query financial data (GL_Journal)

**Why separate from sales?**
- Different team ownership (warehouse vs sales)
- Different security requirements
- Inventory updates more frequently (daily stock checks vs weekly sales reports)

---

### 6. **com.cloudempiere.ai.purchasing** - Purchasing Domain Plugin (Future - Week 5)

**Purpose:** AI capabilities for procurement and purchasing.

**Domain Scope:**
- **Tables:** `C_Order` (purchase orders), `M_Requisition`, `C_BPartner` (vendors), `M_Product`
- **Use Cases:**
  - Requisition analysis
  - Vendor recommendations
  - Purchase order optimization

**Boundary:**
```java
public class PurchasingDomainBoundary extends DomainBoundary {
    public PurchasingDomainBoundary() {
        super("Purchasing");
    }

    protected void registerAllowedTables() {
        allow("C_Order");  // Purchase orders only (filtered by IsSOTrx='N')
        allow("M_Requisition");
        allow("C_BPartner");  // Vendors
        allow("M_Product");
        // NOT allowed: M_Warehouse, GL_Journal, C_Invoice
    }
}
```

**Why separate from sales?**
- Purchase orders vs sales orders (different workflows)
- Vendor management vs customer management
- Procurement team vs sales team

---

### 7. **com.cloudempiere.ai.support** - Support Domain Plugin (Future - Week 5)

**Purpose:** AI capabilities for customer support and request management.

**Domain Scope:**
- **Tables:** `R_Request`, `R_RequestAction`, `C_BPartner`, `M_Product`
- **Use Cases:**
  - Support ticket classification (ADR-019)
  - Automated ticket routing
  - Response suggestions

**Boundary:**
```java
public class SupportDomainBoundary extends DomainBoundary {
    public SupportDomainBoundary() {
        super("Support");
    }

    protected void registerAllowedTables() {
        allow("R_Request");
        allow("R_RequestAction");
        allow("C_BPartner");  // For customer context
        allow("M_Product");   // For product-related issues
        // NOT allowed: C_Order, GL_Journal, M_Warehouse
    }
}
```

**Why separate plugin?**
- Support team owns support features
- 24/7 support may require hotfixes
- Security: support should not see financial details

---

### 8. **com.cloudempiere.ai.kb** - Knowledge Base Domain Plugin (Future - Week 5)

**Purpose:** AI-powered knowledge base and documentation assistance.

**Domain Scope:**
- **Tables:** Knowledge base tables (to be defined), `AD_Table`, `AD_Column`, `AD_Window`
- **Use Cases:**
  - iDempiere development assistant (ADR-025)
  - Documentation search
  - Code generation suggestions

**Boundary:**
```java
public class KnowledgeBaseDomainBoundary extends DomainBoundary {
    public KnowledgeBaseDomainBoundary() {
        super("KnowledgeBase");
    }

    protected void registerAllowedTables() {
        allow("AD_Table");
        allow("AD_Column");
        allow("AD_Window");
        allow("AD_Process");
        // Knowledge base tables (TBD)
        // NOT allowed: Business tables (C_Order, etc.)
    }
}
```

**Why separate plugin?**
- Different lifecycle (documentation updates vs business logic)
- Can be disabled on production servers
- Development vs runtime concerns

---

## Plugin Dependencies

```
┌─────────────────────────────────────┐
│   com.cloudempiere.ai.deps          │
│   (55 JARs: LangChain4j, AWS, etc.) │
└─────────────┬───────────────────────┘
              │ Import-Package
              ↓
┌─────────────────────────────────────┐
│   com.cloudempiere.ai.core          │
│   (Provider, Database, Context)     │
└─────────────┬───────────────────────┘
              │ Import-Package
              ↓
┌─────────────────────────────────────┐
│   Domain Plugins (5 plugins)        │
│   - sales                           │
│   - inventory                       │
│   - purchasing                      │
│   - support                         │
│   - kb                              │
└─────────────────────────────────────┘
```

**Dependency Rules:**
1. **deps** has no dependencies (except iDempiere core)
2. **core** depends on **deps**
3. **Domain plugins** depend on **core** (and transitively **deps**)
4. **Domain plugins never depend on each other** (enforces isolation)

---

## Benefits Summary

| Benefit | Monolithic Plugin | Multi-Plugin Architecture |
|---------|-------------------|---------------------------|
| **JAR Duplication** | 55 JARs × 5 = 275 MB | 55 JARs × 1 = 55 MB |
| **Security Boundaries** | Manual SQL filtering | Enforced by DomainBoundary |
| **Team Ownership** | Shared codebase conflicts | Clear plugin ownership |
| **Deployment Risk** | Bug fix affects everything | Bug fix isolated to domain |
| **Test Scope** | Test entire plugin | Test single domain |
| **Release Cadence** | Synchronized releases | Independent releases |
| **Classpath Isolation** | All classes visible | OSGi enforces boundaries |

---

## Migration Path

### Phase 1: Foundation (✅ Complete)
- Core provider architecture
- Anthropic/AWS integration
- Security layer

### Phase 2: Multi-Plugin (In Progress)
- **Week 3 (✅):** Create deps plugin (55 JARs)
- **Week 4 (✅):** Create core plugin (74 files)
- **Week 5 (Next):** Create 5 domain plugins
- **Week 6:** Migrate monolithic plugin code to domain plugins
- **Week 7:** Deprecate monolithic plugin

### Phase 3: Domain Features
- Implement domain-specific agents
- Enable RAG service
- Enable guardrails pipeline

---

## References

- **ADR-009:** Domain Boundaries and Agent Scope Architecture
- **ADR-013:** Observability and Cost Tracking
- **ADR-014:** Guardrails and Safety
- **ADR-054:** HTML-only Chat Message Storage
- **ADR-055:** Constrained Markdown Syntax Support
- **OSGI_MULTI_PLUGIN_ARCHITECTURE.md:** Detailed multi-plugin design
- **PHASE_2_WEEK_4_COMPLETION.md:** Week 4 completion report

---

**Document Version:** 1.0
**Author:** CloudEmpiere AI Team
**Date:** 2026-01-30
