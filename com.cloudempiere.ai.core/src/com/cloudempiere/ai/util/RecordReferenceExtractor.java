package com.cloudempiere.ai.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import com.cloudempiere.ai.provider.dto.RecordReference;

/**
 * Extracts record references from AI response text.
 *
 * <p>Converts natural language references (e.g., "SO-1234", "Customer Acme Corp")
 * into structured {@link RecordReference} objects that can be rendered as clickable links.
 *
 * <p><b>Supported Patterns:</b>
 * <ul>
 *   <li>Sales Orders: SO-1234, Sales Order 1234, Order #1234</li>
 *   <li>Purchase Orders: PO-1234, Purchase Order 1234</li>
 *   <li>Invoices: INV-1234, Invoice 1234, AR Invoice 1234</li>
 *   <li>Business Partners: BP-1234, Customer 1234, Vendor 1234</li>
 *   <li>Products: PROD-1234, Product 1234, SKU-ABC123</li>
 *   <li>Payments: PAY-1234, Payment 1234</li>
 *   <li>Shipments: SHIP-1234, Shipment 1234</li>
 * </ul>
 *
 * <p><b>Extraction Modes:</b>
 * <ol>
 *   <li><b>LLM Structured</b> (preferred): Parse JSON with explicit references</li>
 *   <li><b>Pattern Matching</b>: Regex-based extraction from natural text</li>
 * </ol>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * RecordReferenceExtractor extractor = new RecordReferenceExtractor(ctx);
 * List&lt;RecordReference&gt; refs = extractor.extract(aiResponseText);
 *
 * for (RecordReference ref : refs) {
 *     System.out.println(ref.getDisplayValue() + " -> " + ref.getTableName() + "#" + ref.getRecordId());
 * }
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 * @see RecordReference
 * @see ZoomLinkProcessor
 */
public class RecordReferenceExtractor {

	private static final CLogger log = CLogger.getCLogger(RecordReferenceExtractor.class);

	/**
	 * Document pattern configuration
	 */
	private static class PatternConfig {
		final Pattern pattern;
		final String tableName;
		final String documentNoColumn;
		final String whereClause;  // Additional filter (e.g., IsSOTrx='Y')

		PatternConfig(Pattern pattern, String tableName, String documentNoColumn, String whereClause) {
			this.pattern = pattern;
			this.tableName = tableName;
			this.documentNoColumn = documentNoColumn;
			this.whereClause = whereClause;
		}

	}

	/**
	 * Registered document patterns.
	 * Order matters - more specific patterns should come first.
	 */
	private static final List<PatternConfig> PATTERNS = new ArrayList<>();

