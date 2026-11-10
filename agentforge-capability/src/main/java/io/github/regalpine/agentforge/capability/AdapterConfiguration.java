package io.github.regalpine.agentforge.capability;

import java.util.Map;
import java.util.Objects;

/**
 * Adapter configuration.
 * AF-ENHANCE-02 §5: immutable
 * configuration passed to adapters
 * at construction time.
 *
 * <p>Each adapter type defines its
 * own expected configuration keys.
 * The configuration is validated
 * during adapter initialisation.
 */
public record AdapterConfiguration(
    String adapterId,
    Map<String, Object> properties
) {

    public AdapterConfiguration {
        Objects.requireNonNull(adapterId, "adapterId");

        if (adapterId.isBlank()) {
            throw new IllegalArgumentException(
                "adapterId cannot be blank"
            );
        }

        properties =
            properties == null
                ? Map.of()
                : Map.copyOf(properties);
    }

    /**
     * Retrieve a required configuration
     * property.
     *
     * @throws IllegalArgumentException
     *   if the key is missing
     */
    public String require(String key) {
        Object value = properties.get(key);

        if (value == null) {
            throw new IllegalArgumentException(
                "Required configuration '"
                    + key + "' missing for adapter '"
                    + adapterId + "'"
            );
        }

        return value.toString();
    }

    /**
     * Retrieve an optional configuration
     * property with a default value.
     */
    public String getOrDefault(
        String key,
        String defaultValue
    ) {
        Object value = properties.get(key);

        return value != null
            ? value.toString()
            : defaultValue;
    }

    /**
     * Retrieve an integer property.
     */
    public int getInt(
        String key,
        int defaultValue
    ) {
        Object value = properties.get(key);

        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number n) {
            return n.intValue();
        }

        return Integer.parseInt(
            value.toString()
        );
    }

    /**
     * Retrieve a long property.
     */
    public long getLong(
        String key,
        long defaultValue
    ) {
        Object value = properties.get(key);

        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number n) {
            return n.longValue();
        }

        return Long.parseLong(
            value.toString()
        );
    }

    /**
     * Retrieve a boolean property.
     */
    public boolean getBoolean(
        String key,
        boolean defaultValue
    ) {
        Object value = properties.get(key);

        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Boolean b) {
            return b;
        }

        return Boolean.parseBoolean(
            value.toString()
        );
    }
}
