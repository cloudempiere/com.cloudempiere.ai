# ADR-029: Multi-Tenant AI Access for Service Providers

**Status:** Proposed
**Date:** 2025-12-03
**Deciders:** CloudEmpiere AI Team
**Related:** ADR-007 (Database Security Model)

---

## Context

### Problem Statement

CloudEmpiere operates as a **service provider** managing multiple iDempiere tenants. The AI system needs to support two distinct access patterns:

| Access Pattern | User Type | Data Scope |
|----------------|-----------|------------|
| **Tenant Mode** | End users | Own tenant data only (AD_Client_ID = N) |
| **Service Provider Mode** | CloudEmpiere team | Cross-tenant + Application Dictionary |

### iDempiere Multi-Tenant Architecture

```
AD_Client_ID = 0 (System)
├── Application Dictionary (AD_Table, AD_Column, AD_Window, etc.)
├── System configuration
├── Reference data (AD_Ref_List, AD_Message)
└── Base element descriptions

AD_Client_ID = 1000000 (GardenWorld Demo)
├── Business partners, orders, invoices
└── Tenant-specific customizations

AD_Client_ID = 1000014 (CloudEmpiere Documentation Tenant)
├── Application Dictionary documentation (EditorJS format)
├── Knowledge base articles
├── How-to guides and tutorials
├── Custom field documentation
└── Business process documentation

AD_Client_ID = 1000015 (Production Tenant)
├── Business partners, orders, invoices
└── Tenant-specific customizations
```

### Documentation Storage Pattern

CloudEmpiere stores **Application Dictionary documentation** in a dedicated tenant (e.g., AD_Client_ID = 1000014) using **EditorJS format**:

| Table | Purpose | Format |
|-------|---------|--------|
| `K_Entry` | Knowledge Base entries | EditorJS JSON (TextMsg field) |
| `K_Category` | KB categories | Text |
| `K_Topic` | KB topics | Text |
| `K_Type` | Entry types | Reference |

**Note:** K_Entry.TextMsg contains EditorJS JSON format. Uses existing **EditorJS parser** (already implemented):
- `EditorJsParser.java` - Base parser
- `EditorJsParserEnhanced.java` - Enhanced with syntax support
- `KnowledgeBaseContextProvider.java` - Context extraction for AI

See [ADR-016: Knowledge Base Agent](016-knowledge-base-agent.md) for full architecture.

**EditorJS Format Example:**
```json
{
  "time": 1701619200000,
  "blocks": [
    {
      "type": "header",
      "data": { "text": "C_Order Table", "level": 2 }
    },
    {
      "type": "paragraph",
      "data": { "text": "The C_Order table stores sales and purchase orders..." }
    },
    {
      "type": "table",
      "data": {
        "content": [
          ["Column", "Description"],
          ["DocumentNo", "Unique order number"],
          ["DateOrdered", "Order date"]
        ]
      }
    }
  ]
}
```

**AI Access Pattern:**
- Service Provider (Tier 3) queries documentation tenant for context
- EditorJS JSON parsed to extract text for RAG embedding
- Documentation enriches AI responses about Application Dictionary

### Service Provider Requirements

The CloudEmpiere team needs AI capabilities for:

1. **Application Dictionary Knowledge**
   - Query AD_* tables to understand data model
   - Generate accurate SQL based on schema metadata
   - Provide context about windows, tabs, fields

2. **Cross-Tenant Support**
   - Troubleshoot issues in specific tenant (e.g., AD_Client_ID = 1000014)
   - Analyze data patterns across tenants (aggregate, anonymized)
   - Compare tenant configurations

3. **Documentation Context**
   - Access AD_Element descriptions (field help text)
   - Query AD_Message for translations
   - Reference AD_Window/AD_Tab/AD_Field for UI context

### Current Limitation (ADR-007)

The existing security model enforces single-tenant access:
```java
// MRole.addAccessSQL() restricts to user's AD_Client_ID
WHERE AD_Client_ID = 1000014  -- User's tenant only
```

This prevents:
- Querying Application Dictionary (AD_Client_ID = 0)
- Cross-tenant analysis for support purposes
- System-level metadata access

---

## Decision

Implement **Tiered AI Access Model** with explicit scope selection:

### Access Tiers

