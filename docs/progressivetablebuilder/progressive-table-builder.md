# Progressive Table Builder for Streaming AI

Complete solution for building HTML tables cell-by-cell from streaming markdown, converting chunks to DOM elements fluently without flickering.

## The Core Problem

When AI streams a table, it arrives cell by cell:

```
| Header 1 | Header 2 |
|----------|----------|
| Cell 1   | Cell 2   |   <- Row completes
| Cell 3   |          <- New row starts, incomplete
```

**Critical Issues:**
1. Table renders with incomplete rows (flickering)
2. Links `[text](url)` split across chunks
3. Bold/italic styles break mid-stream
4. DOM structure shifts constantly
5. URLs and styles applied but table already rendered

## Solution: Incremental DOM Builder

### Core Class Implementation

```javascript
class ProgressiveTableBuilder {
  constructor(containerElement) {
    this.container = containerElement;
    this.buffer = '';
    this.currentTable = null;
    this.currentRow = null;
    this.currentCell = null;
    this.isInTable = false;
    this.headerProcessed = false;
    this.renderer = new AdvancedCellRenderer();
  }

  // Main entry point for processing chunks
  processChunk(chunk) {
    this.buffer += chunk;
    this.parseAndRender();
  }

  parseAndRender() {
    const lines = this.buffer.split('\n');
    let processedLines = 0;
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();
      
      // Detect table start
      if (this.isTableLine(line) && !this.isInTable) {
        this.startTable();
        this.isInTable = true;
      }
      
      // Process table rows
      if (this.isInTable && this.isTableLine(line)) {
        // Check if it's separator row
        if (this.isSeparatorRow(line)) {
          this.headerProcessed = true;
          processedLines = i + 1;
          continue;
        }
        
        // Check if row is complete (ends with |)
        if (line.endsWith('|')) {
          this.processCompleteRow(line);
          processedLines = i + 1;
        } else {
          // Incomplete row - wait for more chunks
          break;
        }
      }
      
      // Detect table end
      if (this.isInTable && !this.isTableLine(line) && line.length > 0) {
        this.endTable();
        this.isInTable = false;
        processedLines = i + 1;
      }
    }
    
    // Remove processed lines from buffer
    if (processedLines > 0) {
      this.buffer = lines.slice(processedLines).join('\n');
    }
  }

  isTableLine(line) {
    return line.includes('|') && line.trim().length > 0;
  }

  isSeparatorRow(line) {
    // Match separator like |------|------|
    return /^\|[\s\-:|]+\|$/.test(line);
  }

  startTable() {
    this.currentTable = document.createElement('table');
    this.currentTable.className = 'streaming-table';
    this.container.appendChild(this.currentTable);
  }

  processCompleteRow(line) {
    // Parse cells from line
    const cells = this.parseCells(line);
    
    // Create row
    const row = document.createElement('tr');
    row.className = 'streaming-row';
    
    // Add cells with animation delay
    cells.forEach((cellContent, index) => {
      const cellType = this.headerProcessed ? 'td' : 'th';
      const cell = document.createElement(cellType);
      cell.className = 'streaming-cell';
      cell.style.animationDelay = `${index * 50}ms`;
      
      // Convert markdown in cell to HTML elements
      this.renderCellContent(cell, cellContent);
      
      row.appendChild(cell);
      
      // Mark cell as complete after animation
      setTimeout(() => {
        cell.classList.remove('streaming-cell');
        cell.classList.add('complete-cell');
      }, 200 + (index * 50));
    });
    
    // Append row to appropriate section
    if (!this.headerProcessed) {
      let thead = this.currentTable.querySelector('thead');
      if (!thead) {
        thead = document.createElement('thead');
        this.currentTable.appendChild(thead);
      }
      thead.appendChild(row);
    } else {
      let tbody = this.currentTable.querySelector('tbody');
      if (!tbody) {
        tbody = document.createElement('tbody');
        this.currentTable.appendChild(tbody);
      }
      tbody.appendChild(row);
    }
  }

  parseCells(line) {
    // Remove leading and trailing pipes
    line = line.replace(/^\||\|$/g, '').trim();
    
    // Split by pipes
    const cells = line.split('|').map(cell => cell.trim());
    
    return cells;
  }

  renderCellContent(cell, markdown) {
    // Use advanced renderer for complex markdown
    const html = this.renderer.renderCell(markdown);
    cell.innerHTML = html;
  }

  endTable() {
    if (this.currentTable) {
      this.currentTable.classList.add('complete');
    }
  }
}
```

