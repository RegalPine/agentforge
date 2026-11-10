package io.github.regalpine.agentforge.provider.agent;

import io.github.regalpine.agentforge.kernel.Reference;

import java.util.Map;

public record AgentInvocation(
    Reference agent,
    Map<String, Object> inputs,
    AgentCallContext context
) {

    public AgentInvocation {
        inputs =
            inputs == null
                ? Map.of()
                : Map.copyOf(inputs);
    }
}
