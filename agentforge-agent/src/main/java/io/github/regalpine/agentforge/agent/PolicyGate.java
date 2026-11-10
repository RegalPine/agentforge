package io.github.regalpine.agentforge.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Policy gate for Agent decisions.
 * <p>
 * The PolicyGate sits between the Decision Engine and the Action phase.
 * It validates each decision against configured policies before allowing
 * the action to proceed.
 * <p>
 * Policies can check things like:
 * <ul>
 *   <li>Maximum tool calls per iteration</li>
 *   <li>Allowed tool IDs</li>
 *   <li>Rate limiting</li>
 *   <li>Custom business rules</li>
 * </ul>
 */
public final class PolicyGate {

    private final List<PolicyRule> rules;

    public PolicyGate(List<PolicyRule> rules) {
        this.rules = rules == null ? List.of() : List.copyOf(rules);
    }

    /**
     * Create an empty policy gate (allows everything).
     *
     * @return a permissive PolicyGate
     */
    public static PolicyGate permissive() {
        return new PolicyGate(List.of());
    }

    /**
     * Create a policy gate with a single rule.
     *
     * @param name the rule name
     * @param predicate the rule predicate (decision, context) -> allowed
     * @return a new PolicyGate
     */
    public static PolicyGate of(String name, BiPredicate<AgentDecision, AgentDecisionContext> predicate) {
        return new PolicyGate(List.of(new PolicyRule(name, predicate)));
    }

    /**
     * Check if a decision is allowed by all policies.
     *
     * @param decision the decision to check
     * @param context  the decision context
     * @return an Optional StopCondition if blocked, empty if allowed
     */
    public Optional<StopCondition> check(AgentDecision decision, AgentDecisionContext context) {
        Objects.requireNonNull(decision, "decision cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        for (PolicyRule rule : rules) {
            if (!rule.predicate().test(decision, context)) {
                return Optional.of(StopCondition.of(
                    StopCondition.StopReason.POLICY_BLOCKED,
                    "Policy rule '" + rule.name() + "' blocked decision: " + decision.getClass().getSimpleName()
                ));
            }
        }
        return Optional.empty();
    }

    /**
     * Get the number of configured rules.
     *
     * @return the rule count
     */
    public int ruleCount() {
        return rules.size();
    }

    /**
     * Get the configured rules (unmodifiable).
     *
     * @return the rules
     */
    public List<PolicyRule> rules() {
        return rules;
    }

    /**
     * A single policy rule.
     *
     * @param name      the rule name
     * @param predicate the rule predicate
     */
    public record PolicyRule(
        String name,
        BiPredicate<AgentDecision, AgentDecisionContext> predicate
    ) {
        public PolicyRule {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(predicate, "predicate cannot be null");
        }
    }
}