	static {
		// Sales Orders: SO-1234, SO 1234, Sales Order 1234, Order #1234
		PATTERNS.add(new PatternConfig(
			Pattern.compile("\\b(?:SO|Sales Order|Order)[- #]?(\\d{4,})\\b", Pattern.CASE_INSENSITIVE),
			"C_Order", "DocumentNo", "IsSOTrx='Y'"
		));

		// Purchase Orders: PO-1234, PO 1234, Purchase Order 1234
		PATTERNS.add(new PatternConfig(
			Pattern.compile("\\b(?:PO|Purchase Order)[- #]?(\\d{4,})\\b", Pattern.CASE_INSENSITIVE),
			"C_Order", "DocumentNo", "IsSOTrx='N'"
		));

		// Invoices: INV-1234, Invoice 1234, AR Invoice, AP Invoice
		PATTERNS.add(new PatternConfig(
			Pattern.compile("\\b(?:INV|Invoice|AR Invoice|AP Invoice|Vendor Invoice|Customer Invoice)[- #]?(\\d{4,})\\b", Pattern.CASE_INSENSITIVE),
			"C_Invoice", "DocumentNo", null
		));

		// Business Partners: BP-1234, Customer-1234, Vendor-1234
		PATTERNS.add(new PatternConfig(
			Pattern.compile("\\b(?:BP|Business Partner|Customer|Vendor)[- #]?(\\d{4,})\\b", Pattern.CASE_INSENSITIVE),
			"C_BPartner", "Value", null
		));

		// Products: PROD-1234, Product 1234, SKU-ABC123
		PATTERNS.add(new PatternConfig(
			Pattern.compile("\\b(?:PROD|Product)[- #]?(\\d{4,})\\b", Pattern.CASE_INSENSITIVE),
			"M_Product", "Value", null
		));

		// SKU pattern (alphanumeric)
		PATTERNS.add(new PatternConfig(
			Pattern.compile("\\bSKU[- ]?([A-Z0-9]{3,20})\\b", Pattern.CASE_INSENSITIVE),
			"M_Product", "SKU", null
		));

		// Payments: PAY-1234, Payment 1234
		PATTERNS.add(new PatternConfig(
			Pattern.compile("\\b(?:PAY|Payment)[- #]?(\\d{4,})\\b", Pattern.CASE_INSENSITIVE),
			"C_Payment", "DocumentNo", null
		));

		// Shipments/In-Out: SHIP-1234, Shipment 1234, Delivery 1234
		PATTERNS.add(new PatternConfig(
			Pattern.compile("\\b(?:SHIP|Shipment|Delivery|Receipt)[- #]?(\\d{4,})\\b", Pattern.CASE_INSENSITIVE),
			"M_InOut", "DocumentNo", null
		));

		// Requests: REQ-1234, Request 1234, Ticket 1234
		PATTERNS.add(new PatternConfig(
			Pattern.compile("\\b(?:REQ|Request|Ticket|Case)[- #]?(\\d{4,})\\b", Pattern.CASE_INSENSITIVE),
			"R_Request", "DocumentNo", null
		));

		// Projects: PROJ-1234, Project 1234
		PATTERNS.add(new PatternConfig(
			Pattern.compile("\\b(?:PROJ|Project)[- #]?(\\d{4,})\\b", Pattern.CASE_INSENSITIVE),
			"C_Project", "Value", null
		));
	}

	/** Context for database queries */
	private final Properties ctx;

	/** Cache for document number lookups (documentNo -> recordId) */
	private final Map<String, Integer> lookupCache = new HashMap<>();

	/**
	 * Create extractor with context
	 * @param ctx iDempiere context for database access
	 */
	public RecordReferenceExtractor(Properties ctx) {
		this.ctx = ctx != null ? ctx : Env.getCtx();
	}

	/**
	 * Create extractor using Env.getCtx()
	 */
	public RecordReferenceExtractor() {
		this(Env.getCtx());
	}

	/**
	 * Table column to table mapping for markdown table parsing.
	 * Maps column header names to iDempiere table configurations.
	 */
	private static final Map<String, TableColumnConfig> TABLE_COLUMN_MAPPINGS = new LinkedHashMap<>();

	static {
		// Order columns -> C_Order
		TABLE_COLUMN_MAPPINGS.put("order", new TableColumnConfig("C_Order", "DocumentNo", null));
		TABLE_COLUMN_MAPPINGS.put("order #", new TableColumnConfig("C_Order", "DocumentNo", null));
		TABLE_COLUMN_MAPPINGS.put("order#", new TableColumnConfig("C_Order", "DocumentNo", null));
		TABLE_COLUMN_MAPPINGS.put("order no", new TableColumnConfig("C_Order", "DocumentNo", null));
		TABLE_COLUMN_MAPPINGS.put("orderno", new TableColumnConfig("C_Order", "DocumentNo", null));
		TABLE_COLUMN_MAPPINGS.put("sales order", new TableColumnConfig("C_Order", "DocumentNo", "IsSOTrx='Y'"));
		TABLE_COLUMN_MAPPINGS.put("purchase order", new TableColumnConfig("C_Order", "DocumentNo", "IsSOTrx='N'"));

		// Invoice columns -> C_Invoice
		TABLE_COLUMN_MAPPINGS.put("invoice", new TableColumnConfig("C_Invoice", "DocumentNo", null));
		TABLE_COLUMN_MAPPINGS.put("invoice #", new TableColumnConfig("C_Invoice", "DocumentNo", null));
		TABLE_COLUMN_MAPPINGS.put("invoice no", new TableColumnConfig("C_Invoice", "DocumentNo", null));

		// Payment columns -> C_Payment
		TABLE_COLUMN_MAPPINGS.put("payment", new TableColumnConfig("C_Payment", "DocumentNo", null));
		TABLE_COLUMN_MAPPINGS.put("payment #", new TableColumnConfig("C_Payment", "DocumentNo", null));

		// Shipment columns -> M_InOut
		TABLE_COLUMN_MAPPINGS.put("shipment", new TableColumnConfig("M_InOut", "DocumentNo", null));
		TABLE_COLUMN_MAPPINGS.put("delivery", new TableColumnConfig("M_InOut", "DocumentNo", null));

		// Product columns -> M_Product
		TABLE_COLUMN_MAPPINGS.put("product", new TableColumnConfig("M_Product", "Value", null));
		TABLE_COLUMN_MAPPINGS.put("sku", new TableColumnConfig("M_Product", "SKU", null));
	}

