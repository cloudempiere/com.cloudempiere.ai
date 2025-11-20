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
package com.cloudempiere.ai.component;

import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.model.MChat;
import org.compiere.model.MChatEntry;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import com.cloudempiere.ai.model.MAIChat;
import com.cloudempiere.ai.model.MAIChatEntry;
import com.cloudempiere.ai.provider.dto.AIResponse;
import com.cloudempiere.ai.service.AIConversationService;

/**
 * AI Chat Widget Component
 * Provides a simple chat interface for AI interactions
 * Persists conversation using CM_Chat and CM_ChatEntry tables
 *
 * @author Cloudempiere
 */
public class AIChatWidget extends Div implements EventListener<Event> {

	private static final long serialVersionUID = 1L;
	private static final CLogger log = CLogger.getCLogger(AIChatWidget.class);

	/** Messages container (scrollable) */
	private Vlayout messagesContainer;

	/** Input textbox */
	private Textbox inputBox;

	/** Send button */
	private Button sendButton;

	/** Clear chat button */
	private Button clearButton;

	/** Loading indicator */
	private Html loadingIndicator;

	/** Current chat instance */
	private MChat chat;

	/** Date formatter for timestamps */
	private SimpleDateFormat dateFormat;

	/** AI conversation service */
	private AIConversationService aiService;

	/** Maximum messages to display (for performance) */
	private static final int MAX_MESSAGES_DISPLAY = 100;

	/**
	 * Default Constructor
	 */
	public AIChatWidget() {
		super();
		init();
	}

	/**
	 * Initialize the widget
	 */
	private void init() {
		setSclass("ai-chat-widget");
		ZKUpdateUtil.setVflex(this, "1");
		ZKUpdateUtil.setHflex(this, "1");
		setStyle("display: flex; flex-direction: column; padding: 12px; background: #FDFDFD; border-radius: 12px;");

		dateFormat = DisplayType.getDateFormat(DisplayType.DateTime);

		// Initialize AI service
		aiService = new AIConversationService();

		// Messages area (scrollable)
		messagesContainer = new Vlayout();
		messagesContainer.setSclass("ai-messages");
		ZKUpdateUtil.setVflex(messagesContainer, "1");
		messagesContainer.setStyle("overflow-y: auto; overflow-x: hidden; margin-bottom: 12px; padding: 6px; gap: 8px;");
		appendChild(messagesContainer);

		// Loading indicator (hidden by default)
		loadingIndicator = new Html();
		loadingIndicator.setContent("<div class='ai-loading' style='display:none; padding: 12px 18px; text-align: left;'>" +
			"<div style='display: flex; align-items: center; gap: 8px;'>" +
			"<div style='width: 18px; height: 18px; border-radius: 27px; background: #E9EAEB;'></div>" +
			"<span style='font-family: Helvetica Neue; font-weight: 400; font-size: 12px; line-height: 18px; color: #717680;'>" +
			"<i>" + Msg.getMsg(Env.getCtx(), "AIThinking") + "</i></span></div></div>");
		messagesContainer.appendChild(loadingIndicator);

		// Input area
		Hlayout inputArea = new Hlayout();
		inputArea.setStyle("width: 100%; gap: 8px; align-items: center;");

		inputBox = new Textbox();
		inputBox.setPlaceholder(Msg.getMsg(Env.getCtx(), "AIChatPlaceholder"));
		ZKUpdateUtil.setHflex(inputBox, "1");
		inputBox.setRows(1);
		inputBox.setMultiline(false);
		inputBox.setStyle("border: 1px solid rgba(122, 128, 140, 0.32); border-radius: 22px; padding: 12px 18px; " +
			"font-size: 12px; line-height: 18px; color: #717680; background: #FFFFFF;");
		inputBox.addEventListener(Events.ON_OK, this); // Enter key

		sendButton = new Button();
		sendButton.addEventListener(Events.ON_CLICK, this);
		sendButton.setSclass("ai-send-btn");
		if (ThemeManager.isUseFontIconForImage())
			sendButton.setIconSclass("z-icon-Send-White");
		else
			sendButton.setImage(ThemeManager.getThemeResource("images/Send-White.png"));
		sendButton.setStyle("width: 42px; height: 42px; background: #181D27; border-radius: 100px; " +
			"display: flex; align-items: center; justify-content: center; border: none; cursor: pointer;");

		clearButton = new Button(Msg.getMsg(Env.getCtx(), "ClearChat"));
		clearButton.addEventListener(Events.ON_CLICK, this);
		clearButton.setSclass("ai-clear-btn");
		clearButton.setTooltiptext(Msg.getMsg(Env.getCtx(), "ClearChat"));

		inputArea.appendChild(inputBox);
		inputArea.appendChild(sendButton);
//		inputArea.appendChild(clearButton); // the clear chat button is disabled for now
		appendChild(inputArea);

		// Load or create chat
		loadOrCreateChat();
	}

