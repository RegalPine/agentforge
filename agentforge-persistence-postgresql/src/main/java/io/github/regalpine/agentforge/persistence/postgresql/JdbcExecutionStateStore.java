package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.persistence.ExecutionStateStore;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

/**
 * JDBC-backed {@link ExecutionStateStore}.
 * Uses optimistic locking via the version column for CAS.
 */
public final class JdbcExecutionStateStore implements ExecutionStateStore {

    private final DataSource dataSource;

    public JdbcExecutionStateStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void create(ExecutionState state) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO execution_state (execution_id, status, current_node, data, completed_nodes, waiting_nodes, version, next_event_sequence) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, state.executionId().value());
            ps.setString(2, state.status().name());
            ps.setString(3, state.currentNode() != null ? state.currentNode().value() : null);
            ps.setString(4, JsonCodec.encodeMap(state.data()));
            ps.setString(5, JsonCodec.encodeEntityIdSet(state.completedNodes()));
            ps.setString(6, JsonCodec.encodeEntityIdSet(state.waitingNodes()));
            ps.setLong(7, state.version());
            ps.setLong(8, state.nextEventSequence());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to create execution state", e);
        }
    }

    @Override
    public Optional<ExecutionState> find(ExecutionId executionId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT execution_id, status, current_node, data, completed_nodes, waiting_nodes, version, next_event_sequence " +
                     "FROM execution_state WHERE execution_id = ?")) {
            ps.setString(1, executionId.value());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find execution state", e);
        }
    }

    @Override
    public boolean compareAndSet(ExecutionId executionId, long expectedVersion, ExecutionState newState) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE execution_state SET status=?, current_node=?, data=?, completed_nodes=?, " +
                     "waiting_nodes=?, version=?, next_event_sequence=?, updated_at=CURRENT_TIMESTAMP " +
                     "WHERE execution_id=? AND version=?")) {
            ps.setString(1, newState.status().name());
            ps.setString(2, newState.currentNode() != null ? newState.currentNode().value() : null);
            ps.setString(3, JsonCodec.encodeMap(newState.data()));
            ps.setString(4, JsonCodec.encodeEntityIdSet(newState.completedNodes()));
            ps.setString(5, JsonCodec.encodeEntityIdSet(newState.waitingNodes()));
            ps.setLong(6, newState.version());
            ps.setLong(7, newState.nextEventSequence());
            ps.setString(8, executionId.value());
            ps.setLong(9, expectedVersion);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to CAS execution state", e);
        }
    }

    private static ExecutionState mapRow(ResultSet rs) throws SQLException {
        return new ExecutionState(
                new ExecutionId(rs.getString("execution_id")),
                ExecutionStatus.valueOf(rs.getString("status")),
                rs.getString("current_node") != null ? new EntityId(rs.getString("current_node")) : null,
                JsonCodec.decodeMap(rs.getString("data")),
                JsonCodec.decodeEntityIdSet(rs.getString("completed_nodes")),
                JsonCodec.decodeEntityIdSet(rs.getString("waiting_nodes")),
                rs.getLong("version"),
                rs.getLong("next_event_sequence")
        );
    }
}
