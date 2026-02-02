# ADR-052: AI Chat Widget Core Decoupling

## Status

⚠️ **Partially Implemented with Temporary Solution** (2025-12-30)

**Current State:**
- ✅ Plugin architecture completed (AIChatGadgetFactory, self-managing context tracking)
- ⚠️ **Temporary core code** in HelpController (marked for removal)
- 📋 **Superseded by ADR-053** for true zero-core-changes solution

**Migration Path:**
- Current: Help panel integration with temporary HelpController code
- Target: [ADR-053 Floating Chat Bubble](053-floating-chat-bubble-zero-core-changes.md) (Zero core changes)
- Timeline: Q1 2026

## Date

- **Created:** 2025-12-26
- **Implemented:** 2025-12-30 (Partial - with temporary code)
- **Target Completion:** Q1 2026 (ADR-053 floating bubble)

## Deciders

- Cloudempiere Development Team

## Context and Problem Statement

The current AI Chat Widget integration requires a custom interface (`IAIChatWidgetFactory`) in the iDempiere core (`org.adempiere.ui.zk`). This creates unnecessary coupling between the core ERP and the AI plugin. The goal is to move as much AI-related code as possible from the core fork (iDempiereCLDE) to the plugin (com.cloudempiere.ai) to:

1. Minimize core fork divergence from upstream iDempiere
2. Simplify plugin deployment and updates
3. Follow iDempiere's established extension patterns

## Decision Drivers

- **Minimize core changes**: Reduce lines of code in iDempiereCLDE fork
- **Standard patterns**: Use existing iDempiere extension mechanisms
- **Plugin autonomy**: Allow AI plugin to be deployed/updated independently
- **Maintainability**: Easier upgrades when iDempiere releases new versions
- **Upstream contribution**: Potential to contribute changes back to iDempiere

## Considered Options

1. **Keep current IAIChatWidgetFactory interface in core**
2. **Use IDashboardGadgetFactory pattern** (existing iDempiere extension)
3. **Use service tracker with string-based lookup** (reflection)
4. **Use event-based widget provisioning**

## Decision Outcome

**Chosen option:** "Option 2 - Use IDashboardGadgetFactory pattern", because it uses an existing, well-tested iDempiere extension mechanism, requires zero new interfaces in core, and follows the principle of least surprise for iDempiere developers.

### Confirmation

- [x] `IAIChatWidgetFactory.java` removed from org.adempiere.ui.zk
- [x] `AIChatGadgetFactory` implements `IDashboardGadgetFactory` in plugin
- [x] ⚠️ `HelpController` uses `Extensions.getDashboardGadget("ai-chat", ...)` **[TEMPORARY CODE]**
- [x] Self-managing context tracking implemented (ZK lifecycle hooks + event subscription)
- [x] All AI-specific code removed from `HelpController.java` and `DefaultDesktop.java`
- [x] Code review validation completed (approved with recommendations)
- [ ] AI Chat Widget displays correctly in Help panel (requires manual testing)
- [ ] Plugin can be undeployed without breaking core UI (requires manual testing)

## Pros and Cons of the Options

### Option 1: Keep current IAIChatWidgetFactory interface in core

Current implementation with custom interface in org.adempiere.ui.zk.

- Good, because already implemented and working
- Good, because type-safe interface
- Bad, because adds custom code to core fork (65 lines)
- Bad, because not a standard iDempiere pattern
- Bad, because increases fork divergence from upstream

### Option 2: Use IDashboardGadgetFactory pattern

Register AI Chat Widget as a dashboard gadget with URI "ai-chat".

```java
// Plugin: AIChatGadgetFactory.java
@Component(service = IDashboardGadgetFactory.class)
public class AIChatGadgetFactory implements IDashboardGadgetFactory {

    private static final String AI_CHAT_URI = "ai-chat";

    @Override
    public Component getGadget(String uri, Component parent, Map<?, ?> arg) {
        if (AI_CHAT_URI.equals(uri)) {
            return new AIChatWidget();
        }
        return null;
    }
}
```

```java
// Core: HelpController.java (modified)
Component aiChat = Extensions.getDashboardGadget("ai-chat", parent, null);
if (aiChat != null) {
    pnlAIChat.appendChild(aiChat);
}
```

- Good, because uses existing iDempiere extension pattern
- Good, because zero new interfaces in core
- Good, because plugin is fully self-contained
- Good, because standard Service.locator() discovery
- Good, because can be contributed upstream
- Neutral, because URI-based lookup (string, not type-safe)
- Bad, because slightly more lookup overhead

### Option 3: Use service tracker with string-based lookup

Use OSGi ServiceTracker with class name filter and reflection.

