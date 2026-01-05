/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.routing;

import java.util.*;
import java.util.regex.Pattern;

import org.compiere.util.CLogger;

/**
 * Analyzes user prompts to determine optimal data source
 *
 * Decision Criteria:
 * - Temporal keywords (current, latest, now) → DATABASE
 * - Bulk operations (all, list, report) → DATABASE
 * - Transactional data (order, invoice, payment) → DATABASE
 * - Entity in recent context → CONTEXT
 * - Pronoun reference (it, that) → CONTEXT
 * - Mixed requirements → HYBRID
 *
 * @author Cloudempiere
 */
public class PromptAnalyzer {

    private static final CLogger log = CLogger.getCLogger(PromptAnalyzer.class);

    // Patterns for temporal requirements (need fresh data)
    private static final Pattern REAL_TIME_PATTERN = Pattern.compile(
        "\\b(current|latest|now|today|recent|updated|live|real-time|up-to-date)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Patterns for bulk data operations
    private static final Pattern BULK_DATA_PATTERN = Pattern.compile(
        "\\b(all|list|report|export|summary of all|every|complete list|show me all)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Patterns for transactional data (changes frequently)
    private static final Pattern TRANSACTIONAL_PATTERN = Pattern.compile(
        "\\b(order|invoice|shipment|payment|transaction|status|balance)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Patterns for pronoun references (use context)
    private static final Pattern PRONOUN_PATTERN = Pattern.compile(
        "\\b(it|that|this|its|their|his|her)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Patterns for context queries (asking about recently mentioned data)
    private static final Pattern CONTEXT_QUERY_PATTERN = Pattern.compile(
        "\\b(what was|what is|tell me about|show me|remind me).*\\b(the|that|this)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private final EntityExtractor entityExtractor;

    /**
     * Constructor
     * @param entityExtractor entity extraction utility
     */
    public PromptAnalyzer(EntityExtractor entityExtractor) {
        this.entityExtractor = entityExtractor;
    }

    /**
     * Main analysis method - determines optimal data source
     *
     * @param prompt user's prompt
     * @param context conversation context manager
     * @return routing decision
     */
    public SourceDecision analyzePrompt(String prompt, ConversationContextManager context) {
        if (prompt == null || prompt.trim().isEmpty()) {
            log.fine("Empty prompt - defaulting to DATABASE_ONLY");
            return SourceDecision.databaseOnly(
                new DbQueryParams(null, null, "Empty prompt"));
        }

        log.fine("Analyzing prompt: " + prompt);

        // 1. Check if data is already in context
        ContextMatch contextMatch = findInContext(prompt, context);

        // 2. Analyze temporal requirements
        boolean needsFreshData = requiresFreshData(prompt);

        // 3. Check data volume requirements
        boolean isLargeDataset = requiresLargeDataset(prompt);

        // 4. Check for transactional operations
        boolean isTransactional = isTransactionalQuery(prompt);

        // 5. Check for pronoun references
        boolean hasPronounReference = containsPronoun(prompt);

        // 6. Check for context-specific queries
        boolean isContextQuery = isContextQuery(prompt);

        // Log analysis results
        log.fine("Analysis: contextMatch=" + contextMatch +
                ", freshData=" + needsFreshData +
                ", largeDataset=" + isLargeDataset +
                ", transactional=" + isTransactional +
                ", pronoun=" + hasPronounReference +
                ", contextQuery=" + isContextQuery);

        // DECISION LOGIC

        // Rule 1: Pronoun reference with entity in context → CONTEXT_ONLY
        if (hasPronounReference && contextMatch.exists()) {
            log.fine("Decision: CONTEXT_ONLY (pronoun reference to existing context)");
            return SourceDecision.contextOnly(contextMatch);
        }

        // Rule 2: Context query with complete match → CONTEXT_ONLY
        if (isContextQuery && contextMatch.isComplete() && !needsFreshData) {
            log.fine("Decision: CONTEXT_ONLY (context query with complete match)");
            return SourceDecision.contextOnly(contextMatch);
        }

        // Rule 3: Complete context match, no freshness requirement → CONTEXT_ONLY
        if (contextMatch.isComplete() && !needsFreshData && !isTransactional) {
            log.fine("Decision: CONTEXT_ONLY (complete match, no freshness requirement)");
            return SourceDecision.contextOnly(contextMatch);
        }

        // Rule 4: Needs fresh data OR large dataset OR transactional → DATABASE_ONLY
        if (needsFreshData || isLargeDataset || (isTransactional && !contextMatch.exists())) {
            String reason = buildDatabaseReasoning(needsFreshData, isLargeDataset, isTransactional);
            log.fine("Decision: DATABASE_ONLY (" + reason + ")");
            return SourceDecision.databaseOnly(
                new DbQueryParams(null, buildParameters(prompt), reason));
        }

        // Rule 5: Partial context match with fresh data needed → HYBRID
        if (contextMatch.isPartial() && (needsFreshData || isTransactional)) {
            log.fine("Decision: HYBRID (partial context + fresh data needed)");
            return SourceDecision.hybrid(
                contextMatch,
                new DbQueryParams(null, buildParameters(prompt),
                    "Using context for reference, database for fresh data"));
        }

        // Default: DATABASE_ONLY (safe fallback)
        log.fine("Decision: DATABASE_ONLY (default fallback)");
        return SourceDecision.databaseOnly(
            new DbQueryParams(null, buildParameters(prompt), "No context match found"));
    }

    /**
     * Check if prompt requires real-time/fresh data
     */
    private boolean requiresFreshData(String prompt) {
        return REAL_TIME_PATTERN.matcher(prompt).find();
    }

    /**
     * Check if prompt requires large dataset
     */
    private boolean requiresLargeDataset(String prompt) {
        return BULK_DATA_PATTERN.matcher(prompt).find();
    }

    /**
     * Check if prompt is about transactional data
     */
    private boolean isTransactionalQuery(String prompt) {
        return TRANSACTIONAL_PATTERN.matcher(prompt).find();
    }

    /**
     * Check if prompt contains pronoun reference
     */
    private boolean containsPronoun(String prompt) {
        return PRONOUN_PATTERN.matcher(prompt).find();
    }

    /**
     * Check if prompt is asking about context
     */
    private boolean isContextQuery(String prompt) {
        return CONTEXT_QUERY_PATTERN.matcher(prompt).find();
    }

    /**
     * Find matching data in conversation context
     */
    private ContextMatch findInContext(String prompt, ConversationContextManager context) {
        // Extract entities mentioned in prompt
        Set<String> entities = entityExtractor.extractEntities(prompt);

        if (entities.isEmpty()) {
            log.fine("No entities extracted from prompt");
            return new ContextMatch(new HashMap<>(), false, false);
        }

        // Check if these entities are in recent context
        Map<String, Object> foundData = new HashMap<>();
        int totalEntities = entities.size();
        int foundEntities = 0;

        for (String entity : entities) {
            Object data = context.get(entity);
            if (data != null) {
                foundData.put(entity, data);
                foundEntities++;
                log.fine("Found in context: " + entity);
            } else {
                log.fine("Not in context: " + entity);
            }
        }

        boolean complete = totalEntities > 0 && foundEntities == totalEntities;
        boolean partial = foundEntities > 0 && foundEntities < totalEntities;

        return new ContextMatch(foundData, complete, partial);
    }

    /**
     * Build reasoning string for database query
     */
    private String buildDatabaseReasoning(boolean freshData, boolean largeDataset, boolean transactional) {
        List<String> reasons = new ArrayList<>();

        if (freshData) {
            reasons.add("Requires real-time data");
        }
        if (largeDataset) {
            reasons.add("Bulk data operation");
        }
        if (transactional) {
            reasons.add("Transactional query");
        }

        if (reasons.isEmpty()) {
            return "Data not available in context";
        }

        return String.join(", ", reasons);
    }

    /**
     * Build parameter map from prompt
     */
    private Map<String, Object> buildParameters(String prompt) {
        Map<String, Object> params = new HashMap<>();
        params.put("prompt", prompt);

        // Extract entity types for context
        Set<EntityExtractor.EntityType> types = entityExtractor.extractEntityTypes(prompt);
        if (!types.isEmpty()) {
            params.put("entity_types", types);
        }

        return params;
    }
}
