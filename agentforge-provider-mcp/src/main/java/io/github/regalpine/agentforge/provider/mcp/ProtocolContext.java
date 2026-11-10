package io.github.regalpine.agentforge.provider.mcp;

import java.util.Map;
import java.util.Objects;

/**
 * Protocol context.
 * AF-ENHANCE-10 §67-68: ProtocolContext.
 *
 * <p>Carries authentication,
 * tenant, and tracing information
 * for protocol operations.
 */
public record ProtocolContext(
    String tenantId,
    String executionId,
    String traceId,
    String credentialReference,
    Map<String, Object> attributes
) {

    public ProtocolContext {
        Objects.requireNonNull(
            tenantId, "tenantId"
        );

        executionId = executionId == null
            ? "" : executionId;
        traceId = traceId == null
            ? "" : traceId;
        credentialReference =
            credentialReference == null
                ? "" : credentialReference;
        attributes = attributes == null
            ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Create a minimal context.
     */
    public static ProtocolContext of(
        String tenantId
    ) {
        return new ProtocolContext(
            tenantId, null, null,
            null, null
        );
    }

    /**
     * Create with execution info.
     */
    public static ProtocolContext of(
        String tenantId,
        String executionId,
        String traceId
    ) {
        return new ProtocolContext(
            tenantId, executionId,
            traceId, null, null
        );
    }
}
