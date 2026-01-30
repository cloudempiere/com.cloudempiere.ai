# Final Implementation Plan - CloudEmpiere AI

**Date:** 2026-01-30
**Status:** Master Implementation Plan
**Based On:** Current code analysis (CLD-1704 branch) + architectural designs

---

## Executive Summary

This plan validates the **current codebase** (CLD-1704 branch) and defines the migration path to the **target multi-plugin architecture**. The plan is broken into **7 phases over 20 weeks** with clear deliverables and validation criteria.

**Current State (as of 2026-01-30):**
- ✅ Streaming rendering complete (Phase 0)
- ✅ Monolithic plugin structure (`com.cloudempiere.ai.plugin`)
- ✅ LangChain4j 0.35.0 integration
- ✅ 4 providers (Anthropic, Bedrock, Ollama, OpenAI)
- ⚠️ No domain separation
- ⚠️ No external API support
- ⚠️ No RAG/vector database

**Target State (Week 20):**
- ✅ Multi-plugin OSGi architecture (core + 5 domains)
- ✅ Shared dependencies plugin
- ✅ External API support (GraphQL, REST)
- ✅ RAG with pgvector
- ✅ Observable and secure

---

## Current Code Analysis and Validation

### Existing Files to KEEP (Core Infrastructure)

| File | Current Location | Target Location | Status | Notes |
|------|------------------|-----------------|--------|-------|
| **Provider Layer** ||||
| IAIProvider.java | provider/ | core/provider/ | ✅ Keep | API interface, no changes |
| IAIProviderFactory.java | provider/factory/ | core/provider/ | ✅ Keep | Service interface |
| AIProviderFactory.java | provider/factory/ | core/provider/factory/ | 🔄 Refactor | Remove domain logic |
| AIRequest.java | provider/dto/ | core/provider/dto/ | ✅ Keep | DTO, no changes |
| AIResponse.java | provider/dto/ | core/provider/dto/ | ✅ Keep | DTO, no changes |
| AIMessage.java | provider/dto/ | core/provider/dto/ | ✅ Keep | DTO, no changes |
| AnthropicProvider.java | provider/impl/ | core/provider/impl/ | ✅ Keep | Implementation OK |
| BedrockStreamingChatModelWrapper.java | provider/langchain4j/ | core/provider/impl/ | ✅ Keep | OSGi workaround |
| **Database Layer** ||||
| SecureDatabaseQueryExecutor.java | database/ | core/database/ | ✅ Keep | Core security |
| SecureQueryRequest.java | database/dto/ | core/database/dto/ | ✅ Keep | DTO |
| SecureQueryResult.java | database/dto/ | core/database/dto/ | ✅ Keep | DTO |
| **Context Layer** ||||
| IAIContextProvider.java | context/ | core/context/ | ✅ Keep | API interface |
| AIContextProviderRegistry.java | context/ | core/context/ | ✅ Keep | Registry service |
| WindowContextProvider.java | context/impl/ | core/context/impl/ | ✅ Keep | Implementation |
| **UI Components** ||||
| AIChatWidget.java | component/ | core/component/ | ✅ Keep | ZK widget |
| AIChatStreamingMessage.java | component/ | core/component/ | ✅ Keep | Streaming component |
| AIChatBubble.java | component/ | core/component/ | ✅ Keep | Floating bubble |
| **Utilities** ||||
| StreamingMarkdownRenderer.java | util/ | core/util/ | ✅ Keep | Rendering engine |
| SecuritySanitizer.java | util/ | core/util/ | ✅ Keep | Security |
| MarkdownValidator.java | util/ | core/util/ | ✅ Keep | Validation |
| ChunkCleaner.java | util/ | core/util/ | ✅ Keep | Sanitization |
| ZoomLinkProcessor.java | util/ | core/util/ | ✅ Keep | Zoom links |
| **Models** ||||
| MAIProvider.java | model/ | core/model/ | ✅ Keep | Provider model |
| MAIChatEntry.java | model/ | core/model/ | ✅ Keep | Chat entry model |
| MAIChat.java | model/ | core/model/ | ✅ Keep | Chat model |

### Existing Files to EXTRACT (Domain-Specific)

| File | Current Location | Target Plugin | Extract To | Action |
|------|------------------|---------------|------------|--------|
| **Agent Classes** |||||
| SalesAgent.java | agent/ | com.cloudempiere.ai.sales | sales/agent/ | 🔄 Extract + refactor |
| InventoryAgent.java | agent/ | com.cloudempiere.ai.inventory | inventory/agent/ | 🔄 Extract + refactor |
| PurchasingAgent.java | agent/ | com.cloudempiere.ai.purchasing | purchasing/agent/ | 🔄 Extract + refactor |
| SupportAgent.java | agent/ | com.cloudempiere.ai.support | support/agent/ | 🔄 Extract + refactor |
| KnowledgeBaseAgent.java | agent/ | com.cloudempiere.ai.knowledge | knowledge/agent/ | 🔄 Extract + refactor |
| **Tool Classes** |||||
| ERPTools.java | tools/ | N/A - SPLIT | (split by domain) | ⚠️ Split into domain tools |
| RagTools.java | tools/ | core/tools/ | Keep in core | ✅ Core tool |
| **Boundary Classes** |||||
| (Not yet created) | N/A | Each domain | boundary/ | ➕ Create new |

**ERPTools.java Split Plan:**

