package io.github.regalpine.agentforge.server.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** 仅为已知服务端记录提供 JSON 映射，不启用多态类型加载。 */
final class PersistenceJson {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .build();

    private PersistenceJson() {}
    static String write(Object record) {
        try { return MAPPER.writeValueAsString(record); }
        catch (JsonProcessingException error) { throw new IllegalArgumentException("Cannot encode stored record", error); }
    }
    static <T> T read(String json, Class<T> type) {
        try { return MAPPER.readValue(json, type); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Corrupt stored record", error); }
    }
}
