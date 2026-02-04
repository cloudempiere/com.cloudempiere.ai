# Standalone AI Chat Widget - Migration Documentation

**Date:** 2025-12-30
**Version:** 10.0.1-SNAPSHOT
**Related ADR:** ADR-052 (OSGi Modernization)

## Overview

The AI Chat Widget has been made 100% standalone - **zero code or references** remain in the core iDempiereCLDE repository. The widget now self-manages its context tracking by discovering the Desktop/WindowContainer and subscribing to ZK events automatically.

## Architecture

### Self-Managing Context Tracking

The widget uses **ZK Desktop Events** to track window/tab context changes without requiring any core code:

```
┌─────────────────────────────────────────────────────────────┐
│ iDempiere Core (iDempiereCLDE)                              │
│  - HelpController: Provides help panels                     │
│  - DefaultDesktop: Manages desktop layout                   │
│  - NO AI-SPECIFIC CODE                                      │
└─────────────────────────────────────────────────────────────┘
                         ▲
                         │ OSGi Extension Point
                         │ Extensions.getDashboardGadget("ai-chat", ...)
                         │
┌─────────────────────────────────────────────────────────────┐
│ com.cloudempiere.ai Plugin                                  │
│  - AIChatGadgetFactory: Returns configured Panel            │
│  - AIChatWidget: Self-managing context tracking             │
│    ├─ onPageAttached(): Discovers WindowContainer           │
│    ├─ Subscribes to Events.ON_SELECT                        │
│    └─ Automatically calls setWindowContext()                │
└─────────────────────────────────────────────────────────────┘
```

### How It Works

1. **Widget Creation** (via Factory Pattern):
   - HelpController calls `Extensions.getDashboardGadget("ai-chat", parent)`
   - AIChatGadgetFactory returns a fully configured Panel with AIChatWidget
   - No AI-specific code in HelpController

2. **Context Discovery** (on Page Attachment):
   - `onPageAttached()` lifecycle hook triggers
   - Widget walks component tree to find Desktop → Borderlayout → Center → WindowContainer
   - Subscribes to `Events.ON_SELECT` on WindowContainer
   - Detects currently active tab and initializes context

3. **Context Updates** (on Tab Changes):
   - User switches tabs → WindowContainer fires `ON_SELECT` event
   - Widget's event listener captures the event
   - Uses reflection to extract windowNo/tabNo from ADWindow component
   - Calls `setWindowContext(windowNo, tabNo)` automatically
   - Context refreshes without core intervention

4. **Cleanup** (on Page Detachment):
   - `onPageDetached()` lifecycle hook triggers
   - Removes event listener from WindowContainer
   - Prevents memory leaks

## Code Changes

### ✅ Added to com.cloudempiere.ai Plugin

#### `AIChatWidget.java`

**New Fields** (lines 158-162):
```java
/** Reference to window container for tab event listening */
private Component windowContainer = null;

/** Event listener for tab selection events */
private EventListener<Event> tabSelectionListener = null;
```

**New Methods** (lines 1844-2196):
- `onPageAttached()` - Sets up automatic context tracking
- `onPageDetached()` - Cleans up event listeners
- `discoverWindowContainer()` - Walks component tree to find WindowContainer
- `handleTabSelectionEvent()` - Processes tab selection events
- `extractContextFromTabpanel()` - Uses reflection to get windowNo/tabNo
- `detectAndSetActiveTab()` - Initializes context on widget creation
- Helper methods for component tree navigation

### ❌ Removed from iDempiereCLDE Core

#### `HelpController.java`

**Removed Fields**:
```java
// Line 73: Removed , pnlAIChat from field declaration
private Panel pnlToolTip, pnlContextHelp, pnlQuickInfo; // (was pnlAIChat)

// Line 75: Removed entire line
private Div aiChatWidget;
```

**Removed Code Blocks**:
- Lines 161-173: AI Chat Panel creation block
- Lines 682-711: `findAIChatWidget()` method
- Lines 802-817: `onTabContextChange()` method
- Lines 819-836: `updateAIChatContext()` method

#### `DefaultDesktop.java`

**Removed Code Block** (lines 1213-1218):
```java
// Removed:
// Notify help gadgets about tab context changes (e.g., AI Chat)
if (X_AD_CtxHelp.CTXTYPE_Tab.equals(ctxType) && gridTab != null) {
    int windowNo = gridTab.getWindowNo();
    int tabNo = gridTab.getTabNo();
    helpController.onTabContextChange(windowNo, tabNo);
}
```

