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
package com.cloudempiere.ai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MChatEntry;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import com.cloudempiere.ai.model.MAIChat;
import com.cloudempiere.ai.model.MAIChatEntry;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.dto.AIMessage;
import com.cloudempiere.ai.provider.dto.AIRequest;
import com.cloudempiere.ai.provider.dto.AIResponse;
import com.cloudempiere.ai.provider.factory.AIProviderFactory;

/**
 * AI Conversation Service
 * Manages AI chat conversations, history, and provider integration
 *
 * @author Cloudempiere
 */
public class AIConversationService {

	private static final CLogger log = CLogger.getCLogger(AIConversationService.class);

	/** FIXME Hardcoded provider ID (temporary - until we add provider selection UI) */
	private static final int DEFAULT_PROVIDER_ID = 1000001;

	/** Default maximum history entries to include */
	private static final int DEFAULT_MAX_HISTORY = 10;

	/** Provider factory instance */
	private AIProviderFactory providerFactory;

	/**
	 * Default constructor
	 */
	public AIConversationService() {
		this.providerFactory = new AIProviderFactory();
	}

	/**
	 * Send message and get AI response with conversation history
	 * @param ctx context
	 * @param chat chat instance
	 * @param userMessage user's message
	 * @param trxName transaction
	 * @return AI response
	 */
	public AIResponse sendMessage(Properties ctx, MAIChat chat, String userMessage, String trxName) {
		return sendMessageWithHistory(ctx, chat, userMessage, DEFAULT_MAX_HISTORY, trxName);
	}

	/**
	 * Send message with full conversation history
	 * @param ctx context
	 * @param chat chat instance
	 * @param userMessage user's message
	 * @param maxHistoryEntries max entries to include (0 = no history)
	 * @param trxName transaction
	 * @return AI response
	 */
	public AIResponse sendMessageWithHistory(Properties ctx, MAIChat chat,
			String userMessage, int maxHistoryEntries, String trxName) {

		long startTime = System.currentTimeMillis();

		try {
			// Get AI provider
			IAIProvider provider = getProvider(ctx, DEFAULT_PROVIDER_ID, trxName);
			if (provider == null) {
				log.severe("AI Provider not found with ID: " + DEFAULT_PROVIDER_ID);
				AIResponse errorResponse = new AIResponse();
				errorResponse.setErrorMessage("AI Provider not configured. Please contact system administrator.");
				errorResponse.setErrorCode("PROVIDER_NOT_FOUND");
				return errorResponse;
			}

			// Build AI request
			AIRequest request = new AIRequest();

			// Add conversation history if requested
			if (maxHistoryEntries > 0) {
				List<AIMessage> history = buildConversationHistory(chat, maxHistoryEntries);
				request.setMessages(history);
			}

			// Add current user message
			AIMessage userMsg = new AIMessage(MAIChatEntry.ROLE_USER, userMessage);
			if (request.getMessages() == null) {
				request.setMessages(new ArrayList<>());
			}
			request.getMessages().add(userMsg);

			// Set request parameters (use provider defaults)
			// Temperature, max tokens, etc. can be configured per provider

			// Send request to AI provider
			log.fine("Sending message to AI provider: " + provider.getProviderName());
			AIResponse response = provider.generateText(request);

			// Add processing time
			long responseTime = System.currentTimeMillis() - startTime;
			response.setProcessingTimeMs(responseTime);

			if (response.isSuccess()) {
				log.fine("AI response received: " + response.getContent().length() + " chars, " +
					(response.getTokenUsage() != null ? response.getTokenUsage().getTotalTokens() : 0) + " tokens");
			} else {
				log.warning("AI response failed: " + response.getErrorMessage());
			}

			return response;

		} catch (Exception e) {
			log.log(Level.SEVERE, "Error calling AI provider", e);

			AIResponse errorResponse = new AIResponse();
			errorResponse.setErrorMessage("Error communicating with AI service: " + e.getMessage());
			errorResponse.setErrorCode("PROVIDER_ERROR");
			errorResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);
			return errorResponse;
		}
	}

	/**
	 * Build conversation history from chat entries
	 * @param chat chat instance
	 * @param maxEntries maximum entries to include
	 * @return list of AI messages
	 */
	private List<AIMessage> buildConversationHistory(MAIChat chat, int maxEntries) {
		List<AIMessage> history = new ArrayList<>();

		if (chat == null) {
			return history;
		}

		// Get all entries
		MChatEntry[] entries = chat.getEntries(true);

		if (entries == null || entries.length == 0) {
			return history;
		}

		// Get most recent entries (up to maxEntries)
		// Exclude the last entry as it will be the one we just added
		int startIndex = Math.max(0, entries.length - maxEntries - 1);
		int endIndex = entries.length - 1; // Don't include the message we just added

		for (int i = startIndex; i < endIndex && i < entries.length; i++) {
			MChatEntry entry = entries[i];

			if (!entry.isActive()) {
				continue;
			}

			// Skip entries with null or empty content
			String content = entry.getCharacterData();
			if (content == null || content.trim().isEmpty()) {
				log.fine("Skipping chat entry with empty content: " + entry.getCM_ChatEntry_ID());
				continue;
			}

			// Determine role based on AD_User_ID
			String role = MAIChatEntry.ROLE_USER;
			if (entry instanceof MAIChatEntry) {
				MAIChatEntry aiEntry = (MAIChatEntry) entry;
				// Check if message is from AI by comparing AD_User_ID with provider's user
				if (aiEntry.isAIResponse()) {
					role = MAIChatEntry.ROLE_ASSISTANT;
				}
			} else {
				// Fallback for regular MChatEntry: check if message is from different user
				int currentUser = Env.getAD_User_ID(chat.getCtx());
				if (entry.getCreatedBy() != currentUser) {
					role = MAIChatEntry.ROLE_ASSISTANT;
				}
			}

			AIMessage message = new AIMessage(role, content);

			history.add(message);
		}

		log.fine("Built conversation history: " + history.size() + " messages");
		return history;
	}

	/**
	 * Get AI provider instance
	 * @param ctx context
	 * @param providerId provider ID
	 * @param trxName transaction
	 * @return provider instance or null
	 */
	private IAIProvider getProvider(Properties ctx, int providerId, String trxName) {
		try {
			return providerFactory.get(ctx, providerId, trxName);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to get AI provider: " + providerId, e);
			return null;
		}
	}

	/**
	 * Get provider name for display
	 * @param providerId provider ID
	 * @return provider name or "Unknown"
	 */
	public String getProviderName(int providerId) {
		IAIProvider provider = getProvider(Env.getCtx(), providerId, null);
		return provider != null ? provider.getProviderName() : "Unknown";
	}

	/**
	 * Check if AI service is available
	 * @return true if provider is configured and ready
	 */
	public boolean isAvailable() {
		IAIProvider provider = getProvider(Env.getCtx(), DEFAULT_PROVIDER_ID, null);
		return provider != null;
	}

	/**
	 * Get default provider ID
	 * @return provider ID
	 */
	public int getDefaultProviderId() {
		return DEFAULT_PROVIDER_ID;
	}
}