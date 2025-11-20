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

import org.compiere.model.I_AD_User;
import org.compiere.model.MChat;
import org.compiere.util.Env;

/**
 * AI Chat Model - extends standard iDempiere Chat
 * Adds AI-specific metadata and functionality
 *
 * @author Cloudempiere
 */
public class MAIChat extends MChat {

	private static final long serialVersionUID = 1L;

	/** Column name for AI Model */
	public static final String COLUMNNAME_AI_Model = "AI_Model";

	/** Column name for AI Provider */
	public static final String COLUMNNAME_AI_Provider = "AI_Provider";

	/** Column name for Total Tokens */
	public static final String COLUMNNAME_TotalTokens = "TotalTokens";

	/**
	 * AD_Table_ID for global AI chats (linked to AD_User table)
	 * Global AI chats are stored with AD_Table_ID = AD_User and Record_ID = user's AD_User_ID
	 */
	public static final int AI_GLOBAL_TABLE_ID = I_AD_User.Table_ID;

	/**
	 * Standard Constructor
	 * @param ctx context
	 * @param CM_Chat_ID id
	 * @param trxName transaction
	 */
	public MAIChat(Properties ctx, int CM_Chat_ID, String trxName) {
		super(ctx, CM_Chat_ID, trxName);
	}

	/**
	 * Load Constructor
	 * @param ctx context
	 * @param rs result set
	 * @param trxName transaction
	 */
	public MAIChat(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Create new AI Chat for current user
	 * @param ctx context
	 * @param description chat description
	 * @param trxName transaction
	 */
	public MAIChat(Properties ctx, String description, String trxName) {
		this(ctx, AI_GLOBAL_TABLE_ID, Env.getAD_User_ID(ctx), description, trxName);
	}

	/**
	 * Full Constructor - creates AI chat linked to specific record
	 * @param ctx context
	 * @param AD_Table_ID table (0 for global AI chat)
	 * @param Record_ID record (user ID for global chat)
	 * @param Description description
	 * @param trxName transaction
	 */
	public MAIChat(Properties ctx, int AD_Table_ID, int Record_ID,
			String Description, String trxName) {
		super(ctx, AD_Table_ID, Record_ID, Description, trxName);
	}

	/**
	 * Get or Create Global AI Chat for current user
	 * @param ctx context
	 * @param trxName transaction
	 * @return AI Chat instance
	 */
	public static MAIChat getOrCreateGlobalChat(Properties ctx, String trxName) {
		int userId = Env.getAD_User_ID(ctx);

		// Try to find existing global AI chat for this user
		int chatId = MChat.getID(AI_GLOBAL_TABLE_ID, userId);

		if (chatId > 0) {
			return new MAIChat(ctx, chatId, trxName);
		}

		// Create new global AI chat
		MAIChat chat = new MAIChat(ctx, "AI Assistant - " + Env.getContext(ctx, "#AD_User_Name"), trxName);
		chat.saveEx();
		return chat;
	}

	/**
	 * Get or Create AI Chat for specific record
	 * @param ctx context
	 * @param AD_Table_ID table
	 * @param Record_ID record
	 * @param description description
	 * @param trxName transaction
	 * @return AI Chat instance
	 */
	public static MAIChat getOrCreateContextChat(Properties ctx, int AD_Table_ID,
			int Record_ID, String description, String trxName) {

		int chatId = MChat.getID(AD_Table_ID, Record_ID);

		if (chatId > 0) {
			return new MAIChat(ctx, chatId, trxName);
		}

		MAIChat chat = new MAIChat(ctx, AD_Table_ID, Record_ID, description, trxName);
		chat.saveEx();
		return chat;
	}

	// ========== AI-Specific Getters/Setters ==========

	/**
	 * Set AI Model name
	 * @param model model name (e.g., "claude-sonnet-4", "gpt-4")
	 */
	public void setAIModel(String model) {
		set_Value(COLUMNNAME_AI_Model, model);
	}

	/**
	 * Get AI Model name
	 * @return model name
	 */
	public String getAIModel() {
		return (String) get_Value(COLUMNNAME_AI_Model);
	}

	/**
	 * Set AI Provider
	 * @param provider provider name (e.g., "anthropic", "openai", "aws-bedrock")
	 */
	public void setAIProvider(String provider) {
		set_Value(COLUMNNAME_AI_Provider, provider);
	}

	/**
	 * Get AI Provider
	 * @return provider name
	 */
	public String getAIProvider() {
		return (String) get_Value(COLUMNNAME_AI_Provider);
	}

	/**
	 * Set Total Tokens used in this chat
	 * @param tokens total tokens
	 */
	public void setTotalTokens(int tokens) {
		set_Value(COLUMNNAME_TotalTokens, tokens);
	}

	/**
	 * Get Total Tokens used
	 * @return total tokens
	 */
	public int getTotalTokens() {
		Integer tokens = (Integer) get_Value(COLUMNNAME_TotalTokens);
		return tokens != null ? tokens : 0;
	}

	/**
	 * Add tokens to the total count
	 * @param additionalTokens tokens to add
	 */
	public void addTokens(int additionalTokens) {
		setTotalTokens(getTotalTokens() + additionalTokens);
	}

	/**
	 * Check if this is a global AI chat (not linked to specific record)
	 * @return true if global chat
	 */
	public boolean isGlobalChat() {
		return getAD_Table_ID() == AI_GLOBAL_TABLE_ID;
	}

	/**
	 * Check if this chat has context (linked to specific record)
	 * @return true if has context
	 */
	public boolean hasContext() {
		return !isGlobalChat();
	}

	/**
	 * Get context description for display
	 * @return context description or null
	 */
	public String getContextDescription() {
		if (isGlobalChat()) {
			return null;
		}

		return getDescription();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("MAIChat[")
			.append(get_ID())
			.append(", Model=").append(getAIModel())
			.append(", Provider=").append(getAIProvider())
			.append(", Tokens=").append(getTotalTokens())
			.append(", Global=").append(isGlobalChat())
			.append("]");
		return sb.toString();
	}
}
