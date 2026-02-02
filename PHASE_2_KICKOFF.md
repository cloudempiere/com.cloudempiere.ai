# Phase 2 Kickoff - Multi-Plugin OSGi Architecture

**Date:** 2026-01-30
**Duration:** Weeks 3-10 (8 weeks)
**Goal:** Transform monolithic plugin into 7-plugin architecture with domain separation

---

## Executive Summary

Phase 2 transforms the current monolithic `com.cloudempiere.ai.plugin` into a modular architecture with:

- **1 Shared Dependencies Plugin** (`com.cloudempiere.ai.deps`)
- **1 Core Infrastructure Plugin** (`com.cloudempiere.ai.core`)
- **5 Domain-Specific Plugins** (sales, inventory, purchasing, support, knowledge)

**Key Benefits:**
- ✅ Domain isolation (ADR-009, ADR-011)
- ✅ Shared dependency management (OSGi Export-Package)
- ✅ Independent versioning per domain
- ✅ Reduced coupling, improved testability
- ✅ Easier maintenance and updates

---

## Architecture Overview

### Current State (Monolithic)

```
com.cloudempiere.ai.plugin/
├── lib/                          # Embedded JARs (LangChain4j, Anthropic, AWS)
├── src/com/cloudempiere/ai/
│   ├── provider/                 # Core: Provider layer
│   ├── database/                 # Core: Database security
│   ├── context/                  # Core: Context extraction
│   ├── component/                # Core: ZK UI components
│   ├── util/                     # Core: Utilities
│   ├── model/                    # Core: iDempiere models
│   ├── agent/                    # Domain: 5 agent classes (EXTRACT)
│   └── tools/                    # Domain: ERPTools.java (SPLIT)
```

### Target State (Multi-Plugin)

```
com.cloudempiere.ai.deps/         # Shared Dependencies (0.35.0)
├── lib/
│   ├── langchain4j-core-0.35.0.jar
│   ├── langchain4j-anthropic-0.35.0.jar
│   ├── anthropic-sdk-2.10.0.jar
│   ├── aws-sdk-bedrock-2.20.162.jar
│   └── ...
└── MANIFEST.MF
    Export-Package: dev.langchain4j.*;version="0.35.0"

com.cloudempiere.ai.core/         # Core Infrastructure
├── src/com/cloudempiere/ai/
│   ├── provider/                 # Provider layer
│   ├── database/                 # Database security
│   ├── context/                  # Context extraction
│   ├── component/                # ZK UI components
│   ├── util/                     # Utilities
│   ├── model/                    # iDempiere models
│   └── boundary/                 # NEW: Domain boundary enforcement
└── MANIFEST.MF
    Import-Package: dev.langchain4j.*
    Export-Package: com.cloudempiere.ai.*;version="0.32.0"

com.cloudempiere.ai.sales/        # Sales Domain
├── src/com/cloudempiere/ai/sales/
│   ├── agent/                    # SalesAgent
│   ├── tools/                    # SalesTools (C_BPartner, C_Order)
│   ├── boundary/                 # SalesDomainBoundary
│   └── chain/                    # LangChain4j chains
└── MANIFEST.MF
    Import-Package: com.cloudempiere.ai.*, dev.langchain4j.*

com.cloudempiere.ai.inventory/    # Inventory Domain
├── src/com/cloudempiere/ai/inventory/
│   ├── agent/                    # InventoryAgent
│   ├── tools/                    # InventoryTools (M_Product, M_Storage)
│   ├── boundary/                 # InventoryDomainBoundary
│   └── chain/                    # LangChain4j chains

com.cloudempiere.ai.purchasing/   # Purchasing Domain
├── src/com/cloudempiere/ai/purchasing/
│   ├── agent/                    # PurchasingAgent
│   ├── tools/                    # PurchasingTools (C_Invoice, C_Payment)
│   ├── boundary/                 # PurchasingDomainBoundary
│   └── chain/                    # LangChain4j chains

com.cloudempiere.ai.support/      # Support Domain
├── src/com/cloudempiere/ai/support/
│   ├── agent/                    # SupportAgent
│   ├── tools/                    # SupportTools (R_Request, AD_Note)
│   ├── boundary/                 # SupportDomainBoundary
│   └── chain/                    # LangChain4j chains

com.cloudempiere.ai.knowledge/    # Knowledge Base Domain
├── src/com/cloudempiere/ai/knowledge/
│   ├── agent/                    # KnowledgeBaseAgent
│   ├── tools/                    # KnowledgeTools (vector search)
│   ├── boundary/                 # KnowledgeDomainBoundary
│   └── chain/                    # LangChain4j chains
```

