package io.github.regalpine.agentforge.observability.inspection;

import java.time.Instant;
import java.util.*;

/**
 * Execution inspector.
 * AF-ENHANCE-06 §12: aggregates
 * diagnostic information for a single
 * execution from input to result.
 */
public record ExecutionInspection(
    String executionId,
    String agentId,
    String graphId,
    String tenantId,
    Instant startTime,
    Instant endTime,
    ExecutionInspectionStatus status,
    Map<String, String> input,
    Map<String, String> output,
    List<NodeInspection> nodeInspections,
    List<String> errors,
    Map<String, String> metadata
) {

    public ExecutionInspection {
        Objects.requireNonNull(executionId);
        startTime = startTime == null
            ? Instant.now() : startTime;
        input = input == null
            ? Map.of() : Map.copyOf(input);
        output = output == null
            ? Map.of() : Map.copyOf(output);
        nodeInspections = nodeInspections == null
            ? List.of()
            : List.copyOf(nodeInspections);
        errors = errors == null
            ? List.of() : List.copyOf(errors);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
        status = status == null
            ? ExecutionInspectionStatus.UNKNOWN
            : status;
    }

    /**
     * Duration in milliseconds.
     */
    public long durationMs() {
        if (endTime == null
            || startTime == null
        ) {
            return 0;
        }
        return endTime.toEpochMilli()
            - startTime.toEpochMilli();
    }

    /**
     * Check if execution succeeded.
     */
    public boolean isSuccess() {
        return status
            == ExecutionInspectionStatus.COMPLETED
            && errors.isEmpty();
    }

    /**
     * Inspection status.
     */
    public enum ExecutionInspectionStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED,
        UNKNOWN
    }

    /**
     * Node-level inspection data.
     */
    public record NodeInspection(
        String nodeId,
        String capabilityId,
        Instant startTime,
        Instant endTime,
        String status,
        Map<String, String> attributes
    ) {

        public NodeInspection {
            Objects.requireNonNull(nodeId);
            attributes = attributes == null
                ? Map.of() : Map.copyOf(attributes);
        }
    }

    /**
     * Builder for creating inspections.
     */
    public static Builder builder(
        String executionId
    ) {
        return new Builder(executionId);
    }

    /**
     * Inspection builder.
     */
    public static final class Builder {

        private final String executionId;
        private String agentId;
        private String graphId;
        private String tenantId;
        private Instant startTime;
        private Instant endTime;
        private ExecutionInspectionStatus status;
        private Map<String, String> input;
        private Map<String, String> output;
        private final List<NodeInspection> nodes =
            new ArrayList<>();
        private final List<String> errors =
            new ArrayList<>();
        private Map<String, String> metadata;

        Builder(String executionId) {
            this.executionId = executionId;
        }

        public Builder agentId(String id) {
            this.agentId = id;
            return this;
        }

        public Builder graphId(String id) {
            this.graphId = id;
            return this;
        }

        public Builder tenantId(String id) {
            this.tenantId = id;
            return this;
        }

        public Builder startTime(Instant t) {
            this.startTime = t;
            return this;
        }

        public Builder endTime(Instant t) {
            this.endTime = t;
            return this;
        }

        public Builder status(
            ExecutionInspectionStatus s
        ) {
            this.status = s;
            return this;
        }

        public Builder input(
            Map<String, String> input
        ) {
            this.input = input;
            return this;
        }

        public Builder output(
            Map<String, String> output
        ) {
            this.output = output;
            return this;
        }

        public Builder addNode(
            NodeInspection node
        ) {
            nodes.add(node);
            return this;
        }

        public Builder addError(String error) {
            errors.add(error);
            return this;
        }

        public Builder metadata(
            Map<String, String> meta
        ) {
            this.metadata = meta;
            return this;
        }

        public ExecutionInspection build() {
            return new ExecutionInspection(
                executionId, agentId, graphId,
                tenantId, startTime, endTime,
                status, input, output,
                List.copyOf(nodes),
                List.copyOf(errors),
                metadata
            );
        }
    }
}
