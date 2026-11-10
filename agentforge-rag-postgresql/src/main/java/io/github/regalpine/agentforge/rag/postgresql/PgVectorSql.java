package io.github.regalpine.agentforge.rag.postgresql;

/**
 * PGVector DDL and SQL statements.
 * AF-ENHANCE-03 §10: all SQL is
 * parameterised and uses the pgvector
 * extension for vector operations.
 */
final class PgVectorSql {

    private PgVectorSql() {}

    static String createTableSql(String q) {
        return "CREATE TABLE IF NOT EXISTS " + q + " ("
            + " chunk_id      TEXT PRIMARY KEY,"
            + " document_id   TEXT NOT NULL,"
            + " source_id     TEXT NOT NULL,"
            + " tenant_id     TEXT NOT NULL,"
            + " text          TEXT NOT NULL,"
            + " page          INT NOT NULL DEFAULT 0,"
            + " section       TEXT NOT NULL DEFAULT '',"
            + " paragraph     INT NOT NULL DEFAULT 0,"
            + " checksum      TEXT NOT NULL DEFAULT '',"
            + " classification TEXT NOT NULL DEFAULT 'INTERNAL',"
            + " language      TEXT NOT NULL DEFAULT 'en',"
            + " embedding     vector(NOT_SET),"
            + " metadata      JSONB NOT NULL DEFAULT '{}'::jsonb,"
            + " created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),"
            + " updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()"
            + ")";
    }

    static String createTableSqlDim(
        String q, int dim
    ) {
        return "CREATE TABLE IF NOT EXISTS " + q + " ("
            + " chunk_id      TEXT PRIMARY KEY,"
            + " document_id   TEXT NOT NULL,"
            + " source_id     TEXT NOT NULL,"
            + " tenant_id     TEXT NOT NULL,"
            + " text          TEXT NOT NULL,"
            + " page          INT NOT NULL DEFAULT 0,"
            + " section       TEXT NOT NULL DEFAULT '',"
            + " paragraph     INT NOT NULL DEFAULT 0,"
            + " checksum      TEXT NOT NULL DEFAULT '',"
            + " classification TEXT NOT NULL DEFAULT 'INTERNAL',"
            + " language      TEXT NOT NULL DEFAULT 'en',"
            + " embedding     vector(" + dim + "),"
            + " metadata      JSONB NOT NULL DEFAULT '{}'::jsonb,"
            + " created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),"
            + " updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()"
            + ")";
    }

    static String createTenantIndexSql(String q) {
        int dot = q.lastIndexOf('.');
        String name = dot >= 0
            ? q.substring(dot + 1) : q;

        return "CREATE INDEX IF NOT EXISTS idx_"
            + name + "_tenant ON " + q
            + " (tenant_id, source_id)";
    }

    static String createVectorIndexSql(
        String q, String method, int ef, int m
    ) {
        int dot = q.lastIndexOf('.');
        String name = dot >= 0
            ? q.substring(dot + 1) : q;

        if ("hnsw".equalsIgnoreCase(method)) {
            return "CREATE INDEX IF NOT EXISTS idx_"
                + name + "_embedding ON " + q
                + " USING hnsw (embedding vector_cosine_ops)"
                + " WITH (m = " + m
                + ", ef_construction = " + ef + ")";
        }

        return "CREATE INDEX IF NOT EXISTS idx_"
            + name + "_embedding ON " + q
            + " USING ivfflat (embedding vector_cosine_ops)"
            + " WITH (lists = 100)";
    }

    static String upsertSql(String q) {
        return "INSERT INTO " + q + " ("
            + "chunk_id, document_id, source_id,"
            + "tenant_id, text, page, section,"
            + "paragraph, checksum, classification,"
            + "language, embedding, metadata"
            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"
            + " ON CONFLICT (chunk_id) DO UPDATE SET"
            + " text=EXCLUDED.text,"
            + " embedding=EXCLUDED.embedding,"
            + " metadata=EXCLUDED.metadata,"
            + " updated_at=now()";
    }

    static String deleteSql(String q) {
        return "DELETE FROM " + q
            + " WHERE chunk_id = ?";
    }

    static String vectorSearchSql(String q) {
        return "SELECT chunk_id, document_id,"
            + " source_id, tenant_id, text,"
            + " page, section, paragraph,"
            + " checksum, classification,"
            + " language, metadata,"
            + " 1 - (embedding <=> ?::vector) AS score"
            + " FROM " + q
            + " WHERE tenant_id = ?"
            + " ORDER BY score DESC"
            + " LIMIT ?";
    }

    static String keywordSearchSql(String q) {
        return "SELECT chunk_id, document_id,"
            + " source_id, tenant_id, text,"
            + " page, section, paragraph,"
            + " checksum, classification,"
            + " language, metadata,"
            + " ts_rank("
            + "   to_tsvector('english', text),"
            + "   plainto_tsquery('english', ?)"
            + " ) AS score"
            + " FROM " + q
            + " WHERE tenant_id = ?"
            + " AND to_tsvector('english', text)"
            + "     @@ plainto_tsquery('english', ?)"
            + " ORDER BY score DESC"
            + " LIMIT ?";
    }

    static String hybridSearchSql(String q) {
        return "WITH vec AS ("
            + "  SELECT chunk_id,"
            + "    1 - (embedding <=> ?::vector) AS vec_score"
            + "  FROM " + q
            + "  WHERE tenant_id = ?"
            + "  ORDER BY vec_score DESC"
            + "  LIMIT ?"
            + "),"
            + "kw AS ("
            + "  SELECT chunk_id,"
            + "    ts_rank("
            + "      to_tsvector('english', text),"
            + "      plainto_tsquery('english', ?)"
            + "    ) AS kw_score"
            + "  FROM " + q
            + "  WHERE tenant_id = ?"
            + "  AND to_tsvector('english', text)"
            + "      @@ plainto_tsquery('english', ?)"
            + "  LIMIT ?"
            + ")"
            + "SELECT c.chunk_id, c.document_id,"
            + " c.source_id, c.tenant_id, c.text,"
            + " c.page, c.section, c.paragraph,"
            + " c.checksum, c.classification,"
            + " c.language, c.metadata,"
            + " COALESCE(v.vec_score,0)*0.6"
            + " + COALESCE(k.kw_score,0)*0.4"
            + "   AS score"
            + " FROM " + q + " c"
            + " LEFT JOIN vec v"
            + "   ON c.chunk_id = v.chunk_id"
            + " LEFT JOIN kw k"
            + "   ON c.chunk_id = k.chunk_id"
            + " WHERE v.chunk_id IS NOT NULL"
            + "    OR k.chunk_id IS NOT NULL"
            + " ORDER BY score DESC"
            + " LIMIT ?";
    }

    static String sizeSql(String q) {
        return "SELECT COUNT(*) FROM " + q;
    }
}
