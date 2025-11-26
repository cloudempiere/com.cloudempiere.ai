# Java Agent Framework Design

A production-ready agent framework for Java applications that integrates with Claude API, supporting commands, tasks, and workflows without the Agent SDK.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     AgentOrchestrator                        │
│  (Manages agent lifecycle, context, state)                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
    ┌───▼──┐     ┌────▼────┐    ┌───▼──┐
    │Agent │     │Command  │    │Task  │
    │Loop  │     │Manager  │    │Queue │
    └──────┘     └─────────┘    └──────┘
        │              │              │
        └──────────────┼──────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
    ┌───▼──────┐  ┌───▼────┐  ┌────▼────┐
    │Tool      │  │Claude  │  │Context  │
    │Registry  │  │API     │  │Manager  │
    └──────────┘  │Client  │  └─────────┘
                  └────────┘
```

## Core Components

### 1. Agent Interface & Base Implementation

```java
// Core agent abstraction
public interface Agent {
    AgentResponse execute(AgentRequest request);
    void registerCommand(Command command);
    void registerTool(Tool tool);
    AgentState getState();
}

public class ClaudeAgent implements Agent {
    private final ClaudeApiClient apiClient;
    private final CommandManager commandManager;
    private final ToolRegistry toolRegistry;
    private final ContextManager contextManager;
    private final AgentState state;
    
    public ClaudeAgent(ClaudeAgentConfig config) {
        this.apiClient = new ClaudeApiClient(config.getApiKey());
        this.commandManager = new CommandManager();
        this.toolRegistry = new ToolRegistry();
        this.contextManager = new ContextManager(config.getMaxContextTokens());
        this.state = new AgentState();
    }
    
    @Override
    public AgentResponse execute(AgentRequest request) {
        try {
            // 1. Validate and prepare context
            AgentContext context = contextManager.prepareContext(
                request, 
                state.getConversationHistory()
            );
            
            // 2. Call Claude with tools
            ClaudeMessage response = callClaudeWithTools(context);
            
            // 3. Process tool calls in a loop
            return processAgentLoop(response, request, context);
            
        } catch (Exception e) {
            return AgentResponse.error("Agent execution failed: " + e.getMessage());
        }
    }
    
    private AgentResponse processAgentLoop(
        ClaudeMessage response, 
        AgentRequest request,
        AgentContext context) {
        
        StringBuilder finalResponse = new StringBuilder();
        int iterations = 0;
        int maxIterations = 10; // Prevent infinite loops
        
        while (iterations < maxIterations) {
            // Check if Claude wants to use tools
            List<ToolUse> toolCalls = extractToolCalls(response);
            
            if (toolCalls.isEmpty()) {
                // No more tool calls, Claude is done
                finalResponse.append(response.getContent());
                break;
            }
            
            // Execute all tool calls
            List<ToolResult> results = new ArrayList<>();
            for (ToolUse toolCall : toolCalls) {
                ToolResult result = executeTool(toolCall, request);
                results.add(result);
                finalResponse.append(String.format(
                    "Tool: %s\nResult: %s\n\n", 
                    toolCall.getName(), 
                    result.getOutput()
                ));
            }
            
            // Send tool results back to Claude
            response = callClaudeWithToolResults(
                context, 
                response, 
                results
            );
            
            iterations++;
        }
        
        // Update state and return
        state.addToHistory(request.getPrompt(), finalResponse.toString());
        return AgentResponse.success(finalResponse.toString());
    }
    