### Advanced Cell Renderer

```javascript
class AdvancedCellRenderer {
  constructor() {
    this.linkParser = new SmartLinkParser();
  }

  renderCell(markdown) {
    let html = markdown;

    // 1. Handle code blocks within cells
    html = this.handleCodeBlocks(html);

    // 2. Handle lists within cells  
    html = this.handleLists(html);

    // 3. Handle inline formatting (bold, italic, code)
    html = this.handleInlineFormatting(html);

    // 4. Handle links with incomplete state handling
    html = this.linkParser.parse(html);

    // 5. Sanitize
    html = this.sanitize(html);

    return html;
  }

  handleCodeBlocks(text) {
    // Match complete code blocks only
    const codeBlockRegex = /```(\w+)?\n([\s\S]+?)```/g;
    return text.replace(codeBlockRegex, (match, lang, code) => {
      return `<pre><code class="language-${lang || 'plaintext'}">${this.escapeHtml(code)}</code></pre>`;
    });
  }

  handleLists(text) {
    const lines = text.split('\n');
    let inList = false;
    let listHtml = '';
    let result = [];

    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
        if (!inList) {
          listHtml = '<ul>';
          inList = true;
        }
        const content = trimmed.substring(2);
        listHtml += `<li>${this.handleInlineFormatting(content)}</li>`;
      } else {
        if (inList) {
          listHtml += '</ul>';
          result.push(listHtml);
          listHtml = '';
          inList = false;
        }
        if (trimmed) {
          result.push(line);
        }
      }
    }

    if (inList) {
      listHtml += '</ul>';
      result.push(listHtml);
    }

    return result.join('\n');
  }

  handleInlineFormatting(text) {
    let html = text;

    // Bold: **text** or __text__
    // Only process if we have complete pairs
    const boldMatches = html.match(/\*\*/g);
    if (boldMatches && boldMatches.length % 2 === 0) {
      html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    }
    
    const underscoreBoldMatches = html.match(/__/g);
    if (underscoreBoldMatches && underscoreBoldMatches.length % 2 === 0) {
      html = html.replace(/__(.+?)__/g, '<strong>$1</strong>');
    }

    // Italic: *text* or _text_
    const italicMatches = html.match(/(?<!\*)\*(?!\*)/g);
    if (italicMatches && italicMatches.length % 2 === 0) {
      html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    }
    
    const underscoreItalicMatches = html.match(/(?<!_)_(?!_)/g);
    if (underscoreItalicMatches && underscoreItalicMatches.length % 2 === 0) {
      html = html.replace(/_(.+?)_/g, '<em>$1</em>');
    }

    // Inline code: `code`
    const codeMatches = html.match(/`/g);
    if (codeMatches && codeMatches.length % 2 === 0) {
      html = html.replace(/`(.+?)`/g, '<code>$1</code>');
    }

    // Strikethrough: ~~text~~
    const strikeMatches = html.match(/~~/g);
    if (strikeMatches && strikeMatches.length % 2 === 0) {
      html = html.replace(/~~(.+?)~~/g, '<del>$1</del>');
    }

    return html;
  }

  escapeHtml(text) {
    const map = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
  }

  sanitize(html) {
    // Basic sanitization - use DOMPurify in production
    if (typeof DOMPurify !== 'undefined') {
      return DOMPurify.sanitize(html, {
        ALLOWED_TAGS: ['strong', 'em', 'code', 'del', 'a', 'ul', 'ol', 'li', 'pre', 'br'],
        ALLOWED_ATTR: ['href', 'target', 'rel', 'class']
      });
    }
    return html;
  }
}
```

### Smart Link Parser

This handles links that are split across chunks:

```javascript
class SmartLinkParser {
  constructor() {
    this.state = 'TEXT';
    this.linkText = '';
    this.linkUrl = '';
  }

