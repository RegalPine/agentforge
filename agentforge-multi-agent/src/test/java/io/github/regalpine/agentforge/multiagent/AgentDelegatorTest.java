package io.github.regalpine.agentforge.multiagent;


import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.multiagent.budget.DelegationBudget;
import io.github.regalpine.agentforge.multiagent.contract.AgentContract;
import io.github.regalpine.agentforge.multiagent.delegation.DelegationContext;
import io.github.regalpine.agentforge.multiagent.delegation.DefaultAgentDelegator;
import io.github.regalpine.agentforge.multiagent.event.DelegationEventSink;
import io.github.regalpine.agentforge.multiagent.event.DelegationEventType;
import io.github.regalpine.agentforge.multiagent.policy.DefaultDelegationPolicy;
import io.github.regalpine.agentforge.multiagent.projection.DefaultContextProjection;
import io.github.regalpine.agentforge.multiagent.reference.InMemoryAgentResolver;
import io.github.regalpine.agentforge.multiagent.result.AgentResult;
import io.github.regalpine.agentforge.multiagent.result.ResultStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Delegation pipeline tests — AE-11 §11, §16.
 * AF-MA-003, AF-MA-005, AF-MA-006, AF-MA-007, AF-MA-008.
 */
class AgentDelegatorTest {

    private DefaultAgentDelegator createDelegator(
        InMemoryAgentResolver resolver,
        DelegationEventSink sink,
        java.util.function.Function<
            DefaultAgentDelegator.AgentExecutionRequest,
            AgentResult<Map<String, Object>>
        > executor
    ) {
        return new DefaultAgentDelegator(
            resolver, sink, executor
        );
    }

    @Test
    void shouldDelegateSuccessfully() {
        var resolver = new InMemoryAgentResolver();
        var childContract = AgentContract.of(
            "child-agent", "1.0.0"
        );
        resolver.register(childContract);

        var sink = DelegationEventSink.collecting();

        var delegator = createDelegator(
            resolver, sink,
            req -> AgentResult.success(
                Map.of("answer", "done"),
                AgentResult.ResultProvenance.of(
                    req.childExecutionId(),
                    req.agent().id().value()
                )
            )
        );

        var budget = DelegationBudget.defaults();
        var ctx = DelegationContext.root(
            "root-exec-1",
            "parent-agent",
            budget,
            DefaultDelegationPolicy.permissive(),
            new DefaultContextProjection()
        );

        var result = delegator.delegate(
            Reference.of("child-agent", "1.0.0"),
            Map.of("task", "analyze"),
            ctx
        );

        assertEquals(
            ResultStatus.SUCCESS, result.status());
        assertEquals("done",
            result.output().get("answer"));

        // Verify events
        assertTrue(sink.size() > 0);
        assertFalse(sink.eventsOfType(
            DelegationEventType.AGENT_DELEGATION_REQUESTED
        ).isEmpty());
        assertFalse(sink.eventsOfType(
            DelegationEventType.AGENT_DELEGATION_ACCEPTED
        ).isEmpty());
        assertFalse(sink.eventsOfType(
            DelegationEventType.AGENT_RESULT_RECEIVED
        ).isEmpty());
    }

    @Test
    void shouldRejectUnresolvableAgent() {
        var resolver = new InMemoryAgentResolver();
        var sink = DelegationEventSink.collecting();

        var delegator = createDelegator(
            resolver, sink,
            req -> AgentResult.success(
                Map.of(), null
            )
        );

        var budget = DelegationBudget.defaults();
        var ctx = DelegationContext.root(
            "root-1", "parent",
            budget, null, null
        );

        var result = delegator.delegate(
            Reference.of("unknown-agent", "1.0.0"),
            Map.of(), ctx
        );

        assertEquals(
            ResultStatus.FAILED, result.status());
        assertEquals("RESOLUTION_FAILED",
            result.errorCode());
    }

