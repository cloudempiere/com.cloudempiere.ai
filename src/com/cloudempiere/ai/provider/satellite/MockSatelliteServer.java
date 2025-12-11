package com.cloudempiere.ai.provider.satellite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Mock Satellite Server for development and testing.
 *
 * <p>Provides OpenAI-compatible REST API endpoints that can be used
 * to test the PROVIDER_SATELLITE integration without requiring the
 * actual Quarkus Satellite service.
 *
 * <p>Features:
 * <ul>
 *   <li>OpenAI-compatible /v1/chat/completions endpoint</li>
 *   <li>OpenAI-compatible /v1/embeddings endpoint</li>
 *   <li>Health check endpoints (/health, /health/live, /health/ready)</li>
 *   <li>Configurable response modes (echo, mock, proxy)</li>
 *   <li>Request logging for debugging</li>
 *   <li>Streaming support (SSE)</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * // Start mock server on port 8090
 * MockSatelliteServer server = new MockSatelliteServer(8090);
 * server.start();
 *
 * // Configure AIG_Provider
 * // Endpoint: http://localhost:8090
 * // APIKey: mock-token
 * // Type: SAT
 *
 * // Stop when done
 * server.stop();
 * </pre>
 *
 * @author Cloudempiere
 * @version 0.22.0
 * @since ADR-042 Satellite AI Provider Integration
 */
public class MockSatelliteServer {

    private static final Logger log = Logger.getLogger(MockSatelliteServer.class.getName());

    /** Response mode for chat completions */
    public enum ResponseMode {
        /** Echo back the user's message */
        ECHO,
        /** Return a fixed mock response */
        MOCK,
        /** Proxy to a real provider (requires API key) */
        PROXY
    }

    private final int port;
    private HttpServer server;
    private ResponseMode responseMode = ResponseMode.MOCK;
    private String proxyApiKey;
    private String proxyBaseUrl;
    private boolean logRequests = true;

    // Statistics
    private int requestCount = 0;
    private int errorCount = 0;

    /**
     * Create a mock satellite server on the specified port.
     *
     * @param port Port to listen on (e.g., 8090)
     */
    public MockSatelliteServer(int port) {
        this.port = port;
    }

    /**
     * Start the mock server.
     *
     * @throws IOException if server cannot be started
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Health endpoints
        server.createContext("/health", new HealthHandler());
        server.createContext("/health/live", new HealthHandler());
        server.createContext("/health/ready", new HealthHandler());

        // OpenAI-compatible endpoints
        server.createContext("/v1/chat/completions", new ChatCompletionsHandler());
        server.createContext("/v1/embeddings", new EmbeddingsHandler());

        // Info endpoint
        server.createContext("/info", new InfoHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        log.info("Mock Satellite Server started on port " + port);
        log.info("Endpoints:");
        log.info("  POST /v1/chat/completions - Chat completions");
        log.info("  POST /v1/embeddings       - Embeddings");
        log.info("  GET  /health              - Health check");
        log.info("  GET  /info                - Server info");
    }

    /**
     * Stop the mock server.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            log.info("Mock Satellite Server stopped");
        }
    }

    /**
     * Set the response mode.
     *
     * @param mode ECHO, MOCK, or PROXY
     */
    public void setResponseMode(ResponseMode mode) {
        this.responseMode = mode;
    }

    /**
     * Configure proxy mode to forward requests to a real provider.
     *
     * @param baseUrl Base URL of the provider (e.g., https://api.anthropic.com)
     * @param apiKey API key for the provider
     */
    public void setProxyConfig(String baseUrl, String apiKey) {
        this.proxyBaseUrl = baseUrl;
        this.proxyApiKey = apiKey;
        this.responseMode = ResponseMode.PROXY;
    }

    /**
     * Enable or disable request logging.
     *
     * @param enabled true to log requests
     */
    public void setLogRequests(boolean enabled) {
        this.logRequests = enabled;
    }

    // ========================================================================
    // HTTP Handlers
    // ========================================================================

