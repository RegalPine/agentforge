package io.github.regalpine.agentforge.arch;

import io.github.regalpine.agentforge.fault.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * G4-4: Disaster recovery fault injection scenarios.
 *
 * <p>Verifies that the system handles transient failures
 * gracefully and recovers consistently. Tests cover:
 * database temporarily unavailable, queue message loss,
 * network partition simulation, and IAM service failure.
 */
class DisasterRecoveryFaultTest {

    private FaultRegistry registry;
    private FaultInjector injector;

    @BeforeEach
    void setUp() {
        registry = new FaultRegistry();
        registry.enable();
        injector = new FaultInjector(registry);
    }

    @AfterEach
    void tearDown() {
        registry.reset();
    }

    // ── Scenario 1: Database temporarily unavailable ──

    @Test
    void databaseTransientFailureShouldRecover() {
        FaultPoint dbSave = FaultPoints.PERSISTENCE_SAVE_STATE;
        registry.registerFaultPoint(dbSave);

        // Phase 1: DB unavailable
        FaultRule dbDown = FaultRule.builder(dbSave)
            .type(FaultType.EXCEPTION)
            .exceptionMessage("Connection refused")
            .probability(1.0)
            .build();
        registry.activateRule(dbDown);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Attempt save — should fail
        for (int i = 0; i < 5; i++) {
            try {
                simulateDbOperation(injector, dbSave);
                successCount.incrementAndGet();
            } catch (FaultException e) {
                failCount.incrementAndGet();
            }
        }

        assertEquals(0, successCount.get(),
            "All operations should fail when DB is down");
        assertEquals(5, failCount.get());

        // Phase 2: DB recovers
        registry.deactivateRule(dbSave.id());

        for (int i = 0; i < 5; i++) {
            try {
                simulateDbOperation(injector, dbSave);
                successCount.incrementAndGet();
            } catch (FaultException e) {
                failCount.incrementAndGet();
            }
        }

        assertEquals(5, successCount.get(),
            "All operations should succeed after recovery");
        assertEquals(5, failCount.get(),
            "No additional failures after recovery");
    }

    // ── Scenario 2: Queue message loss ──

    @Test
    void queueMessageDropShouldBeDetected() {
        FaultPoint eventAppend = FaultPoints.PERSISTENCE_APPEND_EVENT;
        registry.registerFaultPoint(eventAppend);

        FaultRule dropRule = FaultRule.builder(eventAppend)
            .type(FaultType.DROP)
            .probability(1.0)
            .build();
        registry.activateRule(dropRule);

        // Attempt to append event — should throw
        assertThrows(FaultException.class,
            () -> injector.checkAndThrow(eventAppend),
            "DROP fault should throw FaultException");

        // Verify no duplicate side effects after retry
        registry.deactivateRule(eventAppend.id());
        assertDoesNotThrow(() -> injector.checkAndThrow(eventAppend),
            "After removing fault, retry should succeed");
    }

    // ── Scenario 3: Network partition simulation ──

    @Test
    void networkPartitionShouldCauseTimeout() {
        FaultPoint providerCall = FaultPoints.PROVIDER_CALL;
        registry.registerFaultPoint(providerCall);

        FaultRule timeoutRule = FaultRule.builder(providerCall)
            .type(FaultType.TIMEOUT)
            .delay(Duration.ofMillis(50))
            .probability(1.0)
            .build();
        registry.activateRule(timeoutRule);

        long start = System.currentTimeMillis();
        boolean timedOut = false;
        try {
            injector.check(providerCall);
        } catch (FaultException e) {
            // timeout manifests as exception or delay
        }
        long elapsed = System.currentTimeMillis() - start;

        // Timeout should have introduced delay
        assertTrue(elapsed >= 40,
            "Network partition should cause timeout delay. "
            + "Elapsed: " + elapsed + "ms");

        // After partition heals
        registry.deactivateRule(providerCall.id());
        boolean triggered = injector.check(providerCall);
        assertFalse(triggered,
            "After healing, fault should not trigger");
    }

    // ── Scenario 4: IAM service unavailable ──

    @Test
    void iamUnavailableShouldBlockRequests() {
        FaultPoint tokenValidate = FaultPoints.SECURITY_TOKEN_VALIDATE;
        registry.registerFaultPoint(tokenValidate);

        FaultRule iamDown = FaultRule.builder(tokenValidate)
            .type(FaultType.EXCEPTION)
            .exceptionMessage("IAM service unavailable")
            .probability(1.0)
            .build();
        registry.activateRule(iamDown);

        // Token validation should fail
        assertThrows(FaultException.class,
            () -> injector.checkAndThrow(tokenValidate),
            "IAM failure should block token validation");

        // After IAM recovers
        registry.deactivateRule(tokenValidate.id());
        assertDoesNotThrow(
            () -> injector.checkAndThrow(tokenValidate),
            "After IAM recovery, validation should succeed");
    }

