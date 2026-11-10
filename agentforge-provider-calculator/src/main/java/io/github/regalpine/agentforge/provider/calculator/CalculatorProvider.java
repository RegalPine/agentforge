package io.github.regalpine.agentforge.provider.calculator;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CalculatorProvider
    implements CapabilityProvider {

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(
            "calculator",
            new Version("1.0.0"),
            Set.of(
                new Reference(
                    new EntityId(
                        "calculator.evaluate"
                    ),
                    new Version("1.0.0")
                )
            ),
            Map.of("type", "reference")
        );
    }

    @Override
    public CapabilityResult invoke(
        CapabilityInvocation invocation
    ) {
        Object exprArg =
            invocation.arguments()
                .get("expression");

        if (exprArg == null) {
            return new CapabilityFailure(
                "AF-CALC-001",
                "Missing 'expression' argument",
                false
            );
        }

        String expression = exprArg.toString();

        try {
            List<Token> tokens =
                new ExpressionTokenizer(expression)
                    .tokenize();

            Expression ast =
                new ExpressionParser(tokens)
                    .parse();

            double result =
                new ExpressionEvaluator()
                    .evaluate(ast);

            if (result == Math.floor(result)
                && !Double.isInfinite(result)
            ) {
                return new CapabilitySuccess(
                    Map.of(
                        "result",
                        (long) result
                    )
                );
            }

            return new CapabilitySuccess(
                Map.of("result", result)
            );

        } catch (IllegalArgumentException e) {
            return new CapabilityFailure(
                "AF-CALC-002",
                "Invalid expression: "
                    + e.getMessage(),
                false
            );
        } catch (ArithmeticException e) {
            return new CapabilityFailure(
                "AF-CALC-003",
                "Arithmetic error: "
                    + e.getMessage(),
                false
            );
        }
    }
}
