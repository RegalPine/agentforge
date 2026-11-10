package io.github.regalpine.agentforge.rag.qdrant;

/**
 * Qdrant REST API JSON builder.
 * AF-ENHANCE-03 §10: generates the
 * JSON payloads for Qdrant REST API
 * without external dependencies.
 */
final class QdrantJson {

    private QdrantJson() {}

    static String createCollectionJson(int dim) {
        return "{\"vectors\":{"
            + "\"size\":" + dim + ","
            + "\"distance\":\"Cosine\","
            + "\"on_disk\":false"
            + "},"
            + "\"optimizers_config\":{"
            + "\"default_segment_number\":2"
            + "},"
            + "\"on_disk_payload\":false"
            + "}";
    }

    static String upsertPointJson(
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
        float[] embedding
    ) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"points\":[{");
        sb.append("\"id\":\"")
          .append(esc(chunkId)).append("\",");
        sb.append("\"vector\":[");

        if (embedding != null) {
            for (int i = 0; i < embedding.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(embedding[i]);
            }
        }

        sb.append("],\"payload\":{");
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
          .append(esc(classification)).append("\"");
        sb.append("}}]}");
        return sb.toString();
    }

    static String searchJson(
        float[] queryVector, int limit,
        String tenantFilter
    ) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"vector\":[");

        if (queryVector != null) {
            for (int i = 0; i < queryVector.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(queryVector[i]);
            }
        }

        sb.append("],\"limit\":").append(limit);

        if (tenantFilter != null
            && !tenantFilter.isBlank()
        ) {
            sb.append(",\"filter\":{\"must\":[");
            sb.append("{\"key\":\"tenant_id\",");
            sb.append("\"match\":{");
            sb.append("\"value\":\"")
              .append(esc(tenantFilter))
              .append("\"}");
            sb.append("}");
            sb.append("]}");
        }

        sb.append(",\"with_payload\":true");
        sb.append("}");
        return sb.toString();
    }

    static String scrollJson(
        String tenantId, int limit
    ) {
        return "{\"filter\":{\"must\":["
            + "{\"key\":\"tenant_id\","
            + "\"match\":{\"value\":\""
            + esc(tenantId) + "\"}}"
            + "]},"
            + "\"limit\":" + limit + ","
            + "\"with_payload\":true}";
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
