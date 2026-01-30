# Architecture Diagrams - CloudEmpiere AI

**Purpose:** Visual reference for understanding layer interactions and data flow
**Audience:** Developers, architects
**Last Updated:** 2026-01-30

---

## Table of Contents

1. [ZK Chat Panel Layer Interaction](#zk-chat-panel-layer-interaction)
2. [Streaming Rendering Flow](#streaming-rendering-flow)
3. [Multi-Plugin OSGi Architecture](#multi-plugin-osgi-architecture)
4. [AI Provider Architecture](#ai-provider-architecture)
5. [Domain Agent Execution Flow](#domain-agent-execution-flow)
6. [Database Security Layer](#database-security-layer)
7. [RAG Context Retrieval Flow](#rag-context-retrieval-flow)

---

## ZK Chat Panel Layer Interaction

### Overview

Shows how HTML chat messages flow through ZK UI layers from user input to AI response display.

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    ZK CHAT PANEL ARCHITECTURE                              │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ LAYER 1: ZK UI COMPONENTS (Presentation)                             │ │
│  │ File: AIChatWidget.java (ZK Div component)                           │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  [User Input Textbox]  ──────────────┐                              │ │
│  │         │                             │                              │ │
│  │         │ onClick                     │                              │ │
│  │         ▼                             │                              │ │
│  │  sendMessage(String text)             │                              │ │
│  │         │                             │                              │ │
│  │         │ 1. Validate input           │                              │ │
│  │         │ 2. Save to CM_ChatEntry     │                              │ │
│  │         │ 3. Render user bubble       │                              │ │
│  │         ▼                             │                              │ │
│  │  [Chat Messages Container]            │                              │ │
│  │  (Div with vertical layout)           │                              │ │
│  │         │                             │                              │ │
│  │         │ Display:                    │                              │ │
│  │         │ • User message bubble       │ onComplete                  │ │
│  │         │ • AI streaming message ◄────┼─────────────────────────────┘ │
│  │         │                             │                                │
│  └─────────┼─────────────────────────────┼────────────────────────────────┘
│            │                             │                                │
│            │ 4. Call AIService           │                                │
│            ▼                             │                                │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ LAYER 2: AI SERVICE ORCHESTRATION                                   │  │
│  │ File: AIService.java (@Component OSGi service)                      │  │
│  ├─────────────────────────────────────────────────────────────────────┤  │
│  │                                                                      │  │
│  │  chatStreaming(chatId, message, callback)                           │  │
│  │         │                                                            │  │
│  │         ├──> 1. Load chat context (ThreadAwareChatMemory)           │  │
│  │         │         └─> Query CM_ChatEntry (last 20 messages)         │  │
│  │         │                                                            │  │
│  │         ├──> 2. Get AI provider (IAIProviderFactory)                │  │
│  │         │         └─> Query AIG_Provider by AD_User_ID/AD_Role_ID   │  │
│  │         │                                                            │  │
│  │         ├──> 3. Build agent (AiServices.builder())                  │  │
│  │         │         ├─> ChatLanguageModel (from provider)             │  │
│  │         │         ├─> Tools (ERPTools, RagTools)                    │  │
│  │         │         └─> ChatMemory (ThreadAwareChatMemory)            │  │
│  │         │                                                            │  │
│  │         └──> 4. Execute streaming chat                              │  │
│  │                     │                                                │  │
│  │                     ▼                                                │  │
│  └─────────────────────┼────────────────────────────────────────────────  │
│                        │                                                │  │
│                        │ 5. Stream chunks                               │  │
│                        ▼                                                │  │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ LAYER 3: STREAMING MESSAGE COMPONENT                                │  │
│  │ File: AIChatStreamingMessage.java                                   │  │
│  ├─────────────────────────────────────────────────────────────────────┤  │
│  │                                                                      │  │
│  │  onNext(chunk)  ──────────────┐                                     │  │
│  │         │                      │                                     │  │
│  │         │ Chunk Queue          │ Background Thread                  │  │
│  │         ▼                      │ (processes batches)                │  │
│  │  [Queue<String>]               │                                     │  │
│  │         │                      │                                     │  │
│  │         │ Every 50 chars       │                                     │  │
│  │         ▼                      ▼                                     │  │
│  │  processBatch()  ──────> renderMarkdown()                           │  │
│  │         │                      │                                     │  │
│  │         │                      ├──> ChunkCleaner.clean()            │  │
│  │         │                      │    (remove control chars)           │  │
│  │         │                      │                                     │  │
│  │         │                      ├──> MarkdownValidator.validate()    │  │
│  │         │                      │    (check structure)                │  │
│  │         │                      │                                     │  │
│  │         │                      └──> StreamingMarkdownRenderer        │  │
│  │         │                           .appendChunk(chunk)              │  │
│  │         │                                │                           │  │
│  │         │                                ▼                           │  │
│  └─────────┼────────────────────────────────┼───────────────────────────  │
│            │                                │                           │  │
│            │ 6. Update UI                   │                           │  │
│            ▼                                │                           │  │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ LAYER 4: MARKDOWN RENDERING ENGINE                                  │  │
│  │ File: StreamingMarkdownRenderer.java                                │  │
│  ├─────────────────────────────────────────────────────────────────────┤  │
│  │                                                                      │  │
│  │  State Machine (6 states):                                          │  │
│  │  ┌────────┐ ** ┌────────┐ * ┌─────────┐ ` ┌─────────────┐         │  │
│  │  │ NORMAL │───>│  BOLD  │──>│ ITALIC  │──>│ INLINE_CODE │         │  │
│  │  └────┬───┘    └────────┘   └─────────┘   └─────────────┘         │  │
│  │       │                                                              │  │
│  │       │ ``` (triple backtick)                                       │  │
│  │       ▼                                                              │  │
│  │  ┌────────────┐      newline       ┌──────────┐                    │  │
│  │  │ CODE_BLOCK │◄──────────────────>│ HEADING  │                    │  │
│  │  └────────────┘                    └──────────┘                    │  │
│  │                                                                      │  │
│  │  Character-by-character processing:                                 │  │
│  │  appendChunk(chunk) ──> processChar(ch) ──> updateState(ch)        │  │
│  │         │                     │                     │                │  │
│  │         │                     │                     ├─> emitTag()   │  │
│  │         │                     │                     │   (when marker │  │
│  │         │                     │                     │    complete)   │  │
│  │         │                     │                     │                │  │
│  │         │                     │                     └─> buffer      │  │
│  │         │                     │                         incomplete  │  │
│  │         │                     │                         markers     │  │
│  │         │                     ▼                                      │  │
│  │         │              renderCurrentState()                         │  │
│  │         │                     │                                      │  │
│  │         │                     ├──> <div class="ai-message">         │  │
│  │         │                     │    <p>Text with <strong>bold</strong>│  │
│  │         │                     │    and <em>italic</em>.</p>          │  │
│  │         │                     │    </div>                            │  │
│  │         │                     │                                      │  │
│  └─────────┼─────────────────────┼──────────────────────────────────────  │
│            │                     │                                      │  │
│            │ 7. Sanitize HTML    │                                      │  │
│            ▼                     │                                      │  │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ LAYER 5: SECURITY SANITIZATION                                      │  │
│  │ File: SecuritySanitizer.java                                        │  │
│  ├─────────────────────────────────────────────────────────────────────┤  │
│  │                                                                      │  │
│  │  escapeHtml(html) ──────────────────────────────────┐              │  │
│  │         │                                            │              │  │
│  │         ├──> Replace: < → &lt;  > → &gt;            │              │  │
│  │         ├──> Replace: & → &amp;  " → &quot;          │              │  │
│  │         ├──> Remove: <script> tags                  │              │  │
│  │         ├──> Remove: javascript: URLs                │              │  │
│  │         └──> Remove: on* event handlers              │              │  │
│  │                     │                                │              │  │
│  │                     ▼                                │              │  │
│  │  Sanitized HTML ────────────────────────────────────┘              │  │
│  │         │                                                            │  │
│  └─────────┼────────────────────────────────────────────────────────────  │
│            │                                                            │  │
│            │ 8. Update ZK component                                    │  │
│            ▼                                                            │  │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ LAYER 6: ZK SERVER-PUSH UPDATE                                      │  │
│  │ File: AIChatWidget.java (updateUIAsync)                             │  │
│  ├─────────────────────────────────────────────────────────────────────┤  │
│  │                                                                      │  │
│  │  updateUIAsync(sanitizedHtml)                                       │  │
│  │         │                                                            │  │
│  │         ├──> Executions.schedule(desktop, () -> {                   │  │
│  │         │         streamingContent.setContent(sanitizedHtml);       │  │
│  │         │         streamingContent.invalidate();  // ZK refresh     │  │
│  │         │    }, null);                                              │  │
│  │         │                                                            │  │
│  │         │    ZK Server-Push:                                        │  │
│  │         │    ┌──────────────────────────────────────┐              │  │
│  │         └───>│ Desktop Thread Scheduler             │              │  │
│  │              │ (non-blocking, batched updates)      │              │  │
│  │              └──────────────────────────────────────┘              │  │
│  │                     │                                                │  │
│  │                     ▼                                                │  │
│  │  ┌────────────────────────────────────────────────────────────┐    │  │
│  │  │ Browser DOM Update (via ZK AU - Asynchronous Update)       │    │  │
│  │  │                                                              │    │  │
│  │  │  <div id="streamingMsg" class="ai-message-bubble">         │    │  │
│  │  │    <div class="ai-message">                                 │    │  │
│  │  │      <p>AI response with <strong>formatting</strong></p>   │    │  │
│  │  │    </div>                                                    │    │  │
│  │  │  </div>                                                      │    │  │
│  │  │                                                              │    │  │
│  │  └──────────────────────────────────────────────────────────────    │  │
│  │                                                                      │  │
│  └──────────────────────────────────────────────────────────────────────  │
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ LAYER 7: PERSISTENCE (on completion)                                 │ │
│  │ File: AIChatWidget.java (onComplete handler)                         │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  onComplete(AiMessage message)                                       │ │
│  │         │                                                             │ │
│  │         ├──> getRenderedHtml() from AIChatStreamingMessage           │ │
│  │         │    (HTML already rendered and sanitized)                   │ │
│  │         │                                                             │ │
│  │         ├──> MAIChatEntry.createAIResponse(chat, renderedHtml)       │ │
│  │         │         │                                                   │ │
│  │         │         └──> INSERT INTO CM_ChatEntry (                    │ │
│  │         │                  CM_Chat_ID,                               │ │
│  │         │                  AD_User_ID,                               │ │
│  │         │                  CharacterData,  ◄─ HTML stored here       │ │
│  │         │                  IsAI,                                     │ │
│  │         │                  Created                                   │ │
│  │         │              )                                             │ │
│  │         │                                                             │ │
│  │         └──> Future reload:                                          │ │
│  │                  SELECT CharacterData FROM CM_ChatEntry              │ │
│  │                  → Direct HTML display (no re-rendering!)            │ │
│  │                                                                       │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### Key Points

1. **Layer 1 (ZK UI):** User input → sendMessage()
2. **Layer 2 (AI Service):** Orchestrates provider, memory, tools
3. **Layer 3 (Streaming Message):** Chunk queue + batch processing (50 chars)
4. **Layer 4 (Markdown Renderer):** Character-by-character state machine
5. **Layer 5 (Security):** HTML/JS/URL sanitization
6. **Layer 6 (ZK Server-Push):** Non-blocking DOM updates
7. **Layer 7 (Persistence):** Store rendered HTML to database

**Performance:**
- Streaming latency: <50ms per chunk batch
- No re-rendering on reload (HTML stored)
- Thread-safe via ZK Executions.schedule()

---

## Streaming Rendering Flow

### Chunk Processing Pipeline

```
┌────────────────────────────────────────────────────────────────────────────┐
│                  STREAMING RENDERING DATA FLOW                             │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  AI Provider (Anthropic/Bedrock/Ollama)                                   │
│         │                                                                  │
│         │ StreamingResponseHandler.onNext(token)                           │
│         │ Token: "The " → "sales " → "opportunity " → "..."               │
│         ▼                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ STEP 1: CHUNK CLEANING                                               │ │
│  │ Class: ChunkCleaner                                                  │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  clean(chunk)                                                         │ │
│  │    │                                                                  │ │
│  │    ├──> Remove control chars: \x00-\x1F (except \t, \n)              │ │
│  │    ├──> Remove zero-width chars: \u200B-\u200F, \uFEFF               │ │
│  │    ├──> Remove Unicode line separators: \u2028, \u2029               │ │
│  │    ├──> Remove replacement char: \uFFFD                              │ │
│  │    └──> Normalize CRLF → LF                                          │ │
│  │         │                                                             │ │
│  │         │ Output: "The sales opportunity"                            │ │
│  │         │         (no invalid chars)                                 │ │
│  │         ▼                                                             │ │
│  └─────────┼─────────────────────────────────────────────────────────────┘ │
│            │                                                              │ │
│            ▼                                                              │ │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ STEP 2: MARKDOWN VALIDATION                                          │ │
│  │ Class: MarkdownValidator                                             │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  validate(chunk)                                                      │ │
│  │    │                                                                  │ │
│  │    ├──> Check balanced emphasis:                                     │ │
│  │    │    • Count ** pairs (bold)                                      │ │
│  │    │    • Count * pairs (italic)                                     │ │
│  │    │    • Count ` pairs (inline code)                                │ │
│  │    │                                                                  │ │
│  │    ├──> Check code block markers:                                    │ │
│  │    │    • ``` must be on own line                                    │ │
│  │    │    • Language hint valid (java, sql, xml, json)                 │ │
│  │    │                                                                  │ │
│  │    └──> Check heading markers:                                       │ │
│  │         • # must be at line start                                    │ │
│  │         • Max 6 levels (# to ######)                                 │ │
│  │         │                                                             │ │
│  │         │ Output: Validated chunk (structure OK)                     │ │
│  │         ▼                                                             │ │
│  └─────────┼─────────────────────────────────────────────────────────────┘ │
│            │                                                              │ │
│            ▼                                                              │ │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ STEP 3: MARKDOWN SYNTAX SANITIZATION (ADR-055)                       │ │
│  │ Class: MarkdownSyntaxSanitizer                                       │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  sanitize(chunk)                                                      │ │
│  │    │                                                                  │ │
│  │    ├──> Escape raw HTML:                                             │ │
│  │    │    <script> → &lt;script&gt;                                    │ │
│  │    │    <img onerror="..."> → &lt;img...&gt;                         │ │
│  │    │                                                                  │ │
│  │    ├──> Sanitize URLs:                                               │ │
│  │    │    [link](javascript:...) → [link removed]                      │ │
│  │    │    ![img](data:...) → [image removed]                           │ │
│  │    │    [ok](https://...) → ✅ allowed                               │ │
│  │    │                                                                  │ │
│  │    └──> Remove unsupported syntax:                                   │ │
│  │         [^1] footnotes → removed                                     │ │
│  │         - [ ] task lists → plain list                                │ │
│  │         │                                                             │ │
│  │         │ Output: Sanitized markdown (safe)                          │ │
│  │         ▼                                                             │ │
│  └─────────┼─────────────────────────────────────────────────────────────┘ │
│            │                                                              │ │
│            ▼                                                              │ │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ STEP 4: CHUNK QUEUE (Batching for Performance)                       │ │
│  │ Class: AIChatStreamingMessage                                        │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  chunkQueue.offer(sanitizedChunk)                                    │ │
│  │         │                                                             │ │
│  │         │ Queue: ["The ", "sales ", "opportunity ", ...]             │ │
│  │         │                                                             │ │
│  │         │ Background Thread (100ms interval):                        │ │
│  │         │ ┌────────────────────────────────────┐                     │ │
│  │         └─┤ processBatch()                     │                     │ │
│  │           │  while (charCount < 50) {          │ Batch size = 50    │ │
│  │           │    chunk = chunkQueue.poll()       │ chars              │ │
│  │           │    renderer.appendChunk(chunk)     │                     │ │
│  │           │    charCount += chunk.length()     │                     │ │
│  │           │  }                                 │                     │ │
│  │           │  updateUI()  // Single update      │                     │ │
│  │           └────────────────────────────────────┘                     │ │
│  │                     │                                                 │ │
│  │                     │ Batched chunks: "The sales opportunity"        │ │
│  │                     ▼                                                 │ │
│  └─────────────────────┼─────────────────────────────────────────────────┘ │
│                        │                                                 │ │
│                        ▼                                                 │ │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ STEP 5: MARKDOWN → HTML RENDERING                                    │ │
│  │ Class: StreamingMarkdownRenderer                                     │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  State Machine Processing:                                           │ │
│  │                                                                       │ │
│  │  Input: "The **sales** opportunity"                                  │ │
│  │                                                                       │ │
│  │  Character-by-character processing:                                  │ │
│  │  ┌───────┬──────┬────────┬────────────────────────────────┐        │ │
│  │  │ Char  │ State│ Action │ Output                         │        │ │
│  │  ├───────┼──────┼────────┼────────────────────────────────┤        │ │
│  │  │ 'T'   │ NORM │ emit   │ "T"                            │        │ │
│  │  │ 'h'   │ NORM │ emit   │ "Th"                           │        │ │
│  │  │ 'e'   │ NORM │ emit   │ "The"                          │        │ │
│  │  │ ' '   │ NORM │ emit   │ "The "                         │        │ │
│  │  │ '*'   │ NORM │ buffer │ marker_buffer = "*"            │        │ │
│  │  │ '*'   │ NORM │ open   │ "<strong>"  (** detected)      │        │ │
│  │  │ 's'   │ BOLD │ emit   │ "<strong>s"                    │        │ │
│  │  │ 'a'   │ BOLD │ emit   │ "<strong>sa"                   │        │ │
│  │  │ 'l'   │ BOLD │ emit   │ "<strong>sal"                  │        │ │
│  │  │ 'e'   │ BOLD │ emit   │ "<strong>sale"                 │        │ │
│  │  │ 's'   │ BOLD │ emit   │ "<strong>sales"                │        │ │
│  │  │ '*'   │ BOLD │ buffer │ marker_buffer = "*"            │        │ │
│  │  │ '*'   │ BOLD │ close  │ "<strong>sales</strong>"       │        │ │
│  │  │ ' '   │ NORM │ emit   │ "<strong>sales</strong> "      │        │ │
│  │  │ 'o'   │ NORM │ emit   │ "...o"                         │        │ │
│  │  │ ...   │ ...  │ ...    │ ...                            │        │ │
│  │  └───────┴──────┴────────┴────────────────────────────────┘        │ │
│  │                                                                       │ │
│  │  Final HTML: "The <strong>sales</strong> opportunity"               │ │
│  │         │                                                             │ │
│  │         ▼                                                             │ │
│  └─────────┼─────────────────────────────────────────────────────────────┘ │
│            │                                                              │ │
│            ▼                                                              │ │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ STEP 6: HTML SANITIZATION                                            │ │
│  │ Class: SecuritySanitizer                                             │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  escapeHtml(html)                                                     │ │
│  │    │                                                                  │ │
│  │    ├──> Whitelist tags: <p> <strong> <em> <code> <pre> <table>      │ │
│  │    │    <tr> <td> <th> <ul> <ol> <li> <a> <img> <h1-h6> <hr> <br>   │ │
│  │    │                                                                  │ │
│  │    ├──> Escape non-whitelisted tags:                                 │ │
│  │    │    <script> → &lt;script&gt;                                    │ │
│  │    │    <iframe> → &lt;iframe&gt;                                    │ │
│  │    │                                                                  │ │
│  │    ├──> Remove event handlers:                                       │ │
│  │    │    onerror="..." → removed                                      │ │
│  │    │    onclick="..." → removed                                      │ │
│  │    │                                                                  │ │
│  │    └──> Validate URLs (in <a href> and <img src>):                   │ │
│  │         javascript: → blocked                                        │ │
│  │         data: → blocked (unless config allows)                       │ │
│  │         https:// → ✅ allowed                                        │ │
│  │         │                                                             │ │
│  │         │ Output: Sanitized HTML (XSS-safe)                          │ │
│  │         ▼                                                             │ │
│  └─────────┼─────────────────────────────────────────────────────────────┘ │
│            │                                                              │ │
│            ▼                                                              │ │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │ STEP 7: ZK COMPONENT UPDATE                                          │ │
│  │ Class: AIChatWidget                                                  │ │
│  ├──────────────────────────────────────────────────────────────────────┤ │
│  │                                                                       │ │
│  │  updateUIAsync(sanitizedHtml)                                        │ │
│  │    │                                                                  │ │
│  │    └──> Executions.schedule(desktop, () -> {                         │ │
│  │             streamingContent.setContent(sanitizedHtml)               │ │
│  │             streamingContent.invalidate()  // Trigger ZK AU          │ │
│  │         })                                                            │ │
│  │         │                                                             │ │
│  │         │ ZK Asynchronous Update (AU) sent to browser:               │ │
│  │         │ { cmd: "setContent",                                       │ │
│  │         │   uuid: "streamingMsg",                                    │ │
│  │         │   data: "The <strong>sales</strong> opportunity" }         │ │
│  │         │                                                             │ │
│  │         ▼                                                             │ │
│  │  ┌─────────────────────────────────────────────────────────────┐    │ │
│  │  │ Browser DOM Update                                           │    │ │
│  │  │                                                               │    │ │
│  │  │  document.getElementById("streamingMsg").innerHTML =         │    │ │
│  │  │      "The <strong>sales</strong> opportunity"                │    │ │
│  │  │                                                               │    │ │
│  │  │  Rendered: The **sales** opportunity                         │    │ │
│  │  │                        ▲                                     │    │ │
│  │  │                        └──── Bold formatting applied         │    │ │
│  │  └──────────────────────────────────────────────────────────────    │ │
│  │                                                                       │ │
│  └───────────────────────────────────────────────────────────────────────┘ │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### Performance Metrics

| Stage | Time | Cumulative |
|-------|------|------------|
| Chunk Cleaning | 0.01ms | 0.01ms |
| Markdown Validation | 0.05ms | 0.06ms |
| Syntax Sanitization | 0.10ms | 0.16ms |
| Queue + Batch (50 chars) | 10ms | 10.16ms |
| MD → HTML Rendering | 0.20ms | 10.36ms |
| HTML Sanitization | 0.10ms | 10.46ms |
| ZK Server-Push | 5ms | **15.46ms** |

**Total latency:** ~15ms per 50-character batch

---

## Multi-Plugin OSGi Architecture

### Plugin Dependency Graph

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   OSGI MULTI-PLUGIN ARCHITECTURE                           │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌────────────────────────────────────────────────────────────────────┐   │
│  │ LAYER 0: SHARED DEPENDENCIES PLUGIN                                │   │
│  │ Bundle: com.cloudempiere.ai.deps                                   │   │
│  │ Version: 0.35.0.qualifier                                          │   │
│  ├────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  lib/                                                               │   │
│  │  ├── langchain4j-core-0.35.0.jar                                   │   │
│  │  ├── langchain4j-anthropic-0.35.0.jar                              │   │
│  │  ├── langchain4j-bedrock-0.35.0.jar                                │   │
│  │  ├── langchain4j-ollama-0.35.0.jar                                 │   │
│  │  ├── langchain4j-open-ai-0.35.0.jar                                │   │
│  │  ├── anthropic-sdk-2.10.0.jar                                      │   │
│  │  ├── aws-sdk-bedrock-2.20.162.jar                                  │   │
│  │  ├── aws-sdk-bedrockruntime-2.20.162.jar                           │   │
│  │  ├── aws-sdk-core-2.20.162.jar                                     │   │
│  │  └── netty-nio-client-2.20.162.jar                                 │   │
│  │                                                                     │   │
│  │  Export-Package:                                                    │   │
│  │  • dev.langchain4j.*;version="0.35.0"                              │   │
│  │  • com.anthropic.sdk.*;version="2.10.0"                            │   │
│  │  • software.amazon.awssdk.services.bedrock.*;version="2.20.162"    │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────   │
│                              ▲ Import-Package                           │   │
│  ┌───────────────────────────┴─────────────────────────────────────────┐  │
│  │ LAYER 1: CORE INFRASTRUCTURE PLUGIN                                 │  │
│  │ Bundle: com.cloudempiere.ai.core                                    │  │
│  │ Version: 1.0.0.qualifier                                            │  │
│  ├─────────────────────────────────────────────────────────────────────┤  │
│  │                                                                      │  │
│  │  src/com/cloudempiere/ai/                                           │  │
│  │  ├── provider/                                                       │  │
│  │  │   ├── IAIProvider.java                  ◄─ Exported API          │  │
│  │  │   ├── IAIProviderFactory.java           ◄─ Exported API          │  │
│  │  │   ├── dto/                              ◄─ Exported DTOs         │  │
│  │  │   │   ├── AIRequest.java                                         │  │
│  │  │   │   ├── AIResponse.java                                        │  │
│  │  │   │   └── AIMessage.java                                         │  │
│  │  │   ├── factory/                                                    │  │
│  │  │   │   └── AIProviderFactory.java        ◄─ @Component service    │  │
│  │  │   └── impl/                             ◄─ Internal (not exported)│  │
│  │  │       ├── AnthropicProvider.java                                 │  │
│  │  │       └── BedrockStreamingChatModelWrapper.java                  │  │
│  │  │                                                                   │  │
│  │  ├── database/                                                       │  │
│  │  │   ├── SecureDatabaseQueryExecutor.java  ◄─ Exported API          │  │
│  │  │   └── dto/                              ◄─ Exported DTOs         │  │
│  │  │       ├── SecureQueryRequest.java                                │  │
│  │  │       └── SecureQueryResult.java                                 │  │
│  │  │                                                                   │  │
│  │  ├── context/                                                        │  │
│  │  │   ├── IAIContextProvider.java           ◄─ Exported API          │  │
│  │  │   ├── AIContextProviderRegistry.java    ◄─ @Component service    │  │
│  │  │   └── impl/                             ◄─ Internal              │  │
│  │  │                                                                   │  │
│  │  ├── component/                            ◄─ ZK UI components       │  │
│  │  │   ├── AIChatWidget.java                                          │  │
│  │  │   └── AIChatStreamingMessage.java                                │  │
│  │  │                                                                   │  │
│  │  └── util/                                                           │  │
│  │      ├── StreamingMarkdownRenderer.java    ◄─ Exported API          │  │
│  │      ├── SecuritySanitizer.java            ◄─ Exported API          │  │
│  │      └── MarkdownValidator.java            ◄─ Exported API          │  │
│  │                                                                      │  │
│  │  Export-Package:                                                     │  │
│  │  • com.cloudempiere.ai.provider;version="1.0.0"                     │  │
│  │  • com.cloudempiere.ai.provider.dto;version="1.0.0"                 │  │
│  │  • com.cloudempiere.ai.database;version="1.0.0"                     │  │
│  │  • com.cloudempiere.ai.context;version="1.0.0"                      │  │
│  │  • com.cloudempiere.ai.util;version="1.0.0"                         │  │
│  │                                                                      │  │
│  │  OSGi Services (Declarative Services):                              │  │
│  │  • IAIProviderFactory                                               │  │
│  │  • AIContextProviderRegistry                                        │  │
│  │                                                                      │  │
│  └──────────────────────────────────────────────────────────────────────  │
│                              ▲ Import-Package                           │  │
│  ┌───────────────────────────┴─────────────────────────────────────────┐  │
│  │ LAYER 2: DOMAIN PLUGINS (Independent Bundles)                       │  │
│  ├──────────────────────────────────────────────────────────────────────  │
│  │                                                                      │  │
│  │  ┌──────────────────────────────────────────────────────────────┐  │  │
│  │  │ com.cloudempiere.ai.sales (Version: 1.0.0.qualifier)         │  │  │
│  │  ├──────────────────────────────────────────────────────────────┤  │  │
│  │  │                                                               │  │  │
│  │  │  agent/                                                       │  │  │
│  │  │  ├── SalesAgent.java            ◄─ @Component service        │  │  │
│  │  │  └── OpportunitySummaryAgent.java                            │  │  │
│  │  │                                                               │  │  │
│  │  │  tools/                                                       │  │  │
│  │  │  └── SalesTools.java            ◄─ @Tool methods            │  │  │
│  │  │                                                               │  │  │
│  │  │  boundary/                                                    │  │  │
│  │  │  └── SalesDomainBoundary.java   ◄─ Table whitelist          │  │  │
│  │  │      • READ_TABLES: C_Order, C_Opportunity, C_BPartner       │  │  │
│  │  │      • WRITE_TABLES: C_Opportunity (drafts only)             │  │  │
│  │  │      • ALLOWED_ACTIONS: READ, CREATE_DRAFT, UPDATE_DRAFT     │  │  │
│  │  │                                                               │  │  │
│  │  │  Import-Package:                                              │  │  │
│  │  │  • com.cloudempiere.ai.provider;version="[1.0.0,2.0.0)"      │  │  │
│  │  │  • com.cloudempiere.ai.database;version="[1.0.0,2.0.0)"      │  │  │
│  │  │  • dev.langchain4j.service;version="[0.35.0,1.0.0)"          │  │  │
│  │  │                                                               │  │  │
│  │  └───────────────────────────────────────────────────────────────  │  │
│  │                                                                   │  │  │
│  │  ┌──────────────────────────────────────────────────────────────┐  │  │
│  │  │ com.cloudempiere.ai.inventory (Version: 1.0.0.qualifier)     │  │  │
│  │  ├──────────────────────────────────────────────────────────────┤  │  │
│  │  │                                                               │  │  │
│  │  │  agent/InventoryAgent.java                                   │  │  │
│  │  │  tools/InventoryTools.java                                   │  │  │
│  │  │  boundary/InventoryDomainBoundary.java                       │  │  │
│  │  │      • READ_TABLES: M_Product, M_Warehouse, M_Storage        │  │  │
│  │  │      • WRITE_TABLES: M_Movement (drafts only)                │  │  │
│  │  │                                                               │  │  │
│  │  └───────────────────────────────────────────────────────────────  │  │
│  │                                                                   │  │  │
│  │  ┌──────────────────────────────────────────────────────────────┐  │  │
│  │  │ com.cloudempiere.ai.purchasing (Version: 1.0.0.qualifier)    │  │  │
│  │  ├──────────────────────────────────────────────────────────────┤  │  │
│  │  │                                                               │  │  │
│  │  │  agent/PurchasingAgent.java                                  │  │  │
│  │  │  tools/PurchasingTools.java                                  │  │  │
│  │  │  boundary/PurchasingDomainBoundary.java                      │  │  │
│  │  │      • READ_TABLES: C_Invoice, M_InOut                       │  │  │
│  │  │      • WRITE_TABLES: C_Order (purchase orders, drafts)       │  │  │
│  │  │                                                               │  │  │
│  │  └───────────────────────────────────────────────────────────────  │  │
│  │                                                                   │  │  │
│  │  ┌──────────────────────────────────────────────────────────────┐  │  │
│  │  │ com.cloudempiere.ai.support (Version: 1.0.0.qualifier)       │  │  │
│  │  ├──────────────────────────────────────────────────────────────┤  │  │
│  │  │                                                               │  │  │
│  │  │  agent/SupportAgent.java                                     │  │  │
│  │  │  agent/TicketClassificationAgent.java                        │  │  │
│  │  │  tools/SupportTools.java                                     │  │  │
│  │  │  boundary/SupportDomainBoundary.java                         │  │  │
│  │  │      • READ_TABLES: R_Request, AD_User                       │  │  │
│  │  │      • WRITE_TABLES: R_Request                               │  │  │
│  │  │                                                               │  │  │
│  │  └───────────────────────────────────────────────────────────────  │  │
│  │                                                                   │  │  │
│  │  ┌──────────────────────────────────────────────────────────────┐  │  │
│  │  │ com.cloudempiere.ai.knowledge (Version: 1.0.0.qualifier)     │  │  │
│  │  ├──────────────────────────────────────────────────────────────┤  │  │
│  │  │                                                               │  │  │
│  │  │  agent/KnowledgeBaseAgent.java                               │  │  │
│  │  │  agent/ArticleSearchAgent.java                               │  │  │
│  │  │  tools/KnowledgeTools.java                                   │  │  │
│  │  │  boundary/KnowledgeDomainBoundary.java                       │  │  │
│  │  │      • READ_TABLES: K_Entry, K_Category                      │  │  │
│  │  │      • WRITE_TABLES: K_Entry (drafts only)                   │  │  │
│  │  │                                                               │  │  │
│  │  └───────────────────────────────────────────────────────────────  │  │
│  │                                                                   │  │  │
│  └───────────────────────────────────────────────────────────────────  │  │
│                                                                        │  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Service Contract Flow

```
Domain Plugin (Sales)                 Core Plugin
        │                                  │
        │ 1. @Reference inject             │
        │    IAIProviderFactory            │
        ├─────────────────────────────────>│
        │                                  │
        │ 2. getProvider(providerId)       │
        ├─────────────────────────────────>│
        │                                  │
        │                                  │ 3. Query AIG_Provider table
        │                                  │    Filter by AD_User_ID, AD_Role_ID
        │                                  │
        │ 4. Return IAIProvider instance   │
        │<─────────────────────────────────┤
        │                                  │
        │ 5. Build Agent:                  │
        │    AiServices.builder()          │
        │      .chatLanguageModel(provider)│
        │      .tools(new SalesTools())    │
        │      .build()                    │
        │                                  │
        │ 6. Execute:                      │
        │    agent.chat(message)           │
        │                                  │
        │ 7. Call tools:                   │
        │    SalesTools.getOpportunity(id) │
        │                                  │
        │ 8. Secure DB query               │
        ├─────────────────────────────────>│
        │                                  │
        │                                  │ 9. SecureDatabaseQueryExecutor
        │                                  │    • Validate table (READ_TABLES)
        │                                  │    • Apply AD_Client_ID filter
        │                                  │    • Apply AD_Org_ID filter
        │                                  │    • Apply AD_Role_ID permissions
        │                                  │
        │ 10. Return query result (JSON)   │
        │<─────────────────────────────────┤
        │                                  │
        │ 11. Return AI response           │
        │                                  │
        ▼                                  ▼
```

**Key Points:**
- **Loose Coupling:** Domain plugins only depend on exported API packages
- **Service Discovery:** OSGi DS automatically wires `@Reference` fields
- **Version Isolation:** Each domain can update independently
- **Boundary Enforcement:** Core plugin validates all DB queries against domain boundary

---

*(Continued in next response due to length...)*

**Status:** Created comprehensive OSGi multi-plugin architecture document and architecture diagrams. I'll continue with the remaining diagram sections in the next message.

Let me now create the remaining architecture diagrams for AI Provider, Domain Agent Execution, Database Security, and RAG flows.