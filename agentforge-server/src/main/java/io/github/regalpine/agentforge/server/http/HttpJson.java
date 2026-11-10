package io.github.regalpine.agentforge.server.http;

import com.fasterxml.jackson.core.*;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/** HTTP 边界 JSON 编解码；保留嵌套、空值、转义和数值精度。 */
final class HttpJson {
    private static final JsonFactory FACTORY = JsonFactory.builder()
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

    private HttpJson() {}

    static Map<String, Object> object(String json) {
        try (JsonParser parser = FACTORY.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) throw new IllegalArgumentException("Expected JSON object");
            Map<String, Object> result = readObject(parser);
            if (parser.nextToken() != null) throw new IllegalArgumentException("Trailing JSON content");
            return result;
        } catch (IOException error) {
            throw new IllegalArgumentException("Invalid JSON", error);
        }
    }

    private static Map<String, Object> readObject(JsonParser parser) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentToken() != JsonToken.FIELD_NAME) throw new IOException("Expected field");
            String key = parser.currentName();
            parser.nextToken();
            result.put(key, readValue(parser));
        }
        return result;
    }

    private static Object readValue(JsonParser parser) throws IOException {
        if (parser.currentToken() == null) throw new IOException("Unexpected end");
        return switch (parser.currentToken()) {
            case START_OBJECT -> readObject(parser);
            case START_ARRAY -> {
                List<Object> result = new ArrayList<>();
                while (parser.nextToken() != JsonToken.END_ARRAY) result.add(readValue(parser));
                yield result;
            }
            case VALUE_STRING -> parser.getText();
            case VALUE_NUMBER_INT -> parser.getNumberValue();
            case VALUE_NUMBER_FLOAT -> parser.getDecimalValue();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            default -> throw new IOException("Invalid value");
        };
    }

    static String stringify(Object value) {
        StringWriter output = new StringWriter();
        try (JsonGenerator generator = FACTORY.createGenerator(output)) {
            writeValue(generator, value);
        } catch (IOException error) {
            throw new IllegalArgumentException("Cannot encode JSON", error);
        }
        return output.toString();
    }

    private static void writeValue(JsonGenerator generator, Object value) throws IOException {
        if (value == null) generator.writeNull();
        else if (value instanceof String string) generator.writeString(string);
        else if (value instanceof Boolean bool) generator.writeBoolean(bool);
        else if (value instanceof BigDecimal decimal) generator.writeNumber(decimal);
        else if (value instanceof BigInteger integer) generator.writeNumber(integer);
        else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            generator.writeNumber(((Number) value).longValue());
        } else if (value instanceof Number number) {
            if (!Double.isFinite(number.doubleValue())) throw new IllegalArgumentException("Non-finite number");
            generator.writeNumber(number.toString());
        } else if (value instanceof Map<?, ?> map) {
            generator.writeStartObject();
            for (var entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) throw new IllegalArgumentException("Non-string key");
                generator.writeFieldName(key);
                writeValue(generator, entry.getValue());
            }
            generator.writeEndObject();
        } else if (value instanceof Iterable<?> list) {
            generator.writeStartArray();
            for (Object item : list) writeValue(generator, item);
            generator.writeEndArray();
        } else {
            throw new IllegalArgumentException("Unsupported JSON value");
        }
    }
}
