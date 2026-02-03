# AI Chat Streaming - Sequence Diagrams

## Diagram 1: Complete Streaming Flow (Fixed Architecture)

```mermaid
sequenceDiagram
    participant AI as AI Provider
    participant Widget as AIChatWidget
    participant Stream as AIChatStreamingMessage
    participant Validator as MarkdownValidator
    participant Buffer as StreamingTextBuffer
    participant TableR as StreamingTableRenderer
    participant MarkdownR as StreamingMarkdownRenderer
    participant DOM as Browser DOM

    Note over AI,DOM: Phase 1: Chunk Arrival & Validation

    AI->>Widget: onChunk("**bold text")
    Widget->>Stream: appendChunk(chunk)
    Stream->>Stream: Check state (cancelled/complete?)

    Note over Stream: Validation Pipeline (NEW!)
    Stream->>Stream: ChunkCleaner.clean(chunk)
    Stream->>Validator: validate(cleaned)
    Validator->>Validator: Fix unclosed markers
    Validator->>Validator: Reorder invalid nesting
    Validator-->>Stream: Fixed markdown

    Note over Stream: Batching (ADR-047)
    Stream->>Stream: Queue chunk
    Stream->>Stream: Schedule 50ms timer (if not pending)

    Note over AI,DOM: Phase 2: Batched Rendering (every 50ms)

    Stream->>Stream: onBatchRender() [Timer fires]

    loop For each queued chunk
        Stream->>Buffer: append(validatedChunk)
        Stream->>TableR: appendChunk(validatedChunk)
        Stream->>MarkdownR: appendChunk(validatedChunk)
    end

    Note over Stream: Renderer Selection (FIXED!)

    alt Table renderer ACTIVE (isInTable = true)
        Stream->>TableR: renderCurrentState()
        TableR-->>Stream: HTML (table cells)
    else Markdown renderer (pre/post table)
        Stream->>MarkdownR: renderCurrentState()
        MarkdownR-->>Stream: HTML (progressive MD)

        alt Table exists but not active (past table)
            Stream->>TableR: renderFinal()
            TableR-->>Stream: Complete table HTML
            Stream->>Stream: Merge: table + markdown
        end
    end

    Stream->>DOM: Update HTML content
    DOM->>DOM: Browser renders

    Note over AI,DOM: Phase 3: Completion

    AI->>Widget: onComplete()
    Widget->>Stream: complete()

    Stream->>Stream: Flush pending chunks
    Stream->>Stream: renderFinalMarkdown()

    alt Table was streamed
        Stream->>TableR: renderFinal()
        TableR-->>Stream: Final table HTML
    else Markdown was streamed
        Stream->>MarkdownR: renderFinal()
        MarkdownR-->>Stream: Final markdown HTML
        Stream->>Stream: ZoomLinkProcessor.processZoomLinks()
    end

    Stream->>Stream: captureRenderedHtml() [ADR-054]
    Stream->>Widget: getRenderedHtml()
    Widget->>Widget: Save to database (CM_ChatEntry)
```

---

## Diagram 2: Renderer Competition (BEFORE vs AFTER)

### BEFORE (BROKEN) ❌

```mermaid
sequenceDiagram
    participant Stream as AIChatStreamingMessage
    participant TableR as TableRenderer
    participant MarkdownR as MarkdownRenderer

    Note over Stream: Chunk 1: "Pre-table text"
    Stream->>MarkdownR: appendChunk("Pre-table")
    Stream->>Stream: updateContentDisplay()
    Stream->>MarkdownR: renderCurrentState()
    MarkdownR-->>Stream: <p>Pre-table text</p>

    Note over Stream: Chunk 2: "| Table |"
    Stream->>TableR: appendChunk("| Table |")
    TableR->>TableR: DETECT TABLE!
    TableR->>TableR: hasContent() = TRUE
    Stream->>Stream: updateContentDisplay()

    alt tableRenderer.hasContent() ← ALWAYS TRUE NOW!
        Stream->>TableR: renderCurrentState()
        TableR-->>Stream: <table>...</table>
    end

    Note over Stream: Chunk 3: "**Post-table bold**"
    Stream->>MarkdownR: appendChunk("**bold**")
    Stream->>TableR: appendChunk("**bold**")
    Stream->>Stream: updateContentDisplay()

    alt tableRenderer.hasContent() ← STILL TRUE!
        Stream->>TableR: renderCurrentState()
        Note over TableR: Treats "**bold**" as PLAIN TEXT!
        TableR-->>Stream: <table>...</table>**bold**
    end

    Note right of Stream: ❌ BROKEN: Bold not rendered!
```

