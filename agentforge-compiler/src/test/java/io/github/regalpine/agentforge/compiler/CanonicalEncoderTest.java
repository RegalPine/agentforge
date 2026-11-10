package io.github.regalpine.agentforge.compiler;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Batch 5: CanonicalEncoder tests.
 * Verifies versioned fingerprint encoding with sorted keys, type distinction,
 * layout field exclusion, and determinism.
 */
class CanonicalEncoderTest {

    @Test
    void fingerprintIsVersioned() {
        String fp = CanonicalEncoder.fingerprint("test");
        assertTrue(fp.startsWith(CanonicalEncoder.VERSION + ":"), "Fingerprint should start with version marker");
    }

    @Test
    void sameInputProducesSameFingerprint() {
        String fp1 = CanonicalEncoder.fingerprint(Map.of("a", 1, "b", "hello"));
        String fp2 = CanonicalEncoder.fingerprint(Map.of("b", "hello", "a", 1));
        assertEquals(fp1, fp2, "Same data with different key order should produce same fingerprint");
    }

    @Test
    void differentTypesProduceDifferentFingerprints() {
        String fpInt = CanonicalEncoder.fingerprint(42);
        String fpStr = CanonicalEncoder.fingerprint("42");
        assertNotEquals(fpInt, fpStr, "Integer 42 and string '42' should have different fingerprints");
    }

    @Test
    void layoutFieldsAreExcluded() {
        Map<String, Object> withLayout = new LinkedHashMap<>();
        withLayout.put("id", "n1");
        withLayout.put("type", "START");
        withLayout.put("x", 100);
        withLayout.put("y", 200);

        Map<String, Object> withoutLayout = new LinkedHashMap<>();
        withoutLayout.put("id", "n1");
        withoutLayout.put("type", "START");

        String fp1 = CanonicalEncoder.fingerprint(withLayout);
        String fp2 = CanonicalEncoder.fingerprint(withoutLayout);
        assertEquals(fp1, fp2, "Layout fields should not affect fingerprint");
    }

    @Test
    void arrayOrderIsPreserved() {
        String fp1 = CanonicalEncoder.fingerprint(List.of(1, 2, 3));
        String fp2 = CanonicalEncoder.fingerprint(List.of(3, 2, 1));
        assertNotEquals(fp1, fp2, "Different array orders should produce different fingerprints");
    }

    @Test
    void nestedMapsAreSortedRecursively() {
        Map<String, Object> inner1 = Map.of("b", 2, "a", 1);
        Map<String, Object> inner2 = Map.of("a", 1, "b", 2);
        Map<String, Object> outer1 = Map.of("data", inner1);
        Map<String, Object> outer2 = Map.of("data", inner2);
        assertEquals(
            CanonicalEncoder.fingerprint(outer1),
            CanonicalEncoder.fingerprint(outer2)
        );
    }

    @Test
    void nonFiniteNumbersAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> CanonicalEncoder.canonicalize(Double.NaN));
        assertThrows(IllegalArgumentException.class,
            () -> CanonicalEncoder.canonicalize(Double.POSITIVE_INFINITY));
    }

    @Test
    void nullValuesAreHandled() {
        String fp = CanonicalEncoder.fingerprint(null);
        assertNotNull(fp);
        assertTrue(fp.startsWith("v2:"));
    }

    @Test
    void booleanValuesAreTypeDistinguished() {
        String fpTrue = CanonicalEncoder.fingerprint(true);
        String fpFalse = CanonicalEncoder.fingerprint(false);
        assertNotEquals(fpTrue, fpFalse);
        assertNotEquals(fpTrue, CanonicalEncoder.fingerprint("true"));
    }
}
