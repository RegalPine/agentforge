package io.github.regalpine.agentforge.provider.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Schema normalizer.
 * AE-10 §16: Schema Normalization.
 *
 * <p>Normalizes external tool schemas
 * (e.g. from MCP) into the AgentForge
 * internal representation.
 */
public final class SchemaNormalizer {

    private SchemaNormalizer() {}

    /**
     * Normalize a raw MCP tool descriptor
     * into an ExternalCapabilityDescriptor.
     */
    public static ExternalCapabilityDescriptor normalize(
        McpToolDescriptor tool,
        String providerId,
        String serverId
    ) {
        String capabilityId =
            providerId + "." + tool.toolName();

        Map<String, Object> normalizedInput =
            normalizeSchema(tool.inputSchema());

        return new ExternalCapabilityDescriptor(
            capabilityId,
            providerId,
            tool.toolName(),
            tool.toolName(),
            sanitizeDescription(tool.description()),
            normalizedInput,
            Map.of(),
            java.util.Set.of("mcp", serverId),
            null,
            null,
            CapabilityRegistrationStatus.DISCOVERED,
            Map.of(
                "source", "mcp",
                "server", serverId
            )
        );
    }

    /**
     * Normalize a raw schema map.
     * Ensures required keys are present.
     */
    public static Map<String, Object> normalizeSchema(
        Map<String, Object> raw
    ) {
        if (raw == null || raw.isEmpty()) {
            return Map.of(
                "type", "object",
                "properties", Map.of()
            );
        }

        Map<String, Object> normalized =
            new LinkedHashMap<>(raw);

        normalized.putIfAbsent("type", "object");
        normalized.putIfAbsent(
            "properties", Map.of()
        );

        return Map.copyOf(normalized);
    }

    /**
     * AE-10 §42: Tool Description
     * Injection Isolation.
     *
     * <p>Sanitize external tool descriptions
     * so they cannot act as prompt injection
     * vectors. Removes common injection patterns.
     */
    public static String sanitizeDescription(
        String description
    ) {
        if (description == null) {
            return "";
        }

        // Strip patterns that look like
        // system instructions
        String sanitized = description
            .replaceAll(
                "(?i)ignore\\s+(all\\s+)?previous",
                "[filtered]"
            )
            .replaceAll(
                "(?i)you\\s+are\\s+now",
                "[filtered]"
            )
            .replaceAll(
                "(?i)system\\s*:",
                "[filtered]"
            );

        return sanitized.trim();
    }
}
