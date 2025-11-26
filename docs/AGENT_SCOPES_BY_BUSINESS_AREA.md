# Agent Scopes by Business Area

**Specialization: Agents Aligned to ERP Domains**

**Date**: November 26, 2025
**Context**: Maps CloudEmpiere business areas to specific agent scopes with clear boundaries

---

## Executive Summary

Every agent should be **specialized to a single business area** with:
- Clear domain responsibility
- Specific data access
- Limited action set
- Dedicated user roles
- Isolated cost tracking

This document maps the major iDempiere/CloudEmpiere business areas to agent definitions.

---

## Business Area Hierarchy

```
CLOUDEMPIERE ERP
├─ SALES (CRM/SDM)
│  ├─ SalesAgent
│  ├─ CustomerAgent
│  ├─ OrderAnalysisAgent
│  └─ SalesForecasting Agent
│
├─ PROCUREMENT (TMS/PUR)
│  ├─ PurchasingAgent
│  ├─ VendorAgent
│  ├─ SupplierManagementAgent
│  └─ ProcurementAnalysisAgent
│
├─ INVENTORY (WMS/INV)
│  ├─ InventoryAgent
│  ├─ WarehouseAgent
│  ├─ StockOptimizationAgent
│  └─ LogisticsAgent
│
├─ PRODUCTION (MFG/BOM)
│  ├─ ProductionAgent
│  ├─ BOMAgent
│  ├─ ManufacturingPlanningAgent
│  └─ QualityControlAgent
│
├─ ACCOUNTING (GL/AP/AR)
│  ├─ AccountingAgent
│  ├─ APAgent (Accounts Payable)
│  ├─ ARAgent (Accounts Receivable)
│  ├─ TaxAgent
│  └─ FinancialReportingAgent
│
└─ FINANCE (FI/CO)
   ├─ FinanceAgent
   ├─ CostAnalysisAgent
   ├─ BudgetAgent
   ├─ TreasuryAgent
   └─ DashboardAgent

Each agent = ONE business area
Each agent = SPECIFIC data access
Each agent = LIMITED actions
Each agent = CLEAR boundaries
```

---

## 1. SALES DOMAIN AGENTS

### Agent 1.1: SalesAgent

**Purpose**: Analyze sales performance and customer behavior

**Scope**: Sales Order → Invoice → Customer Interactions

```
┌──────────────────────────────────────────────────────────┐
│ SalesAgent                                               │
├──────────────────────────────────────────────────────────┤
│ Responsibility:                                          │
│ ├─ Analyze sales trends                                │
│ ├─ Customer analysis                                    │
│ ├─ Revenue reporting                                    │
│ ├─ Order status tracking                               │
│ └─ Product performance                                 │
│                                                          │
│ PRIMARY DATA ACCESS (READ):                             │
│ ├─ C_Order (sales orders)                              │
│ ├─ C_OrderLine (order details)                         │
│ ├─ C_Invoice (sales invoices)                          │
│ ├─ C_InvoiceLine (invoice details)                     │
│ ├─ C_BPartner (customers/vendors)                      │
│ ├─ C_BPartner_Location (customer locations)            │
│ ├─ M_Product (products)                                │
│ ├─ M_Product_Category (product categories)             │
│ └─ C_DocType (document types)                          │
│                                                          │
│ SECONDARY DATA (READ-ONLY):                             │
│ ├─ C_PriceList (pricing info)                          │
│ ├─ C_UOM (units of measure)                            │
│ └─ AD_Org (organizational data)                        │
│                                                          │
│ CANNOT ACCESS:                                          │
│ ├─ GL_Journal (accounting)                             │
│ ├─ PO_Header (purchasing)                              │
│ ├─ M_Storage (inventory - only summary)                │
│ ├─ HR_Employee (payroll)                               │
│ └─ AD_User (personnel)                                 │
│                                                          │
│ ACTIONS ALLOWED:                                        │
│ ├─ READ: All accessible tables                         │
│ ├─ WRITE: None (read-only analysis)                    │
│ ├─ EXECUTE: generate_sales_report (process)            │
│ └─ GENERATE: PDF/Excel reports                         │
│                                                          │
│ BOUNDARIES:                                             │
│ ├─ Org: Assigned sales org(s)                          │
│ ├─ Period: Current + last 12 months                    │
│ ├─ Max tokens: 15,000 per query                        │
│ ├─ Daily budget: $200                                  │
│ ├─ Data filter: Is_Sales_Trx = 'Y'                     │
│ └─ User filter: Only assigned customers                │
│                                                          │
│ TOOLS PROVIDED:                                         │
│ ├─ QueryDatabase (filtered to sales)                   │
│ ├─ GetSalesMetadata (field, tab info)                  │
│ ├─ AnalyzeTrends (built-in)                            │
│ ├─ GenerateForecast (predictive)                       │
│ ├─ CompareWithBudget (variance analysis)               │
│ ├─ CreateSalesReport (PDF/Excel)                       │
│ └─ GetCustomerHistory (multi-year analysis)            │
│                                                          │
│ RESPONSIBLE FOR COST TRACKING:                          │
│ └─ Charged to sales department budget                  │
│                                                          │
│ AUDIT REQUIREMENTS:                                     │
│ ├─ All queries logged                                  │
│ ├─ Cost per query tracked                              │
│ ├─ Monthly usage report                                │
│ └─ Quarterly security review                           │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

**Example Workflow**:
```
User: "Analyze Q4 sales performance by region"

