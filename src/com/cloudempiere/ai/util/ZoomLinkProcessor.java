package com.cloudempiere.ai.util;

import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.Util;

/**
 * Utility class for processing zoom links in AI responses
 *
 * <p>Converts special zoom link syntax into clickable HTML links that trigger
 * iDempiere's zoom functionality to open specific records in windows.
 *
 * <p><b>Zoom Link Syntax:</b>
 * <pre>
 * [[TableName:RecordID|Display Text]]
 * </pre>
 *
 * <p><b>Examples:</b>
 * <ul>
 *   <li>[[C_BPartner:1000001|Acme Corporation]]</li>
 *   <li>[[C_Order:1000523|Sales Order SO-1000523]]</li>
 *   <li>[[M_Product:1000100|Widget A]]</li>
 * </ul>
 *
 * <p>The processor:
 * <ol>
 *   <li>Parses the zoom link syntax using regex</li>
 *   <li>Validates table name exists in AD_Table</li>
 *   <li>Converts table name to AD_Table_ID</li>
 *   <li>Generates HTML anchor with onclick handler</li>
 * </ol>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class ZoomLinkProcessor {

	private static final CLogger log = CLogger.getCLogger(ZoomLinkProcessor.class);

	/**
	 * Regex pattern for zoom links: [[TableName:RecordID|Display Text]]
	 *
	 * <p>Also matches and strips optional markdown formatting (bold/italic):
	 * <ul>
	 *   <li>[[Table:ID|Text]] - plain</li>
	 *   <li>**[[Table:ID|Text]]** - bold (stripped)</li>
	 *   <li>*[[Table:ID|Text]]* - italic (stripped)</li>
	 *   <li>***[[Table:ID|Text]]*** - bold+italic (stripped)</li>
	 * </ul>
	 *
	 * <p><b>Escaped Pipe Support:</b>
	 * In markdown tables, pipes may be escaped to prevent interpretation as cell separators.
	 * Pattern matches both `|` and `\|` (backslash-escaped pipe).
	 *
	 * Groups:
	 * - Group 1: TableName (e.g., C_BPartner)
	 * - Group 2: RecordID (e.g., 1000001)
	 * - Group 3: Display Text (e.g., Acme Corporation)
	 */
	private static final Pattern ZOOM_LINK_PATTERN = Pattern.compile(
		"\\*{0,3}\\[\\[([A-Za-z_][A-Za-z0-9_]*):(\\d+)\\\\?\\|([^\\]]+)\\]\\]\\*{0,3}"
	);

	/**
	 * Process AI response text and convert zoom link syntax to clickable HTML links
	 *
	 * @param text AI response text that may contain zoom links
	 * @param ctx context for database access
	 * @param widgetId ZK widget ID for event targeting
	 * @return processed text with HTML zoom links
	 */
	public static String processZoomLinks(String text, Properties ctx, String widgetId) {
		if (text == null || text.isEmpty()) {
			return text;
		}

		// Debug: Log the text being processed
		log.warning("[ZOOM-PROCESSOR] Processing text: " + text.substring(0, Math.min(100, text.length())));
		log.warning("[ZOOM-PROCESSOR] Pattern: " + ZOOM_LINK_PATTERN.pattern());

		StringBuffer result = new StringBuffer();
		Matcher matcher = ZOOM_LINK_PATTERN.matcher(text);

		int matchCount = 0;
		while (matcher.find()) {
			matchCount++;
			log.warning("[ZOOM-PROCESSOR] Match #" + matchCount + " found: " + matcher.group(0));
			String tableName = matcher.group(1);
			String recordIdStr = matcher.group(2);
			String displayText = matcher.group(3);

			try {
				int recordId = Integer.parseInt(recordIdStr);

				// Get AD_Table_ID from table name
				int tableId = MTable.getTable_ID(tableName, null);

				if (tableId <= 0) {
					// Table not found - render as plain text
					log.warning("Zoom link references unknown table: " + tableName);
					matcher.appendReplacement(result, Matcher.quoteReplacement(displayText));
					continue;
				}

				// Generate HTML zoom link
				String zoomLink = generateZoomLinkHtml(tableId, recordId, displayText, widgetId);
				matcher.appendReplacement(result, Matcher.quoteReplacement(zoomLink));

			} catch (NumberFormatException e) {
				// Invalid record ID - render as plain text
				log.log(Level.WARNING, "Invalid record ID in zoom link: " + recordIdStr, e);
				matcher.appendReplacement(result, Matcher.quoteReplacement(displayText));
			}
		}

		matcher.appendTail(result);

		if (matchCount == 0) {
			log.warning("[ZOOM-PROCESSOR] No zoom links found in text");
		} else {
			log.warning("[ZOOM-PROCESSOR] Processed " + matchCount + " zoom link(s)");
		}

		return result.toString();
	}

	/**
	 * Generate HTML for a clickable zoom link
	 *
	 * <p>Creates an anchor tag with:
	 * <ul>
	 *   <li>Styled as a link (blue, underlined, pointer cursor)</li>
	 *   <li>onclick handler that fires ZK onZoom event using iDempiere pattern</li>
	 *   <li>Data attributes for table ID and record ID</li>
	 *   <li>Escaped display text for security</li>
	 * </ul>
	 *
	 * <p>Uses the standard iDempiere zoom pattern from report.js:
	 * {@code zk.Widget.$(componentId) + zAu.send(new zk.Event(...))}
	 *
	 * @param tableId AD_Table_ID
	 * @param recordId Record_ID
	 * @param displayText text to display as link
	 * @param widgetId ZK widget UUID to target for event
	 * @return HTML anchor element
	 */
	private static String generateZoomLinkHtml(int tableId, int recordId, String displayText, String widgetId) {
		// Escape display text for HTML security
		String escapedText = Util.maskHTML(displayText, true);

		// Get table name for column name construction
		MTable table = MTable.get(tableId);
		String tableName = table != null ? table.getTableName() : "Record";
		String columnName = tableName + "_ID";

		StringBuilder html = new StringBuilder();
		html.append("<a href=\"javascript:void(0)\" ");
		html.append("class=\"ai-zoom-link\" ");
		html.append("data-table-id=\"").append(tableId).append("\" ");
		html.append("data-record-id=\"").append(recordId).append("\" ");
		html.append("onclick=\"");

		// Use iDempiere standard zoom pattern (same as report.js):
		// 1. Get widget by UUID using zk.Widget.$()
		// 2. Create event with data array [columnName, recordId]
		// 3. Send via zAu.send() for proper server-side processing
		//
		// Note: Html component content runs in iframe/isolated context, so we need to:
		// - Try window.zk first (same window)
		// - Fall back to parent.zk (parent frame)
		// - Use self-contained function to avoid scope issues
		html.append("(function(){");
		html.append("try{");
		html.append("var zkObj=window.zk||parent.zk;");
		html.append("var auObj=window.zAu||parent.zAu;");
		html.append("if(!zkObj){console.error('[Zoom] ZK not found');return;}");
		html.append("if(!auObj){console.error('[Zoom] zAu not found');return;}");
		// Find widget dynamically (handles both new and re-rendered messages)
		// Strategy 1: Try by hardcoded ID (works for new messages)
		html.append("var w=zkObj.Widget.$('").append(widgetId).append("');");
		html.append("if(!w){");
		// Strategy 2: Search document for ai-chat-widget class
		html.append("console.log('[Zoom] ID not found, searching by class');");
		html.append("var chatElems=document.querySelectorAll('.ai-chat-widget');");
		html.append("if(!chatElems||chatElems.length===0){");
		html.append("chatElems=(parent&&parent.document)?parent.document.querySelectorAll('.ai-chat-widget'):[];");
		html.append("}");
		html.append("console.log('[Zoom] Found '+chatElems.length+' chat widgets');");
		html.append("for(var i=0;i<chatElems.length&&!w;i++){");
		html.append("var candidate=zkObj.Widget.$(chatElems[i]);");
		html.append("if(candidate){");
		html.append("w=candidate.length?candidate[0]:candidate;");
		html.append("console.log('[Zoom] Using widget: '+w.uuid);");
		html.append("}");
		html.append("}");
		html.append("}");
		html.append("if(!w){console.error('[Zoom] Widget not found');return;}");
		html.append("var evt=new zkObj.Event(w,'onZoom',");
		html.append("{data:['").append(columnName).append("','").append(recordId).append("']},");
		html.append("{toServer:true});");
		html.append("auObj.send(evt);");
		html.append("console.log('[Zoom] Event sent');");
		html.append("}catch(e){console.error('[Zoom] Error:',e);}");
		html.append("})();");
		html.append("return false;\" ");

		// Styling - blue link with underline
		html.append("style=\"color: #1976D2; text-decoration: underline; cursor: pointer;\">");
		html.append(escapedText);
		html.append("</a>");

		return html.toString();
	}

	/**
	 * Check if text contains any zoom links
	 *
	 * @param text text to check
	 * @return true if text contains zoom link syntax
	 */
	public static boolean containsZoomLinks(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		return ZOOM_LINK_PATTERN.matcher(text).find();
	}

	/**
	 * Extract all zoom links from text for validation/preview
	 *
	 * @param text text to parse
	 * @return array of ZoomLinkInfo objects
	 */
	public static ZoomLinkInfo[] extractZoomLinks(String text) {
		if (text == null || text.isEmpty()) {
			return new ZoomLinkInfo[0];
		}

		java.util.List<ZoomLinkInfo> links = new java.util.ArrayList<>();
		Matcher matcher = ZOOM_LINK_PATTERN.matcher(text);

		while (matcher.find()) {
			String tableName = matcher.group(1);
			String recordIdStr = matcher.group(2);
			String displayText = matcher.group(3);

			try {
				int recordId = Integer.parseInt(recordIdStr);
				links.add(new ZoomLinkInfo(tableName, recordId, displayText));
			} catch (NumberFormatException e) {
				log.log(Level.FINE, "Skipping invalid zoom link: " + matcher.group(0), e);
			}
		}

		return links.toArray(new ZoomLinkInfo[0]);
	}

	/**
	 * Information about a zoom link extracted from text
	 */
	public static class ZoomLinkInfo {
		private final String tableName;
		private final int recordId;
		private final String displayText;

		public ZoomLinkInfo(String tableName, int recordId, String displayText) {
			this.tableName = tableName;
			this.recordId = recordId;
			this.displayText = displayText;
		}

		public String getTableName() {
			return tableName;
		}

		public int getRecordId() {
			return recordId;
		}

		public String getDisplayText() {
			return displayText;
		}

		@Override
		public String toString() {
			return "ZoomLink[" + tableName + ":" + recordId + "|" + displayText + "]";
		}
	}
}