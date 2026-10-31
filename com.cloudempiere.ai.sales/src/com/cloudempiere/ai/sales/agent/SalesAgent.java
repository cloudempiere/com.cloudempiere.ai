package com.cloudempiere.ai.sales.agent;

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
import com.cloudempiere.ai.sales.tools.SalesTools;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import dev.langchain4j.service.guardrail.OutputGuardrails;

/**
 * Sales domain AI agent.
 *
 * <p>This agent provides AI-powered sales assistance including:</p>
 * <ul>
 *   <li>Sales opportunity analysis and summarization</li>
 *   <li>Business partner sales performance analysis</li>
 *   <li>Order history and trend analysis</li>
 *   <li>Product recommendations</li>
 *   <li>Sales pipeline insights</li>
 * </ul>
 *
 * <p><b>Architecture Layer:</b> Domain Agent Layer (Sales)</p>
 * <p><b>Boundary:</b> {@link com.cloudempiere.ai.sales.boundary.SalesDomainBoundary}</p>
 * <p><b>Tools:</b> {@link com.cloudempiere.ai.sales.tools.SalesTools}</p>
 *
 * <p><b>Related ADRs:</b></p>
 * <ul>
 *   <li><a href="../../../docs/adr/009-domain-boundaries-agent-scope.md">ADR-009: Domain Boundaries</a></li>
 *   <li><a href="../../../docs/adr/011-specialized-agent-scopes.md">ADR-011: Specialized Agent Scopes</a></li>
 *   <li><a href="../../../docs/adr/010-agent-orchestration-architecture.md">ADR-010: Agent Orchestration</a></li>
 * </ul>
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 */
@Component(service = {SalesAgent.class, com.cloudempiere.ai.boundary.IDomainAgent.class}, immediate = true)
public class SalesAgent extends AbstractDomainAgent {

    private static final Logger log = Logger.getLogger(SalesAgent.class.getName());

    private static final String[] KEYWORDS = {
        "sales", "opportunity", "quote", "order", "customer", "revenue",
        "deal", "pipeline", "forecast", "crm", "lead", "prospect"
    };

    private static final String[] TABLE_HINTS = {"order", "opportunity", "salesrep", "quote"};

    public SalesAgent() {
        super("sales", KEYWORDS, TABLE_HINTS);
    }

    // CLD-1955: OPTIONAL+DYNAMIC so the agent can activate even when the LangChain4j provider bundle is absent
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ILangChain4jProviderFactory providerFactory;

    @Reference
    private volatile SalesTools salesTools;

    private SalesAgentInterface agent;

    /**
     * Activate the sales agent.
     *
     * <p>This method is called by OSGi when the component is activated.
     * The actual agent initialization is deferred until first use (lazy initialization)
     * to ensure iDempiere context is available for database queries.</p>
     */
    @Activate
    protected void activate() {
        long startTime = System.currentTimeMillis();
        log.warning("[STARTUP TIMING] SalesAgent.activate() START");

        log.info("Sales Agent OSGi component activated and ready");

        long elapsed = System.currentTimeMillis() - startTime;
        log.warning("[STARTUP TIMING] SalesAgent.activate() COMPLETED in " + elapsed + "ms");
    }

