# ADR-036: Chat Ownership and Sharing Model

## Status

**Accepted**

## Date

2025-12-04

## Deciders

Cloudempiere AI Team

## Approval

Approved by Norbert Bede on 2025-12-04

## Context and Problem Statement

The AI chat functionality needs well-defined ownership and sharing rules. iDempiere's native `CM_Chat` table provides generic chat capabilities, but lacks explicit ownership semantics for AI conversations. We need to establish clear patterns for:

1. **Private Record Chats** - Conversations bound to a specific business record
2. **Private Global Chats** - User's personal AI assistant without record context
3. **Shared Chats** - Collaborative conversations accessible to multiple users

Currently, `MAIChat` extends `MChat` and uses the `AD_Table_ID + Record_ID` pattern, but ownership and sharing rules are implicit rather than explicit.

### Current State Analysis

**iDempiere CM_Chat Table Structure:**

| Column | Type | Purpose |
|--------|------|---------|
| `CM_Chat_ID` | Numeric(10) | Primary key |
| `AD_Client_ID` | Numeric(10) | Tenant isolation |
| `AD_Org_ID` | Numeric(10) | Organization |
| `AD_Table_ID` | Numeric(10) | Links to source table |
| `Record_ID` | Numeric(10) | Links to source record |
| `Record_UU` | VARCHAR(36) | UUID for tables without ID |
| `Description` | VARCHAR(255) | Chat title |
| `CreatedBy` | Numeric(10) | Creator user |
| `ConfidentialType` | VARCHAR(60) | Privacy level (A, C, I, P) |

**Current Implementation (MAIChat.java):**
- Global chats: `AD_Table_ID = AD_User.Table_ID`, `Record_ID = user's AD_User_ID`
- Context chats: `AD_Table_ID = target table`, `Record_ID = target record`
- Tenant isolation via `AD_Client_ID` filtering

**Gaps Identified:**
1. No explicit owner field (relies on `CreatedBy`)
2. No sharing mechanism between users
3. No visibility rules beyond `ConfidentialType`
4. No separation between "chat about record" vs "chat owned by user"

## Decision Drivers

- **Security**: Chats may contain sensitive business discussions
- **Multi-tenancy**: Must respect iDempiere's client/org model
- **Collaboration**: Teams need to share AI conversations
- **Auditability**: Track who created, owns, and accesses chats
- **Backward Compatibility**: Existing chats must continue working
- **iDempiere Standards**: Follow established patterns (e.g., `ConfidentialType`)

## Use Cases

### Case 1: Private Chat Over Record (Context Chat)

**Scenario**: User opens a Sales Order and starts an AI conversation about it.

**Requirements:**
- Chat is linked to specific record (C_Order_ID = 12345)
- Only users with access to that record should see the chat
- Multiple users can have separate private chats about same record
- Chat persists with the record lifecycle

**Current Behavior:**
```java
MAIChat chat = MAIChat.getOrCreateContextChat(
    ctx,
    C_Order.Table_ID,  // 259
    12345,             // Order ID
    "Sales Order SO-001",
    null
);
```

**Problem**: Two users chatting about the same order share the same chat (unintended).

### Case 2: Private Global Chat (Personal Assistant)

**Scenario**: User wants a general AI assistant not tied to any record.

**Requirements:**
- One chat per user per tenant
- Completely private to the user
- Survives session restarts
- System users get separate chats per tenant

**Current Behavior:**
```java
MAIChat chat = MAIChat.getOrCreateGlobalChat(ctx, null);
// AD_Table_ID = 102 (AD_User)
// Record_ID = AD_User_ID
// AD_Client_ID = current tenant
```

**Status**: Works correctly. Tenant isolation handled.

### Case 3: Shared Chats (Team Collaboration)

**Scenario**: Support team wants to share AI analysis of a customer issue.

**Requirements:**
- Explicit sharing with specific users or roles
- Read-only vs read-write access
- Audit trail of who shared and accessed
- Revocable sharing

