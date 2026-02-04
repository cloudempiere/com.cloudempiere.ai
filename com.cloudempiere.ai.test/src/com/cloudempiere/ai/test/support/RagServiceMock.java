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
package com.cloudempiere.ai.test.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.cloudempiere.ai.rag.IRagService;
import com.cloudempiere.ai.rag.dto.SearchResult;

import dev.langchain4j.rag.content.retriever.ContentRetriever;

/**
 * Mock implementation of IRagService for testing.
 *
 * Provides configurable behavior for testing RagTools and other
 * RAG-dependent components without requiring database or embedding model.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class RagServiceMock implements IRagService {

    private boolean available = true;
    private List<SearchResult> searchResults = new ArrayList<>();
    private String adEntityResult = "Entity not found";
    private String glossaryResult = "No glossary entry found";
    private Map<String, Object> stats = new HashMap<>();
    private List<String> sourceTypes = Arrays.asList("ad_metadata", "glossary", "knowledge_entry");

    // Tracking for verification
    private String lastSearchQuery;
    private String lastSearchFilter;
    private int lastSearchMaxResults;
    private String lastADEntityName;
    private String lastADEntityType;
    private String lastGlossaryTerm;
    private int searchCallCount = 0;
    private int adEntityCallCount = 0;
    private int glossaryCallCount = 0;

    /**
     * Create an available mock service with default responses.
     */
    public RagServiceMock() {
        initDefaultStats();
    }

    /**
     * Create a mock service with specified availability.
     *
     * @param available Whether service is available
     */
    public RagServiceMock(boolean available) {
        this.available = available;
        initDefaultStats();
    }

    private void initDefaultStats() {
        stats.put("available", available);
        stats.put("clientId", 1000000);
        stats.put("totalCount", 100L);
        Map<String, Long> countsBySource = new HashMap<>();
        countsBySource.put("ad_metadata", 80L);
        countsBySource.put("glossary", 20L);
        stats.put("countsBySource", countsBySource);
        stats.put("sourceTypes", sourceTypes);
    }

    @Override
    public List<SearchResult> search(Properties ctx, String query, String sourceFilter, int maxResults) {
        this.lastSearchQuery = query;
        this.lastSearchFilter = sourceFilter;
        this.lastSearchMaxResults = maxResults;
        this.searchCallCount++;
        return searchResults;
    }

    @Override
    public String findADEntity(Properties ctx, String entityName, String entityType) {
        this.lastADEntityName = entityName;
        this.lastADEntityType = entityType;
        this.adEntityCallCount++;
        return adEntityResult;
    }

    @Override
    public String lookupGlossary(Properties ctx, String term) {
        this.lastGlossaryTerm = term;
        this.glossaryCallCount++;
        return glossaryResult;
    }

    @Override
    public ContentRetriever getContentRetriever(Properties ctx) {
        // Return empty retriever for testing
        return query -> List.of();
    }

    @Override
    public void ingestAll(Properties ctx, boolean forceRefresh) {
        // No-op for testing
    }

    @Override
    public void ingestSource(Properties ctx, String sourceType, boolean forceRefresh) {
        // No-op for testing
    }

    @Override
    public Map<String, Object> getStats(Properties ctx) {
        return stats;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public List<String> getSourceTypes() {
        return sourceTypes;
    }

    // ========================================================================
    // Configuration Methods
    // ========================================================================

    /**
     * Set whether the service is available.
     */
    public RagServiceMock setAvailable(boolean available) {
        this.available = available;
        stats.put("available", available);
        return this;
    }

    /**
     * Set the search results to return.
     */
    public RagServiceMock setSearchResults(List<SearchResult> results) {
        this.searchResults = results;
        return this;
    }

    /**
     * Set a single search result to return.
     */
    public RagServiceMock setSearchResult(SearchResult result) {
        this.searchResults = Arrays.asList(result);
        return this;
    }

    /**
     * Clear search results (return empty list).
     */
    public RagServiceMock clearSearchResults() {
        this.searchResults = new ArrayList<>();
        return this;
    }

    /**
     * Set the AD entity result to return.
     */
    public RagServiceMock setADEntityResult(String result) {
        this.adEntityResult = result;
        return this;
    }

    /**
     * Set the glossary result to return.
     */
    public RagServiceMock setGlossaryResult(String result) {
        this.glossaryResult = result;
        return this;
    }

    /**
     * Set the stats to return.
     */
    public RagServiceMock setStats(Map<String, Object> stats) {
        this.stats = stats;
        return this;
    }

    /**
     * Set the source types.
     */
    public RagServiceMock setSourceTypes(List<String> sourceTypes) {
        this.sourceTypes = sourceTypes;
        stats.put("sourceTypes", sourceTypes);
        return this;
    }

    // ========================================================================
    // Verification Methods
    // ========================================================================

    /**
     * Get the last search query.
     */
    public String getLastSearchQuery() {
        return lastSearchQuery;
    }

    /**
     * Get the last search filter.
     */
    public String getLastSearchFilter() {
        return lastSearchFilter;
    }

    /**
     * Get the last search max results.
     */
    public int getLastSearchMaxResults() {
        return lastSearchMaxResults;
    }

    /**
     * Get the last AD entity name searched.
     */
    public String getLastADEntityName() {
        return lastADEntityName;
    }

    /**
     * Get the last AD entity type searched.
     */
    public String getLastADEntityType() {
        return lastADEntityType;
    }

    /**
     * Get the last glossary term looked up.
     */
    public String getLastGlossaryTerm() {
        return lastGlossaryTerm;
    }

    /**
     * Get search call count.
     */
    public int getSearchCallCount() {
        return searchCallCount;
    }

    /**
     * Get AD entity call count.
     */
    public int getAdEntityCallCount() {
        return adEntityCallCount;
    }

    /**
     * Get glossary call count.
     */
    public int getGlossaryCallCount() {
        return glossaryCallCount;
    }

    /**
     * Reset all counters and tracking.
     */
    public RagServiceMock reset() {
        this.lastSearchQuery = null;
        this.lastSearchFilter = null;
        this.lastSearchMaxResults = 0;
        this.lastADEntityName = null;
        this.lastADEntityType = null;
        this.lastGlossaryTerm = null;
        this.searchCallCount = 0;
        this.adEntityCallCount = 0;
        this.glossaryCallCount = 0;
        return this;
    }

    // ========================================================================
    // Factory Methods for Common Scenarios
    // ========================================================================

    /**
     * Create a mock with window search result.
     */
    public static RagServiceMock withWindowResult(String windowName, String description) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_type", "ad_metadata");
        metadata.put("entity_type", "window");

        SearchResult result = SearchResult.builder()
            .id("mock-window-1")
            .title(windowName)
            .content("Window: " + windowName + "\nDescription: " + description)
            .sourceType("ad_metadata")
            .sourceId("100")
            .score(0.92)
            .metadata(metadata)
            .build();

        return new RagServiceMock().setSearchResult(result);
    }

    /**
     * Create a mock with process search result.
     */
    public static RagServiceMock withProcessResult(String processName, String description) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_type", "ad_metadata");
        metadata.put("entity_type", "process");

        SearchResult result = SearchResult.builder()
            .id("mock-process-1")
            .title(processName)
            .content("Process: " + processName + "\nDescription: " + description)
            .sourceType("ad_metadata")
            .sourceId("200")
            .score(0.88)
            .metadata(metadata)
            .build();

        return new RagServiceMock().setSearchResult(result);
    }

    /**
     * Create a mock with glossary result.
     */
    public static RagServiceMock withGlossaryResult(String term, String definition) {
        SearchResult result = SearchResult.builder()
            .id("mock-glossary-1")
            .title(term)
            .content(definition)
            .sourceType("glossary")
            .score(0.95)
            .build();

        RagServiceMock mock = new RagServiceMock();
        mock.setSearchResult(result);
        mock.setGlossaryResult("**Glossary: " + term + "**\n\n" + definition);
        return mock;
    }

    /**
     * Create an unavailable mock service.
     */
    public static RagServiceMock unavailable() {
        return new RagServiceMock(false);
    }
}
