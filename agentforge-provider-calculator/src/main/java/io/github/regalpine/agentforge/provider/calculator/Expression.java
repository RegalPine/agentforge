package io.github.regalpine.agentforge.provider.calculator;

sealed interface Expression
    permits NumberLiteral,
            BinaryExpression {
}

record NumberLiteral(
    double value
) implements Expression {
}

record BinaryExpression(
    Expression left,
    TokenType operator,
    Expression right
) implements Expression {
}
