package com.cloudempiere.ai.provider.langchain4j.mcp;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Connection configuration for a single external MCP server (ADR-049), one entry of the
 * {@code AIG_MCP_SERVERS} AD_SysConfig JSON array.
 *
 * <p>Two transports are supported: STDIO (a local subprocess, e.g. an npx-launched MCP server) and
 * HTTP (the Streamable HTTP transport). Values may reference server environment variables as
 * {@code ${env:NAME}}; they are resolved when the configuration is parsed (CLD-2543).
 *
 * <p>Optional (CLD-2543):
 * <ul>
 *   <li>{@code roles}: AD_Role_IDs allowed to use this server's tools; empty = every role</li>
 *   <li>{@code auth}: {@code {"type":"service", "tokenUrl", "clientId", "clientSecret", "scope"}} gets an OAuth
 *       client_credentials token (e.g. from Cognito) and sends it as {@code Authorization: Bearer} (HTTP only)</li>
 * </ul>
 */
public final class MAIMcpServerConfig {

    public enum Transport {
        STDIO,
        HTTP
    }

    /** OAuth client_credentials settings of a server with {@code "auth": {"type": "service"}}. */
    public static final class ServiceAuth {
        private final String tokenUrl;
        private final String clientId;
        private final String clientSecret;
        private final String scope;

        public ServiceAuth(String tokenUrl, String clientId, String clientSecret, String scope) {
            this.tokenUrl = tokenUrl;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.scope = scope;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        /** Space-separated scopes, or null. */
        public String getScope() {
            return scope;
        }
    }

    private final String name;
    private final Transport transport;
    private final List<String> command;
    private final Map<String, String> environment;
    private final String url;
    private final Map<String, String> headers;
    private final Set<Integer> roles;
    private final ServiceAuth serviceAuth;

    private MAIMcpServerConfig(String name, Transport transport, List<String> command,
            Map<String, String> environment, String url, Map<String, String> headers,
            Set<Integer> roles, ServiceAuth serviceAuth) {
        this.name = name;
        this.transport = transport;
        this.command = command != null ? command : Collections.emptyList();
        this.environment = environment != null ? environment : Collections.emptyMap();
        this.url = url;
        this.headers = headers != null ? headers : Collections.emptyMap();
        this.roles = roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
        this.serviceAuth = serviceAuth;
    }

    public static MAIMcpServerConfig stdio(String name, List<String> command, Map<String, String> environment) {
        return stdio(name, command, environment, null);
    }

    public static MAIMcpServerConfig stdio(String name, List<String> command, Map<String, String> environment,
            Set<Integer> roles) {
        return new MAIMcpServerConfig(name, Transport.STDIO, command, environment, null, null, roles, null);
    }

    public static MAIMcpServerConfig http(String name, String url, Map<String, String> headers) {
        return http(name, url, headers, null, null);
    }

    public static MAIMcpServerConfig http(String name, String url, Map<String, String> headers,
            Set<Integer> roles, ServiceAuth serviceAuth) {
        return new MAIMcpServerConfig(name, Transport.HTTP, null, null, url, headers, roles, serviceAuth);
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

    /** AD_Role_IDs allowed to use this server; empty = every role. */
    public Set<Integer> getRoles() {
        return roles;
    }

    /** Whether a caller with this role may use the server's tools. */
    public boolean isAllowedFor(int adRoleId) {
        return roles.isEmpty() || roles.contains(adRoleId);
    }

    /** client_credentials settings, or null when the server takes no service token. */
    public ServiceAuth getServiceAuth() {
        return serviceAuth;
    }
}
