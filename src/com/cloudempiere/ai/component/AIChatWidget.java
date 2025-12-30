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

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Combobox;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.model.MChat;
import org.compiere.model.MChatEntry;
import org.compiere.model.MQuery;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.json.JSONArray;
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
import com.cloudempiere.ai.error.AIErrorHandler;
import com.cloudempiere.ai.error.AIErrorHandler.AIErrorResult;
import com.cloudempiere.ai.health.AIPluginHealthService;
import com.cloudempiere.ai.health.AIUIService;
import com.cloudempiere.ai.health.Result;
import com.cloudempiere.ai.model.MAIChat;
import com.cloudempiere.ai.model.MAIChatEntry;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;
import com.cloudempiere.ai.provider.langchain4j.AIService;
import com.cloudempiere.ai.provider.langchain4j.AIService.ChatResult;
import com.cloudempiere.ai.service.ChatAccessService;
import com.cloudempiere.ai.service.IChatAccessService.ChatAccess;
import com.cloudempiere.ai.util.MarkdownTableRenderer;
import com.cloudempiere.ai.util.ZoomLinkProcessor;

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

	/** Flag indicating if the current request was cancelled (atomic for thread-safety) */
	private final java.util.concurrent.atomic.AtomicBoolean requestCancelled = new java.util.concurrent.atomic.AtomicBoolean(false);

	/** Flag indicating if streaming is in progress */
	private volatile boolean streamingInProgress = false;

	/** Lock for thread management operations to prevent race conditions */
	private final Object threadLock = new Object();

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

	/** Reference to window container for tab event listening (self-managing context) */
	private Component windowContainer = null;

	/** Event listener for tab selection events (self-managing context) */
	private EventListener<Event> tabSelectionListener = null;

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

	/** Flag indicating if widget is in unavailable state */
	private boolean unavailableState = false;

	/**
	 * Initialize the widget
	 */
	private void init() {
		setSclass("ai-chat-widget"); // CSS defined in fragment/custom.css.dsp
		ZKUpdateUtil.setVflex(this, "1");
		ZKUpdateUtil.setHflex(this, "1");
		// Styles now applied via CSS class instead of inline styles
		// See: com.cloudempiere.ai.theme/theme/default/css/fragment/custom.css.dsp

		// Capture session context at initialization (important for language, client, etc.)
		// Use Env.getCtx() which should have the session context when called from UI thread
		sessionCtx = Env.getCtx();

		// Check plugin health first (ADR-050, ADR-051: Defensive programming)
		Result<Boolean> availability = AIUIService.checkAvailability();
		if (!availability.isSuccess()) {
			showUnavailableState(availability.getMessage());
			return;
		}

		// Register zoom event listener for clickable record links
		addEventListener(ON_ZOOM, this);

		dateFormat = DisplayType.getDateFormat(DisplayType.DateTime);

		// Load Markdown rendering libraries (marked.js + Prism.js for syntax highlighting)
		loadMarkdownLibraries();

		// Note: CSS is automatically loaded via fragment/custom.css.dsp extension point
		// See: com.cloudempiere.ai.theme/theme/default/css/fragment/custom.css.dsp

		// Initialize AI service (LangChain4j) - use defensive wrapper
		Result<AIService> serviceResult = AIUIService.safeExecute(
				() -> AIService.getInstance(), "chat-init");
		if (!serviceResult.isSuccess()) {
			showUnavailableState(serviceResult.getMessage());
			return;
		}
		langchainService = serviceResult.getValueOrDefault(null);
		if (langchainService == null) {
			showUnavailableState(AIUIService.getDisplayStatus());
			return;
		}
		log.fine("AIChatWidget initialized with LangChain4j service");

		// Context indicator (if enabled)
		if (contextEnabled) {
			contextIndicator = new Html();
			contextIndicator.setId("aiContextIndicator_" + getUuid());
			contextIndicator.setContent(
				"<div class='ai-context-indicator'>" +
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
		threadControlBar.setSclass("ai-thread-control-bar");

		// Thread selector dropdown
		threadSelector = new Combobox();
		threadSelector.setPlaceholder("Select conversation...");
		threadSelector.setSclass("ai-thread-selector");
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
				appendChild(messagesContainer);

		// Loading indicator (hidden by default)
		loadingIndicator = new Html();
		loadingIndicator.setContent("<div class='ai-loading-container'>" +
			"<div class='ai-loading-content'>" +
			"<div class='ai-loading-avatar'></div>" +
			"<span class='ai-loading-text'>" +
			"<i>" + Msg.getMsg(Env.getCtx(), "AIThinking") + "</i></span></div></div>");
		messagesContainer.appendChild(loadingIndicator);

		// Input area - stays at bottom (flex-shrink: 0)
		Hlayout inputArea = new Hlayout();
		inputArea.setSclass("ai-input-area");

		inputBox = new Textbox();
		inputBox.setSclass("ai-input-box");
		inputBox.setPlaceholder(Msg.getMsg(Env.getCtx(), "AIChatPlaceholder"));
		ZKUpdateUtil.setHflex(inputBox, "1");
		inputBox.setRows(1);
		inputBox.setMultiline(false);
		inputBox.addEventListener(Events.ON_OK, this); // Enter key to send

		// Button container - holds send/stop buttons in same position
		Div buttonContainer = new Div();
		buttonContainer.setSclass("ai-button-container");

		sendButton = new Button();
		sendButton.addEventListener(Events.ON_CLICK, this);
		sendButton.setSclass("ai-send-btn");
		if (ThemeManager.isUseFontIconForImage())
			sendButton.setIconSclass("z-icon-Send-White");
		else
			sendButton.setImage(ThemeManager.getThemeResource("images/Send-White.png"));
		
		// Stop button (hidden by default, shown during AI processing)
		stopButton = new Button();
		stopButton.addEventListener(Events.ON_CLICK, this);
		stopButton.setSclass("ai-stop-btn");
		if (ThemeManager.isUseFontIconForImage())
			stopButton.setIconSclass("z-icon-Square-White");
		else
			stopButton.setImage(ThemeManager.getThemeResource("images/Cancel24.png"));
		stopButton.setSclass("ai-stop-btn ai-hidden");
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
			divider.setContent("<div class='ai-message-divider'></div>");
			messagesContainer.insertBefore(divider, loadingIndicator);
		}

		Div msgDiv = new Div();
		msgDiv.setSclass(isAI ? "ai-message" : "user-message");

		// Updated styling based on Figma design screenshot (scaled to 12px base font)
		if (isAI) {
			msgDiv.setSclass((msgDiv.getSclass() != null ? msgDiv.getSclass() : "") + " ai-message-content");
		} else {
			msgDiv.setSclass((msgDiv.getSclass() != null ? msgDiv.getSclass() : "") + " user-message-content");
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
			sb.append("<div class='ai-message-header'>");
			sb.append("<img src='");
			// Use Executions.encodeURL to convert ZK ~./ resource path to browser-accessible URL
			String logoUrl = ThemeManager.THEME_PATH_PREFIX+ThemeManager.getTheme()+"/images/clde-logo-icon-vector.svg";
			sb.append(Executions.encodeURL(logoUrl));
			sb.append("' class='ai-message-header-logo'/>");
			sb.append("<span class='ai-message-header-name'>");
			sb.append(Util.maskHTML(getUserName(entry), true));
			sb.append("</span></div>");
		}

		// Message body with updated typography (scaled to 12px base)
		sb.append("<div class='");
		sb.append(isAI ? "ai-message-body" : "user-message-body");
		sb.append("'>");

		String messageText = entry.getCharacterData();
		if (messageText != null) {
			if (isAI) {
				// AI messages: Pre-render tables with zoom links, then process remaining zoom links
				// This matches the approach in AIChatStreamingMessage.renderFinalMarkdown() (ADR-039)

				String processedText = messageText;
				log.warning("[ZOOM-DEBUG] Original message text: " + messageText.substring(0, Math.min(200, messageText.length())));

				// Step 1: Pre-render tables (with zoom links in cells)
				if (MarkdownTableRenderer.containsTable(processedText)) {
					log.warning("[ZOOM-DEBUG] Table detected, rendering...");
					MarkdownTableRenderer.setContext(sessionCtx);
					MarkdownTableRenderer.setWidgetId(getUuid());
					try {
						processedText = MarkdownTableRenderer.renderTables(processedText);
						log.warning("[ZOOM-DEBUG] After table rendering: " + processedText.substring(0, Math.min(200, processedText.length())));
					} finally {
						MarkdownTableRenderer.clearZoomContext();
					}
				}

				// Step 2: Process zoom links outside tables
				String beforeZoomProcessing = processedText;
				processedText = ZoomLinkProcessor.processZoomLinks(processedText, sessionCtx, getUuid());
				if (!processedText.equals(beforeZoomProcessing)) {
					log.warning("[ZOOM-DEBUG] Zoom links processed - text changed");
					log.warning("[ZOOM-DEBUG] After zoom processing: " + processedText.substring(0, Math.min(200, processedText.length())));
				} else {
					log.warning("[ZOOM-DEBUG] No zoom links found or processed");
				}

				// FUTURE (ADR-039): Pattern-based extraction for natural references like "SO-1234", "Invoice 5678"
				// Currently bypassed - requires vector DB for fast lookup across 2000+ tables/AD elements.
				// See RecordReferenceExtractor and ChatRecordLinkRenderer for the implementation.
				// Uncomment when vector DB caching is available:
				// processedText = ChatRecordLinkRenderer.extractAndRender(processedText, sessionCtx, getUuid());

				// Step 3: Render markdown while preserving HTML (tables and zoom links)
				sb.append(renderMarkdownPreservingHTML(processedText));
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

			sb.append("<div class='ai-copy-button-container'>");
			sb.append("<div class='ai-copy-button' onclick=\"(function(btn){");
			sb.append("var orig=btn.innerHTML;");
			sb.append("navigator.clipboard.writeText('").append(jsEscapedText).append("').then(function(){");
			sb.append("btn.innerHTML='<span class=\\'ai-copy-success\\'>Copied!</span>';");
			sb.append("setTimeout(function(){btn.innerHTML=orig;},2000);");
			sb.append("}).catch(function(err){console.error('Copy failed:',err);});");
			sb.append("})(this);\">");
			if (ThemeManager.isUseFontIconForImage()) {
				sb.append("<i class='z-icon-Copy ai-copy-icon'></i>");
			} else {
				sb.append("<img src='");
				sb.append(ThemeManager.getThemeResource("images/Copy.png"));
				sb.append("' class='ai-copy-icon'/>");
			}
			sb.append("<span class='ai-copy-text'>Copy</span>");
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
		loadingIndicator.setContent("<div class='ai-loading-container'>" +
			"<div class='ai-loading-content'>" +
			"<div class='ai-loading-avatar'></div>" +
			"<span class='ai-loading-text'>" +
			"<i>" + Msg.getMsg(Env.getCtx(), "AIThinking") + "</i></span></div></div>");
		scrollToBottom();
	}

	/**
	 * Hide loading indicator
	 */
	private void hideLoading() {
		loadingIndicator.setContent("<div class='ai-loading-container ai-hidden'></div>");
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
	 * Handle zoom event from clickable record links.
	 *
	 * <p>Supports three formats (in order of preference):
	 * <ol>
	 *   <li><b>Pre-built MQuery</b> (most efficient): Direct MQuery object passed as event data.
	 *       Same pattern as ChartRendererServiceImpl. No parsing required, immediate zoom.</li>
	 *   <li><b>iDempiere standard format</b>: JSONObject with "data" array [columnName, recordId].
	 *       Matches report.js / ZoomCommand pattern. Requires MQuery construction.</li>
	 *   <li><b>Legacy format</b>: JSONObject with tableId and recordId properties.
	 *       Backward compatibility only.</li>
	 * </ol>
	 *
	 * <p>The MQuery format enables pre-building queries (like charts do) for better performance
	 * and more control over zoom behavior (e.g., pre-set zoom window ID, additional restrictions).
	 *
	 * <p>Uses same logic as {@code ZoomCommand} and {@code ChartRendererServiceImpl.ZoomListener}.
	 *
	 * @param event zoom event containing MQuery, JSON data, or legacy format
	 */
	private void handleZoomEvent(Event event) {
		try {
			Object eventData = event.getData();
			log.warning("[Zoom Handler] Event received, data type: " +
				(eventData != null ? eventData.getClass().getName() : "null"));

			// Option 1: Direct MQuery object (same as ChartRendererServiceImpl pattern)
			// Pre-built query passed directly from event - most efficient approach
			//
			// Usage example:
			//   MQuery query = new MQuery("C_Order");
			//   query.addRestriction("C_Order_ID", MQuery.EQUAL, 5678);
			//   query.setRecordCount(1);
			//   query.setZoomTableName("C_Order");
			//   query.setZoomColumnName("C_Order_ID");
			//   query.setZoomValue(5678);
			//   Events.sendEvent(new Event(ON_ZOOM, this, query));
			//
			// This is the same pattern used by ChartRendererServiceImpl.ZoomListener
			// and enables pre-building queries for better performance.
			if (eventData instanceof MQuery) {
				MQuery query = (MQuery) eventData;
				log.warning("[Zoom Handler] Received pre-built MQuery: " + query.toString());

				// Validate query has required data
				if (query.getTableName() != null && !query.getTableName().isEmpty()) {
					log.warning("[Zoom Handler] Calling AEnv.zoom() with MQuery for table: " +
						query.getTableName());
					AEnv.zoom(query);
					log.warning("[Zoom Handler] AEnv.zoom() completed");
					return;
				} else {
					log.warning("Invalid MQuery - no table name specified");
					return;
				}
			}

			// Option 2: Standard iDempiere format: {data: [columnName, recordId]}
			// This matches report.js / ZoomCommand pattern
			if (eventData instanceof JSONObject) {
				JSONObject jsonData = (JSONObject) eventData;
				log.warning("[Zoom Handler] JSON data: " + jsonData.toString());

				// Check for standard iDempiere format first
				if (jsonData.has("data")) {
					Object dataObj = jsonData.get("data");
					JSONArray data = null;

					if (dataObj instanceof JSONArray) {
						data = (JSONArray) dataObj;
					} else if (dataObj instanceof String) {
						// Parse if it came as string
						data = new JSONArray((String) dataObj);
					}

					if (data != null && data.length() >= 2) {
						String columnName = data.getString(0);  // e.g., "C_Order_ID"
						String valueStr = data.getString(1);    // e.g., "1234"
						log.warning("[Zoom Handler] columnName=" + columnName + ", valueStr=" + valueStr);

						// Derive table name from column name (same as MQuery.getZoomTableName)
						String tableName = MQuery.getZoomTableName(columnName);
						log.warning("[Zoom Handler] Resolved tableName=" + tableName);

						// Parse record ID
						int recordId = 0;
						try {
							recordId = Integer.parseInt(valueStr);
						} catch (NumberFormatException e) {
							log.warning("Cannot parse recordId: " + valueStr);
							return;
						}

						if (recordId > 0) {
							log.warning("[Zoom Handler] Calling AEnv.zoom() for " + tableName + "#" + recordId);

							// Create MQuery and zoom (same pattern as ZoomCommand)
							MQuery query = new MQuery(tableName);
							query.addRestriction(columnName, MQuery.EQUAL, recordId);
							query.setRecordCount(1);
							query.setZoomTableName(tableName);
							query.setZoomColumnName(columnName);
							query.setZoomValue(recordId);

							AEnv.zoom(query);
							log.warning("[Zoom Handler] AEnv.zoom() completed");
							return;
						}
					}
				}

				// Legacy format: {tableId: X, recordId: Y}
				if (jsonData.has("tableId") && jsonData.has("recordId")) {
					int tableId = jsonData.optInt("tableId", 0);
					int recordId = jsonData.optInt("recordId", 0);

					if (tableId > 0 && recordId > 0) {
						log.fine("Zoom request (legacy format): tableId=" + tableId + ", recordId=" + recordId);
						AEnv.zoom(tableId, recordId);
						return;
					}
				}

				log.warning("Invalid zoom event data format: " + jsonData.toString());
			} else {
				log.warning("Zoom event data is not a JSON object: " +
					(eventData != null ? eventData.getClass().getName() : "null"));
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to handle zoom event", e);
			Clients.showNotification(Msg.getMsg(Env.getCtx(), "Error") + ": " + e.getMessage(),
				"error", this, null, -1);
		}
	}

	/**
	 * Zoom to a record using a pre-built MQuery.
	 * This is the most efficient zoom method as it requires no parsing or query construction.
	 *
	 * <p><b>Usage Example:</b>
	 * <pre>
	 * // Create query for specific order
	 * MQuery query = new MQuery("C_Order");
	 * query.addRestriction("C_Order_ID", MQuery.EQUAL, 5678);
	 * query.setRecordCount(1);
	 * query.setZoomTableName("C_Order");
	 * query.setZoomColumnName("C_Order_ID");
	 * query.setZoomValue(5678);
	 *
	 * // Zoom to the record
	 * chatWidget.zoomToRecord(query);
	 * </pre>
	 *
	 * <p>This is the same pattern used by ChartRendererServiceImpl for chart drill-down.
	 *
	 * @param query pre-built MQuery with all restrictions and zoom metadata
	 * @see org.idempiere.zk.billboard.chart.ChartRendererServiceImpl.ZoomListener
	 */
	public void zoomToRecord(MQuery query) {
		if (query == null) {
			log.warning("Cannot zoom: MQuery is null");
			return;
		}

		if (query.getTableName() == null || query.getTableName().isEmpty()) {
			log.warning("Cannot zoom: MQuery has no table name");
			return;
		}

		// Fire zoom event with MQuery
		Events.sendEvent(new Event(ON_ZOOM, this, query));
	}

	/**
	 * Zoom to a record by table and record ID.
	 * Convenience method that builds an MQuery and calls zoomToRecord().
	 *
	 * <p><b>Usage Example:</b>
	 * <pre>
	 * chatWidget.zoomToRecord("C_Order", 5678);
	 * </pre>
	 *
	 * @param tableName table name (e.g., "C_Order")
	 * @param recordId record ID
	 */
	public void zoomToRecord(String tableName, int recordId) {
		if (tableName == null || tableName.isEmpty()) {
			log.warning("Cannot zoom: table name is empty");
			return;
		}

		if (recordId <= 0) {
			log.warning("Cannot zoom: invalid record ID: " + recordId);
			return;
		}

		// Build column name from table name (standard iDempiere convention)
		String columnName = tableName + "_ID";

		// Create MQuery using standard pattern
		MQuery query = new MQuery(tableName);
		query.addRestriction(columnName, MQuery.EQUAL, recordId);
		query.setRecordCount(1);
		query.setZoomTableName(tableName);
		query.setZoomColumnName(columnName);
		query.setZoomValue(recordId);

		zoomToRecord(query);
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

		// SAFETY: Atomic operation to prevent race conditions with rapid messages
		final int threadIdForRequest;
		synchronized (threadLock) {
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
			}

			// Capture thread ID before async operation starts
			threadIdForRequest = currentThreadRootId;

			// Clear input
			inputBox.setText("");

			// Show user message immediately
			renderMessage(userEntry);
			showLoading();
			scrollToBottom();
		}

		// Reload thread list OUTSIDE synchronized block to avoid blocking
		if (threadIdForRequest != 0 && currentThreadRootId == threadIdForRequest) {
			loadThreadList();
		}

		// Send message using LangChain4j service (uses captured threadIdForRequest)
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
		requestCancelled.set(false);
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
		// Pass context, widget ID for zoom link processing (ADR-039), and chat ID for language detection (ADR-037)
		final AIChatStreamingMessage streamingMsg = new AIChatStreamingMessage(sessionCtx, getUuid(),
				chat != null ? chat.getCM_Chat_ID() : 0);
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
				divider.setContent("<div class='ai-message-divider'></div>");
				messagesContainer.insertBefore(divider, loadingIndicator);
			}

			// Add AI message header
			Div msgDiv = new Div();
			msgDiv.setSclass("ai-message");
			msgDiv.setSclass((msgDiv.getSclass() != null ? msgDiv.getSclass() : "") + " ai-streaming-message");

			// Header with logo and agent name
			Html header = new Html();
			String logoUrl = ThemeManager.THEME_PATH_PREFIX + ThemeManager.getTheme() + "/images/clde-logo-icon-vector.svg";
			header.setContent(
				"<div class='ai-message-header'>" +
				"<img src='" + Executions.encodeURL(logoUrl) + "' class='ai-message-header-logo'/>" +
				"<span class='ai-message-header-name'>" + agentName + "</span>" +
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
				if (requestCancelled.get()) {
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
				if (requestCancelled.get()) return;
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
				if (requestCancelled.get()) return;
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
				if (requestCancelled.get()) return;
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
				if (requestCancelled.get()) return;
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
				if (requestCancelled.get()) return;
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
						if (requestCancelled.get()) {
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
						if (requestCancelled.get()) {
							log.warning("[UI-STREAM] Request was cancelled, skipping onError");
							return;
						}

						// Show user-friendly error in streaming message with debug tooltip
						// Format: friendly message + warning emoji with tooltip
						// Only add separator if there's existing content (partial AI response)
						String separator = streamingMsg.getContent().trim().isEmpty() ? "" : "\n\n";
						String errorDisplay = separator + errorResult.getUserMessage() +
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

			String warningHtml = "<div class='ai-warning-message'>" +
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
			String errorMsg = "<div class='ai-error-message'>" +
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
	 * Show unavailable state when AI service is not ready.
	 * (ADR-050, ADR-051: Defensive programming - graceful degradation)
	 *
	 * @param message User-friendly explanation of why AI is unavailable
	 */
	private void showUnavailableState(String message) {
		unavailableState = true;

		// Clear any existing children
		getChildren().clear();

		// Create a centered container for the unavailable message
		Vlayout container = new Vlayout();
		container.setSclass("ai-unavailable-container");

		// Icon
		Html iconHtml = new Html("<div class='ai-unavailable-icon'>" +
				"\uD83D\uDEAB</div>"); // 🚫 emoji
		container.appendChild(iconHtml);

		// Title
		String title = Msg.getMsg(Env.getCtx(), "AIServiceUnavailable");
		if (title == null || title.equals("AIServiceUnavailable")) {
			title = "AI Assistant Unavailable";
		}
		Html titleHtml = new Html("<div class='ai-unavailable-title'>" + Util.maskHTML(title, true) + "</div>");
		container.appendChild(titleHtml);

		// Message
		String displayMessage = message != null ? message : "AI features are currently unavailable. Please try again later.";
		Html messageHtml = new Html("<div class='ai-unavailable-message'>" + Util.maskHTML(displayMessage, true) + "</div>");
		container.appendChild(messageHtml);

		// Retry button (allows user to check availability again)
		Button retryButton = new Button(Msg.getMsg(Env.getCtx(), "Retry"));
		retryButton.setSclass("ai-retry-btn");
		retryButton.addEventListener(Events.ON_CLICK, evt -> {
			// Re-check availability and re-initialize if now available
			AIPluginHealthService health = AIPluginHealthService.getInstance();
			if (health != null) {
				health.refresh();
			}
			Result<Boolean> availability = AIUIService.checkAvailability();
			if (availability.isSuccess()) {
				unavailableState = false;
				getChildren().clear();
				init();
			} else {
				// Update the message with current status
				Clients.showNotification(availability.getMessage(), "warning", null, null, 3000);
			}
		});
		container.appendChild(retryButton);

		appendChild(container);

		log.warning("AIChatWidget showing unavailable state: " + displayMessage);
	}

	/**
	 * Show guardrail warning in UI.
	 */
	private void showGuardWarning(String warningMessage) {
		if (warningMessage == null || warningMessage.isEmpty()) {
			return;
		}

		String warningHtml = "<div class='ai-warning-inline'>" +
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
			separator.setSclass("ai-thread-separator");
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

	// =========================================================================================
	// SELF-MANAGING CONTEXT TRACKING (ZK Desktop Events)
	// =========================================================================================
	// ADR-052: Plugin is 100% standalone - no core code dependencies
	// Widget discovers Desktop/WindowContainer and subscribes to tab events automatically

	/**
	 * ZK Lifecycle hook - called when component is attached to a page.
	 * <p>Sets up automatic context tracking by discovering the window container
	 * and subscribing to tab selection events.
	 *
	 * <p><strong>Timing:</strong> Component discovery is deferred using Executions.schedule()
	 * to ensure the component tree is fully initialized before searching for WindowContainer.
	 */
	@Override
	public void onPageAttached(org.zkoss.zk.ui.Page newpage, org.zkoss.zk.ui.Page oldpage) {
		super.onPageAttached(newpage, oldpage);

		// Only set up context tracking if enabled
		if (!contextEnabled) {
			return;
		}

		log.info("AI Chat Widget: Initializing context tracking");

		// Defer component discovery to allow component tree to fully stabilize
		// This prevents timing issues where WindowContainer may not be attached yet
		Executions.schedule(getDesktop(), new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				setupContextTracking();
			}
		}, new Event("onSetupContextTracking"));
	}

	/**
	 * Set up context tracking after component tree has stabilized.
	 * Called via Executions.schedule() from onPageAttached().
	 */
	private void setupContextTracking() {
		try {
			// Discover window container in component tree
			windowContainer = discoverWindowContainer();

			if (windowContainer != null) {
				// Create tab selection listener
				tabSelectionListener = new EventListener<Event>() {
					@Override
					public void onEvent(Event event) throws Exception {
						handleTabSelectionEvent(event);
					}
				};

				// Subscribe to tab selection events
				// WindowContainer fires ON_SELECT when user switches tabs
				windowContainer.addEventListener(Events.ON_SELECT, tabSelectionListener);
				log.info("AI Chat Widget: Context tracking active (listening to tab changes)");

				// Initialize context with currently active tab (if any)
				detectAndSetActiveTab();
			} else {
				log.warning("AI Chat Widget: Could not find WindowContainer - context tracking disabled. " +
					"This may occur if iDempiere UI structure has changed. See docs/STANDALONE_AI_CHAT_WIDGET.md");
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "AI Chat Widget: Failed to set up context tracking - " +
				"see docs/STANDALONE_AI_CHAT_WIDGET.md for troubleshooting", e);
		}
	}

	/**
	 * ZK Lifecycle hook - called when component is detached from a page.
	 * <p>Cleans up event listeners to prevent memory leaks.
	 */
	@Override
	public void onPageDetached(org.zkoss.zk.ui.Page page) {
		super.onPageDetached(page);

		// Clean up event listener
		if (windowContainer != null && tabSelectionListener != null) {
			try {
				windowContainer.removeEventListener(Events.ON_SELECT, tabSelectionListener);
				log.info("AI Chat Widget: Context tracking cleaned up");
			} catch (Exception e) {
				log.log(Level.WARNING, "AI Chat Widget: Error cleaning up tab selection listener", e);
			}
		}

		windowContainer = null;
		tabSelectionListener = null;
	}

	/**
	 * Discover WindowContainer by walking up the component tree.
	 * <p>Component hierarchy: AIChatWidget -> Panelchildren -> Panel ->
	 * Anchorchildren -> Anchorlayout -> East -> Borderlayout -> ... -> Desktop
	 * <p>WindowContainer is typically a child of the Desktop's Center region.
	 *
	 * @return WindowContainer component, or null if not found
	 */
	private Component discoverWindowContainer() {
		try {
			// Walk up to Desktop
			Component current = this;
			org.zkoss.zk.ui.Desktop desktop = null;

			while (current != null) {
				desktop = current.getDesktop();
				if (desktop != null) {
					break;
				}
				current = current.getParent();
			}

			if (desktop == null) {
				log.fine("Could not find Desktop from AI Chat Widget");
				return null;
			}

			// Find Borderlayout (main desktop layout)
			org.zkoss.zul.Borderlayout borderLayout = findComponentByType(
				desktop.getFirstPage(), org.zkoss.zul.Borderlayout.class, "layout");

			if (borderLayout == null) {
				log.fine("Could not find Borderlayout in Desktop");
				return null;
			}

			// Get Center region (where WindowContainer lives)
			org.zkoss.zul.Center center = borderLayout.getCenter();
			if (center == null) {
				log.fine("Could not find Center region in Borderlayout");
				return null;
			}

			// Find WindowContainer (TabbedDocumentPane) in Center
			// Look for component with specific class name or ID pattern
			return findWindowContainerInCenter(center);

		} catch (Exception e) {
			log.log(Level.FINE, "Error discovering WindowContainer", e);
			return null;
		}
	}

	/**
	 * Find WindowContainer within Center region.
	 * WindowContainer is typically an instance of TabbedDocumentPane.
	 *
	 * @param center Center region
	 * @return WindowContainer component, or null if not found
	 */
	private Component findWindowContainerInCenter(org.zkoss.zul.Center center) {
		// Try to find by class name (TabbedDocumentPane)
		for (Component child : center.getChildren()) {
			String className = child.getClass().getSimpleName();
			if (className.contains("TabbedDocument") || className.contains("WindowContainer")) {
				log.fine("Found WindowContainer: " + className);
				return child;
			}

			// Recursively search children
			Component found = findWindowContainerRecursive(child);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/**
	 * Recursively search for WindowContainer in component tree.
	 *
	 * @param parent Parent component
	 * @return WindowContainer component, or null if not found
	 */
	private Component findWindowContainerRecursive(Component parent) {
		for (Component child : parent.getChildren()) {
			String className = child.getClass().getSimpleName();
			if (className.contains("TabbedDocument") || className.contains("WindowContainer")) {
				return child;
			}

			Component found = findWindowContainerRecursive(child);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/**
	 * Find component by type and optional ID.
	 *
	 * @param page Page to search
	 * @param type Component type
	 * @param id Optional ID (can be null)
	 * @return Component, or null if not found
	 */
	@SuppressWarnings("unchecked")
	private <T extends Component> T findComponentByType(org.zkoss.zk.ui.Page page, Class<T> type, String id) {
		if (id != null) {
			try {
				Component comp = page.getFellow(id);
				if (type.isInstance(comp)) {
					return (T) comp;
				}
			} catch (Exception e) {
				// ID not found, continue
			}
		}

		// Recursively search all page components
		for (Component root : page.getRoots()) {
			T found = findComponentByTypeRecursive(root, type);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/**
	 * Recursively find component by type.
	 */
	@SuppressWarnings("unchecked")
	private <T extends Component> T findComponentByTypeRecursive(Component parent, Class<T> type) {
		if (type.isInstance(parent)) {
			return (T) parent;
		}

		for (Component child : parent.getChildren()) {
			T found = findComponentByTypeRecursive(child, type);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/**
	 * Handle tab selection event from WindowContainer.
	 * Extracts windowNo and tabNo from the selected tab and updates context.
	 *
	 * @param event Tab selection event
	 */
	private void handleTabSelectionEvent(Event event) {
		try {
			// Event target should be a Tab or Tabpanel
			Component target = event.getTarget();
			log.fine("Tab selection event received from: " + target.getClass().getSimpleName());

			// Extract windowNo and tabNo from the selected tab
			// The exact approach depends on iDempiere's window implementation
			// Try reflection to get windowNo and tabNo from tab content

			if (target instanceof org.zkoss.zul.Tab) {
				org.zkoss.zul.Tab tab = (org.zkoss.zul.Tab) target;
				org.zkoss.zul.Tabpanel panel = tab.getLinkedPanel();

				if (panel != null) {
					extractContextFromTabpanel(panel);
				}
			} else if (target instanceof org.zkoss.zul.Tabpanel) {
				extractContextFromTabpanel((org.zkoss.zul.Tabpanel) target);
			}

		} catch (Exception e) {
			log.log(Level.FINE, "Error handling tab selection event", e);
		}
	}

	/**
	 * Extract windowNo and tabNo from a Tabpanel.
	 * Uses reflection to find ADWindow components and extract context.
	 *
	 * @param panel Tabpanel
	 */
	private void extractContextFromTabpanel(org.zkoss.zul.Tabpanel panel) {
		try {
			// Look for ADWindow or ADWindowContent in the panel
			for (Component child : panel.getChildren()) {
				// Try to find ADWindow using class name check
				String className = child.getClass().getName();

				if (className.contains("ADWindow")) {
					// Use reflection to get windowNo
					try {
						java.lang.reflect.Method getWindowNo = child.getClass().getMethod("getWindowNo");
						int windowNo = (Integer) getWindowNo.invoke(child);

						// Try to get active tab number
						java.lang.reflect.Method getADWindowContent = child.getClass().getMethod("getADWindowContent");
						Object content = getADWindowContent.invoke(child);

						if (content != null) {
							java.lang.reflect.Method getActiveGridTab = content.getClass().getMethod("getActiveGridTab");
							Object gridTab = getActiveGridTab.invoke(content);

							if (gridTab != null) {
								java.lang.reflect.Method getTabNo = gridTab.getClass().getMethod("getTabNo");
								int tabNo = (Integer) getTabNo.invoke(gridTab);

								// Update context!
								log.info("AI Chat Widget: Context updated (windowNo=" + windowNo + ", tabNo=" + tabNo + ")");
								setWindowContext(windowNo, tabNo);
								return;
							}
						}
					} catch (Exception e) {
						log.log(Level.FINE, "Reflection error while extracting context", e);
					}
				}
			}
		} catch (Exception e) {
			log.log(Level.FINE, "Error extracting context from tabpanel", e);
		}
	}

	/**
	 * Detect and set the currently active tab on widget initialization.
	 * Called when widget is first attached to discover initial context.
	 */
	private void detectAndSetActiveTab() {
		try {
			if (windowContainer == null) {
				return;
			}

			// Find the selected/active tab in window container
			// Look for Tabs component and get selected tab
			Component tabsComponent = findTabsComponent(windowContainer);

			if (tabsComponent instanceof org.zkoss.zul.Tabs) {
				org.zkoss.zul.Tabs tabs = (org.zkoss.zul.Tabs) tabsComponent;
				org.zkoss.zul.Tabbox tabbox = tabs.getTabbox();

				if (tabbox != null) {
					org.zkoss.zul.Tab selectedTab = (org.zkoss.zul.Tab) tabbox.getSelectedTab();

					if (selectedTab != null) {
						org.zkoss.zul.Tabpanel panel = selectedTab.getLinkedPanel();
						if (panel != null) {
							extractContextFromTabpanel(panel);
						}
					}
				}
			}
		} catch (Exception e) {
			log.log(Level.FINE, "Error detecting active tab on init", e);
		}
	}

	/**
	 * Find Tabs component within WindowContainer.
	 */
	private Component findTabsComponent(Component parent) {
		if (parent instanceof org.zkoss.zul.Tabs) {
			return parent;
		}

		for (Component child : parent.getChildren()) {
			if (child instanceof org.zkoss.zul.Tabs) {
				return child;
			}

			Component found = findTabsComponent(child);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	// =========================================================================================
	// END SELF-MANAGING CONTEXT TRACKING
	// =========================================================================================

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
			return "<div id='accessBadge_" + getUuid() + "' class='ai-hidden'></div>";
		}

		if (access == ChatAccess.READ) {
			// Read-only badge
			return "<div id='accessBadge_" + getUuid() + "' class='ai-access-indicator'>" +
				"<i class='z-icon-Lock' style='font-size: 12px;'></i> " +
				"<span>" + Msg.getMsg(sessionCtx, "ReadOnly") + "</span>" +
				"</div>";
		}

		// No access - should not normally be shown
		return "<div id='accessBadge_" + getUuid() + "' class='ai-hidden'></div>";
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
		// IMPORTANT: sanitize must be false to allow HTML zoom links (ADR-039)
		sb.append("    marked.setOptions({");
		sb.append("      highlight: function(code, lang) {");
		sb.append("        if (lang && Prism.languages[lang]) {");
		sb.append("          return Prism.highlight(code, Prism.languages[lang], lang);");
		sb.append("        }");
		sb.append("        return code;");
		sb.append("      },");
		sb.append("      breaks: true,");
		sb.append("      gfm: true,");
		sb.append("      sanitize: false");  // Allow HTML for zoom links
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

	/**
	 * Render markdown while preserving existing HTML (tables and zoom links).
	 * Simplified version of AIChatStreamingMessage.processMarkdownPreservingHTML()
	 */
	private String renderMarkdownPreservingHTML(String text) {
		if (text == null || text.isEmpty()) return "";

		// Normalize excessive line breaks (3+ newlines → 2 newlines for proper paragraph spacing)
		text = text.replaceAll("\n{3,}", "\n\n");

		StringBuilder result = new StringBuilder();
		int pos = 0;

		while (pos < text.length()) {
			int tagStart = text.indexOf('<', pos);
			if (tagStart == -1) {
				result.append(renderSimpleMarkdown(text.substring(pos)));
				break;
			}
			if (tagStart > pos) {
				result.append(renderSimpleMarkdown(text.substring(pos, tagStart)));
			}
			int tagEnd = text.indexOf('>', tagStart);
			if (tagEnd == -1) {
				result.append(text.substring(tagStart));
				break;
			}
			String tag = text.substring(tagStart, tagEnd + 1);
			result.append(tag);
			String tagName = extractTagName(tag);
			if (tagName != null && !tag.endsWith("/>") && !isSelfClosingTag(tagName)) {
				String closingTag = "</" + tagName + ">";
				int closingPos = text.indexOf(closingTag, tagEnd + 1);
				if (closingPos != -1) {
					result.append(text.substring(tagEnd + 1, closingPos + closingTag.length()));
					pos = closingPos + closingTag.length();
					continue;
				}
			}
			pos = tagEnd + 1;
		}
		return result.toString();
	}

	private String extractTagName(String tag) {
		if (tag == null || tag.length() < 3) return null;
		String content = tag.substring(1, tag.length() - 1).trim();
		if (content.startsWith("/")) content = content.substring(1).trim();
		if (content.endsWith("/")) content = content.substring(0, content.length() - 1).trim();
		int spacePos = content.indexOf(' ');
		if (spacePos > 0) content = content.substring(0, spacePos);
		return content.toLowerCase();
	}

	private boolean isSelfClosingTag(String tagName) {
		return tagName.equals("br") || tagName.equals("hr") || tagName.equals("img") || tagName.equals("input");
	}

	private String renderSimpleMarkdown(String text) {
		if (text == null || text.isEmpty()) return "";
		String result = Util.maskHTML(text, true);
		result = result.replaceAll("(?m)^### (.+)$", "<h4 style='font-size: 14px; font-weight: 600; margin: 12px 0 8px 0;'>$1</h4>");
		result = result.replaceAll("(?m)^## (.+)$", "<h3 style='font-size: 15px; font-weight: 600; margin: 14px 0 8px 0;'>$1</h3>");
		result = result.replaceAll("(?m)^# (.+)$", "<h2 style='font-size: 16px; font-weight: 600; margin: 16px 0 10px 0;'>$1</h2>");
		result = result.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
		result = result.replaceAll("__(.+?)__", "<strong>$1</strong>");
		result = result.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "<em>$1</em>");
		result = result.replaceAll("`([^`]+)`", "<code style='background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-family: monospace; font-size: 0.9em;'>$1</code>");
		result = result.replaceAll("(?m)^- (.+)$", "<li style='margin-left: 16px; list-style-type: disc;'>$1</li>");
		result = result.replaceAll("(?m)^\\* (.+)$", "<li style='margin-left: 16px; list-style-type: disc;'>$1</li>");
		result = result.replace("\n", "<br/>");
		result = result.replaceAll("</h2><br/>", "</h2>");
		result = result.replaceAll("</h3><br/>", "</h3>");
		result = result.replaceAll("</h4><br/>", "</h4>");
		result = result.replaceAll("</li><br/>", "</li>");
		return result;
	}

	// ========================================================================
	// Stop Button / Request Cancellation (ADR-031)
	// ========================================================================

	/**
	 * Show the stop button and hide the send button.
	 * Called when AI processing starts.
	 */
	private void showStopButton() {
		sendButton.setSclass("ai-send-btn ai-hidden");
		stopButton.setSclass("ai-stop-btn ai-visible");
	}

	/**
	 * Show the send button and hide the stop button.
	 * Called when AI processing completes or is cancelled.
	 */
	private void showSendButton() {
		stopButton.setSclass("ai-stop-btn ai-hidden");
		sendButton.setSclass("ai-send-btn ai-visible");
	}

	/**
	 * Cancel the current AI request.
	 * Called when stop button is clicked.
	 */
	private void cancelCurrentRequest() {
		log.info("[CANCEL] Cancel request initiated, streamingInProgress=" + streamingInProgress);

		// SAFETY: Atomic cancellation to prevent race conditions
		if (requestCancelled.compareAndSet(false, true)) {
			// Only execute cancel logic once
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
		} else {
			log.info("[CANCEL] Cancel already in progress, ignoring duplicate request");
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
	 * Safely schedule a UI update event, handling cases where desktop becomes unavailable.
	 * SAFETY: Prevents silent failures when user navigates away during streaming.
	 *
	 * @param desktop Desktop instance to schedule on
	 * @param handler Event handler to execute
	 * @param event Event to send
	 */
	private void safeSchedule(Desktop desktop, java.util.function.Consumer<Event> handler, Event event) {
		// Check desktop validity before scheduling
		if (desktop == null || !desktop.isAlive()) {
			log.fine("Desktop no longer available, cannot schedule UI update: " + event.getName());
			return;
		}

		try {
			Executions.schedule(desktop, e -> {
				// Double-check desktop is still alive when event executes
				if (desktop.isAlive()) {
					handler.accept(e);
				} else {
					log.fine("Desktop became unavailable before event executed: " + event.getName());
				}
			}, event);
		} catch (Exception e) {
			// Catch any ZK exceptions related to desktop unavailability
			log.log(Level.FINE, "Desktop became unavailable during schedule: " + e.getMessage(), e);
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
