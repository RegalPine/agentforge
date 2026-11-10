package io.github.regalpine.agentforge.agent;

import java.time.Instant;

/**
 * Agent observation after a capability
 * or Skill invocation.
 * AF-06 §40: AgentObservation.
 * AF-SKILL-01 §30: Skill Result 进入
 * Observation.
 * AF-AD-15 §7.3: sourceType=SKILL /
 * trustLevel=DERIVED.
 *
 * <p>Records the result of a capability
 * or Skill invocation within the Agent
 * decision loop. The observation feeds
 * back into the next decision cycle.
 */
public record AgentObservation(
    String invocationId,
    io.github.regalpine.agentforge.capability.CapabilityResult result,
    Instant timestamp,
    SourceType sourceType,
    TrustLevel trustLevel
) {

    /**
     * Backward-compatible constructor
     * defaulting to CAPABILITY / DERIVED.
     */
    public AgentObservation(
        String invocationId,
        io.github.regalpine.agentforge.capability.CapabilityResult result,
        Instant timestamp
    ) {
        this(
            invocationId,
            result,
            timestamp,
            SourceType.CAPABILITY,
            TrustLevel.DERIVED
        );
    }

    /**
     * Observation source type.
     */
    public enum SourceType {
        CAPABILITY,
        SKILL,
        TOOL,
        AGENT
    }

    /**
     * Trust level of observation data.
     * AF-AD-15 §7.3: Skill Result
     * defaults to DERIVED / EXTERNAL.
     */
    public enum TrustLevel {
        SYSTEM,
        DERIVED,
        EXTERNAL,
        UNTRUSTED
    }
}
