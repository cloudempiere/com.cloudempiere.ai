package com.cloudempiere.ai.purchasing.agent;

import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.cloudempiere.ai.provider.langchain4j.ILangChain4jProviderFactory;
import com.cloudempiere.ai.purchasing.tools.PurchasingTools;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

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
    private volatile ILangChain4jProviderFactory providerFactory;

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
            // TODO: Implement provider configuration loading
            // Need to get MAIProvider from database or configuration
            // Then call: ChatLanguageModel model = providerFactory.createModel(config);

            log.warning("Purchasing Agent activation deferred - provider configuration not yet implemented");

            // Stub for future implementation:
            // MAIProvider config = loadProviderConfig();
            // ChatLanguageModel model = providerFactory.createModel(config);
            // agent = AiServices.builder(PurchasingAgentInterface.class)
            //     .chatLanguageModel(model)
            //     .tools(purchasingTools)
            //     .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
            //     .build();

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
        @SystemMessage("You are a procurement analyst AI specialized in iDempiere ERP purchasing data analysis.\n\n" +
            "Your role is to help procurement teams with:\n" +
            "- Analyzing purchase orders and spending patterns\n" +
            "- Evaluating vendor performance and reliability\n" +
            "- Identifying cost optimization opportunities\n" +
            "- Tracking procurement timelines and delivery performance\n" +
            "- Providing insights on supply chain efficiency\n\n" +
            "You have access to the following data through tools:\n" +
            "- Vendor information (suppliers, payment terms, pricing)\n" +
            "- Purchase orders (historical and current)\n" +
            "- Vendor invoices (payment history, outstanding amounts)\n" +
            "- Product catalog (for procurement planning)\n" +
            "- Purchasing performance metrics\n\n" +
            "When analyzing data:\n" +
            "1. Always verify data exists before drawing conclusions\n" +
            "2. Provide specific numbers, dates, and amounts\n" +
            "3. Identify spending trends and cost-saving opportunities\n" +
            "4. Assess vendor reliability and performance\n" +
            "5. Consider payment terms, delivery schedules, and quality\n\n" +
            "Security boundaries:\n" +
            "- You can READ purchasing, order, and vendor data\n" +
            "- You CANNOT modify orders or invoices\n" +
            "- You CANNOT access sales, inventory financial data beyond what's needed for analysis\n" +
            "- You CANNOT approve or complete documents\n\n" +
            "Always be professional, analytical, and focused on cost optimization and efficiency.")
        String chat(@UserMessage String query);

        /**
         * Analyze a specific purchase order.
         *
         * @param orderId the order to analyze
         * @return analysis summary
         */
        @UserMessage("Analyze purchase order {{orderId}}. Provide:\n" +
            "1. Order summary (total, status, vendor, promised date)\n" +
            "2. Line items breakdown (products, quantities, prices)\n" +
            "3. Vendor background and historical performance\n" +
            "4. Delivery timeline and risks\n" +
            "5. Price comparison with historical orders\n" +
            "6. Recommended actions or concerns\n\n" +
            "Be specific with numbers, dates, and actionable insights.")
        String analyzePurchaseOrder(int orderId);

        /**
         * Analyze vendor performance.
         *
         * @param partnerId the vendor to analyze
         * @return performance analysis
         */
        @UserMessage("Analyze vendor performance for business partner {{partnerId}}. Provide:\n" +
            "1. Vendor profile (payment terms, pricing agreements)\n" +
            "2. Historical purchasing volume and trends\n" +
            "3. Order frequency and delivery performance\n" +
            "4. Invoice payment history and outstanding amounts\n" +
            "5. Product categories supplied\n" +
            "6. Quality and reliability assessment\n" +
            "7. Cost optimization opportunities and recommendations\n\n" +
            "Support your analysis with specific data, trends, and benchmarks.")
        String analyzeVendorPerformance(int partnerId);
    }
}
