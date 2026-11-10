package io.github.regalpine.agentforge.evaluation.dataset;

import java.util.Objects;

/**
 * Dataset identity.
 * AF-ENHANCE-12 §7: DatasetId.
 */
public record DatasetId(
    String namespace,
    String name,
    String version
) {

    public DatasetId {
        Objects.requireNonNull(
            namespace, "namespace"
        );
        Objects.requireNonNull(
            name, "name"
        );

        version = version == null
            ? "1.0.0" : version;
    }

    @Override
    public String toString() {
        return namespace + "/"
            + name + "/" + version;
    }
}
