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
package com.cloudempiere.ai.provider.langchain4j.tools;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.rag.IRagService;
import com.cloudempiere.ai.rag.dto.SearchResult;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * RAG tools for AI agent knowledge search.
 *
 * <p>These @Tool methods allow the AI agent to search the knowledge base
 * for relevant information about iDempiere windows, processes, tables,
 * and business concepts.
 *
 * <p>Usage with AiServices:
 * <pre>
 * RagTools ragTools = new RagTools(ragService, ctx);
 *
 * ERPAgent agent = AiServices.builder(ERPAgent.class)
 *     .chatLanguageModel(model)
 *     .tools(erpTools, ragTools)  // Include both tool classes
 *     .build();
 * </pre>
 *
 * <p>Reference: idempiere-cli RagTools
 *
 * @author Cloudempiere AI Team
 * @version ADR-012
 * @since v0.11.0
 */
public class RagTools {

    private static final CLogger log = CLogger.getCLogger(RagTools.class);

    private final IRagService ragService;
    private final Properties ctx;

    /**
     * Create RagTools with RAG service and context.
     *
     * @param ragService RAG service for knowledge retrieval
     * @param ctx iDempiere context
     */
    public RagTools(IRagService ragService, Properties ctx) {
        this.ragService = ragService;
        this.ctx = ctx;
    }

    /**
     * Search the knowledge base for relevant information.
     *
     * <p>Use this tool to find information about:
     * <ul>
     *   <li>iDempiere windows and how to use them</li>
     *   <li>Business processes and reports</li>
     *   <li>Database tables and their purpose</li>
     *   <li>Business concepts and terminology</li>
     * </ul>
     *
     * @param query Natural language search query
     * @param sourceFilter Optional filter: "ad_metadata", "knowledge_entry", "glossary", or empty for all
     * @return Relevant knowledge snippets with source references
     */
    @Tool("Search the knowledge base for information about iDempiere windows, processes, tables, and business concepts. " +
          "Use this when you need to find how to do something in the system or understand a business concept.")
    public String searchKnowledge(
            @P("The search query in natural language, e.g., 'how to create a sales order'") String query,
            @P("Optional source filter: 'ad_metadata' for windows/processes, 'knowledge_entry' for articles, 'glossary' for terms, or leave empty for all") String sourceFilter) {

        log.fine("searchKnowledge called: query='" + query + "', filter=" + sourceFilter);

        if (ragService == null || !ragService.isAvailable()) {
            return "Knowledge base is not available. Please try again later.";
        }

        try {
            // Normalize empty filter to null
            if (sourceFilter != null && sourceFilter.trim().isEmpty()) {
                sourceFilter = null;
            }

            List<SearchResult> results = ragService.search(ctx, query, sourceFilter, 5);

            if (results.isEmpty()) {
                return "No relevant knowledge found for: " + query + "\n" +
                       "Try rephrasing your query or using different keywords.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(results.size()).append(" relevant item(s):\n\n");

            for (SearchResult result : results) {
                sb.append(result.toFormattedString()).append("\n\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.log(Level.WARNING, "searchKnowledge failed", e);
            return "Error searching knowledge base: " + e.getMessage();
        }
    }

    /**
     * Find Application Dictionary entity by name.
     *
     * <p>Use this tool to find specific iDempiere windows, processes, tables, or fields.
     *
     * @param entityName Name of the entity to find
     * @param entityType Type: "window", "process", "table", "field"
     * @return Entity details with related information
     */
    @Tool("Find a specific iDempiere Application Dictionary entity like a window, process, table, or field. " +
          "Use this when the user asks about a specific named entity.")
    public String findADEntity(
            @P("Name of the entity to find, e.g., 'Sales Order', 'Generate Invoices'") String entityName,
            @P("Type of entity: 'window', 'process', 'table', 'field'") String entityType) {

        log.fine("findADEntity called: name='" + entityName + "', type=" + entityType);

        if (ragService == null || !ragService.isAvailable()) {
            return "Knowledge base is not available. Please try again later.";
        }

        try {
            return ragService.findADEntity(ctx, entityName, entityType);
        } catch (Exception e) {
            log.log(Level.WARNING, "findADEntity failed", e);
            return "Error finding entity: " + e.getMessage();
        }
    }

    /**
     * Look up glossary term or naming convention.
     *
     * <p>Use this tool to understand business terminology, naming conventions,
     * or CloudEmpiere-specific concepts.
     *
     * @param term Term or concept to look up
     * @return Definition, usage examples, and related terms
     */
    @Tool("Look up a glossary term, naming convention, or business concept definition. " +
          "Use this when you need to understand what a term means in the business context.")
    public String lookupGlossary(
            @P("Term or concept to look up, e.g., 'BPartner', 'DocAction'") String term) {

        log.fine("lookupGlossary called: term='" + term + "'");

        if (ragService == null || !ragService.isAvailable()) {
            return "Knowledge base is not available. Please try again later.";
        }

        try {
            return ragService.lookupGlossary(ctx, term);
        } catch (Exception e) {
            log.log(Level.WARNING, "lookupGlossary failed", e);
            return "Error looking up term: " + e.getMessage();
        }
    }

    /**
     * Get knowledge base statistics.
     *
     * <p>Use this tool to check what knowledge is available and when it was last updated.
     *
     * @return Summary of indexed knowledge by source type
     */
    @Tool("Get statistics about the knowledge base content - how many documents are indexed and when they were last updated")
    public String getKnowledgeStats() {
        log.fine("getKnowledgeStats called");

        if (ragService == null) {
            return "Knowledge base is not configured.";
        }

        try {
            Map<String, Object> stats = ragService.getStats(ctx);

            StringBuilder sb = new StringBuilder();
            sb.append("**Knowledge Base Statistics**\n\n");

            boolean available = (Boolean) stats.getOrDefault("available", false);
            sb.append("Status: ").append(available ? "Available" : "Not Available").append("\n");

            if (available) {
                @SuppressWarnings("unchecked")
                Map<String, Long> countsBySource = (Map<String, Long>) stats.get("countsBySource");
                if (countsBySource != null && !countsBySource.isEmpty()) {
                    sb.append("\nDocuments by Source:\n");
                    for (Map.Entry<String, Long> entry : countsBySource.entrySet()) {
                        sb.append("- ").append(entry.getKey()).append(": ")
                          .append(entry.getValue()).append("\n");
                    }
                }

                Long totalCount = (Long) stats.get("totalCount");
                if (totalCount != null) {
                    sb.append("\nTotal Documents: ").append(totalCount).append("\n");
                }

                @SuppressWarnings("unchecked")
                List<String> sourceTypes = (List<String>) stats.get("sourceTypes");
                if (sourceTypes != null && !sourceTypes.isEmpty()) {
                    sb.append("\nRegistered Ingestors: ").append(String.join(", ", sourceTypes));
                }
            }

            return sb.toString();

        } catch (Exception e) {
            log.log(Level.WARNING, "getKnowledgeStats failed", e);
            return "Error getting statistics: " + e.getMessage();
        }
    }
}