```java
// Current ERPTools.java (monolithic)
public class ERPTools {
    @Tool("Get business partner")
    public String getBusinessPartner(int id) { ... }  // → SalesTools

    @Tool("Get product")
    public String getProduct(int id) { ... }  // → InventoryTools

    @Tool("Get order")
    public String getOrder(int id) { ... }  // → SalesTools

    @Tool("Get invoice")
    public String getInvoice(int id) { ... }  // → PurchasingTools

    @Tool("Query database")
    public String queryDatabase(String sql) { ... }  // → Core (via SecureDatabaseQueryExecutor)
}

// Target: SalesTools.java (sales domain)
@Component
public class SalesTools {
    @Reference
    private volatile SecureDatabaseQueryExecutor dbExecutor;

    @Tool("Get business partner information")
    public String getBusinessPartner(
        @Parameter("C_BPartner_ID") int id
    ) {
        SalesDomainBoundary.validateReadTable("C_BPartner");
        String sql = "SELECT * FROM C_BPartner WHERE C_BPartner_ID = ?";
        return dbExecutor.executeSecureQuery(sql, id);
    }

    @Tool("Get sales order")
    public String getOrder(@Parameter("C_Order_ID") int id) {
        SalesDomainBoundary.validateReadTable("C_Order");
        String sql = "SELECT * FROM C_Order WHERE C_Order_ID = ?";
        return dbExecutor.executeSecureQuery(sql, id);
    }
}

// Target: InventoryTools.java (inventory domain)
@Component
public class InventoryTools {
    @Reference
    private volatile SecureDatabaseQueryExecutor dbExecutor;

    @Tool("Get product information")
    public String getProduct(@Parameter("M_Product_ID") int id) {
        InventoryDomainBoundary.validateReadTable("M_Product");
        String sql = "SELECT * FROM M_Product WHERE M_Product_ID = ?";
        return dbExecutor.executeSecureQuery(sql, id);
    }

    @Tool("Get product stock")
    public String getProductStock(@Parameter("M_Product_ID") int id) {
        InventoryDomainBoundary.validateReadTable("M_Storage");
        String sql = "SELECT * FROM M_Storage WHERE M_Product_ID = ?";
        return dbExecutor.executeSecureQuery(sql, id);
    }
}
```

### Existing Files to DEPRECATE

| File | Reason | Replacement |
|------|--------|-------------|
| Old markdown renderers | Superseded by StreamingMarkdownRenderer | StreamingMarkdownRenderer.java |
| Static HTML renderers | Superseded by ADR-054 HTML storage | Store HTML directly |
| Legacy callout factories (if any XML-based) | Superseded by @Callout annotations | Annotation-based callouts |

### New Files to CREATE

| File | Plugin | Purpose | Phase |
|------|--------|---------|-------|
| **Facade Layer** ||||
| IAIChatFacade.java | core | External API contract | Phase 3 |
| AIChatFacadeImpl.java | core | Facade implementation | Phase 3 |
| ChatRequest.java | core | Facade DTO | Phase 3 |
| ChatResponse.java | core | Facade DTO | Phase 3 |
| MessageDTO.java | core | Facade DTO | Phase 3 |
| **Domain Boundaries** ||||
| SalesDomainBoundary.java | sales | Table/action whitelist | Phase 2 |
| InventoryDomainBoundary.java | inventory | Table/action whitelist | Phase 2 |
| PurchasingDomainBoundary.java | purchasing | Table/action whitelist | Phase 2 |
| SupportDomainBoundary.java | support | Table/action whitelist | Phase 2 |
| KnowledgeDomainBoundary.java | knowledge | Table/action whitelist | Phase 2 |
| **RAG Infrastructure** ||||
| RAGContextManager.java | core | RAG service | Phase 4 |
| EmbeddingService.java | core | Embedding generation | Phase 4 |
| PgVectorStore.java | core | PostgreSQL vector storage | Phase 4 |
| **External API Plugins** ||||
| AIChatQueryResolver.java | graphql | GraphQL resolver | Phase 5 |
| AIChatResource.java | rest | REST endpoint | Phase 6 |
| MCPServer.java | mcp | MCP server | Phase 7 (Java 17) |
| **Sanitization** ||||
| MarkdownSyntaxSanitizer.java | core | Syntax sanitization (ADR-055) | Phase 1 |

---

## Phase Breakdown (20 Weeks)

### Phase 0: Streaming Rendering Completion ✅ DONE (CLD-1704)

**Duration:** 2 weeks (completed 2026-01-28)
**Status:** ✅ Complete

**Deliverables:**
- ✅ StreamingMarkdownRenderer.java with full emphasis support
- ✅ AIChatStreamingMessage.java with chunk queue
- ✅ ChunkCleaner.java for invalid character removal
- ✅ SecuritySanitizer.java for HTML/JS/URL escaping
- ✅ MarkdownValidator.java for structure validation

**Validation:**
- ✅ Real-time streaming works (50-char batches)
- ✅ Bold, italic, inline code rendering correct
- ✅ No flickering or DOM update storms
- ✅ Tests pass: ./run-unit-tests.sh

**Branch:** `cld-1704-final`

---

### Phase 1: Sanitization & HTML Storage (Weeks 1-2)

**Goal:** Complete ADR-054 (HTML storage) and ADR-055 (markdown sanitization)

**Week 1: ADR-055 - Markdown Syntax Sanitization**

**Tasks:**
1. ✅ Create `MarkdownSyntaxSanitizer.java` in `com.cloudempiere.ai.plugin/src/com/cloudempiere/ai/util/`
2. ✅ Implement URL sanitization:
   - Block `javascript:`, `data:`, `file:` URLs
   - Whitelist `http://`, `https://`
   - Escape malicious URLs
3. ✅ Implement raw HTML escaping:
   - `<script>` → `&lt;script&gt;`
   - `<iframe>` → `&lt;iframe&gt;`
   - Event handlers removed
4. ✅ Remove unsupported markdown features:
   - Footnotes `[^1]`
   - Task lists `- [ ]`
   - Definition lists
5. ✅ Integrate into streaming pipeline:
   ```java
   // AIChatStreamingMessage.java:245
   String validated = MarkdownValidator.validate(cleaned);
   String sanitized = MarkdownSyntaxSanitizer.sanitize(validated);  // NEW
   chunkQueue.add(sanitized);
   ```
6. ✅ Write unit tests: `MarkdownSyntaxSanitizerTest.java`
   - Test XSS vectors blocked
   - Test javascript: URLs blocked
   - Test data: URLs blocked
   - Test supported syntax preserved

**Week 2: ADR-054 - HTML-Only Storage**

