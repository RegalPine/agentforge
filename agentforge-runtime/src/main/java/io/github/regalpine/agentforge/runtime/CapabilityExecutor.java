package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Map;
import java.util.UUID;

public final class CapabilityExecutor {

    private final CapabilityProviderResolver resolver;

    public CapabilityExecutor(
        CapabilityProviderResolver resolver
    ) {
        this.resolver = resolver;
    }

    public NodeExecutionResult execute(
        ExecutionNode node,
        NodeExecutionContext context
    ) {

        Object referenceValue =
            node.resolvedReferences()
                .get("capability");

        Reference reference;

        if (referenceValue instanceof Reference ref) {
            reference = ref;
        } else if (referenceValue instanceof String str) {
            String[] parts = str.split("@", 2);
            if (parts.length != 2) {
                return new NodeFailed(
                    "AF-R001",
                    "Invalid capability reference format: " + str,
                    false
                );
            }
            reference = new Reference(
                new EntityId(parts[0]),
                new Version(parts[1])
            );
        } else {
            return new NodeFailed(
                "AF-R001",
                "Capability reference missing",
                false
            );
        }

        CapabilityProvider provider =
            resolver.resolve(reference);

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                UUID.randomUUID().toString(),
                reference.id(),
                reference.version(),
                context.state().data(),
                new InvocationContext(
                    context.execution()
                        .id()
                        .value(),
                    null,
                    Map.of()
                )
            );

        CapabilityResult result =
            provider.invoke(invocation);

        if (result instanceof CapabilitySuccess success) {
            return new NodeCompleted(
                success.output()
            );
        }

        if (result instanceof CapabilityFailure failure) {
            return new NodeFailed(
                failure.code(),
                failure.message(),
                failure.retryable()
            );
        }

        if (result instanceof CapabilityWaiting waiting) {
            return new NodeWaiting(
                waiting.reason()
            );
        }

        if (result instanceof CapabilityCancelled cancelled) {
            return new NodeCancelled(
                cancelled.reason()
            );
        }

        throw new IllegalStateException(
            "Unknown CapabilityResult type: "
                + result.getClass()
        );
    }
}
