package io.github.regalpine.agentforge.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PolicyGateTest {

    @Test
    void shouldAllowAllWithPermissiveGate() {
        PolicyGate gate = PolicyGate.permissive();
        AgentDecisionContext ctx = new AgentDecisionContext(null, new AgentContext(Map.of(), "test", Map.of(), List.of(), Map.of(), Map.of()), List.of(), AgentRuntimePolicy.defaults());
        assertTrue(gate.check(new FinishDecision(Map.of("result", "done")), ctx).isEmpty());
    }

    @Test
    void shouldBlockWithCustomRule() {
        PolicyGate gate = PolicyGate.of("no-finish", (decision, ctx) -> !(decision instanceof FinishDecision));
        AgentDecisionContext ctx = new AgentDecisionContext(null, new AgentContext(Map.of(), "test", Map.of(), List.of(), Map.of(), Map.of()), List.of(), AgentRuntimePolicy.defaults());

        assertTrue(gate.check(new FinishDecision(Map.of()), ctx).isPresent());
        assertTrue(gate.check(new FailDecision("ERR", "fail"), ctx).isEmpty());
    }

    @Test
    void shouldBlockWithCorrectReason() {
        PolicyGate gate = PolicyGate.of("block-all", (d, c) -> false);
        AgentDecisionContext ctx = new AgentDecisionContext(null, new AgentContext(Map.of(), "test", Map.of(), List.of(), Map.of(), Map.of()), List.of(), AgentRuntimePolicy.defaults());

        Optional<StopCondition> result = gate.check(new FinishDecision(Map.of()), ctx);
        assertTrue(result.isPresent());
        assertEquals(StopCondition.StopReason.POLICY_BLOCKED, result.get().reason());
        assertTrue(result.get().detail().contains("block-all"));
    }

    @Test
    void shouldReportRuleCount() {
        assertEquals(0, PolicyGate.permissive().ruleCount());
        PolicyGate gate = PolicyGate.of("rule1", (d, c) -> true);
        assertEquals(1, gate.ruleCount());
    }
}
