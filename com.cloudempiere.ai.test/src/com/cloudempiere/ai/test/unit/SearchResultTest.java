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
import com.cloudempiere.ai.test.support.TestLogger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.cloudempiere.ai.rag.dto.SearchResult;

/**
 * Unit tests for SearchResult DTO (ADR-012).
 *
 * Tests the SearchResult builder, formatting, and data access.
 * These tests do not require iDempiere context and can run standalone.
 *
 * Run with -v for detailed logging:
 *   ./run-unit-tests.sh -v SearchResultTest
 *
 * @author Cloudempiere
 * @version 1.0
 */
@UnitTest
@DisplayName("SearchResult Tests")
class SearchResultTest {

    private static final TestLogger log = new TestLogger(SearchResultTest.class);

    @BeforeEach
    void setUp(TestInfo testInfo) {
        testInfo.getTestMethod().ifPresent(m -> log.testStart(m.getName()));
    }

    // ========================================================================
    // Builder Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Builder Pattern")
    class BuilderTests {

        @Test
        @UnitTest
@DisplayName("Builder creates SearchResult with all fields")
        void shouldBuildWithAllFields() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("entity_type", "window");
            metadata.put("ad_client_id", "1000000");

            SearchResult result = SearchResult.builder()
                .id("uuid-123")
                .title("Sales Order")
                .content("Window: Sales Order\nDescription: Manage sales orders")
                .sourceType("ad_metadata")
                .sourceId("100")
                .score(0.95)
                .metadata(metadata)
                .build();

            log.output("id", result.getId());
            log.output("title", result.getTitle());
            log.output("content", result.getContent());
            log.output("sourceType", result.getSourceType());
            log.output("sourceId", result.getSourceId());
            log.output("score", result.getScore());

            assertThat(result.getId()).isEqualTo("uuid-123");
            assertThat(result.getTitle()).isEqualTo("Sales Order");
            assertThat(result.getContent()).contains("Sales Order");
            assertThat(result.getSourceType()).isEqualTo("ad_metadata");
            assertThat(result.getSourceId()).isEqualTo("100");
            assertThat(result.getScore()).isEqualTo(0.95);
            assertThat(result.getMetadata()).containsKey("entity_type");
        }

        @Test
        @UnitTest
@DisplayName("Builder creates SearchResult with minimal fields")
        void shouldBuildWithMinimalFields() {
            SearchResult result = SearchResult.builder()
                .id("uuid-456")
                .content("Some content")
                .score(0.8)
                .build();

            assertThat(result.getId()).isEqualTo("uuid-456");
            assertThat(result.getTitle()).isNull();
            assertThat(result.getContent()).isEqualTo("Some content");
            assertThat(result.getSourceType()).isNull();
            assertThat(result.getSourceId()).isNull();
            assertThat(result.getScore()).isEqualTo(0.8);
            assertThat(result.getMetadata()).isNull();
        }

        @Test
        @UnitTest
@DisplayName("Builder allows null values")
        void shouldAllowNullValues() {
            SearchResult result = SearchResult.builder()
                .id(null)
                .title(null)
                .content(null)
                .sourceType(null)
                .sourceId(null)
                .score(0.0)
                .metadata(null)
                .build();

            assertThat(result.getId()).isNull();
            assertThat(result.getTitle()).isNull();
            assertThat(result.getContent()).isNull();
            assertThat(result.getScore()).isZero();
        }
    }

    // ========================================================================
    // Constructor Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @UnitTest