    private ToolResult executeTool(ToolUse toolCall, AgentRequest request) {
        Tool tool = toolRegistry.getTool(toolCall.getName());
        
        if (tool == null) {
            return ToolResult.error("Tool not found: " + toolCall.getName());
        }
        
        // Check permissions
        if (!request.hasPermission(tool.getRequiredPermission())) {
            return ToolResult.error("Permission denied for tool: " + toolCall.getName());
        }
        
        try {
            String result = tool.execute(toolCall.getInput());
            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }
    
    private ClaudeMessage callClaudeWithTools(AgentContext context) {
        List<Tool> availableTools = toolRegistry.getAvailableTools(
            context.getRequest().getPermissions()
        );
        
        ClaudeRequest claudeRequest = ClaudeRequest.builder()
            .model("claude-sonnet-4-5")
            .maxTokens(4096)
            .systemPrompt(context.getSystemPrompt())
            .messages(context.getMessages())
            .tools(availableTools.stream()
                .map(Tool::toClaudeToolDefinition)
                .collect(Collectors.toList()))
            .build();
        
        return apiClient.createMessage(claudeRequest);
    }
    
    private ClaudeMessage callClaudeWithToolResults(
        AgentContext context,
        ClaudeMessage previousResponse,
        List<ToolResult> toolResults) {
        
        // Add assistant response with tool calls
        context.addMessage(Message.assistant(previousResponse.getContent()));
        
        // Add tool results
        for (ToolResult result : toolResults) {
            context.addMessage(Message.toolResult(
                result.getToolName(),
                result.getOutput(),
                result.isSuccess()
            ));
        }
        
        return callClaudeWithTools(context);
    }
    
    @Override
    public void registerCommand(Command command) {
        commandManager.register(command);
    }
    
    @Override
    public void registerTool(Tool tool) {
        toolRegistry.register(tool);
    }
    
    @Override
    public AgentState getState() {
        return state;
    }
}
```

### 2. Command System

```java
// Command interface - for user-defined actions
public interface Command {
    String getName();
    String getDescription();
    CommandResult execute(String[] args, AgentContext context);
}

public class CommandManager {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();
    
    public void register(Command command) {
        commands.put(command.getName(), command);
    }
    
    public CommandResult execute(String commandName, String[] args, AgentContext context) {
        Command command = commands.get(commandName);
        if (command == null) {
            return CommandResult.error("Command not found: " + commandName);
        }
        return command.execute(args, context);
    }
    
    public List<Command> listCommands() {
        return new ArrayList<>(commands.values());
    }
}

public class CommandResult {
    private final boolean success;
    private final String output;
    private final Map<String, Object> data;
    
    public static CommandResult success(String output) {
        return new CommandResult(true, output, Collections.emptyMap());
    }
    
    public static CommandResult error(String error) {
        return new CommandResult(false, error, Collections.emptyMap());
    }
    
    public static CommandResult withData(String output, Map<String, Object> data) {
        return new CommandResult(true, output, data);
    }
}
```

### 3. Tool Registry & Tool Interface

```java
// Tool interface - for Claude to call
public interface Tool {
    String getName();
    String getDescription();
    String getCategory();
    Map<String, ToolParameter> getParameters();
    String execute(Map<String, Object> input) throws Exception;
    ToolPermission getRequiredPermission();
    
    default ToolDefinition toClaudeToolDefinition() {
        return ToolDefinition.builder()
            .name(getName())
            .description(getDescription())
            .inputSchema(buildJsonSchema())
            .build();
    }
    
    private JsonSchema buildJsonSchema() {
        // Convert parameters to JSON schema for Claude
        // Implementation details...
        return new JsonSchema();
    }
}

public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final Map<String, ToolExecutionStats> stats = new ConcurrentHashMap<>();
    
    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
        stats.put(tool.getName(), new ToolExecutionStats());
    }
    
    public Tool getTool(String name) {
        return tools.get(name);
    }
    
    public List<Tool> getAvailableTools(Set<ToolPermission> permissions) {
        return tools.values().stream()
            .filter(tool -> permissions.contains(tool.getRequiredPermission()))
            .collect(Collectors.toList());
    }
    
    public ToolExecutionStats getStats(String toolName) {
        return stats.get(toolName);
    }
}

public enum ToolPermission {
    READ_ONLY,
    READ_WRITE,
    EXECUTE,
    ADMIN
}
```

### 4. Task & Workflow System

```java
// Task represents a unit of work
public interface Task {
    String getId();
    String getName();
    TaskStatus getStatus();
    void execute(Agent agent);
    List<String> getDependencies();
}

public abstract class AbstractTask implements Task {
    protected final String id;
    protected final String name;
    protected TaskStatus status;
    protected final List<String> dependencies;
    protected TaskResult result;
    
    protected AbstractTask(String name, List<String> dependencies) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.status = TaskStatus.PENDING;
        this.dependencies = dependencies;
    }
    
    @Override
    public final void execute(Agent agent) {
        try {
            status = TaskStatus.RUNNING;
            result = executeTask(agent);
            status = TaskStatus.COMPLETED;
        } catch (Exception e) {
            status = TaskStatus.FAILED;
            result = TaskResult.error(e.getMessage());
        }
    }
    
    protected abstract TaskResult executeTask(Agent agent);
}

// Workflow orchestrates multiple tasks
public class Workflow {
    private final String id;
    private final String name;
    private final List<Task> tasks;
    private final Map<String, TaskResult> results;
    private WorkflowStatus status;
    
