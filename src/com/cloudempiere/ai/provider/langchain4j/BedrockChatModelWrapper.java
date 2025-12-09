package com.cloudempiere.ai.provider.langchain4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.compiere.util.CLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

/**
 * Custom Bedrock chat model wrapper that explicitly creates the HTTP client.
 *
 * <p>This wrapper solves the OSGi ServiceLoader issue where the AWS SDK cannot find
 * the Apache HTTP client implementation through standard ServiceLoader discovery.
 *
 * @author Cloudempiere
 * @version 0.20.0
 */
public class BedrockChatModelWrapper implements ChatLanguageModel {

    private static final CLogger log = CLogger.getCLogger(BedrockChatModelWrapper.class);

    private final BedrockRuntimeClient client;
    private final String modelId;
    private final int maxTokens;
    private final float temperature;

    /**
     * Shared sync HTTP client - created once and reused.
     */
    private static volatile SdkHttpClient sharedHttpClient;
    private static final Object httpClientLock = new Object();

    private BedrockChatModelWrapper(Builder builder) {
        this.modelId = builder.modelId;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;

        // Create client with explicit HTTP client (OSGi workaround)
        this.client = BedrockRuntimeClient.builder()
            .region(builder.region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClient(getSharedHttpClient())
            .build();

        log.info("Created BedrockChatModelWrapper for model: " + modelId);
    }

    /**
     * Get or create the shared sync HTTP client.
     * Explicitly creates Apache client to avoid ServiceLoader issues in OSGi.
     */
    private static SdkHttpClient getSharedHttpClient() {
        if (sharedHttpClient == null) {
            synchronized (httpClientLock) {
                if (sharedHttpClient == null) {
                    log.info("Creating shared Apache sync HTTP client for Bedrock");
                    sharedHttpClient = ApacheHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(30))
                        .socketTimeout(Duration.ofSeconds(300))
                        .build();
                }
            }
        }
        return sharedHttpClient;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        try {
            // Build Anthropic Messages API request body
            JSONObject requestBody = buildAnthropicRequestBody(messages);
            String requestJson = requestBody.toString();

            log.fine("Bedrock request: " + requestJson);

            InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(requestJson))
                .build();

            InvokeModelResponse response = client.invokeModel(request);
            String responseJson = response.body().asUtf8String();

            log.fine("Bedrock response: " + responseJson);

            // Parse Anthropic response
            JSONObject responseObj = new JSONObject(responseJson);
            String responseText = extractResponseText(responseObj);

            AiMessage aiMessage = AiMessage.from(responseText);
            return Response.from(aiMessage);

        } catch (Exception e) {
            log.severe("Bedrock invocation failed: " + e.getMessage());
            throw new RuntimeException("Bedrock invocation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build the Anthropic Messages API request body.
     */
    private JSONObject buildAnthropicRequestBody(List<ChatMessage> messages) {
        JSONObject body = new JSONObject();
        body.put("anthropic_version", "bedrock-2023-05-31");
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);

        // Extract system message if present
        String systemPrompt = null;
        List<ChatMessage> conversationMessages = new ArrayList<>();

        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                systemPrompt = ((SystemMessage) message).text();
            } else {
                conversationMessages.add(message);
            }
        }

        if (systemPrompt != null) {
            body.put("system", systemPrompt);
        }

        // Build messages array
        JSONArray messagesArray = new JSONArray();
        for (ChatMessage message : conversationMessages) {
            JSONObject msgObj = new JSONObject();

            if (message instanceof UserMessage) {
                msgObj.put("role", "user");
                msgObj.put("content", ((UserMessage) message).singleText());
            } else if (message instanceof AiMessage) {
                msgObj.put("role", "assistant");
                msgObj.put("content", ((AiMessage) message).text());
            }

            if (msgObj.has("role")) {
                messagesArray.put(msgObj);
            }
        }

        body.put("messages", messagesArray);

        return body;
    }

    /**
     * Extract response text from Anthropic response.
     */
    private String extractResponseText(JSONObject response) {
        JSONArray content = response.optJSONArray("content");
        if (content != null && content.length() > 0) {
            JSONObject firstContent = content.getJSONObject(0);
            if ("text".equals(firstContent.optString("type"))) {
                return firstContent.getString("text");
            }
        }
        return "";
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
     * Builder for BedrockChatModelWrapper.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Region region = Region.US_EAST_1;
        private String modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0";
        private int maxTokens = 4096;
        private float temperature = 0.7f;

        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        public Builder model(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public BedrockChatModelWrapper build() {
            return new BedrockChatModelWrapper(this);
        }
    }
}
