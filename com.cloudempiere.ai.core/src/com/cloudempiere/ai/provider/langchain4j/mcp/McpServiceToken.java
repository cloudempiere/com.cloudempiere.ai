package com.cloudempiere.ai.provider.langchain4j.mcp;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import org.json.JSONObject;

/**
 * OAuth client_credentials token for an MCP server with {@code "auth": {"type": "service"}} (CLD-2543), e.g. a
 * Cognito app client of an internal service. The token is cached until shortly before it expires and fetched
 * again on demand, so a long-lived MCP connection keeps working across token lifetimes.
 *
 * <p>Thread-safe: {@link #get()} is called from the MCP transport for every message.
 */
public final class McpServiceToken {

    /** Posts a form to the token endpoint and returns the response body; injectable for tests. */
    @FunctionalInterface
    public interface TokenEndpoint {
        String post(String tokenUrl, Map<String, String> form) throws IOException, InterruptedException;
    }

    /** Fetch a new token this long before the cached one expires. */
    static final long REFRESH_MARGIN_MS = 60_000L;
    /** Lifetime assumed when the response has no {@code expires_in}. */
    static final long DEFAULT_LIFETIME_MS = 5 * 60_000L;

    private final MAIMcpServerConfig.ServiceAuth auth;
    private final TokenEndpoint endpoint;
    private final LongSupplier clock;

    private String token;
    private long expiresAt;

    public McpServiceToken(MAIMcpServerConfig.ServiceAuth auth, TokenEndpoint endpoint, LongSupplier clock) {
        this.auth = auth;
        this.endpoint = endpoint;
        this.clock = clock;
    }

    /** With the JDK HTTP client. */
    public static McpServiceToken of(MAIMcpServerConfig.ServiceAuth auth) {
        return new McpServiceToken(auth, McpServiceToken::httpPost, System::currentTimeMillis);
    }

    /**
     * The current access token, fetched when there is none or it is about to expire.
     *
     * @throws IllegalStateException when the token endpoint fails or returns no {@code access_token}; the message
     *         holds the OAuth {@code error} fields only, never a secret
     */
    public synchronized String get() {
        long now = clock.getAsLong();
        if (token != null && now < expiresAt - REFRESH_MARGIN_MS) {
            return token;
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "client_credentials");
        form.put("client_id", auth.getClientId());
        form.put("client_secret", auth.getClientSecret());
        if (auth.getScope() != null && !auth.getScope().isBlank()) {
            form.put("scope", auth.getScope());
        }
        String body;
        try {
            body = endpoint.post(auth.getTokenUrl(), form);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("token request to " + auth.getTokenUrl() + " interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("token request to " + auth.getTokenUrl() + " failed: " + e.getMessage(), e);
        }
        JSONObject json;
        try {
            json = new JSONObject(body);
        } catch (Exception e) {
            throw new IllegalStateException("token endpoint " + auth.getTokenUrl() + " returned no JSON");
        }
        String accessToken = json.optString("access_token", null);
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalStateException("token endpoint " + auth.getTokenUrl() + " returned no access_token: "
                + json.optString("error", "?") + " " + json.optString("error_description", ""));
        }
        long lifetimeMs = json.has("expires_in") ? json.optLong("expires_in", 0) * 1000L : DEFAULT_LIFETIME_MS;
        token = accessToken;
        expiresAt = now + (lifetimeMs > 0 ? lifetimeMs : DEFAULT_LIFETIME_MS);
        return token;
    }

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private static String httpPost(String tokenUrl, Map<String, String> form) throws IOException, InterruptedException {
        String encoded = form.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(e.getValue() != null ? e.getValue() : "", StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(tokenUrl))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(encoded))
            .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
