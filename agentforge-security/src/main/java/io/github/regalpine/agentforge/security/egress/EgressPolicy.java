package io.github.regalpine.agentforge.security.egress;

import java.util.*;

/**
 * EgressPolicy — AF-SEC-01 §45-46.
 *
 * Controls all outbound network access:
 * - Default: DENY
 * - Allowlist: explicit permitted endpoints
 * - Denylist: explicitly blocked endpoints
 *
 * All external network access must pass through
 * this policy. Agents cannot create arbitrary
 * network connections (§45).
 */
public record EgressPolicy(

    EgressDecision defaultDecision,

    Set<String> allowedHosts,

    Set<String> allowedPatterns,

    Set<String> deniedHosts,

    int maxConnectionsPerMinute
) {

    public EgressPolicy {
        defaultDecision = defaultDecision == null
            ? EgressDecision.DENY : defaultDecision;
        allowedHosts = allowedHosts == null
            ? Set.of() : Set.copyOf(allowedHosts);
        allowedPatterns = allowedPatterns == null
            ? Set.of() : Set.copyOf(allowedPatterns);
        deniedHosts = deniedHosts == null
            ? Set.of() : Set.copyOf(deniedHosts);
        if (maxConnectionsPerMinute <= 0) {
            throw new IllegalArgumentException(
                "maxConnectionsPerMinute must be > 0");
        }
    }

    /**
     * Default deny-all policy.
     */
    public static EgressPolicy denyAll() {
        return new EgressPolicy(
            EgressDecision.DENY,
            Set.of(), Set.of(), Set.of(),
            100
        );
    }

    /**
     * Create a policy with specific allowed hosts.
     */
    public static EgressPolicy withAllowed(
        Set<String> allowedHosts
    ) {
        return new EgressPolicy(
            EgressDecision.DENY,
            allowedHosts, Set.of(), Set.of(),
            1000
        );
    }

    /**
     * Check if a host is allowed.
     */
    public EgressDecision check(String host) {
        if (host == null || host.isBlank()) {
            return EgressDecision.DENY;
        }

        String lower = host.toLowerCase();

        // Denylist takes priority
        if (deniedHosts.contains(lower)) {
            return EgressDecision.DENY;
        }

        // Allowlist
        if (allowedHosts.contains(lower)) {
            return EgressDecision.ALLOW;
        }

        // Pattern matching
        for (String pattern : allowedPatterns) {
            if (lower.matches(pattern)) {
                return EgressDecision.ALLOW;
            }
        }

        return defaultDecision;
    }

    /**
     * Egress decision.
     */
    public enum EgressDecision {
        ALLOW,
        DENY
    }
}
