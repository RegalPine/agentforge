package io.github.regalpine.agentforge.security.supply;

import java.security.MessageDigest;
import java.util.*;

/**
 * ArtifactVerifier — AF-SEC-01 §51-52.
 *
 * Verifies artifact integrity:
 * - Checksum verification (SHA-256)
 * - Signature verification (placeholder)
 * - Dependency metadata validation
 *
 * Production deployment must verify
 * artifact integrity before deploy (§51).
 */
public final class ArtifactVerifier {

    /**
     * Verify a checksum matches expected value.
     *
     * @param data the data to verify
     * @param expectedChecksum the expected SHA-256 hex
     * @return verification result
     */
    public VerificationResult verifyChecksum(
        byte[] data,
        String expectedChecksum
    ) {
        if (data == null || data.length == 0) {
            return VerificationResult.failed(
                "Data is null or empty");
        }

        if (expectedChecksum == null
            || expectedChecksum.isBlank()) {
            return VerificationResult.failed(
                "Expected checksum is empty");
        }

        try {
            MessageDigest digest = MessageDigest
                .getInstance("SHA-256");

            byte[] hash = digest.digest(data);
            String actual = bytesToHex(hash);

            if (actual.equalsIgnoreCase(expectedChecksum)) {
                return VerificationResult.verified(actual);
            } else {
                return VerificationResult.failed(
                    "Checksum mismatch: expected="
                        + expectedChecksum
                        + " actual=" + actual);
            }
        } catch (Exception e) {
            return VerificationResult.failed(
                "Checksum error: " + e.getMessage());
        }
    }

    /**
     * Compute SHA-256 checksum of data.
     */
    public String computeChecksum(byte[] data) {
        if (data == null) return null;
        try {
            MessageDigest digest = MessageDigest
                .getInstance("SHA-256");
            return bytesToHex(digest.digest(data));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verify a component record has required fields.
     */
    public List<String> validateComponent(
        ComponentRecord component
    ) {
        var errors = new ArrayList<String>();

        if (component == null) {
            errors.add("Component is null");
            return errors;
        }

        if (component.checksum() == null
            || component.checksum().isBlank()) {
            errors.add("Missing checksum");
        }

        if (component.source() == null
            || component.source().isBlank()) {
            errors.add("Missing source");
        }

        if (component.securityStatus()
            == ComponentRecord.SecurityStatus.BLOCKED) {
            errors.add("Component is blocked");
        }

        if (component.securityStatus()
            == ComponentRecord.SecurityStatus.VULNERABLE) {
            errors.add("Component has known vulnerabilities");
        }

        return errors;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Verification result.
     */
    public record VerificationResult(
        boolean verified,
        String checksum,
        String reason
    ) {

        public static VerificationResult verified(
            String checksum
        ) {
            return new VerificationResult(
                true, checksum, "");
        }

        public static VerificationResult failed(
            String reason
        ) {
            return new VerificationResult(
                false, null, reason);
        }
    }
}
