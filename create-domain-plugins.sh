#!/bin/bash
# Script to create remaining domain plugin files
# Usage: ./create-domain-plugins.sh

set -e

echo "Creating remaining domain plugin files..."

# Complete Inventory Agent
cat > com.cloudempiere.ai.inventory/src/com/cloudempiere/ai/inventory/agent/InventoryAgent.java <<'EOF'
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
EOF

# Copy and adapt MANIFEST, pom.xml, etc for Inventory
cp com.cloudempiere.ai.sales/META-INF/MANIFEST.MF com.cloudempiere.ai.inventory/META-INF/
sed -i '' 's/sales/inventory/g' com.cloudempiere.ai.inventory/META-INF/MANIFEST.MF
sed -i '' 's/Sales/Inventory/g' com.cloudempiere.ai.inventory/META-INF/MANIFEST.MF

cp com.cloudempiere.ai.sales/pom.xml com.cloudempiere.ai.inventory/
sed -i '' 's/sales/inventory/g' com.cloudempiere.ai.inventory/pom.xml
sed -i '' 's/Sales/Inventory/g' com.cloudempiere.ai.inventory/pom.xml

cp com.cloudempiere.ai.sales/build.properties com.cloudempiere.ai.inventory/
cp com.cloudempiere.ai.sales/.project com.cloudempiere.ai.inventory/
cp com.cloudempiere.ai.sales/.classpath com.cloudempiere.ai.inventory/

sed -i '' 's/sales/inventory/g' com.cloudempiere.ai.inventory/.project
sed -i '' 's/sales/inventory/g' com.cloudempiere.ai.inventory/.classpath

# Create OSGI-INF files for Inventory
cat > com.cloudempiere.ai.inventory/OSGI-INF/com.cloudempiere.ai.inventory.tools.InventoryTools.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.3.0"
               name="com.cloudempiere.ai.inventory.tools.InventoryTools"
               immediate="true">
   <implementation class="com.cloudempiere.ai.inventory.tools.InventoryTools"/>
   <service><provide interface="com.cloudempiere.ai.inventory.tools.InventoryTools"/></service>
   <reference bind="setDbExecutor" cardinality="1..1" field="dbExecutor"
              interface="com.cloudempiere.ai.database.SecureDatabaseQueryExecutor"
              name="SecureDatabaseQueryExecutor" policy="static"/>
</scr:component>
EOF

cat > com.cloudempiere.ai.inventory/OSGI-INF/com.cloudempiere.ai.inventory.agent.InventoryAgent.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.3.0"
               name="com.cloudempiere.ai.inventory.agent.InventoryAgent"
               activate="activate" immediate="true">
   <implementation class="com.cloudempiere.ai.inventory.agent.InventoryAgent"/>
   <service><provide interface="com.cloudempiere.ai.inventory.agent.InventoryAgent"/></service>
   <reference bind="setProviderFactory" cardinality="1..1" field="providerFactory"
              interface="com.cloudempiere.ai.provider.factory.IAIProviderFactory"
              name="IAIProviderFactory" policy="static"/>
   <reference bind="setInventoryTools" cardinality="1..1" field="inventoryTools"
              interface="com.cloudempiere.ai.inventory.tools.InventoryTools"
              name="InventoryTools" policy="static"/>
</scr:component>
EOF

echo "✓ Inventory plugin completed"

# Create Purchasing, Support, KB plugins using the same pattern
for DOMAIN in purchasing support kb; do
    DOMAIN_CAP=$(echo $DOMAIN | sed 's/\b\w/\U&/g')

    echo "Creating $DOMAIN_CAP domain plugin..."

    mkdir -p com.cloudempiere.ai.$DOMAIN/src/com/cloudempiere/ai/$DOMAIN/{agent,boundary,tools}
    mkdir -p com.cloudempiere.ai.$DOMAIN/{META-INF,OSGI-INF}

    # Copy and adapt from sales
    cp -r com.cloudempiere.ai.sales/* com.cloudempiere.ai.$DOMAIN/

    # Replace sales with domain name in all files
    find com.cloudempiere.ai.$DOMAIN -type f -exec sed -i '' "s/sales/$DOMAIN/g" {} \;
    find com.cloudempiere.ai.$DOMAIN -type f -exec sed -i '' "s/Sales/$DOMAIN_CAP/g" {} \;

    echo "✓ $DOMAIN_CAP plugin structure created"
done

echo ""
echo "All domain plugins created successfully!"
echo "Next steps:"
echo "1. Review and customize each plugin's boundary tables"
echo "2. Add domain-specific tool methods"
echo "3. Customize agent system messages"
echo "4. Build: mvn clean install"
