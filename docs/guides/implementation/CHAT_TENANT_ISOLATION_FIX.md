# Chat Tenant Isolation Fix (CLD-1606)

## Issue Summary

Users experienced cross-tenant constraint violations when logging into different tenants:

```
org.adempiere.exceptions.AdempiereException: Could not save record - Require unique data:
ERROR: duplicate key value violates unique constraint "cm_chat_record"
Detail: Key (ad_table_id, record_id)=(114, 1134855) already exists.
```

### Root Cause

**Database constraint was not tenant-aware:**
```sql
-- OLD (broken for multi-tenancy)
CONSTRAINT cm_chat_record UNIQUE (ad_table_id, record_id)
```

This enforced **global uniqueness across all tenants**, preventing:
- System users from having separate chats when working in different tenants
- Different tenants from having independent chats for the same records

**Secondary issues:**
1. **Eager chat creation**: Chat was created during widget initialization, triggering constraint violations immediately
2. **No null safety**: Widget didn't handle missing chats gracefully

## Solution

### 1. Fix Database Constraint (Migration Script)

**Update constraint to be tenant-aware:**
```sql
-- NEW (correct for multi-tenancy)
CONSTRAINT cm_chat_record UNIQUE (ad_client_id, ad_table_id, record_id)
```

**Migration Script:** `migration/i10/postgresql/202601271600_CLD-1606_FixChatConstraint.sql`

This allows:
- ✅ System user in Tenant A: `(client=1000026, table=114, record=1134855)`
- ✅ Same system user in Tenant B: `(client=1000083, table=114, record=1134855)`
- ✅ Complete tenant isolation at database level

### 2. Simplify Code (Remove Workarounds)

**MAIChat.java - Simplified global chat logic:**

```java
public static MAIChat getOrCreateGlobalChat(Properties ctx, String trxName, boolean dontCreate) {
    int userId = Env.getAD_User_ID(ctx);
    int clientId = Env.getAD_Client_ID(ctx);

    // Simple query - constraint now includes AD_Client_ID
    String whereClause = "AD_Table_ID=? AND Record_ID=? AND AD_Client_ID=?";
    MChat chat = new Query(ctx, MChat.Table_Name, whereClause, null)
        .setParameters(AI_GLOBAL_TABLE_ID, userId, clientId)
        .first();

    if (chat != null) {
        return new MAIChat(ctx, chat.get_ID(), trxName);
    }

    if (dontCreate) {
        return null; // Lazy creation
    }

    // Create new - constraint allows same table+record in different tenants
    MAIChat newChat = new MAIChat(ctx, AI_GLOBAL_TABLE_ID, userId,
        "AI Assistant - " + Env.getContext(ctx, "#AD_User_Name"), trxName);
    newChat.setAD_Client_ID(clientId);
    newChat.setConfidentialType(CONFIDENTIALTYPE_PrivateInformation);
    newChat.saveEx();
    return newChat;
}
```

**AIChatWidget.java - Lazy chat creation:**

```java
private void loadExistingChat() {
    // Try to load existing chat WITHOUT creating it (lazy creation)
    chat = MAIChat.getOrCreateGlobalChat(sessionCtx, null, true);

    if (chat == null) {
        // No chat exists yet - will create on first message
        log.fine("No existing chat found - will create on first message");
        currentAccess = ChatAccess.OWNER;
        return;
    }

    // Load existing chat...
}

public void sendMessage() {
    // LAZY CHAT CREATION: Create chat on first message if it doesn't exist
    if (chat == null) {
        chat = MAIChat.getOrCreateGlobalChat(sessionCtx, null, false);
        if (chat == null) {
            // Error handling...
            return;
        }
        currentAccess = ChatAccess.OWNER;
    }

    // Now safe to create entry
    MAIChatEntry userEntry = new MAIChatEntry(chat, message);
    // ...
}
```

## How This Compares to iDempiere Core

iDempiere's **Recent Items** feature solved the same problem differently:

### AD_RecentItem Approach

```sql
CREATE TABLE AD_RecentItem (
    AD_Client_ID NUMERIC(10) NOT NULL,
    AD_User_ID NUMERIC(10) DEFAULT NULL,  -- Has user column!
    AD_Table_ID NUMERIC(10) NOT NULL,
    Record_ID NUMERIC(10) NOT NULL,
    -- No UNIQUE constraint on (table, record)!
    CONSTRAINT AD_RecentItem_Key PRIMARY KEY (AD_RecentItem_ID)
)
```

