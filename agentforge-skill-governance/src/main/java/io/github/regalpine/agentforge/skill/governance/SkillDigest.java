package io.github.regalpine.agentforge.skill.governance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Skill Artifact 完整性摘要.
 * AF-SKILL-02 §12: Integrity Verification.
 * AF-AD-15 §5.3: Digest/Signature.
 *
 * <p>SHA-256 digest of Skill artifact
 * content. Same id + version must produce
 * same digest. Used for integrity
 * verification and change detection.
 */
public record SkillDigest(
    String algorithm,
    String value
) {

    public SkillDigest {
        Objects.requireNonNull(
            algorithm, "algorithm"
        );
        Objects.requireNonNull(value, "value");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "digest value cannot be blank"
            );
        }
    }

    /**
     * Compute SHA-256 digest of content.
     */
    public static SkillDigest sha256(
        byte[] content
    ) {
        Objects.requireNonNull(
            content, "content"
        );

        try {
            MessageDigest md =
                MessageDigest.getInstance(
                    "SHA-256"
                );
            byte[] hash = md.digest(content);
            String hex = HexFormat.of()
                .formatHex(hash);
            return new SkillDigest(
                "SHA-256", hex
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "SHA-256 not available", e
            );
        }
    }

    /**
     * Compute SHA-256 digest of a string.
     */
    public static SkillDigest sha256(
        String content
    ) {
        Objects.requireNonNull(
            content, "content"
        );
        return sha256(
            content.getBytes(
                StandardCharsets.UTF_8
            )
        );
    }

    /**
     * Verify content matches this digest.
     */
    public boolean verify(byte[] content) {
        Objects.requireNonNull(
            content, "content"
        );
        SkillDigest computed = sha256(content);
        return value.equals(computed.value());
    }

    /**
     * Verify content matches this digest.
     */
    public boolean verify(String content) {
        return verify(
            content.getBytes(
                StandardCharsets.UTF_8
            )
        );
    }
}
