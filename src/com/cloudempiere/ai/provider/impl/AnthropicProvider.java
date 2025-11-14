package com.cloudempiere.ai.provider.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.compiere.util.CLogger;

import com.anthropic.Anthropic;
import com.anthropic.models.MessageCreateParams;
import com.anthropic.models.MessageStreamEvent;
import com.anthropic.models.TextBlock;
import com.anthropic.models.Usage;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.AIProviderException;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.dto.AIFunction;
import com.cloudempiere.ai.provider.dto.AIHealthStatus;
import com.cloudempiere.ai.provider.dto.AIMessage;
import com.cloudempiere.ai.provider.dto.AIModelCapabilities;
import com.cloudempiere.ai.provider.dto.AIRateLimitStatus;
import com.cloudempiere.ai.provider.dto.AIRequest;
import com.cloudempiere.ai.provider.dto.AIResponse;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;
import com.cloudempiere.ai.provider.dto.AITokenUsage;

import software.amazon.awssdk.services.sqs.model.Message;

/**
 * Anthropic Claude AI Provider Implementation
 *
 * Implements the IAIProvider interface for Anthropic's Claude models.
 * Supports Claude 3 family: Opus, Sonnet, and Haiku.
 *
 * API Documentation: https://docs.anthropic.com/claude/reference
 * Uses official Anthropic Java SDK v2.10.0
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AnthropicProvider implements IAIProvider {

	private static final CLogger log = CLogger.getCLogger(AnthropicProvider.class);

	/** Default model if none specified */
	private static final String DEFAULT_MODEL = "claude-3-5-sonnet-20240620";

	/** Provider configuration from database */
	private MAIProvider providerConfig;

	/** Anthropic SDK client */
	private Anthropic client;

	/** Provider ready flag */
	private boolean ready = false;

	/**
	 * Model pricing per 1M tokens (USD)
	 * Updated as of January 2025
	 */
	private static final java.util.Map<String, ModelPricing> MODEL_PRICING = java.util.Map.of(
		"claude-3-opus-20240229", new ModelPricing(15.0, 75.0),
		"claude-3-sonnet-20240229", new ModelPricing(3.0, 15.0),
		"claude-3-haiku-20240307", new ModelPricing(0.25, 1.25),
		"claude-3-5-sonnet-20240620", new ModelPricing(3.0, 15.0)
	);

	/**
	 * Model pricing structure
	 */
	private static class ModelPricing {
		final double inputPer1M;
		final double outputPer1M;

		ModelPricing(double inputPer1M, double outputPer1M) {
			this.inputPer1M = inputPer1M;
			this.outputPer1M = outputPer1M;
		}
	}

	@Override
	public void initialize(MAIProvider provider) throws AIProviderException {
		if (provider == null) {
			throw new AIProviderException("Provider configuration is null");
		}

		this.providerConfig = provider;
		String apiKey = provider.getAPIKey();

		// Validate required fields
		if (apiKey == null || apiKey.trim().isEmpty()) {
			throw new AIProviderException(
				"API Key is required for Anthropic provider: " + provider.getName()
			);
		}

		try {
			// Initialize Anthropic SDK client
			this.client = Anthropic.builder()
				.apiKey(apiKey)
				.build();

			// Test connection
			if (!testConnection()) {
				throw new AIProviderException(
					"Failed to connect to Anthropic API. Please verify your API key."
				);
			}

			this.ready = true;
			log.info("Anthropic provider initialized successfully: " + provider.getName());

		} catch (Exception e) {
			log.severe("Failed to initialize Anthropic provider: " + e.getMessage());
			throw new AIProviderException("Initialization failed: " + e.getMessage(), e);
		}
	}

	@Override
	public String getProviderType() {
		return MAIProvider.AIGPROVIDERTYPE_AnthropicClaude;
	}

	@Override
	public String getProviderName() {
		return "Anthropic Claude";
	}

	@Override
	public String getProviderVersion() {
		return "2023-06-01"; // Anthropic API version
	}

	@Override
	public AIResponse generateText(AIRequest request) throws AIProviderException {
		if (!ready) {
			throw new AIProviderException("Provider not initialized");
		}

		if (request == null) {
			throw new AIProviderException("Request cannot be null");
		}

		long startTime = System.currentTimeMillis();

		try {
			// Build message request
			MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
				.model(request.getModel() != null ? request.getModel() : DEFAULT_MODEL)
				.maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096);

			// Add temperature if specified
			if (request.getTemperature() != null) {
				paramsBuilder.temperature(request.getTemperature());
			}

			// Add system prompt if specified
			if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
				paramsBuilder.system(request.getSystemPrompt());
			}

			// Convert messages
			if (request.getMessages() != null && !request.getMessages().isEmpty()) {
				List<MessageCreateParams.Message> messages = request.getMessages().stream()
					.map(this::convertMessage)
					.collect(Collectors.toList());
				paramsBuilder.messages(messages);
			}

			// Create message
			Message response = client.messages().create(paramsBuilder.build());

			// Parse response
			AIResponse aiResponse = parseResponse(response, startTime);

			log.fine("Generated text successfully using model: " + request.getModel());
			return aiResponse;

		} catch (Exception e) {
			log.severe("Failed to generate text: " + e.getMessage());
			throw new AIProviderException("Text generation failed: " + e.getMessage(), e);
		}
	}

	@Override
	public void generateTextStream(AIRequest request, AIStreamCallback callback)
			throws AIProviderException {
		if (!ready) {
			throw new AIProviderException("Provider not initialized");
		}

		if (request == null || callback == null) {
			throw new AIProviderException("Request and callback cannot be null");
		}

		try {
			// Build message request
			MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
				.model(request.getModel() != null ? request.getModel() : DEFAULT_MODEL)
				.maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096);

			if (request.getTemperature() != null) {
				paramsBuilder.temperature(request.getTemperature());
			}

			if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
				paramsBuilder.system(request.getSystemPrompt());
			}

			if (request.getMessages() != null && !request.getMessages().isEmpty()) {
				List<MessageCreateParams.Message> messages = request.getMessages().stream()
					.map(this::convertMessage)
					.collect(Collectors.toList());
				paramsBuilder.messages(messages);
			}

			// Stream message
			StringBuilder fullContent = new StringBuilder();

			client.messages().stream(paramsBuilder.build())
				.forEach(event -> {
					if (event instanceof MessageStreamEvent.ContentBlockDelta) {
						MessageStreamEvent.ContentBlockDelta delta =
							(MessageStreamEvent.ContentBlockDelta) event;
						String text = extractDeltaText(delta);
						if (text != null && !text.isEmpty()) {
							fullContent.append(text);
							callback.onChunk(text);
						}
					}
				});

			callback.onComplete(fullContent.toString());
			log.fine("Streaming text generation completed");

		} catch (Exception e) {
			log.severe("Failed to stream text: " + e.getMessage());
			callback.onError(e);
			throw new AIProviderException("Text streaming failed: " + e.getMessage(), e);
		}
	}

	@Override
	public AIResponse generateTextWithFunctions(AIRequest request, List<AIFunction> functions)
			throws AIProviderException {
		if (!ready) {
			throw new AIProviderException("Provider not initialized");
		}

		// Anthropic uses "tools" instead of "functions"
		try {
			MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
				.model(request.getModel() != null ? request.getModel() : DEFAULT_MODEL)
				.maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096);

			if (request.getTemperature() != null) {
				paramsBuilder.temperature(request.getTemperature());
			}

			if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
				paramsBuilder.system(request.getSystemPrompt());
			}

			if (request.getMessages() != null && !request.getMessages().isEmpty()) {
				List<MessageCreateParams.Message> messages = request.getMessages().stream()
					.map(this::convertMessage)
					.collect(Collectors.toList());
				paramsBuilder.messages(messages);
			}

			// Add tools if provided
			if (functions != null && !functions.isEmpty()) {
				List<MessageCreateParams.Tool> tools = functions.stream()
					.map(this::convertFunction)
					.collect(Collectors.toList());
				paramsBuilder.tools(tools);
			}

			long startTime = System.currentTimeMillis();
			Message response = client.messages().create(paramsBuilder.build());
			return parseResponse(response, startTime);

		} catch (Exception e) {
			log.severe("Failed to generate text with functions: " + e.getMessage());
			throw new AIProviderException("Function call failed: " + e.getMessage(), e);
		}
	}

	@Override
	public float[] generateEmbedding(String text, String modelName) throws AIProviderException {
		// Anthropic does not currently provide embedding models
		throw new AIProviderException(
			"Anthropic does not support embedding generation. " +
			"Please use a different provider (e.g., OpenAI) for embeddings."
		);
	}

	@Override
	public AIResponse analyzeImage(byte[] imageData, String prompt, String modelName)
			throws AIProviderException {
		if (!ready) {
			throw new AIProviderException("Provider not initialized");
		}

		try {
			// Claude 3 models support vision through message content blocks
			String base64Image = java.util.Base64.getEncoder().encodeToString(imageData);

			// Build vision request (implementation depends on SDK support)
			// For now, throw exception as this requires specific SDK implementation
			throw new AIProviderException(
				"Image analysis implementation requires SDK-specific vision support. " +
				"This will be implemented in a future version."
			);

		} catch (Exception e) {
			log.severe("Failed to analyze image: " + e.getMessage());
			throw new AIProviderException("Image analysis failed: " + e.getMessage(), e);
		}
	}

	@Override
	public String transcribeAudio(byte[] audioData, String audioFormat, String language)
			throws AIProviderException {
		// Anthropic does not currently support audio transcription
		throw new AIProviderException(
			"Anthropic does not support audio transcription. " +
			"Please use a different provider (e.g., OpenAI Whisper)."
		);
	}

	@Override
	public AIModelCapabilities getModelCapabilities(String modelName) {
		AIModelCapabilities capabilities = new AIModelCapabilities();
		capabilities.setModelName(modelName != null ? modelName : DEFAULT_MODEL);
		capabilities.setProviderType(getProviderType());

		// Claude 3 family capabilities
		if (modelName == null || modelName.startsWith("claude-3")) {
			capabilities.setMaxContextLength(200000); // 200K context window
			capabilities.setSupportsStreaming(true);
			capabilities.setSupportsFunctions(true); // Claude supports tools
			capabilities.setSupportsVision(true); // Claude 3 supports vision
			capabilities.setSupportsAudio(false);
			capabilities.setSupportsEmbeddings(false);
		}

		return capabilities;
	}

	@Override
	public List<String> getSupportedModels() {
		return List.of(
			"claude-3-opus-20240229",
			"claude-3-sonnet-20240229",
			"claude-3-haiku-20240307",
			"claude-3-5-sonnet-20240620"
		);
	}

	@Override
	public AIHealthStatus checkHealth() {
		AIHealthStatus status = new AIHealthStatus();

		try {
			long startTime = System.currentTimeMillis();

			// Simple health check using minimal request
			MessageCreateParams params = MessageCreateParams.builder()
				.model("claude-3-haiku-20240307")
				.maxTokens(10)
				.addMessage(MessageCreateParams.Message.builder()
					.role(MessageCreateParams.Message.Role.USER)
					.content("test")
					.build())
				.build();

			client.messages().create(params);

			long responseTime = System.currentTimeMillis() - startTime;

			status.setHealthy(true);
			status.setStatus("Healthy");
			status.setMessage("Anthropic API is responding normally");
			status.setResponseTimeMs(responseTime);

		} catch (Exception e) {
			status.setHealthy(false);
			status.setStatus("Unhealthy");
			status.setMessage("Health check failed: " + e.getMessage());
		}

		return status;
	}

	@Override
	public boolean testConnection() {
		try {
			AIHealthStatus health = checkHealth();
			return health.isHealthy();
		} catch (Exception e) {
			log.warning("Connection test failed: " + e.getMessage());
			return false;
		}
	}

	@Override
	public AIRateLimitStatus getRateLimitStatus() {
		// Anthropic includes rate limit info in response headers
		// This would require tracking from last request
		AIRateLimitStatus status = new AIRateLimitStatus();
		status.setRemainingRequests(-1); // Unknown
		status.setLimitRequests(-1); // Unknown
		status.setResetTimeMs(System.currentTimeMillis() + 60000); // Estimate 1 minute
		return status;
	}

	@Override
	public double estimateCost(AIRequest request) {
		String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;
		ModelPricing pricing = MODEL_PRICING.get(model);

		if (pricing == null) {
			log.warning("Unknown model for cost estimation: " + model);
			return 0.0;
		}

		// Estimate input tokens (rough approximation: 4 chars per token)
		int estimatedInputTokens = 0;
		if (request.getSystemPrompt() != null) {
			estimatedInputTokens += request.getSystemPrompt().length() / 4;
		}
		if (request.getMessages() != null) {
			for (AIMessage msg : request.getMessages()) {
				if (msg.getContent() != null) {
					estimatedInputTokens += msg.getContent().length() / 4;
				}
			}
		}

		// Estimate output tokens
		int estimatedOutputTokens = request.getMaxTokens() != null ?
			request.getMaxTokens() : 1024;

		// Calculate cost
		double inputCost = (estimatedInputTokens / 1_000_000.0) * pricing.inputPer1M;
		double outputCost = (estimatedOutputTokens / 1_000_000.0) * pricing.outputPer1M;

		return inputCost + outputCost;
	}

	@Override
	public void shutdown() {
		log.info("Shutting down Anthropic provider");
		this.ready = false;
		// SDK client doesn't require explicit shutdown
	}

	@Override
	public boolean isReady() {
		return ready;
	}

	/**
	 * Convert AIMessage to Anthropic SDK Message
	 */
	private MessageCreateParams.Message convertMessage(AIMessage msg) {
		MessageCreateParams.Message.Builder builder = MessageCreateParams.Message.builder();

		// Map role
		if ("user".equalsIgnoreCase(msg.getRole())) {
			builder.role(MessageCreateParams.Message.Role.USER);
		} else if ("assistant".equalsIgnoreCase(msg.getRole())) {
			builder.role(MessageCreateParams.Message.Role.ASSISTANT);
		}

		// Set content
		builder.content(msg.getContent());

		return builder.build();
	}

	/**
	 * Convert AIFunction to Anthropic SDK Tool
	 */
	private MessageCreateParams.Tool convertFunction(AIFunction function) {
		return MessageCreateParams.Tool.builder()
			.name(function.getName())
			.description(function.getDescription())
			.inputSchema(function.getParameters())
			.build();
	}

	/**
	 * Extract text from delta event
	 */
	private String extractDeltaText(MessageStreamEvent.ContentBlockDelta delta) {
		try {
			// Access delta content - actual implementation depends on SDK API
			return delta.toString(); // Placeholder - needs actual SDK API
		} catch (Exception e) {
			log.warning("Failed to extract delta text: " + e.getMessage());
			return "";
		}
	}

	/**
	 * Parse Anthropic Message into AIResponse
	 */
	private AIResponse parseResponse(Message response, long startTime) {
		AIResponse aiResponse = new AIResponse();

		try {
			// Extract content from text blocks
			StringBuilder content = new StringBuilder();
			for (Object block : response.content()) {
				if (block instanceof TextBlock) {
					TextBlock textBlock = (TextBlock) block;
					content.append(textBlock.text());
				}
			}
			aiResponse.setContent(content.toString());

			// Model
			aiResponse.setModel(response.model());

			// Token usage
			Usage usage = response.usage();
			if (usage != null) {
				AITokenUsage tokenUsage = new AITokenUsage();
				tokenUsage.setPromptTokens((int) usage.inputTokens());
				tokenUsage.setCompletionTokens((int) usage.outputTokens());
				tokenUsage.setTotalTokens(
					tokenUsage.getPromptTokens() + tokenUsage.getCompletionTokens()
				);
				aiResponse.setTokenUsage(tokenUsage);

				// Calculate cost
				String model = aiResponse.getModel();
				ModelPricing pricing = MODEL_PRICING.get(model);
				if (pricing != null) {
					double cost = (tokenUsage.getPromptTokens() / 1_000_000.0) * pricing.inputPer1M +
						(tokenUsage.getCompletionTokens() / 1_000_000.0) * pricing.outputPer1M;
					aiResponse.setCostUSD(cost);
				}
			}

			// Processing time
			aiResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);

			// Choices
			List<String> choices = new ArrayList<>();
			choices.add(aiResponse.getContent());
			aiResponse.setChoices(choices);

		} catch (Exception e) {
			log.severe("Error parsing response: " + e.getMessage());
			aiResponse.setErrorMessage("Failed to parse response: " + e.getMessage());
			aiResponse.setErrorCode("PARSE_ERROR");
		}

		return aiResponse;
	}
}