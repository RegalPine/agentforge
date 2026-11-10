package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.persistence.*;
import io.github.regalpine.agentforge.runtime.CommitPort;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Factory for creating all persistence store implementations backed by JDBC.
 * Provides a single entry point for wiring up the complete persistence layer.
 */
public final class PostgresPersistenceModule {

    private final DataSource dataSource;
    private final ExecutionStateStore executionStateStore;
    private final EventStore eventStore;
    private final CheckpointStore checkpointStore;
    private final SnapshotStore snapshotStore;
    private final OutboxStore outboxStore;
    private final InvocationStore invocationStore;
    private final ExecutionStore executionStore;
    private final CommitPort commitPort;

    private PostgresPersistenceModule(DataSource dataSource) {
        this.dataSource = dataSource;
        this.executionStateStore = new JdbcExecutionStateStore(dataSource);
        this.eventStore = new JdbcEventStore(dataSource);
        this.checkpointStore = new JdbcCheckpointStore(dataSource);
        this.snapshotStore = new JdbcSnapshotStore(dataSource);
        this.outboxStore = new JdbcOutboxStore(dataSource);
        this.invocationStore = new JdbcInvocationStore(dataSource);
        this.executionStore = new JdbcExecutionStore(dataSource);
        this.commitPort = new JdbcCommitPort(dataSource);
    }

    /**
     * Create and initialize the persistence module.
     * Runs Flyway schema migration and returns a fully wired module.
     */
    public static PostgresPersistenceModule create(DataSource dataSource) {
        runFlywayMigration(dataSource);
        return new PostgresPersistenceModule(dataSource);
    }

    /**
     * Run Flyway migration using scripts from {@code db/migration/}.
     */
    private static void runFlywayMigration(DataSource dataSource) {
        try {
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
            flyway.migrate();
        } catch (Exception e) {
            throw new PersistenceException("Flyway schema migration failed", e);
        }
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public ExecutionStateStore executionStateStore() {
        return executionStateStore;
    }

    public EventStore eventStore() {
        return eventStore;
    }

    public CheckpointStore checkpointStore() {
        return checkpointStore;
    }

    public SnapshotStore snapshotStore() {
        return snapshotStore;
    }

    public OutboxStore outboxStore() {
        return outboxStore;
    }

    public InvocationStore invocationStore() {
        return invocationStore;
    }

    public ExecutionStore executionStore() {
        return executionStore;
    }

    public CommitPort commitPort() {
        return commitPort;
    }
}
