package com.cloudempiere.ai.provider.langchain4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.model.X_AIG_Provider;
import com.cloudempiere.ai.observability.AIMetricsListener;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import software.amazon.awssdk.regions.Region;

/**
 * Factory for creating LangChain4j ChatLanguageModel instances from iDempiere configuration.
 *
 * Replaces the custom IAIProvider implementations with LangChain4j native providers:
 * - AnthropicChatModel (replaces AnthropicProvider)
 * - BedrockAnthropicChatModel (replaces AWSBedrockProvider)
 * - OllamaChatModel (local LLM support for any Ollama model)
 * - OpenAiChatModel (OpenAI/Azure support)
 * - LlamaModel (Meta Llama models via Ollama - llama3.2, llama3.1, codellama, etc.)
 *
 * @author Cloudempiere
 * @version 0.10.0
 * @since ADR-002 LangChain4j Strategic Adoption
 */
public class LangChain4jProviderFactory {

    private static final CLogger log = CLogger.getCLogger(LangChain4jProviderFactory.class);

    /** Provider type constants matching AD_Ref_List values */
    public static final String PROVIDER_ANTHROPIC = X_AIG_Provider.AIGPROVIDERTYPE_AnthropicClaude;
    public static final String PROVIDER_BEDROCK = X_AIG_Provider.AIGPROVIDERTYPE_AWSBedrock;
    public static final String PROVIDER_MOCK_AI_HUB = X_AIG_Provider.AIGPROVIDERTYPE_MockAIHub;
    public static final String PROVIDER_OLLAMA = X_AIG_Provider.AIGPROVIDERTYPE_Ollama;
    public static final String PROVIDER_OPENAI = "OAI";    // To be added to AD_Ref_List
    public static final String PROVIDER_LLAMA = "LLA";     // Meta Llama via Ollama - To be added to AD_Ref_List
    /** AI Hub provider type - routes requests to iDempiere AI Hub Service (ADR-042) */
    public static final String PROVIDER_AI_HUB = X_AIG_Provider.AIGPROVIDERTYPE_IDempiereAIHub;

    /** Default model names per provider */
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-20250514";
    private static final String DEFAULT_BEDROCK_MODEL = "anthropic.claude-3-5-sonnet-20241022-v2:0";
    private static final String DEFAULT_BEDROCK_REGION = "us-east-1";
    private static final String DEFAULT_MOCK_AI_HUB_MODEL = "llama3.2";
    private static final String DEFAULT_MOCK_AI_HUB_URL = "http://localhost:8090/v1";  // Mock AI Hub for testing
    private static final String DEFAULT_OLLAMA_MODEL = "llama3.2";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o";
    private static final String DEFAULT_LLAMA_MODEL = "llama3.2";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

    /** Default embedding model names per provider */
    private static final String DEFAULT_BEDROCK_EMBEDDING_MODEL = "amazon.titan-embed-text-v2:0";
    private static final String DEFAULT_MOCK_AI_HUB_EMBEDDING_MODEL = "nomic-embed-text";
    private static final String DEFAULT_OLLAMA_EMBEDDING_MODEL = "nomic-embed-text";
    private static final String DEFAULT_OPENAI_EMBEDDING_MODEL = "text-embedding-3-small";

    /** Default AI Hub configuration */
    private static final String DEFAULT_AI_HUB_URL = "http://localhost:8090";
    private static final String DEFAULT_AI_HUB_MODEL = "claude-sonnet-4";
    private static final int DEFAULT_AI_HUB_TIMEOUT_SECONDS = 120;

    /** Cache for model instances by provider ID */
    private static final Map<Integer, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();
    private static final Map<Integer, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();
    private static final Map<Integer, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    /** Flag to enable/disable metrics listener (default: enabled) */
    private static boolean metricsEnabled = true;

    /**
     * Create a ChatLanguageModel from provider configuration.
     *
     * <p>Uses the ModelName from the provider configuration if set,
     * otherwise falls back to provider-specific defaults.
     *
     * @param config Provider configuration (MAIProvider or IAIProviderConfig implementation)
     * @return ChatLanguageModel instance
     * @throws IllegalArgumentException if provider type is unknown
     */
    public static ChatLanguageModel create(IAIProviderConfig config) {
        // Use ModelName from config if set, otherwise null (will use defaults)
        String modelName = config.getModelName();
        if (modelName != null && modelName.trim().isEmpty()) {
            modelName = null;  // Treat empty string as null
        }
        return create(config, modelName, null);
    }

