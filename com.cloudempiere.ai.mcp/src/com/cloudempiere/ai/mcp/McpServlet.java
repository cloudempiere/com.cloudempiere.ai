package com.cloudempiere.ai.mcp;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.adempiere.util.ServerContext;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
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
 * <p>The request logic and its checks live in {@link McpRequestHandler}; this class adapts HTTP and supplies the
 * running server: the token is resolved by {@link RequestContextService} (ADR-0038, the same port GraphQL uses),
 * AI access is {@link MAIProvider#getForUser} (ADR-058), allowed origins and the size limit come from AD_SysConfig,
 * and the tools are those of the chat agent bound to the caller's context, so every database query runs under the
 * caller's role through {@code SecureDatabaseQueryExecutor}.
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

    static final int DEFAULT_MAX_REQUEST_BYTES = 1_048_576;

    static final String INSTRUCTIONS = "Tools of the CloudEmpiere iDempiere ERP AI assistant. "
        + "Every call runs with your iDempiere role: you see only the tables, columns and records it may read. "
        + "Use listTables and getTableMetadata to find tables and columns before writing a query.";

    private transient McpRequestHandler handler;
    private transient ServiceTracker<RequestContextService, RequestContextService> requestContextTracker;
    private transient ServiceTracker<IRagService, IRagService> ragTracker;

    @Override
    public void init() {
        BundleContext bc = bundleContext();
        if (bc != null) {
            requestContextTracker = new ServiceTracker<>(bc, RequestContextService.class, null);
            requestContextTracker.open();
            ragTracker = new ServiceTracker<>(bc, IRagService.class, null);
            ragTracker.open();
        } else {
            log.warning("AI MCP server: no bundle context, every request will be rejected");
        }
        handler = new McpRequestHandler(new McpProtocol(bundleVersion(), INSTRUCTIONS), new ServerEnvironment());
        log.warning("AI MCP server endpoint " + (SERVER_ENABLED ? "ready" : "disabled") + " at /ai-mcp");
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        McpRequestHandler.Response reply = handler.handlePost(new HttpRequestAdapter(request));
        response.setStatus(reply.status());
        for (Map.Entry<String, String> header : reply.headers().entrySet()) {
            response.setHeader(header.getKey(), header.getValue());
        }
        if (reply.body() != null) {
            response.setContentType(reply.contentType());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(reply.body());
        }
    }

    /** No server-initiated SSE stream (allowed by the Streamable HTTP transport). */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        methodNotAllowed(response, "Use POST");
    }

    /** Stateless server: there is no session to end. */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        methodNotAllowed(response, "No sessions");
    }

    private static void methodNotAllowed(HttpServletResponse response, String message) throws IOException {
        response.setHeader("Allow", "POST");
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(message);
    }

    /** The running server behind {@link McpRequestHandler}. */
    private final class ServerEnvironment implements McpRequestHandler.Environment {

        @Override
        public Properties authenticate(String authorizationHeader, String remoteIp, String remoteHost) {
            RequestContextService service = requestContextTracker != null ? requestContextTracker.getService() : null;
            if (service == null) {
                throw new SecurityException("RequestContextService unavailable");
            }
            return service.resolve(authorizationHeader, new RequestSignals(remoteIp, remoteHost, null, null, null));
        }

        @Override
        public boolean isEnabled(Properties ctx) {
            return SERVER_ENABLED;
        }

        @Override
        public String allowedOrigins() {
            return MSysConfig.getValue(SYSCONFIG_ALLOWED_ORIGINS, "");
        }

        @Override
        public int maxRequestBytes(Properties ctx) {
            return MSysConfig.getIntValue(SYSCONFIG_MAX_REQUEST_BYTES, DEFAULT_MAX_REQUEST_BYTES, Env.getAD_Client_ID(ctx));
        }

        @Override
        public McpToolCatalog catalog(Properties ctx) {
            MAIProvider provider = MAIProvider.getForUser(ctx, null);
            if (provider == null) {
                return null;
            }
            IRagService rag = ragTracker != null ? ragTracker.getService() : null;
            RagTools ragTools = rag != null && rag.isAvailable() ? new RagTools(rag, ctx) : null;
            return McpToolCatalog.of(new ERPTools(provider, ctx), ragTools);
        }

        @Override
        public void bind(Properties ctx) {
            ServerContext.setCurrentInstance(ctx);
        }

        @Override
        public void unbind() {
            ServerContext.dispose();
        }
    }

    private static final class HttpRequestAdapter implements McpRequestHandler.Request {
        private final HttpServletRequest request;

        HttpRequestAdapter(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public String header(String name) {
            return request.getHeader(name);
        }

        @Override
        public String remoteAddr() {
            return request.getRemoteAddr();
        }

        @Override
        public String remoteHost() {
            return request.getRemoteHost();
        }

        @Override
        public int contentLength() {
            return request.getContentLength();
        }

        @Override
        public Reader body() throws IOException {
            if (request.getCharacterEncoding() == null) {
                request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            }
            return request.getReader();
        }
    }

    private static BundleContext bundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(McpServlet.class);
        return bundle != null ? bundle.getBundleContext() : null;
    }

    private static String bundleVersion() {
        Bundle bundle = FrameworkUtil.getBundle(McpServlet.class);
        return bundle != null ? bundle.getVersion().toString() : "dev";
    }
}
