package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.skill.governance.SkillDigest;

import java.util.Objects;

/**
 * Skill 签名验证器。
 *
 * <p>验证 Skill 包完整性和来源。
 * 对应 AF-ENHANCE-05 §9。</p>
 */
public final class SkillSignatureVerifier {

    /**
     * 验证 Skill 包完整性。
     */
    public VerificationResult verify(
        byte[] content,
        SkillDigest expectedDigest
    ) {
        Objects.requireNonNull(
            content, "content"
        );
        Objects.requireNonNull(
            expectedDigest,
            "expectedDigest"
        );

        boolean valid =
            expectedDigest.verify(content);

        return new VerificationResult(
            valid,
            valid
                ? "Digest verified"
                : "Digest mismatch",
            expectedDigest
        );
    }

    /**
     * 验证并拒绝不匹配。
     */
    public void verifyOrThrow(
        byte[] content,
        SkillDigest expectedDigest
    ) {
        VerificationResult result =
            verify(content, expectedDigest);
        if (!result.valid()) {
            throw new SkillIntegrityException(
                result.message()
            );
        }
    }

    /**
     * 验证结果。
     */
    public record VerificationResult(
        boolean valid,
        String message,
        SkillDigest digest
    ) {}

    /**
     * Skill 完整性异常。
     */
    public static class
    SkillIntegrityException
    extends RuntimeException {
        public SkillIntegrityException(
            String message
        ) {
            super(message);
        }
    }
}
