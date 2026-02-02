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
package com.cloudempiere.ai.test.unit;

import com.cloudempiere.ai.test.categories.UnitTest;
import com.cloudempiere.ai.test.support.RagServiceMock;
import com.cloudempiere.ai.test.support.TestLogger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.cloudempiere.ai.provider.langchain4j.tools.RagTools;
import com.cloudempiere.ai.rag.dto.SearchResult;

/**
 * Unit tests for RagTools (ADR-012).
 *
 * Tests the @Tool methods used by AI agents for knowledge retrieval.
 * Uses MockRagService to avoid database and embedding model dependencies.
 *
 * Run with -v for detailed logging:
 *   ./run-unit-tests.sh -v RagToolsTest
 *
 * @author Cloudempiere
 * @version 1.0
 */
@UnitTest
@DisplayName("RagTools Tests")
class RagToolsTest {

    private static final TestLogger log = new TestLogger(RagToolsTest.class);
    private RagServiceMock mockRagService;
    private RagTools ragTools;
    private Properties ctx;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        mockRagService = new RagServiceMock();
        ctx = new Properties();
        ctx.setProperty("#AD_Client_ID", "1000000");
        ctx.setProperty("#AD_Org_ID", "0");
        ragTools = new RagTools(mockRagService, ctx);
        testInfo.getTestMethod().ifPresent(m -> log.testStart(m.getName()));
    }

    // ========================================================================
    // searchKnowledge Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("searchKnowledge Tool")
    class SearchKnowledgeTests {

        @Test
        @UnitTest
@DisplayName("Returns formatted results for matching query")
        void shouldReturnFormattedResults() {
            SearchResult windowResult = SearchResult.builder()
                .id("uuid-1")
                .title("Sales Order")
                .content("Window: Sales Order\nDescription: Manage sales orders")
                .sourceType("ad_metadata")
                .sourceId("100")
                .score(0.92)
                .build();

            mockRagService.setSearchResult(windowResult);

            String result = ragTools.searchKnowledge("how to create sales order", null);
            log.output("Result", result);

            assertThat(result)
                .contains("Found 1 relevant item")
                .contains("Sales Order")
                .contains("Manage sales orders");
        }

        @Test
        @UnitTest
@DisplayName("Returns multiple results when available")
        void shouldReturnMultipleResults() {
            SearchResult result1 = SearchResult.builder()
                .title("Sales Order")
                .content("Window for sales")
                .sourceType("ad_metadata")
                .score(0.95)
                .build();
            SearchResult result2 = SearchResult.builder()
                .title("Purchase Order")
                .content("Window for purchases")
                .sourceType("ad_metadata")
                .score(0.85)
                .build();

            mockRagService.setSearchResults(Arrays.asList(result1, result2));

            String result = ragTools.searchKnowledge("order window", null);

            assertThat(result)
                .contains("Found 2 relevant item")
                .contains("Sales Order")
                .contains("Purchase Order");
        }

        @Test
        @UnitTest
@DisplayName("Returns no results message when empty")
        void shouldReturnNoResultsMessage() {
            mockRagService.clearSearchResults();

            String result = ragTools.searchKnowledge("xyz123nonexistent", null);
            log.output("Result", result);

            assertThat(result)
                .contains("No relevant knowledge found")
                .contains("xyz123nonexistent");
        }

        @Test
        @UnitTest
@DisplayName("Passes source filter to service")
        void shouldPassSourceFilter() {
            mockRagService.clearSearchResults();

            ragTools.searchKnowledge("test query", "ad_metadata");

            assertThat(mockRagService.getLastSearchQuery()).isEqualTo("test query");
            assertThat(mockRagService.getLastSearchFilter()).isEqualTo("ad_metadata");
        }

        @ParameterizedTest
        @UnitTest
@DisplayName("Normalizes empty filter to null")
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        void shouldNormalizeEmptyFilter(String filter) {
            mockRagService.clearSearchResults();

            ragTools.searchKnowledge("test query", filter);

            // Empty/whitespace filter should be normalized to null
            assertThat(mockRagService.getLastSearchFilter()).isNull();
        }

        @Test
        @UnitTest
@DisplayName("Returns unavailable message when service unavailable")
        void shouldReturnUnavailableMessage() {
            RagServiceMock unavailable = RagServiceMock.unavailable();
            RagTools tools = new RagTools(unavailable, ctx);

            String result = tools.searchKnowledge("any query", null);

            assertThat(result).contains("not available");
        }

        @Test
        @UnitTest
@DisplayName("Returns unavailable message when service is null")
        void shouldHandleNullService() {
            RagTools tools = new RagTools(null, ctx);

            String result = tools.searchKnowledge("any query", null);

            assertThat(result).contains("not available");
        }
    }

    // ========================================================================
    // findADEntity Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("findADEntity Tool")
    class FindADEntityTests {

        @Test
        @UnitTest
@DisplayName("Returns entity details for window")
        void shouldReturnWindowDetails() {
            mockRagService.setADEntityResult(
                "Found 1 matching window(s):\n\n" +
                "**Sales Order**\n" +
                "Window: Sales Order\n" +
                "Description: Enter and manage sales orders"
            );

            String result = ragTools.findADEntity("Sales Order", "window");
            log.output("Result", result);

            assertThat(result)
                .contains("Sales Order")
                .contains("window");

            assertThat(mockRagService.getLastADEntityName()).isEqualTo("Sales Order");
            assertThat(mockRagService.getLastADEntityType()).isEqualTo("window");
        }

        @Test
        @UnitTest
@DisplayName("Returns entity details for process")
        void shouldReturnProcessDetails() {
            mockRagService.setADEntityResult(
                "Found 1 matching process(s):\n\n" +
                "**Generate Invoices**\n" +
                "Process: Generate Invoices\n" +
                "Description: Generate invoices from shipments"
            );

            String result = ragTools.findADEntity("Generate Invoices", "process");

            assertThat(result).contains("Generate Invoices");
            assertThat(mockRagService.getLastADEntityType()).isEqualTo("process");
        }

        @Test
        @UnitTest
@DisplayName("Returns entity details for table")
        void shouldReturnTableDetails() {
            mockRagService.setADEntityResult(
                "Found 1 matching table(s):\n\n" +
                "**C_Order**\n" +
                "Table: C_Order\n" +
                "Description: Sales Order header"
            );

            String result = ragTools.findADEntity("C_Order", "table");

            assertThat(result).contains("C_Order");
            assertThat(mockRagService.getLastADEntityType()).isEqualTo("table");
        }

        @Test
        @UnitTest
@DisplayName("Returns unavailable message when service unavailable")
        void shouldReturnUnavailableMessage() {
            RagTools tools = new RagTools(RagServiceMock.unavailable(), ctx);

            String result = tools.findADEntity("Test", "window");

            assertThat(result).contains("not available");
        }

        @Test
        @UnitTest
@DisplayName("Tracks call count")
        void shouldTrackCallCount() {
            ragTools.findADEntity("Entity1", "window");
            ragTools.findADEntity("Entity2", "process");
            ragTools.findADEntity("Entity3", "table");

            assertThat(mockRagService.getAdEntityCallCount()).isEqualTo(3);
        }
    }

    // ========================================================================
    // lookupGlossary Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("lookupGlossary Tool")
    class LookupGlossaryTests {

        @Test
        @UnitTest
@DisplayName("Returns glossary definition")
        void shouldReturnGlossaryDefinition() {
            mockRagService.setGlossaryResult(
                "**Glossary: BPartner**\n\n" +
                "Business Partner - Represents a customer, vendor, or employee."
            );

            String result = ragTools.lookupGlossary("BPartner");
            log.output("Result", result);

            assertThat(result)
                .contains("BPartner")
                .contains("Business Partner");

            assertThat(mockRagService.getLastGlossaryTerm()).isEqualTo("BPartner");
        }

        @Test
        @UnitTest
@DisplayName("Returns not found message for unknown term")
        void shouldReturnNotFoundMessage() {
            mockRagService.setGlossaryResult(
                "No glossary entry found for 'XyzUnknown'. " +
                "This term may not be defined in the knowledge base."
            );

            String result = ragTools.lookupGlossary("XyzUnknown");

            assertThat(result)
                .contains("No glossary entry found")
                .contains("XyzUnknown");
        }

        @Test
        @UnitTest
@DisplayName("Returns unavailable message when service unavailable")
        void shouldReturnUnavailableMessage() {
            RagTools tools = new RagTools(RagServiceMock.unavailable(), ctx);

            String result = tools.lookupGlossary("Term");

            assertThat(result).contains("not available");
        }

        @Test
        @UnitTest
@DisplayName("Tracks call count")
        void shouldTrackCallCount() {
            ragTools.lookupGlossary("Term1");
            ragTools.lookupGlossary("Term2");

            assertThat(mockRagService.getGlossaryCallCount()).isEqualTo(2);
        }
    }

    // ========================================================================
    // getKnowledgeStats Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("getKnowledgeStats Tool")
    class GetKnowledgeStatsTests {

        @Test
        @UnitTest
@DisplayName("Returns formatted statistics")
        void shouldReturnFormattedStats() {
            String result = ragTools.getKnowledgeStats();
            log.output("Result", result);

            assertThat(result)
                .contains("Knowledge Base Statistics")
                .contains("Available")
                .contains("ad_metadata")
                .contains("glossary");
        }

        @Test
        @UnitTest
@DisplayName("Shows document counts by source")
        void shouldShowCountsBySource() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("available", true);
            Map<String, Long> countsBySource = new HashMap<>();
            countsBySource.put("ad_metadata", 500L);
            countsBySource.put("glossary", 50L);
            countsBySource.put("knowledge_entry", 100L);
            stats.put("countsBySource", countsBySource);
            stats.put("totalCount", 650L);
            stats.put("sourceTypes", Arrays.asList("ad_metadata", "glossary", "knowledge_entry"));

            mockRagService.setStats(stats);

            String result = ragTools.getKnowledgeStats();

            assertThat(result)
                .contains("ad_metadata")
                .contains("500")
                .contains("Total Documents")
                .contains("650");
        }

        @Test
        @UnitTest