**Current Behavior**: Not supported.

## Considered Options

### Option 1: Extend CM_Chat with AI-Specific Columns

Add columns to `CM_Chat` table:
- `AI_Owner_ID` (AD_User_ID) - Explicit owner
- `AI_IsShared` (Y/N) - Sharing flag
- `AI_ShareType` (P=Private, T=Team, O=Organization, A=All)

**Pros:**
- Simple extension
- Uses existing table

**Cons:**
- Modifies iDempiere core table
- Upgrade risk
- Limited sharing granularity

### Option 2: New AIG_ChatOwnership Table (Chosen)

Create separate table for ownership/sharing rules:

```sql
CREATE TABLE AIG_ChatOwnership (
    AIG_ChatOwnership_ID    NUMERIC(10) PRIMARY KEY,
    CM_Chat_ID              NUMERIC(10) NOT NULL,  -- FK to CM_Chat
    AD_User_ID              NUMERIC(10),           -- User with access
    AD_Role_ID              NUMERIC(10),           -- Role with access
    OwnershipType           VARCHAR(1) NOT NULL,   -- O=Owner, R=Read, W=Write
    SharedBy_User_ID        NUMERIC(10),           -- Who granted access
    ValidFrom               TIMESTAMP,             -- Optional time-bound
    ValidTo                 TIMESTAMP,
    -- Standard columns
    AD_Client_ID            NUMERIC(10) NOT NULL,
    AD_Org_ID               NUMERIC(10) NOT NULL,
    IsActive                CHAR(1) DEFAULT 'Y',
    Created                 TIMESTAMP NOT NULL,
    CreatedBy               NUMERIC(10) NOT NULL,
    Updated                 TIMESTAMP NOT NULL,
    UpdatedBy               NUMERIC(10) NOT NULL
);
```

**Pros:**
- No changes to core CM_Chat
- Flexible sharing model
- Role-based access support
- Time-bound sharing
- Full audit trail

**Cons:**
- Additional table
- Join required for access checks

### Option 3: Use ConfidentialType + Record Rules

Leverage iDempiere's existing `ConfidentialType` with enhanced record access rules.

**ConfidentialType Values:**
- `A` = Public/Anyone
- `C` = Company/Client
- `I` = Internal (Organization)
- `P` = Private (Only Creator)

**Pros:**
- Uses existing iDempiere pattern
- No schema changes

**Cons:**
- Cannot share with specific users
- Binary private/public, no granularity
- No explicit owner vs creator distinction

### Option 4: Hybrid Approach

Combine `ConfidentialType` for baseline + `AIG_ChatOwnership` for explicit grants.

**Flow:**
1. Check `ConfidentialType` first
2. If restricted, check `AIG_ChatOwnership`
3. Apply most permissive access

## Decision Outcome

**Chosen option:** "Option 4: Hybrid Approach" with new `AIG_ChatOwnership` table and enhanced access logic.

### Ownership Model

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Chat Ownership Model                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                      CM_Chat (Existing)                      │   │
│  │  - CM_Chat_ID (PK)                                           │   │
│  │  - AD_Table_ID + Record_ID (context link)                    │   │
│  │  - ConfidentialType (P=Private, I=Internal, C=Client, A=All) │   │
│  │  - CreatedBy (original creator, implicit owner)              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                      │
│                              │ 1:N                                  │
│                              ▼                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                 AIG_ChatOwnership (New)                      │   │
│  │  - CM_Chat_ID (FK)                                           │   │
│  │  - AD_User_ID (granted user) OR AD_Role_ID (granted role)    │   │
│  │  - OwnershipType: O=Owner, R=Read, W=Write                   │   │
│  │  - SharedBy_User_ID (who shared)                             │   │
│  │  - ValidFrom/ValidTo (time-bound access)                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Access Resolution Algorithm

