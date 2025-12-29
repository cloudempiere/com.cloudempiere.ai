# ADR-052: AI Chat Widget Core Decoupling

## Status

Accepted (Implemented)

## Date

2025-12-26

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
- [x] `HelpController` uses `Extensions.getDashboardGadget("ai-chat", ...)`
- [ ] AI Chat Widget displays correctly in Help panel (requires testing)
- [ ] Plugin can be undeployed without breaking core UI (requires testing)

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

### Related ADRs

- [ADR-001](001-initial-architecture.md) - Initial architecture decisions
- [ADR-002](002-langchain4j-strategic-adoption.md) - LangChain4j adoption
- [ADR-031](031-chat-panel-langchain4j-chatmodel-integration.md) - Chat panel integration

### References

- [iDempiere Plugin Development](https://wiki.idempiere.org/en/Developing_Plug-Ins)
- [OSGi Declarative Services](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html)
- [iDempiere Extension Points](https://wiki.idempiere.org/en/Extension_Points)

---

*ADR-052 | Created: 2025-12-26 | CLD-1601*
