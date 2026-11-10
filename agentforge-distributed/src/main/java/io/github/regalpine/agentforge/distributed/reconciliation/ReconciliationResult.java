package io.github.regalpine.agentforge.distributed.reconciliation;

public record ReconciliationResult(
    int recreatedTasks,
    int removedTasks,
    int recoveredLeases,
    int staleTasks
) {
}
