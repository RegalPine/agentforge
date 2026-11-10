package io.github.regalpine.agentforge.server.persistence;

import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlanRegistry;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * PostgreSQL-backed {@link ExecutionPlanRegistry}.
 * <p>
 * Stores compiled {@link ExecutionPlan} as JSON in the
 * {@code agentforge_server.execution_plan} table.
 * Registration is idempotent: re-registering the same graph version
 * is silently accepted (ON CONFLICT DO NOTHING).
 */
public final class JdbcExecutionPlanRegistry implements ExecutionPlanRegistry {

    private static final String TABLE = "agentforge_server.execution_plan";
    private final JdbcAccess db;

    public JdbcExecutionPlanRegistry(DataSource source) {
        this.db = new JdbcAccess(source);
    }

    @Override
    public void register(ExecutionPlan plan) {
        String planJson = ExecutionPlanCodec.encode(plan);
        db.update(
            "INSERT INTO " + TABLE
                + " (graph_id, graph_version, fingerprint, plan_json) VALUES (?, ?, ?, ?)"
                + " ON CONFLICT (graph_id, graph_version) DO NOTHING",
            plan.graphId().value(),
            plan.graphVersion().value(),
            plan.fingerprint(),
            planJson
        );
    }

    @Override
    public Optional<ExecutionPlan> find(EntityId graphId, Version graphVersion) {
        return db.query(
            "SELECT plan_json FROM " + TABLE + " WHERE graph_id = ? AND graph_version = ?",
            row -> ExecutionPlanCodec.decode(row.getString("plan_json")),
            graphId.value(),
            graphVersion.value()
        ).stream().findFirst();
    }
}
