package io.github.regalpine.agentforge.multiagent.projection;

import io.github.regalpine.agentforge.multiagent.contract.AgentContract;

import java.util.Map;

/**
 * ContextProjection — AE-11 §7-8.
 *
 * Projects parent agent context into a child agent context.
 * Default: only pass task, user input, business parameters.
 * Never pass: system prompt, credentials, secrets, internal
 * policy, parent reasoning, other agent's private context.
 */
public interface ContextProjection {

    /**
     * Project parent context to child context.
     *
     * @param parentContext the parent's context data
     * @param childContract the child agent's contract
     * @return projected context for the child
     */
    ProjectedContext project(
        Map<String, Object> parentContext,
        AgentContract childContract
    );

    /**
     * Result of context projection.
     */
    record ProjectedContext(
        Map<String, Object> data,
        boolean secretRedacted
    ) {
        public ProjectedContext {
            data = data == null
                ? Map.of() : Map.copyOf(data);
        }

        public static ProjectedContext empty() {
            return new ProjectedContext(Map.of(), false);
        }
    }
}