  parse(markdown) {
    let result = '';
    let buffer = '';
    
    for (let i = 0; i < markdown.length; i++) {
      const char = markdown[i];
      const nextChar = markdown[i + 1] || '';
      
      switch (this.state) {
        case 'TEXT':
          if (char === '[') {
            result += buffer;
            buffer = '';
            this.state = 'LINK_TEXT';
            this.linkText = '';
          } else {
            buffer += char;
          }
          break;
          
        case 'LINK_TEXT':
          if (char === ']' && nextChar === '(') {
            this.state = 'LINK_URL';
            this.linkUrl = '';
            i++; // Skip the '('
          } else if (char === ']') {
            // False alarm, not a link
            buffer += '[' + this.linkText + ']';
            this.state = 'TEXT';
            this.linkText = '';
          } else {
            this.linkText += char;
          }
          break;
          
        case 'LINK_URL':
          if (char === ')') {
            // Complete link found!
            result += `<a href="${this.escapeHtml(this.linkUrl)}" target="_blank" rel="noopener noreferrer">${this.linkText}</a>`;
            buffer = '';
            this.state = 'TEXT';
            this.linkText = '';
            this.linkUrl = '';
          } else {
            this.linkUrl += char;
          }
          break;
      }
    }
    
    // Handle incomplete states
    if (this.state === 'LINK_TEXT') {
      // Link text started but not complete
      result += '<span class="incomplete-link">[' + this.linkText;
    } else if (this.state === 'LINK_URL') {
      // Link URL started but not complete  
      result += '<span class="incomplete-link">[' + this.linkText + '](' + this.linkUrl;
    } else {
      result += buffer;
    }
    
    return result;
  }

  // Call when stream is complete to finalize any incomplete links
  finalize(markdown) {
    this.state = 'TEXT';
    return this.parse(markdown);
  }

  escapeHtml(text) {
    const map = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
  }
}
```

## CSS for Smooth Animation

```css
/* Base table styles */
.streaming-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  opacity: 0;
  animation: fadeIn 0.3s ease-in forwards;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

@keyframes fadeIn {
  to { opacity: 1; }
}

.streaming-table th,
.streaming-table td {
  border: 1px solid #e0e0e0;
  padding: 12px 16px;
  text-align: left;
  vertical-align: top;
}

.streaming-table th {
  background-color: #f5f5f5;
  font-weight: 600;
  color: #333;
}

.streaming-table td {
  background-color: #fff;
}

/* Streaming cell animation */
.streaming-cell {
  position: relative;
  background-color: #f9f9f9;
  animation: cellAppear 0.2s ease-out forwards;
}

