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
package com.cloudempiere.ai.kb.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO representing editor.js syntax capabilities in CloudEmpiere
 *
 * <p>This class documents all block types, inline tools, and plugins
 * available in the CloudEmpiere editor.js implementation. Used by AI
 * to provide syntax-aware recommendations and content validation.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class EditorJsSyntaxInfo {

    private static final EditorJsSyntaxInfo INSTANCE = new EditorJsSyntaxInfo();

    // Block types with descriptions and examples
    private Map<String, BlockTypeInfo> blockTypes = new HashMap<>();

    // Inline tools with descriptions and shortcuts
    private Map<String, InlineToolInfo> inlineTools = new HashMap<>();

    // Available plugins and their features
    private List<PluginInfo> plugins = new ArrayList<>();

    // Supported output formats
    private List<String> outputFormats = new ArrayList<>();

    /**
     * Private constructor - initialize syntax info from CloudEmpiere setup
     */
    private EditorJsSyntaxInfo() {
        initializeBlockTypes();
        initializeInlineTools();
        initializePlugins();
        initializeOutputFormats();
    }

    /**
     * Get singleton instance of editor.js syntax info
     *
     * @return EditorJsSyntaxInfo instance
     */
    public static EditorJsSyntaxInfo getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize all supported block types in CloudEmpiere editor.js
     */
    private void initializeBlockTypes() {
        // Paragraph block
        blockTypes.put("paragraph", new BlockTypeInfo(
            "paragraph",
            "Paragraph text",
            "Basic text content block",
            new String[]{"text"}
        ));

        // Heading blocks (H2-H5)
        blockTypes.put("heading", new BlockTypeInfo(
            "heading",
            "Heading (H2-H5)",
            "Hierarchical heading with level 2-5",
            new String[]{"text", "level"}
        ));

        // Lists
        blockTypes.put("list", new BlockTypeInfo(
            "list",
            "Unordered/Ordered List",
            "Bullet points or numbered list",
            new String[]{"items", "style"}
        ));

        // Code block
        blockTypes.put("code", new BlockTypeInfo(
            "code",
            "Code Block",
            "Syntax-highlighted code with language specification",
            new String[]{"code", "language"}
        ));

        // Quote/Blockquote
        blockTypes.put("quote", new BlockTypeInfo(
            "quote",
            "Quote/Blockquote",
            "Quoted text with optional attribution",
            new String[]{"text", "caption"}
        ));

        // Image block
        blockTypes.put("image", new BlockTypeInfo(
            "image",
            "Image",
            "Embedded image with caption",
            new String[]{"url", "caption"}
        ));

        // Table block
        blockTypes.put("table", new BlockTypeInfo(
            "table",
            "Table",
            "Data table with header row",
            new String[]{"content"}
        ));

        // Embed block (video, iframe)
        blockTypes.put("embed", new BlockTypeInfo(
            "embed",
            "Embed (Video/Iframe)",
            "Embedded video, iframe, or external content",
            new String[]{"service", "embed", "url", "width", "height"}
        ));

        // Alert/Warning block
        blockTypes.put("alert", new BlockTypeInfo(
            "alert",
            "Alert Block",
            "Styled alert with type (success, warning, danger, info)",
            new String[]{"type", "title", "message"}
        ));

        // Columns block (custom plugin)
        blockTypes.put("columns", new BlockTypeInfo(
            "columns",
            "Columns Layout",
            "Multi-column layout for content organization",
            new String[]{"cols"}
        ));

        // BPMN diagram block (custom plugin)
        blockTypes.put("bpmn", new BlockTypeInfo(
            "bpmn",
            "BPMN Diagram",
            "Business process diagram with bpmn-js",
            new String[]{"xml", "width", "height"}
        ));

        // Mermaid diagram block
        blockTypes.put("mermaid", new BlockTypeInfo(
            "mermaid",
            "Mermaid Diagram",
            "Flowchart, sequence, or Gantt diagram",
            new String[]{"code"}
        ));

        // Excerpt block (custom plugin)
        blockTypes.put("excerpt", new BlockTypeInfo(
            "excerpt",
            "Excerpt",
            "Highlighted excerpt with customizable styling",
            new String[]{"text", "style"}
        ));

        // Raw HTML block
        blockTypes.put("raw", new BlockTypeInfo(
            "raw",
            "Raw HTML",
            "Raw HTML content (sanitized)",
            new String[]{"html"}
        ));

        // File/Attachment block
        blockTypes.put("attaches", new BlockTypeInfo(
            "attaches",
            "File Attachments",
            "Downloadable file attachments",
            new String[]{"files"}
        ));

        // Delimiter/Horizontal rule
        blockTypes.put("delimiter", new BlockTypeInfo(
            "delimiter",
            "Horizontal Rule",
            "Visual separator between content",
            new String[]{}
        ));
    }

    /**
     * Initialize all supported inline tools in CloudEmpiere editor.js
     */
    private void initializeInlineTools() {
        inlineTools.put("bold", new InlineToolInfo(
            "bold",
            "Bold Text",
            "**text** or Ctrl+B",
            "**"
        ));

        inlineTools.put("italic", new InlineToolInfo(
            "italic",
            "Italic Text",
            "_text_ or Ctrl+I",
            "_"
        ));

        inlineTools.put("underline", new InlineToolInfo(
            "underline",
            "Underline Text",
            "^text^ or Ctrl+U",
            "^"
        ));

        inlineTools.put("code", new InlineToolInfo(
            "code",
            "Inline Code",
            "`code` or Ctrl+`",
            "`"
        ));

        inlineTools.put("marker", new InlineToolInfo(
            "marker",
            "Text Highlighting",
            "Mark/highlight text color",
            "=="
        ));

        inlineTools.put("link", new InlineToolInfo(
            "link",
            "Hyperlink",
            "[text](url) or Ctrl+L",
            "[]()"
        ));

        inlineTools.put("comment", new InlineToolInfo(
            "comment",
            "Inline Comment",
            "Add comments to inline text",
            "{comment}"
        ));

        inlineTools.put("changeCase", new InlineToolInfo(
            "changeCase",
            "Change Case",
            "Toggle UPPERCASE, lowercase, Title Case",
            "aA"
        ));
    }

    /**
     * Initialize all supported plugins in CloudEmpiere editor.js
     */
    private void initializePlugins() {
        // Official Editor.js plugins
        plugins.add(new PluginInfo(
            "@editorjs/paragraph",
            "Paragraph Tool",
            "official",
            "Basic paragraph text blocks"
        ));

        plugins.add(new PluginInfo(
            "@editorjs/header",
            "Header Tool",
            "official",
            "H2-H5 heading blocks with level configuration"
        ));

        plugins.add(new PluginInfo(
            "@editorjs/list",
            "List Tool",
            "official",
            "Ordered and unordered lists with nesting"
        ));

        plugins.add(new PluginInfo(
            "@editorjs/code",
            "Code Tool",
            "official",
            "Code blocks with syntax highlighting support"
        ));

        plugins.add(new PluginInfo(
            "@editorjs/quote",
            "Quote Tool",
            "official",
            "Blockquote with optional attribution"
        ));

        plugins.add(new PluginInfo(
            "@editorjs/image",
            "Image Tool",
            "official",
            "Image embedding with caption support"
        ));

        plugins.add(new PluginInfo(
            "@editorjs/table",
            "Table Tool",
            "official",
            "Data tables with header and cell editing"
        ));

        plugins.add(new PluginInfo(
            "@editorjs/embed",
            "Embed Tool",
            "official",
            "Iframe/video embedding from various services"
        ));

        // Third-party plugins
        plugins.add(new PluginInfo(
            "editorjs-alert",
            "Alert Block",
            "third-party",
            "Styled alert boxes with success/warning/danger/info types"
        ));

        plugins.add(new PluginInfo(
            "editorjs-columns",
            "Columns Block",
            "third-party",
            "Multi-column layout for content arrangement"
        ));

        plugins.add(new PluginInfo(
            "editorjs-bpmn",
            "BPMN Diagram",
            "custom",
            "Business process diagram editor (bpmn-js integration)"
        ));

        plugins.add(new PluginInfo(
            "editorjs-mermaid",
            "Mermaid Diagrams",
            "third-party",
            "Flowchart, sequence diagram, Gantt chart support"
        ));

        plugins.add(new PluginInfo(
            "editorjs-excerpt",
            "Excerpt Tool",
            "custom",
            "Highlighted excerpt blocks with style options"
        ));

        plugins.add(new PluginInfo(
            "editorjs-hyperlink",
            "Hyperlink Tool",
            "custom",
            "Enhanced link editing with URL validation"
        ));

        plugins.add(new PluginInfo(
            "editorjs-changeCase",
            "Change Case Tool",
            "custom",
            "Convert text case (uppercase, lowercase, title case)"
        ));

        plugins.add(new PluginInfo(
            "editorjs-raw",
            "Raw HTML Tool",
            "third-party",
            "Raw HTML content (sanitized before storage)"
        ));

        plugins.add(new PluginInfo(
            "editorjs-attaches",
            "Attaches Tool",
            "official",
            "File attachment/download blocks"
        ));
    }

    /**
     * Initialize supported output formats
     */
    private void initializeOutputFormats() {
        outputFormats.add("json");           // Editor.js JSON format
        outputFormats.add("markdown");       // GitHub Flavored Markdown
        outputFormats.add("html");           // HTML (via edjsHTML converter)
        outputFormats.add("plaintext");      // Plain text extraction
    }

    /**
     * Get all block types
     *
     * @return map of block type names to BlockTypeInfo
     */
    public Map<String, BlockTypeInfo> getBlockTypes() {
        return new HashMap<>(blockTypes);
    }

    /**
     * Get specific block type info
     *
     * @param blockType type name (e.g., "paragraph", "heading")
     * @return BlockTypeInfo or null if not found
     */
    public BlockTypeInfo getBlockType(String blockType) {
        return blockTypes.get(blockType);
    }

    /**
     * Check if a block type is supported
     *
     * @param blockType type name
     * @return true if block type is supported
     */
    public boolean supportsBlockType(String blockType) {
        return blockTypes.containsKey(blockType);
    }

    /**
     * Get all inline tools
     *
     * @return map of tool names to InlineToolInfo
     */
    public Map<String, InlineToolInfo> getInlineTools() {
        return new HashMap<>(inlineTools);
    }

    /**
     * Get specific inline tool info
     *
     * @param toolName tool name (e.g., "bold", "link")
     * @return InlineToolInfo or null if not found
     */
    public InlineToolInfo getInlineTool(String toolName) {
        return inlineTools.get(toolName);
    }

    /**
     * Check if an inline tool is supported
     *
     * @param toolName tool name
     * @return true if tool is supported
     */
    public boolean supportsInlineTool(String toolName) {
        return inlineTools.containsKey(toolName);
    }

    /**
     * Get all plugins
     *
     * @return list of available plugins
     */
    public List<PluginInfo> getPlugins() {
        return new ArrayList<>(plugins);
    }

    /**
     * Get plugins by type (official, third-party, custom)
     *
     * @param type plugin type
     * @return list of plugins matching type
     */
    public List<PluginInfo> getPluginsByType(String type) {
        List<PluginInfo> result = new ArrayList<>();
        for (PluginInfo plugin : plugins) {
            if (type.equals(plugin.type)) {
                result.add(plugin);
            }
        }
        return result;
    }

    /**
     * Get supported output formats
     *
     * @return list of output format names
     */
    public List<String> getOutputFormats() {
        return new ArrayList<>(outputFormats);
    }

    /**
     * Check if output format is supported
     *
     * @param format format name
     * @return true if format is supported
     */
    public boolean supportsOutputFormat(String format) {
        return outputFormats.contains(format);
    }

    /**
     * Get summary of all syntax capabilities as text
     *
     * @return formatted string describing all capabilities
     */
    public String getSyntaxSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Editor.js Syntax Capabilities:\n");
        summary.append("===============================\n\n");

        summary.append("Block Types (").append(blockTypes.size()).append("):\n");
        for (String type : blockTypes.keySet()) {
            BlockTypeInfo info = blockTypes.get(type);
            summary.append("  - ").append(info.displayName)
                .append(": ").append(info.description).append("\n");
        }

        summary.append("\nInline Tools (").append(inlineTools.size()).append("):\n");
        for (String tool : inlineTools.keySet()) {
            InlineToolInfo info = inlineTools.get(tool);
            summary.append("  - ").append(info.displayName)
                .append(" (").append(info.syntax).append(")\n");
        }

        summary.append("\nPlugins (").append(plugins.size()).append("):\n");
        for (PluginInfo plugin : plugins) {
            summary.append("  - ").append(plugin.name)
                .append(" [").append(plugin.type).append("]: ")
                .append(plugin.description).append("\n");
        }

        summary.append("\nOutput Formats: ").append(String.join(", ", outputFormats)).append("\n");

        return summary.toString();
    }

    /**
     * Inner class for block type information
     */
    public static class BlockTypeInfo {
        public String type;
        public String displayName;
        public String description;
        public String[] fields;

        public BlockTypeInfo(String type, String displayName, String description, String[] fields) {
            this.type = type;
            this.displayName = displayName;
            this.description = description;
            this.fields = fields;
        }

        @Override
        public String toString() {
            return displayName + " (" + type + "): " + description;
        }
    }

    /**
     * Inner class for inline tool information
     */
    public static class InlineToolInfo {
        public String name;
        public String displayName;
        public String description;
        public String syntax;

        public InlineToolInfo(String name, String displayName, String description, String syntax) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.syntax = syntax;
        }

        @Override
        public String toString() {
            return displayName + " (" + name + "): " + description;
        }
    }

    /**
     * Inner class for plugin information
     */
    public static class PluginInfo {
        public String packageName;
        public String name;
        public String type;          // "official", "third-party", or "custom"
        public String description;

        public PluginInfo(String packageName, String name, String type, String description) {
            this.packageName = packageName;
            this.name = name;
            this.type = type;
            this.description = description;
        }

        @Override
        public String toString() {
            return name + " (" + type + "): " + description;
        }
    }

    /**
     * Get total capabilities count
     *
     * @return total number of blocks + tools + plugins
     */
    public int getTotalCapabilities() {
        return blockTypes.size() + inlineTools.size() + plugins.size();
    }
}
