package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.util.Properties;

/**
 * Model class for AIG_Provider
 */
public class MAIProvider extends X_AIG_Provider {

	private static final long serialVersionUID = -8293759018862021378L;

	public MAIProvider(Properties ctx, int AIG_Provider_ID, String trxName) {
		super(ctx, AIG_Provider_ID, trxName);
	}

	public MAIProvider(Properties ctx, int AIG_Provider_ID, String trxName, String... virtualColumns) {
		super(ctx, AIG_Provider_ID, trxName, virtualColumns);
	}

	public MAIProvider(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
