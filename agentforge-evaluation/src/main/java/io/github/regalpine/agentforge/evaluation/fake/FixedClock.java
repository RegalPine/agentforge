package io.github.regalpine.agentforge.evaluation.fake;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

/**
 * FixedClock — AE-12 §63.
 *
 * A deterministic clock that always returns a fixed instant.
 * Essential for reproducible tests.
 */
public final class FixedClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public FixedClock(Instant instant) {
        this(instant, ZoneId.of("UTC"));
    }

    public FixedClock(Instant instant, ZoneId zone) {
        this.instant = Objects.requireNonNull(instant, "instant");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /**
     * Advance the clock by the given number of milliseconds.
     */
    public FixedClock advance(long millis) {
        this.instant = this.instant.plusMillis(millis);
        return this;
    }

    /**
     * Set the clock to a specific instant.
     */
    public FixedClock set(Instant instant) {
        this.instant = Objects.requireNonNull(instant, "instant");
        return this;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new FixedClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    @Override
    public String toString() {
        return "FixedClock{" + instant + "}";
    }
}
