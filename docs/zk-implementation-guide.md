# ZK Implementation Guide for AI Use Cases

This guide explains how to implement AI use cases (ADR-017 to ADR-020) using iDempiere ZK components, building on the existing `AIChatWidget` infrastructure.

---

## ASCII Diagrams

### AIChatWidget UI Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        AIChatWidget (extends Div)                           │
│  sclass="ai-chat-widget"                                                    │
│  style="display: flex; flex-direction: column; height: 700px"               │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │  Context Indicator (Html) - optional, shown when contextEnabled=true  │  │
│  │  "Context: Sales Order > Header"                                      │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌─────────────────────────────────────────────┬─────────────────────────┐  │
│  │  Thread Selector (Combobox)                 │  New Thread (Button)    │  │
│  │  [▼ Select conversation...              ]   │  [+ New]                │  │
│  └─────────────────────────────────────────────┴─────────────────────────┘  │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                                                                       │  │
│  │  Messages Container (Vlayout) - sclass="ai-messages"                  │  │
│  │  style="overflow-y: auto; flex: 1 1 auto"                             │  │
│  │                                                                       │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │ User Message (Div) - sclass="user-message"                      │  │  │
│  │  │ ┌─────────────────────────────────────────────────────────────┐ │  │  │
│  │  │ │ What are my top 5 sales orders this month?                  │ │  │  │
│  │  │ └─────────────────────────────────────────────────────────────┘ │  │  │
│  │  │ [Copy]                                                          │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                       │  │
│  │  ─────────────────────────────────────────────────────────────────────│  │
│  │                                                                       │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │ AI Message (Div) - sclass="ai-message"                          │  │  │
│  │  │ ┌─────────────────────────────────────────────────────────────┐ │  │  │
│  │  │ │ [🤖] AI Assistant                                           │ │  │  │
│  │  │ └─────────────────────────────────────────────────────────────┘ │  │  │
│  │  │ ┌─────────────────────────────────────────────────────────────┐ │  │  │
│  │  │ │ Here are your top 5 sales orders:                           │ │  │  │
│  │  │ │                                                             │ │  │  │
│  │  │ │ | Order     | Customer    | Amount    |                     │ │  │  │
│  │  │ │ |-----------|-------------|-----------|                     │ │  │  │
│  │  │ │ | SO-1234   | Acme Corp   | $15,000   |  ← Clickable zoom   │ │  │  │
│  │  │ │ | SO-1235   | Tech Inc    | $12,500   |                     │ │  │  │
│  │  │ │ | ...       | ...         | ...       |                     │ │  │  │
│  │  │ │                                                             │ │  │  │
│  │  │ │ (Rendered via marked.js + Prism.js)                         │ │  │  │
│  │  │ └─────────────────────────────────────────────────────────────┘ │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                       │  │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │  │
│  │  │ Loading Indicator (Html) - hidden by default                    │  │  │
│  │  │ "AI is thinking..."                                             │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                       │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────┬────────────┐  │
│  │  Input Box (Textbox)                                     │ Send (Btn) │  │
│  │  [Ask anything about your business data...           ]   │    [➤]     │  │
│  └──────────────────────────────────────────────────────────┴────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Component Hierarchy

```
AIChatWidget (Div)
│
├── contextIndicator (Html) ─────────────────── Optional context info
│
├── threadControlBar (Hlayout)
│   ├── threadSelector (Combobox) ───────────── Thread dropdown
│   └── newThreadButton (Button) ────────────── "+ New" button
│
├── messagesContainer (Vlayout) ─────────────── Scrollable messages area
│   ├── [User Message Divs] ─────────────────── Gray bubble, right-aligned
│   ├── [AI Message Divs] ───────────────────── White, with logo header
│   └── loadingIndicator (Html) ─────────────── "AI is thinking..."
│
└── inputArea (Hlayout)
    ├── inputBox (Textbox) ──────────────────── User input
    └── sendButton (Button) ─────────────────── Send action
```

### Data Flow

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│   User Types     │     │   AIChatWidget   │     │  AIConversation  │
│   Message        │────▶│   sendMessage()  │────▶│     Service      │
└──────────────────┘     └──────────────────┘     └──────────────────┘
                                                           │
                         ┌─────────────────────────────────┘
                         │
                         ▼
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  MAIChatEntry    │     │   IAIProvider    │     │  Context         │
│  (Persistence)   │◀────│  generateText()  │◀────│  Extraction      │
└──────────────────┘     └──────────────────┘     └──────────────────┘
                                │
                                ▼
                         ┌──────────────────┐
                         │  External LLM    │
                         │  (Anthropic/     │
                         │   Bedrock/etc)   │
                         └──────────────────┘
```

### Thread Management

```
CM_Chat (MChat)
│
├── Thread 1 (Root: CM_ChatEntry_ID = 100)
│   ├── Entry 100: "What are today's orders?" (User, Parent=0)
│   ├── Entry 101: "Here are the orders..." (AI, Parent=100)
│   ├── Entry 102: "Show me details of SO-123" (User, Parent=100)
│   └── Entry 103: "SO-123 details: ..." (AI, Parent=100)
│
├── Thread 2 (Root: CM_ChatEntry_ID = 200)
│   ├── Entry 200: "Help with invoices" (User, Parent=0)
│   └── Entry 201: "Invoice help: ..." (AI, Parent=200)
│
└── Thread 3 (Root: CM_ChatEntry_ID = 300)
    └── ...

