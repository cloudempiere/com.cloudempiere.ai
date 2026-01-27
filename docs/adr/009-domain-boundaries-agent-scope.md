# ADR-009: Domain Boundaries and Agent Scope Architecture

**Status**: Accepted
**Date**: 2025-12-01
**Deciders**: Architecture Team
**Related**: [ADR-004](004-java-agent-framework.md), [ADR-007](007-database-security-model.md)

## Context

AI agents operating within an ERP system like iDempiere require strict boundaries to prevent:
- **Security breaches**: Agents accessing data from unauthorized organizations or clients
- **Cost overruns**: Uncontrolled API usage leading to $500+ charges for single queries
- **Unauthorized actions**: Agents modifying or deleting records beyond their authority
- **Compliance violations**: Accessing PII or confidential data without proper authorization
- **Multi-tenancy leaks**: Viewing data from other clients or organizations

Without explicit boundaries, agents can behave unpredictably and cause serious operational and security issues.

## Decision

We will implement a **five-boundary framework** that all agents must respect:

### 1. Organizational Boundaries (MANDATORY)

All agents MUST respect iDempiere's multi-tenant structure:

```java
// Every query MUST include these filters
WHERE AD_Client_ID = currentClientId
  AND AD_Org_ID IN (accessibleOrgIds)
```

**Implementation**:
- Automatic filtering applied to ALL database queries
- No exceptions - even for system administrators
- Inherited from user's role permissions
- Enforced at the `SecureDatabaseQueryExecutor` level

**Example**:
```java
public class BoundaryEnforcementFilter {
    public String enforceOrgFilter(String sql, AgentContext context) {
        return sql + String.format(
            " AND AD_Client_ID = %d AND AD_Org_ID IN (%s)",
            context.getClientId(),
            String.join(",", context.getAccessibleOrgIds())
        );
    }
}
```

### 2. Data Access Boundaries

Agents have three levels of data access:

**Level 1: Read-Only** (lowest risk)
- Master data (products, partners, accounts)
- Historical data (past orders, invoices)
- No write operations permitted
- Examples: SalesAnalysisAgent, InventoryReportingAgent

**Level 2: Read + Limited Write** (medium risk)
- Can create/update draft documents only
- Cannot modify finalized records
- Cannot delete any records
- Examples: PurchasingAgent (draft POs), InventoryAgent (adjustments)

**Level 3: Restricted Data** (highest sensitivity)
- Payroll, salary data (HR only)
- Pricing strategies (management only)
- Bank account details (accounting only)
- Access filtered by user role
- Examples: FinancialReportingAgent (aggregated only)

**Enforcement**:
```java
public class DataAccessValidator {
    public boolean canRead(String tableName, AgentContext context) {
        DataBoundary boundary = getBoundary(context.getAgentName());
        return boundary.canRead(tableName);
    }

    public boolean canWrite(String tableName, AgentContext context) {
        // NEVER allow DELETE operations
        // Only allow INSERT/UPDATE on whitelisted tables
        // Only for draft status documents
        return boundary.canWrite(tableName)
            && isDocumentDraft(tableName);
    }
}
```

### 3. Action Boundaries

**Prohibited Actions (ALL agents)**:
- DELETE any records
- Modify finalized documents
- Change account structure
- Modify user permissions
- Access system settings
- Export PII without authorization

**Allowed Actions**:
- Read-only agents: SELECT statements only
- Write-enabled agents: INSERT/UPDATE on specific tables with draft status
- Process execution: Whitelisted stored procedures only

**Example**:
```java
public class ActionBoundaryValidator {
    public boolean isActionAllowed(String action, AgentContext context) {
        AgentBoundary boundary = getBoundary(context.getAgentName());

        switch(action) {
            case "DELETE": return false; // NEVER
            case "UPDATE": return boundary.allowUpdate
                               && isDocumentDraft();
            case "INSERT": return boundary.allowInsert;
            case "CALL_PROCEDURE":
                return boundary.allowedProcedures.contains(procedureName);
            default: return false; // Deny by default
        }
    }
}
```

### 4. Cost Boundaries

Prevent runaway API costs through token and budget limits:

**Token Limits per Request**:
- InventoryAgent: 10,000 tokens
- SalesAgent: 15,000 tokens
- ReportingAgent: 20,000 tokens
- SystemAgent (admin): 50,000 tokens

**API Call Limits**:
- Per user: 100 calls/day
- Per organization: 1,000 calls/day
- Per system: 10,000 calls/day

