package io.github.regalpine.agentforge.capability.jdbc;

import io.github.regalpine.agentforge.capability.SideEffectClassification;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database adapter security tests.
 * AF-ENHANCE-02 §14: verifies that
 * arbitrary SQL is rejected and only
 * parameterised statements are allowed.
 */
class JdbcSecurityTest {

    @Test
    void shouldAcceptParameterisedSql() {
        var desc = new DatabaseCapabilityDescriptor(
            "db.query.user",
            "Find user by ID",
            "Query user by ID",
            "users",
            DatabaseOperation.SELECT,
            "SELECT * FROM users WHERE id = ?",
            List.of("id"),
            Set.of("id", "name", "email"),
            SideEffectClassification.READ_ONLY,
            100,
            Map.of()
        );

        assertEquals(
            "SELECT * FROM users WHERE id = ?",
            desc.parameterisedSql()
        );

        assertEquals(
            DatabaseOperation.SELECT,
            desc.operation()
        );
    }

    @Test
    void shouldRejectRawStringLiteralSql() {
        // SQL with raw string literal and
        // no ? placeholder should be rejected
        assertThrows(
            IllegalArgumentException.class,
            () -> new DatabaseCapabilityDescriptor(
                "db.bad",
                "Bad query",
                "SQL injection risk",
                "users",
                DatabaseOperation.SELECT,
                "SELECT * FROM users WHERE name = 'admin'",
                List.of(),
                Set.of(),
                null,
                100,
                Map.of()
            )
        );
    }

    @Test
    void shouldDefaultToReadOnly() {
        var desc = new DatabaseCapabilityDescriptor(
            "db.q1",
            "Query",
            "A query",
            "t1",
            DatabaseOperation.SELECT,
            "SELECT 1",
            List.of(),
            Set.of(),
            null,
            100,
            Map.of()
        );

        assertEquals(
            SideEffectClassification.READ_ONLY,
            desc.sideEffect()
        );
    }

    @Test
    void shouldDefaultMaxRows() {
        var desc = new DatabaseCapabilityDescriptor(
            "db.q2",
            "Query",
            "A query",
            "t1",
            DatabaseOperation.SELECT,
            "SELECT 1",
            List.of(),
            Set.of(),
            null,
            0,
            Map.of()
        );

        assertEquals(1000, desc.maxRows());
    }

    @Test
    void shouldHaveAllOperations() {
        assertEquals(
            5,
            DatabaseOperation.values().length
        );
    }
}