	/**
	 * Configuration for table column to iDempiere table mapping.
	 */
	private static class TableColumnConfig {
		final String tableName;
		final String documentNoColumn;
		final String whereClause;

		TableColumnConfig(String tableName, String documentNoColumn, String whereClause) {
			this.tableName = tableName;
			this.documentNoColumn = documentNoColumn;
			this.whereClause = whereClause;
		}
	}

	/**
	 * Extract all record references from text.
	 *
	 * <p>Uses pattern matching to find document references and looks up
	 * record IDs from the database. Also parses markdown tables to extract
	 * references from columns like "Order #".
	 *
	 * @param text AI response text
	 * @return list of RecordReference objects sorted by position
	 */
	public List<RecordReference> extract(String text) {
		if (text == null || text.isEmpty()) {
			return new ArrayList<>();
		}

		// Use LinkedHashMap to preserve insertion order and dedupe by position
		Map<String, RecordReference> foundRefs = new LinkedHashMap<>();

		// Step 1: Extract from markdown tables first (higher priority)
		extractFromMarkdownTables(text, foundRefs);

		// Step 2: Extract from natural language patterns
		for (PatternConfig config : PATTERNS) {
			Matcher matcher = config.pattern.matcher(text);

			while (matcher.find()) {
				String documentNo = matcher.group(1);
				String matchedText = matcher.group(0);
				int startIdx = matcher.start();
				int endIdx = matcher.end();

				// Create unique key based on position to avoid duplicates
				String posKey = startIdx + "-" + endIdx;
				if (foundRefs.containsKey(posKey)) {
					continue;  // Skip if already found at this position
				}

				try {
					// Look up record ID from document number
					int recordId = lookupRecordId(config.tableName, config.documentNoColumn,
												  documentNo, config.whereClause);

					if (recordId > 0) {
						RecordReference ref = RecordReference.builder()
							.tableName(config.tableName)
							.recordId(recordId)
							.displayValue(matchedText)
							.columnName(getKeyColumnName(config.tableName))
							.startIndex(startIdx)
							.endIndex(endIdx)
							.addMetadata("documentNo", documentNo)
							.addMetadata("pattern", config.pattern.pattern())
							.build();

						foundRefs.put(posKey, ref);

						if (log.isLoggable(Level.FINE)) {
							log.fine("Extracted reference: " + matchedText + " -> " +
									 config.tableName + "#" + recordId);
						}
					} else {
						if (log.isLoggable(Level.FINE)) {
							log.fine("No record found for: " + matchedText +
									 " (" + config.tableName + "." + config.documentNoColumn +
									 "=" + documentNo + ")");
						}
					}
				} catch (Exception e) {
					log.log(Level.WARNING, "Error looking up record for " + matchedText, e);
				}
			}
		}

		// Sort by start position
		List<RecordReference> result = new ArrayList<>(foundRefs.values());
		result.sort(Comparator.comparingInt(RecordReference::getStartIndex));

		return result;
	}