	/**
	 * Load existing chat or create new one for current user
	 */
	private void loadOrCreateChat() {
		try {
			// Use MAIChat for AI-specific functionality
			chat = MAIChat.getOrCreateGlobalChat(Env.getCtx(), null);
			renderMessages();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to load/create chat", e);
		}
	}

	/**
	 * Render all messages from chat
	 */
	private void renderMessages() {
		// Clear existing messages (but keep loading indicator)
		messagesContainer.getChildren().stream()
			.filter(c -> c != loadingIndicator)
			.forEach(c -> c.detach());

		if (chat == null) {
			return;
		}

		MChatEntry[] entries = chat.getEntries(true);

		// Limit number of messages displayed for performance
		int startIndex = Math.max(0, entries.length - MAX_MESSAGES_DISPLAY);

		for (int i = startIndex; i < entries.length; i++) {
			MChatEntry entry = entries[i];
			if (entry.isActive()) {
				renderMessage(entry);
			}
		}

		// Scroll to bottom after rendering - use longer delay for initial load
		Clients.evalJavaScript(
			"setTimeout(function(){" +
			"var el=document.querySelector('.ai-messages');" +
			"if(el)el.scrollTop=el.scrollHeight;" +
			"}, 300);"
		);
	}

	/**
	 * Render a single message
	 * @param entry chat entry
	 */
	private void renderMessage(MChatEntry entry) {
		boolean isAI = isAIMessage(entry);

		// Add divider before message (except for first message)
		if (messagesContainer.getChildren().size() > 1) { // More than just loading indicator
			Html divider = new Html();
			divider.setContent("<div style='width: 100%; height: 0; border: 1px solid rgba(24, 29, 39, 0.12); margin: 0;'></div>");
			messagesContainer.insertBefore(divider, loadingIndicator);
		}

		Div msgDiv = new Div();
		msgDiv.setSclass(isAI ? "ai-message" : "user-message");

		// Updated styling based on Figma design screenshot (scaled to 12px base font)
		String baseStyle = "display: flex; flex-direction: column; gap: 12px; ";
		if (isAI) {
			msgDiv.setStyle(baseStyle + "padding: 12px 18px; background: transparent; border-radius: 0;");
		} else {
			// User message: gray bubble, more compact
			msgDiv.setStyle(baseStyle + "padding: 12px 15px; background: #E9EAEB; border-radius: 15px; " +
				"margin: 0 18px; max-width: 85%; align-self: flex-start;");
		}

		Html content = new Html();
		content.setContent(formatMessage(entry, isAI));
		msgDiv.appendChild(content);

		// Insert before loading indicator
		messagesContainer.insertBefore(msgDiv, loadingIndicator);
	}

