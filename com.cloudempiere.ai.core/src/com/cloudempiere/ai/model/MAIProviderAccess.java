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
import java.util.Properties;

import org.compiere.model.PO;
import org.idempiere.cache.ImmutablePOSupport;

/**
 * Model class for AIG_Provider_Access.
 *
 * <p>Access grant rows linking a provider to a role or user.
 * Membership is queried via a subquery in {@link MAIProvider#getAccessibleProvider}.
 *
 * @author Cloudempiere
 * @see ADR-058
 */
public class MAIProviderAccess extends X_AIG_Provider_Access implements ImmutablePOSupport {

	private static final long serialVersionUID = 20260227L;

	public MAIProviderAccess(Properties ctx, int AIG_Provider_Access_ID, String trxName) {
		super(ctx, AIG_Provider_Access_ID, trxName);
	}

	public MAIProviderAccess(Properties ctx, int AIG_Provider_Access_ID, String trxName, String... virtualColumns) {
		super(ctx, AIG_Provider_Access_ID, trxName, virtualColumns);
	}

	public MAIProviderAccess(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (getAIG_Provider_ID() <= 0 && getAIG_Prompt_Config_ID() <= 0) {
			log.saveError("Error", "At least one of AI Provider or AI Prompt Config must be set");
			return false;
		}
		return true;
	}

	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;
		makeImmutable();
		return this;
	}
}
