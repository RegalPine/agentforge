package io.github.regalpine.agentforge.server.health;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

/**
 * Batch 6: Redis connectivity health check.
 * <p>
 * Verifies that a Redis instance is reachable and responds to a PING command.
 * Uses a simple TCP connect + PING to avoid requiring a Redis client library
 * in the server module.
 */
public final class RedisHealthCheck implements HealthCheck {

    private final String host;
    private final int port;
    private final Duration connectTimeout;

    public RedisHealthCheck(String host, int port) {
        this(host, port, Duration.ofSeconds(3));
    }

    public RedisHealthCheck(String host, int port, Duration connectTimeout) {
        this.host = host;
        this.port = port;
        this.connectTimeout = connectTimeout;
    }

    /**
     * Create from a Redis URI (redis://host:port).
     */
    public static RedisHealthCheck fromUri(String redisUri) {
        if (redisUri == null || redisUri.isBlank()) {
            return new RedisHealthCheck("localhost", 6379);
        }
        String stripped = redisUri.replace("redis://", "");
        String[] parts = stripped.split(":", 2);
        String h = parts[0].isEmpty() ? "localhost" : parts[0];
        int p = parts.length > 1 ? Integer.parseInt(parts[1].split("/")[0]) : 6379;
        return new RedisHealthCheck(h, p);
    }

    @Override
    public HealthStatus check() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port),
                (int) connectTimeout.toMillis());
            // Send PING and verify PONG
            var os = socket.getOutputStream();
            os.write("PING\r\n".getBytes());
            os.flush();
            socket.setSoTimeout((int) connectTimeout.toMillis());
            var is = socket.getInputStream();
            byte[] buf = new byte[64];
            int read = is.read(buf);
            if (read > 0) {
                String response = new String(buf, 0, read).trim();
                if (response.startsWith("+PONG")) {
                    return HealthStatus.healthy("redis");
                }
                return HealthStatus.unhealthy("redis",
                    "Unexpected response: " + response);
            }
            return HealthStatus.unhealthy("redis", "No response from PING");
        } catch (Exception e) {
            return HealthStatus.unhealthy("redis", e.getMessage());
        }
    }
}
