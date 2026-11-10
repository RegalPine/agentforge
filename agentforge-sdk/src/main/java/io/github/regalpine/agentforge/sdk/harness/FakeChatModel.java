package io.github.regalpine.agentforge.sdk.harness;

import io.github.regalpine.agentforge.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fake ChatModel for testing purposes.
 * <p>
 * Returns scripted responses in order. When all scripted responses are exhausted,
 * returns the last response repeatedly.
 * <p>
 * Thread-safe.
 */
public final class FakeChatModel implements ChatModel {

    private final String modelId;
    private final List<ChatResponse> scriptedResponses;
    private final AtomicInteger callIndex = new AtomicInteger(0);
    private final List<ChatRequest> receivedRequests = Collections.synchronizedList(new ArrayList<>());

    private FakeChatModel(String modelId, List<ChatResponse> scriptedResponses) {
        this.modelId = Objects.requireNonNull(modelId);
        this.scriptedResponses = List.copyOf(scriptedResponses);
        if (scriptedResponses.isEmpty()) {
            throw new IllegalArgumentException("At least one scripted response is required");
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        receivedRequests.add(request);
        int index = callIndex.getAndIncrement();
        if (index < scriptedResponses.size()) {
            return scriptedResponses.get(index);
        }
        return scriptedResponses.get(scriptedResponses.size() - 1);
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public String providerId() {
        return "fake";
    }

    /**
     * Get all received requests (for assertions).
     */
    public List<ChatRequest> receivedRequests() {
        return List.copyOf(receivedRequests);
    }

    /**
     * Get the number of times chat() was called.
     */
    public int callCount() {
        return callIndex.get();
    }

    /**
     * Reset the call counter.
     */
    public void reset() {
        callIndex.set(0);
        receivedRequests.clear();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FakeChatModel withResponse(String content) {
        return builder().response(ChatResponse.of(content)).build();
    }

    public static final class Builder {
        private String modelId = "fake-model";
        private final List<ChatResponse> responses = new ArrayList<>();

        private Builder() {}

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder response(ChatResponse response) {
            this.responses.add(response);
            return this;
        }

        public Builder response(String content) {
            this.responses.add(ChatResponse.of(content));
            return this;
        }

        public Builder toolCallResponse(List<ToolCall> toolCalls) {
            this.responses.add(ChatResponse.withToolCalls(toolCalls));
            return this;
        }

        public FakeChatModel build() {
            if (responses.isEmpty()) {
                responses.add(ChatResponse.of(""));
            }
            return new FakeChatModel(modelId, responses);
        }
    }
}
