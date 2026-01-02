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
import java.util.List;
import java.util.Properties;

import org.compiere.model.I_AD_User;
import org.compiere.model.MChat;
import org.compiere.util.Env;

import com.cloudempiere.ai.service.ChatAccessService;
import com.cloudempiere.ai.service.IChatAccessService.ChatAccess;

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
	 * NOTE: Due to database constraint cm_chat_record UNIQUE(ad_table_id, record_id),
	 * only ONE global chat can exist per user across all tenants. This method
	 * returns the existing chat regardless of which tenant created it.
	 * <p>
	 * Chat entries (CM_ChatEntry) are filtered by AD_Client_ID for tenant isolation.
	 *
	 * @param ctx context
	 * @param trxName transaction
	 * @return AI Chat instance
	 */
	public static MAIChat getOrCreateGlobalChat(Properties ctx, String trxName) {
		int userId = Env.getAD_User_ID(ctx);

		// Check if ANY global AI chat exists for this user (regardless of tenant)
		// Database constraint cm_chat_record enforces uniqueness on (ad_table_id, record_id)
		int chatId = getGlobalChatID(ctx, AI_GLOBAL_TABLE_ID, userId);

		if (chatId > 0) {
			return new MAIChat(ctx, chatId, trxName);
		}

		// Create new global AI chat for this user
		int contextClientId = Env.getAD_Client_ID(ctx);
		MAIChat chat = new MAIChat(ctx, "AI Assistant - " + Env.getContext(ctx, "#AD_User_Name"), trxName);
		chat.setAD_Client_ID(contextClientId);
		chat.saveEx();
		return chat;
	}

	/**
	 * Get Global Chat ID for user
	 * <p>
	 * Searches across all tenants due to database constraint cm_chat_record
	 * which enforces uniqueness on (ad_table_id, record_id) only.
	 *
	 * @param ctx context
	 * @param AD_Table_ID table ID (should be AD_User table)
	 * @param Record_ID record ID (user ID)
	 * @return CM_Chat_ID or 0 if not found
	 */
	private static int getGlobalChatID(Properties ctx, int AD_Table_ID, int Record_ID) {
		// Query without AD_Client_ID filter - constraint only allows one per (table, record)
		String whereClause = "AD_Table_ID=? AND Record_ID=?";
		MChat chat = new org.compiere.model.Query(ctx, MChat.Table_Name, whereClause, null)
			.setParameters(AD_Table_ID, Record_ID)
			.setClient_ID() // Still respect client security for the query itself
			.first();

		// If not found with client filter, try without (for cross-tenant users)
		if (chat == null) {
			chat = new org.compiere.model.Query(Env.getCtx(), MChat.Table_Name, whereClause, null)
				.setParameters(AD_Table_ID, Record_ID)
				.first();
		}

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

	// ========================================================================
	// Access Control Methods (ADR-036)
	// ========================================================================

	/**
	 * Get the current user's access level to this chat
	 * @return ChatAccess level (NONE, READ, WRITE, or OWNER)
	 */
	public ChatAccess getAccess() {
		return ChatAccessService.get().getAccess(getCtx(), this);
	}

	/**
	 * Check if current user can read this chat
	 * @return true if user has READ, WRITE, or OWNER access
	 */
	public boolean canRead() {
		return ChatAccessService.get().canRead(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Check if current user can write to this chat (add messages)
	 * @return true if user has WRITE or OWNER access
	 */
	public boolean canWrite() {
		return ChatAccessService.get().canWrite(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Check if current user can share this chat with others
	 * @return true if user has OWNER access
	 */
	public boolean canShare() {
		return ChatAccessService.get().canShare(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Check if current user can delete this chat
	 * @return true if user has OWNER access
	 */
	public boolean canDelete() {
		return ChatAccessService.get().canDelete(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Check if current user is the owner of this chat
	 * @return true if user has OWNER access
	 */
	public boolean isOwner() {
		return getAccess() == ChatAccess.OWNER;
	}

	/**
	 * Share this chat with another user
	 * @param AD_User_ID user to share with
	 * @param access access level to grant (READ or WRITE)
	 * @return created ownership record
	 * @throws IllegalStateException if current user cannot share
	 */
	public MAIChatOwnership shareWith(int AD_User_ID, ChatAccess access) {
		return ChatAccessService.get().shareWithUser(getCtx(), get_ID(), AD_User_ID, access, get_TrxName());
	}

	/**
	 * Share this chat with a role
	 * @param AD_Role_ID role to share with
	 * @param access access level to grant
	 * @return created ownership record
	 * @throws IllegalStateException if current user cannot share
	 */
	public MAIChatOwnership shareWithRole(int AD_Role_ID, ChatAccess access) {
		return ChatAccessService.get().shareWithRole(getCtx(), get_ID(), AD_Role_ID, access, get_TrxName());
	}

	/**
	 * Get list of ownership records for this chat (who has access)
	 * @return list of ownership records (empty if user cannot view sharing info)
	 */
	public List<MAIChatOwnership> getSharingInfo() {
		return ChatAccessService.get().getSharingInfo(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Get or Create Private Context Chat for CURRENT USER
	 * Each user gets their own private chat for the same record.
	 * Per ADR-036: Multiple users can have separate private chats about same record.
	 *
	 * @param ctx context
	 * @param AD_Table_ID table
	 * @param Record_ID record
	 * @param description description
	 * @param trxName transaction
	 * @return AI Chat instance for current user
	 */
	public static MAIChat getOrCreatePrivateContextChat(Properties ctx, int AD_Table_ID,
			int Record_ID, String description, String trxName) {

		int userId = Env.getAD_User_ID(ctx);

		// Query includes CreatedBy to get user's own chat
		String whereClause = "AD_Table_ID=? AND Record_ID=? AND CreatedBy=? AND ConfidentialType='P'";
		MChat chat = new org.compiere.model.Query(ctx, MChat.Table_Name, whereClause, trxName)
				.setParameters(AD_Table_ID, Record_ID, userId)
				.setClient_ID()
				.first();

		if (chat != null) {
			return new MAIChat(ctx, chat.get_ID(), trxName);
		}

		// Create new private chat
		MAIChat newChat = new MAIChat(ctx, AD_Table_ID, Record_ID, description, trxName);
		newChat.setConfidentialType(CONFIDENTIALTYPE_PrivateInformation);  // Private
		newChat.saveEx();
		return newChat;
	}

	/**
	 * Get or Create Shared Context Chat (visible to all with record access)
	 * Per ADR-036: One shared chat per record, internal visibility.
	 *
	 * @param ctx context
	 * @param AD_Table_ID table
	 * @param Record_ID record
	 * @param description description
	 * @param trxName transaction
	 * @return Shared AI Chat instance
	 */
	public static MAIChat getOrCreateSharedContextChat(Properties ctx, int AD_Table_ID,
			int Record_ID, String description, String trxName) {

		// Shared context chat - one per record, internal visibility
		String whereClause = "AD_Table_ID=? AND Record_ID=? AND ConfidentialType='I'";
		MChat chat = new org.compiere.model.Query(ctx, MChat.Table_Name, whereClause, trxName)
				.setParameters(AD_Table_ID, Record_ID)
				.setClient_ID()
				.first();

		if (chat != null) {
			return new MAIChat(ctx, chat.get_ID(), trxName);
		}

		MAIChat newChat = new MAIChat(ctx, AD_Table_ID, Record_ID, description, trxName);
		newChat.setConfidentialType(CONFIDENTIALTYPE_Internal);  // Internal - org can see
		newChat.saveEx();
		return newChat;
	}
}