### AFTER (FIXED) ✅

```mermaid
sequenceDiagram
    participant Stream as AIChatStreamingMessage
    participant TableR as TableRenderer
    participant MarkdownR as MarkdownRenderer

    Note over Stream: Chunk 1: "Pre-table text"
    Stream->>MarkdownR: appendChunk("Pre-table")
    Stream->>Stream: updateContentDisplay()
    Stream->>MarkdownR: renderCurrentState()
    MarkdownR-->>Stream: <p>Pre-table text</p>

    Note over Stream: Chunk 2: "| Table |"
    Stream->>TableR: appendChunk("| Table |")
    TableR->>TableR: ENTER TABLE!
    TableR->>TableR: isInTable() = TRUE
    Stream->>Stream: updateContentDisplay()

    alt tableRenderer.isInTable() ← CHECK ACTIVE STATE
        Stream->>TableR: renderCurrentState()
        TableR-->>Stream: <table>...</table>
    end

    Note over Stream: Chunk 3: "**Post-table bold**"
    Stream->>TableR: appendChunk("**bold**")
    TableR->>TableR: EXIT TABLE!
    TableR->>TableR: isInTable() = FALSE
    Stream->>MarkdownR: appendChunk("**bold**")
    Stream->>Stream: updateContentDisplay()

    alt tableRenderer.isInTable() ← FALSE!
        Note over Stream: Switch to markdown renderer
        Stream->>MarkdownR: renderCurrentState()
        MarkdownR-->>Stream: <strong>Post-table bold</strong>

        alt tableRenderer.hasContent() AND NOT isInTable()
            Stream->>TableR: renderFinal()
            TableR-->>Stream: <table>...</table>
            Stream->>Stream: Merge: table + markdown
        end
    end

    Note right of Stream: ✅ FIXED: Bold rendered correctly!
```

---

## Diagram 3: Markdown Validation Flow

```mermaid
sequenceDiagram
    participant AI as AI Provider
    participant Widget as AIChatWidget
    participant Stream as AIChatStreamingMessage
    participant Validator as MarkdownValidator
    participant Renderer as MarkdownRenderer

    Note over AI: AI generates: "**bold *italic"
    Note over AI: (Invalid - unclosed markers)

    AI->>Widget: onChunk("**bold *italic")
    Widget->>Stream: appendChunk(chunk)

    Note over Stream: Validation Pipeline
    Stream->>Stream: ChunkCleaner.clean(chunk)
    Stream->>Validator: validate(cleaned)

    Note over Validator: Step 1: Detect markers
    Validator->>Validator: Scan for **, *, `, #
    Validator->>Validator: Track open/close pairs

    Note over Validator: Step 2: Find issues
    Validator->>Validator: "**" at pos 0 (OPEN)
    Validator->>Validator: "*" at pos 6 (OPEN)
    Validator->>Validator: End of string: 2 UNCLOSED!

    Note over Validator: Step 3: Fix issues (LIFO order)
    Validator->>Validator: Close "*" → "**bold *italic*"
    Validator->>Validator: Close "**" → "**bold *italic***"

    Validator-->>Stream: "**bold *italic***"

    Stream->>Renderer: appendChunk(fixed)
    Renderer->>Renderer: Process progressively
    Renderer-->>Stream: <strong>bold <em>italic</em></strong>

    Note right of Renderer: ✅ Correctly rendered!
