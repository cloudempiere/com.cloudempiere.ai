package com.cloudempiere.ai.provider.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.AIProviderException;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.dto.AIFunction;
import com.cloudempiere.ai.provider.dto.AIFunctionCall;
import com.cloudempiere.ai.provider.dto.AIHealthStatus;
import com.cloudempiere.ai.provider.dto.AIMessage;
import com.cloudempiere.ai.provider.dto.AIModelCapabilities;
import com.cloudempiere.ai.provider.dto.AIRateLimitStatus;
import com.cloudempiere.ai.provider.dto.AIRequest;
import com.cloudempiere.ai.provider.dto.AIResponse;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;
import com.cloudempiere.ai.provider.dto.AITokenUsage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDelta;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolResultBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolResultContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;

/**
 * AWS Bedrock AI Provider Implementation
 *
 * Implements the IAIProvider interface for AWS Bedrock service.
 * Supports various foundation models available through AWS Bedrock including:
 * - Anthropic Claude (via Bedrock)
 * - Amazon Titan
 * - AI21 Jurassic
 * - Meta Llama
 * - Cohere Command
 *
 * API Documentation: https://docs.aws.amazon.com/bedrock/latest/userguide/
 * Uses AWS SDK for Java v2
 *
 * @author Cloudempiere
 * @version 1.0
 * @deprecated Since v0.9.0. Use {@link com.cloudempiere.ai.provider.langchain4j.LangChain4jProviderFactory}
 *             with LangChain4j's BedrockChatModel instead. See ADR-002 for migration guide.
 */
@Deprecated(since = "0.9.0", forRemoval = true)
public class AWSBedrockProvider implements IAIProvider {

	private static final CLogger log = CLogger.getCLogger(AWSBedrockProvider.class);

	/** Default model if none specified */
	private static final String DEFAULT_MODEL = "anthropic.claude-3-haiku-20240307-v1:0";

	/** Default AWS region if not specified in provider config */
	private static final String DEFAULT_REGION = "eu-west-1";

	/** Provider configuration from database */
	private MAIProvider providerConfig;

	/** AWS Bedrock Runtime client (sync) */
	private BedrockRuntimeClient bedrockClient;

	/** AWS Bedrock Runtime async client (for streaming) */
	private BedrockRuntimeAsyncClient bedrockAsyncClient;

	/** Provider ready flag */
	private boolean ready = false;

	/** Last error message */
	private String lastError = null;

	/**
	 * Model pricing per 1M tokens (USD) for common Bedrock models
	 * Note: Pricing varies by region and can change. These are approximate values.
	 * Updated as of January 2025
	 */
	private static final Map<String, ModelPricing> MODEL_PRICING = new HashMap<String, ModelPricing>() {
		
		private static final long serialVersionUID = -1429857427450328751L;

	{
		// Anthropic Claude models via Bedrock
		put("anthropic.claude-3-opus-20240229-v1:0", new ModelPricing(15.0, 75.0));
		put("anthropic.claude-3-sonnet-20240229-v1:0", new ModelPricing(3.0, 15.0));
		put("anthropic.claude-3-haiku-20240307-v1:0", new ModelPricing(0.25, 1.25));
		put("anthropic.claude-3-5-sonnet-20240620-v1:0", new ModelPricing(3.0, 15.0));

		// Amazon Titan models
		put("amazon.titan-text-express-v1", new ModelPricing(0.8, 1.6));
		put("amazon.titan-text-lite-v1", new ModelPricing(0.3, 0.4));

		// Meta Llama models
		put("meta.llama3-70b-instruct-v1:0", new ModelPricing(2.65, 3.5));
		put("meta.llama3-8b-instruct-v1:0", new ModelPricing(0.4, 0.6));

		// Cohere models
		put("cohere.command-text-v14", new ModelPricing(1.5, 2.0));
		put("cohere.command-light-text-v14", new ModelPricing(0.3, 0.6));
	}};

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

		// For AWS Bedrock, APIKey field should contain credentials in format: ACCESS_KEY:SECRET_KEY[:REGION]
		// Alternative: can use IAM role if running on AWS infrastructure
		if (apiKey == null || apiKey.trim().isEmpty()) {
			throw new AIProviderException(
				"API Key (AWS credentials) is required for AWS Bedrock provider: " + provider.getName() +
				". Format: ACCESS_KEY:SECRET_KEY or ACCESS_KEY:SECRET_KEY:REGION"
			);
		}

