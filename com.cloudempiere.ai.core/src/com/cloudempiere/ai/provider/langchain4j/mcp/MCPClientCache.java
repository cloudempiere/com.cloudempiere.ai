package com.cloudempiere.ai.provider.langchain4j.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;

import org.compiere.util.CLogger;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolProvider;

/**
 * Per-AD_Client cache of connected MCP clients (CLD-2540), used by {@link MCPToolProviderFactory}.
 * The client builder and the clock are injected so it can be unit-tested without real MCP servers.
 */
public final class MCPClientCache {

    private static final CLogger log = CLogger.getCLogger(MCPClientCache.class);

    /** How long to wait before retrying servers that failed to connect. */
    public static final long RETRY_INTERVAL_MS = 5 * 60 * 1000L;

    private final Function<MAIMcpServerConfig, McpClient> clientBuilder;
    private final LongSupplier clock;
    private final Map<Integer, Entry> entries = new ConcurrentHashMap<>();

    public MCPClientCache(Function<MAIMcpServerConfig, McpClient> clientBuilder, LongSupplier clock) {
        this.clientBuilder = clientBuilder;
        this.clock = clock;
    }

    public ToolProvider get(int adClientId, String json) {
        String config = json == null ? "" : json.trim();
        Entry entry = entries.get(adClientId);
        if (entry == null && config.isEmpty()) {
            return null; // not configured: the common case, no lock
        }
        if (entry != null && entry.isValidFor(config, clock.getAsLong())) {
            return entry.provider;
        }
        synchronized (this) {
            entry = entries.get(adClientId);
            if (entry != null && entry.isValidFor(config, clock.getAsLong())) {
                return entry.provider;
            }
            Entry rebuilt = config.isEmpty() ? null : connect(config);
            if (rebuilt == null) {
                entries.remove(adClientId);
            } else {
                entries.put(adClientId, rebuilt);
            }
            if (entry != null) {
                entry.close();
            }
            return rebuilt == null ? null : rebuilt.provider;
        }
    }

    public synchronized void closeAll() {
        for (Entry entry : entries.values()) {
            entry.close();
        }
        entries.clear();
    }

    /** Number of AD_Clients with a cached entry (tests). */
    public int size() {
        return entries.size();
    }

    private Entry connect(String config) {
        List<MAIMcpServerConfig> serverConfigs;
        try {
            serverConfigs = MCPToolProviderFactory.parseServerConfigs(config);
        } catch (Exception e) {
            log.warning("Failed to parse " + MCPToolProviderFactory.SYSCONFIG_KEY + " AD_SysConfig value: " + e.getMessage());
            // Cache the bad value too, so it isn't re-parsed and re-logged on every message
            return new Entry(config, null, List.of(), false, clock.getAsLong());
        }

        List<McpClient> clients = new ArrayList<>();
        boolean anyFailed = false;
        for (MAIMcpServerConfig serverConfig : serverConfigs) {
            try {
                clients.add(clientBuilder.apply(serverConfig));
                log.warning("Connected to MCP server: " + serverConfig.getName());
            } catch (Exception e) {
                anyFailed = true;
                log.warning("Failed to connect to MCP server '" + serverConfig.getName() + "': " + e.getMessage());
            }
        }

        ToolProvider provider = clients.isEmpty() ? null
            : McpToolProvider.builder()
                .mcpClients(clients)
                .failIfOneServerFails(false)
                .build();
        return new Entry(config, provider, clients, anyFailed, clock.getAsLong());
    }

    private static final class Entry {
        final String config;
        final ToolProvider provider;
        final List<McpClient> clients;
        final boolean anyFailed;
        final long createdAt;

        Entry(String config, ToolProvider provider, List<McpClient> clients, boolean anyFailed, long createdAt) {
            this.config = config;
            this.provider = provider;
            this.clients = clients;
            this.anyFailed = anyFailed;
            this.createdAt = createdAt;
        }

        boolean isValidFor(String currentConfig, long now) {
            if (!config.equals(currentConfig)) {
                return false;
            }
            return !anyFailed || now - createdAt < RETRY_INTERVAL_MS;
        }

        void close() {
            for (McpClient client : clients) {
                try {
                    client.close();
                } catch (Exception e) {
                    log.warning("Failed to close MCP client '" + client.key() + "': " + e.getMessage());
                }
            }
        }
    }
}
