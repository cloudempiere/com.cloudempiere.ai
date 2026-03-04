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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Timer;

import com.cloudempiere.ai.util.ChunkCleaner;
import com.cloudempiere.ai.util.MarkdownSyntaxSanitizer;
import com.cloudempiere.ai.util.StreamingMarkdownRenderer;
import com.cloudempiere.ai.util.StreamingTextBuffer;
import com.cloudempiere.ai.util.ZoomLinkProcessor;

/**
 * ZK component for rendering streaming AI responses with tool timeline and thinking.
 *
 * <p>This component implements ADR-033: Streaming Responses and Thinking Timeline UX.
 *
 * <p>Features:
 * <ul>
 *   <li>Real-time token streaming with cursor animation</li>
 *   <li>Collapsible thinking/reasoning section</li>
 *   <li>Tool execution timeline with status indicators</li>
 *   <li>Markdown rendering with syntax highlighting</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 1.0
 * @since ADR-033
 */
public class AIChatStreamingMessage extends Div {

    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(AIChatStreamingMessage.class);

    // ============= UI Sections =============

    /** Collapsible thinking display */
    private Div thinkingSection;

    /** Thinking header (clickable to expand/collapse) */
    private Div thinkingHeader;

    /** Thinking content area */
    private Html thinkingContent;

    /** Tool usage timeline */
    private Div toolsSection;

    /** Tools content area */
    private Html toolsContent;

    /** Main streaming text content section */
    private Div contentSection;

    /** The actual streaming text element */
    private Html streamingContent;

    /** Copy button (shown after completion) */
    private Div copyButton;

    // ============= State =============

    /** Accumulated response content with proper UTF-16 surrogate handling */
    private StreamingTextBuffer content = new StreamingTextBuffer();

    /** Accumulated thinking content */
    private StringBuilder thinking = new StringBuilder();

    /** Tool execution events */
    private List<ToolEvent> toolEvents = new ArrayList<>();

    /** Whether thinking section is expanded */
    private boolean isThinkingExpanded = false;

    /** Map of tool names to their inline indicator element IDs (CLD-1704) */
    private java.util.Map<String, String> toolIndicatorIds = new java.util.HashMap<>();

    /** Counter for generating unique tool IDs */
    private int toolIdCounter = 0;

    // ============= State Management (CLD-1704) =============

    /**
     * Current streaming state.
     * <p>Single source of truth for component lifecycle state.
     * Replaces multiple boolean flags (isComplete, isCancelled) with explicit state machine.
     */
    private volatile StreamingState state = StreamingState.IDLE;

    /** Unique ID for JavaScript operations */
    private final String componentId;

    /** Flag to track if CSS has been injected */
    private static boolean cssInjected = false;

    /** Locale for number formatting in tables */
    private Locale locale = Locale.getDefault();

    /** iDempiere context for zoom link processing */
    private Properties ctx;

    /** Parent widget ID for zoom event targeting */
    private String parentWidgetId;

    /** Language detection service for session language (ADR-037) */
    // TEMPORARILY DISABLED - LanguageDetectionService will be implemented in future phase
    // private com.cloudempiere.ai.service.LanguageDetectionService languageService;

    /** Unified streaming renderer for progressive markdown and table formatting (ADR-047 Phase 4) */
    private StreamingMarkdownRenderer markdownRenderer;

    /** Markdown syntax sanitizer with instance-based configuration (multi-tenant safe) */
    private final MarkdownSyntaxSanitizer markdownSanitizer;

    /** Cached rendered HTML for persistence (ADR-054)
     * <p>volatile ensures visibility between streaming thread and UI thread */
    private volatile String renderedHtml = null;

    // ============= Throttled Rendering (ADR-047) =============

    /** Queue for batching chunks before rendering (throttling to 50ms intervals) */
    private final List<String> chunkQueue = new ArrayList<>();

    /** Flag to track if render is scheduled */
    private boolean renderScheduled = false;

    /** Lock object for synchronizing chunk queue access */
    private final Object queueLock = new Object();

    /** Timer for reliable batched rendering (alternative to JavaScript setTimeout) */
    private Timer renderTimer;

    /**
     * Create a new streaming message component.
     */
    public AIChatStreamingMessage() {
        this(Env.getCtx(), null, 0);
    }

    /**
     * Create a new streaming message component with context for zoom links.
     *
     * @param ctx iDempiere context for database access
     * @param parentWidgetId parent widget ID for zoom event targeting (fires onZoom)
     */
    public AIChatStreamingMessage(Properties ctx, String parentWidgetId) {
        this(ctx, parentWidgetId, 0);
    }

    /**
     * Create a new streaming message component with context for zoom links and language detection.
     *
     * @param ctx iDempiere context for database access
     * @param parentWidgetId parent widget ID for zoom event targeting (fires onZoom)
     * @param chatId chat ID for session language lookup (0 if unknown)
     */
    public AIChatStreamingMessage(Properties ctx, String parentWidgetId, int chatId) {
        super();
        this.componentId = "stream_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        this.ctx = ctx != null ? ctx : Env.getCtx();
        this.parentWidgetId = parentWidgetId;
        // TEMPORARILY DISABLED - LanguageDetectionService will be implemented in future phase
        // this.languageService = chatId > 0 ? com.cloudempiere.ai.service.LanguageDetectionService.getInstance() : null;

        int clientId = org.compiere.util.Env.getAD_Client_ID(ctx);

        // Initialize unified streaming renderer for progressive markdown and table formatting (ADR-047 Phase 4)
        this.markdownRenderer = new StreamingMarkdownRenderer();
        log.debug("[STREAM-INIT] Initialized unified StreamingMarkdownRenderer with table support | AD_Client_ID=" + clientId);

        // Initialize markdown sanitizer with default secure configuration
        // TODO: Load provider-specific config from database (MAIProvider)
        this.markdownSanitizer = new MarkdownSyntaxSanitizer();
        log.debug("[STREAM-INIT] Initialized MarkdownSyntaxSanitizer with secure defaults");

        injectCSS();
        init();
    }

    // ============= State Management Methods (Phase 2 - CLD-1704) =============

    /**
     * Transition to a new state with validation.
     *
     * <p>Validates that the transition is allowed and logs the state change.
     * Invalid transitions are logged as warnings but not blocked (fail-safe).
     *
     * <p><b>Visibility:</b> Package-private to allow AIChatWidget to manage state.
     *
     * @param newState the target state
     */
    void transitionTo(StreamingState newState) {
        if (state == newState) {
            return; // Already in target state
        }

        if (!isValidTransition(state, newState)) {
            log.warn("Invalid state transition: " + state + " → " + newState + " (component: " + componentId + ")");
            // Don't block - fail-safe approach
        }

        StreamingState oldState = state;
        state = newState;

        log.debug("State transition: " + oldState + " → " + newState + " (component: " + componentId + ")");

        // Notify state change listeners (can add UI updates here)
        onStateChanged(oldState, newState);
    }