```

---

## Diagram 4: Progressive Markdown Rendering

```mermaid
sequenceDiagram
    participant Stream as AIChatStreamingMessage
    participant Renderer as StreamingMarkdownRenderer
    participant DOM as Browser DOM

    Note over Stream: Streaming: "**b", "o", "l", "d", "**"

    Stream->>Renderer: appendChunk("**b")
    Renderer->>Renderer: Detect "**" (opening)
    Renderer->>Renderer: State: NORMAL → IN_BOLD
    Renderer->>Renderer: Emit: <strong>b
    Renderer-->>DOM: <strong>b

    Stream->>Renderer: appendChunk("o")
    Renderer->>Renderer: State: IN_BOLD
    Renderer->>Renderer: Emit: o
    Renderer-->>DOM: <strong>bo

    Stream->>Renderer: appendChunk("l")
    Renderer->>Renderer: State: IN_BOLD
    Renderer->>Renderer: Emit: l
    Renderer-->>DOM: <strong>bol

    Stream->>Renderer: appendChunk("d")
    Renderer->>Renderer: State: IN_BOLD
    Renderer->>Renderer: Emit: d
    Renderer-->>DOM: <strong>bold

    Stream->>Renderer: appendChunk("**")
    Renderer->>Renderer: Detect "**" (closing)
    Renderer->>Renderer: State: IN_BOLD → NORMAL
    Renderer->>Renderer: Emit: </strong>
    Renderer-->>DOM: <strong>bold</strong>

    Note right of DOM: User sees formatting<br/>appear progressively!
```

---

## Diagram 5: Heading Validation (FIXED)

```mermaid
flowchart TD
    A[Chunk arrives: "###"] --> B{At line start?}
    B -->|Yes| C[Buffer: "###"]
    C --> D[Next chunk: " " or TAB or newline]

    D --> E{Space/Tab?}
    E -->|Yes| F[Valid heading!]
    F --> G[Open: <h3>]
    G --> H[State: IN_HEADING]

    E -->|Newline| I[Empty heading!]
    I --> J[Render: <h3></h3><br/>]
    J --> K[State: NORMAL]

    D --> L{Other char?}
    L -->|Yes| M[Invalid heading]
    M --> N[Emit literal: "###"]
    N --> K

    B -->|No| O[Not heading context]
    O --> N

    style F fill:#90EE90
    style I fill:#FFD700
    style M fill:#FF6B6B
```

---

## Diagram 6: State Machine (StreamingState)

```mermaid
stateDiagram-v2
    [*] --> IDLE

    IDLE --> WAITING_FOR_LLM: transitionTo()

    WAITING_FOR_LLM --> THINKING: onThinking()
    WAITING_FOR_LLM --> STREAMING_TEXT: onChunk()
    WAITING_FOR_LLM --> STREAMING_TABLE: onChunk(table)
    WAITING_FOR_LLM --> TOOL_EXECUTING: onToolStart()
    WAITING_FOR_LLM --> ERROR: onError()
    WAITING_FOR_LLM --> CANCELLED: cancel()

    THINKING --> STREAMING_TEXT: onChunk()
    THINKING --> STREAMING_TABLE: onChunk(table)
    THINKING --> ERROR: onError()
    THINKING --> CANCELLED: cancel()

    TOOL_EXECUTING --> STREAMING_TEXT: onToolComplete()
    TOOL_EXECUTING --> STREAMING_TABLE: onToolComplete()
    TOOL_EXECUTING --> TOOL_EXECUTING: onToolStart()
    TOOL_EXECUTING --> COMPLETE: onComplete()
    TOOL_EXECUTING --> ERROR: onError()
    TOOL_EXECUTING --> CANCELLED: cancel()

    STREAMING_TEXT --> STREAMING_TABLE: tableRenderer.isInTable()
    STREAMING_TEXT --> FINALIZING: complete()
    STREAMING_TEXT --> ERROR: onError()
    STREAMING_TEXT --> CANCELLED: cancel()

    STREAMING_TABLE --> STREAMING_TEXT: !tableRenderer.isInTable()
    STREAMING_TABLE --> FINALIZING: complete()
    STREAMING_TABLE --> ERROR: onError()
    STREAMING_TABLE --> CANCELLED: cancel()

    FINALIZING --> COMPLETE: renderFinalMarkdown()
    FINALIZING --> ERROR: onError()

    COMPLETE --> [*]
    CANCELLED --> [*]
    ERROR --> [*]

    note right of STREAMING_TEXT
        FIXED: Can transition
        to STREAMING_TABLE
        and back!
    end note
