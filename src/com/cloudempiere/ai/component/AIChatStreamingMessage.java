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

import org.compiere.util.Util;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;

import com.cloudempiere.ai.util.MarkdownTableRenderer;
import com.cloudempiere.ai.util.StreamingTextBuffer;

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

    /**
     * Create a new streaming message component.
     */
    public AIChatStreamingMessage() {
        super();
        this.componentId = "stream_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
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
            ".ai-streaming-message { display: flex; flex-direction: column; gap: 12px; padding: 12px 18px; }" +

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
            "font-size: 12px; line-height: 18px; color: #181D27;");

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

        // First, set the container with a placeholder that will be filled by marked.js
        // The container div already exists since we're using the streamingContent element
        String placeholderHtml = "<div class='ai-markdown-content'>" +
            renderPartialMarkdown(markdownText) + "</div>";
        streamingContent.setContent(placeholderHtml);

        // Then use Clients.evalJavaScript to render with marked.js after the DOM is updated
        // This runs AFTER ZK has processed all component updates
        String script =
            "(function() {" +
            "  var renderMD = function() {" +
            "    if (!window.marked) {" +
            "      setTimeout(renderMD, 100);" +
            "      return;" +
            "    }" +
            // Find the streamingContent element by its ZK-generated ID
            "    var zkWidget = zk.Widget.$('$" + contentId + "');" +
            "    var container = zkWidget ? zkWidget.$n() : document.getElementById('" + contentId + "');" +
            "    if (!container) {" +
            // Fallback: try to find by class within ai-streaming-message
            "      container = document.querySelector('.ai-streaming-message .ai-markdown-content');" +
            "    }" +
            "    if (!container) {" +
            "      console.warn('renderFinalMarkdown: container not found, retrying...');" +
            "      setTimeout(renderMD, 200);" +
            "      return;" +
            "    }" +
            // Find the ai-markdown-content div within the container
            "    var mdContainer = container.querySelector('.ai-markdown-content') || container;" +
            "    marked.setOptions({" +
            "      breaks: true," +
            "      gfm: true" +
            "    });" +
            "    try {" +
            "      var html = marked.parse('" + escapedMarkdown + "');" +
            "      mdContainer.innerHTML = html;" +
            "    } catch (e) {" +
            "      console.error('Markdown parse error:', e);" +
            // Fallback already set via renderPartialMarkdown
            "    }" +
            // Apply Prism highlighting if available
            "    if (window.Prism) {" +
            "      mdContainer.querySelectorAll('pre code').forEach(function(block) {" +
            "        Prism.highlightElement(block);" +
            "      });" +
            "    }" +
            "  };" +
            // Use setTimeout to ensure ZK has processed the DOM update first
            "  setTimeout(renderMD, 50);" +
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
            try {
                result = MarkdownTableRenderer.renderTables(cleaned);
            } finally {
                MarkdownTableRenderer.clearLocale();
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
