package io.github.regalpine.agentforge.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AgentLoopTest {

    @Test
    void shouldFinishOnFirstDecision() {
        AgentDecisionEngine engine = ctx -> new FinishDecision(Map.of("result", "done"));
        AgentContext context = new AgentContext(Map.of(), "test goal", Map.of(), List.of(), Map.of(), Map.of());

        AgentLoop loop = AgentLoop.withDefaults(engine, context);
        AgentResult result = loop.run();

        assertEquals(AgentResultStatus.COMPLETED, result.status());
        assertEquals("done", result.outputs().get("result"));
        assertEquals(StopCondition.StopReason.FINISHED, loop.getStopCondition().reason());
    }

    @Test
    void shouldFailOnFailDecision() {
        AgentDecisionEngine engine = ctx -> new FailDecision("ERR", "something failed");
        AgentContext context = new AgentContext(Map.of(), "test", Map.of(), List.of(), Map.of(), Map.of());

        AgentLoop loop = AgentLoop.withDefaults(engine, context);
        AgentResult result = loop.run();

        assertEquals(AgentResultStatus.FAILED, result.status());
        assertEquals("ERR", result.metadata().get("errorCode"));
    }

    @Test
    void shouldWaitOnWaitDecision() {
        AgentDecisionEngine engine = ctx -> new WaitDecision("user-input", null);
        AgentContext context = new AgentContext(Map.of(), "test", Map.of(), List.of(), Map.of(), Map.of());

        AgentLoop loop = AgentLoop.withDefaults(engine, context);
        AgentResult result = loop.run();

        assertEquals(AgentResultStatus.WAITING, result.status());
        assertEquals("user-input", result.outputs().get("waitKey"));
    }

    @Test
    void shouldStopOnMaxIterations() {
        AgentDecisionEngine engine = ctx -> new CapabilityDecision(DecisionType.CALL_CAPABILITY, "tool-1", Map.of(), "test");
        AgentContext context = new AgentContext(Map.of(), "test", Map.of(), List.of(), Map.of(), Map.of());

        // Use high stagnation threshold to avoid stagnation detection interfering
        AgentLoop loop = new AgentLoop(engine, LoopBudget.iterations(5), new StagnationDetector(100, 100), PolicyGate.permissive(), context);
        AgentResult result = loop.run();

        assertEquals(AgentResultStatus.FAILED, result.status());
        assertEquals("MAX_ITERATIONS", result.metadata().get("errorCode"));
    }

    @Test
    void shouldDetectStagnation() {
        AgentDecisionEngine engine = ctx -> new CapabilityDecision(DecisionType.CALL_CAPABILITY, "same-tool", Map.of(), "stagnation test");
        AgentContext context = new AgentContext(Map.of(), "test", Map.of(), List.of(), Map.of(), Map.of());

        StagnationDetector detector = new StagnationDetector(5, 3);
        AgentLoop loop = new AgentLoop(engine, LoopBudget.iterations(100), detector, PolicyGate.permissive(), context);
        AgentResult result = loop.run();

        assertEquals(AgentResultStatus.FAILED, result.status());
        assertEquals("STAGNATION", result.metadata().get("errorCode"));
    }

    @Test
    void shouldBlockWithPolicyGate() {
        AgentDecisionEngine engine = ctx -> new CapabilityDecision(DecisionType.CALL_CAPABILITY, "tool-1", Map.of(), "policy test");
        AgentContext context = new AgentContext(Map.of(), "test", Map.of(), List.of(), Map.of(), Map.of());

        PolicyGate gate = PolicyGate.of("no-capability", (d, c) -> !(d instanceof CapabilityDecision));
        AgentLoop loop = new AgentLoop(engine, LoopBudget.iterations(100), new StagnationDetector(), gate, context);
        AgentResult result = loop.run();

        assertEquals(AgentResultStatus.FAILED, result.status());
        assertEquals("POLICY_BLOCKED", result.metadata().get("errorCode"));
    }

    @Test
    void shouldRecordSteps() {
        AtomicInteger counter = new AtomicInteger(0);
        AgentDecisionEngine engine = ctx -> {
            if (counter.incrementAndGet() >= 3) {
                return new FinishDecision(Map.of("done", true));
            }
            return new CapabilityDecision(DecisionType.CALL_CAPABILITY, "tool-" + counter.get(), Map.of(), "step test");
        };
        AgentContext context = new AgentContext(Map.of(), "test", Map.of(), List.of(), Map.of(), Map.of());

        AgentLoop loop = AgentLoop.withDefaults(engine, context);
        loop.run();

        assertFalse(loop.getSteps().isEmpty());
        assertTrue(loop.getBudgetTracker().getIterations() >= 3);
    }

    @Test
    void shouldHandleDecisionEngineError() {
        AgentDecisionEngine engine = ctx -> { throw new RuntimeException("engine error"); };
        AgentContext context = new AgentContext(Map.of(), "test", Map.of(), List.of(), Map.of(), Map.of());

        AgentLoop loop = AgentLoop.withDefaults(engine, context);
        AgentResult result = loop.run();

        assertEquals(AgentResultStatus.FAILED, result.status());
        assertEquals("DECISION_ENGINE_ERROR", result.metadata().get("errorCode"));
    }
}
