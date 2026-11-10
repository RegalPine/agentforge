package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Objects;

/**
 * Model parameters — AF-ENHANCE-09 §43.
 *
 * <p>Invocation parameters for a model:
 * temperature, topP, maxOutputTokens,
 * seed, extensions.
 *
 * <p>Runtime must record the final
 * effective parameters.
 */
public record ModelParameters(
    Double temperature,
    Double topP,
    Integer maxOutputTokens,
    Long seed,
    Map<String, Object> extensions
) {

    public ModelParameters {
        if (temperature != null) {
            if (temperature < 0.0
                || temperature > 2.0
            ) {
                throw new IllegalArgumentException(
                    "temperature must be 0.0-2.0"
                );
            }
        }
        if (topP != null) {
            if (topP < 0.0 || topP > 1.0) {
                throw new IllegalArgumentException(
                    "topP must be 0.0-1.0"
                );
            }
        }
        if (maxOutputTokens != null) {
            if (maxOutputTokens <= 0) {
                throw new IllegalArgumentException(
                    "maxOutputTokens must be > 0"
                );
            }
        }

        extensions = extensions == null
            ? Map.of()
            : Map.copyOf(extensions);
    }

    /**
     * Default parameters.
     */
    public static ModelParameters defaults() {
        return new ModelParameters(
            0.7, 1.0, null, null, null
        );
    }

    /**
     * Deterministic parameters.
     */
    public static ModelParameters deterministic() {
        return new ModelParameters(
            0.0, 1.0, null, 42L, null
        );
    }

    /**
     * Builder for ModelParameters.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ModelParameters.
     */
    public static class Builder {
        private Double temperature;
        private Double topP;
        private Integer maxOutputTokens;
        private Long seed;
        private Map<String, Object> extensions;

        public Builder temperature(Double t) {
            this.temperature = t;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder maxOutputTokens(Integer m) {
            this.maxOutputTokens = m;
            return this;
        }

        public Builder seed(Long seed) {
            this.seed = seed;
            return this;
        }

        public Builder extensions(
            Map<String, Object> ext
        ) {
            this.extensions = ext;
            return this;
        }

        public ModelParameters build() {
            return new ModelParameters(
                temperature, topP,
                maxOutputTokens, seed,
                extensions
            );
        }
    }
}
