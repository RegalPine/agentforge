package io.github.regalpine.agentforge.hardening;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * TCK test case identifier.
 * AF-RELEASE-02 §7: format is
 * AF-{DOMAIN}-{NNN}.
 *
 * <p>Examples:
 * AF-CORE-001, AF-GRAPH-012,
 * AF-SEC-003.
 */
public record TckId(
    String domain,
    int number
) {

    private static final Pattern FMT =
        Pattern.compile("AF-[A-Z]+-\\d{3}");

    public TckId {
        Objects.requireNonNull(domain, "domain");
        if (domain.isBlank()) {
            throw new IllegalArgumentException(
                "domain must not be blank"
            );
        }
        if (number < 0) {
            number = 0;
        }
    }

    /**
     * Parse from string "AF-DOMAIN-NNN".
     */
    public static TckId parse(String raw) {
        Objects.requireNonNull(raw);
        if (!FMT.matcher(raw).matches()) {
            throw new IllegalArgumentException(
                "Invalid TCK ID format: " + raw
                + " (expected AF-{DOMAIN}-{NNN})"
            );
        }
        String[] parts = raw.split("-");
        return new TckId(
            parts[1],
            Integer.parseInt(parts[2])
        );
    }

    /**
     * Create from domain and number.
     */
    public static TckId of(
        String domain, int number
    ) {
        return new TckId(domain, number);
    }

    /**
     * Full string representation.
     */
    @Override
    public String toString() {
        return "AF-" + domain + "-"
            + String.format("%03d", number);
    }
}
