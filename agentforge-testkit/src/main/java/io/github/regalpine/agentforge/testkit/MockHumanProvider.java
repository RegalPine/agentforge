package io.github.regalpine.agentforge.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 模拟 Human Provider（HITL）。
 *
 * <p>模拟审批决策。
 * 对应 AF-ENHANCE-04 §61-62。</p>
 */
public final class MockHumanProvider {

    private final Map<String,
        Queue<HumanDecision>> decisions =
        new ConcurrentHashMap<>();

    private final List<ApprovalRecord>
        approvalHistory =
        new ArrayList<>();

    private HumanDecision defaultDecision;

    /**
     * 设置默认批准。
     */
    public MockHumanProvider approve() {
        this.defaultDecision =
            HumanDecision.APPROVE;
        return this;
    }

    /**
     * 设置默认拒绝。
     */
    public MockHumanProvider reject() {
        this.defaultDecision =
            HumanDecision.REJECT;
        return this;
    }

    /**
     * 为特定策略设置决策。
     */
    public MockHumanProvider
    forPolicy(
        String policyId,
        HumanDecision decision
    ) {
        decisions.computeIfAbsent(
            policyId,
            k -> new LinkedBlockingQueue<>()
        ).add(decision);
        return this;
    }

    /**
     * 获取审批决策。
     */
    public HumanDecision decide(
        String policyId
    ) {
        approvalHistory.add(
            new ApprovalRecord(
                policyId
            )
        );

        Queue<HumanDecision> queue =
            decisions.get(policyId);
        if (queue != null
            && !queue.isEmpty()
        ) {
            return queue.poll();
        }
        return defaultDecision != null
            ? defaultDecision
            : HumanDecision.APPROVE;
    }

    /**
     * 审批历史。
     */
    public List<ApprovalRecord>
    getApprovalHistory() {
        return List.copyOf(approvalHistory);
    }

    /**
     * 重置。
     */
    public void reset() {
        decisions.clear();
        approvalHistory.clear();
        defaultDecision = null;
    }

    // --- Types ---

    public enum HumanDecision {
        APPROVE,
        REJECT,
        MODIFY,
        ABORT
    }

    public record ApprovalRecord(
        String policyId
    ) {}
}
