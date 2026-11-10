package io.github.regalpine.agentforge.sdk;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Input to an Agent execution.
 * AF-ENHANCE-01 §15: Agent API.
 *
 * <p>Wraps the user message and optional
 * contextual parameters for Agent execution.
 *
 * <p>Immutable. Thread-safe.
 */
public final class AgentInput {

    private final String message;
    private final Map<String, Object> parameters;
    private final String conversationId;
    private final String sessionId;

    private AgentInput(Builder builder) {
        this.message = builder.message;
        this.parameters = Map.copyOf(builder.parameters);
        this.conversationId = builder.conversationId;
        this.sessionId = builder.sessionId;
    }

    /**
     * Create a simple text input.
     */
    public static AgentInput of(String message) {
        return builder().message(message).build();
    }

    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The user message.
     */
    public String message() {
        return message;
    }

    /**
     * Additional parameters.
     */
    public Map<String, Object> parameters() {
        return parameters;
    }

    /**
     * Optional conversation ID for
     * multi-turn conversations.
     */
    public String conversationId() {
        return conversationId;
    }

    /**
     * Optional session ID.
     */
    public String sessionId() {
        return sessionId;
    }

    /**
     * Builder for AgentInput.
     */
    public static final class Builder {

        private String message;
        private final Map<String, Object> parameters =
            new LinkedHashMap<>();
        private String conversationId;
        private String sessionId;

        Builder() {}

        public Builder message(String message) {
            this.message = Objects.requireNonNull(
                message, "message"
            );
            return this;
        }

        public Builder parameter(
            String key, Object value
        ) {
            parameters.put(key, value);
            return this;
        }

        public Builder conversationId(String id) {
            this.conversationId = id;
            return this;
        }

        public Builder sessionId(String id) {
            this.sessionId = id;
            return this;
        }

        public AgentInput build() {
            return new AgentInput(this);
        }
    }
}
