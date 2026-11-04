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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.provider.langchain4j.mcp.MAIMcpServerConfig;
import com.cloudempiere.ai.provider.langchain4j.mcp.MCPToolProviderFactory;
import com.cloudempiere.ai.provider.langchain4j.mcp.McpSecrets;
import com.cloudempiere.ai.provider.langchain4j.mcp.McpServiceToken;
import com.cloudempiere.ai.test.categories.UnitTest;

import dev.langchain4j.mcp.client.McpHeadersSupplier;

/**
 * CLD-2543: AIG_MCP_SERVERS parsing with ${env:NAME}, roles and service auth, and the client_credentials token.
 */
@UnitTest
public class McpClientConfigTest {

    private final Map<String, String> env = new HashMap<>(Map.of(
        "GITHUB_TOKEN", "ghp_secret",
        "TOOLS_ID", "client-123",
        "TOOLS_SECRET", "s3cr3t"));
    private final McpSecrets secrets = new McpSecrets(env::get);
    private final List<String> skipped = new ArrayList<>();

    private List<MAIMcpServerConfig> parse(String json) {
        return MCPToolProviderFactory.parseServerConfigs(json, secrets, skipped::add);
    }

    // ------------------------------------------------------------------------
    // ${env:NAME}
    // ------------------------------------------------------------------------

    @Test
    public void envReferencesAreResolvedEverywhere() {
        List<MAIMcpServerConfig> configs = parse("["
            + "{\"name\":\"github\",\"transport\":\"stdio\",\"command\":[\"npx\",\"--token=${env:GITHUB_TOKEN}\"],"
            + " \"env\":{\"GITHUB_PERSONAL_ACCESS_TOKEN\":\"${env:GITHUB_TOKEN}\"}},"
            + "{\"name\":\"tools\",\"transport\":\"http\",\"url\":\"https://mcp.example.com/${env:TOOLS_ID}\","
            + " \"headers\":{\"Authorization\":\"Bearer ${env:GITHUB_TOKEN}\",\"X-Plain\":\"plain\"}}]");

        assertThat(skipped).isEmpty();
        assertThat(configs.get(0).getEnvironment()).containsEntry("GITHUB_PERSONAL_ACCESS_TOKEN", "ghp_secret");
        assertThat(configs.get(0).getCommand()).containsExactly("npx", "--token=ghp_secret");
        assertThat(configs.get(1).getUrl()).isEqualTo("https://mcp.example.com/client-123");
        assertThat(configs.get(1).getHeaders())
            .containsEntry("Authorization", "Bearer ghp_secret")
            .containsEntry("X-Plain", "plain");
    }

    @Test
    public void unsetVariableSkipsOnlyThatServerAndNamesTheVariableNotAValue() {
        List<MAIMcpServerConfig> configs = parse("["
            + "{\"name\":\"broken\",\"transport\":\"http\",\"url\":\"https://x\",\"headers\":{\"A\":\"${env:MISSING}\"}},"
            + "{\"name\":\"ok\",\"transport\":\"http\",\"url\":\"https://y\"}]");

        assertThat(configs).extracting(MAIMcpServerConfig::getName).containsExactly("ok");
        assertThat(skipped).hasSize(1);
        assertThat(skipped.get(0)).contains("broken").contains("MISSING").doesNotContain("ghp_secret");
    }

    @Test
    public void secretsResolveDollarsAndBackslashesLiterally() {
        env.put("ODD", "a$1b\\c");
        assertThat(secrets.resolve("x=${env:ODD}")).isEqualTo("x=a$1b\\c");
        assertThat(secrets.resolve("no reference")).isEqualTo("no reference");
        assertThat(secrets.resolve(null)).isNull();
    }

    // ------------------------------------------------------------------------
    // roles
    // ------------------------------------------------------------------------

