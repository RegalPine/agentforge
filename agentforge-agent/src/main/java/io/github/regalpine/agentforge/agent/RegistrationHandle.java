package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.Reference;

import java.util.Objects;

/**
 * Handle returned by
 * {@link AgentRegistrar#registerAndStart}
 * that allows subsequent lifecycle
 * operations on the registered Agent.
 *
 * <p>Immutable record.
 */
public record RegistrationHandle(
    Reference reference
) {

    public RegistrationHandle {
        Objects.requireNonNull(
            reference, "reference"
        );
    }
}
