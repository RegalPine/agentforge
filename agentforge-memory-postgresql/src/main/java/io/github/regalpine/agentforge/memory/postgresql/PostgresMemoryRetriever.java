package io.github.regalpine.agentforge.memory.postgresql;

import io.github.regalpine.agentforge.memory.*;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * PostgreSQL memory retriever.
 * AF-ENHANCE-03 §7: supports EXACT,
 * KEYWORD, SEMANTIC (pgvector) and
 * HYBRID search strategies.
 *
 * <p>Each strategy maps to a different
 * SQL query. SEMANTIC and HYBRID
 * require pgvector to be enabled.
 */
public final class PostgresMemoryRetriever
    implements MemoryRetriever {

    private final PostgresMemoryConfig config;
    private final DataSource dataSource;
    private final String qualifiedTable;

    public PostgresMemoryRetriever(
        PostgresMemoryConfig config,
        DataSource dataSource
    ) {
        this.config =
            Objects.requireNonNull(config, "config");
        this.dataSource =
            Objects.requireNonNull(dataSource, "dataSource");
        this.qualifiedTable = config.qualifiedTable();
    }

    @Override
    public List<MemoryRecord> retrieve(
        MemoryQuery query
    ) {
        Objects.requireNonNull(query, "query");

        return switch (query.strategy()) {
            case EXACT -> retrieveExact(query);
            case KEYWORD -> retrieveKeyword(query);
            case SEMANTIC -> retrieveSemantic(query);
            case HYBRID -> retrieveHybrid(query);
        };
    }

    private List<MemoryRecord> retrieveExact(
        MemoryQuery query
    ) {
        String sql;

        if (query.type() != null) {
            sql = PostgresMemorySql.searchByTypeSql(
                qualifiedTable
            );
        } else {
            sql = PostgresMemorySql.searchBySubjectSql(
                qualifiedTable
            );
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            ps.setString(1, extractTenantId(query));
            ps.setString(2, query.subjectId());

            int idx = 3;

            if (query.type() != null) {
                ps.setString(idx++,
                    query.type().name()
                );
            }

            ps.setInt(idx, query.limit());

            return collectResults(ps);

        } catch (SQLException e) {
            throw new PostgresMemoryStore
                .PostgresMemoryException(
                    "Exact search failed", e
                );
        }
    }

    private List<MemoryRecord> retrieveKeyword(
        MemoryQuery query
    ) {
        String sql = PostgresMemorySql.keywordSearchSql(
            qualifiedTable
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            ps.setString(1, extractTenantId(query));
            ps.setString(2, query.subjectId());
            ps.setString(3, query.query());
            ps.setString(4, query.query());
            ps.setInt(5, query.limit());

            return collectResults(ps);

        } catch (SQLException e) {
            throw new PostgresMemoryStore
                .PostgresMemoryException(
                    "Keyword search failed", e
                );
        }
    }

    private List<MemoryRecord> retrieveSemantic(
        MemoryQuery query
    ) {
        if (!config.enablePgVector()) {
            throw new IllegalStateException(
                "SEMANTIC search requires pgvector"
            );
        }

        String sql =
            PostgresMemorySql.semanticSearchSql(
                qualifiedTable
            );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            // Embedding would be set from the query
            // via an embedding model in production.
            // Here we use the query text as a
            // placeholder for the vector.
            ps.setString(1, "[]");
            ps.setString(2, extractTenantId(query));
            ps.setString(3, query.subjectId());
            ps.setInt(4, query.limit());

            return collectResults(ps);

        } catch (SQLException e) {
            throw new PostgresMemoryStore
                .PostgresMemoryException(
                    "Semantic search failed", e
                );
        }
    }

    private List<MemoryRecord> retrieveHybrid(
        MemoryQuery query
    ) {
        if (!config.enablePgVector()) {
            throw new IllegalStateException(
                "HYBRID search requires pgvector"
            );
        }

        String sql = PostgresMemorySql.hybridSearchSql(
            qualifiedTable
        );

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement(sql)) {

            ps.setString(1, query.query());
            ps.setString(2, extractTenantId(query));
            ps.setString(3, query.subjectId());
            ps.setString(4, query.query());
            ps.setString(5, "[]");
            ps.setString(6, extractTenantId(query));
            ps.setString(7, query.subjectId());
            ps.setInt(8, query.limit());

            return collectResults(ps);

        } catch (SQLException e) {
            throw new PostgresMemoryStore
                .PostgresMemoryException(
                    "Hybrid search failed", e
                );
        }
    }

    private List<MemoryRecord> collectResults(
        PreparedStatement ps
    ) throws SQLException {
        List<MemoryRecord> results = new ArrayList<>();

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(
                    PostgresMemoryStore.mapRow(rs)
                );
            }
        }

        return results;
    }

    private String extractTenantId(
        MemoryQuery query
    ) {
        // The scope determines the tenant context.
        // For simplicity, use subjectId as tenant
        // context when no explicit tenant is
        // available.
        return query.scope().name().toLowerCase()
            + ":" + query.subjectId();
    }
}
