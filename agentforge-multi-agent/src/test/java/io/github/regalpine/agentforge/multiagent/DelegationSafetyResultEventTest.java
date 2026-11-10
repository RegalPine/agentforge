package io.github.regalpine.agentforge.multiagent;


import io.github.regalpine.agentforge.multiagent.safety.CircularDelegationDetector;
import io.github.regalpine.agentforge.multiagent.safety.DelegationDepthChecker;
import io.github.regalpine.agentforge.multiagent.safety.DelegationSafetyResult;
import io.github.regalpine.agentforge.multiagent.result.AgentResult;
import io.github.regalpine.agentforge.multiagent.result.ResultStatus;
import io.github.regalpine.agentforge.multiagent.result.ResultValidator;
import io.github.regalpine.agentforge.multiagent.event.DelegationEvent;
import io.github.regalpine.agentforge.multiagent.event.DelegationEventSink;
import io.github.regalpine.agentforge.multiagent.event.DelegationEventType;
import io.github.regalpine.agentforge.multiagent.contract.AgentContract;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Safety, Result, Event tests —
 * AE-11 §23-29, §38.
 * AF-MA-005, AF-MA-006, AF-MA-009, AF-MA-010.
 */
class DelegationSafetyResultEventTest {

    // --- Circular Detection ---

    @Test
    void shouldDetectCircularDelegation() {
        var detector = new CircularDelegationDetector();
        var stack = List.of("A", "B", "C");

        var result = detector.check(stack, "A");
        assertFalse(result.safe());
        assertTrue(result.reason().contains("Circular"));
        assertTrue(result.reason().contains("A"));
    }

    @Test
    void shouldAllowNonCircularDelegation() {
        var detector = new CircularDelegationDetector();
        var stack = List.of("A", "B", "C");

        var result = detector.check(stack, "D");
        assertTrue(result.safe());
    }

    @Test
    void shouldHandleEmptyStack() {
        var detector = new CircularDelegationDetector();

        var result = detector.check(List.of(), "A");
        assertTrue(result.safe());
    }

    @Test
    void shouldHandleNullStack() {
        var detector = new CircularDelegationDetector();

        var result = detector.check(null, "A");
        assertTrue(result.safe());
    }

    @Test
    void shouldDetectSelfDelegation() {
        var detector = new CircularDelegationDetector();
        var stack = List.of("A");

        var result = detector.check(stack, "A");
        assertFalse(result.safe());
    }

    // --- Depth Check ---

    @Test
    void shouldAllowWithinDepth() {
        var checker = new DelegationDepthChecker(5);
        var result = checker.check(3);
        assertTrue(result.safe());
        assertEquals(5, result.maxDepth());
    }

    @Test
    void shouldRejectAtMaxDepth() {
        var checker = new DelegationDepthChecker(5);
        var result = checker.check(5);
        assertFalse(result.safe());
        assertTrue(result.reason().contains("depth"));
    }

    @Test
    void shouldRejectBeyondMaxDepth() {
        var checker = new DelegationDepthChecker(3);
        var result = checker.check(10);
        assertFalse(result.safe());
    }

