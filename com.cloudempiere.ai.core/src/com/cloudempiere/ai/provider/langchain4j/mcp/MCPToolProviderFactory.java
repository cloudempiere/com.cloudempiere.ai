package com.cloudempiere.ai.provider.langchain4j.mcp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.compiere.model.MSysConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;

/**
 * Builds a LangChain4j {@link ToolProvider} from configured external MCP servers (ADR-049).
 *
 * <p>Configuration lives in a single AD_SysConfig entry, {@code AIG_MCP_SERVERS}, holding a
 * JSON array. This is a deliberately minimal MVP wiring - not the full AD-table-driven,
 * role-scoped server management (AIG_MCPServer / AIG_MCPServer_Role / AIG_MCPToolAudit)
 * ADR-049 describes; that's a separate, larger piece of work (CLD-2543).
 *
 * <p>Example value for AIG_MCP_SERVERS:
 * <pre>
 * [
 *   {"name": "github", "transport": "stdio", "command": ["npx", "-y", "@modelcontextprotocol/server-github"],
 *    "env": {"GITHUB_PERSONAL_ACCESS_TOKEN": "..."}},
 *   {"name": "internal-tools", "transport": "http", "url": "https://mcp.example.com/tools"}
 * ]
 * </pre>
 *
 * <p><b>Connection lifecycle (CLD-2540).</b> MCP clients are long-lived: a stdio client owns a
 * subprocess, an HTTP client an open session. They are therefore connected once per AD_Client and
 * cached, keyed by the AIG_MCP_SERVERS value. The cached clients are closed and rebuilt when that
 * value changes, closed when it is cleared, and all closed by {@link #shutdown()} (called from
 * {@code AIService.deactivate()}). When some configured server failed to connect, the set is rebuilt
 * at most once per {@link MCPClientCache#RETRY_INTERVAL_MS}, not on every message.
 */
public final class MCPToolProviderFactory {

    public static final String SYSCONFIG_KEY = "AIG_MCP_SERVERS";

    /** Interval of the client's own ping; a failed ping makes it reconnect the transport. */
    private static final Duration HEALTH_CHECK_INTERVAL = Duration.ofSeconds(60);
    private static final Duration RECONNECT_INTERVAL = Duration.ofSeconds(10);

    private static final MCPClientCache CACHE =
        new MCPClientCache(MCPToolProviderFactory::buildClient, System::currentTimeMillis);

    private MCPToolProviderFactory() {
    }

    /**
     * Return the {@link ToolProvider} for the AIG_MCP_SERVERS AD_SysConfig entry of the given client,
     * or {@code null} if no MCP servers are configured or none could connect (callers treat null as
     * "no MCP tools available", not an error). Repeated calls reuse the same connected clients.
     */
    public static ToolProvider getConfigured(int adClientId) {
        String json = MSysConfig.getValue(SYSCONFIG_KEY, "", adClientId);
        return CACHE.get(adClientId, json);
    }

    /** Close every cached MCP client. The next {@link #getConfigured(int)} connects again. */
    public static void shutdown() {
        CACHE.closeAll();
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
                return new StreamableHttpMcpTransport.Builder()
                    .url(config.getUrl())
                    .customHeaders(config.getHeaders())
                    .build();
            default:
                throw new IllegalArgumentException("Unsupported MCP transport: " + config.getTransport());
        }
    }

    /**
     * Parse the AIG_MCP_SERVERS JSON array into server configs. Secrets (tokens, headers)
     * are expected to be resolved by the caller before they reach AD_SysConfig - this class
     * does no {@code ${env:...}}/{@code ${secret:...}} resolution, unlike ADR-049's original
     * sketch; keep tokens out of AD_SysConfig plaintext until that resolution layer exists.
     */
    static List<MAIMcpServerConfig> parseServerConfigs(String json) {
        List<MAIMcpServerConfig> result = new ArrayList<>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.getJSONObject(i);
            String name = entry.getString("name");
            String transport = entry.optString("transport", "stdio");

            if ("http".equalsIgnoreCase(transport)) {
                String url = entry.getString("url");
                Map<String, String> headers = toStringMap(entry.optJSONObject("headers"));
                result.add(MAIMcpServerConfig.http(name, url, headers));
            } else {
                List<String> command = toStringList(entry.getJSONArray("command"));
                Map<String, String> env = toStringMap(entry.optJSONObject("env"));
                result.add(MAIMcpServerConfig.stdio(name, command, env));
            }
        }
        return result;
    }

    private static List<String> toStringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }

    private static Map<String, String> toStringMap(JSONObject obj) {
        Map<String, String> map = new HashMap<>();
        if (obj == null) {
            return map;
        }
        for (String key : obj.keySet()) {
            map.put(key, obj.getString(key));
        }
        return map;
    }
}