```java
ServiceTracker<Object, Object> tracker = new ServiceTracker<>(
    bundleContext,
    "(objectClass=com.cloudempiere.ai.factory.IAIChatWidgetFactory)",
    null
);
Object factory = tracker.getService();
Method createWidget = factory.getClass().getMethod("createChatWidget");
Div widget = (Div) createWidget.invoke(factory);
```

- Good, because no interface needed in core
- Bad, because uses reflection (fragile, slower)
- Bad, because not type-safe
- Bad, because harder to debug
- Bad, because non-standard pattern

### Option 4: Use event-based widget provisioning

Fire event, plugin responds with widget component.

```java
// Core fires event
Event event = eventManager.sendEvent("AI_CHAT_WIDGET_REQUEST", data);
Component widget = (Component) event.getProperty("widget");

// Plugin listens
eventManager.register("AI_CHAT_WIDGET_REQUEST", this::provideWidget);
```

- Good, because fully decoupled
- Good, because no interface needed
- Bad, because adds complexity
- Bad, because event-based UI creation is unusual
- Bad, because harder to reason about lifecycle

## More Information

### Current Architecture (Before)

```
┌─────────────────────────────────────────────────────────┐
│  iDempiereCLDE (Core Fork)                              │
│                                                          │
│  org.adempiere.ui.zk:                                    │
│  ├── IAIChatWidgetFactory.java  ◄── Custom interface    │
│  └── HelpController.java        ◄── Uses custom lookup  │
│                                                          │
└─────────────────────────────────────────────────────────┘
                         │
                         │ OSGi Service (custom interface)
                         ▼
┌─────────────────────────────────────────────────────────┐
│  com.cloudempiere.ai (Plugin)                           │
│                                                          │
│  ├── AIChatWidgetFactory implements IAIChatWidgetFactory│
│  └── AIChatWidget                                        │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Target Architecture (After)

```
┌─────────────────────────────────────────────────────────┐
│  iDempiereCLDE (Core Fork)                              │
│                                                          │
│  org.adempiere.ui.zk:                                    │
│  └── HelpController.java ◄── Uses Extensions.getDashboardGadget()
│                               (standard iDempiere API)   │
│                                                          │
│  No custom interfaces!                                   │
│                                                          │
└─────────────────────────────────────────────────────────┘
                         │
                         │ OSGi Service (IDashboardGadgetFactory)
                         ▼
┌─────────────────────────────────────────────────────────┐
│  com.cloudempiere.ai (Plugin)                           │
│                                                          │
│  ├── AIChatGadgetFactory implements IDashboardGadgetFactory
│  │   └── getGadget("ai-chat", ...) → AIChatWidget       │
│  └── AIChatWidget                                        │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Implementation Steps

1. **Plugin changes:**
   - Create `AIChatGadgetFactory` implementing `IDashboardGadgetFactory`
   - Register with URI "ai-chat"
   - Keep `AIChatWidget` unchanged

2. **Core changes:**
   - Delete `IAIChatWidgetFactory.java`
   - Modify `HelpController` to use `Extensions.getDashboardGadget("ai-chat", ...)`
   - Update MANIFEST.MF if needed

3. **Testing:**
   - Verify AI Chat Widget appears in Help panel
   - Test plugin deploy/undeploy doesn't break UI
   - Test graceful degradation when plugin not installed

### Migration Path

| Phase | Action | Risk |
|-------|--------|------|
| 1 | Add `AIChatGadgetFactory` to plugin | None (additive) |
| 2 | Modify `HelpController` to try gadget factory first | Low |
| 3 | Remove `IAIChatWidgetFactory` from core | Low (after testing) |

## Implementation Details

### Self-Managing Context Tracking

To achieve 100% plugin autonomy, the AI Chat Widget now self-manages window/tab context tracking using **ZK lifecycle hooks** and **event subscription** without requiring any core code changes.

**Architecture:**

```
AIChatWidget (Plugin)
    ↓ onPageAttached() lifecycle hook
Discover Desktop → Borderlayout → Center → WindowContainer
    ↓ Subscribe to Events.ON_SELECT
WindowContainer fires event on tab change
    ↓ Event listener
Extract windowNo/tabNo via reflection
    ↓ Automatic update
setWindowContext(windowNo, tabNo)
```

**Key Implementation Points:**

1. **Component Discovery** (AIChatWidget.java:1947-1990)
   - Widget walks component tree to find `WindowContainer` (TabbedDocumentPane)
   - Uses `Executions.schedule()` to defer discovery until component tree stabilizes
   - Defensive programming with null checks and exception handling