    /**
     * Create a ChatLanguageModel with optional model name override.
     *
     * @param config Provider configuration (MAIProvider or IAIProviderConfig implementation)
     * @param modelName Optional model name (uses config.getModelName() or default if null)
     * @param baseUrl Optional base URL for Ollama (uses default if null)
     * @return ChatLanguageModel instance
     */
    public static ChatLanguageModel create(IAIProviderConfig config, String modelName, String baseUrl) {
        String providerType = config.getAIGProviderType();
        String apiKey = config.getAPIKey();

        // If modelName not provided as parameter, try to get from config
        if (modelName == null) {
            modelName = config.getModelName();
            if (modelName != null && modelName.trim().isEmpty()) {
                modelName = null;
            }
        }

        log.info("Creating LangChain4j model for provider: " + providerType +
                (modelName != null ? ", model: " + modelName : " (using default model)"));

        switch (providerType) {
            case PROVIDER_ANTHROPIC:
                return createAnthropicModel(apiKey, modelName);
            case PROVIDER_BEDROCK:
                return createBedrockModel(modelName, apiKey);
            case PROVIDER_MOCK_AI_HUB:
                return createMockAIHubModel(baseUrl, modelName, apiKey);
            case PROVIDER_OLLAMA:
                return createOllamaModel(baseUrl, modelName);
            case PROVIDER_OPENAI:
                return createOpenAiModel(apiKey, modelName);
            case PROVIDER_LLAMA:
                return createLlamaModel(baseUrl, modelName);
            case PROVIDER_AI_HUB:
                return createAIHubModel(config);
            default:
                throw new IllegalArgumentException("Unknown provider type: " + providerType);
        }
    }

    /**
     * Create a StreamingChatLanguageModel from MAIProvider configuration.
     *
     * <p>Uses the ModelName from the provider configuration if set,
     * otherwise falls back to provider-specific defaults.
     *
     * @param config Provider configuration (MAIProvider or IAIProviderConfig implementation)
     * @return StreamingChatLanguageModel instance
     */
    public static StreamingChatLanguageModel createStreaming(IAIProviderConfig config) {
        // Use ModelName from config if set, otherwise null (will use defaults)
        String modelName = config.getModelName();
        if (modelName != null && modelName.trim().isEmpty()) {
            modelName = null;  // Treat empty string as null
        }
        return createStreaming(config, modelName, null);
    }

    /**
     * Create a StreamingChatLanguageModel with optional model name override.
     */
    public static StreamingChatLanguageModel createStreaming(IAIProviderConfig config, String modelName, String baseUrl) {
        String providerType = config.getAIGProviderType();
        String apiKey = config.getAPIKey();

        // If modelName not provided as parameter, try to get from config
        if (modelName == null) {
            modelName = config.getModelName();
            if (modelName != null && modelName.trim().isEmpty()) {
                modelName = null;
            }
        }

        log.info("Creating LangChain4j streaming model for provider: " + providerType +
                (modelName != null ? ", model: " + modelName : " (using default model)"));

        switch (providerType) {
            case PROVIDER_ANTHROPIC:
                return createAnthropicStreamingModel(apiKey, modelName);
            case PROVIDER_BEDROCK:
                return createBedrockStreamingModel(modelName, apiKey);
            case PROVIDER_MOCK_AI_HUB:
                return createMockAIHubStreamingModel(baseUrl, modelName, apiKey);
            case PROVIDER_OLLAMA:
                return createOllamaStreamingModel(baseUrl, modelName);
            case PROVIDER_OPENAI:
                return createOpenAiStreamingModel(apiKey, modelName);
            case PROVIDER_LLAMA:
                return createLlamaStreamingModel(baseUrl, modelName);
            case PROVIDER_AI_HUB:
                return createAIHubStreamingModel(config);
            default:
                throw new IllegalArgumentException("Streaming not supported for provider: " + providerType);
        }
    }

    /**
     * Get or create a cached ChatLanguageModel instance.
     */
    public static ChatLanguageModel getOrCreate(IAIProviderConfig config) {
        return modelCache.computeIfAbsent(config.getAIG_Provider_ID(),
            id -> create(config));
    }