---

## Week-by-Week Breakdown

### Week 3: Extract Shared Dependencies Plugin

**Goal:** Create `com.cloudempiere.ai.deps` plugin with embedded LangChain4j, Anthropic, and AWS SDKs

**Tasks:**

#### 1. Create Plugin Structure

```bash
mkdir -p com.cloudempiere.ai.deps/lib
mkdir -p com.cloudempiere.ai.deps/META-INF
mkdir -p com.cloudempiere.ai.deps/OSGI-INF
```

#### 2. Copy JARs from Monolithic Plugin

```bash
# LangChain4j JARs
cp com.cloudempiere.ai.plugin/lib/langchain4j-core-*.jar com.cloudempiere.ai.deps/lib/
cp com.cloudempiere.ai.plugin/lib/langchain4j-anthropic-*.jar com.cloudempiere.ai.deps/lib/
cp com.cloudempiere.ai.plugin/lib/langchain4j-aws-bedrock-*.jar com.cloudempiere.ai.deps/lib/
cp com.cloudempiere.ai.plugin/lib/langchain4j-ollama-*.jar com.cloudempiere.ai.deps/lib/
cp com.cloudempiere.ai.plugin/lib/langchain4j-open-ai-*.jar com.cloudempiere.ai.deps/lib/

# Anthropic SDK
cp com.cloudempiere.ai.plugin/lib/anthropic-sdk-*.jar com.cloudempiere.ai.deps/lib/

# AWS SDK (Bedrock)
cp com.cloudempiere.ai.plugin/lib/aws-sdk-bedrock-*.jar com.cloudempiere.ai.deps/lib/
cp com.cloudempiere.ai.plugin/lib/aws-core-*.jar com.cloudempiere.ai.deps/lib/

# Supporting libraries
cp com.cloudempiere.ai.plugin/lib/jackson-*.jar com.cloudempiere.ai.deps/lib/
cp com.cloudempiere.ai.plugin/lib/kotlin-*.jar com.cloudempiere.ai.deps/lib/
cp com.cloudempiere.ai.plugin/lib/okhttp-*.jar com.cloudempiere.ai.deps/lib/
cp com.cloudempiere.ai.plugin/lib/netty-*.jar com.cloudempiere.ai.deps/lib/
```

#### 3. Create MANIFEST.MF

