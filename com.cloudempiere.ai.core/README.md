# Cloudempiere AI - Core Infrastructure Plugin

**Bundle ID:** `com.cloudempiere.ai.core`
**Version:** 0.32.0-SNAPSHOT
**Type:** OSGi Bundle (Core Infrastructure)
**Status:** ✅ Phase 2 Week 4 Complete

## Purpose

Core infrastructure for AI capabilities in iDempiere. Provides shared business logic, provider interfaces, database security, and domain boundary enforcement used by all domain plugins.

## Architecture

```
┌─────────────────────────────────────┐
│   com.cloudempiere.ai.deps          │
│   (LangChain4j, AWS SDK, Jackson)   │
└─────────────┬───────────────────────┘
              │ Import-Package
              ↓
┌─────────────────────────────────────┐
│   com.cloudempiere.ai.core (THIS)   │ ← Provider Framework
│                                     │ ← Database Security
│                                     │ ← Context Providers
│                                     │ ← Domain Boundaries
└─────────────┬───────────────────────┘
              │ Import-Package
              ↓
┌─────────────────────────────────────┐
│   Domain Plugins                    │
│   - sales, inventory, purchasing,   │
│     support, kb                     │
└─────────────────────────────────────┘
```

## Contents (74 Source Files)

### 1. Provider Framework (`com.cloudempiere.ai.provider`)

**Purpose:** Unified interface for AI providers (Anthropic, AWS Bedrock, Ollama, OpenAI).

**Key Classes:**
- `IAIProvider` - Provider interface with text generation, embeddings, vision
- `AIProviderFactory` - OSGi service (@Component) for provider instantiation
- `AIProviderException` - Exception with error codes and retry flags

**DTOs:**
- `AIRequest` - Request wrapper (Builder pattern)
- `AIResponse` - Response with tokens, cost, function calls
- `AIMessage` - Conversation message (role, content, tools)
- `AITokenUsage` - Token tracking (input, output, total)
- `AIModelCapabilities` - Feature flags (streaming, vision, tools)
- `AIFunction` / `AIFunctionCall` - Tool use support
- `AIStreamCallback` - Streaming interface

**Example:**
```java
IAIProvider provider = AIProviderFactory.getProvider(providerId);

AIRequest request = AIRequest.builder()
    .message("Summarize sales for Q4 2025")
    .model("claude-3-5-sonnet-20241022")
    .temperature(0.7)
    .maxTokens(4096)
    .build();

AIResponse response = provider.generateText(request);
System.out.println(response.getContent());
System.out.println("Tokens: " + response.getTokenUsage().getTotalTokens());
System.out.println("Cost: $" + response.getCost());
```

### 2. LangChain4j Integration (`com.cloudempiere.ai.provider.langchain4j`)

**Purpose:** Production-grade provider implementation using LangChain4j.

**Key Classes:**
- `LangChain4jProviderFactory` - Creates ChatLanguageModel instances
- `BedrockChatModelWrapper` - AWS Bedrock integration (OSGi workaround)
- `ThreadAwareChatMemory` - Thread-isolated conversation memory

**Supported Providers:**
- **Anthropic** - Claude 3 Opus, Sonnet, Haiku, 3.5 Sonnet
- **AWS Bedrock** - Claude, Llama, Mistral via AWS
- **Ollama** - Llama 3.2, Mistral, local LLMs
- **OpenAI** - GPT-4, GPT-3.5 (via compatible API)
- **Mock OpenAI** - Local testing endpoint (localhost:8081)

**Configuration:**
```java
// Factory auto-selects based on AIG_Provider.ProviderType
ChatLanguageModel model = LangChain4jProviderFactory.create(
    "anthropic",           // Provider type
    "claude-3-5-sonnet",   // Model name
    apiKey,                // API key
    null                   // Optional base URL
);
```

### 3. Database Security (`com.cloudempiere.ai.database`)

**Purpose:** Secure execution of AI-generated SQL queries with role-based access control.

**Key Classes:**
- `SecureDatabaseQueryExecutor` - Query executor with security validation

**Security Model:**
1. Query executes as **AI User** (from AIG_Provider.AD_User_ID)
2. **+ Logged-in User's Role** (current session role)
3. **Principle:** If user's role cannot access a table, AI cannot either
4. Uses iDempiere's `AccessSqlParser` for permission checks
5. Audit trail records both AI user and initiating user

**Example:**
```java
SecureQueryRequest request = SecureQueryRequest.builder()
    .sql("SELECT C_Order_ID, GrandTotal FROM C_Order WHERE C_BPartner_ID = ?")
    .parameters(new Object[]{1000001})
    .ctx(Env.getCtx())
    .trxName(null)
    .build();

SecureQueryResult result = SecureDatabaseQueryExecutor.executeSecureQuery(request);
JSONArray rows = result.getResultSetAsJSON();
```

**DTOs:**
- `SecureQueryRequest` - Query + context info
- `SecureQueryResult` - Result set as JSON + metadata
- `SecureQueryAudit` - Audit log records

