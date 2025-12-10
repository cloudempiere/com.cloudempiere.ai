package com.cloudempiere.ai.provider.satellite;

/**
 * Constants for Satellite AI Provider integration.
 *
 * @author Cloudempiere
 * @version 0.22.0
 * @since ADR-042 Satellite AI Provider Integration
 */
public final class SatelliteProviderConstants {

    private SatelliteProviderConstants() {
        // Utility class
    }

    // ========================================================================
    // Provider Type
    // ========================================================================

    /**
     * Provider type code for Satellite AI Service.
     * Must match AD_Ref_List value for AIG_ProviderType reference.
     */
    public static final String PROVIDER_TYPE = "SAT";

    /**
     * Provider type name for display.
     */
    public static final String PROVIDER_NAME = "Quarkus Satellite";

    // ========================================================================
    // API Endpoints
    // ========================================================================

    /** Chat completions endpoint (OpenAI-compatible) */
    public static final String ENDPOINT_CHAT = "/v1/chat/completions";

    /** Embeddings endpoint (OpenAI-compatible) */
    public static final String ENDPOINT_EMBEDDINGS = "/v1/embeddings";

    /** Health check endpoint */
    public static final String ENDPOINT_HEALTH = "/health";

    /** Liveness probe endpoint */
    public static final String ENDPOINT_HEALTH_LIVE = "/health/live";

    /** Readiness probe endpoint */
    public static final String ENDPOINT_HEALTH_READY = "/health/ready";

    /** Metrics endpoint (Prometheus format) */
    public static final String ENDPOINT_METRICS = "/metrics";

    // ========================================================================
    // Default Configuration
    // ========================================================================

    /** Default satellite port for development */
    public static final int DEFAULT_PORT = 8090;

    /** Default satellite URL for development */
    public static final String DEFAULT_URL = "http://localhost:" + DEFAULT_PORT;

    /** Default model to use */
    public static final String DEFAULT_MODEL = "claude-sonnet-4";

    /** Default timeout in seconds */
    public static final int DEFAULT_TIMEOUT_SECONDS = 120;

    /** Health check timeout in milliseconds */
    public static final int HEALTH_CHECK_TIMEOUT_MS = 5000;

    // ========================================================================
    // HTTP Headers for iDempiere Context
    // ========================================================================

    /** Header for AD_Client_ID */
    public static final String HEADER_CLIENT_ID = "X-iDempiere-Client-ID";

    /** Header for AD_Org_ID */
    public static final String HEADER_ORG_ID = "X-iDempiere-Org-ID";

    /** Header for AD_User_ID */
    public static final String HEADER_USER_ID = "X-iDempiere-User-ID";

    /** Header for AD_Role_ID */
    public static final String HEADER_ROLE_ID = "X-iDempiere-Role-ID";

    /** Header for session ID */
    public static final String HEADER_SESSION_ID = "X-iDempiere-Session-ID";

    /** Header for AD_Window_ID (optional) */
    public static final String HEADER_WINDOW_ID = "X-iDempiere-Window-ID";

    /** Header for AD_Tab_ID (optional) */
    public static final String HEADER_TAB_ID = "X-iDempiere-Tab-ID";

    /** Header for language */
    public static final String HEADER_LANGUAGE = "X-iDempiere-Language";

    /** Header for conversation ID */
    public static final String HEADER_CONVERSATION_ID = "X-iDempiere-Conversation-ID";

    // ========================================================================
    // Response Headers from Satellite
    // ========================================================================

    /** Header indicating mock mode */
    public static final String HEADER_MOCK = "X-Satellite-Mock";

    /** Header for satellite version */
    public static final String HEADER_VERSION = "X-Satellite-Version";

    /** Header for upstream provider used */
    public static final String HEADER_PROVIDER = "X-Satellite-Provider";

    // ========================================================================
    // Model Name Prefixes for Routing
    // ========================================================================

    /** Claude models (Anthropic direct) */
    public static final String MODEL_PREFIX_CLAUDE = "claude-";

    /** Anthropic models via Bedrock */
    public static final String MODEL_PREFIX_BEDROCK = "anthropic.";

    /** OpenAI GPT models */
    public static final String MODEL_PREFIX_GPT = "gpt-";

    /** Llama models via Ollama */
    public static final String MODEL_PREFIX_LLAMA = "llama";

    /** Mistral models via Ollama */
    public static final String MODEL_PREFIX_MISTRAL = "mistral";

    // ========================================================================
    // Error Codes
    // ========================================================================

    /** Satellite service unavailable */
    public static final String ERROR_UNAVAILABLE = "SATELLITE_UNAVAILABLE";

    /** Authentication failed */
    public static final String ERROR_AUTH_FAILED = "SATELLITE_AUTH_FAILED";

    /** Invalid request */
    public static final String ERROR_INVALID_REQUEST = "SATELLITE_INVALID_REQUEST";

    /** Upstream provider error */
    public static final String ERROR_UPSTREAM = "SATELLITE_UPSTREAM_ERROR";

    /** Rate limit exceeded */
    public static final String ERROR_RATE_LIMIT = "SATELLITE_RATE_LIMIT";

    /** Context validation failed */
    public static final String ERROR_CONTEXT = "SATELLITE_CONTEXT_ERROR";
}