| Tier | Name | Business Data Access | Knowledge Access | Use Case |
|------|------|---------------------|------------------|----------|
| **1** | End User | User's client only | 0 + 1000014 (read-only) | End users |
| **2** | Service Provider | Target client(s) | 0 + 1000014 (read-only) | CloudEmpiere team |

**Key Insight:** ALL users need read access to knowledge sources, but business data is restricted.

**Knowledge Tenants (Always Accessible for AI Context):**
- `AD_Client_ID = 0` - Application Dictionary (AD_Table, AD_Column, AD_Element, etc.)
- `AD_Client_ID = 1000014` - Knowledge Base (K_Entry with EditorJS content)

**Business Data (Tenant-Restricted):**
- User's own AD_Client_ID for end users
- Target AD_Client_ID for service providers

### Dual-Query Architecture

**Primary Use Case: Chart Analysis**

User sees a chart and asks: *"Explain this chart - what are we measuring and how are we doing?"*

```
┌─────────────────────────────────────────────────────────────────┐
│  User Context: Sales by Month Chart (AD_Client_ID = 1000015)    │
│  Question: "Explain this chart"                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  AI Query Planner                                                │
│                                                                  │
│  Needs: 1. What is being measured? (KNOWLEDGE)                   │
│         2. What are the actual figures? (BUSINESS DATA)          │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────────┐   ┌─────────────────────────────┐
│ KNOWLEDGE QUERY             │   │ BUSINESS DATA QUERY         │
│ AD_Client_ID IN (0, 1000014)│   │ AD_Client_ID = 1000015      │
│                             │   │                             │
│ -- What does TotalLines mean?                                 │
│ SELECT e.Name, e.Description│   │ -- Actual sales figures     │
│ FROM AD_Element e           │   │ SELECT                      │
│ WHERE e.ColumnName =        │   │   DATE_TRUNC('month',       │
│   'TotalLines'              │   │     DateOrdered) as Month,  │
│                             │   │   SUM(TotalLines) as Sales  │
│ -- How to interpret chart?  │   │ FROM C_Order                │
│ SELECT TextMsg              │   │ WHERE IsSOTrx = 'Y'         │
│ FROM K_Entry                │   │   AND DocStatus IN          │
│ WHERE K_Topic =             │   │     ('CO','CL')             │
│   'Chart_SalesByMonth'      │   │ GROUP BY 1                  │
│                             │   │ ORDER BY 1 DESC             │
└─────────────────────────────┘   └─────────────────────────────┘
              │                               │
              └───────────────┬───────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  AI Response (Combined Knowledge + Data)                         │
│                                                                  │
│  "This chart shows **Monthly Sales Revenue**.                    │
│                                                                  │
│  **What we're measuring:**                                       │
│  - TotalLines = Sum of all order line amounts (net of discounts) │
│  - Only completed sales orders (IsSOTrx=Y, DocStatus=CO/CL)      │
│  - Grouped by month                                              │
│                                                                  │
│  **How you're doing:**                                           │
│  - November 2025: €142,500 (+12% vs October)                     │
│  - October 2025: €127,200 (+5% vs September)                     │
│  - YTD Total: €1,245,000 (85% of annual target)                  │
│                                                                  │
│  **Insight:** Strong Q4 performance. On track to exceed target." │
└─────────────────────────────────────────────────────────────────┘
```

### Four Components of AI Context

For any chart/report analysis, AI needs access to **four data sources**:

| Component | Source | AD_Client_ID | Content |
|-----------|--------|--------------|---------|
| **1. Chart Help** | AD_Element, AD_Column | 0 (System) | Field definitions, data types |
| **2. Knowledge Base** | K_Entry | 1000014 (KB) | How-to guides, tutorials |
| **3. Best Practices** | K_Entry (type=BestPractice) | 1000014 (KB) | Benchmarks, KPIs, targets |
| **4. Chart Data** | C_Order, etc. | User's tenant | Actual business figures |

**Example Queries:**

