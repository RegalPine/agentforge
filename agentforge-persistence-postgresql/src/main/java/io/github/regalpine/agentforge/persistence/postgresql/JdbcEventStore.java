package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.persistence.EventStore;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionId;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-backed {@link EventStore}.
 */
public final class JdbcEventStore implements EventStore {

    private final DataSource dataSource;

    public JdbcEventStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void append(ExecutionEvent event) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO execution_event (execution_id, sequence, timestamp, type, payload) " +
                     "VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, event.executionId().value());
            ps.setLong(2, event.sequence());
            ps.setTimestamp(3, Timestamp.from(event.timestamp()));
            ps.setString(4, event.type());
            ps.setString(5, JsonCodec.encodeMap(event.payload()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to append event", e);
        }
    }

    @Override
    public List<ExecutionEvent> list(ExecutionId executionId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT execution_id, sequence, timestamp, type, payload " +
                     "FROM execution_event WHERE execution_id = ? ORDER BY sequence")) {
            ps.setString(1, executionId.value());
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to list events", e);
        }
    }

    @Override
    public List<ExecutionEvent> listAfter(ExecutionId executionId, long sequence) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT execution_id, sequence, timestamp, type, payload " +
                     "FROM execution_event WHERE execution_id = ? AND sequence > ? ORDER BY sequence")) {
            ps.setString(1, executionId.value());
            ps.setLong(2, sequence);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to list events after sequence", e);
        }
    }

    private static List<ExecutionEvent> mapRows(ResultSet rs) throws SQLException {
        List<ExecutionEvent> events = new ArrayList<>();
        while (rs.next()) {
            events.add(new ExecutionEvent(
                    new ExecutionId(rs.getString("execution_id")),
                    rs.getLong("sequence"),
                    rs.getTimestamp("timestamp").toInstant(),
                    rs.getString("type"),
                    JsonCodec.decodeMap(rs.getString("payload"))
            ));
        }
        return events;
    }
}
