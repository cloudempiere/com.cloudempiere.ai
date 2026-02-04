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

// TEMPORARILY DISABLED - ChatAccessService will be implemented in future phase (ADR-036)
// import com.cloudempiere.ai.service.ChatAccessService;
// import com.cloudempiere.ai.service.IChatAccessService.ChatAccess;

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
	 * Get or Create Global AI Chat for current user in CURRENT TENANT
	 * <p>
	 * TENANT ISOLATION (ADR-036, CLD-1606):
	 * - Each tenant has its own global chats per user
	 * - System users (Client_ID=0) get separate chats per tenant they log into
	 * - Non-system users only see their own tenant's chats
	 * - Database constraint: UNIQUE(AD_Client_ID, AD_Table_ID, Record_ID) enforces isolation
	 * <p>
	 * MIGRATION NOTE: Requires migration script CLD-1606 which updates cm_chat_record constraint
	 * to include AD_Client_ID for proper multi-tenancy support.
	 *
	 * @param ctx context (must contain valid AD_Client_ID and AD_User_ID)
	 * @param trxName transaction
	 * @return AI Chat instance for current user in current tenant
	 */
	public static MAIChat getOrCreateGlobalChat(Properties ctx, String trxName) {
		return getOrCreateGlobalChat(ctx, trxName, false);
	}

	/**
	 * Get or Create Global AI Chat for current user in CURRENT TENANT
	 * <p>
	 * TENANT ISOLATION (ADR-036, CLD-1606):
	 * - Each tenant has its own global chats per user
	 * - System users (Client_ID=0) get separate chats per tenant they log into
	 * - Database constraint: UNIQUE(AD_Client_ID, AD_Table_ID, Record_ID) enforces isolation
	 * <p>
	 * LAZY CREATION: When dontCreate=true, returns null instead of creating new chat.
	 * This prevents unnecessary chat creation during widget initialization.
	 *
	 * @param ctx context (must contain valid AD_Client_ID and AD_User_ID)
	 * @param trxName transaction
	 * @param dontCreate if true, returns null instead of creating new chat (lazy creation)
	 * @return AI Chat instance for current user in current tenant, or null if not found and dontCreate is true
	 */
	public static MAIChat getOrCreateGlobalChat(Properties ctx, String trxName, boolean dontCreate) {
		int userId = Env.getAD_User_ID(ctx);
		int clientId = Env.getAD_Client_ID(ctx);

		// Query by table+record+client (constraint ensures uniqueness per tenant)
		String whereClause = "AD_Table_ID=? AND Record_ID=? AND AD_Client_ID=?";
		MChat chat = new org.compiere.model.Query(ctx, MChat.Table_Name, whereClause, null)
			.setParameters(AI_GLOBAL_TABLE_ID, userId, clientId)
			.first();

		if (chat != null) {
			return new MAIChat(ctx, chat.get_ID(), trxName);
		}

		// Lazy creation: return null if chat doesn't exist and creation disabled
		if (dontCreate) {
			return null;
		}

		// Create new global AI chat for this user in current tenant
		// Record_ID = userId makes it user-specific
		// AD_Client_ID makes it tenant-specific (enforced by constraint)
		MAIChat newChat = new MAIChat(ctx, AI_GLOBAL_TABLE_ID, userId,
			"AI Assistant - " + Env.getContext(ctx, "#AD_User_Name"), trxName);
		newChat.setAD_Client_ID(clientId);
		newChat.setConfidentialType(CONFIDENTIALTYPE_PrivateInformation); // Private by default (ADR-036)
		newChat.saveEx();
		return newChat;
	}

	/**
	 * Get or Create AI Chat for specific record in CURRENT TENANT
	 * <p>
	 * TENANT ISOLATION (CLD-1606):
	 * - Each tenant has separate context chats for the same record
	 * - Database constraint: UNIQUE(AD_Client_ID, AD_Table_ID, Record_ID) enforces isolation
	 * - Same Record_ID in different tenants = different chats
	 *
	 * @param ctx context (must contain valid AD_Client_ID)
	 * @param AD_Table_ID table
	 * @param Record_ID record
	 * @param description description
	 * @param trxName transaction
	 * @return AI Chat instance for current tenant
	 */
	public static MAIChat getOrCreateContextChat(Properties ctx, int AD_Table_ID,
			int Record_ID, String description, String trxName) {

		int clientId = Env.getAD_Client_ID(ctx);

		// Query by table+record+client (constraint ensures uniqueness per tenant)
		String whereClause = "AD_Table_ID=? AND Record_ID=? AND AD_Client_ID=?";
		MChat existingChat = new org.compiere.model.Query(ctx, MChat.Table_Name, whereClause, trxName)
			.setParameters(AD_Table_ID, Record_ID, clientId)
			.first();

		if (existingChat != null) {
			return new MAIChat(ctx, existingChat.get_ID(), trxName);
		}

		// Create new context chat in current tenant
		MAIChat chat = new MAIChat(ctx, AD_Table_ID, Record_ID, description, trxName);
		chat.setAD_Client_ID(clientId);
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

	// ===================================================================
	// ACCESS CONTROL METHODS
	// TEMPORARILY DISABLED - ChatAccessService will be implemented in future phase (ADR-036)
	// ===================================================================

	/*
	/**
	 * Get the current user's access level to this chat
	 * @return ChatAccess level (NONE, READ, WRITE, or OWNER)
	 */
	/*
	public ChatAccess getAccess() {
		return ChatAccessService.get().getAccess(getCtx(), this);
	}

	/**
	 * Check if current user can read this chat
	 * @return true if user has READ, WRITE, or OWNER access
	 */
	/*
	public boolean canRead() {
		return ChatAccessService.get().canRead(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Check if current user can write to this chat (add messages)
	 * @return true if user has WRITE or OWNER access
	 */
	/*
	public boolean canWrite() {
		return ChatAccessService.get().canWrite(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Check if current user can share this chat with others
	 * @return true if user has OWNER access
	 */
	/*
	public boolean canShare() {
		return ChatAccessService.get().canShare(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Check if current user can delete this chat
	 * @return true if user has OWNER access
	 */
	/*
	public boolean canDelete() {
		return ChatAccessService.get().canDelete(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Check if current user is the owner of this chat
	 * @return true if user has OWNER access
	 */
	/*
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
	/*
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
	/*
	public MAIChatOwnership shareWithRole(int AD_Role_ID, ChatAccess access) {
		return ChatAccessService.get().shareWithRole(getCtx(), get_ID(), AD_Role_ID, access, get_TrxName());
	}

	/**
	 * Get list of ownership records for this chat (who has access)
	 * @return list of ownership records (empty if user cannot view sharing info)
	 */
	/*
	public List<MAIChatOwnership> getSharingInfo() {
		return ChatAccessService.get().getSharingInfo(getCtx(), get_ID(), get_TrxName());
	}
	*/

	/**
	 * Get or Create Private Context Chat for CURRENT USER
	 * <p>
	 * Each user gets their own private chat for the same record.
	 * Per ADR-036: Multiple users can have separate private chats about same record.
	 * <p>
	 * NOTE: This method uses CreatedBy to distinguish between users since
	 * the cm_chat_record constraint doesn't support multiple chats per (client, table, record).
	 * Consider using record-specific chat IDs or extending the schema if this pattern
	 * is heavily used.
	 *
	 * @param ctx context
	 * @param AD_Table_ID table
	 * @param Record_ID record
	 * @param description description
	 * @param trxName transaction
	 * @return AI Chat instance for current user in current tenant
	 */
	public static MAIChat getOrCreatePrivateContextChat(Properties ctx, int AD_Table_ID,
			int Record_ID, String description, String trxName) {

		int userId = Env.getAD_User_ID(ctx);
		int clientId = Env.getAD_Client_ID(ctx);

		// Query includes CreatedBy to get user's own private chat
		// AND AD_Client_ID for tenant isolation
		String whereClause = "AD_Table_ID=? AND Record_ID=? AND CreatedBy=? AND ConfidentialType='P' AND AD_Client_ID=?";
		MChat chat = new org.compiere.model.Query(ctx, MChat.Table_Name, whereClause, trxName)
				.setParameters(AD_Table_ID, Record_ID, userId, clientId)
				.first();

		if (chat != null) {
			return new MAIChat(ctx, chat.get_ID(), trxName);
		}

		// Create new private chat
		MAIChat newChat = new MAIChat(ctx, AD_Table_ID, Record_ID, description, trxName);
		newChat.setAD_Client_ID(clientId);
		newChat.setConfidentialType(CONFIDENTIALTYPE_PrivateInformation);  // Private
		newChat.saveEx();
		return newChat;
	}

	/**
	 * Get or Create Shared Context Chat (visible to all with record access)
	 * <p>
	 * Per ADR-036: One shared chat per record per tenant, internal visibility.
	 * Database constraint ensures only one chat per (client, table, record).
	 *
	 * @param ctx context
	 * @param AD_Table_ID table
	 * @param Record_ID record
	 * @param description description
	 * @param trxName transaction
	 * @return Shared AI Chat instance for current tenant
	 */
	public static MAIChat getOrCreateSharedContextChat(Properties ctx, int AD_Table_ID,
			int Record_ID, String description, String trxName) {

		int clientId = Env.getAD_Client_ID(ctx);

		// Shared context chat - one per record per tenant, internal visibility
		String whereClause = "AD_Table_ID=? AND Record_ID=? AND ConfidentialType='I' AND AD_Client_ID=?";
		MChat chat = new org.compiere.model.Query(ctx, MChat.Table_Name, whereClause, trxName)
				.setParameters(AD_Table_ID, Record_ID, clientId)
				.first();

		if (chat != null) {
			return new MAIChat(ctx, chat.get_ID(), trxName);
		}

		MAIChat newChat = new MAIChat(ctx, AD_Table_ID, Record_ID, description, trxName);
		newChat.setAD_Client_ID(clientId);
		newChat.setConfidentialType(CONFIDENTIALTYPE_Internal);  // Internal - org can see
		newChat.saveEx();
		return newChat;
	}
}
