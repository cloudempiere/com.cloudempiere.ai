package com.cloudempiere.ai.provider.factory;

import java.util.List;
import java.util.Properties;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.AIProviderException;
import com.cloudempiere.ai.provider.IAIProvider;

/**
 * AI Provider Factory Interface
 *
 * Factory for creating and managing AI provider instances.
 * Uses AIGProviderType from database to determine which implementation to load.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public interface IAIProviderFactory {

	/**
	 * Get a provider instance based on provider model
	 * Uses AIGProviderType field to determine which implementation to load
	 *
	 * @param provider the AI provider model from database
	 * @return an instance of IAIProvider
	 * @throws AIProviderException if provider cannot be created or initialized
	 */
	IAIProvider get(MAIProvider provider) throws AIProviderException;

	/**
	 * Get a provider instance based on provider ID
	 * Convenience method that loads MAIProvider from database
	 *
	 * @param ctx the context properties
	 * @param providerId the AIG_Provider_ID
	 * @param trxName the transaction name
	 * @return an instance of IAIProvider
	 * @throws AIProviderException if provider cannot be loaded
	 */
	IAIProvider get(Properties ctx, int providerId, String trxName) throws AIProviderException;

	/**
	 * Get list of all registered provider types
	 * Returns AIGProviderType values that can be used in database
	 *
	 * @return list of provider type identifiers
	 */
	List<String> getRegisteredProviderTypes();

	/**
	 * Check if a provider type is registered
	 *
	 * @param providerType AIGProviderType value
	 * @return true if implementation is registered
	 */
	boolean isProviderTypeRegistered(String providerType);

	/**
	 * Invalidate cached provider for specific ID
	 * Forces reload on next access
	 *
	 * @param providerId AIG_Provider_ID
	 */
	void invalidateProvider(int providerId);

	/**
	 * Clear all cached providers
	 * Shuts down all cached provider instances and clears the cache
	 */
	void clearCache();

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
	IAIProvider reloadProvider(Properties ctx, int providerId, String trxName)
			throws AIProviderException;
}