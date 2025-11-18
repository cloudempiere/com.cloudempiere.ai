# AI Provider Implementation Plan for iDempiere

## Overview

This document provides a comprehensive implementation plan for AI provider integration in the com.cloudempiere.ai plugin. The architecture uses **AIGProviderType** from the database to dynamically load the appropriate IAIProvider implementation through a factory pattern.

---

## Architecture Principles

### 1. **Database-Driven Provider Discovery**
- **AIGProviderType field** in AIG_Provider table determines which implementation to load
- Factory uses AIGProviderType value to instantiate correct provider class
- No hardcoded provider type strings in application code
- New providers can be added by creating implementation + DB reference list entry

### 2. **Provider Agnostic Design**
- Generic IAIProvider interface that works with any AI service
- Provider-specific implementations hidden behind abstraction
- Easy to add new providers without modifying core code

### 3. **Extensibility**
- Support for text generation, embeddings, vision, audio
- Pluggable architecture via OSGi services
- Provider capabilities declared at runtime

### 4. **Resilience**
- Failover to alternative providers
- Retry logic with exponential backoff
- Circuit breaker pattern for unhealthy providers

### 5. **Observability**
- Request/response logging
- Performance metrics (latency, tokens, cost)
- Health monitoring

---

## Database Schema - AIG_Provider Table

### Field: AIGProviderType

**Purpose**: Reference list field that identifies which IAIProvider implementation to instantiate.

**Database Definition**:
```sql
-- Add AIGProviderType field to AIG_Provider table
ALTER TABLE AIG_Provider
ADD COLUMN AIGProviderType VARCHAR(60);

-- Create reference list for provider types
INSERT INTO AD_Reference (AD_Reference_ID, Name, ValidationType, EntityType)
VALUES (nextval('ad_reference_seq'), 'AIG Provider Type', 'L', 'U');

-- Add reference list values
INSERT INTO AD_Ref_List (AD_Ref_List_ID, AD_Reference_ID, Value, Name, EntityType)
VALUES
  (nextval('ad_ref_list_seq'), <ref_id>, 'OPENAI', 'OpenAI', 'U'),
  (nextval('ad_ref_list_seq'), <ref_id>, 'ANTHROPIC', 'Anthropic Claude', 'U'),
  (nextval('ad_ref_list_seq'), <ref_id>, 'AWS_BEDROCK', 'AWS Bedrock', 'U'),
  (nextval('ad_ref_list_seq'), <ref_id>, 'GOOGLE_AI', 'Google AI (Gemini)', 'U'),
  (nextval('ad_ref_list_seq'), <ref_id>, 'AZURE_OPENAI', 'Azure OpenAI', 'U'),
  (nextval('ad_ref_list_seq'), <ref_id>, 'OLLAMA', 'Ollama (Local LLM)', 'U'),
  (nextval('ad_ref_list_seq'), <ref_id>, 'HUGGINGFACE', 'HuggingFace Inference', 'U'),
  (nextval('ad_ref_list_seq'), <ref_id>, 'COHERE', 'Cohere', 'U'),
  (nextval('ad_ref_list_seq'), <ref_id>, 'MISTRAL', 'Mistral AI', 'U');
```

**Value Mapping**:
| AIGProviderType Value | Java Implementation Class |
|-----------------------|---------------------------|
| OPENAI | `com.cloudempiere.ai.provider.impl.OpenAIProvider` |
| ANTHROPIC | `com.cloudempiere.ai.provider.impl.AnthropicProvider` |
| AWS_BEDROCK | `com.cloudempiere.ai.provider.impl.AWSBedrockProvider` |
| GOOGLE_AI | `com.cloudempiere.ai.provider.impl.GoogleAIProvider` |
| AZURE_OPENAI | `com.cloudempiere.ai.provider.impl.AzureOpenAIProvider` |
| OLLAMA | `com.cloudempiere.ai.provider.impl.OllamaProvider` |
| HUGGINGFACE | `com.cloudempiere.ai.provider.impl.HuggingFaceProvider` |
| COHERE | `com.cloudempiere.ai.provider.impl.CohereProvider` |
| MISTRAL | `com.cloudempiere.ai.provider.impl.MistralProvider` |

---

## Core Interface Design

### IAIProvider - Main Provider Interface

