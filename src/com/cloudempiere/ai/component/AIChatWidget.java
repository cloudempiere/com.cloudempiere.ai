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
import com.cloudempiere.ai.error.AIErrorHandler;
import com.cloudempiere.ai.error.AIErrorHandler.AIErrorResult;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;
import com.cloudempiere.ai.provider.langchain4j.AIService;
import com.cloudempiere.ai.provider.langchain4j.AIService.ChatResult;
import com.cloudempiere.ai.service.ChatAccessService;
import com.cloudempiere.ai.service.IChatAccessService.ChatAccess;
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

	/** Stop button (cancels in-progress AI request) */
	private Button stopButton;

	/** Current async request (for cancellation) */
	private volatile CompletableFuture<?> currentRequest;

	/** Flag indicating if the current request was cancelled */
	private volatile boolean requestCancelled = false;

	/** Flag indicating if streaming is in progress */
	private volatile boolean streamingInProgress = false;

	/** Current streaming message component (for cancellation) */
	private volatile AIChatStreamingMessage currentStreamingMessage;

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

	/** AI service (LangChain4j) */
	private AIService langchainService;

	/** Flag indicating if streaming mode is enabled (ADR-033) */
	private static final String STREAMING_MODE = System.getProperty("ai.chat.streaming", "true");
	private final boolean useStreaming = "true".equalsIgnoreCase(STREAMING_MODE);

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

	/** Current user's access level to the chat (ADR-036) */
	private ChatAccess currentAccess = ChatAccess.OWNER;

	/** Access indicator (shows read-only badge when applicable) */
	private Html accessIndicator;

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

		// Initialize AI service (LangChain4j)
		langchainService = AIService.getInstance();
		log.fine("AIChatWidget initialized with LangChain4j service");

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

		// Access indicator (ADR-036: shows read-only badge when user has limited access)
		accessIndicator = new Html();
		accessIndicator.setId("aiAccessIndicator_" + getUuid());
		// Initially hidden - will be shown by updateAccessIndicator() if needed
		accessIndicator.setContent(buildAccessIndicatorHtml(ChatAccess.OWNER));
		appendChild(accessIndicator);

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
		newThreadButton.setLabel(Msg.getMsg(Env.getCtx(), "New")); // FIXME: create message
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
		inputBox.addEventListener(Events.ON_OK, this); // Enter key to send

		// Button container - holds send/stop buttons in same position
		Div buttonContainer = new Div();
		buttonContainer.setStyle("position: relative; width: 42px; height: 42px;");

		sendButton = new Button();
		sendButton.addEventListener(Events.ON_CLICK, this);
		sendButton.setSclass("ai-send-btn");
		if (ThemeManager.isUseFontIconForImage())
			sendButton.setIconSclass("z-icon-Send-White");
		else
			sendButton.setImage(ThemeManager.getThemeResource("images/Send-White.png"));
		sendButton.setStyle("position: absolute; top: 0; left: 0; width: 42px; height: 42px; background: #181D27; border-radius: 100px; " +
			"display: flex; align-items: center; justify-content: center; border: none; cursor: pointer;");

		// Stop button (hidden by default, shown during AI processing)
		stopButton = new Button();
		stopButton.addEventListener(Events.ON_CLICK, this);
		stopButton.setSclass("ai-stop-btn");
		if (ThemeManager.isUseFontIconForImage())
			stopButton.setIconSclass("z-icon-Square-White");
		else
			stopButton.setImage(ThemeManager.getThemeResource("images/Cancel24.png"));
		stopButton.setStyle("position: absolute; top: 0; left: 0; width: 42px; height: 42px; background: #D32F2F; border-radius: 100px; " +
			"display: none; align-items: center; justify-content: center; border: none; cursor: pointer;");
		stopButton.setTooltiptext(Msg.getMsg(Env.getCtx(), "Stop"));

		buttonContainer.appendChild(sendButton);
		buttonContainer.appendChild(stopButton);

		clearButton = new Button(Msg.getMsg(Env.getCtx(), "ClearChat"));
		clearButton.addEventListener(Events.ON_CLICK, this);
		clearButton.setSclass("ai-clear-btn");
		clearButton.setTooltiptext(Msg.getMsg(Env.getCtx(), "ClearChat"));

		inputArea.appendChild(inputBox);
		inputArea.appendChild(buttonContainer);
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

			// Check user's access level (ADR-036)
			currentAccess = ChatAccessService.get().getAccess(sessionCtx, chat);
			log.fine("Chat access level: " + currentAccess + " for chat " + chat.get_ID());

			// If no access, show error and return
			if (currentAccess == ChatAccess.NONE) {
				log.warning("User has no access to chat " + chat.get_ID());
				Clients.showNotification(
					Msg.getMsg(sessionCtx, "AccessCannotRead"),
					"error", this, "middle_center", 5000);
				return;
			}

			// Update UI based on access level
			updateAccessIndicator();

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
			"}, 600);"
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
			// Use Executions.encodeURL to convert ZK ~./ resource path to browser-accessible URL
			String logoUrl = ThemeManager.THEME_PATH_PREFIX+ThemeManager.getTheme()+"/images/clde-logo-icon-vector.svg";
			sb.append(Executions.encodeURL(logoUrl));
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
			return user != null ? user.getName() : "AI Assistant"; // FIXME: create message
		}

		return user != null ? user.getName() : "User"; // FIXME: create message
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
		} else if (event.getTarget() == stopButton) {
			// Stop button click - cancel if streaming is in progress
			if (streamingInProgress) {
				cancelCurrentRequest();
			}
		} else if (event.getTarget() == clearButton) {
			clearChat();
		} else if (event.getTarget() == newThreadButton) {
			createNewThread();
		} else if (event.getTarget() == threadSelector && event.getName().equals(Events.ON_SELECT)) {
			switchThread();
		} else if (event.getName().equals(ON_ZOOM)) {
			handleZoomEvent(event);
		}
		// TODO: Add ESC key shortcut to cancel streaming (requires ZK keyboard handling research)
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
	 * Send user message and get AI response.
	 * Dispatches to either legacy or LangChain4j service based on feature flag.
	 */
	public void sendMessage() {
		String message = inputBox.getText();
		if (Util.isEmpty(message, true)) {
			return;
		}

		// Check write permission (ADR-036)
		if (currentAccess.ordinal() < ChatAccess.WRITE.ordinal()) {
			Clients.showNotification(
				Msg.getMsg(sessionCtx, "AccessCannotWrite"),
				"warning", inputBox, "top_center", 3000);
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

		// Send message using LangChain4j service
		sendMessageLangChain4j(message);
	}

	/**
	 * Send message using LangChain4j service (new path).
	 * Includes guardrails, metrics, and improved conversation memory.
	 * Supports both streaming (ADR-033) and non-streaming modes.
	 */
	private void sendMessageLangChain4j(String message) {
		final JSONObject contextSnapshot = currentContext;
		final int threadRootIdSnapshot = currentThreadRootId;

		Desktop desktop = Executions.getCurrent().getDesktop();

		// Use streaming mode if enabled (ADR-033)
		if (useStreaming) {
			sendMessageLangChain4jStreaming(message, contextSnapshot, threadRootIdSnapshot, desktop);
		} else {
			sendMessageLangChain4jBatch(message, contextSnapshot, threadRootIdSnapshot, desktop);
		}
	}

	/**
	 * Send message using LangChain4j streaming mode (ADR-033).
	 * Provides real-time token streaming with tool timeline visibility.
	 */
	private void sendMessageLangChain4jStreaming(String message, JSONObject contextSnapshot,
			int threadRootIdSnapshot, Desktop desktop) {

		log.warning("[UI-STREAM] ========================================");
		log.warning("[UI-STREAM] sendMessageLangChain4jStreaming STARTED");
		log.warning("[UI-STREAM] message: " + message.substring(0, Math.min(50, message.length())));
		log.warning("[UI-STREAM] threadRootId: " + threadRootIdSnapshot);
		log.warning("[UI-STREAM] desktop: " + (desktop != null ? desktop.getId() : "null"));
		log.warning("[UI-STREAM] ========================================");

		// Reset cancellation state, mark streaming as in progress, and show stop button
		requestCancelled = false;
		streamingInProgress = true;
		showStopButton();

		// Get MAIChat and provider
		final MAIChat aiChat;
		final MAIProvider provider;
		try {
			aiChat = (chat instanceof MAIChat) ?
				(MAIChat) chat :
				new MAIChat(sessionCtx, chat.getCM_Chat_ID(), null);

			provider = MAIProvider.getDefault(sessionCtx, null);
			if (provider == null) {
				throw new Exception("No AI provider configured. Please configure an AI provider in the system.");
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to initialize streaming", e);
			streamingInProgress = false;
			showSendButton();
			handleErrorResponse(desktop, e, threadRootIdSnapshot, null, message);
			return;
		}

		// Create streaming message component and store reference for cancellation
		final AIChatStreamingMessage streamingMsg = new AIChatStreamingMessage();
		currentStreamingMessage = streamingMsg;

		// Get agent name from provider's AD_User (capture for use in lambda)
		String aiUserName = "AI Assistant";
		try {
			if (provider.getAD_User_ID() > 0) {
				MUser aiUser = MUser.get(sessionCtx, provider.getAD_User_ID());
				if (aiUser != null && aiUser.getName() != null) {
					aiUserName = aiUser.getName();
				}
			}
		} catch (Exception ex) {
			log.fine("Could not get AI user name: " + ex.getMessage());
		}
		final String agentName = aiUserName;

		// Insert streaming message into UI (before loading indicator)
		Executions.schedule(desktop, e -> {
			// Add divider before message
			if (messagesContainer.getChildren().size() > 1) {
				Html divider = new Html();
				divider.setContent("<div style='width: 100%; height: 0; border: 1px solid rgba(24, 29, 39, 0.12); margin: 0;'></div>");
				messagesContainer.insertBefore(divider, loadingIndicator);
			}

			// Add AI message header
			Div msgDiv = new Div();
			msgDiv.setSclass("ai-message");
			msgDiv.setStyle("display: flex; flex-direction: column; gap: 12px; padding: 12px 18px; background: transparent;");

			// Header with logo and agent name
			Html header = new Html();
			String logoUrl = ThemeManager.THEME_PATH_PREFIX + ThemeManager.getTheme() + "/images/clde-logo-icon-vector.svg";
			header.setContent(
				"<div style='display: flex; align-items: center; gap: 8px; margin-bottom: 12px;'>" +
				"<img src='" + Executions.encodeURL(logoUrl) + "' style='width: 18px; height: 18px;'/>" +
				"<span style='font-family: Helvetica Neue; font-weight: 500; font-size: 12px; color: #181D27;'>" + agentName + "</span>" +
				"</div>"
			);
			msgDiv.appendChild(header);
			msgDiv.appendChild(streamingMsg);
			messagesContainer.insertBefore(msgDiv, loadingIndicator);

			hideLoading();
			scrollToBottom();
		}, new Event("onStreamStart"));

		// Build streaming callback with logging
		log.warning("[UI-STREAM] Building streaming callback...");
		AIStreamCallback callback = AIStreamCallback.builder()
			.onChunk(chunk -> {
				// Skip if request was cancelled
				if (requestCancelled) {
					return;
				}
				log.warning("[UI-STREAM] onChunk received, length=" + (chunk != null ? chunk.length() : 0));
				try {
					Executions.schedule(desktop, e -> {
						streamingMsg.appendChunk(chunk);
						scrollToBottom();
					}, new Event("onChunk"));
				} catch (Exception ex) {
					log.severe("[UI-STREAM] Failed to schedule onChunk: " + ex.getMessage());
					ex.printStackTrace();
				}
			})
			.onToolStart((toolName, args) -> {
				if (requestCancelled) return;
				log.warning("[UI-STREAM] onToolStart: " + toolName);
				try {
					Executions.schedule(desktop, e -> {
						streamingMsg.showToolStart(toolName, args);
					}, new Event("onToolStart"));
				} catch (Exception ex) {
					log.severe("[UI-STREAM] Failed to schedule onToolStart: " + ex.getMessage());
				}
			})
			.onToolComplete((toolName, result) -> {
				if (requestCancelled) return;
				log.warning("[UI-STREAM] onToolComplete: " + toolName);
				try {
					Executions.schedule(desktop, e -> {
						streamingMsg.showToolComplete(toolName, result);
					}, new Event("onToolComplete"));
				} catch (Exception ex) {
					log.severe("[UI-STREAM] Failed to schedule onToolComplete: " + ex.getMessage());
				}
			})
			.onToolError((toolName, error) -> {
				if (requestCancelled) return;
				log.warning("[UI-STREAM] onToolError: " + toolName + " - " + error);
				try {
					Executions.schedule(desktop, e -> {
						streamingMsg.showToolError(toolName, error);
					}, new Event("onToolError"));
				} catch (Exception ex) {
					log.severe("[UI-STREAM] Failed to schedule onToolError: " + ex.getMessage());
				}
			})
			.onThinking(thinkingChunk -> {
				if (requestCancelled) return;
				log.warning("[UI-STREAM] onThinking received");
				try {
					Executions.schedule(desktop, e -> {
						streamingMsg.appendThinking(thinkingChunk);
					}, new Event("onThinking"));
				} catch (Exception ex) {
					log.severe("[UI-STREAM] Failed to schedule onThinking: " + ex.getMessage());
				}
			})
			.onThinkingComplete(() -> {
				if (requestCancelled) return;
				log.warning("[UI-STREAM] onThinkingComplete");
				try {
					Executions.schedule(desktop, e -> {
						streamingMsg.completeThinking();
					}, new Event("onThinkingComplete"));
				} catch (Exception ex) {
					log.severe("[UI-STREAM] Failed to schedule onThinkingComplete: " + ex.getMessage());
				}
			})
			.onComplete(() -> {
				log.warning("[UI-STREAM] onComplete - finalizing message");
				try {
					Executions.schedule(desktop, e -> {
						log.warning("[UI-STREAM] Executing onComplete in UI thread");

						// Skip if request was cancelled
						if (requestCancelled) {
							log.warning("[UI-STREAM] Request was cancelled, skipping onComplete");
							return;
						}

						// Finalize streaming message
						streamingMsg.complete();

						// Save AI response to database
						String response = streamingMsg.getContent();
						log.warning("[UI-STREAM] Saving response, length=" + response.length());
						MAIChatEntry aiEntry = MAIChatEntry.createAIResponse(aiChat, response);
						if (threadRootIdSnapshot > 0) {
							aiEntry.setCM_ChatEntryParent_ID(threadRootIdSnapshot);
						}
						aiEntry.saveEx();
						aiChat.saveEx();

						// Re-enable input and show send button
						streamingInProgress = false;
						showSendButton();
						currentStreamingMessage = null;
						inputBox.setDisabled(false);
						sendButton.setDisabled(false);
						inputBox.focus();
						scrollToBottom();
						log.warning("[UI-STREAM] onComplete finished successfully");
					}, new Event("onComplete"));
				} catch (Exception ex) {
					log.severe("[UI-STREAM] Failed to schedule onComplete: " + ex.getMessage());
					ex.printStackTrace();
				}
			})
			.onError(error -> {
				log.severe("[UI-STREAM] onError: " + error.getMessage());
				error.printStackTrace();
				try {
					// Use AIErrorHandler for user-friendly error handling and AD_Issue creation
					final AIErrorResult errorResult = AIErrorHandler.handleError(
						sessionCtx,
						error,
						provider != null ? provider.getName() : "Unknown",
						message,
						aiChat != null ? aiChat.getCM_Chat_ID() : 0
					);

					Executions.schedule(desktop, e -> {
						// Skip if request was cancelled (cancellation triggers error callback)
						if (requestCancelled) {
							log.warning("[UI-STREAM] Request was cancelled, skipping onError");
							return;
						}

						// Show user-friendly error in streaming message with debug tooltip
						// Format: friendly message + warning emoji with tooltip
						String errorDisplay = "\n\n" + errorResult.getUserMessage() +
							" <span class=\"ai-error-ref\" title=\"" + errorResult.getDebugTooltip() +
							"\" style=\"cursor:help; opacity:0.6; font-size:0.8em;\">\u26A0\uFE0F</span>";
						streamingMsg.appendChunk(errorDisplay);
						streamingMsg.complete();

						// Persist partial response with error to database
						persistErrorResponse(streamingMsg.getContent(), errorResult.getUserMessage(), threadRootIdSnapshot);

						// Re-enable input and show send button
						streamingInProgress = false;
						showSendButton();
						currentStreamingMessage = null;
						inputBox.setDisabled(false);
						sendButton.setDisabled(false);

						log.info("[UI-STREAM] Error handled: ref=" + errorResult.getErrorReference() +
								", category=" + errorResult.getCategory() +
								", issueId=" + errorResult.getAD_Issue_ID());
					}, new Event("onError"));
				} catch (Exception ex) {
					log.severe("[UI-STREAM] Failed to schedule onError: " + ex.getMessage());
				}
			})
			.onProgress((stage, detail) -> {
				log.warning("[UI-STREAM] onProgress: " + stage + " - " + detail);
			})
			.build();
		log.warning("[UI-STREAM] Streaming callback built successfully");

		// Start streaming in background and store reference for cancellation
		log.warning("[UI-STREAM] Starting async streaming...");
		log.warning("[UI-STREAM] langchainService: " + (langchainService != null ? "OK" : "NULL!"));
		log.warning("[UI-STREAM] provider: " + (provider != null ? provider.getName() : "NULL!"));
		log.warning("[UI-STREAM] aiChat: " + (aiChat != null ? aiChat.getCM_Chat_ID() : "NULL!"));

		currentRequest = CompletableFuture.runAsync(() -> {
			log.warning("[UI-STREAM] Async task started, calling chatStreamingWithContext...");
			try {
				if (langchainService == null) {
					throw new IllegalStateException("langchainService is null!");
				}
				log.warning("[UI-STREAM] About to call langchainService.chatStreamingWithContext()");
				langchainService.chatStreamingWithContext(
					provider,
					aiChat,
					message,
					contextSnapshot,
					threadRootIdSnapshot,
					callback
				);
				log.warning("[UI-STREAM] chatStreamingWithContext returned successfully");
			} catch (Exception ex) {
				log.severe("[UI-STREAM] Exception in chatStreamingWithContext: " + ex.getMessage());
				ex.printStackTrace();
				// Trigger error callback
				callback.onError(ex);
			} catch (Throwable t) {
				log.severe("[UI-STREAM] THROWABLE in chatStreamingWithContext: " + t.getClass().getName() + ": " + t.getMessage());
				t.printStackTrace();
				callback.onError(new Exception("Unexpected error: " + t.getMessage(), t));
			}
		});
	}

	/**
	 * Send message using LangChain4j batch mode (non-streaming).
	 * Original implementation for backward compatibility.
	 */
	private void sendMessageLangChain4jBatch(String message, JSONObject contextSnapshot,
			int threadRootIdSnapshot, Desktop desktop) {

		CompletableFuture.runAsync(() -> {
			// Declare provider outside try block for access in catch block
			MAIProvider provider = null;
			try {
				// Get MAIChat instance
				MAIChat aiChat = (chat instanceof MAIChat) ?
					(MAIChat) chat :
					new MAIChat(sessionCtx, chat.getCM_Chat_ID(), null);

				// Get default provider
				provider = MAIProvider.getDefault(sessionCtx, null);
				if (provider == null) {
					throw new Exception("No AI provider configured. Please configure an AI provider in the system.");
				}

				// Call LangChain4j service with context
				// Session ID combines chat ID and thread for memory isolation
				ChatResult result = langchainService.chatWithContext(
					provider,
					aiChat,
					message,
					contextSnapshot,
					threadRootIdSnapshot
				);

				// Handle result based on status
				if (result.isBlocked()) {
					// Guardrails blocked the request - show warning
					handleBlockedResponse(desktop, result, threadRootIdSnapshot);
					return;
				}

				if (result.isError()) {
					throw new Exception(result.getResponse());
				}

				// Success - create AI response entry
				String response = result.getResponse();

				// Update thread root ID if it changed (new thread was created)
				if (result.getThreadRootId() != threadRootIdSnapshot && result.getThreadRootId() > 0) {
					currentThreadRootId = result.getThreadRootId();
				}

				// Create AI chat entry with proper thread parent
				MAIChatEntry aiEntry = MAIChatEntry.createAIResponse(aiChat, response);

				if (threadRootIdSnapshot > 0) {
					aiEntry.setCM_ChatEntryParent_ID(threadRootIdSnapshot);
				}

				aiEntry.saveEx();
				aiChat.saveEx();

				// Update UI
				Executions.schedule(desktop, e -> {
					hideLoading();

					// Show warning if content was filtered
					if (result.hasWarning()) {
						showGuardWarning(result.getWarningMessage());
					}

					renderMessage(aiEntry);
					inputBox.setDisabled(false);
					sendButton.setDisabled(false);
					inputBox.focus();
					scrollToBottom();
				}, new Event("onAIResponse"));

			} catch (Exception e) {
				log.log(Level.SEVERE, "LangChain4j AI response failed", e);
				handleErrorResponse(desktop, e, threadRootIdSnapshot, provider, message);
			}
		});
	}

	/**
	 * Handle blocked response from guardrails.
	 */
	private void handleBlockedResponse(Desktop desktop, ChatResult result, int threadRootIdSnapshot) {
		Executions.schedule(desktop, ev -> {
			hideLoading();

			String warningHtml = "<div style='color: #ed6c02; padding: 8px; " +
				"background: #fff4e5; border-radius: 4px; border-left: 3px solid #ed6c02;'>" +
				"<strong>Request blocked:</strong> " + Util.maskHTML(result.getResponse(), true);

			if (result.getViolationType() != null) {
				warningHtml += "<br/><small>Reason: " + Util.maskHTML(result.getViolationType(), true) + "</small>";
			}
			warningHtml += "</div>";

			MChatEntry warningEntry = MAIChatEntry.createAIResponse(chat, warningHtml);
			if (threadRootIdSnapshot > 0) {
				warningEntry.setCM_ChatEntryParent_ID(threadRootIdSnapshot);
			}
			warningEntry.saveEx();
			renderMessage(warningEntry);
			inputBox.setDisabled(false);
			sendButton.setDisabled(false);
		}, new Event("onAIBlocked"));
	}

	/**
	 * Handle error response with user-friendly message and AD_Issue logging.
	 *
	 * @param desktop ZK desktop for UI scheduling
	 * @param e the exception that occurred
	 * @param threadRootIdSnapshot the thread root ID
	 * @param provider the AI provider (can be null)
	 * @param userMessage the original user message (can be null)
	 */
	private void handleErrorResponse(Desktop desktop, Exception e, int threadRootIdSnapshot,
			MAIProvider provider, String userMessage) {

		// Handle error outside of UI thread to create AD_Issue
		AIErrorResult errorResult = AIErrorHandler.handleError(
			sessionCtx,
			e,
			provider != null ? provider.getName() : "Unknown",
			userMessage,
			chat != null ? chat.getCM_Chat_ID() : 0
		);

		Executions.schedule(desktop, ev -> {
			hideLoading();

			// Build user-friendly error display with debug tooltip
			String errorMsg = "<div style='color: #d32f2f; padding: 8px; " +
				"background: #ffebee; border-radius: 4px; border-left: 3px solid #d32f2f;'>" +
				Util.maskHTML(errorResult.getUserMessage(), true) +
				" <span class=\"ai-error-ref\" title=\"" + errorResult.getDebugTooltip() +
				"\" style=\"cursor:help; opacity:0.6;\">\u26A0\uFE0F</span></div>";

			MChatEntry errorEntry = MAIChatEntry.createAIResponse(chat, errorMsg);

			// Set thread parent for error entry
			if (threadRootIdSnapshot > 0) {
				errorEntry.setCM_ChatEntryParent_ID(threadRootIdSnapshot);
			}

			errorEntry.saveEx();
			renderMessage(errorEntry);
			inputBox.setDisabled(false);
			sendButton.setDisabled(false);

			log.info("Error handled: ref=" + errorResult.getErrorReference() +
					", category=" + errorResult.getCategory() +
					", issueId=" + errorResult.getAD_Issue_ID());
		}, new Event("onAIError"));
	}

	/**
	 * Show guardrail warning in UI.
	 */
	private void showGuardWarning(String warningMessage) {
		if (warningMessage == null || warningMessage.isEmpty()) {
			return;
		}

		String warningHtml = "<div style='color: #ed6c02; padding: 6px 10px; margin-bottom: 8px; " +
			"background: #fff4e5; border-radius: 4px; font-size: 12px;'>" +
			"⚠️ " + Util.maskHTML(warningMessage, true) + "</div>";

		Html warningDiv = new Html(warningHtml);
		messagesContainer.appendChild(warningDiv);
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
	 * Handles both local threads (positive IDs) and shared chats (negative IDs)
	 */
	private void switchThread() {
		if (threadSelector.getSelectedItem() == null) {
			return;
		}

		Object value = threadSelector.getSelectedItem().getValue();
		if (value instanceof Integer) {
			int selectedValue = (Integer) value;

			if (selectedValue < 0) {
				// Negative value = shared chat ID (ADR-036)
				int sharedChatId = -selectedValue;
				switchToSharedChat(sharedChatId);
			} else {
				// Positive value = thread ID in current chat
				currentThreadRootId = selectedValue;
				log.fine("Switched to thread: " + currentThreadRootId);

				// Re-render messages for this thread
				renderMessages();
			}
		}
	}

	/**
	 * Switch to a shared chat (ADR-036)
	 * @param chatId the shared chat ID to switch to
	 */
	private void switchToSharedChat(int chatId) {
		try {
			// Load the shared chat
			MChat sharedChat = new MChat(sessionCtx, chatId, null);
			if (sharedChat.get_ID() == 0) {
				log.warning("Shared chat not found: " + chatId);
				return;
			}

			// Check access
			ChatAccess sharedAccess = ChatAccessService.get().getAccess(sessionCtx, sharedChat);
			if (sharedAccess == ChatAccess.NONE) {
				Clients.showNotification(
					Msg.getMsg(sessionCtx, "AccessCannotRead"),
					"error", this, "middle_center", 3000);
				return;
			}

			// Switch to the shared chat
			chat = sharedChat;
			currentAccess = sharedAccess;
			currentThreadRootId = 0; // Reset thread selection

			// Update UI
			updateAccessIndicator();
			loadThreadList();
			renderMessages();

			log.fine("Switched to shared chat: " + chatId + " with access: " + sharedAccess);
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to switch to shared chat", e);
			Clients.showNotification(
				Msg.getMsg(sessionCtx, "Error") + ": " + e.getMessage(),
				"error", this, "middle_center", 3000);
		}
	}

	/**
	 * Load thread list into dropdown
	 * Threads are identified by their root message (first message with no parent)
	 * Also loads shared chats (ADR-036)
	 */
	private void loadThreadList() {
		if (chat == null) {
			return;
		}

		threadSelector.getItems().clear();

		// Add "New Thread" option (only if user can write)
		if (currentAccess.ordinal() >= ChatAccess.WRITE.ordinal()) {
			Comboitem newItem = new Comboitem("+ New Thread");
			newItem.setValue(0);
			threadSelector.appendChild(newItem);
		}

		// --- My Chats section ---
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

		// --- Shared with me section (ADR-036) ---
		loadSharedChatsSection();

		// If no thread selected and we have threads, select the most recent
		if (threadSelector.getSelectedItem() == null && rootEntries.size() > 0) {
			currentThreadRootId = rootEntries.get(rootEntries.size() - 1).getCM_ChatEntry_ID();
			threadSelector.setSelectedIndex(currentAccess.ordinal() >= ChatAccess.WRITE.ordinal() ? 1 : 0);
		} else if (rootEntries.size() == 0) {
			// No threads yet, select "New Thread" if available
			currentThreadRootId = 0;
			if (threadSelector.getItemCount() > 0) {
				threadSelector.setSelectedIndex(0);
			}
		}
	}

	/**
	 * Load shared chats section into thread selector (ADR-036)
	 */
	private void loadSharedChatsSection() {
		try {
			List<MChat> sharedChats = ChatAccessService.get().getSharedChats(sessionCtx, null);

			if (sharedChats.isEmpty()) {
				return;
			}

			// Add separator
			Comboitem separator = new Comboitem("─── " + Msg.getMsg(sessionCtx, "SharedWithMe") + " ───");
			separator.setDisabled(true);
			separator.setStyle("font-style: italic; color: #888;");
			threadSelector.appendChild(separator);

			// Add shared chats
			for (MChat sharedChat : sharedChats) {
				ChatAccess sharedAccess = ChatAccessService.get().getAccess(sessionCtx, sharedChat);

				// Build label with access indicator
				String accessIcon = (sharedAccess == ChatAccess.READ) ? "📖 " : "✏️ ";
				String label = sharedChat.getDescription();
				if (label == null || label.trim().isEmpty()) {
					label = "Chat " + sharedChat.get_ID();
				} else if (label.length() > 40) {
					label = label.substring(0, 37) + "...";
				}

				// Add owner info
				MUser owner = MUser.get(sessionCtx, sharedChat.getCreatedBy());
				String ownerName = owner != null ? owner.getName() : "Unknown";
				label = accessIcon + label + " (from " + ownerName + ")";

				Comboitem item = new Comboitem(label);
				// Store chat ID as negative to distinguish from thread IDs
				item.setValue(-sharedChat.get_ID());
				threadSelector.appendChild(item);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to load shared chats", e);
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

	// ========================================================================
	// Access Control UI (ADR-036)
	// ========================================================================

	/**
	 * Build HTML for access indicator based on access level
	 * @param access current access level
	 * @return HTML string for indicator
	 */
	private String buildAccessIndicatorHtml(ChatAccess access) {
		if (access == ChatAccess.OWNER || access == ChatAccess.WRITE) {
			// No indicator needed for full access
			return "<div id='accessBadge_" + getUuid() + "' style='display: none;'></div>";
		}

		if (access == ChatAccess.READ) {
			// Read-only badge
			return "<div id='accessBadge_" + getUuid() + "' style='display: flex; align-items: center; gap: 6px; " +
				"padding: 6px 12px; background: #FFF3E0; border-radius: 4px; margin-bottom: 8px; " +
				"font-size: 11px; color: #E65100;'>" +
				"<i class='z-icon-Lock' style='font-size: 12px;'></i> " +
				"<span>" + Msg.getMsg(sessionCtx, "ReadOnly") + "</span>" +
				"</div>";
		}

		// No access - should not normally be shown
		return "<div id='accessBadge_" + getUuid() + "' style='display: none;'></div>";
	}

	/**
	 * Update access indicator and input state based on current access level
	 */
	private void updateAccessIndicator() {
		if (accessIndicator == null) {
			return;
		}

		// Update indicator HTML
		accessIndicator.setContent(buildAccessIndicatorHtml(currentAccess));

		// Disable input for read-only access
		boolean canWrite = currentAccess.ordinal() >= ChatAccess.WRITE.ordinal();
		inputBox.setDisabled(!canWrite);
		sendButton.setDisabled(!canWrite);
		newThreadButton.setDisabled(!canWrite);

		if (!canWrite) {
			inputBox.setPlaceholder(Msg.getMsg(sessionCtx, "ReadOnly"));
		} else {
			inputBox.setPlaceholder(Msg.getMsg(sessionCtx, "AIChatPlaceholder"));
		}

		log.fine("Access indicator updated: " + currentAccess + ", canWrite=" + canWrite);
	}

	/**
	 * Get current access level
	 * @return current ChatAccess level
	 */
	public ChatAccess getCurrentAccess() {
		return currentAccess;
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

	// ========================================================================
	// Stop Button / Request Cancellation (ADR-031)
	// ========================================================================

	/**
	 * Show the stop button and hide the send button.
	 * Called when AI processing starts.
	 */
	private void showStopButton() {
		sendButton.setStyle(sendButton.getStyle().replace("display: flex", "display: none"));
		stopButton.setStyle(stopButton.getStyle().replace("display: none", "display: flex"));
	}

	/**
	 * Show the send button and hide the stop button.
	 * Called when AI processing completes or is cancelled.
	 */
	private void showSendButton() {
		stopButton.setStyle(stopButton.getStyle().replace("display: flex", "display: none"));
		sendButton.setStyle(sendButton.getStyle().replace("display: none", "display: flex"));
	}

	/**
	 * Cancel the current AI request.
	 * Called when stop button is clicked.
	 */
	private void cancelCurrentRequest() {
		log.info("[CANCEL] Cancel request initiated, streamingInProgress=" + streamingInProgress);
		requestCancelled = true;
		streamingInProgress = false;

		// Cancel the CompletableFuture if running (may not help much since streaming is async)
		if (currentRequest != null && !currentRequest.isDone()) {
			currentRequest.cancel(true);
			log.info("[CANCEL] CompletableFuture cancelled");
		}

		// Mark streaming message as cancelled and persist partial response
		if (currentStreamingMessage != null) {
			currentStreamingMessage.markCancelled();
			log.info("[CANCEL] Streaming message marked as cancelled");

			// Persist partial response to database
			persistCancelledResponse();
		}

		// Reset UI state
		hideLoading();
		showSendButton();
		sendButton.setDisabled(false);
		inputBox.setDisabled(false);
		inputBox.focus();
		currentStreamingMessage = null;
	}

	// Note: parseErrorMessage and formatKnownError methods have been moved to
	// com.cloudempiere.ai.error.AIErrorHandler for better separation of concerns
	// and to enable AD_Issue creation with error tracking.

	/**
	 * Persist the cancelled/partial AI response to the database.
	 * Called when user cancels a streaming request.
	 */
	private void persistCancelledResponse() {
		if (currentStreamingMessage == null) {
			return;
		}

		try {
			// Get the partial content (may be empty if cancelled immediately)
			String partialContent = currentStreamingMessage.getContent();
			if (partialContent == null) {
				partialContent = "";
			}

			// Append cancellation notice to persisted content
			String persistedContent = partialContent.isEmpty()
				? "_AI request cancelled_"
				: partialContent + "\n\n_AI request cancelled_";

			// Get MAIChat instance
			MAIChat aiChat = (chat instanceof MAIChat) ?
				(MAIChat) chat :
				new MAIChat(sessionCtx, chat.getCM_Chat_ID(), null);

			// Create AI chat entry with partial response
			MAIChatEntry aiEntry = MAIChatEntry.createAIResponse(aiChat, persistedContent);

			// Set thread parent if applicable
			if (currentThreadRootId > 0) {
				aiEntry.setCM_ChatEntryParent_ID(currentThreadRootId);
			}

			aiEntry.saveEx();
			aiChat.saveEx();

			log.info("[CANCEL] Partial response persisted, length=" + partialContent.length());
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to persist cancelled response", e);
		}
	}

	/**
	 * Persist the error AI response to the database.
	 * Called when streaming encounters an error.
	 *
	 * @param partialContent the partial content received before the error
	 * @param errorMessage the user-friendly error message
	 * @param threadRootId the thread root ID for proper threading
	 */
	private void persistErrorResponse(String partialContent, String errorMessage, int threadRootId) {
		try {
			// Build content with error appended
			String content = partialContent;
			if (content == null) {
				content = "";
			}

			// The error is already appended to partialContent via appendChunk,
			// so we just need to persist it as-is
			if (content.isEmpty()) {
				content = "**Error:** " + errorMessage;
			}

			// Get MAIChat instance
			MAIChat aiChat = (chat instanceof MAIChat) ?
				(MAIChat) chat :
				new MAIChat(sessionCtx, chat.getCM_Chat_ID(), null);

			// Create AI chat entry with error response
			MAIChatEntry aiEntry = MAIChatEntry.createAIResponse(aiChat, content);

			// Set thread parent if applicable
			if (threadRootId > 0) {
				aiEntry.setCM_ChatEntryParent_ID(threadRootId);
			}

			aiEntry.saveEx();
			aiChat.saveEx();

			log.info("[ERROR] Error response persisted, length=" + content.length());
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to persist error response", e);
		}
	}
}