**Tasks:**
1. ✅ Add `renderedHtml` field to `AIChatStreamingMessage.java`
2. ✅ Add `captureRenderedHtml()` method (called in `complete()`)
3. ✅ Add `getRenderedHtml()` public method
4. ✅ Update `AIChatWidget.java:1219` (onComplete handler):
   ```java
   // OLD: String response = streamingMsg.getContent();  // markdown
   // NEW:
   String response = streamingMsg.getRenderedHtml();  // HTML
   MAIChatEntry aiEntry = MAIChatEntry.createAIResponse(aiChat, response);
   ```
5. ✅ Update `AIChatWidget.java:554-571` (renderMessageBubble):
   ```java
   if (isAI) {
       boolean isHtml = messageText.contains("<p>") || messageText.contains("<div");
       if (isHtml) {
           // NEW messages: Use stored HTML directly
           sb.append(messageText);
       } else {
           // OLD messages: Fallback to rendering markdown
           String renderedHtml = AIMessageRenderer.render(...);
           sb.append(renderedHtml);
       }
   }
   ```
6. ✅ Test migration:
   - New messages store HTML
   - Old markdown messages still render
   - Reload performance: 100ms → <10ms for 50 messages

**Deliverables:**
- `MarkdownSyntaxSanitizer.java` + tests
- HTML storage in `CM_ChatEntry.CharacterData`
- Backward compatibility with old markdown messages

**Validation:**
- [ ] All XSS tests pass (ADR-055 test suite)
- [ ] New messages store HTML correctly
- [ ] Old messages render via fallback
- [ ] Performance: <10ms reload for 50 messages

**Branch:** Create `phase-1-sanitization-html-storage` from `cld-1704-final`

**Related Docs:**
- [ADR-054: HTML-Only Chat Message Storage](docs/adr/054-html-only-chat-message-storage.md)
- [ADR-055: Constrained Markdown Syntax Support](docs/adr/055-constrained-markdown-syntax-support.md)

---

### Phase 2: Multi-Plugin Architecture (Weeks 3-6)

**Goal:** Extract shared deps, create core plugin, create 5 domain plugins

**Week 3: Extract Shared Dependencies Plugin**

**Tasks:**
1. ✅ Create `com.cloudempiere.ai.deps/` plugin structure
2. ✅ Copy JARs from `com.cloudempiere.ai.plugin/lib/` to `deps/lib/`:
   ```bash
   mkdir -p com.cloudempiere.ai.deps/lib
   cp com.cloudempiere.ai.plugin/lib/langchain4j-*.jar com.cloudempiere.ai.deps/lib/
   cp com.cloudempiere.ai.plugin/lib/anthropic-*.jar com.cloudempiere.ai.deps/lib/
   cp com.cloudempiere.ai.plugin/lib/aws-sdk-*.jar com.cloudempiere.ai.deps/lib/
   ```
3. ✅ Create `MANIFEST.MF`:
   ```manifest
   Bundle-SymbolicName: com.cloudempiere.ai.deps
   Bundle-Version: 0.35.0.qualifier
   Bundle-ClassPath: .,lib/langchain4j-core-0.35.0.jar,...
   Export-Package: dev.langchain4j.*;version="0.35.0",
    com.anthropic.sdk.*;version="2.10.0",
    software.amazon.awssdk.services.bedrock.*;version="2.20.162"
   ```
4. ✅ Create `pom.xml`:
   ```xml
   <artifactId>com.cloudempiere.ai.deps</artifactId>
   <version>0.35.0-SNAPSHOT</version>
   <packaging>eclipse-plugin</packaging>
   ```
5. ✅ Build and verify:
   ```bash
   mvn clean install
   osgi> ss | grep deps
   ```

**Week 4: Create Core Plugin**

**Tasks:**
1. ✅ Create `com.cloudempiere.ai.core/` plugin structure
2. ✅ Move core packages from monolithic plugin:
   ```bash
   mv plugin/src/com/cloudempiere/ai/provider core/src/com/cloudempiere/ai/
   mv plugin/src/com/cloudempiere/ai/database core/src/com/cloudempiere/ai/
   mv plugin/src/com/cloudempiere/ai/context core/src/com/cloudempiere/ai/
   mv plugin/src/com/cloudempiere/ai/component core/src/com/cloudempiere/ai/
   mv plugin/src/com/cloudempiere/ai/util core/src/com/cloudempiere/ai/
   mv plugin/src/com/cloudempiere/ai/model core/src/com/cloudempiere/ai/
   ```
3. ✅ Remove domain-specific code:
   ```bash
   rm -rf core/src/com/cloudempiere/ai/agent/  # Will go to domain plugins
   ```
4. ✅ Update `MANIFEST.MF`:
   ```manifest
   Bundle-SymbolicName: com.cloudempiere.ai.core;singleton:=true
   Bundle-Version: 1.0.0.qualifier
   Export-Package: com.cloudempiere.ai.provider;version="1.0.0",
    com.cloudempiere.ai.provider.dto;version="1.0.0",
    com.cloudempiere.ai.database;version="1.0.0",
    com.cloudempiere.ai.context;version="1.0.0",
    com.cloudempiere.ai.util;version="1.0.0",
    com.cloudempiere.ai.model;version="1.0.0"
   Import-Package: dev.langchain4j;version="[0.35.0,1.0.0)",
    com.anthropic.sdk;version="[2.10.0,3.0.0)",
    org.compiere.*,
    org.adempiere.*
   ```
5. ✅ Remove `lib/` directory (now Import-Package from deps)
6. ✅ Build and verify

**Week 5-6: Create Domain Plugins**

**For each domain (Sales, Inventory, Purchasing, Support, Knowledge):**

**Sales Domain Example:**

1. ✅ Create plugin structure:
   ```bash
   mkdir -p com.cloudempiere.ai.sales/src/com/cloudempiere/ai/sales/{agent,tools,boundary,dto}
   ```

2. ✅ Extract domain code:
   ```bash
   # From monolithic plugin (if exists)
   grep -r "class.*SalesAgent" plugin/src/ | awk '{print $1}' | xargs -I {} cp {} sales/src/...
   ```