```manifest
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Cloudempiere AI - Shared Dependencies
Bundle-SymbolicName: com.cloudempiere.ai.deps
Bundle-Version: 0.35.0.qualifier
Bundle-Vendor: Cloudempiere
Bundle-RequiredExecutionEnvironment: JavaSE-11
Bundle-ClassPath: .,
 lib/langchain4j-core-0.35.0.jar,
 lib/langchain4j-anthropic-0.35.0.jar,
 lib/langchain4j-aws-bedrock-0.35.0.jar,
 lib/langchain4j-ollama-0.35.0.jar,
 lib/langchain4j-open-ai-0.35.0.jar,
 lib/anthropic-sdk-2.10.0.jar,
 lib/aws-sdk-bedrock-2.20.162.jar,
 lib/jackson-core-2.17.0.jar,
 lib/jackson-databind-2.17.0.jar,
 lib/jackson-annotations-2.17.0.jar,
 lib/kotlin-stdlib-1.9.10.jar,
 lib/okhttp-4.12.0.jar,
 lib/okio-3.9.0.jar,
 lib/netty-buffer-4.1.100.jar,
 lib/netty-common-4.1.100.jar
Export-Package: dev.langchain4j;version="0.35.0",
 dev.langchain4j.agent;version="0.35.0",
 dev.langchain4j.agent.tool;version="0.35.0",
 dev.langchain4j.chain;version="0.35.0",
 dev.langchain4j.data;version="0.35.0",
 dev.langchain4j.data.document;version="0.35.0",
 dev.langchain4j.data.embedding;version="0.35.0",
 dev.langchain4j.data.message;version="0.35.0",
 dev.langchain4j.data.segment;version="0.35.0",
 dev.langchain4j.memory;version="0.35.0",
 dev.langchain4j.memory.chat;version="0.35.0",
 dev.langchain4j.model;version="0.35.0",
 dev.langchain4j.model.anthropic;version="0.35.0",
 dev.langchain4j.model.bedrock;version="0.35.0",
 dev.langchain4j.model.chat;version="0.35.0",
 dev.langchain4j.model.embedding;version="0.35.0",
 dev.langchain4j.model.input;version="0.35.0",
 dev.langchain4j.model.ollama;version="0.35.0",
 dev.langchain4j.model.openai;version="0.35.0",
 dev.langchain4j.model.output;version="0.35.0",
 dev.langchain4j.model.streaming;version="0.35.0",
 dev.langchain4j.rag;version="0.35.0",
 dev.langchain4j.service;version="0.35.0",
 dev.langchain4j.store.embedding;version="0.35.0",
 com.anthropic.sdk;version="2.10.0",
 com.anthropic.sdk.models;version="2.10.0",
 software.amazon.awssdk.services.bedrock;version="2.20.162",
 software.amazon.awssdk.services.bedrockruntime;version="2.20.162",
 com.fasterxml.jackson.core;version="2.17.0",
 com.fasterxml.jackson.databind;version="2.17.0",
 com.fasterxml.jackson.annotation;version="2.17.0",
 kotlin;version="1.9.10",
 kotlin.jvm;version="1.9.10",
 okhttp3;version="4.12.0",
 okio;version="3.9.0",
 io.netty.buffer;version="4.1.100",
 io.netty.util;version="4.1.100"
```

#### 4. Create build.properties

```properties
source.. = src/
output.. = target/classes/
bin.includes = META-INF/,\
               .,\
               lib/
```

#### 5. Create pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.idempiere</groupId>
        <artifactId>org.idempiere.parent</artifactId>
        <version>10.0.0-SNAPSHOT</version>
        <relativePath>../iDempiereCLDE/org.idempiere.parent/pom.xml</relativePath>
    </parent>

    <artifactId>com.cloudempiere.ai.deps</artifactId>
    <version>0.35.0-SNAPSHOT</version>
    <packaging>eclipse-plugin</packaging>
    <name>Cloudempiere AI - Shared Dependencies</name>

    <build>
        <plugins>
            <!-- Copy dependencies to lib/ directory -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>copy-langchain4j</id>
                        <phase>validate</phase>
                        <goals>
                            <goal>copy</goal>
                        </goals>
                        <configuration>
                            <artifactItems>
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
                                <!-- ... other dependencies ... -->
                            </artifactItems>
                            <outputDirectory>lib</outputDirectory>
                            <overWriteReleases>true</overWriteReleases>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 6. Build and Verify

```bash
cd com.cloudempiere.ai.deps
mvn clean install

# Verify JAR created
ls -lh target/com.cloudempiere.ai.deps-*.jar

# Check manifest
unzip -p target/com.cloudempiere.ai.deps-*.jar META-INF/MANIFEST.MF | head -50
```

**Deliverables:**
- ✅ `com.cloudempiere.ai.deps/` plugin created
- ✅ All JARs embedded and exported
- ✅ Maven build successful
- ✅ MANIFEST.MF exports all required packages

---

### Week 4: Create Core Plugin

**Goal:** Create `com.cloudempiere.ai.core` with provider, database, context, component, util packages

**Tasks:**

#### 1. Create Plugin Structure

```bash
mkdir -p com.cloudempiere.ai.core/src/com/cloudempiere/ai
mkdir -p com.cloudempiere.ai.core/META-INF
mkdir -p com.cloudempiere.ai.core/OSGI-INF
```

#### 2. Move Core Packages from Monolithic Plugin

