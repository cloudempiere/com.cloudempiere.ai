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
    public static final String PROVIDER_MOCK_OPENAI = X_AIG_Provider.AIGPROVIDERTYPE_MockAIHub;
    public static final String PROVIDER_AI_HUB = X_AIG_Provider.AIGPROVIDERTYPE_IDempiereAIHub;
    public static final String PROVIDER_OLLAMA = X_AIG_Provider.AIGPROVIDERTYPE_Ollama;
    public static final String PROVIDER_OPENAI = "OAI";    // To be added to AD_Ref_List
    public static final String PROVIDER_LLAMA = "LLA";     // Meta Llama via Ollama - To be added to AD_Ref_List

    /** Default model names per provider */
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-20250514";
    private static final String DEFAULT_BEDROCK_MODEL = "anthropic.claude-3-5-sonnet-20241022-v2:0";
    private static final String DEFAULT_BEDROCK_REGION = "us-east-1";
    private static final String DEFAULT_MOCK_OPENAI_MODEL = "llama3.2";
    private static final String DEFAULT_MOCK_OPENAI_URL = "http://localhost:8081/v1";  // iDempiere-CLI Chat API (ADR-048)
    private static final String DEFAULT_OLLAMA_MODEL = "llama3.2";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o";
    private static final String DEFAULT_LLAMA_MODEL = "llama3.2";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

    /** Default embedding model names per provider */
    private static final String DEFAULT_BEDROCK_EMBEDDING_MODEL = "amazon.titan-embed-text-v2:0";
    private static final String DEFAULT_MOCK_OPENAI_EMBEDDING_MODEL = "nomic-embed-text";
    private static final String DEFAULT_OLLAMA_EMBEDDING_MODEL = "nomic-embed-text";
    private static final String DEFAULT_OPENAI_EMBEDDING_MODEL = "text-embedding-3-small";

    /** Cache for model instances by provider ID */
    private static final Map<Integer, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();
    private static final Map<Integer, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();
    private static final Map<Integer, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    /** Flag to enable/disable metrics listener (default: enabled) */
    private static boolean metricsEnabled = true;

    /**
     * Create a ChatLanguageModel from MAIProvider configuration.
     *
     * <p>Uses the ModelName from the provider configuration if set,
     * otherwise falls back to provider-specific defaults.
     *
     * @param config MAIProvider database configuration
     * @return ChatLanguageModel instance
     * @throws IllegalArgumentException if provider type is unknown
     */
    public static ChatLanguageModel create(MAIProvider config) {
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
     * @param config MAIProvider database configuration
     * @param modelName Optional model name (uses config.getModelName() or default if null)
     * @param baseUrl Optional base URL for Ollama (uses default if null)
     * @return ChatLanguageModel instance
     */
    public static ChatLanguageModel create(MAIProvider config, String modelName, String baseUrl) {
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
            case PROVIDER_MOCK_OPENAI:
            case PROVIDER_AI_HUB:
                return createMockOpenAiModel(baseUrl, modelName, apiKey);
            case PROVIDER_OLLAMA:
                return createOllamaModel(baseUrl, modelName);
            case PROVIDER_OPENAI:
                return createOpenAiModel(apiKey, modelName);
            case PROVIDER_LLAMA:
                return createLlamaModel(baseUrl, modelName);
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
     * @param config MAIProvider database configuration
     * @return StreamingChatLanguageModel instance
     */
    public static StreamingChatLanguageModel createStreaming(MAIProvider config) {
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
    public static StreamingChatLanguageModel createStreaming(MAIProvider config, String modelName, String baseUrl) {
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
            case PROVIDER_MOCK_OPENAI:
            case PROVIDER_AI_HUB:
                return createMockOpenAiStreamingModel(baseUrl, modelName, apiKey);
            case PROVIDER_OLLAMA:
                return createOllamaStreamingModel(baseUrl, modelName);
            case PROVIDER_OPENAI:
                return createOpenAiStreamingModel(apiKey, modelName);
            case PROVIDER_LLAMA:
                return createLlamaStreamingModel(baseUrl, modelName);
            default:
                throw new IllegalArgumentException("Streaming not supported for provider: " + providerType);
        }
    }

    /**
     * Get or create a cached ChatLanguageModel instance.
     */
    public static ChatLanguageModel getOrCreate(MAIProvider config) {
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
     * @param config MAIProvider database configuration
     * @return EmbeddingModel instance
     * @throws IllegalArgumentException if provider type is unknown
     */
    public static EmbeddingModel createEmbeddingModel(MAIProvider config) {
        return createEmbeddingModel(config, null, null);
    }

    /**
     * Create an EmbeddingModel with optional model name override.
     *
     * @param config MAIProvider database configuration
     * @param modelName Optional model name (uses default if null)
     * @param baseUrl Optional base URL for Ollama (uses default if null)
     * @return EmbeddingModel instance
     */
    public static EmbeddingModel createEmbeddingModel(MAIProvider config, String modelName, String baseUrl) {
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

            case PROVIDER_MOCK_OPENAI:
            case PROVIDER_AI_HUB:
                return createMockOpenAiEmbeddingModel(baseUrl, modelName, apiKey);

            case PROVIDER_OLLAMA:
                return createOllamaEmbeddingModel(baseUrl, modelName);

            case PROVIDER_OPENAI:
                return createOpenAiEmbeddingModel(apiKey, modelName);

            case PROVIDER_LLAMA:
                // Llama uses Ollama for embeddings with nomic-embed-text or similar
                return createLlamaEmbeddingModel(baseUrl, modelName);

            default:
                // Fallback to Bedrock Titan for unknown providers
                log.warning("Unknown provider type: " + providerType + ", falling back to Bedrock Titan Embeddings");
                return createBedrockEmbeddingModel(modelName, apiKey);
        }
    }

    /**
     * Get or create a cached EmbeddingModel instance.
     *
     * @param config MAIProvider configuration
     * @return Cached or newly created EmbeddingModel
     */
    public static EmbeddingModel getOrCreateEmbeddingModel(MAIProvider config) {
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
               PROVIDER_MOCK_OPENAI.equals(providerType) ||
               PROVIDER_AI_HUB.equals(providerType) ||
               PROVIDER_OLLAMA.equals(providerType) ||
               PROVIDER_OPENAI.equals(providerType) ||
               PROVIDER_LLAMA.equals(providerType);
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
     * Create a ChatLanguageModel for MockOpenAI (OpenAI-compatible API).
     *
     * <p>This provider connects to an OpenAI-compatible API endpoint, such as:
     * <ul>
     *   <li>iDempiere-CLI Chat API (ADR-048) - http://localhost:8081/v1</li>
     *   <li>Local mock servers for testing</li>
     *   <li>Alternative OpenAI-compatible LLM APIs</li>
     * </ul>
     *
     * <p>Useful for:
     * <ul>
     *   <li>Testing without API costs</li>
     *   <li>Development environments</li>
     *   <li>Integration testing</li>
     *   <li>Using local LLMs via OpenAI-compatible wrappers</li>
     * </ul>
     *
     * @param baseUrl API base URL (default: http://localhost:8081/v1)
     * @param modelName Model name (default: llama3.2)
     * @param apiKey API key (optional, can be dummy value "test" for local servers)
     * @return ChatLanguageModel configured for MockOpenAI
     */
    private static ChatLanguageModel createMockOpenAiModel(String baseUrl, String modelName, String apiKey) {
        var builder = OpenAiChatModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_MOCK_OPENAI_URL)
            .apiKey(apiKey != null && !apiKey.isEmpty() ? apiKey : "test")  // Dummy API key for local servers
            .modelName(modelName != null ? modelName : DEFAULT_MOCK_OPENAI_MODEL)
            .temperature(0.7)
            .logRequests(true)
            .logResponses(true);

        // Add observability listener (ADR-013)
        if (metricsEnabled) {
            builder.listeners(List.of(createMetricsListener("mock-openai")));
        }

        return builder.build();
    }

    /**
     * Create a StreamingChatLanguageModel for MockOpenAI (OpenAI-compatible API).
     *
     * @param baseUrl API base URL (default: http://localhost:8081/v1)
     * @param modelName Model name (default: llama3.2)
     * @param apiKey API key (optional, can be dummy value "test" for local servers)
     * @return StreamingChatLanguageModel configured for MockOpenAI
     */
    private static StreamingChatLanguageModel createMockOpenAiStreamingModel(String baseUrl, String modelName, String apiKey) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_MOCK_OPENAI_URL)
            .apiKey(apiKey != null && !apiKey.isEmpty() ? apiKey : "test")  // Dummy API key for local servers
            .modelName(modelName != null ? modelName : DEFAULT_MOCK_OPENAI_MODEL)
            .temperature(0.7)
            .build();
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
     * Create an EmbeddingModel for MockOpenAI (OpenAI-compatible API).
     *
     * <p>Uses OpenAI's embedding model interface but connects to a local
     * or mock endpoint (e.g., iDempiere-CLI Chat API).
     *
     * @param baseUrl API base URL (default: http://localhost:8081/v1)
     * @param modelName Embedding model name (default: nomic-embed-text)
     * @param apiKey API key (optional, can be dummy value "test" for local servers)
     * @return EmbeddingModel configured for MockOpenAI embeddings
     */
    private static EmbeddingModel createMockOpenAiEmbeddingModel(String baseUrl, String modelName, String apiKey) {
        return OpenAiEmbeddingModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_MOCK_OPENAI_URL)
            .apiKey(apiKey != null && !apiKey.isEmpty() ? apiKey : "test")  // Dummy API key for local servers
            .modelName(modelName != null ? modelName : DEFAULT_MOCK_OPENAI_EMBEDDING_MODEL)
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