    public Workflow(String name, List<Task> tasks) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.tasks = tasks;
        this.results = new ConcurrentHashMap<>();
        this.status = WorkflowStatus.PENDING;
    }
    
    public void execute(Agent agent) {
        status = WorkflowStatus.RUNNING;
        
        // Build execution order based on dependencies
        List<Task> executionOrder = topologicalSort();
        
        for (Task task : executionOrder) {
            // Wait for dependencies
            for (String depId : task.getDependencies()) {
                TaskResult depResult = results.get(depId);
                if (depResult == null || !depResult.isSuccess()) {
                    status = WorkflowStatus.FAILED;
                    return;
                }
            }
            
            // Execute task
            task.execute(agent);
            results.put(task.getId(), new TaskResult()); // Store result
        }
        
        status = WorkflowStatus.COMPLETED;
    }
    
    private List<Task> topologicalSort() {
        // Implementation of dependency resolution
        return new ArrayList<>();
    }
}

public enum TaskStatus {
    PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
}

public enum WorkflowStatus {
    PENDING, RUNNING, COMPLETED, FAILED
}
```

### 5. Context Management

```java
public class ContextManager {
    private final int maxContextTokens;
    private final TokenCounter tokenCounter;
    
    public ContextManager(int maxContextTokens) {
        this.maxContextTokens = maxContextTokens;
        this.tokenCounter = new TokenCounter();
    }
    
    public AgentContext prepareContext(
        AgentRequest request,
        List<Message> conversationHistory) {
        
        AgentContext context = new AgentContext();
        context.setSystemPrompt(buildSystemPrompt(request));
        
        // Add conversation history with token management
        int tokensBudget = maxContextTokens - 1000; // Reserve 1000 for response
        int usedTokens = tokenCounter.countTokens(context.getSystemPrompt());
        
        // Add messages in reverse order (most recent first)
        for (int i = conversationHistory.size() - 1; i >= 0; i--) {
            Message msg = conversationHistory.get(i);
            int msgTokens = tokenCounter.countTokens(msg.getContent());
            
            if (usedTokens + msgTokens > tokensBudget) {
                // Compact older messages
                context.addCompactedMessage(
                    createCompactedMessage(conversationHistory.subList(0, i))
                );
                break;
            }
            
            context.addMessage(msg);
            usedTokens += msgTokens;
        }
        
        // Add request context
        context.setRequest(request);
        return context;
    }
    
    private String buildSystemPrompt(AgentRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a helpful AI agent for enterprise ERP systems.\n");
        prompt.append("You have access to various tools to perform tasks.\n");
        prompt.append("Always explain your reasoning before taking action.\n");
        
        if (request.getSystemPromptAddendum() != null) {
            prompt.append("\n").append(request.getSystemPromptAddendum());
        }
        
        return prompt.toString();
    }
}

public class AgentContext {
    private String systemPrompt;
    private final List<Message> messages = new ArrayList<>();
    private AgentRequest request;
    
    // Getters and setters...
}
```

## Example Implementation for iDempiere ERP

### 1. ERP-Specific Tools

```java
// Tool for querying iDempiere database
public class QueryERPDatabaseTool implements Tool {
    private final DataSource dataSource;
    
    public QueryERPDatabaseTool(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public String getName() {
        return "query_erp_database";
    }
    
    @Override
    public String getDescription() {
        return "Execute SQL queries against iDempiere database to retrieve business data";
    }
    
    @Override
    public Map<String, ToolParameter> getParameters() {
        return Map.of(
            "sql", new ToolParameter("string", "SQL query to execute", true),
            "limit", new ToolParameter("integer", "Max rows to return", false)
        );
    }
    
    @Override
    public String execute(Map<String, Object> input) throws Exception {
        String sql = (String) input.get("sql");
        Integer limit = (Integer) input.getOrDefault("limit", 100);
        
        // Validate and sanitize query
        if (!isAllowedQuery(sql)) {
            throw new SecurityException("Query not allowed");
        }
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.setMaxRows(limit);
            ResultSet rs = stmt.executeQuery(sql);
            
            return formatResultSet(rs);
        }
    }
    
    private boolean isAllowedQuery(String sql) {
        // Prevent DELETE, UPDATE, DROP operations
        String upperSql = sql.toUpperCase().trim();
        return !(upperSql.startsWith("DELETE") || 
                 upperSql.startsWith("UPDATE") || 
                 upperSql.startsWith("DROP") ||
                 upperSql.startsWith("TRUNCATE"));
    }
    