```java
package com.cloudempiere.ai.provider;

import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import com.cloudempiere.ai.model.MAIProvider;

/**
 * Generic interface for AI providers (OpenAI, Anthropic, AWS, Google, etc.)
 *
 * Implementation classes are loaded dynamically based on the AIGProviderType
 * field value from the AIG_Provider database table.
 *
 * Each provider implementation must:
 * 1. Implement this interface
 * 2. Have a corresponding AIGProviderType value in AD_Ref_List
 * 3. Be registered in AIProviderFactory
 *
 * @author Cloudempiere
 * @version 1.0
 */
public interface IAIProvider {

    /**
     * Initialize the provider with configuration from database
     * Called once when provider is loaded
     *
     * @param provider MAIProvider model containing configuration
     * @throws AIProviderException if initialization fails
     */
    void initialize(MAIProvider provider) throws AIProviderException;

    /**
     * Get provider type identifier
     * Must match the AIGProviderType value from the database
     *
     * Example: "OPENAI", "ANTHROPIC", "AWS_BEDROCK"
     *
     * @return provider type identifier matching AD_Ref_List.Value
     */
    String getProviderType();

    /**
     * Get provider display name for UI
     *
     * @return human-readable provider name
     */
    String getProviderName();

    /**
     * Get API version supported by this implementation
     *
     * @return API version string (e.g., "v1", "2023-01-01")
     */
    String getAPIVersion();

    // ========================================================================
    // CAPABILITY CHECKS
    // ========================================================================

    /**
     * Check if provider supports text generation (chat/completion)
     *
     * @return true if text generation is supported
     */
    boolean supportsTextGeneration();

    /**
     * Check if provider supports embeddings/vector generation
     *
     * @return true if embeddings are supported
     */
    boolean supportsEmbeddings();

    /**
     * Check if provider supports vision (image analysis)
     *
     * @return true if vision is supported
     */
    boolean supportsVision();

    /**
     * Check if provider supports audio transcription
     *
     * @return true if audio transcription is supported
     */
    boolean supportsAudio();

    /**
     * Check if provider supports streaming responses
     *
     * @return true if streaming is supported
     */
    boolean supportsStreaming();

    /**
     * Check if provider supports function/tool calling
     *
     * @return true if function calling is supported
     */
    boolean supportsFunctionCalling();

    /**
     * Get list of supported models for this provider
     *
     * @return list of model identifiers (e.g., ["gpt-4", "gpt-3.5-turbo"])
     */
    List<String> getSupportedModels();

    /**
     * Get capabilities for a specific model
     *
     * @param modelName model identifier
     * @return capabilities object describing model features
     */
    AIModelCapabilities getModelCapabilities(String modelName);

    // ========================================================================
    // TEXT GENERATION (PRIMARY FUNCTIONALITY)
    // ========================================================================

    /**
     * Generate text completion/chat response (synchronous)
     *
     * This is the main method for AI text generation. It handles:
     * - Single-turn completions
     * - Multi-turn conversations
     * - System prompts
     * - Temperature/sampling control
     *
     * @param request AIRequest containing prompt, parameters, and context
     * @return AIResponse with generated text and metadata
     * @throws AIProviderException if generation fails
     */
    AIResponse generateText(AIRequest request) throws AIProviderException;

    /**
     * Generate text completion with streaming (asynchronous)
     * Useful for real-time UI updates as text is generated
     *
     * @param request AIRequest containing prompt and parameters
     * @param callback AIStreamCallback to receive text chunks
     * @throws AIProviderException if streaming fails
     */
    void generateTextStream(AIRequest request, AIStreamCallback callback)
        throws AIProviderException;

    /**
     * Generate text with function/tool calling
     * Allows AI to call external functions and use results in response
     *
     * @param request AIRequest with prompt
     * @param functions List of available functions
     * @return AIResponse with text and function calls
     * @throws AIProviderException if generation fails
     */
    AIResponse generateTextWithFunctions(AIRequest request, List<AIFunction> functions)
        throws AIProviderException;

    // ========================================================================
    // EMBEDDINGS (VECTOR GENERATION)
    // ========================================================================

    /**
     * Generate embeddings/vectors for text
     * Used for semantic search, similarity matching, RAG systems
     *
     * @param text input text to embed
     * @param modelName embedding model to use
     * @return float array representing the embedding vector
     * @throws AIProviderException if embedding generation fails
     */
    float[] generateEmbedding(String text, String modelName)
        throws AIProviderException;

    /**
     * Generate embeddings for multiple texts in batch
     * More efficient than calling generateEmbedding() multiple times
     *
     * @param texts list of input texts
     * @param modelName embedding model to use
     * @return list of embedding vectors
     * @throws AIProviderException if batch embedding fails
     */
    List<float[]> generateEmbeddingsBatch(List<String> texts, String modelName)
        throws AIProviderException;

    // ========================================================================
    // VISION (IMAGE ANALYSIS)
    // ========================================================================

    /**
     * Analyze image and generate description/answer questions
     *
     * @param imageData byte array of image data
     * @param prompt question or instruction about the image
     * @param modelName vision model to use
     * @return AIResponse with image analysis
     * @throws AIProviderException if analysis fails
     */
    AIResponse analyzeImage(byte[] imageData, String prompt, String modelName)
        throws AIProviderException;

    /**
     * Analyze image from URL
     *
     * @param imageUrl URL of the image
     * @param prompt question or instruction
     * @param modelName vision model to use
     * @return AIResponse with analysis
     * @throws AIProviderException if analysis fails
     */
    AIResponse analyzeImageURL(String imageUrl, String prompt, String modelName)
        throws AIProviderException;

    // ========================================================================
    // AUDIO (TRANSCRIPTION)
    // ========================================================================

    /**
     * Transcribe audio to text
     *
     * @param audioData byte array of audio file
     * @param audioFormat format (e.g., "mp3", "wav", "m4a")
     * @param language optional language code (e.g., "en", "es")
     * @return transcribed text
     * @throws AIProviderException if transcription fails
     */
    String transcribeAudio(byte[] audioData, String audioFormat, String language)
        throws AIProviderException;

    // ========================================================================
    // HEALTH & MONITORING
    // ========================================================================

    /**
     * Perform health check on provider
     * Tests connectivity and API key validity
     *
     * @return AIHealthStatus with health information
     */
    AIHealthStatus checkHealth();

    /**
     * Get current rate limit status
     *
     * @return AIRateLimitStatus with remaining requests/tokens
     */
    AIRateLimitStatus getRateLimitStatus();

    /**
     * Test provider with a simple request
     * Used for validation during configuration
     *
     * @return true if test successful, false otherwise
     */
    boolean testConnection();

    // ========================================================================
    // COST & USAGE
    // ========================================================================

    /**
     * Calculate estimated cost for a request
     *
     * @param request AIRequest to estimate
     * @return estimated cost in USD
     */
    double estimateCost(AIRequest request);

    /**
     * Get token count for text (for cost estimation)
     *
     * @param text input text
     * @param modelName model to use for tokenization
     * @return approximate token count
     */
    int estimateTokenCount(String text, String modelName);

    // ========================================================================
    // LIFECYCLE
    // ========================================================================

    /**
     * Shutdown provider and release resources
     * Called when provider is being unloaded
     */
    void shutdown();

    /**
     * Check if provider is initialized and ready
     *
     * @return true if ready to process requests
     */
    boolean isReady();

    /**
     * Get last error message (if any)
     *
     * @return error message or null
     */
    String getLastError();
}
```