    /**
     * Clear model cache (e.g., when provider configuration changes).
     */
    public static void clearCache() {
        modelCache.clear();
        streamingModelCache.clear();
        log.info("LangChain4j model cache cleared");
    }

    /**
     * Remove a specific provider from cache.
     */
    public static void evictFromCache(int providerId) {
        modelCache.remove(providerId);
        streamingModelCache.remove(providerId);
        embeddingModelCache.remove(providerId);
    }

    // ========================================================================
    // Embedding Model Factory Methods (ADR-012 RAG Support)
    // ========================================================================

    /**
     * Create an EmbeddingModel from MAIProvider configuration.
     *
     * <p>For RAG (Retrieval-Augmented Generation), we need embedding models
     * to convert text to vectors for semantic search.
     *
     * <p>Provider mapping:
     * <ul>
     *   <li>Anthropic → AWS Bedrock Titan Embeddings (Anthropic doesn't have embeddings)</li>
     *   <li>AWS Bedrock → Amazon Titan Embed Text v2</li>
     *   <li>Ollama → nomic-embed-text (local)</li>
     *   <li>OpenAI → text-embedding-3-small</li>
     * </ul>
     *
     * @param config Provider configuration (MAIProvider or IAIProviderConfig implementation)
     * @return EmbeddingModel instance
     * @throws IllegalArgumentException if provider type is unknown
     */
    public static EmbeddingModel createEmbeddingModel(IAIProviderConfig config) {
        return createEmbeddingModel(config, null, null);
    }

    /**
     * Create an EmbeddingModel with optional model name override.
     *
     * @param config Provider configuration (MAIProvider or IAIProviderConfig implementation)
     * @param modelName Optional model name (uses default if null)
     * @param baseUrl Optional base URL for Ollama (uses default if null)
     * @return EmbeddingModel instance
     */
    public static EmbeddingModel createEmbeddingModel(IAIProviderConfig config, String modelName, String baseUrl) {
        String providerType = config.getAIGProviderType();
        String apiKey = config.getAPIKey();

        log.info("Creating LangChain4j embedding model for provider: " + providerType);

        switch (providerType) {
            case PROVIDER_ANTHROPIC:
                // Anthropic doesn't have embedding models - use AWS Bedrock Titan
                log.info("Anthropic provider: using AWS Bedrock Titan Embeddings");
                return createBedrockEmbeddingModel(modelName, apiKey);

            case PROVIDER_BEDROCK:
                return createBedrockEmbeddingModel(modelName, apiKey);

            case PROVIDER_MOCK_AI_HUB:
                return createMockAIHubEmbeddingModel(baseUrl, modelName, apiKey);

            case PROVIDER_OLLAMA:
                return createOllamaEmbeddingModel(baseUrl, modelName);

            case PROVIDER_OPENAI:
                return createOpenAiEmbeddingModel(apiKey, modelName);

            case PROVIDER_LLAMA:
                // Llama uses Ollama for embeddings with nomic-embed-text or similar
                return createLlamaEmbeddingModel(baseUrl, modelName);

            case PROVIDER_AI_HUB:
                return createAIHubEmbeddingModel(config);

            default:
                // Fallback to Bedrock Titan for unknown providers
                log.warning("Unknown provider type: " + providerType + ", falling back to Bedrock Titan Embeddings");
                return createBedrockEmbeddingModel(modelName, apiKey);
        }
    }

    /**
     * Get or create a cached EmbeddingModel instance.
     *
     * @param config Provider configuration (MAIProvider or IAIProviderConfig implementation)
     * @return Cached or newly created EmbeddingModel
     */
    public static EmbeddingModel getOrCreateEmbeddingModel(IAIProviderConfig config) {
        return embeddingModelCache.computeIfAbsent(config.getAIG_Provider_ID(),
            id -> createEmbeddingModel(config));
    }

    /**
     * Check if the provider supports embeddings natively.
     *
     * @param providerType Provider type code
     * @return true if provider has native embedding support
     */
    public static boolean hasNativeEmbeddings(String providerType) {
        // Only these providers have native embedding APIs
        return PROVIDER_BEDROCK.equals(providerType) ||
               PROVIDER_MOCK_AI_HUB.equals(providerType) ||
               PROVIDER_OLLAMA.equals(providerType) ||
               PROVIDER_OPENAI.equals(providerType) ||
               PROVIDER_LLAMA.equals(providerType) ||
               PROVIDER_AI_HUB.equals(providerType);
    }

