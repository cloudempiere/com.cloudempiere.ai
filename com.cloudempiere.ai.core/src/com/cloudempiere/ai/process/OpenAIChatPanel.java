package com.cloudempiere.ai.process;

import org.compiere.process.SvrProcess;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Window;

import com.cloudempiere.ai.component.AIChatWidget;

/**
 * Process to open the AI Chat Panel.
 *
 * <p>This process launches the AI Assistant chat interface in a modal window.
 * Users can interact with AI agents through natural language queries.</p>
 *
 * <p><b>Usage:</b> Can be called from any window, toolbar, or menu in iDempiere.</p>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
public class OpenAIChatPanel extends SvrProcess {

    @Override
    protected void prepare() {
        // No parameters needed
    }

    @Override
    protected String doIt() throws Exception {
        // Open chat panel in UI thread
        Executions.schedule(Executions.getCurrent().getDesktop(), event -> {
            openChatPanel();
        }, null);

        return "AI Chat Panel opened";
    }

    /**
     * Open the chat panel in a modal window.
     */
    private void openChatPanel() {
        try {
            Window window = new Window();
            window.setTitle("AI Assistant");
            window.setWidth("800px");
            window.setHeight("600px");
            window.setClosable(true);
            window.setMaximizable(true);
            window.setSizable(true);
            window.setBorder("normal");
            window.setStyle("padding: 0;");

            AIChatWidget chatWidget = new AIChatWidget();
            chatWidget.setParent(window);

            window.doModal();

        } catch (Exception e) {
            log.severe("Error opening chat panel: " + e.getMessage());
        }
    }
}
