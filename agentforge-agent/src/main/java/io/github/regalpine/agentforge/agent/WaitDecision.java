package io.github.regalpine.agentforge.agent;

import java.time.Instant;

/**
 * Decision to enter waiting state.
 * AF-06 §19: WaitDecision.
 */
public record WaitDecision(
    String waitKey,
    Instant resumeAfter
) implements AgentDecision {
}