```bash
# Move provider layer
mv com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/provider \
   com.cloudempiere.ai.core/src/com/cloudempiere/ai/

# Move database layer
mv com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/database \
   com.cloudempiere.ai.core/src/com/cloudempiere/ai/

# Move context layer
mv com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/context \
   com.cloudempiere.ai.core/src/com/cloudempiere/ai/

# Move UI components
mv com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/component \
   com.cloudempiere.ai.core/src/com/cloudempiere/ai/

# Move utilities
mv com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/util \
   com.cloudempiere.ai.core/src/com/cloudempiere/ai/

# Move models
mv com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/model \
   com.cloudempiere.ai.core/src/com/cloudempiere/ai/
```

#### 3. Remove Domain-Specific Code

```bash
# These will move to domain plugins in Weeks 5-6
rm -rf com.cloudempiere.ai.core/src/com/cloudempiere/ai/agent/
rm -f com.cloudempiere.ai.core/src/com/cloudempiere/ai/tools/ERPTools.java
```

#### 4. Create Domain Boundary Enforcement

**File:** `com.cloudempiere.ai.core/src/com/cloudempiere/ai/boundary/DomainBoundary.java`

```java
package com.cloudempiere.ai.boundary;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for domain boundary enforcement (ADR-009).
 *
 * <p>Each domain plugin extends this class to define its allowed tables.
 * Prevents cross-domain data access via tool methods.
 *
 * @author Cloudempiere
 * @version 1.0
 * @since Phase 2 - Multi-Plugin Architecture
 */
public abstract class DomainBoundary {

    private final String domainName;
    private final Set<String> allowedTables;

    protected DomainBoundary(String domainName) {
        this.domainName = domainName;
        this.allowedTables = new HashSet<>();
        registerAllowedTables();
    }

    /**
     * Subclasses override this to register allowed tables.
     */
    protected abstract void registerAllowedTables();

    /**
     * Register a table as allowed for this domain.
     *
     * @param tableName iDempiere table name (e.g., "C_BPartner")
     */
    protected void allow(String tableName) {
        allowedTables.add(tableName);
    }

    /**
     * Validate if table access is allowed for this domain.
     *
     * @param tableName table to access
     * @throws SecurityException if table not allowed
     */
    public void validateReadTable(String tableName) {
        if (!allowedTables.contains(tableName)) {
            throw new SecurityException(
                String.format("[%s Domain] Table %s not allowed. Allowed tables: %s",
                    domainName, tableName, allowedTables)
            );
        }
    }

    /**
     * Check if table is allowed (non-throwing).
     */
    public boolean isTableAllowed(String tableName) {
        return allowedTables.contains(tableName);
    }

    public String getDomainName() {
        return domainName;
    }

    public Set<String> getAllowedTables() {
        return new HashSet<>(allowedTables);  // Defensive copy
    }
}
```

#### 5. Create MANIFEST.MF

```manifest
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Cloudempiere AI - Core Infrastructure
Bundle-SymbolicName: com.cloudempiere.ai.core;singleton:=true
Bundle-Version: 0.32.0.qualifier
Bundle-Vendor: Cloudempiere
Bundle-RequiredExecutionEnvironment: JavaSE-11
Bundle-Activator: com.cloudempiere.ai.Activator
Import-Package: org.osgi.framework;version="1.10.0",
 org.osgi.service.component.annotations;version="1.3.0",
 org.compiere.model,
 org.compiere.util,
 org.zkoss.zk.ui,
 org.zkoss.zul,
 dev.langchain4j;version="0.35.0",
 dev.langchain4j.agent.tool;version="0.35.0",
 dev.langchain4j.data.message;version="0.35.0",
 dev.langchain4j.memory;version="0.35.0",
 dev.langchain4j.model.chat;version="0.35.0",
 dev.langchain4j.model.streaming;version="0.35.0",
 dev.langchain4j.service;version="0.35.0"
Export-Package: com.cloudempiere.ai;version="0.32.0",
 com.cloudempiere.ai.boundary;version="0.32.0",
 com.cloudempiere.ai.component;version="0.32.0",
 com.cloudempiere.ai.context;version="0.32.0",
 com.cloudempiere.ai.database;version="0.32.0",
 com.cloudempiere.ai.database.dto;version="0.32.0",
 com.cloudempiere.ai.model;version="0.32.0",
 com.cloudempiere.ai.provider;version="0.32.0",
 com.cloudempiere.ai.provider.dto;version="0.32.0",
 com.cloudempiere.ai.provider.factory;version="0.32.0",
 com.cloudempiere.ai.provider.impl;version="0.32.0",
 com.cloudempiere.ai.util;version="0.32.0"
Service-Component: OSGI-INF/*.xml
```

