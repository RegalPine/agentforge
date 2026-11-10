package io.github.regalpine.agentforge.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 审计日志。
 *
 * <p>记录不可篡改的治理事件。</p>
 */
public final class AuditLogger
    implements EventSubscriber {

    private final List<EventEnvelope> auditLog =
        new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(
        EventEnvelope event
    ) {
        if (isAuditEvent(event)) {
            auditLog.add(event);
        }
    }

    /**
     * 判断是否为审计事件。
     */
    public static boolean isAuditEvent(
        EventEnvelope event
    ) {
        String t = event.eventType();
        return "policy.denied".equals(t)
            || "policy.approval.required".equals(t)
            || "human.approval.required".equals(t)
            || "human.approval.received".equals(t)
            || "human.approval.rejected".equals(t)
            || "security.violation".equals(t)
            || "execution.failed".equals(t);
    }

    public List<EventEnvelope> entries() {
        return List.copyOf(auditLog);
    }

    public List<EventEnvelope> entriesFor(
        String executionId
    ) {
        List<EventEnvelope> result =
            new ArrayList<>();
        for (EventEnvelope e : auditLog) {
            if (executionId.equals(
                e.executionId()
            )) {
                result.add(e);
            }
        }
        return List.copyOf(result);
    }

    public void clear() {
        auditLog.clear();
    }
}
