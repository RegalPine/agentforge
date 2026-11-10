package io.github.regalpine.agentforge.memory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Context snapshot.
 * AF-ENHANCE-06 §43: Context Snapshot.
 *
 * <p>Captures the state of an
 * assembled context for trace,
 * audit, debug and replay.
 *
 * <p>Snapshot records which memory
 * IDs, knowledge IDs, workspace
 * references were included, along
 * with token count and policy
 * decision.
 */
public record ContextSnapshot(
    String snapshotId,
    String contextId,
    String executionId,
    String modelId,
    List<String> memoryIds,
    List<String> knowledgeIds,
    List<String> workspaceRefs,
    int tokenCount,
    String policyDecision,
    String assemblyStrategy,
    Instant timestamp
) {

    public ContextSnapshot {
        Objects.requireNonNull(
            snapshotId, "snapshotId"
        );

        contextId =
            contextId == null ? "" : contextId;
        executionId =
            executionId == null ? "" : executionId;
        modelId =
            modelId == null ? "" : modelId;

        memoryIds =
            memoryIds == null
                ? List.of()
                : List.copyOf(memoryIds);
        knowledgeIds =
            knowledgeIds == null
                ? List.of()
                : List.copyOf(knowledgeIds);
        workspaceRefs =
            workspaceRefs == null
                ? List.of()
                : List.copyOf(workspaceRefs);

        policyDecision =
            policyDecision == null
                ? "ALLOW" : policyDecision;
        assemblyStrategy =
            assemblyStrategy == null
                ? "DEFAULT" : assemblyStrategy;

        timestamp =
            timestamp == null
                ? Instant.now()
                : timestamp;
    }

    /**
     * Create a snapshot from a
     * model context.
     */
    public static ContextSnapshot from(
        String snapshotId,
        ModelContext context
    ) {
        Objects.requireNonNull(
            context, "context"
        );

        List<String> memIds =
            context.segmentsOfType(
                ContextSegmentType.MEMORY
            ).stream()
            .map(ContextSegment::source)
            .toList();

        return new ContextSnapshot(
            snapshotId,
            "", "", "",
            memIds,
            List.of(), List.of(),
            context.totalTokens(),
            "ALLOW", "DEFAULT",
            Instant.now()
        );
    }

    /**
     * Create a builder for snapshot
     * construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String snapshotId;
        private String contextId;
        private String executionId;
        private String modelId;
        private List<String> memoryIds;
        private List<String> knowledgeIds;
        private List<String> workspaceRefs;
        private int tokenCount;
        private String policyDecision;
        private String assemblyStrategy;
        private Instant timestamp;

        private Builder() {}

        public Builder snapshotId(String v) {
            this.snapshotId = v;
            return this;
        }

        public Builder contextId(String v) {
            this.contextId = v;
            return this;
        }

        public Builder executionId(String v) {
            this.executionId = v;
            return this;
        }

        public Builder modelId(String v) {
            this.modelId = v;
            return this;
        }

        public Builder memoryIds(List<String> v) {
            this.memoryIds = v;
            return this;
        }

        public Builder knowledgeIds(List<String> v) {
            this.knowledgeIds = v;
            return this;
        }

        public Builder workspaceRefs(List<String> v) {
            this.workspaceRefs = v;
            return this;
        }

        public Builder tokenCount(int v) {
            this.tokenCount = v;
            return this;
        }

        public Builder policyDecision(String v) {
            this.policyDecision = v;
            return this;
        }

        public Builder assemblyStrategy(String v) {
            this.assemblyStrategy = v;
            return this;
        }

        public Builder timestamp(Instant v) {
            this.timestamp = v;
            return this;
        }

        public ContextSnapshot build() {
            return new ContextSnapshot(
                snapshotId, contextId,
                executionId, modelId,
                memoryIds, knowledgeIds,
                workspaceRefs, tokenCount,
                policyDecision, assemblyStrategy,
                timestamp
            );
        }
    }
}
