package io.github.regalpine.agentforge.provider.calculator;

import java.util.ArrayList;
import java.util.List;

final class ExpressionTokenizer {

    private final String input;
    private int pos;

    ExpressionTokenizer(String input) {
        this.input = input;
        this.pos = 0;
    }

    List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                tokens.add(readNumber());
                continue;
            }

            switch (c) {
                case '+' -> tokens.add(
                    new Token(TokenType.PLUS, "+")
                );
                case '-' -> tokens.add(
                    new Token(TokenType.MINUS, "-")
                );
                case '*' -> tokens.add(
                    new Token(TokenType.MULTIPLY, "*")
                );
                case '/' -> tokens.add(
                    new Token(TokenType.DIVIDE, "/")
                );
                case '%' -> tokens.add(
                    new Token(TokenType.MODULO, "%")
                );
                case '(' -> tokens.add(
                    new Token(TokenType.LPAREN, "(")
                );
                case ')' -> tokens.add(
                    new Token(TokenType.RPAREN, ")")
                );
                default -> throw new IllegalArgumentException(
                    "Unexpected character: " + c
                );
            }

            pos++;
        }

        tokens.add(
            new Token(TokenType.EOF, "")
        );

        return tokens;
    }

    private Token readNumber() {
        int start = pos;
        boolean hasDot = false;

        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (c == '.') {
                if (hasDot) {
                    break;
                }
                hasDot = true;
            } else if (!Character.isDigit(c)) {
                break;
            }

            pos++;
        }

        return new Token(
            TokenType.NUMBER,
            input.substring(start, pos)
        );
    }
}