Agent:
1. Query C_Order where date in Q4, org_id = 2
2. Get metadata: Which fields for region?
3. Query C_BPartner for region mapping
4. Aggregate: Total, by product, by customer
5. Compare: vs Q3, vs budget
6. Generate: PDF report with charts
7. Return: Executive summary + attachments

Result: ✅ Sales trend analysis
        ✅ Regional breakdown
        ✅ YoY comparison
        ✅ Variance from budget
```

---

### Agent 1.2: CustomerAgent

**Purpose**: Customer intelligence and relationship management

**Scope**: Customer Data → Order History → Account Status

```
┌──────────────────────────────────────────────────────────┐
│ CustomerAgent                                            │
├──────────────────────────────────────────────────────────┤
│ Responsibility:                                          │
│ ├─ Customer profile analysis                           │
│ ├─ Account status tracking                             │
│ ├─ Payment behavior analysis                           │
│ ├─ Credit limit management                             │
│ └─ Customer segment classification                     │
│                                                          │
│ PRIMARY DATA ACCESS:                                    │
│ ├─ C_BPartner (customers only)                         │
│ ├─ C_BPartner_Location (addresses)                     │
│ ├─ C_Order (order history)                             │
│ ├─ C_Invoice (invoice history)                         │
│ ├─ C_InvoicePayment (payment records)                  │
│ ├─ C_BPartner_Stats (calculated stats)                 │
│ └─ M_Product (products ordered)                        │
│                                                          │
│ CANNOT ACCESS:                                          │
│ ├─ Payroll data                                        │
│ ├─ Competitor data                                     │
│ ├─ Internal cost data                                  │
│ └─ Other customer accounts (if multi-tenant)           │
│                                                          │
│ ACTIONS:                                                │
│ ├─ READ: Customer and order data                       │
│ ├─ UPDATE: Customer classification (draft)             │
│ ├─ CANNOT: Delete customer                             │
│ └─ CANNOT: Modify credit limit                         │
│                                                          │
│ TOOLS PROVIDED:                                         │
│ ├─ QueryCustomerData (encrypted access)                │
│ ├─ AnalyzePaymentBehavior                              │
│ ├─ GetCustomerRisk (predictive)                        │
│ ├─ ClassifyCustomer (segmentation)                     │
│ ├─ IdentifyUpsell (opportunity detection)              │
│ └─ GenerateCustomerProposal                            │
│                                                          │
│ BOUNDARIES:                                             │
│ ├─ Org: Assigned account(s) only                       │
│ ├─ Data: PII filtered based on role                    │
│ ├─ Max tokens: 10,000                                  │
│ ├─ Daily budget: $100                                  │
│ └─ Retention: 7 years history                          │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 2. PROCUREMENT DOMAIN AGENTS