Thread Selection Logic:
- Root entries: CM_ChatEntryParent_ID = 0
- Child entries: CM_ChatEntryParent_ID = root entry ID
- Only ONE level deep (no grandchildren)
```

### Async Processing Pattern

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            ZK UI Thread                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User clicks Send                                                    │
│     │                                                                   │
│     ▼                                                                   │
│  2. Capture Desktop reference                                           │
│     Desktop desktop = Executions.getCurrent().getDesktop();             │
│     │                                                                   │
│     ▼                                                                   │
│  3. Show loading, disable input                                         │
│     │                                                                   │
│     └──────────────────┐                                                │
│                        │                                                │
└────────────────────────│────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       Background Thread                                 │
│                   (CompletableFuture.runAsync)                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  4. Call AI Service (blocking)                                          │
│     AIResponse response = aiService.sendMessage(...);                   │
│     │                                                                   │
│     ▼                                                                   │
│  5. Schedule UI update back to ZK thread                                │
│     Executions.schedule(desktop, event -> {                             │
│         hideLoading();                                                  │
│         renderMessage(response);                                        │
│     }, new Event("onAIResponse"));                                      │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            ZK UI Thread                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  6. Update UI (via scheduled event)                                     │
│     - Hide loading indicator                                            │
│     - Render AI message                                                 │
│     - Re-enable input                                                   │
│     - Scroll to bottom                                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Use Case Integration Points

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        iDempiere Web UI                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────┐                                                    │
│  │  Dashboard      │                                                    │
│  │  ┌───────────┐  │     ADR-017: Chart Executive Overview              │
│  │  │  Chart    │──┼────▶ ChartAIOverlay (Window)                       │
│  │  │  Widget   │  │     Opens on chart click                           │
│  │  └───────────┘  │                                                    │
│  │  ┌───────────┐  │                                                    │
│  │  │  AI Chat  │  │     Existing AIChatWidget                          │
│  │  │  Gadget   │──┼────▶ DashboardPanel wrapper                        │
│  │  └───────────┘  │                                                    │
│  └─────────────────┘                                                    │
│                                                                         │
│  ┌─────────────────┐                                                    │
│  │  C_Opportunity  │     ADR-018: Sales Opportunity Summary             │
│  │  Window         │                                                    │
│  │  ┌───────────┐  │                                                    │
│  │  │ [AI ◉]   │──┼────▶ OpportunitySummaryPanel (Panel)                │
│  │  │ Toolbar   │  │     Toolbar button or side panel                   │
│  │  └───────────┘  │                                                    │
│  └─────────────────┘                                                    │
│                                                                         │
│  ┌─────────────────┐                                                    │
│  │  Menu           │     ADR-019: Ticket Classification                 │
│  │  ┌───────────┐  │                                                    │
│  │  │ AI Ticket │──┼────▶ TicketClassificationForm (ADForm)             │
│  │  │ Classify  │  │     Batch processing form                          │
│  │  └───────────┘  │                                                    │
│  └─────────────────┘                                                    │
│                                                                         │
│  ┌─────────────────┐                                                    │
│  │  R_Request      │     ADR-020: Email Gateway Enhancement             │
│  │  Window         │                                                    │
│  │  ┌───────────┐  │                                                    │
│  │  │Description│──┼────▶ EnhancedEmailViewer (Panel)                   │
│  │  │  Field    │  │     Inline panel with toggle                       │
│  │  └───────────┘  │                                                    │
│  └─────────────────┘                                                    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Table of Contents

1. [Existing Infrastructure](#existing-infrastructure)
2. [ZK Component Patterns in iDempiere](#zk-component-patterns-in-idempiere)
3. [Use Case Implementations](#use-case-implementations)
   - [ADR-017: Chart Executive Overview](#adr-017-chart-executive-overview)
   - [ADR-018: Sales Opportunity Summary](#adr-018-sales-opportunity-summary)
   - [ADR-019: Support Ticket Classification](#adr-019-support-ticket-classification)
   - [ADR-020: Email Gateway Enhancement](#adr-020-email-gateway-enhancement)
4. [Shared Components](#shared-components)
5. [Registration and Integration](#registration-and-integration)
6. [Async Processing Pattern](#async-processing-pattern)

---

## Existing Infrastructure

### AIChatWidget (Already Implemented)

Location: `src/com/cloudempiere/ai/component/AIChatWidget.java`

The existing `AIChatWidget` provides:

| Feature | Implementation |
|---------|---------------|
| Chat UI | `Vlayout` messages container + `Textbox` input + `Button` send |
| Thread Management | `Combobox` thread selector, root message tracking |
| Message Persistence | `MAIChat` / `MAIChatEntry` models |
| Context Awareness | `IAIContextProvider` integration |
| Async AI Calls | `CompletableFuture.runAsync()` + `Executions.schedule()` |
| Markdown Rendering | Client-side marked.js + Prism.js |
| Zoom Links | `ZoomLinkProcessor` for clickable record references |

### Key Classes

```
com.cloudempiere.ai/
├── component/
│   └── AIChatWidget.java           # Main chat component (extends Div)
├── factory/
│   └── AIChatWidgetFactory.java    # Factory for creating widgets
├── service/
│   └── AIConversationService.java  # AI orchestration service
├── context/
│   ├── IAIContextProvider.java     # Context extraction interface
│   └── impl/
│       ├── WindowContextProvider.java
│       └── ChartContextProvider.java
├── model/
│   ├── MAIChat.java               # AI Chat model
│   └── MAIChatEntry.java          # Chat message model
└── util/
    └── ZoomLinkProcessor.java     # Record link processing
```

---

## ZK Component Patterns in iDempiere

### Pattern 1: Modal Dialog Window

**Use when**: Pop-up panels, overlays, contextual assistance

**Base Class**: `org.adempiere.webui.component.Window`

**Reference**: `WChat.java` (line 67 in iDempiere)

```java
public class AIExplanationDialog extends Window implements EventListener<Event> {

    public AIExplanationDialog(String title) {
        super();
        setTitle(title);
        setSclass("popup-dialog ai-dialog");
        setClosable(true);
        setBorder("normal");

        // Use CSS for sizing
        if (ThemeManager.isUseCSSForWindowSize()) {
            addCallback(AFTER_PAGE_ATTACHED, t -> {
                ZKUpdateUtil.setCSSHeight(this);
                ZKUpdateUtil.setCSSWidth(this);
            });
        } else {
            ZKUpdateUtil.setWindowWidthX(this, 500);
            ZKUpdateUtil.setHeight(this, "80%");
        }

        initUI();
    }

    private void initUI() {
        Borderlayout layout = new Borderlayout();
        appendChild(layout);

        // Content area (scrollable)
        Center center = new Center();
        center.setSclass("dialog-content");
        center.setAutoscroll(true);
        layout.appendChild(center);

        // Input area (fixed at bottom)
        South south = new South();
        ZKUpdateUtil.setVflex(south, "min");
        layout.appendChild(south);
    }
}
```

### Pattern 2: Dashboard Gadget

**Use when**: Homepage widgets, always-visible components

**Base Class**: `org.adempiere.webui.dashboard.DashboardPanel`

**Reference**: `DPDocumentStatusWidget.java`

```java
public class AIDashboardGadget extends DashboardPanel implements EventListener<Event> {

    private AIChatWidget chatWidget;

    public AIDashboardGadget() {
        super();
        setSclass("dsb-content ai-dashboard");
        initUI();
    }

    private void initUI() {
        chatWidget = new AIChatWidget(true); // context-enabled
        appendChild(chatWidget);
    }

    @Override
    public boolean isPooling() {
        return false; // No auto-refresh needed
    }

    @Override
    public boolean isLazy() {
        return true; // Load data after initial render
    }

    @Override
    public void refresh(ServerPushTemplate template) {
        // Refresh logic for lazy loading
        if (template != null && isLazy()) {
            template.executeAsync(this);
        }
    }

    @Override
    public void updateUI() {
        // Called after async refresh
    }
}
```

### Pattern 3: Custom Form (ADForm)

**Use when**: Standalone forms, batch processing, complex workflows

**Base Class**: `org.adempiere.webui.panel.ADForm`

```java
public class AIBatchProcessForm extends ADForm implements EventListener<Event> {

    @Override
    protected void initForm() {
        // Form initialization
        Borderlayout layout = new Borderlayout();
        appendChild(layout);

        // Selection area
        North north = new North();
        // ... selection components
        layout.appendChild(north);

        // Results grid
        Center center = new Center();
        Grid resultsGrid = new Grid();
        center.appendChild(resultsGrid);
        layout.appendChild(center);

        // Action buttons
        South south = new South();
        ConfirmPanel confirmPanel = new ConfirmPanel(true);
        confirmPanel.addActionListener(this);
        south.appendChild(confirmPanel);
        layout.appendChild(south);
    }

    @Override
    public void onEvent(Event event) throws Exception {
        // Handle events
    }
}
```

### Pattern 4: Inline Panel

**Use when**: Embedded in existing windows, side panels

**Base Class**: `org.adempiere.webui.component.Panel` or `org.zkoss.zul.Div`

```java
public class AISummaryPanel extends Panel implements EventListener<Event> {

    public AISummaryPanel() {
        super();
        setSclass("ai-summary-panel");
        ZKUpdateUtil.setHflex(this, "1");
        initUI();
    }