### 4. Context Providers (`com.cloudempiere.ai.context`)

**Purpose:** Extract business context from iDempiere windows, charts, and tabs.

**Key Classes:**
- `IAIContextProvider` - Interface for context extraction
- `WindowContextProvider` - Extracts data from GridTab/GridField
- `ChartContextProvider` - Extracts chart metadata and data
- `AIContextProviderRegistry` - Registry for provider lookup

**Example:**
```java
ContextParameters params = ContextParameters.builder()
    .windowNo(1)
    .tabNo(0)
    .ctx(Env.getCtx())
    .build();

IAIContextProvider provider = AIContextProviderRegistry.getProvider("window");
String context = provider.extractContext(params);

// Returns JSON:
// {
//   "table": "C_Order",
//   "currentRecord": {"C_Order_ID": 1000, "GrandTotal": 5000.00},
//   "fields": ["DocumentNo", "DateOrdered", "C_BPartner_ID", ...],
//   "selectedRows": [...]
// }
```

### 5. Domain Boundaries (`com.cloudempiere.ai.boundary`)

**Purpose:** Enforce table access restrictions per business domain (ADR-009).

**Key Classes:**
- `DomainBoundary` - Abstract base class for domain isolation

**Implementation:**
```java
public abstract class DomainBoundary {
    private final String domainName;
    private final Set<String> allowedTables;

    protected DomainBoundary(String domainName) {
        this.domainName = domainName;
        this.allowedTables = new HashSet<>();
        registerAllowedTables();
    }

    protected abstract void registerAllowedTables();

    protected void allow(String tableName) {
        allowedTables.add(tableName);
    }

    public void validateReadTable(String tableName) {
        if (!allowedTables.contains(tableName)) {
            throw new SecurityException(
                String.format("[%s Domain] Table %s not allowed. Allowed: %s",
                    domainName, tableName, allowedTables)
            );
        }
    }
}
```

**Usage in Domain Plugins:**
```java
// com.cloudempiere.ai.sales
public class SalesDomainBoundary extends DomainBoundary {
    public SalesDomainBoundary() {
        super("Sales");
    }

    protected void registerAllowedTables() {
        allow("C_Order");
        allow("C_OrderLine");
        allow("C_BPartner");
        allow("M_Product");  // Read-only
    }
}
```

### 6. Data Models (`com.cloudempiere.ai.model`)

**Purpose:** iDempiere business objects for AI configuration and usage tracking.

**Key Classes:**
- `MAIProvider` - AI provider configuration (API keys, models, settings)
- `MAIChat` - Chat sessions (extends MChat)
- `MAIUsageMetrics` - Token usage and cost tracking

**Example:**
```java
// Get AI provider
MAIProvider provider = new MAIProvider(ctx, providerId, trxName);
String apiKey = provider.getAPIKey();  // Encrypted
String modelName = provider.getModelName();
int aiUserId = provider.getAD_User_ID();  // AI user identity

// Create chat session
MAIChat chat = new MAIChat(ctx, 0, trxName);
chat.setAD_Table_ID(MAIChat.AI_GLOBAL_TABLE_ID);
chat.setRecord_ID(Env.getAD_User_ID(ctx));
chat.save();
```

### 7. Utilities (`com.cloudempiere.ai.util`)

**Purpose:** Helper classes for markdown rendering, sanitization, and zoom links.

**Key Classes:**
- `StreamingMarkdownRenderer` - Progressive HTML rendering (ADR-054, ADR-055)
- `MarkdownSyntaxSanitizer` - Removes unsafe markdown constructs
- `MarkdownTableRenderer` - Renders tables with iDempiere styling
- `ZoomLinkProcessor` - Converts table references to zoom links

**Example:**
```java
// ADR-054: HTML-only storage, no raw markdown
String markdown = "# Sales Report\n\nTotal: **$5,000**";
String html = StreamingMarkdownRenderer.renderToHtml(markdown);
// Returns: <h3>Sales Report</h3><p>Total: <strong>$5,000</strong></p>

// Store HTML in CM_ChatEntry.CharacterData
chatEntry.setCharacterData(html);
chatEntry.save();
```

### 8. Component (`com.cloudempiere.ai.component`)

**Status:** Partially implemented (streaming message component active).

**Key Classes:**
- `AIChatStreamingMessage` - ZK component for streaming AI responses
- `AIChatWidget` (disabled) - Full chat UI (depends on future services)

## Exported Packages (10 APIs)

```
com.cloudempiere.ai;version="0.32.0"
com.cloudempiere.ai.boundary;version="0.32.0"
com.cloudempiere.ai.component;version="0.32.0"
com.cloudempiere.ai.context;version="0.32.0"
com.cloudempiere.ai.database;version="0.32.0"
com.cloudempiere.ai.database.dto;version="0.32.0"
com.cloudempiere.ai.model;version="0.32.0"
com.cloudempiere.ai.provider;version="0.32.0"
com.cloudempiere.ai.provider.dto;version="0.32.0"
com.cloudempiere.ai.provider.langchain4j;version="0.32.0"
```

