package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Routing context —
 * AF-ENHANCE-09 §24, §31.
 *
 * <p>Context for routing decisions:
 * required capabilities, tenant,
 * region, data classification,
 * budget, preferences.
 */
public record RoutingContext(
    Set<String> requiredCapabilities,
    String tenantId,
    String region,
    String dataClassification,
    ModelBudget budget,
    Map<String, String> preferences
) {

    public RoutingContext {
        requiredCapabilities =
            requiredCapabilities == null
                ? Set.of()
                : Set.copyOf(requiredCapabilities);
        preferences = preferences == null
            ? Map.of()
            : Map.copyOf(preferences);
    }

    /**
     * Empty routing context.
     */
    public static RoutingContext empty() {
        return new RoutingContext(
            null, null, null, null, null, null
        );
    }

    /**
     * Builder for RoutingContext.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for RoutingContext.
     */
    public static class Builder {
        private Set<String> caps;
        private String tenantId;
        private String region;
        private String dataClass;
        private ModelBudget budget;
        private Map<String, String> prefs;

        public Builder capabilities(
            Set<String> caps
        ) {
            this.caps = caps;
            return this;
        }

        public Builder tenant(String t) {
            this.tenantId = t;
            return this;
        }

        public Builder region(String r) {
            this.region = r;
            return this;
        }

        public Builder dataClassification(
            String dc
        ) {
            this.dataClass = dc;
            return this;
        }

        public Builder budget(ModelBudget b) {
            this.budget = b;
            return this;
        }

        public Builder preferences(
            Map<String, String> p
        ) {
            this.prefs = p;
            return this;
        }

        public RoutingContext build() {
            return new RoutingContext(
                caps, tenantId, region,
                dataClass, budget, prefs
            );
        }
    }
}
