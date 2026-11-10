package io.github.regalpine.agentforge.graph;

public record Expression(
    String id,
    String expression
) {

    public Expression {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                "Expression id cannot be blank"
            );
        }

        if (expression == null ||
            expression.isBlank()) {

            throw new IllegalArgumentException(
                "Expression cannot be blank"
            );
        }
    }
}
