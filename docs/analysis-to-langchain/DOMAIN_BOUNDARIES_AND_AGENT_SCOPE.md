# Domain Boundaries & Agent Scope Architecture

**Critical Design Decision Document**
**Date**: November 26, 2025

---

## Executive Summary

Before implementing agents, you must clearly define:
1. **Domain Boundaries** - What business domains will agents operate in?
2. **Agent Scope** - What authority and actions can each agent take?
3. **Data Boundaries** - What data can agents read/write/access?
4. **Organizational Boundaries** - Multi-tenancy and org-level isolation

**Without clear boundaries, you risk:**
- Agents making decisions beyond their authority
- Data leakage between organizations
- Uncontrolled cost growth
- Audit trail complexity
- Security vulnerabilities

---

## The Domain Boundary Problem

### What Can Go Wrong Without Clear Boundaries?

**Scenario 1: Runaway Cost**
```
User: "Analyze all our inventory"
Agent: "I'll query all products across all warehouses"

Without boundaries:
- Agent queries ALL products (100,000 items)
- Processes ALL inventory locations (500+ locations)
- Makes 50+ LLM API calls
- Cost: $500+ for one query

With boundaries:
- Agent: "Limited to 100 items per query"
- "Limited to current warehouse"
- Cost: $5 total
```

**Scenario 2: Data Breach**
```
User: "Analyze customer orders"
Agent: "I have access to ALL orders from ALL customers"

Without boundaries:
- Agent sees orders from Organization A, B, C, D...
- Agent sees customer PII (emails, addresses)
- Compliance violation (GDPR, CCPA)

With boundaries:
- Agent: "Limited to Organization A only"
- "Cannot see PII fields"
- "Cannot see other org data"
```

**Scenario 3: Unauthorized Action**
```
User: "Let me know about my order"
Agent: "I can update ANY order in the system"

Without boundaries:
- Agent modifies orders from other customers
- Agent cancels customer shipments
- Agent changes pricing

With boundaries:
- Agent: "Read-only for this customer's orders"
- "Cannot modify records"
- "Cannot access other customers"
```

---

## Domain Analysis: iDempiere Structure

### iDempiere Organization Model

```
┌─────────────────────────────────────────┐
│          Tenant (AD_Client)             │  ← AD_Client_ID
│    (e.g., "CloudEmpiere Slovakia")      │
├─────────────────────────────────────────┤
│  ┌─────────────────────────────────────┐│
│  │ Organization 1 (AD_Org_ID = 1)      ││  ← AD_Org_ID
│  │ "Bratislava Office"                 ││
│  │ ├─ Users                            ││
│  │ ├─ Inventory                        ││
│  │ ├─ Orders                           ││
│  │ └─ Accounts                         ││
│  └─────────────────────────────────────┘│
│  ┌─────────────────────────────────────┐│
│  │ Organization 2 (AD_Org_ID = 2)      ││
│  │ "Vienna Office"                     ││
│  │ ├─ Users                            ││
│  │ ├─ Inventory                        ││
│  │ ├─ Orders                           ││
│  │ └─ Accounts                         ││
│  └─────────────────────────────────────┘│
└─────────────────────────────────────────┘

Each data record has:
- AD_Client_ID (which company)
- AD_Org_ID (which department/office)
- Created_By (which user)
- AD_Role_ID (user permissions)
```

### Key iDempiere Domains

```
SALES DOMAIN
├─ C_Order (Sales Orders)
├─ C_Invoice (Sales Invoices)
├─ C_OrderLine (Order Details)
├─ C_InvoiceLine (Invoice Details)
├─ M_Product (Products)
└─ C_BPartner (Customers/Vendors)

INVENTORY DOMAIN
├─ M_Warehouse (Warehouses)
├─ M_Locator (Storage Locations)
├─ M_Storage (Stock Levels)
├─ M_MovementLine (Stock Movements)
└─ M_InOut (Shipments)

PURCHASING DOMAIN
├─ PO_Header (Purchase Orders)
├─ PO_Line (PO Details)
├─ M_MatchPO (Matching)
└─ C_InvoicePayments

ACCOUNTING DOMAIN
├─ GL_Journal (Journal Entries)
├─ GL_JournalLine (Line Items)
├─ C_Acct_Schema (Accounting Schemes)
└─ GL_Account (Chart of Accounts)

MASTER DATA DOMAIN
├─ M_Product_Category (Categories)
├─ C_BPartner_Location (Locations)
├─ M_Warehouse (Locations)
└─ C_DocType (Document Types)
```