---

## Provider Factory with AIGProviderType Lookup

### AIProviderFactory - Dynamic Provider Instantiation

```java
package com.cloudempiere.ai.provider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.compiere.util.CLogger;
import com.cloudempiere.ai.model.MAIProvider;

/**
 * Factory for creating and managing AI provider instances.
 * Uses AIGProviderType from database to determine which implementation to load.
 *
 * Provider Registration Flow:
 * 1. Provider implementation class created (e.g., OpenAIProvider)
 * 2. AIGProviderType value added to AD_Ref_List (e.g., "OPENAI")
 * 3. Factory maps AIGProviderType value to implementation class
 * 4. When provider loaded from DB, factory instantiates correct class
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIProviderFactory {

    private static final CLogger log = CLogger.getCLogger(AIProviderFactory.class);

    // Singleton instance
    private static AIProviderFactory instance;

    /**
     * Registry mapping AIGProviderType values to implementation classes
     * Key: AIGProviderType value from database (e.g., "OPENAI")
     * Value: IAIProvider implementation class
     */
    private Map<String, Class<? extends IAIProvider>> providerRegistry;

    /**
     * Cache of initialized provider instances
     * Key: AIG_Provider_ID from database
     * Value: Initialized IAIProvider instance
     */
    private Map<Integer, IAIProvider> providerCache;

    /**
     * Private constructor for singleton pattern
     */
    private AIProviderFactory() {
        this.providerRegistry = new ConcurrentHashMap<>();
        this.providerCache = new ConcurrentHashMap<>();
        registerDefaultProviders();
    }

    /**
     * Get singleton instance
     */
    public static synchronized AIProviderFactory getInstance() {
        if (instance == null) {
            instance = new AIProviderFactory();
        }
        return instance;
    }

    /**
     * Register default provider implementations
     * Maps AIGProviderType values to implementation classes
     *
     * This method is called during factory initialization and registers
     * all built-in provider implementations.
     */
    private void registerDefaultProviders() {
        // Register OpenAI
        registerProvider("OPENAI",
            com.cloudempiere.ai.provider.impl.OpenAIProvider.class);

        // Register Anthropic Claude
        registerProvider("ANTHROPIC",
            com.cloudempiere.ai.provider.impl.AnthropicProvider.class);

        // Register AWS Bedrock
        registerProvider("AWS_BEDROCK",
            com.cloudempiere.ai.provider.impl.AWSBedrockProvider.class);

        // Register Google AI (Gemini)
        registerProvider("GOOGLE_AI",
            com.cloudempiere.ai.provider.impl.GoogleAIProvider.class);

        // Register Azure OpenAI
        registerProvider("AZURE_OPENAI",
            com.cloudempiere.ai.provider.impl.AzureOpenAIProvider.class);

        // Register Ollama (local LLM)
        registerProvider("OLLAMA",
            com.cloudempiere.ai.provider.impl.OllamaProvider.class);

        // Register HuggingFace Inference
        registerProvider("HUGGINGFACE",
            com.cloudempiere.ai.provider.impl.HuggingFaceProvider.class);

        // Register Cohere
        registerProvider("COHERE",
            com.cloudempiere.ai.provider.impl.CohereProvider.class);

        // Register Mistral AI
        registerProvider("MISTRAL",
            com.cloudempiere.ai.provider.impl.MistralProvider.class);

        log.info("Registered " + providerRegistry.size() + " AI provider implementations");
    }

    /**
     * Register a provider implementation
     * This method allows plugins to register custom provider implementations
     *
     * @param providerType AIGProviderType value from AD_Ref_List
     * @param providerClass implementation class implementing IAIProvider
     */
    public void registerProvider(String providerType, Class<? extends IAIProvider> providerClass) {
        if (providerType == null || providerType.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider type cannot be null or empty");
        }

        if (providerClass == null) {
            throw new IllegalArgumentException("Provider class cannot be null");
        }

        providerRegistry.put(providerType, providerClass);
        log.fine("Registered provider implementation: " + providerType +
                 " -> " + providerClass.getName());
    }

    /**
     * Unregister a provider implementation
     * Used when a provider plugin is being unloaded
     *
     * @param providerType AIGProviderType value
     */
    public void unregisterProvider(String providerType) {
        if (providerRegistry.remove(providerType) != null) {
            log.info("Unregistered provider: " + providerType);

            // Clear cached instances of this provider type
            providerCache.entrySet().removeIf(entry -> {
                IAIProvider provider = entry.getValue();
                if (provider.getProviderType().equals(providerType)) {
                    provider.shutdown();
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Get or create provider instance from database configuration
     * Uses AIGProviderType field to determine which implementation to load
     *
     * @param providerConfig MAIProvider from database with AIGProviderType set
     * @return initialized provider instance
     * @throws AIProviderException if provider cannot be created or initialized
     */
    public IAIProvider getProvider(MAIProvider providerConfig) throws AIProviderException {
        if (providerConfig == null) {
            throw new AIProviderException("Provider configuration is null");
        }

        int providerId = providerConfig.getAIG_Provider_ID();
        if (providerId <= 0) {
            throw new AIProviderException("Invalid provider ID: " + providerId);
        }

        // Check cache first
        if (providerCache.containsKey(providerId)) {
            IAIProvider cached = providerCache.get(providerId);
            if (cached.isReady()) {
                log.finest("Returning cached provider: " + providerConfig.getName());
                return cached;
            } else {
                // Cached provider is not ready, remove it and recreate
                log.warning("Cached provider not ready, recreating: " + providerConfig.getName());
                providerCache.remove(providerId);
                cached.shutdown();
            }
        }

        // Get AIGProviderType from database
        String providerType = providerConfig.getAIGProviderType();
        if (providerType == null || providerType.trim().isEmpty()) {
            throw new AIProviderException(
                "AIGProviderType is not set for provider: " + providerConfig.getName() +
                " (ID: " + providerId + ")"
            );
        }

        // Lookup implementation class
        Class<? extends IAIProvider> providerClass = providerRegistry.get(providerType);
        if (providerClass == null) {
            throw new AIProviderException(
                "No implementation registered for provider type: " + providerType +
                ". Available types: " + providerRegistry.keySet()
            );
        }

        try {
            // Instantiate provider using reflection
            log.fine("Creating provider instance: " + providerType +
                    " using class: " + providerClass.getName());

            IAIProvider provider = providerClass.getDeclaredConstructor().newInstance();

            // Initialize with configuration from database
            log.fine("Initializing provider: " + providerConfig.getName());
            provider.initialize(providerConfig);

            // Verify provider type matches
            if (!providerType.equals(provider.getProviderType())) {
                throw new AIProviderException(
                    "Provider type mismatch: expected " + providerType +
                    " but provider reports " + provider.getProviderType()
                );
            }

            // Cache the initialized provider
            providerCache.put(providerId, provider);

            log.info("Successfully created and initialized provider: " +
                    providerConfig.getName() + " (Type: " + providerType + ")");

            return provider;

        } catch (AIProviderException e) {
            // Re-throw AIProviderException as-is
            throw e;
        } catch (Exception e) {
            // Wrap other exceptions
            log.severe("Failed to create provider instance: " + e.getMessage());
            throw new AIProviderException(
                "Failed to instantiate provider class " + providerClass.getName() +
                " for type " + providerType + ": " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Get provider by database ID
     * Convenience method that loads MAIProvider from database
     *
     * @param ctx context
     * @param providerId AIG_Provider_ID
     * @param trxName transaction name
     * @return initialized provider instance
     * @throws AIProviderException if provider cannot be loaded
     */
    public IAIProvider getProvider(Properties ctx, int providerId, String trxName)
            throws AIProviderException {

        MAIProvider config = new MAIProvider(ctx, providerId, trxName);

        if (config.getAIG_Provider_ID() <= 0) {
            throw new AIProviderException("Provider not found with ID: " + providerId);
        }

        if (!config.isActive()) {
            throw new AIProviderException("Provider is not active: " + config.getName());
        }

        return getProvider(config);
    }

    /**
     * Get list of all registered provider types
     * Returns AIGProviderType values that can be used in database
     *
     * @return list of provider type identifiers
     */
    public List<String> getRegisteredProviderTypes() {
        return new ArrayList<>(providerRegistry.keySet());
    }

    /**
     * Check if a provider type is registered
     *
     * @param providerType AIGProviderType value
     * @return true if implementation is registered
     */
    public boolean isProviderTypeRegistered(String providerType) {
        return providerRegistry.containsKey(providerType);
    }

    /**
     * Get implementation class for a provider type
     *
     * @param providerType AIGProviderType value
     * @return implementation class or null if not found
     */
    public Class<? extends IAIProvider> getProviderClass(String providerType) {
        return providerRegistry.get(providerType);
    }

    /**
     * Clear cached providers
     * Shuts down all cached provider instances and clears the cache
     */
    public void clearCache() {
        log.info("Clearing provider cache (" + providerCache.size() + " instances)");

        for (IAIProvider provider : providerCache.values()) {
            try {
                provider.shutdown();
            } catch (Exception e) {
                log.warning("Error shutting down provider: " + e.getMessage());
            }
        }

        providerCache.clear();
        log.info("Provider cache cleared");
    }

    /**
     * Invalidate cached provider for specific ID
     * Forces reload on next access
     *
     * @param providerId AIG_Provider_ID
     */
    public void invalidateProvider(int providerId) {
        IAIProvider provider = providerCache.remove(providerId);
        if (provider != null) {
            try {
                provider.shutdown();
                log.info("Invalidated provider cache for ID: " + providerId);
            } catch (Exception e) {
                log.warning("Error shutting down provider: " + e.getMessage());
            }
        }
    }

    /**
     * Get statistics about cached providers
     *
     * @return map with statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cached_providers", providerCache.size());
        stats.put("registered_types", providerRegistry.size());

        // Count providers by type
        Map<String, Integer> byType = new HashMap<>();
        for (IAIProvider provider : providerCache.values()) {
            String type = provider.getProviderType();
            byType.put(type, byType.getOrDefault(type, 0) + 1);
        }
        stats.put("providers_by_type", byType);

        return stats;
    }

    /**
     * Shutdown all providers and clear factory
     * Called during plugin deactivation
     */
    public void shutdownAll() {
        log.info("Shutting down all AI providers");
        clearCache();
        log.info("AI provider factory shut down complete");
    }

    /**
     * Reload provider from database
     * Useful when provider configuration has changed
     *
     * @param ctx context
     * @param providerId AIG_Provider_ID
     * @param trxName transaction name
     * @return reloaded provider instance
     * @throws AIProviderException if reload fails
     */
    public IAIProvider reloadProvider(Properties ctx, int providerId, String trxName)
            throws AIProviderException {
        invalidateProvider(providerId);
        return getProvider(ctx, providerId, trxName);
    }
}
```

