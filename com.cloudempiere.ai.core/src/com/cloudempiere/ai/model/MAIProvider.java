package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.model.X_AD_Role;
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
	 * Resolve the best active provider for the current user, respecting the role's
	 * AIAccessLevel setting.
	 *
	 * <p>Priority in all cases: IsDefault=Y over IsDefault=N, tenant over system.
	 *
	 * <ul>
	 *   <li>{@code All} (or null) — open access; returns the global default provider</li>
	 *   <li>{@code UserRoleAccess} — returns the highest-priority provider the user/role
	 *       has an explicit {@code AIG_Provider_Access} grant for</li>
	 *   <li>{@code None} — returns null (access blocked)</li>
	 * </ul>
	 *
	 * @param ctx     Context
	 * @param trxName Transaction name
	 * @return Best accessible provider, or null if blocked/none found
	 */
	public static MAIProvider getForUser(Properties ctx, String trxName) {
		MRole role = MRole.getDefault(ctx, false);
		String accessLevel = role != null ? role.getAIAccessLevel() : null;
		if (accessLevel == null)
			accessLevel = X_AD_Role.AIACCESSLEVEL_None;

		if (X_AD_Role.AIACCESSLEVEL_None.equals(accessLevel))
			return null;

		if (X_AD_Role.AIACCESSLEVEL_UserRoleAccess.equals(accessLevel)) {
			// role is non-null here: null role → null accessLevel → defaulted to None above
			int userId = Env.getAD_User_ID(ctx);
			return getAccessibleProvider(ctx, role.getAD_Role_ID(), userId, trxName);
		}

		// All — check for explicit user-level override first, then fall back to default
		int userId = Env.getAD_User_ID(ctx);
		if (userId > 0) {
			MAIProvider userOverride = getAccessibleProvider(ctx, 0, userId, trxName);
			if (userOverride != null)
				return userOverride;
		}
		return getDefault(ctx, trxName);
	}

	/**
	 * Get the first active provider the given role and/or user has an explicit
	 * {@code AIG_Provider_Access} grant for, ordered by IsDefault DESC.
	 *
	 * @param ctx     Context
	 * @param roleId  AD_Role_ID (0 to skip)
	 * @param userId  AD_User_ID (0 to skip)
	 * @param trxName Transaction name
	 * @return First accessible provider, or null if none found
	 */
	public static MAIProvider getAccessibleProvider(Properties ctx, int roleId, int userId, String trxName) {
		if (roleId <= 0 && userId <= 0)
			return null;

		StringBuilder whereClause = new StringBuilder(
			"IsActive='Y' AND AD_Client_ID IN (0,?) AND AIG_Provider_ID IN ("
			+ "SELECT AIG_Provider_ID FROM AIG_Provider_Access WHERE IsActive='Y'");
		List<Object> params = new ArrayList<>();
		params.add(Env.getAD_Client_ID(ctx));

		if (userId > 0 && roleId > 0) {
			whereClause.append(" AND (AD_User_ID=? OR AD_Role_ID=?)");
			params.add(userId);
			params.add(roleId);
		} else if (userId > 0) {
			whereClause.append(" AND AD_User_ID=?");
			params.add(userId);
		} else {
			whereClause.append(" AND AD_Role_ID=?");
			params.add(roleId);
		}
		whereClause.append(")");

		return new Query(ctx, Table_Name, whereClause.toString(), trxName)
			.setParameters(params.toArray())
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
