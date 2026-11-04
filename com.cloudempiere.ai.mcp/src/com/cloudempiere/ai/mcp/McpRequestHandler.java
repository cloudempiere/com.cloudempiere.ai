package com.cloudempiere.ai.mcp;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.json.JSONObject;

/**
 * One MCP HTTP POST, without the servlet API (CLD-2542), so every check can be unit-tested.
 * {@link McpServlet} adapts the HTTP request and response and supplies the real {@link Environment}.
 *
 * <p>The checks run in this order, and each one stops the request:
 * <ol>
 *   <li>{@code Origin}, when sent, must be allowed → 403 (DNS-rebinding protection, MCP transport spec)</li>
 *   <li>{@code Authorization: Bearer} must resolve to an iDempiere context → 401 with {@code WWW-Authenticate}</li>
 *   <li>the server must be switched on → 404</li>
 *   <li>the caller's role and user need AI access (ADR-058) → 403</li>
 *   <li>the body must not exceed the size limit → 413</li>
 * </ol>
 * Then the JSON-RPC message is handled: 200 with the reply, 202 for a notification, 400 for a malformed message,
 * 500 for an unexpected failure. The caller's context is bound for the request and always released.
 */
public final class McpRequestHandler {

    private static final CLogger log = CLogger.getCLogger(McpRequestHandler.class);

    public static final String WWW_AUTHENTICATE = "Bearer realm=\"iDempiere AI MCP\"";

    /** The parts of an HTTP request the handler reads. */
    public interface Request {
        String header(String name);

        String remoteAddr();

        String remoteHost();

        /** Declared body length, or -1 when unknown. */
        int contentLength();

        /** The body as text (UTF-8 when the request names no charset). */
        Reader body() throws IOException;
    }

    /** What the handler needs from the running server; faked in tests. */
    public interface Environment {
        /**
         * Resolve the bearer token to the caller's context.
         *
         * @throws SecurityException when the token is missing, invalid, expired or revoked
         */
        Properties authenticate(String authorizationHeader, String remoteIp, String remoteHost);

        boolean isEnabled(Properties ctx);

        /** Comma-separated allowed {@code Origin} values. */
        String allowedOrigins();

        int maxRequestBytes(Properties ctx);

        /** The caller's tools, or null when the caller has no AI access. */
        McpToolCatalog catalog(Properties ctx);

        /** Bind the caller's context to the current thread (iDempiere {@code ServerContext}). */
        void bind(Properties ctx);

        /** Release what {@link #bind} bound; called once for every bind, also on failures. */
        void unbind();
    }

    /** An HTTP response: status, content type, body and extra headers. */
    public static final class Response {
        private final int status;
        private final String contentType;
        private final String body;
        private final Map<String, String> headers;

        Response(int status, String contentType, String body, Map<String, String> headers) {
            this.status = status;
            this.contentType = contentType;
            this.body = body;
            this.headers = headers;
        }

        static Response plain(int status, String message) {
            return new Response(status, "text/plain", message, Map.of());
        }

        static Response json(int status, JSONObject body) {
            return new Response(status, "application/json", body.toString(), Map.of());
        }

        public int status() {
            return status;
        }

        /** null when there is no body (202). */
        public String contentType() {
            return contentType;
        }

        /** null when there is no body (202). */
        public String body() {
            return body;
        }

        public Map<String, String> headers() {
            return headers;
        }
    }

    private final McpProtocol protocol;
    private final Environment environment;

    public McpRequestHandler(McpProtocol protocol, Environment environment) {
        this.protocol = protocol;
        this.environment = environment;
    }

    public Response handlePost(Request request) {
        String origin = request.header("Origin");
        if (origin != null && !isAllowedOrigin(origin, environment.allowedOrigins())) {
            log.warning("MCP request rejected: Origin not allowed: " + origin);
            return Response.plain(403, "Origin not allowed");
        }

        Properties ctx;
        try {
            String authorization = request.header("Authorization");
            if (authorization == null || authorization.isBlank()) {
                throw new SecurityException("no Authorization header");
            }
            String remoteIp = request.header("X-Forwarded-For");
            if (remoteIp == null) {
                remoteIp = request.remoteAddr();
            }
            ctx = environment.authenticate(authorization, remoteIp, request.remoteHost());
            if (ctx == null) {
                throw new SecurityException("no context for the token");
            }
        } catch (SecurityException e) {
            log.warning("MCP request rejected: authentication failed: " + e.getMessage());
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("WWW-Authenticate", WWW_AUTHENTICATE);
            return new Response(401, "text/plain", "Authentication required", headers);
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "MCP request failed while authenticating", e);
            return Response.json(500, McpProtocol.error(null, McpProtocol.INTERNAL_ERROR, "Internal error"));
        }

        environment.bind(ctx);
        try {
            if (!environment.isEnabled(ctx)) {
                return Response.plain(404, "MCP server is not enabled");
            }

            McpToolCatalog catalog = environment.catalog(ctx);
            if (catalog == null) {
                log.warning("MCP request rejected: no AI access " + who(ctx));
                return Response.plain(403, "You do not have access to the AI assistant");
            }

            int maxBytes = environment.maxRequestBytes(ctx);
            String body = readBody(request, maxBytes);
            if (body == null) {
                return Response.plain(413, "Request body exceeds " + maxBytes + " bytes");
            }

            String caller = who(ctx);
            JSONObject reply = protocol.handle(body, catalog, (tool, success, durationMs, error) ->
                log.warning("MCP tools/call " + tool + " " + (success ? "OK" : "ERROR") + " " + durationMs + "ms "
                    + caller + (error != null ? " " + error : "")));

            if (reply == null) {
                return new Response(202, null, null, Map.of());
            }
            return Response.json(isMalformed(reply) ? 400 : 200, reply);
        } catch (Exception e) {
            log.log(Level.SEVERE, "MCP request failed " + who(ctx), e);
            return Response.json(500, McpProtocol.error(null, McpProtocol.INTERNAL_ERROR, "Internal error"));
        } finally {
            environment.unbind();
        }
    }

    static boolean isAllowedOrigin(String origin, String allowedOrigins) {
        if (allowedOrigins == null) {
            return false;
        }
        return Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .anyMatch(o -> !o.isEmpty() && o.equalsIgnoreCase(origin));
    }

    /** The body, or null when it exceeds {@code maxBytes} (checked on the declared length and while reading). */
    private static String readBody(Request request, int maxBytes) throws IOException {
        if (request.contentLength() > maxBytes) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[8192];
        try (Reader reader = request.body()) {
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
                if (sb.length() > maxBytes) {
                    return null;
                }
            }
        }
        return sb.toString();
    }

    private static boolean isMalformed(JSONObject reply) {
        JSONObject error = reply.optJSONObject("error");
        if (error == null) {
            return false;
        }
        int code = error.optInt("code");
        return code == McpProtocol.PARSE_ERROR || code == McpProtocol.INVALID_REQUEST;
    }

    /** The caller for log lines; reads the context keys directly, so the handler needs no {@code Env}. */
    private static String who(Properties ctx) {
        return "user=" + ctx.getProperty("#AD_User_ID", "?") + " role=" + ctx.getProperty("#AD_Role_ID", "?")
            + " client=" + ctx.getProperty("#AD_Client_ID", "?");
    }
}
