/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
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
package com.cloudempiere.ai.factory;

import java.util.Map;
import java.util.logging.Level;

import org.adempiere.webui.factory.IDashboardGadgetFactory;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.zkoss.zk.ui.Component;

import com.cloudempiere.ai.component.AIChatWidget;
import com.cloudempiere.ai.health.AIPluginHealthService;
import com.cloudempiere.ai.model.MAIProvider;

/**
 * Dashboard Gadget Factory for AI Chat Widget.
 *
 * <p>This factory implements the standard iDempiere {@link IDashboardGadgetFactory}
 * pattern, which allows the AI Chat Widget to be provided via the standard extension
 * mechanism without requiring custom interfaces in the core.
 *
 * <p><strong>ADR-052: Core Decoupling</strong>
 * <p>This class replaces the custom {@link AIChatWidgetFactory} which required
 * a custom interface ({@code IAIChatWidgetFactory}) in the iDempiere core.
 * By using the standard gadget factory pattern, we:
 * <ul>
 *   <li>Remove custom code from the core fork</li>
 *   <li>Use standard iDempiere extension mechanism</li>
 *   <li>Allow independent plugin deployment</li>
 * </ul>
 *
 * <p><strong>Usage:</strong>
 * <p>HelpController retrieves the widget via:
 * <pre>
 * Component aiChat = Extensions.getDashboardGadget("ai-chat", parent, null);
 * if (aiChat != null) {
 *     pnlAIChat.appendChild(aiChat);
 * }
 * </pre>
 *
 * @author Cloudempiere
 * @version 10.0.0
 * @since ADR-052 OSGi Modernization
 * @see IDashboardGadgetFactory
 */
@org.osgi.service.component.annotations.Component(
    service = IDashboardGadgetFactory.class,
    immediate = true,
    property = {
        "service.ranking:Integer=10"
    }
)
public class AIChatGadgetFactory implements IDashboardGadgetFactory {

    /** URI for AI Chat Widget */
    public static final String AI_CHAT_URI = "ai-chat";

    private static final CLogger log = CLogger.getCLogger(AIChatGadgetFactory.class);

    /**
     * Get gadget component for the given URI.
     *
     * @param uri Gadget URI (e.g., "ai-chat")
     * @param parent Parent component
     * @param arg Additional arguments (unused)
     * @return AIChatWidget if uri is "ai-chat" and widget is available, null otherwise
     */
    @Override
    public org.zkoss.zk.ui.Component getGadget(String uri, org.zkoss.zk.ui.Component parent, Map<?, ?> arg) {
        if (!AI_CHAT_URI.equals(uri)) {
            return null;
        }

        // Check if widget is available (health check + provider configured)
        if (!isAvailable()) {
            log.fine("AI Chat Widget not available for URI: " + uri);
            return null;
        }

        log.info("Creating AI Chat Widget for URI: " + uri);
        return new AIChatWidget(true);
    }

    /**
     * Check if AI Chat Widget is available.
     *
     * <p>Performs health check and verifies a default AI provider is configured.
     *
     * @return true if widget can be created
     */
    private boolean isAvailable() {
        try {
            // First check plugin health (ADR-050: Defensive programming)
            // This prevents errors when AIG_* tables don't exist
            AIPluginHealthService health = AIPluginHealthService.getInstance();
            if (health == null || !health.isHealthy()) {
                log.fine("AI Chat Widget not available: plugin health check failed");
                return false;
            }

            // Check if a default AI provider is configured in the database
            MAIProvider provider = MAIProvider.getDefault(Env.getCtx(), null);
            return provider != null && provider.isActive();
        } catch (Exception e) {
            // Defensive: never throw from isAvailable - just return false
            log.log(Level.FINE, "AI Chat Widget not available: " + e.getMessage(), e);
            return false;
        }
    }
}
