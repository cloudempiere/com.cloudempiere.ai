package com.cloudempiere.ai.provider.langchain4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.model.X_AIG_Provider;

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
 * Factory for creating LangChain4j model instances from iDempiere AIG_Provider configuration.
 *
 * <p>Supported provider types (from AD_Ref_List):
 * <ul>
 *   <li><strong>SAT</strong> - iDempiere AI Hub (recommended primary path, OpenAI-compatible)</li>
 *   <li><strong>ANT</strong> - Anthropic Claude (direct API)</li>
 *   <li><strong>ABE</strong> - AWS Bedrock (direct API)</li>
 *   <li><strong>OLL</strong> - Ollama (local LLMs)</li>
 *   <li><strong>MOA</strong> - Mock AI Hub (testing/development)</li>
 * </ul>
 *
 * <p>The AI Hub (SAT) is the recommended path: iDempiere sends OpenAI-compatible
 * requests to the Quarkus AI Hub service which handles provider routing, LangChain4j 1.x
 * features, MCP, and centralized cost tracking. Direct providers (ANT, ABE, OLL) are
 * available as fallback or for offline/development scenarios.
 *
 * @author Cloudempiere
 * @version 10.0.0
 * @since ADR-002 LangChain4j Strategic Adoption
 * @since ADR-042 AI Hub Provider Integration
 * @since ADR-052 OSGi Modernization
 */
@Component(
    service = ILangChain4jProviderFactory.class,
    immediate = true,
    property = {"service.ranking:Integer=100"}
)
public class LangChain4jProviderFactory implements ILangChain4jProviderFactory {

    private static final CLogger log = CLogger.getCLogger(LangChain4jProviderFactory.class);

    /** Provider type constants matching AD_Ref_List values */
    public static final String PROVIDER_ANTHROPIC = X_AIG_Provider.AIGPROVIDERTYPE_AnthropicClaude;
    public static final String PROVIDER_BEDROCK = X_AIG_Provider.AIGPROVIDERTYPE_AWSBedrock;
    public static final String PROVIDER_MOCK_OPENAI = X_AIG_Provider.AIGPROVIDERTYPE_MockAIHub;
    public static final String PROVIDER_AI_HUB = X_AIG_Provider.AIGPROVIDERTYPE_IDempiereAIHub;
    public static final String PROVIDER_OLLAMA = X_AIG_Provider.AIGPROVIDERTYPE_Ollama;

    /** Default model names per provider */
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-20250514";
    private static final String DEFAULT_BEDROCK_MODEL = "anthropic.claude-3-5-sonnet-20241022-v2:0";
    private static final String DEFAULT_BEDROCK_REGION = "us-east-1";
    private static final String DEFAULT_AI_HUB_MODEL = "claude-sonnet-4-20250514";
    private static final String DEFAULT_MOCK_OPENAI_MODEL = "llama3.2";
    private static final String DEFAULT_MOCK_OPENAI_URL = "http://localhost:8081/v1";
    private static final String DEFAULT_OLLAMA_MODEL = "llama3.2";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

    /** Default embedding model names per provider */
    private static final String DEFAULT_BEDROCK_EMBEDDING_MODEL = "amazon.titan-embed-text-v2:0";
    private static final String DEFAULT_AI_HUB_EMBEDDING_MODEL = "text-embedding-3-small";
    private static final String DEFAULT_MOCK_OPENAI_EMBEDDING_MODEL = "nomic-embed-text";
    private static final String DEFAULT_OLLAMA_EMBEDDING_MODEL = "nomic-embed-text";

    /** Model caches by provider ID */
    private static final Map<Integer, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();
    private static final Map<Integer, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();
    private static final Map<Integer, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    // ========================================================================
    // OSGi Lifecycle
    // ========================================================================

    @Activate
    protected void activate(BundleContext context) {
        log.warning("[STARTUP TIMING] LangChain4jProviderFactory activated");
    }

    @Deactivate
    protected void deactivate() {
        clearAllCaches();
        log.info("LangChain4jProviderFactory deactivated");
    }

    // ========================================================================
    // ILangChain4jProviderFactory Interface Implementation
    // ========================================================================

    @Override
    public ChatLanguageModel createModel(MAIProvider config) {
        return create(config);
    }

