package io.github.regalpine.agentforge.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 执行断言。
 *
 * <p>流式 API 验证 Execution。
 * 对应 AF-ENHANCE-04 §44-55。</p>
 */
public final class ExecutionAssertion {

    private final ExecutionResult result;
    private final List<AssertionFailure>
        failures = new ArrayList<>();

    private ExecutionAssertion(
        ExecutionResult result
    ) {
        this.result = Objects.requireNonNull(
            result, "result"
        );
    }

    public static ExecutionAssertion
    assertThat(
        ExecutionResult result
    ) {
        return new ExecutionAssertion(
            result
        );
    }

    /**
     * 断言执行完成。
     */
    public ExecutionAssertion
    completed() {
        if (!result.completed()) {
            failures.add(
                new AssertionFailure(
                    "Expected completed",
                    "status="
                        + result.status()
                )
            );
        }
        return this;
    }

    /**
     * 断言包含工具调用。
     */
    public ExecutionAssertion
    containsToolCall(
        String toolName
    ) {
        boolean found =
            result.toolCalls()
                .contains(toolName);
        if (!found) {
            failures.add(
                new AssertionFailure(
                    "Expected tool call: "
                        + toolName,
                    "tools="
                        + result.toolCalls()
                )
            );
        }
        return this;
    }

    /**
     * 断言不包含工具调用。
     */
    public ExecutionAssertion
    neverCallsTool(
        String toolName
    ) {
        if (result.toolCalls()
            .contains(toolName)
        ) {
            failures.add(
                new AssertionFailure(
                    "Tool should not be called: "
                        + toolName,
                    "tools="
                        + result.toolCalls()
                )
            );
        }
        return this;
    }

    /**
     * 断言包含事件。
     */
    public ExecutionAssertion
    containsEvent(
        String eventType
    ) {
        boolean found =
            result.events()
                .contains(eventType);
        if (!found) {
            failures.add(
                new AssertionFailure(
                    "Expected event: "
                        + eventType,
                    "events="
                        + result.events()
                )
            );
        }
        return this;
    }

    /**
     * 断言输出包含。
     */
    public ExecutionAssertion
    outputContains(
        String key,
        Object expected
    ) {
        Object actual =
            result.output().get(key);
        if (!Objects.equals(
            expected, actual
        )) {
            failures.add(
                new AssertionFailure(
                    "Expected " + key
                        + "=" + expected,
                    "actual=" + actual
                )
            );
        }
        return this;
    }

    /**
     * 断言状态包含。
     */
    public ExecutionAssertion
    stateEquals(
        String key,
        Object expected
    ) {
        Object actual =
            result.state().get(key);
        if (!Objects.equals(
            expected, actual
        )) {
            failures.add(
                new AssertionFailure(
                    "Expected state "
                        + key + "="
                        + expected,
                    "actual=" + actual
                )
            );
        }
        return this;
    }

    /**
     * 验证所有断言。
     */
    public void verify() {
        if (!failures.isEmpty()) {
            StringBuilder sb =
                new StringBuilder(
                    "Assertion failures:\n"
                );
            for (var f : failures) {
                sb.append("  - ")
                    .append(f.message())
                    .append(" (")
                    .append(f.detail())
                    .append(")\n");
            }
            throw new AssertionError(
                sb.toString()
            );
        }
    }

    /**
     * 获取失败列表。
     */
    public List<AssertionFailure>
    failures() {
        return List.copyOf(failures);
    }

    /**
     * 是否有失败。
     */
    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    // --- Types ---

    public record AssertionFailure(
        String message,
        String detail
    ) {}

    /**
     * 执行结果（测试用）。
     */
    public record ExecutionResult(
        boolean completed,
        String status,
        List<String> toolCalls,
        List<String> events,
        Map<String, Object> output,
        Map<String, Object> state
    ) {

        public ExecutionResult {
            toolCalls = toolCalls == null
                ? List.of()
                : List.copyOf(toolCalls);
            events = events == null
                ? List.of()
                : List.copyOf(events);
            output = output == null
                ? Map.of()
                : Map.copyOf(output);
            state = state == null
                ? Map.of()
                : Map.copyOf(state);
        }

        public static Builder
        builder() {
            return new Builder();
        }

        public static final class
            Builder {

            private boolean completed;
            private String status =
                "COMPLETED";
            private List<String>
                toolCalls = List.of();
            private List<String>
                events = List.of();
            private Map<String, Object>
                output = Map.of();
            private Map<String, Object>
                state = Map.of();

            Builder() {}

            public Builder completed(
                boolean v
            ) {
                this.completed = v;
                return this;
            }

            public Builder status(
                String v
            ) {
                this.status = v;
                return this;
            }

            public Builder toolCalls(
                List<String> v
            ) {
                this.toolCalls = v;
                return this;
            }

            public Builder events(
                List<String> v
            ) {
                this.events = v;
                return this;
            }

            public Builder output(
                Map<String, Object> v
            ) {
                this.output = v;
                return this;
            }

            public Builder state(
                Map<String, Object> v
            ) {
                this.state = v;
                return this;
            }

            public ExecutionResult
            build() {
                return new ExecutionResult(
                    completed, status,
                    toolCalls, events,
                    output, state
                );
            }
        }
    }
}