3. ✅ Create `SalesDomainBoundary.java`:
   ```java
   package com.cloudempiere.ai.sales.boundary;

   import java.util.Set;

   /**
    * Sales domain boundary - restricts AI access to sales tables only.
    *
    * <p><b>Related ADRs:</b>
    * <ul>
    *   <li><a href="../../../docs/adr/009-domain-boundaries-agent-scope.md">ADR-009</a></li>
    *   <li><a href="../../../docs/adr/011-specialized-agent-scopes.md">ADR-011</a></li>
    * </ul>
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

       public static void validateReadTable(String tableName) {
           if (!READ_TABLES.contains(tableName)) {
               throw new SecurityException("Table " + tableName + " not accessible in Sales domain");
           }
       }

       public static void validateWriteTable(String tableName) {
           if (!WRITE_TABLES.contains(tableName)) {
               throw new SecurityException("Table " + tableName + " not writable in Sales domain");
           }
       }
   }
   ```

4. ✅ Split `ERPTools.java` → `SalesTools.java`:
   ```java
   package com.cloudempiere.ai.sales.tools;

   import org.osgi.service.component.annotations.*;
   import dev.langchain4j.agent.tool.Tool;
   import dev.langchain4j.agent.tool.Parameter;
   import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
   import com.cloudempiere.ai.sales.boundary.SalesDomainBoundary;

   /**
    * Sales domain tools for AI agents.
    *
    * <p><b>Architecture Layer:</b> Domain Layer (Sales)
    * <p><b>Security:</b> All queries validated against {@link SalesDomainBoundary}
    */
   @Component(service = SalesTools.class)
   public class SalesTools {

       @Reference(cardinality = ReferenceCardinality.MANDATORY)
       private volatile SecureDatabaseQueryExecutor dbExecutor;

       @Tool("Get business partner information")
       public String getBusinessPartner(
           @Parameter("C_BPartner_ID") int id
       ) {
           SalesDomainBoundary.validateReadTable("C_BPartner");
           String sql = "SELECT * FROM C_BPartner WHERE C_BPartner_ID = ?";
           return dbExecutor.executeSecureQuery(sql, id).toJSON();
       }

       @Tool("Get sales order details")
       public String getOrder(
           @Parameter("C_Order_ID") int id
       ) {
           SalesDomainBoundary.validateReadTable("C_Order");
           String sql = "SELECT * FROM C_Order WHERE C_Order_ID = ?";
           return dbExecutor.executeSecureQuery(sql, id).toJSON();
       }

       @Tool("Search sales opportunities")
       public String searchOpportunities(
           @Parameter("Business Partner ID (optional)") Integer bPartnerId,
           @Parameter("Status (optional)") String status
       ) {
           SalesDomainBoundary.validateReadTable("C_Opportunity");
           StringBuilder sql = new StringBuilder("SELECT * FROM C_Opportunity WHERE 1=1");
           if (bPartnerId != null) {
               sql.append(" AND C_BPartner_ID = ").append(bPartnerId);
           }
           if (status != null) {
               sql.append(" AND OpportunityStatus = '").append(status).append("'");
           }
           return dbExecutor.executeSecureQuery(sql.toString()).toJSON();
       }
   }
   ```

5. ✅ Create `SalesAgent.java`:
   ```java
   package com.cloudempiere.ai.sales.agent;

   import org.osgi.service.component.annotations.*;
   import dev.langchain4j.service.AiServices;
   import com.cloudempiere.ai.provider.IAIProviderFactory;
   import com.cloudempiere.ai.provider.IAIProvider;
   import com.cloudempiere.ai.sales.tools.SalesTools;

   /**
    * Sales domain AI agent.
    *
    * <p><b>Architecture Layer:</b> Domain Agent Layer (Sales)
    * <p><b>Boundary:</b> {@link com.cloudempiere.ai.sales.boundary.SalesDomainBoundary}
    */
   @Component(service = SalesAgent.class, immediate = true)
   public class SalesAgent {

       @Reference
       private volatile IAIProviderFactory providerFactory;

       @Reference
       private volatile SalesTools salesTools;

       private SalesAgentInterface agent;

       @Activate
       protected void activate() {
           IAIProvider provider = providerFactory.getProvider(/* config */);

           agent = AiServices.builder(SalesAgentInterface.class)
               .chatLanguageModel(provider.getChatModel())
               .tools(salesTools)
               .chatMemory(/* ... */)
               .build();
       }

       public String analyzeSalesOpportunity(int opportunityId) {
           return agent.analyze(opportunityId);
       }
   }

   interface SalesAgentInterface {
       @SystemMessage("You are a sales analyst AI specialized in iDempiere ERP sales data...")
       String analyze(int opportunityId);
   }
   ```

6. ✅ Create `MANIFEST.MF`:
   ```manifest
   Bundle-SymbolicName: com.cloudempiere.ai.sales
   Bundle-Version: 1.0.0.qualifier
   Service-Component: OSGI-INF/*.xml
   Import-Package: com.cloudempiere.ai.provider;version="[1.0.0,2.0.0)",
    com.cloudempiere.ai.provider.dto;version="[1.0.0,2.0.0)",
    com.cloudempiere.ai.database;version="[1.0.0,2.0.0)",
    dev.langchain4j.service;version="[0.35.0,1.0.0)",
    org.compiere.*,
    org.adempiere.*
   ```

7. ✅ Build and verify:
   ```bash
   mvn clean install
   osgi> ss | grep sales
   ```

**Repeat for:**
- com.cloudempiere.ai.inventory
- com.cloudempiere.ai.purchasing
- com.cloudempiere.ai.support
- com.cloudempiere.ai.knowledge

**Deliverables:**
- com.cloudempiere.ai.deps (shared dependencies)
- com.cloudempiere.ai.core (infrastructure)
- com.cloudempiere.ai.sales (sales domain)
- com.cloudempiere.ai.inventory (inventory domain)
- com.cloudempiere.ai.purchasing (purchasing domain)
- com.cloudempiere.ai.support (support domain)
- com.cloudempiere.ai.knowledge (knowledge base domain)

**Validation:**
- [ ] All plugins build successfully
- [ ] OSGi services resolve correctly
- [ ] Domain boundaries prevent cross-domain access
- [ ] Unit tests pass for each domain

