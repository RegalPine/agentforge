package io.github.regalpine.agentforge.capability;

public sealed interface CapabilityResult
    permits CapabilitySuccess,
            CapabilityFailure,
            CapabilityWaiting,
            CapabilityCancelled {
}
