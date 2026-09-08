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

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cloudempiere.ai.guardrails.ExecutionGuard;
import com.cloudempiere.ai.guardrails.ExecutionGuard.ApprovalDecision;
import com.cloudempiere.ai.guardrails.ExecutionGuard.RiskAssessment;
import com.cloudempiere.ai.guardrails.ExecutionGuard.RiskLevel;

/**
 * Unit tests for ExecutionGuard (ADR-014).
 *
 * Tests risk classification, approval routing, and action control.
 * These tests do not require iDempiere context and can run standalone.
 *
 * @author Cloudempiere
 * @version 1.0
 */
@UnitTest
@DisplayName("ExecutionGuard Tests")
@Tag("needs-runtime")   // ADR-020: host class needs org.compiere.util.CLogger at class-init
class ExecutionGuardTest {

    private ExecutionGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ExecutionGuard();
    }

    // ========================================================================
    // Risk Level Classification Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Risk Level Classification")
    class RiskLevelTests {

        @ParameterizedTest
        @UnitTest
@DisplayName("Read-only actions are LOW risk")
        @ValueSource(strings = {
            "SELECT",
            "READ",
            "VIEW",
            "LIST",
            "GET",
            "SEARCH",
            "QUERY",
            "REPORT"
        })
        void shouldClassifyReadOnlyAsLowRisk(String action) {
            RiskAssessment result = guard.assessRisk(action, null, 1, BigDecimal.ZERO);
            assertThat(result.getLevel())
                .as("%s should be LOW risk", action)
                .isEqualTo(RiskLevel.LOW);
        }

        @ParameterizedTest
        @UnitTest
@DisplayName("Single record modifications are MEDIUM risk")
        @ValueSource(strings = {
            "CREATE",
            "UPDATE",
            "SAVE",
            "MODIFY"
        })
        void shouldClassifySingleRecordModificationsAsMediumRisk(String action) {
            RiskAssessment result = guard.assessRisk(action, "M_Product", 1, BigDecimal.ZERO);
            assertThat(result.getLevel())
                .as("%s with 1 record should be MEDIUM risk", action)
                .isEqualTo(RiskLevel.MEDIUM);
        }

        @ParameterizedTest
        @UnitTest
@DisplayName("Delete actions are HIGH risk")
        @ValueSource(strings = {
            "DELETE",
            "REMOVE"
        })
        void shouldClassifyDeleteActionsAsHighRisk(String action) {
            RiskAssessment result = guard.assessRisk(action, "M_Product", 1, BigDecimal.ZERO);
            assertThat(result.getLevel())
                .as("%s should be HIGH risk", action)
                .isEqualTo(RiskLevel.HIGH);
        }

        @Test
        @UnitTest
@DisplayName("Bulk operations (>100 records) are PROHIBITED")
        void shouldClassifyBulkOperationsAsProhibited() {
            RiskAssessment result = guard.assessRisk("UPDATE", "M_Product", 101, BigDecimal.ZERO);
            assertThat(result.getLevel()).isEqualTo(RiskLevel.PROHIBITED);
            assertThat(result.isBlocked()).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("High-value transactions (>$10,000) are CRITICAL risk")
        void shouldClassifyHighValueAsCriticalRisk() {
            RiskAssessment result = guard.assessRisk("CREATE", "C_Order",
                1, new BigDecimal("15000"));
            assertThat(result.getLevel()).isEqualTo(RiskLevel.CRITICAL);
        }

        @Test
        @UnitTest
@DisplayName("Medium-value transactions (>$1,000) are HIGH risk")
        void shouldClassifyMediumValueAsHighRisk() {
            RiskAssessment result = guard.assessRisk("CREATE", "C_Order",
                1, new BigDecimal("5000"));
            assertThat(result.getLevel()).isEqualTo(RiskLevel.HIGH);
        }
    }

    // ========================================================================
    // Approval Decision Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Approval Decision Routing")
    class ApprovalDecisionTests {

        @Test
        @UnitTest
@DisplayName("LOW risk -> AUTO_APPROVE")
        void shouldAutoApproveLowRisk() {
            RiskAssessment result = guard.assessRisk("SELECT", "C_Order", 1, BigDecimal.ZERO);
            assertThat(result.getLevel()).isEqualTo(RiskLevel.LOW);
            assertThat(result.getDecision()).isEqualTo(ApprovalDecision.AUTO_APPROVE);
            assertThat(result.canAutoApprove()).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("MEDIUM risk -> AUTO_APPROVE (with logging)")
        void shouldAutoApproveForMediumRisk() {
            RiskAssessment result = guard.assessRisk("UPDATE", "M_Product", 1, BigDecimal.ZERO);
            assertThat(result.getLevel()).isEqualTo(RiskLevel.MEDIUM);
            assertThat(result.getDecision()).isEqualTo(ApprovalDecision.AUTO_APPROVE);
        }

        @Test
        @UnitTest
@DisplayName("HIGH risk -> REQUIRE_CONFIRMATION")
        void shouldRequireConfirmationForHighRisk() {
            RiskAssessment result = guard.assessRisk("DELETE", "M_Product", 1, BigDecimal.ZERO);
            assertThat(result.getLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(result.getDecision()).isEqualTo(ApprovalDecision.REQUIRE_CONFIRMATION);
            assertThat(result.requiresConfirmation()).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("CRITICAL risk -> REQUIRE_HUMAN_APPROVAL")
        void shouldRequireHumanApprovalForCriticalRisk() {
            RiskAssessment result = guard.assessRisk("CREATE", "C_Order", 1, new BigDecimal("50000"));
            assertThat(result.getLevel()).isEqualTo(RiskLevel.CRITICAL);
            assertThat(result.getDecision()).isEqualTo(ApprovalDecision.REQUIRE_HUMAN_APPROVAL);
        }

        @Test
        @UnitTest
@DisplayName("PROHIBITED risk -> BLOCK")
        void shouldBlockProhibitedRisk() {
            RiskAssessment result = guard.assessRisk("DROP_TABLE", "C_Order", 1, BigDecimal.ZERO);
            assertThat(result.getLevel()).isEqualTo(RiskLevel.PROHIBITED);
            assertThat(result.getDecision()).isEqualTo(ApprovalDecision.BLOCK);
            assertThat(result.isBlocked()).isTrue();
        }
    }

    // ========================================================================
    // Prohibited Actions Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Prohibited Actions")
    class ProhibitedActionsTests {

        @ParameterizedTest
        @UnitTest
@DisplayName("Sensitive table modifications are HIGH risk")
        @ValueSource(strings = {
            "AD_Role",
            "AD_User",
            "C_Payment"
        })
        void shouldEscalateSensitiveTableModifications(String tableName) {
            RiskAssessment result = guard.assessRisk("UPDATE", tableName, 1, BigDecimal.ZERO);
            assertThat(result.getLevel().ordinal())
                .as("Modifying %s should be at least HIGH risk", tableName)
                .isGreaterThanOrEqualTo(RiskLevel.HIGH.ordinal());
        }

        @Test
        @UnitTest
@DisplayName("TRUNCATE is always PROHIBITED")
        void shouldProhibitTruncate() {
            RiskAssessment result = guard.assessRisk("TRUNCATE", "C_Order", 1, BigDecimal.ZERO);
            assertThat(result.getLevel()).isEqualTo(RiskLevel.PROHIBITED);
        }

        @Test
        @UnitTest
@DisplayName("DROP is always PROHIBITED")
        void shouldProhibitDrop() {
            RiskAssessment result = guard.assessRisk("DROP", "C_Order", 1, BigDecimal.ZERO);
            assertThat(result.getLevel()).isEqualTo(RiskLevel.PROHIBITED);
        }

        @Test
        @UnitTest
@DisplayName("isActionAllowed returns false for prohibited actions")
        void shouldReturnFalseForProhibitedActions() {
            assertThat(guard.isActionAllowed("DROP_TABLE")).isFalse();
            assertThat(guard.isActionAllowed("TRUNCATE_TABLE")).isFalse();
            assertThat(guard.isActionAllowed("BULK_DELETE")).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("isActionAllowed returns true for allowed actions")
        void shouldReturnTrueForAllowedActions() {
            assertThat(guard.isActionAllowed("SELECT")).isTrue();
            assertThat(guard.isActionAllowed("UPDATE")).isTrue();
            assertThat(guard.isActionAllowed("CREATE")).isTrue();
        }
    }

    // ========================================================================
    // Table-Specific Risk Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Table-Specific Risk Escalation")
    class TableSpecificRiskTests {

        @Test
        @UnitTest
@DisplayName("Payment table modifications are at least HIGH risk")
        void shouldEscalatePaymentTableRisk() {
            RiskAssessment result = guard.assessRisk("CREATE", "C_Payment", 1, BigDecimal.ZERO);
            assertThat(result.getLevel().ordinal())
                .as("Payment operations should be at least HIGH risk")
                .isGreaterThanOrEqualTo(RiskLevel.HIGH.ordinal());
        }

        @Test
        @UnitTest
@DisplayName("Bank statement modifications are at least HIGH risk")
        void shouldEscalateBankStatementTableRisk() {
            RiskAssessment result = guard.assessRisk("UPDATE", "C_BankStatement", 1, BigDecimal.ZERO);
            assertThat(result.getLevel().ordinal())
                .as("Bank statement operations should be at least HIGH risk")
                .isGreaterThanOrEqualTo(RiskLevel.HIGH.ordinal());
        }
    }

    // ========================================================================
    // Record Count Escalation Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Record Count Escalation")
    class RecordCountTests {

        @Test
        @UnitTest
@DisplayName("Single record is base risk")
        void shouldClassifySingleRecordAsBaseRisk() {
            RiskAssessment result = guard.assessRisk("UPDATE", "M_Product", 1, BigDecimal.ZERO);
            assertThat(result.getLevel()).isEqualTo(RiskLevel.MEDIUM);
        }

        @Test
        @UnitTest
@DisplayName("5 records stays same risk")
        void shouldNotEscalateForFewRecords() {
            RiskAssessment result = guard.assessRisk("UPDATE", "M_Product", 5, BigDecimal.ZERO);
            assertThat(result.getLevel()).isEqualTo(RiskLevel.MEDIUM);
        }

        @Test
        @UnitTest
@DisplayName("11+ records escalates to HIGH (bulk threshold = 10)")
        void shouldEscalateManyRecordsToHigh() {
            RiskAssessment result = guard.assessRisk("UPDATE", "M_Product", 15, BigDecimal.ZERO);
            assertThat(result.getLevel())
                .as("10+ records should escalate to HIGH")
                .isEqualTo(RiskLevel.HIGH);
        }

        @Test
        @UnitTest
@DisplayName("101+ records is PROHIBITED (max = 100)")
        void shouldProhibitExcessiveRecords() {
            RiskAssessment result = guard.assessRisk("UPDATE", "M_Product", 101, BigDecimal.ZERO);
            assertThat(result.getLevel())
                .as("101+ records should be PROHIBITED")
                .isEqualTo(RiskLevel.PROHIBITED);
            assertThat(result.isBlocked()).isTrue();
        }
    }

    // ========================================================================
    // Amount Threshold Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Amount Threshold Escalation")
    class AmountThresholdTests {

        @Test
        @UnitTest
@DisplayName("Small amount stays base risk")
        void shouldNotEscalateSmallAmount() {
            RiskAssessment result = guard.assessRisk("CREATE", "C_Order", 1,
                new BigDecimal("500"));
            assertThat(result.getLevel()).isEqualTo(RiskLevel.MEDIUM);
        }

        @Test
        @UnitTest
@DisplayName("Amount >$1,000 escalates to HIGH")
        void shouldEscalateMediumAmountToHigh() {
            RiskAssessment result = guard.assessRisk("CREATE", "C_Order", 1,
                new BigDecimal("5000"));
            assertThat(result.getLevel()).isEqualTo(RiskLevel.HIGH);
        }

        @Test
        @UnitTest
@DisplayName("Amount >$10,000 escalates to CRITICAL")
        void shouldEscalateLargeAmountToCritical() {
            RiskAssessment result = guard.assessRisk("CREATE", "C_Order", 1,
                new BigDecimal("15000"));
            assertThat(result.getLevel()).isEqualTo(RiskLevel.CRITICAL);
        }
    }

    // ========================================================================
    // ShouldProceed Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("ShouldProceed Decision")
    class ShouldProceedTests {

        @Test
        @UnitTest
@DisplayName("Auto-approve proceeds without confirmation")
        void shouldProceedWithAutoApprove() {
            boolean result = guard.shouldProceed("SELECT", "C_Order", 1, BigDecimal.ZERO, false);
            assertThat(result).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Confirmation required - proceeds when confirmed")
        void shouldProceedWhenConfirmed() {
            boolean result = guard.shouldProceed("DELETE", "M_Product", 1, BigDecimal.ZERO, true);
            assertThat(result).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Confirmation required - blocks when not confirmed")
        void shouldBlockWhenNotConfirmed() {
            boolean result = guard.shouldProceed("DELETE", "M_Product", 1, BigDecimal.ZERO, false);
            assertThat(result).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("Prohibited actions always blocked")
        void shouldBlockProhibitedActions() {
            boolean result = guard.shouldProceed("DROP_TABLE", "C_Order", 1, BigDecimal.ZERO, true);
            assertThat(result).isFalse();
        }
    }

    // ========================================================================
    // RiskAssessment Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("RiskAssessment Object")
    class RiskAssessmentTests {

        @Test
        @UnitTest
@DisplayName("Assessment contains reasons")
        void shouldContainReasons() {
            RiskAssessment result = guard.assessRisk("DELETE", "C_Payment", 15,
                new BigDecimal("5000"));
            assertThat(result.getReasons())
                .as("Assessment should contain reasons")
                .isNotEmpty();
        }

        @Test
        @UnitTest
@DisplayName("toString returns formatted string")
        void shouldReturnFormattedString() {
            RiskAssessment result = guard.assessRisk("SELECT", "C_Order", 1, BigDecimal.ZERO);
            String str = result.toString();
            assertThat(str)
                .contains("RiskAssessment")
                .contains("level=")
                .contains("decision=");
        }
    }
}
