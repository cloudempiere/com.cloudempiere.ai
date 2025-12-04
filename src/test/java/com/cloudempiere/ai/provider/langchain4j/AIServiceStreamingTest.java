package com.cloudempiere.ai.provider.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.cloudempiere.ai.provider.dto.AIStreamCallback;

import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;

/**
 * Unit and integration tests for AIService streaming functionality.
 *
 * Tests ADR-033: Streaming Responses and Thinking Timeline UX
 *
 * @author Cloudempiere
 * @version 1.0
 */
@DisplayName("AIService Streaming Tests")
class AIServiceStreamingTest {

    // ========================================================================
    // AIStreamCallback Tests
    // ========================================================================

    @Nested
    @DisplayName("AIStreamCallback Interface")
    class StreamCallbackTests {

        @Test
        @DisplayName("Simple callback should handle basic events")
        void simpleCallbackShouldWork() {
            AtomicReference<String> receivedChunk = new AtomicReference<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Exception> receivedError = new AtomicReference<>();

            AIStreamCallback callback = AIStreamCallback.simple(
                receivedChunk::set,
                () -> completed.set(true),
                receivedError::set
            );

            // Test onChunk
            callback.onChunk("Hello");
            assertThat(receivedChunk.get()).isEqualTo("Hello");

            // Test onComplete
            callback.onComplete();
            assertThat(completed.get()).isTrue();

            // Test onError
            Exception testError = new RuntimeException("Test error");
            callback.onError(testError);
            assertThat(receivedError.get()).isEqualTo(testError);
        }

        @Test
        @DisplayName("Builder callback should handle all events")
        void builderCallbackShouldWork() {
            List<String> chunks = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Exception> error = new AtomicReference<>();
            AtomicReference<String> toolStarted = new AtomicReference<>();
            AtomicReference<String> toolCompleted = new AtomicReference<>();
            AtomicReference<String> toolError = new AtomicReference<>();
            List<String> thinkingChunks = new ArrayList<>();
            AtomicBoolean thinkingCompleted = new AtomicBoolean(false);
            AtomicReference<String> progressStage = new AtomicReference<>();

            AIStreamCallback callback = AIStreamCallback.builder()
                .onChunk(chunks::add)
                .onComplete(() -> completed.set(true))
                .onError(error::set)
                .onToolStart((name, args) -> toolStarted.set(name))
                .onToolComplete((name, result) -> toolCompleted.set(name))
                .onToolError((name, err) -> toolError.set(name + ":" + err))
                .onThinking(thinkingChunks::add)
                .onThinkingComplete(() -> thinkingCompleted.set(true))
                .onProgress((stage, detail) -> progressStage.set(stage))
                .build();

            // Test all events
            callback.onChunk("Hello ");
            callback.onChunk("World");
            assertThat(chunks).containsExactly("Hello ", "World");

            callback.onToolStart("queryDatabase", "{\"sql\": \"SELECT 1\"}");
            assertThat(toolStarted.get()).isEqualTo("queryDatabase");

            callback.onToolComplete("queryDatabase", "[{\"result\": 1}]");
            assertThat(toolCompleted.get()).isEqualTo("queryDatabase");

            callback.onToolError("badTool", "Not found");
            assertThat(toolError.get()).isEqualTo("badTool:Not found");

            callback.onThinking("Analyzing...");
            callback.onThinking("Processing...");
            assertThat(thinkingChunks).containsExactly("Analyzing...", "Processing...");

            callback.onThinkingComplete();
            assertThat(thinkingCompleted.get()).isTrue();

            callback.onProgress("querying", "Running database query");
            assertThat(progressStage.get()).isEqualTo("querying");

            callback.onComplete();
            assertThat(completed.get()).isTrue();
        }

        @Test
        @DisplayName("Default methods should not throw")
        void defaultMethodsShouldNotThrow() {
            AIStreamCallback minimalCallback = new AIStreamCallback() {
                @Override
                public void onChunk(String chunk) {}

                @Override
                public void onComplete() {}

                @Override
                public void onError(Exception error) {}
            };

            // These should not throw - they are default no-ops
            minimalCallback.onToolStart("test", "{}");
            minimalCallback.onToolComplete("test", "result");
            minimalCallback.onToolError("test", "error");
            minimalCallback.onThinking("thinking");
            minimalCallback.onThinkingComplete();
            minimalCallback.onProgress("stage", "detail");
        }

        @Test
        @DisplayName("Callback should handle null gracefully")
        void callbackShouldHandleNullGracefully() {
            List<String> chunks = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);

            AIStreamCallback callback = AIStreamCallback.builder()
                .onChunk(chunks::add)
                .onComplete(() -> completed.set(true))
                .onError(e -> {})
                .build();

            // These should not throw
            callback.onChunk(null);
            callback.onToolStart(null, null);
            callback.onToolComplete(null, null);
            callback.onToolError(null, null);
            callback.onThinking(null);
            callback.onProgress(null, null);
            callback.onComplete();

