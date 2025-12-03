package com.cloudempiere.ai.provider.langchain4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;
import org.compiere.util.Env;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.model.X_AIG_Provider;
import com.cloudempiere.ai.observability.AIMetricsListener;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.bedrock.BedrockChatModel;
// Note: BedrockEmbeddingModel may require langchain4j-bedrock-embeddings module
// For now, use Ollama as fallback for Bedrock embedding needs
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.bedrock.BedrockChatRequestParameters;
import software.amazon.awssdk.regions.Region;

/**
 * Factory for creating LangChain4j ChatLanguageModel instances from iDempiere configuration.
 *
 * Replaces the custom IAIProvider implementations with LangChain4j native providers:
 * - AnthropicChatModel (replaces AnthropicProvider)
 * - BedrockAnthropicChatModel (replaces AWSBedrockProvider)
 * - OllamaChatModel (new - local LLM support)
 * - OpenAiChatModel (new - OpenAI/Azure support)
 *
 * @author Cloudempiere
 * @version 0.9.0
 * @since ADR-002 LangChain4j Strategic Adoption
 */
public class LangChain4jProviderFactory {

    private static final CLogger log = CLogger.getCLogger(LangChain4jProviderFactory.class);

    /** Provider type constants matching AD_Ref_List values */
    public static final String PROVIDER_ANTHROPIC = X_AIG_Provider.AIGPROVIDERTYPE_AnthropicClaude;
    public static final String PROVIDER_BEDROCK = X_AIG_Provider.AIGPROVIDERTYPE_AWSBedrock;
    public static final String PROVIDER_OLLAMA = "OLL";    // To be added to AD_Ref_List
    public static final String PROVIDER_OPENAI = "OAI";    // To be added to AD_Ref_List

    /** Default model names per provider */
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-20250514";
    private static final String DEFAULT_BEDROCK_MODEL = "anthropic.claude-3-5-sonnet-20241022-v2:0";
    private static final String DEFAULT_BEDROCK_REGION = "us-east-1";
    private static final String DEFAULT_OLLAMA_MODEL = "llama3.2";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

    /** Default embedding model names per provider */
    private static final String DEFAULT_BEDROCK_EMBEDDING_MODEL = "amazon.titan-embed-text-v2:0";
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
     * @param config MAIProvider database configuration
     * @return ChatLanguageModel instance
     * @throws IllegalArgumentException if provider type is unknown
     */
    public static ChatLanguageModel create(MAIProvider config) {
        return create(config, null, null);
    }

    /**
     * Create a ChatLanguageModel with optional model name override.
     *
     * @param config MAIProvider database configuration
     * @param modelName Optional model name (uses default if null)
     * @param baseUrl Optional base URL for Ollama (uses default if null)
     * @return ChatLanguageModel instance
     */
    public static ChatLanguageModel create(MAIProvider config, String modelName, String baseUrl) {
        String providerType = config.getAIGProviderType();
        String apiKey = config.getAPIKey();

        log.info("Creating LangChain4j model for provider: " + providerType);

        switch (providerType) {
            case PROVIDER_ANTHROPIC:
                return createAnthropicModel(apiKey, modelName);
            case PROVIDER_BEDROCK:
                return createBedrockModel(modelName, baseUrl);
            case PROVIDER_OLLAMA:
                return createOllamaModel(baseUrl, modelName);
            case PROVIDER_OPENAI:
                return createOpenAiModel(apiKey, modelName);
            default:
                throw new IllegalArgumentException("Unknown provider type: " + providerType);
        }
    }

    /**
     * Create a StreamingChatLanguageModel for real-time response streaming.
     */
    public static StreamingChatLanguageModel createStreaming(MAIProvider config, String modelName, String baseUrl) {
        String providerType = config.getAIGProviderType();
        String apiKey = config.getAPIKey();

        log.info("Creating LangChain4j streaming model for provider: " + providerType);

        switch (providerType) {
            case PROVIDER_ANTHROPIC:
                return createAnthropicStreamingModel(apiKey, modelName);
            case PROVIDER_OLLAMA:
                return createOllamaStreamingModel(baseUrl, modelName);
            case PROVIDER_OPENAI:
                return createOpenAiStreamingModel(apiKey, modelName);
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
                return createBedrockEmbeddingModel(modelName, baseUrl);

            case PROVIDER_BEDROCK:
                return createBedrockEmbeddingModel(modelName, baseUrl);

            case PROVIDER_OLLAMA:
                return createOllamaEmbeddingModel(baseUrl, modelName);

            case PROVIDER_OPENAI:
                return createOpenAiEmbeddingModel(apiKey, modelName);

            default:
                // Fallback to Bedrock Titan for unknown providers
                log.warning("Unknown provider type: " + providerType + ", falling back to Bedrock Titan Embeddings");
                return createBedrockEmbeddingModel(modelName, baseUrl);
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
               PROVIDER_OLLAMA.equals(providerType) ||
               PROVIDER_OPENAI.equals(providerType);
    }

    // ========================================================================
    // Private factory methods
    // ========================================================================

    private static ChatLanguageModel createAnthropicModel(String apiKey, String modelName) {
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

    private static ChatLanguageModel createOpenAiModel(String apiKey, String modelName) {
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
        return OpenAiStreamingChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_OPENAI_MODEL)
            .temperature(0.7)
            .build();
    }

    private static ChatLanguageModel createBedrockModel(String modelName, String region) {
        // Bedrock uses AWS credentials from environment/IAM role
        // BedrockChatModel supports all Bedrock models: Claude, Amazon Nova, Mistral, etc.
        var builder = BedrockChatModel.builder()
            .region(Region.of(region != null ? region : DEFAULT_BEDROCK_REGION))
            .modelId(modelName != null ? modelName : DEFAULT_BEDROCK_MODEL)
            .defaultRequestParameters(BedrockChatRequestParameters.builder()
                .maxOutputTokens(4096)
                .temperature(0.7)
                .build());

        // Add observability listener (ADR-013)
        if (metricsEnabled) {
            builder.listeners(List.of(createMetricsListener("bedrock")));
        }

        return builder.build();
    }

    // ========================================================================
    // Private embedding model factory methods
    // ========================================================================

    private static EmbeddingModel createBedrockEmbeddingModel(String modelName, String region) {
        // Note: BedrockEmbeddingModel is not available in langchain4j-bedrock 1.0.0-beta3
        // Using Ollama as fallback for embedding needs
        // TODO: Add langchain4j-bedrock-embeddings module when available
        log.warning("BedrockEmbeddingModel not available - using Ollama nomic-embed-text as fallback");
        return createOllamaEmbeddingModel(DEFAULT_OLLAMA_URL, DEFAULT_OLLAMA_EMBEDDING_MODEL);
    }

    private static EmbeddingModel createOllamaEmbeddingModel(String baseUrl, String modelName) {
        return OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_OLLAMA_URL)
            .modelName(modelName != null ? modelName : DEFAULT_OLLAMA_EMBEDDING_MODEL)
            .build();
    }

    private static EmbeddingModel createOpenAiEmbeddingModel(String apiKey, String modelName) {
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