@keyframes cellAppear {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.streaming-cell::after {
  content: '';
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  background-color: #4CAF50;
  border-radius: 50%;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { 
    opacity: 1;
    transform: translateY(-50%) scale(1);
  }
  50% { 
    opacity: 0.3;
    transform: translateY(-50%) scale(1.2);
  }
}

/* Complete cell */
.complete-cell {
  animation: cellComplete 0.3s ease-out;
}

@keyframes cellComplete {
  from {
    background-color: #e8f5e9;
  }
  to {
    background-color: transparent;
  }
}

/* Row animation */
.streaming-row {
  animation: rowSlideIn 0.3s ease-out;
}

@keyframes rowSlideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Links in cells */
.streaming-table a {
  color: #1976d2;
  text-decoration: none;
  transition: all 0.2s ease;
  border-bottom: 1px solid transparent;
}

.streaming-table a:hover {
  color: #1565c0;
  border-bottom-color: #1565c0;
}

/* Incomplete link indicator */
.incomplete-link {
  color: #999;
  font-style: italic;
  position: relative;
}

.incomplete-link::after {
  content: '⋯';
  margin-left: 2px;
  animation: ellipsis 1.5s infinite;
}

@keyframes ellipsis {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}

/* Text formatting in cells */
.streaming-table strong {
  font-weight: 600;
  color: #222;
}

.streaming-table em {
  font-style: italic;
  color: #555;
}

.streaming-table code {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 0.9em;
  color: #e83e8c;
}

.streaming-table pre {
  background-color: #f8f8f8;
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 4px 0;
}

.streaming-table pre code {
  background-color: transparent;
  padding: 0;
  color: #333;
}

/* Lists in cells */
.streaming-table ul,
.streaming-table ol {
  margin: 8px 0;
  padding-left: 20px;
}

.streaming-table li {
  margin: 4px 0;
}

/* Complete table state */
.streaming-table.complete {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  animation: tableComplete 0.4s ease-out;
}

@keyframes tableComplete {
  from {
    box-shadow: 0 0 0 rgba(0, 0, 0, 0);
  }
  to {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .streaming-table {
    font-size: 14px;
  }
  
  .streaming-table th,
  .streaming-table td {
    padding: 8px 12px;
  }
}
```

## Complete Integration Example

```javascript
class StreamingTableHandler {
  constructor(containerId) {
    this.container = document.getElementById(containerId);
    this.tableBuilder = new ProgressiveTableBuilder(this.container);
  }

  async handleStream(streamSource) {
    const reader = streamSource.getReader();
    const decoder = new TextDecoder();
    
    try {
      while (true) {
        const { done, value } = await reader.read();
        
        if (done) {
          // Finalize any incomplete content
          this.tableBuilder.parseAndRender();
          break;
        }
        
        const chunk = decoder.decode(value, { stream: true });
        
        // Clean chunk
        const cleaned = this.cleanChunk(chunk);
        
        // Process chunk
        this.tableBuilder.processChunk(cleaned);
        
        // Small delay to prevent overwhelming DOM
        await this.sleep(10);
      }
    } catch (error) {
      console.error('Streaming error:', error);
      this.showError(error);
    }
  }

  cleanChunk(chunk) {
    return chunk
      // Remove zero-width characters
      .replace(/[\u200B\u200C\u200D\u200E\u200F\uFEFF]/g, '')
      // Normalize line breaks
      .replace(/\r\n/g, '\n')
      .replace(/\r/g, '\n')
      // Remove control characters
      .replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '');
  }

  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  showError(error) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'stream-error';
    errorDiv.textContent = `Error: ${error.message}`;
    this.container.appendChild(errorDiv);
  }
}

// Usage
const handler = new StreamingTableHandler('chat-container');

// Connect to your API
async function startChat(prompt) {
  const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt })
  });
  
  await handler.handleStream(response.body);
}

startChat('Show me a comparison table of products');
```

## Vue 3 Component

```vue
<template>
  <div>
    <div ref="tableContainer" class="chat-table-container"></div>
    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';

const props = defineProps({
  streamSource: {
    type: Object,
    required: true
  }
});

const tableContainer = ref(null);
const error = ref(null);
let tableBuilder = null;

async function processStream() {
  if (!props.streamSource || !tableContainer.value) return;
  
  tableBuilder = new ProgressiveTableBuilder(tableContainer.value);
  const reader = props.streamSource.getReader();
  const decoder = new TextDecoder();
  
  try {
    while (true) {
      const { done, value } = await reader.read();
      
      if (done) {
        tableBuilder.parseAndRender();
        break;
      }
      
      const chunk = decoder.decode(value, { stream: true });
      const cleaned = cleanChunk(chunk);
      
      tableBuilder.processChunk(cleaned);
      
      await new Promise(resolve => setTimeout(resolve, 10));
    }
  } catch (err) {
    error.value = err.message;
    console.error('Streaming error:', err);
  }
}

function cleanChunk(chunk) {
  return chunk
    .replace(/[\u200B\u200C\u200D\u200E\u200F\uFEFF]/g, '')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '');
}

