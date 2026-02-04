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
import org.compiere.util.Msg;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;

import com.cloudempiere.ai.component.AIChatWidget;
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
     * <p>Returns a fully configured {@link Panel} containing the {@link AIChatWidget}.
     * The panel includes all styling, event handlers, and configuration that was
     * previously hardcoded in HelpController.
     *
     * @param uri Gadget URI (e.g., "ai-chat")
     * @param parent Parent component (used to find sibling panels for collapse behavior)
     * @param arg Additional arguments (unused)
     * @return Fully configured Panel with AIChatWidget, or null if not available
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

        // Create Panel wrapper (previously in HelpController lines 165-172)
        Panel pnlAIChat = new Panel();
        pnlAIChat.setSclass("dashboard-widget ai-chat-panel");
        pnlAIChat.setTitle(Msg.getMsg(Env.getCtx(), "AI Assistant"));
        pnlAIChat.setMaximizable(false);
        pnlAIChat.setCollapsible(true);
        pnlAIChat.setOpen(true);
        pnlAIChat.setBorder("normal");
        pnlAIChat.setHeight("700px"); // Fixed height for chat

        // Create panel content container (previously in HelpController lines 175-183)
        Panelchildren content = new Panelchildren();
        content.setStyle("height: 100%; padding: 0;");
        pnlAIChat.appendChild(content);

        // Create and add the AI Chat Widget
        AIChatWidget aiChatWidget = new AIChatWidget(true);
        content.appendChild(aiChatWidget);

        // Add collapse event listener (previously in HelpController lines 186-198)
        // This collapses tooltip and context help panels when AI chat opens
        pnlAIChat.addEventListener(Events.ON_OPEN, new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (event instanceof org.zkoss.zk.ui.event.OpenEvent) {
                    org.zkoss.zk.ui.event.OpenEvent oe = (org.zkoss.zk.ui.event.OpenEvent) event;
                    if (oe.isOpen() && parent != null) {
                        // Find and collapse sibling help panels
                        collapseSiblingHelpPanels(parent);
                    }
                }
            }
        });

        return pnlAIChat;
    }

    /**
     * Collapse other help panels (tooltip, context help) when AI chat opens.
     *
     * <p>This preserves the existing behavior where opening AI chat collapses
     * other help panels to save screen space.
     *
     * @param parent Parent component containing sibling panels
     */
    private void collapseSiblingHelpPanels(Component parent) {
        try {
            // Find panels with specific IDs and collapse them
            Component pnlToolTip = parent.getFellow("pnlToolTip");
            if (pnlToolTip instanceof Panel) {
                ((Panel) pnlToolTip).setOpen(false);
            }
        } catch (Exception e) {
            // Silently ignore - panel may not exist
        }

        try {
            Component pnlContextHelp = parent.getFellow("pnlContextHelp");
            if (pnlContextHelp instanceof Panel) {
                ((Panel) pnlContextHelp).setOpen(false);
            }
        } catch (Exception e) {
            // Silently ignore - panel may not exist
        }
    }

    /**
     * Check if AI Chat Widget is available.
     *
     * <p>Performs health check and verifies a default AI provider is configured.
     * <p>Uses stub health service until Phase 3 (ADR-050, ADR-051).
     *
     * @return true if widget can be created
     */
    private boolean isAvailable() {
        try {
            // TEMPORARILY DISABLED - Health check stub until Phase 3
            // Real implementation will check AIPluginHealthService
            // For now, just check if provider is configured

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
