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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.mcp.McpProtocol;
import com.cloudempiere.ai.mcp.McpToolCatalog;
import com.cloudempiere.ai.test.categories.UnitTest;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * CLD-2542: MCP JSON-RPC handling and the MCP view of {@code @Tool} objects.
 */
@UnitTest
public class McpProtocolTest {

    /** Stand-in for ERPTools: the same annotations. */
    public static class FakeTools {
        @Tool("Look up a business partner by its search key")
        public String getBusinessPartner(@P("Search key of the business partner") String value) {
            return "{\"value\":\"" + value + "\",\"name\":\"Joe Block\"}";
        }

        @Tool("List the tables you can read")
        public String listTables() {
            return "[\"C_BPartner\",\"C_Order\"]";
        }

        @Tool("Count rows of a table")
        public String countRows(@P("Table name") String tableName, @P("Maximum to count") int limit) {
            return tableName + ":" + limit;
        }

        @Tool("Always fails")
        public String broken() {
            throw new IllegalStateException("Table C_Secret is not accessible for your role");
        }

        public String notATool() {
            return "hidden";
        }
    }

    /** Declares a tool name that FakeTools already has. */
    public static class DuplicateTools {
        @Tool("Other listTables")
        public String listTables() {
            return "duplicate";
        }
    }

    private final McpProtocol protocol = new McpProtocol("14.0.0.test", "Use the tools");
    private final McpToolCatalog catalog = McpToolCatalog.of(new FakeTools(), null, new DuplicateTools());
    private final List<String> calls = new ArrayList<>();
    private final McpProtocol.ToolCallListener listener =
        (tool, success, durationMs, error) -> calls.add(tool + ":" + (success ? "ok" : "error"));

    private JSONObject send(String body) {
        return protocol.handle(body, catalog, listener);
    }

    private JSONObject request(Object id, String method, JSONObject params) {
        JSONObject message = new JSONObject().put("jsonrpc", "2.0").put("id", id).put("method", method);
        if (params != null) {
            message.put("params", params);
        }
        return send(message.toString());
    }

    // ------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------

    @Test
    public void initializeEchoesASupportedProtocolVersion() {
        JSONObject reply = request(1, "initialize", new JSONObject()
            .put("protocolVersion", "2025-06-18")
            .put("capabilities", new JSONObject())
            .put("clientInfo", new JSONObject().put("name", "claude-code").put("version", "2.0")));

        assertThat(reply.getString("jsonrpc")).isEqualTo("2.0");
        assertThat(reply.getInt("id")).isEqualTo(1);
        JSONObject result = reply.getJSONObject("result");
        assertThat(result.getString("protocolVersion")).isEqualTo("2025-06-18");
        assertThat(result.getJSONObject("capabilities").getJSONObject("tools").getBoolean("listChanged")).isFalse();
        assertThat(result.getJSONObject("serverInfo").getString("name")).isEqualTo(McpProtocol.SERVER_NAME);
        assertThat(result.getJSONObject("serverInfo").getString("version")).isEqualTo("14.0.0.test");
        assertThat(result.getString("instructions")).isEqualTo("Use the tools");
    }

    @Test
    public void initializeAnswersWithTheLatestVersionForAnUnknownOne() {
        JSONObject reply = request("init", "initialize", new JSONObject().put("protocolVersion", "2099-01-01"));
        assertThat(reply.getString("id")).isEqualTo("init");
        assertThat(reply.getJSONObject("result").getString("protocolVersion")).isEqualTo(McpProtocol.LATEST_VERSION);
    }

    @Test
    public void notificationsGetNoReply() {
        assertThat(send("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")).isNull();
        assertThat(send("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/cancelled\",\"params\":{\"requestId\":3}}"))
            .isNull();
    }

    @Test
    public void clientResponsesGetNoReply() {
        assertThat(send("{\"jsonrpc\":\"2.0\",\"id\":5,\"result\":{}}")).isNull();
    }

    @Test
    public void pingReturnsAnEmptyResult() {
        JSONObject reply = request(7, "ping", null);
        assertThat(reply.getJSONObject("result").length()).isZero();
    }

    // ------------------------------------------------------------------------
    // tools/list
    // ------------------------------------------------------------------------

    @Test
    public void toolsListShowsAnnotatedMethodsOnlyWithTheirSchemas() {
        JSONArray tools = request(2, "tools/list", null).getJSONObject("result").getJSONArray("tools");

        List<String> names = new ArrayList<>();
        for (int i = 0; i < tools.length(); i++) {
            names.add(tools.getJSONObject(i).getString("name"));
        }
        // sorted per object; DuplicateTools.listTables dropped; notATool not a tool
        assertThat(names).containsExactly("broken", "countRows", "getBusinessPartner", "listTables");

        JSONObject bp = tools.getJSONObject(2);
        assertThat(bp.getString("description")).isEqualTo("Look up a business partner by its search key");
        JSONObject schema = bp.getJSONObject("inputSchema");
        assertThat(schema.getString("type")).isEqualTo("object");
        JSONObject property = schema.getJSONObject("properties").getJSONObject("arg0");
        assertThat(property.getString("type")).isEqualTo("string");
        assertThat(property.getString("description")).isEqualTo("Search key of the business partner");
        assertThat(schema.getJSONArray("required").toList()).containsExactly("arg0");

        JSONObject count = tools.getJSONObject(1).getJSONObject("inputSchema").getJSONObject("properties");
        assertThat(count.getJSONObject("arg1").getString("type")).isEqualTo("integer");
    }

