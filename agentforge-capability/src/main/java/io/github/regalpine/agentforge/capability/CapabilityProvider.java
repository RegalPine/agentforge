package io.github.regalpine.agentforge.capability;

public interface CapabilityProvider {

    ProviderDescriptor descriptor();

    CapabilityResult invoke(
        CapabilityInvocation invocation
    );
}
