package io.github.regalpine.agentforge.security.supply;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * ComponentRecord — AF-SEC-01 §49-50.
 *
 * Represents a single component in the
 * Software Bill of Materials (SBOM).
 *
 * Tracks: component ID, version, source,
 * checksum, license, security status.
 */
public record ComponentRecord(

    String componentId,

    String name,

    String version,

    ComponentType type,

    String source,

    String checksum,

    String license,

    SecurityStatus securityStatus,

    Instant recordedAt,

    Map<String, Object> metadata
) {

    public ComponentRecord {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(type, "type");

        securityStatus = securityStatus == null
            ? SecurityStatus.UNKNOWN : securityStatus;
        recordedAt = recordedAt == null
            ? Instant.now() : recordedAt;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Component types in the supply chain.
     */
    public enum ComponentType {
        JAVA_DEPENDENCY,
        MODEL_PROVIDER,
        TOOL_PROVIDER,
        MCP_SERVER,
        AGENT_PACKAGE,
        KNOWLEDGE_SOURCE,
        CONTAINER_IMAGE,
        RUNTIME_PLUGIN
    }

    /**
     * Security status of a component.
     */
    public enum SecurityStatus {
        VERIFIED,
        APPROVED,
        UNKNOWN,
        VULNERABLE,
        BLOCKED
    }
}
