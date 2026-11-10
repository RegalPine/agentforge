package io.github.regalpine.agentforge.runtime.hitl;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RiskAssessment / RiskLevel 测试。
 */
class RiskAssessmentTest {

    @Test
    void shouldCreateBasicRisk() {
        RiskAssessment risk =
            RiskAssessment.of(
                RiskLevel.HIGH,
                "Production write"
            );
        assertEquals(
            RiskLevel.HIGH,
            risk.level()
        );
        assertEquals(
            "Production write",
            risk.reason()
        );
    }

    @Test
    void shouldCreateDetailedRisk() {
        RiskAssessment risk =
            RiskAssessment.of(
                RiskLevel.CRITICAL,
                "database",
                0.95,
                "Delete production data"
            );
        assertEquals(
            RiskLevel.CRITICAL,
            risk.level()
        );
        assertEquals(
            "database",
            risk.category()
        );
        assertEquals(
            0.95, risk.score(),
            0.001
        );
    }

    @Test
    void shouldRejectInvalidScore() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RiskAssessment(
                RiskLevel.LOW, null,
                1.5, null, null, null
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new RiskAssessment(
                RiskLevel.LOW, null,
                -0.1, null, null, null
            )
        );
    }

    @Test
    void shouldCompareRiskLevels() {
        assertTrue(
            RiskLevel.HIGH.isAtLeast(
                RiskLevel.LOW
            )
        );
        assertTrue(
            RiskLevel.HIGH.isAtLeast(
                RiskLevel.HIGH
            )
        );
        assertFalse(
            RiskLevel.LOW.isAtLeast(
                RiskLevel.HIGH
            )
        );
    }

    @Test
    void shouldRejectNullLevel() {
        assertThrows(
            NullPointerException.class,
            () -> RiskAssessment.of(
                null, "reason"
            )
        );
    }
}
