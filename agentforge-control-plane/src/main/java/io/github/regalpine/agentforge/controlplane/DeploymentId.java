package io.github.regalpine.agentforge.controlplane;

/**
 * Deployment identity.
 * AF-ENHANCE-14 §7: Deployment Identity.
 *
 * <p>Deployment Identity ≠ Agent Identity.
 */
public record DeploymentId(
    String namespace,
    String name
) {

    public DeploymentId {
        if (namespace == null
            || namespace.isBlank()) {
            throw new IllegalArgumentException(
                "namespace required");
        }
        if (name == null
            || name.isBlank()) {
            throw new IllegalArgumentException(
                "name required");
        }
    }

    /**
     * Qualified name: namespace/name.
     */
    public String qualifiedName() {
        return namespace + "/" + name;
    }
}
