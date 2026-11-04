package com.cloudempiere.ai.provider.langchain4j.mcp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.model.MAIQueryAudit;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpHeadersSupplier;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;

/**
 * Builds the LangChain4j {@link ToolProvider} of external MCP servers for one caller (ADR-049).
 *
 * <p>Configuration lives in a single AD_SysConfig entry, {@code AIG_MCP_SERVERS}, holding a JSON array (no AD
 * tables, CLD-2543). Example:
 * <pre>
 * [
 *   {"name": "github", "transport": "stdio", "command": ["npx", "-y", "@modelcontextprotocol/server-github"],
 *    "env": {"GITHUB_PERSONAL_ACCESS_TOKEN": "${env:GITHUB_TOKEN}"}, "roles": [1000003]},
 *   {"name": "internal-tools", "transport": "http", "url": "https://mcp.example.com/tools",
 *    "auth": {"type": "service", "tokenUrl": "https://auth.example.com/oauth2/token",
 *             "clientId": "${env:TOOLS_CLIENT_ID}", "clientSecret": "${env:TOOLS_CLIENT_SECRET}", "scope": "api.read"}}
 * ]
 * </pre>
 *
 * <ul>
 *   <li><b>{@code ${env:NAME}}</b> in url, headers, env, command and auth is replaced with the server's environment
 *       variable, so no token is stored as plain text in the AD. An entry referencing an unset variable is skipped
 *       with a warning.</li>
 *   <li><b>{@code roles}</b>: AD_Role_IDs that may use the server's tools; no {@code roles} = every role.</li>
 *   <li><b>{@code auth}</b> (HTTP only): {@code "static"} (default: the headers as given) or {@code "service"}: an
 *       OAuth client_credentials token, cached until it expires and sent as {@code Authorization: Bearer} on every
 *       message ({@link McpServiceToken}).</li>
 * </ul>
 *
 * <p>Every external tool call is audited in {@code AIG_QueryAudit} ({@code AIGContextType='MCP'}), like the
 * ERPTools SQL queries: {@code AD_User_ID} is the provider's AI user, {@code CreatedBy} the caller.
 *
 * <p><b>Connection lifecycle (CLD-2540).</b> MCP clients are long-lived: a stdio client owns a subprocess, an HTTP
 * client an open session. They are connected once per AD_Client and cached, keyed by the AIG_MCP_SERVERS value;
 * see {@link MCPClientCache}. {@link #shutdown()} (called from {@code AIService.clearAllCaches()}) closes them.
 */
public final class MCPToolProviderFactory {

    private static final CLogger log = CLogger.getCLogger(MCPToolProviderFactory.class);

    public static final String SYSCONFIG_KEY = "AIG_MCP_SERVERS";

    /** {@code AIG_QueryAudit.AIGContextType} of external MCP tool calls. */
    public static final String AUDIT_CONTEXT_TYPE = "MCP";

    /** Interval of the client's own ping; a failed ping makes it reconnect the transport. */
    private static final Duration HEALTH_CHECK_INTERVAL = Duration.ofSeconds(60);
    private static final Duration RECONNECT_INTERVAL = Duration.ofSeconds(10);

    private static final MCPClientCache CACHE =
        new MCPClientCache(MCPToolProviderFactory::buildClient, System::currentTimeMillis);

    private MCPToolProviderFactory() {
    }

    /**
     * The external MCP tools the caller may use, or {@code null} when none are configured, none connected, or the
     * caller's role may use none of them (callers treat null as "no MCP tools", not an error). Build one per agent:
     * it is bound to the caller's role and audit context; the connections are shared.
     *
     * @param ctx      the caller's context (role, user, tenant)
     * @param provider the AI provider of the request, recorded in the audit
     */
    public static ToolProvider forCaller(Properties ctx, MAIProvider provider) {
        int adClientId = Env.getAD_Client_ID(ctx);
        String json = MSysConfig.getValue(SYSCONFIG_KEY, "", adClientId);
        List<MCPClientCache.Connection> connections = CACHE.get(adClientId, json);
        if (connections.isEmpty()) {
            return null;
        }
        McpCallerToolProvider tools = new McpCallerToolProvider(connections, Env.getAD_Role_ID(ctx),
            queryAudit(ctx, provider), System::currentTimeMillis);
        return tools.hasServers() ? tools : null;
    }

    /** Close every cached MCP client. The next {@link #forCaller} connects again. */
    public static void shutdown() {
        CACHE.closeAll();
    }

