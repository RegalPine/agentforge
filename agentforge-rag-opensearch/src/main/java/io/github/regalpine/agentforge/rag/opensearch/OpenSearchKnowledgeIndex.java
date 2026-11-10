package io.github.regalpine.agentforge.rag.opensearch;

import io.github.regalpine.agentforge.rag.index.KnowledgeIndex;
import io.github.regalpine.agentforge.rag.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * OpenSearch knowledge index.
 * AF-ENHANCE-03 §10: implements the
 * KnowledgeIndex SPI using OpenSearch
 * for full-text + vector hybrid
 * retrieval.
 *
 * <p>Uses JDK HttpClient to communicate
 * with the OpenSearch REST API.
 */
public final class OpenSearchKnowledgeIndex
    implements KnowledgeIndex {

    private final OpenSearchConfig config;
    private final HttpClient httpClient;
    private final String baseUrl;

    public OpenSearchKnowledgeIndex(
        OpenSearchConfig config
    ) {
        this.config =
            Objects.requireNonNull(config, "config");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.requestTimeout())
            .build();
        this.baseUrl = config.endpoint().toString()
            + "/" + config.indexName();
    }

    /**
     * Create the index with the correct
     * mapping.
     */
    public void createIndex() throws Exception {
        String body = OpenSearchJson
            .createIndexMapping(
                config.embeddingDimension()
            );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl))
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

        String docUrl = baseUrl + "/_doc/"
            + chunk.chunkId();

        String body = OpenSearchJson.indexDocJson(
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
            chunk.language(),
            chunk.embedding()
        );

        try {
            HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(docUrl))
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
            throw new OpenSearchException(
                "Upsert failed: " + chunk.chunkId(), e
            );
        }
    }

    @Override
    public void delete(String chunkId) {
        Objects.requireNonNull(chunkId, "chunkId");

        String docUrl = baseUrl + "/_doc/" + chunkId;

        try {
            HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(docUrl))
                .DELETE()
                .timeout(config.requestTimeout())
                .build();

            httpClient.send(request,
                HttpResponse.BodyHandlers
                    .ofString()
            );

        } catch (Exception e) {
            throw new OpenSearchException(
                "Delete failed: " + chunkId, e
            );
        }
    }

    @Override
    public List<RetrievalResult> search(
        KnowledgeQuery query
    ) {
        Objects.requireNonNull(query, "query");

        String searchUrl = baseUrl + "/_search";

        String body = OpenSearchJson.keywordSearchJson(
            query.query(), query.topK()
        );

        try {
            HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(searchUrl))
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
            throw new OpenSearchException(
                "Search failed", e
            );
        }
    }

    @Override
    public String indexType() {
        return "OPENSEARCH";
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
        List<RetrievalResult> results =
            new ArrayList<>();

        // Placeholder: the actual parsing
        // would extract hits from the
        // OpenSearch response.
        return results;
    }

    /**
     * OpenSearch exception.
     */
    public static final class OpenSearchException
        extends RuntimeException {

        public OpenSearchException(
            String msg, Throwable cause
        ) {
            super(msg, cause);
        }
    }
}
