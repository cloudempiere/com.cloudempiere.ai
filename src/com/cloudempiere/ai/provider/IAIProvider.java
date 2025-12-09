package com.cloudempiere.ai.provider;

import java.util.List;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.dto.AIFunction;
import com.cloudempiere.ai.provider.dto.AIHealthStatus;
import com.cloudempiere.ai.provider.dto.AIModelCapabilities;
import com.cloudempiere.ai.provider.dto.AIRateLimitStatus;
import com.cloudempiere.ai.provider.dto.AIRequest;
import com.cloudempiere.ai.provider.dto.AIResponse;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;

/**
 * Generic interface for AI providers (OpenAI, Anthropic, AWS, Google, etc.)
 *
 * Implementation classes are loaded dynamically based on the AIGProviderType
 * field value from the AIG_Provider database table.
 *
 * Each provider implementation must:
 * 1. Implement this interface
 * 2. Have a corresponding AIGProviderType value in AD_Ref_List
 * 3. Be registered in IAIProviderFactory
 *
 * @author Cloudempiere
 * @version 1.0
 */
public interface IAIProvider {

    /**
     * Initialize the provider with configuration from database
     * Called once when provider is loaded
     *
     * @param provider MAIProvider model containing configuration
     * @throws AIProviderException if initialization fails
     */
    void initialize(MAIProvider provider) throws AIProviderException;

    /**
     * Get provider type identifier
     * Must match the AIGProviderType value from the database
     *
     * Example: "OPENAI", "ANTHROPIC", "AWS_BEDROCK"
     *
     * @return provider type identifier matching AD_Ref_List.Value
     */
    String getProviderType();

    /**
     * Get provider display name for UI
     *
     * @return human-readable provider name
     */
    String getProviderName();

    /**
     * Get API version supported by this implementation
     *
     * @return API version string (e.g., "v1", "2023-01-01")
     */
    String getAPIVersion();

    // ========================================================================
    // CAPABILITY CHECKS
    // ========================================================================

    /**
     * Check if provider supports text generation (chat/completion)
     *
     * @return true if text generation is supported
     */
    boolean supportsTextGeneration();

    /**
     * Check if provider supports embeddings/vector generation
     *
     * @return true if embeddings are supported
     */
    boolean supportsEmbeddings();

    /**
     * Check if provider supports vision (image analysis)
     *
     * @return true if vision is supported
     */
    boolean supportsVision();

    /**
     * Check if provider supports audio transcription
     *
     * @return true if audio transcription is supported
     */
    boolean supportsAudio();

    /**
     * Check if provider supports streaming responses
     *
     * @return true if streaming is supported
     */
    boolean supportsStreaming();

    /**
     * Check if provider supports function/tool calling
     *
     * @return true if function calling is supported
     */
    boolean supportsFunctionCalling();

    /**
     * Get list of supported models for this provider
     *
     * @return list of model identifiers (e.g., ["gpt-4", "gpt-3.5-turbo"])
     */
    List<String> getSupportedModels();

    /**
     * Get capabilities for a specific model
     *
     * @param modelName model identifier
     * @return capabilities object describing model features
     */
    AIModelCapabilities getModelCapabilities(String modelName);

    // ========================================================================
    // TEXT GENERATION (PRIMARY FUNCTIONALITY)
    // ========================================================================

    /**
     * Generate text completion/chat response (synchronous)
     *
     * This is the main method for AI text generation. It handles:
     * - Single-turn completions
     * - Multi-turn conversations
     * - System prompts
     * - Temperature/sampling control
     *
     * @param request AIRequest containing prompt, parameters, and context
     * @return AIResponse with generated text and metadata
     * @throws AIProviderException if generation fails
     */
    AIResponse generateText(AIRequest request) throws AIProviderException;

    /**
     * Generate text completion with streaming (asynchronous)
     * Useful for real-time UI updates as text is generated
     *
     * @param request AIRequest containing prompt and parameters
     * @param callback AIStreamCallback to receive text chunks
     * @throws AIProviderException if streaming fails
     */
    void generateTextStream(AIRequest request, AIStreamCallback callback)
        throws AIProviderException;

