package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.persistence.OutboxMessage;
import io.github.regalpine.agentforge.persistence.OutboxStore;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-backed {@link OutboxStore}.
 */
public final class JdbcOutboxStore implements OutboxStore {

    private final DataSource dataSource;

    public JdbcOutboxStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void append(OutboxMessage message) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO outbox_message (message_id, aggregate_id, event_type, payload, created_at, published) " +
                     "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, message.messageId());
            ps.setString(2, message.aggregateId());
            ps.setString(3, message.eventType());
            ps.setString(4, message.payload());
            ps.setTimestamp(5, Timestamp.from(message.createdAt()));
            ps.setBoolean(6, message.published());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to append outbox message", e);
        }
    }

    @Override
    public List<OutboxMessage> unpublished(int limit) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT message_id, aggregate_id, event_type, payload, created_at, published " +
                     "FROM outbox_message WHERE published = FALSE ORDER BY created_at LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<OutboxMessage> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to list unpublished outbox messages", e);
        }
    }

    @Override
    public void markPublished(String messageId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE outbox_message SET published = TRUE WHERE message_id = ?")) {
            ps.setString(1, messageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to mark outbox message as published", e);
        }
    }

    private static OutboxMessage mapRow(ResultSet rs) throws SQLException {
        return new OutboxMessage(
                rs.getString("message_id"),
                rs.getString("aggregate_id"),
                rs.getString("event_type"),
                rs.getString("payload"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("published")
        );
    }
}
