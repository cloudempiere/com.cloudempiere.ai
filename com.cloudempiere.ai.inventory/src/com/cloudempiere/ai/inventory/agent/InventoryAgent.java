package com.cloudempiere.ai.inventory.agent;

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
import com.cloudempiere.ai.inventory.tools.InventoryTools;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.ILangChain4jProviderFactory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import dev.langchain4j.service.guardrail.OutputGuardrails;

@Component(service = {InventoryAgent.class, com.cloudempiere.ai.boundary.IDomainAgent.class}, immediate = true)
public class InventoryAgent extends AbstractDomainAgent {
    private static final Logger log = Logger.getLogger(InventoryAgent.class.getName());

    private static final String[] KEYWORDS = {
        "inventory", "stock", "warehouse", "product", "sku", "storage",
        "availability", "on hand", "reserved", "shortage"
    };

    private static final String[] TABLE_HINTS = {"storage", "product", "inventory"};

    public InventoryAgent() {
        super("inventory", KEYWORDS, TABLE_HINTS);
    }

    // CLD-1955: OPTIONAL+DYNAMIC so the agent can activate even when the LangChain4j provider bundle is absent
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ILangChain4jProviderFactory providerFactory;

    @Reference
    private volatile InventoryTools inventoryTools;

    private InventoryAgentInterface agent;

    @Activate
    protected void activate() {
        long startTime = System.currentTimeMillis();
        log.warning("[STARTUP TIMING] InventoryAgent.activate() START");

        log.info("Inventory Agent OSGi component activated and ready");

        long elapsed = System.currentTimeMillis() - startTime;
        log.warning("[STARTUP TIMING] InventoryAgent.activate() COMPLETED in " + elapsed + "ms");
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

            log.info("Initializing Inventory Agent with provider: " + providerConfig.getName());

            ChatModel model = providerFactory.createModel(providerConfig);

            agent = AiServices.builder(InventoryAgentInterface.class)
                .chatModel(model)
                .tools(inventoryTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();

            log.info("Inventory Agent initialized successfully");

        } catch (Exception e) {
            log.severe("Failed to initialize Inventory Agent: " + e.getMessage());
            throw new RuntimeException("Inventory Agent initialization failed: " + e.getMessage(), e);
        }
    }

    public String analyzeStock(int productId) {
        ensureInitialized();
        return formatStockAnalysis(agent.analyzeStock(productId));
    }

    /**
     * Render a structured {@link StockAnalysis} as the markdown text the chat UI expects.
     */
    private static String formatStockAnalysis(StockAnalysis a) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Current Levels**\n").append(a.currentLevels()).append("\n\n");
        sb.append("**Trends**\n").append(a.trends()).append("\n\n");
        sb.append("**Recommendations**\n");
        List<String> recommendations = a.recommendations();
        if (recommendations == null || recommendations.isEmpty()) {
            sb.append("- None identified\n");
        } else {
            for (String r : recommendations) {
                sb.append("- ").append(r).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String chat(String query) {
        ensureInitialized();
        return agent.chat(query);
    }

    @InputGuardrails(InputGuard.class)
    @OutputGuardrails(OutputGuard.class)
    interface InventoryAgentInterface {
        @SystemMessage("You are an inventory management AI for iDempiere ERP. Analyze stock levels, warehouse operations, and inventory movements.")
        String chat(@UserMessage String query);

        @UserMessage("Analyze stock for product {{productId}} including current levels, trends, and recommendations")
        StockAnalysis analyzeStock(int productId);
    }

    /**
     * Structured result for {@link InventoryAgentInterface#analyzeStock(int)}.
     */
    record StockAnalysis(
        @Description("Current stock levels") String currentLevels,
        @Description("Stock movement trends") String trends,
        @Description("Recommendations, one item per entry") List<String> recommendations
    ) {
    }
}
