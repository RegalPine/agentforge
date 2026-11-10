package io.github.regalpine.agentforge.workspace;

import java.util.Objects;
import java.util.Set;

/**
 * Sandbox policy.
 * AF-ENHANCE-07 §35-36: Executable
 * Allowlist & Network Policy.
 *
 * <p>Controls: allowed executables,
 * network mode, isolation level,
 * resource limits.
 */
public record SandboxPolicy(
    String policyId,
    Set<String> allowedExecutables,
    NetworkMode networkMode,
    Set<String> allowedHosts,
    SandboxIsolationLevel isolationLevel,
    ResourceLimits limits
) {

    public SandboxPolicy {
        Objects.requireNonNull(
            policyId, "policyId"
        );

        allowedExecutables =
            allowedExecutables == null
                ? Set.of()
                : Set.copyOf(
                    allowedExecutables
                );

        networkMode =
            networkMode == null
                ? NetworkMode.DENY
                : networkMode;

        allowedHosts =
            allowedHosts == null
                ? Set.of()
                : Set.copyOf(allowedHosts);

        isolationLevel =
            isolationLevel == null
                ? SandboxIsolationLevel
                    .LOGICAL
                : isolationLevel;

        limits =
            limits == null
                ? ResourceLimits.defaults()
                : limits;
    }

    /**
     * Permissive sandbox policy.
     */
    public static SandboxPolicy permissive() {
        return new SandboxPolicy(
            "permissive",
            null,
            NetworkMode.FULL,
            null,
            SandboxIsolationLevel.LOGICAL,
            ResourceLimits.large()
        );
    }

    /**
     * Restrictive sandbox policy.
     */
    public static SandboxPolicy restrictive() {
        return new SandboxPolicy(
            "restrictive",
            Set.of("python", "java"),
            NetworkMode.DENY,
            null,
            SandboxIsolationLevel.LOGICAL,
            ResourceLimits.small()
        );
    }

    /**
     * Check if an executable is allowed.
     */
    public boolean isExecutableAllowed(
        String executable
    ) {
        if (allowedExecutables.isEmpty()) {
            return true;
        }
        return allowedExecutables.contains(
            executable
        );
    }

    /**
     * Check if a host is accessible.
     */
    public boolean isHostAllowed(
        String host
    ) {
        return switch (networkMode) {
            case DENY -> false;
            case FULL -> true;
            case ALLOWLIST ->
                allowedHosts.contains(host);
        };
    }
}
