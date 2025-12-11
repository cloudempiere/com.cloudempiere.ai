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
import org.compiere.util.Util;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;

import com.cloudempiere.ai.util.ChatRecordLinkRenderer;
import com.cloudempiere.ai.util.MarkdownTableRenderer;
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

    /**
     * Create a new streaming message component.
     */
    public AIChatStreamingMessage() {
        this(Env.getCtx(), null);
    }

    /**
     * Create a new streaming message component with context for zoom links.
     *
     * @param ctx iDempiere context for database access
     * @param parentWidgetId parent widget ID for zoom event targeting (fires onZoom)
     */
    public AIChatStreamingMessage(Properties ctx, String parentWidgetId) {
        super();
        this.componentId = "stream_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        this.ctx = ctx != null ? ctx : Env.getCtx();
        this.parentWidgetId = parentWidgetId;
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
        content.append(chunk);
        updateContentDisplay();
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
     */
    private void updateContentDisplay() {
        // Use getDisplayableText() to avoid incomplete surrogates
        String html = renderPartialMarkdown(content.getDisplayableText());
        if (!isComplete) {
            html += "<span class='streaming-cursor'>|</span>";
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
        switch (toolName) {
            case "queryDatabase":
                return "Querying database...";
            case "executeQuery":
                return "Running query...";
            case "lookupRecord":
                return "Looking up record...";
            case "searchRecords":
                return "Searching records...";
            case "getTableMetadata":
                return "Reading table metadata...";
            case "listTables":
                return "Listing tables...";
            case "getBusinessPartner":
                return "Looking up business partner...";
            case "getProduct":
                return "Looking up product...";
            case "getOrder":
                return "Looking up order...";
            case "getWindowContext":
                return "Reading window data...";
            case "calculateMetrics":
                return "Calculating metrics...";
            default:
                return toolName + "...";
        }
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
     * Render final content using marked.js for full markdown support (tables, code blocks, etc.).
     *
     * <p>This replaces the simple renderPartialMarkdown() with client-side marked.js rendering.
     *
     * <p><b>Implementation Note:</b> We use the existing content_[componentId] element that was
     * created in init() rather than generating a new container ID. This ensures the DOM element
     * already exists when the JavaScript executes, avoiding race conditions between ZK's DOM
     * update and our script execution.
     */
    private void renderFinalMarkdown() {
        String markdownText = content.flush();

        // Remove hallucinated function call XML blocks
        markdownText = markdownText.replaceAll("(?s)<function_calls>.*?</function_calls>", "");
        markdownText = markdownText.replaceAll("(?s)<function_result>.*?</function_result>", "");
        markdownText = markdownText.replaceAll("(?s)<function_calls>.*$", "");
        markdownText = markdownText.replaceAll("(?s)<function_result>.*$", "");
        markdownText = markdownText.replaceAll("(?s)<invoke[^>]*>.*?</invoke>", "");
        markdownText = markdownText.replaceAll("(?s)<parameter[^>]*>.*?</parameter>", "");

        // Pre-render tables BEFORE passing to marked.js (ADR-039)
        // This ensures zoom links in table cells are processed correctly
        if (MarkdownTableRenderer.containsTable(markdownText)) {
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

        // Use the existing streamingContent element's ID (content_[componentId])
        // This element was created in init() and already exists in the DOM
        String contentId = "content_" + componentId;

        // Escape for JavaScript
        String escapedMarkdown = markdownText
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "")
            .replace("\n", "\\n")
            .replace("</script>", "<\\/script>");

        // At this point, markdownText contains:
        // - Pre-rendered HTML tables (with zoom links)
        // - HTML zoom links outside tables
        // - Raw markdown for everything else (headings, bold, lists, etc.)
        //
        // We need to process the remaining markdown WITHOUT corrupting the HTML we've already generated.
        // Solution: Process markdown ONLY on non-HTML parts
        String finalHtml = processMarkdownPreservingHTML(markdownText);
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
     * <p>For full markdown rendering, the final content uses marked.js on the client.
     * This method provides real-time rendering during streaming for:
     * <ul>
     *   <li>Headings (h1-h4)</li>
     *   <li>Bold and italic text</li>
     *   <li>Inline code</li>
     *   <li>Lists</li>
     *   <li>Tables (GFM pipe tables)</li>
     * </ul>
     */
    private String renderPartialMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Remove hallucinated function call XML blocks (model without tools may generate these)
        // Pattern matches <function_calls>...</function_calls> and <function_result>...</function_result>
        String cleaned = text;
        cleaned = cleaned.replaceAll("(?s)<function_calls>.*?</function_calls>", "");
        cleaned = cleaned.replaceAll("(?s)<function_result>.*?</function_result>", "");
        // Also remove incomplete/partial tags that may appear during streaming
        cleaned = cleaned.replaceAll("(?s)<function_calls>.*$", "");
        cleaned = cleaned.replaceAll("(?s)<function_result>.*$", "");
        cleaned = cleaned.replaceAll("(?s)<invoke[^>]*>.*?</invoke>", "");
        cleaned = cleaned.replaceAll("(?s)<parameter[^>]*>.*?</parameter>", "");

        // IMPORTANT: Tables must be rendered BEFORE HTML escaping because the table
        // detection relies on pipe characters (|) which get escaped by maskHTML().
        // The MarkdownTableRenderer handles cell content escaping internally.
        String result = cleaned;
        if (MarkdownTableRenderer.containsTable(cleaned)) {
            MarkdownTableRenderer.setLocale(locale);
            // Set context for zoom link processing in table cells (ADR-039)
            MarkdownTableRenderer.setContext(ctx);
            MarkdownTableRenderer.setWidgetId(parentWidgetId);
            try {
                result = MarkdownTableRenderer.renderTables(cleaned);
            } finally {
                MarkdownTableRenderer.clearLocale();
                MarkdownTableRenderer.clearZoomContext();
            }
        }

        // Now escape non-table content. We need to be careful here:
        // If tables were rendered, they contain HTML tags that shouldn't be escaped.
        // Split by table tags and only escape non-table parts.
        if (result.contains("<table")) {
            result = escapeNonTableContent(result);
        } else {
            // No tables - escape everything
            result = Util.maskHTML(result, true);
        }

        // Simple markdown transformations for streaming display

        // Headings: # text, ## text, ### text (must be at start of line)
        // Process headings before line breaks to preserve newline matching
        result = result.replaceAll("(?m)^### (.+)$", "<h4 style='font-size: 14px; font-weight: 600; margin: 12px 0 8px 0;'>$1</h4>");
        result = result.replaceAll("(?m)^## (.+)$", "<h3 style='font-size: 15px; font-weight: 600; margin: 14px 0 8px 0;'>$1</h3>");
        result = result.replaceAll("(?m)^# (.+)$", "<h2 style='font-size: 16px; font-weight: 600; margin: 16px 0 10px 0;'>$1</h2>");

        // Bold: **text** or __text__
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        result = result.replaceAll("__(.+?)__", "<strong>$1</strong>");

        // Italic: *text* or _text_
        result = result.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "<em>$1</em>");
        result = result.replaceAll("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)", "<em>$1</em>");

        // Code: `text`
        result = result.replaceAll("`([^`]+)`", "<code style='background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-family: monospace; font-size: 0.9em;'>$1</code>");

        // Lists: - item or * item (basic support)
        result = result.replaceAll("(?m)^- (.+)$", "<li style='margin-left: 16px; list-style-type: disc;'>$1</li>");
        result = result.replaceAll("(?m)^\\* (.+)$", "<li style='margin-left: 16px; list-style-type: disc;'>$1</li>");

        // Line breaks (after all other line-based processing)
        result = result.replace("\n", "<br/>");

        // Clean up extra <br/> after block elements
        result = result.replaceAll("</h2><br/>", "</h2>");
        result = result.replaceAll("</h3><br/>", "</h3>");
        result = result.replaceAll("</h4><br/>", "</h4>");
        result = result.replaceAll("</li><br/>", "</li>");
        result = result.replaceAll("</table><br/>", "</table>");
        result = result.replaceAll("</tr><br/>", "</tr>");
        result = result.replaceAll("</th><br/>", "</th>");
        result = result.replaceAll("</td><br/>", "</td>");

        return result;
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

    private String processSimpleMarkdown(String text) {
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
