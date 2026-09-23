package com.cloudempiere.ai.provider.langchain4j.mcp;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Connection configuration for a single external MCP server (ADR-049).
 *
 * <p>Two transports are supported: STDIO (a local subprocess, e.g. an npx-launched
 * MCP server) and HTTP (the modern Streamable HTTP transport). WebSocket transport
 * exists in langchain4j 1.20.0 too but isn't wired up here - add it the same way
 * if/when a server needs it.
 */
public final class MAIMcpServerConfig {

    public enum Transport {
        STDIO,
        HTTP
    }

    private final String name;
    private final Transport transport;
    private final List<String> command;
    private final Map<String, String> environment;
    private final String url;
    private final Map<String, String> headers;

    private MAIMcpServerConfig(String name, Transport transport, List<String> command,
            Map<String, String> environment, String url, Map<String, String> headers) {
        this.name = name;
        this.transport = transport;
        this.command = command != null ? command : Collections.emptyList();
        this.environment = environment != null ? environment : Collections.emptyMap();
        this.url = url;
        this.headers = headers != null ? headers : Collections.emptyMap();
    }

    public static MAIMcpServerConfig stdio(String name, List<String> command, Map<String, String> environment) {
        return new MAIMcpServerConfig(name, Transport.STDIO, command, environment, null, null);
    }

    public static MAIMcpServerConfig http(String name, String url, Map<String, String> headers) {
        return new MAIMcpServerConfig(name, Transport.HTTP, null, null, url, headers);
    }

    public String getName() {
        return name;
    }

    public Transport getTransport() {
        return transport;
    }

    public List<String> getCommand() {
        return command;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
