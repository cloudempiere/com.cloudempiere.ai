package com.cloudempiere.ai.support.agent;

import java.util.List;
import java.util.logging.Logger;

import org.compiere.util.Env;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.cloudempiere.ai.boundary.AbstractDomainAgent;
import com.cloudempiere.ai.guardrails.InputGuard;
import com.cloudempiere.ai.guardrails.OutputGuard;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.ILangChain4jProviderFactory;
import com.cloudempiere.ai.support.tools.SupportTools;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import dev.langchain4j.service.guardrail.OutputGuardrails;

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
 * @author Cloudempiere AI Team
 * @version 1.0.0
 */
@Component(service = {SupportAgent.class, com.cloudempiere.ai.boundary.IDomainAgent.class}, immediate = true)
public class SupportAgent extends AbstractDomainAgent {

    private static final Logger log = Logger.getLogger(SupportAgent.class.getName());

    private static final String[] KEYWORDS = {
        "support", "ticket", "request", "issue", "problem", "bug",
        "help", "escalation", "complaint", "service"
    };

    private static final String[] TABLE_HINTS = {"r_request", "support", "ticket"};

    public SupportAgent() {
        super("support", KEYWORDS, TABLE_HINTS);
    }

    // CLD-1955: OPTIONAL+DYNAMIC so the agent can activate even when the LangChain4j provider bundle is absent
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ILangChain4jProviderFactory providerFactory;

    @Reference
    private volatile SupportTools supportTools;

    private SupportAgentInterface agent;

    /**
     * Activate the support agent.
     *
     * <p>This method is called by OSGi when the component is activated.
     * The actual agent initialization is deferred until first use.</p>
     */
    @Activate
    protected void activate() {
        long startTime = System.currentTimeMillis();
        log.warning("[STARTUP TIMING] SupportAgent.activate() START");

        log.info("Support Agent OSGi component activated and ready");

        long elapsed = System.currentTimeMillis() - startTime;
        log.warning("[STARTUP TIMING] SupportAgent.activate() COMPLETED in " + elapsed + "ms");
    }

    private synchronized void ensureInitialized() {
        if (agent != null) {
            return;
        }
        if (providerFactory == null) {
            throw new IllegalStateException("AI provider factory not available — langchain4j provider bundle not installed");
        }

        try {
            MAIProvider providerConfig = MAIProvider.getDefault(Env.getCtx(), null);

            if (providerConfig == null) {
                throw new RuntimeException("No AI provider configured. Please create an AIG_Provider record with IsDefault='Y'");
            }

            log.info("Initializing Support Agent with provider: " + providerConfig.getName());

            ChatModel model = providerFactory.createModel(providerConfig);

            agent = AiServices.builder(SupportAgentInterface.class)
                .chatModel(model)
                .tools(supportTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();

            log.info("Support Agent initialized successfully");

        } catch (Exception e) {
            log.severe("Failed to initialize Support Agent: " + e.getMessage());
            throw new RuntimeException("Support Agent initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Classify a support ticket.
     *
     * @param requestId the R_Request_ID
     * @return classification and routing recommendation
     */
    public String classifyTicket(int requestId) {
        ensureInitialized();
        return formatTicketClassification(agent.classifyTicket(requestId));
    }

    /**
     * Analyze customer support history.
     *
     * @param partnerId the C_BPartner_ID
     * @return customer support analysis
     */
    public String analyzeCustomerSupport(int partnerId) {
        ensureInitialized();
        return formatCustomerSupportAnalysis(agent.analyzeCustomerSupport(partnerId));
    }

    /**
     * Render a structured {@link TicketClassification} as the markdown text the chat UI expects.
     */
    private static String formatTicketClassification(TicketClassification c) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Ticket Summary**\n").append(c.ticketSummary()).append("\n\n");
        sb.append("**Category**\n").append(c.category()).append("\n\n");
        sb.append("**Priority Assessment**\n").append(c.priorityAssessment()).append("\n\n");
        sb.append("**Routing Recommendation**\n").append(c.routingRecommendation()).append("\n\n");
        sb.append("**Customer Context**\n").append(c.customerContext()).append("\n\n");
        sb.append("**Escalation Recommendation**\n").append(c.escalationRecommendation()).append("\n\n");
        sb.append("**Suggested Response / Next Actions**\n").append(c.suggestedResponse());
        return sb.toString();
    }

    /**
     * Render a structured {@link CustomerSupportAnalysis} as the markdown text the chat UI expects.
     */
    private static String formatCustomerSupportAnalysis(CustomerSupportAnalysis a) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Customer Profile**\n").append(a.customerProfile()).append("\n\n");
        sb.append("**Ticket Volume and Resolution Rate**\n").append(a.ticketVolumeAndResolutionRate()).append("\n\n");
        sb.append("**Common Issue Categories and Patterns**\n").append(a.issuePatterns()).append("\n\n");
        appendList(sb, "Escalated Tickets and Critical Issues", a.escalatedIssues());
        sb.append("**Response Time and Satisfaction Trends**\n").append(a.responseTrends()).append("\n\n");
        appendList(sb, "Current Open Tickets Requiring Attention", a.openTickets());
        appendList(sb, "Recommendations", a.recommendations());
        return sb.toString();
    }

    private static void appendList(StringBuilder sb, String heading, List<String> items) {
        sb.append("**").append(heading).append("**\n");
        if (items == null || items.isEmpty()) {
            sb.append("- None identified\n\n");
            return;
        }
        for (String item : items) {
            sb.append("- ").append(item).append("\n");
        }
        sb.append("\n");
    }

    /**
     * Get support insights from natural language query.
     *
     * @param query natural language query
     * @return AI-generated response with insights
     */
    @Override
    public String chat(String query) {
        ensureInitialized();
        return agent.chat(query);
    }

    /**
     * Support agent interface for LangChain4j.
     *
     * <p>This interface defines the AI agent's capabilities using
     * LangChain4j's declarative service annotations.</p>
     */
    @InputGuardrails(InputGuard.class)
    @OutputGuardrails(OutputGuard.class)
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
        TicketClassification classifyTicket(int requestId);

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
        CustomerSupportAnalysis analyzeCustomerSupport(int partnerId);
    }

    /**
     * Structured result for {@link SupportAgentInterface#classifyTicket(int)}.
     */
    record TicketClassification(
        @Description("Ticket summary: description, priority, status, created date") String ticketSummary,
        @Description("Category classification, e.g. Technical, Billing, Product") String category,
        @Description("Priority assessment, justified by impact and urgency") String priorityAssessment,
        @Description("Which team should handle this ticket") String routingRecommendation,
        @Description("Customer context: history, previous tickets, patterns") String customerContext,
        @Description("Whether this ticket should be escalated, and why") String escalationRecommendation,
        @Description("Suggested response or next actions") String suggestedResponse
    ) {
    }

    /**
     * Structured result for {@link SupportAgentInterface#analyzeCustomerSupport(int)}.
     */
    record CustomerSupportAnalysis(
        @Description("Customer profile summary") String customerProfile,
        @Description("Total tickets and resolution rate") String ticketVolumeAndResolutionRate,
        @Description("Common issue categories and patterns") String issuePatterns,
        @Description("Escalated tickets and critical issues, one item per entry") List<String> escalatedIssues,
        @Description("Response time and satisfaction trends") String responseTrends,
        @Description("Current open tickets requiring attention, one item per entry") List<String> openTickets,
        @Description("Recommendations for proactive support or account management, one item per entry") List<String> recommendations
    ) {
    }
}
