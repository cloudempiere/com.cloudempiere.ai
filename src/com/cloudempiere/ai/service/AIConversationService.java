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
import org.compiere.util.Language;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.function.AIDatabaseFunctionHandler;
import com.cloudempiere.ai.model.MAIChat;
import com.cloudempiere.ai.model.MAIChatEntry;
import com.cloudempiere.ai.model.MAIPromptConfig;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.dto.AIFunction;
import com.cloudempiere.ai.provider.dto.AIFunctionCall;
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

	/** Maximum function call depth to prevent infinite loops */
	private static final int MAX_FUNCTION_CALL_DEPTH = 5;

	/** Provider factory instance */
	private AIProviderFactory providerFactory;

	/** Database function handler */
	private AIDatabaseFunctionHandler functionHandler;

	/**
	 * Default constructor
	 */
	public AIConversationService() {
		this.providerFactory = new AIProviderFactory();
		this.functionHandler = new AIDatabaseFunctionHandler();
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
		return sendMessageWithContext(ctx, chat, userMessage, null, DEFAULT_MAX_HISTORY, trxName);
	}

	/**
	 * Send message with full conversation history (backward compatibility)
	 * @param ctx context
	 * @param chat chat instance
	 * @param userMessage user's message
	 * @param maxHistoryEntries max entries to include (0 = no history)
	 * @param trxName transaction
	 * @return AI response
	 */
	public AIResponse sendMessageWithHistory(Properties ctx, MAIChat chat,
			String userMessage, int maxHistoryEntries, String trxName) {
		return sendMessageWithContext(ctx, chat, userMessage, null, maxHistoryEntries, trxName);
	}

	/**
	 * Send message with context and conversation history
	 * Now with database function calling support!
	 *
	 * @param ctx context
	 * @param chat chat instance
	 * @param userMessage user's message
	 * @param contextData optional window/tab context (can be null)
	 * @param maxHistoryEntries max entries to include (0 = no history)
	 * @param trxName transaction
	 * @return AI response
	 */
	public AIResponse sendMessageWithContext(
		Properties ctx,
		MAIChat chat,
		String userMessage,
		JSONObject contextData,
		int maxHistoryEntries,
		String trxName
	) {
		long startTime = System.currentTimeMillis();

		try {
			// Get AI provider
			IAIProvider provider = getProvider(ctx, DEFAULT_PROVIDER_ID, trxName);
			if (provider == null) {
				log.severe("AI Provider not found with ID: " + DEFAULT_PROVIDER_ID);
				return createErrorResponse("PROVIDER_NOT_FOUND",
					"AI Provider not configured. Please contact system administrator.",
					startTime);
			}

			// Build AI request
			AIRequest request = new AIRequest();
			List<AIMessage> messages = new ArrayList<>();

			// Add system message with context and database schema info
			String systemPrompt = buildSystemPrompt(ctx, contextData);
			request.setSystemPrompt(systemPrompt);
			log.fine("Including system prompt with database access instructions");

			// Add conversation history if requested
			if (maxHistoryEntries > 0) {
				List<AIMessage> history = buildConversationHistory(chat, maxHistoryEntries);
				messages.addAll(history);
			}

			// Add current user message
			messages.add(new AIMessage(MAIChatEntry.ROLE_USER, userMessage));

			request.setMessages(messages);

			// Check if provider supports function calling
			if (provider.supportsFunctionCalling()) {
				log.fine("Provider supports function calling - enabling database access");

				// Add database query function
				List<AIFunction> functions = new ArrayList<>();
				functions.add(AIDatabaseFunctionHandler.getDatabaseQueryFunction());

				// Call with functions
				AIResponse response = provider.generateTextWithFunctions(request, functions);

				// Check if AI wants to call database function
				if (response.getFunctionCalls() != null && !response.getFunctionCalls().isEmpty()) {
					log.fine("AI requested " + response.getFunctionCalls().size() + " function call(s)");
					// Process function calls recursively
					response = processFunctionCalls(ctx, response, request, provider, DEFAULT_PROVIDER_ID, 0);
				}

				// Add processing time
				long responseTime = System.currentTimeMillis() - startTime;
				response.setProcessingTimeMs(responseTime);

				return response;

			} else {
				// Fallback to regular text generation
				log.fine("Provider does not support function calling - using regular text generation");
				AIResponse response = provider.generateText(request);

				// Add processing time
				long responseTime = System.currentTimeMillis() - startTime;
				response.setProcessingTimeMs(responseTime);

				return response;
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "Error calling AI provider", e);
			return createErrorResponse("PROVIDER_ERROR",
				"Error communicating with AI service: " + e.getMessage(),
				startTime);
		}
	}

	/**
	 * Process function calls from AI and get final response
	 *
	 * @param ctx context
	 * @param initialResponse Initial response with function calls
	 * @param originalRequest Original AI request
	 * @param provider AI provider
	 * @param providerId Provider ID for audit
	 * @param depth Current recursion depth
	 * @return Final AI response incorporating function results
	 */
	private AIResponse processFunctionCalls(
		Properties ctx,
		AIResponse initialResponse,
		AIRequest originalRequest,
		IAIProvider provider,
		int providerId,
		int depth
	) throws Exception {

		// Prevent infinite loops
		if (depth >= MAX_FUNCTION_CALL_DEPTH) {
			log.warning("Max function call depth reached (" + MAX_FUNCTION_CALL_DEPTH + ") - stopping");
			return initialResponse;
		}

		List<AIMessage> messages = new ArrayList<>(originalRequest.getMessages());

		// Add AI's initial response with function call
		AIMessage assistantMsg = new AIMessage(MAIChatEntry.ROLE_ASSISTANT, initialResponse.getContent());
		assistantMsg.setFunctionCalls(initialResponse.getFunctionCalls());
		messages.add(assistantMsg);

		// Process each function call
		for (AIFunctionCall functionCall : initialResponse.getFunctionCalls()) {
			String functionName = functionCall.getName();
			String arguments = functionCall.getArguments();
			String toolUseId = functionCall.getId(); // Get the tool use ID from the function call

			log.fine("Processing function call: " + functionName + " (ID: " + toolUseId + ") with args: " + arguments);

			if ("query_database".equals(functionName)) {
				// Execute database query
				String functionResult = functionHandler.executeQueryFunction(ctx, providerId, arguments);

				log.fine("Function result: " + functionResult.substring(0, Math.min(200, functionResult.length())) + "...");

				// Add function result to conversation
				AIMessage functionMsg = new AIMessage("function", functionResult);
				// Store the tool use ID in functionName field (AWS Bedrock needs this to match results)
				functionMsg.setFunctionName(toolUseId != null ? toolUseId : functionName);
				messages.add(functionMsg);

			} else {
				log.warning("Unknown function call: " + functionName);
				// Add error message
				AIMessage errorMsg = new AIMessage("function",
					"{\"status\":\"ERROR\",\"error\":\"Unknown function: " + functionName + "\"}");
				// Store tool use ID for AWS Bedrock compatibility
				errorMsg.setFunctionName(toolUseId != null ? toolUseId : functionName);
				messages.add(errorMsg);
			}
		}

		// Build follow-up request with function results
		AIRequest followUpRequest = new AIRequest();
		followUpRequest.setMessages(messages);
		followUpRequest.setSystemPrompt(originalRequest.getSystemPrompt());
		followUpRequest.setTemperature(originalRequest.getTemperature());
		followUpRequest.setMaxTokens(originalRequest.getMaxTokens());

		// Log message sequence for debugging
		log.fine("Follow-up request has " + messages.size() + " messages");
		if (!messages.isEmpty()) {
			log.fine("First message role: " + messages.get(0).getRole());
		}

		// Get final response from AI (incorporating query results)
		List<AIFunction> functions = new ArrayList<>();
		functions.add(AIDatabaseFunctionHandler.getDatabaseQueryFunction());

		AIResponse finalResponse = provider.generateTextWithFunctions(followUpRequest, functions);

		// Check if AI wants to make another function call (iterative queries)
		if (finalResponse.getFunctionCalls() != null && !finalResponse.getFunctionCalls().isEmpty()) {
			log.fine("AI requested another function call (depth: " + (depth + 1) + ")");
			// Recursively process
			return processFunctionCalls(ctx, finalResponse, followUpRequest, provider, providerId, depth + 1);
		}

		return finalResponse;
	}

	/**
	 * Build system prompt with context and database access instructions
	 *
	 * @param ctx context
	 * @param contextData Optional context data
	 * @return System prompt string
	 */
	private String buildSystemPrompt(Properties ctx, JSONObject contextData) {
		// Try to load prompt from database first
		String dbPrompt = loadSystemPromptFromDatabase();

		// If database prompt exists, use it; otherwise fall back to hardcoded version
		if (dbPrompt != null && !dbPrompt.trim().isEmpty()) {
			log.fine("Using system prompt from database (AIG_Prompt_Config)");
			return buildPromptWithContext(ctx, dbPrompt, contextData);
		}

		log.fine("Using hardcoded system prompt (database configuration not found)");
		return buildHardcodedSystemPrompt(ctx, contextData);
	}

	/**
	 * Load system prompt from database
	 * @return prompt text or null if not found
	 */
	private String loadSystemPromptFromDatabase() {
		try {
			return MAIPromptConfig.getPromptText(Env.getCtx(), "SYSTEM", null);
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to load system prompt from database", e);
			return null;
		}
	}

	/**
	 * Build prompt with context appended
	 * @param ctx context
	 * @param basePrompt base prompt text
	 * @param contextData optional context data
	 * @return complete prompt with context
	 */
	private String buildPromptWithContext(Properties ctx, String basePrompt, JSONObject contextData) {
		StringBuilder sb = new StringBuilder();
		sb.append(basePrompt);

		// Add language instruction based on session language
		sb.append(buildLanguageInstruction(ctx));

		// Add context if available
		if (contextData != null && contextData.optBoolean("success", false)) {
			sb.append("\n\n## Current Context\n");
			sb.append(buildContextPrompt(contextData));
		}

		return sb.toString();
	}

	/**
	 * Build language instruction for AI based on user's session language
	 * @param ctx context
	 * @return language instruction text
	 */
	private String buildLanguageInstruction(Properties ctx) {
		try {
			String langCode = Env.getAD_Language(ctx);
			Language language = Language.getLanguage(langCode);

			if (language == null) {
				return ""; // No language instruction if language not found
			}

			String languageName = language.getName();
			String languageISO = language.getLanguageCode();

			// Build instruction
			StringBuilder sb = new StringBuilder();
			sb.append("\n\n## Language Requirement\n");
			sb.append("**IMPORTANT**: The user's session language is **").append(languageName);
			sb.append("** (ISO: ").append(languageISO).append(").\n\n");
			sb.append("You MUST respond in **").append(languageName).append("** language. ");
			sb.append("This includes:\n");
			sb.append("- All explanatory text\n");
			sb.append("- Error messages\n");
			sb.append("- Descriptions and summaries\n");
			sb.append("- Table headers and labels\n\n");
			sb.append("Note: Technical terms (SQL keywords, table names, column names) should remain in English, ");
			sb.append("but all descriptive text must be in ").append(languageName).append(".");

			return sb.toString();
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to build language instruction", e);
			return ""; // Return empty string on error
		}
	}

	/**
	 * Build hardcoded system prompt (fallback when database config not available)
	 * @param ctx context
	 * @param contextData optional context data
	 * @return hardcoded system prompt
	 */
	private String buildHardcodedSystemPrompt(Properties ctx, JSONObject contextData) {
		StringBuilder sb = new StringBuilder();

		sb.append("You are a helpful AI assistant for iDempiere ERP system. ");
		sb.append("You have access to query the database to answer user questions.\n\n");

		// Add database access instructions
		sb.append("## Database Access\n");
		sb.append("When users ask questions that require data from the system, ");
		sb.append("use the query_database function to retrieve the information.\n\n");

		sb.append("**When to use database queries:**\n");
		sb.append("- User asks about specific records (orders, products, customers, etc.)\n");
		sb.append("- User wants to see lists or summaries of data\n");
		sb.append("- User asks 'how many', 'show me', 'list', 'find', etc.\n");
		sb.append("- Questions about current state of business data\n\n");

		sb.append("**When NOT to use database queries:**\n");
		sb.append("- General questions about iDempiere features or concepts\n");
		sb.append("- How-to questions that don't need current data\n");
		sb.append("- Questions already answered by provided context\n\n");

		// Add common table information
		sb.append("## Common iDempiere Tables\n\n");
		sb.append("**Business Partners:**\n");
		sb.append("- C_BPartner: Business partners (customers, vendors)\n");
		sb.append("- AD_User: Users and contacts\n\n");

		sb.append("**Sales & Orders:**\n");
		sb.append("- C_Order: Sales and purchase orders\n");
		sb.append("- C_OrderLine: Order lines/items\n");
		sb.append("- C_Invoice: Invoices\n\n");

		sb.append("**Products:**\n");
		sb.append("- M_Product: Products and services\n");
		sb.append("- M_Product_Category: Product categories\n\n");

		sb.append("**Common Columns:**\n");
		sb.append("- Most tables have: IsActive, Created, Updated\n");
		sb.append("- Name, Value, Description are common descriptive fields\n");
		sb.append("- DocumentNo is used for document numbers\n\n");

		// Add language instruction
		sb.append(buildLanguageInstruction(ctx));

		// Add context if available
		if (contextData != null && contextData.optBoolean("success", false)) {
			sb.append("\n\n## Current Context\n");
			sb.append(buildContextPrompt(contextData));
		}

		// Add zoom link instructions
		sb.append("\n\n## Creating Record Links\n\n");
		sb.append("When mentioning specific database records in your responses, ");
		sb.append("create clickable links using this syntax:\n\n");
		sb.append("```\n");
		sb.append("[[TableName:RecordID|Display Text]]\n");
		sb.append("```\n\n");
		sb.append("**Examples:**\n");
		sb.append("- `[[C_BPartner:1000001|Acme Corporation]]` - Links to Business Partner record\n");
		sb.append("- `[[C_Order:1000523|Sales Order SO-1000523]]` - Links to Sales Order\n");
		sb.append("- `[[M_Product:1000100|Widget A]]` - Links to Product record\n");
		sb.append("- `[[R_Request:1041912|Request #1000014]]` - Links to Request record\n\n");
		sb.append("**When to create links:**\n");
		sb.append("- When you retrieve records from database queries\n");
		sb.append("- When mentioning specific business partners, orders, products, invoices, etc.\n");
		sb.append("- When the user asks about a specific record\n");
		sb.append("- Use the primary table name (e.g., C_Order, not C_OrderLine)\n");
		sb.append("- Use the primary key ID from the database query results\n\n");
		sb.append("**Display Text Guidelines:**\n");
		sb.append("- Use human-readable text (e.g., business partner name, document number)\n");
		sb.append("- Keep it concise but descriptive\n");
		sb.append("- Example: \"Sales Order SO-1000523\" instead of just \"1000523\"\n\n");

		sb.append("\n## Guidelines\n");
		sb.append("- Be conversational and helpful\n");
		sb.append("- When showing query results, format them clearly (use tables or lists)\n");
		sb.append("- If a query returns no results, suggest alternatives\n");
		sb.append("- Keep responses concise but informative\n");
		sb.append("- Always create clickable links for specific records you mention\n");

		return sb.toString();
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

		// AWS Bedrock requires conversation to start with USER message
		// If first message is not USER role, find the first USER message and start from there
		if (!history.isEmpty() && !MAIChatEntry.ROLE_USER.equals(history.get(0).getRole())) {
			log.warning("Conversation history starts with " + history.get(0).getRole() +
				" role. AWS Bedrock requires USER role first. Adjusting history...");

			// Find first USER message
			int firstUserIndex = -1;
			for (int i = 0; i < history.size(); i++) {
				if (MAIChatEntry.ROLE_USER.equals(history.get(i).getRole())) {
					firstUserIndex = i;
					break;
				}
			}

			if (firstUserIndex > 0) {
				// Remove messages before first USER message
				history = new ArrayList<>(history.subList(firstUserIndex, history.size()));
				log.fine("Removed " + firstUserIndex + " message(s) to ensure USER message is first");
			} else if (firstUserIndex == -1) {
				// No USER message found at all - this shouldn't happen but handle gracefully
				log.warning("No USER message found in history. Clearing history.");
				history.clear();
			}
		}

		log.fine("Built conversation history: " + history.size() + " messages" +
			(!history.isEmpty() ? " (first role: " + history.get(0).getRole() + ")" : ""));
		return history;
	}

	/**
	 * Build context prompt from JSON context data
	 * @param contextData context JSON
	 * @return formatted context prompt
	 */
	private String buildContextPrompt(JSONObject contextData) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are an AI assistant helping a user in an iDempiere ERP system.\n\n");
		prompt.append("Current Context:\n");

		// User context
		if (contextData.has("user_context")) {
			JSONObject userCtx = contextData.getJSONObject("user_context");
			prompt.append("- User: ").append(userCtx.optString("user_name", "Unknown")).append("\n");
			prompt.append("- Role: ").append(userCtx.optString("role_name", "Unknown")).append("\n");
			prompt.append("- Client: ").append(userCtx.optString("client_name", "Unknown")).append("\n");
			prompt.append("- Organization: ").append(userCtx.optString("org_name", "Unknown")).append("\n\n");
		}

		// Window context
		if (contextData.has("window_metadata")) {
			JSONObject windowMeta = contextData.getJSONObject("window_metadata");
			prompt.append("Window: ").append(windowMeta.optString("name", "Unknown")).append("\n");
			if (windowMeta.has("description") && !windowMeta.isNull("description")) {
				prompt.append("Description: ").append(windowMeta.getString("description")).append("\n");
			}
			prompt.append("\n");
		}

		// Tab context
		if (contextData.has("tab_context")) {
			JSONObject tabCtx = contextData.getJSONObject("tab_context");
			prompt.append("Current Tab: ").append(tabCtx.optString("tab_name", "Unknown")).append("\n");
			prompt.append("Table: ").append(tabCtx.optString("table_name", "Unknown")).append("\n");

			// Show selected record information
			int selectedRecordId = tabCtx.optInt("selected_record_id", -1);
			if (selectedRecordId > 0) {
				String keyColumn = tabCtx.optString("selected_record_key", "Record_ID");
				prompt.append("Selected Record: ").append(keyColumn).append(" = ").append(selectedRecordId).append("\n");
			}

			// Show current row position
			int currentRow = tabCtx.optInt("current_row", -1);
			if (currentRow >= 0) {
				prompt.append("Current Row Position: ").append(currentRow).append("\n");
			}

			// Generic Record_ID (for backwards compatibility)
			int recordId = tabCtx.optInt("record_id", -1);
			if (recordId > 0 && recordId != selectedRecordId) {
				prompt.append("Window Record ID: ").append(recordId).append("\n");
			}

			prompt.append("\n");
		}

		// Business record data (most important - current record business fields and values)
		if (contextData.has("record_data")) {
			JSONObject recordData = contextData.getJSONObject("record_data");
			if (recordData.length() > 0) {
				prompt.append("Current Record Data (Business Fields):\n");
				for (String key : recordData.keySet()) {
					Object value = recordData.get(key);
					if (value != null && !value.toString().trim().isEmpty()) {
						prompt.append("  ").append(key).append(": ").append(value).append("\n");
					}
				}
				prompt.append("\n");
			}
		}

		// Technical data (less important - available if needed but not primary focus)
		if (contextData.has("technical_data")) {
			JSONObject technicalData = contextData.getJSONObject("technical_data");
			if (technicalData.length() > 0) {
				prompt.append("Technical/System Fields (available for reference):\n");
				for (String key : technicalData.keySet()) {
					Object value = technicalData.get(key);
					if (value != null && !value.toString().trim().isEmpty()) {
						prompt.append("  ").append(key).append(": ").append(value).append("\n");
					}
				}
				prompt.append("\n");
			}
		}

		// Child tabs with data
		if (contextData.has("child_tabs")) {
			JSONArray childTabs = contextData.getJSONArray("child_tabs");
			if (childTabs.length() > 0) {
				prompt.append("Related Tabs (Sub-tabs):\n");
				for (int i = 0; i < childTabs.length(); i++) {
					JSONObject tab = childTabs.getJSONObject(i);
					String tabName = tab.optString("tab_name", "Unknown");
					String tableName = tab.optString("table_name", "");

					prompt.append("  ").append(i + 1).append(". ").append(tabName)
						.append(" (").append(tableName).append(")");

					// Add row count if available
					int rowCount = tab.optInt("row_count", -1);
					if (rowCount >= 0) {
						prompt.append(" - ").append(rowCount).append(" record(s)");
					}

					prompt.append("\n");

					// Add description if available
					if (tab.has("description") && !tab.isNull("description")) {
						String desc = tab.getString("description");
						if (desc != null && !desc.trim().isEmpty()) {
							prompt.append("     Description: ").append(desc).append("\n");
						}
					}

					// Add current record data if available
					if (tab.has("current_record_data")) {
						JSONObject recordData = tab.getJSONObject("current_record_data");
						if (recordData.length() > 0) {
							prompt.append("     Current record preview:\n");
							for (String key : recordData.keySet()) {
								Object value = recordData.get(key);
								if (value != null && !value.toString().trim().isEmpty()) {
									prompt.append("       - ").append(key).append(": ")
										.append(value).append("\n");
								}
							}
						}
					}
				}
				prompt.append("\n");
			}
		}

		prompt.append("Please provide helpful, context-aware assistance based on the above information. ");
		prompt.append("When relevant, reference the specific window, tab, or record data shown above. ");
		prompt.append("Keep responses concise and actionable.\n");

		return prompt.toString();
	}

	/**
	 * Create error response
	 * @param errorCode error code
	 * @param errorMessage error message
	 * @param startTime start time for processing time calculation
	 * @return error response
	 */
	private AIResponse createErrorResponse(String errorCode, String errorMessage, long startTime) {
		AIResponse response = new AIResponse();
		response.setErrorCode(errorCode);
		response.setErrorMessage(errorMessage);
		response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
		return response;
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