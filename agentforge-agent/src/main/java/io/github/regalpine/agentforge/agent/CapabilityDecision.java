package io.github.regalpine.agentforge.agent;

import java.util.Map;

/**
 * Decision to invoke a capability.
 * AF-06 §11: CapabilityDecision.
 */
public record CapabilityDecision(
    DecisionType type,
    String capabilityId,
    Map<String, Object> arguments,
    String reason
) implements AgentDecision {

    public CapabilityDecision {
        if (type != DecisionType.CALL_CAPABILITY) {
            throw new IllegalArgumentException(
                "CapabilityDecision type must be CALL_CAPABILITY"
            );
        }

        arguments =
            arguments == null
                ? Map.of()
                : Map.copyOf(arguments);
    }
}
