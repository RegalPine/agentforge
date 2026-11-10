package io.github.regalpine.agentforge.controlplane;

/**
 * Environment identity.
 * AF-ENHANCE-14 §16: Environment.
 */
public record EnvironmentId(
    String value
) {

    public EnvironmentId {
        if (value == null
            || value.isBlank()) {
            throw new IllegalArgumentException(
                "value required");
        }
    }

    /**
     * Standard environments.
     */
    public static final EnvironmentId DEV =
        new EnvironmentId("dev");

    public static final EnvironmentId TEST =
        new EnvironmentId("test");

    public static final EnvironmentId STAGING =
        new EnvironmentId("staging");

    public static final EnvironmentId PRODUCTION =
        new EnvironmentId("production");
}