```java
public enum ChatAccess {
    NONE,       // No access
    READ,       // View only
    WRITE,      // View and add messages
    OWNER       // Full control (delete, share, transfer)
}

public ChatAccess resolveAccess(Properties ctx, MChat chat) {
    int userId = Env.getAD_User_ID(ctx);
    int clientId = Env.getAD_Client_ID(ctx);
    int orgId = Env.getAD_Org_ID(ctx);

    // 1. Creator is always owner
    if (chat.getCreatedBy() == userId) {
        return ChatAccess.OWNER;
    }

    // 2. Check explicit ownership grants
    ChatAccess explicit = checkExplicitOwnership(chat.getCM_Chat_ID(), userId, ctx);
    if (explicit != ChatAccess.NONE) {
        return explicit;
    }

    // 3. Check role-based grants
    ChatAccess roleAccess = checkRoleOwnership(chat.getCM_Chat_ID(), ctx);
    if (roleAccess != ChatAccess.NONE) {
        return roleAccess;
    }

    // 4. Apply ConfidentialType baseline
    String confidential = chat.getConfidentialType();

    if ("A".equals(confidential)) {
        return ChatAccess.READ;  // Anyone in client
    }
    else if ("C".equals(confidential) && chat.getAD_Client_ID() == clientId) {
        return ChatAccess.READ;  // Same client
    }
    else if ("I".equals(confidential) && chat.getAD_Org_ID() == orgId) {
        return ChatAccess.READ;  // Same organization
    }
    else if ("P".equals(confidential)) {
        return ChatAccess.NONE;  // Private - only creator (handled above)
    }

    // 5. For context chats, check record access
    if (chat.getAD_Table_ID() > 0 && chat.getRecord_ID() > 0) {
        if (hasRecordAccess(ctx, chat.getAD_Table_ID(), chat.getRecord_ID())) {
            return ChatAccess.READ;  // Can view record = can view chat
        }
    }

    return ChatAccess.NONE;
}
```

### Chat Types Matrix

| Type | AD_Table_ID | Record_ID | ConfidentialType | Ownership | Sharing |
|------|-------------|-----------|------------------|-----------|---------|
| **Private Global** | AD_User (102) | User's ID | P (Private) | Creator | None |
| **Private Record** | Target Table | Target Record | P (Private) | Creator | None |
| **Record-Linked** | Target Table | Target Record | I (Internal) | Creator | Via record access |
| **Team Shared** | AD_User (102) | User's ID | P (Private) | Creator | Via AIG_ChatOwnership |
| **Org-Wide** | - | - | I (Internal) | Creator | Org members |
| **Client-Wide** | - | - | C (Client) | Creator | All client users |

### Implementation Phases

#### Phase 1: Foundation (v0.5.0)

1. **Create AIG_ChatOwnership table**
   ```sql
   -- PostgreSQL
   CREATE TABLE AIG_ChatOwnership (
       AIG_ChatOwnership_ID    NUMERIC(10,0) NOT NULL,
       AIG_ChatOwnership_UU    VARCHAR(36) DEFAULT NULL,
       CM_Chat_ID              NUMERIC(10,0) NOT NULL,
       AD_User_ID              NUMERIC(10,0) DEFAULT NULL,
       AD_Role_ID              NUMERIC(10,0) DEFAULT NULL,
       OwnershipType           CHAR(1) NOT NULL DEFAULT 'R',
       SharedBy_User_ID        NUMERIC(10,0) DEFAULT NULL,
       ValidFrom               TIMESTAMP DEFAULT NULL,
       ValidTo                 TIMESTAMP DEFAULT NULL,
       AD_Client_ID            NUMERIC(10,0) NOT NULL,
       AD_Org_ID               NUMERIC(10,0) NOT NULL,
       IsActive                CHAR(1) NOT NULL DEFAULT 'Y',
       Created                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CreatedBy               NUMERIC(10,0) NOT NULL,
       Updated                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       UpdatedBy               NUMERIC(10,0) NOT NULL,
       CONSTRAINT AIG_ChatOwnership_Key PRIMARY KEY (AIG_ChatOwnership_ID),
       CONSTRAINT AIG_ChatOwnership_Chat FOREIGN KEY (CM_Chat_ID)
           REFERENCES CM_Chat(CM_Chat_ID) ON DELETE CASCADE,
       CONSTRAINT AIG_ChatOwnership_User FOREIGN KEY (AD_User_ID)
           REFERENCES AD_User(AD_User_ID),
       CONSTRAINT AIG_ChatOwnership_Role FOREIGN KEY (AD_Role_ID)
           REFERENCES AD_Role(AD_Role_ID),
       CONSTRAINT AIG_ChatOwnership_Type CHECK (OwnershipType IN ('O','R','W'))
   );

   CREATE INDEX AIG_ChatOwnership_Chat_IDX ON AIG_ChatOwnership(CM_Chat_ID);
   CREATE INDEX AIG_ChatOwnership_User_IDX ON AIG_ChatOwnership(AD_User_ID);
   CREATE INDEX AIG_ChatOwnership_Role_IDX ON AIG_ChatOwnership(AD_Role_ID);
   ```

