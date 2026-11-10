package io.github.regalpine.agentforge.memory.postgresql;

/**
 * PostgreSQL DDL and SQL statements for
 * the memory store.
 * AF-ENHANCE-03 §5-7: all SQL is
 * parameterised and schema-qualified.
 */
final class PostgresMemorySql {

    private PostgresMemorySql() {}

    static String createTableSql(String qualified) {
        return "CREATE TABLE IF NOT EXISTS " + qualified + " ("
            + " memory_id       TEXT PRIMARY KEY,"
            + " tenant_id       TEXT NOT NULL,"
            + " subject_id      TEXT NOT NULL,"
            + " type            TEXT NOT NULL,"
            + " scope           TEXT NOT NULL,"
            + " content         TEXT NOT NULL,"
            + " attributes      JSONB NOT NULL DEFAULT '{}'::jsonb,"
            + " created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),"
            + " updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),"
            + " expires_at      TIMESTAMPTZ,"
            + " status          TEXT NOT NULL DEFAULT 'CANDIDATE',"
            + " source          TEXT NOT NULL DEFAULT 'SYSTEM',"
            + " confidence_val  DOUBLE PRECISION,"
            + " confidence_basis TEXT,"
            + " importance      TEXT NOT NULL DEFAULT 'NORMAL',"
            + " prov_origin     TEXT,"
            + " prov_chain      JSONB NOT NULL DEFAULT '[]'::jsonb,"
            + " classification  TEXT NOT NULL DEFAULT 'INTERNAL',"
            + " version         BIGINT NOT NULL DEFAULT 0"
            + ")";
    }

    static String createIndexSql(String qualified) {
        return "CREATE INDEX IF NOT EXISTS "
            + "idx_" + simpleName(qualified) + "_tenant "
            + "ON " + qualified + " (tenant_id, subject_id, type, status)";
    }

    static String createVectorColumnSql(
        String qualified, int dim
    ) {
        return "ALTER TABLE " + qualified
            + " ADD COLUMN IF NOT EXISTS embedding vector("
            + dim + ")";
    }

    static String insertSql(String qualified) {
        return "INSERT INTO " + qualified + " ("
            + "memory_id, tenant_id, subject_id, type, scope,"
            + "content, attributes, created_at, updated_at,"
            + "expires_at, status, source, confidence_val,"
            + "confidence_basis, importance, prov_origin,"
            + "prov_chain, classification, version"
            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    }

    static String selectByIdSql(String qualified) {
        return "SELECT * FROM " + qualified
            + " WHERE memory_id = ?";
    }

    static String updateSql(String qualified) {
        return "UPDATE " + qualified
            + " SET content=?, attributes=?, updated_at=?,"
            + " expires_at=?, status=?, version=version+1"
            + " WHERE memory_id = ? AND version = ?";
    }

    static String softDeleteSql(String qualified) {
        return "UPDATE " + qualified
            + " SET status='DELETED', updated_at=now()"
            + " WHERE memory_id = ?";
    }

    static String searchBySubjectSql(
        String qualified
    ) {
        return "SELECT * FROM " + qualified
            + " WHERE tenant_id = ?"
            + " AND subject_id = ?"
            + " AND status = 'ACTIVE'"
            + " AND (expires_at IS NULL"
            + "      OR expires_at > now())"
            + " ORDER BY updated_at DESC"
            + " LIMIT ?";
    }

    static String searchByTypeSql(
        String qualified
    ) {
        return "SELECT * FROM " + qualified
            + " WHERE tenant_id = ?"
            + " AND subject_id = ?"
            + " AND type = ?"
            + " AND status = 'ACTIVE'"
            + " AND (expires_at IS NULL"
            + "      OR expires_at > now())"
            + " ORDER BY updated_at DESC"
            + " LIMIT ?";
    }

    static String keywordSearchSql(
        String qualified
    ) {
        return "SELECT * FROM " + qualified
            + " WHERE tenant_id = ?"
            + " AND subject_id = ?"
            + " AND status = 'ACTIVE'"
            + " AND (expires_at IS NULL"
            + "      OR expires_at > now())"
            + " AND to_tsvector('english', content)"
            + "     @@ plainto_tsquery('english', ?)"
            + " ORDER BY ts_rank("
            + "   to_tsvector('english', content),"
            + "   plainto_tsquery('english', ?)"
            + " ) DESC"
            + " LIMIT ?";
    }

    static String semanticSearchSql(
        String qualified
    ) {
        return "SELECT *, embedding <=> ?::vector AS distance"
            + " FROM " + qualified
            + " WHERE tenant_id = ?"
            + " AND subject_id = ?"
            + " AND status = 'ACTIVE'"
            + " AND (expires_at IS NULL"
            + "      OR expires_at > now())"
            + " ORDER BY distance ASC"
            + " LIMIT ?";
    }

    static String hybridSearchSql(
        String qualified
    ) {
        return "WITH kw AS ("
            + "  SELECT memory_id,"
            + "    ts_rank("
            + "      to_tsvector('english', content),"
            + "      plainto_tsquery('english', ?)"
            + "    ) AS kw_score"
            + "  FROM " + qualified
            + "  WHERE tenant_id = ?"
            + "    AND subject_id = ?"
            + "    AND status = 'ACTIVE'"
            + "    AND to_tsvector('english', content)"
            + "        @@ plainto_tsquery('english', ?)"
            + "),"
            + "vec AS ("
            + "  SELECT memory_id,"
            + "    1 - (embedding <=> ?::vector) AS vec_score"
            + "  FROM " + qualified
            + "  WHERE tenant_id = ?"
            + "    AND subject_id = ?"
            + "    AND status = 'ACTIVE'"
            + "    AND embedding IS NOT NULL"
            + ")"
            + "SELECT m.*, "
            + "  COALESCE(k.kw_score,0)*0.4"
            + "  + COALESCE(v.vec_score,0)*0.6"
            + "    AS combined_score"
            + " FROM " + qualified + " m"
            + " LEFT JOIN kw k"
            + "   ON m.memory_id = k.memory_id"
            + " LEFT JOIN vec v"
            + "   ON m.memory_id = v.memory_id"
            + " WHERE k.memory_id IS NOT NULL"
            + "    OR v.memory_id IS NOT NULL"
            + " ORDER BY combined_score DESC"
            + " LIMIT ?";
    }

    private static String simpleName(String q) {
        int dot = q.lastIndexOf('.');
        return dot >= 0 ? q.substring(dot + 1) : q;
    }
}
