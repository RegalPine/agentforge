package io.github.regalpine.agentforge.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Routing decision —
 * AF-ENHANCE-09 §48.
 *
 * <p>Result of a routing operation:
 * selected model, all candidates,
 * and reasons for each.
 *
 * <p>Must be explainable:
 * why selected, why others rejected.
 */
public record RoutingDecision(
    String selectedModelId,
    String selectedProviderId,
    List<ModelCandidate> candidates,
    List<RoutingReason> reasons,
    RoutingStrategy strategy,
    long decisionTimeMs
) {

    public RoutingDecision {
        Objects.requireNonNull(
            selectedModelId, "selectedModelId"
        );
        Objects.requireNonNull(
            selectedProviderId,
            "selectedProviderId"
        );
        Objects.requireNonNull(
            strategy, "strategy"
        );

        candidates = candidates == null
            ? List.of()
            : List.copyOf(candidates);
        reasons = reasons == null
            ? List.of()
            : List.copyOf(reasons);
    }

    /**
     * Create a simple decision.
     */
    public static RoutingDecision of(
        String modelId,
        String providerId,
        RoutingStrategy strategy
    ) {
        return new RoutingDecision(
            modelId, providerId,
            List.of(), List.of(),
            strategy, 0
        );
    }

    /**
     * Get the reason for a specific
     * model.
     */
    public RoutingReason reasonFor(
        String modelId
    ) {
        return reasons.stream()
            .filter(r ->
                r.modelId().equals(modelId)
            )
            .findFirst()
            .orElse(null);
    }

    /**
     * Check if the decision has
     * a valid selection.
     */
    public boolean hasSelection() {
        return selectedModelId != null
            && !selectedModelId.isEmpty();
    }
}
