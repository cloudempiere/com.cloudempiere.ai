# ADR-011: Specialized Agent Scopes by Business Domain

**Status**: Accepted
**Date**: 2025-12-01
**Deciders**: Architecture Team, Business Stakeholders
**Related**: [ADR-009](009-domain-boundaries-agent-scope.md), [ADR-010](010-agent-orchestration-architecture.md)

## Context

Rather than building a single "super-agent" with access to all ERP data and operations, we need specialized agents aligned to iDempiere business domains. Each agent should have:
- Clear domain responsibility
- Specific data access permissions
- Limited action scope
- Dedicated user roles
- Independent cost tracking

This approach provides:
- **Security**: Reduced blast radius if agent behaves unexpectedly
- **Auditability**: Easy to track which agent performed which action
- **Cost Control**: Per-domain budget allocation
- **Maintainability**: Specialists are easier to understand than generalists
- **Scalability**: Proven pattern for adding new domains

## Decision

We will implement a **three-tier agent specialization model** aligned to iDempiere's functional domains:

### Tier 1: Core Domain Agents (Phase 1)

Low-risk, high-value agents for essential operations:

#### InventoryAgent

**Purpose**: Optimize stock levels and warehouse operations

**Scope**:
```
Domain: Inventory Management
Risk Level: LOW
Users: Warehouse Managers, Operations Team
```

**Data Access**:
```yaml
Read Tables:
  - M_Storage (stock levels)
  - M_Product (product master)
  - M_Warehouse (warehouse config)
  - M_Locator (storage locations)
  - M_MovementLine (historical movements)
  - M_InOut (shipments/receipts)
  - M_Product_Category (categorization)

Write Tables:
  - M_Inventory (adjustments - draft only)
  - M_Movement (stock movements - draft only)

Forbidden Tables:
  - GL_Journal (accounting)
  - C_Invoice (pricing data)
  - AD_User (personnel)
  - Payroll tables
```

**Tools**:
```java
@Agent(
    name = "InventoryAgent",
    domain = "INVENTORY",
    riskLevel = RiskLevel.LOW
)
public class InventoryAgent implements IAgent {
    // Available tools:
    - query_inventory_levels()
    - analyze_movement_trends()
    - identify_slow_movers()
    - calculate_safety_stock()
    - recommend_inventory_adjustment()
    - optimize_warehouse_layout()
    - create_inventory_count()
    - generate_inventory_report()
}
```

**Boundaries**:
```yaml
Organizational: Assigned warehouse(s) only
Time Period: Last 12 months history
Max Tokens/Request: 12,000
Daily Budget: $150
Actions:
  - Create: M_Inventory adjustments
  - Update: M_Locator (location info)
  - Read: All accessible tables
  - Cannot: Delete any records, create shipments
```

---

#### SalesAgent

**Purpose**: Analyze sales performance and customer behavior

**Scope**:
```
Domain: Sales & CRM
Risk Level: LOW
Users: Sales Managers, Directors
```

**Data Access**:
```yaml
Read Tables:
  - C_Order (sales orders)
  - C_OrderLine (order details)
  - C_Invoice (sales invoices)
  - C_InvoiceLine (invoice details)
  - C_BPartner (customers only, filtered by assignment)
  - C_BPartner_Location (customer locations)
  - M_Product (products)
  - M_Product_Category (categories)
  - C_DocType (document types)

Read-Only Secondary:
  - C_PriceList (pricing info)
  - C_UOM (units of measure)
  - AD_Org (organizational data)

Forbidden Tables:
  - GL_Journal (accounting detail)
  - PO_Header (purchasing)
  - M_Storage (inventory detail - summary only)
  - HR_Employee (payroll)
```

**Tools**:
```java
@Agent(
    name = "SalesAgent",
    domain = "SALES",
    riskLevel = RiskLevel.LOW
)
public class SalesAgent implements IAgent {
    // Available tools:
    - analyze_sales_trends()
    - customer_analysis()
    - revenue_reporting()
    - order_status_tracking()
    - product_performance()
    - generate_forecast()
    - compare_with_budget()
    - create_sales_report()
}
```

**Boundaries**:
```yaml
Organizational: Assigned sales org(s)
Time Period: Current + last 12 months
Max Tokens/Request: 15,000
Daily Budget: $200
Data Filter: Is_Sales_Trx = 'Y'
User Filter: Only assigned customers
Actions:
  - Read: All accessible tables
  - Write: None (read-only analysis)
  - Execute: generate_sales_report process
```

---

#### PurchasingAgent

**Purpose**: Optimize purchase orders and supplier management

**Scope**:
```
Domain: Procurement
Risk Level: MEDIUM (can create draft POs)
Users: Procurement Managers
```

