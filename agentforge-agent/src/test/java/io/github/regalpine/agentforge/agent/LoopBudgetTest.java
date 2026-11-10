package io.github.regalpine.agentforge.agent;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LoopBudgetTest {

    @Test
    void shouldCreateIterationsBudget() {
        LoopBudget budget = LoopBudget.iterations(50);
        assertEquals(50L, budget.maxIterations());
        assertNull(budget.maxDuration());
        assertNull(budget.maxTokens());
    }

    @Test
    void shouldCreateBudgetWithDuration() {
        LoopBudget budget = LoopBudget.of(100, Duration.ofMinutes(10));
        assertEquals(100L, budget.maxIterations());
        assertEquals(Duration.ofMinutes(10), budget.maxDuration());
    }

    @Test
    void shouldCreateDefaultBudget() {
        LoopBudget budget = LoopBudget.defaults();
        assertEquals(100L, budget.maxIterations());
        assertEquals(Duration.ofMinutes(5), budget.maxDuration());
    }

    @Test
    void shouldRejectNegativeIterations() {
        assertThrows(IllegalArgumentException.class, () -> LoopBudget.iterations(-1));
    }

    @Test
    void shouldBuildWithAllDimensions() {
        LoopBudget budget = LoopBudget.builder()
            .maxIterations(50L)
            .maxDuration(Duration.ofSeconds(30))
            .maxTokens(10000L)
            .maxToolCalls(20L)
            .maxModelCalls(30L)
            .maxCost(1.0)
            .build();

        assertEquals(50L, budget.maxIterations());
        assertEquals(Duration.ofSeconds(30), budget.maxDuration());
        assertEquals(10000L, budget.maxTokens());
        assertEquals(20L, budget.maxToolCalls());
        assertEquals(30L, budget.maxModelCalls());
        assertEquals(1.0, budget.maxCost());
    }
}
