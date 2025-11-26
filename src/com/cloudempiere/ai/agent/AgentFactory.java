/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.agent;

import java.util.Properties;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.AIProviderException;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.factory.AIProviderFactory;
import com.cloudempiere.ai.tool.ToolRegistry;
import com.cloudempiere.ai.tool.impl.DatabaseQueryTool;
import com.cloudempiere.ai.tool.impl.FieldMetadataTool;
import com.cloudempiere.ai.tool.impl.TableMetadataTool;

/**
 * Factory for creating AI Agents
 *
 * <p>Provides convenient methods for creating agents with default configurations
 * and tools. Integrates with the existing IAIProvider factory system.
 *
 * <p>Example usage:
 * <pre>
 * // Create agent with default tools
 * IAIAgent agent = AgentFactory.createDefaultAgent(ctx, providerId);
 *
 * // Execute
 * AgentContext context = new AgentContext(ctx, providerId);
 * AgentResponse response = agent.execute("What products do we have?", context);
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AgentFactory {

    private static final CLogger log = CLogger.getCLogger(AgentFactory.class);

    /**
     * Create an agent with default ERP tools
     *
     * <p>Creates an agent with the following tools pre-registered:
     * <ul>
     *   <li>DatabaseQueryTool - Execute SQL queries</li>
     *   <li>FieldMetadataTool - Get field/column metadata</li>
     *   <li>TableMetadataTool - Get table metadata</li>
     * </ul>
     *
     * @param ctx iDempiere context
     * @param providerId AI Provider ID from AIG_Provider table
     * @return configured agent ready for execution
     * @throws AgentException if agent creation fails
     */
    public static IAIAgent createDefaultAgent(Properties ctx, int providerId)
            throws AgentException {

        return createDefaultAgent(ctx, providerId, null);
    }

    /**
     * Create an agent with default ERP tools and custom name
     *
     * @param ctx iDempiere context
     * @param providerId AI Provider ID
     * @param agentName custom agent name (null for default)
     * @return configured agent
     * @throws AgentException if creation fails
     */
    public static IAIAgent createDefaultAgent(Properties ctx, int providerId, String agentName)
            throws AgentException {

        try {
            // Get provider from factory
            AIProviderFactory factory = new AIProviderFactory();
            IAIProvider provider = factory.get(ctx, providerId, null);

            // Create tool registry with default tools
            ToolRegistry registry = createDefaultToolRegistry();

            // Create agent
            String name = agentName != null ? agentName : "iDempiere-Agent";
            BaseAgent agent = new BaseAgent(name, provider, registry);

            // Set default system prompt
            agent.setSystemPrompt(buildERPSystemPrompt());

            log.info("Created default agent: " + name + " with " +
                    registry.size() + " tools");

            return agent;

        } catch (AIProviderException e) {
            throw AgentException.initializationError(
                "Failed to initialize provider: " + e.getMessage()
            );
        } catch (Exception e) {
            throw AgentException.initializationError(
                "Failed to create agent: " + e.getMessage()
            );
        }
    }

    /**
     * Create an agent with custom tool registry
     *
     * @param ctx iDempiere context
     * @param providerId AI Provider ID
     * @param agentName agent name
     * @param registry custom tool registry
     * @return configured agent
     * @throws AgentException if creation fails
     */
    public static IAIAgent createAgent(Properties ctx, int providerId,
                                       String agentName, ToolRegistry registry)
            throws AgentException {

        try {
            AIProviderFactory factory = new AIProviderFactory();
            IAIProvider provider = factory.get(ctx, providerId, null);

            BaseAgent agent = new BaseAgent(agentName, provider, registry);
            agent.setSystemPrompt(buildERPSystemPrompt());

            return agent;

        } catch (AIProviderException e) {
            throw AgentException.initializationError(
                "Failed to initialize provider: " + e.getMessage()
            );
        }
    }

    /**
     * Create an agent with an existing provider
     *
     * @param agentName agent name
     * @param provider AI provider to use
     * @return configured agent
     */
    public static IAIAgent createAgent(String agentName, IAIProvider provider) {
        ToolRegistry registry = createDefaultToolRegistry();
        BaseAgent agent = new BaseAgent(agentName, provider, registry);
        agent.setSystemPrompt(buildERPSystemPrompt());
        return agent;
    }

    /**
     * Create the default tool registry with ERP tools
     *
     * @return configured tool registry
     */
    public static ToolRegistry createDefaultToolRegistry() {
        ToolRegistry registry = new ToolRegistry();

        // Register default tools
        registry.register(new DatabaseQueryTool());
        registry.register(new FieldMetadataTool());
        registry.register(new TableMetadataTool());

        log.fine("Created default tool registry with " + registry.size() + " tools");

        return registry;
    }

    /**
     * Build the default ERP system prompt
     */
    private static String buildERPSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI assistant integrated with iDempiere ERP system.\n\n");
        sb.append("## Your Capabilities\n");
        sb.append("You have access to tools that allow you to:\n");
        sb.append("1. **Query the database** - Execute SQL SELECT queries to retrieve data\n");
        sb.append("2. **Get table metadata** - Understand the structure of database tables\n");
        sb.append("3. **Get field metadata** - Get details about specific columns and their types\n\n");
        sb.append("## Important Guidelines\n\n");
        sb.append("### Database Queries\n");
        sb.append("- Always write efficient SQL queries\n");
        sb.append("- Use appropriate WHERE clauses to filter data\n");
        sb.append("- Limit results to avoid returning too much data\n");
        sb.append("- The system automatically applies security filtering based on user permissions\n\n");
        sb.append("### Understanding the Data Model\n");
        sb.append("- Before writing complex queries, use the metadata tools to understand table structures\n");
        sb.append("- iDempiere tables use prefixes: M_ (Material), C_ (Core/Customer), AD_ (Application Dictionary)\n");
        sb.append("- Most tables have AD_Client_ID and AD_Org_ID for multi-tenancy\n\n");
        sb.append("### Common Tables\n");
        sb.append("- M_Product - Products\n");
        sb.append("- C_BPartner - Business Partners (customers, vendors)\n");
        sb.append("- C_Order / C_OrderLine - Sales Orders\n");
        sb.append("- M_InOut / M_InOutLine - Shipments\n");
        sb.append("- C_Invoice / C_InvoiceLine - Invoices\n");
        sb.append("- M_Warehouse - Warehouses\n");
        sb.append("- M_Storage - Stock levels\n\n");
        sb.append("### Response Format\n");
        sb.append("- Be concise and helpful\n");
        sb.append("- Present data in clear, readable format\n");
        sb.append("- If a query returns many rows, summarize the key findings\n");
        sb.append("- Explain any insights you discover in the data\n\n");
        sb.append("### Security\n");
        sb.append("- Never attempt to bypass security restrictions\n");
        sb.append("- Only use SELECT queries (no INSERT, UPDATE, DELETE)\n");
        sb.append("- Respect the user's data access permissions\n");
        return sb.toString();
    }

    /**
     * Create an agent context from iDempiere context
     *
     * <p>Convenience method for creating an AgentContext with default limits.
     *
     * @param ctx iDempiere context
     * @param providerId provider ID
     * @return configured agent context
     */
    public static AgentContext createContext(Properties ctx, int providerId) {
        return new AgentContext(ctx, providerId);
    }

    /**
     * Create an agent context with custom limits
     *
     * @param ctx iDempiere context
     * @param providerId provider ID
     * @param maxCostUSD maximum cost in USD
     * @param maxToolCalls maximum tool calls
     * @return configured agent context
     */
    public static AgentContext createContext(Properties ctx, int providerId,
                                            double maxCostUSD, int maxToolCalls) {
        AgentContext context = new AgentContext(ctx, providerId);
        context.setMaxCostUSD(maxCostUSD);
        context.setMaxToolCalls(maxToolCalls);
        return context;
    }
}