## Imported Packages

**From iDempiere:**
- `org.compiere.model`, `org.compiere.util`, `org.compiere.process`
- `org.zkoss.zk.ui`, `org.zkoss.zul`, `org.zkoss.zhtml`
- `org.adempiere.plugin.utils`

**From deps plugin:**
- `dev.langchain4j.*;version="0.35.0"`
- `software.amazon.awssdk.*;version="2.20.162"`

**From OSGi:**
- `org.osgi.framework;version="1.10.0"`
- `org.osgi.service.component.annotations;version="1.3.0"`

## Building

```bash
cd com.cloudempiere.ai.core
mvn clean install
```

**Prerequisites:**
1. `com.cloudempiere.ai.deps` must be built first
2. iDempiere parent and target platform installed

**Build Output:**
- `target/com.cloudempiere.ai.core-0.32.0-SNAPSHOT.jar`
- 74 source files compiled, 0 errors

## Future-Phase Features (Disabled)

The following features are temporarily disabled (Week 4 focused on infrastructure):

| Feature | Status | Files | ADR | Re-enable |
|---------|--------|-------|-----|-----------|
| **Metrics/Observability** | Commented out | `AIMetricsListener` | ADR-013 | Phase 3 |
| **Chat Access Control** | Commented out | `ChatAccessService` | ADR-036 | Phase 3 |
| **Language Detection** | Commented out | `LanguageDetectionService` | ADR-037 | Phase 3 |
| **RAG Service** | Disabled | `RagTools.java.disabled` | ADR-012 | Phase 3 |
| **Guardrails** | Disabled | `AIService.java.disabled` | ADR-014 | Phase 3 |
| **Knowledge Base** | Disabled | `KnowledgeBaseContextProvider.java.disabled` | ADR-016 | Phase 4 |
| **Chat UI** | Disabled | `AIChatWidget.java.disabled` | ADR-033 | Phase 3 |

**Re-enabling:** Remove `.disabled` suffix and uncomment imports when dependent services are implemented.

## Testing

```bash
# Run all tests
cd ../com.cloudempiere.ai.test
mvn test

# Test specific provider
mvn test -Dtest=LangChain4jProviderFactoryTest
```

**Test Coverage:**
- Provider factory tests
- Database security tests
- Context provider tests
- Domain boundary tests

## Usage in Domain Plugins

```java
// com.cloudempiere.ai.sales/src/.../SalesAgent.java
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.dto.AIRequest;
import com.cloudempiere.ai.provider.dto.AIResponse;
import com.cloudempiere.ai.boundary.DomainBoundary;
import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;

public class SalesAgent {
    private DomainBoundary boundary = new SalesDomainBoundary();
    private IAIProvider provider;

    public String analyzeSales(int orderId) {
        // Domain boundary validation
        boundary.validateReadTable("C_Order");

        // Secure database query
        SecureQueryResult result = SecureDatabaseQueryExecutor.executeSecureQuery(...);

        // AI analysis
        AIRequest request = AIRequest.builder()
            .message("Analyze this order: " + result.getResultSetAsJSON())
            .build();

        AIResponse response = provider.generateText(request);
        return response.getContent();
    }
}
```

## Dependencies

**Required:**
- `com.cloudempiere.ai.deps` (LangChain4j, AWS SDK)
- iDempiere v10 (Java 11)
- OSGi Framework 1.10.0+

**Optional:**
- ZK Framework (for UI components)

## Troubleshooting

### Provider Not Found
**Symptom:** `AIProviderFactory.getProvider(id)` returns null

**Solution:**
1. Check AIG_Provider record exists
2. Verify provider is active (IsActive='Y')
3. Check API key is configured

### Security Exception
**Symptom:** `SecurityException: [Sales Domain] Table M_Warehouse not allowed`

**Solution:**
- AI agent trying to access table outside domain boundary
- This is correct behavior - do not bypass security
- Update domain boundary if table access is legitimately needed

### ClassNotFoundException
**Symptom:** `NoClassDefFoundError: dev/langchain4j/model/chat/ChatLanguageModel`

**Solution:**
1. Verify deps plugin is installed: `ss com.cloudempiere.ai.deps`
2. Rebuild deps plugin if missing
3. Check MANIFEST.MF Import-Package declarations

## References

- **ADR-009:** Domain Boundaries and Agent Scope
- **ADR-012:** RAG-Based Context Retrieval
- **ADR-013:** Observability and Cost Tracking
- **ADR-014:** Guardrails and Safety
- **ADR-033:** Streaming Responses and Thinking Timeline UX
- **ADR-035:** Java Version Strategy
- **ADR-036:** Chat Ownership and Sharing Model
- **ADR-054:** HTML-only Chat Message Storage
- **ADR-055:** Constrained Markdown Syntax
- **PLUGIN_ARCHITECTURE.md:** Multi-plugin design rationale
- **PHASE_2_WEEK_4_COMPLETION.md:** Week 4 completion report

## Maintainers

Cloudempiere AI Team

**Last Updated:** 2026-01-30
