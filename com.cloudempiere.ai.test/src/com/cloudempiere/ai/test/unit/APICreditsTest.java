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
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.cloudempiere.ai.provider.dto.AIRateLimitStatus;

/**
 * Tests for API credit/billing error detection.
 *
 * These tests verify that billing-related errors from AI providers
 * (Anthropic, OpenAI, etc.) are properly detected and handled.
 *
 * Run with -v for detailed logging:
 *   ./run-unit-tests.sh -v APICreditsTest
 *
 * @author Cloudempiere
 * @version 1.0
 */
@UnitTest
@DisplayName("API Credits and Billing Tests")
class APICreditsTest {

    private static final TestLogger log = new TestLogger(APICreditsTest.class);

    @BeforeEach
    void setUp(TestInfo testInfo) {
        testInfo.getTestMethod().ifPresent(m -> log.testStart(m.getName()));
    }

    // Common error patterns from various AI providers
    private static final String[] CREDIT_ERROR_PATTERNS = {
        // Anthropic
        "credit balance is too low",
        "credit_balance_too_low",
        "purchase credits",
        "Plans & Billing",
        // OpenAI
        "insufficient_quota",
        "billing_hard_limit_reached",
        "exceeded your current quota",
        "You exceeded your current quota",
        // AWS Bedrock
        "ThrottlingException",
        "ServiceQuotaExceededException",
        // Generic
        "rate limit",
        "quota exceeded",
        "billing",
        "payment required"
    };

    // ========================================================================
    // Credit Error Detection Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Credit Error Detection")
    class CreditErrorDetectionTests {

