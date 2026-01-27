# Editor.js Cloudempiere Setup Guide

Complete guide to editor.js implementation in Cloudempiere, based on actual clde-angular-ecommerce production setup.

## Overview

Cloudempiere uses **Editor.js v2.30.8** as a block-based editor for rich content creation. The implementation includes:

- **14 block types** (paragraph, heading, list, table, image, code, alerts, BPMN, Mermaid, etc.)
- **8 inline tools** (bold, italic, underline, inline code, comments, hyperlinks, case conversion)
- **5 custom plugins** (copy/cut/paste tunes, excerpts, BPMN, change case, custom table)
- **12 custom HTML parsers** for rendering
- **Performance optimization** - ~800KB removed from initial bundle via lazy loading

---

## Architecture

```
┌─────────────────────────────────────┐
│    EditorJS Component (Angular)      │
│    1,271 lines, fully integrated    │
└────────┬────────────────────────────┘
         │
    ┌────┴────┬────────────┬─────────┐
    ▼         ▼            ▼         ▼
┌────────┐ ┌──────┐ ┌──────────┐ ┌──────────┐
│ Blocks │ │Tools │ │ Plugins  │ │Renderers │
│  (14)  │ │ (8)  │ │  (18+)   │ │ (12+)    │
└────────┘ └──────┘ └──────────┘ └──────────┘
    │         │          │            │
    └─────────┴──────────┴────────────┘
         │
    ┌────▼──────────────────┐
    │  Supported Formats    │
    ├──────────────────────┤
    │ • Editor.js JSON (BLK)│
    │ • Markdown (GFM)      │
    │ • HTML Output         │
    └──────────────────────┘
```

---

## Dependencies

### Core Library

```json
{
  "@editorjs/editorjs": "^2.30.8"
}
```

### Official Block Plugins (13)

```json
{
  "@editorjs/paragraph": "^2.11.7",
  "@editorjs/header": "^2.8.8",
  "@editorjs/list": "^2.0.6",
  "@editorjs/nested-list": "^1.4.3",
  "@editorjs/table": "^2.4.3",
  "@editorjs/image": "^2.10.2",
  "@editorjs/embed": "^2.7.6",
  "@editorjs/raw": "^2.5.1",
  "@editorjs/delimiter": "^1.4.2",
  "@editorjs/attaches": "^1.3.0",
  "@editorjs/link": "^2.6.2"
}
```

### Official Inline Tools (3)

```json
{
  "@editorjs/inline-code": "^1.5.1",
  "@editorjs/underline": "^1.2.1",
  "@editorjs/marker": "^1.4.0"
}
```

### Third-Party Plugins (13+)

```json
{
  "editorjs-alert": "^1.1.4",
  "editorjs-comment": "^1.0.3",
  "editorjs-mermaid": "^1.0.0",
  "editorjs-html": "^3.4.3",
  "editorjs-hyperlink": "^1.0.6",
  "editorjs-inline": "^4.1.5",
  "editorjs-drag-drop": "^1.1.16",
  "editorjs-multiblock-selection-plugin": "^0.1.1",
  "@calumk/editorjs-codecup": "^1.3.0",
  "@calumk/editorjs-columns": "^0.3.2",
  "bpmn-js": "^18.5.0",
  "bpmn-js-properties-panel": "^6.0.0-2",
  "mermaid": "^11.5.0"
}
```

---

## Block Types Configuration

### 1. Paragraph

