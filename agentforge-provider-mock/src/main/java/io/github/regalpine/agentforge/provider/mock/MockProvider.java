package io.github.regalpine.agentforge.provider.mock;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Map;
import java.util.Set;

public final class MockProvider
    implements CapabilityProvider {

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(
            "mock",
            new Version("1.0.0"),
            Set.of(
                new Reference(
                    new EntityId("mock.echo"),
                    new Version("1.0.0")
                )
            ),
            Map.of("type", "reference")
        );
    }

    @Override
    public CapabilityResult invoke(
        CapabilityInvocation invocation
    ) {
        Object message =
            invocation.arguments().get("message");

        if (message == null) {
            return new CapabilitySuccess(
                Map.of()
            );
        }

        return new CapabilitySuccess(
            Map.of("message", message)
        );
    }
}
