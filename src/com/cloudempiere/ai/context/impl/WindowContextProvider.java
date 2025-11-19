/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) CloudEmpiere, Inc. All Rights Reserved.                     *
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

import org.compiere.model.MWindow;
import org.compiere.model.MTab;
import org.compiere.util.Env;
import org.compiere.util.CLogger;
import org.json.JSONObject;
import org.json.JSONArray;
import com.cloudempiere.ai.context.IAIContextProvider;
import com.cloudempiere.ai.context.ContextParameters;

import java.util.Properties;
import java.util.logging.Level;

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
 * @author CloudEmpiere
 * @version 1.0
 */
public class WindowContextProvider implements IAIContextProvider {

    private static final CLogger log = CLogger.getCLogger(WindowContextProvider.class);

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

            // 2. Window metadata
            int windowId = Env.getContextAsInt(ctx, windowNo, "AD_Window_ID");
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

        // Get tab metadata from context
        String tableName = Env.getContext(ctx, windowNo, tabNo, "TableName", false);
        int recordId = Env.getContextAsInt(ctx, windowNo, "Record_ID");
        int tabLevel = Env.getContextAsInt(ctx, windowNo, tabNo, "TabLevel", false);
        String tabName = Env.getContext(ctx, windowNo, tabNo, "Name", false);

        tabCtx.put("tab_no", tabNo);
        tabCtx.put("tab_name", tabName);
        tabCtx.put("table_name", tableName);
        tabCtx.put("record_id", recordId);
        tabCtx.put("tab_level", tabLevel);

        // Get AD_Tab_ID if available
        int tabId = Env.getContextAsInt(ctx, windowNo, tabNo, "AD_Tab_ID");
        if (tabId > 0) {
            tabCtx.put("tab_id", tabId);

            // Load full tab metadata
            MTab tab = MTab.get(tabId);
            if (tab != null) {
                tabCtx.put("tab_description", tab.getDescription());
                tabCtx.put("tab_help", tab.getHelp());
                tabCtx.put("is_readonly", tab.isReadOnly());
                tabCtx.put("is_insert_record", tab.isInsertRecord());
                tabCtx.put("commit_warning", tab.getCommitWarning());
            }
        }

        context.put("tab_context", tabCtx);
    }

    /**
     * Add record data from context
     */
    private void addRecordData(
        JSONObject context,
        Properties ctx,
        int windowNo,
        int tabNo
    ) {
        JSONObject recordData = new JSONObject();

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
                    recordData.put(fieldName, value);
                }
            }
        }

        context.put("record_data", recordData);
    }

    /**
     * Add child tabs data
     */
    private void addChildTabsData(
        JSONObject context,
        Properties ctx,
        int windowNo
    ) {
        JSONArray childTabs = new JSONArray();

        try {
            // Get window ID
            int windowId = Env.getContextAsInt(ctx, windowNo, "AD_Window_ID");
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