package io.github.regalpine.agentforge.model;

import java.util.List;
import java.util.Objects;

/**
 * Sealed interface representing streaming events from a chat model.
 * <p>
 * All providers must map their streaming events to these standard types.
 * The event sequence must follow one of these legal patterns:
 * <pre>
 * ChatStreamStarted → TextDelta* → ToolCallStarted → ToolCallDelta* → ToolCallCompleted → UsageReceived → ChatStreamCompleted
 * ChatStreamStarted → TextDelta* → ChatStreamFailed
 * </pre>
 * Illegal sequences (e.g., Completed → Delta) are forbidden.
 */
public sealed interface ChatEvent {

    /**
     * Stream has started. First event in every stream.
     */
    record ChatStreamStarted(
        String model,
        String providerId
    ) implements ChatEvent {
        public ChatStreamStarted {
            Objects.requireNonNull(model, "model");
        }
    }

    /**
     * Incremental text content delta.
     */
    record TextDelta(
        String delta
    ) implements ChatEvent {
        public TextDelta {
            Objects.requireNonNull(delta, "delta");
        }
    }

    /**
     * A tool call has started being streamed.
     */
    record ToolCallStarted(
        String callId,
        String toolName
    ) implements ChatEvent {
        public ToolCallStarted {
            Objects.requireNonNull(callId, "callId");
            Objects.requireNonNull(toolName, "toolName");
        }
    }

    /**
     * Incremental tool call argument delta.
     */
    record ToolCallDelta(
        String callId,
        String argumentsDelta
    ) implements ChatEvent {
        public ToolCallDelta {
            Objects.requireNonNull(callId, "callId");
            Objects.requireNonNull(argumentsDelta, "argumentsDelta");
        }
    }

    /**
     * A tool call has completed streaming.
     */
    record ToolCallCompleted(
        String callId,
        String toolName,
        String arguments
    ) implements ChatEvent {
        public ToolCallCompleted {
            Objects.requireNonNull(callId, "callId");
            Objects.requireNonNull(toolName, "toolName");
            Objects.requireNonNull(arguments, "arguments");
        }
    }

    /**
     * Token usage information received (typically near end of stream).
     */
    record UsageReceived(
        TokenUsage usage
    ) implements ChatEvent {
        public UsageReceived {
            Objects.requireNonNull(usage, "usage");
        }
    }

    /**
     * Stream completed successfully. Terminal event for successful streams.
     */
    record ChatStreamCompleted(
        FinishReason finishReason
    ) implements ChatEvent {
        public ChatStreamCompleted {
            Objects.requireNonNull(finishReason, "finishReason");
        }
    }

    /**
     * Stream failed. Terminal event for failed streams.
     */
    record ChatStreamFailed(
        String error,
        Throwable cause
    ) implements ChatEvent {
        public ChatStreamFailed {
            Objects.requireNonNull(error, "error");
        }

        public ChatStreamFailed(String error) {
            this(error, null);
        }
    }
}