2. **Event Subscription** (AIChatWidget.java:1882-1915)
   - Subscribes to `Events.ON_SELECT` on WindowContainer
   - Listener fires automatically when user switches tabs
   - No core intervention required

3. **Context Extraction** (AIChatWidget.java:2125-2164)
   - Uses reflection to extract `windowNo` and `tabNo` from ADWindow components
   - No compile-time dependency on core classes
   - Graceful degradation if methods not found

4. **Lifecycle Management** (AIChatWidget.java:1853-1937)
   - `onPageAttached()`: Sets up tracking on widget initialization
   - `onPageDetached()`: Removes event listeners to prevent memory leaks
   - Automatic cleanup on widget removal

**Code Removed from Core:**

- **HelpController.java** (~120 lines):
  - Removed `pnlAIChat` field
  - Removed `aiChatWidget` field
  - Removed AI Chat Panel creation block (lines 161-173)
  - Removed `findAIChatWidget()` method (lines 682-711)
  - Removed `onTabContextChange()` method (lines 802-817)
  - Removed `updateAIChatContext()` method (lines 819-836)

- **DefaultDesktop.java** (~6 lines):
  - Removed call to `helpController.onTabContextChange()` (lines 1213-1218)

**Result:** Zero AI-specific code remains in iDempiereCLDE core.

### CSS Loading via iDempiere Extension Point

To achieve complete self-containment, the plugin uses **iDempiere's NF8.2 Lightweight Theme Customization** extension point for CSS loading instead of explicit JavaScript-based loading.

**Architecture:**

```
com.cloudempiere.ai.theme (Fragment Bundle)
    ↓ Fragment-Host: org.adempiere.ui.zk
    ↓ Jetty-WarFragmentFolderPath: /
theme/default/css/fragment/custom.css.dsp
    ↓ ThemeManager.isThemeHasCustomCSSFragment()
theme.css.dsp includes fragment/custom.css.dsp
    ↓ Automatic inclusion
CSS loaded globally on all pages
```

**Implementation Details:**

1. **Standard Extension Point** (NF8.2):
   - File location: `theme/default/css/fragment/custom.css.dsp`
   - Auto-discovered by `ThemeManager.isThemeHasCustomCSSFragment()`
   - Automatically included in main `theme.css.dsp` compilation
   - No registration code needed

2. **Fragment Bundle Configuration**:
   ```manifest
   Fragment-Host: org.adempiere.ui.zk;bundle-version="10.0.0"
   Jetty-WarFragmentFolderPath: /
   Bundle-SymbolicName: com.cloudempiere.ai.theme;singleton:=true
   ```

3. **DSP Format** (custom.css.dsp):
   ```jsp
   <%@ page contentType="text/css;charset=UTF-8" %>
   <%@ taglib uri="http://www.zkoss.org/dsp/web/core" prefix="c" %>

   /* AI Chat Widget Styles */
   .ai-chat-widget { /* ... */ }
   .ai-chat-panel { /* ... */ }
   /* ... all AI component styles */
   ```

4. **Component Usage** (AIChatWidget.java:200):
   ```java
   // No explicit CSS loading - uses CSS classes only
   setSclass("ai-chat-widget"); // Styles auto-loaded via extension point
   messagesContainer.setSclass("ai-messages");
   sendButton.setSclass("ai-send-btn");
   ```

**Code Removed from Plugin:**

- **AIChatWidget.java** (~18 lines):
  - Removed `loadAIChatPanelCSS()` method (formerly lines 2469-2483)
  - Removed call to `loadAIChatPanelCSS()` (formerly line 228)
  - Removed inline styles in `init()` method (formerly lines 205-206)
  - Replaced with CSS class references and comments

**Benefits:**

- ✅ **Standard iDempiere pattern** - Uses official NF8.2 extension point
- ✅ **Zero registration code** - ThemeManager auto-discovers CSS
- ✅ **Global loading** - CSS available on all pages automatically
- ✅ **Better performance** - Loaded once at startup, browser cached
- ✅ **Theme compatible** - Works with all iDempiere themes
- ✅ **Maintainable** - Follows iDempiere best practices

**Deployment:**

After deploying the fragment bundle, refresh the host:
```
felix> refresh org.adempiere.ui.zk
```

CSS is automatically included in all subsequent page loads.

**Multi-Theme Support:**

The fragment provides CSS for multiple themes to ensure compatibility with custom theme bundles:
- `theme/default/css/fragment/custom.css.dsp` - For standard iDempiere theme
- `theme/cloudempiere/css/fragment/custom.css.dsp` - For Cloudempiere theme (org.cloudempiere.theme)

ThemeManager looks for fragments based on the active theme name:
```java
String customCSSURL = THEME_PATH_PREFIX + theme + "/css/fragment/custom.css.dsp";
```

