package com.cloudempiere.ai.orchestrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.cloudempiere.ai.sales.agent.SalesAgent;
import com.cloudempiere.ai.inventory.agent.InventoryAgent;
import com.cloudempiere.ai.purchasing.agent.PurchasingAgent;
import com.cloudempiere.ai.support.agent.SupportAgent;
import com.cloudempiere.ai.kb.agent.KbAgent;

/**
 * Central orchestrator agent that routes queries to specialized domain agents.
 *
 * <p>This orchestrator implements the agent coordination strategy defined in ADR-010.
 * It analyzes incoming queries and routes them to the appropriate domain specialist(s):</p>
 * <ul>
 *   <li>Sales queries → SalesAgent</li>
 *   <li>Inventory queries → InventoryAgent</li>
 *   <li>Purchasing queries → PurchasingAgent</li>
 *   <li>Support queries → SupportAgent</li>
 *   <li>Knowledge base queries → KbAgent</li>
 *   <li>Multi-domain queries → Multiple agents with response aggregation</li>
 * </ul>
 *
 * <p><b>Routing Strategy:</b></p>
 * <ol>
 *   <li>Keyword-based classification for clear, single-domain queries</li>
 *   <li>Multi-domain detection for queries spanning multiple areas</li>
 *   <li>Fallback to knowledge base for general questions</li>
 * </ol>
 *
 * <p><b>Related ADRs:</b></p>
 * <ul>
 *   <li><a href="../../docs/adr/010-agent-orchestration-architecture.md">ADR-010: Agent Orchestration</a></li>
 *   <li><a href="../../docs/adr/009-domain-boundaries-agent-scope.md">ADR-009: Domain Boundaries</a></li>
 * </ul>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
@Component(service = OrchestratorAgent.class, immediate = true)
public class OrchestratorAgent {

    private static final Logger log = Logger.getLogger(OrchestratorAgent.class.getName());

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private volatile SalesAgent salesAgent;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private volatile InventoryAgent inventoryAgent;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private volatile PurchasingAgent purchasingAgent;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private volatile SupportAgent supportAgent;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private volatile KbAgent kbAgent;

    /** Domain classification keywords */
    private static final Map<String, String[]> DOMAIN_KEYWORDS = new HashMap<>();

    static {
        DOMAIN_KEYWORDS.put("sales", new String[]{
            "sales", "opportunity", "quote", "order", "customer", "revenue",
            "deal", "pipeline", "forecast", "crm", "lead", "prospect"
        });

        DOMAIN_KEYWORDS.put("inventory", new String[]{
            "inventory", "stock", "warehouse", "product", "sku", "storage",
            "availability", "on hand", "reserved", "shortage"
        });

        DOMAIN_KEYWORDS.put("purchasing", new String[]{
            "purchase", "vendor", "supplier", "procurement", "buying",
            "purchase order", "po", "requisition", "sourcing"
        });

        DOMAIN_KEYWORDS.put("support", new String[]{
            "support", "ticket", "request", "issue", "problem", "bug",
            "help", "escalation", "complaint", "service"
        });

        DOMAIN_KEYWORDS.put("kb", new String[]{
            "documentation", "article", "guide", "how to", "tutorial",
            "manual", "knowledge", "wiki", "faq"
        });
    }

    @Activate
    protected void activate() {
        log.info("Orchestrator Agent activated - ready to route queries");
    }

    /**
     * Process a user query by routing to appropriate domain agent(s).
     *
     * <p>This is the main entry point for all AI queries. The orchestrator:
     * <ol>
     *   <li>Classifies the query domain(s)</li>
     *   <li>Routes to specialist agent(s)</li>
     *   <li>Aggregates responses if multiple domains</li>
     *   <li>Returns formatted response</li>
     * </ol>
     *
     * @param query natural language query from user
     * @return AI-generated response
     */
    public String chat(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "Please provide a query.";
        }

        log.info("Orchestrator received query: " + query);

        // Classify which domain(s) this query belongs to
        List<String> domains = classifyQuery(query);

        if (domains.isEmpty()) {
            // No clear domain match - fallback to knowledge base
            log.info("No clear domain match - routing to knowledge base");
            return routeToKnowledgeBase(query);
        }

        if (domains.size() == 1) {
            // Single domain - direct routing
            String domain = domains.get(0);
            log.info("Routing to single domain: " + domain);
            return routeToDomain(domain, query);
        }

        // Multiple domains - invoke all and aggregate responses
        log.info("Multi-domain query detected: " + domains);
        return aggregateResponses(domains, query);
    }

    /**
     * Classify query into domain(s) based on keyword matching.
     *
     * @param query user query
     * @return list of matching domains (empty if no match)
     */
    private List<String> classifyQuery(String query) {
        List<String> matchedDomains = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Map.Entry<String, String[]> entry : DOMAIN_KEYWORDS.entrySet()) {
            String domain = entry.getKey();
            String[] keywords = entry.getValue();

            for (String keyword : keywords) {
                if (lowerQuery.contains(keyword)) {
                    matchedDomains.add(domain);
                    break; // Only add each domain once
                }
            }
        }

        return matchedDomains;
    }

    /**
     * Route query to specific domain agent.
     *
     * @param domain target domain (sales, inventory, purchasing, support, kb)
     * @param query user query
     * @return agent response
     */
    private String routeToDomain(String domain, String query) {
        try {
            switch (domain) {
                case "sales":
                    if (salesAgent == null) {
                        return "Sales agent not available. Please check plugin installation.";
                    }
                    return salesAgent.chat(query);

                case "inventory":
                    if (inventoryAgent == null) {
                        return "Inventory agent not available. Please check plugin installation.";
                    }
                    return inventoryAgent.chat(query);

                case "purchasing":
                    if (purchasingAgent == null) {
                        return "Purchasing agent not available. Please check plugin installation.";
                    }
                    return purchasingAgent.chat(query);

                case "support":
                    if (supportAgent == null) {
                        return "Support agent not available. Please check plugin installation.";
                    }
                    return supportAgent.chat(query);

                case "kb":
                    return routeToKnowledgeBase(query);

                default:
                    return "Unknown domain: " + domain;
            }
        } catch (Exception e) {
            log.severe("Error routing to " + domain + " agent: " + e.getMessage());
            return "Error processing query: " + e.getMessage();
        }
    }

    /**
     * Route to knowledge base agent.
     *
     * @param query user query
     * @return KB agent response
     */
    private String routeToKnowledgeBase(String query) {
        if (kbAgent == null) {
            return "I don't have enough information to answer that. Knowledge base agent not available.";
        }
        return kbAgent.chat(query);
    }

    /**
     * Aggregate responses from multiple domain agents.
     *
     * <p>Invokes each relevant domain agent and combines their responses
     * into a coherent multi-domain answer.</p>
     *
     * @param domains list of domains to query
     * @param query user query
     * @return aggregated response
     */
    private String aggregateResponses(List<String> domains, String query) {
        StringBuilder response = new StringBuilder();
        response.append("I found relevant information across multiple areas:\n\n");

        int domainCount = 0;
        for (String domain : domains) {
            String domainResponse = routeToDomain(domain, query);

            if (domainResponse != null && !domainResponse.isEmpty()
                && !domainResponse.contains("not available")) {

                domainCount++;
                response.append("## ").append(getDomainDisplayName(domain)).append("\n");
                response.append(domainResponse).append("\n\n");
            }
        }

        if (domainCount == 0) {
            return "Unable to process query. No domain agents available.";
        }

        return response.toString();
    }

    /**
     * Get display name for domain.
     *
     * @param domain internal domain code
     * @return human-readable domain name
     */
    private String getDomainDisplayName(String domain) {
        switch (domain) {
            case "sales": return "Sales";
            case "inventory": return "Inventory";
            case "purchasing": return "Purchasing";
            case "support": return "Support";
            case "kb": return "Knowledge Base";
            default: return domain;
        }
    }

    /**
     * Check if orchestrator has any agents available.
     *
     * @return true if at least one agent is available
     */
    public boolean hasAgentsAvailable() {
        return salesAgent != null || inventoryAgent != null
            || purchasingAgent != null || supportAgent != null
            || kbAgent != null;
    }

    /**
     * Get list of available agents.
     *
     * @return list of available agent names
     */
    public List<String> getAvailableAgents() {
        List<String> available = new ArrayList<>();
        if (salesAgent != null) available.add("Sales");
        if (inventoryAgent != null) available.add("Inventory");
        if (purchasingAgent != null) available.add("Purchasing");
        if (supportAgent != null) available.add("Support");
        if (kbAgent != null) available.add("Knowledge Base");
        return available;
    }
}
