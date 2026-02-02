package com.cloudempiere.ai.inventory.agent;

import org.osgi.service.component.annotations.*;
import com.cloudempiere.ai.provider.factory.IAIProviderFactory;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.inventory.tools.InventoryTools;
import dev.langchain4j.service.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import java.util.logging.Logger;

@Component(service = InventoryAgent.class, immediate = true)
public class InventoryAgent {
    private static final Logger log = Logger.getLogger(InventoryAgent.class.getName());

    @Reference
    private volatile IAIProviderFactory providerFactory;

    @Reference
    private volatile InventoryTools inventoryTools;

    private InventoryAgentInterface agent;

    @Activate
    protected void activate() {
        log.info("Activating Inventory Agent...");
        IAIProvider provider = providerFactory.getDefaultProvider();
        if (provider != null) {
            agent = AiServices.builder(InventoryAgentInterface.class)
                .chatLanguageModel(provider.getChatModel())
                .tools(inventoryTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();
            log.info("Inventory Agent activated");
        }
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
