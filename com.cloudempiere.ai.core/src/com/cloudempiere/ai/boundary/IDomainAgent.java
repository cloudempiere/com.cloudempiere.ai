package com.cloudempiere.ai.boundary;

import java.util.Map;

/**
 * Interface for domain-specific AI agents.
 *
 * <p>Domain agents are specialists that handle queries related to specific
 * business areas (sales, inventory, purchasing, support, knowledge base).
 * They are discovered dynamically by the orchestrator using OSGi service tracking.</p>
 *
 * <p><b>Implementation Pattern:</b></p>
 * <pre>
 * @Component(service = IDomainAgent.class, property = {"domain=sales"})
 * public class SalesAgent implements IDomainAgent {
 *     public String getDomain() { return "sales"; }
 *     public boolean canHandle(String query, Map context) { ... }
 *     public String process(String query, Map context) { ... }
 * }
 * </pre>
 *
 * <p><b>Related ADRs:</b></p>
 * <ul>
 *   <li><a href="../../docs/adr/009-domain-boundaries-agent-scope.md">ADR-009: Domain Boundaries</a></li>
 *   <li><a href="../../docs/adr/011-specialized-agent-scopes.md">ADR-011: Specialized Agent Scopes</a></li>
 * </ul>
 *
 * @author Cloudempiere AI Team
 * @version 0.32.0
 */
public interface IDomainAgent {

    /**
     * Get the domain identifier for this agent.
     *
     * <p>Domain identifiers are used for logging, debugging, and explicit routing.
     * Common values: "sales", "inventory", "purchasing", "support", "kb"</p>
     *
     * @return domain identifier (lowercase, no spaces)
     */
    String getDomain();

    /**
     * Determine if this agent can handle the given query.
     *
     * <p>Agents should analyze the query text and context to determine relevance.
     * Multiple agents may return true for the same query (multi-domain scenarios).</p>
     *
     * @param query user query text
     * @param context optional window/record context (may be null)
     * @return true if this agent can handle the query
     */
    boolean canHandle(String query, Map<String, Object> context);

    /**
     * Process the query and return a response.
     *
     * <p>This method is only called if canHandle() returned true.
     * Implementations should use the provided context for record-aware responses.</p>
     *
     * @param query user query text
     * @param context optional window/record context (may be null)
     * @return AI response text
     */
    String process(String query, Map<String, Object> context);
}
