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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.provider.langchain4j.mcp.MAIMcpServerConfig;
import com.cloudempiere.ai.provider.langchain4j.mcp.MCPClientCache;
import com.cloudempiere.ai.provider.langchain4j.mcp.MCPClientCache.Connection;
import com.cloudempiere.ai.test.categories.UnitTest;

import dev.langchain4j.mcp.client.McpClient;

/**
 * CLD-2540: MCP clients are connected once per AD_Client and closed, not leaked on every message.
 */
@UnitTest
public class MCPClientCacheTest {

    private static final int CLIENT_A = 11;
    private static final int CLIENT_B = 1000000;

    private static final String ONE_SERVER =
        "[{\"name\":\"github\",\"transport\":\"stdio\",\"command\":[\"npx\",\"server-github\"]}]";
    private static final String TWO_SERVERS =
        "[{\"name\":\"github\",\"transport\":\"stdio\",\"command\":[\"npx\",\"server-github\"]},"
        + "{\"name\":\"tools\",\"transport\":\"http\",\"url\":\"https://mcp.example.com/tools\"}]";

    private final List<String> connected = new ArrayList<>();
    private final List<String> closed = new ArrayList<>();
    private final Set<String> failing = new HashSet<>();
    private final AtomicLong now = new AtomicLong(1_000_000L);
    private MCPClientCache cache;

    @BeforeEach
    public void setUp() {
        cache = new MCPClientCache(this::fakeClient, now::get);
    }

    private McpClient fakeClient(MAIMcpServerConfig config) {
        if (failing.contains(config.getName())) {
            throw new IllegalStateException("connection refused");
        }
        connected.add(config.getName());
        return (McpClient) Proxy.newProxyInstance(McpClient.class.getClassLoader(),
            new Class<?>[] { McpClient.class }, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "key":
                        return config.getName();
                    case "close":
                        closed.add(config.getName());
                        return null;
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    case "toString":
                        return "FakeMcpClient[" + config.getName() + "]";
                    default:
                        return null;
                }
            });
    }

    @Test
    public void notConfiguredReturnsNullWithoutConnecting() {
        assertThat(cache.get(CLIENT_A, "")).isEmpty();
        assertThat(cache.get(CLIENT_A, null)).isEmpty();
        assertThat(connected).isEmpty();
        assertThat(cache.size()).isZero();
    }

    @Test
    public void repeatedCallsReuseTheSameClients() {
        List<Connection> first = cache.get(CLIENT_A, TWO_SERVERS);
        for (int i = 0; i < 10; i++) {
            assertThat(cache.get(CLIENT_A, TWO_SERVERS)).isSameAs(first);
        }
        assertThat(first).extracting(c -> c.config().getName()).containsExactly("github", "tools");
        assertThat(connected).containsExactly("github", "tools");
        assertThat(closed).isEmpty();
    }

    @Test
    public void whitespaceOnlyDifferenceDoesNotReconnect() {
        List<Connection> first = cache.get(CLIENT_A, ONE_SERVER);
        assertThat(cache.get(CLIENT_A, "  " + ONE_SERVER + "\n")).isSameAs(first);
        assertThat(connected).hasSize(1);
    }

    @Test
    public void configChangeClosesOldClientsAndConnectsNewOnes() {
        List<Connection> first = cache.get(CLIENT_A, ONE_SERVER);
        List<Connection> second = cache.get(CLIENT_A, TWO_SERVERS);

        assertThat(second).isNotSameAs(first);
        assertThat(connected).containsExactly("github", "github", "tools");
        assertThat(closed).containsExactly("github");
    }

    @Test
    public void clearingTheConfigClosesClients() {
        cache.get(CLIENT_A, TWO_SERVERS);
        assertThat(cache.get(CLIENT_A, "")).isEmpty();
        assertThat(closed).containsExactlyInAnyOrder("github", "tools");
        assertThat(cache.size()).isZero();
    }

    @Test
    public void eachAdClientHasItsOwnClients() {
        List<Connection> a = cache.get(CLIENT_A, ONE_SERVER);
        List<Connection> b = cache.get(CLIENT_B, ONE_SERVER);
        assertThat(b).isNotSameAs(a);
        assertThat(cache.size()).isEqualTo(2);

        cache.get(CLIENT_A, "");
        assertThat(closed).containsExactly("github");
        assertThat(cache.get(CLIENT_B, ONE_SERVER)).isSameAs(b);
    }

    @Test
    public void failedServerIsRetriedOnlyAfterTheRetryInterval() {
        failing.add("tools");
        List<Connection> partial = cache.get(CLIENT_A, TWO_SERVERS);
        assertThat(partial).isNotEmpty();
        assertThat(connected).containsExactly("github");

        now.addAndGet(MCPClientCache.RETRY_INTERVAL_MS - 1);
        assertThat(cache.get(CLIENT_A, TWO_SERVERS)).isSameAs(partial);
        assertThat(connected).containsExactly("github");

        failing.clear();
        now.addAndGet(1);
        List<Connection> full = cache.get(CLIENT_A, TWO_SERVERS);
        assertThat(full).isNotSameAs(partial);
        assertThat(connected).containsExactly("github", "github", "tools");
        assertThat(closed).containsExactly("github");
    }

    @Test
    public void allServersFailingReturnsNullAndIsNotRetriedOnEveryMessage() {
        failing.add("github");
        assertThat(cache.get(CLIENT_A, ONE_SERVER)).isEmpty();
        assertThat(cache.get(CLIENT_A, ONE_SERVER)).isEmpty();
        assertThat(cache.size()).isEqualTo(1);

        failing.clear();
        now.addAndGet(MCPClientCache.RETRY_INTERVAL_MS);
        assertThat(cache.get(CLIENT_A, ONE_SERVER)).isNotEmpty();
        assertThat(connected).containsExactly("github");
    }

    @Test
    public void invalidJsonIsCachedAndNotReparsedOnEveryMessage() {
        assertThat(cache.get(CLIENT_A, "not json")).isEmpty();
        assertThat(cache.get(CLIENT_A, "not json")).isEmpty();
        assertThat(cache.size()).isEqualTo(1);
        assertThat(connected).isEmpty();

        assertThat(cache.get(CLIENT_A, ONE_SERVER)).isNotEmpty();
    }

    @Test
    public void closeAllClosesEveryClientAndNextCallReconnects() {
        cache.get(CLIENT_A, TWO_SERVERS);
        cache.get(CLIENT_B, ONE_SERVER);

        cache.closeAll();
        assertThat(closed).containsExactlyInAnyOrder("github", "tools", "github");
        assertThat(cache.size()).isZero();

        assertThat(cache.get(CLIENT_A, ONE_SERVER)).isNotEmpty();
        assertThat(connected).hasSize(4);
    }

    @Test
    public void failingCloseDoesNotStopOtherClientsFromClosing() {
        MCPClientCache throwingCache = new MCPClientCache(config -> {
            McpClient delegate = fakeClient(config);
            return (McpClient) Proxy.newProxyInstance(McpClient.class.getClassLoader(),
                new Class<?>[] { McpClient.class }, (proxy, method, args) -> {
                    if ("close".equals(method.getName()) && "github".equals(config.getName())) {
                        throw new IllegalStateException("already dead");
                    }
                    return method.invoke(delegate, args);
                });
        }, now::get);

        throwingCache.get(CLIENT_A, TWO_SERVERS);
        throwingCache.closeAll();
        assertThat(closed).containsExactly("tools");
        assertThat(throwingCache.size()).isZero();
    }
}
