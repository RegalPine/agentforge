package io.github.regalpine.agentforge.capability;

public record CapabilityCancelled(
    String reason
) implements CapabilityResult {
}