    /** Writes one {@code AIG_QueryAudit} row per external tool call. */
    static McpCallerToolProvider.ToolCallAudit queryAudit(Properties ctx, MAIProvider provider) {
        return (server, tool, arguments, success, durationMs, error) -> {
            int aiUserId = provider != null && provider.getAD_User_ID() > 0
                ? provider.getAD_User_ID() : Env.getAD_User_ID(ctx);
            MAIQueryAudit audit = new MAIQueryAudit(ctx, 0, null);
            if (provider != null) {
                audit.setAIG_Provider_ID(provider.getAIG_Provider_ID());
            }
            audit.setAD_User_ID(aiUserId);
            audit.setAD_Role_ID(Env.getAD_Role_ID(ctx));
            audit.setAIGContextType(AUDIT_CONTEXT_TYPE);
            audit.setAIGQueryPurpose(truncate("mcp:" + server + "." + tool, 255));
            audit.setAIGQuerySQL(arguments);
            audit.setAIGQueryStatus(success ? MAIQueryAudit.AIGQUERYSTATUS_Success : MAIQueryAudit.AIGQUERYSTATUS_Error);
            audit.setAIGExecutionTimeMs((int) durationMs);
            if (error != null) {
                audit.setAIGErrorMessage(truncate(error, 2000));
            }
            audit.saveEx();
        };
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static McpClient buildClient(MAIMcpServerConfig config) {
        McpTransport transport = buildTransport(config);
        return new DefaultMcpClient.Builder()
            .key(config.getName())
            .clientName("cloudempiere-ai")
            .transport(transport)
            .autoHealthCheck(true)
            .autoHealthCheckInterval(HEALTH_CHECK_INTERVAL)
            .reconnectInterval(RECONNECT_INTERVAL)
            .build();
    }

    private static McpTransport buildTransport(MAIMcpServerConfig config) {
        switch (config.getTransport()) {
            case STDIO:
                return new StdioMcpTransport.Builder()
                    .command(config.getCommand())
                    .environment(config.getEnvironment())
                    .build();
            case HTTP:
                StreamableHttpMcpTransport.Builder http = new StreamableHttpMcpTransport.Builder().url(config.getUrl());
                if (config.getServiceAuth() != null) {
                    http.customHeaders(serviceHeaders(config.getHeaders(), McpServiceToken.of(config.getServiceAuth())));
                } else {
                    http.customHeaders(config.getHeaders());
                }
                return http.build();
            default:
                throw new IllegalArgumentException("Unsupported MCP transport: " + config.getTransport());
        }
    }

    /** The static headers plus a fresh {@code Authorization: Bearer} on every message. */
    public static McpHeadersSupplier serviceHeaders(Map<String, String> headers, McpServiceToken token) {
        return callContext -> {
            Map<String, String> all = new HashMap<>(headers);
            all.put("Authorization", "Bearer " + token.get());
            return all;
        };
    }

    /**
     * Parse the AIG_MCP_SERVERS JSON array, resolving {@code ${env:NAME}} from the server's environment. Entries that
     * cannot be used (unset variable, unknown auth type, missing field) are skipped with a warning.
     *
     * @throws org.json.JSONException when the value is not a JSON array
     */
    public static List<MAIMcpServerConfig> parseServerConfigs(String json) {
        return parseServerConfigs(json, new McpSecrets(System::getenv),
            message -> log.warning(SYSCONFIG_KEY + ": " + message));
    }

    /** {@link #parseServerConfigs(String)} with an explicit variable lookup and a sink for skipped entries. */
    public static List<MAIMcpServerConfig> parseServerConfigs(String json, McpSecrets secrets, Consumer<String> skipped) {
        List<MAIMcpServerConfig> result = new ArrayList<>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.optJSONObject(i);
            String name = entry != null ? entry.optString("name", "#" + i) : "#" + i;
            try {
                if (entry == null) {
                    throw new IllegalArgumentException("not a JSON object");
                }
                result.add(parseServer(entry, secrets));
            } catch (Exception e) {
                skipped.accept("server '" + name + "' skipped: " + e.getMessage());
            }
        }
        return result;
    }

    private static MAIMcpServerConfig parseServer(JSONObject entry, McpSecrets secrets) {
        String name = entry.getString("name");
        String transport = entry.optString("transport", "stdio");
        Set<Integer> roles = toRoleSet(entry.optJSONArray("roles"));

        if ("http".equalsIgnoreCase(transport)) {
            String url = secrets.resolve(entry.getString("url"));
            Map<String, String> headers = toStringMap(entry.optJSONObject("headers"), secrets);
            return MAIMcpServerConfig.http(name, url, headers, roles, parseAuth(entry.optJSONObject("auth"), secrets));
        }
        if (entry.optJSONObject("auth") != null
                && !"static".equalsIgnoreCase(entry.getJSONObject("auth").optString("type", "static"))) {
            throw new IllegalArgumentException("auth is only supported for the http transport");
        }
        List<String> command = toStringList(entry.getJSONArray("command"), secrets);
        Map<String, String> env = toStringMap(entry.optJSONObject("env"), secrets);
        return MAIMcpServerConfig.stdio(name, command, env, roles);
    }

    private static MAIMcpServerConfig.ServiceAuth parseAuth(JSONObject auth, McpSecrets secrets) {
        if (auth == null) {
            return null;
        }
        String type = auth.optString("type", "static");
        if ("static".equalsIgnoreCase(type)) {
            return null;
        }
        if (!"service".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("unknown auth type '" + type + "' (static or service)");
        }
        return new MAIMcpServerConfig.ServiceAuth(
            secrets.resolve(auth.getString("tokenUrl")),
            secrets.resolve(auth.getString("clientId")),
            secrets.resolve(auth.getString("clientSecret")),
            secrets.resolve(auth.optString("scope", null)));
    }

    private static Set<Integer> toRoleSet(JSONArray array) {
        Set<Integer> roles = new HashSet<>();
        if (array == null) {
            return roles;
        }
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            int roleId = value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString().trim());
            roles.add(roleId);
        }
        return roles;
    }

    private static List<String> toStringList(JSONArray array, McpSecrets secrets) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(secrets.resolve(array.getString(i)));
        }
        return list;
    }

    private static Map<String, String> toStringMap(JSONObject obj, McpSecrets secrets) {
        Map<String, String> map = new HashMap<>();
        if (obj == null) {
            return map;
        }
        for (String key : obj.keySet()) {
            map.put(key, secrets.resolve(obj.getString(key)));
        }
        return map;
    }
}
