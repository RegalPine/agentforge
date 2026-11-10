package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.capability.CapabilityProvider;
import io.github.regalpine.agentforge.kernel.Reference;

public interface CapabilityProviderResolver {

    CapabilityProvider resolve(
        Reference capability
    );
}
