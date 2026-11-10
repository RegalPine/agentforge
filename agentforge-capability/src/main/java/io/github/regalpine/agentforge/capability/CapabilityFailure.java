package io.github.regalpine.agentforge.capability;

public record CapabilityFailure(
    String code,
    String message,
    boolean retryable
) implements CapabilityResult {
}
