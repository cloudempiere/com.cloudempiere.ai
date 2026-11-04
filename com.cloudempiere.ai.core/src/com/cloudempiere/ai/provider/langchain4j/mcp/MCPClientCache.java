package com.cloudempiere.ai.provider.langchain4j.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;

import org.compiere.util.CLogger;

import dev.langchain4j.mcp.client.McpClient;

/**
 * Per-AD_Client cache of connected MCP clients (CLD-2540), used by {@link MCPToolProviderFactory}.
 * The client builder and the clock are injected so it can be unit-tested without real MCP servers.
 *
 * <p>It hands out the connected servers with their configuration ({@link Connection}), not a ready tool provider:
 * which servers a request may use depends on the caller's role (CLD-2543), see {@link McpCallerToolProvider}.
 */
public final class MCPClientCache {

    private static final CLogger log = CLogger.getCLogger(MCPClientCache.class);

    /** How long to wait before retrying servers that failed to connect. */
    public static final long RETRY_INTERVAL_MS = 5 * 60 * 1000L;

    /** A connected server: its configuration and the shared client. */
    public static final class Connection {
        private final MAIMcpServerConfig config;
        private final McpClient client;

        public Connection(MAIMcpServerConfig config, McpClient client) {
            this.config = config;
            this.client = client;
        }

        public MAIMcpServerConfig config() {
            return config;
        }

        public McpClient client() {
            return client;
        }
    }

    private final Function<MAIMcpServerConfig, McpClient> clientBuilder;
    private final LongSupplier clock;
    private final Map<Integer, Entry> entries = new ConcurrentHashMap<>();

    public MCPClientCache(Function<MAIMcpServerConfig, McpClient> clientBuilder, LongSupplier clock) {
        this.clientBuilder = clientBuilder;
        this.clock = clock;
    }

    /**
     * The connected servers of the tenant's configuration, connecting them on first use or after a change.
     *
     * @return the connections; empty when nothing is configured, the configuration is invalid or no server connected
     */
    public List<Connection> get(int adClientId, String json) {
        String config = json == null ? "" : json.trim();
        Entry entry = entries.get(adClientId);
        if (entry == null && config.isEmpty()) {
            return List.of(); // not configured: the common case, no lock
        }
        if (entry != null && entry.isValidFor(config, clock.getAsLong())) {
            return entry.connections;
        }
        synchronized (this) {
            entry = entries.get(adClientId);
            if (entry != null && entry.isValidFor(config, clock.getAsLong())) {
                return entry.connections;
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
            return rebuilt == null ? List.of() : rebuilt.connections;
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
            return new Entry(config, List.of(), false, clock.getAsLong());
        }

        List<Connection> connections = new ArrayList<>();
        boolean anyFailed = false;
        for (MAIMcpServerConfig serverConfig : serverConfigs) {
            try {
                connections.add(new Connection(serverConfig, clientBuilder.apply(serverConfig)));
                log.warning("Connected to MCP server: " + serverConfig.getName());
            } catch (Exception e) {
                anyFailed = true;
                log.warning("Failed to connect to MCP server '" + serverConfig.getName() + "': " + e.getMessage());
            }
        }

        return new Entry(config, List.copyOf(connections), anyFailed, clock.getAsLong());
    }

    private static final class Entry {
        final String config;
        final List<Connection> connections;
        final boolean anyFailed;
        final long createdAt;

        Entry(String config, List<Connection> connections, boolean anyFailed, long createdAt) {
            this.config = config;
            this.connections = connections;
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
            for (Connection connection : connections) {
                McpClient client = connection.client();
                try {
                    client.close();
                } catch (Exception e) {
                    log.warning("Failed to close MCP client '" + client.key() + "': " + e.getMessage());
                }
            }
        }
    }
}
