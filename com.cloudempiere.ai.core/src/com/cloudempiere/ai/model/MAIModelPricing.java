package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.util.Properties;

/**
 * Model class for AIG_ModelPricing
 */
public class MAIModelPricing extends X_AIG_ModelPricing {

	private static final long serialVersionUID = -1L;

	public MAIModelPricing(Properties ctx, int AIG_ModelPricing_ID, String trxName) {
		super(ctx, AIG_ModelPricing_ID, trxName);
	}

	public MAIModelPricing(Properties ctx, int AIG_ModelPricing_ID, String trxName, String... virtualColumns) {
		super(ctx, AIG_ModelPricing_ID, trxName, virtualColumns);
	}

	public MAIModelPricing(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
