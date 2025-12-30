# ADR-053: Floating Chat Bubble Architecture (Zero Core Changes)

## Status

📋 **Proposed** (2025-12-30)

**Future Implementation** - Documented for migration from temporary HelpController integration

## Date

- **Created:** 2025-12-30
- **Target Implementation:** Q1 2026 (after iDempiere v11 migration)

## Deciders

- Cloudempiere Development Team

## Context and Problem Statement

ADR-052 attempted to achieve zero AI-specific code in iDempiereCLDE core by using the IDashboardGadgetFactory pattern. However, this still requires HelpController to call `Extensions.getDashboardGadget("ai-chat", ...)` - a single line of AI-aware code in core.

**Current temporary solution:** HelpController contains a temporary call marked for removal.

**Goal:** Achieve **100% zero core changes** while providing modern AI chat UX.

## Decision Drivers

- **Zero core changes**: Absolutely no modifications to iDempiereCLDE
- **Modern UX**: Industry-standard chat widget pattern (Intercom, Drift, Zendesk style)
- **Simple implementation**: Minimize complexity and code
- **Better accessibility**: Chat available from any page, not just help panel
- **Maintainability**: Easier to upgrade iDempiere without merge conflicts

## Decision Outcome

**Chosen solution:** "Floating Chat Bubble with ZK Fragment Injection"

A floating chat bubble appears in the bottom-right corner of the screen, expanding into a chat window when clicked. This is injected via ZK's UiLifeCycle extension mechanism using the theme fragment bundle.

### Implementation Summary

1. **ZK Fragment Configuration** (`com.cloudempiere.ai.theme/WEB-INF/zk.xml`)
   - Registers UiLifeCycle listener
   - ZK automatically merges fragment configurations

2. **Auto-Injector Listener** (`AIChatBubbleInjector.java`)
   - Detects desktop/page creation
   - Injects floating bubble component
   - No dependency on core UI structure

3. **Floating Bubble Component** (`AIChatBubble.java`)
   - CSS fixed positioning (bottom-right corner)
   - Expands to chat window on click
   - Modern, familiar UX pattern

### Confirmation Checklist

- [ ] AIChatBubbleInjector.java implemented
- [ ] AIChatBubble.java component created
- [ ] CSS styling in custom.css.dsp
- [ ] WEB-INF/zk.xml in theme fragment
- [ ] Temporary HelpController code removed
- [ ] Manual testing completed
- [ ] Mobile responsive design verified

## Comparison: Help Panel vs Floating Bubble

### Complexity Analysis

| Aspect | Help Panel Widget | Floating Bubble |
|--------|-------------------|-----------------|
| **Core changes** | 1 line (temporary) | **0 lines** |
| **Component detection** | Complex tree navigation | Simple desktop attachment |
| **Timing sensitivity** | Must wait for HelpController render | Any time after desktop creation |
| **Parent discovery** | Find specific Vlayout in Anchorlayout | Just attach to page root |
| **Layout integration** | Must fit into existing dashboard | Independent fixed positioning |
| **Code complexity** | ~200 lines | **~100 lines** |
| **Maintenance burden** | Coupled to HelpController structure | **Zero coupling** |
| **UX accessibility** | Only in help panel | **Available everywhere** |
| **Industry pattern** | Traditional panel | **Modern chat widget** |

**Result:** Floating bubble is **10x simpler** and achieves true zero-core-changes goal.

### Code Size Comparison

```
Help Panel Solution:
- AIChatGadgetFactory.java: 197 lines
- HelpController.java: 3-5 lines (TEMPORARY)
- Component tree navigation: 150+ lines
- Total: ~350 lines

Floating Bubble Solution:
- AIChatBubbleInjector.java: 80 lines
- AIChatBubble.java: 100 lines
- CSS styling: 50 lines
- Total: ~230 lines (35% less code)
```

## Architecture

### Visual Design

