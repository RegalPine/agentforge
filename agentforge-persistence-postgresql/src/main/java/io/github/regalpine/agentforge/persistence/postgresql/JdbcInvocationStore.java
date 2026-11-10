package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.persistence.InvocationStore;
import io.github.regalpine.agentforge.persistence.StoredInvocation;
import io.github.regalpine.agentforge.event.ExecutionId;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

/**
 * JDBC-backed {@link InvocationStore}.
 */
public final class JdbcInvocationStore implements InvocationStore {

    private final DataSource dataSource;

    public JdbcInvocationStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void create(StoredInvocation invocation) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO invocation (invocation_id, execution_id, capability_id, capability_version, " +
                     "idempotency_key, status, started_at, completed_at, result_reference) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            setParams(ps, invocation);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to create invocation", e);
        }
    }

    @Override
    public Optional<StoredInvocation> find(String invocationId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT invocation_id, execution_id, capability_id, capability_version, " +
                     "idempotency_key, status, started_at, completed_at, result_reference " +
                     "FROM invocation WHERE invocation_id = ?")) {
            ps.setString(1, invocationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find invocation", e);
        }
    }

    @Override
    public Optional<StoredInvocation> findByIdempotencyKey(String key) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT invocation_id, execution_id, capability_id, capability_version, " +
                     "idempotency_key, status, started_at, completed_at, result_reference " +
                     "FROM invocation WHERE idempotency_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find invocation by idempotency key", e);
        }
    }

    @Override
    public void update(StoredInvocation invocation) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE invocation SET status=?, completed_at=?, result_reference=? " +
                     "WHERE invocation_id=?")) {
            ps.setString(1, invocation.status());
            ps.setTimestamp(2, invocation.completedAt() != null ? Timestamp.from(invocation.completedAt()) : null);
            ps.setString(3, invocation.resultReference());
            ps.setString(4, invocation.invocationId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to update invocation", e);
        }
    }

    private static void setParams(PreparedStatement ps, StoredInvocation inv) throws SQLException {
        ps.setString(1, inv.invocationId());
        ps.setString(2, inv.executionId().value());
        ps.setString(3, inv.capabilityId().value());
        ps.setString(4, inv.capabilityVersion().value());
        ps.setString(5, inv.idempotencyKey());
        ps.setString(6, inv.status());
        ps.setTimestamp(7, inv.startedAt() != null ? Timestamp.from(inv.startedAt()) : null);
        ps.setTimestamp(8, inv.completedAt() != null ? Timestamp.from(inv.completedAt()) : null);
        ps.setString(9, inv.resultReference());
    }

    private static StoredInvocation mapRow(ResultSet rs) throws SQLException {
        Timestamp startedAt = rs.getTimestamp("started_at");
        Timestamp completedAt = rs.getTimestamp("completed_at");
        return new StoredInvocation(
                rs.getString("invocation_id"),
                new ExecutionId(rs.getString("execution_id")),
                new EntityId(rs.getString("capability_id")),
                new Version(rs.getString("capability_version")),
                rs.getString("idempotency_key"),
                rs.getString("status"),
                startedAt != null ? startedAt.toInstant() : null,
                completedAt != null ? completedAt.toInstant() : null,
                rs.getString("result_reference")
        );
    }
}