### Agent 2.1: PurchasingAgent

**Purpose**: Optimize purchase orders and supplier management

**Scope**: Inventory Levels → Purchase Orders → Supplier Contracts

```
┌──────────────────────────────────────────────────────────┐
│ PurchasingAgent                                          │
├──────────────────────────────────────────────────────────┤
│ Responsibility:                                          │
│ ├─ PO optimization                                      │
│ ├─ Supplier selection                                   │
│ ├─ Cost negotiation support                             │
│ ├─ Lead time management                                 │
│ └─ Purchase forecasting                                 │
│                                                          │
│ PRIMARY DATA ACCESS:                                    │
│ ├─ M_Storage (current inventory)                        │
│ ├─ M_MovementLine (usage history)                       │
│ ├─ PO_Header (purchase orders)                          │
│ ├─ PO_Line (PO details)                                 │
│ ├─ C_BPartner (suppliers)                               │
│ ├─ M_Product (products)                                 │
│ ├─ M_Product_PO (supplier catalog)                      │
│ ├─ C_OrderLine (sales demand)                           │
│ └─ M_MatchPO (invoice matching)                         │
│                                                          │
│ WRITE ACCESS:                                           │
│ ├─ CREATE: PO_Header (draft status only)               │
│ ├─ UPDATE: PO_Line (draft status only)                 │
│ ├─ CANNOT: Approve or process                          │
│ └─ CANNOT: Change supplier after approved              │
│                                                          │
│ TOOLS PROVIDED:                                         │
│ ├─ AnalyzeInventoryUsage                               │
│ ├─ CalculateOptimalPO (EOQ)                             │
│ ├─ ComparePrices (multi-supplier)                       │
│ ├─ PredictDemand (based on sales)                       │
│ ├─ DraftPO (auto-generate)                              │
│ ├─ GetSupplierPerformance (quality, timing)            │
│ └─ IdentifyBulkDiscount (opportunities)                │
│                                                          │
│ BOUNDARIES:                                             │
│ ├─ Org: Assigned warehouse(s)                          │
│ ├─ Max tokens: 20,000 (complex calcs)                  │
│ ├─ Daily budget: $300                                  │
│ ├─ Can only DRAFT POs (not approve)                    │
│ └─ Cost impact: Charged to procurement dept             │
│                                                          │
│ AUDIT TRAIL:                                            │
│ ├─ All PO drafts logged with AI signature              │
│ ├─ Cost analysis saved                                 │
│ ├─ Supplier comparison tracked                         │
│ └─ Approval history maintained                         │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

**Example Workflow**:
```
User: "Create POs to cover the next 30 days of demand"

Agent:
1. Query current inventory M_Storage
2. Query sales orders (demand forecast)
3. Query historical usage M_MovementLine
4. Get lead times from M_Product_PO
5. Calculate: What to order, when, from whom
6. Compare suppliers: price, quality, lead time
7. Draft POs (without approval)
8. Return: PO drafts + cost impact + savings

Result: ✅ Optimized orders created
        ✅ Cost analysis provided
        ✅ Ready for manager approval
        ✅ Lead time covered