2. **Create MAIChatOwnership model class**
3. **Implement ChatAccessService with resolution algorithm**
4. **Add access checks to MAIChat**

#### Phase 2: UI Integration (v0.6.0)

1. **Share button in AIChatWidget**
2. **User/role picker dialog**
3. **Access indicator (read-only badge)**
4. **"Shared with me" chat list**

#### Phase 3: Advanced Features (v0.7.0)

1. **Time-bound sharing** (ValidFrom/ValidTo)
2. **Transfer ownership**
3. **Revoke sharing**
4. **Sharing notifications**
5. **Audit log viewer**

### Migration for Existing Chats

```sql
-- Auto-create ownership record for existing chats (creator = owner)
INSERT INTO AIG_ChatOwnership (
    AIG_ChatOwnership_ID,
    AIG_ChatOwnership_UU,
    CM_Chat_ID,
    AD_User_ID,
    OwnershipType,
    AD_Client_ID,
    AD_Org_ID,
    CreatedBy,
    UpdatedBy
)
SELECT
    nextval('aig_chatownership_seq'),
    uuid_generate_v4(),
    c.CM_Chat_ID,
    c.CreatedBy,
    'O',  -- Owner
    c.AD_Client_ID,
    c.AD_Org_ID,
    c.CreatedBy,
    c.CreatedBy
FROM CM_Chat c
WHERE c.AD_Table_ID = 102  -- AI Global chats (AD_User table)
  AND NOT EXISTS (
      SELECT 1 FROM AIG_ChatOwnership o
      WHERE o.CM_Chat_ID = c.CM_Chat_ID
  );
```

## API Design

### ChatAccessService

```java
public interface IChatAccessService {

    /**
     * Resolve user's access level to a chat
     */
    ChatAccess getAccess(Properties ctx, int CM_Chat_ID, String trxName);

    /**
     * Check if user can perform action
     */
    boolean canRead(Properties ctx, int CM_Chat_ID, String trxName);
    boolean canWrite(Properties ctx, int CM_Chat_ID, String trxName);
    boolean canShare(Properties ctx, int CM_Chat_ID, String trxName);
    boolean canDelete(Properties ctx, int CM_Chat_ID, String trxName);

    /**
     * Share chat with user
     */
    void shareWithUser(Properties ctx, int CM_Chat_ID, int AD_User_ID,
                       ChatAccess access, String trxName);

    /**
     * Share chat with role
     */
    void shareWithRole(Properties ctx, int CM_Chat_ID, int AD_Role_ID,
                       ChatAccess access, String trxName);

    /**
     * Revoke access
     */
    void revokeAccess(Properties ctx, int AIG_ChatOwnership_ID, String trxName);

    /**
     * Transfer ownership
     */
    void transferOwnership(Properties ctx, int CM_Chat_ID,
                          int newOwner_User_ID, String trxName);

    /**
     * Get all chats shared with current user
     */
    List<MChat> getSharedChats(Properties ctx, String trxName);

    /**
     * Get sharing info for a chat
     */
    List<MAIChatOwnership> getSharingInfo(Properties ctx, int CM_Chat_ID, String trxName);
}
```