@DisplayName("Shows not available when service unavailable")
        void shouldShowNotAvailableWhenUnavailable() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("available", false);
            mockRagService.setStats(stats);

            String result = ragTools.getKnowledgeStats();

            assertThat(result)
                .contains("Not Available");
        }

        @Test
        @UnitTest
@DisplayName("Returns not configured when service is null")
        void shouldHandleNullService() {
            RagTools tools = new RagTools(null, ctx);

            String result = tools.getKnowledgeStats();

            assertThat(result).contains("not configured");
        }
    }

    // ========================================================================
    // Integration Scenario Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Integration Scenarios")
    class IntegrationScenarioTests {

        @Test
        @UnitTest
@DisplayName("Agent finds window for user question")
        void shouldFindWindowForUserQuestion() {
            // Simulate: User asks "how do I enter a sales order?"
            RagServiceMock service = RagServiceMock.withWindowResult(
                "Sales Order",
                "Enter and manage sales orders for customers"
            );
            RagTools tools = new RagTools(service, ctx);

            String result = tools.searchKnowledge("how do I enter a sales order", null);

            assertThat(result)
                .contains("Sales Order")
                .contains("sales orders");
        }

        @Test
        @UnitTest
@DisplayName("Agent looks up business term")
        void shouldLookUpBusinessTerm() {
            // Simulate: User asks "what is a BPartner?"
            RagServiceMock service = RagServiceMock.withGlossaryResult(
                "BPartner",
                "Business Partner - A party that can be a customer, vendor, or employee"
            );
            RagTools tools = new RagTools(service, ctx);

            String result = tools.lookupGlossary("BPartner");

            assertThat(result)
                .contains("BPartner")
                .contains("Business Partner");
        }

        @Test
        @UnitTest
@DisplayName("Agent finds process for action")
        void shouldFindProcessForAction() {
            // Simulate: User asks "how to generate invoices?"
            RagServiceMock service = RagServiceMock.withProcessResult(
                "Generate Invoices",
                "Generate invoices from completed shipments"
            );
            RagTools tools = new RagTools(service, ctx);

            String result = tools.searchKnowledge("generate invoices", "ad_metadata");

            assertThat(result)
                .contains("Generate Invoices")
                .contains("invoices");
        }

        @Test
        @UnitTest
@DisplayName("Agent handles service failure gracefully")
        void shouldHandleServiceFailureGracefully() {
            RagServiceMock service = RagServiceMock.unavailable();
            RagTools tools = new RagTools(service, ctx);

            // All tools should return graceful messages
            assertThat(tools.searchKnowledge("query", null))
                .contains("not available");
            assertThat(tools.findADEntity("entity", "window"))
                .contains("not available");
            assertThat(tools.lookupGlossary("term"))
                .contains("not available");
        }

        @Test
        @UnitTest
@DisplayName("Multiple tool calls in sequence")
        void shouldHandleSequentialCalls() {
            SearchResult result1 = SearchResult.builder()
                .title("Result 1")
                .content("Content 1")
                .sourceType("ad_metadata")
                .score(0.9)
                .build();

            mockRagService.setSearchResult(result1);

            // First call
            ragTools.searchKnowledge("first query", null);
            assertThat(mockRagService.getLastSearchQuery()).isEqualTo("first query");

            // Second call
            ragTools.searchKnowledge("second query", "glossary");
            assertThat(mockRagService.getLastSearchQuery()).isEqualTo("second query");
            assertThat(mockRagService.getLastSearchFilter()).isEqualTo("glossary");

            // Verify both calls were tracked
            assertThat(mockRagService.getSearchCallCount()).isEqualTo(2);
        }
    }

    // ========================================================================
    // Edge Case Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @UnitTest
