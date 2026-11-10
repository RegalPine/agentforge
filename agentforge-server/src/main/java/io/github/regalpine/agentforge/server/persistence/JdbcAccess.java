package io.github.regalpine.agentforge.server.persistence;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;

/** 同一事务内复用同一连接；连接作用域不会跨线程或泄漏到回调外。 */
public final class JdbcAccess {
    @FunctionalInterface public interface SqlFunction<T> { T apply(Connection connection) throws SQLException; }
    @FunctionalInterface public interface RowMapper<T> { T map(ResultSet row) throws SQLException; }
    private final DataSource source;
    private final Connection bound;

    public JdbcAccess(DataSource source) { this(source, null); }
    private JdbcAccess(DataSource source, Connection bound) {
        this.source = Objects.requireNonNull(source);
        this.bound = bound;
    }

    public <T> T transaction(Function<JdbcAccess, T> work) {
        if (bound != null) return work.apply(this);
        return connection(connection -> {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(new JdbcAccess(source, connection));
                connection.commit();
                return result;
            } catch (RuntimeException | Error error) {
                try { connection.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                throw error;
            } catch (SQLException error) {
                try { connection.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                throw error;
            }
        });
    }

    public <T> T connection(SqlFunction<T> work) {
        try {
            if (bound != null) return work.apply(bound);
            try (Connection connection = source.getConnection()) { return work.apply(connection); }
        } catch (SQLException error) {
            throw new IllegalStateException("Database operation failed", error);
        }
    }

    public int update(String sql, Object... parameters) {
        return connection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                return statement.executeUpdate();
            }
        });
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
        return connection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet rows = statement.executeQuery()) {
                    List<T> result = new ArrayList<>();
                    while (rows.next()) result.add(mapper.map(rows));
                    return result;
                }
            }
        });
    }

    private static void bind(PreparedStatement statement, Object[] parameters) throws SQLException {
        statement.setQueryTimeout(5);
        for (int i = 0; i < parameters.length; i++) {
            Object value = parameters[i];
            statement.setObject(i + 1, value instanceof Instant instant ? instant.atOffset(ZoneOffset.UTC) : value);
        }
    }
}
