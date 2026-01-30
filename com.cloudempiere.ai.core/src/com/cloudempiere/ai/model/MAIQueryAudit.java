package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MAIQueryAudit extends X_AIG_QueryAudit {

	private static final long serialVersionUID = 6355045964594293096L;

	public MAIQueryAudit(Properties ctx, int AIG_QueryAudit_ID, String trxName) {
		super(ctx, AIG_QueryAudit_ID, trxName);
	}

	public MAIQueryAudit(Properties ctx, int AIG_QueryAudit_ID, String trxName, String... virtualColumns) {
		super(ctx, AIG_QueryAudit_ID, trxName, virtualColumns);
	}

	public MAIQueryAudit(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
