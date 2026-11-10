package io.github.regalpine.agentforge.agent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BudgetTrackerTest {

    @Test
    void shouldTrackIterations() {
        BudgetTracker tracker = new BudgetTracker(LoopBudget.iterations(3));
        assertEquals(1, tracker.recordIteration());
        assertEquals(2, tracker.recordIteration());
        assertEquals(3, tracker.recordIteration());

        Optional<StopCondition> result = tracker.checkBudget();
        assertTrue(result.isPresent());
        assertEquals(StopCondition.StopReason.MAX_ITERATIONS, result.get().reason());
    }

    @Test
    void shouldTrackToolCalls() {
        BudgetTracker tracker = new BudgetTracker(
            new LoopBudget(null, null, null, 2L, null, null)
        );
        tracker.recordToolCall();
        assertTrue(tracker.checkBudget().isEmpty());
        tracker.recordToolCall();
        assertTrue(tracker.checkBudget().isPresent());
    }

    @Test
    void shouldTrackModelCalls() {
        BudgetTracker tracker = new BudgetTracker(
            new LoopBudget(null, null, null, null, 2L, null)
        );
        tracker.recordModelCall();
        tracker.recordModelCall();
        Optional<StopCondition> result = tracker.checkBudget();
        assertTrue(result.isPresent());
        assertEquals(StopCondition.StopReason.MODEL_BUDGET_EXHAUSTED, result.get().reason());
    }

    @Test
    void shouldTrackTokens() {
        BudgetTracker tracker = new BudgetTracker(
            new LoopBudget(null, null, 100L, null, null, null)
        );
        tracker.recordTokens(50);
        assertTrue(tracker.checkBudget().isEmpty());
        tracker.recordTokens(50);
        assertTrue(tracker.checkBudget().isPresent());
    }

    @Test
    void shouldTrackCost() {
        BudgetTracker tracker = new BudgetTracker(
            new LoopBudget(null, null, null, null, null, 1.0)
        );
        tracker.recordCost(0.5);
        assertTrue(tracker.checkBudget().isEmpty());
        tracker.recordCost(0.5);
        assertTrue(tracker.checkBudget().isPresent());
    }

    @Test
    void shouldNotExhaustWithNullLimits() {
        BudgetTracker tracker = new BudgetTracker(
            new LoopBudget(null, null, null, null, null, null)
        );
        for (int i = 0; i < 1000; i++) {
            tracker.recordIteration();
            tracker.recordToolCall();
            tracker.recordModelCall();
            tracker.recordTokens(100);
            tracker.recordCost(0.01);
        }
        assertTrue(tracker.checkBudget().isEmpty());
    }

    @Test
    void shouldReturnAccumulatedStats() {
        BudgetTracker tracker = new BudgetTracker(LoopBudget.defaults());
        tracker.recordIteration();
        tracker.recordIteration();
        tracker.recordToolCall();
        tracker.recordModelCall();
        tracker.recordTokens(100);
        tracker.recordCost(0.5);

        assertEquals(2, tracker.getIterations());
        assertEquals(1, tracker.getToolCallsMade());
        assertEquals(1, tracker.getModelCallsMade());
        assertEquals(100, tracker.getTokensUsed());
        assertEquals(0.5, tracker.getCostAccumulated(), 0.001);
    }
}
