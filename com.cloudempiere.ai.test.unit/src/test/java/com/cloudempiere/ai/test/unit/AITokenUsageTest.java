package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.provider.dto.AITokenUsage;

/**
 * Unit tests for {@link AITokenUsage} class.
 *
 * <p>Tests the token usage DTO without any external dependencies.
 *
 * @author Cloudempiere
 */
@DisplayName("AITokenUsage Unit Tests")
class AITokenUsageTest {

    private AITokenUsage tokenUsage;

    @BeforeEach
    void setUp() {
        tokenUsage = new AITokenUsage();
    }

    @Test
    @DisplayName("New instance should have zero values")
    void newInstance_shouldHaveZeroValues() {
        assertThat(tokenUsage.getPromptTokens()).isZero();
        assertThat(tokenUsage.getCompletionTokens()).isZero();
        assertThat(tokenUsage.getTotalTokens()).isZero();
    }

    @Test
    @DisplayName("setPromptTokens should store prompt token count")
    void setPromptTokens_shouldStorePromptTokenCount() {
        tokenUsage.setPromptTokens(150);

        assertThat(tokenUsage.getPromptTokens()).isEqualTo(150);
    }

    @Test
    @DisplayName("setCompletionTokens should store completion token count")
    void setCompletionTokens_shouldStoreCompletionTokenCount() {
        tokenUsage.setCompletionTokens(200);

        assertThat(tokenUsage.getCompletionTokens()).isEqualTo(200);
    }

    @Test
    @DisplayName("setTotalTokens should store total token count")
    void setTotalTokens_shouldStoreTotalTokenCount() {
        tokenUsage.setTotalTokens(350);

        assertThat(tokenUsage.getTotalTokens()).isEqualTo(350);
    }

    @Test
    @DisplayName("Token counts should be independently settable")
    void tokenCounts_shouldBeIndependentlySettable() {
        tokenUsage.setPromptTokens(100);
        tokenUsage.setCompletionTokens(250);
        tokenUsage.setTotalTokens(350);

        assertThat(tokenUsage.getPromptTokens()).isEqualTo(100);
        assertThat(tokenUsage.getCompletionTokens()).isEqualTo(250);
        assertThat(tokenUsage.getTotalTokens()).isEqualTo(350);
    }

    @Test
    @DisplayName("Token counts can be updated")
    void tokenCounts_canBeUpdated() {
        tokenUsage.setPromptTokens(100);
        tokenUsage.setPromptTokens(200);

        assertThat(tokenUsage.getPromptTokens()).isEqualTo(200);
    }

    @Test
    @DisplayName("Large token values should be supported")
    void largeTokenValues_shouldBeSupported() {
        // Claude 3 supports up to 200K tokens input
        tokenUsage.setPromptTokens(200000);
        tokenUsage.setCompletionTokens(8192);
        tokenUsage.setTotalTokens(208192);

        assertThat(tokenUsage.getPromptTokens()).isEqualTo(200000);
        assertThat(tokenUsage.getCompletionTokens()).isEqualTo(8192);
        assertThat(tokenUsage.getTotalTokens()).isEqualTo(208192);
    }
}
