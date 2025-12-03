package com.cloudempiere.ai.provider.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cloudempiere.ai.model.MAIProvider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/**
 * Unit tests for LangChain4jProviderFactory.
 *
 * Tests provider creation for all supported provider types:
 * - Anthropic Claude
 * - AWS Bedrock
 * - Ollama (local LLM)
 * - OpenAI
 *
 * @author Cloudempiere
 * @version 0.9.0
 */
@DisplayName("LangChain4jProviderFactory Tests")
class LangChain4jProviderFactoryTest {

    private MAIProvider mockConfig;

    @BeforeEach
    void setUp() {
        mockConfig = mock(MAIProvider.class);
    }

    @Nested
    @DisplayName("Provider Type Detection")
    class ProviderTypeDetection {

        @Test
        @DisplayName("Should identify Anthropic provider type")
        void shouldIdentifyAnthropicProvider() {
            assertThat(LangChain4jProviderFactory.PROVIDER_ANTHROPIC).isEqualTo("ANT");
        }

        @Test
        @DisplayName("Should identify Bedrock provider type")
        void shouldIdentifyBedrockProvider() {
            assertThat(LangChain4jProviderFactory.PROVIDER_BEDROCK).isEqualTo("ABE");
        }

        @Test
        @DisplayName("Should identify Ollama provider type")
        void shouldIdentifyOllamaProvider() {
            assertThat(LangChain4jProviderFactory.PROVIDER_OLLAMA).isEqualTo("OLL");
        }

        @Test
        @DisplayName("Should identify OpenAI provider type")
        void shouldIdentifyOpenAiProvider() {
            assertThat(LangChain4jProviderFactory.PROVIDER_OPENAI).isEqualTo("OAI");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw exception for unknown provider type")
        void shouldThrowExceptionForUnknownProvider() {
            when(mockConfig.getAIGProviderType()).thenReturn("UNKNOWN");
            when(mockConfig.getAPIKey()).thenReturn("test-key");

            assertThatThrownBy(() -> LangChain4jProviderFactory.create(mockConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown provider type");
        }

        @Test
        @DisplayName("Should throw exception for null provider type")
        void shouldThrowExceptionForNullProvider() {
            when(mockConfig.getAIGProviderType()).thenReturn(null);
            when(mockConfig.getAPIKey()).thenReturn("test-key");

            assertThatThrownBy(() -> LangChain4jProviderFactory.create(mockConfig))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Cache Management")
    class CacheManagement {

        @Test
        @DisplayName("Should clear cache without error")
        void shouldClearCacheWithoutError() {
            // Should not throw
            LangChain4jProviderFactory.clearCache();
        }

        @Test
        @DisplayName("Should evict specific provider from cache")
        void shouldEvictFromCache() {
            // Should not throw
            LangChain4jProviderFactory.evictFromCache(12345);
        }
    }

    @Nested
    @DisplayName("Anthropic Provider Creation")
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    class AnthropicProviderCreation {

        @Test
        @DisplayName("Should create Anthropic ChatLanguageModel with API key")
        void shouldCreateAnthropicModel() {
            when(mockConfig.getAIGProviderType()).thenReturn(LangChain4jProviderFactory.PROVIDER_ANTHROPIC);
            when(mockConfig.getAPIKey()).thenReturn(System.getenv("ANTHROPIC_API_KEY"));

            ChatLanguageModel model = LangChain4jProviderFactory.create(mockConfig);

            assertThat(model).isNotNull();
        }

        @Test
        @DisplayName("Should create Anthropic model with custom model name")
        void shouldCreateAnthropicModelWithCustomName() {
            when(mockConfig.getAIGProviderType()).thenReturn(LangChain4jProviderFactory.PROVIDER_ANTHROPIC);
            when(mockConfig.getAPIKey()).thenReturn(System.getenv("ANTHROPIC_API_KEY"));

            ChatLanguageModel model = LangChain4jProviderFactory.create(
                mockConfig, "claude-3-haiku-20240307", null);

            assertThat(model).isNotNull();
        }

        @Test
        @DisplayName("Should create streaming Anthropic model")
        void shouldCreateStreamingAnthropicModel() {
            when(mockConfig.getAIGProviderType()).thenReturn(LangChain4jProviderFactory.PROVIDER_ANTHROPIC);
            when(mockConfig.getAPIKey()).thenReturn(System.getenv("ANTHROPIC_API_KEY"));

            StreamingChatLanguageModel model = LangChain4jProviderFactory.createStreaming(
                mockConfig, null, null);

            assertThat(model).isNotNull();
        }
    }

    @Nested
    @DisplayName("Ollama Provider Creation")
    class OllamaProviderCreation {

        @Test
        @DisplayName("Should create Ollama model with default URL")
        void shouldCreateOllamaModelWithDefaultUrl() {
            when(mockConfig.getAIGProviderType()).thenReturn(LangChain4jProviderFactory.PROVIDER_OLLAMA);
            when(mockConfig.getAPIKey()).thenReturn(null);

            // This will create the model but connection may fail if Ollama isn't running
            ChatLanguageModel model = LangChain4jProviderFactory.create(mockConfig);

            assertThat(model).isNotNull();
        }

        @Test
        @DisplayName("Should create Ollama model with custom URL and model")
        void shouldCreateOllamaModelWithCustomConfig() {
            when(mockConfig.getAIGProviderType()).thenReturn(LangChain4jProviderFactory.PROVIDER_OLLAMA);
            when(mockConfig.getAPIKey()).thenReturn(null);

            ChatLanguageModel model = LangChain4jProviderFactory.create(
                mockConfig, "llama3.2", "http://localhost:11434");

            assertThat(model).isNotNull();
        }
    }

    @Nested
    @DisplayName("OpenAI Provider Creation")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    class OpenAiProviderCreation {

        @Test
        @DisplayName("Should create OpenAI ChatLanguageModel")
        void shouldCreateOpenAiModel() {
            when(mockConfig.getAIGProviderType()).thenReturn(LangChain4jProviderFactory.PROVIDER_OPENAI);
            when(mockConfig.getAPIKey()).thenReturn(System.getenv("OPENAI_API_KEY"));

            ChatLanguageModel model = LangChain4jProviderFactory.create(mockConfig);

            assertThat(model).isNotNull();
        }
    }

    @Nested
    @DisplayName("Bedrock Provider Creation")
    @EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY_ID", matches = ".+")
    class BedrockProviderCreation {

        @Test
        @DisplayName("Should create Bedrock ChatLanguageModel")
        void shouldCreateBedrockModel() {
            when(mockConfig.getAIGProviderType()).thenReturn(LangChain4jProviderFactory.PROVIDER_BEDROCK);
            when(mockConfig.getAPIKey()).thenReturn(null); // Bedrock uses IAM

            ChatLanguageModel model = LangChain4jProviderFactory.create(mockConfig);

            assertThat(model).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "anthropic.claude-3-5-sonnet-20241022-v2:0",
            "us.amazon.nova-lite-v1:0",
            "us.amazon.nova-micro-v1:0"
        })
        @DisplayName("Should create Bedrock model for different model IDs")
        void shouldCreateBedrockModelForDifferentIds(String modelId) {
            when(mockConfig.getAIGProviderType()).thenReturn(LangChain4jProviderFactory.PROVIDER_BEDROCK);
            when(mockConfig.getAPIKey()).thenReturn(null);

            ChatLanguageModel model = LangChain4jProviderFactory.create(
                mockConfig, modelId, "us-east-1");

            assertThat(model).isNotNull();
        }
    }
}
