package com.cloudempiere.ai.inventory.agent;

import java.util.Map;
import java.util.logging.Logger;

import org.compiere.util.Env;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.cloudempiere.ai.boundary.IDomainAgent;
import com.cloudempiere.ai.inventory.tools.InventoryTools;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.ILangChain4jProviderFactory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@Component(service = {InventoryAgent.class, IDomainAgent.class}, immediate = true)
public class InventoryAgent implements IDomainAgent {
    private static final Logger log = Logger.getLogger(InventoryAgent.class.getName());

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

            ChatLanguageModel model = providerFactory.createModel(providerConfig);

            agent = AiServices.builder(InventoryAgentInterface.class)
                .chatLanguageModel(model)
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
        return agent.analyzeStock(productId);
    }

    public String chat(String query) {
        ensureInitialized();
        return agent.chat(query);
    }

    @Override
    public String getDomain() {
        return "inventory";
    }

    @Override
    public boolean canHandle(String query, Map<String, Object> context) {
        if (query == null) return false;
        String lower = query.toLowerCase();
        String[] keywords = {"inventory", "stock", "warehouse", "product", "sku", "storage", "availability", "on hand", "reserved", "shortage"};
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        if (context != null && context.containsKey("tableName")) {
            String tn = ((String) context.get("tableName")).toLowerCase();
            if (tn.contains("storage") || tn.contains("product") || tn.contains("inventory")) return true;
        }
        return false;
    }

    @Override
    public String process(String query, Map<String, Object> context) {
        return chat(query);
    }

    interface InventoryAgentInterface {
        @SystemMessage("You are an inventory management AI for iDempiere ERP. Analyze stock levels, warehouse operations, and inventory movements.")
        String chat(@UserMessage String query);

        @UserMessage("Analyze stock for product {{productId}} including current levels, trends, and recommendations")
        String analyzeStock(int productId);
    }
}
