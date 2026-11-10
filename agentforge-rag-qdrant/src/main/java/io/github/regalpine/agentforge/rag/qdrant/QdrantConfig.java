package io.github.regalpine.agentforge.rag.qdrant;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Qdrant RAG store configuration.
 * AF-ENHANCE-03 §10: configures the
 * Qdrant connection for vector retrieval
 * with payload filtering.
 */
public record QdrantConfig(
    URI endpoint,
    String collectionName,
    String apiKey,
    int embeddingDimension,
    String distance,
    boolean onDiskPayload,
    Duration requestTimeout,
    Map<String, Object> properties
) {

    public QdrantConfig {
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(
            collectionName, "collectionName"
        );

        if (embeddingDimension <= 0) {
            embeddingDimension = 1536;
        }

        if (distance == null
            || distance.isBlank()
        ) {
            distance = "Cosine";
        }

        requestTimeout =
            requestTimeout == null
                ? Duration.ofSeconds(30)
                : requestTimeout;

        properties =
            properties == null
                ? Map.of()
                : Map.copyOf(properties);
    }

    public static QdrantConfig of(
        URI endpoint, String collection
    ) {
        return new QdrantConfig(
            endpoint, collection,
            null, 1536, "Cosine",
            false,
            Duration.ofSeconds(30),
            Map.of()
        );
    }
}