	/**
	 * Extract references from markdown tables by parsing column headers.
	 *
	 * <p>Detects columns like "Order #", "Invoice", etc. and extracts
	 * document numbers from corresponding cells.
	 *
	 * @param text full text containing potential markdown tables
	 * @param foundRefs map to add found references to
	 */
	private void extractFromMarkdownTables(String text, Map<String, RecordReference> foundRefs) {
		// Match markdown table pattern: | header | header | ... followed by | --- | --- | ...
		// Then data rows: | value | value | ...
		Pattern tablePattern = Pattern.compile(
			"\\|([^|\\n]+(?:\\|[^|\\n]+)+)\\|\\s*\\n" +  // Header row
			"\\|[-:\\s|]+\\|\\s*\\n" +                    // Separator row (with alignment colons)
			"((?:\\|[^\\n]+\\|\\s*\\n?)+)",              // Data rows
			Pattern.MULTILINE
		);

		Matcher tableMatcher = tablePattern.matcher(text);
		while (tableMatcher.find()) {
			String headerRow = tableMatcher.group(1);
			String dataRows = tableMatcher.group(2);
			int tableStart = tableMatcher.start();

			// Parse headers to find document columns
			String[] headers = headerRow.split("\\|");
			Map<Integer, TableColumnConfig> columnConfigs = new LinkedHashMap<>();

			for (int i = 0; i < headers.length; i++) {
				String header = headers[i].trim().toLowerCase();
				TableColumnConfig config = TABLE_COLUMN_MAPPINGS.get(header);
				if (config != null) {
					columnConfigs.put(i, config);
					if (log.isLoggable(Level.FINE)) {
						log.fine("Found document column: " + header + " -> " + config.tableName);
					}
				}
			}

			if (columnConfigs.isEmpty()) {
				continue;  // No document columns in this table
			}

			// Parse data rows
			String[] rows = dataRows.split("\\n");
			for (String row : rows) {
				if (row.trim().isEmpty()) continue;

				String[] cells = row.split("\\|");
				for (Map.Entry<Integer, TableColumnConfig> entry : columnConfigs.entrySet()) {
					int colIndex = entry.getKey();
					TableColumnConfig config = entry.getValue();

					if (colIndex < cells.length) {
						String cellValue = cells[colIndex].trim();

						// Skip empty cells or cells that are already links
						if (cellValue.isEmpty() || cellValue.startsWith("[") || cellValue.startsWith("<")) {
							continue;
						}

						// Extract numeric document number from cell
						// Handle cases like "20001265" or "[20001265]" or markdown links
						String documentNo = cellValue.replaceAll("[\\[\\]()]", "").trim();

						// Only process if it looks like a document number (numeric or alphanumeric)
						if (!documentNo.matches("[A-Za-z0-9-]+")) {
							continue;
						}

						try {
							int recordId = lookupRecordId(config.tableName, config.documentNoColumn,
														  documentNo, config.whereClause);

							if (recordId > 0) {
								// Find position of this cell value in the original text
								int cellStart = text.indexOf(cellValue, tableStart);
								if (cellStart >= 0) {
									int cellEnd = cellStart + cellValue.length();
									String posKey = cellStart + "-" + cellEnd;

									if (!foundRefs.containsKey(posKey)) {
										RecordReference ref = RecordReference.builder()
											.tableName(config.tableName)
											.recordId(recordId)
											.displayValue(cellValue)
											.columnName(getKeyColumnName(config.tableName))
											.startIndex(cellStart)
											.endIndex(cellEnd)
											.addMetadata("documentNo", documentNo)
											.addMetadata("source", "markdown_table")
											.build();

										foundRefs.put(posKey, ref);

										if (log.isLoggable(Level.FINE)) {
											log.fine("Extracted table reference: " + cellValue + " -> " +
													 config.tableName + "#" + recordId);
										}
									}
								}
							}
						} catch (Exception e) {
							log.log(Level.FINE, "Error looking up table cell: " + cellValue, e);
						}
					}
				}
			}
		}
	}