    /**
     * Health check handler.
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"UP\",\"mode\":\"" + responseMode + "\"}";
            sendJsonResponse(exchange, 200, response);
        }
    }

    /**
     * Server info handler.
     */
    private class InfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = String.format(
                "{\"name\":\"MockSatelliteServer\"," +
                "\"version\":\"0.22.0\"," +
                "\"mode\":\"%s\"," +
                "\"requests\":%d," +
                "\"errors\":%d," +
                "\"uptime\":\"%s\"}",
                responseMode, requestCount, errorCount, Instant.now().toString()
            );
            sendJsonResponse(exchange, 200, response);
        }
    }

    /**
     * Chat completions handler (OpenAI-compatible).
     */
    private class ChatCompletionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount++;

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405,
                    "{\"error\":{\"message\":\"Method not allowed\",\"type\":\"invalid_request_error\"}}");
                return;
            }

            try {
                // Read request body
                String requestBody = readRequestBody(exchange);

                if (logRequests) {
                    log.info("Chat request: " + truncate(requestBody, 500));
                    logHeaders(exchange);
                }

                // Parse request (simple JSON parsing without external dependencies)
                Map<String, Object> request = parseSimpleJson(requestBody);
                String model = (String) request.getOrDefault("model", "mock-model");
                boolean stream = Boolean.TRUE.equals(request.get("stream"));

                // Extract last user message for echo mode
                String userMessage = extractLastUserMessage(requestBody);

                // Generate response based on mode
                String response;
                switch (responseMode) {
                    case ECHO:
                        response = createEchoResponse(model, userMessage);
                        break;
                    case PROXY:
                        response = proxyRequest(requestBody);
                        break;
                    case MOCK:
                    default:
                        response = createMockResponse(model, userMessage);
                        break;
                }

                if (stream) {
                    sendStreamingResponse(exchange, response);
                } else {
                    sendJsonResponse(exchange, 200, response);
                }

            } catch (Exception e) {
                errorCount++;
                log.log(Level.WARNING, "Error processing chat request", e);
                sendJsonResponse(exchange, 500,
                    "{\"error\":{\"message\":\"" + escapeJson(e.getMessage()) + "\",\"type\":\"server_error\"}}");
            }
        }
    }

    /**
     * Embeddings handler (OpenAI-compatible).
     */
    private class EmbeddingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            requestCount++;

            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405,
                    "{\"error\":{\"message\":\"Method not allowed\",\"type\":\"invalid_request_error\"}}");
                return;
            }

            try {
                String requestBody = readRequestBody(exchange);

                if (logRequests) {
                    log.info("Embedding request: " + truncate(requestBody, 500));
                }

                // Generate mock embedding (1536 dimensions like OpenAI)
                String response = createMockEmbeddingResponse();
                sendJsonResponse(exchange, 200, response);

            } catch (Exception e) {
                errorCount++;
                log.log(Level.WARNING, "Error processing embedding request", e);
                sendJsonResponse(exchange, 500,
                    "{\"error\":{\"message\":\"" + escapeJson(e.getMessage()) + "\",\"type\":\"server_error\"}}");
            }
        }
    }

    // ========================================================================
    // Response Generators
    // ========================================================================

    /**
     * Create an echo response that includes the user's message.
     */
    private String createEchoResponse(String model, String userMessage) {
        String id = "chatcmpl-mock-" + UUID.randomUUID().toString().substring(0, 8);
        long timestamp = System.currentTimeMillis() / 1000;

        String content = "[ECHO] You said: " + userMessage;

        return String.format(
            "{\"id\":\"%s\"," +
            "\"object\":\"chat.completion\"," +
            "\"created\":%d," +
            "\"model\":\"%s\"," +
            "\"choices\":[{" +
                "\"index\":0," +
                "\"message\":{\"role\":\"assistant\",\"content\":\"%s\"}," +
                "\"finish_reason\":\"stop\"" +
            "}]," +
            "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":20,\"total_tokens\":30}}",
            id, timestamp, model, escapeJson(content)
        );
    }

    /**
     * Create a mock response with realistic content.
     */
    private String createMockResponse(String model, String userMessage) {
        String id = "chatcmpl-mock-" + UUID.randomUUID().toString().substring(0, 8);
        long timestamp = System.currentTimeMillis() / 1000;

        // Generate contextual mock response
        String content = generateMockContent(userMessage);

        return String.format(
            "{\"id\":\"%s\"," +
            "\"object\":\"chat.completion\"," +
            "\"created\":%d," +
            "\"model\":\"%s\"," +
            "\"choices\":[{" +
                "\"index\":0," +
                "\"message\":{\"role\":\"assistant\",\"content\":\"%s\"}," +
                "\"finish_reason\":\"stop\"" +
            "}]," +
            "\"usage\":{\"prompt_tokens\":15,\"completion_tokens\":50,\"total_tokens\":65}," +
            "\"satellite\":{\"mode\":\"mock\",\"version\":\"0.22.0\"}}",
            id, timestamp, model, escapeJson(content)
        );
    }

    /**
     * Generate mock content based on the user message.
     */
    private String generateMockContent(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return "Hello! I'm the Mock Satellite AI. How can I help you today?";
        }

        String lowerMessage = userMessage.toLowerCase();

        // Context-aware mock responses
        if (lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "Hello! I'm the Mock Satellite AI assistant. I'm here to help you test the satellite integration. What would you like to do?";
        }

        if (lowerMessage.contains("order") || lowerMessage.contains("sales")) {
            return "I can help you with sales orders. In the mock environment, I can simulate:\\n" +
                   "- Order lookups\\n" +
                   "- Sales summaries\\n" +
                   "- Customer information\\n\\n" +
                   "What specific information do you need?";
        }

        if (lowerMessage.contains("invoice") || lowerMessage.contains("payment")) {
            return "For invoices and payments, I can provide mock data showing:\\n" +
                   "- Outstanding invoices: 5 items totaling $12,450.00\\n" +
                   "- Overdue payments: 2 items totaling $3,200.00\\n" +
                   "- Payment terms: Net 30\\n\\n" +
                   "Would you like more details on any of these?";
        }

        if (lowerMessage.contains("product") || lowerMessage.contains("inventory")) {
            return "Mock inventory status:\\n" +
                   "- Total SKUs: 1,234\\n" +
                   "- Low stock items: 15\\n" +
                   "- Out of stock: 3\\n\\n" +
                   "I can provide more specific product information if needed.";
        }

        if (lowerMessage.contains("help") || lowerMessage.contains("what can you do")) {
            return "I'm the Mock Satellite AI for testing purposes. I can:\\n" +
                   "1. Echo your messages (ECHO mode)\\n" +
                   "2. Generate contextual mock responses (MOCK mode)\\n" +
                   "3. Proxy to real AI providers (PROXY mode)\\n\\n" +
                   "Current mode: " + responseMode + "\\n\\n" +
                   "Try asking about orders, invoices, products, or inventory!";
        }

        // Default response
        return "I understand you're asking about: \\\"" + truncate(userMessage, 100) + "\\\"\\n\\n" +
               "As a mock satellite service, I'm simulating the response that would come from the " +
               "real Quarkus Satellite AI service. In production, this would be processed by " +
               "LangChain4j 1.x with full MCP and tool support.\\n\\n" +
               "Mock response generated at: " + Instant.now().toString();
    }

    /**
     * Create a mock embedding response.
     */
    private String createMockEmbeddingResponse() {
        // Generate 1536-dimension mock embedding (OpenAI text-embedding-3-small)
        StringBuilder embedding = new StringBuilder("[");
        for (int i = 0; i < 1536; i++) {
            if (i > 0) embedding.append(",");
            // Generate deterministic mock values
            double value = Math.sin(i * 0.01) * 0.1;
            embedding.append(String.format("%.8f", value));
        }
        embedding.append("]");

        return String.format(
            "{\"object\":\"list\"," +
            "\"data\":[{" +
                "\"object\":\"embedding\"," +
                "\"index\":0," +
                "\"embedding\":%s" +
            "}]," +
            "\"model\":\"mock-embedding\"," +
            "\"usage\":{\"prompt_tokens\":5,\"total_tokens\":5}}",
            embedding.toString()
        );
    }

    /**
     * Proxy request to a real provider.
     */
    private String proxyRequest(String requestBody) {
        // TODO: Implement actual proxy when needed
        // For now, return a message indicating proxy mode is configured
        return createMockResponse("proxy-model",
            "Proxy mode is configured but not yet implemented. " +
            "Target: " + proxyBaseUrl);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("X-Satellite-Mock", "true");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendStreamingResponse(HttpExchange exchange, String fullResponse) throws IOException {
        // Simulate SSE streaming
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("X-Satellite-Mock", "true");
        exchange.sendResponseHeaders(200, 0);

        try (OutputStream os = exchange.getResponseBody()) {
            // Send response in chunks
            String content = extractContentFromResponse(fullResponse);
            String[] words = content.split(" ");

            for (int i = 0; i < words.length; i++) {
                String chunk = createStreamChunk(words[i] + (i < words.length - 1 ? " " : ""));
                os.write(("data: " + chunk + "\n\n").getBytes(StandardCharsets.UTF_8));
                os.flush();

                // Simulate streaming delay
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Send done marker
            os.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    private String createStreamChunk(String content) {
        return String.format(
            "{\"id\":\"chatcmpl-mock\",\"object\":\"chat.completion.chunk\"," +
            "\"choices\":[{\"index\":0,\"delta\":{\"content\":\"%s\"},\"finish_reason\":null}]}",
            escapeJson(content)
        );
    }

    private String extractContentFromResponse(String response) {
        // Simple extraction of content from JSON response
        int contentStart = response.indexOf("\"content\":\"") + 11;
        int contentEnd = response.indexOf("\"}", contentStart);
        if (contentStart > 10 && contentEnd > contentStart) {
            return response.substring(contentStart, contentEnd);
        }
        return "Mock response";
    }

    private String extractLastUserMessage(String requestBody) {
        // Simple extraction of last user message from request
        int lastUserIdx = requestBody.lastIndexOf("\"role\":\"user\"");
        if (lastUserIdx < 0) return "";

        int contentStart = requestBody.indexOf("\"content\":\"", lastUserIdx);
        if (contentStart < 0) return "";
        contentStart += 11;

        int contentEnd = requestBody.indexOf("\"", contentStart);
        if (contentEnd < 0) return "";

        return requestBody.substring(contentStart, contentEnd);
    }

    private Map<String, Object> parseSimpleJson(String json) {
        // Very simple JSON parsing for basic fields
        Map<String, Object> result = new HashMap<>();

        // Extract model
        int modelIdx = json.indexOf("\"model\":\"");
        if (modelIdx >= 0) {
            int start = modelIdx + 9;
            int end = json.indexOf("\"", start);
            if (end > start) {
                result.put("model", json.substring(start, end));
            }
        }

        // Extract stream flag (handle both "stream":true and "stream": true with space)
        result.put("stream", json.contains("\"stream\":true") || json.contains("\"stream\": true"));

        return result;
    }

    private void logHeaders(HttpExchange exchange) {
        log.info("Headers:");
        exchange.getRequestHeaders().forEach((key, values) -> {
            // Don't log authorization header value
            if ("Authorization".equalsIgnoreCase(key)) {
                log.info("  " + key + ": [REDACTED]");
            } else {
                log.info("  " + key + ": " + String.join(", ", values));
            }
        });

        // Log iDempiere context headers
        String clientId = exchange.getRequestHeaders().getFirst("X-iDempiere-Client-ID");
        String userId = exchange.getRequestHeaders().getFirst("X-iDempiere-User-ID");
        String roleId = exchange.getRequestHeaders().getFirst("X-iDempiere-Role-ID");

        if (clientId != null || userId != null || roleId != null) {
            log.info("iDempiere Context: Client=" + clientId + ", User=" + userId + ", Role=" + roleId);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    // ========================================================================
    // Main Method (for standalone testing)
    // ========================================================================

    /**
     * Run the mock server standalone.
     *
     * <p>Usage: java MockSatelliteServer [port] [mode]
     * <p>Example: java MockSatelliteServer 8090 MOCK
     */
    public static void main(String[] args) {
        int port = 8090;
        ResponseMode mode = ResponseMode.MOCK;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: " + args[0]);
                System.exit(1);
            }
        }

        if (args.length > 1) {
            try {
                mode = ResponseMode.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid mode: " + args[1] + ". Use ECHO, MOCK, or PROXY");
                System.exit(1);
            }
        }

        try {
            MockSatelliteServer server = new MockSatelliteServer(port);
            server.setResponseMode(mode);
            server.start();

            System.out.println("\nMock Satellite Server running on http://localhost:" + port);
            System.out.println("Mode: " + mode);
            System.out.println("\nPress Ctrl+C to stop...\n");

            // Keep running until interrupted
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}
