package io.github.regalpine.agentforge.hardening;

import io.github.regalpine.agentforge.fault.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AF-HARDEN-009: Fault injection integration
 * Tests that the fault injection framework correctly integrates
 * with the defined fault points.
 */
class FaultInjectionIntegrationTest {

    private FaultRegistry registry;
    private FaultInjector injector;

    @BeforeEach
    void setUp() {
        registry = new FaultRegistry();
        injector = new FaultInjector(registry);
        registry.enable();
    }

    @AfterEach
    void tearDown() {
        registry.reset();
    }

    @Test
    void shouldInjectFaultAtCompilerCompilePoint() {
        FaultPoint fp = FaultPoints.COMPILER_COMPILE;
        registry.registerFaultPoint(fp);

        FaultRule rule = FaultRule.builder(fp)
            .type(FaultType.EXCEPTION)
            .exceptionMessage("Compiler failure injected")
            .build();
        registry.activateRule(rule);

        FaultException ex = assertThrows(FaultException.class,
            () -> injector.checkAndThrow(fp));

        assertEquals(FaultType.EXCEPTION, ex.faultType());
        assertEquals(fp, ex.faultPoint());
        assertTrue(ex.getMessage().contains("Compiler failure injected"));
    }

    @Test
    void shouldInjectDelayAtNodeExecutionPoint() {
        FaultPoint fp = FaultPoints.RUNTIME_NODE_EXECUTE;
        registry.registerFaultPoint(fp);

        FaultRule rule = FaultRule.builder(fp)
            .type(FaultType.DELAY)
            .delay(Duration.ofMillis(30))
            .build();
        registry.activateRule(rule);

        long start = System.currentTimeMillis();
        boolean triggered = injector.check(fp);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(triggered);
        assertTrue(elapsed >= 20, "Delay should be at least 20ms");
    }

    @Test
    void shouldInjectFaultAtPersistenceSavePoint() {
        FaultPoint fp = FaultPoints.PERSISTENCE_SAVE_STATE;
        registry.registerFaultPoint(fp);

        FaultRule rule = FaultRule.builder(fp)
            .type(FaultType.FAILURE)
            .exceptionMessage("Persistence write failure")
            .build();
        registry.activateRule(rule);

        assertThrows(FaultException.class, () -> injector.check(fp));
    }

    @Test
    void shouldInjectTimeoutAtHttpProviderPoint() {
        FaultPoint fp = FaultPoints.HTTP_PROVIDER_REQUEST;
        registry.registerFaultPoint(fp);

        FaultRule rule = FaultRule.builder(fp)
            .type(FaultType.TIMEOUT)
            .delay(Duration.ofMillis(50))
            .build();
        registry.activateRule(rule);

        long start = System.currentTimeMillis();
        injector.check(fp);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 40, "Timeout simulation should delay at least 40ms");
    }

    @Test
    void shouldNotInjectWhenDisabled() {
        registry.disable();

        FaultPoint fp = FaultPoints.RUNTIME_START;
        registry.registerFaultPoint(fp);

        FaultRule rule = FaultRule.builder(fp)
            .type(FaultType.EXCEPTION)
            .build();
        registry.activateRule(rule);

        assertFalse(injector.check(fp), "No fault should trigger when disabled");
    }

    @Test
    void shouldDeactivateRuleAndResumeNormalOperation() {
        FaultPoint fp = FaultPoints.PROVIDER_CALL;
        registry.registerFaultPoint(fp);

        FaultRule rule = FaultRule.builder(fp)
            .type(FaultType.EXCEPTION)
            .build();
        registry.activateRule(rule);

        assertThrows(FaultException.class, () -> injector.check(fp));

        registry.deactivateRule(fp.id());

        assertFalse(injector.check(fp), "After deactivation, no fault should trigger");
    }

    @Test
    void shouldSupportMultipleSimultaneousFaultRules() {
        FaultPoint fp1 = FaultPoints.COMPILER_COMPILE;
        FaultPoint fp2 = FaultPoints.RUNTIME_NODE_EXECUTE;

        registry.activateRule(FaultRule.builder(fp1)
            .type(FaultType.EXCEPTION)
            .exceptionMessage("compile fail")
            .build());

        registry.activateRule(FaultRule.builder(fp2)
            .type(FaultType.DELAY)
            .delay(Duration.ofMillis(10))
            .build());

        assertThrows(FaultException.class, () -> injector.check(fp1));

        long start = System.currentTimeMillis();
        boolean triggered = injector.check(fp2);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(triggered);
        assertTrue(elapsed >= 5);
    }

    @Test
    void shouldVerifyAllPredefinedFaultPoints() {
        assertNotNull(FaultPoints.COMPILER_COMPILE);
        assertNotNull(FaultPoints.COMPILER_TOPOLOGICAL_SORT);
        assertNotNull(FaultPoints.RUNTIME_START);
        assertNotNull(FaultPoints.RUNTIME_NODE_EXECUTE);
        assertNotNull(FaultPoints.RUNTIME_SCHEDULER_ACQUIRE_LEASE);
        assertNotNull(FaultPoints.RUNTIME_SCHEDULER_HEARTBEAT);
        assertNotNull(FaultPoints.PERSISTENCE_SAVE_STATE);
        assertNotNull(FaultPoints.PERSISTENCE_LOAD_STATE);
        assertNotNull(FaultPoints.PERSISTENCE_APPEND_EVENT);
        assertNotNull(FaultPoints.PERSISTENCE_CHECKPOINT);
        assertNotNull(FaultPoints.PROVIDER_CALL);
        assertNotNull(FaultPoints.HTTP_PROVIDER_REQUEST);
        assertNotNull(FaultPoints.SECURITY_TOKEN_VALIDATE);
        assertNotNull(FaultPoints.SECURITY_AUTHORIZATION_CHECK);
    }
}