---

## Usage Examples

### Example 1: Loading Provider from Database

```java
package com.cloudempiere.ai.example;

import com.cloudempiere.ai.provider.*;
import com.cloudempiere.ai.model.MAIProvider;
import java.util.Properties;

/**
 * Example: Load AI provider from database using AIGProviderType
 */
public class ProviderLoadingExample {

    public void loadAndUseProvider(Properties ctx, String trxName) {
        try {
            // Get provider factory
            AIProviderFactory factory = AIProviderFactory.getInstance();

            // Load provider by ID (AIGProviderType is read from database)
            int providerId = 1000000; // Example ID
            IAIProvider provider = factory.getProvider(ctx, providerId, trxName);

            System.out.println("Loaded provider: " + provider.getProviderName());
            System.out.println("Provider type: " + provider.getProviderType());
            System.out.println("Supports text generation: " + provider.supportsTextGeneration());

            // Use the provider
            AIRequest request = new AIRequest.Builder()
                .model("gpt-3.5-turbo")
                .userMessage("Hello, how are you?")
                .maxTokens(100)
                .build();

            AIResponse response = provider.generateText(request);
            System.out.println("AI Response: " + response.getContent());

        } catch (AIProviderException e) {
            System.err.println("Provider error: " + e.getMessage());
        }
    }
}
```

