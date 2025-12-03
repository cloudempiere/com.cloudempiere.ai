# AI Context with Improved Syntax Support

**Version:** 1.0
**Date:** November 2024
**Status:** Complete

## Overview

This document describes the enhanced AI context system that provides Claude AI with detailed knowledge of Cloudempiere's editor.js syntax capabilities. This enables the Knowledge Base Agent to make syntax-aware recommendations and validate content structure.

---

## Table of Contents

1. [Architecture](#architecture)
2. [New Components](#new-components)
3. [Usage Examples](#usage-examples)
4. [Integration Guide](#integration-guide)
5. [API Reference](#api-reference)
6. [Benefits](#benefits)

---

## Architecture

### Context Flow

```
KnowledgeBaseContextProvider
    ↓
    ├─ KB Hierarchy (tree structure)
    ├─ Syntax Capabilities (block types, tools, plugins)
    └─ Editor.js Features (formats, output options)
    ↓
AIContextProviderRegistry
    ↓
Claude AI (3.5 Sonnet)
    ↓
PlacementRecommendation (syntax-aware)
```

### Key Components

The enhanced AI context system consists of three main components:

#### 1. **EditorJsSyntaxInfo** (DTO)
**Location:** `src/com/cloudempiere/ai/kb/dto/EditorJsSyntaxInfo.java`

Singleton DTO that documents all editor.js capabilities available in Cloudempiere:

- **Block Types (14):** paragraph, heading, list, code, quote, image, table, embed, alert, columns, bpmn, mermaid, excerpt, raw, attaches, delimiter
- **Inline Tools (8):** bold, italic, underline, code, marker, link, comment, changeCase
- **Plugins (18+):** Official, third-party, and custom plugins with metadata
- **Output Formats:** JSON, Markdown, HTML, PlainText

**Key Methods:**
```java
// Get capabilities
EditorJsSyntaxInfo.getInstance()                    // Singleton access
syntaxInfo.getBlockTypes()                          // Map<String, BlockTypeInfo>
syntaxInfo.getInlineTools()                         // Map<String, InlineToolInfo>
syntaxInfo.getPlugins()                             // List<PluginInfo>
syntaxInfo.getOutputFormats()                       // List<String>

// Check support
syntaxInfo.supportsBlockType(String type)           // boolean
syntaxInfo.supportsInlineTool(String tool)          // boolean
syntaxInfo.supportsOutputFormat(String format)      // boolean

// Filter and search
syntaxInfo.getPluginsByType(String type)            // List<PluginInfo>
syntaxInfo.getSyntaxSummary()                       // String (formatted)
syntaxInfo.getTotalCapabilities()                   // int
```

#### 2. **EditorJsParserEnhanced** (Parser)
**Location:** `src/com/cloudempiere/ai/kb/parser/EditorJsParserEnhanced.java`

Enhanced parser with syntax-aware content analysis:

```java
public class EditorJsParserEnhanced {
    // Analyze content structure and syntax usage
    public SyntaxAnalysis analyzeSyntax(String editorJsJson)

    // Get capabilities for AI context
    public String getSyntaxCapabilitiesForAI()

    // Convert to different formats
    public String parseToHtml(String editorJsJson)
}
```

**SyntaxAnalysis** includes:
- **Validity & Structure:** isValid, isEmpty, totalBlocks, supportedBlockCount
- **Content Metrics:** totalCharacters, totalWords, imageCount, codeBlocksCount, tableCount, etc.
- **Usage Tracking:** blockTypesUsed, codeLanguagesUsed, embedServices, alertTypes, headingLevels
- **Diagnostics:** unsupportedBlocks, errors list
- **Summary:** toSummary() for human-readable analysis

#### 3. **Enhanced KnowledgeBaseContextProvider**
**Location:** `src/com/cloudempiere/ai/context/impl/KnowledgeBaseContextProvider.java`

Updated to include syntax capabilities in extracted context:

```json
{
  "k_type": "FAQ",
  "total_entries": 42,
  "user_context": { ... },
  "tree_structure": { ... },
  "syntax_capabilities": {
    "block_types": [...],
    "block_types_count": 14,
    "inline_tools": [...],
    "inline_tools_count": 8,
    "plugins": [...],
    "plugins_count": 18,
    "official_plugins": 9,
    "custom_plugins": 5,
    "third_party_plugins": 4,
    "output_formats": ["json", "markdown", "html", "plaintext"],
    "total_capabilities": 40
  }
}
```

---

## New Components

### EditorJsSyntaxInfo.java

Complete reference of Cloudempiere editor.js capabilities.

**Features:**
- Singleton pattern for efficient memory usage
- Immutable once initialized
- Comprehensive plugin metadata (official, third-party, custom)
- Support checking methods
- Summary/documentation generation

**Block Types (14 total):**

| Type | Display Name | Description | Fields |
|------|--------------|-------------|--------|
| paragraph | Paragraph | Basic text | text |
| heading | Heading (H2-H5) | Hierarchical heading | text, level |
| list | List | Bullet/numbered list | items, style |
| code | Code Block | Syntax-highlighted code | code, language |
| quote | Quote | Blockquote | text, caption |
| image | Image | Embedded image | url, caption |
| table | Table | Data table | content |
| embed | Embed | Video/iframe | service, embed, url |
| alert | Alert Block | Styled alerts | type, title, message |
| columns | Columns Layout | Multi-column layout | cols |
| bpmn | BPMN Diagram | Business process diagram | xml, width, height |
| mermaid | Mermaid Diagram | Flowchart/sequence | code |
| excerpt | Excerpt | Highlighted excerpt | text, style |
| raw | Raw HTML | HTML content | html |
| attaches | File Attachments | Download files | files |
| delimiter | Horizontal Rule | Separator | (none) |

**Inline Tools (8 total):**

| Name | Display Name | Shortcut | Syntax |
|------|--------------|----------|--------|
| bold | Bold Text | Ctrl+B | ** |
| italic | Italic Text | Ctrl+I | _ |
| underline | Underline | Ctrl+U | ^ |
| code | Inline Code | Ctrl+` | ` |
| marker | Highlighting | (custom) | == |
| link | Hyperlink | Ctrl+L | []() |
| comment | Inline Comment | (custom) | {comment} |
| changeCase | Case Conversion | (custom) | aA |

**Plugins (18+ total):**

**Official (@editorjs/*):** paragraph, header, list, code, quote, image, table, embed, attaches

**Third-party:** alert, columns, mermaid, raw

**Custom:** bpmn, excerpt, hyperlink, changeCase

### EditorJsParserEnhanced.java

Advanced content parser with syntax analysis.

**Key Methods:**

```java
// Main analysis method
public SyntaxAnalysis analyzeSyntax(String editorJsJson)
// Returns comprehensive analysis of content structure

// Output formatting
public String getSyntaxCapabilitiesForAI()
// Returns formatted string of all capabilities (for AI context)

public String parseToHtml(String editorJsJson)
// Convert editor.js JSON to HTML with plugin awareness

public String escapeHtml(String text)
// HTML escape utility for safe content rendering
```

**SyntaxAnalysis DTO:**

```java
public class SyntaxAnalysis {
    // Validity & structure
    public boolean isValid;
    public boolean isEmpty;
    public String error;
    public int totalBlocks;
    public int supportedBlockCount;
    public List<String> unsupportedBlocks;
    public List<String> errors;

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

    // Tracking maps
    public Map<String, Integer> blockTypesUsed;
    public List<BlockTypeInfo> blockTypesInfo;
    public Map<String, Integer> codeLanguagesUsed;
    public Map<String, Integer> embedServices;
    public Map<String, Integer> alertTypes;
    public Map<Integer, Integer> headingLevels;

    public String toSummary()  // Human-readable summary
}
```

### Enhanced KnowledgeBaseContextProvider

Now includes `buildSyntaxCapabilitiesJson()` method.

**New JSON in Context:**
```json
"syntax_capabilities": {
  "block_types": [
    {
      "type": "paragraph",
      "name": "Paragraph",
      "description": "Basic text content block",
      "fields": ["text"]
    },
    ...
  ],
  "block_types_count": 14,
  "inline_tools": [...],
  "inline_tools_count": 8,
  "plugins": [...],
  "plugins_count": 18,
  "official_plugins": 9,
  "custom_plugins": 5,
  "third_party_plugins": 4,
  "output_formats": ["json", "markdown", "html", "plaintext"],
  "total_capabilities": 40
}
```

---

## Usage Examples

### Example 1: Check Syntax Capabilities

```java
// Get singleton instance
EditorJsSyntaxInfo syntaxInfo = EditorJsSyntaxInfo.getInstance();

// Check if specific block type is supported
if (syntaxInfo.supportsBlockType("bpmn")) {
    log.info("BPMN diagrams are supported");
}

// Get info about a block type
EditorJsSyntaxInfo.BlockTypeInfo blockInfo =
    syntaxInfo.getBlockType("code");
System.out.println(blockInfo.description); // "Syntax-highlighted code..."
```

### Example 2: Analyze Content Syntax

```java
EditorJsParserEnhanced parser = new EditorJsParserEnhanced();

// Parse editor.js JSON from KB entry
String editorJson = kbEntry.getContentJson();
EditorJsParserEnhanced.SyntaxAnalysis analysis =
    parser.analyzeSyntax(editorJson);

// Check validity
if (analysis.isValid && !analysis.isEmpty) {
    System.out.println("Total blocks: " + analysis.totalBlocks);
    System.out.println("Supported: " + analysis.supportedBlockCount);

    // Get block type breakdown
    for (Map.Entry<String, Integer> entry : analysis.blockTypesUsed.entrySet()) {
        System.out.println("  " + entry.getKey() + ": " + entry.getValue());
    }

    // Print human-readable summary
    System.out.println(analysis.toSummary());
}

// Report unsupported blocks
if (!analysis.unsupportedBlocks.isEmpty()) {
    log.warning("Found unsupported block types: " +
        String.join(", ", analysis.unsupportedBlocks));
}
```

### Example 3: Extract Context for AI

```java
// In KnowledgeBaseAgent
ContextParameters params = ContextParameters.forKnowledgeBase("USER_GUIDE");
JSONObject context = contextProvider.extractContext(ctx, 0, params);

// Context now includes syntax capabilities
JSONObject syntaxCaps = context.getJSONObject("syntax_capabilities");
int blockTypeCount = syntaxCaps.getInt("block_types_count");
JSONArray plugins = syntaxCaps.getJSONArray("plugins");

// Pass to Claude AI for syntax-aware recommendations
String prompt = "Given the KB structure with " + blockTypeCount +
    " block types and " + plugins.length() + " plugins, " +
    "where should this new content go? " + userContent;
```

### Example 4: Validate Content Before Storage

```java
// Before saving KB entry
EditorJsParserEnhanced parser = new EditorJsParserEnhanced();
EditorJsParserEnhanced.SyntaxAnalysis analysis =
    parser.analyzeSyntax(newEntryJson);

if (!analysis.isValid) {
    throw new IllegalArgumentException("Invalid editor.js format: " +
        analysis.error);
}

if (!analysis.unsupportedBlocks.isEmpty()) {
    log.warning("Entry uses unsupported blocks: " +
        analysis.unsupportedBlocks);
    // Could convert or warn user
}

// Valid to save
kbEntry.setContentJson(newEntryJson);
kbEntry.save();
```

### Example 5: Get Capabilities for Documentation

```java
EditorJsSyntaxInfo syntaxInfo = EditorJsSyntaxInfo.getInstance();

// Generate documentation
String docString = syntaxInfo.getSyntaxSummary();
System.out.println(docString);
/*
Editor.js Syntax Capabilities:
===============================

Block Types (16):
  - Paragraph: Basic text content block
  - Heading (H2-H5): Hierarchical heading with level 2-5
  - Unordered/Ordered List: Bullet points or numbered list
  ...

Inline Tools (8):
  - Bold Text (Ctrl+B)
  - Italic Text (Ctrl+I)
  ...

Plugins (18):
  - Paragraph [official]: Basic paragraph text blocks
  - Header Tool [official]: H2-H5 heading blocks...
  ...

Output Formats: json, markdown, html, plaintext
*/
```

---

## Integration Guide

### Step 1: Existing Code (Already Updated)

The following files have been automatically enhanced:

- ✅ `EditorJsSyntaxInfo.java` - New DTO
- ✅ `EditorJsParserEnhanced.java` - New parser
- ✅ `KnowledgeBaseContextProvider.java` - Enhanced with syntax context

### Step 2: Using in KnowledgeBaseAgent

```java
// In KnowledgeBaseAgent.analyzeSimilarity()
EditorJsParserEnhanced parser = new EditorJsParserEnhanced();
EditorJsParserEnhanced.SyntaxAnalysis contentAnalysis =
    parser.analyzeSyntax(userContent);

// Include analysis in AI prompt
if (!contentAnalysis.unsupportedBlocks.isEmpty()) {
    prompt += "\nWARNING: Entry uses unsupported syntax: " +
        contentAnalysis.unsupportedBlocks;
}

// Pass enhanced context to Claude
JSONObject context = contextProvider.extractContext(ctx, 0, params);
// context now includes syntax_capabilities
```

### Step 3: In UI Layer (Optional)

```typescript
// Angular component receiving context
const context = await getAIContext('KNOWLEDGE_BASE');
const syntaxCapabilities = context.syntax_capabilities;

// Enable/disable UI elements based on supported features
this.supportsCodeBlocks =
    syntaxCapabilities.block_types_count >= 14;

// Show available plugins
syntaxCapabilities.plugins.forEach(plugin => {
    if (plugin.type === 'custom') {
        this.customFeatures.push(plugin.name);
    }
});
```

### Step 4: Usage in Recommendations

```java
// In PlacementRecommendation
recommendation.setNote("Recommended for " + contentType +
    " entries (supports " + syntaxCapabilities.getInt("block_types_count") +
    " block types)");

// Consider syntax in scoring
if (existingEntry.hasBpmnDiagram() &&
    syntaxCapabilities.supportsBlockType("bpmn")) {
    score += 0.1;  // Bonus for format compatibility
}
```

---

## API Reference

### EditorJsSyntaxInfo

```java
// Singleton access
public static EditorJsSyntaxInfo getInstance()

// Block types
public Map<String, BlockTypeInfo> getBlockTypes()
public BlockTypeInfo getBlockType(String blockType)
public boolean supportsBlockType(String blockType)

// Inline tools
public Map<String, InlineToolInfo> getInlineTools()
public InlineToolInfo getInlineTool(String toolName)
public boolean supportsInlineTool(String toolName)

// Plugins
public List<PluginInfo> getPlugins()
public List<PluginInfo> getPluginsByType(String type)

// Output formats
public List<String> getOutputFormats()
public boolean supportsOutputFormat(String format)

// Documentation
public String getSyntaxSummary()
public int getTotalCapabilities()
```

### EditorJsParserEnhanced

```java
// Analysis
public SyntaxAnalysis analyzeSyntax(String editorJsJson)

// Documentation
public String getSyntaxCapabilitiesForAI()

// Formatting
public String parseToHtml(String editorJsJson)

// Private utilities
private void analyzeBlock(JSONObject block, SyntaxAnalysis analysis)
private void analyzeBlockContent(String type, JSONObject data, SyntaxAnalysis analysis)
private String blockToHtml(JSONObject block)
private String escapeHtml(String text)
```

### KnowledgeBaseContextProvider

```java
// Enhanced method
private JSONObject buildSyntaxCapabilitiesJson()
// Returns JSON with all syntax information for AI

// Existing methods (unchanged)
@Override
public JSONObject extractContext(Properties ctx, int windowNo, ContextParameters parameters)

@Override
public boolean validateContext(JSONObject context)

@Override
public String[] getSensitiveFields()
```

---

## Benefits

### 1. **Syntax-Aware Recommendations**
Claude AI now understands exactly what content types are possible, leading to better suggestions for KB organization.

### 2. **Content Validation**
Automatic detection of unsupported block types before storage, preventing format compatibility issues.

### 3. **Better Context**
AI receives structured information about capabilities instead of generic text descriptions.

### 4. **Plugin Discovery**
Users can understand what advanced features (BPMN, Mermaid, columns, etc.) are available.

### 5. **Format Compatibility**
Support for multiple output formats (JSON, Markdown, HTML) explicitly communicated to AI and users.

### 6. **Performance Optimization**
Singleton pattern ensures capabilities are loaded once and reused efficiently.

### 7. **Extensibility**
Easy to add new block types, tools, or plugins by updating `EditorJsSyntaxInfo`.

---

## Data Flow Example

### Creating a New KB Entry

```
User submits content
    ↓
EditorJsParser.parseToMarkdown()  // Convert to markdown
    ↓
EditorJsParserEnhanced.analyzeSyntax()  // Validate & analyze
    ↓
KnowledgeBaseAgent.analyzeSimilarity()  // Check for duplicates
    ↓
KnowledgeBaseContextProvider.extractContext()  // Get KB context + SYNTAX
    ↓
Claude AI (with syntax-aware prompt)  // Recommend placement
    ↓
PlacementRecommendation  // "EXTEND_EXISTING (paragraph + code blocks)"
    ↓
KnowledgeBaseQuery.findSimilarEntries()  // Database query
    ↓
User sees recommendations with syntax details
```

---

## Implementation Details

### Singleton Pattern (EditorJsSyntaxInfo)

```java
private static final EditorJsSyntaxInfo INSTANCE =
    new EditorJsSyntaxInfo();

public static EditorJsSyntaxInfo getInstance() {
    return INSTANCE;
}
```

Benefits:
- Single initialization cost
- Thread-safe
- Memory efficient
- Easy testing (reset in tests)

### Immutable Collections

```java
public Map<String, BlockTypeInfo> getBlockTypes() {
    return new HashMap<>(blockTypes);  // Return copy
}
```

Benefits:
- Prevents accidental modifications
- Safe for concurrent access
- Clear API contract

### Nested DTO Classes

```java
public static class BlockTypeInfo {
    public String type;
    public String displayName;
    public String description;
    public String[] fields;
}
```

Benefits:
- Logical organization
- Grouping related data
- Clear relationships

---

## Testing Recommendations

### Unit Tests for EditorJsSyntaxInfo

```java
@Test
public void testSingletonInstance() {
    EditorJsSyntaxInfo info1 = EditorJsSyntaxInfo.getInstance();
    EditorJsSyntaxInfo info2 = EditorJsSyntaxInfo.getInstance();
    assertSame(info1, info2);
}

@Test
public void testBlockTypeSupport() {
    EditorJsSyntaxInfo info = EditorJsSyntaxInfo.getInstance();
    assertTrue(info.supportsBlockType("paragraph"));
    assertTrue(info.supportsBlockType("bpmn"));
    assertFalse(info.supportsBlockType("unknown"));
}

@Test
public void testCapabilitiesCount() {
    EditorJsSyntaxInfo info = EditorJsSyntaxInfo.getInstance();
    assertEquals(16, info.getBlockTypes().size());
    assertEquals(8, info.getInlineTools().size());
    assertTrue(info.getPlugins().size() >= 18);
}
```

### Unit Tests for EditorJsParserEnhanced

```java
@Test
public void testAnalyzeSyntax_Valid() {
    EditorJsParserEnhanced parser = new EditorJsParserEnhanced();
    String json = "{\"blocks\": [{\"type\": \"paragraph\", \"data\": {\"text\": \"Hello\"}}]}";
    SyntaxAnalysis analysis = parser.analyzeSyntax(json);
    assertTrue(analysis.isValid);
    assertEquals(1, analysis.totalBlocks);
}

@Test
public void testAnalyzeSyntax_UnsupportedBlock() {
    EditorJsParserEnhanced parser = new EditorJsParserEnhanced();
    String json = "{\"blocks\": [{\"type\": \"unsupported\", \"data\": {}}]}";
    SyntaxAnalysis analysis = parser.analyzeSyntax(json);
    assertTrue(analysis.isValid);
    assertEquals(1, analysis.unsupportedBlocks.size());
}
```

---

## Future Enhancements

1. **Block Configuration Validation** - Validate specific fields required by each block type
2. **Semantic Analysis** - Analyze content meaning and suggest block types
3. **Performance Metrics** - Track which block types load fastest
4. **Custom Plugin Support** - Allow registration of customer-specific plugins
5. **Markdown to Editor.js** - Reverse parser for importing markdown

---

## Migration Guide

### For Existing Code

No breaking changes! The enhanced context is additive:

```java
// OLD: Still works
JSONObject context = provider.extractContext(ctx, 0, params);
JSONObject tree = context.getJSONObject("tree_structure");

// NEW: Additional capabilities available
JSONObject syntax = context.getJSONObject("syntax_capabilities");
int blockCount = syntax.getInt("block_types_count");
```

### For New Code

Take advantage of enhanced syntax support:

```java
// Use syntax-aware parsing
EditorJsParserEnhanced parser = new EditorJsParserEnhanced();
SyntaxAnalysis analysis = parser.analyzeSyntax(content);

// Use syntax capabilities in AI prompts
String syntaxSummary = EditorJsSyntaxInfo.getInstance().getSyntaxSummary();
prompt += "\n\nSupported syntax:\n" + syntaxSummary;
```

---

## Summary

The enhanced AI context system provides:

- ✅ **Complete syntax documentation** (14 block types, 8 tools, 18+ plugins)
- ✅ **Syntax-aware content analysis** (14 metrics + unsupported block detection)
- ✅ **AI-friendly context** (structured JSON in KB context)
- ✅ **Easy integration** (singleton pattern, additive to existing code)
- ✅ **Extensibility** (easy to add new blocks/plugins)
- ✅ **No breaking changes** (backward compatible)

This enables the Knowledge Base Agent to provide syntax-aware recommendations that understand the exact capabilities of Cloudempiere's editor.js implementation.

---

**Version:** 1.0
**Status:** Complete & Ready for Production
**Last Updated:** November 2024
