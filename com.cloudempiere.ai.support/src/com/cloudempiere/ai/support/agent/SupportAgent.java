package com.cloudempiere.ai.support.agent;

import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.cloudempiere.ai.provider.langchain4j.ILangChain4jProviderFactory;
import com.cloudempiere.ai.support.tools.SupportTools;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Support domain AI agent.
 *
 * <p>This agent provides AI-powered support assistance including:</p>
 * <ul>
 *   <li>Support ticket classification and routing</li>
 *   <li>Ticket priority assessment</li>
 *   <li>Customer history analysis</li>
 *   <li>Escalation detection</li>
 *   <li>Response recommendations</li>
 * </ul>
 *
 * <p><b>Architecture Layer:</b> Domain Agent Layer (Support)</p>
 * <p><b>Boundary:</b> {@link com.cloudempiere.ai.support.boundary.SupportDomainBoundary}</p>
 * <p><b>Tools:</b> {@link com.cloudempiere.ai.support.tools.SupportTools}</p>
 *
 * <p><b>Related ADRs:</b></p>
 * <ul>
 *   <li><a href="../../../docs/adr/009-domain-boundaries-agent-scope.md">ADR-009: Domain Boundaries</a></li>
 *   <li><a href="../../../docs/adr/011-specialized-agent-scopes.md">ADR-011: Specialized Agent Scopes</a></li>
 *   <li><a href="../../../docs/adr/019-support-ticket-classification.md">ADR-019: Support Ticket Classification</a></li>
 * </ul>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
@Component(service = SupportAgent.class, immediate = true)
public class SupportAgent {

    private static final Logger log = Logger.getLogger(SupportAgent.class.getName());

    @Reference
    private volatile ILangChain4jProviderFactory providerFactory;

    @Reference
    private volatile SupportTools supportTools;

    private SupportAgentInterface agent;

    /**
     * Activate the support agent.
     *
     * <p>This method is called by OSGi when the component is activated.
     * It initializes the LangChain4j agent with the configured AI provider
     * and support tools.</p>
     */
    @Activate
    protected void activate() {
        log.info("Activating Support Agent...");

        try {
            // TODO: Implement provider configuration loading
            // Need to get MAIProvider from database or configuration
            // Then call: ChatLanguageModel model = providerFactory.createModel(config);

            log.warning("Support Agent activation deferred - provider configuration not yet implemented");

            // Stub for future implementation:
            // MAIProvider config = loadProviderConfig();
            // ChatLanguageModel model = providerFactory.createModel(config);
            // agent = AiServices.builder(SupportAgentInterface.class)
            //     .chatLanguageModel(model)
            //     .tools(supportTools)
            //     .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
            //     .build();

        } catch (Exception e) {
            log.severe("Failed to activate Support Agent: " + e.getMessage());
            throw new RuntimeException("Support Agent activation failed", e);
        }
    }

    /**
     * Classify a support ticket.
     *
     * @param requestId the R_Request_ID
     * @return classification and routing recommendation
     */
    public String classifyTicket(int requestId) {
        if (agent == null) {
            return "Support agent not available. Please configure an AI provider.";
        }
        return agent.classifyTicket(requestId);
    }

    /**
     * Analyze customer support history.
     *
     * @param partnerId the C_BPartner_ID
     * @return customer support analysis
     */
    public String analyzeCustomerSupport(int partnerId) {
        if (agent == null) {
            return "Support agent not available. Please configure an AI provider.";
        }
        return agent.analyzeCustomerSupport(partnerId);
    }

    /**
     * Get support insights from natural language query.
     *
     * @param query natural language query
     * @return AI-generated response with insights
     */
    public String chat(String query) {
        if (agent == null) {
            return "Support agent not available. Please configure an AI provider.";
        }
        return agent.chat(query);
    }

    /**
     * Support agent interface for LangChain4j.
     *
     * <p>This interface defines the AI agent's capabilities using
     * LangChain4j's declarative service annotations.</p>
     */
    interface SupportAgentInterface {

        /**
         * System message defining the agent's role and capabilities.
         */
        @SystemMessage("You are a support specialist AI for iDempiere ERP customer support.\n\n" +
            "Your role is to help support teams with:\n" +
            "- Classifying and routing support tickets\n" +
            "- Assessing ticket priority and urgency\n" +
            "- Analyzing customer support history and patterns\n" +
            "- Detecting escalation scenarios\n" +
            "- Recommending responses and next actions\n" +
            "- Identifying trends in support issues\n\n" +
            "You have access to the following data through tools:\n" +
            "- Support tickets (requests, status, priority, category)\n" +
            "- Ticket history (actions, updates, resolution)\n" +
            "- Customer information (business partners, contact details)\n" +
            "- Request types and categories\n" +
            "- Escalation status\n\n" +
            "When analyzing tickets:\n" +
            "1. Always verify ticket data exists before classification\n" +
            "2. Consider customer history and previous tickets\n" +
            "3. Assess priority based on impact and urgency\n" +
            "4. Identify patterns that may indicate systemic issues\n" +
            "5. Recommend specific actions with clear reasoning\n" +
            "6. Flag tickets that require immediate attention or escalation\n\n" +
            "Classification guidelines:\n" +
            "- Technical issues → Route to technical support\n" +
            "- Billing/financial → Route to accounting\n" +
            "- Product questions → Route to sales/product team\n" +
            "- Critical/urgent → Escalate immediately\n" +
            "- Recurring issues → Flag for root cause analysis\n\n" +
            "Security boundaries:\n" +
            "- You can READ tickets, customer data, and history\n" +
            "- You can CREATE/UPDATE ticket actions and comments\n" +
            "- You CANNOT modify customer master data\n" +
            "- You CANNOT access financial, sales, or inventory data beyond what's needed for context\n" +
            "- You CANNOT delete tickets or close without human approval\n\n" +
            "Always be empathetic, solution-focused, and provide clear action items.")
        String chat(@UserMessage String query);

        /**
         * Classify a specific support ticket.
         *
         * @param requestId the ticket to classify
         * @return classification and routing recommendation
         */
        @UserMessage("Classify support ticket {{requestId}}. Provide:\n" +
            "1. Ticket summary (description, priority, status, created date)\n" +
            "2. Category classification (Technical, Billing, Product, etc.)\n" +
            "3. Priority assessment (justify based on impact and urgency)\n" +
            "4. Routing recommendation (which team should handle it)\n" +
            "5. Customer context (history, previous tickets, patterns)\n" +
            "6. Escalation recommendation (should it be escalated? why?)\n" +
            "7. Suggested response or next actions\n\n" +
            "Be specific with your classification and provide clear reasoning.")
        String classifyTicket(int requestId);

        /**
         * Analyze customer support history.
         *
         * @param partnerId the customer to analyze
         * @return support history analysis
         */
        @UserMessage("Analyze support history for customer {{partnerId}}. Provide:\n" +
            "1. Customer profile summary\n" +
            "2. Total tickets and resolution rate\n" +
            "3. Common issue categories and patterns\n" +
            "4. Escalated tickets and critical issues\n" +
            "5. Response time and satisfaction trends\n" +
            "6. Current open tickets requiring attention\n" +
            "7. Recommendations for proactive support or account management\n\n" +
            "Support your analysis with specific data and identify actionable insights.")
        String analyzeCustomerSupport(int partnerId);
    }
}