    // ========================================================================
    // Private factory methods
    // ========================================================================

    private static ChatLanguageModel createAnthropicModel(String apiKey, String modelName) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Anthropic API key is not configured. Please set the API Key in the AI Provider configuration.");
        }
        var builder = AnthropicChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_ANTHROPIC_MODEL)
            .maxTokens(4096)
            .temperature(0.7)
            .logRequests(true)
            .logResponses(true);

        // Add observability listener (ADR-013)
        if (metricsEnabled) {
            builder.listeners(List.of(createMetricsListener("anthropic")));
        }

        return builder.build();
    }

    private static StreamingChatLanguageModel createAnthropicStreamingModel(String apiKey, String modelName) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Anthropic API key is not configured. Please set the API Key in the AI Provider configuration.");
        }
        return AnthropicStreamingChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_ANTHROPIC_MODEL)
            .maxTokens(4096)
            .temperature(0.7)
            .build();
    }

    private static ChatLanguageModel createOllamaModel(String baseUrl, String modelName) {
        var builder = OllamaChatModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_OLLAMA_URL)
            .modelName(modelName != null ? modelName : DEFAULT_OLLAMA_MODEL)
            .temperature(0.7);

        // Add observability listener (ADR-013)
        if (metricsEnabled) {
            builder.listeners(List.of(createMetricsListener("ollama")));
        }

        return builder.build();
    }

    private static StreamingChatLanguageModel createOllamaStreamingModel(String baseUrl, String modelName) {
        return OllamaStreamingChatModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_OLLAMA_URL)
            .modelName(modelName != null ? modelName : DEFAULT_OLLAMA_MODEL)
            .temperature(0.7)
            .build();
    }

    /**
     * Create a ChatLanguageModel for Meta Llama models via Ollama.
     *
     * <p>Llama models run locally through Ollama server. Supported models include:
     * <ul>
     *   <li>llama3.2 (default) - Latest Llama 3.2 with 128K context</li>
     *   <li>llama3.1 - Llama 3.1 family (8B, 70B, 405B)</li>
     *   <li>llama3 - Llama 3 family</li>
     *   <li>llama2 - Llama 2 family</li>
     *   <li>codellama - Code-specialized Llama</li>
     * </ul>
     *
     * @param baseUrl Ollama server URL (default: http://localhost:11434)
     * @param modelName Llama model name (default: llama3.2)
     * @return ChatLanguageModel configured for Llama
     */
    private static ChatLanguageModel createLlamaModel(String baseUrl, String modelName) {
        var builder = OllamaChatModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_OLLAMA_URL)
            .modelName(modelName != null ? modelName : DEFAULT_LLAMA_MODEL)
            .temperature(0.7);

        // Add observability listener (ADR-013)
        if (metricsEnabled) {
            builder.listeners(List.of(createMetricsListener("llama")));
        }

        return builder.build();
    }

    /**
     * Create a StreamingChatLanguageModel for Meta Llama models via Ollama.
     *
     * @param baseUrl Ollama server URL (default: http://localhost:11434)
     * @param modelName Llama model name (default: llama3.2)
     * @return StreamingChatLanguageModel configured for Llama
     */
    private static StreamingChatLanguageModel createLlamaStreamingModel(String baseUrl, String modelName) {
        return OllamaStreamingChatModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_OLLAMA_URL)
            .modelName(modelName != null ? modelName : DEFAULT_LLAMA_MODEL)
            .temperature(0.7)
            .build();
    }

    /**
     * Create a ChatLanguageModel for Mock AI Hub (testing/development).
     *
     * <p><b>In-Plugin Mock:</b> No external HTTP server required, runs directly
     * in the OSGi plugin for zero-configuration testing.
     *
     * <p>Useful for:
     * <ul>
     *   <li>Unit and integration testing</li>
     *   <li>Development without AI Hub infrastructure</li>
     *   <li>CI/CD pipelines</li>
     *   <li>Quick prototyping and demos</li>
     * </ul>
     *
     * <p><b>Note:</b> Returns a simple non-streaming model. Use createMockAIHubStreamingModel
     * for streaming support.
     *
     * @param baseUrl Ignored (kept for API compatibility)
     * @param modelName Model name (default: mock-ai-hub)
     * @param apiKey Ignored (kept for API compatibility)
     * @return ChatLanguageModel configured for in-plugin mock
     */
    private static ChatLanguageModel createMockAIHubModel(String baseUrl, String modelName, String apiKey) {
        log.info("Creating in-plugin Mock AI Hub (no HTTP server required)");

        // For non-streaming, wrap the streaming model
        StreamingChatLanguageModel streamingModel = new MockAIHubChatModel(
            modelName != null ? modelName : DEFAULT_MOCK_AI_HUB_MODEL,
            true  // verbose logging
        );

        // Convert to non-streaming by blocking on completion
        return new ChatLanguageModel() {
            @Override
            public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
                    List<dev.langchain4j.data.message.ChatMessage> messages) {
                java.util.concurrent.CompletableFuture<dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>> future =
                    new java.util.concurrent.CompletableFuture<>();

                streamingModel.generate(messages, new dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        // Collect tokens (not needed for non-streaming)
                    }

                    @Override
                    public void onComplete(dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response) {
                        future.complete(response);
                    }

                    @Override
                    public void onError(Throwable error) {
                        future.completeExceptionally(error);
                    }
                });

                try {
                    return future.get();
                } catch (Exception e) {
                    throw new RuntimeException("Mock AI Hub generation failed", e);
                }
            }
        };
    }

    /**
     * Create a StreamingChatLanguageModel for Mock AI Hub.
     *
     * <p><b>In-Plugin Mock:</b> No external HTTP server required, runs directly
     * in the OSGi plugin. Provides streaming responses word-by-word with contextual
     * mock data.
     *
     * <p><b>Benefits:</b>
     * <ul>
     *   <li>✅ Zero configuration - works immediately</li>
     *   <li>✅ No external dependencies</li>
     *   <li>✅ No port conflicts</li>
     *   <li>✅ Fast in-memory processing</li>
     *   <li>✅ Can be used as production fallback</li>
     * </ul>
     *
     * @param baseUrl Ignored (kept for API compatibility)
     * @param modelName Model name (default: mock-ai-hub)
     * @param apiKey Ignored (kept for API compatibility)
     * @return StreamingChatLanguageModel configured for in-plugin mock
     */
    private static StreamingChatLanguageModel createMockAIHubStreamingModel(String baseUrl, String modelName, String apiKey) {
        log.info("Creating in-plugin Mock AI Hub (no HTTP server required)");
        return new MockAIHubChatModel(
            modelName != null ? modelName : DEFAULT_MOCK_AI_HUB_MODEL,
            true  // verbose logging
        );
    }

    private static ChatLanguageModel createOpenAiModel(String apiKey, String modelName) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key is not configured. Please set the API Key in the AI Provider configuration.");
        }
        var builder = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_OPENAI_MODEL)
            .temperature(0.7)
            .logRequests(true)
            .logResponses(true);

        // Add observability listener (ADR-013)
        if (metricsEnabled) {
            builder.listeners(List.of(createMetricsListener("openai")));
        }

        return builder.build();
    }

    private static StreamingChatLanguageModel createOpenAiStreamingModel(String apiKey, String modelName) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key is not configured. Please set the API Key in the AI Provider configuration.");
        }
        return OpenAiStreamingChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_OPENAI_MODEL)
            .temperature(0.7)
            .build();
    }

    /**
     * Create a Bedrock ChatLanguageModel.
     *
     * @param modelName Model ID (e.g., "anthropic.claude-3-5-sonnet-20241022-v2:0")
     * @param apiKey API key in format "accessKeyId:secretAccessKey:region" or null for default credentials
     * @return ChatLanguageModel configured for Bedrock
     */
    private static ChatLanguageModel createBedrockModel(String modelName, String apiKey) {
        // Parse credentials from apiKey if provided (format: accessKeyId:secretAccessKey:region)
        String accessKeyId = null;
        String secretAccessKey = null;
        String region = DEFAULT_BEDROCK_REGION;

        if (apiKey != null && !apiKey.isEmpty()) {
            String[] parts = apiKey.split(":");
            if (parts.length >= 2) {
                accessKeyId = parts[0];
                secretAccessKey = parts[1];
                if (parts.length >= 3 && !parts[2].isEmpty()) {
                    region = parts[2];
                }
                log.info("Bedrock: Using explicit credentials from AIG_Provider, region: " + region);
            } else {
                log.warning("Bedrock: Invalid API key format. Expected 'accessKeyId:secretAccessKey:region'. Using default credentials.");
            }
        } else {
            log.info("Bedrock: No API key configured, using default AWS credentials provider");
        }

        // Use custom wrapper that explicitly creates HTTP client (OSGi workaround)
        var builder = BedrockChatModelWrapper.builder()
            .region(Region.of(region))
            .model(modelName != null ? modelName : DEFAULT_BEDROCK_MODEL)
            .maxTokens(4096)
            .temperature(0.7f);

        if (accessKeyId != null && secretAccessKey != null) {
            builder.credentials(accessKeyId, secretAccessKey);
        }

        return builder.build();
    }

    /**
     * Create a Bedrock StreamingChatLanguageModel.
     *
     * @param modelName Model ID (e.g., "anthropic.claude-3-5-sonnet-20241022-v2:0")
     * @param apiKey API key in format "accessKeyId:secretAccessKey:region" or null for default credentials
     * @return StreamingChatLanguageModel configured for Bedrock
     */
    private static StreamingChatLanguageModel createBedrockStreamingModel(String modelName, String apiKey) {
        // Parse credentials from apiKey if provided (format: accessKeyId:secretAccessKey:region)
        String accessKeyId = null;
        String secretAccessKey = null;
        String region = DEFAULT_BEDROCK_REGION;

        if (apiKey != null && !apiKey.isEmpty()) {
            String[] parts = apiKey.split(":");
            if (parts.length >= 2) {
                accessKeyId = parts[0];
                secretAccessKey = parts[1];
                if (parts.length >= 3 && !parts[2].isEmpty()) {
                    region = parts[2];
                }
                log.info("Bedrock streaming: Using explicit credentials from AIG_Provider, region: " + region);
            } else {
                log.warning("Bedrock streaming: Invalid API key format. Expected 'accessKeyId:secretAccessKey:region'. Using default credentials.");
            }
        } else {
            log.info("Bedrock streaming: No API key configured, using default AWS credentials provider");
        }

        // Use custom wrapper that explicitly creates HTTP client (OSGi workaround)
        var builder = BedrockStreamingChatModelWrapper.builder()
            .region(Region.of(region))
            .model(modelName != null ? modelName : DEFAULT_BEDROCK_MODEL)
            .maxTokens(4096)
            .temperature(0.7f);

        if (accessKeyId != null && secretAccessKey != null) {
            builder.credentials(accessKeyId, secretAccessKey);
        }

        return builder.build();
    }

    // ========================================================================
    // Private embedding model factory methods
    // ========================================================================

    /**
     * Create a Bedrock EmbeddingModel.
     *
     * @param modelName Model ID (e.g., "amazon.titan-embed-text-v2:0")
     * @param apiKey API key in format "accessKeyId:secretAccessKey:region" or null for default credentials
     * @return EmbeddingModel configured for Bedrock Titan
     */
    private static EmbeddingModel createBedrockEmbeddingModel(String modelName, String apiKey) {
        // Parse credentials from apiKey if provided (format: accessKeyId:secretAccessKey:region)
        String accessKeyId = null;
        String secretAccessKey = null;
        String region = DEFAULT_BEDROCK_REGION;

        if (apiKey != null && !apiKey.isEmpty()) {
            String[] parts = apiKey.split(":");
            if (parts.length >= 2) {
                accessKeyId = parts[0];
                secretAccessKey = parts[1];
                if (parts.length >= 3 && !parts[2].isEmpty()) {
                    region = parts[2];
                }
                log.info("Bedrock embedding: Using explicit credentials from AIG_Provider, region: " + region);
            } else {
                log.warning("Bedrock embedding: Invalid API key format. Expected 'accessKeyId:secretAccessKey:region'. Using default credentials.");
            }
        } else {
            log.info("Bedrock embedding: No API key configured, using default AWS credentials provider");
        }

        // Use custom wrapper that explicitly creates HTTP client (OSGi workaround)
        var builder = BedrockEmbeddingModelWrapper.builder()
            .region(Region.of(region))
            .model(modelName != null ? modelName : DEFAULT_BEDROCK_EMBEDDING_MODEL);

        if (accessKeyId != null && secretAccessKey != null) {
            builder.credentials(accessKeyId, secretAccessKey);
        }

        return builder.build();
    }

    private static EmbeddingModel createOllamaEmbeddingModel(String baseUrl, String modelName) {
        return OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_OLLAMA_URL)
            .modelName(modelName != null ? modelName : DEFAULT_OLLAMA_EMBEDDING_MODEL)
            .build();
    }

    /**
     * Create an EmbeddingModel for Mock AI Hub.
     *
     * <p>Uses OpenAI's embedding model interface but connects to a local
     * or mock endpoint (port 8081) for testing without real AI Hub.
     *
     * @param baseUrl API base URL (default: http://localhost:8081/v1)
     * @param modelName Embedding model name (default: nomic-embed-text)
     * @param apiKey API key (optional, uses "test" dummy value)
     * @return EmbeddingModel configured for Mock AI Hub embeddings
     */
    private static EmbeddingModel createMockAIHubEmbeddingModel(String baseUrl, String modelName, String apiKey) {
        return OpenAiEmbeddingModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_MOCK_AI_HUB_URL)
            .apiKey(apiKey != null && !apiKey.isEmpty() ? apiKey : "test")  // Dummy key for mock
            .modelName(modelName != null ? modelName : DEFAULT_MOCK_AI_HUB_EMBEDDING_MODEL)
            .build();
    }

    /**
     * Create an EmbeddingModel for Llama via Ollama.
     *
     * <p>Uses Ollama's embedding models. Recommended models:
     * <ul>
     *   <li>nomic-embed-text (default) - Good general-purpose embeddings</li>
     *   <li>mxbai-embed-large - High-quality embeddings</li>
     *   <li>all-minilm - Fast, lightweight embeddings</li>
     * </ul>
     *
     * @param baseUrl Ollama server URL (default: http://localhost:11434)
     * @param modelName Embedding model name (default: nomic-embed-text)
     * @return EmbeddingModel configured for Llama/Ollama embeddings
     */
    private static EmbeddingModel createLlamaEmbeddingModel(String baseUrl, String modelName) {
        return OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_OLLAMA_URL)
            .modelName(modelName != null ? modelName : DEFAULT_OLLAMA_EMBEDDING_MODEL)
            .build();
    }

    private static EmbeddingModel createOpenAiEmbeddingModel(String apiKey, String modelName) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key is not configured. Please set the API Key in the AI Provider configuration.");
        }
        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_OPENAI_EMBEDDING_MODEL)
            .build();
    }

    // ========================================================================
    // AI Hub Provider Methods (ADR-042)
    // ========================================================================

    /**
     * Create a ChatLanguageModel that routes through iDempiere AI Hub.
     *
     * <p>The AI Hub service exposes an OpenAI-compatible REST API, so we use
     * OpenAiChatModel with custom baseUrl pointing to the AI Hub.
     *
     * <p>Benefits of AI Hub routing:
     * <ul>
     *   <li>Access to LangChain4j 1.x features (MCP, extended thinking)</li>
     *   <li>Centralized AI processing and cost management</li>
     *   <li>Advanced observability and caching</li>
     *   <li>Multi-tenant support with context isolation</li>
     * </ul>
     *
     * @param config Provider configuration with AI Hub endpoint and auth token
     * @return ChatLanguageModel proxying through AI Hub
     */
    private static ChatLanguageModel createAIHubModel(IAIProviderConfig config) {
        String endpoint = config.getEndpoint();
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = DEFAULT_AI_HUB_URL;
            log.info("AI Hub endpoint not configured, using default: " + endpoint);
        }

        String apiKey = config.getAPIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "mock-token";  // Allow mock mode for development
            log.warning("AI Hub API key not configured, using mock token for development");
        }

        // Normalize endpoint URL
        if (!endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }
        String baseUrl = endpoint + "v1";

        String modelName = config.getModelName();
        if (modelName == null || modelName.isEmpty()) {
            modelName = DEFAULT_AI_HUB_MODEL;
        }

        log.warning("Creating AI Hub proxy model: endpoint=" + endpoint + ", model=" + modelName);
        log.warning("AI Hub baseUrl: " + baseUrl);

        // Use OpenAI-compatible client (AI Hub exposes /v1/chat/completions)
        var builder = OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(modelName)
            .temperature(0.7)
            .logRequests(true)
            .logResponses(true);

        // Add observability listener
        if (metricsEnabled) {
            builder.listeners(List.of(createMetricsListener("ai-hub")));
        }

        return builder.build();
    }

    /**
     * Create a StreamingChatLanguageModel through AI Hub.
     *
     * @param config Provider configuration with AI Hub endpoint and auth token
     * @return StreamingChatLanguageModel proxying through AI Hub
     */
    private static StreamingChatLanguageModel createAIHubStreamingModel(IAIProviderConfig config) {
        String endpoint = config.getEndpoint();
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = DEFAULT_AI_HUB_URL;
            log.info("AI Hub streaming endpoint not configured, using default: " + endpoint);
        }

        String apiKey = config.getAPIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "mock-token";
            log.warning("AI Hub streaming API key not configured, using mock token for development");
        }

        if (!endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }
        String baseUrl = endpoint + "v1";

        String modelName = config.getModelName();
        if (modelName == null || modelName.isEmpty()) {
            modelName = DEFAULT_AI_HUB_MODEL;
        }

        log.warning("Creating AI Hub streaming model: endpoint=" + endpoint + ", model=" + modelName);
        log.warning("AI Hub streaming baseUrl: " + baseUrl);

        return OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(modelName)
            .temperature(0.7)
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    /**
     * Create an EmbeddingModel through AI Hub.
     *
     * @param config Provider configuration with AI Hub endpoint and auth token
     * @return EmbeddingModel proxying through AI Hub
     */
    private static EmbeddingModel createAIHubEmbeddingModel(IAIProviderConfig config) {
        String endpoint = config.getEndpoint();
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = DEFAULT_AI_HUB_URL;
            log.info("AI Hub embedding endpoint not configured, using default: " + endpoint);
        }

        String apiKey = config.getAPIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "mock-token";
            log.warning("AI Hub embedding API key not configured, using mock token for development");
        }

        if (!endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }
        String baseUrl = endpoint + "v1";

        log.info("Creating AI Hub embedding model: endpoint=" + endpoint);

        return OpenAiEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName("text-embedding-3-small")  // AI Hub routes appropriately
            .build();
    }

    /**
     * Check if AI Hub service is available.
     *
     * @param config Provider configuration with AI Hub endpoint
     * @return true if AI Hub responds to health check
     */
    public static boolean isAIHubHealthy(IAIProviderConfig config) {
        if (!PROVIDER_AI_HUB.equals(config.getAIGProviderType())) {
            return true;  // Not an AI Hub provider
        }

        String endpoint = config.getEndpoint();
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = DEFAULT_AI_HUB_URL;
        }

        try {
            java.net.URL healthUrl = new java.net.URL(endpoint + "/health");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) healthUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            return status == 200;
        } catch (Exception e) {
            log.warning("AI Hub health check failed: " + e.getMessage());
            return false;
        }
    }

    // ========================================================================
    // Observability (ADR-013)
    // ========================================================================

    /**
     * Create a metrics listener for the given agent name.
     *
     * <p>The listener captures token usage, latency, and cost metrics
     * and persists them to the AIG_UsageMetrics table.
     *
     * @param agentName Agent/provider name for tracking
     * @return AIMetricsListener instance
     */
    private static AIMetricsListener createMetricsListener(String agentName) {
        // Generate a session ID based on current context
        String sessionId = "provider-" + System.currentTimeMillis();
        return new AIMetricsListener(agentName, sessionId);
    }

    /**
     * Enable or disable metrics collection.
     *
     * <p>When disabled, ChatLanguageModel instances are created without
     * the AIMetricsListener, which can be useful for testing or
     * high-throughput scenarios where metrics overhead is a concern.
     *
     * @param enabled true to enable metrics, false to disable
     */
    public static void setMetricsEnabled(boolean enabled) {
        metricsEnabled = enabled;
        log.info("Metrics collection " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Check if metrics collection is enabled.
     *
     * @return true if metrics are enabled
     */
    public static boolean isMetricsEnabled() {
        return metricsEnabled;
    }
}
