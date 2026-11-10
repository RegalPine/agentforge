package io.github.regalpine.agentforge.evaluation.platform;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Gate result.
 * AF-ENHANCE-12 §61: GateResult.
 */
public record GateResult(
    GateStatus status,
    String message,
    List<String> failures,
    Map<String, Object> metadata
) {

    public GateResult {
        Objects.requireNonNull(
            status, "status"
        );

        message = message == null
            ? "" : message;
        failures = failures == null
            ? List.of() : List.copyOf(failures);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a passing result.
     */
    public static GateResult pass() {
        return new GateResult(
            GateStatus.PASS, "",
            List.of(), Map.of()
        );
    }

    /**
     * Create a failing result.
     */
    public static GateResult fail(
        String message
    ) {
        return new GateResult(
            GateStatus.FAIL, message,
            List.of(), Map.of()
        );
    }

    /**
     * Check if the gate passed.
     */
    public boolean isPassed() {
        return status == GateStatus.PASS;
    }
}
