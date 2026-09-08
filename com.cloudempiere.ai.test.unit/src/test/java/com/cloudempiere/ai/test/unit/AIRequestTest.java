package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.provider.dto.AIRequest;

/**
 * Unit tests for {@link AIRequest} class.
 *
 * <p>Tests the request DTO, builder pattern, and message handling
 * without any external dependencies.
 *
 * @author Cloudempiere
 */
@DisplayName("AIRequest Unit Tests")
class AIRequestTest {

    private AIRequest request;

    @BeforeEach
    void setUp() {
        request = new AIRequest();
    }

    @Test
    @DisplayName("New request should have empty messages list")
    void newRequest_shouldHaveEmptyMessagesList() {
        assertThat(request.getMessages())
            .isNotNull()
            .isEmpty();
    }

    @Test
    @DisplayName("New request should have empty context map")
    void newRequest_shouldHaveEmptyContextMap() {
        assertThat(request.getContext())
            .isNotNull()
            .isEmpty();
    }

    @Test
    @DisplayName("New request should have empty provider params map")
    void newRequest_shouldHaveEmptyProviderParamsMap() {
        assertThat(request.getProviderParams())
            .isNotNull()
            .isEmpty();
    }

    @Test
    @DisplayName("addUserMessage should add message with user role")
    void addUserMessage_shouldAddMessageWithUserRole() {
        request.addUserMessage("Hello, world!");

        assertThat(request.getMessages())
            .hasSize(1);
        assertThat(request.getMessages().get(0).getRole())
            .isEqualTo("user");
        assertThat(request.getMessages().get(0).getContent())
            .isEqualTo("Hello, world!");
    }

    @Test
    @DisplayName("addAssistantMessage should add message with assistant role")
    void addAssistantMessage_shouldAddMessageWithAssistantRole() {
        request.addAssistantMessage("I'm an AI assistant.");

        assertThat(request.getMessages())
            .hasSize(1);
        assertThat(request.getMessages().get(0).getRole())
            .isEqualTo("assistant");
        assertThat(request.getMessages().get(0).getContent())
            .isEqualTo("I'm an AI assistant.");
    }

    @Test
    @DisplayName("addSystemMessage should add message with system role")
    void addSystemMessage_shouldAddMessageWithSystemRole() {
        request.addSystemMessage("You are a helpful assistant.");

        assertThat(request.getMessages())
            .hasSize(1);
        assertThat(request.getMessages().get(0).getRole())
            .isEqualTo("system");
        assertThat(request.getMessages().get(0).getContent())
            .isEqualTo("You are a helpful assistant.");
    }

    @Test
    @DisplayName("add methods should be chainable")
    void addMethods_shouldBeChainable() {
        request
            .addSystemMessage("System prompt")
            .addUserMessage("User question")
            .addAssistantMessage("AI response")
            .addUserMessage("Follow-up");

        assertThat(request.getMessages())
            .hasSize(4);
    }

    @Test
    @DisplayName("Builder should create request with model")
    void builder_shouldCreateRequestWithModel() {
        AIRequest builtRequest = new AIRequest.Builder()
            .model("claude-3-sonnet-20240229")
            .build();

        assertThat(builtRequest.getModel())
            .isEqualTo("claude-3-sonnet-20240229");
    }

    @Test
    @DisplayName("Builder should create request with system prompt")
    void builder_shouldCreateRequestWithSystemPrompt() {
        AIRequest builtRequest = new AIRequest.Builder()
            .systemPrompt("You are an iDempiere expert.")
            .build();

        assertThat(builtRequest.getSystemPrompt())
            .isEqualTo("You are an iDempiere expert.");
    }

    @Test
    @DisplayName("Builder should create request with temperature")
    void builder_shouldCreateRequestWithTemperature() {
        AIRequest builtRequest = new AIRequest.Builder()
            .temperature(0.7)
            .build();

        assertThat(builtRequest.getTemperature())
            .isEqualTo(0.7);
    }

    @Test
    @DisplayName("Builder should create request with maxTokens")
    void builder_shouldCreateRequestWithMaxTokens() {
        AIRequest builtRequest = new AIRequest.Builder()
            .maxTokens(1024)
            .build();

        assertThat(builtRequest.getMaxTokens())
            .isEqualTo(1024);
    }

    @Test
    @DisplayName("Builder should create request with topP")
    void builder_shouldCreateRequestWithTopP() {
        AIRequest builtRequest = new AIRequest.Builder()
            .topP(0.9)
            .build();

        assertThat(builtRequest.getTopP())
            .isEqualTo(0.9);
    }

    @Test
    @DisplayName("Builder should create request with frequencyPenalty")
    void builder_shouldCreateRequestWithFrequencyPenalty() {
        AIRequest builtRequest = new AIRequest.Builder()
            .frequencyPenalty(0.5)
            .build();

        assertThat(builtRequest.getFrequencyPenalty())
            .isEqualTo(0.5);
    }

    @Test
    @DisplayName("Builder should create request with presencePenalty")
    void builder_shouldCreateRequestWithPresencePenalty() {
        AIRequest builtRequest = new AIRequest.Builder()
            .presencePenalty(0.3)
            .build();

        assertThat(builtRequest.getPresencePenalty())
            .isEqualTo(0.3);
    }

    @Test
    @DisplayName("Builder should create request with timeout")
    void builder_shouldCreateRequestWithTimeout() {
        AIRequest builtRequest = new AIRequest.Builder()
            .timeoutMs(30000)
            .build();

        assertThat(builtRequest.getTimeoutMs())
            .isEqualTo(30000);
    }

    @Test
    @DisplayName("Builder should create request with messages")
    void builder_shouldCreateRequestWithMessages() {
        AIRequest builtRequest = new AIRequest.Builder()
            .userMessage("What is the capital of France?")
            .assistantMessage("The capital of France is Paris.")
            .userMessage("Thanks!")
            .build();

        assertThat(builtRequest.getMessages())
            .hasSize(3);
    }

    @Test
    @DisplayName("Builder should support full fluent configuration")
    void builder_shouldSupportFullFluentConfiguration() {
        AIRequest builtRequest = new AIRequest.Builder()
            .model("claude-3-haiku-20240307")
            .systemPrompt("You are a helpful ERP assistant.")
            .userMessage("Help me with sales orders.")
            .temperature(0.5)
            .maxTokens(500)
            .topP(0.95)
            .timeoutMs(60000)
            .build();

        assertThat(builtRequest.getModel()).isEqualTo("claude-3-haiku-20240307");
        assertThat(builtRequest.getSystemPrompt()).isEqualTo("You are a helpful ERP assistant.");
        assertThat(builtRequest.getMessages()).hasSize(1);
        assertThat(builtRequest.getTemperature()).isEqualTo(0.5);
        assertThat(builtRequest.getMaxTokens()).isEqualTo(500);
        assertThat(builtRequest.getTopP()).isEqualTo(0.95);
        assertThat(builtRequest.getTimeoutMs()).isEqualTo(60000);
    }

    @Test
    @DisplayName("setModel should update model")
    void setModel_shouldUpdateModel() {
        request.setModel("gpt-4");
        assertThat(request.getModel()).isEqualTo("gpt-4");

        request.setModel("claude-3-opus");
        assertThat(request.getModel()).isEqualTo("claude-3-opus");
    }

    @Test
    @DisplayName("setStopSequences should store sequences")
    void setStopSequences_shouldStoreSequences() {
        request.setStopSequences(java.util.Arrays.asList("END", "STOP", "---"));

        assertThat(request.getStopSequences())
            .containsExactly("END", "STOP", "---");
    }
}
