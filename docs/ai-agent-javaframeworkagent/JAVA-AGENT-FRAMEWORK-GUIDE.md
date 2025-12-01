# Java Agent Framework for CloudEmpiere ERP
## Complete Guide: Agents vs Prompts & Implementation

**Version**: 1.0  
**Created**: November 2025  
**For**: CloudEmpiere Slovakia s.r.o.  
**Target**: iDempiere ERP Integration with Claude AI

---

## Table of Contents

1. [Agents vs Prompts](#agents-vs-prompts)
2. [Why Agents for CloudEmpiere](#why-agents-for-cloudempiere)
3. [Framework Architecture](#framework-architecture)
4. [Core Components](#core-components)
5. [Quick Start Guide](#quick-start-guide)
6. [Tool Implementation Examples](#tool-implementation-examples)
7. [Deployment](#deployment)
8. [Best Practices](#best-practices)

---

## Agents vs Prompts

### What's a Prompt?

A **prompt** is a direct instruction you send to Claude. It's one-way communication: you ask a question, Claude responds, and that's it.

```
You → Claude → Response
(Single turn, no feedback loop)
```

**Example Prompt:**
```
"Analyze this sales data and tell me which products are underperforming:
Q1 Sales: Product A: €50k, Product B: €30k, Product C: €15k"
```

**Limitations:**
- ❌ Claude can't look up actual data from your ERP
- ❌ Claude can't execute actions
- ❌ Claude can't iterate or refine based on results
- ❌ No access to real-time information
- ❌ Static knowledge cutoff (Jan 2025)

---

### What's an Agent?

An **agent** is autonomous software that can:
1. **Receive a goal** ("analyze inventory")
2. **Plan actions** (decide which tools to use)
3. **Execute tools** (query database, generate reports)
4. **Evaluate results** (check if goal is met)
5. **Iterate** (repeat steps 2-4 until done)

```
Goal → Plan → Execute Tool → Check Result → Success?
        ↑_____________________↓
              (iterate if needed)
```

**Example Agent Workflow:**
```
Goal: "Analyze slow-moving inventory and recommend restocking"

Agent executes:
1. Query inventory database → Get stock levels
2. Analyze trends → Calculate movement rates
3. Query supplier info → Check lead times
4. Calculate recommendations → Determine optimal order quantities
5. Generate report → Save as PDF
```

---

### Side-by-Side Comparison

| Aspect | Prompt | Agent |
|--------|--------|-------|
| **Data Access** | Only what you paste in | Full database access via tools |
| **Actions** | None - just responds | Can execute tools, generate reports |
| **Iteration** | Single response | Multi-step loops until done |
| **Real-time Data** | Outdated (knowledge cutoff) | Current data from your systems |
| **Complexity** | Good for simple questions | Excellent for complex workflows |
| **Latency** | Fast (1-2 seconds) | Slower (10-60 seconds) |
| **Cost** | Cheap | Higher (more API calls) |
| **Use Case** | Quick Q&A, brainstorming | Business automation, analysis |
| **Reliability** | Depends on your context | Verifiable (can check results) |

---

### Real-World Example: Inventory Analysis

#### Using a Prompt (❌ Limited)
```
Prompt: "Here's our inventory data:
SKU001: 45 units, SKU002: 200 units, SKU003: 5 units
What should we reorder?"

Claude responds: "SKU003 is low, you should reorder..."
(Problem: Claude guesses without knowing actual daily usage, supplier lead times, warehouse capacity)
```

#### Using an Agent (✅ Comprehensive)
```
Goal: "Analyze inventory and create restocking recommendations"

Agent execution:
1. Tool: query_inventory
   → Retrieves ALL products from ERP database
   
2. Tool: analyze_trends
   → Calculates daily usage for each product
   → Identifies seasonal patterns
   
3. Tool: get_supplier_info
   → Gets lead times, MOQ (minimum order quantity), costs
   
4. Tool: calculate_forecast
   → Uses ML to forecast demand for next 90 days
   
5. Tool: generate_report
   → Creates detailed restocking plan with:
     - Priority items (critical reorder)
     - Cost estimates (€125,000 total)
     - Timeline (delivery in 14 days)
     - Risk analysis

Result: Professional report ready for procurement team
```

**Difference in Quality:**
- **Prompt response**: "Reorder when low"
- **Agent output**: "Reorder SKU001 (450 units, €2,250), delivery 14 days, improves fill rate 94% → 99%"

---

## Why Agents for CloudEmpiere

### CloudEmpiere's Challenges (That Agents Solve)

1. **Multi-tenant ERP Complexity**
   - Different organizations, different accounting rules
   - ❌ Prompt: Can't understand tenant context
   - ✅ Agent: Filters queries by organization automatically

2. **Real-time Decision Making**
   - Need current inventory, sales, compliance status
   - ❌ Prompt: Limited to knowledge cutoff
   - ✅ Agent: Queries live database

3. **Compliance & Audits**
   - VAT, tax, GL reconciliation critical
   - ❌ Prompt: Can hallucinate numbers
   - ✅ Agent: Verifiable, auditable actions

4. **Complex Workflows**
   - Month-end closing: 50+ steps
   - ❌ Prompt: Can't execute sequentially
   - ✅ Agent: Orchestrates multi-step processes

5. **Integration with iDempiere**
   - Need to read/analyze ERP data
   - ❌ Prompt: Requires manual data export
   - ✅ Agent: Direct database access

---

## Framework Architecture

### System Diagram

```
┌──────────────────────────────────────────────────────┐
│         Your CloudEmpiere SaaS Application           │
├──────────────────────────────────────────────────────┤
│                                                      │
│  REST API Layer                                     │
│  └─ POST /api/agent/execute                         │
│  └─ GET /api/agent/tools                            │
│  └─ GET /api/agent/commands                         │
│                                                      │
├──────────────────────────────────────────────────────┤
│                                                      │
│  Agent Service (Java)                               │
│  ┌─ ClaudeAgent (orchestrator)                      │
│  ┌─ ToolRegistry (manages tools)                    │
│  ┌─ CommandManager (user commands)                  │
│  ┌─ ContextManager (token budgeting)                │
│                                                      │
├──────────────────────────────────────────────────────┤
│                                                      │
│  Tool Layer (Pluggable)                             │
│  ├─ QueryERPDatabaseTool                            │
│  ├─ AnalyzeInventoryTool                            │
│  ├─ GenerateComplianceReportTool                    │
│  ├─ CalculateForecastTool                           │
│  └─ ValidateDocumentsTool                           │
│                                                      │
├──────────────────────────────────────────────────────┤
│                                                      │
│  Integration Layer                                  │
│  ├─ iDempiere Database (PostgreSQL)                 │
│  ├─ Claude API (tool calling)                       │
│  └─ File System (report generation)                 │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### Agent Loop (The Magic)

```
START
  │
  ├─ Receive Goal: "Analyze inventory and create restocking plan"
  │
  ├─ Prepare Context: Load conversation history, available tools
  │
  ├─ LOOP:
  │   │
  │   ├─ Call Claude: "Here are the tools, please achieve this goal"
  │   │
  │   ├─ Claude decides: "I need to use query_inventory tool"
  │   │
  │   ├─ Execute Tool: query_inventory → Get all product stock levels
  │   │
  │   ├─ Send results back to Claude: "Here are the results"
  │   │
  │   ├─ Claude checks: "I need more data to complete the goal"
  │   │
  │   ├─ Execute Tool: analyze_trends → Calculate daily usage
  │   │
  │   ├─ Send results back to Claude: "Here are the results"
  │   │
  │   ├─ Claude checks: "Goal complete, I have everything needed"
  │   │
  │   └─ EXIT LOOP
  │
  ├─ Return Final Response: "Analysis complete, restocking plan created"
  │
END
```

---

## Core Components

### 1. Agent (Orchestrator)

The main entry point that manages the entire workflow.

```java
ClaudeAgent agent = new ClaudeAgent(
    apiKey,           // Your Anthropic API key
    maxContextTokens  // Budget for conversation history (200k recommended)
);

// Register tools
agent.registerTool(new QueryERPDatabaseTool());
agent.registerTool(new AnalyzeInventoryTool());

// Execute
AgentRequest request = AgentRequest.builder()
    .prompt("Analyze inventory and recommend restocking")
    .permissions(Set.of(ToolPermission.READ_ONLY))
    .build();

AgentResponse response = agent.execute(request);
```

### 2. Tools (Extension Points)

Tools are functions Claude can call. Each tool:
- Has a name, description, parameters
- Returns text/data
- Requires specific permissions
- Can query databases, call APIs, generate reports

```java
public class QueryERPDatabaseTool implements Tool {
    
    @Override
    public String getName() {
        return "query_erp_database";
    }
    
    @Override
    public String getDescription() {
        return "Query iDempiere database - SELECT queries only";
    }
    
    @Override
    public Map<String, ToolParameter> getParameters() {
        return Map.of(
            "sql", new ToolParameter("string", "SQL SELECT query", true)
        );
    }
    
    @Override
    public String execute(Map<String, Object> input) throws Exception {
        String sql = (String) input.get("sql");
        // Execute query against iDempiere database
        return resultsAsString;
    }
}
```

### 3. Commands (Workflows)

Pre-defined workflows that orchestrate multiple tools.

```java
public class MonthEndClosingCommand implements Command {
    
    @Override
    public String getName() {
        return "month_end_closing";
    }
    
    @Override
    public CommandResult execute(String[] args, Agent agent) {
        // Orchestrate multi-step month-end process:
        // 1. Validate open transactions
        // 2. Reconcile accounts
        // 3. Generate financial statements
        // 4. Generate compliance reports
        // 5. Lock period
        
        return CommandResult.success("Month-end closing completed");
    }
}
```

### 4. Context Manager

Tracks conversation history, manages token budget, prevents context overflow.

---

## Quick Start Guide

### Step 1: Setup (5 minutes)

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-sdk-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

```java
// Initialize agent
ClaudeAgent agent = new ClaudeAgent(
    System.getenv("ANTHROPIC_API_KEY"),
    200000
);
```

### Step 2: Register a Tool (10 minutes)

```java
// Simple database query tool
agent.registerTool(new QueryERPDatabaseTool(dataSource));
```

### Step 3: Execute (2 minutes)

```java
AgentRequest request = AgentRequest.builder()
    .prompt("What are our top 10 selling products?")
    .permissions(Set.of(ToolPermission.READ_ONLY))
    .build();

AgentResponse response = agent.execute(request);
System.out.println(response.getOutput());
```

### Step 4: Expose via REST API (5 minutes)

```java
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    
    @PostMapping("/execute")
    public AgentResponse execute(@RequestBody AgentRequest request) {
        return agent.execute(request);
    }
}
```

### Step 5: Call from Frontend

```javascript
// React example
const response = await fetch('/api/agent/execute', {
    method: 'POST',
    body: JSON.stringify({
        prompt: "Analyze current inventory levels",
        permissions: ["READ_ONLY"]
    })
});

const data = await response.json();
console.log(data.output);
```

---

## Tool Implementation Examples

### Example 1: Simple Query Tool

```java
public class QueryERPDatabaseTool implements Tool {
    private final JdbcTemplate jdbc;
    
    @Override
    public String getName() { return "query_database"; }
    
    @Override
    public String getDescription() { return "Query iDempiere data"; }
    
    @Override
    public Map<String, ToolParameter> getParameters() {
        return Map.of(
            "sql", new ToolParameter("string", "SQL SELECT", true),
            "org_id", new ToolParameter("string", "Organization filter", false)
        );
    }
    
    @Override
    public String execute(Map<String, Object> input) throws Exception {
        String sql = (String) input.get("sql");
        String orgId = (String) input.get("org_id");
        
        // Add org filter for multi-tenant safety
        if (orgId != null) {
            sql = sql + " AND AD_Org_ID = " + orgId;
        }
        
        // Execute and return formatted results
        return jdbc.queryForList(sql).toString();
    }
    
    @Override
    public ToolPermission getRequiredPermission() {
        return ToolPermission.READ_ONLY;
    }
}
```

### Example 2: Analysis Tool

```java
public class InventoryAnalysisTool implements Tool {
    
    @Override
    public String execute(Map<String, Object> input) throws Exception {
        String warehouseId = (String) input.get("warehouse_id");
        
        // Query current inventory
        List<Product> products = queryInventory(warehouseId);
        
        // Analyze each product
        List<String> recommendations = new ArrayList<>();
        for (Product p : products) {
            double daysOfStock = calculateDaysOfStock(p);
            if (daysOfStock < 7) {
                recommendations.add(
                    String.format("URGENT: Reorder %s - only %d days of stock",
                        p.getName(), (int)daysOfStock)
                );
            }
        }
        
        // Return formatted analysis
        return formatAnalysis(products, recommendations);
    }
}
```

### Example 3: Report Generation Tool

```java
public class GenerateReportTool implements Tool {
    
    @Override
    public String execute(Map<String, Object> input) throws Exception {
        String reportType = (String) input.get("type");
        String startDate = (String) input.get("start_date");
        String endDate = (String) input.get("end_date");
        
        // Generate report data
        ReportData data = generateReportData(reportType, startDate, endDate);
        
        // Save as PDF
        String filePath = generatePDF(data);
        
        return String.format(
            "Report generated successfully: %s\nFile: %s",
            reportType, filePath
        );
    }
}
```

---

## Deployment

### Local Development

```bash
# 1. Set environment variable
export ANTHROPIC_API_KEY=sk-ant-...

# 2. Run Spring Boot application
mvn spring-boot:run

# 3. Test with curl
curl -X POST http://localhost:8080/api/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "List top 10 customers by revenue",
    "permissions": ["READ_ONLY"]
  }'
```

### Docker Deployment

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/agent-*.jar app.jar
ENV ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx2g", "-jar", "app.jar"]
```

```bash
# Build and run
docker build -t cloudempiere-agent .
docker run -e ANTHROPIC_API_KEY=sk-ant-... cloudempiere-agent
```

### Kubernetes Deployment (Production)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cloudempiere-agent
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: agent
        image: cloudempiere/agent:latest
        ports:
        - containerPort: 8080
        env:
        - name: ANTHROPIC_API_KEY
          valueFrom:
            secretKeyRef:
              name: anthropic-secrets
              key: api-key
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
```

---

## Best Practices

### 1. Permission Control

Always use least-privilege permissions:

```java
// Good: Specific permission
AgentRequest.builder()
    .permissions(Set.of(ToolPermission.READ_ONLY))
    .build()

// Bad: Too permissive
AgentRequest.builder()
    .permissions(Set.of(ToolPermission.ADMIN))
    .build()
```

### 2. Query Safety

Always filter by organization for multi-tenant:

```java
String sql = "SELECT * FROM C_Order";
if (orgId != null) {
    sql += " WHERE AD_Org_ID = '" + orgId + "'";
}
```

### 3. Error Handling

Graceful fallbacks:

```java
@Override
public String execute(Map<String, Object> input) throws Exception {
    try {
        return performAction(input);
    } catch (SQLException e) {
        log.error("Database error", e);
        return "Error: Could not complete database query";
    } catch (Exception e) {
        log.error("Unexpected error", e);
        return "Error: Internal system error";
    }
}
```

### 4. Token Management

Monitor and limit token usage per organization:

```java
int estimatedTokens = request.getPrompt().length() / 4;
if (!tokenBudget.hasAvailable(orgId, estimatedTokens)) {
    return AgentResponse.error("Token quota exceeded for this month");
}
```

### 5. Testing

Always test with real ERP data:

```java
@Test
public void testInventoryAnalysis() {
    agent.registerTool(new QueryERPDatabaseTool(testDataSource));
    
    AgentRequest request = AgentRequest.builder()
        .prompt("Analyze warehouse WAR-SK-01 inventory")
        .permissions(Set.of(ToolPermission.READ_ONLY))
        .build();
    
    AgentResponse response = agent.execute(request);
    
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getOutput()).contains("inventory");
}
```

---

## Cost Considerations

### Token Pricing

As of November 2025:
- **Input**: $3 per 1M tokens
- **Output**: $15 per 1M tokens

### Estimation Examples

| Operation | Tokens | Cost |
|-----------|--------|------|
| Simple query | 500 | $0.015 |
| Complex analysis (multi-tool) | 5,000 | $0.15 |
| Full month-end report | 50,000 | $1.50 |

### Cost Optimization

1. **Batch operations** - Group multiple analyses together
2. **Caching** - Store recent analysis results
3. **Summarization** - Compact old conversation history
4. **Quotas** - Set per-organization token limits

---

## Troubleshooting

### Agent Execution Too Slow

**Problem**: Agent takes 30+ seconds

**Solutions**:
1. Reduce max iterations (default 10)
2. Simplify tools (fewer queries per tool)
3. Add database indexes to frequently queried tables
4. Use async execution for non-critical tasks

### Agent Makes Wrong Tool Calls

**Problem**: Claude uses wrong tool or passes wrong parameters

**Solutions**:
1. Improve tool descriptions (be more specific)
2. Add examples to system prompt
3. Validate parameters before execution
4. Log rejected tool calls for analysis

### Permission Errors

**Problem**: "Permission denied for tool"

**Solutions**:
1. Increase request permission level
2. Check organization filtering
3. Verify user role in CloudEmpiere
4. Review audit log

### High Token Usage

**Problem**: Agent uses too many tokens

**Solutions**:
1. Reduce conversation history depth
2. Compact older messages
3. Limit tool descriptions
4. Use shorter system prompts

---

## Roadmap & Future Enhancements

### Phase 1 (Current)
- ✅ Basic agent framework
- ✅ Database query tool
- ✅ Simple analysis tools
- ✅ REST API

### Phase 2 (Next)
- ⏳ Audit trail tracking
- ⏳ Advanced caching
- ⏳ ML-based forecasting
- ⏳ Webhook notifications

### Phase 3 (Future)
- 🔮 Multi-agent coordination
- 🔮 Custom DSL for workflows
- 🔮 Computer vision for document processing
- 🔮 Real-time streaming results

---

## Resources

### Official Documentation
- [Anthropic Claude API](https://docs.anthropic.com)
- [Claude Models & Pricing](https://www.anthropic.com/pricing)
- [iDempiere Documentation](https://wiki.idempiere.org)

### Related Files (Included)
1. `ClaudeAgentImplementation.java` - Full source code
2. `CloudEmpiere-Integration-Guide.md` - Detailed integration guide
3. `JavaAgentFramework.md` - Architecture deep dive

### Community
- Anthropic Discord: https://discord.gg/anthropic
- iDempiere Forum: https://idempiere.discourse.group
- CloudEmpiere Team: team@cloudempiere.sk

---

## FAQ

**Q: Can agents access external APIs?**  
A: Yes, via custom tools. Implement a tool that calls your API and return results.

**Q: How do I ensure audit compliance?**  
A: Log all tool executions with timestamps, user, organization, and results.

**Q: What about sensitive data?**  
A: Never expose passwords/keys in prompts. Use org filtering and encrypted connections.

**Q: Can I use this in a multi-tenant SaaS?**  
A: Yes, filter all queries by org_id. Each agent instance can handle multiple tenants.

**Q: How do I handle tool timeouts?**  
A: Set query timeouts (e.g., 30 seconds) and max iterations to prevent hangs.

**Q: Can I use offline/local Claude?**  
A: Not currently, you need Anthropic API key. But you can use offline LLMs by modifying the API client.

---

## Support & Feedback

For questions about:
- **Framework design**: Refer to `JavaAgentFramework.md`
- **Integration**: Refer to `CloudEmpiere-Integration-Guide.md`
- **Implementation**: Refer to `ClaudeAgentImplementation.java`
- **CloudEmpiere specifics**: Contact team@cloudempiere.sk

---

## License & Terms

This framework is designed for CloudEmpiere's iDempiere implementations.

**Important**: 
- Requires valid Anthropic API key
- Assumes PostgreSQL iDempiere database
- Multi-tenant filtering is mandatory
- Always test in staging before production

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Nov 2025 | Initial release |

---

**Last Updated**: November 26, 2025  
**Maintained By**: CloudEmpiere Development Team

---

## Quick Reference: Agent vs Prompt

### When to Use a Prompt ✅
- Quick questions with short answers
- Brainstorming and ideation
- Text analysis or summarization
- Content generation
- One-off requests without state

**Example**: "Write a product description for Product XYZ"

### When to Use an Agent ✅
- Complex, multi-step workflows
- Need real-time data from your systems
- Automated decision-making
- Report generation
- Compliance and audit tasks

**Example**: "Analyze inventory, calculate reorder quantities, generate procurement report, notify suppliers"

### The Key Difference

```
PROMPT (Simple)
Question → Claude → Answer (Done)

AGENT (Powerful)
Goal → Planning → Tool Calls → Results Evaluation → Iteration → Final Answer
```

---

**Ready to get started? See the Quick Start Guide section above or review the included Java implementation files.**