**Branch:** Create `phase-2-multi-plugin-architecture` from `phase-1-sanitization-html-storage`

**Related Docs:**
- [OSGI_MULTI_PLUGIN_ARCHITECTURE.md](OSGI_MULTI_PLUGIN_ARCHITECTURE.md)
- [ADR-009: Domain Boundaries](docs/adr/009-domain-boundaries-agent-scope.md)
- [ADR-011: Specialized Agent Scopes](docs/adr/011-specialized-agent-scopes.md)

---

### Phase 3: Facade Layer for External APIs (Weeks 7-8)

**Goal:** Create protocol-agnostic facade for GraphQL/REST/MCP consumption

**Week 7: Design and Implement Facade**

**Tasks:**
1. ✅ Create facade package in `com.cloudempiere.ai.core/src/com/cloudempiere/ai/facade/`
2. ✅ Create `IAIChatFacade.java` interface (see EXTERNAL_INTEGRATION_ARCHITECTURE.md)
3. ✅ Create DTOs:
   - `ChatRequest.java`
   - `ChatResponse.java`
   - `MessageDTO.java`
   - `UserContext.java`
   - `StreamCallback.java`
4. ✅ Implement `AIChatFacadeImpl.java`:
   ```java
   @Component(service = IAIChatFacade.class)
   public class AIChatFacadeImpl implements IAIChatFacade {

       @Reference
       private volatile AIService aiService;  // Internal service

       @Override
       public ChatResponse chatBlocking(ChatRequest request, UserContext context) {
           // Convert facade DTOs → internal DTOs
           // Call aiService.chat()
           // Convert internal response → facade DTO
           return ChatResponse.builder()
               .responseId(...)
               .message(...)
               .tokens(...)
               .build();
       }

       @Override
       public String chatStreaming(ChatRequest request, UserContext context, StreamCallback callback) {
           // Streaming implementation
       }

       @Override
       public List<MessageDTO> getHistory(String chatId, UserContext context, int maxMessages) {
           // Query CM_ChatEntry
           // Apply AD_Client_ID, AD_User_ID filters
           // Convert to DTOs
       }

       @Override
       public String createThread(UserContext context, int providerId) {
           // Create CM_Chat record
       }
   }
   ```
5. ✅ Update `MANIFEST.MF` (core plugin):
   ```manifest
   Export-Package: com.cloudempiere.ai.facade;version="1.0.0",
    com.cloudempiere.ai.facade.dto;version="1.0.0"
   ```

**Week 8: Testing**

**Tasks:**
1. ✅ Write unit tests: `AIChatFacadeTest.java`
2. ✅ Write integration tests: `AIChatFacadeIntegrationTest.java`
3. ✅ Test DTO serialization (JSON)
4. ✅ Test streaming callback
5. ✅ Test multi-tenancy (AD_Client_ID filtering)

**Deliverables:**
- IAIChatFacade interface + implementation
- Facade DTOs
- Unit + integration tests

**Validation:**
- [ ] Facade tests pass
- [ ] DTOs serialize correctly to JSON
- [ ] Multi-tenancy enforced
- [ ] Streaming callback works

**Branch:** Create `phase-3-facade-layer` from `phase-2-multi-plugin-architecture`

**Related Docs:**
- [EXTERNAL_INTEGRATION_ARCHITECTURE.md](EXTERNAL_INTEGRATION_ARCHITECTURE.md)
- [LAYERED_ARCHITECTURE_DESIGN.md](LAYERED_ARCHITECTURE_DESIGN.md)

---

### Phase 4: RAG Infrastructure (Weeks 9-12)

**Goal:** Implement vector database + RAG retrieval (ADR-012, ADR-026)

**Week 9: PostgreSQL + pgvector Setup**

**Tasks:**
1. ✅ Create database migration script:
   ```sql
   -- Migration script: postgresql/migration/001_pgvector_setup.sql
   CREATE EXTENSION IF NOT EXISTS vector;

   CREATE TABLE AIG_Embedding (
       AIG_Embedding_ID SERIAL PRIMARY KEY,
       AD_Client_ID INT NOT NULL,
       EmbeddingType VARCHAR(50) NOT NULL,  -- 'KNOWLEDGE', 'ENTITY', 'CONVERSATION'
       ContentHash VARCHAR(64),
       Embedding vector(1536),  -- OpenAI/Anthropic embedding dimension
       Metadata JSONB,
       Content TEXT,
       Created TIMESTAMP DEFAULT NOW(),
       CONSTRAINT AIG_Embedding_Client FOREIGN KEY (AD_Client_ID) REFERENCES AD_Client
   );

   CREATE INDEX idx_embedding_hnsw
   ON AIG_Embedding USING hnsw (Embedding vector_cosine_ops);

   CREATE INDEX idx_embedding_client
   ON AIG_Embedding (AD_Client_ID);

   CREATE INDEX idx_embedding_type
   ON AIG_Embedding (EmbeddingType);
   ```
2. ✅ Run migration on dev database
3. ✅ Verify index created:
   ```sql
   SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'aig_embedding';
   ```

**Week 10: Embedding Service**

**Tasks:**
1. ✅ Create `EmbeddingService.java` in `core/src/com/cloudempiere/ai/service/`:
   ```java
   @Component(service = EmbeddingService.class)
   public class EmbeddingService {

       @Reference
       private volatile IAIProviderFactory providerFactory;

       /**
        * Generate embedding vector for text.
        *
        * @param text input text (max 8192 tokens for Anthropic)
        * @return embedding vector (1536 dimensions)
        */
       public float[] embed(String text) {
           IAIProvider provider = providerFactory.getProvider(/* default */);
           EmbeddingModel embeddingModel = provider.getEmbeddingModel();
           Embedding embedding = embeddingModel.embed(text).content();
           return embedding.vectorAsList().stream()
               .mapToDouble(Double::doubleValue)
               .toArray();
       }

       /**
        * Generate embeddings for batch of texts.
        */
       public List<float[]> embedBatch(List<String> texts) {
           // Batch embedding (more efficient)
       }
   }
   ```
