package io.github.regalpine.agentforge.runtime.hitl;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ContextRedactor 脱敏测试。
 */
class ContextRedactorTest {

    @Test
    void shouldRedactField() {
        var redactor =
            new ContextRedactor(
                Map.of(
                    "apiToken",
                    RedactionPolicy.REDACT
                )
            );

        var result = redactor.redact(
            Map.of(
                "apiToken",
                "secret-123",
                "name", "test"
            )
        );

        assertEquals(
            "[REDACTED]",
            result.get("apiToken")
        );
        assertEquals(
            "test",
            result.get("name")
        );
    }

    @Test
    void shouldMaskField() {
        var redactor =
            new ContextRedactor(
                Map.of(
                    "ssn",
                    RedactionPolicy.MASK
                )
            );

        var result = redactor.redact(
            Map.of("ssn",
                "123-45-6789"
            )
        );

        String masked = (String)
            result.get("ssn");
        assertTrue(
            masked.startsWith("*")
        );
        assertFalse(
            masked.contains("123")
        );
    }

    @Test
    void shouldOmitField() {
        var redactor =
            new ContextRedactor(
                Map.of(
                    "password",
                    RedactionPolicy.OMIT
                )
            );

        var result = redactor.redact(
            Map.of(
                "password", "secret",
                "user", "admin"
            )
        );

        assertFalse(
            result.containsKey(
                "password"
            )
        );
        assertEquals(
            "admin",
            result.get("user")
        );
    }

    @Test
    void shouldHashField() {
        var redactor =
            new ContextRedactor(
                Map.of(
                    "data",
                    RedactionPolicy.HASH
                )
            );

        var result = redactor.redact(
            Map.of("data", "value")
        );

        String hashed = (String)
            result.get("data");
        assertEquals(16,
            hashed.length()
        );
    }

    @Test
    void shouldSummarizeField() {
        var redactor =
            new ContextRedactor(
                Map.of(
                    "doc",
                    RedactionPolicy.SUMMARY
                )
            );

        String longText =
            "A".repeat(50);
        var result = redactor.redact(
            Map.of("doc", longText)
        );

        String summary = (String)
            result.get("doc");
        assertTrue(
            summary.contains("50 chars")
        );
    }

    @Test
    void shouldHandleNullContext() {
        var redactor =
            new ContextRedactor(
                Map.of()
            );
        var result =
            redactor.redact(null);
        assertTrue(
            result.isEmpty()
        );
    }
}
