/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.provider.langchain4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;

import com.cloudempiere.ai.model.I_AIG_Provider_Access;
import com.cloudempiere.ai.model.MAIPromptConfig;
import com.cloudempiere.ai.model.X_AIG_Prompt_Config;

/**
 * Resolves the active {@code AIG_Prompt_Config} profile for a given provider+role+user
 * combination using a 3-step resolution chain (ADR-063 Phase 1).
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Explicit {@code AIG_Prompt_Config_ID} on a matching {@code AIG_Provider_Access} row
 *       (user-level takes precedence over role-level; provider-specific over agnostic)</li>
 *   <li>Client default: {@code AIG_Prompt_Config} where {@code IsDefault='Y'} and
 *       {@code AIGStatus='A'}</li>
 *   <li>No profile — returns 0 (no addendum appended)</li>
 * </ol>
 *
 * <p>This is a stateless utility class — no session state, no LLM involvement.
 * Resolution is deterministic at prompt-assembly time.
 *
 * @see com.cloudempiere.ai.docs.adr.ADR063
 */
public class PromptProfileResolver {

	private PromptProfileResolver() {}

	/**
	 * Resolve the prompt profile for the current session.
	 *
	 * @param ctx        iDempiere context
	 * @param providerID AIG_Provider_ID of the active provider (0 to skip provider filter)
	 * @param roleID     AD_Role_ID of the current user's role (0 to skip)
	 * @param userID     AD_User_ID of the current user (0 to skip)
	 * @return resolved AIG_Prompt_Config_ID, or 0 if no profile found
	 */
	public static int resolve(Properties ctx, int providerID, int roleID, int userID) {
		// Step 1: explicit FK on AIG_Provider_Access for this role/user
		int configID = lookupFromProviderAccess(ctx, providerID, roleID, userID);
		if (configID > 0)
			return configID;

		// Step 2: client default (IsDefault='Y', AIGStatus='A')
		configID = lookupClientDefault(ctx);
		if (configID > 0)
			return configID;

		// Step 3: no profile
		return 0;
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Step 1: find the first active {@code AIG_Provider_Access} row for this
	 * role/user that carries a non-null {@code AIG_Prompt_Config_ID}.
	 *
	 * <p>Ordering precedence: user-level before role-level; provider-specific
	 * before provider-agnostic (null {@code AIG_Provider_ID}).
	 */
	private static int lookupFromProviderAccess(Properties ctx, int providerID, int roleID, int userID) {
		if (roleID <= 0 && userID <= 0)
			return 0;

		StringBuilder where = new StringBuilder(
			"IsActive='Y' AND AD_Client_ID IN (0,?)"
			+ " AND AIG_Prompt_Config_ID IS NOT NULL");
		List<Object> params = new ArrayList<>();
		params.add(Env.getAD_Client_ID(ctx));

		if (userID > 0 && roleID > 0) {
			where.append(" AND (AD_User_ID=? OR AD_Role_ID=?)");
			params.add(userID);
			params.add(roleID);
		} else if (userID > 0) {
			where.append(" AND AD_User_ID=?");
			params.add(userID);
		} else {
			where.append(" AND AD_Role_ID=?");
			params.add(roleID);
		}

		// Prefer provider-specific rows; also accept provider-agnostic (null AIG_Provider_ID)
		if (providerID > 0) {
			where.append(" AND (AIG_Provider_ID=? OR AIG_Provider_ID IS NULL)");
			params.add(providerID);
		}

		// user-level first, then provider-specific over agnostic
		String orderBy = "CASE WHEN AD_User_ID IS NOT NULL THEN 0 ELSE 1 END,"
			+ " CASE WHEN AIG_Provider_ID IS NOT NULL THEN 0 ELSE 1 END";

		PO row = new Query(ctx, I_AIG_Provider_Access.Table_Name, where.toString(), null)
			.setParameters(params.toArray())
			.setOrderBy(orderBy)
			.first();

		if (row == null)
			return 0;
		Integer id = (Integer) row.get_Value(I_AIG_Provider_Access.COLUMNNAME_AIG_Prompt_Config_ID);
		return id != null ? id : 0;
	}

	/**
	 * Step 2: find the client-wide default prompt config
	 * ({@code IsDefault='Y'}, {@code AIGStatus='A'}).
	 *
	 * <p>Tenant-specific record takes precedence over system-level (AD_Client_ID=0).
	 */
	private static int lookupClientDefault(Properties ctx) {
		MAIPromptConfig config = new Query(ctx, MAIPromptConfig.Table_Name,
			"IsDefault='Y' AND AIGStatus=? AND IsActive='Y' AND AD_Client_ID IN (0,?)", null)
			.setParameters(X_AIG_Prompt_Config.AIGSTATUS_Active, Env.getAD_Client_ID(ctx))
			.setOrderBy("AD_Client_ID DESC")
			.first();
		return config != null ? config.getAIG_Prompt_Config_ID() : 0;
	}
}
