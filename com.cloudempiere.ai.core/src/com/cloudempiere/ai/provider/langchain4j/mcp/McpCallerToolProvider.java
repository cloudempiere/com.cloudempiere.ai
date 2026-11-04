package com.cloudempiere.ai.provider.langchain4j.mcp;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

import org.compiere.util.CLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

/**
 * The external MCP tools one caller may use, for one agent build (CLD-2543).
 *
 * <ul>
 *   <li><b>Role filter:</b> only servers whose {@code roles} include the caller's AD_Role_ID (or that list no
 *       roles) contribute tools. The connections themselves stay shared per tenant ({@link MCPClientCache}).</li>
 *   <li><b>Audit:</b> every tool call is reported to a {@link ToolCallAudit} with the server, tool, arguments
 *       (secret-looking values masked), outcome and duration.</li>
 * </ul>
 *
 * <p>Two servers offering the same tool name: the first server in the configuration wins and the other's tool is
 * left out, with a warning.
 */
public final class McpCallerToolProvider implements ToolProvider {

    private static final CLogger log = CLogger.getCLogger(McpCallerToolProvider.class);

    /** Argument keys whose values are masked in the audit. */
    private static final Pattern SECRET_KEY =
        Pattern.compile("(?i).*(token|secret|password|passwd|pwd|apikey|api_key|authorization|credential|cookie|private).*");
    public static final String MASK = "***";
    /** Longest arguments text kept in the audit. */
    static final int MAX_AUDIT_ARGUMENTS = 10_000;

    /** Receives one record per external tool call. */
    @FunctionalInterface
    public interface ToolCallAudit {
        void record(String server, String tool, String maskedArguments, boolean success, long durationMs, String error);
    }

    private final List<MCPClientCache.Connection> allowed;
    private final ToolCallAudit audit;
    private final LongSupplier clock;

    /**
     * @param connections the tenant's connected servers
     * @param adRoleId    the caller's role
     * @param audit       where tool calls are recorded
     * @param clock       for durations; {@code System::currentTimeMillis} in production
     */
    public McpCallerToolProvider(List<MCPClientCache.Connection> connections, int adRoleId, ToolCallAudit audit,
            LongSupplier clock) {
        this.allowed = connections.stream().filter(c -> c.config().isAllowedFor(adRoleId)).toList();
        this.audit = audit;
        this.clock = clock;
    }

    /** Whether the caller may use at least one server. */
    public boolean hasServers() {
        return !allowed.isEmpty();
    }

    /** Names of the servers the caller may use, in configuration order. */
    public List<String> serverNames() {
        return allowed.stream().map(c -> c.config().getName()).toList();
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult.Builder result = ToolProviderResult.builder();
        Set<String> names = new HashSet<>();
        for (MCPClientCache.Connection connection : allowed) {
            String server = connection.config().getName();
            ToolProviderResult serverTools;
            try {
                serverTools = McpToolProvider.builder()
                    .mcpClients(connection.client())
                    .failIfOneServerFails(false)
                    .build()
                    .provideTools(request);
            } catch (Exception e) {
                log.warning("MCP server '" + server + "': listing tools failed: " + e.getMessage());
                continue;
            }
            for (Map.Entry<ToolSpecification, ToolExecutor> tool : serverTools.tools().entrySet()) {
                String name = tool.getKey().name();
                if (!names.add(name)) {
                    log.warning("MCP tool '" + name + "' of server '" + server
                        + "' left out: an earlier server offers the same name");
                    continue;
                }
                result.add(tool.getKey(), new AuditedExecutor(server, name, tool.getValue()));
            }
        }
        return result.build();
    }

    private final class AuditedExecutor implements ToolExecutor {
        private final String server;
        private final String tool;
        private final ToolExecutor delegate;

        AuditedExecutor(String server, String tool, ToolExecutor delegate) {
            this.server = server;
            this.tool = tool;
            this.delegate = delegate;
        }

        @Override
        public String execute(ToolExecutionRequest request, Object memoryId) {
            long start = clock.getAsLong();
            try {
                String text = delegate.execute(request, memoryId);
                report(request, true, start, null);
                return text;
            } catch (RuntimeException e) {
                report(request, false, start, e.getMessage());
                throw e;
            }
        }

        @Override
        public ToolExecutionResult executeWithContext(ToolExecutionRequest request, InvocationContext context) {
            long start = clock.getAsLong();
            try {
                ToolExecutionResult result = delegate.executeWithContext(request, context);
                boolean failed = result != null && result.isError();
                report(request, !failed, start, failed ? result.resultText() : null);
                return result;
            } catch (RuntimeException e) {
                report(request, false, start, e.getMessage());
                throw e;
            }
        }

        private void report(ToolExecutionRequest request, boolean success, long start, String error) {
            try {
                audit.record(server, tool, maskArguments(request.arguments()), success, clock.getAsLong() - start, error);
            } catch (Exception e) {
                // An audit failure must not hide the tool result from the agent
                log.warning("MCP tool audit failed for " + server + "." + tool + ": " + e.getMessage());
            }
        }
    }

    /**
     * Mask the values of secret-looking keys (at any depth) in the tool arguments JSON. Non-JSON arguments are
     * kept as they are, and the result is truncated to {@link #MAX_AUDIT_ARGUMENTS} characters.
     */
    public static String maskArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return "{}";
        }
        String masked;
        try {
            Object json = new org.json.JSONTokener(arguments).nextValue();
            masked = String.valueOf(mask(json));
        } catch (Exception e) {
            masked = arguments;
        }
        return masked.length() > MAX_AUDIT_ARGUMENTS ? masked.substring(0, MAX_AUDIT_ARGUMENTS) + "…" : masked;
    }

    private static Object mask(Object value) {
        if (value instanceof JSONObject) {
            JSONObject in = (JSONObject) value;
            JSONObject out = new JSONObject();
            for (String key : in.keySet()) {
                out.put(key, SECRET_KEY.matcher(key).matches() ? MASK : mask(in.get(key)));
            }
            return out;
        }
        if (value instanceof JSONArray) {
            JSONArray in = (JSONArray) value;
            JSONArray out = new JSONArray();
            for (int i = 0; i < in.length(); i++) {
                out.put(mask(in.get(i)));
            }
            return out;
        }
        return value;
    }
}