### Example 2: Getting Provider Directly from MAIProvider

```java
package com.cloudempiere.ai.example;

import com.cloudempiere.ai.provider.*;
import com.cloudempiere.ai.model.MAIProvider;
import org.compiere.model.Query;
import java.util.Properties;

/**
 * Example: Query provider from database and use it
 */
public class ProviderQueryExample {

    public void findAndUseProvider(Properties ctx, String trxName) {
        try {
            // Find active OpenAI provider
            MAIProvider providerConfig = new Query(ctx, MAIProvider.Table_Name,
                "AIGProviderType=? AND IsActive='Y'", trxName)
                .setParameters("OPENAI")
                .setOrderBy("Priority")
                .first();

            if (providerConfig == null) {
                System.err.println("No active OpenAI provider found");
                return;
            }

            // Get provider instance through factory
            AIProviderFactory factory = AIProviderFactory.getInstance();
            IAIProvider provider = factory.getProvider(providerConfig);

            // Check health
            AIHealthStatus health = provider.checkHealth();
            if (!health.isHealthy()) {
                System.err.println("Provider is not healthy: " + health.getMessage());
                return;
            }

            // Use provider
            AIRequest request = new AIRequest.Builder()
                .model("gpt-4")
                .systemPrompt("You are a helpful assistant.")
                .userMessage("Explain quantum computing in simple terms.")
                .temperature(0.7)
                .maxTokens(500)
                .build();

            AIResponse response = provider.generateText(request);

            if (response.isSuccess()) {
                System.out.println("Response: " + response.getContent());
                System.out.println("Tokens used: " + response.getTokenUsage().getTotalTokens());
                System.out.println("Cost: $" + response.getCostUSD());
            } else {
                System.err.println("Error: " + response.getErrorMessage());
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Example 3: Registering Custom Provider

```java
package com.cloudempiere.ai.example;