## Technical Details

### Component Tree Navigation

The widget discovers its environment using this hierarchy:

```
AIChatWidget (this)
  ↓ getParent() repeatedly
Panelchildren
  ↓
Panel (pnlAIChat)
  ↓
Anchorchildren
  ↓
Anchorlayout (dashboardLayout)
  ↓
East
  ↓
Borderlayout (layout)
  ↓ getCenter()
Center
  ↓ find by class name
WindowContainer (TabbedDocumentPane)
```

### Event Listening Strategy

```java
// Subscribe to tab selection events
windowContainer.addEventListener(Events.ON_SELECT, tabSelectionListener);

// Event handler extracts context via reflection
private void extractContextFromTabpanel(Tabpanel panel) {
    // Find ADWindow component
    // Call getWindowNo() via reflection
    // Call getADWindowContent().getActiveGridTab().getTabNo()
    // Update context automatically
    setWindowContext(windowNo, tabNo);
}
```

### Defensive Programming

- **Null Safety**: All component discoveries return null if not found
- **Exception Handling**: Try-catch blocks prevent widget crashes
- **Reflection Safety**: Catches `NoSuchMethodException` gracefully
- **Logging**: Fine-level logging for debugging without noise

## Benefits

### ✅ Complete Decoupling
- **Zero dependencies** in core iDempiere code
- Plugin can be removed without leaving orphaned code
- Core remains unaware of AI features

### ✅ Maintainability
- All AI logic in one plugin repository
- No fork maintenance for AI-specific code
- Easier to upgrade core iDempiere

### ✅ Extensibility
- Other plugins can use same pattern
- Standard OSGi extension mechanism
- No custom interfaces needed

### ✅ Reliability
- Widget manages own lifecycle
- Automatic cleanup prevents memory leaks
- Graceful degradation if components not found

## Potential Issues & Solutions

### Issue 1: WindowContainer Not Found
**Symptom**: Widget logs "Could not find WindowContainer"
**Cause**: Component hierarchy changed in iDempiere
**Solution**: Update `findWindowContainerInCenter()` to search for new class names

### Issue 2: Context Not Updating on Tab Changes
**Symptom**: Context remains stuck on first tab
**Cause**: Event not firing or reflection failing
**Solution**: Check ZK logs, verify ADWindow method signatures haven't changed

### Issue 3: Performance Concerns
**Symptom**: Slow tab switching
**Cause**: Component tree search on every event
**Solution**: Cache discovered components (already implemented via `windowContainer` field)

## Testing

### Manual Test Plan

1. **Widget Initialization**
   - Open iDempiere with AI plugin installed
   - Verify AI Chat Widget appears in East panel
   - Check logs for "AI Chat Widget attached to page"

2. **Context Detection**
   - Open a window (e.g., Business Partner)
   - Verify context indicator shows window/tab info
   - Check logs for "Detected tab change: windowNo=X, tabNo=Y"

3. **Tab Switching**
   - Switch to another tab in the window
   - Verify context indicator updates
   - Switch windows, verify context updates

4. **Cleanup**
   - Close all windows
   - Verify no memory leaks (check Desktop event listeners)

### Automated Tests

See `com.cloudempiere.ai.test` bundle:
- `AIChatWidgetTest` - Unit tests for widget initialization
- `AIChatContextTrackingTest` - Tests for context discovery and updates

## Migration Checklist

### For Core iDempiereCLDE Maintainers

- [ ] Pull latest changes from `iDempiereCLDE` branch
- [ ] Verify no AI-specific code in `HelpController.java`
- [ ] Verify no AI-specific code in `DefaultDesktop.java`
- [ ] Test help panels (ToolTip, ContextHelp, QuickInfo) still work
- [ ] Verify Extensions mechanism still calls gadget factories

### For com.cloudempiere.ai Plugin Developers

- [ ] Verify `AIChatWidget` has new lifecycle methods
- [ ] Test widget in clean iDempiere environment
- [ ] Verify context tracking works across windows/tabs
- [ ] Test cleanup on widget removal
- [ ] Update any documentation references

## See Also

- [ADR-052: OSGi Modernization](../docs/adr/052-osgi-modernization.md)
- [AIChatGadgetFactory.java](../src/com/cloudempiere/ai/factory/AIChatGadgetFactory.java)
- [AIChatWidget.java](../src/com/cloudempiere/ai/component/AIChatWidget.java)

## Questions?

Contact: Cloudempiere Development Team
Repository: https://github.com/cloudempiere/com.cloudempiere.ai