            assertThat(chunks).containsExactly((String) null);
            assertThat(completed.get()).isTrue();
        }

        @Test
        @DisplayName("Multiple chunks should accumulate correctly")
        void multipleChunksShouldAccumulate() {
            StringBuilder fullResponse = new StringBuilder();
            AtomicBoolean completed = new AtomicBoolean(false);

            AIStreamCallback callback = AIStreamCallback.simple(
                fullResponse::append,
                () -> completed.set(true),
                e -> {}
            );

            // Simulate streaming tokens
            callback.onChunk("Hello");
            callback.onChunk(" ");
            callback.onChunk("World");
            callback.onChunk("!");
            callback.onComplete();

            assertThat(fullResponse.toString()).isEqualTo("Hello World!");
            assertThat(completed.get()).isTrue();
        }
    }

    // ========================================================================
    // LangChain4j Provider Factory Tests
    // ========================================================================

    @Nested
    @DisplayName("LangChain4j Provider Factory")
    class ProviderFactoryTests {

        @Test
        @DisplayName("Should have correct provider constants")
        void shouldHaveCorrectProviderConstants() {
            // Provider types use AD_Ref_List codes
            assertThat(LangChain4jProviderFactory.PROVIDER_ANTHROPIC).isEqualTo("ANT");
            assertThat(LangChain4jProviderFactory.PROVIDER_OPENAI).isEqualTo("OAI");
            assertThat(LangChain4jProviderFactory.PROVIDER_OLLAMA).isEqualTo("OLL");
            assertThat(LangChain4jProviderFactory.PROVIDER_BEDROCK).isEqualTo("ABE");
        }
    }

    // ========================================================================
    // Integration Tests with Real Provider
    // ========================================================================

    @Nested
    @DisplayName("Streaming Integration Tests")
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    class StreamingIntegrationTests {

        @Test
        @DisplayName("Should stream response chunks from Anthropic")
        void shouldStreamResponseChunks() throws InterruptedException {
            String apiKey = System.getenv("ANTHROPIC_API_KEY");

            StreamingChatLanguageModel model = AnthropicStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("claude-3-haiku-20240307")
                .maxTokens(100)
                .build();

            CountDownLatch latch = new CountDownLatch(1);
            List<String> chunks = new ArrayList<>();
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<Exception> error = new AtomicReference<>();

            model.generate(
                List.of(UserMessage.from("Say 'Hello World' and nothing else.")),
                new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        System.out.print(token);
                        chunks.add(token);
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        System.out.println("\n[Complete]");
                        completed.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.err.println("[Error] " + throwable.getMessage());
                        error.set(new Exception(throwable));
                        latch.countDown();
                    }
                }
            );

            boolean finished = latch.await(30, TimeUnit.SECONDS);

            assertThat(finished).as("Streaming should complete within 30 seconds").isTrue();
            assertThat(error.get()).as("No error should occur").isNull();
            assertThat(completed.get()).as("onComplete should be called").isTrue();
            assertThat(chunks).as("Should receive at least one chunk").isNotEmpty();

            String fullResponse = String.join("", chunks);
            System.out.println("Full response: " + fullResponse);
            assertThat(fullResponse.toLowerCase()).contains("hello");
        }

        @Test
        @DisplayName("Should handle streaming errors gracefully")
        void shouldHandleStreamingErrors() throws InterruptedException {
            // Use invalid API key to trigger error
            StreamingChatLanguageModel model = AnthropicStreamingChatModel.builder()
                .apiKey("invalid-api-key")
                .modelName("claude-3-haiku-20240307")
                .maxTokens(100)
                .build();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean errorReceived = new AtomicBoolean(false);

            model.generate(
                List.of(UserMessage.from("Hello")),
                new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {}

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        errorReceived.set(true);
                        latch.countDown();
                    }
                }
            );

            boolean finished = latch.await(30, TimeUnit.SECONDS);

            assertThat(finished).as("Should complete within 30 seconds").isTrue();
            assertThat(errorReceived.get()).as("Should receive error for invalid API key").isTrue();
        }
    }

    // ========================================================================
    // ERPStreamingAgent Interface Tests
    // ========================================================================

    @Nested
    @DisplayName("ERPStreamingAgent Interface")
    class ERPStreamingAgentTests {

        @Test
        @DisplayName("Should have system prompt defined")
        void shouldHaveSystemPromptDefined() {
            assertThat(ERPStreamingAgent.SYSTEM_PROMPT).isNotNull();
            assertThat(ERPStreamingAgent.SYSTEM_PROMPT).isNotEmpty();
        }

        @Test
        @DisplayName("System prompt should match ERPAgent")
        void systemPromptShouldMatchERPAgent() {
            assertThat(ERPStreamingAgent.SYSTEM_PROMPT).isEqualTo(ERPAgent.SYSTEM_PROMPT);
        }
    }
}
