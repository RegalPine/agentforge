package io.github.regalpine.agentforge.capability;

public record CapabilityWaiting(
    String reason
) implements CapabilityResult {
}