import com.cloudempiere.ai.provider.*;

/**
 * Example: Register a custom provider implementation
 */
public class CustomProviderRegistrationExample {

    public void registerCustomProvider() {
        AIProviderFactory factory = AIProviderFactory.getInstance();

        // Register custom provider
        // Must match AIGProviderType value in AD_Ref_List
        factory.registerProvider("CUSTOM_LLM",
            com.mycompany.ai.CustomLLMProvider.class);

        System.out.println("Custom provider registered");
        System.out.println("Available providers: " +
            factory.getRegisteredProviderTypes());
    }
}
```

---

## Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1-2)
✅ **Completed:**
- [x] Database schema with AIGProviderType field
- [x] AD_Ref_List entries for provider types
- [x] IAIProvider interface definition
- [x] Supporting DTOs (AIRequest, AIResponse, etc.)
- [x] Exception classes
- [x] AIProviderFactory with dynamic loading

🔨 **In Progress:**
- [ ] Unit tests for factory
- [ ] OpenAIProvider implementation
- [ ] Integration tests

### Phase 2: Core Provider Implementations (Week 3-4)
- [ ] OpenAIProvider (GPT-4, GPT-3.5)
- [ ] AnthropicProvider (Claude)
- [ ] OllamaProvider (local LLM)
- [ ] Provider capability detection
- [ ] Cost calculation per provider

### Phase 3: Additional Providers (Week 5-6)
- [ ] AWSBedrockProvider
- [ ] GoogleAIProvider (Gemini)
- [ ] AzureOpenAIProvider
- [ ] HuggingFaceProvider
- [ ] CohereProvider
- [ ] MistralProvider

### Phase 4: Advanced Features (Week 7-8)
- [ ] Streaming support
- [ ] Function calling support
- [ ] Embeddings support
- [ ] Vision support (image analysis)
- [ ] Audio transcription support
- [ ] Batch processing

### Phase 5: iDempiere Integration (Week 9-10)
- [ ] OSGi service registration
- [ ] Provider configuration window
- [ ] Health check process
- [ ] Usage tracking
- [ ] Cost reporting
- [ ] Provider selection wizard

### Phase 6: Testing & Documentation (Week 11-12)
- [ ] Comprehensive unit tests
- [ ] Integration tests with real providers
- [ ] Performance benchmarking
- [ ] API documentation (Javadoc)
- [ ] User guide
- [ ] Admin guide
- [ ] Migration scripts

---

## Database Migration Script

### Adding AIGProviderType Field

```sql
-- ======================================================================
-- Add AIGProviderType to AIG_Provider table
-- ======================================================================

