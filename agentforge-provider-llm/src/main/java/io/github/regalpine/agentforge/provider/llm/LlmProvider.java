package io.github.regalpine.agentforge.provider.llm;

import io.github.regalpine.agentforge.capability.CapabilityProvider;

public interface LlmProvider
    extends CapabilityProvider {

    LlmResponse complete(
        LlmRequest request
    );
}
