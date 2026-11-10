package io.github.regalpine.agentforge.server.persistence;

import io.github.regalpine.agentforge.security.iam.SessionStore;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

/** PostgreSQL 会话存储：撤销立即对所有实例可见，无本地 token 缓存。 */
public final class JdbcSessionStore implements SessionStore {
    private static final String TABLE = "agentforge_server.iam_session";
    private final JdbcAccess db;
    public JdbcSessionStore(DataSource source) { this.db = new JdbcAccess(source); }

    @Override public void create(SessionRecord session) {
        if (!session.tokenHash().matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Expected SHA-256 token hash");
        db.update("INSERT INTO " + TABLE + "(token_hash,user_id,tenant_id,created_at,expires_at,revoked_at) VALUES (?,?,?,?,?,?)",
            session.tokenHash(), session.userId(), session.tenantId(), session.createdAt(), session.expiresAt(), session.revokedAt());
    }
    @Override public Optional<SessionRecord> find(String hash) {
        return db.query("SELECT * FROM " + TABLE + " WHERE token_hash=?", row -> new SessionRecord(
            row.getString("token_hash"), row.getString("user_id"), row.getString("tenant_id"),
            instant(row, "created_at"), instant(row, "expires_at"), instant(row, "revoked_at")), hash).stream().findFirst();
    }
    @Override public boolean revoke(String hash, Instant now) {
        return db.update("UPDATE " + TABLE + " SET revoked_at=? WHERE token_hash=? AND revoked_at IS NULL", now, hash) == 1;
    }
    @Override public int revokeAll(String userId, Instant now) {
        return db.update("UPDATE " + TABLE + " SET revoked_at=? WHERE user_id=? AND revoked_at IS NULL AND expires_at>?", now, userId, now);
    }
    @Override public int countActive(Instant now) {
        return db.query("SELECT COUNT(*) FROM " + TABLE + " WHERE revoked_at IS NULL AND expires_at>?", row -> row.getInt(1), now).get(0);
    }
    private static Instant instant(ResultSet row, String column) throws SQLException {
        OffsetDateTime value = row.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