    @Test
    void shouldDetectCircularDelegation() {
        var resolver = new InMemoryAgentResolver();
        var childContract = AgentContract.of(
            "agent-a", "1.0.0"
        );
        resolver.register(childContract);

        var sink = DelegationEventSink.collecting();

        var delegator = createDelegator(
            resolver, sink,
            req -> AgentResult.success(Map.of(), null)
        );

        // Stack already contains agent-a
        var budget = DelegationBudget.defaults();
        var ctx = new DelegationContext(
            "root-1", "exec-1",
            "parent-agent",
            1, budget, null, null,
            List.of("parent-agent", "agent-a"),
            Map.of(), null
        );

        var result = delegator.delegate(
            Reference.of("agent-a", "1.0.0"),
            Map.of(), ctx
        );

        assertEquals(
            ResultStatus.POLICY_DENIED, result.status());
        assertTrue(result.errorMessage()
            .contains("Circular"));

        assertFalse(sink.eventsOfType(
            DelegationEventType.AGENT_CIRCULAR_DETECTION
        ).isEmpty());
    }

    @Test
    void shouldRejectDepthExceeded() {
        var resolver = new InMemoryAgentResolver();
        var childContract = AgentContract.of(
            "child", "1.0.0"
        );
        resolver.register(childContract);

        var delegator = createDelegator(
            resolver,
            DelegationEventSink.collecting(),
            req -> AgentResult.success(Map.of(), null)
        );

        // Depth = 5, default max = 5
        var budget = DelegationBudget.defaults();
        var ctx = new DelegationContext(
            "root-1", "exec-1",
            "parent",
            5, budget, null, null,
            List.of("root", "a", "b", "c", "d", "parent"),
            Map.of(), null
        );

        var result = delegator.delegate(
            Reference.of("child", "1.0.0"),
            Map.of(), ctx
        );

        assertEquals(
            ResultStatus.POLICY_DENIED, result.status());
        assertTrue(result.errorMessage()
            .contains("depth"));
    }

    @Test
    void shouldRejectPolicyDenied() {
        var resolver = new InMemoryAgentResolver();
        var childContract = AgentContract.of(
            "evil-agent", "1.0.0"
        );
        resolver.register(childContract);

        var delegator = createDelegator(
            resolver,
            DelegationEventSink.collecting(),
            req -> AgentResult.success(Map.of(), null)
        );

        var budget = DelegationBudget.defaults();
        var policy = new DefaultDelegationPolicy(
            Set.of("evil-agent"), Set.of(), 10
        );
        var ctx = DelegationContext.root(
            "root-1", "parent",
            budget, policy, null
        );

        var result = delegator.delegate(
            Reference.of("evil-agent", "1.0.0"),
            Map.of(), ctx
        );

        assertEquals(
            ResultStatus.POLICY_DENIED, result.status());
    }

    @Test
    void shouldCreateChildContextWithIdentity() {
        var budget = DelegationBudget.defaults();
        var ctx = DelegationContext.root(
            "root-exec",
            "parent-agent",
            budget, null, null
        );

        var childCtx = ctx.deriveChild(
            "child-exec", "child-agent", budget
        );

        assertEquals("root-exec",
            childCtx.rootExecutionId());
        assertEquals("child-exec",
            childCtx.parentExecutionId());
        assertEquals(1, childCtx.delegationDepth());
        assertTrue(childCtx.delegationStack()
            .contains("parent-agent"));
        assertTrue(childCtx.delegationStack()
            .contains("child-agent"));
        assertNotNull(childCtx.idempotencyKey());
    }

    @Test
    void shouldHandleExecutorException() {
        var resolver = new InMemoryAgentResolver();
        var childContract = AgentContract.of(
            "failing-agent", "1.0.0"
        );
        resolver.register(childContract);

        var delegator = createDelegator(
            resolver,
            DelegationEventSink.collecting(),
            req -> { throw new RuntimeException("boom"); }
        );

        var budget = DelegationBudget.defaults();
        var ctx = DelegationContext.root(
            "root-1", "parent",
            budget, null, null
        );

        var result = delegator.delegate(
            Reference.of("failing-agent", "1.0.0"),
            Map.of(), ctx
        );

        assertEquals(
            ResultStatus.FAILED, result.status());
        assertEquals("EXECUTION_ERROR",
            result.errorCode());
        assertEquals("boom", result.errorMessage());
    }
}