```sql
-- 1. CHART HELP: What does TotalLines mean?
SELECT e.Name, e.Description, e.Help
FROM AD_Element e
WHERE e.ColumnName = 'TotalLines'
  AND e.AD_Client_ID = 0;
-- Result: "Total Lines = Document total of all lines"

-- 2. KNOWLEDGE BASE: How to interpret sales chart?
SELECT TextMsg  -- EditorJS JSON
FROM K_Entry e
JOIN K_Topic t ON e.K_Topic_ID = t.K_Topic_ID
WHERE t.Name = 'Chart_SalesByMonth'
  AND e.AD_Client_ID = 1000014;
-- Result: "This chart shows monthly sales trends..."

-- 3. BEST PRACTICES: What's a good monthly growth rate?
SELECT e.Name, e.TextMsg, e.Description
FROM K_Entry e
JOIN K_Type kt ON e.K_Type_ID = kt.K_Type_ID
WHERE kt.Name = 'BestPractice'
  AND e.Name LIKE '%Monthly Growth%'
  AND e.AD_Client_ID = 1000014;
-- Result: "Healthy MoM growth = 5-10%"

-- 4. CHART DATA: Actual sales figures
SELECT DATE_TRUNC('month', DateOrdered) as Month,
       SUM(TotalLines) as Sales
FROM C_Order
WHERE IsSOTrx = 'Y'
  AND DocStatus IN ('CO','CL')
  AND AD_Client_ID = 1000015  -- User's tenant
GROUP BY 1 ORDER BY 1 DESC;
-- Result: Nov=€142,500, Oct=€127,200...
```

**AI Response Structure:**

```
┌─────────────────────────────────────────────────────────────────┐
│  "**Monthly Sales Revenue Chart**                               │
│                                                                  │
│  📊 **What we're measuring:** (from Chart Help)                  │
│  - TotalLines = Sum of all order line amounts                    │
│  - Completed sales orders only (CO/CL status)                    │
│                                                                  │
│  📖 **How to read this chart:** (from Knowledge Base)            │
│  - Each bar represents one month's total sales                   │
│  - Compare bars to see growth trends                             │
│  - Look for seasonal patterns (Q4 typically higher)              │
│                                                                  │
│  📈 **Your performance:** (from Chart Data)                      │
│  - November 2025: €142,500                                       │
│  - October 2025: €127,200                                        │
│  - Month-over-month growth: +12%                                 │
│                                                                  │
│  ✅ **Best Practice Assessment:** (from Best Practices)          │
│  - Benchmark: 5-10% MoM growth is healthy                        │
│  - Your +12% exceeds the benchmark ✓                             │
│  - Recommendation: Maintain momentum into December"              │
└─────────────────────────────────────────────────────────────────┘
```

**Key Principle:** AI combines all four sources to answer not just "what" but also "why it matters" and "how you're doing".

### Why Direct SQL (Not REST/PO Layer)

**iDempiere PO (Persistent Object) has built-in cross-tenant validation:**

```java
// In PO.java - throws exception if AD_Client_ID doesn't match context
protected void checkCrossTenant() {
    if (getAD_Client_ID() != Env.getAD_Client_ID(getCtx())) {
        throw new AdempiereException("Cross-tenant access not allowed");
    }
}
```

**This means:**

| Approach | Cross-Tenant Access | Result |
|----------|---------------------|--------|
| **REST API (hengsin-mcp)** | Uses PO/Model layer | ❌ Exception thrown |
| **Direct SQL (CloudEmpiere)** | Bypasses PO checks | ✅ Works with proper security |

**Why our approach wins:**

```
REST API (PO Layer):
  User (AD_Client_ID=1000015) → GET /models/AD_Element → PO.load()
                                                           ↓
                                              checkCrossTenant()
                                                           ↓
                                              ❌ EXCEPTION: AD_Client_ID=0 ≠ 1000015

Direct SQL (SecureDatabaseQueryExecutor):
  User (AD_Client_ID=1000015) → SELECT * FROM AD_Element
                                           ↓
                                isKnowledgeTable("AD_Element") = true
                                           ↓
                                AD_Client_ID IN (0, 1000014) -- Allowed
                                           ↓
                                ✅ Returns system dictionary data
```

**CloudEmpiere Security Model:**
- Direct SQL allows controlled cross-tenant access
- `SecureDatabaseQueryExecutor` enforces our rules (not PO rules)
- Knowledge tables (AD_*, K_*) explicitly allowed
- Business tables restricted to user's tenant
- Full audit trail maintained

