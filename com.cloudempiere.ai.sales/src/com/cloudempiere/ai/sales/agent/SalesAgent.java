package com.cloudempiere.ai.sales.agent;

import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.cloudempiere.ai.provider.langchain4j.ILangChain4jProviderFactory;
import com.cloudempiere.ai.sales.tools.SalesTools;
import com.cloudempiere.ai.model.MAIProvider;

import org.compiere.util.Env;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

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
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
@Component(service = SalesAgent.class, immediate = true)
public class SalesAgent {

    private static final Logger log = Logger.getLogger(SalesAgent.class.getName());

    @Reference
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
        log.info("Sales Agent OSGi component activated and ready");
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

        try {
            // Load default AI provider from database
            MAIProvider providerConfig = MAIProvider.getDefault(Env.getCtx(), null);

            if (providerConfig == null) {
                throw new RuntimeException("No AI provider configured. Please create an AIG_Provider record with IsDefault='Y'");
            }

            log.info("Initializing Sales Agent with provider: " + providerConfig.getName() +
                     " (Type: " + providerConfig.getAIGProviderType() + ")");

            // Create LangChain4j ChatLanguageModel from provider config
            ChatLanguageModel model = providerFactory.createModel(providerConfig);

            // Build the AI agent with tools and chat memory
            agent = AiServices.builder(SalesAgentInterface.class)
                .chatLanguageModel(model)
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
        return agent.analyzeOpportunity(opportunityId);
    }

    /**
     * Analyze business partner sales performance.
     *
     * @param partnerId the C_BPartner_ID
     * @return performance analysis
     */
    public String analyzePartnerSales(int partnerId) {
        ensureInitialized();
        return agent.analyzePartnerSales(partnerId);
    }

    /**
     * Get sales insights from natural language query.
     *
     * @param query natural language query
     * @return AI-generated response with insights
     */
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
        String analyzeOpportunity(int opportunityId);

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
        String analyzePartnerSales(int partnerId);
    }
}
