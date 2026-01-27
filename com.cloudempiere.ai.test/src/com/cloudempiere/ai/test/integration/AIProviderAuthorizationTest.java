/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.test.integration;

import com.cloudempiere.ai.test.categories.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.compiere.util.Env;
import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.model.X_AIG_Provider;
import com.cloudempiere.ai.provider.langchain4j.LangChain4jProviderFactory;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/**
 * Unit tests for AI Provider Authorization.
 *
 * Tests the ability to create connections for all AI provider types
 * (Anthropic, AWS Bedrock, Ollama) configured in the database and verify
 * they can establish successful connections.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>Provider configurations can be loaded from the AIG_Provider table</li>
 *   <li>LangChain4j models can be created for each configured provider</li>
 *   <li>Connection/authorization is successful using credentials from database</li>
 * </ul>
 *
 * <p>Prerequisites:
 * <ul>
 *   <li>AI Providers must be configured in the AIG_Provider table</li>
 *   <li>API keys must be stored in the APIKey column</li>
 *   <li>For Ollama: Ollama server must be running locally</li>
 *   <li>For AWS Bedrock: AWS credentials must be configured in environment</li>
 * </ul>
 *
 * @author CloudEmpiere
 * @version 1.0
 */
@IntegrationTest
@DisplayName("AI Provider Authorization Tests")
class AIProviderAuthorizationTest extends AbstractTestCase {

    private static final String OLLAMA_BASE_URL = "http://localhost:11434";

    // Environment variable keys for API keys (fallback when not in DB)
    private static final String ENV_ANTHROPIC_API_KEY = "ANTHROPIC_API_KEY";
    private static final String ENV_OPENAI_API_KEY = "OPENAI_API_KEY";

    @BeforeEach
    @Override
    protected void init(TestInfo testInfo) {
        super.init(testInfo);
    }

    /**
     * Helper to get or create an Anthropic provider for testing.
     * First tries to load from database, then falls back to environment variable.
     */
    private MAIProvider getOrCreateAnthropicProvider() {
        // Try to load from database first
        MAIProvider provider = MAIProvider.getByType(
            Env.getCtx(),
            X_AIG_Provider.AIGPROVIDERTYPE_AnthropicClaude,
            getTrxName()
        );

        if (provider != null) {
            return provider;
        }

        // Fall back to creating a temporary provider using environment variable
        String apiKey = System.getenv(ENV_ANTHROPIC_API_KEY);
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }

        // Create temporary provider (not saved to DB)
        provider = new MAIProvider(Env.getCtx(), 0, getTrxName());
        provider.setName("Test Anthropic Provider");
        provider.setAIGProviderType(X_AIG_Provider.AIGPROVIDERTYPE_AnthropicClaude);
        provider.setAPIKey(apiKey);
        provider.setModelName("claude-3-5-haiku-20241022");
        provider.setIsDefault(false);
        // Note: Not saving to DB - this is a transient test object