#### 6. Create pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.idempiere</groupId>
        <artifactId>org.idempiere.parent</artifactId>
        <version>10.0.0-SNAPSHOT</version>
        <relativePath>../iDempiereCLDE/org.idempiere.parent/pom.xml</relativePath>
    </parent>

    <artifactId>com.cloudempiere.ai.core</artifactId>
    <version>0.32.0-SNAPSHOT</version>
    <packaging>eclipse-plugin</packaging>
    <name>Cloudempiere AI - Core Infrastructure</name>
</project>
```

#### 7. Build and Verify

```bash
cd com.cloudempiere.ai.core
mvn clean install

# Check exports
unzip -p target/com.cloudempiere.ai.core-*.jar META-INF/MANIFEST.MF | grep Export-Package
```

**Deliverables:**
- ✅ `com.cloudempiere.ai.core/` plugin created
- ✅ Core packages moved from monolithic plugin
- ✅ Domain-specific code removed
- ✅ DomainBoundary.java created
- ✅ Exports all core packages for domain plugins

---

### Weeks 5-6: Create Domain Plugins

**Goal:** Create 5 domain plugins with agents, tools, and boundary enforcement

#### Domain Plugin Template Structure

Each domain plugin follows this structure:

```
com.cloudempiere.ai.<domain>/
├── src/com/cloudempiere/ai/<domain>/
│   ├── agent/                    # Domain agent
│   ├── tools/                    # Domain tools (@Tool methods)
│   ├── boundary/                 # Domain boundary (extends DomainBoundary)
│   └── chain/                    # LangChain4j chain definitions
├── META-INF/
│   └── MANIFEST.MF
├── OSGI-INF/                     # Auto-generated from @Component
├── build.properties
└── pom.xml
```

**Domains to Create:**

1. **Sales Domain** (`com.cloudempiere.ai.sales`)
   - **Tables:** C_BPartner, C_Order, C_OrderLine, C_Invoice, C_InvoiceLine, C_Opportunity
   - **Agent:** SalesAgent
   - **Tools:** SalesTools (getBusinessPartner, getOrder, getOpportunity)

2. **Inventory Domain** (`com.cloudempiere.ai.inventory`)
   - **Tables:** M_Product, M_ProductCategory, M_Storage, M_Warehouse, M_Locator
   - **Agent:** InventoryAgent
   - **Tools:** InventoryTools (getProduct, getStock, getWarehouse)

3. **Purchasing Domain** (`com.cloudempiere.ai.purchasing`)
   - **Tables:** C_Invoice (vendor), C_Payment, C_PaymentTerm, C_BPartner (vendor)
   - **Agent:** PurchasingAgent
   - **Tools:** PurchasingTools (getInvoice, getPayment, getVendor)

4. **Support Domain** (`com.cloudempiere.ai.support`)
   - **Tables:** R_Request, R_RequestType, AD_Note, AD_ChangeLog
   - **Agent:** SupportAgent
   - **Tools:** SupportTools (getRequest, classifyTicket, createNote)

5. **Knowledge Base Domain** (`com.cloudempiere.ai.knowledge`)
   - **Tables:** K_Entry, K_Category, K_Index, AD_Message
   - **Agent:** KnowledgeBaseAgent
   - **Tools:** KnowledgeTools (searchKnowledgeBase, getArticle)

**Example: Sales Domain Implementation**

**File:** `com.cloudempiere.ai.sales/src/com/cloudempiere/ai/sales/boundary/SalesDomainBoundary.java`

```java
package com.cloudempiere.ai.sales.boundary;