    @Override
    public ChatLanguageModel createModel(MAIProvider config, String modelName, String baseUrl) {
        return create(config, modelName, baseUrl);
    }

    @Override
    public StreamingChatLanguageModel createStreamingModel(MAIProvider config) {
        return createStreaming(config);
    }

    @Override
    public StreamingChatLanguageModel createStreamingModel(MAIProvider config, String modelName, String baseUrl) {
        return createStreaming(config, modelName, baseUrl);
    }

    @Override
    public ChatLanguageModel getOrCreateModel(MAIProvider config) {
        return modelCache.computeIfAbsent(config.getAIG_Provider_ID(),
            id -> createModel(config));
    }

    @Override
    public EmbeddingModel createEmbedding(MAIProvider config) {
        return createEmbeddingModel(config);
    }

    @Override
    public EmbeddingModel createEmbedding(MAIProvider config, String modelName, String baseUrl) {
        return createEmbeddingModel(config, modelName, baseUrl);
    }

    @Override
    public EmbeddingModel getOrCreateEmbedding(MAIProvider config) {
        return embeddingModelCache.computeIfAbsent(config.getAIG_Provider_ID(),
            id -> createEmbedding(config));
    }

    @Override
    public boolean supportsNativeEmbeddings(String providerType) {
        return hasNativeEmbeddings(providerType);
    }

    @Override
    public void clearAllCaches() {
        modelCache.clear();
        streamingModelCache.clear();
        embeddingModelCache.clear();
        log.info("LangChain4j model caches cleared");
    }

    @Override
    public void evict(int providerId) {
        modelCache.remove(providerId);
        streamingModelCache.remove(providerId);
        embeddingModelCache.remove(providerId);
    }

    // ========================================================================
    // Static Factory Methods
    // ========================================================================

    /**
     * Create a ChatLanguageModel from MAIProvider configuration.
     *
     * @param config MAIProvider database configuration
     * @return ChatLanguageModel instance
     * @throws IllegalArgumentException if provider type is unknown
     */
    public static ChatLanguageModel create(MAIProvider config) {
        return create(config, resolveModelName(config), null);
    }

    /**
     * Create a ChatLanguageModel with optional model name and base URL override.
     *
     * @param config MAIProvider database configuration
     * @param modelName Optional model name (uses config or default if null)
     * @param baseUrl Optional base URL override (uses config.getURL() or default if null)
     * @return ChatLanguageModel instance
     */
    public static ChatLanguageModel create(MAIProvider config, String modelName, String baseUrl) {
        String providerType = config.getAIGProviderType();
        String apiKey = config.getAPIKey();

        if (modelName == null) {
            modelName = resolveModelName(config);
        }
        if (baseUrl == null) {
            baseUrl = resolveBaseUrl(config);
        }

        log.info("Creating LangChain4j model for provider: " + providerType +
                (modelName != null ? ", model: " + modelName : " (using default model)"));

        switch (providerType) {
            case PROVIDER_AI_HUB:
                return createAIHubModel(baseUrl, modelName, apiKey);
            case PROVIDER_ANTHROPIC:
                return createAnthropicModel(apiKey, modelName);
            case PROVIDER_BEDROCK:
                return createBedrockModel(modelName, apiKey);
            case PROVIDER_OLLAMA:
                return createOllamaModel(baseUrl, modelName);
            case PROVIDER_MOCK_OPENAI:
                return createMockOpenAiModel(baseUrl, modelName, apiKey);
            default:
                throw new IllegalArgumentException("Unknown provider type: " + providerType);
        }
    }

    /**
     * Create a StreamingChatLanguageModel from MAIProvider configuration.
     *
     * @param config MAIProvider database configuration
     * @return StreamingChatLanguageModel instance
     */
    public static StreamingChatLanguageModel createStreaming(MAIProvider config) {
        return createStreaming(config, resolveModelName(config), null);
    }

