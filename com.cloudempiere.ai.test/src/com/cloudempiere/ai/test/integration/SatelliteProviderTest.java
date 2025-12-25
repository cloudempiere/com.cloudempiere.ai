package com.cloudempiere.ai.test.integration;

import com.cloudempiere.ai.test.categories.IntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.cloudempiere.ai.model.X_AIG_Provider;
import com.cloudempiere.ai.provider.langchain4j.IAIProviderConfig;
import com.cloudempiere.ai.provider.langchain4j.LangChain4jProviderFactory;
import com.cloudempiere.ai.provider.satellite.MockSatelliteServer;
import com.cloudempiere.ai.provider.satellite.SatelliteProviderConstants;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

/**
 * Tests for Satellite Provider Integration (ADR-042).
 *
 * <p>These tests verify that the PROVIDER_SATELLITE type correctly
 * routes requests through the MockSatelliteServer and receives
 * valid responses.
 *
 * @author Cloudempiere
 * @version 0.22.0
 * @since ADR-042
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@IntegrationTest
@DisplayName("Satellite Provider Integration Tests")
class SatelliteProviderTest {

    private static MockSatelliteServer mockServer;
    private static final int TEST_PORT = 18091;
    private static final String TEST_ENDPOINT = "http://127.0.0.1:" + TEST_PORT;

    @BeforeAll
    static void startMockServer() throws Exception {
        mockServer = new MockSatelliteServer(TEST_PORT);
        mockServer.setLogRequests(false);
        mockServer.start();

        // Give server time to fully initialize
        Thread.sleep(500);

        // Verify server is ready with health check
        int maxRetries = 20;
        boolean ready = false;
        for (int i = 0; i < maxRetries; i++) {
            try {
                java.net.URL healthUrl = new java.net.URL(TEST_ENDPOINT + "/health");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) healthUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                int responseCode = conn.getResponseCode();
                conn.disconnect();
                if (responseCode == 200) {
                    ready = true;
                    break;
                }
            } catch (Exception e) {
                // Server not ready yet
                Thread.sleep(100);
            }
        }

        if (!ready) {
            throw new RuntimeException("MockSatelliteServer failed to start within timeout");
        }
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    // ========================================================================
    // Provider Type Tests
    // ========================================================================

    @Test
    @Order(1)
    @IntegrationTest
@DisplayName("PROVIDER_SATELLITE constant matches AD_Ref_List value")
    void testProviderTypeConstant() {
        assertEquals("SAT", LangChain4jProviderFactory.PROVIDER_SATELLITE);
        assertEquals("SAT", X_AIG_Provider.AIGPROVIDERTYPE_QuarkusSatellite);
    }

    @Test
    @Order(2)
    @IntegrationTest
@DisplayName("Satellite provider has native embeddings")
    void testHasNativeEmbeddings() {
        assertTrue(LangChain4jProviderFactory.hasNativeEmbeddings(
            LangChain4jProviderFactory.PROVIDER_SATELLITE));
    }

    // ========================================================================
    // Health Check Tests
    // ========================================================================

    @Test
    @Order(10)
    @IntegrationTest
@DisplayName("Satellite health check returns true when server is running")
    void testSatelliteHealthCheckSuccess() {
        MockMAIProvider config = new MockMAIProvider();
        config.setAIGProviderType(LangChain4jProviderFactory.PROVIDER_SATELLITE);
        config.setEndpoint(TEST_ENDPOINT);

        assertTrue(LangChain4jProviderFactory.isSatelliteHealthy(config),
            "Health check should succeed when server is running");
    }

    @Test
    @Order(11)
    @IntegrationTest
@DisplayName("Satellite health check returns false when server is unavailable")
    void testSatelliteHealthCheckFailure() {
        MockMAIProvider config = new MockMAIProvider();
        config.setAIGProviderType(LangChain4jProviderFactory.PROVIDER_SATELLITE);
        config.setEndpoint("http://localhost:19999");  // Non-existent server

        assertFalse(LangChain4jProviderFactory.isSatelliteHealthy(config),
            "Health check should fail when server is unavailable");
    }

    @Test
    @Order(12)
    @IntegrationTest
@DisplayName("Health check returns true for non-satellite providers")
    void testNonSatelliteHealthCheck() {
        MockMAIProvider config = new MockMAIProvider();
        config.setAIGProviderType(LangChain4jProviderFactory.PROVIDER_ANTHROPIC);

        assertTrue(LangChain4jProviderFactory.isSatelliteHealthy(config),
            "Non-satellite providers should always return healthy");
    }

    // ========================================================================
    // Chat Model Tests
    // ========================================================================

    @Test
    @Order(20)
    @IntegrationTest
@DisplayName("Create satellite ChatLanguageModel successfully")
    void testCreateSatelliteChatModel() {
        MockMAIProvider config = new MockMAIProvider();
        config.setAIGProviderType(LangChain4jProviderFactory.PROVIDER_SATELLITE);
        config.setEndpoint(TEST_ENDPOINT);
        config.setAPIKey("test-token");
        config.setModelName("claude-sonnet-4");

        // Disable metrics for testing
        LangChain4jProviderFactory.setMetricsEnabled(false);

        ChatLanguageModel model = LangChain4jProviderFactory.create(config);
        assertNotNull(model, "Should create ChatLanguageModel for satellite provider");

        // Re-enable metrics
        LangChain4jProviderFactory.setMetricsEnabled(true);
    }

    @Test
    @Order(21)
    @IntegrationTest
@DisplayName("Satellite ChatLanguageModel generates response")
    void testSatelliteChatModelGeneratesResponse() {
        MockMAIProvider config = new MockMAIProvider();
        config.setAIGProviderType(LangChain4jProviderFactory.PROVIDER_SATELLITE);
        config.setEndpoint(TEST_ENDPOINT);
        config.setAPIKey("test-token");
        config.setModelName("claude-sonnet-4");

        LangChain4jProviderFactory.setMetricsEnabled(false);

        ChatLanguageModel model = LangChain4jProviderFactory.create(config);

        // Send a message and verify response
        Response<AiMessage> response = model.generate(UserMessage.from("Hello"));

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.content(), "Response content should not be null");
        assertNotNull(response.content().text(), "Response text should not be null");
        assertFalse(response.content().text().isEmpty(), "Response text should not be empty");

        LangChain4jProviderFactory.setMetricsEnabled(true);
    }

    @Test
    @Order(22)
    @IntegrationTest
@DisplayName("Satellite uses default endpoint when not configured")
    void testSatelliteDefaultEndpoint() {
        MockMAIProvider config = new MockMAIProvider();
        config.setAIGProviderType(LangChain4jProviderFactory.PROVIDER_SATELLITE);
        config.setEndpoint(null);  // No endpoint configured
        config.setAPIKey("test-token");

        LangChain4jProviderFactory.setMetricsEnabled(false);

        // Should not throw - uses default endpoint
        ChatLanguageModel model = LangChain4jProviderFactory.create(config);
        assertNotNull(model);

        LangChain4jProviderFactory.setMetricsEnabled(true);
    }

    @Test
    @Order(23)
    @IntegrationTest
@DisplayName("Satellite uses default model when not configured")
    void testSatelliteDefaultModel() {
        MockMAIProvider config = new MockMAIProvider();
        config.setAIGProviderType(LangChain4jProviderFactory.PROVIDER_SATELLITE);
        config.setEndpoint(TEST_ENDPOINT);
        config.setAPIKey("test-token");
        config.setModelName(null);  // No model configured

        LangChain4jProviderFactory.setMetricsEnabled(false);

        ChatLanguageModel model = LangChain4jProviderFactory.create(config);
        assertNotNull(model);

        LangChain4jProviderFactory.setMetricsEnabled(true);
    }

    // ========================================================================
    // Streaming Model Tests
    // ========================================================================

    @Test
    @Order(30)
    @IntegrationTest
@DisplayName("Create satellite StreamingChatLanguageModel successfully")
    void testCreateSatelliteStreamingModel() {
        MockMAIProvider config = new MockMAIProvider();
        config.setAIGProviderType(LangChain4jProviderFactory.PROVIDER_SATELLITE);
        config.setEndpoint(TEST_ENDPOINT);
        config.setAPIKey("test-token");
        config.setModelName("claude-sonnet-4");

        StreamingChatLanguageModel model = LangChain4jProviderFactory.createStreaming(config);
        assertNotNull(model, "Should create StreamingChatLanguageModel for satellite provider");
    }

    // ========================================================================
    // Embedding Model Tests
    // ========================================================================

    @Test
    @Order(40)
    @IntegrationTest
@DisplayName("Create satellite EmbeddingModel successfully")
    void testCreateSatelliteEmbeddingModel() {
        MockMAIProvider config = new MockMAIProvider();
        config.setAIGProviderType(LangChain4jProviderFactory.PROVIDER_SATELLITE);
        config.setEndpoint(TEST_ENDPOINT);
        config.setAPIKey("test-token");

        EmbeddingModel model = LangChain4jProviderFactory.createEmbeddingModel(config);
        assertNotNull(model, "Should create EmbeddingModel for satellite provider");
    }

    @Test
    @Order(41)
    @IntegrationTest
@DisplayName("Satellite EmbeddingModel generates embeddings")
    void testSatelliteEmbeddingModelGenerates() {
        MockMAIProvider config = new MockMAIProvider();
        config.setAIGProviderType(LangChain4jProviderFactory.PROVIDER_SATELLITE);
        config.setEndpoint(TEST_ENDPOINT);
        config.setAPIKey("test-token");

        EmbeddingModel model = LangChain4jProviderFactory.createEmbeddingModel(config);

        // Generate embedding
        var response = model.embed("Test text for embedding");

        assertNotNull(response, "Embedding response should not be null");
        assertNotNull(response.content(), "Embedding content should not be null");
        assertNotNull(response.content().vector(), "Embedding vector should not be null");
        assertEquals(1536, response.content().vector().length,
            "Embedding should have 1536 dimensions");
    }

    // ========================================================================
    // Constants Tests
    // ========================================================================

    @Test
    @Order(50)
    @IntegrationTest
@DisplayName("SatelliteProviderConstants has correct values")
    void testSatelliteProviderConstants() {
        assertEquals("SAT", SatelliteProviderConstants.PROVIDER_TYPE);
        assertEquals("/v1/chat/completions", SatelliteProviderConstants.ENDPOINT_CHAT);
        assertEquals("/v1/embeddings", SatelliteProviderConstants.ENDPOINT_EMBEDDINGS);
        assertEquals("/health", SatelliteProviderConstants.ENDPOINT_HEALTH);

        // Header constants
        assertEquals("X-iDempiere-Client-ID", SatelliteProviderConstants.HEADER_CLIENT_ID);
        assertEquals("X-iDempiere-User-ID", SatelliteProviderConstants.HEADER_USER_ID);
        assertEquals("X-iDempiere-Role-ID", SatelliteProviderConstants.HEADER_ROLE_ID);
    }

    // ========================================================================
    // Mock Provider Config for Testing
    // ========================================================================

    /**
     * Mock implementation of IAIProviderConfig for testing without database.
     *
     * <p>This avoids extending MAIProvider which requires the full iDempiere
     * PO hierarchy and database dependencies.
     */
    static class MockMAIProvider implements IAIProviderConfig {

        private String providerType;
        private String apiKey;
        private String modelName;
        private String endpoint;
        private int providerId = 999999;

        @Override
        public String getAIGProviderType() {
            return providerType;
        }

        public void setAIGProviderType(String type) {
            this.providerType = type;
        }

        @Override
        public String getAPIKey() {
            return apiKey;
        }

        public void setAPIKey(String key) {
            this.apiKey = key;
        }

        @Override
        public String getModelName() {
            return modelName;
        }

        public void setModelName(String name) {
            this.modelName = name;
        }

        @Override
        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public int getAIG_Provider_ID() {
            return providerId;
        }

        public void setAIG_Provider_ID(int id) {
            this.providerId = id;
        }
    }
}
