package com.cloudempiere.ai.boundary;

import java.util.List;
import java.util.Map;

/**
 * Interface for AI Orchestrator that routes queries to domain agents.
 *
 * <p>This interface defines the contract for the orchestrator component
 * that intelligently routes user queries to appropriate domain-specific agents.</p>
 *
 * @author CloudEmpiere
 * @version 0.32.0
 */
public interface IOrchestrator {

    /**
     * Process a chat query and route to appropriate agents.
     *
     * @param query user query
     * @param context optional window/record context
     * @return AI response
     */
    String chat(String query, Map<String, Object> context);

    /**
     * Check if any agents are available.
     *
     * @return true if at least one agent is available
     */
    boolean hasAgentsAvailable();

    /**
     * Get list of available agent names.
     *
     * @return list of agent identifiers
     */
    List<String> getAvailableAgents();
}
