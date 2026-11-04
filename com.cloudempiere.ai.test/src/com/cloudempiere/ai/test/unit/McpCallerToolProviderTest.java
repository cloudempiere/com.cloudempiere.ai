/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2026 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.provider.langchain4j.mcp.MAIMcpServerConfig;
import com.cloudempiere.ai.provider.langchain4j.mcp.MCPClientCache.Connection;
import com.cloudempiere.ai.provider.langchain4j.mcp.McpCallerToolProvider;
import com.cloudempiere.ai.test.categories.UnitTest;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

/**
 * CLD-2543: external MCP tools per caller role, and the audit of every external tool call.
 */
@UnitTest
public class McpCallerToolProviderTest {

    private static final int SALES = 1000003;
    private static final int WAREHOUSE = 1000229;

    private final List<String> audit = new ArrayList<>();
    private final AtomicLong now = new AtomicLong(5_000L);
    private final McpCallerToolProvider.ToolCallAudit recorder = (server, tool, args, success, ms, error) ->
        audit.add(server + "." + tool + " " + (success ? "OK" : "ERROR") + " " + ms + "ms " + args
            + (error != null ? " " + error : ""));

    /** A fake MCP client offering the named tools; "fail_*" tools return an error result. */
    private Connection server(String name, Set<Integer> roles, String... tools) {
        List<ToolSpecification> specs = new ArrayList<>();
        for (String tool : tools) {
            specs.add(ToolSpecification.builder().name(tool).description(tool + " of " + name).build());
        }
        McpClient client = (McpClient) Proxy.newProxyInstance(McpClient.class.getClassLoader(),
            new Class<?>[] { McpClient.class }, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "key":
                        return name;
                    case "listTools":
                        return specs;
                    case "executeTool": {
                        ToolExecutionRequest request = (ToolExecutionRequest) args[0];
                        now.addAndGet(42);
                        if (request.name().startsWith("throw_")) {
                            throw new IllegalStateException("server crashed");
                        }
                        boolean fail = request.name().startsWith("fail_");
                        return ToolExecutionResult.builder()
                            .isError(fail)
                            .resultText(fail ? "remote failure" : name + ":" + request.name() + ":" + request.arguments())
                            .build();
                    }
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    case "toString":
                        return "FakeMcpClient[" + name + "]";
                    default:
                        return null;
                }
            });
        return new Connection(MAIMcpServerConfig.http(name, "https://" + name, Map.of(), roles, null), client);
    }

    private static ToolProviderRequest request() {
        return new ToolProviderRequest("memory-1", UserMessage.from("hi"));
    }

    private static List<String> names(ToolProviderResult result) {
        List<String> names = new ArrayList<>();
        for (ToolSpecification spec : result.tools().keySet()) {
            names.add(spec.name());
        }
        names.sort(String::compareTo);
        return names;
    }

    private static ToolExecutor executor(ToolProviderResult result, String name) {
        return result.toolExecutorByName(name);
    }

    @Test
    public void callerSeesOnlyServersAllowedForTheRole() {
        List<Connection> connections = List.of(
            server("github", Set.of(SALES), "create_issue"),
            server("weather", Set.of(), "forecast"));

        McpCallerToolProvider sales = new McpCallerToolProvider(connections, SALES, recorder, now::get);
        McpCallerToolProvider warehouse = new McpCallerToolProvider(connections, WAREHOUSE, recorder, now::get);

        assertThat(sales.serverNames()).containsExactly("github", "weather");
        assertThat(names(sales.provideTools(request()))).containsExactly("create_issue", "forecast");
        assertThat(warehouse.serverNames()).containsExactly("weather");
        assertThat(names(warehouse.provideTools(request()))).containsExactly("forecast");
    }

    @Test
    public void noAllowedServerMeansNoServers() {
        McpCallerToolProvider tools = new McpCallerToolProvider(
            List.of(server("github", Set.of(SALES), "create_issue")), WAREHOUSE, recorder, now::get);
        assertThat(tools.hasServers()).isFalse();
        assertThat(tools.provideTools(request()).tools()).isEmpty();
    }

    @Test
    public void everyCallIsAuditedWithServerToolOutcomeAndDuration() {
        ToolProviderResult result = new McpCallerToolProvider(
            List.of(server("github", Set.of(), "create_issue", "fail_merge")), SALES, recorder, now::get)
            .provideTools(request());

        String ok = executor(result, "create_issue").execute(ToolExecutionRequest.builder().id("1")
            .name("create_issue").arguments("{\"title\":\"Bug\"}").build(), "memory-1");
        ToolExecutionResult failed = executor(result, "fail_merge").executeWithContext(ToolExecutionRequest.builder()
            .id("2").name("fail_merge").arguments("{}").build(), null);

        assertThat(ok).isEqualTo("github:create_issue:{\"title\":\"Bug\"}");
        assertThat(failed.isError()).isTrue();
        assertThat(audit).containsExactly(
            "github.create_issue OK 42ms {\"title\":\"Bug\"}",
            "github.fail_merge ERROR 42ms {} remote failure");
    }

    @Test
    public void throwingToolIsAuditedAsErrorThroughBothExecutePaths() {
        ToolProviderResult result = new McpCallerToolProvider(
            List.of(server("github", Set.of(), "throw_merge")), SALES, recorder, now::get).provideTools(request());
        ToolExecutor merge = executor(result, "throw_merge");
        ToolExecutionRequest call = ToolExecutionRequest.builder().id("1").name("throw_merge").arguments("{}").build();

        // McpToolProvider may turn the exception into an error result or let it through; either way it is an ERROR
        outcome(() -> merge.execute(call, "m"));
        outcome(() -> {
            ToolExecutionResult r = merge.executeWithContext(call, null);
            if (r != null && !r.isError()) {
                throw new AssertionError("a crashed tool must not look successful");
            }
            return r;
        });

        assertThat(audit).hasSize(2).allSatisfy(line -> assertThat(line).startsWith("github.throw_merge ERROR"));
    }

    private static void outcome(java.util.concurrent.Callable<?> call) {
        try {
            call.call();
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            // rethrown by the audited executor: fine
        }
    }

    @Test
    public void auditFailureDoesNotHideTheToolResult() {
        McpCallerToolProvider.ToolCallAudit broken = (s, t, a, ok, ms, e) -> {
            throw new IllegalStateException("database down");
        };
        ToolProviderResult result = new McpCallerToolProvider(
            List.of(server("weather", Set.of(), "forecast")), SALES, broken, now::get).provideTools(request());

        String text = executor(result, "forecast").execute(ToolExecutionRequest.builder().id("1").name("forecast")
            .arguments("{\"city\":\"Bratislava\"}").build(), "memory-1");
        assertThat(text).isEqualTo("weather:forecast:{\"city\":\"Bratislava\"}");
    }

    @Test
    public void duplicateToolNameKeepsTheFirstServer() {
        ToolProviderResult result = new McpCallerToolProvider(List.of(
            server("first", Set.of(), "search"),
            server("second", Set.of(), "search", "other")), SALES, recorder, now::get).provideTools(request());

        assertThat(names(result)).containsExactly("other", "search");
        assertThat(executor(result, "search").execute(ToolExecutionRequest.builder().id("1").name("search")
            .arguments("{}").build(), "m")).startsWith("first:");
    }

    @Test
    public void secretLookingArgumentsAreMaskedAtAnyDepth() {
        String masked = McpCallerToolProvider.maskArguments(
            "{\"query\":\"open orders\",\"apiKey\":\"k1\",\"nested\":{\"Authorization\":\"Bearer x\","
            + "\"items\":[{\"password\":\"p\",\"name\":\"n\"}]},\"accessToken\":\"t\"}");

        JSONObject json = new JSONObject(masked);
        assertThat(json.getString("query")).isEqualTo("open orders");
        assertThat(json.getString("apiKey")).isEqualTo(McpCallerToolProvider.MASK);
        assertThat(json.getString("accessToken")).isEqualTo(McpCallerToolProvider.MASK);
        assertThat(json.getJSONObject("nested").getString("Authorization")).isEqualTo(McpCallerToolProvider.MASK);
        JSONObject item = json.getJSONObject("nested").getJSONArray("items").getJSONObject(0);
        assertThat(item.getString("password")).isEqualTo(McpCallerToolProvider.MASK);
        assertThat(item.getString("name")).isEqualTo("n");
        assertThat(masked).doesNotContain("k1").doesNotContain("Bearer x");
    }

    @Test
    public void emptyAndNonJsonArgumentsAreKeptAndLongOnesTruncated() {
        assertThat(McpCallerToolProvider.maskArguments(null)).isEqualTo("{}");
        assertThat(McpCallerToolProvider.maskArguments("not json")).isEqualTo("not json");
        String longArgs = "{\"q\":\"" + "x".repeat(20_000) + "\"}";
        assertThat(McpCallerToolProvider.maskArguments(longArgs)).hasSize(10_001);
    }
}
