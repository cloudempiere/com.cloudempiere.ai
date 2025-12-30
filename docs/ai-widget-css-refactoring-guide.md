# AI Chat Widget CSS Refactoring Guide

## Problem

The AI Chat Widget component is using **18 inline style declarations** via `.setStyle()` which override the CSS from the theme fragment. Inline styles always have higher CSS specificity than external CSS classes, preventing the theme fragment styles from being applied.

## Solution

1. **Updated CSS** with `!important` flags to override default ZK styles
2. **Need to refactor Java component** to use CSS classes instead of inline styles

## Changes Made to CSS

**File:** `com.cloudempiere.ai.theme/web/theme/default/css/fragment/custom.css.dsp`

Added the following CSS classes with proper styling:

- `.ai-chat-widget` - Main container (updated height to 100%)
- `.ai-thread-control-bar` - Thread control bar layout
- `.ai-thread-selector` - Thread selector dropdown
- `.ai-messages` - Messages container with !important flags
- `.ai-input-area` - Input area container
- `.ai-input-box` - Input textbox with !important flags
- `.ai-button-container` - Button container positioning
- `.ai-send-btn` - Send button with !important flags
- `.ai-stop-btn` - Stop button with !important flags
- `.ai-context-indicator` - Context indicator
- `.ai-access-indicator` - Access indicator
- `.ai-loading-container` - Loading indicator container
- `.ai-loading-content` - Loading content flex layout
- `.ai-loading-avatar` - Loading avatar circle
- `.ai-loading-text` - Loading text styling

All critical styles use `!important` to ensure they override ZK default styles.

## Required Java Component Changes

### File: `AIChatWidget.java`

### 1. Thread Control Bar (Line ~264)

**Current:**
```java
Hlayout threadControlBar = new Hlayout();
threadControlBar.setStyle("width: 100%; gap: 8px; align-items: center; margin-bottom: 8px; flex-shrink: 0;");
```

**Change to:**
```java
Hlayout threadControlBar = new Hlayout();
threadControlBar.setSclass("ai-thread-control-bar");
// Remove setStyle()
```

### 2. Thread Selector (Line ~267-269)

**Current:**
```java
threadSelector = new Combobox();
threadSelector.setPlaceholder("Select conversation...");
threadSelector.setStyle("border: 1px solid #E0E0E0; border-radius: 6px; font-size: 12px; background: #FFFFFF;");
```

**Change to:**
```java
threadSelector = new Combobox();
threadSelector.setPlaceholder("Select conversation...");
threadSelector.setSclass("ai-thread-selector");
// Remove setStyle()
```

### 3. Messages Container (Line ~288-292)

**Current:**
```java
messagesContainer = new Vlayout();
messagesContainer.setSclass("ai-messages");
ZKUpdateUtil.setVflex(messagesContainer, "1");
messagesContainer.setStyle("overflow-y: auto; overflow-x: hidden; margin-bottom: 12px; " +
		"padding: 6px; gap: 8px; flex: 1 1 auto; min-height: 0;");
```

**Change to:**
```java
messagesContainer = new Vlayout();
messagesContainer.setSclass("ai-messages");
ZKUpdateUtil.setVflex(messagesContainer, "1");
// Remove setStyle() - all styling now in CSS
```

### 4. Input Area (Line ~305-306)

**Current:**
```java
Hlayout inputArea = new Hlayout();
inputArea.setStyle("width: 100%; gap: 8px; align-items: center; flex-shrink: 0;");
```

**Change to:**
```java
Hlayout inputArea = new Hlayout();
inputArea.setSclass("ai-input-area");
// Remove setStyle()
```

### 5. Input Box (Line ~308-314)

**Current:**
```java
inputBox = new Textbox();
inputBox.setPlaceholder(Msg.getMsg(Env.getCtx(), "AIChatPlaceholder"));
ZKUpdateUtil.setHflex(inputBox, "1");
inputBox.setRows(1);
inputBox.setMultiline(false);
inputBox.setStyle("border: 1px solid rgba(122, 128, 140, 0.32); border-radius: 22px; padding: 12px 18px; " +
	"font-size: 12px; line-height: 18px; color: #717680; background: #FFFFFF;");
```

**Change to:**
```java
inputBox = new Textbox();
inputBox.setPlaceholder(Msg.getMsg(Env.getCtx(), "AIChatPlaceholder"));
ZKUpdateUtil.setHflex(inputBox, "1");
inputBox.setRows(1);
inputBox.setMultiline(false);
inputBox.setSclass("ai-input-box");
// Remove setStyle()
```