import com.cloudempiere.ai.boundary.DomainBoundary;

/**
 * Sales domain boundary enforcement (ADR-009).
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class SalesDomainBoundary extends DomainBoundary {

    public SalesDomainBoundary() {
        super("Sales");
    }

    @Override
    protected void registerAllowedTables() {
        // Sales entities
        allow("C_BPartner");
        allow("C_BPartner_Location");
        allow("C_Order");
        allow("C_OrderLine");
        allow("C_Invoice");
        allow("C_InvoiceLine");
        allow("C_Opportunity");

        // Reference tables
        allow("AD_User");
        allow("AD_Org");
        allow("M_Product");  // Read-only reference
        allow("M_PriceList");
    }
}
```

**File:** `com.cloudempiere.ai.sales/src/com/cloudempiere/ai/sales/tools/SalesTools.java`

```java
package com.cloudempiere.ai.sales.tools;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.sales.boundary.SalesDomainBoundary;

import dev.langchain4j.agent.tool.Tool;

/**
 * Sales domain tools for LangChain4j agents.
 *
 * @author Cloudempiere
 * @version 1.0
 */
@Component(
    service = SalesTools.class,
    immediate = true
)
public class SalesTools {

    @Reference
    private volatile SecureDatabaseQueryExecutor dbExecutor;

    private final SalesDomainBoundary boundary = new SalesDomainBoundary();

    @Tool("Get business partner information by ID")
    public String getBusinessPartner(int C_BPartner_ID) {
        boundary.validateReadTable("C_BPartner");

        String sql = "SELECT C_BPartner_ID, Name, Value, TaxID, " +
                     "       SO_CreditLimit, SO_CreditUsed, TotalOpenBalance " +
                     "FROM C_BPartner WHERE C_BPartner_ID = ?";

        return dbExecutor.executeSecureQuery(sql, C_BPartner_ID);
    }

    @Tool("Get sales order by ID")
    public String getOrder(int C_Order_ID) {
        boundary.validateReadTable("C_Order");

        String sql = "SELECT o.C_Order_ID, o.DocumentNo, o.DateOrdered, " +
                     "       o.GrandTotal, o.DocStatus, bp.Name AS CustomerName " +
                     "FROM C_Order o " +
                     "INNER JOIN C_BPartner bp ON o.C_BPartner_ID = bp.C_BPartner_ID " +
                     "WHERE o.C_Order_ID = ?";

        return dbExecutor.executeSecureQuery(sql, C_Order_ID);
    }

    @Tool("Get sales opportunity by ID")
    public String getOpportunity(int C_Opportunity_ID) {
        boundary.validateReadTable("C_Opportunity");

        String sql = "SELECT C_Opportunity_ID, OpportunityAmt, ExpectedCloseDate, " +
                     "       Probability, OpportunityType " +
                     "FROM C_Opportunity WHERE C_Opportunity_ID = ?";

        return dbExecutor.executeSecureQuery(sql, C_Opportunity_ID);
    }
}
```

**File:** `com.cloudempiere.ai.sales/META-INF/MANIFEST.MF`

```manifest
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Cloudempiere AI - Sales Domain
Bundle-SymbolicName: com.cloudempiere.ai.sales
Bundle-Version: 1.0.0.qualifier
Bundle-Vendor: Cloudempiere
Bundle-RequiredExecutionEnvironment: JavaSE-11
Import-Package: org.osgi.service.component.annotations;version="1.3.0",
 org.compiere.model,
 org.compiere.util,
 com.cloudempiere.ai.boundary;version="0.32.0",
 com.cloudempiere.ai.database;version="0.32.0",
 com.cloudempiere.ai.provider;version="0.32.0",
 dev.langchain4j.agent.tool;version="0.35.0",
 dev.langchain4j.data.message;version="0.35.0",
 dev.langchain4j.service;version="0.35.0"
Export-Package: com.cloudempiere.ai.sales;version="1.0.0",
 com.cloudempiere.ai.sales.agent;version="1.0.0",
 com.cloudempiere.ai.sales.tools;version="1.0.0"