-- Add column
ALTER TABLE AIG_Provider
ADD COLUMN AIGProviderType VARCHAR(60);

-- Create AD_Reference for provider types
INSERT INTO AD_Reference (
    AD_Reference_ID,
    AD_Client_ID,
    AD_Org_ID,
    IsActive,
    Created,
    CreatedBy,
    Updated,
    UpdatedBy,
    Name,
    Description,
    ValidationType,
    EntityType
)
SELECT
    nextval('ad_reference_seq'),
    0,
    0,
    'Y',
    NOW(),
    100,
    NOW(),
    100,
    'AIG Provider Type',
    'AI Provider Implementation Type',
    'L',
    'U';

-- Get the reference ID
-- Store in variable for use below
DO $$
DECLARE
    v_ref_id INTEGER;
BEGIN
    SELECT AD_Reference_ID INTO v_ref_id
    FROM AD_Reference
    WHERE Name = 'AIG Provider Type'
      AND ValidationType = 'L'
    ORDER BY Created DESC
    LIMIT 1;

    -- Add reference list values
    INSERT INTO AD_Ref_List (
        AD_Ref_List_ID, AD_Reference_ID, Value, Name, Description, EntityType
    ) VALUES
    (nextval('ad_ref_list_seq'), v_ref_id, 'OPENAI', 'OpenAI',
     'OpenAI GPT models (GPT-4, GPT-3.5)', 'U'),
    (nextval('ad_ref_list_seq'), v_ref_id, 'ANTHROPIC', 'Anthropic Claude',
     'Anthropic Claude models (Claude 3)', 'U'),
    (nextval('ad_ref_list_seq'), v_ref_id, 'AWS_BEDROCK', 'AWS Bedrock',
     'Amazon Bedrock multi-model service', 'U'),
    (nextval('ad_ref_list_seq'), v_ref_id, 'GOOGLE_AI', 'Google AI',
     'Google Gemini models', 'U'),
    (nextval('ad_ref_list_seq'), v_ref_id, 'AZURE_OPENAI', 'Azure OpenAI',
     'Microsoft Azure OpenAI Service', 'U'),
    (nextval('ad_ref_list_seq'), v_ref_id, 'OLLAMA', 'Ollama',
     'Local LLM inference with Ollama', 'U'),
    (nextval('ad_ref_list_seq'), v_ref_id, 'HUGGINGFACE', 'HuggingFace',
     'HuggingFace Inference API', 'U'),
    (nextval('ad_ref_list_seq'), v_ref_id, 'COHERE', 'Cohere',
     'Cohere AI models', 'U'),
    (nextval('ad_ref_list_seq'), v_ref_id, 'MISTRAL', 'Mistral AI',
     'Mistral AI models', 'U');

    RAISE NOTICE 'Created AIG Provider Type reference with ID: %', v_ref_id;