```
┌─────────────────────────────────────────────────────────┐
│ iDempiere Desktop (Any Page)                            │
│                                                          │
│  ┌────────────────────────────────────────────┐         │
│  │ Active Window Content                      │         │
│  │                                             │         │
│  │  [Form Fields]                              │         │
│  │  [Grid/Table]                               │         │
│  │  [Buttons]                                  │         │
│  │                                             │         │
│  └────────────────────────────────────────────┘         │
│                                                          │
│                                          ┌──────────┐    │
│                                          │    💬    │    │ ← Bubble
│                                          └──────────┘    │
└─────────────────────────────────────────────────────────┘

When clicked → Expands to chat window:

┌─────────────────────────────────────────────────────────┐
│ iDempiere Desktop                                        │
│                              ┌────────────────────────┐  │
│  ┌────────────────────┐      │ AI Assistant         × │  │
│  │ Active Window      │      ├────────────────────────┤  │
│  │                    │      │ 🤖 How can I help?    │  │
│  │  [Form Fields]     │      │                        │  │
│  │                    │      │ User: Show sales       │  │
│  │  [Grid/Table]      │      │ AI: Here are sales... │  │
│  │                    │      │                        │  │
│  │  [Buttons]         │      │ [Input: Type here...] │  │
│  │                    │      └────────────────────────┘  │
│  └────────────────────┘      ┌──────────┐               │
│                              │    💬    │ (minimized)   │
│                              └──────────┘               │
└─────────────────────────────────────────────────────────┘
```

### Component Architecture

```
┌──────────────────────────────────────────────────────────┐
│ iDempiereCLDE Core                                       │
│  - ZERO modifications                                    │
│  - ZERO AI-specific code                                 │
│  - ZERO knowledge of plugin                              │
└──────────────────────────────────────────────────────────┘
                    ▲
                    │ ZK Framework Event
                    │ (afterPageAttached)
                    ▼
┌──────────────────────────────────────────────────────────┐
│ ZK Framework (Fragment Configuration Merge)              │
│  - Loads WEB-INF/zk.xml from host + fragments            │
│  - Registers UiLifeCycle listeners                       │
│  - Fires lifecycle events                                │
└──────────────────────────────────────────────────────────┘
                    │
                    │ Event: Page attached to Desktop
                    ▼
┌──────────────────────────────────────────────────────────┐
│ AIChatBubbleInjector (Plugin)                            │
│  - Registered via theme/WEB-INF/zk.xml                   │
│  - Detects first page attachment                         │
│  - Injects AIChatBubble component                        │
│  - Marks desktop as processed                            │
└──────────────────────────────────────────────────────────┘
                    │
                    │ Creates and attaches
                    ▼
┌──────────────────────────────────────────────────────────┐
│ AIChatBubble (Floating Component)                        │
│  - Fixed CSS positioning (bottom-right)                  │
│  - Contains AIChatWidget                                 │
│  - Toggle expand/collapse on click                       │
│  - Self-managing lifecycle                               │
└──────────────────────────────────────────────────────────┘
```

## Technical Implementation

### 1. ZK Fragment Configuration

**File:** `com.cloudempiere.ai.theme/WEB-INF/zk.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<zk>
    <!-- AI Chat Bubble Auto-Injector via ZK UiLifeCycle -->
    <listener>
        <description>AI Chat Bubble - Floating Widget (Zero Core Changes)</description>
        <listener-class>com.cloudempiere.ai.zk.AIChatBubbleInjector</listener-class>
    </listener>
</zk>
```

**How it works:**
- Fragment bundle attaches to `org.adempiere.ui.zk` via `Fragment-Host`
- Jetty scans fragments for `/WEB-INF/` resources
- ZK merges `zk.xml` from host + all fragments automatically
- Listener registered without any core code changes

### 2. Desktop Injection Listener

**File:** `com.cloudempiere.ai/src/com/cloudempiere/ai/zk/AIChatBubbleInjector.java`

