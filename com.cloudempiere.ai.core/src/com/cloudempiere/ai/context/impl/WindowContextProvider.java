/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.context.impl;

import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.context.ContextParameters;
import com.cloudempiere.ai.context.IAIContextProvider;

/**
 * Context provider for iDempiere windows/tabs
 *
 * <p>Extracts context about the current window the user is viewing,
 * including record data, field definitions, and window metadata.
 *
 * <p>This context enables AI to:
 * <ul>
 *   <li>Help users understand the current record</li>
 *   <li>Suggest field values based on context</li>
 *   <li>Validate data entry</li>
 *   <li>Provide contextual help</li>
 *   <li>Explain business rules</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class WindowContextProvider implements IAIContextProvider {

    private static final CLogger log = CLogger.getCLogger(WindowContextProvider.class);

    /**
     * Technical/system field patterns
     * These fields are categorized separately as technical metadata
     */
    private static final String[] TECHNICAL_FIELD_PATTERNS = {
        // Primary keys and UUIDs
        "_ID", "_UU",
        // Audit fields
        "Created", "CreatedBy", "Updated", "UpdatedBy",
        // Technical status fields
        "IsActive", "Processed", "Processing",
        // Multi-tenancy fields
        "AD_Client_ID", "AD_Org_ID",
        // Technical metadata
        "EntityType", "IsSummary", "IsTranslated",
        "Record_ID", "Record_UU",
        // System references
        "AD_Image_ID", "AD_PrintColor_ID", "AD_PrintFont_ID",
        // Versioning and technical tracking
        "Version", "ColumnSQL", "VFormat", "ValueFormat",
        // Technical configuration
        "ReadOnlyLogic", "DisplayLogic", "MandatoryLogic",
        "DefaultValue", "VFormat", "FormatPattern",
        // Workflow and process control
        "AD_WF_", "WFState", "DocAction", "DocStatus",
        // Import/Export technical fields
        "I_IsImported", "I_ErrorMsg", "Imported",
        // Replication
        "EXP_", "IMP_"
    };

    @Override
    public String getContextType() {
        return "WINDOW";
    }

    @Override
    public JSONObject extractContext(
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    ) {
        JSONObject context = new JSONObject();

        try {
            // 1. User context
            addUserContext(context, ctx);

            // 2. Window metadata — iDempiere stores window ID under _WinInfo_AD_Window_ID
            int windowId = Env.getContextAsInt(ctx, windowNo, "_WinInfo_AD_Window_ID");
            if (windowId > 0) {
                MWindow window = MWindow.get(ctx, windowId);
                if (window != null) {
                    addWindowMetadata(context, window, ctx);
                }
            }

            // 3. Current tab and record info
            Integer tabNo = parameters.getInt("tabNo", 0);
            addTabContext(context, ctx, windowNo, tabNo);

            // 4. Record data (current record values)
            addRecordData(context, ctx, windowNo, tabNo);

            // 5. Related tabs/records if requested
            if (parameters.getBoolean("includeChildTabs", false)) {
                addChildTabsData(context, ctx, windowNo);
            }

            context.put("success", true);

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to extract window context", e);
            context.put("error", "Failed to extract context: " + e.getMessage());
            context.put("success", false);
        }

        return context;
    }

    /**
     * Add user context information
     */
    private void addUserContext(JSONObject context, Properties ctx) {
        JSONObject userCtx = new JSONObject();
        userCtx.put("user_id", Env.getAD_User_ID(ctx));
        userCtx.put("user_name", Env.getContext(ctx, "#AD_User_Name"));
        userCtx.put("client_id", Env.getAD_Client_ID(ctx));
        userCtx.put("client_name", Env.getContext(ctx, "#AD_Client_Name"));
        userCtx.put("org_id", Env.getAD_Org_ID(ctx));
        userCtx.put("org_name", Env.getContext(ctx, "#AD_Org_Name"));
        userCtx.put("role_id", Env.getAD_Role_ID(ctx));
        userCtx.put("role_name", Env.getContext(ctx, "#AD_Role_Name"));
        userCtx.put("language", Env.getContext(ctx, "#AD_Language"));
        userCtx.put("date", Env.getContext(ctx, "#Date"));

        context.put("user_context", userCtx);
    }

    /**
     * Add window metadata
     */
    private void addWindowMetadata(JSONObject context, MWindow window, Properties ctx) {
        JSONObject windowMeta = new JSONObject();
        windowMeta.put("window_id", window.getAD_Window_ID());
        windowMeta.put("name", window.getName());
        windowMeta.put("description", window.getDescription());
        windowMeta.put("help", window.getHelp());
        windowMeta.put("window_type", window.getWindowType());
        windowMeta.put("is_active", window.isActive());

        // Add translated name/description if available
        String language = Env.getContext(ctx, "#AD_Language");
        if (language != null && !language.equals("en_US")) {
            String translatedName = window.get_Translation("Name", language);
            if (translatedName != null && !translatedName.equals(window.getName())) {
                windowMeta.put("name_translated", translatedName);
            }
            String translatedDesc = window.get_Translation("Description", language);
            if (translatedDesc != null && !translatedDesc.equals(window.getDescription())) {
                windowMeta.put("description_translated", translatedDesc);
            }
            String translatedHelp = window.get_Translation("Help", language);
            if (translatedHelp != null && !translatedHelp.equals(window.getHelp())) {
                windowMeta.put("help_translated", translatedHelp);
            }
        }

        context.put("window_metadata", windowMeta);
    }

    /**
     * Add current tab context
     */
    private void addTabContext(
        JSONObject context,
        Properties ctx,
        int windowNo,
        int tabNo
    ) {
        JSONObject tabCtx = new JSONObject();

        // iDempiere stores tab metadata with the _TabInfo_ prefix, e.g. "1|0|_TabInfo_Name".
        // Field values (record data) are stored directly, e.g. "1|0|C_Vocabulary_ID".
        int adTableId = Env.getContextAsInt(ctx, windowNo, tabNo, "_TabInfo_AD_Table_ID");
        int adTabId   = Env.getContextAsInt(ctx, windowNo, tabNo, "_TabInfo_AD_Tab_ID");
        String tabName      = Env.getContext(ctx, windowNo, tabNo, "_TabInfo_Name", true);
        int tabLevel        = Env.getContextAsInt(ctx, windowNo, tabNo, "_TabInfo_TabLevel");
        String keyColumnName = Env.getContext(ctx, windowNo, tabNo, "_TabInfo_KeyColumnName", true);

        // Derive table name from AD_Table_ID
        String tableName = "";
        if (adTableId > 0) {
            MTable table = MTable.get(ctx, adTableId);
            if (table != null) {
                tableName = table.getTableName();
            }
        }

        tabCtx.put("tab_no", tabNo);
        tabCtx.put("tab_name", tabName);
        tabCtx.put("table_name", tableName);
        tabCtx.put("tab_level", tabLevel);

        // Track exact current row being viewed
        int currentRow = Env.getContextAsInt(ctx, windowNo, "CurrentRow");
        if (currentRow >= 0) {
            tabCtx.put("current_row", currentRow);
        }

        // Get record ID from the primary key column stored in context (e.g. "1|0|C_Vocabulary_ID")
        if (keyColumnName != null && !keyColumnName.isEmpty()) {
            int selectedRecordId = Env.getContextAsInt(ctx, windowNo, tabNo, keyColumnName);
            if (selectedRecordId > 0) {
                tabCtx.put("selected_record_id", selectedRecordId);
                tabCtx.put("selected_record_key", keyColumnName);
            }
        }

        // Load AD tab metadata
        if (adTabId > 0) {
            tabCtx.put("tab_id", adTabId);
            MTab tab = MTab.get(adTabId);
            if (tab != null) {
                if (tab.getDescription() != null && !tab.getDescription().isEmpty()) {
                    tabCtx.put("tab_description", tab.getDescription());
                }
                tabCtx.put("is_readonly", tab.isReadOnly());
                tabCtx.put("is_insert_record", tab.isInsertRecord());
            }
        }

        context.put("tab_context", tabCtx);
    }

    /**
     * Add record data from context
     * Separates business fields from technical/system fields for better AI context
     */
    private void addRecordData(
        JSONObject context,
        Properties ctx,
        int windowNo,
        int tabNo
    ) {
        JSONObject businessData = new JSONObject();
        JSONObject technicalData = new JSONObject();

        // Extract all context variables for this tab
        // Format: WindowNo|TabNo|FieldName or WindowNo|FieldName
        String prefixWithTab = windowNo + "|" + tabNo + "|";
        String prefixWithoutTab = windowNo + "|";

        for (Object key : ctx.keySet()) {
            String keyStr = key.toString();
            String fieldName = null;

            // Try tab-specific first
            if (keyStr.startsWith(prefixWithTab)) {
                fieldName = keyStr.substring(prefixWithTab.length());
            }
            // Then window-level (ensure no additional "|" after window prefix)
            else if (keyStr.startsWith(prefixWithoutTab) && keyStr.indexOf("|", prefixWithoutTab.length()) == -1) {
                fieldName = keyStr.substring(prefixWithoutTab.length());
            }

            if (fieldName != null && fieldName.length() > 0) {
                String value = ctx.getProperty(keyStr);
                if (value != null && value.length() > 0) {
                    // Categorize field as technical or business
                    if (isTechnicalField(fieldName)) {
                        technicalData.put(fieldName, value);
                    } else {
                        businessData.put(fieldName, value);
                    }
                }
            }
        }

        // Add business data first (more important)
        context.put("record_data", businessData);

        // Add technical data separately (less important, but still available)
        if (technicalData.length() > 0) {
            context.put("technical_data", technicalData);
        }
    }

    /**
     * Check if a field name matches technical field patterns
     * @param fieldName the field name to check
     * @return true if field is technical/system field
     */
    private boolean isTechnicalField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }

        // Check against all patterns
        for (String pattern : TECHNICAL_FIELD_PATTERNS) {
            if (fieldName.equals(pattern) ||
                fieldName.endsWith(pattern) ||
                fieldName.startsWith(pattern) ||
                fieldName.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Add child tabs data with context information
     * Extracts both metadata and actual data from sub-tabs
     */
    private void addChildTabsData(
        JSONObject context,
        Properties ctx,
        int windowNo
    ) {
        JSONArray childTabs = new JSONArray();

        try {
            // Get window ID
            int windowId = Env.getContextAsInt(ctx, windowNo, "_WinInfo_AD_Window_ID");
            if (windowId > 0) {
                MWindow window = MWindow.get(ctx, windowId);
                if (window != null) {
                    MTab[] tabs = window.getTabs(false, null);
                    if (tabs != null && tabs.length > 0) {
                        for (int i = 0; i < tabs.length; i++) {
                            MTab tab = tabs[i];
                            if (tab.getTabLevel() > 0) { // Only child tabs
                                JSONObject childTab = new JSONObject();
                                childTab.put("tab_id", tab.getAD_Tab_ID());
                                childTab.put("tab_name", tab.getName());
                                childTab.put("table_name", tab.getAD_Table().getTableName());
                                childTab.put("tab_level", tab.getTabLevel());
                                childTab.put("sequence", tab.getSeqNo());

                                // Add description if available
                                if (tab.getDescription() != null && !tab.getDescription().trim().isEmpty()) {
                                    childTab.put("description", tab.getDescription());
                                }

                                // Try to extract context data for this sub-tab
                                // Sub-tabs are indexed starting from 1
                                int subTabNo = i;
                                extractSubTabData(childTab, ctx, windowNo, subTabNo, tab);

                                childTabs.put(childTab);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to extract child tabs", e);
        }

        if (childTabs.length() > 0) {
            context.put("child_tabs", childTabs);
        }
    }

    /**
     * Extract data from a sub-tab
     * Attempts to get record counts and sample data from context
     *
     * @param childTab JSON object to populate with sub-tab data
     * @param ctx context
     * @param windowNo window number
     * @param tabNo tab number
     * @param tab MTab instance
     */
    private void extractSubTabData(
        JSONObject childTab,
        Properties ctx,
        int windowNo,
        int tabNo,
        MTab tab
    ) {
        try {
            // Get current row for this sub-tab
            int currentRow = Env.getContextAsInt(ctx, windowNo, tabNo, "CurrentRow");
            if (currentRow >= 0) {
                childTab.put("current_row", currentRow);
            }

            // Get row count if available
            String rowCountKey = windowNo + "|" + tabNo + "|#rowcount";
            String rowCountStr = ctx.getProperty(rowCountKey);
            if (rowCountStr != null && !rowCountStr.trim().isEmpty()) {
                try {
                    int rowCount = Integer.parseInt(rowCountStr);
                    childTab.put("row_count", rowCount);
                } catch (NumberFormatException e) {
                    // Ignore invalid row count
                }
            }

            // Extract sub-tab record data (similar to addRecordData but for specific sub-tab)
            JSONObject subTabRecordData = new JSONObject();
            String prefixWithTab = windowNo + "|" + tabNo + "|";

            // Limit fields to avoid too much data
            int fieldCount = 0;
            int maxFields = 10; // Limit sub-tab data to avoid overwhelming context

            for (Object key : ctx.keySet()) {
                if (fieldCount >= maxFields) {
                    break;
                }

                String keyStr = key.toString();
                if (keyStr.startsWith(prefixWithTab)) {
                    String fieldName = keyStr.substring(prefixWithTab.length());

                    // Skip internal counters and technical fields
                    if (fieldName.startsWith("#") || fieldName.equals("CurrentRow")) {
                        continue;
                    }

                    String value = ctx.getProperty(keyStr);
                    if (value != null && !value.trim().isEmpty()) {
                        // Only include business fields for sub-tabs (to keep context concise)
                        if (!isTechnicalField(fieldName)) {
                            subTabRecordData.put(fieldName, value);
                            fieldCount++;
                        }
                    }
                }
            }

            if (subTabRecordData.length() > 0) {
                childTab.put("current_record_data", subTabRecordData);
            }

        } catch (Exception e) {
            log.log(Level.FINE, "Failed to extract sub-tab data for tab " + tabNo, e);
            // Don't fail the whole operation if sub-tab data extraction fails
        }
    }

    @Override
    public boolean validateContext(JSONObject context) {
        // Validate required fields
        return context.has("user_context") &&
               context.has("tab_context") &&
               context.optBoolean("success", false);
    }

    @Override
    public String[] getSensitiveFields() {
        // Fields that should be redacted if present in record data
        return new String[] {
            "Password",
            "UserPIN",
            "CreditCardNumber",
            "CreditCardVV",
            "CVV",
            "SSN",
            "TaxID",
            "BankAccount",
            "BankAccountNo",
            "IBAN",
            "APIKey",
            "AccessToken",
            "SecretKey",
            "PrivateKey",
            "CertificateData"
        };
    }

    @Override
    public String getDescription() {
        return "Context provider for iDempiere windows and tabs";
    }
}