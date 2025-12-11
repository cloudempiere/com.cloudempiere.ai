# ADR-039: Chat Panel Record Zoom and Drill Integration

<!-- MADR 3.0 Template -->

## Status

**Implemented** (v0.22.0 - Phase 1 Complete)

## Date

2025-12-11

## Implementation Status

### ✅ Phase 1 Complete (v0.22.0)

**Active:** LLM-Instructed Zoom Link Format
- AI is instructed via system prompt to format record references as `[[TableName:RecordID|DisplayText]]`
- `ZoomLinkProcessor` converts this syntax to clickable HTML links
- Uses iDempiere standard `zAu.send()` + `MQuery` zoom pattern

**Deferred:** Pattern-Based Extraction
- `RecordReferenceExtractor` and `ChatRecordLinkRenderer` are implemented but bypassed
- Requires vector DB for fast lookup across 2000+ tables/AD elements
- Will be enabled when embedding/caching infrastructure is in place (see ADR-040)

## Deciders

Cloudempiere AI Team

## Context and Problem Statement

The AI chat panel displays responses containing references to iDempiere records (orders, invoices, customers, products, etc.). Currently, users cannot click on these references to navigate directly to the record in the ERP system. This breaks the natural workflow where users expect to drill into details after receiving AI insights. Without clickable record links, users must manually search for records mentioned in AI responses, reducing productivity and creating friction in the user experience.

iDempiere has a mature zoom/drill infrastructure (`AEnv.zoom()`, `MQuery`, `ZoomInfoFactory`, `WZoomAcross`) that we should leverage rather than reinvent.

## Decision Drivers

- **User Experience**: Users expect clickable links to records mentioned in chat responses
- **Consistency**: Must use iDempiere's existing zoom/drill patterns for familiar UX
- **Security**: Record access must respect role-based permissions (existing via MRole)
- **Context Preservation**: Users should be able to return to chat after drilling into a record
- **Multi-Target Support**: Some references may have multiple valid zoom targets (zoom-across)
- **Performance**: Link generation should not significantly impact response rendering time

## Considered Options

1. **Native iDempiere Zoom Integration** - Use `AEnv.zoom()` with `MQuery`
2. **Custom Window Navigation** - Build separate navigation system
3. **URL-Based Deep Links** - Generate HTTP URLs for record access

## Decision Outcome

**Chosen option:** "Native iDempiere Zoom Integration", because it leverages the existing, well-tested iDempiere zoom infrastructure, provides consistent UX with rest of application, automatically respects role-based access control, and supports zoom-across for records with multiple target windows.

### Confirmation

The decision will be confirmed when:
- [ ] `RecordReference` DTO captures table/record metadata from AI responses
- [ ] `ChatRecordLinkRenderer` generates clickable ZK components in chat messages
- [ ] Clicking a record link opens the correct iDempiere window via `AEnv.zoom()`
- [ ] Zoom-across popup appears when multiple targets exist
- [ ] Role-based access is enforced (unauthorized records show appropriate message)
- [ ] User can navigate back to chat panel after viewing record
- [ ] Performance: Link rendering adds <50ms to message display

## Pros and Cons of the Options

### Option 1: Native iDempiere Zoom Integration

Use iDempiere's existing `AEnv.zoom()`, `MQuery`, and `ZoomInfoFactory` infrastructure.

- Good, because uses battle-tested zoom/drill code from iDempiere core
- Good, because automatically respects role-based access control
- Good, because supports zoom-across for multi-target scenarios
- Good, because consistent UX with rest of iDempiere application
- Good, because leverages `MZoomCondition` for smart window selection
- Neutral, because requires chat panel to be within ZK desktop context
- Bad, because tightly coupled to ZK web UI (not usable in headless scenarios)

### Option 2: Custom Window Navigation

Build a separate navigation system specific to chat panel.

- Good, because full control over navigation behavior
- Good, because could support non-ZK clients (REST API, mobile)
- Bad, because duplicates existing functionality
- Bad, because must re-implement access control checks
- Bad, because inconsistent with rest of application
- Bad, because significant development effort

### Option 3: URL-Based Deep Links

Generate HTTP URLs that open specific records (e.g., `/webui?Action=Zoom&AD_Table_ID=259&Record_ID=123`).

- Good, because works across browser tabs
- Good, because can be shared/bookmarked
- Bad, because requires URL routing infrastructure
- Bad, because security concerns with URL parameter manipulation
- Bad, because loses ZK session context
- Bad, because no zoom-across support

## More Information

### Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Chat Panel Record Zoom Flow                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  AI Response Text                                                    │
│  "Found 3 pending orders: SO-1234, SO-1235, SO-1236"                │
│       │                                                              │
│       ▼                                                              │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  RecordReferenceExtractor                                     │   │
│  │  - Pattern matching for record identifiers                    │   │
│  │  - LLM-provided structured references (preferred)             │   │
│  │  - Resolves DocumentNo → Record_ID via lookup                 │   │
│  └───────────────────────────┬──────────────────────────────────┘   │
│                              │                                       │
│                              ▼                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  RecordReference DTO                                          │   │
│  │  - tableName: "C_Order"                                       │   │
│  │  - recordId: 1234                                             │   │
│  │  - displayValue: "SO-1234"                                    │   │
│  │  - columnName: "C_Order_ID" (for MQuery)                      │   │
│  │  - windowId: (optional, for direct window targeting)          │   │
│  └───────────────────────────┬──────────────────────────────────┘   │
│                              │                                       │
│                              ▼                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  ChatRecordLinkRenderer                                       │   │
│  │  - Replaces text references with clickable A/Label components │   │
│  │  - Attaches onClick listeners for zoom                        │   │
│  │  - Validates access before rendering (optional optimization)  │   │
│  └───────────────────────────┬──────────────────────────────────┘   │
│                              │                                       │
│                              ▼                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  User Click Event                                             │   │
│  │                                                               │   │
│  │  if (singleTarget) {                                          │   │
│  │      MQuery query = createZoomQuery(ref);                     │   │
│  │      AEnv.zoom(query);  // Opens window                       │   │
│  │  } else {                                                     │   │
│  │      // Multiple targets - show zoom-across popup             │   │
│  │      PO po = new Query(...).first();                          │   │
│  │      new WZoomAcross(component, po, windowId);                │   │
│  │  }                                                            │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| `RecordReference` | DTO for record metadata | `dto/` |
| `RecordReferenceExtractor` | Extracts references from AI response | `conversation/` |
| `ChatRecordLinkRenderer` | Renders clickable links in ZK | `ui/` |
| `ZoomActionHandler` | Executes zoom via AEnv | `ui/` |

### RecordReference DTO

```java
/**
 * Represents a reference to an iDempiere record that can be zoomed to.
 */
public class RecordReference {

    private final String tableName;      // e.g., "C_Order"
    private final int recordId;          // e.g., 1234
    private final String displayValue;   // e.g., "SO-1234" (shown to user)
    private final String columnName;     // e.g., "C_Order_ID" (for MQuery)
    private final Integer windowId;      // Optional: specific AD_Window_ID
    private final int startIndex;        // Position in original text
    private final int endIndex;          // End position in original text

    // Builder pattern for construction
    public static Builder builder() { return new Builder(); }

    /**
     * Create MQuery for zoom operation.
     */
    public MQuery toMQuery() {
        MQuery query = new MQuery(tableName);
        query.addRestriction(columnName, MQuery.EQUAL, recordId);
        query.setZoomTableName(tableName);
        query.setZoomColumnName(columnName);
        query.setZoomValue(recordId);
        if (windowId != null) {
            query.setZoomWindowID(windowId);
        }
        return query;
    }

    /**
     * Check if user has access to this record.
     */
    public boolean isAccessible(int AD_Role_ID) {
        MRole role = MRole.get(Env.getCtx(), AD_Role_ID);
        return role.isTableAccess(MTable.getTable_ID(tableName), false);
    }
}
```

### LLM-Instructed Zoom Link Format (Active Implementation)

The AI is instructed via system prompt to format record references using a simple bracket syntax:

**Syntax:** `[[TableName:RecordID|DisplayText]]`

**System Prompt (ERPAgent.SYSTEM_PROMPT):**
```
RECORD REFERENCE FORMAT (IMPORTANT):
When mentioning specific records (orders, customers, products, invoices, etc.),
format them as clickable links using this syntax: [[TableName:RecordID|DisplayText]]

Examples:
- Business Partner: [[C_BPartner:1000001|Acme Corporation]]
- Sales Order: [[C_Order:5678|SO-50001]]
- Purchase Order: [[C_Order:5679|PO-10023]]
- Invoice: [[C_Invoice:1234|INV-2024-001]]
- Product: [[M_Product:100|Widget A]]
- Payment: [[C_Payment:999|PAY-2024-001]]

This enables users to click and navigate directly to the record in iDempiere.
```

**Processing Flow:**
1. AI returns text with embedded `[[Table:ID|Text]]` references
2. `ZoomLinkProcessor.processZoomLinks()` parses the syntax via regex
3. Converts to HTML `<a>` elements with `onclick="zAu.send(...)"`
4. Click triggers `onZoom` event handled by `AIChatWidget.handleZoomEvent()`
5. `AEnv.zoom(MQuery)` opens the record in appropriate window

