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

import org.json.JSONObject;
import org.json.JSONArray;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.cloudempiere.ai.context.IAIContextProvider;
import com.cloudempiere.ai.context.ContextParameters;
import com.cloudempiere.ai.kb.dto.KnowledgeBaseEntry;
import com.cloudempiere.ai.kb.dto.KnowledgeBaseHierarchy;
import com.cloudempiere.ai.kb.dto.EditorJsSyntaxInfo;
import com.cloudempiere.ai.kb.parser.EditorJsParser;
import com.cloudempiere.ai.kb.parser.EditorJsParserEnhanced;

import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Context provider for knowledge base navigation and structure
 *
 * <p>Extracts and structures the k_entry navigation tree from iDempiere
 * knowledge base tables. Converts editor.js content to markdown for AI analysis.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class KnowledgeBaseContextProvider implements IAIContextProvider {

    private static final CLogger log = CLogger.getCLogger(KnowledgeBaseContextProvider.class);

    @Override
    public String getContextType() {
        return "KNOWLEDGE_BASE";
    }

    @Override
    public JSONObject extractContext(
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    ) {
        JSONObject context = new JSONObject();

        try {
            // Get requested knowledge base type
            String kType = parameters.getString("k_type", null);
            if (kType == null) {
                context.put("error", "k_type parameter is required");
                context.put("success", false);
                return context;
            }

            // Add user context
            addUserContext(context, ctx);

            // Load knowledge base hierarchy
            KnowledgeBaseHierarchy hierarchy = loadKnowledgeBaseHierarchy(ctx, kType);

            // Build context JSON
            context.put("k_type", kType);
            context.put("total_entries", hierarchy.getTotalEntries());
            context.put("tree_structure", buildTreeJson(hierarchy));

            // Add editor.js syntax capabilities
            context.put("syntax_capabilities", buildSyntaxCapabilitiesJson());

            context.put("success", true);

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to extract knowledge base context", e);
            context.put("error", "Failed to extract context: " + e.getMessage());
            context.put("success", false);
        }

        return context;
    }

    /**
     * Load complete knowledge base hierarchy from database
     *
     * <p>Uses v_k_entry_hierarchy materialized view for efficient hierarchical queries.
     * The view handles recursive CTEs and breadcrumb generation at DB level.
     */
    private KnowledgeBaseHierarchy loadKnowledgeBaseHierarchy(Properties ctx, String kType) {
        // Import the DB query helper class
        com.cloudempiere.ai.kb.database.KnowledgeBaseQuery kbQuery =
            new com.cloudempiere.ai.kb.database.KnowledgeBaseQuery();

        // Use materialized view for efficient loading
        KnowledgeBaseHierarchy hierarchy = com.cloudempiere.ai.kb.database.KnowledgeBaseQuery
            .loadHierarchyFromView(kType);

        if (hierarchy.getTotalEntries() == 0) {
            log.warning("No KB entries found for type: " + kType);
        }

        return hierarchy;
    }

    /**
     * Build tree structure as JSON for AI consumption
     */
    private JSONObject buildTreeJson(KnowledgeBaseHierarchy hierarchy) {
        JSONObject treeJson = new JSONObject();
        JSONArray rootEntries = new JSONArray();

        for (KnowledgeBaseEntry root : hierarchy.getRootEntries()) {
            rootEntries.put(buildEntryJson(root, hierarchy));
        }

        treeJson.put("root_entries", rootEntries);
        treeJson.put("total_count", hierarchy.getTotalEntries());

        return treeJson;
    }

    /**
     * Build entry and its children as JSON
     */
    private JSONObject buildEntryJson(KnowledgeBaseEntry entry, KnowledgeBaseHierarchy hierarchy) {
        JSONObject entryJson = new JSONObject();

        entryJson.put("k_entry_id", entry.getK_entry_id());
        entryJson.put("name", entry.getName());
        entryJson.put("title", entry.getTitle());
        entryJson.put("description", entry.getDescription());

        if (entry.getContentMarkdown() != null) {
            // Limit content for context size
            String content = entry.getContentMarkdown();
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            entryJson.put("content_preview", content);
        }

        entryJson.put("sequence_no", entry.getSequence_no());
        entryJson.put("depth", hierarchy.getDepth(entry.getK_entry_id()));

        // Add children recursively
        JSONArray childrenJson = new JSONArray();
        for (KnowledgeBaseEntry child : hierarchy.getChildren(entry.getK_entry_id())) {
            childrenJson.put(buildEntryJson(child, hierarchy));
        }

        if (childrenJson.length() > 0) {
            entryJson.put("children", childrenJson);
        }

        return entryJson;
    }

    /**
     * Build editor.js syntax capabilities as JSON
     *
     * <p>Provides AI with information about supported block types, inline tools,
     * and plugins to enable syntax-aware recommendations and validation.
     */
    private JSONObject buildSyntaxCapabilitiesJson() {
        JSONObject syntaxJson = new JSONObject();

        EditorJsSyntaxInfo syntaxInfo = EditorJsSyntaxInfo.getInstance();

        // Block types
        JSONArray blockTypesArray = new JSONArray();
        for (EditorJsSyntaxInfo.BlockTypeInfo blockType : syntaxInfo.getBlockTypes().values()) {
            JSONObject blockJson = new JSONObject();
            blockJson.put("type", blockType.type);
            blockJson.put("name", blockType.displayName);
            blockJson.put("description", blockType.description);
            blockJson.put("fields", blockType.fields);
            blockTypesArray.put(blockJson);
        }
        syntaxJson.put("block_types", blockTypesArray);
        syntaxJson.put("block_types_count", syntaxInfo.getBlockTypes().size());

        // Inline tools
        JSONArray inlineToolsArray = new JSONArray();
        for (EditorJsSyntaxInfo.InlineToolInfo tool : syntaxInfo.getInlineTools().values()) {
            JSONObject toolJson = new JSONObject();
            toolJson.put("name", tool.name);
            toolJson.put("display_name", tool.displayName);
            toolJson.put("shortcut", tool.shortcut);
            toolJson.put("description", tool.description);
            inlineToolsArray.put(toolJson);
        }
        syntaxJson.put("inline_tools", inlineToolsArray);
        syntaxJson.put("inline_tools_count", syntaxInfo.getInlineTools().size());

        // Plugins by type
        JSONArray pluginsArray = new JSONArray();
        for (EditorJsSyntaxInfo.PluginInfo plugin : syntaxInfo.getPlugins()) {
            JSONObject pluginJson = new JSONObject();
            pluginJson.put("name", plugin.name);
            pluginJson.put("package", plugin.packageName);
            pluginJson.put("type", plugin.type);
            pluginJson.put("description", plugin.description);
            pluginsArray.put(pluginJson);
        }
        syntaxJson.put("plugins", pluginsArray);
        syntaxJson.put("plugins_count", syntaxInfo.getPlugins().size());
        syntaxJson.put("official_plugins", syntaxInfo.getPluginsByType("official").size());
        syntaxJson.put("custom_plugins", syntaxInfo.getPluginsByType("custom").size());
        syntaxJson.put("third_party_plugins", syntaxInfo.getPluginsByType("third-party").size());

        // Output formats
        syntaxJson.put("output_formats", new JSONArray(syntaxInfo.getOutputFormats()));

        // Summary stats
        syntaxJson.put("total_capabilities", syntaxInfo.getTotalCapabilities());

        return syntaxJson;
    }

    /**
     * Add user context information
     */
    private void addUserContext(JSONObject context, Properties ctx) {
        JSONObject userCtx = new JSONObject();
        userCtx.put("user_id", Env.getAD_User_ID(ctx));
        userCtx.put("user_name", Env.getContext(ctx, "#AD_User_Name"));
        userCtx.put("role_id", Env.getAD_Role_ID(ctx));
        userCtx.put("language", Env.getContext(ctx, "#AD_Language"));

        context.put("user_context", userCtx);
    }

    @Override
    public boolean validateContext(JSONObject context) {
        return context.has("k_type") &&
               context.has("tree_structure") &&
               context.optBoolean("success", false);
    }

    @Override
    public String[] getSensitiveFields() {
        // Knowledge base content shouldn't contain sensitive data,
        // but just in case
        return new String[] {
            "APIKey",
            "Password",
            "SecretKey",
            "PrivateKey"
        };
    }

    @Override
    public String getDescription() {
        return "Context provider for iDempiere knowledge base (k_entry) structure and content";
    }
}
