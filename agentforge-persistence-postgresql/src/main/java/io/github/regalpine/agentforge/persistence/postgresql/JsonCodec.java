package io.github.regalpine.agentforge.persistence.postgresql;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.github.regalpine.agentforge.kernel.EntityId;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * JSON codec for persistence serialization.
 * <p>
 * G3-4: Uses Jackson core for proper handling of
 * nested objects, arrays, type preservation
 * (numbers, booleans, null), and edge cases.
 */
final class JsonCodec {

    private static final JsonFactory JSON_FACTORY =
        new JsonFactory();

    private JsonCodec() {
    }

    // ── Map encoding/decoding ──

    static String encodeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = JSON_FACTORY.createGenerator(sw)) {
            writeMap(g, map);
            g.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to encode map to JSON", e
            );
        }
    }

    static Map<String, Object> decodeMap(String json) {
        if (json == null || json.isBlank()
            || json.equals("{}")) {
            return Map.of();
        }
        try (JsonParser p = JSON_FACTORY.createParser(json)) {
            if (p.nextToken() != JsonToken.START_OBJECT) {
                return Map.of();
            }
            return readMap(p);
        } catch (IOException e) {
            return Map.of();
        }
    }

    // ── EntityId Set encoding/decoding ──

    static String encodeEntityIdSet(Set<EntityId> set) {
        if (set == null || set.isEmpty()) {
            return "[]";
        }
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = JSON_FACTORY.createGenerator(sw)) {
            g.writeStartArray();
            set.stream()
                .sorted(Comparator.comparing(e -> e.value()))
                .forEach(e -> {
                    try {
                        g.writeString(e.value());
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
            g.writeEndArray();
            g.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to encode EntityId set", e
            );
        }
    }

    static Set<EntityId> decodeEntityIdSet(String json) {
        if (json == null || json.isBlank()
            || json.equals("[]")) {
            return Set.of();
        }
        try (JsonParser p = JSON_FACTORY.createParser(json)) {
            if (p.nextToken() != JsonToken.START_ARRAY) {
                return Set.of();
            }
            Set<EntityId> result = new LinkedHashSet<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                if (p.currentToken() == JsonToken.VALUE_STRING) {
                    result.add(new EntityId(p.getText()));
                }
            }
            return Set.copyOf(result);
        } catch (IOException e) {
            return Set.of();
        }
    }

    // ── Internal: write helpers ──

    private static void writeMap(
        JsonGenerator g, Map<String, Object> map
    ) throws IOException {
        g.writeStartObject();
        // TreeMap for deterministic ordering
        for (var entry : new TreeMap<>(map).entrySet()) {
            g.writeFieldName(entry.getKey());
            writeValue(g, entry.getValue());
        }
        g.writeEndObject();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(
        JsonGenerator g, Object value
    ) throws IOException {
        if (value == null) {
            g.writeNull();
        } else if (value instanceof String s) {
            g.writeString(s);
        } else if (value instanceof Number n) {
            if (value instanceof Integer i) {
                g.writeNumber(i);
            } else if (value instanceof Long l) {
                g.writeNumber(l);
            } else if (value instanceof Double d) {
                g.writeNumber(d);
            } else if (value instanceof Float f) {
                g.writeNumber(f);
            } else {
                g.writeNumber(n.doubleValue());
            }
        } else if (value instanceof Boolean b) {
            g.writeBoolean(b);
        } else if (value instanceof Map<?, ?> m) {
            writeMap(g, (Map<String, Object>) m);
        } else if (value instanceof List<?> list) {
            g.writeStartArray();
            for (Object item : list) {
                writeValue(g, item);
            }
            g.writeEndArray();
        } else {
            g.writeString(value.toString());
        }
    }

    // ── Internal: read helpers ──

    private static Map<String, Object> readMap(
        JsonParser p
    ) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String key = p.currentName();
            p.nextToken();
            map.put(key, readValue(p));
        }
        return Map.copyOf(map);
    }

    private static Object readValue(JsonParser p)
        throws IOException {
        return switch (p.currentToken()) {
            case VALUE_STRING -> p.getText();
            case VALUE_NUMBER_INT -> p.getLongValue();
            case VALUE_NUMBER_FLOAT -> p.getDoubleValue();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            case START_OBJECT -> readMap(p);
            case START_ARRAY -> readList(p);
            default -> {
                p.skipChildren();
                yield null;
            }
        };
    }

    private static List<Object> readList(JsonParser p)
        throws IOException {
        List<Object> list = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            list.add(readValue(p));
        }
        return List.copyOf(list);
    }
}
