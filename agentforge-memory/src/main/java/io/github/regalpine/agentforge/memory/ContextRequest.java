package io.github.regalpine.agentforge.memory;

import java.util.List;
import java.util.Objects;

/**
 * Context build request.
 * AE-08 §41-42: Context Pipeline input.
 */
public record ContextRequest(
    String subjectId,
    String instructions,
    String stateText,
    List<String> observations,
    MemoryQuery memoryQuery
) {

    public ContextRequest {
        Objects.requireNonNull(
            subjectId, "subjectId"
        );

        observations =
            observations == null
                ? List.of()
                : List.copyOf(observations);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String subjectId;
        private String instructions;
        private String stateText;
        private List<String> observations;
        private MemoryQuery memoryQuery;

        private Builder() {}

        public Builder subjectId(String v) {
            this.subjectId = v; return this;
        }

        public Builder instructions(String v) {
            this.instructions = v; return this;
        }

        public Builder stateText(String v) {
            this.stateText = v; return this;
        }

        public Builder observations(List<String> v) {
            this.observations = v; return this;
        }

        public Builder memoryQuery(MemoryQuery v) {
            this.memoryQuery = v; return this;
        }

        public ContextRequest build() {
            return new ContextRequest(
                subjectId, instructions,
                stateText, observations,
                memoryQuery
            );
        }
    }
}
