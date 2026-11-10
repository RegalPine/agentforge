package io.github.regalpine.agentforge.skill.governance;

import java.util.Map;
import java.util.Set;

/**
 * Skill 生命周期状态机.
 * AF-SKILL-02 §23-§25: Lifecycle States.
 * AF-AD-15 §5.5: 版本与生命周期.
 *
 * <p>State machine:
 * <pre>
 * [*] → DRAFT → VALIDATED → TESTED → EVALUATED
 *       → APPROVED → PUBLISHED → ACTIVE
 *       → DEPRECATED → RETIRED
 * Active/Published → REVOKED
 * </pre>
 *
 * <p>Rules:
 * <ul>
 *   <li>DRAFT → ACTIVE 跳过验证被拒绝</li>
 *   <li>已发布版本不可变</li>
 *   <li>Rollback 只改变 Default Pointer</li>
 * </ul>
 */
public enum SkillLifecycleState {

    DRAFT,

    VALIDATED,

    TESTED,

    EVALUATED,

    APPROVED,

    PUBLISHED,

    ACTIVE,

    DEPRECATED,

    RETIRED,

    REVOKED;

    private static final Map<SkillLifecycleState, Set<SkillLifecycleState>>
        TRANSITIONS = Map.of(
            DRAFT,      Set.of(VALIDATED),
            VALIDATED,  Set.of(TESTED, DRAFT),
            TESTED,     Set.of(EVALUATED, VALIDATED),
            EVALUATED,  Set.of(APPROVED, TESTED),
            APPROVED,   Set.of(PUBLISHED, EVALUATED),
            PUBLISHED,  Set.of(ACTIVE, REVOKED, APPROVED),
            ACTIVE,     Set.of(DEPRECATED, REVOKED, PUBLISHED),
            DEPRECATED, Set.of(RETIRED, ACTIVE),
            RETIRED,    Set.of(),
            REVOKED,    Set.of()
        );

    /**
     * Check if transition to target is allowed.
     */
    public boolean canTransitionTo(
        SkillLifecycleState target
    ) {
        Set<SkillLifecycleState> allowed =
            TRANSITIONS.get(this);
        return allowed != null
            && allowed.contains(target);
    }

    /**
     * Validate and return the target state.
     *
     * @throws IllegalStateException if
     *   transition is not allowed
     */
    public SkillLifecycleState transitionTo(
        SkillLifecycleState target
    ) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                "Cannot transition from "
                    + this + " to " + target
            );
        }
        return target;
    }

    /**
     * Whether this state allows execution.
     * Only ACTIVE skills can be invoked.
     */
    public boolean isExecutable() {
        return this == ACTIVE;
    }

    /**
     * Whether this state is a terminal state.
     */
    public boolean isTerminal() {
        return this == RETIRED
            || this == REVOKED;
    }

    /**
     * Whether this state represents a
     * published (immutable) version.
     */
    public boolean isPublished() {
        return ordinal() >= PUBLISHED.ordinal()
            && !isTerminal();
    }
}