    private void initUI() {
        Vlayout content = new Vlayout();
        ZKUpdateUtil.setHflex(content, "1");
        appendChild(content);

        // Add summary sections
        // ...
    }
}
```

---

## Use Case Implementations

### ADR-017: Chart Executive Overview

**Approach**: Modal dialog triggered by chart click

#### Component: `ChartAIOverlay.java`

```java
package com.cloudempiere.ai.component;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.json.JSONObject;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.*;

import com.cloudempiere.ai.context.impl.ChartContextProvider;
import com.cloudempiere.ai.context.ContextParameters;
import com.cloudempiere.ai.provider.langchain4j.IDempiereAIService;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Chart AI Explanation Overlay
 * Opens when user clicks on a dashboard chart to get AI-powered insights
 */
public class ChartAIOverlay extends Window implements EventListener<Event> {

    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(ChartAIOverlay.class);

    private int chartId;
    private Properties ctx;
    private int windowNo;

    // UI Components
    private Html explanationArea;
    private Vlayout keyFindingsArea;
    private Vlayout suggestedQuestionsArea;
    private Textbox followUpInput;
    private Button askButton;
    private Html loadingIndicator;

    // AI Service
    private IDempiereAIService aiService;
    private String sessionId;

    /**
     * Constructor
     * @param chartId AD_Chart_ID
     * @param ctx context
     * @param windowNo window number
     */
    public ChartAIOverlay(int chartId, Properties ctx, int windowNo) {
        super();
        this.chartId = chartId;
        this.ctx = ctx;
        this.windowNo = windowNo;
        this.sessionId = "chart_" + chartId + "_" + System.currentTimeMillis();

        setTitle(Msg.getMsg(ctx, "ChartInsights")); // "Chart Insights"
        setSclass("popup-dialog ai-chart-dialog");
        setClosable(true);
        setBorder("normal");
        setMaximizable(true);
        setSizable(true);

        // Sizing
        if (ThemeManager.isUseCSSForWindowSize()) {
            addCallback(AFTER_PAGE_ATTACHED, t -> {
                ZKUpdateUtil.setCSSHeight(this);
                ZKUpdateUtil.setCSSWidth(this);
            });
        } else {
            ZKUpdateUtil.setWindowWidthX(this, 600);
            ZKUpdateUtil.setHeight(this, "70%");
        }

        initUI();
        loadExplanation();
    }

    private void initUI() {
        Borderlayout layout = new Borderlayout();
        layout.setStyle("border: none; background-color: white;");
        appendChild(layout);

        // Main content area (scrollable)
        Center center = new Center();
        center.setSclass("dialog-content");
        center.setAutoscroll(true);
        layout.appendChild(center);

        Vlayout content = new Vlayout();
        ZKUpdateUtil.setHflex(content, "1");
        ZKUpdateUtil.setVflex(content, "1");
        content.setStyle("padding: 16px; gap: 16px;");
        center.appendChild(content);

        // Loading indicator
        loadingIndicator = new Html();
        loadingIndicator.setContent(
            "<div style='text-align: center; padding: 40px;'>" +
            "<div style='font-size: 14px; color: #666;'>" +
            "<i>" + Msg.getMsg(ctx, "AnalyzingChart") + "</i>" +
            "</div></div>"
        );
        content.appendChild(loadingIndicator);

        // Explanation summary section
        Div summarySection = new Div();
        summarySection.setStyle("display: none;"); // Hidden until loaded
        summarySection.setId("summarySection");
        content.appendChild(summarySection);

        Label summaryLabel = new Label(Msg.getMsg(ctx, "Summary"));
        summaryLabel.setStyle("font-weight: 600; font-size: 14px; color: #181D27;");
        summarySection.appendChild(summaryLabel);

        explanationArea = new Html();
        explanationArea.setStyle("margin-top: 8px; font-size: 13px; line-height: 1.6; color: #535862;");
        summarySection.appendChild(explanationArea);

        // Key findings section
        Div findingsSection = new Div();
        findingsSection.setStyle("display: none; margin-top: 16px;");
        findingsSection.setId("findingsSection");
        content.appendChild(findingsSection);

        Label findingsLabel = new Label(Msg.getMsg(ctx, "KeyFindings"));
        findingsLabel.setStyle("font-weight: 600; font-size: 14px; color: #181D27;");
        findingsSection.appendChild(findingsLabel);

        keyFindingsArea = new Vlayout();
        keyFindingsArea.setStyle("margin-top: 8px; gap: 8px;");
        findingsSection.appendChild(keyFindingsArea);

        // Suggested questions section
        Div questionsSection = new Div();
        questionsSection.setStyle("display: none; margin-top: 16px;");
        questionsSection.setId("questionsSection");
        content.appendChild(questionsSection);

        Label questionsLabel = new Label(Msg.getMsg(ctx, "SuggestedQuestions"));
        questionsLabel.setStyle("font-weight: 600; font-size: 14px; color: #181D27;");
        questionsSection.appendChild(questionsLabel);

        suggestedQuestionsArea = new Vlayout();
        suggestedQuestionsArea.setStyle("margin-top: 8px; gap: 8px;");
        questionsSection.appendChild(suggestedQuestionsArea);

        // Follow-up input area (fixed at bottom)
        South south = new South();
        ZKUpdateUtil.setVflex(south, "min");
        south.setStyle("border-top: 1px solid #E0E0E0; padding: 12px;");
        layout.appendChild(south);

        Hlayout inputArea = new Hlayout();
        inputArea.setStyle("width: 100%; gap: 8px; align-items: center;");
        south.appendChild(inputArea);

        followUpInput = new Textbox();
        followUpInput.setPlaceholder(Msg.getMsg(ctx, "AskFollowUp"));
        ZKUpdateUtil.setHflex(followUpInput, "1");
        followUpInput.setStyle("border: 1px solid #E0E0E0; border-radius: 20px; padding: 10px 16px;");
        followUpInput.addEventListener(Events.ON_OK, this);
        inputArea.appendChild(followUpInput);

        askButton = new Button();
        askButton.setLabel(Msg.getMsg(ctx, "Ask"));
        askButton.addEventListener(Events.ON_CLICK, this);
        askButton.setStyle("background: #181D27; color: white; border-radius: 20px; padding: 10px 20px;");
        inputArea.appendChild(askButton);
    }

    /**
     * Load chart explanation from AI
     */
    private void loadExplanation() {
        Desktop desktop = Executions.getCurrent().getDesktop();

        CompletableFuture.runAsync(() -> {
            try {
                // Extract chart context
                ChartContextProvider provider = new ChartContextProvider();
                ContextParameters params = new ContextParameters();
                params.put("chartId", chartId);

                JSONObject chartContext = provider.extractContext(ctx, windowNo, params);

                // Build prompt for chart explanation
                String prompt = buildChartExplanationPrompt(chartContext);

                // Call AI service
                // TODO: Replace with actual LangChain4j agent call
                String aiResponse = callChartExplainerAgent(prompt);

                // Parse response
                JSONObject explanation = new JSONObject(aiResponse);

                // Update UI
                Executions.schedule(desktop, e -> {
                    displayExplanation(explanation);
                }, new Event("onExplanationLoaded"));

            } catch (Exception e) {
                log.severe("Failed to load chart explanation: " + e.getMessage());

                Executions.schedule(desktop, ev -> {
                    loadingIndicator.setContent(
                        "<div style='color: #d32f2f; padding: 16px;'>" +
                        Msg.getMsg(ctx, "Error") + ": " + e.getMessage() +
                        "</div>"
                    );
                }, new Event("onError"));
            }
        });
    }

