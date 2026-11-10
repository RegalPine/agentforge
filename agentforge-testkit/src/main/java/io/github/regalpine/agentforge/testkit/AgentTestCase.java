package io.github.regalpine.agentforge.testkit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Agent 测试用例。
 *
 * <p>对应 AF-ENHANCE-04 §42-43。</p>
 */
public final class AgentTestCase {

    private final String id;
    private final String agentId;
    private final String input;
    private final Map<String, Object>
        environment;
    private final List<String>
        expectedToolCalls;
    private final List<String>
        expectedEvents;
    private final Map<String, Object>
        expectedOutput;

    private AgentTestCase(Builder b) {
        this.id = b.id;
        this.agentId = b.agentId;
        this.input = b.input;
        this.environment = b.environment;
        this.expectedToolCalls =
            b.expectedToolCalls;
        this.expectedEvents =
            b.expectedEvents;
        this.expectedOutput =
            b.expectedOutput;
    }

    public String id() { return id; }
    public String agentId() {
        return agentId;
    }
    public String input() {
        return input;
    }
    public Map<String, Object>
    environment() {
        return environment;
    }
    public List<String>
    expectedToolCalls() {
        return expectedToolCalls;
    }
    public List<String>
    expectedEvents() {
        return expectedEvents;
    }
    public Map<String, Object>
    expectedOutput() {
        return expectedOutput;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private String agentId;
        private String input;
        private Map<String, Object>
            environment = Map.of();
        private List<String>
            expectedToolCalls =
            new ArrayList<>();
        private List<String>
            expectedEvents =
            new ArrayList<>();
        private Map<String, Object>
            expectedOutput = Map.of();

        Builder() {}

        public Builder id(String v) {
            this.id = v;
            return this;
        }

        public Builder agent(
            String v
        ) {
            this.agentId = v;
            return this;
        }

        public Builder input(
            String v
        ) {
            this.input = v;
            return this;
        }

        public Builder environment(
            Map<String, Object> v
        ) {
            this.environment = v;
            return this;
        }

        public Builder
        expectToolCall(
            String toolName
        ) {
            this.expectedToolCalls
                .add(toolName);
            return this;
        }

        public Builder
        expectEvent(
            String eventType
        ) {
            this.expectedEvents
                .add(eventType);
            return this;
        }

        public Builder
        expectOutput(
            String key,
            Object value
        ) {
            this.expectedOutput =
                Map.of(key, value);
            return this;
        }

        public AgentTestCase build() {
            Objects.requireNonNull(
                agentId, "agentId"
            );
            if (id == null) {
                id = "test-"
                    + UUID.randomUUID();
            }
            return new AgentTestCase(this);
        }
    }

    // --- Test Result ---

    public record TestResult(
        String testId,
        TestStatus status,
        Duration duration,
        List<ExecutionAssertion
            .AssertionFailure>
            failures,
        String executionId,
        String traceId
    ) {

        public TestResult {
            failures = failures == null
                ? List.of()
                : List.copyOf(failures);
        }
    }

    public enum TestStatus {
        PASSED,
        FAILED,
        ERROR,
        SKIPPED,
        TIMEOUT
    }
}
