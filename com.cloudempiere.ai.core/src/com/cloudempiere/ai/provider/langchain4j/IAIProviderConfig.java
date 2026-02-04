package com.cloudempiere.ai.provider.langchain4j;

/**
 * Interface for AI Provider configuration.
 *
 * <p>This interface allows for testability without requiring
 * the full iDempiere PO hierarchy.
 *
 * @author Cloudempiere
 * @version 0.22.0
 * @since ADR-042
 */
public interface IAIProviderConfig {

    /**
     * Get the provider type (e.g., "SAT", "ANT", "ABE", "OLL").
     * @return Provider type constant
     */
    String getAIGProviderType();

    /**
     * Get the API key for authentication.
     * @return API key or null if not configured
     */
    String getAPIKey();

    /**
     * Get the model name (e.g., "claude-sonnet-4").
     * @return Model name or null to use default
     */
    String getModelName();

    /**
     * Get the endpoint URL for the provider.
     * @return Endpoint URL or null to use default
     */
    String getEndpoint();

    /**
     * Get the provider ID (for caching).
     * @return Provider ID
     */
    int getAIG_Provider_ID();
}
