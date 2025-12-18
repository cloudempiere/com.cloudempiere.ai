# Mock AI Hub Provider Implementation

**Date:** 2025-12-14
**Version:** 0.10.0

## Summary

Added **Mock AI Hub** provider type (MOA) to support testing iDempiere AI Hub integration without requiring full AI Hub deployment. Also renamed all "Satellite" references to "AI Hub" for clarity.

---

## Current Provider Landscape

| Provider | Code | Port | Type | Purpose |
|----------|------|------|------|---------|
| **Anthropic Claude** | ANT | N/A | External | Direct Claude API |
| **AWS Bedrock** | ABE | N/A | External | AWS Foundation Models |
| **Mock AI Hub** | MOA | 8081 | Local Mock | Testing without AI Hub |
| **Ollama** | OLL | 11434 | Local | Local LLMs |
| **iDempiere AI Hub** | SAT | 8090 | Service | Production AI Hub (Quarkus) |

---

## What is Mock AI Hub?

**Mock AI Hub** simulates the iDempiere AI Hub API for testing purposes:

```
Production (Port 8090):
┌──────────────────────────────┐
│ iDempiere AI Hub (SAT)       │
│ - Full Quarkus deployment    │
│ - LangChain4j 1.x features   │
│ - MCP support                │
│ - Advanced caching           │
│ - Multi-tenant               │
└──────────────────────────────┘

Testing (Port 8081):
┌──────────────────────────────┐
│ Mock AI Hub (MOA)            │
│ - OpenAI-compatible mock     │
│ - Local Ollama backend       │
│ - No AI Hub required         │
│ - For unit/integration tests │
└──────────────────────────────┘
```

---

## Changes Made

### 1. Code Changes

#### X_AIG_Provider.java
```java
// Added constant (line 159-160)
/** Mock AI Hub = MOA */
public static final String AIGPROVIDERTYPE_MockAIHub = "MOA";
```

#### LangChain4jProviderFactory.java

**Constants Added (lines 47, 52, 58-59, 67):**
```java
public static final String PROVIDER_MOCK_AI_HUB = X_AIG_Provider.AIGPROVIDERTYPE_MockAIHub;
public static final String PROVIDER_AI_HUB = X_AIG_Provider.AIGPROVIDERTYPE_QuarkusSatellite;

private static final String DEFAULT_MOCK_AI_HUB_MODEL = "llama3.2";
private static final String DEFAULT_MOCK_AI_HUB_URL = "http://localhost:8081/v1";
private static final String DEFAULT_MOCK_AI_HUB_EMBEDDING_MODEL = "nomic-embed-text";
```

**Factory Methods Added:**
- `createMockAIHubModel()` - Chat model (lines 450-465)
- `createMockAIHubStreamingModel()` - Streaming (lines 475-484)
- `createMockAIHubEmbeddingModel()` - Embeddings (lines 665-671)

**Switch Cases Updated:**
- `create()` method - line 131-132
- `createStreaming()` method - line 187-188
- `createEmbeddingModel()` method - line 277-278
- `hasNativeEmbeddings()` method - line 320

**Renamed Satellite → AI Hub:**
- All method names: `createSatellite*` → `createAIHub*`
- All variables: `DEFAULT_SATELLITE_*` → `DEFAULT_AI_HUB_*`
- Method `isSatelliteHealthy()` → `isAIHubHealthy()`
- Comments and documentation updated

### 2. Database Migration

**Files Created:**
- `migration/postgresql/202512142110_Mock_AIHub_Provider.sql`
- `migration/oracle/202512142110_Mock_AIHub_Provider.sql`

**Migration Content:**
```sql
INSERT INTO AD_Ref_List VALUES (
    800301,  -- AD_Ref_List_ID
    'Mock AI Hub',  -- Name
    'Mock AI Hub for testing (port 8081) - simulates iDempiere AI Hub without full deployment',  -- Description
    800124,  -- AIGProviderType reference
    'MOA',   -- Value
    ...
);
```

---

## Configuration

### Setup Mock AI Hub Provider

1. **Apply Migration:**
   ```bash
   psql -U adempiere -d idempiere -f migration/postgresql/202512142110_Mock_AIHub_Provider.sql
   ```

2. **Create Provider in iDempiere UI:**
   - System Admin → AI → AI Provider → New
   - **Name:** Mock AI Hub
   - **Provider Type:** Mock AI Hub (MOA)
   - **Model Name:** llama3.2
   - **API Key:** test (optional)
   - **URL:** http://localhost:8081/v1 (or leave blank for default)