    @Test
    void shouldRejectNegativeMaxDepth() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new DelegationDepthChecker(-1)
        );
    }

    // --- Result ---

    @Test
    void shouldCreateSuccessResult() {
        var prov = AgentResult.ResultProvenance.of(
            "exec-1", "agent-a"
        );
        var result = AgentResult.success(
            java.util.Map.of("answer", 42), prov
        );

        assertEquals(ResultStatus.SUCCESS, result.status());
        assertTrue(result.complete());
        assertNotNull(result.output());
    }

    @Test
    void shouldCreatePartialResult() {
        var prov = AgentResult.ResultProvenance.of(
            "exec-1", "agent-a"
        );
        var result = AgentResult.partial(
            java.util.Map.of("partial", true), prov
        );

        assertEquals(ResultStatus.PARTIAL, result.status());
        assertFalse(result.complete());
    }

    @Test
    void shouldCreateFailureResult() {
        var prov = AgentResult.ResultProvenance.of(
            "exec-1", "agent-a"
        );
        var result = AgentResult.failure(
            "ERR_001", "Something failed", prov
        );

        assertEquals(ResultStatus.FAILED, result.status());
        assertFalse(result.complete());
        assertEquals("ERR_001", result.errorCode());
    }

    @Test
    void shouldCreatePolicyDeniedResult() {
        var prov = AgentResult.ResultProvenance.of(
            "exec-1", "agent-a"
        );
        var result = AgentResult.policyDenied(
            "Not authorized", prov
        );

        assertEquals(
            ResultStatus.POLICY_DENIED, result.status());
        assertEquals("POLICY_DENIED", result.errorCode());
    }

    @Test
    void shouldCreateBudgetExceededResult() {
        var prov = AgentResult.ResultProvenance.of(
            "exec-1", "agent-a"
        );
        var result = AgentResult.budgetExceeded(
            "Token limit reached", prov
        );

        assertEquals(
            ResultStatus.BUDGET_EXCEEDED, result.status());
    }

    // --- Result Validation ---

    @Test
    void shouldValidateSuccessResult() {
        var validator = new ResultValidator();
        var contract = AgentContract.of("a", "1.0.0");
        var prov = AgentResult.ResultProvenance.of(
            "exec-1", "a"
        );
        var result = AgentResult.success("ok", prov);

        var errors = validator.validate(result, contract);
        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldRejectPartialMarkedComplete() {
        var validator = new ResultValidator();
        var contract = AgentContract.of("a", "1.0.0");
        var prov = AgentResult.ResultProvenance.of(
            "exec-1", "a"
        );

        // Manually create invalid partial
        var invalid = new AgentResult<>(
            ResultStatus.PARTIAL, "data", true,
            null, null, prov, null, null, null
        );

        var errors = validator.validate(invalid, contract);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Partial"));
    }

    @Test
    void shouldRejectFailedWithoutErrorCode() {
        var validator = new ResultValidator();
        var contract = AgentContract.of("a", "1.0.0");
        var prov = AgentResult.ResultProvenance.of(
            "exec-1", "a"
        );

        // Failed without error code
        var invalid = new AgentResult<>(
            ResultStatus.FAILED, null, false,
            null, null, prov, null, null, "msg"
        );

        var errors = validator.validate(invalid, contract);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("error code"));
    }

    @Test
    void shouldRejectNullResult() {
        var validator = new ResultValidator();
        var contract = AgentContract.of("a", "1.0.0");

        var errors = validator.validate(null, contract);
        assertFalse(errors.isEmpty());
        assertEquals("Result is null", errors.get(0));
    }

    // --- Events ---

    @Test
    void shouldCreateDelegationEvent() {
        var event = DelegationEvent.of(
            DelegationEventType.AGENT_DELEGATION_REQUESTED,
            "agent-a", "agent-b"
        );

        assertEquals(
            DelegationEventType.AGENT_DELEGATION_REQUESTED,
            event.type());
        assertEquals("agent-a", event.parentAgentId());
        assertEquals("agent-b", event.childAgentId());
        assertNotNull(event.timestamp());
    }

    @Test
    void shouldCreateEventWithExecutions() {
        var event = DelegationEvent.withExecutions(
            DelegationEventType.AGENT_EXECUTION_CREATED,
            "a", "b",
            "exec-parent", "exec-child", "exec-root"
        );

        assertEquals("exec-parent", event.parentExecutionId());
        assertEquals("exec-child", event.childExecutionId());
        assertEquals("exec-root", event.rootExecutionId());
    }

    @Test
    void shouldCollectEvents() {
        var sink = DelegationEventSink.collecting();

        sink.emit(DelegationEvent.of(
            DelegationEventType.AGENT_DELEGATION_REQUESTED,
            "a", "b"
        ));
        sink.emit(DelegationEvent.of(
            DelegationEventType.AGENT_DELEGATION_ACCEPTED,
            "a", "b"
        ));
        sink.emit(DelegationEvent.of(
            DelegationEventType.AGENT_EXECUTION_COMPLETED,
            "a", "b"
        ));

        assertEquals(3, sink.size());
        assertEquals(1,
            sink.eventsOfType(
                DelegationEventType.AGENT_DELEGATION_REQUESTED
            ).size());
        assertEquals(1,
            sink.eventsOfType(
                DelegationEventType.AGENT_EXECUTION_COMPLETED
            ).size());
    }

    @Test
    void shouldClearCollectingSink() {
        var sink = DelegationEventSink.collecting();
        sink.emit(DelegationEvent.of(
            DelegationEventType.AGENT_DELEGATION_REQUESTED,
            "a", "b"
        ));
        assertEquals(1, sink.size());
        sink.clear();
        assertEquals(0, sink.size());
    }
}
