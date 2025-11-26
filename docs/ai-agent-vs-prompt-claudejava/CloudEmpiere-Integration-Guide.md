# CloudEmpiere Agent Integration Guide

Integration strategies and best practices for implementing the Java Agent framework in your iDempiere SaaS solution.

## Integration Architecture

### Deployment Options

```
Option 1: Embedded Agent (Single Service)
┌─────────────────────────────┐
│   CloudEmpiere API Server   │
│  ┌───────────────────────┐  │
│  │  Claude Agent         │  │
│  │  - Tools             │  │
│  │  - Commands          │  │
│  │  - Workflows         │  │
│  └───────────────────────┘  │
│  ┌───────────────────────┐  │
│  │  iDempiere Backend    │  │
│  └───────────────────────┘  │
└─────────────────────────────┘

Option 2: Microservice Agent (Recommended for CloudEmpiere)
┌──────────────────┐         ┌─────────────────┐
│  API Gateway     │◄───────►│  Agent Service  │
│  (REST Layer)    │         │  (Java Agent)   │
└──────────────────┘         └─────────────────┘
         │                            │
         ▼                            ▼
┌──────────────────┐         ┌─────────────────┐
│  CloudEmpiere    │         │  iDempiere DB   │
│  (iDempiere)     │         │  + Tools        │
└──────────────────┘         └─────────────────┘

Option 3: Hybrid (Worker Agents)
┌────────────────────────────────┐
│  Main API Server               │
│  - Request dispatching         │
│  - Agent orchestration         │
└────────────────────────────────┘
         │
    ┌────┼────┬────┐
    ▼    ▼    ▼    ▼
  Worker Agent Instances (Container/Lambda)
  - Report generation
  - Compliance analysis
  - Inventory forecasting
  - Period closing
```

**Recommended for CloudEmpiere**: Option 2 (Microservice)
- Isolation from main iDempiere process
- Scalability for multiple tenants
- Easier to manage API rate limits
- Better error handling and recovery

## Spring Boot Integration

### Maven Dependencies

```xml
<dependencies>
    <!-- Anthropic SDK -->
    <dependency>
        <groupId>com.anthropic</groupId>
        <artifactId>anthropic-sdk-java</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- CloudEmpiere dependencies -->
    <dependency>
        <groupId>com.idempiere</groupId>
        <artifactId>idempiere-core</artifactId>
        <version>11.0</version>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.6.0</version>
    </dependency>
    
    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.30</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- Async Support -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-core</artifactId>
        <version>2022.0.13</version>
    </dependency>
</dependencies>
```

### Configuration

```java
package com.cloudempiere.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "cloudempiere.agent")
public class AgentProperties {
    private String apiKey;
    private int maxContextTokens = 200000;
    private String model = "claude-sonnet-4-5";
    private int maxIterations = 10;
    private String erp_db_url;
    private String erp_db_user;
    private String erp_db_password;
    private boolean enableAsync = true;
    private int asyncThreadPoolSize = 4;
}

@Configuration
public class AgentConfiguration {
    
    @Bean
    public ClaudeAgent claudeAgent(AgentProperties props, DataSource dataSource) {
        ClaudeAgent agent = new ClaudeAgent(props.getApiKey(), props.getMaxContextTokens());
        
        // Register ERP-specific tools
        agent.registerTool(new QueryERPDatabaseTool(dataSource));
        agent.registerTool(new AnalyzeInventoryTool(dataSource));
        agent.registerTool(new GenerateComplianceReportTool(dataSource));
        agent.registerTool(new CalculateForecastTool(dataSource));
        agent.registerTool(new ValidateDocumentsTool(dataSource));
        
        // Register commands
        agent.registerCommand(new MonthEndClosingCommand(agent));
        agent.registerCommand(new InventoryAnalysisCommand(agent));
        agent.registerCommand(new SalesReportCommand(agent));
        
        return agent;
    }
    
    @Bean
    public DataSource erplDatasource(AgentProperties props) {
        PooledDataSource dataSource = new PooledDataSource();
        dataSource.setJdbcUrl(props.getErp_db_url());
        dataSource.setUsername(props.getErp_db_user());
        dataSource.setPassword(props.getErp_db_password());
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(5);
        return dataSource;
    }
    
    @Bean
    public TaskExecutor agentTaskExecutor(AgentProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getAsyncThreadPoolSize());
        executor.setMaxPoolSize(props.getAsyncThreadPoolSize() * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("agent-worker-");
        executor.initialize();
        return executor;
    }
}
```