---

## Domain Boundaries Framework

### Boundary Type 1: Organizational Boundaries

```
MANDATORY - All agents MUST respect org boundaries

┌─ Multi-Tenant Safety ─────────────────────┐
│                                           │
│ RULE 1: Agent can only see data for      │
│         current AD_Client_ID              │
│                                           │
│ RULE 2: Agent can only see data for      │
│         accessible AD_Org_ID              │
│                                           │
│ RULE 3: Agent filters applied to         │
│         EVERY query                       │
│                                           │
│ Implementation:                           │
│ WHERE AD_Client_ID = ?                   │
│ AND AD_Org_ID IN (user_accessible_orgs) │
│                                           │
└───────────────────────────────────────────┘

Example: User from Vienna Office
├─ Can see Vienna inventory (AD_Org_ID = 2)
├─ Can see corporate data (AD_Org_ID = 0)
├─ CANNOT see Bratislava (AD_Org_ID = 1)
└─ All queries automatically filtered

Example Bad Agent Behavior:
Query: "SELECT * FROM M_Storage"
❌ No org filter
❌ Would see ALL warehouses
✅ Auto-filter: WHERE AD_Org_ID IN (0, 2)
✅ Only Vienna + corporate
```

### Boundary Type 2: Data Access Boundaries

```
CRITICAL - Define what data agent can ACCESS

Three Levels of Access:

LEVEL 1: Read-Only Data
├─ Master data (products, partners, accounts)
├─ Reference data (categories, tax rates)
├─ Historical data (past orders, invoices)
├─ No write operations
└─ Agent can: Query, Analyze, Report

LEVEL 2: Read + Historical Write Data
├─ Current period orders/invoices
├─ Warehouse adjustments
├─ Journal entries
├─ Write ONLY to own documents
├─ Cannot modify finalized records
└─ Agent can: Create, Update (draft), Report

LEVEL 3: Restricted Data (Confidential)
├─ Payroll information (DO NOT EXPOSE)
├─ Pricing strategies (hidden from sales agents)
├─ Customer credit limits (show only to credit agents)
├─ Bank account details (accounting only)
├─ Salary data (HR only)
└─ Agent access: Filtered by role

Example:
┌─────────────────────────────────────┐
│ Agent Role: Inventory Agent         │
├─────────────────────────────────────┤
│ CAN READ:                           │
│ ├─ M_Storage (warehouse levels)     │
│ ├─ M_Product (product info)         │
│ ├─ M_MovementLine (movements)       │
│ └─ M_Warehouse (warehouse config)   │
│                                     │
│ CAN WRITE:                          │
│ ├─ M_Inventory (count adjustments)  │
│ ├─ M_Movement (stock movements)     │
│ └─ M_InOut (shipments - draft only) │
│                                     │
│ CANNOT ACCESS:                      │
│ ├─ GL_Journal (accounting)          │
│ ├─ C_Invoice (pricing data)         │
│ ├─ AD_User (personnel)              │
│ └─ Payroll tables                   │
└─────────────────────────────────────┘
```

### Boundary Type 3: Action Boundaries

```
CRITICAL - Define what ACTIONS agent can take

PROHIBITED ACTIONS (All Agents):
├─ Delete ANY records (only updates allowed)
├─ Modify finalized documents
├─ Change account structure
├─ Modify user permissions
├─ Access system settings
└─ Export PII

READ-ONLY AGENTS:
├─ Only SELECT statements
├─ No stored procedure execution
├─ No external API calls
├─ No file operations
└─ Cannot trigger workflows

WRITE-ENABLED AGENTS (Careful!):
├─ INSERT into specific tables only
├─ UPDATE (if draft status)
├─ Execute specific procedures (whitelisted)
├─ Limited to current user/org
└─ Audit trail required

Example:
┌─ Inventory Agent ─────────────────────┐
│                                       │
│ ALLOWED:                              │
│ - CREATE M_Inventory adjustment       │
│ - UPDATE M_Inventory (qty_count)      │
│ - READ all product data               │
│ - CALL proc_reconcile_inventory       │
│                                       │
│ FORBIDDEN:                            │
│ - DELETE any inventory                │
│ - MODIFY M_Product prices             │
│ - CALL any accounting procedures      │
│ - Execute external APIs               │
│ - Upload to cloud storage             │
│                                       │
│ CONDITIONAL:                          │
│ - RUN process: generate_po_from_plan  │
│   (only if min_stock validation OK)   │
│                                       │
└───────────────────────────────────────┘
```

