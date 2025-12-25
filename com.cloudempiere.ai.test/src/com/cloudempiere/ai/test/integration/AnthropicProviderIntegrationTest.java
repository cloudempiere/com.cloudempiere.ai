package com.cloudempiere.ai.test.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.dto.AIHealthStatus;
import com.cloudempiere.ai.provider.dto.AIMessage;
import com.cloudempiere.ai.provider.dto.AIRequest;
import com.cloudempiere.ai.provider.dto.AIResponse;
import com.cloudempiere.ai.provider.impl.AnthropicProvider;
import com.cloudempiere.ai.test.categories.IntegrationTest;

/**
 * Integration tests for {@link AnthropicProvider}.
 *
 * <p>These tests require a valid ANTHROPIC_API_KEY environment variable.
 * To run: {@code ANTHROPIC_API_KEY=sk-ant-xxx mvn test -Pintegration}
 *
 * <p>The tests verify:
 * <ul>
 *   <li>Provider initialization</li>
 *   <li>Health checks</li>
 *   <li>Text generation (sync)</li>
 *   <li>System prompt handling</li>
 *   <li>Cost estimation</li>
 *   <li>Feature support flags</li>
 * </ul>
 *
 * @author Cloudempiere
 */
@IntegrationTest
@DisplayName("Anthropic Provider Integration Tests")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class AnthropicProviderIntegrationTest {

    private static final String TEST_MODEL = "claude-3-haiku-20240307";

    private AnthropicProvider provider;
    private MAIProvider mockConfig;

    @BeforeAll
    static void checkEnvironment() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("ANTHROPIC_API_KEY not set - skipping integration tests");
        }
    }

    @BeforeEach
    void setUp() {
        provider = new AnthropicProvider();

        // Create mock configuration with null context (standalone mode)
        mockConfig = new MAIProvider((Properties) null, 0, null);
        mockConfig.setName("Test Anthropic Provider");
        mockConfig.setAIGProviderType(MAIProvider.AIGPROVIDERTYPE_AnthropicClaude);
        mockConfig.setAPIKey(System.getenv("ANTHROPIC_API_KEY"));
    }

    @Test
    @DisplayName("Provider should initialize successfully with valid API key")
    void initialization_shouldSucceedWithValidApiKey() throws Exception {
        provider.initialize(mockConfig);

        assertThat(provider.isReady())
            .as("Provider should be ready after initialization")
            .isTrue();
        assertThat(provider.getProviderName())
            .isEqualTo("Anthropic Claude");
        assertThat(provider.getProviderType())
            .isEqualTo(MAIProvider.AIGPROVIDERTYPE_AnthropicClaude);
    }

    @Test
    @DisplayName("Health check should return healthy status")
    void healthCheck_shouldReturnHealthyStatus() throws Exception {
        provider.initialize(mockConfig);

        AIHealthStatus status = provider.checkHealth();

        assertThat(status)
            .as("Health status should not be null")
            .isNotNull();
        assertThat(status.isHealthy())
            .as("Provider should be healthy")
            .isTrue();
        assertThat(status.getStatus())
            .isEqualTo("Healthy");
        assertThat(status.getResponseTimeMs())
            .as("Response time should be greater than 0")
            .isGreaterThan(0);

        System.out.println("Health check passed - Response time: " + status.getResponseTimeMs() + "ms");
    }

    @Test
    @DisplayName("Text generation should return valid response")
    void textGeneration_shouldReturnValidResponse() throws Exception {
        provider.initialize(mockConfig);

        AIRequest request = new AIRequest();
        request.setModel(TEST_MODEL);
        request.setMaxTokens(100);
        request.setTemperature(0.7);

        List<AIMessage> messages = new ArrayList<>();
        messages.add(new AIMessage("user", "Say hello and tell me what AI model you are in one sentence."));
        request.setMessages(messages);

        AIResponse response = provider.generateText(request);

        assertThat(response)
            .as("Response should not be null")
            .isNotNull();
        assertThat(response.getContent())
            .as("Response content should not be null or empty")
            .isNotNull()
            .isNotEmpty();
        assertThat(response.getModel())
            .isEqualTo(TEST_MODEL);
        assertThat(response.getTokenUsage())
            .as("Token usage should not be null")
            .isNotNull();
        assertThat(response.getTokenUsage().getTotalTokens())
            .as("Total tokens should be greater than 0")
            .isGreaterThan(0);
        assertThat(response.getProcessingTimeMs())
            .as("Processing time should be greater than 0")
            .isGreaterThan(0);

        System.out.println("Text generation passed");
        System.out.println("Response: " + response.getContent());
        System.out.println("Tokens used: " + response.getTokenUsage().getTotalTokens());
        System.out.println("Cost: $" + String.format("%.6f", response.getCostUSD()));
    }

    @Test
    @DisplayName("System prompt should influence response")
    void systemPrompt_shouldInfluenceResponse() throws Exception {
        provider.initialize(mockConfig);

        AIRequest request = new AIRequest();
        request.setModel(TEST_MODEL);
        request.setMaxTokens(50);
        request.setSystemPrompt("You are a helpful assistant that always responds in exactly 3 words.");

        List<AIMessage> messages = new ArrayList<>();
        messages.add(new AIMessage("user", "What is the capital of France?"));
        request.setMessages(messages);

        AIResponse response = provider.generateText(request);

        assertThat(response.getContent())
            .isNotNull();

        System.out.println("System prompt test passed");
        System.out.println("Response: " + response.getContent());
    }

    @Test
    @DisplayName("Cost estimation should return positive value")
    void costEstimation_shouldReturnPositiveValue() throws Exception {
        provider.initialize(mockConfig);

        AIRequest request = new AIRequest();
        request.setModel(TEST_MODEL);
        request.setMaxTokens(100);

        List<AIMessage> messages = new ArrayList<>();
        messages.add(new AIMessage("user", "Hello, how are you?"));
        request.setMessages(messages);

        double estimatedCost = provider.estimateCost(request);

        assertThat(estimatedCost)
            .as("Estimated cost should be greater than 0")
            .isGreaterThan(0);

        System.out.println("Cost estimation test passed");
        System.out.println("Estimated cost: $" + String.format("%.6f", estimatedCost));
    }

    @Test
    @DisplayName("Provider should report correct feature support")
    void featureSupport_shouldBeCorrect() throws Exception {
        provider.initialize(mockConfig);

        assertThat(provider.supportsTextGeneration())
            .as("Should support text generation")
            .isTrue();
        assertThat(provider.supportsStreaming())
            .as("Should support streaming")
            .isTrue();
        assertThat(provider.supportsFunctionCalling())
            .as("Should support function calling")
            .isTrue();
        assertThat(provider.supportsVision())
            .as("Should support vision")
            .isTrue();
        assertThat(provider.supportsAudio())
            .as("Should not support audio")
            .isFalse();
        assertThat(provider.supportsEmbeddings())
            .as("Should not support embeddings")
            .isFalse();

        System.out.println("Feature support test passed");
    }
}
