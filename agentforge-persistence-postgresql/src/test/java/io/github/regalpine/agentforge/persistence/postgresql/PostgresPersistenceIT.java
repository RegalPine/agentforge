package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionEvent;

import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 真实 PostgreSQL 迁移和提交契约；缺少 Docker 必须失败，不能静默跳过。 */
@Tag("integration")
class PostgresPersistenceIT {
    @Test
    void migrationsAndCommitSurviveAdapterRecreation() {
        try (var postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();
            var source = new PGSimpleDataSource();
            source.setURL(postgres.getJdbcUrl());
            source.setUser(postgres.getUsername());
            source.setPassword(postgres.getPassword());
            var first = PostgresPersistenceModule.create(source);
            var id = new ExecutionId("postgres-contract");
            first.executionStateStore().create(new ExecutionState(
                id, ExecutionStatus.CREATED, null, Map.of(), Set.of(), Set.of(), 0));
            var running = new ExecutionState(
                id, ExecutionStatus.RUNNING, null, Map.of("value", "persisted"),
                Set.of(), Set.of(), 1);
            var event = new ExecutionEvent(id, 1, Instant.now(), "STARTED", Map.of());
            assertTrue(first.commitPort().commit(new CommitPort.CommitCommand(running, event, 0)));

            var second = PostgresPersistenceModule.create(source);
            assertEquals(1, second.executionStateStore().find(id).orElseThrow().version());
            assertEquals("persisted", second.executionStateStore().find(id).orElseThrow().data().get("value"));
            assertEquals(1, second.eventStore().list(id).size());
            assertFalse(second.commitPort().commit(new CommitPort.CommitCommand(running, event, 0)));
            assertEquals(1, second.eventStore().list(id).size());
        }
    }
}
