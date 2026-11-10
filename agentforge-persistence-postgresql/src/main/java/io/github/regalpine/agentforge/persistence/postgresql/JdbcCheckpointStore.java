package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.persistence.Checkpoint;
import io.github.regalpine.agentforge.persistence.CheckpointStore;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed {@link CheckpointStore}.
 */
public final class JdbcCheckpointStore implements CheckpointStore {

    private final DataSource dataSource;

    public JdbcCheckpointStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Checkpoint checkpoint) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO checkpoint (checkpoint_id, execution_id, sequence, state_version, current_node, status, data, state_fingerprint, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, checkpoint.checkpointId());
            ps.setString(2, checkpoint.executionId().value());
            ps.setLong(3, checkpoint.sequence());
            ps.setLong(4, checkpoint.stateVersion());
            ps.setString(5, checkpoint.currentNode() != null ? checkpoint.currentNode().value() : null);
            ps.setString(6, checkpoint.status() != null ? checkpoint.status().name() : null);
            ps.setString(7, JsonCodec.encodeMap(checkpoint.data()));
            ps.setString(8, checkpoint.stateFingerprint());
            ps.setTimestamp(9, Timestamp.from(checkpoint.createdAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to save checkpoint", e);
        }
    }

    @Override
    public Optional<Checkpoint> latest(ExecutionId executionId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT checkpoint_id, execution_id, sequence, state_version, current_node, status, data, state_fingerprint, created_at " +
                     "FROM checkpoint WHERE execution_id = ? ORDER BY state_version DESC LIMIT 1")) {
            ps.setString(1, executionId.value());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find latest checkpoint", e);
        }
    }

    @Override
    public List<Checkpoint> list(ExecutionId executionId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT checkpoint_id, execution_id, sequence, state_version, current_node, status, data, state_fingerprint, created_at " +
                     "FROM checkpoint WHERE execution_id = ? ORDER BY state_version")) {
            ps.setString(1, executionId.value());
            try (ResultSet rs = ps.executeQuery()) {
                List<Checkpoint> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to list checkpoints", e);
        }
    }

    private static Checkpoint mapRow(ResultSet rs) throws SQLException {
        String statusStr = rs.getString("status");
        return new Checkpoint(
                rs.getString("checkpoint_id"),
                new ExecutionId(rs.getString("execution_id")),
                rs.getLong("sequence"),
                rs.getLong("state_version"),
                rs.getString("current_node") != null ? new EntityId(rs.getString("current_node")) : null,
                statusStr != null ? ExecutionStatus.valueOf(statusStr) : null,
                JsonCodec.decodeMap(rs.getString("data")),
                rs.getString("state_fingerprint"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