```

---

## Diagram 7: Error Handling & Recovery

```mermaid
sequenceDiagram
    participant AI as AI Provider
    participant Widget as AIChatWidget
    participant Stream as AIChatStreamingMessage
    participant Validator as MarkdownValidator
    participant Renderer as MarkdownRenderer

    Note over AI: AI sends malformed chunk

    AI->>Widget: onChunk("<script>alert('xss')")
    Widget->>Stream: appendChunk(chunk)

    Stream->>Stream: ChunkCleaner.clean(chunk)
    Stream->>Validator: validate(cleaned)

    alt Validation succeeds
        Validator-->>Stream: Fixed markdown
        Stream->>Renderer: appendChunk(fixed)
        Renderer-->>Stream: Safe HTML
    else Validation fails (exception)
        Validator-->>Stream: Original markdown (fail-safe)
        Note over Validator: Log warning
        Stream->>Renderer: appendChunk(original)
        Renderer->>Renderer: Escape HTML
        Renderer-->>Stream: &lt;script&gt;...
    end

    Stream->>DOM: Update (safe HTML only)

    Note right of DOM: ✅ XSS prevented!
```

---

## Diagram 8: Performance Timeline

```
Time →
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
│ 0ms │ 10  │ 20  │ 30  │ 40  │ 50  │ 60  │ 70  │
└─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘

Chunks arrive:
┌─┐   ┌─┐ ┌─┐   ┌─┐     ┌─┐ ┌─┐ ┌─┐
│1│   │2│ │3│   │4│     │5│ │6│ │7│
└─┘   └─┘ └─┘   └─┘     └─┘ └─┘ └─┘
 ↓     ↓   ↓     ↓       ↓   ↓   ↓
Queue Queue Queue      Queue Queue Queue

Render batches (every 50ms):
        ↓                       ↓
      [1,2,3]                 [4,5,6,7]
        ↓                       ↓
    Validate                Validate
        ↓                       ↓
    Render                  Render
        ↓                       ↓
   Update DOM             Update DOM

Before fix: 7 DOM updates (one per chunk)
After fix:  2 DOM updates (batched)

Performance improvement: 71% reduction! 🚀
```

---

## Legend

### Symbols Used

- **→**: Sequential flow
- **↓**: Data/control flow
- **⇄**: Bidirectional flow
- **✅**: Success/Fixed
- **❌**: Failure/Broken
- **⚠️**: Warning/Caution

### Component Abbreviations

- **AI**: AI Provider (Anthropic Claude)
- **Widget**: AIChatWidget (ZK component)
- **Stream**: AIChatStreamingMessage (streaming handler)
- **Validator**: MarkdownValidator (validation utility)
- **TableR**: StreamingTableRenderer (table streaming)
- **MarkdownR**: StreamingMarkdownRenderer (markdown streaming)
- **DOM**: Browser Document Object Model

---

## Usage Notes

### Viewing Mermaid Diagrams

1. **GitHub**: Automatically renders Mermaid in markdown
2. **VS Code**: Install "Markdown Preview Mermaid Support" extension
3. **IntelliJ**: Built-in Mermaid support in markdown preview
4. **Online**: https://mermaid.live (paste diagram code)

### Diagram Updates

When updating architecture:
1. Update corresponding diagram
2. Update STREAMING_ARCHITECTURE_FIX.md
3. Regenerate from source if using diagram tool
4. Commit both .md and diagram sources

---

## References

- **Mermaid Documentation**: https://mermaid.js.org
- **Sequence Diagrams**: https://mermaid.js.org/syntax/sequenceDiagram.html
- **State Diagrams**: https://mermaid.js.org/syntax/stateDiagram.html
- **Flowcharts**: https://mermaid.js.org/syntax/flowchart.html
