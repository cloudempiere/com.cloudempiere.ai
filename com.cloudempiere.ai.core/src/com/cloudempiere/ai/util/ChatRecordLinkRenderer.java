package com.cloudempiere.ai.util;

import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.cloudempiere.ai.provider.dto.RecordReference;

/**
 * Renders record references as clickable HTML links in AI chat responses.
 *
 * <p>This renderer converts {@link RecordReference} objects into HTML anchor elements
 * that trigger iDempiere's zoom functionality when clicked.
 *
 * <p><b>Features:</b>
 * <ul>
 *   <li>Converts extracted record references to clickable links</li>
 *   <li>Respects role-based access control (hides inaccessible records)</li>
 *   <li>Generates ZK event handlers for zoom navigation</li>
 *   <li>HTML-escapes display text for security</li>
 *   <li>Preserves original text structure</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * String aiResponse = "Found order SO-1234 and invoice INV-5678.";
 *
 * RecordReferenceExtractor extractor = new RecordReferenceExtractor(ctx);
 * List&lt;RecordReference&gt; refs = extractor.extract(aiResponse);
 *
 * String htmlWithLinks = ChatRecordLinkRenderer.renderLinks(
 *     aiResponse, refs, ctx, widgetId);
 * </pre>
 *
 * <p><b>Integration with AIChatWidget:</b>
 * <pre>
 * // In message formatting pipeline (after ZoomLinkProcessor)
 * String processed = ZoomLinkProcessor.processZoomLinks(text, ctx, widgetId);
 * List&lt;RecordReference&gt; refs = extractor.extract(processed);
 * String withLinks = ChatRecordLinkRenderer.renderLinks(processed, refs, ctx, widgetId);
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 * @see RecordReference
 * @see RecordReferenceExtractor
 * @see ZoomLinkProcessor
 */
public class ChatRecordLinkRenderer {

	private static final CLogger log = CLogger.getCLogger(ChatRecordLinkRenderer.class);

	/** CSS class for record links */
	public static final String CSS_CLASS = "ai-record-link";

	/** Link color (Material Design Blue 700) */
	public static final String LINK_COLOR = "#1976D2";

	/**
	 * Render text with record references as clickable links.
	 *
	 * <p>Replaces matched text regions with HTML anchor elements that
	 * fire ZK zoom events when clicked.
	 *
	 * @param text original text containing references
	 * @param references list of extracted references with positions
	 * @param ctx iDempiere context for access control
	 * @param widgetId ZK widget ID for event targeting
	 * @return text with HTML links replacing references
	 */
	public static String renderLinks(String text, List<RecordReference> references,
									 Properties ctx, String widgetId) {
		if (text == null || text.isEmpty()) {
			return text;
		}

		if (references == null || references.isEmpty()) {
			return text;
		}

		// Get current role for access check
		int roleId = Env.getAD_Role_ID(ctx);

		// Sort references by start position (should already be sorted, but ensure)
		references.sort(Comparator.comparingInt(RecordReference::getStartIndex));

		StringBuilder result = new StringBuilder();
		int lastEnd = 0;

		for (RecordReference ref : references) {
			// Validate position indices
			if (ref.getStartIndex() < 0 || ref.getEndIndex() < 0 ||
				ref.getStartIndex() >= ref.getEndIndex() ||
				ref.getEndIndex() > text.length()) {
				log.warning("Invalid reference position: " + ref);
				continue;
			}

			// Skip if overlaps with previous (shouldn't happen if properly extracted)
			if (ref.getStartIndex() < lastEnd) {
				log.fine("Skipping overlapping reference: " + ref);
				continue;
			}

			// Check access - skip if user can't access this table
			if (!ref.isAccessible(roleId)) {
				if (log.isLoggable(Level.FINE)) {
					log.fine("User has no access to " + ref.getTableName() + ", skipping link");
				}
				// Keep original text, don't make it a link
				continue;
			}

			// Append text before this reference
			if (ref.getStartIndex() > lastEnd) {
				result.append(text, lastEnd, ref.getStartIndex());
			}

			// Append clickable link
			result.append(generateLinkHtml(ref, widgetId));

			lastEnd = ref.getEndIndex();
		}

		// Append remaining text after last reference
		if (lastEnd < text.length()) {
			result.append(text.substring(lastEnd));
		}

		return result.toString();
	}