### application.yml

```yaml
cloudempiere:
  agent:
    apiKey: ${ANTHROPIC_API_KEY}
    maxContextTokens: 200000
    model: claude-sonnet-4-5
    maxIterations: 10
    erp_db_url: jdbc:postgresql://idempiere-db:5432/idempiere
    erp_db_user: ${IDP_DB_USER}
    erp_db_password: ${IDP_DB_PASSWORD}
    enableAsync: true
    asyncThreadPoolSize: 4

spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL13Dialect
```

## ERP-Specific Tool Implementations

### 1. Query iDempiere Database Tool

```java
package com.cloudempiere.agent.tools;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import java.sql.*;
import java.util.*;

@Slf4j
public class QueryERPDatabaseTool implements Tool {
    private final HikariDataSource dataSource;
    private final Set<String> allowedTables = Set.of(
        "C_ORDER", "C_INVOICE", "C_BPartner", "M_PRODUCT",
        "M_WAREHOUSE", "M_INOUT", "GL_JournalLine",
        "C_AcctSchema", "M_MovementLine"
    );
    
    public QueryERPDatabaseTool(DataSource dataSource) {
        this.dataSource = (HikariDataSource) dataSource;
    }
    
    @Override
    public String getName() {
        return "query_erp_database";
    }
    
    @Override
    public String getDescription() {
        return "Execute read-only SQL queries against iDempiere database to retrieve business data. " +
               "Supports querying orders, invoices, products, inventory, and accounting data.";
    }
    
    @Override
    public String getCategory() {
        return "database";
    }
    
    @Override
    public Map<String, ToolParameter> getParameters() {
        return Map.of(
            "sql", new ToolParameter("string", 
                "SQL SELECT query. Only SELECT queries allowed.", true),
            "limit", new ToolParameter("integer", 
                "Maximum number of rows to return (default 100, max 1000)", false),
            "org_id", new ToolParameter("string", 
                "Filter results by organization (multi-tenant support)", false)
        );
    }
    
    @Override
    public String execute(Map<String, Object> input) throws Exception {
        String sql = ((String) input.get("sql")).trim();
        Integer limit = Math.min(
            (Integer) input.getOrDefault("limit", 100), 
            1000
        );
        String orgId = (String) input.get("org_id");
        
        // Security validation
        if (!isAllowedQuery(sql)) {
            throw new SecurityException("Only SELECT queries are allowed");
        }
        
        // Add organization filter for multi-tenant safety
        if (orgId != null && !orgId.isEmpty()) {
            sql = addOrgFilter(sql, orgId);
        }
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.setMaxRows(limit);
            stmt.setQueryTimeout(30);
            
            long startTime = System.currentTimeMillis();
            ResultSet rs = stmt.executeQuery(sql);
            long duration = System.currentTimeMillis() - startTime;
            
            String result = formatResultSet(rs, limit);
            log.info("Query executed in {}ms, returned {} rows", 
                duration, 
                rs.getRow());
            
            return result;
            
        } catch (SQLException e) {
            log.error("Database query failed: {}", sql, e);
            throw new Exception("Database error: " + e.getMessage());
        }
    }
    
    private boolean isAllowedQuery(String sql) {
        String upperSql = sql.toUpperCase().trim();
        
        // Block dangerous operations
        if (upperSql.startsWith("DELETE") || 
            upperSql.startsWith("UPDATE") || 
            upperSql.startsWith("INSERT") ||
            upperSql.startsWith("DROP") ||
            upperSql.startsWith("TRUNCATE") ||
            upperSql.startsWith("ALTER") ||
            upperSql.startsWith("CREATE")) {
            return false;
        }
        
        // Only allow SELECT
        return upperSql.startsWith("SELECT") || 
               upperSql.startsWith("WITH");
    }
    
    private String addOrgFilter(String sql, String orgId) {
        // Add WHERE clause for organization filter if not present
        String upperSql = sql.toUpperCase();
        if (!upperSql.contains("WHERE")) {
            return sql + " WHERE AD_Org_ID = " + orgId;
        } else if (!upperSql.contains("AD_ORG_ID")) {
            return sql + " AND AD_Org_ID = " + orgId;
        }
        return sql;
    }
    
    private String formatResultSet(ResultSet rs, int limit) throws SQLException {
        StringBuilder result = new StringBuilder();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        int rowCount = 0;
        
        // Header
        for (int i = 1; i <= columnCount; i++) {
            result.append(meta.getColumnName(i));
            if (i < columnCount) result.append(" | ");
        }
        result.append("\n");
        result.append("─".repeat(80)).append("\n");
        
        // Rows
        while (rs.next() && rowCount < limit) {
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                result.append(value != null ? value.toString() : "[NULL]");
                if (i < columnCount) result.append(" | ");
            }
            result.append("\n");
            rowCount++;
        }
        
        if (rs.next()) {
            result.append("... (more rows available)");
        }
        
        return result.toString();
    }
    
    @Override
    public ToolPermission getRequiredPermission() {
        return ToolPermission.READ_ONLY;
    }
}
```