    /**
     * Check if a state transition is valid.
     *
     * @param from current state
     * @param to target state
     * @return true if transition is allowed
     */
    private boolean isValidTransition(StreamingState from, StreamingState to) {
        switch (from) {
            case IDLE:
                return to == StreamingState.WAITING_FOR_LLM;

            case WAITING_FOR_LLM:
                return to == StreamingState.THINKING ||
                       to == StreamingState.STREAMING_TEXT ||
                       to == StreamingState.TOOL_EXECUTING ||
                       to == StreamingState.ERROR ||
                       to == StreamingState.CANCELLED;

            case TOOL_EXECUTING:
                return to == StreamingState.STREAMING_TEXT ||
                       to == StreamingState.TOOL_EXECUTING ||  // Multiple tool calls
                       to == StreamingState.COMPLETE ||  // Tool result final
                       to == StreamingState.ERROR ||
                       to == StreamingState.CANCELLED;

            case THINKING:
                return to == StreamingState.STREAMING_TEXT ||
                       to == StreamingState.ERROR ||
                       to == StreamingState.CANCELLED;

            case STREAMING_TEXT:
                return to == StreamingState.FINALIZING ||
                       to == StreamingState.ERROR ||
                       to == StreamingState.CANCELLED;

            case FINALIZING:
                return to == StreamingState.COMPLETE ||
                       to == StreamingState.ERROR;

            case COMPLETE:
            case CANCELLED:
            case ERROR:
                return to == StreamingState.IDLE;  // Only allow reset to IDLE

            default:
                return false;
        }
    }

    /**
     * Hook for state change notifications.
     *
     * <p>Can be extended to update UI elements, notify parent components, etc.
     *
     * @param oldState previous state
     * @param newState new state
     */
    private void onStateChanged(StreamingState oldState, StreamingState newState) {
        // Future: Update UI indicators, notify listeners
        // For now, just log
    }

    /**
     * Get current state.
     *
     * @return current streaming state
     */
    public StreamingState getState() {
        return state;
    }

    // ============= End State Management Methods =============

    /**
     * Inject streaming CSS styles (once per page).
     */
    private void injectCSS() {
        if (cssInjected) {
            return;
        }
        cssInjected = true;

        String css =
            // Streaming message container
            ".ai-streaming-message { display: flex; flex-direction: column; gap: 12px; padding: 12px 18px; max-width: 100%; }" +

            // Content section
            ".ai-content-section { max-width: 100%; word-wrap: break-word; overflow-wrap: break-word; white-space: normal; }" +
            ".ai-markdown-content { max-width: 100%; overflow-x: auto; }" +
            ".ai-markdown-content table { max-width: 100%; table-layout: auto; word-wrap: break-word; }" +
            ".ai-markdown-content td, .ai-markdown-content th { word-wrap: break-word; overflow-wrap: break-word; max-width: 300px; }" +

            // Thinking section (collapsible)
            ".ai-thinking-section { border: 1px solid #E8E8E8; border-radius: 8px; background: #FAFAFA; overflow: hidden; }" +
            ".ai-thinking-header { display: flex; align-items: center; gap: 8px; padding: 8px 12px; cursor: pointer; font-size: 11px; color: #666; }" +
            ".ai-thinking-header:hover { background: #F0F0F0; }" +
            ".ai-thinking-toggle { transition: transform 0.2s; }" +
            ".ai-thinking-toggle.expanded { transform: rotate(90deg); }" +
            ".ai-thinking-content { padding: 12px; font-size: 11px; color: #555; line-height: 1.5; max-height: 200px; overflow-y: auto; border-top: 1px solid #E8E8E8; background: #FDFDFD; }" +

            // Tools timeline
            ".ai-tools-timeline { padding: 8px 12px; background: #F8F9FA; border-radius: 8px; border-left: 3px solid #1976D2; }" +
            ".tool-event { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 11px; color: #555; }" +
            ".tool-spinner { color: #1976D2; animation: tool-spin 1s linear infinite; display: inline-block; }" +
            "@keyframes tool-spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }" +
            ".tool-complete { color: #4CAF50; }" +
            ".tool-error { color: #F44336; }" +
            ".tool-name { color: #333; }" +

            // Streaming cursor
            ".streaming-cursor { color: #1976D2; animation: cursor-blink 0.8s ease-in-out infinite; font-weight: bold; }" +
            "@keyframes cursor-blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }" +

            // Thinking indicator (animated dots)
            ".ai-thinking-dots { display: inline-flex; gap: 4px; margin-left: 8px; }" +
            ".ai-thinking-dots span { width: 5px; height: 5px; background: #717680; border-radius: 50%; animation: thinking-pulse 1.4s ease-in-out infinite; }" +
            ".ai-thinking-dots span:nth-child(1) { animation-delay: 0s; }" +
            ".ai-thinking-dots span:nth-child(2) { animation-delay: 0.2s; }" +
            ".ai-thinking-dots span:nth-child(3) { animation-delay: 0.4s; }" +
            "@keyframes thinking-pulse { 0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); } 40% { opacity: 1; transform: scale(1); } }";

        org.zkoss.zk.ui.util.Clients.evalJavaScript(
            "(function() {" +
            "  if (document.getElementById('ai-streaming-css')) return;" +
            "  var style = document.createElement('style');" +
            "  style.id = 'ai-streaming-css';" +
            "  style.textContent = '" + css.replace("'", "\\'") + "';" +
            "  document.head.appendChild(style);" +
            "})();"
        );
    }

