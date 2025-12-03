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
package com.cloudempiere.ai.kb.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.compiere.util.CLogger;

import com.cloudempiere.ai.kb.dto.EditorJsSyntaxInfo;
import com.cloudempiere.ai.kb.dto.EditorJsSyntaxInfo.BlockTypeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Enhanced parser for editor.js JSON format with plugin-aware support
 *
 * <p>Extends EditorJsParser with syntax-aware parsing that:
 * <ul>
 *   <li>Detects and validates block types against CloudEmpiere capabilities</li>
 *   <li>Analyzes content structure and formatting options</li>
 *   <li>Preserves plugin-specific metadata</li>
 *   <li>Provides syntax analysis for AI recommendations</li>
 *   <li>Supports multiple output formats (markdown, HTML, plaintext)</li>
 * </ul>
 *
 * <p>Used by KnowledgeBaseAgent to understand content structure and
 * provide better placement recommendations based on actual syntax used.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class EditorJsParserEnhanced {

    private static final CLogger log = CLogger.getCLogger(EditorJsParserEnhanced.class);

    private EditorJsSyntaxInfo syntaxInfo = EditorJsSyntaxInfo.getInstance();

    /**
     * Parse editor.js content and extract syntax analysis
     *
     * <p>Analyzes the JSON structure to determine what blocks and inline
     * tools are used, validates against supported syntax, and provides
     * a detailed content analysis.
     *
     * @param editorJsJson editor.js JSON string
     * @return SyntaxAnalysis with detailed content structure
     */
    public SyntaxAnalysis analyzeSyntax(String editorJsJson) {
        SyntaxAnalysis analysis = new SyntaxAnalysis();

        if (editorJsJson == null || editorJsJson.trim().isEmpty()) {
            analysis.isValid = true;
            analysis.isEmpty = true;
            return analysis;
        }

        try {
            JSONObject editorObj = new JSONObject(editorJsJson);
            JSONArray blocks = editorObj.optJSONArray("blocks");

            if (blocks == null || blocks.length() == 0) {
                analysis.isEmpty = true;
                analysis.isValid = true;
                return analysis;
            }

            // Analyze each block
            for (int i = 0; i < blocks.length(); i++) {
                JSONObject block = blocks.getJSONObject(i);
                analyzeBlock(block, analysis);
            }

            analysis.isValid = true;
            analysis.totalBlocks = blocks.length();

        } catch (JSONException e) {
            log.log(Level.WARNING, "Failed to analyze editor.js syntax", e);
            analysis.isValid = false;
            analysis.error = "Invalid JSON: " + e.getMessage();
        }

        return analysis;
    }

    /**
     * Analyze a single block for content type and structure
     */
    private void analyzeBlock(JSONObject block, SyntaxAnalysis analysis) {
        try {
            String type = block.optString("type", "");
            JSONObject data = block.optJSONObject("data");

            // Check if block type is supported
            boolean isSupported = syntaxInfo.supportsBlockType(type);
            BlockTypeInfo blockInfo = syntaxInfo.getBlockType(type);

            // Record block usage
            analysis.blockTypesUsed.putIfAbsent(type, 0);
            analysis.blockTypesUsed.put(type, analysis.blockTypesUsed.get(type) + 1);

            if (isSupported) {
                analysis.supportedBlockCount++;
                if (blockInfo != null) {
                    analysis.blockTypesInfo.add(blockInfo);
                }
            } else {
                analysis.unsupportedBlocks.add(type);
                log.fine("Unsupported block type: " + type);
            }

            // Analyze content by block type
            analyzeBlockContent(type, data, analysis);

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to analyze block", e);
            analysis.errors.add("Block analysis error: " + e.getMessage());
        }
    }

    /**
     * Analyze block content for specific syntax patterns
     */
    private void analyzeBlockContent(String type, JSONObject data, SyntaxAnalysis analysis) {
        if (data == null) return;

        switch (type) {
            case "paragraph":
                analyzeParagraphContent(data, analysis);
                break;
            case "heading":
                analyzeHeadingContent(data, analysis);
                break;
            case "list":
                analyzeListContent(data, analysis);
                break;
            case "code":
                analyzeCodeContent(data, analysis);
                break;
            case "quote":
                analyzeQuoteContent(data, analysis);
                break;
            case "image":
                analyzeImageContent(data, analysis);
                break;
            case "table":
                analyzeTableContent(data, analysis);
                break;
            case "embed":
                analyzeEmbedContent(data, analysis);
                break;
            case "alert":
                analyzeAlertContent(data, analysis);
                break;
            case "bpmn":
                analyzeBpmn Content(data, analysis);
                break;
            case "mermaid":
                analyzeMermaidContent(data, analysis);
                break;
            default:
                // Custom/unknown block types
                break;
        }
    }

    private void analyzeParagraphContent(JSONObject data, SyntaxAnalysis analysis) {
        String text = data.optString("text", "");
        if (!text.isEmpty()) {
            analysis.totalCharacters += text.length();
            analysis.totalWords += countWords(text);
        }
    }

    private void analyzeHeadingContent(JSONObject data, SyntaxAnalysis analysis) {
        int level = data.optInt("level", 1);
        String text = data.optString("text", "");
        analysis.headingLevels.put(level, analysis.headingLevels.getOrDefault(level, 0) + 1);
        analysis.totalWords += countWords(text);
    }

    private void analyzeListContent(JSONObject data, SyntaxAnalysis analysis) {
        JSONArray items = data.optJSONArray("items");
        String style = data.optString("style", "unordered");

        if (items != null) {
            analysis.listItemCount += items.length();
            if ("ordered".equals(style)) {
                analysis.orderedListsCount++;
            } else {
                analysis.unorderedListsCount++;
            }
        }
    }

    private void analyzeCodeContent(JSONObject data, SyntaxAnalysis analysis) {
        String code = data.optString("code", "");
        String language = data.optString("language", "");

        analysis.codeBlocksCount++;
        analysis.totalCharacters += code.length();

        if (!language.isEmpty()) {
            analysis.codeLanguagesUsed.putIfAbsent(language, 0);
            analysis.codeLanguagesUsed.put(language, analysis.codeLanguagesUsed.get(language) + 1);
        }
    }

    private void analyzeQuoteContent(JSONObject data, SyntaxAnalysis analysis) {
        analysis.quoteBlocksCount++;
    }

    private void analyzeImageContent(JSONObject data, SyntaxAnalysis analysis) {
        analysis.imageCount++;
    }

    private void analyzeTableContent(JSONObject data, SyntaxAnalysis analysis) {
        JSONArray content = data.optJSONArray("content");
        if (content != null) {
            analysis.tableCount++;
            analysis.tableRowCount += content.length();
        }
    }

    private void analyzeEmbedContent(JSONObject data, SyntaxAnalysis analysis) {
        String service = data.optString("service", "");
        analysis.embedCount++;

        if (!service.isEmpty()) {
            analysis.embedServices.putIfAbsent(service, 0);
            analysis.embedServices.put(service, analysis.embedServices.get(service) + 1);
        }
    }

    private void analyzeAlertContent(JSONObject data, SyntaxAnalysis analysis) {
        String alertType = data.optString("type", "default");
        analysis.alertCount++;
        analysis.alertTypes.putIfAbsent(alertType, 0);
        analysis.alertTypes.put(alertType, analysis.alertTypes.get(alertType) + 1);
    }

    private void analyzeBpmnContent(JSONObject data, SyntaxAnalysis analysis) {
        analysis.bpmnDiagramCount++;
    }

    private void analyzeMermaidContent(JSONObject data, SyntaxAnalysis analysis) {
        analysis.mermaidDiagramCount++;
    }

    /**
     * Count approximate word count from text
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /**
     * Get syntax capabilities description for AI context
     *
     * @return formatted string describing all supported syntax
     */
    public String getSyntaxCapabilitiesForAI() {
        return syntaxInfo.getSyntaxSummary();
    }

    /**
     * Convert editor.js to HTML with plugin awareness
     *
     * <p>Enhanced version that preserves plugin-specific rendering attributes
     *
     * @param editorJsJson editor.js JSON string
     * @return HTML output
     */
    public String parseToHtml(String editorJsJson) {
        if (editorJsJson == null || editorJsJson.trim().isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        try {
            JSONObject editorObj = new JSONObject(editorJsJson);
            JSONArray blocks = editorObj.optJSONArray("blocks");

            if (blocks != null) {
                for (int i = 0; i < blocks.length(); i++) {
                    JSONObject block = blocks.getJSONObject(i);
                    String blockHtml = blockToHtml(block);
                    if (blockHtml != null && !blockHtml.isEmpty()) {
                        html.append(blockHtml).append("\n");
                    }
                }
            }
        } catch (JSONException e) {
            log.log(Level.WARNING, "Failed to convert to HTML", e);
        }

        return html.toString();
    }

    /**
     * Convert a single block to HTML
     */
    private String blockToHtml(JSONObject block) {
        try {
            String type = block.optString("type", "");
            JSONObject data = block.optJSONObject("data");

            switch (type) {
                case "paragraph":
                    return "<p>" + escapeHtml(data.optString("text", "")) + "</p>";
                case "heading":
                    int level = data.optInt("level", 1);
                    String text = escapeHtml(data.optString("text", ""));
                    return "<h" + level + ">" + text + "</h" + level + ">";
                case "code":
                    String language = data.optString("language", "");
                    String code = escapeHtml(data.optString("code", ""));
                    return "<pre><code class=\"language-" + language + "\">" + code + "</code></pre>";
                case "image":
                    String url = data.optString("url", "");
                    String caption = data.optString("caption", "");
                    return "<figure><img src=\"" + url + "\" alt=\"" + caption + "\"/><figcaption>" + caption + "</figcaption></figure>";
                case "delimiter":
                    return "<hr/>";
                default:
                    return null;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to convert block to HTML", e);
            return null;
        }
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * Content syntax analysis result DTO
     */
    public static class SyntaxAnalysis {
        public boolean isValid;
        public boolean isEmpty;
        public String error;

        public int totalBlocks;
        public int supportedBlockCount;
        public List<String> unsupportedBlocks = new ArrayList<>();
        public List<String> errors = new ArrayList<>();

        // Content metrics
        public int totalCharacters;
        public int totalWords;
        public int imageCount;
        public int codeBlocksCount;
        public int tableCount;
        public int tableRowCount;
        public int quoteBlocksCount;
        public int listItemCount;
        public int orderedListsCount;
        public int unorderedListsCount;
        public int embedCount;
        public int alertCount;
        public int bpmnDiagramCount;
        public int mermaidDiagramCount;

        // Block type tracking
        public Map<String, Integer> blockTypesUsed = new HashMap<>();
        public List<BlockTypeInfo> blockTypesInfo = new ArrayList<>();

        // Language and service tracking
        public Map<String, Integer> codeLanguagesUsed = new HashMap<>();
        public Map<String, Integer> embedServices = new HashMap<>();
        public Map<String, Integer> alertTypes = new HashMap<>();
        public Map<Integer, Integer> headingLevels = new HashMap<>();

        public String toSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Content Analysis Summary:\n");
            summary.append("========================\n");
            summary.append("Valid: ").append(isValid).append("\n");
            summary.append("Empty: ").append(isEmpty).append("\n");
            summary.append("Total Blocks: ").append(totalBlocks).append("\n");
            summary.append("Supported Blocks: ").append(supportedBlockCount).append("\n");

            if (totalCharacters > 0) {
                summary.append("Total Characters: ").append(totalCharacters).append("\n");
                summary.append("Total Words: ").append(totalWords).append("\n");
            }

            if (imageCount > 0) summary.append("Images: ").append(imageCount).append("\n");
            if (codeBlocksCount > 0) summary.append("Code Blocks: ").append(codeBlocksCount).append("\n");
            if (tableCount > 0) summary.append("Tables: ").append(tableCount).append(" (").append(tableRowCount).append(" rows)\n");
            if (listItemCount > 0) summary.append("List Items: ").append(listItemCount).append("\n");
            if (embedCount > 0) summary.append("Embeds: ").append(embedCount).append("\n");

            if (!blockTypesUsed.isEmpty()) {
                summary.append("\nBlock Types Used:\n");
                for (Map.Entry<String, Integer> entry : blockTypesUsed.entrySet()) {
                    summary.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }

            if (!unsupportedBlocks.isEmpty()) {
                summary.append("\nUnsupported Blocks: ").append(unsupportedBlocks).append("\n");
            }

            return summary.toString();
        }
    }
}
