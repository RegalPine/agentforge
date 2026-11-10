package io.github.regalpine.agentforge.capability;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Result of an invocation pipeline execution.
 * AE-02 §24: carries stage information,
 * timing, and the underlying CapabilityResult.
 */
public record PipelineResult(
    String invocationId,
    CapabilityResult result,
    InvocationStage completedStage,
    Duration duration,
    int attempts,
    Map<String, Object> metadata
) {

    public PipelineResult {
        Objects.requireNonNull(invocationId, "invocationId");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(completedStage, "completedStage");

        duration =
            duration == null
                ? Duration.ZERO
                : duration;

        if (attempts < 1) {
            attempts = 1;
        }

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }

    public boolean isSuccess() {
        return result instanceof CapabilitySuccess;
    }
}
