package io.github.regalpine.agentforge.multiagent.result;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AgentResult — AE-11 §26, §28-29.
 *
 * Structured result from a child agent execution.
 * Contains status, output, artifacts, references,
 * provenance metadata, and completeness flag.
 *
 * <p>Partial results must have complete=false and
 * must NOT be treated as final results.
 */
public record AgentResult<T>(

    ResultStatus status,

    T output,

    boolean complete,

    List<Artifact> artifacts,

    List<Reference> references,

    ResultProvenance provenance,

    Map<String, Object> metadata,

    String errorCode,

    String errorMessage
) {

    public AgentResult {
        Objects.requireNonNull(status, "status");

        artifacts = artifacts == null
            ? List.of() : List.copyOf(artifacts);

        references = references == null
            ? List.of() : List.copyOf(references);

        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a successful complete result.
     */
    public static <T> AgentResult<T> success(
        T output,
        ResultProvenance provenance
    ) {
        return new AgentResult<>(
            ResultStatus.SUCCESS,
            output, true,
            null, null, provenance,
            null, null, null
        );
    }

    /**
     * Create a partial result.
     */
    public static <T> AgentResult<T> partial(
        T output,
        ResultProvenance provenance
    ) {
        return new AgentResult<>(
            ResultStatus.PARTIAL,
            output, false,
            null, null, provenance,
            null, null, null
        );
    }

    /**
     * Create a failed result.
     */
    public static <T> AgentResult<T> failure(
        String errorCode,
        String errorMessage,
        ResultProvenance provenance
    ) {
        return new AgentResult<>(
            ResultStatus.FAILED,
            null, false,
            null, null, provenance,
            null, errorCode, errorMessage
        );
    }

    /**
     * Create a policy-denied result.
     */
    public static <T> AgentResult<T> policyDenied(
        String reason,
        ResultProvenance provenance
    ) {
        return new AgentResult<>(
            ResultStatus.POLICY_DENIED,
            null, false,
            null, null, provenance,
            null, "POLICY_DENIED", reason
        );
    }

    /**
     * Create a budget-exceeded result.
     */
    public static <T> AgentResult<T> budgetExceeded(
        String reason,
        ResultProvenance provenance
    ) {
        return new AgentResult<>(
            ResultStatus.BUDGET_EXCEEDED,
            null, false,
            null, null, provenance,
            null, "BUDGET_EXCEEDED", reason
        );
    }

    /**
     * Result artifact.
     */
    public record Artifact(
        String type,
        String name,
        Object data
    ) {
        public Artifact {
            Objects.requireNonNull(type, "type");
        }
    }

    /**
     * Result reference to external resources.
     */
    public record Reference(
        String refType,
        String refId,
        Map<String, Object> metadata
    ) {
        public Reference {
            Objects.requireNonNull(refType, "refType");
            Objects.requireNonNull(refId, "refId");
            metadata = metadata == null
                ? Map.of() : Map.copyOf(metadata);
        }
    }

    /**
     * Result provenance — AE-11 §29.
     */
    public record ResultProvenance(
        String rootExecutionId,
        String parentExecutionId,
        String childExecutionId,
        String agentId,
        String agentVersion,
        Instant timestamp
    ) {
        public ResultProvenance {
            Objects.requireNonNull(
                childExecutionId, "childExecutionId");
            timestamp = timestamp == null
                ? Instant.now() : timestamp;
        }

        public static ResultProvenance of(
            String childExecutionId,
            String agentId
        ) {
            return new ResultProvenance(
                null, null,
                childExecutionId,
                agentId, null, null
            );
        }
    }
}
