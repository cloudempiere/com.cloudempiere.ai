# com.cloudempiere.ai.theme

OSGi Fragment Plugin for AI Web Resources using iDempiere Theme Extension Point

## Overview

This fragment bundle uses **iDempiere's NF8.2 Lightweight Theme Customization** extension point to automatically load custom CSS for the Cloudempiere AI plugin. The fragment attaches to `org.adempiere.ui.zk` and provides CSS via the standard `fragment/custom.css.dsp` mechanism.

**Reference:** [iDempiere Wiki - NF8.2 Lightweight theme customization](https://wiki.idempiere.org/en/NF8.2_Lightweight_theme_customization)

## Structure

```
com.cloudempiere.ai.theme/
├── META-INF/
│   └── MANIFEST.MF              Fragment configuration
├── theme/
│   ├── default/
│   │   └── css/
│   │       └── fragment/
│   │           └── custom.css.dsp  ← For default theme
│   └── cloudempiere/
│       └── css/
│           └── fragment/
│               └── custom.css.dsp  ← For Cloudempiere theme
├── build.properties
├── .project
├── .classpath
└── README.md
```

## How It Works

### 1. iDempiere Theme Extension Point

The main iDempiere theme (`theme.css.dsp`) includes this conditional:

```jsp
<c:if test="${u:isThemeHasCustomCSSFragment()}">
    <c:include page="fragment/custom.css.dsp" />
</c:if>
```

When `ThemeManager.isThemeHasCustomCSSFragment()` detects our fragment's `custom.css.dsp` file, it automatically includes it in the main theme CSS.

### 2. Fragment Host Configuration

**MANIFEST.MF:**
```manifest
Fragment-Host: org.adempiere.ui.zk;bundle-version="10.0.0"
Jetty-WarFragmentFolderPath: /
Bundle-SymbolicName: com.cloudempiere.ai.theme;singleton:=true
```

**What this does:**
- `Fragment-Host`: Merges with `org.adempiere.ui.zk` at runtime
- `Jetty-WarFragmentFolderPath: /`: Makes resources web-accessible via Jetty
- `singleton:=true`: Ensures only one instance loads

### 3. Automatic CSS Loading

**No explicit loading required!** The CSS is automatically:
1. Discovered by `ThemeManager` on bundle start
2. Included in `theme.css.dsp` compilation
3. Loaded on every page globally
4. Cached by browser for performance

## Usage

### In Java Components

```java
// Apply CSS class - no explicit CSS loading needed!
this.setSclass("ai-chat-widget");
messagesContainer.setSclass("ai-messages");
sendButton.setSclass("ai-send-btn");
```

### In ZUL Files

```xml
<window apply="com.cloudempiere.ai.component.AIChatPanel">
    <div sclass="ai-chat-panel">
        <div sclass="ai-chat-header">
            <label value="AI Assistant" sclass="ai-chat-header-title"/>
        </div>
        <div sclass="ai-chat-messages">
            <!-- Chat messages -->
        </div>
    </div>
</window>
```

### Available CSS Classes

All classes defined in `theme/default/css/fragment/custom.css.dsp`:

- `.ai-chat-widget` - Main widget container
- `.ai-chat-panel` - Chat panel wrapper
- `.ai-chat-header` - Header section
- `.ai-chat-messages` / `.ai-messages` - Messages container
- `.ai-message` - Message bubble
- `.ai-message-user` - User message
- `.ai-message-assistant` - AI response
- `.user-message` - Alternative user message style
- `.ai-send-btn` - Send button
- `.ai-stop-btn` - Stop button
- `.ai-clear-btn` - Clear button
- `.ai-newthread-btn` - New thread button
- `.ai-provider-badge` - Provider indicator
- `.ai-streaming-indicator` - Streaming animation
- `.ai-error-message` - Error states
- `.ai-loading` / `.ai-loading-spinner` - Loading states

## Build

### Maven Build

```bash
mvn clean install
```

### Eclipse Build

1. Import as "Existing Maven Project"
2. Project builds automatically with PDE/Maven
3. Fragment JAR created in `target/`

## Deployment

### 1. Deploy Fragment

Copy `com.cloudempiere.ai.theme-10.0.0.jar` to iDempiere `plugins/` directory.

### 2. Refresh Host Bundle

In Felix console:
```
refresh org.adempiere.ui.zk
```

### 3. Verify Loading

CSS is automatically included in all pages. Check browser dev tools:
- Look for CSS rules from `custom.css.dsp`
- Styles should apply to elements with `ai-*` classes

### Development Mode

In Eclipse with iDempiere:
1. Fragment loads automatically when host bundle starts
2. CSS changes require refreshing `org.adempiere.ui.zk`
3. No code changes needed - pure CSS customization

## Benefits of This Approach

✅ **Standard iDempiere Pattern** - Uses official NF8.2 extension point
✅ **Zero Registration Code** - ThemeManager auto-discovers the CSS
✅ **Global Loading** - CSS available on all pages automatically
✅ **Better Performance** - Loaded once, cached by browser
✅ **Hot Swappable** - Update CSS without rebuilding Java code
✅ **Theme Compatible** - Works with all iDempiere themes
✅ **100% Self-Contained** - No core modifications required

## Version Compatibility

- **iDempiere Version:** v10 (10.0.0-SNAPSHOT)
- **Jetty Version:** 10.0.7 (uses `Jetty-WarFragmentFolderPath: /`)
- **Java Version:** JavaSE-11
- **Extension Point:** NF8.2 (available since iDempiere v8.2)

## Multi-Theme Support

This fragment provides CSS for **multiple themes** to ensure compatibility with custom theme bundles:

- **default** - Standard iDempiere theme
- **cloudempiere** - Cloudempiere custom theme (org.cloudempiere.theme)

ThemeManager dynamically looks for fragments based on the **active theme name**:
```java
String customCSSURL = THEME_PATH_PREFIX + theme + "/css/fragment/custom.css.dsp";
```

If you use a different custom theme, you need to add the CSS fragment for that theme:
```bash
mkdir -p theme/YOUR_THEME_NAME/css/fragment/
cp theme/default/css/fragment/custom.css.dsp theme/YOUR_THEME_NAME/css/fragment/
```

Both CSS files should be maintained in sync to ensure consistent styling across themes.

## Related Bundles

- **Host**: `org.adempiere.ui.zk` (ZK Web UI)
- **Companion**: `com.cloudempiere.ai` (Java code, providers, models)

## References

- [iDempiere NF8.2 Lightweight Theme Customization](https://wiki.idempiere.org/en/NF8.2_Lightweight_theme_customization)
- [iDempiere WebUI Themes Development](https://wiki.idempiere.org/en/Developing_Plug-Ins_-_WebUI_Themes)
- [iDempiere Plugin Guidelines](https://wiki.idempiere.org/en/Plugin_Guidelines)
- [OSGi Fragment Bundles](https://www.osgi.org/developer/architecture/)

## License

Same as parent project com.cloudempiere.ai