```

---

## 3. INVENTORY DOMAIN AGENTS

### Agent 3.1: InventoryAgent

**Purpose**: Optimize inventory levels and warehouse operations

**Scope**: Stock Levels → Movements → Warehouse Locations

```
┌──────────────────────────────────────────────────────────┐
│ InventoryAgent                                           │
├──────────────────────────────────────────────────────────┤
│ Responsibility:                                          │
│ ├─ Stock level monitoring                               │
│ ├─ Slow mover identification                            │
│ ├─ Stock movement optimization                          │
│ ├─ Warehouse utilization                                │
│ └─ Inventory adjustment                                 │
│                                                          │
│ PRIMARY DATA ACCESS:                                    │
│ ├─ M_Storage (stock levels)                             │
│ ├─ M_Warehouse (warehouse config)                       │
│ ├─ M_Locator (storage locations)                        │
│ ├─ M_MovementLine (historical movements)               │
│ ├─ M_InOut (shipments/receipts)                         │
│ ├─ M_Inventory (physical counts)                        │
│ ├─ M_Product (product info)                             │
│ └─ M_Product_Category (categorization)                  │
│                                                          │
│ WRITE ACCESS:                                           │
│ ├─ CREATE: M_Inventory (adjustments)                    │
│ ├─ UPDATE: M_Locator (location info)                    │
│ ├─ CANNOT: Delete stock                                 │
│ └─ CANNOT: Create shipments                             │
│                                                          │
│ TOOLS PROVIDED:                                         │
│ ├─ QueryInventoryLevels                                 │
│ ├─ AnalyzeMovementTrends                                │
│ ├─ IdentifySlowMovers (no sales >90 days)              │
│ ├─ CalculateSafetyStock                                 │
│ ├─ RecommendInventoryAdjustment                         │
│ ├─ OptimizeWarehouseLayout                              │
│ ├─ CreateInventoryCount (physical verification)         │
│ └─ GenerateInventoryReport (by location/product)        │
│                                                          │
│ BOUNDARIES:                                             │
│ ├─ Org: Assigned warehouse(s) only                      │
│ ├─ Max tokens: 12,000                                  │
│ ├─ Daily budget: $150                                  │
│ ├─ Period: Last 12 months of history                    │
│ └─ Create adjustments: Up to 1000 lines                 │
│                                                          │
│ INTEGRATION WITH PURCHASING:                            │
│ └─ Can trigger PurchasingAgent                         │
│    when safety stock breached                          │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

**Example Workflow**:
```
User: "Identify products we should clear and recommend actions"

Agent:
1. Query M_Storage (stock levels)
2. Query M_MovementLine (last sales dates)
3. Filter: Products with no sales in 90+ days
4. Analyze: Qty on hand vs monthly usage
5. Categorize: A) Discontinue, B) Clearance, C) Reposition
6. Recommend: Discount levels, marketing actions
7. Create: Inventory adjustment if needed
8. Return: Clearance plan with financial impact

Result: ✅ Slow movers identified
        ✅ Clearance strategy provided
        ✅ Actions documented
        ✅ Space freed for better SKUs
```

---

### Agent 3.2: WarehouseAgent

**Purpose**: Optimize warehouse operations and logistics

**Scope**: Locations → Movements → Shipments → Receiving

