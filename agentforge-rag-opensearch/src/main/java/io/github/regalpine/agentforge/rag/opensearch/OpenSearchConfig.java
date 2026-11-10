package io.github.regalpine.agentforge.rag.opensearch;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * OpenSearch RAG store configuration.
 * AF-ENHANCE-03 §10: configures the
 * OpenSearch connection for full-text
 * + vector hybrid retrieval.
 */
public record OpenSearchConfig(
    URI endpoint,
    String indexName,
    String username,
    String password,
    int embeddingDimension,
    String knnEngine,
    Duration requestTimeout,
    Map<String, Object> indexSettings,
    Map<String, Object> properties
) {

    public OpenSearchConfig {
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(indexName, "indexName");

        if (embeddingDimension <= 0) {
            embeddingDimension = 1536;
        }

        if (knnEngine == null
            || knnEngine.isBlank()
        ) {
            knnEngine = "nmslib";
        }

        requestTimeout =
            requestTimeout == null
                ? Duration.ofSeconds(30)
                : requestTimeout;

        indexSettings =
            indexSettings == null
                ? Map.of()
                : Map.copyOf(indexSettings);

        properties =
            properties == null
                ? Map.of()
                : Map.copyOf(properties);
    }

    public static OpenSearchConfig of(
        URI endpoint, String indexName
    ) {
        return new OpenSearchConfig(
            endpoint, indexName,
            null, null, 1536,
            "nmslib",
            Duration.ofSeconds(30),
            Map.of(), Map.of()
        );
    }
}