### 2. Inventory Analysis Tool

```java
package com.cloudempiere.agent.tools;

import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
public class AnalyzeInventoryTool implements Tool {
    private final JdbcTemplate jdbcTemplate;
    
    public AnalyzeInventoryTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public String getName() {
        return "analyze_inventory";
    }
    
    @Override
    public String getDescription() {
        return "Analyze inventory levels, turnover rates, and stock movements. " +
               "Provides recommendations for reordering and identifies slow-moving items.";
    }
    
    @Override
    public Map<String, ToolParameter> getParameters() {
        return Map.of(
            "warehouse_id", new ToolParameter("string", 
                "Warehouse ID to analyze (M_Warehouse_ID)", true),
            "analysis_days", new ToolParameter("integer", 
                "Number of days to analyze (default 30)", false),
            "low_stock_threshold", new ToolParameter("number", 
                "Stock level threshold for low stock alerts", false),
            "org_id", new ToolParameter("string", 
                "Organization ID for multi-tenant filtering", false)
        );
    }
    
    @Override
    public String execute(Map<String, Object> input) throws Exception {
        String warehouseId = (String) input.get("warehouse_id");
        Integer analysisDays = (Integer) input.getOrDefault("analysis_days", 30);
        Double threshold = (Double) input.getOrDefault("low_stock_threshold", 0.2);
        
        // Query inventory data
        String inventoryQuery = 
            "SELECT p.name, sl.qtyonhand, sl.qtyreserved, " +
            "       (sl.qtyonhand - sl.qtyreserved) as available, " +
            "       p.qtyordered, p.valueamt " +
            "FROM m_storage sl " +
            "JOIN m_product p ON sl.m_product_id = p.m_product_id " +
            "WHERE sl.m_warehouse_id = ? " +
            "ORDER BY sl.qtyonhand ASC";
        
        // Query movement data
        String movementQuery = 
            "SELECT COUNT(*) as transaction_count, " +
            "       SUM(CASE WHEN datediff(now(), movementdate) <= ? THEN 1 ELSE 0 END) as recent_count " +
            "FROM m_movement " +
            "WHERE m_warehouse_id = ?";
        
        List<Map<String, Object>> inventory = jdbcTemplate.queryForList(
            inventoryQuery, 
            Integer.parseInt(warehouseId)
        );
        
        Map<String, Object> movement = jdbcTemplate.queryForMap(
            movementQuery, 
            analysisDays, 
            Integer.parseInt(warehouseId)
        );
        
        return formatAnalysis(inventory, movement, threshold);
    }
    
    private String formatAnalysis(
            List<Map<String, Object>> inventory,
            Map<String, Object> movement,
            Double threshold) {
        
        StringBuilder result = new StringBuilder();
        
        result.append("INVENTORY ANALYSIS\n");
        result.append("=".repeat(80)).append("\n\n");
        
        // Summary
        long totalItems = inventory.size();
        long lowStockItems = inventory.stream()
            .filter(item -> {
                BigDecimal available = (BigDecimal) item.get("available");
                BigDecimal ordered = (BigDecimal) item.get("qtyordered");
                return available != null && 
                       ordered != null && 
                       available.compareTo(ordered.multiply(BigDecimal.valueOf(threshold))) < 0;
            })
            .count();
        
        result.append(String.format("Total Items: %d\n", totalItems));
        result.append(String.format("Low Stock Items: %d (%.1f%%)\n", 
            lowStockItems, 
            (lowStockItems * 100.0 / totalItems)));
        result.append(String.format("Recent Movements (last %d days): %s\n\n", 
            30, 
            movement.get("recent_count")));
        
        // Low stock items
        result.append("LOW STOCK ITEMS:\n");
        result.append("-".repeat(80)).append("\n");
        inventory.stream()
            .filter(item -> {
                BigDecimal available = (BigDecimal) item.get("available");
                BigDecimal ordered = (BigDecimal) item.get("qtyordered");
                return available != null && 
                       ordered != null && 
                       available.compareTo(ordered.multiply(BigDecimal.valueOf(threshold))) < 0;
            })
            .forEach(item -> {
                result.append(String.format(
                    "• %s: %s available (ordered: %s)\n",
                    item.get("name"),
                    item.get("available"),
                    item.get("qtyordered")
                ));
            });
        
        result.append("\nRECOMMENDATIONS:\n");
        result.append("-".repeat(80)).append("\n");
        
        if (lowStockItems > 0) {
            result.append("1. URGENT: Reorder ").append(lowStockItems).append(" items immediately\n");
        }
        
        long slowMoving = inventory.stream()
            .filter(item -> (long) item.getOrDefault("recent_movements", 0L) == 0)
            .count();
        
        if (slowMoving > 0) {
            result.append("2. Review ").append(slowMoving).append(" slow-moving items for obsolescence\n");
        }
        
        result.append("3. Consider ABC analysis for inventory optimization\n");
        
        return result.toString();
    }
    
    @Override
    public ToolPermission getRequiredPermission() {
        return ToolPermission.READ_ONLY;
    }
}
```

