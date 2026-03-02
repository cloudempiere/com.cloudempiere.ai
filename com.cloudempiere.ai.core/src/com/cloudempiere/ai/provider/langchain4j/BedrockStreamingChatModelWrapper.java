package com.cloudempiere.ai.provider.langchain4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;

import org.compiere.util.CLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamResponseHandler;

/**
 * Custom Bedrock streaming chat model wrapper that explicitly creates the HTTP client.
 *
 * <p>This wrapper solves the OSGi ServiceLoader issue where the AWS SDK cannot find
 * the Netty HTTP client implementation through standard ServiceLoader discovery.
 *
 * <p>Instead of relying on LangChain4j's internal client creation, we explicitly
 * create the BedrockRuntimeAsyncClient with a pre-configured NettyNioAsyncHttpClient.
 *
 * @author Cloudempiere
 * @version 0.20.0
 */
public class BedrockStreamingChatModelWrapper implements StreamingChatLanguageModel {

    private static final CLogger log = CLogger.getCLogger(BedrockStreamingChatModelWrapper.class);

    private final BedrockRuntimeAsyncClient asyncClient;
    private final String modelId;
    private final int maxTokens;
    private final float temperature;

    /**
     * Shared async HTTP client - created once and reused.
     */
    private static volatile SdkAsyncHttpClient sharedHttpClient;
    private static final Object httpClientLock = new Object();

    private BedrockStreamingChatModelWrapper(Builder builder) {
        this.modelId = builder.modelId;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;

        // Determine credentials provider - use explicit if provided, else default
        AwsCredentialsProvider credentialsProvider;
        if (builder.accessKeyId != null && builder.secretAccessKey != null) {
            log.info("Using explicit AWS credentials for Bedrock streaming");
            credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(builder.accessKeyId, builder.secretAccessKey));
        } else {
            log.info("Using default AWS credentials provider for Bedrock streaming");
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        // Create async client with explicit HTTP client (OSGi workaround)
        this.asyncClient = BedrockRuntimeAsyncClient.builder()
            .region(builder.region)
            .credentialsProvider(credentialsProvider)
            .httpClient(getSharedHttpClient())
            .build();

        log.info("Created BedrockStreamingChatModelWrapper for model: " + modelId);
    }

    /**
     * Custom SdkEventLoopGroup that uses the plugin's classloader.
     */
    private static volatile SdkEventLoopGroup sharedEventLoopGroup;