### 6. Button Container (Line ~318-319)

**Current:**
```java
Div buttonContainer = new Div();
buttonContainer.setStyle("position: relative; width: 42px; height: 42px;");
```

**Change to:**
```java
Div buttonContainer = new Div();
buttonContainer.setSclass("ai-button-container");
// Remove setStyle()
```

### 7. Send Button (Line ~321-329)

**Current:**
```java
sendButton = new Button();
sendButton.addEventListener(Events.ON_CLICK, this);
sendButton.setSclass("ai-send-btn");
if (ThemeManager.isUseFontIconForImage())
	sendButton.setIconSclass("z-icon-Send-White");
else
	sendButton.setImage(ThemeManager.getThemeResource("images/Send-White.png"));
sendButton.setStyle("position: absolute; top: 0; left: 0; width: 42px; height: 42px; background: #181D27; border-radius: 100px; " +
	"display: flex; align-items: center; justify-content: center; border: none; cursor: pointer;");
```

**Change to:**
```java
sendButton = new Button();
sendButton.addEventListener(Events.ON_CLICK, this);
sendButton.setSclass("ai-send-btn");
if (ThemeManager.isUseFontIconForImage())
	sendButton.setIconSclass("z-icon-Send-White");
else
	sendButton.setImage(ThemeManager.getThemeResource("images/Send-White.png"));
// Remove setStyle() - all styling now in CSS
```

### 8. Stop Button (Line ~332-340)

**Current:**
```java
stopButton = new Button();
stopButton.addEventListener(Events.ON_CLICK, this);
stopButton.setSclass("ai-stop-btn");
if (ThemeManager.isUseFontIconForImage())
	stopButton.setIconSclass("z-icon-Square-White");
else
	stopButton.setImage(ThemeManager.getThemeResource("images/Cancel24.png"));
stopButton.setStyle("position: absolute; top: 0; left: 0; width: 42px; height: 42px; background: #D32F2F; border-radius: 100px; " +
	"display: none; align-items: center; justify-content: center; border: none; cursor: pointer;");
stopButton.setTooltiptext(Msg.getMsg(Env.getCtx(), "Stop"));
```

**Change to:**
```java
stopButton = new Button();
stopButton.addEventListener(Events.ON_CLICK, this);
stopButton.setSclass("ai-stop-btn");
if (ThemeManager.isUseFontIconForImage())
	stopButton.setIconSclass("z-icon-Square-White");
else
	stopButton.setImage(ThemeManager.getThemeResource("images/Cancel24.png"));
stopButton.setTooltiptext(Msg.getMsg(Env.getCtx(), "Stop"));
// Remove setStyle() - all styling now in CSS
```

### 9. Context Indicator (if contextEnabled, Line ~244-252)

**Current:**
```java
contextIndicator = new Html();
contextIndicator.setId("aiContextIndicator_" + getUuid());
contextIndicator.setContent(
	"<div style='padding: 6px 12px; background: #E8F5E9; border-radius: 4px; " +
	"margin-bottom: 8px; font-size: 11px; color: #2E7D32; display: none;'>" +
	"<i class='z-icon-InfoCircle'></i> Context: <span id='contextInfo_" + getUuid() + "'>No window open</span>" +
	"</div>"
);
```

**Change to:**
```java
contextIndicator = new Html();
contextIndicator.setId("aiContextIndicator_" + getUuid());
contextIndicator.setContent(
	"<div class='ai-context-indicator' id='contextIndicatorDiv_" + getUuid() + "'>" +
	"<i class='z-icon-InfoCircle'></i> Context: <span id='contextInfo_" + getUuid() + "'>No window open</span>" +
	"</div>"
);
// Remove inline styles - all styling now in CSS
```

### 10. Loading Indicator (Line ~296-302)

**Current:**
```java
loadingIndicator = new Html();
loadingIndicator.setContent("<div class='ai-loading' style='display:none; padding: 12px 18px; text-align: left;'>" +
	"<div style='display: flex; align-items: center; gap: 8px;'>" +
	"<div style='width: 18px; height: 18px; border-radius: 27px; background: #E9EAEB;'></div>" +
	"<span style='font-family: Helvetica Neue; font-weight: 400; font-size: 12px; line-height: 18px; color: #717680;'>" +
	"<i>" + Msg.getMsg(Env.getCtx(), "AIThinking") + "</i></span></div></div>");
```

