package io.github.regalpine.agentforge.fault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class FaultInjectorTest {

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
    void shouldNotTriggerWhenDisabled() {
        registry.disable();
        FaultPoint fp = new FaultPoint("test.fp", "test");
        FaultRule rule = FaultRule.builder(fp).type(FaultType.EXCEPTION).build();
        registry.activateRule(rule);

        assertFalse(injector.check(fp));
    }

    @Test
    void shouldNotTriggerWhenNoRule() {
        FaultPoint fp = new FaultPoint("test.fp", "test");
        assertFalse(injector.check(fp));
    }

    @Test
    void shouldThrowExceptionWhenRuleIsActive() {
        FaultPoint fp = new FaultPoint("test.fp", "test");
        FaultRule rule = FaultRule.builder(fp)
                .type(FaultType.EXCEPTION)
                .exceptionMessage("boom")
                .build();
        registry.activateRule(rule);

        FaultException ex = assertThrows(FaultException.class, () -> injector.check(fp));
        assertEquals(FaultType.EXCEPTION, ex.faultType());
        assertTrue(ex.getMessage().contains("boom"));
    }

    @Test
    void shouldApplyDelay() {
        FaultPoint fp = new FaultPoint("test.delay", "delay test");
        FaultRule rule = FaultRule.builder(fp)
                .type(FaultType.DELAY)
                .delay(Duration.ofMillis(50))
                .build();
        registry.activateRule(rule);

        long start = System.currentTimeMillis();
        assertTrue(injector.check(fp));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 40, "Delay should be at least 40ms, was " + elapsed);
    }

    @Test
    void shouldRespectProbability() {
        FaultPoint fp = new FaultPoint("test.prob", "probability test");
        FaultRule rule = FaultRule.builder(fp)
                .type(FaultType.EXCEPTION)
                .probability(0.0)
                .build();
        registry.activateRule(rule);

        assertFalse(injector.check(fp));
    }

    @Test
    void checkAndThrowShouldThrowForExceptionType() {
        FaultPoint fp = new FaultPoint("test.throw", "throw test");
        FaultRule rule = FaultRule.builder(fp)
                .type(FaultType.EXCEPTION)
                .exceptionMessage("explicit throw")
                .build();
        registry.activateRule(rule);

        FaultException ex = assertThrows(FaultException.class, () -> injector.checkAndThrow(fp));
        assertTrue(ex.getMessage().contains("explicit throw"));
    }

    @Test
    void shouldDeactivateRule() {
        FaultPoint fp = new FaultPoint("test.deactivate", "deactivate test");
        FaultRule rule = FaultRule.builder(fp).type(FaultType.EXCEPTION).build();
        registry.activateRule(rule);
        assertTrue(registry.hasActiveRule(fp.id()));

        registry.deactivateRule(fp.id());
        assertFalse(registry.hasActiveRule(fp.id()));
        assertFalse(injector.check(fp));
    }

    @Test
    void shouldDeactivateAllRules() {
        FaultPoint fp1 = new FaultPoint("test.1", "fp1");
        FaultPoint fp2 = new FaultPoint("test.2", "fp2");
        registry.activateRule(FaultRule.builder(fp1).type(FaultType.EXCEPTION).build());
        registry.activateRule(FaultRule.builder(fp2).type(FaultType.EXCEPTION).build());

        registry.deactivateAllRules();
        assertFalse(registry.hasActiveRule(fp1.id()));
        assertFalse(registry.hasActiveRule(fp2.id()));
    }

    @Test
    void faultRuleBuilderShouldValidateProbability() {
        FaultPoint fp = new FaultPoint("test.valid", "validation");
        assertThrows(IllegalArgumentException.class, () ->
                FaultRule.builder(fp).probability(-0.1));
        assertThrows(IllegalArgumentException.class, () ->
                FaultRule.builder(fp).probability(1.1));
    }

    @Test
    void faultPointEqualityShouldBeById() {
        FaultPoint fp1 = new FaultPoint("same.id", "desc1");
        FaultPoint fp2 = new FaultPoint("same.id", "desc2");
        assertEquals(fp1, fp2);
        assertEquals(fp1.hashCode(), fp2.hashCode());
    }
}
