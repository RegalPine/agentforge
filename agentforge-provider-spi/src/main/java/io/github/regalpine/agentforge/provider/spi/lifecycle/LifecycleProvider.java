package io.github.regalpine.agentforge.provider.spi.lifecycle;

import io.github.regalpine.agentforge.capability.CapabilityProvider;

public interface LifecycleProvider
    extends CapabilityProvider {

    ProviderStatus status();

    void initialize();

    void start();

    void stop();
}
