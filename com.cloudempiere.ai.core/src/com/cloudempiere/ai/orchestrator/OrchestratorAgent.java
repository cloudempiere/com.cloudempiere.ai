package com.cloudempiere.ai.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.cloudempiere.ai.boundary.IDomainAgent;
import com.cloudempiere.ai.boundary.IOrchestrator;

/**
 * Central orchestrator agent that routes queries to specialized domain agents.
 *
 * <p>This orchestrator implements the agent coordination strategy defined in ADR-010
 * using a factory pattern with dynamic service discovery. Domain agents are discovered
 * automatically via OSGi service tracking - no hard-coded dependencies.</p>
 *
 * <p><b>Routing Strategy:</b></p>
 * <ol>
 *   <li>Query all available domain agents via canHandle()</li>
 *   <li>Route to agent(s) that report they can handle the query</li>
 *   <li>Aggregate responses if multiple agents handle the query</li>
 *   <li>Fallback to generic response if no agents can handle</li>
 * </ol>
 *
 * <p><b>Benefits of Factory Pattern:</b></p>
 * <ul>
 *   <li>No Require-Bundle dependencies - uses Import-Package only</li>
 *   <li>New domain agents auto-discovered without orchestrator changes</li>
 *   <li>Domain agents can be added/removed at runtime</li>
 *   <li>Follows OSGi service-oriented architecture</li>
 * </ul>
 *
 * <p><b>Related ADRs:</b></p>
 * <ul>
 *   <li><a href="../../docs/adr/010-agent-orchestration-architecture.md">ADR-010: Agent Orchestration</a></li>
 *   <li><a href="../../docs/adr/009-domain-boundaries-agent-scope.md">ADR-009: Domain Boundaries</a></li>
 * </ul>
 *
 * @author CloudEmpiere AI Team
 * @version 0.32.0
 */
@Component(service = {OrchestratorAgent.class, IOrchestrator.class}, immediate = true)
public class OrchestratorAgent implements IOrchestrator {

    private static final Logger log = Logger.getLogger(OrchestratorAgent.class.getName());

    /**
     * Dynamic list of domain agents.
     * OSGi DS automatically adds/removes agents as they become available/unavailable.
     */
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    private volatile List<IDomainAgent> domainAgents = new CopyOnWriteArrayList<>();

    @Activate
    protected void activate() {
        long startTime = System.currentTimeMillis();
        log.warning("[STARTUP TIMING] OrchestratorAgent.activate() START");

        log.info("Orchestrator Agent activated - using factory pattern for dynamic agent discovery");

        long elapsed = System.currentTimeMillis() - startTime;
        log.warning("[STARTUP TIMING] OrchestratorAgent.activate() COMPLETED in " + elapsed + "ms");
    }

    /**
     * Process a user query by routing to appropriate domain agent(s).
     *
     * <p>This is the main entry point for all AI queries. The orchestrator:
     * <ol>
     *   <li>Queries each domain agent if it can handle the request</li>
     *   <li>Routes to agent(s) that respond positively</li>
     *   <li>Aggregates responses if multiple domains</li>
     *   <li>Returns formatted response</li>
     * </ol>
     *
     * @param query natural language query from user
     * @param context window context map (from WindowContextExtractor), may be null
     * @return AI-generated response
     */
    @Override
    public String chat(String query, Map<String, Object> context) {
        if (query == null || query.trim().isEmpty()) {
            return "Please provide a query.";
        }

        log.info("Orchestrator received query: " + query);

        // Check if we have any agents available
        if (domainAgents.isEmpty()) {
            log.warning("No domain agents available");
            return "AI system not available. No domain agents are currently registered. " +
                   "Please ensure domain agent plugins are installed.";
        }

        log.info("Checking " + domainAgents.size() + " available domain agents");

        // Find agents that can handle this query
        List<IDomainAgent> capableAgents = new ArrayList<>();
        for (IDomainAgent agent : domainAgents) {
            try {
                if (agent.canHandle(query, context)) {
                    capableAgents.add(agent);
                    log.info("Agent " + agent.getDomain() + " can handle query");
                }
            } catch (Exception e) {
                log.warning("Error checking if agent " + agent.getDomain() +
                           " can handle query: " + e.getMessage());
            }
        }

        if (capableAgents.isEmpty()) {
            log.info("No agents can handle this query");
            return "I don't have specific information to answer that question. " +
                   "Please try rephrasing your query or ask about sales, inventory, " +
                   "purchasing, support, or knowledge base topics.";
        }

        if (capableAgents.size() == 1) {
            // Single agent - direct response
            IDomainAgent agent = capableAgents.get(0);
            log.info("Routing to single agent: " + agent.getDomain());
            return processWithAgent(agent, query, context);
        }

        // Multiple agents - aggregate responses
        log.info("Multi-domain query - " + capableAgents.size() + " agents can handle");
        return aggregateResponses(capableAgents, query, context);
    }

    /**
     * Process query with a specific agent.
     *
     * @param agent domain agent
     * @param query user query
     * @param context window context (may be null)
     * @return agent response
     */
    private String processWithAgent(IDomainAgent agent, String query, Map<String, Object> context) {
        try {
            return agent.process(query, context);
        } catch (Exception e) {
            log.severe("Error processing query with " + agent.getDomain() + " agent: " + e.getMessage());
            return "Error processing query: " + e.getMessage();
        }
    }

    /**
     * Aggregate responses from multiple domain agents.
     *
     * <p>Invokes each capable agent and combines their responses
     * into a coherent multi-domain answer.</p>
     *
     * @param agents list of capable agents
     * @param query user query
     * @param context window context (may be null)
     * @return aggregated response
     */
    private String aggregateResponses(List<IDomainAgent> agents, String query, Map<String, Object> context) {
        StringBuilder response = new StringBuilder();
        response.append("I found relevant information across multiple areas:\n\n");

        int successCount = 0;
        for (IDomainAgent agent : agents) {
            try {
                String agentResponse = agent.process(query, context);

                if (agentResponse != null && !agentResponse.trim().isEmpty()) {
                    successCount++;
                    response.append("## ").append(capitalize(agent.getDomain())).append("\n");
                    response.append(agentResponse).append("\n\n");
                }
            } catch (Exception e) {
                log.warning("Error getting response from " + agent.getDomain() + " agent: " + e.getMessage());
            }
        }

        if (successCount == 0) {
            return "Unable to process query. All domain agents encountered errors.";
        }

        return response.toString();
    }

    /**
     * Check if orchestrator has any agents available.
     *
     * @return true if at least one agent is available
     */
    @Override
    public boolean hasAgentsAvailable() {
        return !domainAgents.isEmpty();
    }

    /**
     * Get list of available agents.
     *
     * @return list of available agent domain names
     */
    @Override
    public List<String> getAvailableAgents() {
        List<String> available = new ArrayList<>();
        for (IDomainAgent agent : domainAgents) {
            available.add(capitalize(agent.getDomain()));
        }
        return available;
    }

    /**
     * Capitalize first letter of string.
     *
     * @param str input string
     * @return capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
