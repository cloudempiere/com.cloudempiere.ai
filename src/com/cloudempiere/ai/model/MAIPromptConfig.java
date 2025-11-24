package com.cloudempiere.ai.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * Model class for AIG_Prompt_Config
 */
public class MAIPromptConfig extends X_AIG_Prompt_Config {

	private static final long serialVersionUID = -3589648434948148436L;

	public MAIPromptConfig(Properties ctx, int AIG_Prompt_Config_ID, String trxName) {
		super(ctx, AIG_Prompt_Config_ID, trxName);
	}

	public MAIPromptConfig(Properties ctx, int AIG_Prompt_Config_ID, String trxName, String... virtualColumns) {
		super(ctx, AIG_Prompt_Config_ID, trxName, virtualColumns);
	}

	public MAIPromptConfig(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get prompt configuration by prompt key
	 * @param ctx context
	 * @param promptKey the prompt key (e.g., "SYSTEM")
	 * @param trxName transaction
	 * @return prompt configuration or null if not found
	 */
	public static MAIPromptConfig getByPromptKey(Properties ctx, String promptKey, String trxName) {
		if (promptKey == null || promptKey.trim().isEmpty()) {
			return null;
		}

		int clientId = Env.getAD_Client_ID(ctx);

		String whereClause = COLUMNNAME_AIGPromptKey + "=? AND " +
							COLUMNNAME_AD_Client_ID + "=?";

		return new Query(ctx, Table_Name, whereClause, trxName)
			.setParameters(promptKey, clientId)
			.setOnlyActiveRecords(true)
			.first();
	}

	/**
	 * Get prompt text by prompt key
	 * @param ctx context
	 * @param promptKey the prompt key (e.g., "SYSTEM")
	 * @param trxName transaction
	 * @return prompt text or null if not found
	 */
	public static String getPromptText(Properties ctx, String promptKey, String trxName) {
		MAIPromptConfig config = getByPromptKey(ctx, promptKey, trxName);
		return config != null ? config.getAIGPromptText() : null;
	}

}
