package io.github.regalpine.agentforge.evolution;

import java.util.List;
import java.util.Set;

/**
 * Evolution policy configuration.
 * AF-ENHANCE-15 §58: Evolution
 * Policy.
 */
public record EvolutionPolicy(
    String policyId,
    Set<ChangeScope> allowedScopes,
    Set<ChangeScope> requireHumanApproval,
    boolean requiredEvaluation,
    boolean requiredRegressionCheck,
    boolean autoReleaseEnabled
) {

    public EvolutionPolicy {
        if (policyId == null
            || policyId.isBlank()) {
            throw new IllegalArgumentException(
                "policyId required");
        }
        allowedScopes = allowedScopes == null
            ? Set.of()
            : Set.copyOf(allowedScopes);
        requireHumanApproval =
            requireHumanApproval == null
                ? Set.of()
                : Set.copyOf(
                    requireHumanApproval);
    }

    /**
     * Create default policy.
     */
    public static EvolutionPolicy defaults(
        String policyId
    ) {
        return new EvolutionPolicy(
            policyId,
            Set.of(
                ChangeScope.PROMPT,
                ChangeScope.MODEL,
                ChangeScope.SKILL
            ),
            Set.of(
                ChangeScope.WORKFLOW,
                ChangeScope.CONFIGURATION
            ),
            true, true, false
        );
    }

    /**
     * Check if a scope is allowed.
     */
    public boolean isAllowed(
        ChangeScope scope
    ) {
        return allowedScopes
            .contains(scope);
    }

    /**
     * Check if scope requires human
     * approval.
     */
    public boolean needsApproval(
        ChangeScope scope
    ) {
        return requireHumanApproval
            .contains(scope);
    }
}