    /**
     * Initialize the component structure.
     */
    private void init() {
        setSclass("ai-streaming-message");
        setStyle("display: flex; flex-direction: column; gap: 12px; padding: 12px 18px;");

        // Thinking section (hidden by default)
        thinkingSection = new Div();
        thinkingSection.setId("thinking_" + componentId);
        thinkingSection.setSclass("ai-thinking-section");
        thinkingSection.setVisible(false);
        thinkingSection.setStyle("border: 1px solid #E8E8E8; border-radius: 8px; background: #FAFAFA; overflow: hidden;");

        // Thinking header (clickable)
        thinkingHeader = new Div();
        thinkingHeader.setSclass("ai-thinking-header");
        thinkingHeader.setStyle("display: flex; align-items: center; gap: 8px; padding: 8px 12px; cursor: pointer; font-size: 11px; color: #666;");
        thinkingHeader.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                toggleThinking();
            }
        });

        Html thinkingHeaderContent = new Html();
        thinkingHeaderContent.setContent(
            "<span class='ai-thinking-toggle' id='toggle_" + componentId + "'>&#9654;</span> " +
            "<span>Thinking</span>" +
            "<span class='ai-thinking-dots'><span></span><span></span><span></span></span>"
        );
        thinkingHeader.appendChild(thinkingHeaderContent);
        thinkingSection.appendChild(thinkingHeader);

        // Thinking content
        thinkingContent = new Html();
        thinkingContent.setId("thinking_content_" + componentId);
        thinkingContent.setStyle("display: none; padding: 12px; font-size: 11px; color: #555; " +
            "line-height: 1.5; max-height: 200px; overflow-y: auto; border-top: 1px solid #E8E8E8; background: #FDFDFD;");
        thinkingSection.appendChild(thinkingContent);
        appendChild(thinkingSection);

        // Tools section (hidden by default)
        toolsSection = new Div();
        toolsSection.setId("tools_" + componentId);
        toolsSection.setSclass("ai-tools-section");
        toolsSection.setVisible(false);
        toolsSection.setStyle("padding: 8px 12px; background: #F8F9FA; border-radius: 8px; border-left: 3px solid #1976D2;");

        toolsContent = new Html();
        toolsContent.setContent("<div class='ai-tools-timeline'></div>");
        toolsSection.appendChild(toolsContent);
        appendChild(toolsSection);

        // Content section (main response)
        contentSection = new Div();
        contentSection.setSclass("ai-content-section");
        contentSection.setStyle("font-family: 'Helvetica Neue', sans-serif; font-weight: 400; " +
            "font-size: 12px; line-height: 18px; color: #181D27; " +
            "max-width: 100%; word-wrap: break-word; overflow-wrap: break-word; white-space: normal;");

        streamingContent = new Html();
        streamingContent.setId("content_" + componentId);
        streamingContent.setContent("<span class='streaming-cursor'>|</span>");
        contentSection.appendChild(streamingContent);
        appendChild(contentSection);

        // Copy button (hidden until complete)
        copyButton = new Div();
        copyButton.setId("copy_" + componentId);
        copyButton.setVisible(false);
        copyButton.setStyle("display: flex; align-items: center; gap: 6px; cursor: pointer; " +
            "padding: 4px 8px; margin-top: 8px; width: fit-content;");
        appendChild(copyButton);

        // Initialize ZK Timer for batched rendering (ADR-047)
        // Uses native ZK component instead of JavaScript setTimeout for reliable event delivery
        // Timer fires Events.ON_TIMER after 50ms delay (20 FPS) for smooth streaming
        renderTimer = new Timer();
        renderTimer.setId(componentId + "_timer");
        renderTimer.setDelay(50);  // 50ms = 20 FPS
        renderTimer.setRepeats(false);  // One-shot timer (restarts manually if needed)
        renderTimer.addEventListener(Events.ON_TIMER, new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                onBatchRender();
            }
        });
        appendChild(renderTimer);
        log.debug("[BATCH-RENDER] ZK Timer initialized for component: " + componentId);
    }

    /**
     * Append a text chunk to the streaming content.
     *
     * <p>Called from onChunk callback via Executions.schedule().
     *
     * <p><b>Performance Note (ADR-047):</b> Chunks are queued and rendered
     * at fixed 50ms intervals to prevent DOM thrashing. This reduces updates
     * from 100+/sec to ~20/sec for smoother UX.
     *
     * @param chunk text chunk to append
     */
    public void appendChunk(String chunk) {
        // Ignore chunks if cancelled or already complete
        if (state == StreamingState.CANCELLED || state == StreamingState.COMPLETE || state == StreamingState.ERROR) {
            return;
        }
        if (chunk == null || chunk.isEmpty()) {
            return;
        }

        // Content chunk arrival: transition to STREAMING_TEXT if not already streaming
        // Unified renderer (ADR-047 Phase 4) handles all content types (markdown + tables)
        // Handles: initial response, after thinking, after tool execution
        if (state == StreamingState.WAITING_FOR_LLM ||
            state == StreamingState.IDLE ||
            state == StreamingState.THINKING ||
            state == StreamingState.TOOL_EXECUTING) {
            transitionTo(StreamingState.STREAMING_TEXT);
        }

        // Queue chunk for batched rendering (ADR-047 Phase 2)
        // FIX BUG #4: Move ChunkCleaner.clean() INSIDE synchronized block
        // to prevent race condition where chunk could be dropped
        synchronized (queueLock) {
            // Clean chunk before processing (ADR-047 Phase 1)
            String cleaned = ChunkCleaner.clean(chunk);

            // Sanitize markdown to only supported syntax (ADR-055)
            // This prevents XSS, JavaScript injection, and unsupported markdown features
            String sanitized = markdownSanitizer.sanitize(cleaned);

            chunkQueue.add(sanitized);

            // Schedule render if not already pending
            if (!renderScheduled) {
                renderScheduled = true;
                scheduleRender();
            }
        }
    }

    /**
     * Schedule a batched render update after 50ms delay.
     *
     * <p>Uses ZK Timer component for reliable event delivery.
     * Multiple chunks are batched together and rendered once per interval.
     *
     * <p><b>Performance Impact:</b>
     * <ul>
     *   <li>Before: 100+ DOM updates/sec (per chunk)</li>
     *   <li>After: ~20 DOM updates/sec (every 50ms)</li>
     *   <li>Result: 80% reduction in browser reflows</li>
     * </ul>
     *
     * <p><b>Implementation:</b> Uses native ZK Timer instead of JavaScript setTimeout
     * to avoid widget ID lookup issues and ensure thread-safe event delivery.
     */
    private void scheduleRender() {
        // Start timer if not already running
        // Timer will fire Events.ON_TIMER after 50ms, triggering onBatchRender()
        if (!renderTimer.isRunning()) {
            renderTimer.start();
        }
    }

    /**
     * Handle batched render event (triggered every 50ms by ZK Timer).
     *
     * <p>This method:
     * <ol>
     *   <li>Collects all queued chunks</li>
     *   <li>Processes them through content buffer and table renderer</li>
     *   <li>Updates DOM once for entire batch</li>
     *   <li>Restarts timer if more chunks arrived during processing</li>
     * </ol>
     *
     * <p>Called via ZK Events.ON_TIMER from native Timer component.
     */
    public void onBatchRender() {
        // Collect batch of chunks
        List<String> batch;
        synchronized (queueLock) {
            if (chunkQueue.isEmpty()) {
                renderScheduled = false;
                return;
            }

            // Copy and clear queue
            batch = new ArrayList<>(chunkQueue);
            chunkQueue.clear();
        }

        // Process all chunks in batch
        for (String chunk : batch) {
            content.append(chunk);

            // Feed chunk to unified streaming renderer for progressive markdown and table rendering (ADR-047 Phase 4)
            markdownRenderer.appendChunk(chunk);
        }

        // Update DOM once for entire batch
        updateContentDisplay();

        // Restart timer if more chunks arrived during processing
        synchronized (queueLock) {
            if (!chunkQueue.isEmpty() && !renderTimer.isRunning()) {
                renderTimer.start();
            } else {
                renderScheduled = false;
            }
        }
    }

    /**
     * Show tool execution start inline in the content stream (CLD-1704).
     *
     * <p>Injects a tool indicator directly into the streaming content
     * with a unique ID for later updates.
     *
     * @param toolName name of the tool being executed
     * @param arguments tool arguments (JSON string)
     */
    public void showToolStart(String toolName, String arguments) {
        // Transition to TOOL_EXECUTING if not already there
        if (state != StreamingState.TOOL_EXECUTING) {
            transitionTo(StreamingState.TOOL_EXECUTING);
        }

        // Generate unique ID for this tool indicator
        String toolId = componentId + "_tool_" + (toolIdCounter++);
        toolIndicatorIds.put(toolName, toolId);

        // Get user-friendly display name
        String displayName = getToolDisplayName(toolName);

        // Inject inline tool indicator HTML with CSS classes
        String toolHtml = String.format(
            "<br/><div id='%s' class='ai-tool-indicator running'>" +
            "<span class='ai-tool-spinner'>&#8635;</span>" +
            "<span>%s</span>" +
            "</div><br/>",
            toolId, displayName
        );

        if (markdownRenderer != null) {
            markdownRenderer.appendRawHtml(toolHtml);
            // Force immediate rendering so tool indicator appears right away
            updateContentDisplay();
        }

        // Keep old implementation for backward compatibility (but hidden)
        toolsSection.setVisible(false);
        toolEvents.add(new ToolEvent(toolName, ToolStatus.RUNNING, arguments, null));
    }

    /**
     * Update tool to complete status inline (CLD-1704).
     *
     * <p>Updates the inline indicator via JavaScript to show completion.
     *
     * @param toolName name of the tool that completed
     * @param result result of the tool execution
     */
    public void showToolComplete(String toolName, String result) {
        // Update inline indicator via JavaScript with retry logic
        String toolId = toolIndicatorIds.get(toolName);
        if (toolId != null) {
            String displayName = getToolDisplayName(toolName);

            // Create "complete" state HTML with CSS classes
            String completeHtml = String.format(
                "<div id='%s' class='ai-tool-indicator complete'>" +
                "<span>&#10003;</span>" +
                "<span>%s</span>" +
                "</div>",
                toolId, displayName
            );

            // Update the HTML in the renderer's buffer (prevents re-rendering from reverting state)
            if (markdownRenderer != null) {
                markdownRenderer.replaceHtmlById(toolId, completeHtml);
            }

            // Also update via JavaScript for immediate visual feedback
            String script = String.format(
                "(function() { " +
                "  var tryUpdate = function(attempt) { " +
                "    var el = document.getElementById('%s'); " +
                "    if (el) { " +
                "      el.className = 'ai-tool-indicator complete'; " +
                "      el.innerHTML = '<span>&#10003;</span> <span>%s</span>'; " +
                "      console.log('Tool indicator updated: %s'); " +
                "    } else if (attempt < 10) { " +
                "      console.log('Tool indicator not found (attempt ' + attempt + '), retrying...'); " +
                "      setTimeout(function() { tryUpdate(attempt + 1); }, 50); " +
                "    } else { " +
                "      console.error('Tool indicator not found after 10 attempts: %s'); " +
                "    } " +
                "  }; " +
                "  tryUpdate(1); " +
                "})();",
                toolId, displayName, toolId, toolId
            );
            org.zkoss.zk.ui.util.Clients.evalJavaScript(script);
        }

        // Update old implementation for backward compatibility
        for (ToolEvent event : toolEvents) {
            if (event.getName().equals(toolName) && event.getStatus() == ToolStatus.RUNNING) {
                event.setStatus(ToolStatus.COMPLETE);
                event.setResult(truncateResult(result, 100));
                break;
            }
        }
    }

    /**
     * Show tool error inline (CLD-1704).
     *
     * <p>Updates the inline indicator via JavaScript to show error.
     *
     * @param toolName name of the tool that failed
     * @param error error message
     */
    public void showToolError(String toolName, String error) {
        // Update inline indicator via JavaScript with retry logic
        String toolId = toolIndicatorIds.get(toolName);
        if (toolId != null) {
            String displayName = getToolDisplayName(toolName);

            // Create "error" state HTML with CSS classes
            String errorHtml = String.format(
                "<div id='%s' class='ai-tool-indicator error'>" +
                "<span>&#10007;</span>" +
                "<span>%s failed</span>" +
                "</div>",
                toolId, displayName
            );

            // Update the HTML in the renderer's buffer (prevents re-rendering from reverting state)
            if (markdownRenderer != null) {
                markdownRenderer.replaceHtmlById(toolId, errorHtml);
            }

            // Also update via JavaScript for immediate visual feedback
            String script = String.format(
                "(function() { " +
                "  var tryUpdate = function(attempt) { " +
                "    var el = document.getElementById('%s'); " +
                "    if (el) { " +
                "      el.className = 'ai-tool-indicator error'; " +
                "      el.innerHTML = '<span>&#10007;</span> <span>%s failed</span>'; " +
                "      console.log('Tool indicator updated (error): %s'); " +
                "    } else if (attempt < 10) { " +
                "      console.log('Tool indicator not found (attempt ' + attempt + '), retrying...'); " +
                "      setTimeout(function() { tryUpdate(attempt + 1); }, 50); " +
                "    } else { " +
                "      console.error('Tool indicator not found after 10 attempts: %s'); " +
                "    } " +
                "  }; " +
                "  tryUpdate(1); " +
                "})();",
                toolId, displayName, toolId, toolId
            );
            org.zkoss.zk.ui.util.Clients.evalJavaScript(script);
        }

        // Update old implementation for backward compatibility
        for (ToolEvent event : toolEvents) {
            if (event.getName().equals(toolName) && event.getStatus() == ToolStatus.RUNNING) {
                event.setStatus(ToolStatus.ERROR);
                event.setResult(error);
                break;
            }
        }
    }

    /**
     * Append thinking content (collapsible section).
     *
     * @param thinkingChunk chunk of thinking text
     */
    public void appendThinking(String thinkingChunk) {
        if (thinkingChunk == null || thinkingChunk.isEmpty()) {
            return;
        }
        thinkingSection.setVisible(true);
        thinking.append(thinkingChunk);
        updateThinkingDisplay();
    }

    /**
     * Mark thinking phase as complete.
     */
    public void completeThinking() {
        // Remove the animated dots
        String headerHtml =
            "<span class='ai-thinking-toggle' id='toggle_" + componentId + "'>&#9654;</span> " +
            "<span>Thinking</span>";
        ((Html) thinkingHeader.getFirstChild()).setContent(headerHtml);
    }

    /**
     * Complete the message when streaming finishes.
     *
     * <p>Removes streaming cursor, enables copy button, renders final markdown using marked.js.
     *
     * <p><b>Note:</b> This method was renamed from finalize() to avoid conflict
     * with Java's Object.finalize() which is called by the garbage collector.
     */
    public void complete() {
        if (state == StreamingState.COMPLETE || state == StreamingState.CANCELLED) {
            return; // Already completed or cancelled
        }

        // Transition to FINALIZING state
        transitionTo(StreamingState.FINALIZING);

        // Flush any remaining chunks in queue (ADR-047)
        // FIX BUG #8: Add exception handling to prevent inconsistent state
        synchronized (queueLock) {
            if (!chunkQueue.isEmpty()) {
                for (String chunk : chunkQueue) {
                    content.append(chunk);

                    try {
                        // Use unified renderer (ADR-047 Phase 4)
                        markdownRenderer.appendChunk(chunk);
                    } catch (Exception e) {
                        log.warn("Error appending chunk to renderer during completion: " + e.getMessage());
                        // Continue processing remaining chunks
                    }
                }
                chunkQueue.clear();
            }
            renderScheduled = false;
        }

        // Use marked.js for full markdown rendering (tables, code blocks, etc.)
        renderFinalMarkdown();

        // Capture rendered HTML for persistence (ADR-054)
        captureRenderedHtml();

        enableCopyButton();

        // Transition to COMPLETE state
        transitionTo(StreamingState.COMPLETE);
    }

    /**
     * Get the complete content for persistence.
     *
     * <p>Flushes the buffer to ensure all characters (including any
     * pending surrogates) are included in the output.
     *
     * @return accumulated response content
     */
    public String getContent() {
        return content.flush();
    }

    /**
     * Get the thinking content.
     *
     * @return accumulated thinking content
     */
    public String getThinkingContent() {
        return thinking.toString();
    }

    /**
     * Get the rendered HTML for persistence.
     *
     * <p><b>ADR-054:</b> Returns the final rendered HTML that was displayed to the user
     * during streaming. This HTML should be stored directly in the database instead of
     * markdown to eliminate duplicate rendering on reload.
     *
     * <p>The HTML is captured after {@link #renderFinalMarkdown()} completes, ensuring
     * it matches exactly what the user saw during streaming.
     *
     * @return rendered HTML content, or empty string if not yet captured
     * @since ADR-054
     */
    public String getRenderedHtml() {
        return renderedHtml != null ? renderedHtml : "";
    }

    /**
     * Capture the rendered HTML from streamingContent for persistence.
     *
     * <p><b>ADR-054:</b> Extracts the final HTML from the streamingContent component
     * after markdown rendering is complete. This HTML will be stored in the database
     * instead of markdown, eliminating the need to re-render on reload.
     *
     * <p>Called automatically by {@link #complete()} after {@link #renderFinalMarkdown()}.
     *
     * @since ADR-054
     */
    /**
     * Capture rendered HTML for persistence with field length validation.
     *
     * <p>Validates HTML length against CM_ChatEntry.CharacterData field limit
     * to prevent silent truncation that would break HTML structure on reload.
     *
     * @throws org.compiere.util.AdempiereException if HTML exceeds field limit
     */
    private void captureRenderedHtml() {
        if (streamingContent != null) {
            renderedHtml = streamingContent.getContent();

            int htmlLength = (renderedHtml != null ? renderedHtml.length() : 0);

            // Validate HTML length against database field limit
            // Get field length from AD_Column for CM_ChatEntry.CharacterData
            int maxFieldLength = getCharacterDataFieldLength();

            log.debug("[HTML-CAPTURE] Captured rendered HTML | length=" + htmlLength +
                    " | fieldLimit=" + maxFieldLength +
                    " | withinLimit=" + (htmlLength <= maxFieldLength));

            if (htmlLength > maxFieldLength) {
                String errorMsg = String.format(
                    "AI response HTML too long to save: %d chars exceeds field limit of %d chars. " +
                    "Increase CM_ChatEntry.CharacterData field length or implement content compression.",
                    htmlLength, maxFieldLength);
                log.error("[HTML-CAPTURE] VALIDATION FAILED: " + errorMsg);
                throw new AdempiereException(errorMsg);
            } else if (htmlLength > 0) {
                log.debug("[HTML-CAPTURE] Validation passed - HTML within field limit");
            }
        } else {
            log.warn("[HTML-CAPTURE] streamingContent is null, cannot capture HTML");
        }
    }

    /**
     * Get the field length limit for CM_ChatEntry.CharacterData from AD_Column.
     *
     * @return maximum field length in characters, or Integer.MAX_VALUE if not found
     */
    private int getCharacterDataFieldLength() {
        try {
            // Query AD_Column for CM_ChatEntry.CharacterData field length
            String sql = "SELECT FieldLength FROM AD_Column " +
                        "WHERE AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'CM_ChatEntry') " +
                        "AND ColumnName = 'CharacterData'";

            log.debug("[HTML-CAPTURE] Querying field length with SQL: " + sql);
            int fieldLength = org.compiere.util.DB.getSQLValueEx(null, sql);

            if (fieldLength > 0) {
                log.debug("[HTML-CAPTURE] Retrieved CharacterData field length from AD_Column: " + fieldLength);
                return fieldLength;
            } else {
                log.warn("[HTML-CAPTURE] Query returned invalid field length (" + fieldLength + "), using conservative default 4000");
                return 4000; // Conservative default
            }
        } catch (Exception e) {
            log.error("[HTML-CAPTURE] Error retrieving field length from AD_Column: " + e.getMessage(), e);
            log.warn("[HTML-CAPTURE] Falling back to conservative default: 4000");
            return 4000; // Conservative default on error
        }
    }

    /**
     * Check if streaming is complete.
     *
     * @return true if complete
     */
    public boolean isComplete() {
        return state == StreamingState.COMPLETE;
    }

    /**
     * Check if request was cancelled.
     *
     * @return true if cancelled
     */
    public boolean isCancelled() {
        return state == StreamingState.CANCELLED;
    }

    /**
     * Set the locale for number formatting in tables.
     *
     * <p>Numbers in markdown tables will be formatted according to this locale.
     * For example, in German locale, 1234.56 becomes 1.234,56.
     *
     * @param locale the locale to use (if null, uses system default)
     */
    public void setLocale(Locale locale) {
        this.locale = locale != null ? locale : Locale.getDefault();
    }

    /**
     * Get the current locale for number formatting.
     *
     * @return the current locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Mark this streaming message as cancelled by user.
     *
     * <p>Appends "AI request cancelled" and stops accepting new chunks.
     */
    public void markCancelled() {
        if (state == StreamingState.CANCELLED || state == StreamingState.COMPLETE) {
            return;
        }

        // Transition to CANCELLED state
        transitionTo(StreamingState.CANCELLED);

        // Flush any remaining chunks in queue (ADR-047)
        // FIX BUG #8: Add exception handling to prevent inconsistent state
        synchronized (queueLock) {
            if (!chunkQueue.isEmpty()) {
                for (String chunk : chunkQueue) {
                    content.append(chunk);
                    try {
                        // Use unified renderer (ADR-047 Phase 4)
                        markdownRenderer.appendChunk(chunk);
                    } catch (Exception e) {
                        log.warn("Error appending chunk to renderer during cancellation: " + e.getMessage());
                        // Continue processing remaining chunks
                    }
                }
                chunkQueue.clear();
            }
            renderScheduled = false;
        }

        // Update display to remove cursor and show termination notice on new line
        // Use unified renderer's current state (progressive rendering) (ADR-047 Phase 4)
        String html;
        if (markdownRenderer != null && markdownRenderer.hasContent()) {
            html = markdownRenderer.renderFinal();
        } else {
            html = Util.maskHTML(content.getDisplayableText(), true).replace("\n", "<br/>");
        }
        String terminatedHtml = "<div style='margin-top: 12px; color: #888; font-style: italic;'>AI request cancelled</div>";
        streamingContent.setContent("<div class='ai-markdown-content'>" + html + "</div>" + terminatedHtml);

        // Update any running tools to show cancelled status
        for (ToolEvent event : toolEvents) {
            if (event.getStatus() == ToolStatus.RUNNING) {
                event.setStatus(ToolStatus.ERROR);
                event.setResult("Cancelled");
            }
        }
        updateToolsDisplay();

        // Don't show copy button for cancelled messages
    }

    // ============= Private UI Update Methods =============

    /**
     * Update the main content display during streaming.
     *
     * <p><b>PROGRESSIVE MARKDOWN RENDERING:</b> During streaming, renders markdown progressively
     * as closing markers arrive (bold, italic, code, etc.). This matches ChatGPT's UX where
     * **bold** appears as &lt;strong&gt; immediately when closing ** arrives.
     *
     * <p><b>Behavior:</b>
     * <ul>
     *   <li>During streaming: Unified progressive rendering via {@link StreamingMarkdownRenderer}</li>
     *   <li>Handles all content: markdown (bold, italic, code) + tables (cell-by-cell) in single pass</li>
     *   <li>On completion: Keep streamed HTML as-is (no re-parsing) + post-process zoom links</li>
     * </ul>
     *
     * <p><b>Unified Renderer Architecture (ADR-047 Phase 4):</b>
     * <ol>
     *   <li><b>Single renderer:</b> StreamingMarkdownRenderer handles all content types</li>
     *   <li><b>Inline tables:</b> Tables rendered as IN_TABLE state, no separate renderer</li>
     *   <li><b>Correct ordering:</b> Content appears exactly where it is in stream</li>
     * </ol>
     */
    /**
     * Update the content display with progressive rendering.
     *
     * <p><b>Unified Renderer (ADR-047 Phase 4):</b> Single StreamingMarkdownRenderer handles
     * all content types (markdown + tables) without dual-processing or manual HTML merging.
     *
     * <p><b>Benefits:</b>
     * <ul>
     *   <li>No content duplication (one renderer = one output)</li>
     *   <li>Correct content ordering (tables appear where they are in stream)</li>
     *   <li>Simpler state management (no STREAMING_TEXT vs STREAMING_TABLE)</li>
     *   <li>Better performance (one pass instead of two)</li>
     * </ul>
     */
    private void updateContentDisplay() {
        String html;

        // Use unified renderer for all content (markdown + tables)
        if (markdownRenderer != null && markdownRenderer.hasContent()) {
            // UNIFIED RENDERING: Progressive markdown and table formatting
            // Handles: bold, italic, code, headings, tables (cell-by-cell), etc.
            html = markdownRenderer.renderCurrentState();

            // Show cursor if still streaming
            if (state != StreamingState.COMPLETE && state != StreamingState.CANCELLED && state != StreamingState.ERROR) {
                html += "<span class='streaming-cursor'>|</span>";
            }
        }
        // Fallback: Plain text (shouldn't happen normally)
        else {
            // Use getDisplayableText() to avoid incomplete surrogates
            String text = content.getDisplayableText();

            html = Util.maskHTML(text, true)
                .replace("\n", "<br/>")
                .replace("  ", " &nbsp;");

            // Show cursor if still streaming
            if (state != StreamingState.COMPLETE && state != StreamingState.CANCELLED && state != StreamingState.ERROR) {
                html += "<span class='streaming-cursor'>|</span>";
            }
        }

        // Don't wrap here - renderFinalMarkdown() adds the wrapper
        streamingContent.setContent(html);
    }

    /**
     * Update the tools timeline display.
     */
    private void updateToolsDisplay() {
        StringBuilder html = new StringBuilder();
        html.append("<div class='ai-tools-timeline'>");
        for (ToolEvent event : toolEvents) {
            html.append(renderToolEvent(event));
        }
        html.append("</div>");
        toolsContent.setContent(html.toString());
    }

    /**
     * Update the thinking section display.
     */
    private void updateThinkingDisplay() {
        String escapedThinking = Util.maskHTML(thinking.toString(), true)
            .replace("\n", "<br/>");
        thinkingContent.setContent(escapedThinking);
    }

    /**
     * Render a single tool event.
     */
    private String renderToolEvent(ToolEvent event) {
        String icon;
        String statusClass;
        switch (event.getStatus()) {
            case RUNNING:
                icon = "<span class='tool-spinner'>&#8635;</span>";
                statusClass = "running";
                break;
            case COMPLETE:
                icon = "<span class='tool-complete'>&#10003;</span>";
                statusClass = "complete";
                break;
            case ERROR:
                icon = "<span class='tool-error'>&#10007;</span>";
                statusClass = "error";
                break;
            default:
                icon = "";
                statusClass = "";
        }

        String displayName = getToolDisplayName(event.getName());

        return String.format(
            "<div class='tool-event tool-%s' style='display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 11px; color: #555;'>" +
            "%s <span class='tool-name' style='color: #333;'>%s</span>" +
            "</div>",
            statusClass, icon, Util.maskHTML(displayName, true)
        );
    }

    /**
     * Map internal tool names to user-friendly display names.
     */
    private String getToolDisplayName(String toolName) {
        // Get language code for localization - use context language (ADR-037)
        // TEMPORARILY using Env.getAD_Language() - session-level language detection will be added in future phase
        String langCode = Env.getAD_Language(ctx);
        if (langCode == null) langCode = "en_US";

        // Map tool names to AD_Message keys
        String messageKey = null;
        switch (toolName) {
            case "queryDatabase":
                messageKey = "AIG_QueryDatabase";
                break;
            case "executeQuery":
                messageKey = "AIG_ExecuteQuery";
                break;
            case "lookupRecord":
                messageKey = "AIG_LookupRecord";
                break;
            case "searchRecords":
                messageKey = "AIG_SearchRecords";
                break;
            case "getTableMetadata":
                messageKey = "AIG_GetTableMetadata";
                break;
            case "listTables":
                messageKey = "AIG_ListTables";
                break;
            case "getBusinessPartner":
                messageKey = "AIG_GetBusinessPartner";
                break;
            case "getProduct":
                messageKey = "AIG_GetProduct";
                break;
            case "getOrder":
                messageKey = "AIG_GetOrder";
                break;
            case "getWindowContext":
                messageKey = "AIG_GetWindowContext";
                break;
            case "calculateMetrics":
                messageKey = "AIG_CalculateMetrics";
                break;
        }

        if (messageKey != null) {
            // Use iDempiere's Msg.getMsg() which automatically handles language from context
            String message = Msg.getMsg(ctx, messageKey);
            // If message not found, fall back to tool name
            return (message != null && !message.equals(messageKey)) ? message : toolName + "...";
        }

        return toolName + "...";
    }

    /**
     * Toggle thinking section expanded/collapsed.
     */
    private void toggleThinking() {
        isThinkingExpanded = !isThinkingExpanded;

        String displayStyle = isThinkingExpanded ? "block" : "none";
        String toggleChar = isThinkingExpanded ? "&#9660;" : "&#9654;"; // Down or right arrow

        thinkingContent.setStyle("display: " + displayStyle + "; padding: 12px; font-size: 11px; " +
            "color: #555; line-height: 1.5; max-height: 200px; overflow-y: auto; " +
            "border-top: 1px solid #E8E8E8; background: #FDFDFD;");

        // Update toggle arrow
        String headerHtml = thinkingHeader.getFirstChild() instanceof Html ?
            ((Html) thinkingHeader.getFirstChild()).getContent() : "";
        headerHtml = headerHtml.replaceFirst("&#\\d+;", toggleChar);
        ((Html) thinkingHeader.getFirstChild()).setContent(headerHtml);
    }

    /**
     * Render final content using streaming renderers.
     *
     * <p><b>PROGRESSIVE MARKDOWN APPROACH:</b> Unlike previous implementation (CLD-1653),
     * we now render markdown progressively during streaming via {@link StreamingMarkdownRenderer}.
     * This matches ChatGPT's UX where formatting appears immediately as markers close.
     *
     * <p><b>On Completion:</b> We DON'T re-parse markdown. We keep the HTML that was
     * progressively built during streaming and only apply post-processing:
     * <ul>
     *   <li>Zoom link processing (requires context, can't be done mid-stream)</li>
     *   <li>Syntax highlighting (Prism.js via JavaScript)</li>
     * </ul>
     *
     * <p><b>Rendering Priority:</b>
     * <ol>
     *   <li>Table renderer (if table was streamed)</li>
     *   <li>Markdown renderer (if markdown was streamed)</li>
     *   <li>Fallback to AIMessageRenderer (legacy path)</li>
     * </ol>
     */
    /**
     * Render final markdown with unified renderer (ADR-047 Phase 4).
     *
     * <p><b>Simplified Architecture:</b> Single StreamingMarkdownRenderer handles
     * all content (markdown + tables) without dual-processing or manual HTML merging.
     * No more complex hasTable/hasMarkdown logic - just render and display.
     */
    private void renderFinalMarkdown() {
        String finalHtml;

        // Validate complete markdown BEFORE final render
        // MarkdownValidator was removed from streaming pipeline (breaks cross-chunk markdown)
        // Now validate the complete accumulated content before final rendering
        String completeMarkdown = content.toString();
        String validatedMarkdown = com.cloudempiere.ai.util.MarkdownValidator.validate(completeMarkdown);

        // Strip HTML tags from raw markdown (LLM should only use Markdown, not HTML)
        // This ensures HTML tags don't appear as escaped text in the final output
        validatedMarkdown = com.cloudempiere.ai.util.HtmlStripper.stripHtml(validatedMarkdown);

        // If validation changed the markdown, we need to re-parse with fresh renderer
        // This only happens if the AI generated malformed markdown (unclosed markers, etc.)
        if (!validatedMarkdown.equals(completeMarkdown)) {
            log.warn("Markdown validation fixed structure, re-rendering from validated content");

            // Create fresh renderer for validated content
            com.cloudempiere.ai.util.StreamingMarkdownRenderer freshRenderer =
                new com.cloudempiere.ai.util.StreamingMarkdownRenderer();
            freshRenderer.appendChunk(validatedMarkdown);

            finalHtml = freshRenderer.renderFinal();
            finalHtml = "<div class='ai-markdown-content'>" + finalHtml + "</div>";

            // Update content for display
            String contentId = "content_" + componentId;
            streamingContent.setId(contentId);
            ((Html) streamingContent).setContent(finalHtml);
            return;
        }

        // Use unified renderer (handles markdown AND tables in single pass)
        if (markdownRenderer != null && markdownRenderer.hasContent()) {
            log.debug("[FINAL-RENDER] Using unified renderer (markdown + tables)");

            finalHtml = markdownRenderer.renderFinal();

            // Post-process: Convert zoom link syntax to clickable links
            if (ctx != null && parentWidgetId != null) {
                finalHtml = ZoomLinkProcessor.processZoomLinks(finalHtml, ctx, parentWidgetId);
            }

            finalHtml = "<div class='ai-markdown-content'>" + finalHtml + "</div>";
        }
        // Fallback: Use AIMessageRenderer (for messages loaded from DB without streaming)
        else {
            log.debug("[FINAL-RENDER] Using AIMessageRenderer (fallback for non-streamed messages)");
            String markdownText = content.flush();

            finalHtml = com.cloudempiere.ai.util.AIMessageRenderer.render(
                markdownText, ctx, parentWidgetId, locale);
        }

        // Set the final HTML content directly (no JavaScript escaping needed for setContent)
        streamingContent.setContent(finalHtml);

        // Use the existing streamingContent element's ID (content_[componentId])
        String contentId = "content_" + componentId;

        // Apply syntax highlighting if available (via JavaScript)
        String script =
            "(function() {" +
            "  var applyHighlight = function() {" +
            "    if (!window.Prism) return;" +
            // Find the streamingContent element by its ZK-generated ID
            "    var zkWidget = zk.Widget.$('$" + contentId + "');" +
            "    var container = zkWidget ? zkWidget.$n() : document.getElementById('" + contentId + "');" +
            "    if (!container) {" +
            "      container = document.querySelector('.ai-streaming-message .ai-markdown-content');" +
            "    }" +
            "    if (!container) return;" +
            "    var mdContainer = container.querySelector('.ai-markdown-content') || container;" +
            // Apply Prism highlighting
            "    mdContainer.querySelectorAll('pre code').forEach(function(block) {" +
            "      Prism.highlightElement(block);" +
            "    });" +
            "  };" +
            // Use setTimeout to ensure ZK has processed the DOM update first
            "  setTimeout(applyHighlight, 50);" +
            "})();";

        org.zkoss.zk.ui.util.Clients.evalJavaScript(script);
    }

    /**
     * Enable the copy button after completion.
     */
    private void enableCopyButton() {
        copyButton.setVisible(true);

        // Use script tag with proper escaping to avoid inline onclick issues
        String copyHtml =
            "<div id='copy_btn_" + componentId + "' " +
            "style='display: flex; align-items: center; gap: 6px; cursor: pointer;'>" +
            "<i class='z-icon-Copy' style='font-size: 12px; color: #717680;'></i>" +
            "<span style='font-size: 10.5px; color: #717680;'>Copy</span>" +
            "</div>" +
            "<script>" +
            "(function(){" +
            "var btn=document.getElementById('copy_btn_" + componentId + "');" +
            "if(!btn)return;" +
            "btn.onclick=function(){" +
            "var orig=this.innerHTML;" +
            "var text=" + escapeForJavaScript(content.getDisplayableText()) + ";" +
            "navigator.clipboard.writeText(text).then(function(){" +
            "btn.innerHTML='<span style=\"font-size:10.5px;color:#4CAF50;\">Copied!</span>';" +
            "setTimeout(function(){btn.innerHTML=orig;},2000);" +
            "}).catch(function(err){console.error('Copy failed:',err);});" +
            "};" +
            "})();" +
            "</script>";

        copyButton.getChildren().clear();
        Html copyHtmlContent = new Html(copyHtml);
        copyButton.appendChild(copyHtmlContent);
    }

    /**
     * Escape a string for use in JavaScript, returning a quoted string literal.
     *
     * @param text the text to escape
     * @return a JavaScript string literal (including quotes)
     */
    private String escapeForJavaScript(String text) {
        if (text == null) {
            return "''";
        }
        // Use JSON-style escaping which is safe for JavaScript
        String escaped = text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("</script>", "<\\/script>");
        return "'" + escaped + "'";
    }

    /**
     * Truncate result string for display.
     */
    private String truncateResult(String result, int maxLength) {
        if (result == null) {
            return null;
        }
        if (result.length() <= maxLength) {
            return result;
        }
        return result.substring(0, maxLength - 3) + "...";
    }

    // ============= Tool Event Tracking =============

    /**
     * Tool event for tracking tool execution status.
     * Package-private to avoid OSGi classloading issues with private inner classes.
     */
    static class ToolEvent {
        private final String name;
        private ToolStatus status;
        private final String arguments;
        private String result;

        ToolEvent(String name, ToolStatus status, String arguments, String result) {
            this.name = name;
            this.status = status;
            this.arguments = arguments;
            this.result = result;
        }

        String getName() { return name; }
        ToolStatus getStatus() { return status; }
        void setStatus(ToolStatus status) { this.status = status; }
        String getArguments() { return arguments; }
        String getResult() { return result; }
        void setResult(String result) { this.result = result; }
    }

    /**
     * Tool execution status.
     * Package-private to avoid OSGi classloading issues with private inner enums.
     */
    enum ToolStatus {
        RUNNING,
        COMPLETE,
        ERROR
    }

    /**
     * Streaming message lifecycle state.
     *
     * <p><b>State Machine (CLD-1704):</b> Explicit state management replacing boolean flags.
     * Provides single source of truth for component lifecycle and enables clear state transitions.
     *
     * <p><b>Valid Transitions:</b>
     * <pre>
     * IDLE → WAITING_FOR_LLM
     * WAITING_FOR_LLM → {THINKING, STREAMING_TEXT, TOOL_EXECUTING}
     * TOOL_EXECUTING → {STREAMING_TEXT, TOOL_EXECUTING, COMPLETE}
     * THINKING → STREAMING_TEXT
     * STREAMING_TEXT → FINALIZING → COMPLETE
     * Any state → CANCELLED (user action)
     * Any state → ERROR (failure)
     *
     * Note: Unified renderer (ADR-047 Phase 4) handles all content types in STREAMING_TEXT state.
     * No separate STREAMING_TABLE state needed - tables rendered progressively as part of text stream.
     * </pre>
     *
     * <p>Package-private to avoid OSGi classloading issues with private inner enums.
     */
    enum StreamingState {
        /** No active operation, ready for new request */
        IDLE,

        /** Request sent to LLM, waiting for first token */
        WAITING_FOR_LLM,

        /** LLM requested tool use, executing database query or other tool */
        TOOL_EXECUTING,

        /** Extended thinking mode (not streaming text yet) */
        THINKING,

        /** Streaming content chunks (unified renderer handles markdown + tables) (ADR-047 Phase 4) */
        STREAMING_TEXT,

        /** All chunks received, rendering final markdown */
        FINALIZING,

        /** Successfully completed, final markdown rendered */
        COMPLETE,

        /** User cancelled the request */
        CANCELLED,

        /** Failed (network, API, validation error) */
        ERROR
    }
}