### Enhanced MAIChat Methods

```java
public class MAIChat extends MChat {

    /**
     * Get or create private context chat for CURRENT USER
     * Each user gets their own chat even for the same record
     */
    public static MAIChat getOrCreatePrivateContextChat(
            Properties ctx, int AD_Table_ID, int Record_ID,
            String description, String trxName) {

        int userId = Env.getAD_User_ID(ctx);

        // Query includes CreatedBy to get user's own chat
        String whereClause = "AD_Table_ID=? AND Record_ID=? AND CreatedBy=? AND ConfidentialType='P'";
        MChat chat = new Query(ctx, MChat.Table_Name, whereClause, trxName)
            .setParameters(AD_Table_ID, Record_ID, userId)
            .first();

        if (chat != null) {
            return new MAIChat(ctx, chat.getCM_Chat_ID(), trxName);
        }

        // Create new private chat
        MAIChat newChat = new MAIChat(ctx, AD_Table_ID, Record_ID, description, trxName);
        newChat.setConfidentialType("P");  // Private
        newChat.saveEx();
        return newChat;
    }

    /**
     * Get or create shared context chat (visible to all with record access)
     */
    public static MAIChat getOrCreateSharedContextChat(
            Properties ctx, int AD_Table_ID, int Record_ID,
            String description, String trxName) {

        // Shared context chat - one per record, internal visibility
        String whereClause = "AD_Table_ID=? AND Record_ID=? AND ConfidentialType='I'";
        MChat chat = new Query(ctx, MChat.Table_Name, whereClause, trxName)
            .setParameters(AD_Table_ID, Record_ID)
            .first();

        if (chat != null) {
            return new MAIChat(ctx, chat.getCM_Chat_ID(), trxName);
        }

        MAIChat newChat = new MAIChat(ctx, AD_Table_ID, Record_ID, description, trxName);
        newChat.setConfidentialType("I");  // Internal - org can see
        newChat.saveEx();
        return newChat;
    }

    /**
     * Share this chat with another user
     */
    public void shareWith(int AD_User_ID, ChatAccess access, String trxName) {
        IChatAccessService service = ServiceLocator.get(IChatAccessService.class);
        service.shareWithUser(getCtx(), getCM_Chat_ID(), AD_User_ID, access, trxName);
    }
}
```

## Security Considerations

### Multi-Tenancy

- All queries MUST filter by `AD_Client_ID`
- System users (Client_ID=0) cannot share cross-tenant
- Role-based sharing limited to roles within same client

### Record Access Integration

- Context chats inherit record access rules
- If user loses access to record, loses chat access
- Delete record = cascade delete chat (configurable)

### Audit Trail

- `AIG_ChatOwnership.SharedBy_User_ID` tracks who shared
- Standard `Created/CreatedBy/Updated/UpdatedBy` columns
- Consider integration with AD_ChangeLog for detailed history

### Sensitive Data

- AI responses may contain sensitive analysis
- ConfidentialType=P should be default for all AI chats
- Warn users when sharing chats containing sensitive queries

## Related ADRs

- [ADR-007](007-database-security-model.md) - Database Security Model
- [ADR-029](029-multi-tenant-ai-access.md) - Multi-Tenant AI Access
- [ADR-031](031-chat-panel-langchain4j-chatmodel-integration.md) - Chat Panel Integration

## References

- [iDempiere CM_Chat Table](https://wiki.idempiere.org/en/Tables_by_Module)
- [iDempiere ConfidentialType](https://wiki.idempiere.org/en/Database_structure)
- [iDempiere Record Access](https://wiki.idempiere.org/en/Record_Access)

---

*ADR-036 | Version 1.0 | 2025-12-04*