```java
package com.cloudempiere.ai.zk;

import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.util.UiLifeCycle;
import com.cloudempiere.ai.component.AIChatBubble;

public class AIChatBubbleInjector implements UiLifeCycle {

    private static final String ATTR_BUBBLE_INJECTED = "_aiChatBubbleInjected";

    @Override
    public void afterPageAttached(Page page, Desktop desktop) {
        // Only inject once per desktop session
        if (desktop.getAttribute(ATTR_BUBBLE_INJECTED) != null) {
            return;
        }

        desktop.setAttribute(ATTR_BUBBLE_INJECTED, Boolean.TRUE);

        if (isAvailable()) {
            AIChatBubble bubble = new AIChatBubble();
            if (!page.getRoots().isEmpty()) {
                page.getRoots().get(0).appendChild(bubble);
            }
        }
    }

    // Health check logic...
}
```

**Key advantages:**
- ✅ Simple: Just detect page attachment
- ✅ Robust: No dependency on specific components
- ✅ Efficient: Injects once per desktop session
- ✅ Safe: Health check before injection

### 3. Floating Bubble Component

**File:** `com.cloudempiere.ai/src/com/cloudempiere/ai/component/AIChatBubble.java`

```java
package com.cloudempiere.ai.component;

import org.zkoss.zul.Div;
import org.zkoss.zul.Window;

public class AIChatBubble extends Div {

    private Window chatWindow;
    private AIChatWidget chatWidget;
    private boolean expanded = false;

    public AIChatBubble() {
        super();

        // Style as floating bubble
        setSclass("ai-chat-bubble");
        setTooltiptext("AI Assistant");

        // Create icon
        Div icon = new Div();
        icon.setIconSclass("z-icon-comments");
        appendChild(icon);

        // Toggle on click
        addEventListener(Events.ON_CLICK, e -> toggleChat());

        // Create expandable chat window
        createChatWindow();
    }

    private void createChatWindow() {
        chatWindow = new Window("AI Assistant");
        chatWindow.setSclass("ai-chat-window");
        chatWindow.setStyle("position: fixed; bottom: 80px; right: 20px; " +
                           "width: 400px; height: 600px;");
        chatWindow.setVisible(false);

        chatWidget = new AIChatWidget(true);
        chatWindow.appendChild(chatWidget);

        getParent().appendChild(chatWindow);
    }

    private void toggleChat() {
        expanded = !expanded;
        chatWindow.setVisible(expanded);
    }
}
```

**Key features:**
- ✅ CSS fixed positioning (no layout integration)
- ✅ Self-contained expand/collapse logic
- ✅ Reuses existing AIChatWidget component
- ✅ Clean lifecycle management

### 4. CSS Styling

**File:** `com.cloudempiere.ai.theme/theme/default/css/fragment/custom.css.dsp`

```css
/* Floating bubble button */
.ai-chat-bubble {
    position: fixed;
    bottom: 20px;
    right: 20px;
    width: 60px;
    height: 60px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 50%;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    cursor: pointer;
    z-index: 9999;
    transition: transform 0.3s ease;
}

.ai-chat-bubble:hover {
    transform: scale(1.1);
}

/* Expandable chat window */
.ai-chat-window {
    position: fixed !important;
    bottom: 80px !important;
    right: 20px !important;
    width: 400px !important;
    height: 600px !important;
    z-index: 9998 !important;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2) !important;
    border-radius: 12px !important;
}

/* Mobile responsive */
@media (max-width: 768px) {
    .ai-chat-window {
        width: calc(100vw - 40px) !important;
        height: calc(100vh - 100px) !important;
    }
}
```

## Benefits

### 1. Zero Core Changes ✅

**Absolutely no modifications to iDempiereCLDE:**
- No code in HelpController
- No code in DefaultDesktop
- No interfaces to maintain
- No merge conflicts on upgrades

### 2. Simpler Implementation ✅

**10x less complexity:**
- No component tree navigation
- No timing dependencies
- No parent discovery logic
- 35% less code overall