### Boundary Type 4: Cost Boundaries

```
IMPORTANT - Prevent runaway API costs

Maximum Tokens Per Request:
├─ Inventory Agent: 10,000 tokens
├─ Sales Agent: 15,000 tokens
├─ Reporting Agent: 20,000 tokens
└─ System Agent (admin): 50,000 tokens

Maximum API Calls Per Day:
├─ Per user: 100 calls
├─ Per org: 1,000 calls
├─ Per system: 10,000 calls
└─ Monitor: Daily cost dashboard

Cost Allocation:
├─ Track cost per org (charge back)
├─ Track cost per user (accountability)
├─ Alert on: 2x normal spend
├─ Block on: 10x normal spend

Example Query Optimization:
┌─────────────────────────────────────┐
│ BEFORE (Expensive):                 │
│                                     │
│ Agent: "Analyze all products"       │
│ Query: SELECT * FROM M_Product      │
│        (100,000 rows, 50MB)         │
│ Tokens: 30,000 (ouch!)              │
│ Cost: $10                           │
│                                     │
│ AFTER (Optimized):                  │
│                                     │
│ Agent: "Analyze slow movers"        │
│ Query: SELECT id, name, qty_on_hand │
│        FROM M_Product               │
│        WHERE last_sale > 90 DAYS    │
│        LIMIT 100                    │
│ Tokens: 2,000                       │
│ Cost: $0.20                         │
│                                     │
│ Savings: 50x cost reduction!        │
└─────────────────────────────────────┘
```

### Boundary Type 5: Time-Based Boundaries

```
USEFUL - Limit agent scope by time period

Current Period Only:
├─ Agent cannot modify past invoices
├─ Agent cannot re-open closed periods
├─ Agent locked into GL period concept
└─ Prevents accounting corruption

Example:
┌─────────────────────────────────────┐
│ Current Period: November 2025       │
│                                     │
│ Agent CAN:                          │
│ ├─ Create Nov 2025 invoices        │
│ ├─ Adjust Nov 2025 inventory       │
│ └─ Update Nov 2025 orders          │
│                                     │
│ Agent CANNOT:                       │
│ ├─ Modify October 2025 (closed)    │
│ ├─ Change September data           │
│ └─ Access 2024 invoices            │
│                                     │
└─────────────────────────────────────┘

Document Lifecycle Boundaries:
├─ Draft: Agent can create/modify
├─ In Progress: Agent cannot touch
├─ Completed: Agent read-only
├─ Closed: Agent forbidden
└─ Archived: Agent forbidden
```

---

## Agent Scope Definition

### Scope Pattern 1: Single Domain Agent

```
AGENT: InventoryAgent
DOMAIN: Inventory Management Only

┌────────────────────────────────────────┐
│ InventoryAgent                         │
│                                        │
│ Responsibility:                        │
│ ├─ Monitor stock levels                │
│ ├─ Identify slow movers                │
│ ├─ Suggest reorder points              │
│ ├─ Create inventory adjustments        │
│ └─ Generate stock reports              │
│                                        │
│ Data Access:                           │
│ ├─ READ: M_Product, M_Storage,         │
│ │        M_Warehouse, M_Locator        │
│ ├─ WRITE: M_Inventory (adjustments)    │
│ └─ EXEC: proc_reconcile_inventory      │
│                                        │
│ Actions:                               │
│ ├─ Query inventory                     │
│ ├─ Analyze trends                      │
│ ├─ Suggest POs                         │
│ ├─ Create adjustments                  │
│ └─ Generate reports                    │
│                                        │
│ Boundaries:                            │
│ ├─ Current warehouse only              │
│ ├─ Current org only                    │
│ ├─ Read-only to products               │
│ ├─ Max 5,000 tokens per query         │
│ └─ Cannot modify finalized orders      │
│                                        │
└────────────────────────────────────────┘

Good: Clear scope, limited access, specific purpose
```

### Scope Pattern 2: Multi-Domain Agent (Risky!)

