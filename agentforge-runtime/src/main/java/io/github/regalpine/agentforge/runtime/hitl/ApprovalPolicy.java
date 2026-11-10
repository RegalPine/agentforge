package io.github.regalpine.agentforge.runtime.hitl;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 审批策略。
 *
 * <p>定义在什么条件下必须请求人工决策。
 * 对应 AF-ENHANCE-03 §8-9。</p>
 */
public record ApprovalPolicy(
    String id,
    String name,
    String description,
    String trigger,
    RiskLevel minRiskLevel,
    ApprovalRequirement requirement,
    Duration timeout,
    TimeoutAction timeoutAction,
    Set<String> modifiableFields,
    boolean enabled,
    int priority,
    int version
) {

    public ApprovalPolicy {
        Objects.requireNonNull(
            id, "id"
        );
        Objects.requireNonNull(
            trigger, "trigger"
        );
        if (modifiableFields != null) {
            modifiableFields =
                Set.copyOf(modifiableFields);
        } else {
            modifiableFields = Set.of();
        }
    }

    public static ApprovalPolicyBuilder
    builder() {
        return new ApprovalPolicyBuilder();
    }

    public static final class
        ApprovalPolicyBuilder {

        private String id;
        private String name;
        private String description;
        private String trigger;
        private RiskLevel minRiskLevel =
            RiskLevel.LOW;
        private ApprovalRequirement requirement;
        private Duration timeout =
            Duration.ofMinutes(30);
        private TimeoutAction timeoutAction =
            TimeoutAction.REJECT;
        private Set<String> modifiableFields =
            Set.of();
        private boolean enabled = true;
        private int priority = 0;
        private int version = 1;

        ApprovalPolicyBuilder() {}

        public ApprovalPolicyBuilder id(
            String id
        ) {
            this.id = id;
            return this;
        }

        public ApprovalPolicyBuilder name(
            String name
        ) {
            this.name = name;
            return this;
        }

        public ApprovalPolicyBuilder
        description(
            String description
        ) {
            this.description = description;
            return this;
        }

        public ApprovalPolicyBuilder
        trigger(
            String trigger
        ) {
            this.trigger = trigger;
            return this;
        }

        public ApprovalPolicyBuilder
        minRiskLevel(
            RiskLevel level
        ) {
            this.minRiskLevel = level;
            return this;
        }

        public ApprovalPolicyBuilder
        requirement(
            ApprovalRequirement req
        ) {
            this.requirement = req;
            return this;
        }

        public ApprovalPolicyBuilder
        timeout(
            Duration timeout
        ) {
            this.timeout = timeout;
            return this;
        }

        public ApprovalPolicyBuilder
        timeoutAction(
            TimeoutAction action
        ) {
            this.timeoutAction = action;
            return this;
        }

        public ApprovalPolicyBuilder
        modifiableFields(
            Set<String> fields
        ) {
            this.modifiableFields = fields;
            return this;
        }

        public ApprovalPolicyBuilder
        enabled(
            boolean enabled
        ) {
            this.enabled = enabled;
            return this;
        }

        public ApprovalPolicyBuilder
        priority(
            int priority
        ) {
            this.priority = priority;
            return this;
        }

        public ApprovalPolicyBuilder
        version(
            int version
        ) {
            this.version = version;
            return this;
        }

        public ApprovalPolicy build() {
            return new ApprovalPolicy(
                id, name, description,
                trigger, minRiskLevel,
                requirement, timeout,
                timeoutAction,
                modifiableFields,
                enabled, priority, version
            );
        }
    }
}
