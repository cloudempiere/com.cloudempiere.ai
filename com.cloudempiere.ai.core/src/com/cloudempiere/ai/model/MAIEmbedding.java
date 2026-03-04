package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.util.Properties;

/**
 * Model class for AIG_Embedding
 */
public class MAIEmbedding extends X_AIG_Embedding {

	private static final long serialVersionUID = -1L;

	public MAIEmbedding(Properties ctx, int AIG_Embedding_ID, String trxName) {
		super(ctx, AIG_Embedding_ID, trxName);
	}

	public MAIEmbedding(Properties ctx, int AIG_Embedding_ID, String trxName, String... virtualColumns) {
		super(ctx, AIG_Embedding_ID, trxName, virtualColumns);
	}

	public MAIEmbedding(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
