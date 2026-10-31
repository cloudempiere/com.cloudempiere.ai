package com.cloudempiere.ai.provider.langchain4j.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import dev.langchain4j.mcp.McpToolProvider;
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
 * ADR-049 describes; that's a separate, larger piece of work (new tables, migrations,
 * an AD window, audit logging) this class does not attempt.
 *
 * <p>Example value for AIG_MCP_SERVERS:
 * <pre>
 * [
 *   {"name": "github", "transport": "stdio", "command": ["npx", "-y", "@modelcontextprotocol/server-github"],
 *    "env": {"GITHUB_PERSONAL_ACCESS_TOKEN": "..."}},
 *   {"name": "internal-tools", "transport": "http", "url": "https://mcp.example.com/tools"}
 * ]
 * </pre>
 */
public final class MCPToolProviderFactory {

    private static final CLogger log = CLogger.getCLogger(MCPToolProviderFactory.class);

    public static final String SYSCONFIG_KEY = "AIG_MCP_SERVERS";

    private MCPToolProviderFactory() {
    }

    /**
     * Build a {@link ToolProvider} from the AIG_MCP_SERVERS AD_SysConfig entry for the
     * given client, or return {@code null} if no MCP servers are configured (the default -
     * callers should treat a null return as "no MCP tools available", not an error).
     */
    public static ToolProvider getConfigured(int adClientId) {
        String json = MSysConfig.getValue(SYSCONFIG_KEY, "", adClientId);
        if (json == null || json.isBlank()) {
            return null;
        }

        List<MAIMcpServerConfig> configs;
        try {
            configs = parseServerConfigs(json);
        } catch (Exception e) {
            log.warning("Failed to parse " + SYSCONFIG_KEY + " AD_SysConfig value: " + e.getMessage());
            return null;
        }

        return create(configs);
    }

    /**
     * Build a {@link ToolProvider} from an explicit list of server configs.
     * Returns {@code null} if the list is empty or every server fails to connect.
     */
    public static ToolProvider create(List<MAIMcpServerConfig> serverConfigs) {
        if (serverConfigs == null || serverConfigs.isEmpty()) {
            return null;
        }

        List<McpClient> clients = new ArrayList<>();
        for (MAIMcpServerConfig config : serverConfigs) {
            try {
                clients.add(buildClient(config));
                log.info("Connected to MCP server: " + config.getName());
            } catch (Exception e) {
                log.warning("Failed to connect to MCP server '" + config.getName() + "': " + e.getMessage());
            }
        }

        if (clients.isEmpty()) {
            return null;
        }

        return McpToolProvider.builder()
            .mcpClients(clients)
            .failIfOneServerFails(false)
            .build();
    }

    private static McpClient buildClient(MAIMcpServerConfig config) {
        McpTransport transport = buildTransport(config);
        return new DefaultMcpClient.Builder()
            .key(config.getName())
            .clientName("cloudempiere-ai")
            .transport(transport)
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
