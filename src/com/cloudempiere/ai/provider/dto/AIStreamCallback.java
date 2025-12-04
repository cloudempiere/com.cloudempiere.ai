package com.cloudempiere.ai.provider.dto;

/**
 * Enhanced callback interface for streaming responses with tool and thinking support.
 *
 * <p>This interface extends the basic streaming model to provide full visibility
 * into AI operations, including:
 * <ul>
 *   <li>Token streaming for real-time response display</li>
 *   <li>Tool/function execution events for transparency</li>
 *   <li>Extended thinking/reasoning events (for supported models)</li>
 *   <li>Progress stage indicators</li>
 * </ul>
 *
 * <p>Implements ADR-033: Streaming Responses and Thinking Timeline UX
 *
 * @author Cloudempiere
 * @version 2.0
 * @since ADR-033
 */
public interface AIStreamCallback {

    // ============= Core Streaming Events =============

    /**
     * Called when a new text chunk is received from the model.
     *
     * @param chunk text chunk (typically 1-5 tokens)
     */
    void onChunk(String chunk);

    /**
     * Called when streaming is complete.
     */
    void onComplete();

    /**
     * Called when an error occurs during streaming.
     *
     * @param error exception that occurred
     */
    void onError(Exception error);

    // ============= Tool/Function Events =============

    /**
     * Called when the AI starts executing a tool/function.
     *
     * <p>UI implementations should show a "running" indicator for this tool.
     *
     * @param toolName name of the tool being executed (e.g., "queryDatabase")
     * @param arguments JSON string of tool arguments
     */
    default void onToolStart(String toolName, String arguments) {}

    /**
     * Called when a tool/function execution completes successfully.
     *
     * <p>UI implementations should update the indicator to "complete".
     *
     * @param toolName name of the tool that completed
     * @param result result of the tool execution (may be truncated for display)
     */
    default void onToolComplete(String toolName, String result) {}

    /**
     * Called when a tool/function execution fails.
     *
     * <p>UI implementations should show an error indicator.
     *
     * @param toolName name of the tool that failed
     * @param error error message
     */
    default void onToolError(String toolName, String error) {}

    // ============= Thinking/Reasoning Events =============

    /**
     * Called when thinking/reasoning content is received.
     *
     * <p>Extended thinking is supported by models like Claude 3.5 Sonnet
     * with the extended thinking feature enabled.
     *
     * @param thinkingChunk chunk of thinking/reasoning text
     */
    default void onThinking(String thinkingChunk) {}

    /**
     * Called when the thinking phase completes and response generation begins.
     *
     * <p>UI implementations can use this to collapse the thinking section
     * and prepare for the main response.
     */
    default void onThinkingComplete() {}

    // ============= Progress Events =============

    /**
     * Called to indicate processing stage changes.
     *
     * <p>Useful for showing users what the AI is currently doing,
     * especially during tool execution or multi-step operations.
     *
     * @param stage current stage (e.g., "analyzing", "querying", "formatting")
     * @param detail optional detail message (can be null)
     */
    default void onProgress(String stage, String detail) {}

    // ============= Convenience Factory Methods =============

    /**
     * Create a simple callback that only handles text chunks.
     *
     * @param chunkHandler handler for text chunks
     * @param completeHandler handler for completion
     * @param errorHandler handler for errors
     * @return AIStreamCallback instance
     */
    static AIStreamCallback simple(
            java.util.function.Consumer<String> chunkHandler,
            Runnable completeHandler,
            java.util.function.Consumer<Exception> errorHandler) {

        return new AIStreamCallback() {
            @Override
            public void onChunk(String chunk) {
                if (chunkHandler != null) chunkHandler.accept(chunk);
            }

            @Override
            public void onComplete() {
                if (completeHandler != null) completeHandler.run();
            }

            @Override
            public void onError(Exception error) {
                if (errorHandler != null) errorHandler.accept(error);
            }
        };
    }

    /**
     * Create a full-featured callback with all event handlers.
     *
     * @return Builder for creating a full callback
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating AIStreamCallback with all handlers.
     */
    class Builder {
        private java.util.function.Consumer<String> chunkHandler;
        private Runnable completeHandler;
        private java.util.function.Consumer<Exception> errorHandler;
        private java.util.function.BiConsumer<String, String> toolStartHandler;
        private java.util.function.BiConsumer<String, String> toolCompleteHandler;
        private java.util.function.BiConsumer<String, String> toolErrorHandler;
        private java.util.function.Consumer<String> thinkingHandler;
        private Runnable thinkingCompleteHandler;
        private java.util.function.BiConsumer<String, String> progressHandler;

        public Builder onChunk(java.util.function.Consumer<String> handler) {
            this.chunkHandler = handler;
            return this;
        }

        public Builder onComplete(Runnable handler) {
            this.completeHandler = handler;
            return this;
        }

        public Builder onError(java.util.function.Consumer<Exception> handler) {
            this.errorHandler = handler;
            return this;
        }

        public Builder onToolStart(java.util.function.BiConsumer<String, String> handler) {
            this.toolStartHandler = handler;
            return this;
        }

        public Builder onToolComplete(java.util.function.BiConsumer<String, String> handler) {
            this.toolCompleteHandler = handler;
            return this;
        }

        public Builder onToolError(java.util.function.BiConsumer<String, String> handler) {
            this.toolErrorHandler = handler;
            return this;
        }

        public Builder onThinking(java.util.function.Consumer<String> handler) {
            this.thinkingHandler = handler;
            return this;
        }

        public Builder onThinkingComplete(Runnable handler) {
            this.thinkingCompleteHandler = handler;
            return this;
        }

        public Builder onProgress(java.util.function.BiConsumer<String, String> handler) {
            this.progressHandler = handler;
            return this;
        }

        public AIStreamCallback build() {
            return new AIStreamCallback() {
                @Override
                public void onChunk(String chunk) {
                    if (chunkHandler != null) chunkHandler.accept(chunk);
                }

                @Override
                public void onComplete() {
                    if (completeHandler != null) completeHandler.run();
                }

                @Override
                public void onError(Exception error) {
                    if (errorHandler != null) errorHandler.accept(error);
                }

                @Override
                public void onToolStart(String toolName, String arguments) {
                    if (toolStartHandler != null) toolStartHandler.accept(toolName, arguments);
                }

                @Override
                public void onToolComplete(String toolName, String result) {
                    if (toolCompleteHandler != null) toolCompleteHandler.accept(toolName, result);
                }

                @Override
                public void onToolError(String toolName, String error) {
                    if (toolErrorHandler != null) toolErrorHandler.accept(toolName, error);
                }

                @Override
                public void onThinking(String thinkingChunk) {
                    if (thinkingHandler != null) thinkingHandler.accept(thinkingChunk);
                }

                @Override
                public void onThinkingComplete() {
                    if (thinkingCompleteHandler != null) thinkingCompleteHandler.run();
                }

                @Override
                public void onProgress(String stage, String detail) {
                    if (progressHandler != null) progressHandler.accept(stage, detail);
                }
            };
        }
    }
}
