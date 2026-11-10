package io.github.regalpine.agentforge.testkit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 模拟 Tool Provider。
 *
 * <p>支持静态/顺序/错误/超时模拟。
 * 对应 AF-ENHANCE-04 §56-58。</p>
 */
public final class MockToolProvider {

    private final Map<String,
        Queue<ToolResponse>> responses =
        new ConcurrentHashMap<>();

    private final Map<String,
        List<ToolCallRecord>> callHistory =
        new ConcurrentHashMap<>();

    /**
     * 设置静态响应。
     */
    public MockToolProvider returns(
        String toolName,
        Object result
    ) {
        Objects.requireNonNull(
            toolName, "toolName"
        );
        responses.computeIfAbsent(
            toolName,
            k -> new LinkedBlockingQueue<>()
        ).add(
            ToolResponse.success(result)
        );
        return this;
    }

    /**
     * 设置顺序响应。
     */
    public MockToolProvider sequence(
        String toolName,
        Object... results
    ) {
        Objects.requireNonNull(
            toolName, "toolName"
        );
        var queue =
            responses.computeIfAbsent(
                toolName,
                k -> new LinkedBlockingQueue<>()
            );
        for (Object r : results) {
            queue.add(
                ToolResponse.success(r)
            );
        }
        return this;
    }

    /**
     * 设置错误响应。
     */
    public MockToolProvider throwsError(
        String toolName,
        String errorMessage
    ) {
        Objects.requireNonNull(
            toolName, "toolName"
        );
        responses.computeIfAbsent(
            toolName,
            k -> new LinkedBlockingQueue<>()
        ).add(
            ToolResponse.error(
                errorMessage
            )
        );
        return this;
    }

    /**
     * 调用工具。
     */
    public ToolResponse call(
        String toolName,
        Map<String, Object> input
    ) {
        callHistory.computeIfAbsent(
            toolName,
            k -> new ArrayList<>()
        ).add(
            new ToolCallRecord(
                toolName, input
            )
        );

        Queue<ToolResponse> queue =
            responses.get(toolName);

        if (queue == null
            || queue.isEmpty()
        ) {
            return ToolResponse.success(
                Map.of(
                    "mocked", true,
                    "tool", toolName
                )
            );
        }

        ToolResponse resp = queue.poll();
        if (queue.isEmpty()) {
            // 如果是静态响应，重新添加
            if (resp != null
                && !resp.isError()
            ) {
                queue.add(resp);
            }
        }
        return resp != null
            ? resp
            : ToolResponse.success(
                Map.of("mocked", true)
              );
    }

    /**
     * 获取调用历史。
     */
    public List<ToolCallRecord>
    getCallHistory(String toolName) {
        return callHistory.getOrDefault(
            toolName, List.of()
        );
    }

    /**
     * 工具是否被调用。
     */
    public boolean wasCalled(
        String toolName
    ) {
        return callHistory
            .containsKey(toolName)
            && !callHistory
                .get(toolName)
                .isEmpty();
    }

    /**
     * 调用次数。
     */
    public int callCount(
        String toolName
    ) {
        return callHistory
            .getOrDefault(
                toolName, List.of()
            )
            .size();
    }

    /**
     * 重置。
     */
    public void reset() {
        responses.clear();
        callHistory.clear();
    }

    // --- Types ---

    public record ToolResponse(
        Object result,
        String error,
        boolean isError
    ) {

        public static ToolResponse
        success(Object result) {
            return new ToolResponse(
                result, null, false
            );
        }

        public static ToolResponse
        error(String error) {
            return new ToolResponse(
                null, error, true
            );
        }
    }

    public record ToolCallRecord(
        String toolName,
        Map<String, Object> input
    ) {
        public ToolCallRecord {
            input = input == null
                ? Map.of()
                : Map.copyOf(input);
        }
    }
}
