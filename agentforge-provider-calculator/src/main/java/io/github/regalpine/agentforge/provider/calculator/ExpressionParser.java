package io.github.regalpine.agentforge.provider.calculator;

import java.util.List;

final class ExpressionParser {

    private final List<Token> tokens;
    private int pos;

    ExpressionParser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    Expression parse() {
        Expression expr = parseExpression();

        if (current().type() != TokenType.EOF) {
            throw new IllegalArgumentException(
                "Unexpected token: "
                    + current().value()
            );
        }

        return expr;
    }

    private Expression parseExpression() {
        return parseAddSub();
    }

    private Expression parseAddSub() {
        Expression left = parseMulDiv();

        while (
            current().type() == TokenType.PLUS
            || current().type() == TokenType.MINUS
        ) {
            Token op = current();
            advance();
            Expression right = parseMulDiv();
            left = new BinaryExpression(
                left, op.type(), right
            );
        }

        return left;
    }

    private Expression parseMulDiv() {
        Expression left = parseUnary();

        while (
            current().type() == TokenType.MULTIPLY
            || current().type() == TokenType.DIVIDE
            || current().type() == TokenType.MODULO
        ) {
            Token op = current();
            advance();
            Expression right = parseUnary();
            left = new BinaryExpression(
                left, op.type(), right
            );
        }

        return left;
    }

    private Expression parseUnary() {
        if (current().type() == TokenType.MINUS) {
            advance();
            Expression operand = parsePrimary();
            return new BinaryExpression(
                new NumberLiteral(0),
                TokenType.MINUS,
                operand
            );
        }

        return parsePrimary();
    }

    private Expression parsePrimary() {
        Token token = current();

        if (token.type() == TokenType.NUMBER) {
            advance();
            return new NumberLiteral(
                Double.parseDouble(token.value())
            );
        }

        if (token.type() == TokenType.LPAREN) {
            advance();
            Expression expr = parseExpression();

            if (current().type()
                != TokenType.RPAREN
            ) {
                throw new IllegalArgumentException(
                    "Missing closing parenthesis"
                );
            }

            advance();
            return expr;
        }

        throw new IllegalArgumentException(
            "Unexpected token: "
                + token.value()
        );
    }

    private Token current() {
        return tokens.get(pos);
    }

    private void advance() {
        pos++;
    }
}