    /**
     * Get or create the shared async HTTP client.
     * Explicitly creates Netty client to avoid ServiceLoader issues in OSGi.
     *
     * <p>IMPORTANT: Uses a custom SdkEventLoopGroup with a ThreadFactory that sets
     * the plugin's classloader as the context classloader for all Netty threads.
     * This ensures Netty's EventLoop threads can access classes from the plugin's
     * embedded JARs (like eventstream.jar).
     */
    private static SdkAsyncHttpClient getSharedHttpClient() {
        if (sharedHttpClient == null) {
            synchronized (httpClientLock) {
                if (sharedHttpClient == null) {
                    // Capture plugin classloader for Netty EventLoop threads
                    final ClassLoader pluginClassLoader = BedrockStreamingChatModelWrapper.class.getClassLoader();

                    log.info("Creating shared Netty async HTTP client for Bedrock with OSGi-aware classloader");

                    // Create custom ThreadFactory that sets the plugin classloader
                    ThreadFactory threadFactory = new ThreadFactory() {
                        private final AtomicInteger threadNumber = new AtomicInteger(1);

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread thread = new Thread(() -> {
                                // Set plugin classloader as context classloader for this thread
                                Thread.currentThread().setContextClassLoader(pluginClassLoader);
                                if (log.isLoggable(java.util.logging.Level.FINE)) {
                                    log.fine("Netty thread started with classloader: " +
                                        Thread.currentThread().getContextClassLoader());
                                }
                                r.run();
                            }, "bedrock-netty-" + threadNumber.getAndIncrement());
                            thread.setContextClassLoader(pluginClassLoader);
                            thread.setDaemon(true);
                            return thread;
                        }
                    };

                    // Create custom SdkEventLoopGroup with our ThreadFactory
                    sharedEventLoopGroup = SdkEventLoopGroup.builder()
                        .threadFactory(threadFactory)
                        .numberOfThreads(4)
                        .build();

                    sharedHttpClient = NettyNioAsyncHttpClient.builder()
                        .eventLoopGroup(sharedEventLoopGroup)
                        .connectionTimeout(Duration.ofSeconds(30))
                        .readTimeout(Duration.ofSeconds(300))
                        .writeTimeout(Duration.ofSeconds(30))
                        .maxConcurrency(50)
                        .build();
                }
            }
        }
        return sharedHttpClient;
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        // Delegate to the tool-enabled method with no tools
        generate(messages, (List<ToolSpecification>) null, handler);
    }

    @Override
    public void generate(List<ChatMessage> messages, ToolSpecification toolSpecification,
                         StreamingResponseHandler<AiMessage> handler) {
        // Single tool - wrap in list and delegate
        List<ToolSpecification> tools = toolSpecification != null ?
            java.util.Collections.singletonList(toolSpecification) : null;
        generate(messages, tools, handler);
    }

    @Override
    public void generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications,
                         StreamingResponseHandler<AiMessage> handler) {
        // Capture plugin classloader for OSGi compatibility
        // AWS SDK async threads need access to eventstream classes
        final ClassLoader pluginClassLoader = this.getClass().getClassLoader();
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            // Set plugin classloader as context classloader for AWS SDK
            Thread.currentThread().setContextClassLoader(pluginClassLoader);

            // Build Anthropic Messages API request body (with optional tools)
            JSONObject requestBody = buildAnthropicRequestBody(messages, toolSpecifications);
            String requestJson = requestBody.toString();

            log.fine("Bedrock streaming request: " + requestJson);

            InvokeModelWithResponseStreamRequest request = InvokeModelWithResponseStreamRequest.builder()
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(requestJson))
                .build();

            StringBuilder fullResponse = new StringBuilder();
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            AtomicInteger inputTokenCount = new AtomicInteger(0);
            AtomicInteger outputTokenCount = new AtomicInteger(0);
            // Track tool use blocks
            AtomicReference<JSONArray> toolUseBlocks = new AtomicReference<>(new JSONArray());
            AtomicReference<StringBuilder> currentToolInput = new AtomicReference<>();
            AtomicReference<String> currentToolId = new AtomicReference<>();
            AtomicReference<String> currentToolName = new AtomicReference<>();

            InvokeModelWithResponseStreamResponseHandler responseHandler =
                InvokeModelWithResponseStreamResponseHandler.builder()
                    .onEventStream(stream -> {
                        stream.subscribe(event -> {
                            event.accept(InvokeModelWithResponseStreamResponseHandler.Visitor.builder()
                                .onChunk(chunk -> {
                                    try {
                                        String chunkJson = chunk.bytes().asUtf8String();
                                        JSONObject chunkObj = new JSONObject(chunkJson);

                                        // Handle Anthropic streaming response format
                                        if (chunkObj.has("type")) {
                                            String type = chunkObj.getString("type");

                                            if ("message_start".equals(type)) {
                                                JSONObject msg = chunkObj.optJSONObject("message");
                                                if (msg != null) {
                                                    JSONObject u = msg.optJSONObject("usage");
                                                    if (u != null)
                                                        inputTokenCount.set(u.optInt("input_tokens", 0));
                                                }
                                            } else if ("message_delta".equals(type)) {
                                                JSONObject u = chunkObj.optJSONObject("usage");
                                                if (u != null)
                                                    outputTokenCount.set(u.optInt("output_tokens", 0));
                                            } else if ("content_block_start".equals(type)) {
                                                JSONObject contentBlock = chunkObj.optJSONObject("content_block");
                                                if (contentBlock != null && "tool_use".equals(contentBlock.optString("type"))) {
                                                    currentToolId.set(contentBlock.getString("id"));
                                                    currentToolName.set(contentBlock.getString("name"));
                                                    currentToolInput.set(new StringBuilder());
                                                }
                                            } else if ("content_block_delta".equals(type)) {
                                                JSONObject delta = chunkObj.optJSONObject("delta");
                                                if (delta != null) {
                                                    if (delta.has("text")) {
                                                        String text = delta.getString("text");
                                                        fullResponse.append(text);
                                                        handler.onNext(text);
                                                    } else if (delta.has("partial_json") && currentToolInput.get() != null) {
                                                        currentToolInput.get().append(delta.getString("partial_json"));
                                                    }
                                                }
                                            } else if ("content_block_stop".equals(type)) {
                                                // If we were building a tool use, finalize it
                                                if (currentToolId.get() != null && currentToolName.get() != null) {
                                                    JSONObject toolUse = new JSONObject();
                                                    toolUse.put("id", currentToolId.get());
                                                    toolUse.put("name", currentToolName.get());
                                                    String inputStr = currentToolInput.get() != null ? currentToolInput.get().toString() : "{}";
                                                    try {
                                                        toolUse.put("input", new JSONObject(inputStr));
                                                    } catch (Exception e) {
                                                        toolUse.put("input", new JSONObject());
                                                    }
                                                    toolUseBlocks.get().put(toolUse);
                                                    // Reset
                                                    currentToolId.set(null);
                                                    currentToolName.set(null);
                                                    currentToolInput.set(null);
                                                }
                                            } else if ("message_stop".equals(type)) {
                                                // Message complete
                                            } else if ("error".equals(type)) {
                                                JSONObject error = chunkObj.optJSONObject("error");
                                                String errorMsg = error != null ? error.optString("message", "Unknown error") : "Unknown error";
                                                errorRef.set(new RuntimeException("Bedrock error: " + errorMsg));
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warning("Error parsing Bedrock chunk: " + e.getMessage());
                                    }
                                })
                                .build());
                        });
                    })
                    .onComplete(() -> {
                        Throwable error = errorRef.get();
                        if (error != null) {
                            handler.onError(error);
                        } else {
                            // Build AiMessage with potential tool execution requests
                            AiMessage aiMessage;
                            String responseText = fullResponse.toString();
                            boolean hasText = responseText != null && !responseText.trim().isEmpty();

                            if (toolUseBlocks.get().length() > 0) {
                                // Convert tool use blocks to LangChain4j format
                                List<dev.langchain4j.agent.tool.ToolExecutionRequest> toolRequests = new ArrayList<>();
                                for (int i = 0; i < toolUseBlocks.get().length(); i++) {
                                    JSONObject tu = toolUseBlocks.get().getJSONObject(i);
                                    toolRequests.add(dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                                        .id(tu.getString("id"))
                                        .name(tu.getString("name"))
                                        .arguments(tu.getJSONObject("input").toString())
                                        .build());
                                }
                                // Use appropriate factory: with text if present, tools-only otherwise
                                if (hasText) {
                                    aiMessage = AiMessage.from(responseText, toolRequests);
                                } else {
                                    // Tool-only response (no text) - use tool requests constructor
                                    aiMessage = AiMessage.from(toolRequests);
                                }
                            } else if (hasText) {
                                aiMessage = AiMessage.from(responseText);
                            } else {
                                // Edge case: no text and no tools - send empty acknowledgment
                                log.warning("Bedrock streaming completed with no text and no tools");
                                aiMessage = AiMessage.from("(No response generated)");
                            }
                            TokenUsage tokenUsage = new TokenUsage(inputTokenCount.get(), outputTokenCount.get());
                            handler.onComplete(Response.from(aiMessage, tokenUsage, null));
                        }
                    })
                    .onError(error -> {
                        log.severe("Bedrock streaming error: " + error.getMessage());
                        handler.onError(error);
                    })
                    .build();

            asyncClient.invokeModelWithResponseStream(request, responseHandler);

        } catch (Exception e) {
            log.severe("Failed to start Bedrock streaming: " + e.getMessage());
            handler.onError(e);
        } finally {
            // Restore original classloader
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    /**
     * Build the Anthropic Messages API request body with optional tools.
     */
    private JSONObject buildAnthropicRequestBody(List<ChatMessage> messages, List<ToolSpecification> tools) {
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
                AiMessage aiMsg = (AiMessage) message;
                msgObj.put("role", "assistant");
                // Handle tool use in assistant messages
                if (aiMsg.hasToolExecutionRequests()) {
                    JSONArray contentArray = new JSONArray();
                    if (aiMsg.text() != null && !aiMsg.text().isEmpty()) {
                        JSONObject textBlock = new JSONObject();
                        textBlock.put("type", "text");
                        textBlock.put("text", aiMsg.text());
                        contentArray.put(textBlock);
                    }
                    for (var toolReq : aiMsg.toolExecutionRequests()) {
                        JSONObject toolUseBlock = new JSONObject();
                        toolUseBlock.put("type", "tool_use");
                        toolUseBlock.put("id", toolReq.id());
                        toolUseBlock.put("name", toolReq.name());
                        try {
                            toolUseBlock.put("input", new JSONObject(toolReq.arguments()));
                        } catch (Exception e) {
                            toolUseBlock.put("input", new JSONObject());
                        }
                        contentArray.put(toolUseBlock);
                    }
                    msgObj.put("content", contentArray);
                } else {
                    msgObj.put("content", aiMsg.text());
                }
            } else if (message instanceof ToolExecutionResultMessage) {
                ToolExecutionResultMessage toolResult = (ToolExecutionResultMessage) message;
                msgObj.put("role", "user");
                JSONArray contentArray = new JSONArray();
                JSONObject toolResultBlock = new JSONObject();
                toolResultBlock.put("type", "tool_result");
                toolResultBlock.put("tool_use_id", toolResult.id());
                toolResultBlock.put("content", toolResult.text());
                contentArray.put(toolResultBlock);
                msgObj.put("content", contentArray);
            }

            if (msgObj.has("role")) {
                messagesArray.put(msgObj);
            }
        }

        body.put("messages", messagesArray);

        // Add tools if provided
        if (tools != null && !tools.isEmpty()) {
            JSONArray toolsArray = new JSONArray();
            for (ToolSpecification tool : tools) {
                JSONObject toolObj = new JSONObject();
                toolObj.put("name", tool.name());
                toolObj.put("description", tool.description() != null ? tool.description() : "");

                // Build input schema
                JSONObject inputSchema = new JSONObject();
                inputSchema.put("type", "object");

                if (tool.parameters() != null) {
                    JSONObject properties = new JSONObject();
                    JSONArray required = new JSONArray();

                    // In LangChain4j 0.35.0, parameters().properties() returns Map<String, Map<String, Object>>
                    Map<String, Map<String, Object>> params = tool.parameters().properties();
                    if (params != null) {
                        for (var param : params.entrySet()) {
                            JSONObject paramObj = new JSONObject();
                            Map<String, Object> paramSpec = param.getValue();
                            Object typeVal = paramSpec.get("type");
                            paramObj.put("type", typeVal != null ? typeVal.toString() : "string");
                            Object descVal = paramSpec.get("description");
                            if (descVal != null) {
                                paramObj.put("description", descVal.toString());
                            }
                            properties.put(param.getKey(), paramObj);
                        }
                    }

                    inputSchema.put("properties", properties);
                    if (tool.parameters().required() != null) {
                        for (String req : tool.parameters().required()) {
                            required.put(req);
                        }
                        inputSchema.put("required", required);
                    }
                } else {
                    inputSchema.put("properties", new JSONObject());
                }

                toolObj.put("input_schema", inputSchema);
                toolsArray.put(toolObj);
            }
            body.put("tools", toolsArray);
        }

        return body;
    }

    /**
     * Close the async client when done.
     */
    public void close() {
        if (asyncClient != null) {
            asyncClient.close();
        }
    }

    /**
     * Reset the shared HTTP client and EventLoopGroup.
     * Call this if the client needs to be recreated (e.g., after classloader issues).
     */
    public static void resetSharedHttpClient() {
        synchronized (httpClientLock) {
            if (sharedHttpClient != null) {
                sharedHttpClient.close();
                sharedHttpClient = null;
            }
            if (sharedEventLoopGroup != null) {
                sharedEventLoopGroup.eventLoopGroup().shutdownGracefully();
                sharedEventLoopGroup = null;
            }
            log.info("Shared Netty HTTP client and EventLoopGroup reset");
        }
    }


    /**
     * Builder for BedrockStreamingChatModelWrapper.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Region region = Region.US_EAST_1;
        private String modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0";
        private int maxTokens = 4096;
        private float temperature = 0.7f;
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

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
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

        public BedrockStreamingChatModelWrapper build() {
            return new BedrockStreamingChatModelWrapper(this);
        }
    }
}