END $$;

-- Add column to AD_Column
INSERT INTO AD_Column (
    AD_Column_ID,
    AD_Client_ID,
    AD_Org_ID,
    IsActive,
    Created,
    CreatedBy,
    Updated,
    UpdatedBy,
    Name,
    Description,
    ColumnName,
    AD_Table_ID,
    AD_Reference_ID,
    FieldLength,
    IsMandatory,
    IsUpdateable,
    IsIdentifier,
    IsKey,
    EntityType
)
SELECT
    nextval('ad_column_seq'),
    0,
    0,
    'Y',
    NOW(),
    100,
    NOW(),
    100,
    'AI Provider Type',
    'Type of AI provider implementation to use',
    'AIGProviderType',
    (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'AIG_Provider'),
    (SELECT AD_Reference_ID FROM AD_Reference
     WHERE Name = 'AIG Provider Type' AND ValidationType = 'L'
     ORDER BY Created DESC LIMIT 1),
    60,
    'Y', -- Mandatory
    'Y', -- Updateable
    'N',
    'N',
    'U';

-- Sync column to database
-- This would typically be done through iDempiere 2Pack or manual sync

COMMIT;
```

---

## Next Steps

### Immediate Tasks

1. **✅ Complete Database Schema**
   - Add AIGProviderType field to AIG_Provider table
   - Create AD_Reference and AD_Ref_List entries
   - Sync to Application Dictionary

2. **🔨 Create Core Classes**
   - IAIProvider interface
   - Supporting DTOs (AIRequest, AIResponse, etc.)
   - Exception classes
   - AIProviderFactory

3. **🔨 Implement First Provider**
   - OpenAIProvider as reference implementation
   - Test with real OpenAI API
   - Document implementation patterns

4. **📝 Write Tests**
   - Unit tests for factory
   - Mock provider for testing
   - Integration tests

5. **📚 Update Documentation**
   - Javadoc for all public APIs
   - Implementation guide for new providers
   - User guide for configuration

### Long-term Goals

- Support for 10+ major AI providers
- Automatic provider failover
- Cost optimization through routing
- Provider performance benchmarking
- AI capability marketplace

---

## Summary of Key Changes

### What Changed from Original Plan

1. **AIGProviderType Field**: Provider type is now stored in database (AD_Ref_List) instead of being hardcoded
2. **Dynamic Loading**: Factory uses reflection to instantiate providers based on AIGProviderType value
3. **Extensibility**: New providers can be added by:
   - Creating implementation class
   - Adding AIGProviderType value to AD_Ref_List
   - Registering in factory
4. **No Code Changes Required**: Adding new provider doesn't require modifying factory code
5. **Database-Driven**: Provider types managed through iDempiere Application Dictionary

### Benefits

✅ **More Flexible**: Easy to add new providers without code changes
✅ **iDempiere Native**: Uses standard AD_Ref_List pattern
✅ **User-Friendly**: Provider types visible in dropdown menus
✅ **Maintainable**: Clear separation between registration and usage
✅ **Testable**: Easy to mock and test factory behavior

---

## Conclusion

This implementation plan provides a robust, extensible architecture for AI provider integration in iDempiere using the **AIGProviderType** field for dynamic provider discovery and instantiation. The factory pattern combined with reflection allows for easy addition of new providers while maintaining type safety and clean separation of concerns.

**Ready to implement?** Start with Phase 1 tasks and create the core infrastructure.