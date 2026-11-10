package io.github.regalpine.agentforge.multiagent.policy;

import io.github.regalpine.agentforge.kernel.Reference;

import java.util.Map;

/**
 * DelegationPolicy — AE-11 §21-22.
 *
 * Authorizes agent delegation by checking:
 * parent permission, child permission, tenant boundary,
 * capability permission, data classification, action risk,
 * budget, delegation depth.
 *
 * <p>Child agent permissions must NOT automatically exceed
 * what the parent agent can grant.
 */
public interface DelegationPolicy {

    /**
     * Authorize a delegation from parent to child.
     *
     * @param parent  the parent agent reference
     * @param child   the child agent reference
     * @param context delegation context attributes
     * @return the policy decision
     */
    DelegationPolicyDecision authorize(
        Reference parent,
        Reference child,
        Map<String, Object> context
    );
}