```
AGENT: SuperAgent (NOT RECOMMENDED)
DOMAIN: Everything

┌────────────────────────────────────────┐
│ SuperAgent (ANTI-PATTERN!)            │
│                                        │
│ Problem:                               │
│ ├─ Too much authority                  │
│ ├─ Hard to audit                       │
│ ├─ Complex permission checks           │
│ ├─ Risk of accidental damage           │
│ └─ Cannot optimize costs               │
│                                        │
│ Access to:                             │
│ ├─ Inventory (M_Storage)               │
│ ├─ Sales (C_Order)                     │
│ ├─ Purchasing (PO_Header)              │
│ ├─ Accounting (GL_Journal)             │
│ ├─ HR (AD_User)                        │
│ └─ Everything                          │
│                                        │
│ Result:                                │
│ ❌ Can accidentally delete orders      │
│ ❌ Can post wrong GL entries           │
│ ❌ Can access payroll data             │
│ ❌ Can modify user accounts            │
│                                        │
└────────────────────────────────────────┘

Bad: Too much power, cannot control, high risk
```

### Scope Pattern 3: Role-Based Agent

```
AGENT: SalesAgent
DOMAIN: Sales + Reporting (related domains)

┌────────────────────────────────────────┐
│ SalesAgent                             │
│                                        │
│ Who uses it: Sales team                │
│ What they do: Sales analysis           │
│                                        │
│ Respects User's Role:                  │
│ ├─ Inherited from AD_Role              │
│ ├─ User can only see customers         │
│ │  they're assigned to                 │
│ ├─ Agent inherits same view            │
│ └─ Prevents data leakage               │
│                                        │
│ Primary Domain: Sales                  │
│ ├─ C_Order (sales orders)              │
│ ├─ C_Invoice (sales invoices)          │
│ ├─ C_BPartner (customers)              │
│ └─ M_Product (products)                │
│                                        │
│ Secondary Domain: Reporting            │
│ ├─ Aggregated data only                │
│ ├─ No individual customer PII          │
│ ├─ Revenue by product                  │
│ └─ Sales trends                        │
│                                        │
│ Cannot Access:                         │
│ ├─ GL_Journal (accounting)             │
│ ├─ Purchasing data                     │
│ ├─ Employee info                       │
│ ├─ Payroll                             │
│ └─ System settings                     │
│                                        │
└────────────────────────────────────────┘

Good: Clear domain, respects roles, safe scope
```

---

## Recommended Domain Structure for CloudEmpiere

### Tier 1: Core Domains (Start Here)

```
DOMAIN 1: Inventory Management Agent
├─ Purpose: Optimize stock levels
├─ Data: M_Storage, M_Product, M_Warehouse
├─ Actions: Read inventory, create adjustments
├─ Users: Warehouse managers
└─ Risk Level: LOW

DOMAIN 2: Sales Analysis Agent
├─ Purpose: Analyze sales performance
├─ Data: C_Order, C_Invoice, M_Product, C_BPartner
├─ Actions: Read sales data, generate reports
├─ Users: Sales managers, directors
└─ Risk Level: LOW

DOMAIN 3: Purchase Planning Agent
├─ Purpose: Optimize purchasing
├─ Data: PO_Header, M_Product, C_BPartner
├─ Actions: Suggest POs, analyze supplier data
├─ Users: Procurement managers
└─ Risk Level: MEDIUM (can create POs)
```

### Tier 2: Specialized Domains (Phase 2)

```
DOMAIN 4: Accounting Analysis Agent
├─ Purpose: Analyze GL entries
├─ Data: GL_Journal (read-only), GL_Account
├─ Actions: Query, analyze, report
├─ Users: Accountants, controllers
└─ Risk Level: MEDIUM (sensitive data)

DOMAIN 5: Manufacturing Planning Agent
├─ Purpose: Optimize production
├─ Data: Production data, BOM, routing
├─ Actions: Suggest schedules, analyze capacity
├─ Users: Plant managers
└─ Risk Level: MEDIUM

DOMAIN 6: Document Automation Agent
├─ Purpose: Generate documents
├─ Data: Order, invoice data
├─ Actions: Create PDFs, emails
├─ Users: Admin, accounting
└─ Risk Level: MEDIUM
```

### Tier 3: Advanced Domains (Phase 3+)

```
DOMAIN 7: HR Analytics Agent (Restricted)
├─ Purpose: HR reporting (anonymous data)
├─ Data: Aggregated HR metrics only
├─ Actions: Report only, no individual data
├─ Users: HR, Management
└─ Risk Level: HIGH (confidential data)

DOMAIN 8: Executive Dashboard Agent
├─ Purpose: High-level KPI reporting
├─ Data: Summarized data only
├─ Actions: Generate executive reports
├─ Users: Management, Board
└─ Risk Level: MEDIUM
```

