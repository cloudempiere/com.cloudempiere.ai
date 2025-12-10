package com.cloudempiere.ai.provider.langchain4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.compiere.util.CLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Custom Bedrock Titan embedding model wrapper that explicitly creates the HTTP client.
 *
 * <p>This wrapper solves the OSGi ServiceLoader issue where the AWS SDK cannot find
 * the Apache HTTP client implementation through standard ServiceLoader discovery.
 *
 * @author Cloudempiere
 * @version 0.20.0
 */
public class BedrockEmbeddingModelWrapper implements EmbeddingModel {

    private static final CLogger log = CLogger.getCLogger(BedrockEmbeddingModelWrapper.class);

    private final BedrockRuntimeClient client;
    private final String modelId;

    /**
     * Shared sync HTTP client - created once and reused.
     */
    private static volatile SdkHttpClient sharedHttpClient;
    private static final Object httpClientLock = new Object();

    private BedrockEmbeddingModelWrapper(Builder builder) {
        this.modelId = builder.modelId;

        // Determine credentials provider - use explicit if provided, else default
        AwsCredentialsProvider credentialsProvider;
        if (builder.accessKeyId != null && builder.secretAccessKey != null) {
            log.info("Using explicit AWS credentials for Bedrock embeddings");
            credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(builder.accessKeyId, builder.secretAccessKey));
        } else {
            log.info("Using default AWS credentials provider for Bedrock embeddings");
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        // Create client with explicit HTTP client (OSGi workaround)
        this.client = BedrockRuntimeClient.builder()
            .region(builder.region)
            .credentialsProvider(credentialsProvider)
            .httpClient(getSharedHttpClient())
            .build();

        log.info("Created BedrockEmbeddingModelWrapper for model: " + modelId);
    }

    /**
     * Get or create the shared sync HTTP client.
     *
     * <p>Sets the thread context classloader to ensure proper class resolution in OSGi.
     */
    private static SdkHttpClient getSharedHttpClient() {
        if (sharedHttpClient == null) {
            synchronized (httpClientLock) {
                if (sharedHttpClient == null) {
                    // Capture and set plugin classloader for OSGi compatibility
                    ClassLoader pluginClassLoader = BedrockEmbeddingModelWrapper.class.getClassLoader();
                    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

                    try {
                        Thread.currentThread().setContextClassLoader(pluginClassLoader);
                        log.info("Creating shared Apache sync HTTP client for Bedrock embeddings (with plugin classloader)");

                        sharedHttpClient = ApacheHttpClient.builder()
                            .connectionTimeout(Duration.ofSeconds(30))
                            .socketTimeout(Duration.ofSeconds(120))
                            .build();
                    } finally {
                        Thread.currentThread().setContextClassLoader(originalClassLoader);
                    }
                }
            }
        }
        return sharedHttpClient;
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<Embedding> embeddings = new ArrayList<>();

        for (TextSegment segment : textSegments) {
            try {
                Embedding embedding = embedSingle(segment.text());
                embeddings.add(embedding);
            } catch (Exception e) {
                log.severe("Failed to embed text segment: " + e.getMessage());
                throw new RuntimeException("Failed to embed text: " + e.getMessage(), e);
            }
        }

        return Response.from(embeddings);
    }

    /**
     * Embed a single text string.
     */
    private Embedding embedSingle(String text) {
        // Build Titan embedding request
        JSONObject requestBody = new JSONObject();
        requestBody.put("inputText", text);

        String requestJson = requestBody.toString();

        InvokeModelRequest request = InvokeModelRequest.builder()
            .modelId(modelId)
            .contentType("application/json")
            .accept("application/json")
            .body(SdkBytes.fromUtf8String(requestJson))
            .build();

        InvokeModelResponse response = client.invokeModel(request);
        String responseJson = response.body().asUtf8String();

        // Parse Titan response
        JSONObject responseObj = new JSONObject(responseJson);
        JSONArray embeddingArray = responseObj.getJSONArray("embedding");

        float[] vector = new float[embeddingArray.length()];
        for (int i = 0; i < embeddingArray.length(); i++) {
            vector[i] = embeddingArray.getFloat(i);
        }

        return Embedding.from(vector);
    }

    /**
     * Close the client when done.
     */
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    /**
     * Builder for BedrockEmbeddingModelWrapper.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Region region = Region.US_EAST_1;
        private String modelId = "amazon.titan-embed-text-v2:0";
        private String accessKeyId;
        private String secretAccessKey;

        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        public Builder model(String modelId) {
            this.modelId = modelId;
            return this;
        }

        /**
         * Set explicit AWS credentials.
         * If not set, DefaultCredentialsProvider will be used.
         *
         * @param accessKeyId AWS Access Key ID
         * @param secretAccessKey AWS Secret Access Key
         * @return this builder
         */
        public Builder credentials(String accessKeyId, String secretAccessKey) {
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
            return this;
        }

        public BedrockEmbeddingModelWrapper build() {
            return new BedrockEmbeddingModelWrapper(this);
        }
    }
}