        @Test
        @UnitTest
@DisplayName("Should detect Anthropic credit balance error")
        void shouldDetectAnthropicCreditError() {
            String errorMessage = "Error: Your credit balance is too low to access the Anthropic API. " +
                "Please go to Plans & Billing to upgrade or purchase credits.";
            log.input("Error message", errorMessage);

            boolean isCredit = isCreditOrBillingError(errorMessage);
            log.output("Is credit/billing error", isCredit);
            log.var("matchedPatterns", findMatchingPatterns(errorMessage));

            assertThat(isCredit)
                .as("Should detect Anthropic credit error")
                .isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should detect OpenAI quota error")
        void shouldDetectOpenAIQuotaError() {
            String errorMessage = "You exceeded your current quota, please check your plan and billing details.";
            log.input("Error message", errorMessage);

            boolean isCredit = isCreditOrBillingError(errorMessage);
            log.output("Is credit/billing error", isCredit);
            log.var("matchedPatterns", findMatchingPatterns(errorMessage));

            assertThat(isCredit)
                .as("Should detect OpenAI quota error")
                .isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should detect generic rate limit error")
        void shouldDetectRateLimitError() {
            String errorMessage = "Rate limit exceeded. Please retry after 60 seconds.";
            log.input("Error message", errorMessage);

            boolean isCredit = isCreditOrBillingError(errorMessage);
            log.output("Is credit/billing error", isCredit);
            log.var("matchedPatterns", findMatchingPatterns(errorMessage));

            assertThat(isCredit)
                .as("Should detect rate limit error")
                .isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should NOT flag normal API errors as credit errors")
        void shouldNotFlagNormalErrors() {
            String[] normalErrors = {
                "Connection timeout",
                "Invalid API key",
                "Model not found",
                "Bad request: invalid parameters",
                "Internal server error"
            };

            for (String error : normalErrors) {
                log.input("Normal error", error);
                boolean isCredit = isCreditOrBillingError(error);
                log.output("Is credit/billing error", isCredit);

                assertThat(isCredit)
                    .as("Should not flag '%s' as credit error", error)
                    .isFalse();
            }
        }
    }

    // ========================================================================
    // User-Friendly Message Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("User-Friendly Error Messages")
    class UserFriendlyMessageTests {

        @Test
        @UnitTest
@DisplayName("Should generate user-friendly credit error message")
        void shouldGenerateUserFriendlyMessage() {
            String technicalError = "credit_balance_too_low";
            String userMessage = getUserFriendlyErrorMessage(technicalError);

            assertThat(userMessage)
                .as("User message should be helpful")
                .contains("AI service")
                .contains("administrator")
                .doesNotContain("credit_balance_too_low"); // Hide technical details
        }

        @Test
        @UnitTest
@DisplayName("Should provide different message for rate limits")
        void shouldProvideRateLimitMessage() {
            String technicalError = "Rate limit exceeded";
            String userMessage = getUserFriendlyErrorMessage(technicalError);

            assertThat(userMessage)
                .as("Rate limit message should suggest retry")
                .containsIgnoringCase("try again");
        }
    }

    // ========================================================================
    // Rate Limit Status Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Rate Limit Status Parsing")
    class RateLimitStatusTests {

        @Test
        @UnitTest
@DisplayName("Should parse Anthropic rate limit headers")
        void shouldParseRateLimitHeaders() {
            log.section("Testing Rate Limit Header Parsing");

            // Simulate Anthropic response headers
            int requestsLimit = 1000;
            int requestsRemaining = 950;
            int inputTokensLimit = 450000;
            int inputTokensRemaining = 420000;
            int outputTokensLimit = 90000;
            int outputTokensRemaining = 85000;

            log.input("Requests", requestsRemaining + "/" + requestsLimit);
            log.input("Input Tokens", inputTokensRemaining + "/" + inputTokensLimit);
            log.input("Output Tokens", outputTokensRemaining + "/" + outputTokensLimit);

            // Create and populate status
            AIRateLimitStatus status = new AIRateLimitStatus();
            status.setLimitRequests(requestsLimit);
            status.setRemainingRequests(requestsRemaining);
            status.setLimitInputTokens(inputTokensLimit);
            status.setRemainingInputTokens(inputTokensRemaining);
            status.setLimitOutputTokens(outputTokensLimit);
            status.setRemainingOutputTokens(outputTokensRemaining);

            log.output("Requests Usage %", String.format("%.1f%%", status.getRequestsUsagePercent()));
            log.output("Input Tokens Usage %", String.format("%.1f%%", status.getInputTokensUsagePercent()));
            log.output("Output Tokens Usage %", String.format("%.1f%%", status.getOutputTokensUsagePercent()));
            log.output("Approaching Limit", status.isApproachingLimit());
            log.output("Critical", status.isCritical());
            log.var("summary", status.getSummary());

            assertThat(status.getRequestsUsagePercent()).isCloseTo(5.0, within(0.1));
            assertThat(status.getInputTokensUsagePercent()).isCloseTo(6.67, within(0.1));
            assertThat(status.isApproachingLimit()).isFalse();
            assertThat(status.isCritical()).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("Should detect approaching rate limit")
        void shouldDetectApproachingLimit() {
            log.section("Testing Approaching Limit Detection");

            AIRateLimitStatus status = new AIRateLimitStatus();
            status.setLimitRequests(1000);
            status.setRemainingRequests(150); // 85% used
            status.setLimitInputTokens(450000);
            status.setRemainingInputTokens(400000);
            status.setLimitOutputTokens(90000);
            status.setRemainingOutputTokens(80000);

            log.output("Requests Usage %", String.format("%.1f%%", status.getRequestsUsagePercent()));
            log.output("Approaching Limit", status.isApproachingLimit());

            assertThat(status.getRequestsUsagePercent()).isCloseTo(85.0, within(0.1));
            assertThat(status.isApproachingLimit()).isTrue();
            assertThat(status.isCritical()).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("Should detect critical rate limit")
        void shouldDetectCriticalLimit() {
            log.section("Testing Critical Limit Detection");

            AIRateLimitStatus status = new AIRateLimitStatus();
            status.setLimitRequests(1000);
            status.setRemainingRequests(30); // 97% used
            status.setLimitInputTokens(450000);
            status.setRemainingInputTokens(400000);
            status.setLimitOutputTokens(90000);
            status.setRemainingOutputTokens(80000);

            log.output("Requests Usage %", String.format("%.1f%%", status.getRequestsUsagePercent()));
            log.output("Critical", status.isCritical());

            assertThat(status.getRequestsUsagePercent()).isCloseTo(97.0, within(0.1));
            assertThat(status.isCritical()).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should format summary for display")
        void shouldFormatSummary() {
            log.section("Testing Summary Formatting");

            AIRateLimitStatus status = new AIRateLimitStatus();
            status.setLimitRequests(1000);
            status.setRemainingRequests(900);
            status.setLimitInputTokens(450000);
            status.setRemainingInputTokens(405000);
            status.setLimitOutputTokens(90000);
            status.setRemainingOutputTokens(81000);
            status.setCreditStatus("ok");

            String summary = status.getSummary();
            log.output("Summary", summary);

            assertThat(summary).contains("Requests:");
            assertThat(summary).contains("Input:");
            assertThat(summary).contains("Output:");
            assertThat(summary).contains("Credits: ok");
        }
    }

    // ========================================================================
    // Error Classification Tests
    // ========================================================================

    @Nested
    @UnitTest
@DisplayName("Error Classification")
    class ErrorClassificationTests {

        @Test
        @UnitTest
@DisplayName("Should classify billing errors as recoverable by admin")
        void shouldClassifyBillingAsAdminRecoverable() {
            String error = "Your credit balance is too low";
            ErrorClassification classification = classifyError(error);

            assertThat(classification.isRecoverable()).isTrue();
            assertThat(classification.requiresAdminAction()).isTrue();
            assertThat(classification.isUserRetryable()).isFalse();
        }

        @Test
        @UnitTest
@DisplayName("Should classify rate limits as user retryable")
        void shouldClassifyRateLimitAsRetryable() {
            String error = "Rate limit exceeded";
            ErrorClassification classification = classifyError(error);

            assertThat(classification.isRecoverable()).isTrue();
            assertThat(classification.isUserRetryable()).isTrue();
        }

        @Test
        @UnitTest
@DisplayName("Should classify connection errors as transient")
        void shouldClassifyConnectionAsTransient() {
            String error = "Connection timeout";
            ErrorClassification classification = classifyError(error);

            assertThat(classification.isTransient()).isTrue();
            assertThat(classification.isUserRetryable()).isTrue();
        }
    }

    // ========================================================================
    // Helper Methods (These would typically be in a utility class)
    // ========================================================================

    /**
     * Check if error message indicates a credit/billing issue
     * @param errorMessage the error message to check
     * @return true if this is a credit/billing related error
     */
    private boolean isCreditOrBillingError(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return false;
        }

        String lowerMessage = errorMessage.toLowerCase();

        for (String pattern : CREDIT_ERROR_PATTERNS) {
            if (lowerMessage.contains(pattern.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find which patterns match (for verbose logging)
     */
    private String findMatchingPatterns(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "none";
        }

        StringBuilder matches = new StringBuilder();
        String lowerMessage = errorMessage.toLowerCase();

        for (String pattern : CREDIT_ERROR_PATTERNS) {
            if (lowerMessage.contains(pattern.toLowerCase())) {
                if (matches.length() > 0) matches.append(", ");
                matches.append("'").append(pattern).append("'");
            }
        }

        return matches.length() > 0 ? matches.toString() : "none";
    }

    /**
     * Convert technical error to user-friendly message
     * @param technicalError the technical error
     * @return user-friendly message
     */
    private String getUserFriendlyErrorMessage(String technicalError) {
        if (isCreditOrBillingError(technicalError)) {
            if (technicalError.toLowerCase().contains("rate limit")) {
                return "The AI service is temporarily busy. Please try again in a moment.";
            }
            return "The AI service is currently unavailable due to a configuration issue. " +
                   "Please contact your system administrator.";
        }

        if (technicalError.toLowerCase().contains("timeout") ||
            technicalError.toLowerCase().contains("connection")) {
            return "Could not connect to AI service. Please try again later.";
        }

        return "An error occurred while communicating with the AI service. " +
               "Please try again or contact support if the issue persists.";
    }

    /**
     * Classify an error for appropriate handling
     * @param errorMessage the error message
     * @return error classification
     */
    private ErrorClassification classifyError(String errorMessage) {
        ErrorClassification classification = new ErrorClassification();

        if (errorMessage == null || errorMessage.isEmpty()) {
            classification.setUnknown(true);
            return classification;
        }

        String lower = errorMessage.toLowerCase();

        // Credit/billing errors - admin must fix
        if (lower.contains("credit") || lower.contains("billing") ||
            lower.contains("quota") || lower.contains("purchase")) {
            classification.setRecoverable(true);
            classification.setRequiresAdminAction(true);
            classification.setUserRetryable(false);
            return classification;
        }

        // Rate limits - user can retry
        if (lower.contains("rate limit") || lower.contains("throttl")) {
            classification.setRecoverable(true);
            classification.setUserRetryable(true);
            classification.setSuggestedRetryDelayMs(60000); // 1 minute
            return classification;
        }

        // Connection issues - transient, retry
        if (lower.contains("timeout") || lower.contains("connection") ||
            lower.contains("network")) {
            classification.setTransient(true);
            classification.setRecoverable(true);
            classification.setUserRetryable(true);
            classification.setSuggestedRetryDelayMs(5000); // 5 seconds
            return classification;
        }

        // Default - unknown error
        classification.setRecoverable(false);
        return classification;
    }

    /**
     * Helper class for error classification
     */
    private static class ErrorClassification {
        private boolean recoverable;
        private boolean requiresAdminAction;
        private boolean userRetryable;
        private boolean transient_;
        private boolean unknown;
        private long suggestedRetryDelayMs;

        public boolean isRecoverable() { return recoverable; }
        public void setRecoverable(boolean recoverable) { this.recoverable = recoverable; }

        public boolean requiresAdminAction() { return requiresAdminAction; }
        public void setRequiresAdminAction(boolean requiresAdminAction) {
            this.requiresAdminAction = requiresAdminAction;
        }

        public boolean isUserRetryable() { return userRetryable; }
        public void setUserRetryable(boolean userRetryable) { this.userRetryable = userRetryable; }

        public boolean isTransient() { return transient_; }
        public void setTransient(boolean transient_) { this.transient_ = transient_; }

        public boolean isUnknown() { return unknown; }
        public void setUnknown(boolean unknown) { this.unknown = unknown; }

        public long getSuggestedRetryDelayMs() { return suggestedRetryDelayMs; }
        public void setSuggestedRetryDelayMs(long delay) { this.suggestedRetryDelayMs = delay; }
    }
}
