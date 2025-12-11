/******************************************************************************
 * Product: Cloudempiere ERP & CRM Smart Business Solution                   *
 * Copyright (C) 2025 Cloudempiere, Inc. All Rights Reserved.                 *
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
package com.cloudempiere.ai.process;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import com.cloudempiere.ai.rag.IRagService;
import com.cloudempiere.ai.rag.dto.SearchResult;
import com.cloudempiere.ai.rag.embedding.IEmbeddingStoreProvider;

/**
 * Test Process for P1 Context Layer (RAG).
 *
 * <p>This process tests the RAG implementation by:
 * <ol>
 *   <li>Checking pgvector availability</li>
 *   <li>Getting embedding store statistics</li>
 *   <li>Running ingestion (optional)</li>
 *   <li>Performing semantic search</li>
 *   <li>Testing RagTools functionality</li>
 * </ol>
 *
 * <p>Run this process from iDempiere to verify the Context Layer is working.
 *
 * @author Cloudempiere AI Team
 * @version ADR-012, ADR-026
 * @since v0.12.0
 */
@org.adempiere.base.annotation.Process
public class TestRAGContextLayer extends SvrProcess {

    /** Search query parameter */
    private String p_SearchQuery = "sales order";

    /** Source type filter (optional) */
    private String p_SourceFilter = null;

    /** Run ingestion */
    private boolean p_RunIngestion = false;

    /** Force refresh during ingestion */
    private boolean p_ForceRefresh = false;

    /** Max search results */
    private int p_MaxResults = 5;

    /** RAG Service (OSGi injected or looked up) */
    private IRagService ragService;

    /** Embedding Store Provider */
    private IEmbeddingStoreProvider storeProvider;

    @Override
    protected void prepare() {
        ProcessInfoParameter[] para = getParameter();
        for (int i = 0; i < para.length; i++) {
            String name = para[i].getParameterName();
            if (para[i].getParameter() == null) {
                continue;
            }
            if (name.equals("SearchQuery")) {
                p_SearchQuery = para[i].getParameterAsString();
            } else if (name.equals("SourceFilter")) {
                p_SourceFilter = para[i].getParameterAsString();
            } else if (name.equals("RunIngestion")) {
                p_RunIngestion = "Y".equals(para[i].getParameterAsString());
            } else if (name.equals("ForceRefresh")) {
                p_ForceRefresh = "Y".equals(para[i].getParameterAsString());
            } else if (name.equals("MaxResults")) {
                p_MaxResults = para[i].getParameterAsInt();
            } else {
                log.log(Level.FINE, "Unknown Parameter: " + name);
            }
        }
    }

    @Override
    protected String doIt() throws Exception {
        addLog("╔═══════════════════════════════════════════════════════╗");
        addLog("║  P1 Context Layer (RAG) Test                          ║");
        addLog("║  ADR-012: RAG-Based Context Retrieval                 ║");
        addLog("║  ADR-026: Vector Database (pgvector)                  ║");
        addLog("╚═══════════════════════════════════════════════════════╝");

        // Get services via OSGi
        lookupServices();

        int testsRun = 0;
        int testsPassed = 0;

        // Test 1: pgvector Availability
        testsRun++;
        addLog("");
        addLog("=== Test 1: pgvector Availability ===");
        if (testPgVectorAvailability()) {
            testsPassed++;
        }

        // Test 2: Embedding Store Statistics
        testsRun++;
        addLog("");
        addLog("=== Test 2: Embedding Store Statistics ===");
        if (testEmbeddingStoreStats()) {
            testsPassed++;
        }

        // Test 3: Knowledge Ingestion (optional)
        if (p_RunIngestion) {
            testsRun++;
            addLog("");
            addLog("=== Test 3: Knowledge Ingestion ===");
            if (testKnowledgeIngestion()) {
                testsPassed++;
            }
        }

        // Test 4: Semantic Search
        testsRun++;
        addLog("");
        addLog("=== Test 4: Semantic Search ===");
        if (testSemanticSearch()) {
            testsPassed++;
        }

        // Test 5: Source Type Filtering
        testsRun++;
        addLog("");
        addLog("=== Test 5: Source Type Filtering ===");
        if (testSourceTypeFiltering()) {
            testsPassed++;
        }

        // Test 6: Multi-tenant Filtering
        testsRun++;
        addLog("");
        addLog("=== Test 6: Multi-tenant Filtering ===");
        if (testMultiTenantFiltering()) {
            testsPassed++;
        }

        // Summary
        addLog("");
        addLog("═══════════════════════════════════════════════════════");
        addLog("Test Summary: " + testsPassed + "/" + testsRun + " PASSED");

        if (testsPassed == testsRun) {
            addLog("✓ All P1 Context Layer tests PASSED!");
            return "@OK@ All " + testsRun + " tests passed";
        } else {
            addLog("⚠ " + (testsRun - testsPassed) + " test(s) FAILED");
            return "@Error@ " + (testsRun - testsPassed) + " of " + testsRun + " tests failed";
        }
    }

