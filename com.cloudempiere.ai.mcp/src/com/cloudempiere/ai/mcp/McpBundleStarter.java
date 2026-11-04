package com.cloudempiere.ai.mcp;

import org.compiere.util.CLogger;

/**
 * Makes the MCP web bundle start (CLD-2542).
 *
 * <p>iDempiere installs plugin bundles with start level 4 and no autostart; a bundle with
 * {@code Bundle-ActivationPolicy: lazy} is started and becomes ACTIVE when one of its classes is loaded. The other web
 * bundles (REST, GraphQL, identity) get that class load from their DS components; this bundle has only a servlet, so
 * without a component it stayed STARTING (lazy) or RESOLVED (not lazy) and Jetty never deployed {@code /ai-mcp}.
 * This immediate component is that trigger. Seen on the POC server, 2026-11-04.
 */
public class McpBundleStarter {

    private static final CLogger log = CLogger.getCLogger(McpBundleStarter.class);

    public void activate() {
        log.warning("AI MCP bundle started; Jetty deploys /ai-mcp");
    }
}
