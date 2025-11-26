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
	 * <p>
	 * IMPORTANT: This method ensures proper tenant isolation for system users.
	 * System users (AD_Client_ID=0) will have separate chats for each tenant
	 * they log into, preventing data leakage across tenants.
	 *
	 * @param ctx context
	 * @param trxName transaction
	 * @return AI Chat instance
	 */
	public static MAIChat getOrCreateGlobalChat(Properties ctx, String trxName) {
		int userId = Env.getAD_User_ID(ctx);
		int contextClientId = Env.getAD_Client_ID(ctx); // Current login tenant

		// Try to find existing global AI chat for this user in this tenant
		// CRITICAL: Filter by both userId and contextClientId to ensure tenant isolation
		int chatId = getGlobalChatID(ctx, AI_GLOBAL_TABLE_ID, userId, contextClientId);

		if (chatId > 0) {
			return new MAIChat(ctx, chatId, trxName);
		}

		// Create new global AI chat for this user in this tenant
		MAIChat chat = new MAIChat(ctx, "AI Assistant - " + Env.getContext(ctx, "#AD_User_Name"), trxName);
		// Explicitly set AD_Client_ID to current context (important for system users)
		chat.setAD_Client_ID(contextClientId);
		chat.saveEx();
		return chat;
	}

	/**
	 * Get Global Chat ID for user in specific tenant
	 * <p>
	 * This method replaces MChat.getID() to add tenant filtering.
	 * System users (AD_Client_ID=0) need separate chats per tenant.
	 *
	 * @param ctx context
	 * @param AD_Table_ID table ID (should be AD_User table)
	 * @param Record_ID record ID (user ID)
	 * @param AD_Client_ID client ID (tenant)
	 * @return CM_Chat_ID or 0 if not found
	 */
	private static int getGlobalChatID(Properties ctx, int AD_Table_ID,
			int Record_ID, int AD_Client_ID) {
		// Use Query to filter by AD_Table_ID, Record_ID, and AD_Client_ID
		String whereClause = "AD_Table_ID=? AND Record_ID=? AND AD_Client_ID=?";
		MChat chat = new org.compiere.model.Query(ctx, MChat.Table_Name, whereClause, null)
			.setParameters(AD_Table_ID, Record_ID, AD_Client_ID)
			.setOrderBy("Created DESC") // Get most recent if multiple exist
			.first();

		return chat != null ? chat.get_ID() : 0;
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
}
