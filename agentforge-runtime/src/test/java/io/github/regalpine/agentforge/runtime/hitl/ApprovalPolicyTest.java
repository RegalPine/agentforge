package io.github.regalpine.agentforge.runtime.hitl;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApprovalPolicy 测试。
 */
class ApprovalPolicyTest {

    @Test
    void shouldBuildPolicy() {
        ApprovalPolicy policy =
            ApprovalPolicy.builder()
                .id("prod-write")
                .name(
                    "Production Write"
                )
                .trigger(
                    "production.write"
                )
                .minRiskLevel(
                    RiskLevel.HIGH
                )
                .requirement(
                    ApprovalRequirement
                        .singleApproval()
                )
                .timeout(
                    Duration.ofMinutes(30)
                )
                .timeoutAction(
                    TimeoutAction.REJECT
                )
                .modifiableFields(
                    Set.of("amount")
                )
                .build();

        assertEquals(
            "prod-write",
            policy.id()
        );
        assertEquals(
            RiskLevel.HIGH,
            policy.minRiskLevel()
        );
        assertTrue(
            policy.enabled()
        );
        assertEquals(
            1, policy.version()
        );
        assertEquals(
            Set.of("amount"),
            policy.modifiableFields()
        );
    }

    @Test
    void shouldRejectNullId() {
        assertThrows(
            NullPointerException.class,
            () -> ApprovalPolicy
                .builder()
                .trigger("test")
                .build()
        );
    }

    @Test
    void shouldRejectNullTrigger() {
        assertThrows(
            NullPointerException.class,
            () -> ApprovalPolicy
                .builder()
                .id("test")
                .build()
        );
    }
}