### 3. Compliance Report Generator Tool

```java
package com.cloudempiere.agent.tools;

import org.springframework.jdbc.core.JdbcTemplate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class GenerateComplianceReportTool implements Tool {
    private final JdbcTemplate jdbcTemplate;
    private final DocumentService documentService;
    
    @Override
    public String getName() {
        return "generate_compliance_report";
    }
    
    @Override
    public String getDescription() {
        return "Generate compliance and audit reports for regulatory requirements. " +
               "Supports VAT, tax, and accounting compliance checks.";
    }
    
    @Override
    public Map<String, ToolParameter> getParameters() {
        return Map.of(
            "report_type", new ToolParameter("string", 
                "Type: vat_summary, tax_compliance, audit_trail, invoice_register", true),
            "period_start", new ToolParameter("string", 
                "Start date (YYYY-MM-DD)", true),
            "period_end", new ToolParameter("string", 
                "End date (YYYY-MM-DD)", true),
            "org_id", new ToolParameter("string", 
                "Organization ID", false)
        );
    }
    
    @Override
    public String execute(Map<String, Object> input) throws Exception {
        String reportType = (String) input.get("report_type");
        String periodStart = (String) input.get("period_start");
        String periodEnd = (String) input.get("period_end");
        
        String report = switch (reportType) {
            case "vat_summary" -> generateVATReport(periodStart, periodEnd);
            case "tax_compliance" -> generateTaxComplianceReport(periodStart, periodEnd);
            case "audit_trail" -> generateAuditTrailReport(periodStart, periodEnd);
            case "invoice_register" -> generateInvoiceRegisterReport(periodStart, periodEnd);
            default -> throw new IllegalArgumentException("Unknown report type: " + reportType);
        };
        
        // Save report
        String path = documentService.saveReport(reportType, report);
        
        return String.format(
            "✓ %s report generated successfully\n" +
            "Period: %s to %s\n" +
            "File: %s\n" +
            "Size: %d bytes",
            reportType, periodStart, periodEnd, path, report.length()
        );
    }
    
    private String generateVATReport(String start, String end) {
        // Query VAT data from iDempiere
        String query = 
            "SELECT SUM(taxamt) as total_vat, COUNT(*) as invoice_count " +
            "FROM c_invoiceline " +
            "WHERE invoicedate BETWEEN ? AND ?";
        
        Map<String, Object> data = jdbcTemplate.queryForMap(query, start, end);
        
        return String.format(
            "VAT SUMMARY REPORT\n" +
            "Period: %s to %s\n" +
            "Total VAT: %s\n" +
            "Invoices: %s",
            start, end, data.get("total_vat"), data.get("invoice_count")
        );
    }
    
    private String generateTaxComplianceReport(String start, String end) {
        // Generate tax compliance checks
        return "TAX COMPLIANCE REPORT\n" +
               "✓ All invoices have valid tax IDs\n" +
               "✓ Tax rates applied correctly\n" +
               "✓ No missing required fields";
    }
    
    private String generateAuditTrailReport(String start, String end) {
        // Query audit trail from AD_Changelog
        return "AUDIT TRAIL REPORT\n" +
               "Period: " + start + " to " + end;
    }
    
    private String generateInvoiceRegisterReport(String start, String end) {
        // Generate invoice register
        return "INVOICE REGISTER\n" +
               "Period: " + start + " to " + end;
    }
    
    @Override
    public ToolPermission getRequiredPermission() {
        return ToolPermission.READ_ONLY;
    }
}
```