---

## Boundary Definition Document Template

For EACH agent you create, fill this out:

```
┌─────────────────────────────────────────────────┐
│ AGENT BOUNDARY DEFINITION                       │
├─────────────────────────────────────────────────┤
│                                                 │
│ 1. Agent Name & Purpose                         │
│    Name: ________________                       │
│    Purpose: ________________________________________
│    Primary Users: ____________________________    │
│                                                 │
│ 2. Organizational Boundaries                    │
│    ☐ Single organization only: AD_Org_ID = __  │
│    ☐ Multiple orgs: __________                 │
│    ☐ All orgs (justify):_____________________  │
│    Data Filter:                                 │
│      WHERE AD_Client_ID = current_client_id    │
│      AND AD_Org_ID IN (____________)           │
│                                                 │
│ 3. Data Access Matrix                           │
│                                                 │
│    Table              | READ | WRITE | DELETE  │
│    ──────────────────────────────────────────  │
│    _____________      |  ☐   |   ☐  |   ☐     │
│    _____________      |  ☐   |   ☐  |   ☐     │
│    _____________      |  ☐   |   ☐  |   ☐     │
│    _____________      |  ☐   |   ☐  |   ☐     │
│                                                 │
│ 4. Action Boundaries                            │
│    Maximum tokens per query: __________         │
│    Maximum API calls per day: __________        │
│    Prohibited actions:                          │
│    - _______________________________           │
│    - _______________________________           │
│    - _______________________________           │
│                                                 │
│ 5. Time Boundaries                              │
│    ☐ Current period only                        │
│    ☐ Last X months: ______                      │
│    ☐ All historical data                        │
│    Document status: Can modify ☐ Draft only    │
│                    Read-only ☐ All             │
│                                                 │
│ 6. Cost Boundaries                              │
│    Monthly budget: $__________                  │
│    Alert threshold: $__________                 │
│    Blocking threshold: $__________              │
│                                                 │
│ 7. Audit Requirements                           │
│    ☐ Log all queries                            │
│    ☐ Log all updates                            │
│    ☐ Track cost per org                         │
│    ☐ Daily cost report                          │
│    ☐ Monthly usage analysis                     │
│                                                 │
│ 8. Security Review                              │
│    Risk Level: ☐ LOW ☐ MEDIUM ☐ HIGH          │
│    Approved by: ________________ Date: _____   │
│    Review date: _____________                  │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## Implementation in Code

### Architecture: Enforce Boundaries in Code

```java
// 1. ORGANIZATIONAL BOUNDARIES
public class BoundaryEnforcementFilter {

    public String enforceOrgFilter(String sql, AgentContext context) {
        String orgFilter = String.format(
            " AND AD_Client_ID = %d AND AD_Org_ID IN (%s)",
            context.getClientId(),
            String.join(",", context.getAccessibleOrgIds())
        );
        return sql + orgFilter;
    }
}

// 2. DATA ACCESS BOUNDARIES
public class DataAccessValidator {

    public boolean canRead(String tableName, AgentContext context) {
        DataBoundary boundary = getBoundary(context.getAgentName());
        return boundary.canRead(tableName);
    }

    public boolean canWrite(String tableName, String columnName,
                           AgentContext context) {
        DataBoundary boundary = getBoundary(context.getAgentName());
        return boundary.canWrite(tableName, columnName);
    }
}

// 3. ACTION BOUNDARIES
public class ActionBoundaryValidator {

    public boolean isActionAllowed(String action, AgentContext context) {
        AgentBoundary boundary = getBoundary(context.getAgentName());

        switch(action) {
            case "DELETE": return false; // NEVER allow delete
            case "UPDATE": return boundary.allowUpdate;
            case "INSERT": return boundary.allowInsert;
            case "CALL_PROCEDURE": return boundary.allowedProcedures
                                            .contains(procedureName);
            default: return false;
        }
    }
}

// 4. COST BOUNDARIES
public class CostBoundaryMonitor {

    public void checkCostBoundary(AgentContext context, long tokensUsed) {
        long dailyCost = calculateCost(tokensUsed);
        long dailyBudget = context.getDailyBudget();

        if (dailyCost > dailyBudget) {
            throw new CostBoundaryExceededException(
                "Agent exceeded daily budget: " + dailyCost + " > " + dailyBudget
            );
        }
    }
}

// 5. CONFIGURATION
public class AgentBoundaryConfig {

