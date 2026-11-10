package io.github.regalpine.agentforge.provider.calculator;

final class ExpressionEvaluator {

    double evaluate(Expression expr) {
        if (expr instanceof NumberLiteral num) {
            return num.value();
        }

        if (expr instanceof BinaryExpression bin) {
            double left = evaluate(bin.left());
            double right = evaluate(bin.right());

            return switch (bin.operator()) {
                case PLUS -> left + right;
                case MINUS -> left - right;
                case MULTIPLY -> left * right;
                case DIVIDE -> {
                    if (right == 0.0) {
                        throw new ArithmeticException(
                            "Division by zero"
                        );
                    }
                    yield left / right;
                }
                case MODULO -> {
                    if (right == 0.0) {
                        throw new ArithmeticException(
                            "Division by zero"
                        );
                    }
                    yield left % right;
                }
                default -> throw new IllegalArgumentException(
                    "Unsupported operator: "
                        + bin.operator()
                );
            };
        }

        throw new IllegalArgumentException(
            "Unknown expression type"
        );
    }
}
