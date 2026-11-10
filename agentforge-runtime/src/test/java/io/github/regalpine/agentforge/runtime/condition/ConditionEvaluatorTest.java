package io.github.regalpine.agentforge.runtime.condition;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Batch 5: ConditionEvaluator tests.
 * Verifies restricted expression syntax: paths, comparisons, logical ops, parens,
 * and backward-compatible key-based truthy lookup.
 */
class ConditionEvaluatorTest {

    @Test
    void nullOrBlankExpressionIsTrue() {
        assertTrue(ConditionEvaluator.evaluate(null, Map.of()));
        assertTrue(ConditionEvaluator.evaluate("", Map.of()));
        assertTrue(ConditionEvaluator.evaluate("  ", Map.of()));
    }

    @Test
    void backwardCompatibleKeyLookup() {
        assertTrue(ConditionEvaluator.evaluate("approved", Map.of("approved", true)));
        assertFalse(ConditionEvaluator.evaluate("approved", Map.of("approved", false)));
        assertTrue(ConditionEvaluator.evaluate("status", Map.of("status", "active")));
        assertFalse(ConditionEvaluator.evaluate("missing", Map.of("other", "value")));
    }

    @Test
    void booleanLiterals() {
        assertTrue(ConditionEvaluator.evaluate("true", Map.of()));
        assertFalse(ConditionEvaluator.evaluate("false", Map.of()));
    }

    @Test
    void comparisonOperators() {
        assertTrue(ConditionEvaluator.evaluate("score == 100", Map.of("score", 100)));
        assertFalse(ConditionEvaluator.evaluate("score == 50", Map.of("score", 100)));
        assertTrue(ConditionEvaluator.evaluate("score != 0", Map.of("score", 100)));
        assertTrue(ConditionEvaluator.evaluate("score > 50", Map.of("score", 100)));
        assertTrue(ConditionEvaluator.evaluate("score < 200", Map.of("score", 100)));
        assertTrue(ConditionEvaluator.evaluate("score >= 100", Map.of("score", 100)));
        assertTrue(ConditionEvaluator.evaluate("score <= 100", Map.of("score", 100)));
    }

    @Test
    void stringComparison() {
        assertTrue(ConditionEvaluator.evaluate("status == \"approved\"", Map.of("status", "approved")));
        assertFalse(ConditionEvaluator.evaluate("status == \"rejected\"", Map.of("status", "approved")));
    }

    @Test
    void logicalOperators() {
        assertTrue(ConditionEvaluator.evaluate("a && b", Map.of("a", true, "b", true)));
        assertFalse(ConditionEvaluator.evaluate("a && b", Map.of("a", true, "b", false)));
        assertTrue(ConditionEvaluator.evaluate("a || b", Map.of("a", false, "b", true)));
        assertFalse(ConditionEvaluator.evaluate("a || b", Map.of("a", false, "b", false)));
        assertTrue(ConditionEvaluator.evaluate("!a", Map.of("a", false)));
        assertFalse(ConditionEvaluator.evaluate("!a", Map.of("a", true)));
    }

    @Test
    void parentheses() {
        assertTrue(ConditionEvaluator.evaluate("(a || b) && c",
            Map.of("a", false, "b", true, "c", true)));
        assertFalse(ConditionEvaluator.evaluate("(a || b) && c",
            Map.of("a", false, "b", true, "c", false)));
    }

    @Test
    void pathExpression() {
        assertTrue(ConditionEvaluator.evaluate("user.approved",
            Map.of("user", Map.of("approved", true))));
        assertFalse(ConditionEvaluator.evaluate("user.approved",
            Map.of("user", Map.of("approved", false))));
    }

    @Test
    void complexExpression() {
        Map<String, Object> data = Map.of(
            "score", 85,
            "status", "active",
            "reviewed", true
        );
        assertTrue(ConditionEvaluator.evaluate("score > 80 && status == \"active\" && reviewed", data));
        assertFalse(ConditionEvaluator.evaluate("score > 90 || !reviewed", data));
    }

    @Test
    void unsupportedSyntaxThrows() {
        // Unterminated string
        assertThrows(IllegalArgumentException.class,
            () -> ConditionEvaluator.evaluate("\"unterminated", Map.of()));
        // Missing closing paren
        assertThrows(IllegalArgumentException.class,
            () -> ConditionEvaluator.evaluate("(a && b", Map.of("a", true, "b", true)));
        // Trailing garbage
        assertThrows(IllegalArgumentException.class,
            () -> ConditionEvaluator.evaluate("a b c", Map.of("a", true)));
    }

    @Test
    void numericLiterals() {
        assertTrue(ConditionEvaluator.evaluate("42 == 42", Map.of()));
        assertTrue(ConditionEvaluator.evaluate("3.14 > 3", Map.of()));
    }
}