**Key Files:**
- `ERPAgent.java` - System prompt with format instructions
- `ZoomLinkProcessor.java` - Regex parser and HTML generator
- `AIChatWidget.java` - Event handler calling `AEnv.zoom()`

### Fallback: Pattern-Based Extraction (Deferred)

> **Note:** This feature is implemented but currently bypassed. Requires vector DB for
> fast lookup across 2000+ tables. See ADR-040 for embedding infrastructure plans.

When LLM doesn't provide structured references, use pattern matching:

```java
/**
 * Extract record references from text using patterns.
 */
public class RecordReferenceExtractor {

    // Common iDempiere document patterns
    private static final Map<Pattern, String> PATTERNS = Map.of(
        Pattern.compile("\\b(SO|Sales Order)[- ]?(\\d+)\\b", Pattern.CASE_INSENSITIVE),
            "C_Order",
        Pattern.compile("\\b(PO|Purchase Order)[- ]?(\\d+)\\b", Pattern.CASE_INSENSITIVE),
            "C_Order",  // with IsSOTrx filter
        Pattern.compile("\\b(INV|Invoice)[- ]?(\\d+)\\b", Pattern.CASE_INSENSITIVE),
            "C_Invoice",
        Pattern.compile("\\b(BP|Customer|Vendor)[- ]?(\\d+)\\b", Pattern.CASE_INSENSITIVE),
            "C_BPartner",
        Pattern.compile("\\b(PROD|Product)[- ]?(\\d+)\\b", Pattern.CASE_INSENSITIVE),
            "M_Product"
    );

    /**
     * Extract references from plain text response.
     */
    public List<RecordReference> extract(String text) {
        List<RecordReference> refs = new ArrayList<>();

        for (Map.Entry<Pattern, String> entry : PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(text);
            while (matcher.find()) {
                String docNo = matcher.group(2);
                String tableName = entry.getValue();

                // Lookup Record_ID from DocumentNo
                int recordId = lookupRecordId(tableName, docNo);
                if (recordId > 0) {
                    refs.add(RecordReference.builder()
                        .tableName(tableName)
                        .recordId(recordId)
                        .displayValue(matcher.group(0))
                        .columnName(tableName + "_ID")
                        .startIndex(matcher.start())
                        .endIndex(matcher.end())
                        .build());
                }
            }
        }

        return refs;
    }

    private int lookupRecordId(String tableName, String documentNo) {
        return new Query(Env.getCtx(), tableName, "DocumentNo=?", null)
            .setParameters(documentNo)
            .setClient_ID()
            .firstIdOnly();
    }
}
```

### ZK UI Rendering

```java
/**
 * Renders AI response text with clickable record links.
 */
public class ChatRecordLinkRenderer {

    /**
     * Convert plain text with references to ZK component tree.
     */
    public Component render(String text, List<RecordReference> refs) {
        if (refs.isEmpty()) {
            return new Label(text);
        }

        Div container = new Div();
        container.setSclass("ai-message-content");

        // Sort refs by position
        refs.sort(Comparator.comparing(RecordReference::getStartIndex));

        int lastEnd = 0;
        for (RecordReference ref : refs) {
            // Add text before this reference
            if (ref.getStartIndex() > lastEnd) {
                container.appendChild(new Label(
                    text.substring(lastEnd, ref.getStartIndex())));
            }

            // Add clickable link for reference
            A link = createRecordLink(ref);
            container.appendChild(link);

            lastEnd = ref.getEndIndex();
        }

        // Add remaining text
        if (lastEnd < text.length()) {
            container.appendChild(new Label(text.substring(lastEnd)));
        }

        return container;
    }

    private A createRecordLink(RecordReference ref) {
        A link = new A(ref.getDisplayValue());
        link.setSclass("ai-record-link");
        link.setStyle("color: #0066cc; text-decoration: underline; cursor: pointer;");
        link.setTooltiptext("Click to view " + ref.getTableName() + " record");

        link.addEventListener(Events.ON_CLICK, event -> {
            executeZoom(ref, link);
        });

        return link;
    }

    private void executeZoom(RecordReference ref, Component invoker) {
        // Check access
        int roleId = Env.getAD_Role_ID(Env.getCtx());
        if (!ref.isAccessible(roleId)) {
            Messagebox.show("You do not have access to this record.",
                "Access Denied", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        // Check for multiple zoom targets
        PO po = MTable.get(Env.getCtx(), ref.getTableName())
            .getPO(ref.getRecordId(), null);

        if (po != null) {
            List<ZoomInfo> zoomInfos = ZoomInfoFactory.retrieveZoomInfos(po, 0);

            if (zoomInfos.size() > 1) {
                // Multiple targets - show zoom-across popup
                new WZoomAcross(invoker, po, 0);
            } else {
                // Single target - direct zoom
                MQuery query = ref.toMQuery();
                AEnv.zoom(query);
            }
        }
    }
}
```

