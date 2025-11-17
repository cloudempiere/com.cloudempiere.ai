//package com.cloudempiere.ai.provider.test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
//
//import com.cloudempiere.ai.model.MAIProvider;
//import com.cloudempiere.ai.provider.dto.AIHealthStatus;
//import com.cloudempiere.ai.provider.dto.AIMessage;
//import com.cloudempiere.ai.provider.dto.AIRequest;
//import com.cloudempiere.ai.provider.dto.AIResponse;
//import com.cloudempiere.ai.provider.impl.AnthropicProvider;
//
///**
// * Test class for AnthropicProvider
// *
// * To run this test, you need to set the ANTHROPIC_API_KEY environment variable:
// * export ANTHROPIC_API_KEY=sk-ant-xxx
// *
// * Or run from command line:
// * ANTHROPIC_API_KEY=sk-ant-xxx mvn test -Dtest=AnthropicProviderTest
// */
//@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
//public class AnthropicProviderTest {
//
//	private AnthropicProvider provider;
//	private MAIProvider mockConfig;
//
//	@BeforeEach
//	public void setUp() {
//		provider = new AnthropicProvider();
//
//		// Create mock configuration
//		mockConfig = new MAIProvider(null, 0, null);
//		mockConfig.setName("Test Anthropic Provider");
//		mockConfig.setAIGProviderType(MAIProvider.AIGPROVIDERTYPE_AnthropicClaude);
//		mockConfig.setAPIKey(System.getenv("ANTHROPIC_API_KEY"));
//	}
//
//	@Test
//	public void testInitialization() throws Exception {
//		provider.initialize(mockConfig);
//		assertTrue(provider.isReady(), "Provider should be ready after initialization");
//		assertEquals("Anthropic Claude", provider.getProviderName());
//		assertEquals(MAIProvider.AIGPROVIDERTYPE_AnthropicClaude, provider.getProviderType());
//	}
//
//	@Test
//	public void testHealthCheck() throws Exception {
//		provider.initialize(mockConfig);
//
//		AIHealthStatus status = provider.checkHealth();
//		assertNotNull(status, "Health status should not be null");
//		assertTrue(status.isHealthy(), "Provider should be healthy");
//		assertEquals("Healthy", status.getStatus());
//		assertTrue(status.getResponseTimeMs() > 0, "Response time should be greater than 0");
//
//		System.out.println("✓ Health check passed - Response time: " + status.getResponseTimeMs() + "ms");
//	}
//
//	@Test
//	public void testSimpleTextGeneration() throws Exception {
//		provider.initialize(mockConfig);
//
//		// Create a simple request
//		AIRequest request = new AIRequest();
//		request.setModel("claude-3-haiku-20240307"); // Using fastest/cheapest model for testing
//		request.setMaxTokens(100);
//		request.setTemperature(0.7);
//
//		List<AIMessage> messages = new ArrayList<>();
//		AIMessage userMessage = new AIMessage("user", "Say hello and tell me what AI model you are in one sentence.");
//		messages.add(userMessage);
//
//		request.setMessages(messages);
//
//		// Generate response
//		AIResponse response = provider.generateText(request);
//
//		// Assertions
//		assertNotNull(response, "Response should not be null");
//		assertNotNull(response.getContent(), "Response content should not be null");
//		assertFalse(response.getContent().isEmpty(), "Response content should not be empty");
//		assertEquals("claude-3-haiku-20240307", response.getModel());
//		assertNotNull(response.getTokenUsage(), "Token usage should not be null");
//		assertTrue(response.getTokenUsage().getTotalTokens() > 0, "Total tokens should be greater than 0");
//		assertTrue(response.getProcessingTimeMs() > 0, "Processing time should be greater than 0");
//
//		System.out.println("\n✓ Text generation test passed");
//		System.out.println("Response: " + response.getContent());
//		System.out.println("Tokens used: " + response.getTokenUsage().getTotalTokens());
//		System.out.println("Cost: $" + String.format("%.6f", response.getCostUSD()));
//	}
//
//	@Test
//	public void testSystemPrompt() throws Exception {
//		provider.initialize(mockConfig);
//
//		AIRequest request = new AIRequest();
//		request.setModel("claude-3-haiku-20240307");
//		request.setMaxTokens(50);
//		request.setSystemPrompt("You are a helpful assistant that always responds in exactly 3 words.");
//
//		List<AIMessage> messages = new ArrayList<>();
//		AIMessage userMessage = new AIMessage("user", "What is the capital of France?");
//		messages.add(userMessage);
//
//		request.setMessages(messages);
//
//		AIResponse response = provider.generateText(request);
//
//		assertNotNull(response.getContent());
//		System.out.println("\n✓ System prompt test passed");
//		System.out.println("Response: " + response.getContent());
//	}
//
//	@Test
//	public void testCostEstimation() throws Exception {
//		provider.initialize(mockConfig);
//
//		AIRequest request = new AIRequest();
//		request.setModel("claude-3-haiku-20240307");
//		request.setMaxTokens(100);
//
//		List<AIMessage> messages = new ArrayList<>();
//		AIMessage userMessage = new AIMessage("user", "Hello, how are you?");
//		messages.add(userMessage);
//
//		request.setMessages(messages);
//
//		double estimatedCost = provider.estimateCost(request);
//
//		assertTrue(estimatedCost > 0, "Estimated cost should be greater than 0");
//		System.out.println("\n✓ Cost estimation test passed");
//		System.out.println("Estimated cost: $" + String.format("%.6f", estimatedCost));
//	}
//
//	@Test
//	public void testSupportedFeatures() throws Exception {
//		provider.initialize(mockConfig);
//
//		assertTrue(provider.supportsTextGeneration());
//		assertTrue(provider.supportsStreaming());
//		assertTrue(provider.supportsFunctionCalling());
//		assertTrue(provider.supportsVision());
//		assertFalse(provider.supportsAudio());
//		assertFalse(provider.supportsEmbeddings());
//
//		System.out.println("\n✓ Feature support test passed");
//	}
//}