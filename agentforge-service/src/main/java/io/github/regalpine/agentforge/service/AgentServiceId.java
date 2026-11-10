package io.github.regalpine.agentforge.service;

import java.util.Objects;

/**
 * Agent Service identity.
 * AF-ENHANCE-11 §5: namespace:name:version.
 *
 * <p>Service Identity is separate
 * from Agent Identity.
 */
public record AgentServiceId(
    String namespace,
    String name,
    String version
) {

    public AgentServiceId {
        Objects.requireNonNull(
            namespace, "namespace"
        );
        Objects.requireNonNull(
            name, "name"
        );

        version = version == null
            ? "1.0.0" : version;

        if (namespace.isBlank()) {
            throw new IllegalArgumentException(
                "namespace must not be blank"
            );
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException(
                "name must not be blank"
            );
        }
    }

    /**
     * Qualified name: namespace:name.
     */
    public String qualifiedName() {
        return namespace + ":" + name;
    }

    @Override
    public String toString() {
        return namespace + ":"
            + name + ":" + version;
    }
}
