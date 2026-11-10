package io.github.regalpine.agentforge.agent;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StagnationDetectorTest {

    @Test
    void shouldDetectStagnationAfterThreshold() {
        StagnationDetector detector = new StagnationDetector(5, 3);
        assertTrue(detector.record("A").isEmpty());
        assertTrue(detector.record("A").isEmpty());
        Optional<StopCondition> result = detector.record("A");
        assertTrue(result.isPresent());
        assertEquals(StopCondition.StopReason.STAGNATION, result.get().reason());
    }

    @Test
    void shouldNotDetectWithDifferentSignatures() {
        StagnationDetector detector = new StagnationDetector(5, 3);
        assertTrue(detector.record("A").isEmpty());
        assertTrue(detector.record("B").isEmpty());
        assertTrue(detector.record("C").isEmpty());
        assertTrue(detector.record("A").isEmpty());
    }

    @Test
    void shouldResetAfterMixedSequence() {
        StagnationDetector detector = new StagnationDetector(5, 3);
        detector.record("A");
        detector.record("A");
        detector.record("B"); // breaks the streak
        assertTrue(detector.record("A").isEmpty());
    }

    @Test
    void shouldRespectWindowSize() {
        StagnationDetector detector = new StagnationDetector(3, 3);
        detector.record("A");
        detector.record("B");
        detector.record("C");
        // Window is [A, B, C], A falls out
        assertTrue(detector.record("D").isEmpty());
    }

    @Test
    void shouldResetState() {
        StagnationDetector detector = new StagnationDetector(5, 3);
        detector.record("A");
        detector.record("A");
        detector.reset();
        assertEquals(0, detector.size());
        assertTrue(detector.record("A").isEmpty());
    }

    @Test
    void shouldRejectInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> new StagnationDetector(1, 1));
        assertThrows(IllegalArgumentException.class, () -> new StagnationDetector(3, 1));
        assertThrows(IllegalArgumentException.class, () -> new StagnationDetector(2, 3));
    }
}
