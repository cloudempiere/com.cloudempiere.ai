# Zoom Link Implementation Analysis

**Status**: Implementation Complete, Awaiting Server Restart for Testing
**Date**: 2025-12-10
**Related**: ADR-039

## Current Issue

Zoom links appear correctly in the chat panel and JavaScript successfully sends the zoom event to the server, but the record window doesn't open.

**Evidence from Browser Console** (Screenshot 3):
```
[Zoom] Looking for widget: j9aQ82
[Zoom] Widget found, sending event
[Zoom] Event sent
```

## Implementation Comparison: Chart vs Chat Panel

### Chart Implementation (`ChartRendererServiceImpl.java:311-344`)

```java
private static class ZoomListener implements EventListener<Event> {
    private Map<String, MQuery> queries;

    @Override
    public void onEvent(Event event) throws Exception {
        if(event.getName().equalsIgnoreCase(Billboard.ON_DATA_CLICK_EVENT)) {
            JSONObject json = (JSONObject) event.getData();
            Number seriesIndex = (Number) json.get("seriesIndex");
            Number pointIndex = (Number) json.get("pointIndex");

            MQuery query = null;
            // ... query lookup logic from pre-built map ...

            if (query != null)
                AEnv.zoom(query);  // ← Simple direct call
        }
    }
}
```

**Key Points**:
- Pre-builds `MQuery` objects when chart is created
- Looks up query from map based on series/point index
- Calls `AEnv.zoom(query)` directly with no special handling
- No query construction in event handler

### Chat Panel Implementation (`AIChatWidget.java:665-724`)

```java
private void handleZoomEvent(Event event) {
    try {
        Object eventData = event.getData();
        log.warning("[Zoom Handler] Event received, data type: " +
            (eventData != null ? eventData.getClass().getName() : "null"));

        if (eventData instanceof JSONObject) {
            JSONObject jsonData = (JSONObject) eventData;
            log.warning("[Zoom Handler] JSON data: " + jsonData.toString());

            if (jsonData.has("data")) {
                Object dataObj = jsonData.get("data");
                JSONArray data = null;

                if (dataObj instanceof JSONArray) {
                    data = (JSONArray) dataObj;
                } else if (dataObj instanceof String) {
                    data = new JSONArray((String) dataObj);
                }

                if (data != null && data.length() >= 2) {
                    String columnName = data.getString(0);  // e.g., "C_Order_ID"
                    String valueStr = data.getString(1);    // e.g., "1234"
                    log.warning("[Zoom Handler] columnName=" + columnName +
                               ", valueStr=" + valueStr);

                    String tableName = MQuery.getZoomTableName(columnName);
                    log.warning("[Zoom Handler] Resolved tableName=" + tableName);

                    int recordId = Integer.parseInt(valueStr);

                    if (recordId > 0) {
                        log.warning("[Zoom Handler] Calling AEnv.zoom() for " +
                                   tableName + "#" + recordId);

                        MQuery query = new MQuery(tableName);
                        query.addRestriction(columnName, MQuery.EQUAL, recordId);
                        query.setRecordCount(1);
                        query.setZoomTableName(tableName);
                        query.setZoomColumnName(columnName);
                        query.setZoomValue(recordId);

                        AEnv.zoom(query);
                        log.warning("[Zoom Handler] AEnv.zoom() completed");
                        return;
                    }
                }
            }
        }
    } catch (Exception e) {
        log.log(Level.SEVERE, "Failed to handle zoom event", e);
        Clients.showNotification(Msg.getMsg(Env.getCtx(), "Error") + ": " + e.getMessage(),
            "error", this, null, -1);
    }
}
```

**Key Points**:
- Constructs `MQuery` on-the-fly from event data
- Uses standard iDempiere format: `{data: [columnName, recordId]}`
- Follows same pattern as `ZoomCommand` in report.js
- Calls `AEnv.zoom(query)` with properly configured query
- Extensive logging for diagnostics

## Implementation Assessment

✅ **CORRECT**: Our implementation follows iDempiere patterns correctly:

1. **Event Format**: Uses standard `{data: [columnName, recordId]}` format
2. **Query Construction**: Properly creates `MQuery` with all required fields
3. **Zoom Call**: Calls `AEnv.zoom(query)` exactly like chart implementation
4. **Error Handling**: Comprehensive exception catching and user notifications

❓ **UNKNOWN**: Why zoom window doesn't open despite successful event send

## Client-Side Code (JavaScript)

**Location**: `ZoomLinkProcessor.java:142-168`