**Data Access**:
```yaml
Read Tables:
  - M_Storage (current inventory)
  - M_MovementLine (usage history)
  - PO_Header (purchase orders)
  - PO_Line (PO details)
  - C_BPartner (suppliers)
  - M_Product (products)
  - M_Product_PO (supplier catalog)
  - C_OrderLine (sales demand for forecasting)
  - M_MatchPO (invoice matching)

Write Tables:
  - PO_Header (draft status only)
  - PO_Line (draft status only)

Forbidden:
  - Approve or process POs (manager only)
  - Change supplier after approved
  - Access pricing strategies (confidential)
```

**Tools**:
```java
@Agent(
    name = "PurchasingAgent",
    domain = "PROCUREMENT",
    riskLevel = RiskLevel.MEDIUM
)
public class PurchasingAgent implements IAgent {
    // Available tools:
    - analyze_inventory_usage()
    - calculate_optimal_po() // EOQ calculation
    - compare_prices() // multi-supplier
    - predict_demand() // based on sales
    - draft_po() // auto-generate
    - get_supplier_performance()
    - identify_bulk_discount()
}
```

**Boundaries**:
```yaml
Organizational: Assigned warehouse(s)
Time Period: Last 6 months + forecast period
Max Tokens/Request: 20,000 (complex calculations)
Daily Budget: $300
Actions:
  - Create: PO_Header, PO_Line (draft only)
  - Update: PO_Line (draft only)
  - Cannot: Approve POs, modify finalized POs
Audit: All PO drafts logged with AI signature
```

---

#### KnowledgeBaseAgent

**Purpose**: AI-assisted content management for documentation and help articles

**Scope**:
```
Domain: Knowledge Base Content Management
Risk Level: LOW
Users: Technical Writers, Support Staff, Administrators
```

**Data Access**:
```yaml
Read Tables:
  - KB_Article (articles)
  - KB_Category (categories)
  - KB_Tag (tags)
  - KB_Article_Category (mappings)
  - AD_Table, AD_Column (for context)

Write Tables:
  - KB_Article (draft only)
  - KB_Article_Category (mappings)

Forbidden Tables:
  - AD_User (personnel)
  - C_BPartner (customer data)
  - Financial tables
```

**Tools**:
```java
@Agent(
    name = "KnowledgeBaseAgent",
    domain = "KNOWLEDGE_BASE",
    riskLevel = RiskLevel.LOW
)
public class KnowledgeBaseTools {
    // Available tools:
    - searchArticles()       // Full-text search
    - getArticle()           // Retrieve by ID
    - listCategories()       // Category hierarchy
    - analyzeContent()       // Editor.js metrics
    - findSimilar()          // Similarity search
    - validateSyntax()       // Block validation
    - suggestPlacement()     // AI placement recommendations
    - createArticle()        // Draft creation
    - updateArticle()        // Draft updates
}
```

**Boundaries**:
```yaml
Organizational: Accessible KB categories only
Time Period: All articles (no time limit)
Max Tokens/Request: 8,000
Daily Budget: $50
Actions:
  - Create: KB_Article (draft only)
  - Update: KB_Article (draft only)
  - Read: All accessible KB tables
  - Cannot: Delete articles, publish without approval
Audit: All KB changes logged with AI signature
```

**Related**: [ADR-016](016-knowledge-base-agent.md) - Knowledge Base Agent Domain

---

### Tier 2: Specialized Domain Agents (Phase 2)

Medium-risk agents requiring additional governance:

#### AccountingAnalysisAgent

**Purpose**: Analyze GL entries for management reporting

**Scope**:
```
Domain: Accounting Analysis
Risk Level: MEDIUM (sensitive financial data)
Users: Accountants, Controllers
```

**Data Access**:
```yaml
Read Tables:
  - GL_Journal (read-only)
  - GL_JournalLine
  - GL_Account
  - C_AcctSchema
  - Supporting tables (C_Invoice, C_BPartner for context)

Write: None (strictly read-only)

Forbidden:
  - Write to GL tables
  - Access payroll detail
  - Access bank account numbers (encrypted)
```

**Boundaries**:
```yaml
Organizational: Assigned legal entities
Time Period: Full fiscal year
Max Tokens/Request: 30,000 (complex financial reports)
Daily Budget: $500
Actions: Query and report only
Audit: Quarterly review by external auditor
```

---

#### FinancialReportingAgent

**Purpose**: Generate compliance and management reports

**Scope**:
```
Domain: Financial Reporting
Risk Level: MEDIUM
Users: Management, Board
```

**Tools**:
```java
- generate_income_statement()
- generate_balance_sheet()
- generate_cash_flow()
- analyze_variance() // actual vs budget
- calculate_key_ratios()
- analyze_trends() // multi-period
- export_to_excel() // for auditor
- generate_compliance_report()
```

---

### Tier 3: Executive & Cross-Domain Agents (Phase 3)

High-level agents with aggregated data access only:

#### ExecutiveDashboardAgent

**Purpose**: High-level KPI reporting for C-suite

**Scope**:
```
Domain: Executive Reporting (Cross-domain)
Risk Level: MEDIUM
Users: C-level executives, Board members
```