    @Test
    public void rolesLimitTheServerAndNoRolesMeansEveryone() {
        List<MAIMcpServerConfig> configs = parse("["
            + "{\"name\":\"limited\",\"transport\":\"http\",\"url\":\"https://x\",\"roles\":[1000003,\"1000229\"]},"
            + "{\"name\":\"open\",\"transport\":\"http\",\"url\":\"https://y\"}]");

        MAIMcpServerConfig limited = configs.get(0);
        assertThat(limited.getRoles()).containsExactlyInAnyOrder(1000003, 1000229);
        assertThat(limited.isAllowedFor(1000229)).isTrue();
        assertThat(limited.isAllowedFor(1000222)).isFalse();
        assertThat(configs.get(1).isAllowedFor(1000222)).isTrue();
    }

    @Test
    public void invalidRoleSkipsTheServer() {
        assertThat(parse("[{\"name\":\"bad\",\"transport\":\"http\",\"url\":\"https://x\",\"roles\":[\"admin\"]}]"))
            .isEmpty();
        assertThat(skipped).hasSize(1);
    }

    // ------------------------------------------------------------------------
    // auth
    // ------------------------------------------------------------------------

    @Test
    public void serviceAuthIsParsedWithResolvedCredentials() {
        MAIMcpServerConfig config = parse("[{\"name\":\"internal\",\"transport\":\"http\",\"url\":\"https://x\","
            + "\"auth\":{\"type\":\"service\",\"tokenUrl\":\"https://auth/oauth2/token\","
            + "\"clientId\":\"${env:TOOLS_ID}\",\"clientSecret\":\"${env:TOOLS_SECRET}\",\"scope\":\"api.read\"}}]").get(0);

        MAIMcpServerConfig.ServiceAuth auth = config.getServiceAuth();
        assertThat(auth).isNotNull();
        assertThat(auth.getTokenUrl()).isEqualTo("https://auth/oauth2/token");
        assertThat(auth.getClientId()).isEqualTo("client-123");
        assertThat(auth.getClientSecret()).isEqualTo("s3cr3t");
        assertThat(auth.getScope()).isEqualTo("api.read");
    }

    @Test
    public void staticOrNoAuthHasNoServiceToken() {
        List<MAIMcpServerConfig> configs = parse("["
            + "{\"name\":\"a\",\"transport\":\"http\",\"url\":\"https://x\",\"auth\":{\"type\":\"static\"}},"
            + "{\"name\":\"b\",\"transport\":\"http\",\"url\":\"https://y\"}]");
        assertThat(configs).allSatisfy(c -> assertThat(c.getServiceAuth()).isNull());
    }

    @Test
    public void unknownAuthTypeAndServiceAuthOnStdioAreSkipped() {
        assertThat(parse("["
            + "{\"name\":\"weird\",\"transport\":\"http\",\"url\":\"https://x\",\"auth\":{\"type\":\"magic\"}},"
            + "{\"name\":\"stdio-service\",\"command\":[\"npx\"],\"auth\":{\"type\":\"service\"}}]")).isEmpty();
        assertThat(skipped).hasSize(2);
        assertThat(skipped.get(0)).contains("magic");
        assertThat(skipped.get(1)).contains("http transport");
    }

    @Test
    public void notAnArrayIsAnErrorForTheWholeValue() {
        assertThatThrownBy(() -> parse("{\"name\":\"x\"}")).isInstanceOf(org.json.JSONException.class);
    }

    // ------------------------------------------------------------------------
    // client_credentials token
    // ------------------------------------------------------------------------

    private final MAIMcpServerConfig.ServiceAuth auth =
        new MAIMcpServerConfig.ServiceAuth("https://auth/oauth2/token", "client-123", "s3cr3t", "api.read");
    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final AtomicInteger fetches = new AtomicInteger();
    private final List<Map<String, String>> forms = new ArrayList<>();

    private McpServiceToken token(String... responses) {
        return new McpServiceToken(auth, (url, form) -> {
            forms.add(form);
            return responses[Math.min(fetches.getAndIncrement(), responses.length - 1)];
        }, now::get);
    }

