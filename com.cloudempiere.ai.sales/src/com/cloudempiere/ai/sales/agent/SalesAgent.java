package com.cloudempiere.ai.sales.agent;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.factory.IAIProviderFactory;
import com.cloudempiere.ai.sales.tools.SalesTools;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.Properties;
import java.util.logging.Logger;

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
    private volatile IAIProviderFactory providerFactory;

    @Reference
    private volatile SalesTools salesTools;

    private SalesAgentInterface agent;

    /**
     * Activate the sales agent.
     *
     * <p>This method is called by OSGi when the component is activated.
     * It initializes the LangChain4j agent with the configured AI provider
     * and sales tools.</p>
     */
    @Activate
    protected void activate() {
        log.info("Activating Sales Agent...");

        try {
            // Get default AI provider (TODO: make configurable per client)
            IAIProvider provider = providerFactory.getDefaultProvider();

            if (provider == null) {
                log.warning("No AI provider configured. Sales agent will not be available.");
                return;
            }

            // Build agent with LangChain4j
            agent = AiServices.builder(SalesAgentInterface.class)
                .chatLanguageModel(provider.getChatModel())
                .tools(salesTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();

            log.info("Sales Agent activated successfully with provider: " + provider.getProviderName());

        } catch (Exception e) {
            log.severe("Failed to activate Sales Agent: " + e.getMessage());
            throw new RuntimeException("Sales Agent activation failed", e);
        }
    }

    /**
     * Analyze a sales opportunity.
     *
     * @param opportunityId the C_Opportunity_ID
     * @return analysis summary
     */
    public String analyzeOpportunity(int opportunityId) {
        if (agent == null) {
            return "Sales agent not available. Please configure an AI provider.";
        }
        return agent.analyzeOpportunity(opportunityId);
    }

    /**
     * Analyze business partner sales performance.
     *
     * @param partnerId the C_BPartner_ID
     * @return performance analysis
     */
    public String analyzePartnerSales(int partnerId) {
        if (agent == null) {
            return "Sales agent not available. Please configure an AI provider.";
        }
        return agent.analyzePartnerSales(partnerId);
    }

    /**
     * Get sales insights from natural language query.
     *
     * @param query natural language query
     * @return AI-generated response with insights
     */
    public String chat(String query) {
        if (agent == null) {
            return "Sales agent not available. Please configure an AI provider.";
        }
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
        @SystemMessage("""
            You are a sales analyst AI specialized in iDempiere ERP sales data analysis.

            Your role is to help sales teams with:
            - Analyzing sales opportunities and forecasting
            - Understanding business partner relationships and history
            - Identifying sales trends and patterns
            - Recommending next actions for opportunities
            - Providing insights on sales pipeline health

            You have access to the following data through tools:
            - Business partner information (customers, contacts, credit limits)
            - Sales orders (historical and current)
            - Sales opportunities (pipeline, stages, forecasts)
            - Product catalog (for recommendations)
            - Sales performance metrics

            When analyzing data:
            1. Always verify data exists before drawing conclusions
            2. Provide specific numbers and dates when available
            3. Identify trends and anomalies
            4. Suggest actionable next steps
            5. Consider business context (credit limits, payment terms, etc.)

            Security boundaries:
            - You can READ sales, opportunity, and partner data
            - You can CREATE/UPDATE draft opportunities only
            - You CANNOT modify orders, invoices, or master data
            - You CANNOT access inventory, purchasing, or financial data

            Always be professional, concise, and data-driven in your responses.
            """)
        String chat(@UserMessage String query);

        /**
         * Analyze a specific sales opportunity.
         *
         * @param opportunityId the opportunity to analyze
         * @return analysis summary
         */
        @UserMessage("""
            Analyze sales opportunity {{opportunityId}}. Provide:
            1. Opportunity summary (amount, probability, stage, close date)
            2. Business partner background and history
            3. Products/services in the opportunity
            4. Risk factors and concerns
            5. Recommended next actions
            6. Likelihood assessment based on historical data

            Be specific with numbers, dates, and actionable insights.
            """)
        String analyzeOpportunity(int opportunityId);

        /**
         * Analyze business partner sales performance.
         *
         * @param partnerId the business partner to analyze
         * @return performance analysis
         */
        @UserMessage("""
            Analyze sales performance for business partner {{partnerId}}. Provide:
            1. Customer profile (credit limit, payment terms, outstanding balance)
            2. Historical sales volume and trends
            3. Order frequency and patterns
            4. Product preferences
            5. Opportunities in pipeline
            6. Risk factors (credit issues, payment delays)
            7. Growth opportunities and recommendations

            Support your analysis with specific data and trends.
            """)
        String analyzePartnerSales(int partnerId);
    }
}
