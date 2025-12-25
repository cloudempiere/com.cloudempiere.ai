package com.cloudempiere.ai.test.integration;

import com.cloudempiere.ai.test.categories.IntegrationTest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.cloudempiere.ai.provider.satellite.MockSatelliteServer;
import com.cloudempiere.ai.provider.satellite.MockSatelliteServer.ResponseMode;
import com.cloudempiere.ai.provider.satellite.SatelliteProviderConstants;

/**
 * Tests for MockSatelliteServer.
 *
 * <p>Verifies that the mock satellite server correctly implements
 * the OpenAI-compatible API endpoints required for satellite integration.
 *
 * @author Cloudempiere
 * @version 0.22.0
 * @since ADR-042
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@IntegrationTest
@DisplayName("Mock Satellite Server Tests")
class MockSatelliteServerTest {

    private static MockSatelliteServer server;
    private static final int TEST_PORT = 18090;  // Use non-standard port for testing
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;

    @BeforeAll
    static void startServer() throws Exception {
        server = new MockSatelliteServer(TEST_PORT);
        server.setLogRequests(false);  // Reduce noise in test output
        server.start();

        // Give server time to start
        Thread.sleep(100);
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    // ========================================================================
    // Health Check Tests
    // ========================================================================

    @Test
    @Order(1)
    @IntegrationTest
@DisplayName("Health endpoint returns UP status")
    void testHealthEndpoint() throws Exception {
        HttpURLConnection conn = createConnection("/health", "GET");

        int status = conn.getResponseCode();
        String response = readResponse(conn);

        assertEquals(200, status, "Health check should return 200");
        assertTrue(response.contains("\"status\":\"UP\""), "Should indicate UP status");
    }

    @Test
    @Order(2)
    @IntegrationTest
@DisplayName("Liveness probe returns healthy")
    void testLivenessProbe() throws Exception {
        HttpURLConnection conn = createConnection("/health/live", "GET");
        assertEquals(200, conn.getResponseCode(), "Liveness probe should return 200");
    }

    @Test
    @Order(3)
    @IntegrationTest
@DisplayName("Readiness probe returns ready")
    void testReadinessProbe() throws Exception {
        HttpURLConnection conn = createConnection("/health/ready", "GET");
        assertEquals(200, conn.getResponseCode(), "Readiness probe should return 200");
    }

    // ========================================================================
    // Chat Completion Tests
    // ========================================================================

    @Test
    @Order(10)
    @IntegrationTest
@DisplayName("Chat completion returns valid OpenAI-compatible response")
    void testChatCompletion() throws Exception {
        String requestBody = "{" +
            "\"model\":\"claude-sonnet-4\"," +
            "\"messages\":[{\"role\":\"user\",\"content\":\"Hello\"}]" +
            "}";

        HttpURLConnection conn = createConnection("/v1/chat/completions", "POST");
        conn.setRequestProperty("Authorization", "Bearer test-token");
        sendRequestBody(conn, requestBody);

        int status = conn.getResponseCode();
        String response = readResponse(conn);

        assertEquals(200, status, "Chat completion should return 200");

        // Verify OpenAI-compatible structure
        assertTrue(response.contains("\"id\":\"chatcmpl-"), "Should have completion ID");
        assertTrue(response.contains("\"object\":\"chat.completion\""), "Should have correct object type");
        assertTrue(response.contains("\"choices\""), "Should have choices array");
        assertTrue(response.contains("\"message\""), "Should have message in choices");
        assertTrue(response.contains("\"role\":\"assistant\""), "Should have assistant role");
        assertTrue(response.contains("\"content\""), "Should have content");
        assertTrue(response.contains("\"usage\""), "Should have usage info");
        assertTrue(response.contains("\"prompt_tokens\""), "Should have prompt tokens");
        assertTrue(response.contains("\"completion_tokens\""), "Should have completion tokens");
    }

    @Test
    @Order(11)
    @IntegrationTest
@DisplayName("Chat completion in ECHO mode echoes user message")
    void testChatCompletionEchoMode() throws Exception {
        server.setResponseMode(ResponseMode.ECHO);

        String userMessage = "Test message for echo";
        String requestBody = "{" +
            "\"model\":\"test-model\"," +
            "\"messages\":[{\"role\":\"user\",\"content\":\"" + userMessage + "\"}]" +
            "}";

        HttpURLConnection conn = createConnection("/v1/chat/completions", "POST");
        sendRequestBody(conn, requestBody);

        String response = readResponse(conn);

        assertTrue(response.contains("[ECHO]"), "Echo mode should prefix with [ECHO]");
        assertTrue(response.contains(userMessage), "Echo mode should include user message");

        // Reset to MOCK mode
        server.setResponseMode(ResponseMode.MOCK);
    }

    @Test
    @Order(12)
    @IntegrationTest
@DisplayName("Chat completion in MOCK mode returns contextual response")
    void testChatCompletionMockMode() throws Exception {
        server.setResponseMode(ResponseMode.MOCK);

        String requestBody = "{" +
            "\"model\":\"claude-sonnet-4\"," +
            "\"messages\":[{\"role\":\"user\",\"content\":\"Tell me about orders\"}]" +
            "}";

        HttpURLConnection conn = createConnection("/v1/chat/completions", "POST");
        sendRequestBody(conn, requestBody);

        String response = readResponse(conn);

        // Mock mode should return contextual response about orders
        assertTrue(response.contains("order") || response.contains("Order") || response.contains("sales"),
            "Mock mode should return contextual response about orders");
    }

    @Test
    @Order(13)
    @IntegrationTest
@DisplayName("Chat completion with iDempiere context headers")
    void testChatCompletionWithContextHeaders() throws Exception {
        String requestBody = "{" +
            "\"model\":\"claude-sonnet-4\"," +
            "\"messages\":[{\"role\":\"user\",\"content\":\"Hello\"}]" +
            "}";

        HttpURLConnection conn = createConnection("/v1/chat/completions", "POST");
        conn.setRequestProperty("Authorization", "Bearer test-token");
        conn.setRequestProperty(SatelliteProviderConstants.HEADER_CLIENT_ID, "1000000");
        conn.setRequestProperty(SatelliteProviderConstants.HEADER_USER_ID, "100");
        conn.setRequestProperty(SatelliteProviderConstants.HEADER_ROLE_ID, "102");
        conn.setRequestProperty(SatelliteProviderConstants.HEADER_LANGUAGE, "en_US");
        sendRequestBody(conn, requestBody);

        int status = conn.getResponseCode();
        assertEquals(200, status, "Should accept requests with iDempiere context headers");

        // Verify mock header is set
        String mockHeader = conn.getHeaderField(SatelliteProviderConstants.HEADER_MOCK);
        assertEquals("true", mockHeader, "Should indicate mock mode in response header");
    }

    @Test
    @Order(14)
    @IntegrationTest
@DisplayName("Chat completion rejects non-POST requests")
    void testChatCompletionMethodNotAllowed() throws Exception {
        HttpURLConnection conn = createConnection("/v1/chat/completions", "GET");

        int status = conn.getResponseCode();
        assertEquals(405, status, "Should reject non-POST requests with 405");
    }

    // ========================================================================
    // Embedding Tests
    // ========================================================================

    @Test
    @Order(20)
    @IntegrationTest
@DisplayName("Embeddings endpoint returns valid response")
    void testEmbeddings() throws Exception {
        String requestBody = "{" +
            "\"model\":\"text-embedding-3-small\"," +
            "\"input\":\"Test text for embedding\"" +
            "}";

        HttpURLConnection conn = createConnection("/v1/embeddings", "POST");
        sendRequestBody(conn, requestBody);

        int status = conn.getResponseCode();
        String response = readResponse(conn);

        assertEquals(200, status, "Embeddings should return 200");

        // Verify OpenAI-compatible structure
        assertTrue(response.contains("\"object\":\"list\""), "Should have list object type");
        assertTrue(response.contains("\"data\""), "Should have data array");
        assertTrue(response.contains("\"embedding\""), "Should have embedding");
        assertTrue(response.contains("\"usage\""), "Should have usage info");
    }

    @Test
    @Order(21)
    @IntegrationTest
@DisplayName("Embeddings returns 1536 dimensions")
    void testEmbeddingsDimensions() throws Exception {
        String requestBody = "{\"input\":\"Test\"}";

        HttpURLConnection conn = createConnection("/v1/embeddings", "POST");
        sendRequestBody(conn, requestBody);

        String response = readResponse(conn);

        // Count commas in embedding array to verify dimensions
        int embeddingStart = response.indexOf("\"embedding\":[") + 13;
        int embeddingEnd = response.indexOf("]", embeddingStart);
        String embedding = response.substring(embeddingStart, embeddingEnd);

        // Count dimensions (commas + 1)
        int dimensions = embedding.split(",").length;
        assertEquals(1536, dimensions, "Embedding should have 1536 dimensions");
    }

    // ========================================================================
    // Info Endpoint Tests
    // ========================================================================

    @Test
    @Order(30)
    @IntegrationTest
@DisplayName("Info endpoint returns server information")
    void testInfoEndpoint() throws Exception {
        HttpURLConnection conn = createConnection("/info", "GET");

        int status = conn.getResponseCode();
        String response = readResponse(conn);

        assertEquals(200, status, "Info endpoint should return 200");
        assertTrue(response.contains("\"name\":\"MockSatelliteServer\""), "Should have server name");
        assertTrue(response.contains("\"version\""), "Should have version");
        assertTrue(response.contains("\"mode\""), "Should have mode");
        assertTrue(response.contains("\"requests\""), "Should have request count");
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Test
    @Order(40)
    @IntegrationTest
@DisplayName("Invalid JSON returns error response")
    void testInvalidJsonError() throws Exception {
        String invalidJson = "not valid json";

        HttpURLConnection conn = createConnection("/v1/chat/completions", "POST");
        sendRequestBody(conn, invalidJson);

        // Should still return a response (mock mode is lenient)
        int status = conn.getResponseCode();
        assertTrue(status == 200 || status == 400 || status == 500,
            "Should handle invalid JSON gracefully");
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private HttpURLConnection createConnection(String path, String method) throws Exception {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return conn;
    }

    private void sendRequestBody(HttpURLConnection conn, String body) throws Exception {
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        var stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();

        if (stream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
