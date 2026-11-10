package io.github.regalpine.agentforge.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检查点引擎。
 *
 * <p>管理 Execution 的状态快照，
 * 支持保存/恢复/查询。</p>
 */
public final class CheckpointEngine {

    private final Map<String, List<Checkpoint>>
        checkpoints = new ConcurrentHashMap<>();

    /**
     * 保存检查点。
     */
    public void save(Checkpoint checkpoint) {
        checkpoints
            .computeIfAbsent(
                checkpoint.executionId(),
                k -> new ArrayList<>()
            )
            .add(checkpoint);
    }

    /**
     * 获取最新检查点。
     */
    public Checkpoint latest(
        String executionId
    ) {
        List<Checkpoint> list =
            checkpoints.get(executionId);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
            .max(Comparator.comparingLong(
                Checkpoint::sequence
            ))
            .orElse(null);
    }

    /**
     * 获取指定 execution 的所有检查点。
     */
    public List<Checkpoint> list(
        String executionId
    ) {
        List<Checkpoint> list =
            checkpoints.get(executionId);
        if (list == null) {
            return List.of();
        }
        return List.copyOf(list);
    }

    /**
     * 从检查点恢复状态。
     */
    public Map<String, Object> restore(
        String executionId
    ) {
        Checkpoint cp = latest(executionId);
        if (cp == null) {
            return Map.of();
        }
        return cp.state();
    }

    /**
     * 删除指定 execution 的检查点。
     */
    public void clear(String executionId) {
        checkpoints.remove(executionId);
    }

    /**
     * 检查点总数。
     */
    public int size() {
        int total = 0;
        for (List<Checkpoint> list :
            checkpoints.values()
        ) {
            total += list.size();
        }
        return total;
    }
}