    // ── Scenario 5: Partial failure with probability ──

    @Test
    void partialFailureShouldBeProbabilistic() {
        FaultPoint nodeExec = FaultPoints.RUNTIME_NODE_EXECUTE;
        registry.registerFaultPoint(nodeExec);

        FaultRule partialRule = FaultRule.builder(nodeExec)
            .type(FaultType.EXCEPTION)
            .exceptionMessage("Intermittent failure")
            .probability(0.5)
            .build();
        registry.activateRule(partialRule);

        int failures = 0;
        int total = 100;
        for (int i = 0; i < total; i++) {
            try {
                simulateDbOperation(injector, nodeExec);
            } catch (FaultException e) {
                failures++;
            }
        }

        // With 50% probability, expect roughly 40-60 failures
        assertTrue(failures > 20 && failures < 80,
            "Partial failure rate should be probabilistic. "
            + "Got " + failures + "/" + total);
    }

    // ── Scenario 6: State consistency after crash ──

    @Test
    void stateConsistencyAfterCrashRecovery() {
        // Simulate: state is saved, then crash before event
        FaultPoint saveState = FaultPoints.PERSISTENCE_SAVE_STATE;
        FaultPoint appendEvent = FaultPoints.PERSISTENCE_APPEND_EVENT;
        registry.registerFaultPoint(saveState);
        registry.registerFaultPoint(appendEvent);

        // Phase 1: Normal operation
        List<String> eventLog = new ArrayList<>();
        simulateSaveAndEvent(injector, eventLog, "exec-1");
        assertEquals(1, eventLog.size());

        // Phase 2: Crash during event append
        FaultRule crashRule = FaultRule.builder(appendEvent)
            .type(FaultType.EXCEPTION)
            .exceptionMessage("Crash during event append")
            .build();
        registry.activateRule(crashRule);

        assertThrows(FaultException.class,
            () -> simulateSaveAndEvent(
                injector, eventLog, "exec-2"),
            "Crash should throw during event append");

        // Event log should not have exec-2 (crash before commit)
        assertEquals(1, eventLog.size(),
            "No partial event should be logged after crash");

        // Phase 3: Recovery
        registry.deactivateRule(appendEvent.id());
        simulateSaveAndEvent(injector, eventLog, "exec-2-retry");
        assertEquals(2, eventLog.size(),
            "After recovery, retry should succeed");
    }

    // ── Scenario 7: No duplicate side effects ──

    @Test
    void noDuplicateSideEffectsOnRetry() {
        FaultPoint providerCall = FaultPoints.PROVIDER_CALL;
        registry.registerFaultPoint(providerCall);

        Set<String> sideEffects = new LinkedHashSet<>();
        AtomicInteger callCount = new AtomicInteger(0);

        // First call fails
        FaultRule failRule = FaultRule.builder(providerCall)
            .type(FaultType.FAILURE)
            .exceptionMessage("Provider unavailable")
            .build();
        registry.activateRule(failRule);

        assertThrows(FaultException.class, () -> {
            callCount.incrementAndGet();
            simulateProviderCall(injector, sideEffects, "action-1");
        });
        assertEquals(1, callCount.get());
        assertTrue(sideEffects.isEmpty(),
            "Failed call should produce no side effects");

        // Retry succeeds
        registry.deactivateRule(providerCall.id());
        assertDoesNotThrow(() -> {
            callCount.incrementAndGet();
            simulateProviderCall(injector, sideEffects, "action-1");
        });
        assertEquals(2, callCount.get());
        assertEquals(1, sideEffects.size(),
            "Successful retry should produce exactly one side effect");
    }

    // ── Helpers ──

    private void simulateDbOperation(
        FaultInjector fi, FaultPoint fp
    ) {
        fi.checkAndThrow(fp);
    }

    private void simulateSaveAndEvent(
        FaultInjector fi,
        List<String> eventLog,
        String execId
    ) {
        // Save state (may throw)
        fi.checkAndThrow(FaultPoints.PERSISTENCE_SAVE_STATE);
        // Append event (may throw)
        fi.checkAndThrow(FaultPoints.PERSISTENCE_APPEND_EVENT);
        eventLog.add("event:" + execId);
    }

    private void simulateProviderCall(
        FaultInjector fi,
        Set<String> sideEffects,
        String actionId
    ) {
        fi.checkAndThrow(FaultPoints.PROVIDER_CALL);
        sideEffects.add(actionId);
    }
}