2. ✅ Add LangChain4j embedding dependency to `pom.xml`:
   ```xml
   <artifactItem>
       <groupId>dev.langchain4j</groupId>
       <artifactId>langchain4j-embeddings</artifactId>
       <version>0.35.0</version>
   </artifactItem>
   ```

**Week 11: PgVector Store**

**Tasks:**
1. ✅ Create `PgVectorStore.java` in `core/src/com/cloudempiere/ai/store/`:
   ```java
   @Component(service = EmbeddingStore.class)
   public class PgVectorStore implements EmbeddingStore<TextSegment> {

       /**
        * Store embedding with metadata.
        */
       @Override
       public String add(Embedding embedding, TextSegment segment) {
           String sql = "INSERT INTO AIG_Embedding (AD_Client_ID, EmbeddingType, Content, Embedding, Metadata) " +
                        "VALUES (?, ?, ?, ?::vector, ?::jsonb) RETURNING AIG_Embedding_ID";

           float[] vector = embedding.vectorAsList().stream()
               .mapToDouble(Double::doubleValue)
               .toArray();

           String vectorStr = Arrays.toString(vector);  // [0.1, 0.2, ...]

           int id = DB.getSQLValueEx(null, sql,
               segment.metadata().getInt("AD_Client_ID"),
               segment.metadata().getString("EmbeddingType"),
               segment.text(),
               vectorStr,
               segment.metadata().toJson()
           );

           return String.valueOf(id);
       }

       /**
        * Semantic search using cosine similarity.
        */
       @Override
       public List<EmbeddingMatch<TextSegment>> findRelevant(
           Embedding embedding,
           int maxResults,
           double minScore
       ) {
           float[] queryVector = embedding.vectorAsList().stream()
               .mapToDouble(Double::doubleValue)
               .toArray();

           String sql = "SELECT AIG_Embedding_ID, Content, Metadata, " +
                        "       1 - (Embedding <=> ?::vector) AS similarity " +
                        "FROM AIG_Embedding " +
                        "WHERE AD_Client_ID = ? " +
                        "ORDER BY Embedding <=> ?::vector " +
                        "LIMIT ?";

           // Execute and map results
       }
   }
   ```
2. ✅ Add pgvector JDBC driver dependency

**Week 12: RAG Context Manager**

**Tasks:**
1. ✅ Create `RAGContextManager.java` in `core/src/com/cloudempiere/ai/service/`:
   ```java
   @Component(service = RAGContextManager.class)
   public class RAGContextManager {

       @Reference
       private volatile EmbeddingStore<TextSegment> embeddingStore;

       @Reference
       private volatile EmbeddingService embeddingService;

       /**
        * Get content retriever for agent augmentation.
        */
       public ContentRetriever getRetriever() {
           return EmbeddingStoreContentRetriever.builder()
               .embeddingStore(embeddingStore)
               .embeddingModel(new EmbeddingModelAdapter(embeddingService))
               .maxResults(5)
               .minScore(0.7)
               .build();
       }

       /**
        * Ingest knowledge base article.
        */
       public void ingestArticle(int kEntryId) {
           // Query K_Entry table
           // Split into chunks
           // Generate embeddings
           // Store in pgvector
       }
   }
   ```
2. ✅ Integrate retriever into agents:
   ```java
   // SalesAgent.java
   agent = AiServices.builder(SalesAgentInterface.class)
       .chatLanguageModel(provider.getChatModel())
       .tools(salesTools)
       .contentRetriever(ragManager.getRetriever())  // NEW
       .build();
   ```

**Deliverables:**
- PostgreSQL pgvector setup
- EmbeddingService
- PgVectorStore
- RAGContextManager
- Agent integration

**Validation:**
- [ ] Embedding generation works
- [ ] Semantic search returns relevant results
- [ ] Agents use RAG context automatically
- [ ] Performance: <100ms for embedding + search

**Branch:** Create `phase-4-rag-infrastructure` from `phase-3-facade-layer`

**Related Docs:**
- [ADR-012: RAG-Based Context Retrieval](docs/adr/012-rag-based-context-retrieval.md)
- [ADR-026: Vector Database Strategy](docs/adr/026-vector-database-strategy.md)

---

### Phase 5: GraphQL API Plugin (Weeks 13-14)

**Goal:** Create com.cloudempiere.graphql plugin for external GraphQL API

**Week 13: GraphQL Schema + Resolvers**

**Tasks:**
1. ✅ Create `com.cloudempiere.graphql/` plugin structure
2. ✅ Create GraphQL schema (`src/main/resources/schema.graphqls`):
   ```graphql
   type Query {
       chat(chatId: ID!, message: String!): ChatResponse!
       chatHistory(chatId: ID!, limit: Int = 20): [ChatMessage!]!
   }

   type Mutation {
       createChat(providerId: Int): String!
   }

   type Subscription {
       chatStreaming(chatId: ID!, message: String!): ChatStreamChunk!
   }

   type ChatResponse {
       responseId: String!
       message: String!
       tokens: TokenUsage!
       cost: Float!
       provider: String!
       timestamp: String!
   }

   type ChatMessage {
       messageId: String!
       role: String!
       content: String!
       timestamp: String!
   }

   type ChatStreamChunk {
       chunk: String
       complete: Boolean!
       response: ChatResponse
   }

   type TokenUsage {
       input: Int!
       output: Int!
       total: Int!
   }
   ```
3. ✅ Implement `AIChatQueryResolver.java`:
   ```java
   @Component
   public class AIChatQueryResolver implements GraphQLQueryResolver {

       @Reference
       private volatile IAIChatFacade facade;

       @Reference
       private volatile IAuthenticationService auth;

       public ChatResponse chat(
           String chatId,
           String message,
           DataFetchingEnvironment env
       ) {
           String token = env.getContext().get("Authorization");
           UserContext ctx = auth.validateToken(token);
           ChatRequest request = ChatRequest.builder()
               .chatId(chatId)
               .message(message)
               .build();
           return facade.chatBlocking(request, ctx);
       }
   }
   ```