	/**
	 * Look up record ID from document number using iDempiere Query API.
	 *
	 * <p>Uses standard iDempiere Query with:
	 * <ul>
	 *   <li>setClient_ID() - enforces tenant isolation</li>
	 *   <li>setOnlyActiveRecords() - only active records</li>
	 *   <li>firstIdOnly() - returns key column value</li>
	 * </ul>
	 *
	 * @param tableName table to query (e.g., "C_Order")
	 * @param documentNoColumn column containing document number (e.g., "DocumentNo", "Value")
	 * @param documentNo document number value
	 * @param whereClause additional filter (e.g., "IsSOTrx='Y'")
	 * @return Record_ID (key column value) or 0 if not found
	 */
	private int lookupRecordId(String tableName, String documentNoColumn,
							   String documentNo, String whereClause) {
		// Build cache key
		String cacheKey = tableName + "." + documentNoColumn + "=" + documentNo +
						  (whereClause != null ? ":" + whereClause : "");

		// Check cache
		Integer cached = lookupCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		// Build query using standard iDempiere Query API
		StringBuilder where = new StringBuilder();
		where.append(documentNoColumn).append("=?");
		if (whereClause != null && !whereClause.isEmpty()) {
			where.append(" AND ").append(whereClause);
		}

		// Query.firstIdOnly() automatically returns the key column (e.g., C_Order_ID)
		int recordId = new Query(ctx, tableName, where.toString(), null)
			.setParameters(documentNo)
			.setClient_ID()      // Tenant isolation via AD_Client_ID
			.setOnlyActiveRecords(true)
			.firstIdOnly();      // Returns key column value

		// Cache result (including 0 for not found)
		lookupCache.put(cacheKey, recordId);

		return recordId;
	}

	/**
	 * Get the key column name for a table using iDempiere MTable API.
	 *
	 * @param tableName table name (e.g., "C_Order")
	 * @return key column name (e.g., "C_Order_ID") or tableName + "_ID" as fallback
	 */
	private String getKeyColumnName(String tableName) {
		try {
			MTable table = MTable.get(ctx, tableName);
			if (table != null) {
				String[] keyColumns = table.getKeyColumns();
				if (keyColumns != null && keyColumns.length > 0) {
					return keyColumns[0];
				}
			}
		} catch (Exception e) {
			log.log(Level.FINE, "Could not get key column for " + tableName, e);
		}
		// Fallback to standard convention
		return tableName + "_ID";
	}

	/**
	 * Check if text contains any recognizable record references.
	 *
	 * @param text text to check
	 * @return true if text contains potential record references
	 */
	public static boolean containsReferences(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}

		// Check natural language patterns
		for (PatternConfig config : PATTERNS) {
			if (config.pattern.matcher(text).find()) {
				return true;
			}
		}

		// Check for markdown tables with document columns
		if (containsDocumentTable(text)) {
			return true;
		}

		return false;
	}

	/**
	 * Check if text contains a markdown table with document-related columns.
	 *
	 * @param text text to check
	 * @return true if table with Order/Invoice/etc. column found
	 */
	private static boolean containsDocumentTable(String text) {
		// Quick check for table structure
		if (!text.contains("|")) {
			return false;
		}

		// Look for column headers that map to documents
		String lowerText = text.toLowerCase();
		for (String columnName : TABLE_COLUMN_MAPPINGS.keySet()) {
			// Check if column header appears in a table context (between pipes)
			if (lowerText.contains("| " + columnName + " |") ||
				lowerText.contains("|" + columnName + "|") ||
				lowerText.contains("| " + columnName + "|") ||
				lowerText.contains("|" + columnName + " |")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get all registered table patterns.
	 * Useful for system prompt instructions to guide LLM output format.
	 *
	 * @return map of pattern description to table name
	 */
	public static Map<String, String> getPatternDescriptions() {
		Map<String, String> descriptions = new LinkedHashMap<>();
		descriptions.put("SO-1234, Sales Order 1234", "C_Order (Sales)");
		descriptions.put("PO-1234, Purchase Order 1234", "C_Order (Purchase)");
		descriptions.put("INV-1234, Invoice 1234", "C_Invoice");
		descriptions.put("BP-1234, Customer 1234, Vendor 1234", "C_BPartner");
		descriptions.put("PROD-1234, Product 1234, SKU-ABC123", "M_Product");
		descriptions.put("PAY-1234, Payment 1234", "C_Payment");
		descriptions.put("SHIP-1234, Shipment 1234", "M_InOut");
		descriptions.put("REQ-1234, Request 1234, Ticket 1234", "R_Request");
		descriptions.put("PROJ-1234, Project 1234", "C_Project");
		return descriptions;
	}

	/**
	 * Clear the lookup cache.
	 * Call this if you need to refresh cached lookups.
	 */
	public void clearCache() {
		lookupCache.clear();
	}
}