**Budget Tracking**:
```java
public class CostBoundaryMonitor {
    public void checkCostBoundary(AgentContext context, long tokensUsed) {
        long dailyCost = calculateCost(tokensUsed);
        long dailyBudget = context.getDailyBudget();

        if (dailyCost > dailyBudget) {
            throw new CostBoundaryExceededException(
                "Agent exceeded daily budget: $" + dailyCost
            );
        }
    }
}
```

**Cost Optimization Example**:
```
BEFORE (Expensive):
Query: SELECT * FROM M_Product (100,000 rows)
Tokens: 30,000
Cost: $10

AFTER (Optimized):
Query: SELECT id, name, qty_on_hand
       FROM M_Product
       WHERE last_sale > CURRENT_DATE - 90
       LIMIT 100
Tokens: 2,000
Cost: $0.20

Savings: 50x cost reduction
```

### 5. Time-Based Boundaries

**Period Restrictions**:
- Current period only: Agent cannot modify closed periods
- Historical data: Read access to past periods
- Document lifecycle:
  - Draft: Can create/modify
  - In Progress: Cannot touch
  - Completed: Read-only
  - Closed: Forbidden
  - Archived: Forbidden

**Implementation**:
```java
public boolean canModifyPeriod(String periodName, AgentContext context) {
    Period current = getCurrentPeriod();
    Period requested = getPeriod(periodName);

    return requested.equals(current)
        && !requested.isClosed();
}
```

## Consequences

### Positive

- **Security**: Multi-tenant data isolation enforced automatically
- **Cost Control**: Predictable API spending with hard limits
- **Compliance**: GDPR/CCPA compliance through data access restrictions
- **Auditability**: Complete trail of agent actions and boundaries
- **Scalability**: Same boundary pattern applies to all new agents
- **Governance**: Clear approval process for boundary modifications

### Negative

- **Development Overhead**: Every agent requires boundary definition
- **Maintenance**: Boundaries must be reviewed quarterly
- **Flexibility**: Some legitimate use cases may require boundary exceptions
- **Complexity**: Five-boundary framework requires careful planning

### Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Boundary bypass | HIGH | Code reviews, automated tests |
| Cost overrun | MEDIUM | Real-time monitoring, hard limits |
| Over-restriction | LOW | Regular boundary reviews with business |
| Performance impact | LOW | Filtering optimized with indexes |

## Implementation Plan

### Week 1: Framework
- [ ] Implement `BoundaryEnforcementFilter`
- [ ] Implement `DataAccessValidator`
- [ ] Implement `ActionBoundaryValidator`
- [ ] Implement `CostBoundaryMonitor`
- [ ] Add org filtering to `SecureDatabaseQueryExecutor`

### Week 2: Agent Definitions
- [ ] Define boundaries for InventoryAgent
- [ ] Define boundaries for SalesAgent
- [ ] Define boundaries for PurchasingAgent
- [ ] Document boundary definitions in code

### Week 3: Enforcement
- [ ] Apply automatic filtering to all queries
- [ ] Enable cost tracking and alerts
- [ ] Activate audit logging
- [ ] Test boundary enforcement

### Week 4: Governance
- [ ] Create boundary approval process
- [ ] Define quarterly review schedule
- [ ] Document escalation procedures
- [ ] Train team on boundary framework

## Agent Boundary Template

For each new agent, complete this definition:

```
┌─────────────────────────────────────────┐
│ AGENT BOUNDARY DEFINITION               │
├─────────────────────────────────────────┤
│ Agent Name: ____________________        │
│ Domain: ____________________            │
│ Risk Level: ☐ LOW ☐ MEDIUM ☐ HIGH     │
│                                         │
│ Organizational Boundaries:              │
│   Client: ____________________          │
│   Orgs: ____________________            │
│                                         │
│ Data Access:                            │
│   Read Tables: ____________________     │
│   Write Tables: ____________________    │
│   Forbidden Tables: ________________    │
│                                         │
│ Actions:                                │
│   Allowed: ____________________         │
│   Prohibited: ____________________      │
│                                         │
│ Cost Limits:                            │
│   Max Tokens/Request: __________        │
│   Daily Budget: $__________             │
│                                         │
│ Approvals:                              │
│   Architect: __________ Date: ______    │
│   Security: __________ Date: ______     │
│   Finance: __________ Date: ______      │
└─────────────────────────────────────────┘
```

## References

- [ADR-004: Java Agent Framework Selection](004-java-agent-framework.md)
- [ADR-007: Database Security Model](007-database-security-model.md)
- iDempiere Multi-Tenancy Documentation
- GDPR Data Access Requirements
- OWASP Top 10 Security Guidelines
