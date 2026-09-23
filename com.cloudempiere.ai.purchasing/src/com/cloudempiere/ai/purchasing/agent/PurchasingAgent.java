package com.cloudempiere.ai.purchasing.agent;

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
import com.cloudempiere.ai.purchasing.tools.PurchasingTools;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import dev.langchain4j.service.guardrail.OutputGuardrails;

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
@Component(service = {PurchasingAgent.class, com.cloudempiere.ai.boundary.IDomainAgent.class}, immediate = true)
public class PurchasingAgent extends AbstractDomainAgent {

    private static final Logger log = Logger.getLogger(PurchasingAgent.class.getName());

    private static final String[] KEYWORDS = {
        "purchase", "vendor", "supplier", "procurement", "buying",
        "purchase order", "po", "requisition", "sourcing"
    };

    private static final String[] TABLE_HINTS = {"purchase", "vendor", "requisition"};

    public PurchasingAgent() {
        super("purchasing", KEYWORDS, TABLE_HINTS);
    }

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

            ChatModel model = providerFactory.createModel(providerConfig);

            agent = AiServices.builder(PurchasingAgentInterface.class)
                .chatModel(model)
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
        return formatPurchaseOrderAnalysis(agent.analyzePurchaseOrder(orderId));
    }

    /**
     * Analyze vendor performance.
     *
     * @param partnerId the C_BPartner_ID
     * @return performance analysis
     */
    public String analyzeVendorPerformance(int partnerId) {
        ensureInitialized();
        return formatVendorPerformanceAnalysis(agent.analyzeVendorPerformance(partnerId));
    }

    /**
     * Render a structured {@link PurchaseOrderAnalysis} as the markdown text the chat UI expects.
     */
    private static String formatPurchaseOrderAnalysis(PurchaseOrderAnalysis a) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Order Summary**\n").append(a.orderSummary()).append("\n\n");
        sb.append("**Line Items**\n").append(a.lineItems()).append("\n\n");
        sb.append("**Vendor Background and Performance**\n").append(a.vendorBackground()).append("\n\n");
        sb.append("**Delivery Timeline and Risks**\n").append(a.deliveryTimeline()).append("\n\n");
        sb.append("**Price Comparison**\n").append(a.priceComparison()).append("\n\n");
        appendList(sb, "Recommended Actions or Concerns", a.recommendedActions());
        return sb.toString();
    }

    /**
     * Render a structured {@link VendorPerformanceAnalysis} as the markdown text the chat UI expects.
     */
    private static String formatVendorPerformanceAnalysis(VendorPerformanceAnalysis a) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Vendor Profile**\n").append(a.vendorProfile()).append("\n\n");
        sb.append("**Purchasing Volume and Trends**\n").append(a.purchasingVolumeAndTrends()).append("\n\n");
        sb.append("**Order Frequency and Delivery Performance**\n").append(a.orderFrequencyAndDelivery()).append("\n\n");
        sb.append("**Invoice Payment History**\n").append(a.invoicePaymentHistory()).append("\n\n");
        sb.append("**Product Categories Supplied**\n").append(a.productCategories()).append("\n\n");
        sb.append("**Quality and Reliability Assessment**\n").append(a.qualityAssessment()).append("\n\n");
        appendList(sb, "Cost Optimization Opportunities", a.costOptimizationRecommendations());
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
     * Get purchasing insights from natural language query.
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
     * Purchasing agent interface for LangChain4j.
     *
     * <p>This interface defines the AI agent's capabilities using
     * LangChain4j's declarative service annotations.</p>
     */
    @InputGuardrails(InputGuard.class)
    @OutputGuardrails(OutputGuard.class)
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
        PurchaseOrderAnalysis analyzePurchaseOrder(int orderId);

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
        VendorPerformanceAnalysis analyzeVendorPerformance(int partnerId);
    }

    /**
     * Structured result for {@link PurchasingAgentInterface#analyzePurchaseOrder(int)}.
     */
    record PurchaseOrderAnalysis(
        @Description("Order summary: total, status, vendor, promised date") String orderSummary,
        @Description("Line items breakdown: products, quantities, prices") String lineItems,
        @Description("Vendor background and historical performance") String vendorBackground,
        @Description("Delivery timeline and risks") String deliveryTimeline,
        @Description("Price comparison with historical orders") String priceComparison,
        @Description("Recommended actions or concerns, one item per entry") List<String> recommendedActions
    ) {
    }

    /**
     * Structured result for {@link PurchasingAgentInterface#analyzeVendorPerformance(int)}.
     */
    record VendorPerformanceAnalysis(
        @Description("Vendor profile: payment terms, pricing agreements") String vendorProfile,
        @Description("Historical purchasing volume and trends") String purchasingVolumeAndTrends,
        @Description("Order frequency and delivery performance") String orderFrequencyAndDelivery,
        @Description("Invoice payment history and outstanding amounts") String invoicePaymentHistory,
        @Description("Product categories supplied") String productCategories,
        @Description("Quality and reliability assessment") String qualityAssessment,
        @Description("Cost optimization opportunities and recommendations, one item per entry") List<String> costOptimizationRecommendations
    ) {
    }

}