        return provider;
    }

    /**
     * Helper to get or create a Bedrock provider for testing.
     */
    private MAIProvider getOrCreateBedrockProvider() {
        // Try to load from database first
        MAIProvider provider = MAIProvider.getByType(
            Env.getCtx(),
            X_AIG_Provider.AIGPROVIDERTYPE_AWSBedrock,
            getTrxName()
        );

        if (provider != null) {
            return provider;
        }

        // Check if AWS credentials are configured
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String profile = System.getenv("AWS_PROFILE");

        if ((accessKey == null || secretKey == null) && profile == null) {
            return null;
        }

        // Create temporary provider (not saved to DB)
        provider = new MAIProvider(Env.getCtx(), 0, getTrxName());
        provider.setName("Test Bedrock Provider");
        provider.setAIGProviderType(X_AIG_Provider.AIGPROVIDERTYPE_AWSBedrock);
        provider.setModelName("anthropic.claude-3-haiku-20240307-v1:0");
        provider.setIsDefault(false);

        return provider;
    }

    /**
     * Helper to get or create an Ollama provider for testing.
     */
    private MAIProvider getOrCreateOllamaProvider() {
        // Try to load from database first
        MAIProvider provider = MAIProvider.getByType(
            Env.getCtx(),
            X_AIG_Provider.AIGPROVIDERTYPE_Ollama,
            getTrxName()
        );

        if (provider != null) {
            return provider;
        }

        // Check if Ollama is running
        if (!isOllamaRunning()) {
            return null;
        }

        // Create temporary provider (not saved to DB)
        provider = new MAIProvider(Env.getCtx(), 0, getTrxName());
        provider.setName("Test Ollama Provider");
        provider.setAIGProviderType(X_AIG_Provider.AIGPROVIDERTYPE_Ollama);
        provider.setModelName("llama3.2");
        provider.setIsDefault(false);

        return provider;
    }

    private boolean isOllamaRunning() {
        try {
            java.net.URL url = new java.net.URL(OLLAMA_BASE_URL);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setRequestMethod("GET");
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Nested
    @IntegrationTest
@DisplayName("Anthropic Claude Provider Tests")
    class AnthropicProviderTests {

        @Test
        @IntegrationTest
@DisplayName("Should create Anthropic ChatLanguageModel with valid API key")
        void shouldCreateAnthropicModel() {
            // Get provider from DB or create from env var
            MAIProvider provider = getOrCreateAnthropicProvider();

            // Skip if no provider available (neither in DB nor env var)
            assumeTrue(provider != null,
                "Skipping: No Anthropic provider in DB and ANTHROPIC_API_KEY env var not set");

            // Skip if no API key configured
            String apiKey = provider.getAPIKey();
            assumeTrue(apiKey != null && !apiKey.isEmpty(),
                "Skipping: Anthropic provider has no API key configured");

            // Create model - this validates the API key format and configuration
            ChatLanguageModel model = assertDoesNotThrow(
                () -> LangChain4jProviderFactory.create(provider),
                "Should create Anthropic ChatLanguageModel without exception"
            );

            assertNotNull(model, "ChatLanguageModel should not be null");
            assertTrue(model.getClass().getName().contains("Anthropic"),
                "Model should be an Anthropic implementation");
        }

        @Test
        @IntegrationTest
@DisplayName("Should create Anthropic StreamingChatLanguageModel with valid API key")
        void shouldCreateAnthropicStreamingModel() {
            MAIProvider provider = getOrCreateAnthropicProvider();

            assumeTrue(provider != null,
                "Skipping: No Anthropic provider in DB and ANTHROPIC_API_KEY env var not set");

            String apiKey = provider.getAPIKey();
            assumeTrue(apiKey != null && !apiKey.isEmpty(),
                "Skipping: Anthropic provider has no API key configured");

            StreamingChatLanguageModel model = assertDoesNotThrow(
                () -> LangChain4jProviderFactory.createStreaming(provider),
                "Should create Anthropic StreamingChatLanguageModel without exception"
            );

            assertNotNull(model, "StreamingChatLanguageModel should not be null");
        }

        @Test
        @IntegrationTest
@DisplayName("Should make successful API call to Anthropic")
        void shouldMakeSuccessfulApiCall() {
            MAIProvider provider = getOrCreateAnthropicProvider();

            assumeTrue(provider != null,
                "Skipping: No Anthropic provider in DB and ANTHROPIC_API_KEY env var not set");

            String apiKey = provider.getAPIKey();
            assumeTrue(apiKey != null && !apiKey.isEmpty(),
                "Skipping: Anthropic provider has no API key configured");

            ChatLanguageModel model = LangChain4jProviderFactory.create(provider);

            // Make a minimal API call to verify authorization
            String response = assertDoesNotThrow(
                () -> model.generate("Say 'OK' and nothing else."),
                "Should make successful API call to Anthropic"
            );

            assertNotNull(response, "Response should not be null");
            assertTrue(response.length() > 0, "Response should not be empty");
        }
    }

    @Nested
    @IntegrationTest
@DisplayName("AWS Bedrock Provider Tests")
    class BedrockProviderTests {

        @Test
        @IntegrationTest
@DisplayName("Should create Bedrock ChatLanguageModel with AWS credentials")
        void shouldCreateBedrockModel() {
            // Get provider from DB or create from env
            MAIProvider provider = getOrCreateBedrockProvider();

            // Skip if no provider available
            assumeTrue(provider != null,
                "Skipping: No Bedrock provider in DB and AWS credentials not configured");

            // Create model - this validates AWS credentials and region
            ChatLanguageModel model = assertDoesNotThrow(
                () -> LangChain4jProviderFactory.create(provider),
                "Should create Bedrock ChatLanguageModel without exception"
            );

            assertNotNull(model, "ChatLanguageModel should not be null");
        }

        @Test
        @IntegrationTest
@DisplayName("Should create Bedrock StreamingChatLanguageModel with AWS credentials")
        void shouldCreateBedrockStreamingModel() {
            MAIProvider provider = getOrCreateBedrockProvider();

            assumeTrue(provider != null,
                "Skipping: No Bedrock provider in DB and AWS credentials not configured");

            StreamingChatLanguageModel model = assertDoesNotThrow(
                () -> LangChain4jProviderFactory.createStreaming(provider),
                "Should create Bedrock StreamingChatLanguageModel without exception"
            );

            assertNotNull(model, "StreamingChatLanguageModel should not be null");
        }

        @Test
        @IntegrationTest
@DisplayName("Should make successful API call to AWS Bedrock")
        void shouldMakeSuccessfulBedrockApiCall() {
            MAIProvider provider = getOrCreateBedrockProvider();

            assumeTrue(provider != null,
                "Skipping: No Bedrock provider in DB and AWS credentials not configured");

            ChatLanguageModel model = LangChain4jProviderFactory.create(provider);

            // Make a minimal API call to verify authorization
            String response = assertDoesNotThrow(
                () -> model.generate("Say 'OK' and nothing else."),
                "Should make successful API call to Bedrock"
            );

            assertNotNull(response, "Response should not be null");
            assertTrue(response.length() > 0, "Response should not be empty");
        }
    }

    @Nested
    @IntegrationTest
@DisplayName("Ollama Provider Tests")
    class OllamaProviderTests {

        @Test
        @IntegrationTest
@DisplayName("Should create Ollama ChatLanguageModel")
        void shouldCreateOllamaModel() {
            // Get provider from DB or create if Ollama is running
            MAIProvider provider = getOrCreateOllamaProvider();

            // Skip if no provider available
            assumeTrue(provider != null,
                "Skipping: No Ollama provider in DB and Ollama is not running at " + OLLAMA_BASE_URL);

            // Create model - this validates Ollama configuration
            ChatLanguageModel model = assertDoesNotThrow(
                () -> LangChain4jProviderFactory.create(provider, null, OLLAMA_BASE_URL),
                "Should create Ollama ChatLanguageModel without exception"
            );

            assertNotNull(model, "ChatLanguageModel should not be null");
            assertTrue(model.getClass().getName().contains("Ollama"),
                "Model should be an Ollama implementation");
        }

        @Test
        @IntegrationTest
@DisplayName("Should make successful API call to Ollama")
        void shouldMakeSuccessfulOllamaApiCall() {
            MAIProvider provider = getOrCreateOllamaProvider();

            assumeTrue(provider != null,
                "Skipping: No Ollama provider in DB and Ollama is not running at " + OLLAMA_BASE_URL);

            // Get model name from provider configuration
            String modelName = provider.getModelName();
            if (modelName == null || modelName.isEmpty()) {
                modelName = "llama3.2";  // Default model
            }

            assumeTrue(isModelAvailable(modelName),
                "Skipping: Model '" + modelName + "' not available in Ollama");

            ChatLanguageModel model = LangChain4jProviderFactory.create(provider, null, OLLAMA_BASE_URL);

            // Make a minimal API call to verify connection
            String response = assertDoesNotThrow(
                () -> model.generate("Say 'OK' and nothing else."),
                "Should make successful API call to Ollama"
            );

            assertNotNull(response, "Response should not be null");
            assertTrue(response.length() > 0, "Response should not be empty");
        }

        private boolean isModelAvailable(String modelName) {
            try {
                java.net.URL url = new java.net.URL(OLLAMA_BASE_URL + "/api/tags");
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    // Simple check if model name appears in response
                    return response.toString().contains(modelName);
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
    }

    @Nested
    @IntegrationTest
@DisplayName("Any Available Provider Connection Test")
    class AnyProviderConnectionTest {

        @Test
        @IntegrationTest
@DisplayName("Should connect to any available AI provider")
        void shouldConnectToAnyAvailableProvider() {
            // Try to get any provider (DB first, then env fallbacks)
            MAIProvider provider = getOrCreateAnthropicProvider();
            if (provider == null) {
                provider = getOrCreateBedrockProvider();
            }
            if (provider == null) {
                provider = getOrCreateOllamaProvider();
            }

            // Skip if no provider available at all
            assumeTrue(provider != null,
                "Skipping: No AI provider available. Configure one in DB or set ANTHROPIC_API_KEY/AWS credentials/run Ollama");

            String providerType = provider.getAIGProviderType();
            assertNotNull(providerType, "Provider type should not be null");

            // Create model from configuration
            final MAIProvider finalProvider = provider;
            ChatLanguageModel model;

            if (X_AIG_Provider.AIGPROVIDERTYPE_Ollama.equals(providerType)) {
                model = assertDoesNotThrow(
                    () -> LangChain4jProviderFactory.create(finalProvider, null, OLLAMA_BASE_URL),
                    "Should create ChatLanguageModel for provider without exception"
                );
            } else {
                model = assertDoesNotThrow(
                    () -> LangChain4jProviderFactory.create(finalProvider),
                    "Should create ChatLanguageModel for provider without exception"
                );
            }

            assertNotNull(model, "ChatLanguageModel should not be null");

            // Make a minimal API call to verify authorization
            String response = assertDoesNotThrow(
                () -> model.generate("Say 'OK' and nothing else."),
                "Should make successful API call to provider"
            );

            assertNotNull(response, "Response should not be null");
            assertTrue(response.length() > 0, "Response should not be empty");

            System.out.println("Successfully connected to " + providerType + " provider");
        }
    }
}
