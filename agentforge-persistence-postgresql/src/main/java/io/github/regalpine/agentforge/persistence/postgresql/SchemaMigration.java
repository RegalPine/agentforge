package io.github.regalpine.agentforge.persistence.postgresql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DDL schema migration for AgentForge PostgreSQL persistence.
 * Creates all required tables if they do not exist.
 *
 * @deprecated Use Flyway-based migration via {@link PostgresPersistenceModule#create(javax.sql.DataSource)}.
 *             This class is retained for backward compatibility and will be removed in a future release.
 *             Migration scripts are now managed in {@code src/main/resources/db/migration/}.
 */
@Deprecated
public final class SchemaMigration {

    private SchemaMigration() {
    }

    public static void migrate(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // ── Execution State ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS execution_state (
                    execution_id  VARCHAR(64)  NOT NULL PRIMARY KEY,
                    status        VARCHAR(32)  NOT NULL,
                    current_node  VARCHAR(64),
                    data          TEXT         NOT NULL DEFAULT '{}',
                    completed_nodes TEXT       NOT NULL DEFAULT '[]',
                    waiting_nodes TEXT         NOT NULL DEFAULT '[]',
                    version       BIGINT       NOT NULL DEFAULT 0,
                    next_event_sequence BIGINT NOT NULL DEFAULT 0,
                    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── Execution ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS execution (
                    execution_id   VARCHAR(64)  NOT NULL PRIMARY KEY,
                    graph_id       VARCHAR(64)  NOT NULL,
                    graph_version  VARCHAR(32)  NOT NULL,
                    plan_fingerprint VARCHAR(128),
                    status         VARCHAR(32)  NOT NULL,
                    created_at     TIMESTAMP    NOT NULL,
                    started_at     TIMESTAMP,
                    completed_at   TIMESTAMP
                )
            """);

            // ── Event Store ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS execution_event (
                    execution_id  VARCHAR(64)  NOT NULL,
                    sequence      BIGINT       NOT NULL,
                    timestamp     TIMESTAMP    NOT NULL,
                    type          VARCHAR(64)  NOT NULL,
                    payload       TEXT         NOT NULL DEFAULT '{}',
                    PRIMARY KEY (execution_id, sequence)
                )
            """);

            // ── Checkpoint Store ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS checkpoint (
                    checkpoint_id    VARCHAR(64)  NOT NULL PRIMARY KEY,
                    execution_id     VARCHAR(64)  NOT NULL,
                    sequence         BIGINT       NOT NULL DEFAULT 0,
                    state_version    BIGINT       NOT NULL,
                    current_node     VARCHAR(64),
                    status           VARCHAR(32),
                    data             TEXT         NOT NULL DEFAULT '{}',
                    state_fingerprint VARCHAR(128),
                    created_at       TIMESTAMP    NOT NULL
                )
            """);

            // ── Snapshot Store ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS snapshot (
                    snapshot_id      VARCHAR(64)  NOT NULL PRIMARY KEY,
                    execution_id     VARCHAR(64)  NOT NULL,
                    event_sequence   BIGINT       NOT NULL,
                    state_version    BIGINT       NOT NULL,
                    current_node_id  VARCHAR(64),
                    status           VARCHAR(32),
                    state            TEXT         NOT NULL DEFAULT '{}',
                    state_fingerprint VARCHAR(128),
                    created_at       TIMESTAMP    NOT NULL
                )
            """);

            // ── Outbox Store ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS outbox_message (
                    message_id    VARCHAR(64)  NOT NULL PRIMARY KEY,
                    aggregate_id  VARCHAR(64)  NOT NULL,
                    event_type    VARCHAR(64)  NOT NULL,
                    payload       TEXT         NOT NULL,
                    created_at    TIMESTAMP    NOT NULL,
                    published     BOOLEAN      NOT NULL DEFAULT FALSE
                )
            """);

            // ── Invocation Store ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invocation (
                    invocation_id     VARCHAR(64)  NOT NULL PRIMARY KEY,
                    execution_id      VARCHAR(64)  NOT NULL,
                    capability_id     VARCHAR(64)  NOT NULL,
                    capability_version VARCHAR(32) NOT NULL,
                    idempotency_key   VARCHAR(128) NOT NULL,
                    status            VARCHAR(32)  NOT NULL,
                    started_at        TIMESTAMP,
                    completed_at      TIMESTAMP,
                    result_reference  VARCHAR(256)
                )
            """);

            // ── Indexes ──
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_event_execution
                ON execution_event (execution_id)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_checkpoint_execution
                ON checkpoint (execution_id)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_snapshot_execution
                ON snapshot (execution_id)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
                ON outbox_message (published, created_at)
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_invocation_idempotency
                ON invocation (idempotency_key)
            """);
        }
    }
}
