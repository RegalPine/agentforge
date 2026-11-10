package io.github.regalpine.agentforge.capability;

import java.util.Map;
import java.util.Objects;

/**
 * Enhanced invocation context.
 * AE-02 §44: Idempotency Key support.
 *
 * <p>Carries execution identity, tenant,
 * idempotency key and arbitrary attributes.
 */
public record InvocationContext(
    String executionId,
    String tenantId,
    String idempotencyKey,
    Map<String, Object> attributes
) {

    public InvocationContext {
        Objects.requireNonNull(
            executionId,
            "executionId"
        );

        attributes =
            attributes == null
                ? Map.of()
                : Map.copyOf(attributes);
    }

    public InvocationContext(
        String executionId,
        String tenantId,
        Map<String, Object> attributes
    ) {
        this(executionId, tenantId, null, attributes);
    }
}
