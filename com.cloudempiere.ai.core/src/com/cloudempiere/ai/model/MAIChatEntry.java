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
import org.compiere.util.Env;

/**
 * AI Chat Entry Model - extends standard iDempiere Chat Entry
 * Adds AI-specific metadata and role identification
 *
 * @author Cloudempiere
 */
public class MAIChatEntry extends MChatEntry {

	private static final long serialVersionUID = 1L;

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
		int currentUser = Env.getAD_User_ID(chat.getCtx());
		if (currentUser > 0) {
			setAD_User_ID(currentUser);
		}
	}

	/**
	 * Create AI Response Entry with separate markdown and HTML.
	 * Sets AD_User_ID to the AI user from the given provider.
	 *
	 * <p>Storage strategy:
	 * <ul>
	 *   <li>CharacterData: Stores markdown (source format for AI API)</li>
	 *   <li>ContentHTML: Stores rendered HTML (for efficient UI display)</li>
	 * </ul>
	 *
	 * @param chat parent chat
	 * @param provider AI provider (may be null)
	 * @param markdown markdown text (source)
	 * @param html rendered HTML
	 * @return AI chat entry with AD_User_ID set to AI user
	 */
	public static MAIChatEntry createAIResponse(MChat chat, MAIProvider provider, String markdown, String html) {
		MAIChatEntry entry = new MAIChatEntry(chat.getCtx(), 0, chat.get_TrxName());
		entry.setCM_Chat_ID(chat.getCM_Chat_ID());
		entry.setConfidentialType(chat.getConfidentialType());
		entry.setCharacterData(markdown);
		entry.set_ValueOfColumn("ContentHTML", html);
		entry.setChatEntryType(CHATENTRYTYPE_NoteFlat);

		if (provider != null && provider.getAD_User_ID() > 0) {
			entry.setAD_User_ID(provider.getAD_User_ID());
		}

		return entry;
	}

	/**
	 * Check if this entry is an AI response.
	 * An entry is considered an AI response if its AD_User_ID differs from the current user.
	 *
	 * @return true if this is an AI response, false if user message
	 */
	public boolean isAIResponse() {
		int currentUser = Env.getAD_User_ID(getCtx());
		return getAD_User_ID() != currentUser;
	}

	/**
	 * Get the rendered HTML content
	 * @return HTML content or null if not set
	 */
	public String getContentHTML() {
		return (String) get_Value("ContentHTML");
	}
}
