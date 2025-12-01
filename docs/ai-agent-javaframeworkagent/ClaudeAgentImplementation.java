package com.cloudempiere.agent.core;

import com.anthropic.sdk.client.AnthropicClient;
import com.anthropic.sdk.messages.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// ============================================================================
// AGENT CORE INTERFACES
// ============================================================================

/**
 * Base interface for all tools that Claude can call
 */
public interface Tool {
    String getName();
    String getDescription();
    String getCategory();
    Map<String, ToolParameter> getParameters();
    String execute(Map<String, Object> input) throws Exception;
    ToolPermission getRequiredPermission();
    
    default com.anthropic.sdk.messages.Tool toClaudeTool() {
        return com.anthropic.sdk.messages.Tool.builder()
            .name(getName())
            .description(getDescription())
            .inputSchema(InputSchema.builder()
                .type("object")
                .properties(buildProperties())
                .required(getRequiredParameters())
                .build())
            .build();
    }
    
    private Map<String, Object> buildProperties() {
        return getParameters().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> Map.of(
                    "type", e.getValue().getType(),
                    "description", e.getValue().getDescription()
                )
            ));
    }
    
    private List<String> getRequiredParameters() {
        return getParameters().entrySet().stream()
            .filter(e -> e.getValue().isRequired())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
}

@Data
@AllArgsConstructor
class ToolParameter {
    private String type;
    private String description;
    private boolean required;
    
    public ToolParameter(String type, String description, boolean required) {
        this.type = type;
        this.description = description;
        this.required = required;
    }
}

enum ToolPermission {
    READ_ONLY, READ_WRITE, EXECUTE, ADMIN
}

/**
 * Agent interface - main entry point for agent operations
 */
public interface Agent {
    AgentResponse execute(AgentRequest request);
    void registerCommand(Command command);
    void registerTool(Tool tool);
    AgentState getState();
}

/**
 * Command interface - user-defined actions
 */
public interface Command {
    String getName();
    String getDescription();
    CommandResult execute(String[] args, Agent agent);
}

// ============================================================================
// TOOL REGISTRY
// ============================================================================

@Slf4j
public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final Map<String, ToolExecutionStats> stats = new ConcurrentHashMap<>();
    
    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
        stats.put(tool.getName(), new ToolExecutionStats());
        log.info("Registered tool: {}", tool.getName());
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
    
    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
}

@Data
class ToolExecutionStats {
    private int totalExecutions = 0;
    private int successCount = 0;
    private int failureCount = 0;
    private long totalExecutionTimeMs = 0;
    private long lastExecutionTime;
    
    public void recordExecution(long durationMs, boolean success) {
        totalExecutions++;
        if (success) {
            successCount++;
        } else {
            failureCount++;
        }
        totalExecutionTimeMs += durationMs;
        lastExecutionTime = System.currentTimeMillis();
    }
    
    public double getAverageExecutionTimeMs() {
        return totalExecutions == 0 ? 0 : (double) totalExecutionTimeMs / totalExecutions;
    }
}

// ============================================================================
// COMMAND MANAGER
// ============================================================================

@Slf4j
public class CommandManager {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();
    
    public void register(Command command) {
        commands.put(command.getName(), command);
        log.info("Registered command: {}", command.getName());
    }
    
    public CommandResult execute(String commandName, String[] args, Agent agent) {
        Command command = commands.get(commandName);
        if (command == null) {
            return CommandResult.error("Command not found: " + commandName);
        }
        try {
            return command.execute(args, agent);
        } catch (Exception e) {
            log.error("Command execution failed: {}", commandName, e);
            return CommandResult.error("Execution failed: " + e.getMessage());
        }
    }
    
    public List<Command> listCommands() {
        return new ArrayList<>(commands.values());
    }
}

// ============================================================================
// DATA MODELS
// ============================================================================

@Data
@Builder
class AgentRequest {
    private String prompt;
    private Map<String, Object> context;
    private Set<ToolPermission> permissions;
    private String systemPromptAddendum;
    private Map<String, String> metadata;
    private int maxIterations;
    