    /**
     * Ensure the agent is initialized with AI provider configuration.
     *
     * <p>Lazy initialization pattern: loads provider config from database
     * on first use. Uses MAIProvider.getDefault() to get the configured
     * AI provider for the current client.</p>
     *
     * @throws RuntimeException if no provider is configured or initialization fails
     */
    private synchronized void ensureInitialized() {
        if (agent != null) {
            return; // Already initialized
        }
        if (providerFactory == null) {
            throw new IllegalStateException("AI provider factory not available — langchain4j provider bundle not installed");
        }

        try {
            // Load default AI provider from database
            MAIProvider providerConfig = MAIProvider.getDefault(Env.getCtx(), null);

            if (providerConfig == null) {
                throw new RuntimeException("No AI provider configured. Please create an AIG_Provider record with IsDefault='Y'");
            }

            log.info("Initializing Sales Agent with provider: " + providerConfig.getName() +
                     " (Type: " + providerConfig.getAIGProviderType() + ")");

            // Create LangChain4j ChatModel from provider config
            ChatModel model = providerFactory.createModel(providerConfig);

            // Build the AI agent with tools and chat memory
            agent = AiServices.builder(SalesAgentInterface.class)
                .chatModel(model)
                .tools(salesTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();

            log.info("Sales Agent initialized successfully");

        } catch (Exception e) {
            log.severe("Failed to initialize Sales Agent: " + e.getMessage());
            throw new RuntimeException("Sales Agent initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze a sales opportunity.
     *
     * @param opportunityId the C_Opportunity_ID
     * @return analysis summary
     */
    public String analyzeOpportunity(int opportunityId) {
        ensureInitialized();
        return formatOpportunityAnalysis(agent.analyzeOpportunity(opportunityId));
    }

    /**
     * Analyze business partner sales performance.
     *
     * @param partnerId the C_BPartner_ID
     * @return performance analysis
     */
    public String analyzePartnerSales(int partnerId) {
        ensureInitialized();
        return formatPartnerSalesAnalysis(agent.analyzePartnerSales(partnerId));
    }

    /**
     * Render a structured {@link OpportunityAnalysis} as the markdown text the chat UI expects.
     */
    private static String formatOpportunityAnalysis(OpportunityAnalysis a) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Opportunity Summary**\n").append(a.summary()).append("\n\n");
        sb.append("**Business Partner Background**\n").append(a.partnerBackground()).append("\n\n");
        sb.append("**Products/Services**\n").append(a.products()).append("\n\n");
        appendList(sb, "Risk Factors", a.riskFactors());
        appendList(sb, "Recommended Next Actions", a.recommendedActions());
        sb.append("**Likelihood Assessment**\n").append(a.likelihoodAssessment());
        return sb.toString();
    }

    /**
     * Render a structured {@link PartnerSalesAnalysis} as the markdown text the chat UI expects.
     */
    private static String formatPartnerSalesAnalysis(PartnerSalesAnalysis a) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Customer Profile**\n").append(a.customerProfile()).append("\n\n");
        sb.append("**Sales Volume and Trends**\n").append(a.salesVolumeAndTrends()).append("\n\n");
        sb.append("**Order Frequency and Patterns**\n").append(a.orderFrequencyAndPatterns()).append("\n\n");
        sb.append("**Product Preferences**\n").append(a.productPreferences()).append("\n\n");
        appendList(sb, "Pipeline Opportunities", a.pipelineOpportunities());
        appendList(sb, "Risk Factors", a.riskFactors());
        appendList(sb, "Growth Opportunities and Recommendations", a.growthRecommendations());
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
     * Get sales insights from natural language query.
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
     * Sales agent interface for LangChain4j.
     *
     * <p>This interface defines the AI agent's capabilities using
     * LangChain4j's declarative service annotations.</p>
     */
    @InputGuardrails(InputGuard.class)
    @OutputGuardrails(OutputGuard.class)
    interface SalesAgentInterface {

        /**
         * System message defining the agent's role and capabilities.
         */
        @SystemMessage("You are a sales analyst AI specialized in iDempiere ERP sales data analysis.\n\n" +
            "Your role is to help sales teams with:\n" +
            "- Analyzing sales opportunities and forecasting\n" +
            "- Understanding business partner relationships and history\n" +
            "- Identifying sales trends and patterns\n" +
            "- Recommending next actions for opportunities\n" +
            "- Providing insights on sales pipeline health\n\n" +
            "You have access to the following data through tools:\n" +
            "- Business partner information (customers, contacts, credit limits)\n" +
            "- Sales orders (historical and current)\n" +
            "- Sales opportunities (pipeline, stages, forecasts)\n" +
            "- Product catalog (for recommendations)\n" +
            "- Sales performance metrics\n\n" +
            "When analyzing data:\n" +
            "1. Always verify data exists before drawing conclusions\n" +
            "2. Provide specific numbers and dates when available\n" +
            "3. Identify trends and anomalies\n" +
            "4. Suggest actionable next steps\n" +
            "5. Consider business context (credit limits, payment terms, etc.)\n\n" +
            "Security boundaries:\n" +
            "- You can READ sales, opportunity, and partner data\n" +
            "- You can CREATE/UPDATE draft opportunities only\n" +
            "- You CANNOT modify orders, invoices, or master data\n" +
            "- You CANNOT access inventory, purchasing, or financial data\n\n" +
            "Always be professional, concise, and data-driven in your responses.")
        String chat(@UserMessage String query);

        /**
         * Analyze a specific sales opportunity.
         *
         * @param opportunityId the opportunity to analyze
         * @return analysis summary
         */
        @UserMessage("Analyze sales opportunity {{opportunityId}}. Provide:\n" +
            "1. Opportunity summary (amount, probability, stage, close date)\n" +
            "2. Business partner background and history\n" +
            "3. Products/services in the opportunity\n" +
            "4. Risk factors and concerns\n" +
            "5. Recommended next actions\n" +
            "6. Likelihood assessment based on historical data\n\n" +
            "Be specific with numbers, dates, and actionable insights.")
        OpportunityAnalysis analyzeOpportunity(int opportunityId);

        /**
         * Analyze business partner sales performance.
         *
         * @param partnerId the business partner to analyze
         * @return performance analysis
         */
        @UserMessage("Analyze sales performance for business partner {{partnerId}}. Provide:\n" +
            "1. Customer profile (credit limit, payment terms, outstanding balance)\n" +
            "2. Historical sales volume and trends\n" +
            "3. Order frequency and patterns\n" +
            "4. Product preferences\n" +
            "5. Opportunities in pipeline\n" +
            "6. Risk factors (credit issues, payment delays)\n" +
            "7. Growth opportunities and recommendations\n\n" +
            "Support your analysis with specific data and trends.")
        PartnerSalesAnalysis analyzePartnerSales(int partnerId);
    }

    /**
     * Structured result for {@link SalesAgentInterface#analyzeOpportunity(int)}.
     */
    record OpportunityAnalysis(
        @Description("Opportunity summary: amount, probability, stage, and close date") String summary,
        @Description("Business partner background and history") String partnerBackground,
        @Description("Products or services included in the opportunity") String products,
        @Description("Risk factors and concerns, one item per entry") List<String> riskFactors,
        @Description("Recommended next actions, one item per entry") List<String> recommendedActions,
        @Description("Likelihood assessment based on historical data") String likelihoodAssessment
    ) {
    }

    /**
     * Structured result for {@link SalesAgentInterface#analyzePartnerSales(int)}.
     */
    record PartnerSalesAnalysis(
        @Description("Customer profile: credit limit, payment terms, outstanding balance") String customerProfile,
        @Description("Historical sales volume and trends") String salesVolumeAndTrends,
        @Description("Order frequency and patterns") String orderFrequencyAndPatterns,
        @Description("Product preferences") String productPreferences,
        @Description("Opportunities currently in the pipeline, one item per entry") List<String> pipelineOpportunities,
        @Description("Risk factors such as credit issues or payment delays, one item per entry") List<String> riskFactors,
        @Description("Growth opportunities and recommendations, one item per entry") List<String> growthRecommendations
    ) {
    }
}
