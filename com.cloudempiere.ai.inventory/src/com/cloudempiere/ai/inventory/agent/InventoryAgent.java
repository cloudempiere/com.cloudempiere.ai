package com.cloudempiere.ai.inventory.agent;

import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.cloudempiere.ai.inventory.tools.InventoryTools;
import com.cloudempiere.ai.provider.langchain4j.ILangChain4jProviderFactory;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@Component(service = InventoryAgent.class, immediate = true)
public class InventoryAgent {
    private static final Logger log = Logger.getLogger(InventoryAgent.class.getName());

    @Reference
    private volatile ILangChain4jProviderFactory providerFactory;

    @Reference
    private volatile InventoryTools inventoryTools;

    private InventoryAgentInterface agent;

    @Activate
    protected void activate() {
        log.info("Activating Inventory Agent...");
        // TODO: Implement provider configuration loading
        // Need to get MAIProvider from database or configuration
        // Then call: ChatLanguageModel model = providerFactory.createModel(config);
        log.warning("Inventory Agent activation deferred - provider configuration not yet implemented");
    }

    public String analyzeStock(int productId) {
        return agent != null ? agent.analyzeStock(productId) : "Agent not available";
    }

    public String chat(String query) {
        return agent != null ? agent.chat(query) : "Agent not available";
    }

    interface InventoryAgentInterface {
        @SystemMessage("You are an inventory management AI for iDempiere ERP. Analyze stock levels, warehouse operations, and inventory movements.")
        String chat(@UserMessage String query);

        @UserMessage("Analyze stock for product {{productId}} including current levels, trends, and recommendations")
        String analyzeStock(int productId);
    }
}
