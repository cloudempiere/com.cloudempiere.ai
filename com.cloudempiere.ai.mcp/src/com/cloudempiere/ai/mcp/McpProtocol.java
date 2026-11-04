package com.cloudempiere.ai.mcp;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * MCP JSON-RPC handling for the tools capability, without transport (CLD-2542).
 *
 * <p>Implements {@code initialize}, {@code ping}, {@code tools/list} and {@code tools/call}; notifications are
 * accepted and ignored. Stateless: no session id, every request carries its own credentials, so it fits the
 * Streamable HTTP transport answering each POST with a single JSON response. Resources, prompts, sampling and
 * server-to-client requests are not offered.
 */
public final class McpProtocol {

    /** Protocol versions this server speaks, newest first. */
    public static final List<String> SUPPORTED_VERSIONS = List.of("2025-11-25", "2025-06-18", "2025-03-26");
    public static final String LATEST_VERSION = SUPPORTED_VERSIONS.get(0);

    public static final String SERVER_NAME = "cloudempiere-ai";

    // JSON-RPC 2.0 error codes
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    /** Observes tool calls, for audit logging. */
    @FunctionalInterface
    public interface ToolCallListener {
        void onToolCall(String tool, boolean success, long durationMs, String error);
    }

    private final String serverVersion;
    private final String instructions;

    /**
     * @param serverVersion version reported in {@code serverInfo}
     * @param instructions  optional usage instructions for the client's model; null for none
     */
    public McpProtocol(String serverVersion, String instructions) {
        this.serverVersion = serverVersion;
        this.instructions = instructions;
    }

    /**
     * Parse and handle one HTTP request body.
     *
     * @return the JSON-RPC response, or null when the body was a notification (or a client response) that gets
     *         no reply
     */
    public JSONObject handle(String body, McpToolCatalog catalog, ToolCallListener listener) {
        Object parsed;
        try {
            parsed = new org.json.JSONTokener(body).nextValue();
        } catch (JSONException e) {
            return error(null, PARSE_ERROR, "Parse error: " + e.getMessage());
        }
        if (!(parsed instanceof JSONObject)) {
            // JSON-RPC batches were removed from MCP in 2025-06-18
            return error(null, INVALID_REQUEST, "Expected a single JSON-RPC message object");
        }
        return handle((JSONObject) parsed, catalog, listener);
    }

    /** Handle one JSON-RPC message; see {@link #handle(String, McpToolCatalog, ToolCallListener)}. */
    public JSONObject handle(JSONObject message, McpToolCatalog catalog, ToolCallListener listener) {
        Object id = message.opt("id");
        boolean isRequest = message.has("method") && id != null && id != JSONObject.NULL;

        if (!"2.0".equals(message.optString("jsonrpc"))) {
            return isRequest ? error(id, INVALID_REQUEST, "jsonrpc must be \"2.0\"") : null;
        }
        if (!message.has("method")) {
            return null; // a response to a server request; this server sends none
        }
        if (!isRequest) {
            return null; // notification, e.g. notifications/initialized
        }
        if (!(id instanceof String) && !(id instanceof Number)) {
            return error(null, INVALID_REQUEST, "id must be a string or a number");
        }

        String method = message.optString("method");
        JSONObject params = message.optJSONObject("params");
        try {
            switch (method) {
                case "initialize":
                    return result(id, initialize(params));
                case "ping":
                    return result(id, new JSONObject());
                case "tools/list":
                    return result(id, new JSONObject().put("tools", catalog.toMcpTools()));
                case "tools/call":
                    return toolsCall(id, params, catalog, listener);
                default:
                    return error(id, METHOD_NOT_FOUND, "Method not found: " + method);
            }
        } catch (RuntimeException e) {
            return error(id, INTERNAL_ERROR, "Internal error");
        }
    }

    private JSONObject initialize(JSONObject params) {
        String requested = params != null ? params.optString("protocolVersion", null) : null;
        String version = requested != null && SUPPORTED_VERSIONS.contains(requested) ? requested : LATEST_VERSION;

        JSONObject result = new JSONObject()
            .put("protocolVersion", version)
            .put("capabilities", new JSONObject()
                .put("tools", new JSONObject().put("listChanged", false)))
            .put("serverInfo", new JSONObject()
                .put("name", SERVER_NAME)
                .put("version", serverVersion));
        if (instructions != null && !instructions.isEmpty()) {
            result.put("instructions", instructions);
        }
        return result;
    }

    private JSONObject toolsCall(Object id, JSONObject params, McpToolCatalog catalog, ToolCallListener listener) {
        String name = params != null ? params.optString("name", null) : null;
        if (name == null || name.isEmpty()) {
            return error(id, INVALID_PARAMS, "params.name is required");
        }
        if (!catalog.has(name)) {
            return error(id, INVALID_PARAMS, "Unknown tool: " + name);
        }
        Object rawArguments = params.opt("arguments");
        if (rawArguments != null && rawArguments != JSONObject.NULL && !(rawArguments instanceof JSONObject)) {
            return error(id, INVALID_PARAMS, "params.arguments must be an object");
        }
        JSONObject arguments = rawArguments instanceof JSONObject ? (JSONObject) rawArguments : null;

        long start = System.currentTimeMillis();
        String text;
        boolean isError;
        try {
            text = catalog.call(name, arguments);
            isError = false;
        } catch (McpToolCatalog.McpToolException e) {
            text = "Error: " + e.getMessage();
            isError = true;
        }
        if (listener != null) {
            listener.onToolCall(name, !isError, System.currentTimeMillis() - start, isError ? text : null);
        }

        JSONObject content = new JSONObject().put("type", "text").put("text", text != null ? text : "");
        return result(id, new JSONObject()
            .put("content", new JSONArray().put(content))
            .put("isError", isError));
    }

    static JSONObject result(Object id, JSONObject result) {
        return new JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", result);
    }

    /** A JSON-RPC error response; {@code id} null when the request id is unknown. */
    public static JSONObject error(Object id, int code, String message) {
        return new JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id != null ? id : JSONObject.NULL)
            .put("error", new JSONObject().put("code", code).put("message", message));
    }
}
