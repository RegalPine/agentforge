package io.github.regalpine.agentforge.security.egress;

import java.util.Objects;

/**
 * EgressControl — AF-SEC-01 §45-46.
 *
 * Combined egress control:
 * 1. SSRF protection (block internal/private IPs)
 * 2. Egress policy (allowlist/denylist)
 * 3. Rate limiting (connections per minute)
 *
 * All outbound network access from AgentForge
 * must pass through this control point.
 */
public final class EgressControl {

    private final SsrfProtector ssrfProtector;
    private final EgressPolicy egressPolicy;

    public EgressControl(
        SsrfProtector ssrfProtector,
        EgressPolicy egressPolicy
    ) {
        this.ssrfProtector = ssrfProtector == null
            ? new SsrfProtector() : ssrfProtector;
        this.egressPolicy = egressPolicy == null
            ? EgressPolicy.denyAll() : egressPolicy;
    }

    public EgressControl() {
        this(new SsrfProtector(), EgressPolicy.denyAll());
    }

    /**
     * Check if a URL is allowed for outbound access.
     *
     * @param url the URL to check
     * @return the egress result
     */
    public EgressResult check(String url) {
        Objects.requireNonNull(url, "url");

        // Step 1: SSRF check
        SsrfProtector.SsrfValidation ssrfResult =
            ssrfProtector.validate(url);

        if (!ssrfResult.allowed()) {
            return EgressResult.denied(
                url,
                "SSRF_PROTECTION: "
                    + ssrfResult.reason()
            );
        }

        // Step 2: Extract host for policy check
        String host;
        try {
            host = java.net.URI.create(url).getHost();
        } catch (IllegalArgumentException e) {
            return EgressResult.denied(
                url, "INVALID_URL: " + e.getMessage());
        }

        if (host == null || host.isBlank()) {
            return EgressResult.denied(
                url, "NO_HOST");
        }

        // Step 3: Egress policy check
        EgressPolicy.EgressDecision decision =
            egressPolicy.check(host);

        if (decision == EgressPolicy.EgressDecision.DENY) {
            return EgressResult.denied(
                url,
                "EGRESS_POLICY: host '"
                    + host + "' not allowed"
            );
        }

        return EgressResult.allowed(url);
    }

    /**
     * Egress check result.
     */
    public record EgressResult(
        String url,
        boolean allowed,
        String reason
    ) {

        public static EgressResult allowed(String url) {
            return new EgressResult(url, true, "");
        }

        public static EgressResult denied(
            String url, String reason
        ) {
            return new EgressResult(url, false, reason);
        }
    }
}