    /**
     * Generate text with function/tool calling
     * Allows AI to call external functions and use results in response
     *
     * @param request AIRequest with prompt
     * @param functions List of available functions
     * @return AIResponse with text and function calls
     * @throws AIProviderException if generation fails
     */
    AIResponse generateTextWithFunctions(AIRequest request, List<AIFunction> functions)
        throws AIProviderException;

    // ========================================================================
    // EMBEDDINGS (VECTOR GENERATION)
    // ========================================================================

    /**
     * Generate embeddings/vectors for text
     * Used for semantic search, similarity matching, RAG systems
     *
     * @param text input text to embed
     * @param modelName embedding model to use
     * @return float array representing the embedding vector
     * @throws AIProviderException if embedding generation fails
     */
    float[] generateEmbedding(String text, String modelName)
        throws AIProviderException;

    /**
     * Generate embeddings for multiple texts in batch
     * More efficient than calling generateEmbedding() multiple times
     *
     * @param texts list of input texts
     * @param modelName embedding model to use
     * @return list of embedding vectors
     * @throws AIProviderException if batch embedding fails
     */
    List<float[]> generateEmbeddingsBatch(List<String> texts, String modelName)
        throws AIProviderException;

    // ========================================================================
    // VISION (IMAGE ANALYSIS)
    // ========================================================================

    /**
     * Analyze image and generate description/answer questions
     *
     * @param imageData byte array of image data
     * @param prompt question or instruction about the image
     * @param modelName vision model to use
     * @return AIResponse with image analysis
     * @throws AIProviderException if analysis fails
     */
    AIResponse analyzeImage(byte[] imageData, String prompt, String modelName)
        throws AIProviderException;

    /**
     * Analyze image from URL
     *
     * @param imageUrl URL of the image
     * @param prompt question or instruction
     * @param modelName vision model to use
     * @return AIResponse with analysis
     * @throws AIProviderException if analysis fails
     */
    AIResponse analyzeImageURL(String imageUrl, String prompt, String modelName)
        throws AIProviderException;

    // ========================================================================
    // AUDIO (TRANSCRIPTION)
    // ========================================================================

    /**
     * Transcribe audio to text
     *
     * @param audioData byte array of audio file
     * @param audioFormat format (e.g., "mp3", "wav", "m4a")
     * @param language optional language code (e.g., "en", "es")
     * @return transcribed text
     * @throws AIProviderException if transcription fails
     */
    String transcribeAudio(byte[] audioData, String audioFormat, String language)
        throws AIProviderException;

    // ========================================================================
    // HEALTH & MONITORING
    // ========================================================================

    /**
     * Perform health check on provider
     * Tests connectivity and API key validity
     *
     * @return AIHealthStatus with health information
     */
    AIHealthStatus checkHealth();

    /**
     * Get current rate limit status
     *
     * @return AIRateLimitStatus with remaining requests/tokens
     */
    AIRateLimitStatus getRateLimitStatus();

    /**
     * Test provider with a simple request
     * Used for validation during configuration
     *
     * @return true if test successful, false otherwise
     */
    boolean testConnection();

    // ========================================================================
    // COST & USAGE
    // ========================================================================

    /**
     * Calculate estimated cost for a request
     *
     * @param request AIRequest to estimate
     * @return estimated cost in USD
     */
    double estimateCost(AIRequest request);

    /**
     * Get token count for text (for cost estimation)
     *
     * @param text input text
     * @param modelName model to use for tokenization
     * @return approximate token count
     */
    int estimateTokenCount(String text, String modelName);

    // ========================================================================
    // LIFECYCLE
    // ========================================================================

    /**
     * Shutdown provider and release resources
     * Called when provider is being unloaded
     */
    void shutdown();

    /**
     * Check if provider is initialized and ready
     *
     * @return true if ready to process requests
     */
    boolean isReady();

    /**
     * Get last error message (if any)
     *
     * @return error message or null
     */
    String getLastError();
}
