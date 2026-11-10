package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * A single embedding vector.
 */
public record EmbeddingVector(
    int index,
    float[] values
) {

    public EmbeddingVector {
        Objects.requireNonNull(values, "values");

        if (values.length == 0) {
            throw new IllegalArgumentException("values cannot be empty");
        }

        values = values.clone();
    }

    /**
     * Create an embedding vector.
     *
     * @param index  the index in the response
     * @param values the vector values
     * @return a new EmbeddingVector
     */
    public static EmbeddingVector of(int index, float[] values) {
        return new EmbeddingVector(index, values);
    }

    /**
     * Get the dimension of this vector.
     *
     * @return the number of dimensions
     */
    public int dimension() {
        return values.length;
    }
}
