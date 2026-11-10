package io.github.regalpine.agentforge.server.execution;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.server.persistence.JdbcAccess;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

/**
 * PostgreSQL-backed {@link ExecutionStore}.
 * <p>
 * Replaces {@link InMemoryExecutionStore} in production mode.
 * Uses the {@code agentforge_server} schema tables:
 * {@code server_execution} and {@code server_execution_event}.
 */
public final class JdbcServerExecutionStore implements ExecutionStore {

    private static final String EXEC_TABLE = "agentforge_server.server_execution";
    private static final String EVENT_TABLE = "agentforge_server.server_execution_event";
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private final JdbcAccess db;

    public JdbcServerExecutionStore(DataSource source) {
        this.db = new JdbcAccess(source);
    }

    @Override
    public void create(ExecutionRecord record) {
        try {
            db.update(
                "INSERT INTO " + EXEC_TABLE
                    + " (execution_id, tenant_id, graph_id, graph_version, plan_fingerprint,"
                    + " status, version, idempotency_key, inputs_json, created_at, updated_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                record.executionId(),
                record.tenantId(),
                record.graphId(),
                record.graphVersion(),
                record.planFingerprint(),
                record.status().name(),
                record.version(),
                record.idempotencyKey(),
                encodeMap(record.inputs()),
                record.createdAt(),
                record.updatedAt()
            );
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Execution already exists: " + record.executionId(), e);
        }
    }

    @Override
    public Optional<ExecutionRecord> find(String tenantId, String executionId) {
        return db.query(
            "SELECT * FROM " + EXEC_TABLE + " WHERE tenant_id = ? AND execution_id = ?",
            this::mapRecord,
            tenantId, executionId
        ).stream().findFirst();
    }

    @Override
    public List<ExecutionRecord> list(String tenantId) {
        return db.query(
            "SELECT * FROM " + EXEC_TABLE + " WHERE tenant_id = ? ORDER BY created_at DESC",
            this::mapRecord,
            tenantId
        );
    }

    @Override
    public boolean updateStatus(String tenantId, String executionId,
                                ExecutionStatus newStatus, long expectedVersion) {
        int updated = db.update(
            "UPDATE " + EXEC_TABLE
                + " SET status = ?, version = ?, updated_at = ?"
                + " WHERE tenant_id = ? AND execution_id = ? AND version = ?",
            newStatus.name(),
            expectedVersion + 1,
            Instant.now(),
            tenantId, executionId, expectedVersion
        );
        return updated > 0;
    }

    @Override
    public void appendEvent(String tenantId, String executionId, ExecutionEvent event) {
        // Verify execution exists
        var found = find(tenantId, executionId);
        if (found.isEmpty()) {
            throw new IllegalStateException("Execution not found: " + executionId);
        }

        db.update(
            "INSERT INTO " + EVENT_TABLE + " (execution_id, sequence, event_json) VALUES (?, ?, ?)",
            executionId,
            event.sequence(),
            encodeEvent(event)
        );
    }

    @Override
    public List<ExecutionEvent> timeline(String tenantId, String executionId) {
        return db.query(
            "SELECT sequence, event_json FROM " + EVENT_TABLE
                + " WHERE execution_id = ? ORDER BY sequence ASC",
            row -> decodeEvent(
                executionId,
                row.getLong("sequence"),
                row.getString("event_json")
            ),
            executionId
        );
    }

    // ── Row mapping ──

    private ExecutionRecord mapRecord(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ExecutionRecord(
            rs.getString("execution_id"),
            rs.getString("tenant_id"),
            rs.getString("graph_id"),
            rs.getString("graph_version"),
            rs.getString("plan_fingerprint"),
            ExecutionStatus.valueOf(rs.getString("status")),
            rs.getLong("version"),
            rs.getString("idempotency_key"),
            decodeMap(rs.getString("inputs_json")),
            instant(rs, "created_at"),
            instant(rs, "updated_at")
        );
    }

    private static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        var odt = rs.getObject(column, java.time.OffsetDateTime.class);
        return odt == null ? null : odt.toInstant();
    }

    // ── Event JSON codec ──

    private static String encodeEvent(ExecutionEvent event) {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = JSON_FACTORY.createGenerator(sw)) {
            g.writeStartObject();
            g.writeNumberField("sequence", event.sequence());
            g.writeStringField("timestamp", event.timestamp().toString());
            g.writeStringField("type", event.type());
            g.writeFieldName("payload");
            writeMap(g, event.payload());
            g.writeEndObject();
            g.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode event", e);
        }
    }

    private static ExecutionEvent decodeEvent(String executionId, long sequence, String json) {
        try (JsonParser p = JSON_FACTORY.createParser(json)) {
            p.nextToken(); // START_OBJECT
            Instant timestamp = null;
            String type = null;
            Map<String, Object> payload = Map.of();

            while (p.nextToken() != JsonToken.END_OBJECT) {
                String field = p.currentName();
                p.nextToken();
                switch (field) {
                    case "timestamp" -> timestamp = Instant.parse(p.getText());
                    case "type" -> type = p.getText();
                    case "payload" -> payload = readMap(p);
                    default -> p.skipChildren();
                }
            }

            return new ExecutionEvent(
                new ExecutionId(executionId), sequence, timestamp, type, payload
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode event", e);
        }
    }

    // ── Map JSON codec ──

    private static String encodeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = JSON_FACTORY.createGenerator(sw)) {
            writeMap(g, map);
            g.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode map", e);
        }
    }

    private static Map<String, Object> decodeMap(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) return Map.of();
        try (JsonParser p = JSON_FACTORY.createParser(json)) {
            if (p.nextToken() != JsonToken.START_OBJECT) return Map.of();
            return readMap(p);
        } catch (IOException e) {
            return Map.of();
        }
    }

    private static void writeMap(JsonGenerator g, Map<String, Object> map) throws IOException {
        g.writeStartObject();
        for (var entry : new TreeMap<>(map).entrySet()) {
            g.writeFieldName(entry.getKey());
            writeValue(g, entry.getValue());
        }
        g.writeEndObject();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(JsonGenerator g, Object value) throws IOException {
        if (value == null) {
            g.writeNull();
        } else if (value instanceof String s) {
            g.writeString(s);
        } else if (value instanceof Integer i) {
            g.writeNumber(i);
        } else if (value instanceof Long l) {
            g.writeNumber(l);
        } else if (value instanceof Double d) {
            g.writeNumber(d);
        } else if (value instanceof Float f) {
            g.writeNumber(f);
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

    private static Map<String, Object> readMap(JsonParser p) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String key = p.currentName();
            p.nextToken();
            map.put(key, readValue(p));
        }
        return Map.copyOf(map);
    }

    private static Object readValue(JsonParser p) throws IOException {
        return switch (p.currentToken()) {
            case VALUE_STRING -> p.getText();
            case VALUE_NUMBER_INT -> p.getLongValue();
            case VALUE_NUMBER_FLOAT -> p.getDoubleValue();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            case START_OBJECT -> readMap(p);
            case START_ARRAY -> {
                List<Object> list = new ArrayList<>();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    list.add(readValue(p));
                }
                yield List.copyOf(list);
            }
            default -> {
                p.skipChildren();
                yield null;
            }
        };
    }
}