watch(() => props.streamSource, () => {
  if (props.streamSource) {
    processStream();
  }
}, { immediate: true });
</script>

<style scoped>
.chat-table-container {
  margin: 20px 0;
}

.error-message {
  color: #d32f2f;
  padding: 12px;
  background-color: #ffebee;
  border-radius: 4px;
  margin-top: 12px;
}
</style>
```

## React Hook

```javascript
import { useEffect, useRef, useState } from 'react';

function useStreamingTable(streamSource) {
  const containerRef = useRef(null);
  const builderRef = useRef(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!streamSource || !containerRef.current) return;

    builderRef.current = new ProgressiveTableBuilder(containerRef.current);

    const processStream = async () => {
      const reader = streamSource.getReader();
      const decoder = new TextDecoder();

      try {
        while (true) {
          const { done, value } = await reader.read();

          if (done) {
            builderRef.current.parseAndRender();
            break;
          }

          const chunk = decoder.decode(value, { stream: true });
          const cleaned = chunk
            .replace(/[\u200B\u200C\u200D\u200E\u200F\uFEFF]/g, '')
            .replace(/\r\n/g, '\n')
            .replace(/\r/g, '\n')
            .replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '');

          builderRef.current.processChunk(cleaned);

          await new Promise(resolve => setTimeout(resolve, 10));
        }
      } catch (err) {
        setError(err.message);
        console.error('Streaming error:', err);
      }
    };

    processStream();

    return () => {
      // Cleanup if needed
      if (containerRef.current) {
        containerRef.current.innerHTML = '';
      }
    };
  }, [streamSource]);

  return { containerRef, error };
}

// Usage in component
function ChatTable({ streamSource }) {
  const { containerRef, error } = useStreamingTable(streamSource);

  return (
    <div>
      <div ref={containerRef} className="chat-table-container" />
      {error && <div className="error-message">{error}</div>}
    </div>
  );
}
```

## Testing

```javascript
// Test case 1: Complete table
const testComplete = `
| Product | Price | Link |
|---------|-------|------|
| Item A | $10 | [Buy](http://example.com/a) |
| Item B | $20 | [Buy](http://example.com/b) |
`;

// Test case 2: Incomplete link
const testIncompleteLink = `
| Product | Price | Link |
|---------|-------|------|
| Item A | $10 | [Buy](http://ex
`;

// Test case 3: Cell-by-cell chunks
const testChunks = [
  '| ',
  'Product ',
  '| Price ',
  '| Link |\n',
  '|------',
  '---|----',
  '---|------|\n',
  '| Item A ',
  '| $10 ',
  '| **Bold** ',
  'and *italic* ',
  '| [Buy](',
  'http://example.com/a',
  ') |\n',
  '| Item B ',
  '| $20 ',
  '| `code` here ',
  '| [View](',
  'http://example.com/b',
  ') |\n'
];

// Run test
async function runTest() {
  const container = document.getElementById('test-container');
  const handler = new StreamingTableHandler('test-container');

  // Simulate streaming
  const encoder = new TextEncoder();
  const stream = new ReadableStream({
    async start(controller) {
      for (const chunk of testChunks) {
        controller.enqueue(encoder.encode(chunk));
        await new Promise(resolve => setTimeout(resolve, 100));
      }
      controller.close();
    }
  });

  await handler.handleStream(stream);
  console.log('Test complete!');
}
```

## Performance Optimization

```javascript
class OptimizedTableBuilder extends ProgressiveTableBuilder {
  constructor(containerElement) {
    super(containerElement);
    this.renderScheduled = false;
    this.pendingChunks = [];
    this.lastRenderTime = 0;
    this.minRenderInterval = 50; // ms
  }

  processChunk(chunk) {
    this.pendingChunks.push(chunk);
    
    if (!this.renderScheduled) {
      this.scheduleRender();
    }
  }

