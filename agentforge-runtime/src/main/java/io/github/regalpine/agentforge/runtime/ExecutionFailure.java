package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
/**
 * Execution failure information.
 * AF-10 §27: ExecutionFailure.
 *
 * <p>Companion type for {@link ExecutionResult}
 * that captures failure details.
 */
public record ExecutionFailure(
    String code,
    String message,
    boolean retryable
) {
}
