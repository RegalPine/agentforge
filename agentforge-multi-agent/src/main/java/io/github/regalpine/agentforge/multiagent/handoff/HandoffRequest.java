package io.github.regalpine.agentforge.multiagent.handoff;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HandoffRequest — AF-ENHANCE-08 §15-16.
 *
 * Request for transferring control
 * from one agent to another.
 *
 * <p>Must explicitly pass:
 * Task, Conversation, Relevant Context,
 * Execution Metadata, Artifacts,
 * Constraints.
 *
 * <p>Must NOT pass by default:
 * Credentials, Secrets, Internal Policy,
 * Unrelated Memory.
 */
public record HandoffRequest(

    String task,

    Map<String, Object> contextItems,

    List<String> artifactRefs,

    List<String> conversationHistory,

    HandoffPolicy policy,

    Map<String, Object> constraints

) {

    public HandoffRequest {
        Objects.requireNonNull(
            task, "task"
        );

        contextItems = contextItems == null
            ? Map.of()
            : Map.copyOf(contextItems);

        artifactRefs = artifactRefs == null
            ? List.of()
            : List.copyOf(artifactRefs);

        conversationHistory =
            conversationHistory == null
                ? List.of()
                : List.copyOf(conversationHistory);

        policy = policy == null
            ? HandoffPolicy.WAIT
            : policy;

        constraints = constraints == null
            ? Map.of()
            : Map.copyOf(constraints);
    }

    /**
     * Create a simple handoff request.
     */
    public static HandoffRequest of(
        String task
    ) {
        return new HandoffRequest(
            task, null, null, null,
            null, null
        );
    }

    /**
     * Create with context.
     */
    public static HandoffRequest of(
        String task,
        Map<String, Object> context
    ) {
        return new HandoffRequest(
            task, context, null, null,
            null, null
        );
    }

    /**
     * Builder for handoff request.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * HandoffRequest builder.
     */
    public static final class Builder {
        private String task;
        private Map<String, Object> contextItems;
        private List<String> artifactRefs;
        private List<String> history;
        private HandoffPolicy policy;
        private Map<String, Object> constraints;

        public Builder task(String t) {
            this.task = t;
            return this;
        }

        public Builder context(
            Map<String, Object> ctx
        ) {
            this.contextItems = ctx;
            return this;
        }

        public Builder artifacts(
            List<String> refs
        ) {
            this.artifactRefs = refs;
            return this;
        }

        public Builder history(
            List<String> h
        ) {
            this.history = h;
            return this;
        }

        public Builder policy(
            HandoffPolicy p
        ) {
            this.policy = p;
            return this;
        }

        public Builder constraints(
            Map<String, Object> c
        ) {
            this.constraints = c;
            return this;
        }

        public HandoffRequest build() {
            return new HandoffRequest(
                task, contextItems,
                artifactRefs, history,
                policy, constraints
            );
        }
    }
}
