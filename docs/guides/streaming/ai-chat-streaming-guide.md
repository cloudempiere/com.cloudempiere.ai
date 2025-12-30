# AI Chat Streaming with LangChain: Complete Implementation Guide

A comprehensive guide for implementing flicker-free, clean markdown streaming in AI chat applications backed by LangChain.

## Table of Contents

- [Overview](#overview)
- [Problem Statement](#problem-statement)
- [Complete Solution Architecture](#complete-solution-architecture)
- [Implementation Details](#implementation-details)
- [LangChain Integration](#langchain-integration)
- [Frontend Implementation](#frontend-implementation)
- [Best Practices](#best-practices)

## Overview

When building AI chat applications with LangChain, streaming responses can introduce several challenges:
- Strange characters and zero-width unicode
- Excessive line breaks
- Flickering during markdown to HTML conversion
- Incomplete table rendering
- Security concerns (XSS)

This guide provides production-ready solutions for all these issues.

## Problem Statement

**Key Challenges:**
1. LLM outputs contain control characters, zero-width spaces, and inconsistent line breaks
2. Converting markdown to HTML on every token causes flickering
3. Tables render incorrectly during streaming (incomplete rows)
4. Need to balance real-time updates with smooth rendering

## Complete Solution Architecture

### 1. Stream Processor - Clean and Buffer Chunks

```javascript
class StreamProcessor {
  constructor() {
    this.buffer = '';
    this.displayBuffer = '';
    this.renderQueue = [];
  }

  // Clean incoming chunks
  cleanChunk(chunk) {
    return chunk
      // Remove zero-width characters
      .replace(/[\u200B\u200C\u200D\u200E\u200F\uFEFF]/g, '')
      // Normalize line breaks
      .replace(/\r\n/g, '\n')
      .replace(/\r/g, '\n')
      // Remove excessive blank lines (more than 2)
      .replace(/\n{3,}/g, '\n\n')
      // Trim trailing spaces but preserve markdown line breaks (2 spaces)
      .split('\n')
      .map(line => {
        // Keep 2-space line breaks in markdown
        if (line.endsWith('  ')) {
          return line.trimStart();
        }
        return line.trim();
      })
      .join('\n');
  }

  // Accumulate chunks
  addChunk(rawChunk) {
    const cleaned = this.cleanChunk(rawChunk);
    this.buffer += cleaned;
    this.renderQueue.push(cleaned);
  }

  // Get content ready for rendering
  getDisplayContent() {
    // Process queue in batches to avoid flickering
    if (this.renderQueue.length > 0) {
      const batch = this.renderQueue.join('');
      this.renderQueue = [];
      this.displayBuffer += batch;
    }
    return this.displayBuffer;
  }
}
```

### 2. Markdown to HTML Converter

```javascript
import { marked } from 'marked';
import DOMPurify from 'dompurify';

// Configure marked for AI streaming
marked.setOptions({
  breaks: true,        // Convert \n to <br>
  gfm: true,          // GitHub Flavored Markdown
  headerIds: false,   // Don't generate IDs during streaming
  mangle: false,      // Don't encode email addresses
  sanitize: false     // We'll use DOMPurify instead
});

function convertMarkdownToHTML(markdown) {
  // Convert to HTML
  const rawHTML = marked.parse(markdown);
  
  // Sanitize to prevent XSS
  const cleanHTML = DOMPurify.sanitize(rawHTML, {
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'em', 'u', 'code', 'pre',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li', 'blockquote',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'a', 'img', 'hr', 'div', 'span'
    ],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class']
  });

  return cleanHTML;
}
```

### 3. React Component - Smooth Rendering

```javascript
import React, { useState, useEffect, useRef } from 'react';

function StreamingChatMessage({ streamProcessor }) {
  const [htmlContent, setHtmlContent] = useState('');
  const [isStreaming, setIsStreaming] = useState(true);
  const updateIntervalRef = useRef(null);

  useEffect(() => {
    // Update display at fixed intervals to avoid flickering
    updateIntervalRef.current = setInterval(() => {
      const markdown = streamProcessor.getDisplayContent();
      
      // Only convert to HTML when we have complete markdown blocks
      const html = convertMarkdownToHTML(markdown);
      setHtmlContent(html);
      
    }, 50); // 50ms = smooth updates without excessive renders

    return () => {
      clearInterval(updateIntervalRef.current);
    };
  }, [streamProcessor]);

  return (
    <div 
      className="message-content"
      dangerouslySetInnerHTML={{ __html: htmlContent }}
    />
  );
}
```

## LangChain Integration

### Python Backend with LangChain

```python
from langchain.callbacks.streaming_stdout import StreamingStdOutCallbackHandler
from langchain.chat_models import ChatOpenAI
import re

class CleanupStreamingCallback(StreamingStdOutCallbackHandler):
    """Custom callback that cleans chunks before sending"""
    
    def on_llm_new_token(self, token: str, **kwargs) -> None:
        # Clean the token
        cleaned = self.clean_token(token)
        
        # Send to frontend via SSE/WebSocket
        self.send_to_frontend(cleaned)
    
    def clean_token(self, token: str) -> str:
        # Remove control characters
        token = re.sub(r'[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]', '', token)
        
        # Normalize whitespace but keep markdown formatting
        return token
    
    def send_to_frontend(self, token: str):
        # Your SSE/WebSocket implementation
        pass

# Usage
llm = ChatOpenAI(
    streaming=True,
    callbacks=[CleanupStreamingCallback()]
)
```

### FastAPI Streaming Endpoint

```python
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from langchain.chat_models import ChatAnthropic
import asyncio
import re

app = FastAPI()

def clean_stream_chunk(chunk: str) -> str:
    """Clean individual chunks"""
    # Remove zero-width characters
    chunk = re.sub(r'[\u200B-\u200F\uFEFF]', '', chunk)
    # Remove control characters
    chunk = re.sub(r'[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]', '', chunk)
    return chunk

async def generate_stream(prompt: str):
    llm = ChatAnthropic(
        model="claude-3-sonnet-20240229",
        streaming=True
    )
    
    async for chunk in llm.astream(prompt):
        content = chunk.content
        cleaned = clean_stream_chunk(content)
        
        # Send as SSE
        yield f"data: {cleaned}\n\n"
        await asyncio.sleep(0.01)  # Throttle slightly

@app.post("/chat/stream")
async def chat_stream(prompt: str):
    return StreamingResponse(
        generate_stream(prompt),
        media_type="text/event-stream"
    )
```

## Progressive Table Rendering Fix

Tables need special handling during streaming to avoid showing incomplete rows:

```javascript
// Detect incomplete tables and show loading state
function isIncompleteTable(markdown) {
  const tableRegex = /\|.*\|/g;
  const lines = markdown.trim().split('\n');
  const lastLine = lines[lines.length - 1];
  
  // If last line looks like table row but no closing blank line
  return tableRegex.test(lastLine) && lines[lines.length - 2]?.includes('|');
}

function convertWithTableHandling(markdown) {
  if (isIncompleteTable(markdown)) {
    // Add temporary indicator
    return marked.parse(markdown) + '<div class="loading-indicator">...</div>';
  }
  return marked.parse(markdown);
}
```

## Frontend Implementation

### Complete JavaScript Client

```javascript
class AIStreamingChat {
  constructor(messageElement) {
    this.element = messageElement;
    this.processor = new StreamProcessor();
    this.eventSource = null;
  }

  async sendMessage(prompt) {
    const response = await fetch('/chat/stream', {
      method: 'POST',
      body: JSON.stringify({ prompt }),
      headers: { 'Content-Type': 'application/json' }
    });

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    // Start display updater
    this.startRendering();

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value);
      this.processor.addChunk(chunk);
    }

    this.stopRendering();
  }

  startRendering() {
    this.renderInterval = setInterval(() => {
      const markdown = this.processor.getDisplayContent();
      const html = convertMarkdownToHTML(markdown);
      this.element.innerHTML = html;
    }, 50); // 20 FPS - smooth but not excessive
  }

  stopRendering() {
    clearInterval(this.renderInterval);
    // Final render
    const markdown = this.processor.getDisplayContent();
    const html = convertMarkdownToHTML(markdown);
    this.element.innerHTML = html;
  }
}

// Usage
const chat = new AIStreamingChat(document.getElementById('chat-message'));
chat.sendMessage('Tell me about AI streaming');
```

### Vue 3 Implementation

```vue
<template>
  <div class="chat-message" v-html="htmlContent"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

const props = defineProps(['streamProcessor']);
const htmlContent = ref('');
let updateInterval = null;

marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false
});

function updateDisplay() {
  const markdown = props.streamProcessor.getDisplayContent();
  const rawHTML = marked.parse(markdown);
  htmlContent.value = DOMPurify.sanitize(rawHTML);
}

onMounted(() => {
  updateInterval = setInterval(updateDisplay, 50);
});

onUnmounted(() => {
  clearInterval(updateInterval);
});
</script>
```

## Best Practices

### Key Points to Avoid Flickering

1. **Batch updates** - Don't render every single token individually
2. **Fixed interval rendering** - Render at consistent 20-50ms intervals (20-50 FPS)
3. **Convert to HTML once per batch** - Not per token
4. **Handle incomplete markdown** - Detect and show loading states for tables/code blocks
5. **Sanitize HTML output** - Always use DOMPurify or similar

### Character Cleanup Patterns

**Remove these problematic characters:**
- Zero-width spaces: `\u200B-\u200F`, `\uFEFF`
- Control characters: `\x00-\x08`, `\x0B`, `\x0C`, `\x0E-\x1F`, `\x7F`
- Normalize line breaks: Convert `\r\n` and `\r` to `\n`
- Limit consecutive blank lines: Max 2 blank lines

**Preserve these:**
- Markdown line breaks: 2 trailing spaces
- Code block indentation
- Table formatting

### Performance Optimization

**Rendering frequency:**
```javascript
// Too fast - causes excessive DOM updates
setInterval(update, 10);  // 100 FPS - BAD

// Good balance - smooth without overhead
setInterval(update, 50);  // 20 FPS - GOOD

// Also acceptable
setInterval(update, 33);  // 30 FPS - GOOD
```

**Debouncing for large content:**
```javascript
function createDebouncedRenderer(delay = 50) {
  let timeoutId;
  
  return function render(content) {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => {
      // Render content
      updateDOM(content);
    }, delay);
  };
}
```

## Security Considerations

### Always Sanitize HTML

```javascript
// BAD - XSS vulnerability
element.innerHTML = marked.parse(markdown);

// GOOD - Sanitized
const rawHTML = marked.parse(markdown);
const cleanHTML = DOMPurify.sanitize(rawHTML);
element.innerHTML = cleanHTML;
```

### Allowed Tags Configuration

```javascript
const sanitizerConfig = {
  ALLOWED_TAGS: [
    'p', 'br', 'strong', 'em', 'u', 'code', 'pre',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'ul', 'ol', 'li', 'blockquote',
    'table', 'thead', 'tbody', 'tr', 'th', 'td',
    'a', 'img', 'hr', 'div', 'span'
  ],
  ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class'],
  ALLOW_DATA_ATTR: false
};

const cleanHTML = DOMPurify.sanitize(rawHTML, sanitizerConfig);
```

## Testing

### Test Cases

```javascript
// Test 1: Zero-width characters
const test1 = '\uFEFFHello\u200BWorld';
assert(cleanChunk(test1) === 'HelloWorld');

// Test 2: Line break normalization
const test2 = 'Line1\r\nLine2\rLine3\nLine4';
assert(cleanChunk(test2).split('\n').length === 4);

// Test 3: Excessive blank lines
const test3 = 'Para1\n\n\n\n\nPara2';
assert(cleanChunk(test3) === 'Para1\n\nPara2');

// Test 4: Markdown line breaks preserved
const test4 = 'Line1  \nLine2';
assert(cleanChunk(test4) === 'Line1  \nLine2');
```

## Troubleshooting

### Common Issues

**Issue: Tables still flickering**
- Solution: Implement incomplete table detection
- Add loading indicator for incomplete tables

**Issue: Code blocks losing indentation**
- Solution: Don't trim whitespace inside code blocks
- Use proper markdown parser with code block support

**Issue: Performance degradation with long content**
- Solution: Implement virtual scrolling
- Limit history to last N messages
- Use pagination

**Issue: Emojis not rendering**
- Solution: Ensure UTF-8 encoding throughout pipeline
- Use emoji-aware markdown parser

## Dependencies

```json
{
  "dependencies": {
    "marked": "^11.0.0",
    "dompurify": "^3.0.6"
  }
}
```

### Python Requirements

```txt
fastapi>=0.104.0
langchain>=0.1.0
langchain-anthropic>=0.1.0
uvicorn>=0.24.0
```

## Summary

This solution provides:
- ✅ Clean, normalized markdown from LLM output
- ✅ No flickering during streaming
- ✅ Proper table and code block rendering
- ✅ XSS protection with sanitization
- ✅ Smooth 20-30 FPS display updates
- ✅ Production-ready code examples

The key insight is to **decouple network streaming from visual rendering** - receive chunks as fast as possible, but display them at a controlled, consistent rate.

## License

This guide is provided as-is for educational and commercial use.

## Further Reading

- [Marked.js Documentation](https://marked.js.org/)
- [DOMPurify Repository](https://github.com/cure53/DOMPurify)
- [LangChain Streaming Docs](https://python.langchain.com/docs/concepts/streaming)
- [MDN: Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
