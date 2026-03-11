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
package com.cloudempiere.ai.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.compiere.util.CLogger;

import java.util.logging.Level;

/**
 * Parser for editor.js JSON format to markdown
 *
 * <p>Converts editor.js block-based content to markdown for AI analysis.
 * Supports: headings, paragraphs, lists, code blocks, quotes, etc.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class EditorJsParser {

    private static final CLogger log = CLogger.getCLogger(EditorJsParser.class);

    /**
     * Parse editor.js JSON string to markdown
     *
     * @param editorJsJson editor.js JSON string
     * @return markdown representation
     */
    public static String parseToMarkdown(String editorJsJson) {
        if (editorJsJson == null || editorJsJson.trim().isEmpty()) {
            return "";
        }

        try {
            JSONObject editorObj = new JSONObject(editorJsJson);
            return parseEditorJson(editorObj);
        } catch (JSONException e) {
            log.log(Level.WARNING, "Failed to parse editor.js JSON", e);
            return editorJsJson; // Fallback to original
        }
    }

    /**
     * Parse editor.js JSONObject to markdown
     *
     * @param editorObj editor.js JSONObject
     * @return markdown representation
     */
    public static String parseEditorJson(JSONObject editorObj) {
        StringBuilder markdown = new StringBuilder();

        try {
            JSONArray blocks = editorObj.optJSONArray("blocks");
            if (blocks != null) {
                for (int i = 0; i < blocks.length(); i++) {
                    JSONObject block = blocks.getJSONObject(i);
                    String blockMarkdown = parseBlock(block);
                    if (blockMarkdown != null && !blockMarkdown.isEmpty()) {
                        markdown.append(blockMarkdown).append("\n");
                    }
                }
            }
        } catch (JSONException e) {
            log.log(Level.WARNING, "Failed to parse editor.js blocks", e);
        }

        return markdown.toString().trim();
    }

    /**
     * Parse a single editor.js block
     *
     * @param block editor.js block
     * @return markdown for this block
     */
    private static String parseBlock(JSONObject block) {
        try {
            String type = block.optString("type", "");
            JSONObject data = block.optJSONObject("data");

            switch (type) {
                case "heading":
                    return parseHeading(data);
                case "paragraph":
                    return parseParagraph(data);
                case "list":
                    return parseList(data);
                case "code":
                    return parseCode(data);
                case "quote":
                    return parseQuote(data);
                case "image":
                    return parseImage(data);
                case "table":
                    return parseTable(data);
                case "delimiter":
                    return "---";
                case "warning":
                    return parseWarning(data);
                default:
                    log.fine("Unknown block type: " + type);
                    return null;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to parse block", e);
            return null;
        }
    }

    private static String parseHeading(JSONObject data) {
        if (data == null) return "";
        int level = data.optInt("level", 1);
        String text = data.optString("text", "");
        return "#".repeat(level) + " " + escapeMarkdown(text);
    }

    private static String parseParagraph(JSONObject data) {
        if (data == null) return "";
        String text = data.optString("text", "");
        return escapeMarkdown(text);
    }

    private static String parseList(JSONObject data) {
        if (data == null) return "";
        StringBuilder listMarkdown = new StringBuilder();
        JSONArray items = data.optJSONArray("items");
        String style = data.optString("style", "unordered");

        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                String item = items.optString(i, "");
                if (!item.isEmpty()) {
                    if ("ordered".equals(style)) {
                        listMarkdown.append((i + 1)).append(". ");
                    } else {
                        listMarkdown.append("- ");
                    }
                    listMarkdown.append(escapeMarkdown(item)).append("\n");
                }
            }
        }

        return listMarkdown.toString().trim();
    }

    private static String parseCode(JSONObject data) {
        if (data == null) return "";
        String code = data.optString("code", "");
        String language = data.optString("language", "");

        StringBuilder codeBlock = new StringBuilder("```");
        if (!language.isEmpty()) {
            codeBlock.append(language);
        }
        codeBlock.append("\n").append(code).append("\n```");
        return codeBlock.toString();
    }

    private static String parseQuote(JSONObject data) {
        if (data == null) return "";
        String text = data.optString("text", "");
        String caption = data.optString("caption", "");

        StringBuilder quote = new StringBuilder("> ").append(escapeMarkdown(text));
        if (!caption.isEmpty()) {
            quote.append("\n> — ").append(escapeMarkdown(caption));
        }
        return quote.toString();
    }

    private static String parseImage(JSONObject data) {
        if (data == null) return "";
        String url = data.optString("url", "");
        String caption = data.optString("caption", "");

        StringBuilder image = new StringBuilder("![");
        if (!caption.isEmpty()) {
            image.append(escapeMarkdown(caption));
        }
        image.append("](").append(url).append(")");
        return image.toString();
    }

    private static String parseTable(JSONObject data) {
        if (data == null) return "";
        JSONArray content = data.optJSONArray("content");
        if (content == null || content.length() == 0) return "";

        StringBuilder table = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            JSONArray row = content.optJSONArray(i);
            if (row != null) {
                table.append("| ");
                for (int j = 0; j < row.length(); j++) {
                    String cell = row.optString(j, "");
                    table.append(escapeMarkdown(cell)).append(" | ");
                }
                table.append("\n");

                // Add separator after first row (header)
                if (i == 0) {
                    table.append("|");
                    for (int j = 0; j < row.length(); j++) {
                        table.append(" --- |");
                    }
                    table.append("\n");
                }
            }
        }

        return table.toString().trim();
    }

    private static String parseWarning(JSONObject data) {
        if (data == null) return "";
        String title = data.optString("title", "Warning");
        String message = data.optString("message", "");

        return "[Warning] **" + escapeMarkdown(title) + "**: " + escapeMarkdown(message);
    }

    /**
     * Escape special markdown characters
     *
     * @param text text to escape
     * @return escaped text
     */
    private static String escapeMarkdown(String text) {
        return SecuritySanitizer.escapeMarkdown(text != null ? text : "");
    }

    /**
     * Convert markdown back to plain text (for similarity matching)
     *
     * @param markdown markdown text
     * @return plain text
     */
    public static String markdownToPlainText(String markdown) {
        if (markdown == null) return "";

        return markdown
            // Remove headers
            .replaceAll("^#+\\s+", "")
            // Remove bold/italic
            .replaceAll("\\*\\*|__|\\*|_", "")
            // Remove code blocks
            .replaceAll("```[^`]*```", "")
            // Remove inline code
            .replaceAll("`[^`]*`", "")
            // Remove links
            .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
            // Remove images
            .replaceAll("!\\[([^\\]]*)]\\([^)]+\\)", "")
            // Remove list markers
            .replaceAll("^[-*+]\\s+", "")
            .replaceAll("^\\d+\\.\\s+", "")
            // Remove blockquotes
            .replaceAll("^>\\s+", "")
            // Remove horizontal rules
            .replaceAll("^-{3,}$", "")
            .trim();
    }
}