    @Test
    public void toolWithoutParametersStillHasAnObjectSchema() {
        JSONArray tools = request(2, "tools/list", null).getJSONObject("result").getJSONArray("tools");
        JSONObject schema = tools.getJSONObject(3).getJSONObject("inputSchema");
        assertThat(schema.getString("type")).isEqualTo("object");
        assertThat(schema.has("properties")).isTrue();
    }

    // ------------------------------------------------------------------------
    // tools/call
    // ------------------------------------------------------------------------

    @Test
    public void toolsCallReturnsTheToolTextAndReportsTheCall() {
        JSONObject reply = request(3, "tools/call", new JSONObject()
            .put("name", "getBusinessPartner")
            .put("arguments", new JSONObject().put("arg0", "JoeBlock")));

        JSONObject result = reply.getJSONObject("result");
        assertThat(result.getBoolean("isError")).isFalse();
        JSONObject content = result.getJSONArray("content").getJSONObject(0);
        assertThat(content.getString("type")).isEqualTo("text");
        assertThat(content.getString("text")).isEqualTo("{\"value\":\"JoeBlock\",\"name\":\"Joe Block\"}");
        assertThat(calls).containsExactly("getBusinessPartner:ok");
    }

    @Test
    public void toolsCallConvertsArgumentTypes() {
        JSONObject reply = request(3, "tools/call", new JSONObject()
            .put("name", "countRows")
            .put("arguments", new JSONObject().put("arg0", "C_Order").put("arg1", 25)));
        assertThat(reply.getJSONObject("result").getJSONArray("content").getJSONObject(0).getString("text"))
            .isEqualTo("C_Order:25");
    }

    @Test
    public void toolsCallWithoutArgumentsWorksForParameterlessTools() {
        JSONObject reply = request(3, "tools/call", new JSONObject().put("name", "listTables"));
        assertThat(reply.getJSONObject("result").getJSONArray("content").getJSONObject(0).getString("text"))
            .isEqualTo("[\"C_BPartner\",\"C_Order\"]");
    }

    @Test
    public void failingToolIsAToolResultWithIsErrorNotAProtocolError() {
        JSONObject reply = request(4, "tools/call", new JSONObject().put("name", "broken"));

        assertThat(reply.has("error")).isFalse();
        JSONObject result = reply.getJSONObject("result");
        assertThat(result.getBoolean("isError")).isTrue();
        assertThat(result.getJSONArray("content").getJSONObject(0).getString("text"))
            .isEqualTo("Error: Table C_Secret is not accessible for your role");
        assertThat(calls).containsExactly("broken:error");
    }

    @Test
    public void unparseableArgumentIsAToolError() {
        JSONObject reply = request(4, "tools/call", new JSONObject()
            .put("name", "countRows")
            .put("arguments", new JSONObject().put("arg0", "C_Order").put("arg1", "lots")));
        assertThat(reply.has("error")).isFalse();
        assertThat(reply.getJSONObject("result").getBoolean("isError")).isTrue();
        assertThat(calls).containsExactly("countRows:error");
    }

    @Test
    public void unknownToolIsInvalidParams() {
        JSONObject reply = request(5, "tools/call", new JSONObject().put("name", "dropDatabase"));
        assertThat(reply.getJSONObject("error").getInt("code")).isEqualTo(McpProtocol.INVALID_PARAMS);
        assertThat(calls).isEmpty();
    }

    @Test
    public void missingToolNameAndNonObjectArgumentsAreInvalidParams() {
        assertThat(request(6, "tools/call", new JSONObject()).getJSONObject("error").getInt("code"))
            .isEqualTo(McpProtocol.INVALID_PARAMS);
        JSONObject reply = request(6, "tools/call", new JSONObject().put("name", "listTables").put("arguments", "x"));
        assertThat(reply.getJSONObject("error").getInt("code")).isEqualTo(McpProtocol.INVALID_PARAMS);
    }

    // ------------------------------------------------------------------------
    // Malformed messages
    // ------------------------------------------------------------------------

    @Test
    public void unknownMethodIsMethodNotFound() {
        JSONObject reply = request(8, "resources/list", null);
        assertThat(reply.getInt("id")).isEqualTo(8);
        assertThat(reply.getJSONObject("error").getInt("code")).isEqualTo(McpProtocol.METHOD_NOT_FOUND);
    }

    @Test
    public void invalidJsonIsAParseErrorWithNullId() {
        JSONObject reply = send("{not json");
        assertThat(reply.get("id")).isEqualTo(JSONObject.NULL);
        assertThat(reply.getJSONObject("error").getInt("code")).isEqualTo(McpProtocol.PARSE_ERROR);
    }

    @Test
    public void batchesAreRejected() {
        JSONObject reply = send("[{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}]");
        assertThat(reply.getJSONObject("error").getInt("code")).isEqualTo(McpProtocol.INVALID_REQUEST);
    }

    @Test
    public void wrongJsonRpcVersionIsInvalidRequest() {
        JSONObject reply = send("{\"jsonrpc\":\"1.0\",\"id\":1,\"method\":\"ping\"}");
        assertThat(reply.getJSONObject("error").getInt("code")).isEqualTo(McpProtocol.INVALID_REQUEST);
    }

    @Test
    public void nonScalarIdIsInvalidRequest() {
        JSONObject reply = send("{\"jsonrpc\":\"2.0\",\"id\":{\"a\":1},\"method\":\"ping\"}");
        assertThat(reply.getJSONObject("error").getInt("code")).isEqualTo(McpProtocol.INVALID_REQUEST);
    }
}
