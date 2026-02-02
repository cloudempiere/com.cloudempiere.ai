package com.cloudempiere.ai.component;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.compiere.util.Env;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import com.cloudempiere.ai.orchestrator.OrchestratorAgent;
import com.cloudempiere.ai.util.ServiceLocator;

/**
 * AI Chat Panel component for iDempiere.
 *
 * <p>Provides a conversational interface for interacting with AI agents.
 * Users can type natural language queries and receive intelligent responses
 * from the appropriate domain specialist agents.</p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Natural language query input</li>
 *   <li>Automatic routing to domain agents via orchestrator</li>
 *   <li>Chat history display with user/AI message distinction</li>
 *   <li>Real-time response rendering</li>
 *   <li>Session-based conversation tracking</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * ChatPanel chatPanel = new ChatPanel();
 * chatPanel.setParent(parentComponent);
 * </pre>
 *
 * <p><b>Related ADRs:</b></p>
 * <ul>
 *   <li><a href="../../docs/adr/015-conversational-ux-patterns.md">ADR-015: Conversational UX</a></li>
 *   <li><a href="../../docs/adr/036-chat-ownership-and-sharing-model.md">ADR-036: Chat Ownership</a></li>
 * </ul>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
public class ChatPanel extends Div {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(ChatPanel.class.getName());

    /** Chat history container */
    private Vlayout chatHistory;

    /** Input textbox */
    private Textbox inputBox;

    /** Send button */
    private Button sendButton;

    /** Orchestrator for routing queries */
    private OrchestratorAgent orchestrator;

    /** Message history for context */
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * Constructor.
     */
    public ChatPanel() {
        super();
        init();
    }

    /**
     * Initialize the chat panel UI.
     */
    private void init() {
        // Set panel styling
        setStyle("width: 100%; height: 600px; border: 1px solid #ccc; border-radius: 8px; " +
                 "display: flex; flex-direction: column; background: #f5f5f5;");

        // Header
        Div header = new Div();
        header.setStyle("padding: 12px 16px; background: #2196F3; color: white; " +
                       "font-weight: bold; font-size: 16px; border-radius: 8px 8px 0 0;");
        Label headerLabel = new Label("AI Assistant");
        headerLabel.setStyle("color: white;");
        header.appendChild(headerLabel);
        this.appendChild(header);

        // Chat history area (scrollable)
        Div historyContainer = new Div();
        historyContainer.setStyle("flex: 1; overflow-y: auto; padding: 16px; background: white;");

        chatHistory = new Vlayout();
        chatHistory.setStyle("width: 100%;");
        historyContainer.appendChild(chatHistory);
        this.appendChild(historyContainer);

        // Input area
        Hlayout inputArea = new Hlayout();
        inputArea.setStyle("padding: 12px; background: #f5f5f5; border-top: 1px solid #ddd; gap: 8px;");
        inputArea.setVflex("min");

        inputBox = new Textbox();
        inputBox.setPlaceholder("Ask me anything...");
        inputBox.setStyle("flex: 1; padding: 8px 12px; border: 1px solid #ccc; " +
                         "border-radius: 4px; font-size: 14px;");
        inputBox.setHflex("1");
        inputBox.setRows(1);
        inputBox.addEventListener(Events.ON_OK, new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                handleSendMessage();
            }
        });

        sendButton = new Button("Send");
        sendButton.setStyle("padding: 8px 24px; background: #2196F3; color: white; " +
                           "border: none; border-radius: 4px; cursor: pointer; font-weight: bold;");
        sendButton.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override
            public void onEvent(Event event) throws Exception {
                handleSendMessage();
            }
        });

        inputArea.appendChild(inputBox);
        inputArea.appendChild(sendButton);
        this.appendChild(inputArea);

        // Load orchestrator
        loadOrchestrator();

        // Add welcome message
        addWelcomeMessage();
    }

    /**
     * Load the orchestrator agent from OSGi.
     */
    private void loadOrchestrator() {
        try {
            // Lookup orchestrator from OSGi service registry
            orchestrator = ServiceLocator.getService(OrchestratorAgent.class);

            if (orchestrator == null) {
                log.warning("Orchestrator service not found");
                addSystemMessage("Error: AI Orchestrator not available. Please check system configuration.");
                return;
            }

            if (!orchestrator.hasAgentsAvailable()) {
                log.warning("No AI agents available");
                addSystemMessage("Warning: No AI agents are currently available. " +
                               "Please ensure domain agent plugins are installed and AI provider is configured.");
            } else {
                List<String> agents = orchestrator.getAvailableAgents();
                log.info("Chat panel initialized with agents: " + agents);
            }
        } catch (Exception e) {
            log.severe("Failed to load orchestrator: " + e.getMessage());
            addSystemMessage("Error: Unable to initialize AI system. " + e.getMessage());
        }
    }

    /**
     * Add welcome message to chat.
     */
    private void addWelcomeMessage() {
        addSystemMessage("Hello! I'm your AI assistant. I can help you with sales, inventory, " +
                        "purchasing, support tickets, and access knowledge base articles. " +
                        "What would you like to know?");
    }

    /**
     * Handle send message button click.
     */
    private void handleSendMessage() {
        String query = inputBox.getValue();

        if (query == null || query.trim().isEmpty()) {
            return;
        }

        // Clear input
        inputBox.setValue("");

        // Add user message to UI
        addUserMessage(query);

        // Show typing indicator
        Div typingIndicator = addTypingIndicator();

        // Process query asynchronously
        Events.echoEvent("onAIQuery", this, query);
    }

    /**
     * Process AI query (should be called asynchronously).
     *
     * @param query user query
     */
    public void processQuery(String query) {
        try {
            // Remove typing indicator
            removeTypingIndicator();

            if (orchestrator == null || !orchestrator.hasAgentsAvailable()) {
                addSystemMessage("AI system not available. Please configure an AI provider.");
                return;
            }

            // Route through orchestrator
            String response = orchestrator.chat(query);

            // Add AI response
            addAIMessage(response);

            // Store in message history
            messages.add(new ChatMessage("user", query));
            messages.add(new ChatMessage("assistant", response));

        } catch (Exception e) {
            log.severe("Error processing query: " + e.getMessage());
            addSystemMessage("Error: " + e.getMessage());
        }
    }

    /**
     * Add user message bubble to chat.
     *
     * @param message user message text
     */
    private void addUserMessage(String message) {
        Hlayout messageRow = new Hlayout();
        messageRow.setStyle("width: 100%; justify-content: flex-end; margin-bottom: 12px;");

        Div bubble = new Div();
        bubble.setStyle("max-width: 70%; padding: 10px 14px; background: #2196F3; " +
                       "color: white; border-radius: 18px 18px 4px 18px; " +
                       "word-wrap: break-word; box-shadow: 0 1px 2px rgba(0,0,0,0.1);");

        Label label = new Label(message);
        label.setStyle("color: white; white-space: pre-wrap;");
        bubble.appendChild(label);
        messageRow.appendChild(bubble);

        chatHistory.appendChild(messageRow);
        scrollToBottom();
    }

    /**
     * Add AI response bubble to chat.
     *
     * @param message AI response text
     */
    private void addAIMessage(String message) {
        Hlayout messageRow = new Hlayout();
        messageRow.setStyle("width: 100%; justify-content: flex-start; margin-bottom: 12px;");

        Div bubble = new Div();
        bubble.setStyle("max-width: 70%; padding: 10px 14px; background: #e3f2fd; " +
                       "color: #333; border-radius: 18px 18px 18px 4px; " +
                       "word-wrap: break-word; box-shadow: 0 1px 2px rgba(0,0,0,0.1);");

        Label label = new Label(message);
        label.setStyle("white-space: pre-wrap;");
        bubble.appendChild(label);
        messageRow.appendChild(bubble);

        chatHistory.appendChild(messageRow);
        scrollToBottom();
    }

    /**
     * Add system message (info/warning/error).
     *
     * @param message system message
     */
    private void addSystemMessage(String message) {
        Div systemMsg = new Div();
        systemMsg.setStyle("width: 100%; text-align: center; margin: 12px 0; " +
                          "padding: 8px; background: #fff3cd; border-radius: 4px; " +
                          "font-size: 12px; color: #856404;");

        Label label = new Label(message);
        label.setStyle("white-space: pre-wrap;");
        systemMsg.appendChild(label);

        chatHistory.appendChild(systemMsg);
        scrollToBottom();
    }

    /**
     * Add typing indicator.
     *
     * @return typing indicator component
     */
    private Div addTypingIndicator() {
        Hlayout messageRow = new Hlayout();
        messageRow.setStyle("width: 100%; justify-content: flex-start; margin-bottom: 12px;");
        messageRow.setId("typingIndicator");

        Div bubble = new Div();
        bubble.setStyle("padding: 10px 14px; background: #e3f2fd; border-radius: 18px; " +
                       "font-style: italic; color: #666;");

        Label label = new Label("AI is thinking...");
        bubble.appendChild(label);
        messageRow.appendChild(bubble);

        chatHistory.appendChild(messageRow);
        scrollToBottom();

        return messageRow;
    }

    /**
     * Remove typing indicator.
     */
    private void removeTypingIndicator() {
        Component indicator = chatHistory.getFellow("typingIndicator", false);
        if (indicator != null) {
            chatHistory.removeChild(indicator);
        }
    }

    /**
     * Scroll chat history to bottom.
     */
    private void scrollToBottom() {
        Clients.scrollIntoView(chatHistory.getLastChild());
    }

    /**
     * Clear chat history.
     */
    public void clearChat() {
        chatHistory.getChildren().clear();
        messages.clear();
        addWelcomeMessage();
    }

    /**
     * Set orchestrator (for dependency injection).
     *
     * @param orchestrator orchestrator agent
     */
    public void setOrchestrator(OrchestratorAgent orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Chat message for history.
     */
    private static class ChatMessage {
        String role;
        String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
