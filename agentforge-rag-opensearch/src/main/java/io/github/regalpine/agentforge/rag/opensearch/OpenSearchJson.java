package io.github.regalpine.agentforge.rag.opensearch;

/**
 * OpenSearch index mapping and query
 * JSON builder.
 * AF-ENHANCE-03 §10: generates the
 * JSON payloads for OpenSearch REST API
 * without external dependencies.
 */
final class OpenSearchJson {

    private OpenSearchJson() {}

    static String createIndexMapping(int dim) {
        return "{"
            + "\"settings\":{\"number_of_shards\":1,"
            + "\"index.knn\":true},"
            + "\"mappings\":{\"properties\":{"
            + "\"chunk_id\":{\"type\":\"keyword\"},"
            + "\"document_id\":{\"type\":\"keyword\"},"
            + "\"source_id\":{\"type\":\"keyword\"},"
            + "\"tenant_id\":{\"type\":\"keyword\"},"
            + "\"text\":{\"type\":\"text\","
            + "\"analyzer\":\"standard\"},"
            + "\"page\":{\"type\":\"integer\"},"
            + "\"section\":{\"type\":\"keyword\"},"
            + "\"paragraph\":{\"type\":\"integer\"},"
            + "\"checksum\":{\"type\":\"keyword\"},"
            + "\"classification\":{\"type\":\"keyword\"},"
            + "\"language\":{\"type\":\"keyword\"},"
            + "\"embedding\":{\"type\":\"knn_vector\","
            + "\"dimension\":" + dim + ","
            + "\"method\":{\"name\":\"hnsw\","
            + "\"engine\":\"nmslib\","
            + "\"space_type\":\"cosinesimil\"}"
            + "},"
            + "\"metadata\":{\"type\":\"object\","
            + "\"enabled\":true}"
            + "}}}"
            + "}";
    }

    static String indexDocJson(
        String chunkId,
        String documentId,
        String sourceId,
        String tenantId,
        String text,
        int page,
        String section,
        int paragraph,
        String checksum,
        String classification,
        String language,
        float[] embedding
    ) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"chunk_id\":\"")
          .append(esc(chunkId)).append("\",");
        sb.append("\"document_id\":\"")
          .append(esc(documentId)).append("\",");
        sb.append("\"source_id\":\"")
          .append(esc(sourceId)).append("\",");
        sb.append("\"tenant_id\":\"")
          .append(esc(tenantId)).append("\",");
        sb.append("\"text\":\"")
          .append(esc(text)).append("\",");
        sb.append("\"page\":").append(page).append(",");
        sb.append("\"section\":\"")
          .append(esc(section)).append("\",");
        sb.append("\"paragraph\":")
          .append(paragraph).append(",");
        sb.append("\"checksum\":\"")
          .append(esc(checksum)).append("\",");
        sb.append("\"classification\":\"")
          .append(esc(classification)).append("\",");
        sb.append("\"language\":\"")
          .append(esc(language)).append("\",");
        sb.append("\"embedding\":[");

        if (embedding != null) {
            for (int i = 0; i < embedding.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(embedding[i]);
            }
        }

        sb.append("],\"metadata\":{}}");
        return sb.toString();
    }

    static String knnSearchJson(
        float[] queryEmbedding, int k
    ) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"size\":").append(k).append(",");
        sb.append("\"query\":{\"knn\":{");
        sb.append("\"embedding\":{");
        sb.append("\"vector\":[");

        if (queryEmbedding != null) {
            for (int i = 0;
                 i < queryEmbedding.length;
                 i++
            ) {
                if (i > 0) sb.append(",");
                sb.append(queryEmbedding[i]);
            }
        }

        sb.append("],\"k\":").append(k).append("}");
        sb.append("}}}");
        return sb.toString();
    }

    static String keywordSearchJson(
        String queryText, int size
    ) {
        return "{\"size\":" + size + ","
            + "\"query\":{\"match\":{"
            + "\"text\":{\"query\":\""
            + esc(queryText) + "\"}"
            + "}}}";
    }

    static String hybridSearchJson(
        String queryText,
        float[] queryEmbedding,
        int size
    ) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"size\":").append(size).append(",");
        sb.append("\"query\":{\"bool\":{\"should\":[");
        sb.append("{\"match\":{\"text\":{\"query\":\"")
          .append(esc(queryText))
          .append("\"}}},");
        sb.append("{\"knn\":{\"embedding\":{");
        sb.append("\"vector\":[");

        if (queryEmbedding != null) {
            for (int i = 0;
                 i < queryEmbedding.length;
                 i++
            ) {
                if (i > 0) sb.append(",");
                sb.append(queryEmbedding[i]);
            }
        }

        sb.append("],\"k\":").append(size).append("}");
        sb.append("}}");
        sb.append("]}}}");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