    public static AgentRequestBuilder builder() {
        return new AgentRequestBuilder()
            .permissions(Set.of(ToolPermission.READ_ONLY))
            .maxIterations(10);
    }
    
    public boolean hasPermission(ToolPermission permission) {
        return permissions.contains(permission) || permissions.contains(ToolPermission.ADMIN);
    }
}

@Data
class AgentResponse {
    private final boolean success;
    private final String output;
    private final String error;
    private final List<ToolExecution> toolExecutions;
    private final long executionTimeMs;
    
    public static AgentResponse success(String output, List<ToolExecution> executions) {
        return new AgentResponse(true, output, null, executions, 0);
    }
    
    public static AgentResponse error(String error) {
        return new AgentResponse(false, null, error, Collections.emptyList(), 0);
    }
}

@Data
@AllArgsConstructor
class ToolExecution {
    private String toolName;
    private Map<String, Object> input;
    private String output;
    private boolean success;
    private long executionTimeMs;
}

@Data
@Builder
class CommandResult {
    private final boolean success;
    private final String output;
    private final Map<String, Object> data;
    
    public static CommandResult success(String output) {
        return builder()
            .success(true)
            .output(output)
            .data(Collections.emptyMap())
            .build();
    }
    
    public static CommandResult error(String error) {
        return builder()
            .success(false)
            .output(error)
            .data(Collections.emptyMap())
            .build();
    }
    
    public static CommandResult withData(String output, Map<String, Object> data) {
        return builder()
            .success(true)
            .output(output)
            .data(data)
            .build();
    }
}

@Data
class AgentState {
    private List<Message> conversationHistory = new ArrayList<>();
    private Map<String, Object> variables = new ConcurrentHashMap<>();
    private long createdAt = System.currentTimeMillis();
    
    public void addToHistory(String userMessage, String assistantMessage) {
        conversationHistory.add(new Message("user", userMessage));
        conversationHistory.add(new Message("assistant", assistantMessage));
    }
    
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }
    
    public Object getVariable(String key) {
        return variables.get(key);
    }
}

@Data
@AllArgsConstructor
class Message {
    private String role;
    private String content;
}

// ============================================================================
// CONTEXT MANAGER
// ============================================================================

@Slf4j
public class ContextManager {
    private final int maxContextTokens;
    
    public ContextManager(int maxContextTokens) {
        this.maxContextTokens = maxContextTokens;
    }
    
    public AgentContext prepareContext(
            AgentRequest request,
            List<Message> conversationHistory) {
        
        AgentContext context = new AgentContext();
        context.setSystemPrompt(buildSystemPrompt(request));
        context.setRequest(request);
        
        // Simple token estimation (1 token ≈ 4 characters)
        int tokensBudget = maxContextTokens - 2000; // Reserve for response + tools
        int usedTokens = estimateTokens(context.getSystemPrompt());
        
        // Add recent messages first (recency bias)
        for (int i = conversationHistory.size() - 1; i >= 0 && usedTokens < tokensBudget; i--) {
            Message msg = conversationHistory.get(i);
            int msgTokens = estimateTokens(msg.getContent());
            
            if (usedTokens + msgTokens > tokensBudget) {
                log.warn("Context budget exhausted, skipping older messages");
                break;
            }
            
            context.addMessage(msg);
            usedTokens += msgTokens;
        }
        
        return context;
    }
    
    private String buildSystemPrompt(AgentRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a helpful AI assistant for enterprise ERP systems.\n");
        prompt.append("You have access to various tools to perform tasks.\n");
        prompt.append("Always explain your reasoning before taking action.\n");
        prompt.append("Use the available tools to gather information and complete tasks.\n");
        
        if (request.getSystemPromptAddendum() != null) {
            prompt.append("\n").append(request.getSystemPromptAddendum());
        }
        
        prompt.append("\nImportant: Always verify your results and handle errors gracefully.");
        
        return prompt.toString();
    }
    
