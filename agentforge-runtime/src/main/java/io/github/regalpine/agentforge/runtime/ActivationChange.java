package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.kernel.EntityId;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 激活变更：记录一次提交中新增、完成和等待的激活。 */
public record ActivationChange(
    String activationId,
    EntityId nodeId,
    ActivationStatus status,
    String frameToken,
    int iterationIndex,
    Map<String, Object> result
) {
    public enum ActivationStatus { STARTED, COMPLETED, FAILED, CANCELLED, WAITING }

    public ActivationChange {
        Objects.requireNonNull(activationId);
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(status);
        result = result == null ? Map.of() : Map.copyOf(result);
    }

    public static ActivationChange started(String activationId, EntityId nodeId) {
        return new ActivationChange(activationId, nodeId, ActivationStatus.STARTED, null, 0, Map.of());
    }

    public static ActivationChange completed(String activationId, EntityId nodeId, Map<String, Object> result) {
        return new ActivationChange(activationId, nodeId, ActivationStatus.COMPLETED, null, 0, result);
    }

    public static ActivationChange failed(String activationId, EntityId nodeId) {
        return new ActivationChange(activationId, nodeId, ActivationStatus.FAILED, null, 0, Map.of());
    }

    public static ActivationChange cancelled(String activationId, EntityId nodeId) {
        return new ActivationChange(activationId, nodeId, ActivationStatus.CANCELLED, null, 0, Map.of());
    }

    public static ActivationChange waiting(String activationId, EntityId nodeId, String frameToken, Set<EntityId> conditions) {
        return new ActivationChange(activationId, nodeId, ActivationStatus.WAITING, frameToken, 0,
            Map.of("conditions", conditions.stream().map(EntityId::value).toList()));
    }
}