	/**
	 * Format message HTML
	 * @param entry chat entry
	 * @param isAI true if AI message
	 * @return formatted HTML
	 */
	private String formatMessage(MChatEntry entry, boolean isAI) {
		StringBuilder sb = new StringBuilder();

		// Message header based on Figma design (scaled to 12px base)
		if (isAI) {
			// AI message header with logo and assistant name
			sb.append("<div style='display: flex; flex-direction: row; align-items: center; padding: 0; gap: 8px; margin-bottom: 12px;'>");
			sb.append("<img src='");
			sb.append(ThemeManager.getThemeResource("images/clde-logo-icon-vector.svg"));
			sb.append("' style='width: 18px; height: 18px;'/>");
			sb.append("<span style='font-family: Helvetica Neue; font-weight: 500; font-size: 12px; line-height: 15px; color: #181D27;'>");
			sb.append(Util.maskHTML(getUserName(entry), true));
			sb.append("</span></div>");
		}

		// Message body with updated typography (scaled to 12px base)
		sb.append("<div style='font-family: Helvetica Neue; font-weight: 400; font-size: 12px; line-height: 18px; color: ");
		sb.append(isAI ? "#181D27" : "#535862");
		sb.append(";'>");

		String messageText = entry.getCharacterData();
		if (messageText != null) {
			// Check if message contains HTML (AI responses may include formatting)
			if (isAI && (messageText.contains("<") || messageText.contains(">"))) {
				// Allow HTML for AI responses (already sanitized by AI provider)
				sb.append(messageText);
			} else {
				// Escape HTML for user messages, but preserve line breaks
				String escaped = Util.maskHTML(messageText, true);
				// Convert newlines to <br> tags
				escaped = escaped.replace("\n", "<br/>");
				sb.append(escaped);
			}
		}

		sb.append("</div>");

		// Add copy button for user messages (based on screenshot, scaled to 12px base)
		if (!isAI) {
			sb.append("<div style='display: flex; flex-direction: row; align-items: flex-start; padding: 0; gap: 18px; margin-top: 12px;'>");
			sb.append("<div style='display: flex; flex-direction: row; justify-content: center; align-items: center; padding: 0; gap: 6px; cursor: pointer;'>");
			if (ThemeManager.isUseFontIconForImage()) {
				sb.append("<i class='z-icon-Copy' style='font-size: 12px; color: #717680;'></i>");
			} else {
				sb.append("<img src='");
				sb.append(ThemeManager.getThemeResource("images/Copy.png"));
				sb.append("' style='width: 12px; height: 12px;'/>");
			}
			sb.append("<span style='font-family: Helvetica Neue; font-weight: 500; font-size: 10.5px; line-height: 13.5px; color: #717680;'>Copy</span>");
			sb.append("</div></div>");
		}

		return sb.toString();
	}

	/**
	 * Check if message is from AI
	 * @param entry chat entry
	 * @return true if AI message
	 */
	private boolean isAIMessage(MChatEntry entry) {
		// If it's an MAIChatEntry, use the isAIResponse method
		if (entry instanceof MAIChatEntry) {
			return ((MAIChatEntry) entry).isAIResponse();
		}

		// Fallback: check if AD_User_ID of chat entry is different from current user
		int currentUser = Env.getAD_User_ID(Env.getCtx());
		return entry.getAD_User_ID() != currentUser;
	}

	/**
	 * Get user name for display
	 * @param entry chat entry
	 * @return user name
	 */
	private String getUserName(MChatEntry entry) {
		MUser user = MUser.get(Env.getCtx(), entry.getAD_User_ID());

		if (isAIMessage(entry)) {
			// Return the actual AI user's name from the provider
			return user != null ? user.getName() : "AI Assistant";
		}

		return user != null ? user.getName() : "User";
	}

	/**
	 * Format timestamp for display
	 * @param timestamp timestamp
	 * @return formatted string
	 */
	private String formatTimestamp(java.sql.Timestamp timestamp) {
		if (timestamp == null) {
			return "";
		}
		return dateFormat.format(timestamp);
	}

	/**
	 * Scroll messages container to bottom
	 */
	private void scrollToBottom() {
		Clients.evalJavaScript(
			"setTimeout(function(){" +
			"var el=document.querySelector('.ai-messages');" +
			"if(el)el.scrollTop=el.scrollHeight;" +
			"}, 200);"
		);
	}

	/**
	 * Show loading indicator
	 */
	private void showLoading() {
		loadingIndicator.setContent("<div class='ai-loading' style='padding: 8px; text-align: center; color: #666;'>" +
			"<i>" + Msg.getMsg(Env.getCtx(), "AIThinking") + "</i></div>");
		scrollToBottom();
	}

	/**
	 * Hide loading indicator
	 */
	private void hideLoading() {
		loadingIndicator.setContent("<div class='ai-loading' style='display:none;'></div>");
	}

