package io.github.regalpine.agentforge.a2a;

import java.net.URI;
import java.util.Objects;

/**
 * External agent reference.
 * AF-ENHANCE-10 §29: Agent Reference.
 *
 * <p>Identifies an external agent
 * with namespace, name, version,
 * and endpoint.
 */
public record ExternalAgentReference(
    String namespace,
    String name,
    String version,
    URI endpoint
) {

    public ExternalAgentReference {
        Objects.requireNonNull(
            namespace, "namespace"
        );
        Objects.requireNonNull(
            name, "name"
        );
        Objects.requireNonNull(
            endpoint, "endpoint"
        );

        version = version == null
            ? "1.0.0" : version;
    }

    /**
     * Create a reference.
     */
    public static ExternalAgentReference of(
        String namespace,
        String name,
        URI endpoint
    ) {
        return new ExternalAgentReference(
            namespace, name, null, endpoint
        );
    }

    /**
     * Fully qualified name.
     */
    public String qualifiedName() {
        return namespace + ":" + name;
    }
}