		try {
			// Parse credentials from API key field
			String[] credentials = apiKey.split(":", 3);
			if (credentials.length < 2) {
				throw new AIProviderException(
					"Invalid AWS credentials format. Expected: ACCESS_KEY:SECRET_KEY[:REGION]"
				);
			}

			String accessKeyId = credentials[0].trim();
			String secretAccessKey = credentials[1].trim();
			String region = credentials.length > 2 ? credentials[2].trim() : DEFAULT_REGION;

			// Create AWS credentials
			AwsCredentials awsCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
			StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCredentials);
			Region awsRegion = Region.of(region);

			// Create HTTP clients explicitly for OSGi environment
			// AWS SDK cannot auto-discover HTTP implementations via SPI in OSGi
			SdkHttpClient syncHttpClient = UrlConnectionHttpClient.builder().build();
			SdkAsyncHttpClient asyncHttpClient = NettyNioAsyncHttpClient.builder().build();

			// Initialize Bedrock Runtime clients (sync and async)
			this.bedrockClient = BedrockRuntimeClient.builder()
				.region(awsRegion)
				.credentialsProvider(credentialsProvider)
				.httpClient(syncHttpClient)
				.build();

			this.bedrockAsyncClient = BedrockRuntimeAsyncClient.builder()
				.region(awsRegion)
				.credentialsProvider(credentialsProvider)
				.httpClient(asyncHttpClient)
				.build();

			// Test connection
			if (!testConnection()) {
				throw new AIProviderException(
					"Failed to connect to AWS Bedrock. Please verify your credentials and region."
				);
			}