    private String buildChartExplanationPrompt(JSONObject chartContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze this chart and provide insights:\n\n");
        sb.append("Chart Name: ").append(chartContext.optString("chart_name", "Unknown")).append("\n");
        sb.append("Chart Type: ").append(chartContext.optString("chart_type", "Unknown")).append("\n");
        sb.append("Period: ").append(chartContext.optString("period", "Unknown")).append("\n");
        sb.append("\nData:\n").append(chartContext.optString("data", "{}"));
        return sb.toString();
    }

    private String callChartExplainerAgent(String prompt) {
        // TODO: Implement actual LangChain4j agent call
        // This is a placeholder that returns mock data
        return "{"
            + "\"summary\": \"Revenue is trending upward with a 15% increase compared to last quarter.\","
            + "\"key_findings\": ["
            + "\"Total revenue increased by $125,000 this quarter\","
            + "\"Product A is the top performer with 40% of sales\","
            + "\"Customer acquisition cost decreased by 8%\""
            + "],"
            + "\"suggested_questions\": ["
            + "\"What drove the increase in Product A sales?\","
            + "\"How does this compare to the same period last year?\","
            + "\"Which regions contributed most to growth?\""
            + "],"
            + "\"methodology\": \"Data aggregated from C_Invoice over the current fiscal quarter.\""
            + "}";
    }

    private void displayExplanation(JSONObject explanation) {
        // Hide loading
        loadingIndicator.setContent("");

        // Show summary
        explanationArea.setContent(
            "<div>" + explanation.optString("summary", "") + "</div>" +
            "<div style='margin-top: 8px; font-size: 11px; color: #999;'>" +
            explanation.optString("methodology", "") + "</div>"
        );

        // Show sections via JavaScript
        Clients.evalJavaScript(
            "document.getElementById('summarySection').style.display='block';" +
            "document.getElementById('findingsSection').style.display='block';" +
            "document.getElementById('questionsSection').style.display='block';"
        );

        // Add key findings
        keyFindingsArea.getChildren().clear();
        if (explanation.has("key_findings")) {
            for (Object finding : explanation.getJSONArray("key_findings")) {
                Div findingDiv = new Div();
                findingDiv.setStyle(
                    "padding: 8px 12px; background: #F5F5F5; border-radius: 6px; " +
                    "font-size: 13px; color: #535862;"
                );
                Html findingHtml = new Html("<span style='color: #4CAF50; margin-right: 8px;'>&#10003;</span>" + finding);
                findingDiv.appendChild(findingHtml);
                keyFindingsArea.appendChild(findingDiv);
            }
        }

        // Add suggested questions as clickable buttons
        suggestedQuestionsArea.getChildren().clear();
        if (explanation.has("suggested_questions")) {
            for (Object question : explanation.getJSONArray("suggested_questions")) {
                Button questionBtn = new Button(question.toString());
                questionBtn.setStyle(
                    "text-align: left; background: #E3F2FD; color: #1976D2; " +
                    "border: none; border-radius: 6px; padding: 8px 12px; " +
                    "cursor: pointer; width: 100%;"
                );
                questionBtn.addEventListener(Events.ON_CLICK, e -> {
                    followUpInput.setValue(question.toString());
                    askFollowUp();
                });
                suggestedQuestionsArea.appendChild(questionBtn);
            }
        }
    }

    private void askFollowUp() {
        String question = followUpInput.getValue();
        if (question == null || question.trim().isEmpty()) return;

        // Clear input and disable
        followUpInput.setValue("");
        followUpInput.setDisabled(true);
        askButton.setDisabled(true);

        // TODO: Implement follow-up question handling via LangChain4j
        // This would use the same session to maintain conversation context

        // Re-enable after response
        followUpInput.setDisabled(false);
        askButton.setDisabled(false);
        followUpInput.focus();
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (event.getTarget() == askButton ||
            (event.getTarget() == followUpInput && Events.ON_OK.equals(event.getName()))) {
            askFollowUp();
        }
    }
}
```

#### Integration Point

Hook into chart component's onClick:

```java
// In chart rendering code (e.g., WGraph or dashboard chart component)
chart.addEventListener(Events.ON_CLICK, event -> {
    int chartId = getChartId();
    ChartAIOverlay overlay = new ChartAIOverlay(chartId, Env.getCtx(), getWindowNo());
    overlay.setPage(getPage());
    overlay.doHighlighted();
});
```

---

### ADR-018: Sales Opportunity Summary

**Approach**: Side panel or toolbar button on C_Opportunity window

#### Component: `OpportunitySummaryPanel.java`

```java
package com.cloudempiere.ai.component;

import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Opportunity Summary Panel
 * Displays AI-generated health assessment, stakeholders, and next actions
 */
public class OpportunitySummaryPanel extends Panel implements EventListener<Event> {

    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(OpportunitySummaryPanel.class);

    // Health score styles
    private static final String HEALTH_ON_TRACK = "background: #E8F5E9; color: #2E7D32; border-left: 4px solid #4CAF50;";
    private static final String HEALTH_NEEDS_ATTENTION = "background: #FFF3E0; color: #E65100; border-left: 4px solid #FF9800;";
    private static final String HEALTH_AT_RISK = "background: #FFEBEE; color: #C62828; border-left: 4px solid #F44336;";

    private int opportunityId;
    private Properties ctx;

    // UI Components
    private Div healthBadge;
    private Label reasoningLabel;
    private Grid stakeholderGrid;
    private Listbox actionsListbox;
    private Listbox risksListbox;
    private Vlayout timelineArea;
    private Html loadingIndicator;

    public OpportunitySummaryPanel(int opportunityId, Properties ctx) {
        super();
        this.opportunityId = opportunityId;
        this.ctx = ctx;

        setSclass("ai-opportunity-summary");
        ZKUpdateUtil.setHflex(this, "1");
        setStyle("padding: 16px; background: #FAFAFA; border-radius: 8px;");

        initUI();
        loadSummary();
    }

