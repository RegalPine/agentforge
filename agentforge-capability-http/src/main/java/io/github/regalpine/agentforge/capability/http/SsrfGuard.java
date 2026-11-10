package io.github.regalpine.agentforge.capability.http;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * SSRF (Server-Side Request Forgery)
 * protection guard.
 * AF-ENHANCE-02 §11: validates
 * URLs before making HTTP requests.
 *
 * <p>Protection rules (in order):
 * <ol>
 *   <li>Allowlist takes priority — if
 *       allowedHosts is non-empty, only
 *       those hosts are permitted</li>
 *   <li>Block localhost / loopback</li>
 *   <li>Block private network ranges
 *       (10.x, 172.16-31.x, 192.168.x)</li>
 *   <li>Block link-local (169.254.x)
 *       including cloud metadata
 *       endpoints</li>
 *   <li>Block IPv6 loopback / link-local</li>
 * </ol>
 */
public final class SsrfGuard {

    private final Set<String> allowedHosts;
    private final boolean blockPrivateNetworks;
    private final boolean blockMetadataEndpoints;

    public SsrfGuard(
        Set<String> allowedHosts,
        boolean blockPrivateNetworks,
        boolean blockMetadataEndpoints
    ) {
        this.allowedHosts =
            allowedHosts == null
                ? Set.of()
                : Set.copyOf(allowedHosts);

        this.blockPrivateNetworks =
            blockPrivateNetworks;

        this.blockMetadataEndpoints =
            blockMetadataEndpoints;
    }

    public SsrfGuard(Set<String> allowedHosts) {
        this(allowedHosts, true, true);
    }

    public SsrfGuard() {
        this(Set.of(), true, true);
    }

    /**
     * Validate a URI against SSRF
     * rules.
     *
     * @throws SsrfViolationException
     *   if the URI is blocked
     */
    public void validate(URI uri) {
        if (uri == null) {
            throw new SsrfViolationException(
                "URI must not be null"
            );
        }

        String scheme = uri.getScheme();

        if (scheme == null) {
            throw new SsrfViolationException(
                "URI scheme is required"
            );
        }

        String lowerScheme = scheme.toLowerCase();

        if (!lowerScheme.equals("http")
            && !lowerScheme.equals("https")
        ) {
            throw new SsrfViolationException(
                "Only http/https schemes allowed: "
                    + scheme
            );
        }

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            throw new SsrfViolationException(
                "URI host is required"
            );
        }

        // 1. Allowlist takes priority
        if (!allowedHosts.isEmpty()) {
            if (!allowedHosts.contains(host)) {
                throw new SsrfViolationException(
                    "Host not in allowlist: " + host
                );
            }

            return;
        }

        // 2. Block localhost
        String lowerHost = host.toLowerCase();

        // Strip IPv6 brackets
        String bareHost = lowerHost;

        if (bareHost.startsWith("[")
            && bareHost.endsWith("]")
        ) {
            bareHost = bareHost.substring(
                1, bareHost.length() - 1
            );
        }

        if (lowerHost.equals("localhost")
            || bareHost.equals("127.0.0.1")
            || bareHost.equals("::1")
            || bareHost.equals("0.0.0.0")
        ) {
            throw new SsrfViolationException(
                "Loopback addresses are blocked: "
                    + host
            );
        }

        if (!blockPrivateNetworks
            && !blockMetadataEndpoints
        ) {
            return;
        }

        // 3. DNS resolution for IP check
        try {
            InetAddress[] addresses =
                InetAddress.getAllByName(host);

            for (InetAddress addr : addresses) {
                checkAddress(addr, host);
            }
        } catch (UnknownHostException e) {
            throw new SsrfViolationException(
                "Cannot resolve host: " + host
            );
        }
    }

    private void checkAddress(
        InetAddress addr,
        String originalHost
    ) {
        byte[] bytes = addr.getAddress();

        if (blockMetadataEndpoints) {
            // 169.254.169.254 — cloud metadata
            if (bytes.length == 4
                && (bytes[0] & 0xFF) == 169
                && (bytes[1] & 0xFF) == 254
            ) {
                throw new SsrfViolationException(
                    "Link-local / metadata endpoint blocked: "
                        + originalHost
                );
            }

            // 0:0:0:0:0:ffff:a9fe:a9fe (IPv4-mapped)
            if (bytes.length == 16
                && isIpv4Mapped(bytes)
            ) {
                int b0 = bytes[12] & 0xFF;
                int b1 = bytes[13] & 0xFF;

                if (b0 == 169 && b1 == 254) {
                    throw new SsrfViolationException(
                        "Link-local / metadata endpoint blocked: "
                            + originalHost
                    );
                }
            }
        }

        if (blockPrivateNetworks) {
            if (bytes.length == 4) {
                int b0 = bytes[0] & 0xFF;
                int b1 = bytes[1] & 0xFF;

                // 10.0.0.0/8
                if (b0 == 10) {
                    throw new SsrfViolationException(
                        "Private network blocked (10.x): "
                            + originalHost
                    );
                }

                // 172.16.0.0/12
                if (b0 == 172
                    && b1 >= 16 && b1 <= 31
                ) {
                    throw new SsrfViolationException(
                        "Private network blocked (172.16-31.x): "
                            + originalHost
                    );
                }

                // 192.168.0.0/16
                if (b0 == 192 && b1 == 168) {
                    throw new SsrfViolationException(
                        "Private network blocked (192.168.x): "
                            + originalHost
                    );
                }
            }

            // IPv6 link-local: fe80::/10
            if (bytes.length == 16) {
                int b0 = bytes[0] & 0xFF;
                int b1 = bytes[1] & 0xFF;

                if (b0 == 0xFE && (b1 & 0xC0) == 0x80) {
                    throw new SsrfViolationException(
                        "IPv6 link-local blocked: "
                            + originalHost
                    );
                }
            }
        }
    }

    private boolean isIpv4Mapped(byte[] bytes) {
        // ::ffff:x.x.x.x
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }

        return bytes[10] == (byte) 0xFF
            && bytes[11] == (byte) 0xFF;
    }

    /**
     * Thrown when a URI is blocked by
     * SSRF protection.
     */
    public static class SsrfViolationException
        extends RuntimeException {

        public SsrfViolationException(
            String message
        ) {
            super(message);
        }
    }
}