4. ✅ Implement `AIChatSubscriptionResolver.java`:
   ```java
   @Component
   public class AIChatSubscriptionResolver implements GraphQLSubscriptionResolver {

       @Reference
       private volatile IAIChatFacade facade;

       @Reference
       private volatile IAuthenticationService auth;

       public Publisher<ChatStreamChunk> chatStreaming(
           String chatId,
           String message,
           DataFetchingEnvironment env
       ) {
           String token = env.getContext().get("Authorization");
           UserContext ctx = auth.validateToken(token);

           return Flux.create(sink -> {
               facade.chatStreaming(
                   ChatRequest.builder().chatId(chatId).message(message).build(),
                   ctx,
                   new StreamCallback() {
                       @Override
                       public void onChunk(String chunk) {
                           sink.next(ChatStreamChunk.builder()
                               .chunk(chunk)
                               .complete(false)
                               .build());
                       }

                       @Override
                       public void onComplete(ChatResponse response) {
                           sink.next(ChatStreamChunk.builder()
                               .complete(true)
                               .response(response)
                               .build());
                           sink.complete();
                       }

                       @Override
                       public void onError(AIException error) {
                           sink.error(error);
                       }
                   }
               );
           });
       }
   }
   ```

**Week 14: Testing + Deployment**

**Tasks:**
1. ✅ GraphQL Playground setup
2. ✅ Write GraphQL integration tests
3. ✅ Test WebSocket subscriptions
4. ✅ Deploy to P2 repository

**Deliverables:**
- com.cloudempiere.graphql plugin
- GraphQL schema
- Query/Mutation/Subscription resolvers
- GraphQL Playground

**Validation:**
- [ ] GraphQL queries work
- [ ] GraphQL subscriptions stream chunks
- [ ] OAuth2 authentication enforced
- [ ] Multi-tenancy enforced

**Branch:** Create `phase-5-graphql-api` from `phase-4-rag-infrastructure`

**Related Docs:**
- [EXTERNAL_INTEGRATION_ARCHITECTURE.md](EXTERNAL_INTEGRATION_ARCHITECTURE.md)

---

### Phase 6: REST API Plugin (Weeks 15-16)

**Goal:** Create com.cloudempiere.rest plugin for external REST API

**Week 15: JAX-RS Endpoints**

**Tasks:**
1. ✅ Create `com.cloudempiere.rest/` plugin structure
2. ✅ Implement `AIChatResource.java`:
   ```java
   @Path("/api/v1/chat")
   @Component(service = AIChatResource.class)
   public class AIChatResource {

       @Reference
       private volatile IAIChatFacade facade;

       @Reference
       private volatile IAuthenticationService auth;

       @POST
       @Path("/{chatId}/message")
       @Produces(MediaType.APPLICATION_JSON)
       public ChatResponse sendMessage(
           @HeaderParam("Authorization") String token,
           @PathParam("chatId") String chatId,
           ChatRequest request
       ) {
           UserContext ctx = auth.validateToken(token);
           return facade.chatBlocking(request, ctx);
       }

       @POST
       @Path("/{chatId}/stream")
       @Produces(MediaType.SERVER_SENT_EVENTS)
       public void streamMessage(
           @HeaderParam("Authorization") String token,
           @PathParam("chatId") String chatId,
           @Context SseEventSink eventSink,
           ChatRequest request
       ) {
           UserContext ctx = auth.validateToken(token);
           facade.chatStreaming(request, ctx,
               chunk -> eventSink.send(SseEvent.event("chunk", chunk))
           );
       }

       @GET
       @Path("/{chatId}/history")
       @Produces(MediaType.APPLICATION_JSON)
       public List<MessageDTO> getHistory(
           @HeaderParam("Authorization") String token,
           @PathParam("chatId") String chatId,
           @QueryParam("limit") @DefaultValue("20") int limit
       ) {
           UserContext ctx = auth.validateToken(token);
           return facade.getHistory(chatId, ctx, limit);
       }

       @POST
       @Path("/create")
       @Produces(MediaType.APPLICATION_JSON)
       public String createChat(
           @HeaderParam("Authorization") String token,
           @QueryParam("providerId") Integer providerId
       ) {
           UserContext ctx = auth.validateToken(token);
           return facade.createThread(ctx, providerId);
       }
   }
   ```

**Week 16: OpenAPI Documentation + Testing**

**Tasks:**
1. ✅ Generate OpenAPI/Swagger spec
2. ✅ Write REST integration tests
3. ✅ Test SSE streaming
4. ✅ Deploy to P2 repository

**Deliverables:**
- com.cloudempiere.rest plugin
- JAX-RS endpoints
- OpenAPI documentation
- REST integration tests

**Validation:**
- [ ] REST endpoints work
- [ ] SSE streaming works
- [ ] Swagger UI accessible
- [ ] OAuth2 authentication enforced

**Branch:** Create `phase-6-rest-api` from `phase-5-graphql-api`

**Related Docs:**
- [EXTERNAL_INTEGRATION_ARCHITECTURE.md](EXTERNAL_INTEGRATION_ARCHITECTURE.md)

---

### Phase 7: MCP Server Plugin (Weeks 17-18) - BLOCKED BY JAVA 17

**Goal:** Create com.cloudempiere.mcp plugin for Model Context Protocol

**Status:** ❌ Blocked - Requires Java 17 + LangChain4j 1.x

**Prerequisites:**
1. iDempiere v11 migration (Java 17)
2. LangChain4j upgrade to 1.x
3. See [ADR-035: Java Version Strategy](docs/adr/035-java-version-strategy.md)

**Tasks (When Unblocked):**
1. ✅ Create `com.cloudempiere.mcp/` plugin structure
2. ✅ Implement MCP protocol (JSON-RPC)
3. ✅ Implement tool/resource/prompt registration
4. ✅ Test with Claude Desktop, Zed

**Deliverables:**
- com.cloudempiere.mcp plugin
- MCP server implementation
- Integration with Claude Desktop

**Timeline:** Q2 2026 (after Java 17 migration)

**Related Docs:**
- [ADR-003: MCP Server Integration](docs/adr/003-mcp-server-integration.md)
- [ADR-049: MCP Client External Tools Integration](docs/adr/049-mcp-client-external-tools-integration.md)
- [ADR-035: Java Version Strategy](docs/adr/035-java-version-strategy.md)