@DisplayName("Handles very long query")
        void shouldHandleLongQuery() {
            StringBuilder longQuery = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longQuery.append("word ").append(i).append(" ");
            }

            mockRagService.clearSearchResults();

            String result = ragTools.searchKnowledge(longQuery.toString(), null);

            assertThat(mockRagService.getLastSearchQuery()).isNotNull();
            assertThat(result).contains("No relevant knowledge found");
        }

        @Test
        @UnitTest
@DisplayName("Handles special characters in query")
        void shouldHandleSpecialCharacters() {
            mockRagService.clearSearchResults();

            ragTools.searchKnowledge("O'Brien & Co. <test>", null);

            assertThat(mockRagService.getLastSearchQuery())
                .isEqualTo("O'Brien & Co. <test>");
        }

        @Test
        @UnitTest
@DisplayName("Handles unicode in query")
        void shouldHandleUnicode() {
            mockRagService.clearSearchResults();

            ragTools.searchKnowledge("Japanese: 日本語, Chinese: 中文", null);

            assertThat(mockRagService.getLastSearchQuery())
                .contains("日本語")
                .contains("中文");
        }

        @Test
        @UnitTest
@DisplayName("Reset clears tracking state")
        void shouldResetClearTracking() {
            ragTools.searchKnowledge("query", "filter");
            ragTools.findADEntity("entity", "type");
            ragTools.lookupGlossary("term");

            mockRagService.reset();

            assertThat(mockRagService.getLastSearchQuery()).isNull();
            assertThat(mockRagService.getLastADEntityName()).isNull();
            assertThat(mockRagService.getLastGlossaryTerm()).isNull();
            assertThat(mockRagService.getSearchCallCount()).isZero();
        }
    }
}