@DisplayName("Direct constructor works")
        void shouldConstructDirectly() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("key", "value");

            SearchResult result = new SearchResult(
                "id-1",
                "Test Title",
                "Test Content",
                "glossary",
                "123",
                0.75,
                metadata
            );

            assertThat(result.getId()).isEqualTo("id-1");
            assertThat(result.getTitle()).isEqualTo("Test Title");
            assertThat(result.getContent()).isEqualTo("Test Content");
            assertThat(result.getSourceType()).isEqualTo("glossary");
            assertThat(result.getSourceId()).isEqualTo("123");
            assertThat(result.getScore()).isEqualTo(0.75);
            assertThat(result.getMetadata()).containsEntry("key", "value");
        }
    }

    // ========================================================================
    // Formatted String Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Formatted String Output")
    class FormattedStringTests {

        @Test
        @UnitTest
@DisplayName("toFormattedString includes title in bold")
        void shouldFormatTitleAsBold() {
            SearchResult result = SearchResult.builder()
                .title("Sales Order Window")
                .content("Use this window to manage sales orders")
                .sourceType("ad_metadata")
                .sourceId("100")
                .score(0.92)
                .build();

            String formatted = result.toFormattedString();
            log.output("formatted", formatted);

            assertThat(formatted).contains("**Sales Order Window**");
        }

        @Test
        @UnitTest
@DisplayName("toFormattedString includes content")
        void shouldIncludeContent() {
            SearchResult result = SearchResult.builder()
                .title("Test")
                .content("This is the main content of the search result")
                .sourceType("knowledge_entry")
                .score(0.85)
                .build();

            String formatted = result.toFormattedString();

            assertThat(formatted).contains("This is the main content");
        }

        @Test
        @UnitTest
@DisplayName("toFormattedString includes source and score")
        void shouldIncludeSourceAndScore() {
            SearchResult result = SearchResult.builder()
                .title("Process")
                .content("Generate invoices process")
                .sourceType("ad_metadata")
                .sourceId("200")
                .score(0.88)
                .build();

            String formatted = result.toFormattedString();

            assertThat(formatted).contains("ad_metadata");
            assertThat(formatted).contains("200");
            assertThat(formatted).contains("0.88");
        }

        @Test
        @UnitTest
@DisplayName("toFormattedString handles null title")
        void shouldHandleNullTitle() {
            SearchResult result = SearchResult.builder()
                .title(null)
                .content("Content without title")
                .sourceType("glossary")
                .score(0.70)
                .build();

            String formatted = result.toFormattedString();

            assertThat(formatted).contains("**Untitled**");
            assertThat(formatted).contains("Content without title");
        }

        @Test
        @UnitTest
@DisplayName("toFormattedString handles empty sourceId")
        void shouldHandleEmptySourceId() {
            SearchResult result = SearchResult.builder()
                .title("Test")
                .content("Test content")
                .sourceType("ad_metadata")
                .sourceId("")
                .score(0.75)
                .build();

            String formatted = result.toFormattedString();

            assertThat(formatted).contains("ad_metadata");
            // Empty sourceId should not show parentheses
            assertThat(formatted).doesNotContain("()");
        }
    }

    // ========================================================================
    // toString Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("toString Method")
    class ToStringTests {

        @Test
        @UnitTest
@DisplayName("toString returns summary format")
        void shouldReturnSummaryFormat() {
            SearchResult result = SearchResult.builder()
                .id("abc-123")
                .title("Test Window")
                .sourceType("ad_metadata")
                .score(0.90)
                .build();

            String str = result.toString();
            log.output("toString", str);

            assertThat(str).contains("SearchResult");
            assertThat(str).contains("id=abc-123");
            assertThat(str).contains("title=Test Window");
            assertThat(str).contains("source=ad_metadata");
            assertThat(str).contains("score=0.90");
        }

        @Test
        @UnitTest
@DisplayName("toString handles null values")
        void shouldHandleNullValuesInToString() {
            SearchResult result = SearchResult.builder()
                .id(null)
                .title(null)
                .sourceType(null)
                .score(0.5)
                .build();

            String str = result.toString();

            // Should not throw, should contain nulls
            assertThat(str).contains("SearchResult");
            assertThat(str).contains("id=null");
        }
    }

    // ========================================================================
    // Score Boundary Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Score Boundaries")
    class ScoreBoundaryTests {

        @Test
        @UnitTest
@DisplayName("Accepts perfect score of 1.0")
        void shouldAcceptPerfectScore() {
            SearchResult result = SearchResult.builder()
                .id("perfect")
                .score(1.0)
                .build();

            assertThat(result.getScore()).isEqualTo(1.0);
        }

        @Test
        @UnitTest
@DisplayName("Accepts zero score")
        void shouldAcceptZeroScore() {
            SearchResult result = SearchResult.builder()
                .id("zero")
                .score(0.0)
                .build();

            assertThat(result.getScore()).isZero();
        }

        @Test
        @UnitTest
@DisplayName("Accepts typical relevance score")
        void shouldAcceptTypicalScore() {
            SearchResult result = SearchResult.builder()
                .id("typical")
                .score(0.756)
                .build();

            assertThat(result.getScore()).isEqualTo(0.756);
        }
    }

    // ========================================================================
    // Metadata Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Metadata Handling")
    class MetadataTests {

        @Test
        @UnitTest
@DisplayName("Metadata preserves all key-value pairs")
        void shouldPreserveAllMetadata() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_type", "ad_metadata");
            metadata.put("source_id", "100");
            metadata.put("entity_type", "window");
            metadata.put("ad_client_id", "1000000");
            metadata.put("title", "Sales Order");

            SearchResult result = SearchResult.builder()
                .id("test")
                .metadata(metadata)
                .build();

            assertThat(result.getMetadata())
                .hasSize(5)
                .containsEntry("source_type", "ad_metadata")
                .containsEntry("source_id", "100")
                .containsEntry("entity_type", "window")
                .containsEntry("ad_client_id", "1000000")
                .containsEntry("title", "Sales Order");
        }

        @Test
        @UnitTest
