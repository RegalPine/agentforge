package io.github.regalpine.agentforge.runtime;

/**
 * Result type for skipped nodes.
 * AF-05 §23: SKIPPED state.
 *
 * <p>Produced when a node is bypassed due to
 * routing, conditional branching, or loop logic.
 */
public record NodeSkipped(
    String reason
) implements NodeExecutionResult {
}