    private void initUI() {
        Vlayout content = new Vlayout();
        ZKUpdateUtil.setHflex(content, "1");
        content.setStyle("gap: 16px;");
        appendChild(content);

        // Title
        Label title = new Label(Msg.getMsg(ctx, "AISummary"));
        title.setStyle("font-weight: 600; font-size: 16px; color: #181D27;");
        content.appendChild(title);

        // Loading indicator
        loadingIndicator = new Html();
        loadingIndicator.setContent(
            "<div style='text-align: center; padding: 20px; color: #666;'>" +
            "<i>" + Msg.getMsg(ctx, "AnalyzingOpportunity") + "</i></div>"
        );
        content.appendChild(loadingIndicator);

        // Health score badge
        healthBadge = new Div();
        healthBadge.setStyle("display: none; padding: 12px; border-radius: 6px; margin-bottom: 8px;");
        content.appendChild(healthBadge);

        // Reasoning
        reasoningLabel = new Label();
        reasoningLabel.setStyle("display: none; font-size: 13px; color: #535862; margin-bottom: 16px;");
        content.appendChild(reasoningLabel);

        // Stakeholders section
        Groupbox stakeholderGroup = new Groupbox();
        stakeholderGroup.setMold("3d");
        stakeholderGroup.setOpen(true);
        stakeholderGroup.setStyle("display: none;");
        stakeholderGroup.setId("stakeholderGroup");

        Caption stakeholderCaption = new Caption(Msg.getMsg(ctx, "KeyStakeholders"));
        stakeholderGroup.appendChild(stakeholderCaption);

        stakeholderGrid = new Grid();
        stakeholderGrid.setStyle("border: none;");
        Columns cols = new Columns();
        cols.appendChild(new Column(Msg.getMsg(ctx, "Name")));
        cols.appendChild(new Column(Msg.getMsg(ctx, "Role")));
        cols.appendChild(new Column(Msg.getMsg(ctx, "Sentiment")));
        cols.appendChild(new Column(Msg.getMsg(ctx, "LastContact")));
        stakeholderGrid.appendChild(cols);
        stakeholderGrid.appendChild(new Rows());
        stakeholderGroup.appendChild(stakeholderGrid);
        content.appendChild(stakeholderGroup);

        // Critical actions section
        Groupbox actionsGroup = new Groupbox();
        actionsGroup.setMold("3d");
        actionsGroup.setOpen(true);
        actionsGroup.setStyle("display: none;");
        actionsGroup.setId("actionsGroup");

        Caption actionsCaption = new Caption(Msg.getMsg(ctx, "CriticalActions"));
        actionsGroup.appendChild(actionsCaption);

        actionsListbox = new Listbox();
        actionsListbox.setStyle("border: none;");
        actionsListbox.setCheckmark(true);
        actionsGroup.appendChild(actionsListbox);
        content.appendChild(actionsGroup);

        // Risk factors section
        Groupbox risksGroup = new Groupbox();
        risksGroup.setMold("3d");
        risksGroup.setOpen(true);
        risksGroup.setStyle("display: none;");
        risksGroup.setId("risksGroup");

        Caption risksCaption = new Caption(Msg.getMsg(ctx, "RiskFactors"));
        risksGroup.appendChild(risksCaption);

        risksListbox = new Listbox();
        risksListbox.setStyle("border: none;");
        risksGroup.appendChild(risksListbox);
        content.appendChild(risksGroup);

        // Timeline section
        Groupbox timelineGroup = new Groupbox();
        timelineGroup.setMold("3d");
        timelineGroup.setOpen(false); // Collapsed by default
        timelineGroup.setStyle("display: none;");
        timelineGroup.setId("timelineGroup");

        Caption timelineCaption = new Caption(Msg.getMsg(ctx, "Timeline"));
        timelineGroup.appendChild(timelineCaption);

        timelineArea = new Vlayout();
        timelineArea.setStyle("gap: 8px;");
        timelineGroup.appendChild(timelineArea);
        content.appendChild(timelineGroup);
    }

    private void loadSummary() {
        Desktop desktop = Executions.getCurrent().getDesktop();

        CompletableFuture.runAsync(() -> {
            try {
                // TODO: Extract opportunity context and call OpportunitySummaryAgent
                // For now, return mock data
                JSONObject summary = getMockSummary();

                Executions.schedule(desktop, e -> {
                    displaySummary(summary);
                }, new Event("onSummaryLoaded"));

            } catch (Exception e) {
                log.severe("Failed to load opportunity summary: " + e.getMessage());

                Executions.schedule(desktop, ev -> {
                    loadingIndicator.setContent(
                        "<div style='color: #d32f2f;'>" +
                        Msg.getMsg(ctx, "Error") + ": " + e.getMessage() +
                        "</div>"
                    );
                }, new Event("onError"));
            }
        });
    }

    private JSONObject getMockSummary() {
        // Mock data - replace with actual AI agent call
        return new JSONObject()
            .put("health_score", "NEEDS_ATTENTION")
            .put("health_emoji", "&#x1F7E1;") // Yellow circle
            .put("reasoning", "Activity gap of 10 days. Last contact was a demo, but no follow-up scheduled.")
            .put("key_stakeholders", new JSONArray()
                .put(new JSONObject()
                    .put("name", "John Smith")
                    .put("role", "Decision Maker")
                    .put("sentiment", "positive")
                    .put("last_interaction", "2025-11-25")
                    .put("notes", "Excited about demo, waiting for budget approval"))
                .put(new JSONObject()
                    .put("name", "Jane Doe")
                    .put("role", "Influencer")
                    .put("sentiment", "neutral")
                    .put("last_interaction", "2025-11-20")
                    .put("notes", "Technical questions remain")))
            .put("critical_actions", new JSONArray()
                .put("Schedule follow-up call with John Smith (Decision Maker)")
                .put("Send technical documentation to Jane Doe")
                .put("Prepare pricing proposal before Dec 10"))
            .put("risk_factors", new JSONArray()
                .put("No activity in 10 days - risk of losing momentum")
                .put("Competitor mentioned in last call - need to address"))
            .put("timeline", new JSONArray()
                .put(new JSONObject().put("date", "2025-11-15").put("event", "Initial contact"))
                .put(new JSONObject().put("date", "2025-11-20").put("event", "Discovery call"))
                .put(new JSONObject().put("date", "2025-11-25").put("event", "Product demo")))
            .put("next_milestone", "Budget approval expected by Dec 15")
            .put("win_probability_assessment", "Stated 60% seems optimistic. Recommend 45% given activity gap.");
    }

    private void displaySummary(JSONObject summary) {
        // Hide loading
        loadingIndicator.setContent("");

        // Health badge
        String healthScore = summary.optString("health_score", "UNKNOWN");
        String healthEmoji = summary.optString("health_emoji", "");
        String healthStyle = getHealthStyle(healthScore);

        healthBadge.setStyle("display: block; padding: 12px; border-radius: 6px; " + healthStyle);
        Html healthHtml = new Html(
            "<span style='font-size: 18px;'>" + healthEmoji + "</span> " +
            "<span style='font-weight: 600; font-size: 14px;'>" + formatHealthScore(healthScore) + "</span>"
        );
        healthBadge.getChildren().clear();
        healthBadge.appendChild(healthHtml);

        // Reasoning
        reasoningLabel.setValue(summary.optString("reasoning", ""));
        reasoningLabel.setStyle("display: block; font-size: 13px; color: #535862; margin-bottom: 16px;");

        // Show sections
        Clients.evalJavaScript(
            "document.getElementById('stakeholderGroup').style.display='block';" +
            "document.getElementById('actionsGroup').style.display='block';" +
            "document.getElementById('risksGroup').style.display='block';" +
            "document.getElementById('timelineGroup').style.display='block';"
        );

        // Stakeholders
        Rows rows = (Rows) stakeholderGrid.getRows();
        rows.getChildren().clear();
        if (summary.has("key_stakeholders")) {
            JSONArray stakeholders = summary.getJSONArray("key_stakeholders");
            for (int i = 0; i < stakeholders.length(); i++) {
                JSONObject s = stakeholders.getJSONObject(i);
                Row row = new Row();
                row.appendChild(new Label(s.optString("name")));
                row.appendChild(new Label(s.optString("role")));

                // Sentiment with color
                String sentiment = s.optString("sentiment", "neutral");
                Label sentimentLabel = new Label(sentiment);
                sentimentLabel.setStyle("color: " + getSentimentColor(sentiment) + ";");
                row.appendChild(sentimentLabel);

                row.appendChild(new Label(s.optString("last_interaction")));
                rows.appendChild(row);
            }
        }

        // Actions
        actionsListbox.getItems().clear();
        if (summary.has("critical_actions")) {
            JSONArray actions = summary.getJSONArray("critical_actions");
            for (int i = 0; i < actions.length(); i++) {
                Listitem item = new Listitem(actions.getString(i));
                actionsListbox.appendChild(item);
            }
        }

        // Risks
        risksListbox.getItems().clear();
        if (summary.has("risk_factors")) {
            JSONArray risks = summary.getJSONArray("risk_factors");
            for (int i = 0; i < risks.length(); i++) {
                Listitem item = new Listitem(risks.getString(i));
                item.setStyle("color: #C62828;");
                risksListbox.appendChild(item);
            }
        }

        // Timeline
        timelineArea.getChildren().clear();
        if (summary.has("timeline")) {
            JSONArray timeline = summary.getJSONArray("timeline");
            for (int i = 0; i < timeline.length(); i++) {
                JSONObject event = timeline.getJSONObject(i);
                Div eventDiv = new Div();
                eventDiv.setStyle(
                    "padding: 8px 12px; border-left: 2px solid #1976D2; " +
                    "margin-left: 8px; background: #F5F5F5; border-radius: 0 4px 4px 0;"
                );
                Html eventHtml = new Html(
                    "<span style='font-weight: 500; color: #1976D2;'>" + event.optString("date") + "</span> - " +
                    "<span style='color: #535862;'>" + event.optString("event") + "</span>"
                );
                eventDiv.appendChild(eventHtml);
                timelineArea.appendChild(eventDiv);
            }
        }
    }

