package io.github.regalpine.agentforge.debugger;

import java.util.Map;
import java.util.Objects;

/**
 * 执行快照。
 *
 * <p>只读状态检查。
 * 对应 AF-ENHANCE-04 §26。</p>
 */
public record ExecutionSnapshot(
    String executionId,
    long sequence,
    String stateVersion,
    Map<String, Object> agentState,
    Map<String, Object> executionState,
    Map<String, Object> context,
    Map<String, Object> metadata
) {

    public ExecutionSnapshot {
        Objects.requireNonNull(
            executionId, "executionId"
        );
        agentState = agentState == null
            ? Map.of()
            : Map.copyOf(agentState);
        executionState =
            executionState == null
                ? Map.of()
                : Map.copyOf(executionState);
        context = context == null
            ? Map.of()
            : Map.copyOf(context);
        metadata = metadata == null
            ? Map.of()
            : Map.copyOf(metadata);
    }

    public static ExecutionSnapshot of(
        String executionId,
        long sequence,
        Map<String, Object> state
    ) {
        return new ExecutionSnapshot(
            executionId, sequence,
            "1", state, Map.of(),
            Map.of(), Map.of()
        );
    }
}
