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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.mcp.McpProtocol;
import com.cloudempiere.ai.mcp.McpRequestHandler;
import com.cloudempiere.ai.mcp.McpToolCatalog;
import com.cloudempiere.ai.test.categories.UnitTest;

import dev.langchain4j.agent.tool.Tool;

/**
 * CLD-2542: the checks of the /ai-mcp endpoint, in order, and the HTTP status of each outcome.
 */
@UnitTest
public class McpRequestHandlerTest {

    private static final String TOKEN = "Bearer good-token";
    private static final String PING = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}";

    public static class Tools {
        @Tool("List the tables you can read")
        public String listTables() {
            return "[\"C_BPartner\"]";
        }
    }

    /** Fake running server; records what the handler asked for. */
    private final class FakeEnvironment implements McpRequestHandler.Environment {
        boolean enabled = true;
        String origins = "";
        int maxBytes = 1_000;
        boolean aiAccess = true;
        RuntimeException authFailure;
        RuntimeException catalogFailure;
        final List<String> calls = new ArrayList<>();
        String seenRemoteIp;
        int binds;
        int unbinds;

        @Override
        public Properties authenticate(String authorizationHeader, String remoteIp, String remoteHost) {
            calls.add("authenticate");
            seenRemoteIp = remoteIp;
            if (authFailure != null) {
                throw authFailure;
            }
            if (!TOKEN.equals(authorizationHeader)) {
                throw new SecurityException("Token is revoked");
            }
            Properties ctx = new Properties();
            ctx.setProperty("#AD_Client_ID", "1000036");
            ctx.setProperty("#AD_Role_ID", "1000229");
            ctx.setProperty("#AD_User_ID", "1126641");
            return ctx;
        }

        @Override
        public boolean isEnabled(Properties ctx) {
            calls.add("isEnabled");
            return enabled;
        }

        @Override
        public String allowedOrigins() {
            calls.add("allowedOrigins");
            return origins;
        }

        @Override
        public int maxRequestBytes(Properties ctx) {
            return maxBytes;
        }

        @Override
        public McpToolCatalog catalog(Properties ctx) {
            calls.add("catalog");
            if (catalogFailure != null) {
                throw catalogFailure;
            }
            return aiAccess ? McpToolCatalog.of(new Tools()) : null;
        }

        @Override
        public void bind(Properties ctx) {
            binds++;
        }

        @Override
        public void unbind() {
            unbinds++;
        }
    }

    private static final class FakeRequest implements McpRequestHandler.Request {
        final Map<String, String> headers = new HashMap<>();
        String body = PING;
        int contentLength = -1;

        FakeRequest header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        FakeRequest body(String value) {
            body = value;
            return this;
        }

        @Override
        public String header(String name) {
            return headers.get(name);
        }

        @Override
        public String remoteAddr() {
            return "10.0.0.5";
        }

        @Override
        public String remoteHost() {
            return "client.local";
        }

        @Override
        public int contentLength() {
            return contentLength;
        }

        @Override
        public Reader body() throws IOException {
            return new StringReader(body);
        }
    }

    private FakeEnvironment env;
    private McpRequestHandler handler;

    @BeforeEach
    public void setUp() {
        env = new FakeEnvironment();
        handler = new McpRequestHandler(new McpProtocol("14.0.0.test", null), env);
    }

    private FakeRequest authorized() {
        return new FakeRequest().header("Authorization", TOKEN);
    }

    // ------------------------------------------------------------------------
    // 1. Origin
    // ------------------------------------------------------------------------

    @Test
    public void unknownOriginIsRejectedBeforeAuthentication() {
        McpRequestHandler.Response response = handler.handlePost(authorized().header("Origin", "https://evil.example"));
        assertThat(response.status()).isEqualTo(403);
        assertThat(response.body()).isEqualTo("Origin not allowed");
        assertThat(env.calls).doesNotContain("authenticate");
        assertThat(env.binds).isZero();
    }

    @Test
    public void listedOriginIsAcceptedCaseInsensitively() {
        env.origins = " https://app.example.com , https://inspector.local ";
        McpRequestHandler.Response response = handler.handlePost(authorized().header("Origin", "HTTPS://Inspector.Local"));
        assertThat(response.status()).isEqualTo(200);
    }

    @Test
    public void noOriginHeaderNeedsNoAllowList() {
        assertThat(handler.handlePost(authorized()).status()).isEqualTo(200);
        assertThat(env.calls).doesNotContain("allowedOrigins");
    }

    // ------------------------------------------------------------------------
    // 2. Authentication
    // ------------------------------------------------------------------------

    @Test
    public void missingTokenIs401WithChallengeWithoutAskingTheResolver() {
        McpRequestHandler.Response response = handler.handlePost(new FakeRequest());
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.headers()).containsEntry("WWW-Authenticate", McpRequestHandler.WWW_AUTHENTICATE);
        assertThat(env.calls).doesNotContain("authenticate");
    }

    @Test
    public void blankTokenIs401() {
        assertThat(handler.handlePost(new FakeRequest().header("Authorization", "  ")).status()).isEqualTo(401);
    }

    @Test
    public void rejectedTokenIs401AndNothingIsBound() {
        McpRequestHandler.Response response = handler.handlePost(new FakeRequest().header("Authorization", "Bearer bad"));
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.body()).isEqualTo("Authentication required");
        assertThat(env.binds).isZero();
        assertThat(env.calls).containsExactly("authenticate");
    }

    @Test
    public void unexpectedResolverFailureIs500NotA401() {
        env.authFailure = new IllegalStateException("identity bundle not ready");
        McpRequestHandler.Response response = handler.handlePost(authorized());
        assertThat(response.status()).isEqualTo(500);
        assertThat(new JSONObject(response.body()).getJSONObject("error").getInt("code"))
            .isEqualTo(McpProtocol.INTERNAL_ERROR);
    }

    @Test
    public void forwardedForIsPassedAsRemoteIp() {
        handler.handlePost(authorized().header("X-Forwarded-For", "203.0.113.7"));
        assertThat(env.seenRemoteIp).isEqualTo("203.0.113.7");
        handler.handlePost(authorized());
        assertThat(env.seenRemoteIp).isEqualTo("10.0.0.5");
    }

    // ------------------------------------------------------------------------
    // 3. Switch, 4. AI access, 5. size
    // ------------------------------------------------------------------------

    @Test
    public void switchedOffIs404AndTheContextIsReleased() {
        env.enabled = false;
        McpRequestHandler.Response response = handler.handlePost(authorized());
        assertThat(response.status()).isEqualTo(404);
        assertThat(env.calls).doesNotContain("catalog");
        assertThat(env.binds).isEqualTo(1);
        assertThat(env.unbinds).isEqualTo(1);
    }

    @Test
    public void noAiAccessIs403() {
        env.aiAccess = false;
        McpRequestHandler.Response response = handler.handlePost(authorized());
        assertThat(response.status()).isEqualTo(403);
        assertThat(response.body()).isEqualTo("You do not have access to the AI assistant");
        assertThat(env.unbinds).isEqualTo(1);
    }

    @Test
    public void declaredLengthOverTheLimitIs413WithoutReading() {
        FakeRequest request = authorized();
        request.contentLength = 5_000;
        request.body = null; // reading would fail
        McpRequestHandler.Response response = handler.handlePost(request);
        assertThat(response.status()).isEqualTo(413);
    }

    @Test
    public void streamedBodyOverTheLimitIs413() {
        env.maxBytes = 50;
        McpRequestHandler.Response response = handler.handlePost(authorized().body(
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\",\"params\":{\"pad\":\"" + "x".repeat(100) + "\"}}"));
        assertThat(response.status()).isEqualTo(413);
        assertThat(response.body()).contains("50");
    }

    // ------------------------------------------------------------------------
    // JSON-RPC outcomes
    // ------------------------------------------------------------------------

    @Test
    public void requestIs200WithJsonReplyAndTheContextIsBoundOnce() {
        McpRequestHandler.Response response = handler.handlePost(authorized());
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("application/json");
        assertThat(new JSONObject(response.body()).getInt("id")).isEqualTo(1);
        assertThat(env.binds).isEqualTo(1);
        assertThat(env.unbinds).isEqualTo(1);
        assertThat(env.calls).containsExactly("authenticate", "isEnabled", "catalog");
    }

    @Test
    public void toolCallRunsTheCallersCatalog() {
        McpRequestHandler.Response response = handler.handlePost(authorized().body(
            "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{\"name\":\"listTables\"}}"));
        JSONObject result = new JSONObject(response.body()).getJSONObject("result");
        assertThat(result.getBoolean("isError")).isFalse();
        assertThat(result.getJSONArray("content").getJSONObject(0).getString("text")).isEqualTo("[\"C_BPartner\"]");
    }

    @Test
    public void notificationIs202WithoutBody() {
        McpRequestHandler.Response response = handler.handlePost(authorized().body(
            "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));
        assertThat(response.status()).isEqualTo(202);
        assertThat(response.body()).isNull();
        assertThat(response.contentType()).isNull();
    }

    @Test
    public void malformedJsonIs400() {
        assertThat(handler.handlePost(authorized().body("{not json")).status()).isEqualTo(400);
        assertThat(handler.handlePost(authorized().body("[" + PING + "]")).status()).isEqualTo(400);
    }

    @Test
    public void unknownMethodIsA200JsonRpcError() {
        McpRequestHandler.Response response = handler.handlePost(authorized().body(
            "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"resources/list\"}"));
        assertThat(response.status()).isEqualTo(200);
        assertThat(new JSONObject(response.body()).getJSONObject("error").getInt("code"))
            .isEqualTo(McpProtocol.METHOD_NOT_FOUND);
    }

    @Test
    public void failureAfterBindingIs500AndTheContextIsStillReleased() {
        env.catalogFailure = new IllegalStateException("database down");
        McpRequestHandler.Response response = handler.handlePost(authorized());
        assertThat(response.status()).isEqualTo(500);
        assertThat(env.binds).isEqualTo(1);
        assertThat(env.unbinds).isEqualTo(1);
    }
}
