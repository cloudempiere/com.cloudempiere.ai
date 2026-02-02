package com.cloudempiere.ai.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.Env;

/**
 * Extracts context from iDempiere windows for AI agents.
 *
 * <p>Provides structured context information about:
 * <ul>
 *   <li>Current window and tab</li>
 *   <li>Active record ID and data</li>
 *   <li>Key field values</li>
 *   <li>User and client information</li>
 * </ul>
 *
 * <p>This context enables AI agents to provide record-specific insights
 * without requiring users to manually specify IDs or details.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>
 * // User is viewing Sales Order #12345
 * Map&lt;String, Object&gt; context = WindowContextExtractor.extract(gridTab);
 * // context contains: {tableName: "C_Order", recordId: 12345, docNo: "SO-12345", ...}
 * </pre>
 *
 * <p><b>Related ADRs:</b></p>
 * <ul>
 *   <li><a href="../../docs/adr/009-domain-boundaries-agent-scope.md">ADR-009: Domain Boundaries</a></li>
 *   <li><a href="../../docs/adr/015-conversational-ux-patterns.md">ADR-015: Conversational UX</a></li>
 * </ul>
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 */
public class WindowContextExtractor {

    /**
     * Extract context from a GridTab.
     *
     * @param gridTab grid tab to extract from
     * @return context map
     */
    public static Map<String, Object> extract(GridTab gridTab) {
        Map<String, Object> context = new HashMap<>();

        if (gridTab == null) {
            return context;
        }

        // Table information
        context.put("tableName", gridTab.getTableName());
        context.put("tabName", gridTab.getName());
        context.put("windowId", gridTab.getAD_Window_ID());

        // Record ID
        int recordId = gridTab.getRecord_ID();
        context.put("recordId", recordId);

        if (recordId > 0) {
            // Extract key fields
            extractKeyFields(gridTab, context);
        }

        // User and client context
        Properties ctx = gridTab.getVO().ctx;
        context.put("AD_Client_ID", Env.getAD_Client_ID(ctx));
        context.put("AD_Org_ID", Env.getAD_Org_ID(ctx));
        context.put("AD_User_ID", Env.getAD_User_ID(ctx));

        return context;
    }

    /**
     * Extract key fields from the tab.
     *
     * @param gridTab grid tab
     * @param context context map to populate
     */
    private static void extractKeyFields(GridTab gridTab, Map<String, Object> context) {
        Map<String, Object> fieldValues = new HashMap<>();

        // Common key fields to extract
        String[] keyFields = {
            "DocumentNo", "Name", "Value", "Description",
            "C_BPartner_ID", "M_Product_ID", "C_Order_ID",
            "C_Invoice_ID", "M_InOut_ID", "C_Opportunity_ID",
            "R_Request_ID", "M_Warehouse_ID", "DocStatus"
        };

        for (String fieldName : keyFields) {
            GridField field = gridTab.getField(fieldName);
            if (field != null) {
                Object value = field.getValue();
                if (value != null) {
                    fieldValues.put(fieldName, value);

                    // Also get display value for lookup fields
                    Object displayValue = field.getValue();
                    if (displayValue != null && !displayValue.equals(value)) {
                        fieldValues.put(fieldName + "_Display", displayValue.toString());
                    }
                }
            }
        }

        context.put("fields", fieldValues);
    }

    /**
     * Format context as human-readable text for AI prompt.
     *
     * @param context context map
     * @return formatted text
     */
    public static String formatForAI(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n[Current Context]\n");

        // Table and record info
        if (context.containsKey("tableName")) {
            sb.append("Table: ").append(context.get("tableName")).append("\n");
        }
        if (context.containsKey("tabName")) {
            sb.append("Tab: ").append(context.get("tabName")).append("\n");
        }
        if (context.containsKey("recordId")) {
            sb.append("Record ID: ").append(context.get("recordId")).append("\n");
        }

        // Field values
        if (context.containsKey("fields")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) context.get("fields");

            if (!fields.isEmpty()) {
                sb.append("\nCurrent Record Fields:\n");
                for (Map.Entry<String, Object> entry : fields.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    // Skip display value keys (already shown with main value)
                    if (key.endsWith("_Display")) {
                        continue;
                    }

                    sb.append("- ").append(key).append(": ").append(value);

                    // Add display value if available
                    String displayKey = key + "_Display";
                    if (fields.containsKey(displayKey)) {
                        sb.append(" (").append(fields.get(displayKey)).append(")");
                    }

                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Detect domain from table name.
     *
     * <p>Maps iDempiere tables to AI agent domains for intelligent routing.</p>
     *
     * @param tableName iDempiere table name
     * @return domain name (sales, inventory, purchasing, support, kb) or null
     */
    public static String detectDomain(String tableName) {
        if (tableName == null) {
            return null;
        }

        String upper = tableName.toUpperCase();

        // Sales domain
        if (upper.startsWith("C_ORDER") || upper.startsWith("C_INVOICE")
            || upper.startsWith("C_OPPORTUNITY") || upper.contains("SALES")) {
            return "sales";
        }

        // Inventory domain
        if (upper.startsWith("M_PRODUCT") || upper.startsWith("M_STORAGE")
            || upper.startsWith("M_WAREHOUSE") || upper.startsWith("M_INOUT")
            || upper.startsWith("M_INVENTORY")) {
            return "inventory";
        }

        // Purchasing domain
        if (upper.contains("PURCHASE") || upper.startsWith("M_REQUISITION")
            || (upper.startsWith("C_ORDER") && upper.contains("VENDOR"))) {
            return "purchasing";
        }

        // Support domain
        if (upper.startsWith("R_REQUEST") || upper.contains("SUPPORT")
            || upper.contains("TICKET")) {
            return "support";
        }

        // Knowledge base domain
        if (upper.startsWith("K_ENTRY") || upper.startsWith("K_CATEGORY")
            || upper.contains("KNOWLEDGE")) {
            return "kb";
        }

        return null;
    }

    /**
     * Create context-aware query hint for user.
     *
     * <p>Suggests relevant questions based on current window context.</p>
     *
     * @param gridTab grid tab
     * @return query hint
     */
    public static String getQueryHint(GridTab gridTab) {
        if (gridTab == null) {
            return "What would you like to know?";
        }

        String tableName = gridTab.getTableName();
        String domain = detectDomain(tableName);

        if (domain == null) {
            return "What would you like to know?";
        }

        switch (domain) {
            case "sales":
                return "Ask about this order, customer history, or sales insights...";
            case "inventory":
                return "Ask about stock levels, warehouse locations, or product availability...";
            case "purchasing":
                return "Ask about this purchase order, vendor performance, or procurement insights...";
            case "support":
                return "Ask about this ticket, customer issues, or support history...";
            case "kb":
                return "Ask about this article, related topics, or documentation...";
            default:
                return "What would you like to know?";
        }
    }
}
