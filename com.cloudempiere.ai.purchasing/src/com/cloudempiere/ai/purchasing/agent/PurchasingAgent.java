package com.cloudempiere.ai.purchasing.agent;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.factory.IAIProviderFactory;
import com.cloudempiere.ai.purchasing.tools.PurchasingTools;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Purchasing domain AI agent.
 *
 * <p>This agent provides AI-powered purchasing assistance including:</p>
 * <ul>
 *   <li>Purchase order analysis and tracking</li>
 *   <li>Vendor performance analysis and evaluation</li>
 *   <li>Spending pattern analysis</li>
 *   <li>Cost optimization recommendations</li>
 *   <li>Procurement insights and forecasting</li>
 * </ul>
 *
 * <p><b>Architecture Layer:</b> Domain Agent Layer (Purchasing)</p>
 * <p><b>Boundary:</b> {@link com.cloudempiere.ai.purchasing.boundary.PurchasingDomainBoundary}</p>
 * <p><b>Tools:</b> {@link com.cloudempiere.ai.purchasing.tools.PurchasingTools}</p>
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
@Component(service = PurchasingAgent.class, immediate = true)
public class PurchasingAgent {

    private static final Logger log = Logger.getLogger(PurchasingAgent.class.getName());

    @Reference
    private volatile IAIProviderFactory providerFactory;

    @Reference
    private volatile PurchasingTools purchasingTools;

    private PurchasingAgentInterface agent;

    /**
     * Activate the purchasing agent.
     *
     * <p>This method is called by OSGi when the component is activated.
     * It initializes the LangChain4j agent with the configured AI provider
     * and purchasing tools.</p>
     */
    @Activate
    protected void activate() {
        log.info("Activating Purchasing Agent...");

        try {
            // Get default AI provider (TODO: make configurable per client)
            IAIProvider provider = providerFactory.getDefaultProvider();

            if (provider == null) {
                log.warning("No AI provider configured. Purchasing agent will not be available.");
                return;
            }

            // Build agent with LangChain4j
            agent = AiServices.builder(PurchasingAgentInterface.class)
                .chatLanguageModel(provider.getChatModel())
                .tools(purchasingTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();

            log.info("Purchasing Agent activated successfully with provider: " + provider.getProviderName());

        } catch (Exception e) {
            log.severe("Failed to activate Purchasing Agent: " + e.getMessage());
            throw new RuntimeException("Purchasing Agent activation failed", e);
        }
    }

    /**
     * Analyze a purchase order.
     *
     * @param orderId the C_Order_ID
     * @return analysis summary
     */
    public String analyzePurchaseOrder(int orderId) {
        if (agent == null) {
            return "Purchasing agent not available. Please configure an AI provider.";
        }
        return agent.analyzePurchaseOrder(orderId);
    }

    /**
     * Analyze vendor performance.
     *
     * @param partnerId the C_BPartner_ID
     * @return performance analysis
     */
    public String analyzeVendorPerformance(int partnerId) {
        if (agent == null) {
            return "Purchasing agent not available. Please configure an AI provider.";
        }
        return agent.analyzeVendorPerformance(partnerId);
    }

    /**
     * Get purchasing insights from natural language query.
     *
     * @param query natural language query
     * @return AI-generated response with insights
     */
    public String chat(String query) {
        if (agent == null) {
            return "Purchasing agent not available. Please configure an AI provider.";
        }
        return agent.chat(query);
    }

    /**
     * Purchasing agent interface for LangChain4j.
     *
     * <p>This interface defines the AI agent's capabilities using
     * LangChain4j's declarative service annotations.</p>
     */
    interface PurchasingAgentInterface {

        /**
         * System message defining the agent's role and capabilities.
         */
        @SystemMessage("""
            You are a procurement analyst AI specialized in iDempiere ERP purchasing data analysis.

            Your role is to help procurement teams with:
            - Analyzing purchase orders and spending patterns
            - Evaluating vendor performance and reliability
            - Identifying cost optimization opportunities
            - Tracking procurement timelines and delivery performance
            - Providing insights on supply chain efficiency

            You have access to the following data through tools:
            - Vendor information (suppliers, payment terms, pricing)
            - Purchase orders (historical and current)
            - Vendor invoices (payment history, outstanding amounts)
            - Product catalog (for procurement planning)
            - Purchasing performance metrics

            When analyzing data:
            1. Always verify data exists before drawing conclusions
            2. Provide specific numbers, dates, and amounts
            3. Identify spending trends and cost-saving opportunities
            4. Assess vendor reliability and performance
            5. Consider payment terms, delivery schedules, and quality

            Security boundaries:
            - You can READ purchasing, order, and vendor data
            - You CANNOT modify orders or invoices
            - You CANNOT access sales, inventory financial data beyond what's needed for analysis
            - You CANNOT approve or complete documents

            Always be professional, analytical, and focused on cost optimization and efficiency.
            """)
        String chat(@UserMessage String query);

        /**
         * Analyze a specific purchase order.
         *
         * @param orderId the order to analyze
         * @return analysis summary
         */
        @UserMessage("""
            Analyze purchase order {{orderId}}. Provide:
            1. Order summary (total, status, vendor, promised date)
            2. Line items breakdown (products, quantities, prices)
            3. Vendor background and historical performance
            4. Delivery timeline and risks
            5. Price comparison with historical orders
            6. Recommended actions or concerns

            Be specific with numbers, dates, and actionable insights.
            """)
        String analyzePurchaseOrder(int orderId);

        /**
         * Analyze vendor performance.
         *
         * @param partnerId the vendor to analyze
         * @return performance analysis
         */
        @UserMessage("""
            Analyze vendor performance for business partner {{partnerId}}. Provide:
            1. Vendor profile (payment terms, pricing agreements)
            2. Historical purchasing volume and trends
            3. Order frequency and delivery performance
            4. Invoice payment history and outstanding amounts
            5. Product categories supplied
            6. Quality and reliability assessment
            7. Cost optimization opportunities and recommendations

            Support your analysis with specific data, trends, and benchmarks.
            """)
        String analyzeVendorPerformance(int partnerId);
    }
}