    @Test
    public void tokenIsFetchedWithClientCredentialsAndCachedUntilShortlyBeforeExpiry() {
        McpServiceToken token = token("{\"access_token\":\"t1\",\"expires_in\":3600}",
            "{\"access_token\":\"t2\",\"expires_in\":3600}");

        assertThat(token.get()).isEqualTo("t1");
        assertThat(forms.get(0)).containsEntry("grant_type", "client_credentials")
            .containsEntry("client_id", "client-123").containsEntry("client_secret", "s3cr3t")
            .containsEntry("scope", "api.read");

        now.addAndGet(3600_000L - 61_000L);
        assertThat(token.get()).isEqualTo("t1");
        assertThat(fetches).hasValue(1);

        now.addAndGet(2_000L); // within the refresh margin
        assertThat(token.get()).isEqualTo("t2");
        assertThat(fetches).hasValue(2);
    }

    @Test
    public void tokenErrorNamesTheOAuthErrorButNoSecret() {
        McpServiceToken token = token("{\"error\":\"invalid_client\",\"error_description\":\"bad secret\"}");
        assertThatThrownBy(token::get)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("invalid_client")
            .hasMessageNotContaining("s3cr3t");
    }

    @Test
    public void tokenEndpointFailureIsReported() {
        McpServiceToken token = new McpServiceToken(auth, (url, form) -> {
            throw new IOException("connection refused");
        }, now::get);
        assertThatThrownBy(token::get).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("connection refused");
    }

    @Test
    public void nonJsonTokenResponseIsReportedWithoutTheBody() {
        McpServiceToken token = token("<html>gateway error s3cr3t</html>");
        assertThatThrownBy(token::get).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no JSON").hasMessageNotContaining("s3cr3t");
    }

    @Test
    public void missingOrZeroExpiresInUsesTheDefaultLifetime() {
        McpServiceToken token = token("{\"access_token\":\"t1\"}", "{\"access_token\":\"t2\",\"expires_in\":0}",
            "{\"access_token\":\"t3\"}");
        assertThat(token.get()).isEqualTo("t1");
        now.addAndGet(5 * 60_000L - 61_000L);
        assertThat(token.get()).isEqualTo("t1");
        now.addAndGet(2_000L);
        assertThat(token.get()).isEqualTo("t2"); // expires_in 0 -> default lifetime as well
        now.addAndGet(5 * 60_000L - 61_000L);
        assertThat(token.get()).isEqualTo("t2");
        assertThat(fetches).hasValue(2);
    }

    @Test
    public void noScopeSendsNoScopeParameter() {
        McpServiceToken token = new McpServiceToken(
            new MAIMcpServerConfig.ServiceAuth("https://auth/oauth2/token", "c", "s", null),
            (url, form) -> {
                forms.add(form);
                return "{\"access_token\":\"t\"}";
            }, now::get);
        token.get();
        assertThat(forms.get(0)).doesNotContainKey("scope");
    }

    @Test
    public void interruptedTokenRequestKeepsTheInterruptFlag() {
        McpServiceToken token = new McpServiceToken(auth, (url, form) -> {
            throw new InterruptedException("stop");
        }, now::get);
        try {
            assertThatThrownBy(token::get).isInstanceOf(IllegalStateException.class).hasMessageContaining("interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted(); // clear for the next test
        }
    }

    @Test
    public void serviceHeadersAddAFreshBearerToTheStaticHeadersOnEveryMessage() {
        McpServiceToken token = token("{\"access_token\":\"t1\",\"expires_in\":120}",
            "{\"access_token\":\"t2\",\"expires_in\":120}");
        McpHeadersSupplier headers = MCPToolProviderFactory.serviceHeaders(Map.of("X-Tenant", "1000036"), token);

        Map<String, String> first = headers.apply(null);
        assertThat(first).containsEntry("X-Tenant", "1000036").containsEntry("Authorization", "Bearer t1");
        now.addAndGet(120_000L);
        assertThat(headers.apply(null)).containsEntry("Authorization", "Bearer t2");
        assertThat(first).containsEntry("Authorization", "Bearer t1"); // a new map per message
    }
}