Service-Component: OSGI-INF/*.xml
```

**Repeat for other 4 domains:**
- com.cloudempiere.ai.inventory
- com.cloudempiere.ai.purchasing
- com.cloudempiere.ai.support
- com.cloudempiere.ai.knowledge

**Deliverables:**
- ✅ 5 domain plugins created
- ✅ Domain boundaries enforce table access
- ✅ Tools registered via @Component + @Tool
- ✅ Each plugin imports core and deps packages

---

### Weeks 7-8: P2 Repository & Integration Testing

**Goal:** Create P2 composite repository and verify all plugins resolve and work together

#### 1. Create P2 Repository Structure

```bash
mkdir -p cloudempiere-p2/v10/latest
mkdir -p cloudempiere-p2/v10/plugins/ai-deps/0.35.0
mkdir -p cloudempiere-p2/v10/plugins/ai-core/0.32.0
mkdir -p cloudempiere-p2/v10/plugins/ai-sales/1.0.0
mkdir -p cloudempiere-p2/v10/plugins/ai-inventory/1.0.0
mkdir -p cloudempiere-p2/v10/plugins/ai-purchasing/1.0.0
mkdir -p cloudempiere-p2/v10/plugins/ai-support/1.0.0
mkdir -p cloudempiere-p2/v10/plugins/ai-knowledge/1.0.0
```

#### 2. Build Each Plugin and Copy to P2

```bash
for plugin in com.cloudempiere.ai.deps \
              com.cloudempiere.ai.core \
              com.cloudempiere.ai.sales \
              com.cloudempiere.ai.inventory \
              com.cloudempiere.ai.purchasing \
              com.cloudempiere.ai.support \
              com.cloudempiere.ai.knowledge; do
    cd $plugin
    mvn clean install
    cp target/$plugin-*.jar ../cloudempiere-p2/v10/plugins/$plugin/
    cd ..
done
```

#### 3. Create Composite Repository

**File:** `cloudempiere-p2/v10/latest/compositeContent.xml`

```xml
<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='Cloudempiere AI - Latest (v10)'
    type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository'
    version='1.0.0'>
  <properties size='1'>
    <property name='p2.timestamp' value='1738281600000'/>
  </properties>
  <children size='7'>
    <child location='../../plugins/ai-deps/0.35.0'/>
    <child location='../../plugins/ai-core/0.32.0'/>
    <child location='../../plugins/ai-sales/1.0.0'/>
    <child location='../../plugins/ai-inventory/1.0.0'/>
    <child location='../../plugins/ai-purchasing/1.0.0'/>
    <child location='../../plugins/ai-support/1.0.0'/>
    <child location='../../plugins/ai-knowledge/1.0.0'/>
  </children>
</repository>
```

#### 4. Integration Testing

**Test Checklist:**

```bash
# 1. Start iDempiere with new plugins
cd iDempiereCLDE/org.idempiere.p2/target/products/org.adempiere.server.product/linux/gtk/x86_64/
./idempiere-server.sh console

# 2. Verify all bundles are ACTIVE
osgi> ss | grep cloudempiere
  101 ACTIVE      com.cloudempiere.ai.deps_0.35.0
  102 ACTIVE      com.cloudempiere.ai.core_0.32.0
  103 ACTIVE      com.cloudempiere.ai.sales_1.0.0
  104 ACTIVE      com.cloudempiere.ai.inventory_1.0.0
  105 ACTIVE      com.cloudempiere.ai.purchasing_1.0.0
  106 ACTIVE      com.cloudempiere.ai.support_1.0.0
  107 ACTIVE      com.cloudempiere.ai.knowledge_1.0.0

# 3. Check for dependency issues
osgi> diag 101
osgi> diag 102
osgi> diag 103
...

# 4. Verify services registered
osgi> services "(objectClass=com.cloudempiere.ai.provider.factory.IAIProviderFactory)"
osgi> services "(objectClass=com.cloudempiere.ai.sales.tools.SalesTools)"

# 5. Test chat widget loads
# → Log into iDempiere UI
# → Open chat bubble
# → Send message: "Get business partner 100"
# → Verify: SalesTools.getBusinessPartner() called
```

**Test Scenarios:**

1. **Domain Boundary Enforcement:**
   - Sales agent tries to access M_Product → ❌ Allowed (read-only reference)
   - Sales agent tries to access R_Request → ❌ BLOCKED (cross-domain)
   - Support agent tries to access C_Order → ❌ BLOCKED (cross-domain)

2. **Tool Registration:**
   - SalesTools registered → ✅
   - InventoryTools registered → ✅
   - All @Tool methods discoverable → ✅

3. **Provider Layer:**
   - AnthropicProvider still works → ✅
   - Chat streaming functional → ✅
   - HTML storage working → ✅

4. **Performance:**
   - Plugin startup time < 5 seconds → ✅
   - No bundle resolution delays → ✅

**Deliverables:**
- ✅ P2 composite repository created
- ✅ All 7 plugins deployed
- ✅ Integration tests pass
- ✅ Domain boundaries enforce table access
- ✅ Performance validated

---

## Weeks 9-10: Migration & Documentation

**Goal:** Migrate remaining features, update documentation, and prepare for Phase 3

### Tasks:

1. **Migrate Remaining Features:**
   - RAG tools to core plugin
   - Provider configurations to database
   - Update all references to new package structure

2. **Update Documentation:**
   - Update CLAUDE.md with new plugin structure
   - Update ADR-009 and ADR-011 with implementation notes
   - Create migration guide for future developers

3. **Performance Optimization:**
   - Lazy bundle activation where possible
   - Minimize Import-Package lists
   - Verify no duplicate classes across plugins

4. **Testing:**
   - Run full test suite: `./run-unit-tests.sh --integration`
   - Manual testing of all 11 use cases (ADR-017 through ADR-025)
   - Regression testing: Verify Phase 0 and Phase 1 still work

**Deliverables:**
- ✅ All features migrated to multi-plugin architecture
- ✅ Documentation updated
- ✅ Full test suite passes
- ✅ Ready for Phase 3 (External Integrations)

---

## Phase 2 Validation Criteria

Before proceeding to Phase 3, verify:

- [ ] All 7 plugins build successfully via Maven
- [ ] All bundles start to ACTIVE state in OSGi
- [ ] No unresolved dependencies (check with `osgi> diag`)
- [ ] Domain boundaries enforce table access (test cross-domain access)
- [ ] Chat widget loads and functions correctly
- [ ] All existing tests pass
- [ ] No performance regression (startup time, chat latency)
- [ ] P2 repository accessible and serves plugins

---

## Next: Phase 3 - External Integrations (Weeks 11-14)

After Phase 2 completion, proceed to:

- Create `com.cloudempiere.graphql` plugin (GraphQL API)
- Create `com.cloudempiere.rest` plugin (REST API)
- Create `com.cloudempiere.auth` plugin (OAuth2 provider)
- Implement IAIChatFacade for external consumption

**Reference:** See `EXTERNAL_INTEGRATION_ARCHITECTURE.md`

---

## Rollback Plan

If Phase 2 fails or issues arise:

1. Revert to monolithic plugin: `git checkout cld-1704-final`
2. Document lessons learned
3. Adjust architecture design
4. Re-plan with smaller scope

**Critical Success Factor:** All domain plugins must be independently testable before integration.

---

## Resources

- **OSGi Skill:** Use `idempiere-tools:idempiere-osgi-p2` skill for MANIFEST.MF help
- **Architecture Docs:**
  - `OSGI_MULTI_PLUGIN_ARCHITECTURE.md` - Full 7-plugin design
  - `ADR-009` - Domain boundaries
  - `ADR-011` - Specialized agent scopes
- **iDempiere Docs:** https://wiki.idempiere.org/en/OSGi_Plugin_Development

---

## Conclusion

Phase 2 transforms the monolithic plugin into a modular, domain-driven architecture with:

- ✅ Clear separation of concerns
- ✅ Enforced domain boundaries
- ✅ Shared dependency management
- ✅ Independent versioning per domain
- ✅ Foundation for external API integrations (Phase 3)

**Start Date:** After Phase 1 verification complete
**End Date:** 8 weeks from start
**Next Review:** End of Week 6 (domain plugins complete)
