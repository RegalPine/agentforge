package io.github.regalpine.agentforge.provider.mcp;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Protocol capabilities.
 * AF-ENHANCE-10 §66: ProtocolAdapter.
 */
public record ProtocolCapabilities(
    boolean discovery,
    boolean invocation,
    boolean streaming,
    boolean authentication,
    boolean resourceAccess,
    boolean promptAccess,
    Set<String> extensions
) {

    public ProtocolCapabilities {
        extensions = extensions == null
            ? Set.of() : Set.copyOf(extensions);
    }

    /**
     * Create with all capabilities
     * enabled.
     */
    public static ProtocolCapabilities full() {
        return new ProtocolCapabilities(
            true, true, true, true,
            true, true, Set.of()
        );
    }

    /**
     * Create with minimal capabilities.
     */
    public static ProtocolCapabilities minimal() {
        return new ProtocolCapabilities(
            true, true, false, false,
            false, false, Set.of()
        );
    }

    /**
     * Check if a specific extension
     * is supported.
     */
    public boolean hasExtension(
        String name
    ) {
        return extensions.contains(name);
    }
}