### CSS Styling

```css
/* AI Chat Record Links */
.ai-record-link {
    color: #0066cc;
    text-decoration: underline;
    cursor: pointer;
    padding: 0 2px;
    border-radius: 2px;
    transition: background-color 0.2s;
}

.ai-record-link:hover {
    background-color: #e6f0ff;
    text-decoration: none;
}

.ai-record-link:active {
    background-color: #cce0ff;
}

/* Table-specific icons (optional enhancement) */
.ai-record-link[data-table="C_Order"]::before {
    content: "\f07a"; /* shopping cart icon */
    font-family: FontAwesome;
    margin-right: 4px;
}

.ai-record-link[data-table="C_BPartner"]::before {
    content: "\f007"; /* user icon */
    font-family: FontAwesome;
    margin-right: 4px;
}
```

### Drill-Down Support

For reports and charts, support drill-down to detail records:

```java
/**
 * Handle drill-down from chart data points.
 */
public class ChartDrillHandler {

    /**
     * Create drill query for chart segment.
     */
    public MQuery createDrillQuery(String columnName, Object value) {
        String tableName = MQuery.getZoomTableName(columnName);

        MQuery query = new MQuery(tableName);
        query.addRestriction(columnName, MQuery.EQUAL, value);
        query.setZoomColumnName(columnName);
        query.setZoomValue(value);

        return query;
    }

    /**
     * Execute drill from AI-explained chart.
     */
    public void drillToSegment(String segmentLabel, String columnName,
                               Object value, Component invoker) {
        MQuery query = createDrillQuery(columnName, value);

        // Find best window for this table
        int windowId = Env.getZoomWindowID(query);

        if (windowId > 0) {
            AEnv.zoom(windowId, query);
        } else {
            // Fallback: show info window
            AEnv.zoom(query);
        }
    }
}
```

### Integration with ADR-015 Response Format

Update the response format from ADR-015 to support structured references:

```markdown
# Updated Response Structure (ADR-015 + ADR-039)

**Acknowledgment** (1 line)
I found 3 pending orders totaling $31,400.

**Data Display** (with record references)
| Order | Customer | Amount | Status |
|-------|----------|--------|--------|
| [SO-1234](#record:C_Order:1234) | Acme Corp | $12,500 | Pending |
| [SO-1235](#record:C_Order:1235) | TechCo | $8,900 | Pending |
| [SO-1236](#record:C_Order:1236) | GlobalInc | $10,000 | Pending |

**Quick Actions**
- [View all pending orders](#zoom:C_Order?DocStatus=IP)
- [Customer credit report](#process:123?C_BPartner_ID=456)
```

### Implementation Phases

**Phase 1 (3 days):** Core Infrastructure
- `RecordReference` DTO
- `RecordReferenceExtractor` with pattern matching
- Basic `ChatRecordLinkRenderer` with single-target zoom

**Phase 2 (2 days):** Enhanced Navigation
- Zoom-across support for multi-target records
- Access control validation with user-friendly messages
- CSS styling and visual feedback

**Phase 3 (2 days):** LLM Integration
- Update system prompts to request structured references
- JSON parsing for LLM-provided references
- Fallback to pattern extraction when not provided

**Phase 4 (1 day):** Drill-Down Support
- `ChartDrillHandler` for chart segment drilling
- Integration with `ChartContextProvider` (ADR-017)

**Total Effort:** 8 days

### Security Considerations

1. **Role-Based Access**: Always check `MRole.isTableAccess()` before rendering clickable links
2. **Record-Level Security**: Use `MRole.addAccessSQL()` when looking up records
3. **No ID Exposure**: Display DocumentNo/Name to users, resolve to ID server-side
4. **Audit Trail**: Log zoom actions initiated from chat panel for compliance

### Related ADRs

- [ADR-015](015-conversational-ux-patterns.md) - Response formatting includes action links
- [ADR-017](017-chart-executive-overview.md) - Chart drill-down integration
- [ADR-007](007-database-security-model.md) - Security model for record access
- [ADR-033](033-streaming-thinking-timeline-ux.md) - UI rendering integration

### References

- iDempiere Core: `org.adempiere.webui.apps.AEnv` (zoom methods)
- iDempiere Core: `org.compiere.model.MQuery` (query descriptor)
- iDempiere Core: `org.adempiere.model.ZoomInfoFactory` (zoom target discovery)
- iDempiere Core: `org.adempiere.webui.WZoomAcross` (multi-target popup)
- [ADR-015 ActionLinkGenerator](015-conversational-ux-patterns.md#idempiere-integration)