This is a **fundamental architectural advantage** over REST-based MCP implementations.

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  AI Request                                                      │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ User: CloudEmpiere Support                                  ││
│  │ Role: System Administrator                                  ││
│  │ Access Tier: SERVICE_PROVIDER                               ││
│  │ Target Client: 1000014 (or ALL for cross-tenant)            ││
│  │ Question: "Why are invoices failing validation?"            ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  SecureDatabaseQueryExecutor (Enhanced)                          │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  1. Validate Access Tier                                    ││
│  │     - Tier 3 requires IsServiceProvider=Y role              ││
│  │     - Verify user has AD_Client access in AD_Role_OrgAccess ││
│  └─────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  2. Build Client Filter                                     ││
│  │     Tier 1: AD_Client_ID = {user_client}                    ││
│  │     Tier 2: AD_Client_ID IN (0, {user_client})              ││
│  │     Tier 3: AD_Client_ID IN (0, {target_clients})           ││
│  └─────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  3. Apply Security (as before)                              ││
│  │     - MRole.addAccessSQL() with modified client list        ││
│  │     - Table/column access validation                        ││
│  │     - Sensitive data redaction                              ││
│  └─────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  4. Enhanced Audit                                          ││
│  │     - Log access tier used                                  ││
│  │     - Log target client(s)                                  ││
│  │     - Flag cross-tenant queries                             ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### Tier Definitions

#### Tier 1: End User Mode (Default)

**Who:** All end users
**Business Data:** Own AD_Client_ID only
**Knowledge Access:** AD_Client_ID IN (0, 1000014) - always allowed

```sql
-- BUSINESS QUERY: User from AD_Client_ID = 1000015 asks: "Show my orders"
SELECT * FROM C_Order
WHERE AD_Client_ID = 1000015  -- User's tenant only
  AND AD_Org_ID IN (...)       -- Org restriction

-- KNOWLEDGE QUERY: Same user asks: "What is the C_Order table?"
SELECT t.TableName, e.Description
FROM AD_Table t
JOIN AD_Element e ON t.AD_Element_ID = e.AD_Element_ID
WHERE t.TableName = 'C_Order'
  AND t.AD_Client_ID IN (0, 1000014)  -- Knowledge tenants (always allowed)

-- KNOWLEDGE QUERY: How do I create an order?
SELECT TextMsg FROM K_Entry e
JOIN K_Topic t ON e.K_Topic_ID = t.K_Topic_ID
WHERE e.AD_Client_ID = 1000014  -- KB tenant
  AND t.Name = 'C_Order_HowTo'
```

**Use cases:**
- Query own business data
- Get help from Knowledge Base
- Understand Application Dictionary metadata
- Access field descriptions and help text

#### Tier 2: Service Provider Mode

**Who:** CloudEmpiere team (System Administrator role with `IsServiceProvider=Y`)
**Business Data:** Any AD_Client_ID (explicit target required)
**Knowledge Access:** AD_Client_ID IN (0, 1000014) - always allowed

```sql
-- Support asks: "Show failed invoices in tenant 1000015"
SELECT * FROM C_Invoice
WHERE DocStatus = 'IN'
  AND AD_Client_ID = 1000015  -- Explicit target tenant
```

```sql
-- Support asks: "Compare order counts across all tenants"
SELECT AD_Client_ID, COUNT(*) as OrderCount
FROM C_Order
WHERE Created > '2025-01-01'
GROUP BY AD_Client_ID
-- Cross-tenant aggregation (service provider only)
```

**Security controls:**
- Requires role flag: `IsServiceProvider = Y`
- Must specify target client(s) in request
- Enhanced audit logging
- Cannot access without explicit authorization

### Table Classification

| Table Pattern | Classification | Access Rule |
|---------------|----------------|-------------|
| `AD_*` | Application Dictionary | Knowledge (0 + 1000014) |
| `K_*` | Knowledge Base | Knowledge (1000014) |
| `AD_Element`, `AD_Message` | Help Text | Knowledge (0) |
| `C_*`, `M_*`, `A_*`, etc. | Business Data | Tenant-restricted |

**Implementation:**
```java
public boolean isKnowledgeTable(String tableName) {
    return tableName.startsWith("AD_")
        || tableName.startsWith("K_")
        || KNOWLEDGE_TABLES.contains(tableName);
}

public String getClientFilter(String tableName, int userClientId, boolean isServiceProvider) {
    if (isKnowledgeTable(tableName)) {
        return "AD_Client_ID IN (0, " + KNOWLEDGE_CLIENT_ID + ")";  // 1000014
    } else if (isServiceProvider) {
        return "AD_Client_ID = " + targetClientId;  // Specified target
    } else {
        return "AD_Client_ID = " + userClientId;  // User's tenant
    }
}
```

### Data Model Changes

#### New: AIG_AccessTier Reference List