When the active theme is "cloudempiere", ThemeManager looks for `theme/cloudempiere/css/fragment/custom.css.dsp`. Both CSS files must be maintained in sync to ensure consistent styling across themes.

**References:**
- [NF8.2 Lightweight Theme Customization](https://wiki.idempiere.org/en/NF8.2_Lightweight_theme_customization)
- [Developing Plug-Ins - WebUI Themes](https://wiki.idempiere.org/en/Developing_Plug-Ins_-_WebUI_Themes)

### Code Review Validation

Implementation was validated against iDempiere and project standards using comprehensive code review:

**Approval:** ✅ **APPROVED** - Solution meets architectural requirements

**High-Priority Fixes Implemented:**
- ✅ Fixed component tree timing issues using `Executions.schedule()`
- ✅ Updated logging levels to INFO/WARNING per CLAUDE.md requirements

**Recommendations for Future Work:**
- Add unit test coverage for component discovery and context extraction
- Implement multi-strategy reflection fallbacks for robustness
- Add class name registry for version-specific component lookups
- Consider optimizing to breadth-first search for component discovery

**Documentation Created:**
- [STANDALONE_AI_CHAT_WIDGET.md](../STANDALONE_AI_CHAT_WIDGET.md) - Comprehensive migration documentation
- Code review findings documented in implementation notes

## Temporary Solution Notice

⚠️ **IMPORTANT: This ADR contains a temporary solution that requires core code.**

### Current Implementation

**Location:** `iDempiereCLDE/org.adempiere.ui.zk/WEB-INF/src/org/adempiere/webui/panel/HelpController.java`

**Lines 162-180:** Temporary code marked with visible comments:

```java
// ╔════════════════════════════════════════════════════════════════════════╗
// ║ TEMPORARY CODE - TODO: REMOVE AFTER ADR-053 IMPLEMENTATION            ║
// ║                                                                        ║
// ║ This code temporarily injects AI Chat Widget into help panel.         ║
// ║ Target solution: Floating chat bubble (Zero core changes)             ║
// ║                                                                        ║
// ║ See: docs/adr/053-floating-chat-bubble-zero-core-changes.md          ║
// ║ Issue: CLD-1601                                                        ║
// ║ Target removal: Q1 2026 (after floating bubble implementation)        ║
// ╚════════════════════════════════════════════════════════════════════════╝
try {
    org.zkoss.zk.ui.Component aiChat = org.adempiere.base.Extensions.getDashboardGadget("ai-chat", dashboardColumnLayout, null);
    if (aiChat != null) {
        dashboardColumnLayout.appendChild(aiChat);
    }
} catch (Exception e) {
    // Silently ignore - plugin may not be installed
}
```

### Why Temporary Solution Exists

1. **Goal:** Achieve 100% zero core changes
2. **Challenge:** Help panel integration requires HelpController to call gadget factory
3. **Discovery:** Even minimal core code (1 line) violates zero-changes goal
4. **Solution:** Designed floating chat bubble approach (ADR-053)
5. **Timeline:** Temporary code until floating bubble implemented (Q1 2026)

### Migration to ADR-053 (Floating Chat Bubble)

**Target Architecture:** Floating chat bubble in bottom-right corner (Intercom-style)

**Benefits:**
- ✅ **Zero core changes** - Uses ZK fragment lifecycle injection
- ✅ **Simpler** - 50% less code, no component tree navigation
- ✅ **Better UX** - Always accessible, modern chat widget pattern
- ✅ **More robust** - No dependency on HelpController structure

**Migration Steps:**
1. Implement AIChatBubbleInjector (ZK UiLifeCycle listener)
2. Create AIChatBubble component (floating widget)
3. Add WEB-INF/zk.xml to theme fragment
4. **Remove temporary HelpController code**
5. Remove AIChatGadgetFactory (no longer needed)

**Documentation:** See [ADR-053](053-floating-chat-bubble-zero-core-changes.md)

### Related ADRs

- [ADR-053](053-floating-chat-bubble-zero-core-changes.md) - **Floating Chat Bubble (Target Solution)**
- [ADR-001](001-initial-architecture.md) - Initial architecture decisions
- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j adoption
- [ADR-031](031-chat-panel-langchain4j-chatmodel-integration.md) - Chat panel integration

### References

- [iDempiere Plugin Development](https://wiki.idempiere.org/en/Developing_Plug-Ins)
- [OSGi Declarative Services](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html)
- [iDempiere Extension Points](https://wiki.idempiere.org/en/Extension_Points)

---

*ADR-052 | Created: 2025-12-26 | Status: Partial (with temporary code) | Superseded by: ADR-053*