**Change to:**
```java
loadingIndicator = new Html();
loadingIndicator.setContent("<div class='ai-loading-container'>" +
	"<div class='ai-loading-content'>" +
	"<div class='ai-loading-avatar'></div>" +
	"<span class='ai-loading-text'>" +
	"<i>" + Msg.getMsg(Env.getCtx(), "AIThinking") + "</i></span></div></div>");
```

## Summary of Changes

- Remove all `.setStyle()` calls from the component (18 occurrences)
- Use `.setSclass()` with the CSS class names defined in `custom.css.dsp`
- Update HTML content to use CSS classes instead of inline styles
- Keep `.setPlaceholder()`, `.setTooltiptext()`, and other non-style setters

## Testing

After making these changes:

1. **Rebuild main plugin:** `mvn clean package`
2. **Deploy both bundles:**
   - `com.cloudempiere.ai-10.0.1-SNAPSHOT.jar`
   - `com.cloudempiere.ai.theme-10.0.0-SNAPSHOT.jar`
3. **Refresh bundles in OSGi console**
4. **Clear browser cache**
5. **Open AI Chat Widget**
6. **Verify styles are applied:**
   - Check browser DevTools → Elements → verify CSS classes
   - Check browser DevTools → Computed styles → verify no inline styles
   - Verify button styling, input styling, layout

## Additional Changes - Button Visibility Control

### CSS Classes Added for Visibility Control

Added to all four CSS files:

```css
/* Visibility control classes */
.ai-hidden {
    display: none !important;
}

.ai-visible {
    display: flex !important;
}
```

### Java Code Updates

**File:** `AIChatWidget.java`

**1. Stop Button Initialization (Line ~334)**

Changed from:
```java
stopButton.setSclass("ai-stop-btn");
```

Changed to:
```java
stopButton.setSclass("ai-stop-btn ai-hidden");
```

**2. showStopButton() Method (Line ~2615-2617)**

Changed from:
```java
private void showStopButton() {
    sendButton.setStyle(sendButton.getStyle().replace("display: flex", "display: none"));
    stopButton.setStyle(stopButton.getStyle().replace("display: none", "display: flex"));
}
```

Changed to:
```java
private void showStopButton() {
    sendButton.setSclass("ai-send-btn ai-hidden");
    stopButton.setSclass("ai-stop-btn ai-visible");
}
```

**3. showSendButton() Method (Line ~2624-2626)**

Changed from:
```java
private void showSendButton() {
    stopButton.setStyle(stopButton.getStyle().replace("display: flex", "display: none"));
    sendButton.setStyle(sendButton.getStyle().replace("display: none", "display: flex"));
}
```

Changed to:
```java
private void showSendButton() {
    stopButton.setSclass("ai-stop-btn ai-hidden");
    sendButton.setSclass("ai-send-btn ai-visible");
}
```

**4. CSS Update for .ai-stop-btn**

The `display: none !important;` property was removed from `.ai-stop-btn` CSS rule and replaced with:
```css
/* display controlled by ai-hidden/ai-visible classes */
```

## Files Modified

1. ✅ `com.cloudempiere.ai.theme/web/theme/default/css/fragment/custom.css.dsp`
2. ✅ `com.cloudempiere.ai.theme/web/theme/cloudempiere/css/fragment/custom.css.dsp`
3. ✅ `com.cloudempiere.ai.theme/WEB-INF/src/web/theme/default/css/fragment/custom.css.dsp`
4. ✅ `com.cloudempiere.ai.theme/WEB-INF/src/web/theme/cloudempiere/css/fragment/custom.css.dsp`
5. ✅ `com.cloudempiere.ai.theme/META-INF/MANIFEST.MF` (version corrected to 10.0.0.qualifier)
6. ✅ `com.cloudempiere.ai/src/com/cloudempiere/ai/component/AIChatWidget.java` (refactored)

## Backup

Original component backed up at:
- `AIChatWidget.java.backup`

## References

- CSS Specificity: Inline styles > CSS classes
- ZK setStyle() documentation: https://www.zkoss.org/javadoc/latest/zk/org/zkoss/zk/ui/HtmlBasedComponent.html#setStyle-java.lang.String-
- iDempiere Theme Extension: NF8.2 Lightweight theme customization