### 3. Better User Experience ✅

**Modern chat widget pattern:**
- Always accessible from any page
- Doesn't consume screen space when minimized
- Familiar UX (Intercom, Drift, Zendesk)
- Mobile responsive

### 4. More Maintainable ✅

**Zero coupling to core:**
- Independent of HelpController changes
- Independent of desktop layout changes
- Survives iDempiere upgrades without modification
- Clear separation of concerns

### 5. Future-Proof ✅

**Standard extension pattern:**
- Uses official ZK extension mechanism
- Can be contributed upstream to iDempiere
- Works with any iDempiere version
- No deprecated APIs

## Migration Path

### Phase 1: Document Solution (✅ Done)
- Create ADR-053
- Document architecture and implementation
- Define acceptance criteria

### Phase 2: Implement Floating Bubble (Target: Q1 2026)
1. Create `AIChatBubbleInjector.java`
2. Create `AIChatBubble.java`
3. Add CSS styling
4. Create `WEB-INF/zk.xml` in theme fragment
5. Update MANIFEST.MF exports

### Phase 3: Remove Temporary Code (After Phase 2)
1. Remove HelpController temporary code
2. Remove AIChatGadgetFactory.java (no longer needed)
3. Update ADR-052 status to "Superseded by ADR-053"

### Phase 4: Testing and Refinement
1. Test on multiple browsers
2. Test mobile responsive design
3. Test desktop session persistence
4. Verify no core dependencies

## Risks and Mitigation

### Risk 1: ZK Fragment Configuration Not Working
**Mitigation:** Test fragment WEB-INF/zk.xml merging early. If ZK doesn't merge fragment zk.xml, fall back to programmatic listener registration via OSGi.

### Risk 2: Desktop Root Component Not Available
**Mitigation:** Defensive code checks for root components before injection. Can also attach to Desktop directly if needed.

### Risk 3: CSS Conflicts with Themes
**Mitigation:** Use high-specificity selectors and !important flags. Test with multiple iDempiere themes.

### Risk 4: Mobile UX Issues
**Mitigation:** Implement responsive CSS with media queries. Test on multiple viewport sizes.

## Alternatives Considered

### Alternative 1: Help Panel Integration (Current Temporary)
- **Pros:** Already partially implemented
- **Cons:** Requires core code, complex detection, limited accessibility
- **Decision:** Rejected - doesn't achieve zero-core-changes goal

### Alternative 2: Event-Based Injection
- **Pros:** No core code needed
- **Cons:** Requires adding new event topic to core IEventTopics
- **Decision:** Rejected - still requires core modification

### Alternative 3: Service Tracker with Reflection
- **Pros:** Pure OSGi solution
- **Cons:** Complex, fragile, non-standard
- **Decision:** Rejected - too complex and non-idiomatic

### Alternative 4: JavaScript-Based Injection
- **Pros:** No Java code needed
- **Cons:** Hard to integrate with ZK components, brittle
- **Decision:** Rejected - doesn't fit ZK architecture

## Related ADRs

- [ADR-052](052-ai-chat-widget-core-decoupling.md) - AI Chat Widget Core Decoupling (Superseded)
- [ADR-031](031-chat-panel-langchain4j-chatmodel-integration.md) - Chat Panel LangChain4j Integration
- [ADR-033](033-streaming-thinking-timeline-ux.md) - Streaming Responses UX

## References

- [ZK UiLifeCycle Documentation](https://www.zkoss.org/wiki/ZK_Developer%27s_Reference/UI_Composing/Component_Lifecycle)
- [Modern Chat Widget UX Patterns](https://www.intercom.com/messenger)
- [iDempiere Fragment Bundle Development](https://wiki.idempiere.org/en/Developing_Plug-Ins)
- [ZK Fragment Configuration](https://www.zkoss.org/wiki/ZK_Configuration_Reference/zk.xml)

---

**ADR-053** | Created: 2025-12-30 | Status: Proposed | Target: Q1 2026
