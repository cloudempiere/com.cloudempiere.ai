package com.cloudempiere.ai.mcp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.compiere.util.CLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.service.tool.DefaultToolExecutor;

/**
 * The MCP view of LangChain4j {@code @Tool} objects (CLD-2542).
 *
 * <p>Tool names, descriptions and JSON input schemas come from the same annotations the chat agent uses, through
 * {@link ToolSpecifications}, and calls run through {@link DefaultToolExecutor}, so the chat and MCP clients see
 * one definition of every tool. The tool objects are bound to the caller's context (for example
 * {@code new ERPTools(provider, ctx)}), so a catalog is built per request.
 */
public final class McpToolCatalog {

    private static final CLogger log = CLogger.getCLogger(McpToolCatalog.class);

    /** Memory id passed to the executor; MCP calls have no conversation memory. */
    private static final String MEMORY_ID = "mcp";

    private final Map<String, Entry> tools;

    private McpToolCatalog(Map<String, Entry> tools) {
        this.tools = tools;
    }

    /**
     * Build a catalog from tool objects. Methods annotated with {@link Tool} become MCP tools; when two objects
     * declare the same tool name, the first one wins.
     *
     * @param toolObjects tool objects; null entries are skipped
     */
    public static McpToolCatalog of(Object... toolObjects) {
        Map<String, Entry> tools = new LinkedHashMap<>();
        for (Object toolObject : toolObjects) {
            if (toolObject == null) {
                continue;
            }
            List<Method> methods = new ArrayList<>();
            for (Method method : toolObject.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    methods.add(method);
                }
            }
            // getDeclaredMethods() has no defined order: sort for a stable tools/list
            methods.sort((a, b) -> a.getName().compareTo(b.getName()));
            for (Method method : methods) {
                ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
                if (tools.containsKey(spec.name())) {
                    log.warning("MCP tool '" + spec.name() + "' of " + toolObject.getClass().getName()
                            + " skipped: the name is already taken");
                    continue;
                }
                // Propagate failures: by default the executor returns the exception message as a normal result,
                // and the MCP client could not tell a failed call from a successful one (isError)
                DefaultToolExecutor executor = DefaultToolExecutor.builder()
                    .object(toolObject)
                    .originalMethod(method)
                    .methodToInvoke(method)
                    .wrapToolArgumentsExceptions(true)
                    .propagateToolExecutionExceptions(true)
                    .build();
                tools.put(spec.name(), new Entry(spec, executor));
            }
        }
        return new McpToolCatalog(Collections.unmodifiableMap(tools));
    }

    /** Tool names in list order. */
    public List<String> names() {
        return new ArrayList<>(tools.keySet());
    }

    public boolean has(String name) {
        return tools.containsKey(name);
    }

    /** The {@code tools} array of an MCP {@code tools/list} result. */
    public JSONArray toMcpTools() {
        JSONArray array = new JSONArray();
        for (Entry entry : tools.values()) {
            JSONObject tool = new JSONObject();
            tool.put("name", entry.spec.name());
            if (entry.spec.description() != null) {
                tool.put("description", entry.spec.description());
            }
            tool.put("inputSchema", inputSchema(entry.spec));
            array.put(tool);
        }
        return array;
    }

    private static JSONObject inputSchema(ToolSpecification spec) {
        if (spec.parameters() == null) {
            // MCP requires an object schema even for tools without parameters
            return new JSONObject().put("type", "object").put("properties", new JSONObject());
        }
        Map<String, Object> schema = JsonSchemaElementUtils.toMap(spec.parameters());
        return new JSONObject(schema);
    }

    /**
     * Run a tool.
     *
     * @param name      tool name; must exist ({@link #has(String)})
     * @param arguments the {@code arguments} object of {@code tools/call}; null for none
     * @return the tool's text result
     * @throws McpToolException when the tool or its argument parsing fails; the message is meant for the client
     */
    public String call(String name, JSONObject arguments) throws McpToolException {
        Entry entry = tools.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .id(UUID.randomUUID().toString())
            .name(name)
            .arguments(arguments != null ? arguments.toString() : "{}")
            .build();
        try {
            return entry.executor.execute(request, MEMORY_ID);
        } catch (Exception e) {
            throw new McpToolException(rootMessage(e), e);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable cause = e;
        while ((cause instanceof InvocationTargetException || cause.getMessage() == null) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message != null ? message : cause.getClass().getSimpleName();
    }

    /** A tool failed; reported to the client as a tool result with {@code isError: true}. */
    public static final class McpToolException extends Exception {
        private static final long serialVersionUID = 20261104L;

        McpToolException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class Entry {
        final ToolSpecification spec;
        final DefaultToolExecutor executor;

        Entry(ToolSpecification spec, DefaultToolExecutor executor) {
            this.spec = spec;
            this.executor = executor;
        }
    }
}