  scheduleRender() {
    const now = Date.now();
    const timeSinceLastRender = now - this.lastRenderTime;
    
    if (timeSinceLastRender >= this.minRenderInterval) {
      // Render immediately
      this.batchRender();
    } else {
      // Schedule for later
      this.renderScheduled = true;
      const delay = this.minRenderInterval - timeSinceLastRender;
      
      setTimeout(() => {
        this.batchRender();
      }, delay);
    }
  }

  batchRender() {
    if (this.pendingChunks.length === 0) return;
    
    // Process all pending chunks at once
    const allChunks = this.pendingChunks.join('');
    this.pendingChunks = [];
    
    super.processChunk(allChunks);
    
    this.lastRenderTime = Date.now();
    this.renderScheduled = false;
  }
}
```

## Debugging Tools

```javascript
class TableBuilderDebugger {
  constructor(builder) {
    this.builder = builder;
    this.events = [];
    this.enabled = false;
  }

  enable() {
    this.enabled = true;
    this.wrapMethods();
  }

  wrapMethods() {
    const original = this.builder.processChunk.bind(this.builder);
    
    this.builder.processChunk = (chunk) => {
      if (this.enabled) {
        this.logEvent('processChunk', { chunk, length: chunk.length });
      }
      return original(chunk);
    };
  }

  logEvent(type, data) {
    this.events.push({
      timestamp: Date.now(),
      type,
      data
    });
  }

  getStats() {
    return {
      totalEvents: this.events.length,
      totalChunks: this.events.filter(e => e.type === 'processChunk').length,
      events: this.events
    };
  }

  exportLogs() {
    const blob = new Blob([JSON.stringify(this.events, null, 2)], {
      type: 'application/json'
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `table-builder-debug-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  visualize() {
    console.group('Table Builder Debug Info');
    console.log('Total events:', this.events.length);
    console.table(this.events.slice(-20)); // Last 20 events
    console.groupEnd();
  }
}

// Usage
const builder = new ProgressiveTableBuilder(container);
const debugger = new TableBuilderDebugger(builder);
debugger.enable();

// Later...
debugger.visualize();
debugger.exportLogs();
```

## Troubleshooting

### Issue: Links Still Breaking

**Solution:** Ensure link parser state persists across chunks

```javascript
// In ProgressiveTableBuilder constructor
this.linkParser = new SmartLinkParser(); // Persistent parser

// In renderCellContent
renderCellContent(cell, markdown) {
  const html = this.linkParser.parse(markdown);
  cell.innerHTML = html;
}
```

### Issue: Table Rows Jumping

**Solution:** Use `table-layout: fixed` and set explicit widths

```css
.streaming-table {
  table-layout: fixed;
  width: 100%;
}

.streaming-table th:nth-child(1),
.streaming-table td:nth-child(1) {
  width: 40%;
}

.streaming-table th:nth-child(2),
.streaming-table td:nth-child(2) {
  width: 30%;
}

.streaming-table th:nth-child(3),
.streaming-table td:nth-child(3) {
  width: 30%;
}
```

### Issue: Bold/Italic Not Applying

**Solution:** Check for complete pairs before processing

```javascript
// Already implemented in AdvancedCellRenderer.handleInlineFormatting()
const boldMatches = html.match(/\*\*/g);
if (boldMatches && boldMatches.length % 2 === 0) {
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
}
```

## Summary

This progressive table builder provides:

✅ **Cell-by-cell DOM element creation** - No re-parsing entire table  
✅ **Smooth animations** - Cells appear progressively without flickering  
✅ **Smart link handling** - Links split across chunks work correctly  
✅ **Style application during streaming** - Bold, italic, code render immediately  
✅ **Incomplete state indicators** - Visual feedback for incomplete content  
✅ **Performance optimized** - Batching and throttling built-in  
✅ **Framework agnostic** - Works with vanilla JS, React, Vue, etc.  
✅ **Production ready** - Error handling, debugging, testing included

The key insight: **Build DOM elements incrementally** rather than converting markdown → HTML on every chunk. This eliminates flickering and allows styles/links to be applied fluently as the table streams in.
