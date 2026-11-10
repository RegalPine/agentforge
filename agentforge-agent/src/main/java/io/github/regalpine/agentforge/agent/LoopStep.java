package io.github.regalpine.agentforge.agent;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A single step in the Agent Loop execution.
 * <p>
 * Each step represents one iteration of the Observe → Decision → Action → Observation
 * cycle. Steps are recorded for observability and debugging.
 *
 * @param iteration  the iteration number (1-based)
 * @param phase      the phase name (OBSERVE, DECISION, ACTION, OBSERVATION, FINISH, WAIT, FAIL, etc.)
 * @param decision   the decision type name
 * @param startTime  when the step started
 * @param endTime    when the step ended
 * @param metadata   additional step metadata
 */
public record LoopStep(
    long iteration,
    String phase,
    String decision,
    Instant startTime,
    Instant endTime,
    Map<String, Object> metadata
) {

    public LoopStep {
        Objects.requireNonNull(phase, "phase cannot be null");
        Objects.requireNonNull(startTime, "startTime cannot be null");
        Objects.requireNonNull(endTime, "endTime cannot be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Get the duration of this step in milliseconds.
     *
     * @return duration in millis
     */
    public long durationMillis() {
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }
}