    private int estimateTokens(String text) {
        // Rough estimation: ~4 characters = 1 token
        return Math.max(1, text.length() / 4);
    }
}

@Data
class AgentContext {
    private String systemPrompt;
    private final List<Message> messages = new ArrayList<>();
    private AgentRequest request;
    
    public void addMessage(Message message) {
        messages.add(message);
    }
    
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }
}

// ============================================================================
// MAIN AGENT IMPLEMENTATION
// ============================================================================

@Slf4j
@AllArgsConstructor
public class ClaudeAgent implements Agent {
    private final AnthropicClient apiClient;
    private final ClaudeAgentConfig config;
    private final CommandManager commandManager;
    private final ToolRegistry toolRegistry;
    private final ContextManager contextManager;
    private final AgentState state;
    
    public ClaudeAgent(String apiKey, int maxContextTokens) {
        this.apiClient = new AnthropicClient(apiKey);
        this.config = ClaudeAgentConfig.builder()
            .apiKey(apiKey)
            .maxContextTokens(maxContextTokens)
            .model("claude-sonnet-4-5")
            .build();
        this.commandManager = new CommandManager();
        this.toolRegistry = new ToolRegistry();
        this.contextManager = new ContextManager(maxContextTokens);
        this.state = new AgentState();
    }
    
    @Override
    public AgentResponse execute(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        List<ToolExecution> executions = new ArrayList<>();
        
        try {
            // Prepare context
            AgentContext context = contextManager.prepareContext(
                request,
                state.getConversationHistory()
            );
            
            // Execute agent loop
            String result = executeAgentLoop(context, request, executions);
            
            // Update state
            state.addToHistory(request.getPrompt(), result);
            
            long duration = System.currentTimeMillis() - startTime;
            return new AgentResponse(true, result, null, executions, duration);
            
        } catch (Exception e) {
            log.error("Agent execution failed", e);
            long duration = System.currentTimeMillis() - startTime;
            return new AgentResponse(false, null, e.getMessage(), executions, duration);
        }
    }
    
    private String executeAgentLoop(
            AgentContext context,
            AgentRequest request,
            List<ToolExecution> executions) throws Exception {
        
        StringBuilder finalOutput = new StringBuilder();
        int iteration = 0;
        int maxIterations = request.getMaxIterations() > 0 ? request.getMaxIterations() : 10;
        
        // Initial Claude call
        com.anthropic.sdk.messages.Message response = callClaude(context, null);
        
        while (iteration < maxIterations) {
            iteration++;
            log.debug("Agent iteration: {}", iteration);
            
            // Extract tool calls from response
            List<ToolUseBlock> toolCalls = extractToolCalls(response);
            
            if (toolCalls.isEmpty()) {
                // No tool calls, Claude is done
                finalOutput.append(extractTextContent(response));
                break;
            }
            
            // Execute all tool calls
            List<ToolResultBlockParam> toolResults = new ArrayList<>();
            for (ToolUseBlock toolCall : toolCalls) {
                ToolResult result = executeTool(
                    toolCall,
                    request,
                    executions
                );
                
                if (result.isSuccess()) {
                    finalOutput.append(String.format("✓ %s\n", toolCall.getName()));
                } else {
                    finalOutput.append(String.format("✗ %s: %s\n", 
                        toolCall.getName(), result.getError()));
                }
                
                toolResults.add(ToolResultBlockParam.builder()
                    .toolUseId(toolCall.getId())
                    .content(result.getOutput())
                    .build());
            }
            
            // Send tool results back to Claude
            context.addMessage(new Message("assistant", response.getContent().toString()));
            for (ToolResultBlockParam result : toolResults) {
                context.addMessage(new Message("tool", result.getContent()));
            }
            
            response = callClaude(context, toolResults);
        }
        
        if (iteration >= maxIterations) {
            log.warn("Agent reached max iterations: {}", maxIterations);
            finalOutput.append("\n[Max iterations reached]");
        }
        
        return finalOutput.toString();
    }
    