```sql
INSERT INTO AD_Ref_List (AD_Ref_List_ID, AD_Reference_ID, Value, Name)
VALUES
  (nextval, ref_id, 'T', 'Tenant Mode'),
  (nextval, ref_id, 'D', 'Tenant + Dictionary'),
  (nextval, ref_id, 'S', 'Service Provider');
```

#### Enhanced: AIG_Provider Table

```sql
ALTER TABLE AIG_Provider ADD COLUMN AIGDefaultAccessTier CHAR(1) DEFAULT 'T';
-- T = Tenant, D = Dictionary, S = Service Provider
```

#### Enhanced: AIG_QueryAudit Table

```sql
ALTER TABLE AIG_QueryAudit ADD COLUMN AIGAccessTier CHAR(1);
ALTER TABLE AIG_QueryAudit ADD COLUMN AIGTargetClients VARCHAR(255);
ALTER TABLE AIG_QueryAudit ADD COLUMN IsCrossTenant CHAR(1) DEFAULT 'N';
```

#### New: Role Flag for Service Provider

```sql
-- Option A: New column on AD_Role
ALTER TABLE AD_Role ADD COLUMN IsServiceProvider CHAR(1) DEFAULT 'N';

-- Option B: Use existing System Administrator flag
-- IsAccessAllOrgs = 'Y' implies service provider capability
```

### Request/Response Changes

#### SecureQueryRequest Enhancement

```java
public class SecureQueryRequest {
    // Existing fields...

    // New fields for multi-tenant access
    private AIGAccessTier accessTier = AIGAccessTier.TENANT;  // Default
    private List<Integer> targetClientIds;  // For SERVICE_PROVIDER tier

    public enum AIGAccessTier {
        TENANT,           // Tier 1: Own client only
        TENANT_DICTIONARY, // Tier 2: Own client + system
        SERVICE_PROVIDER   // Tier 3: Specified clients
    }
}
```

#### Context Provider Enhancement

```java
// AI context includes Application Dictionary metadata when Tier 2+
public class AIContextWithDictionary {
    private List<TableMetadata> relevantTables;
    private List<ColumnMetadata> relevantColumns;
    private Map<String, String> elementDescriptions;  // Help text
    private Map<String, String> referenceValues;      // Dropdown options
}
```

### Application Dictionary Context

When using Tier 2 or 3, AI automatically has access to:

| AD Table | Purpose | AI Use |
|----------|---------|--------|
| `AD_Table` | Table definitions | Know which tables exist |
| `AD_Column` | Column definitions | Know field types, constraints |
| `AD_Element` | Element descriptions | Field help text, labels |
| `AD_Reference` | Reference lists | Valid dropdown values |
| `AD_Window` | Window definitions | UI context |
| `AD_Tab` | Tab definitions | Related tables |
| `AD_Field` | Field definitions | Display logic, mandatory flags |
| `AD_Message` | System messages | Translations, error messages |
| `AD_Process` | Process definitions | Available actions |

**Example AI Enhancement:**

```
User (Tier 1): "Show orders with status DR"
AI: SELECT * FROM C_Order WHERE DocStatus='DR' AND AD_Client_ID=1000014

User (Tier 2): "Show orders with status DR"
AI: [Queries AD_Ref_List for DocStatus values first]
AI: "DR means 'Drafted'. Here are your draft orders..."
    [Provides context from Application Dictionary]

User (Tier 3): "Why are invoices failing in tenant 1000014?"
AI: [Queries AD_Column for C_Invoice validation rules]
    [Queries C_Invoice in tenant 1000014 for errors]
    [Cross-references with AD_Message for error descriptions]
AI: "Found 5 invoices failing validation. Common issue:
     Missing C_BPartner_Location_ID (mandatory per AD_Column definition)..."
```

---

## Security Considerations

### Authorization Matrix

| Access Tier | Role Requirement | Client Access | Audit Level |
|-------------|------------------|---------------|-------------|
| Tier 1 | Any role | Own client | Standard |
| Tier 2 | Any role | Own + System (0) | Standard |
| Tier 3 | IsServiceProvider=Y | Target client(s) | Enhanced |

### Service Provider Authorization

