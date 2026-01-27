# Chat Tenant Isolation and Lazy Creation Fix

## Issue Summary

Users experienced cross-tenant data access errors and `NullPointerException` when logging into different tenants than where their chat was created. The error occurred because:

1. **Cross-tenant data leak**: The `getGlobalChatID()` method had a fallback query that retrieved chats from other tenants
2. **Eager chat creation**: Chat was created during widget initialization, causing NPE when tenant validation failed
3. **Missing tenant filtering**: Queries didn't properly filter by `AD_Client_ID` for tenant isolation

## Error Log Example

```
Cross tenant PO reading request detected from session 23200892 for table CM_Chat Record_ID=1000189
    at org.compiere.model.PO.checkCrossTenant(PO.java:5749)
    at com.cloudempiere.ai.model.MAIChat.getGlobalChatID(MAIChat.java:141)
    at com.cloudempiere.ai.model.MAIChat.getOrCreateGlobalChat(MAIChat.java:104)
    at com.cloudempiere.ai.component.AIChatWidget.loadOrCreateChat(AIChatWidget.java:359)

java.lang.NullPointerException
    at org.compiere.model.MChatEntry.<init>(MChatEntry.java:58)
    at com.cloudempiere.ai.model.MAIChatEntry.<init>(MAIChatEntry.java:71)
    at com.cloudempiere.ai.component.AIChatWidget.sendMessage(AIChatWidget.java:938)
```

## Changes Made

### 1. MAIChat.java - Tenant Isolation

#### getOrCreateGlobalChat()

**Before:**
```java
// Query without AD_Client_ID filter - constraint only allows one per (table, record)
MChat chat = new Query(ctx, MChat.Table_Name, whereClause, null)
    .setParameters(AD_Table_ID, Record_ID)
    .setClient_ID() // Still respect client security for the query itself
    .first();

// PROBLEMATIC: If not found with client filter, try without (for cross-tenant users)
if (chat == null) {
    chat = new Query(Env.getCtx(), MChat.Table_Name, whereClause, null)
        .setParameters(AD_Table_ID, Record_ID)
        .first(); // ← CROSS-TENANT QUERY!
}
```

**After:**
```java
// CRITICAL: Always filter by BOTH user AND client for tenant isolation
int chatId = getGlobalChatID(ctx, AI_GLOBAL_TABLE_ID, userId, clientId);

// Added dontCreate parameter for lazy creation support
public static MAIChat getOrCreateGlobalChat(Properties ctx, String trxName, boolean dontCreate) {
    // Returns null if chat doesn't exist and dontCreate is true
    // This enables lazy creation pattern
}
```

**Key improvements:**
- ✅ Always filters by `AD_Client_ID` - no cross-tenant queries
- ✅ System users (Client_ID=0) get separate chats per tenant they log into
- ✅ Non-system users only see their own tenant's chats
- ✅ Added lazy creation support via `dontCreate` parameter
- ✅ Sets `ConfidentialType` to Private by default (ADR-036)

#### getGlobalChatID()

**Before:**
```java
private static int getGlobalChatID(Properties ctx, int AD_Table_ID, int Record_ID) {
    String whereClause = "AD_Table_ID=? AND Record_ID=?";
    // Missing AD_Client_ID filter!
}
```

**After:**
```java
private static int getGlobalChatID(Properties ctx, int AD_Table_ID, int Record_ID, int AD_Client_ID) {
    // CRITICAL: Filter by BOTH table/record AND client for tenant isolation
    String whereClause = "AD_Table_ID=? AND Record_ID=? AND AD_Client_ID=?";
    MChat chat = new Query(ctx, MChat.Table_Name, whereClause, null)
        .setParameters(AD_Table_ID, Record_ID, AD_Client_ID)
        .first();
}
```

**Key improvements:**
- ✅ Added `AD_Client_ID` parameter
- ✅ Always filters by tenant in WHERE clause
- ✅ Each tenant has separate global chats, even for the same user

#### getOrCreateContextChat()

