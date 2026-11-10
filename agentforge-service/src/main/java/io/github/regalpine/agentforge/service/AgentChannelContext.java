package io.github.regalpine.agentforge.service;

import java.util.Map;
import java.util.Objects;

/**
 * Agent Channel context.
 * AF-ENHANCE-11 §59: Channel Context.
 *
 * <p>Carries tenant, actor,
 * service, and security
 * information for channel
 * operations.
 */
public record AgentChannelContext(
    String tenantId,
    String actorId,
    String applicationId,
    AgentServiceId serviceId,
    Map<String, Object> attributes
) {

    public AgentChannelContext {
        Objects.requireNonNull(
            tenantId, "tenantId"
        );

        actorId = actorId == null
            ? "" : actorId;
        applicationId = applicationId == null
            ? "" : applicationId;
        attributes = attributes == null
            ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Create a minimal context.
     */
    public static AgentChannelContext of(
        String tenantId
    ) {
        return new AgentChannelContext(
            tenantId, "", "", null, Map.of()
        );
    }
}