			this.ready = true;
			log.info("AWS Bedrock provider initialized successfully: " + provider.getName() +
					" (Region: " + region + ")");

		} catch (AIProviderException e) {
			throw e;
		} catch (Exception e) {
			log.severe("Failed to initialize AWS Bedrock provider: " + e.getMessage());
			throw new AIProviderException("Initialization failed: " + e.getMessage(), e);
		}
	}

	@Override
	public String getProviderType() {
		return MAIProvider.AIGPROVIDERTYPE_AWSBedrock;
	}

	@Override
	public String getProviderName() {
		return "AWS Bedrock";
	}

	@Override
	public String getAPIVersion() {
		return "2023-09-30"; // AWS Bedrock Runtime API version
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
			// Build conversation messages
			List<Message> messages = new ArrayList<>();

			if (request.getMessages() != null && !request.getMessages().isEmpty()) {
				for (AIMessage aiMsg : request.getMessages()) {
					messages.add(convertMessage(aiMsg));
				}
			}

			// Build inference configuration
			InferenceConfiguration.Builder inferenceBuilder = InferenceConfiguration.builder()
				.maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096);

			if (request.getTemperature() != null) {
				inferenceBuilder.temperature(request.getTemperature().floatValue());
			}

			if (request.getTopP() != null) {
				inferenceBuilder.topP(request.getTopP().floatValue());
			}

			// Build converse request
			ConverseRequest.Builder requestBuilder = ConverseRequest.builder()
				.modelId(request.getModel() != null ? request.getModel() : DEFAULT_MODEL)
				.messages(messages)
				.inferenceConfig(inferenceBuilder.build());

			// Add system prompt if specified
			if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
				SystemContentBlock systemBlock = SystemContentBlock.builder()
					.text(request.getSystemPrompt())
					.build();
				requestBuilder.system(systemBlock);
			}

			// Execute API call
			ConverseResponse response = bedrockClient.converse(requestBuilder.build());

			// Parse response
			AIResponse aiResponse = parseResponse(response, startTime);

			log.fine("Generated text successfully using model: " +
					(request.getModel() != null ? request.getModel() : DEFAULT_MODEL));

			this.lastError = null;
			return aiResponse;

		} catch (Exception e) {
			this.lastError = "Text generation failed: " + e.getMessage();
			log.severe(this.lastError);
			throw new AIProviderException(this.lastError, e);
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
			// Build conversation messages
			List<Message> messages = new ArrayList<>();

			if (request.getMessages() != null && !request.getMessages().isEmpty()) {
				for (AIMessage aiMsg : request.getMessages()) {
					messages.add(convertMessage(aiMsg));
				}
			}

			// Build inference configuration
			InferenceConfiguration.Builder inferenceBuilder = InferenceConfiguration.builder()
				.maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4096);

			if (request.getTemperature() != null) {
				inferenceBuilder.temperature(request.getTemperature().floatValue());
			}

			// Build streaming request
			ConverseStreamRequest.Builder requestBuilder = ConverseStreamRequest.builder()
				.modelId(request.getModel() != null ? request.getModel() : DEFAULT_MODEL)
				.messages(messages)
				.inferenceConfig(inferenceBuilder.build());

			// Add system prompt if specified
			if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
				SystemContentBlock systemBlock = SystemContentBlock.builder()
					.text(request.getSystemPrompt())
					.build();
				requestBuilder.system(systemBlock);
			}

			// Create streaming response handler using visitor pattern
			ConverseStreamResponseHandler.Visitor visitor = ConverseStreamResponseHandler.Visitor.builder()
				.onContentBlockDelta(event -> {
					// Extract text from delta
					ContentBlockDelta delta = event.delta();
					if (delta != null && delta.text() != null && !delta.text().isEmpty()) {
						callback.onChunk(delta.text());
					}
				})
				.onDefault(event -> {
					// Handle other event types if needed
				})
				.build();

			ConverseStreamResponseHandler handler = ConverseStreamResponseHandler.builder()
				.subscriber(visitor)
				.onComplete(() -> callback.onComplete())
				.onError(error -> {
					// Convert Throwable to Exception for callback
					Exception exception = (error instanceof Exception)
						? (Exception) error
						: new Exception(error);
					callback.onError(exception);
				})
				.build();

			// Execute streaming API call using async client
			bedrockAsyncClient.converseStream(requestBuilder.build(), handler).join();

			this.lastError = null;

		} catch (Exception e) {
			this.lastError = "Streaming text generation failed: " + e.getMessage();
			log.severe(this.lastError);
			callback.onError(e);
			throw new AIProviderException(this.lastError, e);
		}
	}

	@Override
	public AIResponse generateTextWithFunctions(AIRequest request, List<AIFunction> functions)
			throws AIProviderException {
		if (!ready) {
			throw new AIProviderException("Provider not initialized");
		}

		if (request == null) {
			throw new AIProviderException("Request cannot be null");
		}

		long startTime = System.currentTimeMillis();

		try {
			// Build Converse request with tools
			ConverseRequest.Builder requestBuilder = ConverseRequest.builder()
				.modelId(request.getModel() != null ? request.getModel() : DEFAULT_MODEL);

			// Add messages
			List<Message> messages = convertMessages(request.getMessages());

			// AWS Bedrock requires conversation to start with USER role
			// Validate and log if messages is empty or doesn't start with USER
			if (messages.isEmpty()) {
				throw new AIProviderException("AWS Bedrock requires at least one user message in the conversation");
			}

			if (messages.get(0).role() != ConversationRole.USER) {
				log.warning("First message is not USER role: " + messages.get(0).role() +
					". AWS Bedrock requires conversations to start with a user message.");
				throw new AIProviderException(
					"AWS Bedrock requires conversation to start with a user message, but got: " + messages.get(0).role());
			}

			requestBuilder.messages(messages);

			// Add system prompt if provided
			if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
				requestBuilder.system(SystemContentBlock.builder()
					.text(request.getSystemPrompt())
					.build());
			}

			// Add tools if provided
			if (functions != null && !functions.isEmpty()) {
				List<Tool> tools = new ArrayList<>();
				for (AIFunction function : functions) {
					tools.add(convertFunctionToTool(function));
				}

				ToolConfiguration toolConfig = ToolConfiguration.builder()
					.tools(tools)
					.build();
				requestBuilder.toolConfig(toolConfig);

				log.fine("Added " + tools.size() + " tool(s) to AWS Bedrock request");
			}

			// Add inference configuration
			InferenceConfiguration.Builder inferenceBuilder = InferenceConfiguration.builder();
			if (request.getMaxTokens() != null && request.getMaxTokens() > 0) {
				inferenceBuilder.maxTokens(request.getMaxTokens());
			} else {
				inferenceBuilder.maxTokens(4096); // Default
			}

			if (request.getTemperature() != null && request.getTemperature() > 0) {
				inferenceBuilder.temperature(request.getTemperature().floatValue());
			}

			requestBuilder.inferenceConfig(inferenceBuilder.build());

			// Make API call
			ConverseResponse response = bedrockClient.converse(requestBuilder.build());

			// Parse response
			AIResponse aiResponse = parseConverseResponse(response, startTime);

			log.fine("AWS Bedrock response received" +
				(aiResponse.getFunctionCalls() != null && !aiResponse.getFunctionCalls().isEmpty() ?
					" with " + aiResponse.getFunctionCalls().size() + " function call(s)" : ""));

			return aiResponse;

		} catch (Exception e) {
			lastError = e.getMessage();
			log.severe("Failed to generate text with functions via AWS Bedrock: " + e.getMessage());
			throw new AIProviderException("AWS Bedrock API call failed: " + e.getMessage(), e);
		}
	}

	@Override
	public float[] generateEmbedding(String text, String modelName) throws AIProviderException {
		// AWS Bedrock supports embedding models (e.g., Amazon Titan Embeddings)
		// This requires using the InvokeModel API with embedding-specific models
		throw new AIProviderException(
			"Embeddings not yet implemented for AWS Bedrock provider. " +
			"Use Amazon Titan Embeddings models directly via InvokeModel API."
		);
	}

	@Override
	public List<float[]> generateEmbeddingsBatch(List<String> texts, String modelName)
			throws AIProviderException {
		throw new AIProviderException(
			"Batch embeddings not yet implemented for AWS Bedrock provider."
		);
	}

	@Override
	public AIResponse analyzeImage(byte[] imageData, String prompt, String modelName)
			throws AIProviderException {
		// Claude 3 models via Bedrock support vision
		// This would require adding image content blocks to messages
		throw new AIProviderException(
			"Image analysis not yet implemented for AWS Bedrock provider. " +
			"Will be added in future version."
		);
	}

	@Override
	public AIResponse analyzeImageURL(String imageUrl, String prompt, String modelName)
			throws AIProviderException {
		throw new AIProviderException(
			"Image URL analysis not yet implemented for AWS Bedrock provider."
		);
	}

	@Override
	public String transcribeAudio(byte[] audioData, String audioFormat, String language)
			throws AIProviderException {
		throw new AIProviderException(
			"Audio transcription not supported by AWS Bedrock. " +
			"Use Amazon Transcribe service instead."
		);
	}

	@Override
	public boolean supportsTextGeneration() {
		return true;
	}

	@Override
	public boolean supportsEmbeddings() {
		// Bedrock supports embeddings via Titan Embeddings, but not implemented yet
		return false;
	}

	@Override
	public boolean supportsVision() {
		// Claude 3 via Bedrock supports vision, but not implemented yet
		return false;
	}

	@Override
	public boolean supportsAudio() {
		return false; // Use Amazon Transcribe/Polly instead
	}

	@Override
	public boolean supportsStreaming() {
		return true;
	}

	@Override
	public boolean supportsFunctionCalling() {
		// AWS Bedrock Converse API supports tool use for Claude models
		return true;
	}

	@Override
	public List<String> getSupportedModels() {
		List<String> models = new ArrayList<>();

		// Anthropic Claude models
		models.add("anthropic.claude-3-opus-20240229-v1:0");
		models.add("anthropic.claude-3-sonnet-20240229-v1:0");
		models.add("anthropic.claude-3-haiku-20240307-v1:0");
		models.add("anthropic.claude-3-5-sonnet-20240620-v1:0");

		// Amazon Titan models
		models.add("amazon.titan-text-express-v1");
		models.add("amazon.titan-text-lite-v1");

		// Meta Llama models
		models.add("meta.llama3-70b-instruct-v1:0");
		models.add("meta.llama3-8b-instruct-v1:0");

		// Cohere models
		models.add("cohere.command-text-v14");
		models.add("cohere.command-light-text-v14");

		return models;
	}

	@Override
	public AIModelCapabilities getModelCapabilities(String modelName) {
		AIModelCapabilities capabilities = new AIModelCapabilities();

		// Set capabilities based on model
		if (modelName != null && modelName.startsWith("anthropic.claude-3")) {
			capabilities.setSupportsStreaming(true);
			capabilities.setSupportsVision(true); // Claude 3 supports vision
			capabilities.setMaxContextLength(200000); // Claude 3 has 200k context window
			capabilities.setMaxOutputTokens(4096);
			capabilities.setSupportsFunctions(true); // Claude 3 supports tools
		} else {
			// Default capabilities for other models
			capabilities.setSupportsStreaming(true);
			capabilities.setSupportsVision(false);
			capabilities.setMaxContextLength(8192);
			capabilities.setMaxOutputTokens(2048);
			capabilities.setSupportsFunctions(false);
		}

		return capabilities;
	}

	@Override
	public AIHealthStatus checkHealth() {
		AIHealthStatus status = new AIHealthStatus();

		try {
			long startTime = System.currentTimeMillis();

			// Create a minimal test request
			Message testMessage = Message.builder()
				.role(ConversationRole.USER)
				.content(ContentBlock.fromText("test"))
				.build();

			ConverseRequest testRequest = ConverseRequest.builder()
				.modelId("anthropic.claude-3-haiku-20240307-v1:0") // Use fast model for health check
				.messages(testMessage)
				.inferenceConfig(InferenceConfiguration.builder()
					.maxTokens(10)
					.build())
				.build();

			bedrockClient.converse(testRequest);

			long responseTime = System.currentTimeMillis() - startTime;

			status.setHealthy(true);
			status.setStatus("Healthy");
			status.setResponseTimeMs(responseTime);
			status.setMessage("AWS Bedrock connection successful");

		} catch (Exception e) {
			status.setHealthy(false);
			status.setStatus("Unhealthy");
			status.setMessage("Health check failed: " + e.getMessage());
			log.warning("Health check failed: " + e.getMessage());
		}

		return status;
	}

	@Override
	public AIRateLimitStatus getRateLimitStatus() {
		// AWS Bedrock doesn't expose rate limit headers in the same way as other APIs
		// Rate limits are enforced at the account level
		// Return a status indicating limits are not known
		return new AIRateLimitStatus();
	}

	@Override
	public boolean testConnection() {
		try {
			// Simple test to verify credentials and connectivity
			Message testMessage = Message.builder()
				.role(ConversationRole.USER)
				.content(ContentBlock.fromText("test"))
				.build();

			ConverseRequest testRequest = ConverseRequest.builder()
				.modelId("anthropic.claude-3-haiku-20240307-v1:0")
				.messages(testMessage)
				.inferenceConfig(InferenceConfiguration.builder()
					.maxTokens(5)
					.build())
				.build();

			bedrockClient.converse(testRequest);
			return true;
		} catch (Exception e) {
			log.warning("Connection test failed: " + e.getMessage());
			return false;
		}
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

		int estimatedOutputTokens = request.getMaxTokens() != null ?
			request.getMaxTokens() : 1024;

		double inputCost = (estimatedInputTokens / 1_000_000.0) * pricing.inputPer1M;
		double outputCost = (estimatedOutputTokens / 1_000_000.0) * pricing.outputPer1M;

		return inputCost + outputCost;
	}

	@Override
	public int estimateTokenCount(String text, String modelName) {
		// Rough approximation: 4 characters per token
		// More accurate estimation would require model-specific tokenizer
		if (text == null) {
			return 0;
		}
		return text.length() / 4;
	}

	@Override
	public boolean isReady() {
		return ready;
	}

	@Override
	public void shutdown() {
		if (bedrockClient != null) {
			try {
				bedrockClient.close();
				log.info("AWS Bedrock sync client closed successfully");
			} catch (Exception e) {
				log.warning("Error closing Bedrock sync client: " + e.getMessage());
			}
		}
		if (bedrockAsyncClient != null) {
			try {
				bedrockAsyncClient.close();
				log.info("AWS Bedrock async client closed successfully");
			} catch (Exception e) {
				log.warning("Error closing Bedrock async client: " + e.getMessage());
			}
		}
		ready = false;
	}

	@Override
	public String getLastError() {
		return lastError;
	}

	/**
	 * Convert AIMessage to Bedrock Message
	 */
	private Message convertMessage(AIMessage aiMessage) {
		// Validate content is not null or empty
		if (aiMessage.getContent() == null || aiMessage.getContent().trim().isEmpty()) {
			throw new IllegalArgumentException("Message content cannot be null or empty.");
		}

		ConversationRole role;

		// Map role
		switch (aiMessage.getRole().toLowerCase()) {
			case "user":
				role = ConversationRole.USER;
				break;
			case "assistant":
				role = ConversationRole.ASSISTANT;
				break;
			default:
				role = ConversationRole.USER; // Default to user
		}

		// Create content block
		ContentBlock contentBlock = ContentBlock.fromText(aiMessage.getContent());

		return Message.builder()
			.role(role)
			.content(contentBlock)
			.build();
	}

	/**
	 * Parse Bedrock ConverseResponse into AIResponse
	 */
	private AIResponse parseResponse(ConverseResponse response, long startTime) {
		AIResponse aiResponse = new AIResponse();

		try {
			// Extract text content
			if (response.output() != null && response.output().message() != null) {
				StringBuilder content = new StringBuilder();

				for (ContentBlock block : response.output().message().content()) {
					if (block.text() != null) {
						content.append(block.text());
					}
				}

				aiResponse.setContent(content.toString());
			}

			// Set model
			aiResponse.setModel(response.usage() != null ?
				"AWS Bedrock Model" : DEFAULT_MODEL);

			// Set stop reason
			if (response.stopReason() != null) {
				aiResponse.setFinishReason(response.stopReason().toString());
			}

			// Set token usage
			if (response.usage() != null) {
				AITokenUsage tokenUsage = new AITokenUsage();
				tokenUsage.setPromptTokens(response.usage().inputTokens());
				tokenUsage.setCompletionTokens(response.usage().outputTokens());
				tokenUsage.setTotalTokens(
					response.usage().inputTokens() + response.usage().outputTokens()
				);
				aiResponse.setTokenUsage(tokenUsage);

				// Calculate cost
				String model = aiResponse.getModel();
				ModelPricing pricing = MODEL_PRICING.get(model);
				if (pricing != null) {
					double cost = (response.usage().inputTokens() / 1_000_000.0) * pricing.inputPer1M +
								  (response.usage().outputTokens() / 1_000_000.0) * pricing.outputPer1M;
					aiResponse.setCostUSD(cost);
				}
			}

			// Set processing time
			aiResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);

		} catch (Exception e) {
			log.warning("Error parsing response: " + e.getMessage());
			aiResponse.setErrorMessage("Failed to parse response: " + e.getMessage());
		}

		return aiResponse;
	}

	/**
	 * Convert AIFunction to AWS Bedrock Tool
	 */
	private Tool convertFunctionToTool(AIFunction function) {
		// Build tool specification
		ToolSpecification.Builder specBuilder = ToolSpecification.builder()
			.name(function.getName());

		if (function.getDescription() != null) {
			specBuilder.description(function.getDescription());
		}

		// Convert function parameters to AWS Document format
		if (function.getParameters() != null) {
			try {
				// Convert parameters Map to AWS Document
				Document inputSchema = convertMapToDocument(function.getParameters());
				ToolInputSchema toolInputSchema = ToolInputSchema.builder()
					.json(inputSchema)
					.build();
				specBuilder.inputSchema(toolInputSchema);
			} catch (Exception e) {
				log.warning("Failed to convert function parameters: " + e.getMessage());
			}
		}

		return Tool.builder()
			.toolSpec(specBuilder.build())
			.build();
	}

	/**
	 * Convert Map to AWS Document
	 */
	@SuppressWarnings("unchecked")
	private Document convertMapToDocument(Map<String, Object> map) {
		if (map == null) {
			return Document.fromMap(new HashMap<>());
		}

		// AWS SDK Document.fromMap expects Map<String, Document>
		// We need to convert each value to a Document
		Map<String, Document> documentMap = new HashMap<>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Object value = entry.getValue();

			if (value == null) {
				documentMap.put(entry.getKey(), Document.fromNull());
			} else if (value instanceof Map) {
				documentMap.put(entry.getKey(), convertMapToDocument((Map<String, Object>) value));
			} else if (value instanceof List) {
				documentMap.put(entry.getKey(), convertListToDocument((List<?>) value));
			} else if (value instanceof String) {
				documentMap.put(entry.getKey(), Document.fromString((String) value));
			} else if (value instanceof Number) {
				// Convert Number to string representation for Document
				documentMap.put(entry.getKey(), Document.fromNumber(value.toString()));
			} else if (value instanceof Boolean) {
				documentMap.put(entry.getKey(), Document.fromBoolean((Boolean) value));
			} else {
				// Fallback - convert to string
				documentMap.put(entry.getKey(), Document.fromString(value.toString()));
			}
		}

		return Document.fromMap(documentMap);
	}

	/**
	 * Convert List to AWS Document
	 */
	@SuppressWarnings("unchecked")
	private Document convertListToDocument(List<?> list) {
		if (list == null) {
			return Document.fromList(new ArrayList<>());
		}

		List<Document> documentList = new ArrayList<>();
		for (Object item : list) {
			if (item == null) {
				documentList.add(Document.fromNull());
			} else if (item instanceof Map) {
				documentList.add(convertMapToDocument((Map<String, Object>) item));
			} else if (item instanceof List) {
				documentList.add(convertListToDocument((List<?>) item));
			} else if (item instanceof String) {
				documentList.add(Document.fromString((String) item));
			} else if (item instanceof Number) {
				// Convert Number to string representation for Document
				documentList.add(Document.fromNumber(item.toString()));
			} else if (item instanceof Boolean) {
				documentList.add(Document.fromBoolean((Boolean) item));
			} else {
				documentList.add(Document.fromString(item.toString()));
			}
		}

		return Document.fromList(documentList);
	}

	/**
	 * Parse JSON string to AWS Document
	 */
	@SuppressWarnings("unchecked")
	private Document parseJsonToDocument(String jsonString) throws Exception {
		if (jsonString == null || jsonString.trim().isEmpty()) {
			return Document.fromMap(new HashMap<>());
		}

		// Parse JSON string using org.json
		org.json.JSONObject json = new org.json.JSONObject(jsonString);

		// Convert to Map
		Map<String, Object> map = new HashMap<>();
		for (String key : json.keySet()) {
			map.put(key, json.get(key));
		}

		// Convert map to Document
		return convertMapToDocument(map);
	}

	/**
	 * Convert AWS Document to JSON string
	 */
	private String documentToJson(Document document) {
		if (document == null) {
			return "{}";
		}

		// Convert Document back to JSON using its internal representation
		// AWS Document stores data as Map, List, or primitives
		try {
			if (document.isMap()) {
				Map<String, Document> map = document.asMap();
				org.json.JSONObject json = new org.json.JSONObject();
				for (Map.Entry<String, Document> entry : map.entrySet()) {
					json.put(entry.getKey(), documentToJsonValue(entry.getValue()));
				}
				return json.toString();
			} else if (document.isList()) {
				List<Document> list = document.asList();
				org.json.JSONArray jsonArray = new org.json.JSONArray();
				for (Document doc : list) {
					jsonArray.put(documentToJsonValue(doc));
				}
				return jsonArray.toString();
			} else {
				// Primitive value
				return String.valueOf(documentToJsonValue(document));
			}
		} catch (Exception e) {
			log.warning("Failed to convert Document to JSON: " + e.getMessage());
			return "{}";
		}
	}

	/**
	 * Convert Document to Java object for JSON serialization
	 */
	private Object documentToJsonValue(Document document) {
		if (document == null || document.isNull()) {
			return null;
		} else if (document.isString()) {
			return document.asString();
		} else if (document.isNumber()) {
			// asNumber() returns SdkNumber, convert to string first
			String numStr = document.asNumber().toString();
			try {
				if (numStr.contains(".")) {
					return Double.parseDouble(numStr);
				} else {
					return Long.parseLong(numStr);
				}
			} catch (NumberFormatException e) {
				return numStr;
			}
		} else if (document.isBoolean()) {
			return document.asBoolean();
		} else if (document.isMap()) {
			Map<String, Document> map = document.asMap();
			org.json.JSONObject json = new org.json.JSONObject();
			for (Map.Entry<String, Document> entry : map.entrySet()) {
				json.put(entry.getKey(), documentToJsonValue(entry.getValue()));
			}
			return json;
		} else if (document.isList()) {
			List<Document> list = document.asList();
			org.json.JSONArray jsonArray = new org.json.JSONArray();
			for (Document doc : list) {
				jsonArray.put(documentToJsonValue(doc));
			}
			return jsonArray;
		}
		return null;
	}

	/**
	 * Convert list of AIMessages to Bedrock Messages (for function calling)
	 */
	private List<Message> convertMessages(List<AIMessage> aiMessages) {
		List<Message> messages = new ArrayList<>();

		for (AIMessage aiMessage : aiMessages) {
			// Skip null or invalid messages
			if (aiMessage == null) {
				continue;
			}

			// Handle function result messages
			if ("function".equals(aiMessage.getRole())) {
				// Function result - convert to tool result content block
				// Parse the JSON content into a Document
				Document resultDoc;
				try {
					resultDoc = parseJsonToDocument(aiMessage.getContent());
				} catch (Exception e) {
					log.warning("Failed to parse function result as JSON: " + e.getMessage());
					// Fallback to text result
					resultDoc = Document.fromString(aiMessage.getContent());
				}

				ToolResultContentBlock resultContent = ToolResultContentBlock.builder()
					.json(resultDoc)
					.build();

				// Use the stored tool use ID from the function call
				String toolUseId = aiMessage.getFunctionName(); // This should contain the tool use ID
				ToolResultBlock toolResult = ToolResultBlock.builder()
					.toolUseId(toolUseId)
					.content(resultContent)
					.build();

				ContentBlock contentBlock = ContentBlock.fromToolResult(toolResult);

				messages.add(Message.builder()
					.role(ConversationRole.USER) // Tool results come from user
					.content(contentBlock)
					.build());

			} else if (aiMessage.getFunctionCalls() != null && !aiMessage.getFunctionCalls().isEmpty()) {
				// Assistant message with tool calls
				List<ContentBlock> contentBlocks = new ArrayList<>();

				// Add text content if present
				if (aiMessage.getContent() != null && !aiMessage.getContent().trim().isEmpty()) {
					contentBlocks.add(ContentBlock.fromText(aiMessage.getContent()));
				}

				// Add tool use blocks
				for (AIFunctionCall functionCall : aiMessage.getFunctionCalls()) {
					try {
						// Parse the arguments JSON into a Document
						Document inputDoc = parseJsonToDocument(functionCall.getArguments());

						// Use the stored ID if available, otherwise generate new one
						String toolUseId = functionCall.getId();
						if (toolUseId == null || toolUseId.isEmpty()) {
							toolUseId = "tool_" + System.currentTimeMillis();
						}

						ToolUseBlock toolUse = ToolUseBlock.builder()
							.toolUseId(toolUseId)
							.name(functionCall.getName())
							.input(inputDoc)
							.build();

						contentBlocks.add(ContentBlock.fromToolUse(toolUse));
					} catch (Exception e) {
						log.warning("Failed to convert function call: " + e.getMessage());
						e.printStackTrace();
					}
				}

				messages.add(Message.builder()
					.role(ConversationRole.ASSISTANT)
					.content(contentBlocks)
					.build());

			} else {
				// Regular text message
				messages.add(convertMessage(aiMessage));
			}
		}

		return messages;
	}

	/**
	 * Parse ConverseResponse with tool/function call support
	 */
	private AIResponse parseConverseResponse(ConverseResponse response, long startTime) {
		AIResponse aiResponse = new AIResponse();
		aiResponse.setSuccess(true);

		try {
			// Extract content from response
			StringBuilder textContent = new StringBuilder();
			List<AIFunctionCall> functionCalls = new ArrayList<>();

			if (response.output() != null && response.output().message() != null) {
				Message message = response.output().message();

				// Process content blocks
				for (ContentBlock block : message.content()) {
					if (block.text() != null && !block.text().isEmpty()) {
						textContent.append(block.text());
					} else if (block.toolUse() != null) {
						// Extract tool/function call
						ToolUseBlock toolUse = block.toolUse();
						AIFunctionCall functionCall = new AIFunctionCall();
						functionCall.setName(toolUse.name());

						// Store the tool use ID for later reference (needed when sending results back)
						functionCall.setId(toolUse.toolUseId());

						// Convert Document input to JSON string
						if (toolUse.input() != null) {
							// AWS Document needs proper JSON serialization
							functionCall.setArguments(documentToJson(toolUse.input()));
						} else {
							functionCall.setArguments("{}");
						}

						functionCalls.add(functionCall);
						log.fine("Extracted tool call: " + toolUse.name() + " (ID: " + toolUse.toolUseId() + ")");
					}
				}
			}

			aiResponse.setContent(textContent.toString());

			// Add function calls if any
			if (!functionCalls.isEmpty()) {
				aiResponse.setFunctionCalls(functionCalls);
				log.fine("Response contains " + functionCalls.size() + " function call(s)");
			}

			// Set model
			aiResponse.setModel(response.sdkHttpResponse().headers().get("x-amzn-requestid").get(0));

			// Extract token usage
			if (response.usage() != null) {
				AITokenUsage tokenUsage = new AITokenUsage();
				tokenUsage.setPromptTokens(response.usage().inputTokens());
				tokenUsage.setCompletionTokens(response.usage().outputTokens());
				tokenUsage.setTotalTokens(response.usage().totalTokens());
				aiResponse.setTokenUsage(tokenUsage);

				// Calculate cost if pricing available
				String modelId = response.sdkHttpResponse().headers().getOrDefault("x-amzn-bedrock-model-id",
					java.util.Collections.singletonList(DEFAULT_MODEL)).get(0);
				ModelPricing pricing = MODEL_PRICING.get(modelId);
				if (pricing != null) {
					double cost = (response.usage().inputTokens() / 1_000_000.0) * pricing.inputPer1M +
								  (response.usage().outputTokens() / 1_000_000.0) * pricing.outputPer1M;
					aiResponse.setCostUSD(cost);
				}
			}

			// Set processing time
			aiResponse.setProcessingTimeMs(System.currentTimeMillis() - startTime);

		} catch (Exception e) {
			log.warning("Error parsing response: " + e.getMessage());
			aiResponse.setSuccess(false);
			aiResponse.setErrorMessage("Failed to parse response: " + e.getMessage());
		}

		return aiResponse;
	}
}