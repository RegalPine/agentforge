package io.github.regalpine.agentforge.model;

import java.util.concurrent.CompletionStage;

/**
 * Interface for chat models that can process conversation messages.
 * <p>
 * A ChatModel takes a {@link ChatRequest} and returns a {@link ChatResponse}.
 * This is the primary interface for LLM interactions in AgentForge.
 * <p>
 * Supports synchronous, asynchronous, and streaming invocations.
 * <p>
 * Example usage:
 * <pre>
 * ChatModel model = ...;
 * ChatRequest request = ChatRequest.of(List.of(UserMessage.of("Hello")));
 * ChatResponse response = model.chat(request);
 * </pre>
 */
public interface ChatModel {

    /**
     * Process a chat request and return a response.
     *
     * @param request the chat request
     * @return the chat response
     * @throws ModelException if the model invocation fails
     */
    ChatResponse chat(ChatRequest request);

    /**
     * Process a chat request asynchronously.
     *
     * @param request the chat request
     * @return a CompletionStage that completes with the chat response
     * @throws ModelException if the model invocation fails
     */
    default CompletionStage<ChatResponse> chatAsync(ChatRequest request) {
        java.util.concurrent.CompletableFuture<ChatResponse> failed = new java.util.concurrent.CompletableFuture<>();
        failed.completeExceptionally(new UnsupportedOperationException("Async not supported"));
        return failed;
    }

    /**
     * Process a chat request as a stream of events.
     * <p>
     * The returned publisher emits {@link ChatEvent} instances following
     * the legal streaming sequence:
     * <pre>
     * ChatStreamStarted → TextDelta* → [ToolCall → ToolCallDelta* → ToolCallCompleted]* → UsageReceived → ChatStreamCompleted
     * </pre>
     *
     * @param request the chat request
     * @return a Flow.Publisher of streaming events
     * @throws ModelException if streaming is not supported or fails
     */
    default java.util.concurrent.Flow.Publisher<ChatEvent> stream(ChatRequest request) {
        throw new UnsupportedOperationException("Streaming not supported by this model");
    }

    /**
     * Get the model identifier.
     *
     * @return the model ID
     */
    String modelId();

    /**
     * Get the provider identifier.
     *
     * @return the provider ID
     */
    String providerId();
}