    private com.anthropic.sdk.messages.Message callClaude(
            AgentContext context,
            List<ToolResultBlockParam> toolResults) throws Exception {
        
        // Build messages list
        List<MessageParam> messages = new ArrayList<>();
        
        if (toolResults == null) {
            // Initial call - add context messages
            for (Message msg : context.getMessages()) {
                messages.add(MessageParam.builder()
                    .role(msg.getRole().equals("user") ? MessageParam.Role.USER : MessageParam.Role.ASSISTANT)
                    .content(msg.getContent())
                    .build());
            }
            // Add current prompt
            messages.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content(context.getRequest().getPrompt())
                .build());
        } else {
            // Continue with tool results
            for (ToolResultBlockParam result : toolResults) {
                messages.add(MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .content(result.getContent())
                    .build());
            }
        }
        
        // Get available tools
        List<Tool> availableTools = toolRegistry.getAvailableTools(
            context.getRequest().getPermissions()
        );
        
        // Call Claude API
        com.anthropic.sdk.messages.MessageCreateParams.Builder paramsBuilder =
            com.anthropic.sdk.messages.MessageCreateParams.builder()
                .model(config.getModel())
                .maxTokens(4096)
                .systemPrompt(context.getSystemPrompt())
                .messages(messages);
        
        if (!availableTools.isEmpty()) {
            paramsBuilder.tools(availableTools.stream()
                .map(Tool::toClaudeTool)
                .collect(Collectors.toList()));
        }
        
