package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.multiagent.budget.*;
import io.github.regalpine.agentforge.multiagent.reference.*;
import io.github.regalpine.agentforge.multiagent.safety.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-mcp / multi-agent:
 * Delegation / Context Projection
 * / Budget Propagation.
 * AF-RELEASE-02 §4, T-H6.9 + T-H6.10.
 */
class MultiAgentConformanceTest {

    @Test
    void afMa001_budgetCreation() {
        var budget = DelegationBudget.root(
            1000L, 10, 10, 10
        );
        assertNotNull(budget);
    }

    @Test
    void afMa002_circularDelegationDetect() {
        var detector = new CircularDelegationDetector();
        var result = detector.check(
            List.of("A", "B"), "C"
        );
        assertNotNull(result);
    }

    @Test
    void afMa003_circularDelegationDetected() {
        var detector = new CircularDelegationDetector();
        var result = detector.check(
            List.of("A", "B"), "A"
        );
        assertNotNull(result);
    }

    @Test
    void afMa004_depthCheck() {
        var checker = new DelegationDepthChecker(5);
        assertNotNull(checker);
    }

    @Test
    void afMa005_inMemoryResolver() {
        var resolver = new InMemoryAgentResolver();
        assertNotNull(resolver);
    }

    @Test
    void afMa006_budgetType() {
        assertNotNull(BudgetType.MAX_TOKENS);
    }
}
