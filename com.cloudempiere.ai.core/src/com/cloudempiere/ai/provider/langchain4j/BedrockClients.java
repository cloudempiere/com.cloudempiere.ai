package com.cloudempiere.ai.provider.langchain4j;

import java.time.Duration;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.compiere.util.CLogger;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

/**
 * Builds Bedrock sync/async clients with an explicitly-constructed HTTP client,
 * bypassing OSGi's broken ServiceLoader-based discovery of the AWS SDK's
 * Apache/Netty HTTP implementations.
 *
 * <p>langchain4j-bedrock 1.20.0's {@code BedrockChatModel}/{@code BedrockStreamingChatModel}/
 * {@code BedrockTitanEmbeddingModel} builders accept a pre-built client via {@code .client(...)},
 * so this replaces the hand-rolled {@code BedrockChatModelWrapper}/{@code BedrockStreamingChatModelWrapper}/
 * {@code BedrockEmbeddingModelWrapper} classes from the 0.35.0 era - only the client construction
 * needed to survive, not a full reimplementation of the Bedrock request/response mapping.
 */
final class BedrockClients {

    private static final CLogger log = CLogger.getCLogger(BedrockClients.class);

    private static volatile SdkHttpClient sharedSyncHttpClient;
    private static final Object SYNC_LOCK = new Object();

    private static volatile SdkAsyncHttpClient sharedAsyncHttpClient;
    private static volatile SdkEventLoopGroup sharedEventLoopGroup;
    private static final Object ASYNC_LOCK = new Object();

    private BedrockClients() {
    }

    static BedrockRuntimeClient createSyncClient(Region region, AwsCredentialsProvider credentialsProvider) {
        return BedrockRuntimeClient.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .httpClient(getSharedSyncHttpClient())
            .build();
    }

    static BedrockRuntimeAsyncClient createAsyncClient(Region region, AwsCredentialsProvider credentialsProvider) {
        return BedrockRuntimeAsyncClient.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .httpClient(getSharedAsyncHttpClient())
            .build();
    }

    /**
     * Sets the plugin classloader as context classloader while building the Apache client
     * so the AWS SDK's ServiceLoader-based internals (and OSGi-embedded classes it needs,
     * e.g. eventstream) resolve against this bundle instead of the calling thread's classloader.
     */
    private static SdkHttpClient getSharedSyncHttpClient() {
        if (sharedSyncHttpClient == null) {
            synchronized (SYNC_LOCK) {
                if (sharedSyncHttpClient == null) {
                    ClassLoader pluginClassLoader = BedrockClients.class.getClassLoader();
                    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(pluginClassLoader);
                        log.info("Creating shared Apache sync HTTP client for Bedrock (OSGi-aware classloader)");
                        sharedSyncHttpClient = ApacheHttpClient.builder()
                            .connectionTimeout(Duration.ofSeconds(30))
                            .socketTimeout(Duration.ofSeconds(300))
                            .build();
                    } finally {
                        Thread.currentThread().setContextClassLoader(originalClassLoader);
                    }
                }
            }
        }
        return sharedSyncHttpClient;
    }

    /**
     * Netty's own EventLoop threads need the plugin classloader set explicitly (not just the
     * thread that builds the client), since they run independently afterward.
     */
    private static SdkAsyncHttpClient getSharedAsyncHttpClient() {
        if (sharedAsyncHttpClient == null) {
            synchronized (ASYNC_LOCK) {
                if (sharedAsyncHttpClient == null) {
                    final ClassLoader pluginClassLoader = BedrockClients.class.getClassLoader();
                    log.info("Creating shared Netty async HTTP client for Bedrock (OSGi-aware classloader)");

                    ThreadFactory threadFactory = new ThreadFactory() {
                        private final AtomicInteger threadNumber = new AtomicInteger(1);

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread thread = new Thread(() -> {
                                Thread.currentThread().setContextClassLoader(pluginClassLoader);
                                r.run();
                            }, "bedrock-netty-" + threadNumber.getAndIncrement());
                            thread.setContextClassLoader(pluginClassLoader);
                            thread.setDaemon(true);
                            return thread;
                        }
                    };

                    sharedEventLoopGroup = SdkEventLoopGroup.builder()
                        .threadFactory(threadFactory)
                        .numberOfThreads(4)
                        .build();

                    sharedAsyncHttpClient = NettyNioAsyncHttpClient.builder()
                        .eventLoopGroup(sharedEventLoopGroup)
                        .connectionTimeout(Duration.ofSeconds(30))
                        .readTimeout(Duration.ofSeconds(300))
                        .writeTimeout(Duration.ofSeconds(30))
                        .maxConcurrency(50)
                        .build();
                }
            }
        }
        return sharedAsyncHttpClient;
    }
}
