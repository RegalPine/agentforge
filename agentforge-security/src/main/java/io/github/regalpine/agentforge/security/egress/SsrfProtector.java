package io.github.regalpine.agentforge.security.egress;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SsrfProtector — AF-SEC-01 §46.
 *
 * Prevents Server-Side Request Forgery (SSRF) by
 * validating URLs against dangerous targets:
 * - localhost / 127.0.0.1
 * - Link-local (169.254.x.x, including AWS metadata)
 * - Private networks (10.x, 172.16-31.x, 192.168.x)
 * - Internal admin endpoints
 *
 * Production: Allowlist > Denylist (§46).
 */
public final class SsrfProtector {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "::1",
        "169.254.169.254",
        "metadata.google.internal",
        "metadata.internal"
    );

    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
        // Private networks
        Pattern.compile("^10\\..*"),
        Pattern.compile("^172\\.(1[6-9]|2[0-9]|3[01])\\..*"),
        Pattern.compile("^192\\.168\\..*"),
        // Link-local
        Pattern.compile("^169\\.254\\..*"),
        // Loopback variants
        Pattern.compile("^127\\..*"),
        // IPv6 loopback
        Pattern.compile("^\\[?::1\\]?")
    );

    private final Set<String> additionalBlockedHosts;
    private final Set<String> allowedHosts;

    public SsrfProtector() {
        this(Set.of(), Set.of());
    }

    public SsrfProtector(
        Set<String> additionalBlockedHosts,
        Set<String> allowedHosts
    ) {
        this.additionalBlockedHosts = additionalBlockedHosts == null
            ? Set.of() : Set.copyOf(additionalBlockedHosts);
        this.allowedHosts = allowedHosts == null
            ? Set.of() : Set.copyOf(allowedHosts);
    }

    /**
     * Check if a URL is safe to access.
     *
     * @param url the URL to validate
     * @return validation result
     */
    public SsrfValidation validate(String url) {
        if (url == null || url.isBlank()) {
            return SsrfValidation.blocked(url, "URL is null or blank");
        }

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return SsrfValidation.blocked(url,
                "Invalid URL: " + e.getMessage());
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return SsrfValidation.blocked(url,
                "URL has no host");
        }

        String lowerHost = host.toLowerCase();

        // Allowlist takes priority
        if (allowedHosts.contains(lowerHost)) {
            return SsrfValidation.allowed(url);
        }

        // Check blocked hosts
        if (BLOCKED_HOSTS.contains(lowerHost)
            || additionalBlockedHosts.contains(lowerHost)) {
            return SsrfValidation.blocked(url,
                "Host is blocked: " + host);
        }

        // Check blocked patterns (private IPs)
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(lowerHost).matches()) {
                return SsrfValidation.blocked(url,
                    "Host matches blocked pattern: " + host);
            }
        }

        // Check scheme
        String scheme = uri.getScheme();
        if (scheme != null
            && !scheme.equals("http")
            && !scheme.equals("https")) {
            return SsrfValidation.blocked(url,
                "Scheme not allowed: " + scheme);
        }

        return SsrfValidation.allowed(url);
    }

    /**
     * SSRF validation result.
     */
    public record SsrfValidation(
        String url,
        boolean allowed,
        String reason
    ) {

        public static SsrfValidation allowed(String url) {
            return new SsrfValidation(url, true, "");
        }

        public static SsrfValidation blocked(
            String url, String reason
        ) {
            return new SsrfValidation(url, false, reason);
        }
    }
}
