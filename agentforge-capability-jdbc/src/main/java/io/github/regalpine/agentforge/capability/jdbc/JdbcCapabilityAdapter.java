package io.github.regalpine.agentforge.capability.jdbc;

import io.github.regalpine.agentforge.capability.*;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * JDBC capability adapter.
 * AF-ENHANCE-02 §12-14: invokes
 * pre-defined parameterised SQL
 * statements via JDBC.
 *
 * <p>Security model:
 * <ul>
 *   <li>LLM cannot generate arbitrary SQL</li>
 *   <li>All queries are pre-defined
 *       parameterised statements</li>
 *   <li>Default side effect is READ_ONLY</li>
 *   <li>Parameters are bound via
 *       PreparedStatement</li>
 * </ul>
 */
public final class JdbcCapabilityAdapter
    implements CapabilityAdapter {

    private final DatabaseCapabilityDescriptor descriptor;
    private final javax.sql.DataSource dataSource;

    private volatile AdapterHealthStatus healthStatus =
        AdapterHealthStatus.UNKNOWN;

    public JdbcCapabilityAdapter(
        DatabaseCapabilityDescriptor descriptor,
        javax.sql.DataSource dataSource
    ) {
        Objects.requireNonNull(
            descriptor, "descriptor"
        );

        Objects.requireNonNull(
            dataSource, "dataSource"
        );

        this.descriptor = descriptor;
        this.dataSource = dataSource;
    }

    @Override
    public AdapterMetadata metadata() {
        return new AdapterMetadata(
            "jdbc-adapter-"
                + descriptor.capabilityId(),
            "jdbc",
            "1.0.0",
            "JDBC Adapter: "
                + descriptor.name(),
            descriptor.description(),
            Set.of(descriptor.sideEffect()),
            Map.of(),
            descriptor.metadata()
        );
    }

    @Override
    public CapabilityResult execute(
        CapabilityInvocation invocation
    ) {
        try (Connection conn =
                 dataSource.getConnection()
        ) {
            conn.setAutoCommit(true);

            String sql =
                descriptor.parameterisedSql();

            try (PreparedStatement stmt =
                     conn.prepareStatement(sql)
            ) {
                // Bind parameters
                bindParameters(
                    stmt, invocation.arguments()
                );

                // Apply row limit for queries
                if (descriptor.operation()
                    == DatabaseOperation.SELECT
                ) {
                    stmt.setMaxRows(
                        descriptor.maxRows()
                    );
                }

                boolean hasResultSet =
                    stmt.execute();

                healthStatus =
                    AdapterHealthStatus.AVAILABLE;

                if (hasResultSet) {
                    return mapResultSet(stmt);
                }

                int affected =
                    stmt.getUpdateCount();

                Map<String, Object> output =
                    new LinkedHashMap<>();

                output.put(
                    "affectedRows",
                    affected
                );

                return new CapabilitySuccess(output);
            }

        } catch (SQLException e) {
            healthStatus =
                AdapterHealthStatus.DEGRADED;

            return new CapabilityFailure(
                "SQL_ERROR",
                "Database error: "
                    + e.getMessage(),
                false
            );

        } catch (Exception e) {
            healthStatus =
                AdapterHealthStatus.DEGRADED;

            return new CapabilityFailure(
                "JDBC_ERROR",
                e.getMessage(),
                true
            );
        }
    }

    @Override
    public AdapterHealth health() {
        // Try a connection to check health
        try (Connection conn =
                 dataSource.getConnection()
        ) {
            healthStatus =
                conn.isValid(5)
                    ? AdapterHealthStatus.AVAILABLE
                    : AdapterHealthStatus.UNAVAILABLE;

        } catch (SQLException e) {
            healthStatus =
                AdapterHealthStatus.UNAVAILABLE;
        }

        return new AdapterHealth(
            healthStatus,
            Instant.now(),
            null,
            Map.of(
                "table",
                descriptor.tableName() != null
                    ? descriptor.tableName()
                    : "n/a",
                "operation",
                descriptor.operation().name()
            )
        );
    }

    private void bindParameters(
        PreparedStatement stmt,
        Map<String, Object> arguments
    ) throws SQLException {
        List<String> paramNames =
            descriptor.parameterNames();

        for (int i = 0; i < paramNames.size(); i++) {
            String paramName =
                paramNames.get(i);

            Object value =
                arguments.get(paramName);

            if (value == null) {
                stmt.setNull(
                    i + 1,
                    Types.VARCHAR
                );
            } else if (value instanceof String s) {
                stmt.setString(i + 1, s);
            } else if (value instanceof Integer n) {
                stmt.setInt(i + 1, n);
            } else if (value instanceof Long l) {
                stmt.setLong(i + 1, l);
            } else if (value instanceof Double d) {
                stmt.setDouble(i + 1, d);
            } else if (value instanceof Boolean b) {
                stmt.setBoolean(i + 1, b);
            } else {
                stmt.setObject(
                    i + 1, value
                );
            }
        }
    }

    private CapabilityResult mapResultSet(
        PreparedStatement stmt
    ) throws SQLException {
        ResultSet rs = stmt.getResultSet();
        ResultSetMetaData meta =
            rs.getMetaData();

        int columnCount =
            meta.getColumnCount();

        List<String> columnNames =
            new ArrayList<>();

        for (int i = 1;
             i <= columnCount;
             i++
        ) {
            columnNames.add(
                meta.getColumnName(i)
            );
        }

        List<Map<String, Object>> rows =
            new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> row =
                new LinkedHashMap<>();

            for (int i = 1;
                 i <= columnCount;
                 i++
            ) {
                row.put(
                    columnNames.get(i - 1),
                    rs.getObject(i)
                );
            }

            rows.add(row);
        }

        Map<String, Object> output =
            new LinkedHashMap<>();

        output.put("columns", columnNames);
        output.put("rows", rows);
        output.put("rowCount", rows.size());

        return new CapabilitySuccess(output);
    }
}
