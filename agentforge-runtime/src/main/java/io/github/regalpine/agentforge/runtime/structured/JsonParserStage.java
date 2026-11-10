package io.github.regalpine.agentforge.runtime.structured;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pipeline stage that parses JSON from raw LLM output.
 * <p>
 * Handles common LLM output patterns:
 * <ul>
 *   <li>Pure JSON: {"key": "value"}</li>
 *   <li>Markdown code blocks: ```json ... ```</li>
 *   <li>Text with embedded JSON</li>
 * </ul>
 */
public final class JsonParserStage implements PipelineStage<String, Map<String, Object>> {

    @Override
    public Map<String, Object> process(String input, PipelineContext context) throws PipelineException {
        if (input == null || input.isBlank()) {
            throw new PipelineException(name(), "Input is null or blank");
        }

        String json = extractJson(input);
        if (json == null) {
            throw new PipelineException(name(), "No JSON found in output");
        }

        try {
            return parseJson(json);
        } catch (Exception e) {
            throw new PipelineException(name(), "Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public String name() {
        return "JsonParser";
    }

    private String extractJson(String input) {
        String trimmed = input.trim();

        // Try markdown code block
        if (trimmed.contains("```json")) {
            int start = trimmed.indexOf("```json") + 7;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        if (trimmed.contains("```")) {
            int start = trimmed.indexOf("```") + 3;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                String block = trimmed.substring(start, end).trim();
                if (block.startsWith("{") || block.startsWith("[")) {
                    return block;
                }
            }
        }

        // Try direct JSON object
        if (trimmed.startsWith("{")) {
            return trimmed;
        }

        // Try to find JSON object in text
        int braceStart = trimmed.indexOf('{');
        int braceEnd = trimmed.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            return trimmed.substring(braceStart, braceEnd + 1);
        }

        return null;
    }

    /**
     * Simple JSON parser for objects (no external dependencies).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Expected JSON object, got: " + json.substring(0, Math.min(50, json.length())));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        int i = 1; // skip opening brace
        int len = json.length() - 1; // skip closing brace

        while (i < len) {
            // Skip whitespace
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= len) break;

            // Parse key
            if (json.charAt(i) != '"') {
                throw new IllegalArgumentException("Expected '\"' at position " + i);
            }
            int keyStart = i + 1;
            int keyEnd = findClosingQuote(json, keyStart);
            String key = unescapeString(json.substring(keyStart, keyEnd));
            i = keyEnd + 1;

            // Skip whitespace and colon
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= len || json.charAt(i) != ':') {
                throw new IllegalArgumentException("Expected ':' after key at position " + i);
            }
            i++;
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;

            // Parse value
            Object[] valueAndNext = parseValue(json, i, len);
            result.put(key, valueAndNext[0]);
            i = (int) valueAndNext[1];

            // Skip whitespace and comma
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;
            if (i < len && json.charAt(i) == ',') i++;
        }

        return result;
    }

    private Object[] parseValue(String json, int i, int len) {
        if (i >= len) throw new IllegalArgumentException("Unexpected end of JSON");

        char c = json.charAt(i);
        if (c == '"') {
            int end = findClosingQuote(json, i + 1);
            return new Object[]{unescapeString(json.substring(i + 1, end)), end + 1};
        }
        if (c == '{') {
            int end = findClosingBrace(json, i);
            return new Object[]{parseJson(json.substring(i, end + 1)), end + 1};
        }
        if (c == '[') {
            int end = findClosingBracket(json, i);
            return new Object[]{parseArray(json.substring(i, end + 1)), end + 1};
        }
        if (c == 't' || c == 'f') {
            if (json.startsWith("true", i)) return new Object[]{true, i + 4};
            if (json.startsWith("false", i)) return new Object[]{false, i + 5};
            throw new IllegalArgumentException("Invalid value at position " + i);
        }
        if (c == 'n') {
            if (json.startsWith("null", i)) return new Object[]{null, i + 4};
            throw new IllegalArgumentException("Invalid value at position " + i);
        }
        if (c == '-' || Character.isDigit(c)) {
            int end = i;
            boolean isFloat = false;
            if (json.charAt(end) == '-') end++;
            while (end < len && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == 'e' || json.charAt(end) == 'E' || json.charAt(end) == '+' || json.charAt(end) == '-')) {
                if (json.charAt(end) == '.' || json.charAt(end) == 'e' || json.charAt(end) == 'E') isFloat = true;
                end++;
            }
            String numStr = json.substring(i, end);
            if (isFloat) return new Object[]{Double.parseDouble(numStr), end};
            long val = Long.parseLong(numStr);
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) return new Object[]{(int) val, end};
            return new Object[]{val, end};
        }
        throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + i);
    }

    private java.util.List<Object> parseArray(String json) {
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        java.util.List<Object> result = new java.util.ArrayList<>();
        int i = 1;
        int len = json.length() - 1;
        while (i < len) {
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= len) break;
            Object[] valueAndNext = parseValue(json, i, len);
            result.add(valueAndNext[0]);
            i = (int) valueAndNext[1];
            while (i < len && Character.isWhitespace(json.charAt(i))) i++;
            if (i < len && json.charAt(i) == ',') i++;
        }
        return result;
    }

    private int findClosingQuote(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '\\') { i++; continue; }
            if (json.charAt(i) == '"') return i;
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    private int findClosingBrace(String json, int start) {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && inString) { i++; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') depth++;
            if (c == '}') { depth--; if (depth == 0) return i; }
        }
        throw new IllegalArgumentException("Unterminated object");
    }

    private int findClosingBracket(String json, int start) {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && inString) { i++; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '[') depth++;
            if (c == ']') { depth--; if (depth == 0) return i; }
        }
        throw new IllegalArgumentException("Unterminated array");
    }

    private String unescapeString(String s) {
        if (!s.contains("\\")) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '/': sb.append('/'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'b': sb.append('\b'); i++; break;
                    case 'f': sb.append('\f'); i++; break;
                    case 'u':
                        if (i + 5 < s.length()) {
                            sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                            i += 5;
                        }
                        break;
                    default: sb.append(c); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
