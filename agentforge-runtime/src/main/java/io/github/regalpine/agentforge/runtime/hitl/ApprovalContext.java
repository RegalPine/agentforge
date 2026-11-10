package io.github.regalpine.agentforge.runtime.hitl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 审批上下文。
 *
 * <p>提供审批者需要的决策信息。
 * 对应 AF-ENHANCE-03 §13。</p>
 */
public record ApprovalContext(
    String executionId,
    String agentId,
    String action,
    Map<String, Object> input,
    String plannedOperation,
    RiskAssessment risk,
    String policyId,
    String tool,
    Map<String, Object> toolArguments,
    String expectedEffect,
    List<String> previousSteps,
    List<String> relevantEvents,
    Map<String, Object> securityContext,
    Map<String, Object> dataClassification
) {

    public ApprovalContext {
        Objects.requireNonNull(
            executionId, "executionId"
        );
        input = input == null
            ? Map.of()
            : Map.copyOf(input);
        toolArguments =
            toolArguments == null
                ? Map.of()
                : Map.copyOf(toolArguments);
        previousSteps =
            previousSteps == null
                ? List.of()
                : List.copyOf(previousSteps);
        relevantEvents =
            relevantEvents == null
                ? List.of()
                : List.copyOf(relevantEvents);
        securityContext =
            securityContext == null
                ? Map.of()
                : Map.copyOf(securityContext);
        dataClassification =
            dataClassification == null
                ? Map.of()
                : Map.copyOf(dataClassification);
    }

    public static ApprovalContext
    of(
        String executionId,
        String agentId,
        String action
    ) {
        return new ApprovalContext(
            executionId, agentId,
            action, Map.of(),
            null, null, null,
            null, Map.of(),
            null, List.of(),
            List.of(), Map.of(),
            Map.of()
        );
    }

    /**
     * 使用脱敏器创建脱敏副本。
     */
    public ApprovalContext redacted(
        ContextRedactor redactor
    ) {
        return new ApprovalContext(
            executionId,
            agentId,
            action,
            redactor.redact(input),
            plannedOperation,
            risk,
            policyId,
            tool,
            redactor.redact(toolArguments),
            expectedEffect,
            previousSteps,
            relevantEvents,
            redactor.redact(securityContext),
            redactor.redact(
                dataClassification
            )
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String executionId;
        private String agentId;
        private String action;
        private Map<String, Object> input =
            Map.of();
        private String plannedOperation;
        private RiskAssessment risk;
        private String policyId;
        private String tool;
        private Map<String, Object>
            toolArguments = Map.of();
        private String expectedEffect;
        private List<String> previousSteps =
            List.of();
        private List<String> relevantEvents =
            List.of();
        private Map<String, Object>
            securityContext = Map.of();
        private Map<String, Object>
            dataClassification = Map.of();

        Builder() {}

        public Builder executionId(
            String v
        ) {
            this.executionId = v;
            return this;
        }

        public Builder agentId(
            String v
        ) {
            this.agentId = v;
            return this;
        }

        public Builder action(
            String v
        ) {
            this.action = v;
            return this;
        }

        public Builder input(
            Map<String, Object> v
        ) {
            this.input = v;
            return this;
        }

        public Builder
        plannedOperation(
            String v
        ) {
            this.plannedOperation = v;
            return this;
        }

        public Builder risk(
            RiskAssessment v
        ) {
            this.risk = v;
            return this;
        }

        public Builder policyId(
            String v
        ) {
            this.policyId = v;
            return this;
        }

        public Builder tool(
            String v
        ) {
            this.tool = v;
            return this;
        }

        public Builder toolArguments(
            Map<String, Object> v
        ) {
            this.toolArguments = v;
            return this;
        }

        public Builder expectedEffect(
            String v
        ) {
            this.expectedEffect = v;
            return this;
        }

        public Builder previousSteps(
            List<String> v
        ) {
            this.previousSteps = v;
            return this;
        }

        public Builder relevantEvents(
            List<String> v
        ) {
            this.relevantEvents = v;
            return this;
        }

        public Builder securityContext(
            Map<String, Object> v
        ) {
            this.securityContext = v;
            return this;
        }

        public Builder
        dataClassification(
            Map<String, Object> v
        ) {
            this.dataClassification = v;
            return this;
        }

        public ApprovalContext build() {
            return new ApprovalContext(
                executionId, agentId,
                action, input,
                plannedOperation, risk,
                policyId, tool,
                toolArguments,
                expectedEffect,
                previousSteps,
                relevantEvents,
                securityContext,
                dataClassification
            );
        }
    }
}
