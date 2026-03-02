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
package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.PO;
import org.compiere.model.Query;
import org.idempiere.cache.ImmutablePOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 * Model class for AIG_Provider_Access.
 * Mirrors MDashboardAccess - user-level rows take precedence over role-level rows.
 *
 * @author Cloudempiere
 * @see ADR-058
 */
public class MAIProviderAccess extends X_AIG_Provider_Access implements ImmutablePOSupport {

	private static final long serialVersionUID = 20260227L;

	/** Cache */
	private static ImmutablePOCache<String, MAIProviderAccess> s_cache =
			new ImmutablePOCache<>(Table_Name, 100);

	public MAIProviderAccess(Properties ctx, int AIG_Provider_Access_ID, String trxName) {
		super(ctx, AIG_Provider_Access_ID, trxName);
	}

	public MAIProviderAccess(Properties ctx, int AIG_Provider_Access_ID, String trxName, String... virtualColumns) {
		super(ctx, AIG_Provider_Access_ID, trxName, virtualColumns);
	}

	public MAIProviderAccess(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get access record for a specific provider and role.
	 */
	public static MAIProviderAccess getByRole(Properties ctx, int AIG_Provider_ID, int AD_Role_ID, String trxName) {
		return get(ctx, AIG_Provider_ID, AD_Role_ID, 0, trxName);
	}

	/**
	 * Get access record for a specific provider and user.
	 */
	public static MAIProviderAccess getByUser(Properties ctx, int AIG_Provider_ID, int AD_User_ID, String trxName) {
		return get(ctx, AIG_Provider_ID, 0, AD_User_ID, trxName);
	}

	/**
	 * Get access record for a specific provider, role and/or user.
	 * User-level row takes precedence over role-level row (ORDER BY AD_User_ID DESC).
	 *
	 * @param ctx context
	 * @param AIG_Provider_ID mandatory provider ID
	 * @param AD_Role_ID role ID (0 to skip)
	 * @param AD_User_ID user ID (0 to skip)
	 * @param trxName transaction name
	 * @return matching access record, or null if none found
	 */
	public static MAIProviderAccess get(Properties ctx, int AIG_Provider_ID, int AD_Role_ID, int AD_User_ID, String trxName) {
		if (AIG_Provider_ID <= 0 || (AD_Role_ID <= 0 && AD_User_ID <= 0))
			return null;

		String cacheKey = AIG_Provider_ID + "_" + AD_Role_ID + "_" + AD_User_ID;
		MAIProviderAccess result = s_cache.get(cacheKey);

		if (result == null) {
			StringBuilder whereClause = new StringBuilder("AIG_Provider_ID = ? ");
			List<Object> parameters = new ArrayList<>();
			parameters.add(AIG_Provider_ID);

			if (AD_User_ID > 0 && AD_Role_ID > 0) {
				whereClause.append("AND (AD_User_ID = ? OR AD_Role_ID = ?) ");
				parameters.add(AD_User_ID);
				parameters.add(AD_Role_ID);
			} else if (AD_User_ID > 0) {
				whereClause.append("AND AD_User_ID = ? ");
				parameters.add(AD_User_ID);
			} else {
				whereClause.append("AND AD_Role_ID = ? ");
				parameters.add(AD_Role_ID);
			}

			result = new Query(ctx, Table_Name, whereClause.toString(), trxName)
					.setParameters(parameters.toArray())
					.setOrderBy("AD_User_ID DESC") // User-level takes precedence over role-level
					.setOnlyActiveRecords(true)
					.setClient_ID()
					.first();

			if (result != null)
				s_cache.put(cacheKey, result);
		}

		return result;
	}

	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;
		makeImmutable();
		return this;
	}
}
