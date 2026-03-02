package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.Env;

import com.cloudempiere.ai.provider.langchain4j.IAIProviderConfig;

/**
 * Model class for AIG_Provider
 *
 * <p>Provides access to AI provider configuration stored in the AIG_Provider table.
 * Supports caching for performance.
 */
public class MAIProvider extends X_AIG_Provider implements IAIProviderConfig {

	private static final long serialVersionUID = -8293759018862021378L;

	/** Cache of providers by ID */
	private static CCache<Integer, MAIProvider> s_cache =
		new CCache<>(Table_Name, 10, 60); // 60 minute timeout

	public MAIProvider(Properties ctx, int AIG_Provider_ID, String trxName) {
		super(ctx, AIG_Provider_ID, trxName);
	}

	public MAIProvider(Properties ctx, int AIG_Provider_ID, String trxName, String... virtualColumns) {
		super(ctx, AIG_Provider_ID, trxName, virtualColumns);
	}

	public MAIProvider(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get AI Provider by ID (cached)
	 *
	 * @param ctx Context
	 * @param AIG_Provider_ID Provider ID
	 * @param trxName Transaction name
	 * @return MAIProvider or null if not found
	 */
	public static MAIProvider get(Properties ctx, int AIG_Provider_ID, String trxName) {
		if (AIG_Provider_ID <= 0) {
			return null;
		}

		MAIProvider provider = s_cache.get(AIG_Provider_ID);
		if (provider != null) {
			return provider;
		}

		provider = new MAIProvider(ctx, AIG_Provider_ID, trxName);
		if (provider.getAIG_Provider_ID() == AIG_Provider_ID) {
			s_cache.put(AIG_Provider_ID, provider);
			return provider;
		}

		return null;
	}

	/**
	 * Get default AI Provider for the current client.
	 *
	 * <p>Priority order:
	 * <ol>
	 *   <li>Default provider for current tenant (IsDefault='Y' AND AD_Client_ID=current)</li>
	 *   <li>Default system provider (IsDefault='Y' AND AD_Client_ID=0)</li>
	 *   <li>Any active provider for current tenant</li>
	 *   <li>Any active system provider (fallback)</li>
	 * </ol>
	 *
	 * @param ctx Context
	 * @param trxName Transaction name
	 * @return Default provider or null if none found
	 */
	public static MAIProvider getDefault(Properties ctx, String trxName) {
		// Order by IsDefault DESC (Y before N), then AD_Client_ID DESC (tenant before system)
		return new Query(ctx, Table_Name, "AD_Client_ID IN (0,?) AND IsActive='Y'", trxName)
			.setParameters(Env.getAD_Client_ID(ctx))
			.setOrderBy("IsDefault DESC, AD_Client_ID DESC, AIG_Provider_ID")
			.first();
	}

	/**
	 * Get AI Provider by provider type
	 *
	 * @param ctx Context
	 * @param providerType Provider type (e.g., "ANT" for Anthropic, "ABE" for AWS Bedrock)
	 * @param trxName Transaction name
	 * @return First matching provider or null
	 */
	public static MAIProvider getByType(Properties ctx, String providerType, String trxName) {
		if (providerType == null || providerType.isEmpty()) {
			return null;
		}

		return new Query(ctx, Table_Name, "AIGProviderType=? AND IsActive='Y'", trxName)
			.setClient_ID()
			.setParameters(providerType)
			.first();
	}

	/**
	 * Clear the provider cache
	 */
	public static void clearCache() {
		s_cache.reset();
	}

	@Override
	public String getEndpoint() {
		return getURL();
	}
}