    private String getHealthStyle(String healthScore) {
        switch (healthScore) {
            case "ON_TRACK": return HEALTH_ON_TRACK;
            case "NEEDS_ATTENTION": return HEALTH_NEEDS_ATTENTION;
            case "AT_RISK": return HEALTH_AT_RISK;
            default: return "background: #F5F5F5; color: #666;";
        }
    }

    private String formatHealthScore(String healthScore) {
        return healthScore.replace("_", " ");
    }

    private String getSentimentColor(String sentiment) {
        switch (sentiment.toLowerCase()) {
            case "positive": return "#4CAF50";
            case "negative": return "#F44336";
            default: return "#FF9800";
        }
    }

    @Override
    public void onEvent(Event event) throws Exception {
        // Handle events
    }
}
```

---

### ADR-019: Support Ticket Classification

**Approach**: Custom Form (ADForm) for batch processing + ModelValidator for auto-classification

#### Component: `TicketClassificationForm.java`

```java
package com.cloudempiere.ai.form;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Batch Ticket Classification Form
 * Allows processing multiple emails/tickets with AI classification
 */
public class TicketClassificationForm extends ADForm implements EventListener<Event> {

    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(TicketClassificationForm.class);

    private Listbox emailListbox;
    private Progressmeter progressBar;
    private Label statusLabel;
    private Button processButton;
    private Grid resultsGrid;

    @Override
    protected void initForm() {
        Borderlayout layout = new Borderlayout();
        ZKUpdateUtil.setHflex(layout, "1");
        ZKUpdateUtil.setVflex(layout, "1");
        appendChild(layout);

        // Header with instructions
        North north = new North();
        north.setStyle("padding: 16px; border-bottom: 1px solid #E0E0E0;");
        layout.appendChild(north);

        Vlayout header = new Vlayout();
        header.appendChild(new Label(Msg.getMsg(Env.getCtx(), "TicketClassificationDesc")));
        north.appendChild(header);

        // Main content - email list and results
        Center center = new Center();
        center.setStyle("padding: 16px;");
        center.setAutoscroll(true);
        layout.appendChild(center);

        Vlayout content = new Vlayout();
        ZKUpdateUtil.setHflex(content, "1");
        ZKUpdateUtil.setVflex(content, "1");
        content.setStyle("gap: 16px;");
        center.appendChild(content);

        // Email selection list
        Groupbox emailGroup = new Groupbox();
        emailGroup.setMold("3d");
        Caption emailCaption = new Caption(Msg.getMsg(Env.getCtx(), "UnclassifiedEmails"));
        emailGroup.appendChild(emailCaption);

        emailListbox = new Listbox();
        emailListbox.setCheckmark(true);
        emailListbox.setMultiple(true);
        ZKUpdateUtil.setHflex(emailListbox, "1");
        emailListbox.setRows(10);

        Listhead listhead = new Listhead();
        listhead.appendChild(new Listheader("")); // Checkbox
        listhead.appendChild(new Listheader(Msg.getMsg(Env.getCtx(), "Subject")));
        listhead.appendChild(new Listheader(Msg.getMsg(Env.getCtx(), "From")));
        listhead.appendChild(new Listheader(Msg.getMsg(Env.getCtx(), "Date")));
        emailListbox.appendChild(listhead);

        emailGroup.appendChild(emailListbox);
        content.appendChild(emailGroup);

        // Load unclassified emails
        loadUnclassifiedEmails();

        // Progress section
        Div progressSection = new Div();
        progressSection.setStyle("padding: 16px; background: #F5F5F5; border-radius: 8px;");

        progressBar = new Progressmeter();
        progressBar.setValue(0);
        ZKUpdateUtil.setWidth(progressBar, "100%");
        progressSection.appendChild(progressBar);

        statusLabel = new Label("");
        statusLabel.setStyle("margin-top: 8px; display: block; color: #666;");
        progressSection.appendChild(statusLabel);

        content.appendChild(progressSection);

        // Results grid
        Groupbox resultsGroup = new Groupbox();
        resultsGroup.setMold("3d");
        Caption resultsCaption = new Caption(Msg.getMsg(Env.getCtx(), "ClassificationResults"));
        resultsGroup.appendChild(resultsCaption);

        resultsGrid = new Grid();
        resultsGrid.setStyle("border: none;");

        Columns cols = new Columns();
        cols.appendChild(new Column(Msg.getMsg(Env.getCtx(), "Email")));
        cols.appendChild(new Column(Msg.getMsg(Env.getCtx(), "Type")));
        cols.appendChild(new Column(Msg.getMsg(Env.getCtx(), "Priority")));
        cols.appendChild(new Column(Msg.getMsg(Env.getCtx(), "Category")));
        cols.appendChild(new Column(Msg.getMsg(Env.getCtx(), "Confidence")));
        cols.appendChild(new Column(Msg.getMsg(Env.getCtx(), "Status")));
        resultsGrid.appendChild(cols);
        resultsGrid.appendChild(new Rows());

        resultsGroup.appendChild(resultsGrid);
        content.appendChild(resultsGroup);

        // Action buttons
        South south = new South();
        south.setStyle("padding: 16px; border-top: 1px solid #E0E0E0;");
        layout.appendChild(south);

        Hlayout buttons = new Hlayout();
        buttons.setStyle("gap: 8px;");

        processButton = new Button(Msg.getMsg(Env.getCtx(), "ClassifySelected"));
        processButton.addEventListener(Events.ON_CLICK, this);
        processButton.setStyle("background: #1976D2; color: white;");
        buttons.appendChild(processButton);

        Button selectAllBtn = new Button(Msg.getMsg(Env.getCtx(), "SelectAll"));
        selectAllBtn.addEventListener(Events.ON_CLICK, e -> selectAll(true));
        buttons.appendChild(selectAllBtn);

        Button deselectAllBtn = new Button(Msg.getMsg(Env.getCtx(), "DeselectAll"));
        deselectAllBtn.addEventListener(Events.ON_CLICK, e -> selectAll(false));
        buttons.appendChild(deselectAllBtn);

        south.appendChild(buttons);
    }