```
┌──────────────────────────────────────────────────────────┐
│ WarehouseAgent                                           │
├──────────────────────────────────────────────────────────┤
│ Responsibility:                                          │
│ ├─ Receiving operations                                 │
│ ├─ Picking optimization                                 │
│ ├─ Shipment coordination                                │
│ ├─ Location allocation                                  │
│ └─ Dock scheduling                                      │
│                                                          │
│ PRIMARY DATA ACCESS:                                    │
│ ├─ M_Locator (locations)                                │
│ ├─ M_InOut (shipments/receipts)                         │
│ ├─ M_InOutLine (shipment details)                       │
│ ├─ C_Order (orders to be picked)                        │
│ ├─ PO_Header (receipts to process)                      │
│ ├─ M_Storage (current stock per location)               │
│ └─ M_Warehouse (dock configuration)                     │
│                                                          │
│ WRITE ACCESS:                                           │
│ ├─ UPDATE: M_InOut (shipment status)                    │
│ ├─ UPDATE: M_Locator (location allocation)              │
│ ├─ CREATE: M_Movement (internal transfers)              │
│ └─ CANNOT: Create order or purchase order               │
│                                                          │
│ TOOLS PROVIDED:                                         │
│ ├─ GeneratePickList (by order)                          │
│ ├─ OptimizePickRoute (minimal travel)                   │
│ ├─ AllocateLocation (receiving goods)                   │
│ ├─ TrackShipment (status monitoring)                    │
│ ├─ PredictCapacity (dock utilization)                   │
│ ├─ GenerateLabelingInstructions                         │
│ └─ CreateDockSchedule (receiving plan)                  │
│                                                          │
│ BOUNDARIES:                                             │
│ ├─ Org: Assigned warehouse(s)                          │
│ ├─ Max tokens: 10,000                                  │
│ ├─ Daily budget: $100                                  │
│ └─ Cannot change pricing or quantities                 │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 4. PRODUCTION DOMAIN AGENTS

### Agent 4.1: ProductionAgent

**Purpose**: Optimize manufacturing and production planning

**Scope**: BOM → Work Orders → Production Schedule

```
┌──────────────────────────────────────────────────────────┐
│ ProductionAgent                                          │
├──────────────────────────────────────────────────────────┤
│ Responsibility:                                          │
│ ├─ Production planning                                  │
│ ├─ Schedule optimization                                │
│ ├─ Resource allocation                                  │
│ ├─ BOM validation                                       │
│ └─ Yield analysis                                       │
│                                                          │
│ PRIMARY DATA ACCESS:                                    │
│ ├─ PP_Product_BOM (BOMs)                                │
│ ├─ PP_Order (production orders)                         │
│ ├─ PP_Order_BOMLine (order details)                     │
│ ├─ M_Product (products)                                 │
│ ├─ M_Resource (equipment)                               │
│ ├─ M_Resourcetype (resource types)                      │
│ ├─ C_OrderLine (sales demand)                           │
│ └─ M_Storage (available materials)                      │
│                                                          │
│ WRITE ACCESS:                                           │
│ ├─ CREATE: PP_Order (draft)                             │
│ ├─ UPDATE: PP_Order (scheduling changes)                │
│ └─ CANNOT: Execute or change approved orders            │
│                                                          │
│ TOOLS PROVIDED:                                         │
│ ├─ AnalyzeBOM (component requirements)                  │
│ ├─ CheckMaterialAvailability                            │
│ ├─ CalculateProductionQty (demand + buffer)             │
│ ├─ OptimizeProduction Schedule                          │
│ ├─ PredictYield (historical patterns)                   │
│ ├─ CreateProductionOrder (draft)                        │
│ └─ GenerateProductionReport                             │
│                                                          │
│ BOUNDARIES:                                             │
│ ├─ Org: Manufacturing site(s)                          │
│ ├─ Max tokens: 25,000 (complex calcs)                  │
│ ├─ Daily budget: $400                                  │
│ └─ Cannot change engineering specs                     │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 5. ACCOUNTING DOMAIN AGENTS

### Agent 5.1: APAgent (Accounts Payable)

**Purpose**: Manage supplier invoices and payments

**Scope**: PO Matching → Invoices → Payments