    @Override
    public ToolPermission getRequiredPermission() {
        return ToolPermission.READ_ONLY;
    }
}

// Tool for analyzing inventory and calculating recommendations
public class AnalyzeInventoryTool implements Tool {
    private final ProductService productService;
    private final InventoryService inventoryService;
    
    @Override
    public String getName() {
        return "analyze_inventory";
    }
    
    @Override
    public String execute(Map<String, Object> input) throws Exception {
        String productId = (String) input.get("product_id");
        Integer days = (Integer) input.getOrDefault("days", 30);
        
        Product product = productService.getProduct(productId);
        InventoryAnalysis analysis = inventoryService.analyzeInventory(
            product, 
            days
        );
        
        return formatAnalysis(analysis);
    }
    
    private String formatAnalysis(InventoryAnalysis analysis) {
        return String.format(
            "Product: %s\n" +
            "Current Stock: %d units\n" +
            "Average Daily Usage: %.2f units\n" +
            "Days of Stock: %.1f days\n" +
            "Recommended Order: %d units\n" +
            "Estimated Cost: €%.2f",
            analysis.getProductName(),
            analysis.getCurrentStock(),
            analysis.getAverageDailyUsage(),
            analysis.getDaysOfStock(),
            analysis.getRecommendedOrder(),
            analysis.getEstimatedCost()
        );
    }
}

// Tool for generating compliance reports
public class GenerateComplianceReportTool implements Tool {
    private final ComplianceService complianceService;
    private final DocumentService documentService;
    
    @Override
    public String execute(Map<String, Object> input) throws Exception {
        String reportType = (String) input.get("report_type");
        String periodStart = (String) input.get("period_start");
        String periodEnd = (String) input.get("period_end");
        
        ComplianceReport report = complianceService.generateReport(
            reportType,
            LocalDate.parse(periodStart),
            LocalDate.parse(periodEnd)
        );
        
        // Save as PDF
        String documentPath = documentService.saveReport(report);
        
        return String.format(
            "Compliance report generated successfully.\n" +
            "Type: %s\n" +
            "Period: %s to %s\n" +
            "File: %s\n" +
            "Total Issues Found: %d",
            reportType,
            periodStart,
            periodEnd,
            documentPath,
            report.getIssuesCount()
        );
    }
}
```

### 2. ERP-Specific Commands

```java
// Command for running an end-of-month closing process
public class MonthEndClosingCommand implements Command {
    private final ClosingProcessService closingService;
    private final Agent agent;
    
    @Override
    public String getName() {
        return "month_end_closing";
    }
    
    @Override
    public String getDescription() {
        return "Execute month-end closing process with validation and reporting";
    }
    
    @Override
    public CommandResult execute(String[] args, AgentContext context) {
        try {
            YearMonth yearMonth = YearMonth.parse(args[0]);
            
            // Create a workflow for month-end closing
            Workflow workflow = createClosingWorkflow(yearMonth);
            workflow.execute(agent);
            
            return CommandResult.success(
                "Month-end closing completed for " + yearMonth
            );
        } catch (Exception e) {
            return CommandResult.error("Closing failed: " + e.getMessage());
        }
    }
    
    private Workflow createClosingWorkflow(YearMonth yearMonth) {
        List<Task> tasks = List.of(
            new ValidateOpenTransactionsTask(yearMonth),
            new ReconcileAccountsTask(yearMonth),
            new GenerateFinancialStatementsTask(yearMonth),
            new GenerateComplianceReportsTask(yearMonth),
            new LockPeriodTask(yearMonth)
        );
        
        return new Workflow("Month-End Closing", tasks);
    }
}

// Command for analyzing sales performance
public class AnalyzeSalesCommand implements Command {
    private final SalesService salesService;
    
    @Override
    public String execute(String[] args, AgentContext context) {
        String period = args.length > 0 ? args[0] : "current_month";
        
        SalesAnalysis analysis = salesService.analyze(period);
        
        return CommandResult.withData(
            formatAnalysis(analysis),
            Map.of(
                "total_revenue", analysis.getTotalRevenue(),
                "total_orders", analysis.getTotalOrders(),
                "average_order_value", analysis.getAverageOrderValue(),
                "top_products", analysis.getTopProducts()
            )
        );
    }
}
```

### 3. Complex Workflow Example

```java
// Multi-step agent workflow for demand forecasting
public class DemandForecastingWorkflow extends Workflow {
    
    public DemandForecastingWorkflow(String warehouseId) {
        super("Demand Forecasting", createTasks(warehouseId));
    }
    
