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
package com.cloudempiere.ai.util;

import java.util.Properties;

import org.adempiere.util.ContextRunnable;
import org.adempiere.util.ServerContext;
import org.compiere.util.Env;

/**
 * The authorised user's context for AI work on background threads (ADR-007, CLD-2432).
 *
 * <p>ADR-007 guards the LLM's database scope with the requesting user's tenant and role. In the web UI
 * {@code Env.getCtx()} is a wrapper that reads the context of whichever thread uses it; the AI runs on pool and
 * HTTP client threads, where that is empty (System tenant, System Administrator role) or a context inherited from
 * another request. So the UI takes a plain copy of the user's context on its own thread and the AI only ever sees
 * that copy: the background task runs as iDempiere's {@link ContextRunnable} ({@link #runAs}), and the chat handed
 * to the AI service is bound to the copy, which also covers the threads LangChain4j runs tool calls on.
 */
public final class AuthorisedUserContext {

    private AuthorisedUserContext() {
    }

    /**
     * Copies the current thread's context. Call it on the thread that serves the user (the UI thread).
     *
     * @return a plain Properties copy, independent of the thread it is used on
     */
    public static Properties capture() {
        Properties copy = new Properties();
        copy.putAll(ServerContext.getCurrentInstance());
        return copy;
    }

    /**
     * Wraps a task so it runs in the given context, the iDempiere way: an {@link ContextRunnable} sets it as the
     * worker thread's context for the task and clears it afterwards, so the thread keeps no stale login. Submit it
     * to {@code Adempiere.getThreadPoolExecutor()}.
     *
     * @param ctx context from {@link #capture()}
     * @param task work to run
     * @return runnable for the thread pool
     */
    public static Runnable runAs(Properties ctx, Runnable task) {
        return new ContextRunnable() {
            {
                context = ctx; // the captured copy, not the session's live context
            }

            @Override
            protected void doRun() {
                task.run();
            }
        };
    }

    /**
     * Whether the context carries a login: tenant, role and user are set. A context without them would make
     * {@code Env.getAD_Client_ID} and {@code Env.getAD_Role_ID} return 0, i.e. the System tenant and the System
     * Administrator role.
     *
     * @param ctx context to check
     * @return true when tenant, role and user are present
     */
    public static boolean hasLogin(Properties ctx) {
        // read the #-keys directly: for them Env.getContext is ctx.getProperty(key, "")
        return ctx != null
            && !ctx.getProperty(Env.AD_CLIENT_ID, "").isEmpty()
            && !ctx.getProperty(Env.AD_ROLE_ID, "").isEmpty()
            && !ctx.getProperty(Env.AD_USER_ID, "").isEmpty();
    }
}