3. **Start Mock Server:**
   ```bash
   # This is your Quarkus mock service on port 8081
   # (The one showing in your console output)
   ```

### Setup Real AI Hub Provider

1. **Update Existing SAT Provider:**
   - System Admin → AI → AI Provider → Find SAT provider
   - **Name:** iDempiere AI Hub
   - **Provider Type:** Quarkus Satellite (SAT)
   - **URL:** http://localhost:8090
   - **Model Name:** claude-sonnet-4

2. **Start AI Hub Service:**
   ```bash
   # Start the full Quarkus AI Hub on port 8090
   ```

---

## Use Cases

### When to Use Mock AI Hub (MOA, port 8081)

✅ **Use for:**
- Unit testing
- Integration testing in CI/CD
- Local development without AI Hub infrastructure
- Quick prototyping
- Cost-free testing

❌ **Don't use for:**
- Production deployments
- Customer-facing features
- Features requiring MCP support
- Features requiring extended thinking

### When to Use Real AI Hub (SAT, port 8090)

✅ **Use for:**
- Production deployments
- Customer-facing AI features
- MCP (Model Context Protocol) support
- Extended thinking/reasoning
- Advanced caching and observability
- Multi-tenant scenarios

---

## Testing

### Example Test with Mock AI Hub

```java
@Test
public void testWithMockAIHub() {
    // Setup Mock AI Hub provider
    MAIProvider provider = new MAIProvider(ctx, 0, trxName);
    provider.setName("Mock AI Hub");
    provider.setAIGProviderType(X_AIG_Provider.AIGPROVIDERTYPE_MockAIHub);
    provider.setModelName("llama3.2");
    provider.setAPIKey("test");
    provider.saveEx();

    // Create chat model
    ChatLanguageModel model = LangChain4jProviderFactory.create(provider);

    // Test
    String response = model.generate("Hello, test!");
    assertNotNull(response);
    assertTrue(response.length() > 0);
}
```

---

## Backward Compatibility

✅ **Fully Backward Compatible:**
- Existing providers (ANT, ABE, OLL, SAT) unchanged
- No breaking changes to API
- SAT provider still works (now called "AI Hub" internally)
- Migration adds new provider, doesn't modify existing ones

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────────┐
│ iDempiere AI Plugin                                     │
│                                                         │
│  LangChain4jProviderFactory.create(config)             │
│         ↓                                              │
│  switch (providerType) {                               │
│    case PROVIDER_MOCK_AI_HUB (MOA):                    │
│      → createMockAIHubModel()                          │
│         → OpenAiChatModel.builder()                    │
│            .baseUrl("http://localhost:8081/v1")        │
│            .apiKey("test")                             │
│            → Mock Server (Ollama backend)              │
│                                                         │
│    case PROVIDER_AI_HUB (SAT):                         │
│      → createAIHubModel()                              │
│         → OpenAiChatModel.builder()                    │
│            .baseUrl("http://localhost:8090/v1")        │
│            → Real Quarkus AI Hub                       │
│  }                                                     │
└─────────────────────────────────────────────────────────┘
```

---

## Next Steps

1. ✅ **Apply migration scripts** to add MOA to database
2. ✅ **Test Mock AI Hub** provider with local Ollama
3. ⏳ **Update documentation** (README, user guides)
4. ⏳ **Create unit tests** for Mock AI Hub
5. ⏳ **Update ADR-042** to reflect AI Hub naming

---

## Files Modified

| File | Changes |
|------|---------|
| `X_AIG_Provider.java` | Added AIGPROVIDERTYPE_MockAIHub constant |
| `LangChain4jProviderFactory.java` | Added Mock AI Hub methods, renamed Satellite → AI Hub |
| `migration/postgresql/202512142110_Mock_AIHub_Provider.sql` | Created |
| `migration/oracle/202512142110_Mock_AIHub_Provider.sql` | Created |

---

## Terminology Clarification

| Old Term | New Term | Reason |
|----------|----------|--------|
| Satellite | AI Hub | More descriptive, matches Quarkus service name |
| iDempiere-CLI | Mock AI Hub | Clarifies it's a mock for testing |
| Quarkus Satellite | iDempiere AI Hub | Aligns with actual product name |

---

## Summary

**Added:** Mock AI Hub provider (MOA, port 8081) for testing without AI Hub deployment
**Renamed:** All "Satellite" references to "AI Hub" for clarity
**Status:** ✅ Complete and ready for testing
**Version:** 0.10.0