    private static List<Task> createTasks(String warehouseId) {
        return List.of(
            new FetchHistoricalDataTask(warehouseId),
            new AnalyzeSeasonsalPatternsTask(warehouseId),
            new CalculateForecasts(warehouseId),
            new GenerateProcurementRecommendationsTask(warehouseId),
            new NotifyProcurementTeam(warehouseId)
        );
    }
}

// Individual task for demand forecasting
public class CalculateForecasts extends AbstractTask {
    private final String warehouseId;
    
    public CalculateForecasts(String warehouseId) {
        super("Calculate Forecasts", List.of("historical_data", "seasonality"));
        this.warehouseId = warehouseId;
    }
    
    @Override
    protected TaskResult executeTask(Agent agent) {
        // Use agent to call forecast calculation tools
        AgentRequest request = AgentRequest.builder()
            .prompt(
                "Based on the historical data and seasonality patterns, " +
                "calculate demand forecast for the next 3 months using " +
                "exponential smoothing and machine learning models."
            )
            .context("warehouse_id", warehouseId)
            .build();
        
        AgentResponse response = agent.execute(request);
        
        return response.isSuccess() 
            ? TaskResult.success(response.getOutput())
            : TaskResult.error(response.getError());
    }
}
```

## Configuration Example

```java
// Configuration class
@Configuration
public class AgentConfiguration {
    
    @Bean
    public ClaudeAgent erplntegrationAgent(
            ClaudeApiProperties apiProps,
            DataSource dataSource) {
        
        ClaudeAgentConfig config = ClaudeAgentConfig.builder()
            .apiKey(apiProps.getApiKey())
            .maxContextTokens(200000)
            .model("claude-sonnet-4-5")
            .build();
        
        ClaudeAgent agent = new ClaudeAgent(config);
        
        // Register tools
        agent.registerTool(new QueryERPDatabaseTool(dataSource));
        agent.registerTool(new AnalyzeInventoryTool(...));
        agent.registerTool(new GenerateComplianceReportTool(...));
        
        // Register commands
        agent.registerCommand(new MonthEndClosingCommand(...));
        agent.registerCommand(new AnalyzeSalesCommand(...));
        
        return agent;
    }
}
```

## Usage Examples

### Simple Agent Call
```java
AgentRequest request = AgentRequest.builder()
    .prompt("Analyze current inventory levels and recommend restocking for slow-moving items")
    .permission(ToolPermission.READ_ONLY)
    .build();

AgentResponse response = agent.execute(request);
System.out.println(response.getOutput());
```

### Workflow Execution
```java
Workflow workflow = new DemandForecastingWorkflow("warehouse_SK_01");
workflow.execute(agent);

if (workflow.getStatus() == WorkflowStatus.COMPLETED) {
    System.out.println("Forecasting complete");
    workflow.getResults().forEach(result -> System.out.println(result));
}
```

### Command Execution
```java
CommandResult result = commandManager.execute(
    "month_end_closing",
    new String[]{"2024-11"},
    context
);

if (result.isSuccess()) {
    System.out.println(result.getOutput());
    Map<String, Object> data = result.getData();
}
```

## Key Features

✅ **Tool Calling Loop** - Automatic iteration with tool results  
✅ **Permission System** - Fine-grained access control  
✅ **Context Management** - Automatic token budgeting and compaction  
✅ **Workflow Orchestration** - Task dependencies and parallel execution  
✅ **Error Handling** - Comprehensive exception management  
✅ **Extensibility** - Easy to add custom tools and commands  
✅ **ERP-Ready** - Designed for enterprise workflows  
✅ **Type Safety** - Full Java type system support  

## Best Practices

1. **Limit Iterations** - Set max iterations to prevent infinite loops
2. **Permission Scoping** - Always restrict to minimum required permissions
3. **Error Recovery** - Implement retry logic for transient failures
4. **Context Awareness** - Monitor token usage and compact proactively
5. **Tool Validation** - Sanitize inputs before execution
6. **Monitoring** - Track tool execution stats and performance
7. **Testing** - Create mock tools for testing workflows

## Next Steps

1. Implement core classes (Agent, Tool, Command)
2. Create Claude API client wrapper
3. Build tool registry with your ERP tools
4. Develop domain-specific commands
5. Create workflow definitions for your use cases
6. Implement comprehensive error handling
7. Add monitoring and observability
8. Set up testing framework

This framework gives you agent capabilities in Java while maintaining type safety and architectural control. You can evolve it as your ERP automation needs grow.