```
┌──────────────────────────────────────────────────────────┐
│ APAgent (Accounts Payable)                               │
├──────────────────────────────────────────────────────────┤
│ Responsibility:                                          │
│ ├─ Invoice matching (3-way: PO/Receipt/Invoice)        │
│ ├─ Payment scheduling                                   │
│ ├─ Discount optimization                                │
│ ├─ Supplier aging                                       │
│ └─ Exception reporting                                  │
│                                                          │
│ PRIMARY DATA ACCESS:                                    │
│ ├─ C_Invoice (vendor invoices)                          │
│ ├─ C_InvoiceLine (invoice details)                      │
│ ├─ C_InvoicePayment (payment records)                   │
│ ├─ PO_Header (purchase orders)                          │
│ ├─ M_InOut (receipts)                                   │
│ ├─ M_MatchPO (matching)                                 │
│ ├─ C_BPartner (suppliers)                               │
│ └─ C_DueList (aging)                                    │
│                                                          │
│ WRITE ACCESS:                                           │
│ ├─ UPDATE: C_Invoice (match status)                     │
│ ├─ CREATE: C_Payment (if approved budget)               │
│ └─ CANNOT: Delete invoice                               │
│                                                          │
│ TOOLS PROVIDED:                                         │
│ ├─ ValidateInvoiceMatch (3-way matching)                │
│ ├─ IdentifyDiscrepancies                                │
│ ├─ OptimizePayment Schedule                             │
│ ├─ CalculateDiscountOpportunity                         │
│ ├─ AnalyzeSupplierAging                                 │
│ ├─ FlagDuplicateInvoice                                 │
│ └─ GenerateAPReport (aging, exceptions)                 │
│                                                          │
│ BOUNDARIES:                                             │
│ ├─ Org: Assigned cost centers                          │
│ ├─ Max tokens: 15,000                                  │
│ ├─ Daily budget: $200                                  │
│ ├─ Period: Current + prior period                       │
│ └─ Cannot approve payments >budget                      │
│                                                          │
│ AUDIT TRAIL:                                            │
│ ├─ All matches logged                                  │
│ ├─ Approval history tracked                            │
│ ├─ Payment recommendations audited                      │
│ └─ Exception resolutions documented                    │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

### Agent 5.2: ARAgent (Accounts Receivable)

**Purpose**: Manage customer collections and credits

**Scope**: Invoices → Payments → Collections

```
┌──────────────────────────────────────────────────────────┐
│ ARAgent (Accounts Receivable)                             │
├──────────────────────────────────────────────────────────┤
│ Responsibility:                                          │
│ ├─ Collection prioritization                            │
│ ├─ Aging analysis                                       │
│ ├─ Credit limit validation                              │
│ ├─ Dispute resolution                                   │
│ └─ DSO optimization                                     │
│                                                          │
│ PRIMARY DATA ACCESS:                                    │
│ ├─ C_Invoice (customer invoices)                        │
│ ├─ C_InvoicePayment (receipts)                          │
│ ├─ C_BPartner (customers)                               │
│ ├─ C_BPartner_Stats (customer stats)                    │
│ ├─ C_DueList (aging)                                    │
│ ├─ C_Order (order history)                              │
│ └─ GL_Journal (disputed items)                          │
│                                                          │
│ READ ONLY (no create/update):                           │
│ └─ No writes - analysis only                            │
│                                                          │
│ TOOLS PROVIDED:                                         │
│ ├─ AnalyzeAging (aged balances)                         │
│ ├─ IdentifyAtRisk (non-payment patterns)                │
│ ├─ PrioritizeCollections (by amount/age)                │
│ ├─ AnalyzeDisputes (invoice questions)                  │
│ ├─ CalculateDSO (days sales outstanding)                │
│ ├─ IdentifyFraud (unusual patterns)                     │
│ └─ GenerateARReport (aging, collections)                │
│                                                          │
│ BOUNDARIES:                                             │
│ ├─ Org: Assigned sales org(s)                          │
│ ├─ Max tokens: 12,000                                  │
│ ├─ Daily budget: $150                                  │
│ ├─ Period: Last 24 months history                       │
│ └─ Cannot write/change invoices                        │
│                                                          │
│ INTEGRATION:                                            │
│ └─ Can escalate to SalesAgent for negotiations         │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 6. FINANCE DOMAIN AGENTS

### Agent 6.1: FinancialReportingAgent

**Purpose**: Generate compliance and management reports

**Scope**: GL Entries → Financial Statements

