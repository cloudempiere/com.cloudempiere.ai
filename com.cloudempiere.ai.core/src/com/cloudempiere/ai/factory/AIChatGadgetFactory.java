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
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.model.MForm;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Vlayout;

import com.cloudempiere.ai.component.AIChatWidget;
import com.cloudempiere.ai.form.AIChatForm;
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

    /** Posted to the panel to add the chat once the application desktop exists */
    private static final String ON_ADD_CHAT = "onAddAIChat";

    /**
     * Get gadget component for the given URI (upstream iDempiere signature).
     *
     * <p>Both {@code getGadget} variants are declared without {@code @Override}: this one is the
     * abstract method of upstream's {@link IDashboardGadgetFactory}; the variant with a {@code Map}
     * argument is the abstract method of the iDempiereCLDE fork's modified interface. Implementing
     * both lets this plugin build and run against either core, so it does not have to be released
     * together with the core change that restores upstream's signature.
     *
     * @param uri Gadget URI (e.g., "ai-chat")
     * @param parent Parent component (used to find sibling panels for collapse behavior)
     * @return Fully configured Panel with AIChatWidget, or null if not available
     */
    public org.zkoss.zk.ui.Component getGadget(String uri, org.zkoss.zk.ui.Component parent) {
        return createGadget(uri, parent);
    }

    /**
     * Get gadget component for the given URI (iDempiereCLDE fork signature, {@code arg} unused).
     * Remove once every supported core has upstream's {@link IDashboardGadgetFactory} again.
     *
     * @param uri Gadget URI (e.g., "ai-chat")
     * @param parent Parent component
     * @param arg Additional arguments (unused)
     * @return Fully configured Panel with AIChatWidget, or null if not available
     */
    public org.zkoss.zk.ui.Component getGadget(String uri, org.zkoss.zk.ui.Component parent, Map<?, ?> arg) {
        return createGadget(uri, parent);
    }

    /**
     * The chat of the help panel: the AI Assistant form (CLD-2438), the same component the menu opens as a tab. The
     * help panel keeps the AI provider access ({@link #isAvailable()}) as its gate, not the role's form access: most
     * roles are manual and would lose the panel until an administrator grants the form. The bare widget until
     * migration 202610290100_CLD-2438 has created the form, or when the form cannot be opened.
     *
     * <p>Called once the application desktop exists: {@code ADForm} registers itself with it, and HelpController
     * builds the help panel before the desktop is stored in the session.
     *
     * @return the form, or the widget
     */
    private org.zkoss.zk.ui.Component createChat() {
        // By class name: UUIDs are minted per database at import (clde-hub convention), the class is the same everywhere
        int formId = new Query(Env.getCtx(), MForm.Table_Name, MForm.COLUMNNAME_Classname + "=?", null)
            .setParameters(AIChatForm.class.getName())
            .setOnlyActiveRecords(true)
            .firstId();
        if (formId <= 0) {
            log.warning("AI Assistant form (" + AIChatForm.class.getName() + ") not in the dictionary: "
                + "showing the chat without it, apply migration 202610290100_CLD-2438");
            return new AIChatWidget(true);
        }
        try {
            ADForm form = ADForm.openForm(formId);
            form.setTitle(null);
            form.setBorder("none");
            ZKUpdateUtil.setVflex(form, "1");
            ZKUpdateUtil.setHflex(form, "1");
            return form;
        } catch (Exception e) {
            log.log(Level.WARNING, "AI Assistant form " + formId + " could not be opened, showing the chat without it", e);
            return new AIChatWidget(true);
        }
    }

    /**
     * Build the AI chat gadget.
     *
     * <p>Returns a fully configured {@link Panel} containing the {@link AIChatWidget}.
     * The panel includes all styling, event handlers, and configuration that was
     * previously hardcoded in HelpController.
     *
     * @param uri Gadget URI (e.g., "ai-chat")
     * @param parent Parent component (used to find sibling panels for collapse behavior)
     * @return Fully configured Panel with AIChatWidget, or null if not available
     */
    private org.zkoss.zk.ui.Component createGadget(String uri, org.zkoss.zk.ui.Component parent) {
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
        // Fills the rest of the help column below the other panels instead of a fixed 700px (CLD-2437). The column
        // layout HelpController passes as parent is only as tall as its content, so it gets vflex too.
        // Minimum height of the open panel: theme CSS (.ai-chat-panel), so a collapsed panel shrinks to its title bar.
        ZKUpdateUtil.setVflex(pnlAIChat, "1");
        if (parent instanceof Vlayout) {
            ZKUpdateUtil.setVflex((Vlayout) parent, "1");
        }

        // Create panel content container (previously in HelpController lines 175-183)
        Panelchildren content = new Panelchildren();
        content.setStyle("height: 100%; padding: 0;");
        pnlAIChat.appendChild(content);

        // Added once the request that builds the desktop has finished: the form needs the application desktop, which
        // AdempiereWebUI stores only after HelpController has rendered the help panel (CLD-2438)
        pnlAIChat.addEventListener(ON_ADD_CHAT, event -> content.appendChild(createChat()));
        Events.postEvent(ON_ADD_CHAT, pnlAIChat, null);

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
            java.util.Properties ctx = Env.getCtx();
            MAIProvider provider = MAIProvider.getForUser(ctx, null);
            boolean available = provider != null;
            log.fine("AI Chat isAvailable=" + available
                    + (available ? " provider=" + provider.getName() + " (ID=" + provider.getAIG_Provider_ID() + ")" : " (no accessible provider)"));
            return available;
        } catch (Exception e) {
            log.log(Level.WARNING, "AI Chat Widget not available - Exception: " + e.getMessage(), e);
            return false;
        }
    }
}
