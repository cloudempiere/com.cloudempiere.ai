package com.cloudempiere.ai.provider.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudempiere.ai.model.MAIProvider;

/**
 * Unit tests for IDempiereAIService.
 *
 * Tests the main AI service facade including:
 * - Singleton pattern
 * - Chat and execute methods
 * - Memory management
 * - Cache management
 *
 * @author CloudEmpiere
 * @version 0.9.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IDempiereAIService Tests")
class IDempiereAIServiceTest {

    @Mock
    private MAIProvider mockProvider;

    @Mock
    private Properties mockCtx;

    private IDempiereAIService service;

    @BeforeEach
    void setUp() {
        service = IDempiereAIService.getInstance();
        // Clear caches before each test
        service.clearAllCaches();
    }

    @Nested
    @DisplayName("Singleton Pattern")
    class SingletonPattern {

        @Test
        @DisplayName("Should return same instance on multiple calls")
        void shouldReturnSameInstance() {
            IDempiereAIService instance1 = IDempiereAIService.getInstance();
            IDempiereAIService instance2 = IDempiereAIService.getInstance();

            assertThat(instance1).isSameAs(instance2);
        }

        @Test
        @DisplayName("Instance should not be null")
        void instanceShouldNotBeNull() {
            IDempiereAIService instance = IDempiereAIService.getInstance();
            assertThat(instance).isNotNull();
        }
    }

    @Nested
    @DisplayName("Memory Management")
    class MemoryManagement {

        @Test
        @DisplayName("Should return 0 for non-existent session")
        void shouldReturnZeroForNonExistentSession() {
            int size = service.getMemorySize("non-existent-session");
            assertThat(size).isZero();
        }

        @Test
        @DisplayName("Should clear memory for session")
        void shouldClearMemoryForSession() {
            String sessionId = "test-session-" + UUID.randomUUID();

            // Clear and verify
            service.clearMemory(sessionId);
            assertThat(service.getMemorySize(sessionId)).isZero();
        }

        @Test
        @DisplayName("Should clear all caches without error")
        void shouldClearAllCaches() {
            // Should not throw
            service.clearAllCaches();
        }
    }

    @Nested
    @DisplayName("Chat Method")
    class ChatMethod {

        @Test
        @DisplayName("Should throw exception when provider config is invalid")
        void shouldThrowWhenProviderInvalid() {
            when(mockProvider.getAIGProviderType()).thenReturn("INVALID");
            when(mockProvider.getAIG_Provider_ID()).thenReturn(1);

            assertThatThrownBy(() ->
                service.chat(mockProvider, mockCtx, "session-1", "Hello")
            ).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Execute Method")
    class ExecuteMethod {

        @Test
        @DisplayName("Should throw exception when provider config is invalid")
        void shouldThrowWhenProviderInvalid() {
            when(mockProvider.getAIGProviderType()).thenReturn("INVALID");

            assertThatThrownBy(() ->
                service.execute(mockProvider, mockCtx, "List all orders")
            ).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Integration Tests with Real Provider")
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    class IntegrationTests {

        @BeforeEach
        void setUpProvider() {
            when(mockProvider.getAIGProviderType()).thenReturn(LangChain4jProviderFactory.PROVIDER_ANTHROPIC);
            when(mockProvider.getAPIKey()).thenReturn(System.getenv("ANTHROPIC_API_KEY"));
            when(mockProvider.getAIG_Provider_ID()).thenReturn(1);
        }

        @Test
        @DisplayName("Should chat with AI and get response")
        void shouldChatWithAI() {
            String sessionId = "integration-test-" + UUID.randomUUID();

            String response = service.chat(mockProvider, mockCtx, sessionId, "Say hello in one word");

            assertThat(response).isNotNull();
            assertThat(response).isNotEmpty();

            // Clean up
            service.clearMemory(sessionId);
        }

        @Test
        @DisplayName("Should maintain conversation memory across messages")
        void shouldMaintainConversationMemory() {
            String sessionId = "memory-test-" + UUID.randomUUID();

            // First message
            service.chat(mockProvider, mockCtx, sessionId, "My name is TestUser");

            // Memory should now have messages
            assertThat(service.getMemorySize(sessionId)).isGreaterThan(0);

            // Clean up
            service.clearMemory(sessionId);
        }

        @Test
        @DisplayName("Should execute one-shot task")
        void shouldExecuteOneShotTask() {
            String response = service.execute(mockProvider, mockCtx, "What is 2+2? Answer with just the number.");

            assertThat(response).isNotNull();
            assertThat(response).contains("4");
        }
    }

    @Nested
    @DisplayName("Ollama Integration Tests")
    @EnabledIfEnvironmentVariable(named = "OLLAMA_ENABLED", matches = "true")
    class OllamaIntegrationTests {

        @BeforeEach
        void setUpProvider() {
            when(mockProvider.getAIGProviderType()).thenReturn(LangChain4jProviderFactory.PROVIDER_OLLAMA);
            when(mockProvider.getAPIKey()).thenReturn(null);
            when(mockProvider.getAIG_Provider_ID()).thenReturn(2);
        }

        @Test
        @DisplayName("Should chat with local Ollama model")
        void shouldChatWithOllama() {
            String sessionId = "ollama-test-" + UUID.randomUUID();

            String response = service.chat(mockProvider, mockCtx, sessionId, "Say hello");

            assertThat(response).isNotNull();

            service.clearMemory(sessionId);
        }
    }
}