```
┌──────────────────────────────────────────────────────────┐
│ FinancialReportingAgent                                  │
├──────────────────────────────────────────────────────────┤
│ Responsibility:                                          │
│ ├─ Financial statement generation                       │
│ ├─ Variance analysis                                    │
│ ├─ Budget vs Actual reporting                           │
│ ├─ Compliance reporting                                 │
│ └─ Management KPI reporting                             │
│                                                          │
│ PRIMARY DATA ACCESS:                                    │
│ ├─ GL_Journal (journal entries)                         │
│ ├─ GL_JournalLine (entry details)                       │
│ ├─ GL_Account (account definitions)                     │
│ ├─ GL_Acct_Schema (accounting scheme)                   │
│ ├─ C_Invoice (supporting data)                          │
│ ├─ C_BPartner (business partners)                       │
│ └─ Budget tables (budget vs actual)                     │
│                                                          │
│ READ ONLY (no modifications):                           │
│ └─ All reads are read-only                              │
│                                                          │
│ TOOLS PROVIDED:                                         │
│ ├─ GenerateIncomeStatement                              │
│ ├─ GenerateBalanceSheet                                 │
│ ├─ GenerateCashFlow                                     │
│ ├─ AnalyzeVariance (actual vs budget)                   │
│ ├─ CalculateKey Ratios                                  │
│ ├─ AnalyzeTrends (multi-period)                         │
│ ├─ ExportToExcel (for auditor)                          │
│ └─ GenerateComplianceReport (regulatory)                │
│                                                          │
│ BOUNDARIES:                                             │
│ ├─ Org: Assigned legal entities                        │
│ ├─ Max tokens: 30,000 (complex calcs)                  │
│ ├─ Daily budget: $500 (extensive queries)              │
│ ├─ Period: Full fiscal year                            │
│ └─ Cannot modify GL entries                            │
│                                                          │
│ AUDIT & CONTROL:                                        │
│ ├─ All reports sign-dated                              │
│ ├─ Query log maintained                                │
│ ├─ Quarterly audit review                              │
│ └─ Change control on reports                           │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 7. CROSS-DOMAIN AGENTS (Advanced)

These agents operate across multiple domains but with strict boundaries:

### Agent 7.1: ExecutiveDashboardAgent

**Purpose**: Executive KPI dashboard and reporting

```
┌──────────────────────────────────────────────────────────┐
│ ExecutiveDashboardAgent                                  │
├──────────────────────────────────────────────────────────┤
│ SCOPE: Read-only aggregated data from all domains       │
│                                                          │
│ DATA ACCESS:                                             │
│ ├─ Sales metrics (aggregated)                           │
│ ├─ Purchase spending (aggregated)                       │
│ ├─ Inventory values (aggregated)                        │
│ ├─ Financial KPIs (aggregated)                          │
│ ├─ HR metrics (aggregated, no PII)                      │
│ └─ Production metrics (aggregated)                      │
│                                                          │
│ CANNOT ACCESS:                                          │
│ ├─ Individual employee data                            │
│ ├─ Customer details (PII)                              │
│ ├─ Supplier negotiations                               │
│ ├─ Detailed cost data                                  │
│ └─ Confidential information                            │
│                                                          │
│ TOOLS:                                                  │
│ ├─ CalculateRevenue (total, by month)                  │
│ ├─ CalculateGrossProfit                                │
│ ├─ CalculateInventoryTurnover                          │
│ ├─ AnalyzeRiskMetrics                                  │
│ ├─ PredictCashFlow                                     │
│ └─ GenerateExecutiveBrief (1-page)                      │
│                                                          │
│ BOUNDARIES:                                             │
│ ├─ Org: All orgs (reporting consolidation)             │
│ ├─ Data: Aggregated only (no detail)                    │
│ ├─ Access: C-level + CFO only                           │
│ ├─ Max tokens: 20,000                                  │
│ ├─ Daily budget: $300                                  │
│ └─ Cannot drill into detail (no PII access)            │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 8. Domain Boundary Reference Matrix

```
┌─────────────────────┬──────┬──────────┬───────┬────────┬──────────┐
│ Agent               │ Org  │ Max Tokens│ Cost  │ Writes │ Audited  │
├─────────────────────┼──────┼──────────┼───────┼────────┼──────────┤
│ SalesAgent          │ Asgd │ 15,000   │ $200  │ No     │ Monthly  │
│ CustomerAgent       │ Asgd │ 10,000   │ $100  │ Maybe  │ Quarterly│
│ PurchasingAgent     │ Asgd │ 20,000   │ $300  │ Draft  │ Monthly  │
│ InventoryAgent      │ Asgd │ 12,000   │ $150  │ Minor  │ Monthly  │
│ WarehouseAgent      │ Asgd │ 10,000   │ $100  │ Status │ Daily    │
│ ProductionAgent     │ Asgd │ 25,000   │ $400  │ Draft  │ Monthly  │
│ APAgent             │ Asgd │ 15,000   │ $200  │ Minor  │ Daily    │
│ ARAgent             │ Asgd │ 12,000   │ $150  │ No     │ Weekly   │
│ FinancialReport     │ Asgd │ 30,000   │ $500  │ No     │ Quarterly│
│ ExecutiveDashboard  │ All  │ 20,000   │ $300  │ No     │ Quarterly│
└─────────────────────┴──────┴──────────┴───────┴────────┴──────────┘

Legend:
Org: Asgd = Assigned org(s), All = All orgs (consolidated)
Cost: Daily budget for API calls
Writes: No=Read-only, Minor=Config, Draft=Draft status only, Maybe=Restricted
Audited: Audit review frequency
```

