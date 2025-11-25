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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.adempiere.webui.component.Combobox;
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
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import com.cloudempiere.ai.context.AIContextProviderRegistry;
import com.cloudempiere.ai.context.ContextParameters;
import com.cloudempiere.ai.context.IAIContextProvider;
import com.cloudempiere.ai.model.MAIChat;
import com.cloudempiere.ai.model.MAIChatEntry;
import com.cloudempiere.ai.provider.dto.AIResponse;
import com.cloudempiere.ai.service.AIConversationService;
import com.cloudempiere.ai.util.ZoomLinkProcessor;

import org.adempiere.webui.apps.AEnv;

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

	/** Custom event name for zoom requests */
	public static final String ON_ZOOM = "onZoom";

	/** Messages container (scrollable) */
	private Vlayout messagesContainer;

	/** Input textbox */
	private Textbox inputBox;

	/** Send button */
	private Button sendButton;

	/** Clear chat button */
	private Button clearButton;

	/** New thread button */
	private Button newThreadButton;

	/** Thread selector dropdown (for switching between conversation threads) */
	private Combobox threadSelector;

	/** Loading indicator */
	private Html loadingIndicator;

	/** Current thread root ID (first message ID in current thread) */
	private int currentThreadRootId = 0;

	/** Current chat instance */
	private MChat chat;

	/** Date formatter for timestamps */
	private SimpleDateFormat dateFormat;

	/** AI conversation service */
	private AIConversationService aiService;

	/** Maximum messages to display (for performance) */
	private static final int MAX_MESSAGES_DISPLAY = 100;

	/** Context support enabled */
	private boolean contextEnabled = false;

	/** Current window context */
	private JSONObject currentContext;

	/** Current window number */
	private int currentWindowNo = -1;

	/** Current tab number */
	private int currentTabNo = -1;

	/** Context indicator (if context enabled) */
	private Html contextIndicator;

	/** Session context (captured at initialization to preserve user's session info) */
	private Properties sessionCtx;

	/**
	 * Default Constructor
	 */
	public AIChatWidget() {
		this(false);
	}

	/**
	 * Constructor with context support option
	 * @param enableContext if true, widget will use window/tab context
	 */
	public AIChatWidget(boolean enableContext) {
		super();
		this.contextEnabled = enableContext;
		init();
	}

	/**
	 * Initialize the widget
	 */
	private void init() {
		setSclass("ai-chat-widget");
		ZKUpdateUtil.setVflex(this, "1");
		ZKUpdateUtil.setHflex(this, "1");
		// Use flexbox to fill available space with minimum 600px height
		// The vflex="1" setting makes this widget respect parent container constraints
		setStyle("display: flex; flex-direction: column; padding: 12px; background: #FDFDFD; " +
				"border-radius: 12px; height: 700px;");

		// Capture session context at initialization (important for language, client, etc.)
		// Use Env.getCtx() which should have the session context when called from UI thread
		sessionCtx = Env.getCtx();

		// Register zoom event listener for clickable record links
		addEventListener(ON_ZOOM, this);

		dateFormat = DisplayType.getDateFormat(DisplayType.DateTime);

		// Load Markdown rendering libraries (marked.js + Prism.js for syntax highlighting)
		loadMarkdownLibraries();

		// Initialize AI service
		aiService = new AIConversationService();

		// Context indicator (if enabled)
		if (contextEnabled) {
			contextIndicator = new Html();
			contextIndicator.setId("aiContextIndicator_" + getUuid());
			contextIndicator.setContent(
				"<div style='padding: 6px 12px; background: #E8F5E9; border-radius: 4px; " +
				"margin-bottom: 8px; font-size: 11px; color: #2E7D32; display: none;'>" +
				"<i class='z-icon-InfoCircle'></i> Context: <span id='contextInfo_" + getUuid() + "'>No window open</span>" +
				"</div>"
			);
			appendChild(contextIndicator);
		}

		// Thread control bar (thread selector + new thread button)
		Hlayout threadControlBar = new Hlayout();
		threadControlBar.setStyle("width: 100%; gap: 8px; align-items: center; margin-bottom: 8px; flex-shrink: 0;");

		// Thread selector dropdown
		threadSelector = new Combobox();
		threadSelector.setPlaceholder("Select conversation...");
		threadSelector.setStyle("border: 1px solid #E0E0E0; border-radius: 6px; font-size: 12px; background: #FFFFFF;");
		threadSelector.addEventListener(Events.ON_SELECT, this);

		// New Thread button
		newThreadButton = new Button();
		newThreadButton.addEventListener(Events.ON_CLICK, this);
		newThreadButton.setSclass("ai-newthread-btn");
		newThreadButton.setLabel(Msg.getMsg(Env.getCtx(), "New"));
		if (ThemeManager.isUseFontIconForImage())
			newThreadButton.setIconSclass("z-icon-New");
		else
			newThreadButton.setImage(ThemeManager.getThemeResource("images/New-White.png"));
		newThreadButton.setTooltiptext("Start a new conversation thread");

		threadControlBar.appendChild(threadSelector);
		threadControlBar.appendChild(newThreadButton);
		appendChild(threadControlBar);

		// Messages area (scrollable) - flex-grow to fill available space
		messagesContainer = new Vlayout();
		messagesContainer.setSclass("ai-messages");
		ZKUpdateUtil.setVflex(messagesContainer, "1");
		messagesContainer.setStyle("overflow-y: auto; overflow-x: hidden; margin-bottom: 12px; " +
				"padding: 6px; gap: 8px; flex: 1 1 auto; min-height: 0;");
		appendChild(messagesContainer);

		// Loading indicator (hidden by default)
		loadingIndicator = new Html();
		loadingIndicator.setContent("<div class='ai-loading' style='display:none; padding: 12px 18px; text-align: left;'>" +
			"<div style='display: flex; align-items: center; gap: 8px;'>" +
			"<div style='width: 18px; height: 18px; border-radius: 27px; background: #E9EAEB;'></div>" +
			"<span style='font-family: Helvetica Neue; font-weight: 400; font-size: 12px; line-height: 18px; color: #717680;'>" +
			"<i>" + Msg.getMsg(Env.getCtx(), "AIThinking") + "</i></span></div></div>");
		messagesContainer.appendChild(loadingIndicator);

		// Input area - stays at bottom (flex-shrink: 0)
		Hlayout inputArea = new Hlayout();
		inputArea.setStyle("width: 100%; gap: 8px; align-items: center; flex-shrink: 0;");

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
			chat = MAIChat.getOrCreateGlobalChat(sessionCtx, null);

			// Load thread list and select most recent thread
			loadThreadList();

			// Render messages for current thread
			renderMessages();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to load/create chat", e);
		}
	}

	/**
	 * Render all messages from chat (filtered by current thread)
	 */
	private void renderMessages() {
		// Clear existing messages (but keep loading indicator)
		// Collect children first to avoid ConcurrentModificationException
		List<Component> toRemove = messagesContainer.getChildren().stream()
			.filter(c -> c != loadingIndicator)
			.collect(Collectors.toList());
		toRemove.forEach(c -> c.detach());

		if (chat == null) {
			return;
		}

		MChatEntry[] entries = chat.getEntries(true);

		// Filter messages by current thread
		List<MChatEntry> threadEntries = getThreadEntries(entries, currentThreadRootId);

		// Limit number of messages displayed for performance
		int startIndex = Math.max(0, threadEntries.size() - MAX_MESSAGES_DISPLAY);

		for (int i = startIndex; i < threadEntries.size(); i++) {
			MChatEntry entry = threadEntries.get(i);
			if (entry.isActive()) {
				renderMessage(entry);
			}
		}

		// Scroll to bottom after rendering - use longer delay for initial load
		Clients.evalJavaScript(
			"setTimeout(function(){" +
			"var el=document.querySelector('.ai-messages');" +
			"if(el)el.scrollTop=el.scrollHeight;" +
			"}, 400);"
		);
	}

	/**
	 * Get all entries for a specific thread (root message and all its children)
	 * @param allEntries all chat entries
	 * @param threadRootId root message ID (0 for new thread - shows nothing)
	 * @return list of entries in this thread
	 */
	private List<MChatEntry> getThreadEntries(MChatEntry[] allEntries, int threadRootId) {
		List<MChatEntry> threadEntries = new ArrayList<>();

		if (threadRootId == 0) {
			// New thread - show nothing
			return threadEntries;
		}

		// Add root message
		MChatEntry rootEntry = null;
		for (MChatEntry entry : allEntries) {
			if (entry.getCM_ChatEntry_ID() == threadRootId) {
				rootEntry = entry;
				threadEntries.add(entry);
				break;
			}
		}

		if (rootEntry == null) {
			return threadEntries;
		}

		// Add all child messages (messages with this root as parent)
		// We only support one level: root + children (no grandchildren)
		for (MChatEntry entry : allEntries) {
			if (entry.getCM_ChatEntry_ID() != threadRootId &&
				entry.getCM_ChatEntryParent_ID() == threadRootId) {
				threadEntries.add(entry);
			}
		}

		return threadEntries;
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
				"max-width: 85%; align-self: flex-start;");
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
			sb.append(ThemeManager.THEME_PATH_PREFIX+ThemeManager.getTheme()+"/images/clde-logo-icon-vector.svg");
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
			if (isAI) {
				// AI messages: Process zoom links first, then render as Markdown
				String processedText = ZoomLinkProcessor.processZoomLinks(messageText, sessionCtx, getUuid());
				sb.append(renderMarkdown(processedText));
			} else {
				// User messages: Escape HTML for security, but preserve line breaks
				String escaped = Util.maskHTML(messageText, true);
				// Convert newlines to <br> tags
				escaped = escaped.replace("\n", "<br/>");
				sb.append(escaped);
			}
		}

		sb.append("</div>");

		// Add copy button for user messages (based on screenshot, scaled to 12px base)
		if (!isAI) {
			// Escape message text for JavaScript
			String jsEscapedText = messageText != null ?
				messageText.replace("\\", "\\\\")
					.replace("'", "\\'")
					.replace("\"", "\\\"")
					.replace("\n", "\\n")
					.replace("\r", "\\r") : "";

			sb.append("<div style='display: flex; flex-direction: row; align-items: flex-start; padding: 0; gap: 18px; margin-top: 12px;'>");
			sb.append("<div onclick=\"(function(btn){");
			sb.append("var orig=btn.innerHTML;");
			sb.append("navigator.clipboard.writeText('").append(jsEscapedText).append("').then(function(){");
			sb.append("btn.innerHTML='<span style=\\'font-family: Helvetica Neue; font-weight: 500; font-size: 10.5px; line-height: 13.5px; color: #4CAF50;\\'>Copied!</span>';");
			sb.append("setTimeout(function(){btn.innerHTML=orig;},2000);");
			sb.append("}).catch(function(err){console.error('Copy failed:',err);});");
			sb.append("})(this);\" ");
			sb.append("style='display: flex; flex-direction: row; justify-content: center; align-items: center; padding: 0; gap: 6px; cursor: pointer;'>");
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
		int currentUser = Env.getAD_User_ID(sessionCtx);
		return entry.getAD_User_ID() != currentUser;
	}

	/**
	 * Get user name for display
	 * @param entry chat entry
	 * @return user name
	 */
	private String getUserName(MChatEntry entry) {
		MUser user = MUser.get(sessionCtx, entry.getAD_User_ID());

		if (isAIMessage(entry)) {
			// Return the actual AI user's name from the provider
			return user != null ? user.getName() : "AI Assistant";
		}

		return user != null ? user.getName() : "User";
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
		} else if (event.getTarget() == newThreadButton) {
			createNewThread();
		} else if (event.getTarget() == threadSelector && event.getName().equals(Events.ON_SELECT)) {
			switchThread();
		} else if (event.getName().equals(ON_ZOOM)) {
			handleZoomEvent(event);
		}
	}

	/**
	 * Handle zoom event from clickable record links
	 * @param event zoom event containing tableId and recordId
	 */
	private void handleZoomEvent(Event event) {
		try {
			// Extract table ID and record ID from event data
			Object data = event.getData();
			if (data instanceof JSONObject) {
				JSONObject jsonData = (JSONObject) data;
				Integer tableId = (Integer) jsonData.get("tableId");
				Integer recordId = (Integer) jsonData.get("recordId");

				if (tableId != null && recordId != null && tableId > 0 && recordId > 0) {
					log.fine("Zoom request: tableId=" + tableId + ", recordId=" + recordId);
					// Call AEnv.zoom to open the record window
					AEnv.zoom(tableId, recordId);
				} else {
					log.warning("Invalid zoom event data: tableId=" + tableId + ", recordId=" + recordId);
				}
			} else {
				log.warning("Zoom event data is not a JSON object: " + (data != null ? data.getClass().getName() : "null"));
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to handle zoom event", e);
			Clients.showNotification(Msg.getMsg(Env.getCtx(), "Error") + ": " + e.getMessage(),
				"error", this, null, -1);
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

		// Refresh context before sending (in case window/tab changed)
		if (contextEnabled) {
			refreshContext();
		}

		// Save user message with proper thread parent
		MAIChatEntry userEntry = new MAIChatEntry(chat, message);

		// Set thread parent if we're in an existing thread
		// We only support one level: root message + children (no grandchildren)
		if (currentThreadRootId > 0) {
			userEntry.setCM_ChatEntryParent_ID(currentThreadRootId);
		}

		userEntry.saveEx();

		// If this is a new thread, it becomes the root
		if (currentThreadRootId == 0) {
			currentThreadRootId = userEntry.getCM_ChatEntry_ID();
			// Reload thread list to show new thread
			loadThreadList();
		}

		// Clear input
		inputBox.setText("");

		// Show user message immediately
		renderMessage(userEntry);
		showLoading();
		scrollToBottom();

		// Capture context snapshot for async operation
		final JSONObject contextSnapshot = currentContext;

		// Call AI service asynchronously
		Desktop desktop = Executions.getCurrent().getDesktop();
		CompletableFuture.runAsync(() -> {
			try {
				// Get MAIChat instance (use sessionCtx to preserve language, client, etc.)
				MAIChat aiChat = (chat instanceof MAIChat) ?
					(MAIChat) chat :
					new MAIChat(sessionCtx, chat.getCM_Chat_ID(), null);

				// Call AI service with session context (CRITICAL: use sessionCtx not Env.getCtx()!)
				AIResponse aiResponse = aiService.sendMessageWithContext(
					sessionCtx,  // Use session context here to get correct language/client
					aiChat,
					message,
					contextSnapshot,  // Pass context here
					10,  // Include last 10 messages for conversation history
					null
				);

				if (!aiResponse.isSuccess()) {
					throw new Exception(aiResponse.getErrorMessage() != null ?
						aiResponse.getErrorMessage() : "AI service returned error");
				}

				// Create AI chat entry with proper thread parent
				MAIChatEntry aiEntry = MAIChatEntry.createAIResponse(aiChat, aiResponse.getContent());

				// Set thread parent (AI response is child of the thread root)
				// We only support one level: root message + children
				if (currentThreadRootId > 0) {
					aiEntry.setCM_ChatEntryParent_ID(currentThreadRootId);
				}

				aiEntry.saveEx();
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

					// Set thread parent for error entry
					// We only support one level: root message + children
					if (currentThreadRootId > 0) {
						errorEntry.setCM_ChatEntryParent_ID(currentThreadRootId);
					}

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
	 * Create a new conversation thread
	 */
	private void createNewThread() {
		// Reset current thread (next message will start a new root thread)
		currentThreadRootId = 0;

		// Clear the display - collect children first to avoid ConcurrentModificationException
		List<Component> toRemove = messagesContainer.getChildren().stream()
			.filter(c -> c != loadingIndicator)
			.collect(Collectors.toList());
		toRemove.forEach(c -> c.detach());

		// Reload thread list to refresh UI
		loadThreadList();

		// Focus input for new message
		inputBox.focus();

		log.fine("New thread created (currentThreadRootId reset to 0)");
	}

	/**
	 * Switch to selected thread from dropdown
	 */
	private void switchThread() {
		if (threadSelector.getSelectedItem() == null) {
			return;
		}

		Object value = threadSelector.getSelectedItem().getValue();
		if (value instanceof Integer) {
			currentThreadRootId = (Integer) value;
			log.fine("Switched to thread: " + currentThreadRootId);

			// Re-render messages for this thread
			renderMessages();
		}
	}

	/**
	 * Load thread list into dropdown
	 * Threads are identified by their root message (first message with no parent)
	 */
	private void loadThreadList() {
		if (chat == null) {
			return;
		}

		threadSelector.getItems().clear();

		// Add "New Thread" option
		Comboitem newItem = new Comboitem("+ New Thread");
		newItem.setValue(0);
		threadSelector.appendChild(newItem);

		// Get all root-level entries (messages with no parent)
		MChatEntry[] entries = chat.getEntries(true);
		List<MChatEntry> rootEntries = new ArrayList<>();

		for (MChatEntry entry : entries) {
			if (entry.isActive() && entry.getCM_ChatEntryParent_ID() == 0) {
				rootEntries.add(entry);
			}
		}

		// Add threads to dropdown (most recent first)
		for (int i = rootEntries.size() - 1; i >= 0; i--) {
			MChatEntry rootEntry = rootEntries.get(i);

			// Create thread label (first 50 chars of first message)
			String label = rootEntry.getCharacterData();
			if (label == null || label.trim().isEmpty()) {
				label = "Thread " + (rootEntries.size() - i);
			} else {
				label = label.trim();
				if (label.length() > 50) {
					label = label.substring(0, 47) + "...";
				}
			}

			// Add formatted date
			if (rootEntry.getCreated() != null) {
				String date = dateFormat.format(rootEntry.getCreated());
				label = label + " (" + date + ")";
			}

			Comboitem item = new Comboitem(label);
			item.setValue(rootEntry.getCM_ChatEntry_ID());
			threadSelector.appendChild(item);

			// Select current thread
			if (currentThreadRootId == rootEntry.getCM_ChatEntry_ID()) {
				threadSelector.setSelectedItem(item);
			}
		}

		// If no thread selected and we have threads, select the most recent
		if (threadSelector.getSelectedItem() == null && rootEntries.size() > 0) {
			currentThreadRootId = rootEntries.get(rootEntries.size() - 1).getCM_ChatEntry_ID();
			threadSelector.setSelectedIndex(1); // Index 1 (first real thread, after "New Thread")
		} else if (rootEntries.size() == 0) {
			// No threads yet, select "New Thread"
			currentThreadRootId = 0;
			threadSelector.setSelectedIndex(0);
		}
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

	/**
	 * Set window context for context-aware assistance
	 * @param windowNo window number
	 * @param tabNo tab number
	 */
	public void setWindowContext(int windowNo, int tabNo) {
		this.currentWindowNo = windowNo;
		this.currentTabNo = tabNo;
		refreshContext();
	}

	/**
	 * Refresh window context from current window/tab
	 */
	public void refreshWindowContext() {
		refreshContext();
	}

	/**
	 * Refresh context from current window/tab settings
	 */
	private void refreshContext() {
		if (!contextEnabled || currentWindowNo < 0) {
			currentContext = null;
			updateContextIndicator(false);
			return;
		}

		try {
			AIContextProviderRegistry registry = AIContextProviderRegistry.getInstance();
			IAIContextProvider provider = registry.getProvider("WINDOW");

			if (provider != null) {
				ContextParameters params = ContextParameters.forWindow(currentWindowNo, currentTabNo)
					.put("includeChildTabs", true);

				currentContext = provider.extractContext(sessionCtx, currentWindowNo, params);

				// Redact sensitive fields
				if (currentContext != null && currentContext.optBoolean("success", false)) {
					redactSensitiveData(currentContext, provider.getSensitiveFields());
					updateContextIndicator(true);
					log.fine("Context refreshed for window " + currentWindowNo + ", tab " + currentTabNo);
				} else {
					currentContext = null;
					updateContextIndicator(false);
					log.warning("Context extraction failed or returned unsuccessful result");
				}
			} else {
				log.warning("Window context provider not found");
				currentContext = null;
				updateContextIndicator(false);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to extract window context", e);
			currentContext = null;
			updateContextIndicator(false);
		}
	}

	/**
	 * Redact sensitive data from context
	 * @param context context object
	 * @param sensitiveFields array of sensitive field names
	 */
	private void redactSensitiveData(JSONObject context, String[] sensitiveFields) {
		if (context == null || !context.has("record_data")) {
			return;
		}

		JSONObject recordData = context.getJSONObject("record_data");
		for (String field : sensitiveFields) {
			// Case-sensitive check
			if (recordData.has(field)) {
				recordData.put(field, "[REDACTED]");
			}
			// Case-insensitive check
			for (String key : recordData.keySet()) {
				if (key.equalsIgnoreCase(field)) {
					recordData.put(key, "[REDACTED]");
				}
			}
		}
	}

	/**
	 * Update context indicator display
	 * @param hasContext true if context is available
	 */
	private void updateContextIndicator(boolean hasContext) {
		if (!contextEnabled || contextIndicator == null) {
			return;
		}

		String windowName = "No window";
		String tabName = "";

		if (hasContext && currentContext != null) {
			if (currentContext.has("window_metadata")) {
				JSONObject windowMeta = currentContext.getJSONObject("window_metadata");
				windowName = windowMeta.optString("name", "Unknown");
			}
			if (currentContext.has("tab_context")) {
				JSONObject tabCtx = currentContext.getJSONObject("tab_context");
				tabName = tabCtx.optString("tab_name", "Unknown");
			}
		}

		String display = hasContext ? "block" : "none";
		String contextInfo = hasContext ? windowName + " > " + tabName : "No window open";
		String escapedInfo = Util.maskHTML(contextInfo, true);

		Clients.evalJavaScript(
			"var el = document.getElementById('aiContextIndicator_" + getUuid() + "');" +
			"if(el) { var div = el.querySelector('div'); if(div) div.style.display='" + display + "'; }" +
			"var info = document.getElementById('contextInfo_" + getUuid() + "');" +
			"if(info) info.textContent='" + escapedInfo + "';"
		);
	}

	/**
	 * Get current context
	 * @return current context JSON or null
	 */
	public JSONObject getCurrentContext() {
		return currentContext;
	}

	/**
	 * Check if context is enabled
	 * @return true if context support is enabled
	 */
	public boolean isContextEnabled() {
		return contextEnabled;
	}

	/**
	 * Load Markdown rendering libraries (marked.js for Markdown, Prism.js for syntax highlighting)
	 */
	private void loadMarkdownLibraries() {
		// Load libraries via CDN using Clients.evalJavaScript
		// This runs once when the widget is initialized
		String script =
			"(function() {" +
			"  if (window.markedLoaded && window.prismLoaded) return;" +

			// Load marked.js (Markdown parser)
			"  if (!window.markedLoaded) {" +
			"    var markedScript = document.createElement('script');" +
			"    markedScript.src = 'https://cdn.jsdelivr.net/npm/marked@11.1.1/marked.min.js';" +
			"    markedScript.onload = function() { window.markedLoaded = true; };" +
			"    document.head.appendChild(markedScript);" +
			"  }" +

			// Load Prism.js CSS (syntax highlighting styles)
			"  if (!window.prismLoaded) {" +
			"    var prismCSS = document.createElement('link');" +
			"    prismCSS.rel = 'stylesheet';" +
			"    prismCSS.href = 'https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism.min.css';" +
			"    document.head.appendChild(prismCSS);" +

			// Add custom CSS for markdown content
			"    var customCSS = document.createElement('style');" +
			"    customCSS.textContent = '" +
			"      .ai-markdown-content { line-height: 1.6; }" +
			"      .ai-markdown-content p { margin: 0.5em 0; }" +
			"      .ai-markdown-content h1 { font-size: 1.5em; font-weight: 600; margin: 1em 0 0.5em 0; }" +
			"      .ai-markdown-content h2 { font-size: 1.3em; font-weight: 600; margin: 0.9em 0 0.4em 0; }" +
			"      .ai-markdown-content h3 { font-size: 1.1em; font-weight: 600; margin: 0.8em 0 0.3em 0; }" +
			"      .ai-markdown-content code { " +
			"        background: #f5f5f5; " +
			"        padding: 2px 6px; " +
			"        border-radius: 3px; " +
			"        font-family: Consolas, Monaco, \"Courier New\", monospace; " +
			"        font-size: 0.9em; " +
			"      }" +
			"      .ai-markdown-content pre { " +
			"        background: #f5f5f5; " +
			"        border: 1px solid #e0e0e0; " +
			"        border-radius: 6px; " +
			"        padding: 12px; " +
			"        overflow-x: auto; " +
			"        margin: 0.8em 0; " +
			"      }" +
			"      .ai-markdown-content pre code { " +
			"        background: transparent; " +
			"        padding: 0; " +
			"        font-size: 0.85em; " +
			"      }" +
			"      .ai-markdown-content ul, .ai-markdown-content ol { " +
			"        margin: 0.5em 0; " +
			"        padding-left: 1.5em; " +
			"      }" +
			"      .ai-markdown-content li { margin: 0.3em 0; }" +
			"      .ai-markdown-content blockquote { " +
			"        border-left: 3px solid #e0e0e0; " +
			"        padding-left: 1em; " +
			"        margin: 0.8em 0; " +
			"        color: #666; " +
			"      }" +
			"      .ai-markdown-content a { color: #1976D2; text-decoration: none; }" +
			"      .ai-markdown-content a:hover { text-decoration: underline; }" +
			"      .ai-markdown-content table { " +
			"        border-collapse: collapse; " +
			"        margin: 0.8em 0; " +
			"        width: 100%; " +
			"      }" +
			"      .ai-markdown-content table th, .ai-markdown-content table td { " +
			"        border: 1px solid #e0e0e0; " +
			"        padding: 8px; " +
			"        text-align: left; " +
			"      }" +
			"      .ai-markdown-content table th { " +
			"        background: #f5f5f5; " +
			"        font-weight: 600; " +
			"      }" +
			"    ';" +
			"    document.head.appendChild(customCSS);" +

			// Load Prism.js core
			"    var prismScript = document.createElement('script');" +
			"    prismScript.src = 'https://cdn.jsdelivr.net/npm/prismjs@1.29.0/prism.min.js';" +
			"    prismScript.setAttribute('data-manual', '');" +

			// Load common language components
			"    prismScript.onload = function() {" +
			"      var languages = ['java', 'javascript', 'python', 'sql', 'json', 'xml', 'bash'];" +
			"      var loaded = 0;" +
			"      languages.forEach(function(lang) {" +
			"        var langScript = document.createElement('script');" +
			"        langScript.src = 'https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-' + lang + '.min.js';" +
			"        langScript.onload = function() {" +
			"          loaded++;" +
			"          if (loaded === languages.length) window.prismLoaded = true;" +
			"        };" +
			"        document.head.appendChild(langScript);" +
			"      });" +
			"    };" +
			"    document.head.appendChild(prismScript);" +
			"  }" +
			"})();";

		Clients.evalJavaScript(script);
	}

	/**
	 * Render Markdown to HTML using marked.js (client-side)
	 * Returns a unique ID for the container so we can process it after rendering
	 * @param markdownText the markdown text to render
	 * @return HTML string with markdown container and script to render it
	 */
	private String renderMarkdown(String markdownText) {
		// Generate unique ID for this markdown block
		String containerId = "md_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);

		// Escape the markdown text for JavaScript (critical for security)
		String escapedMarkdown = markdownText
			.replace("\\", "\\\\")
			.replace("'", "\\'")
			.replace("\r", "")
			.replace("\n", "\\n")
			.replace("</script>", "<\\/script>");

		StringBuilder sb = new StringBuilder();

		// Container for rendered markdown
		sb.append("<div id='").append(containerId).append("' class='ai-markdown-content'></div>");

		// Script to render markdown when libraries are loaded
		sb.append("<script>");
		sb.append("(function() {");
		sb.append("  var renderMD = function() {");
		sb.append("    if (!window.marked || !window.Prism) {");
		sb.append("      setTimeout(renderMD, 100);");
		sb.append("      return;");
		sb.append("    }");
		sb.append("    var container = document.getElementById('").append(containerId).append("');");
		sb.append("    if (!container) return;");

		// Configure marked to use Prism for code highlighting
		sb.append("    marked.setOptions({");
		sb.append("      highlight: function(code, lang) {");
		sb.append("        if (lang && Prism.languages[lang]) {");
		sb.append("          return Prism.highlight(code, Prism.languages[lang], lang);");
		sb.append("        }");
		sb.append("        return code;");
		sb.append("      },");
		sb.append("      breaks: true,");
		sb.append("      gfm: true");
		sb.append("    });");

		sb.append("    var html = marked.parse('").append(escapedMarkdown).append("');");
		sb.append("    container.innerHTML = html;");

		// Apply Prism to any code blocks that weren't caught by marked's highlight
		sb.append("    container.querySelectorAll('pre code').forEach(function(block) {");
		sb.append("      if (!block.classList.contains('language-')) {");
		sb.append("        Prism.highlightElement(block);");
		sb.append("      }");
		sb.append("    });");
		sb.append("  };");
		sb.append("  renderMD();");
		sb.append("})();");
		sb.append("</script>");

		return sb.toString();
	}
}
