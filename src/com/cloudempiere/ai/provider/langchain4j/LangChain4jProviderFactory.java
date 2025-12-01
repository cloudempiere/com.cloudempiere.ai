package com.cloudempiere.ai.provider.langchain4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.model.X_AIG_Provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
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
 * @author CloudEmpiere
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

    /** Cache for model instances by provider ID */
    private static final Map<Integer, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();
    private static final Map<Integer, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();

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

        return switch (providerType) {
            case PROVIDER_ANTHROPIC -> createAnthropicModel(apiKey, modelName);
            case PROVIDER_BEDROCK -> createBedrockModel(modelName, baseUrl);
            case PROVIDER_OLLAMA -> createOllamaModel(baseUrl, modelName);
            case PROVIDER_OPENAI -> createOpenAiModel(apiKey, modelName);
            default -> throw new IllegalArgumentException("Unknown provider type: " + providerType);
        };
    }

    /**
     * Create a StreamingChatLanguageModel for real-time response streaming.
     */
    public static StreamingChatLanguageModel createStreaming(MAIProvider config, String modelName, String baseUrl) {
        String providerType = config.getAIGProviderType();
        String apiKey = config.getAPIKey();

        log.info("Creating LangChain4j streaming model for provider: " + providerType);

        return switch (providerType) {
            case PROVIDER_ANTHROPIC -> createAnthropicStreamingModel(apiKey, modelName);
            case PROVIDER_OLLAMA -> createOllamaStreamingModel(baseUrl, modelName);
            case PROVIDER_OPENAI -> createOpenAiStreamingModel(apiKey, modelName);
            default -> throw new IllegalArgumentException("Streaming not supported for provider: " + providerType);
        };
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
    }

    // ========================================================================
    // Private factory methods
    // ========================================================================

    private static ChatLanguageModel createAnthropicModel(String apiKey, String modelName) {
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
        return AnthropicStreamingChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_ANTHROPIC_MODEL)
            .maxTokens(4096)
            .temperature(0.7)
            .build();
    }

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

    private static ChatLanguageModel createOpenAiModel(String apiKey, String modelName) {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelName != null ? modelName : DEFAULT_OPENAI_MODEL)
            .temperature(0.7)
            .logRequests(true)
            .logResponses(true)
            .build();
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
        return BedrockChatModel.builder()
            .region(Region.of(region != null ? region : DEFAULT_BEDROCK_REGION))
            .modelId(modelName != null ? modelName : DEFAULT_BEDROCK_MODEL)
            .maxTokens(4096)
            .temperature(0.7)
            .build();
    }
}
