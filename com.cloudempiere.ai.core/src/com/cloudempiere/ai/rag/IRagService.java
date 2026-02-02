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
package com.cloudempiere.ai.rag;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.cloudempiere.ai.rag.dto.SearchResult;

import dev.langchain4j.rag.content.retriever.ContentRetriever;

/**
 * Core RAG service interface for knowledge retrieval.
 *
 * <p>Combines embedding store, embedding model, and metadata filtering
 * to provide semantic search capabilities for AI agents.
 *
 * <p>Key operations:
 * <ul>
 *   <li>Semantic search with source filtering</li>
 *   <li>Knowledge ingestion from multiple sources</li>
 *   <li>ContentRetriever factory for agent integration</li>
 * </ul>
 *
 * <p>Reference: idempiere-cli RagService
 *
 * @author Cloudempiere AI Team
 * @version ADR-012
 * @since v0.11.0
 */
public interface IRagService {

    /**
     * Search the knowledge base using semantic similarity.
     *
     * @param ctx iDempiere context (for AD_Client_ID)
     * @param query Natural language search query
     * @param sourceFilter Filter by source type (null for all)
     * @param maxResults Maximum results to return
     * @return List of search results ordered by relevance
     */
    List<SearchResult> search(Properties ctx, String query, String sourceFilter, int maxResults);

    /**
     * Search with default max results (5).
     *
     * @param ctx iDempiere context
     * @param query Natural language search query
     * @param sourceFilter Filter by source type
     * @return List of search results
     */
    default List<SearchResult> search(Properties ctx, String query, String sourceFilter) {
        return search(ctx, query, sourceFilter, 5);
    }

    /**
     * Find Application Dictionary entity by name.
     *
     * @param ctx iDempiere context
     * @param entityName Name of window, process, table, or field
     * @param entityType Type: "window", "process", "table", "field"
     * @return Entity details as formatted string
     */
    String findADEntity(Properties ctx, String entityName, String entityType);

    /**
     * Look up glossary term or naming convention.
     *
     * @param ctx iDempiere context
     * @param term Term to look up
     * @return Definition and examples, or "not found" message
     */
    String lookupGlossary(Properties ctx, String term);

    /**
     * Get ContentRetriever for agent integration.
     *
     * <p>The returned retriever can be used with AiServices.builder()
     * to automatically inject relevant context into conversations.
     *
     * @param ctx iDempiere context
     * @return Configured ContentRetriever
     */
    ContentRetriever getContentRetriever(Properties ctx);

    /**
     * Ingest knowledge from all registered sources.
     *
     * @param ctx iDempiere context
     * @param forceRefresh If true, re-ingest even if already up-to-date
     */
    void ingestAll(Properties ctx, boolean forceRefresh);

    /**
     * Ingest knowledge from a specific source.
     *
     * @param ctx iDempiere context
     * @param sourceType Source type identifier
     * @param forceRefresh If true, re-ingest even if already up-to-date
     */
    void ingestSource(Properties ctx, String sourceType, boolean forceRefresh);

    /**
     * Get knowledge base statistics.
     *
     * @param ctx iDempiere context
     * @return Statistics map with counts by source type
     */
    Map<String, Object> getStats(Properties ctx);

    /**
     * Check if RAG service is available (embeddings configured).
     *
     * @return true if semantic search is available
     */
    boolean isAvailable();

    /**
     * Get registered source types.
     *
     * @return List of available source type identifiers
     */
    List<String> getSourceTypes();
}
