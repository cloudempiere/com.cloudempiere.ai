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
package com.cloudempiere.ai.util;

import java.util.Locale;
import java.util.Properties;

import org.compiere.util.CLogger;

/**
 * Unified renderer for complete AI messages.
 *
 * <p>This class provides a single source of truth for rendering AI messages
 * to HTML, used by both:
 * <ul>
 *   <li>Post-streaming finalization ({@link com.cloudempiere.ai.component.AIChatStreamingMessage})</li>
 *   <li>Message reload from database ({@link com.cloudempiere.ai.component.AIChatWidget})</li>
 * </ul>
 *
 * <p><b>Rendering Pipeline:</b>
 * <ol>
 *   <li>Normalize whitespace (3+ newlines → 2)</li>
 *   <li>Remove hallucinated function calls</li>
 *   <li>Pre-render tables with zoom links ({@link MarkdownTableRenderer})</li>
 *   <li>Process zoom links outside tables ({@link ZoomLinkProcessor})</li>
 *   <li>Parse remaining markdown ({@link CommonMarkRenderer})</li>
 * </ol>
 *
 * <p><b>Design Decision:</b> This unifies the previously duplicated rendering
 * logic that existed in three places, causing inconsistent output and bugs.
 * Streaming display still uses specialized {@link StreamingTableRenderer} for
 * incremental updates, but finalization uses this unified renderer.
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * // In AIChatStreamingMessage.complete()
 * String html = AIMessageRenderer.render(content, ctx, widgetId, locale);
 * streamingContent.setContent(html);
 *
 * // In AIChatWidget.formatMessage()
 * String html = AIMessageRenderer.render(messageText, ctx, widgetId, locale);
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 * @since CLD-1704 (Rendering unification)
 */
public class AIMessageRenderer {

	private static final CLogger log = CLogger.getCLogger(AIMessageRenderer.class);

	/**
	 * Render complete AI message markdown to HTML.
	 *
	 * <p>This is the single source of truth for AI message rendering.
	 * It handles the complete pipeline from markdown text to final HTML,
	 * ensuring consistent output regardless of where it's called from.
	 *
	 * <p><b>Thread Safety:</b> This method uses thread-local state in
	 * MarkdownTableRenderer for zoom context. Callers must ensure this
	 * is called from a UI thread or within proper ZK execution context.
	 *
	 * @param markdownText raw markdown text from AI response
	 * @param ctx iDempiere context for zoom link processing (required for clickable links)
	 * @param widgetId ZK widget UUID for zoom event targeting (required for clickable links)
	 * @param locale locale for number formatting in tables (null = system default)
	 * @return complete HTML ready for display
	 */
	public static String render(String markdownText, Properties ctx, String widgetId, Locale locale) {
		if (markdownText == null || markdownText.isEmpty()) {
			return "";
		}

		String processed = markdownText;

		// Step 1: Normalize excessive line breaks (3+ newlines → 2 newlines for proper paragraph spacing)
		// This prevents AI-generated text with extra spacing from creating huge gaps
		processed = processed.replaceAll("\n{3,}", "\n\n");

		// Step 2: Remove hallucinated function call XML blocks
		// Some models generate tool-like syntax even without tool support enabled
		processed = removeFunctionCalls(processed);

		// Step 3: Pre-render tables (with zoom links in cells)
		// Tables must be rendered BEFORE markdown parsing to preserve table structure
		if (MarkdownTableRenderer.containsTable(processed)) {
			int clientId = org.compiere.util.Env.getAD_Client_ID(ctx);
			log.warning("[RENDER-UNIFIED] Setting context on MarkdownTableRenderer | AD_Client_ID=" + clientId);

			MarkdownTableRenderer.setLocale(locale != null ? locale : Locale.getDefault());
			MarkdownTableRenderer.setContext(ctx);
			MarkdownTableRenderer.setWidgetId(widgetId);
			try {
				processed = MarkdownTableRenderer.renderTables(processed);
			} finally {
				MarkdownTableRenderer.clearLocale();
				MarkdownTableRenderer.clearZoomContext();
			}
		}

		// Step 4: Process zoom links outside tables
		// Explicit syntax: [[Table:ID|Display]] - AI is instructed to format references this way
		// Note: Links inside tables are already processed in step 3
		if (widgetId != null && ctx != null) {
			processed = ZoomLinkProcessor.processZoomLinks(processed, ctx, widgetId);

			// FUTURE (ADR-039): Pattern-based extraction for natural references like "SO-1234"
			// Currently bypassed - requires vector DB for fast lookup across 2000+ tables.
			// See RecordReferenceExtractor and ChatRecordLinkRenderer for implementation.
			// Uncomment when vector DB caching is available:
			// processed = ChatRecordLinkRenderer.extractAndRender(processed, ctx, widgetId);
		}

		// Step 5: Normalize excessive line breaks AGAIN after table/link rendering
		// Table rendering may have preserved or added extra newlines around tables
		processed = processed.replaceAll("\n{3,}", "\n\n");

		// Step 6: Ensure headings have proper line breaks before them for markdown parsing
		// Fix cases where AI doesn't put newline before heading: "text## Heading" → "text\n## Heading"
		processed = processed.replaceAll("([^\n])(\n?)(#{1,3} )", "$1\n\n$3");

		// Step 7: Parse remaining markdown while preserving HTML (tables and zoom links)
		// At this point, processed contains:
		// - Pre-rendered HTML tables (with zoom links)
		// - HTML zoom links outside tables
		// - Raw markdown for everything else (headings, bold, lists, etc.)
		//
		// We use CommonMarkRenderer which extracts HTML, parses markdown, then restores HTML
		String finalHtml = CommonMarkRenderer.render(processed);

		// WRAPPER DESIGN: This div.ai-markdown-content wrapper is required for CSS styling.
		// It's added in 3 places: (1) HERE for legacy markdown reload, (2) AIChatStreamingMessage
		// renderFinalMarkdown() for new streaming, (3) cancel() for cancelled messages.
		// Progressive renderers return unwrapped HTML fragments.
		return "<div class='ai-markdown-content'>" + finalHtml + "</div>";
	}

	/**
	 * Remove hallucinated function call XML blocks.
	 *
	 * <p>Removes function call blocks that may appear when models without
	 * tool support generate tool-like syntax.
	 *
	 * @param text text to clean
	 * @return cleaned text
	 */
	private static String removeFunctionCalls(String text) {
		String result = text;
		// Remove complete function call blocks
		result = result.replaceAll("(?s)<function_calls>.*?</function_calls>", "");
		result = result.replaceAll("(?s)<function_result>.*?</function_result>", "");
		// Remove incomplete/partial tags that may appear during streaming
		result = result.replaceAll("(?s)<function_calls>.*$", "");
		result = result.replaceAll("(?s)<function_result>.*$", "");
		result = result.replaceAll("(?s)<invoke[^>]*>.*?</invoke>", "");
		result = result.replaceAll("(?s)<parameter[^>]*>.*?</parameter>", "");
		return result;
	}
}