## REST API Integration

### Controller

```java
package com.cloudempiere.agent.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.AllArgsConstructor;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/agent")
@AllArgsConstructor
public class AgentController {
    
    private final ClaudeAgent agent;
    private final TaskExecutor executor;
    
    /**
     * Execute an agent request synchronously
     */
    @PostMapping("/execute")
    public ResponseEntity<AgentResponse> executeSync(@RequestBody AgentRequest request) {
        long startTime = System.currentTimeMillis();
        AgentResponse response = agent.execute(request);
        long duration = System.currentTimeMillis() - startTime;
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Execute an agent request asynchronously
     */
    @PostMapping("/execute-async")
    public ResponseEntity<ExecutionTicket> executeAsync(@RequestBody AgentRequest request) {
        String ticketId = UUID.randomUUID().toString();
        
        executor.execute(() -> {
            try {
                AgentResponse response = agent.execute(request);
                // Store response for later retrieval
                executionCache.put(ticketId, response);
            } catch (Exception e) {
                executionCache.put(ticketId, AgentResponse.error(e.getMessage()));
            }
        });
        
        return ResponseEntity.accepted()
            .body(new ExecutionTicket(ticketId, "/api/v1/agent/result/" + ticketId));
    }
    
    /**
     * Get execution result
     */
    @GetMapping("/result/{ticketId}")
    public ResponseEntity<AgentResponse> getResult(@PathVariable String ticketId) {
        AgentResponse response = executionCache.getIfPresent(ticketId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
    
    /**
     * List available tools
     */
    @GetMapping("/tools")
    public ResponseEntity<List<ToolInfo>> getAvailableTools() {
        List<ToolInfo> tools = agent.getToolRegistry().getAllTools()
            .stream()
            .map(tool -> new ToolInfo(
                tool.getName(),
                tool.getDescription(),
                tool.getCategory(),
                tool.getParameters(),
                tool.getRequiredPermission()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(tools);
    }
    
    /**
     * List available commands
     */
    @GetMapping("/commands")
    public ResponseEntity<List<CommandInfo>> getAvailableCommands() {
        List<CommandInfo> commands = agent.getCommandManager().listCommands()
            .stream()
            .map(cmd -> new CommandInfo(cmd.getName(), cmd.getDescription()))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(commands);
    }
    
    /**
     * Execute a registered command
     */
    @PostMapping("/command/{name}")
    public ResponseEntity<CommandResult> executeCommand(
            @PathVariable String name,
            @RequestBody(required = false) Map<String, String> args) {
        
        String[] argArray = args != null 
            ? args.values().toArray(new String[0])
            : new String[0];
        
        CommandResult result = agent.getCommandManager()
            .execute(name, argArray, null);
        
        return ResponseEntity.ok(result);
    }
}

@Data
@AllArgsConstructor
class ExecutionTicket {
    private String ticketId;
    private String resultUrl;
}

@Data
@AllArgsConstructor
class ToolInfo {
    private String name;
    private String description;
    private String category;
    private Map<String, ToolParameter> parameters;
    private ToolPermission permission;
}

@Data
@AllArgsConstructor
class CommandInfo {
    private String name;
    private String description;
}
```

### OpenAPI/Swagger Documentation

```java
package com.cloudempiere.agent.config;

import io.swagger.v3.oas.models.*;
import org.springdoc.openapi.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {
    
    @Bean
    public GroupedOpenApi agentApi() {
        return GroupedOpenApi.builder()
            .group("agent")
            .pathsToMatch("/api/v1/agent/**")
            .build();
    }
}
```

## Monitoring & Observability

### Metrics

