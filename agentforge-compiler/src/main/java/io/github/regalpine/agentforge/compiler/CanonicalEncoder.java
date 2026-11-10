package io.github.regalpine.agentforge.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Versioned canonical encoding for graph fingerprinting.
 * <p>
 * Rules:
 * <ul>
 *   <li>Object keys sorted recursively (TreeMap order)</li>
 *   <li>Arrays preserve order</li>
 *   <li>Numbers and strings are type-distinguished</li>
 *   <li>Layout-only fields are excluded</li>
 *   <li>Fingerprint prefixed with version marker (e.g. "v2:")</li>
 * </ul>
 */
public final class CanonicalEncoder {

    /** Current fingerprint encoding version. */
    public static final String VERSION = "v2";

    /** Fields to exclude from fingerprint (layout-only). */
    private static final Set<String> EXCLUDED_FIELDS = Set.of(
        "x", "y", "width", "height", "position", "style", "color",
        "backgroundColor", "borderColor", "zIndex", "selected",
        "dragging", "resizing"
    );

    private CanonicalEncoder() {}

    /**
     * Compute a SHA-256 fingerprint from a structured object.
     * The object is first canonicalized (sorted keys, type tags, etc.)
     * then hashed.
     */
    public static String fingerprint(Object obj) {
        String canonical = canonicalize(obj);
        String versioned = VERSION + ":" + canonical;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(versioned.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return VERSION + ":" + hex;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Produce a canonical string representation of an object.
     */
    @SuppressWarnings("unchecked")
    public static String canonicalize(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Boolean b) return "bool:" + b;
        if (obj instanceof Integer i) return "int:" + i;
        if (obj instanceof Long l) return "long:" + l;
        if (obj instanceof BigInteger bi) return "bigint:" + bi;
        if (obj instanceof Float f) return "dec:" + BigDecimal.valueOf(f);
        if (obj instanceof Double d) {
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("Non-finite number: " + d);
            }
            return "dec:" + BigDecimal.valueOf(d);
        }
        if (obj instanceof BigDecimal bd) return "dec:" + bd.toPlainString();
        if (obj instanceof String s) return "str:" + s;
        if (obj instanceof Map<?, ?> map) {
            return canonicalizeMap(map);
        }
        if (obj instanceof List<?> list) {
            return canonicalizeList(list);
        }
        // Fallback: treat as string
        return "str:" + obj;
    }

    private static String canonicalizeMap(Map<?, ?> map) {
        // Filter out layout fields, then sort by key
        TreeMap<String, String> entries = new TreeMap<>();
        for (var entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (EXCLUDED_FIELDS.contains(key)) continue;
            entries.put(key, canonicalize(entry.getValue()));
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : entries.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("str:").append(entry.getKey()).append(":").append(entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }

    private static String canonicalizeList(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(canonicalize(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
}