**Before:**
```java
int chatId = MChat.getID(AD_Table_ID, Record_ID);
// No tenant filtering!
```

**After:**
```java
int clientId = Env.getAD_Client_ID(ctx);
String whereClause = "AD_Table_ID=? AND Record_ID=? AND AD_Client_ID=?";
MChat existingChat = new Query(ctx, MChat.Table_Name, whereClause, trxName)
    .setParameters(AD_Table_ID, Record_ID, clientId)
    .first();
```

**Key improvements:**
- ✅ Context chats are also tenant-isolated
- ✅ Each tenant has separate context chats for the same record
- ✅ Prevents cross-tenant data leaks in record-linked chats

### 2. AIChatWidget.java - Lazy Creation Pattern

#### loadExistingChat() (renamed from loadOrCreateChat)

**Before:**
```java
private void loadOrCreateChat() {
    // ALWAYS creates chat on init
    chat = MAIChat.getOrCreateGlobalChat(sessionCtx, null);
    // If this fails due to cross-tenant error, chat is null → NPE later
}
```

**After:**
```java
private void loadExistingChat() {
    // Try to load existing chat WITHOUT creating it (lazy creation)
    chat = MAIChat.getOrCreateGlobalChat(sessionCtx, null, true);

    if (chat == null) {
        // No chat exists yet - this is OK!
        // Widget will create one on first message submission
        log.fine("No existing chat found - will create on first message");
        currentAccess = ChatAccess.OWNER;
        updateAccessIndicator();
        inputBox.setPlaceholder(Msg.getMsg(sessionCtx, "AIChatPlaceholder"));
        return;
    }

    // Load existing chat...
}
```

**Key improvements:**
- ✅ Chat is NOT created during widget initialization
- ✅ Prevents cross-tenant errors on init
- ✅ Prevents unnecessary chat creation for users who don't interact with AI
- ✅ No NPE when chat fails to load

#### sendMessage() - Lazy Chat Creation

**Before:**
```java
public void sendMessage() {
    // Assumes chat exists - NPE if null!
    MAIChatEntry userEntry = new MAIChatEntry(chat, message);
}
```

**After:**
```java
public void sendMessage() {
    // LAZY CHAT CREATION: Create chat on first message if it doesn't exist
    if (chat == null) {
        try {
            log.fine("Creating chat on first message for user " +
                Env.getAD_User_ID(sessionCtx) + " in client " +
                Env.getAD_Client_ID(sessionCtx));

            // Create chat in current tenant
            chat = MAIChat.getOrCreateGlobalChat(sessionCtx, null, false);

            if (chat == null) {
                log.severe("Failed to create chat");
                Clients.showNotification("Failed to create chat",
                    "error", inputBox, "top_center", 5000);
                return;
            }

            currentAccess = ChatAccess.OWNER;
            updateAccessIndicator();

            log.fine("Chat created successfully: ID=" + chat.get_ID());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to create chat on first message", e);
            Clients.showNotification("Failed to create chat: " + e.getMessage(),
                "error", inputBox, "top_center", 5000);
            return;
        }
    }

    // Now safe to create entry
    MAIChatEntry userEntry = new MAIChatEntry(chat, message);
}
```

**Key improvements:**
- ✅ Chat is created on first message, not on init
- ✅ Proper error handling with user notifications
- ✅ Defensive null checks prevent NPE
- ✅ Creator becomes owner automatically

## Architecture Changes per ADR-036

### Tenant Isolation Model

