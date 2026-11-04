package com.cloudempiere.ai.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.adempiere.util.ServerContext;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.ERPTools;
import com.cloudempiere.ai.provider.langchain4j.tools.RagTools;
import com.cloudempiere.ai.rag.IRagService;
import com.cloudempiere.core.base.auth.RequestContextService;
import com.cloudempiere.core.base.auth.RequestSignals;

/**
 * MCP server endpoint, Streamable HTTP transport (ADR-003, CLD-2542). Served at {@code /ai-mcp}.
 *
 * <p>Each POST carries one JSON-RPC message and gets one {@code application/json} response (or 202 for a
 * notification). There is no server-initiated stream and no session: GET and DELETE answer 405.
 *
 * <p>Per request:
 * <ol>
 *   <li>{@code Origin}, when sent, must be listed in {@value #SYSCONFIG_ALLOWED_ORIGINS} (DNS-rebinding
 *       protection required by the MCP transport spec); MCP clients outside a browser send none</li>
 *   <li>{@code Authorization: Bearer} is resolved to an iDempiere context by {@link RequestContextService}
 *       (ADR-0038), the same port GraphQL uses; 401 otherwise</li>
 *   <li>{@link #SERVER_ENABLED} must be true; 404 otherwise (POC: a constant, a per-tenant setting later)</li>
 *   <li>the caller's role and user need AI access (ADR-058, {@link MAIProvider#getForUser}); 403 otherwise</li>
 *   <li>the tools are those of the chat agent, bound to the caller's context, so every database query runs
 *       under the caller's role through {@code SecureDatabaseQueryExecutor}</li>
 * </ol>
 */
public class McpServlet extends HttpServlet {

    private static final long serialVersionUID = 20261104L;

    private static final CLogger log = CLogger.getCLogger(McpServlet.class);

    /**
     * Whether the endpoint answers at all. POC: a hardcoded switch, so no AD_SysConfig record is needed; it becomes
     * the per-tenant setting {@code AI_MCP_SERVER_ENABLED} later. Every request still needs a valid token and a role
     * with AI access (ADR-058).
     */
    public static final boolean SERVER_ENABLED = true;
    public static final String SYSCONFIG_ALLOWED_ORIGINS = "AI_MCP_ALLOWED_ORIGINS";
    public static final String SYSCONFIG_MAX_REQUEST_BYTES = "AI_MCP_MAX_REQUEST_BYTES";

    private static final int DEFAULT_MAX_REQUEST_BYTES = 1_048_576;

    private static final String INSTRUCTIONS = "Tools of the CloudEmpiere iDempiere ERP AI assistant. "
        + "Every call runs with your iDempiere role: you see only the tables, columns and records it may read. "
        + "Use listTables and getTableMetadata to find tables and columns before writing a query.";

    private transient McpProtocol protocol;
    private transient ServiceTracker<RequestContextService, RequestContextService> requestContextTracker;
    private transient ServiceTracker<IRagService, IRagService> ragTracker;

