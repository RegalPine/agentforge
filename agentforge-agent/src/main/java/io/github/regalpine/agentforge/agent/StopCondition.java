package io.github.regalpine.agentforge.agent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Describes why the Agent Loop stopped.
 * <p>
 * Every Agent Loop execution ends with a StopCondition that explains
 * the termination reason. This is critical for observability and
 * debugging.
 */
public record StopCondition(
    StopReason reason,
    String detail,
    Instant timestamp,
    Map<String, Object> metadata
) {

    public StopCondition {
        Objects.requireNonNull(reason, "reason cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a stop condition for a specific reason.
     *
     * @param reason the stop reason
     * @return a new StopCondition
     */
    public static StopCondition of(StopReason reason) {
        return new StopCondition(reason, null, Instant.now(), Map.of());
    }

    /**
     * Create a stop condition with detail.
     *
     * @param reason the stop reason
     * @param detail additional detail
     * @return a new StopCondition
     */
    public static StopCondition of(StopReason reason, String detail) {
        return new StopCondition(reason, detail, Instant.now(), Map.of());
    }

    /**
     * Stop reasons for the Agent Loop.
     */
    public enum StopReason {
        /** Agent completed its goal successfully. */
        FINISHED,
        /** Agent decided to fail. */
        FAILED,
        /** Agent is waiting for external input. */
        WAITING,
        /** Iteration limit reached. */
        MAX_ITERATIONS,
        /** Duration limit reached. */
        TIMEOUT,
        /** Token budget exhausted. */
        TOKEN_BUDGET_EXHAUSTED,
        /** Tool call budget exhausted. */
        TOOL_BUDGET_EXHAUSTED,
        /** Model call budget exhausted. */
        MODEL_BUDGET_EXHAUSTED,
        /** Cost budget exhausted. */
        COST_BUDGET_EXHAUSTED,
        /** No progress detected (stagnation). */
        STAGNATION,
        /** Policy gate rejected action. */
        POLICY_BLOCKED,
        /** External cancellation. */
        CANCELLED
    }
}
