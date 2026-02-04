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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.cloudempiere.ai.rag.ingest.IngestResult;

/**
 * Unit tests for IngestResult DTO (ADR-012).
 *
 * Tests the IngestResult tracking, error handling, and reporting.
 * These tests do not require iDempiere context and can run standalone.
 *
 * Run with -v for detailed logging:
 *   ./run-unit-tests.sh -v IngestResultTest
 *
 * @author Cloudempiere
 * @version 1.0
 */
@UnitTest
@DisplayName("IngestResult Tests")
class IngestResultTest {

    private static final TestLogger log = new TestLogger(IngestResultTest.class);
    private IngestResult result;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        result = new IngestResult();
        testInfo.getTestMethod().ifPresent(m -> log.testStart(m.getName()));
    }

    // ========================================================================
    // Initial State Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Initial State")
    class InitialStateTests {

        @Test
        @UnitTest
@DisplayName("New IngestResult has zero counters")
        void shouldHaveZeroCounters() {
            assertThat(result.getDocumentsAdded()).isZero();
            assertThat(result.getDocumentsUpdated()).isZero();
            assertThat(result.getDocumentsDeleted()).isZero();
            assertThat(result.getDocumentsSkipped()).isZero();
            assertThat(result.getDurationMs()).isZero();
        }

        @Test
        @UnitTest
@DisplayName("New IngestResult is successful by default")
        void shouldBeSuccessfulByDefault() {
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @UnitTest
@DisplayName("New IngestResult has empty errors list")
        void shouldHaveEmptyErrorsList() {
            assertThat(result.getErrors()).isEmpty();
        }
    }

    // ========================================================================
    // Counter Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Counter Operations")
    class CounterTests {

        @Test
        @UnitTest
@DisplayName("incrementAdded increases documentsAdded")
        void shouldIncrementAdded() {
            result.incrementAdded();
            result.incrementAdded();
            result.incrementAdded();

            assertThat(result.getDocumentsAdded()).isEqualTo(3);
        }

        @Test
        @UnitTest
@DisplayName("incrementUpdated increases documentsUpdated")
        void shouldIncrementUpdated() {
            result.incrementUpdated();
            result.incrementUpdated();

            assertThat(result.getDocumentsUpdated()).isEqualTo(2);
        }

        @Test
        @UnitTest
@DisplayName("incrementSkipped increases documentsSkipped")
        void shouldIncrementSkipped() {
            result.incrementSkipped();

            assertThat(result.getDocumentsSkipped()).isEqualTo(1);
        }

        @Test
        @UnitTest
@DisplayName("setDocumentsAdded sets exact value")
        void shouldSetDocumentsAdded() {
            result.setDocumentsAdded(100);

            assertThat(result.getDocumentsAdded()).isEqualTo(100);
        }

        @Test
        @UnitTest
@DisplayName("setDocumentsUpdated sets exact value")
        void shouldSetDocumentsUpdated() {
            result.setDocumentsUpdated(50);

            assertThat(result.getDocumentsUpdated()).isEqualTo(50);
        }

        @Test
        @UnitTest
@DisplayName("setDocumentsDeleted sets exact value")
        void shouldSetDocumentsDeleted() {
            result.setDocumentsDeleted(25);

            assertThat(result.getDocumentsDeleted()).isEqualTo(25);
        }

        @Test
        @UnitTest
@DisplayName("setDocumentsSkipped sets exact value")
        void shouldSetDocumentsSkipped() {
            result.setDocumentsSkipped(10);

            assertThat(result.getDocumentsSkipped()).isEqualTo(10);
        }
    }

    // ========================================================================
    // Total Processed Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Total Processed Calculation")
    class TotalProcessedTests {

        @Test
        @UnitTest
@DisplayName("getTotalProcessed sums added, updated, and skipped")
        void shouldSumAllProcessed() {
            result.setDocumentsAdded(100);
            result.setDocumentsUpdated(50);
            result.setDocumentsSkipped(10);
            result.setDocumentsDeleted(5);  // Not included in total

            int total = result.getTotalProcessed();
            log.output("Total processed", total);

            assertThat(total).isEqualTo(160);  // 100 + 50 + 10
        }

        @Test
        @UnitTest
@DisplayName("getTotalProcessed excludes deleted")
        void shouldExcludeDeleted() {
            result.setDocumentsAdded(10);
            result.setDocumentsDeleted(100);

            assertThat(result.getTotalProcessed()).isEqualTo(10);
        }

        @Test
        @UnitTest
@DisplayName("getTotalProcessed returns zero when all counters are zero")
        void shouldReturnZeroWhenEmpty() {
            assertThat(result.getTotalProcessed()).isZero();
        }
    }

    // ========================================================================
    // Duration Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Duration Tracking")
    class DurationTests {

        @Test
        @UnitTest
@DisplayName("setDurationMs sets duration")
        void shouldSetDuration() {
            result.setDurationMs(1500);

            assertThat(result.getDurationMs()).isEqualTo(1500);
        }

        @Test
        @UnitTest
@DisplayName("Duration can be large for long operations")
        void shouldHandleLargeDuration() {
            result.setDurationMs(300000);  // 5 minutes

            assertThat(result.getDurationMs()).isEqualTo(300000);
        }
    }

    // ========================================================================
    // Success/Failure Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Success/Failure Handling")
    class SuccessFailureTests {

        @Test
        @UnitTest
@DisplayName("setSuccess(false) marks as failed")
        void shouldMarkAsFailed() {
            result.setSuccess(false);

            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("setErrorMessage sets message and marks as failed")
        void shouldSetErrorMessageAndFail() {
            result.setErrorMessage("Connection timeout");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("Connection timeout");
        }

        @Test
        @UnitTest
@DisplayName("Static failed() factory creates failed result")
        void shouldCreateFailedResult() {
            IngestResult failed = IngestResult.failed("Embedding model not available");

            log.output("isSuccess", failed.isSuccess());
            log.output("errorMessage", failed.getErrorMessage());

            assertThat(failed.isSuccess()).isFalse();
            assertThat(failed.getErrorMessage()).isEqualTo("Embedding model not available");
        }
    }

    // ========================================================================
    // Error List Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Error List Management")
    class ErrorListTests {

        @Test
        @UnitTest
@DisplayName("addError adds to errors list")
        void shouldAddErrors() {
            result.addError("Window: Failed to embed AD_Window 100");
            result.addError("Process: Connection timeout for AD_Process 200");

            assertThat(result.getErrors())
                .hasSize(2)
                .contains("Window: Failed to embed AD_Window 100")
                .contains("Process: Connection timeout for AD_Process 200");
        }

        @Test
        @UnitTest
@DisplayName("Multiple errors can be accumulated")
        void shouldAccumulateErrors() {
            for (int i = 1; i <= 10; i++) {
                result.addError("Error " + i);
            }

            assertThat(result.getErrors()).hasSize(10);
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
@DisplayName("toString includes all counters")
        void shouldIncludeAllCounters() {
            result.setDocumentsAdded(100);
            result.setDocumentsUpdated(20);
            result.setDocumentsDeleted(5);
            result.setDocumentsSkipped(3);
            result.setDurationMs(2500);

            String str = result.toString();
            log.output("toString", str);

            assertThat(str)
                .contains("added=100")
                .contains("updated=20")
                .contains("deleted=5")
                .contains("skipped=3")
                .contains("duration=2500ms");
        }

        @Test
        @UnitTest
@DisplayName("toString shows FAILED for failed results")
        void shouldShowFailedStatus() {
            result.setErrorMessage("Test failure");

            String str = result.toString();

            assertThat(str).contains("FAILED");
            assertThat(str).contains("Test failure");
        }

        @Test
        @UnitTest
@DisplayName("toString does not show FAILED for successful results")
        void shouldNotShowFailedForSuccess() {
            result.setDocumentsAdded(50);
            result.setSuccess(true);

            String str = result.toString();

            assertThat(str).doesNotContain("FAILED");
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
@DisplayName("Successful AD metadata ingestion")
        void shouldTrackSuccessfulIngestion() {
            // Simulate ingesting 200 windows, 150 processes, 300 tables
            result.setDocumentsAdded(650);
            result.setDocumentsSkipped(10);  // Some empty entries skipped
            result.setDurationMs(45000);  // 45 seconds
            result.setSuccess(true);

            log.output("Result", result);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTotalProcessed()).isEqualTo(660);
            assertThat(result.getDocumentsAdded()).isEqualTo(650);
        }

        @Test
        @UnitTest
@DisplayName("Partial failure with some errors")
        void shouldTrackPartialFailure() {
            result.setDocumentsAdded(500);
            result.setDocumentsSkipped(5);
            result.addError("Window 100: Embedding failed");
            result.addError("Process 200: Timeout");
            result.setDurationMs(30000);
            result.setSuccess(true);  // Still successful overall

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getErrors()).hasSize(2);
            assertThat(result.getDocumentsAdded()).isEqualTo(500);
        }

        @Test
        @UnitTest
@DisplayName("Force refresh with deletions")
        void shouldTrackForceRefresh() {
            // First, delete existing
            result.setDocumentsDeleted(400);
            // Then add new
            result.setDocumentsAdded(450);
            result.setDurationMs(60000);
            result.setSuccess(true);

            log.output("Deleted", result.getDocumentsDeleted());
            log.output("Added", result.getDocumentsAdded());

            assertThat(result.getDocumentsDeleted()).isEqualTo(400);
            assertThat(result.getDocumentsAdded()).isEqualTo(450);
        }

        @Test
        @UnitTest
@DisplayName("Complete failure scenario")
        void shouldTrackCompleteFailure() {
            IngestResult failed = IngestResult.failed("Database connection lost");
            failed.setDurationMs(100);

            assertThat(failed.isSuccess()).isFalse();
            assertThat(failed.getErrorMessage()).contains("Database connection");
            assertThat(failed.getDocumentsAdded()).isZero();
        }

        @Test
        @UnitTest
@DisplayName("Incremental ingestion with mixed operations")
        void shouldTrackIncrementalIngestion() {
            // Simulate incremental update
            for (int i = 0; i < 50; i++) {
                result.incrementAdded();  // New documents
            }
            for (int i = 0; i < 30; i++) {
                result.incrementUpdated();  // Updated documents
            }
            for (int i = 0; i < 5; i++) {
                result.incrementSkipped();  // Unchanged
            }
            result.setDurationMs(15000);
            result.setSuccess(true);

            assertThat(result.getDocumentsAdded()).isEqualTo(50);
            assertThat(result.getDocumentsUpdated()).isEqualTo(30);
            assertThat(result.getDocumentsSkipped()).isEqualTo(5);
            assertThat(result.getTotalProcessed()).isEqualTo(85);
        }
    }
}
