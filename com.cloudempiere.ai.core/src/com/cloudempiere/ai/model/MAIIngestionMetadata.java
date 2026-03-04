package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.util.Properties;

/**
 * Model class for AIG_IngestionMetadata
 */
public class MAIIngestionMetadata extends X_AIG_IngestionMetadata {

	private static final long serialVersionUID = -1L;

	public MAIIngestionMetadata(Properties ctx, int AIG_IngestionMetadata_ID, String trxName) {
		super(ctx, AIG_IngestionMetadata_ID, trxName);
	}

	public MAIIngestionMetadata(Properties ctx, int AIG_IngestionMetadata_ID, String trxName, String... virtualColumns) {
		super(ctx, AIG_IngestionMetadata_ID, trxName, virtualColumns);
	}

	public MAIIngestionMetadata(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