```typescript
paragraph: {
  class: Paragraph,
  inlineToolbar: true,
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Features:**
- Inline toolbar for formatting
- Block operations (copy/cut/paste/duplicate)
- Auto-focus support

---

### 2. Header (H2-H5)

```typescript
header: {
  class: Header,
  config: {
    levels: [2, 3, 4, 5],
    defaultLevel: 2
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Features:**
- Heading levels 2-5
- Anchor ID auto-generation
- Copy-link icon for navigation

**Output Example:**
```html
<h2 id="my-header-anchor">
  My Header
  <a href="#my-header-anchor" class="copy-link"></a>
</h2>
```

---

### 3. List

```typescript
list: {
  class: EditorjsList,
  inlineToolbar: true,
  config: {
    defaultStyle: 'unordered'
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Features:**
- Unordered lists
- Ordered lists
- Checklist style
- Nested lists support

---

### 4. Table

```typescript
table: {
  class: CustomTable,  // Extended @editorjs/table
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Custom Features:**
- Double-click column headers to set width
- Support for px or % units
- Column resize dialog

---

### 5. Image

```typescript
image: {
  class: ImageTool,
  config: {
    uploader: {
      uploadByFile: (file: File) => store.postCDNEditorJS(file),
      uploadByUrl: (url: string) => store.postCDNEditorJSByURL(url)
    },
    features: { caption: 'optional' }
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Features:**
- File upload to CDN
- Caption support
- Gallery integration (Fancybox)
- Border/background styling options

---

### 6. Code

```typescript
code: {
  class: CodeCup,  // @calumk/editorjs-codecup
  config: {
    languages: ['javascript', 'python', 'java', 'go', 'typescript', 'css', 'html'],
    defaultLanguage: 'javascript'
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Features:**
- Syntax highlighting
- Language selection
- HTML escaping for security
- Line numbers (language-specific)

**Output:**
```html
<pre><code class="language-javascript">
  // Code here with HTML-escaped content
</code></pre>
```

---

### 7. Embed

```typescript
embed: {
  class: Embed,
  config: {
    services: {
      youtube: true,
      facebook: true
    }
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Supported:**
- YouTube videos
- Facebook posts
- Custom embed URLs

---

### 8. Alert

```typescript
alert: {
  class: AlertBlockPlugin,
  inlineToolbar: true,
  shortcut: 'CMD+SHIFT+A',
  config: {
    defaultType: 'info',
    messagePlaceholder: 'Enter alert message'
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Alert Types:**
- **Info** (ℹ️ blue)
- **Success** (✓ green)
- **Warning** (⚠️ yellow)
- **Danger** (✕ red)

**Output:**
```html
<div class="alert alert-info">
  <i class="fas fa-info-circle"></i>
  Alert message here
</div>
```

---

### 9. Columns

```typescript
columns: {
  class: Columns,
  config: {
    EditorJS: EditorJS,
    defaultColumns: 2,
    minColumns: 1,
    maxColumns: 4
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Features:**
- 1-4 columns
- Nested editor.js in each column
- Responsive layout

---

### 10. BPMN (Custom)

```typescript
bpmn: {
  class: BpmnPlugin,
  config: {
    services: {
      image: true,
      embed: true
    }
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Features:**
- Full BPMN diagram editor
- File upload support (.bpmn, .xml)
- Export to SVG
- Properties panel
- Fullscreen mode

**Data Structure:**
```json
{
  "type": "bpmn",
  "data": {
    "bpmnUrl": "https://cdn.../diagram.xml",
    "svgUrl": "https://cdn.../diagram.svg"
  }
}
```

---

### 11. Mermaid

```typescript
mermaid: {
  class: Mermaid,
  config: {
    mermaidUrl: 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js'
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Features:**
- Flowcharts
- Sequence diagrams
- Class diagrams
- State diagrams
- Gantt charts

**Example:**
```
graph TD
    A[Start] --> B{Decision}
    B -->|Yes| C[Process]
    B -->|No| D[End]
```

---

### 12. Excerpt (Custom)

```typescript
excerpt: {
  class: ExcerptTool,
  config: {
    maxLength: 100,
    placeholder: 'Add some excerpt...'
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Features:**
- Limited to 100 characters
- Displayed as `<p class="perex">`
- Preview for articles

---

### 13. Raw HTML

```typescript
raw: {
  class: RawTool,
  config: {
    placeholder: 'Enter raw HTML'
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Security:** Uses Angular DomSanitizer

---

### 14. Attaches (Files)

```typescript
attaches: {
  class: Attaches,
  config: {
    uploader: {
      uploadByFile: (file: File) => store.postCDNEditorJS(file)
    },
    extensions: ['jpg', 'png', 'pdf', 'doc', 'docx', 'zip'],
    allowMultiple: false
  },
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

---

### 15. Delimiter

```typescript
delimiter: {
  class: Delimiter,
  tunes: ['copyBlockTune', 'cutBlockTune', 'pasteBlockTune', 'duplicateBlockTune']
}
```

**Output:**
```html
<hr>
```

---

## Inline Tools

### 1. Bold

```typescript
bold: {
  class: createGenericInlineTool({
    name: 'bold',
    shortcut: 'CMD+B',
    button: { text: 'B' }
  }),
  shortcut: 'CMD+B'
}
```

---

### 2. Italic & Underline

```typescript
italic: { shortcut: 'CMD+I' },
underline: {
  class: Underline,
  shortcut: 'CMD+U'
}
```

---

### 3. Inline Code

```typescript
inlineCode: {
  class: InlineCode,
  shortcut: 'CMD+SHIFT+M'
}
```

**Output:** `<code>inline code</code>`

---

### 4. Marker (Highlight)

```typescript
marker: {
  class: Marker,
  shortcut: 'CMD+SHIFT+M'
}
```

**Output:** `<mark>highlighted text</mark>`

---

### 5. Hyperlink (Custom)

```typescript
hyperlink: {
  class: Hyperlink,
  config: {
    shortcut: 'CMD+L',
    searchEndpoint: (query) => store.searchHyperlink(query)
  }
}
```

**Features:**
- URL input with search
- Target attribute (_blank, _self, custom)
- Rel attribute (nofollow, noreferrer, author, etc.)
- URL validation with regex
- Protocol auto-detection

---

### 6. Comment

```typescript
comment: {
  class: CommentBlockPlugin,
  config: {
    renderBody: (comment) => commentRenderService.render(comment)
  }
}
```

**Features:**
- Inline commenting
- User mentions
- Custom rendering

---

### 7. Change Case (Custom)

```typescript
changecase: {
  class: ChangeCase,
  config: {
    options: [
      { name: 'Title Case', action: 'titleCase' },
      { name: 'UPPER CASE', action: 'uppercase' },
      { name: 'lower case', action: 'lowercase' },
      { name: 'Sentence case', action: 'sentencecase' },
      { name: 'tOGGLE cASE', action: 'togglecase' }
    ]
  }
}
```

---

## Data Formats

### 1. Editor.js Block JSON (BLK)

**Input Structure:**
```json
{
  "blocks": [
    {
      "id": "abc123",
      "type": "paragraph",
      "data": {
        "text": "Hello <b>world</b>!"
      }
    },
    {
      "id": "def456",
      "type": "header",
      "data": {
        "text": "My Title",
        "level": 2
      }
    },
    {
      "id": "ghi789",
      "type": "image",
      "data": {
        "file": { "url": "https://cdn.../image.jpg" },
        "caption": "Image caption",
        "withBorder": true,
        "stretched": false,
        "withBackground": false
      }
    }
  ]
}
```

---

### 2. GitHub Flavored Markdown (GFM)

**Automatic Conversion Features:**
- Markdown → Editor.js JSON
- Heading anchor ID generation
- Code block language detection
- Link parsing
- Image handling
- Table support

**Example Input:**
```markdown
# My Document

## Section 1

This is a paragraph with **bold** and *italic*.

- List item 1
- List item 2

```javascript
const code = "Hello World";
```

![Alt text](https://example.com/image.jpg)
```

---

### 3. HTML Output

**Custom Parsers** convert Editor.js JSON to HTML:

```html
<article>
  <h2 id="my-document">
    My Document
    <a href="#my-document" class="copy-link">🔗</a>
  </h2>

  <h3>Section 1</h3>
  <p>This is a paragraph with <strong>bold</strong> and <em>italic</em>.</p>

  <ul>
    <li>List item 1</li>
    <li>List item 2</li>
  </ul>

  <pre><code class="language-javascript">
const code = "Hello World";
  </code></pre>

  <figure>
    <img src="https://example.com/image.jpg" alt="Alt text">
  </figure>
</article>
```

---

## Custom Plugins Details

### 1. Copy/Cut/Paste/Duplicate Tunes

**File:** `plugins/copytunes.ts`

**Classes:**
- `CopyBlockTune` - Copy block to shared clipboard
- `CutBlockTune` - Cut block (copy + remove)
- `PasteBlockTune` - Paste from clipboard
- `DuplicateBlockTune` - Create identical copy

**Implementation:**
```typescript
class CopyBlockTune implements BlockTune {
  static get isTune() { return true; }

  save(blockData: BlockToolData): BlockTuneSaveData {
    SharedClipboard.copy(blockData);
    showNotification('Block copied to clipboard');
  }
}
```

---

### 2. BPMN Plugin

**File:** `plugins/bpmn/bpmn.ts`

**Features:**
- Full BPMN editor with bpmn-js
- Properties panel integration
- File upload support
- Export to BPMN XML
- Export to SVG image
- Fullscreen toggle

**Configuration:**
```typescript
class BpmnPlugin implements BlockTool {
  static get toolbox() {
    return { title: 'BPMN Diagram', icon: '📊' };
  }

  render() {
    // Create BPMN editor instance
    // Upload, edit, export functionality
  }

  save() {
    // Return { bpmnUrl, svgUrl }
  }
}
```

---

### 3. Change Case Inline Tool

**File:** `plugins/changecase/change-case.ts`

**Cases Supported:**
- **Title Case:** First Letter Of Each Word Capitalized
- **UPPER CASE:** ALL LETTERS CAPITALIZED
- **lower case:** all letters lowercase
- **Sentence case:** Only first letter capitalized
- **tOGGLE cASE:** Invert case
- **Locale aware:** Language-specific capitalization

---

### 4. Hyperlink Inline Tool

**File:** `plugins/hyperlink/hyperlink.ts`

**Features:**
- URL input with validation
- Target attribute selection
- Rel attribute selection
- Search/autocomplete integration
- Protocol auto-detection (http, https, //, #)
- Custom SearchResult interface

**Configuration:**
```typescript
class Hyperlink implements InlineTool {
  static get shortcut() { return 'CMD+L'; }

  surround(range: Range) {
    // Show URL input dialog
    // Search for existing links
    // Apply link with attributes
  }
}
```

---

## Performance Optimization

### Lazy Loading Strategy

**Bundle Reduction:**
- Main editor.js and plugins loaded dynamically
- UMD bundles cached for reuse
- edjsHTML module lazy-loaded on first use
- ~800KB removed from initial bundle

**Implementation:**
```typescript
const { default: EditorJS } = await import("editorjs.umd.js");
const { default: Paragraph } = await import("@editorjs/paragraph");
```

### Angular @defer

**Component Template:**
```html
@defer (on idle; prefetch on idle) {
  <div id="editorjs"></div>
} @placeholder {
  <div>Loading editor...</div>
}
```

---

## Integration with Cloudempiere

### Store Methods

**File Uploads:**
```typescript
store.postCDNEditorJS(file: File): Promise<{ success: 1, file: { url: string } }>
store.postCDNEditorJSByURL(url: string): Promise<{ success: 0 }>
```

**Search Integration:**
```typescript
store.searchHyperlink(query: string): Promise<SearchResult[]>
```

**Save Content:**
```typescript
store.updatedataToSave(entry: SKEntry): void
```

### Database Model

**Table:** `k_entry`
**Column:** `textmsg` (JSON)
**Edit Mode:** `k_entryeditmode.id = "BLK"`

---

## Advanced Features

### 1. Drag and Drop

- Move blocks up/down
- Drag between different editors
- Visual feedback during drag

### 2. Multi-block Selection

- Select multiple blocks
- Copy/cut/delete multiple blocks
- Bulk operations

### 3. Paste Handling

```typescript
onPaste(event: ClipboardEvent) {
  // Auto-embed images from paste
  // Parse pasted content format
  // Insert appropriate blocks
}
```

### 4. Comments System

- Inline comments on any block
- User mentions
- Custom rendering
- Persistence

### 5. Search Integration

- Hyperlink search with autocomplete
- Existing link detection
- URL validation

---

## Security Considerations

### HTML Sanitization

```typescript
const editor = new EditorJS({
  sanitizer: {
    table: true,
    thead: true,
    tbody: true,
    tr: true,
    td: true,
    th: true
  }
});
```

### Code Block Escaping

```typescript
// User input in code blocks is HTML-escaped
const safe = text.replace(/[&<>"]/g, char => ({
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;'
})[char]);
```

### Angular DomSanitizer

- Used for HTML output
- Prevents XSS attacks
- Whitelist-based approach

---

## Configuration Example

### Complete Editor Setup

```typescript
const editorConfig = {
  holder: "editorjs",
  autofocus: true,
  placeholder: "Let's write awesome content!",

  inlineToolbar: [
    'bold', 'italic', 'underline', 'inlineCode',
    'marker', 'hyperlink', 'comment', 'changecase'
  ],

  tools: {
    // Block tools configuration (14 types)
    paragraph: { class: Paragraph, inlineToolbar: true },
    header: { class: Header, config: { levels: [2,3,4,5] } },
    list: { class: EditorjsList, inlineToolbar: true },
    table: { class: CustomTable },
    image: { class: ImageTool, config: { uploader: {...} } },
    code: { class: CodeCup, config: { languages: [...] } },
    embed: { class: Embed, config: { services: {...} } },
    alert: { class: AlertBlockPlugin },
    columns: { class: Columns, config: {...} },
    bpmn: { class: BpmnPlugin },
    mermaid: { class: Mermaid },
    excerpt: { class: ExcerptTool },
    raw: { class: RawTool },
    attaches: { class: Attaches },
    delimiter: { class: Delimiter }
  },

  sanitizer: {
    table: true,
    thead: true,
    tbody: true,
    tr: true,
    td: true,
    th: true
  },

  data: initialData,

  onReady: () => console.log('Editor ready'),
  onChange: () => updatePreview(),
  onPaste: (event) => handlePaste(event)
};

const editor = new EditorJS(editorConfig);
```

---

## Testing & Development

### Development Workflow

1. **Local Setup:**
   ```bash
   npm install @editorjs/editorjs @editorjs/paragraph ...
   ```

2. **Component Integration:**
   ```typescript
   <ck-editor [inputData]="content"
              [inputDataType]="'BLK'"
              (outputEvent)="onEditorEvent($event)">
   </ck-editor>
   ```

3. **Save Content:**
   ```typescript
   const savedData = await editor.save();
   store.updatedataToSave({ textmsg: JSON.stringify(savedData) });
   ```

### Testing Examples

```typescript
// Test markdown to JSON conversion
const markdown = "# Title\n\nContent";
const json = editor.blocks.renderFromHTML(markdown);

// Test HTML output
const html = edjsHTML.generate(editor.save());

// Test block operations
editor.blocks.copy();
editor.blocks.paste();
```

---

## Best Practices

### 1. Performance

✅ Use lazy loading for plugins
✅ Cache edjsHTML module
✅ Defer editor initialization
✅ Limit block types to needed ones
✅ Use CDN for uploads

❌ Don't load all plugins upfront
❌ Don't create multiple editor instances
❌ Don't store large inline data

### 2. Security

✅ Sanitize user input
✅ HTML-escape code blocks
✅ Validate URLs in hyperlinks
✅ Use Angular DomSanitizer
✅ Escape content before rendering

❌ Don't allow arbitrary HTML
❌ Don't trust user-provided URLs
❌ Don't bypass sanitization

### 3. UX

✅ Show loading indicators
✅ Provide clear error messages
✅ Support keyboard shortcuts
✅ Implement auto-save
✅ Provide copy/paste functionality

❌ Don't require manual file uploads only
❌ Don't hide formatting options
❌ Don't allow invalid data

---

## Troubleshooting

### Issue: Editor not initializing

**Solution:** Check DOM element ID, ensure plugins are loaded

```typescript
// Correct
const editor = new EditorJS({ holder: 'editorjs' });
// HTML: <div id="editorjs"></div>
```

### Issue: Images not uploading

**Solution:** Verify uploader endpoint

```typescript
config: {
  uploader: {
    uploadByFile: (file) => {
      return store.postCDNEditorJS(file)  // Must return Promise
        .then(response => ({
          success: 1,
          file: { url: response.url }
        }));
    }
  }
}
```

### Issue: Performance degradation

**Solution:** Implement lazy loading and caching

```typescript
// Lazy load plugins
const { default: EditorJS } = await import("editorjs.umd.js");

// Cache HTML module
this.edjsHtmlCache = await import("editorjs-html");
```

---

## Resources

- **Editor.js Docs:** https://editorjs.io/
- **Plugin List:** https://github.com/editor-js/awesome-editorjs
- **BPMN.js:** https://bpmn.io/
- **Mermaid:** https://mermaid.js.org/

---

**Version:** 1.0
**Date:** November 2024
**Based on:** clde-angular-ecommerce production implementation
**Status:** Production Ready