---

### Phase 8: P2 Repository + Documentation (Weeks 19-20)

**Goal:** Set up P2 composite repository and complete documentation

**Week 19: P2 Repository Setup**

**Tasks:**
1. ✅ Create P2 repository structure (see OSGI_MULTI_PLUGIN_ARCHITECTURE.md)
2. ✅ Build individual plugin repositories:
   ```bash
   cd com.cloudempiere.ai.deps && mvn clean install -Pp2-repository
   cd ../com.cloudempiere.ai.core && mvn clean install -Pp2-repository
   cd ../com.cloudempiere.ai.sales && mvn clean install -Pp2-repository
   # ... repeat for all plugins
   ```
3. ✅ Create composite repository:
   ```bash
   mkdir -p p2-repo/v10/latest
   # Copy compositeContent.xml and compositeArtifacts.xml
   # Update child locations
   ```
4. ✅ Deploy to web server (http://p2.cloudempiere.com/ai/v10/latest)

**Week 20: Final Documentation**

**Tasks:**
1. ✅ Update ADRs with implementation status:
   - Mark ADR-054, ADR-055 as "Implemented"
   - Mark ADR-009, ADR-011 as "Implemented"
   - Mark ADR-012, ADR-026 as "Implemented"
   - Mark ADR-003, ADR-049 as "Blocked (Java 17)"
2. ✅ Update CHANGELOG.md with all changes
3. ✅ Update FEATURES.md with completed features
4. ✅ Create deployment guide
5. ✅ Create user documentation

**Deliverables:**
- P2 composite repository
- Deployment guide
- User documentation
- Updated ADRs

**Validation:**
- [ ] P2 repository accessible
- [ ] All plugins installable via P2
- [ ] Documentation complete
- [ ] All tests pass

**Branch:** Merge all phase branches to `master`

---

## Success Criteria (End of Phase 8)

### Technical Success

- [ ] All 7 plugins build successfully
- [ ] All OSGi services resolve correctly
- [ ] All unit tests pass (>90% coverage)
- [ ] All integration tests pass
- [ ] P2 repository deployed and accessible
- [ ] Performance targets met:
  - Chat response: <3s (blocking)
  - Streaming latency: <50ms per chunk
  - RAG retrieval: <100ms
  - Chat reload: <10ms for 50 messages

### Functional Success

- [ ] Streaming chat works in ZK UI
- [ ] GraphQL API works with OAuth2
- [ ] REST API works with OAuth2
- [ ] RAG provides relevant context
- [ ] Domain boundaries enforced
- [ ] Multi-tenancy enforced
- [ ] HTML storage working
- [ ] Markdown sanitization working

### Operational Success

- [ ] Observability metrics collected
- [ ] Cost tracking functional
- [ ] Security audit passed (OWASP LLM Top 10)
- [ ] Deployment guide tested
- [ ] User documentation complete
- [ ] Developer documentation complete

---

## Rollback Plan

### If Phase Fails

Each phase has a dedicated branch. Rollback procedure:

1. **Identify failing phase**
2. **Revert to previous phase branch:**
   ```bash
   git checkout phase-N-previous
   git branch -D phase-N-current
   ```
3. **Re-attempt phase with fixes**

### If Production Deployment Fails

1. **Uninstall new plugins:**
   ```bash
   osgi> uninstall com.cloudempiere.ai.sales
   osgi> uninstall com.cloudempiere.ai.core
   ```
2. **Reinstall previous version:**
   ```bash
   p2 install com.cloudempiere.ai.plugin/1.0.0  # Monolithic plugin
   ```
3. **Restart iDempiere server**

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Phase 2 refactor breaks existing code | High | High | Extensive testing, phased rollout |
| Java 17 migration delayed | Medium | Medium | MCP deferred to Phase 7, not blocking |
| pgvector performance issues | Low | Medium | Index optimization, query tuning |
| OAuth2 integration complexity | Medium | Medium | Use existing com.cloudempiere.auth plugin |
| Domain boundary violations | Medium | High | Comprehensive security tests |
| P2 repository setup issues | Low | Medium | Test locally before production deployment |

---

## Team Allocation

| Phase | Weeks | Developer | QA | DevOps |
|-------|-------|-----------|----|----|------|
| Phase 1 | 2 | 2 FTE | 0.5 FTE | 0 |
| Phase 2 | 4 | 3 FTE | 1 FTE | 0 |
| Phase 3 | 2 | 2 FTE | 0.5 FTE | 0 |
| Phase 4 | 4 | 2 FTE | 1 FTE | 0.5 FTE |
| Phase 5 | 2 | 2 FTE | 0.5 FTE | 0 |
| Phase 6 | 2 | 2 FTE | 0.5 FTE | 0 |
| Phase 7 | 2 | 2 FTE | 0.5 FTE | 0 |
| Phase 8 | 2 | 1 FTE | 1 FTE | 1 FTE |
| **Total** | **20** | **2-3 FTE** | **0.5-1 FTE** | **0.5-1 FTE** |

---

## Related Documents

- [ADR_CONSOLIDATION_2026.md](ADR_CONSOLIDATION_2026.md) - Master ADR reference
- [OSGI_MULTI_PLUGIN_ARCHITECTURE.md](OSGI_MULTI_PLUGIN_ARCHITECTURE.md) - Multi-plugin design
- [EXTERNAL_INTEGRATION_ARCHITECTURE.md](EXTERNAL_INTEGRATION_ARCHITECTURE.md) - External API design
- [ARCHITECTURE_DIAGRAMS.md](ARCHITECTURE_DIAGRAMS.md) - Layer interaction diagrams
- [LAYERED_ARCHITECTURE_DESIGN.md](LAYERED_ARCHITECTURE_DESIGN.md) - Five-layer facade architecture

**All ADRs referenced in this plan should be updated with implementation status at the end of each phase.**

---

**Document Status:** ✅ Complete
**Next Action:** Begin Phase 1 (Weeks 1-2)
**Owner:** Development Team
**Approval Required:** Architecture Review Board
