package io.github.regalpine.agentforge.rag.postgresql;

import io.github.regalpine.agentforge.rag.index.KnowledgeIndex;
import io.github.regalpine.agentforge.rag.model.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * PGVector knowledge index.
 * AF-ENHANCE-03 §10: implements the
 * KnowledgeIndex SPI using PostgreSQL
 * with the pgvector extension for
 * vector similarity search.
 *
 * <p>Supports upsert, delete, vector
 * search, keyword search and hybrid
 * search (weighted combination).
 */
public final class PgVectorKnowledgeIndex
    implements KnowledgeIndex {

    private final PgVectorConfig config;
    private final DataSource dataSource;
    private final String qualifiedTable;

    public PgVectorKnowledgeIndex(
        PgVectorConfig config,
        DataSource dataSource
    ) {
        this.config =
            Objects.requireNonNull(config, "config");
        this.dataSource =
            Objects.requireNonNull(dataSource, "dataSource");
        this.qualifiedTable = config.qualifiedTable();
    }

    /**
     * Initialise the schema (DDL).
     */
    public void initialiseSchema() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                PgVectorSql.createTableSqlDim(
                    qualifiedTable,
                    config.embeddingDimension()
                )
            );
            stmt.execute(
                PgVectorSql.createTenantIndexSql(
                    qualifiedTable
                )
            );
            stmt.execute(
                PgVectorSql.createVectorIndexSql(
                    qualifiedTable,
                    config.indexMethod(),
                    config.efConstruction(),
                    config.mParam()
                )
            );
        }
    }

    @Override
    public void upsert(KnowledgeChunk chunk) {
        Objects.requireNonNull(chunk, "chunk");

        String sql = PgVectorSql.upsertSql(qualifiedTable);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            ps.setString(1, chunk.chunkId());
            ps.setString(2, chunk.documentId());
            ps.setString(3, chunk.sourceId());
            ps.setString(4, chunk.tenantId());
            ps.setString(5, chunk.text());
            ps.setInt(6, chunk.location().page());
            ps.setString(7, chunk.location().section());
            ps.setInt(8, chunk.location().paragraph());
            ps.setString(9, chunk.checksum());
            ps.setString(10,
                chunk.classification().name()
            );
            ps.setString(11, chunk.language());
            ps.setString(12,
                formatVector(chunk.embedding())
            );
            ps.setString(13, "{}");

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new PgVectorException(
                "Upsert failed: " + chunk.chunkId(), e
            );
        }
    }

    @Override
    public void delete(String chunkId) {
        Objects.requireNonNull(chunkId, "chunkId");

        String sql = PgVectorSql.deleteSql(qualifiedTable);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            ps.setString(1, chunkId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new PgVectorException(
                "Delete failed: " + chunkId, e
            );
        }
    }

    @Override
    public List<RetrievalResult> search(
        KnowledgeQuery query
    ) {
        Objects.requireNonNull(query, "query");

        // Default to keyword search when no
        // embedding is available.
        String sql = PgVectorSql.keywordSearchSql(
            qualifiedTable
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            ps.setString(1, query.query());
            ps.setString(2, query.scope().tenantId());
            ps.setString(3, query.query());
            ps.setInt(4, query.topK());

            return collectResults(ps);

        } catch (SQLException e) {
            throw new PgVectorException(
                "Search failed", e
            );
        }
    }

    /**
     * Vector similarity search with a
     * pre-computed query embedding.
     */
    public List<RetrievalResult> vectorSearch(
        float[] queryEmbedding,
        String tenantId,
        int topK
    ) {
        String sql = PgVectorSql.vectorSearchSql(
            qualifiedTable
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            ps.setString(1, formatVector(queryEmbedding));
            ps.setString(2, tenantId);
            ps.setInt(3, topK);

            return collectResults(ps);

        } catch (SQLException e) {
            throw new PgVectorException(
                "Vector search failed", e
            );
        }
    }

    @Override
    public String indexType() {
        return "PGVECTOR";
    }

    @Override
    public int size() {
        String sql = PgVectorSql.sizeSql(qualifiedTable);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new PgVectorException(
                "Size query failed", e
            );
        }
    }

    private List<RetrievalResult> collectResults(
        PreparedStatement ps
    ) throws SQLException {
        List<RetrievalResult> results = new ArrayList<>();

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String chunkId = rs.getString("chunk_id");
                String docId = rs.getString("document_id");
                String text = rs.getString("text");
                double score = rs.getDouble("score");

                ChunkLocation loc = new ChunkLocation(
                    rs.getInt("page"),
                    rs.getString("section"),
                    rs.getInt("paragraph")
                );

                RetrievalScoreBreakdown breakdown =
                    RetrievalScoreBreakdown.keyword(score);

                RetrievalProvenance prov =
                    new RetrievalProvenance(
                        rs.getString("source_id"),
                        docId, "",
                        chunkId, loc, ""
                    );

                results.add(new RetrievalResult(
                    chunkId, docId, text,
                    score, breakdown, loc, prov
                ));
            }
        }

        return results;
    }

    static String formatVector(float[] vec) {
        if (vec == null || vec.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * PGVector exception.
     */
    public static final class PgVectorException
        extends RuntimeException {

        public PgVectorException(
            String msg, Throwable cause
        ) {
            super(msg, cause);
        }
    }
}
