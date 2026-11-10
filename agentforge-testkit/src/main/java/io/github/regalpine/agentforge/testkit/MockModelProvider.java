package io.github.regalpine.agentforge.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 模拟 Model Provider。
 *
 * <p>支持静态/顺序响应。
 * 对应 AF-ENHANCE-04 §59-60。</p>
 */
public final class MockModelProvider {

    private final Queue<ModelResponse>
        responses =
        new LinkedBlockingQueue<>();

    private final List<ModelCallRecord>
        callHistory = new ArrayList<>();

    private ModelResponse defaultResponse;

    /**
     * 设置默认响应。
     */
    public MockModelProvider returns(
        String content
    ) {
        this.defaultResponse =
            ModelResponse.of(content);
        return this;
    }

    /**
     * 设置顺序响应。
     */
    public MockModelProvider sequence(
        String... contents
    ) {
        for (String c : contents) {
            responses.add(
                ModelResponse.of(c)
            );
        }
        return this;
    }

    /**
     * 调用模型。
     */
    public ModelResponse call(
        String prompt
    ) {
        callHistory.add(
            new ModelCallRecord(prompt)
        );

        ModelResponse resp =
            responses.poll();
        if (resp != null) {
            return resp;
        }
        return defaultResponse != null
            ? defaultResponse
            : ModelResponse.of(
                "Mock model response"
            );
    }

    /**
     * 获取调用历史。
     */
    public List<ModelCallRecord>
    getCallHistory() {
        return List.copyOf(callHistory);
    }

    /**
     * 调用次数。
     */
    public int callCount() {
        return callHistory.size();
    }

    /**
     * 重置。
     */
    public void reset() {
        responses.clear();
        callHistory.clear();
        defaultResponse = null;
    }

    // --- Types ---

    public record ModelResponse(
        String content,
        String modelId
    ) {

        public ModelResponse {
            Objects.requireNonNull(
                content, "content"
            );
        }

        public static ModelResponse of(
            String content
        ) {
            return new ModelResponse(
                content, "mock-model"
            );
        }
    }

    public record ModelCallRecord(
        String prompt
    ) {}
}
