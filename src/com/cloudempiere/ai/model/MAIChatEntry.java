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

import org.compiere.model.MChat;
import org.compiere.model.MChatEntry;

/**
 * AI Chat Entry Model - extends standard iDempiere Chat Entry
 * Adds AI-specific metadata and role identification
 *
 * @author Cloudempiere
 */
public class MAIChatEntry extends MChatEntry {

	private static final long serialVersionUID = 1L;

	/**
	 * Role constants for AI provider message format
	 * These are used when sending conversation history to AI providers
	 */
	public static final String ROLE_USER = "user";
	public static final String ROLE_ASSISTANT = "assistant";
	public static final String ROLE_SYSTEM = "system";

	/** System User ID for AI responses (from AIG_Provider) */
	private static Integer AI_SYSTEM_USER_ID = null;

	/**
	 * Standard Constructor
	 * @param ctx context
	 * @param CM_ChatEntry_ID id
	 * @param trxName transaction
	 */
	public MAIChatEntry(Properties ctx, int CM_ChatEntry_ID, String trxName) {
		super(ctx, CM_ChatEntry_ID, trxName);
	}

	/**
	 * Load Constructor
	 * @param ctx context
	 * @param rs result set
	 * @param trxName transaction
	 */
	public MAIChatEntry(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Parent Constructor - for user messages
	 * Creates a user message with current logged-in user as creator.
	 *
	 * @param chat parent chat
	 * @param data message text
	 */
	public MAIChatEntry(MChat chat, String data) {
		super(chat, data);
		// Explicitly set AD_User_ID to current logged-in user
		int currentUser = org.compiere.util.Env.getAD_User_ID(chat.getCtx());
		if (currentUser > 0) {
			setAD_User_ID(currentUser);
		}
	}

	/**
	 * Create AI Response Entry
	 * Sets AD_User_ID to the AI user from the provider configuration
	 *
	 * @param chat parent chat
	 * @param data response text
	 * @return AI chat entry with AD_User_ID set to AI user
	 */
	public static MAIChatEntry createAIResponse(MChat chat, String data) {
		Properties ctx = chat.getCtx();
		String trxName = chat.get_TrxName();

		MAIChatEntry entry = new MAIChatEntry(ctx, 0, trxName);
		entry.setCM_Chat_ID(chat.getCM_Chat_ID());
		entry.setConfidentialType(chat.getConfidentialType());
		entry.setCharacterData(data);
		entry.setChatEntryType(CHATENTRYTYPE_NoteFlat);

		// Set AD_User_ID to AI system user from provider
		Integer aiUserId = getAISystemUserId(ctx);
		if (aiUserId != null && aiUserId > 0) {
			entry.setAD_User_ID(aiUserId);
		}

		return entry;
	}

	/**
	 * Create System Message Entry (for context, instructions)
	 * @param chat parent chat
	 * @param data system message text
	 * @return system chat entry
	 */
	public static MAIChatEntry createSystemMessage(MChat chat, String data) {
		MAIChatEntry entry = new MAIChatEntry(chat, data);
		return entry;
	}

	/**
	 * Get AI System User ID from the provider configuration
	 * @param ctx context
	 * @return user ID or null
	 */
	private static Integer getAISystemUserId(Properties ctx) {
		if (AI_SYSTEM_USER_ID == null) {
			// Get AI user from the default provider (ID 1000001)
			// FIXME: Should get this from chat's associated provider instead of hardcoding
			MAIProvider provider = new MAIProvider(ctx, 1000001, null); // FIXME: hardcoded ID
			if (provider.getAIG_Provider_ID() > 0 && provider.getAD_User_ID() > 0) {
				AI_SYSTEM_USER_ID = provider.getAD_User_ID();
			} else {
				// Fallback: Use a system user marker (0 = System)
				AI_SYSTEM_USER_ID = 0;
			}
		}
		return AI_SYSTEM_USER_ID;
	}

	/**
	 * Check if this entry is an AI response
	 * Determined by comparing CreatedBy with the AI user from the provider
	 *
	 * @return true if this is an AI response, false if user message
	 */
	public boolean isAIResponse() {
		Integer aiUserId = getAISystemUserId(getCtx());
		if (aiUserId == null || aiUserId <= 0) {
			return false;
		}
		return getAD_User_ID() == aiUserId;
	}

	/**
	 * Check if this is a user message
	 * @return true if user message (not AI)
	 */
	public boolean isUserMessage() {
		return !isAIResponse();
	}
}
