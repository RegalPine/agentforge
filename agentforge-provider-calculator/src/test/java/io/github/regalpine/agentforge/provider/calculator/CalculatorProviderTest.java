package io.github.regalpine.agentforge.provider.calculator;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorProviderTest {

    private final CalculatorProvider provider =
        new CalculatorProvider();

    @Test
    void shouldEvaluateSimpleAddition() {
        var result = evaluate("10 + 20");

        assertInstanceOf(
            CapabilitySuccess.class,
            result
        );

        assertEquals(
            30L,
            ((CapabilitySuccess) result)
                .output().get("result")
        );
    }

    @Test
    void shouldRespectOperatorPrecedence() {
        var result = evaluate("10 + 20 * 3");

        assertEquals(
            70L,
            ((CapabilitySuccess) result)
                .output().get("result")
        );
    }

    @Test
    void shouldHandleParentheses() {
        var result = evaluate("(10 + 20) * 3");

        assertEquals(
            90L,
            ((CapabilitySuccess) result)
                .output().get("result")
        );
    }

    @Test
    void shouldHandleDivision() {
        var result = evaluate("100 / 4");

        assertEquals(
            25L,
            ((CapabilitySuccess) result)
                .output().get("result")
        );
    }

    @Test
    void shouldHandleModulo() {
        var result = evaluate("10 % 3");

        var success =
            (CapabilitySuccess) result;

        assertEquals(
            1L,
            success.output().get("result")
        );
    }

    @Test
    void shouldHandleNegativeResult() {
        var result = evaluate("0 - 5");

        assertEquals(
            -5L,
            ((CapabilitySuccess) result)
                .output().get("result")
        );
    }

    @Test
    void shouldReturnFailureForDivisionByZero() {
        var result = evaluate("10 / 0");

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );

        assertEquals(
            "AF-CALC-003",
            ((CapabilityFailure) result).code()
        );
    }

    @Test
    void shouldReturnFailureForInvalidExpr() {
        var result = evaluate("10 + + 20");

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );
    }

    @Test
    void shouldReturnFailureForMissingExpr() {
        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-001",
                new EntityId(
                    "calculator.evaluate"
                ),
                new Version("1.0.0"),
                Map.of(),
                null
            );

        var result = provider.invoke(invocation);

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );
    }

    @Test
    void shouldRejectScriptInjection() {
        var result = evaluate(
            "Runtime.exec('evil')"
        );

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );
    }

    @Test
    void shouldHandleDecimalResult() {
        var result = evaluate("10 / 3");

        var success =
            (CapabilitySuccess) result;

        double value = (double)
            success.output().get("result");

        assertTrue(
            Math.abs(value - 3.333333) < 0.001
        );
    }

    private CapabilityResult evaluate(
        String expression
    ) {
        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-" + expression.hashCode(),
                new EntityId(
                    "calculator.evaluate"
                ),
                new Version("1.0.0"),
                Map.of(
                    "expression",
                    expression
                ),
                null
            );

        return provider.invoke(invocation);
    }
}
