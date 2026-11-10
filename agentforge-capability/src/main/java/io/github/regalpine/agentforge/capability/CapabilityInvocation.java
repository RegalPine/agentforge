package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Map;
import java.util.Objects;

public record CapabilityInvocation(
    String invocationId,
    EntityId capabilityId,
    Version capabilityVersion,
    Map<String, Object> arguments,
    InvocationContext context
) {

    public CapabilityInvocation {
        Objects.requireNonNull(
            invocationId,
            "invocationId"
        );

        Objects.requireNonNull(
            capabilityId,
            "capabilityId"
        );

        Objects.requireNonNull(
            capabilityVersion,
            "capabilityVersion"
        );

        arguments =
            arguments == null
                ? Map.of()
                : Map.copyOf(arguments);
    }
}