```
┌─────────────────────────────────────────────────────────────────┐
│                    Tenant Isolation Model                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  System User (AD_Client_ID=0)                                   │
│    ├─> Logs into Tenant A (AD_Client_ID=1000026)                │
│    │   └─> Gets Chat A (CM_Chat_ID=X, AD_Client_ID=1000026)     │
│    │                                                             │
│    └─> Logs into Tenant B (AD_Client_ID=1000083)                │
│        └─> Gets Chat B (CM_Chat_ID=Y, AD_Client_ID=1000083)     │
│                                                                  │
│  Tenant User (AD_Client_ID=1000026)                             │
│    └─> Can ONLY log into Tenant A                               │
│        └─> Sees Chat A (CM_Chat_ID=X, AD_Client_ID=1000026)     │
│                                                                  │
│  Each tenant has separate:                                       │
│    • Global chats (per user)                                     │
│    • Context chats (per record)                                  │
│    • Chat entries (filtered by AD_Client_ID)                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Chat Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│                       Chat Lifecycle                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Widget Init (AIChatWidget constructor)                       │
│     ├─> loadExistingChat()                                       │
│     │   ├─> getOrCreateGlobalChat(ctx, null, true) [lazy]       │
│     │   │   └─> Returns existing chat OR null                    │
│     │   └─> If null: Show empty state                            │
│     └─> No errors, no cross-tenant queries                       │
│                                                                  │
│  2. First Message Submission                                     │
│     ├─> sendMessage()                                            │
│     │   ├─> if (chat == null)                                    │
│     │   │   └─> getOrCreateGlobalChat(ctx, null, false)          │
│     │   │       └─> Creates chat in current tenant               │
│     │   └─> new MAIChatEntry(chat, message)                      │
│     └─> Success - chat exists for subsequent messages            │
│                                                                  │
│  3. Subsequent Messages                                          │
│     └─> sendMessage()                                            │
│         └─> chat != null → create entry directly                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Benefits

1. **Complete Tenant Isolation**
   - ✅ No cross-tenant data leaks
   - ✅ Each tenant has separate chats
   - ✅ System users get per-tenant chats
   - ✅ All queries filter by AD_Client_ID

2. **Lazy Creation Pattern**
   - ✅ No NPE on widget initialization
   - ✅ No unnecessary chat creation
   - ✅ Better error handling
   - ✅ Follows iDempiere best practices

3. **Security & Compliance**
   - ✅ Respects ADR-036 ownership model
   - ✅ ConfidentialType set to Private by default
   - ✅ Creator is always owner
   - ✅ Full audit trail (Created/CreatedBy)

4. **User Experience**
   - ✅ No errors when switching tenants
   - ✅ Clear error messages
   - ✅ Graceful degradation
   - ✅ Consistent behavior across tenants

## Testing Scenarios

### Scenario 1: System User Switching Tenants

1. **Login as System User** (Client_ID=0) into Tenant A (Client_ID=1000026)
2. **Open AI Chat Widget** - should show empty state (no error)
3. **Send first message** - chat created in Tenant A
4. **Logout and login** into Tenant B (Client_ID=1000083)
5. **Open AI Chat Widget** - should show empty state (new chat, not Tenant A's)
6. **Send first message** - chat created in Tenant B

**Expected Result:** ✅ No cross-tenant errors, separate chats per tenant

### Scenario 2: Non-System User Single Tenant

1. **Login as Tenant User** (Client_ID=1000026)
2. **Open AI Chat Widget** - shows existing chat or empty state
3. **Send message** - creates/uses chat in user's tenant
4. **Cannot access** other tenants' chats

**Expected Result:** ✅ Only sees own tenant's chats

### Scenario 3: Context Chats (Record-Linked)

1. **Open Sales Order** (C_Order_ID=12345) in Tenant A
2. **Open AI Chat** - context chat for this record in Tenant A
3. **Same user** opens same order in Tenant B
4. **Gets different chat** - separate context chat for Tenant B

**Expected Result:** ✅ Context chats are tenant-isolated

## Migration Notes

- **Existing chats**: No migration needed - existing chats will continue to work
- **Cross-tenant chats**: If any exist (shouldn't per design), they will be isolated per tenant going forward
- **Database**: No schema changes required
- **Backward compatibility**: Fully compatible with existing chat data

## Related ADRs

- [ADR-036](docs/adr/036-chat-ownership-and-sharing-model.md) - Chat Ownership and Sharing Model
- [ADR-007](docs/adr/007-database-security-model.md) - Database Security Model

## Version

**Fixed in:** v0.6.1 (2026-01-27)
**Issue:** CLD-1606
**Author:** Claude Code (Anthropic)
