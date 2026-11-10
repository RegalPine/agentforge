package io.github.regalpine.agentforge.server.health;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.Properties;

/**
 * G4-2: Health check for the database dependency.
 *
 * <p>Attempts a lightweight JDBC connection and
 * executes {@code SELECT 1} to verify the database
 * is reachable. Used only in production mode when
 * a real database is configured.
 */
public final class DatabaseHealthCheck implements HealthCheck {

    private final String url;
    private final String username;
    private final String password;
    private final Duration connectTimeout;
    private final Duration queryTimeout;

    public DatabaseHealthCheck(
        String url,
        String username,
        String password
    ) {
        this(url, username, password, Duration.ofSeconds(3), Duration.ofSeconds(5));
    }

    public DatabaseHealthCheck(
        String url,
        String username,
        String password,
        Duration connectTimeout,
        Duration queryTimeout
    ) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.connectTimeout = connectTimeout;
        this.queryTimeout = queryTimeout;
    }

    @Override
    public HealthStatus check() {
        if (url == null || url.isBlank()) {
            return HealthStatus.unhealthy(
                "database",
                "No database URL configured"
            );
        }

        try {
            Properties props = new Properties();
            if (username != null) props.setProperty("user", username);
            if (password != null) props.setProperty("password", password);
            props.setProperty("loginTimeout", String.valueOf(connectTimeout.toSeconds()));

            try (Connection conn = DriverManager.getConnection(url, props)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.setQueryTimeout((int) queryTimeout.toSeconds());
                    try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
                        if (rs.next()) {
                            return HealthStatus.healthy("database");
                        }
                    }
                }
            }
            return HealthStatus.unhealthy(
                "database",
                "Query returned no result"
            );
        } catch (Exception e) {
            return HealthStatus.unhealthy(
                "database",
                e.getMessage()
            );
        }
    }
}
