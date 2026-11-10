package io.github.regalpine.agentforge.runtime.hitl;

import io.github.regalpine.agentforge.event.Execution;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 上下文脱敏器。
 *
 * <p>将 Execution Context 转换为
 * Approval Context，对敏感字段
 * 应用脱敏策略。
 * 对应 AF-ENHANCE-03 §14。</p>
 */
public final class ContextRedactor {

    private final Map<String,
        RedactionPolicy> rules;

    public ContextRedactor(
        Map<String, RedactionPolicy> rules
    ) {
        Objects.requireNonNull(
            rules, "rules"
        );
        this.rules = Map.copyOf(rules);
    }

    /**
     * 对上下文中敏感字段脱敏。
     */
    public Map<String, Object> redact(
        Map<String, Object> context
    ) {
        if (context == null) {
            return Map.of();
        }
        Map<String, Object> result =
            new LinkedHashMap<>();
        for (var entry
            : context.entrySet()
        ) {
            String key = entry.getKey();
            Object value = entry.getValue();
            RedactionPolicy policy =
                rules.get(key);
            if (policy == null) {
                result.put(key, value);
                continue;
            }
            switch (policy) {
                case MASK ->
                    result.put(
                        key, mask(value)
                    );
                case REDACT ->
                    result.put(
                        key, "[REDACTED]"
                    );
                case OMIT -> {
                    // 完全移除
                }
                case HASH ->
                    result.put(
                        key, hash(value)
                    );
                case SUMMARY ->
                    result.put(
                        key, summarize(value)
                    );
            }
        }
        return Map.copyOf(result);
    }

    /**
     * 部分遮盖。
     */
    static String mask(Object value) {
        if (value == null) {
            return "****";
        }
        String s = value.toString();
        if (s.length() <= 4) {
            return "****";
        }
        int visible = Math.max(
            1, s.length() / 4
        );
        String tail = s.substring(
            s.length() - visible
        );
        return "*".repeat(
            s.length() - visible
        ) + tail;
    }

    /**
     * SHA-256 哈希。
     */
    static String hash(Object value) {
        if (value == null) {
            return "null-hash";
        }
        try {
            byte[] bytes =
                value.toString()
                    .getBytes(
                        StandardCharsets.UTF_8
                    );
            MessageDigest md =
                MessageDigest.getInstance(
                    "SHA-256"
                );
            byte[] digest = md.digest(bytes);
            return HexFormat.of()
                .formatHex(digest)
                .substring(0, 16);
        } catch (Exception e) {
            return "hash-error";
        }
    }

    /**
     * 摘要。
     */
    static String summarize(
        Object value
    ) {
        if (value == null) {
            return "<null>";
        }
        String s = value.toString();
        if (s.length() <= 20) {
            return "<" + s.length()
                + " chars>";
        }
        return "<" + s.length()
            + " chars, "
            + s.substring(0, 20)
            + "...>";
    }
}
