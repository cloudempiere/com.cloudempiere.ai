package com.cloudempiere.ai.provider.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;
import org.osgi.service.component.annotations.Component;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.AIProviderException;
import com.cloudempiere.ai.provider.IAIProvider;

/**
 * AI Provider Factory Implementation
 *
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
@Component(
	service = IAIProviderFactory.class,
	immediate = true,
	property = {"service.ranking:Integer=100"}
)
public class AIProviderFactory implements IAIProviderFactory {

	private static final CLogger log = CLogger.getCLogger(AIProviderFactory.class);

	/**
	 * Registry mapping AIGProviderType values to implementation classes
	 * Key: AIGProviderType value from database (e.g., "OPENAI")
	 * Value: IAIProvider implementation class
	 */
	private Map<String, Class<? extends IAIProvider>> providerRegistry = new ConcurrentHashMap<>();

	/**
	 * Cache of initialized provider instances
	 * Key: AIG_Provider_ID from database
	 * Value: Initialized IAIProvider instance
	 */
	private Map<Integer, IAIProvider> providerCache = new ConcurrentHashMap<>();

	/**
	 * Constructor
	 * Registers default provider implementations
	 */
	public AIProviderFactory() {
		registerDefaultProviders();
	}

	/**
	 * Register default provider implementations
	 * Maps AIGProviderType values to implementation classes
	 *
	 * This method is called during factory initialization and registers
	 * all built-in provider implementations.
	 */
	private void registerDefaultProviders() {
		// Register Anthropic Claude provider
		registerProvider(
			MAIProvider.AIGPROVIDERTYPE_AnthropicClaude,
			com.cloudempiere.ai.provider.impl.AnthropicProvider.class
		);

		// Register AWS Bedrock provider
		registerProvider(
			MAIProvider.AIGPROVIDERTYPE_AWSBedrock,
			com.cloudempiere.ai.provider.impl.AWSBedrockProvider.class
		);

		// TODO: Register other provider implementations as they are created
		// Example:
		// registerProvider(MAIProvider.AIGPROVIDERTYPE_OpenAI, OpenAIProvider.class);

		log.info("AI Provider Factory initialized - " + providerRegistry.size() + " provider types registered");
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

	@Override
	public IAIProvider get(MAIProvider providerConfig) throws AIProviderException {
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

	@Override
	public IAIProvider get(Properties ctx, int providerId, String trxName)
			throws AIProviderException {

		MAIProvider config = new MAIProvider(ctx, providerId, trxName);

		if (config.getAIG_Provider_ID() <= 0) {
			throw new AIProviderException("Provider not found with ID: " + providerId);
		}

		if (!config.isActive()) {
			throw new AIProviderException("Provider is not active: " + config.getName());
		}

		return get(config);
	}

	@Override
	public List<String> getRegisteredProviderTypes() {
		return new ArrayList<>(providerRegistry.keySet());
	}

	@Override
	public boolean isProviderTypeRegistered(String providerType) {
		return providerRegistry.containsKey(providerType);
	}

	@Override
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

	@Override
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

	@Override
	public IAIProvider reloadProvider(Properties ctx, int providerId, String trxName)
			throws AIProviderException {
		invalidateProvider(providerId);
		return get(ctx, providerId, trxName);
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
	 * Shutdown all providers and clear factory
	 * Called during plugin deactivation
	 */
	public void shutdownAll() {
		log.info("Shutting down all AI providers");
		clearCache();
		log.info("AI provider factory shut down complete");
	}
}