**Data Access**:
```yaml
Read: Aggregated data only from ALL domains
  - Sales metrics (totals, no customer detail)
  - Purchase spending (totals, no supplier detail)
  - Inventory values (totals, no SKU detail)
  - Financial KPIs (summarized, no GL detail)
  - HR metrics (headcount, no PII)

Cannot Access:
  - Individual employee data
  - Customer PII
  - Supplier negotiations
  - Detailed cost breakdowns
  - Confidential information
```

**Tools**:
```java
@Agent(
    name = "ExecutiveDashboardAgent",
    domain = "EXECUTIVE",
    riskLevel = RiskLevel.MEDIUM
)
public class ExecutiveDashboardAgent implements IAgent {
    // Available tools:
    - calculate_revenue() // aggregated
    - calculate_gross_profit()
    - calculate_inventory_turnover()
    - analyze_risk_metrics()
    - predict_cash_flow()
    - generate_executive_brief() // 1-page summary
}
```

**Boundaries**:
```yaml
Organizational: All orgs (consolidation)
Data: Aggregated only, NO detail drill-down
Access: C-level + CFO only
Max Tokens/Request: 20,000
Daily Budget: $300
Actions: Report generation only
Special: Cannot access PII or individual records
```

---

## Agent Boundary Matrix

Quick reference for all agents:

| Agent Name | Org Scope | Max Tokens | Daily Budget | Write Access | Risk Level | Audit Freq |
|-----------|-----------|------------|--------------|--------------|------------|------------|
| InventoryAgent | Assigned | 12,000 | $150 | Minor | LOW | Monthly |
| SalesAgent | Assigned | 15,000 | $200 | None | LOW | Monthly |
| PurchasingAgent | Assigned | 20,000 | $300 | Draft POs | MEDIUM | Monthly |
| AccountingAnalysisAgent | Assigned | 30,000 | $500 | None | MEDIUM | Quarterly |
| FinancialReportingAgent | Assigned | 30,000 | $500 | None | MEDIUM | Quarterly |
| ExecutiveDashboardAgent | All (consolidated) | 20,000 | $300 | None | MEDIUM | Quarterly |

**Legend**:
- Org Scope: "Assigned" = Limited to specific organizations, "All" = Consolidated view
- Write Access: "None" = Read-only, "Minor" = Config changes, "Draft" = Draft documents only
- Audit Freq: How often boundaries are reviewed

---

## Inter-Agent Communication

Agents can trigger other agents with strict boundaries:

```
InventoryAgent detects low stock
    ↓ triggers (with item, qty, deadline)
PurchasingAgent creates draft PO
    ↓ escalates
Manager approves PO

SalesAgent identifies high-value customer
    ↓ triggers (with customer_id)
CustomerAgent performs deeper analysis
    ↓ returns
SalesAgent receives risk assessment
```

**Rules**:
1. Respect domain boundaries (no data leakage)
2. Pass minimum required data only
3. Log all inter-agent calls
4. Max call depth = 3 (prevent loops)
5. Each agent accountable for its domain

---

## Implementation Checklist

For each new agent:

```yaml
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

## Consequences

### Positive

1. **Security**: Limited blast radius per agent
2. **Auditability**: Clear responsibility per domain
3. **Cost Control**: Per-domain budget allocation
4. **Scalability**: Proven pattern for new agents
5. **Maintainability**: Specialists easier to understand than generalists
6. **Governance**: Clear approval chain per agent
7. **Performance**: Optimized tools per domain

### Negative

1. **Complexity**: More agents to manage than single super-agent
2. **Coordination**: Inter-agent communication requires orchestration
3. **Duplication**: Some tools may be duplicated across agents
4. **Overhead**: Each agent requires separate boundary definition

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Agent scope creep | MEDIUM | Quarterly boundary review |
| Inter-agent loops | MEDIUM | Max call depth = 3 |
| Cost overrun | MEDIUM | Hard budget limits per agent |
| Data leakage between agents | HIGH | Explicit data passing, audit logs |

---

## Rollout Plan

### Phase 1: Core Agents (Weeks 1-2)
- Week 1: InventoryAgent implementation
- Week 2: SalesAgent implementation

### Phase 2: Procurement (Week 3)
- Week 3: PurchasingAgent implementation

### Phase 3: Financial (Week 4)
- Week 4: AccountingAnalysisAgent + FinancialReportingAgent

### Phase 4: Executive (Week 5+)
- Week 5+: ExecutiveDashboardAgent (after core agents proven)

---

## Success Metrics

Per Agent:
- Response accuracy > 90%
- Average cost per query < $1
- Boundary violations = 0
- User satisfaction > 4/5
- Audit findings = 0

Overall System:
- Total daily cost < $2,000
- Agent availability > 99.9%
- Cross-agent coordination success > 95%

---

## References

- [ADR-009: Domain Boundaries and Agent Scope](009-domain-boundaries-agent-scope.md)
- [ADR-010: Agent Orchestration Architecture](010-agent-orchestration-architecture.md)
- iDempiere Application Dictionary Reference
- OWASP Top 10 Security Guidelines
- Domain-Driven Design (Eric Evans)
