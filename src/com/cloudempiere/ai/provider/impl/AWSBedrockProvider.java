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
 */
public class AWSBedrockProvider implements IAIProvider {

	private static final CLogger log = CLogger.getCLogger(AWSBedrockProvider.class);

	/** Default model if none specified - using Claude 3.5 Sonnet via Bedrock */
	private static final String DEFAULT_MODEL = "anthropic.claude-3-5-sonnet-20240620-v1:0";

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
		// AWS Bedrock supports tool use, but implementation varies by model
		// For now, fall back to standard text generation
		// TODO: Implement tool/function calling for compatible models
		log.warning("Function calling not yet implemented for AWS Bedrock provider. " +
				   "Falling back to standard text generation.");
		return generateText(request);
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
		// Some Bedrock models support tool use, but not implemented yet
		return false;
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
}