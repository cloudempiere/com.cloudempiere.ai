package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.adempiere.util.ServerContext;
import org.compiere.util.Env;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.test.categories.UnitTest;
import com.cloudempiere.ai.util.AuthorisedUserContext;

/**
 * ADR-007 guards the AI's database scope with the asking user's tenant and role. The AI runs on other threads than
 * the UI, so it must get a copy of that user's context, not the thread-bound one: on a pool thread the latter is
 * empty (System tenant, System Administrator role) or another user's (CLD-2432).
 */
@UnitTest
@DisplayName("AuthorisedUserContext")
class AuthorisedUserContextTest {

    @AfterEach
    void clearThreadContext() {
        ServerContext.dispose();
    }

    private static Properties login(int clientId, int roleId, int userId) {
        Properties ctx = new Properties();
        ctx.setProperty(Env.AD_CLIENT_ID, String.valueOf(clientId));
        ctx.setProperty(Env.AD_ROLE_ID, String.valueOf(roleId));
        ctx.setProperty(Env.AD_USER_ID, String.valueOf(userId));
        return ctx;
    }

    /** reads a #-key like Env.getContextAsInt does, without loading org.compiere.util.Util in the unit runner */
    private static int id(Properties ctx, String key) {
        return Integer.parseInt(ctx.getProperty(key, "-1"));
    }

    @Test
    @DisplayName("a captured context keeps the user's tenant and role on another thread")
    void captureSurvivesThreadChange() throws Exception {
        ServerContext.setCurrentInstance(login(1000034, 1000218, 1000579));
        Properties captured = AuthorisedUserContext.capture();
        ServerContext.dispose(); // the UI thread's context is gone, as it is for a pool thread

        int[] seen = CompletableFuture.supplyAsync(() ->
            new int[] { id(captured, Env.AD_CLIENT_ID), id(captured, Env.AD_ROLE_ID), id(captured, Env.AD_USER_ID) })
            .get(5, TimeUnit.SECONDS);

        assertThat(seen).containsExactly(1000034, 1000218, 1000579);
    }

    @Test
    @DisplayName("the capture is a copy: later changes to the thread context do not leak into it")
    void captureIsACopy() {
        ServerContext.setCurrentInstance(login(1000034, 1000218, 1000579));
        Properties captured = AuthorisedUserContext.capture();

        ServerContext.setCurrentInstance(login(1000036, 1000999, 1000580)); // another request on this thread

        assertThat(captured.getClass()).isEqualTo(Properties.class);
        assertThat(id(captured, Env.AD_CLIENT_ID)).isEqualTo(1000034);
        assertThat(id(captured, Env.AD_ROLE_ID)).isEqualTo(1000218);
    }

    @Test
    @DisplayName("runAs gives the worker thread the user's context for the task only")
    void runAsSetsAndClearsThreadContext() throws Exception {
        Properties captured = login(1000034, 1000218, 1000579);
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            int[] during = new int[2];
            worker.submit(AuthorisedUserContext.runAs(captured, () -> {
                during[0] = id(ServerContext.getCurrentInstance(), Env.AD_CLIENT_ID);
                during[1] = id(ServerContext.getCurrentInstance(), Env.AD_ROLE_ID);
            })).get(5, TimeUnit.SECONDS);

            // the same thread afterwards: no login left behind for the next task
            String after = worker.submit(() -> ServerContext.getCurrentInstance().getProperty(Env.AD_CLIENT_ID, ""))
                .get(5, TimeUnit.SECONDS);

            assertThat(during).containsExactly(1000034, 1000218);
            assertThat(after).isEmpty();
        } finally {
            worker.shutdownNow();
        }
    }

    @Test
    @DisplayName("a context with tenant, role and user is a login")
    void loginContext() {
        assertThat(AuthorisedUserContext.hasLogin(login(1000034, 1000218, 1000579))).isTrue();
        // System login: tenant 0 and role 0 are set explicitly, which is legitimate
        assertThat(AuthorisedUserContext.hasLogin(login(0, 0, 100))).isTrue();
    }

    @Test
    @DisplayName("an empty or partial context is not a login (it would read as System / System Administrator)")
    void noLoginContext() {
        assertThat(AuthorisedUserContext.hasLogin(null)).isFalse();
        assertThat(AuthorisedUserContext.hasLogin(new Properties())).isFalse();

        Properties noRole = login(1000034, 1000218, 1000579);
        noRole.remove(Env.AD_ROLE_ID);
        assertThat(AuthorisedUserContext.hasLogin(noRole)).isFalse();

        Properties noClient = login(1000034, 1000218, 1000579);
        noClient.remove(Env.AD_CLIENT_ID);
        assertThat(AuthorisedUserContext.hasLogin(noClient)).isFalse();
    }
}