    @Override
    public void init() {
        protocol = new McpProtocol(bundleVersion(), INSTRUCTIONS);
        BundleContext bc = bundleContext();
        if (bc != null) {
            requestContextTracker = new ServiceTracker<>(bc, RequestContextService.class, null);
            requestContextTracker.open();
            ragTracker = new ServiceTracker<>(bc, IRagService.class, null);
            ragTracker.open();
        } else {
            log.warning("AI MCP server: no bundle context, every request will be rejected");
        }
        log.warning("AI MCP server endpoint " + (SERVER_ENABLED ? "ready" : "disabled") + " at /ai-mcp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && !isAllowedOrigin(origin)) {
            log.warning("MCP request rejected: Origin not allowed: " + origin);
            sendPlain(response, HttpServletResponse.SC_FORBIDDEN, "Origin not allowed");
            return;
        }

        Properties ctx;
        try {
            ctx = resolveContext(request);
        } catch (SecurityException e) {
            log.warning("MCP request rejected: authentication failed: " + e.getMessage());
            response.setHeader("WWW-Authenticate", "Bearer realm=\"iDempiere AI MCP\"");
            sendPlain(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        ServerContext.setCurrentInstance(ctx);
        try {
            int clientId = Env.getAD_Client_ID(ctx);
            if (!SERVER_ENABLED) {
                sendPlain(response, HttpServletResponse.SC_NOT_FOUND, "MCP server is not enabled");
                return;
            }

            MAIProvider provider = MAIProvider.getForUser(ctx, null);
            if (provider == null) {
                log.warning("MCP request rejected: no AI access " + who(ctx));
                sendPlain(response, HttpServletResponse.SC_FORBIDDEN, "You do not have access to the AI assistant");
                return;
            }

            String body;
            try {
                body = readBody(request, maxRequestBytes(clientId));
            } catch (PayloadTooLargeException e) {
                sendPlain(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, e.getMessage());
                return;
            }

            McpToolCatalog catalog = McpToolCatalog.of(new ERPTools(provider, ctx), ragTools(ctx));
            String caller = who(ctx);
            JSONObject reply = protocol.handle(body, catalog, (tool, success, durationMs, error) ->
                log.warning("MCP tools/call " + tool + " " + (success ? "OK" : "ERROR") + " " + durationMs + "ms "
                    + caller + (error != null ? " " + error : "")));

            if (reply == null) {
                response.setStatus(HttpServletResponse.SC_ACCEPTED);
                return;
            }
            int status = isMalformed(reply) ? HttpServletResponse.SC_BAD_REQUEST : HttpServletResponse.SC_OK;
            sendJson(response, status, reply);
        } catch (Exception e) {
            log.log(Level.SEVERE, "MCP request failed " + who(ctx), e);
            sendJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                McpProtocol.error(null, McpProtocol.INTERNAL_ERROR, "Internal error"));
        } finally {
            ServerContext.dispose();
        }
    }

    @Override
    public void destroy() {
        if (requestContextTracker != null) {
            requestContextTracker.close();
        }
        if (ragTracker != null) {
            ragTracker.close();
        }
    }

    /** No server-initiated SSE stream (allowed by the Streamable HTTP transport). */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Allow", "POST");
        sendPlain(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Use POST");
    }

    /** Stateless server: there is no session to end. */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Allow", "POST");
        sendPlain(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "No sessions");
    }

    private Properties resolveContext(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            throw new SecurityException("no Authorization header");
        }
        RequestContextService service = requestContextTracker != null ? requestContextTracker.getService() : null;
        if (service == null) {
            throw new SecurityException("RequestContextService unavailable");
        }
        String remoteIp = request.getHeader("X-Forwarded-For");
        if (remoteIp == null) {
            remoteIp = request.getRemoteAddr();
        }
        return service.resolve(authHeader, new RequestSignals(remoteIp, request.getRemoteHost(), null, null, null));
    }

    private RagTools ragTools(Properties ctx) {
        IRagService rag = ragTracker != null ? ragTracker.getService() : null;
        return rag != null && rag.isAvailable() ? new RagTools(rag, ctx) : null;
    }

    private static boolean isAllowedOrigin(String origin) {
        String allowed = MSysConfig.getValue(SYSCONFIG_ALLOWED_ORIGINS, "");
        return Arrays.stream(allowed.split(","))
            .map(String::trim)
            .anyMatch(o -> !o.isEmpty() && o.equalsIgnoreCase(origin));
    }

    private static int maxRequestBytes(int clientId) {
        return MSysConfig.getIntValue(SYSCONFIG_MAX_REQUEST_BYTES, DEFAULT_MAX_REQUEST_BYTES, clientId);
    }

    private static String readBody(HttpServletRequest request, int maxBytes) throws IOException {
        if (request.getContentLength() > maxBytes) {
            throw new PayloadTooLargeException("Request body exceeds " + maxBytes + " bytes");
        }
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[8192];
        BufferedReader reader = request.getReader();
        int read;
        while ((read = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, read);
            if (sb.length() > maxBytes) {
                throw new PayloadTooLargeException("Request body exceeds " + maxBytes + " bytes");
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

    private static String who(Properties ctx) {
        return "user=" + Env.getAD_User_ID(ctx) + " role=" + Env.getAD_Role_ID(ctx) + " client=" + Env.getAD_Client_ID(ctx);
    }

    private static void sendJson(HttpServletResponse response, int status, JSONObject body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body.toString());
    }

    private static void sendPlain(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(message);
    }

    private static BundleContext bundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(McpServlet.class);
        return bundle != null ? bundle.getBundleContext() : null;
    }

    private static String bundleVersion() {
        Bundle bundle = FrameworkUtil.getBundle(McpServlet.class);
        return bundle != null ? bundle.getVersion().toString() : "dev";
    }

    private static final class PayloadTooLargeException extends IOException {
        private static final long serialVersionUID = 20261104L;

        PayloadTooLargeException(String message) {
            super(message);
        }
    }
}
