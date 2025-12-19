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

import com.cloudempiere.ai.util.ChunkCleaner;
import com.cloudempiere.ai.util.CommonMarkRenderer;
import com.cloudempiere.ai.util.MarkdownRenderer;
import com.cloudempiere.ai.util.MarkdownTableRenderer;
import com.cloudempiere.ai.util.StreamingTableRenderer;
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

    /** Whether streaming is complete */
    private boolean isComplete = false;

    /** Whether request was cancelled by user */
    private boolean isCancelled = false;

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

    /** Chat ID for session language lookup */
    private int chatId;

    /** Language detection service for session language (ADR-037) */
    private com.cloudempiere.ai.service.LanguageDetectionService languageService;

    /** Streaming table renderer for cell-by-cell table rendering */
    private StreamingTableRenderer tableRenderer;

    // ============= Throttled Rendering (ADR-047) =============

    /** Queue for batching chunks before rendering (throttling to 50ms intervals) */
    private final List<String> chunkQueue = new ArrayList<>();

    /** Flag to track if render is scheduled */
    private boolean renderScheduled = false;

    /** Lock object for synchronizing chunk queue access */
    private final Object queueLock = new Object();

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
        this.chatId = chatId;
        this.languageService = chatId > 0 ? com.cloudempiere.ai.service.LanguageDetectionService.getInstance() : null;

        // Initialize streaming table renderer for cell-by-cell rendering
        this.tableRenderer = new StreamingTableRenderer();
        int clientId = org.compiere.util.Env.getAD_Client_ID(ctx);
        log.warn("[STREAM-INIT] Setting context on StreamingTableRenderer | AD_Client_ID=" + clientId);
        this.tableRenderer.setContext(ctx, parentWidgetId);

        injectCSS();
        init();
    }

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
        if (isCancelled || isComplete) {
            return;
        }
        if (chunk == null || chunk.isEmpty()) {
            return;
        }

        // Clean chunk before processing (ADR-047 Phase 1)
        String cleaned = ChunkCleaner.clean(chunk);

        // Queue chunk for batched rendering (ADR-047 Phase 2)
        synchronized (queueLock) {
            chunkQueue.add(cleaned);

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
     * <p>Uses ZK's client-side timer mechanism to throttle DOM updates.
     * Multiple chunks are batched together and rendered once per interval.
     *
     * <p><b>Performance Impact:</b>
     * <ul>
     *   <li>Before: 100+ DOM updates/sec (per chunk)</li>
     *   <li>After: ~20 DOM updates/sec (every 50ms)</li>
     *   <li>Result: 80% reduction in browser reflows</li>
     * </ul>
     */
    private void scheduleRender() {
        // Use JavaScript setTimeout to schedule server callback after 50ms
        // This ensures smooth 20 FPS rendering without overwhelming the browser
        String script = String.format(
            "setTimeout(function() {" +
            "  var w = zk.Widget.$('%s');" +
            "  if (w) {" +
            "    zAu.send(new zk.Event(w, 'onBatchRender'));" +
            "  }" +
            "}, 50);",  // 50ms = 20 FPS (smooth without overhead)
            getId()
        );

        org.zkoss.zk.ui.util.Clients.evalJavaScript(script);
    }

    /**
     * Handle batched render event (triggered every 50ms by scheduleRender).
     *
     * <p>This method:
     * <ol>
     *   <li>Collects all queued chunks</li>
     *   <li>Processes them through content buffer and table renderer</li>
     *   <li>Updates DOM once for entire batch</li>
     *   <li>Reschedules if more chunks arrived during processing</li>
     * </ol>
     *
     * <p>Called via ZK event system from client-side JavaScript timer.
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

            // Feed chunk to streaming table renderer for cell-by-cell rendering
            tableRenderer.appendChunk(chunk);
        }

        // Update DOM once for entire batch
        updateContentDisplay();

        // Reschedule if more chunks arrived during processing
        synchronized (queueLock) {
            if (!chunkQueue.isEmpty()) {
                scheduleRender();
            } else {
                renderScheduled = false;
            }
        }
    }

    /**
     * Show tool execution start in the timeline.
     *
     * @param toolName name of the tool being executed
     * @param arguments tool arguments (JSON string)
     */
    public void showToolStart(String toolName, String arguments) {
        toolsSection.setVisible(true);
        toolEvents.add(new ToolEvent(toolName, ToolStatus.RUNNING, arguments, null));
        updateToolsDisplay();
    }

    /**
     * Update tool to complete status in the timeline.
     *
     * @param toolName name of the tool that completed
     * @param result result of the tool execution
     */
    public void showToolComplete(String toolName, String result) {
        for (ToolEvent event : toolEvents) {
            if (event.getName().equals(toolName) && event.getStatus() == ToolStatus.RUNNING) {
                event.setStatus(ToolStatus.COMPLETE);
                event.setResult(truncateResult(result, 100));
                break;
            }
        }
        updateToolsDisplay();
    }

    /**
     * Show tool error in the timeline.
     *
     * @param toolName name of the tool that failed
     * @param error error message
     */
    public void showToolError(String toolName, String error) {
        for (ToolEvent event : toolEvents) {
            if (event.getName().equals(toolName) && event.getStatus() == ToolStatus.RUNNING) {
                event.setStatus(ToolStatus.ERROR);
                event.setResult(error);
                break;
            }
        }
        updateToolsDisplay();
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
        if (isComplete) {
            return; // Already completed
        }

        // Flush any remaining chunks in queue (ADR-047)
        synchronized (queueLock) {
            if (!chunkQueue.isEmpty()) {
                for (String chunk : chunkQueue) {
                    content.append(chunk);
                    tableRenderer.appendChunk(chunk);
                }
                chunkQueue.clear();
            }
            renderScheduled = false;
        }

        isComplete = true;
        // Use marked.js for full markdown rendering (tables, code blocks, etc.)
        renderFinalMarkdown();
        enableCopyButton();
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
     * Check if streaming is complete.
     *
     * @return true if complete
     */
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * Check if request was cancelled.
     *
     * @return true if cancelled
     */
    public boolean isCancelled() {
        return isCancelled;
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
        if (tableRenderer != null) {
            tableRenderer.setLocale(this.locale);
        }
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
        if (isCancelled || isComplete) {
            return;
        }

        // Flush any remaining chunks in queue (ADR-047)
        synchronized (queueLock) {
            if (!chunkQueue.isEmpty()) {
                for (String chunk : chunkQueue) {
                    content.append(chunk);
                    tableRenderer.appendChunk(chunk);
                }
                chunkQueue.clear();
            }
            renderScheduled = false;
        }

        isCancelled = true;
        isComplete = true;

        // Update display to remove cursor and show termination notice on new line
        String html = renderPartialMarkdown(content.getDisplayableText());
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
     * Update the main content display.
     *
     * <p><b>ADR-047 Phase 2.5:</b> Uses CommonMarkRenderer for consistent rendering
     * during both streaming and final phases. This eliminates content jumps and
     * markdown leaks caused by the previous dual-path approach (regex during
     * streaming, CommonMark after completion).
     */
    private void updateContentDisplay() {
        String html;

        // Use streaming table renderer if we're currently in a table
        if (tableRenderer != null && tableRenderer.isInTable()) {
            // Cell-by-cell rendering for smooth table streaming
            html = tableRenderer.renderCurrentState();
        } else {
            // Use getDisplayableText() to avoid incomplete surrogates
            String markdownText = content.getDisplayableText();

            // Pre-render tables if present (before CommonMark parsing)
            if (MarkdownTableRenderer.containsTable(markdownText)) {
                int clientId = org.compiere.util.Env.getAD_Client_ID(ctx);
                log.warn("[STREAMING] Setting context on MarkdownTableRenderer | AD_Client_ID=" + clientId);
                MarkdownTableRenderer.setLocale(locale);
                MarkdownTableRenderer.setContext(ctx);
                MarkdownTableRenderer.setWidgetId(parentWidgetId);
                try {
                    markdownText = MarkdownTableRenderer.renderTables(markdownText);
                } finally {
                    MarkdownTableRenderer.clearLocale();
                    MarkdownTableRenderer.clearZoomContext();
                }
            }

            // Use CommonMarkRenderer for consistent rendering (ADR-047 Phase 2.5)
            // This ensures streaming and final rendering are identical
            html = processMarkdownPreservingHTML(markdownText);

            if (!isComplete) {
                html += "<span class='streaming-cursor'>|</span>";
            }
        }

        streamingContent.setContent("<div class='ai-markdown-content'>" + html + "</div>");
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
        // Get language code for localization - use session language if available (ADR-037)
        String langCode;
        if (languageService != null && chatId > 0) {
            langCode = languageService.getSessionLanguage(ctx, chatId);
        } else {
            langCode = Env.getAD_Language(ctx);
        }
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
     * Render final content using CommonMark Java library for full markdown support.
     *
     * <p><b>ADR-047 Phase 2.5:</b> This uses the same CommonMarkRenderer used during streaming
     * to ensure consistent rendering throughout the message lifecycle. This eliminates content
     * jumps and layout shifts that occurred with the previous dual-path approach.
     *
     * <p><b>Implementation Note:</b> We use the existing content_[componentId] element that was
     * created in init() rather than generating a new container ID. This ensures the DOM element
     * already exists when the JavaScript executes, avoiding race conditions between ZK's DOM
     * update and our script execution.
     */
    private void renderFinalMarkdown() {
        String markdownText = content.flush();

        // Normalize excessive line breaks (3+ newlines → 2 newlines for proper paragraph spacing)
        markdownText = markdownText.replaceAll("\n{3,}", "\n\n");

        // Remove hallucinated function call XML blocks
        markdownText = markdownText.replaceAll("(?s)<function_calls>.*?</function_calls>", "");
        markdownText = markdownText.replaceAll("(?s)<function_result>.*?</function_result>", "");
        markdownText = markdownText.replaceAll("(?s)<function_calls>.*$", "");
        markdownText = markdownText.replaceAll("(?s)<function_result>.*$", "");
        markdownText = markdownText.replaceAll("(?s)<invoke[^>]*>.*?</invoke>", "");
        markdownText = markdownText.replaceAll("(?s)<parameter[^>]*>.*?</parameter>", "");

        // Pre-render tables using the streaming table renderer's final output
        // This provides consistent rendering between streaming and final display
        if (tableRenderer != null && tableRenderer.isInTable()) {
            // Use streaming table renderer for final output (already processed)
            markdownText = tableRenderer.renderFinal();
        } else if (MarkdownTableRenderer.containsTable(markdownText)) {
            // Fall back to standard renderer if table wasn't streamed
            int clientId = org.compiere.util.Env.getAD_Client_ID(ctx);
            log.warn("[FINAL-RENDER] Setting context on MarkdownTableRenderer | AD_Client_ID=" + clientId);
            MarkdownTableRenderer.setLocale(locale);
            MarkdownTableRenderer.setContext(ctx);
            MarkdownTableRenderer.setWidgetId(parentWidgetId);
            try {
                markdownText = MarkdownTableRenderer.renderTables(markdownText);
            } finally {
                MarkdownTableRenderer.clearLocale();
                MarkdownTableRenderer.clearZoomContext();
            }
        }

        // Process zoom links if parent widget ID is available (ADR-039)
        // Uses explicit syntax [[Table:ID|Display]] - AI is instructed to format references this way.
        // Note: This processes links OUTSIDE tables. Links inside tables are handled above.
        if (parentWidgetId != null) {
            markdownText = ZoomLinkProcessor.processZoomLinks(markdownText, ctx, parentWidgetId);

            // FUTURE (ADR-039): Pattern-based extraction for natural references like "SO-1234"
            // Currently bypassed - requires vector DB for fast lookup across 2000+ tables.
            // See RecordReferenceExtractor and ChatRecordLinkRenderer for implementation.
            // Uncomment when vector DB caching is available:
            // markdownText = ChatRecordLinkRenderer.extractAndRender(markdownText, ctx, parentWidgetId);
        }

        // Normalize excessive line breaks AGAIN after table/link rendering (ADR-047 Phase 1)
        // Table rendering may have preserved or added extra newlines around tables
        markdownText = markdownText.replaceAll("\n{3,}", "\n\n");

        // Ensure headings have proper line breaks before them for markdown parsing
        // Fix cases where AI doesn't put newline before heading: "text## Heading" → "text\n## Heading"
        markdownText = markdownText.replaceAll("([^\n])(\n?)(#{1,3} )", "$1\n\n$3");

        // Use the existing streamingContent element's ID (content_[componentId])
        // This element was created in init() and already exists in the DOM
        String contentId = "content_" + componentId;

        // At this point, markdownText contains:
        // - Pre-rendered HTML tables (with zoom links)
        // - HTML zoom links outside tables
        // - Raw markdown for everything else (headings, bold, lists, etc.)
        //
        // We need to process the remaining markdown WITHOUT corrupting the HTML we've already generated.
        // Solution: Process markdown ONLY on non-HTML parts
        String finalHtml = processMarkdownPreservingHTML(markdownText);

        // Set the final HTML content directly (no JavaScript escaping needed for setContent)
        streamingContent.setContent("<div class='ai-markdown-content'>" + finalHtml + "</div>");

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
     * Render partial markdown (simple implementation for streaming).
     *
     * <p><b>DEPRECATED (ADR-047 Phase 2.5):</b> This method used regex-based
     * MarkdownRenderer which caused inconsistencies with the CommonMark-based
     * final rendering. Use {@link #processMarkdownPreservingHTML(String)} instead
     * for consistent rendering in both streaming and final phases.
     *
     * <p>This method is kept for backward compatibility but now delegates to
     * the unified CommonMark renderer to ensure consistency.
     *
     * @deprecated Use {@link #processMarkdownPreservingHTML(String)} for unified
     *             CommonMark-based rendering. This method will be removed in v0.32.0.
     */
    @Deprecated
    private String renderPartialMarkdown(String text) {
        // ADR-047 Phase 2.5: Delegate to unified CommonMark renderer
        // This ensures consistency between streaming and final rendering
        log.warn("renderPartialMarkdown() is deprecated - using CommonMark renderer");
        return processMarkdownPreservingHTML(text);
    }

    /**
     * Escape HTML in non-table content while preserving table HTML tags.
     *
     * <p>This splits the content by table tags, escapes the non-table parts,
     * and reassembles them.
     */
    private String escapeNonTableContent(String content) {
        StringBuilder result = new StringBuilder();
        int pos = 0;

        while (pos < content.length()) {
            int tableStart = content.indexOf("<table", pos);
            if (tableStart == -1) {
                // No more tables - escape the rest
                result.append(Util.maskHTML(content.substring(pos), true));
                break;
            }

            // Escape content before the table
            if (tableStart > pos) {
                result.append(Util.maskHTML(content.substring(pos, tableStart), true));
            }

            // Find the end of the table
            int tableEnd = content.indexOf("</table>", tableStart);
            if (tableEnd == -1) {
                // Incomplete table - keep as is (will be completed in next chunk)
                result.append(content.substring(tableStart));
                break;
            }
            tableEnd += "</table>".length();

            // Append table as-is (already has escaped cell content)
            result.append(content.substring(tableStart, tableEnd));
            pos = tableEnd;
        }

        return result.toString();
    }

    /**
     * Process markdown while preserving existing HTML (tables and zoom links).
     */
    private String processMarkdownPreservingHTML(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        int pos = 0;

        while (pos < text.length()) {
            int tagStart = text.indexOf('<', pos);

            if (tagStart == -1) {
                result.append(processSimpleMarkdown(text.substring(pos)));
                break;
            }

            if (tagStart > pos) {
                result.append(processSimpleMarkdown(text.substring(pos, tagStart)));
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
        return tagName.equals("br") || tagName.equals("hr") ||
               tagName.equals("img") || tagName.equals("input");
    }

    /**
     * Process simple markdown (for final rendering with HTML preservation).
     *
     * <p>Uses CommonMark Java library for full markdown support (ADR-047).
     * Preserves pre-rendered HTML elements (tables, zoom links).
     *
     * <p><b>Supported Features:</b>
     * <ul>
     *   <li>Numbered lists (1. 2. 3.)</li>
     *   <li>Unordered lists (-, *)</li>
     *   <li>Nested lists</li>
     *   <li>Code blocks (```)</li>
     *   <li>Headings (# ## ###)</li>
     *   <li>Bold, italic, strikethrough</li>
     *   <li>Links and images</li>
     *   <li>Blockquotes</li>
     * </ul>
     *
     * @param text markdown text to process
     * @return HTML output
     */
    private String processSimpleMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Use CommonMark for full markdown support (ADR-047)
        // This handles lists, code blocks, and all standard markdown
        return CommonMarkRenderer.render(text);
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
}