    /**
     * Lookup OSGi services.
     */
    private void lookupServices() {
        // Try to get services from OSGi
        try {
            org.osgi.framework.BundleContext bc = org.osgi.framework.FrameworkUtil
                .getBundle(getClass()).getBundleContext();

            // Get IEmbeddingStoreProvider
            org.osgi.framework.ServiceReference<?> storeRef = bc
                .getServiceReference(IEmbeddingStoreProvider.class.getName());
            if (storeRef != null) {
                storeProvider = (IEmbeddingStoreProvider) bc.getService(storeRef);
            }

            // Get IRagService
            org.osgi.framework.ServiceReference<?> ragRef = bc
                .getServiceReference(IRagService.class.getName());
            if (ragRef != null) {
                ragService = (IRagService) bc.getService(ragRef);
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to lookup OSGi services", e);
        }

        if (storeProvider == null) {
            throw new AdempiereException("EmbeddingStoreProvider not available - check OSGi bundle");
        }
        if (ragService == null) {
            throw new AdempiereException("RagService not available - check OSGi bundle");
        }

        addLog("Services loaded: EmbeddingStoreProvider, RagService");
    }

    /**
     * Test 1: pgvector availability.
     */
    private boolean testPgVectorAvailability() {
        try {
            boolean available = storeProvider.isAvailable();
            boolean fallback = storeProvider.isFallbackMode();

            addLog("pgvector Available: " + (available ? "YES ✓" : "NO ✗"));
            addLog("Fallback Mode: " + (fallback ? "YES (using InMemory)" : "NO (using pgvector)"));
            addLog("Table Name: " + storeProvider.getTableName());
            addLog("Dimension: " + storeProvider.getDimension());

            if (!available) {
                addLog("⚠ pgvector not available - run migration and enable extension");
                addLog("  SQL: CREATE EXTENSION IF NOT EXISTS vector;");
                return false;
            }

            addLog("Test Result: PASSED ✓");
            return true;

        } catch (Exception e) {
            addLog("Error: " + e.getMessage());
            addLog("Test Result: FAILED ✗");
            return false;
        }
    }

    /**
     * Test 2: Embedding store statistics.
     */
    private boolean testEmbeddingStoreStats() {
        try {
            int clientId = Env.getAD_Client_ID(getCtx());
            Map<String, Object> stats = storeProvider.getStatistics(clientId);

            addLog("Client ID: " + clientId);
            addLog("Total Embeddings: " + stats.getOrDefault("totalCount", 0));

            @SuppressWarnings("unchecked")
            Map<String, Long> countsBySource = (Map<String, Long>) stats.get("countsBySource");
            if (countsBySource != null && !countsBySource.isEmpty()) {
                addLog("Embeddings by Source:");
                for (Map.Entry<String, Long> entry : countsBySource.entrySet()) {
                    addLog("  - " + entry.getKey() + ": " + entry.getValue());
                }
            } else {
                addLog("No embeddings found (run ingestion first)");
            }

            addLog("Test Result: PASSED ✓");
            return true;

        } catch (Exception e) {
            addLog("Error: " + e.getMessage());
            addLog("Test Result: FAILED ✗");
            return false;
        }
    }

    /**
     * Test 3: Knowledge ingestion.
     */
    private boolean testKnowledgeIngestion() {
        try {
            addLog("Force Refresh: " + (p_ForceRefresh ? "YES" : "NO"));
            addLog("Starting ingestion...");

            long startTime = System.currentTimeMillis();
            ragService.ingestAll(getCtx(), p_ForceRefresh);
            long duration = System.currentTimeMillis() - startTime;

            addLog("Ingestion completed in " + duration + "ms");

            // Get updated stats
            int clientId = Env.getAD_Client_ID(getCtx());
            Map<String, Object> stats = storeProvider.getStatistics(clientId);
            addLog("Total Embeddings After: " + stats.getOrDefault("totalCount", 0));

            addLog("Test Result: PASSED ✓");
            return true;

        } catch (Exception e) {
            addLog("Error: " + e.getMessage());
            log.log(Level.SEVERE, "Ingestion failed", e);
            addLog("Test Result: FAILED ✗");
            return false;
        }
    }

    /**
     * Test 4: Semantic search.
     */
    private boolean testSemanticSearch() {
        try {
            addLog("Search Query: \"" + p_SearchQuery + "\"");
            addLog("Max Results: " + p_MaxResults);

            if (!ragService.isAvailable()) {
                addLog("RAG service not available");
                addLog("Test Result: FAILED ✗");
                return false;
            }

            long startTime = System.currentTimeMillis();
            List<SearchResult> results = ragService.search(getCtx(), p_SearchQuery, null, p_MaxResults);
            long duration = System.currentTimeMillis() - startTime;

            addLog("Search completed in " + duration + "ms");
            addLog("Results found: " + results.size());

            if (results.isEmpty()) {
                addLog("⚠ No results found - embedding store may be empty");
                addLog("  Run ingestion first: p_RunIngestion=Y");
                addLog("Test Result: PASSED ✓ (search works, but no data)");
                return true;
            }

            addLog("");
            addLog("Search Results:");
            for (int i = 0; i < results.size(); i++) {
                SearchResult result = results.get(i);
                addLog("--- Result " + (i + 1) + " ---");
                addLog("Title: " + result.getTitle());
                addLog("Source: " + result.getSourceType() + " (" + result.getSourceId() + ")");
                addLog("Score: " + String.format("%.3f", result.getScore()));
                addLog("Content: " + truncate(result.getContent(), 150));
            }

            addLog("");
            addLog("Test Result: PASSED ✓");
            return true;

        } catch (Exception e) {
            addLog("Error: " + e.getMessage());
            log.log(Level.SEVERE, "Search failed", e);
            addLog("Test Result: FAILED ✗");
            return false;
        }
    }

    /**
     * Test 5: Source type filtering.
     */
    private boolean testSourceTypeFiltering() {
        try {
            String[] sourceTypes = {"ad_metadata", "knowledge_entry"};

            for (String sourceType : sourceTypes) {
                addLog("Testing filter: " + sourceType);

                List<SearchResult> results = ragService.search(getCtx(), p_SearchQuery, sourceType, 3);
                addLog("  Results: " + results.size());

                // Verify all results match the filter
                boolean allMatch = results.stream()
                    .allMatch(r -> sourceType.equals(r.getSourceType()));

                if (!results.isEmpty() && !allMatch) {
                    addLog("  ⚠ Filter not applied correctly!");
                    addLog("Test Result: FAILED ✗");
                    return false;
                }
            }

            addLog("All source type filters working correctly");
            addLog("Test Result: PASSED ✓");
            return true;

        } catch (Exception e) {
            addLog("Error: " + e.getMessage());
            addLog("Test Result: FAILED ✗");
            return false;
        }
    }

    /**
     * Test 6: Multi-tenant filtering.
     */
    private boolean testMultiTenantFiltering() {
        try {
            int clientId = Env.getAD_Client_ID(getCtx());
            addLog("Current Client ID: " + clientId);

            List<SearchResult> results = ragService.search(getCtx(), p_SearchQuery, null, 10);

            // Check that all results belong to current client
            for (SearchResult result : results) {
                Map<String, Object> metadata = result.getMetadata();
                if (metadata != null) {
                    Object adClientId = metadata.get("ad_client_id");
                    if (adClientId != null) {
                        int resultClientId = Integer.parseInt(adClientId.toString());
                        if (resultClientId != clientId && resultClientId != 0) {
                            addLog("⚠ Found result from different client: " + resultClientId);
                            addLog("Test Result: FAILED ✗");
                            return false;
                        }
                    }
                }
            }

            addLog("All results belong to current client or system (0)");
            addLog("Test Result: PASSED ✓");
            return true;

        } catch (Exception e) {
            addLog("Error: " + e.getMessage());
            addLog("Test Result: FAILED ✗");
            return false;
        }
    }

    /**
     * Truncate string for display.
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return "(null)";
        s = s.replace("\n", " ").replace("\r", "");
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }
}
