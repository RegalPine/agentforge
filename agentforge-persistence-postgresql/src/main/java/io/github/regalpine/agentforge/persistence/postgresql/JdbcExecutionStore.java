package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.persistence.ExecutionStore;
import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

/**
 * JDBC-backed {@link ExecutionStore}.
 */
public final class JdbcExecutionStore implements ExecutionStore {

    private final DataSource dataSource;

    public JdbcExecutionStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void create(Execution execution) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO execution (execution_id, graph_id, graph_version, plan_fingerprint, " +
                     "status, created_at, started_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            setParams(ps, execution);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to create execution", e);
        }
    }

    @Override
    public Optional<Execution> find(ExecutionId executionId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT execution_id, graph_id, graph_version, plan_fingerprint, " +
                     "status, created_at, started_at, completed_at FROM execution WHERE execution_id = ?")) {
            ps.setString(1, executionId.value());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find execution", e);
        }
    }

    @Override
    public void update(Execution execution) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE execution SET status=?, started_at=?, completed_at=? WHERE execution_id=?")) {
            ps.setString(1, execution.status().name());
            ps.setTimestamp(2, execution.startedAt() != null ? Timestamp.from(execution.startedAt()) : null);
            ps.setTimestamp(3, execution.completedAt() != null ? Timestamp.from(execution.completedAt()) : null);
            ps.setString(4, execution.id().value());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to update execution", e);
        }
    }

    private static void setParams(PreparedStatement ps, Execution exec) throws SQLException {
        ps.setString(1, exec.id().value());
        ps.setString(2, exec.graphId().value());
        ps.setString(3, exec.graphVersion().value());
        ps.setString(4, exec.planFingerprint());
        ps.setString(5, exec.status().name());
        ps.setTimestamp(6, Timestamp.from(exec.createdAt()));
        ps.setTimestamp(7, exec.startedAt() != null ? Timestamp.from(exec.startedAt()) : null);
        ps.setTimestamp(8, exec.completedAt() != null ? Timestamp.from(exec.completedAt()) : null);
    }

    private static Execution mapRow(ResultSet rs) throws SQLException {
        Timestamp startedAt = rs.getTimestamp("started_at");
        Timestamp completedAt = rs.getTimestamp("completed_at");
        return new Execution(
                new ExecutionId(rs.getString("execution_id")),
                new EntityId(rs.getString("graph_id")),
                new Version(rs.getString("graph_version")),
                rs.getString("plan_fingerprint"),
                ExecutionStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                startedAt != null ? startedAt.toInstant() : null,
                completedAt != null ? completedAt.toInstant() : null
        );
    }
}