---

## 9. Inter-Agent Communication

Agents can trigger other agents (with boundaries):

```
InventoryAgent detects low stock
    ↓
Triggers → PurchasingAgent (with item, qty, deadline)
    ↓
PurchasingAgent creates draft PO
    ↓
Escalates → Manager for approval

SalesAgent identifies high-value customer
    ↓
Triggers → CustomerAgent (for deeper analysis)
    ↓
CustomerAgent provides risk assessment
    ↓
Returns → To SalesAgent for follow-up

ProductionAgent identifies yield issue
    ↓
Escalates → QualityControlAgent (specialized, new)
    ↓
QualityControlAgent investigates
    ↓
Both agents log findings (audit trail)
```

**Rules for Inter-Agent Communication**:
1. Always respect domain boundaries
2. Pass minimum required data (no PII)
3. Log all inter-agent calls
4. No infinite loops (max depth = 3)
5. Each agent remains responsible for its domain

---

## 10. Implementation Checklist

### For Each Business Area Agent:

```
☐ Define domain scope clearly
☐ List accessible tables (specific to domain)
☐ List forbidden tables (explicitly)
☐ Define write boundaries (what, when, who)
☐ Set token limits (by complexity)
☐ Set daily budget (by criticality)
☐ Define user roles (who can use)
☐ Create audit requirements
☐ List specific tools (5-10 tools per agent)
☐ Document inter-agent dependencies
☐ Get domain expert review
☐ Get security team approval
☐ Get CFO approval (budget)
☐ Get CTO approval (integration)
```

---

## 11. Example: Full Agent Definition Template

```
┌──────────────────────────────────────────────────────────┐
│ AGENT: ____________________                               │
│ DOMAIN: ____________________                              │
│ VERSION: 1.0                                              │
│ APPROVED: [Signature] [Date]                              │
├──────────────────────────────────────────────────────────┤
│ PRIMARY PURPOSE:                                          │
│ _____________________________________________________    │
│                                                          │
│ PRIMARY DATA TABLES (READ):                              │
│ ├─ _____________________                                │
│ ├─ _____________________                                │
│ └─ _____________________                                │
│                                                          │
│ WRITABLE TABLES (CONDITIONS):                            │
│ ├─ _____ (status: draft only)                           │
│ └─ _____ (permission: org manager)                      │
│                                                          │
│ FORBIDDEN TABLES:                                        │
│ ├─ _____________________                                │
│ └─ _____________________                                │
│                                                          │
│ TOOLS (Min 5, Max 10):                                  │
│ ├─ _____________________                                │
│ ├─ _____________________                                │
│ └─ _____________________                                │
│                                                          │
│ BOUNDARIES:                                              │
│ ├─ Org: ________________                                │
│ ├─ Period: ________________                              │
│ ├─ Max tokens: ________________                          │
│ ├─ Daily budget: $________________                       │
│ └─ Cost center: ________________                         │
│                                                          │
│ APPROVAL SIGNATURES:                                     │
│ ├─ Architect: ________________ Date: _____              │
│ ├─ Security: ________________ Date: _____              │
│ ├─ Domain Owner: ________________ Date: _____           │
│ ├─ Finance: ________________ Date: _____              │
│ └─ Executive: ________________ Date: _____             │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## Conclusion

Each agent should be:

✅ **Specialized** - One clear business domain
✅ **Bounded** - Clear data access limits
✅ **Auditable** - Complete logging
✅ **Safe** - Cannot cause unintended damage
✅ **Governed** - Approval chain documented
✅ **Monitored** - Cost and usage tracked
✅ **Escalable** - Can add new domains

By defining agents by business area with clear boundaries, you create:
- Easy to understand (business language)
- Easy to audit (one domain per agent)
- Easy to scale (replicate proven patterns)
- Safe to deploy (limited scope = lower risk)
- Cost-controlled (per-domain budgets)