```java
public boolean canUseServiceProviderMode(MRole role, int targetClientId) {
    // 1. Role must have service provider flag
    if (!role.isServiceProvider()) {
        return false;
    }

    // 2. User must have access to target client via AD_Role_OrgAccess
    //    or be System Administrator
    if (!role.isAccessAllOrgs() && !hasClientAccess(role, targetClientId)) {
        return false;
    }

    // 3. Additional validation for sensitive clients (optional)
    if (isSensitiveClient(targetClientId) && !hasExplicitApproval()) {
        return false;
    }

    return true;
}
```

### Audit Requirements

**Tier 3 queries require enhanced logging:**

```java
// AIG_QueryAudit record for service provider query
{
    "AD_User_ID": 100,           // AI user
    "CreatedBy": 1000050,        // Support engineer
    "AIGAccessTier": "S",        // Service Provider
    "AIGTargetClients": "1000014",
    "IsCrossTenant": "Y",
    "AIGQuerySQL": "SELECT * FROM C_Invoice WHERE DocStatus='IN'",
    "AIGSecuredSQL": "SELECT * FROM C_Invoice WHERE DocStatus='IN' AND AD_Client_ID=1000014",
    "AIGJustification": "Support ticket #12345 - invoice validation issue"
}
```

### Justification Requirement

For Tier 3 access, optionally require justification:

```java
public class SecureQueryRequest {
    // For Tier 3, require reason for cross-tenant access
    private String accessJustification;  // e.g., "Support ticket #12345"
}
```

---

## Implementation Plan

### Phase 1: Tier 2 (Dictionary Access)

**Effort:** 1 week

1. Modify SecureDatabaseQueryExecutor to allow AD_Client_ID IN (0, user_client)
2. Create AIContextWithDictionary provider
3. Pre-populate AI context with relevant AD_* metadata
4. Update audit logging

### Phase 2: Tier 3 (Service Provider)

**Effort:** 2 weeks

1. Add IsServiceProvider flag to AD_Role (or use existing)
2. Implement target client authorization
3. Add enhanced audit fields
4. Build justification tracking
5. Create admin UI for service provider role management

### Phase 3: RAG Enhancement

**Effort:** 2 weeks (parallel with Phase 2)

1. Index Application Dictionary in vector store
2. Index AD_Element descriptions for semantic search
3. Enable AI to retrieve relevant schema context automatically

---

## Alternatives Considered

### Alternative 1: Always Include System Client

**Approach:** All queries automatically include AD_Client_ID = 0

**Rejected because:**
- Leaks system configuration to all users
- Cannot control metadata exposure
- Some AD_* tables contain sensitive data

### Alternative 2: Separate AI Instance Per Tenant

**Approach:** Deploy separate AI provider per tenant

**Rejected because:**
- Operational complexity (N deployments)
- Cannot provide cross-tenant support
- Higher infrastructure costs
- No shared knowledge base

### Alternative 3: Superuser Mode (No Restrictions)

**Approach:** Service provider bypasses all security

**Rejected because:**
- Violates principle of least privilege
- Audit trail less meaningful
- Risk of accidental data exposure
- Compliance concerns

---

## Consequences

### Positive

- ✅ Service provider can effectively support tenants
- ✅ AI has Application Dictionary context for better answers
- ✅ Cross-tenant analysis possible when authorized
- ✅ Clear audit trail for all access tiers
- ✅ Gradual privilege escalation (not all-or-nothing)
- ✅ Respects iDempiere security model

### Negative

- ❌ Adds complexity to security model
- ❌ Requires careful role configuration
- ❌ Enhanced audit storage requirements
- ❌ Potential for misconfiguration

### Neutral

- Service provider access requires explicit setup
- Documentation needed for tier selection
- Monitoring dashboard recommended for Tier 3 usage

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Dictionary queries (Tier 2) | Available to all power users |
| Cross-tenant support time | 50% reduction with AI assistance |
| Unauthorized access attempts | 0 (blocked by tier validation) |
| Audit coverage | 100% for all tiers |
| AI answer accuracy (with dictionary) | +30% improvement |

---

## References

- [ADR-007: Database Security Model](007-database-security-model.md)
- [iDempiere Multi-Tenant Architecture](https://wiki.idempiere.org/en/Multi-Tenant)
- [Application Dictionary Overview](https://wiki.idempiere.org/en/Application_Dictionary)
- Implementation: `SecureDatabaseQueryExecutor.java`

---

*ADR-029 | Version 1.0 | 2025-12-03*
*Status: Proposed*
*Decision: Tiered AI Access Model (Tenant / Dictionary / Service Provider)*