**Query pattern:**
```java
String sql = "SELECT * FROM AD_RecentItem " +
             "WHERE AD_Table_ID=? AND Record_ID=? " +
             "AND NVL(AD_User_ID,0)=? " +  // Filter by USER
             "AND AD_Client_ID=?";          // Filter by CLIENT
```

**Key difference:** AD_RecentItem has an explicit `AD_User_ID` column and no uniqueness constraint, allowing multiple users to have the same record as "recent".

### CM_Chat Challenge

CM_Chat couldn't use the same approach because:
1. ❌ No `AD_User_ID` column (only `CreatedBy`)
2. ❌ Had `UNIQUE(ad_table_id, record_id)` constraint
3. ❌ `Record_ID` must be valid FK to table (can't encode composite keys)

**Solution:** Fix the constraint to include `AD_Client_ID`, which is the semantically correct solution for multi-tenancy.

## Architecture Benefits

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
│  Database constraint ensures:                                    │
│    UNIQUE(AD_Client_ID, AD_Table_ID, Record_ID)                 │
│    = Each tenant has isolated chats                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Benefits

1. **Proper Tenant Isolation**
   - ✅ Database-level enforcement
   - ✅ No cross-tenant conflicts
   - ✅ System users get per-tenant chats
   - ✅ Semantically correct

2. **Simplified Code**
   - ✅ No workarounds or encoding tricks
   - ✅ Clean query logic
   - ✅ Maintainable codebase
   - ✅ Follows iDempiere patterns

3. **Lazy Creation Pattern**
   - ✅ No NPE on widget initialization
   - ✅ No unnecessary chat creation
   - ✅ Better error handling
   - ✅ Improved user experience

4. **Standards Compliance**
   - ✅ Respects ADR-036 ownership model
   - ✅ Follows iDempiere multi-tenancy patterns
   - ✅ ConfidentialType set to Private by default
   - ✅ Full audit trail (Created/CreatedBy)

## Testing Scenarios

### Scenario 1: System User Switching Tenants

1. **Login as System User** (Client_ID=0) into Tenant K (Client_ID=1000026)
2. **Send first message** - chat created: `(client=1000026, table=114, record=1134855)`
3. **Logout and login** into Tenant T (Client_ID=1000083)
4. **Send first message** - chat created: `(client=1000083, table=114, record=1134855)` ✅

**Expected Result:** ✅ No constraint violation, separate chats per tenant

### Scenario 2: Non-System User Single Tenant

1. **Login as Tenant User** (Client_ID=1000026)
2. **Send message** - creates/uses chat in user's tenant
3. **Cannot access** other tenants' chats

**Expected Result:** ✅ Only sees own tenant's chats

### Scenario 3: Context Chats (Record-Linked)

1. **Open Sales Order** (C_Order_ID=12345) in Tenant A
2. **Open AI Chat** - context chat: `(client=A, table=259, record=12345)`
3. **Same order** in Tenant B → different chat: `(client=B, table=259, record=12345)`

**Expected Result:** ✅ Context chats are tenant-isolated

## Migration Notes

### For Plugin Maintainers

1. **Migration script** `CLD-1606` must be applied before using the plugin
2. **Existing chats** will continue to work (no data migration needed)
3. **Constraint change** is backward compatible

### For iDempiere Community

This fix should be contributed to iDempiere core:

1. **JIRA Issue**: "CM_Chat constraint not tenant-aware"
2. **Pull Request**: Include PostgreSQL and Oracle migration scripts
3. **Impact**: Low - improves multi-tenancy, no breaking changes

## Files Changed

1. **Migration Script:**
   - `migration/i10/postgresql/202601271600_CLD-1606_FixChatConstraint.sql`
   - `migration/i10/oracle/202601271600_CLD-1606_FixChatConstraint.sql`

2. **Model Layer:**
   - `MAIChat.java` - Simplified global chat logic, added AD_Client_ID filtering

3. **UI Layer:**
   - `AIChatWidget.java` - Implemented lazy chat creation pattern

## Related ADRs

- [ADR-036](adr/036-chat-ownership-and-sharing-model.md) - Chat Ownership and Sharing Model
- [ADR-007](adr/007-database-security-model.md) - Database Security Model

## Version

**Fixed in:** v0.6.1 (2026-01-27)
**Issue:** CLD-1606
**Author:** Norbert Bede / Claude Code (Anthropic)

---

*This fix addresses the root cause of cross-tenant chat conflicts by making the database constraint tenant-aware, following the same multi-tenancy patterns used throughout iDempiere.*