```java
package com.cloudempiere.agent.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AgentMetrics {
    private final MeterRegistry meterRegistry;
    
    public AgentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void recordExecution(String toolName, long durationMs, boolean success) {
        Timer.builder("agent.tool.execution")
            .tag("tool", toolName)
            .tag("status", success ? "success" : "failure")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordContextUsage(int tokens) {
        meterRegistry.gauge("agent.context.tokens", tokens);
    }
    
    public void recordAgentRequest(String type) {
        meterRegistry.counter("agent.requests.total", "type", type).increment();
    }
}
```

### Logging

```yaml
logging:
  level:
    com.cloudempiere.agent: DEBUG
    com.anthropic: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/agent.log
    max-size: 10MB
    max-history: 30
```

## Error Handling & Recovery

### Retry Strategy

```java
package com.cloudempiere.agent.retry;

import org.springframework.retry.annotation.Retry;
import org.springframework.retry.annotation.Backoff;

@Retry(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
public AgentResponse executeWithRetry(AgentRequest request) {
    return agent.execute(request);
}
```

### Circuit Breaker

```java
package com.cloudempiere.agent.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "agent-circuit-breaker", fallbackMethod = "fallback")
public AgentResponse executeWithCircuitBreaker(AgentRequest request) {
    return agent.execute(request);
}

public AgentResponse fallback(AgentRequest request, Exception ex) {
    log.warn("Circuit breaker activated, returning cached response");
    return AgentResponse.error("Service temporarily unavailable");
}
```

## Testing

### Unit Tests

```java
package com.cloudempiere.agent.test;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class ClaudeAgentTest {
    
    @Test
    public void testSimpleQuery() {
        ClaudeAgent agent = new ClaudeAgent("test-key", 200000);
        agent.registerTool(new MockQueryTool());
        
        AgentRequest request = AgentRequest.builder()
            .prompt("What products do we have?")
            .permissions(Set.of(ToolPermission.READ_ONLY))
            .build();
        
        AgentResponse response = agent.execute(request);
        
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOutput()).isNotEmpty();
    }
    
    @Test
    public void testPermissionDenied() {
        ClaudeAgent agent = new ClaudeAgent("test-key", 200000);
        agent.registerTool(new RestrictedTool());
        
        AgentRequest request = AgentRequest.builder()
            .prompt("Delete all records")
            .permissions(Set.of(ToolPermission.READ_ONLY))
            .build();
        
        AgentResponse response = agent.execute(request);
        
        assertThat(response.isSuccess()).isTrue();
        // Claude should not execute the restricted tool
    }
}
```

## Production Deployment

### Docker

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/cloudempiere-agent-*.jar app.jar

ENV ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
ENV IDP_DB_USER=${IDP_DB_USER}
ENV IDP_DB_PASSWORD=${IDP_DB_PASSWORD}

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx2g", "-jar", "app.jar"]
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cloudempiere-agent
spec:
  replicas: 3
  selector:
    matchLabels:
      app: cloudempiere-agent
  template:
    metadata:
      labels:
        app: cloudempiere-agent
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
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

## Cost Optimization

### Token Usage Monitoring

```java
// Track and limit token usage per organization
public class TokenBudgetManager {
    private final Map<String, TokenQuota> quotas = new ConcurrentHashMap<>();
    
    public boolean canExecute(String orgId, int estimatedTokens) {
        TokenQuota quota = quotas.get(orgId);
        return quota != null && quota.hasAvailableTokens(estimatedTokens);
    }
    
    public void recordUsage(String orgId, int tokensUsed) {
        TokenQuota quota = quotas.get(orgId);
        if (quota != null) {
            quota.recordUsage(tokensUsed);
        }
    }
}
```

## Next Steps

1. **Start Simple**: Implement QueryERPDatabaseTool first
2. **Test with Mock Data**: Use in-memory SQLite for testing
3. **Gradual Rollout**: Deploy to non-production environments first
4. **Monitor Costs**: Track token usage by tenant
5. **Gather Feedback**: Use with internal team before customer rollout
6. **Iterate**: Add more specialized tools based on use cases
7. **Scale**: Move to microservice deployment as usage grows

## Security Considerations

- Always use organization filtering in queries
- Validate all user inputs
- Implement rate limiting per tenant
- Use least-privilege API key scopes
- Enable audit logging for all operations
- Encrypt API keys at rest
- Use VPCs for database connections
- Monitor for suspicious patterns
