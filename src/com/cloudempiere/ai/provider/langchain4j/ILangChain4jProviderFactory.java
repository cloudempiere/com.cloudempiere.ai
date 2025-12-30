package com.cloudempiere.ai.provider.langchain4j;

import com.cloudempiere.ai.model.MAIProvider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * OSGi service interface for creating LangChain4j model instances.
 *
 * <p>This interface provides a clean contract for AI model creation,
 * replacing the static factory pattern with proper OSGi service injection.
 *
 * <p>Usage with OSGi Declarative Services:
 * <pre>
 * &#64;Reference
 * private ILangChain4jProviderFactory providerFactory;
 *
 * public void useModel(MAIProvider config) {
 *     ChatLanguageModel model = providerFactory.createModel(config);
 *     // use model...
 * }
 * </pre>
 *
 * @author Cloudempiere
 * @version 10.0.0
 * @since ADR-052 OSGi Modernization
 * @see LangChain4jProviderFactory
 */
public interface ILangChain4jProviderFactory {

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
    ChatLanguageModel createModel(MAIProvider config);

    /**
     * Create a ChatLanguageModel with optional model name and base URL override.
     *
     * @param config MAIProvider database configuration
     * @param modelName Optional model name (uses config.getModelName() or default if null)
     * @param baseUrl Optional base URL for Ollama (uses default if null)
     * @return ChatLanguageModel instance
     */
    ChatLanguageModel createModel(MAIProvider config, String modelName, String baseUrl);

    /**
     * Create a StreamingChatLanguageModel from MAIProvider configuration.
     *
     * <p>Uses the ModelName from the provider configuration if set,
     * otherwise falls back to provider-specific defaults.
     *
     * @param config MAIProvider database configuration
     * @return StreamingChatLanguageModel instance
     */
    StreamingChatLanguageModel createStreamingModel(MAIProvider config);

    /**
     * Create a StreamingChatLanguageModel with optional model name and base URL override.
     *
     * @param config MAIProvider database configuration
     * @param modelName Optional model name (uses config.getModelName() or default if null)
     * @param baseUrl Optional base URL for Ollama (uses default if null)
     * @return StreamingChatLanguageModel instance
     */
    StreamingChatLanguageModel createStreamingModel(MAIProvider config, String modelName, String baseUrl);

    /**
     * Get or create a cached ChatLanguageModel instance.
     *
     * @param config MAIProvider configuration
     * @return Cached or newly created ChatLanguageModel
     */
    ChatLanguageModel getOrCreateModel(MAIProvider config);

    /**
     * Create an EmbeddingModel from MAIProvider configuration.
     *
     * <p>For RAG (Retrieval-Augmented Generation), we need embedding models
     * to convert text to vectors for semantic search.
     *
     * @param config MAIProvider database configuration
     * @return EmbeddingModel instance
     * @throws IllegalArgumentException if provider type is unknown
     */
    EmbeddingModel createEmbedding(MAIProvider config);

    /**
     * Create an EmbeddingModel with optional model name and base URL override.
     *
     * @param config MAIProvider database configuration
     * @param modelName Optional model name (uses default if null)
     * @param baseUrl Optional base URL for Ollama (uses default if null)
     * @return EmbeddingModel instance
     */
    EmbeddingModel createEmbedding(MAIProvider config, String modelName, String baseUrl);

    /**
     * Get or create a cached EmbeddingModel instance.
     *
     * @param config MAIProvider configuration
     * @return Cached or newly created EmbeddingModel
     */
    EmbeddingModel getOrCreateEmbedding(MAIProvider config);

    /**
     * Check if the provider supports embeddings natively.
     *
     * @param providerType Provider type code
     * @return true if provider has native embedding support
     */
    boolean supportsNativeEmbeddings(String providerType);

    /**
     * Clear all model caches.
     *
     * <p>Call this when provider configuration changes to ensure
     * stale model instances are not reused.
     */
    void clearAllCaches();

    /**
     * Remove a specific provider from all caches.
     *
     * @param providerId AIG_Provider_ID to evict
     */
    void evict(int providerId);

    /**
     * Enable or disable metrics collection.
     *
     * @param enabled true to enable metrics, false to disable
     */
    void enableMetrics(boolean enabled);

    /**
     * Check if metrics collection is enabled.
     *
     * @return true if metrics are enabled
     */
    boolean metricsEnabled();
}