@DisplayName("Metadata supports various value types")
        void shouldSupportVariousMetadataTypes() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("string_val", "text");
            metadata.put("int_val", 42);
            metadata.put("double_val", 3.14);
            metadata.put("boolean_val", true);

            SearchResult result = SearchResult.builder()
                .id("test")
                .metadata(metadata)
                .build();

            assertThat(result.getMetadata().get("string_val")).isEqualTo("text");
            assertThat(result.getMetadata().get("int_val")).isEqualTo(42);
            assertThat(result.getMetadata().get("double_val")).isEqualTo(3.14);
            assertThat(result.getMetadata().get("boolean_val")).isEqualTo(true);
        }

        @Test
        @UnitTest
@DisplayName("Empty metadata is handled")
        void shouldHandleEmptyMetadata() {
            Map<String, Object> metadata = new HashMap<>();

            SearchResult result = SearchResult.builder()
                .id("test")
                .metadata(metadata)
                .build();

            assertThat(result.getMetadata()).isEmpty();
        }
    }

    // ========================================================================
    // Real-World Scenario Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Real-World Scenarios")
    class RealWorldScenarioTests {

        @Test
        @UnitTest
@DisplayName("AD Window search result")
        void shouldRepresentADWindowResult() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_type", "ad_metadata");
            metadata.put("source_table", "AD_Window");
            metadata.put("entity_type", "window");
            metadata.put("ad_client_id", "1000000");

            SearchResult result = SearchResult.builder()
                .id("uuid-window-100")
                .title("Sales Order")
                .content("Window: Sales Order\nDescription: Enter and manage sales orders\nMain Tab: Order\nTable: C_Order")
                .sourceType("ad_metadata")
                .sourceId("100")
                .score(0.92)
                .metadata(metadata)
                .build();

            log.input("Query", "how to create sales order");
            log.output("Result", result.toFormattedString());

            assertThat(result.getTitle()).isEqualTo("Sales Order");
            assertThat(result.getContent()).contains("C_Order");
            assertThat(result.getScore()).isGreaterThan(0.9);
        }

        @Test
        @UnitTest
@DisplayName("AD Process search result")
        void shouldRepresentADProcessResult() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_type", "ad_metadata");
            metadata.put("source_table", "AD_Process");
            metadata.put("entity_type", "process");

            SearchResult result = SearchResult.builder()
                .id("uuid-process-200")
                .title("Generate Invoices")
                .content("Process: Generate Invoices\nCode: C_Invoice_Generate\nDescription: Generate invoices from shipments")
                .sourceType("ad_metadata")
                .sourceId("200")
                .score(0.88)
                .metadata(metadata)
                .build();

            assertThat(result.getContent()).contains("Generate Invoices");
            assertThat(result.getMetadata().get("entity_type")).isEqualTo("process");
        }

        @Test
        @UnitTest
@DisplayName("Glossary search result")
        void shouldRepresentGlossaryResult() {
            SearchResult result = SearchResult.builder()
                .id("uuid-glossary-1")
                .title("BPartner")
                .content("Business Partner - Represents a customer, vendor, or employee. " +
                        "Key fields: Name, Value, C_BPartner_ID. Related tables: C_BPartner_Location, AD_User")
                .sourceType("glossary")
                .sourceId("bpartner-def")
                .score(0.95)
                .build();

            String formatted = result.toFormattedString();

            assertThat(formatted).contains("BPartner");
            assertThat(formatted).contains("Business Partner");
            assertThat(result.getSourceType()).isEqualTo("glossary");
        }
    }
}
