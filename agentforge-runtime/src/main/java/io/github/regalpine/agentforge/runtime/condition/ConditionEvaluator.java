package io.github.regalpine.agentforge.runtime.condition;

import java.util.Map;
import java.util.Objects;

/**
 * Restricted condition expression evaluator.
 * <p>
 * Supports: path expressions (a.b.c), boolean literals, comparisons
 * (==, !=, >, <, >=, <=), logical operators (!, &&, ||), parentheses.
 * <p>
 * Backward compatible: a bare identifier is treated as a key lookup
 * in the state data map (truthy check).
 * <p>
 * No eval/reflection is used. Unsupported syntax causes compilation
 * failure (IllegalArgumentException).
 */
public final class ConditionEvaluator {

    private ConditionEvaluator() {}

    /**
     * Evaluate a condition expression against state data.
     *
     * @param expression the condition expression (may be null/blank → true)
     * @param stateData  the current execution state data
     * @return true if the condition is satisfied
     * @throws IllegalArgumentException if syntax is unsupported
     */
    public static boolean evaluate(String expression, Map<String, Object> stateData) {
        if (expression == null || expression.isBlank()) return true;
        if (stateData == null) stateData = Map.of();

        Parser parser = new Parser(expression.trim(), stateData);
        Object result = parser.parseExpression();
        if (parser.pos < parser.input.length()) {
            throw new IllegalArgumentException(
                "Unexpected characters at position " + parser.pos + " in: " + expression);
        }
        return toBoolean(result);
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value == null) return false;
        String s = value.toString();
        return !s.isBlank() && !"false".equalsIgnoreCase(s) && !"0".equals(s);
    }

    private static Object resolvePath(String path, Map<String, Object> data) {
        String[] parts = path.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(Object left, Object right) {
        if (left instanceof Number ln && right instanceof Number rn) {
            return Double.compare(ln.doubleValue(), rn.doubleValue());
        }
        if (left instanceof Comparable lc && right != null) {
            try {
                return lc.compareTo(right);
            } catch (ClassCastException e) {
                return left.toString().compareTo(right.toString());
            }
        }
        return Objects.toString(left, "").compareTo(Objects.toString(right, ""));
    }

    // ── Recursive descent parser ──

    private static final class Parser {
        final String input;
        final Map<String, Object> data;
        int pos;

        Parser(String input, Map<String, Object> data) {
            this.input = input;
            this.data = data;
            this.pos = 0;
        }

        void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
        }

        boolean match(String s) {
            skipWhitespace();
            if (input.startsWith(s, pos)) {
                pos += s.length();
                return true;
            }
            return false;
        }

        boolean peek(String s) {
            skipWhitespace();
            return input.startsWith(s, pos);
        }

        // expression = orExpr
        Object parseExpression() {
            return parseOr();
        }

        // orExpr = andExpr ('||' andExpr)*
        Object parseOr() {
            Object left = parseAnd();
            while (match("||")) {
                Object right = parseAnd();
                left = toBoolean(left) || toBoolean(right);
            }
            return left;
        }

        // andExpr = notExpr ('&&' notExpr)*
        Object parseAnd() {
            Object left = parseNot();
            while (match("&&")) {
                Object right = parseNot();
                left = toBoolean(left) && toBoolean(right);
            }
            return left;
        }

        // notExpr = '!' notExpr | comparison
        Object parseNot() {
            if (match("!")) {
                Object val = parseNot();
                return !toBoolean(val);
            }
            return parseComparison();
        }

        // comparison = primary (('==' | '!=' | '>' | '<' | '>=' | '<=') primary)?
        Object parseComparison() {
            Object left = parsePrimary();
            skipWhitespace();
            if (match("==")) return Objects.equals(left, parsePrimary());
            if (match("!=")) return !Objects.equals(left, parsePrimary());
            if (match(">=")) return compare(left, parsePrimary()) >= 0;
            if (match("<=")) return compare(left, parsePrimary()) <= 0;
            if (match(">")) return compare(left, parsePrimary()) > 0;
            if (match("<")) return compare(left, parsePrimary()) < 0;
            return left;
        }

        // primary = '(' expression ')' | literal | path
        Object parsePrimary() {
            skipWhitespace();
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of expression: " + input);
            }

            // Parenthesized expression
            if (input.charAt(pos) == '(') {
                pos++;
                Object val = parseExpression();
                skipWhitespace();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw new IllegalArgumentException("Missing ')' in expression: " + input);
                }
                pos++;
                return val;
            }

            // String literal
            if (input.charAt(pos) == '"') {
                return parseStringLiteral();
            }

            // Number literal
            if (Character.isDigit(input.charAt(pos))
                || (input.charAt(pos) == '-' && pos + 1 < input.length()
                    && Character.isDigit(input.charAt(pos + 1)))) {
                return parseNumberLiteral();
            }

            // Boolean/null literals or path
            if (Character.isLetter(input.charAt(pos)) || input.charAt(pos) == '_') {
                String ident = parseIdentifier();
                return switch (ident) {
                    case "true" -> Boolean.TRUE;
                    case "false" -> Boolean.FALSE;
                    case "null" -> null;
                    default -> resolvePath(ident, data);
                };
            }

            throw new IllegalArgumentException(
                "Unexpected character '" + input.charAt(pos) + "' at position " + pos
                    + " in: " + input);
        }

        String parseIdentifier() {
            int start = pos;
            while (pos < input.length()
                && (Character.isLetterOrDigit(input.charAt(pos))
                    || input.charAt(pos) == '_' || input.charAt(pos) == '.')) {
                // Don't consume dots that are followed by non-identifier chars
                if (input.charAt(pos) == '.' && (pos + 1 >= input.length()
                    || (!Character.isLetterOrDigit(input.charAt(pos + 1))
                        && input.charAt(pos + 1) != '_'))) {
                    break;
                }
                pos++;
            }
            if (pos == start) {
                throw new IllegalArgumentException("Expected identifier at position " + pos);
            }
            return input.substring(start, pos);
        }

        String parseStringLiteral() {
            pos++; // skip opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < input.length() && input.charAt(pos) != '"') {
                if (input.charAt(pos) == '\\' && pos + 1 < input.length()) {
                    pos++;
                    sb.append(switch (input.charAt(pos)) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        case '\\' -> '\\';
                        case '"' -> '"';
                        default -> input.charAt(pos);
                    });
                } else {
                    sb.append(input.charAt(pos));
                }
                pos++;
            }
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Unterminated string in: " + input);
            }
            pos++; // skip closing quote
            return sb.toString();
        }

        Number parseNumberLiteral() {
            int start = pos;
            if (input.charAt(pos) == '-') pos++;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
            if (pos < input.length() && input.charAt(pos) == '.') {
                pos++;
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
                return Double.parseDouble(input.substring(start, pos));
            }
            long val = Long.parseLong(input.substring(start, pos));
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) return (int) val;
            return val;
        }
    }
}
