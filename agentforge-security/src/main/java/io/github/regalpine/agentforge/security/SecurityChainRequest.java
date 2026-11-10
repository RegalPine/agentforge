package io.github.regalpine.agentforge.security;

import java.util.Map;
import java.util.Objects;

/**
 * Request context for the security execution chain.
 */
public record SecurityChainRequest(
    String actionType,
    String principalId,
    String tenantId,
    String resourceType,
    String resourceId,
    Map<String, Object> attributes
) {

    public SecurityChainRequest {
        Objects.requireNonNull(actionType, "actionType cannot be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static SecurityChainRequest authenticate(String principalId, String tenantId) {
        return new SecurityChainRequest("AUTHENTICATE", principalId, tenantId, null, null, null);
    }

    public static SecurityChainRequest authorize(String principalId, String tenantId, String resourceType, String resourceId) {
        return new SecurityChainRequest("AUTHORIZE", principalId, tenantId, resourceType, resourceId, null);
    }

    public static SecurityChainRequest invokeCapability(String principalId, String tenantId, String capabilityId) {
        return new SecurityChainRequest("INVOKE_CAPABILITY", principalId, tenantId, "capability", capabilityId, null);
    }
}
