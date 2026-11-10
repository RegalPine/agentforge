package io.github.regalpine.agentforge.testkit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestKit 测试。
 */
class TestKitTest {

    // --- MockToolProvider ---

    @Test
    void shouldMockToolReturns() {
        var mock = new MockToolProvider();
        mock.returns(
            "web-search",
            "result-1"
        );

        var resp = mock.call(
            "web-search",
            Map.of("query", "test")
        );

        assertFalse(resp.isError());
        assertEquals(
            "result-1",
            resp.result()
        );
        assertTrue(
            mock.wasCalled("web-search")
        );
        assertEquals(
            1,
            mock.callCount("web-search")
        );
    }

    @Test
    void shouldMockToolSequence() {
        var mock = new MockToolProvider();
        mock.sequence(
            "search", "r1", "r2", "r3"
        );

        assertEquals(
            "r1",
            mock.call("search", Map.of())
                .result()
        );
        assertEquals(
            "r2",
            mock.call("search", Map.of())
                .result()
        );
    }

    @Test
    void shouldMockToolError() {
        var mock = new MockToolProvider();
        mock.throwsError(
            "payment",
            "Timeout"
        );

        var resp = mock.call(
            "payment",
            Map.of()
        );

        assertTrue(resp.isError());
        assertEquals(
            "Timeout",
            resp.error()
        );
    }

    // --- MockModelProvider ---

    @Test
    void shouldMockModelReturns() {
        var mock = new MockModelProvider();
        mock.returns("Hello world");

        var resp = mock.call("test prompt");
        assertEquals(
            "Hello world",
            resp.content()
        );
        assertEquals(1,
            mock.callCount()
        );
    }

    @Test
    void shouldMockModelSequence() {
        var mock = new MockModelProvider();
        mock.sequence(
            "resp1", "resp2"
        );

        assertEquals(
            "resp1",
            mock.call("p1").content()
        );
        assertEquals(
            "resp2",
            mock.call("p2").content()
        );
    }

    // --- MockHumanProvider ---

    @Test
    void shouldMockHumanApprove() {
        var mock = new MockHumanProvider();
        mock.approve();

        assertEquals(
            MockHumanProvider
                .HumanDecision.APPROVE,
            mock.decide("any-policy")
        );
    }

    @Test
    void shouldMockHumanForPolicy() {
        var mock = new MockHumanProvider();
        mock.forPolicy(
            "prod-write",
            MockHumanProvider
                .HumanDecision.REJECT
        );

        assertEquals(
            MockHumanProvider
                .HumanDecision.REJECT,
            mock.decide("prod-write")
        );
        assertEquals(
            MockHumanProvider
                .HumanDecision.APPROVE,
            mock.decide("other")
        );
    }

    // --- MockMemoryProvider ---

    @Test
    void shouldMockMemory() {
        var mock = new MockMemoryProvider();
        mock.put(
            "key1",
            Map.of("data", "value1")
        );

        assertEquals(
            "value1",
            mock.get("key1").get("data")
        );
        assertTrue(
            mock.get("unknown").isEmpty()
        );
    }

    // --- ExecutionAssertion ---

    @Test
    void shouldAssertCompleted() {
        var result =
            ExecutionAssertion
                .ExecutionResult
                .builder()
                .completed(true)
                .build();

        ExecutionAssertion
            .assertThat(result)
            .completed()
            .verify();
    }

    @Test
    void shouldAssertToolCall() {
        var result =
            ExecutionAssertion
                .ExecutionResult
                .builder()
                .completed(true)
                .toolCalls(
                    List.of(
                        "web-search",
                        "calculator"
                    )
                )
                .build();

        ExecutionAssertion
            .assertThat(result)
            .containsToolCall("web-search")
            .neverCallsTool("delete-data")
            .verify();
    }

    @Test
    void shouldAssertEvent() {
        var result =
            ExecutionAssertion
                .ExecutionResult
                .builder()
                .events(
                    List.of(
                        "agent.started",
                        "tool.completed"
                    )
                )
                .build();

        ExecutionAssertion
            .assertThat(result)
            .containsEvent("agent.started")
            .verify();
    }

    @Test
    void shouldAssertOutput() {
        var result =
            ExecutionAssertion
                .ExecutionResult
                .builder()
                .output(
                    Map.of(
                        "result", "ok"
                    )
                )
                .build();

        ExecutionAssertion
            .assertThat(result)
            .outputContains(
                "result", "ok"
            )
            .verify();
    }

    @Test
    void shouldFailOnMismatch() {
        var result =
            ExecutionAssertion
                .ExecutionResult
                .builder()
                .completed(false)
                .build();

        var assertion =
            ExecutionAssertion
                .assertThat(result)
                .completed();

        assertTrue(
            assertion.hasFailures()
        );
        assertThrows(
            AssertionError.class,
            assertion::verify
        );
    }

    // --- AgentTestCase ---

    @Test
    void shouldBuildTestCase() {
        var test =
            AgentTestCase.builder()
                .id("test-1")
                .agent("researcher")
                .input("Find X")
                .expectToolCall(
                    "web-search"
                )
                .expectEvent(
                    "agent.completed"
                )
                .build();

        assertEquals(
            "test-1", test.id()
        );
        assertEquals(
            "researcher",
            test.agentId()
        );
        assertEquals(
            1,
            test.expectedToolCalls().size()
        );
    }
}
