package io.github.regalpine.agentforge.memory.postgresql;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.JsonReadContext;
import io.github.regalpine.agentforge.memory.*;
import io.github.regalpine.agentforge.security.DataClassification;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * PostgreSQL memory store.
 * AF-ENHANCE-03 §5-7: persists memory
 * records via JDBC with parameterised
 * SQL. Supports FACT / PREFERENCE /
 * TASK / EXPERIENCE / SUMMARY types
 * and EXACT / KEYWORD / SEMANTIC /
 * HYBRID search strategies.
 *
 * <p>Requires a {@link DataSource}
 * (e.g. HikariCP). When pgvector is
 * enabled, the embedding column uses
 * the pgvector extension.
 */
public final class PostgresMemoryStore
    implements MemoryStore {

    private final PostgresMemoryConfig config;
    private final DataSource dataSource;
    private final String qualifiedTable;

    public PostgresMemoryStore(
        PostgresMemoryConfig config,
        DataSource dataSource
    ) {
        this.config =
            Objects.requireNonNull(config, "config");
        this.dataSource =
            Objects.requireNonNull(dataSource, "dataSource");
        this.qualifiedTable = config.qualifiedTable();
    }

    /**
     * Initialise the schema (DDL).
     * Safe to call multiple times.
     */
    public void initialiseSchema() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                PostgresMemorySql.createTableSql(
                    qualifiedTable
                )
            );
            stmt.execute(
                PostgresMemorySql.createIndexSql(
                    qualifiedTable
                )
            );

            if (config.enablePgVector()) {
                stmt.execute(
                    PostgresMemorySql
                        .createVectorColumnSql(
                            qualifiedTable,
                            config.embeddingDimension()
                        )
                );
            }
        }
    }

    @Override
    public MemoryRecord save(MemoryRecord record) {
        Objects.requireNonNull(record, "record");

        String sql = PostgresMemorySql.insertSql(
            qualifiedTable
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, record.memoryId());
            ps.setString(2, record.tenantId());
            ps.setString(3, record.subjectId());
            ps.setString(4, record.type().name());
            ps.setString(5, record.scope().name());
            ps.setString(6, record.content());
            ps.setString(7, mapToJson(record.attributes()));
            ps.setTimestamp(8,
                Timestamp.from(record.createdAt()));
            ps.setTimestamp(9,
                Timestamp.from(record.updatedAt()));
            ps.setTimestamp(10,
                record.expiresAt() == null ? null
                    : Timestamp.from(record.expiresAt()));
            ps.setString(11, record.status().name());
            ps.setString(12, record.source().name());
            ps.setDouble(13,
                record.confidence().score());
            ps.setString(14,
                record.confidence().basis().name());
            ps.setString(15,
                record.importance().name());
            ps.setString(16,
                record.provenance().sourceExecutionId());
            // G3-5: serialize full provenance chain
            ps.setString(17,
                provenanceToJson(record.provenance()));
            ps.setString(18,
                record.classification().name());
            ps.setLong(19, record.version());

            ps.executeUpdate();
            return record;

        } catch (SQLException e) {
            throw new PostgresMemoryException(
                "Failed to save memory: "
                    + record.memoryId(), e
            );
        }
    }

    @Override
    public Optional<MemoryRecord> get(
        String memoryId
    ) {
        Objects.requireNonNull(memoryId, "memoryId");

        String sql = PostgresMemorySql.selectByIdSql(
            qualifiedTable
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, memoryId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(
                        mapRow(rs)
                    );
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new PostgresMemoryException(
                "Failed to get memory: " + memoryId, e
            );
        }
    }

    @Override
    public void update(MemoryRecord record) {
        Objects.requireNonNull(record, "record");

        String sql = PostgresMemorySql.updateSql(
            qualifiedTable
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, record.content());
            ps.setString(2, mapToJson(record.attributes()));
            ps.setTimestamp(3,
                Timestamp.from(record.updatedAt()));
            ps.setTimestamp(4,
                record.expiresAt() == null ? null
                    : Timestamp.from(record.expiresAt()));
            ps.setString(5, record.status().name());
            ps.setString(6, record.memoryId());
            ps.setLong(7, record.version());

            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new PostgresMemoryException(
                    "Memory not found or version conflict: "
                        + record.memoryId()
                );
            }

        } catch (SQLException e) {
            throw new PostgresMemoryException(
                "Failed to update memory: "
                    + record.memoryId(), e
            );
        }
    }

    @Override
    public void delete(String memoryId) {
        Objects.requireNonNull(memoryId, "memoryId");

        String sql = PostgresMemorySql.softDeleteSql(
            qualifiedTable
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, memoryId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new PostgresMemoryException(
                "Failed to delete memory: " + memoryId, e
            );
        }
    }

    /**
     * Get the underlying config.
     */
    public PostgresMemoryConfig config() {
        return config;
    }

    // G3-5: Jackson-based JSON codec
    private static final JsonFactory JSON_FACTORY =
        new JsonFactory();

    // --- internal mapping ---

    static MemoryRecord mapRow(ResultSet rs)
        throws SQLException
    {
        String confBasisStr = rs.getString(
            "confidence_basis"
        );

        ConfidenceBasis confBasis;
        try {
            confBasis = ConfidenceBasis.valueOf(
                confBasisStr
            );
        } catch (Exception e) {
            confBasis = ConfidenceBasis.MANUAL;
        }

        double confScore = rs.getDouble(
            "confidence_val"
        );

        if (rs.wasNull()) {
            confScore = 0.9;
        }

        String provOrigin = rs.getString(
            "prov_origin"
        );

        // G3-5: read attributes JSONB column
        String attrsJson = rs.getString("attributes");
        Map<String, Object> attrs = jsonToMap(attrsJson);

        // G3-5: read provenance chain from JSONB
        String provChainJson = rs.getString("prov_chain");
        MemoryProvenance provenance = parseProvenance(
            provOrigin, provChainJson
        );

        return MemoryRecord.builder()
            .memoryId(rs.getString("memory_id"))
            .tenantId(rs.getString("tenant_id"))
            .subjectId(rs.getString("subject_id"))
            .type(MemoryType.valueOf(
                rs.getString("type")
            ))
            .scope(MemoryScope.valueOf(
                rs.getString("scope")
            ))
            .content(rs.getString("content"))
            .attributes(attrs)
            .createdAt(rs.getTimestamp("created_at")
                .toInstant())
            .updatedAt(rs.getTimestamp("updated_at")
                .toInstant())
            .expiresAt(mapOptionalTimestamp(
                rs, "expires_at"
            ))
            .status(MemoryStatus.valueOf(
                rs.getString("status")
            ))
            .source(MemorySource.valueOf(
                rs.getString("source")
            ))
            .confidence(new MemoryConfidence(
                confScore, confBasis
            ))
            .importance(MemoryImportance.valueOf(
                rs.getString("importance")
            ))
            .provenance(provenance)
            .classification(DataClassification.valueOf(
                rs.getString("classification")
            ))
            .version(rs.getLong("version"))
            .build();
    }

    private static Instant mapOptionalTimestamp(
        ResultSet rs, String col
    ) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts == null ? null : ts.toInstant();
    }

    // G3-5: Jackson-based map to JSON with type preservation
    static String mapToJson(Map<String, Object> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return "{}";
        }
        try (ByteArrayOutput out =
                 new ByteArrayOutput();
             JsonGenerator g = JSON_FACTORY
                 .createGenerator(out)) {
            writeMap(g, attrs);
            g.flush();
            return out.toString(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "{}";
        }
    }

    // G3-5: Jackson-based JSON to map with type preservation
    static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try (JsonParser p = JSON_FACTORY
                 .createParser(json)) {
            if (p.nextToken() == JsonToken.START_OBJECT) {
                return readMap(p);
            }
            return Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    // G3-5: parse provenance from prov_origin + prov_chain JSONB
    private static MemoryProvenance parseProvenance(
        String provOrigin, String provChainJson
    ) {
        String origin = provOrigin == null
            ? "" : provOrigin;
        if (provChainJson == null
            || provChainJson.isBlank()
            || "[]".equals(provChainJson)) {
            return new MemoryProvenance(
                origin, "", "", ""
            );
        }
        try (JsonParser p = JSON_FACTORY
                 .createParser(provChainJson)) {
            if (p.nextToken() == JsonToken.START_OBJECT) {
                Map<String, Object> m = readMap(p);
                return new MemoryProvenance(
                    origin,
                    str(m, "sourceMessageId"),
                    str(m, "sourceCapabilityId"),
                    str(m, "createdBy")
                );
            }
            return new MemoryProvenance(
                origin, "", "", ""
            );
        } catch (Exception e) {
            return new MemoryProvenance(
                origin, "", "", ""
            );
        }
    }

    // G3-5: serialize provenance chain to JSONB
    static String provenanceToJson(
        MemoryProvenance prov
    ) {
        if (prov == null) return "[]";
        String msgId = prov.sourceMessageId();
        String capId = prov.sourceCapabilityId();
        String createdBy = prov.createdBy();
        if ((msgId == null || msgId.isEmpty())
            && (capId == null || capId.isEmpty())
            && (createdBy == null
                || createdBy.isEmpty())) {
            return "[]";
        }
        try (ByteArrayOutput out =
                 new ByteArrayOutput();
             JsonGenerator g = JSON_FACTORY
                 .createGenerator(out)) {
            g.writeStartObject();
            if (msgId != null && !msgId.isEmpty()) {
                g.writeStringField(
                    "sourceMessageId", msgId
                );
            }
            if (capId != null && !capId.isEmpty()) {
                g.writeStringField(
                    "sourceCapabilityId", capId
                );
            }
            if (createdBy != null
                && !createdBy.isEmpty()) {
                g.writeStringField(
                    "createdBy", createdBy
                );
            }
            g.writeEndObject();
            g.flush();
            return out.toString(
                java.nio.charset.StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String str(
        Map<String, Object> m, String key
    ) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }

    private static void writeMap(
        JsonGenerator g,
        Map<String, Object> map
    ) throws Exception {
        g.writeStartObject();
        TreeMap<String, Object> sorted =
            new TreeMap<>(map);
        for (var e : sorted.entrySet()) {
            g.writeFieldName(e.getKey());
            writeValue(g, e.getValue());
        }
        g.writeEndObject();
    }

    private static void writeValue(
        JsonGenerator g, Object v
    ) throws Exception {
        if (v == null) {
            g.writeNull();
        } else if (v instanceof String s) {
            g.writeString(s);
        } else if (v instanceof Integer i) {
            g.writeNumber(i);
        } else if (v instanceof Long l) {
            g.writeNumber(l);
        } else if (v instanceof Double d) {
            g.writeNumber(d);
        } else if (v instanceof Float f) {
            g.writeNumber(f);
        } else if (v instanceof Boolean b) {
            g.writeBoolean(b);
        } else if (v instanceof Map<?,?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed =
                (Map<String, Object>) m;
            writeMap(g, typed);
        } else if (v instanceof List<?> list) {
            g.writeStartArray();
            for (Object item : list) {
                writeValue(g, item);
            }
            g.writeEndArray();
        } else {
            g.writeString(v.toString());
        }
    }

    private static Map<String, Object> readMap(
        JsonParser p
    ) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        while (p.nextToken() == JsonToken.FIELD_NAME) {
            String field = p.currentName();
            p.nextToken();
            map.put(field, readValue(p));
        }
        return Map.copyOf(map);
    }

    private static Object readValue(
        JsonParser p
    ) throws Exception {
        JsonToken t = p.currentToken();
        return switch (t) {
            case VALUE_STRING -> p.getText();
            case VALUE_NUMBER_INT -> {
                long l = p.getLongValue();
                if (l >= Integer.MIN_VALUE
                    && l <= Integer.MAX_VALUE) {
                    yield (int) l;
                }
                yield l;
            }
            case VALUE_NUMBER_FLOAT -> p.getDoubleValue();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            case START_OBJECT -> readMap(p);
            case START_ARRAY -> {
                List<Object> list = new ArrayList<>();
                while (p.nextToken()
                    != JsonToken.END_ARRAY) {
                    list.add(readValue(p));
                }
                yield List.copyOf(list);
            }
            default -> null;
        };
    }

    /**
     * Minimal ByteArrayOutputStream that
     * also implements OutputStream for
     * Jackson's createGenerator.
     */
    private static final class ByteArrayOutput
        extends java.io.ByteArrayOutputStream {
        ByteArrayOutput() { super(256); }
    }

    /**
     * PostgreSQL memory exception.
     */
    public static final class PostgresMemoryException
        extends RuntimeException {

        public PostgresMemoryException(
            String msg
        ) {
            super(msg);
        }

        public PostgresMemoryException(
            String msg, Throwable cause
        ) {
            super(msg, cause);
        }
    }
}