    private void loadUnclassifiedEmails() {
        // TODO: Load actual unclassified emails from database
        // For now, add mock data
        for (int i = 1; i <= 5; i++) {
            Listitem item = new Listitem();
            item.setValue(i); // Email ID
            item.appendChild(new Listcell(""));
            item.appendChild(new Listcell("RE: Order Problem #" + (1000 + i)));
            item.appendChild(new Listcell("customer" + i + "@example.com"));
            item.appendChild(new Listcell("2025-12-0" + i));
            emailListbox.appendChild(item);
        }
    }

    private void selectAll(boolean select) {
        for (Listitem item : emailListbox.getItems()) {
            item.setSelected(select);
        }
    }

    private void processClassification() {
        List<Integer> selectedIds = new ArrayList<>();
        for (Listitem item : emailListbox.getSelectedItems()) {
            selectedIds.add((Integer) item.getValue());
        }

        if (selectedIds.isEmpty()) {
            statusLabel.setValue(Msg.getMsg(Env.getCtx(), "NoEmailsSelected"));
            return;
        }

        // Disable UI during processing
        processButton.setDisabled(true);
        emailListbox.setDisabled(true);

        Desktop desktop = Executions.getCurrent().getDesktop();
        int total = selectedIds.size();

        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < selectedIds.size(); i++) {
                    final int idx = i;
                    final int emailId = selectedIds.get(i);

                    // Update progress
                    Executions.schedule(desktop, e -> {
                        int progress = (int) (((double) (idx + 1) / total) * 100);
                        progressBar.setValue(progress);
                        statusLabel.setValue(
                            Msg.getMsg(Env.getCtx(), "Processing") + " " + (idx + 1) + "/" + total
                        );
                    }, new Event("onProgress"));

                    // TODO: Call actual classification agent
                    // Simulate processing time
                    Thread.sleep(500);

                    // Add result to grid
                    Executions.schedule(desktop, e -> {
                        addResultRow(emailId, "SUPPORT", "HIGH", "SALES", 0.92, "Success");
                    }, new Event("onResult"));
                }

                // Done
                Executions.schedule(desktop, e -> {
                    statusLabel.setValue(Msg.getMsg(Env.getCtx(), "ProcessingComplete"));
                    processButton.setDisabled(false);
                    emailListbox.setDisabled(false);
                }, new Event("onComplete"));

            } catch (Exception e) {
                log.severe("Classification failed: " + e.getMessage());

                Executions.schedule(desktop, ev -> {
                    statusLabel.setValue(Msg.getMsg(Env.getCtx(), "Error") + ": " + e.getMessage());
                    processButton.setDisabled(false);
                    emailListbox.setDisabled(false);
                }, new Event("onError"));
            }
        });
    }

    private void addResultRow(int emailId, String type, String priority, String category,
                              double confidence, String status) {
        Rows rows = (Rows) resultsGrid.getRows();
        Row row = new Row();

        row.appendChild(new Label("Email #" + emailId));
        row.appendChild(new Label(type));

        Label priorityLabel = new Label(priority);
        priorityLabel.setStyle("color: " + getPriorityColor(priority) + "; font-weight: 600;");
        row.appendChild(priorityLabel);

        row.appendChild(new Label(category));

        // Confidence with color coding
        Label confLabel = new Label(String.format("%.0f%%", confidence * 100));
        confLabel.setStyle("color: " + getConfidenceColor(confidence) + ";");
        row.appendChild(confLabel);

        row.appendChild(new Label(status));

        rows.appendChild(row);
    }

    private String getPriorityColor(String priority) {
        switch (priority) {
            case "HIGH": return "#F44336";
            case "MEDIUM": return "#FF9800";
            default: return "#4CAF50";
        }
    }

    private String getConfidenceColor(double confidence) {
        if (confidence >= 0.85) return "#4CAF50";
        if (confidence >= 0.70) return "#FF9800";
        return "#F44336";
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (event.getTarget() == processButton) {
            processClassification();
        }
    }
}
```

---

### ADR-020: Email Gateway Enhancement

**Approach**: Custom panel for enhanced email viewing in R_Request window

#### Component: `EnhancedEmailViewer.java`

```java
package com.cloudempiere.ai.component;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.*;

import java.util.Properties;

/**
 * Enhanced Email Viewer
 * Displays cleaned email content with entity extraction
 */
public class EnhancedEmailViewer extends Panel implements EventListener<Event> {

    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(EnhancedEmailViewer.class);

    private Html contentArea;
    private Hlayout entitiesPanel;
    private Button toggleButton;
    private Div processingInfoDiv;

    private String enhancedContent;
    private String rawContent;
    private JSONObject extractedEntities;
    private boolean showingEnhanced = true;

    private Properties ctx;

    public EnhancedEmailViewer(Properties ctx) {
        super();
        this.ctx = ctx;

        setSclass("enhanced-email-viewer");
        ZKUpdateUtil.setHflex(this, "1");
        setStyle("padding: 16px; background: white; border-radius: 8px; border: 1px solid #E0E0E0;");

        initUI();
    }

    private void initUI() {
        Vlayout layout = new Vlayout();
        ZKUpdateUtil.setHflex(layout, "1");
        layout.setStyle("gap: 12px;");
        appendChild(layout);

        // Header with toggle button
        Hlayout header = new Hlayout();
        header.setStyle("justify-content: space-between; align-items: center;");
        layout.appendChild(header);

        Label titleLabel = new Label(Msg.getMsg(ctx, "EmailContent"));
        titleLabel.setStyle("font-weight: 600; font-size: 14px;");
        header.appendChild(titleLabel);

        toggleButton = new Button(Msg.getMsg(ctx, "ShowRaw"));
        toggleButton.addEventListener(Events.ON_CLICK, this);
        toggleButton.setStyle("font-size: 12px;");
        header.appendChild(toggleButton);

        // Extracted entities panel
        entitiesPanel = new Hlayout();
        entitiesPanel.setStyle("gap: 8px; flex-wrap: wrap; padding: 8px; background: #F5F5F5; border-radius: 6px;");
        layout.appendChild(entitiesPanel);

        // Content area
        contentArea = new Html();
        contentArea.setStyle("font-size: 13px; line-height: 1.6; color: #333;");
        layout.appendChild(contentArea);

        // Processing info (collapsible)
        processingInfoDiv = new Div();
        processingInfoDiv.setStyle("display: none; padding: 8px; background: #E3F2FD; border-radius: 4px; font-size: 11px; color: #1565C0;");
        layout.appendChild(processingInfoDiv);
    }

    /**
     * Set email content (both enhanced and raw versions)
     */
    public void setEmailContent(String enhanced, String raw, JSONObject entities, String processingSteps) {
        this.enhancedContent = enhanced;
        this.rawContent = raw;
        this.extractedEntities = entities;

        // Display enhanced by default
        showingEnhanced = true;
        displayContent();

        // Display entities
        displayEntities();

        // Display processing info
        if (processingSteps != null && !processingSteps.isEmpty()) {
            processingInfoDiv.setStyle("display: block; padding: 8px; background: #E3F2FD; border-radius: 4px; font-size: 11px; color: #1565C0;");
            Html infoHtml = new Html("<i class='z-icon-InfoCircle'></i> " + processingSteps);
            processingInfoDiv.getChildren().clear();
            processingInfoDiv.appendChild(infoHtml);
        }
    }

