package io.github.regalpine.agentforge.multiagent.delegation;

import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.multiagent.result.AgentResult;

import java.util.Map;

/**
 * AgentDelegator — AE-11 §11, §58.
 *
 * Core delegation interface. Delegates execution
 * from a parent agent to a child agent.
 *
 * <p>Flow: Resolve → Validate Contract → Authorize →
 * Create Child Execution → Project Context → Execute →
 * Validate Result → Return.
 */
public interface AgentDelegator {

    /**
     * Delegate to a child agent.
     *
     * @param agent   the child agent reference
     * @param input   the structured input
     * @param context the delegation context
     * @return the child agent's result
     */
    AgentResult<Map<String, Object>> delegate(
        Reference agent,
        Map<String, Object> input,
        DelegationContext context
    );
}
