package com.cloudempiere.ai.purchasing.agent;

import java.util.Map;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.cloudempiere.ai.boundary.IDomainAgent;

import com.cloudempiere.ai.provider.langchain4j.ILangChain4jProviderFactory;
import com.cloudempiere.ai.purchasing.tools.PurchasingTools;
import com.cloudempiere.ai.model.MAIProvider;

import org.compiere.util.Env;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

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
 * @author Cloudempiere AI Team
 * @version 1.0.0
 */
@Component(service = {PurchasingAgent.class, IDomainAgent.class}, immediate = true)
public class PurchasingAgent implements IDomainAgent {

    private static final Logger log = Logger.getLogger(PurchasingAgent.class.getName());

    // CLD-1955: OPTIONAL+DYNAMIC so the agent can activate even when the LangChain4j provider bundle is absent
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ILangChain4jProviderFactory providerFactory;

    @Reference
    private volatile PurchasingTools purchasingTools;

    private PurchasingAgentInterface agent;

    /**
     * Activate the purchasing agent.
     *
     * <p>This method is called by OSGi when the component is activated.
     * The actual agent initialization is deferred until first use.</p>
     */
    @Activate
    protected void activate() {
        long startTime = System.currentTimeMillis();
        log.warning("[STARTUP TIMING] PurchasingAgent.activate() START");

        log.info("Purchasing Agent OSGi component activated and ready");

        long elapsed = System.currentTimeMillis() - startTime;
        log.warning("[STARTUP TIMING] PurchasingAgent.activate() COMPLETED in " + elapsed + "ms");
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

            log.info("Initializing Purchasing Agent with provider: " + providerConfig.getName());

            ChatLanguageModel model = providerFactory.createModel(providerConfig);

            agent = AiServices.builder(PurchasingAgentInterface.class)
                .chatLanguageModel(model)
                .tools(purchasingTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();

            log.info("Purchasing Agent initialized successfully");

        } catch (Exception e) {
            log.severe("Failed to initialize Purchasing Agent: " + e.getMessage());
            throw new RuntimeException("Purchasing Agent initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze a purchase order.
     *
     * @param orderId the C_Order_ID
     * @return analysis summary
     */
    public String analyzePurchaseOrder(int orderId) {
        ensureInitialized();
        return agent.analyzePurchaseOrder(orderId);
    }

    /**
     * Analyze vendor performance.
     *
     * @param partnerId the C_BPartner_ID
     * @return performance analysis
     */
    public String analyzeVendorPerformance(int partnerId) {
        ensureInitialized();
        return agent.analyzeVendorPerformance(partnerId);
    }

    /**
     * Get purchasing insights from natural language query.
     *
     * @param query natural language query
     * @return AI-generated response with insights
     */
    public String chat(String query) {
        ensureInitialized();
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

    @Override
    public String getDomain() {
        return "purchasing";
    }

    @Override
    public boolean canHandle(String query, Map<String, Object> context) {
        if (query == null) return false;
        String lower = query.toLowerCase();
        String[] keywords = {"purchase", "vendor", "supplier", "procurement", "buying", "purchase order", "po", "requisition", "sourcing"};
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        if (context != null && context.containsKey("tableName")) {
            String tn = ((String) context.get("tableName")).toLowerCase();
            if (tn.contains("purchase") || tn.contains("vendor") || tn.contains("requisition")) return true;
        }
        return false;
    }

    @Override
    public String process(String query, Map<String, Object> context) {
        return chat(query);
    }
}
