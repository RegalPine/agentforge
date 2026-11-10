package io.github.regalpine.agentforge.provider.http;

import java.net.URI;
import java.net.InetAddress;
import java.util.Set;

public final class SsrfValidator {

    private static final Set<String> BLOCKED_SCHEMES =
        Set.of("file", "ftp", "gopher", "telnet");

    private static final Set<String> ALLOWED_SCHEMES =
        Set.of("http", "https");

    public void validate(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException(
                "URI cannot be null"
            );
        }

        String scheme = uri.getScheme();

        if (scheme == null
            || !ALLOWED_SCHEMES.contains(
                scheme.toLowerCase()
            )
        ) {
            throw new SecurityException(
                "Blocked scheme: " + scheme
            );
        }

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(
                "URI must have a host"
            );
        }

        if (isLoopback(host)
            || isPrivateNetwork(host)
            || isLinkLocal(host)
            || isMetadataEndpoint(host)
        ) {
            throw new SecurityException(
                "Blocked host: " + host
            );
        }
    }

    private boolean isLoopback(String host) {
        if ("localhost".equalsIgnoreCase(host)) {
            return true;
        }

        if ("127.0.0.1".equals(host)
            || "0.0.0.0".equals(host)
        ) {
            return true;
        }

        try {
            InetAddress addr =
                InetAddress.getByName(host);
            return addr.isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPrivateNetwork(String host) {
        try {
            InetAddress addr =
                InetAddress.getByName(host);

            return addr.isSiteLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLinkLocal(String host) {
        try {
            InetAddress addr =
                InetAddress.getByName(host);

            return addr.isLinkLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isMetadataEndpoint(
        String host
    ) {
        return "169.254.169.254".equals(host)
            || "metadata.google.internal"
                .equalsIgnoreCase(host);
    }
}
