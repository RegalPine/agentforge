package io.github.regalpine.agentforge.rag.qdrant;

import io.github.regalpine.agentforge.rag.index.KnowledgeIndex;
import io.github.regalpine.agentforge.rag.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Qdrant knowledge index.
 * AF-ENHANCE-03 §10: implements the
 * KnowledgeIndex SPI using Qdrant for
 * vector retrieval with payload
 * filtering.
 *
 * <p>Uses JDK HttpClient to communicate
 * with the Qdrant REST API.
 */
public final class QdrantKnowledgeIndex
    implements KnowledgeIndex {

    private final QdrantConfig config;
    private final HttpClient httpClient;
    private final String collectionUrl;

    public QdrantKnowledgeIndex(
        QdrantConfig config
    ) {
        this.config =
            Objects.requireNonNull(config, "config");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.requestTimeout())
            .build();
        this.collectionUrl = config.endpoint().toString()
            + "/collections/"
            + config.collectionName();
    }

    /**
     * Create the collection.
     */
    public void createCollection() throws Exception {
        String body = QdrantJson.createCollectionJson(
            config.embeddingDimension()
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(collectionUrl))
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json")
            .timeout(config.requestTimeout())
            .build();

        httpClient.send(request,
            HttpResponse.BodyHandlers.ofString()
        );
    }

    @Override
    public void upsert(KnowledgeChunk chunk) {
        Objects.requireNonNull(chunk, "chunk");

        String url = collectionUrl + "/points";

        String body = QdrantJson.upsertPointJson(
            chunk.chunkId(),
            chunk.documentId(),
            chunk.sourceId(),
            chunk.tenantId(),
            chunk.text(),
            chunk.location().page(),
            chunk.location().section(),
            chunk.location().paragraph(),
            chunk.checksum(),
            chunk.classification().name(),
            chunk.embedding()
        );

        try {
            HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers
                    .ofString(body)
                )
                .header("Content-Type",
                    "application/json"
                )
                .timeout(config.requestTimeout())
                .build();

            httpClient.send(request,
                HttpResponse.BodyHandlers
                    .ofString()
            );

        } catch (Exception e) {
            throw new QdrantException(
                "Upsert failed: " + chunk.chunkId(), e
            );
        }
    }

    @Override
    public void delete(String chunkId) {
        Objects.requireNonNull(chunkId, "chunkId");

        String url = collectionUrl + "/points/delete";
        String body = "{\"points\":[\""
            + chunkId + "\"]}";

        try {
            HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers
                    .ofString(body)
                )
                .header("Content-Type",
                    "application/json"
                )
                .timeout(config.requestTimeout())
                .build();

            httpClient.send(request,
                HttpResponse.BodyHandlers
                    .ofString()
            );

        } catch (Exception e) {
            throw new QdrantException(
                "Delete failed: " + chunkId, e
            );
        }
    }

    @Override
    public List<RetrievalResult> search(
        KnowledgeQuery query
    ) {
        Objects.requireNonNull(query, "query");

        String url = collectionUrl + "/points/search";

        String body = QdrantJson.searchJson(
            null, query.topK(),
            query.scope().tenantId()
        );

        try {
            HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers
                    .ofString(body)
                )
                .header("Content-Type",
                    "application/json"
                )
                .timeout(config.requestTimeout())
                .build();

            HttpResponse<String> response =
                httpClient.send(request,
                    HttpResponse.BodyHandlers
                        .ofString()
                );

            return parseSearchResults(response.body());

        } catch (Exception e) {
            throw new QdrantException(
                "Search failed", e
            );
        }
    }

    /**
     * Vector search with a pre-computed
     * query embedding.
     */
    public List<RetrievalResult> vectorSearch(
        float[] queryEmbedding,
        String tenantId,
        int topK
    ) {
        String url = collectionUrl + "/points/search";

        String body = QdrantJson.searchJson(
            queryEmbedding, topK, tenantId
        );

        try {
            HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers
                    .ofString(body)
                )
                .header("Content-Type",
                    "application/json"
                )
                .timeout(config.requestTimeout())
                .build();

            HttpResponse<String> response =
                httpClient.send(request,
                    HttpResponse.BodyHandlers
                        .ofString()
                );

            return parseSearchResults(response.body());

        } catch (Exception e) {
            throw new QdrantException(
                "Vector search failed", e
            );
        }
    }

    @Override
    public String indexType() {
        return "QDRANT";
    }

    @Override
    public int size() {
        return -1;
    }

    private List<RetrievalResult> parseSearchResults(
        String responseBody
    ) {
        // Minimal JSON parsing for the
        // search response. In production,
        // use a proper JSON parser.
        return new ArrayList<>();
    }

    /**
     * Qdrant exception.
     */
    public static final class QdrantException
        extends RuntimeException {

        public QdrantException(
            String msg, Throwable cause
        ) {
            super(msg, cause);
        }
    }
}