    /**
     * Create a StreamingChatLanguageModel with optional overrides.
     */
    public static StreamingChatLanguageModel createStreaming(MAIProvider config, String modelName, String baseUrl) {
        String providerType = config.getAIGProviderType();
        String apiKey = config.getAPIKey();

        if (modelName == null) {
            modelName = resolveModelName(config);
        }
        if (baseUrl == null) {
            baseUrl = resolveBaseUrl(config);
        }

        log.info("Creating LangChain4j streaming model for provider: " + providerType +
                (modelName != null ? ", model: " + modelName : " (using default model)"));

        switch (providerType) {
            case PROVIDER_AI_HUB:
                return createAIHubStreamingModel(baseUrl, modelName, apiKey);
            case PROVIDER_ANTHROPIC:
                return createAnthropicStreamingModel(apiKey, modelName);
            case PROVIDER_BEDROCK:
                return createBedrockStreamingModel(modelName, apiKey);
            case PROVIDER_OLLAMA:
                return createOllamaStreamingModel(baseUrl, modelName);
            case PROVIDER_MOCK_OPENAI:
                return createMockOpenAiStreamingModel(baseUrl, modelName, apiKey);
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
        embeddingModelCache.clear();
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
     * <p>Provider mapping:
     * <ul>
     *   <li>AI Hub → Routes through Quarkus service (OpenAI-compatible)</li>
     *   <li>Anthropic → Falls back to AWS Bedrock Titan (no native embeddings)</li>
     *   <li>AWS Bedrock → Amazon Titan Embed Text v2</li>
     *   <li>Ollama → nomic-embed-text (local)</li>
     * </ul>
     *
     * @param config MAIProvider database configuration
     * @return EmbeddingModel instance
     */
    public static EmbeddingModel createEmbeddingModel(MAIProvider config) {
        return createEmbeddingModel(config, null, null);
    }

    /**
     * Create an EmbeddingModel with optional model name and base URL override.
     */
    public static EmbeddingModel createEmbeddingModel(MAIProvider config, String modelName, String baseUrl) {
        String providerType = config.getAIGProviderType();
        String apiKey = config.getAPIKey();

        if (baseUrl == null) {
            baseUrl = resolveBaseUrl(config);
        }

        log.info("Creating LangChain4j embedding model for provider: " + providerType);

        switch (providerType) {
            case PROVIDER_AI_HUB:
                return createAIHubEmbeddingModel(baseUrl, modelName, apiKey);

            case PROVIDER_ANTHROPIC:
                // Anthropic doesn't have embedding models - use AWS Bedrock Titan
                log.info("Anthropic provider: using AWS Bedrock Titan Embeddings");
                return createBedrockEmbeddingModel(modelName, apiKey);

            case PROVIDER_BEDROCK:
                return createBedrockEmbeddingModel(modelName, apiKey);

            case PROVIDER_OLLAMA:
                return createOllamaEmbeddingModel(baseUrl, modelName);

            case PROVIDER_MOCK_OPENAI:
                return createMockOpenAiEmbeddingModel(baseUrl, modelName, apiKey);

            default:
                throw new IllegalArgumentException("Unknown provider type for embeddings: " + providerType);
        }
    }

    /**
     * Get or create a cached EmbeddingModel instance.
     */
    public static EmbeddingModel getOrCreateEmbeddingModel(MAIProvider config) {
        return embeddingModelCache.computeIfAbsent(config.getAIG_Provider_ID(),
            id -> createEmbeddingModel(config));
    }

    /**
     * Check if the provider supports embeddings natively.
     */
    public static boolean hasNativeEmbeddings(String providerType) {
        return PROVIDER_AI_HUB.equals(providerType) ||
               PROVIDER_BEDROCK.equals(providerType) ||
               PROVIDER_OLLAMA.equals(providerType) ||
               PROVIDER_MOCK_OPENAI.equals(providerType);
    }

    // ========================================================================
    // Config helpers
    // ========================================================================

    /**
     * Resolve model name from config, returning null if empty.
     */
    private static String resolveModelName(MAIProvider config) {
        String modelName = config.getModelName();
        if (modelName != null && modelName.trim().isEmpty()) {
            return null;
        }
        return modelName;
    }

    /**
     * Resolve base URL from config (AIG_Provider.URL field), returning null if empty.
     */
    private static String resolveBaseUrl(MAIProvider config) {
        String url = config.getURL();
        if (url != null && url.trim().isEmpty()) {
            return null;
        }
        return url;
    }

    // ========================================================================
    // AI Hub (SAT) - OpenAI-compatible proxy to Quarkus AI Hub service
    // ========================================================================

    /**
     * Create a ChatLanguageModel that routes through iDempiere AI Hub Service.
     *
     * <p>The AI Hub exposes an OpenAI-compatible API, so we use OpenAiChatModel
     * pointed at the hub's URL. The hub handles provider routing, LangChain4j 1.x
     * features, MCP, and centralized cost tracking.
     *
     * @param baseUrl AI Hub service URL (from AIG_Provider.URL)
     * @param modelName Target model name (hub routes to upstream provider)
     * @param apiKey Bearer token for AI Hub authentication
     * @return ChatLanguageModel proxying through AI Hub
     */
    private static ChatLanguageModel createAIHubModel(String baseUrl, String modelName, String apiKey) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException(
                "AI Hub URL not configured. Set URL in AIG_Provider (e.g., http://ai-hub:8080/v1).");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException(
                "AI Hub API key not configured. Set APIKey (bearer token) in AIG_Provider.");
        }

        return OpenAiChatModel.builder()
            .baseUrl(normalizeUrl(baseUrl))
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_AI_HUB_MODEL)
            .temperature(0.7)
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    private static StreamingChatLanguageModel createAIHubStreamingModel(String baseUrl, String modelName, String apiKey) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException(
                "AI Hub URL not configured. Set URL in AIG_Provider (e.g., http://ai-hub:8080/v1).");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException(
                "AI Hub API key not configured. Set APIKey (bearer token) in AIG_Provider.");
        }

        return OpenAiStreamingChatModel.builder()
            .baseUrl(normalizeUrl(baseUrl))
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_AI_HUB_MODEL)
            .temperature(0.7)
            .build();
    }

    private static EmbeddingModel createAIHubEmbeddingModel(String baseUrl, String modelName, String apiKey) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException(
                "AI Hub URL not configured. Set URL in AIG_Provider (e.g., http://ai-hub:8080/v1).");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException(
                "AI Hub API key not configured. Set APIKey (bearer token) in AIG_Provider.");
        }

        return OpenAiEmbeddingModel.builder()
            .baseUrl(normalizeUrl(baseUrl))
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_AI_HUB_EMBEDDING_MODEL)
            .build();
    }

    /**
     * Normalize URL to ensure it ends with /v1 for OpenAI-compatible endpoints.
     * Handles cases like "http://host:8080", "http://host:8080/", "http://host:8080/v1".
     */
    private static String normalizeUrl(String url) {
        if (url.endsWith("/v1") || url.endsWith("/v1/")) {
            return url;
        }
        if (url.endsWith("/")) {
            return url + "v1";
        }
        return url + "/v1";
    }

    // ========================================================================
    // Anthropic (ANT) - Direct API
    // ========================================================================

    private static ChatLanguageModel createAnthropicModel(String apiKey, String modelName) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Anthropic API key is not configured. Please set the API Key in the AI Provider configuration.");
        }
        return AnthropicChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_ANTHROPIC_MODEL)
            .maxTokens(4096)
            .temperature(0.7)
            .logRequests(true)
            .logResponses(true)
            .build();
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

    // ========================================================================
    // Ollama (OLL) - Local LLMs
    // ========================================================================

    private static ChatLanguageModel createOllamaModel(String baseUrl, String modelName) {
        return OllamaChatModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_OLLAMA_URL)
            .modelName(modelName != null ? modelName : DEFAULT_OLLAMA_MODEL)
            .temperature(0.7)
            .build();
    }

    private static StreamingChatLanguageModel createOllamaStreamingModel(String baseUrl, String modelName) {
        return OllamaStreamingChatModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_OLLAMA_URL)
            .modelName(modelName != null ? modelName : DEFAULT_OLLAMA_MODEL)
            .temperature(0.7)
            .build();
    }

    private static EmbeddingModel createOllamaEmbeddingModel(String baseUrl, String modelName) {
        return OllamaEmbeddingModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_OLLAMA_URL)
            .modelName(modelName != null ? modelName : DEFAULT_OLLAMA_EMBEDDING_MODEL)
            .build();
    }

    // ========================================================================
    // Mock OpenAI (MOA) - Testing/Development
    // ========================================================================

    private static ChatLanguageModel createMockOpenAiModel(String baseUrl, String modelName, String apiKey) {
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_MOCK_OPENAI_URL)
            .apiKey(apiKey != null && !apiKey.isEmpty() ? apiKey : "test")
            .modelName(modelName != null ? modelName : DEFAULT_MOCK_OPENAI_MODEL)
            .temperature(0.7)
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    private static StreamingChatLanguageModel createMockOpenAiStreamingModel(String baseUrl, String modelName, String apiKey) {
        return OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_MOCK_OPENAI_URL)
            .apiKey(apiKey != null && !apiKey.isEmpty() ? apiKey : "test")
            .modelName(modelName != null ? modelName : DEFAULT_MOCK_OPENAI_MODEL)
            .temperature(0.7)
            .build();
    }

    private static EmbeddingModel createMockOpenAiEmbeddingModel(String baseUrl, String modelName, String apiKey) {
        return OpenAiEmbeddingModel.builder()
            .baseUrl(baseUrl != null ? baseUrl : DEFAULT_MOCK_OPENAI_URL)
            .apiKey(apiKey != null && !apiKey.isEmpty() ? apiKey : "test")
            .modelName(modelName != null ? modelName : DEFAULT_MOCK_OPENAI_EMBEDDING_MODEL)
            .build();
    }

    // ========================================================================
    // AWS Bedrock (ABE) - Direct API with OSGi workaround
    // ========================================================================

    private static ChatLanguageModel createBedrockModel(String modelName, String apiKey) {
        BedrockCredentials creds = parseBedrockCredentials(apiKey, "Bedrock");

        var builder = BedrockChatModelWrapper.builder()
            .region(Region.of(creds.region))
            .model(modelName != null ? modelName : DEFAULT_BEDROCK_MODEL)
            .maxTokens(4096)
            .temperature(0.7f);

        if (creds.accessKeyId != null) {
            builder.credentials(creds.accessKeyId, creds.secretAccessKey);
        }

        return builder.build();
    }

    private static StreamingChatLanguageModel createBedrockStreamingModel(String modelName, String apiKey) {
        BedrockCredentials creds = parseBedrockCredentials(apiKey, "Bedrock streaming");

        var builder = BedrockStreamingChatModelWrapper.builder()
            .region(Region.of(creds.region))
            .model(modelName != null ? modelName : DEFAULT_BEDROCK_MODEL)
            .maxTokens(4096)
            .temperature(0.7f);

        if (creds.accessKeyId != null) {
            builder.credentials(creds.accessKeyId, creds.secretAccessKey);
        }

        return builder.build();
    }

    private static EmbeddingModel createBedrockEmbeddingModel(String modelName, String apiKey) {
        BedrockCredentials creds = parseBedrockCredentials(apiKey, "Bedrock embedding");

        var builder = BedrockEmbeddingModelWrapper.builder()
            .region(Region.of(creds.region))
            .model(modelName != null ? modelName : DEFAULT_BEDROCK_EMBEDDING_MODEL);

        if (creds.accessKeyId != null) {
            builder.credentials(creds.accessKeyId, creds.secretAccessKey);
        }

        return builder.build();
    }

    /**
     * Parse Bedrock credentials from API key format "accessKeyId:secretAccessKey:region".
     */
    private static BedrockCredentials parseBedrockCredentials(String apiKey, String context) {
        BedrockCredentials creds = new BedrockCredentials();
        creds.region = DEFAULT_BEDROCK_REGION;

        if (apiKey != null && !apiKey.isEmpty()) {
            String[] parts = apiKey.split(":");
            if (parts.length >= 2) {
                creds.accessKeyId = parts[0];
                creds.secretAccessKey = parts[1];
                if (parts.length >= 3 && !parts[2].isEmpty()) {
                    creds.region = parts[2];
                }
                log.info(context + ": Using explicit credentials, region: " + creds.region);
            } else {
                log.warning(context + ": Invalid API key format. Expected 'accessKeyId:secretAccessKey:region'. Using default credentials.");
            }
        } else {
            log.info(context + ": No API key configured, using default AWS credentials provider");
        }

        return creds;
    }

    /** Simple holder for parsed Bedrock credentials. */
    private static class BedrockCredentials {
        String accessKeyId;
        String secretAccessKey;
        String region;
    }
}