	/**
	 * Generate HTML for a single record link.
	 *
	 * <p>Creates an anchor element with:
	 * <ul>
	 *   <li>CSS class for styling</li>
	 *   <li>Data attributes for table and record IDs</li>
	 *   <li>onclick handler firing ZK onZoom event using iDempiere standard pattern</li>
	 *   <li>Tooltip showing table name</li>
	 * </ul>
	 *
	 * <p>Uses the standard iDempiere zoom pattern from report.js:
	 * <pre>
	 * zk.Widget.$(componentId) + zAu.send(new zk.Event(...))
	 * </pre>
	 *
	 * @param ref record reference
	 * @param widgetId ZK widget ID (UUID) for event targeting
	 * @return HTML anchor element
	 */
	public static String generateLinkHtml(RecordReference ref, String widgetId) {
		int tableId = ref.getTableId();
		int recordId = ref.getRecordId();
		String columnName = ref.getColumnName();  // e.g., C_Order_ID
		String displayText = ref.getDisplayValue();

		// Escape display text for HTML security
		String escapedText = Util.maskHTML(displayText, true);

		StringBuilder html = new StringBuilder();
		html.append("<a href=\"javascript:void(0)\" ");
		html.append("class=\"").append(CSS_CLASS).append("\" ");
		html.append("data-table-id=\"").append(tableId).append("\" ");
		html.append("data-table-name=\"").append(ref.getTableName()).append("\" ");
		html.append("data-record-id=\"").append(recordId).append("\" ");
		html.append("data-column-name=\"").append(columnName).append("\" ");
		html.append("title=\"Open ").append(ref.getTableName()).append(" record\" ");

		// Use iDempiere standard zoom pattern (same as report.js):
		// 1. Get widget by UUID using zk.Widget.$()
		// 2. Create event with data array [columnName, recordId]
		// 3. Send via zAu.send() for proper server-side processing
		//
		// Note: Html component content runs in iframe/isolated context, so we need to:
		// - Try window.zk first (same window)
		// - Fall back to parent.zk (parent frame)
		// - Use self-contained function to avoid scope issues
		html.append("onclick=\"");
		html.append("(function(){");
		html.append("var zkObj=window.zk||parent.zk;");
		html.append("var auObj=window.zAu||parent.zAu;");
		html.append("if(!zkObj||!auObj){console.error('ZK not found');return false;}");
		html.append("var w=zkObj.Widget.$('").append(widgetId).append("');");
		html.append("if(w){");
		html.append("var evt=new zkObj.Event(w,'onZoom',");
		html.append("{data:['").append(columnName).append("','").append(recordId).append("']},");
		html.append("{toServer:true});");
		html.append("auObj.send(evt);");
		html.append("}else{console.error('Widget not found: ").append(widgetId).append("');}");
		html.append("})();");
		html.append("return false;\" ");

		// Styling - consistent with ZoomLinkProcessor
		html.append("style=\"color: ").append(LINK_COLOR).append("; ");
		html.append("text-decoration: underline; cursor: pointer;\">");
		html.append(escapedText);
		html.append("</a>");

		return html.toString();
	}

	/**
	 * Combined extraction and rendering in one call.
	 *
	 * <p>Convenience method that extracts references and renders them as links.
	 *
	 * @param text original text
	 * @param ctx iDempiere context
	 * @param widgetId ZK widget ID
	 * @return text with clickable record links
	 */
	public static String extractAndRender(String text, Properties ctx, String widgetId) {
		if (text == null || text.isEmpty()) {
			return text;
		}

		// Quick check - avoid extraction if no patterns match
		if (!RecordReferenceExtractor.containsReferences(text)) {
			return text;
		}

		RecordReferenceExtractor extractor = new RecordReferenceExtractor(ctx);
		List<RecordReference> refs = extractor.extract(text);

		if (refs.isEmpty()) {
			return text;
		}

		return renderLinks(text, refs, ctx, widgetId);
	}

	/**
	 * Check if text would benefit from record link processing.
	 *
	 * @param text text to check
	 * @return true if text contains potential record references
	 */
	public static boolean hasRecordReferences(String text) {
		return RecordReferenceExtractor.containsReferences(text);
	}

	/**
	 * Get CSS styles for record links (for inclusion in page/component).
	 *
	 * @return CSS style block
	 */
	public static String getCssStyles() {
		StringBuilder css = new StringBuilder();
		css.append("<style>\n");
		css.append(".").append(CSS_CLASS).append(" {\n");
		css.append("  color: ").append(LINK_COLOR).append(";\n");
		css.append("  text-decoration: underline;\n");
		css.append("  cursor: pointer;\n");
		css.append("  transition: background-color 0.2s;\n");
		css.append("}\n");
		css.append(".").append(CSS_CLASS).append(":hover {\n");
		css.append("  background-color: #E3F2FD;\n");  // Light blue background on hover
		css.append("  border-radius: 2px;\n");
		css.append("}\n");
		css.append("</style>\n");
		return css.toString();
	}
}