    @Bean
    public Map<String, AgentBoundary> agentBoundaries() {
        Map<String, AgentBoundary> boundaries = new HashMap<>();

        boundaries.put("InventoryAgent", AgentBoundary.builder()
            .name("InventoryAgent")
            .allowedOrgs(Arrays.asList(1, 2, 0)) // Orgs 1,2 + corporate
            .readableTables(Arrays.asList("M_Storage", "M_Product"))
            .writeableTables(Arrays.asList("M_Inventory"))
            .allowedProcedures(Arrays.asList("proc_reconcile_inventory"))
            .maxTokensPerRequest(5000)
            .dailyBudget(100.0) // $100/day
            .build());

        return boundaries;
    }
}
```

---

## Governance: Who Decides Boundaries?

```
┌─────────────────────────────────────────────────┐
│ AGENT BOUNDARY GOVERNANCE                       │
├─────────────────────────────────────────────────┤
│                                                 │
│ DEFINE (Initial)                                │
│ ├─ Product Architect: Broad scope              │
│ ├─ Security Team: Restrictions                 │
│ ├─ Business Owner: Use case scope              │
│ ├─ Finance: Cost limits                        │
│ └─ Compliance: Audit requirements              │
│                                                 │
│ REVIEW (Quarterly)                              │
│ ├─ IT Security: Still safe?                    │
│ ├─ Finance: Cost trending OK?                  │
│ ├─ Business: Meeting needs?                    │
│ └─ Compliance: Audit trail sufficient?         │
│                                                 │
│ MODIFY (As Needed)                              │
│ ├─ New use case: Expand scope                  │
│ ├─ Breach attempt: Restrict scope              │
│ ├─ Cost runaway: Reduce limits                 │
│ └─ Performance: Optimize queries               │
│                                                 │
│ ESCALATION                                      │
│ ├─ Unauthorized action detected               │
│ │  → Security team notified                    │
│ │  → Agent access suspended                    │
│ ├─ Cost overage                                │
│ │  → Finance review                            │
│ │  → Possible limits reduction                 │
│ ├─ Data breach                                 │
│ │  → Compliance team                           │
│ │  → Incident response                         │
│ └─ Agent error                                 │
│    → Product team review                       │
│    → Boundary adjustment                       │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## Boundary Matrix: Quick Reference

```
┌─────────────────┬─────────┬──────────┬──────┬──────────────┐
│ Agent Name      │ Org     │ Data     │ Cost │ Risk Level   │
├─────────────────┼─────────┼──────────┼──────┼──────────────┤
│ Inventory       │ Single  │ Read+    │ Low  │ LOW          │
│ Sales Analysis  │ Multiple│ Read     │ Low  │ LOW          │
│ Purchase Plan   │ Single  │ Read+    │ Med  │ MEDIUM       │
│ GL Analysis     │ Single  │ Read     │ Med  │ MEDIUM       │
│ HR Analytics    │ Multi   │ Agg Only │ High │ HIGH         │
│ Exec Dashboard  │ Multi   │ Agg Only │ Med  │ MEDIUM       │
└─────────────────┴─────────┴──────────┴──────┴──────────────┘
```

---

## Conclusion: Boundary-Driven Design

### Key Principles

1. **Default: Deny** - Start with no access, grant as needed
2. **Org Filtering: Mandatory** - Every query must be filtered
3. **Role Inheritance** - Agents respect user roles
4. **Audit Everything** - Track all agent actions
5. **Cost Control** - Budget and alerts on spending
6. **Regular Review** - Quarterly boundary audits

### Implementation Order

**Week 1: Implement Boundary Framework**
- [ ] Define agent boundary interface
- [ ] Implement org filtering
- [ ] Add data access validation
- [ ] Add cost monitoring

**Week 2: Define Agent Boundaries**
- [ ] InventoryAgent boundaries
- [ ] SalesAgent boundaries
- [ ] PurchasingAgent boundaries

**Week 3: Enforce Boundaries**
- [ ] All queries filtered
- [ ] All actions validated
- [ ] Cost tracking active
- [ ] Audit logging on

**Week 4: Governance**
- [ ] Approval process defined
- [ ] Review schedule set
- [ ] Escalation procedures documented
- [ ] Team trained

### Result: Safe, Auditable, Controlled Agents

When boundaries are clear:
- ✅ Agents know their authority
- ✅ Data stays where it should
- ✅ Costs are predictable
- ✅ Audit trail is clean
- ✅ Security team sleeps better

