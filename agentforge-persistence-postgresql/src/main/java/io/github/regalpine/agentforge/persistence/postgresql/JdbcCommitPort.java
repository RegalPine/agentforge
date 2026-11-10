package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.runtime.CommitPort;
import io.github.regalpine.agentforge.event.ExecutionState;

import javax.sql.DataSource;
import java.sql.*;

/**
 * JDBC-backed {@link CommitPort} implementation.
 * Uses database transactions to atomically update state and append events.
 * G2-5: Unified transactional boundary for state + event.
 */
public final class JdbcCommitPort implements CommitPort {

    private final DataSource dataSource;

    public JdbcCommitPort(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void create(ExecutionState state) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO execution_state (execution_id, status, current_node, data, " +
                "completed_nodes, waiting_nodes, version, next_event_sequence) " +
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
            throw new PersistenceException("Failed to create initial execution state", e);
        }
    }

    @Override
    public boolean commit(CommitCommand command) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. CAS update execution_state
                boolean casOk;
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE execution_state SET status=?, current_node=?, data=?, " +
                        "completed_nodes=?, waiting_nodes=?, version=?, next_event_sequence=?, " +
                        "updated_at=CURRENT_TIMESTAMP " +
                        "WHERE execution_id=? AND version=?")) {
                    ExecutionState ns = command.newState();
                    ps.setString(1, ns.status().name());
                    ps.setString(2, ns.currentNode() != null ? ns.currentNode().value() : null);
                    ps.setString(3, JsonCodec.encodeMap(ns.data()));
                    ps.setString(4, JsonCodec.encodeEntityIdSet(ns.completedNodes()));
                    ps.setString(5, JsonCodec.encodeEntityIdSet(ns.waitingNodes()));
                    ps.setLong(6, ns.version());
                    ps.setLong(7, ns.nextEventSequence());
                    ps.setString(8, ns.executionId().value());
                    ps.setLong(9, command.expectedVersion());
                    casOk = ps.executeUpdate() > 0;
                }

                if (!casOk) {
                    conn.rollback();
                    return false;
                }

                // 2. Append event in same transaction
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO execution_event (execution_id, sequence, timestamp, type, payload) " +
                        "VALUES (?, ?, ?, ?, ?)")) {
                    var event = command.event();
                    ps.setString(1, event.executionId().value());
                    ps.setLong(2, event.sequence());
                    ps.setTimestamp(3, Timestamp.from(event.timestamp()));
                    ps.setString(4, event.type());
                    ps.setString(5, JsonCodec.encodeMap(event.payload()));
                    ps.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw new PersistenceException("Commit failed, rolled back", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to get connection for commit", e);
        }
    }
}
