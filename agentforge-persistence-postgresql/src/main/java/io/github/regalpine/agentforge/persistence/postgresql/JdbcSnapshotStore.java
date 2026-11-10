package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.persistence.Snapshot;
import io.github.regalpine.agentforge.persistence.SnapshotStore;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

/**
 * JDBC-backed {@link SnapshotStore}.
 */
public final class JdbcSnapshotStore implements SnapshotStore {

    private final DataSource dataSource;

    public JdbcSnapshotStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Snapshot snapshot) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO snapshot (snapshot_id, execution_id, event_sequence, state_version, current_node_id, status, state, state_fingerprint, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, snapshot.snapshotId());
            ps.setString(2, snapshot.executionId().value());
            ps.setLong(3, snapshot.eventSequence());
            ps.setLong(4, snapshot.stateVersion());
            ps.setString(5, snapshot.currentNodeId() != null ? snapshot.currentNodeId().value() : null);
            ps.setString(6, snapshot.status() != null ? snapshot.status().name() : null);
            ps.setString(7, JsonCodec.encodeMap(snapshot.state()));
            ps.setString(8, snapshot.stateFingerprint());
            ps.setTimestamp(9, Timestamp.from(snapshot.createdAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to save snapshot", e);
        }
    }

    @Override
    public Optional<Snapshot> latest(ExecutionId executionId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT snapshot_id, execution_id, event_sequence, state_version, current_node_id, status, state, state_fingerprint, created_at " +
                     "FROM snapshot WHERE execution_id = ? ORDER BY event_sequence DESC LIMIT 1")) {
            ps.setString(1, executionId.value());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find latest snapshot", e);
        }
    }

    private static Snapshot mapRow(ResultSet rs) throws SQLException {
        String statusStr = rs.getString("status");
        return new Snapshot(
                rs.getString("snapshot_id"),
                new ExecutionId(rs.getString("execution_id")),
                rs.getLong("event_sequence"),
                rs.getLong("state_version"),
                rs.getString("current_node_id") != null ? new EntityId(rs.getString("current_node_id")) : null,
                statusStr != null ? ExecutionStatus.valueOf(statusStr) : null,
                JsonCodec.decodeMap(rs.getString("state")),
                rs.getString("state_fingerprint"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