	/**
	 * Event handler
	 */
	@Override
	public void onEvent(Event event) throws Exception {
		if (event.getTarget() == sendButton ||
			(event.getTarget() == inputBox && event.getName().equals(Events.ON_OK))) {
			sendMessage();
		} else if (event.getTarget() == clearButton) {
			clearChat();
		}
	}

	/**
	 * Send user message and get AI response
	 */
	public void sendMessage() {
		String message = inputBox.getText();
		if (Util.isEmpty(message, true)) {
			return;
		}

		// Disable input while processing
		inputBox.setDisabled(true);
		sendButton.setDisabled(true);

		// Save user message
		MAIChatEntry userEntry = new MAIChatEntry(chat, message);
		userEntry.saveEx();

		// Clear input
		inputBox.setText("");

		// Show user message immediately
		renderMessage(userEntry);
		showLoading();
		scrollToBottom();

		// Call AI service asynchronously
		Desktop desktop = Executions.getCurrent().getDesktop();
		CompletableFuture.runAsync(() -> {
			try {
				// Get MAIChat instance
				MAIChat aiChat = (chat instanceof MAIChat) ?
					(MAIChat) chat :
					new MAIChat(Env.getCtx(), chat.getCM_Chat_ID(), null);

				// TODO: Call AI service with conversation history
//				AIResponse aiResponse = aiService.sendMessageWithHistory(
//					Env.getCtx(),
//					aiChat,
//					message,
//					10,  // FIXME hardcoded: Include last 10 messages for context
//					null
//				);
				AIResponse aiResponse = aiService.sendMessage(
						Env.getCtx(),
						aiChat,
						message,
						null
					);

				if (!aiResponse.isSuccess()) {
					throw new Exception(aiResponse.getErrorMessage() != null ?
						aiResponse.getErrorMessage() : "AI service returned error");
				}

				// Create AI chat entry
				MAIChatEntry aiEntry = MAIChatEntry.createAIResponse(aiChat, aiResponse.getContent());
				aiEntry.saveEx();

				// Update chat metadata
				if (aiResponse.getModel() != null) {
					aiChat.setAIModel(aiResponse.getModel());
				}
				if (aiService.getProviderName(aiService.getDefaultProviderId()) != null) {
					aiChat.setAIProvider(aiService.getProviderName(aiService.getDefaultProviderId()));
				}
				if (aiResponse.getTokenUsage() != null) {
					aiChat.addTokens(aiResponse.getTokenUsage().getTotalTokens());
				}
				aiChat.saveEx();

				// Update UI (must happen in ZK thread)
				Executions.schedule(desktop, e -> {
					hideLoading();
					renderMessage(aiEntry);
					inputBox.setDisabled(false);
					sendButton.setDisabled(false);
					inputBox.focus();
					// Scroll to bottom after AI response is rendered
					scrollToBottom();
				}, new Event("onAIResponse"));

			} catch (Exception e) {
				log.log(Level.SEVERE, "AI response failed", e);

				// Show error in UI
				Executions.schedule(desktop, ev -> {
					hideLoading();

					String errorMsg = "<div style='color: #d32f2f; padding: 8px; " +
						"background: #ffebee; border-radius: 4px; border-left: 3px solid #d32f2f;'>" +
						"<strong>Error:</strong> " + Util.maskHTML(e.getMessage(), true) + "</div>";

					MChatEntry errorEntry = MAIChatEntry.createAIResponse(chat, errorMsg);
					errorEntry.saveEx();
					renderMessage(errorEntry);
					inputBox.setDisabled(false);
					sendButton.setDisabled(false);
				}, new Event("onAIError"));
			}
		});
	}

	/**
	 * Clear chat history
	 */
	private void clearChat() {
		if (chat == null) {
			return;
		}

		// Deactivate all entries
		MChatEntry[] entries = chat.getEntries(true);
		for (MChatEntry entry : entries) {
			entry.setIsActive(false);
			entry.saveEx();
		}

		// Refresh display
		renderMessages();
	}

	/**
	 * Public method to send message programmatically
	 * @param message message text
	 */
	public void sendMessageProgrammatically(String message) {
		if (!Util.isEmpty(message, true)) {
			inputBox.setText(message);
			sendMessage();
		}
	}

	/**
	 * Get current chat
	 * @return chat instance
	 */
	public MChat getChat() {
		return chat;
	}
}
