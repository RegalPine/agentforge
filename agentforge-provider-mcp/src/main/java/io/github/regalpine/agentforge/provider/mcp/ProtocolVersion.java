package io.github.regalpine.agentforge.provider.mcp;

import java.util.Map;
import java.util.Objects;

/**
 * Protocol version.
 * AF-ENHANCE-10 §69: Protocol Versioning.
 *
 * <p>Protocol version is independent
 * of AgentForge version.
 */
public record ProtocolVersion(
    String value
) {

    public ProtocolVersion {
        Objects.requireNonNull(
            value, "value"
        );
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "version must not be blank"
            );
        }
    }

    /**
     * MCP protocol version.
     */
    public static ProtocolVersion mcp(
        String version
    ) {
        return new ProtocolVersion(version);
    }

    /**
     * A2A protocol version.
     */
    public static ProtocolVersion a2a(
        String version
    ) {
        return new ProtocolVersion(version);
    }

    @Override
    public String toString() {
        return value;
    }
}