        return apiClient.messages().create(paramsBuilder.build());
    }
    
    private ToolResult executeTool(
            ToolUseBlock toolCall,
            AgentRequest request,
            List<ToolExecution> executions) {
        
        long startTime = System.currentTimeMillis();
        Tool tool = toolRegistry.getTool(toolCall.getName());
        
        if (tool == null) {
            return ToolResult.error("Tool not found: " + toolCall.getName());
        }
        
        // Check permissions
        if (!request.hasPermission(tool.getRequiredPermission())) {
            String error = "Permission denied for tool: " + toolCall.getName();
            recordExecution(toolCall.getName(), toolCall.getInput(), error, false, 
                System.currentTimeMillis() - startTime, executions);
            return ToolResult.error(error);
        }
        
        try {
            String result = tool.execute(toolCall.getInput());
            recordExecution(toolCall.getName(), toolCall.getInput(), result, true,
                System.currentTimeMillis() - startTime, executions);
            return ToolResult.success(result);
        } catch (Exception e) {
            String error = "Tool execution failed: " + e.getMessage();
            log.error("Tool execution error: {}", toolCall.getName(), e);
            recordExecution(toolCall.getName(), toolCall.getInput(), error, false,
                System.currentTimeMillis() - startTime, executions);
            return ToolResult.error(error);
        }
    }
    
    private void recordExecution(
            String toolName,
            Map<String, Object> input,
            String output,
            boolean success,
            long duration,
            List<ToolExecution> executions) {
        
        executions.add(new ToolExecution(toolName, input, output, success, duration));
        ToolExecutionStats stats = toolRegistry.getStats(toolName);
        if (stats != null) {
            stats.recordExecution(duration, success);
        }
    }
    
    private List<ToolUseBlock> extractToolCalls(com.anthropic.sdk.messages.Message response) {
        List<ToolUseBlock> toolCalls = new ArrayList<>();
        // Extract tool use blocks from response content
        // Implementation depends on Anthropic SDK structure
        return toolCalls;
    }
    
    private String extractTextContent(com.anthropic.sdk.messages.Message response) {
        // Extract text from response content
        return response.getContent().toString();
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

@Data
@Builder
class ClaudeAgentConfig {
    private String apiKey;
    private int maxContextTokens;
    private String model;
}

@Data
class ToolResult {
    private final boolean success;
    private final String output;
    private final String error;
    
    public static ToolResult success(String output) {
        return new ToolResult(true, output, null);
    }
    
    public static ToolResult error(String error) {
        return new ToolResult(false, null, error);
    }
}

// ============================================================================
// EXAMPLE USAGE
// ============================================================================

public class AgentExample {
    
    public static void main(String[] args) {
        // Initialize agent
        ClaudeAgent agent = new ClaudeAgent(
            System.getenv("ANTHROPIC_API_KEY"),
            200000  // max context tokens
        );
        
        // Register tools
        agent.registerTool(new SampleQueryTool());
        agent.registerTool(new SampleAnalysisTool());
        
        // Register commands
        agent.registerCommand(new SampleCommand());
        
        // Execute agent request
        AgentRequest request = AgentRequest.builder()
            .prompt("Analyze current inventory levels and recommend restocking items")
            .permissions(Set.of(ToolPermission.READ_ONLY))
            .systemPromptAddendum("Focus on slow-moving items in the warehouse.")
            .maxIterations(5)
            .build();
        
        AgentResponse response = agent.execute(request);
        
        if (response.isSuccess()) {
            System.out.println("✓ Agent execution successful");
            System.out.println("\nOutput:\n" + response.getOutput());
            System.out.println("\nTool Executions:");
            response.getToolExecutions().forEach(e ->
                System.out.printf("  - %s: %s (%.0fms)\n", 
                    e.getToolName(), 
                    e.isSuccess() ? "OK" : "FAIL",
                    e.getExecutionTimeMs())
            );
        } else {
            System.out.println("✗ Agent execution failed: " + response.getError());
        }
    }
    
    // Sample tool implementation
    static class SampleQueryTool implements Tool {
        @Override
        public String getName() {
            return "query_inventory";
        }
        
        @Override
        public String getDescription() {
            return "Query current inventory levels from the ERP system";
        }
        
        @Override
        public String getCategory() {
            return "inventory";
        }
        
        @Override
        public Map<String, ToolParameter> getParameters() {
            return Map.of(
                "warehouse_id", new ToolParameter("string", "Warehouse ID", true),
                "limit", new ToolParameter("integer", "Max items to return", false)
            );
        }
        
        @Override
        public String execute(Map<String, Object> input) throws Exception {
            String warehouseId = (String) input.get("warehouse_id");
            int limit = (int) input.getOrDefault("limit", 50);
            
            // Simulate query
            return String.format(
                "Inventory for warehouse %s (top %d items):\n" +
                "Item SKU123: 45 units (below threshold)\n" +
                "Item SKU456: 200 units (adequate)\n" +
                "Item SKU789: 5 units (critical)",
                warehouseId, limit
            );
        }
        
        @Override
        public ToolPermission getRequiredPermission() {
            return ToolPermission.READ_ONLY;
        }
    }
    
    static class SampleAnalysisTool implements Tool {
        @Override
        public String getName() {
            return "analyze_trends";
        }
        
        @Override
        public String getDescription() {
            return "Analyze inventory trends and movement patterns";
        }
        
        @Override
        public String getCategory() {
            return "analysis";
        }
        
        @Override
        public Map<String, ToolParameter> getParameters() {
            return Map.of(
                "item_id", new ToolParameter("string", "Item SKU", true),
                "days", new ToolParameter("integer", "Analysis period in days", false)
            );
        }
        
        @Override
        public String execute(Map<String, Object> input) throws Exception {
            String itemId = (String) input.get("item_id");
            int days = (int) input.getOrDefault("days", 30);
            
            return String.format(
                "Trend analysis for %s (last %d days):\n" +
                "Average daily usage: 2.5 units\n" +
                "Current stock: 5 units\n" +
                "Days until stockout: 2 days\n" +
                "Recommendation: Urgent reorder",
                itemId, days
            );
        }
        
        @Override
        public ToolPermission getRequiredPermission() {
            return ToolPermission.READ_ONLY;
        }
    }
    
    static class SampleCommand implements Command {
        @Override
        public String getName() {
            return "analyze_inventory";
        }
        
        @Override
        public String getDescription() {
            return "Comprehensive inventory analysis and recommendations";
        }
        
        @Override
        public CommandResult execute(String[] args, Agent agent) {
            return CommandResult.success("Inventory analysis completed successfully");
        }
    }
}