```javascript
(function(){
  try{
    var zkObj=window.zk||parent.zk;
    var auObj=window.zAu||parent.zAu;
    if(!zkObj){console.error('[Zoom] ZK object not found');return;}
    if(!auObj){console.error('[Zoom] zAu object not found');return;}
    console.log('[Zoom] Looking for widget: j9aQ82');
    var w=zkObj.Widget.$('j9aQ82');
    if(!w){console.error('[Zoom] Widget not found');return;}
    console.log('[Zoom] Widget found, sending event');
    var evt=new zkObj.Event(w,'onZoom',
      {data:['C_Order_ID','2330003']},
      {toServer:true});
    auObj.send(evt);
    console.log('[Zoom] Event sent');
  }catch(e){console.error('[Zoom] Error:',e);}
})();
```

✅ **WORKING**: Browser console shows all three success messages

## Diagnostic Steps (After Server Restart)

### 1. Check Server Logs for Event Reception

After clicking a zoom link, look for these log messages:

```
[Zoom Handler] Event received, data type: org.json.JSONObject
[Zoom Handler] JSON data: {"data":["C_Order_ID","2330003"]}
[Zoom Handler] columnName=C_Order_ID, valueStr=2330003
[Zoom Handler] Resolved tableName=C_Order
[Zoom Handler] Calling AEnv.zoom() for C_Order#2330003
[Zoom Handler] AEnv.zoom() completed
```

### 2. Possible Scenarios

#### Scenario A: No Logs Appear
**Issue**: Event not reaching server-side handler
**Possible Causes**:
- Event listener not registered properly
- Widget UUID mismatch
- ZK event routing issue

**Debug Actions**:
- Verify `addEventListener("onZoom", ...)` is called in `onCreate()`
- Check if widget UUID in HTML matches component UUID
- Add log in event listener registration

#### Scenario B: Logs Appear, AEnv.zoom() Called
**Issue**: `AEnv.zoom()` fails silently
**Possible Causes**:
- Desktop/session context not available
- Window already open
- Permission issues
- Query malformed

**Debug Actions**:
- Check for exceptions in `AEnv.zoom()` source
- Verify `Env.getCtx()` has valid desktop
- Test with simpler query (just table name, no restrictions)

#### Scenario C: Logs Up To "Calling AEnv.zoom()" Only
**Issue**: `AEnv.zoom()` blocks or throws exception
**Possible Causes**:
- Exception in AEnv.zoom() not caught by our handler
- Deadlock or blocking call
- Missing window definition in Application Dictionary

**Debug Actions**:
- Wrap `AEnv.zoom()` in try-catch with stack trace logging
- Check if record/table exists
- Verify AD_Window definition exists for table

## Testing Plan

### Phase 1: Verify Event Reception (Immediate)
1. Restart iDempiere server with new compiled code
2. Open chat panel
3. Trigger a zoom link click
4. Check server console/logs for `[Zoom Handler]` messages

### Phase 2: Isolate AEnv.zoom() (If Event Received)
1. If logs show event received but no zoom, add try-catch around `AEnv.zoom()`
2. Log full stack trace if exception occurs
3. Test with direct `AEnv.zoom()` call (not from chat panel)

### Phase 3: Compare With Working Implementation (If Still Failing)
1. Add zoom link to chart component
2. Compare event data format
3. Compare desktop/session context
4. Check if `Html` component affects ZK context

## Known Issues Fixed

### Issue 1: JavaScript Context Isolation ✅ FIXED
**Problem**: `Html` component runs in isolated context without `zk`/`zAu` objects
**Solution**: Try `window.zk||parent.zk` with fallback logic

### Issue 2: HTML Sanitization ✅ FIXED
**Problem**: Markdown rendering escaped HTML tags
**Solution**: Added `sanitize: false` to `marked.setOptions()`

### Issue 3: AI Response Format ✅ FIXED
**Problem**: AI didn't know to format records as `[[Table:ID|Display]]`
**Solution**: Added "RECORD REFERENCE FORMAT" section to system prompt

## Next Steps

1. **IMMEDIATE**: Restart server and check for `[Zoom Handler]` logs
2. **IF NO LOGS**: Debug event listener registration
3. **IF LOGS APPEAR**: Debug `AEnv.zoom()` execution
4. **IF STILL FAILING**: Compare with chart implementation in debugger

## Files Modified

- `AIChatWidget.java:665-724` - Event handler with extensive logging
- `AIChatWidget.java:1889` - Markdown sanitization disabled
- `ZoomLinkProcessor.java:142-168` - JavaScript context fix
- `ChatRecordLinkRenderer.java:177-199` - JavaScript context fix
- `ERPAgent.java:55-65` - AI system prompt for record format

## References

- ADR-039: Record Reference Detection and Zoom Links
- `ChartRendererServiceImpl.java:311-344` - Working zoom implementation
- `ZoomCommand.java` - Standard iDempiere zoom pattern
- `MQuery.java` - Query construction utilities
