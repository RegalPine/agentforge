package io.github.regalpine.agentforge.distributed.jdbc;

import io.github.regalpine.agentforge.distributed.lease.ExecutionLease;
import io.github.regalpine.agentforge.distributed.lease.LeaseStore;
import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.event.ExecutionId;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PostgreSQL 租约存储：acquire/renew/release 全部条件更新，使用数据库时间。
 * 默认租期 30 秒、10 秒续约；失租后停止推进；提交端再次验证 token。
 */
public final class JdbcLeaseStore implements LeaseStore {
    private static final String TABLE = "agentforge_distributed.execution_lease";
    private static final long DEFAULT_LEASE_MS = 30_000L;
    private static final long RENEW_LEASE_MS = 10_000L;
    private final DataSource source;
    private final AtomicLong tokenSequence;

    public JdbcLeaseStore(DataSource source) {
        this.source = source;
        this.tokenSequence = initTokenSequence(source);
    }

    private static AtomicLong initTokenSequence(DataSource source) {
        try (Connection c = source.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COALESCE(MAX(fencing_token), 0) FROM " + TABLE)) {
            rs.next();
            return new AtomicLong(rs.getLong(1));
        } catch (SQLException e) {
            return new AtomicLong(0);
        }
    }

    @Override
    public Optional<ExecutionLease> acquire(ExecutionId executionId, WorkerId workerId, long durationMillis) {
        long token = tokenSequence.incrementAndGet();
        long leaseMs = durationMillis > 0 ? durationMillis : DEFAULT_LEASE_MS;
        try (Connection c = source.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 条件插入：仅当无有效租约时
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO " + TABLE + " (execution_id, worker_id, fencing_token, acquired_at, expires_at) " +
                        "SELECT ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + (? * interval '1 millisecond') " +
                        "WHERE NOT EXISTS (SELECT 1 FROM " + TABLE + " WHERE execution_id=? AND expires_at > CURRENT_TIMESTAMP)")) {
                    ps.setString(1, executionId.value());
                    ps.setString(2, workerId.value());
                    ps.setLong(3, token);
                    ps.setLong(4, leaseMs);
                    ps.setString(5, executionId.value());
                    if (ps.executeUpdate() == 0) {
                        c.rollback();
                        return current(executionId);
                    }
                }
                c.commit();
                return current(executionId);
            } catch (SQLException e) {
                try { c.rollback(); } catch (SQLException suppressed) { e.addSuppressed(suppressed); }
                throw new IllegalStateException("Lease acquire failed", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lease acquire connection failed", e);
        }
    }

    @Override
    public boolean renew(ExecutionId executionId, WorkerId workerId, long fencingToken, long durationMillis) {
        long leaseMs = durationMillis > 0 ? durationMillis : RENEW_LEASE_MS;
        try (Connection c = source.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE " + TABLE + " SET expires_at = CURRENT_TIMESTAMP + (? * interval '1 millisecond') " +
                 "WHERE execution_id=? AND worker_id=? AND fencing_token=? AND expires_at > CURRENT_TIMESTAMP")) {
            ps.setLong(1, leaseMs);
            ps.setString(2, executionId.value());
            ps.setString(3, workerId.value());
            ps.setLong(4, fencingToken);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Lease renew failed", e);
        }
    }

    @Override
    public boolean release(ExecutionId executionId, WorkerId workerId, long fencingToken) {
        try (Connection c = source.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE FROM " + TABLE + " WHERE execution_id=? AND worker_id=? AND fencing_token=?")) {
            ps.setString(1, executionId.value());
            ps.setString(2, workerId.value());
            ps.setLong(3, fencingToken);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Lease release failed", e);
        }
    }

    @Override
    public Optional<ExecutionLease> current(ExecutionId executionId) {
        try (Connection c = source.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT worker_id, fencing_token, acquired_at, expires_at FROM " + TABLE +
                 " WHERE execution_id=? AND expires_at > CURRENT_TIMESTAMP")) {
            ps.setString(1, executionId.value());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new ExecutionLease(
                    executionId,
                    new WorkerId(rs.getString("worker_id")),
                    rs.getLong("fencing_token"),
                    instant(rs, "acquired_at"),
                    instant(rs, "expires_at")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Lease query failed", e);
        }
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime odt = rs.getObject(column, OffsetDateTime.class);
        return odt == null ? Instant.EPOCH : odt.toInstant();
    }
}