    private void displayContent() {
        String content = showingEnhanced ? enhancedContent : rawContent;
        if (content == null) content = "";

        // Escape HTML for raw content, keep enhanced as-is
        if (!showingEnhanced) {
            content = content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
        }

        contentArea.setContent("<div style='white-space: pre-wrap;'>" + content + "</div>");
        toggleButton.setLabel(showingEnhanced ?
            Msg.getMsg(ctx, "ShowRaw") : Msg.getMsg(ctx, "ShowEnhanced"));
    }

    private void displayEntities() {
        entitiesPanel.getChildren().clear();

        if (extractedEntities == null) return;

        // Order numbers
        if (extractedEntities.has("order_numbers")) {
            for (Object orderNo : extractedEntities.getJSONArray("order_numbers")) {
                addEntityBadge("&#x1F4E6;", orderNo.toString(), "Order", "#E3F2FD", "#1565C0");
            }
        }

        // Dates
        if (extractedEntities.has("dates")) {
            for (Object date : extractedEntities.getJSONArray("dates")) {
                addEntityBadge("&#x1F4C5;", date.toString(), "Date", "#E8F5E9", "#2E7D32");
            }
        }

        // Amounts
        if (extractedEntities.has("amounts")) {
            for (Object amount : extractedEntities.getJSONArray("amounts")) {
                addEntityBadge("&#x1F4B0;", amount.toString(), "Amount", "#FFF3E0", "#E65100");
            }
        }

        // SKUs
        if (extractedEntities.has("skus")) {
            for (Object sku : extractedEntities.getJSONArray("skus")) {
                addEntityBadge("&#x1F3F7;", sku.toString(), "SKU", "#F3E5F5", "#7B1FA2");
            }
        }
    }

    private void addEntityBadge(String icon, String value, String type, String bgColor, String textColor) {
        Div badge = new Div();
        badge.setStyle(
            "display: inline-flex; align-items: center; gap: 4px; " +
            "padding: 4px 8px; border-radius: 4px; " +
            "background: " + bgColor + "; color: " + textColor + "; " +
            "font-size: 12px; cursor: pointer;"
        );
        badge.setTooltiptext(type);

        Html badgeContent = new Html(icon + " " + value);
        badge.appendChild(badgeContent);

        // Click to copy
        badge.addEventListener(Events.ON_CLICK, e -> {
            org.zkoss.zk.ui.util.Clients.evalJavaScript(
                "navigator.clipboard.writeText('" + value.replace("'", "\\'") + "');" +
                "alert('Copied: " + value.replace("'", "\\'") + "');"
            );
        });

        entitiesPanel.appendChild(badge);
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (event.getTarget() == toggleButton) {
            showingEnhanced = !showingEnhanced;
            displayContent();
        }
    }
}
```

---

## Shared Components

### Base AI Panel Class

Create a reusable base class for AI-powered panels:

```java
package com.cloudempiere.ai.component;

import org.adempiere.webui.component.Panel;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.CLogger;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Html;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for AI-powered panels
 * Provides common async processing and UI update patterns
 */
public abstract class BaseAIPanel extends Panel {

    private static final long serialVersionUID = 1L;
    protected static final CLogger log = CLogger.getCLogger(BaseAIPanel.class);

    protected Properties ctx;
    protected Html loadingIndicator;

    public BaseAIPanel(Properties ctx) {
        super();
        this.ctx = ctx;
        ZKUpdateUtil.setHflex(this, "1");
    }

    /**
     * Execute AI operation asynchronously
     */
    protected void executeAsync(AIOperation operation, AICallback callback) {
        Desktop desktop = Executions.getCurrent().getDesktop();

        showLoading();

        CompletableFuture.runAsync(() -> {
            try {
                Object result = operation.execute();

                Executions.schedule(desktop, e -> {
                    hideLoading();
                    callback.onSuccess(result);
                }, new Event("onAISuccess"));

            } catch (Exception e) {
                log.severe("AI operation failed: " + e.getMessage());

                Executions.schedule(desktop, ev -> {
                    hideLoading();
                    callback.onError(e);
                }, new Event("onAIError"));
            }
        });
    }

    protected void showLoading() {
        if (loadingIndicator != null) {
            loadingIndicator.setContent(
                "<div style='text-align: center; padding: 20px; color: #666;'>" +
                "<i>Analyzing...</i></div>"
            );
        }
    }

    protected void hideLoading() {
        if (loadingIndicator != null) {
            loadingIndicator.setContent("");
        }
    }

    @FunctionalInterface
    public interface AIOperation {
        Object execute() throws Exception;
    }

    public interface AICallback {
        void onSuccess(Object result);
        void onError(Exception e);
    }
}
```

---

## Registration and Integration

### Dashboard Gadget Registration

Create ZUL file: `zul/dashboard/aiChat.zul`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<window use="com.cloudempiere.ai.dashboard.AIDashboardGadget"/>
```

Register in `PA_DashboardContent`:
- **Name**: AI Assistant
- **ZUL File Path**: `/zul/dashboard/aiChat.zul`
- **IsCollapsible**: Y
- **GoalDisplay**: N

### Custom Form Registration

Register in `AD_Form`:
- **Name**: AI Ticket Classification
- **Classname**: `com.cloudempiere.ai.form.TicketClassificationForm`
- **Access Level**: Client+Organization

### Toolbar Button Registration

Register in `AD_ToolBarButton`:
- **Name**: AI Summary
- **Action**: Window
- **AD_Window_ID**: C_Opportunity
- **ComponentName**: CustomButton
- **Classname**: `com.cloudempiere.ai.component.OpportunitySummaryButton`

---

## Async Processing Pattern

All AI calls should use this pattern to avoid blocking the UI:

```java
// 1. Capture desktop reference BEFORE async call
Desktop desktop = Executions.getCurrent().getDesktop();

// 2. Show loading state
showLoading();

// 3. Execute async
CompletableFuture.runAsync(() -> {
    try {
        // Long-running AI operation
        Object result = callAIAgent();

        // 4. Schedule UI update back on ZK thread
        Executions.schedule(desktop, e -> {
            hideLoading();
            updateUI(result);
        }, new Event("onComplete"));

    } catch (Exception e) {
        // 5. Handle errors on ZK thread
        Executions.schedule(desktop, ev -> {
            hideLoading();
            showError(e.getMessage());
        }, new Event("onError"));
    }
});
```

**Important**: Always capture `Executions.getCurrent().getDesktop()` before the async operation, as `Executions.getCurrent()` returns null inside the async block.

---

## Next Steps

1. **Implement LangChain4j Integration** (per ADR-031)
   - Replace mock AI calls with actual `IDempiereAIService` calls
   - Integrate with `ChartExplainerAgent`, `OpportunitySummaryAgent`, etc.

2. **Add Streaming Support**
   - Use `StreamingChatLanguageModel` for real-time responses
   - Update UI progressively as tokens arrive

3. **Add Caching Layer**
   - Cache AI responses based on context hash
   - Implement TTL-based invalidation

4. **Create Unit Tests**
   - Test component rendering
   - Test async operations
   - Test error handling

---

*Last updated: 2025-12-03*
